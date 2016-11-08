package com.mediatek.settings.plugin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.telephony.TelephonyManagerEx;
/**
* Switch rat mode.
*/
public class ChangeRatModeService extends Service {

    private static final String TAG = "ChangeRatModeService";
    private static final String MODE_STATUS = "mode_status";
    private static final String LTETDD_CDMA = "ltetdd_cdma";

    private final IBinder mSetRatMode = new SetRatMode();
    private TelephonyManagerEx mTelephonyManagerEx = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "ChangeRatModeService onCreate");
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
    }
    @Override
    public void onDestroy() {
        Log.i(TAG, "ChangeRatModeService onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        Log.i(TAG, "ChangeRatModeService start");
        try {
            saveData(true);
            switchSvlte(true);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mSetRatMode;
    }
    /**
    *Binder for client.
    */
    public class SetRatMode extends Binder {
        public ChangeRatModeService getService() {
            return ChangeRatModeService.this;
        }
    }
    /**
    * Save RAT mode.
    * @param isChecked open or close the 4G switch.
    * @throws RemoteException remote exception
    */
    public void saveData(boolean isChecked) throws RemoteException {
        if (!isChecked) {
            int lastMode = Settings.Global.getInt(this.getContentResolver(),
                                  mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                                      .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                                                  TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            Log.i(TAG, "saveData isChecked = false lastMode = " + lastMode);
            this.getSharedPreferences(MODE_STATUS, Context.MODE_PRIVATE)
                .edit().putInt(LTETDD_CDMA, lastMode).commit();
            Settings.Global.putInt(this.getContentResolver(),
                        mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                            .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                        TelephonyManagerEx.SVLTE_RAT_MODE_3G);
        } else {
            int lteCdma = this.getSharedPreferences(MODE_STATUS, Context.MODE_PRIVATE)
                .getInt(LTETDD_CDMA, -1);
            Log.i(TAG, "saveData isChecked = true lteCdma = " + lteCdma);
            if (lteCdma != -1) {
                Settings.Global.putInt(this.getContentResolver(),
                        mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                            .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                        lteCdma);
            } else {
                Settings.Global.putInt(this.getContentResolver(),
                        mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                            .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                        TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            }
        }
    }
    /**
    * Switch RAT mode.
    * @param isChecked open or close the 4G switch.
    * @throws RemoteException remote exception
    */
    public void switchSvlte(boolean isChecked) throws RemoteException {
        ITelephonyEx telephony = ITelephonyEx.Stub.asInterface(
                  ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (null != telephony) {
            if (!isChecked) {
                Log.i(TAG, "switchSvlte isChecked = false switchSvlteRatMode 3g");
                telephony.switchRadioTechnology(SvlteRatController.RAT_MODE_SVLTE_2G
                              | SvlteRatController.RAT_MODE_SVLTE_3G);
                Log.i(TAG, "switchSvlte switchRadioTechnology 1, value= " +
                        (SvlteRatController.RAT_MODE_SVLTE_2G
                         | SvlteRatController.RAT_MODE_SVLTE_3G));
            } else {
                int lteCdma = this.getSharedPreferences(MODE_STATUS,
                        Context.MODE_PRIVATE).getInt(LTETDD_CDMA, -1);

                Log.i(TAG, "switchSvlte isChecked = true last mode lte_cdma = " + lteCdma);
                if (lteCdma != -1) {
                    Log.i(TAG, "switchSvlte saveData isChecked = true put LTETDD_CDMA = -1");
                    this.getSharedPreferences(MODE_STATUS, Context.MODE_PRIVATE)
                        .edit().putInt(LTETDD_CDMA, -1).commit();

                    if (lteCdma == TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY) {
                        Log.i(TAG, "switchSvlte last mode SVLTE_RAT_MODE_4G_DATA_ONLY");
                        telephony.switchRadioTechnology(SvlteRatController.RAT_MODE_SVLTE_4G);
                        Log.i(TAG, "switchSvlte 4G_DATA_ONLY, value= " +
                            SvlteRatController.RAT_MODE_SVLTE_4G);
                    } else if (lteCdma == TelephonyManagerEx.SVLTE_RAT_MODE_4G) {
                        Log.i(TAG, "switchSvlte last mode SVLTE_RAT_MODE_4G");
                        telephony.switchRadioTechnology((SvlteRatController.RAT_MODE_SVLTE_2G
                            | SvlteRatController.RAT_MODE_SVLTE_3G
                            | SvlteRatController.RAT_MODE_SVLTE_4G));
                        Log.i(TAG, "switchSvlte RAT_MODE_4G, value= " +
                                                (SvlteRatController.RAT_MODE_SVLTE_2G
                                                | SvlteRatController.RAT_MODE_SVLTE_3G
                                                | SvlteRatController.RAT_MODE_SVLTE_4G));
                    } else {
                        Log.i(TAG, "switchSvlte last mode error, do open 4G mode");
                        telephony.switchRadioTechnology(SvlteRatController.RAT_MODE_SVLTE_2G
                            | SvlteRatController.RAT_MODE_SVLTE_3G
                            | SvlteRatController.RAT_MODE_SVLTE_4G);
                        Log.i(TAG, "switchSvlte mode error, value= " +
                                                (SvlteRatController.RAT_MODE_SVLTE_2G
                                                | SvlteRatController.RAT_MODE_SVLTE_3G
                                                | SvlteRatController.RAT_MODE_SVLTE_4G));
                    }
                } else {
                    telephony.switchRadioTechnology(SvlteRatController.RAT_MODE_SVLTE_2G
                            | SvlteRatController.RAT_MODE_SVLTE_3G
                            | SvlteRatController.RAT_MODE_SVLTE_4G);
                    Log.i(TAG, "switchSvlte switchRadioTechnology 2, value= " +
                            (SvlteRatController.RAT_MODE_SVLTE_2G
                              | SvlteRatController.RAT_MODE_SVLTE_3G
                              | SvlteRatController.RAT_MODE_SVLTE_4G));
                }
            }
        }
    }

}
