/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.app.Activity;
import android.graphics.Point;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dolby.ds1appCoreUI.Tag;

public class ViewTools {

    private ViewTools() {
    }

    public static int getVisibleChildIndexAt(final ViewGroup vg, final float x,
            final float y) {
        for (int i = 0; i < vg.getChildCount(); ++i) {
            View ch = vg.getChildAt(i);
            if (x >= ch.getLeft() && x <= ch.getRight() && y >= ch.getTop() && y <= ch.getBottom() && View.VISIBLE == ch.getVisibility()) {
                return i;
            }
        }
        return -1;
    }

    public static View getVisibleChildViewAt(final ViewGroup vg, final float x,
            final float y) {
        int n = getVisibleChildIndexAt(vg, x, y);
        if (n == -1) {
            return null;
        }
        return vg.getChildAt(n);
    }

    public static boolean testDrawableState(final int[] state,
            final int[] referenceState) {
        boolean matches = true;
        for (int i = 0; i < referenceState.length; ++i) {
            boolean found = false;
            for (int j = 0; j < state.length; ++j) {
                if (referenceState[i] == state[j]) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    /**
     * Check if calling thread is UI thread.
     * 
     * @return
     */
    public static boolean isUIThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    /**
     * Show a tooltip with arrow at bottom. The arrow will point to the view
     * specified.
     * 
     * @param activity
     *            activity to which add a tooltip.
     * @param rootView
     *            root view of the activity. It should be determined with
     *            determineNativeViewContainer method.
     * @param pointTo
     *            a view to which arrow should point to.
     * @param title
     *            tooltip title.
     * @param text
     *            tooltip text.
     * @return a tooltip view. It is already added to activity.
     */
    public static View showTooltip(Activity activity, final ViewGroup rootView,
            View pointTo, CharSequence title, CharSequence text) {
        final Point posPointTo = getRelativePosition(pointTo, rootView);
        posPointTo.x += pointTo.getWidth() / 2;

        final ViewGroup vTooltip = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.tooltip2, null);
        final ViewGroup vTooltipMain = (ViewGroup) vTooltip.findViewById(R.id.main);
        final TextView vTitle = (TextView) vTooltip.findViewById(R.id.title);
        final TextView vText = (TextView) vTooltip.findViewById(R.id.text);
        final View vArrow = vTooltip.findViewById(R.id.bottom_arrow);
        if(activity.getResources().getString(R.string.tooltip_eq_title).contentEquals(title)){
            vText.setMaxWidth(vText.getMaxWidth()*2);
        }
        vTitle.setTypeface(Assets.getFont(Assets.FontType.REGULAR));
        vText.setTypeface(Assets.getFont(Assets.FontType.REGULAR));
        vTitle.setText(title);
        vText.setText(text);

        final FrameLayout.LayoutParams lpArrow = (android.widget.FrameLayout.LayoutParams) vArrow.getLayoutParams();
        final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        if (3 * posPointTo.x <= screenWidth) {
            lpArrow.gravity = Gravity.LEFT;
        } else if (3 * posPointTo.x >= 2 * screenWidth) {
            lpArrow.gravity = Gravity.RIGHT;
        } else {
            lpArrow.gravity = Gravity.CENTER_HORIZONTAL;
        }

        final FrameLayout.LayoutParams lpTooltip = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        final FrameLayout.LayoutParams lpTooltipMain = (android.widget.FrameLayout.LayoutParams) vTooltipMain.getLayoutParams();

        vTooltip.setVisibility(View.INVISIBLE);
        activity.addContentView(vTooltip, lpTooltip);

        vTooltip.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            private int counter = 0;

            @Override
            public void onGlobalLayout() {
                if (counter == 0) {
                    final Point posArrow = getRelativePosition(vArrow, vTooltipMain);
                    Log.d(Tag.MAIN, "posArrow: " + posArrow);

                    lpTooltipMain.leftMargin = posPointTo.x - (posArrow.x + vArrow.getWidth() / 2);
                    if(lpTooltipMain.leftMargin<0){
                    	final FrameLayout.LayoutParams vArrowlp = (android.widget.FrameLayout.LayoutParams) vArrow.getLayoutParams();
                    	vArrowlp.leftMargin += lpTooltipMain.leftMargin;
                    	lpTooltipMain.leftMargin = 0;
                    }
                    
                    lpTooltipMain.topMargin = posPointTo.y - vTooltipMain.getHeight();
                    lpTooltipMain.gravity = Gravity.LEFT | Gravity.RIGHT;

                    vTooltip.requestLayout();
                    vTooltipMain.requestLayout();
                } else {
                    vTooltip.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    vTooltip.setVisibility(View.VISIBLE);
                    rootView.bringChildToFront(vTooltip);
                    vTooltip.requestLayout();
                }

                counter++;
            }
        });

        vTooltip.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                removeFromParent(vTooltip);
                return true;
            }
        });

        return vTooltip;
    }

    /**
     * Remove a view from its parent.
     * 
     * @param v
     */
    public static void removeFromParent(View v) {
        ViewParent vp = v.getParent();
        if (vp instanceof ViewGroup) {
            ((ViewGroup) vp).removeView(v);
        }
    }

    /**
     * 
     * Determine a ViewGroup instance to which view is added either by
     * setContentView or addContentView.
     * 
     * @param activity
     * @return
     */
    public static ViewGroup determineNativeViewContainer(Activity activity) {
        // determine native view container
        View view = new View(activity);
        activity.addContentView(view, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ViewParent rootView = view.getParent();
        removeFromParent(view);
        if (rootView instanceof ViewGroup) {
            return (ViewGroup) rootView;
        } else {
            return null;
        }
    }

    /**
     * Determine a position relative to a parent view in a view hierarchy.
     * relativeTo Param must be one of the parents in view tree.
     * 
     * @param v
     * @param relativeTo
     * @return Point object with x, y coords.
     */
    public static Point getRelativePosition(final View v,
            final ViewGroup relativeTo) {
        if (v == null || relativeTo == null) {
            return null;
        }

        final Point p = new Point(v.getLeft(), v.getTop());
        View view = v;

        ViewParent vp;
        while ((vp = view.getParent()) != relativeTo) {
            if (vp instanceof View) {
                view = (View) vp;
                p.x += view.getLeft();
                p.y += view.getTop();
            } else {
                // should not happen
                break;
            }
        }

        return p;
    }
}
