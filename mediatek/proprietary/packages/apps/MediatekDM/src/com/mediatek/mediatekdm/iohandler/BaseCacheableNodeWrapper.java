package com.mediatek.mediatekdm.iohandler;

import com.mediatek.mediatekdm.mdm.NodeIoHandler;

public abstract class BaseCacheableNodeWrapper implements NodeIoHandler {
    protected final NodeIoHandler mHandler;
    protected byte[] mCache;
    protected final IoCacheManager mManager;

    /**
     * Argument handler must implement ICacheable.
     *
     * @param handler
     * @param manager
     */
    public BaseCacheableNodeWrapper(NodeIoHandler handler, IoCacheManager manager) {
        mHandler = handler;
        mManager = manager;
        mManager.put(((ICacheable) handler).getKey(), this);
    }

    public abstract void purge();

    public abstract void flush();
}
