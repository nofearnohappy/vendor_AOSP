package com.mediatek.mediatekdm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class CollectSetPermissionDialog extends Activity {

    private static final String STATE_CHECKBOX = "state_checkbox";
    private static final String ON_SECOND_DIALOG = "on_second_dialog";

    private NotificationManager mNotificationManager = null;
    private Dialog mDialog = null;
    private CheckBox mCheckBox = null;

    private Intent mIntent = null;
    private boolean mIsChecked = false;
    private boolean mIsNeedNotify = true;
    private boolean mIsOnSecondDiag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "onCreate, intent is " + getIntent());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mDialog = buildConfirmDialog();
        mDialog.show();
        sendNotification();

        mIntent = getIntent();
    }

    private Dialog buildConfirmDialog() {
        Log.d(DmConst.TAG.COLLECT_SET_DIALOG, "buildConfirmDialog.");

        View layout = LayoutInflater.from(this).inflate(R.layout.notify_dialog_customview, null);
        mCheckBox = (CheckBox) layout.findViewById(R.id.checkbox);
        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsChecked = isChecked;
                mIsNeedNotify = !isChecked;
            }
        });

        mIsChecked = getIntent().getBooleanExtra(STATE_CHECKBOX, false);
        mIsNeedNotify = !mIsChecked;

        mCheckBox.setChecked(mIsChecked);

        return new AlertDialog.Builder(this).setView(layout).setCancelable(false)
                .setTitle(R.string.collect_set_permission_dlg_title)
                .setPositiveButton(R.string.alert_dlg_ok_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("@M_" + DmConst.TAG.COLLECT_SET_DIALOG, "Click positive button.");

                        dialog.dismiss();
                        clear();

                        boolean isNeedAgree = true;
                        responsePermissionDialog(isNeedAgree, mIsNeedNotify);
                    }
                }).setNegativeButton(R.string.alert_dlg_cancel_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("@M_" + DmConst.TAG.COLLECT_SET_DIALOG, "Click negative button.");

                        dialog.dismiss();
                        showSecondDialog();
                    }
                }).create();
    }

    private void showSecondDialog() {
        Log.i("@M_" + DmConst.TAG.COLLECT_SET_DIALOG, "Current is on second dialog.");
        mIsOnSecondDiag = true;

        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.confirm_dlg_title).setMessage(R.string.confirm_dlg__msg)
                .setPositiveButton(R.string.alert_dlg_ok_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("@M_" + DmConst.TAG.COLLECT_SET_DIALOG, "Click positive button.");

                        dialog.dismiss();
                        clear();

                        boolean isNeedAgree = false;
                        responsePermissionDialog(isNeedAgree, mIsNeedNotify);
                    }
                }).setNegativeButton(R.string.alert_dlg_cancel_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("@M_" + DmConst.TAG.COLLECT_SET_DIALOG, "Click negative button.");

                        dialog.dismiss();
                        clear();

                        mIsOnSecondDiag = false;
                        mIntent.putExtra(STATE_CHECKBOX, mIsChecked);
                        startActivity(mIntent);
                    }
                }).create();
        dialog.show();
    }

    private void sendNotification() {
        Intent intent = getIntent();
        intent.setClass(this, CollectSetPermissionDialog.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(
                        getResources()
                                .getString(R.string.collect_set_permission_notification_title))
                .setContentText(
                        getResources().getString(R.string.collect_set_permission_notification_msg))
                .setTicker(
                        getResources().getString(
                                R.string.collect_set_permission_notification_tickerText))
                .setSmallIcon(R.drawable.perm_group_turn_on_data_connection)
                .setContentIntent(pendingIntent).build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        mNotificationManager.notify(
                DmConst.NotificationInteractionType.TYPE_COLLECT_SET_PERM_NOTIFICATION,
                notification);
    }

    private void clear() {
        this.finish();
        mNotificationManager
                .cancel(DmConst.NotificationInteractionType.TYPE_COLLECT_SET_PERM_NOTIFICATION);
    }

    private void responsePermissionDialog(boolean isNeedAgree, boolean isNeedNotify) {
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "[responsePermissionDialog] isNeedAgree is "
                + isNeedAgree + ", isNeedNotify is " + isNeedNotify);
        Intent intent = getIntent();
        intent.putExtra(DmConst.ExtraKey.IS_NEED_AGREE, isNeedAgree);
        intent.putExtra(DmConst.ExtraKey.IS_NEED_NOTIFY, isNeedNotify);
        intent.setAction(DmConst.IntentAction.DM_COLLECT_SET_DIALOG_END);
        intent.setClass(this, DmReceiver.class);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCheckBox.setText(getResources().getText(R.string.collect_set_permission_dlg_check_hint));
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "Checkbox text is " + mCheckBox.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "onRestoreInstanceState");

        if (savedInstanceState != null) {
            Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "[onRestore] mIsOnSecondDiag is "
                    + savedInstanceState.getBoolean(ON_SECOND_DIALOG));
            Log.i(DmConst.TAG.COLLECT_SET_DIALOG,
                    "[onRestore] isChecked is " + savedInstanceState.getBoolean(STATE_CHECKBOX));

            mCheckBox.setChecked(savedInstanceState.getBoolean(STATE_CHECKBOX));
            if (savedInstanceState.getBoolean(ON_SECOND_DIALOG)) {
                mDialog.dismiss();
                showSecondDialog();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "onSaveInstanceState");
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "[onSave] mIsOnSecondDiag is " + mIsOnSecondDiag);
        Log.i(DmConst.TAG.COLLECT_SET_DIALOG, "[onSave] mIsChecked is " + mIsChecked);

        outState.putBoolean(STATE_CHECKBOX, mIsChecked);
        outState.putBoolean(ON_SECOND_DIALOG, mIsOnSecondDiag);
    }
}
