package com.hesine.nmsg.business.bean.response;

import com.hesine.nmsg.business.bean.ClientUpdateInfo;

public class Upgrade extends Base {

    private ClientUpdateInfo clientUpdateInfo = null;

    public ClientUpdateInfo getClientUpdateInfo() {
        return clientUpdateInfo;
    }

    public void setClientUpdateInfo(ClientUpdateInfo versionInfo) {
        this.clientUpdateInfo = versionInfo;
    }

}
