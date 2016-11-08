package com.mediatek.engineermode.networkselect;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.ModemCategory;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

import java.util.Arrays;

/**
 *
 * For setting network mode.
 * @author mtk54043
 *
 */
public class NetworkSelectActivity extends Activity {
    private static final String TAG = "EM/NetworkMode";
    private static final int EVENT_QUERY_NETWORKMODE_DONE = 101;
    private static final int EVENT_SET_NETWORKMODE_DONE = 102;

    private static final int MODEM_FDD = 1;
    private static final int MODEM_TD = 2;
    private static final int MODEM_NO3G = 3;

    private static final int MODEM_MASK_WCDMA = 0x04;
    private static final int MODEM_MASK_TDSCDMA = 0x08;

    private static final int INDEX_WCDMA_PREFERRED = 0;
    private static final int INDEX_GSM_ONLY = 1;
    private static final int INDEX_WCDMA_ONLY = 2;
    private static final int INDEX_TDSCDMA_ONLY = 3;
    private static final int INDEX_GSM_WCDMA_AUTO = 4;
    private static final int INDEX_GSM_TDSCDMA_AUTO = 5;
    private static final int INDEX_LTE_ONLY = 6;
    private static final int INDEX_CDMA_ONLY = 7;
    private static final int INDEX_LTE_GSM_WCDMA = 8;
    private static final int INDEX_LTE_WCDMA = 9;

    private static final int WCDMA_PREFERRED = Phone.NT_MODE_WCDMA_PREF;
    private static final int GSM_ONLY = Phone.NT_MODE_GSM_ONLY;
    private static final int WCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY;
    private static final int TDSCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY;
    private static final int GSM_WCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;
    private static final int GSM_TDSCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;
    private static final int LTE_ONLY = Phone.NT_MODE_LTE_ONLY;
    private static final int CDMA_ONLY = 0;
    private static final int LTE_GSM_WCDMA = Phone.NT_MODE_LTE_GSM_WCDMA;
    private static final int LTE_CDMA_EVDO_GSM_WCDMA = Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
    private static final int LTE_GSM_WCDMA_PREFERRED = 31;
    //RILConstants.NETWORK_MODE_LTE_GSM_WCDMA_PREF;
    private static final int LTE_WCDMA = Phone.NT_MODE_LTE_WCDMA;

    private Phone mPhone = null;
    private Phone mCdmaPhone = null;
    private int mModemType;
    private int mSimType = PhoneConstants.SIM_ID_1;
    private int mSubId = 1;
    private int[] mNetworkTypeValues = new int[] {WCDMA_PREFERRED, GSM_ONLY, WCDMA_ONLY,
            TDSCDMA_ONLY, GSM_WCDMA_AUTO, GSM_TDSCDMA_AUTO,
            LTE_ONLY, CDMA_ONLY, LTE_GSM_WCDMA, LTE_WCDMA};
    private int mCurrentSelected = 0;
    private Spinner mPreferredNetworkSpinner = null;

    private OnItemSelectedListener mPreferredNetworkListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            Log.d("@M_" + TAG, "onItemSelected " + pos);
            if (mCurrentSelected == pos) {
                return; // avoid listener being invoked by setSelection()
            }
            mCurrentSelected = pos;

            Message msg = mHandler.obtainMessage(EVENT_SET_NETWORKMODE_DONE);
            int selectNetworkMode = mNetworkTypeValues[pos];

            Log.d("@M_" + TAG, "selectNetworkMode " + selectNetworkMode);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId, selectNetworkMode);
            if (ModemCategory.isCdma()) {
                if (pos == INDEX_LTE_ONLY) {
                    String[] cmd = new String[] {"AT+ERAT=3", ""};
                    mPhone.invokeOemRilRequestStrings(cmd, msg);
                    msg = mHandler.obtainMessage(EVENT_SET_NETWORKMODE_DONE);
                    cmd = new String[] {"AT+EMDSTATUS=0,0", ""};
                    mCdmaPhone.invokeOemRilRequestStrings(cmd, msg);
                    return;
                }
                if (pos == INDEX_CDMA_ONLY) {
                    String[] cmd = new String[] {"AT+EFUN=0", ""};
                    mPhone.invokeOemRilRequestStrings(cmd, msg);
                    msg = mHandler.obtainMessage(EVENT_SET_NETWORKMODE_DONE);
                    cmd = new String[] {"AT+EMDSTATUS=1,0", ""};
                    mCdmaPhone.invokeOemRilRequestStrings(cmd, msg);
                    return;
                }
            }
            msg = mHandler.obtainMessage(EVENT_SET_NETWORKMODE_DONE);
            mPhone.setPreferredNetworkType(selectNetworkMode, msg);
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_QUERY_NETWORKMODE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int type = ((int[]) ar.result)[0];
                    Log.d("@M_" + TAG, "Get Preferred Type " + type);
                    int index = findSpinnerIndexByType(type);
                    if (index >= 0 && index < mPreferredNetworkSpinner.getCount()) {
                        mCurrentSelected = index;
                        mPreferredNetworkSpinner.setSelection(index, true);
                    }
                } else {
                    Toast.makeText(NetworkSelectActivity.this, R.string.query_preferred_fail,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case EVENT_SET_NETWORKMODE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    mPhone.getPreferredNetworkType(obtainMessage(EVENT_QUERY_NETWORKMODE_DONE));
                }
                break;
            default:
                break;
            }
        }
    };

    /**
     * Adapter.
     */
    public class CustomAdapter extends ArrayAdapter<String> {
        /**
         * Constructor.
         *
         * @param context
         *          Context
         * @param textViewResourceId
         *          Resource id
         * @param objects
         *          Objects
         */
        public CustomAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = null;
            Log.d("@M_" + TAG, "isAvailable: " + position + " is " + isAvailable(position));
            if (!isAvailable(position)) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                v = tv;
            } else {
                v = super.getDropDownView(position, null, parent);
            }
            return v;
        }
    }

    private boolean isAvailable(int index) {
        if (mModemType == ModemCategory.MODEM_TD
                && (index == INDEX_WCDMA_PREFERRED || index == INDEX_WCDMA_ONLY
                    || index == INDEX_GSM_WCDMA_AUTO)) {
            return false;
        }
        if (mModemType == ModemCategory.MODEM_FDD
                && (index == INDEX_TDSCDMA_ONLY || index == INDEX_GSM_TDSCDMA_AUTO)) {
            return false;
        }
        if (!FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)
                && (index == INDEX_LTE_ONLY || index == INDEX_LTE_GSM_WCDMA
                    || index == INDEX_LTE_WCDMA)) {
            return false;
        }
        if (!FeatureSupport.isSupported(FeatureSupport.FK_WCDMA_PREFERRED)
                && index == INDEX_WCDMA_PREFERRED) {
            return false;
        }
        if (index == INDEX_CDMA_ONLY) {
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.networkmode_switching);
        mPreferredNetworkSpinner = (Spinner) findViewById(R.id.networkModeSwitching);
        if (!ModemCategory.isCdma()) {
            findViewById(R.id.network_mode_set_hint).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSimType = getIntent().getIntExtra("mSimType", ModemCategory.getCapabilitySim());
        Log.i("@M_" + TAG, "mSimType " + mSimType);
        int[] subId = SubscriptionManager.getSubId(mSimType);
        if (subId != null) {
            for (int i = 0; i < subId.length; i++) {
                Log.i("@M_" + TAG, "subId[" + i + "]: " + subId[i]);
            }
        }
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            Toast.makeText(this, "Invalid sub id, please insert SIM Card!",
                    Toast.LENGTH_LONG).show();
            Log.e("@M_" + TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }

        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(mSimType);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }

        if (FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT)) {
            if ((FeatureSupport.isSupported(FeatureSupport.FK_MTK_SVLTE_SUPPORT)
                 || FeatureSupport.isSupported(FeatureSupport.FK_SRLTE_SUPPORT))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if (FeatureSupport.isSupported(FeatureSupport.FK_EVDO_DT_SUPPORT)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }

        if (ModemCategory.isCdma()) {
            mCdmaPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }
        mModemType = ModemCategory.getModemType();
        mCurrentSelected = 0;
        if (mModemType != ModemCategory.MODEM_NO3G) {
            String[] labels = getResources().getStringArray(R.array.network_mode_labels);
            CustomAdapter adapter =
                    new CustomAdapter(this, android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPreferredNetworkSpinner.setAdapter(adapter);
            mPreferredNetworkSpinner.setOnItemSelectedListener(mPreferredNetworkListener);
        } else {
//            mPreferredNetworkSpinner.setEnabled(false);
            Log.w("@M_" + TAG, "Isn't TD/WCDMA modem: " + mModemType);
            String[] labels = new String[] {"GSM only"};
            mNetworkTypeValues = new int[] {GSM_ONLY};
            CustomAdapter adapter =
                    new CustomAdapter(this, android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPreferredNetworkSpinner.setAdapter(adapter);
            for (int i = 0; i < mNetworkTypeValues.length; i++){
                 if(isAvailable(i)){
                     mPreferredNetworkSpinner.setSelection(i, true);
                     break;
                 }
            }
            mPreferredNetworkSpinner.setOnItemSelectedListener(mPreferredNetworkListener);
        }

        mPhone.getPreferredNetworkType(mHandler.obtainMessage(EVENT_QUERY_NETWORKMODE_DONE));
    }

    private int getModemType() {
        int mode = MODEM_NO3G;
        int mask = WorldPhoneUtil.get3GDivisionDuplexMode();
        if (mask == 2) {
                    mode = MODEM_TD;
        } else if (mask == 1) {
            mode = MODEM_FDD;
        } else {
            mode = MODEM_NO3G;
        }
        Log.i("@M_" + TAG, "mode = " + mode);
        return mode;
    }

    private int findSpinnerIndexByType(int type) {
        // Not WCDMA preferred for TD
        if (type == WCDMA_PREFERRED && mModemType == ModemCategory.MODEM_TD) {
            type = GSM_WCDMA_AUTO;
        }
        // Not support WCDMA preferred
        if (type == WCDMA_PREFERRED
                && !FeatureSupport.isSupported(FeatureSupport.FK_WCDMA_PREFERRED)) {
            type = GSM_WCDMA_AUTO;
        }
        // Consider LTE_GSM_WCDMA_PREFERRED as same with LTE_GSM_WCDMA
        if (type == LTE_GSM_WCDMA_PREFERRED || type == LTE_CDMA_EVDO_GSM_WCDMA) {
            type = LTE_GSM_WCDMA;
        }
        for (int i = 0; i < mNetworkTypeValues.length; i++) {
            if (mNetworkTypeValues[i] == type && isAvailable(i)) {
                return i;
            }
        }
        return -1;
    }
}
