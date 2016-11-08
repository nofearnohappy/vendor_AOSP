package com.mediatek.settings.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.widget.Switch;

import com.mediatek.common.PluginImpl;
import com.mediatek.op03.plugin.R;
import com.mediatek.settings.ext.DefaultDataUsageSummaryExt;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IDataUsageSummaryExt")
public class OP03DataUsageSummaryExt extends DefaultDataUsageSummaryExt {

    private static final String TAG = "OP03DataUsageSummaryExt";
    private Context mContext;
    private View.OnClickListener mDialogListener;
    private Switch mDataEnabled;
    private Activity mActivity;
    private View mView;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        Log.d("@M_" + TAG, "mDataEnabled = " + mDataEnabled);
        int message = (mDataEnabled.isChecked() ?
                R.string.networksettings_tips_data_disabled
                : R.string.networksettings_tips_data_enabled);
        AlertDialog.Builder dialogBuild = new AlertDialog.Builder(mActivity);
        dialogBuild.setMessage(mContext.getText(message))
        .setTitle(android.R.string.dialog_alert_title)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                mDialogListener.onClick(mView);
                }
        })
        .setNegativeButton(android.R.string.no, null)
        .create();
        dialogBuild.show();
        }
    };

    public OP03DataUsageSummaryExt(Context context) {
        super(context);
        mContext = context;
    }

    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, View.OnClickListener dataEnabledDialogListerner) {
        Log.d("@M_" + TAG, "setDataEnableClickListener: isChecked " + dataEnabled.isChecked() + "dataEnabledDialogListerner " + dataEnabledDialogListerner);
        mActivity = activity;
        dataEnabledView.setOnClickListener(mClickListener);
        mDataEnabled = dataEnabled;
        mView = dataEnabledView;
        mDialogListener = dataEnabledDialogListerner;
        return true;
    }
    
    public boolean needToShowDialog() {
            return false;
    }
    
    public void resetData() {
        mActivity = null;
        mDialogListener = null;
        mDataEnabled = null;
    }
}
