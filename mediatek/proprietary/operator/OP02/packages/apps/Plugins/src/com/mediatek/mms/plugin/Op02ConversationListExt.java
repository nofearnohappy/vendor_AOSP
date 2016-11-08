package com.mediatek.mms.plugin;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.mediatek.mms.ext.DefaultOpConversationListExt;

public class Op02ConversationListExt extends DefaultOpConversationListExt {

    private Op02MmsConversationExt mMmsConversationExt;

    public Op02ConversationListExt(Context context) {
        super(context);
        mMmsConversationExt = new Op02MmsConversationExt(context);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuItem searchItem, int base, int searchId,
            SearchView searchView) {
        mMmsConversationExt.addOptionMenu(menu, base);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mMmsConversationExt.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item, int actionSettingsId) {
        return mMmsConversationExt.onOptionsItemSelected(item);
    }
}
