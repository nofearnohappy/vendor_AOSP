package com.mediatek.phone.plugin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telephony.PhoneNumberUtils;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
//import com.android.phone.common.HapticFeedback;
//import com.android.phone.common.util.ViewUtil;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.phone.ext.DefaultEmergencyDialerExt;

import java.util.List;

/**
 * EmergencyDialer extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.phone.ext.IEmergencyDialerExt")
public class OP09EmergencyDialerExt extends DefaultEmergencyDialerExt
        implements View.OnClickListener {

    private static final String TAG = "OP09EmergencyDialerExt";
    private static final String ID = "id";
    private static final String ID_NAME_TOP = "top";
    private static final String BOOLEAN = "bool";
    private static final String ID_BUTTON_CONTAINER = "floating_action_button_container";
    private static final String BOOLEAN_KEY_VIBRATION = "config_enable_dialer_key_vibration";
    private static final String TEL_SERVICE = "TelephonyConnectionService";

    private static final int BAD_EMERGENCY_NUMBER_DIALOG = 0;
    private static final int SLOT1 = 0;
    private static final int SLOT2 = 1;

    private Context mContext;
    private Activity mActivity;
    private FrameLayout mDialButtonContainers;
    private View mContainerLeft;
    private View mContainerRight;
    private FrameLayout mFrameView;
    private ImageButton mDialButtonLeft;
    private ImageButton mDialButtonRight;
    private EditText mDigits;
    // Haptic feedback (vibration) for dialer key presses.
    //private HapticFeedback mHaptic = new HapticFeedback();
    private EmergencyDialerBroadcastReceiver mReceiver = new EmergencyDialerBroadcastReceiver();

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;

    public OP09EmergencyDialerExt(Context context) {
        mContext = context;
    }

    public void onCreate(Activity activity) {
        TelephonyManager telephonyManager = (TelephonyManager) activity.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getDefault().getPhoneCount() < 2) {
            return;
        }
        mActivity = activity;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mActivity.registerReceiver(mReceiver, intentFilter);

        Resources resource = mActivity.getResources();
        String packageName = mActivity.getPackageName();
        mFrameView =
                (FrameLayout) mActivity.findViewById(
                                resource.getIdentifier(ID_NAME_TOP, ID, packageName));
        FrameLayout dialButtonContainer =
                (FrameLayout) mActivity.findViewById(
                                resource.getIdentifier(ID_BUTTON_CONTAINER, ID, packageName));
        mDigits = (EditText) mFrameView.findViewById(resource.getIdentifier("digits",
                ID, packageName));
        log("dialButtonContainer = " + dialButtonContainer);
        if (null != dialButtonContainer) {
            int indexOfDialpadButtonContainer = mFrameView.indexOfChild(
                    dialButtonContainer);
            ViewGroup.LayoutParams dialButtonContainerLayoutParams =
                    (ViewGroup.LayoutParams) dialButtonContainer.getLayoutParams();
            mFrameView.removeView(dialButtonContainer);

            int dialContainerWidth = mContext.getResources().
                            getDimensionPixelSize(R.dimen.floating_action_buttons_width);
            log("indexOfDialpadButtonContainer = " + indexOfDialpadButtonContainer);
              dialButtonContainerLayoutParams.width = dialContainerWidth;
            LayoutInflater inflaterOfPlugin = LayoutInflater.from(mContext);
            mDialButtonContainers = (FrameLayout) inflaterOfPlugin.inflate(
                    R.layout.emergency_dialer_button_container, null);
            mDialButtonContainers.setId(
                    resource.getIdentifier(ID_BUTTON_CONTAINER, ID, packageName));
            mContainerLeft =
                    mDialButtonContainers.findViewById(
                        R.id.dialpad_floating_action_button_container_left);
            mContainerRight =
                    mDialButtonContainers.findViewById(
                        R.id.dialpad_floating_action_button_container_right);
            mDialButtonLeft =
                    (ImageButton) mDialButtonContainers.findViewById(
                        R.id.dialpad_floating_action_button_left);
            mDialButtonRight =
                    (ImageButton) mDialButtonContainers.findViewById(
                        R.id.dialpad_floating_action_button_right);
            if (null != mDialButtonLeft) {
                    mDialButtonLeft.setOnClickListener(this);
                    //ViewUtil.setupFloatingActionButton(
                            //mContainerLeft, mActivity.getResources());
            }
            if (null != mDialButtonRight) {
                    mDialButtonRight.setOnClickListener(this);
                    //ViewUtil.setupFloatingActionButton(
                            //mContainerRight, mActivity.getResources());
            }
            if (null != mDialButtonContainers) {
                mFrameView.addView(mDialButtonContainers, indexOfDialpadButtonContainer,
                            dialButtonContainerLayoutParams);
            }

            try {
                //mHaptic.init(mActivity, resource.getBoolean(resource.getIdentifier(
                     //BOOLEAN_KEY_VIBRATION, BOOLEAN, packageName)));
            } catch (Resources.NotFoundException nfe) {
                log("Vibrate control bool missing." + nfe);
            }
            updateDialButton();

        }

        initTonePlayer();
    }

    private void initTonePlayer() {
        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled = Settings.System.getInt(mActivity.getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                } catch (RuntimeException e) {
                    log("Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }
    }

    /**
     * Called when need to update dial buttons.
     *
     * @param view need to update.
     */
    public void updateDialButtonIcon(View view) {
        log("updateDialButton view " + view);
        if (null != view) {
            if (SubscriptionManager.from(
                    mContext).getActiveSubscriptionInfoCount() < 2) {
                if (null != mDialButtonLeft) {
                    if (isSimInsert(SLOT1)) {
                        mDialButtonLeft.setImageDrawable(
                            mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1));
                    } else if (isSimInsert(SLOT2)) {
                        mDialButtonLeft.setImageDrawable(
                            mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2));
                    } else {
                        mDialButtonLeft.setImageDrawable(
                            mContext.getResources().getDrawable(R.drawable.fab_ic_call));
                    }
                }
                if (null != mDialButtonRight && isSimInsert(SLOT2)) {
                    mDialButtonRight.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2));
                }
                return;
            }

            if (null == mDialButtonLeft || null == mDialButtonRight) {
                return;
            }

            int containerWidth = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_button_width);
            int containerHeight = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_button_height);
            int containerSmallWidth = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_small_button_width);
            int containerSmallHeight = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_small_button_height);

            ViewGroup.LayoutParams layoutRightParams =
                (ViewGroup.LayoutParams) mContainerRight.getLayoutParams();
            ViewGroup.LayoutParams layoutLeftParams =
                (ViewGroup.LayoutParams) mContainerLeft.getLayoutParams();

            TelecomManager telecomManager =
                    (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
            TelephonyManager telephonyManager = TelephonyManager.from(mContext);
            PhoneAccountHandle defaultHandle =
                    telecomManager.getUserSelectedOutgoingPhoneAccount();
            int slot = SLOT1;
            if (null != defaultHandle && defaultHandle.getId() != null) {
                PhoneAccount account = telecomManager.getPhoneAccount(defaultHandle);
                int subId = telephonyManager.getSubIdForPhoneAccount(account);
                log("subId: " + subId + " slot " + SubscriptionManager.getSlotId(subId));
                slot = SubscriptionManager.getSlotId(subId);
            }
            log("updateDialButton slot " + slot);

            if (SLOT1 == slot) {
                layoutRightParams.width = containerSmallWidth;
                layoutRightParams.height = containerSmallWidth;
                layoutLeftParams.width = containerWidth;
                layoutLeftParams.height = containerHeight;
                mContainerLeft.setLayoutParams(layoutLeftParams);
                mContainerRight.setLayoutParams(layoutRightParams);

                mDialButtonLeft.setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1));
                mDialButtonRight.setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2_small));
            } else if (SLOT2 == slot) {
                layoutRightParams.width = containerWidth;
                layoutRightParams.height = containerWidth;
                layoutLeftParams.width = containerSmallWidth;
                layoutLeftParams.height = containerSmallWidth;
                mContainerLeft.setLayoutParams(layoutLeftParams);
                mContainerRight.setLayoutParams(layoutRightParams);

                mDialButtonLeft.setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1_small));
                mDialButtonRight.setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2));
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialpad_floating_action_button_left: {
                //mHaptic.vibrate();  // Vibrate here too, just like we do for the regular keys
                placeCall(SLOT1);
                return;
            }
            case R.id.dialpad_floating_action_button_right: {
                //mHaptic.vibrate();  // Vibrate here too, just like we do for the regular keys
                placeCall(SLOT2);
                return;
            }
        }
    }

    /**
     * check sim state.
     *
     * @param slot id need to check.
     * @return boolean, true if sim ready.
     */
    public static boolean isSimInsert(final int slot) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        boolean isSimInsert = false;
        log("[isSimInserted] slot :" + slot);
        try {
            if (iTel != null && SubscriptionManager.isValidSlotId(slot)) {
                isSimInsert = iTel.hasIccCardUsingSlotId(slot);
                log("[isSimInserted] isSimInsert :" + isSimInsert);
            }
        } catch (RemoteException e) {
            log("[isSimInserted]catch exception:");
            e.printStackTrace();
            isSimInsert = false;
        }
        return isSimInsert;
    }

    private void updateDialButton() {
        View buttonContainers = mFrameView.findViewById(
                    R.id.dialpad_floating_action_button_containers);
        ViewGroup.LayoutParams layoutParams =
                (ViewGroup.LayoutParams) buttonContainers.getLayoutParams();
        int containerWidth = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_buttons_width);
        log("mContainerRight = " + mContainerRight);

        if (SubscriptionManager.from(
                    mContext).getActiveSubscriptionInfoCount() >= 2 &&
                null != mContainerLeft && null != mContainerRight) {
            mContainerLeft.setVisibility(View.VISIBLE);
            mContainerRight.setVisibility(View.VISIBLE);
            updateDialButtonBackground(mContainerLeft, SLOT1);
            updateDialButtonBackground(mContainerRight, SLOT2);
        } else if (null != mContainerLeft && null != mContainerRight) {
            mContainerLeft.setVisibility(View.VISIBLE);
            mContainerRight.setVisibility(View.GONE);
            containerWidth = mContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_button_width);
            if (isSimInsert(SLOT1)) {
                updateDialButtonBackground(mContainerLeft, SLOT1);
            } else if (isSimInsert(SLOT2)) {
                //updateDialButtonBackground(mContainerRight, SLOT2);
                updateDialButtonBackground(mContainerLeft, SLOT2);
            } else {
                mContainerLeft.getBackground().setTintList(null);
            }
        }
        layoutParams.width = containerWidth;
        buttonContainers.setLayoutParams(layoutParams);
        updateDialButtonIcon(buttonContainers);
    }

    /**
     * update dial button background by slot id.
     *
     * @param dial button need udpate.
     * @param slot indicator.
     * @return void.
     */
    private void updateDialButtonBackground(View dialButton, int slotId) {
        int[] sub = SubscriptionManager.getSubId(slotId);
        int subId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        if (sub != null) {
            subId = sub[0];
        }
        SubscriptionInfo record = SubscriptionManager.from(
                    mContext).getActiveSubscriptionInfo(subId);
        int color = 0;
        if (record != null) {
           color = record.getIconTint();
        }
        if(0 != color) {
            dialButton.getBackground().setTint(color);
        }
    }

    /**
     * place the call, but check to make sure it is a viable number.
     */
    private void placeCall(int slotId) {
        String mLastNumber = mDigits.getText().toString();
        if (PhoneNumberUtils.isLocalEmergencyNumber(mContext, mLastNumber)) {
            log("placing call to " + mLastNumber);

            // place the call if it is a valid number
            if (mLastNumber == null || !TextUtils.isGraphic(mLastNumber)) {
                // There is no number entered.
                playTone(ToneGenerator.TONE_PROP_NACK);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
            if (SubscriptionManager.isValidSlotId(slotId)) {
                PhoneAccountHandle accountHandle = getPhoneAccount(slotId);
                log("accountHandle: " + accountHandle + " slotId " + slotId);
                if (accountHandle != null) {
                    intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
                }
            }
            intent.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, mLastNumber, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(intent);
            mActivity.finish();
        } else {
            log("rejecting bad requested number " + mLastNumber);

            // erase the number and throw up an alert dialog.
            mDigits.getText().delete(0, mDigits.getText().length());
            mActivity.showDialog(BAD_EMERGENCY_NUMBER_DIALOG);
        }
    }

    /**
     * M: add for evdo world phone ecc dial rule,
     * get phone account by slot id if contain cdma phone.
     */
    public PhoneAccountHandle getPhoneAccount(int slotId) {
        TelecomManager telecomManager = (TelecomManager) mContext.
                getSystemService(Context.TELECOM_SERVICE);
        TelephonyManager telephonyManager = TelephonyManager.from(mContext);
        final List<PhoneAccountHandle> accountHandles = telecomManager
                .getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : accountHandles) {
            if (handle.getComponentName().getShortClassName().endsWith(TEL_SERVICE)) {
                if (handle.getId() != null) {
                    PhoneAccount account = telecomManager.getPhoneAccount(handle);
                    int subId = telephonyManager.getSubIdForPhoneAccount(account);
                    log("subId: " + subId + " slot " + SubscriptionManager.getSlotId(subId));
                    if (slotId == SubscriptionManager.getSlotId(subId)) {
                        return handle;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroy(Activity activity) {
        //M:ALPS02416187 Consistent with onCreate for SS condition@{
        TelephonyManager telephonyManager = (TelephonyManager) activity.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getDefault().getPhoneCount() < 2) {
            return;
        }
        //@}
        log("onDestroy, mActivity: " + mActivity + " activity: " + activity);
        if (activity != mActivity) {
            activity.unregisterReceiver(mReceiver);
            return;
        }

        if (null != mActivity && null != mReceiver) {
            mActivity.unregisterReceiver(mReceiver);
        }
        mActivity = null;
    }

    private class EmergencyDialerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("EmergencyDialerBroadcastReceiver, onReceive action = " + action);

            if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(intent.getAction()) ||
                        TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(intent.getAction())) {
                updateDialButton();
            }
        }
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
     *
     * The tone is played locally, using the audio stream for phone calls.
     * Tones are played only if the "Audible touch tones" user preference
     * is checked, and are NOT played if the device is in silent mode.
     *
     * @param tone a tone code from {@link ToneGenerator}
     */
    private void playTone(int tone) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
            || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                log("playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, TONE_LENGTH_MS);
        }
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
