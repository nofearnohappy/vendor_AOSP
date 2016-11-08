package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodePolicy;
import com.mediatek.mediatekdm.iohandler.PlainBooleanHandler;

public class PolicyRoamingHandler extends PlainBooleanHandler {
    private NodePolicy.X mNode;

    public PolicyRoamingHandler(String uri, NodePolicy.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected boolean readValue() {
        return (mNode.roaming == 1);
    }

    @Override
    protected void writeValue(boolean value) {
        mNode.roaming = value ? ((byte) 1) : ((byte) 0);
    }

}