
package com.mediatek.mediatekdm.volte.imsio;

import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * A wrapper NodeIoHandler for ImsManager's ID style String data access interface with writable switch support.
 */
public class StringHandler implements NodeIoHandler {

    protected final ImsManager mImsManager;
    protected byte[] mWriteCache = null;
    protected final boolean mWritable;
    protected final int mImsId;
    protected final String mUri;

    public StringHandler(String uri, int imsId, ImsManager manager, boolean writable) {
        mImsManager = manager;
        mWritable = writable;
        mImsId = imsId;
        mUri = uri;
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+StringHandler.read(" + mUri + ", " + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "StringHandler.read: mImsId = " + mImsId);
        byte[] result = readValue();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-StringHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+StringHandler.write(" + mUri + ", " + offset + ", " + data + ", " + totalSize + ")");
        if (!mWritable) {
            Log.e(TAG.NODEIOHANDLER, mUri + " does not support <replace>");
            return;
        }

        if (offset == 0 && data.length == totalSize) {
            writeValue(new String(data));
        } else {
            if (offset == 0 && data.length == 0 && totalSize != 0) {
                mWriteCache = new byte[totalSize];
            } else {
                System.arraycopy(data, 0, mWriteCache, offset, data.length);
                if (offset + data.length == totalSize) {
                    // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                    //       Check document for write() for details.
                    String result = new String(mWriteCache, 0, totalSize);
                    writeValue(result);
                    mWriteCache = null;
                }
            }
        }
        Log.d(TAG.NODEIOHANDLER, "-StringHandler.write()");
    }

    private byte[] readValue() {
        byte[] result = null;

        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            result = imsConfig.getMasterStringValue(mImsId).getBytes();
        } catch (ImsException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void writeValue(String value) {
        try {
            ImsConfig imsConfig = mImsManager.getConfigInterface();
            imsConfig.setProvisionedStringValue(mImsId, value);
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }
}
