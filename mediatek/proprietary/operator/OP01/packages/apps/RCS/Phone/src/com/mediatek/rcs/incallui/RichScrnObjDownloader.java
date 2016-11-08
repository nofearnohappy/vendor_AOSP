package com.mediatek.rcs.incallui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.cmcc.sso.sdk.auth.AuthnConstants;
import com.cmcc.sso.sdk.auth.AuthnHelper;
import com.cmcc.sso.sdk.auth.TokenListener;
import com.cmcc.sso.sdk.util.SsoSdkConstants;
import com.cmdm.control.util.client.ResultEntity;
import com.cmdm.rcs.biz.RichScrnPersonBiz;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Downloader Rich Screen by using CMCC SDK.
 */
public class RichScrnObjDownloader implements TokenListener {
    private static final String TAG = "RichScrnObjDownloader";

    private static final int MSG_INIT_SDK = 1000;
    private static final int MSG_LOG_IN = 1001;
    private static final int MSG_DOWNLOAD_SCRN = 1002;
    private static final int MSG_GET_NUMBERS = 1003;

    private static final int INIT_DISK_SIZE = 64;
    private static final String SOURCE_ID = "005001";
    private static final String APP_ID = "00500131";
    private static final String APP_KEY = "F09C9C070AE495FB";
    private static final String CONTACT_PHONE_EVENT = "9341020000";

    private RichScrnPersonBiz mRichScrnPersonBiz = null;
    private Context mContext;
    private static RichScrnObjDownloader sInstance;
    private DownloadHandler mHandler;
    private ArrayList<String> mNumbersQueue = new ArrayList<String>();
    private int mLoginState = 0;

    private static final int MAX_NUMBER_QUEUE_SIZE = 1000;

    private static final int RICH_SCRN_LOGIN_NONE = 0;
    private static final int RICH_SCRN_LOGIN_PROCESS = 1;
    private static final int RICH_SCRN_LOGIN_SUCESS = 2;
    private static final int RICH_SCRN_LOGIN_FAIL = 3;

    /**
     * constructed function.
     */
    private RichScrnObjDownloader() {
        HandlerThread thread = new HandlerThread("DownloadThread");
        thread.start();
        mHandler = new DownloadHandler(thread.getLooper());
    }

    /**
     * Single instance.
     *@return RichScrnObjDownloader object.
     */
    public static RichScrnObjDownloader getInstance() {

        if (sInstance == null) {
            sInstance = new RichScrnObjDownloader();
        }
        return sInstance;
    }

    /**
     * Load Rich screen object by rawContactId. operation new contact, edit contact.
     * @param id  Contact Id.
     * @param lookupUri  Contact lookup Uri.
     * @param context  Context.
     */
    public void loadRichScrnByContactId(long id, Uri lookupUri, Context context) {
        mContext = context.getApplicationContext();
        long rawContactId = id;
        final String type = mContext.getContentResolver().getType(lookupUri);
        Log.d(TAG, "[loadRichScrnByContactId]id = " + id + " type=" + type);
        if (type.equals(Contacts.CONTENT_ITEM_TYPE)) {
            String selection = RawContacts.CONTACT_ID + " = " + id;
            final Cursor cursor = mContext.getContentResolver()
                    .query(RawContacts.CONTENT_URI, new String[] {
                    RawContacts.CONTACT_ID, RawContacts._ID
            }, selection, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    rawContactId = cursor.getLong(cursor.getColumnIndex(RawContacts._ID));
                    Log.d(TAG, "[loadRichScrnByContactId]New raw id = " + rawContactId);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        Message msg = mHandler.obtainMessage(MSG_GET_NUMBERS);
        msg.obj = rawContactId;
        msg.sendToTarget();

    }

    /**
     * Load Rich screen object by number. operation import vcard.
     * @param isFirst  Flag of if vcard first number.
     * @param numbers  Phone numbers.
     * @param context  Context.
     */
    public void loadRichScrnByNumbers(boolean isFirst, ArrayList<String> numbers, Context context) {
        mContext = context.getApplicationContext();
        Log.d(TAG, "numbers = " + numbers.toString() + " is First:" + isFirst
                + " login state:" + getLoginState());
        if (isFirst) {
            enqueueNumbers(numbers);
            Message initMsg = mHandler.obtainMessage(MSG_INIT_SDK);
            initMsg.sendToTarget();
            setLoginState(RICH_SCRN_LOGIN_PROCESS);
        } else if (getLoginState() == RICH_SCRN_LOGIN_SUCESS) {
            Message loadMsg = mHandler.obtainMessage(MSG_DOWNLOAD_SCRN);
            loadMsg.obj = numbers;
            loadMsg.sendToTarget();
        } else if (getLoginState() == RICH_SCRN_LOGIN_PROCESS) {
            enqueueNumbers(numbers);
        }
    }

    /**
     * Set Server login state.
     * @param state  Server login state.
     */
    public void setLoginState(int state) {
        mLoginState = state;
    }

    /**
     * get Server login state.
     * @return server login state.
     */
    public int getLoginState() {
        return mLoginState;
    }

    /**
     * get contact numbers by getAllContactNumbers.
     * @param rawContactId Contact Id.
     * @return number of raw contact id.
     */
    private synchronized ArrayList<String> getAllContactNumbers(long rawContactId) {

        ArrayList<String> numbers = new ArrayList<String>();

        String[] projections = {Data._ID, Phone.NUMBER};
        String selection = Data.MIMETYPE + " = ?"
                + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] {
                Phone.CONTENT_ITEM_TYPE,
                String.valueOf(rawContactId)
        };

        Cursor c = mContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projections, selection, selectionArgs, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                String number = c.getString(c.getColumnIndex(Phone.NUMBER));
                numbers.add(number.replaceAll(" ", ""));
            }
        }
        c.close();

        Log.d(TAG, "numbers = " + numbers.toString());

        return numbers;
    }

    /**
     * Download rich screen object of numbers queue.
     */
    private void loadRichScrnOfNumberQueue() {

        if (getNumberQueueSize() > 0 && getLoginState() == RICH_SCRN_LOGIN_SUCESS) {
            Message loadMsg = mHandler.obtainMessage(MSG_DOWNLOAD_SCRN);
            ArrayList<String> queue = new ArrayList<String>();
            queue.addAll(dequeueNumbers());
            loadMsg.obj = queue;
            loadMsg.sendToTarget();
            clearNumberQueue();
        }

    }

    /**
     * DownloadHandler: Download rich screen object handler.
     */
    private final class DownloadHandler extends Handler {

        public DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "DownloadHandler: msg = " + msg.what);
            if (null == msg) {
                return;
            }
            switch (msg.what) {

                case MSG_INIT_SDK:
                    mRichScrnPersonBiz = new RichScrnPersonBiz(mContext);
                    ResultEntity result = mRichScrnPersonBiz.init(Integer.valueOf(INIT_DISK_SIZE));
                    if (result != null && result.isSuccessed()) {
                        AuthnHelper authnHelper = new AuthnHelper(mContext);
                        authnHelper.setDefaultUI(false);
                        authnHelper.getAccessToken(APP_ID, APP_KEY, "",
                                SsoSdkConstants.LOGIN_TYPE_DEFAULT, RichScrnObjDownloader.this);
                    } else {
                        setLoginState(RICH_SCRN_LOGIN_FAIL);
                    }
                    break;

                case MSG_LOG_IN:
                    ResultEntity result1 = null;
                    Bundle data = msg.getData();
                    String token = data.getString("token");
                    result1 = mRichScrnPersonBiz.RichScrnCMCCSSOLogin(token, SOURCE_ID);
                    if (result1 != null && result1.isSuccessed()) {
                        Log.d(TAG, "Log in success");
                        setLoginState(RICH_SCRN_LOGIN_SUCESS);
                        loadRichScrnOfNumberQueue();

                    } else {
                        setLoginState(RICH_SCRN_LOGIN_FAIL);
                    }
                    break;

                case MSG_DOWNLOAD_SCRN:
                    ArrayList<String> list =  (ArrayList<String>) msg.obj;
                    Log.d(TAG, "Numbers " + list);
                    for (String phone : list) {
                        ResultEntity ret = mRichScrnPersonBiz
                                     .DownloadRichScrnObj(phone, CONTACT_PHONE_EVENT);
                        if (ret != null && ret.isSuccessed()) {
                            Log.d(TAG, phone + ret.getResMsg().toString());

                        } else {
                            Log.d(TAG, phone + " fail!!!");
                        }
                    }
                    break;

                case MSG_GET_NUMBERS:
                    long id = (long) msg.obj;
                    ArrayList<String> numbers = getAllContactNumbers(id);
                    if (numbers != null && numbers.size() > 0) {
                        enqueueNumbers(numbers);
                        Message initMsg = mHandler.obtainMessage(MSG_INIT_SDK);
                        initMsg.sendToTarget();
                        setLoginState(RICH_SCRN_LOGIN_PROCESS);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * onGetTokenComplete: call back of getAccessToken.
     * @param jsonobj  Json result.
     */
    @Override
    public void onGetTokenComplete(JSONObject jsonobj) {
        int result = jsonobj.optInt(SsoSdkConstants.VALUES_KEY_RESULT_CODE, -1);
        Log.d(TAG, "onGetTokenComplete: result:" + result);
        if (result == AuthnConstants.CLIENT_CODE_SUCCESS) {

            final String token = jsonobj.optString(SsoSdkConstants.VALUES_KEY_TOKEN, null);
            if (token != null && !token.equals("")) {
                Log.d(TAG, "onGetTokenComplete: token:" + token);
                Message loginMsg = mHandler.obtainMessage(MSG_LOG_IN);
                Bundle data = new Bundle();
                data.putString("token", token);
                loginMsg.setData(data);
                loginMsg.sendToTarget();
            } else {
                setLoginState(RICH_SCRN_LOGIN_FAIL);
            }
        } else {
            setLoginState(RICH_SCRN_LOGIN_FAIL);
        }
    }

    /**
     * getNumberQueueSize: get number queue size.
     * @return queue size.
     */
    private int getNumberQueueSize() {
        return mNumbersQueue.size();
    }

    /**
     * enqueueNumbers: enqueue numbers to queue.
     * @param numbers  Numbers ArrayList.
     */
    private void enqueueNumbers(ArrayList numbers) {
        if (getNumberQueueSize() < MAX_NUMBER_QUEUE_SIZE) {
            mNumbersQueue.addAll(numbers);
        }
    }

    /**
     * dequeueNumbers: dequeue all numbers outof queue.
     * @reurn ArrayList  Msg queue.
     */
    private ArrayList dequeueNumbers() {
        return mNumbersQueue;
    }

    /**
     * clearNumberQueue: clear msg queue.
     */
    private void clearNumberQueue() {
        mNumbersQueue.clear();
    }

}
