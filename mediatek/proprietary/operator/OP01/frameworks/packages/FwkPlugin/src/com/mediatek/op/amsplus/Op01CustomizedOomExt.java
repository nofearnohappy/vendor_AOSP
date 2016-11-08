package com.mediatek.op.amsplus;

import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.util.MemInfoReader;
import com.mediatek.common.PluginImpl;
import com.mediatek.common.amsplus.ICustomizedOomExt;


@PluginImpl(interfaceName="com.mediatek.common.amsplus.ICustomizedOomExt")
public class Op01CustomizedOomExt extends DefaultCustomizedOomExt {
    private static final String TAG = "Op01CustomizedOomExt";
    private static final String OOMPOLICY_SUPPORT = "1";
    private static final String PROCESS_A = "com.sinelife.theone";
    private static final String PROCESS_B = "com.nexon.kartriderrushplus.qihowj";
    private static final long MAX_ALLOW_MEM = 1024*1024*1024;
    private static final int APP_ADJ_HIGH = 6;
    private static final int APP_ADJ_LOW = 5;
    private static final int UNKNOWN_ADJ = 16;

    private boolean mOpenConfig;
    private int mCurAdj;

    public Op01CustomizedOomExt() {
        super();
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long totalMemMb = minfo.getTotalSize();
        Log.d(TAG, "totalMemMb: " + totalMemMb);

        boolean config = OOMPOLICY_SUPPORT.equals(
                SystemProperties.get("ro.mediatek.cmcc.oompolicy")) ? true : false;
        Log.d(TAG, "config: " + config);

        mCurAdj = APP_ADJ_HIGH;
        if (totalMemMb <= MAX_ALLOW_MEM && config) {
            mOpenConfig = true;
            if (totalMemMb <= MAX_ALLOW_MEM/2) {
                mCurAdj = APP_ADJ_LOW;
            }
        } else {
            mOpenConfig = false;
        }
        Log.d(TAG, "mOpenConfig: " + mOpenConfig);
    }

    @Override
    public int getCustomizedAdj(String processName) {
        if (mOpenConfig
            && (PROCESS_A.equals(processName) || PROCESS_B.equals(processName))) {
            return mCurAdj;
        } else {
            return UNKNOWN_ADJ;
        }
    }
}
