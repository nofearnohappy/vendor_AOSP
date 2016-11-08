package com.mediatek.rcs.pam.ui.messageitem;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.ui.PaWebViewActivity;
import com.mediatek.rcs.pam.util.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.provider.Browser;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TextItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX + "TextItem";

    private static final String TEL_PREFIX = "tel:";
    private static final String SMS_PREFIX = "smsto:";
    private static final String MAIL_PREFIX = "mailto";

    private TextView mBodyTextView;

    public TextItem(ViewGroup layout) {
        super(layout);

        mBodyTextView = (TextView) mLayout.findViewById(R.id.text_view);
        mBodyTextView.setVisibility(View.VISIBLE);

    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        SpannableStringBuilder buf = new SpannableStringBuilder();
        buf.append(mMessageData.getMessageContent().text);
        buf = new SpannableStringBuilder(Utils.formatTextMessage(
                mMessageData.getMessageContent().text, true, buf));

        mBodyTextView.setText(buf);
    }

    @Override
    public void unbind() {
        mBodyTextView.setText("");
        mBodyTextView.setVisibility(View.GONE);
        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {

    }

    @Override
    public void onMessageListItemClick() {
        if (mMessageData == null) {
            Log.d(TAG, "onMessageListItemClick():Message item is null !");
            return;
        }
        Log.d(TAG, "onMessageListItemClick onClick");

        URLSpan[] spans = mBodyTextView.getUrls();
        List<String> urls = new ArrayList<String>();
        for (URLSpan span : spans) {
            if (!urls.contains(span.getURL())) {
                urls.add(span.getURL());
            }
        }
        handleUrls(urls);
    }

    @Override
    public void setBodyTextSize(float size) {
        if (mBodyTextView != null
                && mBodyTextView.getVisibility() == View.VISIBLE) {
            mBodyTextView.setTextSize(size);
        }
    }

    private Drawable parseAppIcon(Context context, String url) {
        int drawableId;

        if (url.startsWith(TEL_PREFIX)) {
            drawableId = R.drawable.common_phone;
        } else if (url.startsWith(SMS_PREFIX)) {
            drawableId = R.drawable.common_message;
        } else if (url.startsWith(MAIL_PREFIX)) {
            drawableId = R.drawable.common_email;
        } else {
            drawableId = R.drawable.common_browser;
        }
        return context.getResources().getDrawable(drawableId);
    }

    private void openActivity(String url) {
        if (url.startsWith(TEL_PREFIX) || url.startsWith(SMS_PREFIX)
                || url.startsWith(MAIL_PREFIX)) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, mLayout.getContext()
                    .getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            mLayout.getContext().startActivity(intent);
        } else {
            PaWebViewActivity
                    .openHyperLink(
                            mLayout.getContext(),
                            url,
                            (mMessageData.getMessageContent().forwardable ==
                                Constants.MESSAGE_FORWARDABLE_YES ? true : false));
        }
    }

    private void handleUrls(final List<String> urls) {
        boolean isTel = false;
        for (String url : urls) {
            if (url.startsWith(TEL_PREFIX)) {
                isTel = true;
                urls.add("smsto:" + url.substring(TEL_PREFIX.length()));
            }
        }
        if (urls.size() == 0) {
            // do nothing
        } else if (urls.size() == 1 && !isTel) {
            openActivity(urls.get(0));
        } else {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    mLayout.getContext(), android.R.layout.select_dialog_item,
                    urls) {

                @Override
                public View getView(int position, View convertView,
                        ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v;
                    String url = getItem(position).toString();
                    Uri uri = Uri.parse(url);

                    Drawable d = parseAppIcon(getContext(), url);
                    if (d != null) {
                        d.setBounds(0, 0, d.getIntrinsicHeight(),
                                d.getIntrinsicHeight());
                        tv.setCompoundDrawablePadding(10);
                        tv.setCompoundDrawables(d, null, null, null);
                    } else {

                        tv.setCompoundDrawables(null, null, null, null);
                    }

                    if (url.startsWith(TEL_PREFIX)) {
                        url = PhoneNumberUtils.formatNumber(url
                                .substring(TEL_PREFIX.length()));
                        if (url == null) {
                            Log.w(TAG,
                                    "url turn to null after calling PhoneNumberUtils.formatNumber");
                            url = getItem(position).toString().substring(
                                    TEL_PREFIX.length());
                        }
                    } else if (url.startsWith(SMS_PREFIX)) {
                        url = PhoneNumberUtils.formatNumber(url
                                .substring(SMS_PREFIX.length()));
                        if (url == null) {
                            Log.w(TAG,
                                    "url turn to null after calling PhoneNumberUtils.formatNumber");
                            url = getItem(position).toString().substring(
                                    SMS_PREFIX.length());
                        }
                    } else if (url.startsWith(MAIL_PREFIX)) {
                        String uu = url.substring(MAIL_PREFIX.length() + 1,
                                url.length());
                        uu = Uri.encode(uu);
                        uu = MAIL_PREFIX + ":" + uu;
                        MailTo mt = MailTo.parse(uu);
                        url = mt.getTo();
                    }
                    tv.setText(url);
                    return v;
                }
            };

            AlertDialog.Builder b = new AlertDialog.Builder(
                    mLayout.getContext());

            DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialog, int which) {
                    if (which >= 0) {
                        openActivity(urls.get(which));
                    }
                    dialog.dismiss();
                }
            };

            b.setTitle(R.string.select_link_title);
            b.setCancelable(true);
            b.setAdapter(adapter, click);

            b.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialog,
                                int which) {
                            dialog.dismiss();
                        }
                    });

            b.show();
        }
    }
}
