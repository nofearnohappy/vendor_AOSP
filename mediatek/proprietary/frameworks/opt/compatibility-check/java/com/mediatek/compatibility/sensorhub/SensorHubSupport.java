package com.mediatek.compatibility.sensorhub;

import android.os.SystemProperties;

public class SensorHubSupport {
    public static boolean isSensorHubFeatureAvailable() {
        return 1 == SystemProperties.getInt("ro.mtk_sensorhub_support", 0);
    }
}
