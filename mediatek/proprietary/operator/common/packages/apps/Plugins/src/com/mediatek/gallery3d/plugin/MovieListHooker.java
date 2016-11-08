package com.mediatek.gallery3d.plugin;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import com.mediatek.common.plugin.R;
import com.mediatek.gallery3d.video.IMovieItem;
import com.mediatek.gallery3d.video.IMovieList;
import com.mediatek.gallery3d.video.IMovieListLoader;
import com.mediatek.gallery3d.video.IMovieListLoader.LoaderListener;
import com.mediatek.gallery3d.video.DefaultMovieListLoader;


public class MovieListHooker extends PluginBaseHooker implements LoaderListener {
    private static final String TAG = "Gallery2/VideoPlayer/AbstractMovieListHooker";
    private static final boolean LOG = true;

    private static final int MENU_NEXT = 1;
    private static final int MENU_PREVIOUS = 2;

    private static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST"; // Gallery will enable this feature

    private MenuItem mMenuNext;
    private MenuItem mMenuPrevious;

    private IMovieListLoader mMovieLoader;
    private IMovieList mMovieList;

    public MovieListHooker(Context context) {
        super(context);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovieLoader = new DefaultMovieListLoader();
        mMovieLoader.fillVideoList(getContext(), getIntent(), this, getMovieItem());
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMovieLoader.cancelList();
    }
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mMovieList != null) { //list should be filled
            if (mMovieLoader != null && isEnabledVideoList(getIntent())) {
                mMenuPrevious = menu.add(MENU_HOOKER_GROUP_ID,
                                         getMenuActivityId(MENU_PREVIOUS),
                                         0,
                                         mPluginContext.getString(R.string.previous));
                mMenuNext = menu.add(MENU_HOOKER_GROUP_ID,
                                     getMenuActivityId(MENU_NEXT),
                                     0,
                                     mPluginContext.getString(R.string.next));
            }
        }
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updatePrevNext();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_PREVIOUS:
            if (mMovieList == null) {
                return false;
            }
            getPlayer().startNextVideo(mMovieList.getPrevious(getMovieItem()));
            return true;
        case MENU_NEXT:
            if (mMovieList == null) {
                return false;
            }
            getPlayer().startNextVideo(mMovieList.getNext(getMovieItem()));
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onMovieItemChanged(final IMovieItem item) {
        super.onMovieItemChanged(item);
        updatePrevNext();
    }

    private void updatePrevNext() {
        if (LOG) {
            Log.v(TAG, "updatePrevNext()");
        }
        if (mMovieList != null && mMenuPrevious != null && mMenuNext != null) {
            if (isFirst(getMovieItem()) && isLast(getMovieItem())) { //only one movie
                mMenuNext.setVisible(false);
                mMenuPrevious.setVisible(false);
            } else {
                mMenuNext.setVisible(true);
                mMenuPrevious.setVisible(true);
            }
            if (isFirst(getMovieItem())) {
                mMenuPrevious.setEnabled(false);
            } else {
                mMenuPrevious.setEnabled(true);
            }
            if (isLast(getMovieItem())) {
                mMenuNext.setEnabled(false);
            } else {
                mMenuNext.setEnabled(true);
            }
        }
    }

    @Override
    public void onListLoaded(final IMovieList movieList) {
        mMovieList = movieList;
        getContext().invalidateOptionsMenu();
        if (LOG) {
            Log.v(TAG, "onListLoaded() " + (mMovieList != null ? mMovieList.size() : "null"));
        }
    }


    private boolean isFirst(IMovieItem item) {
        return mMovieList.getPrevious(item) == null;
    }


    private boolean isLast(IMovieItem item) {
        return mMovieList.getNext(item) == null;
    }

    private boolean isEnabledVideoList(Intent intent) {
        boolean enable = true;
        if (intent != null && intent.hasExtra(EXTRA_ENABLE_VIDEO_LIST)) {
            enable = intent.getBooleanExtra(EXTRA_ENABLE_VIDEO_LIST, true);
        }
        if (LOG) {
            Log.v(TAG, "isEnabledVideoList() return " + enable);
        }
        return enable;
    }
}
