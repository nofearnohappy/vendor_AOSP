package com.hesine.nmsg.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AESCoder {
    private static final String KEY = "0B41883A7B4599F51C1462CF9606CE3C";

    public byte[] rnmDecrypt(byte[] encrypts) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] bkey = key2Byte(KEY);
        return decrypt(encrypts, bkey);
    }

    public byte[] rnmEncrypt(String encrypts) throws UnsupportedEncodingException,
            InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] bkey = key2Byte(KEY);
        return encrypt(encrypts.getBytes("UTF-8"), bkey);
    }

    protected byte[] key2Byte(String key) {
        byte[] fileKey = new byte[16];

        for (int i = 0, j = 0; j < 16;) {
            String s = key.substring(i, i + 2);
            int n = Integer.valueOf(s, 16).intValue();
            fileKey[j] = (byte) n;
            j++;
            i = i + 2;

        }

        // return fileKey;
        return key.getBytes();
    }

    private byte[] encrypt(byte[] encrypts, byte[] key) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cp = Cipher.getInstance("AES");// /CTR/NoPadding
        cp.init(Cipher.ENCRYPT_MODE, secretKeySpec/*
                                                   * , new
                                                   * IvParameterSpec(dummyBlock)
                                                   */);
        return cp.doFinal(encrypts);
    }

    private byte[] decrypt(byte[] encrypts, byte[] key) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cp = Cipher.getInstance("AES");// /CTR/NoPadding
        cp.init(Cipher.DECRYPT_MODE, secretKeySpec/*
                                                   * , new
                                                   * IvParameterSpec(dummyBlock)
                                                   */);
        return cp.doFinal(encrypts);
    }

}
