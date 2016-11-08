package com.mediatek.rcs.message.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.GridView;

// public class RcsGroupManagementGridView extends GridView {
//  public RcsGroupManagementGridView(Context context) {
//      super(context);
//  }

//  public RcsGroupManagementGridView(Context context, AttributeSet attrs) {
//      super(context, attrs);
//  }

//  @Override
//  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//      //int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
//      //super.onMeasure(widthMeasureSpec, expandSpec);

//         //int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
//         //super.onMeasure(widthMeasureSpec, expandSpec);
//         //ViewGroup.LayoutParams params = getLayoutParams();
//         //params.height = getMeasuredHeight();
//         super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//  }

//  @Override
//  public boolean dispatchTouchEvent(MotionEvent ev) {
//      if (ev.getAction() == MotionEvent.ACTION_MOVE) {
//          return true;
//         }
//      return super.dispatchTouchEvent(ev);
//  }
// }


// ==================================================
public class RcsGroupManagementGridView extends GridView {
    private static final String TAG = "Rcs/RcsGroupManagementGridView";

    public RcsGroupManagementGridView(Context context) {
        super(context);
    }

    public RcsGroupManagementGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
        //         MeasureSpec.AT_MOST);
        // super.onMeasure(widthMeasureSpec, expandSpec);

        if (isExpanded()) {
            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = getMeasuredHeight();
            Log.d(TAG, "onMeasure() height=" + getMeasuredHeight());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    boolean mExpanded = false;
    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (pointToPosition((int) event.getX(), (int) event.getY()) == -1
            && event.getAction() == MotionEvent.ACTION_UP) {
                if (mListener != null) {
                    mListener.onNoItemClick();
                }
        }

        //if(ev.getAction() == MotionEvent.ACTION_MOVE)
        //  return true;
        return super.dispatchTouchEvent(event);
    }

    private OnNoItemClickListener mListener;
    public interface OnNoItemClickListener {
        void onNoItemClick();
    }

    public void setOnNoItemClickListener(OnNoItemClickListener listener) {
        mListener = listener;
    }
}
