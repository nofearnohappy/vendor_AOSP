package com.mediatek.op.documentsui;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.documentsui.IDocumentsUIExtension;

/**
 * This is the default implementation of documentsui operator plugin.
 */

@PluginImpl(interfaceName = "com.mediatek.common.documentsui.IDocumentsUIExtension")
public class DefaultDocumentsUIExtension implements IDocumentsUIExtension {

    private static final String TAG = "DefaultDocumentsUIExtension";
    private Context mContext;

    public DefaultDocumentsUIExtension(Context context) {
        mContext = context;
    }

    /**
     * @return Return current download item whether is downloading
     */
    public boolean checkIsDownloadingItem(String docId) {
       return false;
    }
}

