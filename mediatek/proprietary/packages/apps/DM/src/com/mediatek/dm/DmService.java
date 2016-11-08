/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.dm.bootstrap.OmacpParser;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.bootstrap.DmBootstrapHandler;
import com.mediatek.dm.bootstrap.DmCpObserver;
import com.mediatek.dm.conn.DmDataConnection;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.ext.MTKFileUtil;
import com.mediatek.dm.ext.MTKOptions;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.fumo.DmClient;
import com.mediatek.dm.fumo.FOTADeltaFiles;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.scomo.DmPLInventory;
import com.mediatek.dm.scomo.DmScomoDcHandler;
import com.mediatek.dm.scomo.DmScomoDpHandler;
import com.mediatek.dm.scomo.DmScomoHandler;
import com.mediatek.dm.scomo.DmScomoNotification;
import com.mediatek.dm.scomo.DmScomoPackageManager;
import com.mediatek.dm.scomo.DmScomoState;
import com.mediatek.dm.scomo.OnDmScomoUpdateListener;
import com.mediatek.dm.session.DmSessionStateObserver.DmAction;
import com.mediatek.dm.util.DmThreadPool;
import com.mediatek.dm.xml.DmXMLParser;
import com.redbend.android.RbException.VdmError;
import com.redbend.vdm.BootProfile;
import com.redbend.vdm.CpSecurity;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.NIAMsgHandler.UIMode;
import com.redbend.vdm.SessionInitiator;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;
import com.redbend.vdm.fumo.FumoAction;
import com.redbend.vdm.fumo.FumoState;
import com.redbend.vdm.fumo.VdmFumo;
import com.redbend.vdm.fumo.FumoResultCode;
import com.redbend.vdm.lawmo.LawmoAction;
import com.redbend.vdm.scomo.ScomoAction;
import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDc;
import com.redbend.vdm.scomo.VdmScomoDp;
import com.redbend.vdm.scomo.VdmScomoResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

import org.w3c.dom.Node;

public class DmService extends Service {
    public static final int SESSION_TYPE_NONE = -1;
    public static final int SESSION_TYPE_FUMO = 0;
    public static final int SESSION_TYPE_SCOMO = 1;
    public static final int SESSION_TYPE_LAWMO = 2;
    public static final int SESSION_TYPE_BOOTSTRAP = 3;

    public static int sSessionType = SESSION_TYPE_NONE;

    private static DmService sServiceInstance;
    private boolean mNeedCloseNetwork;
    private final IBinder mBinder = new DmBinder();
    private ExecutorService mExec;
    private DmController mDmController;
    public DmDownloadNotification mDmDownloadNotification;

    private AlarmManager mAlarmManager;
    private PendingIntent mNiaOperation;
    private static final long ONESECOND = 1000;
    private static final long ONEMINUTE = ONESECOND * 60;
    private byte[] mNiaMessage;

    // added for resume NIA session when session aborted due to
    // HTTP_SOCKET_ERROR
    private byte[] mMessageToResume;
    public static Map<String, String> sCCStoredParams = new HashMap<String, String>();
    private boolean mNiaNotificationShown;
    // end added

    // Add for process multi-sessions,
    private static final int MAXNIAQUEUESIZE = 3;
    private ArrayBlockingQueue<NiaInfo> mNiaQueue =
            new ArrayBlockingQueue<NiaInfo>(MAXNIAQUEUESIZE);
    private String mCurrentNiaMsgName;

    public static final String INTENT_EXTRA_UPDATE = "update";
    public static final String INTENT_EXTRA_POLLING = "polling";
    public static final String INTENT_EXTRA_NIA = "nia";
    private static final String INTENT_EXTRA_NIA_ALERT = "nia_alert";
    private static final String INTENT_EXTRA_UI_INTERACT = "ui_interact";

    // All time out are defined by CMCC spec. in dm session, MAXDT=30
    private static final int TIMEOUT_ALERT_1101 = 30;
    private static final int TIMEOUT_UI_VISIBLE = 10;
    private static final int TIMEOUT_UI_INTERACT = 10 * 60;
    /**
     * Used for received WAP_PUSH before data connection ready
     */
    private static Intent sReceivedIntent;
    /**
     * Used for Lawmo Factory reset, return result to Server, then implement Factory reset if
     * mFakeLawmoAction equals LawmoAction.FACTORY_RESET_EXECUTED when Session Completed
     */
    public static int sFakeLawmoAction;

    private int mSessionInitor = IDmPersistentValues.SERVER;
    private int mDlStatus;
    public Handler mSessionStateHandler;
    private FotaSessionStateThread mFotaThread;
    // for FOTA reminder
    private PendingIntent mReminderPendingIntent;
    private int[] mTimingArray;
    private String[] mTextArray;
    private static final int UPDATENOW = 0;
    private static final int NEVERASK = 0x1ffff;
    // end for FOTA reminder

    // for scomo
    private int mDownloadingNotificationCount;
    private List<OnDmScomoUpdateListener> mScomoUpdateListeners =
            new ArrayList<OnDmScomoUpdateListener>();
    public static boolean sIsScomoSession;
    private static boolean sIsNeedScanPkgList = true;

    // end scomo

    // For bootstrap
    private boolean waitNetworkForBootstrap;

    /**
     * Get the reference of dm service instance.
     *
     * @return The reference of dm service instance
     */
    public static DmService getInstance() {
        return sServiceInstance;
    }

    /**
     * Override function of android.app.Service, initiate vdm controls.
     */
    public void onCreate() {
        Log.d(TAG.SERVICE, "On create service");
        super.onCreate();
        isDmTreeReady();

        if (mDmDownloadNotification == null) {
            Log.e(TAG.SERVICE, "new DmDownloadNotification");
            mDmDownloadNotification = new DmDownloadNotification(this);
        }

        if (sServiceInstance == null) {
            sServiceInstance = this;
        }

        DmDataConnection.getInstance(this);
        mDlStatus = PersistentContext.getInstance(this).getDLSessionStatus();
        if (mFumoObserver != null) {
            Log.d(TAG.SERVICE, "register FUMO Listener");
            PersistentContext.getInstance(this).registerObserver(mFumoObserver);
        }
        Log.i(TAG.SERVICE, "On create service done");
    }

    /**
     * Override function of android.app.Service, handle three types of intents: 1. dm wap push 2.
     * boot complete if system upgrades 3. download foreground
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            Log.w(TAG.SERVICE, "onStartCommand intent is null");
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG.SERVICE, "onStartCommand action is null");
            return START_NOT_STICKY;
        }

        if (Options.USE_SMS_REGISTER) {
            int registerSubId = DmCommonFun.getRegisterSubID(this);
            if (registerSubId == -1) {
                Log.w(TAG.SERVICE, "The sim card is not register to DM server");
                return START_NOT_STICKY;
            } else if (DmConst.IntentAction.DM_WAP_PUSH.equals(action)) {
                int receivedSubId = intent.getIntExtra(MTKPhone.SUBSCRIPTION_KEY, -1);
                if (receivedSubId != registerSubId) {
                    Log.w(TAG.SERVICE,
                            new StringBuilder(
                                    "The sim card is not register to DM server, receivedSubId = ")
                                    .append(receivedSubId).append(",register subscription id = ")
                                    .append(registerSubId).toString());
                    return START_NOT_STICKY;
                }
            }

            if (DmConst.IntentAction.DM_WAP_PUSH.equals(action)) {
                openMobileNetWork();
            }
        }

        Log.i(TAG.SERVICE, new StringBuilder("Received start id ").append(startId).append(" : ")
                .append(intent).append(" action is ").append(action).toString());

        writeNiaMessage(intent);

        if (!Options.USE_DIRECT_INTERNET) {
            int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
            Log.i(TAG.SERVICE, "starting DM WAP conn...ret=" + result);

            if (result == MTKPhone.NETWORK_AVAILABLE) {
                Log.i(TAG.SERVICE, "handling intent as WAP is ready.");
                handleStartEvent(intent);
            } else {
                Log.i(TAG.SERVICE, "saving intent as WAP is not ready yet.");
                Bundle data = intent.getExtras();
                if (data != null
                        && (data.getBoolean(INTENT_EXTRA_UPDATE) || data
                                .getBoolean(INTENT_EXTRA_NIA))) {
                    Log.v(TAG.SERVICE, "[fota]saving reboot-update intent...");
                    sReceivedIntent = intent;
                } else if (action.equalsIgnoreCase(DmConst.IntentAction.DM_WAP_PUSH)) {
                    Log.v(TAG.SERVICE, "[NIA]saving WAP_PUSH intent...");
                    sReceivedIntent = intent;
                } else if (action.equals(DmConst.IntentAction.DM_REMINDER)) {
                    Log.v(TAG.SERVICE, "[fota]saving reminder intent...");
                    sReceivedIntent = intent;
                }
            }
        } else {
            Log.i(TAG.SERVICE, "starting intent handling when using internet.");
            handleStartEvent(intent);
        }

        return START_STICKY;
    }

    private void handleStartEvent(Intent intent) {
        Log.i(TAG.SERVICE, "handleStartEvent");
        if (mDmController == null) {
            initDmController();
        }

        if (intent == null) {
            intent = sReceivedIntent;
            sReceivedIntent = null;
        }
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG.SERVICE, "handleStatrtEvent receivedIntent is null");
            return;
        }
        String action = intent.getAction();
        if (action.equals(DmConst.IntentAction.DM_WAP_PUSH)) {
            // NIA WAP push received.
            Log.i(TAG.SERVICE, "Receive NIA wap push intent");
            String type = intent.getType();
            byte[] message = intent.getByteArrayExtra("data");

            if (DmConst.IntentType.DM_NIA.equals(type)) {
                Log.w(TAG.SERVICE, "receive DM_NIA message");
                if (message != null) {
                    receivedNiaMessage(message);
                }

            } else if (DmConst.IntentType.BOOTSTRAP_NIA.equals(type)
                    || DmConst.IntentType.BOOTSTRAP_CP.equals(type)) {

                BootProfile profile = BootProfile.WAP;
                if (DmConst.IntentType.BOOTSTRAP_NIA.equals(type)) {
                    profile = BootProfile.PLAIN;
                } else {
                    boolean dmBootstrap = OmacpParser.isDmCpBootstrap(message);
                    if (!dmBootstrap) {
                        return;
                    }
                }

                HashMap<String, String> contentTypeParameter = (HashMap<String, String>) intent
                        .getSerializableExtra("contentTypeParameters");
                String mac = null;
                String sec = null;
                if (contentTypeParameter != null) {
                    sec = contentTypeParameter.get("SEC");
                    mac = contentTypeParameter.get("MAC");
                }

                Log.d(TAG.SERVICE, new StringBuilder("mine_type: ").append(type).append(",sec: ")
                        .append(sec).append(", mac: ").append(mac).toString());

                CpSecurity security = CpSecurity.NONE;
                if ("".equals(sec) || "0".equals(sec)) {
                    security = CpSecurity.NETWPIN;
                } else if ("1".equals(sec)) {
                    security = CpSecurity.USERPIN;
                } else if ("2".equals(sec)) {
                    security = CpSecurity.USERNETWPIN;
                } else if ("3".equals(sec)) {
                    security = CpSecurity.USERPINMAC;
                }

                triggerBootstrapSession(profile, security, mac, message);
            }
        } else if (action.equals(DmConst.IntentAction.DM_DL_FOREGROUND)
                || action.equals(DmConst.IntentAction.DM_REMINDER)) {
            Log.i(TAG.SERVICE, "Receive show dm client intent");
            int state = PersistentContext.getInstance(this).getDLSessionStatus();
            if (action.equals(DmConst.IntentAction.DM_REMINDER)
                    && state != IDmPersistentValues.STATE_DL_PKG_COMPLETE) {
                Log.w(TAG.SERVICE, "[DM_REMINDER]the dl state is not STATE_DLPKGCOMPLETE, it = "
                        + state);
                return;
            }
            Intent activityIntent = new Intent(this, DmClient.class);
            if (activityIntent != null) {
                activityIntent.setAction(DmConst.IntentAction.DM_CLIENT);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
            }
        } else if (action.equalsIgnoreCase(DmConst.IntentAction.ACTION_REBOOT_CHECK)) {
            Log.i(TAG.SERVICE, "received DM reboot check intent.");
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            if (extras.getBoolean(INTENT_EXTRA_UPDATE)) {
                Log.i(TAG.SERVICE, "[reboot-state]=>reboot from update.");

                PersistentContext.getInstance(this).deleteDeltaPackage();
                boolean isUpdateSuccessfull = checkUpdateResult();
                // Intent activityIntent = new Intent(this, DmReport.class);
                // activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // activityIntent.putExtra(DmReport.EXTRA_RESULT, isUpdateSuccessfull);
                // startActivity(activityIntent);
                reportState(isUpdateSuccessfull);
            }

            if (mNiaQueue != null && mNiaQueue.size() <= 0 && extras.getBoolean(INTENT_EXTRA_NIA)) {
                Log.i(TAG.SERVICE, "[reboot-state]=>has pending NIA.");
                NiaMsgReader rnia = new NiaMsgReader();
                if (rnia != null) {
                    if (mExec == null) {
                        mExec = DmThreadPool.getInstance();
                    }

                    if (mExec != null) {
                        mExec.execute(rnia);
                    }

                }
            }
        } else if (action.equals(DmConst.IntentAction.DM_SWUPDATE)) {
            Log.i(TAG.SERVICE, "Receive software update intent");
        } else if (action.equals(DmConst.IntentAction.DM_NIA_START)) {
            Log.w(TAG.SERVICE, "action is DM_NIA_START");
            cancleNiaAlarm();
            sendBroadcast(new Intent(DmConst.IntentAction.DM_CLOSE_DIALOG));
            if (mDmDownloadNotification != null) {
                mDmDownloadNotification.clearDownloadNotification();
            }

            if (intent.getBooleanExtra(INTENT_EXTRA_NIA_ALERT, false)
                    || intent.getBooleanExtra(INTENT_EXTRA_UI_INTERACT, false)) {
                cancleDmSession();
            } else {
                proceedNiaMessage();
            }
        } else if (action.equals(DmConst.IntentAction.ACTION_FUMO_CI)) {
            // background querying for firmware update.
            Log.i(TAG.SERVICE, "------- fumo ci request start ------");
            setSessionInitor(IDmPersistentValues.CLIENT_POLLING);
            queryNewVersion(VdmFumo.ClientType.DEVICE);
            Log.i(TAG.SERVICE, "------- fumo ci session triggered ------");

        } else {
            Log.i(TAG.SERVICE, "Receive other intent!");
        }
    }

    public void initDmController() {
        mDmController = new DmController(this);

        if (Options.SCOMO_SUPPORT) {
            initAndNotifyScomo();
        }
    }

    public boolean isInitDmController() {
        return (mDmController != null);
    }

    /**
     * Override function of android.app.Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG.SERVICE, "On bind service");
        return mBinder;
    }

    /**
     * Override function of android.app.Binder
     */
    public void onRebind(Intent intent) {
        Log.i(TAG.SERVICE, "On rebind service");
        super.onRebind(intent);
    }

    /**
     * Override function of android.app.Binder
     */
    public boolean onUnbind(Intent intent) {
        Log.i(TAG.SERVICE, "On unbind service");
        return super.onUnbind(intent);
    }

    /**
     * Override function of android.app.Service
     */
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG.SERVICE, "On destroy service");
        if (mDmController != null) {
            mDmController.stop();
            mDmController = null;
        }

        if (!Options.USE_DIRECT_INTERNET) {
            DmDataConnection.getInstance(this).stopDmDataConnectivity();
            closeMobileNetWork();
        }

        DmDataConnection.destroyInstance();

        if (mFumoObserver != null) {
            Log.d(TAG.SERVICE, "clear FUMO Listener");
            PersistentContext.getInstance(this).clearObserver();
        }

        sServiceInstance = null;
        mAlarmManager = null;
        sReceivedIntent = null;
        if (mDmDownloadNotification != null) {
            mDmDownloadNotification.clearDownloadNotification();
            mDmDownloadNotification = null;
        }

        if (mNiaQueue != null) {
            mNiaQueue.clear();
            mNiaQueue = null;
        }
    }

    // start for scomo
    public void initAndNotifyScomo() {
        // for scomo

        try {
            ArrayList<VdmScomoDp> dps = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT,
                    DmScomoHandler.getInstance()).getDps();
            if (dps != null && dps.size() != 0) {
                DmScomoState.getInstance(DmService.this).mCurrentDp = dps.get(0);
            }
        } catch (VdmException e) {
            e.printStackTrace();
        }
        Log.w(TAG.SERVICE,
                "initAndNotifyScomo currentDp "
                        + DmScomoState.getInstance(DmService.this).mCurrentDp.getName());

        notifyScomoListener();

        this.registerScomoListener(new DmScomoNotification(this));
    }

    public void deleteScomoFile() {
        deleteFile(IDmPersistentValues.SCOMO_FILE_NAME);
        deleteFile(IDmPersistentValues.RESUME_SCOMO_FILE_NAME);
    }

    public static synchronized void setScanPkgListStatus(boolean isNeed) {
        sIsNeedScanPkgList = isNeed;
    }

    public static synchronized boolean getScanPkgListStatus() {
        return sIsNeedScanPkgList;
    }

    public Handler getScomoHandler() {
        return mScomoHandler;
    }

    Handler mScomoHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }

            VdmScomoDp dp = null;
            int lastError = -1;
            switch (msg.what) {
            case IDmPersistentValues.MSG_SCOMO_DL_PKG_UPGRADE:
                onScomoDownloading(msg.arg1, msg.arg2);
                break;
            case IDmPersistentValues.MSG_SCOMO_CONFIRM_DOWNLOAD:
                dp = (VdmScomoDp) ((Object[]) msg.obj)[0];
                DownloadDescriptor dd = (DownloadDescriptor) ((Object[]) msg.obj)[1];
                onScomoConfirmDownload(dp, dd);
                break;
            case IDmPersistentValues.MSG_SCOMO_CONFIRM_INSTALL:
                dp = (VdmScomoDp) msg.obj;
                onScomoConfirmInstall(dp);
                break;
            case IDmPersistentValues.MSG_SCOMO_EXEC_INSTALL:
                dp = (VdmScomoDp) msg.obj;
                onScomoExecuteInstall(dp);
                break;
            case IDmPersistentValues.MSG_SCOMO_DL_SESSION_COMPLETED:
                break;
            case IDmPersistentValues.MSG_DM_SESSION_ABORTED:
                lastError = msg.arg1;
                onScomoError(lastError);
                processNextNiaMessage();
                break;
            case IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED:
                lastError = msg.arg1;
                onScomoError(lastError);
                if (DmScomoState.getInstance(DmService.this).mState == DmScomoState.IDLE) {
                    processNextNiaMessage();
                }
                break;
            case IDmPersistentValues.MSG_DM_SESSION_COMPLETED:
                deleteScomoFile();
                processNextNiaMessage();
                break;
            case IDmPersistentValues.MSG_SCOMO_DL_SESSION_START:
                DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOADING_STARTED;
                DmScomoState.store(DmService.this);
                notifyScomoListener();
                break;
            // case IDmPersistentValues.MSG_SCOMO_DL_SESSION_PAUSED: {
            // DmScomoState.getInstance(DmService.this).mState=DmScomoState.PAUSED;
            // DmScomoState.store(DmService.getInstance());
            // notifyScomoListener();
            // } break;
            // case IDmPersistentValues.MSG_SCOMO_DL_SESSION_RESUMED: {
            // DmScomoState.getInstance(DmService.this).mState=DmScomoState.RESUMED;
            // DmScomoState.store(DmService.getInstance());
            // notifyScomoListener();
            // } break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    private void onScomoError(int lastError) {
        Log.e(TAG.SERVICE, new StringBuilder("last error is ").append(lastError).append(" str is ")
                .append(VdmException.VdmError.fromInt(lastError)).toString());
        if ((lastError == VdmException.VdmError.COMMS_FATAL.val)
                || (lastError == VdmException.VdmError.COMMS_NON_FATAL.val)
                || (lastError == VdmException.VdmError.COMMS_SOCKET_ERROR.val)) {
            DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOAD_FAILED;
            DmScomoState.getInstance(DmService.this).mErrorMessage = "connection failed";
        } else if (lastError == VdmException.VdmError.COMMS_SOCKET_TIMEOUT.val) {
            DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOAD_FAILED;
            DmScomoState.getInstance(DmService.this).mErrorMessage = "timeout";
        } else if (lastError == VdmException.VdmError.CANCEL.val) {
            Log.w(TAG.SERVICE, "last error is cancel");
        } else {
            DmScomoState.getInstance(DmService.this).mState = DmScomoState.GENERIC_ERROR;
            DmScomoState.getInstance(DmService.this).mErrorMessage = "Error: "
                    + VdmException.VdmError.fromInt(lastError);
        }

        DmScomoState.store(this);
        notifyScomoListener();
    }

    void onScomoConfirmInstall(VdmScomoDp dp) {
        String archiveFilePath = "";
        try {
            archiveFilePath = new StringBuilder(DmService.this.getFilesDir().getAbsolutePath())
                    .append(File.separator).append(dp.getDeliveryPkgPath()).toString();
            Log.v(TAG.SERVICE, "scomo archive file path " + archiveFilePath);
        } catch (VdmException e) {
            e.printStackTrace();
        }

        DmScomoState.getInstance(DmService.this).mCurrentDp = dp; // currentDp is set again, because
        // confirm_download may not be
        // executed, if we are using direct http
        // download instead of
        // OMA download
        DmScomoState.getInstance(DmService.this).setArchivePath(archiveFilePath); // side effect:
                                                                                  // will set
        // pkgInfo
        Log.w(TAG.SERVICE, "onScomoConfirmInstall currentDp " + dp.getName());

        DmScomoState.getInstance(DmService.this).mState = DmScomoState.CONFIRM_INSTALL;
        DmScomoState.store(DmService.this);

        // // CMCC spec: no need user confirm download, install software
        // directly
        // if (DmScomoState.getInstance(DmService.this).mVerbose) {
        // Log.i(TAG.SERVICE,
        // "scomoConfirmInstall:mVerbose is true, notifyScomoListener");
        // DmService.this.notifyScomoListener();
        // } else {
        // Log.e(TAG.SERVICE,
        // "scomoConfirmInstall:mVerbose is nil, confirm directly");
        // }

        try {
            dp.executeInstall();
        } catch (VdmException e) {
            e.printStackTrace();
        }
        // }
    }

    void onScomoExecuteInstall(final VdmScomoDp dp) {
        DmScomoState.getInstance(DmService.this).mState = DmScomoState.INSTALLING;
        DmScomoState.store(this);
        DmService.this.notifyScomoListener();
        try {
            String archiveFilePath = new StringBuilder(getFilesDir().getAbsolutePath())
                    .append(File.separator).append(dp.getDeliveryPkgPath()).toString();

            DmScomoPackageManager.getInstance().install(archiveFilePath,
                    new DmScomoPackageManager.ScomoPackageInstallObserver() {
                        public void packageInstalled(String pkgName, int status) {
                            Log.e(TAG.SERVICE, "dmservice: package installed, status: " + status);
                            if (status == DmScomoPackageManager.STATUS_OK) {
                                try {
                                    String dpId = dp.getId();
                                    VdmScomo scomo = VdmScomo.getInstance(
                                            DmConst.NodeUri.SCOMO_ROOT,
                                            DmScomoHandler.getInstance());
                                    VdmScomoDc dc = scomo.createDC(pkgName,
                                            DmScomoDcHandler.getInstance(),
                                            DmPLInventory.getInstance());
                                    dc.deleteFromInventory();
                                    dc.destroy();
                                    dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(),
                                            DmPLInventory.getInstance());
                                    dc.addToInventory(pkgName, pkgName, dpId, null, null, null,
                                            true);
                                    new VdmTree().writeToPersistentStorage();
                                    dp.triggerReportSession(new VdmScomoResult(
                                            VdmScomoResult.SUCCESSFUL));
                                    DmScomoState.getInstance(DmService.this).mState
                                            = DmScomoState.INSTALL_OK;
                                    DmScomoState.store(DmService.this);
                                    DmService.this.notifyScomoListener();
                                } catch (VdmException e) {
                                    e.printStackTrace();
                                    onScomoInstallFailed(dp);
                                }
                            } else {
                                onScomoInstallFailed(dp);
                            }
                        }
                    }, true);
        } catch (VdmException e) {
            e.printStackTrace();
            onScomoInstallFailed(dp);
        }
    }

    private void onScomoInstallFailed(VdmScomoDp dp) {
        try {
            dp.triggerReportSession(new VdmScomoResult(VdmScomoResult.INSTALL_FAILED));
        } catch (VdmException e) {
            e.printStackTrace();
        }
        DmScomoState.getInstance(DmService.this).mState = DmScomoState.INSTALL_FAILED;
        DmScomoState.store(DmService.this);
        DmService.this.notifyScomoListener();
    }

    private void onScomoConfirmDownload(VdmScomoDp dp, DownloadDescriptor dd) {
        Log.e(TAG.SERVICE, "confirm download");
        DmScomoState.getInstance(DmService.this).mCurrentDd = dd;
        DmScomoState.getInstance(DmService.this).mCurrentDp = dp;

        Log.d(TAG.SERVICE, "onScomoConfirmDownload currentDp " + dp.getName());

        DmScomoState.getInstance(DmService.this).mState = DmScomoState.CONFIRM_DOWNLOAD;
        DmScomoState.store(DmService.this);
        try {
            DmScomoState.getInstance(DmService.this).mTotalSize = Integer.parseInt(dd
                    .getField(DownloadDescriptor.Field.SIZE));
        } catch (NumberFormatException e) {
            DmScomoState.getInstance(DmService.this).mTotalSize = 0;
        }
        if (DmScomoState.getInstance(DmService.this).mVerbose) {
            Log.e(TAG.SERVICE, "scomoConfirmDownload:mVerbose is true, notifyScomoListener");
            DmService.this.notifyScomoListener();
        } else {
            Log.e(TAG.SERVICE, "scomoConfirmDownload:mVerbose is nil, confirm directly");
            this.startDlScomoPkg();
        }
    }

    private void onScomoDownloading(int currentSize, int totalSize) {
        int state = DmScomoState.getInstance(DmService.this).mState;
        Log.i(TAG.SERVICE, "onScomoDownloading, state is " + state);
        if (state == DmScomoState.DOWNLOADING || state == DmScomoState.RESUMED
                || state == DmScomoState.IDLE || state == DmScomoState.DOWNLOADING_STARTED) {

            DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOADING;
            DmScomoState.store(DmService.this);

            DmScomoState.getInstance(DmService.this).mCurrentSize = currentSize;
            DmScomoState.getInstance(DmService.this).mTotalSize = totalSize;
            // downloading notification is too much...reshape the frequency
            if (currentSize > (mDownloadingNotificationCount) * totalSize / 20) {
                mDownloadingNotificationCount++;
                DmService.this.notifyScomoListener();
            }
            if (currentSize >= totalSize) {
                mDownloadingNotificationCount = 0;
            }
        }
    }

    private void notifyScomoListener() {
        if (!DmScomoState.getInstance(DmService.this).mVerbose) {
            Log.v(TAG.SERVICE,
                    "---notifyScomoListener, DmScomoState.mVerbose is false---");
        }
        synchronized (mScomoUpdateListeners) {
            // scomoUpdateListeners.get(0).onScomoUpdated();
            for (OnDmScomoUpdateListener listener : mScomoUpdateListeners) {
                listener.onScomoUpdated();
            }
        }
    }

    public void registerScomoListener(OnDmScomoUpdateListener listener) {
        synchronized (mScomoUpdateListeners) {
            mScomoUpdateListeners.add(listener);
        }
    }

    public void removeScomoListener(OnDmScomoUpdateListener listener) {
        synchronized (mScomoUpdateListeners) {
            mScomoUpdateListeners.remove(listener);
        }
    }

    public void startScomoSession(String dpName) {
        DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOADING_STARTED;

        try {
            VdmScomoDp currentDp = VdmScomo.getInstance(DmConst.NodeUri.SCOMO_ROOT,
                    DmScomoHandler.getInstance()).createDP(dpName, DmScomoDpHandler.getInstance());
            DmScomoState.getInstance(DmService.this).mCurrentDp = currentDp;
        } catch (VdmException e) {
            e.printStackTrace();
        }

        DmScomoState.store(this);
    }

    public void startDlScomoPkg() {
        Log.i(TAG.SERVICE, "startDLScomoPkg");

        DmScomoState.getInstance(DmService.this).mState = DmScomoState.DOWNLOADING_STARTED;
        DmScomoState.getInstance(DmService.this).mCurrentSize = 0;
        DmScomoState.getInstance(DmService.this).mTotalSize = 0;
        DmScomoState.store(this);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                deleteScomoFile();
                mDmController.proceedDLSession();
            }
        });
    }

    public void cancelDlScomoPkg() {
        Log.i(TAG.SERVICE, "cancelDlScomoPkg");

        DmScomoState.getInstance(DmService.this).mState = DmScomoState.ABORTED;
        DmScomoState.store(this);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                VdmScomoDp dp = DmScomoState.getInstance(DmService.this).mCurrentDp;
                try {
                    if (dp != null) {
                        Log.i(TAG.SERVICE, "cancelDlScomoPkg: triggerReportSession 1401");
                        dp.triggerReportSession(new VdmScomoResult(1401));
                    } else {
                        Log.e(TAG.SERVICE, "dp is null");
                    }
                } catch (VdmException e) {
                    e.printStackTrace();
                }
                deleteScomoFile();
                Log.d(TAG.SERVICE, "scomo dl canceled, delete delta package");
            }
        });
    }

    // public void cancelScomoSession() {
    // Log.e(TAG.Service, "cancelScomoSession");
    // mDmController.cancelDLSession();
    // }

    public void pauseDlScomoPkg() {
        Log.i(TAG.SERVICE, "pauseDlScomoPkg");

        if (DmScomoState.getInstance(DmService.this).mState == DmScomoState.RESUMED) {
            Log.w(TAG.SERVICE, "-- The ScomoState is RESUMED, cannot be paused --");
            return;
        }
        if (DmScomoState.getInstance(DmService.this).mState == DmScomoState.PAUSED) {
            Log.w(TAG.SERVICE, "-- The ScomoState is PAUSED, no need pause again --");
            return;
        }
        DmScomoState.getInstance(DmService.this).mState = DmScomoState.PAUSED;
        DmScomoState.store(this);
        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                mDmController.cancelDLSession();
                Log.i(TAG.SERVICE, "pauseDlScomoPkg end");
            }
        });

    }

    public void resumeDlScomoPkg() {
        Log.d(TAG.SERVICE, "resumeDlScomoPkg");

        if (DmScomoState.getInstance(DmService.this).mState != DmScomoState.PAUSED) {
            Log.e(TAG.SERVICE, "-- DmScomoState.getInstance(DmService.this) is not PAUSED!! --");
            return;
        }
        if (!sIsScomoSession) {
            // used for resume scomo when DM died abnormally.
            if (!mDmController.isIdle()) {
                // --it's other DM Session, but User want to resume scomo--
                Log.e(TAG.SERVICE, "--cannot resume! mDmController is not idle!! --");
                notifyScomoListener();
                return;
            }
            sIsScomoSession = true;
            DmPLDlPkg.setDeltaFileName(IDmPersistentValues.SCOMO_FILE_NAME);
        }

        DmScomoState.getInstance(DmService.this).mState = DmScomoState.RESUMED;
        DmScomoState.store(this);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                try {
                    DmScomoState.getInstance(DmService.this).mCurrentDp.resumeDLSession();
                    Log.d(TAG.SERVICE, "resumDlScomoPkg end");
                } catch (VdmException e) {
                    Log.e(TAG.SERVICE, "resumDlScomoPkg exception " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    public void resumeDlScomoPkgNoUI() {
        Log.d(TAG.SERVICE, "resumeDlScomoPkgNoUI");
        if (DmScomoState.getInstance(DmService.this).mState == DmScomoState.RESUMED) {
            Log.e(TAG.SERVICE, "-- duplicated resume!! --");
        }
        DmScomoState.getInstance(DmService.this).mState = DmScomoState.RESUMED;
        DmScomoState.store(this);

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                try {
                    DmScomoState.getInstance(DmService.this).mCurrentDp.resumeDLSession();
                    Log.d(TAG.SERVICE, "resumDlScomoPkgNoUI end");
                } catch (VdmException e) {
                    Log.e(TAG.SERVICE, "resumDlScomoPkgNoUI exception " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    // end scomo

    /**
     * Trigger boot strap session
     *
     * @param BootProfile
     *            profile - boot profile
     * @param CpSecurity
     *            security - security level
     * @param String
     *            mac - mac region of wap push
     * @param byte[] message - message body of wap push
     */
    public void triggerBootstrapSession(BootProfile profile, CpSecurity security, String mac,
            byte[] message) {
        Log.i(TAG.SERVICE, "Trigger bootstrap session.");
        if (mDmController != null) {
            mDmController.triggerBootstrapSession(profile, security, mac, message);
        }
        Log.i(TAG.SERVICE, "Bootstrap session triggered.");
    }

    /**
     * Trigger DM session.
     * @param account The DM account in tree.xml.
     * @param genericAlertType Generic alert type.
     * @param initiator Server initiated or client initiated.
     */
    public void triggerDMSession(String account, String genericAlertType,
            SessionInitiator initiator) {
        Log.i(TAG.SERVICE, "Trigger DM session.");
        if (mDmController != null) {
            mDmController.triggerDmSession(account, genericAlertType, initiator);
        }
    }

    private void onBootstrapComplete(String initiatorName) {

        boolean isDmBootstrap = DmConst.SessionInitiatorId.INITIATOR_DM_BOOTSTRAP
                .equals(initiatorName);

        ContentValues values = null;
        if (isDmBootstrap) {
            Log.d(TAG.SERVICE, "[onBootstrapComplete] isDmBootstrap");
            values = getApnFromTree();
        } else if (DmCpObserver.isBootstrap()) {
            Log.d(TAG.SERVICE, "[onBootstrapComplete] isCpBootstrap");
            values = DmCpObserver.getContentValue();
            DmCpObserver.resetStatus();
        } else {
            Log.w(TAG.SERVICE, "[onBootstrapComplete] Not really Bootstrap");
            DmCpObserver.resetStatus();
            return;
        }

        setPrefereUri(values);

        Log.d(TAG.SERVICE, "[onBootstrapComplete]trigger DM session");
        triggerDMSession(null, null,
                new DmBootstrapHandler(DmConst.SessionInitiatorId.INITIATOR_CI));
    }

    public void setPrefereUri(ContentValues values) {
        if (values == null) {
            Log.w(TAG.SERVICE, "[setPrefereUri] values == null");
            return;
        }

        TelephonyManager teleMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String mccmnc = teleMgr.getSimOperator(0);
        if (mccmnc != null && mccmnc.length() > 4) {
            // Country code
            String mcc = mccmnc.substring(0, 3);
            // Network code
            String mnc = mccmnc.substring(3);
            Log.d(TAG.SERVICE, "[setPrefereUri]mcc&mnc is right, mMcc = " + mcc + " mMnc = " + mnc);
            values.put(Telephony.Carriers.MCC, mcc);
            values.put(Telephony.Carriers.MNC, mnc);
            values.put(Telephony.Carriers.NUMERIC, mccmnc);

            values.put(Telephony.Carriers.SOURCE_TYPE, 1);

            ContentResolver resolver = this.getContentResolver();
            Uri uri = Telephony.Carriers.CONTENT_URI;
            long insertNum = -1;

            try {

                Uri newRow = resolver.insert(uri, values);
                if (newRow != null) {
                    Log.d(TAG.SERVICE, "[setPrefereUri]uri = " + newRow);
                    if (newRow.getPathSegments().size() == 2) {
                        insertNum = Long.parseLong(newRow.getLastPathSegment());
                        Log.d(TAG.SERVICE, "[setPrefereUri]insert row id = " + insertNum);
                    }
                }
            } catch (SQLException e) {
                Log.d(TAG.SERVICE, "[setPrefereUri]insert SQLException happened!");
            }
            if (insertNum > 0) {
                ContentValues preferedValues = new ContentValues();
                preferedValues.put("apn_id", insertNum);
                Uri preferedUri = Uri.parse("content://telephony/carriers/preferapn");
                int rows = resolver.update(preferedUri, preferedValues, null, null);
                Log.d(TAG.SERVICE, "[setPrefereUri]update preferedUri rows = " + rows);

            }
        } else {
            Log.w(TAG.SERVICE, "[setPrefereUri]mcc&mnc is NOT right , mccmnc = " + mccmnc);
        }

    }

    public ContentValues getApnFromTree() {
        Log.d(TAG.SERVICE, "[getApnFromTree]");

        ContentValues values = null;

        try {
            DmXMLParser parser = new DmXMLParser(DmConst.PathName.TREE_FILE_IN_DATA);
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_NAME) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_NAME);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_NAME " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.NAME, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_ADDR) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_ADDR);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_ADDR " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.APN, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_PROXYADDR) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_PROXYADDR);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_PROXYADDR " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.PROXY, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_PROXYPORT) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_PROXYPORT);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_PROXYPORT " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.PORT, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_USERNAME) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_USERNAME);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_USERNAME " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.USER, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_PASSWORD) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_PASSWORD);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_PASSWORD " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.PASSWORD, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_AUTHTYPE) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_AUTHTYPE);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_AUTHTYPE " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.AUTH_TYPE, value);

            }
            if (parser.getNodeByTreeUri(DmConst.NodeUri.CON_BEARER) != null) {
                String value = parser.getValueByTreeUri(DmConst.NodeUri.CON_BEARER);
                Log.d(TAG.SERVICE, "[getApnFromTree] value of CON_BEARER " + value);

                if (values == null) {
                    values = new ContentValues();
                }
                values.put(Telephony.Carriers.BEARER, value);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return values;
    }

    /**
     * Called when receive dm session complete message
     */
    private void onDmSessionComplete(DmAction action) {
        sIsScomoSession = false;
        if (mDmController != null) {
            int fumoAction = 0;
            int lawmoAction = 0;
            int scomoAction = 0;
            if (action != null) {
                Log.w(TAG.SERVICE, "onDmSessionComplete  action is not null");
                fumoAction = action.mFumoAction;
                lawmoAction = action.mLawmoAction;
                scomoAction = action.mScomoAction;
            }
            Log.i(TAG.SERVICE, "onDmSessionComplete fumo action is " + fumoAction);
            Log.i(TAG.SERVICE, "onDmSessionComplete lawmo action is " + lawmoAction);
            Log.i(TAG.SERVICE, "onDmSessionComplete scomo action is " + scomoAction);
            if (fumoAction != FumoAction.NONE) {
                DmPLDlPkg.setDeltaFileName(IDmPersistentValues.DELTA_FILE_NAME);
                initFotaThread();
                setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_COMPLETE);
            } else if (scomoAction != ScomoAction.NONE) {
                sIsScomoSession = true;
                DmPLDlPkg.setDeltaFileName(IDmPersistentValues.SCOMO_FILE_NAME);
                int state = DmScomoState.getInstance(DmService.this).mState;
                Log.d(TAG.SERVICE,
                        new StringBuilder("scomo begin, mState = ")
                                .append(state)
                                .append(", currentDp = ")
                                .append(String.valueOf(
                                        DmScomoState.getInstance(DmService.this).mCurrentDp))
                                .toString());
                if (state != DmScomoState.IDLE) {
                    DmScomoState.getInstance(DmService.this).mState = DmScomoState.IDLE;
                    DmScomoState.store(DmService.this);
                }
                if (DmScomoState.getInstance(DmService.this).mCurrentDp == null) {
                    try {
                        ArrayList<VdmScomoDp> dps = VdmScomo.getInstance(
                                DmConst.NodeUri.SCOMO_ROOT, DmScomoHandler.getInstance()).getDps();
                        if (dps != null && dps.size() != 0) {
                            DmScomoState.getInstance(DmService.this).mCurrentDp = dps.get(0);
                        }
                    } catch (VdmException e) {
                        e.printStackTrace();
                    }

                    Log.w(TAG.SERVICE, "onDmSessionComplete currentDp "
                            + DmScomoState.getInstance(DmService.this).mCurrentDp.getName());
                }
                mDownloadingNotificationCount = 0;
                setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_COMPLETE);
            } else if (sFakeLawmoAction == LawmoAction.FACTORY_RESET_EXECUTED) {
                sFakeLawmoAction = LawmoAction.NONE;
                // Erase SD card & Factory reset
                // temporary disable format and factory reset till new method is found
                //Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
                //intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                //this.startService(intent);
                // format phone memory
                StorageManager mStorage = getSystemService(StorageManager.class);
                for (VolumeInfo vol : mStorage.getVolumes()) {
                    String diskId = vol.getDiskId();
                    if (diskId != null && diskId.equals("disk:179,0")) {
                        Log.i(TAG.SERVICE, "onDmSessionComplete Storage manager" +
                        " notified to format phone memory");
                    mStorage.format(vol.getId());
                  }
                }
                // Perform Factory Reset
                sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                Log.i(TAG.SERVICE, "onDmSessionComplete broadcast sent to perform factory reset");
                // Factory reset
                // this.sendBroadcast(new
                // Intent("android.intent.action.MASTER_CLEAR"));
                // Log.i(TAG.Service,
                // "onDmSessionComplete send broadcast android.intent.action.MASTER_CLEAR");
                setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_COMPLETE);
            } else {
                Log.i(TAG.SERVICE, "onDmSessionComplete no action");
                if (mDlStatus == IDmPersistentValues.STATE_QUERY_NEW_VERSION) {
                    setDLStatus(IDmPersistentValues.STATE_NOT_DOWNLOAD);
                }
                setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_COMPLETE);
                processNextNiaMessage();
            }
        }
    }

    /**
     * Called when receive session abort message
     */
    private void onSessionAbort(String initiator, int lastError, int what) {
        String errorCode = VdmException.VdmError.fromInt(lastError).toString();
        Log.i(TAG.SERVICE, "Get error message. LastError is " + errorCode);

        if (initiator.startsWith(DmConst.SessionInitiatorId.INITIATOR_NETWORK)
                && lastError == VdmError.COMMS_SOCKET_ERROR.val
                && what == IDmPersistentValues.MSG_DMSESSIONABORTED) {
            if (mNiaMessage != null) {
                Log.i(TAG.SERVICE, "Save Nia message for resume. ");
                mMessageToResume = mNiaMessage;
            } else {
                Log.i(TAG.SERVICE, "NiaMessage is null, do nothing. ");
            }
        } else if (initiator.startsWith(DmConst.SessionInitiatorId.INITIATOR_CI)
                && (lastError == VdmError.COMMS_SOCKET_ERROR.val
                        || lastError == VdmError.BAD_URL.val)
                && what == IDmPersistentValues.MSG_DMSESSIONABORTED) {
            waitNetworkForBootstrap = true;
        } else if (what == IDmPersistentValues.MSG_DLSESSIONABORTED
                && (initiator != null && initiator
                        .contains(DmConst.SessionInitiatorId.INITIATOR_FUMO))) {
            if (lastError == VdmException.VdmError.COMMS_FATAL.val
                    || lastError == VdmException.VdmError.COMMS_NON_FATAL.val
                    || lastError == VdmException.VdmError.COMMS_SOCKET_ERROR.val
                    || lastError == VdmException.VdmError.COMMS_HTTP_ERROR.val
                    || lastError == VdmException.VdmError.COMMS_SOCKET_TIMEOUT.val) {
                Log.i(TAG.SERVICE, "[onSessionAbort]last error is network error");

                PersistentContext.getInstance(DmService.this).setDLSessionStatus(
                        IDmPersistentValues.STATE_PAUSE_DOWNLOAD);

            } else if ((mDlStatus != IDmPersistentValues.STATE_PAUSE_DOWNLOAD
                            && mDlStatus != IDmPersistentValues.STATE_NOT_DOWNLOAD)
                    || lastError != VdmException.VdmError.CANCEL.val) {
                Log.i(TAG.SERVICE, "[onSessionAbort]last error is not user cancle");

                PersistentContext.getInstance(DmService.this).resetDLStatus();
                reportresult(FumoResultCode.UNDEFINED_ERROR);
            }

        } else if (lastError == VdmException.VdmError.CANCEL.val) {
            Log.i(TAG.SERVICE, "last error is user cancle,status is " + mDlStatus);
            processNextNiaMessage();
        }
    }

    public void cancleDmSession() {
        Log.i(TAG.SERVICE, "cancleDmSession");
        if (mDmController != null) {
            mDmController.cancelSession();
        }
    }

    // start for fumo
    /**
     * Trigger fumo session to query new version
     */
    public void queryNewVersion(VdmFumo.ClientType clientType) {
        Log.i(TAG.SERVICE, "queryNewVersion Trigger fumo session.");
        if (mDmController != null) {
            mDmController.triggerFumoSession(null, clientType);
        }
        Log.i(TAG.SERVICE, "queryNewVersion Fumo session triggered.");
    }

    /**
     * Called when receive new version detected message
     *
     * @param DownloadDescriptor
     *            dd - download descriptor of current download
     */
    private int onNewVersionDetected(DownloadDescriptor dd) {
        Log.i(TAG.SERVICE, "onNewVersionDetected Get new version founded message.");
        int ret = FumoResultCode.SUCCESSFUL;
        if (dd == null) {
            Log.i(TAG.SERVICE, "dd is null, return MSG_NIASESSION_INVALID");
            return FumoResultCode.NOT_IMPLEMENTED;
        }

        String ddurl = dd.getField(DownloadDescriptor.Field.OBJECT_URI);
        String ddversion = dd.getField(DownloadDescriptor.Field.VERSION);
        Log.i(TAG.SERVICE, "onNewVersionDetected url is " + ddurl + " version is " + ddversion);
        if (ddurl == null) {
            Log.i(TAG.SERVICE, "ddurl is null, return MSG_NIASESSION_INVALID");
            return FumoResultCode.BAD_URL;
        }

        return ret;
    }

    /**
     * Proceed download session to start download
     */
    public void startDlPkg() {
        Log.i(TAG.SERVICE, "startDlPkg Proceed the download session.");
        mDmController.proceedDLSession();
        Log.i(TAG.SERVICE, "startDlPkg Download session proceeded.");
    }

    /**
     * Cancel download session and delete delta packaeg to cancel download
     *
     * @param boolean userConfirmed - if user confirm to cancel download, delete the delta package
     */
    public void cancelDlPkg() {
        // PersistentContext.getInstance(this).deleteDeltaPackage();
        PersistentContext.getInstance(this).resetDLStatus();
        Log.i(TAG.SERVICE, "[cancelDlPkg],User cancel the download process.");

        if (mDmController != null) {
            mDmController.cancelDLSession();
            Log.i(TAG.SERVICE, "cancelDlPkg report state to server");
            reportresult(FumoResultCode.USER_CANCELED);
        }
    }

    /**
     * Cancel download session to pause download
     */
    public void pauseDlPkg() {
        Log.i(TAG.SERVICE, "pauseDlPkg Pause the download session.");
        if (mDmController != null) {
            mDmController.cancelDLSession();
        }
        Log.i(TAG.SERVICE, "pauseDlPkg Download session Paused.");
    }

    /**
     * Resume download session to resume download
     */
    public void resumeDlPkg() {
        Log.i(TAG.SERVICE, "resumeDlPkg Resume the download session.");
        if (mDmController != null) {
            Log.w(TAG.SERVICE, "resumeDlPkg engine is idle");
            mDmController.resumeDLSession();
        } else {
            Log.w(TAG.SERVICE, "resumeDlPkg engine is not idle");
        }
        Log.i(TAG.SERVICE, "resumeDlPkg Download session resumed.");
    }

    /**
     * Called when receive download complete message
     */
    private void onDlPkgComplete(int verifyResult) {
        Log.i(TAG.SERVICE, "onDlPkgComplete service received the download finish message");
        if (verifyResult == FOTADeltaFiles.DELTA_VERIFY_OK) {
            setDLStatus(IDmPersistentValues.STATE_DL_PKG_COMPLETE);
        } else if (verifyResult == FOTADeltaFiles.DELTA_NO_STORAGE) {
            setDLStatus(IDmPersistentValues.STATE_VERIFY_NO_STORAGE);
            Log.w(TAG.SERVICE, "[onDlPkgComplete]: DELTA_NO_STORAGE!");
        } else {
            setDLStatus(IDmPersistentValues.STATE_VERIFY_FAIL);
            PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
            reportresult(FumoResultCode.FW_UP_CORRUPT);
            Log.e(TAG.SERVICE, "[onDlPkgComplete]: STATE_VERIFY_FAIL!");
        }
    }

    private void setDLStatus(int status) {
        PersistentContext.getInstance(this).setDLSessionStatus(status);
    }

    private void setDMSessionStatus(int status) {
        PersistentContext.getInstance(this).setDMSessionStatus(status);
    }

    public void initFotaThread() {
        Log.v(TAG.SERVICE, "initFotaThread begin");
        if (mFotaThread == null) {
            mFotaThread = new FotaSessionStateThread();
            Log.d(TAG.SERVICE, "new FotaSessionStateThread");
            mExec = DmThreadPool.getInstance();
            if (mExec != null) {
                mExec.execute(mFotaThread);
            }
        }
    }

    /**
     * Thread of calling dm service functions. Start a new thread to call functions of service to
     * avoid ANR. Use handler to process in order.
     */
    private class FotaSessionStateThread extends Thread {
        public void run() {
            Looper.prepare();
            mSessionStateHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Log.i(TAG.SERVICE, "In FotaSessionStateThread to invoke service's function: "
                            + msg.what);
                    switch (msg.what) {
                    case IDmPersistentValues.STATE_QUERY_NEW_VERSION:
                        queryNewVersion(VdmFumo.ClientType.USER);
                        break;
                    case IDmPersistentValues.STATE_START_TO_DOWNLOAD:
                        startDlPkg();
                        break;
                    case IDmPersistentValues.STATE_CANCEL_DOWNLOAD:
                        cancelDlPkg();
                        break;
                    case IDmPersistentValues.STATE_PAUSE_DOWNLOAD:
                        Log.i(TAG.SERVICE, "FotaSessionStateThread state is pausedownload");
                        pauseDlPkg();
                        break;
                    case IDmPersistentValues.STATE_RESUME_DOWNLOAD:
                        resumeDlPkg();
                        break;
                    case IDmPersistentValues.STATE_DL_PKG_COMPLETE:
                        int updateType = msg.arg1;
                        setUpdateType(updateType);
                        break;
                    case IDmPersistentValues.STATE_NOT_DOWNLOAD:
                        PersistentContext.getInstance(sServiceInstance).resetDLStatus();
                        cancleDmSession();
                        reportresult(FumoResultCode.USER_CANCELED);
                        break;
                    /// M: Move the unzip process here from mHandler.
                    case IDmPersistentValues.STATE_VERIFYING_PKG:
                        int result = FOTADeltaFiles
                                .unpackAndVerify(DmConst.PathName.DELTA_ZIP_FILE);
                        Message message = mHandler
                                .obtainMessage(IDmPersistentValues.MSG_VERIFYING_PKG);
                        message.arg1 = result;
                        mHandler.sendMessage(message);
                        break;
                    /// M: Move the set download size process here from mHandler to avoid ANR,
                    ///    the message is from MmiProgress.
                    case IDmPersistentValues.STATE_DOWNLOADING:
                        Log.v(TAG.SERVICE, "Downloading..., size is " + msg.arg1
                                + ", sDlStatus is " + mDlStatus);
                        long downloadsize = Long.valueOf(msg.arg1);
                        PersistentContext.getInstance(DmService.this)
                                .setDownloadedSize(downloadsize);

                        /// M: After set the downloaded size, send message to mHandler to sync UI.
                        mHandler.sendMessage(mHandler
                                .obtainMessage(IDmPersistentValues.MSG_DLPKGUPGRADE));
                        break;
                    default:
                        break;
                    }
                }
            };
            Looper.loop();
        }
    }

    public void reportresult(int resultCode) {
        if (mDmController != null) {
            mDmController.triggerReportSession(new FumoResultCode(resultCode));
        }
    }

    /**
     * Execute firmware update
     */
    public void executeFwUpdate() {
        Log.i(TAG.SERVICE, "executeFwUpdate Execute firmware update.");
        if (mDmController != null) {
            mDmController.executeFwUpdate();
        }
    }

    /**
     * Set reminder alarm due to the item id user selected
     *
     * @param long checkedItem - item user selected on list view
     */
    public void setAlarm(long checkedItem) {
        Log.i(TAG.SERVICE, "setAlarm Set reminder alarm");
        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.DM_REMINDER);
        if (mAlarmManager == null) {
            Log.w(TAG.SERVICE, "setAlarm alarmMgr is null, get alarmMgr.");
            mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }
        mReminderPendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mAlarmManager.cancel(mReminderPendingIntent);
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP,
                (System.currentTimeMillis() + mTimingArray[(int) checkedItem] * ONEMINUTE),
                mReminderPendingIntent);

        PersistentContext.getInstance(this).setPostPoneTimes();
    }

    /**
     * Cancel reminder alarm
     */
    public void cancelAlarm() {
        Log.i(TAG.SERVICE, "cancelAlarm, cancel reminder alarm");
        if (mAlarmManager != null && mReminderPendingIntent != null) {
            Log.w(TAG.SERVICE, "cancle reminder Alarm");
            mAlarmManager.cancel(mReminderPendingIntent);
            mReminderPendingIntent = null;
        }
    }

    /**
     * Get texts of update types
     *
     * @return String array contains texts of update types
     */
    public String[] getUpdateTypes() {
        getReminderAndTiming();
        return mTextArray;
    }

    /**
     * Get timing of update types
     *
     * @return int array contains texts of update types
     */
    public int[] getTimingType() {
        if (mTimingArray == null) {
            getReminderAndTiming();
        }
        return mTimingArray;
    }

    /**
     * Set update type user selected
     *
     * @param long type - the item id user selected
     */
    public void setUpdateType(long type) {

        Log.i(TAG.SERVICE, "setUpdateType type is " + type);
        if (type > mTimingArray.length) {
            return;
        }

        int leftTimes = 3;
        if (DmService.isDmTreeReady()) {
            try {
                DmXMLParser parser = new DmXMLParser(DmConst.PathName.TREE_FILE_IN_DATA);
                String postponeString = parser.getValueByTreeUri(DmConst.NodeUri.FUMO_EXT_POSTPONE);

                Log.d(TAG.SERVICE, "[setUpdateType], get postpone value:" + postponeString);

                int defaultTimes = Integer.valueOf(postponeString);
                int times = PersistentContext.getInstance(this).getPostPoneTimes();
                leftTimes = defaultTimes - times;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG.SERVICE, "[setUpdateType], select is  " + mTimingArray[(int) type]);
        Log.i(TAG.SERVICE, "[setUpdateType], left is  " + leftTimes);

        if (mTimingArray[(int) type] == UPDATENOW) {
            cancelAlarm();
            executeFwUpdate();
        } else if (mTimingArray[(int) type] == NEVERASK || leftTimes <= 0) {
            cancelAlarm();
            /// M: Handle NIA message in UI thread.
            mHandler.sendMessage(mHandler.obtainMessage(IDmPersistentValues.MSG_PROCESS_NEXT_NIA));
        } else {
            setAlarm(type);
            /// M: Handle NIA message in UI thread.
            mHandler.sendMessage(mHandler.obtainMessage(IDmPersistentValues.MSG_PROCESS_NEXT_NIA));
        }
    }

    /**
     * Get update select items from configuration file
     */
    private void getReminderAndTiming() {
        Log.i(TAG.SERVICE, "Execute getReminderParser");
        DmXMLParser xmlParser = new DmXMLParser(DmConst.PathName.REMINDER_FILE);
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getChildNode(nodeList, "operator");

        if (nodeList != null && nodeList.size() > 0) {
            Node node = nodeList.get(0);
            List<Node> timeNodeList = new ArrayList<Node>();
            xmlParser.getChildNode(node, timeNodeList, DmConst.NodeName.TIMING);
            Node timingNode = timeNodeList.get(0);
            List<Node> timingNodeList = new ArrayList<Node>();
            xmlParser.getLeafNode(timingNode, timingNodeList, "item");
            if (timingNodeList != null && timingNodeList.size() > 0) {
                int size = timingNodeList.size();
                mTimingArray = new int[size];
                for (int i = 0; i < size; i++) {
                    String nodeStr = timingNodeList.get(i).getFirstChild().getNodeValue();
                    // Here do not catch exception for test of the reminder file
                    mTimingArray[i] = Integer.parseInt(nodeStr);
                }
            }

            List<Node> textList = new ArrayList<Node>();
            xmlParser.getChildNode(node, textList, DmConst.NodeName.TEXT);
            Node textNode = textList.get(0);
            List<Node> textNodeList = new ArrayList<Node>();
            xmlParser.getLeafNode(textNode, textNodeList, "item");
            if (textNodeList != null && textNodeList.size() > 0) {
                int size = textNodeList.size();
                mTextArray = new String[size];
                for (int i = 0; i < size; i++) {
                    String nodeStr = textNodeList.get(i).getFirstChild().getNodeValue();
                    Field filedname = null;
                    int strOffset = 0;
                    try {
                        filedname = R.string.class.getField(nodeStr);
                        strOffset = filedname.getInt(null);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    mTextArray[i] = getString(strOffset);
                }
            }
        }
    }

    /**
     * check firmware update result, if success, return true, else return false
     */
    public static boolean checkUpdateResult() {
        boolean isUpdateSuccessfull = false;
        if (MTKOptions.MTK_EMMC_SUPPORT) {
            Log.d(TAG.SERVICE, "----- reading OTA result for eMMC -----");
            int otaResult = 0;

            try {
                otaResult = MTKPhone.getDmAgent().readOtaResult();
                Log.d(TAG.SERVICE, "OTA result = " + otaResult);
            } catch (RemoteException ex) {
                Log.e(TAG.SERVICE, "DMAgent->readOtaResult failed:" + ex);
            }
            isUpdateSuccessfull = otaResult == 1;
        } else {
            Log.d(TAG.SERVICE, "----- reading OTA result for NAND -----");
            File updateFile = new File(DmConst.PathName.UPDATE_RESULT_FILE);
            if (!updateFile.exists()) {
                Log.w(TAG.SERVICE, "RebootChecker the update file is not exist");
                return false;
            }
            try {
                Log.i(TAG.SERVICE, "RebootChecker the update file is  exist");
                byte[] ret = new byte[0];
                FileInputStream in = new FileInputStream(DmConst.PathName.UPDATE_RESULT_FILE);
                ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                byte[] buff = new byte[512];
                int rc = 0;
                while ((rc = in.read(buff, 0, 512)) > 0) {
                    swapStream.write(buff, 0, rc);
                }
                ret = swapStream.toByteArray();
                in.close();
                swapStream.close();
                String result = new String(ret);
                Log.i(TAG.SERVICE, "RebootChecker update result is " + result);
                if (result != null) {
                    isUpdateSuccessfull = result.equalsIgnoreCase("1");
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG.SERVICE, "RebootChecker " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            } finally {
                if (updateFile != null) {
                    Log.w(TAG.SERVICE, "RebootChecker deltet the update files");
                    updateFile.delete();
                }
            }
        }
        return isUpdateSuccessfull;
    }

    /**
     * Trigger report session to report the update result
     */
    private void reportState(boolean isUpdateSuccessfull) {
        if (isUpdateSuccessfull) {
            Log.i(TAG.SERVICE, "RebootChecker update result is 1, report code is "
                    + FumoResultCode.SUCCESSFUL);
            reportresult(FumoResultCode.SUCCESSFUL);
        } else {
            Log.i(TAG.SERVICE, "RebootChecker update result is 0, report code is "
                    + FumoResultCode.UPDATE_FAILED);
            reportresult(FumoResultCode.UPDATE_FAILED);
        }
    }

    private PersistentContext.FumoUpdateObserver mFumoObserver =
            new PersistentContext.FumoUpdateObserver() {
        @Override
        public void syncDLstatus(int status) {
            Log.d(TAG.SERVICE, "syncDLstatus =>" + status);
            mDlStatus = status;
        }

        @Override
        public void syncDmSession(int status) {
        }

    };

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }

            switch (msg.what) {
            case IDmPersistentValues.MSG_NEWVERSIONDETECTED:
                int resultCode = onNewVersionDetected((DownloadDescriptor) msg.obj);
                if (resultCode != FumoResultCode.SUCCESSFUL) {
                    Log.w(TAG.SERVICE, "[mHandler]new version is invalid");
                    PersistentContext.getInstance(DmService.this).resetDLStatus();
                    cancleDmSession();
                    reportresult(resultCode);
                } else {
                    Log.w(TAG.SERVICE, "new version is valid");
                    DownloadDescriptor dd = (DownloadDescriptor) msg.obj;

                    Log.d(TAG.SERVICE, "++saving dd..." + dd);
                    Log.d(TAG.SERVICE, "dd size=" + dd.size);
                    Log.d(TAG.SERVICE, "--------------------");
                    for (String value : dd.field) {
                        Log.d(TAG.SERVICE, "\t" + value);
                    }
                    Log.d(TAG.SERVICE, "--------------------");
                    PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
                    PersistentContext.getInstance(DmService.this).setDownloadDescriptor(dd);
                    setDLStatus(IDmPersistentValues.STATE_NEW_VERSION_DETECTED);
                }
                break;
            case IDmPersistentValues.MSG_DLPKGSTARTED:
                Log.v(TAG.SERVICE, "----mHandler, receive MSG_DLPKGSTARTED msg----, mDlStatus = "
                        + mDlStatus);
                if (mDlStatus == IDmPersistentValues.STATE_RESUME_DOWNLOAD) {
                    setDLStatus(IDmPersistentValues.STATE_START_TO_DOWNLOAD);
                }
                break;
            case IDmPersistentValues.MSG_DLPKGUPGRADE:
                /// M: Modify logic, after set downloaded size, update the UI.
                // maybe is in the pausing process, then ignore the status
                if (mDlStatus == IDmPersistentValues.STATE_DOWNLOADING
                        || mDlStatus == IDmPersistentValues.STATE_START_TO_DOWNLOAD) {
                    setDLStatus(IDmPersistentValues.STATE_DOWNLOADING);
                }
                break;
            case IDmPersistentValues.MSG_DLPKGCOMPLETE:
                // maybe is in the pausing process, then ignore the status
                if (mDlStatus != IDmPersistentValues.STATE_DOWNLOADING
                        && mDlStatus != IDmPersistentValues.STATE_START_TO_DOWNLOAD
                        && mDlStatus != IDmPersistentValues.STATE_VERIFY_NO_STORAGE) {
                    Log.d(TAG.SERVICE,
                            "mDlStatus is not State downloading, ignore DL PKG COMPLETE msg");
                    return;
                }
                /// M: Move unzip process to FotaSessionStateThread.
                setDLStatus(IDmPersistentValues.STATE_VERIFYING_PKG);
                mSessionStateHandler.sendMessage(mSessionStateHandler
                        .obtainMessage(IDmPersistentValues.STATE_VERIFYING_PKG));
                break;
            case IDmPersistentValues.MSG_DMSESSIONABORTED:
            case IDmPersistentValues.MSG_DLSESSIONABORTED:
            case IDmPersistentValues.MSG_BOOTSTRAPSESSIONABORTED:
                int errorCode = msg.arg1;
                String initiator = (String) msg.obj;
                if (initiator.contains(DmConst.SessionInitiatorId.INITIATOR_FUMO)) {
                    PersistentContext.getInstance(DmService.this).setFumoErrorCode(errorCode);
                } else {
                    PersistentContext.getInstance(DmService.this).setFumoErrorCode(
                            VdmException.VdmError.OK.val);
                }
                setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_CANCLE);

                onSessionAbort(initiator, msg.arg1, msg.what);
                break;
            case IDmPersistentValues.MSG_NIACONFIRMED:

                int uiMode = (Integer) msg.obj;
                Log.i(TAG.SERVICE, "ui mode is " + uiMode);

                int status = -1;
                if (uiMode == UIMode.BACKGROUND.val() || uiMode == UIMode.NOT_SPECIFIED.val()) {
                    status = IDmPersistentValues.STATE_DM_USERMODE_INVISIBLE;
                    proceedNiaMessage();
                } else if (uiMode == UIMode.INFORMATIVE.val()) {
                    setNiaAlarm(TIMEOUT_UI_VISIBLE, false, false);
                    status = IDmPersistentValues.STATE_DM_USERMODE_VISIBLE;
                } else if (uiMode == UIMode.UI.val()) {
                    setNiaAlarm(TIMEOUT_UI_INTERACT, true, false);
                    status = IDmPersistentValues.STATE_DM_USERMODE_INTERACT;
                }
                setDMSessionStatus(status);

                break;
            case IDmPersistentValues.MSG_DMSESSIONCOMPLETED:
                deleteNia();
                onDmSessionComplete((DmAction) msg.obj);
                break;
            case IDmPersistentValues.MSG_BOOTSTRAPSESSIONCOMPLETED:
                onBootstrapComplete((String) msg.obj);
                break;
            case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
                Log.d(TAG.SERVICE, "mHandler message is MSG_WAP_CONNECTION_SUCCESS");
                Log.d(TAG.SERVICE, "wap connect success");
                if (mDmController == null) {
                    initDmController();
                }

                if (sReceivedIntent != null) {
                    handleStartEvent(null);
                } else {
                    if (waitNetworkForBootstrap) {
                        waitNetworkForBootstrap = false;
                        Log.i(TAG.SERVICE, "Bootstrap complete, triggerDmSession");
                        triggerDMSession(null, null, new DmBootstrapHandler(
                                DmConst.SessionInitiatorId.INITIATOR_CI));
                    }
                    /*
                     * added: for handle the issue of data connection reconnected/socket timeout
                     */
                    if (mMessageToResume != null) {
                        Log.i(TAG.SERVICE, "there is messageToResume, triggerNiaDmSession");
                        mDmController.triggerNiaDmSession(mMessageToResume);
                        mMessageToResume = null;
                    }
                }
                break;
            // added: end
            case IDmPersistentValues.MSG_VERIFYING_PKG:
                onDlPkgComplete(msg.arg1);
                break;
            case IDmPersistentValues.MSG_PROCESS_NEXT_NIA:
                processNextNiaMessage();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    public class DmBinder extends Binder {
        public DmService getService() {
            return DmService.this;
        }
    }

    public FumoState getFumoState() {
        if (mDmController != null) {
            return mDmController.getFumoState();
        } else {
            return FumoState.IDLE;
        }
    }

    public int getSessionInitor() {
        return mSessionInitor;
    }

    public void setSessionInitor(int initor) {
        Log.d(TAG.SERVICE, "set session initor=" + initor);
        mSessionInitor = initor;
    }

    // end for fumo

    // start for Nia
    private void setNiaAlarm(long seconds, boolean uiInteract, boolean niaAlert) {
        Log.i(TAG.SERVICE,
                new StringBuilder("setNiaAlarm reminder alarm: nia alert").append(niaAlert)
                        .append(", interact ").append(uiInteract).toString());
        Intent intent = new Intent(this, DmService.class);
        intent.putExtra(INTENT_EXTRA_UI_INTERACT, uiInteract);
        intent.putExtra(INTENT_EXTRA_NIA_ALERT, niaAlert);

        intent.setAction(DmConst.IntentAction.DM_NIA_START);
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }

        mNiaOperation = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.cancel(mNiaOperation);
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + seconds
                * ONESECOND), mNiaOperation);
    }

    public void cancleNiaAlarm() {
        Log.i(TAG.SERVICE, "cancle Nia Alarm enter");
        if (mAlarmManager != null && mNiaOperation != null) {
            Log.w(TAG.SERVICE, "cancle Nia Alarm");
            mAlarmManager.cancel(mNiaOperation);
            mNiaOperation = null;
        }
    }

    /**
     * Trigger NIA session to start a network initiated dm session
     *
     * @param byte[] message - message body of wap push
     */
    public void triggerNiaMessage(byte[] message) {
        if (message == null) {
            Log.i(TAG.SERVICE, "Do not trigger Nia session : message is null");
            return;
        }
        if (mDmController == null) {
            Log.i(TAG.SERVICE, "Do not trigger Nia session : mDmController is null");
            return;
        }
        Log.i(TAG.SERVICE, "Trigger Nia session.");

        setSessionInitor(IDmPersistentValues.SERVER);

        setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_START);

        if (Options.SCOMO_SUPPORT) {
            DmScomoState.getInstance(DmService.this).mVerbose = false;
            DmScomoState.store(this);
        }

        mDmController.triggerNiaDmSession(message);

        Log.i(TAG.SERVICE, "Nia session triggered.");
    }

    /**
     * Proceed NIA session
     */
    private void proceedNiaMessage() {
        Log.i(TAG.SERVICE, "Proceed Nia session.");
        if (mExec == null) {
            mExec = DmThreadPool.getInstance();
        }

        if (mExec != null) {
            mExec.execute(new NiaProcessor());
        }

        Log.i(TAG.SERVICE, "Nia session proceeded.");
    }

    public void showNiaNotification(int msgState) {
        Log.i(TAG.SERVICE, "showNiaNotification msgState is " + msgState);
        if (mNiaNotificationShown) {
            Log.i(TAG.SERVICE, "there is messageToResume, cancel the notification");
            DmConfirmInfo.sObserver.notifyConfirmationResult(true);
            return;
        }
        if (Options.SCOMO_SUPPORT) {
            DmScomoState.getInstance(DmService.this).mVerbose = true;
            DmScomoState.store(this);
            Log.i(TAG.SERVICE, "mVerbose set to true");
        }

        Log.i(TAG.SERVICE, "setDMSessionStatus: IDmPersistentValues.STATE_DM_NIA_ALERT");
        if (msgState == IDmPersistentValues.MSG_NIASESSION_START) {
            setDMSessionStatus(IDmPersistentValues.STATE_DM_NIA_ALERT);
        } else if (msgState == IDmPersistentValues.MSG_NIA_ALERT_1102) {
            setDMSessionStatus(IDmPersistentValues.MSG_NIA_ALERT_1102);
        }
        setNiaAlarm(TIMEOUT_ALERT_1101, false, true);
        mNiaNotificationShown = true;
    }

    public void userCancled() {
        Log.i(TAG.SERVICE, "userCancled");

        // also cancel NIA alarm when user cancelled.
        cancleNiaAlarm();

        processNextNiaMessage();
    }

    public void receivedNiaMessage(byte[] message) {
        if (mDmController == null) {
            Log.w(TAG.SERVICE, "receivedNiaMessage mDmController is null");
            return;
        }
        boolean idle = mDmController.isIdle();
        Log.i(TAG.SERVICE, "receivedNiaMessage the idle state of the engine is " + idle);
        if (idle) {
            processNextNiaMessage();
        }
    }

    public void writeNiaMessage(Intent intent) {
        if (intent == null) {
            Log.w(TAG.SERVICE, "writeNiaMessage receivedIntent is null");
            return;
        }
        if (intent.getAction() == null) {
            Log.w(TAG.SERVICE, "writeNiaMessage receivedIntent asction is null");
            return;
        }
        if (intent.getAction().equals(DmConst.IntentAction.DM_WAP_PUSH)) {
            Log.i(TAG.SERVICE, "Receive NIA wap push intent");
            String type = intent.getType();
            byte[] message = intent.getByteArrayExtra("data");
            Log.i(TAG.SERVICE, "wap-push message: " + new String(message));

            if (DmConst.IntentType.DM_NIA.equals(type)) {
                String filename = String.valueOf(System.currentTimeMillis());
                if (mNiaQueue == null) {
                    Log.w(TAG.SERVICE, "receivedNiaMessage NiaQueue is null");
                    return;
                }
                if (mNiaQueue.size() == MAXNIAQUEUESIZE) {
                    Log.w(TAG.SERVICE, "receivedNiaMessage exceeds the max number messages");
                    return;
                }

                NiaInfo info = new NiaInfo();
                info.mFilename = filename;
                info.mMsg = message;
                mNiaQueue.add(info);

                if (mExec == null) {
                    mExec = DmThreadPool.getInstance();
                }
                if (mExec != null) {
                    mExec.execute(new MsgInfoWriter(message, filename));
                }
            }
        }

    }

    public void processNiaMessage(byte[] message, String filename) {
        Log.i(TAG.SERVICE, "processNiaMessage Enter");

        if (Options.SCOMO_SUPPORT) {
            // Scan package list for SCOMO if needed:
            if (getScanPkgListStatus()) {
                DmScomoPackageManager.getInstance().scanPackage();
                setScanPkgListStatus(false);
            }
        }
        if (message == null || filename == null || filename.length() <= 0) {
            Log.w(TAG.SERVICE, "Invalid parameters, filename is " + filename);
            return;
        }
        mCurrentNiaMsgName = filename;
        mNiaMessage = message;
        triggerNiaMessage(message);
    }

    public void processNextNiaMessage() {
        // Reset parameters for Resume DM Session when account abnormal APN
        // Error
        mNiaNotificationShown = false;
        mMessageToResume = null;
        sCCStoredParams.clear();
        Log.v(TAG.SERVICE, "reset resume DM Session params, the CCStoredParams number is "
                + sCCStoredParams.size());
        // End reset parameters Resume DM Session when account abnormal APN
        // Error
        // TODO: follows reset scomo state
        if (Options.SCOMO_SUPPORT) {
            int state = DmScomoState.getInstance(DmService.this).mState;
            if (state != DmScomoState.IDLE
                    && (state == DmScomoState.ABORTED || state == DmScomoState.INSTALL_OK
                            || state == DmScomoState.INSTALL_FAILED
                            || state == DmScomoState.DOWNLOAD_FAILED
                            || state == DmScomoState.GENERIC_ERROR)) {
                Log.v(TAG.SERVICE, "processNextNiaMessage, reset scomo state");
                DmScomoState.getInstance(DmService.this).mState = DmScomoState.IDLE;
                DmScomoState.store(DmService.this);
            }
        }
        deleteNia();
        NiaInfo currentMsg = null;
        if (mNiaQueue == null || mNiaQueue.size() <= 0) {
            Log.w(TAG.SERVICE, "processNextNiaMessage there is no nia message to proceed");
            // stopSelf();
            if (!Options.USE_DIRECT_INTERNET) {

                final Runnable stopConnJob = new Runnable() {
                    @Override
                    public void run() {
                        if (mDmController.isIdle() && mNiaQueue.size() <= 0
                                && DmApplication.getInstance().isDMWapConnected()) {
                            Log.d(TAG.SERVICE, "****** stopping DM connection when idle ******");
                            DmDataConnection.getInstance(DmService.this).stopDmDataConnectivity();
                            closeMobileNetWork();
                        }
                    }
                };

                // delay stop network for 2 minute
                DmApplication.getInstance().scheduleJob(stopConnJob, 2 * 60 * 1000);
            }

            return;
        }
        currentMsg = mNiaQueue.poll();

        if (currentMsg != null) {
            processNiaMessage(currentMsg.mMsg, currentMsg.mFilename);
        }
    }

    private void openMobileNetWork() {
        TelephonyManager teleMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (!teleMgr.getDataEnabled()) {
            boolean isLock = false;
            try {
                DmAgent agent = MTKPhone.getDmAgent();
                if (agent == null) {
                    Log.e(TAG.SERVICE, "get dm_agent_binder failed.");
                } else {
                    isLock = agent.isLockFlagSet();
                }
            } catch (RemoteException e) {
                Log.e(TAG.SERVICE, "get registered IMSI failed", e);
            }

            if (isLock) {
                teleMgr.setDataEnabled(true);
                mNeedCloseNetwork = true;
                Log.d(TAG.SERVICE, "DM Service open connectivity self in lawmo lock state");
            }
        }
    }

    private void closeMobileNetWork() {
        if (mNeedCloseNetwork) {
            TelephonyManager teleMgr =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (teleMgr.getDataEnabled()) {
                teleMgr.setDataEnabled(false);
                mNeedCloseNetwork = false;
                Log.d(TAG.SERVICE, "DM Service open connectivity self in lawmo lock state");
            }
        }
    }

    class MsgInfoWriter implements Runnable {
        private byte[] mMsg;
        private String mFilename;

        public MsgInfoWriter(byte[] message, String name) {
            mMsg = message;
            mFilename = name;
        }

        public void run() {
            Log.d(TAG.SERVICE, "+++ writeMsgInfo run +++");
            String dirpath = DmConst.PathName.PATH_IN_DATA;
            File dir = new File(dirpath);
            if (!dir.exists()) {
                Log.w(TAG.SERVICE, "writeMsgInfo the data dir is not exist");
                return;
            }
            String niaPath = DmConst.PathName.NIA_FILE;
            File nia = new File(niaPath);
            if (!nia.exists()) {
                boolean ret = nia.mkdirs();
                if (!ret) {
                    Log.w(TAG.SERVICE, "writeMsgInfo make the nia dir fail");
                    return;
                }
            }
            FileOutputStream msgFile = null;
            try {
                String filepath = new StringBuilder(niaPath).append(File.separator)
                        .append(mFilename).toString();
                msgFile = new FileOutputStream(filepath);
                msgFile.write(mMsg);
            } catch (FileNotFoundException e) {
                Log.e(TAG.SERVICE, e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (msgFile != null) {
                        msgFile.close();
                        msgFile = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG.SERVICE, e.getMessage());
                }
            }
        }
    }

    public void deleteNia() {
        Log.i(TAG.SERVICE, "deleteNia Enter");
        if (mCurrentNiaMsgName == null || mCurrentNiaMsgName.length() <= 0) {
            Log.w(TAG.SERVICE, "deleteNia the current msg name is null");
            return;
        }
        String filePath = new StringBuilder(DmConst.PathName.NIA_FILE).append(File.separator)
                .append(mCurrentNiaMsgName).toString();
        File file = new File(filePath);
        if (file.exists()) {
            boolean ret = file.delete();
            if (ret) {
                Log.w(TAG.SERVICE, "deleteNia delete file sucess, file name is "
                        + mCurrentNiaMsgName);
                mCurrentNiaMsgName = null;
            }
        }
    }

    class NiaMsgReader implements Runnable {

        @Override
        public void run() {
            if (mNiaQueue == null) {
                Log.w(TAG.SERVICE, "ReadNiaMsg the niaqueue is null");
                return;
            }
            String niaFolder = DmConst.PathName.NIA_FILE;
            File folder = new File(niaFolder);
            if (!folder.exists()) {
                Log.w(TAG.SERVICE, "ReadNiaMsg the nia dir is noet exist");
                return;
            }

            String[] fileExist = folder.list();
            if (fileExist == null || fileExist.length <= 0) {
                Log.w(TAG.SERVICE, "ReadNiaMsg there is no unproceed message");
                return;
            }
            // long[] files=new long[fileExist.length];
            // for(int i=0;i<fileExist.length;i++)
            // {
            // files[i]=Long.valueOf(fileExist[i]);
            // }
            Arrays.sort(fileExist);
            int length = fileExist.length;
            for (int i = 0; i < length && i < MAXNIAQUEUESIZE; i++) {
                String name = fileExist[i];
                if (name == null || name.length() <= 0) {
                    continue;
                }

                FileInputStream in = null;
                try {
                    in = new FileInputStream(niaFolder + File.separator + name);
                    ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                    byte[] buff = new byte[512];
                    int rc = 0;
                    while ((rc = in.read(buff, 0, 512)) > 0) {
                        swapStream.write(buff, 0, rc);
                    }
                    NiaInfo info = new NiaInfo();
                    info.mFilename = name;
                    info.mMsg = swapStream.toByteArray();
                    mNiaQueue.add(info);
                    swapStream.close();

                } catch (FileNotFoundException e) {
                    Log.e(TAG.SERVICE, e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                            in = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG.SERVICE, e.getMessage());
                    }
                }
            }

            if (mNiaQueue.size() > 0) {
                processNextNiaMessage();
            }
        }
    }

    class NiaProcessor implements Runnable {

        public void run() {
            if (mDmController != null) {
                mDmController.proceedNiaSession();
            }
        }

    }

    class NiaInfo {
        private String mFilename;
        private byte[] mMsg;
    }

    public static Boolean isDmTreeReady() {
        Log.d(TAG.SERVICE, "is dm tree ready? begin");
        Boolean ret = false;
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            File systemTree = new File(DmConst.PathName.TREE_FILE_IN_SYSTEM);
            File dataTree = new File(DmConst.PathName.TREE_FILE_IN_DATA);
            File dataFilesDir = new File(DmConst.PathName.PATH_IN_DATA);
            if (!dataTree.exists()) {
                if (!systemTree.exists()) {
                    Log.e(TAG.SERVICE, "The tree in system is not exist");
                    return ret;
                }
                if (!dataFilesDir.exists()) {
                    Log.e(TAG.SERVICE, "there is no /files dir in dm folder");
                    if (dataFilesDir.mkdir()) {
                        // chmod for recovery access?
                        MTKFileUtil.openPermission(DmConst.PathName.PATH_IN_DATA);
                    } else {
                        Log.e(TAG.SERVICE, "Create files dir in dm folder error");
                        return ret;
                    }
                }
                int length = 1024 * 50;
                in = new FileInputStream(systemTree);
                out = new FileOutputStream(dataTree);
                byte[] buffer = new byte[length];
                while (true) {
                    Log.i(TAG.SERVICE, "in while");
                    int ins = in.read(buffer);
                    if (ins == -1) {
                        in.close();
                        out.flush();
                        out.close();
                        Log.i(TAG.SERVICE, "there is no more data");
                        break;
                    } else {
                        out.write(buffer, 0, ins);
                    }

                } // while
            }
            ret = true;
        } catch (IOException e) {
            Log.e(TAG.SERVICE, e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                Log.e(TAG.SERVICE, e.getMessage());
            }
        }
        return ret;
    }

    public void notifyUserPinSet(String pinCode, boolean b) {
        this.mDmController.notifyUserPinSet(pinCode, b);

    }

}
