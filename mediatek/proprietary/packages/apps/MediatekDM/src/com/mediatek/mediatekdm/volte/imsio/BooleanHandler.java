
package com.mediatek.mediatekdm.volte.imsio;

import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * A wrapper NodeIoHandler for ImsManager's ID style Boolean data access interface with writable switch support.
 */
public class BooleanHandler implements NodeIoHandler {

    private static final String TRUE_REPRESENTATION = "1";
    private static final String FALSE_REPRESENTATION = "0";

    protected final ImsManager mImsManager;
    protected byte[] mWriteCache = null;
    protected final boolean mWritable;
    protected final int mImsId;
    protected final String mUri;

    public BooleanHandler(String uri, int imsId, ImsManager manager, boolean writable) {
        mImsManager = manager;
        mWritable = writable;
        mImsId = imsId;
        mUri = uri;
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+BooleanHandler.read(" + mUri + ", " + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "BooleanHandler.read: mImsId = " + mImsId);
        boolean value = readValue();
        byte[] result = (value ? TRUE_REPRESENTATION : FALSE_REPRESENTATION).getBytes();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-BooleanHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+BooleanHandler.write(" + mUri + ", " + offset + ", " + data + ", " + totalSize + ")");
        if (!mWritable) {
            Log.e(TAG.NODEIOHANDLER, mUri + " does not support <replace>");
            return;
        }

        if (offset == 0 && data.length == totalSize) {
            boolean value = (new String(data)).equalsIgnoreCase(TRUE_REPRESENTATION);
            writeValue(value);
        } else {
            if (offset == 0 && data.length == 0 && totalSize != 0) {
                mWriteCache = new byte[totalSize];
            } else {
                System.arraycopy(data, 0, mWriteCache, offset, data.length);
                if (offset + data.length == totalSize) {
                    // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                    // Check document for write() for details.
                    boolean value = (new String(mWriteCache, 0, totalSize)).equalsIgnoreCase(TRUE_REPRESENTATION);
                    writeValue(value);
                }
            }
        }
        Log.d(TAG.NODEIOHANDLER, "-BooleanHandler.write()");
    }

    private Boolean readValue() {
        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            return imsConfig.getMasterValue(mImsId) == 1;
        } catch (ImsException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void writeValue(Boolean value) {
        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            imsConfig.setProvisionedValue(mImsId, value ? 1 : 0);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }
}
