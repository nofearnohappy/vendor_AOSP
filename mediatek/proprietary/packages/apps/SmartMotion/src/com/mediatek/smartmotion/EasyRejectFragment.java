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

public class EasyRejectFragment extends ParentFragment {
    private static final String TAG = "EasyRejectFragment";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        EasyRejectEnabler.registerSwitch(mActivity, mActionBarSwitch);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        EasyRejectEnabler.unregisterSwitch(mActionBarSwitch);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.easy_reject_fragment, container, false);
        return view;
    }

}
