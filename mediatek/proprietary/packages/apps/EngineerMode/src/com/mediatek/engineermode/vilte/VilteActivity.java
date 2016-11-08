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

package com.mediatek.engineermode.vilte;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.mediatek.engineermode.R;

/**
 * ViLTE Configuration.
 */
public class VilteActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "EM/ViLTE";
    private static final String PROP_VILTE_UT_SUPPORT = "persist.radio.vilte_ut_support";
    private static final String PROP_VILTE_VIDEO_FPS = "persist.radio.vilte_video_fps";
    private static final String PROP_VILTE_SOURCE_BITSTREAM = "persist.radio.vilte_dump_source";
    private static final String PROP_VILTE_SINK_BITSTREAM = "persist.radio.vilte_dump_sink";

    private TextView mVilteUtSupport;
    private TextView mVilteVideoFps;
    private TextView mVilteSourceBitstream;
    private TextView mVilteSinkBitstream;
    private Button mButtonEnable;
    private Button mButtonDisable;
    private Button mButtonSetFps;
    private Button mButtonEnableSourceBitstream;
    private Button mButtonDisableSourceBitstream;
    private Button mButtonEnableSinkBitstream;
    private Button mButtonDisableSinkBitstream;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("@M_" + TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vilte_config);
        mVilteUtSupport = (TextView) findViewById(R.id.vilte_ut_support_status);
        mVilteVideoFps = (TextView) findViewById(R.id.vilte_video_fps_status);
        mVilteSourceBitstream = (TextView) findViewById(R.id.vilte_source_bitstream_status);
        mVilteSinkBitstream = (TextView) findViewById(R.id.vilte_sink_bitstream_status);
        mSpinner = (Spinner) findViewById(R.id.vilte_video_fps_values);
        mButtonEnable = (Button) findViewById(R.id.vilte_ut_support_enable);
        mButtonDisable = (Button) findViewById(R.id.vilte_ut_support_disable);
        mButtonSetFps = (Button) findViewById(R.id.vilte_video_fps_set);
        mButtonEnableSourceBitstream = (Button) findViewById(R.id.vilte_source_bitstream_enable);
        mButtonDisableSourceBitstream = (Button) findViewById(R.id.vilte_source_bitstream_disable);
        mButtonEnableSinkBitstream = (Button) findViewById(R.id.vilte_sink_bitstream_enable);
        mButtonDisableSinkBitstream = (Button) findViewById(R.id.vilte_sink_bitstream_disable);
        mButtonEnable.setOnClickListener(this);
        mButtonDisable.setOnClickListener(this);
        mButtonSetFps.setOnClickListener(this);
        mButtonEnableSourceBitstream.setOnClickListener(this);
        mButtonDisableSourceBitstream.setOnClickListener(this);
        mButtonEnableSinkBitstream.setOnClickListener(this);
        mButtonDisableSinkBitstream.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        Log.d("@M_" + TAG, "onResume()");
        super.onResume();
        queryCurrentValue();
    }

    @Override
    public void onClick(View v) {
        if (v == mButtonEnable) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_UT_SUPPORT + " = 1");
            SystemProperties.set(PROP_VILTE_UT_SUPPORT, "1");
        } else if (v == mButtonDisable) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_UT_SUPPORT + " = 0");
            SystemProperties.set(PROP_VILTE_UT_SUPPORT, "0");
        } else if (v == mButtonSetFps) {
            String fps = mSpinner.getSelectedItem().toString();
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_VIDEO_FPS + " = " + fps);
            SystemProperties.set(PROP_VILTE_VIDEO_FPS, fps);
        } else if (v == mButtonEnableSourceBitstream) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_SOURCE_BITSTREAM + " = 1");
            SystemProperties.set(PROP_VILTE_SOURCE_BITSTREAM, "1");
        } else if (v == mButtonDisableSourceBitstream) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_SOURCE_BITSTREAM + " = 0");
            SystemProperties.set(PROP_VILTE_SOURCE_BITSTREAM, "0");
        } else if (v == mButtonEnableSinkBitstream) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_SINK_BITSTREAM + " = 1");
            SystemProperties.set(PROP_VILTE_SINK_BITSTREAM, "1");
        } else if (v == mButtonDisableSinkBitstream) {
            Log.d("@M_" + TAG, "Set " + PROP_VILTE_SINK_BITSTREAM + " = 0");
            SystemProperties.set(PROP_VILTE_SINK_BITSTREAM, "0");
        }
        queryCurrentValue();
    }

    private void queryCurrentValue() {
        String ut = SystemProperties.get(PROP_VILTE_UT_SUPPORT, "");
        String fps = SystemProperties.get(PROP_VILTE_VIDEO_FPS, "");
        String source = SystemProperties.get(PROP_VILTE_SOURCE_BITSTREAM, "");
        String sink = SystemProperties.get(PROP_VILTE_SINK_BITSTREAM, "");
        mVilteUtSupport.setText(PROP_VILTE_UT_SUPPORT + " = " + ut);
        mVilteVideoFps.setText(PROP_VILTE_VIDEO_FPS + " = " + fps);
        mVilteSourceBitstream.setText(PROP_VILTE_SOURCE_BITSTREAM + " = " + source);
        mVilteSinkBitstream.setText(PROP_VILTE_SINK_BITSTREAM + " = " + sink);
        for (int i = 0; i < mSpinner.getCount(); i++) {
            if (mSpinner.getItemAtPosition(i).toString().equals(fps)) {
                mSpinner.setSelection(i);
                break;
            }
        }
    }
}
