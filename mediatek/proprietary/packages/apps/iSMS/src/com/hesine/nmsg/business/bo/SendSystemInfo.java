package com.hesine.nmsg.business.bo;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.thirdparty.Location;

public class SendSystemInfo
        extends
        com.hesine.nmsg.business.bo.Base<com.hesine.nmsg.business.bean.request.Base, 
                                    com.hesine.nmsg.business.bean.response.Base> {

    public static void updateSystemInfo(Pipe listener) {
        SendSystemInfo mSendSystemInfo = new SendSystemInfo();
        mSendSystemInfo.setListener(listener);
        mSendSystemInfo.request();
    }
    
    @Override
    public com.hesine.nmsg.business.bean.request.Base contentObject() {
        com.hesine.nmsg.business.bean.request.Base obj = new com.hesine.nmsg.business.bean.request.Base();
        ActionInfo actionInfo = new ActionInfo();
        actionInfo.setActionid(ActionInfo.ACTION_ID_SEND_PNTOKEN);
        obj.setActionInfo(actionInfo);
        return obj;
    }

    @Override
    public com.hesine.nmsg.business.bean.response.Base parseObject() {
        return new com.hesine.nmsg.business.bean.response.Base();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.Base parseData,
            int success) {
    }
}
