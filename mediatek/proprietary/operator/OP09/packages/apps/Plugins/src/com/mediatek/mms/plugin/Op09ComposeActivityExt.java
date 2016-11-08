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
import java.util.Arrays;
import java.util.List;

import android.view.inputmethod.InputMethodManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;

import com.android.mms.data.ContactList;
import com.android.mms.ui.RecipientsEditor;

import com.android.mtkex.chips.RecipientEntry;
import com.android.mtkex.chips.MTKRecipientEditTextView;

import com.mediatek.mms.callback.IMessageListAdapterCallback;
import com.mediatek.mms.callback.ISlideshowModelCallback;
import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.callback.IComposeActivityCallback;
import com.mediatek.mms.callback.IWorkingMessageCallback;
import com.mediatek.mms.ext.DefaultOpComposeExt;
import com.mediatek.mms.ext.IOpAttachmentEditorExt;
import com.mediatek.mms.ext.IOpMessageItemExt;
import com.mediatek.mms.ext.IOpWorkingMessageExt;
import com.mediatek.op09.plugin.R;

public class Op09ComposeActivityExt extends DefaultOpComposeExt {

    private Context mResourceContext;
    private Op09MmsTextSizeAdjustExt mMmsTextSizeAdjustExt;
    private boolean mNeedConfirmMmsToSms = true;
    private long mMessageGroupId = 0;

    private static final String[] SMS_PROJECTION = new String[] {
        Sms.ADDRESS,
        Sms.DATE,
        Sms.TYPE,
        Sms.ERROR_CODE,
        Sms._ID,
        Sms.SUBSCRIPTION_ID};
    private static final int COLUMN_ADDRESS = 0;
    private static final int COLUMN_DATE = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_ERROR_CODE = 3;
    private static final int COLUMN_MSG_ID = 4;
    private static final int COLUMN_SIM_ID = 5;

    private static final String PREF_KEY_SHOW_DIALOG = "pref_key_show_dialog";
    private static String RESEND_MESSAGE_ACTION = "com.mediatek.mms.op09.RESEND_MESSAGE";

    public Op09ComposeActivityExt(Context context) {
        super(context);
        mResourceContext = context;
    }

    private static final String TAG = "Op09ComposeActivityExt";

    // For op09 cc feature.
    public static final int REQUEST_CODE_PICK_CC             = 131;


    /// M: add for number balance ; add in OP09 project as a common function
    private static final String PICK_CONTACT_NUMBER_BALANCE = "NUMBER_BALANCE";

    static final int MSG_SEND_SLIDESHOW   = 2;


    ///M: the below menu id must is same as host, can not change the number @{
    private static final int MENU_DELIVERY_REPORT       = 20;
    private static final int MENU_PREFERENCES           = 31;
    private static final int MENU_SAVE_MESSAGE_TO_SUB   = 32;
    public static final int MSG_LIST_SHOW_MSGITEM_DETAIL = 3600;
    /// @}

    /// M: add for CT feature menu
    private static final int MENU_ADD_MMS_CC            = 1000;
    private static final int MENU_SPLIT_MESSAGE         = 1001;
    private static final int MENU_SPLIT_THREAD          = 1002;

    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 106;

    private static final int MSG_SAVE_MESSAGE_TO_SUB_FAILED_SUB_FULL     = 110;


    private static final int MSG_RESUME_SEND_BUTTON = 112;

    private static final long RESUME_BUTTON_INTERVAL = 1000;

    public static final String SHOW_EMAIL_ADDRESS = "pref_key_show_email_address";

    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";

    public static final String SELECTION_CONTACT_RESULT = "contactId";

    /// M: add for OP09, mms cc feature. UI control for editing cc recipients
    private RecipientsEditor mRecipientsCcEditor;

    // / M: Code analyze 048, Add this can send msg from a marked sub card
    // / which is delivered in Intent.@{
    private int mSendSubIdForDualBtn = -1;
    // / @}

    private Activity mComposeActivity;
    private IComposeActivityCallback mComposeCallback;
    private Op09WorkingMessageExt mWorkingMessage;
    private Handler mUiHandler;
    private ContextMenu mChipViewMenu = null;
    private Op09DualSendButton mBottomDualSendButton;
    private Op09AttachmentEditorExt mOp09AttachmentEditor;
    private View mOriginSmsSendButton;
    private View mOriginMmsSendButton;

    @Override
    public void setConfirmMmsToSms(boolean needConfirm) {
        mNeedConfirmMmsToSms = needConfirm;
    }

    @Override
    public Uri onQueryMsgListRun(Uri uriSrc, long threadId) {
        Uri uri = null;
        if (MessageUtils.isMassTextEnable()) {
            uri = getConverationUriForMassingMessage(threadId);
        } else {
            uri = uriSrc;
        }
        return uri;
    }

    @Override
    public boolean handleAttachmentEditorHandlerMessage(Message msg, boolean sendButtonCanResponse,
            IWorkingMessageCallback opWorkingMessageCallback, ISlideshowModelCallback slideshow,
            boolean compressingImage) {
        switch (msg.what) {
            case MSG_SEND_SLIDESHOW:
                if (mComposeCallback.isPreparedForSendingCallback()) {
                    /// M: add for OP09 @{
                    if (MessageUtils.isDualSendButtonEnable() && msg != null) {
                        mSendSubIdForDualBtn = msg.getData().getInt("send_sub_id");
                    }
                    /// @}
                    /// M: Code analyze 028, Before sending message,check the recipients count
                    /// and add sub card selection dialog if multi sub cards exist.@{
                    mComposeCallback.checkRecipientsCountCallback();
                    /// @}
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onMessageListItemHandler(Message msg, String timestamp) {

        switch (msg.what) {
            case MSG_LIST_SHOW_MSGITEM_DETAIL:
                if (MessageUtils.isMassTextEnable()) {
                    long time = 0;
                    if (timestamp != null) {
                        time = Long.parseLong(timestamp);
                    }
                    showMassTextMsgDetail(mComposeActivity, time);
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean showMessageDetails(String timeStamp) {
        if (MessageUtils.isMassTextEnable()
                && showMassTextMsgDetail(mComposeActivity, Long.parseLong(timeStamp))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean updateCounter(CharSequence text, TextView textCounter,
            final IWorkingMessageCallback workingMessageCallback) {
        if (text.length() == 0) {
            textCounter.setVisibility(View.GONE);
            /// M: OP09 @{
            if (MessageUtils.isDualSendButtonEnable()) {
                int[] paramsOp09 = null;
                int encodingTypeOp09 = SmsMessage.ENCODING_UNKNOWN;
                if (MessageUtils.isEnableSmsEncodingType()) {
                    encodingTypeOp09 = MessageUtils.getSmsEncodingType(mComposeActivity);
                }
                paramsOp09 = SmsMessage.calculateLength(text, false, encodingTypeOp09);
                final int remainingInCurrentMessageOp09 = paramsOp09[2];
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBottomDualSendButton.updateTextCounter(
                                workingMessageCallback.requiresIpMms(),
                                remainingInCurrentMessageOp09, 1);
                    }
                }, 100);
            }
            /// @}
            workingMessageCallback.setLengthRequiresMmsCallback(false, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateCounterUiRun(TextView textEditor, int remainingInCurrentMessage,
            int msgCount, IWorkingMessageCallback workingMessageCallback) {
        /// M: OP09 text counter {
        if (MessageUtils.isDualSendButtonEnable()) {
            mBottomDualSendButton.updateTextCounter(workingMessageCallback.requiresIpMms(),
                    remainingInCurrentMessage, msgCount);
            return true;
        }
        /// @}
        return false;
    }

    @Override
    public boolean onDeleteMessageListenerClick(AsyncQueryHandler backQueryHandler, long msgId,
            long timeStamp) {
        Log.d(TAG, "onDeleteMessageListenerClick: msgId:" + msgId + " timeStamp:" + timeStamp);
        if (timeStamp >= 0) {
            return false;
        }

        if (backQueryHandler != null) {
            backQueryHandler.startDelete(9700, null, android.provider.Telephony.Sms.CONTENT_URI,
                "ipmsg_id = ?", new String[] {timeStamp + ""});
        }
        return true;
    }

    @Override
    public boolean confirmSendMessageIfNeeded(boolean isRecipientsEditorVisible,
                                                    boolean hasInvalidRecipient) {
        /// M: for length required MMS for op09@{
        if (MessageUtils.isChangeLengthRequiredMmsToSmsEnable()) {
            if (mNeedConfirmMmsToSms) {
                mComposeCallback.confirmForChangeMmsToSmsCallback();
                return true;
            } else {
                setConfirmMmsToSms(true);
            }
        }
        /// @}
        if (isRecipientsEditorVisible) {
            if (hasInvalidRecipient) {
            /** M: support mms cc feature. CT requested. here use simple logic.
             *  check to and cc only show one dialog, if users ignore To invalid address,
             *  don't show cc invalid too.
             */
            } else if (isRecipientsCcEditorVisible()
                        && mRecipientsCcEditor.hasInvalidRecipient(true)) {
                //updateSendButtonState();
                String title = getResourcesString(R.string.has_invalid_cc, mRecipientsCcEditor
                        .formatInvalidNumbers(true));
                new AlertDialog.Builder(mComposeActivity)
                    .setCancelable(false)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(title)
                    .setMessage(mResourceContext.getString(R.string.invalid_recipient_message))
                    .setPositiveButton(
                        mResourceContext.getString(R.string.try_to_send),
                        new SendIgnoreInvalidRecipientListener())
                    .setNegativeButton(mResourceContext.getString(R.string.mms_cancel),
                    new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            mComposeCallback.callbackUpdateSendButtonState();
                        }
                    }).show();
                return true;
            }
        }

        return false;
    }

    private class SendIgnoreInvalidRecipientListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            /// M: Code analyze 030, Check condition before sending message.@{
            mComposeCallback.callbackCheckConditionsAndSendMessage(true);
            /// @}
            dialog.dismiss();
        }
    }

    @Override
    public boolean onCreateContextMenu(boolean isSmsEnabled, boolean isSms, int subCount,
            boolean isSending, MenuItem.OnMenuItemClickListener l, ContextMenu menu, View v,
            ContextMenuInfo menuInfo, long messageGroupId, int messagesCount) {
        boolean ret = false;
        if (isSmsEnabled) {
            if (isSms) {
                if (subCount > 0 && !isSending) {
                    // M: replace "SUB" to "UIM". but the click action is processed at host@{
                    String str = Op09StringReplacementExt.getInstance(getBaseContext()).getStrings(
                            Op09StringReplacementExt.SAVE_MSG_TO_CARD);
                    if (MessageUtils.isStringReplaceEnable() && str != null) {
                        menu.add(0, MENU_SAVE_MESSAGE_TO_SUB, 0, str).setOnMenuItemClickListener(l);
                        ret = true;
                    } // @}
                }
            }
        }
        // M: For OP09: split message apart @{
        if (MessageUtils.isMassTextEnable() && messageGroupId < 0) {
            mMessageGroupId = messageGroupId;
            menu.add(0, MENU_SPLIT_MESSAGE, 0,
                    mResourceContext.getString(R.string.split_message_apart))
                    .setOnMenuItemClickListener(l);
        }
        // M: @}
        return ret;
    }

    @Override
    public void editMmsMessageItem(IOpWorkingMessageExt opWorkingMessageExt) {
        /// M: add for mms cc feature. OP09 requested.
        if (((Op09WorkingMessageExt) opWorkingMessageExt).hasMmsCc()) {
            showRecipientsCcEditor(true);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item, IOpMessageItemExt opMessageItemExt,
            Intent intentMultiSave, Intent deliveryIntent) {
        switch (item.getItemId()) {
            case MENU_DELIVERY_REPORT:
                /// M: add for OP09; @{
                if (MessageUtils.isMassTextEnable()) {
                    showDeliveryReportForCT(((Op09MessageItemExt) opMessageItemExt).mMsgId,
                            ((Op09MessageItemExt) opMessageItemExt).mType,
                            ((Op09MessageItemExt) opMessageItemExt).mIpMessageId, deliveryIntent);
                    return true;
                }
                /// @}
            case MENU_SPLIT_MESSAGE:
                splitSingleMessage(mComposeActivity, mMessageGroupId);
                break;
            default:
        }
        return false;
    }

    @Override
    public boolean lockMessage(final Context context, Uri uri, final long msgId,
                                            final long timeStamp, boolean locked) {
        /// M: For OP09 @{
        if (uri == Mms.CONTENT_URI) {
            return false;
        }
        if (MessageUtils.isMassTextEnable()) {
            final ContentValues values = new ContentValues(1);
            values.put("locked", locked ? 1 : 0);
            new Thread(new Runnable() {

                public void run() {
                    if (timeStamp < 0) {
                        context.getContentResolver().update(Sms.CONTENT_URI, values, "ipmsg_id = ?",
                            new String[] {timeStamp + ""});
                    } else {
                        context.getContentResolver().update(Sms.CONTENT_URI, values, "_id = ?",
                            new String[] {msgId + ""});
                    }
                }
            }).start();
            return true;
        }
        return false;
        /// @}
    }

    @Override
    public String updateTitle(int cnt, String number, String subTitle) {
        if (cnt == 1 && MessageUtils.isNumberLocationEnable()) {
            subTitle = getNumberLocation(mComposeActivity, number);
        }
        return subTitle;
    }

    @Override
    public void initRecipientsEditor(Bundle bundle, View recipientsCcEditor) {

        mRecipientsCcEditor = (RecipientsEditor) recipientsCcEditor;
        // For op09, if the mms process was killed, restore the cc status
        if (MessageUtils.isSupportSendMmsWithCc() && bundle != null
                && bundle.get("ccrecipients") != null) {
            showRecipientsCcEditor(true);
        }
    }

    @Override
    public void onCreate(IComposeActivityCallback ipComposeActivityCallback, Intent intent,
            IntentFilter intentFilter, Activity oldCompose, Activity compose,
            Bundle savedInstanceState, Handler uiHandler, ImageButton shareButton,
            LinearLayout panel, EditText textEditor) {
        if (!MessageUtils.isMultiComposeEnable() && oldCompose != null && !oldCompose.isFinishing()
                && savedInstanceState == null) {
            oldCompose.finish();
        }
        mComposeActivity = compose;
        mComposeCallback = ipComposeActivityCallback;
        mUiHandler = uiHandler;
        /// M: for OP09 Feature: dual send button @{
        if (MessageUtils.isDualSendButtonEnable()) {
            intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        }
        /// @}
        mMmsTextSizeAdjustExt = Op09MmsTextSizeAdjustExt.getInstance();
    }

    @Override
    public void onStart(ITextSizeAdjustHost host, Activity activity) {
        mMmsTextSizeAdjustExt.init(host, activity);
        mMmsTextSizeAdjustExt.refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // For op09, if the mms process was killed, restore the cc status
        if (MessageUtils.isSupportSendMmsWithCc() && mRecipientsCcEditor != null
                && isRecipientsCcEditorVisible()) {
            if (mRecipientsCcEditor.getRecipientCount() > 0) {
                ArrayList<String> numbers = (ArrayList<String>) (mRecipientsCcEditor.getNumbers());
                outState.putString("ccrecipients", TextUtils.join(";", numbers.toArray()));
            }
        }
    }

    @Override
    public void onResume(int subCount, String text, boolean isSmsEnabled,
            View recipientsEditor, View subjectTextEditor) {
        if (MessageUtils.isDualSendButtonEnable() && subCount < 1) {
            if (text != null) {
                mComposeCallback.updateCounterCallback(text, 0, 0, 0);
            }
        }
        setCCRecipientEditorStatus(isSmsEnabled);
    }

    @Override
    public boolean onProtocolChanged(boolean isMms, CharSequence text) {
        /// M: For OP09 DualSentButton Feature: @{
        if (MessageUtils.isDualSendButtonEnable()) {
            if (!isMms) {
                mComposeCallback.updateCounterCallback(text, 0, 0, 0);
            } else {
                // just show the mms charactor ,when reach the long sms limit
                mBottomDualSendButton.updateTextCounter(isMms, 0, 0);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu, boolean isSmsEnabled,
            boolean isRecipientsEditorVisible, int contactsSize, long threadId) {
        if (isSmsEnabled) {
            if (!isRecipientsEditorVisible) {
                // / M: Add for OP09: @{
                if (MessageUtils.isMassTextEnable()) {
                    if (contactsSize > 1) {
                        menu.add(0, MENU_SPLIT_THREAD, 0,
                                mResourceContext.getString(R.string.split_thread_apart));
                    }
                }
                // / @}
            }
            // / M: support mms cc feature. OP09 requested
            if (MessageUtils.isSupportSendMmsWithCc() && !isRecipientsCcEditorVisible()) {
                /**
                 * M: this constraint is not so needed, just for simple. cc
                 * recipients can be added as mms subject, but more logic need add.
                 */
                if (isRecipientsEditorVisible) {
                    menu.add(0, MENU_ADD_MMS_CC, 0,
                            mResourceContext.getString(R.string.add_mms_cc));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivityResult(int requestCode, Intent data,
            IOpWorkingMessageExt newOpWorkingMessageExt, IOpWorkingMessageExt opWorkingMessageExt) {
        switch (requestCode) {
            case REQUEST_CODE_CREATE_SLIDESHOW:
                if (newOpWorkingMessageExt != null) {
                    /// M: support mms cc feature. OP09 requested.
                    ((Op09WorkingMessageExt) newOpWorkingMessageExt).setMmsCc(
                            ((Op09WorkingMessageExt) opWorkingMessageExt).getMmsCc(), false);
                }
                return true;
                /// M: support mms cc feature. OP09 requested.
            case REQUEST_CODE_PICK_CC:
                if (data != null) {
                    processCCPickResult(data);
                    mComposeCallback.setIsPickContatct(false);
                }
                return true;
        }
        return false;
    }

    @Override
    public void drawBottomPanel(boolean hasSlideshow, boolean isUpdateAttachEditor,
            boolean isSmsEnabled) {
        if (hasSlideshow) {
            setCCRecipientEditorStatus(isSmsEnabled);
        } else {
            setCCRecipientEditorStatus(isSmsEnabled);
        }
    }

    @Override
    public void drawTopPanel(boolean isRecipientsEditorVisible,
                                    IOpWorkingMessageExt opWorkingMessageExt) {
        if (!(isRecipientsEditorVisible && isRecipientsCcEditorVisible())) {
            showRecipientsCcEditor(((Op09WorkingMessageExt)opWorkingMessageExt).hasMmsCc());
        }
    }

    @Override
    public boolean onClick(View v, ImageButton recipientsPicker) {
        if (v == recipientsPicker) {
            /// M: support mms cc feature. OP09 feature.
            if (isPickRecipientsCc()) {
                if (mRecipientsCcEditor.getRecipientCount() >= RECIPIENTS_LIMIT_CC) {
                    Toast.makeText(mComposeActivity, R.string.cannot_add_recipient,
                            Toast.LENGTH_SHORT).show();
                } else {
                    mComposeCallback.addIpContacts(
                                RECIPIENTS_LIMIT_CC - mRecipientsCcEditor.getNumbers().size(),
                                REQUEST_CODE_PICK_CC);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void initResourceRefs(LinearLayout buttonWithCounter, TextView textCounter,
            IOpAttachmentEditorExt attachmentEditor) {
        if (MessageUtils.isDualSendButtonEnable()) {
            mBottomDualSendButton = new Op09DualSendButton(mResourceContext);
            mOriginSmsSendButton = buttonWithCounter.getChildAt(4);
            mOriginMmsSendButton = buttonWithCounter.getChildAt(3);
            mBottomDualSendButton.initView(mComposeActivity,
                                    (ImageButton) buttonWithCounter.getChildAt(0),
                                    (ImageButton) buttonWithCounter.getChildAt(1),
                                    textCounter);
            mBottomDualSendButton.setOnClickListener(mDualSendBtnListener);
        }
        mOp09AttachmentEditor = (Op09AttachmentEditorExt) attachmentEditor;
    }

    @Override
    public Uri startMsgListQuery(Uri conversationUri, long threadId) {
        /// M: For OP09 @{
        Uri conversationUriSrc = conversationUri;
        if (MessageUtils.isMassTextEnable()) {
            conversationUriSrc = getConverationUriForMassingMessage(threadId);
        }
        return conversationUriSrc;
        /// @}
    }

    @Override
    public void saveDraft(IOpWorkingMessageExt opWorkingMessageExt) {
        /// M: add for mms cc feature. OP09 requested.
        if (MessageUtils.isSupportSendMmsWithCc() && mRecipientsCcEditor != null) {
            ((Op09WorkingMessageExt) opWorkingMessageExt).setMmsCc(
                    mRecipientsCcEditor.constructContactsFromInput(false), false);
        }
    }

    @Override
    public boolean sendMessage(IOpWorkingMessageExt opWorkingMessageExt, int subId) {
        /// M: For OP09 4GDataOnly Feature; @{
        if (Op09MmsUtils.getInstance().is4GDataOnly(this, subId)) {
            mComposeCallback.updateSendButtonStateCallback(true);
            return true;
        }
        /// @}

        /// M: support mms cc feature. Op09 requested.
        if (isRecipientsCcEditorVisible()) {
            Log.d(TAG, "sendMessage, cc editor visible set cc.");
            ((Op09WorkingMessageExt) opWorkingMessageExt).setMmsCc(
                    mRecipientsCcEditor.constructContactsFromInput(false), false);
        }
        return false;
    }

    @Override
    public void resetMessage() {
        /// M: add for mms cc feature. OP09 requested.
        showRecipientsCcEditor(false);

        /// M: support mms cc feature. OP09 requested.
        mLastRecipientCcCount = 0;
    }

    @Override
    public boolean updateSendButtonState(boolean requiresMms, int recipientCount,
                                    int smsRecipientLimit, int subCount) {
        /// M: For OP09 @{
        if (MessageUtils.isDualSendButtonEnable() && subCount > 0) {
            boolean enable = true;
            Log.d(TAG,"UpdateSendButton ,recipientCount = " + recipientCount);
            Log.d(TAG,"UpdateSendButton ,requiresMms = " + requiresMms);
            Log.d(TAG,"UpdateSendButton ,enabled = " + enable);
            mBottomDualSendButton.setEnabled(enable);
            hideSrcSendButton();
            mComposeCallback.updateTextEditorHintCallback();
            return true;
        }
        /// @}
        return false;
    }

    @Override
    public boolean initFocus() {
        /// M: add for OP09 CC recipents
        if (mRecipientsCcEditor != null && isRecipientsCcEditorVisible() &&
                mRecipientsCcEditor.isFocused() && (mRecipientsCcEditor.getRecipientCount() < 1)) {
                mRecipientsCcEditor.requestFocus();
                return true;
        }
        return false;
    }

    @Override
    public boolean handleUiMessage(Message msg, int subCount, int selectedSubId) {
        switch (msg.what) {
            case MSG_SAVE_MESSAGE_TO_SUB_FAILED_SUB_FULL:
                /// M: For OP09 Feature, replace "SIM" to "UIM". @{
                List<SubscriptionInfo> subInfoList = SubscriptionManager.from(mComposeActivity)
                        .getActiveSubscriptionInfoList();
                int slotId = -1;
                if (subCount == 1) {
                    slotId = subInfoList.get(0).getSimSlotIndex(); // getSlot();
                } else {
                    if (selectedSubId > 0) {
                        SubscriptionInfo subInfo = SubscriptionManager.from(mComposeActivity)
                                .getActiveSubscriptionInfo(selectedSubId);
                        if (subInfo != null) {
                            slotId = subInfo.getSimSlotIndex();
                        }
                    }
                }
                String ctString = Op09StringReplacementExt.getInstance(mComposeActivity)
                        .getStrings(Op09StringReplacementExt.UIM_FULL_TITLE);
                Toast.makeText(mComposeActivity,
                        getString(R.string.save_message_to_sim_unsuccessful) + ". "
                                + ((MessageUtils.isStringReplaceEnable()
                                        && ctString != null && slotId == 0)
                                ? ctString : getString(R.string.sim_full_title)),
                        Toast.LENGTH_SHORT).show();
                /// @}
                return true;
        }
        return false;
    }

    @Override
    public int getMessageAndSaveToSub(String[] numbers, String scAddress,
            ArrayList<String> messages, int smsStatus,
            long timeStamp, int subId, int srcResult) {
        /// M: Modify for OP09 : save massTextMsg to sub @{
        if (MessageUtils.isMassTextEnable()
            && srcResult != SmsManager.RESULT_ERROR_SUCCESS
            && srcResult != SmsManager.RESULT_ERROR_SIM_MEM_FULL) {
            Log.d(TAG, "save sms message to sim");
            if (numbers == null || numbers.length < 1) {
                return srcResult;
            }
            int result = -1;
            /// M: choose the first validate number to save to sim card.
            for (String number : numbers) {
                result = SmsManager.getSmsManagerForSubscriptionId(subId).copyTextMessageToIccCard(
                    scAddress, number, messages, smsStatus, timeStamp);
                if (result == 0 || result == 7) {
                    Log.d(TAG, "save sms message to sim successed: number:" + number);
                    return result;
                }
            }
            return srcResult;
        }
        /// @}
        return srcResult;
    }

    @Override
    public String showSubSelectedDialog(String src) {
        /// M: For OP09 Feature, replace "Sub" to "UIM". @{
        if (MessageUtils.isStringReplaceEnable()) {
            String str = Op09StringReplacementExt.getInstance(mComposeActivity).getStrings(
                    Op09StringReplacementExt.SELECT_CARD);
            return str;
        }
        /// @}
        return src;
    }

    @Override
    public boolean updateSendButton(boolean enabled, int subCount, boolean requiresMms,
                                                                  boolean hasSlideshow) {
        /// M: for OP09
        if (MessageUtils.isDualSendButtonEnable() && subCount > 0) {
            Log.d(TAG,"UpdateSendButton ,enabled = " + enabled);
            if (!hasSlideshow) {
                mBottomDualSendButton.setEnabled(enabled);
            }
            hideSrcSendButton();
            return true;
        }
        return false;
    }

    @Override
    public boolean addContacts(Intent intent, int pickCount, int requestCode) {
       /// M: OP09 add For pick limit: set number balance for picking contacts; As a common function
        intent.putExtra(PICK_CONTACT_NUMBER_BALANCE, pickCount);
        try {
            mComposeActivity.startActivityForResult(intent, requestCode);
            return true;
        } catch (ActivityNotFoundException e) {
            /// M: add for mms cc feature. OP09 requested.
            mPickRecipientsCc = false;
            return false;
        }

    }

    @Override
    public String checkRecipientsCount(String message, boolean requiresMms, int recipientCount,
            int mmsLimitCount,
            boolean isRecipientsEditorEmpty, boolean isConversationRecipientEmpty, Intent intent,
            IMessageListAdapterCallback msgListAdapter, List<SubscriptionInfo> subInfoList,
            boolean isRecipientsEditorVisible) {
        /// M: support mms cc feature. OP09 requested. add cc check.
        if (requiresMms && (recipientCount > mmsLimitCount || isRecipientsCcTooMany())) {
            /// M: support mms cc feature. OP09 requested.
            if (isRecipientsCcTooMany()) {
                String ccMessage = message;
                if (!ccMessage.equals("")) {
                    ccMessage += "\r\n";
                }
                ccMessage += getString(R.string.max_cc_message, RECIPIENTS_LIMIT_CC);
                return ccMessage;
            }
        }
        return message;
    }

    @Override
    public void onSubInforChanged() {
        /// M: For OP09;
        if (MessageUtils.isDualSendButtonEnable()) {
            mBottomDualSendButton.updateSendButton();
        }
    }

    @Override
    public boolean onReceive(String action, Runnable getSubInfoRunnable) {
        Log.d(TAG, "action = " + action);
        if (MessageUtils.isDualSendButtonEnable() && action != null
                && (action.equals(TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED)
                        || action.equals(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE))) {
            getSubInfoRunnable.run();
            if (mOp09AttachmentEditor != null) {
                mOp09AttachmentEditor.notifySubChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public void getSubInfoRunnable(boolean hasSlideshow) {
        if (MessageUtils.isDualSendButtonEnable()) {
            mBottomDualSendButton.updateSendButton();
            if (hasSlideshow) {
                mComposeCallback.callbackUpdateSendButtonState();
            }
        }
    }

    @Override
    public int getSmsEncodingType(int encodingType, Context context) {
        if (MessageUtils.isEnableSmsEncodingType()) {
            return MessageUtils.getSmsEncodingType(context);
        }
        return encodingType;
    }

    @Override
    public void onDiscardDraftListenerClick() {
        if (MessageUtils.isDualSendButtonEnable()) {
            mComposeCallback.hideIpInputMethod();
        }
    }

    @Override
    public boolean hideOrShowTopPanel(View topPanel) {
        if (isRecipientsCcEditorVisible()) {
            topPanel.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    @Override
    public boolean processNewIntent(Intent intent) {
        mSendSubIdForDualBtn = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        Log.d(TAG, "processNewIntent get subId from intent = " + mSendSubIdForDualBtn);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item, IOpWorkingMessageExt workingMessageExt,
            InputMethodManager inputMethodManager) {
        switch (item.getItemId()) {
            case MENU_PREFERENCES: {
                Intent settingIntent = new Intent();
                if (MessageUtils.isSupportTabSetting()) {
                    settingIntent.setAction("com.mediatek.action.MessageTabSettingActivity");
                    settingIntent.setPackage("com.android.mms");
                }
                mComposeActivity.startActivity(settingIntent);
                return true;
            }
            case MENU_ADD_MMS_CC:
                ((Op09WorkingMessageExt)workingMessageExt).setMmsCc(null, true);
                showRecipientsCcEditor(true);
                mComposeCallback.callbackUpdateSendButtonState();
                inputMethodManager.showSoftInput(mComposeActivity.getWindow().getCurrentFocus(),
                    InputMethodManager.SHOW_IMPLICIT);
                mRecipientsCcEditor.requestFocus();
                return true;
            case MENU_SPLIT_THREAD:
                long threadId = mComposeCallback.getCurrentThreadId();
                splitThreadMassingTextMessage(mComposeActivity, threadId);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void initActivityState(Intent intent) {
        mSendSubIdForDualBtn = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        Log.d(TAG, "init get subId from intent = " + mSendSubIdForDualBtn);
    }

    @Override
    public int subSelection() {
        int subId = 0;
        if (mSendSubIdForDualBtn >= 1) {
            subId = mSendSubIdForDualBtn;
            mSendSubIdForDualBtn = -1;
            Log.d(TAG, "send msg from mSendSubIdForDualBtn = " + subId);
            mComposeCallback.setSubIdCallback(subId);
            mComposeCallback.confirmSendMessageIfNeededCallback();
        }
        return subId;
    }

    @Override
    public void chipOnCreateContextMenu(ContextMenu menu) {
        mChipViewMenu = menu;
    }

    /**
     * M: For OP09 buttonListener.
     *
     * @return
     */
    Op09DualSendButton.OnClickListener mDualSendBtnListener =
                            new Op09DualSendButton.OnClickListener() {

        @Override
        public void onClick(View view, int subId) {
            mSendSubIdForDualBtn = subId;
            if (mComposeCallback.getSendButtonCanResponse()) {
                mComposeCallback.setSendButtonCanResponse(false);
                if (mComposeCallback.isPreparedForSendingCallback()) {
                    mComposeCallback.checkRecipientsCountCallback();
                    mUiHandler.sendEmptyMessageDelayed(MSG_RESUME_SEND_BUTTON,
                            RESUME_BUTTON_INTERVAL);

                    // / M: pop-up dialog when request delivery report which is
                    // diabled in roaming status. @{
                    SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(mComposeActivity);
                    boolean requestDeliveryReport = false;
                    requestDeliveryReport = prefs.getBoolean(Long.toString(mSendSubIdForDualBtn) +
                            "_" + SMS_DELIVERY_REPORT_MODE, false);

                    if (requestDeliveryReport) {
                        if (MessageUtils.isAllowDRWhenRoaming(mComposeActivity,
                                mSendSubIdForDualBtn)) {
                            // / M: Enable the dialog again when not in roaming
                            // status.
                            enableDRWarningDialog(mComposeActivity, true, mSendSubIdForDualBtn);
                        } else {
                            // / M: delivery report not allowed.
                            showDisableDRDialog(mComposeActivity, mSendSubIdForDualBtn);
                        }
                    }
                    // / @}
                } else {
                    mComposeCallback.setSendButtonCanResponse(true);
                    if (!mComposeCallback.isHasRecipientCountCallback()) {
                        new AlertDialog.Builder(mComposeActivity)
                                .setIconAttribute(android.R.attr.alertDialogIcon)
                                .setTitle(R.string.cannot_send_message)
                                .setMessage(R.string.cannot_send_message_reason)
                                .setPositiveButton(R.string.yes, new CancelSendingListener())
                                .show();
                    } else {
                        new AlertDialog.Builder(mComposeActivity)
                                .setIconAttribute(android.R.attr.alertDialogIcon)
                                .setTitle(R.string.cannot_send_message)
                                .setMessage(R.string.cannot_send_message_reason_no_content)
                                .setPositiveButton(R.string.yes, new CancelSendingListener())
                                .show();
                    }
                }
            }
        }
    };

    private class CancelSendingListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            mComposeCallback.requestRecipientsEditorFocus();
            dialog.dismiss();
            /// M: @{
            mComposeCallback.callbackUpdateSendButtonState();
            /// @}
        }
    }

    /**
     * Set CC Editor's Status.
     *
     * @param enable true: enable; false: disable.
     */
    private void setCCRecipientEditorStatus(boolean enable) {
        if (mRecipientsCcEditor != null && isRecipientsCcEditorVisible()) {
            mRecipientsCcEditor.setEnabled(enable);
            mRecipientsCcEditor.setFocusableInTouchMode(enable);
            mRecipientsCcEditor.setIsTouchable(enable);
        }
    }

    /// M: add for mms cc feature, OP09 requested. @{
    private final int RECIPIENTS_LIMIT_CC = MessageUtils.getMmsRecipientLimit();
    private int mLastRecipientCcCount = 0;

    private ProgressDialog mContactPickDialog;

    private final MTKRecipientEditTextView.ChipWatcher mCCChipWatcher =
        new MTKRecipientEditTextView.ChipWatcher() {
        public void onChipChanged(ArrayList<RecipientEntry> allChips,
                ArrayList<String> changedChipAddresses, String lastString) {
            if (mChipViewMenu != null) {
                mChipViewMenu.close();
                mChipViewMenu = null;
            }
            if (!isRecipientsCcEditorVisible()) {
                Log.w(TAG, "ChipWatcher: onChipChanged called with invisible mRecipientsCcEditor");
                return;
            }

            mRecipientsCcEditor.parseRecipientsFromChipWatcher(allChips,
                    changedChipAddresses, lastString, RECIPIENTS_LIMIT_CC);
            List<String> numbers = mRecipientsCcEditor.getNumbersFromChipWatcher();

            int recipientCount = numbers.size();
            if (recipientCount == 0) {
                mRecipientsCcEditor.setHint(getString(R.string.to_hint_ipmsg));
            }
            boolean tooMany = recipientCount > RECIPIENTS_LIMIT_CC;
            if (recipientCount != mLastRecipientCcCount) {
                // Don't warn the user on every character they type when they're over the limit,
                // only when the actual # of recipients changes.
                mLastRecipientCcCount = recipientCount;
                if (tooMany) {
                    String tooManyMsg = getString(R.string.too_many_recipients, recipientCount,
                            RECIPIENTS_LIMIT_CC);
                    Toast.makeText(mComposeActivity, tooManyMsg, Toast.LENGTH_LONG).show();
                }
            }

            if (MessageUtils.isSupportSendMmsWithCc()) {
                mWorkingMessage.setMmsCc(mRecipientsCcEditor.constructContactsFromInput(false),
                        true);
                mComposeCallback.updateCounterCallback(mComposeCallback.getTextEditorText(),
                                                                    0, 0, 0);
            }
        }
    };

    private void showRecipientsCcEditor(boolean show) {
        if (mRecipientsCcEditor == null) {
            if (!show) {
                return;
            }
        }

        if (!show) {
            mRecipientsCcEditor.setOnKeyListener(null);
            mRecipientsCcEditor.removeChipChangedListener(mCCChipWatcher);
            mRecipientsCcEditor.setVisibility(View.GONE);
            mComposeCallback.hideOrShowTopPanelCallback();
            Log.d(TAG, "hide cc editor.");
            return;
        }

        // M: indicate contain email address or not in RecipientsEditor candidates. @{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mComposeActivity);
        boolean showEmailAddress = prefs.getBoolean(SHOW_EMAIL_ADDRESS, true);
        Log.d(TAG, "showRecipientsCcEditor(), showEmailAddress = " + showEmailAddress);
        if (mRecipientsCcEditor.getAdapter() == null) {
            ChipsRecipientAdapter chipsAdapter = new ChipsRecipientAdapter(this);
            chipsAdapter.setShowEmailAddress(showEmailAddress);
            mRecipientsCcEditor.setAdapter(chipsAdapter);
        } else {
            ((ChipsRecipientAdapter) mRecipientsCcEditor.getAdapter())
                        .setShowEmailAddress(showEmailAddress);
        }
        // @}

        // Must grab the recipients before the view is made visible because getRecipients()
        // returns empty recipients when the editor is visible.
        ContactList recipients = mWorkingMessage.getMmsCc();
        while (!recipients.isEmpty() && recipients.size() > RECIPIENTS_LIMIT_CC) {
            recipients.remove(RECIPIENTS_LIMIT_CC);
        }
        /// @}
        mRecipientsCcEditor.populate(new ContactList());
        mRecipientsCcEditor.populate(recipients);

        setCCRecipientEditorStatus(mComposeCallback.getIsSmsEnabled());

        mRecipientsCcEditor.setOnCreateContextMenuListener(mComposeCallback
                .getOnCreateContextMenuListener());
        mRecipientsCcEditor.addChipChangedListener(mCCChipWatcher);

        mRecipientsCcEditor.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }

                // When the subject editor is empty, press "DEL" to hide the input field.
                if ((keyCode == KeyEvent.KEYCODE_DEL) && (mRecipientsCcEditor.length() == 0)) {
                    showRecipientsCcEditor(false);
                    mWorkingMessage.setMmsCc(null, true);
                    return true;
                }
                return false;
            }
        });

        mRecipientsCcEditor.setVisibility(View.VISIBLE);
        mComposeCallback.hideOrShowTopPanelCallback();
    }

    private boolean isRecipientsCcEditorVisible() {
        return (mRecipientsCcEditor != null)
                && (mRecipientsCcEditor.getVisibility() == View.VISIBLE);
    }

    private boolean mPickRecipientsCc = false;
    private boolean isPickRecipientsCc() {
        return ((mRecipientsCcEditor != null) && mRecipientsCcEditor.hasFocus())
                || mPickRecipientsCc;
    }

    private boolean isRecipientsCcTooMany() {
        return (mRecipientsCcEditor != null) &&
                    mRecipientsCcEditor.getRecipientCount() > RECIPIENTS_LIMIT_CC;
    }
    /// @}

    /**
     * Set CC pick contact result
     *
     * @param data the result of select contacts.
     */
    private void processCCPickResult(final Intent data) {
        if (mRecipientsCcEditor == null) {
            Log.i(TAG, "mRecipientsCcEditor is null, cannot set selected contacts");
            return;
        }
        final long[] contactsId = data
                .getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
        // // M: Add in OP09 projcet as a common function ; @{
        String numbersSelectedFromRecent = mComposeCallback.getNumbersFromIntentCallback(data);

        if (numbersSelectedFromRecent == null || numbersSelectedFromRecent.length() < 1) {
            // / M: add for ip message {@
            numbersSelectedFromRecent = data.getStringExtra(SELECTION_CONTACT_RESULT);
        }
        final String mSelectContactsNumbers = numbersSelectedFromRecent;
        // / @}
        Log.i(TAG,
                "processPickResult, data = " + data.toString() + ", contactsId = "
                        + Arrays.toString(contactsId) + ", mSelectContactsNumbers = "
                        + mSelectContactsNumbers);
        if ((contactsId == null || contactsId.length <= 0)
                && TextUtils.isEmpty(mSelectContactsNumbers)) {
            return;
        }

        // final Handler handler = new Handler();
        if (mContactPickDialog == null) {
            mContactPickDialog = new ProgressDialog(this);
            mContactPickDialog.setMessage(getText(R.string.adding_recipients));
            mContactPickDialog.setIndeterminate(true);
            mContactPickDialog.setCancelable(false);
        }

        // Only show the progress dialog if we can not finish off parsing the
        // return data in 1s,
        // otherwise the dialog could flicker.
        mUiHandler.postDelayed(mContactPickRunnable, 500);
        new Thread(new Runnable() {
            public void run() {
                mUiHandler.post(new Runnable() {
                    public void run() {
                        final ContactList list = new ContactList();
                        final ContactList allList = new ContactList();
                        //final boolean isDuplicate = processDuplicatePickResult(
                        //        mSelectContactsNumbers, mRecipientsCcEditor, contactsId, list,
                        //        allList);
                        if (list.size() > 0) {
                            mRecipientsCcEditor.clearFocus();
                            mRecipientsCcEditor.populate(list);
                        }
                        mRecipientsCcEditor.requestFocus();
                        //if (isDuplicate) {
                        //    Toast.makeText(mComposeActivity,
                        //            R.string.add_duplicate_recipients, Toast.LENGTH_SHORT).show();
                        //}
                    }
                });
            }
        }, "ComoseMessageActivity.processCCPickResult").start();
    }

    private Runnable mContactPickRunnable = new Runnable() {
        public void run() {
            if (mContactPickDialog != null) {
                mContactPickDialog.show();
            }
        }
    };

    /**
     * M: Add for OP09;
     *
     * @param messageId
     * @param type
     * @param groupId
     */
    private void showDeliveryReportForCT(long messageId, String type, long groupId,
            Intent deliveryIntent) {
        deliveryIntent.putExtra("message_id", messageId);
        deliveryIntent.putExtra("message_type", type);
        Op09MmsUtils.getInstance().setIntentDateForMassTextMessage(deliveryIntent, groupId);
        mComposeActivity.startActivity(deliveryIntent);
    }

    private String getResourcesString(int id, String mediaName) {
        Resources r = mResourceContext.getResources();
        return r.getString(id, mediaName);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mMmsTextSizeAdjustExt.dispatchTouchEvent(ev);
    }


    private boolean showMassTextMsgDetail(final Context context, long timeStamp) {
        Log.d(TAG, "showMassTextMsgDetail:" + timeStamp);
        if (context == null || timeStamp >= 0) {
            return false;
        }
        String[] datas = null;
        Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI, SMS_PROJECTION,
            "ipmsg_id = ?", new String[] {timeStamp + ""}, null);
        long msgIds[][] = null;
        try {
            if (cursor.getCount() < 2) {
                return false;
            }
            msgIds = new long[cursor.getCount()][2];
            datas = new String[cursor.getCount() + 1];

            int index = 0;
            int dataIndex = 1;
            while (cursor.moveToNext()) {
                ///M: set sms's address @{
                String addressStr = mResourceContext.getString(R.string.to_address_label)
                      + cursor.getString(COLUMN_ADDRESS);
                ///@}
                ///M: set sms's status @{
                String status = mResourceContext.getString(R.string.msg_status);
                msgIds[index][0] = -1;
                int type = cursor.getInt(COLUMN_TYPE);
                Log.d(TAG, "Sms Type:" + type);
                switch (type) {
                    case Sms.MESSAGE_TYPE_SENT:
                        status = status + mResourceContext.getString(R.string.msg_status_success);
                        break;
                    case Sms.MESSAGE_TYPE_DRAFT:
                        status = status + mResourceContext.getString(R.string.saved_label);
                        break;
                    case Sms.MESSAGE_TYPE_FAILED:
                        status = status + mResourceContext.getString(
                            R.string.msg_status_failed);
                        msgIds[index][0] = cursor.getLong(COLUMN_MSG_ID);
                        msgIds[index][1] = cursor.getLong(COLUMN_SIM_ID);
                        break;
                    case Sms.MESSAGE_TYPE_OUTBOX:
                    case Sms.MESSAGE_TYPE_QUEUED:
                        status = status + mResourceContext.getString(R.string.msg_status_sending);
                        break;
                    default:
                        break;
                }
                ///@}
                datas[dataIndex] =  addressStr + "\n" + status;
                index++;
                dataIndex++;
            }
            ///M: set MessageListDetails head @{
            String typeLabel = mResourceContext.getString(R.string.message_type_label);
            String headStr = typeLabel + mResourceContext.getString(R.string.text_message);
            String dateLabel = mResourceContext.getString(R.string.sent_label);
            if (cursor.moveToFirst()) {
                headStr = headStr
                    + "\n"
                    + (dateLabel + MessageUtils.formatDateOrTimeStampStringWithSystemSetting(
                        context, cursor.getLong(COLUMN_DATE), true));
            }
            datas[0] = headStr;
            /// @}
            final long[][] msgIDS = msgIds;
            DialogInterface.OnClickListener clickListner = new DialogInterface.OnClickListener() {
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    if (msgIDS != null && msgIDS.length > 0) {
                        for (int index = 0; index < msgIDS.length; index++) {
                            Log.d(TAG, "msgIDS:" + msgIDS[index][0]);
                            if (msgIDS[index][0] > 0) {
                                Uri resendUri = ContentUris.withAppendedId(Sms.CONTENT_URI,
                                    msgIDS[index][0]);
                                Log.d(TAG, "Resend SMS Uri:" + resendUri + " SMs MsgID:"
                                    + msgIDS[index][1]);
                                asyncResendMessage(context, resendUri, (int) msgIDS[index][1]);
                            }
                        }
                    }
                    dialog.dismiss();
                }
            };
            String sendButtonStr = mResourceContext.getString(R.string.btn_resend_str);
            boolean hasFailed = false;
            if (msgIds == null || msgIds.length < 1) {
                hasFailed = false;
            } else {
                for (int msgIdIndex = 0; msgIdIndex < msgIds.length; msgIdIndex++) {
                    if (msgIds[msgIdIndex][0] > 0) {
                        hasFailed = true;
                        break;
                    }
                }
            }
            if (hasFailed) {
                showMassTextMsgDetails(datas, clickListner, sendButtonStr, hasFailed);
            } else {
                showMassTextMsgDetails(datas, null, sendButtonStr, hasFailed);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "query mass text message's details failed.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    /**
     * M: Show detail for the mass text message.
     * @param items list's items.
     * @param clickListener the click listener.
     * @param btnStr the button name.
     * @param showButton true: show the button for resend. false: not show button.
     */
    private void showMassTextMsgDetails(String[] items, OnClickListener clickListener,
                                                String btnStr, boolean showButton) {
        AlertDialog.Builder detailDialog = new AlertDialog.Builder(mComposeActivity);
        detailDialog.setTitle(mResourceContext.getString(R.string.message_details_title));
        detailDialog.setCancelable(true);
        if (clickListener != null && showButton) {
            detailDialog.setPositiveButton(btnStr, clickListener);
        }
        detailDialog.setItems(items, null);
        detailDialog.show();
    }

    private String getNumberLocation(Context context, String number) {
        String location = PhoneNumberUtils.getNumberLocation(context, number);
        if (location == null || location.trim().equals("")) {
            return number;
        }
        return number + " (" + location + ")";
    }

    private void showDisableDRDialog(final Activity activity, final int subId) {
        Log.d(TAG, "showDisableDRDialog() subId = " + subId);
        final Context context = activity.getApplicationContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sp.getBoolean(PREF_KEY_SHOW_DIALOG + "_" + subId, true)) {
            return;
        }

        final View contents = View.inflate(mResourceContext,
                                        R.layout.disable_delivery_report_dialog, null);
        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.dlg_never_shown);

        activity.runOnUiThread(new Runnable() {

            public void run() {
                new AlertDialog.Builder(activity).setIconAttribute(android.R.attr.alertDialogIcon)
                        .setView(contents).setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface arg0, int arg1) {
                                    if (checkbox.isChecked()) {
                                        enableDRWarningDialog(context, false, subId);
                                    }
                                }
                            }).create().show();
            }
        });
    }

    private void enableDRWarningDialog(Context context, boolean isEnable, int subId) {
        SharedPreferences.Editor editor
                = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PREF_KEY_SHOW_DIALOG + "_" + subId, isEnable).apply();
    }

    /**
     * M: For CT. split single massing text message to separater message.
     *
     * @param context
     *            the Context.
     * @param messageGroupId
     *            the id for sms mass text message.
     */
    private void splitSingleMessage(final Context context, final long messageGroupId) {
        if (messageGroupId >= 0) {
            Log.d(TAG, "splitSingleMessage failed: messageGroupId >= 0; messageGroupId = "
                + messageGroupId);
            return;
        }
        if (context == null) {
            Log.d(TAG, "splitSingleMessage failed: context == null;");
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                ///M: First: Get all messages which own the same messageGroupId(ipmessageId)
                Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI,
                        new String[] {Sms._ID, Sms.ADDRESS, Sms.THREAD_ID, Sms.IPMSG_ID},
                        Sms.IPMSG_ID + " = ? ", new String[] {messageGroupId + ""},
                    null);
                long threadId = 0;
                if (cursor != null) {
                    try {
                        int smsCount = cursor.getCount();
                        while (cursor.moveToNext()) {
                            ///M: Second: get or create thread Id
                            String address = cursor.getString(1);
                            long smsId = cursor.getLong(0);
                            threadId = cursor.getLong(2);
                            long newThreadId = Threads.getOrCreateThreadId(context, address);
                            ///M: Third: Update sms's threadId and messageGroupId
                            ContentValues cv = new ContentValues();
                            cv.put(Sms.THREAD_ID, newThreadId);
                            cv.put(Sms.IPMSG_ID, 0);
                            Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
                            context.getContentResolver().update(uri, cv, null, null);
                        }
                        closeActivity(context, threadId);
                    } finally {
                        cursor.close();
                    }
                }
            }
        }).start();
    }

    /**
     * For : Cloase activity if the activity has no message in compose.
     * @param context The Context.
     * @param threadId the thread id.
     */
    private void closeActivity(Context context, long threadId) {
        if (threadId < 1) {
            return;
        }
        Uri conUri = ContentUris.withAppendedId(MmsSms.CONTENT_CONVERSATIONS_URI, threadId);
        Cursor conCursor = context.getContentResolver().query(conUri,
            new String[] {BaseColumns._ID}, null, null, null);
        try {
            if (conCursor != null) {
                int msgCount = conCursor.getCount();
                Log.d(TAG, "closeActivity, msgCount: " + msgCount);
                if (msgCount < 1 && mComposeActivity != null) {
                    mComposeActivity.runOnUiThread(new Runnable() {

                        public void run() {
                            // TODO Auto-generated method stub
                            mComposeActivity.finish();
                        }
                    });
                }
            }
        } finally {
            if (conCursor != null) {
                conCursor.close();
            }
        }
    }

    private void splitThreadMassingTextMessage(final Context context, final long threadId) {
        if (threadId <= 0) {
            Log.d(TAG, "splitThreadMassingTextMessage failed: threadId <= 0; threadId = "
                + threadId);
            return;
        }
        if (context == null) {
            Log.d(TAG, "splitThreadMassingTextMessage failed: context == null;");
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                ///M: First: Get all messages which own the same messageGroupId(ipmessageId)
                Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI,
                        new String[] {Sms._ID, Sms.ADDRESS, Sms.THREAD_ID, Sms.IPMSG_ID},
                        Sms.THREAD_ID + " = ? ", new String[] {threadId + ""},
                        null);
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            ///M: Second: get or create thread Id
                            String address = cursor.getString(1);
                            long smsId = cursor.getLong(0);
                            long newThreadId = Threads.getOrCreateThreadId(context, address);
                            ///M: Third: Update sms's threadId and messageGroupId
                            ContentValues cv = new ContentValues();
                            cv.put(Sms.THREAD_ID, newThreadId);
                            cv.put(Sms.IPMSG_ID, 0);
                            Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, smsId);
                            context.getContentResolver().update(uri, cv, null, null);
                        }
                        closeActivity(context, threadId);
                    } finally {
                        cursor.close();
                    }
                }
            }
        }).start();
    }

    private void asyncResendMessage(final Context context, final Uri uri, final long subId) {
        Log.d("@M_" + TAG, "ResendSmsExt.resendMessage");

        new Thread(new Runnable() {
            public void run() {
                // Update the date.
                Long date = System.currentTimeMillis();
                ContentValues values = new ContentValues(1);
                values.put(Sms.DATE, date);
                context.getContentResolver().update(uri, values, null, null);

                // Move the failed message from failed box to queued box.
                boolean isMoved = Sms.moveMessageToFolder(context, uri, Sms.MESSAGE_TYPE_QUEUED, 0);
                Log.d("@M_" + TAG, "Move message to queued box: " + isMoved);

                // Send Broadcast and handled by the SmsReceiverService
                Intent sentIt = new Intent(RESEND_MESSAGE_ACTION);
                sentIt.putExtra("subId", subId);
                context.sendBroadcast(sentIt);
            }
        }).start();

    }

    private void hideSrcSendButton() {
        if (mOriginSmsSendButton != null) {
            mOriginSmsSendButton.setVisibility(View.GONE);
        }
        if (mOriginMmsSendButton != null) {
            mOriginMmsSendButton.setVisibility(View.GONE);
        }
    }

    public static Uri getConverationUriForMassingMessage(long threadId) {
        return Uri.parse("content://mms-sms/conversations_distinct/" + threadId);
    }

    @Override
    public boolean onInitialize(Intent intent, IOpWorkingMessageExt opWorkingMessageExt) {
        Log.i(TAG, "onInitialize");
        mWorkingMessage = (Op09WorkingMessageExt)opWorkingMessageExt;
        return true;
    }
}

