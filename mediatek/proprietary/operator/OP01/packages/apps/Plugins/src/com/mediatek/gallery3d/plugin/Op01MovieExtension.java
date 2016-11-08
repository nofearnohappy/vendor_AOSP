package com.mediatek.gallery3d.plugin;

import android.content.Context;

import com.android.gallery3d.app.MovieActivity;
import com.mediatek.common.PluginImpl;
import com.mediatek.gallery3d.ext.DefaultMovieExtension;
import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.ext.IServerTimeoutExtension;

import java.util.ArrayList;

/**
 * OP01 plugin implementation of MovieExtension.
 */
@PluginImpl(interfaceName = "com.mediatek.gallery3d.ext.IMovieExtension")
public class Op01MovieExtension extends DefaultMovieExtension {
    private static final String TAG = "Op01MovieExtension";
    private static final boolean LOG = true;

    /**
     * @hide
     *
     * @param context context instance
     */
    public Op01MovieExtension(Context context) {
        super(context);
    }

    @Override
    public boolean shouldEnableCheckLongSleep() {
        return false;
    }

    @Override
    public ArrayList<IActivityHooker> getHookers(Context context) {
        ArrayList<IActivityHooker> list = new ArrayList<IActivityHooker>();
        if (context instanceof MovieActivity) {
            list.add(new MovieListHooker(mContext));
        }
        list.add(new BookmarkHooker(mContext));
        list.add(new StereoAudioHooker(mContext));
        list.add(new StreamingHooker(mContext));
        return list;
    }

    @Override
    public IServerTimeoutExtension getServerTimeoutExtension() {
        return new ServerTimeout(mContext);
    }
}
