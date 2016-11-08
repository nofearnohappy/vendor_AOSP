package com.mediatek.systemui.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;
import com.mediatek.settings.plugin.PlmnDisplaySettingsExt;

/**
 * M: OP03 implementation of Plug-in definition of Status bar. This class
 * extends TextView, implements IStatusBarPlugin interface This is done to allow
 * or class to inherit from the TextView class
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlmnPlugin")
public class Op03StatusBarPlmnPlugin extends DefaultStatusBarPlmnPlugin {
    static final String TAG = "Op03StatusBarPlmnPlugin";
    private String mPlmn = null;
    private TextView mPlmnTextView = null;
    StringBuilder mstrSub = null;
    StringBuilder mstr = null;
    boolean mshowText = false;
    String mshowTextStr = "false";
    Context mconxt = null;
    Context msystemContext = null;
    SharedPreferences mprefs = null;
    private final String SETTING_SERVICE = "setting_service_intent";
    private final String PLMN_SETTING_STATE = "plmn_setting_state";
    private final String MSHOWTEXT = "mshowText";

    /************************************************************************/
    /*********************** constructor ***************************************/
    /********** this is a TextView, we will receive the parameters in the constructor to **/
    /** use the plmn to show the text *******************************************/

    public Op03StatusBarPlmnPlugin(Context context) {
        super(context);
        mconxt = context;
        mstrSub = new StringBuilder();
        mstr = new StringBuilder();
        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(PlmnDisplaySettingsExt.ACTION_PLMN_CHANGED);
        filter.addAction("com.mediatek.settings.PLMN_TEXT_SETTING");
        context.registerReceiver(mBroadcastReceiver, filter);
        if (mPlmnTextView == null) {
            mPlmnTextView = new TextView(context);
        }
        mshowText = getShowText();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "into broadcast recevier");
            String action = intent.getAction();
            boolean dataShared = false;
            StringBuilder str = new StringBuilder();
            Log.d(TAG, "Into received intent " + action);
            if (PlmnDisplaySettingsExt.ACTION_PLMN_CHANGED.equals(action)) {
                String message = intent.getStringExtra("plmnSettingCheck");
                Log.d(TAG, "received the intent");
                Log.d(TAG, message);
                if (message.equals("true")) {
                    mshowText = true;
                    mshowTextStr = "true";
                } else {
                    mshowText = false;
                    mshowTextStr = "false";
                }
                try {
                    Log.d(TAG, "write into setSettingparameter");
                    // plmnInterface.setSettingparameter(PLMN_SETTING_STATE,mshowTextStr);
                } catch (Exception e) {
                    Log.d(TAG,
                            "Fail to write using setSettingparameter catch exception");
                }
                android.provider.Settings.System.putInt(mconxt
                        .getContentResolver(), MSHOWTEXT, mshowText ? 1 : 0);
            } else if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                StringBuilder strTemp = new StringBuilder();
                mstr = strTemp;
                mstr.append(intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
                boolean showPlmn = false;
            } else {
                Log.e(TAG, "Unhandled intent");
            }

            if (mshowText == true) {
                mPlmnTextView.setText(mstr.toString(),
                        TextView.BufferType.NORMAL);
                Log.d(TAG, "true to show now mstr= " + mstr);
            } else {
                mPlmnTextView.setText("", TextView.BufferType.NORMAL);
                Log.d(TAG, "false dont show now = mstr" + mstr);
            }
            Log.d(TAG, "set text of textview = " + str);
        }
    };

    private boolean getShowText() {
        int showText = android.provider.Settings.System.getInt(
                mconxt.getContentResolver(), MSHOWTEXT, -1);
        Log.d(TAG, "getShowTextParameter" + showText);
        if (showText == 1) {
            return true;
        } else {
            return false;
        }

    }

    public void addPlmn(LinearLayout statusBarContents, Context sysContx) {
        Log.d(TAG, "addPlmn");
        msystemContext = sysContx;
        statusBarContents.addView(mPlmnTextView, 0);
        mPlmnTextView.setVisibility(View.GONE);
    }

    public void setPlmnVisibility(int visibility) {
        mPlmnTextView.setVisibility(visibility);
    }

}
