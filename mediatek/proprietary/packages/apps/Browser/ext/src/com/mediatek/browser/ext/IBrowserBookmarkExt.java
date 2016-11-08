package com.mediatek.browser.ext;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public interface IBrowserBookmarkExt {

    /**
     * Add the default bookmarks for customer
     * @param db the Browser database
     * @return the number of the bookmarks added
     * @internal
     */
    int addDefaultBookmarksForCustomer(SQLiteDatabase db);

    /**
     * Create the bookmark page option menu
     * @param menu the menu
     * @param inflater the menu inflater
     * @internal
     */
    void createBookmarksPageOptionsMenu(Menu menu, MenuInflater inflater);

    /**
     * Handle the bookmark page option menu item selected operation
     * @param item the menu item
     * @param activity the bookmark page activity
     * @param folderId the current folder id
     * @return true to consume the menu handling already
     * @internal
     */
    boolean bookmarksPageOptionsMenuItemSelected(MenuItem item, Activity activity, long folderId);

}
