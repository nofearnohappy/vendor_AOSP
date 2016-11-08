
package com.mediatek.mediatekdm.volte.imsio;

import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.mo.ImsIcsi;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

public class ICSIModeHandler implements NodeIoHandler {

    private final ImsManager mImsManager;
    private byte[] mWriteCache = null;
    private final String mUri;
    private final int mIndex;

    public ICSIModeHandler(String uri, ImsManager manager, int index) {
        mImsManager = manager;
        mUri = uri;
        mIndex = index;
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+ICSIModeHandler.read(" + mUri + ", " + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "ICSIModeHandler.read: mIndex = " + mIndex);
        byte[] result = readValue();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-ICSIModeHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+ICSIModeHandler.write(" + mUri + ", " + offset + ", " + data + ", " + totalSize + ")");

        if (offset == 0 && data.length == totalSize) {
            boolean value = Boolean.parseBoolean(new String(data));
            writeValue(value);
        } else {
            if (offset == 0 && data.length == 0 && totalSize != 0) {
                mWriteCache = new byte[totalSize];
            } else {
                System.arraycopy(data, 0, mWriteCache, offset, data.length);
                if (offset + data.length == totalSize) {
                    // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                    //       Check document for write() for details.
                    boolean value = Boolean.parseBoolean(new String(mWriteCache, 0, totalSize));
                    writeValue(value);
                    mWriteCache = null;
                }
            }
        }
        Log.d(TAG.NODEIOHANDLER, "-ICSIModeHandler.write()");
    }

    private byte[] readValue() {
        ImsIcsi[] imsIcsi = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            imsIcsi = imsConfig.getMasterIcsiValue();
        } catch (ImsException e) {
            e.printStackTrace();
        }

        return (imsIcsi[mIndex].getIsAllocated() ? "true" : "false").getBytes();
    }

    private void writeValue(Boolean value) {
        ImsIcsi[] values = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            values = imsConfig.getMasterIcsiValue();

            values[mIndex].setIsAllocated(value);
            imsConfig.setProvisionedIcsiValue(values);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }
}
