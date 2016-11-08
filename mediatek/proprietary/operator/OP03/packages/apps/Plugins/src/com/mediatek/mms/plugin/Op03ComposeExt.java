package com.mediatek.mms.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.mms.ui.ComposeMessageActivity;
import com.mediatek.mms.callback.IComposeActivityCallback;
import com.mediatek.op03.plugin.R;
import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpComposeExt;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpComposeExt")
public class Op03ComposeExt extends DefaultOpComposeExt {
    private Handler mHandler;
    private Context mContext;
    private IComposeActivityCallback mComposeActivityCallback;
    private Activity mActivity;

    public Op03ComposeExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public int getSmsEncodingType(int encodingType, Context context) {
        return Op03MessageUtils.getSmsEncodingType(context);
    }

    @Override
    public void onCreate(IComposeActivityCallback ipComposeActivityCallback, Intent intent,
            IntentFilter intentFilter, Activity oldCompose, Activity compose,
            Bundle savedInstanceState, Handler uiHandler, ImageButton shareButton,
            LinearLayout panel, EditText textEditor) {
            mHandler = uiHandler;
            mComposeActivityCallback = ipComposeActivityCallback;
            mActivity = compose;
    }

    @Override
    public boolean checkConditionsAndSendMessage(boolean isMms, final boolean bcheckMode) {
        final ConnectivityManager mConnMgr = (ConnectivityManager) mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        final TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);

        if (isMms && mConnMgr != null && tm != null && !mConnMgr.getMobileDataEnabled() &&
                tm.isNetworkRoaming()) {
            mHandler.post(new Runnable() {
                   public void run() {
                       AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setTitle(mContext.getString(R.string.roaming));
                        builder.setCancelable(false);
                        builder.setMessage(mContext.getString(R.string.roaming_warning));
                        builder.setPositiveButton(mContext.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                /// M: Code analyze 030, Check condition before sending message.@{
                                mComposeActivityCallback.callbackCheckBeforeSendMessage(bcheckMode);
                                dialog.dismiss();
                                    /// @}
                                }
                        });
                        builder.setNegativeButton(mContext.getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mComposeActivityCallback.callbackUpdateSendButtonState();
                            }
                        });
                        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode,
                                    KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    dialog.dismiss();
                                    mComposeActivityCallback.callbackUpdateSendButtonState();
                                }
                                return false;
                            }
                        });
                        builder.show();
                   }
                });

        }
        else {
            mComposeActivityCallback.callbackCheckBeforeSendMessage(bcheckMode);
        }
        return true;
    }
}
