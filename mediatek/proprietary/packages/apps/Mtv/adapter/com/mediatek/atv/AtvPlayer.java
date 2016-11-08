/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.atv;


import java.io.IOException;

import com.mediatek.mtvbase.Player;

import android.media.MediaPlayer;
import android.util.Log;
import android.media.AudioManager;
import android.view.Surface;

public class AtvPlayer implements Player, MediaPlayer.OnErrorListener {

    private static final String TAG = "ATV/AtvPlayer";
    private AtvService mService;
    private static AtvService.EventCallback mEventCallback;
    private int mState;
    private MediaPlayer mMediaPlayer;
    private boolean mMuted;

    public class RetriableException extends RuntimeException {
        private static final long serialVersionUID = -4453804913819820712L;

        public RetriableException() {
        }

        public RetriableException(Throwable t) {
            super(t);
        }
    }

    public AtvPlayer(AtvService.EventCallback e, AtvService s) {
        mEventCallback = e;
        mService = s;
        mMediaPlayer = new MediaPlayer();
    }

    public void open(int width, int height) throws IOException, IllegalArgumentException,
            IllegalStateException, SecurityException {
        Log.d("@M_" + TAG, "open mState = " + mState);
        if (mState == MTV_PLAYER_END || mState == MTV_PLAYER_STOPPED) {
            if (mState == MTV_PLAYER_END) {
                mService.openVideo(true);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mMediaPlayer.setDataSource("MEDIATEK://MEDIAPLAYER_PLAYERTYPE_MATV");
                    mState = MTV_PLAYER_OPENED;
                } catch (IOException e) {
                    Log.e("@M_" + TAG, "open() fail:" + e);
                    mService.closeVideo(true);
                    throw new RetriableException();
                } catch (IllegalStateException e) {
                    Log.e("@M_" + TAG, "open() fail:" + e);
                    mService.closeVideo(true);
                    throw e;
                } catch (IllegalArgumentException e) {
                    Log.e("@M_" + TAG, "open() fail:" + e);
                    mService.closeVideo(true);
                    throw e;
                } catch (SecurityException e) {
                    Log.e("@M_" + TAG, "open() fail:" + e);
                    mService.closeVideo(true);
                    throw e;
                }
            } else {
                mService.openVideo(false); // true means reconnect.
            }
            mMediaPlayer.prepare();
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mState = MTV_PLAYER_PREPARED;
        } else {
            release();
            throw new IllegalStateException();
        }
    }

    /*Releases resources associated with this MtvPlayer object.*/
    public void  release() throws IllegalStateException {
        Log.d("@M_" + TAG, "release mState = " + mState);
        if (mState != MTV_PLAYER_END) {
            mMediaPlayer.release();
            mService.closeVideo(true); //true means disconnect camera
            mMediaPlayer = null;
            mService = null;
            mState = MTV_PLAYER_END;
        }
    }

    /*Starts or resumes playback.*/
    public void  start(Surface s)throws IOException, IllegalStateException {
    Log.d("@M_" + TAG, "start mState = " + mState);
        if (mState == MTV_PLAYER_PREPARED) {
            mService.setSurface(s);
            if (!mMuted) {
                mMediaPlayer.start();
            }
            mState = MTV_PLAYER_STARTED;
        } else {
            release();
            throw new IllegalStateException();
        }
    }

    /*Stops playback after playback has been stopped or paused.*/
    public void stop() throws IllegalStateException {
        Log.d("@M_" + TAG, "stop mState = " + mState);

        if (mState == MTV_PLAYER_STARTED || mState == MTV_PLAYER_PREPARED) {
            if (!mMuted) {
                mMediaPlayer.stop();
            }
            mService.closeVideo(false); //false means keep camera connected.
            mState = MTV_PLAYER_STOPPED;
        } else {
            release();
            throw new IllegalStateException();
        }
    }

    /*configure video parameters.*/
    public void  configVideo(byte item, int val) {
        Log.d("@M_" + TAG, "configVideo item = " + item + " val = " + val);
        mService.adjustSetting(item, val);
    }

    //mute by pausing player.
    public void setMute(boolean mute) {

    Log.d("@M_" + TAG, "setMute mMuted = " + mMuted + " mute = " + mute);
        if (mMuted != mute) {
            if (mute) {
                if (mState == MTV_PLAYER_STARTED) {
                    mMediaPlayer.pause();
                }
                //we always set muted = ture in case mediaplayer is not started at this time it will also mute when it is started.
                mMuted = true;
            } else {
                if (mState == MTV_PLAYER_STARTED) {
                    mMediaPlayer.start();
                }
                mMuted = false;
            }
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mState != MTV_PLAYER_END) {
            mService.closeVideo(true);
            //mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mService = null;
            mState = MTV_PLAYER_END;
            mEventCallback.callOnEvent(what | 0xf0003000, extra, 0, mp);
        } else {
            throw new IllegalStateException();
        }
        return true;
    }

}
