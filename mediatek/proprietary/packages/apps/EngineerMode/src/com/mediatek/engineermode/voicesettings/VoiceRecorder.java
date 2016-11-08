package com.mediatek.engineermode.voicesettings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * VoiceRecorder is the main activity to implement the recorder audio.
 * @author mtk81238
 *
 */
public class VoiceRecorder extends Activity implements OnClickListener {
    private static final String TAG = "EM/VoiceRecorder";
    private static final String MAGIEAR = "MagiEar";
    private static final String VOICE_FILE_PREFIX = "EMVoice_";
    private static final int MSG_CYCLE_CHECK = 3;
    private static final int CHECK_INTERVAL = 500;
    private static final int MIN_STORAGE = 5 * 1024 * 1024;
    private static final String FMGR_DIR_KEY = "select_path";

    private Button mBtnRecord = null;
    private Button mBtnDirectory = null;
    private TextView mTvState = null;
    private RadioGroup mRgStorageLocation = null;
    private String mDefStoragePath = null;
    private String mVoiceFile = null;
    private EmRecorder mEmRecorder = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (!hasEnoughStorage()) {
                Toast.makeText(VoiceRecorder.this, "Storage is not enough",
                        Toast.LENGTH_SHORT).show();
                onClickRecordButton(mBtnRecord);
            } else {
                sendEmptyMessageDelayed(MSG_CYCLE_CHECK, CHECK_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_recorder);
        mBtnRecord = (Button) findViewById(R.id.voice_recorder_start_btn);
        mBtnDirectory = (Button) findViewById(R.id.voice_recorder_directory_btn);
        mTvState = (TextView) findViewById(R.id.voice_recorder_state);
        mRgStorageLocation = (RadioGroup) findViewById(R.id.voice_recorder_storage_grp);
        mBtnRecord.setOnClickListener(this);
        mBtnDirectory.setOnClickListener(this);
        mEmRecorder = new EmRecorder();
        mEmRecorder.setErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder recorder, int what, int extra) {
                Toast.makeText(VoiceRecorder.this, "Recorder error happened!",
                        Toast.LENGTH_SHORT).show();
                VoiceRecorder.this.finish();
            }
        });
        setRecorderParameters();
    }

    private void setRecorderParameters() {
        EmRecorder.Parameters recorderParams = new EmRecorder.Parameters();
        recorderParams.audioSource = EmRecorder.AUDIO_SOURCE_VOICE_UNLOCK;
        recorderParams.audioEncoder = MediaRecorder.AudioEncoder.PCM;
        recorderParams.channel = EmRecorder.CHANNEL_STERO;
        recorderParams.outputFormat = MediaRecorder.OutputFormat.OUTPUT_FORMAT_WAV;
        recorderParams.samplingRate = EmRecorder.SAMPLING_RATE_48K;
        mEmRecorder.setRecorderParameters(recorderParams);
    }

    private boolean isStorageMounted() {
        mDefStoragePath = StorageManagerEx.getDefaultPath();
        String state = Environment.getStorageState(new File(mDefStoragePath));
        Elog.d(TAG, "isStorageMounted mDefStoragePath:" + mDefStoragePath + " state:" + state);
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private long getAvailableStorage() {
        mDefStoragePath = StorageManagerEx.getDefaultPath();
        StatFs statFs = new StatFs(mDefStoragePath);
        return statFs.getAvailableBytes();
    }

    private boolean hasEnoughStorage() {
        long availableBytes = getAvailableStorage();
        if (availableBytes < MIN_STORAGE) {
            return false;
        }
        return true;
    }

    private void onClickRecordButton(Button btn) {
        CharSequence text = btn.getText();
        if (text.equals(getString(R.string.voice_recorder_start))) {
            if (!isStorageMounted()) {
                Toast.makeText(this, "No available storage, make sure the storage is ready",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (!hasEnoughStorage()) {
                Toast.makeText(this, "Storage is not enough",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            prepareVoiceFile();
            btn.setEnabled(false);
            mTvState.setText(R.string.voice_recorder_state_recording);
            mEmRecorder.startRecordVoice(mVoiceFile);
            btn.setText(R.string.voice_recorder_stop);
            btn.setEnabled(true);
            mHandler.sendEmptyMessageDelayed(MSG_CYCLE_CHECK, CHECK_INTERVAL);
        } else if (text.equals(getString(R.string.voice_recorder_stop))) {
            btn.setEnabled(false);
            mEmRecorder.stopRecordVoice();
            mHandler.removeMessages(MSG_CYCLE_CHECK);
            btn.setText(R.string.voice_recorder_start);
            btn.setEnabled(true);
            mTvState.setText(R.string.voice_recorder_state_stopped);
        }
    }

    private void prepareVoiceFile() {
        mVoiceFile = mDefStoragePath + "/" + MAGIEAR + "/";
        int id = mRgStorageLocation.getCheckedRadioButtonId();
        if (id == R.id.voice_recorder_male_rb) {
            mVoiceFile += getString(R.string.voice_recorder_male);
        } else if (id == R.id.voice_recorder_female_rb) {
            mVoiceFile += getString(R.string.voice_recorder_female);
        }
        File dir = new File(mVoiceFile);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formatDate = dateFormat.format(new Date());
        mVoiceFile += "/" + VOICE_FILE_PREFIX + formatDate + EmRecorder.FILE_POSTFIX_WAV;
        Elog.d(TAG, "mVoiceFile:" + mVoiceFile);
    }

    private void openAudioDirectory() {
        mDefStoragePath = StorageManagerEx.getDefaultPath();
        String audioDir = mDefStoragePath + "/" + MAGIEAR + "/";
        int id = mRgStorageLocation.getCheckedRadioButtonId();
        if (id == R.id.voice_recorder_male_rb) {
            audioDir += getString(R.string.voice_recorder_male);
        } else if (id == R.id.voice_recorder_female_rb) {
            audioDir += getString(R.string.voice_recorder_female);
        }
        File dir = new File(audioDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Intent intent = new Intent();
        intent.setClassName("com.mediatek.filemanager",
                "com.mediatek.filemanager.FileManagerOperationActivity");
        intent.putExtra(FMGR_DIR_KEY, audioDir);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Elog.d(TAG, "ActivityNotFoundException:" + e.getMessage());
            Toast.makeText(this, "No found MTK FileManager to open audio directory",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mBtnRecord) {
            onClickRecordButton(mBtnRecord);
        } else if (view == mBtnDirectory) {
            openAudioDirectory();
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeMessages(MSG_CYCLE_CHECK);
        mEmRecorder.release();
        super.onDestroy();
    }
}
