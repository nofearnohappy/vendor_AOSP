/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cmcc.barcode.lib.iot.barcode.decode;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import com.mediatek.rcs.contacts.R;

import java.io.Closeable;
import java.io.IOException;

/**
 * Manages beeps and vibrations for {@link CaptureActivity}.
 */
final class BeepManager implements
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, Closeable {

    private static final String TAG = BeepManager.class.getSimpleName();

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;

    private final Activity mActivity;
    private MediaPlayer mMediaPlayer;
    private boolean mPlayBeep;

    BeepManager(Activity activity) {
        this.mActivity = activity;
        this.mMediaPlayer = null;
        update();
    }

    synchronized void update() {
        mPlayBeep = shouldBeep(mActivity);
        if (mPlayBeep && mMediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = buildMediaPlayer(mActivity);
        }
    }

    synchronized void playBeepSoundAndVibrate(boolean isVibrate) {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (isVibrate) {
            Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private static boolean shouldBeep(Context activity) {
        boolean shouldPlayBeep = true;
        // See if sound settings overrides this
        AudioManager audioService =
                (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            shouldPlayBeep = false;
        }
        return shouldPlayBeep;
    }

    private MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        try {
            AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mMediaPlayer.prepare();
            return mMediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, "buildMediaPlayer");
            mMediaPlayer.release();
            return null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0);
    }

    @Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required and finish
            Log.w(TAG, "onError");
            mActivity.finish();
        } else {
            // possibly media player error, so release and recreate
            mp.release();
            mMediaPlayer = null;
            update();
        }
        return true;
    }

    @Override
    public synchronized void close() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}
