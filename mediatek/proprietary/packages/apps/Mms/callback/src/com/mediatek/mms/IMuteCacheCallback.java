package com.mediatek.mms.callback;

import android.content.Context;

public interface IMuteCacheCallback {
    void setMuteCacheCallback(long lthreadId, long lthreadMute, long lthreadMuteStart,
            boolean lthreadNotificationEnabled);

    void initCallback(Context context);
}
