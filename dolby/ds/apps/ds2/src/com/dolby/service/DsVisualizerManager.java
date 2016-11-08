/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013 - 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * DsVisualizerManager.java
 *
 * 
 */
package com.dolby.service;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import com.dolby.ds.DsManager;
import com.dolby.api.DsCommon;
import com.dolby.api.DsConstants;
import com.dolby.api.DsLog;

public class DsVisualizerManager
{
    private static final String TAG = "DsVisualizerManager";

    /**
     * The lock for protecting the DsVisualizerManager context to be thread safe.
     *
     * @internal
     */
    private static final Object lock_ = new Object();

    /**
     * The list to store the client who is interested in the visualizer data.
     *
     * @internal
     */
    private ArrayList<Integer> visualizerList_ = null;

    /**
     * The handler for the visualizer data.
     *
     * @internal
     */
    private Handler visualizerHandler_;

    /**
     * The handler thread for the visualizer data.
     *
     * @internal
     */
    private HandlerThread visualizerThread_;

    /**
     * The period of generating visualizer data, in millisecond.
     *
     * @internal
     */
    private static final int VISUALIZER_UPDATE_TIME = 50;

    /**
     * The gains of visualizer data.
     *
     * @internal
     */
    private float[] gains_ = null;

    /**
     * The excitations of visualizer data.
     *
     * @internal
     */
    private float[] excitations_ = null;

    /**
     * The flag whether the visualizer data is suspended.
     *
     * @internal
     */
    private boolean isVisualizerSuspended_ = false;

    /**
     * The counter which is used to decide whether to change the suspended status.
     *
     * @internal
     */
    private int noVisualizerCounter_ = 0;

    /**
     * The size of the visualizer date retrieved in previous update period.
     *
     * @internal
     */
    private int previousVisualizerSize_ = 0;

    /**
     * The threshold of the counter.
     *
     * @internal
     */
    private static final int COUNTER_THRESHOLD = 500 / VISUALIZER_UPDATE_TIME;

    /**
     * The band number.
     *
     * @internal
     */
    private int geqBandCount_ = 20;
 
    /**
     * The instance of DsManager.
     *
     * @internal
     */
    private DsManager dsManager_ = null;

    /**
     * The instance of the callback manager.
     *
     * @internal
     */
    private DsCallbackManager cbkManager_ = null;

    /**
     * The int value for suspended status.
     *
     * @internal
     */
    private static final int VIS_SUSPENDED = 1;
    private static final int VIS_NOT_SUSPENDED = 0;

    /**
     * The constructor.
     *
     */
    public DsVisualizerManager(DsManager ds, DsCallbackManager cbk)
    {
        synchronized (lock_)
        {
            dsManager_ = ds;
            cbkManager_ = cbk;
            visualizerList_ = new ArrayList<Integer>();
            gains_ = new float[geqBandCount_];
            excitations_ = new float[geqBandCount_];
        }
    }

    /**
     * The method release the resource.
     *
     */
    public void release()
    {
        synchronized (lock_)
        {
            // cleanup the visualizer list
            if (visualizerList_ != null)
            {
                int size = visualizerList_.size();
                for(int i=0; i<size; i++)
                {
                    visualizerList_.remove(i);
                }
                visualizerList_ = null;
            }
            gains_ = null;
            excitations_ = null;
            dsManager_ = null;
            cbkManager_ = null;
        }
    }

    /**
     * The method to register the visualizer data.
     *
     */
    public void register(int handle)
    {
        synchronized (lock_)
        {
            if (visualizerList_ != null)
            {
                int size = visualizerList_.size();
                if (size == 0)
                {
                    // The fisrt visualizer client is registering, enable the visualizer.
                    startVisualizer();
                }
                visualizerList_.add(new Integer(handle));
                // Notify the newly-registered client that visualizer is already suspended
                if (isVisualizerSuspended_)
                {
                    invokeVisualizerCbk(DsCommon.VISUALIZER_SUSPENDED_MSG, 0, VIS_SUSPENDED, 0, null, null);
                }
                DsLog.log1(TAG, "Add a visualzier handle "+ handle);
            }
        }
    }

    /**
     * The method to unregister the visualizer data.
     *
     */
    public void unregister(int handle)
    {
        synchronized (lock_)
        {
            if (visualizerList_ != null)
            {
                int size = visualizerList_.size();
                if (size == 0)
                {
                    DsLog.log1(TAG, "No client registering, do nothing.");
                    return;
                }
                for (int i=0; i<size; i++)
                {
                    if (handle == (Integer)visualizerList_.get(i).intValue())
                    {
                        visualizerList_.remove(i);
                        DsLog.log1(TAG, "remove a visualzier handle "+ handle);
                        int newSize = visualizerList_.size();
                        if(newSize == 0)
                        {
                            // The last visualizer client is unregistering, disable the visualizer.
                            stopVisualizer();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * The callback of the visualizer thread.
     *
     * @internal
     */
    private final Runnable cbkOnVisualizerUpdate_ = new Runnable()
    {
        public void run()
        {
            visualizerUpdate();
        }
    };

    /**
     * The method runs in the visualizer thread.
     *
     * @internal
     */
    private void visualizerUpdate()
    {
        synchronized (lock_)
        {
            int len = 0;
            try
            {
                len = dsManager_.getVisualizerData(gains_, excitations_);
                if (len != previousVisualizerSize_)
                {
                    noVisualizerCounter_ = 0;
                }
                previousVisualizerSize_ = len;
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception in visualizerUpdate");
                e.printStackTrace();
            }
            if (len == 0)
            {
                // no audio is processing
                if (!isVisualizerSuspended_)
                {
                    // increase the counter
                    noVisualizerCounter_++;
                    if (noVisualizerCounter_ >= COUNTER_THRESHOLD)
                    {
                        // call onVisualizerSuspended with true
                        isVisualizerSuspended_ = true;
                        noVisualizerCounter_ = 0;
                        DsLog.log1(TAG, "VISUALIZER_SUSPENDED true");
                        invokeVisualizerCbk(DsCommon.VISUALIZER_SUSPENDED_MSG, 0, VIS_SUSPENDED, 0, null, null);
                    }
                }
                //Still in suspend mode, do nothing
            }
            else
            {
                // processing audio
                if (isVisualizerSuspended_)
                {
                     // increase the counter
                    noVisualizerCounter_++;
                    if (noVisualizerCounter_ >= COUNTER_THRESHOLD)
                    {
                        // call onVisualizerSuspended with false
                        isVisualizerSuspended_ = false;
                        noVisualizerCounter_ = 0;
                        DsLog.log1(TAG, "VISUALIZER_SUSPENDED false");
                        invokeVisualizerCbk(DsCommon.VISUALIZER_SUSPENDED_MSG, 0, VIS_NOT_SUSPENDED, 0, null, null);
                    }
                }
                else
                {
                    // To avoid the last timer changes the gains and excitations form all zero to other values
                    // when the ds is turned off.
                    try
                    {
                        if (dsManager_.getDsOn() != DsConstants.DS_STATE_ON)
                        {
                            for(int i = 0; i < geqBandCount_; i++)
                            {
                                gains_[i] = 0.0f;
                                excitations_[i] = 0.0f;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                         Log.e(TAG, "Exception found in visualizerUpdate");
                         e.printStackTrace();
                    }
                    //Still in working mode, call onVisualizerUpdated
                    invokeVisualizerCbk(DsCommon.VISUALIZER_UPDATED_MSG, 0, 0, 0, gains_, excitations_);
                }
            }

            if (visualizerHandler_ != null)
            {
                visualizerHandler_.removeCallbacks(cbkOnVisualizerUpdate_);
                visualizerHandler_.postDelayed(cbkOnVisualizerUpdate_, VISUALIZER_UPDATE_TIME);
            }
        }
    }

    /**
     * The method starts visualizer data updating.
     *
     * @internal
     */
    private void startVisualizer()
    {
        synchronized (lock_)
        {
            try
            {
                if (dsManager_.getDsOn() == DsConstants.DS_STATE_ON)
                {
                    dsManager_.setVisualizerOn(true);

                    if (visualizerThread_ == null)
                    {
                        visualizerThread_ = new HandlerThread("visualiser thread");
                        visualizerThread_.start();
                    }

                    if (visualizerHandler_ == null)
                    {
                        visualizerHandler_ = new Handler(visualizerThread_.getLooper());
                    }
                    visualizerHandler_.post(cbkOnVisualizerUpdate_);
                    DsLog.log1(TAG, "Visualizer thread is started.");
                }
                else
                {
                    DsLog.log1(TAG, "DS is off, will start visualizer thread when it switches to on.");
                }
            }
            catch (Exception e)
            {
                 Log.e(TAG, "Exception found in startVisualizer");
                 e.printStackTrace();
            }
        }
    }

    /**
     * The method stops visualizer data updating.
     *
     * @internal
     */
    private void stopVisualizer()
    {
        synchronized (lock_)
        {
            // When this method is called, both on/off status is valid.
            // So we don't need to check the status of the effect, just remove the thread.
            try
            {
                dsManager_.setVisualizerOn(false);

                if (visualizerHandler_ != null)
                {
                    visualizerHandler_.getLooper().quit();
                    visualizerHandler_ = null;
                    visualizerThread_ = null;
                }
            }
            catch (Exception e)
            {
                 Log.e(TAG, "Exception found in stopVisualizer");
                 e.printStackTrace();
            }
            // Set gains and excitations to zero
            for(int i = 0; i < geqBandCount_; i++)
            {
                gains_[i] = 0.0f;
                excitations_[i] = 0.0f;
            }
            noVisualizerCounter_ = 0;
        }
    }

    /**
     * The method toggle the on/off status of visualizer.
     * 
     */
    public void toggleVisualizer(boolean on)
    {
        synchronized (lock_)
        {
            if (visualizerList_ != null)
            {
                // Update the visualizer state
                int size = visualizerList_.size();
                if (size > 0)
                {
                    if (on)
                    {
                        // Turn on the visualizer if necessary
                        startVisualizer();
                    }
                    else
                    {
                        // Turn off the visualizer if necessary
                        stopVisualizer();
                        // In stopVisualizer gains and excitations are already set to zeros
                        // Send the last all zeros message
                        invokeVisualizerCbk(DsCommon.VISUALIZER_UPDATED_MSG, 0, 0, 0, gains_, excitations_);
                    }
                }
            }
        }
    }

    /**
     * The method invoke the visualzier callback.
     *
     * @internal
     */
    private void invokeVisualizerCbk(int what, int handle, int arg1, int arg2, Object obj1, Object obj2)
    {
        synchronized (lock_)
        {
            if (visualizerList_ != null)
            {
                try
                {
                    for (Integer i : visualizerList_)
                    {
                        int vis_handle = i.intValue();
                        cbkManager_.invokeCallback(what, vis_handle, arg1, arg2, obj1, obj2);
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Exception found in invokeVisualizerCbk");
                    e.printStackTrace();
                }
            }
        }
    }
}
