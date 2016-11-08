package com.mediatek.rcs.pam.ui.messageitem;

import com.mediatek.rcs.pam.util.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class GifImageView extends ImageView {

    private Movie mMovie;
    private long mStart;
    int mLayerType = -1;
    int mDuration;

    private float mScaleX = 1;
    private float mScaleY = 1;

    public GifImageView(Context context) {
        super(context);
    }

    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void drawGif(String path) {

        mMovie = Movie.decodeFile(path);
        mDuration = mMovie.duration();
        Size size = Utils.getFineImageSize(
                new Size(mMovie.width(), mMovie.height()), getContext());
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = size.getHeight();
        params.width = size.getWidth();
        setLayoutParams(params);
        mLayerType = getLayerType();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mScaleX = ((float )size.getWidth())/mMovie.width();
        mScaleY = ((float )size.getHeight())/mMovie.height();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMovie == null) {
            super.onDraw(canvas);
        } else {
            long now = System.currentTimeMillis();
            if (mStart == 0) {
                mStart = now;
            }
            mMovie.setTime((int) ((now - mStart) % mDuration));
            canvas.scale(mScaleX, mScaleY);
            mMovie.draw(canvas, 0, 0);
            invalidate();
        }
    }

    public void unbind() {
        mMovie = null;
        mStart = 0;
        if (mLayerType != -1) {
            setLayerType(mLayerType, null);
        }
        mLayerType = -1;
    }
}
