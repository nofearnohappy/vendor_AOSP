package com.mediatek.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.mediatek.internal.R;

public class CustomAccountRemoteViews {

    private static final String TAG = "CustomAccountRemoteViews";
    private static final int ROWACCOUNTNUMBER = 4;
    private static final int MOSTACCOUNTNUMBER = 8;

    private RemoteViews mNormalRemoteViews;
    private RemoteViews mBigRemoteViews;
    private Context mContext;
    private int mRequestCode;

    private List<AccountInfo> mData;
    private List<AccountInfo> mOtherAccounts = new ArrayList<AccountInfo>();

    private final int RESOURCE_ID[][] = {
            { R.id.account_zero_container, R.id.account_zero_img, R.id.account_zero_name,
              R.id.account_zero_number, R.id.account_zero_normal_divider,
              R.id.account_zero_highlight_divider, R.id.account_zero_normal_divider_more,
              R.id.account_zero_highlight_divider_more },
            { R.id.account_one_container, R.id.account_one_img, R.id.account_one_name,
              R.id.account_one_number, R.id.account_one_normal_divider,
              R.id.account_one_highlight_divider, R.id.account_one_normal_divider_more,
              R.id.account_one_highlight_divider_more },
            { R.id.account_two_container, R.id.account_two_img, R.id.account_two_name,
              R.id.account_two_number, R.id.account_two_normal_divider,
              R.id.account_two_highlight_divider, R.id.account_two_normal_divider_more,
              R.id.account_two_highlight_divider_more },
            { R.id.account_three_container, R.id.account_three_img, R.id.account_three_name,
              R.id.account_three_number, R.id.account_three_normal_divider,
              R.id.account_three_highlight_divider, R.id.account_three_normal_divider_more,
              R.id.account_three_highlight_divider_more },
            { R.id.account_four_container, R.id.account_four_img, R.id.account_four_name,
              R.id.account_four_number, R.id.account_four_normal_divider,
              R.id.account_four_highlight_divider, R.id.account_four_normal_divider_more,
              R.id.account_four_highlight_divider_more },
            { R.id.account_five_container, R.id.account_five_img, R.id.account_five_name,
              R.id.account_five_number, R.id.account_five_normal_divider,
              R.id.account_five_highlight_divider, R.id.account_five_normal_divider_more,
              R.id.account_five_highlight_divider_more },
            { R.id.account_six_container, R.id.account_six_img, R.id.account_six_name,
              R.id.account_six_number, R.id.account_six_normal_divider,
              R.id.account_six_highlight_divider, R.id.account_six_normal_divider_more,
              R.id.account_six_highlight_divider_more },
            { R.id.account_seven_container, R.id.account_seven_img, R.id.account_seven_name,
              R.id.account_seven_number, R.id.account_seven_normal_divider,
              R.id.account_seven_highlight_divider, R.id.account_seven_normal_divider_more,
              R.id.account_seven_highlight_divider_more } };

    private final class IdIndex {

        public static final int CONTAINER_ID = 0;
        public static final int IMG_ID = 1;
        public static final int NAME_ID = 2;
        public static final int NUMBER_ID = 3;
        public static final int NORMAL_DIVIDER_ID = 4;
        public static final int HIGHTLIGHT_DIVIDER_ID = 5;
        public static final int NORMAL_DIVIDER_MORE_ID = 6;
        public static final int HIGHTLIGHT_DIVIDER_MORE_ID = 7;

        private IdIndex() {
        }
    }

    /**
     * Constructor
     * @param context The context where the view associated with the notification is posted.
     * @param packageName The package name of the component which posts the notification.
     */
    public CustomAccountRemoteViews(Context context, String packageName) {

        this(context, packageName, null);
    }

    /**
     * Constructor
     * @param context The context where the view associated with the notification is posted.
     * @param packageName The package name of the component which posts the notification.
     * @param data The content of the Account {@link AccountInfo} which will be shown in the notification.
     */
    public CustomAccountRemoteViews(Context context, String packageName,
            final List<AccountInfo> data) {

        mNormalRemoteViews = new RemoteViews(packageName,
                R.layout.normal_default_account_select_title);
        mBigRemoteViews = new RemoteViews(packageName,
                R.layout.custom_select_default_account_notification);

        mData = data;
        mContext = context;
        mRequestCode = 0;
    }

    /**
     *
     * @return The normal remoteviews {@link RemoteViews} of the notification.
     * @see Notification
     */
    public RemoteViews getNormalRemoteViews() {
        return mNormalRemoteViews;
    }

    /**
     *
     * @return The big remoteviews {@link RemoteViews} of the notification.
     * @see Notification
     */
    public RemoteViews getBigRemoteViews() {
        return mBigRemoteViews;
    }

    /**
     *
     * @param data The content of the Account {@link AccountInfo} which will be shown in
     *  the notification.
     */
    public void setData(final List<AccountInfo> data) {
        mData = data;
    }

    /**
     * Configure all the account views of the notification.
     */
    public void configureView() {
        boolean showRowTwo;
        // The number of the account slection item.
        int itemCount;

        if (mData != null) {

            Log.d(TAG, "---configureView---" + mData.size());

            resetAccounts();

            // Classify accounts by whether it is SIM account
            List<AccountInfo> simAccounts = new ArrayList<AccountInfo>();
            for (AccountInfo account : mData) {
                if (account.isSimAccount()) {
                    simAccounts.add(account);
                } else {
                    mOtherAccounts.add(account);
                }
            }

            // Judge whether we need show the second row container
            if (mOtherAccounts.size() > 0) {
                itemCount = simAccounts.size() + 1;
                showRowTwo = itemCount > ROWACCOUNTNUMBER;
            } else {
                itemCount = simAccounts.size();
                showRowTwo = itemCount > ROWACCOUNTNUMBER;
            }

            if (showRowTwo) {
                mBigRemoteViews.setViewVisibility(R.id.select_account_row_two_container,
                        View.VISIBLE);
            } else {
                mBigRemoteViews.setViewVisibility(R.id.select_account_row_two_container, View.GONE);
            }

            // Configure account selection items, SIM account, SIM account... other account
            for (int i = 0; i < itemCount && i < MOSTACCOUNTNUMBER; i++) {
                Log.d(TAG, "--- configure account id: " + i + ", mOtherAccounts.size = "
                        + mOtherAccounts.size());

                if (i == itemCount - 1 && mOtherAccounts.size() > 0) {
                    if (mOtherAccounts.size() > 1) {
                        if (hasActiveAccount(mOtherAccounts)) {
                            AccountInfo activeAccount = getActiveAccount(mOtherAccounts);
                            AccountInfo otherAccount = generateOtherAccount(activeAccount);
                            configureAccount(RESOURCE_ID[i], otherAccount);
                        } else {
                            AccountInfo otherAccount = generateOtherAccount();
                            otherAccount.setActiveStatus(false);
                            configureAccount(RESOURCE_ID[i], otherAccount);
                        }
                    } else if (mOtherAccounts.size() == 1) {
                        AccountInfo accountInfo = mOtherAccounts.get(0);
                        int resourceId[] = RESOURCE_ID[i];
                        configureAccount(resourceId, accountInfo);
                    }
                } else {
                    AccountInfo accountInfo = simAccounts.get(i);
                    int resourceId[] = RESOURCE_ID[i];
                    configureAccount(resourceId, accountInfo);
                }
            }

        } else {
            Log.w(TAG, "Data can not be null");
        }
    }

    /**
     * @return other accounts
     */
    public List<AccountInfo> getOtherAccounts() {
        return mOtherAccounts;
    }

    private void resetAccounts() {
        for (int i = 0; i < ROWACCOUNTNUMBER; i++) {
            mBigRemoteViews.setViewVisibility(RESOURCE_ID[i][0], View.GONE);
        }
        for (int i = ROWACCOUNTNUMBER; i < MOSTACCOUNTNUMBER; i++) {
            mBigRemoteViews.setViewVisibility(RESOURCE_ID[i][0], View.INVISIBLE);
        }
    }

    private boolean hasActiveAccount(List<AccountInfo> accounts) {
        for(AccountInfo account : accounts) {
            if(account.isActive()) {
                return true;
            }
        }
        return false;
    }

    private AccountInfo getActiveAccount(List<AccountInfo> accounts) {
        for(AccountInfo account : accounts) {
            if(account.isActive()) {
                return account;
            }
        }
        return null;
    }

    private AccountInfo generateOtherAccount() {
        Intent otherIntent = new Intent(DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION);
        String other_accounts = mContext.getString(R.string.other_accounts);
        AccountInfo otherAccount = new AccountInfo(com.mediatek.R.drawable.other_accounts_icon,
                other_accounts, null,
                otherIntent);
        return otherAccount;
    }

    private AccountInfo generateOtherAccount(AccountInfo accountInfo) {
        Intent otherIntent = new Intent(DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION);
        String other_accounts = mContext.getString(R.string.other_accounts);
        AccountInfo otherAccount = new AccountInfo(accountInfo.getIconId(), accountInfo.getIcon(),
                accountInfo.getLabel(), accountInfo.getNumber(), otherIntent,
                accountInfo.isActive(), accountInfo.isSimAccount());
        return otherAccount;
    }

    private void configureAccount(int resourceId[], AccountInfo accountInfo) {

        if (accountInfo.getIcon() != null) {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.CONTAINER_ID], View.VISIBLE);
            mBigRemoteViews.setImageViewBitmap(resourceId[IdIndex.IMG_ID], accountInfo.getIcon());
        } else if (accountInfo.getIconId() != 0) {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.CONTAINER_ID], View.VISIBLE);
            mBigRemoteViews.setImageViewResource(resourceId[IdIndex.IMG_ID], accountInfo.getIconId());
        } else {
            Log.w(TAG, "--- The icon of account is null ---");
        }

        if (accountInfo.getLabel() == null) {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NAME_ID], View.GONE);
        } else {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NAME_ID], View.VISIBLE);
            mBigRemoteViews.setTextViewText(resourceId[IdIndex.NAME_ID], accountInfo.getLabel());
        }

        if (accountInfo.getNumber() == null) {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NUMBER_ID], View.GONE);
        } else {
            mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NUMBER_ID], View.VISIBLE);
            mBigRemoteViews.setTextViewText(resourceId[IdIndex.NUMBER_ID], accountInfo.getNumber());
        }

        Log.d(TAG, "active: " + accountInfo.isActive());

        if (accountInfo.isActive()) {
            if (DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION.equals(
                    accountInfo.getIntent().getAction())) {
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_MORE_ID],
                        View.VISIBLE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_ID], View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_MORE_ID],
                        View.GONE);
            } else {
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_ID],
                        View.VISIBLE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_MORE_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_MORE_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_ID], View.GONE);
            }
        } else {
            if (DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION.equals(
                    accountInfo.getIntent().getAction())) {
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_MORE_ID],
                        View.VISIBLE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_MORE_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_ID],
                        View.GONE);
            } else {
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_ID],
                        View.VISIBLE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.NORMAL_DIVIDER_MORE_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_ID],
                        View.GONE);
                mBigRemoteViews.setViewVisibility(resourceId[IdIndex.HIGHTLIGHT_DIVIDER_MORE_ID],
                        View.GONE);
            }
        }

        if (accountInfo.getIntent() != null) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, mRequestCode ++,
                    accountInfo.getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
            mBigRemoteViews.setOnClickPendingIntent(resourceId[IdIndex.CONTAINER_ID], pendingIntent);
        }
    }

    public static class AccountInfo implements Parcelable {
        private int mIconId;
        private Bitmap mIcon;
        private String mLabel;
        private String mNumber;
        private Intent mIntent;
        private boolean mIsActive;
        private boolean mIsSimAccount;

        /**
         * Constructor
         * @param icon The icon of the account.
         * @param label The label of the account.
         * @param number The number of the account.
         * @param intent The behavior of the account.
         */
        public AccountInfo(Bitmap icon, String label, String number, Intent intent) {
            this(0, icon, label, number, intent, false, true);

        }

        /**
         * Constructor
         * @param iconId The icon id of the account.
         * @param label The label of the account.
         * @param number The number of the account.
         * @param intent The behavior of the account.
         */
        public AccountInfo(int iconId, String label, String number, Intent intent) {
            this(iconId, null, label, number, intent, false, true);
        }

        /**
         * Constructor
         * @param iconId The icon id of the account.
         * @param icon The icon of the account.
         * @param label The label of the account.
         * @param number The number of the account.
         * @param intent The behavior of the account.
         * @param isActive Whether the account is active or not.
         */
        public AccountInfo(int iconId, Bitmap icon, String label, String number, Intent intent,
                boolean isActive) {
            this(iconId, icon, label, number, intent, isActive, true);
        }

        /**
         * Constructor
         * @param iconId The icon id of the account.
         * @param icon The icon of the account.
         * @param label The label of the account.
         * @param number The number of the account.
         * @param intent The behavior of the account.
         * @param isActive Whether the account is active or not.
         * @param isSimAccount Whether the account is sim account or not
         */
        public AccountInfo(int iconId, Bitmap icon, String label, String number, Intent intent,
                boolean isActive, boolean isSimAccount) {
            mIconId = iconId;
            mIcon = icon;
            mLabel = label;
            mNumber = number;
            mIntent = intent;
            mIsActive = isActive;
            mIsSimAccount = isSimAccount;
        }

        /**
         *
         * @return Get the Icon id of the account.
         */
        public int getIconId() {
            if (mIconId != 0) {
                return mIconId;
            }
            return 0;
        }

        /**
         *
         * @return Get the Icon of the account.
         */
        public Bitmap getIcon() {
            if (mIcon != null) {
                return mIcon;
            }
            return null;
        }

        /**
         *
         * @return Get the label of the account.
         */
        public String getLabel() {
            return mLabel;
        }

        /**
         *
         * @return Get the number of the account.
         */
        public String getNumber() {
            return mNumber;
        }

        /**
         *
         * @return Set the intent of the account.
         */
        public void setIntent(Intent intent) {
            mIntent = intent;
        }

        /**
         *
         * @return Get the intent of the account.
         */
        public Intent getIntent() {
            return mIntent;
        }

        /**
         *
         * @return Get the active status of the account.
         */
        public boolean isActive() {
            return mIsActive;
        }

        /**
         *
         * @param active Set the active status of the account.
         */
        public void setActiveStatus(boolean active) {
            mIsActive = active;
        }

        /**
         *
         * @return Return true if the account is SIM account.
         */
        public boolean isSimAccount() {
            return mIsSimAccount;
        }

        /**
         *
         * @param isSimAccount Set whether the account is SIM account .
         */
        public void setSimAccount(boolean isSimAccount) {
            mIsSimAccount = isSimAccount;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mIconId);
            dest.writeParcelable(mIcon, 0);
            dest.writeString(mLabel);
            dest.writeString(mNumber);
            dest.writeParcelable(mIntent, 0);
            dest.writeInt(mIsActive ? 1 : 0);
            dest.writeInt(mIsSimAccount ? 1 : 0);
        }

        public AccountInfo(Parcel in) {
            final ClassLoader loader = getClass().getClassLoader();
            mIconId = in.readInt();
            mIcon = in.readParcelable(loader);
            mLabel = in.readString();
            mNumber = in.readString();
            mIntent = in.readParcelable(loader);
            mIsActive = in.readInt() == 1;
            mIsSimAccount = in.readInt() == 1;
        }

        public static final Parcelable.Creator<AccountInfo> CREATOR =
                new Parcelable.Creator<AccountInfo>() {
            @Override
            public AccountInfo createFromParcel(Parcel in) {
                return new AccountInfo(in);
            }

            @Override
            public AccountInfo[] newArray(int size) {
                return new AccountInfo[size];
            }
        };
    }
}
