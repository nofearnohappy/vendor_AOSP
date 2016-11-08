package com.mediatek.mms.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduPart;

import com.mediatek.mms.callback.IFileAttachmentModelCallback;
import com.mediatek.mms.callback.IMediaModelCallback;
import com.mediatek.mms.callback.IMessageUtilsCallback;
import com.mediatek.mms.callback.ISlideModelCallback;
import com.mediatek.mms.ext.DefaultOpFileAttachmentUtilsExt;
import com.mediatek.mms.ext.IOpFileAttachmentModelExt;
import com.mediatek.op01.plugin.R;

import java.util.ArrayList;

/**
 * Op01FileAttachmentUtilsExt.
 *
 */
public class Op01FileAttachmentUtilsExt extends DefaultOpFileAttachmentUtilsExt {

    private static final String TAG = "Op01FileAttachmentUtilsExt";
    private Context mPluginContext;

    /**
     * Construction.
     * @param context Context
     */
    public Op01FileAttachmentUtilsExt(Context context) {
        mPluginContext = context;
    }

    @Override
    public boolean setOrAppendFileAttachment(boolean append) {
        return append;
    }

    @Override
    public boolean createFileAttachmentView(Context context, ArrayList attachFiles,
            IFileAttachmentModelCallback callback, TextView tvText, ImageView ivThumb,
            TextView tvSize, String strSize, TextView name2, ImageView thumb2, Drawable ipmsgVcard,
            Drawable ipmsgCalendar, IOpFileAttachmentModelExt opVCardModel, String strSize2) {
        String nameText = "";
        int thumbResId = 0;
        Drawable dbThumb = null;

        if (attachFiles.size() > 1) {
            // multi attachments files
            Log.i(TAG, "createFileAttachmentView, attachFiles.size() > 1");
            nameText = Op01MessagePluginExt.sMessageUtilsCallback
                    .getString(IMessageUtilsCallback.file_attachment_common_name,
                                                        attachFiles.size());
            dbThumb = mPluginContext.getResources().getDrawable(R.drawable.multi_files);
        } else if (attachFiles.size() == 1) {
            // single attachment(file)
            if (callback.isVCardCallback()) {
                // vCard
                nameText = Op01MessagePluginExt.sMessageUtilsCallback
                        .getString(IMessageUtilsCallback.attachment_vcard_name,
                                callback.getSrcCallback());
                dbThumb = Op01MessagePluginExt.sMessageUtilsCallback
                        .getDrawable(IMessageUtilsCallback.ic_vcard_attach);
            } else if (callback.isVCalendarCallback()) {
                // VCalendar
                nameText = Op01MessagePluginExt.sMessageUtilsCallback
                .getString(IMessageUtilsCallback.attachment_vcalendar_name,
                        callback.getSrcCallback());
                dbThumb = Op01MessagePluginExt.sMessageUtilsCallback
                        .getDrawable(IMessageUtilsCallback.ic_vcalendar_attach);
            } else {
                // other attachment
                nameText = callback.getSrcCallback();
                dbThumb = Op01MessagePluginExt.sMessageUtilsCallback
                        .getDrawable(IMessageUtilsCallback.unsupported_file);
            }
        }

        ivThumb.setImageDrawable(dbThumb);
        tvText.setText(nameText);
        tvSize.setText(strSize);

        return true;
    }

    @Override
    public void getAttachFiles(Context context, PduPart part, String fileName,
            ArrayList slides, ArrayList attachFiles,
            IFileAttachmentModelCallback callback) {
        String partUri = null;
        if (part.getDataUri() != null) {
                Log.d(TAG, "part Uri = " + part.getDataUri().toString());
                partUri = part.getDataUri().toString();
        }

        Log.d(TAG, "partUri = " + partUri + "fileName = " + fileName);
        if ((ContentType.isImageType(new String(part.getContentType()))
                || ContentType.isVideoType(new String(part.getContentType()))
                || ContentType.isAudioType(new String(part.getContentType()))
                || Op01MmsUtils.isOtherAttachment(part))
                && !isInSmil(fileName, slides, partUri)) {
            callback.addFileAttachmentModelCallback(attachFiles,
                    callback.createFileModelByUriCallback(context, part, fileName));
            Log.d(TAG, "In add file attachment: add " + new String(part.getContentType()));
        } else {
            byte[] ci = part.getContentId();
            String cid = null;
            if (ci != null) {
                cid = new String(ci);
            }

            if ((isTextType(part) || isHtmlType(part)) && !isInSmil(fileName, cid, slides)) {
                callback.addFileAttachmentModelCallback(attachFiles,
                        callback.createFileModelByDataCallback(context, part, fileName));
                Log.d(TAG, "In add text or html attachment: add "
                        + new String(part.getContentType()));
            }
        }
    }

    /**
     * isTextType.
     * @param part Pdupart
     * @return true if it's text type
     */
    public static boolean isTextType(final PduPart part) {
        final String type = new String(part.getContentType());
        if (type.equals("text/plain")) {
            Log.d(TAG, "is TEXT type");
            return true;
        } else {
            return false;
        }
    }

    /**
     * isHtmlType.
     * @param part Pdupart
     * @return true if it's htmp tyle
     */
    public static boolean isHtmlType(final PduPart part) {
        final String type = new String(part.getContentType());
        if (type.equals("text/html")) {
            Log.d(TAG, "is HTML type");
            return true;
        } else {
            return false;
        }
    }

    /*
     * add for attachment enhance
     * This function is for justify if this attachment is in slides
     */
    private static boolean isInSmil(String filename,
            ArrayList<ISlideModelCallback> slides, String partUri) {
        for (ISlideModelCallback slide : slides) {
            for (int i = 0; i < slide.getMediaModelSize(); i++) {
                if (slide.getMediaModel(i).getUriCallback() != null && partUri != null) {
                    Log.d(TAG, "isInSmil() slide model Uri = "
                            + slide.getMediaModel(i).getUriCallback().toString());
                    if (slide.getMediaModel(i).getUriCallback()
                                            .toString().compareTo(partUri) == 0) {
                        Log.d(TAG, "isInSmil() This media is in smil (uri)");
                        return true;
                    }
                }

                // /M: ALPS00654796 Modify for consistence test case
                if (filename != null && slide.getMediaModel(i).getSrcCallback() != null
                        && filename.compareTo(slide.getMediaModel(i).getSrcCallback()) == 0) {
                    Log.d(TAG, "isInSmil() This media is in smil (filename)");
                    return true;
                }
            }
        }

        Log.d(TAG, "isInSmil This media is NOT in smil");
        return false;
    }

    /*
     * Justify if the text or html is in slide or not
     */
    private static boolean isInSmil(String filename, String cid,
            ArrayList<ISlideModelCallback> slides) {
        for (ISlideModelCallback slide : slides) {
            for (int i = 0; i < slide.getMediaModelSize(); i++) {
                IMediaModelCallback media = (IMediaModelCallback) slide.getMediaModel(i);
                Log.d(TAG, "text html filename = " + filename);

                // The src in MediaModel is content location or filename
                if (filename != null && media != null) {
                    if (filename.compareTo(media.getSrcCallback()) == 0) {
                        Log.d(TAG, "This media is in smil (1)");
                        return true;
                    }
                }

                // The src in MediaModel is content id
                String cidSrc1 = null;
                if (media != null) {
                 // src in MediaModel
                    cidSrc1 = Op01MmsUtils.unescapeXML(media.getSrcCallback());
                }
                String cidSrc2 = null;
                if (cidSrc1 != null && cid != null) {
                    if (cidSrc1.startsWith("cid:")) {
                        cidSrc2 = "<" + cidSrc1.substring("cid:".length())
                                + ">"; // <XXX>

                        if (cidSrc2.compareTo(cid) == 0) {
                            Log.d(TAG, "This media is in smil (2)");
                            return true;
                        } else {
                            cidSrc2 = "<" + cidSrc1 + ">"; // <cid:XXX>
                            if (cidSrc2.compareTo(cid) == 0) {
                                Log.d(TAG, "This media is in smil (3)");
                                return true;
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "This media is NOT in smil !! ");
        return false;
    }
}
