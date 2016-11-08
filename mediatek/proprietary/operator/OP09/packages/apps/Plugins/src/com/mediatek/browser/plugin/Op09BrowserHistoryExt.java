package com.mediatek.browser.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Browser;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mediatek.browser.ext.DefaultBrowserHistoryExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;

/**
 * Add clear history menu item for CT.
 */
@PluginImpl(interfaceName = "com.mediatek.browser.ext.IBrowserHistoryExt")
public class Op09BrowserHistoryExt extends DefaultBrowserHistoryExt {

    private static final String TAG = "Op09BrowserHistoryExt";

    private int mClearMenuId;
    private Context mContext;
    /**
     * Constructor.
     * @param context browser context
     */
    public Op09BrowserHistoryExt(Context context) {
        super();
        mContext = context;
    }

    /**
     * Create the history page option menu.
     * @param menu the menu
     * @param inflater the menu inflater
     */
    public void createHistoryPageOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i("@M_" + TAG, "Enter: " + "createHistoryPageOptionsMenu" + " --OP09 implement");
        MenuItem clearMenu = menu.add(mContext.getResources().getString(R.string.clear_history));
        mClearMenuId = clearMenu.getItemId();
    }

    /**
     * Prepare the history page option menu.
     * @param menu the menu
     * @param isNull whether the adapter is null or not
     * @param isEmpty whether the adapter is empty or not
     */
    public void prepareHistoryPageOptionsMenuItem(Menu menu, boolean isNull, boolean isEmpty) {
        Log.i("@M_" + TAG, "Enter: " + "prepareHistoryPageOptionsMenuItem" + " --OP09 implement");
        MenuItem clearMenu = menu.findItem(mClearMenuId);
        if (!isNull && !isEmpty) {
            clearMenu.setEnabled(true);
        } else {
            clearMenu.setEnabled(false);
        }
    }

    /**
     * Handle the history page option menu item selected operation.
     * @param item the menu item
     * @param activity the history page activity
     * @return true to consume the menu handling already
     */
    public boolean historyPageOptionsMenuItemSelected(MenuItem item, Activity activity) {
        Log.i("@M_" + TAG, "Enter: " + "historyPageOptionsMenuItemSelected" + " --OP09 implement");
        if (item.getItemId() == mClearMenuId) {
            promptToClearHistory(activity);
            return true;
        } else {
            return false;
        }
    }
    /**
     * clear history in work thread.
     */
    static class ClearHistoryTask extends Thread {
        ContentResolver mResolver;

        public ClearHistoryTask(ContentResolver resolver) {
            mResolver = resolver;
        }

        @Override
        public void run() {
            Browser.clearHistory(mResolver);
            Browser.clearSearches(mResolver);
        }
    }

    private void promptToClearHistory(Activity activity) {
        final ContentResolver resolver = activity.getContentResolver();
        final ClearHistoryTask clear = new ClearHistoryTask(resolver);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setMessage(mContext.getResources().getString(R.string.clear_history_dlg))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                         if (which == DialogInterface.BUTTON_POSITIVE) {
                             if (!clear.isAlive()) {
                                 clear.start();
                             }
                         }
                     }
                });
        final Dialog dialog = builder.create();
        dialog.show();
    }

}
