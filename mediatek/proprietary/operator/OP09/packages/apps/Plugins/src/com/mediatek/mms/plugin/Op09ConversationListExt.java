package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;

import com.mediatek.mms.ext.DefaultOpConversationListExt;

/**
 * Op09ConversationListExt.
 */
public class Op09ConversationListExt extends DefaultOpConversationListExt {

    private static final String TAG = "Op09ConversationListExt";

    ImageButton mImageSearchBtn;
    AdvancedSearchView mAdvancedSearchView;
    Context mContext;
    Activity mConversationList;
    SearchView mSearchView;
    public Op09ConversationListExt(Context context) {
        super(context);
        mContext =context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuItem searchItem, int base, int searchId,
            SearchView searchView) {
        mAdvancedSearchView = new AdvancedSearchView(mContext, mConversationList);
        searchItem.setActionView(mAdvancedSearchView);
        mSearchView = mAdvancedSearchView.getSearchView();
        mImageSearchBtn = mAdvancedSearchView.getImageSearchBtn();
        mImageSearchBtn.setVisibility(View.VISIBLE);
        mImageSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, AdvancedSearchActivity.class);
                intent.setAction("com.mediatek.action.AdvancedSearchActivity");
                intent.setPackage("com.mediatek.mms.plugin");
                mConversationList.startActivity(intent);
            }
        });
    }

    @Override
    public View getSearchView(SearchView searchView) {
        return mSearchView;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.equals("")) {
            if (mImageSearchBtn != null) {
                mImageSearchBtn.setVisibility(View.VISIBLE);
            }
        } else {
            mImageSearchBtn.setVisibility(View.GONE);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item, int actionSettingsId) {
        boolean result = false;
        if (item.getItemId() == actionSettingsId) {
            Intent settingIntent = new Intent();
            if (MessageUtils.isSupportTabSetting()) {
                //settingIntent = new Intent(mContext, MessageTabSettingActivity.class);
                settingIntent.setAction("com.mediatek.action.MessageTabSettingActivity");
                settingIntent.setPackage("com.android.mms");
            }
            mConversationList.startActivity(settingIntent);
            result = true;
        }
        return result;
    }

    @Override
    public boolean onClickSmsPromoBanner() {
        Intent settingIntent = new Intent();
        settingIntent.setAction("com.mediatek.action.MessageTabSettingActivity");
        settingIntent.setPackage("com.android.mms");
        mConversationList.startActivity(settingIntent);
        return false;
    }

    @Override
    public boolean onCreate(Activity activity, Bundle savedInstanceState) {
        mConversationList = activity;
        return false;
    }
}
