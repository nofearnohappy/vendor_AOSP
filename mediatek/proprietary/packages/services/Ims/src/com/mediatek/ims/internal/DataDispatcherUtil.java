
package com.mediatek.ims.internal;

import android.util.Log;

import com.mediatek.ims.ImsAdapter.VaEvent;

public class DataDispatcherUtil {
    protected static final String TAG = "GSM";

    static final int IMC_MAXIMUM_NW_IF_NAME_STRING_SIZE = 100;
    static final int IMC_PCSCF_MAX_NUM = 10;
    static final int IMC_IPV4_ADDR_LEN = 0x04;
    static final int IMC_IPV6_ADDR_LEN = 0x10;
    static final boolean DBG = true;

    public DataDispatcherUtil() {
    }

    PdnActivationInd extractDefaultPdnActInd(VaEvent event) {
        PdnActivationInd defaultPdnActInd = new PdnActivationInd();

        defaultPdnActInd.transactionId = event.getByte();
        defaultPdnActInd.rat_type = event.getByte();
        defaultPdnActInd.isEmergency = (event.getByte() == 1);
        defaultPdnActInd.pad = event.getBytes(defaultPdnActInd.pad.length); // skip pad size 1

        log("extractDefaultPdnActInd DefaultPdnActInd" + defaultPdnActInd);
        return defaultPdnActInd;
    }

    PdnDeactivationInd extractDeactInd(VaEvent event) {
        PdnDeactivationInd deact = new PdnDeactivationInd();
        deact.transactionId = event.getByte();
        deact.abortTransactionId = event.getByte();
        deact.isValid = (event.getByte() == 1);
        deact.isEmergency = (event.getByte() == 1);

        log("extractDeactInd PdnDeactivationInd" + deact);
        return deact;
    }

    public class PdnActivationInd {
        public int transactionId;
        public boolean isEmergency;
        public int rat_type;
        public byte[] pad = new byte[1];

        @Override
        public String toString() {
            return "[ transactionId= " + transactionId + ", isEmergency= "
                    + isEmergency + ", rat_type= " + rat_type + " ]";
        }
    }

    public class PdnDeactivationInd {
        public int transactionId;
        public int abortTransactionId;
        public boolean isValid;
        public boolean isEmergency;

        @Override
        public String toString() {
            return "[ transactionId= " + transactionId + ", abortTransactionId= "
                    + abortTransactionId + ", isValid= " + isValid + ", isEmergency= "
                    + isEmergency + " ]";
        }
    }

    static void log(String text) {
        Log.d(TAG, "[dedicate] DataDispatcherUtil " + text);
    }

}
