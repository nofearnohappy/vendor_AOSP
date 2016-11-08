package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeCircular;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainBinHandler;
import com.mediatek.mediatekdm.iohandler.PlainIntegerHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class LocationGeoHandler extends NodeIoHandlerWrapper {
    static final String ANCHOR_LATITUDE = "AnchorLatitude";
    static final String ANCHOR_LONGTITUDE = "AnchorLongtitude";
    static final String RADIUS = "Radius";

    private NodeCircular.X mNode;
    private String mUri;

    public LocationGeoHandler(String uri, NodeCircular.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(ANCHOR_LATITUDE)) {
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.anchorLatitude = value[0];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {mNode.anchorLatitude};
                }
            });
        } else if (nodeName.equals(ANCHOR_LONGTITUDE)) {
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.anchorLongitude = value[0];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {mNode.anchorLongitude};
                }
            });
        } else if (nodeName.equals(RADIUS)) {
            setHandler(new PlainIntegerHandler(uri) {
                @Override
                protected void writeValue(int value) {
                    mNode.radius = value;
                }

                @Override
                protected int readValue() {
                    return mNode.radius;
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }
}