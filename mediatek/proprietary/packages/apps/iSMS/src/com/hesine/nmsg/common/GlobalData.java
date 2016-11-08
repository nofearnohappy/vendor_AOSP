package com.hesine.nmsg.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.bean.ClientInfo;
import com.hesine.nmsg.business.bean.SystemInfo;
import com.hesine.nmsg.business.dao.Config;

public class GlobalData {

    private static GlobalData ins = null;

    public static GlobalData instance() {
        if (null == ins) {
            ins = new GlobalData();
        }
        return ins;
    }

    GlobalData() {
        initSystemInfo();
        initClientInfo();
    }

    private SystemInfo systemInfo = null;
    private ClientInfo clientInfo = null;
    private String id = null;
    private String curServiceAccount = null;

    private void initSystemInfo() {
        Context context = Application.getInstance().getApplicationContext();
        systemInfo = new SystemInfo();
        systemInfo.setPhoneNum(DeviceInfo.getPhonenum(context));
        systemInfo.setImsi(DeviceInfo.getIMSI(context));
        systemInfo.setImei(DeviceInfo.getIMEI(context));
        systemInfo.setDevice(DeviceInfo.getDeviceModel());
        systemInfo.setBrand(DeviceInfo.getDeviceBrand());
        systemInfo.setLanguage(DeviceInfo.getLanuage(context));
        systemInfo.setPnToken(Config.getPnToken());
        systemInfo.setPnType("HPNS");
        systemInfo.setOsver(DeviceInfo.getSystemVersion());
        systemInfo.setUuid(Config.getUuid());
    }

    private void initClientInfo() {
        clientInfo = new ClientInfo();
        clientInfo.setVersion(getVersionName()/* EnumConstants.version */);
        clientInfo.setChannelId(EnumConstants.CHANNEL_ID);
        clientInfo.setAppName("nmsg");
    }

    public static String getVersionName() {
        try {
            Context context = Application.getInstance();
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
            return "V1.00.000.000.00";
        }
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurServiceAccount() {
        return curServiceAccount;
    }

    public void setCurServiceAccount(String curServiceAccount) {
        this.curServiceAccount = curServiceAccount;
    }

}
