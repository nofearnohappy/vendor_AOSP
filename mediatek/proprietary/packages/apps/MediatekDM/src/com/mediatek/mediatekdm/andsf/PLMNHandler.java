package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeISRP;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class PLMNHandler extends PlainStringHandler {
    private NodeISRP.X mNode;

    public PLMNHandler(String uri, NodeISRP.X node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.plmn;
    }

    @Override
    protected void writeValue(String value) {
        mNode.plmn = value;
    }

}
