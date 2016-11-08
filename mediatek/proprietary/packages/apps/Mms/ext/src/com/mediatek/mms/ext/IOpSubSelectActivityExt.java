package com.mediatek.mms.ext;

import android.app.Activity;

public interface IOpSubSelectActivityExt {
    /**
     * @internal
     */
    void onCreate(Activity hostActivity);
    /**
     * @internal
     */
    boolean onListItemClick(Activity hostActivity, final int subId);
    /**
     * @internal
     */
    String [] setSaveLocation();
    boolean isSimSupported(int subId);
}
