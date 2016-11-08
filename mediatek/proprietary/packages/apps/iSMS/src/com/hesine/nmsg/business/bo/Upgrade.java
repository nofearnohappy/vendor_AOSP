package com.hesine.nmsg.business.bo;

import android.annotation.SuppressLint;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.R;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.ClientUpdateInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.ui.NmsgNotification;

public class Upgrade extends
        Base<com.hesine.nmsg.business.bean.request.Base, com.hesine.nmsg.business.bean.response.Upgrade> {

    public static final long ONE_MONTH = 2592000000L;
    public static final long YEAR_ABOUT_2010 = 40 * 12 * ONE_MONTH;

    @Override
    public com.hesine.nmsg.business.bean.request.Base contentObject() {
        com.hesine.nmsg.business.bean.request.Base obj = new com.hesine.nmsg.business.bean.request.Base();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_UPGRADE);
        obj.getActionInfo().setServiceAccount(GlobalData.instance().getCurServiceAccount());
        return obj;
    }

    @Override
    public com.hesine.nmsg.business.bean.response.Upgrade parseObject() {
        return new com.hesine.nmsg.business.bean.response.Upgrade();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.Upgrade parseData, int success) {
        if (success >= Pipe.NET_SUCCESS) {
            ClientUpdateInfo clientUpdateInfo = parseData.getClientUpdateInfo();
            procNetworkUpgrade(clientUpdateInfo);
        }
    }

    @SuppressLint("DefaultLocale")
    private static int compareVersion(String newVersion, String oldVersion) {
        String[] osvs = oldVersion.substring(oldVersion.toUpperCase().indexOf('V') + 1)
                .split("[.]");
        String[] nsvs = newVersion.substring(newVersion.toUpperCase().indexOf('V') + 1)
                .split("[.]");
        for (int i = 0; i < osvs.length; i++) {
            int osv = Integer.parseInt(osvs[i]);
            int nsv = Integer.parseInt(nsvs[i]);
            if (nsv > osv) {
                return 1;
            } else if (nsv < osv) {
                return -1;
            }
        }

        return 0;
    }

    private static void notifyAppDownload(String url) {
        String ticker = Application.getInstance().getString(R.string.update_title);
        String title = Application.getInstance().getString(R.string.update_title);
        String content = Application.getInstance().getString(R.string.update_content);
        NmsgNotification.getInstance(Application.getInstance().getApplicationContext())
                .showApkDownloadNotification(url, ticker, title, content);
    }

    public static void procNetworkUpgrade(ClientUpdateInfo clientUpdateInfo) {
        if (null != clientUpdateInfo) {
            String cV = Config.getNewVersion();
            String nv = clientUpdateInfo.getVersion();
            boolean saveUpgradeConfig = false;
            if (cV != null) {
                if (Upgrade.compareVersion(nv, cV) > 0) {
                    saveUpgradeConfig = true;
                }
            } else {
                saveUpgradeConfig = true;
            }
            if (saveUpgradeConfig) {
                Config.saveLastUpgradeTime(System.currentTimeMillis());
                Config.saveNewVersion(clientUpdateInfo.getVersion());
                String url = clientUpdateInfo.getUrl();
                Config.saveNewClientUrl(url);
                notifyAppDownload(url);
            }
        }
    }

    private static void procLocalUpgrade() {
        long lastUpdateTime = Config.getLastUpgradeTime();
        String cV = GlobalData.instance().getClientInfo().getVersion();
        String nv = Config.getNewVersion();
        if (nv != null && lastUpdateTime > Upgrade.YEAR_ABOUT_2010) {
            long curTime = System.currentTimeMillis();
            long offTime = curTime - lastUpdateTime;
            if (Upgrade.compareVersion(nv, cV) > 0 && offTime > Upgrade.ONE_MONTH) {
                String url = Config.getNewClientUrl();
                Config.saveLastUpgradeTime(System.currentTimeMillis());
                Upgrade.notifyAppDownload(url);
            }
        }
    }

    public static void procUpgrade(String version) {
        boolean needUpgrade = false;
        if (null != version) {
            String cV = Config.getNewVersion();
            String nv = version;
            if (cV != null) {
                if (Upgrade.compareVersion(nv, cV) > 0) {
                    needUpgrade = true;
                }
            } else {
                needUpgrade = true;
            }
        }

        if (!needUpgrade) {
            procLocalUpgrade();
        } else {
            Upgrade upgrade = new Upgrade();
            upgrade.start();
        }
    }

    public static void checkVersion() {
        procLocalUpgrade();
        // Make sure that checking version from the server as well as having new
        // local version
        Upgrade upgrade = new Upgrade();
        upgrade.start();
    }

}
