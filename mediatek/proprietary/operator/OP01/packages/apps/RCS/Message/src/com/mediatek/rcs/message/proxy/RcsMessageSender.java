package com.mediatek.rcs.message.proxy;


import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
//import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.util.SqliteWrapper;

//import com.mediatek.rcs.message.R;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.message.aidl.*;
import com.mediatek.rcs.message.plugin.RcsUtilsPlugin;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

public class RcsMessageSender extends IMessageSender.Stub {
    private static final String TAG = "RcsMessageSender";
    private static RcsMessageSender sInstance;
    private Context mContext;

    private RcsMessageSender(Context context) {
        mContext = context;
    }

    public static RcsMessageSender getInstance(Context context) {
        if (sInstance == null) {
            synchronized (RcsMessageSender.class) {
                if (sInstance == null) {
                    sInstance = new RcsMessageSender(context);
                }
            }
        }
        return sInstance;
    }


    public boolean sendTextMessage(List<String> numbers, String message) {
        return false;
    }

    public boolean sendTextMessageToGroup(long threadId, String message) {
        return false;
    }
    public Uri insertMmsFromIntent(Intent intent) {
        Context context = ContextCacher.getHostContext();
        Log.d(TAG, "[insertMmsFromIntent]: context is " + context);
        return null;
    }
    public boolean sendMms(int subId, List<String> numbers, Uri uri) {
        Log.d(TAG, "sendMms() subId:" + subId + ",numbers:" + numbers + ",uri:" + uri);
        SendReq sendReq = new SendReq();
        PduPersister persister = PduPersister.getPduPersister(ContextCacher.getHostContext());
        try {
            GenericPdu pdu = persister.load(uri);
            if (pdu instanceof MultimediaMessagePdu) {
                EncodedStringValue subject = ((MultimediaMessagePdu)pdu).getSubject();
                if (subject != null) {
                    sendReq.setSubject(subject);
                }
                sendReq.setBody(((MultimediaMessagePdu)pdu).getBody());

                for (String number : numbers) {
                    sendReq.addTo(new EncodedStringValue(number));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri mTempMmsUri = null;
        try {
            mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);

            long messageSize = 300 * 1024;
            //long subId = SubscriptionManager.getDefaultDataSubId();
            long threadId = 0;
            try {
                HashSet<String> recipients = new HashSet<String>(numbers);
                threadId = Threads.getOrCreateThreadId(ContextCacher.getHostContext(), recipients);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "get thread id fail");
            }
            if (!RcsUtilsPlugin.sendMms(mTempMmsUri, messageSize, subId, threadId)) {
                SqliteWrapper.delete(ContextCacher.getHostContext(),
                                     ContextCacher.getHostContext().getContentResolver(),
                                     mTempMmsUri,
                                     null,
                                     null);
            }
        } catch (MmsException e) {
            Log.e(TAG, "Failed to copy message: " + uri);
            // runOnUiThread(new Runnable() {
            //         public void run() {
            //             Toast.makeText(ForwardActivity.this,
            //             R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
            //         }
            //     });
            return false;
        }

        return true;
    }

    public boolean sendFavoriteMms(int subId, List<String> numbers, String fileName) {
        Log.d(TAG, "sendFavoriteMms() subId:" + subId + ",numbers:" + numbers
              + ",fileName:" + fileName);
        SendReq sendReq = new SendReq();
        PduPersister persister = PduPersister.getPduPersister(ContextCacher.getHostContext());
        try {
            byte[] pduByte = readFileContent(fileName);
            GenericPdu pdu = new PduParser(pduByte,false).parse();
            if (pdu instanceof MultimediaMessagePdu) {
                EncodedStringValue subject = ((MultimediaMessagePdu)pdu).getSubject();
                if (subject != null) {
                    sendReq.setSubject(subject);
                }
                sendReq.setBody(((MultimediaMessagePdu)pdu).getBody());
                for (String number : numbers) {
                    sendReq.addTo(new EncodedStringValue(number));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri mTempMmsUri = null;
        try {
            mTempMmsUri = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);

            long messageSize = 300 * 1024;
            //long subId = SubscriptionManager.getDefaultDataSubId();

            long threadId  = 0;
            try {
                HashSet<String> recipients = new HashSet<String>(numbers);
                threadId = Threads.getOrCreateThreadId(ContextCacher.getHostContext(), recipients);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "get thread id fail");
            }

            boolean result = RcsUtilsPlugin.sendMms(mTempMmsUri, messageSize, subId, threadId);
            Log.d(TAG, "sendFavoriteMms() mTempMmsUri:" + mTempMmsUri
                  + ",messageSize:" + messageSize
                  + ",subId:" + subId
                  + ",threadId:" + threadId
                  + ",result:" + result);
            if (!result) {
                SqliteWrapper.delete(ContextCacher.getHostContext(),
                                     ContextCacher.getHostContext().getContentResolver(),
                                     mTempMmsUri,
                                     null,
                                     null);
            }
        } catch (MmsException e) {
            e.printStackTrace();
        }

        return true;
    }

    private byte[] readFileContent(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            is.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Uri insertMmsFromPdu(byte[] pduByte) {
        Uri ret = null;
        try {
            SendReq sendReq = new SendReq();
            MultimediaMessagePdu mmsPdu = (MultimediaMessagePdu)new PduParser(pduByte, false).parse();
            if (mmsPdu.getSubject() != null) {
                sendReq.setSubject(mmsPdu.getSubject());
            }
            sendReq.setBody(mmsPdu.getBody());
            PduPersister persister = PduPersister.getPduPersister(mContext);
            ret = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean deleteMms(Uri pdu) {
        boolean ret = false;
        try {
            ContentResolver resolver = mContext.getContentResolver();
            int count = resolver.delete(pdu, null, null);
            if (count == 1) {
                ret = true;
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, "[deleteMms]: e = " + e.toString());
        }
        return ret;
    }

    @Override
    public boolean startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }
}
