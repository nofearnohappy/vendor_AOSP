package com.mediatek.esntrack;

public class Const {

    public static final String TAG = "EsnTrack";
    public static final String ACTION_BOOTCOMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_DUMMY = "com.mediatek.esntrack.DUMMY";
    public static final String ACTION_CDMA_AUTO_SMS_REGISTER_FEASIBLE = "android.provider.Telephony.CDMA_AUTO_SMS_REGISTER_FEASIBLE";
    public static final String ACTION_CDMA_NEW_OUTGOING_CALL = "com.android.server.telecom.ESN_OUTGOING_CALL_PLACED";
    public static final String ACTION_CDMA_NEW_SMS_RECVD = "android.provider.Telephony.SMS_RECEIVED";
    public static final String ACTION_CDMA_MT_CALL = "android.intent.action.SUBSCRIPTION_PHONE_STATE";
    public static final String ACTION_CDMA_UTK_MENU_SELECTION = "com.android.internal.telephony.cdma.utk.ESN_MENU_SELECTION";
    public static final String ACTION_CDMA_SMS_MSG_SENT = "com.android.mms.transaction.TRIGGER_ESN_MSG_SENT";
    public static final String ACTION_REGISTER_SERVICE_FINISH = "com.mediatek.esntrack.SERVICE_FINISH";
    public static final String ACTION_REGISTER_MESSAGE_SEND = "android.intent.action.REGISTER_SMS_SEND";
    public static final String ACTION_CDMA_DATA_CONNECTION_ACTIVE = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String ACTION_CDMA_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    // L1.MP3 Patchback only
    public static final String ACTION_CDMA_ADDRESS_CHANGE_ENGMODE = "com.mediatek.engineermode.EsnTrack.ADDRESS_CHANGE";
    public static final String KEY_ADDRESS = "Address";
    // L1.MP3 Patchback only
    public static final String TATA_SERVER_ADDRESS = "09223053751";
    public static final String MTS_SERVER_ADDRESS = "51718";
    public static final int HALF_MINUTE = 30 * 1000;
    public static final int FIRST_RETRY = 20 * 1000;
    public static final int SECOND_RETRY = 30 * 1000;

    public static final String KEY_RECEIVED_FEASIBLE_BROADCAST = "received_feasible_broadcast";
    public static final String KEY_PU_ESN = "pu_flag";
    public static final String KEY_OG_ESN = "og_flag";
    public static final String KEY_IC_ESN = "ic_flag";
    public static final String KEY_UT_ESN = "ut_flag";
    public static final String KEY_MTS_PU_ESN = "mts_pu_flag";
    public static final String KEY_MTS_OC_ESN = "mts_oc_flag";
    public static final String KEY_MTS_IC_ESN = "mts_ic_flag";
    public static final String KEY_MTS_OS_ESN = "mts_os_flag";
    public static final String KEY_MTS_IS_ESN = "mts_is_flag";
    public static final String KEY_RUIM_ID = "ruim_id";

    public static final int PU_TYPE = 1;
    public static final int OC_TYPE = 2;
    public static final int IC_TYPE = 3;
    public static final int OS_TYPE = 4;
    public static final int IS_TYPE = 5;
    public static final int UT_TYPE = 6;
    public static final int DA_TYPE = 7;

    public static final boolean DEFALT_RECEIVED_FEASIBLE_BROADCAST = false;
    public static final int SEND_MESSAGE_RETRY_TIMES_MAX = 3;
    public static final String PESN_PREFIX = "80";
    public static final String MEID_TO_PESN_HASH_NAME = "SHA-1";
    public static final int RUIM_ID_EF_BYTES = 7;
    public static final int UIM_NONE = -1;
    public static final int[] UIM_ID_LIST = { 0, 1 };
    public static final int[] SINGLE_UIM_ID = { 0 };
    public static final String SPN_TATA = "TATA DOCOMO";
    public static final String SPN_MTS = "MTS";
    public static final int TATA = 1;
    public static final int MTS = 2;
}
