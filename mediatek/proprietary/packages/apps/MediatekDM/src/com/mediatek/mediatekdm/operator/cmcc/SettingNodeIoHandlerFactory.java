package com.mediatek.mediatekdm.operator.cmcc;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.iohandler.CachedWriteCacheableWrapper;
import com.mediatek.mediatekdm.iohandler.ICacheable;
import com.mediatek.mediatekdm.iohandler.IoCacheManager;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SettingNodeIoHandlerFactory {
    private Context mContext = null;

    public SettingNodeIoHandlerFactory(Context context) {
        mContext = context;
    }

    public NodeIoHandler createNodeHandler(String configItem, Uri uri, String parameterString) {
        Class<?> ioHandlerClass = null;
        String className = "com.mediatek.mediatekdm.operator.cmcc.setting.Dm" + configItem
                + "NodeIoHandler";
        Log.d(TAG.NODEIOHANDLER, "Load IO handler class: " + className);

        try {
            ioHandlerClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.w(TAG.NODEIOHANDLER, "IO handler class: " + className + " is not found.");
            return null;
        }

        Constructor<?> ctorWith3Args = null;
        Constructor<?> ctorWith2Args = null;

        try {
            ctorWith3Args = ioHandlerClass.getConstructor(Context.class, Uri.class, String.class);
        } catch (NoSuchMethodException e) {
            Log.i(TAG.NODEIOHANDLER, "There is no constructor with 3 proper arguments found.");
            try {
                ctorWith2Args = ioHandlerClass.getConstructor(Context.class, Uri.class);
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                Log.i(TAG.NODEIOHANDLER,
                        "There is no constructor with 2 proper arguments found. Please your code.");
                return null;
            }
        }

        NodeIoHandler result = null;
        try {
            if (ctorWith3Args != null) {
                Log.d(TAG.NODEIOHANDLER, "Create IO handler: " + configItem + "(" + uri + ","
                        + parameterString + ")");
                result = (NodeIoHandler) (ctorWith3Args.newInstance(mContext, uri, parameterString));
            } else {
                if (ctorWith2Args != null) {
                    Log.d(TAG.NODEIOHANDLER, "Create IO handler: " + configItem + "(" + uri + ")");
                    result = (NodeIoHandler) (ctorWith2Args.newInstance(mContext, uri));
                }
            }
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        }

        if (result instanceof ICacheable) {
            Log.d(TAG.NODEIOHANDLER, result.getClass().toString() + " is cacheable");
            // result = new WriteThroughCacheableWrapper(result, IoCacheManager.getInstance());
            result = new CachedWriteCacheableWrapper(result, IoCacheManager.getInstance());
        } else {
            Log.d(TAG.NODEIOHANDLER, result.getClass().toString() + " is not cacheable");
        }

        return result;
    }
}
