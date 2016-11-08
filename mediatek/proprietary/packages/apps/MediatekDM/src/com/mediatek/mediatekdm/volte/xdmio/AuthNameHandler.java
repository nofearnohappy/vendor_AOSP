
package com.mediatek.mediatekdm.volte.xdmio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class AuthNameHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public AuthNameHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsXcapInfoMo().getXcapAuth();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsXcapInfoMo().setXcapAuth(value);
    }

}
