package com.mediatek.systemui.plugin;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;
import android.view.View;
import android.widget.LinearLayout;
import com.mediatek.common.PluginImpl;
import android.util.Log;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.op07.plugin.R;

@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlmnPlugin")
public class Op07StatusBarPlmnPluginExt extends DefaultStatusBarPlmnPlugin {
    static final String TAG = "Op07StatusBarPlmnPluginExt";
    private String mPlmn = null;
    private TextView mPlmnTextView = null;
    Context mconxt = null;
    Context mContext = null;

    public Op07StatusBarPlmnPluginExt(Context context) {
        super(context);
        mContext = context;
        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        context.registerReceiver(mBroadcastReceiver, filter);
        if (mPlmnTextView == null)
            mPlmnTextView = new TextView(context);
    }

    /********************************************************************************/
    /********* Broadcast receiver for change in network *****************************/
    /****
     * Receives the intent to display and change the operator name on status bar
     ***/
    /********************************************************************/

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "into broadcast recevier");
            String action = intent.getAction();

            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                Log.e(TAG, "SPN_STRINGS_UPDATED_ACTION");
                updateNetworkName(intent.getBooleanExtra(
                        TelephonyIntents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(
                                TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));

            }
        }

    };

    /*****************************************************************************/
    /*************************** updateNetworkName *********************************/
    /*************************** receive the textview and add the text *************/
    /*****************************************************************************/

    private String mOldPlmn = null;

    private void updateNetworkName(boolean showSpn, String spn,
            boolean showPlmn, String plmn) {
        Log.e(TAG, "For AT&T updateNetworkName, showSpn=" + showSpn + " spn="
                + spn + " showPlmn=" + showPlmn + " plmn=" + plmn);

        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(mContext
                        .getString(R.string.status_bar_network_name_separator_plug));
            }
            str.append(spn);
            something = true;
        }
        if (something) {
            mOldPlmn = str.toString();
        } else {
            mOldPlmn = mContext.getResources().getString(
                    com.android.internal.R.string.lockscreen_carrier_default);
        }
        mPlmnTextView.setText(str);
    }

    public TextView getPlmnTextView(Context context) {
        Log.e(TAG, "return mPlmnTextView");
        Log.e(TAG, "return mPlmnTextView");
        return mPlmnTextView;
    }

    /*
     * * update the plmn when in search mode
     */
    private void updatePLMNSearchingStateView(boolean searching) {
        if (searching) {
            mPlmnTextView.setText(R.string.plmn_searching);
            Log.e(TAG, "updatePLMNSearchingStateView");
        } else {
            mPlmnTextView.setText(mOldPlmn);
            Log.e(TAG, "updatePLMNSearchingStateView");
        }
    }

    public void addPlmn(LinearLayout statusBarContents, Context sysContx) {
        Log.d(TAG, "addPlmn");
        statusBarContents.addView(mPlmnTextView, 0);
        mPlmnTextView.setVisibility(View.GONE);
    }

    public void setPlmnVisibility(int visibility) {
        mPlmnTextView.setVisibility(visibility);
    }

}