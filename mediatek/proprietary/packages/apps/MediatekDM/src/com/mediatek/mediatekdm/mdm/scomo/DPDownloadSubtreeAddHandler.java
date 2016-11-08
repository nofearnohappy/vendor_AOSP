package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeOnAddHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

class DPDownloadSubtreeAddHandler implements NodeOnAddHandler {
    private MdmScomo mScomo;

    public DPDownloadSubtreeAddHandler(MdmScomo scomo) {
        mScomo = scomo;
    }

    public void onAdd(String uri) {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DPDownloadSubtreeAddHandler.onAdd(" + uri + ")");
        String downloadRoot = MdmTree.makeUri(mScomo.getRootUri(), Uri.DOWNLOAD)
                + MdmTree.URI_SEPERATOR;
        mScomo.logMsg(MdmLogLevel.DEBUG, "downloadRoot is " + downloadRoot);
        if (uri.startsWith(downloadRoot)) {
            String trailing = uri.substring(downloadRoot.length());
            mScomo.logMsg(MdmLogLevel.DEBUG, "trailing is " + trailing);
            int index = trailing.indexOf(MdmTree.URI_SEPERATOR);
            if (index == -1 || index == trailing.length() - 1) {
                // trailing is the DP node name
                if (index != -1) {
                    trailing = trailing.substring(0, trailing.length() - 1);
                }
                mScomo.logMsg(MdmLogLevel.DEBUG, "DP node name is " + trailing);
                try {
                    mScomo.onNewDP(trailing);
                } catch (MdmException e) {
                    e.printStackTrace();
                }
            }
        }
        mScomo.logMsg(MdmLogLevel.DEBUG, "-DPDownloadSubtreeAddHandler.onAdd()");
    }
}
