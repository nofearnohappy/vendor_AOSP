
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.mo.ImsIcsi;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class ICSIHandler extends PlainStringHandler {

    private final ImsManager mImsManager;
    private final int mIndex;

    public ICSIHandler(String uri, ImsManager manager, int index) {
        super(uri);
        mImsManager = manager;
        mIndex = index;
    }

    @Override
    protected String readValue() {
        ImsIcsi[] imsIcsi = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            imsIcsi = imsConfig.getMasterIcsiValue();
        } catch (ImsException e) {
            e.printStackTrace();
        }

        return imsIcsi[mIndex].getIcsi();
    }

    @Override
    protected void writeValue(String value) {
        ImsIcsi[] values = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            values = imsConfig.getMasterIcsiValue();

            values[mIndex].setIcsi(value);
            imsConfig.setProvisionedIcsiValue(values);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

}
