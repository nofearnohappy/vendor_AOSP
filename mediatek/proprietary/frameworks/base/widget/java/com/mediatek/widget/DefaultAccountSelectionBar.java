package com.mediatek.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;

import com.mediatek.internal.R;

public class DefaultAccountSelectionBar {

    public static final String SELECT_OTHER_ACCOUNTS_ACTION = "SELECT_OTHER_ACCOUNTS";

    private final static String TAG = "DefaultAccountSelectionBar";
    private CustomAccountRemoteViews mCustomAccountRemoteViews;
    private Context mContext;
    private String mPackageName;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    private boolean mIsRegister = false;
    private BroadcastReceiver mReceiver = null;

    /**
     * 
     * @param context The context where the view associated with the notification is posted. 
     * @param packageName The package name of the component which posts the notification.
     * @param data The content of the Account {@link AccountInfo} which will be shown in the notification.
     */
    public DefaultAccountSelectionBar(Context context, String packageName, List<AccountInfo> data) {
        mContext = context;
        mPackageName = packageName;

        configureView(data);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.account_select_notification)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_MAX).build();

        mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
    }

    /**
     * Update the content of the account{@link AccountInfo}.
     * @see configureView(List<AccountInfo> data)
     * @param data The content of the Account {@link AccountInfo} which will be shown in the notification.
     */
    public void updateData(List<AccountInfo> data) {
        configureView(data);
    }

    /**
     * Show the customized notification.
     */
    public void show() {

        mNotification.contentView = mCustomAccountRemoteViews.getNormalRemoteViews();
        mNotification.bigContentView = mCustomAccountRemoteViews.getBigRemoteViews();

        mNotificationManager.notify(R.id.custom_select_default_account_notification_container, mNotification);
        Log.d(TAG, "In package show accountBar: " + mPackageName + " ,mIsRegister: " + mIsRegister);

        if (!mIsRegister && mCustomAccountRemoteViews.getOtherAccounts() != null ) {
            registerReceiver(mContext.getApplicationContext());
            mIsRegister = true;
        }
    }

    /**
     * Hide the customized notification.
     */
    public void hide() {
        mNotificationManager.cancel(R.id.custom_select_default_account_notification_container);
        Log.d(TAG, "In package hide accountBar: " + mPackageName + " ,mIsRegister: " + mIsRegister);

        if (mIsRegister && mCustomAccountRemoteViews.getOtherAccounts() != null) {
            unregisterReceiver(mContext.getApplicationContext());
            mIsRegister = false;
        }
    }

    private void configureView(List<AccountInfo> data) {
        mCustomAccountRemoteViews = new CustomAccountRemoteViews(mContext, mPackageName, data);
        mCustomAccountRemoteViews.configureView();
    }

    private void registerReceiver(Context context) {
        if (mReceiver == null) {
            mReceiver = new OtherAccountSelectionReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SELECT_OTHER_ACCOUNTS_ACTION);
        context.registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterReceiver(Context context) {
        if (mReceiver != null) {
            context.unregisterReceiver(mReceiver);
        }
        mReceiver = null;
    }

    private void hideNotification(Context context) {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(intent);
    }

    private class OtherAccountSelectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "[onReceive] action = " + action);

            final List<AccountInfo> accountItems = mCustomAccountRemoteViews.getOtherAccounts();

            if (SELECT_OTHER_ACCOUNTS_ACTION.equals(action) && mContext instanceof Activity) {

                if (((Activity)mContext).isFinishing() || ((Activity)mContext).isDestroyed()) {
                    Log.d(TAG, "--- wrong activity status ---");

                    return;
                }

                FragmentManager fm = ((Activity)mContext).getFragmentManager();

                if (fm.findFragmentByTag(DefaultAccountPickerDialog.TAG) == null) {
                    DefaultAccountPickerDialog defaultAccountPickerDialog =
                            DefaultAccountPickerDialog.build(mContext).setData(accountItems);

                    FragmentTransaction ft = ((Activity) mContext).getFragmentManager()
                            .beginTransaction();
                    ft.add(defaultAccountPickerDialog, DefaultAccountPickerDialog.TAG);
                    ft.commitAllowingStateLoss();
                }
            } else {
                Log.d(TAG, "--- wrong context ---");
            }

            hideNotification(mContext);
        }
    }
}
