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
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.dolby.IDsClientEvents;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tools;

public class FragProfilePresetEditor extends Fragment implements
        OnClickListener, OnLongClickListener, OnEditorActionListener,
        OnKeyListener, IDsClientEvents {

    // Data members.
    // Profile stuff.
    private ProfileEditInfo mCurrentlyEditedProfile;

    // DsClient instance.
    private DsClient mDsClient;
    // Required to know whether our local instance has connected to the service
    // or not.
    private boolean mDolbyClientConnected = false;

    // Our observers.
    // Specific Profile Editor Fragment Observer.
    private IDsFragProfileEditorObserver mSpecificObserver;
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
            mSpecificObserver = (IDsFragProfileEditorObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IDsFragProfileEditorObserver");
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
        // Inflating appropriate layout.
        View v = inflater.inflate(R.layout.fragprofileeditor, container, false);

        // Updating labels' font, related to this fragment.
        final int textIds[] = { R.id.presetName };
        for (int id : textIds) {
            TextView tv = (TextView) v.findViewById(id);
            if (tv != null) {
                tv.setTypeface(Assets.getFont(Assets.FontType.REGULAR));
            }
        }

        // Adding Click Listeners.
        // Preset name editable only when not in full screen.
        View theV = v.findViewById(R.id.presetName);
        if (theV != null) {
            theV.setOnLongClickListener(this);
        }

        // Revert button in portrait mode, in large and xlarge.
        theV = v.findViewById(R.id.revertButtonMain);
        if (theV != null) {
            theV.setOnClickListener(this);
        }

        // Finding out if using mobile layout.
        mMobileLayout = getResources().getBoolean(R.bool.newLayout);

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
            mSpecificObserver.profileEditorIsAlive();
        }
    }

    // From IDsClientEvents.
    @Override
    public void onClientConnected() {
        mDolbyClientConnected = true;
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
        setResetProfileVisibility();

        final int selectedProfile;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }

        if (profile == selectedProfile) {
            // change the profile name TextView if used in the current layout
            View temp = getView();
            if (temp != null) {
                TextView tv = (TextView) temp.findViewById(R.id.presetName);
                if (tv != null) {
                    tv.setText(name);
                }
            }
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
            setResetProfileVisibility();
        }
    }

    // From OnLongClickListener.
    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.presetName) {
            if (getView() != null) {
                startEditingProfileName((TextView) view, (EditText) getView().findViewById(R.id.presetNameEdit), mSpecificObserver.getProfileSelected() + 1);
            }
        }
        return true;
    }

    // From OnClickListener.
    @Override
    public void onClick(View view) {
        onDolbyClientUseClick(view);
    }

    // From OnEditorActionListener.
    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if ((view.getId() == R.id.presetNameEdit)) {
            if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_PREVIOUS) && (event == null || (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                // Accept profile name change.
                endEditingProfileName(true);
                return true;
            }
        }
        return false;
    }

    // From OnKeyListener.
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if ((view.getId() == R.id.presetNameEdit)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                // Cancel profile name change.
                endEditingProfileName(false);
                return true;
            }
        }
        return false;
    }

    public void setEnabled(boolean on) {
        View theFragV = getView();
        if (theFragV == null) {
            return;
        }

        if (!on) {
            // Turning off. So finishing edition.
            endEditingProfileName(true);
        }

        setResetProfileVisibility();

        TextView tv = (TextView) theFragV.findViewById(R.id.presetName);
        if (tv != null) {
            if (!on) {
                tv.setText(R.string.off);
            }
            tv.setEnabled(on);
        }

        View v = theFragV.findViewById(R.id.revertButtonMain);
        if (v != null) {
            v.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setResetProfileVisibility() {
        if (!mDolbyClientConnected) {
            return;
        }
        // check this fragment is attached or not. 
        if (mFObserver == null) {
            return;
        }
        
        boolean modified = false;
        int tmpProfile = -1;
        try {
            final int profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
            tmpProfile = profile;
            modified = mDsClient.isProfileModified(profile);
        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }

        String profileName = "";
        switch (tmpProfile) {
        case 0:
            profileName = getString(R.string.movie);
            break;
        case 1:
            profileName = getString(R.string.music);
            break;
        case 2:
            profileName = getString(R.string.game);
            break;
        case 3:
            profileName = getString(R.string.voice);
            break;
        case 4:
            profileName = getString(R.string.preset_1);
            break;
        case 5:
            profileName = getString(R.string.preset_2);
            break;
        default:
            break;
        }
        try {
            if (tmpProfile >= Constants.PREDEFINED_PROFILE_COUNT) {
                final int cmf = DS1Application.getCustomModifyFlag(getActivity());
                if (Constants.PROFILE_CUSTOM1_INDEX == tmpProfile + 1) {
                    if (DS1Application.CUSTOM_1_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_1_NAME_MODIFIED)) {
                        modified |= true;
                        profileName = mDsClient.getProfileNames()[tmpProfile];
                    }
                } else if (Constants.PROFILE_CUSTOM2_INDEX == tmpProfile + 1) {
                    if (DS1Application.CUSTOM_2_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_2_NAME_MODIFIED)) {
                        modified |= true;
                        profileName = mDsClient.getProfileNames()[tmpProfile];
                    }
                }
            }
            
            if (getView() != null) {
                TextView tv = (TextView) getView().findViewById(R.id.presetName);
                if (tv != null) {
                    tv.setText(profileName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }

        if (getView() == null) {
            return;
        }
        ImageView v = (ImageView) getView().findViewById(R.id.revertButtonMain);
        if (v != null) {
        	//DS1SOC-509 Both MobileUI and TabletUI all should use revert_profile as the revert icon
        	/*
            if (!mMobileLayout) {
                v.setImageResource(tmpProfile < Constants.PREDEFINED_PROFILE_COUNT ? R.drawable.revert_profile : R.drawable.presetremove);

            } else {
                v.setImageResource(R.drawable.revert_profile);
            }
            */
        	v.setImageResource(R.drawable.revert_profile);

            v.setVisibility(modified ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void cancelPendingEdition() {
        endEditingProfileName(true);
    }

    private void onDolbyClientUseClick(View view) {
        if (!mDolbyClientConnected || !mFObserver.useDsApiOnUiEvent()) {
            return;
        }

		// 1. get current custom modify status.
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

        if (R.id.revertButtonMain == id) { // Revert icon in the portrait
                                           // layout.
            // Doing local reset (nothing really left to do).

            // Notifying observer that the given profile was reset and that
            // the "event" should be propagated.
            mSpecificObserver.profileReset(selectedProfile);
        }
    }

    private void startEditingProfileName(TextView text, final EditText edit,
            int position) {
        endEditingProfileName(true);

        if ((position > Constants.PREDEFINED_PROFILE_COUNT) && (text != null) && (edit != null)) {
            edit.setFilters(new InputFilter[] { new InputFilter.LengthFilter(14) });
            edit.setTypeface(Assets.getFont(Assets.FontType.REGULAR));
            edit.setText(text.getText());
            text.setVisibility(View.INVISIBLE);
            edit.setVisibility(View.VISIBLE);
            edit.setOnEditorActionListener(this);
            edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            edit.setOnKeyListener(this);
            Tools.showVirtualKeyboard(getActivity());
            if (edit.isInTouchMode()) {
                edit.requestFocusFromTouch();
            } else {
                edit.requestFocus();
            }
            edit.setSelection(0, edit.getText().length());

            mCurrentlyEditedProfile = new ProfileEditInfo(position - 1, text, edit);
            mSpecificObserver.onProfileNameEditStarted();
        }
    }

    private void endEditingProfileName(boolean accept) {
        if (mCurrentlyEditedProfile != null) {
            if (accept) {
		boolean bModified_Custom1 = false;
		boolean bModified_Custom2 = false;

		// DS1SOC-677 <<<begin>>>
		// CONSUMER UI: Profile name "Custom 1" and "Custom 2" can not be modified at the same time
		final int cmf = DS1Application.getCustomModifyFlag(getActivity());
		if (DS1Application.CUSTOM_1_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_1_NAME_MODIFIED)) {
			bModified_Custom1 = true;
		}

		if (DS1Application.CUSTOM_2_NAME_MODIFIED == (cmf & DS1Application.CUSTOM_2_NAME_MODIFIED)) {
			bModified_Custom2 = true;
		}
		// DS1SOC-677 <<<end>>>

                String newName = mCurrentlyEditedProfile.mEditText.getText().toString();
                if (!newName.isEmpty()) {

		// change the custom modify status, then save it.
		if (mCurrentlyEditedProfile.mPosition + 1 == Constants.PROFILE_CUSTOM1_INDEX) {
			if (newName.equals(getActivity().getString(R.string.preset_1))) {
				bModified_Custom1= false;
			} else {
				bModified_Custom1= true;
			}
		} else if (mCurrentlyEditedProfile.mPosition + 1 == Constants.PROFILE_CUSTOM2_INDEX) {
			if (newName.equals(getActivity().getString(R.string.preset_2))) {
				bModified_Custom2= false;
			} else {
				bModified_Custom2= true;
			}
		}
		DS1Application.saveCustomNameModifiedStatus(getActivity(), bModified_Custom1, bModified_Custom2);

                    mCurrentlyEditedProfile.mTextView.setText(newName);
                    try {
                        mDsClient.setProfileName(mCurrentlyEditedProfile.mPosition, newName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mFObserver.onDsApiError();
                        return;
                    }
                }
            }
            Tools.hideVirtualKeyboard(getActivity());
            mCurrentlyEditedProfile.mEditText.setOnEditorActionListener(null);
            mCurrentlyEditedProfile.mEditText.setOnKeyListener(null);
            mCurrentlyEditedProfile.mEditText.setVisibility(View.GONE);
            mCurrentlyEditedProfile.mTextView.setVisibility(View.VISIBLE);
            mCurrentlyEditedProfile = null;
        }
        mSpecificObserver.onProfileNameEditEnded();
        setResetProfileVisibility();
    }
}
