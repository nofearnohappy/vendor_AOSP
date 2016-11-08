package com.mtk.offlinek;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.mtk.offlinek.component.DeviceType;
import com.mtk.offlinek.component.LteRunner;
import com.mtk.offlinek.component.WifiRunner;
import com.mtk.offlinek.fragment.RecordFragment;
import com.mtk.offlinek.fragment.RecordItem;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ETTRunner extends Thread {
	Thread olThread;
	Handler uiHandler;
	Handler progressHD;
	private Context mContext;
	DeviceIF device;
	DeviceType curType = DeviceType.LTE;
	public static final String msdcNodePath = "/proc/msdc_tune";
	public int host_id = 3;
	boolean isETTing = false;
	int curRecordIndex;
	public ETTRunner(Context context, Handler uiHandler, Handler progressHD, int i) {
		this.uiHandler = uiHandler;
		this.mContext = context;
		this.progressHD = progressHD;
		curRecordIndex = i;
	}

	public void setDevice(DeviceType src) {
		this.curType = src;
	}
	
	public void setPort(int port) {
		this.host_id = port;
	}
	
	private void createDeviceIF() {
		if (curType == DeviceType.WIFI) {
			device = new WifiRunner(host_id);
		} else {
			device = new LteRunner(host_id);
		}
		device.setContext(mContext);
	}
	
	public boolean checkPermission(){
		PermissionChecker pChecker = new PermissionChecker(mContext, uiHandler);
		pChecker.setDaemon(true);
		pChecker.start();
		try {
			pChecker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!PermissionChecker.isGranted){
			String text = "No Permission";
			sendUIText(text);
			if(!PermissionChecker.isEttLogPass)
				text = "ETT Log Path Access Fail";
			else if (!PermissionChecker.isMsdcProcPass)
				text = "/proc/msdc_tune permission check fail";
			else if (!PermissionChecker.isMsdcDebugPass)
				text = "/proc/msdc_debug permission check fail";
			
			sendPopDialog(text);
			return false;
		}
		return true;
	}
	
	@Override
	public void run() {
		Double doubleVoltage;		
		//sendToast(pChecker.outputText);		
		// sendUIText(Boolean.toString(pChecker.isMsdcDebugPass));
		if (PermissionChecker.isGranted) {
			String juju = "Permission Granted";
			RecordItem curRecord = RecordItem.recordList.get(curRecordIndex);
			curRecord.mLogPath = curRecord.genPath(mContext);
			if(curRecord.isDone())
				return;
			sendUIText(juju);
			createDeviceIF();
			device.deviceOn();
			enableTestNode();
			
			NativeLoader.getInstance().setRecordItem(curRecord);
			isETTing = true;
			Thread progThread = new Thread(new Runnable(){
				@Override
				public void run(){
					int prog = 8;
					setProgress(prog);
					while(isETTing && prog<100){
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						prog = NativeLoader.getInstance().getProgress();
						setProgress(prog);
					}
					setProgress(100);
				}
			});
			progThread.setDaemon(true);
			progThread.start();
			MainActivity.hwConfig.getVoltageList();
			doubleVoltage = Double.parseDouble(curRecord.mVoltage);
			MainActivity.hwConfig.setVoltage(String.valueOf((int)(doubleVoltage * 1000000)));
			juju = NativeLoader.getInstance().doETT();
			//curRecord.isDone = true;
			curRecord.setDone(progressHD, true);
			isETTing = false;
			disableTestNode();
			sendUIText(juju);
			device.deviceOff();
		} 
		
	}
	
	private void setProgress(int prog){
		Message olMsg = progressHD.obtainMessage(RecordFragment.MSG_SHOW_PROGRESS, prog, curRecordIndex);
		progressHD.sendMessage(olMsg);
		olMsg = null;
	}
	
	private void sendUIText(String text) {
		Message olMsg = uiHandler.obtainMessage(RecordFragment.MSG_SHOW_TEXT);
		Bundle bundle = new Bundle();
		bundle.putString("TEXT", text);
		olMsg.setData(bundle);
		uiHandler.sendMessage(olMsg);
		bundle = null;
	}
	
	private void sendPopDialog(String text) {
		Message olMsg = uiHandler.obtainMessage(RecordFragment.MSG_POP_DIALOG);
		Bundle bundle = new Bundle();
		bundle.putString("TEXT", text);
		olMsg.setData(bundle);
		uiHandler.sendMessage(olMsg);
		bundle = null;
	}
	
	/*private void sendToast(String text) {
		Message olMsg = uiHandler.obtainMessage(RecordFragment.MSG_DUMP_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString("TEXT", text);
		olMsg.setData(bundle);
		uiHandler.sendMessage(olMsg);
		bundle = null;
	}	*/
	
	public void enableTestNode() {
		File msdcProcFile = new File(msdcNodePath);
		FileWriter fr;  
        try {  
        	fr = new FileWriter(msdcProcFile);  
            fr.write("0 "+host_id+" 1");   
            fr.close();  
        }  
        catch (IOException e) {  
        	e.printStackTrace();  
        } finally{
        	fr = null;
        }
	}

	public void disableTestNode() {
		File msdcProcFile = new File(msdcNodePath);
		FileWriter fr;  
        try {  
        	fr = new FileWriter(msdcProcFile);  
            fr.write("0 "+host_id+" 0");   
            fr.close();  
        }  
        catch (IOException e) {  
        	e.printStackTrace();  
        } finally{
        	fr = null;
        }
	}
}

