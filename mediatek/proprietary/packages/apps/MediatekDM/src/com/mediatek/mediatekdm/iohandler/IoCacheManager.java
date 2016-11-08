package com.mediatek.mediatekdm.iohandler;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.util.HashMap;

public class IoCacheManager extends HashMap<String, BaseCacheableNodeWrapper> {
    private static final long serialVersionUID = -9091348551633375697L;
    private static IoCacheManager sInstance;

    public synchronized static IoCacheManager getInstance() {
        if (sInstance == null) {
            sInstance = new IoCacheManager();
        }
        return sInstance;
    }

    private IoCacheManager() {
        super();
    }

    public void purge() {
        Log.d(TAG.NODEIOHANDLER, "Clear node IO cache.");
        for (BaseCacheableNodeWrapper wrapper : values()) {
            wrapper.purge();
        }
    }

    public void flush() {
        Log.d(TAG.NODEIOHANDLER, "Flush cache to backing handlers.");
        for (BaseCacheableNodeWrapper wrapper : values()) {
            wrapper.flush();
        }
    }
}
