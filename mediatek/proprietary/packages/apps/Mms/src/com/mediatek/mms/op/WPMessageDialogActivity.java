/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mms.op;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.Telephony.WapPush;
import android.view.Window;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.MmsLog;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.wappush.WapPushMessagingNotification;




/** M:
 * Display a si WPMessage to the user. Wait for the user to dismiss
 */
public class WPMessageDialogActivity extends Activity {
    private static final String TAG = "WPMessageDialogActivity";
    public static final long THREAD_ALL = -1;
    public static final long THREAD_NONE = -2;
    public static final int SI_ACTION_NONE = 0;

    public static final int SI_ACTION_LOW = 1;

    public static final int SI_ACTION_MEDIUM = 2;

    public static final int SI_ACTION_HIGH = 3;

    public static final int SI_ACTION_DELETE = 4;

    public static final int SL_ACTION_LOW = 1;

    public static final int SL_ACTION_HIGH = 2;

    public static final int SL_ACTION_CACHE = 3;
    private WPMessageDialog mDialog = null;
    private String mUrl;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (PermissionCheckUtil.requestRequiredPermissions(this)) {
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(
                R.drawable.class_zero_background);
        initDialog();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (PermissionCheckUtil.requestRequiredPermissions(this)) {
            return;
        }

        initDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss(0);
            mDialog = null;
        }
    }

    private void initDialog() {
        Intent intent = getIntent();
        String messageDetails = getWPMessageDetails(this, intent);
        /// M: close the old one if a new one come, keep old unread.
        if (mDialog != null) {
            mDialog.dismiss(0);
            mDialog = null;
        }

//        AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        WPMessageDialog b = new WPMessageDialog(this, AlertDialog.THEME_HOLO_LIGHT);
        b.setTitle(R.string.menu_wappush);
        b.setCancelable(true);
        b.setMessage(messageDetails);

        b.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
        new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        b.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.visit_website),
        new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialog, int which) {
                if (mUrl != null) {
                    Uri uri = Uri.parse(MessageUtils.checkAndModifyUrl(mUrl));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                            WPMessageDialogActivity.this.getPackageName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(WPMessageDialogActivity.this,
                                R.string.error_unsupported_scheme, Toast.LENGTH_LONG)
                                .show();
                        MmsLog.e(TAG, "Scheme " + uri.getScheme() + "is not supported!");
                    }
                }
                dialog.dismiss();
            }
        });
        mDialog = b;
        mDialog.show();
    }

    private String getWPMessageDetails(Context context, Intent intent) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        String senderAddress = intent.getStringExtra(WapPush.ADDR);
        String centerAddress = intent.getStringExtra(WapPush.SERVICE_ADDR);
        int subId = intent.getIntExtra(WapPush.SUBSCRIPTION_ID, -1);
        mUrl = intent.getStringExtra(WapPush.URL);
        String siid = intent.getStringExtra(WapPush.SIID);
        int action = intent.getIntExtra(WapPush.ACTION, -1);
        int create = intent.getIntExtra(WapPush.CREATE, 0);
        long expiration = intent.getLongExtra(WapPush.EXPIRATION, 0);
        String text = intent.getStringExtra(WapPush.TEXT);
        long date = intent.getLongExtra(WapPush.DATE, 0);
        mUri = Uri.parse(intent.getStringExtra("uri"));

        //Priority: Low, Medium, High
        //SI: None, Low, Medium, High, Delete
        //Priority: SL: High, Low, Cache
        details.append(res.getString(R.string.wp_msg_priority_label));
        int priority = action;
        int type = WapPush.TYPE_SI; /// M: support si only.
        if (WapPush.TYPE_SI == type) {
            switch (priority) {
            case SI_ACTION_NONE:
                MmsLog.i(TAG, "action error, none");
                break;
            case SI_ACTION_LOW:
                details.append(res.getString(R.string.wp_msg_priority_low));
                break;
            case SI_ACTION_MEDIUM:
                details.append(res.getString(R.string.wp_msg_priority_medium));
                break;
            case SI_ACTION_HIGH:
                details.append(res.getString(R.string.wp_msg_priority_high));
                break;
            case SI_ACTION_DELETE:
                MmsLog.i(TAG, "action error, delete");
                break;
            default:
                MmsLog.i(TAG, "getWPMessageDetails si priority error.");
            }
        } else if (WapPush.TYPE_SL == type) {
            switch (priority) {
            case SL_ACTION_LOW:
                details.append(res.getString(R.string.wp_msg_priority_low));
                break;
            case SL_ACTION_HIGH:
                details.append(res.getString(R.string.wp_msg_priority_high));
                break;
            case SL_ACTION_CACHE:
                details.append(res.getString(R.string.wp_msg_priority_low));
                break;
            default:
                MmsLog.i(TAG, "getWPMessageDetails sl priority error.");
            }
        } else {
            MmsLog.i(TAG, "getWPMessageDetails type error.");
        }

        // Address: ***
        details.append('\n');
        details.append(res.getString(R.string.from_label));
        details.append(senderAddress);

        // Date: ***
        details.append('\n');
        details.append(res.getString(R.string.received_label));
        details.append(MessageUtils.formatTimeStampString(context, date, true));

        //Expired time
        long expiredDate = expiration * 1000;
        if (expiredDate != 0) {
            details.append('\n');
            details.append(String.format(context.getString(R.string.wp_msg_expiration_label),
MessageUtils.formatTimeStampString(context, expiredDate, true)));
        }

        // WebSite: ***
        if ((mUrl != null) && (!mUrl.equals(""))) {
            details.append("\n\n");
            details.append(res.getString(R.string.website));
            details.append(mUrl);
        }

        // Message Content
        if ((text != null) && (!text.equals(""))) {
            details.append("\n\n");
            details.append(text);
        }

        return details.toString();
    }

    private void markAsRead() {
        new Thread(new Runnable() {
            public void run() {
                if (mUri != null) {
                    ContentValues values = new ContentValues(2);
                    values.put("read", 1);
                    values.put("seen", 1);
                    MmsLog.d(TAG, "markAsRead uri:" + mUri.toString());
                    WPMessageDialogActivity.this.getContentResolver().
                                  update(mUri, values, null, null);
                    WapPushMessagingNotification.blockingUpdateNewMessageIndicator
(WPMessageDialogActivity.this,
                            WapPushMessagingNotification.THREAD_NONE);
                }
            }
        }).start();
    }

    private class WPMessageDialog extends AlertDialog {
        public WPMessageDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        public void dismiss() {
            super.dismiss();
            mDialog = null;
            markAsRead();
            finish();
        }

        public void dismiss(int dummy) {
            super.dismiss();
        }
    }
}
