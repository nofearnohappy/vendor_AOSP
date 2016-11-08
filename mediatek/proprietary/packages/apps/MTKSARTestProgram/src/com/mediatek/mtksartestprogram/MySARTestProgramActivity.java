/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.mtksartestprogram;



import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.net.wifi.WifiManager;



public class MySARTestProgramActivity extends Activity implements SensorEventListener {

    private TextView command_text;
    private TextView result_text;
    private EditText command_edit;
    private EditText reductionRat_edit_2g;
    private EditText reductionRat_edit_3g;
    private EditText reductionRat_edit_4g;
    private EditText reductionRat_edit_band;
    private EditText setwifi_edit;
    private Button reset;
    private Button command;
    private Button reductionRat;
    private Button reductionRatByband;
    private Spinner modemSpinner;
    private Spinner bandSpinner;
    ArrayAdapter<String> modemSpinnerAdapter;
    ArrayAdapter<String> bandSpinnerAdapter;
    private Button open_wifi;
    private Button close_wifi;
    private Button set_wifi;
    private CommandTool tool;
    protected WifiManager mWM;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        command_text = (TextView) findViewById(R.id.command_text);
        result_text = (TextView) findViewById(R.id.result_text);
        command_edit = (EditText) findViewById(R.id.command_edit);
        reductionRat_edit_2g = (EditText) findViewById(R.id.reductionRat_edit_2g);
        reductionRat_edit_3g = (EditText) findViewById(R.id.reductionRat_edit_3g);
        reductionRat_edit_4g = (EditText) findViewById(R.id.reductionRat_edit_4g);
        reductionRat_edit_band = (EditText) findViewById(R.id.reductionRat_edit_band);
        setwifi_edit = (EditText) findViewById(R.id.setwifi_edit);

        tool = new CommandTool();

        modemSpinner = (Spinner) findViewById(R.id.modem);
        modemSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                CommandTool.modem);
        modemSpinner.setAdapter(modemSpinnerAdapter);
        modemSpinner.setSelection(0, true);
        bandSpinner = (Spinner) findViewById(R.id.band);
        bandSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                CommandTool.band[0]);
        bandSpinner.setAdapter(bandSpinnerAdapter);
        bandSpinner.setSelection(0, true);
        modemSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                bandSpinnerAdapter = new ArrayAdapter<String>(MySARTestProgramActivity.this,
                        android.R.layout.simple_spinner_item, CommandTool.band[position]);
                bandSpinner.setAdapter(bandSpinnerAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        reset = (Button) findViewById(R.id.reset);
        command = (Button) findViewById(R.id.command);
        reductionRat = (Button) findViewById(R.id.reductionRat);
        reductionRatByband = (Button) findViewById(R.id.reductionRatByband);
        open_wifi = (Button) findViewById(R.id.open_wifi);
        close_wifi = (Button) findViewById(R.id.close_wifi);
        set_wifi = (Button) findViewById(R.id.set_wifi);

        reset.setOnClickListener(restListener);
        command.setOnClickListener(commandListener);
        reductionRat.setOnClickListener(reductionRatListener);
        reductionRatByband.setOnClickListener(reductionRatByband_Listener);
        open_wifi.setOnClickListener(openwifi_Listener);
        close_wifi.setOnClickListener(closewifi_Listener);
        set_wifi.setOnClickListener(setwifi_Listener);

        mWM = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        pSenserInit();
    }

    private OnClickListener restListener = new OnClickListener() {
        public void onClick(View v) {
            String[] back = tool.reset();
            command_text.setText(back[0]);
            result_text.setText(back[1]);
        }
    };

    private OnClickListener commandListener = new OnClickListener() {
        public void onClick(View v) {
            if (check(command_edit.getText().toString(), false, 0, 0)) {
                String[] back = tool.command(command_edit.getText().toString());
                command_text.setText(back[0]);
                result_text.setText(back[1]);
            }
        }
    };

    private OnClickListener reductionRatListener = new OnClickListener() {
        public void onClick(View v) {
            if (check(reductionRat_edit_2g.getText().toString(), true, 31, 0)
                    && check(reductionRat_edit_3g.getText().toString(), true, 31, 0)
                    && check(reductionRat_edit_4g.getText().toString(), true, 31, 0)) {
                String[] back = tool.reductionRat(reductionRat_edit_2g.getText().toString(),
                        reductionRat_edit_3g.getText().toString(), reductionRat_edit_4g.getText()
                                .toString());
                command_text.setText(back[0]);
                result_text.setText(back[1]);
            }
        }
    };

    private OnClickListener reductionRatByband_Listener = new OnClickListener() {
        public void onClick(View v) {
            if (check(reductionRat_edit_band.getText().toString(), true, 31, 0)) {
                int modemIndex = modemSpinner.getSelectedItemPosition();
                int bandIndex = bandSpinner.getSelectedItemPosition();
                String[] back = tool.reductionRatByBand(modemIndex, bandIndex,
                        reductionRat_edit_band.getText().toString());
                command_text.setText(back[0]);
                result_text.setText(back[1]);
                reductionRat_edit_band.setText("");
            }
        }
    };

    private OnClickListener openwifi_Listener = new OnClickListener() {
        public void onClick(View v) {
            mWM.setTxPowerEnabled(true);
            command_text.setText("open wifiTXpower");
            result_text.setText("OK");
        }
    };

    private OnClickListener closewifi_Listener = new OnClickListener() {
        public void onClick(View v) {
            mWM.setTxPowerEnabled(false);
            command_text.setText("close wifiTXpower");
            result_text.setText("OK");
        }
    };

    private OnClickListener setwifi_Listener = new OnClickListener() {
        public void onClick(View v) {
            if (check(setwifi_edit.getText().toString(), true, 20, -60)) {
                mWM.setTxPower(Integer.parseInt(setwifi_edit.getText().toString()));
                mWM.setTxPowerEnabled(true);
                command_text.setText("set wifiTXpower = "
                        + Integer.parseInt(setwifi_edit.getText().toString()));
                setwifi_edit.setText("");
                result_text.setText("OK");
            }
        }
    };

    private boolean check(String str, boolean checkNum, int max, int min) {
        if (str != null && !("".equals(str)) && str.length() > 0) {
            if (checkNum) {
                boolean isNUM = false;
                if (str.matches("\\d*")) {
                    isNUM = true;
                } else {
                    int lenght = str.length();
                    if (lenght > 1) {
                        if (str.substring(0, 1).equals("-") || str.substring(0, 1).equals("+")) {
                            String tail = str.substring(1, lenght - 1);
                            if (tail.matches("\\d*")) {
                                isNUM = true;
                            }
                        }
                    }
                }
                if (isNUM) {
                    if (Integer.parseInt(str) <= max && Integer.parseInt(str) >= min) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        Toast.makeText(this, " input error! please check ", Toast.LENGTH_SHORT).show();
        return false;
    }

    // -----------------Senser-------------------
    private SensorManager mSensorManager;
    private Sensor mProximity;

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];

            if (distance == 0) {
                Toast.makeText(MySARTestProgramActivity.this, "near ", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MySARTestProgramActivity.this, "far ", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void pSenserInit() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        String type = "";
        for (Sensor sor : allSensors) {
            type = type + " " + sor.getType();
        }
        boolean bool = mSensorManager.registerListener(sensorEventListener, mProximity,
                SensorManager.SENSOR_DELAY_NORMAL);
        Toast.makeText(MySARTestProgramActivity.this,
                "pSenserInit Sensor type list: " + type + " register:" + bool, Toast.LENGTH_SHORT)
                .show();

       
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(sensorEventListener);
        super.onDestroy();
    }

    // -----------------Senser-------------------

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent arg0) {
        // TODO Auto-generated method stub

    }
}
