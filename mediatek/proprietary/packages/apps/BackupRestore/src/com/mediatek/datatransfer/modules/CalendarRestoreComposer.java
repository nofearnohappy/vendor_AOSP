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

package com.mediatek.datatransfer.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;

import com.mediatek.datatransfer.utils.Constants.ModulePath;
import com.mediatek.datatransfer.utils.ModuleType;
import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.vcalendar.VCalParser;
import com.mediatek.vcalendar.VCalStatusChangeOperator;

public class CalendarRestoreComposer extends Composer implements VCalStatusChangeOperator {
    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/CalendarRestoreComposer";
    private static final String COLUMN_ID = "_id";
    private static final Uri calanderEventURI = CalendarContract.Events.CONTENT_URI;
    private int mIndex;
    private boolean mResult = true;

    VCalParser mCalParser;
    private int mCount;

    /**
     * Creates a new <code>CalendarRestoreComposer</code> instance.
     *
     * @param context a <code>Context</code> value
     */
    public CalendarRestoreComposer(Context context) {
        super(context);
    }


    /**
     * Describe <code>getModuleType</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getModuleType() {
        return ModuleType.TYPE_CALENDAR;
    }

    /**
     * Describe <code>getCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getCount() {
        MyLogger.logD(CLASS_TAG, "getCount():" + mCount);
        return mCount;
    }

    /**
     * Describe <code>init</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean init() {
        boolean result = false;
        mCalParser = null;
        String fileName = mParentFolderPath + File.separator + ModulePath.FOLDER_CALENDAR
                + File.separator + ModulePath.NAME_CALENDAR;
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            mCount = getCalendarEventNum(fileName);
            if (fileName.contains("#")) {
                return false;
            }
            mCalParser = new VCalParser(Uri.parse("file://" + fileName), mContext, this);
            mIndex = 0;
            result = true;
        }

        MyLogger.logD(CLASS_TAG, "init():" + result);
        return result;
    }

    private int getCalendarEventNum(String fileName) {
        int calEventNum = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String str = null;
            while ((str = reader.readLine()) != null) {
                if (str.contains("END:VEVENT")) {
                    ++calEventNum;
                }
            }
        } catch (IOException e) {
            MyLogger.logE(CLASS_TAG, "getCalendarEventNum read file failed");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    MyLogger.logE(CLASS_TAG, "getCalendarEventNum close reader failed");
                }
            }
        }

        return (calEventNum == 0) ? -1 : calEventNum;
    }

    /**
     * Describe <code>isAfterLast</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAfterLast() {
        boolean result = false;
        if (mCount > -1) {
            result = (mIndex >= mCount) ? true : false;
        }

        MyLogger.logD(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * Describe <code>composeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean composeOneEntity() {
        return implementComposeOneEntity();
    }


    /**
     * Describe <code>implementComposeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean implementComposeOneEntity() {
        MyLogger.logD(CLASS_TAG, "implementComposeOneEntity():" + mIndex++);
        return mResult;
    }

    /**
     * Describe <code>deleteAllCalendarEvents</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    private boolean deleteAllCalendarEvents() {
        String selection = CalendarContract.Events._ID + ">0";
        mContext.getContentResolver().delete(calanderEventURI, selection, null);

        MyLogger.logD(CLASS_TAG, "deleteAllCalendarEvents() and all events will be deleted!");
        return true;
    }

    public void onStart() {
        mResult = true;
        deleteAllCalendarEvents();
        if (mCalParser != null) {
            // Note: all the restoration work are done here
            mCalParser.startParse();
        } else {
            // init() failed, jump over
            super.onStart();
        }

        MyLogger.logD(CLASS_TAG, "onStart()");
    }


    public void onEnd() {
        if (mCalParser != null) {
            mCalParser.close();
        }

        super.onEnd();
        MyLogger.logD(CLASS_TAG, "onEnd()");
    }

    /**
     * Will be called when compose/parse started.
     * @param totalCount total count of items
     */
    @Override
    public void vCalOperationStarted(int totalCount) {
        mCount = totalCount;
        super.onStart();
        MyLogger.logD(CLASS_TAG, "vCalOperationStarted():" + totalCount);
    }

    /**
     * Will be called when the compose/parse operation finished
     *
     * @param successCnt
     *            the successful handled count
     * @param totalCnt
     *            total count
     */
    @Override
    public void vCalOperationFinished(int successCnt, int totalCnt, Object obj) {
        mResult = true;
        MyLogger.logD(
                CLASS_TAG,
                "vCalOperationFinished(): success " + successCnt + ", total " + totalCnt);
    }

    /**
     * Will be called when the process status update
     *
     * @param currentCount
     *            current handled count
     * @param totalCount
     *            total count
     */
    @Override
    public void vCalProcessStatusUpdate(int currentCount, int totalCount) {
        mCount = totalCount;
        increaseComposed(true);
        MyLogger.logD(
                CLASS_TAG,
                "vCalProcessStatusUpdate(): current " + currentCount + ", total " + totalCount);
    }

    /**
     * Will be called when the cancel request has been finished.
     *
     * @param finishedCnt
     *            the count has been finished before the cancel operation
     * @param totalCnt
     *            total count
     */
    @Override
    public void vCalOperationCanceled(int finishedCnt, int totalCnt) {
        throw new Error("vCalOperationCanceled should not be called.");
    }

    /**
     * Will be called when exception occurred.
     *
     * @param finishedCnt
     *            the count has been finished before the exception occurred.
     * @param totalCnt
     *            total count
     * @param type
     *            the exception type.
     */
    @Override
    public void vCalOperationExceptionOccured(int finishedCnt, int totalCnt, int type) {
        mResult = false;
        MyLogger.logD(
                CLASS_TAG,
                "vCalOperationExceptionOccured():finishedCnt:" + finishedCnt
                        + "; totalCnt:" + totalCnt + "; type: " + type);
    }
}

