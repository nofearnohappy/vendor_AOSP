package com.mediatek.rcs.message;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.ProxyActivity;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Telephony.Sms;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Transformation;

/**
 * Process rcs message send fail and need notify transfer to sms.
 * @author mtk80999
 *
 */
public class SendModeChangedNotifyService extends Service {

    private static final String TAG = "SendModeChangedNotifyService";
    private static final String TYPE = "type";
    private static final int TYPE_UNKNOWN = 0;
    private static final int TYPE_NEW_FAILED = 1;
    private static final int TYPE_SET_FINISH = 2;
    private static final String KEY_SMSID = "smsId";
    private static final String KEY_IPMSGID = "ipmsgId";
    private static final String KEY_THREADID = "threadid";
    private static final String KEY_SUBID = "subid";
    private static final String KEY_BODY = "body";
    private static final String KEY_ADDRESS = "address";

    private static final int EVENT_NEW_INTENT = 1;
    private static final int EVENT_QUIT = 2;
    private static final String WAITING_PREFERENCE = "waiting_items";
//    private String[] mFaiedMsgProjection = { Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY,
//            Sms.IPMSG_ID, Sms.PROTOCOL, Sms.SUBSCRIPTION_ID};

    private List<FailedMessageInfo> mFailedItemList = new ArrayList<FailedMessageInfo>();
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private boolean mWaitingSettingCompleted;

    public static Intent createSmsNotifyIntent(long msgId, long ipmsgId, long threadId,
                                                int subId, String body, String address) {
        Intent intent = new Intent("com.mediatek.rcs.message.SendModeChangedNotifyService");
        intent.setPackage("com.mediatek.rcs.message");
        intent.putExtra(TYPE, TYPE_NEW_FAILED);
        intent.putExtra(KEY_SMSID, msgId);
        intent.putExtra(KEY_IPMSGID, ipmsgId);
        intent.putExtra(KEY_THREADID, threadId);
        intent.putExtra(KEY_SUBID, subId);
        intent.putExtra(KEY_BODY, body);
        intent.putExtra(KEY_ADDRESS, address);
        return intent;
    }

    public static Intent createSetFinishIntent() {
        Intent intent = new Intent("com.mediatek.rcs.message.SendModeChangedNotifyService");
        intent.setPackage("com.mediatek.rcs.message");
        intent.putExtra(TYPE, TYPE_SET_FINISH);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
     // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("TransactionService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        Notification noti = new Notification();
//        Notification noti = new Notification(R.drawable.ic_launcher_smsmms, null,
//                System.currentTimeMillis());
//        noti.flags |= Notification.FLAG_NO_CLEAR;
//        noti.flags |= Notification.FLAG_HIDE_NOTIFICATION;
//        startForeground(1, noti);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent != null) {
                Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
                msg.arg1 = startId;
                msg.obj = intent;
                mServiceHandler.sendMessage(msg);
            }
        }
        Log.d(TAG, "onStartCommand: intent = " + intent);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy.");
        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
//        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private FailedMessageInfo processNewFailedIntent(Intent intent) {
        long smsId = intent.getLongExtra(KEY_SMSID, 0);
        long ipmsgId = intent.getLongExtra(KEY_IPMSGID, 0);
        long threadId = intent.getLongExtra(KEY_THREADID, 0);
        int subId = intent.getIntExtra(KEY_SUBID, 0);
        String body = intent.getStringExtra(KEY_BODY);
        String address = intent.getStringExtra(KEY_ADDRESS);
        FailedMessageInfo info = new FailedMessageInfo(smsId, ipmsgId, threadId, subId, body,
                                    address);
        return info;
    }

    private void startNotify(Intent intent) {
        FailedMessageInfo info = processNewFailedIntent(intent);
        Log.w(TAG, "[startNotify]: smsId = " + info.mSmsId);
        boolean needNotifyToSms = RcsMessageUtils.isNeedNotifyUserWhenToSms(this, info.mSubId);
        Log.w(TAG, "[startNotify]: needNotifyToSms = " + needNotifyToSms);
        if (!needNotifyToSms) {
            boolean transfered = RcsMessageUtils.isTransferToSMSWhenSendFailed(this, info.mSubId);
            processFailedItem(info, transfered);
            stopSelf();
        } else {
            mFailedItemList.add(info);
            addWaitingMessageItem(info);
            if (!mWaitingSettingCompleted) {
//                Intent activityIntent = new Intent();
//                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
//                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                activityIntent.setClass(this, SendModeChangedActivity.class);
//                this.startActivity(activityIntent);
                showNotifyDialog();
                mWaitingSettingCompleted = true;
            }
        }
    }

    private void processSetFinish(Intent intent) {
        int currentSubId = 0;
        Log.d(TAG, "[processSetFinish]");
        if (mFailedItemList.size() == 0) {
            Log.e(TAG, "[processSetFinish] waiting size = 0, get from preference");
            mFailedItemList = getFailedMsgInfoListFromPreference();
            if (mFailedItemList.size() == 0) {
                Log.e(TAG, "[processSetFinish] waiting size = 0");
                return;
            }
        }
        clearWaitingMessageItem(this);
        FailedMessageInfo firstItem = mFailedItemList.get(0);
        if (firstItem != null) {
            currentSubId = firstItem.mSubId;
        } else {
            Log.e(TAG, "No failed item exist when processSetFinish: " + intent);
            return;
        }
        //don't notify anymore in future
        RcsMessageUtils.updateNeedNotifyUserWhenToSmsValue(this, currentSubId, false);
        boolean transfered = RcsMessageUtils.isTransferToSMSWhenSendFailed(this, currentSubId);
        for (FailedMessageInfo info : mFailedItemList) {
            processFailedItem(info, transfered);
        }
        stopSelf();
    }

    private void processFailedItem(FailedMessageInfo info, boolean transferToSms) {
        Log.d(TAG, "[processFailedItem]: msgId = " + info.mSmsId + "transfered = " + transferToSms);
        Context context = ContextCacher.getHostContext();
        if (context == null) {
            context = this;
        }
        if (transferToSms) {
            RcsMessageUtils.transferToSMSFromFailedRcsMessage(context, info.mThreadId,
                    info.mSmsId, info.mIpmsgId, info.mSubId, info.mAddress, info.mBody);
        } else {
            RCSDataBaseUtils.updateMessageStatus(context, info.mSmsId, MessageStatus.FAILED);
        }
    }

    private void processNewIntent(Intent intent) {
        int type = intent.getIntExtra(TYPE, TYPE_UNKNOWN);
        switch (type) {
        case TYPE_NEW_FAILED:
            startNotify(intent);
            break;
        case TYPE_SET_FINISH:
            processSetFinish(intent);
            break;
        default:
            break;
        }
    }
    private final class FailedMessageInfo {
        public FailedMessageInfo(long smsId, long ipid, long threadId, int subId, String body,
                                    String address) {
            mSmsId = smsId;
            mIpmsgId = ipid;
            mThreadId = threadId;
            mSubId = subId;
            mBody = body;
            mAddress = address;
        }
        long mSmsId;
        long mIpmsgId;
        long mThreadId;
        int mSubId;
        String mBody;
        String mAddress;

        @Override
        public String toString() {
            return "smsId = " + mSmsId + ", ipMsgId = " + mIpmsgId + ", address = " + mAddress;
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_NEW_INTENT:
                processNewIntent((Intent) msg.obj);
                break;
            case EVENT_QUIT:
                getLooper().quit();
                break;
            default:
                break;
            }
        }
    }

    private void showNotifyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send_way_use_sms_Title);
        builder.setMessage(R.string.send_way_use_sms_indicate);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // transfer to sms
                RcsMessageUtils.setTransferToSMSWhenSendFailed(getApplicationContext(), 0, true);
                Intent intent = SendModeChangedNotifyService.createSetFinishIntent();
                startService(intent);
            }
        });
        builder.setNegativeButton(R.string.send_way_set, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // to set
                Intent intent = new Intent(getApplicationContext(), ProxyActivity.class);
                intent.putExtra(ProxyActivity.KEY_TYPE, ProxyActivity.VALUE_START_RCS_SETTING);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        builder.setCancelable(false);

        AlertDialog dialog  = builder.create();
        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub

            }
        });
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//        setStatusBarEnableStatus(false);
        dialog.show();
    }

    private void addWaitingMessageItem(FailedMessageInfo item) {
        SharedPreferences sp = this.getSharedPreferences(WAITING_PREFERENCE,
                Context.MODE_WORLD_WRITEABLE);
        int count = sp.getInt("count", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("item_" + count, item.mSmsId);
        editor.putInt("count", count+1);
        editor.commit();
    }



    private List<FailedMessageInfo> getFailedMsgInfoListFromPreference() {
        SharedPreferences sp = this.getSharedPreferences(WAITING_PREFERENCE,
                Context.MODE_WORLD_WRITEABLE);
        int count = sp.getInt("count", 0);
        Log.d(TAG, "[getFailedMsgInfoListFromPreference]: count = " + count);
        List<FailedMessageInfo> list = new ArrayList<FailedMessageInfo>(count);
        for (int key = 0; key < count; key++) {
            long smsId = sp.getLong("item_"+ key, 0);
            if (smsId > 0) {
                FailedMessageInfo info = createFailedMsgItemBySmsId(smsId);
                if (info != null) {
                    list.add(info);
                }
            }
        }
        return list;
    }

    private FailedMessageInfo createFailedMsgItemBySmsId(long msgId) {
        FailedMessageInfo info = null;
        Cursor cursor = RCSDataBaseUtils.getMessage(this, msgId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                long threadId = cursor.getLong(cursor.getColumnIndex(MessageColumn.CONVERSATION));
                long ipmsgId = cursor.getLong(cursor.getColumnIndex(MessageColumn.IPMSG_ID));
                String body = cursor.getString(cursor.getColumnIndex(MessageColumn.BODY));
                int subId = cursor.getInt(cursor.getColumnIndex(MessageColumn.SUB_ID));
                String address = cursor.getString(
                                        cursor.getColumnIndex(MessageColumn.CONTACT_NUMBER));
                info = new FailedMessageInfo(msgId, ipmsgId, threadId, subId, body, address);
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "[getFailedMsgInfoListFromPreference]: info = " + info);
        return info;
    }

    /**
     * reset when reboot
     * @param context
     */
    public static void resetWhenReBoot(Context context) {
        clearWaitingMessageItem(context);
    }

    private static void clearWaitingMessageItem(Context context) {
        Log.d(TAG, "[resetWhenReBoot]");
        SharedPreferences sp = context.getSharedPreferences(WAITING_PREFERENCE,
                Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }
}
