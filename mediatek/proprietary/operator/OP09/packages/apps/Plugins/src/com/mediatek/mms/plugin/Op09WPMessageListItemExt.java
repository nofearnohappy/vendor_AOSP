package com.mediatek.mms.plugin;

import android.content.Context;
import android.widget.TextView;

import com.mediatek.mms.ext.DefaultOpWPMessageListItemExt;

public class Op09WPMessageListItemExt extends DefaultOpWPMessageListItemExt {

    @Override
    public void bindCommonMessage(Context context, int subId, TextView view) {
        Op09MmsUtils.showSimTypeBySubId(context, subId, view);
    }
}
