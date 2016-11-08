
package com.mediatek.mediatekdm.volte.xdmio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class UriHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public UriHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsXcapInfoMo().getXcapRootUri();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsXcapInfoMo().setXcapRootUri(value);
    }

}
