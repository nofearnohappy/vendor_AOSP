package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

import java.util.Arrays;

/**
 * This wrapper take a full cache of the handler it wraps, so be careful if the handler contains big
 * amount of data.
 *
 * @author mtk81226
 */
public class CachedWriteCacheableWrapper extends BaseCacheableNodeWrapper {

    public CachedWriteCacheableWrapper(NodeIoHandler handler, IoCacheManager manager) {
        super(handler, manager);
    }

    /**
     * Read from cache first.
     */
    @Override
    public int read(int offset, byte[] data) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "CachedWriteCacheableWrapper.read()");
        if (mCache == null) {
            return mHandler.read(offset, data);
        } else {
            if (data != null) {
                System.arraycopy(mCache, offset, data, 0, data.length);
            }
            return mCache.length;
        }
    }

    /**
     * Write to cache.
     */
    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        Log.d(TAG.NODEIOHANDLER, "CachedWriteCacheableWrapper.write()");
        if (offset == 0 && (data == null || data.length == 0)) {
            mCache = new byte[totalSize];
        } else if (offset == 0 && data != null && data.length > 0 && data.length == totalSize) {
            mCache = new byte[totalSize];
            System.arraycopy(data, 0, mCache, 0, data.length);
        } else if (data != null) {
            System.arraycopy(data, 0, mCache, offset, data.length);
            if (offset + data.length == totalSize && totalSize < mCache.length) {
                mCache = Arrays.copyOf(mCache, totalSize);
            }
        }
    }

    @Override
    public void purge() {
        mCache = null;
    }

    @Override
    public void flush() {
        if (mCache != null) {
            try {
                mHandler.write(0, mCache, mCache.length);
            } catch (MdmException e) {
                throw new Error(e);
            }
        }
        mCache = null;
    }
}
