package com.mediatek.mms.plugin;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mediatek.mms.callback.IAttachmentEditorCallback;
import com.mediatek.mms.callback.ISlideModelCallback;
import com.mediatek.mms.callback.ISlideshowModelCallback;
import com.mediatek.mms.ext.DefaultOpAttachmentEditorExt;

/**
 * Op01AttachmentEditorExt.
 *
 */
public class Op01AttachmentEditorExt extends DefaultOpAttachmentEditorExt {

    private IAttachmentEditorCallback mAttachmentEditor;

    public void init(IAttachmentEditorCallback attachmentEditor) {
        mAttachmentEditor = attachmentEditor;
    }

    @Override
    public void createMediaView(Button removeButton) {
        mAttachmentEditor.setOnClickListener(removeButton, MSG_REMOVE_SLIDES_ATTACHMENT);
    }

    @Override
    public void createFileAttachmentView(View remove) {
        mAttachmentEditor.setOnClickListener(remove, MSG_REMOVE_EXTERNAL_ATTACHMENT);
    }

    @Override
    public void createSlideshowView(Context context,
            LinearLayout slideshowPanel, Button sendButton, Button removeButton) {
        mAttachmentEditor.setOnClickListener(removeButton, MSG_REMOVE_SLIDES_ATTACHMENT);
    }

    @Override
    public boolean update() {
        return true;
    }

    @Override
    public boolean onTextChangeForOneSlide() {
        return true;
    }
}
