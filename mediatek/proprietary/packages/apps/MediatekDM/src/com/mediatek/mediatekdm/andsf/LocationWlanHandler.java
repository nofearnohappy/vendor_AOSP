package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeWLANLocation;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class LocationWlanHandler extends NodeIoHandlerWrapper {
    static final String HESSID = "HESSID";
    static final String SSID = "SSID";
    static final String BSSID = "BSSID";

    private NodeWLANLocation.X mNode;
    private String mUri;

    public LocationWlanHandler(String uri, NodeWLANLocation.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(HESSID)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.hessid = value;
                }

                @Override
                protected String readValue() {
                    return mNode.hessid;
                }
            });
        } else if (nodeName.equals(SSID)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.ssid = value;
                }

                @Override
                protected String readValue() {
                    return mNode.ssid;
                }
            });
        } else if (nodeName.equals(BSSID)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.bssid = value;
                }

                @Override
                protected String readValue() {
                    return mNode.bssid;
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }
}