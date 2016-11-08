package com.mediatek.deviceregister;

public class Const {

    public static final String TAG_PREFIX = "DeviceRegister/";

    public static final String ACTION_BOOTCOMPLETED = "android.intent.action.BOOT_COMPLETED";

    public static final String ACTION_PRE_BOOT_COMPLETED =
            "android.intent.action.PRE_BOOT_COMPLETED";

    //Broadcast when network is OK (then can begin to register)
    public static final String ACTION_REGISTER_FEASIBLE =
            "android.provider.Telephony.CDMA_AUTO_SMS_REGISTER_FEASIBLE";

    // Broadcast from SMS framework, when Server returns register result
    public static final String ACTION_CT_CONFIRMED_MESSAGE =
            "android.telephony.sms.CDMA_REG_SMS_ACTION";

    // Broadcast when register SMS has been sent successfully
    public static final String ACTION_MESSAGE_SEND =
            "com.mediatek.deviceregister.MESSAGE_SEND";

    // Shared preferences to store flag whether need to listen feasible broadcast
    public static final String PRE_KEY_NOT_FIRST_SUBINFO = "pref_key_not_first_subinfo";

    public static final String VALUE_DEFAULT_IMSI = "000000000000000";
}
