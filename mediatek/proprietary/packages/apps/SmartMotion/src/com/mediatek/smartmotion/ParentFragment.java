package com.mediatek.smartmotion;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.mediatek.smartmotion.enabler.EasyRejectEnabler;

public class ParentFragment extends PreferenceFragment {
    protected Switch mActionBarSwitch;
    protected Activity mActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        LayoutInflater inflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mActionBarSwitch = new Switch(mActivity);//(Switch) inflater.inflate(
//                com.mediatek.internal.R.layout.imageswitch_layout, null);

        final int padding = mActivity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
        mActivity.getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        mActivity.getActionBar().setCustomView(
                mActionBarSwitch,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));

        ((MainActivity) mActivity).attachFragment(true);
        mActivity.invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        ((MainActivity)mActivity).attachFragment(false);
        mActivity.invalidateOptionsMenu();
        super.onDestroy();
    }
}
