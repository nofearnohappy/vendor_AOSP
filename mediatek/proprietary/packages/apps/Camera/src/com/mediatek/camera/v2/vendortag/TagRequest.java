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
 * MediaTek Inc. (C) 2015. All rights reserved.
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
package com.mediatek.camera.v2.vendortag;

import android.hardware.camera2.CaptureRequest.Key;

/**
 * Contains the configuration for the mtk added processing pipeline.
 * <p>
 * CaptureRequests can be created by using a {@link android.hardware.camera2.CaptureRequest.Builder}
 * instance, obtained by calling {@link android.hardware.camera2.CameraDevice#createCaptureRequest}
 * </p>
 *
 * <p>
 * CaptureRequests are given to {@link android.hardware.camera2.CameraCaptureSession#capture} or
 * {@link android.hardware.camera2.CameraCaptureSession#setRepeatingRequest} to capture images from
 * a camera.
 * </p>
 *
 */
public class TagRequest {
    /**
     * <p>
     * Operating mode for the smile detector unit.
     * </p>
     * <p>
     * Whether smile detection is enabled, and whether it should output just the basic fields or the
     * full set of fields.
     * </p>
     * <p>
     * <b>Possible values:</b>
     * <ul>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_SMILE_MODE_OFF OFF}</li>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_SMILE_MODE_SIMPLE SIMPLE}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Available values for this device:</b><br>
     * {@link TagMetadata#SMILE_AVAILABLE_MODES com.mediatek.facefeature.availablesmiledetectmodes}
     * </p>
     * <p>
     * This key is not available on all devices currently.
     * </p>
     *
     * @see TagMetadata#SMILE_AVAILABLE_MODES
     * @see TagMetadata#MTK_FACE_FEATURE_SMILE_MODE_OFF
     * @see TagMetadata#MTK_FACE_FEATURE_SMILE_MODE_SIMPLE
     */
    public static final Key<Integer> STATISTICS_SMILE_MODE = new Key<Integer>(
            "com.mediatek.facefeature.smiledetectmode", int.class);
    /**
     * <p>
     * Operating mode for the gesture detector unit.
     * </p>
     * <p>
     * Whether gesture detection is enabled, and whether it should output just the basic fields or
     * the full set of fields.
     * </p>
     * <p>
     * <b>Possible values:</b>
     * <ul>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_GESTURE_MODE_OFF OFF}</li>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_GESTURE_MODE_SIMPLE SIMPLE}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Available values for this device:</b><br>
     * {@link TagMetadata#GESTURE_AVAILABLE_MODES com.mediatek.facefeature.availablegesturemodes}
     * </p>
     * <p>
     * This key is not available on all devices currently.
     * </p>
     *
     * @see TagMetadata#GESTURE_AVAILABLE_MODES
     * @see TagMetadata#MTK_FACE_FEATURE_GESTURE_MODE_OFF
     * @see TagMetadata#MTK_FACE_FEATURE_GESTURE_MODE_SIMPLE
     */
    public static final Key<Integer> STATISTICS_GESTURE_MODE = new Key<Integer>(
            "com.mediatek.facefeature.gesturemode", int.class);
    /**
     * <p>
     * Operating mode for the smile detector unit.
     * </p>
     * <p>
     * Whether auto scene detection is enabled, and whether it should output just the basic fields
     * or the full set of fields.
     * </p>
     * <p>
     * <b>Possible values:</b>
     * <ul>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_ASD_MODE_OFF OFF}</li>
     * <li>{@link TagMetadata#MTK_FACE_FEATURE_ASD_MODE_SIMPLE SIMPLE}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Available values for this device:</b><br>
     * {@link TagMetadata#ASD_AVAILABLE_MODES com.mediatek.facefeature.availableasdmodes}
     * </p>
     * <p>
     * This key is not available on all devices currently.
     * </p>
     *
     * @see TagMetadata#ASD_AVAILABLE_MODES
     * @see TagMetadata#MTK_FACE_FEATURE_ASD_MODE_OFF
     * @see TagMetadata#MTK_FACE_FEATURE_ASD_MODE_SIMPLE
     */
    public static final Key<Integer> STATISTICS_ASD_MODE = new Key<Integer>(
            "com.mediatek.facefeature.asdmode", int.class);
    /**
     * <p>
     * Operating mode for the 3DNR detector unit.
     * </p>
     * <p>
     * Whether 3DNR is enabled.
     * </p>
     * <p>
     * <b>Possible values:</b>
     * <ul>
     * <li>{@link TagMetadata#MTK_3DNR_MODE_OFF OFF}</li>
     * <li>{@link TagMetadata#MTK_3DNR_MODE_ON ON}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Available values for this device:</b><br>
     * {@link TagMetadata#NR3D_AVAILABLE_MODES com.mediatek.nrfeature.available3dnrmodes}
     * </p>
     * <p>
     * This key is not available on all devices currently.
     * </p>
     *
     * @see TagMetadata#NR3D_AVAILABLE_MODES
     * @see TagMetadata#MTK_3DNR_MODE_OFF
     * @see TagMetadata#MTK_3DNR_MODE_ON
     */
    public static final Key<Integer> STATISTICS_3DNR_MODE = new Key<Integer>(
            "com.mediatek.nrfeature.3dnrmode", int.class);
}
