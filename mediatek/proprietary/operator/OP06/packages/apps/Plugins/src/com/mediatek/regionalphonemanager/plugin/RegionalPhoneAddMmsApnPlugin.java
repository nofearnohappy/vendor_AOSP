package com.mediatek.regionalphonemanager.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.regionalphone.RegionalPhone;
import com.mediatek.rpm.ext.DefaultRegionalPhoneAddMmsApnExt;

import java.util.ArrayList;
import java.util.HashMap;

@PluginImpl(interfaceName="com.mediatek.rpm.ext.IRegionalPhoneAddMmsApnExt")
public class RegionalPhoneAddMmsApnPlugin extends DefaultRegionalPhoneAddMmsApnExt {
    private static final String TAG = "RegionalPhone";
    
    private static final int SIM_CARD_SINGLE = 0;
    private static final int SIM_CARD_UNDEFINED = -1;
    
    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM1 = "content://telephony/carriers_sim1/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM2 = "content://telephony/carriers_sim2/preferapn";

    private Bundle mApnParameter = null;  //this parameter will include apn parameters
    
    private int mSimId;
    private long mApnToUseId = -1;
    private boolean mResult;
    
    private Uri mUri;
    private String mNumeric;
    private Uri mPreferedUri;
    
    private static final String SIM_ID = "SIMID";
    private static final String APN_NAME = "NAP-NAME";     //name
    private static final String APN_APN = "NAP-ADDRESS";   //apn
    private static final String APN_PROXY = "PXADDR";     //proxy
    private static final String APN_PORT = "PORTNBR";     //port
    private static final String APN_USERNAME = "AUTHNAME";  //username
    private static final String APN_PASSWORD = "AUTHSECRET";  //password
    private static final String APN_SERVER = "SERVER";   //server
    private static final String APN_MMSC = "MMSC";      //mmsc
    private static final String APN_MMS_PROXY = "MMS-PROXY";   //mms proxy
    private static final String APN_MMS_PORT = "MMS-PORT";   //mms port
    private static final String APN_AUTH_TYPE = "AUTHTYPE";  //auth type
    private static final String APN_TYPE = "APN-TYPE";    //type
    private static final String APN_ID = "APN-ID";   //type
    private static final String APN_NAP_ID = "NAPID";  //type
    private static final String APN_PROXY_ID = "PROXY-ID";  //type
    private static final String APN_BEARER = "bearer";     //bearer
    
    private String mName;
    private String mApn;
    private String mProxy;
    private String mPort;
    private String mUserName;
    private String mPassword;
    private String mServer;
    private String mMmsc;
    private String mMmsProxy;
    private String mMmsPort;
    private String mAuthType;
    private String mType;
    private String mApnId;
    private String mMcc;
    private String mMnc;
    private String mNapId;
    private String mProxyId;
    private int mBearer;
    
    private static int sAuthType = -1;
    
    private RegionalPhoneMmsApn mRegionalApn;
    
    private Context mContext;
    
    public RegionalPhoneAddMmsApnPlugin(Context context) {
        mContext = context;
    }    
    
    /**
     * add an apn record to telephony.db
     * @param context
     * @return  success return true, or return false
     */
    public boolean addMmsApn(Context context) {
        
        if (queryMatchMmsApn(context)) {
            Log.i(TAG, "enter the add mms apn plugin. \n");

            if (initState(mApnParameter)) {
                extractAPN(mApnParameter);
                ContentValues values = new ContentValues();                
                validateProfile(values);                
                insertAPN(context, values);                
                if (mApnToUseId != -1) {                
                    mResult = setCurrentApn(context, mApnToUseId, mPreferedUri);                
                }
                
                return true; 
            }            
        }
        
        return false; 
    }
    
    /**
     * query contentprovider of mms apn in regionalphonemanager
     * @param context
     * @return: true or false
     */
    private boolean queryMatchMmsApn(Context context) {        
		Uri uri = RegionalPhone.APN_URI;
        ContentResolver cr = context.getContentResolver();		
		String []proj = new String[]{RegionalPhone.APN.MMS_NAME, RegionalPhone.APN.MMS_SERVER, RegionalPhone.APN.MMS_GPRS_APN, 
        							RegionalPhone.APN.SMS_PREFERRED_BEARER, RegionalPhone.APN.MMS_PROXY, RegionalPhone.APN.MMS_PORT, "mcc_mnc_timestamp"};
        Cursor cursor = null;
		cursor = cr.query(uri, proj, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Log.i("RegionalPhone", "there has mms apn data in regional phone manager. \n");
            mApnParameter = new Bundle();
            mApnParameter.putInt(SIM_ID, SIM_CARD_SINGLE);   //the sim card symbol
			mApnParameter.putString(APN_NAME, cursor.getString(cursor.getColumnIndex(RegionalPhone.APN.MMS_NAME)));    //apn name
            mApnParameter.putString(APN_APN, cursor.getString(cursor.getColumnIndex(RegionalPhone.APN.MMS_GPRS_APN)));  //apn
            mApnParameter.putString(APN_MMSC, cursor.getString(cursor.getColumnIndex(RegionalPhone.APN.MMS_SERVER)));   // mmsc address            
            mApnParameter.putString(APN_MMS_PROXY, cursor.getString(cursor.getColumnIndex(RegionalPhone.APN.MMS_PROXY)));   //mms proxy
            mApnParameter.putString(APN_MMS_PORT, String.valueOf(cursor.getInt(cursor.getColumnIndex(RegionalPhone.APN.MMS_PORT))));   //mms port
            String bearer = cursor.getString(cursor.getColumnIndex(RegionalPhone.APN.SMS_PREFERRED_BEARER));  //mms bearer

            Log.i(TAG, "bearer is " + bearer);
            if (bearer.equalsIgnoreCase("LTE")) {
                mApnParameter.putInt(APN_BEARER, 14);
            } else if (bearer.equalsIgnoreCase("eHRPD")) {
                mApnParameter.putInt(APN_BEARER, 13);
            } else {
                mApnParameter.putInt(APN_BEARER, 0);
            }            
            
			mApnParameter.putString(APN_TYPE, "mms");    // apn type is 'mms'
            String mccMncStamp = cursor.getString(cursor.getColumnIndex("mcc_mnc_timestamp"));
			mNumeric = mccMncStamp.substring(0, 5);  
            
            cursor.close();
            
            return true;
        }
        
		Log.i(TAG, "Query mms apn parameter data from regionalphonemanager failed. \n");

        return false;        
    }
    
    /**
     * init the insert or update Uri according to simId
     * @param bundle
     * @return
     */
    private boolean initState(Bundle bundle) {
	mSimId = bundle.getInt(SIM_ID, SIM_CARD_UNDEFINED); 
	int mSubId;       
        switch (mSimId) {
            case SIM_CARD_SINGLE:
                mUri = Telephony.Carriers.CONTENT_URI;
                //mPreferedUri = Uri.parse(PREFERRED_APN_URI);
                int[] subIds = SubscriptionManager.getSubId(mSimId);
                if ((subIds != null) && (subIds.length != 0)) {
                    mSubId = subIds[0];
                } else {
                    mSubId = SubscriptionManager.getDefaultSubId();
                }
                mPreferedUri = ContentUris.withAppendedId( Uri.parse(PREFERRED_APN_URI), mSubId);
                break;
            default:
                break;   
        }
        
        return getMccMnc();
    }
    
    /**
     * extra mcc and mnc
     * @return
     */
    private boolean getMccMnc() {
        // MCC is first 3 chars and then in 2 - 3 chars of MNC

        if (mNumeric != null && mNumeric.length() > 4) {
            // Country code
            String mcc = mNumeric.substring(0, 3);
            // Network code
            String mnc = mNumeric.substring(3);
            // Auto populate MNC and MCC for new entries, based on what SIM reports
            mMcc = mcc;
            mMnc = mnc;
            return true;

        } else {
            return false;
        }
    }
    
    /**
     * extra apn parameters from Bundle object
     * @param bundle
     */
    private void extractAPN(Bundle bundle) {         
        //apn parameters
        mName = bundle.getString(APN_NAME, "unName");         
        mApn = bundle.getString(APN_APN); 
        mProxy = bundle.getString(APN_PROXY);
        mPort = bundle.getString(APN_PORT);
        mUserName = bundle.getString(APN_USERNAME);
        mPassword = bundle.getString(APN_PASSWORD);
        mAuthType = bundle.getString(APN_AUTH_TYPE);
        mServer = bundle.getString(APN_SERVER);
 
        mMmsc = bundle.getString(APN_MMSC);//MMSC
        mMmsProxy = bundle.getString(APN_MMS_PROXY);//MMSC proxy
        mMmsPort = bundle.getString(APN_MMS_PORT);//MMSC port
        mType = bundle.getString(APN_TYPE);//type
        mApnId = bundle.getString(APN_ID);//apnId:should be unique
        mNapId = bundle.getString(APN_NAP_ID);
        mProxyId = bundle.getString(APN_PROXY_ID);
//      mApnId =  String.valueOf(intent.getLongExtra(APN_ID, -1l));  
        mBearer = bundle.getInt(APN_BEARER);
    }
    
    /**
     * construct insert values
     * @param values
     * @return
     */
    private boolean validateProfile(ContentValues values) {        
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.NAME_INDEX], mName);
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.APN_INDEX], checkNotSet(mApn));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.PROXY_INDEX], checkNotSet(mProxy));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.PORT_INDEX], checkNotSet(mPort));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.USER_INDEX], checkNotSet(mUserName));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.SERVER_INDEX], checkNotSet(mServer));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.PASSWORD_INDEX], checkNotSet(mPassword));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.MMSC_INDEX], checkNotSet(mMmsc));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.MCC_INDEX], mMcc);
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.MNC_INDEX], mMnc);
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.MMSPROXY_INDEX], checkNotSet(mMmsProxy));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.MMSPORT_INDEX], checkNotSet(mMmsPort));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.AUTH_TYPE_INDEX], sAuthType);
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.TYPE_INDEX], checkNotSet(mType));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.SOURCE_TYPE_INDEX], 0);  
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.APN_ID_INDEX], checkNotSet(mApnId));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.NAP_ID_INDEX], checkNotSet(mNapId));
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.PROXY_ID_INDEX], checkNotSet(mProxyId));        
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.NUMERIC_INDEX], mNumeric);
        values.put(RegionalPhoneMmsApn.PROJECTION[RegionalPhoneMmsApn.BEARER_INDEX], mBearer);

        return true;
    }
    
    private String checkNotSet(String value) {
        if (value == null || value.length() == 0) {
            return "";
        } else {
            return value;
        }
    }
    
    private void insertAPN(Context context, ContentValues values) {
        boolean isApnExisted = false;
        boolean isMmsApn = "mms".equalsIgnoreCase(mType);
        
        mRegionalApn = new RegionalPhoneMmsApn(context, mSimId, mUri, mNumeric);
        ArrayList<HashMap<String, String>> matchApnList = mRegionalApn.getExistMatchApnList(mApn);
        int sizeApn = matchApnList.size();
        
        if (sizeApn > 0) {
          isApnExisted = true;  
          mResult = true;
          HashMap<String, String> map = matchApnList.get(0);
          if (!isMmsApn) {
              mApnToUseId = Long.parseLong(map.get("apnId"));  
          }
        } 
        
        if (!isApnExisted) {
            
            long id = mRegionalApn.insert(context, values);
            if (id != -1) {
                mResult = true;
                if (!isMmsApn) {
                    mApnToUseId = id;
                }
            }
        } else {
            HashMap<String, String> map = matchApnList.get(0);
            String apnIndex = map.get("apnId");
            mRegionalApn.update(context, values, apnIndex);
        }
    }
    
    private boolean setCurrentApn(final Context context, final long apnToUseId, final Uri preferedUri) {
        int row = 0;
        ContentValues values = new ContentValues();
        values.put("apn_id", apnToUseId);
        ContentResolver mContentResolver = context.getContentResolver();
        try {
            row = mContentResolver.update(preferedUri, values, null, null);
        } catch (SQLException e) {
            Log.d("@M_" + TAG, "SetCurrentApn SQLException happened!");
        }
        return (row > 0) ? true : false;
    }
}
