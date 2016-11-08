package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodePolicy;
import com.mediatek.mediatekdm.iohandler.PlainBooleanHandler;

public class PolicyUpdatePolicyHandler extends PlainBooleanHandler {
    private NodePolicy.X mNode;

    public PolicyUpdatePolicyHandler(String uri, NodePolicy.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected boolean readValue() {
        return mNode.updatePolicy;
    }

    @Override
    protected void writeValue(boolean value) {
        mNode.updatePolicy = value;
    }

}

