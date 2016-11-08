package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * PlainBooleanHandler is an abstract NodeIoHandler implementation which provides writable control
 * and a simple wrapper for straightforward Boolean data access interface. Descendants of this class
 * should override readValue and writeValue methods to provide simple Boolean read/write function
 * for PlainBooleanHandler. The interaction protocol (e.g. size inspection) is taken care of by
 * PlainBooleanHandler.
 *
 * @author mtk81226
 */
public abstract class PlainBooleanHandler implements NodeIoHandler {

    private static final String TRUE_REPRESENTATION = "1";
    private static final String FALSE_REPRESENTATION = "0";

    private byte[] mWriteCache = null;
    protected final String mUri;
    protected final boolean mWritable;
    private final String mTrueString;
    private final String mFalseString;

    public PlainBooleanHandler(String uri, boolean writable, String trueString, String falseString) {
        mUri = uri;
        mWritable = writable;
        mTrueString = trueString;
        mFalseString = falseString;
    }

    public PlainBooleanHandler(String uri, boolean writable) {
        this(uri, writable, TRUE_REPRESENTATION, FALSE_REPRESENTATION);
    }

    public PlainBooleanHandler(String uri) {
        this(uri, true);
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainBooleanHandler.read(" + offset + ", " + data + ")");
        Log.d(TAG.NODEIOHANDLER, "PlainBooleanHandler.read: mUri = " + mUri);
        byte[] result = (readValue() ? mTrueString : mFalseString).getBytes();
        if (data != null) {
            System.arraycopy(result, offset, data, 0, data.length);
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainBooleanHandler.read(" + result.length + ")");
        return result.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "+PlainBooleanHandler.write(" + mUri + ", " + offset + ", " + data
                + ", " + totalSize + ")");
        if (!mWritable) {
            Log.e(TAG.NODEIOHANDLER, "mUri is not writable");
            return;
        }
        try {
            if (offset == 0 && data.length == totalSize) {
                writeValue((new String(data)).equalsIgnoreCase(mTrueString));
            } else {
                if (offset == 0 && data.length == 0 && totalSize != 0) {
                    mWriteCache = new byte[totalSize];
                } else {
                    System.arraycopy(data, 0, mWriteCache, offset, data.length);
                    if (offset + data.length == totalSize) {
                        // NOTE: totalSize maybe a little smaller than mWriteCache at this moment.
                        // Check document for write() for details.
                        writeValue((new String(mWriteCache, 0, totalSize))
                                .equalsIgnoreCase(mTrueString));
                        mWriteCache = null;
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG.NODEIOHANDLER, "Invalid input: " + new String(data));
            throw new MdmException(MdmError.BAD_INPUT);
        }
        Log.d(TAG.NODEIOHANDLER, "-PlainBooleanHandler.write()");
    }

    protected abstract boolean readValue();

    protected abstract void writeValue(boolean value);
}
