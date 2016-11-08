package com.mediatek.mediatekdm.mdm.fumo;

public class FumoAction {
    /** No action has been performed during the last session. */
    public static final int NONE = 0;
    /** An Exec command has been performed on the 'Download' node. */
    public static final int DOWNLOAD_EXECUTED = 1;
    /** An Exec command has been performed on the 'DownloadAndUpdate' node. */
    public static final int DOWNLOAD_AND_UPDATE_EXECUTED = 2;
    /** An Exec command has been performed on the 'Update' node. */
    public static final int UPDATE_EXECUTED = 4;
    /** A Replace command has been performed on the 'PkgData' node. */
    public static final int PKGDATA_REPLACED = 8;
    public static final int ALL = DOWNLOAD_EXECUTED | DOWNLOAD_AND_UPDATE_EXECUTED
            | UPDATE_EXECUTED | PKGDATA_REPLACED;
}
