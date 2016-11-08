package com.mediatek.op.media;

import com.mediatek.common.PluginImpl;
import android.util.Log;

/**
 * Implementation of plugin for IOmaSettingHelper.
 */
@PluginImpl(interfaceName = "com.mediatek.common.media.IOmaSettingHelper")
public class OmaSettingHelperOP01 extends DefaultOmaSettingHelper {
    private static final String TAG = "OmaSettingHelperOP01";
    private static final boolean LOG = true;
    private static final int DEFAULT_RTSP_BUFFER_SIZE = 6; //seconds

    /**
     * Whether oma is supported or not.
     * @return true enabled, otherwise false.
     */
    protected boolean isOMAEnabled() {
        if (LOG) {
            Log.v("@M_" + TAG, "isOMAEnabled: enabled=true.");
        }
        return true;
    }

    /**
     * Gets RTSP default buffer size.
     * @return RTSP default buffer size.
     */
    protected int getRtspDefaultBufferSize() {
        return DEFAULT_RTSP_BUFFER_SIZE;
    }
}
