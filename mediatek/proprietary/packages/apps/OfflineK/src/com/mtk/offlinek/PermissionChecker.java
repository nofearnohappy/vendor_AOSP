package com.mtk.offlinek;

import java.io.File;

import android.content.Context;
import android.os.Handler;

public class PermissionChecker extends Thread {
	
	public static String ettLogPath = null;
	public static final String msdcProcPath = "/proc/msdc_tune";
	public static final String msdcDebugPath = "/proc/msdc_debug";
	public static final String msdcTestPath = "/sys/mtk_sdio/test";
	public static boolean isEttLogPass = false;
	public static boolean isMsdcProcPass = false;
	public static boolean isMsdcDebugPass = false;
	static boolean isGranted = false;
	public static String outputText = "NODATA";
	public int host_id = 3;
	Context mContext;
	Handler uiHandler;
	public PermissionChecker(Context context, Handler handler){
		//this.isGranted = false;
		this.mContext = context;
		uiHandler = handler;
	}
	
	@Override
	public void run() {
		ettLogPath = mContext.getExternalFilesDir(null)+"sdio_ETT_result.txt";
		File ettLogFile = new File(mContext.getExternalFilesDir(null).getAbsolutePath());
		if(ettLogFile.exists() && ettLogFile.canWrite()){
			ettLogFile.setReadable(true);
			ettLogFile.setWritable(true);
			isEttLogPass = true;
		}
		ettLogFile = null;
		File msdcProcFile = new File(msdcProcPath);
		if(msdcProcFile.exists() && msdcProcFile.canWrite()){
			msdcProcFile.setReadable(true);
			msdcProcFile.setWritable(true);
			isMsdcProcPass = true;
			File msdcTestFile = new File(msdcTestPath);		
			if(!msdcTestFile.exists()){
				// Put ettagent.ko into storage
				/*FileHandler file = FileHandler.getInstance(mContext);
				String ettName = getEttAgentName();
				String destName = null;
				if((destName = file.copyAssets(ettName)) != null){
					sendPopDialog("insmod "+destName);
				}*/
				
			}
		} else {
			msdcProcFile = null;
			return;
		}
			
		File msdcDebugFile = new File(msdcDebugPath);
		if(msdcDebugFile.exists() && msdcDebugFile.canWrite()){
			msdcDebugFile.setReadable(true);
			msdcDebugFile.setWritable(true);
			isMsdcDebugPass = true;
		} 		
		
		msdcProcFile = null;
		msdcDebugFile = null;
		isGranted = isEttLogPass & isMsdcProcPass & isMsdcDebugPass;
	}
	
	/*private String getEttAgentName(){
		String chipID = CmdAgent.doCommand("getprop ro.hardware").trim();
		String mode = CmdAgent.doCommand("getprop ro.build.type").trim();
		StringBuilder ettAgentName = new StringBuilder();
		ettAgentName.append(chipID.toLowerCase());
		ettAgentName.append("_");
		ettAgentName.append(mode.toLowerCase());
		ettAgentName.append("_ettagent.ko");
		return ettAgentName.toString();
	}
	
	private void sendPopDialog(String text) {
		Message olMsg = uiHandler.obtainMessage(RecordFragment.MSG_POP_DIALOG);
		Bundle bundle = new Bundle();
		bundle.putString("TEXT", text);
		olMsg.setData(bundle);
		uiHandler.sendMessage(olMsg);
		bundle = null;
	}*/
}
