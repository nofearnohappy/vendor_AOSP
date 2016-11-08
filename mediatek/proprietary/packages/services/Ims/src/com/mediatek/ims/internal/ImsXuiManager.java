package com.mediatek.ims.internal;

import android.telephony.TelephonyManager;

/**
 * ImsXuiManager class.
 * To manage XUI
 *
 */
public class ImsXuiManager {
    public String[] mXui;
    static ImsXuiManager sInstance;

    /**
     * Constructor.
     */
    private ImsXuiManager() {
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        mXui = new String[numPhones];
    }

    /**
     * Instance constructor.
     *
     * @return ImsXuiManager instance
     */
    static public ImsXuiManager getInstance() {
        if (sInstance == null) {
            sInstance = new ImsXuiManager();
            sInstance.loadXui();
        }
        return sInstance;
    }


    public String getXui() {
        return mXui[0];
    }

    public String getXui(int phoneId) {
        return mXui[phoneId];
    }


    /**
     * Clear XUI.
     * Should be called if SIM card changed
     *
     */
    public void clearStoredXui() {
        mXui = null;
        // Todo: Clear the NV storage that XUI belongs to.
    }

    /**
     * Update from IMSA.
     *
     * @param xui  XUI
     */
    public void setXui(String xui) {
        mXui[0] = xui;
        // Todo: Save XUI to a NV storage
    }

    /**
     * Update from IMSA.
     *
     * @param phoneId IMS Phone Id
     * @param xui  XUI
     */
    public void setXui(int phoneId, String xui) {
        mXui[phoneId] = xui;
        // Todo: Save XUI to a NV storage
    }

    private void loadXui() {
        // Todo: load XUI from a NV storage
    }
}
