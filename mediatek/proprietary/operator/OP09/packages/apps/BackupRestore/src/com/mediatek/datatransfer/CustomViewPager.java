package com.mediatek.datatransfer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {

    private boolean fibiddenScroll = false;

    public void setFibiddenScroll(boolean fibiddenScroll) {
        this.fibiddenScroll = fibiddenScroll;
    }

    public CustomViewPager(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        // TODO Auto-generated method stub
        if (this.fibiddenScroll) {
            return false;
        }
        return super.onTouchEvent(arg0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        // TODO Auto-generated method stub
        if (this.fibiddenScroll) {
            return false;
        }
        return super.onInterceptTouchEvent(arg0);
    }

}
