package com.mediatek.rcs.pam.ui.messagehistory;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.util.Utils;

public class MessageHistoryItemView extends LinearLayout {
    private MessageHistoryItem mDataItem;
    private ImageView mLogo;
    private TextView mAccountName;
    private TextView mLatestMessage;
    private TextView mTimestamp;

    public MessageHistoryItemView(Context context) {
        super(context);
    }

    public MessageHistoryItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLogo = (ImageView) findViewById(R.id.logo);
        mAccountName = (TextView) findViewById(R.id.account_name);
        mLatestMessage = (TextView) findViewById(R.id.message_content);
        mTimestamp = (TextView) findViewById(R.id.timestamp);
    }

    public void bind(MessageHistoryItem data, int index) {
        mDataItem = data;
        data.view = this;
        if (data.logoBitmap != null) {
            mLogo.setImageBitmap(mDataItem.logoBitmap);
        } else {
            // TODO use placeholder image (H Zhang has one)
            mLogo.setImageResource(android.R.drawable.gallery_thumb);
        }
        mAccountName.setText(mDataItem.accountName);
        mLatestMessage.setText(mDataItem.lastMessageSummary);
        mTimestamp.setText(Utils.formatTimeStampString(getContext(),
                mDataItem.lastMessageTimestamp, false));
    }

    public void setLogo(Bitmap logo) {
        mLogo.setImageBitmap(logo);
    }

    public MessageHistoryItem getDataItem() {
        return mDataItem;
    }
}
