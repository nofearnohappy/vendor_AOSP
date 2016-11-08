/** 
 * Project Name:nmsgclient 
 * File Name:Deactivation.java 
 * Package Name:com.hesine.nmsg.api 
 * Function:去激活
 * Date:2014-12-16 11:24:25 
 * Copyright (c) 2014, Hesine All Rights Reserved. 
 * 
 */

package com.hesine.nmsg.business.bo;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.MLog;

/** 
 * ClassName: Deactivation <br/> 
 * Function: 去激活类. <br/> 
 * date: 2014年12月24日 上午9:46:42 <br/> 
 * 
 * @author HST00113 
 * @version  
 */  
public class Deactivation
        extends
        com.hesine.nmsg.business.bo.Base<com.hesine.nmsg.business.bean.request.Base, 
                                       com.hesine.nmsg.business.bean.response.Base> {

    private static Deactivation ins = null;
    public static Deactivation instance() {
        if (null == ins) {
            ins = new Deactivation();
        }
        return ins;
    }

   
    /** 
     * 组装request数据结构. 
     * @see com.hesine.nmsg.business.bo.Base#contentObject() 
     */  
    @Override
    public com.hesine.nmsg.business.bean.request.Base contentObject() {
        com.hesine.nmsg.business.bean.request.Base obj = new com.hesine.nmsg.business.bean.request.Base();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_DEACTIVATE);
        obj.getSystemInfo().setUuid(Config.getDeactiveUuid());
        return obj;
    }

   
    @Override
    public com.hesine.nmsg.business.bean.response.Base parseObject() {
        return new com.hesine.nmsg.business.bean.response.Base();
    }


    /** 
     * 接收到服务器下发的激活响应后的处理. 
     * @see com.hesine.nmsg.business.bo.Base#procReplyDataStore(java.lang.Object, int) 
     */  
    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.Base parseData, int success) {
        if (success >= Pipe.NET_SUCCESS) {
            MLog.info("deactivation deactive success");
            Config.saveDeactiveUuid("");
            Config.saveDeactivatationIp("");
        }
    }

    @Override
    public String getUrl() {
        MLog.info("deactivation IP :" + Config.getDeactivatationIp());
        return Config.getDeactivatationIp();
    }

}
