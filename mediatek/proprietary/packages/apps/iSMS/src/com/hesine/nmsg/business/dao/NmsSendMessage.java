package com.hesine.nmsg.business.dao;

import java.util.Set;

import android.text.TextUtils;

public class NmsSendMessage {

    public static final int UNSAVE_SMS = 0x00;
    public static final int SAVE_SMS = 0x01;
    public static final int MAX_SMS_LENGTH = 70;
    public static final String SMS_ID = "id";
    public static final String SMS_SENT_ACTION = "com.hissage.nmssendmessage.sent_action";
    public static final String SMS_DELIVER_ACTION = "com.hissage.nmssendmessage.deliver_action";

    public static NmsSendMessage mInstance = null;

    class SmsCont {
        SmsCont(String strAddrIn, String strMsgIn, long lThreadId, long lSmsIdIn, long simId) {
            strAddr = strAddrIn;
            strMsg = strMsgIn;
            lSmsId = lSmsIdIn;
            threadId = lThreadId ;
            this.simId = simId;
        }

        public String strAddr;
        public String strMsg;
        public long lSmsId;
        public long threadId ;
        public long simId;
    }
    
    public static NmsSendMessage getInstance() {
        if (null == mInstance) {
            mInstance = new NmsSendMessage();
        }
        return mInstance;
    }

    public boolean isAddressLegal(String strAddrIn, Set<String> setAddrIn) {
        if (TextUtils.isEmpty(strAddrIn)) {
            return false;
        }
        String[] strArrayAddr = strAddrIn.split(",");
        for (String strAddrTemp : strArrayAddr) {
            setAddrIn.add(strAddrTemp);
        }
        if (setAddrIn.size() > 0) {
            return true;
        }
        return false;
    }

}
