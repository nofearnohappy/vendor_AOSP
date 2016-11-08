package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodePolicy;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;

public class PolicyRulePriorityGenericHandler extends PlainIntegerHandler {
    private NodePolicy.X mNode;

    public PolicyRulePriorityGenericHandler(String uri, NodePolicy.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected int readValue() {
        return mNode.rulePriority;
    }

    @Override
    protected void writeValue(int value) {
        mNode.rulePriority = value;
    }

}
