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

package com.mediatek.common.sms;

import android.os.Handler;

public interface IConcatenatedSmsFwkExt {

    /**
     * Event to dispatch concatenated rest sms segments.
     *
     * @internal
     */
    public static final int EVENT_DISPATCH_CONCATE_SMS_SEGMENTS = 3001;

    /**
     * Updated segments tag to put on the intent extra value.
     *
     * @internal
     */
    public static final String UPLOAD_FLAG_TAG = "upload_flag";

    /**
     * Default segments to dispatch flag type.
     * Application doesn't need to check this value.
     *
     */
    public static final int UPLOAD_FLAG_NONE = 0;

    /**
     * New segments to dispatch flag type.
     *
     * @internal
     */
    public static final int UPLOAD_FLAG_NEW = 1;

    /**
     * Updated segments to dispatch flag type.
     *
     * @internal
     */
    public static final int UPLOAD_FLAG_UPDATE = 2;

    /**
     * Clear all segments on the database intent.
     *
     * @internal
     */
    public static final String ACTION_CLEAR_OUT_SEGMENTS = "android.sms.ACTION_CLEAR_OUT_SEGMENTS";

    /**
     * Time out criteria.
     *
     * @internal
     */
    public static final int OUT_OF_DATE_TIME = 12 * 60 * 60 * 1000;

    public boolean isFirstConcatenatedSegment(String address, int refNumber);

    public boolean isLastConcatenatedSegment(String address, int refNumber, int msgCount);

    public void startTimer(Handler h, Object r);

    public void cancelTimer(Handler h, Object r);

    public void refreshTimer(Handler h, Object r);

    public TimerRecord queryTimerRecord(String address, int refNumber, int msgCount);

    public byte[][] queryExistedSegments(TimerRecord record);

    public void deleteExistedSegments(TimerRecord record);

    public int getUploadFlag(TimerRecord record);

    /*
    * set upload_flag as UPLOAD_FLAG_UPDATE
    */
    public void setUploadFlag(TimerRecord record);

    /**
     * Set the correct phone id.
     *
     * @param phoneId related phone id
     *
     */
    public void setPhoneId(int phoneId);
}
