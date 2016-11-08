package com.mediatek.deviceregister;

import android.os.Build;
import android.util.Log;

import com.mediatek.deviceregister.utils.PlatformManager;

import java.util.zip.CRC32;

public class RegisterMessage {
    private static final String TAG = Const.TAG_PREFIX + "RegisterMessage";

    // Value specified by CT
    private static final byte PROTOCOL_VERSION_ESN = 0x01;
    private static final byte PROTOCOL_VERSION_MEID = 0x02;

    private static final byte COMMAND_TYPE_SEND = 0x03;
    public static final byte COMMAND_TYPE_RECEIVED = 0x04;

    private static final int LENGTH_MAX_MESSAGE = 127;
    private static final int LENGTH_MAX_MANUFACTURE = 3;
    private static final int LENGTH_MAX_MODEL = 20;
    private static final int LENGTH_MAX_VERSION = 60;
    private static final int LENGTH_CHECKSUM = 8;

    private byte mProtocolVersion = PROTOCOL_VERSION_MEID;
    private byte mCommandType = COMMAND_TYPE_SEND;
    private byte mDataLength = 0;
    private byte mFillByte = 0x0;

    private String mData = "";
    private String mChecksum = "";
    private RegisterService mService;

    public RegisterMessage(RegisterService service) {
        mService = service;
    }

    public byte[] getRegisterMessage() {
        Log.d(TAG, "getRegisterMessage");
        mData = generateMessageData();
        mDataLength = (byte) mData.length();

        int byteArrayLenth = 4 + mDataLength;
        byte[] message = new byte[byteArrayLenth];
        message[0] = mProtocolVersion;
        message[1] = mCommandType;
        message[2] = mDataLength;
        message[3] = mFillByte;
        byte[] dataByte = mData.getBytes();
        int i = 4;
        for (int j = 0; j < dataByte.length; j++) {
            message[i] = dataByte[j];
            i++;
        }

        mChecksum = generateChecksum(message);
        Log.d(TAG, "checksum: " + mChecksum);
        byte[] crcByte = mChecksum.getBytes();

        byte[] messageFinal = new byte[message.length + LENGTH_CHECKSUM];

        int k = 0;
        for (int j = 0; j < message.length; j++) {
            messageFinal[k] = message[j];
            k++;
        }

        for (int j = 0; j < crcByte.length; j++) {
            messageFinal[k] = crcByte[j];
            k++;
        }
        return messageFinal;
    }

    private String generateMessageData() {
        Log.d(TAG, "generateMessageData");
        String beginTag = "<a1>";
        String endTag = "</a1>";
        String modelBeginTag = "<b1>";
        String modelEndTag = "</b1>";
        String meidBeginTag = "<b2>";
        String meidEndTag = "</b2>";
        String imsiBeginTag = "<b3>";
        String imsiEndTag = "</b3>";
        String versionBeginTag = "<b4>";
        String versionEndTag = "</b4>";
        StringBuffer data = new StringBuffer();
        data.append(beginTag);
        data.append(modelBeginTag).append(getModel()).append(modelEndTag);
        data.append(meidBeginTag).append(getMeid()).append(meidEndTag);
        data.append(imsiBeginTag).append(getIMSI()).append(imsiEndTag);
        data.append(versionBeginTag).append(getSoftwareVersion()).append(versionEndTag);
        data.append(endTag);

        if (data.length() > LENGTH_MAX_MESSAGE) {
            Log.w(TAG, "Message length > " + LENGTH_MAX_MESSAGE + ", cut it!");
            int exceedLength = data.length() - LENGTH_MAX_MESSAGE;
            data = data.delete(data.length() - 10 - exceedLength, data.length() - 10);
        }

        Log.d(TAG, "message: " + data.toString());
        return data.toString();
    }

    private String getModel() {
        String manufacturer = PlatformManager.getManufacturer();
        if (manufacturer.length() > LENGTH_MAX_MANUFACTURE) {
            Log.w(TAG, "Manufacturer length > " + LENGTH_MAX_MANUFACTURE + ", cut it!");
            manufacturer = manufacturer.substring(0, LENGTH_MAX_MANUFACTURE);
        }

        String model = Build.MODEL;
        Log.d(TAG, "model: " + model);
        model = model.replaceAll("-", " ");
        if (model.indexOf(manufacturer) != -1) {
            model = model.replaceFirst(manufacturer, "");
        }

        String result = manufacturer + "-" + model;
        if (result.length() > LENGTH_MAX_MODEL) {
            Log.w(TAG, "Model length > " + LENGTH_MAX_MODEL + ", cut it!");
            result = result.substring(0, LENGTH_MAX_MODEL);
        }

        return result;
    }

    private String getMeid() {
        return mService.getCurrentMeid();
    }

    private String getIMSI() {
        return mService.getCurrentImsi();
    }

    private String getSoftwareVersion() {
        String result = PlatformManager.getSoftwareVersion();
        if (result.length() > LENGTH_MAX_VERSION) {
            Log.w(TAG, "Software version length > " + LENGTH_MAX_VERSION + ", cut it!");
            result = result.substring(0, LENGTH_MAX_VERSION);
        }

        return result;
    }

    private String generateChecksum(byte[] data) {
        CRC32 checksum = new CRC32();
        checksum.update(data);
        long value = checksum.getValue();

        String crcString = Long.toHexString(value);
        int crcStringLength = crcString.length();
        if (crcStringLength < LENGTH_CHECKSUM) {
            String prefix = "";
            for (int i = crcStringLength; i < LENGTH_CHECKSUM; i++) {
                prefix += "0";
            }
            crcString = prefix + crcString;
        }
        return crcString;
    }

}
