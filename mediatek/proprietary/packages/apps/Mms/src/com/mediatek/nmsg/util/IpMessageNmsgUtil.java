package com.mediatek.nmsg.util;

import android.content.Context;

import com.android.mms.MmsPluginManager;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.mediatek.mms.ext.INmsgPluginExt;

/**
* Provide api for nmsg.
*/
public class IpMessageNmsgUtil {

    /**
    * open type.
    */
    public static final class OpenType {
        public static final String SMS_LIST = "1";
        public static final String NOTIFICATION = "2";
        public static final String SEARCH_LIST = "3";
    }

    /**
    * Start activitys.
    * @param context  the context
    * @param cv  the conversation
    * @param type  the type
    * @return boolean  success or not
    */
    public static boolean startNmsgActivity(Context context, Conversation cv,
            String type) {

        ContactList list = cv.getRecipients();
        String number = null;
        if (list == null || list.size() != 1) {
            return false;
        }
        INmsgPluginExt nmsgPlugin = (INmsgPluginExt) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_IPMSG_PLUGIN);
        return nmsgPlugin.startRemoteActivity(context, cv.getThreadId(), list
                .get(0).getNumber(), type);
    }

    /**
    * Check nmsg service wether is running.
    * @param context
    */
    public static void nmsgCheckService() {
    INmsgPluginExt nmsgPlugin = (INmsgPluginExt) MmsPluginManager
        .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_IPMSG_PLUGIN);
        nmsgPlugin.nmsgCheckService();
	}
}
