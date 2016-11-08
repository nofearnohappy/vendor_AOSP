
package com.mediatek.cmas.ext;

import android.preference.PreferenceActivity;

public interface ICmasMainSettingsExt {
    
    /**
     * Get Alert Volume value
     * @internal
     */
    public float getAlertVolume(int msgId);

    /**
     * Get Alert Vibration value
     * @internal
     */
    public boolean getAlertVibration(int msgId);

    /**
     * Set Alert Volume and Vibration value
     * @internal
     */
    public boolean setAlertVolumeVibrate(int msgId, boolean currentValue);

    /**
     * Stop mediplayer when press Home key
     * @internal
     */
    public void stopMediaPlayer();

    /**
     * Add Alert Volume and Vibration in Main Setting
     * @internal
     */
    public void addAlertSoundVolumeAndVibration(PreferenceActivity prefActivity);
}
