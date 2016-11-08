package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

/**
 * This wrapper take a full cache of the handler it wraps, so be careful if the handler contains big
 * amount of data.
 *
 * @author mtk81226
 */
public class WriteThroughCacheableWrapper extends BaseCacheableNodeWrapper {

    public WriteThroughCacheableWrapper(NodeIoHandler handler, IoCacheManager manager) {
        super(handler, manager);
    }

    /**
     * Read from cache first.
     */
    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "WriteThroughCacheableWrapper.read()");
        if (mCache == null) {
            final int totalLength = mHandler.read(0, null);
            mCache = new byte[totalLength];
            mHandler.read(0, mCache);
        }

        if (data != null) {
            System.arraycopy(mCache, offset, data, 0, data.length);
        }

        return mCache.length;
    }

    /**
     * Write through.
     */
    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "WriteThroughCacheableWrapper.write()");
        mHandler.write(offset, data, totalSize);
    }

    @Override
    public void purge() {
        mCache = null;
    }

    @Override
    public void flush() {
        // No need to write back.
        mCache = null;
    }
}
