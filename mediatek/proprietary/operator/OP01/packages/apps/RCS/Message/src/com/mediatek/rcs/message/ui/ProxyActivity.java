package com.mediatek.rcs.message.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.message.SendModeChangedNotifyService;
import com.mediatek.rcs.message.utils.RcsMessageUtils;


public class ProxyActivity extends Activity {
    private static final String TAG = "ProxyActivity";
    private final String ACTION_START_GROUP = "com.mediatek.rcs.groupchat.START";
    private final String KEY_CHAT_ID = "chat_id";
    private Handler mHandler;

    public static final String KEY_TYPE = "type";
    public static final int VALUE_START_RCS_SETTING = 1;
    private static final int REQUEST_CODE_START_RCS_SETTING = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        initialize(savedInstanceState, getIntent());
    }

    private void initialize(Bundle saveBundle, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "initialize action: " +action);
        int type = intent.getIntExtra(KEY_TYPE, 0);
        if (action != null) {
            if (action.equals(ACTION_START_GROUP)) {
                String chatId = intent.getStringExtra(KEY_CHAT_ID);
                Log.e(TAG, "initialize chatId: " +chatId);
                if (chatId != null) {
                    startGroupChat(chatId);
                } else {
                    finish();
                }
            }
        } else if (type == VALUE_START_RCS_SETTING) {
            RcsMessageUtils.startRcsSettingActivity(this, REQUEST_CODE_START_RCS_SETTING);
        }
        else {
            Log.e(TAG, "unkown action: " +action);
            finish();
        }
    }

    private void startGroupChat(final String chatId) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                GroupChatCache mapCache = null;
                while(mapCache == null) {
                    mapCache = GroupChatCache.getInstance();
                    if (mapCache != null) {
                        final long threadId = RCSDataBaseUtils.findThreadIdForGroup(
                                ProxyActivity.this, chatId);
                        if (threadId > 0) {
                            mHandler.post(new Runnable() {

                                @Override
                                public void run() {
                                    // TODO Auto-generated method stub
                                    openCreatedGroup(threadId, chatId);
                                }
                            });

                        }
                    }
                }
            }
        }, "startGroupChat").start();
    }

    private void openCreatedGroup(long threadId, String chatId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("vnd.android-dir/mms-sms");
//        if (threadId < 0) {
//            threadId = -threadId;
//        }
        intent.putExtra("thread_id", threadId);
        intent.putExtra("chat_id", chatId);
        intent.setPackage("com.android.mms");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(): requestCode = " + requestCode);
        switch (requestCode) {
        case REQUEST_CODE_START_RCS_SETTING:
            Intent service = SendModeChangedNotifyService.createSetFinishIntent();
            startService(service);
            finish();
            break;

        default:
            break;
        }
    }
}
