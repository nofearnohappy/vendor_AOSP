package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeUEProfile;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class UEProfileHandler extends PlainStringHandler {
    private NodeUEProfile mNode;
    private int mIndex;

    public UEProfileHandler(String uri, NodeUEProfile node, int index) {
        super(uri);
        mNode = node;
        mIndex = index;
    }

    @Override
    protected String readValue() {
        return mNode.listOSId.get(mIndex);
    }

    @Override
    protected void writeValue(String value) {
        mNode.listOSId.set(mIndex, value);
    }

}
