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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.mediatek.camera.v2.setting;

import android.media.CameraProfile;
import android.util.Log;
import android.util.Size;

import com.mediatek.camcorder.CamcorderProfileEx;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;

import java.util.List;


public class SettingDataBase {
    private static final String TAG = "SettingDataBase";

    public static final int UNKNOWN = -1;

    public static final int STATE_D0 = 100; // disable
    public static final int STATE_E0 = 200; // enable
    public static final int STATE_R0 = 300; // reset value 0
    public static final int STATE_R1 = 301; // reset value 1
    public static final int STATE_R2 = 302; // reset value 2
    public static final int STATE_R3 = 303; // reset value 3
    public static final int STATE_R4 = 304; // reset value 4
    public static final int STATE_R5 = 305; // reset value 5
    public static final int STATE_R6 = 306; // reset value 6
    public static final int STATE_R7 = 307; // reset value 7

    private static final int[][] MATRIX_RESTRICTION_STATE = new int[SettingKeys.SETTING_COUNT][];

    static {
//normal hdr facebeauty panorama mav asd photopip video videopip smile gesture refecous
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_FLASH]                 = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //0: flash
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_DUAL_CAMERA]           = new int[]{
                STATE_E0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_E0,
                STATE_E0, STATE_E0, STATE_D0, STATE_E0, STATE_D0}; //1: dual camera
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_EXPOSURE]              = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //2: ev
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SCENCE_MODE]           = new int[]{
                STATE_E0, STATE_R1, STATE_E0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //3: scence mode
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_WHITE_BALANCE]         = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_R0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0}; //4: white balance
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SLOW_MOTION]           = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_R0, STATE_R0,
                STATE_D0, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //5: slow motion
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_COLOR_EFFECT]          = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //6: color effect
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SELF_TIMER]            = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0,
                STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0}; //7: self timer
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_ZSD]                   = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_E0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0}; //8: zsd
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_RECORD_LOCATION]       = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //10: geo tag
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_PICTURE_RATIO]         = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //21: picture ratio
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_PICTURE_SIZE]          = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //11: picture size
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_ISO]                   = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //12: ISO
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_AIS]                   = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0}; //1//ais
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_ANTI_FLICKER]          = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //14: Anti flicker
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_VIDEO_STABLE]          = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //15: video stable
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_MICROPHONE]            = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //16 microphone
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_AUDIO_MODE]            = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //17 audio mode
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_TIME_LAPSE]            = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //18 timelapse
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_VIDEO_QUALITY]         = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //20 video quality
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY]    = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //20 video quality
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_VOICE]                 = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //22 voice
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_3DNR]                  = new int[]{
                STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_R0,
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0}; //23: 3DNR
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SHARPNESS]             = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //30: image adjustment sharpness
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_HUE]                   = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //31: image adjustment hue
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SATURATION]            = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //32: image adjustment saturation
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_BRIGHTNESS]            = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //33: image adjustment brightness
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_CONTRAST]              = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //34: image adjustment contrast
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_FAST_AF]               = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //33: Fast AF
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_DISTANCE]              = new int[]{
                STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0}; //34: Distance info
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_CAMERA_MODE]           = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R1, STATE_R1, STATE_R0, STATE_R0, STATE_R0}; //21 camera mode
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_CAPTURE_MODE]          = new int[]{
                STATE_R0, STATE_R0, STATE_R2, STATE_R7, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0}; //22: capture mode
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_CONTINUOUS_NUM]        = new int[]{
                STATE_R0, STATE_D0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_D0, STATE_D0, STATE_R0};
                //23: initial continuous number, it is different from 6 for here is real number,
                //6 is enable/disable state.
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_RECORDING_HINT]        = new int[]{
                STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R1, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //24 recording hint
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_JPEG_QUALITY]          = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //25 jpeg quality
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_FACEBEAUTY_SMOOTH]     = new int[]{
                STATE_E0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0,
                STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_D0}; //47: face beauty smooth
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = new int[]{
                STATE_E0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0,
                STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_D0}; //48: face beauty skin-color
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_FACEBEAUTY_SHARP]      = new int[]{
                STATE_E0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0,
                STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_D0}; //49: face beauty sharp
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_CAMERA_FACE_DETECT]    = new int[]{
                STATE_E0, STATE_E0, STATE_R1, STATE_R0, STATE_R1, STATE_E0,
                STATE_R0, STATE_R0, STATE_R1, STATE_R1, STATE_E0}; //50: face detection
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_MULTI_FACE_MODE]       = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0,
                STATE_E0, STATE_D0, STATE_E0, STATE_E0, STATE_E0}; //55: multi-face mode
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_HDR]                   = new int[]{
                STATE_E0, STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0}; //51: hdr
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_SMILE_SHOT]            = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0,
                STATE_R0, STATE_R0, STATE_E0, STATE_R0, STATE_R0}; //52: smile shot
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_ASD]                   = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0}; //53: asd
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_MUTE_RECORDING_SOUND]  = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_R0}; //54 enable recording sound
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_GESTURE_SHOT]          = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0,
                STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_R0}; //55 gesture shot
        MATRIX_RESTRICTION_STATE[SettingKeys.ROW_SETTING_DNG]                   = new int[]{
                STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0}; //56 dng
    }


    private static final int[] RESTRCTION_SETTING_INDEX = new int[] {
        SettingKeys.ROW_SETTING_NORMAL,
        SettingKeys.ROW_SETTING_HDR,
        SettingKeys.ROW_SETTING_FACE_BEAUTY,
        SettingKeys.ROW_SETTING_PANORAMA,
        SettingKeys.ROW_SETTING_ASD,
        SettingKeys.ROW_SETTING_PHOTO_PIP,
        SettingKeys.ROW_SETTING_VIDEO,
        SettingKeys.ROW_SETTING_VIDEO_PIP,
        SettingKeys.ROW_SETTING_SMILE_SHOT,
        SettingKeys.ROW_SETTING_GESTURE_SHOT,
        SettingKeys.ROW_SETTING_REFOCUS,
    };

    private static final String[][] RESET_STATE_VALUE = new String[SettingKeys.SETTING_COUNT][];
    static {
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_FLASH] = new String[]{"off"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_DUAL_CAMERA] = new String[]{"0"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_EXPOSURE] = new String[]{"0", "1"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SCENCE_MODE] = new String[]{"auto", "hdr"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_WHITE_BALANCE] = new String[]
                {"auto",
                 "daylight",
                 "incandescent"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_AIS] = new String[]{"off"}; //
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_COLOR_EFFECT] = new String[]{"none"}; //none
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SELF_TIMER] = new String[]{"0"}; //none
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_ZSD] = new String[]{"off"}; //none
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_RECORD_LOCATION] = null; //none
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_PICTURE_SIZE] = null; //none need check
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_ISO] = new String[]{"auto"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_ANTI_FLICKER] = null; //none
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_VIDEO_STABLE] = new String[]{"off"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_3DNR] = new String[]{"off"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_MICROPHONE] = new String[]{"on", "off"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_AUDIO_MODE] = new String[]{"normal"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_TIME_LAPSE] = new String[]{"0"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_VIDEO_QUALITY] = new String[]{"9"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY]
                = new String[]{"21"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SHARPNESS] = new String[]{
                "middle", "low", "high"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_FAST_AF] = new String[]{"off"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_DISTANCE] = new String[]{"off"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_HUE] = new String[]{"middle"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SATURATION]
                = new String[]{"middle", "low"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_BRIGHTNESS]
                = new String[]{"middle"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_CONTRAST]
                = new String[]{"middle"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_FACEBEAUTY_SMOOTH]
                = new String[]{"0"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_FACEBEAUTY_SKIN_COLOR]
                = new String[]{"0"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_FACEBEAUTY_SHARP]
                = new String[]{"0"}; //middle
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_CAMERA_FACE_DETECT]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_MULTI_FACE_MODE]
                = new String[]{"Single", "Multi", "OFF"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_RECORDING_HINT]
                = new String[]{Boolean.toString(false), Boolean.toString(true)};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_CAPTURE_MODE]
                = new String[]{
                "normal",
                "hdr",
                "face_beauty",
                "asd",
                "smileshot",
                "burstshot",
                "evbracketshot",
                "autorama"
        };

        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_CONTINUOUS_NUM]        = new String[]{"40"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_JPEG_QUALITY]          = new String[]{
                Integer.toString(CameraProfile.QUALITY_HIGH),
                Integer.toString(CameraProfile.QUALITY_HIGH), //should be rechecked
        };
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_CAMERA_MODE]
                = new String[]{"1", "2"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_VOICE]
                = new String[]{"off"}; // off
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SLOW_MOTION]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_HDR]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_SMILE_SHOT]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_ASD]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_GESTURE_SHOT]
                = new String[]{"off", "on"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_MUTE_RECORDING_SOUND]
                = new String[]{"0", "1"};
        RESET_STATE_VALUE[SettingKeys.ROW_SETTING_DNG]
                = new String[]{"off", "on"};
    }

    private static final String PICTURE_RATIO_16_9 = "1.7778";
    private static final String PICTURE_RATIO_5_3 = "1.6667";
    private static final String PICTURE_RATIO_3_2 = "1.5";
    private static final String PICTURE_RATIO_4_3 = "1.3333";

    private static final String[] PICTURE_SIZE_4_3 = new String[] {
        "320x240",
        "640x480",
        "1024x768",
        "1280x960",
        "1600x1200",
        "2048x1536",
        "2560x1920",
        "3264x2448",
        "3600x2700",
        "3672x2754",
        "4096x3072",
        "4160x3120",
        "4608x3456",
        "5120x3840",
        "5632x4224",
    };
    private static final String[] PICTURE_SIZE_16_9 = new String[] {
        "256x144",
        "512x288",
        "1280x720",
        "1600x912",
        "1920x1088",
        "2048x1152",
        "2560x1440",
        "3072x1728",
        "3328x1872",
        "4096x2304",
        "4608x2592",
        "5120x2880",
        "5280x2992",
        "5312x2944",
    };
    private static final String[] PICTURE_SIZE_5_3 = new String[] {
        "400x240",
        "560x336",
        "1280x768",
        "1600x960",
        "2880x1728",
        "3600x2160",
    };
    private static final String[] PICTURE_SIZE_3_2 = new String[] {
        "1024x688",
        "1280x864",
        "1440x960",
        "2048x1360",
        "2560x1712"
    };

    public static final String[] VIDEO_QUALITY_WHITHOUT_FINE = new String[] {
        Integer.toString(CamcorderProfileEx.QUALITY_HIGH) ,
        Integer.toString(CamcorderProfileEx.QUALITY_MEDIUM),
        Integer.toString(CamcorderProfileEx.QUALITY_LOW),
     };

    private static final String[] VIDEO_SUPPORT_SCENE_MODE = new String[] {
        "auto",
        "night",
        "sunset",
        "party",
        "portrait",
        "landscape",
        "night-portrait",
        "theatre",
        "beach",
        "snow",
        "steadyphoto",
        "sports",
        "candlelight"
    };

    private static final Restriction[] RESTRICTIOINS = new Restriction[]{
        new Restriction(SettingKeys.ROW_SETTING_TIME_LAPSE) //should be checked.
            .setValues("1000", "1500", "2000", "2500", "3000", "5000", "10000")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_MICROPHONE)
                    .setEnable(false)
                    .setValues("off"),
                new Restriction(SettingKeys.ROW_SETTING_AUDIO_MODE)
                    .setEnable(false)
                    .setValues("normal")),

        new Restriction(SettingKeys.ROW_SETTING_SLOW_MOTION)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_VIDEO_STABLE)
                    .setEnable(false)
                    .setValues("off"),
                new Restriction(SettingKeys.ROW_SETTING_TIME_LAPSE)
                    .setEnable(false)
                    .setValues("0")),

        new Restriction(SettingKeys.ROW_SETTING_SLOW_MOTION)
            .setType(Restriction.TYPE_SETTING)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_SCENCE_MODE)
                    .setEnable(false)
                    .setValues("auto"),
                new Restriction(SettingKeys.ROW_SETTING_ASD)
                    .setEnable(false)
                    .setValues("off"),
                new Restriction(SettingKeys.ROW_SETTING_3DNR)
                    .setEnable(false)
                    .setValues("off"),
                new Restriction(SettingKeys.ROW_SETTING_HDR)
                    .setEnable(false)
                    .setValues("off")),

        new Restriction(SettingKeys.ROW_SETTING_FAST_AF)
              .setValues("on")
              .setRestrictions(new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
                  .setEnable(true)
                  .setValues(PICTURE_RATIO_16_9)),

       new Restriction(SettingKeys.ROW_SETTING_DISTANCE)
                .setValues("on")
                .setRestrictions(new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
                   .setEnable(true)
                   .setValues(PICTURE_RATIO_16_9)),

        new Restriction(SettingKeys.ROW_SETTING_MICROPHONE) //should be checked.
            .setValues("off")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_AUDIO_MODE)
                    .setEnable(false)
                    .setValues("normal")),

        new Restriction(SettingKeys.ROW_SETTING_VIDEO_QUALITY)//23 means quality is Fine(4k2k)
            .setValues("23")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_3DNR)
                    .setEnable(false)
                    .setValues("off")),

        new Restriction(SettingKeys.ROW_SETTING_HDR)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_VIDEO_QUALITY)
                    .setEnable(true)
                    .setValues(VIDEO_QUALITY_WHITHOUT_FINE)),

        new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(PICTURE_RATIO_4_3)
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_PICTURE_SIZE)
                    .setEnable(true)
                    .setValues(PICTURE_SIZE_4_3)),

        new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(PICTURE_RATIO_16_9)
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_PICTURE_SIZE)
                    .setEnable(true)
                    .setValues(PICTURE_SIZE_16_9)),

        new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(PICTURE_RATIO_5_3)
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_PICTURE_SIZE)
                    .setEnable(true)
                    .setValues(PICTURE_SIZE_5_3)),

        new Restriction(SettingKeys.ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(PICTURE_RATIO_3_2)
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_PICTURE_SIZE)
                    .setEnable(true)
                    .setValues(PICTURE_SIZE_3_2)),

        new Restriction(SettingKeys.ROW_SETTING_SMILE_SHOT)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_SCENCE_MODE)
                    .setEnable(true)
                    .setValues("auto", "portrait")),

        new Restriction(SettingKeys.ROW_SETTING_VIDEO_QUALITY)
            .setValues(Integer.toString(CamcorderProfileEx.QUALITY_FINE_4K2K))
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_3DNR)
                    .setEnable(false)
                    .setValues("off")),

        new Restriction(SettingKeys.ROW_SETTING_VIDEO)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_SCENCE_MODE)
                    .setEnable(true)
                    .setValues(VIDEO_SUPPORT_SCENE_MODE)),
        new Restriction(SettingKeys.ROW_SETTING_CAPTURE_MODE)
            .setValues("normal", "autorama")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_FACEBEAUTY_SHARP)
                    .setValues(Utils.RESET_STATE_VALUE_DISABLE),
                new Restriction(SettingKeys.ROW_SETTING_FACEBEAUTY_SKIN_COLOR)
                    .setValues(Utils.RESET_STATE_VALUE_DISABLE),
                new Restriction(SettingKeys.ROW_SETTING_FACEBEAUTY_SMOOTH)
                    .setValues(Utils.RESET_STATE_VALUE_DISABLE)),

        new Restriction(SettingKeys.ROW_SETTING_DNG)
            .setValues("on")
            .setRestrictions(
                new Restriction(SettingKeys.ROW_SETTING_HDR)
                    .setEnable(false)
                    .setValues("off"),
                new Restriction(SettingKeys.ROW_SETTING_CONTINUOUS_NUM)
                    .setEnable(false)
                    .setValues("disable-value")),
    };

    private static final int[][] MATRIX_SCENE_STATE = new int[SettingKeys.SETTING_COUNT][];
    static {
        //action  portrait    land     night    nightport theatre    beach
        // snow     sunset    steady  fireworks    spot     party    candle     auto     normal
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_EXPOSURE]        = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R1, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_E0, STATE_E0}; //2: ev
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_SLOW_MOTION]     = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_E0};
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_WHITE_BALANCE]   = new int[]{
                STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R2, STATE_E0, STATE_E0}; //4: white balance
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_ISO]             = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_E0, STATE_E0}; //12: ISO
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_SHARPNESS]       = new int[]{
                STATE_R0, STATE_R1, STATE_R2, STATE_R1, STATE_R1, STATE_R2,
                STATE_R2, STATE_R2, STATE_R2, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_E0, STATE_E0};
                //32: image adjustment sharpness
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_HUE]             = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_E0}; //30: image adjustment hue
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_SATURATION]      = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_E0}; //33: image adjustment saturation
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_BRIGHTNESS]      = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_E0}; //34: image adjustment brightness
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_CONTRAST]        = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_E0}; //31: image adjustment contrast
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_FLASH]           = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0}; //0: flash
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_FAST_AF]         = new int[]{
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0,
                STATE_E0, STATE_E0}; //31: image adjustment contrast
        MATRIX_SCENE_STATE[SettingKeys.ROW_SETTING_DISTANCE]        = new int[]{
                STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0,
                STATE_E0, STATE_E0}; //0: flash
    }

    private static final String[] MATRIX_SCENE_COLUMN = {
        "action",
        "portrait",
        "landscape",
        "night",
        "night-portrait",
        "theatre",
        "beach",
        "snow",
        "sunset",
        "steadyphoto",
        "fireworks",
        "sports",
        "party",
        "candlelight",
        "auto",
        "normal",
    };

    private static final String[] SETTING_DEFAULT_VALUES = new String[SettingKeys.SETTING_COUNT];
    static {
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FACE_BEAUTY]               = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_PANORAMA]                  = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_PHOTO_PIP]                 = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_VIDEO_PIP]                 = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_VIDEO]                     = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_REFOCUS]                   = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_NORMAL]                    = "on";

        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SLOW_MOTION]               = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SMILE_SHOT]                = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_GESTURE_SHOT]              = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_HDR]                       = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_ASD]                       = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_DUAL_CAMERA]               = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_EXPOSURE]                  = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SCENCE_MODE]               = "auto";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_WHITE_BALANCE]             = "auto";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_COLOR_EFFECT]              = "none";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SELF_TIMER]                = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_CONTINUOUS_NUM]            = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_RECORD_LOCATION]           = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_VIDEO_QUALITY]             = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_ISO]                       = "auto";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_ANTI_FLICKER]              = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_VIDEO_STABLE]              = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_MICROPHONE]                = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_AUDIO_MODE]                = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_TIME_LAPSE]                = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_PICTURE_RATIO]             = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_PICTURE_SIZE]              = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_ZSD]                       = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_VOICE]                     = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_3DNR]                      = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY] = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_AIS]                       = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SHARPNESS]                 = "middle";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_HUE]                       = "middle";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_SATURATION]                = "middle";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_BRIGHTNESS]                = "middle";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_CONTRAST]                  = "middle";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_IMAGE_PROPERTIES]          = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_CAMERA_MODE]               = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_CAPTURE_MODE]              = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_RECORDING_HINT]            = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FLASH]                     = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_JPEG_QUALITY]              = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_STEREO_MODE]               = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FACEBEAUTY_PROPERTIES]     = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FACEBEAUTY_SMOOTH]         = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FACEBEAUTY_SKIN_COLOR]     = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FACEBEAUTY_SHARP]          = "0";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_CAMERA_FACE_DETECT]        = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_MUTE_RECORDING_SOUND]      = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_MULTI_FACE_MODE]           = null;
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_FAST_AF]                   = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_DISTANCE]                  = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_DUAL_CAMERA_MODE]          = "off";
        SETTING_DEFAULT_VALUES[SettingKeys.ROW_SETTING_OBJECT_TRACKING]           = "off";
    }

    public static String getSettingResetValue(int row, int state) {
        if (state == STATE_E0) {
            return null;
        }
        if (state == STATE_D0) {
            return Utils.RESET_STATE_VALUE_DISABLE;
        }
        if (state >= STATE_R0 && state <= STATE_R7) {
            return RESET_STATE_VALUE[row][state % 300];
        }

        return null;
    }

    public static int[][] getRestrictionMatrix() {
        return MATRIX_RESTRICTION_STATE;
    }

    public static int getSettingIndex(int column) {
        int settingIndex = -1;
        if (column < RESTRCTION_SETTING_INDEX.length) {
            settingIndex = RESTRCTION_SETTING_INDEX[column];
        }
        return settingIndex;
    }

    public static int getSettingColumn(int settingId) {
        int column = -1;
        for (int i = 0; i < RESTRCTION_SETTING_INDEX.length; i++) {
            if (settingId == RESTRCTION_SETTING_INDEX[i]) {
                column = i;
                break;
            }
        }
        return column;
    }

    public static String getSceneMode(int column) {
        return MATRIX_SCENE_COLUMN[column];
    }

    public static int[][] getSceneRestrictionMatrix() {
        return MATRIX_SCENE_STATE;
    }

    public static Restriction[] getRestrictions() {
        return RESTRICTIOINS;
    }

    /**
     * Get the default value of setting which is set for internal logic.
     * @param settingIndex Index of setting.
     * @return The default value of setting.
     */
    public static String getDefaultValue(int settingIndex) {
        if (settingIndex >= SettingKeys.SETTING_COUNT) {
            return null;
        } else {
            return SETTING_DEFAULT_VALUES[settingIndex];
        }
    }
}
