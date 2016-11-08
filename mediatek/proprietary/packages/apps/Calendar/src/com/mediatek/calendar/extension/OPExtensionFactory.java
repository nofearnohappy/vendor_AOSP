package com.mediatek.calendar.extension;

import android.content.Context;

import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.ext.DefaultLunarExtension;
import com.mediatek.calendar.ext.DefaultEditEventViewExt;
import com.mediatek.calendar.ext.ILunarExt;
import com.mediatek.calendar.ext.IEditEventViewExt;
import com.mediatek.common.MPlugin;

/**
 * M: this class is a factory to produce the operator plug-in object.
 */
public class OPExtensionFactory {

    private static final String TAG = "OPExtensionFactory";

    private static ILunarExt sLunarExtension;
    private static IEditEventViewExt sEditEventViewExt;
    /**
     * The Lunar Extension is an Single instance. It would hold the ApplicationContext, and
     * alive within the whole Application.
     * @param context MPlugin use it to retrieve the plug-in object
     * @return the single instance of Lunar Extension
     */
    public static ILunarExt getLunarExtension(Context context) {
        if (sLunarExtension == null) {
            sLunarExtension = (ILunarExt) MPlugin.createInstance(
                    ILunarExt.class.getName(), context.getApplicationContext());
            if (sLunarExtension == null) {
                sLunarExtension = new DefaultLunarExtension();
                LogUtil.i(TAG, "get lunar plugin failed, use default");
            }
        }
        return sLunarExtension;
    }

    /**
     * Get Plugin instance of EditEventViewExtension
     * @param context MPlugin use it to retrieve the plug-in object
     * @return the single instance of EditEventView Extension
     */
    public static IEditEventViewExt getEditEventViewExtension(Context context) {
        if (sEditEventViewExt == null) {
            sEditEventViewExt = (IEditEventViewExt) MPlugin.createInstance(
                    IEditEventViewExt.class.getName(), context.getApplicationContext());
            if (sEditEventViewExt == null) {
                sEditEventViewExt = new DefaultEditEventViewExt();
                LogUtil.i(TAG, "get EditEventView plugin failed, use default");
            }
        }
        return sEditEventViewExt;
    }
}
