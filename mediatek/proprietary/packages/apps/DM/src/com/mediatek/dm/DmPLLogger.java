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

package com.mediatek.dm;

import android.content.Context;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;

import com.redbend.android.VdmLogLevel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DmPLLogger /* implements PLLogger */ {

    private FileLogger mFileLog;

    DmPLLogger(Context context) {
        mFileLog = new FileLogger();
        if (!mFileLog.init(context)) {
            mFileLog = null;
        }
    }

    @Override
    public void finalize() {
        if (mFileLog != null) {
            mFileLog.term();
            mFileLog = null;
        }
    }

    // PLLogger implementation
    public boolean init(Context context) {
        return (mFileLog != null);
    }

    public void logMsg(VdmLogLevel level, String message) {
        int priority;

        switch (level) {
        case ERROR:
            priority = Log.ERROR;
            break;
        case WARNING:
            priority = Log.WARN;
            break;
        case NOTICE:
            priority = Log.INFO;
            break;
        case INFO:
            priority = Log.DEBUG;
            break;
        case DEBUG:
        default:
            priority = Log.VERBOSE;
            break;
        }

        logMsg(priority, message);
    }

    public void term() {

    }

    public void logMsg(int priority, String message) {

        // Append thread id to tag
        String threadId = Thread.currentThread().getName();
        String logTag = new StringBuilder(TAG.PL)
                .append(" (")
                .append(threadId)
                .append(") ")
                .toString();

        // Log tag + message to file.
        if (mFileLog != null) {
            mFileLog.logMsg(logTag + message);
            Log.i(TAG.PL, logTag + message);
        } else {
            Log.i(TAG.PL, "No " + logTag + message);
        }

        Log.println(priority, logTag, message);
    }

    // private static final String TAG = "vDMC";

    private class FileLogger {

        OutputStreamWriter mOut;
        private static final String LOG_FILENAME = "dm.log";

        synchronized boolean init(Context context) {
            boolean result = true;
            try {
                FileOutputStream logFile = context.openFileOutput(LOG_FILENAME,
                        Context.MODE_PRIVATE);
                mOut = new OutputStreamWriter(logFile);
                Log.i(TAG.PL, "File logger inited");
            } catch (FileNotFoundException e) {
                Log.e(TAG.PL, "FileLogger.init - caught exception: " + e);
                result = false;
            }
            return result;
        }

        public void logMsg(String message) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            try {
                mOut.write(new StringBuilder(sdf.format(cal.getTime()))
                    .append(" ")
                    .append(message).append("\n").toString());
                mOut.flush();
            } catch (IOException e) {
                Log.e(TAG.PL, "FileLogger.prefixMsg - caught exception: " + e);
                e.printStackTrace();
            }
        }

        synchronized void term() {
            try {
                mOut.close();
                Log.i(TAG.PL, "Close file logger");
            } catch (IOException e) {
                Log.e(TAG.PL, "FileLogger.term - caught exception: " + e);
                e.printStackTrace();
            }
        }
    }
}
