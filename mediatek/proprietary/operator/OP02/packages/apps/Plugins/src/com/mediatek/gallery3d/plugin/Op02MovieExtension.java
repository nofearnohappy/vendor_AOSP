package com.mediatek.gallery3d.plugin;

import android.content.Context;

import com.mediatek.gallery3d.ext.DefaultMovieExtension;
import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.ext.IRewindAndForwardExtension;

import com.android.gallery3d.app.MovieActivity;
import com.mediatek.common.PluginImpl;

import java.util.ArrayList;

@PluginImpl(interfaceName="com.mediatek.gallery3d.ext.IMovieExtension")
public class Op02MovieExtension extends DefaultMovieExtension {
    private static final String TAG = "Op02MovieExtension";
    private static final boolean LOG = true;

    public Op02MovieExtension(Context context) {
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
        list.add(new StepOptionSettingsHooker(mContext));
        return list;
    }

    @Override
    public IRewindAndForwardExtension getRewindAndForwardExtension() {
        return new RewindAndForward(mContext);
    }
}
