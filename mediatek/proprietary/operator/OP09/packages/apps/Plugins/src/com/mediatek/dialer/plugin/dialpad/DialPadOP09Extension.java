package com.mediatek.dialer.plugin.dialpad;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CDMAPhone;

import com.mediatek.common.PluginImpl;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.dialer.ext.DefaultDialPadExtension;
import com.mediatek.dialer.ext.DialpadExtensionAction;
import com.mediatek.dialer.plugin.OP09DialerPluginUtil;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

/**
 * dialpad extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.dialer.ext.IDialPadExtension")
public class DialPadOP09Extension extends DefaultDialPadExtension implements View.OnClickListener {

    private static final String TAG = "DialPadOP09Extension";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";

    private static final String ID = "id";
    private static final String ID_NAME_DIALPAD_CONTAINER = "dialpadContainer";
    private static final String ID_NAME_DIALPAD_BUTTON_CONTAINER = "dialpad_floating_action_button_container";
    private static final String PRL_VERSION_DISPLAY = "*#0000#";
    private static final String CDMAINFO = "android.intent.action.CdmaInfoSpecification";
    private static final String GOOGLE_EM_SECRET_CODE = "*#*#4636#*#*";

    private static final String ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED
            = "com.android.dialer.ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED";
    private static final String EXTRA_ACCOUNT = "extra_account";

    private static final int SLOT1 = 0;
    private static final int SLOT2 = 1;

    private Fragment mFragment;
    private DialpadExtensionAction mDialpadFragment;
    private Context mPluginContext;
    private Context mHostContext;

    private ImageButton mDialButtonLeft;
    private ImageButton mDialButtonRight;
    private ImageButton mDialButtonCenter;
    private View mButtonContainerLeft;
    private View mButtonContainerRight;
    private View mButtonContainerCenter;
    private FloatingActionButtonController mButtonControllerLeft;
    private FloatingActionButtonController mButtonControllerRight;
    private FloatingActionButtonController mButtonControllerCenter;
    private int mDialFromSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;

    private EditText mDigits;
    private ListView mDialpadChooser;
    private FrameLayout mDialpadButtonContainer;
    private BroadcastReceiver mReceiver;

    public void onCreate(Context context, Fragment fragment, DialpadExtensionAction dialpadFragment) {
        mFragment = fragment;
        mDialpadFragment = dialpadFragment;
        OP09DialerPluginUtil dialerPlugin = new OP09DialerPluginUtil(context);
        mPluginContext = dialerPlugin.getPluginContext();
        mHostContext = dialerPlugin.getHostContext();

        mReceiver = new DialpadBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mFragment.getActivity().registerReceiver(mReceiver, intentFilter);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState, View fragmentView) {
        log("onCreateView()");
        Resources resource = mFragment.getActivity().getResources();
        String packageName = mFragment.getActivity().getPackageName();
        RelativeLayout dialpadButtonContainerLand =
                (RelativeLayout) fragmentView.findViewById(
                        resource.getIdentifier(ID_NAME_DIALPAD_CONTAINER, ID, packageName));
        FrameLayout dialpadButtonContainer =
                (FrameLayout) fragmentView.findViewById(
                        resource.getIdentifier(ID_NAME_DIALPAD_BUTTON_CONTAINER, ID, packageName));

        log("dialpadButtonContainer = " + dialpadButtonContainer);
        if (null != dialpadButtonContainer) {
            ViewGroup viewGroup = (ViewGroup) dialpadButtonContainer.getParent();
            if (null == viewGroup) {
                viewGroup = (ViewGroup) fragmentView;
            }

            int indexOfDialpadButtonContainer = viewGroup.indexOfChild(dialpadButtonContainer);
            ViewGroup.LayoutParams dialpadButtonContainerLayoutParams =
                    (ViewGroup.LayoutParams) dialpadButtonContainer.getLayoutParams();
            viewGroup.removeView(dialpadButtonContainer);
            int mDialpadContainerWidth = mPluginContext.getResources().
                    getDimensionPixelSize(R.dimen.floating_action_buttons_width);
            log("indexOfDialpadButtonContainer = " + indexOfDialpadButtonContainer);

            dialpadButtonContainerLayoutParams.width = mDialpadContainerWidth;
            LayoutInflater inflaterOfPlugin = LayoutInflater.from(mPluginContext);
            mDialpadButtonContainer = (FrameLayout) inflaterOfPlugin.inflate(R.layout.dialpad_button_containers, null);
            mDialpadButtonContainer.setId(
                    resource.getIdentifier(ID_NAME_DIALPAD_BUTTON_CONTAINER, ID, packageName));

            mButtonContainerLeft =
                    mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_container_left);
            mDialButtonLeft =
                    (ImageButton) mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_left);
            if (null != mDialButtonLeft) {
                    mDialButtonLeft.setOnClickListener(this);
                    mButtonControllerLeft = new FloatingActionButtonController(mFragment.getActivity(),
                            mButtonContainerLeft, mDialButtonLeft);
            }

            mButtonContainerCenter =
                    mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_container_center);
            mDialButtonCenter =
                    (ImageButton) mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_center);
            if (null != mDialButtonCenter) {
                    mDialButtonCenter.setOnClickListener(this);
                    mButtonControllerCenter = new FloatingActionButtonController(mFragment.getActivity(),
                            mButtonContainerCenter, mDialButtonCenter);
            }

            mButtonContainerRight =
                    mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_container_right);
            mDialButtonRight =
                    (ImageButton) mDialpadButtonContainer.findViewById(R.id.dialpad_floating_action_button_right);
            if (null != mDialButtonRight) {
                    mDialButtonRight.setOnClickListener(this);
                    mButtonControllerRight = new FloatingActionButtonController(mFragment.getActivity(),
                            mButtonContainerRight, mDialButtonRight);
            }

            if (null != mDialpadButtonContainer) {
                viewGroup.addView(mDialpadButtonContainer, indexOfDialpadButtonContainer,
                            dialpadButtonContainerLayoutParams);
            }
        }
        mDigits = (EditText) fragmentView.findViewById(resource.getIdentifier("digits", ID, packageName));
        mDialpadChooser = (ListView) fragmentView.findViewById(
                resource.getIdentifier("dialpadChooser", ID, packageName));
        TelecomManager telecomManager = (TelecomManager) mPluginContext.
                getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle selectedAccountHandle = telecomManager.
                getUserSelectedOutgoingPhoneAccount();
        updateDialButtons(selectedAccountHandle);
        return fragmentView;
    }

    /**
     * for OP09
     * called when dialpad resume
     */
    public void onResume() {
        log("onResume");
        TelecomManager telecomManager = (TelecomManager) mPluginContext.
                getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle selectedAccountHandle = telecomManager.
                getUserSelectedOutgoingPhoneAccount();
        updateDialButtons(selectedAccountHandle);
    }

    public void onDestroy() {
        if (null != mReceiver && null != mFragment) {
            mFragment.getActivity().unregisterReceiver(mReceiver);
        }
        mFragment = null;
    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.dialpad_floating_action_button_left:
                log("click dialButtonLeft button");
                dialFromSlot(SLOT1);
                break;

            case R.id.dialpad_floating_action_button_right:
                log("click dialButtonRight button");
                dialFromSlot(SLOT2);
                break;

            case R.id.dialpad_floating_action_button_center:
                log("click dialButtonCenter button");
                boolean isEcc = PhoneNumberUtils.isPotentialLocalEmergencyNumber(
                            mPluginContext, mDigits.getText().toString());
                log("isEcc : " + isEcc + " mDialFromSlotId " + mDialFromSlotId);
                if (SubscriptionManager.INVALID_SIM_SLOT_INDEX != mDialFromSlotId &&
                        isEcc) {
                    mDialFromSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
                }
                dialFromSlot(mDialFromSlotId);
                break;

            default:
                break;
        }
    }

    public void updateDialAndDeleteButtonEnabledState(final String lastNumberDialed) {
        if (null != mDialButtonLeft && null != mDialButtonRight) {
            if (phoneIsCdma() && phoneIsOffhook()) {
                mDialButtonLeft.setEnabled(true);
                mDialButtonRight.setEnabled(true);
            } else {
                mDialButtonLeft.setEnabled(!isDigitsEmpty() || !TextUtils.isEmpty(lastNumberDialed));
                mDialButtonRight.setEnabled(!isDigitsEmpty() || !TextUtils.isEmpty(lastNumberDialed));
            }
        }
    }

    /**
     * @return true if the phone is a CDMA phone type
     */
    private boolean phoneIsCdma() {
        boolean isCdma = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                isCdma = (phone.getActivePhoneType() == TelephonyManager.PHONE_TYPE_CDMA);
            }
        } catch (RemoteException e) {
            log("phone.getActivePhoneType() failed");
        }
        return isCdma;
    }

    /**
     * @return true if the phone state is OFFHOOK
     */
    private boolean phoneIsOffhook() {
        boolean phoneOffhook = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                phoneOffhook = phone.isOffhook(PACKAGE_NAME);
            }
        } catch (RemoteException e) {
            log("phone.isOffhook() failed");
        }
        return phoneOffhook;
    }

    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return (null == mDigits) ? false : mDigits.length() == 0;
    }

    private boolean dialpadChooserVisible() {
        return (null == mDialpadChooser) ? false : (mDialpadChooser.getVisibility() == View.VISIBLE);
    }

    private void onDialButtonPressed(Intent intent) {
        log("onDialButtonPressed intent = " + intent);
        if (TextUtils.isEmpty(mDigits.getText().toString())) {
            if (null != mDialpadFragment) {
                mDialpadFragment.handleDialButtonClickWithEmptyDigits();
            }
        } else {
            if (null != mDialpadFragment) {
                mDialpadFragment.doCallOptionHandle(intent);
                if (null != mDigits) {
                    if (mDigits.getText().length() > 0) {
                        log("mDigits.getText() " + mDigits.getText().toString());
                        mDigits.setText("");
                    }
                }
            }
        }
    }

    private void updateDialButtons(PhoneAccountHandle accountHandle) {
        TelephonyManager telephonyManager = (TelephonyManager) mPluginContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (0 == SubscriptionManager.from(
                    mHostContext).getActiveSubscriptionInfoCount() ||
                    telephonyManager.getDefault().getPhoneCount() < 2) {
            showCenterDialButton(SubscriptionManager.INVALID_SIM_SLOT_INDEX);
            return;
        } else if (1 == SubscriptionManager.from(
                    mHostContext).getActiveSubscriptionInfoCount() ) {
            if (OP09DialerPluginUtil.isSimInsert(SLOT1)) {
                showCenterDialButton(SLOT1);
            } else if (OP09DialerPluginUtil.isSimInsert(SLOT2)) {
                showCenterDialButton(SLOT2);
            }
            return;
        }

        TelecomManager telecomManager = (TelecomManager) mPluginContext.
                getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle defaultHandle = null;
        if (null != accountHandle) {
            defaultHandle = accountHandle;
        } else {
            defaultHandle = telecomManager.
                getUserSelectedOutgoingPhoneAccount();
        }
        if (defaultHandle == null) {
            defaultHandle = OP09DialerPluginUtil.getPstnPhoneAccountHandleBySlotId(mPluginContext, SLOT1);
            if (defaultHandle != null) {
                telecomManager.setUserSelectedOutgoingPhoneAccount(defaultHandle);
            }
            showLeftRightDialButton(SLOT1);
            log("error get default account,change the voice call setting to slot = " + SLOT1);
            return;
        }

        PhoneAccount account = telecomManager.getPhoneAccount(defaultHandle);
        int defaultSubId = telephonyManager.getSubIdForPhoneAccount(account);
        log("updateDialButtons defaultSubId " + defaultSubId);
        boolean isValideSubId = SubscriptionManager.isValidSubscriptionId(defaultSubId);
        int slot = SubscriptionManager.getSlotId(defaultSubId);
        log("updateDialButtons slot " + slot);
        showLeftRightDialButton(slot);
    }

    private class DialpadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("DialpadBroadcastReceiver, onReceive action = " + action);

            if (TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED.equals(action)
                    || TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)
                    || ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED.equals(action)
                    || TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)) {
                PhoneAccountHandle handle = (PhoneAccountHandle) intent.getParcelableExtra(EXTRA_ACCOUNT);
                updateDialButtons(handle);
            }
        }
    }

    public void showCenterDialButton(int slotId) {
        mButtonContainerLeft.setVisibility(View.GONE);
        mButtonContainerCenter.setVisibility(View.VISIBLE);
        mButtonContainerRight.setVisibility(View.GONE);
        if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            if (slotId == SLOT1) {
                mDialFromSlotId = SLOT1;
                updateDialButtonBackground(mButtonContainerCenter, SLOT1);
                mDialButtonCenter.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1));
            } else if (slotId == SLOT2) {
                mDialFromSlotId = SLOT2;
                updateDialButtonBackground(mButtonContainerCenter, SLOT2);
                mDialButtonCenter.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2));
            } else {
                log("invalide slot id ");
            }
        } else {
            mDialFromSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
            /// M: fix ALPS02335525 update background and reset the default image
            /// When no sim inserted @{
            updateDialButtonBackground(mButtonContainerCenter, mDialFromSlotId);
            mDialButtonCenter.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call));
            /// @}
        }
    }

    public void showLeftRightDialButton(int slotId) {
        log("showLeftRightDialButton ");
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            return;
        }

       int containerWidth = mPluginContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_button_width);
        int containerHeight = mPluginContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_button_height);
        int containerSmallWidth = mPluginContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_small_button_width);
        int containerSmallHeight = mPluginContext.getResources().
                getDimensionPixelSize(R.dimen.floating_action_small_button_height);

        ViewGroup.LayoutParams layoutRightParams =
                (ViewGroup.LayoutParams) mButtonContainerRight.getLayoutParams();
        ViewGroup.LayoutParams layoutLeftParams =
                (ViewGroup.LayoutParams) mButtonContainerLeft.getLayoutParams();

        if (SLOT1 == slotId) {
            layoutRightParams.width = containerSmallWidth;
            layoutRightParams.height = containerSmallWidth;
            layoutLeftParams.width = containerWidth;
            layoutLeftParams.height = containerHeight;
            mButtonContainerLeft.setLayoutParams(layoutLeftParams);
            mButtonContainerRight.setLayoutParams(layoutRightParams);

            mDialButtonLeft.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1));
            mDialButtonRight.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2_small));
        } else if (SLOT2 == slotId) {
            layoutRightParams.width = containerWidth;
            layoutRightParams.height = containerWidth;
            layoutLeftParams.width = containerSmallWidth;
            layoutLeftParams.height = containerSmallWidth;
            mButtonContainerLeft.setLayoutParams(layoutLeftParams);
            mButtonContainerRight.setLayoutParams(layoutRightParams);
            mDialButtonLeft.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim1_small));
            mDialButtonRight.setImageDrawable(
                    mPluginContext.getResources().getDrawable(R.drawable.fab_ic_call_sim2));
        } else {
            log("slot is not 0 or 1, maybe it's triple sim version or above, please check it");
        }
        updateDialButtonBackground(mButtonContainerLeft, SLOT1);
        updateDialButtonBackground(mButtonContainerRight, SLOT2);
        mButtonContainerLeft.setVisibility(View.VISIBLE);
        mButtonContainerCenter.setVisibility(View.GONE);
        mButtonContainerRight.setVisibility(View.VISIBLE);
    }

    /**
     * For OP09, brings up the "dialpad chooser" UI in place of the usual Dialer
     * elements (the textfield/button and the dialpad underneath).
     *
     * @param enabled If true, show the "dialpad chooser" instead
     *                of the regular Dialer UI
     */
    public void showDialpadChooser(boolean enabled) {
        log("showDialpadChooser enabled : " + enabled);
        if (null != mDialpadButtonContainer) {
            if (enabled) {
                mDialpadButtonContainer.setVisibility(View.GONE);
            } else {
                mDialpadButtonContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    public void dialFromSlot(int slotId) {
        if (OP09DialerPluginUtil.isSimInsert(slotId)) {
            onDialButtonPressed(
                    OP09DialerPluginUtil.getCallIntent(
                            mDigits.getText().toString(),
                            OP09DialerPluginUtil.getPstnPhoneAccountHandleBySlotId(
                                    mPluginContext, slotId)));
        } else {
            onDialButtonPressed(
                    OP09DialerPluginUtil.getCallIntent(mDigits.getText().toString(), null));
        }
    }

    /**
     * for OP09
     * the dialpad hidden changed
     * @param enabled If true, show the dialpad 
     */
    public void onHiddenChanged(boolean scaleIn, int delayMs) {
        log("onHiddenChanged scaleIn : " + scaleIn + " delayMs : " + delayMs);
        if (scaleIn) {
            if (mButtonContainerLeft.isShown()) {
                mButtonControllerLeft.scaleIn(delayMs);
            }
            if (mButtonContainerCenter.isShown()) {
                mButtonControllerCenter.scaleIn(delayMs);
            }
            if (mButtonContainerRight.isShown()) {
                mButtonControllerRight.scaleIn(delayMs);
            }
        } else {
            if (mButtonContainerLeft.isShown()) {
                mButtonControllerLeft.scaleOut();
            }
            if (mButtonContainerCenter.isShown()) {
                mButtonControllerCenter.scaleOut();
            }
            if (mButtonContainerRight.isShown()) {
                mButtonControllerRight.scaleOut();
            }
        }
    }

    /**
     * handle special chars from user input on dial pad.
     *
     * @param context from host app.
     * @param input from user input in dial pad.
     * @return boolean, check if the input string is handled.
     */
    public boolean handleChars(Context context, String input) {
        if (input.equals(PRL_VERSION_DISPLAY)) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            int length = SubscriptionManager.from(
                    mHostContext).getActiveSubscriptionIdList().length;
            try {
                ITelephony iTel = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));
                log("handleChars getActiveSubscriptionIdList length:" + length);
                for (int i = 0; i < length; i++) {
                    int activeSubId = SubscriptionManager.from(
                    mHostContext).getActiveSubscriptionIdList()[i];
                    int phoneType = iTel.getActivePhoneTypeForSubscriber(activeSubId);
                    if (PhoneConstants.PHONE_TYPE_CDMA == phoneType) {
                        subId = activeSubId;
                        log("handleChars subId:" + subId);
                        break;
                    }
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }

            if (0 != length && SubscriptionManager.isValidSubscriptionId(subId)) {
                showPRLVersionSetting(context, subId);
                return true;
            } else {
                showPRLVersionSetting(context, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                return true;
            }
        }
        return false;
    }

    /**
     * show version by cdma phone provider info.
     *
     * @param context from host app.
     * @param slot indicator which slot is cdma phone.
     * @return void.
     */
    private void showPRLVersionSetting(Context context, int subId) {
        Intent intentCdma = new Intent(CDMAINFO);
        intentCdma.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentCdma.putExtra(CdmaInfoSpecification.KEY_SUB_ID, subId);
        context.startActivity(intentCdma);
    }

    /**
     * called when handle secrete code in dialpad. If will return
     * incomming value except the value is GOOGLE_EM_SECRET_CODE.
     * @param input the string to be handled
     * @return The result of "input" be handled
     */
    public String handleSecretCode(String input) {
        log("[handleSecretCode] incomming value input : " + input);
        if (null != input && GOOGLE_EM_SECRET_CODE.equals(input)) {
            return "*#*##*#*";
        }
        return input;
    }

    /**
     * update dial button background by slot id.
     *
     * @param dial button need udpate.
     * @param slot indicator.
     * @return void.
     */
    private void updateDialButtonBackground(View dialButton, int slotId) {
        /// M: fix ALPS02335525 clear the color tint when no sim inserted
        if (slotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            dialButton.getBackground().setTintList(null);
            return;
        }
        /// @}

        int subId = OP09DialerPluginUtil.getSubIdUsingSlotId(slotId);
        SubscriptionInfo record = SubscriptionManager.from(
                    mHostContext).getActiveSubscriptionInfo(subId);
        int color = 0;
        if (record != null) {
           color = record.getIconTint();
        }
        log("updateDialButtonBackground color : " + color);
        if(0 != color) {
            dialButton.getBackground().setTint(color);
        }
    }

    /**
     * for OP09
     * called when handle ADN query.
     * @param progressDialog
     */
    public void customADNProgressDialog(ProgressDialog progressDialog) {
        log("customADNProgressDialog");
        progressDialog.setTitle(mPluginContext.getResources().getString(
                R.string.simContacts_title));
        progressDialog.setMessage(mPluginContext.getResources().getString(
                R.string.simContacts_emptyLoading));
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
