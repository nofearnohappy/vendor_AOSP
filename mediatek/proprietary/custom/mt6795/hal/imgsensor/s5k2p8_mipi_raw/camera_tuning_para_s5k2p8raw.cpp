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
#include "camera_AE_PLineTable_s5k2p8raw.h"
#include "camera_info_s5k2p8raw.h"
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
            3,               // i4BVOffset delta BV = -2.3
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
                    174,  //u4Thd
                    72, //52,   //u4FlatThd

                    200,  //u4BrightTonePcent
                    90, //u4BrightToneThd

                    500,  //u4LowBndPcent
                    5,    //u4LowBndThd
                    20,    //u4LowBndThdLimit

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
                        2000, //i4X2
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
                130,    // u4BVCompRatio
                {
                    {
                        47,  //u4Y_Target
                        20,  //u4AOE_OE_percent
                        160,  //u4AOE_OEBound
                        10,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                        1,    //u4AOE_LowlightBound
                        145,    //u4AOESceneLV_L
                        180,    //u4AOESceneLV_H
                        40,    //u4AOE_SWHdrLE_Bound
                    },
                    {
                        47,  //u4Y_Target
                        20,  //u4AOE_OE_percent
                        180,  //u4AOE_OEBound
                        15, //20,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                        3, //10,    //u4AOE_LowlightBound
                        145,    //u4AOESceneLV_L
                        180,    //u4AOESceneLV_H
                        40,    //u4AOE_SWHdrLE_Bound
                    },
                    {
                        47,  //u4Y_Target
                        20,  //u4AOE_OE_percent
                        200,  //u4AOE_OEBound
                        25,    //u4AOE_DarkBound
                        950,    //u4AOE_LowlightPrecent
                        8,    //u4AOE_LowlightBound
                        145,    //u4AOESceneLV_L
                        180,    //u4AOESceneLV_H
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
                                1095,    // i4R
                                512,    // i4G
                                736    // i4B
                        }
                },
                // Original XY coordinate of AWB light source
                {
                        // Strobe
                        {
                                0,    // i4X
                                0    // i4Y
                        },
                        // Horizon
                        {
		-425,	// i4X
		-449	// i4Y	
                        },
                        // A
                        {
			-278,	// i4X	
			-462	    // i4Y	
                        },
                        // TL84
                        {
			-78,	// i4X		
			-493	// i4Y		
                        },
                        // CWF
                        {
			-37,	// i4X	
			-530	// i4Y	
                        },
                        // DNP
                        {
			23,	// i4X		
			-487	// i4Y		
                        },
                        // D65
                        {
			188,	// i4X			
			-442	// i4Y			
                        },
                        // DF
                        {
			168,    // i4X
			-527	// i4Y	
                        }
                },
                // Rotated XY coordinate of AWB light source
                {
                        // Strobe
                        {
                                0,    // i4X
                                0    // i4Y
                        },
                        // Horizon
                        {
			-441,	// i4X				                  
			-434	// i4Y				                  
                        },
                        // A
                        {
			-294,	// i4X				                    
			-452	// i4Y				                    
                        },
                        // TL84
                        {
			-95,	// i4X				                
			-490	// i4Y				                
                        },
                        // CWF
                        {
			-56,	// i4X				                  
			-529	// i4Y				                  
                        },
                        // DNP
                        {
			6,	// i4X				                  
			-488	// i4Y				                  
                        },
                        // D65
                        {
			172,	// i4X				                    
			-449	// i4Y				                  
                        },
                        // DF
                        {
			149,	// i4X				                    
			-533	// i4Y				                      
                        }
                },
                // AWB gain of AWB light source
                {
                        // Strobe
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        },
                        // Horizon
                        {
			529,	// i4R				            
                                512,    // i4G
			1671	// i4B				            
                        },
                        // A
                        {
			656,	// i4R				              
                                512,    // i4G
			1394	// i4B				              
                        },
                        // TL84
                        {
			898,	// i4R				            
                                512,    // i4G
			1109	// i4B				            
                        },
                        // CWF
                        {
			998,	// i4R				            
                                512,    // i4G
			1103	// i4B				            
                        },
                        // DNP
                        {
			1022,	// i4R				            
                                512,    // i4G
			959	// i4B				              
                        },
                        // D65
                        {
			1202,	// i4R				            
                                512,    // i4G
			722	// i4B				              
                        },
                        // DF
                        {
			1312,	// i4R				              
                                512,    // i4G
			833	// i4B				                
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
		-138,	// i4SlopeNumerator		
                        128    // i4SlopeDenominator
                },
	            // Predictor gain
                {
                        // i4PrefRatio100
                        101,

                        // DaylightLocus_L
                        {
                            1233,    // i4R
                            512, // i4G
                            703     // i4B
                        },
                        // DaylightLocus_H
                        {
                            938,    // i4R
                            512, // i4G
                            943     // i4B
                        },
                        // Temporal General
                        {
                            1233,    // i4R
                            512, // i4G
                            703     // i4B
                        }
                },
                // AWB light area
                {
                        // Strobe
                        {
			0,	// StrobeRightBound				            
			0,	// StrobeLeftBound				          
			0,	// StrobeUpperBound				          
			0	// StrobeLowerBound				          
                        },
                        // Tungsten
                        {
			-195,	// TungRightBound				            
			-841,	// TungLeftBound				            
			-399,	// TungUpperBound				            
			-471	// TungLowerBound				            
                        },
                        // Warm fluorescent
                        {
			-195,	// WFluoRightBound				          
			-841,	// WFluoLeftBound				            
			-471,	// WFluoUpperBound				          
			-529	// WFluoLowerBound				          
                        },
                        // Fluorescent
                        {
			-19,	// FluoRightBound				            
			-195,	// FluoLeftBound				            
			-417,	// FluoUpperBound				            
			-514	// FluoLowerBound				            
                        },
                        // CWF
                        {
			26,	// CWFRightBound				            
			-170,	// CWFLeftBound				              
			-514,	// CWFUpperBound				            
			-574	// CWFLowerBound				            
                        },
                        // Daylight
                        {
			202,	// DayRightBound				              
			-19,	// DayLeftBound				              
			-417,	// DayUpperBound				            
			-514	// DayLowerBound				            
                        },
                        // Shade
                        {
			532,	// ShadeRightBound				          
			202,	// ShadeLeftBound				              
			-417,	// ShadeUpperBound				          
			-481	// ShadeLowerBound				          
                        },
                        // Daylight Fluorescent
                        {
			202,	// DFRightBound				                
			26,	// DFLeftBound				                
			-514,	// DFUpperBound				                
			-574	// DFLowerBound				                  
                        }
                },
                // PWB light area
                {
                        // Reference area
                        {
			532,	// PRefRightBound				            
			-841,	// PRefLeftBound				            
			-374,	// PRefUpperBound				            
			-574	// PRefLowerBound				            
                        },
                        // Daylight
                        {
			227,	// PDayRightBound				            
			-19,	// PDayLeftBound				            
			-417,	// PDayUpperBound				            
			-514	// PDayLowerBound				            
                        },
                        // Cloudy daylight
                        {
			327,	// PCloudyRightBound				        
			152,	// PCloudyLeftBound				            
			-417,	// PCloudyUpperBound				        
			-514	// PCloudyLowerBound				        
                        },
                        // Shade
                        {
			427,	// PShadeRightBound				          
			152,	// PShadeLeftBound				          
			-417,	// PShadeUpperBound				          
			-514	// PShadeLowerBound				          
                        },
                        // Twilight
                        {
			-19,	// PTwiRightBound				            
			-179,	// PTwiLeftBound				            
			-417,	// PTwiUpperBound				            
			-514	// PTwiLowerBound				            
                        },
                        // Fluorescent
                        {
			222,	// PFluoRightBound				          
			-195,	// PFluoLeftBound				            
			-399,	// PFluoUpperBound				          
			-579	// PFluoLowerBound				          
                        },
                        // Warm fluorescent
                        {
			-194,	// PWFluoRightBound				          
			-394,	// PWFluoLeftBound				          
			-399,	// PWFluoUpperBound				          
			-579	// PWFluoLowerBound				          
                        },
                        // Incandescent
                        {
			-194,	// PIncaRightBound				          
			-394,	// PIncaLeftBound				            
			-417,	// PIncaUpperBound				          
			-514	// PIncaLowerBound				          
                        },
                        // Gray World
                        {
			5000,	// PGWRightBound				            
			-5000,	// PGWLeftBound				            
			5000,	// PGWUpperBound				            
			-5000	// PGWLowerBound				            
                        }
                },
                // PWB default gain
                {
                        // Daylight
                        {
			1125,	// PWB_Day_R				                
			512,	// PWB_Day_G				                
			812	// PWB_Day_B				                  
                        },
                        // Cloudy daylight
                        {
			1343,	// PWB_Cloudy_R				              
			512,	// PWB_Cloudy_G				              
			672	// PWB_Cloudy_B				                
                        },
                        // Shade
                        {
			1433,	// PWB_Shade_R				              
			512,	// PWB_Shade_G				              
			627	// PWB_Shade_B				                
                        },
                        // Twilight
                        {
			863,	// PWB_Twi_R				                
			512,	// PWB_Twi_G				                
			1079	// PWB_Twi_B				                
                        },
                        // Fluorescent
                        {
			1033,	// PWB_Fluo_R				                
			512,	// PWB_Fluo_G				                
			951	// PWB_Fluo_B				                  
                        },
                        // Warm fluorescent
                        {
			692,	// PWB_WFluo_R				              
			512,	// PWB_WFluo_G				              
			1462	// PWB_WFluo_B				              
                        },
                        // Incandescent
                        {
			669,	// PWB_Inca_R				                
			512,	// PWB_Inca_G				                
			1418	// PWB_Inca_B				                
                        },
                        // Gray World
                        {
			512,	// PWB_GW_R				                  
			512,	// PWB_GW_G				                  
			512	// PWB_GW_B				                    
                        }
                },
                // AWB preference color
                {
                        // Tungsten
                        {
			100,	// TUNG_SLIDER				                
			4004	// TUNG_OFFS				                
                        },
                        // Warm fluorescent
                        {
			100,	// WFluo_SLIDER				                
			4004	// WFluo_OFFS				                
                        },
                        // Shade
                        {
			50,	// Shade_SLIDER				                
			909	// Shade_OFFS				                  
                        },
                        // Preference gain: strobe
                        {
			512,	// PRF_STROBE_R				              
			512,	// PRF_STROBE_G				              
			512	// PRF_STROBE_B				                
                        },
                        // Preference gain: tungsten
                        {
			512,	// PRF_TUNG_R				                
			512,	// PRF_TUNG_G				                
			512	// PRF_TUNG_B				                  
                        },
                        // Preference gain: warm fluorescent
                        {
			512,	// PRF_WFluo_R				              
			512,	// PRF_WFluo_G				              
			512	// PRF_WFluo_B				                
                        },
                        // Preference gain: fluorescent
                        {
			512,	// PRF_Fluo_R				                
			512,	// PRF_Fluo_G				                
			512	// PRF_Fluo_B				                  
                        },
                        // Preference gain: CWF
                        {
			512,	// PRF_CWF_R				                
			512,	// PRF_CWF_G				                
			512	// PRF_CWF_B				                  
                        },
                        // Preference gain: daylight
                        {
			512,	// PRF_Day_R				                
			512,	// PRF_Day_G				                
			512	// PRF_Day_B				                  
                        },
                        // Preference gain: shade
                        {
			512,	// PRF_Shade_R				              
			512,	// PRF_Shade_G				              
			512	// PRF_Shade_B				                
                        },
                        // Preference gain: daylight fluorescent
                        {
			512,	// PRF_DF_R				                  
			512,	// PRF_DF_G				                  
			512	// PRF_DF_B				                    
                        }
                },

                // Algorithm Tuning Paramter
                {
                    // AWB Backup Enable
                    0,

                    // AWB LSC Gain
                    {
                        938,        // i4R
                        512, // i4G
                        943         // i4B
                    },
                    // Parent block weight parameter
                    {
                        1,      // bEnable
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
                        //LV0   1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        { 100, 100, 100, 100, 100, 100, 100, 100, 50,  25,   2,   2,   2,   2,   2,   2,   2,   2,   2}  // (%) i4CWFDF_LUTThr
                    },
                    // AWB light neutral noise reduction for outdoor
                    {
                        //LV0  1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        // Non neutral
		                { 5,   5,   5,   5,   5,   5,   5,   5,    5,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
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
                                19,   // i4Sunset_BoundXr_Thr
                                -488     // i4Sunset_BoundYr_Thr
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
                                -70,   // i4BoundXrThr
                                -449    // i4BoundYrThr
                            },
                            128         // i4DaylightProb
                        },

                        // Shade CWF Detection
                        {
                            1,          // i4Enable
                            95,         // i4LVThr
                            {
                                -56,   // i4BoundXrThr
                                -529    // i4BoundYrThr
                            },
                            128         // i4DaylightProb
                        },

                        // Low CCT
                        {
                            1,          // i4Enable
                            512,        // i4SpeedRatio
                            {
                               -491,       // i4BoundXrThr
                               387         // i4BoundYrThr
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
                            832     // Gain ratio
                        }

                    },

                    // AWB non-neutral probability for spatial and temporal weighting look-up table (Max: 100; Min: 0)
                    {
                        //LV0   1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        { 100, 100, 100, 100, 100, 100, 100, 100, 100, 70,  30,  20,  10,   0,   0,   0,   0,   0,   0}
                    },

                    // AWB daylight locus probability look-up table (Max: 100; Min: 0)
                    {   //LV0    1     2     3      4     5     6     7     8      9      10     11    12   13     14    15   16    17    18
		                { 100,  100,  100,  100,  100,  100,  100,  100,  100,  100,  100,  50,  0,  0,  0,  0,  0,  0,  0}, // Strobe
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Tungsten
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  25,  0,   0,  0,   0,   0}, // Warm fluorescent
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,  0,   0,  0,   0,   0}, // Fluorescent
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
			                3750,	// i4CCT[2]
			                5100,	// i4CCT[3]
			                6500 	// i4CCT[4]
		            },
		{						                                
			                -547,	// i4RotatedXCoordinate[0]
			                -423,	// i4RotatedXCoordinate[1]
			                -271,	// i4RotatedXCoordinate[2]
			                -104,	// i4RotatedXCoordinate[3]
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
                                1095,    // i4R
                                512,    // i4G
                                736    // i4B
                        }
                },
                {
                        {
                                0,    // i4X
                                0    // i4Y
                        },
                        {
                                -400,    // i4X
                                -390    // i4Y
                        },
                        {
                                -276,    // i4X
                                -411    // i4Y
                        },
                        {
                                -124,    // i4X
                                -459    // i4Y
                        },
                        // CWF
                        {
                                -89,    // i4X
                                -509    // i4Y
                        },
                        // DNP
                        {
                                43,    // i4X
                                -463    // i4Y
                        },
                        // D65
                        {
                                147,    // i4X
                                -415    // i4Y
                        },
                        // DF
                        {
                                0,    // i4X
                                0    // i4Y
                        }
                },
                // Rotated XY coordinate of AWB light source
                {
                        // Strobe
                        {
                                0,    // i4X
                                0    // i4Y
                        },
                        // Horizon
                        {
                                -400,    // i4X
                                -390    // i4Y
                        },
                        // A
                        {
                                -276,    // i4X
                                -411    // i4Y
                        },
                        // TL84
                        {
                                -124,    // i4X
                                -459    // i4Y
                        },
                        // CWF
                        {
                                -89,    // i4X
                                -509    // i4Y
                        },
                        // DNP
                        {
                                43,    // i4X
                                -463    // i4Y
                        },
                        // D65
                        {
                                147,    // i4X
                                -415    // i4Y
                        },
                        // DF
                        {
                                138,    // i4X
                                -498    // i4Y
                        }
                },
                // AWB gain of AWB light source
                {
                        // Strobe
                        {
                                810,    // i4R
                                512,    // i4G
                                677    // i4B
                        },
                        // Horizon
                        {
                                512,    // i4R
                                519,    // i4G
                                1514    // i4B
                        },
                        // A
                        {
                                615,    // i4R
                                512,    // i4G
                                1298    // i4B
                        },
                        // TL84
                        {
                                806,    // i4R
                                512,    // i4G
                                1128    // i4B
                        },
                        // CWF
                        {
                                904,    // i4R
                                512,    // i4G
                                1150    // i4B
                        },
                        // DNP
                        {
                                1015,    // i4R
                                512,    // i4G
                                903    // i4B
                        },
                        // D65
                        {
                                1095,    // i4R
                                512,    // i4G
                                736    // i4B
                        },
                        // DF
                        {
                                512,    // i4R
                                512,    // i4G
                                512    // i4B
                        }
                },
                // Rotation matrix parameter
                {
                        0,    // i4RotationAngle
                        256,    // i4Cos
                        0    // i4Sin
                },
                // Daylight locus parameter
                {
                        -126,    // i4SlopeNumerator
                        128    // i4SlopeDenominator
                },
	            // Predictor gain
                {
                        // i4PrefRatio100
                        101,

                        // DaylightLocus_L
                        {
                            1114,    // i4R
                            512,    // i4G
                            724     // i4B
                        },
                        // DaylightLocus_H
                        {
                            933,    // i4R
                            512,    // i4G
                            865     // i4B
                        },
                        // Temporal General
                        {
                            1114,    // i4R
                            512,    // i4G
                            724     // i4B
                        }
                },
                // AWB light area
                {
                        // Strobe:FIXME
                        {
                                -100, // i4RightBound
                                -250, // i4LeftBound
                                -361, // i4UpperBound
                                -600  // i4LowerBound
                        },
                        // Tungsten
                        {
                                -200,    // i4RightBound
                                -800,    // i4LeftBound
                                -355,    // i4UpperBound
                                -435    // i4LowerBound
                        },
                        // Warm fluorescent
                        {
                                -200,    // i4RightBound
                                -800,    // i4LeftBound
                                -435,    // i4UpperBound
                                -509    // i4LowerBound
                        },
                        // Fluorescent
                        {
                                1,    // i4RightBound
                                -200,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // CWF
                        {
                                2,    // i4RightBound
                                -180,    // i4LeftBound
                                -490,    // i4UpperBound
                                -554    // i4LowerBound
                        },
                        // Daylight
                        {
                               177,    // i4RightBound
                                1,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // Shade
                        {
                                507,    // i4RightBound
                                177,    // i4LeftBound
                                -373,    // i4UpperBound
                                -453    // i4LowerBound
                        },
                        // Daylight Fluorescent
                        {
                                177,    // i4RightBound
                                2,    // i4LeftBound
                                -490,    // i4UpperBound
                                -554    // i4LowerBound
                        }
                },
                // PWB light area
                {
                        // Reference area
                        {
                                507,    // i4RightBound
                                -800,    // i4LeftBound
                                -330,    // i4UpperBound
                                -554    // i4LowerBound
                        },
                        // Daylight
                        {
                                202,    // i4RightBound
                                1,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // Cloudy daylight
                        {
                                302,    // i4RightBound
                                127,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // Shade
                        {
                                402,    // i4RightBound
                                127,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // Twilight
                        {
                                1,    // i4RightBound
                                -159,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
                        },
                        // Fluorescent
                        {
                                197,    // i4RightBound
                                -224,    // i4LeftBound
                                -365,    // i4UpperBound
                                -559    // i4LowerBound
                        },
                        // Warm fluorescent
                        {
                                -176,    // i4RightBound
                                -376,    // i4LeftBound
                                -365,    // i4UpperBound
                                -559    // i4LowerBound
                        },
                        // Incandescent
                        {
                                -176,    // i4RightBound
                                -376,    // i4LeftBound
                                -373,    // i4UpperBound
                                -490    // i4LowerBound
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
                                1054,    // i4R
                                512,    // i4G
                                800    // i4B
                        },
                        // Cloudy daylight
                        {
                                1228,    // i4R
                                512,    // i4G
                                687    // i4B
                        },
                        // Shade
                        {
                                1314,    // i4R
                                512,    // i4G
                                642    // i4B
                        },
                        // Twilight
                        {
                                825,    // i4R
                                512,    // i4G
                                1022    // i4B
                        },
                        // Fluorescent
                        {
                                940,    // i4R
                                512,    // i4G
                                975    // i4B
                        },
                        // Warm fluorescent
                        {
                                659,    // i4R
                                512,    // i4G
                                1391    // i4B
                        },
                        // Incandescent
                        {
                                632,    // i4R
                                512,    // i4G
                                1334    // i4B
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
                                100,    // i4SliderValue
                                3727    // i4OffsetThr
                        },
                        // Warm fluorescent
                        {
                                100,    // i4SliderValue
                                3727    // i4OffsetThr
                        },
                        // Shade
                        {
                                50,    // i4SliderValue
                                909    // i4OffsetThr
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
                    0,

                    // AWB LSC Gain
                    {
                        933, //856,        // i4R
                        512,        // i4G
                        865, //942         // i4B
                    },
                    // Parent block weight parameter
                    {
                        1,      // bEnable
                        6           // i4ScalingFactor: [6] 1~12, [7] 1~6, [8] 1~3, [9] 1~2, [>=10]: 1
                    },
                    // AWB LV threshold for predictor
                    {
                            115,    // i4InitLVThr_L
                            155,    // i4InitLVThr_H
                            80      // i4EnqueueLVThr
                    },
                    // AWB number threshold for temporal predictor
                    {
                            65,     // i4Neutral_ParentBlk_Thr
                        //LV0  1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        { 100, 100, 100, 100, 100, 100, 100, 100, 100, 50,  25,   2,   2,   2,   2,   2,   2,   2,   2}  // (%) i4CWFDF_LUTThr
                    },
                    // AWB light neutral noise reduction for outdoor
                    {
                        //LV0  1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18
                        // Non neutral
		                { 5,   5,   5,   5,   5,   5,   5,   5,    5,   5,  10,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // Flurescent
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   5,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // CWF
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   5,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
		                // Daylight
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   0,   2,   4,   4,   4,   4,   4,   4,   4},  // (%)
		                // DF
		                { 0,   0,   0,   0,   0,   0,   0,   0,    0,   0,   5,  10,  10,  10,  10,  10,  10,  10,  10},  // (%)
                    },
                    // AWB feature detection
                    {
                        // Sunset Prop
                        {
                            1,          // i4Enable
                            145,        // i4LVThr
                            {
                                64,   // i4Sunset_BoundXr_Thr
                                -463     // i4Sunset_BoundYr_Thr
                            },
                            10,         // i4SunsetCountThr
                            0,          // i4SunsetCountRatio_L
                            171         // i4SunsetCountRatio_H
                        },

                        // Shade F Detection
                        {
                            1,          // i4Enable
                            110,        // i4LVThr
                            {

                                -82,   // i4BoundXrThr
                                -415    // i4BoundYrThr
                            },
                            128         // i4DaylightProb
                        },

                        // Shade CWF Detection
                        {
                            1,          // i4Enable
                            100,         // i4LVThr
                            {
                                -89,   // i4BoundXrThr
                                -509    // i4BoundYrThr
                            },
                            128         // i4DaylightProb
                        },

                        // Low CCT
                        {
                            1,          // i4Enable
                            512,        // i4SpeedRatio
                            {
                                -450,       // i4BoundXrThr
                                450         // i4BoundYrThr
                            }
                        }

                    },

                    // AWB Gain Limit
                    {
                        // rNormalLowCCT
                        {
                            1,      // Gain Limit Enable
                            691 //717     // Gain ratio
                        },
                        // rPrefLowCCT
                        {
                            1,      // Gain Limit Enable
                            768 //832     // Gain ratio
                        }

                    },

                    // AWB non-neutral probability for spatial and temporal weighting look-up table (Max: 100; Min: 0)
                    {
                        //LV0   1    2    3    4    5    6    7    8    9   10   11   12   13   14   15   16   17   18
                        { 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 70,  30,  20,  10,   0,   0,   0,   0,   0}
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
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 75,  35,   0,   0,  0,   0,   0}, // Strobe
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 100, 75,  25,   0,  0,   0,   0}, // Tungsten
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 100, 75,  25,   0,  0,   0,   0}, // Warm fluorescent
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 100, 50,  25,   0,  0,   0,   0}, // Fluorescent
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,   0,   0,  0,   0,   0}, // CWF
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 100, 75,  50,  50, 40,  30,   0}, // Daylight
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,   0,   0,  0,   0,   0}, // Shade
                        {100, 100, 100, 100, 100, 100, 100, 100, 100, 100,  100,  100, 50,  25,   0,   0,  0,   0,   0}  // Daylight fluorescent
    		        }
                },

	            // CCT estimation
	            {
		            // CCT
		            {
			                2300,	// i4CCT[0]
			                2850,	// i4CCT[1]
			                3750,	// i4CCT[2]
			                5100,	// i4CCT[3]
			                6500 	// i4CCT[4]
		            },
		            // Rotated X coordinate
		            {
			                -547,	// i4RotatedXCoordinate[0]
			                -423,	// i4RotatedXCoordinate[1]
			                -271,	// i4RotatedXCoordinate[2]
			                -104,	// i4RotatedXCoordinate[3]
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
        1,  // isTsfEn
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


