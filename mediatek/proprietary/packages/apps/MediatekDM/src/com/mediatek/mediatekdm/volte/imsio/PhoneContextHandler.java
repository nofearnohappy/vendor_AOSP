
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.mo.ImsPhoneCtx;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class PhoneContextHandler extends PlainStringHandler {

    private final ImsManager mImsManager;
    private final int mIndex;

    public PhoneContextHandler(String uri, ImsManager manager, int index) {
        super(uri);
        mImsManager = manager;
        mIndex = index;
    }

    @Override
    protected String readValue() {
        ImsPhoneCtx[] phoneCtxArray = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            phoneCtxArray = imsConfig.getMasterImsPhoneCtxValue();
        } catch (ImsException e) {
            e.printStackTrace();
        }

        return phoneCtxArray[mIndex].getPhoneCtx();
    }

    @Override
    protected void writeValue(String value) {
        ImsPhoneCtx[] values = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            values = imsConfig.getMasterImsPhoneCtxValue();

            values[mIndex].setPhoneCtx(value);
            imsConfig.setProvisionedPhoneCtxValue(values);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

}
