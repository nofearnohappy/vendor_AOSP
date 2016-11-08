package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * PlainIntegerHandler is an abstract NodeIoHandler implementation which provides writable control
 * and a simple wrapper for straightforward Integer data access interface. Descendants of this class
 * should override readValue and writeValue methods to provide simple Integer read/write function
 * for PlainIntegerHandler. The interaction protocol (e.g. size inspection) is taken care of by
 * PlainIntegerHandler.
 *
 * @author mtk81226
 */
public abstract class PlainIntegerHandler implements NodeIoHandler {

    private byte[] mWriteCache = null;
    protected final String mUri;
    protected final boolean mWritable;

    public PlainIntegerHandler(String uri, boolean writable) {
        mUri = uri;
        mWritable = writable;
    }

    public PlainIntegerHandler(String uri) {
        this(uri, true);
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainIntegerHandler.read(" + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "PlainIntegerHandler.read: mUri = " + mUri);
        byte[] result = Integer.toString(readValue()).getBytes();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainIntegerHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainIntegerHandler.write(" + mUri + ", " + offset + ", " + data
                + ", " + totalSize + ")");
        if (!mWritable) {
            Log.e(TAG.NODEIOHANDLER, "mUri is not writable");
            return;
        }
        try {
            if (offset == 0 && data.length == totalSize) {
                writeValue(Integer.parseInt(new String(data)));
            } else {
                if (offset == 0 && data.length == 0 && totalSize != 0) {
                    mWriteCache = new byte[totalSize];
                } else {
                    System.arraycopy(data, 0, mWriteCache, offset, data.length);
                    if (offset + data.length == totalSize) {
                        // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                        // Check document for write() for details.
                        writeValue(Integer.parseInt(new String(mWriteCache, 0, totalSize)));
                        mWriteCache = null;
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG.NODEIOHANDLER, "Invalid input: " + new String(data));
            throw new MdmException(MdmError.BAD_INPUT);
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainIntegerHandler.write()");
    }

    protected abstract int readValue();

    protected abstract void writeValue(int value);
}
