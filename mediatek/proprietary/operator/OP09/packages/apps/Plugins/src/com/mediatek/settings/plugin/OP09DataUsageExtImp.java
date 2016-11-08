package com.mediatek.settings.plugin;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

import com.android.internal.telephony.ITelephony;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultDataUsageSummaryExt;
//import com.android.widget.Switch;


import java.util.List;
import java.util.Map;

/**
 * For settings SIM management feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IDataUsageSummaryExt")
public class OP09DataUsageExtImp extends DefaultDataUsageSummaryExt {
    private static final String TAG = "OP09DataUsageExt";

    private Context mContext;
    private int mToCloseSlot = -1;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;
    private OnClickListener mDialogListerner;
    private Map<String, Boolean> mMobileDataEnabled;
    Switch  mDataswitch;
    View    mDataView;
    int     mTabPage;
    /**
     * update the preference screen of sim management.
     * @param context The context
     */
    public OP09DataUsageExtImp(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
        Switch dataEnabled, OnClickListener dataEnabledDialogListerner) {
        mDialogListerner = dataEnabledDialogListerner;
        mDataswitch = dataEnabled;
        mDataView = dataEnabledView;
        return true;
    }

    @Override
    public void setCurrentTab(int tab) {
        mTabPage = tab;
        Log.d(TAG, "getCurrentTab: tabPage=" + mTabPage);
    }

    private boolean isMobileDataEnabled(int subId) {
        boolean isEnable = false;
        if (mMobileDataEnabled.get(String.valueOf(subId)) != null) {
            isEnable = mMobileDataEnabled.get(String.valueOf(subId)).booleanValue();
            Log.d(TAG, "isMobileDataEnabled: != null, subId=" + subId
                    + " isEnable=" + isEnable);
        }
        return isEnable;
    }

    private ContentObserver mDataConnectionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange selfChange=" + selfChange);
            if (!selfChange) {
                TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
                List<SubscriptionInfo> si =  SubscriptionManager
                    .from(mContext).getActiveSubscriptionInfoList();
                if (si != null && si.size() > 0) {
                    for (int i = 0; i < si.size(); i++) {
                        SubscriptionInfo subInfo = si.get(i);
                        int subId = subInfo.getSubscriptionId();
                        Log.i(TAG, "onChanged, updateMap key subId = " + subId +
                            " value:" + mTelephonyManager.getDataEnabled(subId));
                        mMobileDataEnabled.put(String.valueOf(subId),
                            mTelephonyManager.getDataEnabled(subId));
                    }
                }
                if (mDataswitch != null) {
                    mDataswitch.setChecked(isMobileDataEnabled(mTabPage));
                }
                if (mDataView != null) {
                    mDataView.invalidate();
                }
            }
        }
    };


    @Override
    public void create(Map<String, Boolean> mobileDataEnabled) {
        Log.i(TAG, "OP09DataUsageExtImp resume go");
        int subId = 0;
        mMobileDataEnabled = mobileDataEnabled;
        List<SubscriptionInfo> si = SubscriptionManager.from(mContext)
            .getActiveSubscriptionInfoList();

        if (si != null && si.size() > 0) {
            for (int i = 0; i < si.size(); i++) {
                SubscriptionInfo subInfo = si.get(i);
                subId = subInfo.getSubscriptionId();
                mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId),
                    true, mDataConnectionObserver);
            }
        }
    }

    @Override
    public void destroy() {
        mContext.getContentResolver().unregisterContentObserver(mDataConnectionObserver);
        mDataswitch = null;
        mDataView = null;
    }

}
