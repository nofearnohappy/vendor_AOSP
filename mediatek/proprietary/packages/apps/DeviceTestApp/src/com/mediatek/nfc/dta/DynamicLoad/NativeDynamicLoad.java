package com.mediatek.nfc.dta.dynamicload;

import android.util.Log;
//import com.mediatek.common.featureoption.FeatureOption;

public class NativeDynamicLoad {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;

    static {
        try {
            System.loadLibrary("dta_dynamic_load_jni");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "NfcDynamicLoad library not found!");
        }
    }

    private native static int doQueryVersion();

    public static int queryVersion() {
        if (DBG) Log.d(TAG, "query version");
        //if( FeatureOption.MTK_NFC_SUPPORT == false) {
        //    if (DBG) Log.d(TAG, "NOT SUPPORT MTK NFC !");
	    //return -1;
	    //}
        return doQueryVersion();
    }
}

