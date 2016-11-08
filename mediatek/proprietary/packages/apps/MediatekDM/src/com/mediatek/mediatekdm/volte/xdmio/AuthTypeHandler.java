
package com.mediatek.mediatekdm.volte.xdmio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class AuthTypeHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public AuthTypeHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsXcapInfoMo().getXcapAuthType();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsXcapInfoMo().setXcapAuthType(value);
    }

}
