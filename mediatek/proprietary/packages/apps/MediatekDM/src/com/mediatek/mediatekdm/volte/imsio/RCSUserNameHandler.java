
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class RCSUserNameHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public RCSUserNameHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsAuthInfoMo().getUserName();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsAuthInfoMo().setUserName(value);
    }

}
