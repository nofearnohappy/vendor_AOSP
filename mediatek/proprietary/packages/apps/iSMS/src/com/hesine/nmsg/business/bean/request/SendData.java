package com.hesine.nmsg.business.bean.request;

import com.hesine.nmsg.business.bean.PicInfo;
import com.hesine.nmsg.business.bean.UserInfo;

public class SendData extends Base {

    private UserInfo userInfo = null;
    private PicInfo picInfo = null;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public PicInfo getPicInfo() {
        return picInfo;
    }

    public void setPicInfo(PicInfo picInfo) {
        this.picInfo = picInfo;
    }

}
