/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SubscriptionInfo;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.mms.pdu.PduHeaders;

import com.mediatek.mms.callback.IFileAttachmentModelCallback;
import com.mediatek.mms.callback.IMessageListItemCallback;
import com.mediatek.mms.callback.IMessageUtilsCallback;
import com.mediatek.mms.callback.ISlideshowModelCallback;
import com.mediatek.mms.ext.DefaultOpMessageListItemExt;
import com.mediatek.mms.ext.IOpFileAttachmentModelExt;
import com.mediatek.mms.ext.IOpMessageItemExt;
import com.mediatek.mms.ext.IOpMessagePluginExt;
import com.mediatek.op09.plugin.R;

public class Op09MessageListItemExt extends DefaultOpMessageListItemExt {

    public Op09MessageListItemExt(Context context) {
        super(context);
        mResourceContext = context;
    }

    private static final String TAG = "Op09MessageListItemExt";

    public static final int STATE_UNKNOWN = 0x00;
    public static final int STATE_UNSTARTED = 0x80;
    public static final int STATE_DOWNLOADING = 0x81;
    public static final int STATE_TRANSIENT_FAILURE = 0x82;
    public static final int STATE_PERMANENT_FAILURE = 0x87;

    private Op09MessageItemExt mMessageItemExt;
    private IMessageListItemCallback mMessageListItemCallback;
    Op09FileAttachmentModelExt mFileAttachmentModel;

    private Context mUIContext;
    private Context mResourceContext;

    @Override
    public void init(IMessageListItemCallback messageListItem, Context context) {
        mUIContext = context;
        mMessageListItemCallback = messageListItem;
    }

    @Override
    public void bind(IOpMessageItemExt messageItemExt) {
        mMessageItemExt = (Op09MessageItemExt) messageItemExt;
    }

    @Override
    public void bindNotifInd(Context context, String msgSizeText, TextView expireText,
            TextView subStatus, CheckBox selectedBox, boolean isSmsEnabled, TextView sendTime,
            LinearLayout doubleTime) {
        Log.d(TAG, "op09 bindNotifInd");
        setNotifyContent(Op09MessagePluginExt.sCallback.getString(IMessageUtilsCallback.from_label)
                + mMessageItemExt.mAddress,
                Op09MessagePluginExt.sCallback.getString(IMessageUtilsCallback.subject_label),
                msgSizeText, mMessageItemExt.mTimestamp, expireText);
        showSimType(context, mMessageItemExt.mSubId, subStatus);
        if (sendTime != null) {
            sendTime.setVisibility(View.GONE);
        }
        if (doubleTime != null) {
            doubleTime.setVisibility(View.GONE);
        }

        switch (mMessageItemExt.getMmsDownloadStatus()) {
            case STATE_DOWNLOADING:
                break;
            case STATE_UNKNOWN:
            case STATE_UNSTARTED:
            case STATE_TRANSIENT_FAILURE:
            case STATE_PERMANENT_FAILURE:
            default:
                Log.d(TAG, "op09 bindNotifInd isSmsEnabled: "+isSmsEnabled);
                if (isSmsEnabled) {
                    showDownloadButton(mMessageItemExt.mMessageUri,
                            selectedBox, mMessageItemExt.mMsgId,
                            Op09MessagePluginExt.sCallback.getDeviceStorageFullStatus(),
                            mDownloadBtnListener,
                            mCancelDownloadListener);
                }
        }
    }

    @Override
    public boolean onDownloadButtonClick(Activity activity, boolean storageFull) {
        if (MessageUtils.allowSafeDraft(activity, mResourceContext,storageFull, true,
                MessageUtils.TOAST_TYPE_FOR_DOWNLOAD_MMS)) {
            return true;
        }
        return false;
    }

    @Override
    public void showDownloadingAttachment(CheckBox selectedBox) {
        hideDownloadButton(mMessageItemExt.mMessageUri,
                mCancelDownloadListener, selectedBox, mMessageItemExt.mMsgId);
    }

    @Override
    public void bindCommonMessage(Button downloadButton, TextView dateView) {
        if (downloadButton != null) {
            hideAllButton();
        }
        if (mMessageItemExt.isReceivedMessage()
                 && !TextUtils.isEmpty(mMessageItemExt.mTimestamp)) {
            mMessageListItemCallback.buildTimestampLineCallback(mMessageItemExt.mTimestamp);
            if (MessageUtils.isShowDateManagementEnable()) {
                String dateStr = null;
                if (mMessageItemExt.isSms()) {
                    dateStr = MessageUtils.getShortTimeString(mUIContext,
                                                mMessageItemExt.mSmsSentDate);
                } else if (mMessageItemExt.isMms()
                    && mMessageItemExt.mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                    dateStr = MessageUtils.getShortTimeString(mUIContext,
                                                    mMessageItemExt.mMmsSentDate * 1000L);
                }
                if (!TextUtils.isEmpty(dateStr)) {
                    dateView.setText(dateStr);
                }
            }
        }
    }

    @Override
    public SpannableStringBuilder formatMessage(IOpMessageItemExt msgItemExt,
            SpannableStringBuilder buf, String body, boolean hasSubject) {
        Op09MessageItemExt messageItemExt = (Op09MessageItemExt)msgItemExt;
        if (messageItemExt.mMmsCc != null && MessageUtils.isSupportSendMmsWithCc()) {
            if (hasSubject || !TextUtils.isEmpty(body)) {
                buf.append("\n");
            }
            buf.append(TextUtils.replace(mResourceContext.getString(R.string.inline_cc),
                    new String[] { "%s" }, new CharSequence[] { messageItemExt.mMmsCc }));
        }
        return buf;
    }

    @Override
    public boolean onMessageListItemClick() {
        if (mMessageItemExt != null
                && ((mMessageItemExt.isOutgoingMessage() && mMessageItemExt.mIsFailedMessage)
                || mMessageItemExt.mDeliveryStatus == Op09MessageItemExt.DeliveryStatus.FAILED)) {
            if (!needEditFailedMessge(mUIContext, mMessageItemExt.mMsgId,
                                        mMessageItemExt.mIpMessageId)) {
                mMessageListItemCallback.showMessageDetail();
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void drawRightStatusIndicator(Context context, LinearLayout statusLayout,
            IOpMessageItemExt MsgItemExt, TextView subStatus, TextView sendTimeText,
            LinearLayout doubleTimeLayout, String stampLine) {
        Op09MessageItemExt msgItem = (Op09MessageItemExt)MsgItemExt;
        drawMassTextMsgStatus(context, statusLayout, msgItem.isSms(), msgItem.mIpMessageId);
        Log.d(TAG, "MessageListItem SubId:" + msgItem.mSubId);
        if (!msgItem.isSubMsg()) {
            showSimType(context, msgItem.mSubId, subStatus);
        }
        if (MessageUtils.isShowDualTimeForMsgItemEnable() && !msgItem.isSubMsg()) {
            setDualTime(mResourceContext, msgItem.isReceivedMessage(),
                    msgItem.mSubId, sendTimeText, doubleTimeLayout, stampLine);
        }
    }

    @Override
    public void bindDefault(Button downloadButton) {
        if (downloadButton != null) {
            hideAllButton();
        }
    }

    @Override
    public boolean onFileAttachmentViewClick(boolean isVcard, String src) {
        final boolean isCtFeature = MessageUtils.isSupportVCardPreview();
        if (isCtFeature && isVcard) {
            try {
                Intent intent = new Intent(mResourceContext, VCardViewerActivity.class);
                intent.setData(mFileAttachmentModel.getUri());
                intent.putExtra("file_name", src);
                //TODO sure use new task?
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("com.mediatek.action.VCardViewerActivity");
                intent.setPackage("com.mediatek.mms.plugin");
                mUIContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
            }
            return true;
        }
        return false;

    }

    @Override
    public void showFileAttachmentView(View fileAttachmentView, TextView name2, TextView name,
            ImageView thumb, TextView size, ArrayList attachFiles, Intent intent, long msgId,
            final IFileAttachmentModelCallback attach,
            IOpFileAttachmentModelExt opFileAttachmentModelExt) {
        IMessageUtilsCallback opMessagePluginExt = Op09MessagePluginExt.sCallback;
        mFileAttachmentModel = (Op09FileAttachmentModelExt) opFileAttachmentModelExt;
        final boolean isCtFeature = MessageUtils.isSupportVCardPreview();
        if (isCtFeature) {
            fileAttachmentView.setBackgroundDrawable(null);
        }
        if ((!isCtFeature || !mFileAttachmentModel.isVCard()) && name2 != null) {
            name2.setText("");
            name2.setVisibility(View.GONE);
        }

        if (isCtFeature) {
            thumb.setVisibility(View.GONE);
            thumb.setVisibility(View.VISIBLE);
        }
        String nameText = "";
        Drawable drawable = null;
        if (isCtFeature) {

            if (mFileAttachmentModel.isVCard()) {
                nameText = mFileAttachmentModel.getDisplayName();
                if (TextUtils.isEmpty(nameText)) {
                    nameText = opMessagePluginExt.getString(
                            IMessageUtilsCallback.attachment_vcard_name,
                            mFileAttachmentModel.getSrc());
                }
                drawable = opMessagePluginExt
                        .getDrawable(IMessageUtilsCallback.ipmsg_chat_contact_vcard);
                if (name2 != null && mFileAttachmentModel.getContactCount() > 1) {
                    name2.setText(" +" + (mFileAttachmentModel.getContactCount() - 1));
                    name2.setVisibility(View.VISIBLE);
                }
            } else if (mFileAttachmentModel.isVCalendar()) {
                nameText = opMessagePluginExt.getString(
                        IMessageUtilsCallback.attachment_vcalendar_name,
                        mFileAttachmentModel.getSrc());
                drawable = opMessagePluginExt
                        .getDrawable(IMessageUtilsCallback.ipmsg_chat_contact_calendar);

            }

            name.setText(nameText);
            thumb.setImageDrawable(drawable);
            size.setText(Op09MmsUtils.getHumanReadableSize(mFileAttachmentModel.getAttachSize()));
        }

    }




    // / M: OP09 Feature: CancelDownloadMms; download mms listener;
    private OnClickListener mDownloadBtnListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {
            // add for gemini
            int subId = 0;
            // get sub id by uri
            Cursor cursor = SqliteWrapper.query(mUIContext, mUIContext.getContentResolver(),
                    mMessageItemExt.mMessageUri, new String[] {Telephony.Mms.SUBSCRIPTION_ID},
                    null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        subId = cursor.getInt(0);
                    }
                } finally {
                    cursor.close();
                }
            }
            Intent intent = new Intent();
            intent.setClassName("com.android.mms",
                                "com.android.mms.transaction.TransactionService");
            intent.putExtra("bundle_uri", mMessageItemExt.mMessageUri.toString());
            intent.putExtra("type", 1);
            intent.putExtra("subscription", subId);
            mUIContext.startService(intent);
        }
    };

    // / M: OP09 Feature: CancelDownloadMms; listener for cancelling download
    // mms;
    private OnClickListener mCancelDownloadListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Op09MmsCancelDownloadExt.getIntance(mUIContext)
                .cancelDownload(mMessageItemExt.mMessageUri);
        }
    };

    @Override
    public void hideAllView() {
        hideAllButton();
    }
    @Override
    public void inflateDownloadControls(Activity activity, LinearLayout downladBtnLayout,
            TextView expireText) {
        mActivity = activity;
        mExpireText = expireText;
        if (downladBtnLayout == null) {
            return;
        }
        downladBtnLayout.setVisibility(View.VISIBLE);
        mDownloadButton = (Button) downladBtnLayout.getChildAt(0);
        mDownloadingLabel = (TextView) downladBtnLayout.getChildAt(1);
    }

    /// M: --------------------------OP09 Plug-in Re-factory-------------------------
    private Button mDownloadButton;
    private TextView mDownloadingLabel;
    private Activity mActivity;
    private TextView mExpireText;
    private static final String[] MASS_SMS_PROJECTION = new String[] {Sms.THREAD_ID, Sms._ID,
        Sms.TYPE, Sms.DATE};

    private static final int COLUMN_MSG_SMS_THREAD_ID = 0;
    private static final int COLUMN_MSG_SMS_ID = 1;
    private static final int COLUMN_MSG_SMS_TYPE = 2;
    private static final int COLUMN_MSG_SMS_DATE = 3;

    private static final int MSG_TYPE_SMS = 0;

    private void showDownloadButton(final Uri messageUri, final CheckBox selectedBox,
            final long msgId, final boolean deviceStorageIsFull,
            final OnClickListener downloadBtnListener,
            final OnClickListener canceldownloadListener) {
        Log.d(TAG, "op09 showDownloadButton");
        if (mDownloadButton == null) {
            Log.d(TAG, "[showDownloadButton] failed, as mDownloadButton == null");
            return;
        }
        mDownloadButton.setVisibility(View.VISIBLE);

        Uri notificationUri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
        final Op09MmsCancelDownloadExt mmsCancelDownload =
                                    new Op09MmsCancelDownloadExt(mResourceContext);
        int actionStatus = mmsCancelDownload.getStateExt(notificationUri);
        Log.d(TAG, "MessageListItemHost. show download BUTOON;" + actionStatus);
        if (actionStatus == Op09MmsCancelDownloadExt.STATE_DOWNLOADING
            || actionStatus == Op09MmsCancelDownloadExt.STATE_CANCELLING
            || actionStatus == Op09MmsCancelDownloadExt.STATE_ABORTED) {
            hideDownloadButton(messageUri, canceldownloadListener, selectedBox, msgId);
            return;
        }

        if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.VISIBLE);
            mDownloadButton.setEnabled(true);
            mDownloadButton.setText(mResourceContext.getString(R.string.download));
            mDownloadButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if (selectedBox != null && selectedBox.getVisibility() == View.VISIBLE) {
                        return;
                    }
                    if (!MessageUtils.allowSafeDraft(mActivity, mResourceContext,
                            deviceStorageIsFull, true, MessageUtils.TOAST_TYPE_FOR_DOWNLOAD_MMS)) {
                        return;
                    }

                    int simId = 0;
                    Cursor cursor = SqliteWrapper.query(mUIContext, mUIContext.getContentResolver(),
                        messageUri, new String[] {"sim_id"}, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                                simId = cursor.getInt(0);
                            }
                        } finally {

                            cursor.close();
                        }
                    }

                    mmsCancelDownload.markStateExt(messageUri,
                        Op09MmsCancelDownloadExt.STATE_DOWNLOADING);
                    mDownloadingLabel.setVisibility(View.VISIBLE);
                    mDownloadButton.setVisibility(View.GONE);
                    mDownloadButton.setEnabled(false);
                    downloadBtnListener.onClick(v);
                    v.setVisibility(View.GONE);
                    Log.d(TAG, "op09 hideDownloadButton");
                    hideDownloadButton(messageUri, canceldownloadListener, selectedBox, msgId);
                }
            });
        }
        if (mDownloadingLabel != null) {
            mDownloadingLabel.setVisibility(View.GONE);
        }
    }

    private void hideDownloadButton(final Uri messageUri,
            final OnClickListener canceldownloadListener, final CheckBox selectedBox, long msgId) {
        Log.d(TAG, "hideDownloadButton messageUri:" + messageUri + "\t msgId:" + msgId);
        /**
         * Hide the download button and show the cancel download button.
         * Before show cancel download button should check the Mms setting of downloading Mms
         */
        if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.GONE);
        }
        Uri notificationUri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
        final Op09MmsCancelDownloadExt mmsCancelDownloadExt =
            new Op09MmsCancelDownloadExt(mUIContext);
        int actionStatus = mmsCancelDownloadExt.getStateExt(notificationUri);
        Log.d(TAG, "MessageListItemHost. Mms CacenlingStatus:" + actionStatus);
        if (actionStatus == Op09MmsCancelDownloadExt.STATE_DOWNLOADING) {
            if (mDownloadButton != null) {
                mDownloadButton.setText(mResourceContext.getString(R.string.cancel));
                mDownloadButton.setEnabled(true);
                mDownloadButton.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        // add for multi-delete
                        v.setEnabled(false);
                        if (selectedBox != null && selectedBox.getVisibility() == View.VISIBLE) {
                            return;
                        }
                        canceldownloadListener.onClick(v);
                    }
                });
                Log.d(TAG, "op09 show cancel download button");
                mDownloadButton.setVisibility(View.VISIBLE);
            }
        }

        if (mDownloadingLabel != null) {
            mDownloadingLabel.setVisibility(View.VISIBLE);
        }
    }


    private void hideAllButton() {
        if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.GONE);
        }
        if (mDownloadingLabel != null) {
            mDownloadingLabel.setVisibility(View.GONE);
        }
        if (mExpireText != null) {
            mExpireText.setVisibility(View.GONE);
        }
    }

    private void drawMassTextMsgStatus(Context context, LinearLayout statusLayout, boolean isSms,
            long timestamp) {
        Log.d(TAG, "drawMassTextMsgStatus; context:" + context + "\tisSMS:" + isSms
            + "\ttimeStamp:" + timestamp);
        if (context == null || !isSms || timestamp >= 0) {
            drawMassTextMsgStatus(statusLayout, 0, 0, 0, "");
            return;
        }
        int sendingCount = 0;
        int sendSuccess = 0;
        int sendFailed = 0;
        long sendDate = 0;
        Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI, MASS_SMS_PROJECTION,
            " ipmsg_id = ? ", new String[] {timestamp + ""}, null);
        try {
            if (cursor == null || cursor.getCount() < 2) {
                sendingCount = 0;
                sendSuccess = 0;
                sendFailed = 0;
            } else {
                while (cursor.moveToNext()) {
                    int type = cursor.getInt(COLUMN_MSG_SMS_TYPE);
                    sendDate = cursor.getLong(COLUMN_MSG_SMS_DATE);
                    switch (type) {
                        case Sms.MESSAGE_TYPE_OUTBOX:
                        case Sms.MESSAGE_TYPE_QUEUED:
                            sendingCount++;
                            break;
                        case Sms.MESSAGE_TYPE_SENT:
                            sendSuccess++;
                            break;
                        case Sms.MESSAGE_TYPE_FAILED:
                            sendFailed++;
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "drawMassTextMsgStatus error.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "sendingCount:" + sendingCount + " sendSuccess:" + sendSuccess +
             " sendFailed:" + sendFailed);
        drawMassTextMsgStatus(statusLayout, sendingCount, sendSuccess, sendFailed, MessageUtils
                .getShortTimeString(context, sendDate));
    }

    private void drawMassTextMsgStatus(LinearLayout statusLayout, int sendingMsgCount,
            int sendMsgSuccessCount, int sendMsgFailedCount, String smsDate) {
        Log.d(TAG, "sendingMsgCount:" + sendingMsgCount + " sendMsgSuccessCount:"
            + sendMsgSuccessCount + " sendMsgFailedCount:" + sendMsgFailedCount);
        if (statusLayout == null) {
            return;
        }
        statusLayout.getChildAt(0).setVisibility(View.GONE);
        int baseIndex = 1;
        TextView dateView = (TextView) statusLayout.getChildAt(baseIndex);
        ImageView sendingImg = (ImageView) statusLayout.getChildAt(baseIndex + 1);
        TextView sendingTxt = (TextView) statusLayout.getChildAt(baseIndex + 2);
        ImageView successImg = (ImageView) statusLayout.getChildAt(baseIndex + 3);
        TextView successTxt = (TextView) statusLayout.getChildAt(baseIndex + 4);
        ImageView failedImg = (ImageView) statusLayout.getChildAt(baseIndex + 5);
        TextView failedTxt = (TextView) statusLayout.getChildAt(baseIndex + 6);
        ImageView deliveredIndicator = (ImageView) statusLayout.getChildAt(baseIndex + 7);
        if ((sendMsgSuccessCount + sendMsgFailedCount + sendingMsgCount) < 2) {
            if (sendingImg != null) {
                sendingImg.setVisibility(View.GONE);
                sendingTxt.setVisibility(View.GONE);
            }
            if (successImg != null) {
                successImg.setVisibility(View.GONE);
                successTxt.setVisibility(View.GONE);
            }
            if (failedImg != null) {
                failedImg.setVisibility(View.GONE);
                failedTxt.setVisibility(View.GONE);
            }
            return;
        }
        if (deliveredIndicator != null) {
            deliveredIndicator.setVisibility(View.GONE);
        }
        if (sendingMsgCount <= 0) {
            if (sendingImg != null) {
                sendingImg.setVisibility(View.GONE);
            }
            if (sendingTxt != null) {
                sendingTxt.setVisibility(View.GONE);
            }
        } else {
            if (sendingImg != null) {
                /// M: keep the same action with the common.
                sendingImg.setVisibility(View.VISIBLE);
                sendingTxt.setVisibility(View.GONE);
            }
            if (successImg != null) {
                successImg.setVisibility(View.GONE);
                successTxt.setVisibility(View.GONE);
            }
            if (failedImg != null) {
                failedImg.setVisibility(View.GONE);
                failedTxt.setVisibility(View.GONE);
            }
            return;
        }
        if (sendMsgSuccessCount <= 0) {
            if (successImg != null) {
                successImg.setVisibility(View.GONE);
            }
            if (successTxt != null) {
                successTxt.setVisibility(View.GONE);
            }
        } else {
            if (successImg != null) {
                if (sendMsgFailedCount > 0) {
                    successImg.setVisibility(View.VISIBLE);
                    successTxt.setVisibility(View.VISIBLE);
                    successTxt.setText(sendMsgSuccessCount + "");
                } else {
                    successImg.setVisibility(View.GONE);
                    successTxt.setVisibility(View.GONE);
                    if (deliveredIndicator != null) {
                        deliveredIndicator.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        if (sendMsgFailedCount <= 0) {
            if (failedImg != null) {
                failedImg.setVisibility(View.GONE);
            }
            if (failedTxt != null) {
                failedTxt.setVisibility(View.GONE);
            }
        } else {
            if (failedImg != null) {
                failedImg.setVisibility(View.VISIBLE);
                if (sendMsgSuccessCount > 0) {
                    failedTxt.setVisibility(View.VISIBLE);
                    failedTxt.setText(sendMsgFailedCount + "");
                } else {
                    failedTxt.setVisibility(View.GONE);
                }
            }
        }

        /// M: always show sent time;
        if (dateView != null && ((sendingMsgCount + sendMsgFailedCount
                + sendMsgSuccessCount) > 1)) {
            dateView.setVisibility(View.VISIBLE);
            dateView.setText(smsDate);
        }
    }

    private void setNotifyContent(String address, String subject, String msgSizeText,
            String expireText, TextView expireTextView) {
        if (expireTextView == null) {
            return;
        }
        expireTextView.setText(address + "\n" + subject + "\n" + msgSizeText + "\n" + expireText);
    }

    private boolean needEditFailedMessge(Context context, long msgId, long timeStamp) {
        if (timeStamp >= 0) {
            return true;
        }
        Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI, MASS_SMS_PROJECTION,
            " ipmsg_id < 0 and ipmsg_id = ? ", new String[] {timeStamp + ""}, null);
        try {
            if (cursor == null || cursor.getCount() < 2) {
                return true;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "needEditFailedMessge error.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

//    private String getSentDateStr(Context context, String srcTxt, long msgId, int msgType,
//            long smsSentDate, int boxId) {
//        Log.d(TAG, "smsSentDate:" + smsSentDate + " msgType:" + msgType);
//        if (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
//            return srcTxt;
//        }
//        if (boxId == Mms.MESSAGE_BOX_INBOX) {
//            if (smsSentDate > 0) {
//                /// M: this is sms date
//                return MessageUtils.getShortTimeString(context, smsSentDate);
//            }
//            if (msgType != MSG_TYPE_SMS) {
//                /// M: this is mms date
//                Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
//                String[] projection = new String[] {Mms.DATE_SENT};
//                Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
//                long dateSent = 0;
//                if (c != null) {
//                    try {
//                        if (c.moveToFirst()) {
//                            dateSent = c.getLong(0);
//                        }
//                    } finally {
//                        c.close();
//                    }
//                }
//                if (dateSent == 0) {
//                    Log.e(TAG, "Failed to load the message: " + uri);
//                    return srcTxt;
//                }
//                Log.d(TAG, "[getSentDateStr]: dateSent = " + dateSent);
//                return MessageUtils.getShortTimeString(context, dateSent * 1000L);
//            }
//        }
//        return srcTxt;
//    }


    private void showSimType(Context context, long subId, TextView textView) {
        Drawable simTypeDraw = null;
        textView.setVisibility(View.VISIBLE);
        SubscriptionInfo simInfo = MessageUtils.getSimInfoBySubId(context, subId);
        if (simInfo != null) {
            Bitmap origenBitmap = simInfo.createIconBitmap(context);
            Bitmap bitmap = MessageUtils.resizeBitmap(mResourceContext, origenBitmap);
            Log.d(TAG, "showSimType for MessageListItem");
            simTypeDraw = new BitmapDrawable(context.getResources(), bitmap);
        } else {
            simTypeDraw = mResourceContext.getResources().getDrawable(
                                  R.drawable.sim_indicator_no_sim_mms);
        }
        if (textView != null) {
            textView.setText("");
            textView.setCompoundDrawablesWithIntrinsicBounds(simTypeDraw, null, null, null);
            Log.d(TAG, "showSimType for MessageListItem :" + textView.getText());
        }
    }

    public static void setDualTime(Context context, boolean isRecievedMsg, long subId,
            TextView dateView, LinearLayout linearLayout, String timeDate) {
        if (!isRecievedMsg || linearLayout == null) {
            Log.d(TAG, "Do not set dual time, just return");
            return;
        }

        /// M: Set sent time text
        dateView.setVisibility(View.VISIBLE);
        dateView.setText(context.getString(R.string.time_send));

        linearLayout.setVisibility(View.VISIBLE);
        /// M:Set text for receive time
        TextView tv = (TextView) linearLayout.getChildAt(0);
        tv.setText(context.getString(R.string.time_receive));

        /// M: set time for receive time
        TextView tv2 = (TextView) linearLayout.getChildAt(1);
        tv2.setText(timeDate);

        /// M: Get the sim card's status of roaming.
        boolean isInternational = MessageUtils.isInternationalRoamingStatusBySubId(context, subId);
        Log.d(TAG, "ternational status:" + isInternational);

        /// M: set timeZone
        TextView timeZone = (TextView) linearLayout.getChildAt(2);
        Calendar cal = Calendar.getInstance();
        String displayName = cal.getTimeZone().getDisplayName();
        if (isInternational) {
            /// M: Get the status of auto time zone. 1: auto get time zone; 0: no;
            int autoTimeZone = Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 1);
            Log.d(TAG, "Auto time zone:" + autoTimeZone);
            String date = (String) dateView.getText();
            if (autoTimeZone == 0) {
                timeZone.setText(displayName);
            } else {
                timeZone.setText(context.getString(R.string.local_time_msg));
            }
        } else {
            timeZone.setVisibility(View.GONE);
        }
    }
}
