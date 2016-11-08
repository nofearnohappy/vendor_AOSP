
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class RCSRealmHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public RCSRealmHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsAuthInfoMo().getRelam();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsAuthInfoMo().setRelam(value);
    }

}
