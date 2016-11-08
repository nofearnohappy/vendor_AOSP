package com.mediatek.wallpaper.plugin;

import android.content.Context;
//import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.OperationCanceledException;

//import android.database.ContentObserver;
//import android.os.Handler;

import android.util.Log;
//import com.mediatek.xlog.Xlog;

import com.mediatek.common.PluginImpl;

import com.mediatek.common.wallpaper.IWallpaperPlugin;
import com.mediatek.op03.plugin.R;

@PluginImpl(interfaceName="com.mediatek.common.wallpaper.IWallpaperPlugin")
public class Op03WallpaperPluginExt /*extends ContextWrapper */implements IWallpaperPlugin {

    private static final String TAG = "Op03WallpaperPluginExt";
    private Context mContextWallpaperMgr = null;
    private Context mContext = null;

    public Op03WallpaperPluginExt(Context context) {
        //super(context);
        mContext = context;
      //  Xlog.d(TAG, "Op03WallpaperPluginExt: call to constructor");
        if (context == null) {
          //  Xlog.d(TAG, "Op03WallpaperPluginExt: cntx null");
        } else {
       //     Xlog.d(TAG, "Op03WallpaperPluginExt: input parameter context valid");
        }
    }

    public Resources getPluginResources(Context context) {
        mContextWallpaperMgr = context;
            return mContext.getResources();
    }

    public int getPluginDefaultImage() {
        int imageID = 0;
        imageID = R.drawable.default_wallpaper;
        log("getPluginDefaultImage: image_file_name used is default_wallpaper");
        return imageID;
    }
    public void log(String text) {
     //   Xlog.d(TAG, text);
    }

}
