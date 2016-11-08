package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.CollapsibleActionView;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.mediatek.op09.plugin.R;

/**
 * M: AdvanceSearchView ; Add for OP09;
 *
 */
public class AdvancedSearchView extends LinearLayout implements CollapsibleActionView {
    private SearchView mSearchView;
    private ImageButton mImageSearchBtn;
    private Activity mHostActivity;

    public AdvancedSearchView(Context context, Activity activity) {
        super(context, null);
        mHostActivity = activity;
        Resources resources = context.getResources();
        XmlResourceParser xrp = resources.getLayout(R.layout.advanced_search_view);
        LayoutInflater inflater = (LayoutInflater) mHostActivity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(xrp, this, true);
        mSearchView = (SearchView) findViewById(R.id.search_view);
        mImageSearchBtn = (ImageButton) findViewById(R.id.image_search_btn);
        Drawable d = resources.getDrawable(R.drawable.ic_menu_search_by_time_period);
        mImageSearchBtn.setImageDrawable(d);
        String contentDescription = resources.getString(R.string.search_by_time_period);
        mImageSearchBtn.setContentDescription(contentDescription);
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    public ImageButton getImageSearchBtn() {
        return mImageSearchBtn;
    }

    @Override
    public void onActionViewCollapsed() {
        if (mSearchView != null) {
            mSearchView.onActionViewCollapsed();
        }
    }

    @Override
    public void onActionViewExpanded() {
        if (mSearchView != null) {
            mSearchView.onActionViewExpanded();
        }
    }
}
