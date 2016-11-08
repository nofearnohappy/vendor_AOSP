package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeDiscoveryInfo;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;

public class DiscoveryNetworkTypeHandler extends PlainIntegerHandler {
    private NodeDiscoveryInfo.X mNode;

    public DiscoveryNetworkTypeHandler(String uri, NodeDiscoveryInfo.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected int readValue() {
        return mNode.accessNetworkType;
    }

    @Override
    protected void writeValue(int value) {
        mNode.accessNetworkType = value;
    }

}
