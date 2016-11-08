package com.hesine.nmsg.business.bean.response;

import com.hesine.nmsg.business.bean.PicInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;

public class GetData extends Base {

    private ServiceInfo serviceInfo = null;
    private PicInfo picInfo = null;

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public PicInfo getPicInfo() {
        return picInfo;
    }

    public void setPicInfo(PicInfo picInfo) {
        this.picInfo = picInfo;
    }

}
