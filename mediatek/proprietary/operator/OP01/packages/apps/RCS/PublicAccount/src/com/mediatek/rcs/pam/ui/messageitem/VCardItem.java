package com.mediatek.rcs.pam.ui.messageitem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.util.PaVcardUtils;

public class VCardItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX + "VCardItem";

    private View mVcardView;
    private TextView mVCardInfo;
    private TextView mVCardNumber;
    private ImageView mVCardPortraint;

    public VCardItem(ViewGroup layout) {
        super(layout);

        mVcardView = (View) mLayout.findViewById(R.id.ip_vcard);
        mVCardInfo = (TextView) mLayout.findViewById(R.id.vcard_info);
        mVCardNumber = (TextView) mLayout.findViewById(R.id.vcard_number);
        mVCardPortraint = (ImageView) mLayout.findViewById(R.id.ip_vcard_icon);

        mVcardView.setVisibility(View.VISIBLE);
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.mVcardCount == 1) {
            if (mMessageData.mVcardBitmap != null) {
                mVCardPortraint.setImageBitmap(mMessageData.mVcardBitmap);
            } else {
                mVCardPortraint
                        .setImageResource(R.drawable.ipmsg_chat_contact_vcard);
            }
            if (mMessageData.mVcardName != null
                    && !mMessageData.mVcardName.isEmpty()) {
                mVCardInfo.setText(mMessageData.mVcardName);
            }
            if (mMessageData.mVcardNumber != null
                    && !mMessageData.mVcardNumber.isEmpty()) {
                mVCardNumber.setVisibility(View.VISIBLE);
                mVCardNumber.setText(mMessageData.mVcardNumber);
            } else {
                mVCardNumber.setVisibility(View.GONE);
            }
        } else {
            mVCardPortraint
                    .setImageResource(R.drawable.ipmsg_chat_contact_vcard);
            mVCardInfo.setText(R.string.multi_contacts_name);
            mVCardNumber.setVisibility(View.GONE);
        }
    }

    @Override
    public void unbind() {
        mVCardPortraint.setImageBitmap(null);
        mVCardInfo.setText("");
        mVCardNumber.setText("");
        mVcardView.setVisibility(View.GONE);
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

        String mediaPath = mMessageData.getMessageContent().mediaPath;
        if (TextUtils.isEmpty(mediaPath)) {
            Toast.makeText(mLayout.getContext(), "mediaPath is null",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int entryCount = PaVcardUtils.getVcardEntryCount(mediaPath);
        Log.d(TAG, "vCard entryCount=" + entryCount);
        if (entryCount <= 0) {
            Toast.makeText(mLayout.getContext(), "parse vCard error",
                    Toast.LENGTH_LONG).show();
        } else if (entryCount == 1) {
            Uri uri = Uri.parse("file://" + mediaPath);
            Intent vcardIntent = new Intent(
                    "android.intent.action.rcs.contacts.VCardViewActivity");
            vcardIntent.setDataAndType(uri, "text/x-vcard".toLowerCase());
            vcardIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mLayout.getContext().startActivity(vcardIntent);
        } else {
            Resources res = mLayout.getContext().getResources();
            AlertDialog.Builder b = new AlertDialog.Builder(
                    mLayout.getContext());
            b.setTitle(R.string.multi_contacts_name)
                    .setMessage(
                            res.getString(R.string.multi_contacts_notification))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    importVcard();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
        }
    }

    private void importVcard() {
        Uri uri = Uri.parse("file://"
                + mMessageData.getMessageContent().basicMedia.originalPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/x-vcard");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mLayout.getContext().startActivity(intent);
    }
}
