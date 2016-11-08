package com.mediatek.browser.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import com.mediatek.op01.plugin.R;

public class Op01BrowserDataConnectionDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    private CheckBox mCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.dialog_title);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.browser_confirm_dialog, null);
        TextView contentView = (TextView) view.findViewById(R.id.content);
        contentView.setText(getString(R.string.wifi_failover_gprs_content));
        mCb = (CheckBox) view.findViewById(R.id.setPrimary);
        return view;
    }

    public void onClick(DialogInterface dialog, int which) {
        TelephonyManager teleMgr =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (teleMgr != null) {
                    teleMgr.setDataEnabled(true);
                }
                setResult(mCb.isChecked() ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                if (teleMgr != null) {
                    teleMgr.setDataEnabled(false);
                }
                setResult(mCb.isChecked() ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
                finish();
                break;

            default:
                break;
        }
    }
}
