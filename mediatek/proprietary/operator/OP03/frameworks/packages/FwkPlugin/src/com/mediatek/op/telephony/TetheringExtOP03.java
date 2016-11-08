package com.mediatek.op.telephony;

import android.text.TextUtils;
import android.net.Uri;
import android.database.Cursor;
import android.provider.Telephony;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.ServiceManager;
import android.os.SystemProperties;
import java.util.ArrayList;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.common.telephony.IApnContext;
import com.mediatek.common.telephony.IApnSetting;

public class TetheringExtOP03 extends TetheringExt {
    private static final Uri PREFER_TETHERING_APN_URI = Uri.parse("content://telephony/carriers/prefertetheringapn");
    private static final Uri PREFER_TETHERING_APN_URI2 = Uri.parse("content://telephony/carriers_gemini/prefertetheringapn");
    private static final String APN_ID = "apn_id";

    private Context mContext;
    private int mSimId;
    private ApnSetting mPreferredTetheringApn;
    private boolean mCanSetPreferTetheringApn;

    public TetheringExtOP03(Context context, int simId) {
        mContext = context;
        mSimId = simId;
    }

    public void onDataSetupComplete(IApnContext itApnContext) {
        ApnContext apnContext = (ApnContext) itApnContext;
        if (TextUtils.equals(apnContext.getApnType(), PhoneConstants.APN_TYPE_HIPRI) &&
            mCanSetPreferTetheringApn && mPreferredTetheringApn == null)
        {
            mPreferredTetheringApn = apnContext.getApnSetting();
            if (mPreferredTetheringApn != null) {
                setPreferredTetheringApn(mPreferredTetheringApn.id);
            }
        }
    }

    public void onCreateAllApnList(ArrayList<IApnSetting> allApnSettings, String operator) {
        mPreferredTetheringApn = (ApnSetting) getPreferredTetheringApn(allApnSettings);
        log("Get preferred tethering APN: " + mPreferredTetheringApn);
        if (mPreferredTetheringApn != null && !mPreferredTetheringApn.numeric.equals(operator)) {
            mPreferredTetheringApn = null;
            setPreferredTetheringApn(-1);
        }
    }

    public void onBuildWaitingApns(String requestedApnType, ArrayList<IApnSetting> apnSettingList, String operator) {
        if (PhoneConstants.APN_TYPE_HIPRI.equals(requestedApnType)) {
            if (mCanSetPreferTetheringApn && mPreferredTetheringApn != null) {
                if (mPreferredTetheringApn.numeric.equals(operator)) {
                    log("Waiting APN set to preferred  Tethering APN: " + mPreferredTetheringApn);
                    //apnSettingList.add(mPreferredTetheringApn); //TODO akhil
                } else {
                    setPreferredTetheringApn(-1);
                    mPreferredTetheringApn = null;
                }
            }
        }
    }

    public IApnSetting getPreferredTetheringApn(ArrayList<IApnSetting> allApnSettings) {
        if (allApnSettings.isEmpty())
            return null;

        Uri queryPreferApnUri = null;
        if (SystemProperties.getBoolean("ro.mtk_gemini_support", false) && mSimId == 1) {
            queryPreferApnUri = PREFER_TETHERING_APN_URI2;
        } else {
            queryPreferApnUri = PREFER_TETHERING_APN_URI;
        }

        Cursor cursor = mContext.getContentResolver().query(
                queryPreferApnUri, new String[] { "_id", "name", "apn" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            try {
                int nSlotId = 0;
                ITelephonyEx telephony = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
                mCanSetPreferTetheringApn = true;
                if (telephony != null) {
                    if (telephony.isTestIccCard(nSlotId)) {
                        log("Test SIM and cannot set prefer tethering APN");
                        mCanSetPreferTetheringApn = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mCanSetPreferTetheringApn = false;
        }

        if (mCanSetPreferTetheringApn && cursor.getCount() > 0) {
            int position = 0;
            cursor.moveToFirst();
            position = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
            for (IApnSetting obj : allApnSettings) {
                ApnSetting apnSetting = (ApnSetting) obj;
                if (apnSetting.id == position) {
                    cursor.close();
                    //return apnSetting;//TODO akhil
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    public void setPreferredTetheringApn(int position) {
        if (!mCanSetPreferTetheringApn) {
            return;
        }
        log("set preferred tethering APN: " + position);
        ContentResolver resolver = mContext.getContentResolver();

        if (SystemProperties.getBoolean("ro.mtk_gemini_support", false)) {
            if (mSimId == 0) {
                resolver.delete(PREFER_TETHERING_APN_URI, null, null);
            } else {
                resolver.delete(PREFER_TETHERING_APN_URI2, null, null);
            }
        } else {
            resolver.delete(PREFER_TETHERING_APN_URI, null, null);
        }

        if (position >= 0) {
            ContentValues values = new ContentValues();
            values.put(APN_ID, position);

            if (SystemProperties.getBoolean("ro.mtk_gemini_support", false)) {
                if (mSimId == 0) {
                    resolver.insert(PREFER_TETHERING_APN_URI, values);
                } else {
                    resolver.insert(PREFER_TETHERING_APN_URI2, values);
                }
            } else {
                resolver.insert(PREFER_TETHERING_APN_URI, values);
            }
        }
    }
}
