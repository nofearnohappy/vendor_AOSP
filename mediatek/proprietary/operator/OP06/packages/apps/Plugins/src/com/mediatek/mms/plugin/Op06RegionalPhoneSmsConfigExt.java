package com.mediatek.mms.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.common.regionalphone.RegionalPhone;
import com.mediatek.mms.callback.IMmsAppCallback;

public class Op06RegionalPhoneSmsConfigExt {
    private final static String TAG = "Op06RegionalPhoneSmsConfigExt";

    private Context mContext = null;
    private HashMap<String, String> mValues = null;
    private String mCreateMode;
    private String mSmsCenter;
    private String mTimeStamp;
    
    //private Uri mUri = Uri.parse("content://com.mediatek.provider.regionalphone/mms_sms");
	private Uri mUri = RegionalPhone.MMS_SMS_URI; 

    private RegionalPhoneContentObserver mObserver = null;
    private IMmsAppCallback mMmsApp;

    public Op06RegionalPhoneSmsConfigExt(Context context) {
        mContext = context;
        mValues = new HashMap<String, String> ();
    }

    /**
     * Implementation method of in interface IMmsSettingsExt
     * @param host
     */
    public void init(IMmsAppCallback mmsApp) {
        if (getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
            mMmsApp = mmsApp;

            if (querySmsParameter()) {
                Log.i(TAG, "there has sms parameter data in regionalphonemanager database. \n");
                String lastTimeStamp = getSmsTimeStamp(mContext);
                int compareRst = mTimeStamp.compareTo(lastTimeStamp);
                if (compareRst > 0) {
                    mMmsApp.setSmsValues(mValues);
                    mMmsApp.registerSmsStateReceiver();
                    setSmsTimeStamp(mContext, mTimeStamp);
                }
            } else {
                Log.i(TAG, "there has not sms parameter data in regionalphonemanager databse. \n");
                Log.i(TAG, "Register contentobserver to listen database change. \n");
				mObserver = new RegionalPhoneContentObserver(new Handler());
                mContext.getContentResolver().registerContentObserver(mUri, true, mObserver);
            }
        }
    }
    
    /**
     * query sms parameter from RegionalPhoneManager database
     * @return ture if has data, or return false
     */
    private boolean querySmsParameter() {           
        ContentResolver cr = mContext.getContentResolver();
        //String []proj = new String[]{"CreationMode", "CNumber", "mcc_mnc_timestamp"};
		String []proj = new String[]{RegionalPhone.MMS_SMS.MMS_CREATION_MODE, RegionalPhone.MMS_SMS.SMS_C_NUMBER, RegionalPhone.MMS_SMS.MCC_MNC_TIMESTAMP};
        Cursor cursor = null;
        cursor = cr.query(mUri, proj, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            //mCreateMode = cursor.getString(cursor.getColumnIndex("CreationMode"));
            //mSmsCenter = cursor.getString(cursor.getColumnIndex("CNumber"));
            //mTimeStamp = cursor.getString(cursor.getColumnIndex("mcc_mnc_timestamp"));
			mCreateMode = cursor.getString(cursor.getColumnIndex(RegionalPhone.MMS_SMS.MMS_CREATION_MODE));
            mSmsCenter = cursor.getString(cursor.getColumnIndex(RegionalPhone.MMS_SMS.SMS_C_NUMBER));
            mTimeStamp = cursor.getString(cursor.getColumnIndex(RegionalPhone.MMS_SMS.MCC_MNC_TIMESTAMP));
            mCreateMode = mCreateMode.toUpperCase();
            if (!mCreateMode.equals("FREE") && !mCreateMode.equals("RESTRICTED") && !mCreateMode.equals("WARNING")) {
            	mCreateMode = "FREE";
            }
            mValues.put("creationmode", mCreateMode);
            mValues.put("servicecenter", mSmsCenter);
            cursor.close();
            
            return true;
        }
        
        Log.i(TAG, "Query sms parameter data from regionalphonemanager failed. \n");

        return false;                
    }
    
    /**
     * Implementation method in interface IMmsSettingsExt
     * @return  sms center number query from RegionalPhoneManager
     */
    public String getSmsServiceCenter() {        
        return mSmsCenter;
    }   
    
    /**
     * get sms parameter time stamp
     * @param context
     * @return
     */
    private String getSmsTimeStamp(Context context) {
        SharedPreferences sp = context.getSharedPreferences("sms_time_stamp", Context.MODE_PRIVATE);
        return sp.getString("sms_time_stamp", "0");
    }
    
    /**
     * save sms parameter time stamp
     * @param context
     * @param timeStamp
     */
    private void setSmsTimeStamp(Context context, String timeStamp) {
        SharedPreferences sp = context.getSharedPreferences("sms_time_stamp", Context.MODE_PRIVATE);
        sp.edit().putString("sms_time_stamp", timeStamp).commit(); 
    }
    
    /**
     * 
     * @author mtk54378
     *
     */
    private class RegionalPhoneContentObserver extends ContentObserver { 

        public RegionalPhoneContentObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            
            querySmsParameter();
            mMmsApp.setSmsValues(mValues);
            mMmsApp.registerSmsStateReceiver();
            setSmsTimeStamp((Context)mMmsApp, mTimeStamp);
            
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }         
    }
    
    /**
     * get Sim card state
     * @return
     */
    private int getSimState() {
        TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimState();
    }
}
