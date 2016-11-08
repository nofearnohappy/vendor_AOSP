package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeOnAddHandler;
import com.mediatek.mediatekdm.mdm.NodeOnDeleteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

public class DPDataAddDelHandler implements NodeOnAddHandler, NodeOnDeleteHandler {
    private MdmScomoDp mDp;
    private MdmScomo mScomo;

    public DPDataAddDelHandler(MdmScomo scomo, MdmScomoDp dp) {
        mDp = dp;
        mScomo = scomo;
    }

    public void onAdd(String uri) {
        try {
            mScomo.getTree().registerNodeIoHandler(
                    MdmTree.makeUri(mDp.getDeliveredUri(), Uri.DATA),
                    new DPDataIoHandler(mScomo, mDp));
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    public void onDelete(String uri) {
        try {
            mScomo.getTree().unregisterNodeIoHandler(
                    MdmTree.makeUri(mDp.getDeliveredUri(), Uri.DATA));
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

}
