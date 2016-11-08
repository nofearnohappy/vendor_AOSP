package com.mediatek.engineermode.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.os.RemoteException;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.wifi.EmPerformanceWrapper;
import com.mediatek.engineermode.R;

/**
 * a broadcast Receiver of EM boot.
 * @author: mtk81238
 */
public class EmBootupReceiver extends BroadcastReceiver {

    private static final String TAG = "EM/BootupReceiver";
    private static final String MODEM_FILTER_SHAREPRE= "telephony_modem_filter_settings";
    private static final String EHRPD_BG_DATA_SHREDPRE_NAME = "ehrpdBgData";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            onBootupCompleted(context, intent);
        }
    }

    private void onBootupCompleted(Context context, Intent intent) {
        Elog.d(TAG, "Start onBootupCompleted");
        if (FeatureSupport.isSupported(FeatureSupport.FK_C2K_SUPPORT)) {
            onBootupUsbTethering(context);
        }
        if (ChipSupport.isFeatureSupported(ChipSupport.MTK_WLAN_SUPPORT)) {
            if (EmPerformanceWrapper.isPerfSettingEnabled(context)) {
                EmBootStartService.requestStartService(context, new WifiSpeedUpBootHandler());
            }
        }

        //MD EM filter
        writeSharedPreference(context, false);

        setEhrpdBackgroundData(context);

        Elog.d(TAG, "End onBootupCompleted");
    }

    private void onBootupUsbTethering(Context context) {
        if (UsbTetheringBootHandler.isSupportBootUsbTethering()) {
            EmBootStartService.requestStartService(context, new UsbTetheringBootHandler());
        }
    }

    private void writeSharedPreference(Context context, boolean flag) {
        final SharedPreferences modemFilterSh = context.getSharedPreferences(
                         MODEM_FILTER_SHAREPRE, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = modemFilterSh.edit();
        editor.putBoolean(context.getString(R.string.enable_md_filter), flag);
        editor.commit();
    }

    private void setEhrpdBackgroundData(Context context) {
        final String BUTTON_FLAG = "flag";
        INetworkManagementService nwService = INetworkManagementService.Stub.asInterface(
            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));

        final SharedPreferences autoAnswerSh = context.getSharedPreferences(
                EHRPD_BG_DATA_SHREDPRE_NAME,
                android.content.Context.MODE_WORLD_READABLE);
        boolean mEhrpdBgDataEnable = autoAnswerSh.getBoolean(BUTTON_FLAG, false);
        if (null != nwService) {
            try {
                if (mEhrpdBgDataEnable) {
                    Elog.d(TAG, "setIotFirewall");
                    nwService.setIotFirewall();
                }
            } catch (RemoteException e) {
                Elog.d(TAG, "RomoteException");
            }
        } else {
            Elog.d(TAG, "nwService == null");
        }
    }
}
