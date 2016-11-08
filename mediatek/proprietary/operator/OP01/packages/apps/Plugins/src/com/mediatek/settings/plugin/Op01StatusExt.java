package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.settings.ext.DefaultStatusExt;

/**
 * Device status info plugin.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IStatusExt")
public class Op01StatusExt extends DefaultStatusExt {
    private static final String TAG = "Op01StatusExt";

    private static final String STRING_MEID = "MEID";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String s_keyList[] = {
        KEY_MIN_NUMBER
        , KEY_PRL_VERSION
        , KEY_IMEI
        , KEY_IMEI_SV
        , KEY_ICC_ID
    };

    private Context mContext;

    /**
     * Init context.
     * @param context The Context
     */
    public Op01StatusExt(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void customizeImei(String imeiKey, String imeiSvKey,
            PreferenceScreen parent, int slotId) {
        if (SystemProperties.get("ro.mtk_single_imei").equals("1")) {
            if (SystemProperties.get("ro.mtk_c2k_support").equals("1")) {
                String meidKey = "_" + KEY_MEID_NUMBER + String.valueOf(slotId);
                Preference meid = parent.findPreference(meidKey);
                if (meid != null) {
                    //for C+G or G+C, CDMAPhone only dispaly MEID
                    Log.d("@M_" + TAG, "meid:" + slotId);
                    // for meid, shall set title without slot id
                    meid.setTitle(STRING_MEID);
                    for (int i = 0; i < s_keyList.length; i++) {
                        String key = "_" + s_keyList[i] + String.valueOf(slotId);
                        Preference pref = parent.findPreference(key);
                        if (pref != null) {
                            parent.removePreference(pref);
                        }
                    }
                    return;
                }

                // G+G, sim2's imei shall be hiden
                if (slotId == 1) {
                    String imei1Key = "_" + KEY_IMEI + "0";
                    Preference imei1 = parent.findPreference(imei1Key);
                    if (imei1 != null) {
                        Log.d("@M_" + TAG, "G+G, hide imei2");
                        Preference imei = parent.findPreference(imeiKey);
                        Preference imeiSv = parent.findPreference(imeiSvKey);
                        if (imei != null) {
                            parent.removePreference(imei);
                        }
                        if (imeiSv != null) {
                            parent.removePreference(imeiSv);
                        }
                    }
                }

                // for imei and imeiSv, shall set title without slot id
                Preference imei = parent.findPreference(imeiKey);
                Preference imeiSv = parent.findPreference(imeiSvKey);
                if (imei != null) {
                    Log.d("@M_" + TAG, "imei:" + slotId);
                    imei.setTitle(mContext.getString(R.string.status_imei));
                }
                if (imeiSv != null) {
                    imeiSv.setTitle(mContext.getString(R.string.status_imei_sv));
                }
            } else {
                Preference imei = parent.findPreference(imeiKey);
                Preference imeiSv = parent.findPreference(imeiSvKey);
                if (slotId == 0) {
                    if (imei != null) {
                        imei.setTitle(mContext.getString(R.string.status_imei));
                    }
                    if (imeiSv != null) {
                        imeiSv.setTitle(mContext.getString(R.string.status_imei_sv));
                    }
                } else {
                    if (imei != null) {
                        parent.removePreference(imei);
                    }
                    if (imeiSv != null) {
                        parent.removePreference(imeiSv);
                    }
                }
            }
        }
    }
}
