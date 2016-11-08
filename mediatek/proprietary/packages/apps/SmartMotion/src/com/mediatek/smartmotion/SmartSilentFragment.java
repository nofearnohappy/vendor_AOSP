package com.mediatek.smartmotion;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.mediatek.smartmotion.enabler.SmartSilentEnabler;

public class SmartSilentFragment extends ParentFragment {
    private static final String TAG = "SmartSilentFragment";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        SmartSilentEnabler.registerSwitch(mActivity, mActionBarSwitch);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        SmartSilentEnabler.unregisterSwitch(mActionBarSwitch);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.smart_silent_fragment, container, false);
        return view;
    }

}
