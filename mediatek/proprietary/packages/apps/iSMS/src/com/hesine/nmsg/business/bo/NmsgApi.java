package com.hesine.nmsg.business.bo;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.hesine.nmsg.common.MLog;

public final class NmsgApi {
    private static final String FUN_ID_NMSG_SERVICE_IS_READY = "1";    
    private static final String FUN_ID_IS_NMSG_NUMBER= "2";
    private String auth = "com.hesine.remote.api.providers";
    public final Uri apiContentUri = Uri.parse("content://" + auth);
    private static NmsgApi mInstance = null;
    private Context mContext = null;
    private ContentResolver mApiProviders = null;

    private NmsgApi(Context context) {
        mContext = context;
        mApiProviders = mContext.getContentResolver();
    }

    public static synchronized NmsgApi getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NmsgApi(context);
        }
        return mInstance;
    }

    public boolean isNmsgServiceReady() {
        try {
            Bundle back = mApiProviders.call(apiContentUri, FUN_ID_NMSG_SERVICE_IS_READY, null, null);
            if (back != null) {
                return back.getBoolean(FUN_ID_NMSG_SERVICE_IS_READY, false);
            } else {
                return false;
            }
        } catch (NullPointerException e) {
            MLog.error(MLog.getStactTrace(e));
            return false;
        } catch (IllegalArgumentException e) {
            MLog.error(MLog.getStactTrace(e));
            return false;
        }
    }

    public void checkNmsgService(Context context) {
        if (!isNmsgServiceReady()) {
            MLog.info("checkNmsgService is not running so start it");
            Intent i = new Intent();
            i.setAction("com.hesine.nmsg.startservice");
            context.sendBroadcast(i);
            return;
        }
    }

    public boolean isNmsgNumber(String number) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FUN_ID_IS_NMSG_NUMBER + 1, number);
            Bundle back = mApiProviders.call(apiContentUri, FUN_ID_IS_NMSG_NUMBER, null, bundle);
            if (back != null) {
                return back.getBoolean(FUN_ID_IS_NMSG_NUMBER, false);
            } else {
                return false;
            }
        } catch (NullPointerException e) {
            MLog.error(MLog.getStactTrace(e));
            return false;
        } catch (IllegalArgumentException e) {
            MLog.error(MLog.getStactTrace(e));
            return false;
        }
    }

    public boolean startConversationActivity(Context context, long threadId, String number,
            String openType) {
        if (isNmsgNumber(number)) {
            try {
                Intent intent = new Intent();
                intent.setAction("com.hesine.nmsg.ui.ConversationActivity");
                intent.setClassName("com.hesine.nmsg",
                        "com.hesine.nmsg.ui.ConversationActivity");
                intent.putExtra("thread_id", threadId);
                intent.putExtra("phone_number", number);
                intent.putExtra("open_type", openType);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                MLog.error(MLog.getStactTrace(e));
            }
        }
        return false;
    }

    public void startSettingActivity(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.hesine.nmsg", "com.hesine.nmsg.ui.NmsgSettingActivity");
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

}
