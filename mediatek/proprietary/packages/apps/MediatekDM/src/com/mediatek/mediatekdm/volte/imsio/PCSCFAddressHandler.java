
package com.mediatek.mediatekdm.volte.imsio;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.mo.ImsLboPcscf;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;

public class PCSCFAddressHandler extends PlainStringHandler {

    private final ImsManager mImsManager;
    private final int mIndex;

    public PCSCFAddressHandler(String uri, ImsManager manager, int index) {
        super(uri);
        mImsManager = manager;
        mIndex = index;
    }

    @Override
    protected String readValue() {
        ImsLboPcscf[] lboPcscfs = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            lboPcscfs = imsConfig.getMasterLboPcscfValue();
        } catch (ImsException e) {
            e.printStackTrace();
        }

        return lboPcscfs[mIndex].getLboPcscfAddress();
    }

    @Override
    protected void writeValue(String value) {
        ImsLboPcscf[] values = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            values = imsConfig.getMasterLboPcscfValue();

            values[mIndex].setLboPcscfAddress(value);
            imsConfig.setProvisionedLboPcscfValue(values);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

}
