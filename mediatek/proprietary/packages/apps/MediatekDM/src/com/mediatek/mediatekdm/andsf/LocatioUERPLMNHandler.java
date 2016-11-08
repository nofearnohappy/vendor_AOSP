package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeUELocation;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class LocatioUERPLMNHandler extends PlainStringHandler {
    private NodeUELocation mNode;

    public LocatioUERPLMNHandler(String uri, NodeUELocation node) {
        super(uri);
        mNode = node;
    }

    @Override
    protected String readValue() {
        return mNode.rplmn;
    }

    @Override
    protected void writeValue(String value) {
        mNode.rplmn = value;
    }

}