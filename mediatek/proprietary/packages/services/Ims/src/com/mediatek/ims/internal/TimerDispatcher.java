    package com.mediatek.ims.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.SystemClock;
import android.os.Bundle;
import android.app.AlarmManager;
import android.app.PendingIntent;

import android.util.Log;

import com.mediatek.ims.ImsAdapter;
import com.mediatek.ims.ImsAdapter.VaSocketIO;
import com.mediatek.ims.ImsAdapter.VaEvent;
import com.mediatek.ims.ImsEventDispatcher;

import static com.mediatek.ims.VaConstants.*;

public class TimerDispatcher implements ImsEventDispatcher.VaEventDispatcher {

    private Context mContext;
    private VaSocketIO mSocket;
    private static final String TAG = "Timer-IMSA";

    private static final Object mLock = new Object();
    private static ImsAdapter imsAdapt;

    protected static final String INTENT_VOLTE_TIMER_ALARM = "com.android.internal.telephony.volte_timer_alarm";

    public TimerDispatcher(Context context, VaSocketIO IO) {
        mContext = context;
        mSocket = IO;

        log("TimerDispatcher()");

        if (imsAdapt == null) {
            log("ImsAdapter.getInstance");
            imsAdapt = ImsAdapter.getInstance();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_VOLTE_TIMER_ALARM);
        mContext.registerReceiver(mResultReceiver, filter);
    }

    public void enableRequest() {
        log("enableRequest()");
    }

    public void disableRequest() {
        log("disableRequest()");
    }


    private void sendResponse(int request_id, int timer_id, int user_data) {
        int phone_id = ImsAdapter.Util.getDefaultVoltePhoneId();
        VaEvent event = new ImsAdapter.VaEvent(phone_id, request_id);

        // timer_id
        event.putInt(timer_id);

        // timeout
        event.putInt(0);

        // user_data
        event.putInt(user_data);

        // send the event to va
        mSocket.writeEvent(event);

        log("send event, request_id = " + request_id + ", timer_id = " + timer_id + ", user_data = " + user_data);
    }

    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(INTENT_VOLTE_TIMER_ALARM)) {
                    Bundle bundle  = intent.getExtras();
                    if (bundle != null) {
                        int timer_id = bundle.getInt("timer_id");
                        int user_data = bundle.getInt("user_data");

                        sendResponse(MSG_ID_NOTIFY_TIMER_EXPIRY, timer_id, user_data);

                        log("timer timeout, timer_id = " + timer_id + ", user_data = " + user_data);

                    } else {
                        log("receive intent = " + intent.getAction());
                    }
                } else {
                    log("receive intent = " + intent.getAction());
                }
            } else {
                log("receive intent = " + intent.getAction());
            }
        }
    };

    public void vaEventCallback(VaEvent event) {

        try {
            int request_id;

            request_id = event.getRequestID();
            int timer_id  = event.getInt();
            int timeout   = event.getInt();
            int user_data = event.getInt();

            // log("reqeust_id = " + request_id + ", timer_id = " + timer_id + ", user_data = " + user_data + ", timeout = " + timeout);
            switch (request_id) {
                case MSG_ID_REQUEST_TIMER_CREATE : {
                    AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

                    Bundle bundle = new Bundle();
                    bundle.putInt("timer_id", timer_id);
                    bundle.putInt("user_data", user_data);

                    Intent intent = new Intent(INTENT_VOLTE_TIMER_ALARM);
                    intent.putExtras(bundle);

                    PendingIntent pi = PendingIntent.getBroadcast(mContext, user_data, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    long triggerAtTime = SystemClock.elapsedRealtime() + (timeout * 1000);
                    am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

                    log("timer set, timer_id = " + timer_id + ", user_data = " + user_data + ", timeout = " + timeout + ",triggerAtTime = " + triggerAtTime);
                    break;
                }

                case MSG_ID_REQUEST_TIMER_CANCEL : {
                    Bundle bundle = new Bundle();

                    bundle.putInt("timer_id", timer_id);
                    bundle.putInt("user_data", user_data);

                    Intent intent = new Intent(INTENT_VOLTE_TIMER_ALARM);
                    intent.putExtras(bundle);

                    PendingIntent pi = PendingIntent.getBroadcast(mContext, user_data, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    if (pi != null) {
                        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                        am.cancel(pi);
                        // pi.cancel();
                    }

                    log("timer cancel, timer_id = " + timer_id + ", user_data = " + user_data);
                    break;
                }

                default:
                    log("Unknown request: " + request_id);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void log(String s) {
        Log.d(TAG, s);
    }

}
