package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeForFlowBased;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;

/**
 * Common handler for both ForFlowBased & ForNonSeamlessOffload RulePriority.
 */
public class RulePriorityGenericHandler extends PlainIntegerHandler {
    private NodeForFlowBased.X mNode;

    public RulePriorityGenericHandler(String uri, NodeForFlowBased.X node) {
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
