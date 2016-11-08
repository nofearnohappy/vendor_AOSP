package com.mediatek.rcs.message.utils;

import java.util.ArrayList;

import android.util.Log;

public class BackgroundLoader {
    private final String TAG = "BackgroundLoader";
    boolean mThreadOver = false;
    Thread mWorkerThread;
    private final ArrayList<Runnable> mThingsToLoad;

//    private static BackgroundLoader sInstance;

    public BackgroundLoader() {
        mThingsToLoad = new ArrayList<Runnable>();
        mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (!mThreadOver) {
                    Runnable r = null;
                    synchronized (mThingsToLoad) {
                        if (mThingsToLoad.size() == 0) {
                            try {
                                mThingsToLoad.wait();
                            } catch (InterruptedException ex) {
                                Log.w(TAG, ex.getMessage());
                            }
                        }
                        if (mThingsToLoad.size() > 0) {
                            r = mThingsToLoad.remove(0);
                        }
                    }
                    if (r != null) {
                        r.run();
                    }
                }
            }
        });
        mWorkerThread.start();
    }

//    public static void initialize(Context context)  {
//        if (sInstance == null) {
//            synchronized (BackgroundLoader.class) {
//                sInstance = new BackgroundLoader();
//            }
//        }
//    }

    public void pushTask(Runnable task) {
        synchronized (mThingsToLoad) {
            if (!mThingsToLoad.contains(task)) {
                mThingsToLoad.add(task);
                mThingsToLoad.notify();
            }
        }
    }

    public void cacelTask(Runnable task) {
        synchronized (mThingsToLoad) {
            mThingsToLoad.remove(task);
        }
    }

    public void destroy() {
        synchronized (mThingsToLoad) {
            mThreadOver = true;
            mThingsToLoad.clear();
            mThingsToLoad.notify();
        }
    }
}
