package com.mediatek.rcs.pam.ui.messageitem;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.ui.PaWebViewActivity;
import com.mediatek.rcs.pam.util.Utils;

public class MultipleArticleItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX
            + "MultipleArticleItem";

    private LinearLayout mMultiMixView;
    private RelativeLayout mMultiMixHeaderView;
    private ImageView mMultiMixHeaderThumb;
    private TextView mMultiMixHeaderText;
    private LinearLayout mMultiAricleLayout[] = new LinearLayout[4];
    private LinearLayout mMutliMixBodyView[] = new LinearLayout[4];
    private ImageView mMultiMixBodyThumb[] = new ImageView[4];
    private TextView mMultiMixBodyText[] = new TextView[4];

    public MultipleArticleItem(ViewGroup layout) {
        super(layout);

        mMultiMixView = (LinearLayout) mLayout.findViewById(R.id.ip_multi_mix);
        mMultiMixHeaderView = (RelativeLayout) mLayout
                .findViewById(R.id.multi_mix_header);
        mMultiMixHeaderThumb = (ImageView) mLayout
                .findViewById(R.id.multi_mix_header_thumb);
        mMultiMixHeaderText = (TextView) mLayout
                .findViewById(R.id.multi_mix_header_title);

        if (mMultiMixView != null) {
            mMultiMixView.setVisibility(View.VISIBLE);
        } else {
            Log.i(TAG, "prepareForVisibility(). Error for no mulit mix view");
        }
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.getMessageContent().direction != Constants.MESSAGE_DIRECTION_INCOMING) {
            Log.e(TAG, "setMultiMixItem() but not incoming message: "
                    + mMessageData.getMessageContent().direction);
            return;
        }

        int size = mMessageData.getMessageContent().article.size();
        Log.d(TAG, "setMultiMixItem size=" + size);
        MediaArticle ma;
        String path;
        String title;
        for (int i = 0; i < size; i++) {
            ma = mMessageData.getMessageContent().article.get(i);
            path = ma.thumbnailPath; // mMessageItem.getThumbnailPath(i);
            title = ma.title;
            if (0 == i) {
                mMultiMixHeaderText.setText(title);
                if (Utils.isExistsFile(path)) {
                    drawThumbnail(mMultiMixHeaderThumb, null, i, path,
                            DRAW_TYPE_NORMAL);
                } else {
                    sendDownloadReq(ma.thumbnailUrl, Constants.THUMBNAIL_TYPE,
                            i);

                }
                setMixListeners(mMultiMixHeaderView, ma.bodyUrl, i);
            } else {
                int pos = i - 1;
                mMultiAricleLayout[pos] = (LinearLayout) LayoutInflater.from(
                        mLayout.getContext()).inflate(
                        R.layout.multi_article_item, mMultiMixView, false);
                Log.d(TAG, "mMultiAricleLayout[" + pos + "]="
                        + mMultiAricleLayout[pos]);
                mMutliMixBodyView[pos] = (LinearLayout) mMultiAricleLayout[pos]
                        .findViewById(R.id.multi_mix_body);
                mMultiMixBodyText[pos] = (TextView) mMultiAricleLayout[pos]
                        .findViewById(R.id.multi_mix_body_title);
                mMultiMixBodyText[pos].setText(title);
                mMultiMixBodyThumb[pos] = (ImageView) mMultiAricleLayout[pos]
                        .findViewById(R.id.multi_mix_body_thumb);
                mMultiMixBodyThumb[pos].setVisibility(View.VISIBLE);
                if (Utils.isExistsFile(path)) {
                    drawThumbnail(mMultiMixBodyThumb[pos], null, i, path,
                            DRAW_TYPE_SMALL);
                } else {
                    sendDownloadReq(ma.thumbnailUrl, Constants.THUMBNAIL_TYPE,
                            i);
                }
                setMixListeners(mMultiAricleLayout[pos], ma.bodyUrl, i);
                mMultiMixView.addView(mMultiAricleLayout[pos], i);
            }
        }
    }

    @Override
    public void unbind() {
        mMultiMixHeaderThumb.setImageBitmap(null);
        mMultiMixHeaderText.setText("");
        for (int i = 0; i < 4; i++) {
            if (mMultiAricleLayout[i] != null) {
                mMultiMixView.removeView(mMultiAricleLayout[i]);
                mMultiAricleLayout[i] = null;
                mMutliMixBodyView[i] = null;
                mMultiMixBodyThumb[i] = null;
                mMultiMixBodyText[i] = null;
            }
        }
        if (mMultiMixView != null) {
            mMultiMixView.setVisibility(View.GONE);
        }

        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {
        mMessageData.getMessageContent().article.get(index).thumbnailPath = path;
        if (index == 0) {
            drawThumbnail(mMultiMixHeaderThumb, null, 0, path, DRAW_TYPE_NORMAL);
        } else {
            drawThumbnail(mMultiMixBodyThumb[index - 1], null, index, path,
                    DRAW_TYPE_SMALL);
        }
    }

    @Override
    public void onMessageListItemClick() {

    }

    private void setMixListeners(View view, final String url, final int index) {

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setMultiMixItem onClick");
                PaWebViewActivity.openHyperLink(
                        mLayout.getContext(),
                        url,
                        (mMessageData.getMessageContent().forwardable ==
                            Constants.MESSAGE_FORWARDABLE_YES ? true : false));
            }
        });

        view.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMessageData.mContextMenuIndex = index;
                v.showContextMenu();
                return true;
            }
        });
    }
}
