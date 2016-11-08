package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * PlainBinHandler is an abstract NodeIoHandler implementation which provides writable control and a
 * simple wrapper for straightforward String data access interface. Descendants of this class should
 * override readValue and writeValue methods to provide simple String read/write function for
 * PlainBinHandler. The interaction protocol (e.g. size inspection) is taken care of by
 * PlainBinHandler.
 *
 * @author mtk81226
 */
public abstract class PlainBinHandler implements NodeIoHandler {

    private byte[] mWriteCache = null;
    protected final String mUri;
    protected final boolean mWritable;

    public PlainBinHandler(String uri, boolean writable) {
        mUri = uri;
        mWritable = writable;
    }

    public PlainBinHandler(String uri) {
        this(uri, true);
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainStringHandler.read(" + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "PlainStringHandler.read: mUri = " + mUri);
        byte[] result = readValue();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainStringHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainStringHandler.write(" + mUri + ", " + offset + ", " + data
                + ", " + totalSize + ")");
        if (!mWritable) {
            Log.e(TAG.NODEIOHANDLER, "mUri is not writable");
            return;
        }

        if (offset == 0 && data.length == totalSize) {
            writeValue(data);
        } else {
            if (offset == 0 && data.length == 0 && totalSize != 0) {
                mWriteCache = new byte[totalSize];
            } else {
                System.arraycopy(data, 0, mWriteCache, offset, data.length);
                if (offset + data.length == totalSize) {
                    // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                    // Check document for write() for details.
                    byte[] result = new byte[totalSize];
                    System.arraycopy(mWriteCache, 0, result, 0, totalSize);
                    writeValue(result);
                    mWriteCache = null;
                }
            }
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainStringHandler.write()");
    }

    protected abstract byte[] readValue();

    protected abstract void writeValue(byte[] value);
}
