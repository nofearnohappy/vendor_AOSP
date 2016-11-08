package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeDiscoveryInfo;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class DiscoveryNetworkInfoRefHandler extends PlainStringHandler {
    private NodeDiscoveryInfo.X mNode;

    public DiscoveryNetworkInfoRefHandler(String uri, NodeDiscoveryInfo.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.accessNetworkInfoRef;
    }

    @Override
    protected void writeValue(String value) {
        mNode.accessNetworkInfoRef = value;
    }

}
