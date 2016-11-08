package com.mediatek.gallery3d.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mediatek.gallery3d.ext.DefaultActivityHooker;
import com.mediatek.gallery3d.video.IMoviePlayer;
import android.util.Log;

public class PluginBaseHooker extends DefaultActivityHooker {
    private static final String TAG = "Gallery2/VideoPlayer/PluginBaseHooker";
    private static final boolean LOG = true;
    protected Context mPluginContext;
    private IMoviePlayer mPlayer;

    public PluginBaseHooker() {
        super();
        mPluginContext = null;
    }

    public PluginBaseHooker(Context context) {
        super();
        mPluginContext = context;
    }

    @Override
    public void setParameter(String key, Object value) {
        super.setParameter(key, value);
        Log.i(TAG, "setParameter() value = " + value);
        if (value instanceof IMoviePlayer) {
            mPlayer = (IMoviePlayer) value;
            onMoviePlayerChanged(mPlayer);
            Log.i(TAG, "setParameter() mPlayer = " + mPlayer);
        }
    }

    @Override
    public int getMenuActivityId(int id) {
        return super.getMenuActivityId(id);
    };

    @Override
    public int getMenuOriginalId(int id) {
        return super.getMenuOriginalId(id);
    }

    @Override
    public void init(Activity context, Intent intent) {
        super.init(context, intent);
    }

    @Override
    public Activity getContext() {
        return super.getContext();
    }

    @Override
    public Intent getIntent() {
        return super.getIntent();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setVisibility(boolean visible) {
        super.setVisibility(visible);
    }

    public IMoviePlayer getPlayer() {
        return mPlayer;
    }

    public void onMoviePlayerChanged(final IMoviePlayer player) {
    }
}
