package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeForServiceBased;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;

public class FSBRulePriorityHandler extends PlainIntegerHandler {
    private NodeForServiceBased.X mNode;

    public FSBRulePriorityHandler(String uri, NodeForServiceBased.X node) {
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
