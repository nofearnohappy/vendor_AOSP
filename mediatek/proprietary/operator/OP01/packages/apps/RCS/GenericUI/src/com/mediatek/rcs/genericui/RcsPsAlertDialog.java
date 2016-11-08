package com.mediatek.rcs.genericui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class RcsPsAlertDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    private static final String TAG = "RcsPsAlertDialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.rcs_core_rcs_notification_title);
        p.mMessage = getString(R.string.remind_open_data);
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }
    
    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        Log.d(TAG, "onClick, button=" + button);
        if (button == DialogInterface.BUTTON_POSITIVE) {
            Intent it = new Intent(android.provider.Settings.ACTION_SETTINGS);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(it);
            return;
        }
    }

}
