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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;

/**
 * The back-end data adapter of a message list.
 */
public class PaMessageListAdapter extends CursorAdapter {
    private static final String TAG = "PA/PaMessageListAdapter";

    private static final int CACHE_SIZE = 50;

    protected LayoutInflater mInflater;
    private final MessageDataCache mMessageDataCache;
    private OnDataSetChangedListener mOnDataSetChangedListener;
    private Handler mMsgListItemHandler;
    private Pattern mHighlight;
    private Context mContext;

    public boolean mIsDeleteMode = false;
    private Map<Long, Boolean> mListItem;

    private float mTextSize = 0;

    ConcurrentHashMap<Long, Integer> mSendingQueue = new ConcurrentHashMap<Long, Integer>();

    public ConcurrentHashMap<Long, Integer> getSendingQueue() {
        return mSendingQueue;
    }

    public PaMessageListAdapter(Context context, Cursor c, ListView listView,
            boolean useDefaultColumnsMap, Pattern highlight) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);

        mContext = context;
        mHighlight = highlight;

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMessageDataCache = new MessageDataCache(CACHE_SIZE);

        mListItem = new LinkedHashMap<Long, Boolean>();

        listView.setRecyclerListener(new AbsListView.RecyclerListener() {

            @Override
            public void onMovedToScrapHeap(View view) {
                if (view instanceof PaMessageListItem) {
                    PaMessageListItem pmli = (PaMessageListItem) view;
                    pmli.unbind();
                }
            }
        });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log.d(TAG, "bindView() start. pos=" + cursor.getPosition());
        if (view instanceof PaMessageListItem) {
            long msgId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageColumns.ID));
            MessageData msgItem = getCachedMessageData(msgId, cursor);
            Log.d(TAG, "bindView getItem done. msgId=" + msgId);
            if (msgItem != null) {
                PaMessageListItem pmli = (PaMessageListItem) view;
                if (mIsDeleteMode) {
                    if (mListItem.get(msgId) == null) {
                        mListItem.put(msgId, false);
                    } else {
                        pmli.setSelectedState(mListItem.get(msgId));
                    }
                }
                int position = cursor.getPosition();
                Integer tempProgress = mSendingQueue.get(msgId);
                int progress = tempProgress == null ? 0 : tempProgress
                        .intValue();
                // must set callback before bind ,due to it user in bind process
                pmli.setMsgListItemHandler(mMsgListItemHandler);
                pmli.bind(msgItem, position, progress, mIsDeleteMode);
            } else {
                Log.e(TAG, "bindView getItem but result is NULL !!!");
                view.setVisibility(View.GONE);
            }
            Log.d(TAG, "bindView getItem end.");
        }
        if (mTextSize != 0) {
            PaMessageListItem mli = (PaMessageListItem) view;
            mli.setBodyTextSize(mTextSize);
        }
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged(PaMessageListAdapter adapter);

        void onContentChanged(PaMessageListAdapter adapter);
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
        mOnDataSetChangedListener = l;
    }

    public void setMsgListItemHandler(Handler handler) {
        mMsgListItemHandler = handler;
    }

    public void cancelBackgroundLoading() {
        mMessageDataCache.evictAll();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        long time = System.currentTimeMillis();
        Log.v(TAG, "MessageListAdapter.notifyDataSetChanged()." + time);

        mMessageDataCache.evictAll();
        // mDownloadMap.clear();

        if (mOnDataSetChangedListener != null) {
            mOnDataSetChangedListener.onDataSetChanged(this);
        }
    }

    @Override
    protected void onContentChanged() {
        Log.i(TAG, "onContentChanged()");
        if (getCursor() != null && !getCursor().isClosed()) {
            if (mOnDataSetChangedListener != null) {
                mOnDataSetChangedListener.onContentChanged(this);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.d(TAG, "newView pos=" + cursor.getPosition());
        int type = getItemViewType(cursor);
        View view;
        switch (type) {
        case Constants.MESSAGE_DIRECTION_INCOMING:
            view = mInflater.inflate(R.layout.message_list_item_recv_ipmsg,
                    parent, false);
            break;
        case Constants.MESSAGE_DIRECTION_OUTGOING:
        default:
            view = mInflater.inflate(R.layout.message_list_item_send_ipmsg,
                    parent, false);
            break;
        }
        return view; // new PaMessageListItem(context, view);
    }

    public MessageData getCachedMessageData(long msgId, Cursor c) {
        MessageData item = mMessageDataCache.get(msgId);
        if (item == null && c != null && isCursorValid(c)) {
            Log.d(TAG, "getCachedMessageContent() but create new: " + msgId);
            item = new MessageData(MessageContent.buildFromPAProviderCursor(
                    mContext.getContentResolver(), c), mContext);
            mMessageDataCache.put(item.getMessageContent().id, item);
        }
        return item;
    }

    private boolean isCursorValid(Cursor cursor) {
        // Check whether the cursor is valid or not.
        if (cursor == null || cursor.isClosed() || cursor.isBeforeFirst()
                || cursor.isAfterLast()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    public void changeSelectedState(long listId) {
        Log.d(TAG, "listId = " + listId);
        if (mListItem == null) {
            Log.e(TAG, "mListItem is null");
            return;
        }
        mListItem.put(listId, !mListItem.get(listId));
    }

    public Map<Long, Boolean> getItemList() {
        return mListItem;
    }

    public void initListMap(Cursor cursor) {
        if (cursor != null) {
            long msgId = 0;
            while (cursor.moveToNext()) {
                msgId = cursor.getLong(cursor
                        .getColumnIndexOrThrow(MessageColumns.ID));
                if (mListItem.get(msgId) == null) {
                    mListItem.put(msgId, false);
                }
            }
        }
    }

    public void setItemsValue(boolean value, long[] keyArray) {
        Iterator<Entry<Long, Boolean>> iter = mListItem.entrySet().iterator();
        if (keyArray == null) {
            while (iter.hasNext()) {
                Map.Entry<Long, Boolean> entry = iter.next();
                entry.setValue(value);
            }
        } else {
            for (int i = 0; i < keyArray.length; i++) {
                mListItem.put(keyArray[i], value);
            }
        }
    }

    public void clearList() {
        if (mListItem != null) {
            mListItem.clear();
        }
    }

    public int getAllNumber() {
        return mListItem.size();
    }

    public int getSelectedNumber() {
        int number = 0;
        if (mListItem != null) {
            Iterator<Entry<Long, Boolean>> iter = mListItem.entrySet()
                    .iterator();
            while (iter.hasNext()) {
                @SuppressWarnings("unchecked")
                Map.Entry<Long, Boolean> entry = iter.next();
                if (entry.getValue()) {
                    number++;
                }
            }
        }
        Log.d(TAG, "getSelectedNumber=" + number);
        return number;
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }

    /*
     * MessageListAdapter says that it contains four types of views. Really, it
     * just contains a single type, a MessageListItem. Depending upon whether
     * the message is an incoming or outgoing message, the avatar and text and
     * other items are laid out either left or right justified. That works fine
     * for everything but the message text. When views are recycled, there's a
     * greater than zero chance that the right-justified text on outgoing
     * messages will remain left-justified. The best solution at this point is
     * to tell the adapter we've got two different types of views. That way we
     * won't recycle views between the two types.
     *
     * @see android.widget.BaseAdapter#getViewTypeCount()
     */
    @Override
    public int getViewTypeCount() {
        return 2; // Incoming and outgoing messages
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    private int getItemViewType(Cursor cursor) {
        int type = cursor.getInt(cursor
                .getColumnIndexOrThrow(MessageColumns.DIRECTION));
        long time = System.currentTimeMillis();
        Log.d(TAG, "getItemViewType=" + type + "time" + time);
        return type;
    }

    private static class MessageDataCache extends LruCache<Long, MessageData> {
        public MessageDataCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Long key,
                MessageData oldValue, MessageData newValue) {
            // oldValue.cancelPduLoading();
        }
    }
}
