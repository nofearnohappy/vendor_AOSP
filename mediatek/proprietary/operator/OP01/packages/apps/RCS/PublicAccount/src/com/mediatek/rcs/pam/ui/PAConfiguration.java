package com.mediatek.rcs.pam.ui;

public class PAConfiguration {
    private static PAConfiguration sInstance = null;

    public synchronized static PAConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new PAConfiguration();
        }
        return sInstance;
    }

    private PAConfiguration() {
    }

    public String getServerUrl() {
        // FIXME
        return null;
    }

    public String getNafUrl() {
        // FIXME
        return null;
    }

    public String getUserId() {
        // FIXME
        return null;
    }
}
