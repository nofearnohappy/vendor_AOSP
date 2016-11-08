package com.mediatek.gallery3d.plugin;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;

import com.mediatek.op02.plugin.R;


public class StepOptionSettingsHooker extends PluginBaseHooker {
    private static final int MENU_STEP_OPTION_SETTING = 1;
    private MenuItem mMenuStepOption;

    public StepOptionSettingsHooker(Context context) {
        super(context);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuStepOption = menu.add(MENU_HOOKER_GROUP_ID, getMenuActivityId(MENU_STEP_OPTION_SETTING), 0, mPluginContext.getString(R.string.settings));
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_STEP_OPTION_SETTING:
            //start activity
            Intent mIntent = new Intent();
            mIntent.setClass(mPluginContext, VideoSettingsActivity.class);
            //mIntent.setFlags(mIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(mIntent);
            return true;
        default:
            return false;
        }
    }
    @Override
    public void setVisibility(boolean visible) {
        if (mMenuStepOption != null) {
            mMenuStepOption.setVisible(visible);
        }
    }
}