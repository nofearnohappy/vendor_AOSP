package com.hesine.nmsg.business.bean.response;

import com.hesine.nmsg.business.bean.ResultInfo;

public class Base {

    private Integer actionid = 0;
    private String account = null;
    private ResultInfo resultInfo = null;

    public Integer getActionid() {
        return actionid;
    }

    public void setActionid(Integer actionid) {
        this.actionid = actionid;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public ResultInfo getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(ResultInfo resultInfo) {
        this.resultInfo = resultInfo;
    }

}
