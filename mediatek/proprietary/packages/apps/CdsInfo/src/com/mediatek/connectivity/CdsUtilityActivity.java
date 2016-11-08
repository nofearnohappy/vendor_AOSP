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

/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.mediatek.connectivity;

import android.app.ProgressDialog;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.SystemProperties;

import com.mediatek.gba.GbaHttpUrlCredential;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;


public class CdsUtilityActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CDSINFO/CdsUtilityActivity";

    private static final String HTTPSTRING = "http://";
    private static final String PINGSTRING = "ping -c 5 www.google.com";
    private static final String PINGV6STRING = "ping6 -c 5 www.google.com";
    private static final String HTTPAUTHSTRING =
                            "http://httpbin.org/digest-auth/auth/test/1234";
    private static final String HTTPXCAPSTRING =
                            "https://xcap.msg.pc.t-mobile.com";

    private ProgressDialog mDialog = null;

    private Context mContext;
    private ConnectivityManager mConnMgr;
    private ProgressThread mProgressThread = null;
    private ArrayAdapter<String> mAutoCompleteAdapter;
    private AutoCompleteTextView mCmdLineList;

    private TextView mOutputScreen = null;
    private TextView mSysPropName = null;
    private TextView mSysPropValue = null;
    private Toast mToast;

    private int mCmdOption = 0;

    private static final String[] WEBSITES = new String[] {"netstat", "mtk_ifconfig",
            "ping -c 1 -s 0 www.google.com",
            "http://www.google.com", "http://www.baidu.cn", "http://www.sina.cn",
            "ps", "getprop", "setprop ",
            "8.8.8.8"
                                                          };
    private static final String[] CMDTYPESTRING = new String[] {"SHELL",
                            "PING", "PING IPV6", "DNS",
                            "HTTP RESPONSE", "HTTP URL",
                            "XCAP URL"};
    private static final String[] SYSPROP_LIST = new String[] {"media.wfd.video-format",
                        "wlan.wfd.bt.exclude", "wfd.dumpts", "wfd.dumprgb", "wfd.slice.size"};


    private static final int RUN          = 0x1001;
    private static final int PING         = 0x1002;
    private static final int PINGV6       = 0x1003;
    private static final int DNS          = 0x1004;
    private static final int HTTPRESPONSNE =  0x1005;
    private static final int HTTPURL       =  0x1006;
    private static final int XCAPURL       =  0x1007;

    private static final int BASE          = RUN;

    private static final int MSG_UPDATE_UI = 0x3001;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.cds_network_tool);

        mContext = this.getBaseContext();

        if (mContext == null) {
            Log.e(TAG, "Could not get Conext of this activity");
        }

        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (mConnMgr == null) {
            Log.e(TAG, "Could not get Connectivity Manager");
            return;
        }

        mAutoCompleteAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, WEBSITES);

        mCmdLineList = (AutoCompleteTextView) findViewById(R.id.cmdLine);
        mCmdLineList.setThreshold(3);
        mCmdLineList.setAdapter(mAutoCompleteAdapter);

        mOutputScreen = (TextView) findViewById(R.id.outputText);
        mSysPropName  = (EditText) findViewById(R.id.syspropName);
        mSysPropValue  = (EditText) findViewById(R.id.syspropValue);

        Spinner spinner = (Spinner) findViewById(R.id.cmdSpinnner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CMDTYPESTRING);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
            int position, long arg3) {
                // TODO Auto-generated method stub

                position += BASE;

                if (position == PING) {
                    mCmdLineList.setText(PINGSTRING);
                } else if (position == PINGV6) {
                    mCmdLineList.setText(PINGV6STRING);
                } else if (position == HTTPRESPONSNE) {
                    mCmdLineList.setText(HTTPSTRING);
                } else if (position == RUN) {
                    mCmdLineList.setText("");
                } else if (position == HTTPURL) {
                    mCmdLineList.setText(HTTPAUTHSTRING);
                } else if (position == XCAPURL) {
                    mCmdLineList.setText(HTTPXCAPSTRING);
                } else {
                    mCmdLineList.setText("www.google.com");
                }

                mCmdOption = position;
                mCmdLineList.requestFocus();
                mCmdLineList.setSelection(mCmdLineList.getText().length());
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        Button button = (Button) findViewById(R.id.runBtn);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.setBtn);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.getBtn);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.stopBtn);
        button.setOnClickListener(this);

        Spinner spinner2 = (Spinner) findViewById(R.id.sysPropSpinnner);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SYSPROP_LIST);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View arg1,
            int position, long arg3) {
                Object obj = adapterView.getSelectedItem();

                if (obj == null) {
                    Log.e(TAG, "obj is null");
                    return;
                }

                mSysPropName.setText(obj.toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        mToast = Toast.makeText(this, null, Toast.LENGTH_SHORT);

        Log.i(TAG, "CdsUtilityActivity is started");
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    public void onPause() {
        super.onPause();


    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

    }


    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();

    }

    public void onClick(View v) {
        int buttonId = v.getId();

        Log.d(TAG, "button id:" + buttonId);

        switch (buttonId) {
        case R.id.runBtn:
            handleRunCmd();
            break;
        case R.id.setBtn:
            handleSysProp(true);
            break;
        case R.id.getBtn:
            handleSysProp(false);
            break;
        case R.id.stopBtn:
            handleStopCmd();
            break;
        default:
            break;
        }
    }

    private void handleStopCmd() {
        CdsShellExe.finish();
        mOutputScreen.setText("");
    }

    private void handleSysProp(Boolean isSet) {

        String name = mSysPropName.getText().toString();
        String value = mSysPropValue.getText().toString();

        if (name.length() <= 0 || name.length() > SystemProperties.PROP_NAME_MAX) {
            String errMsg = "Please input the correct system property name";
            Log.e(TAG, errMsg);
            mOutputScreen.setText(errMsg);
            return;
        }

        if (isSet) {
            if (name.length() <= 0 || name.length() > SystemProperties.PROP_VALUE_MAX) {
                String errMsg = "Please input the correct system property value";
                Log.e(TAG, errMsg);
                mOutputScreen.setText(errMsg);
                return;
            }

            Log.i(TAG, "name:" + name + "/" + value);
            SystemProperties.set(name, value);
        }

        String result = SystemProperties.get(name);
        Log.i(TAG, "result:" + result);
        mSysPropValue.setText(result);
        mOutputScreen.setText(result);
    }

    private void handleRunCmd() {

        String cmdStr = mCmdLineList.getText().toString();
        Log.d(TAG, "" + cmdStr);

        if (cmdStr == null || cmdStr.length() == 0) {
            mToast.setText("Please input command");
            mToast.show();
            return;
        }

        if (mCmdOption == PING || mCmdOption == RUN || mCmdOption == PINGV6) {
            Log.i(TAG, "Run PING/RUN command");
            new Thread(new Runnable() {
                public void run() {
                    mProgressThread = new ProgressThread(mHandler);

                    try {
                        String cmdLineStr = mCmdLineList.getText().toString();

                        mProgressThread.start();
                        CdsShellExe.execCommand(cmdLineStr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mProgressThread.setState(ProgressThread.STATE_DONE);
                    }
                }
            }).start();
        } else if (mCmdOption == HTTPRESPONSNE) {
            Log.i(TAG, "Run HTTPRESPONSNE command");
            HttpTask httpTask = new HttpTask();
            httpTask.execute(cmdStr);
        } else if (mCmdOption == DNS) {
            Log.i(TAG, "Run DNS command");
            DnsTask dnsTask = new DnsTask();
            dnsTask.execute(cmdStr);
        } else if (mCmdOption == HTTPURL) {
            Log.i(TAG, "Run HTTP URL command");
            HttpUrlTask urlTask = new HttpUrlTask();
            urlTask.execute(cmdStr);
        } else if (mCmdOption == XCAPURL) {
            Log.i(TAG, "Run XCAP URL command");
            XcapUrlTask urlTask = new XcapUrlTask();
            urlTask.execute(cmdStr);
        }
    };

    // Define the Handler that receives messages from the thread and update the
    // progress
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            String output = "";

            switch (msg.what) {
            case MSG_UPDATE_UI:
                output = CdsShellExe.getOutput();
                mOutputScreen.setText(output);
                break;
            default:
                break;
            }
        }
    };


    /** Nested class that performs screen update */
    private class ProgressThread extends Thread {
        Handler mHandler = null;
        private final static int STATE_DONE = 0;
        private final static int STATE_RUNNING = 1;
        private int mState = 0;

        ProgressThread(Handler h) {
            this.mHandler = h;
        }

        public void run() {
            setState(STATE_RUNNING);

            while (STATE_RUNNING == mState) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread Interrupted");
                }

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_UPDATE_UI;
                mHandler.sendMessage(msg);
            }
        }

        /**
        * sets the current state for the thread, used to stop the thread
        */
        public void setState(int state) {
            mState = state;
        }
    }

    private class HttpTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            StringBuilder mOutputString = new StringBuilder();

            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                Map<String, List<String>> headers = urlConnection.getHeaderFields();

                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    mOutputString.append(entry.getKey() + ":" + entry.getValue() + "\r\n");
                }

                Log.i(TAG, "Http result:" + mOutputString);
            } catch (SocketException e) {
                e.printStackTrace();
                mOutputString.append(e.toString());
            } catch (IOException ee) {
                ee.printStackTrace();
                mOutputString.append(ee.toString());
            }

            return mOutputString.toString();
        }

        protected void onPostExecute(String result) {
            mOutputScreen.setText(result);
        }

    }

    /**
      * Class for test HttpUrlConnection API.
      *
      */
    private class HttpUrlTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            StringBuilder mOutputString = new StringBuilder();

            try {

                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("test", "1234".toCharArray());
                    }
                });
                System.setProperty("http.digest.support", "true");

                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                CookieHandler.setDefault(cookieManager);

                String host = params[0];
                final URL url = new URL(host);
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Map<String, List<String>> hf = conn.getHeaderFields();
                for (String key: hf.keySet()) {
                    mOutputString.append(key + ": " + conn.getHeaderField(key) + "\r\n");
                }
                Log.i(TAG, "Http result:" + mOutputString);
            } catch (SocketException e) {
                e.printStackTrace();
                mOutputString.append(e.toString());
            } catch (IOException ee) {
                ee.printStackTrace();
                mOutputString.append(ee.toString());
            }

            return mOutputString.toString();
        }

        protected void onPostExecute(String result) {
            mOutputScreen.setText(result);
        }

    }

    /**
      * Class for test XCAP by using HttpUrlConnection API.
      *
      */
    private class XcapUrlTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            StringBuilder mOutputString = new StringBuilder();

            try {

                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                CookieHandler.setDefault(cookieManager);

                String host = params[0];
                GbaHttpUrlCredential gbaCredential = new GbaHttpUrlCredential(mContext, host);
                Authenticator.setDefault(gbaCredential.getAuthenticator());

                host = host + "/simservs.ngn.etsi.org/users/sip:16262155680@msg.pc.t-mobile.com";
                host = host + "/simservs.xml/~~/simservs/";
                host = host + "originating-identity-presentation-restriction";

                final URL url = new URL(host);
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("X-3GPP-Intended-Identity",
                                        "sip:16262155680@msg.pc.t-mobile.com");
                conn.setRequestProperty("User-Agent", "XCAP Client 3gpp-gba");

                Map<String, List<String>> hf = conn.getHeaderFields();
                for (String key: hf.keySet()) {
                    mOutputString.append(key + ": " + conn.getHeaderField(key) + "\r\n");
                }
                Log.i(TAG, "Http result:" + mOutputString);
            } catch (SocketException e) {
                e.printStackTrace();
                mOutputString.append(e.toString());
            } catch (IOException ee) {
                ee.printStackTrace();
                mOutputString.append(ee.toString());
            }

            return mOutputString.toString();
        }

        protected void onPostExecute(String result) {
            mOutputScreen.setText(result);
        }

    }

    private class DnsTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String mOutputString = "";

            try {
                String hostInfo = "";
                String host = params[0];
                InetAddress addresses[]  = InetAddress.getAllByName(host);

                for (int i = 0; i < addresses.length; i++) {
                    hostInfo = i + ":" + "(" + addresses[i].getHostName()
                                + "/" + addresses[i].getHostAddress()
                                + ")\r\n" + addresses[i].getCanonicalHostName() + "\r\n";
                    mOutputString += hostInfo;
                }

                Log.i(TAG, "Dns result:" + mOutputString);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                mOutputString = e.toString();
            }

            return mOutputString;
        }

        protected void onPostExecute(String result) {
            mOutputScreen.setText(result);
        }
    }

}
