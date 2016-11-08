package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeRoutingRule;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class RoutingRuleGenericHandler extends NodeIoHandlerWrapper {
    static final String ACCESS_TECHNOLOGY = "AccessTechnology";
    static final String ACCESS_ID = "AccessId";
    static final String SECONDARY_ACCESS_ID = "SecondaryAccessId";
    static final String ACCESS_NETWORK_PRIORITY = "AccessNetworkPriority";

    private NodeRoutingRule.X mNode;
    private String mUri;

    public RoutingRuleGenericHandler(String uri, NodeRoutingRule.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(ACCESS_TECHNOLOGY)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.accessTechnology = value;
                }

                @Override
                protected int readValue() {
                    return mNode.accessTechnology;
                }
            });
        } else if (nodeName.equals(ACCESS_ID)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.accessId = value;
                }

                @Override
                protected String readValue() {
                    return mNode.accessId;
                }
            });
        } else if (nodeName.equals(SECONDARY_ACCESS_ID)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.secondaryAccessId = value;
                }

                @Override
                protected String readValue() {
                    return mNode.secondaryAccessId;
                }
            });
        } else if (nodeName.equals(ACCESS_NETWORK_PRIORITY)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.accessNetworkPriority = value;
                }

                @Override
                protected int readValue() {
                    return mNode.accessNetworkPriority;
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }
}
