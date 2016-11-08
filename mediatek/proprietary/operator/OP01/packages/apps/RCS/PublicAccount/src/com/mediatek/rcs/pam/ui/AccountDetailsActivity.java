package com.mediatek.rcs.pam.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.IPAServiceCallback;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.ui.accounthistory.AccountHistoryActivity;
import com.mediatek.rcs.pam.ui.conversation.PaComposeActivity;

import java.io.File;

/**
 * Activity to display public account detail information.
 *
 * @author mtk81226
 */
public class AccountDetailsActivity extends LoadingMaskActivity {

    private static final String TAG = Constants.TAG_PREFIX
            + "AccountDetailsActivity";

    public static final String ACTION = "com.mediatek.pam.AccountDetailsActivity";

    public static final String KEY_UUID = "com.medaitek.pam.AccountDetailsActivity.KEY_UUID";
    public static final String KEY_ID = "com.medaitek.pam.AccountDetailsActivity.KEY_ID";

    public static final int GET_DETAILS_QUERY_TOKEN = 0;
    // public static final int CLEAR_MESSAGE_HISTORY_QUERY_TOKEN = 1;
    public static final int UPDATE_LOGO_INFO_TOKEN = 2;
    public static final int GET_DETAILS_QUERY_AND_CHECK_STATUS_TOKEN = 3;

    private boolean mIsQuerying = false;

    private boolean mDestroyed = false;

    public static enum Mode {
        NOT_SUBSCRIBED, SUBSCRIBED,
    }

    // Views
    private ImageView mLogoView;
    private TextView mAccountNameView;
    private TextView mUuidView;
    private TextView mDescriptionView;
    private Switch mNotificationSwitch;
    private Button mEnterChatButton;
    private Button mViewHistoryButton;
    private Button mSubscribeButton;
    private Button mUnsubscribeButton;
    private Button mClearHistoryButton;
    private TextView mTelephoneView;
    private TextView mEmailView;
    private TextView mAddressView;
    private TextView mFieldView;
    private TextView mTelephoneTitleView;
    private TextView mEmailTitleView;
    private TextView mAddressTitleView;
    private TextView mFieldTitleView;
    private ImageView mTelephoneLogoView;
    private ImageView mEmailLogoView;
    private ImageView mAddressLogoView;
    private ImageView mFieldLogoView;
    private View mSeparator1;
    private View mSeparator2;
    private View mSeparator3;
    private View mSeparator4;
    private View mSeparator5;

    private PAService mService;
    private IPAServiceCallback mServiceCallback;
    private long mToken = Constants.INVALID;
    private AsyncQueryHandler mProviderActionHandler;
    private Mode mMode = Mode.NOT_SUBSCRIBED;
    // private long mRequestId = -1;
    private PublicAccount mAccount = new PublicAccount();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSeparator1 = findViewById(R.id.separator1);
        mSeparator2 = findViewById(R.id.separator2);
        mSeparator3 = findViewById(R.id.separator3);
        mSeparator4 = findViewById(R.id.separator4);
        mSeparator5 = findViewById(R.id.separator5);

        mLogoView = (ImageView) findViewById(R.id.logo);
        mAccountNameView = (TextView) findViewById(R.id.account_name);
        mUuidView = (TextView) findViewById(R.id.uuid);
        mDescriptionView = (TextView) findViewById(R.id.description);
        mNotificationSwitch = (Switch) findViewById(R.id.notification_switch);
        mNotificationSwitch.setTag(false);
        mNotificationSwitch
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (!checkRegistrationStatusAndPrompt()) {
                            return;
                        }
                        if (!((Boolean) mNotificationSwitch.getTag())) {
                            Log.d(TAG, "Notification Change by User: "
                                    + isChecked);
                            switchToLoadingView();
                            mAccount.acceptStatus = isChecked ? Constants.ACCEPT_STATUS_YES
                                    : Constants.ACCEPT_STATUS_NO;
                            mService.setAcceptStatus(mToken, mAccount.uuid,
                                    mAccount.acceptStatus);
                        } else {
                            Log.d(TAG, "Notification Change by Program: "
                                    + isChecked);
                            mNotificationSwitch.setTag(false);
                        }
                    }
                });
        mEnterChatButton = (Button) findViewById(R.id.enter_chat_button);
        mEnterChatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Launch Chat with UUID: " + mAccount.uuid);
                Intent intent = new Intent();
                intent.setClass(AccountDetailsActivity.this,
                        PaComposeActivity.class);
                intent.putExtra(PaComposeActivity.ACCOUNT_ID, mAccount.id);
                intent.putExtra(PaComposeActivity.UUID, mAccount.uuid); // uuid
                intent.putExtra(PaComposeActivity.NAME, mAccount.name); // account
                                                                        // name
                if (!TextUtils.isEmpty(mAccount.logoPath)) {
                    intent.putExtra(PaComposeActivity.IMAGE_PATH,
                            mAccount.logoPath); // profile
                                                // image
                                                // path
                }
                startActivity(intent);
            }
        });
        mViewHistoryButton = (Button) findViewById(R.id.view_history_button);
        mViewHistoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(AccountDetailsActivity.this,
                        AccountHistoryActivity.class);
                intent.putExtra(AccountHistoryActivity.KEY_ACCOUNT_UUID,
                        mAccount.uuid);
                intent.putExtra(AccountHistoryActivity.KEY_ACCOUNT_TITLE,
                        mAccount.name);
                startActivity(intent);
            }
        });
        mSubscribeButton = (Button) findViewById(R.id.subscribe_button);
        mSubscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMode != Mode.NOT_SUBSCRIBED) {
                    throw new Error("Account is already subscribed.");
                }
                if (!checkRegistrationStatusAndPrompt()) {
                    return;
                }
                switchToLoadingView();
                /* mRequestId = */mService.subscribe(mToken, mAccount.uuid);
            }
        });
        mUnsubscribeButton = (Button) findViewById(R.id.unsubscribe_button);
        mUnsubscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if (mRequestId != -1) {
                // throw new Error("Request exits");
                // }
                if (mMode != Mode.SUBSCRIBED) {
                    throw new Error("Account is not subscribed.");
                }
                if (!checkRegistrationStatusAndPrompt()) {
                    return;
                }
                switchToLoadingView();
                /* mRequestId = */mService.unsubscribe(mToken, mAccount.uuid);
            }
        });
        mClearHistoryButton = (Button) findViewById(R.id.clear_history_button);
        mClearHistoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDelete();
            }
        });

        mTelephoneView = (TextView) findViewById(R.id.telephone_text);
        mEmailView = (TextView) findViewById(R.id.email_text);
        mAddressView = (TextView) findViewById(R.id.address_text);
        mFieldView = (TextView) findViewById(R.id.field_text);
        mTelephoneTitleView = (TextView) findViewById(R.id.telephone_title);
        mEmailTitleView = (TextView) findViewById(R.id.email_title);
        mAddressTitleView = (TextView) findViewById(R.id.address_title);
        mFieldTitleView = (TextView) findViewById(R.id.field_title);
        mTelephoneLogoView = (ImageView) findViewById(R.id.telephone);
        mEmailLogoView = (ImageView) findViewById(R.id.email);
        mAddressLogoView = (ImageView) findViewById(R.id.address);
        mFieldLogoView = (ImageView) findViewById(R.id.field);

        bindData(null);

        mProviderActionHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {
                mIsQuerying = false;
                if (mDestroyed || mService == null
                        || !mService.isServiceConnected(mToken)) {
                    Log.w(TAG, "onQueryComplete " + token
                            + " invoked when no service connection. Do nothing");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                Log.d(TAG, "onQueryComplete: " + token);
                if (token == GET_DETAILS_QUERY_TOKEN) {
                    bindData(cursor);
                    switchToNormalView();
                } else if (token == GET_DETAILS_QUERY_AND_CHECK_STATUS_TOKEN) {
                    bindData(cursor);
                    if (mAccount.activeStatus == Constants.ACTIVE_STATUS_NORMAL) {
                        switchToNormalView();
                    } else if (mAccount.subscribeStatus == Constants.SUBSCRIPTION_STATUS_YES) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                AccountDetailsActivity.this);
                        builder.setMessage(R.string.unsubscribe_alert_message);
                        builder.setTitle(R.string.unsubscribe_alert_title);
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        switchToLoadingView();
                                        mService.unsubscribe(mToken,
                                                mAccount.uuid);
                                    }
                                });
                        builder.setNegativeButton(android.R.string.cancel, null);

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    throw new Error("Invalid query token " + token);
                }
            }

            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                if (mDestroyed || mService == null
                        || !mService.isServiceConnected(mToken)) {
                    Log.w(TAG, "onDeleteComplete " + token
                            + " invoked when no service connection. Do nothing");
                    return;
                }
                Log.d(TAG, "onDeleteComplete: " + token);
                switchToNormalView();
                throw new Error("Invalid delete token " + token);
            }

            @Override
            protected void onUpdateComplete(int token, Object cookie, int result) {
                if (mDestroyed || mService == null
                        || !mService.isServiceConnected(mToken)) {
                    Log.w(TAG, "onUpdateComplete " + token
                            + " invoked when no service connection. Do nothing");
                    return;
                }
                if (token != UPDATE_LOGO_INFO_TOKEN) {
                    throw new Error("Invalid update token " + token);
                }
                Log.d(TAG, "onUpdateComplete: " + token);
                // The status in the switch widget is correct, no need to update
                // from provider.
            }
        };

        Bundle arguments = getIntent().getExtras();
        mAccount.id = arguments.getLong(KEY_ID, Constants.INVALID);
        mAccount.uuid = arguments.getString(KEY_UUID, null);
        Log.d(TAG, "Created with KEY_ID: " + mAccount.id);
        Log.d(TAG, "Created with KEY_UUID: " + mAccount.uuid);

        mServiceCallback = new SimpleServiceCallback() {

            @Override
            public void reportSubscribeResult(long requestId, int resultCode)
                    throws RemoteException {
                if (resultCode != ResultCode.SUCCESS) {
                    Toast.makeText(AccountDetailsActivity.this,
                            getString(R.string.subscribe_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    startQueryForAccountDetails(false);
                    Log.d(TAG, "Launch Chat with UUID: " + mAccount.uuid);
                    Intent intent = new Intent();
                    intent.setClass(AccountDetailsActivity.this,
                            PaComposeActivity.class);
                    intent.putExtra(PaComposeActivity.ACCOUNT_ID, mAccount.id);
                    intent.putExtra(PaComposeActivity.UUID, mAccount.uuid); // uuid
                    intent.putExtra(PaComposeActivity.NAME, mAccount.name); // account
                                                                            // name
                    if (!TextUtils.isEmpty(mAccount.logoPath)) {
                        intent.putExtra(PaComposeActivity.IMAGE_PATH,
                                mAccount.logoPath); // profile
                                                    // image
                                                    // path
                    }
                    startActivity(intent);
                }
            }

            @Override
            public void reportUnsubscribeResult(long requestId, int resultCode)
                    throws RemoteException {
                if (resultCode != ResultCode.SUCCESS) {
                    Toast.makeText(AccountDetailsActivity.this,
                            getString(R.string.unsubscribe_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    startQueryForAccountDetails(false);
                }
            }

            @Override
            public void reportGetDetailsResult(long requestId, int resultCode,
                    long accountId) throws RemoteException {
                if (resultCode != ResultCode.SUCCESS) {
                    Toast.makeText(AccountDetailsActivity.this,
                            getString(R.string.get_details_failed),
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    startQueryForAccountDetails(true);
                }
            }

            @Override
            public void reportSetAcceptStatusResult(long requestId,
                    int resultCode) throws RemoteException {
                if (resultCode != ResultCode.SUCCESS) {
                    Toast.makeText(AccountDetailsActivity.this,
                            getString(R.string.get_details_failed),
                            Toast.LENGTH_SHORT).show();
                }
                startQueryForAccountDetails(false);
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                startQueryForAccountDetails(false);
                updateDetailsFromServer();
            }

            @Override
            public void onServiceDisconnected(int reason)
                    throws RemoteException {
                Log.d(TAG, "onServiceDisconnected: reason = " + reason);
                if (reason == PAService.INTERNAL_ERROR) {
                    finish();
                } else {
                    Toast.makeText(AccountDetailsActivity.this,
                            R.string.text_service_not_connect,
                            Toast.LENGTH_SHORT).show();
                    switchToLoadingView();
                }
            }

            @Override
            public void reportDeleteMessageResult(long requestId, int resultCode)
                    throws RemoteException {
                Log.d(TAG, "reportDeleteMessageResult: " + resultCode);
                switchToNormalView();
            }
        };

        ActionBar actionBar = getActionBar();
        actionBar.setLogo(R.drawable.ic_account_detail);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.account_details_actionbar_selector));

        mService = PAService.getInstance();
        if (mService != null) {
            mToken = mService.registerCallback(mServiceCallback, false);
            mService.registerAck(mToken);
        } else {
            switchToLoadingView();
            PAService.init(AccountDetailsActivity.this,
                    new PAService.ServiceConnectNotify() {

                        @Override
                        public void onServiceConnected() {
                            Log.i(TAG, "onServiceConnectedsss");
                            mService = PAService.getInstance();
                            mToken = mService.registerCallback(
                                    mServiceCallback, false);
                            mService.registerAck(mToken);
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAccount.id != Constants.INVALID
                || !TextUtils.isEmpty(mAccount.uuid)) {
            startQueryForAccountDetails(false);
        }
    }

    private synchronized void updateDetailsFromServer() {
        if (TextUtils.isEmpty(mAccount.uuid)) {
            if (mAccount.id != Constants.INVALID) {
                mAccount.uuid = PublicAccount.queryAccountUuid(this,
                        mAccount.id);
            } else {
                Log.e(TAG, "No account id or uuid specified.");
                // throw new Error("No account id or uuid specified.");
                finish();
                return;
            }
        }
        // always get the latest info
        mService.getDetails(mToken, mAccount.uuid, null);
    }

    private void complainPublicAccount() {
        if (TextUtils.isEmpty(mAccount.uuid)) {
            if (mAccount.id != Constants.INVALID) {
                mAccount.uuid = PublicAccount.queryAccountUuid(this,
                        mAccount.id);
            } else {
                Log.e(TAG, "No account id or uuid specified.");
                Toast.makeText(this, R.string.account_details_no_account,
                        Toast.LENGTH_SHORT).show();
            }
        }
        Intent intent = new Intent();
        intent.setAction(ComplainAccountActivity.ACTION);
        intent.setClass(this, ComplainAccountActivity.class);
        intent.putExtra(ComplainAccountActivity.KEY_UUID, mAccount.uuid);
        intent.putExtra(ComplainAccountActivity.KEY_NAME, mAccount.name);
        startActivity(intent);
    }

    private void startQueryForAccountDetails(boolean checkStatus) {
        if (mIsQuerying) {
            return;
        }
        mIsQuerying = true;
        final int token = checkStatus ? GET_DETAILS_QUERY_AND_CHECK_STATUS_TOKEN
                : GET_DETAILS_QUERY_TOKEN;
        Log.d(TAG, "startQueryForAccountDetails");
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                switchToLoadingView();

            }
        });
        if (mAccount.id != Constants.INVALID) {
            mProviderActionHandler.startQuery(token, null,
                    AccountColumns.CONTENT_URI,
                    PublicAccount.sDetailProjection, AccountColumns.TABLE + "."
                            + AccountColumns.ID + "=?",
                    new String[] { Long.toString(mAccount.id) }, null);
        } else if (!TextUtils.isEmpty(mAccount.uuid)) {
            mProviderActionHandler.startQuery(token, null,
                    AccountColumns.CONTENT_URI,
                    PublicAccount.sDetailProjection, AccountColumns.TABLE + "."
                            + AccountColumns.UUID + "=?",
                    new String[] { mAccount.uuid }, null);
        } else {
            Log.e(TAG, "No account id or uuid specified.");
            Toast.makeText(this, "No account id or uuid specified.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startDelete() {
        Log.d(TAG, "startDelete");
        switchToLoadingView();
        mService.deleteMessageByAccount(mToken, mAccount.id);
    }

    private void switchToMode(Mode mode) {
        Mode lastMode = mMode;
        mMode = mode;
        if (mode == Mode.NOT_SUBSCRIBED) {
            mNotificationSwitch.setVisibility(View.INVISIBLE);
            mSubscribeButton.setVisibility(View.VISIBLE);
            mEnterChatButton.setVisibility(View.GONE);
            mUnsubscribeButton.setVisibility(View.GONE);
            mClearHistoryButton.setVisibility(View.GONE);
            mSeparator1.setVisibility(View.GONE);
            mSeparator2.setVisibility(View.GONE);
            mSeparator3.setVisibility(View.GONE);
            mSeparator4.setVisibility(View.GONE);
            mSeparator5.setVisibility(View.GONE);
            mTelephoneLogoView.setVisibility(View.GONE);
            mTelephoneView.setVisibility(View.GONE);
            mTelephoneTitleView.setVisibility(View.GONE);
            mEmailLogoView.setVisibility(View.GONE);
            mEmailView.setVisibility(View.GONE);
            mEmailTitleView.setVisibility(View.GONE);
            mAddressLogoView.setVisibility(View.GONE);
            mAddressView.setVisibility(View.GONE);
            mAddressTitleView.setVisibility(View.GONE);
            mFieldLogoView.setVisibility(View.GONE);
            mFieldView.setVisibility(View.GONE);
            mFieldTitleView.setVisibility(View.GONE);
        } else if (mode == Mode.SUBSCRIBED) {
            mNotificationSwitch.setVisibility(View.VISIBLE);
            mEnterChatButton.setVisibility(View.VISIBLE);
            mSubscribeButton.setVisibility(View.GONE);
            mUnsubscribeButton.setVisibility(View.VISIBLE);
            mClearHistoryButton.setVisibility(View.VISIBLE);
            mSeparator1.setVisibility(View.VISIBLE);
            mSeparator2.setVisibility(View.VISIBLE);
            mSeparator3.setVisibility(View.VISIBLE);
            mSeparator4.setVisibility(View.VISIBLE);
            mSeparator5.setVisibility(View.VISIBLE);
            mTelephoneLogoView.setVisibility(View.VISIBLE);
            mTelephoneView.setVisibility(View.VISIBLE);
            mTelephoneTitleView.setVisibility(View.VISIBLE);
            mEmailLogoView.setVisibility(View.VISIBLE);
            mEmailView.setVisibility(View.VISIBLE);
            mEmailTitleView.setVisibility(View.VISIBLE);
            mAddressLogoView.setVisibility(View.VISIBLE);
            mAddressView.setVisibility(View.VISIBLE);
            mAddressTitleView.setVisibility(View.VISIBLE);
            mFieldLogoView.setVisibility(View.VISIBLE);
            mFieldView.setVisibility(View.VISIBLE);
            mFieldTitleView.setVisibility(View.VISIBLE);
        } else {
            throw new Error("Invalid mode switching: " + lastMode + " to "
                    + mode);
        }
    }

    private Bitmap loadLogoImage() {
        if (mAccount.logoImage != null) {
            return mAccount.logoImage;
        }

        if (mAccount.logoPath != null) {
            File file = new File(mAccount.logoPath);
            if (file.exists()) {
                return (new BitmapDrawable(getResources(), mAccount.logoPath))
                        .getBitmap();
            } else {
                downloadLogo();
                return null;
            }
        } else {
            downloadLogo();
            return null;
        }
    }

    private void downloadLogo() {
        FileDownloader.getInstance().sendDownloadRequest(mAccount.logoUrl,
                Constants.MEDIA_TYPE_PICTURE, -1, 0,
                new FileDownloader.DownloadListener() {
                    @Override
                    public void reportDownloadResult(int resultCode,
                            final String path, long mediaId, long msgId,
                            int index) {
                        if (resultCode == ResultCode.SUCCESS) {
                            mAccount.logoPath = path;
                            mAccount.logoId = mediaId;
                            startUpdateLogoInfo();
                            mAccount.logoImage = BitmapFactory
                                    .decodeFile(mAccount.logoPath);
                            if (mLogoView != null) {
                                mLogoView.setImageBitmap(mAccount.logoImage);
                            }
                        }
                    }

                    @Override
                    public void reportDownloadProgress(long msgId, int index,
                            int percentage) {

                    }
                });
    }

    private void startUpdateLogoInfo() {
        Log.d(TAG, "startUpdateLogoInfo: " + mAccount + ", "
                + mAccount.logoPath);
        // switchToLoadingView();
        ContentValues cv = new ContentValues();
        cv.put(AccountColumns.LOGO_ID, mAccount.logoId);
        mProviderActionHandler.startUpdate(UPDATE_LOGO_INFO_TOKEN, null,
                AccountColumns.CONTENT_URI, cv, AccountColumns.ID + "=?",
                new String[] { Long.toString(mAccount.id) });
    }

    private void bindData(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            mAccount.loadFullInfoFromCursor(cursor);
            mLogoView.setImageBitmap(loadLogoImage());
            mAccountNameView.setText(mAccount.name);
            mUuidView.setText(mAccount.uuid);
            mDescriptionView.setText(mAccount.introduction);
            if (mAccount.subscribeStatus == Constants.SUBSCRIPTION_STATUS_YES) {
                mTelephoneView.setText(mAccount.telephone);
                mEmailView.setText(mAccount.email);
                mAddressView.setText(mAccount.address);
                mFieldView.setText(mAccount.field);
                final boolean newChecked = mAccount.acceptStatus == Constants.ACCEPT_STATUS_YES;
                if (newChecked != mNotificationSwitch.isChecked()) {
                    mNotificationSwitch.setTag(true);
                    mNotificationSwitch.setChecked(newChecked);
                }
                switchToMode(Mode.SUBSCRIBED);
            } else {
                switchToMode(Mode.NOT_SUBSCRIBED);
            }
            switchToNormalView();
        } else {
            // bind placeholder data
            mLogoView.setImageResource(R.drawable.ic_account_detail_black);
            mAccountNameView.setText("<name>");
            mUuidView.setText("<uuid>");
            mDescriptionView.setText("<introduction>");
            mTelephoneView.setText("<telephone>");
            mEmailView.setText("<email>");
            mAddressView.setText("<address>");
            mFieldView.setText("<field>");
            switchToMode(Mode.NOT_SUBSCRIBED);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private boolean checkRegistrationStatusAndPrompt() {
        if (mService != null && mService.isServiceConnected(mToken)
                && mService.isServiceRegistered(mToken)) {
            return true;
        } else {
            Toast.makeText(this, R.string.not_registered_promt,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.account_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.account_details_refresh) {
            Log.d(TAG, "Refresh details manually.");
            updateDetailsFromServer();
            return true;
        } else if (id == R.id.account_details_complain) {
            Log.d(TAG, "Complain public account.");
            complainPublicAccount();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_account_details;
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mService != null) {
            mService.unregisterCallback(mToken);
        }
        super.onDestroy();
    }
}
