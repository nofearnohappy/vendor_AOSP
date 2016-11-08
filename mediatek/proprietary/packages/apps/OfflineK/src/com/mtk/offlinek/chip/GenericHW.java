package com.mtk.offlinek.chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mtk.offlinek.CmdAgent;
import com.mtk.offlinek.component.DeviceType;

public abstract class GenericHW {
	List<Integer> portList;
	Map<DeviceType, Integer> deviceMap;
	List<String> voltageList;
	String mProjectName;
	
	public List<Integer> getPortList(){
		return portList;
	}
	public Map<DeviceType, Integer> getDeviceMap(){
		return deviceMap;
	}
	public List<String> getVoltageList(){
		return voltageList;
	}
	
	GenericHW(){
		mProjectName = CmdAgent.doCommand("getprop ro.build.product").trim();
		portList = new ArrayList<Integer>();
		deviceMap = new HashMap<DeviceType, Integer>();
		voltageList = new ArrayList<String>();
	}
	
	public abstract int setVoltage(String volText);
}
