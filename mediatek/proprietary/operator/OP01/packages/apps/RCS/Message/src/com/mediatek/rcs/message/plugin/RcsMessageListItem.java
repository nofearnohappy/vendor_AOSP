package com.mediatek.rcs.message.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.QuickContact;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.Sms;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.mms.ContentType;

import com.mediatek.mms.callback.IMessageListItemCallback;
import com.mediatek.mms.ipmessage.DefaultIpMessageListItemExt;
import com.mediatek.mms.ipmessage.IIpMessageItemExt;
import com.mediatek.mms.ipmessage.IIpMessageListAdapterExt;

import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpEmoticonMessage;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.EmojiShop.OnLoadExpressionListener;
import com.mediatek.rcs.common.utils.RcsVcardUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.MemberInfo;
import com.mediatek.rcs.message.group.PortraitManager.onMemberInfoChangedListener;
import com.mediatek.rcs.message.location.GeoLocService;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.widget.ImageViewEx;

/**
 * Plugin implements. response MessageListItem.java in MMS host.
 *
 */
public class RcsMessageListItem extends DefaultIpMessageListItemExt implements
            onMemberInfoChangedListener, OnClickListener {
    private static String TAG = "RcsMessageListItem";

    // / M: add for ip message, download file, accept or reject
    private View mIpmsgFileDownloadContrller; // ipmsg_file_downloading_controller_view
    private TextView mIpmsgResendButton; // ipmsg_resend
    private Button mIpmsgAcceptButton; // ipmsg_accept
    private Button mIpmsgRejectButton; // ipmsg_reject
    private View mIpmsgFileDownloadView; // ipmsg_file_download
    private TextView mIpmsgFileSize; // ipmsg_download_file_size
    private ImageView mIpmsgCancelDownloadButton; // ipmsg_download_file_cancel
    private ImageView mIpmsgPauseResumeButton; // ipmsg_download_file_resume
    private ProgressBar mIpmsgDownloadFileProgress; // ipmsg_download_file_progress

    private View mIpImageView; // ip_image
    private ImageViewEx mImageContent; // image_content
    private View mIpImageSizeBg; // image_size_bg
    private ImageView mActionButton; // action_btn
    private TextView mContentSize; // content_size
    private ImageView mDeleteBARMsgIndicator; // delete msg indicator
    // private View mCaptionSeparator; // caption_separator
    private TextView mCaption; // text_caption
    private ImageView mMediaPlayView;
    private TextView mVideoDur;
    // private View mVideoCaptionSeparator; // caption_separator
    // private TextView mVideoCaption; // text_caption
    // / M: add for audio
    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info
    private TextView mAudioDur; // audio_dur
    // / M: add for vcard
    private View mIpVCardView;
    private View mIpVCardInfoView;
    private TextView mVCardInfo;
    private ImageView mVCardPortrait;
    private TextView mVCardName;
    private TextView mVCardNumber;
    // / M: add for vcalendar
    private View mIpVCalendarView;
    private TextView mVCalendarInfo;

    private View mIpGeolocView;
    private TextView mBodyTextView;
    private TextView mSmsRecipient;
    private LinearLayout mSmsInfo;
    private ImageView mSenderPhoto;
    private TextView mSenderName;

    // ip burned message
    private int delayTimerLen;
    Timer deleteBARMSGTimer = null;
    BurnedMsgTask timerTask;
    //Handler burnedMsgHandler;
    Handler mMsgListItemHandler;
    static final int LAUNCH_DOWNLOAD_UI_MESSAGE = 1001;
    private static final int EVENT_DELETE_BAR_MSG = 1001;
    private static final int[] ipbarmsgshareIconArr = { R.drawable.ic_ipbar_timer_1,
                                                        R.drawable.ic_ipbar_timer_2,
                                                        R.drawable.ic_ipbar_timer_3,
                                                        R.drawable.ic_ipbar_timer_4,
                                                        R.drawable.ic_ipbar_timer_5};
    private int REQUEST_CODE_IPMSG_RECORD_AUDIO = 212;

    public static final int MSG_LIST_RESEND_IPMSG = 20;
    static final int MSG_LIST_NEED_REFRASH = 100;
    public static final int MESSAGE_LIST_REFRASH_WITH_CLEAR_CACHE = 1;

    private final static float MAX_SCALE = 0.3f;
    private final static float MIN_SCALE = 0.2f;
    private final static float COMP_NUMBER = 0.5f;
    private final static float HEIGHT_SCALE = 0.25f;

    private final static int CHAT_TYPE_ONE2ONE = 1;
    private final static int CHAT_TYPE_ONE2MULTI = 2;
    private final static int CHAT_TYPE_GROUP = 3;
    static int mChatType = CHAT_TYPE_ONE2ONE;

    private int mTimerNum = 5;
    public IMessageListItemCallback mIpMessageItemCallback;
    private RcsMessageListAdapter mMessageListAdapter;
    public Context mContext;
    public Context mRcsContext;
    public RcsMessageItem mRcsMessageItem;
    public LinearLayout mItemView;
    private View mMsgListItem;

    private int mDirection;
    private MemberInfo mMemberInfo;
    private long mThreadId = -1;
    private boolean mIsGroupItem;
    private boolean mVisible; //true between onbind and ondetached
    private boolean burnedAudioMsg = false;

    private TextView mSystemEventText;
    private View mMessageContent;
    private boolean mIsDeleteMode = false;
    private boolean mIsLastItem = false;
    private PortraitManager mPortraitManager;
    private RCSMessageManager mMessageManager;

    public static final int text_view = 1;
    public static final int date_view = 2;
    public static final int sim_status = 3;
//    public static final int account_icon = 4;
    public static final int locked_indicator = 5;
    public static final int delivered_indicator = 6;
    public static final int details_indicator = 7;
    public static final int avatar = 8;
    public static final int message_block = 9;
    public static final int select_check_box = 10;
    public static final int time_divider = 11;
    public static final int time_divider_str = 12;
    public static final int unread_divider = 13;
    public static final int unread_divider_str = 14;
    public static final int on_line_divider = 15;
    public static final int on_line_divider_str = 16;
    public static final int sim_divider = 17;
    public static final int text_expire = 18;
    public static final int sender_name = 19;
    public static final int sender_name_separator = 20;
    public static final int sender_photo = 21;
    public static final int send_time_txt = 22;
    public static final int double_time_layout = 23;
    public static final int mms_file_attachment_view_stub = 24;
    public static final int file_attachment_view = 25;
    public static final int file_attachment_thumbnail = 26;
    public static final int file_attachment_name_info = 27;
    public static final int file_attachment_name_info2 = 28;
    public static final int file_attachment_thumbnail2 = 29;
    public static final int file_attachment_size_info = 30;
    public static final int mms_view = 31;
    public static final int mms_layout_view_stub = 32;
    public static final int image_view = 33;
    public static final int play_slideshow_button = 34;
    public static final int mms_downloading_view_stub = 35;
    public static final int btn_download_msg = 36;
    public static final int label_downloading = 37;
    public static final int mms_download_controls = 38;
    public static final int status_panel = 39;
    public static final int TAG_THREAD_ID = R.id.msg_list_item_recv;
    public static final int TAG_ITEM_TYPE = R.id.msg_list_item_send;
    public static final int TYPE_INCOMING = 1;
    public static final int TYPE_OUTGOING = 2;
    public static final int TYPE_SYSTEM_EVENT = 3;

    private static final int MSG_EVENT_IPMSG_LOADED = 1;

    public RcsMessageListItem(Context context) {
        mRcsContext = context;
        mPortraitManager = PortraitManager.getInstance();
    }

    @Override
    public boolean onIpFinishInflate(Context context, TextView bodyTextView,
          IMessageListItemCallback ipMessageItemCallback, Handler handler, LinearLayout itemView) {
        mContext = context;
        mIpMessageItemCallback = ipMessageItemCallback;
        mItemView = itemView;
        int childCount = itemView.getChildCount();
        for (int index = 0; index < childCount; index ++) {
            View child = itemView.getChildAt(index);
            String className = child.getClass().getName();
            String string = child.toString();
            if (className.equals("com.mediatek.rcs.message.ui.MsgListItem")) {
                mMsgListItem =  child;
                mSenderPhoto = (ImageView)child.findViewById(R.id.sender_photo);
                mSenderName = (TextView)child.findViewById(R.id.sender_name);
                mSystemEventText = (TextView)child.findViewById(R.id.systen_event_text);
                mMessageContent = child.findViewById(R.id.message_content);
                mBodyTextView = (TextView)child.findViewById(R.id.text_view);
                break;
            }
        }
        mMessageManager = RCSMessageManager.getInstance();
        initIpMessageResource();
        return false;
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case MSG_EVENT_IPMSG_LOADED:
                // ip message loaded;
                if (!mVisible) {
                    return;
                }
                mRcsMessageItem.setGetIpMessageFinishMessage(null);
                if (mRcsMessageItem.getIpMessage() != null) {
                    bindIpmsg(mIsDeleteMode);
                }
                break;
            default:
                break;
            }
        }
    };

    @Override
    public View findIpView(int id, View parentView) {
        if (mMsgListItem == null) {
            return null;
        }
        if (mDirection == TYPE_SYSTEM_EVENT) {
            return null;
        }
        switch (id) {
            case text_view:
                return parentView.findViewById(R.id.text_view);
            case date_view:
                return parentView.findViewById(R.id.date_view);
            case sim_status:
                return parentView.findViewById(R.id.sim_status);
//            case account_icon:
//                return parentView.findViewById(R.id.account_icon);
            case locked_indicator:
                return parentView.findViewById(R.id.locked_indicator);
            case delivered_indicator:
                return parentView.findViewById(R.id.delivered_indicator);
            case details_indicator:
                return parentView.findViewById(R.id.details_indicator);
            case avatar:
                return null;
//                return parentView.findViewById(R.id.avatar);
            case select_check_box:
                return parentView.findViewById(R.id.select_check_box);
            case time_divider:
                return parentView.findViewById(R.id.time_divider);
            case time_divider_str:
                return parentView.findViewById(R.id.time_divider_str);
            case unread_divider:
                return parentView.findViewById(R.id.unread_divider);
            case unread_divider_str:
                return parentView.findViewById(R.id.unread_divider_str);
            case on_line_divider:
                return parentView.findViewById(R.id.on_line_divider);
            case on_line_divider_str:
                return parentView.findViewById(R.id.on_line_divider_str);
            case sim_divider:
                return parentView.findViewById(R.id.sim_divider);
            case text_expire:
                return parentView.findViewById(R.id.text_expire);
            case sender_name:
                return null;//parentView.findViewById(R.id.sender_name);
            case sender_name_separator:
                return parentView.findViewById(R.id.sender_name_separator);
            case sender_photo:
                return null;//parentView.findViewById(R.id.sender_photo);
            case send_time_txt:
                return null;
            case double_time_layout:
                return null;
            case mms_file_attachment_view_stub:
                return parentView.findViewById(R.id.mms_file_attachment_view_stub);
            case file_attachment_view:
                return parentView.findViewById(R.id.file_attachment_view);
            case file_attachment_thumbnail:
                return parentView.findViewById(R.id.file_attachment_thumbnail);
            case file_attachment_name_info:
                return parentView.findViewById(R.id.file_attachment_name_info);
            case file_attachment_name_info2:
                return parentView.findViewById(R.id.file_attachment_name_info2);
            case file_attachment_thumbnail2:
                return parentView.findViewById(R.id.file_attachment_thumbnail2);
            case file_attachment_size_info:
                return parentView.findViewById(R.id.file_attachment_size_info);
            case mms_view:
                return parentView.findViewById(R.id.mms_view);
            case mms_layout_view_stub:
                return parentView.findViewById(R.id.mms_layout_view_stub);
            case image_view:
                return parentView.findViewById(R.id.image_view);
            case play_slideshow_button:
                return parentView.findViewById(R.id.play_slideshow_button);
            case mms_downloading_view_stub:
                return parentView.findViewById(R.id.mms_downloading_view_stub);
            case btn_download_msg:
                return parentView.findViewById(R.id.btn_download_msg);
            case label_downloading:
                return parentView.findViewById(R.id.label_downloading);
            case mms_download_controls:
                return parentView.findViewById(R.id.mms_download_controls);
            case status_panel:
                return parentView.findViewById(R.id.status_panel);
            default:
        }
        return null;
    }

    private void initIpMessageResource() {
        if (mMsgListItem != null) {

            /// M: add for audio
            mIpAudioView = (View) mMsgListItem.findViewById(R.id.ip_audio);
            mAudioIcon = (ImageView) mMsgListItem.findViewById(R.id.ip_audio_icon);
            mAudioInfo = (TextView) mMsgListItem.findViewById(R.id.audio_info);
            mAudioDur = (TextView) mMsgListItem.findViewById(R.id.audio_dur);

            /// M: add for image and video
            mIpImageView = (View) mMsgListItem.findViewById(R.id.ip_image);
            mImageContent = (ImageViewEx) mMsgListItem.findViewById(R.id.image_content);
            mVideoDur = (TextView) mMsgListItem.findViewById(R.id.video_dur);
            mIpImageSizeBg = (View) mMsgListItem.findViewById(R.id.image_size_bg);
            //mActionButton = (ImageView) findViewById(R.id.action_btn);
            mContentSize = (TextView) mMsgListItem.findViewById(R.id.content_size);
            mDeleteBARMsgIndicator =
                            (ImageView) mMsgListItem.findViewById(R.id.deleteBARMsg_indicator);
            /// M: add for vCard
            mIpVCardView = (View) mMsgListItem.findViewById(R.id.ip_vcard);
            mIpVCardInfoView = (View) mMsgListItem.findViewById(R.id.ip_vcard_info);
            mVCardName = (TextView) mMsgListItem.findViewById(R.id.vcard_info);
            mVCardNumber = (TextView) mMsgListItem.findViewById(R.id.vcard_number);
            mVCardPortrait = (ImageView) mMsgListItem.findViewById(R.id.ip_vcard_icon);

            mIpGeolocView = (View) mMsgListItem.findViewById(R.id.ip_geoloc);

            mCaption = (TextView) mMsgListItem.findViewById(R.id.text_caption);
            mBodyTextView = (TextView) mMsgListItem.findViewById(R.id.text_view);
            mMediaPlayView = (ImageView) mMsgListItem.findViewById(R.id.video_media_paly);

            mSmsInfo = (LinearLayout) mMsgListItem.findViewById(R.id.sms_info);
            mSmsRecipient = (TextView) mMsgListItem.findViewById(R.id.sms_to);
        }
    }

    /**
     * on IpBind.
     * @param ipMessageItem IIpMessageItemExt
     * @param msgId messageId
     * @param ipMessageId ip message id
     * @param isDeleteMode delete mode
     * @return if processed, return true, else return false.
     */
    public boolean onIpBind(IIpMessageItemExt ipMessageItem, long msgId,
                                            long ipMessageId, boolean isDeleteMode) {
        Log.d(TAG, "bindView(): IpMessageId = " + ipMessageId);
        mIsDeleteMode = isDeleteMode;
        mRcsMessageItem = (RcsMessageItem) ipMessageItem;
        mVisible = true;
        if (!mRcsMessageItem.mType.equals("rcs")) {
            Log.d(TAG, "[onIpBind]: not rcs message: mType = " + mRcsMessageItem.mType);
            return false;
        }
        delayTimerLen = mRcsMessageItem.mBurnedMsgTimerNum;
        if (mMsgListItem != null) {
            mThreadId = mRcsMessageItem.mThreadId;
            if (!TextUtils.isEmpty(mRcsMessageItem.mRcsChatId)) {
                mIsGroupItem = true;
            }
            mDirection =(Integer)mMsgListItem.getTag(TAG_ITEM_TYPE);
        }

        //system event
        if (mRcsMessageItem != null && mRcsMessageItem.isSystemEvent()) {
            bindIpSystemEvent(mRcsMessageItem, isDeleteMode);
            return true;
        }
        bindPartipantInfo(mRcsMessageItem, isDeleteMode);

        if (mRcsMessageItem.mType.equals("rcs")) {
            bindIpmsg(isDeleteMode);
            return true;
        }
        return false;
    }

    @Override
    public void onIpUnbind() {
        if (mRcsMessageItem == null) {
            return;
        }
        //when upbind, remove unprocessed message
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mRcsMessageItem.setGetIpMessageFinishMessage(null);
        if (mMemberInfo != null) {
            mMemberInfo.removeChangedListener(this);
            mMemberInfo = null;
        }
        Log.d(TAG, "drawDeleteBARMsgIndicator: onIpDetachedFromWindow()  mTimeTask = " + mTimeTask);
        if (mTimeTask != null) {
            mTimeTask.setHandler(null);
        }
        if (mDeleteBARMsgIndicator != null) {
            mDeleteBARMsgIndicator.setImageDrawable(null);
        }
        mVisible = false;
    }

    public boolean onIpMessageListItemClick() {
        if (mIsDeleteMode) {
            return false;
        }
        if (!mRcsMessageItem.mType.equals("rcs")) {
            return false;
        }
        IpMessage ipMessage = mRcsMessageItem.mIpMessage;
        Log.w(TAG, " [BurnedMsg]: onIpMessageListItemClick(), mThreadId = " + mThreadId
                 + "ipMessage = " + ipMessage);
        if (mRcsMessageItem.mType.equals("rcs")
                && mRcsMessageItem.mRcsStatus == MessageStatus.FAILED) {
            if (!mMessageListAdapter.isChatActive()) {
                showIpMessageDetail(mRcsMessageItem);
            } else {
                //resend
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage(mRcsContext.getString(R.string.retry_indicator))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        mRcsMessageItem.resend();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
            }
            return true;
        }

        if (ipMessage != null && (ipMessage.getType() == IpMessageType.VIDEO ||
            ipMessage.getType() == IpMessageType.PICTURE)) {

            /* this ipmessage's type is video or image, so we will show it or download it
            or redownload it, which action will be determined by RCSStatus*/

            Log.d(TAG, "onIpMessageListItemClick(), image or video " +
                    "ipMessage.getStatus() " + ipMessage.getStatus() +
                    "ipMessage.getRCSStatus() " + ((IpAttachMessage) ipMessage).getRcsStatus());

            //we call show this media
             if (ipMessage instanceof IpAttachMessage &&
                  (ipMessage.getStatus() == MessageStatus.READ
                      || ipMessage.getStatus() == MessageStatus.UNREAD)
                  && (((IpAttachMessage) ipMessage).getRcsStatus() == Status.WAITING
                      || ((IpAttachMessage) ipMessage).getRcsStatus() == Status.FAILED)) {
                boolean serviceReady = RCSServiceManager.getInstance().serviceIsReady();
                if (!serviceReady) {
                    Toast.makeText(mContext,
                                   mRcsContext.getString(R.string.download_file_fail),
                                   Toast.LENGTH_SHORT).show();
                    return true;
                }
             }
            Intent ipMsgIntent = new Intent(mRcsContext,
                                                    RcsIpMsgContentShowActivity.class);
            ipMsgIntent.putExtra("msg_id", mRcsMessageItem.mMsgId);
            ipMsgIntent.putExtra("chat_id", mRcsMessageItem.mRcsChatId);
            ipMsgIntent.putExtra("ipmsg_id", mRcsMessageItem.mIpMessageId);
            ipMsgIntent.putExtra("thread_id", mThreadId);
            ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, " [BurnedMsg]: onIpMessageListItemClick() mMsgId = " +
                        mRcsMessageItem.mMsgId + " mThreadId = " + mThreadId);
            try {
                mRcsContext.startActivity(ipMsgIntent);
            } catch (android.content.ActivityNotFoundException e) {
                 Log.w(TAG, "onItemClick,Cannot open file: ");
            }
             return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.VOICE) {
            //this is a audio, so click it and play it
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String filePath = ((IpAttachMessage)ipMessage).getPath();
            File file = new File(filePath);
            Uri audioUri = Uri.fromFile(file);
            Log.w(TAG, "audioUri = " + audioUri);
            intent.setDataAndType(audioUri, ContentType.AUDIO_AMR);
            try {
                mContext.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                 Log.w(TAG, "onItemClick,Cannot open file: ");
            }
            return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.GEOLOC) {
            //this is a geolocation info, so will open map
            if (((IpAttachMessage)ipMessage).getPath() != null) {
                GeoLocXmlParser parser =
                            GeoLocUtils.parseGeoLocXml(((IpAttachMessage) ipMessage).getPath());
                double latitude = parser.getLatitude();
                double longitude = parser.getLongitude();
                Log.d(TAG, "parseGeoLocXml:latitude=" + latitude + ",longitude=" + longitude);

                Toast.makeText(mContext, "latitude = " + latitude + ",longitude=" + longitude,
                    Toast.LENGTH_SHORT).show();

                if (latitude != 0.0 || longitude != 0.0) {
                      Uri uri = Uri.parse("geo:" + latitude + "," + longitude);
                      Intent it = new Intent(Intent.ACTION_VIEW, uri);
                      mContext.startActivity(it);
                } else {
                    Toast.makeText(mContext, mRcsContext.getString(R.string.geoloc_map_failed),
                                    Toast.LENGTH_SHORT).show();
                }
            }
            return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.VCARD) {
            //this is a vcard file, import or prview it

            final IpMessage ipMsg = ipMessage;

            int entryCount = ((IpVCardMessage)ipMessage).getEntryCount();
            if (entryCount == 0) {
                // there is no entrycount in ipMessage
                entryCount = RcsVcardUtils.getVcardEntryCount(
                                                ((IpAttachMessage) ipMessage).getPath());
                ((IpVCardMessage)ipMessage).setEntryCount(entryCount);
            }

            Log.d(TAG,"onItemClick, vcard entry count = " + entryCount);
            if (entryCount == 1) {
                // preview it
                Uri uri = Uri.fromFile(new File(((IpVCardMessage)ipMessage).getPath()));
                Intent intent = new Intent("android.intent.action.rcs.contacts.VCardViewActivity");
                intent.setDataAndType(uri,"text/x-vCard".toLowerCase());
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            } else {
                Resources res = mRcsContext.getResources();
                AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setTitle(res.getString(R.string.multi_cantacts_name))
                    .setMessage(res.getString(R.string.multi_contacts_notification));
                b.setCancelable(true);
                b.setPositiveButton(android.R.string.ok,
                   new DialogInterface.OnClickListener() {
                        public final void onClick(DialogInterface dialog, int which) {
                            importVcard(ipMsg);
                        }
                   }
                );

                b.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public final void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                b.create().show();
             }
             return true;
         } else if (ipMessage != null && ipMessage.getType() == IpMessageType.EMOTICON) {
             Intent ipMsgIntent = new Intent(mRcsContext,
                                      RcsIpMsgContentShowActivity.class);
             ipMsgIntent.putExtra("msg_id", mRcsMessageItem.mMsgId);
             ipMsgIntent.putExtra("chat_id", mRcsMessageItem.mRcsChatId);
             ipMsgIntent.putExtra("ipmsg_id", mRcsMessageItem.mIpMessageId);
             ipMsgIntent.putExtra("thread_id", mThreadId);
             ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             try {
                 mRcsContext.startActivity(ipMsgIntent);
             } catch (android.content.ActivityNotFoundException e) {
                  Log.w(TAG, "onItemClick,Cannot open file: ");
             }
             return true;
         }
        // Host to handle it
        return false;
    }

    private void importVcard(IpMessage ipMessage) {
        Log.d(TAG,"importVcard(), entry, = ");
        final File tempVCard = new File(((IpAttachMessage)ipMessage).getPath());
        if (!tempVCard.exists() || tempVCard.length() <= 0) {
            Log.e(TAG, "importVCard fail! because of error path");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(tempVCard), "text/x-vCard".toLowerCase());
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);
    }

    @Override
    public void setIpMessageListItemAdapter(IIpMessageListAdapterExt adapter) {
        mMessageListAdapter = (RcsMessageListAdapter) adapter;
    }

    /**
     * set message list item handler.
     * @param handler
     */
    public void setMsgListItemHandler(Handler handler) {
        mMsgListItemHandler = handler;
    }

    /**
     * Called when bind default.
     * @param item IIpMessageItemExt
     * @return true if proccessed
     */
    public boolean onIpBindDefault(IIpMessageItemExt item) {
        hideFileTranfserViews();
        Log.d(TAG,"onIpBindDefault, enter");
        if (item == null) {
            Log.e(TAG, "[onIpBindDefault]: item is null");
            return false;
        }
        RcsMessageItem msgItem = (RcsMessageItem) item;
        if (msgItem.mType.endsWith("rcs")) {
            if (msgItem.mRcsMsgClass == RcsLog.Class.SYSTEM) {
                bindIpSystemEvent(msgItem, false);
            } else if (msgItem.mRcsMsgType == MessageType.IM
                            && msgItem.mRcsMsgClass != RcsLog.Class.BURN) {
                mBodyTextView.setVisibility(View.VISIBLE);
                mBodyTextView.setText(msgItem.mBody);
            } else {
                mBodyTextView.setVisibility(View.VISIBLE);
                mBodyTextView.setText(mRcsContext.getString(R.string.refreshing));
            }
        }
        return false;
    }

    private void bindSenderPhoto(int chatType) {
        switch (chatType) {
        case CHAT_TYPE_ONE2ONE:
            showSenderPhoto(true);
            showSenderName(false);
            break;
        case CHAT_TYPE_GROUP:
            showSenderPhoto(true);
            showSenderName(false);
            break;

        default:
            showSenderPhoto(false);
            showSenderName(false);
            break;
        }
    }

    private void showSenderPhoto(boolean show) {
        if (mSenderPhoto != null) {
            int visible = show ? View.VISIBLE : View.GONE;
            mSenderPhoto.setVisibility(visible);
            //TODO: get address image
//            mSenderPhoto.setImageBitmap(bm);
//            mSenderPhoto.assignContactFromPhone(mRcsMessageItem.mAddress, false);
            mSenderPhoto.setOnClickListener(this);
        }
    }

    private void showSenderName(boolean show) {
        if (mSenderName != null) {
            int visible = show ? View.VISIBLE : View.GONE;
            mSenderName.setVisibility(visible);
            //TODO: get address Name
            if (show) {
                mSenderName.setText(mRcsMessageItem.mAddress);
            }
        }
    }

    /**
     * bind Rcs Message.
     * @param isDeleteMode
     * @return true if processed, else return false.
     */
    public boolean bindIpmsg(boolean isDeleteMode) {
        Log.d(TAG, " [BurnedMsg] bindIpmsg(): msgId = " + mRcsMessageItem.mMsgId);
        if (mMessageContent != null) {
            mMessageContent.setVisibility(View.VISIBLE);
        }
        if (mSystemEventText != null) {
            mSystemEventText.setVisibility(View.GONE);
        }
        boolean processed = bindIpTextItem(mRcsMessageItem, isDeleteMode);
        if (processed) {
            return true;
        }
        IpMessage ipMessage = mRcsMessageItem.getIpMessage();
        if (null == ipMessage) {
            Log.d(TAG, "bindIpmsg(): ip message is null! not loaded");
            if (mBodyTextView != null) {
                mBodyTextView.setText(mRcsContext.getString(R.string.refreshing));
            }
            hideFileTranfserViews();
            mIpMessageItemCallback.setSubDateView(null);
            mRcsMessageItem.setGetIpMessageFinishMessage(
                                        mHandler.obtainMessage(MSG_EVENT_IPMSG_LOADED));
            return false;
        }

        // / M: hide file transfer view
        if (mIpmsgFileDownloadContrller != null) {
            mIpmsgFileDownloadContrller.setVisibility(View.GONE);
        }
        if (mIpmsgFileDownloadView != null) {
            mIpmsgFileDownloadView.setVisibility(View.GONE);
        }

        //bind burned voice message,if showed, need to delete.
        if (!bindBurnedVoiceMsg(ipMessage)) {
            return false;
        }
        processed = bindBurnedMsgIndicator(ipMessage);
        Log.d(TAG, "[bindIpmsg]: burn message processed = " + processed);
        if (!processed) {
            switch (ipMessage.getType()) {
            case IpMessageType.PICTURE:
                setIpImageItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.VOICE:
                setIpVoiceItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.VIDEO:
                setIpVideoItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.VCARD:
                setIpVCardItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.GEOLOC:
                setIpGeolocItem(ipMessage, isDeleteMode);
                break;
            case IpMessageType.EMOTICON:
                setIpEmShopItem(ipMessage, isDeleteMode);
                break;
            default:
                Log.e(TAG, "bindIpmsg(): Error IP message type. type = " + ipMessage.getType());
                break;
            }
        }
        mIpMessageItemCallback.setSubDateView(null);
        return true;
    }

    private boolean bindBurnedVoiceMsg(IpMessage ipMessage) {
        Log.d(TAG, " [BurnedMsg]: bindBurnedVoiceMsg()");
        if (((mDirection == TYPE_INCOMING) && ipMessage.getBurnedMessage()) && burnedAudioMsg) {
            burnedAudioMsg = false;
            Log.d(TAG," [BurnedMsg]: bindIpmsg incoming burn audio message"
                            + "((IpAttachMessage)ipMessage).getRcsStatus() = "
                            + ((IpAttachMessage)ipMessage).getRcsStatus());
            if (ipMessage.getType() == IpMessageType.VOICE &&
                    ((IpAttachMessage)ipMessage).getRcsStatus() == Status.FINISHED) {
                mMessageManager.sendBurnDeliveryReport(ipMessage.getTo(), ipMessage.getMessageId());
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Log.d(TAG, " [BurnedMsg]: delete voice burned message");
                            Thread.sleep(1000);
                            mMessageManager.deleteRCSMessage(mRcsMessageItem.mMsgId);
                            removeIpMsgId(mRcsMessageItem.mMsgId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            return false;
        }
        return true;
    }

    private boolean bindBurnedMsgIndicator(IpMessage ipMessage) {
        Log.d(TAG, " [BurnedMsg]: bindBurnedMsgIndicator()");
        if (ipMessage == null || !ipMessage.getBurnedMessage()) {
            Log.d(TAG, "[BurnedMsg]bindIpmsg: ipMessage is null or not burn message");
            ImageView viewImage = (ImageView) mMsgListItem
                    .findViewById(R.id.hide_ip_bar_message);
            if (viewImage != null) {
                viewImage.setVisibility(View.GONE);
            }
            return false;
        }
        if (ipMessage != null) {
            Log.d(TAG," [BurnedMsg]: bindIpmsg: ipMessage.getBurnedMessage() = "
                 + ipMessage.getBurnedMessage() + " mDirection = "
                 + mDirection + "  getType = " + ipMessage.getType());
        }
        if (mDirection == TYPE_OUTGOING) {
            // send burned message logic
            if ((ipMessage.getType() == IpMessageType.TEXT
                    && (mRcsMessageItem.mRcsStatus == MessageStatus.SENT ||
                        mRcsMessageItem.mRcsStatus == MessageStatus.DELIVERED))
                    || (ipMessage instanceof IpAttachMessage
                    && ((IpAttachMessage) ipMessage).getRcsStatus() == Status.FINISHED)) {
                Log.d(TAG, " [BurnedMsg]:bindIpmsg: sent text message or finished filetranser");
                drawDeleteBARMsgIndicator(mRcsMessageItem);
                saveIpMsgId(mRcsMessageItem.mMsgId);
            }
            return false;
        } else {
            // receive the burned msg,show mail icon
            if (mBodyTextView != null) {
                mBodyTextView.setVisibility(View.GONE);
            }
            hideFileTranfserViews();
            ImageView viewImage = (ImageView) mMsgListItem
                    .findViewById(R.id.hide_ip_bar_message);
            if (viewImage != null) {
                viewImage.setVisibility(View.VISIBLE);
                LinearLayout mMsgContainer = (LinearLayout) mMsgListItem
                        .findViewById(R.id.mms_layout_view_parent);
                mMsgContainer.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        try {
                            boolean serviceReady = RCSServiceManager
                                    .getInstance().serviceIsReady();
                            if (!serviceReady) {
                                Toast.makeText(mContext,mRcsContext
                                    .getString(R.string.download_file_fail),
                                                Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Intent ipMsgIntent = null;
                            IpMessage ipMsg = mRcsMessageItem.getIpMessage();
                            if (ipMsg != null
                                    && (ipMsg.getType() == IpMessageType.TEXT
                                       || ipMsg.getType() == IpMessageType.VIDEO
                                       || ipMsg.getType() == IpMessageType.PICTURE)) {
                                ipMsgIntent = new Intent(
                                        mRcsContext,
                                        RcsIpMsgContentShowActivity.class);
                                ipMsgIntent.putExtra("chat_id", mRcsMessageItem.mRcsChatId);
                                ipMsgIntent.putExtra("msg_id", mRcsMessageItem.mMsgId);
                                ipMsgIntent.putExtra("ipmsg_id", mRcsMessageItem.mIpMessageId);
                                ipMsgIntent.putExtra("thread_id", mThreadId);
                                ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                Log.d(TAG,"[BurnedMsg]: onClick mMsgId = "
                                  + mRcsMessageItem.mMsgId + " mThreadId = " + mThreadId);
                            } else if (ipMsg != null && ipMsg.getType() == IpMessageType.VOICE) {
                                // this is a audio, so click it and play it
                                ipMsgIntent = new Intent(Intent.ACTION_VIEW);
                                String filePath = ((IpAttachMessage) ipMsg).getPath();
                                File file = new File(filePath);
                                Uri audioUri = Uri.fromFile(file);
                                Log.w(TAG, "audioUri = " + audioUri);
                                burnedAudioMsg = true;
                                Log.d(TAG," [BurnedMsg]: onClick audio " +
                                        "burnedAudioMsg = " + burnedAudioMsg);
                                ipMsgIntent.setDataAndType(audioUri,
                                        ContentType.AUDIO_AMR);
                                ipMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                saveIpMsgId(mRcsMessageItem.mMsgId);
                            } else {
                                Log.e(TAG, "[BurnedMsg] unkown burn type");
                            }
                            if (ipMsg != null) {
                                Message msg = new Message();
                                msg.what = LAUNCH_DOWNLOAD_UI_MESSAGE;
                                msg.obj = ipMsgIntent;
                                mMsgListItemHandler.sendMessage(msg);
                            }
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.w(TAG, "[BurnedMsg]: activity not found ");
                        }
                    }
                });
            }
            return true;
        }
    }

    private void saveIpMsgId(long msgId) {
        Log.d(TAG, "[BurnedMsg]: saveIpMsgId()");
        SharedPreferences sp = mRcsContext.getSharedPreferences(
                IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> burnedMsgList =
                    sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
        if (burnedMsgList == null) {
            Log.d(TAG, "[BurnedMsg]: burnedMsgList is null" + "  msgId = "+msgId);
            burnedMsgList = new HashSet<String>();
            burnedMsgList.add(String.valueOf(msgId));
        } else {
            Log.d(TAG, "[BurnedMsg]: msgId = "+msgId + " burnedMsgList = "+burnedMsgList);
            boolean isInsert = true;
            burnedMsgList = new HashSet<String>(burnedMsgList);
            for (String id : burnedMsgList) {
                if (Long.valueOf(id) == msgId) {
                    isInsert = false;
                    return;
                }
            }
            if (isInsert)
                burnedMsgList.add(String.valueOf(msgId));
            Log.d(TAG, "[BurnedMsg]: isInsert = "+isInsert + " burnedMsgList = "+burnedMsgList);
        }

        // Set<String> burnedMsgList = new HashSet<String>();
        SharedPreferences.Editor prefs = sp.edit();
        prefs.putStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , burnedMsgList);
        prefs.apply();
        Log.d(TAG, "[BurnedMsg]: save success burnedMsgList = "+burnedMsgList);
    }

    private void removeIpMsgId(long msgId) {
        Log.d(TAG, "[BurnedMsg]: removeIpMsgId()");
        SharedPreferences sp = mRcsContext.getSharedPreferences(
                IpMessageConsts.BurnedMsgStoreSP.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> burnedMsgList =
                sp.getStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY, null);
        if (burnedMsgList == null) {
            Log.d(TAG, "[BurnedMsg]: burnedMsgList is null");
            return;
        }
        burnedMsgList = new HashSet<String>(burnedMsgList);
        Log.d(TAG, "[BurnedMsg]: removeIpMsgId burnedMsgList = "+burnedMsgList);
        for (String id : burnedMsgList) {
            if (Long.valueOf(id) == msgId) {
                burnedMsgList.remove(String.valueOf(msgId));
                break;
            }
        }
        SharedPreferences.Editor prefs = sp.edit();
        prefs.putStringSet(IpMessageConsts.BurnedMsgStoreSP.PREF_PREFIX_KEY , burnedMsgList);
        prefs.apply();
        Log.d(TAG, "[BurnedMsg]: remove success burnedMsgList = "+burnedMsgList);
    }

    private boolean bindIpTextItem(RcsMessageItem item, boolean isDeleteMode) {
        if (item.mRcsMsgType == MessageType.IM) {
            //normal text message don't query content from stack
            if (!TextUtils.isEmpty(mRcsMessageItem.mBody)) {
                if (EmojiShop.matchEmXml(mRcsMessageItem.mBody)) {
                    setIpTextItem(EmojiShop.parseEmSmsString(mRcsMessageItem.mBody), isDeleteMode);
                } else {
                    setIpTextItem(mRcsMessageItem.mBody, isDeleteMode);
                }
                bindBurnedMsgIndicator(item.getIpMessage());
                return true;
            } else {
                Log.e(TAG, "[bindIpTextItem] content is null");
            }
        }
        return false;
    }

    private void setIpTextItem(String body, boolean isDeleteMode) {
        Log.d(TAG, "setIpTextItem(): body = " + body);
        mIpMessageItemCallback.setTextMessage(body);
        mIpMessageItemCallback.setSubDateView(null);
        hideFileTranfserViews();
    }

    private void setIpEmShopItem(IpMessage ipMessage, boolean isDeleteMode) {
        Log.d(TAG, "setIpEmShopItem(): ipMessage = " + ipMessage);
        if (ipMessage == null) {
            Log.e(TAG, "setIpEmShopItem(): ipMessage = null");
            return;
        }
        IpTextMessage textMessage = (IpTextMessage) ipMessage;
        String emXml = textMessage.getBody();
        if (!RCSServiceManager.getInstance().serviceIsReady()) {
            String smsStr = EmojiShop.parseEmSmsString(emXml);
            setIpTextItem(smsStr, false);
            return;
        }
        String emId = EmojiShop.getEmIdByXml(emXml);
        Log.d(TAG, "setIpEmShopItem(): emId = " + emId);

        if (EmojiShop.isLocalEmoticon(emId)) {
            String path = EmojiShop.getEmResPath(emXml);
            Log.d(TAG, "setIpEmShopItem(): path = " + path);
            if (path != null) {
                mImageContent.setImageURI(Uri.fromFile(new File(path)));
            }
        } else {
            mImageContent.setTag(emId);
            // use default image
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
        //    EmojiShop.addOnLoadExpressionListener(this);
            EmojiShop.loadEmIconsFromServer((Activity) mContext ,emId);
        }

        mIpImageView.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.GONE);
        mBodyTextView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mVideoDur.setVisibility(View.GONE);
    }

    private void hideFileTranfserViews() {
        if (mIpImageView != null) {
            mIpImageView.setVisibility(View.GONE);
        }
        if (mIpAudioView != null) {
            mIpAudioView.setVisibility(View.GONE);
        }
        if (mCaption != null) {
            mCaption.setVisibility(View.GONE);
        }
        if (mIpVCardView != null) {
            mIpVCardView.setVisibility(View.GONE);
        }
        if (mIpVCalendarView != null) {
            mIpVCalendarView.setVisibility(View.GONE);
        }
        if (mIpGeolocView != null) {
            mIpGeolocView.setVisibility(View.GONE);
        }
    }

    private void setIpImageItem(IpMessage ipMessage, boolean isDeleteMode) {
        IpImageMessage imageMessage = (IpImageMessage) ipMessage;
        Log.d(TAG, "setIpImageItem(): message Id = " + mRcsMessageItem.mMsgId
                + " ipThumbPath:" + imageMessage.getThumbPath() + " imagePath:"
                + imageMessage.getPath());
        mIpImageView.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        updateIpMessageVideoOrImageView(mRcsMessageItem, imageMessage);
        if (!setPicView(mRcsMessageItem, imageMessage.getPath())) {
                setPicView(mRcsMessageItem, imageMessage.getThumbPath());
            }
        /// M: add for ip message, hide text, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mVideoDur.setVisibility(View.GONE);
    }

    private void updateIpMessageVideoOrImageView(RcsMessageItem msgItem,
            IpAttachMessage message) {
        mContentSize.setText(RcsMessageUtils.formatFileSize(message.getSize()));
        mContentSize.setVisibility(View.VISIBLE);
    }

    private boolean setPicView(RcsMessageItem msgItem, String filePath) {
        Log.d(TAG, "setPicView(): filePath = " + filePath + ", imageView = " + mImageContent);
        if (TextUtils.isEmpty(filePath) || null == mImageContent) {
            return false;
        }
        Bitmap bitmap = msgItem.getIpMessageBitmap();
        if (null == bitmap) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(filePath, options);
            int width = options.outWidth;
            int height = options.outHeight;
            int w = options.outWidth;

            /// M: get screen width
            DisplayMetrics dm = new DisplayMetrics();
            int screenWidth = 0;
            int screenHeight = 0;
            WindowManager wmg = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wmg.getDefaultDisplay().getMetrics(dm);
            if (dm.heightPixels > dm.widthPixels) {
                screenWidth = dm.widthPixels;
                screenHeight = dm.heightPixels;
            } else {
                screenWidth = dm.heightPixels;
                screenHeight = dm.widthPixels;
            }
            /// M: the returned bitmap's w/h is different with the input!
            if (width > screenWidth * MAX_SCALE) {
                w = (int) (screenWidth * MAX_SCALE);
                if (height * w / width < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w, height * w / width);
                msgItem.setIpMessageBitmapSize(w, height * w / width);
                } else {
                    bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w,
                            (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(w, (int) (screenHeight * HEIGHT_SCALE));
                }
            } else if (width > screenWidth * MIN_SCALE) {
                w = (int) (screenWidth * MAX_SCALE);
                if (height * w / width < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w, height * w / width);
                msgItem.setIpMessageBitmapSize(w, height * w / width);
            } else {
                    bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, w,
                            (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(w, (int) (screenHeight * HEIGHT_SCALE));
                }
            } else {
                if (height < screenHeight * HEIGHT_SCALE) {
                bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, width, height);
                msgItem.setIpMessageBitmapSize(width, height);
                } else {
                    bitmap = RcsMessageUtils.getBitmapByPath(filePath, options, width,
                            (int) (screenHeight * HEIGHT_SCALE));
                    msgItem.setIpMessageBitmapSize(width, (int) (screenHeight * HEIGHT_SCALE));
                }
            }
            msgItem.setIpMessageBitmapCache(bitmap);
        }

        if (null != bitmap) {
            ViewGroup.LayoutParams params =
                        (ViewGroup.LayoutParams) mImageContent.getLayoutParams();
            params.height = msgItem.getIpMessageBitmapHeight();
            params.width = msgItem.getIpMessageBitmapWidth();
            mImageContent.setLayoutParams(params);
            mImageContent.setImageBitmap(bitmap);
            return true;
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
            return false;
        }
    }

    private void setIpVoiceItem(IpMessage ipMessage, boolean isDeleteMode) {
        Log.d(TAG, "setIpVoiceItem(): message Id = " + mRcsMessageItem.mMsgId);
        IpVoiceMessage voiceMessage = (IpVoiceMessage) ipMessage;

        /// M: add for ip message, show audio view
        mIpAudioView.setVisibility(View.VISIBLE);
        mAudioInfo.setVisibility(View.VISIBLE);
        mAudioIcon.setVisibility(View.VISIBLE);
        mAudioDur.setVisibility(View.VISIBLE);
        mAudioInfo.setText(RcsMessageUtils.formatFileSize(voiceMessage.getSize()));
        String duration = RcsMessageUtils.formatAudioTime(voiceMessage.getDuration());
        if (duration.compareTo(mRcsContext.
                                getString(R.string.unknown_duration)) != 0) {
            mAudioDur.setText(duration);
        } else {
            Log.d(TAG, "setIpVoiceItem, no duration, setVisible(gone)");
            mAudioDur.setVisibility(View.GONE);
        }
        mCaption.setVisibility(View.GONE);
        /// M: add for ip message, hide text, image, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        //mIpDynamicEmoticonView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }

   private void setIpVCardItem(IpMessage ipMessage, boolean isDeleteMode) {

        Log.d(TAG, "setIpVCardItem(): message Id = " + mRcsMessageItem.mMsgId);
        IpVCardMessage vCardMessage = (IpVCardMessage)ipMessage;

        int entryCount = vCardMessage.getEntryCount();
        if (entryCount == 0) {
            // there is no entrycount in ipMessage
            entryCount = RcsVcardUtils.getVcardEntryCount(vCardMessage.getPath());
            ((IpVCardMessage)ipMessage).setEntryCount(entryCount);
        }

        Bitmap bitmap = vCardMessage.getPortrait();
        String name = vCardMessage.getName();
        String email = vCardMessage.getEmail();
        String number = vCardMessage.getMobilePhone();

        Log.d(TAG, "setIpVCardItem(): bitmap = " + bitmap);
        Log.d(TAG, "setIpVCardItem(): name = " + name);
        Log.d(TAG, "setIpVCardItem(): email = " + email);
        Log.d(TAG, "setIpVCardItem(): number = " + number);

        mIpVCardView.setVisibility(View.VISIBLE);
        mIpVCardInfoView.setVisibility(View.VISIBLE);
        mVCardName.setVisibility(View.VISIBLE);
        mVCardNumber.setVisibility(View.VISIBLE);

        if (entryCount == 1) {
            if (bitmap != null) {
                mVCardPortrait.setImageBitmap(bitmap);
            } else {
                try {
                      bitmap = BitmapFactory.decodeResource(mRcsContext.getResources(),
                                R.drawable.ipmsg_chat_contact_vcard);
                      mVCardPortrait.setImageBitmap(bitmap);
                } catch (java.lang.OutOfMemoryError e) {
                        Log.e(TAG, "setImage: out of memory: ", e);
                }
            }
            mVCardName.setText(name);
            if (number != null && !number.equals("")) {
                mVCardNumber.setText(number);
            } else if (email != null && !email.equals("")) {
                mVCardNumber.setText(email);
            } else {
                mVCardNumber.setText("Unknown");
            }
        } else {
            mVCardName.setText(name);
            if (number != null && !number.equals("")) {
                mVCardNumber.setText(number + "...");
            } else if (email != null && !email.equals("")) {
                mVCardNumber.setText(email + "...");
            } else {
                mVCardNumber.setText("Unknown");
            }
            bitmap = BitmapFactory.decodeResource(mRcsContext.getResources(),
                                R.drawable.ipmsg_chat_contact_vcard);
            if (bitmap != null) {
                mVCardPortrait.setImageBitmap(bitmap);
                // set portrait
                vCardMessage.setPortrait(bitmap);
            }
        }
        mBodyTextView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }
      private void setIpGeolocItem(IpMessage ipMessage, boolean isDeleteMode) {

        Log.d(TAG, "setIpGeolocItem(): message Id = " + mRcsMessageItem.mMsgId);
        IpGeolocMessage geolocMessage = (IpGeolocMessage)ipMessage;

        mBodyTextView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mCaption.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.VISIBLE);
    }

    private void setIpVideoItem(IpMessage ipMessage, boolean isDeleteMode) {
        Log.d(TAG, "setIpVideoItem(): message Id = " + mRcsMessageItem.mMsgId);

        IpVideoMessage videoMessage = (IpVideoMessage) ipMessage;
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        mVideoDur.setVisibility(View.VISIBLE);
        updateIpMessageVideoOrImageView(mRcsMessageItem, videoMessage);
        setVideoView(videoMessage.getPath(), videoMessage.getThumbPath());
        String duration = RcsMessageUtils.formatAudioTime(videoMessage.getDuration());
        if (duration.compareTo(mRcsContext.
                                getString(R.string.unknown_duration)) != 0) {
            mVideoDur.setText(duration);
        } else {
             Log.d(TAG, "setIpVoiceItem, no duration, setVisible(gone)");
            mVideoDur.setVisibility(View.GONE);
        }
        mCaption.setVisibility(View.GONE);
        /// M: add for ip message, hide text, audio, vCard, vCalendar, location view
        mBodyTextView.setVisibility(View.GONE);
        //mIpDynamicEmoticonView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }

    public void setVideoView(String path, String bakPath) {
        Bitmap bp = null;
        int degree = 0;
        mMediaPlayView.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(path)) {
            Log.d(TAG, "setVideoView, path is " + path);
            bp = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MICRO_KIND);
            degree = RcsMessageUtils.getExifOrientation(path);
        }

        if (null == bp) {
            if (!TextUtils.isEmpty(bakPath)) {
                Log.d(TAG, "setVideoView, bakPath = " + bakPath);
                BitmapFactory.Options options = RcsMessageUtils.getOptions(bakPath);
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(bakPath, options);
                bp = RcsMessageUtils.getBitmapByPath(bakPath, RcsMessageUtils.getOptions(bakPath),
                        options.outWidth, options.outHeight);
                degree = RcsMessageUtils.getExifOrientation(bakPath);
            }
        }
        /** M: we use the same view show image/big emoticon/video snap, but they should have
         *     different property.
         *  image layout change to a dynamic size, big emoticon/video snap is still wrap_content
         *  we change ipmessage image layout to keep uniform with group chat activity.
         */
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mImageContent.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mImageContent.setLayoutParams(params);

        if (null != bp) {
            if (degree != 0) {
                bp = RcsMessageUtils.rotate(bp, degree);
            }
            mImageContent.setImageBitmap(bp);
        } else {
            mImageContent.setImageResource(R.drawable.ic_missing_thumbnail_picture);
        }
    }

    private void drawDeleteBARMsgIndicator(RcsMessageItem msgItem) {
        Log.d(TAG, "drawDeleteBARMsgIndicator() ");
        mDeleteBARMsgIndicator.setVisibility(View.VISIBLE);
        startTimer();
    }

    BurnedMsgTask mTimeTask;
    private static HashMap<Long, BurnedMsgTask> sBurnedTimerMap
                                        = new HashMap<Long, BurnedMsgTask>();
    private void startTimer() {
        Log.d(TAG, "drawDeleteBARMsgIndicator: startTimer() ");

        mTimeTask = sBurnedTimerMap.get(Long.valueOf(mRcsMessageItem.mMsgId));
        Log.d(TAG, "drawDeleteBARMsgIndicator: startTimer()  mMsgId = " + mRcsMessageItem.mMsgId +
                   " sBurnedTimerMap = " +sBurnedTimerMap+" mTimeTask = "+mTimeTask);
        if (mTimeTask != null) {
            mTimeTask.setHandler(burnedMsgHandler);
            int count = mTimeTask.getCurrentCount();
            Log.d(TAG, "drawDeleteBARMsgIndicator: startTimer()  count = " + count);
            if (count > 0) {
                mDeleteBARMsgIndicator.setImageResource(ipbarmsgshareIconArr[count-1]);
            } else {
                //TODO: delete this msg
            }
        } else {
            long sentTime = mRcsMessageItem.mSentDate;
            int count = mTimerNum;
            Log.d(TAG, "drawDeleteBARMsgIndicator: text sentTime = " + sentTime);
            if (sentTime > 0) {
                int timer = (int) ((System.currentTimeMillis() - sentTime) / 1000);
                Log.d(TAG, "drawDeleteBARMsgIndicator: timer = " + timer);
                if (timer >= 5 && mTimerNum == 5) {
                   deleteBurnedMsg(mRcsMessageItem.mMsgId);
                   removeIpMsgId(mRcsMessageItem.mMsgId);
                   return ;
                } else if (timer >= 1 && timer < 5) {
                    count = mTimerNum - timer;
                }
            }
            Log.d(TAG, "drawDeleteBARMsgIndicator: count = " + count);
            deleteBARMSGTimer= new Timer();
            mTimeTask = new BurnedMsgTask(mRcsMessageItem.mMsgId, burnedMsgHandler,count);
            deleteBARMSGTimer.schedule(mTimeTask, 0, 1000);
            sBurnedTimerMap.put(Long.valueOf(mRcsMessageItem.mMsgId), mTimeTask);
        }
    }

  private class BurnedMsgTask extends TimerTask {
      long mBurnedMsgId;
      Handler mCallBackHandler;
      int mCount = mTimerNum ;
      public BurnedMsgTask(long msgId, Handler handler,int timerNum) {
          mBurnedMsgId = msgId;
          mCallBackHandler = handler;
          mCount = timerNum;
      }

      public void setHandler(Handler handler) {
          mCallBackHandler = handler;
      }

      public int getCurrentCount() {
          synchronized (this) {
              return mCount;
          }
      }
        public void run() {
            Message msg = new Message();
            synchronized (this) {
                msg.what = mCount--;
            }
            msg.obj = Long.valueOf(mBurnedMsgId);
            Log.d(TAG, " drawDeleteBARMsgIndicator:  send  msg.what = " +
                    msg.what + " mBurnedMsgId = "+mBurnedMsgId);
            if (mCallBackHandler != null) {
                mCallBackHandler.sendMessage(msg);
            }
            if (mCount == -1) {
                Log.d(TAG, " drawDeleteBARMsgIndicator:  this.cancel() ");
                this.cancel();
                //TODO: delete the msg
                if (!mVisible) {
                    deleteBurnedMsg(mBurnedMsgId);
                    removeIpMsgId(mBurnedMsgId);
                }
            }
        }
    }

    Handler burnedMsgHandler = new Handler() {
        int num = -1;
        public void handleMessage(Message msg) {
            num = msg.what;
            long msgId = (Long)msg.obj;
            Log.d(TAG, "drawDeleteBARMsgIndicator: burnedMsgHandler icon num = " + num + ", " +
                "mVisible: " + mVisible + " msgId = " + msgId);
            if (!mVisible || msgId != mRcsMessageItem.mMsgId) {
                return;
            }
            if (num > 0 ) {
                mDeleteBARMsgIndicator.setImageResource(ipbarmsgshareIconArr[num-1]);
            } else if (num == 0 ) {
                mDeleteBARMsgIndicator.setImageDrawable(null);
                mDeleteBARMsgIndicator.setVisibility(View.GONE);

                synchronized (sBurnedTimerMap) {
                    sBurnedTimerMap.remove(mRcsMessageItem.mMsgId);
                }
                Log.d(TAG, "drawDeleteBARMsgIndicator: burnedMsgHandler sBurnedTimerMap = "
                        + sBurnedTimerMap);
                deleteBurnedMsg(mRcsMessageItem.mMsgId);
                removeIpMsgId(mRcsMessageItem.mMsgId);
            }
        }
    };

    private void deleteBurnedMsg(final long msgBurnedId) {
        Log.w(TAG, "deleteBurnedMsg(), msgBurnedId = " +msgBurnedId);
        IpMessage ipMessage = mMessageManager.getRCSMessageInfo(mRcsMessageItem.mMsgId);
        if (ipMessage == null) {
            return;
        }
        Log.w(TAG, "drawDeleteBARMsgIndicator, mThreadId = " + mThreadId
                + "  msgBurnedId = " + msgBurnedId + "  ipMessage type = "
                + ipMessage.getType());

        if (mDirection == TYPE_INCOMING && (ipMessage.getType() == IpMessageType.TEXT ||
                (ipMessage.getType() > IpMessageType.FT_BASE &&
                        (((IpAttachMessage) ipMessage).getRcsStatus() == Status.FINISHED)))) {

            Log.d(TAG, "[BurnedMsg]: ipMessage.getFrom() = "
                    + ipMessage.getFrom() + " ipMessage.getMessageId() = "
                    + ipMessage.getMessageId());
            mMessageManager.sendBurnDeliveryReport(ipMessage.getFrom(),
                            ipMessage.getMessageId());
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, " [BurnedMsg]:  deleteBurnedMsg");
                    mMessageManager.deleteRCSMessage(mRcsMessageItem.mMsgId);
                    removeIpMsgId(mRcsMessageItem.mMsgId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean bindIpSystemEvent(RcsMessageItem item, boolean isDeleteMode) {
        Log.d(TAG, "bindIpmsg(): msgId = " + item.mMsgId);
        if (item == null) {
            Log.e(TAG, "bindIpSystemEvent, item is null");
            throw new RuntimeException("bindIpSystemEvent, item is null");
        }
        if (mMessageContent != null) {
            mMessageContent.setVisibility(View.GONE);
        }
        if (mSystemEventText != null) {
            mSystemEventText.setVisibility(View.VISIBLE);
            String body = item.mBody;
            if (!TextUtils.isEmpty(body)) {
                mSystemEventText.setText(item.mBody);
            }
        }
        return true;
    }

    private void bindPartipantInfo(RcsMessageItem item, boolean isDeleteMode) {
        if (mThreadId != -1) {
            if (!mIsGroupItem) {
                /// M: for multi send items, not show senderPhoto and senderName
                int chatType = mMessageListAdapter.getChatType();
                Log.d(TAG, "bindView(): chatType = " + chatType);
                if (chatType == mMessageListAdapter.CHAT_TYPE_ONE2MULTI) {
                    mVisible = false;
                    showSenderPhoto(false);
                    showSenderName(false);
                    /// M: for sms message, show recipient name view
                    if ("sms".equals(item.mType)) {
                        if (mSmsRecipient != null && mSmsInfo != null) {
                            StringBuilder address = new StringBuilder();
                            address.append(mRcsContext.getString(R.string.to_address_label));
                            String[] numbers = item.mAddress.split(",");
                            int num = numbers.length;
                            for (int i = 0; i < num-1; i++) {
                                address.append(mPortraitManager.getMemberInfo(numbers[i]).mName);
                                address.append(", ");
                            }
                            address.append(mPortraitManager.getMemberInfo(numbers[num-1]).mName);
                            mSmsInfo.setVisibility(View.VISIBLE);
                            mSmsRecipient.setVisibility(View.VISIBLE);
                            mSmsRecipient.setText(address);
                        }
                    } else {
                        if (mSmsRecipient != null && mSmsInfo != null) {
                            mSmsRecipient.setVisibility(View.GONE);
                            mSmsInfo.setVisibility(View.GONE);
                        }
                    }
                    return;
                }
                if (!RCSServiceManager.getInstance().isServiceEnabled()) {
                    showSenderPhoto(false);
                    showSenderName(false);
                    return;
                }
            }

            showSenderPhoto(true);
            if (mDirection == TYPE_INCOMING) {
                showSenderName(mIsGroupItem);
                if (!mIsGroupItem) {
                    //only portrait
                    mMemberInfo = mPortraitManager.getMemberInfo(item.mAddress);
                } else {
                    // portrait and name
                    mMemberInfo = mPortraitManager.getMemberInfo(item.mRcsChatId, item.mAddress);
                }
            } else {
                //only name
                showSenderName(false);
                mMemberInfo = mPortraitManager.getMyInfo(mRcsMessageItem.mSubId);
            }
            updateParticipantInfo(mMemberInfo.mName, mMemberInfo.mDrawable);
            if (mMemberInfo != null) {
                mMemberInfo.addChangedListener(this);
            }
        }
    }

    private void updateParticipantInfo(final String name, final Drawable portrait) {
        if (mSenderName != null && mSenderName.getVisibility() == View.VISIBLE) {
            mSenderName.setText(name);
        }
        if (mSenderPhoto != null) {
            mSenderPhoto.setImageDrawable(portrait);
        }

        if (mDirection == TYPE_OUTGOING) {
            int rcsSubId = RcsMessageUtils.getRcsSubId(mContext);
            if (mRcsMessageItem.mSubId != rcsSubId) {
                Drawable d = mRcsContext.getResources().getDrawable(R.drawable.ic_default_contact);
                mSenderPhoto.setImageDrawable(d);
            }
        }
    }

    @Override
    public void onChanged(final MemberInfo info) {
        // TODO Auto-generated method stub
        if (info == null) {
            Log.e(TAG, "onChanged: MemberInfo is null");
        }

        String number = mRcsMessageItem.mAddress;
        if (number == null || !number.equals(info.mNumber)) {
            Log.e(TAG, "onChanged: MemberInfo is wrong: number = " + number +
                            ", info.mNumber = " + info.mNumber);
            return;
        }
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mVisible) {
                        updateParticipantInfo(info.mName, info.mDrawable);
                    }
                }
            });
        }
    }

    private void showIpMessageDetail(RcsMessageItem msgItem) {
        Resources res = mRcsContext.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(res.getString(R.string.message_details_title))
        .setMessage(getIpTextMessageDetails(mRcsContext, msgItem))
        .setCancelable(true)
        .show();
    }

    public static String getIpTextMessageDetails(Context context, RcsMessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.rcs_message_type));


        // Address: ***
        int smsType = msgItem.mBoxId;
        if (msgItem.mRcsDirection == RcsLog.Direction.INCOMING) {
            details.append('\n');
            details.append(res.getString(R.string.from_label));
            details.append(msgItem.mAddress);
        } else {
            if (!msgItem.mIsGroupItem) {
                details.append('\n');
                details.append(res.getString(R.string.to_address_label));
                details.append(msgItem.mAddress);
            }
        }
        /*
        if (msgItem.mSentDate > 0 && smsType == Sms.MESSAGE_TYPE_INBOX) {
            details.append('\n');
            details.append(res.getString(R.string.sent_label));
            String dateStr = "";
            /// M: For OP09 @{
            if (MmsConfig.isFormatDateAndTimeStampEnable()) {
                dateStr = mmsUtils.formatDateAndTimeStampString(context, 0, msgItem.mSmsSentDate,
                                            true, dateStr);
            /// @}
            } else {
                dateStr = MessageUtils.formatTimeStampString(context, msgItem.mSmsSentDate, true);
            }

            details.append(dateStr);
        }
        */
        if (msgItem.mDate > 0L) {
            details.append('\n');
            if (msgItem.mRcsDirection == RcsLog.Direction.INCOMING) {
                details.append(res.getString(R.string.received_label));
            } else {
                details.append(res.getString(R.string.sent_label));
            }
            String dateStr = RcsUtilsPlugin.formatIpTimeStampString(msgItem.mDate, true);
            details.append(dateStr);
        }
        return details.toString();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: mDirection + " + mDirection + ", address = "
                                        + mRcsMessageItem.mAddress);
        if (mDirection == TYPE_OUTGOING) {
            Intent intent = new Intent(RcsProfile.PROFILE_ACTION);
            mContext.startActivity(intent);
            return;
        }
        if (mIsGroupItem) {
            mPortraitManager.invalidatePortrait(mRcsMessageItem.mAddress);
        }
        final String[] PHONE_LOOKUP_PROJECTION = new String[] {
                PhoneLookup._ID,
                PhoneLookup.LOOKUP_KEY,
            };
        Cursor cursor = null;
        Uri uri = null;
        try {
            cursor = mContext.getContentResolver().query(Uri.withAppendedPath(
                                PhoneLookup.CONTENT_FILTER_URI, mRcsMessageItem.mAddress),
                    PHONE_LOOKUP_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                String lookupKey = cursor.getString(1);
                uri = Contacts.getLookupUri(contactId, lookupKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "query error: " +e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "onClick: uri = " + uri);
        if (uri != null) {
            QuickContact.showQuickContact(mContext, mSenderPhoto, uri,
                    QuickContact.MODE_MEDIUM, null);
        } else {
            Uri createUri = Uri.fromParts("tel", mRcsMessageItem.mAddress, null);
            final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "Activity not exist");
            }
        }
    }

    public void setIsLastItem(boolean isLast) {
        mIsLastItem = isLast;
    }

    @Override
    public void drawRightStatusIndicator(Context context, IIpMessageItemExt item,
                                ImageView deliveredIndicator, ImageView detailsIndicator) {
        RcsMessageItem msgItem = (RcsMessageItem) item;
        if (!msgItem.mType.equals("rcs")) {
            return;
        }
        int resId = 0;
        switch (msgItem.mRcsStatus) {
        case RcsLog.MessageStatus.SENDING:
            resId = com.android.mms.R.drawable.im_meg_status_sending;
            break;
        case RcsLog.MessageStatus.SENT:
            resId = com.android.mms.R.drawable.im_meg_status_out;
            break;
        case RcsLog.MessageStatus.DELIVERED:
            resId = com.android.mms.R.drawable.im_meg_status_reach;
            break;
        case RcsLog.MessageStatus.FAILED:
            resId = com.android.mms.R.drawable.ic_list_alert_sms_failed;
            break;
        default:
            resId = 0;
            break;
        }
        if (resId > 0) {
            deliveredIndicator.setVisibility(View.VISIBLE);
            deliveredIndicator.setImageDrawable(context.getResources().getDrawable(resId));
        } else {
            deliveredIndicator.setVisibility(View.GONE);
        }
    }
}
