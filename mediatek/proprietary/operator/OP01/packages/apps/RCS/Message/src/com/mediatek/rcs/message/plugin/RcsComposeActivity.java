/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */
package com.mediatek.rcs.message.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore.Audio;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.data.Conversation;
import com.android.mms.ui.ComposeMessageActivity;
import com.cmcc.ccs.blacklist.CCSblacklist;
import com.cmcc.ccs.chat.ChatMessage;
import com.cmcc.ccs.chat.ChatService;
import com.mediatek.mms.callback.IComposeActivityCallback;
import com.mediatek.mms.callback.IWorkingMessageCallback;
import com.mediatek.mms.ipmessage.DefaultIpComposeActivityExt;
import com.mediatek.mms.ipmessage.IIpContactExt;
import com.mediatek.mms.ipmessage.IIpConversationExt;
import com.mediatek.mms.ipmessage.IIpMessageItemExt;
import com.mediatek.mms.ipmessage.IIpMessageListAdapterExt;
import com.mediatek.rcs.message.ui.SharePanel;
import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.GroupManager.IInitGroupListener;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpEmoticonMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.IpMessageConsts.GroupActionList;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.ISpamReportListener;
import com.mediatek.rcs.common.IFileSpamReportListener;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RCSGroup;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.EmojiShop.OnLoadExpressionListener;
import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.MemberInfo;
import com.mediatek.rcs.message.location.GeoLocService;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.plugin.RCSEmoji;
import com.mediatek.rcs.message.provider.FavoriteMsgProvider;
import com.mediatek.rcs.message.ui.CreateGroupActivity;
import com.mediatek.rcs.message.ui.RcsSettingsActivity;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.utils.ThreadNumberCache;
import com.mediatek.rcs.message.R;
import com.mediatek.services.rcs.phone.ICallStatusService;
import com.mediatek.services.rcs.phone.IServiceMessageCallback;
import com.mediatek.storage.StorageManagerEx;
import com.mediatek.telecom.TelecomManagerEx;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.OnServiceChangedListener;
import com.mediatek.rcs.common.provider.FavoriteMsgData;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.RCSCacheManager;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;

import com.mediatek.rcs.message.plugin.RCSVCardAttachment;
import com.mediatek.rcs.message.plugin.RcsMessageListAdapter.OnMessageListChangedListener;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.IBurnMessageCapabilityListener;
import android.provider.Telephony.Sms;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;


/**
 * class RcsComposeActivity, plugin implements response ComposeMessageActivity.
 *
 */
public class RcsComposeActivity extends DefaultIpComposeActivityExt implements
        IBurnMessageCapabilityListener, OnServiceChangedListener,
        ISpamReportListener, IFileSpamReportListener, OnLoadExpressionListener {

    private static final String TAG = "RcsComposeActivity";

    // / M: host's option menu and context menu
    private static final int MENU_ADD_SUBJECT           = 0;
    private static final int MENU_ADD_ATTACHMENT        = 2;
    private static final int MENU_CALL_RECIPIENT        = 5;
    private static final int MENU_GROUP_PARTICIPANTS    = 32;
    private static final int MENU_SELECT_MESSAGE        = 101;
    private static final int MENU_SHOW_CONTACT          = 121;
    private static final int MENU_CREATE_CONTACT        = 122;
    private static final int MENU_CHAT_SETTING          = 137;
    //rcs message new option menu item from 5000;
    private static final int MENU_GROUP_CHAT_INFO = 5000;
    private static final int MENU_EDIT_BURNED_MSG = 5001;

    // / M: context menu
    private static final int MENU_DELIVERY_REPORT = 20;
    private static final int MENU_FORWARD_MESSAGE = 21;
    private static final int MENU_SAVE_MESSAGE_TO_SUB = 32;
    private static final int MENU_IPMSG_DELIVERY_REPORT   = 33;
    private static final int MENU_SELECT_TEXT = 36;
    private static final int MENU_INVITE_FRIENDS_TO_CHAT = 100;
    private static final int MENU_RETRY = 200;
    private static final int MENU_COPY = 207;
    private static final int MENU_EXPORT_SD_CARD = 209;
    private static final int MENU_FORWARD_IPMESSAGE = 210;
    private static final int MENU_FAVORITE = 211;
    private static final int MENU_BLACK_LIST = 214;
    private static final int MENU_REPORT = 1000;

    private static final int MENU_ADD_TO_BOOKMARK = 35;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 27;

    public static final int OK = 0;
    public static final int UPDATE_SENDBUTTON = 2;

    private static final String PREFERENCE_SEND_WAY_CHANGED_NAME = "sendway";
    private static final String PREFERENCE_KEY_SEND_WAY = "sendway";
    private static final int PREFERENCE_VALUE_SEND_WAY_UNKNOWN = 0;
    private static final int PREFERENCE_VALUE_SEND_WAY_IM = 1;
    private static final int PREFERENCE_VALUE_SEND_WAY_SMS = 2;
    private static final String PREFERENCE_KEY_SEND_WAY_IM = "sendway_im";
    private static final String PREFERENCE_KEY_SEND_WAY_SMS = "sendway_sms";

    private ImageButton mSendButtonIpMessage; // press to send ipmessage
    private TextView mTypingStatus;
    private TextView mShowCallTimeText;

    // chat mode number
    private String mChatModeNumber = "";
    private boolean mIsSmsEnabled;

    private static final int REQUEST_CODE_IPMSG_TAKE_PHOTO = 200;
    private static final int REQUEST_CODE_IPMSG_RECORD_VIDEO = 201;
    private static final int REQUEST_CODE_IPMSG_SHARE_CONTACT = 203;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_PHOTO = 204;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_VIDEO = 205;
    private static final int REQUEST_CODE_IPMSG_RECORD_AUDIO = 206;
    private static final int REQUEST_CODE_IPMSG_CHOOSE_AUDIO = 208;
    private static final int REQUEST_CODE_IPMSG_SHARE_VCALENDAR = 209;
    private static final int REQUEST_CODE_INVITE_FRIENDS_TO_CHAT = 210;
    private static final int REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO = 212;
    private static final int REQUEST_CODE_START_RCSSETTING_ACTIVITY = 213;

    // / M: IP message
    public static final int IPMSG_TAKE_PHOTO = 100;
    public static final int IPMSG_RECORD_VIDEO = 101;
    public static final int IPMSG_RECORD_AUDIO = 102;
    public static final int IPMSG_CHOOSE_PHOTO = 104;
    public static final int IPMSG_CHOOSE_VIDEO = 105;
    public static final int IPMSG_CHOOSE_AUDIO = 106;
    public static final int IPMSG_SHARE_CONTACT = 108;
    public static final int IPMSG_SHARE_CALENDAR = 109;
    public static final int IPMSG_SHARE_POSITION = 110;

    // send or received ipMsg, don't change the value
    public static final int DIRECTION_OUTGOING = 1;

    private static final int ACTION_SHARE = 0;
    private static final int ACTION_RCS_SHARE = 1;
    public static final String SHARE_ACTION = "shareAction";

    private boolean mShowKeyBoardFromShare = false;
    private boolean mIsSoftKeyBoardShow = false;
    private boolean mIsLandscape = false;
    private Object mWaitingImeChangedObject = new Object();

    private static final int mReferencedTextEditorTwoLinesHeight = 65;
    private static final int mReferencedTextEditorThreeLinesHeight = 110;
    private static final int mReferencedTextEditorFourLinesHeight    = 140;
    private static final int mReferencedTextEditorSevenLinesHeight = 224;

    private static final int ADD_IMAGE               = 0;
    private static final int TAKE_PICTURE            = 1;
    private static final int ADD_VIDEO               = 2;
    private static final int RECORD_VIDEO            = 3;
    private static final int ADD_SOUND               = 4;
    private static final int RECORD_SOUND            = 5;
    private static final int ADD_SLIDESHOW           = 6;
    private static final int ADD_VCARD               = 7;
    private static final int ADD_VCALENDAR           = 8;

    public static final int RESULT_CANCELLED = 0;
    private String mIpMessageVcardName = "";
    private String mPhotoFilePath = "";
    private String mVideoFilePath = "";
    private String mAudioPath = "";

    private String mDstPath = "";
    private int mDuration = 0;
    private EditText mTextEditor;
    private TextWatcher mEditorWatcher;
    private TextView mTextCounter;
    private Handler mUiHandler;

    private ImageButton mShareButton;
    private SharePanel mSharePanel;

    private Activity mContext;
    private Context mPluginContext;
    private IComposeActivityCallback mCallback;
    private IWorkingMessageCallback mWorkingMessage;
    private boolean mDisplayBurned = false;
    private boolean mBurnedCapbility = false;
    private long mThreadId = 0;
    private String mOldcontact = null;
    private boolean mRegistCapbility = false;
    private boolean mIsGroupChat;
    private String mGroupChatId;
    private boolean mIsChatActive = true;
    private View mBottomPanel;
    private View mEditZone;
    private RCSEmoji mEmoji;
    private ListView mListView;
    private RcsMessageListAdapter mRcsMessageListAdapter;
    private final static int DIALOG_ID_GETLOC_PROGRESS = 101;
    private final static int DIALOG_ID_GEOLOC_SEND_CONFRIM = 102;
    private final static int DIALOG_ID_NOTIFY_SEND_BY_RCS = 103;

    private static Location mLocation = null;
    private GeoLocService mGeoLocSrv = null;
    private RCSGroup mGroup;
    private int mGroupStatus;
    private Dialog mInvitationDialog;
    private Dialog mInvitationExpiredDialog;
    private ProgressDialog mProgressDialog;
    private boolean mIsNeedShowCallTime;
    private ICallStatusService mCallStatusService;
    private static final String RCS_MODE_FLAG = "rcsmode";
    private static final int RCS_MODE_VALUE = 1;
    private ServiceConnection mCallConnection = null;
    private int mSendSubId = 0;
    private RCSMessageManager mRcsMessageManager;

    final private int[] mUnusedOptionMenuForGroup = { MENU_ADD_SUBJECT, MENU_CHAT_SETTING,
            MENU_GROUP_PARTICIPANTS, MENU_SHOW_CONTACT, MENU_CREATE_CONTACT };

    private static List<RcsComposeActivity> sComposerList = new ArrayList<RcsComposeActivity>(2);
    private RCSServiceManager mServiceManager;
    private boolean mServiceEnabled = false;
    private boolean mServiceRegistered = false;
    private boolean mIsRcsMode = false;

    private long mFileTransferMaxSize = 0;
    /**
     * Construction.
     * @param context Context
     */
    public RcsComposeActivity(Context context) {
        mPluginContext = context;
        mRcsMessageManager = RCSMessageManager.getInstance();
    }

    @Override
    public void onRequestBurnMessageCapabilityResult(String contact, boolean result) {

        String number = getNumber();
        Log.d(TAG, "onRequestBurnMessageCapabilityResult contact = " + contact + " result = "
                + result + " number = " + number);
        if (PhoneNumberUtils.compare(contact, number)) {
            Log.d(TAG,"onRequestBurnMessageCapabilityResult contact = number ");
            mBurnedCapbility = result;
            if (mDisplayBurned && !mBurnedCapbility) {
                checkBurnedMsgCapbility();
            }
            // mDisplayBurned = result;
        } else {
            mBurnedCapbility = false;
        }
        Log.d(TAG, "onRequestBurnMessageCapabilityResult mBurnedCapbility = "
                + mBurnedCapbility);
        return;
    }

    @Override
    public void onSpamReportResult(String contact, String msgId, final int errorcode) {
        Log.d(TAG, "[spam-report] onSpamReportResult contact: "+contact+
              " msgId: "+msgId+ " errorcode: "+errorcode  );
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errorcode == 200) {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.spam_report_success),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.spam_report_fail),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onFileSpamReportResult(String contact, String msgId, final int errorcode) {
        Log.d(TAG, "[spam-report] onFileSpamReportResult contact: "+contact+
              " msgId: "+msgId+ " errorcode: "+errorcode  );
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errorcode == 200) {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.spam_report_success),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.spam_report_fail),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onIpComposeActivityCreate(Activity context,
            IComposeActivityCallback callback, Handler handler, Handler uiHandler,
            ImageButton sendButton, TextView typingTextView, TextView strangerTextView,
            View bottomPanel,Bundle bundle,
            ImageButton shareButton, LinearLayout panel,  EditText textEditor) {
        Log.d(TAG, "onIpComposeActivityCreate enter. sendButton = " + sendButton);
        mContext = context;
        mCallback = callback;
        mUiHandler = handler;
        mSendButtonIpMessage = sendButton;
        mTypingStatus = typingTextView;
        mShowCallTimeText = strangerTextView;
        mBottomPanel = bottomPanel;
        mEditZone = (View) mBottomPanel.getParent();
        mServiceManager = RCSServiceManager.getInstance();
        mServiceEnabled = mServiceManager.isServiceEnabled();
        if (mServiceManager.getServiceState() == RCSServiceManager.RCS_SERVICE_STATE_CONNECTTED) {
            mServiceRegistered = mServiceManager.serviceIsReady();
        }
        mServiceManager.addOnServiceChangedListener(this);
        mServiceManager.registSpamReportListener(this);
        mServiceManager.registFileSpamReportListener(this);
        EmojiShop.addOnLoadExpressionListener(this);
        // / M: add for ip emoji
        if (mServiceManager.isServiceEnabled() && mEmoji == null) {
            mEmoji = new RCSEmoji(context, (ViewParent) mBottomPanel, this);
        }

        Log.d(TAG, "mServiceEnabled =" + mServiceEnabled);
        mContext.registerReceiver(mChangeSubReceiver, new IntentFilter(
                TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED));
        sComposerList.add(this);
        if (bundle != null && bundle.getBoolean("burned_mode", false)) {
            Log.d(TAG, "[BurnedMsg] initBurnedMode() mDisplayBurned");
            initBurnedMode(bundle);
        }
        mShareButton = shareButton;
        mShareButton.setVisibility(View.VISIBLE);
        mSharePanel = new SharePanel(mPluginContext, this);
        panel.addView(mSharePanel);
        /// sharepanel
        initShareResource();
        return false;
    }
    private void initBurnedMode(Bundle bundle) {
        boolean hasBurnedCap = bundle.getBoolean("burned_cap", false);
        Log.d(TAG, "[BurnedMsg] initBurnedMode() before mDisplayBurned: " + mDisplayBurned
                + " hasBurnedCap: " + hasBurnedCap);
        mDisplayBurned = true;
        mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                .getDrawable(R.drawable.ic_send_ipbar));
        //mOldcontact = null;
        mSendButtonIpMessage.setClickable(true);
        Log.d(TAG, "[BurnedMsg] initBurnedMode() after mDisplayBurned: " + mDisplayBurned);
        RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
//        mCallback.resetSharePanel();
        mSharePanel.resetShareItem();
        if (hasBurnedCap) {
            mBurnedCapbility = true;
            mOldcontact = bundle.getString("burned_contact");
        }
    }

    @Override
    public boolean onIpComposeActivityResume(boolean isSmsEnabled, EditText textEditor,
            TextWatcher watcher, TextView textCounter,
            View recipientsEditor, View subjectTextEditor) {
        invalidateParticipantsPortrait();
        String[] numbers = mCallback.getConversationInfo();
        mIsSmsEnabled = isSmsEnabled;
        mTextEditor = textEditor;
        mEditorWatcher = watcher;
        mTextCounter = textCounter;
        /* hide the regist operation
        Log.d(TAG, "onIpComposeActivityResume: mRegistCapbility = " + mRegistCapbility
                           + "mServiceEnabled = " + mServiceEnabled);
        if (!mRegistCapbility) {
            mServiceManager.registBurnMsgCapListener(this);
            mRegistCapbility = true;
        }
        */
        if ((isViewVisible(recipientsEditor) && recipientsEditor.hasFocus())
                || (isViewVisible(subjectTextEditor) && subjectTextEditor.hasFocus())
                || (isViewVisible(mTextEditor) && mTextEditor.hasFocus())) {
            showSharePanel(false);
        }

        if (numbers != null && numbers.length > 0 && numbers[0].length() > 0 || mIsGroupChat) {
            mShareButton.setClickable(true);
            mShareButton.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ipmsg_share));
        } else {
            mShareButton.setClickable(false);
            mShareButton.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ipmsg_share_disable));
        }
        if (mServiceEnabled && mEmoji != null) {
            mEmoji.setEmojiEditor(textEditor);
            if (mIsGroupChat) {
                mEmoji.setGroupInfo(true, mGroupChatId);
            } else {
                mEmoji.setGroupInfo(false, null);
            }
        }

        if (mIsGroupChat) {
            mCallback.hideIpRecipientEditor();
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mGroupChatId);
            if (info != null
                    && info.getStatus() == GroupActionList.GROUP_STATUS_INVALID) {
                Log.d(TAG, "onIpComposeActivityResume: set chat inactive");
                setChatActive(false);
            } else if (info == null) {
                mContext.finish();
            }
        } else if (mIsNeedShowCallTime) {
            mCallback.hideIpRecipientEditor();
            mCallback.updateIpTitle();
        }

//        initDualSimState();
        return true;
    }

    @Override
    public boolean onIpComposeActivityPause() {
        Log.d(TAG, "onIpComposeActivityPause enter. mServiceEnabled = "
                + mServiceEnabled);
        // / M: add for ip message, notification listener
        invalidateParticipantsPortrait();
        return true;
    }

    private void invalidateParticipantsPortrait() {
        if (!mIsGroupChat) {
            List<String> numbers = getRecipientsList();
            if (numbers != null && numbers.size() == 1) {
                PortraitManager.getInstance().invalidatePortrait(numbers.get(0));
            }
        } else {
            PortraitManager.getInstance().invalidateGroupPortrait(mGroupChatId);
        }
    }

    @Override
    public boolean onIpComposeActivityDestroy() {
        Log.d(TAG, "onIpComposeActivityDestroy ");
        if (mIsGroupChat) {
            GroupManager.getInstance().removeGroupListener(mInitGroupListener);
            mGroup.removeActionListener(mGroupListener);
            GroupManager.getInstance().releaseRCSGroup(mGroupChatId);
        }
        mContext.unregisterReceiver(mChangeSubReceiver);
        if (mRcsMessageListAdapter != null) {
            mRcsMessageListAdapter.removeOnMessageListChangedListener(mMsgListChangedListener);
        }
        mServiceManager.removeOnServiceChangedListener(this);
        mServiceManager.unregistBurnMsgCapListener(this);
        Log.d(TAG, "[spam-report] onIpComposeActivityDestroy ");
        mServiceManager.unregistSpamReportListener(this);
        mServiceManager.unregistFileSpamReportListener(this);
        EmojiShop.removeOnLoadExpressionListener(this);
        mRegistCapbility = false;
        mDisplayBurned = false;
        RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
        sComposerList.remove(this);
        RCSCacheManager.clearCache();
        if (mEmoji != null) {
            mEmoji.unInit();
            mEmoji = null;
        }
        return false;
    }

    @Override
    public boolean onIpUpdateCounter(CharSequence text, int start, int before, int count) {
        Log.d(TAG, "onIpUpdateCounter text =" + text + " count =" + count);
        if (isRcsMode()) {
            return true;
        }
        return super.onIpUpdateCounter(text, start, before, count);
    }

    @Override
    public boolean onIpDeleteMessageListenerClick(IIpMessageItemExt ipMsgItem) {
        RcsMessageItem item = (RcsMessageItem) ipMsgItem;
        Log.d(TAG, "onIpDeleteMessageListenerClick type = " + item.mType + ", id = " + item.mMsgId);
        if (item.mType.equals("rcs")) {
            mRcsMessageManager.deleteRCSMessage(item.mMsgId);
        }
        return super.onIpDeleteMessageListenerClick(ipMsgItem);
    }

    @Override
    public boolean onIpCreateContextMenu(ContextMenu menu, boolean isSmsEnabled,
            boolean isForwardEnabled, IIpMessageItemExt ipMsgItem) {
        Log.d(TAG, "onIpCreateContextMenu ipMsgItem =" + ipMsgItem);
        Context pluginContext = mPluginContext;
        if (ipMsgItem == null) {
            return false;
        }
        RcsMessageItem rcsMsgItem = (RcsMessageItem) ipMsgItem;
//        long ipMessageId = rcsMsgItem.mIpMessageId;
        long msgId = rcsMsgItem.mMsgId;
        String type = rcsMsgItem.mType;
        IpMessage ipMessage = rcsMsgItem.mIpMessage;
        RcsMsgListMenuClickListener l = new RcsMsgListMenuClickListener(ipMsgItem);
        if (isSmsEnabled && type.endsWith("rcs")) {
            MenuItem item = null;
            if (mServiceEnabled) {
                if (rcsMsgItem.mRcsStatus == RcsLog.MessageStatus.FAILED) {
                    if (mIsChatActive && mServiceRegistered) {
                        menu.add(0, MENU_RETRY, 0,
                                pluginContext.getString(R.string.ipmsg_resend))
                                .setOnMenuItemClickListener(l);
                    }
                }
                if (rcsMsgItem.mRcsDirection == RcsLog.Direction.OUTGOING) {
                    item = menu.findItem(MENU_DELIVERY_REPORT);
                    if (item != null) {
                        menu.removeItem(MENU_DELIVERY_REPORT);
                    }
                    menu.add(0, MENU_IPMSG_DELIVERY_REPORT, 0,
                            pluginContext.getString(R.string.ipmsg_delivery_report))
                            .setOnMenuItemClickListener(l);
                }
                item = menu.findItem(MENU_SAVE_MESSAGE_TO_SUB);
                if (item != null) {
                    menu.removeItem(MENU_SAVE_MESSAGE_TO_SUB);
                }

                if (rcsMsgItem.isNormalTextMessage()) {
                    item = menu.findItem(MENU_COPY);
                    if (item == null) {
                        menu.add(0, MENU_COPY, 0, pluginContext.getString(R.string.ipmsg_copy))
                                .setOnMenuItemClickListener(l);
                    }
                    menu.add(0, MENU_FAVORITE, 0,
                            pluginContext.getString(R.string.menu_favorite))
                            .setOnMenuItemClickListener(l);
                } else if (ipMessage.getType() == IpMessageType.PICTURE
                        || ipMessage.getType() == IpMessageType.VIDEO
                        || ipMessage.getType() == IpMessageType.VOICE
                        || ipMessage.getType() == IpMessageType.VCARD
                        || ipMessage.getType() == IpMessageType.GEOLOC
                        || ipMessage.getType() == IpMessageType.EMOTICON) {
                    if (((ipMessage.getStatus() == MessageStatus.UNREAD
                            || ipMessage.getStatus() == MessageStatus.READ) &&
                            ((IpAttachMessage) ipMessage).getRcsStatus() != Status.FINISHED)) {
                        Log.d(TAG, "this is a undownload FT");
                    } else {
                        if (!ipMessage.getBurnedMessage()) {
                            menu.add(0, MENU_EXPORT_SD_CARD, 0,
                                    pluginContext.getString(R.string.copy_to_sdcard))
                                    .setOnMenuItemClickListener(l);
                            menu.add(0, MENU_FAVORITE, 0,
                                    pluginContext.getString(R.string.menu_favorite))
                                    .setOnMenuItemClickListener(l);
                        }
                    }

                    item = menu.findItem(MENU_COPY);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SELECT_TEXT);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SAVE_MESSAGE_TO_SUB);
                    if (item != null) {
                        menu.removeItem(MENU_SAVE_MESSAGE_TO_SUB);
                    }

                    item = menu.findItem(MENU_ADD_TO_BOOKMARK);
                    if (item != null) {
                        menu.removeItem(MENU_ADD_TO_BOOKMARK);
                    }

                    item = menu.findItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                    while (item != null) {
                        menu.removeItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                        item = menu.findItem(MENU_ADD_ADDRESS_TO_CONTACTS);
                    }

                } else {
                    // TODO: not text
                    item = menu.findItem(MENU_COPY);
                    if (item != null) {
                        item.setVisible(false);
                    }

                    item = menu.findItem(MENU_SELECT_TEXT);
                    if (item != null) {
                        item.setVisible(false);
                    }
                }
                // hide the report menu ,because cmcc requirement is not clear.
                //menu.add(0, MENU_REPORT, 0,
                //pluginContext.getString(R.string.ipmsg_report))
                // .setOnMenuItemClickListener(l);

            }
        }

        if ("mms".equals(rcsMsgItem.mType)) {
            Log.d(TAG, "rcsMsgItem.mBoxId =" + rcsMsgItem.mBoxId);
            if (rcsMsgItem.mBoxId == Mms.MESSAGE_BOX_INBOX
                    && rcsMsgItem.mMessageType == MESSAGE_TYPE_NOTIFICATION_IND) {
                Log.d(TAG, "this is a mms,rcsMsgItem.mBoxId =" + rcsMsgItem.mBoxId);
            } else {
                menu.add(0, MENU_FAVORITE, 0, pluginContext.getString(R.string.menu_favorite))
                        .setOnMenuItemClickListener(l);
            }
        } else if ("sms".equals(rcsMsgItem.mType)) {
            menu.add(0, MENU_FAVORITE, 0, pluginContext.getString(R.string.menu_favorite))
            .setOnMenuItemClickListener(l);
        }

        int forwardMenuId = forwardMsgHandler(rcsMsgItem);
        if (MENU_FORWARD_IPMESSAGE == forwardMenuId) {
            MenuItem item = menu.findItem(MENU_FORWARD_MESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }
            menu.add(0, MENU_FORWARD_IPMESSAGE, 0,
                    pluginContext.getString(R.string.ipmsg_forward))
                    .setOnMenuItemClickListener(l);
        } else if (0 == forwardMenuId) {
            MenuItem item = null;
            item = menu.findItem(MENU_FORWARD_MESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }

            item = menu.findItem(MENU_FORWARD_IPMESSAGE);
            if (item != null) {
                menu.removeItem(MENU_FORWARD_IPMESSAGE);
            }
        }
        if (ipMessage != null && ipMessage.getBurnedMessage()) {
            MenuItem itemBurned = menu.findItem(MENU_COPY);
            if (itemBurned != null) {
                menu.removeItem(MENU_COPY);
            }

            itemBurned = menu.findItem(MENU_SELECT_TEXT);
            if (itemBurned != null) {
                menu.removeItem(MENU_SELECT_TEXT);
            }

            itemBurned = menu.findItem(MENU_FORWARD_IPMESSAGE);
            if (itemBurned != null) {
                menu.removeItem(MENU_FORWARD_IPMESSAGE);
            }
            itemBurned = menu.findItem(MENU_FORWARD_MESSAGE);
            if (itemBurned != null) {
                menu.removeItem(MENU_FORWARD_MESSAGE);
            }
            itemBurned = menu.findItem(MENU_IPMSG_DELIVERY_REPORT);
            if (itemBurned != null) {
                menu.removeItem(MENU_IPMSG_DELIVERY_REPORT);
            }
            itemBurned = menu.findItem(MENU_REPORT);
            if (itemBurned != null) {
                menu.removeItem(MENU_REPORT);
            }
        }
        return true;
    }

    private int forwardMsgHandler(RcsMessageItem rcsMsgItem) {
        Intent sendIntent;
//        long ipMessageId = rcsMsgItem.mIpMessageId;
        IpMessage ipMessage = mRcsMessageManager.getRCSMessageInfo(rcsMsgItem.mMsgId);
        Log.d(TAG, "forwardMsgHandler()  mType =" + rcsMsgItem.mType + " mMsgid = "
                + rcsMsgItem.mMsgId);
        if ("sms".equals(rcsMsgItem.mType)) {
            // sms forward
            sendIntent = RcsMessageUtils.createForwordIntentFromSms(mContext,
                    rcsMsgItem.mBody);
            if (sendIntent != null) {
                return MENU_FORWARD_IPMESSAGE;
            } else {
                return MENU_FORWARD_MESSAGE;
            }
        } else if ("rcs".equals(rcsMsgItem.mType)) {
            // ip message forward
            if (ipMessage == null) {
                return 0;
            }
            sendIntent = RcsMessageUtils.createForwordIntentFromIpmessage(mContext, ipMessage);
            Log.d(TAG,
                    "forwardMsgHandler()  sendIntent is null   getType = "
                            + ipMessage.getType());
            if ((ipMessage.getType() == IpMessageType.TEXT) ||
                (ipMessage.getType() == IpMessageType.EMOTICON)) {
                if (sendIntent != null) {
                    Log.d(TAG, "forwardMsgHandler()  sendIntent != null");
                    return MENU_FORWARD_IPMESSAGE;
                } else {
                    return MENU_FORWARD_MESSAGE;
                }
            }

            if ((ipMessage.getType() == IpMessageType.PICTURE
                    || ipMessage.getType() == IpMessageType.VIDEO
                    || ipMessage.getType() == IpMessageType.VOICE
                    || ipMessage.getType() == IpMessageType.VCARD
                    || ipMessage.getType() == IpMessageType.GEOLOC)) {
                Log.d(TAG, "forwardMsgHandler()  sendIntent is null   getStatus = "
                        + ipMessage.getStatus() + " getRcsStatus() = "
                        + ((IpAttachMessage) ipMessage).getRcsStatus());
                if (((ipMessage.getStatus() == MessageStatus.UNREAD
                        || ipMessage.getStatus() == MessageStatus.READ) &&
                        ((IpAttachMessage) ipMessage).getRcsStatus() != Status.FINISHED)) {
                    return 0;
                } else {
                    if (!ipMessage.getBurnedMessage()) {
                        return MENU_FORWARD_IPMESSAGE;
                    }
                    return 0;
                }
            }
        } else if ("mms".equals(rcsMsgItem.mType)) {
            // mms forward
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, rcsMsgItem.mMsgId);
            sendIntent = RcsMessageUtils.createForwardIntentFromMms(mContext, realUri);
            if (sendIntent != null) {
                return MENU_FORWARD_IPMESSAGE;
            } else {
                return MENU_FORWARD_MESSAGE;
            }
        }
        return MENU_FORWARD_IPMESSAGE;
    }

    /**
     * private class RcsMsgListMenuClickListener.
     *
     */
    private final class RcsMsgListMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private long mIpMessageId;
        private long mMsgId;
        private IIpMessageItemExt mIpMsgItem;

        public RcsMsgListMenuClickListener(IIpMessageItemExt ipMsgItem) {
            mIpMsgItem = ipMsgItem;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onIpMenuItemClick(item, mIpMsgItem);
        }
    }

    @Override
    public boolean onIpMenuItemClick(MenuItem item, IIpMessageItemExt ipMsgItem) {
        Log.d(TAG, "onIpMenuItemClick ipMsgItem =" + ipMsgItem);
        if (ipMsgItem != null && onIpMessageMenuItemClick(item, (RcsMessageItem) ipMsgItem)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIpUpdateTitle(String number, String titleOriginal,
            ImageView ipCustomView, ArrayList<String> titles) {
        Log.d(TAG, "onIpUpdateTitle number =" + number);
        /*TODO should use RcsGroup better.
        if (mGroup != null) {
            String groupName = mGroup.getGroupNickName();
            if (TextUtils.isEmpty(groupName)) {
                groupName = mGroup.getSubject();
            }
            titles.add(groupName);
            titles.add("");
            return true;
        }
        */
        if (mIsGroupChat) {
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mGroupChatId);
            if (info != null) {
                String groupName = info.getNickName();
                if (TextUtils.isEmpty(groupName)) {
                    groupName = info.getSubject();
                }
                titles.add(groupName);
                titles.add("");
                return true;
            }
        }
        return super.onIpUpdateTitle(number, titleOriginal, ipCustomView, titles);
    }

    @Override
    public boolean onIpTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "onIpTextChanged s=" + s + ", start=" + start + ", before=" + before
                + ", count=" + count);
        // delete
        if (before > 0) {
            return true;
        }
        if (!TextUtils.isEmpty(s.toString())) {
            if (mServiceEnabled && mEmoji != null && mTextEditor != null) {
                float textSize = mTextEditor.getTextSize();
                EmojiImpl emojiImpl = EmojiImpl.getInstance(mPluginContext);
                CharSequence subSchars = s.subSequence(start, start + count);
                if (!emojiImpl.hasAnyEmojiExpression(subSchars)) {
                    return false;
                }
                CharSequence str = emojiImpl.getEmojiExpression(s, true, start, count,
                        (int) textSize);
                mTextEditor.removeTextChangedListener(mEditorWatcher);
                mTextEditor.setTextKeepState(str);
                mTextEditor.addTextChangedListener(mEditorWatcher);
            }
        }
        return true;
    }

    private void setEmojiActive(final boolean active) {
        if (mEmoji == null) {
            return;
        }
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!active) {
                    // hide emoticon panel, hide emoticon button
                    if (mEmoji.isEmojiPanelShow()) {
                        mEmoji.showEmojiPanel(false);
                    }
                    mEmoji.setEmojiButtonVisible(false);
                    mEmoji.setEmojiEditor(null);
                } else {
                    // show emoticon button
                    mEmoji.setEmojiButtonVisible(true);
                    mEmoji.setEmojiEditor(mTextEditor);
                }
            }
        });
    }

    @Override
    public boolean onIpRecipientsEditorFocusChange(boolean hasFocus, List<String> numbers) {
        Log.d(TAG, "onIpRecipientsEditorFocusChange hasFocus =" + hasFocus);
        if (hasFocus) {
            showSharePanel(false);
        }
        return super.onIpRecipientsEditorFocusChange(hasFocus, numbers);
    }

    @Override
    public boolean onIpInitialize(Intent intent, IWorkingMessageCallback workingMessageCallback) {
        mWorkingMessage = workingMessageCallback;
        long threadId = intent.getLongExtra("thread_id", 0);
        mIsNeedShowCallTime = false;
        if (threadId != 0) {
            mThreadId = threadId;
        } else {
            Uri uri = intent.getData();
            if (uri != null && uri.toString().startsWith("content://mms-sms/conversations/")) {
                String threadIdStr = uri.getPathSegments().get(1);
                threadIdStr = threadIdStr.replaceAll("-", "");
                mThreadId = Long.parseLong(threadIdStr);
            }
        }
        RcsConversation conversation = getRcsConversation();
        conversation.setThreadId(mThreadId);
        Log.d(TAG, "[onIpInitialize]: mThreadId = " + mThreadId);
        if (intent.getIntExtra(RCS_MODE_FLAG, 0) == RCS_MODE_VALUE) {
            mIsNeedShowCallTime = true;
        }
        if (mIsGroupChat) {
            GroupManager.getInstance().removeGroupListener(mInitGroupListener);
            dismissInvitationDialog();
            dismissInvitationTimeOutDialog();
            mGroup.removeActionListener(mGroupListener);
            GroupManager.getInstance().releaseRCSGroup(mGroup.getChatId());
        }

        if (mIsNeedShowCallTime) {
            setupCallStatusServiceConnection();
        }
        mIsGroupChat = false;
        String chatId = intent.getStringExtra("chat_id");
        if (!TextUtils.isEmpty(chatId)) {
            mIsGroupChat = true;
            mGroupChatId = chatId;
        } else if (mThreadId != 0){
            chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, mThreadId);
            if (!TextUtils.isEmpty(chatId)) {
                mIsGroupChat = true;
                mGroupChatId = chatId;
            }
        }
        if (mIsGroupChat) {
            mCallback.hideIpRecipientEditor();
            final GroupManager manager = GroupManager.getInstance();
            manager.addGroupListener(mInitGroupListener);
            mGroup = manager.getRCSGroup(mGroupChatId);
            mGroup.addActionListener(mGroupListener);
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mGroupChatId);
            int status = (int)info.getStatus();
            Log.d(TAG, "[onIpInitialize]group status is : " + status);
            if (status == GroupActionList.GROUP_STATUS_INVITING
                    || status == GroupActionList.GROUP_STATUS_INVITING_AGAIN) {
                 RcsMessagingNotification.cancelNewGroupInviations(mContext);
                 showInvitaionDialog(status);
                 setChatActive(false);
            }else if (status == GroupActionList.GROUP_STATUS_INVALID) {
                setChatActive(false);
            } else {
                 setChatActive(mServiceEnabled);
                 if (mServiceEnabled &&
                         RcsMessageUtils.isNeedSubscribeOfflineGroupInfo(mContext, mGroupChatId)) {
                    mGroup.sendGroupConferenceSubscription();
                    RcsMessageUtils.updateSubscribeOfflineGroupTime(mContext, mGroupChatId);
                 }
            }
            PortraitManager.getInstance().initGroupChatPortrait(mGroupChatId);
            conversation.setGroupChatId(chatId);
        }
        Log.d(TAG, "mThreadID = " + mThreadId + ", mGroupChatID = " + mGroupChatId);
        updateRcsMode();
        return true;
    }

    @Override
    public boolean onIpSaveInstanceState(Bundle outState, long threadId) {
        Log.d(TAG, "onIpSaveInstanceState initBurnedMode threadId =" + threadId +
                "  mDisplayBurned = " + mDisplayBurned +
                "  mBurnedCapbility = " + mBurnedCapbility);
        if (mDisplayBurned) {
            outState.putBoolean("burned_mode", mDisplayBurned);
            outState.putBoolean("burned_cap", mBurnedCapbility);
            if (getNumber() != null) {
                outState.putString("burned_contact", getNumber());
            }
        }
        return super.onIpSaveInstanceState(outState, threadId);
    }

    @Override
    public boolean onIpShowSmsOrMmsSendButton(boolean isMms) {
        Log.d(TAG, "onIpShowSmsOrMmsSendButton, isMms :" + isMms + "  mDisplayBurned = "
                + mDisplayBurned);
        if (mDisplayBurned && mServiceEnabled && mServiceRegistered) {
            mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ic_send_ipbar));
            return true;
        } else {
            if (isRcsMode()) {
                List<SubscriptionInfo> subInfos = SubscriptionManager.from(mContext)
                        .getActiveSubscriptionInfoList();
                int subCount = (subInfos == null || subInfos.isEmpty()) ? 0 : subInfos.size();
                 if (subCount > 0 && mIsSmsEnabled && (getRecipientSize() > 0 || mIsGroupChat) &&
                     (mTextEditor != null && !TextUtils.isEmpty(mTextEditor.getText()))) {
                     mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                             .getDrawable(R.drawable.ic_send_rcs_actived));
                     return true;
                 } else {
                     mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                             .getDrawable(R.drawable.ic_send_rcs));
                     return true;
                 }
            }
        }
        return false;
    }

    private void updateSendButton() {
        mWorkingMessage = mCallback.getWorkingMessage();
        boolean isMms = false;
        if (mWorkingMessage != null) {
            isMms = mWorkingMessage.requiresIpMms();
        }
        mCallback.showIpOrMmsSendButton(isMms);
    }

    @Override
    public boolean onIpPrepareOptionsMenu(IIpConversationExt ipConv, Menu menu) {
        Log.d(TAG, "onIpPrepareOptionsMenu, menu :" + menu);
        // / M: whether has IPMsg APK or not. ture: has ; false: no;
        Context pluginContext = mPluginContext;

        // / M: true: the host has been activated.
        boolean hasActivatedHost = RcsMessageConfig.isActivated();
        Log.d(TAG, "onRequestBurnMessageCapabilityResult mOldcontact = "
                + mOldcontact);
        Log.d(TAG, "onRequestBurnMessageCapabilityResult getNumber() = "
                + getNumber());
        boolean isServiceAvalible = RCSServiceManager.getInstance().isServiceActivated();
        if (isServiceAvalible && getNumber() != null &&
                !(getNumber().equals(mOldcontact))) {
            // getContactCapbility();
            if (1 == getRecipientSize()) {
                Log.d(TAG,"onIpPrepareOptionsMenu onRequestBurnMessageCapabilityResult" +
                    " getNumber() = " + getNumber() + " mBurnedCapbility = "
                    + mBurnedCapbility + "  mRegistCapbility = " + mRegistCapbility);
                if (!mRegistCapbility) {
                    mServiceManager.registBurnMsgCapListener(this);
                    mRegistCapbility = true;
                }
                mBurnedCapbility = false;
                mServiceManager.getBurnMsgCap(getNumber());
                mOldcontact = getNumber();
            } else {
                mBurnedCapbility = false;
                Log.d(TAG,
                        "onIpPrepareOptionsMenu onRequestBurnMessageCapabilityResult "
                                + "mBurnedCapbility = " + mBurnedCapbility);
            }
        }

        Log.d(TAG, "[addGroupMenuOptions] group info");
        if (mIsGroupChat) {
            if (mIsChatActive) {
                MenuItem groupInfoItem = menu.add(0, MENU_GROUP_CHAT_INFO, 0,
                        pluginContext.getText(R.string.menu_group_info));
                groupInfoItem.setIcon(pluginContext.getResources().getDrawable(
                        R.drawable.ic_add_chat_holo_dark));
                groupInfoItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                // disable unused menu
                for (int id : mUnusedOptionMenuForGroup) {
                    MenuItem item = menu.findItem(id);
                    if (item != null) {
                        item.setVisible(false);
                    }
                }
            } else {
                // only can select menu
                int size = menu.size();
                for (int index = 0; index < size; index++) {
                    MenuItem item = menu.getItem(index);
                    if (item.getItemId() != MENU_SELECT_MESSAGE) {
                        item.setVisible(false);
                    }
                }
            }
        } else {

            if (1 == getRecipientSize() && !mIsGroupChat) {
                if (mIsSmsEnabled && !isRecipientsEditorVisible() && mServiceEnabled) {
                    MenuItem item = menu.findItem(MENU_SHOW_CONTACT);
                    if (item != null) {
                        // if show cantact means the catact exist in db
                        menu.add(0, MENU_INVITE_FRIENDS_TO_CHAT, 0, pluginContext
                                .getString(R.string.menu_invite_friends_to_chat));
                    }
                }
                if (isRcsMode()) {
                    if (mDisplayBurned) {
                        menu.add(0, MENU_EDIT_BURNED_MSG, 0,
                                pluginContext.getString(R.string.menu_cacel_burned_msg));
                    } else {
                        menu.add(0, MENU_EDIT_BURNED_MSG, 0,
                                pluginContext.getString(R.string.menu_burned_msg));
                    }
                }
                String number = getNumber();
                boolean isInBlackList = RCSUtils.isIpSpamMessage(mContext, number);
                if (isInBlackList) {
                    menu.add(0, MENU_BLACK_LIST, 0,
                            pluginContext.getString(R.string.menu_remove_black_list));
                } else {
                    menu.add(0, MENU_BLACK_LIST, 0,
                            pluginContext.getString(R.string.menu_add_black_list));
                }
            }
        }
        // not allow to edit mms
        MenuItem item = menu.findItem(MENU_ADD_SUBJECT);
        if (item != null) {
            if (isRcsMode()) {
                item.setVisible(false);
            } else {
                item.setVisible(true);
            }
        }
        MenuItem attachItem = menu.findItem(MENU_ADD_ATTACHMENT);
        if (attachItem != null) {
            attachItem.setVisible(false);
        }
        return true;
    }

    @Override
    public void onIpMsgActivityResult(Context context, int requestCode, int resultCode,
            Intent data) {
        Log.d(TAG, " onIpMsgActivityResult(): requestCode = " + requestCode);
        boolean isServiceAvalible = RCSServiceManager.getInstance().isServiceActivated();
        Log.d(TAG,"onIpMsgActivityResult, isServiceAvalible = " + isServiceAvalible);

        if (resultCode != Activity.RESULT_OK &&
                requestCode != REQUEST_CODE_START_RCSSETTING_ACTIVITY) {
            Log.d(TAG, "[BurnedMsg]  requestCode :" + requestCode
                    + " resultCode: "+resultCode);
            if (!(resultCode == RESULT_CANCELLED
                    && requestCode == REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO)) {
                return;
            }
        }

        boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(mPluginContext);

        // / M: add for ip message
        switch (requestCode) {
        case REQUEST_CODE_INVITE_FRIENDS_TO_CHAT:
            Intent intent = new Intent(CreateGroupActivity.ACTIVITY_ACTION);
            ArrayList<String> allList = new ArrayList<String>();
            String[] numbers = mCallback.getConversationInfo();
            if (numbers != null && numbers.length > 0) {
                for (String number : numbers) {
                    allList.add(number);
                    Log.v(TAG, "now chat contact number: " + number);
                }
            }
            if (allList != null && allList.size() > 0) {
                intent.putStringArrayListExtra(
                        CreateGroupActivity.TAG_CREATE_GROUP_BY_NUMBERS, allList);
            }
            long[] ids = data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            final long[] contactsId = data
                    .getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            int count = mServiceManager.getGroupChatMaxParticipantsNumber();
            Log.d(TAG, "getGroupChatMaxParticipantsNumber, result = " + count);
            if (count > numbers.length && (contactsId.length + 1 + numbers.length > count)) {
                String toastString = mPluginContext.getString(
                        R.string.select_group_number_exceed_toast, count -1 - numbers.length);
                Toast.makeText(mContext, toastString, Toast.LENGTH_LONG).show();
                return;
            }
            if (contactsId != null && contactsId.length > 0) {
                intent.putExtra(CreateGroupActivity.TAG_CREATE_GROUP_BY_IDS, contactsId);
            }
            mContext.startActivity(intent);
            return;

        case REQUEST_CODE_IPMSG_TAKE_PHOTO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext,
                               mPluginContext.getString(R.string.cannot_send_filetransfer),
                               Toast.LENGTH_SHORT).show();
                return;
            }

            if (data != null) {
                Uri uri = data.getData();
                String scheme = uri.getScheme();
                if (scheme.equals("file")) {
                    mDstPath = uri.getEncodedPath();
                }
                Log.d(TAG, "take photo, data != null mDstPath = " + mDstPath);
            }
            mPhotoFilePath = mDstPath;

            Log.d(TAG, "take photo, mDstPath = " + mDstPath
                                + " mPhotoFilePath = " + mPhotoFilePath);

            if (!RcsMessageUtils.isValidAttach(mDstPath, false)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_err_file), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isPic(mDstPath)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                Toast.LENGTH_SHORT).show();
                return;
            }
            handleTakePhoto();
            return;
        case REQUEST_CODE_IPMSG_RECORD_VIDEO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_err_file), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!RcsMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): record video failed, invalid file");
                return;
            }

            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_SHARE_CONTACT:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            asyncIpAttachVCardByContactsId(data);
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_PHOTO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                Toast.makeText(mPluginContext, R.string.ipmsg_err_file, Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (!RcsMessageUtils.isValidAttach(mDstPath, false)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_err_file), Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (!RcsMessageUtils.isPic(mDstPath)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            handleChoosePhoto();
            return;

        case REQUEST_CODE_IPMSG_CHOOSE_VIDEO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_err_file), Toast.LENGTH_SHORT)
                            .show();
                return;
            }
            if (!RcsMessageUtils.isVideo(mDstPath)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): choose video failed, invalid file");
                return;
            }
            mVideoFilePath = mDstPath;
            Log.e(TAG, " send video old filename = " + mDstPath);
            mDstPath = RcsMessageUtils.getVideoDstFilePath(mVideoFilePath, mContext);
            Log.e(TAG, " send video new filename = " + mDstPath);
            RcsMessageUtils.copyFileToDst(mVideoFilePath, mDstPath);
            mIpMsgHandler.postDelayed(mSendVideo, 100);
            return;

        case REQUEST_CODE_IPMSG_RECORD_AUDIO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!getMediaMsgInfo(data, requestCode)) {
                return;
            }
            if (!RcsMessageUtils.isAudio(mDstPath)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!RcsMessageUtils.isFileStatusOk(mContext, mDstPath)) {
                Log.e(TAG, "onIpMsgActivityResult(): record audio failed, invalid file");
                return;
            }
            mAudioPath = mDstPath;
            mDstPath = RcsMessageUtils.getAudioDstPath(mAudioPath, mContext);
            RcsMessageUtils.copyFileToDst(mAudioPath, mDstPath);
            mIpMsgHandler.postDelayed(mSendAudio, 100);
            return;
        case REQUEST_CODE_IPMSG_CHOOSE_AUDIO:
            if (!isServiceAvalible) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.cannot_send_filetransfer), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (data != null) {
                Uri uri = (Uri) data
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (Settings.System.getUriFor(Settings.System.RINGTONE).equals(uri)) {
                    return;
                }
                if (getAudio(data)) {
                    mAudioPath = mDstPath;
                    mDstPath = RcsMessageUtils.getAudioDstPath(mAudioPath, mContext);
                    RcsMessageUtils.copyFileToDst(mAudioPath, mDstPath);
                    mIpMsgHandler.postDelayed(mSendAudio, 100);
                }
            }
            return;
        case REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO:
            Log.d(TAG, " [BurnedMsg]: bindIpmsg: change data ");
            mCallback.notifyIpDataSetChanged();
            RcsConversation conversation = getRcsConversation();
            if (conversation != null) {
                 int msgCount =  conversation.getMessageCount();
                 long threadID = conversation.getThreadId();
                 boolean isSmsExsist = mTextEditor != null && !mTextEditor.getText()
                                                        .toString().isEmpty();
                 if ((isSmsExsist == false && threadID <= 0) || isMms() ||
                         threadID <= 0 || msgCount <= 1 ) {
                     mContext.finish();
                 }
             }
            return;
        case REQUEST_CODE_START_RCSSETTING_ACTIVITY:
            if (mServiceRegistered) {
                sendIpTextMessage();
            } else {
                //TODO don't send?
                Toast.makeText(mContext, mPluginContext.getString(R.string.send_again),
                        Toast.LENGTH_SHORT).show();
                mCallback.callbackUpdateButtonState(true);
            }
            return;
        default:
            break;
        }
    }
    private void handleTakePhoto() {
        Runnable takePhotoRunnable = new Runnable() {
            @Override
            public void run() {
                long photoSize = getFileSize(mDstPath);
                boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(mPluginContext);
                Log.d(TAG, " isSendOrgPic = " + isSendOrgPic);
                if (RcsMessageUtils.isGif(mDstPath)) {
                    Log.d(TAG, " it is a gif");
                    sendImage();
                } else if (photoSize < mFileTransferMaxSize && isSendOrgPic) {
                    Log.d(TAG, " photoSize < RCSMaxSize and is org pic sent, photoSize = "
                            + photoSize);
                    sendImage();
                } else if (photoSize > mFileTransferMaxSize && isSendOrgPic) {
                    // exceed max size and don't org pic sent
                    Log.d(TAG, " photoSize > RCSMaxSize and is org pic sent, photoSize = "
                            + photoSize);
                    showSendPicAlertDialog();
                } else {
                    Log.d(TAG, " It is NOT org pic sent ");
                    new Thread(mResizePic, "ipmessage_resize_pic").start();
                }
            }
        };
        (new getMaxSizeAsyncTask(takePhotoRunnable)).execute();
    }

    private void handleChoosePhoto() {
        Runnable choosePhotoRunnable = new Runnable() {
            @Override
            public void run() {
                mPhotoFilePath = mDstPath;
                Log.e(TAG, " send image old filename = " + mDstPath);
                mDstPath = RcsMessageUtils.getPhotoDstFilePath(mDstPath, mContext);
                Log.e(TAG, " send image new filename = " + mDstPath);
                long photoSize = getFileSize(mPhotoFilePath);
                boolean isSendOrgPic = RcsSettingsActivity.getSendMSGStatus(mPluginContext);
                Log.d(TAG, " isSendOrgPic = " + isSendOrgPic);
                if (RcsMessageUtils.isGif(mDstPath)) {
                    Log.d(TAG, " it is a gif");
                    RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                    sendImage();
                } else if (photoSize < mFileTransferMaxSize && isSendOrgPic) {
                    Log.d(TAG, " photoSize < RCSMaxSize and is org pic sent, photoSize = "
                            + photoSize);
                    RcsMessageUtils.copyFileToDst(mPhotoFilePath, mDstPath);
                    sendImage();
                } else if (photoSize > mFileTransferMaxSize && isSendOrgPic) {
                    // exceed max size and don't org pic sent
                    Log.d(TAG, " photoSize > RCSMaxSize and is org pic sent, photoSize = "
                            + photoSize);
                    showSendPicAlertDialog();
                } else {
                    Log.d(TAG, " It is NOT org pic sent ");
                    new Thread(mResizePic, "ipmessage_resize_pic").start();
                }
            }
        };
        (new getMaxSizeAsyncTask(choosePhotoRunnable)).execute();
    }

    /**
     * this class extends AsyncTask, it is for getting the maxsize of FileTransfer
     * and sending image. In doInBackground(), it will get the FileTransfer maxsize
     * in onPostExecute(), it will send the image.
     */

    private class getMaxSizeAsyncTask extends AsyncTask<Void,Void,Void> {
        private Runnable mPostExecuteTask;
        public getMaxSizeAsyncTask(Runnable r) {
            mPostExecuteTask = r;
        }

        @Override
        protected void onPreExecute() {
            // show progress bar
            Log.d(TAG, " getMaxSizeAsyncTask,  onPreExecute");
            showProgressDialog(mPluginContext
                             .getString(R.string.pref_please_wait));
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, " getMaxSizeAsyncTask,  doInBackground");
            mFileTransferMaxSize = getRcsFileMaxSize();
            return null;
        }

        @Override
        protected void onPostExecute(Void arg0) {
            Log.d(TAG, " getMaxSizeAsyncTask,  onPostExecute");
            super.onPostExecute(arg0);
            dismissProgressDialog();
            if (mPostExecuteTask != null) {
                mPostExecuteTask.run();
            }
        }
    }

    private boolean checkBurnedMsgCapbility() {
        Log.d(TAG, "initBurnedMode  onRequestBurnMessageCapabilityResult " +
               " checkBurnedMsgCapbility() " + "mBurnedCapbility = " + mBurnedCapbility
                + "  mDisplayBurned = "+ mDisplayBurned);
        if (mDisplayBurned && !mBurnedCapbility) {

            mDisplayBurned = false;
            mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ic_send_ipbar));

            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_burn_cap),
                    Toast.LENGTH_SHORT).show();
            RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
            mSharePanel.resetShareItem();
            return false;
        }
        return true;
    }

    private void showSendPicAlertDialog() {
        Resources res = mPluginContext.getResources();
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setTitle(mPluginContext.getString(R.string.ipmsg_over_file_limit))
            .setMessage(mPluginContext.getString(R.string.ipmsg_resize_pic));
        b.setCancelable(true);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                new Thread(mResizePic, "ipmessage_resize_pic").start();
            }
        });

        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.create().show();
    }

    @Override
    public boolean onIpInitMessageList(ListView list, IIpMessageListAdapterExt adapter) {
        Log.d(TAG, "onIpInitMessageList");
        mListView = list;
        mRcsMessageListAdapter = (RcsMessageListAdapter) adapter;
        mRcsMessageListAdapter.addOnMessageListChangedListener(mMsgListChangedListener);
        mRcsMessageListAdapter.setMsgListItemHandler(mMsgListItemHandler);
        return super.onIpInitMessageList(list, adapter);
    }

    private final Handler mMsgListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case RcsMessageListItem.LAUNCH_DOWNLOAD_UI_MESSAGE:
                Intent intent = (Intent)msg.obj;
                mContext.startActivityForResult(intent,
                       REQUEST_CODE_IP_MSG_BURNED_MSG_AUDIO);
                break;
            default:
                Log.d(TAG, "msg type: " + msg.what + "not handler");
                break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onIpSaveDraft(long threadId) {
        Log.d(TAG, "onIpSaveDraft, threadId =" + threadId);
        if (mIsGroupChat) {
            final String body = mTextEditor.getText().toString();
            if (mThreadId <= 0 && !TextUtils.isEmpty(body)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mCallback.guaranteeIpThreadId();
                        mThreadId = mCallback.getCurrentThreadId();
                        ContentValues values = new ContentValues(3);
                        values.put(Sms.THREAD_ID, mThreadId);
                        values.put(Sms.BODY, body);
                        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT);
                        mContext.getContentResolver().insert(Sms.CONTENT_URI, values);
                    }
                }, "onIpSaveDraft").run();
                return true;
            }
        }
        return super.onIpSaveDraft(threadId);
    }


    @Override
    public boolean onIpUpdateTextEditorHint() {
        if (updateTextEditorHint()) {
            return true;
        }
        return super.onIpUpdateTextEditorHint();
    }

    private boolean updateTextEditorHint() {
        Log.d(TAG, "onIpUpdateTextEditorHint, mServiceEnabled :" + mServiceEnabled);
        if (mServiceEnabled && mTextEditor != null && mIsSmsEnabled) {
            if (isRcsMode()) {
                mTextEditor.setHint(mPluginContext.getText(R.string.hint_rcs));
            } else {
                mTextEditor.setHint(mPluginContext.getText(R.string.hint_smsmms));
            }
            return true;
        }
        return false;
    }

    public Handler mIpMsgHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "mIpMsgHandler handleMessage, msg.what: " + msg.what);
            switch (msg.what) {
            case ACTION_RCS_SHARE:
                if (RcsMessageConfig.isServiceEnabled(mContext)
                        && isNetworkConnected(mContext)) {
                    doMoreAction(msg);
                }
                break;
            case ACTION_SHARE:
                doMmsAction(msg);
                break;
            default:
                Log.d(TAG, "msg type: " + msg.what + "not handler");
                break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public boolean loadIpMessagDraft(long threadId) {
        Log.d(TAG, "loadIpMessagDraft() threadId = " + threadId);
        return super.loadIpMessagDraft(threadId);
    }

    @Override
    public boolean checkIpMessageBeforeSendMessage(long subId, boolean bCheckEcmMode) {
        Log.d(TAG, "checkIpMessageBeforeSendMessage()");
        if (RcsMessageUtils.getConfigStatus()) {
            int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
            if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)
                    || (int) subId != mainCardSubId) {
                if (!mIsGroupChat) {
                    return false;
                }
            }
        } else {
            if (!mIsGroupChat) {
                return false; // not Config
            } else {
                return true;
            }
        }
        Log.d(TAG, "checkIpMessageBeforeSendMessage() conitnue; subId = " + subId);
        mSendSubId = (int)subId;
        mWorkingMessage = mCallback.getWorkingMessage();

        // if the message is mms, need use mms send
        boolean isMms = mWorkingMessage.requiresIpMms();
        if (isMms) {
            if (mIsGroupChat) {
                throw new RuntimeException("can not send mms in group chat");
            }
            return false;
        }

        if (!isRcsMode()) {
            return false;
        }

        if (!checkBurnedMsgCapbility()) {
            return true;
        }

//        if (!mIsGroupChat && mServiceRegistered && !isMms) {
        if (!mIsGroupChat && isRcsMode() && !isMms) {
            if (RcsMessageUtils.isNeedNotifyUserWhenToRCS(mPluginContext, (int)subId)) {
                mContext.showDialog(DIALOG_ID_NOTIFY_SEND_BY_RCS);
                return true;
            }
        }

        String body = mTextEditor.getText().toString();
        if (body != null && body.length() > 0) {
            sendIpTextMessage();
            return true;
        }
        return false;
    }

    private boolean onIpMessageMenuItemClick(MenuItem menuItem, final RcsMessageItem rcsMsgItem) {
        long msgId = rcsMsgItem.mMsgId;
        IpMessage iPMsg = mRcsMessageManager.getRCSMessageInfo(rcsMsgItem.mMsgId);
        Log.d(TAG, "onIpMessageMenuItemClick(): ");
        switch (menuItem.getItemId()) {
        case MENU_RETRY:
            return rcsMsgItem.resend();

        case MENU_FORWARD_IPMESSAGE:
            Log.d(TAG, "MENU_FORWARD_IPMESSAGE");
            hideInputMethod();
            forwardIpMsg(mContext, rcsMsgItem);
            return true;

        case MENU_EXPORT_SD_CARD:
//            IpMessage ipMessageForSave = mRcsMessageManager.getRCSMessageInfo(msgId);
            Log.d(TAG, "onIpMessageMenuItemClick(): Save IP message. msgId = " + rcsMsgItem.mMsgId);
            copyFile((IpAttachMessage) iPMsg);
            return true;

        case MENU_FAVORITE:
            ContentValues values = new ContentValues();
            //FLAG
            int flag = 0;
            if (mIsGroupChat) {
                flag = ChatMessage.MTM;
            } else if (getRecipientSize() > 1) {
                flag = ChatMessage.OTM;
            } else {
                flag = ChatMessage.OTO;
            }
            values.put(FavoriteMsgData.COLUMN_DA_FLAG, flag);

            //CONTACT_NUMBER
            if (flag == ChatMessage.MTM) {
                if (mGroupChatId != null) {
                    values.put(FavoriteMsgData.COLUMN_DA_CONTACT, mGroupChatId);
                }
            } else {
                values.put(FavoriteMsgData.COLUMN_DA_CONTACT, getRecipientStr());
            }
            values.put(FavoriteMsgData.COLUMN_DA_TIMESTAMP, rcsMsgItem.mDate);
            if (rcsMsgItem.isRcs()) {
                if (rcsMsgItem.mRcsDirection == RcsLog.Direction.INCOMING) {
                    //only receive time,need sync with xun han
                    values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.INCOMING);
                } else {
                    values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.OUTCOMING);
                }
                IpMessage ipMessageForFavorite =
                        mRcsMessageManager.getRCSMessageInfo(rcsMsgItem.mMsgId);
                values.put(FavoriteMsgData.COLUMN_DA_ID, ipMessageForFavorite.getMessageId());
                values.put(FavoriteMsgData.COLUMN_DA_MESSAGE_STATUS,
                        ipMessageForFavorite.getStatus());
                if (rcsMsgItem.mMessageType == RcsLog.MessageType.FT) {
                    IpAttachMessage attachMessage = (IpAttachMessage) ipMessageForFavorite;

                    setAttachIpmessageFavorite(msgId, attachMessage, values);
                } else if (rcsMsgItem.mMessageType == RcsLog.MessageType.IM) {
                    IpTextMessage textMessage = (IpTextMessage) ipMessageForFavorite;
                    setTextIpmessageFavorite(msgId, textMessage.getType(),
                            textMessage.getBody(), values);
                }
            } else if (rcsMsgItem.isSms()) {
                if (rcsMsgItem.mBoxId == 1) {
                    //only receive time,need sync with xun han
                    values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.INCOMING);
                } else {
                    values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.OUTCOMING);
                }
                setSmsFavorite(mContext, msgId, rcsMsgItem.mBody, values);
            } else {
                setMmsFavorite(mContext, msgId, rcsMsgItem.mSubject,
                        rcsMsgItem.mBoxId, rcsMsgItem.mMessageType, values);
            }
            return true;

        case MENU_REPORT:
            try {
                // Context menu handlers for the spam report.
                String contact = RcsSettingsActivity.getReportNum(mPluginContext);
                Log.d(TAG, "[spam-report]:  msgId = " + rcsMsgItem.mMsgId + "  contact = " + contact
                        + "  mThreadId = " + mThreadId);
                String spamheader = "Spam-From:"+"Spam-To:"+"DateTime:";
                String content = mTextEditor.getText().toString()+ spamheader;
                boolean isPagerMode = (content.getBytes("UTF-8").length + 50)> 900?false:true;
                mRcsMessageManager.initSpamReport(
                        contact, iPMsg.getMessageId(),iPMsg.getType(),isPagerMode);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return true;

        case MENU_IPMSG_DELIVERY_REPORT:
            showIpDeliveryReport(mContext, rcsMsgItem);
            return true;

        case MENU_COPY:
            if ("rcs".equals(rcsMsgItem.mType)) {
                copyToClipboard(rcsMsgItem.mBody);
                return true;
            }
            break;

        default:
            break;
        }
        return false;
    }

    private void showIpDeliveryReport(Context context, RcsMessageItem rcsMsgItem) {
        Log.d(TAG, "showIpDeliveryReport()");
        if (rcsMsgItem == null) {
            return;
        }
        if (rcsMsgItem.isRcs()) {
            new AlertDialog.Builder(context)
                .setTitle(mPluginContext.getString(R.string.ipmsg_delivery_report))
                .setMessage(getIpDeliveryStatus(mPluginContext, rcsMsgItem))
                .setCancelable(true)
                .show();
        }
        return;
    }

    private String getIpDeliveryStatus(Context context, RcsMessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();
        if (!msgItem.mIsGroupItem) {
            details.append(res.getString(R.string.to_address_label));
            details.append(msgItem.mAddress);
            details.append('\n');
        }

        String status = res.getString(R.string.status_none);
        int ipStatus = msgItem.getIpMessage().getStatus();
        if (ipStatus == MessageStatus.DELIVERED) {
            status = res.getString(R.string.status_received);
        } else if (ipStatus == MessageStatus.FAILED) {
            status = res.getString(R.string.status_failed);
        } else if (ipStatus == MessageStatus.SENT) {
            status = res.getString(R.string.status_sent);
        } else if (ipStatus == MessageStatus.SENDING) {
            status = res.getString(R.string.status_sending);
        }
        details.append(res.getString(R.string.ipmsg_status_label));
        details.append(status);

        if (msgItem.mDate > 0L) {
            details.append('\n');
            details.append(res.getString(R.string.sent_label));
            String dateStr = RcsUtilsPlugin.formatIpTimeStampString(msgItem.mDate, true);
            details.append(dateStr);
        }
        return details.toString();
    }

    private boolean forwardIpMsg(Context context, RcsMessageItem rcsMsgItem) {

        Intent sendIntent = new Intent();
//        long ipMessageId = rcsMsgItem.mIpMessageId;
        IpMessage ipMessage = mRcsMessageManager.getRCSMessageInfo(rcsMsgItem.mMsgId);
        Log.d(TAG, "forwardIpMsg()  mType =" + rcsMsgItem.mType);
        if ("sms".equals(rcsMsgItem.mType)) {
            // sms forward
            sendIntent = RcsMessageUtils.createForwordIntentFromSms(context,
                    rcsMsgItem.mBody);
        } else if ("mms".equals(rcsMsgItem.mType)) {
            // mms forward
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, rcsMsgItem.mMsgId);
            sendIntent = RcsMessageUtils.createForwardIntentFromMms(context, realUri);
        } else if (rcsMsgItem.isRcs()) {
            // ip message forward
            sendIntent = RcsMessageUtils.createForwordIntentFromIpmessage(context, ipMessage);
            if (sendIntent != null) {
            } else {
                if ((ipMessage.getType() == IpMessageType.PICTURE
                        || ipMessage.getType() == IpMessageType.VIDEO
                        || ipMessage.getType() == IpMessageType.VOICE
                        || ipMessage.getType() == IpMessageType.VCARD || ipMessage
                            .getType() == IpMessageType.GEOLOC)) {
                    Toast.makeText(context,
                                    mPluginContext.getString(R.string.no_service_cannot_forward),
                                    Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        mContext.startActivity(sendIntent);
        return true;
    }

    private boolean isNetworkConnected(Context context) {
        boolean isNetworkConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(mContext.CONNECTIVITY_SERVICE);
        State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (State.CONNECTED == state) {
            isNetworkConnected = true;
        }
        if (!isNetworkConnected) {
            state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
            if (State.CONNECTED == state) {
                isNetworkConnected = true;
            }
        }
        return isNetworkConnected;
    }

    private long getRcsFileMaxSize() {
        long maxSize = RCSUtils.getFileTransferMaxSize() * 1024;
        Log.d(TAG, "getRcsFileMaxSize() = " + maxSize);
        return maxSize;
    }

    private long getFileSize(String filepath) {
        return RcsMessageUtils.getFileSize(filepath);
    }

    private Runnable mResizePic = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mResizePic(): start resize pic.");
            long maxLen = RcsMessageUtils.getCompressLimit();
            byte[] img = RcsMessageUtils.resizeImg(mPhotoFilePath, (float) maxLen);
            if (null == img) {
                return;
            }
            Log.d(TAG, "mResizePic(): put stream to file.");
            try {
                RcsMessageUtils.nmsStream2File(img, mDstPath);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Log.d(TAG, "mResizePic(): post send pic.");
            mIpMsgHandler.postDelayed(mSendPic, 100);
        }
    };

    private boolean sendMessageForIpMsg(final IpMessage ipMessage,
            boolean isSendSecondTextMessage, final boolean isDelDraft) {
        Log.d(TAG, "sendMessageForIpMsg(): start.");
        if (!mIsGroupChat) {
            mWorkingMessage.syncWorkingIpRecipients();
            mChatModeNumber = getRecipientStr();
        }

        ipMessage.setTo(mChatModeNumber);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendMessageForIpMsg(): calling API: saveIpMsg().");
                int ret = -1;
                ipMessage.setStatus(RcsLog.MessageStatus.SENDING);
                mCallback.guaranteeIpThreadId();
                mThreadId = mCallback.getCurrentThreadId();
                getRcsConversation().setThreadId(mThreadId);
                String chatId = mIsGroupChat ? mGroupChatId : "";
                ret = mRcsMessageManager.sendRCSMessage(chatId, ipMessage);
                if (ipMessage.getType() == IpMessageType.TEXT) {
                    //for text call this function to reset message for clear TextEditor
                    mCallback.onPreIpMessageSent();
                } else if (mCallback.isIpRecipientEditorVisible()) {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //hide recipientEditor, so query can be run
                            mCallback.hideIpRecipientEditor();
                        }
                    });
                }

                if (ipMessage.getType() == IpMessageConsts.IpMessageType.TEXT) {
                    mCallback.asyncDeleteDraftSmsMessage();
                } else if (ipMessage.getType() == IpMessageConsts.IpMessageType.EMOTICON) {
                    // do nothing
                } else {
                    Message msg = new Message();
                    msg.what = ret;
                    mFTNotifyHandler.sendMessage(msg);
                }
                mCallback.onIpMessageSent();
            }
        }).start();

        return true;
    }

    public Handler mFTNotifyHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "mIpMsgHandler handleMessage, msg.what: " + msg.what);
            switch (msg.what) {
            case RCSMessageManager.ERROR_CODE_UNSUPPORT_TYPE:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_INVALID_PATH:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_EXCEED_MAXSIZE:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_over_file_limit),
                        Toast.LENGTH_SHORT).show();
                break;
            case RCSMessageManager.ERROR_CODE_UNKNOWN:
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.ipmsg_invalid_file_type),
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    private void sendIpTextMessage() {
        String body = mTextEditor.getText().toString();
        if (TextUtils.isEmpty(body)) {
            Log.w(TAG, "sendIpTextMessage(): No content for sending!");
            return;
        }
        IpTextMessage msg = new IpTextMessage();
        msg.setBody(body);
        msg.setType(IpMessageType.TEXT);
        msg.setBurnedMessage(mDisplayBurned);
        sendMessageForIpMsg(msg, false, false);
    }

    public void sendIpEmoticonMessage(String emXml) {
        if (TextUtils.isEmpty(emXml)) {
            Log.w(TAG, "sendIpEmoticonMessage(): emXml is null!");
            return;
        }
        if (!RCSServiceManager.getInstance().serviceIsReady()) {
            Toast.makeText(mContext,
                    mPluginContext.getString(R.string.rcs_not_availble),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        IpTextMessage msg = new IpTextMessage();
        msg.setBody(emXml);
        msg.setType(IpMessageType.EMOTICON);
        sendMessageForIpMsg(msg, false, false);
    }

    private Runnable mSendAudio = new Runnable() {
        public void run() {
            if (!checkBurnedMsgCapbility()) {
                return;
            }

            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVoiceMessage msg = new IpVoiceMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
                msg.setType(IpMessageType.VOICE);
                msg.setBurnedMessage(mDisplayBurned);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendAudio);
            }
        }
    };

    private Runnable mSendGeoLocation = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendGeoLocation(): start.");
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpGeolocMessage msg = new IpGeolocMessage();
                msg.setPath(mDstPath);
                msg.setType(IpMessageType.GEOLOC);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendGeoLocation);
            }
        }
    };
    private Runnable mSendPic = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendPic(): start.");
            if (!checkBurnedMsgCapbility()) {
                return;
            }
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                Log.d(TAG, "mSendPic(): start send image.");
                sendImage();
                mIpMsgHandler.removeCallbacks(mSendPic);
            }
            Log.d(TAG, "mSendPic(): end.");
        }
    };

    private Runnable mSendVideo = new Runnable() {
        public void run() {
            Log.d(TAG, "mSendVideo(): start send video. Path = " + mDstPath);
            if (!checkBurnedMsgCapbility()) {
                return;
            }
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVideoMessage msg = new IpVideoMessage();
                msg.setPath(mDstPath);
                msg.setDuration(mDuration);
                msg.setType(IpMessageType.VIDEO);
                msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
                msg.setBurnedMessage(mDisplayBurned);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendVideo);
            }
        }
    };

    private Runnable mSendVcard = new Runnable() {
        public void run() {
            if (RcsMessageUtils.isExistsFile(mDstPath)
                    && RcsMessageUtils.getFileSize(mDstPath) != 0) {
                IpVCardMessage msg = new IpVCardMessage();
                msg.setPath(mDstPath);
                msg.setName(mIpMessageVcardName);
                msg.setType(IpMessageType.VCARD);
                sendMessageForIpMsg(msg, false, false);
                mIpMsgHandler.removeCallbacks(mSendVcard);
            }
        }
    };

    public boolean getMediaMsgInfo(Intent data, int requestCode) {
        if (null == data) {
            Log.e(TAG, "getMediaMsgInfo(): take video error, result intent is null.");
            return false;
        }

        Uri uri = data.getData();
        Cursor cursor = null;
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO
                || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            final String[] selectColumn = { "_data" };
            cursor = mContext.getContentResolver().query(uri, selectColumn, null, null, null);
        } else {
            final String[] selectColumn = { "_data", "duration" };
            cursor = mContext.getContentResolver().query(uri, selectColumn, null, null, null);
        }
        if (null == cursor) {
            if (requestCode == REQUEST_CODE_IPMSG_RECORD_AUDIO) {
                mDstPath = uri.getEncodedPath();
                mDuration = data.getIntExtra("audio_duration", 0);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            } else {
                mPhotoFilePath = uri.getEncodedPath();
                mDstPath = mPhotoFilePath;
            }
            return true;
        }
        if (0 == cursor.getCount()) {
            cursor.close();
            Log.e(TAG, "getMediaMsgInfo(): take video cursor getcount is 0");
            return false;
        }
        cursor.moveToFirst();
        if (requestCode == REQUEST_CODE_IPMSG_TAKE_PHOTO
                || requestCode == REQUEST_CODE_IPMSG_CHOOSE_PHOTO) {
            mPhotoFilePath = cursor.getString(cursor.getColumnIndex("_data"));
            mDstPath = mPhotoFilePath;
        } else {
            mDstPath = cursor.getString(cursor.getColumnIndex("_data"));
            mDuration = cursor.getInt(cursor.getColumnIndex("duration"));
            mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        return true;
    }

    private boolean getAudio(Intent data) {
        Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (Settings.System.getUriFor(Settings.System.RINGTONE).equals(uri)) {
            return false;
        }
        if (null == uri) {
            uri = data.getData();
        }
        if (null == uri) {
            Log.e(TAG, "getAudio(): choose audio failed, uri is null");
            return false;
        }
        final String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            mDstPath = uri.getEncodedPath();
        } else {
            ContentResolver cr = mContext.getContentResolver();
            Cursor c = cr.query(uri, null, null, null, null);
            c.moveToFirst();
            mDstPath = c.getString(c.getColumnIndexOrThrow(Audio.Media.DATA));
            c.close();
        }

        if (!RcsMessageUtils.isAudio(mDstPath)) {
            Toast.makeText(mPluginContext, R.string.ipmsg_invalid_file_type,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!mServiceManager.isFeatureSupported(IpMessageConsts.FeatureId.FILE_TRANSACTION)
                && !RcsMessageUtils.isFileStatusOk(mContext, mDstPath)) {
            Log.e(TAG, "getAudio(): choose audio failed, invalid file");
            return false;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mContext, uri);
            String dur = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                mDuration = Integer.parseInt(dur);
                mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
            }
        } catch (Exception ex) {
            Log.e(TAG,
                    "getAudio(): MediaMetadataRetriever failed to get duration for "
                            + uri.getPath());
            return false;
        } finally {
            retriever.release();
        }
        return true;
    }

    private void sendImage() {
        IpImageMessage msg = new IpImageMessage();
        msg.setType(IpMessageType.PICTURE);
        msg.setPath(mDstPath);
        msg.setSize(RcsMessageUtils.getFileSize(mDstPath));
        msg.setBurnedMessage(mDisplayBurned);
        // msg.setRcsStatus(Status.TRANSFERING);
        msg.setStatus(RcsLog.MessageStatus.SENDING);
        Log.d(TAG, "sendImage(): start send message.");
        sendMessageForIpMsg(msg, false, false);
    }

    @Override
    public boolean onIpMsgOptionsItemSelected(IIpConversationExt ipConv, MenuItem item,
            long threadId) {
        Log.d(TAG, "onIpMsgOptionsItemSelected: menu item is: " + item.getItemId());
        switch (item.getItemId()) {
        case MENU_INVITE_FRIENDS_TO_CHAT:
            if (mServiceManager
                    .isFeatureSupported(IpMessageConsts.FeatureId.EXTEND_GROUP_CHAT)) {
                Intent intent = new Intent(
                        "android.intent.action.contacts.list.PICKMULTIPHONES");
                intent.setType(Phone.CONTENT_TYPE);
                intent.putExtra("Group", true);
                List<String> existNumbers = new ArrayList<String>();
                String number = getNumber();
                existNumbers.add(number);
                String me = RcsProfile.getInstance().getNumber();
                if (!TextUtils.isEmpty(me)) {
                    existNumbers.add(me);
                }
                String[] numbers = existNumbers.toArray(new String[existNumbers.size()]);
                intent.putExtra("ExistNumberArray", numbers);
                mContext.startActivityForResult(intent, REQUEST_CODE_INVITE_FRIENDS_TO_CHAT);
                return true;
            }
            break;
        case MENU_GROUP_CHAT_INFO:
            Log.i(TAG, "launch GroupChatInfo Activity");
            {
                Intent intent = new Intent(
                        "com.mediatek.rcs.message.ui.RcsGroupManagementSetting");
                intent.setPackage("com.mediatek.rcs.message");
                intent.putExtra("SCHATIDKEY", mGroupChatId);
                mContext.startActivity(intent);
                return true;
            }
        case MENU_EDIT_BURNED_MSG:
            Log.d(TAG,
                    "onRequestBurnMessageCapabilityResult MENU_EDIT_BURNED_MSG mBurnedCapbility = "
                            + mBurnedCapbility);
            if ((mDisplayBurned == false)&&(mBurnedCapbility == false)) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_burn_cap),
                        Toast.LENGTH_SHORT).show();
                mOldcontact = null;
                return true;
            }
            mDisplayBurned = !mDisplayBurned;

            if (mDisplayBurned) {
                // mSendButtonIpMessage.setImageResource(R.drawable.ic_send_ipbar);
                mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                        .getDrawable(R.drawable.ic_send_ipbar));
            } else {
                if (mTextEditor != null && !mTextEditor.getText().toString().isEmpty()) {
                    mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                            .getDrawable(R.drawable.ic_send_rcs_actived));
                } else {
                    mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                            .getDrawable(R.drawable.ic_send_rcs));
                }
                mOldcontact = null;
            }
            mSendButtonIpMessage.setClickable(true);
            Log.d(TAG, "[BurnedMsg] burned mode mDisplayBurned: "+ mDisplayBurned);
            RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
            mSharePanel.resetShareItem();
            return true;
        case MENU_CALL_RECIPIENT:
            if (mIsGroupChat) {
                // TODO: multi call
                List<Participant> participants = mGroup.getParticipants();
                ArrayList<Participant> toCallParticipants = new ArrayList<Participant>();
                // ArrayList<String> numbers = new ArrayList<String>();
                String myNumber = RcsProfile.getInstance().getNumber();
                PortraitManager pManager = PortraitManager.getInstance();
                for (Participant p : participants) {
                    if (!p.getContact().equals(myNumber)) {
                        MemberInfo info = pManager.getMemberInfo(mGroup.getChatId(),
                                p.getContact());
                        Participant partipant = new Participant(info.mNumber, info.mName);
                        toCallParticipants.add(partipant);
                    }
                }
                if (toCallParticipants.size() > 1 &&
                        RcsMessageUtils.isVoLTEConfCallEnable(mContext)) {
                    showMultiSelectContactDialogForCall(toCallParticipants);
                } else if (toCallParticipants.size() == 1) {
                    showSingleSelectContactDialogForCall(toCallParticipants);
                } else {
                    Log.e(TAG, "No participant for select to call in group");
                }
                return true;
            } else if (getRecipientSize() > 1) {
                // one2multi
                ArrayList<Participant> toCallParticipants = new ArrayList<Participant>();
                RcsConversation conversation = getRcsConversation();
                if (conversation != null) {
                    List<IIpContactExt> list = getIpContactList();
                    for (IIpContactExt c : list) {
                        RcsContact contact = (RcsContact) c;
                        Participant p = new Participant(contact.getNumber(), contact.getName());
                        toCallParticipants.add(p);
                    }
                    if (toCallParticipants.size() > 1 &&
                                RcsMessageUtils.isVoLTEConfCallEnable(mContext)) {
                        showMultiSelectContactDialogForCall(toCallParticipants);
                    } else {
                        showSingleSelectContactDialogForCall(toCallParticipants);
                    }
                } else {
                    Log.e(TAG, "init call data error: conversation is null");
                }
                return true;
            }
            break;

        case MENU_BLACK_LIST:
            String number = getNumber();
            String recipient = getRecipientStr();
            boolean isInBlackList = RCSUtils.isIpSpamMessage(mContext, number);
            CCSblacklist blist = new CCSblacklist(mContext);
            if (isInBlackList) {
                blist.removeblackNumber(number);
            } else {
                blist.addblackNumber(number, recipient);
            }
            break;
        default:
            break;
        }
        return false;
    }

    private boolean doMoreAction(Message msg) {
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SHARE_ACTION);
        boolean ret = true;
        boolean isNoRecipient = (getRecipientSize() == 0 && !mIsGroupChat);
        switch (action) {
        case IPMSG_TAKE_PHOTO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                takePhoto();
            }
            break;

        case IPMSG_RECORD_VIDEO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                recordVideo();
            }
            break;

        case IPMSG_SHARE_CONTACT:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                shareContact();
            }
            break;

        case IPMSG_CHOOSE_PHOTO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                choosePhoto();
            }
            break;

        case IPMSG_CHOOSE_VIDEO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                chooseVideo();
            }
            break;

        case IPMSG_RECORD_AUDIO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                recordAudio();
            }
            break;

        case IPMSG_CHOOSE_AUDIO:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                chooseAudio();
            }
            break;

        case IPMSG_SHARE_POSITION:
            if (isNoRecipient) {
                toastNoRecipients(mContext);
            } else {
                sharePosition();
            }
            break;

        default:
            ret = false;
            Log.e(TAG, "doMoreAction(): invalid share action type: " + action);
            break;
        }
        if (ret) {
            hideSharePanel();
        }
        return ret;
    }

    private class GeoLocCallback implements GeoLocService.callback {

        public void queryGeoLocResult(boolean ret, final Location location) {

            mContext.removeDialog(DIALOG_ID_GETLOC_PROGRESS);
            mGeoLocSrv.removeCallback();
            // if success, store location and show send location confirm dialog
            // if fail, show fail toast
            if (ret == true) {
                mLocation = location;
                mContext.showDialog(DIALOG_ID_GEOLOC_SEND_CONFRIM);
            } else {
                Toast.makeText(mContext, mPluginContext.getString(R.string.geoloc_get_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void sharePosition() {
        mGeoLocSrv = new GeoLocService(mContext);
        queryGeoLocation();
        // sendGeoLocation();
    }

    private void queryGeoLocation() {
        if (mGeoLocSrv.isEnable()) {
            mContext.showDialog(DIALOG_ID_GETLOC_PROGRESS);
        } else {
            Toast.makeText(mContext, mPluginContext.getString(R.string.geoloc_check_gps),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public Dialog onIpCreateDialog(int id) {
        Log.d(TAG, "onCreateDialog, id:" + id);
        switch (id) {
        case DIALOG_ID_GETLOC_PROGRESS:
            mGeoLocSrv.queryCurrentGeoLocation(new GeoLocCallback());
            ProgressDialog dialog = new ProgressDialog(mContext);
            dialog.setMessage(mPluginContext.getString(R.string.geoloc_being_get));
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    mGeoLocSrv.removeCallback();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            return dialog;

        case DIALOG_ID_GEOLOC_SEND_CONFRIM:
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(mPluginContext.getString(R.string.geoloc_send_confrim))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    sendGeoLocation();
                                }
                            }).setNegativeButton(android.R.string.cancel, null);
            // Create the AlertDialog object
            AlertDialog dialog2 = builder.create();
            return dialog2;
        case DIALOG_ID_NOTIFY_SEND_BY_RCS:
            return createNotifySendByRcsDialog();
        default:
            break;
        }
        return super.onIpCreateDialog(id);
    }

    private void sendGeoLocation() {
        String path = RcsMessageUtils.getGeolocPath(mContext);
        String fileName = GeoLocUtils.buildGeoLocXml(mLocation, "Sender Number", "Message Id",
                path);
        mDstPath = fileName;

        // mDstPath = RcsMessageConfig.getGeolocTempPath(mContext) +
        // File.separator + "geoloc_1.xml";
        Log.e(TAG, "sendGeoLocation() , mDstPath = " + mDstPath);

        // mDstPath = path + "geoloc_1.xml";
        mIpMsgHandler.postDelayed(mSendGeoLocation, 100);
    }

    public void takePhoto() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mPhotoFilePath = RcsMessageUtils.getPhotoDstFilePath(mContext);
        mDstPath = mPhotoFilePath;
        File out = new File(mPhotoFilePath);
        Uri uri = Uri.fromFile(out);

        long sizeLimit = RcsMessageUtils.getPhotoSizeLimit();
        String resolutionLimit = RcsMessageUtils.getPhotoResolutionLimit();

        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        imageCaptureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        try {
            mContext.startActivityForResult(imageCaptureIntent, REQUEST_CODE_IPMSG_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "takePhoto()");
        }
    }

    public void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("com.mediatek.gallery3d.extra.RCS_PICKER", true);
        mDstPath = mPhotoFilePath;
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "choosePhoto()");
        }
    }

    public void recordVideo() {
        int durationLimit = RcsMessageUtils.getVideoCaptureDurationLimit();
        String resolutionLimit = RcsMessageUtils.getVideoResolutionLimit();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mVideoFilePath = RcsMessageUtils.getVideoDstFilePath(mContext);
        mDstPath = mVideoFilePath;
        File out = new File(mVideoFilePath);
        Uri uri = Uri.fromFile(out);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
        intent.putExtra("mediatek.intent.extra.EXTRA_RESOLUTION_LIMIT", resolutionLimit);
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_RECORD_VIDEO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "recordVideo()");
        }
    }

    public void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_VIDEO);
        } catch (Exception e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "chooseVideo()");
        }
    }

    public void recordAudio() {
        Log.d(TAG, "recordAudio(), enter");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/amr");
        intent.setClassName("com.android.soundrecorder",
                "com.android.soundrecorder.SoundRecorder");
        intent.putExtra("com.android.soundrecorder.maxduration",
                RcsMessageUtils.getAudioDurationLimit());
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_RECORD_AUDIO);
    }

    private void shareContact() {
        addContacts();
    }

    private void addContacts() {
        Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTICONTACTS");
        intent.setType(Contacts.CONTENT_TYPE);
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_CONTACT);
    }

    private void addGroups() {
        Intent intent = new Intent("android.intent.action.rcs.contacts.GroupListActivity");
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_CONTACT);
    }

    private void chooseAudio() {
        // Not support ringtone
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(ContentType.AUDIO_UNSPECIFIED);
        String[] mimeTypess = new String[] { ContentType.AUDIO_UNSPECIFIED,
                ContentType.AUDIO_MP3, ContentType.AUDIO_3GPP, "audio/M4A",
                ContentType.AUDIO_AAC, ContentType.AUDIO_AMR };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypess);

        // / @}
        mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_CHOOSE_AUDIO);
    }

    private void shareCalendar() {
        Intent intent = new Intent("android.intent.action.CALENDARCHOICE");
        intent.setType("text/x-vcalendar");
        intent.putExtra("request_type", 0);
        try {
            mContext.startActivityForResult(intent, REQUEST_CODE_IPMSG_SHARE_VCALENDAR);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_no_app),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "shareCalendar()");
        }
    }

    private void asyncIpAttachVCardByContactsId(final Intent data) {
        if (data == null) {
            return;
        }
        long[] contactsId = data
                .getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");
        RCSVCardAttachment va = new RCSVCardAttachment(mPluginContext);
        mIpMessageVcardName = va.getVCardFileNameByContactsId(contactsId, true);
        mDstPath = RcsMessageUtils.getVcardTempPath(mContext)
                          + File.separator + mIpMessageVcardName;
        Log.d(TAG, "asyncIpAttachVCardByContactsId(): mIpMessageVcardName = "
                + mIpMessageVcardName + ", mDstPath = " + mDstPath);
        mIpMsgHandler.postDelayed(mSendVcard, 100);
    }

    private void toastNoRecipients(Context context) {
        Toast.makeText(context,
                mPluginContext.getString(R.string.ipmsg_need_input_recipients),
                Toast.LENGTH_SHORT).show();
    }

    private static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.mms",
                "com.android.mms.ui.ComposeMessageActivity"));
        if (threadId > 0) {
            intent.setData(getUri(threadId));
        }
        return intent;
    }

    private static Uri getUri(long threadId) {
        return ContentUris.withAppendedId(Threads.CONTENT_URI, threadId);
    }

    private boolean isRecipientsEditorVisible() {
        return mCallback.isIpRecipientEditorVisible();
    }

    private List<String> getRecipientsList() {
        List<String> list;
        if (isRecipientsEditorVisible()) {
            list = mCallback.getRecipientsEditorInfoList();
        } else {
            String[] numbers = mCallback.getConversationInfo();
            list = new ArrayList<String>(numbers.length);
            for (String number : numbers) {
                list.add(number);
            }
        }
        return list;
    }

    private String formatRecipientsStr(List<String> list) {
        StringBuffer builder = new StringBuffer();
        for (String number : list) {
            if (!TextUtils.isEmpty(number)) {
                builder.append(number);
                builder.append(",");
            }
        }
        String recipientStr = builder.toString();
        if (recipientStr.endsWith(",")) {
            recipientStr = recipientStr.substring(0, recipientStr.lastIndexOf(","));
        }
        return recipientStr;
    }

    private String getRecipientStr() {
        String ret = "";
        List<String> list = getRecipientsList();
        if (list != null && list.size() > 0) {
            ret = formatRecipientsStr(list);
        }
        Log.d(TAG, "getRecipientStr: " + ret);
        return ret;
    }

    private String getNumber() {
        List<String> list = getRecipientsList();
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return "";
    }

    private int getRecipientSize() {
        int ret = 0;
        List<String> list = getRecipientsList();
        if (list != null) {
            ret = list.size();
        }
        Log.d(TAG, "getRecipientSize: " + ret);
        return ret;
    }

    private void hideInputMethod() {
        Log.d(TAG, "hideInputMethod()");
        if (mContext.getWindow() != null && mContext.getWindow().getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(mContext.getWindow().getCurrentFocus()
                    .getWindowToken(), 0);
        }
    }

    public void onIpRecipientsChipChanged(int number) {
        if (number > 0) {
            enableShareButton(true);
            if (mDisplayBurned) {
                if (number == 1) {
                    mSendButtonIpMessage.setClickable(true);
                    mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                            .getDrawable(R.drawable.ic_send_ipbar));
                } else {
                    mSendButtonIpMessage.setClickable(false);
                    if (mTextEditor != null && !mTextEditor.getText().toString().isEmpty()) {
                        mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                                .getDrawable(R.drawable.ic_send_rcs_actived));
                    } else {
                        mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                                .getDrawable(R.drawable.ic_send_rcs));
                    }
                    Toast.makeText(mContext, mPluginContext.getString(R.string.ipmsg_burn_cap_indicator),
                            Toast.LENGTH_LONG).show();
                }
            }
        } else {
            enableShareButton(false);
        }
    }

    private void copyFile(IpAttachMessage ipAttachMessage) {
        Log.d(TAG, "copyFile type = " + ipAttachMessage.getType());
        String source = ipAttachMessage.getPath();
        if (TextUtils.isEmpty(source)) {
            Log.e(TAG, "saveMsgInSDCard(): save ipattachmessage failed, source empty!");
            Toast.makeText(mContext, mPluginContext.getString(R.string.copy_to_sdcard_fail),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File inFile = new File(source);
        if (!inFile.exists()) {
            Log.e(TAG,
                    "saveMsgInSDCard(): save ipattachmessage failed, source file not exist!");
            return;
        }
        String attName = source.substring(source.lastIndexOf("/") + 1);
        File dstFile = RcsMessageUtils.getStorageFile(attName);
        if (dstFile == null) {
            Log.i(TAG, "saveMsgInSDCard(): save ipattachmessage failed, dstFile not exist!");
            return;
        }
        RcsMessageUtils.copy(inFile, dstFile);
        Toast.makeText(mContext, mPluginContext.getString(R.string.copy_to_sdcard_success),
                Toast.LENGTH_SHORT).show();
        // Notify other applications listening to scanner events
        // that a media file has been added to the sd card
        mPluginContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                .fromFile(dstFile)));
    }

    private boolean setSmsFavorite(Context mContext, long id, String mBody,
            ContentValues values) {
        Log.d(TAG, "setSmsFavorite id =" + id + ",mBody = " + mBody);
        values.put(FavoriteMsgData.COLUMN_DA_ID, id);
        values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.SMS);
        values.put(FavoriteMsgData.COLUMN_DA_BODY, mBody);
        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
        return true;
    }

    private boolean setMmsFavorite(Context mContext, long id, String mSubject,
            int mBoxId, int type, ContentValues values) {
        Log.d(TAG, "setMmsFavorite id =" + id + ",mSubject = " +
            mSubject + ",mBoxId = " + mBoxId);
        byte[] pduMid;
        String pduFilePath = RcsMessageUtils.getFavoritePath(mContext, "favorite_pdu");
        Log.d(TAG, "thiss pduFilePath =" + pduFilePath);
        if (pduFilePath == null) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.toast_favorite_fail),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        try {
            Log.d(TAG, "thiss cache id =" + id);
            Log.d(TAG, "thiss time =" + System.currentTimeMillis());
            Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
            Log.d(TAG, "thiss realUri =" + realUri);
            PduPersister p = PduPersister.getPduPersister(mContext);
            Log.d(TAG, "thiss mBoxId =" + mBoxId);
            if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
                    pduMid = null;
                } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
                    RetrieveConf rPdu = (RetrieveConf) p.load(realUri, false);
                    pduMid = new PduComposer(mContext, rPdu).make(false);
                    values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.INCOMING);
                } else {
                    pduMid = null;
                }
            } else {
                SendReq sPdu = (SendReq) p.load(realUri);
                pduMid = new PduComposer(mContext, sPdu).make();
                values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.OUTCOMING);
                Log.d(TAG, "thiss SendReq pduMid =" + pduMid);
            }
            String mFile = pduFilePath + "/" + System.currentTimeMillis() + ".pdu";
            if (pduMid != null) {
                byte[] pduByteArray = pduMid;
                Log.d(TAG, "thiss fileName =" + mFile);
                writeToFile(mFile, pduByteArray);
            }
            if (pduMid != null) {
                values.put(FavoriteMsgData.COLUMN_DA_ID, Long.toString(id));
                values.put(FavoriteMsgData.COLUMN_DA_FILENAME, mFile);
                values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.MMS);
                if (!TextUtils.isEmpty(mSubject)) {
                    values.put(FavoriteMsgData.COLUMN_DA_BODY, mSubject);
                }
                values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
                mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean setTextIpmessageFavorite(long id, int ct, String mBody,
            ContentValues values) {
        Log.d(TAG, "setTextIpmessageFavorite id =" + id + ",mBody = " + mBody);
        values.put(FavoriteMsgData.COLUMN_DA_MIME_TYPE, "text/plain");
        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
        values.put(FavoriteMsgData.COLUMN_DA_BODY, mBody);
        if (ct == IpMessageType.EMOTICON) {
            values.put(FavoriteMsgData.COLUMN_DA_TYPE, FavoriteMsgProvider.FAVORITEEMOJI);
        } else {
            values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.IM);
        }
        if (mGroupChatId != null) {
            values.put(FavoriteMsgData.COLUMN_CHATID, mGroupChatId);
        }
        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
        return true;
    }

    private boolean setAttachIpmessageFavorite(long id, IpAttachMessage ipAttachMessage,
            ContentValues values) {
        values.put(FavoriteMsgData.COLUMN_DA_FILEICON, ipAttachMessage.getSize());

        int ct = ipAttachMessage.getType();
        String mPath = ipAttachMessage.getPath();
           String thumbPath = null;
        if (ct == IpMessageType.PICTURE) {
            IpImageMessage imageMessage = (IpImageMessage) ipAttachMessage;
            thumbPath = imageMessage.getThumbPath();
        } else if (ct == IpMessageType.VIDEO) {
            IpVideoMessage videoMessage = (IpVideoMessage) ipAttachMessage;
            thumbPath = videoMessage.getThumbPath();
        }
        Log.d(TAG, "setAttachIpmessageFavorite id =" + id + ",mPath = " + mPath);
        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
        values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.FT);
        if (mGroupChatId != null) {
            values.put(FavoriteMsgData.COLUMN_CHATID, mGroupChatId);
        }
        if (mPath != null) {
            String imFilePath = RcsMessageUtils.getFavoritePath(mContext, "favorite_ipmessage");
            Log.d(TAG, "thiss imFilePath =" + imFilePath);
            if (imFilePath == null) {
                Toast.makeText(mContext, mPluginContext.getString(R.string.toast_favorite_fail),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            String fileName = imFilePath + "/" + getFileName(mPath);
            String newpath = RcsMessageUtils.getUniqueFileName(fileName);
            Log.d(TAG, "thiss newpath =" + newpath);
            RcsMessageUtils.copy(mPath, newpath);
            values.put(FavoriteMsgData.COLUMN_DA_FILENAME, newpath);
            String mimeType = RCSUtils.getFileType(getFileName(mPath));
            if (mimeType != null) {
                values.put(FavoriteMsgData.COLUMN_DA_MIME_TYPE, mimeType);
            }
        }
        if (thumbPath != null) {

        }


        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
        return true;
    }

    private String getFileName(String mFile) {
        return mFile.substring(mFile.lastIndexOf("/") + 1);
    }

    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onIpKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown(): keyCode = " + keyCode);
        switch (keyCode) {
        case KeyEvent.KEYCODE_DEL:
            break;

        case KeyEvent.KEYCODE_BACK:
            if (isSharePanelShow()) {
                hideSharePanel();
                return true;
            }
            if (mEmoji != null && mEmoji.isEmojiPanelShow()) {
                mEmoji.showEmojiPanel(false);
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onIPQueryMsgList(AsyncQueryHandler mQueryHandler, int token, Object cookie,
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        String[] rcsProjections = RcsMessageUtils.combineTwoStringArrays(projection,
                RcsMessageListAdapter.RCS_MESSAGE_PROJECTION_EXTENDS);
        Uri rcsUri = getRcsConversation().getUri();
        mQueryHandler.startQuery(token, cookie, rcsUri, rcsProjections, selection, selectionArgs,
                orderBy);
        setChatType();
        return true;
    }

    public boolean startMsgListQuery(AsyncQueryHandler queryHandler, int token, Object cookie,
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        return onIPQueryMsgList(queryHandler, token, cookie, uri, projection, selection,
                selectionArgs, orderBy);
    }

    @Override
    public void onIpConfig(Configuration newConfig) {
        Log.d(TAG, "onIpConfig()");
        if (mEmoji != null && mEmoji.isEmojiPanelShow()) {
            mEmoji.resetEmojiPanel(newConfig);
        }
        if (mSharePanel != null) {
            mSharePanel.resetShareItem();
        }
    }

    private Dialog createInvitationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder
                .setTitle(mPluginContext.getString(R.string.group_chat))
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(mPluginContext.getString(R.string.group_invitation_indicater))
                .setPositiveButton(mPluginContext.getString(R.string.group_accept),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // accept invitation
                                ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(
                                        mGroupChatId);
                                if (info != null && info.getStatus() == IpMessageConsts.
                                           GroupActionList.GROUP_STATUS_INVITE_EXPAIRED) {
                                    showInvitaionTimeOutDialog();
                                } else {
                                    if (mIsSmsEnabled) {
                                        boolean ret = GroupManager.getInstance()
                                                .acceptGroupInvitation(mGroupChatId);
                                        if (ret == false) {
                                            Toast.makeText(mContext, R.string.group_aborted,
                                                    Toast.LENGTH_SHORT).show();
                                            mContext.finish();
                                        } else {
                                            // setChatActive(true);
                                            showProgressDialog(mPluginContext
                                                    .getString(R.string.pref_please_wait));
                                        }
                                    }
                                }
                            }
                        })
                .setNegativeButton(mPluginContext.getString(R.string.group_reject),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // reject invitation
                                GroupManager.getInstance().rejectGroupInvitation(
                                        mGroupChatId);
                                showProgressDialog(mPluginContext
                                        .getString(R.string.pref_please_wait));
                            }
                        })
                .setNeutralButton(mPluginContext.getString(R.string.group_shelve),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // don't process now. do noting but exit this
                                // screen
                                mContext.finish();
                            }
                        });
        Dialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void showInvitaionDialog(final int status) {
        mGroupStatus = status;
        if (mInvitationDialog == null) {
            mInvitationDialog = createInvitationDialog();
        }
        if (!mInvitationDialog.isShowing()) {
            mInvitationDialog.show();
        }
    }

    private void dismissInvitationDialog() {
        if (mInvitationDialog != null && mInvitationDialog.isShowing()) {
            mInvitationDialog.dismiss();
        }
    }

    private Dialog createInvitationTimeOutDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mPluginContext.getString(R.string.group_chat))
                .setCancelable(false).setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(mPluginContext.getString(R.string.group_invitation_expired))
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (mRcsMessageListAdapter == null
                                || mRcsMessageListAdapter.isOnlyHasSystemMsg()) {
                            HashSet<Long> set = new HashSet<Long>();
                            set.add(mThreadId);
                            mRcsMessageManager.deleteRCSThreads(set, 0, true);
                            mContext.finish();
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(Threads.STATUS,
                                    GroupActionList.GROUP_STATUS_INVALID);
                            Uri uri = ContentUris.withAppendedId(
                                    RCSUtils.URI_THREADS_UPDATE_STATUS, mThreadId);
                            if (mIsSmsEnabled) {
                                mContext.getContentResolver().update(uri, values, null, null);
                            }
                            setChatActive(false);
                        }
                    }
                });
        Dialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void showInvitaionTimeOutDialog() {
        if (mInvitationExpiredDialog == null) {
            mInvitationExpiredDialog = createInvitationTimeOutDialog();
        }
        if (!mInvitationExpiredDialog.isShowing()) {
            mInvitationExpiredDialog.show();
        }
    }

    private void dismissInvitationTimeOutDialog() {
        if (mInvitationExpiredDialog != null && mInvitationExpiredDialog.isShowing()) {
            mInvitationExpiredDialog.dismiss();
        }
    }

    @Override
    public boolean onIpCheckRecipientsCount() {
        if (mIsGroupChat) {
            mCallback.callbackCheckConditionsAndSendMessage(true);
            return true;

        } else {
            return super.onIpCheckRecipientsCount();
        }
    }

    @Override
    public boolean isIpRecipientCallable(String[] numbers) {
        if (mIsGroupChat) {
            if (mIsChatActive && mGroup.getParticipants().size() > 0) {
                return true;
            }
        } else if (numbers.length > 0) {
            return true;
        }
        return super.isIpRecipientCallable(numbers);
    }

    /**
     * Override DefaultIpComposeActivityExt's showIpMessageDetails.
     * @param msgItem IIpMessageItemExt
     * @return true if show.
     */
    public boolean showIpMessageDetails(IIpMessageItemExt msgItem) {
        if (msgItem == null || !(msgItem instanceof RcsMessageItem)) {
            return false;
        }
//        if (!mIsGroupChat) {
//            return false;
//        }
        RcsMessageItem item = (RcsMessageItem) msgItem;
        if (!item.mType.equals("rcs")) {
            return false;
        }
        Resources res = mPluginContext.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(res.getString(R.string.message_details_title))
        .setMessage(RcsMessageListItem.getIpTextMessageDetails(mPluginContext, item))
        .setCancelable(true)
        .show();
        return true;
    }
    private boolean isMms() {
        boolean ret = false;
        mWorkingMessage = mCallback.getWorkingMessage();
        if (mWorkingMessage != null) {
            ret = mWorkingMessage.requiresIpMms();
        }
        return ret;
    }

    private boolean convertTextMmsToRcsIfNeeded() {
        boolean ret = false;
        if (mServiceRegistered) {
            mWorkingMessage = mCallback.getWorkingMessage();
            if (mWorkingMessage != null) {
                if (mWorkingMessage.requiresIpMms()) {
                    CharSequence content = mWorkingMessage.getIpText();
                    if (mTextEditor != null && !TextUtils.isEmpty(content)) {
                        //firt clear textedit
                        mTextEditor.setText("");
                        mTextEditor.setText(content);
                        ret = true;
                    }
                }
            }
        }
        return ret;
    }

    private void invalidateOptionMenu() {
        if (mCallback != null) {
            mCallback.invalidateIpOptionsMenu();
        }
    }

    private void setChatType() {
        if (mRcsMessageListAdapter == null) {
            return;
        }
        if (mIsGroupChat) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_GROUP);
        } else if (getRecipientSize() == 1) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_ONE2ONE);
        } else if (getRecipientSize() > 1) {
            mRcsMessageListAdapter.setChatType(mRcsMessageListAdapter.CHAT_TYPE_ONE2MULTI);
        } else {
            Log.d(TAG, "setChatType(): unknown chat type");
        }
    }

    private void setChatActive(final boolean active) {
        mIsChatActive = active;
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                showEditZone(active);
                if (mRcsMessageListAdapter != null) {
                    mRcsMessageListAdapter.setChatActive(active);
                }
                invalidateOptionMenu();
                if (!active) {
                    mContext.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                    mCallback.hideIpInputMethod();
                    if (mTextEditor != null) {
                        mTextEditor.clearFocus();
                    }
                }
            }
        });
    }

    private void showEditZone(boolean show) {
        int visible = show ? View.VISIBLE : View.GONE;
        if (mEditZone != null) {
            mEditZone.setVisibility(visible);
            if (mTextEditor != null) {
                mTextEditor.setVisibility(visible);
            }
        }
    }

    private int getLastSendMode() {
        SharedPreferences preferece = mContext.getSharedPreferences(
                PREFERENCE_SEND_WAY_CHANGED_NAME, Context.MODE_WORLD_READABLE);
        return preferece.getInt(PREFERENCE_KEY_SEND_WAY, PREFERENCE_VALUE_SEND_WAY_UNKNOWN);
    }


    private Dialog createNotifySendByRcsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mPluginContext.getString(R.string.send_way_use_im_indicate))
               .setTitle(mPluginContext.getString(R.string.send_way_use_im_Title))
               .setCancelable(false)
               .setPositiveButton(android.R.string.ok,new OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //send by rcs
                                String body = mTextEditor.getText().toString();
                                if (body != null && body.length() > 0) {
                                    sendIpTextMessage();
                                }
                                RcsMessageUtils.updateNeedNotifyUserWhenToRcsValue(mPluginContext,
                                        mSendSubId, false);
                            }
                        })
               .setNegativeButton(mPluginContext.getString(R.string.send_way_set),
                                               new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO to set
                    RcsMessageUtils.updateNeedNotifyUserWhenToRcsValue(mPluginContext,
                                            mSendSubId, false);
                    RcsMessageUtils.startRcsSettingActivity(mContext,
                           REQUEST_CODE_START_RCSSETTING_ACTIVITY);
                }
            });
        return builder.create();
    }

    int mCheckedContactNumber = 0;
    int mCheckedContactIndex = 0;
    private static final int MAX_CONTACT_NUMBER_FOR_MULTI_CALL = 5;
    private void showMultiSelectContactDialogForCall(final ArrayList<Participant> participants) {
        final boolean[] checkedArray = new boolean[participants.size()];
        final String[] sourceArray = new String[participants.size()];
        int index = 0;
        for (Participant p : participants) {
            String content = p.getDisplayName();
            if (TextUtils.isEmpty(content)) {
                content = p.getContact();
            }
            sourceArray[index] = content;
            index++;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMultiChoiceItems(sourceArray, checkedArray,
                new OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int posion,
                            boolean checked) {
                        AlertDialog dialog = (AlertDialog) arg0;
                        Log.d(TAG, "setMultiChoiceItems: " + posion + ", " + checked);
                        if (checked) {
                            mCheckedContactNumber++;
                            if (mCheckedContactNumber == MAX_CONTACT_NUMBER_FOR_MULTI_CALL + 1) {
                                dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(
                                        false);
                            }
                        } else {
                            mCheckedContactNumber--;
                            if (mCheckedContactNumber == MAX_CONTACT_NUMBER_FOR_MULTI_CALL) {
                                dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(
                                        true);
                            }
                        }
                    }
                })
                .setTitle(mPluginContext.getString(R.string.multi_select_contact_title))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int id) {
                                ArrayList<String> selectedNumbers = new ArrayList<String>();
                                int totalNumber = checkedArray.length;
                                StringBuilder contentBuilder = new StringBuilder("");
                                for (int checkedIndex = 0; checkedIndex < totalNumber;
                                        checkedIndex++) {
                                    if (checkedArray[checkedIndex]) {
                                        selectedNumbers.add(participants.get(
                                                checkedIndex).getContact());
                                    }
                                }
                                startMultiCall(selectedNumbers);
                            }
                        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showSingleSelectContactDialogForCall(final ArrayList<Participant> participants) {
        int count = participants.size();
        Log.d(TAG, "showSingleSelectContactDialogForCall: count: " + count);
        if (count < 1) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final String[] sourceArray = new String[participants.size()];
        int index = 0;
        for (Participant p : participants) {
            String content = p.getDisplayName();
            if (TextUtils.isEmpty(content)) {
                content = p.getContact();
            }
            sourceArray[index] = content;
            index++;
        }
        builder.setSingleChoiceItems(sourceArray, 0, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCheckedContactIndex = which;
                    }
                })
                .setTitle(mPluginContext.getString(R.string.single_select_contact_title))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<String> selectedNumbers = new ArrayList<String>();
                        selectedNumbers.add(participants.get(mCheckedContactIndex).getContact());
                        startMultiCall(selectedNumbers);
                    }
                });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void startMultiCall(ArrayList<String> numbers) {
        if (numbers == null || numbers.size() == 0) {
            Toast.makeText(mContext, mPluginContext.getString(R.string.no_contact_selected),
                                    Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.fromParts("tel", numbers.get(0), null);
        final Intent intent;
        if (numbers.size() > 1) {
            intent = new Intent(Intent.ACTION_CALL, uri);
            intent.putExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, true);
            intent.putStringArrayListExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS, numbers);
        } else {
            intent = new Intent(Intent.ACTION_DIAL, uri);
        }
        mContext.startActivity(intent);
    }

    private int getIpAutoSelectSubId() {
        if (isRecipientsEditorVisible()) {
            int userSuggestionId = mContext.getIntent().getIntExtra(
                    PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (userSuggestionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return userSuggestionId;
            } else {
                SubscriptionInfo subInfoRecord = SubscriptionManager.from(
                        mContext).getActiveSubscriptionInfoForSimSlotIndex(0);
                if (subInfoRecord != null) {
                    int subId = subInfoRecord.getSubscriptionId();
                    Log.d(TAG, "getIpAutoSelectSubId subId = " + subId);
                    return subId;
                }
            }
        }
        Log.d(TAG, "getIpAutoSelectSubId isRecipientsEditorVisible false");
        return -1;
    }

    /**
     * get current composer instance
     * @return RcsComposeActivity
     */
    public static RcsComposeActivity getRcsComposer() {
        int size = sComposerList.size();
        if (size > 0) {
            return sComposerList.get(size - 1);
        } else {
            return null;
        }
    }

    /**
     * Process new Invitation when the new invitation's threadid is same as current.
     * @param threadId thread id
     * @return true processed, or false
     */
    public boolean processNewInvitation(long threadId) {
        Log.d(TAG, "processNewInvitation: threadId = " + threadId);
        if (sComposerList.contains(this) && mThreadId == threadId) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    showInvitaionDialog(GroupActionList.GROUP_STATUS_INVITING_AGAIN);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private OnMessageListChangedListener mMsgListChangedListener =
                                                            new OnMessageListChangedListener() {

        @Override
        public void onChanged() {
            // TODO Auto-generated method stub
            updateRcsMode();
            if (mIsGroupChat) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mCallback.hideIpRecipientEditor();
                    }
                });
            }
        }
    };

    private void updateRcsMode() {
        boolean modeChanged = false;
        boolean rcsMode = true;
        if (!mIsGroupChat) {
            //group chat always is rcs mode
            if (isMms()) {
                Log.d(TAG, "[updateRcsMode]: current is mms");
                rcsMode = false;
            } else if (!mServiceRegistered) {
                Log.d(TAG, "[updateRcsMode]: mServiceRegistered is false");
                rcsMode = false;
            } else {
                List<SubscriptionInfo> subInfos = SubscriptionManager.from(mContext)
                        .getActiveSubscriptionInfoList();
                int subCount = (subInfos == null || subInfos.isEmpty()) ? 0 : subInfos.size();
                int rcsSubId = RcsMessageUtils.getRcsSubId(mContext);
                int userSelectedId = RcsMessageUtils.getUserSelectedId(mContext);
                Log.d(TAG, "[updateRcsMode]rcsSubId is: " + rcsSubId + "subCount = " + subCount +
                        ", userSelectedId = " + userSelectedId);
                if (subCount == 0 || rcsSubId == -1) {
                    rcsMode = false;
                } else if (subCount > 1) {
                    if (userSelectedId == (int) Settings.System.SMS_SIM_SETTING_AUTO) {
                    //auto
                    if (isRecipientsEditorVisible()) {
                        int userSuggestionId = (int) mContext.getIntent().getLongExtra(
                                PhoneConstants.SUBSCRIPTION_KEY,
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                        if (userSuggestionId != (int) SubscriptionManager.INVALID_SUBSCRIPTION_ID
                                && userSuggestionId != rcsSubId) {
                            rcsMode = false;
                        }
                    } else if (mRcsMessageListAdapter != null) {
                        int lastMsgSubId = (int) mRcsMessageListAdapter.getAutoSelectSubId();
                        Log.d(TAG, "[updateRcsMode]last message subid is: " + lastMsgSubId);
                        if (rcsSubId != lastMsgSubId && lastMsgSubId != -1) {
                            // not rcs subid
                            rcsMode = false;
                        }
                    }
                    } else if (userSelectedId != rcsSubId) {
                        rcsMode = false;
                    }
                }
            }
        }
        Log.d(TAG, "[updateRcsMode]: old = " + mIsRcsMode + ", new = " + rcsMode);
        if (rcsMode != mIsRcsMode) {
            mIsRcsMode = rcsMode;
            // rcs mode changed
            if (mSharePanel != null) {
                mSharePanel.resetShareItem();
            }
            mCallback.drawIpBottomPanel();
            if (rcsMode) {
                if (mTextCounter != null) {
                    mTextCounter.setVisibility(View.GONE);
                }
            }
        }
    }

    public boolean onIpProtocolChanged(boolean mms, boolean needToast) {
        updateRcsMode();
        return false;
    }

    public boolean toastIpConvertInfo(boolean toMms) {
        if (!toMms && mIsRcsMode) {
            Toast.makeText(mContext,
                    mPluginContext.getString(R.string.converting_to_rcs_message),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /**
     * Is Rcs Mode.
     * @return true if edit rcs message or false
     */
    public boolean isRcsMode() {
        return mIsRcsMode;
    }

    private BroadcastReceiver mChangeSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null
                    && action.equals(TelephonyIntents.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED)) {
                int subIdinSetting = (int) intent.getLongExtra(
                        PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                updateRcsMode();
 //               initDualSimState();
            }
        }
    };

    private ProgressDialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        return dialog;
    }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDialog();
        }
        if (TextUtils.isEmpty(msg)) {
            mProgressDialog.setMessage("");
        } else {
            mProgressDialog.setMessage(msg);
        }
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    IInitGroupListener mInitGroupListener = new IInitGroupListener() {

        @Override
        public void onRejectGroupInvitationResult(final int result, final long threadId,
                final String chatId) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onRejectGroupInvitationResult: result : " + result);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mThreadId == threadId) {
                        dismissProgressDialog();
                        if (result != GroupActionList.VALUE_SUCCESS) {
                            // fail
                            Toast.makeText(mContext,
                                    mPluginContext.getString(R.string.group_reject_fail),
                                    Toast.LENGTH_SHORT).show();
                            showInvitaionDialog(mGroupStatus);
                        } else {
                            if (mGroupStatus ==
                                    GroupActionList.GROUP_STATUS_INVITING
                                    || mRcsMessageListAdapter == null
                                    || mRcsMessageListAdapter.isOnlyHasSystemMsg()) {
                                mContext.finish();
                                HashSet<Long> set = new HashSet<Long>();
                                set.add(mThreadId);
                                mRcsMessageManager.deleteRCSThreads(set,0, true);
                            } else if (mGroupStatus ==
                                    GroupActionList.GROUP_STATUS_INVITING_AGAIN) {
                                setChatActive(false);
                                ContentValues values = new ContentValues();
                                values.put(Threads.STATUS,
                                        GroupActionList.GROUP_STATUS_INVALID);
                                Uri uri = ContentUris.withAppendedId(
                                        RCSUtils.URI_THREADS_UPDATE_STATUS, mThreadId);
                                try {
                                    mContext.getContentResolver().update(uri, values, null,
                                            null);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                    Log.e(TAG, "[showInvitaionTimeOutDialog]: e = " + e);
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onInitGroupResult(int result, long threadId, String chatId) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAcceptGroupInvitationResult(final int result, final long threadId,
                final String chatId) {
            Log.d(TAG, "onAcceptGroupInvitationResult: result : " + result);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mThreadId == threadId) {
                        dismissProgressDialog();
                        if (result == GroupActionList.VALUE_SUCCESS) {
                            // success
                            // Toast.makeText(mContext, "Accept result sucess",
                            // Toast.LENGTH_SHORT).show();
                            setChatActive(true);
                        } else {
                            Toast.makeText(mContext,
                                    mPluginContext.getString(R.string.group_accept_fail),
                                    Toast.LENGTH_SHORT).show();
                            showInvitaionDialog(mGroupStatus);
                        }
                    }
                }
            });
        }
    };

    private void setupCallStatusServiceConnection() {
        Log.i(TAG, "setupCallStatusServiceConnection.");
        if (mCallStatusService == null || mCallConnection == null) {
            mCallConnection = new CallStatusServiceConnection();
            boolean failedConnection = false;

            Intent intent = getCallStatusServiceIntent();
            if (!mContext.bindService(intent, mCallConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Bind service failed!");
                mCallConnection = null;
                failedConnection = true;
            } else {
                Log.d(TAG, "Bind service successfully!");
            }
        } else {
            Log.d(TAG, "Alreay bind service!");
        }
    }

    private void unbindCallStatusService() {
        Log.i(TAG, "unbindCallStatusService.");
        if (mCallStatusService != null) {
            unregisterCallListener();
            mContext.unbindService(mCallConnection);
            mCallStatusService = null;
        }
        mCallConnection = null;
    }

    private Intent getCallStatusServiceIntent() {
        Log.i(TAG, "getCallStatusServiceIntent.");
        final Intent intent = new Intent(ICallStatusService.class.getName());
        final ComponentName component = new ComponentName("com.mediatek.rcs.phone",
                "com.mediatek.rcs.incallui.service.CallStatusService");
        intent.setComponent(component);
        return intent;
    }

    private final IServiceMessageCallback.Stub mCallServiceListener =
                                              new IServiceMessageCallback.Stub() {
        @Override
        public void updateMsgStatus(final String name, final String status, final String time) {
            try {
                // updatedMessageInfo(name, status, time);
                Log.i(TAG, "updateMsgStatus: name = " + name + ", status = " + status
                        + ", time = " + time);
                mContext.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.VISIBLE);
                        mShowCallTimeText.setText(status + "  " + time);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updateMsgStatus", e);
            }
        }

        @Override
        public void stopfromClient() {
            try {
                Log.i(TAG, "[IServiceMessageCallback]stopfromClient");
                unbindCallStatusService();
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.GONE);
                        mIsNeedShowCallTime = false;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error stopfromClient", e);
            }
        }
    };

    private void registerCallListener() {
        Log.d(TAG, "registerCallback.");
        try {
            if (mCallStatusService != null) {
                mCallStatusService.registerMessageCallback(mCallServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unregisterCallListener() {
        Log.d(TAG, "unregisterCallback.");
        try {
            if (mCallStatusService != null) {
                mCallStatusService.unregisterMessageCallback(mCallServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for connect CallStatus from Call service.
     *
     */
    private class CallStatusServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected.");
            if (mCallStatusService != null) {
                Log.d(TAG, "Service alreay connected, service = " + mCallStatusService);
                return;
            }
            mCallStatusService = ICallStatusService.Stub.asInterface(service);
            registerCallListener();
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected.");
            if (mCallStatusService != null) {
                unregisterCallListener();
                mContext.unbindService(mCallConnection);
                mCallStatusService = null;
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mShowCallTimeText.setVisibility(View.GONE);
                        mIsNeedShowCallTime = false;
                    }
                }, 1500);
            }
            mCallConnection = null;
        }
    }

    @Override
    public void onServiceStateChanged(final int state, final boolean activated,
            final boolean configured, final boolean registered) {
        Log.d(TAG, "[onServiceStateChanged]: (activated, configured, registered): " + activated +
                        configured + registered);
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                boolean enable = activated && configured;
                Log.d(TAG, "[onServiceStateChanged]: mDisplayBurned mServiceEnabled : " +
                   mServiceEnabled + " enable: " + enable + " mDisplayBurned: " + mDisplayBurned);
                if (mServiceEnabled != enable) {
                    mServiceEnabled = enable;
                    invalidateOptionMenu();
                    if (mIsGroupChat) {
                        setChatActive(mServiceEnabled);
                    }

                    mContext.closeContextMenu();
                    ListAdapter adapter = mListView.getAdapter();
                    if (adapter instanceof CursorAdapter) {
                        CursorAdapter cursorAdapter = (CursorAdapter) adapter;
                        cursorAdapter.notifyDataSetChanged();
                    }
                    setEmojiActive(enable);
                }
                Log.d(TAG, "[onServiceStateChanged]: mDisplayBurned mServiceRegistered : " +
                        mServiceRegistered + " registered: " + registered);
                if (mServiceRegistered != registered) {
                    mServiceRegistered = registered;
                    convertTextMmsToRcsIfNeeded();
                    updateRcsMode();
                    //update emoticon items if rcs state changed
                    ListAdapter adapter = mListView.getAdapter();
                    if (adapter instanceof CursorAdapter) {
                        CursorAdapter cursorAdapter = (CursorAdapter) adapter;
                        cursorAdapter.notifyDataSetChanged();
                    }
                }
                if (!enable || !registered) {
                    quitBurnedMode();
                }
            }
        });
    }

    private void quitBurnedMode() {
        Log.d(TAG, "[onServiceStateChanged]: mDisplayBurned quitBurnedMode() ");
        mDisplayBurned = false;
        mOldcontact = null;
        mBurnedCapbility = false;
        if (mTextEditor != null && !mTextEditor.getText().toString().isEmpty()) {
            mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ic_send_ipmsg));
        } else {
            mSendButtonIpMessage.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ic_send_sms_unsend));
        }
        RcsMessageConfig.setEditingDisplayBurnedMsg(mDisplayBurned);
//        mCallback.resetSharePanel();
        mSharePanel.resetShareItem();
    }

    RCSGroup.SimpleGroupActionListener mGroupListener = new RCSGroup.SimpleGroupActionListener() {

        @Override
        public void onGroupAborted() {
            // TODO Auto-generated method stub
            Log.d(TAG, "onGroupAborted");
            setChatActive(false);
            // Toast.makeText(mContext, "add onGroupAborted",
            // Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSubjectModified(String newSubject) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onSubjectModified: " + newSubject);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mCallback.updateIpTitle();
                }
            });
        }

        @Override
        public void onMeRemoved() {
            // TODO Auto-generated method stub
            Log.d(TAG, "onMeRemoved");
            mContext.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    setChatActive(false);
                }
            });
        }
    };

    private RcsConversation getRcsConversation() {
        ComposeMessageActivity composer = (ComposeMessageActivity) mContext;
        return (RcsConversation) composer.getIpConversation();
    }

    private List<IIpContactExt> getIpContactList() {
        ComposeMessageActivity composer = (ComposeMessageActivity) mContext;
        Conversation conversation = composer.getConversation();
        if (conversation == null) {
            Log.e(TAG, "conversation can not be null");
            return null;
        }
        return conversation.getIpContactList();
    }

    /// sharepanel begin
    public boolean onIpLayoutSizeChanged(boolean isSoftKeyBoardShow) {
        mIsSoftKeyBoardShow = isSoftKeyBoardShow;
        return isSharePanelShow();
    }

    public void onIpRecipientsEditorTouch() {
        showSharePanel(false);
    }

    public void onIpSubjectTextEditorFocusChange(boolean hasFocus) {
        if (hasFocus) {
            showSharePanel(false);
        }
    }

    public void onIpSubjectTextEditorTouch() {
        showSharePanel(false);
    }

    public void onIpTextEditorTouch() {
        if (mShowKeyBoardFromShare) {
            showSharePanel(false);
            updateIpFullScreenTextEditorHeight();
        }
        if (mEmoji != null && mEmoji.isEmojiPanelShow()) {
            mEmoji.showEmojiPanel(false);
        }
    }

    public void resetIpConfiguration(boolean isLandscapeOld,
            boolean isLandscapeNew, boolean isSoftKeyBoardShow) {
        mIsLandscape = isLandscapeNew;
        if (!isLandscapeNew && isLandscapeOld == isLandscapeNew && isSoftKeyBoardShow) {
            showSharePanel(false);
        }
    }

    public boolean updateIpFullScreenTextEditorHeight() {
        return isSharePanelShow();
    }

    public boolean isSharePanelShow() {
        if (null != mSharePanel && mSharePanel.isShown()) {
            return true;
        }
        return false;
    }

    private void initShareResource() {
        mShareButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowKeyBoardFromShare) {
                    showSharePanelOrKeyboard(false, true);
                } else {
                    Log.d(TAG, "initShareResource mShowKeyBoardFromShare false");
                    if (mEmoji != null) {
                        mEmoji.showEmojiPanel(false);
                    }
                    showSharePanelOrKeyboard(true, false);
                    mTextEditor.requestFocus();
                }
            }
        });
        mSharePanel.setHandler(mIpMsgHandler);
        showSharePanelOrKeyboard(false, false);
    }

    private void showSharePanel(boolean isShow) {
        if (null != mSharePanel) {
            if (!mIsSmsEnabled) {
                mSharePanel.setVisibility(View.GONE);
                mShareButton.setClickable(false);
                mShareButton.setImageDrawable(mPluginContext.getResources()
                        .getDrawable(R.drawable.ipmsg_share_disable));
                return;
            }
            if (isShow) {
                mSharePanel.setVisibility(View.VISIBLE);
                mShareButton.setImageDrawable(mPluginContext.getResources()
                        .getDrawable(R.drawable.ipmsg_keyboard));
            } else {
                mSharePanel.setVisibility(View.GONE);
                mShareButton.setImageDrawable(mPluginContext.getResources()
                        .getDrawable(R.drawable.ipmsg_share));
            }
            mShareButton.setClickable(true);
            mShowKeyBoardFromShare = isShow;
        }
    }

    public void showSharePanelOrKeyboard(final boolean isShowShare, final boolean isShowKeyboard) {
        if (isShowShare && isShowKeyboard) {
            Log.w(TAG, "Can not show both SharePanel and Keyboard");
            return;
        }

        Log.d(TAG, "showSharePanelOrKeyboard(): isShowShare = " + isShowShare
                + ", isShowKeyboard = " + isShowKeyboard + ", mIsSoftKeyBoardShow = "
                + mIsSoftKeyBoardShow);
        if (!isShowKeyboard && mIsSoftKeyBoardShow && !mIsLandscape) {
            if (!isShowShare && mShowKeyBoardFromShare) {
                showSharePanel(isShowShare);
            }
            mShowKeyBoardFromShare = isShowShare;
            mCallback.showKeyBoardCallback(isShowKeyboard);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mWaitingImeChangedObject) {
                        try {
                            /// M: fix bug ALPS00447850, wait HideSoftKeyBoard longer
                            int waitTime = 300;
                            Log.d(TAG, "showSharePanelOrKeyboard(): object start wait.");
                            mWaitingImeChangedObject.wait(waitTime);
                            Log.d(TAG, "c(): object end wait.");
                        } catch (InterruptedException e) {
                            Log.d(TAG, "InterruptedException");
                        }
                    }
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isShowShare) {
                                showSharePanel(isShowShare);
                                if (mIsLandscape) {
                                    mCallback.setTextEditorMaxHeight(
                                            mReferencedTextEditorTwoLinesHeight);
                                } else {
                                    mCallback.setTextEditorMaxHeight(
                                            mReferencedTextEditorFourLinesHeight);
                                }
                            } else {
                                Log.d(TAG, "showSharePanelOrKeyboard(): new thread.");
                                updateIpFullScreenTextEditorHeight();
                            }
                        }
                    });
                }
            }).start();
        } else {
            if (isShowShare && !isShowKeyboard && mIsLandscape) {
                mCallback.showKeyBoardCallback(isShowKeyboard);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mWaitingImeChangedObject) {
                            try {
                                /// M: fix bug ALPS01297085, wait HideSoftKeyBoard longer
                                int waitTime = 100;
                                Log.d(TAG, "showSharePanelOrKeyboard() :start wait.");
                                mWaitingImeChangedObject.wait(waitTime);
                                Log.d(TAG, "c(): mIsLandscape object end wait.");
                            } catch (InterruptedException e) {
                                Log.d(TAG, "InterruptedException");
                            }
                        }
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSharePanel(isShowShare);
                                mCallback.setTextEditorMaxHeight(
                                        mReferencedTextEditorTwoLinesHeight);
                            }
                        });
                    }
                }).start();
            } else {
                mCallback.showKeyBoardCallback(isShowKeyboard);
                showSharePanel(isShowShare);
                if (isShowShare || isShowKeyboard) {
                    if (mIsLandscape) {
                        mCallback.setTextEditorMaxHeight(
                                mReferencedTextEditorTwoLinesHeight);
                    } else {
                        mCallback.setTextEditorMaxHeight(
                                mReferencedTextEditorFourLinesHeight);
                    }
                } else {
                    Log.d(TAG, "showSharePanelOrKeyboard()");
                    updateIpFullScreenTextEditorHeight();
                }
            }
        }
    }

    public void hideSharePanel() {
        Log.d(TAG, "hideSharePanel()");
        showSharePanelOrKeyboard(false, false);
        updateIpFullScreenTextEditorHeight();
    }

    private void doMmsAction(Message msg) {
        int commonAttachmentType = 0;
        Bundle bundle = msg.getData();
        int action = bundle.getInt(SharePanel.SHARE_ACTION);
        switch (action) {
        case SharePanel.TAKE_PICTURE:
            commonAttachmentType = TAKE_PICTURE;
            break;

        case SharePanel.RECORD_VIDEO:
            commonAttachmentType = RECORD_VIDEO;
            break;

        case SharePanel.RECORD_SOUND:
            commonAttachmentType = RECORD_SOUND;
            break;

        case SharePanel.ADD_VCARD:
            commonAttachmentType = ADD_VCARD;
            break;

        case SharePanel.ADD_IMAGE:
            commonAttachmentType = ADD_IMAGE;
            break;

        case SharePanel.ADD_VIDEO:
            commonAttachmentType = ADD_VIDEO;
            break;


        case SharePanel.ADD_SOUND:
            commonAttachmentType = ADD_SOUND;
            break;

        case SharePanel.ADD_VCALENDAR:
            commonAttachmentType = ADD_VCALENDAR;
            break;

        case SharePanel.ADD_SLIDESHOW:
            commonAttachmentType = ADD_SLIDESHOW;
            break;

        default:
            Log.e(TAG, "invalid share action type: " + action);
            hideSharePanel();
            return;
        }

        mCallback.addIpAttachment(commonAttachmentType, true);
        Log.d(TAG, "attach: addAttachment(commonAttachmentType, true)");
        /// @}

        hideSharePanel();
    }

//    public Handler mMsgHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            Log.d(TAG, "mMsgHandler handleMessage, msg.what: " + msg.what);
//            switch (msg.what) {
//            case ACTION_SHARE:
//                doMmsAction(msg);
//                break;
//            default:
//                Log.d(TAG, "msg type: " + msg.what + "not handler");
//                break;
//            }
//            super.handleMessage(msg);
//        }
//    };

    private boolean isViewVisible(View view) {
        return (null != view)
                    && (View.VISIBLE == view.getVisibility());
    }

    private void enableShareButton(boolean enable) {
        Log.d(TAG, "enableShareButton() enable = " + enable);
        if (!enable) {
            showSharePanel(false);
            mShareButton.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ipmsg_share_disable));
            mShareButton.setClickable(false);
        } else {
            mShareButton.setImageDrawable(mPluginContext.getResources()
                    .getDrawable(R.drawable.ipmsg_share));
            mShareButton.setClickable(true);
        }
    }
    /// sharepanel end

    @Override
    public void onLoadExpressionComplete(int result, String emId) {
        Log.d(TAG, "onLoadExpressionComplete(): result = " + result + ", emId = " + emId);
        mCallback.notifyIpDataSetChanged();
    }

    @Override
    public boolean lockMessage(Context context, IIpMessageItemExt msgItem, boolean locked) {
        RcsMessageItem item = (RcsMessageItem) msgItem;
        if (item.mType.equals("rcs")) {
            Uri lockUri = ContentUris.withAppendedId(Uri.parse("content://rcs"), item.mMsgId);
            final ContentValues values = new ContentValues(1);
            values.put("locked", locked ? 1 : 0);
            Log.d(TAG, "[lockMessage]: uri = " + lockUri);
            context.getContentResolver().update(lockUri, values, null, null);
            return true;
        } else {
            return false;
        }
    }

    private void copyToClipboard(String str) {
        ClipboardManager clipboard =
                    (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, str));
    }
}
