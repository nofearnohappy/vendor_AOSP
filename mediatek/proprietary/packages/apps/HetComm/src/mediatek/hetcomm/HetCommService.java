package com.mediatek.hetcomm;

import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;
import android.util.Log;

public class HetCommService extends Service {
    static protected final String TAG = "HetCommService";

    private Handler mHandler;
    private Context mContext;
    private HetCommStateMachine mHsm;

    private ConnectivityManager mConnMgr;
    private ConnectivityManager.NetworkCallback mNetworkCallback =
    new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d(TAG, "NetworkCallbackListener.onAvailable");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d(TAG, "NetworkCallbackListener.onLost");
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.d(TAG, "NetworkCallbackListener.onUnavailable");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");
        mContext = this.getBaseContext();

        mConnMgr = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        HandlerThread handlerThread = new HandlerThread("HetCommServiceThread");
        handlerThread.start();

        mHandler = new HetCommSmHandler(handlerThread.getLooper());
        mHsm = new HetCommStateMachine(mHandler, mContext, mConnMgr);

        //This is a debug purpose.
        //mHsm.setDbg(true);
        mHsm.start();
        mHsm.regiterCallback();

        acquireNetwork();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "The service is ended");
        mHsm.stop();
        mHsm = null;
        releaseNetwork();
        //Toast.makeText(this, "service is stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * Save the HetComm enable setting into global settings.
     *
     */
    private void updateHetCommSetting(boolean isEnabled) {
        Log.i(TAG, "updateHetCommSetting enable = " + isEnabled);
        int value  = (isEnabled) ? 1 : 0;

        //Stop HetComm service when HetComm setting is disabled.
        if (!isEnabled) {
            stopSelf();
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, value);
    }

    private void acquireNetwork() {
        NetworkRequest request = new NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
        .build();
        mConnMgr.requestNetwork(request, mNetworkCallback);
    }

    private void releaseNetwork() {
        mConnMgr.unregisterNetworkCallback(mNetworkCallback);
    }

    private class HetCommSmHandler extends Handler {
        public HetCommSmHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "processMessage what=" + msg.what);
            final Resources r = mContext.getResources();

            switch (msg.what) {
            case HetCommStateMachine.EVENT_VPN_ON:
                updateHetCommSetting(false);
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_vpn),
                            Toast.LENGTH_SHORT).show();
                break;
            case HetCommStateMachine.EVENT_TETHERING_ON:
                updateHetCommSetting(false);
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_tether),
                            Toast.LENGTH_SHORT).show();
                break;
            case HetCommStateMachine.EVENT_ROAMING_ON:
                updateHetCommSetting(false);
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_roaming),
                            Toast.LENGTH_SHORT).show();
                break;
            case HetCommStateMachine.EVENT_USER_SWITCH:
                Log.d(TAG, "Stop HetComm service");
                updateHetCommSetting(false);
                return;
            }
            Intent intent = new Intent("com.android.settings.HETCOMM_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
