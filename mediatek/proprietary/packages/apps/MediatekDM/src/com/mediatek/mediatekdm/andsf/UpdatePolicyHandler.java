package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeISRP;
import com.mediatek.mediatekdm.iohandler.PlainBooleanHandler;

public class UpdatePolicyHandler extends PlainBooleanHandler {
    private NodeISRP.X mNode;

    public UpdatePolicyHandler(String uri, NodeISRP.X node) {
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
