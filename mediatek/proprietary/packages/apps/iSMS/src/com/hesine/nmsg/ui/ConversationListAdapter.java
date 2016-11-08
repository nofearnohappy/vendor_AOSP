package com.hesine.nmsg.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
//import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.AttachInfo;
import com.hesine.nmsg.business.bean.ImageInfo;
import com.hesine.nmsg.business.bean.LocalMessageInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.bean.UserInfo;
import com.hesine.nmsg.business.bo.ImageLoader;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.DateTool;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.common.Image;
import com.hesine.nmsg.common.MLog;

public class ConversationListAdapter extends BaseAdapter {
    public static final int TIME_GAP = 5 * 60 * 1000;// message time view gap 5
                                                     // minutes
    private LayoutInflater mInflater;
    private Context mContext;
    protected Cursor mCursor;
    protected int mRowIDColumn;
    private int mScreenWidth = 0;
    private int mSingleMultipoleImageWith = 0;
    private int mPlayRecordId;
    private String mAccount;
    private HashMap<Integer, LocalMessageInfo> mMsgCache;
    // private LruCache<String, Bitmap> mImageCache;
    private ImageLoader mImageLoader = null;
    private int mUserIconIndex = 0;
    private int[] userAvadars = new int[] { R.drawable.avadar1, R.drawable.avadar2,
            R.drawable.avadar3, R.drawable.avadar4, R.drawable.avadar5, R.drawable.avadar6,
            R.drawable.avadar7, R.drawable.avadar8, R.drawable.avadar9 };// ,
    TextView mTime;
    TextView mType;
    TextView mMsgBody;
    ImageView mMsgPhoto;

    private static final int NMSG_TEXT_FROM_VIEW = 0;
    private static final int NMSG_IMAGE_FROM_VIEW = 1;
    private static final int NMSG_AUDIO_FROM_VIEW = 2;
    private static final int NMSG_TEXT_TO_VIEW = 3;
    private static final int NMSG_IMAGE_TO_VIEW = 4;
    private static final int NMSG_AUDIO_TO_VIEW = 5;
    private static final int NMSG_VIDEO_FROM_VIEW = 6;
    private static final int NMSG_SINGLE_VIEW = 7;
    private static final int NMSG_MULTIPOLE_VIEW = 8;
    private static final int NMS_READ_MODE_MAX = 9;
    // private Vibrator vibrator;
    private Bitmap mAccountBitmap = null;

    private class ViewHolderFromText {
        TextView timeView;
        TextView msgView;
        ImageView avatar;
    };

    private class ViewHolderToText {
        TextView timeView;
        TextView msgView;
        ImageView msgStatus;
        ImageView avatar;
    };

    private class ViewHolderFromImage {
        TextView timeView;
        ImageView msgPic;
        ImageView avatar;
        ImageView imageFrame;
    };

    private class ViewHolderToImage {
        TextView timeView;
        ImageView msgPic;
        ImageView msgStatus;
        ImageView avatar;
        ImageView imageFrame;
    };

    private class ViewHolderFromVideo {
        TextView timeView;
        // ImageView msgPic;
        ImageView avatar;
        // ImageView imageFrame;
        TextView descView;
    };

    private class ViewHolderFromAudio {
        TextView timeView;
        ImageView playState;
        TextView audioTime;
        ImageView avatar;
    };

    private class ViewHolderToAudio {
        TextView timeView;
        ImageView msgStatus;
        ImageView playState;
        TextView audioTime;
        ImageView avatar;
    };

    private class ViewHolderSingle {
        TextView timeView;
        TextView subject;
        TextView summary;
        ImageView msgPic;
    };

    private class ViewHolerMultipoleItem {
        TextView subjectView;
        ImageView msgPic;
        View divider;
    };

    private class ViewHolderMultipole {
        TextView timeView;
        TextView subject;
        // TextView summary;
        ImageView msgPic;
        LinearLayout container;
        List<ViewHolerMultipoleItem> items;
    };

    public LocalMessageInfo getMsg(int position) {
        LocalMessageInfo msg = mMsgCache.get(position);
        if (msg == null) {
            msg = getItemViaCursor(position);
            mMsgCache.put(position, msg);
        }
        return msg;
    }

    public void refresh() {
        ServiceInfo si = DBUtils.getServiceInfo(mAccount);
        if (si != null) {
            mAccountBitmap = BitmapFactory.decodeFile(si.getIcon());
        }
        if (Config.getUuid() != null) {
            UserInfo ui = DBUtils.getUser(Config.getUuid());
            if (ui != null) {
                mUserIconIndex = ui.getIcon();
            }
        } else {
            mUserIconIndex = 0;
        }
    }

    @SuppressLint("UseSparseArrays")
    public ConversationListAdapter(Context context, String account) {
        mContext = context;
        mAccount = account;

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMsgCache = new HashMap<Integer, LocalMessageInfo>();
        // mImageCache = new LruCache<String, Bitmap>(20);
        // vibrator = (Vibrator) context
        // .getSystemService(Context.VIBRATOR_SERVICE);
        mCursor = DBUtils.getMessageCursor(account);
        mImageLoader = new ImageLoader();
        mImageLoader.setListener(new Pipe() {
            @Override
            public void complete(Object owner, Object data, int success) {
                notifyDataSetChanged();
            }
        });
        initSomeSpecifics();
    }

    private void initSomeSpecifics() {
        if (mScreenWidth == 0) {
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wmg = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wmg.getDefaultDisplay().getMetrics(dm);
            if (dm.heightPixels > dm.widthPixels) {
                mScreenWidth = dm.widthPixels;
            } else {
                mScreenWidth = dm.heightPixels;
            }
            mSingleMultipoleImageWith = (int) (mScreenWidth - 40 * (mContext.getResources()
                    .getDisplayMetrics().density));
        }
    }

    public void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    public void changeCursor(Cursor cursor) {
        Cursor oldCursor = null;
        if (mCursor == cursor) {
            return;
        } else {
            oldCursor = mCursor;
            mCursor = cursor;
        }
        mMsgCache.clear();
        notifyDataSetChanged();
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    public LocalMessageInfo getItemViaCursor(int position) {
        if (mCursor != null) {
            mCursor.moveToPosition(position);
            return DBUtils.getMsgViaCursor(mCursor);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                if (mRowIDColumn < 0) {
                    mRowIDColumn = mCursor.getColumnIndexOrThrow("_id");
                }
                return mCursor.getLong(mRowIDColumn);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getItemViewTypeViaMsg(LocalMessageInfo msg) {
        int dispType = getDisplayType(msg.getType(), msg.getStatus());
        return dispType;
    }

    @Override
    public int getItemViewType(int position) {
        LocalMessageInfo msg = getMsg(position);
        int dispType = getItemViewTypeViaMsg(msg);
        return dispType;
    }

    @Override
    public int getViewTypeCount() {
        return NMS_READ_MODE_MAX;
    }

    public String getTime(int position, long timeStamp) {
        boolean ret = false;
        //MLog.debug("getTime:" + position);

        if (position == 0) {
            ret = true;
        } else {
            long lastTimeStamp = getMsg(position - 1).getTime();
            if (timeStamp - lastTimeStamp > TIME_GAP) {
                ret = true;
            }
        }

        if (ret) {
            return DateTool.getCurrentFormatTime(timeStamp);
        } else {
            return null;
        }
    }

    public String getAttachmentViaMsg(LocalMessageInfo msg, int index) {
        String attachment = null;
        int dispType = getItemViewTypeViaMsg(msg);
        if (dispType == NMSG_AUDIO_FROM_VIEW || dispType == NMSG_IMAGE_FROM_VIEW
                || dispType == NMSG_SINGLE_VIEW || dispType == NMSG_MULTIPOLE_VIEW
                || dispType == NMSG_VIDEO_FROM_VIEW) {
            attachment = AttachInfo.getAttachmentAbsPathByUrl(msg.getMessageItems().get(index)
                    .getAttachInfo());
            if (!FileEx.isFileExisted(attachment)) {
                ImageInfo ii = new ImageInfo();
                ii.setPath(attachment);
                ii.setUrl(msg.getMessageItems().get(index).getAttachInfo().getUrl());
                mImageLoader.request(ii);
            }
        } else {
            attachment = AttachInfo.getAttachmentAbsPath(msg.getMessageItems().get(index)
                    .getAttachInfo());
        }
        return attachment;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LocalMessageInfo msg = getMsg(position);
        int dispType = getItemViewTypeViaMsg(msg);
        ViewHolderFromText holderFromText = null;
        ViewHolderToText holderToText = null;
        ViewHolderFromImage holderFromPic = null;
        ViewHolderFromAudio holderFromAudio = null;
        ViewHolderToImage holderToPic = null;
        ViewHolderToAudio holderToAudio = null;
        ViewHolderFromVideo holderFromVideo = null;
        ViewHolderSingle holderSingle = null;
        ViewHolderMultipole holderMultipole = null;

        if (convertView == null) {
            switch (dispType) {
                case NMSG_TEXT_FROM_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_text, null);
                    holderFromText = new ViewHolderFromText();
                    holderFromText.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderFromText.msgView = (TextView) convertView.findViewById(R.id.chat_msgcont);
                    holderFromText.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    convertView.setTag(holderFromText);
                    break;
                case NMSG_TEXT_TO_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_to_text, null);
                    holderToText = new ViewHolderToText();
                    holderToText.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderToText.msgView = (TextView) convertView.findViewById(R.id.chat_msgcont);
                    holderToText.msgStatus = (ImageView) convertView.findViewById(R.id.chat_status);
                    holderToText.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    convertView.setTag(holderToText);
                    break;
                case NMSG_IMAGE_FROM_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_image, null);
                    holderFromPic = new ViewHolderFromImage();
                    holderFromPic.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderFromPic.msgPic = (ImageView) convertView
                            .findViewById(R.id.chat_from_picture);
                    holderFromPic.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    holderFromPic.imageFrame = (ImageView) convertView
                            .findViewById(R.id.image_frame);
                    convertView.setTag(holderFromPic);
                    break;
                case NMSG_IMAGE_TO_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_to_image, null);
                    holderToPic = new ViewHolderToImage();
                    holderToPic.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderToPic.msgStatus = (ImageView) convertView.findViewById(R.id.chat_status);
                    holderToPic.msgPic = (ImageView) convertView.findViewById(R.id.chat_to_picture);
                    holderToPic.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    holderToPic.imageFrame = (ImageView) convertView.findViewById(R.id.image_frame);
                    convertView.setTag(holderToPic);
                    break;
                case NMSG_VIDEO_FROM_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_video, null);
                    holderFromVideo = new ViewHolderFromVideo();
                    holderFromVideo.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderFromVideo.descView = (TextView) convertView
                            .findViewById(R.id.chat_from_video_text);
                    holderFromVideo.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    // holderFromVideo.imageFrame = (ImageView) convertView
                    // .findViewById(R.id.image_frame);
                    convertView.setTag(holderFromVideo);
                    break;

                case NMSG_AUDIO_FROM_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_audio, null);
                    holderFromAudio = new ViewHolderFromAudio();
                    holderFromAudio.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderFromAudio.playState = (ImageView) convertView
                            .findViewById(R.id.chat_from_audio);
                    holderFromAudio.audioTime = (TextView) convertView
                            .findViewById(R.id.chat_from_audio_time);
                    holderFromAudio.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    convertView.setTag(holderFromAudio);
                    break;
                case NMSG_AUDIO_TO_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_to_audio, null);
                    holderToAudio = new ViewHolderToAudio();
                    holderToAudio.timeView = (TextView) convertView.findViewById(R.id.chat_time);

                    holderToAudio.msgStatus = (ImageView) convertView
                            .findViewById(R.id.chat_status);
                    holderToAudio.playState = (ImageView) convertView
                            .findViewById(R.id.chat_to_audio);
                    holderToAudio.audioTime = (TextView) convertView
                            .findViewById(R.id.chat_to_audio_time);
                    holderToAudio.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                    convertView.setTag(holderToAudio);
                    break;
                case NMSG_SINGLE_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_single, null);
                    holderSingle = new ViewHolderSingle();
                    holderSingle.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderSingle.msgPic = (ImageView) convertView.findViewById(R.id.image);
                    holderSingle.subject = (TextView) convertView.findViewById(R.id.subject);
                    holderSingle.summary = (TextView) convertView.findViewById(R.id.summary);
                    convertView.setTag(holderSingle);
                    break;

                case NMSG_MULTIPOLE_VIEW:
                    convertView = mInflater.inflate(R.layout.chat_item_from_multipole, null);
                    holderMultipole = new ViewHolderMultipole();
                    holderMultipole.timeView = (TextView) convertView.findViewById(R.id.chat_time);
                    holderMultipole.msgPic = (ImageView) convertView.findViewById(R.id.image);
                    holderMultipole.subject = (TextView) convertView.findViewById(R.id.subject);
                    holderMultipole.container = (LinearLayout) convertView
                            .findViewById(R.id.container);
                    holderMultipole.items = new ArrayList<ViewHolerMultipoleItem>();
                    convertView.setTag(holderMultipole);
                    break;

                default:
                    MLog.error("unknow display type");
                    break;
            }
        } else {
            switch (dispType) {
                case NMSG_TEXT_FROM_VIEW:
                    holderFromText = (ViewHolderFromText) convertView.getTag();
                    break;
                case NMSG_TEXT_TO_VIEW:
                    holderToText = (ViewHolderToText) convertView.getTag();
                    break;
                case NMSG_IMAGE_FROM_VIEW:
                    holderFromPic = (ViewHolderFromImage) convertView.getTag();
                    break;
                case NMSG_IMAGE_TO_VIEW:
                    holderToPic = (ViewHolderToImage) convertView.getTag();
                    break;
                case NMSG_VIDEO_FROM_VIEW:
                    holderFromVideo = (ViewHolderFromVideo) convertView.getTag();
                    break;
                case NMSG_MULTIPOLE_VIEW:
                    holderMultipole = (ViewHolderMultipole) convertView.getTag();
                    break;
                case NMSG_SINGLE_VIEW:
                    holderSingle = (ViewHolderSingle) convertView.getTag();
                    break;
                case NMSG_AUDIO_FROM_VIEW:
                    holderFromAudio = (ViewHolderFromAudio) convertView.getTag();
                    break;
                case NMSG_AUDIO_TO_VIEW:
                    holderToAudio = (ViewHolderToAudio) convertView.getTag();
                    break;

                default:
                    MLog.error("unknow display type");
                    break;
            }
        }
        if (position < 0 || position >= getCount()) { // this judgment should be
                                                      // done in the top ?????
            return convertView;
        }

        String time = getTime(position, msg.getTime());
        String msgBody = msg.getMessageItems().get(0).getBody();
        String attachment = getAttachmentViaMsg(msg, 0);
        if (msgBody != null && !TextUtils.isEmpty(msgBody)) {
            msgBody = msgBody.replace("\r", "");
            while (msgBody.endsWith("\n")) {
                int index = msgBody.lastIndexOf("\n");
                msgBody = msgBody.substring(0, index);
            }
        }
        SpannableStringBuilder styles = setSpan(msgBody);
        int drawable = getDispStatus(msg.getStatus());
        switch (dispType) {
            case NMSG_TEXT_FROM_VIEW:
                holderFromText.msgView.setText(styles);
                setTimeView(holderFromText.timeView, time);
                setSpanClickable(holderFromText.msgView);
                setContactAvatar(holderFromText.avatar, msg.getFrom());
                break;
            case NMSG_TEXT_TO_VIEW:
                holderToText.msgView.setText(styles);
                setTimeView(holderToText.timeView, time);
                setSpanClickable(holderToText.msgView);
                setStatusView(holderToText.msgStatus, drawable, msg);
                setContactAvatar(holderToText.avatar, msg.getFrom());
                break;
            case NMSG_IMAGE_FROM_VIEW:
                setTimeView(holderFromPic.timeView, time);
                setPicView(holderFromPic.msgPic, attachment, R.drawable.ic_lose_small, false,
                        holderFromPic.imageFrame);
                setContactAvatar(holderFromPic.avatar, msg.getFrom());
                break;
            case NMSG_IMAGE_TO_VIEW:
                setTimeView(holderToPic.timeView, time);
                setPicView(holderToPic.msgPic, attachment, R.drawable.ic_lose_small, false,
                        holderToPic.imageFrame);
                setStatusView(holderToPic.msgStatus, drawable, msg);
                setContactAvatar(holderToPic.avatar, msg.getFrom());
                break;
            case NMSG_VIDEO_FROM_VIEW:
                setTimeView(holderFromVideo.timeView, time);
                holderFromVideo.descView.setText(msg.getMessageItems().get(0).getSubject());
                setContactAvatar(holderFromVideo.avatar, msg.getFrom());
                break;
            case NMSG_MULTIPOLE_VIEW:
                setTimeView(holderMultipole.timeView, time);
                setPicView(holderMultipole.msgPic, attachment, R.drawable.ic_lose_big, true, null);
                holderMultipole.subject.setText(msg.getMessageItems().get(0).getSubject());

                holderMultipole.items.clear();
                holderMultipole.container.removeAllViews();
                for (int i = 1; i < msg.getMessageItems().size(); i++) {
                    View v = mInflater.inflate(R.layout.chat_item_multipole_subitem, null);
                    ViewHolerMultipoleItem item = new ViewHolerMultipoleItem();
                    item.divider = v.findViewById(R.id.divider);
                    item.divider.setVisibility(View.VISIBLE);
                    item.subjectView = (TextView) v.findViewById(R.id.subject);
                    item.msgPic = (ImageView) v.findViewById(R.id.image);
                    item.subjectView.setText(msg.getMessageItems().get(i).getSubject());
                    attachment = getAttachmentViaMsg(msg, i);
                    setPicView(item.msgPic, attachment, R.drawable.ic_lose_small, false, null);
                    v.setTag(msg.getMessageItems().get(i));
                    v.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ((ConversationActivity) mContext).onItemClick(null, v, -1, -1);
                        }
                    });
                    v.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            ((ConversationActivity) mContext).onItemLongClick(null, v, -1, -1);
                            return true;
                        }
                    });
                    if (i == msg.getMessageItems().size() - 1) {
                        item.divider.setVisibility(View.GONE);
                    }
                    holderMultipole.items.add(item);
                    holderMultipole.container.addView(v);
                }
                break;
            case NMSG_SINGLE_VIEW:
                setTimeView(holderSingle.timeView, time);
                setPicView(holderSingle.msgPic, attachment, R.drawable.ic_lose_big, true, null);
                holderSingle.subject.setText(msg.getMessageItems().get(0).getSubject());
                holderSingle.summary.setText(msg.getMessageItems().get(0).getDesc());
                break;
            case NMSG_AUDIO_FROM_VIEW:
                setTimeView(holderFromAudio.timeView, time);
                setContactAvatar(holderFromAudio.avatar, msg.getFrom());
                if (mPlayRecordId >= 0
                        && mPlayRecordId == msg.getId()) {
                    holderFromAudio.playState.setImageResource(R.drawable.play);
                } else {
                    holderFromAudio.playState.setImageResource(R.drawable.ic_play_01);
                }
                if (attachment.indexOf("_") != -1) {
                    String attachmentName = msg.getMessageItems().get(0).getAttachInfo().getName();
                    String length = attachmentName.substring(0, attachmentName.indexOf("_"));
                    String text = length + "'";
                    holderFromAudio.audioTime.setText(text);
                } else {
                    holderFromAudio.audioTime.setText("");
                }
                break;
            case NMSG_AUDIO_TO_VIEW:
                setTimeView(holderToAudio.timeView, time);
                if (msg.getId() == mPlayRecordId) {
                    holderToAudio.playState.setImageResource(R.drawable.play);
                } else {
                    holderToAudio.playState.setImageResource(R.drawable.ic_play_01);
                }
                if (attachment.indexOf("_") != -1) {
                    String attachmentName = msg.getMessageItems().get(0).getAttachInfo().getName();
                    String length = attachmentName.substring(0, attachmentName.indexOf("_"));
                    String text = length + "'";
                    holderToAudio.audioTime.setText(text);
                } else {
                    holderToAudio.audioTime.setText("");
                }
                setStatusView(holderToAudio.msgStatus, drawable, msg);
                setContactAvatar(holderToAudio.avatar, msg.getFrom());
                break;

            default:
                MLog.error("unknow display type");
                break;
        }
        return convertView;
    }

    public void setPlayRecordId(int recordId) {
        mPlayRecordId = recordId;
    }

    public int getPlayRecordId() {
        return mPlayRecordId;
    }

    private void setSpanClickable(TextView v) {
        v.setMovementMethod(LinkMovementMethod.getInstance());
        v.setFocusable(false);
    }

    private void setContactAvatar(ImageView v, final String contactId) {

        if (contactId.equals(mAccount)) {
            if (mAccountBitmap != null) {
                v.setImageBitmap(mAccountBitmap);
            } else {
                Drawable avatar = mContext.getResources().getDrawable(R.drawable.ic_avatar);
                v.setImageDrawable(avatar);
            }
            v.setTag(contactId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = (String) v.getTag();
                    Intent intent = new Intent(mContext, VendorAccountSetting.class);
                    intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, id);
                    intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_THREADID,
                            ((ConversationActivity) mContext).getThreadId());
                    mContext.startActivity(intent);
                }
            });
        } else {
            Drawable avatar = mContext.getResources().getDrawable(userAvadars[mUserIconIndex]);
            v.setImageDrawable(avatar);
            v.setTag(contactId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = (String) v.getTag();

                    Intent intent = new Intent(mContext, UserInfoActivity.class);
                    intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, id);
                    mContext.startActivity(intent);
                }
            });
        }
    }

    private void setTimeView(TextView timeView, String time) {
        if (time == null) {
            timeView.setVisibility(View.GONE);
        } else {
            timeView.setVisibility(View.VISIBLE);
            timeView.setText(time);
        }
    }

    private void setStatusView(ImageView v, int drawable, LocalMessageInfo msg) {
        v.clearAnimation();
        if (drawable == 0) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);
            v.setImageResource(drawable);
            v.setTag(msg);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ((ConversationActivity) mContext).getResendDialog().show().setTag(v.getTag());
                }
            });
        }
    }

    private int getDispStatus(int status) {
        int id = 0;
        if (status == LocalMessageInfo.STATUS_TO_FAILED) {
            id = R.drawable.ic_msg_failed;
        }
        return id;
    }

    private void setPicView(ImageView v, String path, int defaultDrawable, boolean isWide,
            ImageView frame) {
        Drawable d = v.getDrawable();
        if (null != d) {
            BitmapDrawable bd = (BitmapDrawable) d;
            Bitmap bm = bd.getBitmap();
            if (null != bm && !bm.isRecycled()) {
                bm.recycle();
            }
        }
        Bitmap bitmap = null;// mImageCache.get(path);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(path, options);
            options.inJustDecodeBounds = false;
            int l = Math.max(options.outHeight, options.outWidth);
            int be = (int) (l / 500);
            if (be <= 0) {
                be = 1;
            }
            options.inSampleSize = be;
            if (null != bitmap && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
            bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap != null) {
                if (isWide) {
                    int targetWidth = mSingleMultipoleImageWith;
                    int targetHeight = (bitmap.getHeight() * targetWidth) / bitmap.getWidth();
                    bitmap = Image.resizeImage(bitmap, targetWidth, targetHeight, true);
                } else {
                    if (bitmap.getWidth() > mScreenWidth * 3 / 7) {
                        int w = mScreenWidth * 3 / 7;
                        if (w > bitmap.getWidth()) {
                            bitmap = Image.resizeImage(bitmap, bitmap.getWidth(),
                                    bitmap.getHeight(), true);
                        } else {
                            bitmap = Image
                                    .resizeImage(bitmap, mScreenWidth * 3 / 7, bitmap.getHeight()
                                            * (mScreenWidth * 3 / 7) / bitmap.getWidth(), true);
                        }
                    } else if (bitmap.getWidth() > mScreenWidth * 2 / 7) {
                        bitmap = Image.resizeImage(bitmap, mScreenWidth * 2 / 7,
                                bitmap.getHeight() * (mScreenWidth * 2 / 7) / bitmap.getWidth(),
                                true);
                    }
                }
                // mImageCache.put(path, bitmap);
            } else { 
                MLog.error("img decode error: " + path);
                FileEx.deleteFile(path);
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), defaultDrawable);
            }
        }

        if (frame != null) {
            LayoutParams lp = frame.getLayoutParams();
            lp.width = bitmap.getWidth();
            lp.height = bitmap.getHeight();
            frame.setLayoutParams(lp);
        }
        v.setImageBitmap(bitmap);

    }

    class MyURLSpan extends ClickableSpan {
        private String mUrl;

        MyURLSpan(String url) {
            mUrl = url;
        }

        @Override
        public void onClick(View widget) {

            if (mUrl.startsWith("http") && mUrl.contains("://")) {
                Message msg = new Message();
                msg.what = UiMessageId.NMS_IM_GET_WEB;
                Bundle bundle = new Bundle();
                bundle.putString(NmsgIntentStrId.NMS_IM_WEB, mUrl);
                msg.setData(bundle);
                ((ConversationActivity) mContext).mHandler.sendMessage(msg);
            }
            if (mUrl.startsWith("mailto:")) {
                Message msg = new Message();
                msg.what = UiMessageId.NMS_IM_GET_EMAIL;
                Bundle bundle = new Bundle();
                bundle.putString(NmsgIntentStrId.NMS_IM_EMAIL, mUrl);
                msg.setData(bundle);
                ((ConversationActivity) mContext).mHandler.sendMessage(msg);
            }
            if (mUrl.startsWith("tel:")) {
                Message msg = new Message();
                msg.what = UiMessageId.NMS_IM_GET_PHONE;
                Bundle bundle = new Bundle();
                bundle.putString(NmsgIntentStrId.NMS_IM_PHONENUM, mUrl);
                msg.setData(bundle);
                ((ConversationActivity) mContext).mHandler.sendMessage(msg);
            }
        }
    }

    private SpannableStringBuilder setSpan(CharSequence msg) {
        if (msg == null) {
            return null;
        }
        SpannableStringBuilder style = new SpannableStringBuilder(msg);
        Spannable s2 = Spannable.Factory.getInstance().newSpannable(msg);

        Linkify.addLinks(s2, Linkify.ALL);

        if (s2 instanceof Spannable) {
            int end = msg.length();
            URLSpan[] urls = s2.getSpans(0, end, URLSpan.class);
            style.clearSpans(); // should clear old spans
            for (URLSpan url : urls) {
                String temp = url.getURL();
                MyURLSpan myURLSpan = new MyURLSpan(temp);
                style.setSpan(myURLSpan, s2.getSpanStart(url), s2.getSpanEnd(url),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // String text = s2.toString();
        //
        // Pattern mPattern = CommonUtils.buildEmoticonPattern();
        // Matcher matcher = mPattern.matcher(text);
        // while (matcher.find()) {
        // Drawable drawable =
        // CommonUtils.getEmoticonsDrawable(matcher.group());
        // ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
        // style.setSpan(span, matcher.start(), matcher.end(),
        // Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        // }

        return style;
    }

    private int getDisplayType(int orignalType, int status) {
        int type = 0;
        boolean isFrom = false;
        if (status > LocalMessageInfo.STATUS_TO_SENT) {
            isFrom = true;
        }

        switch (orignalType) {
            case LocalMessageInfo.TYPE_TEXT:
                type = isFrom ? NMSG_TEXT_FROM_VIEW : NMSG_TEXT_TO_VIEW;
                break;
            case LocalMessageInfo.TYPE_AUDIO:
                type = isFrom ? NMSG_AUDIO_FROM_VIEW : NMSG_AUDIO_TO_VIEW;
                break;
            case LocalMessageInfo.TYPE_IMAGE:
                type = isFrom ? NMSG_IMAGE_FROM_VIEW : NMSG_IMAGE_TO_VIEW;
                break;
            case LocalMessageInfo.TYPE_VIDEO:
                type = NMSG_VIDEO_FROM_VIEW;
                break;
            case LocalMessageInfo.TYPE_SINGLE:
                type = NMSG_SINGLE_VIEW;
                break;
            case LocalMessageInfo.TYPE_MULTIPOLE:
                type = NMSG_MULTIPOLE_VIEW;
                break;
            case LocalMessageInfo.TYPE_SINGLE_LINK:
                type = NMSG_SINGLE_VIEW;
                break;
            case LocalMessageInfo.TYPE_MULTI_LINK:
                type = NMSG_MULTIPOLE_VIEW;
                break;
            default:
                type = NMSG_TEXT_FROM_VIEW;
                break;
        }
        return type;
    }

    public void notifyDataSetChanged() {
        // num = engineadapter.get().nmsUISetImMode(mContactId, 0, 1, 0);
        super.notifyDataSetChanged();
    }

}
