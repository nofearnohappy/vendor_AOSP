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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.engineermode.cameranew;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.CheckBox;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoCalibration extends Activity implements OnItemClickListener {

    public static final String PREFERENCE_KEY = "camera_inter_settings";
    private static final String TAG = "EM/AutoCalibration";
    private static final int DIALOG_ISO_SPEED = 5;
    private static final int DIALOG_VERIFICATION_AWB = 6;
    private static final int DIALOG_VERIFICATION_SHADING = 7;


    private static final String[] ISO_STRS_ARRAY = {"0", "100", "150", "200", "300", "400", "600", "800", "1200",
            "1600", "2000", "2400", "2800", "3200" };
    public static final int ID_EV_CALIBRATION_NONE = 0;
    public static final int ID_EV_CALIBRATION_ACTION = 1;

    // camera parameter key for eng mode
    private static final String KEY_ENG_MFLL_SUPPORTED = "eng-mfll-s";
    private static final String KEY_ENG_SENSOR_MODE_SLIM_VIDEO1_SUPPORTED = "sv1-s";
    private static final String KEY_ENG_SENSOR_MODE_SLIM_VIDEO2_SUPPORTED = "sv2-s";
    private static final String KEY_ENG_SENSOR_MODE_SUPPORTED = "sen-mode-s";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_2M_SUPPORTED = "vdr-r2m-s";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_4K2K_SUPPORTED = "vdr-r4k2k-s";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_CROP_CENTER_2M_SUPPORTED = "vdr-cc2m-s";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_SUPPORTED = "vrd-mfr-s";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MIN = "vrd-mfr-min";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MAX = "vrd-mfr-max";

    private static final String KEY_ENG_MTK_AWB_SUPPORTED = "mtk-awb-s";
    private static final String KEY_ENG_SENSOR_AWB_SUPPORTED = "sr-awb-s";
    private static final String KEY_ENG_MTK_AWB_ENABLE = "mtk-awb-e";
    private static final String KEY_ENG_SENSOR_AWB_ENABLE = "sr-awb-e";

    private static final String KEY_ENG_MTK_SHADING_SUPPORTED = "mtk-shad-s";
    private static final String KEY_ENG_MTK_1TO3_SHADING_SUPPORTED = "mtk-123-shad-s";
    private static final String KEY_ENG_SENSOR_SHADNING_SUPPORTED = "sr-shad-s";
    private static final String KEY_ENG_MTK_SHADING_ENABLE = "mtk-shad-e";
    private static final String KEY_ENG_MTK_1TO3_SHADING_ENABLE = "mtk-123-shad-e";
    private static final String KEY_ENG_SENSOR_SHADNING_ENABLE = "sr-shad-e";

    private static final String KEY_ENG_VIDEO_HDR_SUPPORTED = "vhdr-s";

    private static final String KEY_ENG_MULTI_NR_SUPPORTED = "mnr-s";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String KEY_IMAGE_REFOCUS_SUPPORTED = "stereo-image-refocus-values";
    private static final String KEY_ENG_VIDEO_RAW_DUMP_SUPPORTED = "vrd-s";
    private static final int CAPTURE_SIZE_NUM = 11;
    private static final int CAPTURE_SIZE_ZSD_INDEX = 2;
    private static final int CAPTURE_SIZE_SLIM_VIDEO1 = 4;
    private static final int CAPTURE_SIZE_SLIM_VIDEO2 = 5;
    private static final int CAPTURE_SIZE_CUSTOM_BASE = 6;
    private static final int CAPTURE_SIZE_CUSTOM_END = 10;

    private static final int SECNE_MODE_AUTO = 0;
    private static final int SECNE_MODE_FIREWORKS = 11;
    // components
    private RadioButton mNormalMode;
    private LinearLayout mNormalCaptureLayout;
    private Spinner mNormalCaptureSize;
    private Spinner mNormalCaptureType;
    private EditText mNormalCaptureNum;
    private Spinner mMulFrameMode;
    private LinearLayout mMulFrameLayout;
    private LinearLayout mMulFrameSetLayout;
    private EditText mMulFrameCaptureNum;
    private RadioButton mVideoCliplMode;
    private LinearLayout mVideoClipLayout;
    private Spinner mVideoClipResolution;
    private RadioButton mAfAuto;
    private RadioButton mAfFullScan;
    private RadioButton mAfBracket;
    private RadioButton mAfThrough;
    private LinearLayout mAfBracketLayout;
    private EditText mAfBracketRange;
    private Spinner mAfBracketInterval;
    private LinearLayout mAfThroughLayout;
    private EditText mAfThroughInterval;
    private Spinner mAfThroughDirec;
    private ListView mIsoListView;
    private ListView mCaptureListView;

    private RadioButton mAfContinuous;
    private Spinner mAfThroughRepeat;
    private LinearLayout mAfFullScanSetLayout;
    private EditText mAfFullScanFrameInterval;
    private int mMaxFullScanFrameInterval;
    private int mMinFullScanFrameInterval;
    // Use for 2nd MP(JB2)
    // private Spinner mPreFlashSpinner;
    // private Spinner mMainFlashSpinner;
    private Spinner mStrobeModeSpinner;
    private Spinner mFlickerSpinner;
    // Through focus: Manual configure
    private LinearLayout mThroughFocusStart;
    private LinearLayout mThroughFocusEnd;
    private EditText mThroughFocsuStartPos;
    private EditText mThroughFocsuEndPos;
    private LinearLayout mStrobeModeLayout;
    private Spinner mSensorSpinner;
    private int mCameraId;
    private boolean mIsMain2 = false;
    private boolean mIsInitialized = false;
    private int mAvailCameraNumber = 0;
    private ListView mCommonOptionLv;
    private Spinner mSdblkSpinner;
    private Spinner mShadingSpinner;
    private Spinner mShutterDelaySpinner;
    // Video Raw Dump - Multi Frame Rate
    private LinearLayout mVideoFrameRateLayout;
    private Spinner mVideoFrameRateSpinner;
    private EditText mVideoFrameMinRate;
    private EditText mVideoFrameMaxRate;
    private int mMinFrameRate;
    private int mMaxFrameRate;
    private boolean mVideoFrameRateSupport = false;
    private int mVideoFrameRateMin = 0;
    private int mVideoFrameRateMax = 0;

    private boolean[] mMulISOFlags = {true, false, true, false, false, false, false, false, false, false, false,
            false, false, false, false, false };
    private boolean mAfModeStatus = true; // auto is true, others is false
    private int mAfSpecialIso = 0;
    private String mIsoValueStr;
    private int mCaptureMode = 0;
    private int mAfMode = 0;
    // ISO interplation
    private EditText mIsoRatioEdit;
    private Spinner mIsoInterSpinner;
    // HDR debug
    private static final String HDR_KEY = "mediatek.hdr.debug";
    private Spinner mHdrSpinner;

    private CheckBox[] mAwbCkBoxs = new CheckBox[2];
    private CheckBox[] mShadingCkBoxs = new CheckBox[3];
    private TextView mAwb;
    private TextView mShading;
    // Vidoe HDR
    private Spinner mVideoHdrSpinner;
    private Spinner mVHdrModeSpinner;
    private EditText mVHdrRatio;
    // Multi Pass NR
    private Spinner mMultiNrSpinner;
    // Capture Size - Start
    private ArrayAdapter<String> mCaptureSizeAdapter = null;
    //default true: preview(0), capture(1), video(3);
    private boolean[] mCaptureSizeState = {true, true, false, true, false, false, false, false, false, false, false};
    private String[] mCaptureSize;
    // Capture Size - End
    private EditText mShutterSpeed;
    private EditText mSensorGain;

    // Scene mode
    private Spinner mSceneModeSpinner;

    private CompoundButton.OnCheckedChangeListener mRadioListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Elog.d(TAG, "Button is " + buttonView.getId() + "isChecked " + isChecked);
            if (isChecked) {
                View[] afLayoutArr = {mAfBracketLayout, mAfThroughLayout, mAfFullScanSetLayout};
                RadioButton[] afRadioArr = {mAfAuto, mAfBracket, mAfFullScan, mAfThrough, mAfContinuous};
                // when select full scan, capture size need support "capture size(ZSD)"
                boolean changeAf = false;
                for (RadioButton rb : afRadioArr) {
                    if (rb == buttonView) {
                        changeAf = true;
                        break;
                    }
                }
                if (changeAf) {
                    if (mAfMode != Camera.AF_MODE_FULL_SCAN && buttonView == mAfFullScan) {
                        mCaptureSizeState[CAPTURE_SIZE_ZSD_INDEX] = true;
                    } else {
                        mCaptureSizeState[CAPTURE_SIZE_ZSD_INDEX] = false;
                    }
                    updateCaptureSizeAdapter();
                }

                if (buttonView == mNormalMode) {
                    mCaptureMode = 0;
                    mNormalCaptureLayout.setVisibility(View.VISIBLE);
                    mVideoClipLayout.setVisibility(View.GONE);
                    mVideoCliplMode.setChecked(false);
                    // set af status
                    setAfModeAccessble(true);
                }  else if (buttonView == mVideoCliplMode) {
                    mCaptureMode = 2;
                    mNormalCaptureLayout.setVisibility(View.GONE);
                    mVideoClipLayout.setVisibility(View.VISIBLE);
                    mNormalMode.setChecked(false);
                    // set af status
                    mAfAuto.setChecked(true);
                    setAfModeAccessble(false);
                } else if (buttonView == mAfAuto) {
                    mAfMode = Camera.AF_MODE_AUTO;
                    setVisibleLayout(afLayoutArr, null);
                    setCheckedRadioGroup(afRadioArr, mAfAuto);
                } else if (buttonView == mAfBracket) {
                    mAfMode = Camera.AF_MODE_BRACKET;
                    setVisibleLayout(afLayoutArr, new View[]{mAfBracketLayout});
                    setCheckedRadioGroup(afRadioArr, mAfBracket);
                } else if (buttonView == mAfFullScan) {
                    mAfMode = Camera.AF_MODE_FULL_SCAN;
                    setVisibleLayout(afLayoutArr, new View[]{mAfFullScanSetLayout});
                    setCheckedRadioGroup(afRadioArr, mAfFullScan);
                } else if (buttonView == mAfThrough) {
                    mAfMode = Camera.AF_MODE_THROUGH_FOCUS;
                    setVisibleLayout(afLayoutArr, new View[]{mAfThroughLayout});
                    setCheckedRadioGroup(afRadioArr, mAfThrough);
                } else if (buttonView == mAfContinuous) {
                    mAfMode = Camera.AF_MODE_CONTINUOUS;
                    setVisibleLayout(afLayoutArr, null);
                    setCheckedRadioGroup(afRadioArr, mAfContinuous);
                }
                if (mAfMode == Camera.AF_MODE_AUTO) {
                    if (!mAfModeStatus) {
                        statusChangesByAf(true);
                        mAfModeStatus = true;
                    }
                } else {
                    if (mAfModeStatus) {
                        statusChangesByAf(false);
                        mAfModeStatus = false;
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Elog.i(TAG, "onCreate.");
        mIsInitialized = false;
        setContentView(R.layout.new_auto_calibration);
        mAvailCameraNumber = android.hardware.Camera.getNumberOfCameras();
        Elog.i(TAG, "mAvailCameraNumber =" + mAvailCameraNumber);
        if (mAvailCameraNumber <= 0) {
            Toast.makeText(this, "No available Camera", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        inintComponents();
        mCameraId = 0;
        safeUseCameraDevice(mCameraId, new CameraSafeUser() {
            @Override
            public void useCamera(android.hardware.Camera camera) {
                initByCameraDeviceCapability(camera.getParameters());
            }
        });
        setStatusTodefault();
        mIsInitialized = true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Elog.i(TAG, "onItemClick view: " + parent.getId() + " position: " + position);
        if (parent.getId() == mIsoListView.getId()) {
            if (position == 0) { // set ISO speed
                showDialog(DIALOG_ISO_SPEED);
            }
        } else if (parent.getId() == R.id.common_option_list) {
            if (position == 0) { // EV Calibration
                if (!putValuesToPreference()) {
                    return;
                }
                Elog.d(TAG, "Start EV Calibration");
                Intent previewIntent = new Intent(this, Camera.class);
                previewIntent.putExtra(getString(R.string.camera_key_ev_calibration), ID_EV_CALIBRATION_ACTION);
                startActivity(previewIntent);
            }
        } else if (parent.getId() == R.id.listview_capture) {
            if (position == 0) { // start preview
                if (!putValuesToPreference()) {
                    return;
                }
                Intent captureIntent = new Intent();
                captureIntent.setClass(this, Camera.class);
                this.startActivity(captureIntent);
                Elog.i(TAG, "Start captureIntent!");
            }
        } else {
            Elog.w(TAG, "UNKNOWN Parent ID:" + parent.getId());
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Elog.i(TAG, "onCreateDialog id: " + id + "AF mode: " + mAfMode);
        Dialog dialog = null;
        Builder builder = null;
        if (DIALOG_ISO_SPEED == id) {
            builder = new AlertDialog.Builder(AutoCalibration.this);
            builder.setTitle(R.string.auto_clibr_iso_setting);
            if (mAfMode == Camera.AF_MODE_AUTO) {
                builder.setMultiChoiceItems(R.array.auto_calib_iso, mMulISOFlags,
                        new DialogInterface.OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mMulISOFlags[which] = isChecked;
                                if (which == 0) {
                                    if (isChecked) {
                                        for (int i = 1; i < mMulISOFlags.length; i++) {
                                            if (i == 2) {
                                                mMulISOFlags[i] = true;
                                                ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                                            } else {
                                                mMulISOFlags[i] = false;
                                                ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                                            }
                                        }
                                    }
                                } else if (which == 1) {
                                    if (isChecked) {
                                        mMulISOFlags[0] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                                        for (int i = 2; i < mMulISOFlags.length; i++) {
                                            mMulISOFlags[i] = true;
                                            ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                                        }
                                    }
                                } else {
                                    if (isChecked) {
                                        mMulISOFlags[0] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                                    } else {
                                        mMulISOFlags[1] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(1, false);
                                    }
                                }
                            }
                        });
            } else {
                builder.setSingleChoiceItems(R.array.auto_calib_special_iso, mAfSpecialIso,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mAfSpecialIso = whichButton;
                                mIsoValueStr = ISO_STRS_ARRAY[mAfSpecialIso];
                            }
                        });
            }
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mAfMode == Camera.AF_MODE_AUTO) {
                        putStrInPreference(R.string.auto_clibr_key_iso_speed, getIsoArrayValue(mMulISOFlags));
                    } else {
                        putStrInPreference(R.string.auto_clibr_key_iso_speed, mIsoValueStr);
                    }
                    removeDialog(DIALOG_ISO_SPEED);
                    if (mVideoCliplMode.isChecked()) {
                        Toast.makeText(AutoCalibration.this, R.string.auto_clibr_video_dump_tips, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
        if (builder != null) {
            dialog = builder.create();
            dialog.setCancelable(false);
        }
        return dialog;
    }

    private void setAfLayout(int[] visis, RadioButton[] buttons) {
        mAfBracketLayout.setVisibility(visis[0]);
        mAfThroughLayout.setVisibility(visis[1]);
        for (RadioButton radioButton : buttons) {
            radioButton.setChecked(false);
        }
    }

    private void setVisibleLayout(View[] allLayouts, View[] visibleLayouts) {
        for (View view : allLayouts) {
            if (visibleLayouts != null) {
                boolean inVisible = false;
                for (View visiLayout : visibleLayouts) {
                    if (visiLayout == view) {
                        inVisible = true;
                        break;
                    }
                }
                if (inVisible) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    private void setCheckedRadioGroup(RadioButton[] allRadios, RadioButton checkedRadio) {
        for (RadioButton radio: allRadios) {
            if (radio == checkedRadio) {
                radio.setChecked(true);
            } else {
                radio.setChecked(false);
            }
        }
    }

    private void setAfModeAccessble(boolean access) {
        RadioButton[] buttons = {mAfBracket, mAfFullScan, mAfThrough, mAfContinuous};
        for (RadioButton radioButton : buttons) {
            radioButton.setEnabled(access);
        }
    }

    private String collectCustomParams() {
        StringBuilder builder = new StringBuilder();
        int[] editIds = {R.id.camera_custom_param_0_edit,
                R.id.camera_custom_param_1_edit,
                R.id.camera_custom_param_2_edit};
        for (int i = 0; i < editIds.length; i++) {
            String value = ((EditText) findViewById(editIds[i])).getText().toString();
            if (value != null) {
                builder.append(value).append(";");
            } else {
                builder.append(";");
            }
        }
        return builder.toString();
    }
    private void updateCaptureSizeAdapter() {
        mCaptureSizeAdapter.clear();
        for (int i = 0; i < CAPTURE_SIZE_NUM; i++) {
            if (mCaptureSizeState[i]) {
                mCaptureSizeAdapter.add(mCaptureSize[i]);
            }
        }
    }
    private void inintComponents() {
        // Capture mode
        mNormalMode = (RadioButton) findViewById(R.id.raido_capture_normal);
        mNormalMode.setOnCheckedChangeListener(mRadioListener);
        mMulFrameMode = (Spinner) findViewById(R.id.auto_clibr_multi_frame);
        mMulFrameMode.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 1) { // MFLL - ON
                    mMulFrameSetLayout.setVisibility(View.VISIBLE);
                    if (mNormalCaptureType.getSelectedItemPosition() != 0) {
                         mNormalCaptureType.setSelection(0); // Processed Raw Type only
                         Toast.makeText(AutoCalibration.this, "Swtich to Processed Raw", Toast.LENGTH_LONG).show();
                    }
                    mStrobeModeSpinner.setSelection(2); // off
                } else {
                    mMulFrameSetLayout.setVisibility(View.GONE);
                    mStrobeModeSpinner.setSelection(0); // auto
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "MulFrame - select nothing.");
            }

        });
        mVideoCliplMode = (RadioButton) findViewById(R.id.raido_capture_video);
        mVideoCliplMode.setOnCheckedChangeListener(mRadioListener);
        mNormalCaptureLayout = (LinearLayout) findViewById(R.id.normal_capture_set);
        mNormalCaptureSize = (Spinner) findViewById(R.id.normal_capture_size);
        mCaptureSize = getResources().getStringArray(R.array.normal_captrue_size);
        mCaptureSizeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item);
        for (int i = 0; i < CAPTURE_SIZE_NUM; i++) {
            if (mCaptureSizeState[i]) {
                mCaptureSizeAdapter.add(mCaptureSize[i]);
            }
        }
        mCaptureSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNormalCaptureSize.setAdapter(mCaptureSizeAdapter);

        mNormalCaptureType = (Spinner) findViewById(R.id.normal_capture_type);
        mNormalCaptureNum = (EditText) findViewById(R.id.normal_capture_number);
        mShutterSpeed = (EditText) findViewById(R.id.normal_capture_shutter_speed);
        mSensorGain = (EditText) findViewById(R.id.normal_capture_sensor_gain);
        mMulFrameLayout = (LinearLayout) findViewById(R.id.mult_frame_capture);
        mMulFrameSetLayout = (LinearLayout) findViewById(R.id.mult_frame_capture_set);
        mMulFrameCaptureNum = (EditText) findViewById(R.id.mult_capture_number);
        mVideoClipLayout = (LinearLayout) findViewById(R.id.video_capture_set);
        mVideoClipResolution = (Spinner) findViewById(R.id.video_capture_resolution);
        // Video Raw Dump - Multi Frame Rate
        String[] videoResolution = AutoCalibration.this.getResources().getStringArray(R.array.auto_calib_video_clip);
        List<String> videoResolutionList = new ArrayList<String>();
        for (int i = 0; i < videoResolution.length; i++) {
            videoResolutionList.add(videoResolution[i]);
        }
        ArrayAdapter<String> adp = new ArrayAdapter<String>(AutoCalibration.this,
                android.R.layout.simple_spinner_item, videoResolutionList);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVideoClipResolution.setAdapter(adp);
        // Manual Frame Rate
        mVideoFrameRateLayout = (LinearLayout) findViewById(R.id.video_frame_set);
        mVideoFrameMinRate = (EditText) findViewById(R.id.video_frame_min_rate);
        mVideoFrameMaxRate = (EditText) findViewById(R.id.video_frame_max_rate);
        mVideoFrameRateSpinner = (Spinner) findViewById(R.id.auto_clibr_video_frame_set);
        mVideoFrameRateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 1) {
                    mVideoFrameMinRate.setEnabled(true);
                    mVideoFrameMaxRate.setEnabled(true);
                } else {
                    mVideoFrameMinRate.setEnabled(false);
                    mVideoFrameMaxRate.setEnabled(false);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "Frame rate - select nothing.");
            }

        });
        // Af mode
        mAfAuto = (RadioButton) findViewById(R.id.raido_af_auto);
        mAfAuto.setOnCheckedChangeListener(mRadioListener);
        mAfFullScan = (RadioButton) findViewById(R.id.raido_af_full);
        mAfFullScan.setOnCheckedChangeListener(mRadioListener);
        mAfBracket = (RadioButton) findViewById(R.id.raido_af_bracket);
        mAfBracket.setOnCheckedChangeListener(mRadioListener);
        mAfThrough = (RadioButton) findViewById(R.id.raido_af_through);
        mAfThrough.setOnCheckedChangeListener(mRadioListener);
        mAfBracketLayout = (LinearLayout) findViewById(R.id.af_bracket_set);
        mAfBracketInterval = (Spinner) findViewById(R.id.af_bracket_interval);
        mAfBracketRange = (EditText) findViewById(R.id.af_bracket_range);
        mAfThroughLayout = (LinearLayout) findViewById(R.id.af_through_set);
        mAfThroughDirec = (Spinner) findViewById(R.id.af_through_dirct);
        // If Manual configure(2) has been selected, show start/stop pos
        // editors, else let them gone.
        mAfThroughDirec.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 2) {
                    mThroughFocusStart.setVisibility(View.VISIBLE);
                    mThroughFocusEnd.setVisibility(View.VISIBLE);
                } else {
                    mThroughFocusStart.setVisibility(View.GONE);
                    mThroughFocusEnd.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "select nothing.");
            }

        });
        mAfThroughInterval = (EditText) findViewById(R.id.af_through_interval);
        // Through focus: Manual configure
        mThroughFocusStart = (LinearLayout) findViewById(R.id.through_focus_start_set);
        mThroughFocusEnd = (LinearLayout) findViewById(R.id.through_focus_end_set);
        mThroughFocsuStartPos = (EditText) findViewById(R.id.af_through_manual_start);
        mThroughFocsuEndPos = (EditText) findViewById(R.id.af_through_manual_end);
        // ISO list view
        mIsoListView = (ListView) findViewById(R.id.listview_iso);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new String[] {getString(R.string.auto_clibr_iso_speed) });
        mIsoListView.setAdapter(adapter);
        setLvHeight(mIsoListView);
        mIsoListView.setOnItemClickListener(this);
        // init spinners
        // mPreFlashSpinner = (Spinner) findViewById(R.id.pre_flash_lev);
        // mMainFlashSpinner = (Spinner) findViewById(R.id.main_flash_lev);
        mFlickerSpinner = (Spinner) findViewById(R.id.flicker);
        mStrobeModeSpinner = (Spinner) findViewById(R.id.led_flash);
        // Capture lis view
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Arrays.asList(getResources()
                .getStringArray(R.array.auto_calib_capture)));
        mCaptureListView = (ListView) findViewById(R.id.listview_capture);
        mCaptureListView.setAdapter(adapter);
        setLvHeight(mCaptureListView);
        mCaptureListView.setOnItemClickListener(this);
        // HDR debug
        mHdrSpinner = (Spinner) findViewById(R.id.hdr_debug);
        if ("1".equals(SystemProperties.get(HDR_KEY))) {
            mHdrSpinner.setSelection(1);
        } else {
            mHdrSpinner.setSelection(0);
        }
        mHdrSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SystemProperties.set(HDR_KEY, String.valueOf(arg2));
                Elog.i(TAG, "hdrValue : " + SystemProperties.get(HDR_KEY));
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "select nothing.");
            }
        });

        mAfContinuous = (RadioButton) findViewById(R.id.raido_af_continuous);
        mAfContinuous.setOnCheckedChangeListener(mRadioListener);
        mAfThroughRepeat = (Spinner) findViewById(R.id.af_through_repeat_sp);
        mAfFullScanSetLayout = (LinearLayout) findViewById(R.id.af_full_scan_set_layout);
        mAfFullScanFrameInterval = (EditText) findViewById(R.id.af_full_scan_frame_interval_edit);

        mStrobeModeLayout = (LinearLayout) findViewById(R.id.auto_calib_strobe_mode_ll);
        mSensorSpinner = (Spinner) findViewById(R.id.auto_clibr_camera_sensor_spnr);

        mCommonOptionLv = (ListView) findViewById(R.id.common_option_list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources()
                .getStringArray(R.array.camera_common_option_entries));
        mCommonOptionLv.setAdapter(adapter);
        mCommonOptionLv.setOnItemClickListener(this);
        // Verification
        mAwb = (TextView) findViewById(R.id.auto_clibr_veri_awb);
        mShading = (TextView) findViewById(R.id.auto_clibr_veri_shading);
        mAwbCkBoxs[0] = (CheckBox) findViewById(R.id.auto_clibr_veri_awb_mtk);
        mAwbCkBoxs[1] = (CheckBox) findViewById(R.id.auto_clibr_veri_awb_sensor);

        mShadingCkBoxs[0] = (CheckBox) findViewById(R.id.auto_clibr_veri_shading_mtk);
        mShadingCkBoxs[1] = (CheckBox) findViewById(R.id.auto_clibr_veri_shading_mtk_1to3);
        mShadingCkBoxs[2] = (CheckBox) findViewById(R.id.auto_clibr_veri_shading_mtk_sensor);
        // Video Hdr
        mVideoHdrSpinner = (Spinner) findViewById(R.id.video_hdr_spinner);
        mVideoHdrSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 1) {
                    if (mNormalCaptureSize.getSelectedItemPosition() != 2) {
                         mNormalCaptureSize.setSelection(2); // Video Size only
                         Toast.makeText(AutoCalibration.this, "Swtich to Video Size", Toast.LENGTH_LONG).show();
                    }
                    findViewById(R.id.vhdr_set_id).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.vhdr_set_id).setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "mVideoHdrSpinner - select nothing.");
            }

        });
        mVHdrModeSpinner = (Spinner) findViewById(R.id.vhdr_set_mode_spinner);
        mVHdrRatio = (EditText) findViewById(R.id.vhdr_set_ratio_edit);
        // Multi Pass NR
        mMultiNrSpinner = (Spinner) findViewById(R.id.multi_pass_nr_spinner);
        mSdblkSpinner = (Spinner) findViewById(R.id.output_sdblk_file_spn);
        mShadingSpinner = (Spinner) findViewById(R.id.shading_table_spn);
        mShutterDelaySpinner = (Spinner) findViewById(R.id.shutter_delay_spn);
        mIsoRatioEdit = (EditText) findViewById(R.id.auto_clibr_iso_inter_edit);
        mIsoInterSpinner = (Spinner) findViewById(R.id.auto_clibr_iso_inter_spinner);
        mIsoInterSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 1) {
                    findViewById(R.id.auto_clibr_iso_inter_set).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.auto_clibr_iso_inter_set).setVisibility(View.GONE);
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "mIsoInterSpinner - select nothing.");
            }

        });
       mSceneModeSpinner = (Spinner) findViewById(R.id.auto_clibr_scene_mode_spinner);
       mSceneModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            // need sync with normal camera
            // ISO, Flash
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != SECNE_MODE_AUTO) {
                    resetISOValue();  // change ISO to Auto.
                }
                if (arg2 == SECNE_MODE_FIREWORKS) {
                    mStrobeModeSpinner.setSelection(2); // fireworks need set flash off.
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "mSceneModeSpinner - select nothing.");
            }

        });
    }
    private void resetISOValue() {
        mMulISOFlags[0] = true;
        mMulISOFlags[1] = false;
        mMulISOFlags[2] = true;
        for (int i = 3; i < mMulISOFlags.length; i++) {
            mMulISOFlags[i] = false;
        }
    }

    private void initSupportedUi(Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes == null)
            return;
        if (!focusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            findViewById(R.id.camera_af_auto_ll).setVisibility(View.GONE);
        } else {
            findViewById(R.id.camera_af_auto_ll).setVisibility(View.VISIBLE);
        }
        if (!focusModes.contains(Parameters.FOCUS_MODE_FULLSCAN)) {
            findViewById(R.id.camera_af_full_scan_ll).setVisibility(View.GONE);
        } else {
            findViewById(R.id.camera_af_full_scan_ll).setVisibility(View.VISIBLE);
        }
        if (!focusModes.contains(Parameters.FOCUS_MODE_MANUAL)) {
            findViewById(R.id.camera_af_bracket_ll).setVisibility(View.GONE);
            findViewById(R.id.camera_af_through_focus_ll).setVisibility(View.GONE);
        } else {
            findViewById(R.id.camera_af_bracket_ll).setVisibility(View.VISIBLE);
            findViewById(R.id.camera_af_through_focus_ll).setVisibility(View.VISIBLE);
        }
        if (!focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            findViewById(R.id.camera_af_continuous).setVisibility(View.GONE);
        } else {
            findViewById(R.id.camera_af_continuous).setVisibility(View.VISIBLE);
        }
        int silmV1Support = getParameterValue(KEY_ENG_SENSOR_MODE_SLIM_VIDEO1_SUPPORTED, parameters);
        if ((silmV1Support == 1 && mCameraId == 0) ||
                (silmV1Support == 2 && mCameraId == 1) || silmV1Support == 3) {
            mCaptureSizeState[CAPTURE_SIZE_SLIM_VIDEO1] = true;
        } else {
            mCaptureSizeState[CAPTURE_SIZE_SLIM_VIDEO1] = false;
        }
        int silmV2Support = getParameterValue(KEY_ENG_SENSOR_MODE_SLIM_VIDEO2_SUPPORTED, parameters);
        if ((silmV2Support == 1 && mCameraId == 0) ||
                (silmV2Support == 2 && mCameraId == 1) || silmV2Support == 3) {
            mCaptureSizeState[CAPTURE_SIZE_SLIM_VIDEO2] = true;
        } else {
            mCaptureSizeState[CAPTURE_SIZE_SLIM_VIDEO2] = false;
        }
        int customSupport = getParameterValue(KEY_ENG_SENSOR_MODE_SUPPORTED, parameters);
        for (int i = 0; i < 5; i++) {
            mCaptureSizeState[CAPTURE_SIZE_CUSTOM_BASE + i] = (customSupport >> i & 1) == 1 ? true : false;
        }
        updateCaptureSizeAdapter();
        if (!Camera.isStrobeSupported(parameters)) {
            mStrobeModeLayout.setVisibility(View.GONE);
        } else {
            mStrobeModeLayout.setVisibility(View.VISIBLE);
        }
        if (!getFeatureSupported(KEY_ENG_MFLL_SUPPORTED, parameters)) {
            mMulFrameLayout.setVisibility(View.GONE);
        } else {
            mMulFrameLayout.setVisibility(View.VISIBLE);
        }
        mMulFrameSetLayout.setVisibility(View.GONE);
        // Video Raw Dump - Multi Frame Rate
        String[] videoResolution = AutoCalibration.this.getResources().getStringArray(R.array.auto_calib_video_clip);
        List<String> videoResolutionList = new ArrayList<String>();
        for (int i = 0; i < videoResolution.length; i++) {
            videoResolutionList.add(videoResolution[i]);
        }
        if (getFeatureSupported(KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_2M_SUPPORTED, parameters)) {
            videoResolutionList.add(AutoCalibration.this.getString(R.string.auto_clibr_video_clip_2M));
        }
        if (getFeatureSupported(KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_4K2K_SUPPORTED, parameters)) {
            videoResolutionList.add(AutoCalibration.this.getString(R.string.auto_clibr_video_clip_2K4K));
        }
        if (getFeatureSupported(KEY_ENG_VIDEO_RAW_DUMP_CROP_CENTER_2M_SUPPORTED, parameters)) {
            videoResolutionList.add(AutoCalibration.this.getString(R.string.auto_clibr_video_clip_center_2M));
        }
        ArrayAdapter<String> adp = new ArrayAdapter<String>(AutoCalibration.this,
                android.R.layout.simple_spinner_item, videoResolutionList);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVideoClipResolution.setAdapter(adp);
        if (!getFeatureSupported(KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_SUPPORTED, parameters)) {
            mVideoFrameRateLayout.setVisibility(View.GONE);
            mVideoFrameRateSupport = false;
        } else {
            mVideoFrameRateLayout.setVisibility(View.VISIBLE);
            mVideoFrameRateSupport = true;
            mVideoFrameRateMin = getParameterValue(KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MIN, parameters);
            mVideoFrameRateMax = getParameterValue(KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MAX, parameters);
        }
        // Verification
        if (!getFeatureSupported(KEY_ENG_MTK_AWB_SUPPORTED, parameters) && !getFeatureSupported(KEY_ENG_SENSOR_AWB_SUPPORTED, parameters)) {
            mAwb.setVisibility(View.GONE);
            findViewById(R.id.auto_clibr_veri_awb_id).setVisibility(View.GONE);
        } else {
            mAwb.setVisibility(View.VISIBLE);
            findViewById(R.id.auto_clibr_veri_awb_id).setVisibility(View.VISIBLE);
            if (getFeatureSupported(KEY_ENG_MTK_AWB_SUPPORTED, parameters)) {
                findViewById(R.id.auto_clibr_veri_awb_mtk).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.auto_clibr_veri_awb_mtk).setVisibility(View.GONE);
            }
            if (getFeatureSupported(KEY_ENG_SENSOR_AWB_SUPPORTED, parameters)) {
                findViewById(R.id.auto_clibr_veri_awb_sensor).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.auto_clibr_veri_awb_sensor).setVisibility(View.GONE);
            }

        }
        if (!getFeatureSupported(KEY_ENG_MTK_SHADING_SUPPORTED, parameters) && !getFeatureSupported(KEY_ENG_MTK_1TO3_SHADING_SUPPORTED, parameters)
            && !getFeatureSupported(KEY_ENG_SENSOR_SHADNING_SUPPORTED, parameters)) {
            mShading.setVisibility(View.GONE);
            findViewById(R.id.auto_clibr_veri_shading_id).setVisibility(View.GONE);
        } else {
            mShading.setVisibility(View.VISIBLE);
            findViewById(R.id.auto_clibr_veri_shading_id).setVisibility(View.VISIBLE);
            if (getFeatureSupported(KEY_ENG_MTK_SHADING_SUPPORTED, parameters)) {
                findViewById(R.id.auto_clibr_veri_shading_mtk).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.auto_clibr_veri_shading_mtk).setVisibility(View.GONE);
            }
            if (getFeatureSupported(KEY_ENG_MTK_1TO3_SHADING_SUPPORTED, parameters)) {
                findViewById(R.id.auto_clibr_veri_shading_mtk_1to3).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.auto_clibr_veri_shading_mtk_1to3).setVisibility(View.GONE);
            }
            if (getFeatureSupported(KEY_ENG_SENSOR_SHADNING_SUPPORTED, parameters)) {
                findViewById(R.id.auto_clibr_veri_shading_mtk_sensor).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.auto_clibr_veri_shading_mtk_sensor).setVisibility(View.GONE);
            }
        }
        List<String> vHdrList = getVHdrSupportedList(parameters);
        if (vHdrList == null) {
            findViewById(R.id.video_hdr_id).setVisibility(View.GONE);
        } else {
            findViewById(R.id.video_hdr_id).setVisibility(View.VISIBLE);
            // update vHDR list by return string array
            ArrayAdapter<String> vHdrAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, vHdrList);
            vHdrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mVHdrModeSpinner.setAdapter(vHdrAdapter);
        }
        findViewById(R.id.vhdr_set_id).setVisibility(View.GONE);
        if (getFeatureSupported(KEY_ENG_MULTI_NR_SUPPORTED, parameters)) {
            findViewById(R.id.multi_pass_nr_id).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.multi_pass_nr_id).setVisibility(View.GONE);
        }

        if (isSupportedVideoClipRaw(parameters)) {
            findViewById(R.id.video_clip_raw_id).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.video_clip_raw_id).setVisibility(View.GONE);
        }
        // init sensor spinner
        if (!mIsInitialized) {
            List<String> sensorList = new ArrayList<String>();
            boolean isAddMain2 = false;
            for (int i = 0; i < mAvailCameraNumber; i++) {
                CameraInfo info = new CameraInfo();
                android.hardware.Camera.getCameraInfo(i, info);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK
                        && Camera.isRawSensor(CameraInfo.CAMERA_FACING_BACK, parameters)) {
                    sensorList.add(getString(R.string.auto_clibr_camera_sensor_main));
                    if (isImageRefocusSupported(parameters)) {
                        isAddMain2 = true;
                    }
                } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT
                        && Camera.isRawSensor(CameraInfo.CAMERA_FACING_FRONT, parameters)) {
                    sensorList.add(getString(R.string.auto_clibr_camera_sensor_sub));
                } else {
                    Elog.w(TAG, "add raw sensor; Invalid facing info from CameraInfo.facing: " + info.facing);
                }
            }
            if (isAddMain2) {
                sensorList.add(getString(R.string.auto_clibr_camera_sensor_main2));
            }
            if (sensorList.size() == 0) {
                Toast.makeText(this, "no available raw sensor", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ArrayAdapter<String> sensorAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, sensorList);
            sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSensorSpinner.setAdapter(sensorAdapter);
            mSensorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                        int position, long id) {
                    Elog.d(TAG, "mSensorSpinner.onItemSelected; position" + position + " mIsInitialized:" + mIsInitialized);
                    if (!mIsInitialized) {
                        return;
                    }
                    String tag = parent.getSelectedItem().toString();
                    int oriPos = mCameraId;
                    if (tag.equals(getString(R.string.auto_clibr_camera_sensor_main))) {
                        mCameraId = 0;
                        mIsMain2 = false;
                    } else if (tag.equals(getString(R.string.auto_clibr_camera_sensor_main2))) {
                        mCameraId = 0;
                        mIsMain2 = true;
                    } else if (tag.equals(getString(R.string.auto_clibr_camera_sensor_sub))) {
                        mIsMain2 = false;
                        mCameraId = 1;
                    }
                    Elog.d(TAG, "Camera ID:" + mCameraId + " Tag:" + tag);
                    safeUseCameraDevice(mCameraId, new CameraSafeUser() {
                        @Override
                        public void useCamera(android.hardware.Camera camera) {
                            initByCameraDeviceCapability(camera.getParameters());
                        }
                    });
                    setStatusTodefault();
                    if (oriPos != position) {
                        Toast.makeText(AutoCalibration.this, "Reset to default state!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    Elog.d(TAG, "mSensorSpinner onNothingSelected()");
                }
            });
        }
    }

    private void initByCameraDeviceCapability(Parameters parameters) {
        initSupportedUi(parameters);

        mMaxFullScanFrameInterval = parameters.getEngFocusFullScanFrameIntervalMax();
        mMinFullScanFrameInterval = parameters.getEngFocusFullScanFrameIntervalMin();
    }

    private interface CameraSafeUser {
        void useCamera(android.hardware.Camera camera);
    }

    int safeUseCameraDevice(int cameraId, CameraSafeUser user) {
        android.hardware.Camera cameraDevice = null;
        if (mIsMain2) {
            Elog.e(TAG, "enabled main2");
            enabledMain2(true);
        }
        cameraDevice = android.hardware.Camera.open(cameraId);
        if (cameraDevice == null) {
            Elog.e(TAG, "Open Camera fail; ");
            Toast.makeText(this, "fail to open camera", Toast.LENGTH_SHORT).show();
            return -1;
        }
        user.useCamera(cameraDevice);
        cameraDevice.release();
        if (mIsMain2) {
            Elog.e(TAG, "disabled main2");
            enabledMain2(false);
        }
        return 0;
    }

    private void enabledMain2(boolean enabled) {
        String enabledStr = "1";
        String disabledStr = "-1";
        if (mAvailCameraNumber == 2) {
            enabledStr = "2";
        }
        if (enabled) {
            android.hardware.Camera.setProperty("debug.camera.open", enabledStr);
        } else {
            android.hardware.Camera.setProperty("debug.camera.open", disabledStr);
        }
    }

    private void setStatusTodefault() {
        Elog.v(TAG, "setStatusTodefault()");
        mCaptureMode = 0;
        mNormalMode.setChecked(true);
        mMulISOFlags[0] = true;
        mMulISOFlags[1] = false;
        mMulISOFlags[2] = true;
        for (int i = 3; i < mMulISOFlags.length; i++) {
            mMulISOFlags[i] = false;
        }
        mAfMode = Camera.AF_MODE_AUTO;
        mAfAuto.setChecked(true);
        mAfModeStatus = true;
        mAfBracketRange.setText("0");
        mAfThroughInterval.setText("1");
        mNormalCaptureNum.setText("1");
        mShutterSpeed.setText("0");
        mSensorGain.setText("0");
        mMulFrameCaptureNum.setText("4");  // current only support 4
        mMulFrameCaptureNum.setEnabled(false);  // don't allow user to input.
        mVideoFrameMinRate.setText("15");
        mVideoFrameMaxRate.setText("30");
        mIsoInterSpinner.setSelection(0);
        mIsoRatioEdit.setText("10"); //set 10% as default.
        final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.auto_clibr_key_capture_mode), mCaptureMode);
        editor.putInt(getString(R.string.auto_clibr_key_capture_size), 0);
        editor.putInt(getString(R.string.auto_clibr_key_capture_type), 0);
        editor.putInt(getString(R.string.auto_clibr_key_capture_number), 1);
        editor.putInt(getString(R.string.auto_clibr_key_flicker), 0);
        editor.putInt(getString(R.string.auto_clibr_key_led_flash), 0);
        editor.putInt(getString(R.string.auto_clibr_key_pre_flash), 1);
        editor.putInt(getString(R.string.auto_clibr_key_main_flash), 1);
        editor.putString(getString(R.string.auto_clibr_key_iso_speed), getIsoArrayValue(mMulISOFlags));
        editor.putInt(getString(R.string.auto_clibr_key_af_mode), mAfMode);
        editor.commit();
        mThroughFocsuStartPos.setText(String.valueOf(preferences.getInt(
                getString(R.string.auto_clibr_key_through_manual_start_pos), 0)));
        mThroughFocsuEndPos.setText(String.valueOf(preferences.getInt(
                getString(R.string.auto_clibr_key_through_manual_end_pos), 1023)));
        mThroughFocusStart.setVisibility(View.GONE);
        mThroughFocusEnd.setVisibility(View.GONE);

        List<String> throughRepeatList = new ArrayList<String>();
        for (int i = 0; i < 8; i++) {
            throughRepeatList.add(String.valueOf(i + 1));
        }
        ArrayAdapter<String> afThroughRepeatAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, throughRepeatList);
        afThroughRepeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAfThroughRepeat.setAdapter(afThroughRepeatAdapter);

        mAfFullScanFrameInterval.setText(String.valueOf(mMinFullScanFrameInterval));
        // set default Capture size = capture size
        mNormalCaptureSize.setSelection(1);
        // set default Capture type = pure raw
        mNormalCaptureType.setSelection(1);
        mMulFrameMode.setSelection(0);
        mVideoFrameRateSpinner.setSelection(0);
        mVideoHdrSpinner.setSelection(0); // off
        mMultiNrSpinner.setSelection(0);
        mSdblkSpinner.setSelection(0);
        mShadingSpinner.setSelection(0);
        mShutterDelaySpinner.setSelection(0);
        mFlickerSpinner.setSelection(0);
        for (int i = 0; i < 2; i++) {
            mAwbCkBoxs[i].setChecked(false);
        }
        for (int i = 0; i < 3; i++) {
            mShadingCkBoxs[i].setChecked(true); // default ON as AE request
        }
        mSceneModeSpinner.setSelection(0);
    }

    private int getShutterDelayFromUi() {
        String strVal = mShutterDelaySpinner.getSelectedItem().toString();
        int endPos = strVal.indexOf("s");
        if (endPos > 0) {
            strVal = strVal.substring(0, endPos);
        }
        strVal = strVal.trim();
        int val = 0;
        try {
            val = Integer.valueOf(strVal);
        } catch (NumberFormatException e) {
            Elog.e(TAG, "NumberFormatException:" + e.getMessage() + " strVal:" + strVal);
        }
        return val;
    }

    private int getShadingTableFromUi() {
        int selectedIdx = mShadingSpinner.getSelectedItemPosition();
        return selectedIdx;
    }

    private int getOutputSdblkFromUi() {
        int val = 0;
        String strVal;
        strVal = mSdblkSpinner.getSelectedItem().toString();
        if (strVal.equalsIgnoreCase("on")) {
            val = 1;
        }
        return val;
    }

    private boolean putValuesToPreference() {
        final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.auto_clibr_key_capture_mode), mCaptureMode);
        int value;
        if (mCaptureMode == 0) {
            int pos = mNormalCaptureSize.getSelectedItemPosition();
            String size = (String) mNormalCaptureSize.getItemAtPosition(pos);
            for (int i = 0; i < CAPTURE_SIZE_NUM; i++) {
                if (size.equals(mCaptureSize[i])) {
                    pos = i;
                }
            }
            editor.putInt(getString(R.string.auto_clibr_key_capture_size), pos);
            editor.putInt(getString(R.string.auto_clibr_key_capture_type), mNormalCaptureType.getSelectedItemPosition());
            value = getIntegerFromEdit(mNormalCaptureNum, 1, true, 100, true);
            if (value == -1) {
                Toast.makeText(this, R.string.auto_clibr_capture_number_range_tip, Toast.LENGTH_LONG).show();
                return false;
            }
            editor.putInt(getString(R.string.auto_clibr_key_capture_number),
                    value);
            editor.putInt(getString(R.string.auto_clibr_key_mfll_enable),
                    mMulFrameMode.getSelectedItemPosition());
            value = getIntegerFromEdit(mMulFrameCaptureNum, 1, true, 8, true);
            if (value == -1) {
                Toast.makeText(this, R.string.auto_clibr_mfll_count_range_tip, Toast.LENGTH_LONG).show();
                return false;
            }
            editor.putInt(getString(R.string.auto_clibr_key_mfll_count), value);
            value = getIntegerFromEdit(mShutterSpeed, 0, true, 100000, true);
            if (value == -1) {
                Toast.makeText(this, R.string.capture_shutter_range_tip, Toast.LENGTH_LONG).show();
                return false;
            }
            editor.putInt(getString(R.string.auto_clibr_capture_shutter_speed), value);
            value = getIntegerFromEdit(mSensorGain, 0, true, 100000, true);
            if (value == -1) {
                Toast.makeText(this, R.string.capture_sensor_range_tip, Toast.LENGTH_LONG).show();
                return false;
            }
            editor.putInt(getString(R.string.auto_clibr_capture_sensor_gain), value);
        } else {
            int pos = mVideoClipResolution.getSelectedItemPosition();
            String clip = (String) mVideoClipResolution.getItemAtPosition(pos);
            if (clip.equals(getString(R.string.auto_clibr_video_clip_2M))) {
                pos = 2;
            } else if (clip.equals(getString(R.string.auto_clibr_video_clip_2K4K))) {
                pos = 3;
            } else if (clip.equals(getString(R.string.auto_clibr_video_clip_center_2M))) {
                pos = 4;
            }
            editor.putInt(getString(R.string.auto_clibr_key_capture_resolution),
                    pos);
            if (mVideoFrameRateSupport) {
                editor.putInt(getString(R.string.auto_clibr_video_frame_enable),
                        mVideoFrameRateSpinner.getSelectedItemPosition());

                value = getIntegerFromEdit(mVideoFrameMinRate, mVideoFrameRateMin, true, mVideoFrameRateMax, true);
                if (value == -1) {
                    Toast.makeText(this, R.string.auto_clibr_video_frame_range_tip, Toast.LENGTH_LONG).show();
                    return false;
                }
                editor.putInt(getString(R.string.auto_clibr_video_frame_low), value);
                int highValue = getIntegerFromEdit(mVideoFrameMaxRate, mVideoFrameRateMin, true, mVideoFrameRateMax, true);
                if (highValue == -1 || highValue < value) {
                    Toast.makeText(this, R.string.auto_clibr_video_frame_range_tip, Toast.LENGTH_LONG).show();
                    return false;
                }
                editor.putInt(getString(R.string.auto_clibr_video_frame_high), highValue);
            }
        }
        editor.putInt(getString(R.string.auto_clibr_key_flicker), mFlickerSpinner.getSelectedItemPosition());
        editor.putInt(getString(R.string.auto_clibr_key_led_flash), mStrobeModeSpinner.getSelectedItemPosition());
        // editor.putInt(getString(R.string.auto_clibr_key_pre_flash),
        // mPreFlashSpinner.getSelectedItemPosition() + 1);
        // editor.putInt(getString(R.string.auto_clibr_key_main_flash),
        // mMainFlashSpinner.getSelectedItemPosition() + 1);
        editor.putInt(getString(R.string.auto_clibr_key_af_mode), mAfMode);
        editor.putInt(getString(R.string.auto_clibr_key_branket_interval),
                Integer.valueOf(mAfBracketInterval.getSelectedItem().toString()));
        value = getIntegerFromEdit(mAfBracketRange, 0, true, 511, true);
        if (value == -1) {
            Toast.makeText(this, R.string.auto_clibr_af_bracket_range_tip, Toast.LENGTH_LONG).show();
            return false;
        }
        editor.putInt(getString(R.string.auto_clibr_key_branket_range), value);
        editor.putInt(getString(R.string.auto_clibr_key_through_focus_dirct), mAfThroughDirec.getSelectedItemPosition());
        if (mAfThroughDirec.getSelectedItemPosition() == 2) {
            editor.putInt(getString(R.string.auto_clibr_key_through_manual_start_pos),
                    Integer.valueOf(mThroughFocsuStartPos.getText().toString()));
            editor.putInt(getString(R.string.auto_clibr_key_through_manual_end_pos),
                    Integer.valueOf(mThroughFocsuEndPos.getText().toString()));
        }
        value = getIntegerFromEdit(mAfThroughInterval, 1, true, 511, true);
        if (value == -1) {
            Toast.makeText(this, R.string.auto_clibr_af_through_interval_tip, Toast.LENGTH_LONG).show();
            return false;
        }
        editor.putInt(getString(R.string.auto_clibr_key_through_focus_interval), value);

        value = getIntegerFromEdit(mAfFullScanFrameInterval, mMinFullScanFrameInterval, true, mMaxFullScanFrameInterval, true);
        if (value == -1) {
            Toast.makeText(this, "Frame inverval range is " +
                    mMinFullScanFrameInterval + " to " + mMaxFullScanFrameInterval,
                            Toast.LENGTH_SHORT).show();
            return false;
        }
        editor.putInt(getString(R.string.auto_clibr_key_full_frame_interval), value);
        String strRepeat = mAfThroughRepeat.getSelectedItem().toString();
        editor.putInt(getString(R.string.auto_clibr_key_through_repeat), Integer.valueOf(strRepeat));
        editor.putString(getString(R.string.camera_key_custom_parameter), collectCustomParams());
        editor.putInt(getString(R.string.camera_key_selected_sensor_id), mCameraId);
        editor.putInt(getString(R.string.camera_key_shutter_delay), getShutterDelayFromUi());
        editor.putInt(getString(R.string.camera_key_shading_table), getShadingTableFromUi());
        editor.putInt(getString(R.string.camera_key_output_sdblk), getOutputSdblkFromUi());
        //isChecked())
        int awbVeri = 0;
        awbVeri |= mAwbCkBoxs[0].isChecked() ? (1 << 0) : 0;
        awbVeri |= mAwbCkBoxs[1].isChecked() ? (1 << 1) : 0;
        editor.putInt(getString(R.string.auto_clibr_verification_awb), awbVeri);
        int shadingVeri = 0;
        shadingVeri |= mShadingCkBoxs[0].isChecked() ? (1 << 0) : 0;
        shadingVeri |= mShadingCkBoxs[1].isChecked() ? (1 << 1) : 0;
        shadingVeri |= mShadingCkBoxs[2].isChecked() ? (1 << 2) : 0;
        editor.putInt(getString(R.string.auto_clibr_verification_shading), shadingVeri);
        editor.putInt(getString(R.string.video_hdr), mVideoHdrSpinner.getSelectedItemPosition());
        if (mVideoHdrSpinner.getSelectedItemPosition() == 1) {
            // set mode
            int pos = mVHdrModeSpinner.getSelectedItemPosition();
            String mode = (String) mVHdrModeSpinner.getItemAtPosition(pos);
            editor.putString(getString(R.string.vhdr_set_mode), mode);
            // set ratio (1000<= ration <= 9999)
            value = getIntegerFromEdit(mVHdrRatio, 1000, true, 9999, true);
            if (value == -1) {
                Toast.makeText(this, R.string.vhdr_ratio_range_tip, Toast.LENGTH_LONG).show();
                return false;
            }
            editor.putInt(getString(R.string.vhdr_set_ratio), value);
        }
        editor.putInt(getString(R.string.multi_pass_nr), mMultiNrSpinner.getSelectedItemPosition());
        editor.putInt(getString(R.string.auto_clibr_scene_mode),
                        mSceneModeSpinner.getSelectedItemPosition());
        if (mIsMain2) {
            editor.putInt(getString(R.string.auto_clibr_camera_sensor_main2), mAvailCameraNumber);
        } else {
            editor.putInt(getString(R.string.auto_clibr_camera_sensor_main2), 0);
        }
        editor.commit();
        if (mAfMode == Camera.AF_MODE_AUTO) {
            putStrInPreference(R.string.auto_clibr_key_iso_speed, getIsoArrayValue(mMulISOFlags));
        } else {
            putStrInPreference(R.string.auto_clibr_key_iso_speed, mIsoValueStr);
        }
        return true;
    }

    private int getIntegerFromEdit(EditText edit, int from, boolean includeFrom, int end, boolean includeEnd) {
        String editStr = edit.getText().toString();
        int value = -1;
        try {
            value = Integer.valueOf(editStr);
        } catch (NumberFormatException e) {
            Elog.w(TAG, "getIntegerFromEdit() NumberFormatException:" + e.getMessage());
        }

        if (value < from || value > end) {
            value = -1;
        }  else if (!includeFrom && (value == from)) {
            value = -1;
        } else if (!includeEnd && (value == end)) {
            value = -1;
        }
        return value;
    }

    private String getIsoArrayValue(boolean[] array) {
        String result = "";
        int min = array.length;
        int max = 0;

        for (int i = 2; i < array.length; i++) {
            if (array[i]) {
                result += ISO_STRS_ARRAY[i - 2] + ",";
            }
        }
        for (int i = 3; i < array.length; i++) {
            if (array[i]) {
                if (i < min) {
                    min = i;  // find min iso index except "auto".
                }
                max = i;  // find max iso index
            }
        }
        if (result.length() == 0) {
            Toast.makeText(this, R.string.auto_clibr_iso_tips, Toast.LENGTH_LONG);
            return "0,";
        }
        if (mIsoInterSpinner.getSelectedItemPosition() == 0 || max <= min) {
            return result;  // iso interpolation off, use iso values directly
        } else {
            String editStr = mIsoRatioEdit.getText().toString();
            int ratio = -1;
            int min_iso = 0;
            int max_iso = 0;
            try {
                ratio = Integer.valueOf(editStr);
                min_iso = Integer.valueOf(ISO_STRS_ARRAY[min - 2]);
                max_iso = Integer.valueOf(ISO_STRS_ARRAY[max - 2]);
            } catch (NumberFormatException e) {
                Elog.w(TAG, "getIntegerFromEdit() NumberFormatException:" + e.getMessage());
                ratio = -1;
            }
            if (ratio == -1) {
                return result;
            }
            if (array[2]) {  // add AUTO
                result = ISO_STRS_ARRAY[0] + ",";
            } else {
                result = "";
            }
            int iso = min_iso;
            while (iso <= max_iso) {
                result += iso + ",";
                iso += min_iso * ratio / 100;
            }
            return result;
        }
    }

    private void statusChangesByAf(boolean afStatus) {
        // mPreFlashSpinner.setEnabled(afStatus);
        // mMainFlashSpinner.setEnabled(afStatus);
        mStrobeModeSpinner.setSelection(2); // off
        mStrobeModeSpinner.setEnabled(afStatus);
        mVideoCliplMode.setEnabled(afStatus);
        mNormalCaptureNum.setEnabled(afStatus);
        if (mCaptureMode != 0) {
            mNormalMode.setChecked(true);
        }
        if (afStatus) {
            mMulISOFlags[0] = true;
            mMulISOFlags[1] = false;
            mMulISOFlags[2] = true;
            for (int i = 3; i < mMulISOFlags.length; i++) {
                mMulISOFlags[i] = false;
            }
            putStrInPreference(R.string.auto_clibr_key_iso_speed, getIsoArrayValue(mMulISOFlags));
        } else {
            mAfSpecialIso = 0;
            mIsoValueStr = "0";
            putStrInPreference(R.string.auto_clibr_key_iso_speed, mIsoValueStr);
        }
    }

    private void putInPreference(int keyId, int value) {
        String key = getString(keyId);
        Elog.i(TAG, "putInPreference key: " + key + ",value: " + value);
        try {
            final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY,
                    android.content.Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, value);
            editor.commit();
        } catch (NullPointerException ne) {
            Elog.i(TAG, ne.getMessage());
        }
    }

    private void putStrInPreference(int keyId, String value) {
        String key = getString(keyId);
        Elog.i(TAG, "putInPreference key: " + key + ",value: " + value);
        try {
            final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY,
                    android.content.Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (NullPointerException ne) {
            Elog.i(TAG, ne.getMessage());
        }
    }

    private void setLvHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            Elog.d(TAG, "no data in ListView");
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = adapter.getView(i, null, listView);
            itemView.measure(0, 0);
            totalHeight += itemView.getMeasuredHeight();
        }
        ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        layoutParams.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(layoutParams);
    }

   private boolean getFeatureSupported(String para, Parameters parameters) {
        String str = parameters.get(para);
        Elog.d(TAG, "getFeatureSupport - " + para + " is " + str);
        return TRUE.equals(str);
   }

   private int getParameterValue(String para, Parameters parameters) {
        int val = 0;
        try {
            val = parameters.getInt(para);
        } catch (NumberFormatException ex) {
            Elog.d(TAG, "getParameterValue - " + para + " is NumberFormatException");
        }
        Elog.d(TAG, "getParameterValue - " + para + " is " + val);
        return val;
   }

   private ArrayList<String> getVHdrSupportedList(Parameters parameters) {
        String str = parameters.get(KEY_ENG_VIDEO_HDR_SUPPORTED);
        if (str == null) {
            return null;
        }
        TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<String> supportedList = new ArrayList<String>();
        for (String s : splitter) {
            if (s != null) {
                supportedList.add(s);
                Elog.d(TAG, "getVHDRSupportedList - " + s);
            }
        }
        // workaround util low layer is ready, then replace to 0.
        if (supportedList.size() < 1) {
            return null;
        }
        return supportedList;
   }
   private  boolean isImageRefocusSupported(Parameters parameters) {
        if (parameters == null) {
            return false;
        }
        String str = parameters.get(KEY_IMAGE_REFOCUS_SUPPORTED);
        Elog.d(TAG, "isImageRefocusSupported - " + str);
        if ("off".equals(str) || null == str) {
            return false;
        } else {
            return true;
        }
    }
   private boolean isSupportedVideoClipRaw(Parameters parameters) {
        if (parameters == null) {
            return false;
        }
        String str = parameters.get(KEY_ENG_VIDEO_RAW_DUMP_SUPPORTED);
        Elog.d(TAG, "isSupportedVideoClipRaw - " + str);
        if (FALSE.equals(str)) {
            return false;
        } else {
            return true;
        }
    }
}
