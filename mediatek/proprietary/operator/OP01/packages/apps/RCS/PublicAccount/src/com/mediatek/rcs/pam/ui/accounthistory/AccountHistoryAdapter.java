package com.mediatek.rcs.pam.ui.accounthistory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;

import java.util.ArrayList;
import java.util.List;

public class AccountHistoryAdapter extends BaseAdapter {

    private static final String TAG = Constants.TAG_PREFIX
            + "AccountHistoryAdapter";

    private Context mContext;
    private List<MessageData> mMessageList;

    public AccountHistoryAdapter(Context context,
            List<MessageContent> messageList) {
        mContext = context;
        setMessageList(messageList);

    }

    @Override
    public int getCount() {
        return mMessageList.size();
    }

    @Override
    public MessageData getItem(int position) {
        return mMessageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView " + position + ", type "
                + getItem(position).getMessageContent().mediaType + ", count "
                + getCount());

        AccountHistoryMsgItemView messageView = (AccountHistoryMsgItemView) convertView;
        if (messageView == null) {
            messageView = (AccountHistoryMsgItemView) LayoutInflater.from(
                    mContext).inflate(R.layout.account_history_item, parent,
                    false);
        }

        messageView.bind(getItem(position), position);

        return messageView;

    }

    public void setMessageList(List<MessageContent> messageList) {
        mMessageList = new ArrayList<MessageData>();
        int index = 0;
        for (MessageContent messageContent : messageList) {
            messageContent.id = index++;
            mMessageList.add(new MessageData(messageContent, mContext));
        }
        notifyDataSetChanged();
    }

    public List<MessageContent> getMessageList() {
        List<MessageContent> result = new ArrayList<MessageContent>();
        for (MessageData messageContent : mMessageList) {
            result.add(messageContent.getMessageContent());
        }
        return result;
    }
}
