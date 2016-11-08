
package com.mediatek.mediatekdm.volte.xdmio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class AuthSecretHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public AuthSecretHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsXcapInfoMo().getXcapAuthSecret();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsXcapInfoMo().setXcapAuthSecret(value);
    }

}
