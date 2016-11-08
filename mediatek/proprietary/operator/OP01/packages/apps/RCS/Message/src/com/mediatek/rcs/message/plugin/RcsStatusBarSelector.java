package com.mediatek.rcs.message.plugin;

import com.mediatek.mms.ipmessage.DefaultIpStatusBarSelectorExt;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import java.util.ArrayList;

/**
 * Plugin implements. response StatusBarSelectorCreator.java in MMS host.
 *
 */
public class RcsStatusBarSelector extends DefaultIpStatusBarSelectorExt {

    @Override
    public boolean onIpRefreshData(ArrayList<AccountInfo> data) {
        data.remove(0);
        return true;
    }
}
