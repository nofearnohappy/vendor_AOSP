package com.mediatek.deviceregister.utils;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.deviceregister.Const;

public class AgentProxy {

    private static final String TAG = Const.TAG_PREFIX + "AgentProxy";

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
    // Function related to register flag
    //------------------------------------------------------

    public void resetRegisterFlag() {
        setRegisterFlag(false);
    }

    public boolean isRegistered() {
        return getRegisterFlag();
    }

    public boolean setRegisterFlag(boolean flag) {
        Log.i(TAG, "setRegisterFlag " + flag);

        String value = flag ? "1" : "0";
        boolean result = false;

        try {
            byte[] bytes = value.getBytes();
            result = mAgent.setRegisterFlag(bytes, bytes.length);

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
            e.printStackTrace();
        }

        return result;
    }

    private boolean getRegisterFlag() {
        int result = 0;

        try {
            byte[] value = mAgent.readRegisterFlag();
            if (value != null && value.length > 0) {
                result = Integer.parseInt(new String(value));
            }

        } catch (RemoteException e) {
            Log.e(TAG, "remote exception " + e);
            e.printStackTrace();

        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed " + e);
            e.printStackTrace();
        }

        return result == 1;
    }

    //------------------------------------------------------
    // Check state
    //------------------------------------------------------

    public boolean isFeatureEnabled() {
        int result = 1;

        try {
            byte[] value = mAgent.getRegisterSwitch();

            if (value != null && value.length > 0) {
                result = Integer.parseInt(new String(value));
                Log.i(TAG, "Get the switch value = " + result);
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
            e.printStackTrace();

        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException:" + e);
            e.printStackTrace();
        }

        Log.i(TAG, "Feature is enabled? " + ((result == 1)));
        return (result == 1);
    }

    //------------------------------------------------------
    //Imsi
    //------------------------------------------------------

    public String[] getSavedImsi(int slotNumber) {
        String[] imsiArray = new String[slotNumber];

        try {
            byte[] imsi1 = mAgent.readImsi1();
            if (imsi1 != null) {
                imsiArray[0] = new String(imsi1);
            }

            if (slotNumber > 1) {
                byte[] imsi2 = mAgent.readImsi2();
                if (imsi2 != null) {
                    imsiArray[1] = new String(imsi2);
                }
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
        }

        return imsiArray;
    }

    public Boolean setSavedImsi(String[] imsi) {
        Boolean result = false;

        try {
            byte[] bytes = imsi[0].getBytes();
            result = mAgent.writeImsi1(bytes, bytes.length);

            if (imsi.length > 1) {
                bytes = imsi[1].getBytes();
                result = result & mAgent.writeImsi2(bytes, bytes.length);
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException " + e);
            e.printStackTrace();
        }

        return result;
    }
}
