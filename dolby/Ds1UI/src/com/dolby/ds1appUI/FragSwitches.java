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


import com.dolby.ds1appCoreUI.Tag;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class FragSwitches extends Fragment implements OnClickListener, OnLongClickListener {

    private ToggleButton msvButton;
    private ToggleButton mdeButton;
    private ToggleButton mvlButton;
    private TextView msvText;
    private TextView mdeText;
    private TextView mvlText;
    private ImageView mqm_sv;
    private ImageView mqm_de;
    private ImageView mqm_vl;
    private DsClient mDsClient;
    private Activity mActivity;

    // Our observers.
    // Specific Switches Fragment Observer.
    private IDsFragSwitchesObserver mSpecificObserver;
    // Generic Fragment Observer (error handling / generic provider).
    private IDsFragObserver mFObserver;

    private boolean mMobileLayout = false;
    int mHeadset_plug = 0;
    private int mA2dpConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle bun = intent.getExtras();
            int selectedProfile;

            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                mHeadset_plug = bun.getInt("state");
                DsClientSettings selectedProfileSettings = null;
                try {
                    selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                    selectedProfileSettings = DsClientCache.INSTANCE.getProfileSettings(mDsClient, selectedProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (selectedProfileSettings != null) {
                    msvButton.setChecked(getVirtualizer(selectedProfileSettings));
                    setEnabled(mdeButton.isEnabled());
                }
            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                mA2dpConnectionState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                //Log.d("FragSwitches", "onReceive, BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, mA2dpConnectionState = " + mA2dpConnectionState);
                DsClientSettings selectedProfileSettings = null;
                try {
                    selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                    selectedProfileSettings = DsClientCache.INSTANCE.getProfileSettings(mDsClient, selectedProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (selectedProfileSettings != null) {
                    msvButton.setChecked(getVirtualizer(selectedProfileSettings));
                    setEnabled(mdeButton.isEnabled());
                }
            }
        }
    };

    // From Fragment.
    // First method called by the framework.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
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
            mSpecificObserver = (IDsFragSwitchesObserver) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IDsFragSwitchesObserver");
        }

        // Activity supports both Interfaces!
        // Fetching DsClient instance.
        mDsClient = mFObserver.getDsClient();

        AudioManager am = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
        mA2dpConnectionState = am.isBluetoothA2dpOn() ? BluetoothProfile.STATE_CONNECTED : BluetoothProfile.STATE_DISCONNECTED;

        IntentFilter headsetFilter = new IntentFilter();
        headsetFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        headsetFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        mActivity.registerReceiver(mReceiver, headsetFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity.unregisterReceiver(mReceiver);
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
        View v = inflater.inflate(R.layout.fragswitches, container, false);
        // Finding out if using mobile layout.
        mMobileLayout = getResources().getBoolean(R.bool.newLayout);
        final int textIds[] = { R.id.svText, R.id.deText, R.id.vlText };
        for (int id : textIds) {
            TextView tv = (TextView) v.findViewById(id);
            if (tv != null) {
                if (mMobileLayout) {
                    tv.setTypeface(Assets.getFont(Assets.FontType.LIGHT));
                } else {
                    tv.setTypeface(Assets.getFont(Assets.FontType.REGULAR));
                }
            }
        }

        final ToggleSlideListener slideListener = new ToggleSlideListener();

        int buttonids[] = { R.id.svButton, R.id.deButton, R.id.vlButton };
        View v2;
        for (int bid : buttonids) {
            v2 = v.findViewById(bid);
            if (v2 != null) {
                v2.setOnClickListener(this);
                v2.setSoundEffectsEnabled(false);
                v2.setOnTouchListener(slideListener);
                if (mMobileLayout) {
                    v2.setOnLongClickListener(this);
                }
            }
        }

        mqm_sv = (ImageView) v.findViewById(R.id.qm_sv);
        mqm_de = (ImageView) v.findViewById(R.id.qm_de);
        mqm_vl = (ImageView) v.findViewById(R.id.qm_vl);
        msvButton = (ToggleButton) v.findViewById(R.id.svButton);
        mdeButton = (ToggleButton) v.findViewById(R.id.deButton);
        mvlButton = (ToggleButton) v.findViewById(R.id.vlButton);
        msvText = (TextView) v.findViewById(R.id.svText);
        mdeText = (TextView) v.findViewById(R.id.deText);
        mvlText = (TextView) v.findViewById(R.id.vlText);

        View viewobj[] = { mqm_sv, mqm_de, mqm_vl };
        for (View iv : viewobj) {
            if (iv != null) {
                iv.setOnClickListener(this);
                iv.setSoundEffectsEnabled(false);
            }
        }

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
            mSpecificObserver.switchesAreAlive();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        final int selectedProfile;
        final DsClientSettings selectedProfileSettings;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
            selectedProfileSettings = DsClientCache.INSTANCE.getProfileSettings(mDsClient, selectedProfile);

            switch (id) {
            case R.id.svButton:
                if ((mHeadset_plug == 0) && (mA2dpConnectionState != BluetoothProfile.STATE_CONNECTED) && (((MainActivity)mActivity).isMonoSpeaker())) {
                    msvButton.setChecked(false);
                    return;
                }
                updateSVButtonImage(msvText.isEnabled(), msvText.getLayoutDirection());
                msvButton.cancelLongPress();
                setVirtualizer(selectedProfileSettings, msvButton.isChecked());
                mSpecificObserver.setUserProfilePopulated();
                DsClientCache.INSTANCE.setProfileSettings(mDsClient, selectedProfile, selectedProfileSettings);
                mSpecificObserver.onProfileSettingsChanged(selectedProfile, selectedProfileSettings);
                break;
            case R.id.deButton:
                mdeButton.cancelLongPress();
                selectedProfileSettings.setDialogEnhancerOn(!selectedProfileSettings.getDialogEnhancerOn());
                mSpecificObserver.setUserProfilePopulated();
                DsClientCache.INSTANCE.setProfileSettings(mDsClient, selectedProfile, selectedProfileSettings);
                mSpecificObserver.onProfileSettingsChanged(selectedProfile, selectedProfileSettings);
                break;
            case R.id.vlButton:
                mvlButton.cancelLongPress();
                selectedProfileSettings.setVolumeLevellerOn(!selectedProfileSettings.getVolumeLevellerOn());
                mSpecificObserver.setUserProfilePopulated();
                DsClientCache.INSTANCE.setProfileSettings(mDsClient, selectedProfile, selectedProfileSettings);
                break;
            case R.id.qm_sv:
                mSpecificObserver.displayTooltip(view, R.string.tooltip_sv_title, R.string.tooltip_sv_text);
                break;
            case R.id.qm_de:
                mSpecificObserver.displayTooltip(view, R.string.tooltip_de_title, R.string.tooltip_de_text);
                break;
            case R.id.qm_vl:
                mSpecificObserver.displayTooltip(view, R.string.tooltip_vl_title, R.string.tooltip_vl_text);
                break;
            default:
                break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            mFObserver.onDsApiError();
            return;
        }
        mSpecificObserver.onProfileSettingsChanged(selectedProfile, selectedProfileSettings);
    }

    private void setVirtualizer(DsClientSettings selectedProfileSettings,
            boolean on) {
        boolean isMonoSpeaker = ((MainActivity)mActivity).isMonoSpeaker();
        if ((mA2dpConnectionState == BluetoothProfile.STATE_CONNECTED) || (mHeadset_plug == 1) || (!isMonoSpeaker)) {
            if (msvButton != null) {
                msvButton.setEnabled(true);
            }
        }
        /*
         * if (mA2dp_device == 1) {
         * selectedProfileSettings.setSpeakerVirtualizerOn(false);
         * selectedProfileSettings.setHeadphoneVirtualizerOn(on); return; } if
         * (mSCO_device == 1) {
         * selectedProfileSettings.setSpeakerVirtualizerOn(false);
         * selectedProfileSettings.setHeadphoneVirtualizerOn(false); return; }
         */
        if ((mHeadset_plug == 1) || (mA2dpConnectionState == BluetoothProfile.STATE_CONNECTED)) {
            // selectedProfileSettings.setSpeakerVirtualizerOn(false);
            selectedProfileSettings.setHeadphoneVirtualizerOn(on);
            return;
        }
        // selectedProfileSettings.setHeadphoneVirtualizerOn(false);
        if (isMonoSpeaker) {
            selectedProfileSettings.setSpeakerVirtualizerOn(false);
        } else {
            selectedProfileSettings.setSpeakerVirtualizerOn(on);
        }
    }

    private boolean getVirtualizer(DsClientSettings selectedProfileSettings) {
        if (selectedProfileSettings.getSpeakerVirtualizerOn() && (((MainActivity)mActivity).isMonoSpeaker())) {
            selectedProfileSettings.setSpeakerVirtualizerOn(false);
        }
        if ((mHeadset_plug == 1) || (mA2dpConnectionState == BluetoothProfile.STATE_CONNECTED)) {
            return selectedProfileSettings.getHeadphoneVirtualizerOn();
        }
        return selectedProfileSettings.getSpeakerVirtualizerOn();
    }

    public void onProfileSettingsChanged(DsClientSettings settings) {

        if (msvButton != null) {
            msvButton.setChecked(getVirtualizer(settings));
			updateSVButtonImage(msvText.isEnabled(), msvText.getLayoutDirection());
        }
        if (mdeButton != null) {
            mdeButton.setChecked(settings.getDialogEnhancerOn());
        }
        if (mvlButton != null) {
            mvlButton.setChecked(settings.getVolumeLevellerOn());
        }
    }

    public void updateSVButtonImage(boolean enabled, int direction)
    {
        if (enabled) {
        	if (this.msvButton.isChecked()) {
        		if (View.LAYOUT_DIRECTION_RTL == direction) {
        			msvButton.setBackgroundResource(R.drawable.switchon_ldrtl);
        		} else {
        			msvButton.setBackgroundResource(R.drawable.switchon);
        		}
        	} else {
        		if (View.LAYOUT_DIRECTION_RTL == direction) {
        			msvButton.setBackgroundResource(R.drawable.switchoff_ldrtl);
        		} else {
            		msvButton.setBackgroundResource(R.drawable.switchoff);
        		}
        	}
        } else {
    		if (View.LAYOUT_DIRECTION_RTL == direction) {
        		msvButton.setBackgroundResource(R.drawable.switchdis_ldrtl);
    		} else {
        		msvButton.setBackgroundResource(R.drawable.switchdis);
    		}
        }
    }

    public void setEnabled(boolean on) {
        if (msvButton != null) {
            msvText.setEnabled(on);
            updateSVButtonImage(msvText.isEnabled(), msvText.getLayoutDirection());	// msvButton.setEnabled(on);
            if ((mHeadset_plug == 0) && (mA2dpConnectionState != BluetoothProfile.STATE_CONNECTED) && (((MainActivity)mActivity).isMonoSpeaker())) {
				msvText.setEnabled(false);
                msvButton.setChecked(false);
                updateSVButtonImage(msvText.isEnabled(), msvText.getLayoutDirection());	// msvButton.setEnabled(false);
            }
        }
        View viewobj[] = { mdeButton, mvlButton, mdeText, mvlText, mqm_sv, mqm_de, mqm_vl };
        for (View iv : viewobj) {
            if (iv != null) {
                iv.setEnabled(on);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mMobileLayout) {
            return false;
        }
        DialogFragment diag = null;
        if(v == this.mvlButton){
            diag = TooltipDialog.newInstance(R.string.tooltip_vl_title, R.string.tooltip_vl_text);
        }
        if(v == this.mdeButton){
            diag = TooltipDialog.newInstance(R.string.tooltip_de_title, R.string.tooltip_de_text);
        }
        if(v == this.msvButton){
            diag = TooltipDialog.newInstance(R.string.tooltip_sv_title, R.string.tooltip_sv_text);
        }
        diag.show(getFragmentManager(), "TooltipDialog");

        return true;
    }
}
