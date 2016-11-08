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

package com.mediatek.mediatekdm.test.server;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.mediatekdm.test.server.MockServerService.TestResult;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TemplateDrivenBinder extends IMockServer.Stub {
    private static final String TAG = "MDMTest/TemplateDrivenBinder";
    public static final String SERVER_USERNAME = "OMADM";
    public static final String SERVER_PASSWORD = "mvpdm";
    public static final String CLIENT_USERNAME = "mvpdm";
    public static final String CLIENT_PASSWORD = "mvpdm";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TIMEOUT = "timeout";
    public static final int DEFAULT_TIMEOUT = 300;
    public static final String KEY_BREAKPOINT = "breakpoint";
    public static final String KEY_BREAKTIME = "breaktime";
    public static final int ACCEPT_TIMEOUT = 5 * 60;
    private final int mPort;
    private final Context mContext;
    private final Map<String, String> mParameters;
    private MockServerThread mServerThread;
    private Semaphore mSemaphore;
    private BlockingQueue<Integer> mResultQueue;

    public TemplateDrivenBinder(int port, Context context) {
        mPort = port;
        mContext = context;
        mParameters = new HashMap<String, String>();
        mSemaphore = new Semaphore(1);
        mSemaphore.drainPermits();
        mResultQueue = new ArrayBlockingQueue<Integer>(1);
    }

    @Override
    public String getHost() {
        return MockServerService.HOST;
    }

    @Override
    public int getPort() {
        return mPort;
    }

    @Override
    public void start() throws RemoteException {
        Log.d(TAG, "start() start");
        mServerThread = new MockServerThread();
        mServerThread.setDaemon(true);
        mServerThread.start();
        Log.d(TAG, "start() end");
    }

    @Override
    public void stop() throws RemoteException {
        Log.d(TAG, "stop() start");
        mServerThread.closeSocket();
        syncWait();
        Log.d(TAG, "stop() end");
    }

    @Override
    public String getParameter(String key) throws RemoteException {
        return mParameters.get(key);
    }

    @Override
    public String setParameter(String key, String value) throws RemoteException {
        return mParameters.put(key, value);
    }

    protected class MockServerThread extends Thread implements
            TemplateHttpRequestHandler.IControlPanel {
        private ServerSocket mServerSocket = null;
        private Socket mClientSocket = null;
        private boolean mRequestExit;
        private int mRequestCount = 0;
        private int mTestResult = TestResult.Unknown;
        private final int mBreakPoint;
        private final int mBreakTime;

        public MockServerThread() {
            super();
            Log.d(TAG,
                    "MockServerThread(" + android.os.Process.myPid() + ", "
                            + android.os.Process.myTid() + ")");
            mRequestExit = false;
            try {
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(mPort));
                mServerSocket.setSoTimeout(ACCEPT_TIMEOUT * 1000);
                mBreakPoint = mParameters.containsKey(KEY_BREAKPOINT) ? Integer
                        .parseInt(mParameters.get(KEY_BREAKPOINT)) : -1;
                mBreakTime = mParameters.containsKey(KEY_BREAKTIME) ? Integer.parseInt(mParameters
                        .get(KEY_BREAKTIME)) : -1;
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "MockServerThread.run(" + android.os.Process.myPid() + ", "
                    + android.os.Process.myTid() + ")");
            Log.d(TAG, "Before loop()");
            while (!Thread.interrupted() && mTestResult == TestResult.Unknown) {
                Log.w(TAG, "In loop()");
                try {
                    Log.d(TAG, "Waiting for client with timeout " + mServerSocket.getSoTimeout()
                            + " ...");
                    mClientSocket = null;
                    try {
                        mClientSocket = mServerSocket.accept();
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Accept timed out");
                        mTestResult = TestResult.Fail;
                        break;
                    }

                    Log.d(TAG, "Incoming connection from " + mClientSocket.getInetAddress());

                    if (mBreakPoint == (mRequestCount + 1)) {
                        Log.d(TAG, "Simulate network error.");
                        mClientSocket.close();
                        mRequestCount += 1;
                        if (mBreakTime != -1) {
                            try {
                                Thread.sleep(mBreakTime);
                            } catch (InterruptedException e) {
                                throw new Error(e);
                            }
                        }
                        continue;
                    }

                    // Build HTTP parameters.
                    HttpParams parameters = new BasicHttpParams();
                    parameters.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000); // 30s
                    parameters.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024); // 8KB
                    parameters.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK,
                            false);
                    parameters.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
                    parameters.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "DmMockServer/1.1");
                    // Build HTTP protocol processor.
                    BasicHttpProcessor processor = new BasicHttpProcessor();
                    processor.addInterceptor(new ResponseDate());
                    processor.addInterceptor(new ResponseServer());
                    processor.addInterceptor(new ResponseContent());
                    processor.addInterceptor(new ResponseConnControl());
                    // Build HTTP service.
                    HttpService httpService = new HttpService(processor,
                            new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
                    // Build request handlers.
                    HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
                    registry.register("*", new TemplateHttpRequestHandler(
                            mParameters.get(KEY_TYPE), Integer.toString(mPort), mContext, this));
                    httpService.setHandlerResolver(registry);
                    // Build HTTP connection instance.
                    DefaultHttpServerConnection connection = new DefaultHttpServerConnection();
                    connection.bind(mClientSocket, parameters);

                    HttpContext context = new BasicHttpContext(null);
                    try {
                        Log.d(TAG, "Start to communicate with client ...");
                        while (!Thread.interrupted() && connection.isOpen()) {
                            Log.e(TAG, "Before handle request ...");
                            httpService.handleRequest(connection, context);
                            Log.e(TAG, "After handle request ...");
                        }
                        Log.d(TAG, "Stop to communicate with client ...");
                    } catch (ConnectionClosedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Server caught an exception:" + e.getClass());
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Server caught an exception:" + e.getClass());
                        break;
                    } catch (HttpException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Server caught an exception:" + e.getClass());
                        break;
                    } finally {
                        try {
                            connection.shutdown();
                        } catch (IOException ignore) {
                            ignore.printStackTrace();
                        }
                    }
                } catch (SocketException e) {
                    Log.e(TAG, "A: Loop socket interrupted");
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    break;
                } catch (InterruptedIOException e) {
                    Log.e(TAG, "B: Loop IO interrupted");
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "C: Loop IO interrupted");
                    e.printStackTrace();
                    break;
                }
            }

            if (exitRequested()) {
                Log.e(TAG, "Exit requested.");
            }

            syncNotify();

            notifyTestResult(mTestResult);

            Log.d(TAG, "After loop()");
        }

        public synchronized void closeSocket() {
            Log.d(TAG, "closeSocket()");
            try {
                mRequestExit = true;
                mServerSocket.close();
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        private synchronized boolean exitRequested() {
            return mRequestExit;
        }

        @Override
        public void notify(int result) {
            mTestResult = result;
        }

        @Override
        public int getCount() {
            return mRequestCount;
        }

        @Override
        public void incCount() {
            mRequestCount += 1;
        }

        @Override
        public boolean testRetry() {
            return (mBreakPoint != -1);
        }
    }

    String put(String key, String value) {
        return mParameters.put(key, value);
    }

    String get(String key) {
        return mParameters.get(key);
    }

    private void syncWait() {
        Log.d(TAG, "syncWait()");
        while (true) {
            try {
                mSemaphore.acquire();
                Log.d(TAG, "acquire() returned");
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
    }

    private void syncNotify() {
        Log.d(TAG, "syncNotify()");
        mSemaphore.release();
    }

    @Override
    public String getId() throws RemoteException {
        return Integer.toString(mPort);
    }

    @Override
    public int getResult() throws RemoteException {
        Log.d(TAG, "TemplateDrivenBinder.getResult(" + android.os.Process.myPid() + ", "
                + android.os.Process.myTid() + ")");
        int result = TestResult.Fail;
        try {
            int to = DEFAULT_TIMEOUT;
            if (mParameters.containsKey(KEY_TIMEOUT)) {
                to = Integer.parseInt(mParameters.get(KEY_TIMEOUT));
            }
            Log.d(TAG, "wait begin for " + to / 1000 + " Seconds");
            result = mResultQueue.poll(to, TimeUnit.MILLISECONDS);
            Log.d(TAG, "wait end");
        } catch (InterruptedException e) {
            Log.e(TAG, "waitTestResult is interrupted.");
            new Error(e);
        }
        Log.d(TAG, "waitTestResult result is " + result);
        return result;
    }

    private void notifyTestResult(int result) {
        Log.d(TAG, "notifyTestResult: " + result);
        try {
            mResultQueue.put(result);
            Log.d(TAG, "result has been put to queue");
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }
}
