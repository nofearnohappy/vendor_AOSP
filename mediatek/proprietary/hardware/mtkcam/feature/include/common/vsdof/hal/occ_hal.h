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
#ifndef _OCC_HAL_H_
#define _OCC_HAL_H_

#include "stereo_common.h"

using namespace StereoHAL;

struct OCC_HAL_PARAMS
{
    IImageBuffer    *imageMain1;            //YV12, output of N3D HAL
    IImageBuffer    *imageMain2;            //YV12, output of N3D HAL
    MUINT16         *disparityLeftToRight;  //16-bit, output of DPE
    MUINT16         *disparityRightToLEft;  //16-bit, output of DPE
    MUINT8          *ldcMain1;              //8-bit, output of N3D HAL
    MUINT8          *confidenceMap;         //not used now
};

struct OCC_HAL_OUTPUT
{
    IImageBuffer *downScaledImg;    //YV12      MY_S
    MUINT8 *depthMap;               //Y only    DMH
    MUINT8 *occMap;                 //Y only, not used now
    MUINT8 *nocMap;                 //Y only, not used now
};

class OCC_HAL
{
public:
    static OCC_HAL *createInstance();
    //
    virtual bool OCCHALRun(OCC_HAL_PARAMS &occHalParam, OCC_HAL_OUTPUT &occHalOutput) = 0;
protected:
    OCC_HAL() {}
    virtual ~OCC_HAL() {}
};

#endif