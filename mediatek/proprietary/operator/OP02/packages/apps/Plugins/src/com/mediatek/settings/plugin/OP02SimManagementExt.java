package com.mediatek.settings.plugin;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.op02.plugin.R;
import com.mediatek.settings.ext.DefaultSimManagementExt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * CU sim switch.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISimManagementExt")
public class OP02SimManagementExt extends DefaultSimManagementExt {

    private static final String TAG = "OP02SimManagementExt";
    private Context mContext;
    private AlertDialog mDialog;
    private SubscriptionManager mSubscriptionManager = null;
    private int[] mSubscriptionIdListCache = null;
    ArrayAdapter<String> mAdapter = null;


    private BroadcastReceiver mHotSwapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int[] subscriptionIdListCurrent = mSubscriptionManager.getActiveSubscriptionIdList();
            print("current subId list: ", subscriptionIdListCurrent);
            boolean isEqual = true;
            isEqual = Arrays.equals(mSubscriptionIdListCache, subscriptionIdListCurrent);
            Log.d(TAG, "isEqual: " + isEqual);
            if (!isEqual && mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
            if (isEqual && mDialog != null && mDialog.isShowing()) {
                Log.d(TAG, "update dialog!");
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
    * Constructor method.
    * @param context is Settings's context.
    */
    public OP02SimManagementExt(Context context) {
        super();
        mContext = context;
        mSubscriptionManager = SubscriptionManager.from(mContext);
    }

    private void setDefaultDataSubId(final int subId) {
        if (TelecomManager.from(mContext).isInCall()) {
            String textErr =
                    mContext.getResources().getString(R.string.default_data_switch_err_msg1);
            Toast.makeText(mContext, textErr, Toast.LENGTH_SHORT).show();
            return;
        }
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        setDataState(subId);
        subscriptionManager.setDefaultDataSubId(subId);
        String text = mContext.getResources().getString(R.string.data_switch_started);
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }

    private void print(String msg, int[] lists) {
        if (lists != null) {
            for (int i : lists) {
                Log.d(TAG, msg + i);
            }
        } else {
            Log.d(TAG, msg + "is null");
        }
    }

    private void showSimDialog() {
        final ArrayList<String> list = new ArrayList<String>();
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getActiveSubscriptionInfoList();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();

        for (int i = 0; i < selectableSubInfoLength; ++i) {
            final SubscriptionInfo sir = subInfoList.get(i);
            CharSequence displayName = sir.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }
        String[] arr = list.toArray(new String[0]);

        mAdapter = new SelectAccountListAdapter(subInfoList,
                mContext, R.layout.select_account_list_item, arr);

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {
                        Log.d(TAG, "click dialog value: " + value);
                        SubscriptionInfo sir = subInfoList.get(value);
                        setDefaultDataSubId(sir.getSubscriptionId());
                    }
                };

        final DialogInterface.OnDismissListener dismissListener =
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.d(TAG, "dialog onDismiss called!");
                        mContext.unregisterReceiver(mHotSwapReceiver);
                        mAdapter = null;
                    }
                };

        AlertDialog.Builder builder =
                new AlertDialog.Builder(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(R.string.select_sim_for_data);
        builder.setAdapter(mAdapter, selectionListener);
        builder.setOnDismissListener(dismissListener);
        mDialog = builder.create();
        mDialog.setCancelable(false);
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        mSubscriptionIdListCache = mSubscriptionManager.getActiveSubscriptionIdList();
        print("Init subId list: ", mSubscriptionIdListCache);
        IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mContext.registerReceiver(mHotSwapReceiver, filter);
        mDialog.show();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TelephonyIntents.ACTION_SIM_STATE_CHANGED) {
                int status = RadioCapabilitySwitchUtil.isNeedShowSimDialog();
                Log.d(TAG, "onReceive ACTION_SIM_STATE_CHANGED status = " + status);
                if (status == RadioCapabilitySwitchUtil.SHOW_DIALOG) {
                    showSimDialog();
                }
                if (status != RadioCapabilitySwitchUtil.IMSI_NOT_READY_OR_SIM_LOCKED &&
                        status != RadioCapabilitySwitchUtil.ICCID_ERROR) {
                    mContext.unregisterReceiver(this);
                }
            }
        }
    };

    /**
     * Called when SIM dialog is about to show for SIM info changed
     * @return false if plug-in do not need SIM dialog
     */
    @Override
    public boolean isSimDialogNeeded() {
        /*
        int status = RadioCapabilitySwitchUtil.isNeedShowSimDialog();
        Log.d(TAG, "isNeedShowSimDialog() = " + status);
        if (status == RadioCapabilitySwitchUtil.SHOW_DIALOG) {
            showSimDialog();
        }
        if (status == RadioCapabilitySwitchUtil.IMSI_NOT_READY_OR_SIM_LOCKED
                || status == RadioCapabilitySwitchUtil.ICCID_ERROR) {
            IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
        }
        */
        return false;
    }

    @Override
    public void setDataState(int subid) {
        TelephonyManager mTelephonyManager;
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        Log.d(TAG, "setDataState subid=" + subid);
        mTelephonyManager = TelephonyManager.from(mContext);
        boolean enableBefore = mTelephonyManager.getDataEnabled();
        Log.d(TAG, "setDataState enable_before=" + enableBefore);
        int mResetSubId = subscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "setDataState mResetSubId=" + mResetSubId);
        if (subscriptionManager.isValidSubscriptionId(subid) &&
                subid != mResetSubId) {
            subscriptionManager.setDefaultDataSubId(subid);
            if (enableBefore) {
                mTelephonyManager.setDataEnabled(subid, true);
                mTelephonyManager.setDataEnabled(mResetSubId, false);
            }
        }
    }

    /**
    * Adapter for sim list view in the dialog.
    */
    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private final static float OPACITY = 0.54f;
        private int mResId;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mSubInfoList = subInfoList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
            mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = null;
            final ViewHolder holder;

            if (convertView == null) {
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.mTitle = (TextView) rowView.findViewById(R.id.title);
                holder.mSummary = (TextView) rowView.findViewById(R.id.summary);
                holder.mIcon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
            if (mSubInfoList == null) {
                Log.d(TAG, "mSubInfoList is null");
                return rowView;
            }
            int size = mSubInfoList.size();
            if (size <= position) {
                Log.d(TAG, "mSubInfoList size = " + size);
                return rowView;
            }
            final SubscriptionInfo sir = mSubInfoList.get(position);
            if (sir == null) {
                Log.d(TAG, "position " + position + " is null!");
                holder.mTitle.setText(getItem(position));
                holder.mSummary.setText("");
                holder.mIcon.setImageDrawable(mContext.getResources()
                        .getDrawable(R.drawable.ic_live_help));
                holder.mIcon.setAlpha(OPACITY);
            } else {
                Log.d(TAG, "Title: " + sir.getDisplayName() + " Number: " + sir.getNumber());
                holder.mTitle.setText(sir.getDisplayName());
                holder.mSummary.setText(sir.getNumber());
                holder.mIcon.setImageBitmap(sir.createIconBitmap(mContext));
            }
            return rowView;
        }
        /**
        * ViewHolder for each line of the list view.
        */
        private class ViewHolder {
            TextView mTitle;
            TextView mSummary;
            ImageView mIcon;
        }
    }
}
