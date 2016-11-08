package com.mediatek.op.telephony;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.INetworkManagementService;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;

/**
 * Interface that defines methos which are implemented in IGsmDCTExt
 */

 /** {@hide} */
@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmDCTExt")
public class GsmDCTExtOP01 extends GsmDCTExt {
    static final String TAG = "GsmDCTExtOp01";

    private Context mContext;

    public GsmDCTExtOP01(Context context) {
        mContext = context;
    }

    public boolean isDataAllowedAsOff(String apnType) {
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_MMS)
                || TextUtils.equals(apnType, PhoneConstants.APN_TYPE_SUPL)
                || TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS)
                || TextUtils.equals(apnType, PhoneConstants.APN_TYPE_EMERGENCY)
                || TextUtils.equals(apnType, PhoneConstants.APN_TYPE_RCS)) {
            return true;
        }
        return false;
    }

    public boolean getFDForceFlag(boolean force_flag) {
        return force_flag;
    }

    public void onDcActivated(String[] apnTypes, String ifc) {
        if (apnTypes == null || ifc == null) {
            return;
        }

        log("onDcActivated. ifc: " + ifc);
        if (ifc.length() == 0) {
            return;
        }

        if (!hasImsApnType(apnTypes)) {
            return;
        }

        enableVolteIotFirewall(true, ifc);
    }

    public void onDcDeactivated(String[] apnTypes, String ifc) {
        if (apnTypes == null || ifc == null) {
            return;
        }

        log("onDcDeactivated. ifc: " + ifc);
        if (ifc.length() == 0) {
            return;
        }

        if (!hasImsApnType(apnTypes)) {
            return;
        }

        enableVolteIotFirewall(false, ifc);
    }

    private boolean hasImsApnType(String[] apnTypes) {
        boolean ret = false;
        if (apnTypes != null) {
            for (String apnType : apnTypes) {
                if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS)) {
                    log("apnType = " + apnType);
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    private void enableVolteIotFirewall(boolean enable, String ifc) {
        log("enableVolteIotFirewall, ifc: "
            + (ifc == null ? "null" : ifc) + ", " + enable);

        boolean isTestSim =
            SystemProperties.get("gsm.sim.ril.testsim").equals("1")
            || (SystemProperties.get("ro.mtk_gemini_support").equals("1")
                && SystemProperties.get("gsm.sim.ril.testsim.2").equals("1"));
        if (!isTestSim) {
            log("enableVolteIotFirewall, not TEST SIM");
            return;
        }

// Bypass USB check.
/*
        UsbManager usbMgr = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        int usbState = usbMgr.getCurrentState();
        if (usbState != 1  && enable) {
            log("enableVolteIotFirewall, USB not connected");
            return;
        }
        if (!usbMgr.isFunctionEnabled("acm") && enable) {
            log("enableVolteIotFirewall, not contains acm");
            return;
        }
*/

        INetworkManagementService netd = INetworkManagementService.Stub.asInterface(
            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        if (netd == null) {
            log("enableVolteIotFirewall, netd == null");
            return;
        }

        try {
            if (enable) {
                netd.setVolteIotFirewall(ifc);
                log("enableVolteIotFirewall, setVolteIotFirewall()");
            } else {
                netd.clearVolteIotFirewall(ifc);
                log("enableVolteIotFirewall, clearVolteIotFirewall()");
            }
        } catch (RemoteException e) {
            log("enableVolteIotFirewall, exception: " + e);
        }
    }
}

