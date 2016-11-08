package com.mediatek.rcs.pam.ui.messageitem;

import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.ui.PaWebViewActivity;
import com.mediatek.rcs.pam.util.Utils;

public class SingleArticleItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX
            + "SingleArticleItem";

    private View mSingleMixView;
    private TextView mSingleMixTitle;
    private TextView mSingleMixCreateDate;
    private ImageView mSingleMixLogo;
    private TextView mSingleMixText;

    public SingleArticleItem(ViewGroup layout) {
        super(layout);

        mSingleMixView = (View) mLayout.findViewById(R.id.ip_single_mix);
        mSingleMixTitle = (TextView) mLayout
                .findViewById(R.id.single_mix_title);
        mSingleMixCreateDate = (TextView) mLayout
                .findViewById(R.id.single_create_date);
        mSingleMixLogo = (ImageView) mLayout.findViewById(R.id.single_mix_logo);
        mSingleMixText = (TextView) mLayout.findViewById(R.id.single_mix_text);

        if (mSingleMixView != null) {
            mSingleMixView.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(352,
                    288);
            mSingleMixLogo.setLayoutParams(para);
            mSingleMixLogo.setImageDrawable(new ColorDrawable(
                    android.R.color.transparent));
        } else {
            Log.i(TAG, "prepareForVisibility(). Error for no single mix view");
        }
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.getMessageContent().article == null
                || mMessageData.getMessageContent().article.size() == 0) {
            Log.e(TAG, "setSingleMixItem. mediaArticle is null !!!");
            return;
        }

        mSingleMixTitle
                .setText(mMessageData.getMessageContent().article.get(0).title);
        mSingleMixText
                .setText(mMessageData.getMessageContent().article.get(0).mainText);
        if (mMessageData.getMessageContent().timestamp > 0) {
            mSingleMixCreateDate
                    .setText(Utils.covertTimestampToString(mMessageData
                            .getMessageContent().timestamp));
        }

        String path = mMessageData.getMessageContent().article.get(0).thumbnailPath;
        Log.d(TAG, "Single mix item thumbnail path:" + path + ".");
        if (Utils.isExistsFile(path)) {
            drawThumbnail(mSingleMixLogo, null, 0, path, DRAW_TYPE_NORMAL);
        } else {
            String url = mMessageData.getMessageContent().article.get(0).thumbnailUrl;
            sendDownloadReq(url, Constants.THUMBNAIL_TYPE, 0);
            Log.d(TAG, "setSingleMixItem() download req for:");
        }
    }

    @Override
    public void unbind() {
        mSingleMixTitle.setText("");
        mSingleMixCreateDate.setText("");
        mSingleMixText.setText("");
        mSingleMixLogo.setImageBitmap(null);
        if (mSingleMixView != null) {
            mSingleMixView.setVisibility(View.GONE);
        }

        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {
        mMessageData.getMessageContent().article.get(0).thumbnailPath = path;
        drawThumbnail(mSingleMixLogo, null, 0, path, DRAW_TYPE_NORMAL);
    }

    @Override
    public void onMessageListItemClick() {
        if (mMessageData == null) {
            Log.d(TAG, "onMessageListItemClick():Message item is null !");
            return;
        }

        if (mMessageData.getMessageContent().article == null) {
            Toast.makeText(mLayout.getContext(), "mediaArtical is null",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String url = mMessageData.getMessageContent().article.get(0).bodyUrl;
        PaWebViewActivity
                .openHyperLink(
                        mLayout.getContext(),
                        url,
                        (mMessageData.getMessageContent().forwardable ==
                            Constants.MESSAGE_FORWARDABLE_YES ? true : false));
    }
}
