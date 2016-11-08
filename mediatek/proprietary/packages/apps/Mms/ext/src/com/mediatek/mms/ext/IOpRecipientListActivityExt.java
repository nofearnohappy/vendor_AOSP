package com.mediatek.mms.ext;

import android.app.Activity;
import android.os.Bundle;



public interface IOpRecipientListActivityExt {

    /**
     * @internal
     */
    boolean onOptionsItemSelected(boolean isSetting);

    /**
     *
     * @param activity Activity
     * @param savedInstanceState Bundle
     * @return boolean
     * @internal
     */
    void onCreate(Activity activity, Bundle savedInstanceState);
}
