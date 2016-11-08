package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeIPFlow;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainBinHandler;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class IPFlowGenericHandler extends NodeIoHandlerWrapper {
    static final String ADDRESS_TYPE = "AddressType";
    static final String START_SOURCE_IP_ADDRESS = "StartSourceIPAddress";
    static final String END_SOURCE_IP_ADDRESS = "EndSourceIPAddress";
    static final String START_DEST_IP_ADDRESS = "StartDestIPAddress";
    static final String END_DEST_IP_ADDRESS = "EndDestIPAddress";
    static final String PROTOCOL_TYPE = "ProtocolType";
    static final String START_SOURCE_PORT_NUMBER = "StartSourcePortNumber";
    static final String END_SOURCE_PORT_NUMBER = "EndSourcePortNumber";
    static final String START_DEST_PORT_NUMBER = "StartDestPortNumber";
    static final String END_DEST_PORT_NUMBER = "EndDestPortNumber";
    static final String QOS = "QoS";
    static final String DOMAIN_NAME = "DomainName";

    private NodeIPFlow.X mNode;
    private String mUri;

    public IPFlowGenericHandler(String uri, NodeIPFlow.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(ADDRESS_TYPE)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.addressType = value;
                }

                @Override
                protected String readValue() {
                    return mNode.addressType;
                }
            });
        } else if (nodeName.equals(START_SOURCE_IP_ADDRESS)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.startSrcIPAddress = value;
                }

                @Override
                protected String readValue() {
                    return mNode.startSrcIPAddress;
                }
            });
        } else if (nodeName.equals(END_SOURCE_IP_ADDRESS)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.endSrcIPAddress = value;
                }

                @Override
                protected String readValue() {
                    return mNode.endSrcIPAddress;
                }
            });
        } else if (nodeName.equals(START_DEST_IP_ADDRESS)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.startDestIPAddress = value;
                }

                @Override
                protected String readValue() {
                    return mNode.startDestIPAddress;
                }
            });
        } else if (nodeName.equals(END_DEST_IP_ADDRESS)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.endDestIPAddress = value;
                }

                @Override
                protected String readValue() {
                    return mNode.endDestIPAddress;
                }
            });
        } else if (nodeName.equals(PROTOCOL_TYPE)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.protocolType = value;
                }

                @Override
                protected int readValue() {
                    return mNode.protocolType;
                }
            });
        } else if (nodeName.equals(START_SOURCE_PORT_NUMBER)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.startSrcPortNumber = value;
                }

                @Override
                protected int readValue() {
                    return mNode.startSrcPortNumber;
                }
            });
        } else if (nodeName.equals(END_SOURCE_PORT_NUMBER)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.endSrcPortNumber = value;
                }

                @Override
                protected int readValue() {
                    return mNode.endSrcPortNumber;
                }
            });
        } else if (nodeName.equals(START_DEST_PORT_NUMBER)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.startDestPortNumber = value;
                }

                @Override
                protected int readValue() {
                    return mNode.startDestPortNumber;
                }
            });
        } else if (nodeName.equals(END_DEST_PORT_NUMBER)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.endDestPortNumber = value;
                }

                @Override
                protected int readValue() {
                    return mNode.endDestPortNumber;
                }
            });
        } else if (nodeName.equals(QOS)) {
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.qos = value[0];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {mNode.qos};
                }
            });
        } else if (nodeName.equals(DOMAIN_NAME)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.domainName = value;
                }

                @Override
                protected String readValue() {
                    return mNode.domainName;
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }
}
