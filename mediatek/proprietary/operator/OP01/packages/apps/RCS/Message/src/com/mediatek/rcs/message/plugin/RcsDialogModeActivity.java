package com.mediatek.rcs.message.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.mipmap;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Message;
import android.provider.Settings;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Telephony.Mms.Draft;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MessageUtils;
import com.mediatek.mms.callback.IDialogModeActivityCallback;
import com.mediatek.mms.ipmessage.DefaultIpDialogModeActivityExt;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVCalendarMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.GroupThumbnail;
import com.mediatek.rcs.message.group.PortraitManager.onGroupPortraitChangedListener;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * Plugin implements. response DialogModeActivity.java in MMS host.
 *
 */
public class RcsDialogModeActivity extends DefaultIpDialogModeActivityExt implements
                onGroupPortraitChangedListener {

    private static String TAG = "RcseDialogModeActivity";
    private TextView mGroupSender;
    private ImageView mContactImageView;
    private final Map<Uri, IpMessage> mIpMessages;
    private final Map<String, Bitmap> mBitmaps;
    private List<Uri> mUris;
    private View mMmsView;
    private TextView mSmsContentText;
    private EditText mEditor;

    private ViewGroup mIpViewHost;
    private View mIpView;
    private ImageView mBurnMessageImage;

    private View mIpImageView; // ip_image | ip_video
    private ImageView mImageContent; // image_content
    private ImageView mMediaPlayView; //video play

    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info

    private View mIpVCardView; //vcard
    private ImageView mVCardIcon;
    private TextView mVCardInfo;
    private View mIpGeolocView;

    private Activity mActivity;
    private Context mRcsContext;
    private Context mHostContext;
    private Cursor mCursor;
    private String mChatId;
    private IDialogModeActivityCallback mCallback;

    private long mCurThreadId;

    GroupThumbnail mThumbnail;

    private static final String RCS_MESSAGE_UNREAD_SELECTION =
            "(" + MessageColumn.MESSAGE_STATUS + "=" +MessageStatus.UNREAD
            +" OR " + MessageColumn.SEEN + "=0)";

    private static final String[] RCS_MESSAGE_PROJECTION = {
        MessageColumn.ID,               /*this item is same order number as host SMS/MMS*/
        MessageColumn.CONVERSATION,     /*this item is same order number as host SMS/MMS*/
        MessageColumn.CONTACT_NUMBER,
        MessageColumn.TIMESTAMP,        /*this item is same order number as host SMS/MMS*/
        MessageColumn.MESSAGE_STATUS,
        MessageColumn.BODY,
        MessageColumn.SUB_ID,           /*this item is same order number as host SMS/MMS*/
        MessageColumn.CHAT_ID,
    };
    private static final int RCS_COLUMN_ID = 0;
    private static final int RCS_COLUMN_TID = 1;
    private static final int RCS_COLUMN_ADDR = 2;
    private static final int RCS_COLUMN_DATE = 3;
    private static final int RCS_COLUMN_READ = 4;
    private static final int RCS_COLUMN_BODY = 5;
    private static final int RCS_COLUMN_SUB = 6;
    private static final int RCS_COLUMN_CHAT_ID = 7;

    private static final int SMS_ADDR = 2;
    private static final int PREVIEW_IMAGE_SIZE = 90;

    public RcsDialogModeActivity(Context hostContext, Context rcsContext) {
//        mIpMessageUris = new ArrayList<Uri>();
        mIpMessages = new HashMap<Uri, IpMessage>();
        mBitmaps = new HashMap<String, Bitmap>();
        mHostContext = hostContext;
        mRcsContext = rcsContext;
    }

    @Override
    public boolean onIpInitDialogView(Activity context, List<Uri> uris, View mmsView,
            Cursor cursor, IDialogModeActivityCallback callback, EditText replyEditor,
            TextView smsContentText, View ipView, TextView groupSender, ImageView contactImage) {
        mActivity = context;
        mUris = uris;
        mMmsView = mmsView;
        mEditor = replyEditor;
        mSmsContentText = smsContentText;
        mIpViewHost = (ViewGroup)ipView;
        mCallback = callback;
        mContactImageView = contactImage;
        return true;
    }

    private void initIpView() {
        if (mIpView == null) {
            LayoutInflater inflater = LayoutInflater.from(mRcsContext);
            mIpView = inflater.inflate(R.layout.dialog_mode_ipmsg, mIpViewHost, true);
            mBurnMessageImage = (ImageView) mIpView.findViewById(R.id.rcs_burn_message);
            mIpImageView = mIpView.findViewById(R.id.ip_image);
            mImageContent = (ImageView)mIpView.findViewById(R.id.image_content);
            mMediaPlayView = (ImageView)mIpView.findViewById(R.id.video_media_play);
            mIpAudioView = mIpView.findViewById(R.id.ip_audio);
            mAudioIcon = (ImageView)mIpView.findViewById(R.id.ip_audio_icon);
            mAudioInfo = (TextView)mIpView.findViewById(R.id.audio_info);

            mIpVCardView = mIpView.findViewById(R.id.ip_vcard);
            mVCardIcon = (ImageView)mIpView.findViewById(R.id.ip_vcard_icon);
            mVCardInfo = (TextView)mIpView.findViewById(R.id.vcard_info);
            mIpGeolocView = mIpView.findViewById(R.id.ip_geoloc);
        }
    }

    @Override
    public boolean onIpAddNewUri(Intent intent, Uri newUri) {
        if (intent.getBooleanExtra("ipmessage", false)
                || newUri.getAuthority().equals(MessageColumn.CONTENT_URI.getAuthority())) {
            Log.d(TAG, "receiver a ipmessage,uri:" + newUri.toString());
            long msgId = Long.parseLong(newUri.getLastPathSegment());
            IpMessage ipMsg  =
                        RCSMessageManager.getInstance().getRCSMessageInfo(msgId);
            mIpMessages.put(newUri, ipMsg);
        }
        return super.onIpAddNewUri(intent, newUri);
    }

    @Override
    public boolean onIpDestroy() {
//        IpMessageUtils.removeIpMsgNotificationListeners(mContext, this);
//        return true;
        mIpMessages.clear();
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
        return super.onIpDestroy();
    }

    @Override
    public Cursor loadCurMsg(Uri uri, String[] projection, String selection) {
        Log.d(TAG, "[loadCurMsg]: uri = " + uri);
        if (uri.getAuthority().equals(MessageColumn.CONTENT_URI.getAuthority())) {
            Log.d(TAG, "rcs item");
            Cursor cursor = mHostContext.getContentResolver().query(uri, RCS_MESSAGE_PROJECTION,
                    RCS_MESSAGE_UNREAD_SELECTION, null, null);
            if (cursor == null) {
                Log.e(TAG, "[loadCurMsg]: no content loaded");
                mActivity.finish();
                return null;
            }
            if (cursor.moveToFirst()) {
                mChatId = cursor.getString(RCS_COLUMN_CHAT_ID);
            }
            mCursor = cursor;
            return cursor;
        } else {
            mCursor = null;
            mChatId = null;
            return null;
        }
    }

    /**
     * Implements onIpSetDialogView.
     * @return CharSequence
     */
    public String onIpSetDialogView() {
        IpMessage ipMessage = getCurIpMessage();
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
        if (ipMessage != null && mCursor != null) {
            if (mMmsView != null) {
                Log.d(TAG, "Hide MMS views");
                mMmsView.setVisibility(View.GONE);
            }
            initIpView();
            mIpViewHost.setVisibility(View.VISIBLE);
            Log.d(TAG, "id:" + ipMessage.getIpDbId() + ",type:" + ipMessage.getType());
            if (ipMessage.getBurnedMessage()) {
                setBurnMessageItem(ipMessage);
            } else {
                mBurnMessageImage.setVisibility(View.GONE);
                switch (ipMessage.getType()) {
                    case IpMessageType.TEXT:
                        setIpTextItem((IpTextMessage) ipMessage);
                        break;
                    case IpMessageType.EMOTICON:
                        setIpEmoticonItem((IpTextMessage) ipMessage);
                        break;
                    case IpMessageType.PICTURE:
                        setIpImageItem((IpImageMessage) ipMessage);
                        break;
                    case IpMessageType.VOICE:
                        setIpVoiceItem((IpVoiceMessage) ipMessage);
                        break;
                    case IpMessageType.VCARD:
                        setIpVCardItem((IpVCardMessage) ipMessage);
                        break;
                    case IpMessageType.VIDEO:
                        setIpVideoItem((IpVideoMessage) ipMessage);
                        break;
                    case IpMessageType.GEOLOC:
                        setIpGeolocItem((IpGeolocMessage)ipMessage);
                        break;
                    case IpMessageType.UNKNOWN_FILE:
                        Log.w(TAG, "Unknown IP message type. type = " + ipMessage.getType());
                        break;
                    default:
                        Log.w(TAG, "Error IP message type. type = " + ipMessage.getType());
                        break;
                }
            }
            //udpate group drawable
            if (!TextUtils.isEmpty(mChatId)) {
                ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mChatId);
                if (info != null) {
                    mThumbnail = PortraitManager.getInstance().getGroupPortrait(info.getChatId());
                    if (mThumbnail != null) {
                        mContactImageView.setImageBitmap(mThumbnail.mBitmap);
                    }
                    mThumbnail.addChangedListener(this);
                }
            }
        } else {
            mIpViewHost.setVisibility(View.GONE);
        }
        return null;
    }

    @Override
    public String onIpGetSenderString() {
        Uri uri = mCallback.opGetCurUri();
        if (!TextUtils.isEmpty(mChatId)) {
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mChatId);
            if (info != null) {
                String groupName = info.getNickName();
                if (TextUtils.isEmpty(groupName)) {
                    groupName = info.getSubject();
                }
                return groupName;
            }
        }
        return super.onIpGetSenderNumber();
    }



    @Override
    public boolean onIpSendReplySms(String body, String to) {
        long threadId = mCallback.getIpThreadId();
        Log.d(TAG, "onIpSendReplySms: threadId=" + threadId);
        if (!TextUtils.isEmpty(mChatId)) {
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(mChatId);
            if (info != null) {
                //group chat
                IpTextMessage msg = new IpTextMessage();
                msg.setBody(body);
                sendIpTextMessage(msg);
                return true;
            }
        }
        return super.onIpSendReplySms(body, to);
    }

    @Override
    public boolean onIpSendMessage(String body, String to) {
        if (RCSServiceManager.getInstance().isServiceEnabled()) {
            int selected = mCallback.getIpSelectedId();
            int rcsSubId = RcsMessageUtils.getRcsSubId(mActivity);
            if (selected == rcsSubId) {
                IpTextMessage msg = new IpTextMessage();
                msg.setBody(body);
                msg.setTo(to);
                sendIpTextMessage(msg);
                return true;
            }
        }
        return super.onIpSendMessage(body, to);
    }


    // / M: add for ipmessage
    private IpMessage getCurIpMessage() {
        IpMessage ipmsg = null;
        Uri curUri;
        if (mUris.size() <= 0) {
            mCallback.setCurUriIdx(0, null);
            return null;
        }
        curUri = (Uri) mUris.get(mCallback.getCurUriIdx());
        if (curUri != null) {
            ipmsg = mIpMessages.get(curUri);
            Log.d(TAG, "check ipMsg " + ipmsg);
        }
        return ipmsg;
    }

    private void setBurnMessageItem(IpMessage message) {
        mBurnMessageImage.setVisibility(View.VISIBLE);
        mSmsContentText.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
    }

    private void setIpTextItem(IpTextMessage textMessage) {
        Log.d(TAG, "setIpTextItem()");
        if (TextUtils.isEmpty(textMessage.getBody())) {
            Log.w(TAG, "setIpTextItem(): No message content!");
            return;
        }
        String body = textMessage.getBody();
        if (TextUtils.isEmpty(body)) {
            body = mSmsContentText.getText().toString();
        }
        EmojiImpl emoji = EmojiImpl.getInstance(mActivity);
        CharSequence formattedMessage = emoji.getEmojiExpression(body, true);
        mSmsContentText.setVisibility(View.VISIBLE);
        mSmsContentText.setText(formattedMessage);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpImageView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }

    private void setIpEmoticonItem(IpTextMessage emoticonMessage) {
        Log.d(TAG, "setIpEmoticonItem()");
        mIpImageView.setVisibility(View.VISIBLE);
        String emXml = emoticonMessage.getBody();
        String emId = EmojiShop.getEmIdByXml(emXml);
        if (EmojiShop.isLocalEmoticon(emId)) {
            String path = EmojiShop.getEmResPath(emXml);
            Log.d(TAG, "setIpEmShopItem(): path = " + path);
            if (path != null) {
                mImageContent.setImageURI(Uri.fromFile(new File(path)));
            }
        } else {
            mImageContent.setTag(emId);
            // use default image
            Drawable defaultDrawable = mRcsContext.getResources().
                                        getDrawable(R.drawable.ic_missing_thumbnail_picture);
            mImageContent.setImageDrawable(defaultDrawable);
        //    EmojiShop.addEmDownloadListener(this);
        //    EmojiShop.loadEmIconsFromServer((Activity) mContext ,emId);
         }
         // / M: add for ipmessage, hide text view
         mMediaPlayView.setVisibility(View.GONE);
         mIpAudioView.setVisibility(View.GONE);
         mIpVCardView.setVisibility(View.GONE);
         mIpGeolocView.setVisibility(View.GONE);
         mSmsContentText.setVisibility(View.GONE);
     }

    private void setIpImageItem(IpImageMessage imageMessage) {
        Log.d(TAG, "setIpImageItem()");
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.INVISIBLE);
        String path = imageMessage.getThumbPath();
        Bitmap bitmap = getImageBitmap(path);
        if (bitmap != null) {
            mImageContent.setImageBitmap(bitmap);
        } else {
            Drawable defaultDrawable = mRcsContext.getResources().
                                        getDrawable(R.drawable.ic_missing_thumbnail_picture);
            mImageContent.setImageDrawable(defaultDrawable);
        }

        // / M: add for ipmessage, hide text view
        mMediaPlayView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }

    private void setIpGeolocItem(IpGeolocMessage imageMessage) {
        Log.d(TAG, "setIpImageItem()");
        mIpGeolocView.setVisibility(View.VISIBLE);

        // / M: add for ipmessage, hide text view
        mIpImageView.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }

    private Bitmap getImageBitmap(String filePath) {
        Log.d(TAG, "decodeImageBitmap(): filePath = " + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        Bitmap bitmap = mBitmaps.get(filePath);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = RcsMessageUtils.getBitmapByPath(filePath, options,
                                        PREVIEW_IMAGE_SIZE, PREVIEW_IMAGE_SIZE);
            if (bitmap != null) {
                mBitmaps.put(filePath, bitmap);
            }
        }
        return bitmap;
    }

    private void setIpVideoItem(IpVideoMessage videoMessage) {
        String path = videoMessage.getThumbPath();
        Log.d(TAG, "setIpVideoItem(): path = " + path);
        Bitmap bitmap = getVideoThumbnail(path);
        if (bitmap != null) {
            mImageContent.setImageBitmap(bitmap);
        } else {
            Drawable defaultDrawable = mRcsContext.getResources().
                                getDrawable(R.drawable.ic_missing_thumbnail_picture);
            mImageContent.setImageDrawable(defaultDrawable);
        }
        mMediaPlayView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        mSmsContentText.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
    }

    private Bitmap getVideoThumbnail(String filePath) {
        Log.d(TAG, "setVideoView(): filePath = " + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        Bitmap bitmap = mBitmaps.get(filePath);
        if (bitmap == null) {
            bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MICRO_KIND);
            if (bitmap != null) {
                mBitmaps.put(filePath, bitmap);
            }
        }
        return bitmap;
    }

    private void setIpVoiceItem(IpVoiceMessage voiceMessage) {
        Log.d(TAG, "setIpVoiceItem(): message Id = " + voiceMessage.getIpDbId());

        mAudioInfo.setText(RcsMessageUtils.formatFileSize(voiceMessage.getSize()));
//        mAudioDur.setText(RcsMessageUtils.formatAudioTime(voiceMessage.getDuration()));
        mIpAudioView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.GONE);
        mIpVCardView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
    }

    private void setIpVCardItem(IpVCardMessage vCardMessage) {
        Log.d(TAG, "setIpVCardItem(): vCardMessage = " + vCardMessage);
        String name = vCardMessage.getName();
        if (name != null && name.lastIndexOf(".") != -1) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        mVCardInfo.setText(name);
        mIpVCardView.setVisibility(View.VISIBLE);
        mIpImageView.setVisibility(View.GONE);
        mIpAudioView.setVisibility(View.GONE);
        mIpGeolocView.setVisibility(View.GONE);
        mSmsContentText.setVisibility(View.GONE);
        // / M: add for ipmessage, hide image view or video view
    }

    private void sendIpTextMessage(final IpTextMessage ipMessage) {
        final long threadId = mCallback.getIpThreadId();
        Uri uri = (Uri) mUris.get(mCallback.getCurUriIdx());
        mCallback.markIpAsRead(uri);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendMessageForIpMsg(): calling API: saveIpMsg().");
                int ret = -1;
                ipMessage.setStatus(RcsLog.MessageStatus.SENDING);
                mCallback.onPreMessageSent();
                ret = RCSMessageManager.getInstance().sendRCSMessage(mChatId, ipMessage);
                mCallback.onMessageSent();
            }
        }).start();
    }

    @Override
    public void onChanged(Bitmap newBitmap) {
        // TODO Auto-generated method stub
        mContactImageView.setImageBitmap(newBitmap);
    }

    @Override
    public boolean markAsRead(Context context, Uri uri) {
        if (uri != null && uri.getAuthority().equals(MessageColumn.CONTENT_URI.getAuthority())) {
            //rcs
            final ContentValues values = new ContentValues(2);
            values.put(MessageColumn.SEEN, 1);
            values.put(MessageColumn.MESSAGE_STATUS, RcsLog.MessageStatus.READ);
            context.getContentResolver().update(uri, values, null, null);
            MessagingNotification.blockingUpdateNewMessageIndicator(context,
                    MessagingNotification.THREAD_NONE, false, null);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getReceivedTime(Context context, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "cursor is null");
            return null;
        }
        if (cursor.moveToFirst()) {
            long date = cursor.getLong(RCS_COLUMN_DATE);
            return MessageUtils.formatTimeStampString(context, date);
        }
        return null;
    }
}
