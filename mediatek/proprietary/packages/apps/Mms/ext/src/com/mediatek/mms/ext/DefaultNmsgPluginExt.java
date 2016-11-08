package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;

public class DefaultNmsgPluginExt extends ContextWrapper implements INmsgPluginExt {

    /**
    * Construct function.
    * @param context  the context
    */
    public DefaultNmsgPluginExt(Context context) {
        super(context);
    }

    /**
    * Check nmsg service wether is running.
    */
    @Override
    public void nmsgCheckService() {
    // TODO Auto-generated method stub
    }

    /**
    * Start activitys.
    * @param context  the context
    * @param threadID  conversation threadid
    * @param number  phone number
    * @param type  opentype
    * @return boolean
    */
    @Override
    public boolean startRemoteActivity(Context context, long threadID, String number, String type) {
        // TODO Auto-generated method stub
        return false;
    }
}
