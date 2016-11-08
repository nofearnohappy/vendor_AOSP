package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodePolicy;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class PolicyPLMNHandler extends PlainStringHandler {
    private NodePolicy.X mNode;

    public PolicyPLMNHandler(String uri, NodePolicy.X node) {
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