package com.mediatek.esntrack;

import android.os.Build;
import android.util.Log;

public class EsnTrackSmsComposer {
    private static final String TAG = "EsnTrackSmsComposer";

    private EsnTrackService mService;
    private String mAction;
    private int mOptrCode;

    public EsnTrackSmsComposer(EsnTrackService service, String action,
            int optrCode) {
        mService = service;
        mAction = action;
        mOptrCode = optrCode;
    }

    public String getRegisterMessage() {
        Log.d(TAG, "generateMessageData");

        if (mOptrCode == Const.MTS) {
            StringBuffer data = new StringBuffer();
            String beginTagMts = "ESNTRACK MTS";
            String space = " ";
            String model = Build.MODEL;
            String manufacturer = Build.MANUFACTURER;
            String version = "Build.VERSION";
            String ruimIdTag = "RUIM_ID:";
            String meIdTag = "ESN_ME:";
            String actionTag = getActionTag();
            String versionV = "V2";
            String meId = EsnTrackController.getInstance().getDeviceId();
            String ruimId = EsnTrackController.getInstance()
                    .getRuimIdFromDevice();
            Log.d(TAG, "generateMessageData model = " + model
                    + " manufacturer= " + manufacturer + " version = "
                    + version + "actionTag = " + actionTag + "meId = " + meId
                    + "ruimId = " + ruimId);

            data.append(beginTagMts).append(space);
            data.append(manufacturer).append(space).append(model).append(space);
            data.append(actionTag).append(space);
            data.append(ruimIdTag).append(space).append(ruimId).append(space);
            data.append(meIdTag).append(space).append(meId).append(space)
                    .append(versionV);

            Log.d(TAG, "MTS message:" + data.toString().toUpperCase());
            return (data.toString().toUpperCase());

        } else if (mOptrCode == Const.TATA) {

            StringBuffer data = new StringBuffer(
                    "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
            String beginTagTata = "TRACK";
            String space = " ";
            String model = Build.MODEL;
            model = getformattedCode(model, 10);
            String manufacturer = Build.MANUFACTURER;
            manufacturer = getformattedCode(manufacturer, 10);

            String SwVersion = Build.VERSION.RELEASE;
            SwVersion = getformattedCode(SwVersion, 15);

            String ruimIdTag = "RUIM_ID:";
            String meIdTag = "ESN_ME:";
            String actionTag = getActionTag();
            String cardVendorCode = "FFFFFF";

            String meId = EsnTrackController.getInstance().getDeviceId();
            meId = getformattedCode(meId, 14);
            String ruimId = EsnTrackController.getInstance()
                    .getRuimIdFromDevice();
            ruimId = getformattedCode(ruimId, 14);

            String version = "V3";
            Log.d(TAG, "generateMessageData model = " + model
                    + " manufacturer= " + manufacturer + " version = "
                    + version + "actionTag = " + actionTag + "meId = " + meId
                    + "ruimId = " + ruimId);
            data.replace(0, 5, beginTagTata);// ADD TRACK
            data.replace(5, 6, space); // add space
            data.replace(6, 12, cardVendorCode); // Add Card vendor
            data.replace(12, 13, space); // add space
            data.replace(13, 13 + manufacturer.length(), manufacturer);
            data.replace(23, 24, space); // add space
            data.replace(24, 24 + model.length(), model);
            data.replace(34, 35, space); // add space
            data.replace(35, 37, actionTag);
            data.replace(37, 38, space); // add space
            data.replace(38, 38 + ruimIdTag.length(), ruimIdTag);

            data.replace(46, 47, space); // add space
            data.replace(47, 47 + ruimId.length(), ruimId);
            data.replace(61, 62, space); // add space

            data.replace(62, 62 + meIdTag.length(), meIdTag);
            data.replace(69, 70, space); // add space

            data.replace(70, 70 + meId.length(), meId);
            data.replace(84, 85, space); // add space

            data.replace(85, 85 + SwVersion.length(), SwVersion);
            data.replace(100, 101, space); // add space

            data.replace(101, 103, version); // add space

            Log.d(TAG, "TATA message:" + data.toString().toUpperCase());
            return (data.toString().toUpperCase());

        }
        return null;

    }

    public String getformattedCode(String str, int max) {
        if (str != null) {
            if (str.length() >= max) {
                return str.substring(0, max); // return the first max allowed
                                              // chars
            } else {
                return str;

            }

        }
        return null;
    }

    private String getActionTag() {
        String actionTag = "";
        if (mOptrCode == Const.MTS) {
            if (Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                    .equalsIgnoreCase(mAction)) {
                actionTag = "PU";
            } else if (Const.ACTION_CDMA_NEW_OUTGOING_CALL
                    .equalsIgnoreCase(mAction)
                    || Const.ACTION_CDMA_NEW_SMS_RECVD
                            .equalsIgnoreCase(mAction)
                    || Const.ACTION_CDMA_MT_CALL.equalsIgnoreCase(mAction)
                    || Const.ACTION_CDMA_SMS_MSG_SENT.equalsIgnoreCase(mAction)) {
                actionTag = "OT";
            }
        } else if (mOptrCode == Const.TATA) {
            if (Const.ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE
                    .equalsIgnoreCase(mAction)) {
                actionTag = "PU";

            } else if (Const.ACTION_CDMA_NEW_OUTGOING_CALL
                    .equalsIgnoreCase(mAction)) {
                actionTag = "OV";

            } else if (Const.ACTION_CDMA_NEW_SMS_RECVD
                    .equalsIgnoreCase(mAction)) {
                actionTag = "TS";

            } else if (Const.ACTION_CDMA_MT_CALL.equalsIgnoreCase(mAction)) {
                actionTag = "TV";

            } else if (Const.ACTION_CDMA_UTK_MENU_SELECTION
                    .equalsIgnoreCase(mAction)) {
                actionTag = "UT";

            } else if (Const.ACTION_CDMA_SMS_MSG_SENT.equalsIgnoreCase(mAction)) {
                actionTag = "OS";

            } else if (Const.ACTION_CDMA_DATA_CONNECTION_ACTIVE
                    .equalsIgnoreCase(mAction)) {
                actionTag = "OD";

            }
        }
        Log.d(TAG, "getActionTag() actionTag:" + actionTag);
        return actionTag;
    }
}
