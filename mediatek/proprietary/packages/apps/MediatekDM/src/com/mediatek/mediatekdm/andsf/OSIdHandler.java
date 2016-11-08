package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeAppID;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class OSIdHandler extends PlainStringHandler {
    private NodeAppID.X mNode;

    public OSIdHandler(String uri, NodeAppID.X node) {
        super(uri, true);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.osId;
    }

    @Override
    protected void writeValue(String value) {
        mNode.osId = value;
    }

}
