package com.mediatek.op.telephony;

import android.content.Context;
import android.telephony.ServiceState;
import android.util.Log;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IServiceStateExt")
public class ServiceStateExtOP07 extends DefaultServiceStateExt {
    private Context mContext;
    static final String LOG_TAG = "GSM";

    public ServiceStateExtOP07() {
    }

    public ServiceStateExtOP07(Context context) {
        mContext = context;
    }

    public boolean isImeiLocked() {
        return true;
    }

    public boolean needBrodcastAcmt(int errorType, int errorCause) {
        /* <errorType>: integer
           0  UNDEFINED
           1  MM reject code received during a MM procedure
           2  CM reject code received during a CM procedure
           3  GMM reject code received during a non-combined GMM procedure for GPRS services
           4  SM reject code
           5  GMM reject code received during a combined GMM procedure for non-GPRS services
           6  GMM reject code received during a combined GMM procedure for GPRS and non-GPRS
              services
           7  EMM reject code received during a non-combined EMM procedure for EPS services
              (not supported,RFU)
           8  EMM reject code received during a combined EMM procedure for non-EPS services
              (not supported,RFU)
           9  EMM reject code received during a combined EMM procedure for EPS and non-EPS
              services (not supported,RFU)
           10 ESM reject code received during a ESM procedure (not supported,RFU)
           11 GMM reject code received during a GMM Service procedure (not supported,RFU)
           12 GMM reject code received during a GMM MT Detach procedure (not supported,RFU)
        */
        if (((errorType == 1) && ((errorCause == 17) || (errorCause == 22)))
                || ((errorType == 2) && ((errorCause == 17) || (errorCause == 22)))
                || ((errorType == 3)
                        && ((errorCause == 7) || (errorCause == 8) || (errorCause == 10)
                                || (errorCause == 17) || (errorCause == 22)))
                || ((errorType == 4)
                        && ((errorCause == 27) || (errorCause == 28) || (errorCause == 30)
                                || (errorCause == 31) || (errorCause == 33)
                                || (errorCause == 36) || (errorCause == 38)))
                || ((errorType == 5)
                        && ((errorCause == 17) || (errorCause == 22)))
                || ((errorType == 6)
                        && ((errorCause == 7) || (errorCause == 8) || (errorCause == 10)
                                || (errorCause == 17) || (errorCause == 22)))
                || ((errorType == 11)
                        && ((errorCause == 7) || (errorCause == 10) || (errorCause == 17)
                                || (errorCause == 22) || (errorCause == 40)))) {
            Log.w(LOG_TAG, "needBrodcastAcmt return false. type=" + errorType
                    + "cause=" + errorCause);
            return true;
        }

        Log.w(LOG_TAG, "needBrodcastAcmt return false. type=" + errorType + "cause=" + errorCause);
        return false;
    }

    public boolean needRejectCauseNotification(int cause) {
        boolean needNotification = false;
        Log.i(LOG_TAG, "needRejectCauseNotification cause:" + cause);

        /* ALPS00283696 CDR-NWS-241 */
        switch(cause) {
            case 2:
            case 3:
            case 5:
            case 6:
                Log.w(LOG_TAG, "needRejectCauseNotification return true");
                needNotification = true;
                break;
            default:
                break;
        }
        return needNotification;
    }

   public boolean needIgnoredState(int state, int newState, int cause) {
       Log.i(LOG_TAG, "needIgnoredState state:" + state + "newState:" + newState
               + "cause:" + cause);

       /* ALPS00267573 */
       if (state == ServiceState.STATE_IN_SERVICE) {
           if ((newState == 2) || ((newState == 3) && (cause == 0))) {
               Log.i(LOG_TAG, "set dontUpdateNetworkStateFlag");

               return true;
           }
       }

       Log.i(LOG_TAG, "clear dontUpdateNetworkStateFlag");
       return false;
   }

   public boolean ignoreDomesticRoaming() {
       /* ALPS00296372 */
       return true;
   }

   public boolean isSupportRatBalancing() {
       return true;
   }

}
