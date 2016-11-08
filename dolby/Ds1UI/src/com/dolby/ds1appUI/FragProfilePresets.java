/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
package com.dolby.ds1appUI;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.dolby.IDsClientEvents;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;

import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tag;
import com.dolby.ds1appCoreUI.Tools;

public class FragProfilePresets extends Fragment implements OnClickListener,
        OnItemClickListener, OnItemLongClickListener, IDsClientEvents {

    // Data members.
    // Profile stuff.
    private ProfilesAdapter mProfilesAdapter;

    // Tooltip stuff?
    private ViewGroup mNativeRootContainer;

    // DsClient instance.
    private DsClient mDsClient;
    // Required to know whether our local instance has connected to the service
    // or not.
    private boolean mDolbyClientConnected = false;

    // Our observers.
    // Specific Profile Presets Fragment Observer.
    private IDsFragProfilePresetsObserver mSpecificObserver;
    // Generic Fragment Observer (error handling / generic provider).
    private IDsFragObserver mFObserver;

    private boolean mMobileLayout = false;

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
            mSpecificObserver = (IDsFragProfilePresetsObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IDsFragProfilePresetsObserver");
        }

        // Activity supports both Interfaces!
        // Fetching DsClient instance.
        mDsClient = mFObserver.getDsClient();
        // Learning whether we are operating in a "mobile" layout or not.
        mMobileLayout = getResources().getBoolean(R.bool.newLayout);
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
        // Inflating appropriate layout.
        View v = inflater.inflate(R.layout.fragprofilepresets, container, false);

        // Setting up profile presets list.
        @SuppressWarnings("unchecked")
        AdapterView<ListAdapter> lv = (AdapterView<ListAdapter>) v.findViewById(R.id.presetsListView);
        // TODO change this to expect IDsFragObserver somehow.
        mProfilesAdapter = new ProfilesAdapter((MainActivity) getActivity(), R.layout.preset_list_item, mDsClient, (OnClickListener) this);
        lv.setAdapter(mProfilesAdapter);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);

        // Related to tooltip display.
        mNativeRootContainer = ViewTools.determineNativeViewContainer(getActivity());

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If in mobile layout, reporting for second phase initialization.
        if (mMobileLayout == true) {
            mSpecificObserver.profilePresetsAreAlive();
        }
    }

    @Override
    public void onPause() {

        if (null != mProfilesAdapter) {
            mProfilesAdapter.endEditingProfileName(true);
        }

        super.onPause();
    }

    // From IDsClientEvents.
    @Override
    public void onClientConnected() {
        mDolbyClientConnected = true;
        if (mMobileLayout == true) {
            mSpecificObserver.profilePresetsAreAlive();
        }
    }

    @Override
    public void onClientDisconnected() {
        mDolbyClientConnected = false;
    }

    @Override
    public void onDsOn(boolean on) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProfileSelected(int profile) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProfileSettingsChanged(int profile) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProfileNameChanged(int profile, String name) {
        if (mProfilesAdapter != null) {
            // Redraw the list of profiles (with new profile name).
            mProfilesAdapter.scheduleNotifyDataSetChanged();
        }
    }

    @Override
    public void onEqSettingsChanged(int profile, int preset) {

        DsClientSettings settings;
        final int selectedProfile;
        try {
            settings = mDsClient.getProfileSettings(profile);
            DsClientCache.INSTANCE.cacheProfileSettings(mDsClient, profile, settings);
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }

        if (profile == selectedProfile) {
            // Redraw the list of profiles (with optional revert button).
            if (null != mProfilesAdapter) {
                mProfilesAdapter.scheduleNotifyDataSetChanged();
            }
        }
    }

    // From OnClickListener.
    @Override
    public void onClick(View view) {
        onDolbyClientUseClick(view);
    }

    // From OnItemLongClickListener.
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (parent.getId() == R.id.presetsListView) {
            try {
                if (DsClientCache.INSTANCE.getSelectedProfile(mDsClient) != (position-1)) {
                    onItemClick(parent, view, position, id);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mFObserver.onDsApiError();
                return true;
            }
            
            if (null != mProfilesAdapter) {
                mProfilesAdapter.startEditingProfileName(position);
            }
            return true;
        }
        return false;
    }

    // From OnItemClickListener.
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (position == Constants.DEMO_POSITION){
			if ((mFObserver != null) && mFObserver.isDolbyClientConnected()) {
				startActivity(new Intent(MainActivity.ACTION_LAUNCH_DS1_INSTOREDEMO_APP));
			}
            return;
        }
        if (mMobileLayout == true) {
            try {
                if (DsClientCache.INSTANCE.getSelectedProfile(mDsClient) == (position-1)) {
                    // Click was on an already selected profile. Activating
                    // edition mode.
                    mSpecificObserver.editProfile();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                mFObserver.onDsApiError();
                return;
            }
        }

        if (null != mProfilesAdapter) {
            mProfilesAdapter.endEditingProfileName(true);
        }
        
        if ((getView() == null) || (mFObserver == null)) {
            Log.w(Tag.MAIN, "FragProfilePresets.onItemClick(), getView() == null or mFObserver == null.");
            return;
        }
        if (parent == getView().findViewById(R.id.presetsListView) && mFObserver.useDsApiOnUiEvent()) {
            mSpecificObserver.chooseProfile(position-1);
        }
    }

    public void setSelection(int profile) {
        if (mProfilesAdapter != null) {
            mProfilesAdapter.setSelection(profile+1);
        }
    }

    public int getSelection() {
        if (null != mProfilesAdapter) {
            return mProfilesAdapter.getSelection()-1;
        } else {
            return 0;
        }
    }

    public String getDefaultProfileName(int profile) {
        if (null != mProfilesAdapter) {
            return mProfilesAdapter.getDefaultProfileName(profile+1);
        } else {
            return "";
        }
    }

    public String getItemName(int profile) {
        if (mProfilesAdapter != null) {
            return mProfilesAdapter.getItemName(profile+1);
        }
        return "";
    }

    public void setEnabled(boolean on) {

        if (this.isAdded()) {
            View theFragV = getView();

            if (!on) {
                if (mProfilesAdapter != null) {
                    mProfilesAdapter.endEditingProfileName(true);
                }
            }

            View listView = theFragV.findViewById(R.id.presetsListView);
            if (listView != null) {
                listView.setEnabled(on);
            }
        }
    }

    public void scheduleNotifyDataSetChanged() {
        if (mProfilesAdapter != null) {
            mProfilesAdapter.scheduleNotifyDataSetChanged();
        }
    }

    /**
     * Call this when going into profile name edit mode using software keyboard.
     */
    // Making public so it can be accessed from MainActivity.
    public void onProfileNameEditStarted() {
        Log.d(Tag.MAIN, "Main.onProfileNameEditStarted()");

        if (!Tools.isLandscapeScreenOrientation(getActivity()) || mNativeRootContainer == null) {
            return;
        }

        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {

            private int counter = 30;

            private final Runnable refreshLayout = new Runnable() {

                @Override
                public void run() {
                    refreshLayout();
                }
            };

            private final Runnable removePreDrawListener = new Runnable() {

                @Override
                public void run() {
                    removePreDrawListener();
                }
            };

            private boolean skipNext = false;

            @Override
            public boolean onPreDraw() {
                Log.d(Tag.MAIN, "Main.onProfileNameEditStarted.onPreDraw() " + counter--);
                if (!skipNext) {
                    DS1Application.HANDLER.removeCallbacks(refreshLayout);
                    DS1Application.HANDLER.postDelayed(refreshLayout, 100);
                } else {
                    skipNext = false;
                }
                if (counter <= 0) {
                    removePreDrawListener();
                }
                return true;
            }

            private void refreshLayout() {
                Log.d(Tag.MAIN, "Main.onProfileNameEditStarted.refreshLayout()");
                if (mNativeRootContainer == null) {
                    return;
                }

                skipNext = true;

                mNativeRootContainer.requestLayout();
                mNativeRootContainer.invalidate();

                DS1Application.HANDLER.removeCallbacks(refreshLayout);
                DS1Application.HANDLER.removeCallbacks(removePreDrawListener);
                DS1Application.HANDLER.postDelayed(removePreDrawListener, 2000);
            }

            private void removePreDrawListener() {
                Log.d(Tag.MAIN, "Main.onProfileNameEditStarted.removePreDrawListener()");
                DS1Application.HANDLER.removeCallbacks(refreshLayout);
                DS1Application.HANDLER.removeCallbacks(removePreDrawListener);
                mNativeRootContainer.getViewTreeObserver().removeOnPreDrawListener(this);
            }
        };

        mNativeRootContainer.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private void onDolbyClientUseClick(View view) {
        if (!mDolbyClientConnected || !mFObserver.useDsApiOnUiEvent()) {
            return;
        }

		// 1. get the custom modify status.
		boolean bModified_Custom1 = false;
		boolean bModified_Custom2 = false;
		final int cmf = DS1Application.getCustomModifyFlag(getActivity());
		if (DS1Application.CUSTOM_1_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_1_NAME_MODIFIED)) {
			bModified_Custom1 = true;
		}

		if (DS1Application.CUSTOM_2_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_2_NAME_MODIFIED)) {
			bModified_Custom2 = true;
		}

        final int selectedProfile;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);

            // 2. save the modified custom status.
            if (selectedProfile + 1 == Constants.PROFILE_CUSTOM1_INDEX) {
                bModified_Custom1 = false;
            } else if (selectedProfile + 1 == Constants.PROFILE_CUSTOM2_INDEX) {
                bModified_Custom2 = false;
            }
            DS1Application.saveCustomNameModifiedStatus(getActivity(), bModified_Custom1, bModified_Custom2);
        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }

        final int id = view.getId();

        if (R.id.revertButton == id) { // Revert icon in ProfilesAdapter.
            // Doing local reset (nothing really left to do).

            // Notifying observer that the given profile was reset and that
            // the "event" should be propagated.
            mSpecificObserver.profileReset(selectedProfile);
        }
    }
}
