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

package com.mediatek.dm.util;

import android.content.Context;
import android.util.Log;

import com.mediatek.dm.DmConst;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileLogger {
    private static final String CLASS_TAG = DmConst.TAG.LOG_TAG_PREFIX + "[FileLogger]";
    private static final String LOG_FILENAME = "tool.log";
    private static final String DATE_FORMAT = "yy-MM-dd HH:mm:ss";

    private static FileLogger sInstance;

    public static synchronized FileLogger getInstance(Context cxt) {
        if (sInstance == null) {
            sInstance = new FileLogger();
            sInstance.init(cxt);
        }
        return sInstance;
    }

    OutputStreamWriter mOut;

    synchronized boolean init(Context context) {
        boolean result = true;
        try {
            FileOutputStream logFile = context.openFileOutput(LOG_FILENAME, Context.MODE_APPEND);
            mOut = new OutputStreamWriter(logFile);
            Log.d(CLASS_TAG, "File logger inited");
        } catch (IOException e) {
            Log.d(CLASS_TAG, "FileLogger.init - caught exception: " + e);
            result = false;
        }
        return result;
    }

    public void logMsg(String message) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        int pid = android.os.Process.myPid();
        int tid = android.os.Process.myTid();

        try {
            String msg = String.format("%s[%d][%d]: %s\n", sdf.format(cal.getTime()), pid, tid,
                    message);
            mOut.write(msg);
            mOut.flush();
        } catch (IOException e) {
            Log.d(CLASS_TAG, "FileLogger.prefixMsg - caught exception: " + e);
            e.printStackTrace();
        }
    }

    synchronized void term() {
        try {
            mOut.close();
            Log.d(CLASS_TAG, "Close file logger");
        } catch (IOException e) {
            Log.d(CLASS_TAG, "FileLogger.term - caught exception: " + e);
            e.printStackTrace();
        }
    }

}
