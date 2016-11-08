package com.mediatek.mms.ext;

import android.content.Context;


public interface INmsgPluginExt {

    /**
     * M: check the running status of NmsgService.<br/>
     * @internal
     */
    void nmsgCheckService();

    /**
     * M: start nmsg conversation activity.<br/>
    * @param context  the context
    * @param threadID  conversation threadid
    * @param number  phone number
    * @param type  opentype
    * @return boolean
     * @internal
     */
    boolean startRemoteActivity(Context context, long threadID, String number, String type);
}
