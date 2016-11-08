package com.mtk.offlinek.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mtk.offlinek.FileHandler;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RecordItem {
	int mIndex;
	String mFrequency;
	String mPort;
	String mDevice;
	String mCmdTimes;
	String mRDataTimes;
	String mWDataTimes;
	public String mVoltage;
	public int mProgress;
	public boolean isDone = false;
	public boolean isETTing = false;
	public String mLogPath;
	static class ViewHolder{ 
        ImageView iv = null ;  
        Button mBtn = null ; 
        ProgressBar pb = null ;  
        TextView tv = null ;  
    } 
	ViewHolder mViewHolder;
	public static List<RecordItem> recordList;
	public final static String ETT_KEY = "ETT TUNING RESULT"; 
	public static String orderSample = "Freq/Port/Dev/CmdT/RDatT/WDatT/Vol";
	RecordItem(String freq, String port, String device, 
			String cmdTimes, String rDataTimes, String wDataTimes, String voltage){
		this.mFrequency = freq;
		this.mPort = port;
		this.mDevice = device;
		this.mCmdTimes = cmdTimes;
		this.mRDataTimes = rDataTimes;
		this.mWDataTimes = wDataTimes;
		this.mVoltage = voltage;
		this.mProgress = 0;
	}
	
	public static void addRecord(RecordItem item){
		if(recordList == null){
			 recordList = new ArrayList<RecordItem>();
			 item.mIndex = 0;
		} else {
			for(int i=0; i<getCount(); i++){
				RecordItem curItem = recordList.get(i);
				curItem.mIndex = i;
				if(curItem.equals(item))
					return;
			}
			item.mIndex = getCount();
			//item.mIndex = recordList.get(getCount()-1).mIndex+1;
		}
		recordList.add(item);
		
	}
	
	public static void delRecord(int index){
		RecordItem item = recordList.get(index);
		FileHandler.deleteFile(item.mLogPath);
		item = null;
		recordList.remove(index);
	}
	
	public String pack(){	// Use to display to enduser
		String [] fieldOrder = {"mFrequency", "mPort", "mDevice", "mCmdTimes", "mRDataTimes", "mWDataTimes", "mVoltage"};
		StringBuilder build = new StringBuilder();
		String separator = "/";
		Class<?> cls = this.getClass();
		String data;
		//Field[] field = .getDeclaredFields();
		
		for(int i=0; i<fieldOrder.length; i++){
			try {
				/*if(java.lang.reflect.Modifier.isStatic(field[i].getModifiers()))
					continue;
				*/
				data = (String) cls.getDeclaredField(fieldOrder[i]).get(this);
				build.append(data);
				build.append(separator);
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return build.toString();
	}
	
	public String genPath(Context context){
		return context.getExternalFilesDir(null).getAbsolutePath()+File.separator+this.pack(1).replace('.', '_')+".txt";
	}
	
	public String pack(int dummy){	// Use to get file name
		String [] fieldOrder = {"mFrequency", "mPort", "mDevice", "mCmdTimes", "mRDataTimes", "mWDataTimes", "mVoltage"};
		StringBuilder build = new StringBuilder();
		String separator = "_";
		Class<?> cls = this.getClass();
		String data;
		//Field[] field = .getDeclaredFields();
		
		for(int i=0; i<fieldOrder.length; i++){
			try {
				/*if(java.lang.reflect.Modifier.isStatic(field[i].getModifiers()))
					continue;
				*/
				data = (String) cls.getDeclaredField(fieldOrder[i]).get(this);
				build.append(data);
				build.append(separator);
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return build.toString();
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof RecordItem))return false;
	    RecordItem otherMyClass = (RecordItem)other;
		return otherMyClass.pack().equals(this.pack());
	}
	
	public boolean equals(String other){
	    return this.pack().equals(other);
	}
	
	public static int getCount(){
		if(recordList == null)
			return 0;
		return recordList.size();
	}	
	
	public static RecordItem getObjFromPath(String path){
		// Sample: 200_2_LTE_1_1_1_1_125_.txt
		RecordItem item = null;
		String freqText, portText, deviceText, cTimesText, rDTimesText, wDTimesText, vText1, vText2;
		
		String patternStr = "(\\d+)_(\\d)_(\\w+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)_.txt";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(path);
		if (matcher.find()) {
			freqText = matcher.group(1);
			portText = matcher.group(2);
			deviceText = matcher.group(3);
			cTimesText = matcher.group(4);
			rDTimesText = matcher.group(5);
			wDTimesText = matcher.group(6);
			vText1 = matcher.group(7);
			vText2 = matcher.group(8);
			item = new RecordItem(freqText, portText
					, deviceText, cTimesText, 
					rDTimesText, wDTimesText, 
					vText1+"."+vText2);
		}
		return item;
	}
	
	public boolean isDone(){
		if(isDone && mLogPath != null && new File(mLogPath).exists())
			return FileHandler.isKeywordInFile(mLogPath, RecordItem.ETT_KEY);
		return isDone;
	}
	
	public void setDone(Handler hd, boolean isDone){
		Message olMsg = hd.obtainMessage(RecordFragment.MSG_RESULT_AVAILABLE);
		olMsg.obj = this;
		olMsg.arg2 = mIndex;
		this.isDone = isDone;
		hd.sendMessage(olMsg);
	}
}
