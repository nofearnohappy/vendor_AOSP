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

/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>
#include <string.h>
#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_ov4688mipiraw.h"
#include "camera_info_ov4688mipiraw.h"
#include "camera_custom_AEPlinetable.h"
#include "camera_custom_tsf_tbl.h"


const NVRAM_CAMERA_ISP_PARAM_STRUCT CAMERA_ISP_DEFAULT_VALUE =
{{
    //Version
    Version: NVRAM_CAMERA_PARA_FILE_VERSION,

    //SensorId
    SensorId: SENSOR_ID,
    ISPComm:{
      {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      }
    },
    ISPPca: {
#include INCLUDE_FILENAME_ISP_PCA_PARAM
    },
    ISPRegs:{
#include INCLUDE_FILENAME_ISP_REGS_PARAM
    },
    ISPMfbMixer:{{
      0x01FF0001, // MIX3_CTRL_0
      0x00FF0000, // MIX3_CTRL_1
      0xFFFF0000  // MIX3_SPARE
    }},
    ISPMulitCCM:{
      Poly22:{
        87900,    // i4R_AVG
        18363,    // i4R_STD
        101980,   // i4B_AVG
        20674,    // i4B_STD
        4095,      // i4R_MAX
        512,      // i4R_MIN
        4095,      // i4G_MAX
        512,      // i4G_MIN   
        4095,      // i4B_MAX
        512,      // i4B_MIN
                { // i4P00[9]
                8897296,-2989626,-787686,-1208954,6829026,-500072,170190,-2770908,7720566
                },
                { // i4P10[9]
                1867396,-1257886,-609516,-495040,-44440,539480,-147722,392332,-245110
                },
                { // i4P01[9]
                1628734,-988046,-640704,-716820,-361112,1077932,-114812,-380908,495378
                },
                { // i4P20[9]
                788014,-983900,196062,-43050,119624,-76574,281758,-1043902,762090
                },
                { // i4P11[9]
                -71500,-689612,761476,243148,119000,-362148,286776,-619070,332618
                },
                { // i4P02[9]
                -631502,130466,501236,302926,68298,-371224,43616,-17274,-25994
                }

      },
      AWBGain:{
        // Strobe
        {
          810,    // i4R
          512,    // i4G
          677    // i4B
        },
        // A
        {
            519,    // i4R
            512,    // i4G
            1450    // i4B
        },
        // TL84
        {
            605,    // i4R
            512,    // i4G
            1172    // i4B
        },
        // CWF
        {
            771,    // i4R
            512,    // i4G
            1293    // i4B
        },
        // D65
        {
            810,    // i4R
            512,    // i4G
            677    // i4B
        },
        // Reserved 1
        {
            512,    // i4R
            512,    // i4G
            512    // i4B
        },
        // Reserved 2
        {
            512,    // i4R
            512,    // i4G
            512    // i4B
        },
        // Reserved 3
        {
            512,    // i4R
            512,    // i4G
            512    // i4B
        }
      },
      Weight:{
        1, // Strobe
        1, // A
        1, // TL84
        1, // CWF
        1, // D65
        1, // Reserved 1
        1, // Reserved 2
        1  // Reserved 3
      }
    },
    
    //bInvokeSmoothCCM
    bInvokeSmoothCCM: MTRUE
}};

const NVRAM_CAMERA_3A_STRUCT CAMERA_3A_NVRAM_DEFAULT_VALUE =
{
    NVRAM_CAMERA_3A_FILE_VERSION, // u4Version
    SENSOR_ID, // SensorId

    // AE NVRAM
    {
        // rDevicesInfo
        {
            1195,   // u4MinGain, 1024 base =  1x
            8192,  // u4MaxGain, 16x
            85,     // u4MiniISOGain, ISOxx
            128,    // u4GainStepUnit, 1x/8
            19770,     // u4PreExpUnit
            30,     // u4PreMaxFrameRate
            19770,     // u4VideoExpUnit
            30,     // u4VideoMaxFrameRate
            1024,   // u4Video2PreRatio, 1024 base = 1x
            13139,     // u4CapExpUnit
            24,     // u4CapMaxFrameRate
            1024,   // u4Cap2PreRatio, 1024 base = 1x
            19770,     // u4Video1ExpUnit
            30,     // u4Video1MaxFrameRate
            1024,   // u4Video12PreRatio, 1024 base = 1x
            19770,     // u4Video2ExpUnit
            30,     // u4Video2MaxFrameRate
            1024,   // u4Video22PreRatio, 1024 base = 1x
            19770,     // u4Custom1ExpUnit
            30,     // u4Custom1MaxFrameRate
            1024,   // u4Custom12PreRatio, 1024 base = 1x
            19770,     // u4Custom2ExpUnit
            30,     // u4Custom2MaxFrameRate
            1024,   // u4Custom22PreRatio, 1024 base = 1x
            19770,     // u4Custom3ExpUnit
            30,     // u4Custom3MaxFrameRate
            1024,   // u4Custom32PreRatio, 1024 base = 1x
            19770,     // u4Custom4ExpUnit
            30,     // u4Custom4MaxFrameRate
            1024,   // u4Custom42PreRatio, 1024 base = 1x
            19770,     // u4Custom5ExpUnit
            30,     // u4Custom5MaxFrameRate
            1024,   // u4Custom52PreRatio, 1024 base = 1x
            28,      // u4LensFno, Fno = 2.8
            350     // u4FocusLength_100x
        },
        // rHistConfig
        {
            4, // 2,   // u4HistHighThres
            40,  // u4HistLowThres
            2,   // u4MostBrightRatio
            1,   // u4MostDarkRatio
            160, // u4CentralHighBound
            20,  // u4CentralLowBound
            {240, 230, 220, 210, 200}, // u4OverExpThres[AE_CCT_STRENGTH_NUM]
            {62, 70, 82, 108, 141},  // u4HistStretchThres[AE_CCT_STRENGTH_NUM]
            {18, 22, 26, 30, 34}       // u4BlackLightThres[AE_CCT_STRENGTH_NUM]
        },
        // rCCTConfig
        {
            TRUE,            // bEnableBlackLight
            TRUE,            // bEnableHistStretch
            TRUE,           // bEnableAntiOverExposure
            TRUE,            // bEnableTimeLPF
            TRUE,            // bEnableCaptureThres
            TRUE,            // bEnableVideoThres
            TRUE,            // bEnableVideo1Thres
            TRUE,            // bEnableVideo2Thres
            TRUE,            // bEnableCustom1Thres
            TRUE,            // bEnableCustom2Thres
            TRUE,            // bEnableCustom3Thres
            TRUE,            // bEnableCustom4Thres
            TRUE,            // bEnableCustom5Thres
            TRUE,            // bEnableStrobeThres
            47,                // u4AETarget
            47,                // u4StrobeAETarget

            50,                // u4InitIndex
            4,                 // u4BackLightWeight
            32,                // u4HistStretchWeight
            4,                 // u4AntiOverExpWeight
            2,                 // u4BlackLightStrengthIndex
            2,                 // u4HistStretchStrengthIndex
            2,                 // u4AntiOverExpStrengthIndex
            2,                 // u4TimeLPFStrengthIndex
            {1, 3, 5, 7, 8}, // u4LPFConvergeTable[AE_CCT_STRENGTH_NUM]
            90,                // u4InDoorEV = 9.0, 10 base
                        13,               // i4BVOffset delta BV = -2.3
            64,                 // u4PreviewFlareOffset
            64,                 // u4CaptureFlareOffset
            3,                 // u4CaptureFlareThres
            64,                 // u4VideoFlareOffset
            3,                 // u4VideoFlareThres
            64,               // u4CustomFlareOffset
            3,                 //  u4CustomFlareThres
            64,                 // u4StrobeFlareOffset //12 bits
            3,                 // u4StrobeFlareThres // 0.5%
            160,                 // u4PrvMaxFlareThres //12 bit
            0,                 // u4PrvMinFlareThres
            160,                 // u4VideoMaxFlareThres // 12 bit
            0,                 // u4VideoMinFlareThres
            18,                // u4FlatnessThres              // 10 base for flatness condition.
            75,    // u4FlatnessStrength
            //rMeteringSpec
            {
                //rHS_Spec
                {
                    TRUE,//bEnableHistStretch           // enable histogram stretch
                    1024,//u4HistStretchWeight          // Histogram weighting value
                    40, //50, //20,//u4Pcent                      // 1%=10, 0~1000
                    160, //166,//176,//u4Thd                        // 0~255
                    75, //54, //74,//u4FlatThd                    // 0~255

                    120,//u4FlatBrightPcent
                    120,//u4FlatDarkPcent
                    //sFlatRatio
                    {
                        1000,  //i4X1
                        1024,  //i4Y1
                        2400, //i4X2
                        0     //i4Y2
                    },
                    TRUE, //bEnableGreyTextEnhance
                    1800, //u4GreyTextFlatStart, > sFlatRatio.i4X1, < sFlatRatio.i4X2
                    {
                        10,     //i4X1
                        1024,   //i4Y1
                        80,     //i4X2
                        0       //i4Y2
                    }
                },
                //rAOE_Spec
                {
                    TRUE, //bEnableAntiOverExposure
                    1024, //u4AntiOverExpWeight
                    10,    //u4Pcent
                    200,  //u4Thd

                    TRUE, //bEnableCOEP
                    1,    //u4COEPcent
                    106,  //u4COEThd
                    0,  // u4BVCompRatio
                    //sCOEYRatio;     // the outer y ratio
                    {
                        23,   //i4X1
                        1024,  //i4Y1
                        47,   //i4X2
                        0     //i4Y2
                    },
                    //sCOEDiffRatio;  // inner/outer y difference ratio
                    {
                        1500, //i4X1
                        0,    //i4Y1
                        2100, //i4X2
                        1024   //i4Y2
                    }
                },
                //rABL_Spec
                {
                    TRUE,//bEnableBlackLigh
                    1024,//u4BackLightWeigh
                    400,//u4Pcent
                    22,//u4Thd,
                    255, // center luminance
                    256, // final target limitation, 256/128 = 2x
                    //sFgBgEVRatio
                    {
                        2200, //i4X1
                        0,    //i4Y1
                        4000, //i4X2
                        1024   //i4Y2
                    },
                    //sBVRatio
                    {
                        3800,//i4X1
                        0,   //i4Y1
                        5000,//i4X2
                        1024  //i4Y2
                    }
                },
                //rNS_Spec
                {
                    TRUE, // bEnableNightScene
                    5,    //u4Pcent
                    170,  //u4Thd
                    72, //52,   //u4FlatThd

                    200,  //u4BrightTonePcent
                    92, //u4BrightToneThd

                    500,  //u4LowBndPcent
                    5,    //u4LowBndThd
                    26,    //u4LowBndThdLimit

                    50,  //u4FlatBrightPcent;
                    300,   //u4FlatDarkPcent;
                    //sFlatRatio
                    {
                        1200, //i4X1
                        1024, //i4Y1
                        2400, //i4X2
                        0    //i4Y2
                    },
                    //sBVRatio
                    {
                        -500, //i4X1
                        1024,  //i4Y1
                        3000, //i4X2
                        0     //i4Y2
                    },
                    TRUE, // bEnableNightSkySuppresion
                    //sSkyBVRatio
                    {
                        -4000, //i4X1
                        1024, //i4X2
                        -2000,  //i4Y1
                        0     //i4Y2
                    }
                },
                // rTOUCHFD_Spec
                {
                    40, //uMeteringYLowBound;
                    50, //uMeteringYHighBound;
                    40, //uFaceYLowBound;
                    50, //uFaceYHighBound;
                    3,  //uFaceCentralWeight;
                    120,//u4MeteringStableMax;
                    80, //u4MeteringStableMin;
                }
            }, //End rMeteringSpec
            // rFlareSpec
            {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //uPrvFlareWeightArr[16];
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //uVideoFlareWeightArr[16];
                96,                                               //u4FlareStdThrHigh;
                48,                                               //u4FlareStdThrLow;
                0,                                                //u4PrvCapFlareDiff;
                4,                                                //u4FlareMaxStepGap_Fast;
                0,                                                //u4FlareMaxStepGap_Slow;
                1800,                                             //u4FlarMaxStepGapLimitBV;
                0,                                                //u4FlareAEStableCount;
            },
            //rAEMoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                190, //u4Bright2TargetEnd
                20,   //u4Dark2TargetStart
                90, //u4B2TEnd
                70,  //u4B2TStart
                60,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAEVideoMoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAEVideo1MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAEVideo2MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAECustom1MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAECustom2MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAECustom3MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAECustom4MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAECustom5MoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                150,  //u4Bright2TargetEnd
                20,    //u4Dark2TargetStart
                90, //u4B2TEnd
                10,  //u4B2TStart
                10,  //u4D2TEnd
                90,  //u4D2TStart
            },

            //rAEFaceMoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                190,  //u4Bright2TargetEnd
                10,    //u4Dark2TargetStart
                80, //u4B2TEnd
                30,  //u4B2TStart
                20,  //u4D2TEnd
                60,  //u4D2TStart
            },

            //rAETrackingMoveRatio =
            {
                100, //u4SpeedUpRatio
                100, //u4GlobalRatio
                190,  //u4Bright2TargetEnd
                10,    //u4Dark2TargetStart
                80, //u4B2TEnd
                30,  //u4B2TStart
                20,  //u4D2TEnd
                60,  //u4D2TStart
            },
            //rAEAOENVRAMParam =
            {
                1,      // i4AOEStrengthIdx: 0 / 1 / 2
                128,    // u4BVCompRatio
                {
                    {
                        47,  //u4Y_Target
                        10,  //u4AOE_OE_percent
                        160,  //u4AOE_OEBound
                        15,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                        5,    //u4AOE_LowlightBound
                        100,    //u4AOESceneLV_L
                        150,    //u4AOESceneLV_H
                        40,    //u4AOE_SWHdrLE_Bound
                    },
                    {
                        47,  //u4Y_Target
                        10,  //u4AOE_OE_percent
                        180,  //u4AOE_OEBound
                20,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                10,    //u4AOE_LowlightBound
                100,    //u4AOESceneLV_L
                150,    //u4AOESceneLV_H
                        40,    //u4AOE_SWHdrLE_Bound
                    },
                    {
                        47,  //u4Y_Target
                        10,  //u4AOE_OE_percent
                        200,  //u4AOE_OEBound
                        25,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                        15,    //u4AOE_LowlightBound
                        100,    //u4AOESceneLV_L
                        150,    //u4AOESceneLV_H
                        40,    //u4AOE_SWHdrLE_Bound
                    }
                }
            }
        }
    },

        // AWB NVRAM
        {
        {
                // AWB calibration data
                {
                        // rUnitGain (unit gain: 1.0 = 512)
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        // rGoldenGain (golden sample gain: 1.0 = 512)
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        // rTuningUnitGain (Tuning sample unit gain: 1.0 = 512)
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        // rD65Gain (D65 WB gain: 1.0 = 512)
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        }
                },
                // Original XY coordinate of AWB light source
                {
                        // Strobe
                        {
                53,    // i4X
                -441    // i4Y
                        },
                        // Horizon
                        {
                -309,    // i4X
                -307    // i4Y
                        },
                        // A
                        {
                -212,    // i4X
                -341    // i4Y
                        },
                        // TL84
                        {
                -72,    // i4X
                -385    // i4Y
                        },
                        // CWF
                        {
                -55,    // i4X
                -460    // i4Y
                        },
                        // DNP
                        {
                39,    // i4X
                -410    // i4Y
                        },
                        // D65
                        {
                134,    // i4X
                -353    // i4Y
                        },
                        // DF
                        {
                101,    // i4X
                -453    // i4Y
                        }
                },
                // Rotated XY coordinate of AWB light source
                {
                        // Strobe
                        {
                37,    // i4X
                -443    // i4Y
                        },
                        // Horizon
                        {
                -320,    // i4X
                -296    // i4Y
                        },
                        // A
                        {
                -224,    // i4X
                -334    // i4Y
                        },
                        // TL84
                        {
                -86,    // i4X
                -382    // i4Y
                        },
                        // CWF
                        {
                -71,    // i4X
                -458    // i4Y
                        },
                        // DNP
                        {
                25,    // i4X
                -411    // i4Y
                        },
                        // D65
                        {
                122,    // i4X
                -358    // i4Y
                        },
                        // DF
                        {
                85,    // i4X
                -457    // i4Y
                        }
                },
                // AWB gain of AWB light source
                {
                        // Strobe
                        {
                1000,    // i4R
                                512,    // i4G
                865    // i4B
                        },
                        // Horizon
                        {
                                512,    // i4R
                513,    // i4G
                1182    // i4B
                        },
                        // A
                        {
                610,    // i4R
                                512,    // i4G
                1083    // i4B
                        },
                        // TL84
                        {
                783,    // i4R
                                512,    // i4G
                951    // i4B
                        },
                        // CWF
                        {
						850,    // i4R
                                512,    // i4G
            820    // i4B
                        },
                        // DNP
                        {
                941,    // i4R
                                512,    // i4G
                846    // i4B
                        },
                        // D65
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        },
                        // DF
                        {
                1085,    // i4R
                                512,    // i4G
                825    // i4B
                        }
                },
                // Rotation matrix parameter
                {
		2,	// i4RotationAngle					              
                        256,    // i4Cos
		9	// i4Sin					                        
                },
                // Daylight locus parameter
                {
            -114,    // i4SlopeNumerator
                        128    // i4SlopeDenominator
                },
	            // Predictor gain
                {
                        // i4PrefRatio100
                        101,

                        // DaylightLocus_L
                        {
                            810,    // i4R
                            512,    // i4G
                            677     // i4B
                        },
                        // DaylightLocus_H
                        {
                            684,    // i4R
                            512,    // i4G
                            901     // i4B
                        },
                        // Temporal General
                        {
                            810,    // i4R
                            512,    // i4G
                            677     // i4B
                        }
                },
                // AWB light area
                {
                        // Strobe:FIXME
                        {
            0,    // i4RightBound
            0,    // i4LeftBound
            0,    // i4UpperBound
            0    // i4LowerBound
                        },
                        // Tungsten
                        {
            -136,    // i4RightBound
            -500,    // i4LeftBound
            -300,    // i4UpperBound
            -420    // i4LowerBound
                        },
                        // Warm fluorescent
                        {
            -136,    // i4RightBound
            -450,    // i4LeftBound
            -420,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        // Fluorescent
                        {
            -35,    // i4RightBound
            -136,    // i4LeftBound
            -300,    // i4UpperBound
            -460    // i4LowerBound
                        },
                        // CWF
                        {
            -35,    // i4RightBound
            -136,    // i4LeftBound
            -460,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        // Daylight
                        {
            120,    // i4RightBound
            -35,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Shade
                        {
            480,    // i4RightBound
            160,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Daylight Fluorescent
                        {
            210,    // i4RightBound
            -35,    // i4LeftBound
            -440,    // i4UpperBound
            -550    // i4LowerBound
                        }
                },
                // PWB light area
                {
                        // Reference area
                        {
            480,    // i4RightBound
            -786,    // i4LeftBound
            0,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        // Daylight
                        {
            145,    // i4RightBound
            -35,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Cloudy daylight
                        {
            245,    // i4RightBound
            70,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Shade
                        {
            345,    // i4RightBound
            70,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Twilight
                        {
            -35,    // i4RightBound
            -195,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Fluorescent
                        {
            172,    // i4RightBound
            -186,    // i4LeftBound
            -308,    // i4UpperBound
            -508    // i4LowerBound
                        },
                        // Warm fluorescent
                        {
            -124,    // i4RightBound
            -324,    // i4LeftBound
            -308,    // i4UpperBound
            -508    // i4LowerBound
                        },
                        // Incandescent
                        {
            -124,    // i4RightBound
            -324,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        // Gray World
                        {
                                5000,    // i4RightBound
                                -5000,    // i4LeftBound
                                5000,    // i4UpperBound
                                -5000    // i4LowerBound
                        }
                },
                // PWB default gain
                {
                        // Daylight
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        },
                        // Cloudy daylight
                        {
            1078,    // i4R
                                512,    // i4G
            679    // i4B
                        },
                        // Shade
                        {
            1150,    // i4R
                                512,    // i4G
            633    // i4B
                        },
                        // Twilight
                        {
            755,    // i4R
                                512,    // i4G
            994    // i4B
                        },
                        // Fluorescent
                        {
            898,    // i4R
                                512,    // i4G
            880    // i4B
                        },
                        // Warm fluorescent
                        {
            677,    // i4R
                                512,    // i4G
            1193    // i4B
                        },
                        // Incandescent
                        {
            655,    // i4R
                                512,    // i4G
            1158    // i4B
                        },
                        // Gray World
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        }
                },
                // AWB preference color
                {
                        // Tungsten
                        {
            20,    // i4SliderValue
            4719    // i4OffsetThr
                        },
                        // Warm fluorescent
                        {
            0,    // i4SliderValue
            4519    // i4OffsetThr
                        },
                        // Shade
                        {
            0,    // i4SliderValue
            1518    // i4OffsetThr
                        },

                        // Preference gain: strobe
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: tungsten
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: warm fluorescent
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: fluorescent
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: CWF
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: daylight
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: shade
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Preference gain: daylight fluorescent
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        }
                },

                // Algorithm Tuning Paramter
                {
                    // AWB Backup Enable
                    FALSE,

                    // AWB LSC Gain
                    {
                        684,        // i4R
                        512,        // i4G
                        901         // i4B
                    },
                    // Parent block weight parameter
                    {
                        TRUE,      // bEnable
                        6           // i4ScalingFactor: [6] 1~12, [7] 1~6, [8] 1~3, [9] 1~2, [>=10]: 1
                    },
                    // AWB LV threshold for predictor
                    {
                            100,    // i4InitLVThr_L
                            140,    // i4InitLVThr_H
                            80      // i4EnqueueLVThr
                    },
                    // AWB number threshold for temporal predictor
                    {
                            65,     // i4Neutral_ParentBlk_Thr
                        //LV0  1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                            { 100, 100, 100, 100, 100, 100, 100, 100, 50,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2}  // (%) i4CWFDF_LUTThr
                    },
                    // AWB light neutral noise reduction for outdoor
                    {
                        //LV0  1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        // Non neutral
		                { 10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // Flurescent
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // CWF
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // Daylight
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   2,   4,   4,   4,   4,   4,   4,   4,   4},  // (%)
		                // DF
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
                    },
                    // AWB feature detection
                    {
                        // Sunset Prop
                        {
                            1,          // i4Enable
                            130,        // i4LVThr
                            {
                                71,   // i4Sunset_BoundXr_Thr
                                -425     // i4Sunset_BoundYr_Thr
                            },
                            10,         // i4SunsetCountThr
                            0,          // i4SunsetCountRatio_L
                            171         // i4SunsetCountRatio_H
                        },

                        // Shade F Detection
                        {
                            1,          // i4Enable
                            105,        // i4LVThr
                            {

                                -78,   // i4BoundXrThr
                                -376    // i4BoundYrThr
                            },
                            192         // i4DaylightProb
                        },

                        // Shade CWF Detection
                        {
                            1,          // i4Enable
                            95,         // i4LVThr
                            {
                                -112,   // i4BoundXrThr
                                -470    // i4BoundYrThr
                            },
                            192         // i4DaylightProb
                        },

                        // Low CCT
                        {
                            1,          // i4Enable
                            384,        // i4SpeedRatio
                            {
                            -465,       // i4BoundXrThr
                            237         // i4BoundYrThr
                            }
                        }

                    },

                    // AWB Gain Limit
                    {
                        // rNormalLowCCT
                        {
                            1,      // Gain Limit Enable
                            717     // Gain ratio
                        },
                        // rPrefLowCCT
                        {
                            1,      // Gain Limit Enable
                            870     // Gain ratio
                        }

                    },

                    // AWB non-neutral probability for spatial and temporal weighting look-up table (Max: 100; Min: 0)
                    {
                        //LV0   1    2    3    4    5    6    7    8    9   10   11   12   13   14   15   16   17   18
                        { 100, 100, 100, 100, 100, 100, 100, 100, 75,  50,   30,  20,  10,  0,   0,   0,   0,   0,   0}
                    },

                    // AWB daylight locus probability look-up table (Max: 100; Min: 0)
                    {   //LV0    1     2     3      4     5     6     7     8      9      10     11    12   13     14    15   16    17    18
		                //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  75,  35,  0,   0,   0,  0,   0,   0}, // Strobe
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Tungsten
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Warm fluorescent
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,  0,   0,  0,   0,   0}, // Fluorescent
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // CWF
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  50,  50,  40, 30,  0,   0}, // Daylight
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // Shade
                        //{100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}  // Daylight fluorescent
		                {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  75,  35,  0,   0,   0,  0,   0,   0}, // Strobe
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Tungsten
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Warm fluorescent
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,   0,   0,  0,   0,   0}, // CWF
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // CWF
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  50,  50,  40, 30,  0,   0}, // Daylight
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // Shade
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}  // Daylight fluorescent
    		        }
                },
                {// CCT estimation
                        {// CCT
			                2300,	// i4CCT[0]
			                2850,	// i4CCT[1]
                                4100,    // i4CCT[2]
			                5100,	// i4CCT[3]
			                6500 	// i4CCT[4]
		            },
                        {// Rotated X coordinate
                -442,    // i4RotatedXCoordinate[0]
                -346,    // i4RotatedXCoordinate[1]
                -208,    // i4RotatedXCoordinate[2]
                -97,    // i4RotatedXCoordinate[3]
			                0 	    // i4RotatedXCoordinate[4]
		            }
	            }
        },
                {
                {
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        {
                                0,    // i4R
                                0,    // i4G
                                0    // i4B
                        },
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        }
                },
                {
                        {
                53,    // i4X
                -441    // i4Y
                        },
                        {
                -309,    // i4X
                -307    // i4Y
                        },
                        {
                -212,    // i4X
                -341    // i4Y
                        },
                        {
                -72,    // i4X
                -385    // i4Y
                        },
                        {
                -55,    // i4X
                -460    // i4Y
                        },
                        {
                39,    // i4X
                -410    // i4Y
                        },
                        {
                134,    // i4X
                -353    // i4Y
                        },
                        {
                101,    // i4X
                -453    // i4Y
                        }
                },
                {
                        {
                37,    // i4X
                -443    // i4Y
                        },
                        {
                -320,    // i4X
                -296    // i4Y
                        },
                        {
                -224,    // i4X
                -334    // i4Y
                        },
                        {
                -86,    // i4X
                -382    // i4Y
                        },
                        {
                -71,    // i4X
                -458    // i4Y
                        },
                        {
                25,    // i4X
                -411    // i4Y
                        },
                        {
                122,    // i4X
                -358    // i4Y
                        },
                        {
                85,    // i4X
                -457    // i4Y
                        }
                },
                {
                        {
                1000,    // i4R
                                512,    // i4G
                865    // i4B
                        },
                        {
                                512,    // i4R
                513,    // i4G
                1182    // i4B
                        },
                        {
                610,    // i4R
                                512,    // i4G
                1083    // i4B
                        },
                        {
                783,    // i4R
                                512,    // i4G
                951    // i4B
                        },
                        {
						850,    // i4R
                                512,    // i4G
            820    // i4B
                        },
                        {
                941,    // i4R
                                512,    // i4G
                846    // i4B
                        },
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        },
                        {
                1085,    // i4R
                                512,    // i4G
                825    // i4B
                        }
                },
                {
		2,	// i4RotationAngle					              
                        256,    // i4Cos
		9	// i4Sin					                        
                },
                {
            -114,    // i4SlopeNumerator
                        128    // i4SlopeDenominator
                },
                {
                        101,
                        {
                            810,    // i4R
                            512, // i4G
                            677     // i4B
                        },
                        {
                            684,    // i4R
                            512, // i4G
                            901     // i4B
                        },
                        {
                            810,    // i4R
                            512, // i4G
                            677     // i4B
                        }
                },
                {
                        {
            0,    // i4RightBound
            0,    // i4LeftBound
            0,    // i4UpperBound
            0    // i4LowerBound
                        },
                        {
            -136,    // i4RightBound
            -500,    // i4LeftBound
            -300,    // i4UpperBound
            -420    // i4LowerBound
                        },
                        {
            -136,    // i4RightBound
            -450,    // i4LeftBound
            -420,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        {
            -35,    // i4RightBound
            -136,    // i4LeftBound
            -300,    // i4UpperBound
            -460    // i4LowerBound
                        },
                        {
            -35,    // i4RightBound
            -136,    // i4LeftBound
            -460,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        {
            120,    // i4RightBound
            -35,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            480,    // i4RightBound
            160,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            210,    // i4RightBound
            -35,    // i4LeftBound
            -440,    // i4UpperBound
            -550    // i4LowerBound
                        }
                },
                {
                        {
            480,    // i4RightBound
            -786,    // i4LeftBound
            0,    // i4UpperBound
            -550    // i4LowerBound
                        },
                        {
            145,    // i4RightBound
            -35,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            245,    // i4RightBound
            70,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            345,    // i4RightBound
            70,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            -35,    // i4RightBound
            -195,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
            172,    // i4RightBound
            -186,    // i4LeftBound
            -308,    // i4UpperBound
            -508    // i4LowerBound
                        },
                        {
            -124,    // i4RightBound
            -324,    // i4LeftBound
            -308,    // i4UpperBound
            -508    // i4LowerBound
                        },
                        {
            -124,    // i4RightBound
            -324,    // i4LeftBound
            -330,    // i4UpperBound
            -440    // i4LowerBound
                        },
                        {
                                5000,    // i4RightBound
                                -5000,    // i4LeftBound
                                5000,    // i4UpperBound
                                -5000    // i4LowerBound
                        }
                },
                {
                        {
						830,    // i4R
                                512,    // i4G
            840    // i4B
                        },
                        {
            1078,    // i4R
                                512,    // i4G
            679    // i4B
                        },
                        {
            1150,    // i4R
                                512,    // i4G
            633    // i4B
                        },
                        {
            755,    // i4R
                                512,    // i4G
            994    // i4B
                        },
                        {
            898,    // i4R
                                512,    // i4G
            880    // i4B
                        },
                        {
            677,    // i4R
                                512,    // i4G
            1193    // i4B
                        },
                        {
            655,    // i4R
                                512,    // i4G
            1158    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        }
                },
                {
                        {
            20,    // i4SliderValue
            4719    // i4OffsetThr
                        },
                        {
            0,    // i4SliderValue
            4519    // i4OffsetThr
                        },
                        {
            0,    // i4SliderValue
            1518    // i4OffsetThr
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        }
                },
                {
                    FALSE,
                    {
                        684,        // i4R
                        512, // i4G
                        901         // i4B
                    },
                    {
                        TRUE,      // bEnable
                        6           // i4ScalingFactor: [6] 1~12, [7] 1~6, [8] 1~3, [9] 1~2, [>=10]: 1
                    },
                    {
                            100,    // i4InitLVThr_L
                            140,    // i4InitLVThr_H
                            80      // i4EnqueueLVThr
                    },
                    {
                            65,     // i4Neutral_ParentBlk_Thr
                            { 100, 100, 100, 100, 100, 100, 100, 100, 50,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2}  // (%) i4CWFDF_LUTThr
                    },
                    {
		                { 10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   2,   4,   4,   4,   4,   4,   4,   4,   4},  // (%)
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
                    },
                    {
                        {
                            1,          // i4Enable
                            130,        // i4LVThr
                            {
                                71,   // i4Sunset_BoundXr_Thr
                                -425     // i4Sunset_BoundYr_Thr
                            },
                            10,         // i4SunsetCountThr
                            0,          // i4SunsetCountRatio_L
                            171         // i4SunsetCountRatio_H
                        },
                        {
                            1,          // i4Enable
                            105,        // i4LVThr
                            {
                                -78,   // i4BoundXrThr
                                -376    // i4BoundYrThr
                            },
                            192         // i4DaylightProb
                        },
                        {
                            1,          // i4Enable
                            95,         // i4LVThr
                            {
                                -112,   // i4BoundXrThr
                                -470    // i4BoundYrThr
                            },
                            192         // i4DaylightProb
                        },
                        {
                            1,          // i4Enable
                            384,        // i4SpeedRatio
                            {
                            -465,       // i4BoundXrThr
                            237         // i4BoundYrThr
                            }
                        }
                    },
                    {
                        {
                            1,      // Gain Limit Enable
                            717     // Gain ratio
                        },
                        {
                            1,      // Gain Limit Enable
                            870     // Gain ratio
                        }
                    },
                    {
                        { 100, 100, 100, 100, 100, 100, 100, 100, 75,  50,   30,  20,  10,  0,   0,   0,   0,   0,   0}
                    },
                    {   //LV0    1     2     3      4     5     6     7     8      9      10     11    12   13     14    15   16    17    18
		                {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  75,  35,  0,   0,   0,  0,   0,   0}, // Strobe
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Tungsten
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Warm fluorescent
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,   0,   0,  0,   0,   0}, // Shade
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // CWF
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  50,  50,  40, 30,  0,   0}, // Daylight
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}, // Shade
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  50,  25,  0,   0,   0,  0,   0,   0}  // Daylight fluorescent
    		        }
                },

	            // CCT estimation
	            {
		            // CCT
		            {
			                2300,	// i4CCT[0]
			                2850,	// i4CCT[1]
                                4100,    // i4CCT[2]
			                5100,	// i4CCT[3]
			                6500 	// i4CCT[4]
		            },
		            // Rotated X coordinate
		            {
                -442,    // i4RotatedXCoordinate[0]
                -346,    // i4RotatedXCoordinate[1]
                -208,    // i4RotatedXCoordinate[2]
                -97,    // i4RotatedXCoordinate[3]
			                0 	    // i4RotatedXCoordinate[4]
		            }
		            }
	            }
        },

    // Flash AWB NVRAM
    {
#include INCLUDE_FILENAME_FLASH_AWB_PARA
    },

    {0}
};

#include INCLUDE_FILENAME_ISP_LSC_PARAM
//};  //  namespace

const CAMERA_TSF_TBL_STRUCT CAMERA_TSF_DEFAULT_VALUE =
{
    {
                0,  // isTsfEn
        2,  // tsfCtIdx
        {20, 2000, -110, -110, 512, 512, 512, 0}    // rAWBInput[8]
    },

#include INCLUDE_FILENAME_TSF_PARA
#include INCLUDE_FILENAME_TSF_DATA
};

const NVRAM_CAMERA_FEATURE_STRUCT CAMERA_FEATURE_DEFAULT_VALUE =
{
#include INCLUDE_FILENAME_FEATURE_PARA
};

typedef NSFeature::RAWSensorInfo<SENSOR_ID> SensorInfoSingleton_T;


namespace NSFeature {
  template <>
  UINT32
  SensorInfoSingleton_T::
  impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, VOID*const pDataBuf, UINT32 const size) const
  {
    UINT32 dataSize[CAMERA_DATA_TYPE_NUM] = {sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT),
        sizeof(NVRAM_CAMERA_3A_STRUCT),
        sizeof(NVRAM_CAMERA_SHADING_STRUCT),
        sizeof(NVRAM_LENS_PARA_STRUCT),
        sizeof(AE_PLINETABLE_T),
        0,
        sizeof(CAMERA_TSF_TBL_STRUCT),
        0,
        sizeof(NVRAM_CAMERA_FEATURE_STRUCT)
    };

    if (CameraDataType > CAMERA_NVRAM_DATA_FEATURE || NULL == pDataBuf || (size < dataSize[CameraDataType]))
    {
      return 1;
    }

    switch(CameraDataType)
    {
      case CAMERA_NVRAM_DATA_ISP:
        memcpy(pDataBuf,&CAMERA_ISP_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT));
        break;
      case CAMERA_NVRAM_DATA_3A:
        memcpy(pDataBuf,&CAMERA_3A_NVRAM_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_3A_STRUCT));
        break;
      case CAMERA_NVRAM_DATA_SHADING:
        memcpy(pDataBuf,&CAMERA_SHADING_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_SHADING_STRUCT));
        break;
      case CAMERA_DATA_AE_PLINETABLE:
        memcpy(pDataBuf,&g_PlineTableMapping,sizeof(AE_PLINETABLE_T));
        break;
      case CAMERA_DATA_TSF_TABLE:
        memcpy(pDataBuf,&CAMERA_TSF_DEFAULT_VALUE,sizeof(CAMERA_TSF_TBL_STRUCT));
        break;
      case CAMERA_NVRAM_DATA_FEATURE:
        memcpy(pDataBuf,&CAMERA_FEATURE_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_FEATURE_STRUCT));
        break;
      default:
        break;
    }
    return 0;
  }};  //  NSFeature


