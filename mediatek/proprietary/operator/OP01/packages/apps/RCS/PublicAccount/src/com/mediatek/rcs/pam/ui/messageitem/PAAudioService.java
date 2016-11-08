package com.mediatek.rcs.pam.ui.messageitem;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.mediatek.rcs.pam.Constants;

import android.media.MediaPlayer;
import android.util.Log;

public class PAAudioService {

    private static final String TAG = "PA/PAAudioPlayer";

    private static PAAudioService mAudioService = null;
    private MediaPlayer mMediaPlayer;

    // Passed by caller
    private long mId = Constants.INVALID;
    private IAudioServiceCallBack mCallback;
    private String mAudioPath;

    // timer for onPlayProgress callback
    class AudioTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mPlayerState == mPlayState && mCallback != null) {
                int currentPos = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                int persentage = (int) Math.round(((double) currentPos) * 100
                        / duration);
                mCallback.onPlayProgress(persentage, currentPos);
            }
        }
    };

    private AudioTimerTask sTimerTask;
    private Timer sTimer;

    interface IAudioServiceCallBack {

        void onError(int what, int extra);

        void onCompletion();

        void onPlayProgress(int persentage, int currentTime);
    }

    private void stopTimer() {
        if (sTimer != null) {
            sTimerTask.cancel();
            sTimerTask = null;
            sTimer.cancel();
            sTimer = null;
        }
    }

    private void startTimer() {
        if (sTimer == null) {
            sTimerTask = new AudioTimerTask();
            sTimer = new Timer(true);
            sTimer.schedule(sTimerTask, 0, 500);
        }
    }

    class ErrorLisener implements MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            stopTimer();
            stateChange(mIdleState);

            if (mCallback != null) {
                mCallback.onError(what, extra);
            }
            // cause media player send onCompletion callback.

            return false;
        }
    }

    class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            stopTimer();
            stateChange(mIdleState);

            if (mCallback != null) {
                mCallback.onCompletion();
            }

        }
    }

    private PAAudioService() {

        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setOnErrorListener(new ErrorLisener());

        mMediaPlayer.setOnCompletionListener(new CompletionListener());

        stateChange(mIdleState);
    }

    public static PAAudioService getService() {
        Log.e(TAG, "getService()");
        if (mAudioService == null) {
            synchronized (PAAudioService.class) {
                if (mAudioService == null) {
                    mAudioService = new PAAudioService();
                }
            }

        }
        return mAudioService;
    }

    private void resetService() {
        Log.e(TAG, "reset()");
        stopTimer();
        mMediaPlayer.reset();
        stateChange(mIdleState);
        mId = Constants.INVALID;
        mAudioPath = null;
        mCallback = null;
    }

    public void releaseService() {
        Log.e(TAG, "release()");
        // do nothing because we want mMediaPlayer all the app life
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public boolean bindAudio(long id, String path,
            IAudioServiceCallBack callback) {
        Log.d(TAG, "bindAudioFile(). id=" + id + ". path=" + path + ". state="
                + mPlayerState);

        return mPlayerState.bindAudio(id, path, callback);
    }

    public boolean unBindAudio(long id) {
        Log.d(TAG, "unBindAudio(). mId=" + id);
        if (mId == id) {
            stopTimer();
            mCallback = null;
            return true;
        }
        Log.d(TAG, "unBindAudio() fail. current mId =" + mId);
        return false;
    }

    public boolean touch(long id, String path, IAudioServiceCallBack callback) {
        Log.d(TAG, "PlayAudio()  current state = " + mPlayerState);

        return mPlayerState.touch(id, path, callback);
    }

    public void stopAudio() {
        Log.d(TAG, "pauseAudio()  current state = " + mPlayerState);
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            if (mCallback != null) {
                mCallback.onCompletion();
            }
            resetService();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private boolean stateChange(AudioState newState) {
        if (mPlayerState != newState) {
            Log.d(TAG, "stateChange from " + mPlayerState + "->" + newState);
            mPlayerState = newState;
            return true;
        } else {
            Log.d(TAG, "stateChange no change:" + mPlayerState);
            return false;
        }
    }

    private abstract class AudioState {
        public boolean bindAudio(long id, String path,
                IAudioServiceCallBack callback) {
            return check(id, path);
        }

        public boolean touch(long id, String path,
                IAudioServiceCallBack callback) {
            return false;
        }

        protected boolean check(long id, String path) {
            if (path == null || path.isEmpty()) {
                Log.d(TAG, "bindAudioFile() failed for path is null.");
                return false;
            }

            File file = new File(path);
            if (!file.exists()) {
                Log.d(TAG, "bindAudioFile() fail for path not exist.");
                return false;
            }

            if (mId != Constants.INVALID && mPlayerState != mIdleState
                    && mId == id && path.equals(mAudioPath)) {
                return true;
            }
            return false;
        }

        protected boolean playNew(long id, String path, IAudioServiceCallBack callback) {
            try {
                stopAudio();
                mId = id;
                mAudioPath = path;
                mCallback = callback;
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.prepare();
                stateChange(mPlayState);
                mMediaPlayer.start();
                startTimer();
            } catch (IllegalArgumentException | SecurityException |
                    IllegalStateException | IOException e) {
                e.printStackTrace();
                //prepare audio fail, reset Service
                resetService();
                return false;
            }
            return true;
        }
    }

    AudioState mIdleState = new AudioState() {
        @Override
        public boolean touch(long id, String path,
                IAudioServiceCallBack callback) {
            return playNew(id, path, callback);
        }
    };

    AudioState mPlayState = new AudioState() {
        @Override
        public boolean bindAudio(long id, String path,
                IAudioServiceCallBack callback) {
            if (check(id, path)) {
                mCallback = callback;
                startTimer();
                Log.d(TAG, "bindAudioFile() already bind, return directly");
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean touch(long id, String path,
                IAudioServiceCallBack callback) {
            if (check(id, path)) {
                stopTimer();
                try {
                    mMediaPlayer.pause();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                }

                stateChange(mPauseState);
                return true;
            } else {
                return playNew(id, path, callback);
            }
        }
    };

    AudioState mPauseState = new AudioState() {
        @Override
        public boolean bindAudio(long id, String path,
                IAudioServiceCallBack callback) {
            if (check(id, path)) {
                mCallback = callback;
                Log.d(TAG, "bindAudioFile() already bind, return directly");
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean touch(long id, String path,
                IAudioServiceCallBack callback) {
            if (check(id, path)) {
                try {
                    mMediaPlayer.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                }
                stateChange(mPlayState);
                startTimer();
                return true;
            } else {
                return playNew(id, path, callback);
            }
        }
    };

    private AudioState mPlayerState = mIdleState;
}
