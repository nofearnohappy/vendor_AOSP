package com.mediatek.rcs.pam.ui.accounthistory;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;
import com.mediatek.rcs.pam.ui.messageitem.MessageListItem;
import com.mediatek.rcs.pam.util.Utils;


public class AccountHistoryMsgItemView extends LinearLayout {

    private static String TAG = Constants.TAG_PREFIX
            + "AccountHistoryMsgItemView";

    private int mPosition;

    private TextView mTimeView;
    private ViewGroup mListTiemLayout;
    private MessageListItem mMessageListItem;

    public AccountHistoryMsgItemView(Context context) {
        super(context);
    }

    public AccountHistoryMsgItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "AccountHistoryMsgItemView(" + context + ", " + attrs + ")");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTimeView = (TextView) findViewById(R.id.tv_text_time);
        mListTiemLayout = (ViewGroup) findViewById(R.id.tv_message_item);
    }

    public void bind(MessageData message, int position) {
        Log.i(TAG, "bind " + position);

        long timeStamp = message.getMessageContent().timestamp;
        String time = Utils.formatTimeStampString(getContext(), timeStamp,
                false);
        mTimeView.setText(time);
        mPosition = position;
        mMessageListItem = MessageListItem.generateItem(message,
                mListTiemLayout);
        mMessageListItem.bind(message);
    }

    public void unbind() {
        Log.i(TAG, "unbind " + mPosition);

        mMessageListItem.unbind();
    }

    public int getPosition() {
        return mPosition;
    }

    public void onMessageListItemClick() {
        mMessageListItem.onMessageListItemClick();
    }
}
