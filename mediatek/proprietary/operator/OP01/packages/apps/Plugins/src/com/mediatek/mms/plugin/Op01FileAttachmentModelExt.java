package com.mediatek.mms.plugin;

import com.mediatek.mms.ext.DefaultOpFileAttachmentModelExt;

/**
 * Op01FileAttachmentModelExt.
 *
 */
public class Op01FileAttachmentModelExt extends DefaultOpFileAttachmentModelExt {

    @Override
    public boolean checkContentRestriction() {
        return true;
    }
}
