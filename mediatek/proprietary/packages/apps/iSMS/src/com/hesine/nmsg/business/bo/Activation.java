package com.hesine.nmsg.business.bo;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Location;
import com.hesine.nmsg.thirdparty.PNControler;
import com.hesine.nmsg.thirdparty.Statistics;

public class Activation
        extends
        com.hesine.nmsg.business.bo.Base<com.hesine.nmsg.business.bean.request.Base, 
        com.hesine.nmsg.business.bean.response.Activation> {

    private static Activation ins = null;

    public static Activation instance() {
        if (null == ins) {
            ins = new Activation();
        }
        return ins;
    }

    @Override
    public com.hesine.nmsg.business.bean.request.Base contentObject() {
        com.hesine.nmsg.business.bean.request.Base obj = new com.hesine.nmsg.business.bean.request.Base();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_ACTIVATE);
        return obj;
    }

    @Override
    public com.hesine.nmsg.business.bean.response.Activation parseObject() {
        return new com.hesine.nmsg.business.bean.response.Activation();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.Activation parseData,
            int success) {
        if (success >= Pipe.NET_SUCCESS) {
            String redirectIp = Config.getRedirectIp();
            boolean isRedirect = false;
            if (redirectIp != null && !redirectIp.equals("")) {
                isRedirect = true;
                MLog.info("redirect success");
                Config.saveDeactiveUuid(Config.getUuid());
                Config.saveDeactivatationIp(Config.getIp());
                MLog.info("save deactive uuid:" + Config.getUuid() + ", IP:" + Config.getIp());
            }
            Config.saveUuid(parseData.getUuid());
            Config.saveIsActivated(true);
            Config.saveIp(getUrl());
            Config.saveRedirectIp("");
            CommonUtils.clearLatestWifiMsgIds();
            GlobalData.instance().getSystemInfo().setUuid(parseData.getUuid());
            Statistics.getInstance().uuid(parseData.getUuid());
            PNControler.startPN(Application.getInstance().getApplicationContext());
            Location.getInstance().requestLocation();
            Statistics.getInstance().apkListInfo();
            if (isRedirect) {
                Deactivation.instance().start();
                MLog.info("deactive start");
            }
        }
    }

    @Override
    public String getUrl() {
        String redirectIp = Config.getRedirectIp();
        if (redirectIp == null || redirectIp.equals("")) {
            return super.getUrl();
        } else {
            return redirectIp;
        }
    }
}
