package com.mediatek.mms.plugin;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.mms.callback.IFileAttachmentModelCallback;
import com.mediatek.mms.ext.DefaultOpFileAttachmentUtilsExt;
import com.mediatek.mms.ext.IOpFileAttachmentModelExt;
import com.mediatek.mms.callback.IMessageUtilsCallback;

public class Op09FileAttachmentUtilsExt extends DefaultOpFileAttachmentUtilsExt {

    @Override
    public boolean createFileAttachmentView(Context context, ArrayList attachFiles,
            IFileAttachmentModelCallback callback, TextView tvText, ImageView ivThumb,
            TextView tvSize, String strSize, TextView name2, ImageView thumb2,
            Drawable ipmsgVcard, Drawable ipmsgCalendar,
            IOpFileAttachmentModelExt opFileAttachmentModel, String strSize2) {
        boolean isCtFeature = MessageUtils.isSupportVCardPreview();
        Op09FileAttachmentModelExt op09FileAttachExt =
                                (Op09FileAttachmentModelExt)opFileAttachmentModel;
        IMessageUtilsCallback utilsCallback =  Op09MessagePluginExt.sCallback;
        //Op09VCardModelExt op09vCardModelExt = null;
        if (isCtFeature) {
            ivThumb.setVisibility(View.GONE);
            ivThumb = thumb2;
            ivThumb.setVisibility(View.VISIBLE);
        }
        String nameText = null;
        Drawable thumbRes = null;

        if (op09FileAttachExt.isVCard()) {
            if (isCtFeature) {

               // op09vCardModelExt = (Op09VCardModelExt) opVCardModel;
                nameText = op09FileAttachExt.getDisplayName();
                if (TextUtils.isEmpty(nameText)) {
                    nameText = utilsCallback.getString(IMessageUtilsCallback.attachment_vcard_name);
                }
                thumbRes = ipmsgVcard;

                if (isCtFeature && name2 != null) {
                    if (op09FileAttachExt.getContactCount() > 1) {
                        name2.setText(" +" + (op09FileAttachExt.getContactCount() - 1));
                        name2.setVisibility(View.VISIBLE);
                    }
                }
            }
        } else if (op09FileAttachExt.isVCalendar()) {
            nameText = utilsCallback.getString(IMessageUtilsCallback.attachment_vcalendar_name);;
            if (isCtFeature) {
                thumbRes = ipmsgCalendar;
            }
        }
        tvText.setText(nameText);
        ivThumb.setImageDrawable(thumbRes);
        tvSize.setText(strSize2);

        if ((!isCtFeature || !op09FileAttachExt.isVCard()
                || (op09FileAttachExt != null && op09FileAttachExt.getContactCount() <= 1))
                && name2 != null) {
            name2.setText("");
            name2.setVisibility(View.GONE);
        }

        return isCtFeature;

    }
}
