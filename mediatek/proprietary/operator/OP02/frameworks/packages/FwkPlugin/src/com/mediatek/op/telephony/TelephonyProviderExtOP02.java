package com.mediatek.op.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.internal.R;

/**
 * CU feature 46001 and 46009 is cu card.use new String instead.
 */
@PluginImpl(interfaceName = "com.mediatek.common.telephony.ITelephonyProviderExt")
public class TelephonyProviderExtOP02 extends TelephonyProviderExt {
    private static final String TAG = "TelephonyProviderExtOP02";
    private static final String CU_NUMERIC_1 = "46001";
    private static final String CU_NUMERIC_2 = "46009";
    private static final String CU_APN_NET = "3gnet";
    private static final String CU_APN_WAP = "3gwap";

    private Context mContext;
    private String mCustomizedApnNet;
    private String mCustomizedApnWap;
    private static final String SUPPORTED = "1";
    private static final String FK_LTE_SUPPORT = "ro.mtk_lte_support";

    private static boolean isMtkLteSupported() {
        return SUPPORTED.equals(SystemProperties.get(FK_LTE_SUPPORT));
    }

    /**
     * Constructor method.
     * @param context ApnSettings constext
     */
    public TelephonyProviderExtOP02(Context context) {
        mContext = context;
        if (isMtkLteSupported()) {
            mCustomizedApnNet = mContext.getResources().getString(R.string.cu_3gnet_name_in_lte);
            mCustomizedApnWap = mContext.getResources().getString(R.string.cu_3gwap_name_in_lte);
        } else {
            mCustomizedApnNet = mContext.getResources().getString(R.string.cu_3gnet_name);
            mCustomizedApnWap = mContext.getResources().getString(R.string.cu_3gwap_name);
        }
        Log.d(TAG, "Constructor, mCustomizedApnNet=" + mCustomizedApnNet +
                ", mCustomizedApnWap=" + mCustomizedApnWap);
    }

    /**
     * put new String to ContentValues.
     * @param row ContentValues
     * @return 1 if CU apn and put new String
     */
    public int onLoadApns(ContentValues row) {
        if (row != null) {
            if ((row.get(Telephony.Carriers.NUMERIC) != null)
                    && (row.get(Telephony.Carriers.NUMERIC).equals(CU_NUMERIC_1)
                            || row.get(Telephony.Carriers.NUMERIC).equals(CU_NUMERIC_2))
                            && row.get(Telephony.Carriers.APN) != null) {
                if (row.get(Telephony.Carriers.APN).equals(CU_APN_NET)) {
                    row.put(Telephony.Carriers.NAME, mCustomizedApnNet);
                    return 1;
                } else if (row.get(Telephony.Carriers.APN).equals(CU_APN_WAP)) {
                    if (!(row.containsKey(Telephony.Carriers.TYPE)
                            && (row.get(Telephony.Carriers.TYPE) != null)
                            && (row.get(Telephony.Carriers.TYPE).equals("mms")))) {
                        row.put(Telephony.Carriers.NAME, mCustomizedApnWap);
                        return 1;
                    }
                }
            }
        }
        return 0;
    }
}
