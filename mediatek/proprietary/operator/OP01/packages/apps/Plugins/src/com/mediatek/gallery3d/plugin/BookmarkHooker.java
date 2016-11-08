package com.mediatek.gallery3d.plugin;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.mediatek.gallery3d.video.MovieUtils;
import com.mediatek.op01.plugin.R;

/**
 * OP01 plugin implementation of BookmarkHooker.
 */
public class BookmarkHooker extends PluginBaseHooker {
    private static final String TAG = "Gallery2/VideoPlayer/BookmarkHooker";
    private static final boolean LOG = true;

    private static final String ACTION_BOOKMARK = "com.mediatek.bookmark.VIEW";
    private static final int MENU_BOOKMARK_ADD = 1;
    private static final int MENU_BOOKMARK_DISPLAY = 2;
    private MenuItem mMenuBookmarks;
    private MenuItem mMenuBookmarkAdd;

    public static final String KEY_LOGO_BITMAP = "logo-bitmap";

    /**
     * @hide
     *
     * @param context context instance
     */
    public BookmarkHooker(Context context) {
        super(context);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuBookmarkAdd = menu.add(MENU_HOOKER_GROUP_ID,
                                 getMenuActivityId(MENU_BOOKMARK_ADD), 0,
                                 mPluginContext.getString(R.string.bookmark_add));
        mMenuBookmarks = menu.add(MENU_HOOKER_GROUP_ID,
                                getMenuActivityId(MENU_BOOKMARK_DISPLAY), 0,
                                mPluginContext.getString(R.string.bookmark_display));
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (MovieUtils.isLocalFile(getMovieItem().getUri(), getMovieItem().getMimeType())) {
            if (mMenuBookmarkAdd != null) {
                mMenuBookmarkAdd.setVisible(false);
            }
            if (mMenuBookmarks != null) {
                mMenuBookmarks.setVisible(false);
            }
        } else {
            if (mMenuBookmarkAdd != null) {
                mMenuBookmarkAdd.setVisible(true);
            }
            if (mMenuBookmarks != null) {
                mMenuBookmarks.setVisible(true);
            }
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_BOOKMARK_ADD:
            getPlayer().addBookmark();
            return true;
        case MENU_BOOKMARK_DISPLAY:
            gotoBookmark();
            return true;
        default:
            return false;
        }
    }

    private void gotoBookmark() {
        final Intent intent = new Intent(ACTION_BOOKMARK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        intent.putExtra(KEY_LOGO_BITMAP, getIntent().getParcelableExtra(KEY_LOGO_BITMAP));
        getContext().startActivity(intent);
    }
}
