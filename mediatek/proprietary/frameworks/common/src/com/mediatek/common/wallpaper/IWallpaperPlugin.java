package com.mediatek.common.wallpaper;

import android.content.res.Resources;
import android.content.Context;

public interface IWallpaperPlugin {

    /**
     * @return Return the resources object of plug-in package.
     */
    public Resources getPluginResources(Context context);

    /**
     * @return Return res id of default wallpaper resource.
     */
    public int getPluginDefaultImage();

}
