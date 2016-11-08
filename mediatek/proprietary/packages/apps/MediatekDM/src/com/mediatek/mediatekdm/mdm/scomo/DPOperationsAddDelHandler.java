package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeOnAddHandler;
import com.mediatek.mediatekdm.mdm.NodeOnDeleteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

class DPOperationsAddDelHandler implements NodeOnAddHandler, NodeOnDeleteHandler {
    private MdmScomoDp mDp;
    private MdmScomo mScomo;

    public DPOperationsAddDelHandler(MdmScomo scomo, MdmScomoDp dp) {
        mDp = dp;
        mScomo = scomo;
    }

    public void onAdd(String uri) {
        String[] segments = uri.split("/");
        String operator = segments[segments.length - 1];
        try {
            if (operator.equals(Uri.DOWNLOAD)) {
                mScomo.getTree().registerExecute(uri, new DLDownloadExecHandler(mScomo, mDp));
            } else if (operator.equals(Uri.DOWNLOADINSTALL)) {
                mScomo.getTree()
                        .registerExecute(uri, new DLDownloadInstallExecHandler(mScomo, mDp));
            } else if (operator.equals(Uri.DOWNLOADINSTALLINACTIVE)) {
                mScomo.getTree().registerExecute(uri,
                        new DLDownloadInstallInactiveExecHandler(mScomo, mDp));
            } else if (operator.equals(Uri.INSTALL)) {
                mScomo.getTree().registerExecute(uri, new DPInstallExecHandler(mScomo, mDp));
            } else if (operator.equals(Uri.INSTALLINACTIVE)) {
                mScomo.getTree()
                        .registerExecute(uri, new DPInstallInactiveExecHandler(mScomo, mDp));
            } else if (operator.equals(Uri.REMOVE)) {
                mScomo.getTree().registerExecute(uri, new DPRemoveExecHandler(mScomo, mDp));
            }
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    public void onDelete(String uri) {
        try {
            mScomo.getTree().unregisterExecute(uri);
            mScomo.getTree().unregisterOnAddHandler(uri);
            mScomo.getTree().unregisterOnDeleteHandler(uri);
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }
}