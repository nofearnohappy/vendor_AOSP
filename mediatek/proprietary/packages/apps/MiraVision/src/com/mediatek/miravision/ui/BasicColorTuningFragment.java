package com.mediatek.miravision.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.mediatek.miravision.setting.MiraVisionJni;
import com.mediatek.miravision.setting.MiraVisionJni.Range;

public class BasicColorTuningFragment extends BaseTuningFragment {

    private static final String TAG = "Miravision/BasicColorTuningFragment";
    private final int[] mTuningItems = { R.string.basic_color_item_contrast,
            R.string.basic_color_item_saturation, R.string.basic_color_item_brightness };

    public BasicColorTuningFragment() {
    }

    public BasicColorTuningFragment(int title) {
        mTuningTitle = title;
        for (int i = 0; i < mTuningItems.length; i++) {
            if (mTuningTitle == mTuningItems[i]) {
                index = i;
                break;
            }
        }
        Log.d(TAG, "mTuningTitle = " + mTuningTitle + ", index = " + index);
    }

    private Range mContrastRange;
    private Range mSaturationRange;
    private Range mBrightnessRange;

    private Image mImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        mImageAdvAfter.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        mContrastRange = MiraVisionJni.getContrastIndexRange();
        mSaturationRange = MiraVisionJni.getSaturationIndexRange();
        mBrightnessRange = MiraVisionJni.getPicBrightnessIndexRange();

        updateUi();

        mImage = (Image) mImageBasicAfter;
        mImage.init(new Handler(), (KeyguardManager) getActivity().getSystemService(
                Activity.KEYGUARD_SERVICE));
        mImage.setHostFragmentResumed(true);

        if ("tablet".equals(SystemProperties.get("ro.build.characteristics"))) {
            mImage.postInvalidate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mImage.setHostFragmentResumed(false);
        MiraVisionJni.nativeSetPQColorRegion(0, 0, 0, 0, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MiraVisionJni.nativeSetPQColorRegion(0, 0, 0, 0, 0);
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        super.onClick(v);
        Log.d(TAG, "onClick index = " + index);
        updateUi();
        v.setEnabled(true);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            onSeekBarChange(seekBar.getProgress());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void onSeekBarChange(int value) {
        Log.d(TAG, "onChange value = " + value);
        switch (mTuningTitle) {
        case R.string.basic_color_item_contrast:
            MiraVisionJni.setContrastIndex(value);
            break;
        case R.string.basic_color_item_saturation:
            MiraVisionJni.setSaturationIndex(value);
            break;
        case R.string.basic_color_item_brightness:
            MiraVisionJni.setPicBrightnessIndex(value);
            break;
        default:
            break;
        }
    }

    private void updateUi() {
        Log.d(TAG, "updateUi index = " + index);
        mTuningTitle = mTuningItems[index];
        mSeekBarText.setText(mTuningTitle);
        switch (mTuningTitle) {
        case R.string.basic_color_item_contrast:
            mLastButton.setVisibility(View.INVISIBLE);
            mSeekBar.setMax(mContrastRange.max - mContrastRange.min);
            mSeekBar.setProgress(MiraVisionJni.getContrastIndex() - mContrastRange.min);
            break;
        case R.string.basic_color_item_saturation:
            mLastButton.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.VISIBLE);
            mSeekBar.setMax(mSaturationRange.max - mSaturationRange.min);
            mSeekBar.setProgress(MiraVisionJni.getSaturationIndex() - mSaturationRange.min);
            break;
        case R.string.basic_color_item_brightness:
            mNextButton.setVisibility(View.INVISIBLE);
            mSeekBar.setMax(mBrightnessRange.max - mBrightnessRange.min);
            mSeekBar.setProgress(MiraVisionJni.getPicBrightnessIndex() - mBrightnessRange.min);
            break;
        default:
            break;
        }

        initImageResource();
    }

    private void initImageResource() {
        Log.d(TAG, "initImageResource");
        int resId = -1;
        switch (mTuningTitle) {
        case R.string.basic_color_item_contrast:
            resId = R.drawable.basic_color_tuning_contrast;
            break;
        case R.string.basic_color_item_saturation:
            resId = R.drawable.basic_color_tuning_saturation;
            break;
        case R.string.basic_color_item_brightness:
            resId = R.drawable.basic_color_tuning_brightness;
            break;
        default:
            break;
        }
        if (resId != -1) {
            mImageOrignal.setImageResource(resId);
            mImageBasicAfter.setImageResource(resId);
        }
    }
}