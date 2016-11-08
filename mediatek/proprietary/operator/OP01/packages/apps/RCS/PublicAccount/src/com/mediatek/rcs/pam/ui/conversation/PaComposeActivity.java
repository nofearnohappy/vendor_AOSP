/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.pam.ui.conversation;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.gsma.joyn.JoynServiceListener;

import com.cmcc.ccs.profile.ProfileListener;
import com.cmcc.ccs.profile.ProfileService;
import com.google.android.mms.ContentType;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MenuInfo;
import com.mediatek.rcs.pam.model.MenuEntry;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.ui.AccountDetailsActivity;
import com.mediatek.rcs.pam.ui.AsyncDialog;
import com.mediatek.rcs.pam.ui.PaWebViewActivity;
import com.mediatek.rcs.pam.ui.PaWorkingMessage;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;
import com.mediatek.rcs.pam.ui.messageitem.PAAudioService;
import com.mediatek.rcs.pam.util.ContextCacher;
import com.mediatek.rcs.pam.util.EmojiImpl;
import com.mediatek.rcs.pam.util.GeoLocService;
import com.mediatek.rcs.pam.util.RCSEmoji;
import com.mediatek.rcs.pam.util.ScaleDetector;
import com.mediatek.rcs.pam.util.Utils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main UI for: 1. Composing a new message; 2. Viewing/managing
 * message history of a conversation.
 *
 * This activity can handle following parameters from the intent by which it's
 * launched. thread_id long Identify the conversation to be viewed. When
 * creating a new message, this parameter shouldn't be present. msg_uri Uri The
 * message which should be opened for editing in the editor. address String The
 * addresses of the recipients in current conversation. exit_on_sent boolean
 * Exit this activity after the message is sent.
 */
public class PaComposeActivity extends Activity implements
        View.OnClickListener, TextView.OnEditorActionListener,
        ScaleDetector.OnScaleListener {

    private static final String TAG = "PA/PaComposeActivity";

    private int mSendErrorCode;

    public static final int ERR_SRV_NOT_READY = -20;

    // Menu ID
    private static final int MENU_PA_DETAILS = 1;
    private static final int MENU_PA_DELETE_ALL = 2;
    // Context menu ID
    private static final int MENU_PA_DELETE_MESSAGE = 52;
    private static final int MENU_PA_FORWARD_MESSAGE = 53;
    private static final int MENU_PA_COPY_MESSAGE_TEXT = 54;
    private static final int MENU_PA_REPORT_MESSAGE = 55;
    private static final int MENU_PA_SELECT_TEXT = 56;
    private static final int MENU_PA_EXPORD_SDCARD = 57;
    private static final int MENU_PA_SELECT_MESSAGE = 58;
    private static final int MENU_PA_ADD_FAVORITE = 59;

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN = 9528;
    private static final int ACCOUNT_DETAILS_QUERY_TOKEN = 9529;
    private static final int ACCOUNT_DETAILS_QUERY_TOKEN_REFRESH = 9530;

    private static final int DELETE_MESSAGE_TOKEN = 9700;

    // private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 10;

    private static final int REQUEST_CODE_PAMSG_TAKE_PHOTO = 300;
    private static final int REQUEST_CODE_PAMSG_RECORD_VIDEO = 301;
    private static final int REQUEST_CODE_PAMSG_SHARE_CONTACT = 303;
    private static final int REQUEST_CODE_PAMSG_CHOOSE_PHOTO = 304;
    private static final int REQUEST_CODE_PAMSG_CHOOSE_VIDEO = 305;
    private static final int REQUEST_CODE_PAMSG_RECORD_AUDIO = 306;
    private static final int REQUEST_CODE_PAMSG_CHOOSE_AUDIO = 308;

    private static final int REQUEST_CODE_FOR_MULTIDELETE = 320;

    static final float DEFAULT_TEXT_SIZE = 18.0f;
    private static final int MIN_TEXT_SIZE = 10;
    private static final int MAX_TEXT_SIZE = 32;
    private static final float MIN_ADJUST_TEXT_SIZE = 0.2f;

    // private static final String KEY_EXIT_ON_SENT = "exit_on_sent";
    private static final String KEY_FORWARDED_MESSAGE = "forwarded_message";
    private static final String KEY_FORWARDED_IPMESSAGE = "forwarded_ip_message";

    // When the conversation has a lot of messages and a new message is sent,
    // the list is scrolled
    // so the user sees the just sent message. If we have to scroll the list
    // more than 20 items,
    // then a scroll shortcut is invoked to move the list near the end before
    // scrolling.
    private static final int MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT = 20;

    // Any change in height in the message list view greater than this threshold
    // will not
    // cause a smooth scroll. Instead, we jump the list directly to the desired
    // position.
    private static final int SMOOTH_SCROLL_THRESHOLD = 200;

    // To reduce janky interaction when message history + draft loads and
    // keyboard opening
    // query the messages + draft after the keyboard opens. This controls that
    // behavior.
    // private static final boolean DEFER_LOADING_MESSAGES_AND_DRAFT = true;

    // The max amount of delay before we force load messages and draft.
    // 500ms is determined empirically. We want keyboard to have a chance to be
    // shown before
    // we force loading. However, there is at least one use case where the
    // keyboard never shows
    // even if we tell it to (turning off and on the screen). So we need to
    // force load the
    // messages+draft after the max delay.
    // private static final int LOADING_MESSAGES_AND_DRAFT_MAX_DELAY_MS = 500;

    private ContentResolver mContentResolver;

    private BackgroundQueryHandler mBackgroundQueryHandler;

    private long mAccountId; // Conversation we are working in
    private String mUuid;
    private String mName;
    private String mPath;
    private long mSelectId;
    public static Bitmap sLogoBitmap = null;

    public static Object sLockObj = new Object();

    // When mSendDiscreetMode is true, this activity only allows a user to type
    // in and send
    // a single sms, send the message, and then exits. The message history and
    // menus are hidden.
    // private boolean mSendDiscreetMode;
    // private boolean mForwardMessageMode;

    private RCSEmoji mEmoji;
    private static Location sLocation = null;
    private GeoLocService mGeoLocSrv = null;

    Uri mAttamentUri;

    private View mBottomPanel; // View containing the text editor, send button,
                               // ec.
    private EditText mTextEditor; // Text editor to type your message into
    private TextView mTextCounter; // Shows the number of characters used in
                                   // text editor
    private ImageButton mSendButton; // Press to send mms

    private PaMessageListView mMsgListView; // ListView for messages in this
                                            // conversation
    public PaMessageListAdapter mMsgListAdapter; // and its corresponding
                                                 // ListAdapter

    // For HW keyboard, 'mIsKeyboardOpen' indicates if the HW keyboard is open.
    // For SW keyboard, 'mIsKeyboardOpen' should always be true.
    private boolean mIsKeyboardOpen;
    private boolean mIsLandscape; // Whether we're in landscape mode

    private boolean mToastForDraftSave; // Whether to notify the user that a
                                        // draft is being saved

    private PaWorkingMessage mWorkingMessage; // The message currently being
                                              // composed.

    private boolean mWaitingForSubActivity;

    // private boolean mSendingMessage; // Indicates the current message is
    // sending, and shouldn't send again.

    private AsyncDialog mAsyncDialog; // Used for background tasks.

    private int mLastSmoothScrollPosition;
    private boolean mScrollOnSend; // Flag that we need to scroll the list to
                                   // the end.
    private int mSavedScrollPosition = -1; // we save the ListView's scroll
                                           // position in onPause(),
                                           // so we can remember it after
                                           // re-entering the activity.
                                           // If the value >= 0, then we jump to
                                           // that line. If the
                                           // value is maxint, then we jump to
                                           // the end.
    private long mLastMessageId;

    private ImageView mWallPaper;

    private ScaleDetector mScaleDetector;

    private float mTextSize;

    private String mBeforeTextChangeString = "";

    private boolean mIsStartMultiDeleteActivity = false;

    /**
     * Whether this activity is currently running (i.e. not paused)
     */
    // private boolean mIsRunning;

    // we may call loadMessageAndDraft() from a few different places. This is
    // used to make
    // sure we only load message+draft once.
    // private boolean mMessagesAndDraftLoaded;

    // whether we should load the draft. For example, after attaching a photo
    // and coming back
    // in onActivityResult(), we should not load the draft because that will
    // mess up the draft
    // state of mWorkingMessage. Also, if we are handling a Send or Forward
    // Message Intent,
    // we should not load the draft.
    private boolean mShouldLoadDraft;

    private boolean mSendOrgPic;

    public static final int ACTION_RESEND = 1;

    public static final String ACCOUNT_ID = "account_id";
    public static final String UUID = "uuid";
    public static final String NAME = "name";
    public static final String IMAGE_PATH = "image_path";
    public static final String SELECT_ID = "select_id";

    public static final int PASRV_STATE_INIT = 0;
    public static final int PASRV_STATE_CONNECTED = 1;
    public static final int PASRV_STATE_REGISTED = 2;

    private static final int ACCOUNT_STATUS_SUBSCRIBED = 0x00000001;
    private static final int ACCOUNT_STATUS_ACTIVITED = 0x00000002;

    private HeightChangedLinearLayout mHeightChangedLinearLayout;
    private static final int mReferencedTextEditorTowLinesHeight = 65;
    private static final int mReferencedTextEditorFourLinesHeight = 140;
    private static final int mReferencedTextEditorSevenLinesHeight = 224;
    private static final int mReferencedMaxHeight = 800;
    private int mCurrentMaxHeight = 800;

    private static final int DIALOG_ID_GEOLOC_PROGESS = 201;
    private static final int DIALOG_ID_GEOLOC_SEND_CONFIRM = 202;

    private boolean mShowKeyBoardFromShare = false;
    private boolean mIsSoftKeyBoardShow;
    private static final int SOFT_KEY_BOARD_MIN_HEIGHT = 150;
    private Object mWaitingImeChangedObject = new Object();

    private InputMethodManager mInputMethodManager = null;

    private PaSharePanel mSharePanel;
    private ImageButton mShareButton;

    private View mActionBarCustomView;
    private TextView mTopTitle;

    // -----------add by Public account
    private PAServiceCallback mPAServiceCallback;
    private PAService mPAService;
    private long mToken = Constants.INVALID;
    private int mPASrvState;
    private int mAccountStatus;

    private MenuInfo mMenuInfo = new MenuInfo();
    private boolean mIsMenuMode = false; // if is Menu shown currently.
    private int mMenuSize; // mMenuSize == 0 means no menu exists.
    private String mMenuTimeStamp;

    private LinearLayout mInputContainerLayout;
    private LinearLayout mMenuContainerLayout;
    private LinearLayout mInputSwitchLayout;
    private LinearLayout mMenuLayout;
    private ImageButton mSwitchToMenuBtn;
    private ImageButton mSwitchToInputBtn;
    private Button[] mMenuButtons;
    private LinearLayout[] mMenuOutters;

    PAAudioService mAudioServcie;

    ProfileService mProfileService;
    private static byte[] sPortrait = {};
    public static Bitmap sPortraitBitmap;

    AccountObserver mAccountObserver;

    // @TODO : Sync operation
    public static final String MSG_BUNDLE_URL = "url";
    public static final String MSG_BUNDLE_TYPE = "type";

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PaSharePanel.ACTION_SHARE:
                doMoreAction(msg);
                break;

            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    // ==========================================================
    // Inner classes
    // ==========================================================

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case ACTION_RESEND:
                if (null != msg && null != msg.obj) {
                    if (isPASrvReady() && isAccountValid()) {
                        MessageData messageData = (MessageData) msg.obj;
                        showRetryDialog(messageData.getMessageContent().id,
                                messageData.getMessageContent().mediaType);
                    } else if (!isPASrvReady()) {
                        Toast.makeText(PaComposeActivity.this,
                                R.string.service_not_ready, Toast.LENGTH_LONG)
                                .show();
                    } else if (!isAccountSubscribed()) {
                        Toast.makeText(PaComposeActivity.this,
                                R.string.account_not_subscribed,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PaComposeActivity.this,
                                R.string.account_stopped, Toast.LENGTH_LONG)
                                .show();
                    }
                }
                break;

            default:
                break;
            }
        }
    };

    public static boolean isMultimediaMessage(int type) {
        if (Constants.MEDIA_TYPE_AUDIO == type
                || Constants.MEDIA_TYPE_VIDEO == type
                || Constants.MEDIA_TYPE_PICTURE == type
                || Constants.MEDIA_TYPE_GEOLOC == type
                || Constants.MEDIA_TYPE_VCARD == type) {
            return true;
        }

        return false;
    }

    private void showRetryDialog(final long msgId, final int type) {
        Log.d(TAG, "showRetryDialog():" + msgId);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.retry_indicator)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Log.d(TAG, "resendMessage():" + msgId);
                                mPAService.resendMessage(mToken, msgId);

                                if (isMultimediaMessage(type)) {
                                    Log.d(TAG,
                                            "update mSendingQueue when retry");
                                    updateSendingProgress(msgId, 0);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private boolean isCursorValid() {
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            Log.e(TAG, "Bad cursor.", new RuntimeException());
            return false;
        }
        return true;
    }

    private void updateCounter(CharSequence text, int start, int before,
            int count) {
        Log.d(TAG, "updateCounter.(" + start + "," + before + "," + count
                + ") text=" + text);

        if (text == null) {
            return;
        }

        if (text.length() == 0) {
            mTextCounter.setVisibility(View.GONE);
            // mWorkingMessage.setLengthRequiresMms(false, true);
            return;
        }

        final int length = text.length();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mTextEditor.getLineCount() > 1) {
                    mTextCounter.setVisibility(View.VISIBLE);
                    mTextCounter.setText(length + "/" + 3000);
                } else {
                    mTextCounter.setVisibility(View.GONE);
                    mTextCounter.setText(length + "/" + 3000);
                }

            }
        }, 100);

        /*
         * if (!TextUtils.isEmpty(text)) { EmojiImpl emojiImpl =
         * EmojiImpl.getInstance(this); if (mTextEditor != null) { CharSequence
         * str = emojiImpl.getEmojiExpression(text, true);
         * mTextEditor.removeTextChangedListener(mTextEditorWatcher);
         * mTextEditor.setTextKeepState(str);
         * mTextEditor.addTextChangedListener(mTextEditorWatcher); } }
         */
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // requestCode >= 0 means the activity in question is a sub-activity.
        if (requestCode >= 0) {
            mWaitingForSubActivity = true;
        }
        // The camera and other activities take a long time to hide the keyboard
        // so we pre-hide
        // it here. However, if we're opening up the quick contact window while
        // typing, don't
        // mess with the keyboard.
        if (mIsKeyboardOpen) {
            hideKeyboard();
        }

        super.startActivityForResult(intent, requestCode);
    }

    private class DeleteMessageListener implements OnClickListener {
        private final MessageData mMessageData;

        public DeleteMessageListener(MessageData messageData) {
            mMessageData = messageData;
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();

            mPAService.deleteMessage(mToken,
                    mMessageData.getMessageContent().id);
        }
    }

    private class DeleteAllMessageListener implements OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
            mPAService.deleteMessageByAccount(mToken, mAccountId);
        }
    }

    private void confirmSendMessageIfNeeded() {
        checkConditionsAndSendMessage();
    }

    private void addPositionBasedMenuItems(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo");
            return;
        }
        final int position = info.position;

        addUriSpecificMenuItems(menu, v, position);
    }

    private Uri getSelectedUriFromMessageList(ListView listView, int position) {

        // If the context menu was opened over a uri, get that uri.
        PaMessageListItem msglistItem = (PaMessageListItem) listView
                .getChildAt(position);
        if (msglistItem == null) {
            // FIXME: Should get the correct view. No such interface in ListView
            // currently
            // to get the view by position. The ListView.getChildAt(position)
            // cannot
            // get correct view since the list doesn't create one child for each
            // item.
            // And if setSelection(position) then getSelectedView(),
            // cannot get corrent view when in touch mode.
            Log.d(TAG,
                    "getSelectedUriFromMessageList. msgListItem is null !!! ");
            return null;
        }

        TextView textView;
        CharSequence text = null;
        int selStart = -1;
        int selEnd = -1;

        // check if message sender is selected
        textView = (TextView) msglistItem.findViewById(R.id.text_view);
        if (textView != null) {
            text = textView.getText();
            selStart = textView.getSelectionStart();
            selEnd = textView.getSelectionEnd();
        }

        // Check that some text is actually selected, rather than the cursor
        // just being placed within the TextView.
        if (selStart != selEnd) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            URLSpan[] urls = ((android.text.Spanned) text).getSpans(min, max,
                    URLSpan.class);

            if (urls.length == 1) {
                return Uri.parse(urls[0].getURL());
            }
        }

        // no uri was selected
        return null;
    }

    private void addUriSpecificMenuItems(ContextMenu menu, View v, int position) {
        Uri uri = getSelectedUriFromMessageList((ListView) v, position);

        if (uri != null) {
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
            menu.addIntentOptions(0, 0, 0, new android.content.ComponentName(
                    this, PaComposeActivity.class), null, intent, 0, null);
        }
    }

    private final OnCreateContextMenuListener mMsgListMenuCreateListener =
            new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {

            if (!isCursorValid()) {
                return;
            }
            Cursor cursor = mMsgListAdapter.getCursor();
            int type = cursor.getInt(cursor
                    .getColumnIndexOrThrow(MessageColumns.TYPE));
            long msgId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageColumns.ID));
            int pos = cursor.getPosition();

            Log.d(TAG, "OnCreateContextMenuListener(): id=" + msgId + ", pos="
                    + pos);

            addPositionBasedMenuItems(menu, v, menuInfo);

            MessageData msgItem = mMsgListAdapter.getCachedMessageData(msgId,
                    cursor);
            if (!hasContextMenu(msgItem)) {
                Log.e(TAG, "No option menu: " + type + ", msgId = " + msgId);
                return;
            }

            menu.setHeaderTitle(R.string.message_options);

            configContextMenu(msgItem, menu, menuInfo);
        }
    };

    private boolean hasContextMenu(MessageData msgItem) {
        if (msgItem == null) {
            return false;
        }
        return true;
    }

    private void configContextMenu(MessageData msgData, ContextMenu menu,
            ContextMenuInfo menuInfo) {
        MsgListMenuClickListener l = new MsgListMenuClickListener(msgData);

        menu.add(0, MENU_PA_DELETE_MESSAGE, 0, R.string.delete_message)
                .setOnMenuItemClickListener(l);

        // if (msgItem.mDirection == Constants.MESSAGE_DIRECTION_INCOMING) {
        // menu.add(0, MENU_PA_REPORT_MESSAGE, 0, R.string.menu_report).
        // setOnMenuItemClickListener(l);
        // }

        MessageContent msgItem = msgData.getMessageContent();
        if (Constants.MEDIA_TYPE_TEXT == msgItem.mediaType) {
            menu.add(0, MENU_PA_COPY_MESSAGE_TEXT, 0, R.string.menu_copy)
                    .setOnMenuItemClickListener(l);
            menu.add(0, MENU_PA_SELECT_TEXT, 0, R.string.select_text)
                    .setOnMenuItemClickListener(l);
        }

        switch (msgItem.mediaType) {
        case Constants.MEDIA_TYPE_PICTURE:
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_AUDIO:
            if (msgItem.basicMedia != null
                    && msgItem.basicMedia.originalPath != null
                    && !msgItem.basicMedia.originalPath.isEmpty()) {
                menu.add(0, MENU_PA_EXPORD_SDCARD, 0, R.string.copy_to_sdcard)
                        .setOnMenuItemClickListener(l);
            }
            break;
        case Constants.MEDIA_TYPE_VCARD:
        case Constants.MEDIA_TYPE_GEOLOC:
            if (TextUtils.isEmpty(msgItem.mediaPath)) {
                menu.add(0, MENU_PA_EXPORD_SDCARD, 0, R.string.copy_to_sdcard)
                        .setOnMenuItemClickListener(l);
            }
            break;
        default:
            break;
        }

        switch (msgItem.mediaType) {
        case Constants.MEDIA_TYPE_TEXT:
        case Constants.MEDIA_TYPE_VCARD:
        case Constants.MEDIA_TYPE_GEOLOC:
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            if (Constants.MESSAGE_FORWARDABLE_YES == msgItem.forwardable) {
                menu.add(0, MENU_PA_FORWARD_MESSAGE, 0, R.string.menu_forward)
                        .setOnMenuItemClickListener(l);
            }
            menu.add(0, MENU_PA_ADD_FAVORITE, 0, R.string.action_favorite)
                    .setOnMenuItemClickListener(l);
            break;

        case Constants.MEDIA_TYPE_AUDIO:
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_PICTURE:
            if (null != msgItem.basicMedia
                    && null != msgItem.basicMedia.originalPath) {
                if (Constants.MESSAGE_FORWARDABLE_YES == msgItem.forwardable) {
                    menu.add(0, MENU_PA_FORWARD_MESSAGE, 0,
                            R.string.menu_forward)
                            .setOnMenuItemClickListener(l);
                }
                menu.add(0, MENU_PA_ADD_FAVORITE, 0, R.string.action_favorite)
                        .setOnMenuItemClickListener(l);
            }
            break;

        default:
            break;
        }

    }

    public void toastForDebug(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    private void copyToClipboard(String str) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, str));
    }

    private void forwardMessage(final MessageData msgData) {
        Intent intent = new Intent();
        // intent.setClass(this,
        // "com.mediatek.rcs.message.ui.forwardActivity".getClass());
        // intent.setClassName("com.mediatek.rcs.message",
        // "com.mediatek.rcs.message.ui.ForwardActivity");
        intent.putExtra(KEY_FORWARDED_MESSAGE, true);
        intent.putExtra(KEY_FORWARDED_IPMESSAGE, true);

        intent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        MessageContent msgItem = msgData.getMessageContent();
        switch (msgItem.mediaType) {
        case Constants.MEDIA_TYPE_TEXT:
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msgItem.text);
            Log.d(TAG, "forward text Message:" + msgItem.text);
            break;
        case Constants.MEDIA_TYPE_VCARD:
            intent.setType("text/x-vcard");
            intent.putExtra(Intent.EXTRA_STREAM, msgItem.mediaPath);
            Log.d(TAG, "forward vCard Message:" + msgItem.mediaPath);
            break;

        case Constants.MEDIA_TYPE_AUDIO:
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_STREAM,
                    msgItem.basicMedia.originalPath);
            Log.d(TAG, "forward audio Message:"
                    + msgItem.basicMedia.originalPath);
            break;

        case Constants.MEDIA_TYPE_GEOLOC:
            intent.setType("geo/*");
            intent.putExtra(Intent.EXTRA_STREAM, msgItem.mediaPath);
            Log.d(TAG, "forward geoloc Message:" + msgItem.mediaPath);
            break;

        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            intent.setType("text/plain");
            String body = msgItem.article.get(0).mainText
                    + msgItem.article.get(0).bodyUrl;
            intent.putExtra(Intent.EXTRA_TEXT, body);
            Log.d(TAG, "forward single article Message:" + body);
            break;

        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            intent.setType("text/plain");
            String text = msgItem.article.get(msgData.mContextMenuIndex).mainText
                    + msgItem.article.get(msgData.mContextMenuIndex).bodyUrl;
            intent.putExtra(Intent.EXTRA_TEXT, text);
            Log.d(TAG, "forward multiple article Message:" + text);
            break;

        case Constants.MEDIA_TYPE_VIDEO:
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM,
                    msgItem.basicMedia.originalPath);
            Log.d(TAG, "forward video message:"
                    + msgItem.basicMedia.originalPath);
            break;

        case Constants.MEDIA_TYPE_PICTURE:
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM,
                    msgItem.basicMedia.originalPath);
            Log.d(TAG, "forward picture message:"
                    + msgItem.basicMedia.originalPath);
            break;

        default:
            return;
        }

        startActivity(intent);
        return;
    }

    /**
     * Context menu handlers for the message list view.
     */
    private final class MsgListMenuClickListener implements
            MenuItem.OnMenuItemClickListener {
        private MessageData mMsgItem;

        public MsgListMenuClickListener(MessageData msgItem) {
            mMsgItem = msgItem;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Log.d(TAG, "onMenuItemClick(). item=" + item);
            if (mMsgItem == null) {
                return false;
            }

            MessageContent messageContent = mMsgItem.getMessageContent();
            switch (item.getItemId()) {
            case MENU_PA_DELETE_MESSAGE:
                DeleteMessageListener l = new DeleteMessageListener(mMsgItem);
                confirmDeleteDialog(l, false);
                return true;
            case MENU_PA_FORWARD_MESSAGE:
                forwardMessage(mMsgItem);
                return true;
            case MENU_PA_COPY_MESSAGE_TEXT:
                if (messageContent.mediaType == Constants.MEDIA_TYPE_TEXT) {
                    copyToClipboard(messageContent.text);
                }
                return true;
            case MENU_PA_REPORT_MESSAGE:
                showReportConfirmDialog(messageContent.id);
                return true;

            case MENU_PA_SELECT_TEXT:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                        .getMenuInfo();
                Log.d(TAG, "onMenuItemClick(): info.position=" + info.position);
                mMsgListAdapter.getItemId(info.position);
                PaMessageListItem msgListItem = (PaMessageListItem) info.targetView;
                if (msgListItem != null) {
                    selectText(msgListItem);
                } else {
                    Log.d(TAG, "Select text but item is null");
                }
                return true;

            case MENU_PA_EXPORD_SDCARD:
                Log.d(TAG, "Save message. msgId = " + messageContent.id);
                copyFile(messageContent);
                return true;

            case MENU_PA_ADD_FAVORITE:
                Log.d(TAG, "Save as favorite. msgId = " + messageContent.id
                        + " with index = " + mMsgItem.mContextMenuIndex);
                mPAService.setFavourite(mToken, messageContent.id,
                        mMsgItem.mContextMenuIndex);
                return true;

            default:
                return false;
            }
        }
    }

    private void selectText(PaMessageListItem msgListItem) {
        Log.d(TAG, "selectText()");
        TextView textView = (TextView) msgListItem.findViewById(R.id.text_view);
        AlertDialog.Builder builder = new AlertDialog.Builder(
                PaComposeActivity.this);
        LayoutInflater factory = LayoutInflater.from(builder.getContext());
        final View textEntryView = factory.inflate(
                R.layout.alert_dialog_text_entry, null);
        EditText contentSelector = (EditText) textEntryView
                .findViewById(R.id.content_selector);
        contentSelector.setText(textView.getText());
        builder.setTitle(R.string.select_text).setView(textEntryView)
                .setPositiveButton(R.string.yes, null).show();

    }

    private void copyFile(MessageContent msgItem) {
        Log.d(TAG, "copyFile type = " + msgItem.mediaType);

        String source = msgItem.basicMedia.originalPath;
        if (!Utils.isExistsFile(source)) {
            Toast.makeText(this, R.string.copy_to_sdcard_fail,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File inFile = new File(source);
        String attName = source.substring(source.lastIndexOf("/") + 1);
        File dstFile = Utils.getStorageFile(attName);
        if (dstFile == null) {
            Log.d(TAG,
                    "saveMsgInSDCard(): save attachment failed, dstFile not exist!");
            return;
        }

        try {
            Utils.copyFile(inFile, dstFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.copy_to_sdcard_fail,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.copy_to_sdcard_success,
                Toast.LENGTH_SHORT).show();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(dstFile)));
    }

    private void showReportConfirmDialog(final long msgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setMessage("Confirm report this message ?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPAService.complainSpamMessage(mToken, msgId);
                    }
                }).show();
    }

    private void updateTitle(String title) {

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        if (actionBar.getCustomView() == null) {
            actionBar.setCustomView(R.layout.actionbar_pamessage_title);
        }
        mActionBarCustomView = actionBar.getCustomView();
        mTopTitle = (TextView) mActionBarCustomView
                .findViewById(R.id.tv_top_title);
        mTopTitle.setMaxWidth(3000);
        mTopTitle.setText(title);
        Log.d(TAG, "setTitle test:" + title);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    // ==========================================================
    // Activity methods
    // ==========================================================

    public static boolean cancelFailedToDeliverNotification(Intent intent,
            Context context) {
        return false;
    }

    public static boolean cancelFailedDownloadNotification(Intent intent,
            Context context) {

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // mIsSmsEnabled = MmsConfig.isSmsEnabled(this);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() enter");

        mScaleDetector = new ScaleDetector(this);

        resetConfiguration(getResources().getConfiguration());

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (mIsLandscape) {
            mCurrentMaxHeight = windowManager.getDefaultDisplay().getWidth();
        } else {
            mCurrentMaxHeight = windowManager.getDefaultDisplay().getHeight();
        }
        Log.d(TAG, "onCreate: mCurrentMaxHeight=" + mCurrentMaxHeight);
        setContentView(R.layout.pa_compose_activity);
        setProgressBarVisibility(false);

        // Initialize members for UI elements.
        initResourceRefs();
        initShareRessource();

        // @TODO temp code
        ContextCacher.setPluginContext(this);
        mEmoji = new RCSEmoji(this, (ViewParent) mBottomPanel, this);

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);

        mProfileService = new ProfileService(this, new ProfileServiceHandler());
        if (mProfileService != null) {
            Log.d(TAG, "Add profile listener");
            mProfileService.addProfileListener(new MyProfileListener());
        }

        initialize(savedInstanceState);

        initPAService();

        if (!SystemProperties.get("ro.mtk_lca_rom_optimize").equals("1")) {
            Log.d(TAG, "changeWallPaper()");
            if (!setWallPaper()) {
                mWallPaper.setImageResource(R.color.list_background);
            }
        }
    }

    public void initialize(Bundle savedInstanceState) {
        // Create a new empty working message.
        mWorkingMessage = PaWorkingMessage.createEmpty(this);

        // Read parameters or previously saved state of this activity. This will
        // load a new
        // mConversation
        initActivityState(savedInstanceState);

        getProfileFromDB(true);

        // Set up the message history ListAdapter
        initMessageList();
        loadMenuInfo();

        mShouldLoadDraft = true;
        loadDraft();

        // Let the working message know what conversation it belongs to
        mWorkingMessage.setAccountId(mAccountId);

        updateSendButtonState();

        drawTopPanel();
        if (!mShouldLoadDraft) {
            // We're not loading a draft, so we can draw the bottom panel
            // immediately.
            drawBottomPanel();
        }

        onKeyboardStateChanged();

        updateTitle(mName);

    }

    /*
     * onNewIntent -> onRestart -> onStart -> onResume
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: intent = " + intent.toString());
        processNewIntent(intent);
    }

    private void processNewIntent(Intent intent) {
        setIntent(intent);

        // mSentMessage = false;

        long accountId = intent.getLongExtra(ACCOUNT_ID, 0);
        if (accountId != mAccountId) {
            Log.e(TAG, "processNewIntent with different accountID !!!, quit");
            finish();
        }

        // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        // resetConfiguration(getResources().getConfiguration());
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // hideBottomPanel();

        if (mWorkingMessage.isDiscarded()) {
            // If the message isn't worth saving, don't resurrect it. Doing so
            // can lead to
            // a situation where a new incoming message gets the old thread id
            // of the discarded
            // draft. This activity can end up displaying the recipients of the
            // old message with
            // the contents of the new message. Recognize that dangerous
            // situation and bail out
            // to the ConversationList where the user can enter this in a clean
            // manner.
            if (mWorkingMessage.isWorthSaving()) {
                Log.d(TAG, "onRestart: mWorkingMessage.unDiscard()");
                mWorkingMessage.unDiscard(); // it was discarded in onStop().

            } else {
                mWorkingMessage.setAccountId(mAccountId);
                updateTextEditorHeightInFullScreen();
                invalidateOptionsMenu();
            }
        }
    }

    private float getSettingTextSize() {
        float textSize = DEFAULT_TEXT_SIZE;
        try {
            Context context = createPackageContext("com.android.mms",
                    Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sp = context.getSharedPreferences(
                    "com.android.mms_preferences", Context.MODE_WORLD_READABLE
                            | Context.MODE_MULTI_PROCESS);
            textSize = sp.getFloat("message_font_size", 18);
        } catch (NameNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        Log.d(TAG, "getSettingTextSize=" + textSize);
        return textSize;
    }

    private boolean getSettingSendOrgPic() {

        boolean org = false;
        try {
            Context context = createPackageContext("com.mediatek.rcs.message",
                    Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sp = context.getSharedPreferences(
                    "com.mediatek.rcs.message_preferences",
                    Context.MODE_MULTI_PROCESS);
            org = sp.getBoolean("pref_key_send_org_pic", false);
            Log.d(TAG, "getSettingSendOrgPic() is " + org);
        } catch (NameNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        Log.d(TAG, "getSettingSendOrgPic=" + org);
        return org;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() enter");

        mTextSize = getSettingTextSize();
        setTextSize(mTextSize);
        mScaleDetector.setActivity(this);

        mSendOrgPic = getSettingSendOrgPic();

        mAudioServcie = PAAudioService.getService();

        initFocus();

        int mode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        // @TODO : add no draft check
        if (mMenuSize == 0) {
            // For composing a new message, bring up the softkeyboard so the
            // user can
            // immediately enter recipients. This call won't do anything on
            // devices with
            // a hard keyboard.
            mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        } else {
            mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
        }
        getWindow().setSoftInputMode(mode);

        if (mMenuSize > 0) {
            loadMenuView();
        }

        updateBottomPanel(mIsMenuMode);

        loadMessageContent();

        ActionBar actionbar = getActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

    }

    public void loadMessageContent() {
        Log.d(TAG, "loadMessageContent() enter");
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 50);
        drawBottomPanel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState");
        outState.putLong(ACCOUNT_ID, mAccountId);
        outState.putString(UUID, mUuid);
        outState.putString(NAME, mName);
        outState.putString(IMAGE_PATH, mPath);
    }

    private boolean mIsRestore = false;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mIsRestore = true;
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mEmoji.setEmojiEditor(mTextEditor);

        updateSendButtonState();
        updateShareButtonEnabled();

        startQueryAccountDetails(ACCOUNT_DETAILS_QUERY_TOKEN);

        mAccountObserver = new AccountObserver(new Handler());
        getContentResolver().registerContentObserver(
                AccountColumns.CONTENT_URI, true, mAccountObserver);

        startQueryMyPortrait();

        Log.d(TAG, "onResume()");

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");

        if (mAccountObserver != null) {
            getContentResolver().unregisterContentObserver(mAccountObserver);
        }

        if (mProfileService != null) {
            mProfileService.disconnect();
        }

        // remove any callback to display a progress spinner
        if (mAsyncDialog != null) {
            mAsyncDialog.clearPendingProgressDialog();
        }

        mAudioServcie.stopAudio();

        if (mMsgListAdapter != null
                && mMsgListView.getLastVisiblePosition() > mMsgListAdapter
                        .getCount() - 1) {
            mSavedScrollPosition = Integer.MAX_VALUE;
        } else {
            mSavedScrollPosition = mMsgListView.getFirstVisiblePosition();
        }
        Log.d(TAG, "onPause: mSaveScrollPosition=" + mSavedScrollPosition);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);

        /*
         * if (mMsgListAdapter != null) { // Close the cursor in the ListAdapter
         * if the activity stopped. Cursor cursor = mMsgListAdapter.getCursor();
         *
         * if (cursor != null && !cursor.isClosed()) { cursor.close(); }
         *
         * mMsgListAdapter.changeCursor(null);
         * mMsgListAdapter.cancelBackgroundLoading(); }
         */

        mAudioServcie.releaseService();

        saveDraft(true);

        // set 'mShouldLoadDraft' to true, so when coming back to
        // ComposeMessageActivity, we would
        // load the draft, unless we are coming back to the activity after
        // attaching a photo, etc,
        // in which case we should set 'mShouldLoadDraft' to false.
        mShouldLoadDraft = true;
    }

    @Override
    protected void onDestroy() {

        mIsStartMultiDeleteActivity = false;

        mPAService.unregisterCallback(mToken);
        mPAService = null;

        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged() -- begin");

        super.onConfigurationChanged(newConfig);
        if (mEmoji.isEmojiPanelShow()) {
            mEmoji.resetEmojiPanel(newConfig);
        }
        if (mSharePanel != null) {
            mSharePanel.resetShareItem();
        }
        if (resetConfiguration(newConfig)) {
            drawTopPanel();
        }
        onKeyboardStateChanged();
        Log.d(TAG, "onConfigurationChanged() -- end");
    }

    // returns true if landscape/portrait configuration has changed
    private boolean resetConfiguration(Configuration config) {
        Log.d(TAG, "resetConfiguration begin");
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        Log.d(TAG, "resetConfiguration: isLandscape=" + isLandscape);
        if ((mTextEditor != null)
                && (mTextEditor.getVisibility() == View.VISIBLE) && isLandscape) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mTextEditor
                            .setMaxHeight(mReferencedTextEditorTowLinesHeight
                                    * mCurrentMaxHeight / mReferencedMaxHeight);
                }
            }, 100);
        }
        // why
        if (!isLandscape && mIsLandscape == isLandscape && mIsSoftKeyBoardShow) {
            showSharePanel(false);
        }

        if (mIsLandscape != isLandscape) {
            // mUpdateForScrnOrientationChanged = true;
            mIsLandscape = isLandscape;
            Log.d(TAG, "resetConfiguration end. mLandscape change to "
                    + mIsLandscape);
            return true;
        }
        Log.d(TAG, "resetConfiguration end. no change");
        return false;
    }

    private void updateForEnable() {
        Log.d(TAG, "updateForEnable");
        onKeyboardStateChanged();
        updateSendButtonState();
        updateShareButtonEnabled();
        invalidateOptionsMenu();
    }

    private void onKeyboardStateChanged() {
        Log.d(TAG, "onKeyboardStateChanged:" + isPASrvReady() + ","
                + isAccountValid());
        mTextEditor.setEnabled(isPASrvReady() && isAccountValid());
        if (!isPASrvReady()) {
            mTextEditor.setFocusableInTouchMode(false);
            mTextEditor.setHint(R.string.service_not_ready);
            mTextEditor.clearFocus();
        } else if (!isAccountValid()) {
            mTextEditor.setFocusableInTouchMode(false);
            if (!isAccountSubscribed()) {
                mTextEditor.setHint(R.string.account_not_subscribed);
            } else {
                mTextEditor.setHint(R.string.account_stopped);
            }
            mTextEditor.clearFocus();
        } else if (mIsKeyboardOpen) {
            mTextEditor.setFocusableInTouchMode(true);
            mTextEditor.setHint(R.string.ipmsg_sms_hint);
            mTextEditor
                    .setFilters(new InputFilter[] { new InputFilter.LengthFilter(
                            3000) });
            updateCounter(mWorkingMessage.getText(), 0, 0, 0);
            mTextEditor.requestFocus();
        } else {
            mTextEditor.setFocusable(false);
            mTextEditor.setHint(R.string.open_keyboard_to_compose_message);
            mTextEditor.clearFocus();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
        case KeyEvent.KEYCODE_DEL:
            if ((mMsgListAdapter != null) && mMsgListView.isFocused()) {
                Cursor cursor;
                try {
                    cursor = (Cursor) mMsgListView.getSelectedItem();
                } catch (ClassCastException e) {
                    Log.e(TAG, "Unexpected ClassCastException.", e);
                    return super.onKeyDown(keyCode, event);
                }

                if (cursor != null) {
                    long msgId = cursor.getLong(cursor
                            .getColumnIndexOrThrow(MessageColumns.ID));
                    MessageData msgItem = mMsgListAdapter.getCachedMessageData(
                            msgId, cursor);
                    if (msgItem != null) {
                        DeleteMessageListener l = new DeleteMessageListener(
                                msgItem);
                        confirmDeleteDialog(l, msgItem.mLocked);
                    }
                    return true;
                }
            }
            break;

        case KeyEvent.KEYCODE_BACK:
            if (mEmoji.isEmojiPanelShow()) {
                mEmoji.showEmojiPanel(false);
                return true;
            }
            if (isSharePanelShow()) {
                hideSharePanel();
                return true;
            }
            exitComposeMessageActivity(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            return true;
        case KeyEvent.KEYCODE_MENU:
            invalidateOptionsMenu();
            return false;
        default:
            break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exitComposeMessageActivity(final Runnable exit) {
        // If the message is empty, just quit -- finishing the
        // activity will cause an empty draft to be deleted.
        if (!mWorkingMessage.isWorthSaving()) {
            exit.run();
            return;
        }

        mToastForDraftSave = true;
        exit.run();
    }

    Runnable mResetMessageRunnable = new Runnable() {
        @Override
        public void run() {
            resetMessage();
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();
        menu.add(0, MENU_PA_DETAILS, 0, R.string.show_pa_details)
                .setIcon(R.drawable.ic_account_detail)
                .setTitle(R.string.show_pa_details)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (isPASrvReady() && mMsgListAdapter.getCount() > 0) {
            menu.add(0, MENU_PA_SELECT_MESSAGE, 0, R.string.select_message);
        }
        Log.d(TAG, "add details menu icon");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PA_DETAILS:
            Intent intent = new Intent();
            intent.setAction(AccountDetailsActivity.ACTION);
            intent.setClass(this, AccountDetailsActivity.class);
            intent.putExtra(AccountDetailsActivity.KEY_UUID, mUuid);
            intent.putExtra(AccountDetailsActivity.KEY_ID, mAccountId);
            Log.d(TAG, "Goto AccountDetailsActivity with uuid:" + mUuid);
            startActivity(intent);
            break;
        case MENU_PA_DELETE_ALL:
            DeleteAllMessageListener l = new DeleteAllMessageListener();
            confirmDeleteDialog(l, true);
            break;

        case MENU_PA_SELECT_MESSAGE:
            Intent intentSelectMessage = new Intent(this,
                    PaMultiDeleteActivity.class);
            intentSelectMessage.putExtra(ACCOUNT_ID, mAccountId);
            startActivityForResult(intentSelectMessage,
                    REQUEST_CODE_FOR_MULTIDELETE);
            mIsStartMultiDeleteActivity = true;
            break;

        case android.R.id.home:
            exitComposeMessageActivity(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            break;
        default:
            Log.d(TAG,
                    "onOptionsItemSelected() unkonw option:" + item.getItemId());
            break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode
                + ", resultCode=" + resultCode + ", data=" + data);

        mWaitingForSubActivity = false; // We're back!
        mShouldLoadDraft = false;

        if (resultCode != RESULT_OK) {
            Log.d(TAG, "onActivityResult() failed. resultCode=" + resultCode);
            return;
        }

        if (data == null) {
            Log.d(TAG, "onActivityResult() but data is null.");
        }

        Uri uri = null;
        switch (requestCode) {
        case REQUEST_CODE_PAMSG_TAKE_PHOTO:
            if (null == mAttamentUri) {
                return;
            }
            sendImageAsync(mAttamentUri);
            break;

        case REQUEST_CODE_PAMSG_CHOOSE_PHOTO:
            uri = data.getData();
            if (null == uri) {
                Log.e(TAG, "REQUEST_CODE_PAMSG_CHOOSE_PHOTO: uri is null");
                return;
            }
            sendImageAsync(uri);
            break;

        case REQUEST_CODE_PAMSG_RECORD_VIDEO:
        case REQUEST_CODE_PAMSG_CHOOSE_VIDEO:
            uri = data.getData();
            if (null == uri) {
                Log.e(TAG, "REQUEST_CODE_PAMSG_CHOOSE_VIDEO: uri is null");
                return;
            }
            sendVideoAsync(uri);
            break;

        case REQUEST_CODE_PAMSG_SHARE_CONTACT:
            long[] contactsId = data
                    .getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");
            if (null == contactsId || contactsId.length == 0) {
                Log.e(TAG,
                        "REQUEST_CODE_PAMSG_SHARE_CONTACT: contactsId is empty");
                return;
            }
            sendVCardAsync(contactsId);
            break;

        case REQUEST_CODE_PAMSG_RECORD_AUDIO:
            uri = data.getData();
            if (null == uri) {
                Log.e(TAG, "REQUEST_CODE_PAMSG_RECORD_AUDIO: uri is null");
                return;
            }
            sendAudioAsync(uri);
            break;

        case REQUEST_CODE_PAMSG_CHOOSE_AUDIO:
            uri = (Uri) data
                    .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (Settings.System.getUriFor(Settings.System.RINGTONE).equals(uri)) {
                Log.e(TAG,
                        "REQUEST_CODE_PAMSG_CHOOSE_AUDIO: this is Sysytem ringtone");
                return;
            }
            if (uri == null) {
                uri = data.getData();
            }
            if (uri == null) {
                Log.e(TAG, "REQUEST_CODE_PAMSG_CHOOSE_AUDIO: uri is null");
                return;
            }
            sendAudioAsync(uri);
            break;

        case REQUEST_CODE_FOR_MULTIDELETE:
            mIsStartMultiDeleteActivity = false;
            break;

        default:
            Log.d(TAG, "onActivityResult() unkown requestCode=" + requestCode);
            break;
        }
    }

    boolean waitForSrvReady(int interval, int timeout) {
        int time = 0;
        while (time <= timeout) {
            if (isPASrvReady()) {
                Log.d(TAG, "waitForSrvReady ready:" + time);
                return true;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            time += interval;
        }

        Log.d(TAG, "waitForSrvReady timeout:" + time);
        return false;
    }

    boolean waitForSrvReady() {
        return waitForSrvReady(200, 5000);
    }

    private long getMaxFileTransferSize() {
        return mPAService.getMaxFileTransferSize(mToken) * 1024;
    }

    void sendAudioAsync(final Uri uri) {

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (!waitForSrvReady()) {
                    mSendErrorCode = ERR_SRV_NOT_READY;
                    return;
                }
                mSendErrorCode = mWorkingMessage.setAttachment(
                        PaWorkingMessage.AUDIO, uri, getMaxFileTransferSize());
                if (PaWorkingMessage.OK == mSendErrorCode) {
                    sendMessage(PaWorkingMessage.AUDIO);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                    return;
                }
            }
        }, R.string.sending);
    }

    void sendVideoAsync(final Uri uri) {

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (!waitForSrvReady()) {
                    mSendErrorCode = ERR_SRV_NOT_READY;
                    return;
                }
                mSendErrorCode = mWorkingMessage.setAttachment(
                        PaWorkingMessage.VIDEO, uri, getMaxFileTransferSize());
                if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "Video setAttachment fail");
                    return;
                }
                mSendErrorCode = mWorkingMessage.generateThumbnail();
                if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "Video generate thumbnail fail");
                    return;
                }
                sendMessage(PaWorkingMessage.VIDEO);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                }
            }
        }, R.string.sending);
    }

    void sendImageAsync(final Uri uri) {

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (!waitForSrvReady()) {
                    mSendErrorCode = ERR_SRV_NOT_READY;
                    return;
                }
                mSendErrorCode = mWorkingMessage.setAttachment(
                        PaWorkingMessage.IMAGE, uri, getMaxFileTransferSize());
                if (!mSendOrgPic && !mWorkingMessage.isGif()) {
                    mSendErrorCode = mWorkingMessage.zoomInImage();
                    if (PaWorkingMessage.OK != mSendErrorCode) {
                        Log.d(TAG, "sendImageAsync zoomin fail");
                        return;
                    }
                } else if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "Image setAttachment fail");
                    return;
                }
                sendMessage(PaWorkingMessage.IMAGE);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (PaWorkingMessage.ERR_IMAGE_TOO_LARGE == mSendErrorCode) {
                    if (mWorkingMessage.isGif()) {
                        showSendErrorDialog();
                    } else {
                        showResizeConfirmDialog();
                    }
                } else if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                }
            }
        }, R.string.sending);
    }

    void resizeAndSendImageAsync() {

        if (PaWorkingMessage.IMAGE != mWorkingMessage.getAttachmentType()) {
            Log.d(TAG,
                    "error attachment type:"
                            + mWorkingMessage.getAttachmentType());
            return;
        }

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (!waitForSrvReady()) {
                    mSendErrorCode = ERR_SRV_NOT_READY;
                    return;
                }
                mSendErrorCode = mWorkingMessage.zoomInImage();
                if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "zoomin fail");
                    return;
                }
                // mSendErrorCode = mWorkingMessage.generateThumbnail();
                // if (PaWorkingMessage.OK != mSendErrorCode) {
                // Log.d(TAG, "Image generate thumbnail fail");
                // return;
                // }
                sendMessage(PaWorkingMessage.IMAGE);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                }
            }
        }, R.string.sending);
    }

    void sendVCardAsync(final long[] ids) {

        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                if (!waitForSrvReady()) {
                    mSendErrorCode = ERR_SRV_NOT_READY;
                    return;
                }
                mSendErrorCode = mWorkingMessage.setVCardAttachment(ids);
                if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "VCard setAttachment fail");
                    return;
                }
                sendMessage(PaWorkingMessage.VCARD);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                }
            }
        }, R.string.sending);
    }

    private void sendGeoLocationAsync() {
        Log.e(TAG, "sendGeoLocationAsync location=" + sLocation);
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                mSendErrorCode = mWorkingMessage.setLocAttachment(sLocation);
                if (PaWorkingMessage.OK != mSendErrorCode) {
                    Log.d(TAG, "location setAttachment fail");
                    return;
                }
                sendMessage(PaWorkingMessage.GEOLOC);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable
                // is run
                // on the UI thread.
                if (mSendErrorCode != 0) {
                    showSendErrorDialog();
                }
            }
        }, R.string.sending);
    }

    AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }

    private void showSendErrorDialog() {
        String errorHint;

        switch (mSendErrorCode) {
        case PaWorkingMessage.ERR_UNKOWN:
            errorHint = new String(getString(R.string.error_unkown));
            break;
        case PaWorkingMessage.ERR_AUDIO_TOO_LONG:
            errorHint = new String(getString(R.string.error_audio_too_long));
            break;
        case PaWorkingMessage.ERR_VIDEO_TOO_LARGE:
            errorHint = new String(getString(R.string.error_video_too_large));
            break;
        case PaWorkingMessage.ERR_IMAGE_TOO_LARGE:
            errorHint = new String(getString(R.string.error_image_too_large));
            break;
        case PaWorkingMessage.ERR_UNSUPPORT_TYPE:
            errorHint = new String(getString(R.string.error_unsupport_type));
            break;
        case PaWorkingMessage.ERR_PATH_NOT_EXIST:
            errorHint = new String(getString(R.string.error_path_not_exist));
            break;
        case PaWorkingMessage.ERR_FILE_NOT_FIND:
            errorHint = new String(getString(R.string.error_file_not_found));
            break;
        case ERR_SRV_NOT_READY:
            errorHint = new String(getString(R.string.service_not_ready));
            break;
        default:
            errorHint = new String(getString(R.string.error_unkown));
            break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                PaComposeActivity.this);
        builder.setIcon(R.drawable.ic_sms_mms_not_delivered);
        builder.setTitle(R.string.send_fail);
        builder.setMessage(errorHint);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    dialog.dismiss();
                }
            }
        }).show();
    }

    private void showResizeConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                PaComposeActivity.this);
        builder.setIcon(R.drawable.ic_sms_mms_not_delivered)
                .setTitle(R.string.image_tool_large)
                .setMessage(R.string.image_resize_hint)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            dialog.dismiss();
                            resizeAndSendImageAsync();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    dialog.dismiss();
                                    showSendErrorDialog();
                                }
                            }
                        }).show();
    }

    /**
     * draw the compose view at the bottom of the screen.
     */
    private void drawBottomPanel() {
        Log.d(TAG, "drawBottomPanel");

        // resetCounter();
        updateTextEditorHeightInFullScreen();
        mBottomPanel.setVisibility(View.VISIBLE);

        CharSequence text = mWorkingMessage.getText();
        if (text != null) { // && isPASrvReady()
            mTextEditor.setTextKeepState(text);
            try {
                mTextEditor.setSelection(mTextEditor.length());
            } catch (IndexOutOfBoundsException e) {
                mTextEditor.setSelection(mTextEditor.length() - 1);
            }
        } else {
            mTextEditor.setText("");
        }
        onKeyboardStateChanged();

        updateCounter(mWorkingMessage.getText(), 0, 0, 0);
    }

    private void hideBottomPanel() {
        Log.d(TAG, "hideBottomPanel");
        mBottomPanel.setVisibility(View.INVISIBLE);
    }

    private void drawTopPanel() {
        updateTextEditorHeightInFullScreen();
    }

    // ==========================================================
    // Interface methods
    // ==========================================================

    @Override
    public void onClick(View v) {
        if (v == mSendButton) {
            confirmSendMessageIfNeeded();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        return true;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {

        private Runnable mUpdateRunnable = new Runnable() {
            public void run() {
                updateSendButtonState();
            }
        };

        // private Runnable mRunnable = new Runnable() {
        // public void run() {
        // Toast.makeText(this, R.string.dialog_sms_limit,
        // Toast.LENGTH_LONG).show();
        // }
        // };

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            mBeforeTextChangeString = s.toString();
            Log.d(TAG, "beforeTextChanged count = " + count);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
            mWorkingMessage.setText(s);

            // if (mWorkingMessage.getIsExceedSize()) {
            // mWorkingMessage.setIsExceedSize(false);
            // mHandler.removeCallbacks(mRunnable);
            // mHandler.postDelayed(mRunnable, 200);
            // }

            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.postDelayed(mUpdateRunnable, 100);

            // updateSendButtonState();

            updateCounter(s, start, before, count);

            // ensureCorrectButtonHeight();
            if (before > 0) {
                return;
            }

            if (!TextUtils.isEmpty(s.toString())) {
                if (mTextEditor != null) {
                    float textSize = mTextEditor.getTextSize();
                    EmojiImpl emojiImpl = EmojiImpl
                            .getInstance(PaComposeActivity.this);
                    CharSequence str = emojiImpl.getEmojiExpression(s, true,
                            start, count, (int) textSize);
                    mTextEditor.removeTextChangedListener(mTextEditorWatcher);
                    mTextEditor.setTextKeepState(str);
                    mTextEditor.addTextChangedListener(mTextEditorWatcher);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mIsRestore) {
                mIsRestore = false;
                if (!PaComposeActivity.this.isResumed()) {
                    mTextEditor.setText(mBeforeTextChangeString);
                }
            }
        }
    };

    /**
     * Ensures that if the text edit box extends past two lines then the button
     * will be shifted up to allow enough space for the character counter string
     * to be placed beneath it.
     */
    private void ensureCorrectButtonHeight() {
        int currentTextLines = mTextEditor.getLineCount();
        if (currentTextLines <= 2) {
            mTextCounter.setVisibility(View.GONE);
        } else if (currentTextLines > 2
                && mTextCounter.getVisibility() == View.GONE) {
            // Making the counter invisible ensures that it is used to correctly
            // calculate the position of the send button even if we choose not
            // to
            // display the text.
            mTextCounter.setVisibility(View.INVISIBLE);
        }
    }

    // ==========================================================
    // Private methods
    // ==========================================================

    /**
     * Initialize all UI elements from resources.
     */
    private void initResourceRefs() {
        mHeightChangedLinearLayout =
                (HeightChangedLinearLayout) findViewById(R.id.changed_linear_layout);
        mHeightChangedLinearLayout
                .setLayoutSizeChangedListener(mLayoutSizeChangedListener);

        mMsgListView = (PaMessageListView) findViewById(R.id.history);
        mMsgListView.setDivider(null);
        mMsgListView.setDividerHeight(getResources().getDimensionPixelOffset(
                R.dimen.ipmsg_message_list_divider_height));
        mMsgListView.setClipToPadding(false);
        mMsgListView.setClipChildren(false);

        mBottomPanel = findViewById(R.id.bottom_panel);
        mTextEditor = (EditText) findViewById(R.id.embedded_text_editor);
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
        // Todo: read configure
        mTextEditor
                .setFilters(new InputFilter[] { new InputFilter.LengthFilter(
                        3000) });
        mTextEditor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEmoji.showEmojiPanel(false);
                if (mShowKeyBoardFromShare) {
                    showSharePanel(false);
                    updateFullScreenTextEditorHeight();
                    v.performClick();
                }
                return false;
            }
        });

        mTextCounter = (TextView) findViewById(R.id.text_counter);
        mSendButton = (ImageButton) findViewById(R.id.send_button_ipmsg);
        mSendButton.setOnClickListener(this);

        // For Menu layout
        mInputContainerLayout = (LinearLayout) findViewById(R.id.bottom_panel_container);
        mMenuContainerLayout = (LinearLayout) findViewById(R.id.bottom_panel_container2);
        mInputSwitchLayout = (LinearLayout) findViewById(R.id.bottom_switch);
        mSwitchToMenuBtn = (ImageButton) findViewById(R.id.switch_button);
        mSwitchToMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBottomPanel(true);
            }
        });

        mMenuLayout = (LinearLayout) getLayoutInflater().inflate(
                R.layout.bottom_menu_panel, null);
        mSwitchToInputBtn = (ImageButton) mMenuLayout
                .findViewById(R.id.switch_button_menu);
        mSwitchToInputBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBottomPanel(false);
            }
        });

        mMenuButtons = new Button[] {
                (Button) mMenuLayout.findViewById(R.id.button_1st),
                (Button) mMenuLayout.findViewById(R.id.button_2nd),
                (Button) mMenuLayout.findViewById(R.id.button_3rd) };

        mMenuOutters = new LinearLayout[] {
                (LinearLayout) mMenuLayout.findViewById(R.id.button_outter_1),
                (LinearLayout) mMenuLayout.findViewById(R.id.button_outter_2),
                (LinearLayout) mMenuLayout.findViewById(R.id.button_outter_3) };

        mMenuContainerLayout.addView(mMenuLayout);

        mWallPaper = (ImageView) findViewById(R.id.wall_paper);
    }

    public void setTextSize(float size) {
        if (mTextEditor != null) {
            mTextEditor.setTextSize(size);
        }
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }
        if (mMsgListView != null
                && mMsgListView.getVisibility() == View.VISIBLE) {
            int count = mMsgListView.getChildCount();
            for (int i = 0; i < count; i++) {
                PaMessageListItem item = (PaMessageListItem) mMsgListView
                        .getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        super.dispatchTouchEvent(event);
        mScaleDetector.onTouchEvent(event);

        return false;
    }

    private void initShareRessource() {
        mShareButton = (ImageButton) findViewById(R.id.share_button);
        mShareButton
                .setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mEmoji) {
                            mEmoji.showEmojiPanel(false);
                        }
                        if (mShowKeyBoardFromShare) {
                            showSharePanelOrKeyboard(false, true);
                        } else {
                            showSharePanelOrKeyboard(true, false);
                            mTextEditor.requestFocus();
                        }
                    }
                });
        mSharePanel = (PaSharePanel) findViewById(R.id.share_panel);
        mSharePanel.setHandler(mHandler);
        showSharePanelOrKeyboard(false, false);
    }

    private void showSharePanel(boolean isShow) {
        Log.d(TAG, "showSharePanel isShow=" + isShow);
        if (null != mSharePanel) {
            if (!isPASrvReady() || !isAccountValid()) {
                mSharePanel.setVisibility(View.GONE);
                mShareButton.setClickable(false);
                mShareButton.setImageResource(R.drawable.ipmsg_share_disable);
                return;
            }
            if (isShow) {
                mSharePanel.setVisibility(View.VISIBLE);
                mShareButton.setImageResource(R.drawable.ipmsg_keyboard);
            } else {
                mSharePanel.setVisibility(View.GONE);
                mShareButton.setImageResource(R.drawable.ipmsg_share);
            }
            mShareButton.setClickable(true);
            mShowKeyBoardFromShare = isShow;
        }
    }

    private void updateShareButtonEnabled() {
        if (null != mSharePanel) {
            if (!isPASrvReady() || !isAccountValid()) {
                showSharePanel(false);
                mShareButton.setEnabled(false);
                mShareButton.setImageResource(R.drawable.ipmsg_share_disable);
            } else {
                mShareButton.setImageResource(R.drawable.ipmsg_share);
                mShareButton.setEnabled(true);
                mShareButton.setClickable(true);
            }
        }
    }

    private void showKeyBoard(boolean isShow) {
        Log.d(TAG, "showKeyBoard isShow=" + isShow);
        if (isShow) {
            mTextEditor.requestFocus();
            mInputMethodManager.showSoftInput(mTextEditor, 0);
            mIsKeyboardOpen = true;
        } else {
            hideInputMethod();
        }
    }

    private void hideInputMethod() {
        Log.d(TAG, "InputMethod()");
        if (this.getWindow() != null
                && this.getWindow().getCurrentFocus() != null) {
            mInputMethodManager.hideSoftInputFromWindow(this.getWindow()
                    .getCurrentFocus().getWindowToken(), 0);
        }
    }

    public boolean isSharePanelShow() {
        if (null != mSharePanel && mSharePanel.isShown()) {
            return true;
        }
        return false;
    }

    public void showSharePanelOrKeyboard(final boolean isShowShare,
            final boolean isShowKeyboard) {
        Log.d(TAG, "showSharePanelOrKeyboard. isShowShare=" + isShowShare
                + ", isShowKeyboard=" + isShowKeyboard
                + ", mIsSoftKeyboardShow=" + mIsSoftKeyBoardShow);
        if (isShowShare && isShowKeyboard) {
            return;
        }

        if (!isShowKeyboard && mIsSoftKeyBoardShow && !mIsLandscape) {
            Log.d(TAG, "showSharePanelOrKeyboard. case 1");
            if (!isShowShare && mShowKeyBoardFromShare) {
                showSharePanel(isShowShare);
            }
            mShowKeyBoardFromShare = isShowShare;
            showKeyBoard(isShowKeyboard);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mWaitingImeChangedObject) {
                        try {
                            int waitTime = 300;
                            Log.d(TAG,
                                    "showSharePanelOrKeyboard(): object start wait.");
                            mWaitingImeChangedObject.wait(waitTime);
                            Log.d(TAG,
                                    "showSharePanelOrKeyboard(): object end wait");
                        } catch (InterruptedException e) {
                            Log.d(TAG, "InterruptedException");
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isShowShare) {
                                showSharePanel(isShowShare);
                                if (mIsLandscape) {
                                    mTextEditor
                                            .setMaxHeight(mReferencedTextEditorTowLinesHeight
                                                    * mCurrentMaxHeight
                                                    / mReferencedMaxHeight);
                                } else {
                                    mTextEditor
                                            .setMaxHeight(mReferencedTextEditorFourLinesHeight
                                                    * mCurrentMaxHeight
                                                    / mReferencedMaxHeight);
                                }
                            } else {
                                Log.d(TAG,
                                        "showSharePanelOrKeyboard(): new thread.");
                                updateFullScreenTextEditorHeight();
                            }
                        }
                    });
                }

            }).start();
        } else {
            Log.d(TAG, "showSharePanelOrKeyboard. case 2");
            if (isShowShare && !isShowKeyboard && mIsLandscape) {
                Log.d(TAG, "showSharePanelOrKeyboard. case 21");
                showKeyBoard(isShowKeyboard);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mWaitingImeChangedObject) {
                            try {
                                int waitTime = 100;
                                Log.d(TAG,
                                        "showSharePanelOrKeyboard() mIsLandscape: " +
                                        "object start wait.");
                                mWaitingImeChangedObject.wait(waitTime);
                                Log.d(TAG,
                                        "showSharePanelOrKeyboard(): object end wait");
                            } catch (InterruptedException e) {
                                Log.d(TAG, "InterruptedException");
                                e.printStackTrace();
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSharePanel(isShowShare);
                                mTextEditor
                                        .setMaxHeight(mReferencedTextEditorTowLinesHeight
                                                * mCurrentMaxHeight
                                                / mReferencedMaxHeight);
                            }
                        });
                    }
                }).start();
            } else {
                Log.d(TAG, "showSharePanelOrKeyboard. case 22");
                showKeyBoard(isShowKeyboard);
                showSharePanel(isShowShare);
                if (isShowShare || isShowKeyboard) {
                    if (mIsLandscape) {
                        mTextEditor
                                .setMaxHeight(mReferencedTextEditorTowLinesHeight
                                        * mCurrentMaxHeight
                                        / mReferencedMaxHeight);
                    } else {
                        mTextEditor
                                .setMaxHeight(mReferencedTextEditorFourLinesHeight
                                        * mCurrentMaxHeight
                                        / mReferencedMaxHeight);
                    }
                } else {
                    // updateFullScreenTextEditorHeight();
                }
            }
        }
    }

    public void hideSharePanel() {
        Log.d(TAG, "hideSharePanel()");
        showSharePanelOrKeyboard(false, false);
        updateFullScreenTextEditorHeight();
    }

    public void doMoreAction(Message msg) {
        Bundle bundle = msg.getData();
        int action = bundle.getInt(PaSharePanel.SHARE_ACTION);
        Log.d(TAG, "doMoreAction. action=" + action);
        switch (action) {
        case PaSharePanel.IPMSG_TAKE_PHOTO:
            takePhoto();
            break;
        case PaSharePanel.IPMSG_RECORD_VIDEO:
            recordVideo();
            break;
        case PaSharePanel.IPMSG_RECORD_AUDIO:
            recordAudio();
            break;
        case PaSharePanel.IPMSG_CHOOSE_PHOTO:
            choosePhoto();
            break;
        case PaSharePanel.IPMSG_CHOOSE_VIDEO:
            chooseVideo();
            break;
        case PaSharePanel.IPMSG_CHOOSE_AUDIO:
            chooseAudio();
            break;
        case PaSharePanel.IPMSG_SHARE_CONTACT:
            shareContact();
            break;
        case PaSharePanel.IPMSG_SHARE_POSITION:
            sharePosition();
            break;
        default:
            Log.d(TAG, "doMoreAction(). invalid action type");
            break;
        }
        hideSharePanel();
    }

    public void takePhoto() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String photoFilePath = Utils.getTempFilePath(Utils.PA_FILE_PREFIX_IMG,
                Utils.PA_FILE_SUFFIX_JPG);
        Log.d(TAG, "takePhoto() with path:" + photoFilePath);
        File out = new File(photoFilePath);
        mAttamentUri = Uri.fromFile(out);

        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mAttamentUri);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,
                Utils.getPhotoSizeLimit());
        // imageCaptureIntent.putExtra("mediatek.intent.extra.EXTRA_RESOLUTION_LIMIT",
        // Utils.getPhotoResolutionLimit());

        startActivityForResult(imageCaptureIntent,
                REQUEST_CODE_PAMSG_TAKE_PHOTO);
    }

    private void recordVideo() {
        int durationLimit = Utils.getVideoCaptureDurationLimit();
        String resolutionLimit = Utils.getVideoResolutionLimit();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        String videoFilePath = Utils.getTempFilePath(Utils.PA_FILE_PREFIX_VDO,
                Utils.PA_FILE_SUFFIX_3GP);
        File out = new File(videoFilePath);
        Uri uri = Uri.fromFile(out);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        // intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 300*1024);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
        intent.putExtra("mediatek.intent.extra.EXTRA_RESOLUTION_LIMIT",
                resolutionLimit);

        try {
            startActivityForResult(intent, REQUEST_CODE_PAMSG_RECORD_VIDEO);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_such_app),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "no record video app");
        }
    }

    private void recordAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/amr");
        intent.setClassName("com.android.soundrecorder",
                "com.android.soundrecorder.SoundRecorder");
        intent.putExtra("com.android.soundrecorder.maxduration",
                Utils.getAudioDurationLimit());
        try {
            startActivityForResult(intent, REQUEST_CODE_PAMSG_RECORD_AUDIO);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_such_app),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "no record audio app");
        }
    }

    private void choosePhoto() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_PAMSG_CHOOSE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_such_app),
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "no choose photo app");
        }
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_CODE_PAMSG_CHOOSE_VIDEO);
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // intent.setType("audio/*");
        intent.setType(ContentType.AUDIO_UNSPECIFIED);
        // supprt 5 types:mp3, 3gpp, m4a, aac, amr
        // String[] mimeTypess = new String[] {
        // "audio/mpeg", "audio/3gpp", "audio/mpeg", "audio/aac", "audio/amr"
        // };
        String[] mimeTypess = new String[] { ContentType.AUDIO_UNSPECIFIED,
                ContentType.AUDIO_MP3, ContentType.AUDIO_3GPP, "audio/M4A",
                ContentType.AUDIO_AAC, ContentType.AUDIO_AMR };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypess);
        startActivityForResult(intent, REQUEST_CODE_PAMSG_CHOOSE_AUDIO);
    }

    private void shareContact() {
        /*
         * String[] items = new String[] {"Add contacts","add groups"};
         * AlertDialog.Builder b = new AlertDialog.Builder(this);
         * b.setTitle("Add vcard"). setCancelable(true). setItems(items, new
         * OnClickListener() {
         *
         * @Override public void onClick(DialogInterface dialog, int which) {
         * switch(which) { case 0: addContacts(); break; case 1: addGroups();
         * break; default: break; } } }).create().show();
         */
        addContacts();
    }

    private void addContacts() {
        Intent intent = new Intent(
                "android.intent.action.contacts.list.PICKMULTICONTACTS");
        intent.setType(Contacts.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_CODE_PAMSG_SHARE_CONTACT);
    }

    private void sharePosition() {
        mGeoLocSrv = new GeoLocService(this);
        queryGeoLocation();
    }

    private void queryGeoLocation() {
        if (mGeoLocSrv.isEnable()) {
            showDialog(DIALOG_ID_GEOLOC_PROGESS);
        } else {
            Toast.makeText(this, getString(R.string.geoloc_check_gps),
                    Toast.LENGTH_LONG).show();
        }
    }

    private class GeoLocCallback implements GeoLocService.callback {
        public void queryGeoLocResult(boolean ret, final Location location) {
            removeDialog(DIALOG_ID_GEOLOC_PROGESS);
            mGeoLocSrv.removeCallback();
            if (ret) {
                sLocation = location;
                // debug code
                // mLocation = new Location("debug");
                // mLocation.setLatitude(24.9694964);
                // mLocation.setLongitude(102.7673732);
                showDialog(DIALOG_ID_GEOLOC_SEND_CONFIRM);
            } else {
                Toast.makeText(PaComposeActivity.this,
                        getString(R.string.geoloc_get_failed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean setWallPaper() {
        Log.d(TAG, "setWallPaper()");
        Uri tempuri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI,
                "thread_settings");
        Cursor cursor = getContentResolver().query(
                tempuri,
                new String[] { Telephony.ThreadSettings._ID,
                        Telephony.ThreadSettings.WALLPAPER },
                Telephony.ThreadSettings.THREAD_ID + " = 0", null, null);

        long threadSettingId = 0L;
        String filePath = null;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                threadSettingId = cursor.getLong(0);
                filePath = cursor.getString(1);
                Log.d(TAG, "threadId=" + threadSettingId + ". filePath="
                        + filePath);
            }
            cursor.close();
        }
        if (TextUtils.isEmpty(filePath)) {
            Log.d(TAG, "no wallpaper");
            return false;
        }

        Uri imageUri;
        if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
            imageUri = Uri.parse(filePath);
        } else if (filePath
                .startsWith("/data/data/com.android.providers.telephony/app_wallpaper")) {
            imageUri = ContentUris.withAppendedId(tempuri, 0);
        } else {
            Log.d(TAG, "Invalid path = " + filePath);
            return false;
        }
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(imageUri);
            BitmapDrawable db = new BitmapDrawable(is);
            mWallPaper.setImageDrawable(db);
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unknown wall paper resource! filePath=" + filePath);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemory:" + e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException when close InputStream:" + e);
                }
            }
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Log.d(TAG, "onCreateDialog, id=" + id);
        switch (id) {
        case DIALOG_ID_GEOLOC_PROGESS:
            mGeoLocSrv.queryCurrentGeoLocation(new GeoLocCallback());

            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.geoloc_being_get));
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    mGeoLocSrv.removeCallback();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            return dialog;

        case DIALOG_ID_GEOLOC_SEND_CONFIRM:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.geoloc_send_confirm))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    sendGeoLocationAsync();
                                }
                            }).setNegativeButton(android.R.string.cancel, null);
            AlertDialog dialog2 = builder.create();
            return dialog2;
        default:
            break;
        }
        return null;
    }

    private void confirmDeleteDialog(OnClickListener listener, boolean deleteAll) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.delete_message);
        if (deleteAll) {
            builder.setMessage(R.string.delete_all_confirm);
        } else {
            builder.setMessage(R.string.delete_confirm);
        }
        builder.setPositiveButton(R.string.delete_message, listener);
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    private void startMsgListQuery() {
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 100);
    }

    private void startMsgListQuery(final int token, int delay) {
        Log.d(TAG, "startMsgListQuery, timeout=" + delay + "AccountId="
                + mAccountId);

        mBackgroundQueryHandler.cancelOperation(token);

        try {
            mBackgroundQueryHandler.postDelayed(new Runnable() {
                public void run() {
                    mBackgroundQueryHandler.startQuery(token, mAccountId,
                            MessageColumns.CONTENT_URI,
                            MessageContent.sFullProjection,
                            MessageColumns.ACCOUNT_ID + "=" + mAccountId
                                    + " AND " + MessageColumns.STATUS + "<"
                                    + Constants.MESSAGE_STATUS_DRAFT + " AND "
                                    + MessageColumns.DELETED + "="
                                    + Constants.DELETED_NO, null, null);
                }
            }, delay);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void initMessageList() {
        if (mMsgListAdapter != null) {
            return;
        }

        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new PaMessageListAdapter(this, null, mMsgListView,
                true, null);
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setSelector(android.R.color.transparent);
        // mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
        mMsgListView.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
        mMsgListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        if (view != null) {
                            ((PaMessageListItem) view).onMessageListItemClick();
                        }
                    }
                });

    }

    /**
     * Load the draft
     *
     * If mWorkingMessage has content in memory that's worth saving, return
     * false. Otherwise, call the async operation to load draft and return true.
     */
    private boolean loadDraft() {
        if (mWorkingMessage.isWorthSaving()) {
            Log.w(TAG,
                    "CMA.loadDraft: called with non-empty working message, bail");
            return false;
        }
        Log.d(TAG, "loadDraft()");

        mWorkingMessage = PaWorkingMessage.loadDraft(this, mAccountId,
                new Runnable() {
                    @Override
                    public void run() {
                        drawTopPanel();
                        drawBottomPanel();
                        updateSendButtonState();
                    }
                });

        // WorkingMessage.loadDraft() can return a new WorkingMessage object
        // that doesn't
        // have its conversation set. Make sure it is set.
        mWorkingMessage.setAccountId(mAccountId);

        return true;
    }

    private void saveDraft(boolean isStopping) {
        Log.d(TAG, "saveDraft");
        // TODO: Do something better here. Maybe make discard() legal
        // to call twice and make isEmpty() return true if discarded
        // so it is caught in the clause above this one?
        if (mWorkingMessage.isDiscarded()) {
            return;
        }

        if (!mWaitingForSubActivity && !mWorkingMessage.isWorthSaving()) {
            Log.d(TAG, "not worth saving, discard WorkingMessage and bail");
            mWorkingMessage.discard();
            return;
        }

        mWorkingMessage.saveDraft(isStopping);

        if (mToastForDraftSave) {
            Toast.makeText(this, R.string.message_saved_as_draft,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPreparedForSending() {
        return mWorkingMessage.hasAttachment() || mWorkingMessage.hasText();
    }

    private long sendMessage(int type) {
        Log.d(TAG, "sendMessage: type:" + type);

        long ret = -1;

        switch (type) {
        case PaWorkingMessage.TEXT:
            String text = mWorkingMessage.getText().toString();
            Log.d(TAG, "sendMessage:" + text);
            ret = mPAService.sendMessage(mToken, mAccountId, text, false);
            mWorkingMessage.discard();
            break;
        case PaWorkingMessage.AUDIO:
            ret = mPAService.sendAudio(mToken, mAccountId,
                    mWorkingMessage.getPath(), mWorkingMessage.getDuration());
            break;
        case PaWorkingMessage.VIDEO:
            ret = mPAService.sendVideo(mToken, mAccountId,
                    mWorkingMessage.getPath(), mWorkingMessage.getThumbnail(),
                    mWorkingMessage.getDuration());
            break;
        case PaWorkingMessage.IMAGE:
            String path = mWorkingMessage.getZoomInImage();
            if (path == null || path.isEmpty()) {
                path = mWorkingMessage.getPath();
            }
            Log.d(TAG, "send image:" + path);
            ret = mPAService.sendImage(mToken, mAccountId, path,
                    mWorkingMessage.getThumbnail());
            break;
        case PaWorkingMessage.VCARD:
            ret = mPAService.sendVcard(mToken, mAccountId,
                    mWorkingMessage.getVCardContent());
            break;
        case PaWorkingMessage.GEOLOC:
            ret = mPAService.sendGeoLoc(mToken, mAccountId,
                    mWorkingMessage.getGeolocContent());
            break;
        default:
            Log.e(TAG, "send unkown type !!!");
            return ret;
        }

        if (ret == Constants.INVALID) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PaComposeActivity.this, R.string.send_fail,
                            Toast.LENGTH_LONG).show();
                }
            });
            Log.e(TAG, "send fail !!!");
            return ret;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetMessage();
            }
        });

        if (ret >= 0) {
            mScrollOnSend = true;
        }
        return ret;
    }

    private void resetMessage() {
        Log.d(TAG, "resetMessage");

        // Focus to the text editor.
        mTextEditor.requestFocus();

        // We have to remove the text change listener while the text editor gets
        // cleared and
        // we subsequently turn the message back into SMS. When the listener is
        // listening while
        // doing the clearing, it's fighting to update its counts and itself try
        // and turn
        // the message one way or the other.
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);

        // Clear the text box.
        TextKeyListener.clear(mTextEditor.getText());

        // mWorkingMessage.clearConversation(mConversation, false);
        mWorkingMessage = PaWorkingMessage.createEmpty(this);
        mWorkingMessage.setAccountId(mAccountId);

        drawBottomPanel();

        // "Or not", in this case.
        updateSendButtonState();

        // Our changes are done. Let the listener respond to text changes once
        // again.
        mTextEditor.addTextChangedListener(mTextEditorWatcher);

        // Close the soft on-screen keyboard if we're in landscape mode so the
        // user can see the
        // conversation.
        if (mIsLandscape) {
            hideKeyboard();
        }

        invalidateOptionsMenu();
        startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN, 0);
    }

    private void hideKeyboard() {
        Log.d(TAG, "hideKeyboard");
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                mTextEditor.getWindowToken(), 0);
    }

    public void updateSendingProgress(final long msgId, final int progress) {
        Log.d(TAG, "updateSendingProgress. msgId=" + msgId + ", progress="
                + progress);

        if (msgId == Constants.INVALID || progress < 0 || progress > 100) {
            return;
        }

        mMsgListAdapter.getSendingQueue().put(msgId, progress);

        final MessageData msgItem = mMsgListAdapter.getCachedMessageData(msgId,
                null);
        if (msgItem == null) {
            Log.d(TAG, "updateSendingProgress() msgItem not in cache now");
            return;
        }

        if (msgItem.getMessageContent().direction != Constants.MESSAGE_DIRECTION_INCOMING) {
            Log.d(TAG, "updateSendingProgress() invalid for incoming message");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int count = mMsgListView.getChildCount();
                for (int i = 0; i < count; i++) {
                    PaMessageListItem listItem = (PaMessageListItem) mMsgListView
                            .getChildAt(i);
                    listItem.setSendingProgress(progress);
                    if (listItem == null || listItem.getMessageData() == null) {
                        continue;
                    }
                    if (listItem.getMessageData().getMessageContent().id == msgId) {
                        listItem.updateSendingProgress();
                        break;
                    }
                }
            }
        });

    }

    private void checkConditionsAndSendMessage() {
        Log.d(TAG, "CheckConditionsAndSendMessage");
        // @TODO, add check condition but only supported in MTK solution
        // final CellConnMgr cellConnMgr = new
        // CellConnMgr(getApplicationContext());
        sendMessage(PaWorkingMessage.TEXT);
    }

    private void updateSendButtonState() {
        boolean enable = false;
        if (isPASrvReady() && isAccountValid()) {
            if (isPreparedForSending()) {
                enable = true;
            }
        }
        Log.d(TAG, "updateSendButtonState set as " + enable);
        mSendButton.setEnabled(enable);
        mSendButton.setFocusable(enable);
    }

    private final HeightChangedLinearLayout.LayoutSizeChangedListener
        mLayoutSizeChangedListener = new HeightChangedLinearLayout.LayoutSizeChangedListener() {

        private int mMaxHeight = 0;

        @Override
        public void onLayoutSizeChanged(int w, int h, int oldw, int oldh) {
            if (h - oldh > SOFT_KEY_BOARD_MIN_HEIGHT) {
                mIsSoftKeyBoardShow = false;
            } else {
                mIsSoftKeyBoardShow = true;
            }
            mMaxHeight = (h > mMaxHeight) ? h : mMaxHeight;
            synchronized (mWaitingImeChangedObject) {
                mWaitingImeChangedObject.notifyAll();
                Log.d(TAG, "onLayoutSizeChanged(): object notified.");
            }
            if (h == oldh || mTextEditor == null
                    || mTextEditor.getVisibility() == View.GONE) {
                return;
            }
            Log.d(TAG, "onLayoutSizeChanged(): mIsLandscape=" + mIsLandscape);
            if (!mIsLandscape) {
                if (h > oldh && !isSharePanelShow() && !mShowKeyBoardFromShare) {
                    updateTextEditorHeightInFullScreen();
                } else if (h > oldh && !isSharePanelShow()
                        && !mShowKeyBoardFromShare) {
                    updateTextEditorHeightInFullScreen();
                } else {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mTextEditor
                                    .setMaxHeight(mReferencedTextEditorFourLinesHeight
                                            * mCurrentMaxHeight
                                            / mReferencedMaxHeight);
                        }
                    }, 100);
                }
            }
        }
    };

    private void updateTextEditorHeightInFullScreen() {
        if (mIsLandscape || mTextEditor == null
                || mTextEditor.getVisibility() == View.GONE) {
            return;
        }
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateFullScreenTextEditorHeight();
            }
        }, 100);
    }

    private void updateFullScreenTextEditorHeight() {
        if (mIsSoftKeyBoardShow && !mIsLandscape) {
            mTextEditor.setMaxHeight(mReferencedTextEditorFourLinesHeight
                    * mCurrentMaxHeight / mReferencedMaxHeight);
        } else if (!mIsSoftKeyBoardShow && !mIsLandscape) {
            if (!isSharePanelShow()) {
                mTextEditor.setMaxHeight(mReferencedTextEditorSevenLinesHeight
                        * mCurrentMaxHeight / mReferencedMaxHeight);
            }
        } else {
            mTextEditor.setMaxHeight(mReferencedTextEditorTowLinesHeight
                    * mCurrentMaxHeight / mReferencedMaxHeight);
        }
    }

    private void initActivityState(Bundle bundle) {
        Intent intent = getIntent();
        if (bundle != null) {
            mAccountId = bundle.getLong(ACCOUNT_ID, 0);
            mUuid = bundle.getString(UUID, null);
            mName = bundle.getString(NAME, null);
            mPath = bundle.getString(IMAGE_PATH, null);
            Log.d(TAG, "get Account Id from bundle");
        } else {
            // If we have been passed a thread_id, use that to find our
            // conversation.
            mAccountId = intent.getLongExtra(ACCOUNT_ID, 0);
            mUuid = intent.getStringExtra(UUID);
            mName = intent.getStringExtra(NAME);
            mPath = intent.getStringExtra(IMAGE_PATH);
            mSelectId = intent.getLongExtra(SELECT_ID, -1);
        }
        if (mPath != null) {
            File file = new File(mPath);
            if (file.exists()) {
                BitmapDrawable bmd = new BitmapDrawable(getResources(), mPath);
                synchronized (sLockObj) {
                    sLogoBitmap = bmd.getBitmap();
                }
            }
        } else {
            synchronized (sLockObj) {
                sLogoBitmap = null;
            }
        }

        Log.d(TAG, "initActivityState: mAccountId=" + mAccountId + ". mName="
                + mName + ". mSelectId=" + mSelectId + ". mUuid=" + mUuid
                + ". mName=" + mName + ". mPath=" + mPath);
    }

    private void initFocus() {
        if (!mIsKeyboardOpen) {
            return;
        }
        if (mTextEditor.isShown()) {
            mTextEditor.requestFocus();
        }
    }

    private final PaMessageListAdapter.OnDataSetChangedListener mDataSetChangedListener =
            new PaMessageListAdapter.OnDataSetChangedListener() {
        @Override
        public void onDataSetChanged(PaMessageListAdapter adapter) {

        }

        @Override
        public void onContentChanged(PaMessageListAdapter adapter) {
            Log.d(TAG, "onContentChanged() to startMsgListQuery()");
            startMsgListQuery();
        }
    };

    /**
     * smoothScrollToEnd will scroll the message list to the bottom if the list
     * is already near the bottom. Typically this is called to smooth scroll a
     * newly received message into view. It's also called when sending to scroll
     * the list to the bottom, regardless of where it is, so the user can see
     * the just sent message. This function is also called when the message list
     * view changes size because the keyboard state changed or the compose
     * message field grew.
     *
     * @param force always scroll to the bottom regardless of current list
     *            position
     * @param listSizeChange the amount the message list view size has
     *            vertically changed
     */
    private void smoothScrollToEnd(boolean force, int listSizeChange) {
        int lastItemVisible = mMsgListView.getLastVisiblePosition();
        int lastItemInList = mMsgListAdapter.getCount() - 1;
        if (lastItemVisible < 0 || lastItemInList < 0) {
            Log.v(TAG, "smoothScrollToEnd: lastItemVisible=" + lastItemVisible
                    + ", lastItemInList=" + lastItemInList
                    + ", mMsgListView not ready");
            return;
        }

        View lastChildVisible = mMsgListView.getChildAt(lastItemVisible
                - mMsgListView.getFirstVisiblePosition());
        int lastVisibleItemBottom = 0;
        int lastVisibleItemHeight = 0;
        if (lastChildVisible != null) {
            lastVisibleItemBottom = lastChildVisible.getBottom();
            lastVisibleItemHeight = lastChildVisible.getHeight();
        }

        Log.v(TAG,
                "smoothScrollToEnd newPosition: "
                        + lastItemInList
                        + " mLastSmoothScrollPosition: "
                        + mLastSmoothScrollPosition
                        + " first: "
                        + mMsgListView.getFirstVisiblePosition()
                        + " lastItemVisible: "
                        + lastItemVisible
                        + " lastVisibleItemBottom: "
                        + lastVisibleItemBottom
                        + " lastVisibleItemBottom + listSizeChange: "
                        + (lastVisibleItemBottom + listSizeChange)
                        + " mMsgListView.getHeight() - mMsgListView.getPaddingBottom(): "
                        + (mMsgListView.getHeight() - mMsgListView
                                .getPaddingBottom()) + " listSizeChange: "
                        + listSizeChange);

        // Only scroll if the list if we're responding to a newly sent message
        // (force == true) or
        // the list is already scrolled to the end. This code also has to handle
        // the case where
        // the listview has changed size (from the keyboard coming up or down or
        // the message entry
        // field growing/shrinking) and it uses that grow/shrink factor in
        // listSizeChange to
        // compute whether the list was at the end before the resize took place.
        // For example, when the keyboard comes up, listSizeChange will be
        // negative, something
        // like -524. The lastChild listitem's bottom value will be the old
        // value before the
        // keyboard became visible but the size of the list will have changed.
        // The test below
        // add listSizeChange to bottom to figure out if the old position was
        // already scrolled
        // to the bottom. We also scroll the list if the last item is taller
        // than the size of the
        // list. This happens when the keyboard is up and the last item is an
        // mms with an
        // attachment thumbnail, such as picture. In this situation, we want to
        // scroll the list so
        // the bottom of the thumbnail is visible and the top of the item is
        // scroll off the screen.
        int listHeight = mMsgListView.getHeight();
        boolean lastItemTooTall = lastVisibleItemHeight > listHeight;
        boolean willScroll = force
                || ((listSizeChange != 0 || lastItemInList != mLastSmoothScrollPosition)
                        && lastVisibleItemBottom + listSizeChange
                            <= listHeight - mMsgListView.getPaddingBottom());
        if (willScroll
                || (lastItemTooTall && lastItemInList == lastItemVisible)) {
            if (Math.abs(listSizeChange) > SMOOTH_SCROLL_THRESHOLD) {
                // When the keyboard comes up, the window manager initiates a
                // cross fade
                // animation that conflicts with smooth scroll. Handle that case
                // by jumping the
                // list directly to the end.
                Log.v(TAG, "keyboard state changed. setSelection="
                        + lastItemInList);
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole
                    // height of the list,
                    // we need to scroll that item so that its top is negative
                    // or above the top of
                    // the list. That way, the bottom of the last item will be
                    // exposed above the
                    // keyboard.
                    mMsgListView.setSelectionFromTop(lastItemInList, listHeight
                            - lastVisibleItemHeight);
                } else {
                    mMsgListView.setSelection(lastItemInList);
                }
            } else if (lastItemInList - lastItemVisible > MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT) {
                Log.v(TAG, "too many to scroll, setSelection=" + lastItemInList);
                mMsgListView.setSelection(lastItemInList);
            } else {
                Log.v(TAG, "smooth scroll to " + lastItemInList);
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole
                    // height of the list,
                    // we need to scroll that item so that its top is negative
                    // or above the top of
                    // the list. That way, the bottom of the last item will be
                    // exposed above the
                    // keyboard. We should use smoothScrollToPositionFromTop
                    // here, but it doesn't
                    // seem to work -- the list ends up scrolling to a random
                    // position.
                    mMsgListView.setSelectionFromTop(lastItemInList, listHeight
                            - lastVisibleItemHeight);
                } else {
                    mMsgListView.smoothScrollToPosition(lastItemInList);
                }
                mLastSmoothScrollPosition = lastItemInList;
            }
        }
    }

    private class AccountObserver extends ContentObserver {
        public AccountObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "AccountObserver onChange:" + selfChange);

            Cursor cursor = getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    PublicAccount.sDetailProjection,
                    AccountColumns.TABLE + "." + AccountColumns.ID + "=?",
                    new String[] { Long.toString(mAccountId) }, null, null);

            if (cursor == null) {
                Log.d(TAG, "query account but cursor is null. account="
                        + mAccountId);
                return;
            }

            if (cursor.getCount() <= 0) {
                Log.d(TAG, "query account but count is 0. account="
                        + mAccountId);
                cursor.close();
                return;
            }

            cursor.moveToFirst();
            String logoPath = cursor.getString(cursor
                    .getColumnIndexOrThrow(AccountColumns.LOGO_PATH));

            if ((logoPath != null && mPath == null)
                    || (logoPath != null && mPath != null && !mPath
                            .equals(logoPath))) {
                mPath = logoPath;
                File file = new File(mPath);
                if (file.exists()) {
                    Log.d(TAG, "update mLogoBitmap");
                    BitmapDrawable bmd = new BitmapDrawable(getResources(),
                            mPath);
                    synchronized (sLockObj) {
                        sLogoBitmap = bmd.getBitmap();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (null != mMsgListAdapter) {
                                mMsgListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
            cursor.close();
        }
    }

    private void startQueryAccountDetails(int token) {
        Log.d(TAG, "startQueryAccountDetails. token=" + token);

        mBackgroundQueryHandler.cancelOperation(token);

        try {
            mBackgroundQueryHandler.startQuery(token, null,
                    AccountColumns.CONTENT_URI,
                    PublicAccount.sDetailProjection, AccountColumns.TABLE + "."
                            + AccountColumns.ID + "=?",
                    new String[] { Long.toString(mAccountId) }, null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case MESSAGE_LIST_QUERY_TOKEN:

                // check consistency between the query result and
                // 'mConversation'
                long accountId = (Long) cookie;

                if (accountId != mAccountId) {
                    Log.d(TAG,
                            "onQueryComplete: msg history query result is for accountId "
                                    + accountId
                                    + ", but mConversation has accountId "
                                    + mAccountId + " starting a new query");
                    if (cursor != null) {
                        cursor.close();
                    }
                    startMsgListQuery();
                    return;
                }

                int newSelectionPos = -1;
                long targetMsgId = mSelectId;
                mSelectId = -1; // clear selected info
                if (targetMsgId != -1) {
                    if (cursor != null) {
                        cursor.moveToPosition(-1);
                        while (cursor.moveToNext()) {
                            long msgId = cursor.getLong(cursor
                                    .getColumnIndexOrThrow(MessageColumns.ID));
                            if (msgId == targetMsgId) {
                                newSelectionPos = cursor.getPosition();
                                break;
                            }
                        }
                    }
                } else if (mSavedScrollPosition != -1) {
                    // mSavedScrollPosition is set when this activity pauses. If
                    // equals maxint,
                    // it means the message list was scrolled to the end.
                    // Meanwhile, messages
                    // could have been received. When the activity resumes and
                    // we were
                    // previously scrolled to the end, jump the list so any new
                    // messages are
                    // visible.
                    if (mSavedScrollPosition == Integer.MAX_VALUE) {
                        int cnt = mMsgListAdapter.getCount();
                        if (cnt > 0) {
                            // Have to wait until the adapter is loaded before
                            // jumping to
                            // the end.
                            newSelectionPos = cnt - 1;
                            mSavedScrollPosition = -1;
                        }
                    } else {
                        // remember the saved scroll position before the
                        // activity is paused.
                        // reset it after the message list query is done
                        newSelectionPos = mSavedScrollPosition;
                        mSavedScrollPosition = -1;
                    }
                }

                if (cursor == null) {
                    Log.d(TAG, "onQueryComplete set cursor is null");
                    return;
                }

                Log.d(TAG, "onQueryComplete set cursor=" + cursor.getCount());
                mMsgListAdapter.changeCursor(cursor);

                if (newSelectionPos != -1) {
                    mMsgListView.setSelection(newSelectionPos); // jump the list
                                                                // to the pos
                } else {
                    int count = mMsgListAdapter.getCount();
                    Log.d(TAG, "onQueryComplete getCount()=" + count);

                    long lastMsgId = 0;
                    if (cursor != null && count > 0) {
                        cursor.moveToLast();
                        lastMsgId = cursor.getLong(cursor
                                .getColumnIndexOrThrow(MessageColumns.ID));
                    }
                    // mScrollOnSend is set when we send a message. We always
                    // want to scroll
                    // the message list to the end when we send a message, but
                    // have to wait
                    // until the DB has changed. We also want to scroll the list
                    // when a
                    // new message has arrived.
                    smoothScrollToEnd(mScrollOnSend
                            || lastMsgId != mLastMessageId, 0);
                    mLastMessageId = lastMsgId;
                    mScrollOnSend = false;
                }
                // Adjust the conversation's message count to match reality. The
                // conversation's message count is eventually used in
                // WorkingMessage.clearConversation to determine whether to
                // delete
                // the conversation or not.
                // mConversation.setMessageCount(mMsgListAdapter.getCount());

                // FIXME: freshing layout changes the focused view to an
                // unexpected
                // one, set it back to TextEditor forcely.
                mTextEditor.requestFocus();

                invalidateOptionsMenu(); // some menu items depend on the
                                         // adapter's count
                return;

            case MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN:
                // check consistency between the query result and
                // 'mConversation'
                accountId = (Long) cookie;

                Log.d(TAG,
                        "onQueryComplete (after delete): msg history result for accountId "
                                + accountId);
                break;

            case ACCOUNT_DETAILS_QUERY_TOKEN:
                Log.d(TAG, "onQueryComplete: account details.");
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int subscribeStatus = cursor
                            .getInt(cursor
                                    .getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
                    int activeStatus = cursor
                            .getInt(cursor
                                    .getColumnIndexOrThrow(AccountColumns.ACTIVE_STATUS));
                    activeStatus = (activeStatus + 1) % 2;
                    updateAccountStatus(subscribeStatus, activeStatus);

                    if (mPath == null || mPath.isEmpty()) {
                        String logoPath = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(AccountColumns.LOGO_PATH));
                        Log.d(TAG, "query logo path from DB:" + logoPath);
                        if (logoPath != null && !logoPath.isEmpty()) {
                            mPath = logoPath;
                            File file = new File(mPath);
                            if (file.exists()) {
                                Log.d(TAG, "update mLogoBitmap");
                                BitmapDrawable bmd = new BitmapDrawable(
                                        getResources(), mPath);
                                synchronized (sLockObj) {
                                    sLogoBitmap = bmd.getBitmap();
                                }
                            }
                        }
                    }
                }
                break;
            case ACCOUNT_DETAILS_QUERY_TOKEN_REFRESH:
                Log.d(TAG, "onQueryComplete: account details for refresh.");
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int subscribeStatus = cursor
                            .getInt(cursor
                                    .getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
                    int activeStatus = cursor
                            .getInt(cursor
                                    .getColumnIndexOrThrow(AccountColumns.ACTIVE_STATUS));
                    int tmpActiveStatus = (activeStatus + 1) % 2;
                    updateAccountStatus(subscribeStatus, tmpActiveStatus);

                    if (subscribeStatus == Constants.SUBSCRIPTION_STATUS_YES
                            && activeStatus != Constants.ACTIVE_STATUS_NORMAL) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                PaComposeActivity.this);
                        builder.setMessage(R.string.unsubscribe_alert_message);
                        builder.setTitle(R.string.unsubscribe_alert_title);
                        builder.setNegativeButton(android.R.string.no, null);
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        mPAService.unsubscribe(mToken, mUuid);
                                        finish();
                                    }
                                });

                        AlertDialog dialog = builder.create();
                        if (!isFinishing()) {
                            dialog.show();
                        } else {
                            Log.d(TAG,
                                    "Activity has finished before alert show !");
                        }
                    }
                }

            default:
                break;

            }
            if (cursor == null) {
                return;
            }
            cursor.close();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            super.onDeleteComplete(token, cookie, result);

            Log.d(TAG, "onDeleteComplete. accountId=" + cookie);

            switch (token) {
            case DELETE_MESSAGE_TOKEN:
                if (cookie instanceof Boolean
                        && ((Boolean) cookie).booleanValue()) {
                    // If we just deleted the last message, reset the saved id.
                    mLastMessageId = 0;
                }
                break;

            default:
                break;
            }
            if (token == DELETE_MESSAGE_TOKEN) {
                // Check to see if we just deleted the last message
                startMsgListQuery(MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN, 0);
            }
        }
    }

    class ProfileServiceHandler implements JoynServiceListener {

        @Override
        public void onServiceConnected() {
            Log.d(TAG, "onServiceConnected()");
            if (mProfileService == null) {
                Log.e(TAG, "mProfileService is null !!!");
            }
            mProfileService.getProfileInfo();
        }

        @Override
        public void onServiceDisconnected(int arg0) {
            Log.d(TAG, "onServiceDisconnected()");
        }

    }

    class MyProfileListener extends ProfileListener {

        @Override
        public void onGetProfile(int result) {
            Log.d(TAG, "onGetProfile:" + result);
            if (result < ProfileService.OK) {
                return;
            }

            getProfileFromDB(false);
        }

        @Override
        public void onUpdateProfile(int result) {
            Log.d(TAG, "onGetProfile:" + result);
            if (result != ProfileService.OK) {
                return;
            }

            getProfileFromDB(false);
        }

        @Override
        public void onUpdateProfilePortrait(int result){
            //yizheng add for build
        }

        @Override
        public void onGetProfilePortrait(int result){
            //yizheng add for build
        }
    }

    private void startQueryMyPortrait() {
        Log.d(TAG, "startQueryMyPortrait()");
        if (mProfileService == null) {
            Log.e(TAG, "mProfileService is null !!!");
        }

        mProfileService.connect();
    }

    private void getProfileFromDB(boolean queryOnly) {
        Log.d(TAG, "getProfileFromDB");

        Uri uri = Uri.parse("content://com.cmcc.ccs.profile");
        String[] keySet = { ProfileService.PORTRAIT,
                ProfileService.PORTRAIT_TYPE };

        Cursor c = getContentResolver().query(uri, keySet, null, null, null);
        if (c == null) {
            Log.d(TAG, "getProfileFromDB. cursor is null");
            return;
        }

        byte[] photo = {};

        if (c.getCount() >= 1) {
            c.moveToFirst();
            String imageType = c.getString(c
                    .getColumnIndex(ProfileService.PORTRAIT_TYPE));
            String photoStr = c.getString(c
                    .getColumnIndex(ProfileService.PORTRAIT));

            if (photoStr != null && !photoStr.isEmpty()) {
                Log.d(TAG, "getProfileFromDB() get valid sender photo");
                photo = Base64.decode(photoStr, Base64.DEFAULT);
            }
        }
        c.close();

        if (!Arrays.equals(sPortrait, photo)) {
            Log.d(TAG, "getProfileFromDB() update sender photo");
            synchronized (sLockObj) {
                sPortrait = photo;
                if (sPortrait.length > 0) {
                    sPortraitBitmap = BitmapFactory.decodeByteArray(sPortrait,
                            0, sPortrait.length);
                } else {
                    sPortraitBitmap = null;
                }
            }
            if (queryOnly) {
                Log.d(TAG, "query only.");
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateForEnable();
                    if (null != mMsgListAdapter) {
                        mMsgListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    class PAServiceCallback extends SimpleServiceCallback {

        @Override
        public void onServiceConnected() throws RemoteException {
            Log.i(TAG, "onServiceConnected.");

            if (isPASrvReady()) {
                // workaround duplicate connect message.
                return;
            }

            changePASrvState(PASRV_STATE_CONNECTED);
            mPAService.getDetails(mToken, mUuid, null);
        }

        @Override
        public void onServiceDisconnected(int reason) throws RemoteException {
            Log.i(TAG, "onServiceDisconnected For reason:" + reason);
            if (reason == PAService.INTERNAL_ERROR) {
                finish();
            } else {
                changePASrvState(PASRV_STATE_INIT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateForEnable();
                    }
                });
            }

        }

        @Override
        public void onServiceRegistered() throws RemoteException {
            Log.i(TAG, "onServiceRegistered.");
            if (!changePASrvState(PASRV_STATE_REGISTED)) {
                Log.d(TAG, "Service already registed, ignore it !");
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateForEnable();
                    // if (null != mMsgListAdapter) {
                    // mMsgListAdapter.notifyDataSetChanged();
                    // }
                }
            });
            updateMenuInfo();
        }

        @Override
        public void onServiceUnregistered() throws RemoteException {
            Log.i(TAG, "onServiceUnregistered");

            if (mPAService.isServiceConnected(mToken)) {
                changePASrvState(PASRV_STATE_CONNECTED);
            } else {
                changePASrvState(PASRV_STATE_INIT);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateForEnable();
                }
            });
        }

        @Override
        public void onNewMessage(long accountId, long messageId)
                throws RemoteException {
            Log.i(TAG, "onNewMessage. accountId=" + accountId + ". messageId="
                    + messageId);
        }

        @Override
        public void onReportMessageFailed(long messageId)
                throws RemoteException {
            Log.i(TAG, "onReportMessageFailed. id=" + messageId);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PaComposeActivity.this,
                            getString(R.string.send_fail_text),
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onReportMessageDisplayed(long messageId)
                throws RemoteException {
            Log.i(TAG, "onReportMessageDisplayed. id=" + messageId);

            // runOnUiThread(new Runnable() {
            // @Override
            // public void run() {
            // mMsgListAdapter.notifyDataSetChanged();
            // }
            // });
        }

        @Override
        public void onReportMessageDelivered(long messageId)
                throws RemoteException {
            Log.i(TAG, "onReportMessageDelivered. id=" + messageId);

            mMsgListAdapter.getSendingQueue().remove(messageId);

            // runOnUiThread(new Runnable() {
            // @Override
            // public void run() {
            // mMsgListAdapter.notifyDataSetChanged();
            // }
            // });
        }

        @Override
        public void onComposingEvent(long accountId, boolean status)
                throws RemoteException {
            Log.i(TAG, "onComposingEvent. sccountid =" + accountId
                    + ". status=" + status);
        }

        @Override
        public void reportUnsubscribeResult(long requestId, int resultCode)
                throws RemoteException {
            Log.i(TAG, "reportUnsubscribeResult. requestId =" + requestId
                    + ". resultCode=" + resultCode);
        }

        @Override
        public void reportGetSubscribedResult(long requestId, int resultCode,
                long[] accountIds) throws RemoteException {
            Log.i(TAG, "reportGetSubscribedResult. requestId =" + requestId
                    + ". resultCode=" + resultCode + ". accountIds="
                    + accountIds);
        }

        @Override
        public void reportGetDetailsResult(long requestId, int resultCode,
                long accountId) throws RemoteException {
            Log.i(TAG, "reportGetDetailsResult. requestId =" + requestId
                    + ". resultCode=" + resultCode + ". accountId=" + accountId);
            if (accountId == mAccountId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startQueryAccountDetails(ACCOUNT_DETAILS_QUERY_TOKEN_REFRESH);
                    }
                });
            }
        }

        @Override
        public void reportGetMenuResult(long requestId, int resultCode)
                throws RemoteException {
            Log.i(TAG, "reportGetMenuResult. requestId =" + requestId
                    + ". resultCode=" + resultCode);

            if (ResultCode.SUCCESS != resultCode) {
                Log.e(TAG, "request[" + requestId
                        + "] reportGetMenuResult error:" + resultCode);
                return;
            }

            if (loadMenuInfo()) {
                loadMenuView();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateBottomPanel(mIsMenuMode);
                    }
                });
            }
        }

        @Override
        public void onTransferProgress(long messageId, long currentSize,
                long totalSize) throws RemoteException {
            Log.i(TAG, "onTransferProgress. messageId =" + messageId
                    + ". currentSize=" + currentSize + ". totalSize="
                    + totalSize);
            if (totalSize > 0 && currentSize <= totalSize) {
                int progress = (int) (currentSize * 100 / totalSize);
                updateSendingProgress(messageId, progress);
            } else if (totalSize > 0 && currentSize > totalSize) {
                updateSendingProgress(messageId, 100);
            }
        }

        @Override
        public void reportDeleteMessageResult(long requestId, int resultCode)
                throws RemoteException {
            Log.i(TAG, "reportDeleteMessageResult. requestId =" + requestId
                    + ". resultCode=" + resultCode);
            if (ResultCode.SUCCESS != resultCode) {
                Toast.makeText(PaComposeActivity.this, R.string.delete_fail,
                        Toast.LENGTH_LONG).show();
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBackgroundQueryHandler.onDeleteComplete(
                            DELETE_MESSAGE_TOKEN, mAccountId, 0);
                }
            });
        }

        @Override
        public void reportSetFavourite(long requestId, int resultCode)
                throws RemoteException {
            Log.i(TAG, "reportSetFavourite. requestId =" + requestId
                    + ". resultCode=" + resultCode);

            if (resultCode != ResultCode.SUCCESS) {
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PaComposeActivity.this,
                            R.string.add_favorite_success, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    private boolean isPASrvReady() {
        if (mPASrvState == PASRV_STATE_REGISTED) {
            return true;
        }
        return false;
    }

    private boolean changePASrvState(int state) {
        Log.d(TAG, "changePaSrvState from " + mPASrvState + " to " + state);
        boolean ret = false;
        if (mPASrvState != state) {
            mPASrvState = state;
            ret = true;
        }
        return ret;
    }

    private void initPAService() {
        Log.d(TAG, "initPAService. state=" + mPASrvState);
        mPAServiceCallback = new PAServiceCallback();
        mPAService = PAService.getInstance();
        if (mPAService != null) {
            mToken = mPAService.registerCallback(mPAServiceCallback, false);
            mPAService.registerAck(mToken);
            Log.d(TAG, "initPAService done. mToken=" + mToken);
        } else {
            changePASrvState(PASRV_STATE_INIT);
            updateForEnable();
            PAService.init(PaComposeActivity.this,
                    new PAService.ServiceConnectNotify() {

                        @Override
                        public void onServiceConnected() {
                            Log.i(TAG, "onServiceConnectedsss");
                            mPAService = PAService.getInstance();
                            mToken = mPAService.registerCallback(
                                    mPAServiceCallback, false);
                            mPAService.registerAck(mToken);
                            Log.d(TAG, "initPAService done. mToken=" + mToken);
                        }
                    });
        }
    }

    private void updateAccountStatus(int subscribeStatus, int activeStatus) {
        Log.d(TAG, "updateAccountStatus:" + subscribeStatus + ","
                + activeStatus);

        int newStatus = subscribeStatus + (activeStatus << 1);

        if (mAccountStatus == newStatus) {
            Log.d(TAG, "updateAccountStatus, state no change:" + mAccountStatus);
            return;
        } else {
            mAccountStatus = newStatus;
            updateForEnable();
            return;
        }
    }

    private boolean isAccountValid() {
        boolean ret = (mAccountStatus & ACCOUNT_STATUS_SUBSCRIBED) > 0
                && (mAccountStatus & ACCOUNT_STATUS_ACTIVITED) > 0;
        Log.d(TAG, "isAccountValid:" + ret);
        return ret;
    }

    private boolean isAccountActive() {
        return (mAccountStatus & ACCOUNT_STATUS_ACTIVITED) > 0;
    }

    private boolean isAccountSubscribed() {
        return (mAccountStatus & ACCOUNT_STATUS_SUBSCRIBED) > 0;
    }

    // --------------- MTK ADD for Public account ------------

    private void updateBottomPanel(boolean showMenu) {

        Log.d(TAG, "UpdateBottomPanle mode=" + showMenu);
        mIsMenuMode = showMenu;

        if (0 == mMenuSize) {
            mIsMenuMode = false;
            mInputSwitchLayout.setVisibility(View.GONE);
        } else {
            mInputSwitchLayout.setVisibility(View.VISIBLE);
        }

        if (mIsMenuMode) {
            mMenuContainerLayout.setVisibility(View.VISIBLE);
            mInputContainerLayout.setVisibility(View.GONE);
            hideKeyboard();
        } else {
            mMenuContainerLayout.setVisibility(View.GONE);
            mInputContainerLayout.setVisibility(View.VISIBLE);
        }
    }

    private void updateMenuInfo() {
        Log.d(TAG, "updateMenuInfo(). timeStamp=" + mMenuTimeStamp);
        mPAService.getMenu(mToken, mUuid, mMenuTimeStamp);
    }

    private boolean loadMenuInfo() {
        Log.d(TAG, "loadMenuInfo() begin");
        MenuInfo tempInfo = new MenuInfo();
        boolean ret = tempInfo.loadFromContentProvider(getContentResolver(),
                mAccountId);
        Log.i(TAG, " loadMenuInfo(). ret=" + ret + ". timestamp="
                + tempInfo.timestamp);
        Log.i(TAG, " loadMenuInfo(). size=" + tempInfo.menu.size());

        if (ret && (tempInfo.menu.size() > 0)) { // &&
            // (tempInfo.timestamp != mMenuInfo.timestamp)) {
            mMenuInfo = tempInfo;
            mMenuSize = tempInfo.menu.size();
            mMenuTimeStamp = Utils.covertTimestampToString(tempInfo.timestamp);
            Log.d(TAG, "loadMenuInfo() changed. size=" + mMenuSize
                    + ". timestamp=" + mMenuInfo.timestamp);
            return true;
        }
        return false;
    }

    private void loadMenuView() {

        if (null == mMenuInfo) {
            Log.e(TAG, "LoadMenuView(): mMenuInfo is null !!!");
            return;
        }
        Log.i(TAG, "LoadMenuView() size = " + mMenuSize);

        // hide/ buttons
        for (int i = 0; i < mMenuSize; i++) {
            mMenuOutters[i].setVisibility(View.VISIBLE);
        }
        for (int i = mMenuSize; i < mMenuButtons.length; ++i) {
            mMenuOutters[i].setVisibility(View.GONE);
        }

        for (int i = 0; i < mMenuSize; ++i) {
            final MenuEntry menuEntry = mMenuInfo.menu.get(i);
            final Button button = mMenuButtons[i];
            int subMenuSize = menuEntry.subMenuItems.size();
            Log.i(TAG, "Menu[" + i + "] subItemSize=" + subMenuSize);
            if (subMenuSize > 0) {
                button.setText(menuEntry.title);
                button.setOnClickListener(new MenuItemOnClick(button,
                        menuEntry, false));
            } else {
                button.setText(menuEntry.title);
                button.setOnClickListener(new MenuItemOnClick(button,
                        menuEntry, true));
            }
        }
    }

    private class MenuItemOnClick implements View.OnClickListener {
        private View mView;
        private MenuEntry mMenuEntry;
        private boolean mIsSubMenu;

        public MenuItemOnClick(View view, MenuEntry menuItem, boolean isSubMenu) {
            mView = view;
            mMenuEntry = menuItem;
            mIsSubMenu = isSubMenu;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "menu onClick. isSub=" + mIsSubMenu);
            if (mIsSubMenu) {
                doOperation(mMenuEntry);
            } else {
                buildPopupMenu(mView, mMenuEntry).show();
            }
        }
    }

    private PopupMenu buildPopupMenu(View view, final MenuEntry menuItem) {
        PopupMenu popup = new PopupMenu(PaComposeActivity.this, view);

        for (int i = 0; i < menuItem.subMenuItems.size(); ++i) {
            popup.getMenu().add(1, i, i, menuItem.subMenuItems.get(i).title);
        }

        popup.setOnMenuItemClickListener(new SubMenuItemOnClick(menuItem));

        return popup;
    }

    private class SubMenuItemOnClick implements
            PopupMenu.OnMenuItemClickListener {
        MenuEntry mMenuEntry;

        public SubMenuItemOnClick(MenuEntry menuEntry) {
            mMenuEntry = menuEntry;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            doOperation(mMenuEntry.subMenuItems.get(item.getItemId()));
            return false;
        }

    }

    private void doOperation(MenuEntry menuEntry) {
        if (menuEntry.type == 0) {
            Log.i(TAG, "Send menu command: " + menuEntry.commandId);
            if (isPASrvReady() && isAccountValid()) {
                mPAService.sendMessage(mToken, mAccountId, menuEntry.commandId,
                        true);
            } else {
                if (!isPASrvReady()) {
                    Toast.makeText(PaComposeActivity.this,
                            R.string.service_not_ready, Toast.LENGTH_SHORT)
                            .show();
                } else if (!isAccountValid()) {
                    if (!isAccountSubscribed()) {
                        Toast.makeText(PaComposeActivity.this,
                                R.string.account_not_subscribed,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PaComposeActivity.this,
                                R.string.account_stopped, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        } else if (menuEntry.type == 1) {
            PaWebViewActivity.openHyperLink(PaComposeActivity.this,
                    menuEntry.commandId);
        }
    }

    @Override
    public boolean onScaleStart(ScaleDetector detector) {
        Log.d(TAG, "onScaleStart -> mTextSize = " + mTextSize);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleDetector detector) {
        Log.d(TAG, "onScaleEnd -> mTextSize = " + mTextSize);

        if (mTextSize < MIN_TEXT_SIZE) {
            mTextSize = MIN_TEXT_SIZE;
        } else if (mTextSize > MAX_TEXT_SIZE) {
            mTextSize = MAX_TEXT_SIZE;
        }

        try {
            Context context = createPackageContext("com.android.mms",
                    Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sp = context.getSharedPreferences(
                    "com.android.mms_preferences", Context.MODE_WORLD_WRITEABLE
                            | Context.MODE_MULTI_PROCESS);
            SharedPreferences.Editor editor = sp.edit();
            editor.putFloat("message_font_size", mTextSize);
            editor.commit();
            Log.d(TAG, "set SharedPreferences:" + mTextSize);
        } catch (NameNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onScale(ScaleDetector detector) {
        float size = mTextSize * detector.getScaleFactor();

        if (Math.abs(size - mTextSize) < MIN_ADJUST_TEXT_SIZE) {
            return false;
        }
        if (size < MIN_TEXT_SIZE) {
            size = MIN_TEXT_SIZE;
        }
        if (size > MAX_TEXT_SIZE) {
            size = MAX_TEXT_SIZE;
        }
        if ((size - mTextSize > 0.0000001) || (size - mTextSize < -0.0000001)) {
            mTextSize = size;
            setTextSize(size);
        }
        return true;
    }

}
