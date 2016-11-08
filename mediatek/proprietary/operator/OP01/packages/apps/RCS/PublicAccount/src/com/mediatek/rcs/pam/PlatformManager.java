package com.mediatek.rcs.pam;

import android.content.Context;
import android.util.Log;

import com.mediatek.rcs.pam.client.PAMClient.Transmitter;
import com.mediatek.rcs.pam.connectivity.HttpDigestTransmitter;
import com.mediatek.rcs.pam.util.Utils;

import org.gsma.joyn.JoynServiceConfiguration;

public class PlatformManager {
    public static PlatformManager sInstance;

//    private static final String SERVER_URL = "http://122.70.137.46:8088/interface/index.php";
    private static final String SERVER2_URL = "http://221.179.192.78:8188/";
    private static final String NAF_URL = "http://221.179.192.78:8188/";
    // FIXME for test only
    private static final String TEST_SIP_DOMAIN = "@bj.ims.mnc460.mcc000.3gppnetwork.org";

    private static final String TAG = Constants.TAG_PREFIX + "PlatformManager";

    protected Transmitter mTransmitter;

    private final JoynServiceConfiguration mJoynConfig;

    protected PlatformManager() {
        mJoynConfig = new JoynServiceConfiguration();
    }

    public static synchronized PlatformManager getInstance() {
        if (sInstance == null) {
            sInstance = new PlatformManager();
        }
        return sInstance;
    }

    public synchronized Transmitter getTransmitter(Context context) {
        if (mTransmitter == null) {
            mTransmitter = new HttpDigestTransmitter(
                    context, getServerUrl(context), "12345678");
            // mTransmitter = new GbaTransmitter(context, getServerUrl(context), NAF_URL);
            // mTransmitter = new HttpTransmitter(context, getServerUrl(context));
        }
        return mTransmitter;
    }

    public String getUserId(Context context) {
//        return "tel:+8618811047941";
        String publicUri = mJoynConfig.getPublicUri(context);
//        publicUri = "tel:+8618811047941";
        Log.d(TAG, "getUserId: publicUri = " + publicUri);
        String uuid = Utils.extractUuidFromSipUri(publicUri);
        if (uuid == null) {
            return null;
        }
        if (publicUri.startsWith(Constants.TEL_PREFIX)) {
            String phoneNumber = publicUri.substring(Constants.TEL_PREFIX.length());
            return Constants.SIP_PREFIX + phoneNumber + TEST_SIP_DOMAIN;
        } else {
            return Constants.SIP_PREFIX + Utils.extractNumberFromUuid(uuid) + TEST_SIP_DOMAIN;
        }
    }

    public String getIdentity(Context context) {
        String publicUri = mJoynConfig.getPublicUri(context);
//        publicUri = "tel:+8618811047941";
        if (publicUri.startsWith(Constants.TEL_PREFIX)) {
            return publicUri;
        }
        String phoneNumber = Utils.extractNumberFromUuid(Utils.extractUuidFromSipUri(publicUri));
        if (phoneNumber == null) {
            return null;
        }
        return Constants.TEL_PREFIX + phoneNumber;
    }

    public String getServerUrl(Context context) {
        return SERVER2_URL;
//        URI uri = URI.create(getPublicAccountServerAddress(context));
//        String port = getPublicAccountServerAddressPort(context);
//        if (!TextUtils.isEmpty(port)) {
//            try {
//                uri = new URI(uri.getScheme(),
//                                uri.getUserInfo(),
//                                uri.getHost(),
//                                Integer.parseInt(port),
//                                uri.getPath(),
//                                uri.getQuery(),
//                                uri.getFragment());
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//            } catch (URISyntaxException e) {
//                e.printStackTrace();
//                return SERVER_URL;
//            }
//            return uri.toString();
//        } else {
//            return SERVER_URL;
//        }
    }

    public String getNafUrl(Context context) {
        return getServerUrl(context);
    }

    public String getPublicAccountServerAddress(Context context) {
        String result = mJoynConfig.getPublicAccountAddress(context);
        Log.d(TAG, "PA Address from Joyn Config: " + result);
        return result;
    }

    public String getPublicAccountServerAddressPort(Context context) {
        String result = mJoynConfig.getPublicAccountAddressPort(context);
        Log.d(TAG, "PA Port from Joyn Config: " + result);
        return result;
    }

    public String getPublicAccountServerAddressType(Context context) {
        String result = mJoynConfig.getPublicAccountAddressType(context);
        Log.d(TAG, "PA Type from Joyn Config: " + result);
        return result;
    }

    public boolean getPublicAccountServerAUTH(Context context) {
        return mJoynConfig.getPublicAccountAUTH(context);
    }

    public boolean supportCcs() {
        return false;
    }

    public boolean isRcsServiceActivated(Context context) {
        return JoynServiceConfiguration.isServiceActivated(context);
    }
}
