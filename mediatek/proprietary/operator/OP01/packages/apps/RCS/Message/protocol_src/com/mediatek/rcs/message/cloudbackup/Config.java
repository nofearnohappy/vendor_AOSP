package com.mediatek.rcs.message.cloudbackup;

/**
 * Configure constants.
 *
 */
public final class Config {
    public static final int FLAG_GBA          = 0x01;    // Use GBA authentication
    public static final int FLAG_TRUST_ALL    = 0x02;    // Trust all certificates

    /**
     * @return If can trust all certificates.
     */
    public static boolean canTrustAllCertificates() {
        return false;
    }
    /**
     * @return If use GBA Authentication.
     */
    public static boolean useGBA() {
        return false;
    }

    /**
     * @return Server address.
     * TODO: how to config it.
     */
    public static String getHost() {
        //return mHostAuth.mAddress;
        return "120.197.90.53";
    }

    /**
     * @return Server port.
     */
    public static int getPort() {
        //return mHostAuth.mPort;
        return 22228;
    }

}
