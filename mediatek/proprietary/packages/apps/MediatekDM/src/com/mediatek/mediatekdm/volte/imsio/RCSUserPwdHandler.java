
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class RCSUserPwdHandler extends PlainStringHandler {

    private final ImsManager mImsManager;

    public RCSUserPwdHandler(String uri, ImsManager manager) {
        super(uri);
        mImsManager = manager;
    }

    @Override
    protected String readValue() {
        return "";
//        return mImsManager.readImsAuthInfoMo().getUserPwd();
    }

    @Override
    protected void writeValue(String value) {
//        mImsManager.readImsAuthInfoMo().setUserPwd(value);
    }

}
