package com.mtk.offlinek;

import com.mtk.offlinek.fragment.RecordItem;

public class NativeLoader {
	static NativeLoader thisPtr;
	static {
		System.loadLibrary("SdioETT");
	}
	public native void init();
	public native String getHString();	//For test only
	public native String doETT();
	/*public native void setPort(String port);
	public native void setModule(String module);
	public native void setVoltage(String voltage);
	public native void setFrequency(String frequency);
	public native void setCmdTimes(String cmdTimes);
	public native void setRDataTimes(String rDataTimes);
	public native void setWDataTimes(String wDataTimes);
	*/
	public native int setRecordItem(RecordItem item);
	public native void switchVoltage();
	public native int getProgress();
	public static NativeLoader getInstance(){
		if(thisPtr == null)
			thisPtr = new NativeLoader();
		return thisPtr;
	}
}
