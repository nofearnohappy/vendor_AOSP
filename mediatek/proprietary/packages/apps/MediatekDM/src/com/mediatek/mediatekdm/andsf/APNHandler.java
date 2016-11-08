package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeRoutingCriteria;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class APNHandler extends PlainStringHandler {
    private NodeRoutingCriteria.X mNode;

    public APNHandler(String uri, NodeRoutingCriteria.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.apn;
    }

    @Override
    protected void writeValue(String value) {
        mNode.apn = value;
    }

}
