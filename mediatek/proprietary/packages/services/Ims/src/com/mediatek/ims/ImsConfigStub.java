package com.mediatek.ims;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.android.ims.ImsConfig;
import com.android.ims.ImsConfigListener;
import com.android.ims.internal.IImsConfig;


import java.util.ArrayList;
import java.util.List;

/**
 * IMSConfig class for handle the IMS MO configruation.
 *
 * The implementation is based on 3GPP 24.167  3GPP IMS Management Object (MO); Stage 3
 *
 *  @hide
 */
public class ImsConfigStub extends IImsConfig.Stub {
    private static final String TAG = "ImsConfigService";

    private static final int  MAX_BYTE_COUNT               = 256;

    private Context mContext;
    private String  mAtCmdResult = "";
    private static TelephonyManager sTelephonyManager = null;
    private String  mPcscf;
    private static final String PROPERTY_VOLTE_ENALBE = "ro.mtk.volte.enable";

    /**
     *
     * Construction function for ImsConfigStub.
     *
     * @param context the application context
     *
     */
    public ImsConfigStub(Context context) {
        mContext = context;
        mPcscf = "";
    }

    /**
     * Gets the value for ims service/capabilities parameters from the master
     * value storage. Synchronous blocking call.
     *
     * @param item as defined in com.android.ims.ImsConfig#ConfigConstants.
     * @return value in Integer format.
     */
    @Override
    public int getProvisionedValue(int item) {
        return handleGetMasterValue(item);
    }


    /**
     * Gets the value for ims service/capabilities parameters from the master
     * value storage. Synchronous blocking call.
     *
     * @param item as defined in com.android.ims.ImsConfig#ConfigConstants.
     * @return value in String format.
     */
    @Override
    public String getProvisionedStringValue(int item) {
        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) mContext.getSystemService(
                                    Context.TELEPHONY_SERVICE);
        }

        return "";
    }

    /**
     * Sets the value for IMS service/capabilities parameters by the operator device
     * management entity. It sets the config item value in the provisioned storage
     * from which the master value is derived. Synchronous blocking call.
     *
     * @param item as defined in com.android.ims.ImsConfig#ConfigConstants.
     * @param value in Integer format.
     */
    @Override
    public int setProvisionedValue(int item, int value) {
        return handleProvisionedValue(item, value);
    }

    /**
     * Sets the value for IMS service/capabilities parameters by the operator device
     * management entity. It sets the config item value in the provisioned storage
     * from which the master value is derived.  Synchronous blocking call.
     *
     * @param item as defined in com.android.ims.ImsConfig#ConfigConstants.
     * @param value in String format.
     */
    @Override
    public int setProvisionedStringValue(int item, String value) {

        return 0;

    }

    /**
     * Gets the value of the specified IMS feature item for specified network type.
     * This operation gets the feature config value from the master storage (i.e. final
     * value) asynchronous non-blocking call.
     *
     * @param feature as defined in com.android.ims.ImsConfig#FeatureConstants.
     * @param network as defined in android.telephony.TelephonyManager#NETWORK_TYPE_XXX.
     * @param listener feature value returned asynchronously through listener.
     */
    @Override
    public void getFeatureValue(int feature, int network, ImsConfigListener listener) {
    switch (feature) {
        case ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE:
        case ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE:
            int value = android.provider.Settings.Global.getInt(
                    mContext.getContentResolver(),
                    android.provider.Settings.Global.ENHANCED_4G_MODE_ENABLED,
                    ImsConfig.FeatureValueConstants.OFF);
            if (listener != null) {
                try {
                    listener.onGetFeatureResponse(feature, network, value,
                            ImsConfig.OperationStatusConstants.SUCCESS);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException occurs when onGetFeatureResponse.");
                }
            }
            break;
        default:
            break;
    }

    }

    /**
     * Sets the value for IMS feature item for specified network type.
     * This operation stores the user setting in setting db from which master db
     * is dervied.
     *
     * @param feature as defined in com.android.ims.ImsConfig#FeatureConstants.
     * @param network as defined in android.telephony.TelephonyManager#NETWORK_TYPE_XXX.
     * @param value as defined in com.android.ims.ImsConfig#FeatureValueConstants.
     * @param listener provided if caller needs to be notified for set result.
     */
    @Override
    public void setFeatureValue(int feature, int network, int value, ImsConfigListener listener) {

        switch (feature) {
            case ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE:
            case ImsConfig.FeatureConstants.FEATURE_TYPE_VIDEO_OVER_LTE:
                int oldValue = SystemProperties.getInt(PROPERTY_VOLTE_ENALBE, 0);
                if( value != oldValue) {
                    if (value ==1) {
                        SystemProperties.set(PROPERTY_VOLTE_ENALBE,"1");
                    } else {
                        SystemProperties.set(PROPERTY_VOLTE_ENALBE,"0");
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Gets the value for IMS volte provisioned.
     * This should be the same as the operator provisioned value if applies.
     *
     * @return boolean
     */
    @Override
    public boolean getVolteProvisioned() {
        return true;
    }

    /**
     * Gets the value for IMS feature item for video call quality.
     *
     * @param listener, provided if caller needs to be notified for set result.
     * @return void
     */
    public void getVideoQuality(ImsConfigListener listener) {

    }

    /**
     * Sets the value for IMS feature item video quality.
     *
     * @param quality, defines the value of video quality.
     * @param listener, provided if caller needs to be notified for set result.
     * @return void
     */
     public void setVideoQuality(int quality, ImsConfigListener listener) {

     }

    private String getAtCmdLine(int item) {
        String atCmdString = "";
        Log.i(TAG, "getAtCmdLine:" + item);
        return atCmdString;
    }

    private String getAtCmdSetLine(int item, int value) {
        String atCmdString = "";
        Log.i(TAG, "getAtCmdLine:" + item);
        return atCmdString;
    }

    private synchronized int handleGetMasterValue(int item) {
        Log.i(TAG, "handleGetMasterValue:" + item);

        String retValue = executeCommandResponse(getAtCmdLine(item));

        if (retValue.length() >  0) {
            try {
                return Integer.parseInt(retValue);
            } catch (NumberFormatException ne) {
                ne.printStackTrace();
            }
        }

        return 0;
    }

    private synchronized int handleProvisionedValue(int item, int value) {
        Log.i(TAG, "handleProvisionedValue:" + item + ":" + value);
        // Not support from now, ignore it
        // executeCommandResponse(getAtCmdSetLine(item, value));
        return 24;
    }

    private synchronized String executeCommandResponse(String atCmdLine) {
        String atCmdResult = "";

        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager)
                                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        byte[] rawData = atCmdLine.getBytes();
        byte[] cmdByte = new byte[rawData.length + 1];
        byte[] respByte = new byte[MAX_BYTE_COUNT + 1];
        System.arraycopy(rawData, 0, cmdByte, 0, rawData.length);
        cmdByte[cmdByte.length - 1] = 0;

        if (sTelephonyManager.invokeOemRilRequestRaw(cmdByte, respByte) > 0) {
            atCmdResult = new String(respByte);
        }

        //Handle CME ERROR
        if (atCmdResult.indexOf("+CME ERROR") != -1) {
            atCmdResult = "";
        }
        return atCmdResult;
    }
}
