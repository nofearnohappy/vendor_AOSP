package com.mediatek.telecom.plugin;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.telecom.ext.DefaultPhoneAccountExt;

import java.util.List;
import java.util.Objects;

/**
 * PhoneAccount extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.telecom.ext.IPhoneAccountExt")
public class OP09PhoneAccountExt extends DefaultPhoneAccountExt {

    private static final String TAG = "OP09PhoneAccountExt";

    /**
     * should remove the default MO phone account.
     *
     * @param accountHandleList
     *            capable account list
     * @return true if need to remove.
     */
    @Override
    public boolean shouldRemoveDefaultPhoneAccount(List<PhoneAccountHandle> accountHandleList) {
        if (null != accountHandleList && accountHandleList.size() == 2) {
            return false;
        }
        return true;
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

