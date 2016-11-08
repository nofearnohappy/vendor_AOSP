package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeUELocation;
import com.mediatek.mediatekdm.iohandler.PlainBinHandler;

public class LocationUELongtitudeHandler extends PlainBinHandler {
    private NodeUELocation mNode;

    public LocationUELongtitudeHandler(String uri, NodeUELocation node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected void writeValue(byte[] value) {
        mNode.locationGeo[1] = value[0];
    }

    @Override
    protected byte[] readValue() {
        return new byte[] {mNode.locationGeo[1]};
    }

}