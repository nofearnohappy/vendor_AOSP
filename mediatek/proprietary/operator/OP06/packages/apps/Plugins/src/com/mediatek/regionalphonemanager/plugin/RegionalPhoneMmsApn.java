package com.mediatek.regionalphonemanager.plugin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.ServiceManager;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegionalPhoneMmsApn {
    private static final String TAG = "RegionalPhone";
    
    protected static final String[] PROJECTION = new String[] {
        Telephony.Carriers._ID,     // 0
        Telephony.Carriers.NAME,    // 1
        Telephony.Carriers.APN,     // 2
        Telephony.Carriers.PROXY,   // 3
        Telephony.Carriers.PORT,    // 4
        Telephony.Carriers.USER,    // 5
        Telephony.Carriers.SERVER,  // 6
        Telephony.Carriers.PASSWORD, // 7
        Telephony.Carriers.MMSC, // 8
        Telephony.Carriers.MCC, // 9
        Telephony.Carriers.MNC, // 10
        Telephony.Carriers.NUMERIC, // 11
        Telephony.Carriers.MMSPROXY,// 12
        Telephony.Carriers.MMSPORT, // 13
        Telephony.Carriers.AUTH_TYPE, // 14
        Telephony.Carriers.TYPE, // 15
        Telephony.Carriers.SOURCE_TYPE, // 16
        Telephony.Carriers.OMACPID,//17
        Telephony.Carriers.NAPID,//18
        Telephony.Carriers.PROXYID,//19
        Telephony.Carriers.BEARER, //20
    };
    
    protected static final int ID_INDEX = 0;
    protected static final int NAME_INDEX = 1;
    protected static final int APN_INDEX = 2;
    protected static final int PROXY_INDEX = 3;
    protected static final int PORT_INDEX = 4;
    protected static final int USER_INDEX = 5;
    protected static final int SERVER_INDEX = 6;
    protected static final int PASSWORD_INDEX = 7;
    protected static final int MMSC_INDEX = 8;
    protected static final int MCC_INDEX = 9;
    protected static final int MNC_INDEX = 10;
    protected static final int NUMERIC_INDEX = 11;
    protected static final int MMSPROXY_INDEX = 12;
    protected static final int MMSPORT_INDEX = 13;
    protected static final int AUTH_TYPE_INDEX = 14;
    protected static final int TYPE_INDEX = 15;
    protected static final int SOURCE_TYPE_INDEX = 16;
    protected static final int APN_ID_INDEX = 17;
    protected static final int NAP_ID_INDEX = 18;
    protected static final int PROXY_ID_INDEX = 19;
    protected static final int BEARER_INDEX = 20;
    
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mSimId;
    private Uri mUri;
    private String mNumeric;
    private int mSubId;
    
    public RegionalPhoneMmsApn(Context context, int simId, Uri uri, String numeric) {
        mContentResolver = context.getContentResolver();
        mSimId = simId;
        mUri = uri;
        mNumeric = numeric;
        int[] subIds = SubscriptionManager.getSubId(mSimId);
        if ((subIds != null) && (subIds.length != 0)) {
            mSubId = subIds[0];
        } else {
            mSubId = SubscriptionManager.getDefaultSubId();
        }
    }
    
    public long insert(final Context context, ContentValues values) {
        String id = null;
        Cursor cursor = null;

        String spn;
        String imsi;
        String pnn;
        
        ITelephonyEx mTelephonyService;
        mTelephonyService = ITelephonyEx.Stub.asInterface(ServiceManager.getService(context.TELEPHONY_SERVICE_EX));
		/*try {
			spn = mTelephonyService.getMvnoPattern(mSubId, PhoneConstants.MVNO_TYPE_SPN);
			imsi = mTelephonyService.getMvnoPattern(mSubId, PhoneConstants.MVNO_TYPE_IMSI);
			pnn = mTelephonyService.getMvnoPattern(mSubId, PhoneConstants.MVNO_TYPE_PNN);
			Log.d(TAG, "spn = " + spn);
			Log.d(TAG, "imsi = " + imsi);
			Log.d(TAG, "pnn = " + pnn);
			if (imsi != null && !imsi.isEmpty()) {
				values.put(Telephony.Carriers.IMSI, imsi);
			} else if (spn != null && !spn.isEmpty()) {
				values.put(Telephony.Carriers.SPN, spn);
			} else {
				values.put(Telephony.Carriers.PNN, pnn);
			}
		} catch (android.os.RemoteException e) {
			Log.d(TAG, "RemoteException " + e);
		}  */

        try {
            Uri newRow = mContentResolver.insert(mUri, values);
            if (newRow != null) {
                Log.d(TAG, "uri = " + newRow);
                if (newRow.getPathSegments().size() == 2) {
                    id = newRow.getLastPathSegment();
                }
            }
        } catch (SQLException e) {
            Log.d(TAG, "insert SQLException happened!");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        } 
        if (id != null) {
            return Long.parseLong(id);
        } else {
            return -1;
        }
    }
    
    /**
     * 
     * @return
     */
    public ArrayList<HashMap<String, String>> getExistMatchApnList(String apnName) {
        ArrayList<HashMap<String, String>> matchApnList = new ArrayList<HashMap<String, String>> ();        
        HashMap<String, String> map = new HashMap<String, String>();        
        String where = "numeric=? and apn=?";
        String []selectionArgs = new String[]{mNumeric, apnName};         
        Cursor cursor = mContentResolver.query(
                mUri, 
                new String[] {Telephony.Carriers._ID}, 
                where, 
                selectionArgs, 
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                map.put("apnId", cursor.getString(0));  
                matchApnList.add(map);
                cursor.moveToNext();
            }// end of while
            cursor.close();
        }
        
        return matchApnList;
    }
    
    public void update(final Context context, ContentValues values, String apnIndex) {        
        if(apnIndex != null) {
            try {
                Uri existApnURI = ContentUris.withAppendedId(mUri, Integer.parseInt(apnIndex));
                int result = mContentResolver.update(existApnURI, values, null, null);
                Log.i(TAG, "result is "+result);

            } catch(NumberFormatException e) {
                Log.e(TAG, "parseInt failed, the id is "+apnIndex);
            }
        }
    }
}
