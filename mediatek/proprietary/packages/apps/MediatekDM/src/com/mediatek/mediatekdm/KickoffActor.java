package com.mediatek.mediatekdm;

import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class KickoffActor implements Runnable {
    protected final Context mContext;
    private static ExecutorService sExecutorService;

    public KickoffActor(Context context) {
        mContext = context;
    }

    public static ExecutorService getExecutorService() {
        synchronized (KickoffActor.class) {
            if (sExecutorService == null) {
                sExecutorService = Executors.newSingleThreadExecutor();
            }
            return sExecutorService;
        }
    }

    /**
     * Schedule to execute one by one.
     *
     * @param checker
     */
    public static void kickoff(KickoffActor checker) {
        Log.d(TAG.APPLICATION, "kickoff " + checker);
        getExecutorService().execute(checker);
    }
}
