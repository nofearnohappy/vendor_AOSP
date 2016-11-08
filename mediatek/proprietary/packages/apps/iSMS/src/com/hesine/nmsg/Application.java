package com.hesine.nmsg;

import android.content.Intent;

import com.hesine.hstat.HstatSdk;
import com.hesine.hstat.util.DevInfo;
import com.hesine.nmsg.business.bo.Activation;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.LocalStore;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.observer.NmsgService;
import com.hesine.nmsg.thirdparty.Location;
import com.hesine.nmsg.thirdparty.PNControler;
import com.hesine.nmsg.thirdparty.PNMessageHandler;

public class Application extends android.app.Application {
    private static Application instance;

    public Application() {
        super();
        instance = this;
    }

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLog.init();
        MLog.readIniFile();
        HstatSdk.init(this);
        LocalStore store = LocalStore.instance();
        if (store.isDbUpgrade()) {
            store.dbDbUpgrade();
        }

        Location.getInstance().registerLocationClient(this);

        if (!Config.getIsActivated()) {
            Activation.instance().start();
        } else {
            PNControler.startPN(getApplicationContext());
        }

        if (!NmsgService.getInstance().isServiceStart()) {
            MLog.error("NmsgService is not running, so start it from application");
            Intent i = new Intent(Application.getInstance(), NmsgService.class);
            Application.getInstance().startService(i);
        }

        if (DeviceInfo.isWifiNetwork(this)) {
            Config.saveWifiConnected(true);
        }

        PNMessageHandler.procPendingStatusMsgs();

        MLog.error("version:" + GlobalData.getVersionName() + " pn:" + Config.getPnToken()
                + " uuid:" + Config.getUuid() + " imsi:" + Config.getImsi() + " model:"
                + DeviceInfo.getDeviceModel() + " brand:" + DeviceInfo.getDeviceBrand()
                + " Hstat Id:" + DevInfo.getDeviceId(this));
    }
}
