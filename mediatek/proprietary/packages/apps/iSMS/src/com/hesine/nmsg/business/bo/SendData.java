package com.hesine.nmsg.business.bo;

import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.PicInfo;
import com.hesine.nmsg.business.bean.UserInfo;

public class SendData extends
        Base<com.hesine.nmsg.business.bean.request.SendData, com.hesine.nmsg.business.bean.response.Base> {

    private UserInfo mUserInfo = null;
    private PicInfo mPicInfo = null;

    // if need be called when launcher send msg
    public void setMessageInfo(UserInfo info) {
        mUserInfo = info;
    }

    public void setPicInfo(PicInfo info) {
        mPicInfo = info;
    }

    @Override
    public com.hesine.nmsg.business.bean.request.SendData contentObject() {
        com.hesine.nmsg.business.bean.request.SendData obj = new com.hesine.nmsg.business.bean.request.SendData();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_SEND_DATA);
        obj.setUserInfo(mUserInfo);
        obj.setPicInfo(mPicInfo);
        return obj;
    }

    @Override
    public void procRequestDataStore(com.hesine.nmsg.business.bean.request.SendData submitData) {
        // TODO if need save userinfo data to DB before send?
    }

    @Override
    public com.hesine.nmsg.business.bean.response.Base parseObject() {
        return new com.hesine.nmsg.business.bean.response.Base();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.Base parseData, int success) {
    }

}
