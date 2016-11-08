package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeOnAddHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

public class DPDeliveredSubtreeAddHandler implements NodeOnAddHandler {
    private MdmScomo mScomo;

    public DPDeliveredSubtreeAddHandler(MdmScomo scomo) {
        mScomo = scomo;
    }

    public void onAdd(String uri) {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DPDeliveredSubtreeAddHandler.onAdd(" + uri + ")");
        String deliveredRoot = MdmTree.makeUri(mScomo.getRootUri(), Uri.INVENTORY, Uri.DELIVERED)
                + MdmTree.URI_SEPERATOR;
        mScomo.logMsg(MdmLogLevel.DEBUG, "deliveredRoot is " + deliveredRoot);
        if (uri.startsWith(deliveredRoot)) {
            String trailing = uri.substring(deliveredRoot.length());
            mScomo.logMsg(MdmLogLevel.DEBUG, "trailing is " + trailing);
            int index = trailing.indexOf(MdmTree.URI_SEPERATOR);
            if (index == -1 || index == trailing.length() - 1) {
                // trailing is the DP node name
                mScomo.logMsg(MdmLogLevel.DEBUG, "DP node name is " + trailing);
                MdmTree tree = mScomo.getTree();
                MdmScomoDp dp = mScomo.createDP(trailing, null);
                DPOperationsAddDelHandler opHandler = new DPOperationsAddDelHandler(mScomo, dp);
                DPDataAddDelHandler dataHandler = new DPDataAddDelHandler(mScomo, dp);
                try {
                    tree.registerOnAddHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.INSTALL),
                            opHandler);
                    tree.registerOnAddHandler(
                            MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.INSTALLINACTIVE), opHandler);
                    tree.registerOnAddHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.REMOVE),
                            opHandler);
                    tree.registerOnAddHandler(MdmTree.makeUri(uri, Uri.DATA), dataHandler);
                    tree.registerOnDeleteHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.INSTALL),
                            opHandler);
                    tree.registerOnDeleteHandler(
                            MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.INSTALLINACTIVE), opHandler);
                    tree.registerOnDeleteHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.REMOVE),
                            opHandler);
                    tree.registerOnDeleteHandler(MdmTree.makeUri(uri, Uri.DATA), dataHandler);
                } catch (MdmException e) {
                    e.printStackTrace();
                }
            }
        }
        mScomo.logMsg(MdmLogLevel.DEBUG, "-DPDeliveredSubtreeAddHandler.onAdd()");
    }
}
