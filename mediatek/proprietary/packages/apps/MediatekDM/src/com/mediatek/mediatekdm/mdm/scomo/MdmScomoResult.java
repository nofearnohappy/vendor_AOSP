package com.mediatek.mediatekdm.mdm.scomo;

public class MdmScomoResult {
    public static final int SUCCESSFUL = 1200;
    public static final int CLIENT_ERROR = 1400;
    public static final int DOWNLOAD_CANCELED = 1401;
    public static final int USER_CANCELED = 1401;
    public static final int DOWNLOAD_FAILED = 1402;
    public static final int AUTH_FAILED = 1403;
    public static final int OUT_OF_MEMORY_FOR_DOWNLOAD = 1404;
    public static final int INSTALL_FAILED = 1405;
    public static final int OUT_OF_MEMORY_FOR_INSTALL = 1406;
    public static final int SIGNATURE_FAILED = 1407;
    public static final int ACTIVATE_FAILED = 1409;
    public static final int REMOVE_FAILED = 1408;
    public static final int DEACTIVATE_FAILED = 1410;
    public static final int NOT_IMPLEMENTED = 1411;
    public static final int UNDEFINED_ERROR = 1412;
    public static final int UNSUPPORTED_ENV = 1413;
    public static final int DL_SERVER_ERROR = 1500;
    public static final int DL_SERVER_UNAVAILABLE = 1501;

    public final int val;

    public MdmScomoResult(int result) {
        val = result;
    }
}
