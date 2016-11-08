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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;
import com.mediatek.rcs.pam.ui.messageitem.MessageListItem;
import com.mediatek.rcs.pam.ui.messageitem.PAAudioService;
import com.mediatek.rcs.pam.util.Utils;

/**
 * This class provides view of a message in the messages list.
 */
public class PaMessageListItem extends LinearLayout {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final String TAG = "PA/PaMessageListItem";

    public static final int DOWNLOAD_TYPE_THUMBNAIL = 1;
    public static final int DOWNLOAD_TYPE_IMAGE = 2;
    public static final int DOWNLOAD_TYPE_AUDIO = 3;
    public static final int DOWNLOAD_TYPE_VIDEO = 4;

    private static final float MAX_SCALE = 0.4f;
    private static final float MIN_SCALE = 0.3f;

    private static final int AUDIO_MODE_PREVIEW = 0;
    private static final int AUDIO_MODE_PLAY = 1;

    private static final int DRAW_TYPE_NORMAL = 0;
    private static final int DRAW_TYPE_SMALL = 1;

    private int mAudioMode;

    private Handler mHandler;
    private TextView mDateView;
    private TextView mProgressView;
    public View mMessageBlock;

    private int mPosition;

    private CheckBox mSelectedBox;
    private View mTimeDivider;
    private TextView mTimeDividerStr;

    private LinearLayout mSendStatusBar;
    private ImageView mSendingIcon;
    private TextView mSendingText;
    private ImageView mSendingSuccessIcon;
    private TextView mSendingSuccessText;
    private ImageView mSendingFailIcon;
    private TextView mSendingFailText;

    private ViewGroup mListItemLayout;
    private MessageListItem mMessageListItem;

    private QuickContactBadge mSenderPhoto;

    private boolean mVisible;
    private boolean mIsSelect;
    private int mSendingProgress;

    PAAudioService mAudioService;

    public PaMessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "PaMessageListItem(Context context, AttributeSet attrs)");

        mIsSelect = false;
    }

    public void setSelectedState(boolean selectedState) {
        mIsSelect = selectedState;
    }

    public void setSendingProgress(int sendingProgress) {
        mSendingProgress = sendingProgress;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSelectedBox = (CheckBox) findViewById(R.id.select_check_box);

        mTimeDivider = (View) findViewById(R.id.time_divider);
        mTimeDividerStr = (TextView) mTimeDivider
                .findViewById(R.id.time_divider_str);

        mDateView = (TextView) findViewById(R.id.date_view);
        mProgressView = (TextView) findViewById(R.id.progress_text);
        mSendStatusBar = (LinearLayout) findViewById(R.id.status_panel);
        mSendingIcon = (ImageView) findViewById(R.id.delivered_sending);
        mSendingText = (TextView) findViewById(R.id.delivered_sending_txt);
        mSendingSuccessIcon = (ImageView) findViewById(R.id.delivered_success);
        mSendingSuccessText = (TextView) findViewById(R.id.delivered_success_txt);
        mSendingFailIcon = (ImageView) findViewById(R.id.delivered_failed);
        mSendingFailText = (TextView) findViewById(R.id.delivered_failed_txt);

        mListItemLayout = (ViewGroup) findViewById(R.id.tv_message_item);

    }

    public void bind(MessageData messageData, int position, int progress,
            boolean isDeleteMode) {
        if (null == messageData) {
            Log.e(TAG, "bind: messageContent is null, position=" + position);
            return;
        }

        Log.e(TAG, "bind: msgItem position=" + position + ". type="
                + messageData.getMessageContent().mediaType);

        mPosition = position;
        mTimeDividerStr.setText("");

        mSenderPhoto = (QuickContactBadge) findViewById(R.id.sender_photo);

        if (isDeleteMode) {
            mSelectedBox.setVisibility(View.VISIBLE);
            if (mIsSelect) {
                setSelectedBackground(true);
            } else {
                setSelectedBackground(false);
            }
        } else {
            mSelectedBox.setVisibility(View.GONE);
        }

        setLongClickable(false);
        setFocusable(false);
        setClickable(false);

        mSendingProgress = progress;
        mMessageListItem = MessageListItem.generateItem(messageData,
                mListItemLayout);
        mMessageListItem.bind(messageData);
        bindMessage();
        String time = Utils.formatTimeStampString(getContext(),
                messageData.getMessageContent().timestamp, false);
        mTimeDividerStr.setText(time);
        updateStatusView(messageData.getMessageContent().mediaType);
    }

    public void unbind() {
        Log.i(TAG, "unbind()");

        mMessageListItem.unbind();
    }

    public MessageData getMessageData() {
        return mMessageListItem.getMessageData();
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    public void setSelectedBackground(boolean selected) {
        if (selected) {
            mSelectedBox.setChecked(true);
            setBackgroundResource(R.drawable.list_selected_holo_light);
        } else {
            setBackground(null);
            mSelectedBox.setChecked(false);
        }
    }

    private void bindMessage() {
        mVisible = true;
        boolean isOutgoing = getMessageData().getMessageContent().direction ==
                Constants.MESSAGE_DIRECTION_OUTGOING;
        if (mVisible && !isOutgoing) {
            synchronized (PaComposeActivity.sLockObj) {
                if (PaComposeActivity.sLogoBitmap != null) {
                    mSenderPhoto.setImageBitmap(PaComposeActivity.sLogoBitmap);
                }
            }
        } else {
            synchronized (PaComposeActivity.sLockObj) {
                if (PaComposeActivity.sPortraitBitmap != null) {
                    mSenderPhoto
                            .setImageBitmap(PaComposeActivity.sPortraitBitmap);
                }
            }
        }
    }

    private void updateStatusView(int type) {
        if (getMessageData().getMessageContent().direction ==
                Constants.MESSAGE_DIRECTION_INCOMING) {
            return;
        }
        mSendStatusBar.setVisibility(View.VISIBLE);
        mSendingIcon.setVisibility(View.GONE);
        mSendingText.setVisibility(View.GONE);
        mProgressView.setVisibility(View.GONE);
        mSendingSuccessIcon.setVisibility(View.GONE);
        mSendingSuccessText.setVisibility(View.GONE);
        mSendingFailIcon.setVisibility(View.GONE);
        mSendingFailText.setVisibility(View.GONE);

        int status = getMessageData().getMessageContent().status;
        Log.d(TAG, "updateStatusView(). status=" + status);
        switch (status) {
        case Constants.MESSAGE_STATUS_TO_SEND:
            mDateView.setText(R.string.to_send_text);
            mSendingIcon.setVisibility(View.VISIBLE);
            break;

        case Constants.MESSAGE_STATUS_SENDING:
            mDateView.setText(R.string.sending_text);
            if (PaComposeActivity.isMultimediaMessage(getMessageData()
                    .getMessageContent().mediaType)) {
                mProgressView.setVisibility(View.VISIBLE);
                updateSendingProgress();
            } else {
                mSendingIcon.setVisibility(View.VISIBLE);
            }

            break;

        case Constants.MESSAGE_STATUS_SENT:
            // mSendingSuccessIcon.setVisibility(View.VISIBLE);
            mSendStatusBar.setVisibility(View.GONE);
            break;

        case Constants.MESSAGE_STATUS_FAILED:
            mDateView.setText(R.string.send_fail_text);
            mSendingFailIcon.setVisibility(View.VISIBLE);
            break;

        default:
            Log.d(TAG, "updateStatusView(). unrecoganized state");
            break;
        }

    }

    private void sendMessage(MessageData messageItem, int message) {
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, message);
            msg.obj = messageItem;
            msg.sendToTarget();
        }
    }

    static final int ITEM_CLICK = 5;

    public void onMessageListItemClick() {
        if (getMessageData().getMessageContent() == null) {
            Log.d(TAG, "onMessageListItemClick():getMessageContent is null !");
            return;
        }
        Log.d(TAG, "onMessageListItemClick onClick");

        if (mSelectedBox != null
                && mSelectedBox.getVisibility() == View.VISIBLE) {
            if (!mSelectedBox.isChecked()) {
                setSelectedBackground(true);
            } else {
                setSelectedBackground(false);
            }
            if (null != mHandler) {
                sendMessage(getMessageData(), ITEM_CLICK);
            }
            Log.d(TAG,
                    "Click on Multi delete screen:" + mSelectedBox.isChecked());
            return;
        }

        // re-send for sent fail message.
        if (Constants.MESSAGE_STATUS_FAILED == getMessageData()
                .getMessageContent().status
                && Constants.MESSAGE_DIRECTION_OUTGOING == getMessageData()
                        .getMessageContent().direction) {

            Log.d(TAG, "onMessageListItemClick on sent fail item, do resent");
            sendMessage(getMessageData(), PaComposeActivity.ACTION_RESEND);
            return;
        }

        mMessageListItem.onMessageListItemClick();
    }

    public void updateSendingProgress() {
        Log.d(TAG, "updateSendingProgress()");

        if (getMessageData().getMessageContent() == null) {
            Log.d(TAG, "updateSendingProgress() but msgItem is null");
            return;
        }

        if (getMessageData().getMessageContent().status != Constants.MESSAGE_STATUS_SENDING) {
            Log.d(TAG, "Invalid status for updateSendingProgress():"
                    + getMessageData().getMessageContent().status);
            return;
        }

        if (PaComposeActivity.isMultimediaMessage(getMessageData()
                .getMessageContent().mediaType)) {
            mProgressView.setText(mSendingProgress + "%");
        }
    }

    public void setBodyTextSize(float size) {
        mMessageListItem.setBodyTextSize(size);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
    }

}
