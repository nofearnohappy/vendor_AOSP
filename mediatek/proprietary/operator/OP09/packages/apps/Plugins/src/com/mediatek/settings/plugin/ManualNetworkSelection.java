
package com.mediatek.settings.plugin;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;

import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.android.phone.INetworkQueryService;
import com.android.phone.INetworkQueryServiceCallback;

import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.HashMap;
import java.util.List;


/**
 * Handles manual network selection for both CDMA and GSM.
 */
public class ManualNetworkSelection extends PreferenceActivity implements
        DialogInterface.OnCancelListener {

    private static final String TAG = "ManualNetworkSelection";
    private static final boolean DBG = true;
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    // TODO: Slot 1 SIM card type is saved in this System Settings value by IR
    // framework. We use this to query SIM 1's card type here. It is just
    // workaround to make sure we get correct card type.
    private static final String KEY_GSM1_NETWORK_SELECTION = "gsm_sim1_network_selection";
    private static final String KEY_GSM2_NETWORK_SELECTION = "gsm_sim2_network_selection";
    private static final String KEY_MANUAL_NOTES = "key_notes";
    private static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    // GSM network switching dialog
    private static final int DIALOG_NETWORK_SELECTION = 1003;
    private static final int DIALOG_NETWORK_LIST_LOAD = 1004;

    private static final int EVENT_NETWORK_SELECTION_DONE = 101;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 102;
    private static final int EVENT_NETWORK_SCAN_COMPLETED_2 = 103;
    private static final int NETWORK_QUERY_FAILED_RETRY_DELAY = 10 * 1000;
    private static final int NETWORK_QUERY_FAILED_RETRY_TIMES = 30;
    private static final int NETWORK_QUERY_FAILED_RETRY_TOTAL_TIME = 300 * 1000;

    // Status that will be retured in the callback.
    private static final int QUERY_OK = 0;

    // GSM NETWORK_RAT.
    private static final int NETWORK_RAT_UNKNOWN = 0;
    private static final int NETWORK_RAT_2G = 1;
    private static final int NETWORK_RAT_3G = 2;
    private static final int NETWORK_RAT_4G = 3;

    public static final String NUMERIC_UNKNOWN = "-1";
    // Map of GSM network information.
    private HashMap<Preference, OperatorInfo> mNetworkMap = new HashMap<Preference, OperatorInfo>();
    private Phone mPhone;
    private boolean mIsForeground;

    private PreferenceGroup mGsm1NetworkSelectionPref;
    private PreferenceGroup mGsm2NetworkSelectionPref;
    private Preference mNotesPref;

    private int mTargetSlot;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mExModemSlot;
    private boolean mIsExternalSlot;
    private boolean mIsCTSupportIRCard;
    private String mSelectedNetwork = "";
    private String mSelectedNumeric = "";
    private int mSelectedNetworkRate = NETWORK_RAT_UNKNOWN;
    private String mSelectedNetworkText;
    private ServiceState mServiceState;

    private ITelephony mTelephony;
    private TelephonyManager mTelephonyManager;
    private TelephonyManagerEx mTelephonyManagerEx;

    private boolean mIsAborting = false;
    private Handler mRetryTimeoutHandler = new Handler();
    private Runnable mRetryTimeoutRunnable = new Runnable() {
                              @Override
                              public void run() {
                                  if (mNewQuery == true) {
                                      forceQuitGSMNetworkQuery();
                                      displayNetworkQueryFailed();
                                      displayEmptyNetworkList(true);
                                  }
                              }
                          };
    // GSM network selection service.
    private INetworkQueryService mNetworkQueryService = null;

    private PhoneStateListener mPhoneStateListener;
    private int mRetryTimes = 0;
    private boolean mNewQuery = true;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            log("onReceive: action = " + action + ", mTargetSlot = " +
                   mTargetSlot + ", this = " + this);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                    || action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED) && (mNewQuery == true)) {
                    forceQuitGSMNetworkQuery();
                    displayNetworkQueryFailed();
                    displayEmptyNetworkList(true);
                }
                updateScreen();
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                // Finish SIM2's manual network settings fragment if SIM2 is
                // plugged out.
                int status = intent.getIntExtra(SubscriptionManager.INTENT_KEY_DETECT_STATUS,
                        SubscriptionManager.EXTRA_VALUE_NOCHANGE);
                if ((mTargetSlot != mExModemSlot)
                        && (status == SubscriptionManager.EXTRA_VALUE_REMOVE_SIM)) {
                    final boolean simInserted = isSimInserted(mTargetSlot);
                    log("ACTION_SIM_DETECTED: simInserted = " + simInserted);
                    if (!simInserted) {
                        finish();
                    }
                }
            } else if (action.equals(TelephonyIntents.ACTION_CDMA_CARD_TYPE)) {
                IccCardConstants.CardType cardType =
                        (IccCardConstants.CardType) intent.getSerializableExtra(
                        TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
                if ((cardType != null)
                    && ((cardType.compareTo(IccCardConstants.CardType.CT_UIM_SIM_CARD) == 0)
                    || (cardType.compareTo(IccCardConstants.CardType.CT_4G_UICC_CARD) == 0))) {
                    mIsCTSupportIRCard = true;
                } else {
                    mIsCTSupportIRCard = false;
                }
                updateScreen();
                log("ACTION_CDMA_CARD_TYPE: cardType = " + cardType
                    + " and mIsCTSupportIRCard = " + mIsCTSupportIRCard);
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTargetSlot = getIntent().getIntExtra(PhoneConstants.SLOT_KEY, -1);
        int[] subId  = SubscriptionManager.getSubId(mTargetSlot);
        if (subId != null) {
            mSubId = subId[0];
            int phoneID = SubscriptionManager.getPhoneId(mSubId);
            mPhone = PhoneFactory.getPhone(phoneID);
            mServiceState = mPhone.getServiceState();
            if (mPhone == null) {
                Log.e("@M_" + TAG, "onCreate: mSubId is invalid, finsh");
                mIsAborting = true;
                finish();
            }
        } else {
            Log.e("@M_" + TAG, "onCreate: getSubId = null,finsh");
            mIsAborting = true;
            finish();
        }

        mExModemSlot = getExternalModemSlot();
        mIsExternalSlot = (mTargetSlot == mExModemSlot);

        setTitleAndInitPreferences();


        mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();


        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        intentFilter.addAction(TelephonyIntents.ACTION_CDMA_CARD_TYPE);
        registerReceiver(mReceiver, intentFilter);

        // If this is slot 2, start and bind service early, so that we can start
        // search gsm network quickly
        if (mTargetSlot != mExModemSlot && mTargetSlot == PhoneConstants.SIM_ID_2) {
            startAndBindNetworkQueryService();
        }

        log("onCreate: mTargetSlot=" + mTargetSlot + ", mExModemSlot=" + mExModemSlot);
    }

    @Override
    public void onResume() {
        super.onResume();
        createPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        mIsForeground = true;
        updateScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mIsForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsAborting == true) {
            mIsAborting = false;
            return;
        }
        forceQuitGSMNetworkQuery();

        unregisterReceiver(mReceiver);
        if (mNetworkQueryService != null) {
            unbindService(mNetworkQueryServiceConnection);
        }
    }

    private void setTitleAndInitPreferences() {
        // Update Activity's title according to SIM slot.
        int slotResId = (mTargetSlot == PhoneConstants.SIM_ID_1) ? R.string.sim_slot_1
                : R.string.sim_slot_2;
        String simSlot = getResources().getString(slotResId);
        setTitle(getResources()
                .getString(R.string.manual_network_selection_title_with_sim, simSlot));

        addPreferencesFromResource(R.xml.ct_manual_network_selection);


        mGsm1NetworkSelectionPref = (PreferenceGroup) findPreference(KEY_GSM1_NETWORK_SELECTION);
        mGsm2NetworkSelectionPref = (PreferenceGroup) findPreference(KEY_GSM2_NETWORK_SELECTION);
        mNotesPref = findPreference(KEY_MANUAL_NOTES);

        if (mTargetSlot == PhoneConstants.SIM_ID_1) {
            getPreferenceScreen().removePreference(mGsm2NetworkSelectionPref);
        } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
            getPreferenceScreen().removePreference(mGsm1NetworkSelectionPref);
            getPreferenceScreen().removePreference(mNotesPref);
        }
    }

 private boolean targetSlotRadioOn() {
        boolean isRadioInOn = false;
        try {
            if (mTelephony != null) {
                isRadioInOn = mTelephony.isRadioOnForSubscriber(mSubId, PACKAGE_NAME);
                Log.d("@M_" + TAG, "Slot " + mTargetSlot + " is in radion state " + isRadioInOn);
            }
        } catch (RemoteException e) {
            Log.e("@M_" + TAG, "mTelephony exception");
        }
         return isRadioInOn;
    }

    private void updateScreen() {
        boolean isAirplanMode = Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) > 0;
        boolean isRadioOn = false;
        isRadioOn = targetSlotRadioOn();
        if (isRadioOn && !isAirplanMode) {
            if (mTargetSlot == PhoneConstants.SIM_ID_1) {
                boolean externalSlotInRoaming = (mIsCTSupportIRCard
                     && mPhone != null && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM
                     && SlotSettingsFragment.externalSlotInRoamingService(mServiceState));
                mGsm1NetworkSelectionPref.setEnabled(externalSlotInRoaming);
                log("updateScreen: externalSlotInRoaming = " + externalSlotInRoaming);
            } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                mGsm2NetworkSelectionPref.setEnabled(true);
            }

            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                mGsm1NetworkSelectionPref.setEnabled(false);
            }
        } else {
            if (mTargetSlot == PhoneConstants.SIM_ID_1) {
                mGsm1NetworkSelectionPref.setEnabled(false);
            } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                mGsm2NetworkSelectionPref.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mGsm1NetworkSelectionPref) {
            log("onPreferenceTreeClick gsm network selection, mTargetSlot = " + mTargetSlot);
            updateNetworkSelectionMode();
        } else if (preference instanceof CarrierRadioPreference) {
            Preference selectedCarrier = preference;
            String networkStr = selectedCarrier.getTitle().toString();
            log("onPreferenceTreeClick: selected network = " + networkStr);
            Message msg = mNetworkSelectionHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);

            final OperatorInfo ni = mNetworkMap.get(selectedCarrier);
            mPhone.selectNetworkManually(ni, msg);

            mSelectedNetwork = ni.getOperatorAlphaLong();
            mSelectedNumeric = ni.getOperatorNumeric();
            mSelectedNetworkRate = getNetworkRate(ni);
            if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                SlotSettingsFragment.setGsmAutoNetowrkSelection(this, false);
            }
            displayNetworkSeletionInProgress(networkStr);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = null;
        log("onCreateDialog: id = " + id);
        switch (id) {
            case DIALOG_NETWORK_LIST_LOAD:
                // reinstate the cancelablity of the dialog.
                dialog = new ProgressDialog(this);
                ((ProgressDialog) dialog).setMessage(getResources().getString(
                        R.string.load_networks_progress));
                ((ProgressDialog) dialog).setCancelable(false);
                ((ProgressDialog) dialog).setOnCancelListener(this);
                break;

            case DIALOG_NETWORK_SELECTION:
                dialog = new ProgressDialog(this);
                ((ProgressDialog) dialog).setMessage(mSelectedNetworkText);
                ((ProgressDialog) dialog).setCancelable(false);
                ((ProgressDialog) dialog).setIndeterminate(true);
                break;

            default:
                break;
        }
        return dialog;
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        log("onCancel, force quit gsm network network query. dialog = " + dialog);
        forceQuitGSMNetworkQuery();
        if (mTargetSlot != mExModemSlot) {
            finish();
        }
    }

    // TODO: slot 2 not implemented
    private void updateNetworkSelectionMode() {
        log("updateNetworkSelectionMode: mTargetSlot = " + mTargetSlot);
        if (mIsExternalSlot) {
                if (mNetworkQueryService == null) {
                    startAndBindNetworkQueryService();
                } else {
                        loadGsmNetworksList();
                }
            updateScreen();
        } else {
            Log.e("@M_" + TAG, "updateNetworkSelectionMode should not come here");
        }
    }


    private PreferenceGroup getGsmNetworkPref(int slotId) {
        if (mTargetSlot == PhoneConstants.SIM_ID_1) {
            return mGsm1NetworkSelectionPref;
        } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
            return mGsm2NetworkSelectionPref;
        }

        // Should never run into this case.
        Log.w("@M_" + TAG, "Should never run into this case: mTargetSlot = " + mTargetSlot);
        return null;
    }


    private boolean isSimInserted(int slotId) {
        if (mTelephonyManagerEx != null) {
            return mTelephonyManagerEx.hasIccCard(slotId);
        }
        return false;
    }

    /** GSM network selection Service connection. */
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {

        /** Handle the task of binding the local object to the service */
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) {
                log("connection created, binding local service.");
            }
            mNetworkQueryService = INetworkQueryService.Stub.asInterface(service);
            // as soon as it is bound, run a query.
            loadGsmNetworksList();
        }

        /** Handle the task of cleaning up the local binding */
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) {
                log("connection disconnected, cleaning local binding.");
            }
            mNetworkQueryService = null;
        }
    };

    /**
     * This implementation of INetworkQueryServiceCallback is used to receive
     * callback notifications from the network query service.
     */
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {

        /** place the message on the looper queue upon query completion. */
        public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) {
            if (DBG) {
                log("notifying message loop of query completion.");
            }
            Message msg;
            if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                msg = mNetworkSelectionHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED_2,
                        status, 0, networkInfoArray);
            } else {
                msg = mNetworkSelectionHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED, status,
                        0, networkInfoArray);
            }
            msg.sendToTarget();
        }
    };

    private final Handler mNetworkSelectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_NETWORK_SCAN_COMPLETED:
                    log("EVENT_NETWORK_SCAN_COMPLETED: mTargetSlot = " + mTargetSlot);
                    if (mTargetSlot == PhoneConstants.SIM_ID_2) {
                        return;
                    }
                    gsmNetworksListLoaded((List<OperatorInfo>) msg.obj, msg.arg1);
                    break;

                case EVENT_NETWORK_SCAN_COMPLETED_2:
                    log("EVENT_NETWORK_SCAN_COMPLETED_2: mTargetSlot = " + mTargetSlot);
                    if (mTargetSlot == PhoneConstants.SIM_ID_1) {
                        return;
                    }
                    gsmNetworksListLoaded((List<OperatorInfo>) msg.obj, msg.arg1);
                    break;

                case EVENT_NETWORK_SELECTION_DONE:
                    // Dismiss all dialog when manual select done.
                    if (DBG) {
                        log("hideProgressPanel");
                    }

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        removeDialog(DIALOG_NETWORK_SELECTION);
                        log("Manual network selection failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        log("Manual network selection maybe succeeded!");
                        handleNetworkSelectionDone(msg);
                    }
                    break;

                default:
                    break;
            }
        }


        /**
        * Delay to query the result of selection.
        * @param msg the message of callback.
        */
        private void handleNetworkSelectionDone(final Message msg) {
            final AsyncResult ar = (AsyncResult) msg.obj;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int networkTypeTl = -1;

                    removeDialog(DIALOG_NETWORK_SELECTION);
                    if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                        mServiceState.getDataNetworkType()) {
                        networkTypeTl = getServiceNetworkRat(mServiceState.getDataNetworkType());
                    } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                               mServiceState.getVoiceNetworkType()) {
                        networkTypeTl = getServiceNetworkRat(mServiceState.getVoiceNetworkType());
                    }
                    log("handleNetworkSelection mSelectedNetworkRate=" + mSelectedNetworkRate);
                    log("handleNetworkSelectionDone networkTypeTl=" + networkTypeTl);

                    if (mSelectedNetworkRate == networkTypeTl) {
                        log("Manual network selection succeeded!");
                        displayNetworkSelectionSucceeded();
                    } else {
                        log("Manual network selection fail!");
                        mSelectedNumeric = mServiceState.getOperatorNumeric();
                        mSelectedNetworkRate = networkTypeTl;
                        log("handleNetworkSelectionDone selection fail mSelectedNumeric="
                            + mSelectedNumeric);
                        updateCarrierPreferenceCheckedState();
                        String status = getString(R.string.manual_network_register_hint_fail,
                                                  mSelectedNetwork);
                        if (mIsForeground) {
                            showDialogAlert(status);
                        }
                    }
                }
            };
            mNetworkSelectionHandler.postDelayed(runnable, 2000);
        }

        private int getServiceNetworkRat(int networktype) {
            switch (networktype) {
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    return NETWORK_RAT_2G;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return NETWORK_RAT_3G;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return NETWORK_RAT_4G;
                default:
                    return -1;
            }
        }
    };

    private void startAndBindNetworkQueryService() {
        Intent i = new Intent("com.mediatek.phone.INTERNATIONAL_ROAMING_NETWORK_QUERY");
        i.setPackage("com.android.phone");
        i.putExtra(SUB_ID_EXTRA, mSubId);
        startService(i);
        bindService(i, mNetworkQueryServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadGsmNetworksList() {
        if (DBG) {
            log("loadGsmNetworksList...");
        }
        mNewQuery = true;
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_LIST_LOAD);
        }
        networkQuery();
    }

    private void networkQuery() {
        int phoneID = SubscriptionManager.getPhoneId(mSubId);
        log("networkQuery, mTargetSlot = " + mTargetSlot + " phoneID = " + phoneID);
        try {
            mNetworkQueryService.startNetworkQuery(mCallback, phoneID);
        } catch (RemoteException e) {
            Log.w("@M_" + TAG, "RemoteException when startNetworkQuery.", e);
        }
        displayEmptyNetworkList(true);
    }


    private void networkQueryRetry() {
        log("networkQueryRetry, mTargetSlot = " + mTargetSlot + ", mRetryTimes = " + mRetryTimes);
        if (mRetryTimes == 0) {
            mRetryTimeoutHandler.postDelayed(mRetryTimeoutRunnable,
                NETWORK_QUERY_FAILED_RETRY_TOTAL_TIME);
        }

        try {
            if (mNetworkQueryService != null) {
                mNetworkQueryService.stopNetworkQuery(mCallback);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mRetryTimeoutHandler.postDelayed(new Runnable() {
           @Override
            public void run() {
                networkQuery();
            }
        }, NETWORK_QUERY_FAILED_RETRY_DELAY);

        mRetryTimes++;
    }

  /**
     * networksListLoaded has been rewritten to take an array of OperatorInfo
     * objects and a status field, instead of an AsyncResult. Otherwise, the
     * functionality which takes the OperatorInfo array and creates a list of
     * preferences from it, remains unchanged.
     */
    private void gsmNetworksListLoaded(List<OperatorInfo> result, int status) {
        if (DBG) {
            log("gsmNetworksListLoaded networks list loaded.");
        }
        // used to un-register callback
        try {
            mNetworkQueryService.unregisterCallback(mCallback);
        } catch (RemoteException e) {
            log("gsmNetworksListLoaded: exception from unregisterCallback " + e);
        }

        if (status != QUERY_OK && mNewQuery == true
                && mRetryTimes < NETWORK_QUERY_FAILED_RETRY_TIMES) {
             networkQueryRetry();
             return;
        }

        // Add for dismiss the dialog is showing.
        removeDialog(DIALOG_NETWORK_LIST_LOAD);
        clearGsmNetworkList();

        if (status != QUERY_OK) {
            log("Error happend while querying available networks.");
            displayNetworkQueryFailed();
            displayEmptyNetworkList(true);
        } else {
            if (result != null) {
                displayEmptyNetworkList(false);

                // Create a preference for each item in the list.
                // Use the operator name instead of the mildly
                // confusing mcc/mnc, add forbidden at the end if needed.
                /**
                 * M: Get the current network type, the bigger type should
                 * be the current network type displayed in status bar.
                 */
                boolean isDataNet = isCurrentDataNetworkType(
                                        mServiceState.getDataNetworkType(),
                                        mServiceState.getVoiceNetworkType());
                for (OperatorInfo ni : result) {
                    CarrierRadioPreference carrier = new CarrierRadioPreference(this, null);
                    String forbidden = "";
                    if (ni.getState() == OperatorInfo.State.FORBIDDEN) {
                        forbidden = "(" + getResources().getString(R.string.network_forbidden)
                                    + ")";
                    }
                    carrier.setTitle(getNetworkTitle(ni) + forbidden);
                    carrier.setPersistent(false);
                    carrier.setCarrierNumeric(ni.getOperatorNumeric());
                    carrier.setCarrierRate(getNetworkRate(ni));
                    getGsmNetworkPref(mTargetSlot).addPreference(carrier);

                    mNetworkMap.put(carrier, ni);

                    // Set current selected carrier to checked state.
                    //carrier.setChecked(ni.getState() == OperatorInfo.State.CURRENT);

                    /// Set checked state if operator info matches current service state.
                    log("gsmNetworksListLoaded isCurrentNetwork return: "
                        + isCurrentNetwork(isDataNet, ni));
                    carrier.setChecked(isCurrentNetwork(isDataNet, ni));

                    if (DBG) {
                        log("  " + ni);
                    }
                }
            } else {
                displayEmptyNetworkList(true);
            }
        }
        forceQuitGSMNetworkQuery();
    }

    /**
     * Check whether current is data network type.
     *
     * @param ps data network type.
     * @param cs voice network type.
     *
     * @return true if data network type.
     */
    private boolean isCurrentDataNetworkType(int ps, int cs) {
        if (TelephonyManager.getNetworkClass(ps) > TelephonyManager.getNetworkClass(cs)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether the operator info matches the current service state.
     *
     * @param isDataNet if current network type is data network type.
     * @param ni the operator info.
     *
     * @return true if matches.
     */
    private boolean isCurrentNetwork(boolean isDataNet, OperatorInfo ni) {
        String opNumeric = isDataNet ?
            mServiceState.getDataOperatorNumeric() : mServiceState.getVoiceOperatorNumeric();
        log("isCurrentNetwork isDataNet = " + isDataNet + ", opNumeric = " + opNumeric);
        if (opNumeric == null) {
            return false;
        }
        if (opNumeric.equals(ni.getOperatorNumeric())) {
            int networkType = isDataNet ? mServiceState.getDataNetworkType() :
                mServiceState.getVoiceNetworkType();
            log("isCurrentNetwork networkType = " + networkType +
                ", oprate = " + getNetworkRate(ni));
            switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return (getNetworkRate(ni) == NETWORK_RAT_2G);
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return (getNetworkRate(ni) == NETWORK_RAT_3G);
            case TelephonyManager.NETWORK_TYPE_LTE:
                return (getNetworkRate(ni) == NETWORK_RAT_4G);
            default:
                return false;
            }
        }
        return false;
    }

    private void clearGsmNetworkList() {
        for (Preference p : mNetworkMap.keySet()) {
            getGsmNetworkPref(mTargetSlot).removePreference(p);
        }
        mNetworkMap.clear();
    }

    private void displayEmptyNetworkList(boolean isEmpty) {
        if (mTargetSlot == PhoneConstants.SIM_ID_1) {
            if (isEmpty) {
                clearGsmNetworkList();
            }
        } else if (mTargetSlot == PhoneConstants.SIM_ID_2) {
            if (isEmpty) {
                getPreferenceScreen().removePreference(mGsm2NetworkSelectionPref);
            } else {
                if (getPreferenceScreen().findPreference(KEY_GSM2_NETWORK_SELECTION) == null) {
                    getPreferenceScreen().addPreference(mGsm2NetworkSelectionPref);
                }
            }
        }
    }

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param OperatorInfo contains the information of the network.
     * @return Long Name if not null/empty, otherwise Short Name if not
     *         null/empty, else MCCMNC string.
     */
    private String getNetworkTitle(OperatorInfo ni) {
        log("getNetworkTitle: ni = " + ni);
        if (!TextUtils.isEmpty(ni.getOperatorAlphaLong())) {
            return ni.getOperatorAlphaLong();
        } else if (!TextUtils.isEmpty(ni.getOperatorAlphaShort())) {
            return ni.getOperatorAlphaShort();
        } else {
            return ni.getOperatorNumeric();
        }
    }

    /**
     * Get the RATE from operator name.
     *
     * @param ni
     * @return
     */
    private int getNetworkRate(OperatorInfo ni) {
        String operatorLong = ni.getOperatorAlphaLong();
        log("getNetworkType: ni = " + ni + ",operatorLong = " + operatorLong);
        if (!TextUtils.isEmpty(operatorLong) && operatorLong.length() >= 2) {
            String ratStr = operatorLong.substring(operatorLong.length() - 2);
            log("getNetworkType: ratStr = " + ratStr);
            if (ratStr.equals("4G")) {
                return NETWORK_RAT_4G;
            } else if (ratStr.equals("3G")) {
                return NETWORK_RAT_3G;
            } else if (ratStr.equals("2G")) {
                return NETWORK_RAT_2G;
            }
        }
        return NETWORK_RAT_UNKNOWN;
    }

    /**
     * Update the checked status of the carrier preference, check both the
     * numerice and the network rate, check the radio of the carrier which
     * matches these two items.
     */
    private void updateCarrierPreferenceCheckedState() {
        final PreferenceGroup gsmNetworkPref = getGsmNetworkPref(mTargetSlot);
        final int preferenceCount = gsmNetworkPref.getPreferenceCount();
        log("updateCarrierPreferenceCheckedState: currentNumeric = " + mSelectedNumeric
                + ", currentNetworkRate = " + mSelectedNetworkRate
                + ", preferenceCount = " + preferenceCount);
        for (int index = 0; index < preferenceCount; index++) {
            Preference pref = gsmNetworkPref.getPreference(index);
            if (pref instanceof CarrierRadioPreference) {
                CarrierRadioPreference carrierPref = (CarrierRadioPreference) pref;
                boolean checked = carrierPref.getCarrierNumeric().equals(mSelectedNumeric)
                        && mSelectedNetworkRate == carrierPref.getCarrierRate();
                if (DBG) {
                    log("updateCarrierPreferenceCheckedState: index=" + index + " pref=" + pref
                            + " carrierPref.getCarrierNumeric()=" + carrierPref.getCarrierNumeric()
                            + " carrierPref.getCarrierRate()=" + carrierPref.getCarrierRate()
                            + " checked=" + checked);
                }
                carrierPref.setChecked(checked);
            }
        }
    }

    private void displayNetworkSeletionInProgress(String networkStr) {
        mSelectedNetworkText = getResources().getString(R.string.register_on_network, networkStr);
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_SELECTION);
        }
    }

    private void displayNetworkQueryFailed() {
        mSelectedNumeric = TelephonyManager.getTelephonyProperty(
                SubscriptionManager.getPhoneId(mSubId),
                TelephonyProperties.PROPERTY_OPERATOR_NUMERIC,
                NUMERIC_UNKNOWN);
        mSelectedNetworkRate = TelephonyManager.getNetworkClass(
            mTelephonyManager.getNetworkType(mSubId));
        updateCarrierPreferenceCheckedState();
        String status = getResources().getString(R.string.network_query_error);
        showToastAlert(status);
    }

    private void displayNetworkSelectionSucceeded() {
        updateCarrierPreferenceCheckedState();
        String status = getString(R.string.manual_network_register_hint_ok, mSelectedNetwork);
        log("displayNetworkSelectionSucceeded mTargetSlot=" + mTargetSlot + ", status=" + status);
        if (mIsForeground) {
            showDialogAlert(status);
        }
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status = getString(R.string.manual_network_register_hint_fail, mSelectedNetwork);
        if (mIsForeground) {
            showDialogAlert(status);
        }
    }

    private void showDialogAlert(String message) {
        Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = builder.setMessage(message)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        log("onClick OK.");
                    }
                }).create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    private void showToastAlert(String status) {
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    private void log(String msg) {
        Log.d("@M_" + TAG, "[MNWS" + mTargetSlot + "]" + msg);
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop, removed force quit gsm network network query.");
    }

    private void forceQuitGSMNetworkQuery() {
        log("forceQuitGSMNetworkQuery");
        // Request that the service stop the query with this callback object.
        try {
            if (mNetworkQueryService != null) {
                mNetworkQueryService.stopNetworkQuery(mCallback);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        removeDialog(DIALOG_NETWORK_LIST_LOAD);
        mRetryTimeoutHandler.removeCallbacks(mRetryTimeoutRunnable);
        mNewQuery = false;
        mRetryTimes = 0;
    }

    private String getSwitchNetworkFailErrorMessage() {
        String networkString = getNetworkOperatorName();
        String errorMessage;

        if (networkString != null) {
            errorMessage = getString(R.string.manual_network_register_hint_fail,
                    networkString);
        } else {
            errorMessage = getString(R.string.manual_network_register_hint_fail_null);
        }
        return errorMessage;
    }

    private int getExternalModemSlot() {
      int sExternalMD = -1;
        if (sExternalMD < 0) {
            sExternalMD = SystemProperties.getInt("ril.external.md", 0);
        }
        log("getExternalModemSlot:" + (sExternalMD - 1));
        return (sExternalMD - 1);
    }

    private String getNetworkOperatorName() {
        String  nWOpName = null;
        if (mTelephonyManager != null) {
            nWOpName = mTelephonyManager.getNetworkOperatorName(mSubId);
        }
        return nWOpName;
    }

    private void createPhoneStateListener() {
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneStateListener(mSubId) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.i("@M_" + TAG, "onServiceStateChanged, serviceState: " + serviceState);
                    mServiceState = serviceState;
                    updateScreen();
                }
            };
        }
    }
}
