package com.mediatek.calendar.extension;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.android.calendar.R;
import com.mediatek.calendar.MTKUtils;
import com.android.calendar.Utils;

/**
 * M: #Share# Extension for EventInfoFragment, to extend its options menu
 * this plug-in adds the share event function to host
 */
public class EventInfoOptionsMenuExt implements IOptionsMenuExt {

    private static final int MENU_ITEM_ID = R.id.info_action_share;
    ///M: Do not want to show the share icon on Actionbar for tablet. @{
    private static boolean mIsTabletConfig = false;
    ///@}
    private Context mContext;
    private long mEventId;

    /**
     * M: Constructor need context to start activity and need to
     * know the current event's id
     * @param context context
     * @param eventId id of current event
     */
    public EventInfoOptionsMenuExt(Context context, long eventId) {
        mContext = context;
        mEventId = eventId;
        ///M: Do not want to show the share icon on Actionbar for tablet. @{
        mIsTabletConfig = Utils.getConfigBool(mContext, R.bool.tablet_config);
        ///@}
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        ///M: share in action bar.
        MenuItem share = menu.findItem(R.id.info_action_share);
        ///M: judge whether display the share icon in action bar. @{
        if (share != null && mIsTabletConfig == false) {
            share.setEnabled(true);
            share.setVisible(true);
        }
        ///@}
    }

    @Override
    public boolean onOptionsItemSelected(int itemId) {
        if (MENU_ITEM_ID == itemId) {
            MTKUtils.sendShareEvent(mContext, mEventId);
            return true;
        }
        return false;
    }

}
