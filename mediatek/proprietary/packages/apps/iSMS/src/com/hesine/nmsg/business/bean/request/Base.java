package com.hesine.nmsg.business.bean.request;

import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.ClientInfo;
import com.hesine.nmsg.business.bean.SystemInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.GlobalData;

public class Base {

    private SystemInfo systemInfo = null;//
    private ClientInfo clientInfo = null;// GlobalData.instance().getClientInfo();
    private ActionInfo actionInfo = null;

    public Base() {
        if (null == systemInfo) {
            systemInfo = GlobalData.instance().getSystemInfo();
        }
        if (null == clientInfo) {
            clientInfo = GlobalData.instance().getClientInfo();
        }
        if (null == actionInfo) {
            actionInfo = new ActionInfo();
        }
        systemInfo.setUuid(Config.getUuid());
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public ActionInfo getActionInfo() {
        return actionInfo;
    }

    public void setActionInfo(ActionInfo actionInfo) {
        this.actionInfo = actionInfo;
    }

    public static String getStringValue(String value) {
        if (value == null) {
            value = "";
        }
        return value;
    }

    public static long getLongValue(Long value) {
        if (value == null) {
            value = 0L;
        }
        return value;
    }

    public static int getIntegerValue(Integer value) {
        if (value == null) {
            value = 0;
        }
        return value;
    }

    public static float getFloatValue(Float value) {
        if (value == null) {
            value = 0f;
        }
        return value;
    }
}
