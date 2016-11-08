package com.mtk.offlinek.component;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;
import com.mtk.offlinek.DeviceIF;

public class WifiRunner extends DeviceIF {
	private static WifiManager mWifiManager = null;
	//private static WifiConfiguration mWifiConfig = null;
	private static final int WIFI_OPT_MAX_LOOP = 80;
	private static final int WIFI_OPT_MAX_SLEEP = 200;
	private String TAG = this.getClass().getCanonicalName();
	public static final String msdcLockNode = "";
	
	public WifiRunner(int host_id) {
		super();
		this.host_id = host_id;
	}

	@Override
	public void deviceOn() {
		if (null == mWifiManager) {
			mWifiManager = (WifiManager) mContext
					.getSystemService(Context.WIFI_SERVICE);
			new WifiConfiguration();
		}
		enableWifi();
		doScan();
	}

	@Override
	public void deviceOff() {
		if (null == mWifiManager) {
			mWifiManager = (WifiManager) mContext
					.getSystemService(Context.WIFI_SERVICE);
			new WifiConfiguration();
		}
		disableWifi();
	}

	private void enableWifi() {
		int i = 0;
		if (!mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(true);
			Log.i(TAG, "Enabling Wifi");
			while (!mWifiManager.isWifiEnabled() && i < WIFI_OPT_MAX_LOOP) {
				SystemClock.sleep(WIFI_OPT_MAX_SLEEP);
				i++;
			}
			if (WIFI_OPT_MAX_LOOP == i) {
				Log.w(TAG, "Enable Wifi failed");
			} else {
				Log.i(TAG, "Enable Wifi success: " + i);
			}
			//CmdAgent.doCommand("svc wifi enable");
		}
	}

	private void disableWifi() {
		int i = 0;
		if (mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(false);
			Log.i(TAG, "Disabling Wifi");
			while (mWifiManager.isWifiEnabled() && i < WIFI_OPT_MAX_LOOP) {
				SystemClock.sleep(WIFI_OPT_MAX_SLEEP);
				i++;
			}
			if (WIFI_OPT_MAX_LOOP == i) {
				Log.w(TAG, "Disable Wifi failed");
			} else {
				Log.i(TAG, "Disable Wifi success: " + i);
			}
			//CmdAgent.doCommand("svc wifi disable");
		}
	}

	private void doScan() {
		Log.i(TAG, "doScan");
		mWifiManager.startScan();
	}
}
