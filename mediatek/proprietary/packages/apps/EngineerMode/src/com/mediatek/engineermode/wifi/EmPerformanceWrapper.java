package com.mediatek.engineermode.wifi;

import android.content.Context;
import android.content.SharedPreferences;

import com.mediatek.engineermode.Elog;
import com.mediatek.perfservice.IPerfServiceWrapper;
import com.mediatek.perfservice.PerfServiceWrapper;

/**
 * The helper class to use PerformanceService.
 * @author mtk81238
 *
 */
public class EmPerformanceWrapper {
    public static final int FREQ_1_3G = 1300000;
    private static final String KEY_EM_PERF_PREFER = "EM_PERF_PREFER";
    private static final String FIELD_EM_PERF_ENABLED = "EM_PERF_ENABLED";
    private static final String FIELD_EM_PERF_HANDLE_ID = "EM_PERF_HANDLE_ID";
    private int mHandle = -1;
    private IPerfServiceWrapper mPerfService = null;
    private boolean mEnabled = false;
    private Context mContext = null;
    private static final String TAG = "EM/WIFI_PERFORMANCE";

    private static EmPerformanceWrapper sPerfService = null;
    /**
     * get instance of EmPerformanceWrapper.
     * @param context system Context
     * @param boot whether is boot procedure
     * @return instance of EmPerformanceWrapper
     */
    public static EmPerformanceWrapper initialize(Context context, boolean boot) {
        if (sPerfService != null) {
            return sPerfService;
        }
        int coreNum = 2;
        int freq = FREQ_1_3G;
        try {
            sPerfService = new EmPerformanceWrapper(context, coreNum, freq, boot);
        } catch (UnsupportedOperationException e) {
            Elog.d(TAG, "UnsupportedOperationException:" + e.getMessage());
            sPerfService = null;
        }
        return sPerfService;
    }

    /**
     * Constructor.
     * @param context system context
     * @param coreNum to open CPU
     * @param freq CPU run at
     * @param boot whether in boot procedure
     */
    private EmPerformanceWrapper(Context context, int coreNum, int freq, boolean boot) {
        mContext = context;
        mPerfService = new PerfServiceWrapper(null);
        if (boot) {
            mHandle = mPerfService.userReg(coreNum, freq);
            if (mHandle == -1) {
                throw new UnsupportedOperationException("fail to register performance scenario");
            }
            boolean enabled = isPerfSettingEnabled(context);
            if (enabled) {
                forceEnable();
            }
            storePerformanceState(mHandle, enabled);
        } else {
            restorePerformanceState();
            if (mHandle == -1) {
                mEnabled = false;
                mHandle = mPerfService.userReg(coreNum, freq);
                if (mHandle == -1) {
                    throw new UnsupportedOperationException(
                            "fail to register performance scenario");
                }
                storePerformanceState(mHandle, mEnabled);
            }
        }
    }

    /**
     * tell whether performance setting is enabled.
     * @param context system context
     * @return true if the setting is enabled
     */
    public static final boolean isPerfSettingEnabled(Context context) {
        SharedPreferences pref = context.getSharedPreferences(KEY_EM_PERF_PREFER,
                Context.MODE_PRIVATE);
        return pref.getBoolean(FIELD_EM_PERF_ENABLED, false);
    }

    private void storePerformanceState(int handle, boolean enabled) {
        SharedPreferences pref = mContext.getSharedPreferences(KEY_EM_PERF_PREFER,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(FIELD_EM_PERF_HANDLE_ID, handle);
        editor.putBoolean(FIELD_EM_PERF_ENABLED, enabled);
        editor.commit();
    }

    private void restorePerformanceState() {
        SharedPreferences pref = mContext.getSharedPreferences(KEY_EM_PERF_PREFER,
                Context.MODE_PRIVATE);
        mHandle = pref.getInt(FIELD_EM_PERF_HANDLE_ID, -1);
        mEnabled = pref.getBoolean(FIELD_EM_PERF_ENABLED, false);
    }

    /**
     * whether enable the performance scenario.
     * @return true if enabled
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * enable the performance scenario.
     */
    public void enable() {
        if (!mEnabled) {
            Elog.d(TAG, "wifi performance enable");
            forceEnable();
            storePerformanceState(mHandle, mEnabled);
        }
    }

    private void forceEnable() {
        mPerfService.userEnable(mHandle);
        mEnabled = true;
    }

    /**
     * disable the performance scenario.
     */
    public void disable() {
        if (mEnabled) {
            Elog.d(TAG, "wifi performance disable");
            mPerfService.userDisable(mHandle);
            mEnabled = false;
            storePerformanceState(mHandle, mEnabled);
        }
    }

    /**
     * destroy Performance service.
     */
    public void destroy() {
        disable();
        mPerfService.userUnreg(mHandle);
        storePerformanceState(-1, false);
    }
}
