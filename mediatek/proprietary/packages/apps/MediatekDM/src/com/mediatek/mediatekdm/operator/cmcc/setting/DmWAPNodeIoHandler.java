package com.mediatek.mediatekdm.operator.cmcc.setting;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.iohandler.DmDBNodeIoHandler;

public class DmWAPNodeIoHandler extends DmDBNodeIoHandler {

    private String[] mItem = { "HomePage" };
    private String[] mContentValue = { null };

    String[] mProjection = { "homepage" };

    Uri mTable = Uri.parse("content://com.android.browser/homepage");

    public DmWAPNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NODEIOHANDLER, "Conn constructed");

        mContext = ctx;
        mUri = treeUri;
        mMccMnc = mccMnc;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    @Override
    protected String[] getContentValue() {
        return mContentValue;
    }

    @Override
    protected String[] getItem() {
        return mItem;
    }

    @Override
    protected String[] getProjection() {
        return mProjection;
    }

    @Override
    protected Uri getTableToBeQueryed() {
        return mTable;
    }

}
