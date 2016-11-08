package com.mediatek.engineermode.voicesettings;

import android.media.MediaRecorder;

import com.mediatek.engineermode.Elog;

import java.io.IOException;

/**
 * EmRecorder is a simple wrapper class of MediaRecorder.
 * which simplify the state of MediaRecorder to start, stop and release state
 * @author mtk81238
 *
 */
public class EmRecorder implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final int AUDIO_SOURCE_VOICE_UNLOCK = 80;
    public static final int CHANNEL_STERO = 2;
    public static final int SAMPLING_RATE_48K = 48000;
    public static final int STATE_INITIAL = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_RELEASED = 3;
    public static final int ERROR_PREPARE = -11;
    public static final String FILE_POSTFIX_WAV = ".wav";
    private static final String TAG = "voice/EmRecorder";

    private MediaRecorder mRecorder = null;
    private Parameters mParams = null;
    private int mState;
    private String mLastOutputFile = null;
    private MediaRecorder.OnErrorListener mExtraErrorListener = null;

    /**
     * Parameters is a assistance class to set recorder parameters.
     * @author mtk81238
     *
     */
    public static class Parameters {
        public int audioSource; // AUDIO_SOURCE_VOICE_UNLOCK = 80
        public int outputFormat; // OUTPUT_FORMAT_WAV
        public int audioEncoder; // PCM
        public int encodingBitRate;
        public int samplingRate; // 48k
        public int channel; // stero = 2
    }

    /**
     * get recorder state.
     * @return recorder state
     */
    public int getState() {
        return mState;
    }

    /**
     * set recorder parameter.
     * @param params the parameter passed to recorder
     */
    public void setRecorderParameters(Parameters params) {
        mParams = params;
    }

    /**
     * set error listener to handle error when error happen on recorder.
     * @param errorListener the listener to handle error
     */
    public void setErrorListener(MediaRecorder.OnErrorListener errorListener) {
        mExtraErrorListener = errorListener;
    }

    /**
     * start record voice.
     * @param outputFile the output file path
     * @return true if start operation is ok; else return false
     */
    public boolean startRecordVoice(String outputFile) {
        if (mState == STATE_RECORDING && mLastOutputFile != null
                && mLastOutputFile.equals(outputFile)) {
            Elog.d(TAG, "recording to the same file; ignore the start operation");
            return false;
        }
        mLastOutputFile = outputFile;
        release();
        if (mParams == null) {
            throw new IllegalArgumentException("Recorder Parameter must be set");
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(mParams.audioSource);
        mRecorder.setOutputFormat(mParams.outputFormat);
        mRecorder.setAudioEncoder(mParams.audioEncoder);
        mRecorder.setOutputFile(outputFile);
        mRecorder.setAudioChannels(mParams.channel);
        mRecorder.setAudioSamplingRate(mParams.samplingRate);
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);
        try {
            mRecorder.prepare();
        } catch (IOException  e) {
            Elog.d(TAG, "Prepare() IOException:" + e.getMessage());
            mRecorder.reset();
            mState = STATE_INITIAL;
            if (mExtraErrorListener != null) {
                mExtraErrorListener.onError(mRecorder, 0, ERROR_PREPARE);
            }
            return false;
        }
        mRecorder.start();
        mState = STATE_RECORDING;
        return true;
    }

    /**
     * stop the record operation.
     * @return true is stop operation is ok, else return false
     */
    public boolean stopRecordVoice() {
        if (mRecorder == null || mState != STATE_RECORDING) {
            Elog.d(TAG, "invalid mRecorder or recording was not ongoing;"
                    + "ignore stop operation; state:" + mState);
            return false;
        }
        mRecorder.stop();
        mState = STATE_STOPPED;
        release();
        return true;
    }

    /**
     * release the resource the recorder used.
     */
    public void release() {
        if (mRecorder != null) {
            if (mState == STATE_RECORDING) {
                mRecorder.stop();
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            mState = STATE_RELEASED;
        }
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        Elog.d(TAG, "mediaRecorder.onError what:" + what + " extra:" + extra);
        if (mExtraErrorListener != null) {
            mExtraErrorListener.onError(mediaRecorder, what, extra);
        }
        release();
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        Elog.d(TAG, "mediaRecorder.onInfo what:" + what + " extra:" + extra);
    }

}
