/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
package com.dolby.ds1appUI;

import android.app.Activity;
import android.app.Fragment;
import android.dolby.DsClient;
import android.dolby.DsConstants;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class FragPower extends Fragment implements OnClickListener {
	private final static String TAG = "FragPower";

    private ImageView mImgon;
    private ImageView mImgoff;
    private DsClient mDsClient;

    // Our observers.
    // Specific Power Fragment Observer.
    private IDsFragPowerObserver mSpecificObserver;
    // Generic Fragment Observer (error handling / generic provider).
    private IDsFragObserver mFObserver;

    // From Fragment.
    // First method called by the framework.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // The activity shall implement the interfaces we require.
        // Ideally, we should receive those as parameters, since that
        // way, it's more obvious and detached from an activity object. We'll do
        // just that after finishing bringing this up, unless this way is found
        // to be sufficient.
        // CONSIDER THIS TEMPORARY. This might change.

        try {
            mFObserver = (IDsFragObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IDsFragObserver");
        }

        try {
            mSpecificObserver = (IDsFragPowerObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IDsFragPowerObserver");
        }

        // Activity supports both Interfaces!
        // Fetching DsClient instance.
        mDsClient = mFObserver.getDsClient();
    }

    // Second method called by the framework.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Third method called by the framework: this is where we
    // actually inflate the layout.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragpower, container, false);
        mImgon = (ImageView) v.findViewById(R.id.powerButtonOn);
        mImgoff = (ImageView) v.findViewById(R.id.powerButtonOff);
        mImgon.setOnClickListener(this);
        mImgon.setSoundEffectsEnabled(false);
        mImgoff.setOnClickListener(this);
        mImgoff.setSoundEffectsEnabled(false);
        return v;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (R.id.powerButtonOn == id || R.id.powerButtonOff == id) {
            try {
                final boolean on = !DsClientCache.INSTANCE.isDsOn();
				int result = mDsClient.setDsOnChecked(on);
				// Check for failure to enable the effect
				if (result != DsConstants.DS_NO_ERROR) {
					Log.e(TAG, "FragPower.onClick, setDsOnChecked failed due to return code: " + result);
					return;
				}
			
                // Ds may have refused our request. Make sure we cache the actual value
                DsClientCache.INSTANCE.cacheDsOn(mDsClient.getDsOn());
            } catch (Exception e) {
                e.printStackTrace();
                mFObserver.onDsApiError();
                return;
            }
            mSpecificObserver.onDsClientUseChanged(true);
        }
    }

    public void setEnabled(boolean on) {
        mImgon.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
    }
}
