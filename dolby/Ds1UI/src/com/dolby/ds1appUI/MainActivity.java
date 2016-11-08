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
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.dolby.IDsClientEvents;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tag;
import com.dolby.ds1appCoreUI.Tools;

public class MainActivity extends Activity implements OnClickListener,
        IDsClientEvents, IDsFragSwitchesObserver, IDsFragPowerObserver,
        IDsFragGraphicVisualizerObserver, IDsFragObserver,
        IDsFragProfilePresetsObserver, IDsFragProfileEditorObserver,
        IDsFragEqualizerPresetsObserver, IDsActivityCommonTemp {

    // Dolby Digital logo reference.
    private ImageView mDSLogo;

    // Dolby Surround Service Client stuff.
    private final DsClient mDsClient = new DsClient();
    private boolean mDolbyClientConnected = false;

    // Tooltip stuff?
    private ViewGroup mNativeRootContainer;

    // Splash screen stuff.
    private Dialog mSplashScreenDialog;
    private final int mSplashScreenDelayTime = 3000;
    private boolean mSplashTimerElapsed = false;
    private boolean mSplashClientBound = false;
    private Runnable mSplashScreenDelay = null;

    // ????
    private static long mOnDestroyTimer;

    // Flag indicating whether modifier part of DS API is to be used on UI
    // event. Setting it to false prevents from looping DS API calls.
    private boolean mUseDsApiOnUiEvent = true;

    // Application configuration.
    private static com.dolby.ds1appCoreUI.Configuration configuration;

    // In Store demo stuff.
    // Menu ID for in store demo launch
    private static final int INSTORE_MENU_ID = 1001;
    public static final String ACTION_LAUNCH_DS1_INSTOREDEMO_APP = "com.dolby.LAUNCH_DS1_INSTOREDEMO_APP";

    // Controls whether the visualizer has been registered with the DsClient or
    // not.
    private boolean mVisualizerRegistered = false;

    // Fragments references, since we add them dynamically.
    private FragProfilePresetEditor mFPPE = null;
    private FragProfilePresets mFPP = null;
    private FragSwitches mFS = null;
    private FragEqualizerPresets mFEP = null;
    // For mobile-landscape mode only.
    private LinearLayout mLinearLayout;
    private ScrollView mScrollview;
    private final int DYNAMIC_LINEAR_LAYOUT_ID = 8; // TOTALLY RANDOM NUMBER.

    private boolean mMobileLayout = false;
    static private boolean mEditProfile = false;

    private int mOriginX;
    private int mOriginY;

    private boolean mIsScreenOn = false;
	private boolean mIsActivityRunning = false; 

    private boolean mIsMonoSpeaker = false;
    public boolean isMonoSpeaker() {
        return mIsMonoSpeaker;
    }

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(Tag.MAIN, "ACTION_SCREEN_OFF");
                mIsScreenOn = false;
                registerVisualizer(mIsScreenOn);
                if (gv != null) {
                    gv.setEnabled(mIsScreenOn);
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(Tag.MAIN, "ACTION_SCREEN_ON");
                mIsScreenOn = true;
                if (gv != null) {
                    gv.setEnabled(mIsScreenOn);
                }
                registerVisualizer(mIsScreenOn);
            }
        }
    };

    // //////////////////////////////////////////////////////////////////////////////////

    // Called when the activity is first created.
    // Step 1.
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Debug statement.
        ((DS1Application) getApplication()).printScreenSpecs();
        // Loading appropriate Status Bar Height.
        Constants.STATUS_BAR_HEIGHT = getResources().getInteger(R.integer.statusbar_height);
        // Calling base class onCreate.
        super.onCreate(savedInstanceState);
        changeScale();
        // Loading type font.
        Assets.init(this);
        // Requesting window feature: no title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Registering for screen events.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);

        // Checking current screen state.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mIsScreenOn = pm.isScreenOn();

        final Runnable showMainUi = new Runnable() {
            @Override
            public void run() {
                doInitMainUI();
            }
        };

        // If splash screen is to be displayed now, then postpone
        // initialization of main UI to next main thread loop
        // iteration. Otherwise main UI is visible before splash screen
        // for a while.
        if (displaySplashScreen()) {
            DS1Application.HANDLER.postDelayed(showMainUi, mSplashScreenDelayTime);
        } else {
            showMainUi.run();
        }
    }

    public void changeScale() {
        Configuration sys = getBaseContext().getResources().getConfiguration();
        if (sys.smallestScreenWidthDp >= 480) {
            Configuration conf = new Configuration();
            conf.fontScale = sys.smallestScreenWidthDp / 800.0f;
            
            getBaseContext().getResources().updateConfiguration(conf, null);
            return;
        }
        if (sys.smallestScreenWidthDp >= 360) {
            Configuration conf = new Configuration();
            conf.fontScale = sys.smallestScreenWidthDp / 360.0f;
            
            getBaseContext().getResources().updateConfiguration(conf, null);
        }
    }

    // Step 2, typically.
    private boolean displaySplashScreen() {
        boolean isOrientationChange = mOnDestroyTimer > 0 && (mOnDestroyTimer + 500) > SystemClock.elapsedRealtime();

        if (!isOrientationChange) {
            mSplashScreenDialog = new Dialog(this, R.layout.splash_screen);
            mSplashScreenDialog.setContentView(R.layout.splash_screen);
            mSplashScreenDialog.setCancelable(false);
            mSplashScreenDialog.show();
            mSplashScreenDelay = new Runnable() {
                @Override
                public void run() {
                    mSplashTimerElapsed = true;
                    DS1Application.HANDLER.removeCallbacks(this);
                    hideSplashScreen();
                }
            };
            DS1Application.HANDLER.postDelayed(mSplashScreenDelay, mSplashScreenDelayTime);
            return true;
        }
        return false;
    }

    // Step 3, typically.
    private void hideSplashScreen() {
        if (mSplashTimerElapsed && mSplashClientBound) {
            // Second possible way of fixing below:
            // http://bend-ing.blogspot.mx/2008/11/properly-handle-progress-dialog-in.html
            // Might not be required due to our application.
            try {
                mSplashScreenDialog.dismiss();
            } catch (Exception e) {
                // NOTHING.
            }
            mSplashScreenDialog = null;
            mSplashScreenDelay = null;
        }
    }

    /**
     * This method contains what usually is called inside onCreate(...).
     * Invocation of this is postponed on purpose to measure actual screen
     * resolution.
     */
    // Step 4.
    private void doInitMainUI() {
        try {
            setContentView(R.layout.main);
        } catch (Exception e) {
            // NOTHING.
            return;
        }

        // Setting onClick listener for Dolby Digital logo
        // (drawable-large/dslogo.png).
        mDSLogo = (ImageView) findViewById(R.id.dsLogo);
        mDSLogo.setOnClickListener(this);
        mDSLogo.setSoundEffectsEnabled(false);

        // Related to tooltip display.
        mNativeRootContainer = ViewTools.determineNativeViewContainer(this);

        // We'll listen for Dolby Client Events, not visualizer events.
        // Those will be listened directly by the visualizer.
        mDsClient.setEventListener(this);
        // Cleaning up settings cache.
        DsClientCache.INSTANCE.reset();

        // Binding to remote service.
        Log.d(Tag.MAIN, "doInitMainUI - mDsClient.bindDsService");
        mDsClient.bindDsService(this);

        if (configuration == null) {
            configuration = com.dolby.ds1appCoreUI.Configuration.getInstance(getApplicationContext());
            Log.i(Tag.MAIN, "doInitMainUI - NEW CONFIG:" + configuration.getMaxEditGain() + " : " + configuration.getMinEditGain());
        }

        // Finding out if using mobile layout.
        mMobileLayout = getResources().getBoolean(R.bool.newLayout);

        if (mMobileLayout == true) {
            // Allocating for the moment only this fragment.
            // The rest will be allocated when required.
            mFPP = new FragProfilePresets();
            // Plugging in the FragProfilePresets fragment.
            // We have to. Otherwise, won't work.
            getFragmentManager().beginTransaction().add(R.id.fragmentcontainer, mFPP).commit();
        }
        if (mEditProfile) {
            this.editProfile();
        }
    }

    private void displayTooltip(View pointToView, CharSequence title,
            CharSequence text) {
        if (mNativeRootContainer == null || pointToView == null) {
            return;
        }
        ViewTools.showTooltip(MainActivity.this, mNativeRootContainer, pointToView, title, text);
    }

    @Override
    protected void onResume() {
        super.onResume();
		mIsActivityRunning = true;
        onDsClientUseChanged(true);
    }

    public static com.dolby.ds1appCoreUI.Configuration getConfiguration() {
        return configuration;
    }

    @Override
    protected void onPause() {
        onDsClientUseChanged(false);
        mIsActivityRunning = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOnDestroyTimer = SystemClock.elapsedRealtime();
        if (mSplashScreenDelay != null) {
            DS1Application.HANDLER.removeCallbacks(mSplashScreenDelay);
            hideSplashScreen();
        }
        unbindFromDsApi();
        configuration = null;
        unregisterReceiver(mScreenReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AssetFileDescriptor demoAfd = getResources().openRawResourceFd(R.raw.instore_demo_media);
        AssetFileDescriptor loopAfd = getResources().openRawResourceFd(R.raw.instore_demo_loop);
        if ((demoAfd != null) && (demoAfd.getLength() > 0) && (loopAfd != null) && (loopAfd.getLength() > 0)) {
            menu.add(0, INSTORE_MENU_ID, 0, R.string.instore_menu_text);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == INSTORE_MENU_ID) {
            startActivity(new Intent(ACTION_LAUNCH_DS1_INSTOREDEMO_APP));
        }
        return super.onOptionsItemSelected(item);
    }

    private void unbindFromDsApi() {
        if (mDolbyClientConnected) {
            mDolbyClientConnected = false;
            mDsClient.setEventListener(null);
            Log.d(Tag.MAIN, "MainActivity.unBindDsService");
            mDsClient.unBindDsService(this);
            DsClientCache.INSTANCE.reset();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // turn off the dithering and use full 24-bit+alpha graphics
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    @Override
    public void onDsClientUseChanged(final boolean on) {
        if (on) {
            if (mDolbyClientConnected) {
                FragGraphicVisualizer fgv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
                if (fgv != null) {
                    fgv.updateGraphicEqInUI();
                }

                // Shadowing behind FragGraphicVisualizer calls. Only in mobile
                // layout. This fragment shouldn't be used in the tablet for the
                // moment.
                // The tablet should only use FragGraphicVisualizer.
                // The reason for this is: FragGraphicVisualizer contains
                // FragEqualizerPresets in it. That functionality shall be
                // removed and layouts supplied so that FragEqualizerPresets can
                // be used too in the tablet.
                if (mMobileLayout == true && mFEP != null) {
                    mFEP.updateGraphicEqInUI();
                }

                // ELSE!
                boolean dsOn = DsClientCache.INSTANCE.isDsOn();
                internalOnDsOn(dsOn);
            }
        } else {
            if (mDolbyClientConnected) {
                registerVisualizer(false);
            }
        }
    }

    @Override
    public void chooseProfile(int profile) {
        try {
            if (DsClientCache.INSTANCE.getSelectedProfile(mDsClient) != profile) {
                DsClientCache.INSTANCE.setSelectedProfile(mDsClient, profile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        if (mIsMonoSpeaker) {
            try {
                mDsClient.getProfileSettings(profile).setSpeakerVirtualizerOn(false);
            } catch (Exception e) {
                e.printStackTrace();
                onDsApiError();
                return;
            }
        }

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            String profileName = pp.getItemName(profile);
            pp.setSelection(profile);

            FragProfilePresetEditor pe;
            if (mMobileLayout == true) {
                pe = mFPPE;
            } else {
                pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
            }
            if (pe != null) {
                // Telling to cancel any text edition.
                // Has to be done here to avoid a stack overflow :S if called
                // inside pe.onProfileNameChangedor just before. STRANGE.
                pe.cancelPendingEdition();

            }

            onProfileNameChanged(profile, profileName);
        }

        mUseDsApiOnUiEvent = false;
        onProfileSettingsChanged(profile, null);
        mUseDsApiOnUiEvent = true;
    }

    /**
     * Set selected preset in the service, update EQ
     * 
     * @param preset
     *            number of the preset selected in UI.
     * 
     *            Note: UI assumes EQUALIZER_SETTING_CUSTOM = -1 as the custom
     *            preset (default), predefined presets = 0..5.
     * 
     *            Service assumes custom preset idx = 0, predefided presets =
     *            1..6.
     */

    @Override
    public void onClick(View view) {
        final int id = view.getId();

        if (R.id.dsLogo == id) {
            FragGraphicVisualizer fgv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (fgv != null) {
                fgv.hideEqualizer();
            }
        } else {
            onDolbyClientUseClick(view);
        }
    }

    private void onDolbyClientUseClick(View view) {
        if (!mDolbyClientConnected || !mUseDsApiOnUiEvent) {
            return;
        }
    }

    public void powerOnOff(boolean on) {

        FragPower pwv = (FragPower) getFragmentManager().findFragmentById(R.id.fragpower);
        if (pwv != null) {
            pwv.setEnabled(on);
        }

        mDSLogo.setImageResource(on ? R.drawable.dslogo : R.drawable.dslogodis);

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            if (this.mFEP == null && this.mFPPE == null && this.mFS == null) {
                pp = mFPP;
            } else {
                pp = null;
            }
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            pp.setEnabled(on);
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            pe.setEnabled(on);
        }

        FragSwitches swv;
        if (mMobileLayout == true) {
            swv = mFS;
        } else {
            swv = (FragSwitches) getFragmentManager().findFragmentById(R.id.fragswitches);
        }
        if (swv != null) {
            swv.setEnabled(on);
        }

        if (mIsScreenOn) {
            FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (gv != null) {
                gv.setEnabled(on);
            }
        }

        // Shadowing behind FragGraphicVisualizer calls. Only in mobile
        // layout. This fragment shouldn't be used in the tablet for the
        // moment.
        // The tablet should only use FragGraphicVisualizer.
        // The reason for this is: FragGraphicVisualizer contains
        // FragEqualizerPresets in it. That functionality shall be
        // removed and layouts supplied so that FragEqualizerPresets can
        // be used too in the tablet.
        if (mMobileLayout == true && mFEP != null) {
            mFEP.setEnabled(on);
        }
    }

    @Override
    public void onDsOn(boolean on) {
        final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();
        if (cacheOn == on) {
            return;
        }

        DsClientCache.INSTANCE.cacheDsOn(on);

        mUseDsApiOnUiEvent = false;
        internalOnDsOn(on);
        mUseDsApiOnUiEvent = true;
    }

    private void internalOnDsOn(boolean on) {
        powerOnOff(on);
        if (on) {
            final int profile;
            try {
                profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
            } catch (Exception e) {
                e.printStackTrace();
                onDsApiError();
                return;
            }
            chooseProfile(profile);
            registerVisualizer(true);
        } else {
            registerVisualizer(false);
        }

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            pp.scheduleNotifyDataSetChanged();
        }
    }

    private void registerVisualizer(boolean on) {
        if (!mDolbyClientConnected || mVisualizerRegistered == on) {
            return;
        }

        if (mIsScreenOn == true && mIsActivityRunning == true) {

            mVisualizerRegistered = on;

            FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (gv != null) {
                gv.registerVisualizer(on);
            }
        }
    }

    @Override
    public void onProfileSelected(int profile) {
        if (!mDolbyClientConnected) {
            return;
        }

        DsClientCache.INSTANCE.cacheSelectedProfile(profile);

        final boolean dsOn = DsClientCache.INSTANCE.isDsOn();
        internalOnDsOn(dsOn);
    }

    @Override
    public void onProfileSettingsChanged(int profile) {
        mUseDsApiOnUiEvent = false;
        onProfileSettingsChanged(profile, null);
        mUseDsApiOnUiEvent = true;
    }

    @Override
    public DsClient getDsClient() {
        return mDsClient;
    }

    @Override
    public void onProfileSettingsChanged(int profile, DsClientSettings settings) {
        Log.d(Tag.MAIN, "onProfileSettingsChanged " + profile);

        if (settings == null) {
            try {
                settings = mDsClient.getProfileSettings(profile);
            } catch (Exception e) {
                e.printStackTrace();
                onDsApiError();
                return;
            }
        }

        try {
            DsClientCache.INSTANCE.cacheProfileSettings(mDsClient, profile, settings);
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        final int selectedProfile;
        try {
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        if (profile == selectedProfile) {

            FragProfilePresets pp;
            if (mMobileLayout == true) {
                pp = mFPP;
            } else {
                pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
            }
            if (pp != null) {
                pp.scheduleNotifyDataSetChanged();
            }

            FragProfilePresetEditor pe;
            if (mMobileLayout == true) {
                pe = mFPPE;
            } else {
                pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
            }
            if (pe != null) {
                pe.setResetProfileVisibility();
            }

            // Set up the toggle buttons.
            FragSwitches swv;
            if (mMobileLayout == true) {
                swv = mFS;
            } else {
                swv = (FragSwitches) getFragmentManager().findFragmentById(R.id.fragswitches);
            }
            if (swv != null) {
                swv.onProfileSettingsChanged(settings);
            }

            FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (gv != null) {
                // Show or hide the equalizer Reset Button.
                gv.setResetEqButtonVisibility();
            }

            int iEqPreset;
            try {
                iEqPreset = mDsClient.getIeqPreset(profile);
            } catch (Exception e) {
                e.printStackTrace();
                onDsApiError();
                return;
            }

            if (gv != null) {
                gv.selectIEqPresetInUI(iEqPreset - 1);
            }

            // Shadowing behind FragGraphicVisualizer calls. Only in mobile
            // layout. This fragment shouldn't be used in the tablet for the
            // moment.
            // The tablet should only use FragGraphicVisualizer.
            // The reason for this is: FragGraphicVisualizer contains
            // FragEqualizerPresets in it. That functionality shall be
            // removed and layouts supplied so that FragEqualizerPresets can
            // be used too in the tablet.
            if (mMobileLayout == true && mFEP != null) {
                mFEP.setResetEqButtonVisibility();
                mFEP.selectIEqPresetInUI(iEqPreset - 1);
            }
        }
    }

    @Override
    public void setUserProfilePopulated() {
        final int profile;
        FragProfilePresets pp;

        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            profile = pp.getSelection();

            if (profile < Constants.PREDEFINED_PROFILE_COUNT) {
                return;
            }

            try {
                String name = mDsClient.getProfileNames()[profile];
                if (name != null) {
                    return;
                }
                mDsClient.setProfileName(profile, pp.getDefaultProfileName(profile));
            } catch (Exception e) {
                e.printStackTrace();
                onDsApiError();
                return;
            }
        }
    }

    @Override
    public void displayTooltip(View pointToView, int idTitle, int idText) {
        displayTooltip(pointToView, getString(idTitle), getString(idText));
    }

    @Override
    public void onDsApiError() {
        unbindFromDsApi();
        finish();
    }

    /**
     * Called on clients in response to the name of a profile being changed
     * (i.e. setProfileName method call). Clients not interested in name changes
     * (because their GUI does not display the names, such as a widget) can
     * simply ignore this event.
     * 
     * @param profile
     *            Index of profile whose name changed.
     * @param name
     *            Specifies the new name for the profile.
     * 
     * @see com.dolby.ds1appCoreUI.client.IDsClientEvents#onProfileNameChanged(int,
     *      java.lang.String)
     */
    @Override
    public void onProfileNameChanged(int profile, String name) {
        // Letting know the fragments about the event, so they can update
        // internal state too.
        FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (gv != null) {
            ((IDsClientEvents) gv).onProfileNameChanged(profile, name);
        }

        // Shadowing behind FragGraphicVisualizer calls. Only in mobile
        // layout. This fragment shouldn't be used in the tablet for the
        // moment.
        // The tablet should only use FragGraphicVisualizer.
        // The reason for this is: FragGraphicVisualizer contains
        // FragEqualizerPresets in it. That functionality shall be
        // removed and layouts supplied so that FragEqualizerPresets can
        // be used too in the tablet.
        if (mMobileLayout == true && mFEP != null) {
            ((IDsClientEvents) mFEP).onProfileNameChanged(profile, name);
        }

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            ((IDsClientEvents) pp).onProfileNameChanged(profile, name);
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            ((IDsClientEvents) pe).onProfileNameChanged(profile, name);
        }
    }

    // From IDsClientEvents.

    // @see IDsClientEvents#onClientConnected()
    // Step 7.
    @Override
    public void onClientConnected() {
        // Handling connection event locally, first.

        // Setting local variables.
        // Client has connected.
        mDolbyClientConnected = true;
        mSplashClientBound = true;

        try {
            mIsMonoSpeaker = mDsClient.isMonoSpeaker();
            Log.d(Tag.MAIN, "mIsMonoSpeaker = " + mIsMonoSpeaker);
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        // Hiding splash screen.
        hideSplashScreen();

        // Caching the Dolby Service status: on or off.
        // try... catch'ing just because
        // ::getDsOn can throw an exception, but it shouldn't
        // since it should have already connected (look the method we are at).
        try {
            DsClientCache.INSTANCE.cacheDsOn(mDsClient.getDsOn());
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        // Local Variables Set.
        // Letting know the fragments about the connection, so they can set up
        // internal state too.
        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            ((IDsClientEvents) pp).onClientConnected();
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            ((IDsClientEvents) pe).onClientConnected();
        }

        FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (gv != null) {
            ((IDsClientEvents) gv).onClientConnected();
        }

        // Shadowing behind FragGraphicVisualizer calls. Only in mobile
        // layout. This fragment shouldn't be used in the tablet for the
        // moment.
        // The tablet should only use FragGraphicVisualizer.
        // The reason for this is: FragGraphicVisualizer contains
        // FragEqualizerPresets in it. That functionality shall be
        // removed and layouts supplied so that FragEqualizerPresets can
        // be used too in the tablet.
        if (mMobileLayout == true && mFEP != null) {
            ((IDsClientEvents) mFEP).onClientConnected();
        }

        // Fragments informed.
        // Continuing with initialization.
        mUseDsApiOnUiEvent = false;
        onDsClientUseChanged(true);
        mUseDsApiOnUiEvent = true;
    }

    // @see IDsClientEvents#onClientDisconnected().
    @Override
    public void onClientDisconnected() {

        // Letting know the fragments about the disconnection, so they can set
        // up internal state too.
        FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (gv != null) {
            ((IDsClientEvents) gv).onClientDisconnected();
        }

        // Shadowing behind FragGraphicVisualizer calls. Only in mobile
        // layout. This fragment shouldn't be used in the tablet for the
        // moment.
        // The tablet should only use FragGraphicVisualizer.
        // The reason for this is: FragGraphicVisualizer contains
        // FragEqualizerPresets in it. That functionality shall be
        // removed and layouts supplied so that FragEqualizerPresets can
        // be used too in the tablet.
        if (mMobileLayout == true && mFEP != null) {
            ((IDsClientEvents) mFEP).onClientDisconnected();
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            ((IDsClientEvents) pe).onClientDisconnected();
        }

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            ((IDsClientEvents) pp).onClientDisconnected();
        }

        // Handling disconnection event locally now.
        mDolbyClientConnected = false;
        // Continuing with deinitialization.
        onDsClientUseChanged(false);
    }

    // From IDsFragGraphicVisualizerObserver.
    @Override
    public void exitActivity() {
        if (mDolbyClientConnected) {
            mDolbyClientConnected = false;
            onDsClientUseChanged(false);
            mDsClient.setEventListener(null);
            Log.d(Tag.MAIN, "MainActivity.unBindDsService");
            mDsClient.unBindDsService(this);
        }
        finish();
    }

    // From IDsFragObserver.
    @Override
    public boolean isDolbyClientConnected() {
        return mDolbyClientConnected;
    }

    @Override
    public boolean useDsApiOnUiEvent() {
        return mUseDsApiOnUiEvent;
    }

    @Override
    public void profileReset(int profile) {
        FragGraphicVisualizer fgv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (fgv != null) {
            fgv.resetUserGains(profile);
        }

        chooseProfile(profile);
    }

    // From IDsFragGraphicVisualizerObserver.
    @Override
    public void onEqualizerEditStart() {

        // Have to call this here so that the shadow fragment
        // can reset properly the EQ reset button.
        if (mMobileLayout == true && mFEP != null) {
            mFEP.setResetEqButtonVisibility();
        }

        setUserProfilePopulated();

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            pp.scheduleNotifyDataSetChanged();
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            pe.setResetProfileVisibility();
        }
    }

    // From IDsFragPresetEditorObserver.
    // Method to catch "event" from profile adapter.
    @Override
    public void onProfileNameEditStarted() {

        FragProfilePresets pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        if (pp != null) {
            pp.onProfileNameEditStarted();
        }
    }

    // From IDsFragPresetEditorObserver.
    // Method to catch "event" from profile adapter.
    @Override
    public void onProfileNameEditEnded() {

    }

    // From IDsFragPresetEditorObserver.
    @Override
    public int getProfileSelected() {
        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            return pp.getSelection();
        }
        return -1;
    }

    @Override
    public void onEqSettingsChanged(int profile, int preset) {
        Log.d(Tag.MAIN, "onEqSettingsChanged " + profile);

        // Letting fragments handle the event.
        FragGraphicVisualizer fgv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (fgv != null) {
            ((IDsClientEvents) fgv).onEqSettingsChanged(profile, preset);
        }

        // Shadowing behind FragGraphicVisualizer calls. Only in mobile
        // layout. This fragment shouldn't be used in the tablet for the
        // moment.
        // The tablet should only use FragGraphicVisualizer.
        // The reason for this is: FragGraphicVisualizer contains
        // FragEqualizerPresets in it. That functionality shall be
        // removed and layouts supplied so that FragEqualizerPresets can
        // be used too in the tablet.
        if (mMobileLayout == true && mFEP != null) {
            ((IDsClientEvents) mFEP).onEqSettingsChanged(profile, preset);
        }

        FragProfilePresets pp;
        if (mMobileLayout == true) {
            pp = mFPP;
        } else {
            pp = (FragProfilePresets) getFragmentManager().findFragmentById(R.id.fragprofilepresets);
        }
        if (pp != null) {
            ((IDsClientEvents) pp).onEqSettingsChanged(profile, preset);
        }

        FragProfilePresetEditor pe;
        if (mMobileLayout == true) {
            pe = mFPPE;
        } else {
            pe = (FragProfilePresetEditor) getFragmentManager().findFragmentById(R.id.fragprofileeditor);
        }
        if (pe != null) {
            ((IDsClientEvents) pe).onEqSettingsChanged(profile, preset);
        }

        DsClientSettings settings;
        final int selectedProfile;
        try {
            settings = mDsClient.getProfileSettings(profile);
            DsClientCache.INSTANCE.cacheProfileSettings(mDsClient, profile, settings);
            selectedProfile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
        } catch (Exception e) {
            e.printStackTrace();
            onDsApiError();
            return;
        }

        if (profile == selectedProfile) {
            // Set up the toggle buttons.
            // TODO refactor like above.
            FragSwitches swv;
            if (mMobileLayout == true) {
                swv = mFS;
            } else {
                swv = (FragSwitches) getFragmentManager().findFragmentById(R.id.fragswitches);
            }
            if (swv != null) {
                swv.onProfileSettingsChanged(settings);
            }
        }
    }

    @Override
    public void editProfile() {

        if (mMobileLayout == true) {
            // Only available when on. When off, this shouldn't be possible.
            final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();
            if (cacheOn == false) {
                return;
            }

            // Profile edition is requested.
            // Switching fragments only if in mobile layout and fragments are
            // null.
            if (mFS == null && mFPPE == null && mFEP == null) {
                mEditProfile = true;
                FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
                if (gv != null) {
                    gv.setEnableEditGraphic(true);
                }
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                fragmentTransaction.remove(mFPP);
                fragmentTransaction.commit();
                fragmentManager.executePendingTransactions();

                mFPPE = new FragProfilePresetEditor();
                mFS = new FragSwitches();
                mFEP = new FragEqualizerPresets();

                int fragmentContainerId = R.id.fragmentcontainer;

                if (Tools.isLandscapeScreenOrientation(this) == true) {

                    mLinearLayout = new LinearLayout(this);
                    mLinearLayout.setId(DYNAMIC_LINEAR_LAYOUT_ID);
                    mLinearLayout.setOrientation(LinearLayout.VERTICAL);
                    mScrollview = new ScrollView(this);
                    mScrollview.setId(R.id.thescrollview);

                    mScrollview.addView(mLinearLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    ((LinearLayout) findViewById(R.id.fragmentcontainer)).addView(mScrollview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    fragmentContainerId = DYNAMIC_LINEAR_LAYOUT_ID;
                }

                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                fragmentTransaction.add(R.id.preseteditorcontainer, mFPPE);
                fragmentTransaction.add(fragmentContainerId, mFS);
                fragmentTransaction.add(fragmentContainerId, mFEP);
                fragmentTransaction.commit();
                fragmentManager.executePendingTransactions();
                ScrollView theView = (ScrollView) findViewById(R.id.thescrollview);
                if (theView != null) {

                    // Only doing this in portrait mode, where the
                    // scrollview remains alive always.
                    if (Tools.isLandscapeScreenOrientation(this) == false) {
                        mOriginX = theView.getScrollX();
                        mOriginY = theView.getScrollY();
                    }

                    theView.smoothScrollTo(0, 0);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (mMobileLayout == true && mFS != null && mFPPE != null && mFEP != null) {
            mEditProfile = false;
            FragGraphicVisualizer gv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
            if (gv != null) {
                gv.hideEqualizer();
                gv.setEnableEditGraphic(false);
            }
            // Doing this only if the right fragments are loaded while
            // using mobile layout.

            // Unloading "edition fragments" and restoring profile
            // selector fragment.
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            fragmentTransaction.remove(mFEP);
            fragmentTransaction.remove(mFPPE);
            fragmentTransaction.remove(mFS);
            fragmentTransaction.commit();
            fragmentManager.executePendingTransactions();
 
            // Letting the garbage collector get to them.
            mFPPE = null;
            mFS = null;
            mFEP = null;

            if (Tools.isLandscapeScreenOrientation(this) == true) {

                ((LinearLayout) findViewById(R.id.fragmentcontainer)).removeView(mScrollview);
                mLinearLayout = null;
                mScrollview = null;
            } else {
                ScrollView theView = (ScrollView) findViewById(R.id.thescrollview);
                // Restoring previous state.
                if (theView != null) {

                    theView.post(new Runnable() {
                        public void run() {
                            ScrollView theView = (ScrollView) findViewById(R.id.thescrollview);
                            if (theView != null) {
                                theView.scrollTo(mOriginX, mOriginY);
                            }
                        }
                    });
                }
            }

            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.add(R.id.fragmentcontainer, mFPP);
            fragmentTransaction.commit();
            fragmentManager.executePendingTransactions();
        } else {
            // Calling base class functionality.
            super.onBackPressed();
        }
        return;
    }

    @Override
    public void profileEditorIsAlive() {

        if (mMobileLayout == true) {
            // Only available when on. When off, this shouldn't be possible.
            final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();

            // If we are connected to the Dolby Service.
            if (mDolbyClientConnected == true && cacheOn == true) {
                // Propagating connected status.
                ((IDsClientEvents) mFPPE).onClientConnected();
                // Enabling fragment.
                mFPPE.setEnabled(true);

                // Extracting selected profile index.
                final int profile;
                try {
                    profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                } catch (Exception e) {
                    e.printStackTrace();
                    onDsApiError();
                    return;
                }
                // Setting selected profile name in Profile Editor.
                ((IDsClientEvents) mFPPE).onProfileNameChanged(profile, mFPP.getItemName(profile));
            }
        }
    }

    @Override
    public void switchesAreAlive() {
        if (mMobileLayout == true) {
            // Only available when on. When off, this shouldn't be possible.
            final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();

            // If we are connected to the Dolby Service.
            if (mDolbyClientConnected == true && cacheOn == true) {
                mFS.setEnabled(true);

                final int profile;
                DsClientSettings settings;
                try {
                    // Extracting selected profile index.
                    profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                    // Extracting associated settings.
                    settings = mDsClient.getProfileSettings(profile);
                    // Caching them.
                    DsClientCache.INSTANCE.cacheProfileSettings(mDsClient, profile, settings);
                } catch (Exception e) {
                    e.printStackTrace();
                    onDsApiError();
                    return;
                }

                // Set up the toggle buttons.
                FragSwitches swv;
                if (mMobileLayout == true) {
                    swv = mFS;
                } else {
                    swv = (FragSwitches) getFragmentManager().findFragmentById(R.id.fragswitches);
                }
                if (swv != null) {
                    swv.onProfileSettingsChanged(settings);
                }
            }
        }
    }

    @Override
    public void equalizerPresetsAreAlive() {

        if (mMobileLayout == true) {
            // Only available when on. When off, this shouldn't be possible.
            final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();

            // If we are connected to the Dolby Service.
            if (mDolbyClientConnected == true && cacheOn == true) {
                // Propagating connected status.
                ((IDsClientEvents) mFEP).onClientConnected();
                // Telling it to update visually speaking.
                mFEP.updateGraphicEqInUI();
                // Enabling fragment.
                mFEP.setEnabled(true);

                // Calling chooseProfile to handle everything else as part of
                // the
                // initialization. Should we initialize individually?
                // Might be more efficient.
                final int profile;
                try {
                    profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                } catch (Exception e) {
                    e.printStackTrace();
                    onDsApiError();
                    return;
                }
                chooseProfile(profile);
            }
        }
    }

    @Override
    public void profilePresetsAreAlive() {

        if (mMobileLayout == true) {
            // Only available when on. When off, this shouldn't be possible.
            final boolean cacheOn = DsClientCache.INSTANCE.isDsOn();
            this.mFPP.setEnabled(cacheOn);

            // If we are connected to the Dolby Service.
            if (mDolbyClientConnected == true && cacheOn == true) {
                // Required to highlight the profile currently active. :S
                // This even though the object itself is never destroyed.
                // Interestingly, the state is not saved.
                final int profile;
                try {
                    profile = DsClientCache.INSTANCE.getSelectedProfile(mDsClient);
                } catch (Exception e) {
                    e.printStackTrace();
                    onDsApiError();
                    return;
                }
                ListView lv = (ListView) this.findViewById(R.id.presetsListView);
                if (lv != null) {
                    lv.setSelection(profile);
                }
                chooseProfile(profile);

                ScrollView theView = (ScrollView) findViewById(R.id.thescrollview);
                if (theView != null) {
                    theView.post(new Runnable() {
                        public void run() {
                            ScrollView theView = (ScrollView) findViewById(R.id.thescrollview);
                            if (theView != null) {
                                theView.scrollTo(0, profile * theView.getHeight() / 6);
                            }
                        }
                    });

                }
            }
        }
    }

    @Override
    public void resetEqUserGains() {
        FragGraphicVisualizer fgv = (FragGraphicVisualizer) getFragmentManager().findFragmentById(R.id.fraggraphicvisualizer);
        if (fgv != null) {
            fgv.resetUserGains();
        }
    }
}
