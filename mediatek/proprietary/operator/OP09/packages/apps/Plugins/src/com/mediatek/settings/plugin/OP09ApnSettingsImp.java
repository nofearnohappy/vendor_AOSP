package com.mediatek.settings.plugin;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.op09.plugin.R;
import com.mediatek.settings.ext.DefaultApnSettingsExt;

/**
 * APN CT feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IApnSettingsExt")
public class OP09ApnSettingsImp extends DefaultApnSettingsExt {

    private static final String TAG = "OP09ApnSettingsImp";
    private EditTextPreference mPppEditTextPreference;
    private Context mContext;

    private static final String CHINA_NW_MCC = "460";
    private static final String MACOO_NW_MCC = "455";
    private static final String MANUAL_MODE = "manual_mode";
    private static final int APN_AUTO_MODE = 0;
    private static final int APN_MANUAL_MODE = 1;
    private static final String CT_NUMERIC_1 = "46011";
    private static final String CT_NUMERIC_2 = "46003";
    private static final String CT_CTWAP = "CTWAP";
    private static final String CT_CTNET = "CTNET";
    private static final int SOURCE_TYPE_DEFAULT = 0;
    private SwitchPreference mSwitchPreference;
    /**
     * Constructor method.
     * @param context is Settings's context.
     */
    public OP09ApnSettingsImp(Context context) {
        mContext = context;
    }

    /**
     * Add a preference in the prefernce screen according to the slotId.
     * CT need PPP dialog for connect internet, so add PPP,
     * Customize apn titles
     * @param subId sub id
     * @param root PPP's parent
     */
    @Override
    public void customizePreference(int subId, PreferenceScreen root) {
        Log.d(TAG, "customizePreference subId = " + subId + "  slotId = "
              + SubscriptionManager.getSlotId(subId));
        if (PhoneConstants.SIM_ID_1 == SubscriptionManager.getSlotId(subId)) {
            mPppEditTextPreference = new EditTextPreference(root.getContext(),
                    null);
            mPppEditTextPreference.setKey(mContext.getResources().getString(
                    R.string.ppp_dialing));
            mPppEditTextPreference.setTitle(mContext.getResources().getString(
                    R.string.ppp_dialing));
            mPppEditTextPreference.setDialogTitle(mContext.getResources()
                    .getString(R.string.ppp_dialing));
            root.addPreference(mPppEditTextPreference);

            // PPP Phone number already added.
            root.findPreference("apn_name").setTitle(
                    mContext.getString(R.string.apn_name));
            root.findPreference("apn_apn").setTitle(
                    mContext.getString(R.string.apn_apn));
            root.findPreference("auth_type").setTitle(
                    mContext.getString(R.string.apn_auth_type));
            root.findPreference("apn_user").setTitle(
                    mContext.getString(R.string.apn_user));
            root.findPreference("apn_password").setTitle(
                    mContext.getString(R.string.apn_password));
        }
    }

    /**
     * Customize apn projection, such as add Telephony.Carriers.PPP Called at
     * onCreate in ApnEditor.
     * @param projection old String[]
     * @return new String[] add PPP
     */
    @Override
    public String[] customizeApnProjection(String[] projection) {
        Log.d(TAG, "customizeApnProjection");

        if (Telephony.Carriers.PPP.equals(projection[projection.length - 1])) {
            return projection;
        }
        Log.d(TAG, "customizeApnProjection, Carriers");
        String[] newStrs = new String[projection.length + 1];
        for (int i = 0; i < projection.length; i++) {
            newStrs[i] = projection[i];
        }
        newStrs[newStrs.length - 1] = Telephony.Carriers.PPP;
        return newStrs;
    }

    /**
     * Save the added apn values called when save the added apn vaule in apnEditor.
     * @param contentValues contentValues
     */
    @Override
    public void saveApnValues(ContentValues contentValues) {
        contentValues.put(Telephony.Carriers.PPP, getPppDialingText());
    }

    /**
     * Set the preference text and summary according to the slotId.
     * called at update UI, in ApnEditor.
     * @param subId sub id
     * @param text text and summary
     */
    @Override
    public void setPreferenceTextAndSummary(int subId, String text) {
        if (PhoneConstants.SIM_ID_1 == SubscriptionManager.getSlotId(subId)) {
            mPppEditTextPreference.setText(text);
            mPppEditTextPreference.setSummary(text);
        }
    }

    /**
     * Update the customized status(enable , disable).
     * Called at update screen status
     * @param subId sub id
     * @param root PPP's parent
     */
    @Override
    public void updateFieldsStatus(int subId, int sourceType, PreferenceScreen root) {
        Log.d(TAG, "updateFieldsStatus subId=" + subId + " sourceType= " + sourceType);

        if (sourceType != SOURCE_TYPE_DEFAULT) {
            return;
        }
        if (PhoneConstants.SIM_ID_1 == SubscriptionManager.getSlotId(subId)) {
            root.findPreference("apn_mms_proxy").setEnabled(false);
            root.findPreference("apn_mms_port").setEnabled(false);
            root.findPreference("apn_mmsc").setEnabled(false);
            root.findPreference("apn_mcc").setEnabled(false);
            root.findPreference("apn_mnc").setEnabled(false);
        }
    }

    /**
     * M: For CT fature :add ppp_dialing use class.
     * ApnEditor return the ppp_dialing's text
     */
    private String getPppDialingText() {
        if (mPppEditTextPreference != null) {
            return mPppEditTextPreference.getText();
        } else {
            return mContext.getResources().getString(R.string.apn_not_set);
        }
    }

    // For CT-A feature to hide CTWAP in roaming state
    //@Override
    public String getFillListQuery(String numeric, int subId) {
        String result;
        String sqlStr = "";
        String apn = "";
        String sourceType = "";
        try {
            ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            String mvnoType = telephonyEx.getMvnoMatchType(subId);
            String mvnoPattern = telephonyEx.getMvnoPattern(subId, mvnoType);
            // If mvnoType or mvnoPattern is null, should replace with ''
            sqlStr = " mvno_type=\'" + replaceNullString(mvnoType)
                + "\'" + " and mvno_match_data=\"" + replaceNullString(mvnoPattern) + "\"";
        }  catch (android.os.RemoteException e) {
            Log.d(TAG, "RemoteException " + e);
        }
        Log.i(TAG, "subId = " + subId + " slotid = " + SubscriptionManager.getSlotId(subId));
        if (PhoneConstants.SIM_ID_1 == SubscriptionManager.getSlotId(subId)) {
            ///M: for ap irat feature,numeric need get from PROPERTY_OPERATOR_NUMERIC,
            ///when has lte and ctwap,use lte
            String plmnNumeric =
                SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC);
            Log.d(TAG, "plmnNumeric = " + plmnNumeric);
            if (null != numeric && (numeric.contains(CT_NUMERIC_1)
                    || numeric.contains(CT_NUMERIC_2))) {
                Log.d(TAG, "numeric = " + numeric);
                int pstype = getNetworkType(subId);
                if (plmnNumeric.contains(CT_NUMERIC_1) && plmnNumeric.contains(CT_NUMERIC_2)) {
                    if (pstype == TelephonyManager.NETWORK_TYPE_LTE ||
                        pstype == TelephonyManager.NETWORK_TYPE_EHRPD) {
                        numeric = CT_NUMERIC_1;
                    } else {
                        //In case catch 4G network, but data actually connect to 3G, so show 3G apn list
                        numeric = CT_NUMERIC_2;
                    }
                } else if (plmnNumeric.contains(CT_NUMERIC_1)) {
                    // 4G only data, so numeric is only 46011
                    numeric = CT_NUMERIC_1;
                } else if (plmnNumeric.contains(CT_NUMERIC_2)) {
                    // In CT case if data network is EHRPD, need use CTLTE apn data
                    if (pstype == TelephonyManager.NETWORK_TYPE_EHRPD) {
                        numeric = CT_NUMERIC_1;
                    } else {
                        numeric = CT_NUMERIC_2;
                    }
                } else {
                    Log.d(TAG, "plmnNumeric not contains 46003 or 46011, as ROAMING");
                    final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                        mContext.TELEPHONY_SERVICE);
                    numeric = tm.getNetworkOperatorForSubscription(subId);
                    if (numeric == null) {
                    numeric = CT_NUMERIC_2;
                }
                    Log.d(TAG, "ROAMING numeric = " + numeric);
                    sqlStr = sqlStr + " and apn <> \"ctwap\"";
                }
                Log.i(TAG, "plmnNumeric = " + plmnNumeric + " numeric = " + numeric);
                sqlStr = sqlStr + " or sourceType = \"1\"";
                result = "numeric=\"" + numeric + "\" and " + sqlStr ;
                Log.d(TAG, "getFillListQuery result=" + result);
                return result;
            }
            if (plmnNumeric != null && plmnNumeric.length() >= 3
                    && !plmnNumeric.startsWith(CHINA_NW_MCC)
                    && !numeric.startsWith(MACOO_NW_MCC)) {
                Log.i(TAG, "ROAMING");
                apn += " and apn <> \"ctwap\"";
                result = "numeric=\"" + numeric + "\" and "
                    + "((" + sqlStr + apn + ")" + " or (sourceType = \"1\"))" ;
                Log.d(TAG, "getFillListQuery roaming result=" + result);
                return result;
            }
        }
        result = "numeric=\"" + numeric + "\" and " + sqlStr ;
        Log.d(TAG, "getFillListQuery result=" + result);
        return result;
    }
    // Since APN list is for current data network, so getDataNetworkType will be more accurate
    private int getNetworkType(int subId) {
        int pstype = TelephonyManager.SIM_STATE_UNKNOWN;
        try {
            ITelephonyEx tphony = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            if (null != tphony) {
                Bundle bd = null;
                bd = tphony.getSvlteServiceState(subId);
                if (null != bd) {
                    ServiceState ss = ServiceState.newFromBundle(bd);
                    if (ss != null) {
                        Log.i(TAG, "ss = " + ss);
                        pstype = ss.getDataNetworkType();
                    }
                }
            }
        } catch (android.os.RemoteException e) {
            Log.d(TAG, "RemoteException " + e);
        }
       Log.d(TAG, "pstype = " + pstype);
       return pstype;
    }

    private String replaceNullString(String origString) {
        if (origString == null) {
            return "";
        } else {
            return origString;
        }
    }

    //TODO: Fix build error, should add API in plugin interface @Override
    public void customizeSwitchPreference(int subId, PreferenceGroup apnList) {

        int slotId = SubscriptionManager.getSlotId(subId);
        Log.d(TAG, "customizeSwitchPreference subId = " + subId + "  slotId = " + slotId);
        if (PhoneConstants.SIM_ID_1 == slotId) {
            mSwitchPreference = new SwitchPreference(apnList.getContext());
            mSwitchPreference.setTitle(mContext.getString(R.string.manual_selection_mode));
            mSwitchPreference.setKey(MANUAL_MODE);
            apnList.addPreference(mSwitchPreference);
            int apnPatternMode =  Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.CDMA_APN_MANUAL_MODE, APN_AUTO_MODE);
            Log.d(TAG, "customizeSwitchPreference apnPatternMode = " + apnPatternMode);

            mSwitchPreference.setChecked(apnPatternMode != APN_AUTO_MODE);

            mSwitchPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference != null && MANUAL_MODE.equals(preference.getKey())) {
                        SwitchPreference switchPreference = (SwitchPreference) preference;
                      Log.d(TAG, "onPreferenceChange switchPreference.isChecked() = "
                              + switchPreference.isChecked());
                      Settings.Global.putInt(mContext.getContentResolver(),
                              Settings.Global.CDMA_APN_MANUAL_MODE,
                              (switchPreference.isChecked() ? APN_MANUAL_MODE : APN_AUTO_MODE));
                      int apnPatternModeSetGet =
                          Settings.Global.getInt(mContext.getContentResolver(),
                              Settings.Global.CDMA_APN_MANUAL_MODE, APN_AUTO_MODE);
                      Log.d(TAG, "onPreferenceChange apnPatternModeSetGet = "
                              + apnPatternModeSetGet);
                      return true;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Customize the unselected APN.
     * @param mnoApnList the unselectable mno apn list.
     * @param mvnoApnList the unselectable mvno apn list.
     * @param subId the subscription ID.
     */
    @Override
    public void customizeUnselectableApn(ArrayList<Preference> mnoApnList,
        ArrayList<Preference> mvnoApnList, int subId) {
        SvlteUiccUtils utils = SvlteUiccUtils.getInstance();
        boolean isCtCard = utils.isRuimCsim(SubscriptionManager.getSlotId(subId));
        Log.d(TAG, "customizeUnselectableApn() mnoApnList = " + mnoApnList.size()
              + " mvnoApnList = " + mvnoApnList.size());

        if (isCtCard) {
            mnoApnList.clear();
        }
        Log.d(TAG, "customizeUnselectableApn() subId = " + subId + " isCT = " + isCtCard);
    }

    /**
     * Update the APN name.
     * @param name the APN name to be updated.
     * @param sourcetype the APN source type.
     * @return the updated name.
     */
    @Override
    public String updateApnName(String name, int sourcetype) {
        String name_t = name;

        if (sourcetype == 1) {
            return name_t;
        }
        if (CT_CTWAP.equals(name)) {
            name_t = mContext.getString(R.string.ctAPN_name_ctwap);
        } else if (CT_CTNET.equals(name)) {
            name_t = mContext.getString(R.string.ctAPN_name_ctnet);
        }

        return name_t;
    }

    /**
     * set MVNO preference state, called in ApnEditor.
     * @param mvnoType The mvno preference to set state
     * @param mvnoMatchData The mvnoMatch preference to set state
     */
    @Override
    public void setMvnoPreferenceState(Preference mvnoType, Preference mvnoMatchData) {
        Log.d(TAG, "setMvnoPreferenceState()");
        if ("mvno_type".equals(mvnoType.getKey())) {
            mvnoType.setEnabled(false);
            Log.d(TAG, "Disable MVNO type preference");
        }
        if ("mvno_match_data".equals(mvnoMatchData.getKey())) {
            mvnoMatchData.setEnabled(false);
            Log.d(TAG, "Disable MVNO match data preference");
        }
    }
}
