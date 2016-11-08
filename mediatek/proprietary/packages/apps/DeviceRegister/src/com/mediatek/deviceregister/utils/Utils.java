package com.mediatek.deviceregister.utils;

import android.util.Log;

import com.mediatek.deviceregister.Const;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    private static final String TAG = Const.TAG_PREFIX + "Utils";

    private static final String PESN_PREFIX = "80";
    private static final String MEID_TO_PESN_HASH_NAME = "SHA-1";

    /**
     * Convert MEID info to pEsn.
     * @param meid
     * @return if error happends, then return ""
     *         if valid ESN is calculated, then return the value
     */
    public static String getEsnFromMeid(String meid) {
        if (meid == null || meid.length() == 0) {
            return "";
        }

        byte[] meidByte = hexStringToBytes(meid);
        MessageDigest md;
        String pEsn = "";

        try {

            md = MessageDigest.getInstance(Utils.MEID_TO_PESN_HASH_NAME);
            md.update(meidByte);
            String result = bytesToHexString(md.digest());
            int length = result.length();

            if (length > 6) {
                pEsn = Utils.PESN_PREFIX + result.substring(length - 6, length);
            } else {
                Log.e(TAG, "digest result length < 6, it is not valid:" + result);
            }

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "No such algorithm:" + Utils.MEID_TO_PESN_HASH_NAME);
            e.printStackTrace();
        }

        if (pEsn != null) {
            pEsn = pEsn.toUpperCase();
        }
        return pEsn;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }

        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }

        return stringBuilder.toString();
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];

        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }

        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static byte[] getReverseBytes(byte[] byteSrc) {
        byte[] resultByte = new byte[byteSrc.length + 1];
        int i = 0;
        resultByte[i] = (byte) byteSrc.length;

        for (int j = byteSrc.length - 1; j >= 0; j--) {
            i++;
            resultByte[i] = byteSrc[j];
        }

        return resultByte;
    }

    public static byte[] getRealBytes(byte[] byteSrc) {
        byte[] resultByte = new byte[byteSrc.length - 1];

        for (int i = 0, j = byteSrc.length - 1; j > 0; j--, i++) {
            resultByte[i] = byteSrc[j];
        }

        return resultByte;
    }

}
