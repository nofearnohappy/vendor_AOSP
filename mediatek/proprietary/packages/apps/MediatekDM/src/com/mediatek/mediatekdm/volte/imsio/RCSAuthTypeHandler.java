
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class RCSAuthTypeHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public RCSAuthTypeHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsAuthInfoMo().getAuthType();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsAuthInfoMo().setAuthType(value);
    }

}
