package com.mediatek.mms.ipmessage;

import android.app.ListActivity;
import android.view.View;
import android.widget.ListView;


public interface IIpSettingListActivityExt {

    /**
     * called on onCreate
     * @param activity: this activity
     * @return boolean
     */
    public boolean onIpCreate(ListActivity activity);

    /**
     * called on isNeedUpdateView
     * @param needUpdate: is needUpdate
     * @return boolean: is need update
     */
    public boolean isIpNeedUpdateView(boolean needUpdate);

    /**
     * called on setAdapter
     * @param settingList: setting list
     * @return boolean
     */
    public String[] setIpAdapter(String[] settingList);

    /**
     * called on mUpdateViewStateHandler
     * @return boolean
     */
    public boolean handleIpMessage();

    /**
     * called on onListItemClick
     * @param position: position
     * @return boolean
     */
    public boolean onIpListItemClick(int position);
}
