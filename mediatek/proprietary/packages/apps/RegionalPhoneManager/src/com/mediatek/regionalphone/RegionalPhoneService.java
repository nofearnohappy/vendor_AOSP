package com.mediatek.regionalphone;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.common.MPlugin;
import com.mediatek.rpm.ext.DefaultRegionalPhoneAddMmsApnExt;
import com.mediatek.rpm.ext.DefaultSettingsExt;
import com.mediatek.rpm.ext.IRegionalPhoneAddMmsApnExt;
import com.mediatek.rpm.ext.ISettingsExt;

public class RegionalPhoneService extends Service {

    public static final String ACTION = "com.mediatek.regionalphone.regionalphoneservice";

    private static final String TAG = Common.LOG_TAG;
    private ContentResolver mContentResolver;
    private Context mContext;
    private Thread mRPMThread = null;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("@M_" + TAG, "RegionalPhoneService::onCreate");
        mContentResolver = this.getContentResolver();
        mContentResolver.registerContentObserver(RegionalPhone.MMS_SMS_URI,
                true, mMmsObserver);
        mContentResolver.registerContentObserver(RegionalPhone.SETTINGS_URI,
                true, mSettingsObserver);
        mContext = this.getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("@M_" + TAG, "RegionalPhoneService::onStartCommand");
        boolean isSIMLoaded = (intent != null && intent.getBooleanExtra(Common.SIM_LOADED, true));
        Log.d("@M_" + TAG, "RegionalPhoneService::onStartCommand, is sim loaded: " + isSIMLoaded);
        if (mRPMThread == null || isSIMLoaded) {
            mRPMThread = new Thread(new RegionalPhoneRunnable(mContext));
            mRPMThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContentResolver.unregisterContentObserver(mMmsObserver);
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    private ContentObserver mMmsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            IRegionalPhoneAddMmsApnExt mIRegionalPhoneAddMmsApn = MPlugin.createInstance(
                IRegionalPhoneAddMmsApnExt.class.getName(), mContext);
            if (mIRegionalPhoneAddMmsApn == null) {
                mIRegionalPhoneAddMmsApn = new DefaultRegionalPhoneAddMmsApnExt();
            }
            mIRegionalPhoneAddMmsApn.addMmsApn(mContext);
        }
    };

    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            ISettingsExt mISettingsExt = MPlugin.createInstance(
                ISettingsExt.class.getName(), mContext);
            if (mISettingsExt == null) {
                mISettingsExt = new DefaultSettingsExt();
            }
            mISettingsExt.updateConfiguration(mContext);
        }
    };

}
