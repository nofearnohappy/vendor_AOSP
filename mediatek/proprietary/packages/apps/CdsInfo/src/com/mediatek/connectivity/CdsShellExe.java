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

package com.mediatek.connectivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.util.Log;

public class CdsShellExe {

    static final String TAG = "CDSINFO/Shell";

    public static String ERROR = "ERROR";
    private static StringBuilder mSb = new StringBuilder("");
    private static Process mProc;

    public static String getOutput()
    {
        return mSb.toString();
    }

    static void finish() {
        if (mProc != null) {
            mProc.destroy();
        }
    }

    public static int execCommand(String command) throws IOException {

        Runtime runtime = Runtime.getRuntime();
        Log.d(TAG, "execCommand>>:" + command);
        mProc = runtime.exec(command);
        Log.d(TAG, "execCommand<<");
        final BufferedReader errBufReader;
        final BufferedReader datBufReader;

        mSb.delete(0, mSb.length());

        try {
            Log.d(TAG, "waitFor:" + command);
            errBufReader = new BufferedReader(
                                    new InputStreamReader(mProc.getErrorStream()));
            datBufReader = new BufferedReader(
                                    new InputStreamReader(mProc.getInputStream()));

            Thread errThread = new Thread() {
                public void run() {
                    String line = null;
                    try {
                        while ((line = errBufReader.readLine()) != null) {
                            mSb.append(line);
                            mSb.append('\n');
            }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (errBufReader != null) {
                                errBufReader.close();
            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            errThread.start();

            Thread datThread = new Thread() {
                public void run() {
                    String line = null;
                    try {
                        while ((line = datBufReader.readLine()) != null) {

                            mSb.append(line);
                            mSb.append('\n');
                            if (mSb.length() > 1024 * 1024 * 5) {
                                mSb.delete(0, mSb.length());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (datBufReader != null) {
                                datBufReader.close();
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                }
            }
            };
            datThread.start();

            if (mProc.waitFor() != 0) {
                Log.i(TAG, "exit value = " + mProc.exitValue());
                mSb.append(ERROR);
            }
            Log.d(TAG, "execCommand done");
            return 0;
        } catch (InterruptedException e) {
            Log.i(TAG, "exe fail " + e.toString());
            mSb.append(ERROR);
            return -1;
        }
    }
}
