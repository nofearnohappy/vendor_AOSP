package com.mediatek.browser.ext;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class DefaultBrowserBookmarkExt implements IBrowserBookmarkExt {

    private static final String TAG = "DefaultBrowserBookmarkExt";

    @Override
    public int addDefaultBookmarksForCustomer(SQLiteDatabase db) {
        Log.i("@M_" + TAG, "Enter: " + "addDefaultBookmarksForCustomer" + " --default implement");
        return 0;
    }

    @Override
    public void createBookmarksPageOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i("@M_" + TAG, "Enter: " + "createBookmarksPageOptionsMenu" + " --default implement");
    }

    @Override
    public boolean bookmarksPageOptionsMenuItemSelected(MenuItem item, Activity activity, long folderId) {
        Log.i("@M_" + TAG, "Enter: " + "bookmarksPageOptionsMenuItemSelected" + " --default implement");
        return false;
    }

}
