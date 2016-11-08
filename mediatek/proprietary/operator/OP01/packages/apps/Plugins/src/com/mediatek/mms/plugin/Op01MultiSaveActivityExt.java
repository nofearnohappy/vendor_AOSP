package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduPart;

import com.mediatek.mms.callback.IFileAttachmentModelCallback;
import com.mediatek.mms.callback.IMultiSaveActivityCallback;
import com.mediatek.mms.ext.DefaultOpMultiSaveActivityExt;

import java.util.ArrayList;

/**
 * Op01MultiSaveActivityExt.
 *
 */
public class Op01MultiSaveActivityExt extends DefaultOpMultiSaveActivityExt {
    private static final String TAG = "Op01MultiSaveActivityExt";

    @Override
    public boolean initListAdapter(Context context, String src,
            long sMode, String type, ArrayList attachDatas,
            PduPart part, long msgId, ArrayList attachFiles,
            IMultiSaveActivityCallback callback,
            IFileAttachmentModelCallback attachCallback) {
        if (src == null) {
            return false;
        }

        Log.v(TAG, "In multisave initList. smode = " + sMode);
        if (sMode == Op01AttachmentEnhance.MMS_SAVE_ALL_ATTACHMENT) {
            // save all attachment including slides
            Log.v(TAG, "save all attachment including slides");
            if ((ContentType.isImageType(type)
                    || ContentType.isVideoType(type)
                    || "application/ogg".equalsIgnoreCase(type)
                    || ContentType.isAudioType(type)
                    || attachCallback.isSupportedFileCallback(part))
                    && !type.equals(ContentType.TEXT_PLAIN)
                    && !type.equals(ContentType.TEXT_HTML)) {
                callback.addMultiListItemData(context, attachDatas, part, msgId);
            }
        } else if (sMode == Op01AttachmentEnhance.MMS_SAVE_OTHER_ATTACHMENT) {
            // Only save attachment files no including slides
            Log.v(TAG, "Only save attachment files no including slides");
            String partUri = part.getDataUri().toString();
            for (int i = 0; i < attachFiles.size(); i++) {
                IFileAttachmentModelCallback attach
                        = (IFileAttachmentModelCallback) attachFiles.get(i);

                        if (partUri != null && attach.getUriCallback() != null
                            && !type.equals(ContentType.TEXT_PLAIN)
                            && !type.equals(ContentType.TEXT_HTML)) {
                    if (partUri.compareTo(attach.getUriCallback().toString()) == 0) {
                        callback.addMultiListItemData(context, attachDatas, part, msgId);
                        Log.d(TAG, "add attach not text and html");
                    }
                }
            }
        }

        /// M: add text and html attachment
        for (int i = 0; i < attachFiles.size(); i++) {
            if (src.equals(((IFileAttachmentModelCallback) attachFiles.get(i)).getSrcCallback())
                    && (type.equals(ContentType.TEXT_PLAIN)
                            || type.equals(ContentType.TEXT_HTML))) {
                callback.addMultiListItemData(context, attachDatas, part, msgId);
            }
        }

        return true;
    }

    @Override
    public void save(Context context, long sMode,
            boolean succeeded, String copySuccess, String copyFailed) {
            Log.v(TAG, "OUT MMS_SAVE_OTHER_ATTACHMENT");
            if (sMode == Op01AttachmentEnhance.MMS_SAVE_OTHER_ATTACHMENT) {
                Log.v(TAG, "IN MMS_SAVE_OTHER_ATTACHMENT");
                String msg = succeeded ? copySuccess : copyFailed;
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
    }

    @Override
    public long onCreate(Intent intent) {
        long msgId = -1;
        if (intent != null && intent.hasExtra("msgid")) {
            msgId = intent.getLongExtra("msgid", -1);

            return Op01AttachmentEnhance.getSaveAttachMode(intent);
        }
        return -1L;
    }
}
