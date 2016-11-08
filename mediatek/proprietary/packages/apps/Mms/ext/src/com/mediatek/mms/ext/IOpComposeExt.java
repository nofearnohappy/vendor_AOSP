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

package com.mediatek.mms.ext;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.mms.pdu.PduBody;

import com.mediatek.mms.callback.IMessageItemCallback;
import com.mediatek.mms.callback.IMessageListAdapterCallback;
import com.mediatek.mms.callback.IRecipientsEditorCallback;
import com.mediatek.mms.callback.ISlideshowModelCallback;
import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.callback.IComposeActivityCallback;
import com.mediatek.mms.callback.IWorkingMessageCallback;

public interface IOpComposeExt {

    int MSG_LIST_SHOW_MSGITEM_DETAIL = 3600;
    int MENU_ADD_ATTACHMENT        = 2;

    String USING_COLON = "USE_COLON";
    String SELECTION_CONTACT_RESULT = "contactId";
    String NUMBERS_SEPARATOR_COLON = ":";
    String NUMBERS_SEPARATOR_SIMCOLON = ";";
    String NUMBERS_SEPARATOR_COMMA = ",";

    static final int MMS_SAVE_OTHER_ATTACHMENT = 0;
    static final int MMS_SAVE_ALL_ATTACHMENT = 1;


    /**
     * @internal
     */
    void showSubjectEditor(EditText subjectEditor);

    /**
     * @internal
     */
    boolean handleAttachmentEditorHandlerMessage(Message msg, boolean sendButtonCanResponse,
            IWorkingMessageCallback opWorkingMessageCallback, ISlideshowModelCallback slideshow,
            boolean compressingImage);

    /**
     * @internal
     */
    boolean attachVCalendar(boolean append, int type, Uri uri, int mediaTypeStringId);

    /**
     * @internal
     */
    boolean asyncAttachVCardByContactsId(boolean append);

    /**
     * @internal
     */
    void onCreate(IComposeActivityCallback ipComposeActivityCallback, Intent intent,
            IntentFilter intentFilter, Activity oldCompose, Activity compose,
            Bundle savedInstanceState, Handler uiHandler, ImageButton shareButton,
            LinearLayout panel, EditText textEditor);

    /**
     * @internal
     */
    boolean haveSomethingToCopyToSDCard(Context context, PduBody body);

    /**
     * @internal
     */
    boolean onMenuItemClick(MenuItem item, IOpMessageItemExt opMessageItemExt,
            Intent intentMultiSave, Intent deliveryIntent);

    /**
     * @internal
     */
    boolean processNewIntent(Intent intent);

    /**
     * @internal
     */
    boolean subSelectionDialog(int subId);

    /**
     * @internal
     */
    void editSlideshow(boolean hadToSlideShowEditor);

    /**
     * @internal
     */
    void onResume(int subCount, String text, boolean isSmsEnabled,
            View recipientsEditor, View subjectTextEditor);

    /**
     * @internal
     */
    boolean onNewIntent(final Intent intent, boolean hadToSlideShowEditor, Uri tempMmsUri,
            final IWorkingMessageCallback workingMessage, Object activity,
            IRecipientsEditorCallback recipientsEditor, final int box);

            /**
     * M: showMessageDetails called
     *
     * @param ipMessageId: ipMessageId
     * @return boolean
     * @internal
     */
    public boolean showMessageDetails(String timeStamp);

    /**
     * @internal
     */
    public boolean updateCounter(CharSequence text, TextView textCounter,
            IWorkingMessageCallback workingMessageCallback);

    /**
     * @internal
     */
    public boolean updateCounterUiRun(TextView textEditor, int remainingInCurrentMessage,
            int msgCount, IWorkingMessageCallback workingMessageCallback);

    /**
     * @internal
     */
    public boolean onDeleteMessageListenerClick(AsyncQueryHandler backQueryHandler, long msgId,
            long timeStamp);

    /**
     * @internal
     */
    public boolean confirmSendMessageIfNeeded(boolean isRecipientsEditorVisible,
            boolean hasInvalidRecipient);

    /**
     * @internal
     */
    public boolean onCreateContextMenu(boolean isSmsEnabled, boolean isSms, int subCount,
            boolean isSending, MenuItem.OnMenuItemClickListener l, ContextMenu menu, View v,
            ContextMenuInfo menuInfo, long messageGroupId, int messagesCount);

    /**
     * @internal
     */
    public boolean forwardMessage(Context context, IMessageItemCallback msgItem,
            String nameAndNumber, Intent intent);

    /**
     * @internal
     */
    public boolean dispatchTouchEvent(MotionEvent ev);

    /**
     * @internal
     */
    public void onStart(ITextSizeAdjustHost host, Activity activity);

    /**
     * @internal
     */
    void editMmsMessageItem(IOpWorkingMessageExt opWorkingMessageExt);

    /**
     * @internal
     */
    boolean lockMessage(Context context, Uri uri, long msgId, long timeStamp, boolean lock);

    /**
     * @internal
     */
    String updateTitle(int cnt, String number, String subTitle);

    /**
     * @internal
     */
    void initRecipientsEditor(Bundle bundle, View recipientsCcEditor);

    /**
     * @internal
     */
    void onSaveInstanceState(Bundle outState);

    /**
     * @internal
     */
    boolean onProtocolChanged(boolean isMms, CharSequence text);

    /**
     * @internal
     */
    boolean onPrepareOptionsMenu(Menu menu, boolean isSmsEnabled,
            boolean isRecipientsEditorVisible, int contactsSize, long threadId);

    /**
     * @internal
     */
    boolean onActivityResult(int requestCode, Intent data,
            IOpWorkingMessageExt newOpWorkingMessageExt, IOpWorkingMessageExt opWorkingMessageExt);

    /**
     * @internal
     */
    void drawTopPanel(boolean isRecipientsEditorVisible, IOpWorkingMessageExt opWorkingMessageExt);

    /**
     * @internal
     */
    void drawBottomPanel(boolean hasSlideshow, boolean isUpdateAttachEditor,
            boolean isSmsEnabled);

    /**
     * @internal
     */
    boolean onClick(View v, ImageButton recipientsPicker);

    /**
     * @internal
     */
    void initResourceRefs(LinearLayout buttonWithCounter, TextView textCounter,
                                IOpAttachmentEditorExt attachmentEditor);

    /**
     * @internal
     */
    Uri startMsgListQuery(Uri conversationUri, long threadId);

    /**
     * @internal
     */
    public void saveDraft(IOpWorkingMessageExt opWorkingMessageExt);

    /**
     * @internal
     */
    public boolean sendMessage(IOpWorkingMessageExt opWorkingMessageExt, int subId);

    /**
     * @internal
     */
    public void resetMessage();

    /**
     * @internal
     */
    public boolean updateSendButtonState(boolean requiresMms, int recipientCount,
            int smsRecipientLimit, int subCount);

    /**
     * @internal
     */
    public boolean initFocus();

    /**
     * @internal
     */
    public boolean handleUiMessage(Message msg, int subCount, int selectedSubId);

    /**
     * @internal
     */
    public String showSubSelectedDialog(String src);

    /**
     * @internal
     */
    public boolean addContacts(Intent intent, int pickCount, int requestCode);

    /**
     * @internal
     */
    public String checkRecipientsCount(String message, boolean requiresMms,
            int recipientCount, int mmsLimitCount, boolean isRecipientsEditorEmpty,
            boolean isConversationRecipientEmpty, Intent intent,
            IMessageListAdapterCallback msgListAdapter, List<SubscriptionInfo> subInfoList,
            boolean isRecipientsEditorVisible);

    /**
     * @internal
     */
    public void onSubInforChanged();

    /**
     * @internal
     */
    public boolean onReceive(String action, Runnable getSubInfoRunnable);

    /**
     * @internal
     */
    public void getSubInfoRunnable(boolean hasSlideshow);

    /**
     * @internal
     */
    public int getMessageAndSaveToSub(String[] numbers, String scAddress,
            ArrayList<String> messages, int smsStatus, long timeStamp, int subId, int srcResult);

    /**
     * @internal
     */
    public void addCallAndContactMenuItems(Context context, ContextMenu menu, int menuId,
            ArrayList<String> urls, IMessageItemCallback msgItem);

    /**
     * @internal
     */
    public void setConfirmMmsToSms(boolean needConfirm);

    /**
     * @internal
     */
    public boolean onOptionsItemSelected(MenuItem item, IOpWorkingMessageExt workingMessageExt,
            InputMethodManager inputMethodManager);

    /**
     * @internal
     */
    public Uri onQueryMsgListRun(Uri uriSrc, long threadId);

    /**
     * @internal
     */
    public int getSmsEncodingType(int encodingType, Context context);

    /**
     * @internal
     */
    public boolean checkConditionsAndSendMessage(boolean isMms, final boolean bcheckMode);

    /**
     * @internal
     */
    public void onDiscardDraftListenerClick();

    /**
     * @internal
     */
    public boolean hideOrShowTopPanel(View topPanel);

    /**
     * @internal
     */
    public void initActivityState(Intent intent);

    /**
     * @internal
     */
    public int subSelection();

    /**
     * @internal
     */
    public boolean onMessageListItemHandler(Message msg, String timestamp);

    /**
     * @internal
     */
    public void chipOnCreateContextMenu(ContextMenu menu);
    /**
     * @internal
     */
    public boolean updateSendButton(boolean enabled, int subCount, boolean requiresMms,
                        boolean hasSlideshow);

    /**
     * @internal
     */
    public void onRecipientsEditorFocusChange(boolean hasFocus);
    /**
     * @internal
     */
    public void onRecipientsEditorTouch();
    /**
     * @internal
     */
    public void onSubjectTextEditorTouch();
    /**
     * @internal
     */
    public void onSubjectTextEditorFocusChange(boolean hasFocus);
    /**
     * @internal
     */
    public void onConfigurationChanged();

    /**
     * @internal
     */
    public void resetConfiguration(boolean isLandscapeOld,
            boolean isLandscapeNew, boolean isSoftKeyBoardShow);
    /**
     * @internal
     */
    public boolean onKeyDown(int keyCode, KeyEvent event);
    /**
     * @internal
     */
    public void onTextEditorTouch();
    /**
     * @internal
     */
    public boolean onLayoutSizeChanged(boolean isSoftKeyBoardShow);
    /**
     * @internal
     */
    public boolean updateFullScreenTextEditorHeight();
    public boolean onInitialize(Intent intent, IOpWorkingMessageExt opWorkingMessageExt);
}

