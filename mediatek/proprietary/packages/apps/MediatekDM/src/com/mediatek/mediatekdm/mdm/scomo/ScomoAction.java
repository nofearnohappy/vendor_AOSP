package com.mediatek.mediatekdm.mdm.scomo;

public class ScomoAction {
    public static final int NONE = 0;
    public static final int DOWNLOAD_EXECUTED = 1;
    public static final int DOWNLOAD_INSTALL_EXECUTED = 2;
    public static final int DOWNLOAD_INSTALL_INACTIVE_EXECUTED = 4;
    public static final int INSTALL_EXECUTED = 8;
    public static final int INSTALL_INACTIVE_EXECUTED = 16;
    public static final int DP_REMOVE_EXECUTED = 32;
    public static final int DP_DATA_REPLACED = 64;
    public static final int ACTIVATE_EXECUTED = 128;
    public static final int DC_REMOVE_EXECUTED = 512;
    public static final int DEACTIVATE_EXECUTED = 256;
    public static final int ALL = DOWNLOAD_EXECUTED | DOWNLOAD_INSTALL_EXECUTED
            | DOWNLOAD_INSTALL_INACTIVE_EXECUTED | INSTALL_EXECUTED | INSTALL_INACTIVE_EXECUTED
            | DP_REMOVE_EXECUTED | DP_DATA_REPLACED | ACTIVATE_EXECUTED | DC_REMOVE_EXECUTED
            | DEACTIVATE_EXECUTED;
}
