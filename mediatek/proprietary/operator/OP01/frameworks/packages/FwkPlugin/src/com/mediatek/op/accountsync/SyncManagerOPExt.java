package com.mediatek.op.accountsync;


public class SyncManagerOPExt extends SyncManagerExt {
    private static final String TAG = "SyncManagerExtOP01";
    private static final boolean LOG = true;
    private static final boolean isAutoSync = false;

    /**
     * Get the OP SyncAutomatically setting.
     * @return isAutoSync default auto sync setting.
     */
    public boolean getSyncAutomatically() {
        return isAutoSync;
    }
}
