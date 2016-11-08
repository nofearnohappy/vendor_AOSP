package com.android.mms;

import android.content.Context;

import com.android.mms.util.MmsLog;
import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.INmsgPluginExt;
import com.mediatek.mms.ext.DefaultNmsgPluginExt;

public class MmsPluginManager {

    private static String TAG = "MmsPluginManager";

    /// M: add for ALPS01749707 for ipmessage plugin
    public static final int MMS_PLUGIN_TYPE_IPMSG_PLUGIN = 0X0031;

    /// M: add for ALPS01766374 for ipmessage plugin
    private static INmsgPluginExt mNmsgPlugin = null;

    public static void initPlugins(Context context) {
        /// M: add for ALPS01766374 for ipmessage plugin @{
        mNmsgPlugin = (INmsgPluginExt) MPlugin.createInstance(INmsgPluginExt.class.getName(),context);
        if(mNmsgPlugin == null) {
        	mNmsgPlugin = new DefaultNmsgPluginExt(context);
            MmsLog.d(TAG, "default DefaultNmsgPluginExt = " + mNmsgPlugin);
        }
        ///@}
    }

    public static Object getMmsPluginObject(int type) {
        Object obj = null;
        MmsLog.d(TAG, "getMmsPlugin, type = " + type);
        switch(type) {

            /// M: add for ALPS01766374 for ipmessage plugin @{
            case  MMS_PLUGIN_TYPE_IPMSG_PLUGIN:
              obj = mNmsgPlugin;
             break;
            ///@}
            default:
                MmsLog.e(TAG, "getMmsPlugin, type = " + type + " don't exist");
                break;
        }
        return obj;

    }
}
