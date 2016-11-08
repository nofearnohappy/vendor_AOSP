package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeUEProfile;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class DevCapabilityHandler extends PlainStringHandler {
    private NodeUEProfile mNode;

    public DevCapabilityHandler(String uri, NodeUEProfile node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.devCapability;
    }

    @Override
    protected void writeValue(String value) {
        mNode.devCapability = value;
    }

}
