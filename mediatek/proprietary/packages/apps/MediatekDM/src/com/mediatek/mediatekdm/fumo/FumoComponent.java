package com.mediatek.mediatekdm.fumo;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.IntentAction;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.KickoffActor;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FumoComponent implements IDmComponent {
    class FumoBinder extends DmService.DmBinder {
        public FumoBinder() {
            super(mService);
        }

        public FumoManager getManager() {
            return mManager;
        }
    }

    static final String BIND_FUMO = "com.mediatek.mediatekdm.BIND_FUMO";
    static final String BOOT_TIME_FILE_NAME = "bootTimeStamp.ini";
    static final String DELTA_FILE = "delta.zip";
    static final String FOTA_FLAG_FILE = "fota_executing";
    static final String FUMO_FILE_NAME = "delta.zip";
    static final String FUMO_RESUME_FILE_NAME = "dlresume.dat";
    static final String NAME = "FUMO";
    static final String REMINDER_CONFIG_FILE = "reminder.xml";
    static final String RESUME_FILE = "dlresume.dat";
    static final String ROOT_URI = "./FwUpdate";
    static final String TIME_STAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss";
    static final String UPDATE_REBOOT_CHECK = "com.mediatek.mediatekdm.UPDATE_REBOOT_CHECK";

    static String getTimeStamp(Context context, String filename) {
        String result = null;
        FileInputStream in = null;
        try {
            final String stampFilePath = PlatformManager.getInstance().getPathInData(context,
                    filename);
            File stampFile = new File(stampFilePath);

            if (stampFile.exists()) {
                Log.d(TAG.COMMON, "Boot file exists.");
                // read boot time, then write into file
                in = new FileInputStream(stampFilePath);
                byte[] buf = new byte[in.available()];
                in.read(buf);
                result = new String(buf);
            }
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return result;
    }

    static void saveTimeStamp(Context context, Date date, String filename) {
        FileOutputStream out = null;
        try {
            String timeBoot = new SimpleDateFormat(TIME_STAMP_FORMAT).format(date);
            Log.d(TAG.RECEIVER, "Device boot time is " + timeBoot);

            out = context.openFileOutput(filename, Context.MODE_PRIVATE);
            out.write(timeBoot.getBytes());
            out.flush();
            out.close();

            Log.d(TAG.RECEIVER, "Write boot time " + timeBoot + " to file " + filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG.RECEIVER, "Not found time stamp file: " + e);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.RECEIVER, "Failed to write time stamp file: " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                Log.e(TAG.RECEIVER, e.getMessage());
            }
        }
    }

    private FumoBinder mBinder = null;
    private FumoManager mManager = null;
    private DmService mService = null;

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        if (initiator != null && mManager.isFumoInitiator(initiator.getId())) {
            return true;
        } else if (operation != null
                && (operation.getProperty(KEY.TYPE).contains("FUMO") || operation
                        .getBooleanProperty(KEY.FUMO_TAG, false))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void attach(DmService service) {
        mService = service;
        mManager = new FumoManager(service);
        mBinder = new FumoBinder();
    }

    @Override
    public void configureDmTree(MdmTree tree) {
    }

    @Override
    public void detach(DmService service) {
        mManager.destroy();
        mBinder = null;
        mService = null;
        mManager = null;
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        Log.d(TAG.FUMO, "dispatchBroadcast" + intent.getAction());
        if (intent.getAction().equals(DmConst.IntentAction.DM_BOOT_COMPLETED)) {
            FumoComponent.saveTimeStamp(context, new Date(), BOOT_TIME_FILE_NAME);
            return DispatchResult.ACCEPT;
        } else if (intent.getAction().equals(DmConst.IntentAction.DM_SWUPDATE)) {
            // User clicked update in system settings preference.
            Log.i(TAG.FUMO, "Launch system update UI.");
            Intent activityIntent = new Intent(context, DmEntry.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
            return DispatchResult.ACCEPT;
        } else {
            return DispatchResult.IGNORE;
        }
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        String action = intent.getAction();
        if (action.equals(FumoComponent.UPDATE_REBOOT_CHECK)) {
            Log.d(TAG.FUMO, "Reboot check");
            if (intent.hasExtra("UpdateSucceeded")) {
                boolean succeeded = intent.getBooleanExtra("UpdateSucceeded", true);
                DmOperation operation = new DmOperation();
                int result = succeeded ? FumoManager.RESULT_SUCCESSFUL
                        : FumoManager.RESULT_UPDATE_FAILED;
                operation.initReportFumo(result,
                        mManager.generateFumoReportInformation(result, true));
                DmOperationManager.getInstance().enqueue(operation, true);
                File updateFile = new File(PlatformManager.getInstance().getPathInData(mService,
                        FumoComponent.FOTA_FLAG_FILE));
                if (updateFile.exists()) {
                    Log.d(TAG.SERVICE, "Delete FUMO update flag file.");
                    updateFile.delete();
                }
                return DispatchResult.ACCEPT_AND_TRIGGER;
            } else {
                Log.w(TAG.FUMO, "No update result found!");
                return DispatchResult.ACCEPT;
            }
        } else if (action.equals(IntentAction.DM_FUMO_REMINDER)) {
            Log.d(TAG.FUMO, "FUMO update reminder timeout, start DmClient");
            if (!mManager.isDownloadComplete()) {
                Log.w(TAG.SERVICE, "DL state is not STATE_DLPKGCOMPLETE, do nothing");
            } else {
                Intent activityIntent = new Intent(mService, DmClient.class);
                activityIntent.setAction("com.mediatek.mediatekdm.DMCLIENT");
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mService.startActivity(activityIntent);
            }
            // DmClient should activate network by itself.
            return DispatchResult.ACCEPT;
        } else if (action.equals(IntentAction.DM_DL_FOREGROUND)) {
            Log.i(TAG.FUMO, "Bring DmClient to foreground");
            Intent activityIntent = new Intent(mService, DmClient.class);
            activityIntent.setAction("com.mediatek.mediatekdm.DMCLIENT");
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(activityIntent);
            // Remain alive as we may be bound by DmClient in near future.
            return DispatchResult.ACCEPT;
        } else {
            return DispatchResult.IGNORE;
        }
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
        if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_CI_FUMO)
                || operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
            mManager.updateDownloadProgress(current, total);
        }
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        switch (action) {
            case NEW:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_FUMO)) {
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_CI_FUMO)) {
                    mManager.queryNewVersion();
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            case RECOVER:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_FUMO)) {
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_CI_FUMO)) {
                    mManager.retryQueryNewVersion();
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                        && operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
                    mManager.recoverDlPkg();
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            case RETRY:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_FUMO)) {
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_CI_FUMO)) {
                    mManager.retryQueryNewVersion();
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                        && operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
                    mManager.recoverDlPkg();
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            default:
                return DispatchResult.IGNORE;
        }
    }

    @Override
    public SessionHandler dispatchSessionStateChange(SessionType type, SessionState state,
            int lastError, SessionInitiator initiator, DmOperation operation) {
        if (acceptOperation(initiator, operation) || mManager.queryActions() != 0) {
            Log.d(TAG.FUMO, "FUMO session");
            operation.setProperty(KEY.FUMO_TAG, true);
            operation.setProperty(KEY.ACTION_MASK, mManager.queryActions());
            return mManager;
        } else {
            return null;
        }
    }

    @Override
    public boolean forceSilentMode() {
        return false;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        final String action = intent.getAction();
        if (action != null && action.equals(BIND_FUMO)) {
            return mBinder;
        } else {
            return null;
        }
    }

    @Override
    public String getDlPackageFilename() {
        return FUMO_FILE_NAME;
    }

    @Override
    public String getDlResumeFilename() {
        return FUMO_RESUME_FILE_NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void kickoff(Context context) {
        KickoffActor.kickoff(new UpdateRebootChecker(context));
    }

    FumoManager getFumoManager() {
        return mManager;
    }

    @Override
    public DispatchResult validateWapPushMessage(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public boolean checkPrerequisites() {
        return true;
    }
}
