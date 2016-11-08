package com.mediatek.gallery3d.plugin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.mediatek.gallery3d.video.MovieUtils;
import com.mediatek.op01.plugin.R;

/**
 * OP01 plugin implementation of StreamingHooker.
 */
public class StreamingHooker extends PluginBaseHooker {
    private static final String TAG = "Gallery2/VideoPlayer/StreamingHooker";
    private static final boolean LOG = true;

    private static final String ACTION_STREAMING = "com.mediatek.settings.streaming";
    private static final int MENU_INPUT_URL = 1;
    private static final int MENU_SETTINGS = 2;
    private static final int MENU_DETAIL = 3;
    private MenuItem mMenuDetail;
    private MenuItem mMenuSettings;
    private MenuItem mMenuInput;

    public static final String KEY_LOGO_BITMAP = "logo-bitmap";

    /**
     * @hide
     *
     * @param context context instance
     */
    public StreamingHooker(Context context) {
        super(context);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        //when in rtsp streaming type, generally it only has one uri.
        mMenuDetail = menu.add(MENU_HOOKER_GROUP_ID, getMenuActivityId(MENU_DETAIL),
                      0, mPluginContext.getString(R.string.media_detail));
        mMenuInput = menu.add(MENU_HOOKER_GROUP_ID, getMenuActivityId(MENU_INPUT_URL),
                      0, mPluginContext.getString(R.string.input_url));
        mMenuSettings = menu.add(MENU_HOOKER_GROUP_ID, getMenuActivityId(MENU_SETTINGS),
                      0, mPluginContext.getString(R.string.streaming_settings));
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (MovieUtils.isLocalFile(getMovieItem().getUri(), getMovieItem().getMimeType())) {
            if (mMenuDetail != null) {
                mMenuDetail.setVisible(false);
            }
            if (mMenuSettings != null) {
                mMenuSettings.setVisible(false);
            }
            if (mMenuInput != null) {
                mMenuInput.setVisible(false);
            }
        } else {
            if (mMenuDetail != null) {
                mMenuDetail.setVisible(true);
            }
            if (mMenuSettings != null) {
                mMenuSettings.setVisible(true);
            }
            if (mMenuInput != null) {
                mMenuInput.setVisible(true);
            }
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_INPUT_URL:
            gotoInputUrl();
            return true;
        case MENU_SETTINGS:
            gotoSettings();
            return true;
        case MENU_DETAIL:
            getPlayer().showDetail();
            return true;
        default:
            return false;
        }
    }

    private void gotoInputUrl() {
        final String appName = getClass().getName();
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("about:blank"));
        intent.putExtra("inputUrl", true);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, appName);
        getContext().startActivity(intent);
        if (LOG) {
            Log.v(TAG, "gotoInputUrl() appName=" + appName);
        }
    }

    private void gotoSettings() {
        final Intent intent = new Intent(ACTION_STREAMING);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        intent.putExtra(KEY_LOGO_BITMAP, getIntent().getParcelableExtra(KEY_LOGO_BITMAP));
        getContext().startActivity(intent);
        if (LOG) {
            Log.v(TAG, "gotoInputUrl()");
        }
    }
}
