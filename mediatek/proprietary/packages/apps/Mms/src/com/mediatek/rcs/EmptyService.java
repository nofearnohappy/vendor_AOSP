package com.mediatek.rcs;

import com.mediatek.ipmsg.util.IpMessageUtils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.mediatek.mms.ipmessage.IIpEmptyServiceExt;

public class EmptyService extends Service {
    
    IIpEmptyServiceExt mIpPlugin;

    @Override
    public void onCreate() {
        super.onCreate();
        initPlugin();
        if (mIpPlugin != null) {
            mIpPlugin.onCreate(this);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIpPlugin != null) {
            return mIpPlugin.onStartCommand(intent, flags, startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIpPlugin != null) {
            mIpPlugin.onDestroy();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        if (mIpPlugin != null) {
            return mIpPlugin.onBind(intent);
        }
        return null;
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mIpPlugin != null) {
            mIpPlugin.onLowMemory();
        }
    }
    
    @Override 
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        if (mIpPlugin != null) {
            mIpPlugin.onRebind(intent);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mIpPlugin != null) {
            return mIpPlugin.onUnbind(intent);
        }
        return super.onUnbind(intent);
    }
    
    private void initPlugin() {
        mIpPlugin = IpMessageUtils.getIpMessagePlugin(this).getIPEmptyService();
    }

}
