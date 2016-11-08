package com.mtk.offlinek;

import android.content.Context;

public abstract class DeviceIF {
	boolean isDeviceReady = false;
	protected int host_id = 2;
	protected Context mContext = null;
	public void setContext(Context context){
		this.mContext = context;
	}
	public abstract void deviceOn();
	public abstract void deviceOff();
}
