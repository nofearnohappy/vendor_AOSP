package com.mediatek.mms.plugin;

import android.content.Context;
import android.database.Cursor;

import com.mediatek.mms.callback.IColumnsMapCallback;
import com.mediatek.mms.ext.DefaultOpMessageListAdapterExt;

/**
 * Op01MessageListAdapterExt.
 * Plugin Implement.
 *
 */
public class Op01MessageListAdapterExt extends DefaultOpMessageListAdapterExt {

    private Op01MmsMultiDeleteAndForward mMultiDeleteAndForwardExt
            = new Op01MmsMultiDeleteAndForward();

    /**
     * Op01MessageListAdapterExt Construction.
     * @param context Context
     */
    public Op01MessageListAdapterExt(Context context) {
        super(context);
    }

    @Override
    public void initListMap(Cursor cursor, IColumnsMapCallback columnsMap) {
        mMultiDeleteAndForwardExt.setBodyandAddress(cursor, columnsMap);
    }

    @Override
    public void clearList() {
        mMultiDeleteAndForwardExt.clearBodyandAddressList();
    }

    @Override
    public  String getBody(long id) {
        return mMultiDeleteAndForwardExt.getBody(id);
    }

    @Override
    public  String getAddress(long id) {
        return mMultiDeleteAndForwardExt.getAddress(id);
    }

    @Override
    public int getBoxType(long id) {
        return mMultiDeleteAndForwardExt.getBoxType(id);
    }
}
