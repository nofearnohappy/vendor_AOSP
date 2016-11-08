package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.mediatek.op09.plugin.R;
import com.mediatek.mms.ext.DefaultOpRecipientListActivityExt;

public class Op09RecipientListActivityExt extends DefaultOpRecipientListActivityExt {

    Context mContext;
    Activity mRecipientListActivity;

    @Override
    public boolean onOptionsItemSelected(boolean isSetting) {
        // TODO Auto-generated method stub
        if (isSetting) {
            Intent settingIntent = new Intent();
            settingIntent.setAction("com.mediatek.action.MessageTabSettingActivity");
            settingIntent.setPackage("com.android.mms");
            mRecipientListActivity.startActivity(settingIntent);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Activity activity, Bundle savedInstanceState) {
        mRecipientListActivity = activity;
    }
}
