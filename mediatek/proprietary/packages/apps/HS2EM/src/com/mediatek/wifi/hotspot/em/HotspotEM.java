package com.mediatek.wifi.hotspot.em;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class HotspotEM extends TabActivity {
    TabHost mTabHost;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Resources res = getResources(); // Resource object to get Drawables
        mTabHost = getTabHost();  // The activity TabHost
        //0
        mTabHost.addTab(mTabHost.newTabSpec("Add").setIndicator("Add")
                .setContent(new Intent().setClass(this, HSPT_AddCred.class)));
        // 1
        mTabHost.addTab(mTabHost.newTabSpec("Assoc").setIndicator("Assoc")
                .setContent(new Intent().setClass(this, HSPT_Associate.class)));
        // 2
        mTabHost.addTab(mTabHost.newTabSpec("Dump&Del").setIndicator("Del&Dump")
                .setContent(new Intent().setClass(this, HSPT_DelCred.class)));

        //mTabHost.setCurrentTab(2);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.wifi.hotspot.em.changetab");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("com.mediatek.wifi.hotspot.em.changetab")) {
                Bundle bundle = intent.getExtras();
                int tab = bundle.getInt("tab");
                mTabHost.setCurrentTab(tab);
            }
        }
    };

    //=================== basic utility ========================\\
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}
