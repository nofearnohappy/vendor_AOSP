package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeForServiceBased;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

/**
 * APN hander for ForServiceBased node.
 */
public class FSBAPNHandler extends PlainStringHandler {
    private NodeForServiceBased.X mNode;

    public FSBAPNHandler(String uri, NodeForServiceBased.X node) {
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
