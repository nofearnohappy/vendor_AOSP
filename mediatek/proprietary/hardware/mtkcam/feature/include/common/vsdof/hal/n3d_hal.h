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
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef _N3D_HAL_H_
#define _N3D_HAL_H_

#include <vector>
#include "stereo_common.h"

using namespace StereoHAL;

const MUINT32 MAX_GEO_LEVEL  = 3;

struct HWFEFM_DATA
{
    MUINT16 *geoDataMain1[MAX_GEO_LEVEL];       //FE
    MUINT16 *geoDataMain2[MAX_GEO_LEVEL];       //FE
    MUINT16 *geoDataLeftToRight[MAX_GEO_LEVEL]; //FM
    MUINT16 *geoDataRightToLeft[MAX_GEO_LEVEL]; //FM

    HWFEFM_DATA() {
        ::memset(this, 0, sizeof(HWFEFM_DATA));
    }
};

struct SWFEFM_DATA
{
    MUINT8* geoSrcImgMain1[MAX_GEO_LEVEL];
    MUINT8* geoSrcImgMain2[MAX_GEO_LEVEL];
};

struct EIS_DATA
{
    bool isON;
    MPoint eisOffset;
    MSize eisImgSize;

    EIS_DATA() {
        ::memset(this, 0, sizeof(EIS_DATA));
    }
};

struct N3D_HAL_INIT_PARAM
{
    ENUM_STEREO_SCENARIO    eScenario;
    MUINT8                  fefmRound;
};

struct N3D_HAL_PARAM_COMMON
{
    HWFEFM_DATA hwfefmData;
    MUINT8 *rectifyImgMain1;    //warp_src_addr_main

    MUINT8 *ccImgMain1;         //pho_src_addr_main
    MUINT8 *ccImgMain2;         //pho_src_addr_auxi
};

struct N3D_HAL_PARAM : public N3D_HAL_PARAM_COMMON  //For Preview/VR
{
    EIS_DATA eisData;           //NULL is off

    MUINT8 *rectifyImgMain2;    //warp_src_addr_auxi
};

struct N3D_HAL_OUTPUT
{
    MUINT8 *rectifyImgMain1;    //warp_dst_addr_main
    MUINT8 *maskMain1;          //warp_msk_addr_main    //point to m_main1Mask

    MUINT8 *rectifyImgMain2;    //warp_dst_addr_auxi
    MUINT8 *maskMain2;          //warp_msk_addr_auxi

    MUINT8 *ldcMain1;           //warp_ldc_addr_main
};

struct N3D_HAL_PARAM_CAPTURE : public N3D_HAL_PARAM_COMMON
{
    void    *rectifyGBMain2;     //YV12, 2176x1152
    MUINT32 captureOrientation;
};

struct N3D_HAL_PARAM_CAPTURE_SWFE : public N3D_HAL_PARAM_COMMON
{
    SWFEFM_DATA *swfeData;
};

struct N3D_HAL_OUTPUT_CAPTURE : public N3D_HAL_OUTPUT
{
    //Main1 is output by DepthMap_Node
    MUINT8 *jpsImgMain2;  //1080 YV12 main2 image with warpping
    MUINT8 *stereoDebugInfo;
};

struct RUN_LENGTH_DATA
{
    //Data is always 0/255
    MUINT32 offset;
    MUINT32 len;

    RUN_LENGTH_DATA(MUINT32 o, MUINT32 l) {
        offset = o;
        len = l;
    }
};

class N3D_HAL
{
public:
    static N3D_HAL *createInstance();
    //
    virtual bool N3DHALInit(N3D_HAL_INIT_PARAM &n3dInitParam) = 0;
    //
    virtual bool N3DHALRun(N3D_HAL_PARAM &n3dParams, N3D_HAL_OUTPUT &n3dOutput) = 0;
    //
    virtual bool N3DHALRun(N3D_HAL_PARAM_CAPTURE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput) = 0;
    //
    virtual bool N3DHALRun(N3D_HAL_PARAM_CAPTURE_SWFE &n3dParams, N3D_HAL_OUTPUT_CAPTURE &n3dOutput) = 0;
    //
    virtual char *getStereoExtraData() = 0;
protected:
    N3D_HAL() {}
    virtual ~N3D_HAL() {}
};

#endif  // _N3D_HAL_H_