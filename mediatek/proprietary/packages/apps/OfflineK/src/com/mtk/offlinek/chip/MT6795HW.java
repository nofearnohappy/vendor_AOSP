package com.mtk.offlinek.chip;

import com.mtk.offlinek.FileHandler;
import com.mtk.offlinek.component.DeviceType;

public class MT6795HW extends GenericHW{
	
	public MT6795HW(){
		super();
		portList.add(3);
		deviceMap.put(DeviceType.WIFI, 3);
		
		voltageList.add("1068750");
		voltageList.add("1125000");
		voltageList.add("1181250");
	}

	@Override
	public int setVoltage(String volText) {
		String cmd;
		String devNode;
		long voltage = Long.parseLong(volText);
		int volIndex = 0;
		volIndex = (int) ((voltage-700000)/6250);
		// For VCORE_AO
		devNode = "/sys/devices/platform/mt-pmic/pmic_access";
		cmd = "36A "+ Integer.toHexString(volIndex);
		FileHandler.setDevNode(devNode, cmd);
		cmd = "36C "+ Integer.toHexString(volIndex);
		FileHandler.setDevNode(devNode, cmd);
		
		// For VCORE_PND
		cmd = "24C "+ Integer.toHexString(volIndex);
		FileHandler.setDevNode(devNode, cmd);
		cmd = "24E "+ Integer.toHexString(volIndex);
		FileHandler.setDevNode(devNode, cmd);
		
		return 0;
	}

	
}
