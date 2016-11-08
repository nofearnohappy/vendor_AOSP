package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeDiscoveryInfo;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class DiscoveryPLMNHandler extends PlainStringHandler {
    private NodeDiscoveryInfo.X mNode;

    public DiscoveryPLMNHandler(String uri, NodeDiscoveryInfo.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.plmn;
    }

    @Override
    protected void writeValue(String value) {
        mNode.plmn = value;
    }

}

