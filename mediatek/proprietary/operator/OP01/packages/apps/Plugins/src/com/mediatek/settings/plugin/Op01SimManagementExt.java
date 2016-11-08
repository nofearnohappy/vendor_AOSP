package com.mediatek.settings.plugin;

import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.op01.plugin.DataSwitchDialog;
import com.mediatek.op01.plugin.R;
import com.mediatek.widget.AccountViewAdapter.AccountElements;
import com.mediatek.settings.ext.DefaultSimManagementExt;

import java.util.ArrayList;
import java.util.List;

@PluginImpl(interfaceName="com.mediatek.settings.ext.ISimManagementExt")
public class Op01SimManagementExt extends DefaultSimManagementExt {

    private static final String TAG = "OP01SimManagementExt";

    private Context mContext;
    PreferenceFragment mPrefFragment;
    private boolean mIsDataSwitchWaiting = false;
    private int mToCloseSlot = -1;
    private int mSimMode;
    private ContentResolver mContentObserver;
    private IntentFilter mIntentFilter;

    private static final int DATA_SWITCH_TIME_OUT_MSG = 2000;
    private static final int MODE_PHONE_ALL = 3;
    private static final int MODE_PHONE1_ONLY = 1;
    private static final int MODE_PHONE2_ONLY = 2;
    private static final String SIM_COLOR = "sim_color";
    private static final String KEY_3G_SERVICE_SETTING = "3g_service_settings";
    private static final String KEY_AUTO_WAP_PUSH = "wap_push_settings";
    private static final String KEY_SIM_STATUS = "status_info";
    private static final String USIM = "USIM";
    private static final String LTE_SUPPORT = "1";
    private static final String VOLTE_SUPPORT = "1";
    private static final String RCS_SUPPORT = "1";
    private static final int VOLTE_DIALOG = 0x01;
    private static final int RCS_DIALOG = 0x02;
    private static final int SMS_PICK = 2;
    private static final String ACTION_KEY = "switch_data_sub";
    private static final String[] MCCMNC_TABLE_TYPE_CMCC = {
        "46000", "46002", "46007"};
    private static final String[] MCCMNC_TABLE_TYPE_CU = {
        "46001", "46006", "46009", "45407", "46005"};
    private static final String[] MCCMNC_TABLE_TYPE_CT = {
        "45502", "46003", "46011", "46012", "46013"};
    private static final String DATA_SWITCH_CLASS = "com.mediatek.op01.plugin.DataSwitchDialog";

    // Subinfo record change listener.
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("@M_" + TAG, "mSubReceiver action = " + action);
                if (action.equals(
                        TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                    String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                    // M: just process default type data change, avoid unnecessary change broadcast
                    if (PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList)) {
                    //M:Auto open the other card's data connection,when current card is radio off
                        Log.d("@M_" + TAG, "mSubReceiver,dealWithDataConnChanged...");
                        dealWithDataConnChanged(intent);
                    // @}
                    }
                }
            }
    };

    //Timeout handler
    private Handler mTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (DATA_SWITCH_TIME_OUT_MSG == msg.what) {

                Log.i("@M_" + TAG, "reveive time out msg...");
                if (mIsDataSwitchWaiting) {

                    mTimerHandler.removeMessages(DATA_SWITCH_TIME_OUT_MSG);
                    mIsDataSwitchWaiting = false;
                }
            }
        }
    };

    /**
     * update the preference screen of sim management
     * @param parent parent preference
     */
    public Op01SimManagementExt(Context context) {
        super();
        mContext = context;
        Log.d("@M_" + TAG, "mContext = " + mContext);
        mContentObserver = mContext.getContentResolver();
    }

    private void registerObserver() {
        Log.d("@M_" + TAG, "registerObserver()");
        mSimMode = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.MSIM_MODE_SETTING, -1);

        if (mContentObserver != null) {
            mContentObserver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.MSIM_MODE_SETTING),
                    true, mMsimModeValue);
        } else {
            Log.d("@M_" + TAG, "observer is null");
        }
    }

    private void unregisterObserver() {
        Log.d("@M_" + TAG, "unregisterObserver()");
        if (mContentObserver != null) {
            mContentObserver.unregisterContentObserver(mMsimModeValue);
        } else {
            Log.d("@M_" + TAG, "observer is null");
        }
    }

    private ContentObserver mMsimModeValue = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            int simModevalue = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.MSIM_MODE_SETTING, -1);

            Log.d("@M_" + TAG, "onChange(), simMode " + mSimMode + " to " + simModevalue);
            if (mSimMode == MODE_PHONE_ALL) {
                if (simModevalue == MODE_PHONE1_ONLY) {
                    closeSimSlot(1);
                } else if (simModevalue == MODE_PHONE2_ONLY) {
                    closeSimSlot(0);
                }
            } else {
                closeSimSlot(-1);
            }
            mSimMode = simModevalue;
        };
    };

    public void hideSimEditorView(View view, Context context) {
        Log.d("@M_" + TAG, "hideSimEditorView()");
        if (view != null) {
            Resources res = context.getResources();
            String packageName = context.getPackageName();

            LinearLayout layout =
                (LinearLayout) view.findViewById(res.getIdentifier(SIM_COLOR, "id", packageName));

            if (layout != null) {
                //hide sim color item
                layout.setVisibility(View.GONE);
            }
        }
    }

    public void onResume(Context context) {
        Log.d("@M_" + TAG, "onResume");
        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mSubReceiver, mIntentFilter);
        registerObserver();
        dealWithDataConnChanged(null);
    }

    private void dealWithDataConnChanged(Intent intent) {

        Log.d("@M_" + TAG, "dealWithDataConnChanged: mToClosedSimCard is " + mToCloseSlot);
        //remove confrm dialog
        int curConSubId = SubscriptionManager.getDefaultDataSubId();

        if (mToCloseSlot >= 0) {

            int toCloseSubId = getSubIdBySlot(mToCloseSlot);
            Log.i("@M_" + TAG, "dealWithDataConnChanged: toCloseSimId is " + toCloseSubId);
            Log.i("@M_" + TAG, "dealWithDataConnChanged: curConSimId is " + curConSubId);

            if (toCloseSubId != curConSubId) {
                Log.d("@M_" + TAG, "toCloseSubId != curConSubId,set mToCloseSlot -1");
                mToCloseSlot = -1;
            }
         }
        //remove waiting dialog
        if (intent != null) {
            String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
            PhoneConstants.DataState state = getMobileDataState(intent);

            if ((state == PhoneConstants.DataState.CONNECTED)
                    || (state == PhoneConstants.DataState.DISCONNECTED)) {

                if ((PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))) {

                    if (mIsDataSwitchWaiting) {

                        mTimerHandler.removeMessages(DATA_SWITCH_TIME_OUT_MSG);
                        mIsDataSwitchWaiting = false;
                    }

                }
            }
        }
    }

    public void updateDefaultSmsSummary(Preference pref) {
        int defaultSmsSubId = SubscriptionManager.getDefaultSmsSubId();
        Log.i("@M_" + TAG, "updateDefaultSmsSummary,defaultSmsSubId=" + defaultSmsSubId);
        if (defaultSmsSubId == (int) Settings.System.SMS_SIM_SETTING_AUTO) {
            pref.setSummary(mContext.getString(R.string.gemini_default_sim_auto));
        }
    }

    public void setSmsAutoItemIcon(ImageView icon, int mDialogId, int position) {
      if (mDialogId == SMS_PICK && position > SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoCount()) {
            Log.d(TAG, "mDialogId: " + mDialogId + ", position: " + position);
            icon.setImageDrawable(mContext.getDrawable(R.drawable.mms_notification_auto_select));
      }
    }

    public int getDefaultSmsSubIdForAuto() {
        int subId = (int) Settings.System.SMS_SIM_SETTING_AUTO;
        return subId;
    }

    public void initAutoItemForSms(ArrayList<String> list,
            ArrayList<SubscriptionInfo> smsSubInfoList) {
        //ALPS01970308
        // if no  if(smsSubInfoList != null) ,smsSubInfoList.size() will happen JE
        if(smsSubInfoList != null){
            if (smsSubInfoList.size() >1 && list != null) {
                list.add(mContext.getString(R.string.gemini_default_sim_auto));
                smsSubInfoList.add(null);
            }
        }
    }

    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
        Log.d("@M_" + TAG, "showChangeDataConnDialog(), mToCloseSlot=" + mToCloseSlot);
        mPrefFragment = prefFragment;

        if (mToCloseSlot >= 0
            && SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount() > 1) {
            int curConSubId = SubscriptionManager.getDefaultDataSubId();
            int toCloseSubId = getSubIdBySlot(mToCloseSlot);
            Log.d("@M_" + TAG, "toCloseSimId= " + toCloseSubId + "curConSimId= " + curConSubId);
            TelephonyManager tm = TelephonyManager.from(mContext);
            if(curConSubId > 0 && toCloseSubId > 0){
                boolean flag1 = tm.getDataEnabled(curConSubId);
                boolean flag2 = tm.getDataEnabled(toCloseSubId);
                Log.d("@M_" + TAG, "isResumed:" + isResumed + "flag1:" + flag1 + "flag2:" + flag2);
                if (toCloseSubId == curConSubId && isResumed && (flag1 || flag2)) {
                    // if no SIM cards is enbale in datausage, then do not show dialog.
                    Intent i = new Intent();
                    i.setClassName(mContext.getPackageName(), SimMgrChangeConnDialog.class.getName());
                    i.putExtra("slotId", mToCloseSlot);
                    Log.d("@M_" + TAG, "put intent slotId " + mToCloseSlot);
                    prefFragment.getActivity().startActivity(i);
                    mToCloseSlot = -1;
                    Log.d("@M_" + TAG, "showChangeDataConnDialog(), set toCloseSimId -1");
                }
            }

        }

    }

    public void closeSimSlot(int simSlot) {
        Log.d("@M_" + TAG, "closeSimSlot = " + simSlot);
        mToCloseSlot = simSlot;
        if (mToCloseSlot >= 0
            && SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount() > 1) {
            TelecomManager telecomMgr = TelecomManager.from(mContext);
            PhoneAccountHandle handle = telecomMgr.getUserSelectedOutgoingPhoneAccount();
            if (handle == null) {
                Log.d("@M_" + TAG, "closeSimSlot handle == null,return");
                return;
            }
            PhoneAccount phoneAccount = telecomMgr.getPhoneAccount(handle);
            TelephonyManager tm = TelephonyManager.from(mContext);
            int curVoiceSubId = tm.getSubIdForPhoneAccount(phoneAccount);
            if (curVoiceSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.d("@M_" + TAG, "closeSimSlot current not valid subid, return");
                return;
            }
            int toCloseSubId = getSubIdBySlot(mToCloseSlot);
            Log.d("@M_" + TAG, "closeSimSlot curVoiceSubId = " + curVoiceSubId);
            if (toCloseSubId == curVoiceSubId && toCloseSubId >= 0) {
                int subid = getSubIdBySlot(1 - mToCloseSlot);
                if (subid >= 0) {
                    switchVoiceCallDefaultSim(subid);
                }
            }
        }
    }


    private int getSubIdBySlot(int slotId) {
        Log.d("@M_" + TAG, "SlotId = " + slotId);
        if (slotId < 0 || slotId > 1) {
            return -1;
        }
        int[] subids = SubscriptionManager.getSubId(slotId);
        int subid = -1;
        if (subids != null && subids.length >= 1) {
            subid = subids[0];
        }
        Log.d("@M_" + TAG, "GetSimIdBySlot: sub id = " + subid
                + "sim Slot = " + slotId);
        return subid;
    }


    private void switchVoiceCallDefaultSim(int subid) {
        Log.d("@M_" + TAG, "switchVoiceCallDefaultSim() with subid=" + subid);
        if(subid < 0) {
            return;
        }
        TelecomManager telecomMgr = TelecomManager.from(mContext);
        List<PhoneAccountHandle> allHandles = telecomMgr.getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : allHandles) {
            PhoneAccount phoneAccount = telecomMgr.getPhoneAccount(handle);
            TelephonyManager tm = TelephonyManager.from(mContext);
            int curSubId = tm.getSubIdForPhoneAccount(phoneAccount);
            Log.d("@M_" + TAG, "switchVoiceCallDefaultSim() subId =" + curSubId);
            if (curSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                continue;
            } else if (curSubId == subid) {
                Log.d("@M_" + TAG, "switch voice call to subid=" + subid);
                telecomMgr.setUserSelectedOutgoingPhoneAccount(handle);
                break;
            }

        }

        Log.d("@M_" + TAG, "switchVoiceCallDefaultSim() ==>end");
    }

    private PhoneConstants.DataState getMobileDataState(Intent intent) {

        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);

        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }


    @Override
    public boolean switchDefaultDataSub(Context context, int subId) {
        boolean isShow = false;
        int showType = 0;

        if (isShowVoLTEDialog(subId)) {
            isShow = true;
            showType |= VOLTE_DIALOG;
        }
        if (isShowRcsDialog(subId)) {
            isShow = true;
            showType |= RCS_DIALOG;
        }

        if (isShow) {
            Intent start = new Intent(mContext, DataSwitchDialog.class);
            start.putExtra("subId", subId);
            start.putExtra("showType", showType);
            context.startActivity(start);
            Log.d("@M_" + TAG, "switchDefaultDataSub(), showType" + showType);
            return true;
        } else {
            Log.d("@M_" + TAG, "switchDefaultDataSub(), false");
            return false;
        }
    }

    /**
     * app use to judge the Card is CMCC
     * @param slotId
     * @return true is CMCC
     */
    private boolean isCMCCCard(int subId) {
        Log.d("@M_" + TAG, "isCMCCCard, subId = " + subId);
        String simOperator = null;
        simOperator = getSimOperator(subId);
        if (simOperator != null) {
            Log.d("@M_" + TAG, "isCMCCCard, simOperator =" + simOperator);
            for (String mccmnc : MCCMNC_TABLE_TYPE_CMCC) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * app use to judge the Card is CU or CT.
     * @param slotId
     * @return true is CU or CT
     */
    private boolean isCUOrCTCard(int subId) {
        Log.d("@M_" + TAG, "isCUOrCTCard, subId = " + subId);
        String simOperator = null;
        simOperator = getSimOperator(subId);
        if (simOperator != null) {
            Log.d("@M_" + TAG, "isCUOrCTCard, simOperator =" + simOperator);
            for (String mccmnc : MCCMNC_TABLE_TYPE_CU) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }
            for (String mccmnc : MCCMNC_TABLE_TYPE_CT) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
      * @Get simConfig by TelephonyManager.getDefault().getMultiSimConfiguration()
      * @return true if the device has 2 or more slots
      */
    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants mSimConfig = TelephonyManager
                .getDefault().getMultiSimConfiguration();
        if (mSimConfig == TelephonyManager.MultiSimVariants.DSDS ||
                mSimConfig == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    /**
     * Gets the MCC+MNC (mobile country code + mobile network code)
     * of the provider of the SIM. 5 or 6 decimal digits.
     * Availability: The result of calling getSimState()
     * must be android.telephony.TelephonyManager.SIM_STATE_READY.
     * @param slotId  Indicates which SIM to query.
     * @return MCC+MNC (mobile country code + mobile network code)
     * of the provider of the SIM. 5 or 6 decimal digits.
     */
    private String getSimOperator(int subId) {
        if (subId < 0) {
            return null;
        }
        String simOperator = null;
        int status = TelephonyManager.SIM_STATE_UNKNOWN;
        int slotId = SubscriptionManager.getSlotId(subId);
        if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
             status = TelephonyManager.getDefault().getSimState(slotId);
        }
        if (status == TelephonyManager.SIM_STATE_READY) {
            simOperator = TelephonyManager.getDefault().getSimOperator(subId);
        }
        Log.d("@M_" + TAG, "getSimOperator, simOperator = " + simOperator + " subId = " + subId);
        return simOperator;
    }

    /**
     * app use to judge LTE open.
     * @return true is LTE open
     */
    private boolean isLTESupport() {
        boolean isSupport = LTE_SUPPORT.equals(
                SystemProperties.get("ro.mtk_lte_support")) ? true : false;
        return isSupport;
    }

    /**
     * app use to judge LTE open.
     * @return true is LTE open
     */
    private boolean isVoLTESupport() {
        boolean isSupport = VOLTE_SUPPORT.equals(
                SystemProperties.get("ro.mtk_volte_support")) ? true : false;
        return isSupport;
    }

    /**
     * app use to judge RCS open.
     * @return true is RCS open
     */
    private boolean isRcsSupport() {
        boolean isSupport = RCS_SUPPORT.equals(
                SystemProperties.get("ro.mtk_op01_rcs")) ? true : false;
        return isSupport;
    }

    private String getSIMType(int subId) {
        String type = null;
        if (subId > 0) {
            try {
                type = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"))
                       .getIccCardType(subId);
            } catch (RemoteException e) {
                Log.d("@M_" + TAG, "getSIMType, exception: ", e);
            }
        }
        return type;
     }

    /**
     * get the 3G/4G Capability subId.
     * @return the 3G/4G Capability subId
     */
    private int get34GCapabilitySubId() {
        int subId = -1;
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager
                                      .getService(Context.TELEPHONY_SERVICE_EX));
        if (iTelEx != null) {
            try {
                int phoneId = iTelEx.getMainCapabilityPhoneId();
                Log.d("@M_" + TAG, "subId : " + subId + ", PhoneId : " + phoneId);
                if (phoneId >= 0) {
                    subId = SubscriptionManager.getSubIdUsingPhoneId(iTelEx
                                    .getMainCapabilityPhoneId());
                    }
             } catch (RemoteException e) {
                Log.d("@M_" + TAG, "get34GCapabilitySubId FAIL to getSubId" + e.getMessage());
            }
        }
        return subId;
    }

    /**
    * app use to judge if need confirm before switch data.
    * @return false is no need confirm
    */
    private boolean isShowRcsDialog(int switchtoSubId) {
        if (!isGeminiSupport() || !isRcsSupport()) {
            Log.d("@M_" + TAG, "isShowRcsDialog(),not support");
            return false;
        }

        int subId = SubscriptionManager.getDefaultDataSubId();
        Log.d("@M_" + TAG, "subId:" + subId);
        //currently no default data card
        //if ((!isCUOrCTCard(subId) && USIM.equals(getSIMType(subId)))
        //    && (isCUOrCTCard(switchtoSubId) || !USIM.equals(getSIMType(switchtoSubId)))) {
        if ((isCMCCCard(subId) && USIM.equals(getSIMType(subId)))
            && !(isCMCCCard(switchtoSubId) && USIM.equals(getSIMType(switchtoSubId)))) {
            return true;
        } else {
            return false;
        }
    }

    /**
    * app use to judge if need confirm before switch data.
    * @return false is no need confirm
    */
    private boolean isShowVoLTEDialog(int switchtoSubId) {
        if (!isGeminiSupport() || !isLTESupport() || !isVoLTESupport()) {
            Log.d("@M_" + TAG, "isShowConfirmDialog(),not support");
            return false;
        }

        int subId = get34GCapabilitySubId();
        Log.d("@M_" + TAG, "subId:" + subId);
        //currently no major 4G card
        if (subId < 0) {
            return false;
        } else if (switchtoSubId == subId) {
            //switch data to default 4G card, only data switch, 4G isn't switch
            //Data from wwop SIM to wwop USIM, or CMCC SIM to CMCC USIM
            //or cu/ct/wwop to CMCC
            Log.d("@M_" + TAG, "isShowConfirmDialog(),0-1");
            return false;
        } else if ((switchtoSubId >= 0) && (switchtoSubId < Integer.MAX_VALUE)) {
            //(switchtoSubId != subId)
            if (isCMCCCard(switchtoSubId)) {
                if (USIM.equals(getSIMType(switchtoSubId))) {
                    //Data From CMCC USIM to CMCC USIM
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),1-1");
                    return false;
                } else if (isCMCCCard(subId) &&  (USIM.equals(getSIMType(subId)))) {
                    //Data From CMCC USIM to CMCC SIM
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),1-2");
                    return true;
                } else {
                    //Data From CMCC SIM to CMCC SIM
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),1-3");
                    return false;
                }
            } else if (isCUOrCTCard(switchtoSubId)) {
                //Data to CU or CT
                if (!isCUOrCTCard(subId) && USIM.equals(getSIMType(subId))) {
                    //Data from CMCC/wwop USIM to CU/CT
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),2-1");
                    return true;
                } else {
                    //Data from CU/CT SUIM/SIM or CMCC/wwop SIM to CU/CT
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),2-2");
                    return false;
                }
            } else {
                if (isCMCCCard(subId) && USIM.equals(getSIMType(subId))) {
                    //Data form CMCC USIM to wwop card
                    Log.d("@M_" + TAG, "isShowConfirmDialog(),3-1");
                    return true;
                } else {
                    if (!isCMCCCard(subId) && !isCUOrCTCard(subId)
                        && USIM.equals(getSIMType(subId))
                        && !USIM.equals(getSIMType(switchtoSubId))) {
                        Log.d("@M_" + TAG, "isShowConfirmDialog(),3-2");
                        //data from wwop USIM card to wwop/unkonw SIM
                        return true;
                    } else {
                        //data from CMCC/wwop SIM card to wwop/unkonw SIM
                        Log.d("@M_" + TAG, "isShowConfirmDialog(),3-3");
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    /**
     * set curSubId in dataUsage false,if curSubId == subId, return;
     * @param subid subId
     */
    public void setDataState(int subid) {
        if (subid != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        int curConSubId = SubscriptionManager.getDefaultDataSubId();
        TelephonyManager tm = TelephonyManager.from(mContext);
            Log.d("@M_" + TAG, "setDataState,curConSubId: " + curConSubId + "subid:" + subid);
            if (curConSubId == subid) {
                return;
            }
        if (tm.getDataEnabled(curConSubId) || tm.getDataEnabled(subid)) {
                Log.d("@M_" + TAG, "setDataState: setDataEnabled curConSubId false");
                tm.setDataEnabled(curConSubId, false);
                tm.setDataEnabled(subid, true);
            }
        }
    }

    /**
     * Called after setDefaultDataSubId
     * @param subid subId
     */
    public void setDataStateEnable(int subid) {
        TelephonyManager tm = TelephonyManager.from(mContext);
        if (tm.getDataEnabled(subid)) {
            Log.d("@M_" + TAG, "setDataStateEnable true subId:" + subid);
            tm.setDataEnabled(subid, true);
        }
    }

    /**
     * Called when SIM dialog is about to show for SIM info changed.
     * @return false if plug-in do not need SIM dialog
     */
    public boolean isSimDialogNeeded() {
        return false;
    }

    /**
     * Called when SimSettings fragment onPause.
     */
    public void onPause() {
        Log.d("@M_" + TAG, "onPause");
        unregisterObserver();
        mContext.unregisterReceiver(mSubReceiver);
    }

}
