package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeISRP;
import com.mediatek.mediatekdm.iohandler.PlainBooleanHandler;

public class RoamingHandler extends PlainBooleanHandler {
    private NodeISRP.X mNode;

    public RoamingHandler(String uri, NodeISRP.X node) {
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
