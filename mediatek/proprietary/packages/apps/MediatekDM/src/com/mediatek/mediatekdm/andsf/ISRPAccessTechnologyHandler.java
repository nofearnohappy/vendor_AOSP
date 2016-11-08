package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.CollaboRadioManager;
import com.mediatek.common.collaboradio.andsf.NodeISRP;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class ISRPAccessTechnologyHandler extends PlainIntegerHandler {
    private CollaboRadioManager mCollaboRadioManager;
    private static final int ISRP_OFFSET = 1;
    private static final int FOR_FLOW_BASED_OFFSET = 3;
    private static final int ROUTING_RULE_OFFSET = 5;
    private int mISRPIndex;
    private int mForFlowBasedIndex;
    private int mRoutingRuleIndex;

    public ISRPAccessTechnologyHandler(String uri, boolean writable, CollaboRadioManager collaboRadioManager) {
        super(uri, writable);
        mCollaboRadioManager = collaboRadioManager;
        String[] parts = uri.split(MdmTree.URI_SEPERATOR);
        int baseOffset = ANDSFComponent.ROOT_URI.split(MdmTree.URI_SEPERATOR).length;
        mISRPIndex = Integer.parseInt(parts[baseOffset + ISRP_OFFSET]) - 1;
        mForFlowBasedIndex = Integer.parseInt(parts[baseOffset + FOR_FLOW_BASED_OFFSET]) - 1;
        mRoutingRuleIndex = Integer.parseInt(parts[baseOffset + ROUTING_RULE_OFFSET]) - 1;
    }

    @Override
    protected int readValue() {
        NodeISRP nodeISRP = (NodeISRP) mCollaboRadioManager.readAndsfMO("/ISRP");
        return nodeISRP.x.get(mISRPIndex).flowBased
                       .x.get(mForFlowBasedIndex).routingRule
                       .x.get(mRoutingRuleIndex).accessNetworkPriority;
    }

    @Override
    protected void writeValue(int value) {
        NodeISRP nodeISRP = (NodeISRP) mCollaboRadioManager.readAndsfMO("/ISRP");
        nodeISRP.x.get(mISRPIndex).flowBased
                .x.get(mForFlowBasedIndex).routingRule
                .x.get(mRoutingRuleIndex).accessNetworkPriority = value;
        mCollaboRadioManager.writeAndsfMO("/ISRP", nodeISRP);
    }
}
