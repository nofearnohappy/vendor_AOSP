package com.mediatek.mediatekdm.fumo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RecoverySystem;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmOperationManager.TriggerResult;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.SimpleXMLAccessor;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;
import com.mediatek.mediatekdm.mdm.fumo.FumoAction;
import com.mediatek.mediatekdm.mdm.fumo.FumoHandler;
import com.mediatek.mediatekdm.mdm.fumo.FumoState;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumo;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumoUpdateResult.ResultCode;
import com.mediatek.mediatekdm.pl.DmPLDlPkg;

import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class FumoManager extends SessionHandler implements FumoHandler {
    private int mUpdateType = 0;
    private Handler mHandler = null;
    private HandlerThread mHandlerThread = null;
    private static final int EXIT_HANDER_THREAD = -1;
    private Set<IDmFumoStateObserver> mFumoObservers = null;
    private Set<IDmFumoDownloadProgressObserver> mFumoDownloadObservers = null;
    private String mCancelReason = null;
    private int mFumoState;
    private DmOperationManager mOperationManager;
    private int[] mTimingArray = null;
    private String[] mTextArray = null;
    private AlarmManager mAlarmManager;
    private PendingIntent mReminderOperation = null;
    private DmFumoNotification mNotification = null;
    private MdmFumo mFumo;

    static final int RESULT_SUCCESSFUL = 200; /* MdmFumoUpdateResult.ResultCode.SUCCESSFUL */
    static final int RESULT_UPDATE_FAILED = 410; /* MdmFumoUpdateResult.ResultCode.UPDATE_FAILED */
    public static final int UPDATE_ALARM_NEVER = Integer.MAX_VALUE;
    public static final int UPDATE_ALARM_NOW = 0;
    public static final int DL_TIME_OUT = 5 * 60 * 1000; // 5 minutes
    public static final int DL_MAX_RETRY = 1;

    public FumoManager(DmService service) {
        super(service);
        mOperationManager = DmOperationManager.getInstance();
        mAlarmManager = (AlarmManager) mService.getSystemService(Context.ALARM_SERVICE);
        mFumoState = PersistentContext.getInstance().getFumoState();
        mFumoObservers = new HashSet<IDmFumoStateObserver>();
        mFumoDownloadObservers = new HashSet<IDmFumoDownloadProgressObserver>();
        mHandlerThread = new HandlerThread("FUMO handler thread");
        mHandlerThread.start();
        mNotification = new DmFumoNotification(service);
        registerDownloadObserver(mNotification);
        registerObserver(mNotification);

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == EXIT_HANDER_THREAD) {
                    getLooper().quit();
                }
            }
        };

        try {
            mFumo = new MdmFumo(FumoComponent.ROOT_URI, this);
            if (PersistentContext.getInstance().getFumoState() == DmFumoState.DOWNLOAD_PAUSED) {
                /* For corruption or power off when downloading */
                Log.d(TAG.FUMO, "Reset state to DOWNLOAD_FAILED");
                mFumo.setState(FumoState.DOWNLOAD_FAILED);
            }
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    public synchronized void destroy() {
        mHandler.sendEmptyMessage(EXIT_HANDER_THREAD);
        mHandler = null;
        mHandlerThread = null;
        clearObservers();
        clearDownloadObserver();
        mFumo.destroy();
    }

    @Override
    public synchronized boolean confirmDownload(final DownloadDescriptor dd, MdmFumo fumo) {
        final DmOperation operation = mOperationManager.current();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG.FUMO, "confirmDownload");
                // Clear previous package files
                PersistentContext.getInstance().deleteDeltaPackage();
                // Save new package information
                PersistentContext.getInstance().setDownloadDescriptor(dd);
                setFumoState(DmFumoState.NEW_VERSION_FOUND, operation, dd);
            }
        });
        return false;
    }

    @Override
    public synchronized boolean confirmUpdate(MdmFumo fumo) {
        Log.i(TAG.FUMO, "confirmUpdate");
        final DmOperation operation = mOperationManager.current();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int status = getFumoState();
                // The cancel status transition may be delayed, so here come the defensive codes.
                if (status == DmFumoState.DOWNLOAD_CANCELED) {
                    Log.d(TAG.FUMO, "Already canceled, ignore complete message");
                    return;
                }
                setFumoState(DmFumoState.DOWNLOAD_COMPLETE, operation, null);
            }
        });
        return false;
    }

    @Override
    public synchronized MdmFumoUpdateResult executeUpdate(String updatePkgPath, MdmFumo fumo) {
        // We do not execute update here, so ignore it.
        return null;
    }

    @Override
    protected synchronized void dmComplete() {
        Log.d(TAG.FUMO, "dmComplete");
        // Catch last operation before we invoke super.dmComplete()
        DmOperation operation = mOperationManager.current();
        super.dmComplete();
        // Read and clear.
        final int fumoAction = operation.getIntProperty(KEY.ACTION_MASK, 0);
        operation.remove(KEY.ACTION_MASK);
        Log.d(TAG.FUMO, "FUMO action is " + fumoAction);

        if (Type.isReportOperation(operation.getProperty(KEY.TYPE))) {
            mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            return;
        }

        if (fumoAction != FumoAction.NONE) {
            // New package found
            DmPLDlPkg.setDeltaFileName(FumoComponent.FUMO_FILE_NAME);
            DmOperation dlOperation = new DmOperation(DmOperation.generateId(),
                    FumoManager.DL_TIME_OUT, FumoManager.DL_MAX_RETRY);
            dlOperation.initDLFumo();
            dlOperation.setProperty(KEY.INITIATOR, operation.getProperty(KEY.INITIATOR, "Server"));
            // Engine will trigger DL session later automatically, so we cut in line in
            // operations to match that.
            mOperationManager.triggerNow(dlOperation);
            // DmFumoState.NEW_VERSION_FOUND will be reached later when DD is ready
        } else {
            Log.i(TAG.FUMO, "No FUMO action");
            setFumoState(DmFumoState.NO_NEW_VERSION_FOUND, operation, "NO_FUMO_ACTION");
            // Trigger next operation.
            mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        }
    }

    @Override
    protected void dmAbort(int lastError) {
        // Catch last operation
        DmOperation operation = mOperationManager.current();
        super.dmAbort(lastError);

        if (!Type.isReportOperation(operation.getProperty(KEY.TYPE))) {
            // If FUMO DL session is aborted, FUMO engine will remove pending DL session trigger
            // automatically. So we only need to notify observers.
            if (lastError == MdmError.CANCEL.val) {
                // FUMO state transferred to NO_NEW_VERSION_FOUND, with message DM_SESSION_CANCELED
                setFumoState(DmFumoState.NO_NEW_VERSION_FOUND, operation, "USER_CANCELED");
            } else if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
                setFumoState(DmFumoState.NO_NEW_VERSION_FOUND, operation, "FAILED");
            } else if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() <= 0) {
                setFumoState(DmFumoState.NO_NEW_VERSION_FOUND, operation, "NETWORK_ERROR");
            }
        }
    }

    @Override
    protected void dlStart() {
        mCancelReason = null;
        DmPLDlPkg.setDeltaFileName(FumoComponent.FUMO_FILE_NAME);
        if (mFumoState == DmFumoState.DOWNLOAD_PAUSED || mFumoState == DmFumoState.DOWNLOADING) {
            setFumoState(DmFumoState.DOWNLOADING, mOperationManager.current(), null);
        } else {
            setFumoState(DmFumoState.DOWNLOAD_STARTED, mOperationManager.current(), null);
        }
    }

    @Override
    protected void dlAbort(int lastError) {
        String errorCode = MdmException.MdmError.fromInt(lastError).toString();
        Log.i(TAG.FUMO, "Get error message. LastError is " + errorCode);

        DmOperation operation = mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            // Recoverable network error
            mOperationManager.notifyCurrentAborted();
            Message msg = mService.getHandler().obtainMessage(
                    IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.getHandler().sendMessageDelayed(msg, operation.timeout);
        } else {
            // Irrecoverable errors or max retry count reached or user paused/canceled
            if (lastError == MdmError.CANCEL.val) {
                if (mCancelReason != null) {
                    if (mCancelReason.equals("Cancel")) {
                        setFumoState(DmFumoState.DOWNLOAD_PAUSED, operation, "USER_CANCELED");
                    } else if (mCancelReason.equals("Pause")) {
                        setFumoState(DmFumoState.DOWNLOAD_PAUSED, operation, "USER_PAUSED");
                    }
                } else {
                    Log.w(TAG.FUMO, "Non-user-triggered cancellation");
                }
            } else if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
                setFumoState(DmFumoState.DOWNLOAD_FAILED, operation, "FAILED");
            } else if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() <= 0) {
                setFumoState(DmFumoState.DOWNLOAD_FAILED, operation, "NETWORK_ERROR");
                mService.clearDmNotification();
            }
            mOperationManager.finishCurrent();
        }
    }

    /**
     * Clear FUMO DL states and schedule an report operation. This method will not trigger DM engine
     * to start an report session. The report session will be triggered in reportResult() later when
     * the operation is processed.
     *
     * @param reportDelay
     *        Time delay for report operation. 0 means no delay.
     */
    public void clearDlStateAndReport(int reportDelay, int result) {
        PersistentContext.getInstance().deleteDeltaPackage();
        mService.deleteFile(FumoComponent.FUMO_RESUME_FILE_NAME);
        setFumoState(DmFumoState.IDLE, null, null);

        DmOperation reportOperation = new DmOperation();
        reportOperation.initReportFumo(result, generateFumoReportInformation(result, true));
        mOperationManager.enqueue(reportOperation, true);
        if (!mOperationManager.isInRecovery()) {
            if (reportDelay > 0) {
                mService.getHandler().sendEmptyMessageDelayed(
                        IServiceMessage.MSG_OPERATION_PROCESS_NEXT, reportDelay);
            } else {
                mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            }
        }
    }

    public void queryNewVersion() {
        // Do NOT post run this method otherwise other operation may be inserted before CI
        // operation.
        Log.i(TAG.FUMO, "+queryNewVersion()");
        if (!mOperationManager.isBusy()) {
            DmOperation operation = new DmOperation();
            operation.initCIFumo();
            operation.setProperty(KEY.INITIATOR, "User");
            TriggerResult result = mOperationManager.triggerNow(operation);
            if (result != TriggerResult.SUCCESS) {
                throw new Error("Failed to trigger CI FUMO operation.");
            }
            try {
                mFumo.triggerSession(null, MdmFumo.ClientType.USER);
            } catch (MdmException e) {
                throw new Error(e);
            }
        } else if (mOperationManager.isInRecovery()
                && mOperationManager.current().getBooleanProperty(KEY.FUMO_TAG, false)
                && mOperationManager.current().getProperty(KEY.TYPE).equals(Type.TYPE_DL)) {
            Log.d(TAG.FUMO, "Recovery will be issued by service. Do nothing.");
        } else {
            throw new Error("Invalid operation state!");
        }
        Log.i(TAG.FUMO, "-queryNewVersion()");
    }

    public void retryQueryNewVersion() {
        Log.i(TAG.FUMO, "+retryQueryNewVersion()");
        try {
            mFumo.triggerSession(null, MdmFumo.ClientType.USER);
        } catch (MdmException e) {
            throw new Error(e);
        }
        Log.i(TAG.FUMO, "-retryQueryNewVersion()");
    }

    /**
     * Proceed download session to start download FUMO package.
     */
    public void startDlPkg() {
        Log.i(TAG.FUMO, "startDlPkg Proceed the download session.");
        mService.getController().proceedDLSession();
        setFumoState(DmFumoState.DOWNLOADING, mOperationManager.current(), null);
        Log.i(TAG.FUMO, "startDlPkg Download session proceeded.");
    }

    /**
     * Cancel download session and delete delta package to cancel download
     */
    public void cancelDlPkg() {
        mCancelReason = "Cancel";
        mService.clearDmNotification();
        Log.i(TAG.FUMO, "User cancel the download process.");
        mService.getController().cancelSession();
        if (mOperationManager.isInRecovery()) {
            setFumoState(DmFumoState.DOWNLOAD_PAUSED, mOperationManager.current(), "USER_CANCELED");
            mOperationManager.finishCurrent();
        }
    }

    /**
     * Cancel download session to pause download.
     */
    public void pauseDlPkg() {
        Log.i(TAG.FUMO, "pauseDlPkg() issued.");
        Log.i(TAG.FUMO, "pauseDlPkg Pause the download session.");
        mCancelReason = "Pause";
        mService.clearDmNotification();
        if (mService.getController() != null) {
            // cancelSession can be safely invoked in idle state of engine
            mService.getController().cancelSession();
        }
        if (mOperationManager.isInRecovery()) {
            setFumoState(DmFumoState.DOWNLOAD_PAUSED, mOperationManager.current(), "USER_PAUSED");
            mOperationManager.finishCurrent();
        }
        // else { the operation will be finished in dlAbort }
        Log.i(TAG.FUMO, "pauseDlPkg Download session Paused.");
    }

    /**
     * Resume download session to resume download. This method trigger an DL operation immediately
     * (by invoking triggerNow()).
     */
    public void resumeDlPkg() {
        Log.d(TAG.FUMO, "+resumeDlPkg()");
        DmOperation operation = new DmOperation(DmOperation.generateId(), DL_TIME_OUT, DL_MAX_RETRY);
        operation.initDLFumo();
        if (mOperationManager.triggerNow(operation) == TriggerResult.SUCCESS) {
            try {
                mFumo.resumeDLSession();
            } catch (MdmException e) {
                throw new Error(e);
            }
        } else {
            Log.e(TAG.FUMO, "DL operation trigger failed!");
        }
        Log.d(TAG.FUMO, "-resumeDlPkg()");
    }

    public void recoverDlPkg() {
        Log.d(TAG.FUMO, "recoverDlPkg()");
        try {
            mFumo.resumeDLSession();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    public void updateDownloadProgress(final long current, final long total) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG.FUMO, "updateDownloadProgress(" + current + "/" + total + ")");
                PersistentContext.getInstance().setDownloadedSize(current);
                for (IDmFumoDownloadProgressObserver o : mFumoDownloadObservers) {
                    o.updateProgress(current, total);
                }
            }
        });
    }

    /**
     * Perform firmware update action according to current update type. Update type is set by end
     * user via GUI.
     */
    public void firmwareUpdate() {
        Log.d(TAG.FUMO, "type is " + mUpdateType);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final String deltaFilePath = PlatformManager.getInstance().getPathInData(mService,
                        FumoComponent.DELTA_FILE);
                if (FotaDeltaFiles.unpackAndVerify(deltaFilePath) != FotaDeltaFiles.DELTA_VERIFY_OK) {
                    Log.i(TAG.FUMO, "UnpackAndVerify fail, report to server");
                    completeUpdate(ResultCode.PACKAGE_MISMATCH, true);
                    return;
                }

                int updateType = mTimingArray[mUpdateType];
                Log.i(TAG.FUMO, "setUpdateType type is " + updateType);
                if (updateType == FumoManager.UPDATE_ALARM_NOW) {
                    cancelReminderAlarm();
                    executeFwUpdate();
                } else if (updateType == FumoManager.UPDATE_ALARM_NEVER) {
                    cancelReminderAlarm();
                    mService.getHandler().sendEmptyMessage(
                            IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                } else {
                    setReminderAlarm(updateType);
                    mService.getHandler().sendEmptyMessage(
                            IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                }
            }
        };
        mHandler.post(r);
    }

    public void reportResult(DmOperation operation) {
        if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_FUMO)) {
            String account = operation.getProperty("report_account");
            if (account.equals("")) {
                account = null;
            }
            mService.getController().triggerReportSession(
                    operation.getProperty("report_correlator"),
                    operation.getProperty("report_format"), operation.getProperty("report_type"),
                    null, operation.getProperty("report_source"), null,
                    operation.getProperty("report_data"), account,
                    new SimpleSessionInitiator(operation.getProperty("report_initiator")));
        } else {
            throw new Error("Current operation is not FUMO report!");
        }
    }

    /**
     * Execute firmware update.
     */
    private void executeFwUpdate() {
        Log.d(TAG.FUMO, "+executeFwUpdate()");

        // Create a flag file to indicate FUMO update operation.
        // If file exist when reboot, it's triggered by FOTA.
        Log.d(TAG.FUMO, "Touch flag file");
        FumoComponent.saveTimeStamp(mService, new Date(), FumoComponent.FOTA_FLAG_FILE);

        // Install the OTA package to update
        try {
            File pkg = new File(PlatformManager.getInstance().getPathInData(mService,
                    FumoComponent.DELTA_FILE));
            RecoverySystem.installPackage(DmApplication.getInstance(), pkg);
            Log.e(TAG.FUMO, "RecoverySystem run installPackage.");
        } catch (IOException e) {
            Log.d(TAG.FUMO, "[executeFwUpdate] FumoManager installation failed");
            e.printStackTrace();
        }
        Log.d(TAG.FUMO, "-executeFwUpdate()");
    }

    /**
     * Remove packages, report result to DM server and show dialog to the user.
     *
     * @param result
     *        : update result
     * @param delOTA
     *        : whether delete OTA related files
     */
    private void completeUpdate(ResultCode result, boolean delOTA) {
        DmOperation operation = new DmOperation();
        operation.initReportFumo(result.val, generateFumoReportInformation(result.val, true));
        mOperationManager.enqueue(operation, true);
        mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        setFumoState(DmFumoState.UPDATE_COMPLETE, null, null);

        // Check whether remove the OTA related files
        if (delOTA) {
            PersistentContext.getInstance().deleteDeltaPackage();
            FotaDeltaFiles.delFingerprintFile();
        }

        // create result dialogue
        initReportActivity(result == ResultCode.SUCCESSFUL);
    }

    /**
     * Set reminder alarm due to the item id user selected
     *
     * @param int timeout - minutes to timeout
     */
    private void setReminderAlarm(int timeout) {
        Log.i(TAG.FUMO, "setAlarm Set reminder alarm");
        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.DM_FUMO_REMINDER);
        mReminderOperation = PendingIntent.getBroadcast(mService, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mAlarmManager.cancel(mReminderOperation);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + timeout * 60000),
                mReminderOperation);
    }

    /**
     * Cancel reminder alarm
     */
    private void cancelReminderAlarm() {
        Log.i(TAG.FUMO, "cancelAlarm, cancel reminder alarm");
        if (mReminderOperation != null) {
            Log.w(TAG.FUMO, "cancle reminder Alarm");
            mAlarmManager.cancel(mReminderOperation);
            mReminderOperation = null;
        }
    }

    /**
     * Start DmFumoReport to prompt user
     */
    public void initReportActivity(boolean isUpdateSucc) {
        Intent activityIntent = new Intent(mService, DmFumoReport.class);
        activityIntent.setAction("com.mediatek.mediatekdm.UPDATECOMPLETE");
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra("isUpdateSuccessfull", isUpdateSucc);
        mService.startActivity(activityIntent);
    }

    /**
     * Get update type strings for UI.
     *
     * @return An array of update type strings. UI will display these to end user.
     */
    public String[] getUpdateTypeStrings() {
        getReminderAndTiming();
        return mTextArray;
    }

    private static class ReminderConfAccessor extends SimpleXMLAccessor {
        public ReminderConfAccessor(InputStream is) {
            super();
            parse(is);
        }

        public String[] getTextArray() {
            String[] result = null;
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expression = xpath.compile("//operator/text/item");
                NodeList textList = (NodeList) expression.evaluate(mDocument,
                        XPathConstants.NODESET);
                result = new String[textList.getLength()];
                for (int i = 0; i < textList.getLength(); i++) {
                    result[i] = textList.item(i).getTextContent();
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
            return result;
        }

        public int[] getTimingArray() {
            int[] result = null;
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expression = xpath.compile("//operator/timing/item");
                NodeList textList = (NodeList) expression.evaluate(mDocument,
                        XPathConstants.NODESET);
                result = new int[textList.getLength()];
                for (int i = 0; i < textList.getLength(); i++) {
                    result[i] = Integer.parseInt(textList.item(i).getTextContent());
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
            return result;
        }
    }

    /**
     * Get update select items from configuration file CMCC UI request
     */
    private void getReminderAndTiming() {
        Log.i(TAG.FUMO, "Execute getReminderParser");
        FileInputStream is = null;
        try {
            is = new FileInputStream(PlatformManager.getInstance().getPathInSystem(
                    FumoComponent.REMINDER_CONFIG_FILE));
            ReminderConfAccessor xmlParser = new ReminderConfAccessor(is);
            mTimingArray = xmlParser.getTimingArray();
            mTextArray = xmlParser.getTextArray();
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
    }

    /**
     * Set update type. This is invoked by end user via UI.
     *
     * @param index
     *        Index to the update type user selected.
     */
    public void setUpdateType(int index) {
        mUpdateType = index;
    }

    /**
     * Get the current update type user selected.
     *
     * @return Index to the current update type.
     */
    public int getUpdateType() {
        return mUpdateType;
    }

    /**
     * Set state of FUMO. All the FUMO state observers will be notified with the state transfer. The
     * state will be saved to persistent storage if it's not WAP connection state.
     *
     * @param state
     *        New state.
     * @param operation
     *        DM operation associated with this state transfer.
     * @param extra
     */
    public synchronized void setFumoState(int state, DmOperation operation, Object extra) {
        Log.d(TAG.FUMO, "Set FUMO state to " + state);
        Log.d(TAG.FUMO, "Operation is " + operation);
        Log.d(TAG.FUMO, "Extra is " + extra);
        int previousState = mFumoState;
        mFumoState = state;
        if (!DmFumoState.isWAPState(state)) {
            PersistentContext.getInstance().setFumoState(state);
        }
        // Notify status listeners
        for (IDmFumoStateObserver o : mFumoObservers) {
            Log.d(TAG.FUMO, "Notify observer " + o);
            o.notify(state, previousState, operation, extra);
        }
    }

    /**
     * Get current FUMO state from internal cache.
     *
     * @return Current FUMO state.
     */
    public synchronized int getFumoState() {
        return mFumoState;
    }

    /**
     * Read FUMO state from persistent storage to internal cache and return it.
     *
     * @return FUMO state from persistent storage.
     */
    public synchronized int reloadFumoState() {
        mFumoState = PersistentContext.getInstance().getFumoState();
        return mFumoState;
    }

    /**
     * Register a FUMO state observer.
     *
     * @param observer
     */
    public void registerObserver(IDmFumoStateObserver observer) {
        mFumoObservers.add(observer);
    }

    /**
     * Unregister a FUMO state observer.
     *
     * @param observer
     */
    public void unregisterObserver(IDmFumoStateObserver observer) {
        mFumoObservers.remove(observer);
    }

    private void clearObservers() {
        mFumoObservers.clear();
    }

    /**
     * Register download observer. Client can monitor download progress via an observer.
     *
     * @param observer
     */
    public void registerDownloadObserver(IDmFumoDownloadProgressObserver observer) {
        mFumoDownloadObservers.add(observer);
    }

    /**
     * Unregister download observer.
     *
     * @param observer
     */
    public void unregisterDownloadObserver(IDmFumoDownloadProgressObserver observer) {
        mFumoDownloadObservers.remove(observer);
    }

    public SessionHandler getSessionHandler() {
        return this;
    }

    private void clearDownloadObserver() {
        mFumoDownloadObservers.clear();
    }

    public void clearFumoNotification() {
        if (mNotification != null) {
            Log.i(TAG.FUMO, "Clear Fumo Notification.");
            mNotification.clear();
        }
    }

    public boolean isDownloadComplete() {
        return (DmFumoState.DOWNLOAD_COMPLETE == getFumoState());
    }

    public boolean isDownloadPaused() {
        return (DmFumoState.DOWNLOAD_PAUSED == getFumoState());
    }

    public Map<String, String> generateFumoReportInformation(int resultCode, boolean clearState) {
        Map<String, String> result = null;
        try {
            result = mFumo.generateReportInformation(ResultCode.buildFromInt(resultCode),
                    clearState);
        } catch (MdmException e) {
            throw new Error(e);
        }
        return result;
    }

    public int queryActions() {
        return mFumo.querySessionActions();
    }

    public boolean isFumoInitiator(String initiator) {
        return initiator.startsWith(MdmFumo.SESSION_INITIATOR_PREFIX);
    }

    public DmService getService() {
        return mService;
    }
}