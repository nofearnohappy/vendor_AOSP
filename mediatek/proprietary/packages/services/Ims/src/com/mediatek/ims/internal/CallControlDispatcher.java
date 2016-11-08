package com.mediatek.ims.internal;

import android.content.Context;
import android.content.Intent;
import android.telephony.Rlog;

import com.mediatek.ims.ImsAdapter.VaSocketIO;
import com.mediatek.ims.ImsAdapter.VaEvent;
import com.mediatek.ims.ImsEventDispatcher;
import static com.mediatek.ims.VaConstants.*;



public class CallControlDispatcher implements ImsEventDispatcher.VaEventDispatcher {

    private Context mContext;
    private VaSocketIO mSocket;
    private static final String TAG = "[CallControlDispatcher]";
    private static final int IMC_PROGRESS_NOTIFY_CONFERENCE = 257;
    private static final int IMC_PROGRESS_NOTIFY_MWI = 258;

    public static final String ACTION_LTE_MESSAGE_WAITING_INDICATION = "android.intent.action.lte.mwi";
    public static final String ACTION_IMS_CONFERENCE_CALL_INDICATION = "android.intent.action.ims.conference";
    public static final String EXTRA_LTE_MWI_BODY = "lte_mwi_body";
    public static final String EXTRA_MESSAGE_CONTENT = "message.content";
    public static final String EXTRA_CALL_ID = "call.id";
    public static final String EXTRA_PHONE_ID = "phone.id";

    public CallControlDispatcher(Context context, VaSocketIO IO) {
        mContext = context;
        mSocket = IO;
    }

    public void enableRequest() {
        Rlog.d(TAG, "enableRequest()");
    }

    public void disableRequest() {
        Rlog.d(TAG, "disableRequest()");
    }

    public void vaEventCallback(VaEvent event) {
        try {
            int requestId;
            int len;
            int callId;
            int phoneId;
            int serviceId;
            String data;
            byte[] byteData;

            requestId = event.getRequestID();
            len = event.getDataLen();
            callId = event.getInt();
            phoneId = event.getPhoneId();
            serviceId = event.getInt();
            byteData = event.getBytes(4000);
            data  = new String(byteData);
            len = getDataLength(byteData, 4000);
            Rlog.d(TAG, "requestId = " + requestId + ", length = " + len +
                    ", callId = " + callId + ", phoneId = " + phoneId +
                    ", serviceId = " + serviceId + ", data = " + data.substring(0, len));

            switch (serviceId) {
                case IMC_PROGRESS_NOTIFY_MWI:
                    /* Send to APP directly */
                    Intent intent = new Intent(ACTION_LTE_MESSAGE_WAITING_INDICATION);
                    intent.putExtra(EXTRA_LTE_MWI_BODY, data);
                    intent.putExtra(EXTRA_PHONE_ID, phoneId);
                    mContext.sendBroadcast(intent);
                    Rlog.d(TAG, "Message Waiting Message is sent.");
                    break;
                case IMC_PROGRESS_NOTIFY_CONFERENCE:
                    /* Send to GSMPhone object */
                    Intent intent1 = new Intent(ACTION_IMS_CONFERENCE_CALL_INDICATION);
                    intent1.putExtra(EXTRA_MESSAGE_CONTENT, data.substring(0, len));
                    intent1.putExtra(EXTRA_CALL_ID, callId);
                    intent1.putExtra(EXTRA_PHONE_ID, phoneId);
                    mContext.sendBroadcast(intent1);
                    Rlog.d(TAG, "Conference call XML message is sent.");
                    break;
                default:
                    Rlog.d(TAG, "Unkonwn serviceId: " + serviceId);
                    break;
            }


        } catch (Exception e) {
            e.printStackTrace();
        } /* Enf of try */
    }

    /* Caculate the data length except 0x0 */
    private int getDataLength(byte[] data, int originLen) {
       int i;
       for (i = 0; i < originLen; i++) {
          if (data[i] == 0x0) {
             return i;
          }
       }

       return i;
    }
}
