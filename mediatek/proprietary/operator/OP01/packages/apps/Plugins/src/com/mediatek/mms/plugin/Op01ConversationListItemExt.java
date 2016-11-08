package com.mediatek.mms.plugin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.mms.ext.DefaultOpConversationListItemExt;
import com.mediatek.mms.ext.IOpConversationExt;
import com.mediatek.op01.plugin.R;

/**
 * Op01ConversationListItemExt.
 *
 */
public class Op01ConversationListItemExt extends DefaultOpConversationListItemExt {

    private final static int ICON_ID = 0x7f0f0f0f;

    /**
     * Construction.
     * @param context Context
     */
    public Op01ConversationListItemExt(Context context) {
        super(context);
    }

    @Override
    public boolean bind(Context context, TextView dateView, TextView unreadView, ImageView simType,
            IOpConversationExt opConversation, boolean showDraftIcon, LinearLayout la) {
        Log.d("Op01MmsConversationListItemExt", "showDraftIcon = " + showDraftIcon);
        if (showDraftIcon && la.findViewById(ICON_ID) == null) {
            Log.d("Op01MmsConversationListItemExt", "add draft icon view");
            ImageView iv = new ImageView(context);
            iv.setId(ICON_ID);
            Drawable icon = getResources().getDrawable(R.drawable.ic_draft);
            iv.setImageDrawable(icon);
            iv.setVisibility(View.VISIBLE);
            la.addView(iv, 0);
        } else if (!showDraftIcon && la.findViewById(ICON_ID) != null) {
            Log.d("Op01MmsConversationListItemExt", "remove draft icon view");
            la.removeView(la.findViewById(ICON_ID));
        }
        return false;
    }

}
