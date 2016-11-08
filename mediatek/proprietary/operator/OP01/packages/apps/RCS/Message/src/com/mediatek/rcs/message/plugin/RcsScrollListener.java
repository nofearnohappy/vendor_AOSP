package com.mediatek.rcs.message.plugin;


import android.content.Context;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.HeaderViewListAdapter;

import com.mediatek.mms.ipmessage.DefaultIpScrollListenerExt;

/**
 * Plugin implements. response ScrollListener.java in MMS host.
 *
 */
public class RcsScrollListener extends DefaultIpScrollListenerExt {
    private static String TAG = "RcseScrollListener";

    @Override
    public Adapter onIpScrollStateChanged(AbsListView view) {
        Log.d(TAG, "onIpQueryComplete ");
        if ((view.getAdapter()) instanceof HeaderViewListAdapter) {
            Log.d(TAG, "header adapter, unwrapp.");
            HeaderViewListAdapter wrappedAdapter = (HeaderViewListAdapter) view.getAdapter();
            return wrappedAdapter.getWrappedAdapter();
        } else {
            Log.d(TAG, "return null.");
            return null;
        }
    }
}
