package com.android.quicksearchbox;

import com.android.quicksearchbox.preferences.SearchEngineItemsController;
import com.mediatek.search.SearchEngineManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/**
 * QsbReceiver to receive some brocast such as search engine changed.
 *
 */
public class QsbReceiver extends BroadcastReceiver {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.QsbReceiver";

    /**
     * broadcast intent action for search engine change, immediately update web
     * search hint in search widget
     */
    public static final String ACTION_SEARCH_ENGINE_CHANGED
            = "com.android.quicksearchbox.SEARCH_ENGINE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG) {
            Log.d(TAG, "onReceive(" + intent.toUri(0) + ")");
        }
        String action = intent.getAction();
        SharedPreferences p = context.getSharedPreferences(
                SearchSettingsImpl.PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (ACTION_SEARCH_ENGINE_CHANGED.equals(action)) {
            // the broadcast is from QSB or Browser
            Bundle extra = intent.getExtras();
            String engineName = null;

            if (null != extra) {
                // the broadcast is from Browser
                engineName = extra.getString(SearchEngineItemsController.SEARCH_ENGINE_PREF);
                QsbApplication.get(context).updateSearchEngineExternal(p, engineName);
            } else {
                // the broadcast is from QSB
                QsbApplication.get(context).updateSearchEngine(p);
            }
        } else if (SearchEngineManager.ACTION_SEARCH_ENGINE_CHANGED.equals(action)) {
            // the broadcast is from framework
            QsbApplication.get(context).updateSearchEngineExternal(p, null);
        }
    }

}
