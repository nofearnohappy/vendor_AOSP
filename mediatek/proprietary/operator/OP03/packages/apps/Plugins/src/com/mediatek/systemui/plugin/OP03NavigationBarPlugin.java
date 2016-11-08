
package com.mediatek.systemui.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.mediatek.common.PluginImpl;
import com.mediatek.op03.plugin.R;
import com.mediatek.systemui.ext.DefaultNavigationBarPlugin;


/**
 * M: OP03 implementation of Plug-in definition of Navigation bar.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.INavigationBarPlugin")
public class OP03NavigationBarPlugin extends DefaultNavigationBarPlugin {

    public Context mContext;

    public OP03NavigationBarPlugin(Context context) {
        super(context);
        mContext = context;
    }

    public Drawable getBackImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_back);
    }

    public Drawable getBackLandImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_back_land);
    }

    public Drawable getBackImeImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_back_ime);
    }

    public Drawable getBackImelandImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_back_ime_land);
    }

    public Drawable getHomeImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_home);
    }

    public Drawable getHomeLandImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_home_land);
    }

    public Drawable getRecentImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_recent);
    }

    public Drawable getRecentLandImage(Drawable drawable) {
        return mContext.getResources().getDrawable(R.drawable.ic_sysbar_recent_land);
    }
}
