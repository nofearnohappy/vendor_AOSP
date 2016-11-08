package com.mediatek.selfregister.utils;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;

public class AgentProxy {

    private static final String TAG = "AgentProxy";

    private static AgentProxy sInstance;
    private DmAgent mAgent;

    private AgentProxy(DmAgent agent) {
        mAgent = agent;
    }

    public static AgentProxy getInstance() {
        if (sInstance == null) {

            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder == null) {
                throw new Error("binder is null!");
            }
            DmAgent agent = DmAgent.Stub.asInterface(binder);

            if (agent == null) {
                throw new Error("DmAgent is null!");
            }
            sInstance = new AgentProxy(agent);
        }
        return sInstance;
    }

    //------------------------------------------------------
    //  Mac Address
    //------------------------------------------------------

    public boolean isSelfRegistered() {
        return getSeflRegisterFlag();
    }

    private boolean getSeflRegisterFlag() {
        int registerFlag = 0;

        try {
            byte[] readData = mAgent.readSelfRegisterFlag();
            if (readData != null && readData.length > 0) {
                registerFlag = Integer.parseInt(new String(readData));
            }

        } catch (RemoteException re) {
            Log.e(TAG, "Remote exception when read register flag!" + re);

        } catch (NumberFormatException nfe) {
            Log.w(TAG, "Register flag parse int failed!");
        }

        return registerFlag == 1;
    }

    public void setSelfRegisterFlag(boolean flag) {
        Log.d(TAG, "setSelfRegisterFlag " + flag);

        try {
            String value = flag ? "1" : "0";
            boolean result = mAgent.setSelfRegisterFlag(value.getBytes(), value.length());

            if (!result) {
                Log.e(TAG, "setRegisterFlag(), Set register flag fail!");
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
        }
    }

    //------------------------------------------------------
    //  Mac Address
    //------------------------------------------------------

    public byte[] getMacAddress() {
        byte[] macAddr = null;

        try {
            macAddr = mAgent.getMacAddr();

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return macAddr;
    }

    //------------------------------------------------------
    //  IccIds
    //------------------------------------------------------

    public String[] getSavedIccId(int slotNumber) {
        String[] result = new String[slotNumber];

        try {
            byte[] value = mAgent.readIccID1();
            if (value != null) {
                result[0] = new String(value);
            }

            if (slotNumber > 1) {
                value = mAgent.readIccID2();
                if (value != null) {
                    result[1] = new String(value);
                }
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
        }

        return result;
    }

    public Boolean setSavedIccId(String[] iccIDs ) {
        Boolean result = false;

        try {
            result = mAgent.writeIccID1(iccIDs[0].getBytes(), iccIDs[0].length());

            if (iccIDs.length > 1) {
                result = result & mAgent.writeIccID2(iccIDs[1].getBytes(), iccIDs[1].length());
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
            e.printStackTrace();
        }

        return result;
    }
}
