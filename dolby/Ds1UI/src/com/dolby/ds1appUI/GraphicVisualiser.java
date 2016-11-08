/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tag;

public class GraphicVisualiser extends SurfaceView implements
        SurfaceHolder.Callback {

    /**
     * Whether or not to use a handler in which painting should be performed. If
     * set to true then painting is forced to be performed on handler thread.
     */
    private static final boolean USE_PAINT_HANDLER = true;

    /**
     * Whether custom paint handler should be based on UI thread. If not then
     * separate thread is created for painting visualizer.
     */
    private static final boolean USE_UI_PAINT_HANDLER = false;

    private static final Handler PAINT_HANDLER;

    static {
        Handler h = null;
        if (USE_PAINT_HANDLER) {
            if (USE_UI_PAINT_HANDLER) {
                h = DS1Application.HANDLER;
            } else {
                HandlerThread ht = new HandlerThread("VisPaint", Process.THREAD_PRIORITY_DISPLAY);
                ht.start();
                h = new Handler(ht.getLooper());
            }
        }

        PAINT_HANDLER = h;
    }

    private GraphicVisualiserPainter mPainter;

    private GraphicEqualizerPainter mEqualizer;

    private SurfaceHolder mHolder;

    private boolean mSufraceCreated;

    public boolean mSuspended = false;

    public boolean mEnableEditTouch = true;
    
    private boolean mFragmentIsActive = false;

    /**
     * UI gains.
     */
    private final float[] mGainsUi = new float[Constants.BANDS];

    /**
     * User gains.
     */
    private final float[] mGainsUserSmoothed = new float[Constants.BANDS];

    public GraphicVisualiser(Context context) {
        super(context);
        init(context);
    }

    public GraphicVisualiser(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GraphicVisualiser(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mPainter = new GraphicVisualiserPainter(context, this, mGainsUi, mGainsUserSmoothed);
        mEqualizer = new GraphicEqualizerPainter(context, this, mGainsUi, mGainsUserSmoothed);

        mPainter.setEnabled(isEnabled());
        mEqualizer.setEnabled(isEnabled());
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public GraphicEqualizerPainter getEqualizer() {
        return mEqualizer;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(Tag.MAIN, "GraphicVisualiser.onSizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);

        // ---------------------------------------------------------------------------------
        // CHANGE HOLDER SIZE TO FIT CELLS NEATLY
        // ---------------------------------------------------------------------------------
        // ((w - 1) / Constants.BANDS) - 1

        // (w - 1): each bar has to allow for a 1 pixel border
        // Constants.BANDS: there are this many bars in total
        // - 1: there is a final border pixel at the end
        int bar_width = ((w - 1) / Constants.BANDS) - 1;
        int bar_height = ((h - 1) / GraphicVisualiserPainter.ROWS) - 1;

        // Now resize the background image to ensure cells fit perfectly
        int neww = ((bar_width + 1) * Constants.BANDS) + 1;
        int newh = ((bar_height + 1) * GraphicVisualiserPainter.ROWS) + 1;

        mPainter.onSizeChanged(neww, newh, oldw, oldh);
        mEqualizer.onSizeChanged(neww, newh, oldw, oldh, getHeight());
        mHolder.setFixedSize(neww, newh);
    }

    /**
     * Set current excitations values. Thread-safe.
     * 
     * @param excitations
     *            updated excitations
     */
    public void setExcitations(final float[] excitations) {
        this.mPainter.setExcitations(excitations);
    }

    public void onVisualizerUpdate(final float[] gains) {
        // Log.d(Tag.MAIN, "onVisualizerUpdate: " +
        // Tools.floatArrayToString(gains));
        System.arraycopy(gains, 0, this.mGainsUi, 0, gains.length);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEnableEditTouch) {
            return mEqualizer.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void drawableStateChanged() {
        Log.d(Tag.MAIN, "GraphicVisualiser.drawableStateChanged"+" mSufraceCreated:"+mSufraceCreated);
        super.drawableStateChanged();
        //DS1SOC-468 do not repaint it before the surface view is created 
        if(mSufraceCreated == false)
        	return;
        setEnabled(ViewTools.testDrawableState(getDrawableState(), ENABLED_STATE_SET));
        mPainter.setEnabled(isEnabled());
        mEqualizer.setEnabled(isEnabled());
        repaint(true);
    }

    public void repaint() {
        repaint(false);
    }

    public void repaint(boolean force) {
        // Log.d(Tag.MAIN, "GraphicVisualiser.repaint");
        // run in Main UI Thread.
        canvasPaint();
    }

    private void canvasPaint() {
        if (!mSufraceCreated || !mFragmentIsActive) {
            return;
        }

        Canvas c = null;
        try { 
            c = mHolder.lockCanvas();
	        if (c != null) {
		        mPainter.onDraw(c);
		        mEqualizer.onDraw(c);
	        }
        }catch (Exception e) {
            Log.d(Tag.MAIN, e.getMessage());  
        } finally {
            if (c != null) {
                mHolder.unlockCanvasAndPost(c);
            }
        }

        if ((mEqualizer != null) && (mEqualizer.isAnimating())) {
            // equlizerAnimatingRunnable runs in Main UI Thread.
            DS1Application.HANDLER.removeCallbacks(equlizerAnimatingRunnable);
            DS1Application.HANDLER.postDelayed(equlizerAnimatingRunnable, 30);
        }
    }

    private final Runnable equlizerAnimatingRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            canvasPaint();
        }
    };
    
    //DS1SOC-468 set the fragment active or not
    public void setActiveStatus(final boolean active){
    	mFragmentIsActive = active;
    }

	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d(Tag.MAIN, "GraphicVisualiser.surfaceChanged " + width + " x " + height + " format: " + format);
        //DS1SOC-468 do not repaint it before the surface view is created        
        if(mSufraceCreated == true){
            repaint(true);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(Tag.MAIN, "GraphicVisualiser.surfaceCreated");
        mSufraceCreated = true;
        repaint(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Tag.MAIN, "GraphicVisualiser.surfaceDestroyed");
        DS1Application.HANDLER.removeCallbacks(equlizerAnimatingRunnable);
        mSufraceCreated = false;
    }

    boolean isSurfaceCreated() {
        return mSufraceCreated;
    }

    public void setSuspended(boolean suspended) {
        this.mSuspended = suspended;
        mEqualizer.readUserGainsFromEngine();
    }
}
