/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetoothle.fmp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.mediatek.bluetoothle.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Provides a default alert behavior when FMP server is alerted by client.
 * The default alert behavior is vibrate with beep-beep-beep alarm for 3 seconds.
 */
public class DefaultAlerter implements IAlerter {
    private static final String TAG = "DefaultAlerter";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final int VIBRATE_DURATION = 3000;
    private static final int ALERT_DURATION = 3000;
    private static final float ALERT_VOLUMN = 1.0f;

    private Context mCtx;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private AudioManager mAm;
    private int mUserVolume;
    private int mMaxVolume;

    DefaultAlerter(final Context ctx) {
        if (VDBG) Log.v(TAG, "DefaultAlerter");
        mCtx = ctx;

        // Init AudioManager
        mAm = (AudioManager) mCtx.getSystemService(Context.AUDIO_SERVICE);
        mUserVolume = mAm.getStreamVolume(AudioManager.STREAM_MUSIC);
        mMaxVolume = mAm.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // Init Vibrator
        mVibrator = (Vibrator) mCtx.getSystemService(Context.VIBRATOR_SERVICE);

        // Init MediaPlayer
        mMediaPlayer = new MediaPlayer();

        // Init Timer
        mTimer = new Timer("DefaultAlerter Timer");

        if (VDBG) Log.v(TAG, "mMediaPlayer = " + mMediaPlayer + ", mTimer = " + mTimer);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(final MediaPlayer player) {
                player.seekTo(0);
            }
        });
        if (null != mMediaPlayer) {
            mMediaPlayer.setVolume(ALERT_VOLUMN, ALERT_VOLUMN);
            mMediaPlayer.setLooping(true);
            try {
                setDataSourceFromResource(mCtx, mMediaPlayer, R.raw.beep_beep_beep_alarm);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            } catch (final SecurityException e) {
                e.printStackTrace();
            } catch (final IllegalStateException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mMediaPlayer is null!");
        }
    }

    private static void setDataSourceFromResource(Context context, MediaPlayer player, int res)
            throws IOException {
        if (VDBG) Log.v(TAG, "setDataSourceFromResource");
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    private void setTimerTask() {
        if (VDBG) Log.v(TAG, "setTimerTask");
        /// M: ALPS01898391: Add null pointer checking @{
        if ((null == mTimer) || (null == mMediaPlayer)) {
            Log.e(TAG, "mTimer = " + mTimer + ", mMediaPlayer = " + mMediaPlayer + ", Return!");
            return;
        }
        /// @}
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "TimeTask timeout, stop MediaPlayer");
                mAm.setStreamVolume(AudioManager.STREAM_MUSIC, mUserVolume, 0);
                mMediaPlayer.stop();
            }
        };
        mTimer.schedule(mTimerTask, ALERT_DURATION);
    }

    private boolean ring(int level) {
        if (VDBG) Log.v(TAG, "ring: level = " + level);
        if (null == mMediaPlayer) {
            Log.e(TAG, "mMediaPlayer is null!");
            return false;
        }
        switch (level) {
            case FmpServerService.LEVEL_HIGH:
            case FmpServerService.LEVEL_MILD:
                if (VDBG) Log.v(TAG, "Start ringing");
                if (DBG) Log.d(TAG, "mMediaPlayer.isPlaying() = " + mMediaPlayer.isPlaying());
                if (mMediaPlayer.isPlaying()) {
                    stopAlert();
                }

                try {
                    mMediaPlayer.prepare();
                } catch (final IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                } catch (final IOException e) {
                    e.printStackTrace();
                    return false;
                }
                // Request audio focus
                final int result = mAm.requestAudioFocus(mAfChangeListener,
                        AudioManager.STREAM_MUSIC,
                        // Request permanent focus.
                        AudioManager.AUDIOFOCUS_GAIN);
                if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED != result) {
                    Log.e(TAG, "Requset Audio Focus fail, result = " + result);
                    return false;
                }
                mAm.setStreamVolume(AudioManager.STREAM_MUSIC, mMaxVolume - 2, 0);
                mMediaPlayer.start();
                if (null == mTimer) {
                    Log.e(TAG, "mTimer = nul!");
                    return false;
                }
                setTimerTask();
                break;
            case FmpServerService.LEVEL_NO:
                if (VDBG) Log.v(TAG, "Stop ringing");
                if (null != mTimerTask) {
                    mTimerTask.cancel();
                } else {
                    if (DBG) Log.d(TAG, "mTimerTask is null!");
                    break;
                }
                mAm.setStreamVolume(AudioManager.STREAM_MUSIC, mUserVolume, 0);
                mMediaPlayer.stop();
                mAm.abandonAudioFocus(mAfChangeListener);
                break;
            default:
                Log.e(TAG, "Invalid level");
                break;
        }
        return true;
    }

    private boolean vibrate(boolean isVibrate) {
        if (VDBG) Log.v(TAG, "vibrate: isVibrate = " + isVibrate);
        if (null == mVibrator) {
            Log.e(TAG, "mVibrator is null!");
            return false;
        }
        if (isVibrate) {
            if (VDBG) Log.v(TAG, "Start vibration");
            mVibrator.vibrate(VIBRATE_DURATION);
        } else {
            if (VDBG) Log.v(TAG, "Stop Vibration");
            mVibrator.cancel();
        }
        return true;
    }

    private boolean startAlert(int level) {
        if (VDBG) Log.v(TAG, "startAlert: level = " + level);
        boolean vRet = vibrate(true);
        boolean rRet = ring(level);
        return (vRet && rRet);
    }

    private boolean stopAlert() {
        if (VDBG) Log.v(TAG, "stopAlert");
        boolean vRet = vibrate(false);
        boolean rRet = ring(FmpServerService.LEVEL_NO);
        return (vRet && rRet);
    }

    private final OnAudioFocusChangeListener mAfChangeListener = new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (DBG) Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                stopAlert();
                mAm.abandonAudioFocus(mAfChangeListener);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (DBG) Log.d(TAG, "AUDIOFOCUS_GAIN");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (DBG) Log.d(TAG, "AUDIOFOCUS_LOSS");
                stopAlert();
                mAm.abandonAudioFocus(mAfChangeListener);
            }
        }
    };

    @Override
    public final boolean alert(final int level) {
        if (DBG) Log.d(TAG, "alert: level = " + level);
        boolean ret = false;
        switch (level) {
            case FmpServerService.LEVEL_HIGH:
            case FmpServerService.LEVEL_MILD:
                ret = startAlert(level);
                break;
            case FmpServerService.LEVEL_NO:
                ret = stopAlert();
                break;
            default:
                Log.e(TAG, "Invalid level");
                break;
        }
        return ret;
    }

    @Override
    public final boolean uninit() {
        stopAlert();
        /// M: ALPS01898391: Add null pointer checking
        if (null != mTimer) {
            mTimer.cancel();
        }
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
        }
        return true;
    }
}
