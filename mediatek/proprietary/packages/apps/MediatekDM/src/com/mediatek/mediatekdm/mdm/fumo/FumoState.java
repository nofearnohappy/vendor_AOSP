package com.mediatek.mediatekdm.mdm.fumo;

/**
 * FUMO States. Please refer to Firmware Update Management Object specification Figure 2 for states
 * machine.
 */
public enum FumoState {
    /** <b>Idle/Start</b>. No pending operation. */
    IDLE(10),
    /** <b>Download Failed</b>. Download failed. */
    DOWNLOAD_FAILED(20),
    /** <b>Download Progressing</b>. Download has started. */
    DOWNLOAD_PROGRESSING(30),
    /** <b>Download Complete</b>. Download has been completed successfully. */
    DOWNLOAD_COMPLETE(40),
    /** <b>Read to Update</b>. Have data and awaiting command to start update. */
    UPDATE_READY_TO_UPDATE(50),
    /** <b>Update Progressing</b>. Update has started. */
    UPDATE_PROGRESSING(60),
    /** <b>Update Failed/Have Data</b>. Update failed but have update package. */
    UPDATE_FAILED_HAVE_DATA(70),
    /** <b>Update Failed/No Data</b>. Update failed and no update package available. */
    UPDATE_FAILED_NO_DATA(80),
    /** <b>Update Successful/Have Data</b>. Update complete and data still available. */
    UPDATE_SUCCESSFUL_HAVE_DATA(90),
    /** <b>Update Successful/No Data</b>. Data deleted or removed after a successful Update. */
    UPDATE_SUCCESSFUL_NO_DATA(100);

    public final int val;

    private FumoState(int value) {
        val = value;
    }

    public static FumoState fromInt(int value) {
        for (FumoState s : FumoState.values()) {
            if (s.val == value) {
                return s;
            }
        }
        return null;
    }
}
