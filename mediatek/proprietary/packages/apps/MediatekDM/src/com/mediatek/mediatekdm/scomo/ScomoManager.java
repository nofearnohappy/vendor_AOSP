package com.mediatek.mediatekdm.scomo;

import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse;
import com.mediatek.mediatekdm.DmOperation.InteractionResponse.InteractionType;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmOperationManager.TriggerResult;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDc;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDcHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDp;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDpHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoResult;
import com.mediatek.mediatekdm.mdm.scomo.ScomoAction;
import com.mediatek.mediatekdm.mdm.scomo.ScomoOperationResult;
import com.mediatek.mediatekdm.pl.DmPLDlPkg;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScomoManager extends SessionHandler implements MdmScomoHandler, MdmScomoDpHandler,
        MdmScomoDcHandler {
    private Handler mHandler = null;
    private HandlerThread mHandlerThread = null;
    private static final int EXIT_HANDER_THREAD = -1;
    private Set<IDmScomoStateObserver> mScomoObservers;
    private Set<IDmScomoDownloadProgressObserver> mScomoDownloadObservers;
    private String mCancelReason = null;
    private DmScomoState mScomoState;
    private DmScomoNotification mScomoNotification;
    private DmOperationManager mOperationManager;
    private MdmScomo mScomo;
    private DmScomoPackageManagerReceiver mPkgMangReceiver = null;
    private DmService mDmService = null;

    static final int DL_MAX_RETRY = 1;
    static final int DL_TIME_OUT = 5 * 60 * 1000; // 5 minutes

    public ScomoManager(DmService service) {
        super(service);
        mScomoState = DmScomoState.load(mService, this);
        mScomoObservers = new HashSet<IDmScomoStateObserver>();
        mScomoDownloadObservers = new HashSet<IDmScomoDownloadProgressObserver>();
        mHandlerThread = new HandlerThread("SCOMO handler thread");
        mHandlerThread.start();
        mOperationManager = DmOperationManager.getInstance();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == EXIT_HANDER_THREAD) {
                    getLooper().quit();
                }
            }
        };
        mScomoNotification = new DmScomoNotification(mService);
        registerObserver(mScomoNotification);
        registerDownloadObserver(mScomoNotification);
        try {
            mScomo = MdmScomo.getInstance(ScomoComponent.ROOT_URI, this);
            mScomo.setAutoAddDPChildNodes(true);
            ArrayList<MdmScomoDp> dpList = mScomo.getDps();
            if (dpList != null) {
                for (MdmScomoDp dp : dpList) {
                    dp.setHandler(this);
                }
            }
            ArrayList<MdmScomoDc> dcList = mScomo.getDcs();
            if (dcList != null) {
                for (MdmScomoDc dc : dcList) {
                    dc.setHandler(this);
                }
            }
            // FIXME This will overwrite the DP loaded when ScomoState is initialized. Is this
            // correct?
            ArrayList<MdmScomoDp> dps = MdmScomo.getInstance(ScomoComponent.ROOT_URI, this)
                    .getDps();
            if (dps != null && dps.size() != 0) {
                mScomoState.currentDp = dps.get(0);
            }
            Log.w(TAG.SCOMO, "Overwrite currentDp with " + mScomoState.currentDp.getName()
                    + "retrieved from engine.");
        } catch (MdmException e) {
            throw new Error(e);
        }
        scomoScanPackage();

        Log.w(TAG.SCOMO, "register for DmScomoPackageManagerReceiver");
        mDmService = service;
        mPkgMangReceiver = new DmScomoPackageManagerReceiver();

        IntentFilter intFilter = new IntentFilter();
        intFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intFilter.addDataScheme("package");
        mDmService.registerReceiver(mPkgMangReceiver, intFilter);
    }

    public void destroy() {
        mHandler.sendEmptyMessage(EXIT_HANDER_THREAD);
        try {
            mScomo.destroy();
        } catch (MdmException e) {
            throw new Error(e);
        }
        mScomo = null;
        mHandler = null;
        mHandlerThread = null;
        Log.w(TAG.SCOMO, "unregister for DmScomoPackageManagerReceiver");
        mDmService.unregisterReceiver(mPkgMangReceiver);
        mPkgMangReceiver = null;
        mDmService = null;
    }

    @Override
    public void newDpAdded(String dpName) {
        Log.i(TAG.SCOMO, "+newDpAdded(" + dpName + ")");
        // Update Current DP
        try {
            MdmScomo scomo = MdmScomo.getInstance(ScomoComponent.ROOT_URI, this);
            mScomoState.currentDp = scomo.createDP(dpName, this);
        } catch (MdmException e) {
            e.printStackTrace();
        }
        DmScomoState.store(mService, mScomoState);
        Log.i(TAG.SCOMO, "-newDpAdded()");
    }

    @Override
    public boolean confirmDownload(final MdmScomoDp dp, final DownloadDescriptor dd) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG.SCOMO, "confirmDownload(" + dp + ", " + dd + ")");
                mScomoState.currentDd = dd;
                mScomoState.currentDp = dp;
                Log.w(TAG.SCOMO, "confirmDownload currentDp name is " + dp.getName());
                mScomoState.currentSize = 0;
                try {
                    mScomoState.totalSize = Integer.parseInt(dd
                            .getField(DownloadDescriptor.Field.SIZE));
                } catch (NumberFormatException e) {
                    mScomoState.totalSize = 0;
                }
                setScomoState(DmScomoState.NEW_DP_FOUND, mOperationManager.current(), null);
                if (!mScomoState.verbose) {
                    Log.d(TAG.SCOMO, "Verbose flag is false, start downloading directly.");
                    startDlPkg();
                }
            }
        });
        return false;
    }

    @Override
    public boolean confirmInstall(final MdmScomoDp dp) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG.SCOMO, "+confirmInstall(" + dp.getName() + ")");

                // set file path & get new version
                String archiveFilePath = "";
                try {
                    archiveFilePath = mService.getFilesDir().getAbsolutePath() + "/"
                            + dp.getDeliveryPkgPath();
                    Log.v(TAG.SCOMO, "scomo archive file path " + archiveFilePath);
                } catch (MdmException e) {
                    e.printStackTrace();
                }

                mScomoState.setArchivePath(archiveFilePath); // side effect: will set pkgInfo
                Log.w(TAG.SCOMO, "confirmInstall currentDp " + dp.getName());

                String newVersion = mScomoState.getVersion();
                Log.w(TAG.SCOMO, "New version is " + newVersion + ".");

                // install directly or postpone
                if (!mScomoState.verbose || getPreVersion() == null) {
                    startInstall();
                    Log.d(TAG.SCOMO, "Verbose flag is false or soft not installed, "
                            + "start install directly.");
                } else {
                    setScomoState(DmScomoState.CONFIRM_INSTALL, null, null);
                }
                Log.d(TAG.SCOMO, "-confirmInstall()");
            }
        });
        return false;
    }

    public String getPreVersion() {
        Log.i(TAG.SCOMO, "getPreVersion.");
        String preVersion = null;
        String pkgName = mScomoState.getPackageName();

        DmScomoPackageManager scomoPm = DmScomoPackageManager.getInstance();
        if (scomoPm.isPackageInstalled(pkgName)) {
            preVersion = scomoPm.getPackageInfo(pkgName).version;
            Log.i(TAG.SCOMO, "Pkg " + pkgName + " installed.");
            Log.i(TAG.SCOMO, "Pre version is " + preVersion);
            return preVersion;
        } else {
            Log.i(TAG.SCOMO, "Pkg" + pkgName + " not installed.");
            return null;
        }
    }

    public void startInstall() {
        Log.i(TAG.SCOMO, "+startInstall.");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mScomoState.currentDp.executeInstall();
                } catch (MdmException e) {
                    e.printStackTrace();
                }
            }
        });

        Log.i(TAG.SCOMO, "-startInstall.");
    }

    public void cancelInstall() {
        Log.i(TAG.SCOMO, "+cancelInstall.");
        final MdmScomoDp lastDp = mScomoState.currentDp;
        setScomoState(DmScomoState.IDLE, null, null);
        clearFiles();
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    if (lastDp != null) {
                        Log.i(TAG.SCOMO, "cancle install: triggerReportSession USER_CANCELED");
                        mOperationManager.finishCurrent();
                        DmOperation reportOperation = new DmOperation();
                        MdmScomoResult result = new MdmScomoResult(MdmScomoResult.USER_CANCELED);
                        reportOperation.initReportScomo(result.val,
                                lastDp.generateReportInformation(result, true));
                        mOperationManager.enqueue(reportOperation, true);
                        mService.getHandler().sendEmptyMessage(
                                IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                    } else {
                        throw new Error("dp is null");
                    }
                } catch (MdmException e) {
                    throw new Error(e);
                }
            }
        });
        Log.i(TAG.SCOMO, "-cancelInstall.");
    }

    public ScomoOperationResult executeInstall(final MdmScomoDp dp, String deliveryPkgPath,
            boolean isActive) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG.SCOMO, "+executeInstall()");
                setScomoState(DmScomoState.INSTALLING, null, null);

                String archiveFilePath = null;
                try {
                    archiveFilePath = mScomoState.archiveFilePath;
                    Log.i(TAG.SCOMO, "archiveFilePath is " + archiveFilePath);

                    File file = new File(archiveFilePath);
                    if (!file.isFile()) {
                        Log.e(TAG.SCOMO, "Error: " + dp.getDeliveryPkgPath() + " not a file.");
                        throw new Error();
                    }
                } catch (MdmException e) {
                    e.printStackTrace();
                    onScomoInstallFailed(dp);
                    return;
                }

                DmScomoPackageManager.getInstance().install(archiveFilePath,
                        new DmScomoPackageManager.ScomoPackageInstallObserver() {
                            public void packageInstalled(final String pkgName, final int status) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(TAG.SCOMO, "Package " + pkgName
                                                + " installed with status: " + status);
                                        if (status == DmScomoPackageManager.STATUS_OK) {
                                            try {
                                                String dpId = dp.getId();
                                                MdmScomo scomo = MdmScomo.getInstance(
                                                        ScomoComponent.ROOT_URI, ScomoManager.this);
                                                MdmScomoDc dc = scomo.createDC(pkgName,
                                                        ScomoManager.this);
                                                dc.deleteFromInventory();
                                                dc.destroy();
                                                dc = scomo.createDC(pkgName, ScomoManager.this);
                                                PackageManager pm = ScomoManager.this.mService
                                                        .getPackageManager();
                                                String versionString = null;
                                                try {
                                                    versionString = pm.getPackageInfo(pkgName, 0).versionName;
                                                } catch (NameNotFoundException e) {
                                                    Log.e(TAG.SCOMO,
                                                            "No package found! Use null as version string");
                                                }
                                                dc.addToInventory(pkgName, pkgName, dpId,
                                                        versionString, null, null, true);
                                                new MdmTree().writeToPersistentStorage();

                                                // Change state & clear file before operation finish
                                                setScomoState(DmScomoState.IDLE, null, "INSTALL_OK");
                                                clearFiles();
                                                mOperationManager.finishCurrent();
                                                // Report to server
                                                DmOperation reportOperation = new DmOperation();
                                                MdmScomoResult result = new MdmScomoResult(1200);
                                                reportOperation.initReportScomo(result.val,
                                                        dp.generateReportInformation(result, true));
                                                mOperationManager.enqueue(reportOperation, true);
                                                mService.getHandler().sendEmptyMessage(
                                                        IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                                            } catch (MdmException e) {
                                                e.printStackTrace();
                                                onScomoInstallFailed(dp);
                                            }
                                        } else {
                                            onScomoInstallFailed(dp);
                                        }
                                    }
                                });
                            }
                        }, true);
            }
        });
        return new ScomoOperationResult();
    }

    /* MdmScomoDcHandler */
    public boolean confirmActivate(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: confirmActivate()");
        return false;
    }

    public boolean confirmDeactivate(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: confirmDeactivate()");
        return false;
    }

    public boolean confirmRemove(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: confirmRemove()");
        return false;
    }

    public ScomoOperationResult executeActivate(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: executeActivate()");
        return new ScomoOperationResult();
    }

    public ScomoOperationResult executeDeactivate(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: executeDeactivate()");
        return null;
    }

    public ScomoOperationResult executeRemove(MdmScomoDc dc) {
        Log.w(TAG.SCOMO, "NOT IMPLEMENTED: executeRemove()");
        return null;
    }

    @Override
    protected void dmComplete() {
        // Catch last operation
        DmOperation operation = mOperationManager.current();
        super.dmComplete();
        // Set verbose flag
        mScomoState.verbose = false;
        LinkedList<InteractionResponse> interactions = operation.dumpInteractionResponses();
        for (InteractionResponse ir : interactions) {
            if (ir.type == InteractionType.CONFIRMATION
                    && ir.response == InteractionResponse.POSITIVE) {
                mScomoState.verbose = true;
            }
        }
        Log.d(TAG.SCOMO, "+setVerbose(" + mScomoState.verbose + ")");
        DmScomoState.store(mService, mScomoState);
        // Read and clear.
        final int scomoAction = operation.getIntProperty(KEY.ACTION_MASK, 0);
        operation.remove(KEY.ACTION_MASK);
        if (Type.isReportOperation(operation.getProperty(KEY.TYPE))) {
            return;
        }
        // switch to SCOMO file for DL session
        DmPLDlPkg.setDeltaFileName(ScomoComponent.SCOMO_FILE_NAME);
        setScomoState(DmScomoState.IDLE, operation, null);
        clearFiles();
        if (mScomoState.currentDp == null) {
            try {
                ArrayList<MdmScomoDp> dps = MdmScomo.getInstance(ScomoComponent.ROOT_URI, this)
                        .getDps();
                if (dps != null && dps.size() != 0) {
                    mScomoState.currentDp = dps.get(0);
                }
            } catch (MdmException e) {
                e.printStackTrace();
            }
            Log.w(TAG.SCOMO, "onDmSessionComplete currentDp " + mScomoState.currentDp.getName());
        }

        if ((scomoAction & ScomoAction.DOWNLOAD_EXECUTED) != 0
                || (scomoAction & ScomoAction.DOWNLOAD_INSTALL_EXECUTED) != 0
                || (scomoAction & ScomoAction.DOWNLOAD_INSTALL_INACTIVE_EXECUTED) != 0) {
            // Reset file state
            clearFiles();
            // Cut in line
            DmOperation dlOperation = new DmOperation(DmOperation.generateId(), DL_TIME_OUT,
                    DL_MAX_RETRY);
            dlOperation.initDLScomo();
            dlOperation.setProperty(KEY.INITIATOR, operation.getProperty(KEY.INITIATOR, "Server"));
            // Engine will trigger DL session later automatically, so we cut in line in
            // operations to match that.
            mOperationManager.triggerNow(dlOperation);
        }
    }

    public void dmAbort(int lastError) {
        // Catch last operation
        DmOperation operation = mOperationManager.current();
        if (!Type.isReportOperation(operation.getProperty(KEY.TYPE))) {
            // If SCOMO DL session is aborted, SCOMO engine will remove pending DL session trigger
            // automatically. So we only need to notify observers.
            if (lastError == MdmError.CANCEL.val) {
                Log.d(TAG.SCOMO, "User canceled. Trigger next operation later.");
                // Set state to IDLE will clear the states
                setScomoState(DmScomoState.IDLE, operation, "DM_USER_CANCELED");
                clearFiles();
            } else if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
                // Set state to IDLE will clear the states
                setScomoState(DmScomoState.IDLE, operation, "DM_FAILED");
                clearFiles();
            } else if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() <= 0) {
                // Set state to IDLE will clear the states
                setScomoState(DmScomoState.IDLE, operation, "DM_NETWORK_ERROR");
                clearFiles();
            }
        }
        // Report session has nothing specific to do.

        // May finish current operation, run at end
        super.dmAbort(lastError);
    }

    @Override
    protected void dlStart() {
        mCancelReason = null;
        DmPLDlPkg.setDeltaFileName(ScomoComponent.SCOMO_FILE_NAME);
        Log.d(TAG.SCOMO, "+dlStart(): state is " + mScomoState.state);
        if (mScomoState.state == DmScomoState.DOWNLOAD_PAUSED
                || mScomoState.state == DmScomoState.DOWNLOADING) {
            setScomoState(DmScomoState.DOWNLOADING, mOperationManager.current(), null);
        } else {
            setScomoState(DmScomoState.DOWNLOADING_STARTED, mOperationManager.current(), null);
        }
    }

    @Override
    protected void dlAbort(int lastError) {
        // Catch last operation
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
                // Do nothing?
                if (mCancelReason != null) {
                    if (mCancelReason.equals("Cancel")) {
                        setScomoState(DmScomoState.DOWNLOAD_PAUSED, operation, "USER_CANCELED");

                        Log.i(TAG.SCOMO, "[dlAbort] User cancel, clear and report to server.");
                        clearDlStateAndReport(0);
                    } else if (mCancelReason.equals("Pause")) {
                        setScomoState(DmScomoState.DOWNLOAD_PAUSED, operation, "USER_PAUSED");
                    }
                } else {
                    Log.e(TAG.SCOMO, "Non-user-triggered cancellation. This is an error!");
                }
            } else if (lastError == MdmError.MO_STORAGE.val) {
                setScomoState(DmScomoState.DOWNLOAD_FAILED, operation, "MO_STORAGE");

                // Report fail and not write operation
                reportDownloadFail(false);
            } else if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
                setScomoState(DmScomoState.DOWNLOAD_FAILED, operation, "FAILED");
                reportDownloadFail(true);
            } else if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() <= 0) {
                setScomoState(DmScomoState.DOWNLOAD_FAILED, operation, "NETWORK_ERROR");

                Log.i(TAG.SCOMO, "[dlAbort] Download fail, report to server.");
                reportDownloadFail(true);
            }
            mOperationManager.finishCurrent();
        }
    }

    @Override
    public void dlComplete() {
        // Override super.dlComplete() to finish current operation later (after installation is
        // finished).
    }

    /**
     * Clear SCOMO DL files.
     */
    private void clearFiles() {
        Log.d(TAG.SCOMO, "+clearFiles()");
        mService.deleteFile(ScomoComponent.SCOMO_FILE_NAME);
        mService.deleteFile(ScomoComponent.SCOMO_RESUME_FILE_NAME);
        Log.d(TAG.SCOMO, "-clearFiles()");
    }

    public void clearDlStateAndReport(int reportDelay) {
        final MdmScomoDp lastDp = mScomoState.currentDp;
        setScomoState(DmScomoState.IDLE, null, null);
        clearFiles();
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    if (lastDp != null) {
                        Log.i(TAG.SCOMO, "cancelDlScomoPkg: triggerReportSession USER_CANCELED");
                        DmOperation reportOperation = new DmOperation();
                        MdmScomoResult result = new MdmScomoResult(MdmScomoResult.USER_CANCELED);
                        reportOperation.initReportScomo(result.val,
                                lastDp.generateReportInformation(result, true));
                        mOperationManager.enqueue(reportOperation, true);
                        mService.getHandler().sendEmptyMessage(
                                IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
                    } else {
                        throw new Error("dp is null");
                    }
                } catch (MdmException e) {
                    throw new Error(e);
                }
                Log.d(TAG.SCOMO, "scomo dl canceled, delete delta package");
            }
        });
    }

    private void reportDownloadFail(boolean writeToFS) {
        Log.d(TAG.SCOMO, "+reportDownloadFai.");
        final MdmScomoDp lastDp = mScomoState.currentDp;
        try {
            if (lastDp != null) {
                Log.i(TAG.SCOMO, "ReportFailure: triggerReportSession DOWNLOAD_FAILED");
                DmOperation reportOperation = new DmOperation();
                MdmScomoResult result = new MdmScomoResult(MdmScomoResult.DOWNLOAD_FAILED);
                reportOperation.initReportScomo(result.val,
                        lastDp.generateReportInformation(result, true));
                mOperationManager.enqueue(reportOperation, writeToFS);
                mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            } else {
                throw new Error("dp is null");
            }
        } catch (MdmException e) {
            throw new Error(e);
        }
        Log.d(TAG.SCOMO, "-reportDownloadFai.");
    }

    private void onScomoInstallFailed(MdmScomoDp dp) {
        setScomoState(DmScomoState.IDLE, null, "INSTALL_FAILED");
        clearFiles();
        try {
            mOperationManager.finishCurrent();
            DmOperation reportOperation = new DmOperation();
            MdmScomoResult result = new MdmScomoResult(500);
            reportOperation.initReportScomo(result.val, dp.generateReportInformation(result, true));
            mOperationManager.enqueue(reportOperation, true);
            mService.getHandler().sendEmptyMessage(IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    public void registerObserver(IDmScomoStateObserver observer) {
        Log.d(TAG.SCOMO, "Register listener " + observer);
        synchronized (mScomoObservers) {
            mScomoObservers.add(observer);
        }
    }

    public void unregisterObserver(IDmScomoStateObserver observer) {
        Log.d(TAG.SCOMO, "Unregister listener " + observer);
        synchronized (mScomoObservers) {
            mScomoObservers.remove(observer);
        }
    }

    public void registerDownloadObserver(IDmScomoDownloadProgressObserver observer) {
        mScomoDownloadObservers.add(observer);
    }

    public void unregisterDownloadObserver(IDmScomoDownloadProgressObserver observer) {
        mScomoDownloadObservers.remove(observer);
    }

    public void startDlPkg() {
        Log.d(TAG.SCOMO, "Invoke startDlPkg()");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG.SCOMO, "+startDlPkg()");
                mService.getController().proceedDLSession();
                // This invocation will save state to persistent storage
                setScomoState(DmScomoState.DOWNLOADING, mOperationManager.current(), null);
                Log.d(TAG.SCOMO, "-startDlPkg()");
            }
        });
    }

    public void cancelDlPkg() {
        Log.d(TAG.SCOMO, "+cancelDlPkg()");
        int state = mScomoState.state;
        if (state == DmScomoState.NEW_DP_FOUND || state == DmScomoState.DOWNLOADING) {
            mCancelReason = "Cancel";
            mService.getController().cancelSession();
            // The state will be set in dlAbort
        } else {
            Log.e(TAG.SCOMO, "Cancel requested in invalid state " + state + ", ignored.");
        }

        if (mOperationManager.isInRecovery()) {
            DmOperation operation = mOperationManager.current();
            if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                    && operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                setScomoState(DmScomoState.DOWNLOAD_PAUSED, mOperationManager.current(),
                        "USER_CANCELED");
                mOperationManager.finishCurrent();
            } else {
                Log.e(TAG.SCOMO, "In recover but not Scomo DL, invalid invoke");
            }
        }
        Log.d(TAG.SCOMO, "-cancelDlPkg()");
    }

    public void pauseDlPkg() {
        Log.d(TAG.SCOMO, "+pauseDlPkg()");
        int state = mScomoState.state;
        if (state == DmScomoState.NEW_DP_FOUND || state == DmScomoState.DOWNLOADING) {
            mCancelReason = "Pause";
            mService.getController().cancelSession();
            // The state will be set in dlAbort
        } else {
            Log.e(TAG.SCOMO, "Pause requested in invalid state " + state + ", ignored.");
        }

        if (mOperationManager.isInRecovery()) {
            DmOperation operation = mOperationManager.current();
            if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                    && operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                setScomoState(DmScomoState.DOWNLOAD_PAUSED, mOperationManager.current(),
                        "USER_CANCELED");
                mOperationManager.finishCurrent();
            } else {
                Log.e(TAG.SCOMO, "In recover but not Scomo DL, invalid invoke");
            }
        }
        Log.d(TAG.SCOMO, "-pauseDlPkg()");
    }

    public void resumeDlPkg() {
        Log.d(TAG.SCOMO, "+resumeDlPkg()");

        if (mScomoState.state != DmScomoState.DOWNLOAD_PAUSED) {
            Log.e(TAG.SCOMO, "resumeDlPkg is invoked in invalid state");
            return;
        }

        DmOperation operation = new DmOperation(DmOperation.generateId(), DL_TIME_OUT, DL_MAX_RETRY);
        operation.initDLScomo();
        if (mOperationManager.triggerNow(operation) == TriggerResult.SUCCESS) {
            try {
                mScomoState.currentDp.resumeDLSession();
                Log.d(TAG.SCOMO, "resumDlScomoPkg end");
            } catch (MdmException e) {
                Log.e(TAG.SCOMO, "resumDlScomoPkg exception " + e);
                e.printStackTrace();
            }
        } else {
            Log.e(TAG.SCOMO, "DL operation trigger failed!");
        }
        Log.d(TAG.SCOMO, "-resumeDlPkg()");
    }

    public void recoverDlPkg() {
        try {
            Log.d(TAG.SCOMO, "Current DP is " + mScomoState.currentDp);
            mScomoState.currentDp.resumeDLSession();
            Log.d(TAG.SCOMO, "resumDlScomoPkg end");
        } catch (MdmException e) {
            Log.e(TAG.SCOMO, "resumDlScomoPkg exception " + e);
            e.printStackTrace();
        }
    }

    public void scomoScanPackage() {
        Log.d(TAG.SCOMO, "+scomoScanPackage()");
        mHandler.post(new Runnable() {
            public void run() {
                scomoScanPackageInternal();
            }
        });
        Log.d(TAG.SCOMO, "-scomoScanPackage()");
    }

    private void scomoScanPackageInternal() {
        Log.d(TAG.SCOMO, "+scomoScanPackageInternal()");
        try {
            PackageManager pm = mService.getPackageManager();
            MdmScomo scomoInstance = MdmScomo.getInstance(ScomoComponent.ROOT_URI, null);
            List<ApplicationInfo> installedList = pm.getInstalledApplications(0);
            List<MdmScomoDc> dcs = scomoInstance.getDcs();
            Set<String> dcsNames = new HashSet<String>();
            for (MdmScomoDc dc : dcs) {
                dcsNames.add(dc.getName());
            }
            Set<String> appNames = new HashSet<String>();
            Map<String, String> appVersions = new HashMap<String, String>();
            for (ApplicationInfo appInfo : installedList) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    continue;
                }
                appNames.add(appInfo.packageName);
                String versionString = "";
                try {
                    versionString = pm.getPackageInfo(appInfo.packageName, 0).versionName;
                } catch (NameNotFoundException e) {
                    // should not happen
                    e.printStackTrace();
                }
                appVersions.put(appInfo.packageName, versionString);
            }

            Set<String> dcsNamesTmp = new HashSet<String>(dcsNames);
            Set<String> appNamesTmp = new HashSet<String>(appNames);

            dcsNamesTmp.removeAll(appNames); // dcsNamesTmp now contains pkg
                                             // need to be removed from dcs
            appNamesTmp.removeAll(dcsNames); // appNamesTMp now contains pkg
                                             // need to be added to dcs

            for (String pkgName : dcsNamesTmp) {
                Log.i(TAG.SCOMO, "scanPackage: remove " + pkgName);
                MdmScomoDc dc = scomoInstance.createDC(pkgName, this);
                dc.deleteFromInventory();
                dc.destroy();
            }
            for (String pkgName : appNamesTmp) {
                Log.i(TAG.SCOMO, "scanPackage: add " + pkgName);
                MdmScomoDc dc = scomoInstance.createDC(pkgName, this);
                dc.addToInventory(pkgName, pkgName, null, appVersions.get(pkgName), null, null,
                        true);
            }
            new MdmTree().writeToPersistentStorage();
        } catch (MdmException e) {
            e.printStackTrace();
        }
        Log.d(TAG.SCOMO, "-scomoScanPackageInternal()");
    }

    public DmScomoState getScomoState() {
        return mScomoState;
    }

    public void setScomoState(final int state, final DmOperation operation, final Object extra) {
        Log.d(TAG.SCOMO, "Set SCOMO state to " + state);
        Log.d(TAG.SCOMO, "Operation is " + operation);
        Log.d(TAG.SCOMO, "Extra is " + extra);
        final int previousState = mScomoState.state;
        mScomoState.state = state;
        DmScomoState.store(mService, mScomoState);
        Log.d(TAG.SCOMO, "Listener count is " + mScomoObservers.size());
        if (mScomoState.verbose) {
            for (final IDmScomoStateObserver listener : mScomoObservers) {
                Log.w(TAG.SCOMO, "Notify SCOMO listener " + listener);
                mService.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.notify(state, previousState, operation, extra);
                    }
                });
            }
        }
        Log.d(TAG.SCOMO, "Notification sent.");
    }

    public void updateDownloadProgress(final long current, final long total) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mScomoState.currentSize = current;
                mScomoState.totalSize = total;

                // current size change, write to file
                DmScomoState.store(mService, mScomoState);
                if (mScomoState.verbose) {
                    Log.d(TAG.SCOMO, "Update progress with state " + mScomoState.state);
                    for (final IDmScomoDownloadProgressObserver o : mScomoDownloadObservers) {
                        mService.getHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                o.updateProgress(current, total);
                            }
                        });
                    }
                } else {
                    Log.d(TAG.SCOMO, "Verbose flag is not set. Do not update progress in state "
                            + mScomoState.state);
                }
            }
        });
    }

    public void reportResult(DmOperation operation) {
        if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_SCOMO)) {
            String account = operation.getProperty("report_account");
            if (account.equals("")) {
                account = null;
            }

            mService.getController().triggerReportSession(
                    operation.getProperty("report_correlator"),
                    operation.getProperty("report_format"), operation.getProperty("report_type"),
                    operation.getProperty("report_mark"), operation.getProperty("report_source"),
                    operation.getProperty("report_target"), operation.getProperty("report_data"),
                    account, new SimpleSessionInitiator(operation.getProperty("report_initiator")));
        } else {
            throw new Error("Current operation is not SCOMO report!");
        }
    }

    public SessionHandler getSessionHandler() {
        return this;
    }

    public int queryActions() {
        return mScomo.querySessionActions();
    }

    public boolean isScomoInitiator(String initiator) {
        return initiator.startsWith(MdmScomo.Initiator.PREFIX);
    }

}
