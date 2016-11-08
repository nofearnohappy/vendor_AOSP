package com.mediatek.mediatekdm.mdm.lawmo;

public class LawmoResultCode {
    public static final int CLIENT_ERROR = 1400;
    public static final int FULLY_LOCK_FAILED = 1402;
    public static final int OPERATION_SUCCESSSFUL = 1200;
    public static final int PARTIALLY_LOCK_FAILED = 1403;
    public static final int UNLOCK_FAILED = 1404;
    public static final int USER_CANCELLED = 1401;
    public static final int WIPE_DATA_AND_ASSOCIATED_LIST_ITEMS_SUCCESSFUL = 1202;
    public static final int WIPE_DATA_FAILED = 1405;
    public static final int WIPE_DATA_NOT_PERFORMED = 1406;
    public static final int WIPE_DATA_SUCCESSFUL = 1201;

    public final int code;

    public LawmoResultCode(int resultCode) {
        code = resultCode;
    }

}
