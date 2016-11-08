package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeAppID;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class OSAppIdHandler extends PlainStringHandler {
    private NodeAppID.X mNode;
    private int mIndex;

    public OSAppIdHandler(String uri, NodeAppID.X node, int index) {
        super(uri, true);
        mNode = node;
        mIndex = index;
    }

    @Override
    protected String readValue() {
        return mNode.listOSAppId.get(mIndex);
    }

    @Override
    protected void writeValue(String value) {
        mNode.listOSAppId.set(mIndex, value);
    }

}
