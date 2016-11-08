package com.mediatek.soundrecorder.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.soundrecorder.ext.DefaultQualityLevel;

/**
 * A class for customizing SoundRecorder's behavior in OP01 project.
 *
 * We define three quality levels and three sets of parameters for them.
 */
@PluginImpl(interfaceName = "com.mediatek.soundrecorder.ext.IQualityLevel")
public class Op01QualityLevel extends DefaultQualityLevel {
    private static final int QUALITY_LEVEL_NUM = 3;
    private static final int CONFIG_ITEM_NUM = 5;
    public Op01QualityLevel(Context ctx) {
        super(ctx);
    }

    @Override
    public int[][] getQualityLevelParams() {
        int[][] params = new int[QUALITY_LEVEL_NUM][CONFIG_ITEM_NUM];
        params[0] = this.getResources().getIntArray(R.array.operator_high_params);
        params[1] = this.getResources().getIntArray(R.array.operator_standard_params);
        params[2] = this.getResources().getIntArray(R.array.operator_low_params);
        return params.clone();
    }

    @Override
    public String[] getQualityLevelStrings() {
        String[] strs = new String[QUALITY_LEVEL_NUM];
        strs[0] = this.getResources().getString(R.string.recording_format_high);
        strs[1] = this.getResources().getString(R.string.recording_format_mid);
        strs[2] = this.getResources().getString(R.string.recording_format_low);
        return strs;
    }

    @Override
    public int getDefaultQualityLevel() {
        // default recording quality is low(the last quality level,index:2).
        return (QUALITY_LEVEL_NUM - 1);
    }
}
