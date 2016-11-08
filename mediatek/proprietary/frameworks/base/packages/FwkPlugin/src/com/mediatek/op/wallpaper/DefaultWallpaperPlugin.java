package com.mediatek.op.wallpaper;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.wallpaper.IWallpaperPlugin;

/**
 * This is the default implementation of low storage operator plugin.
 */

@PluginImpl(interfaceName = "com.mediatek.common.wallpaper.IWallpaperPlugin")
public class DefaultWallpaperPlugin implements IWallpaperPlugin {

    private static final String TAG = "DefaultWallpaperPlugin";

    /**
     * @return Return the resources object of plug-in package.
     */
    public Resources getPluginResources(Context context) {
        Log.i("@M_" + TAG, "getPluginResources..");
        return null;
    }

    /**
     * @return Return res id of default wallpaper resource.
     */
    public int getPluginDefaultImage() {
        Log.i("@M_" + TAG, "getPluginDefaultImage..");
        return 0;
    }

}
