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

/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#ifndef _MTK_BSS_H
#define _MTK_BSS_H

#include "MTKBssType.h"
#include "MTKBssErrCode.h"

typedef enum BSS_PROC_ENUM
{
    BSS_PROC1 = 0,
    BSS_UNKNOWN_PROC,
} BSS_PROC_ENUM;

typedef enum DRVBssObject_s
{
    DRV_BSS_OBJ_NONE = 0,
    DRV_BSS_OBJ_SW,
    DRV_BSS_OBJ_SW_NEON,
    DRV_BSS_OBJ_UNKNOWN = 0xFF,
} DrvBssObject_e;

typedef enum BSS_FTCTRL_ENUM
{
    BSS_FTCTRL_GET_RESULT,
    BSS_FTCTRL_SET_PROC_INFO,
    BSS_FTCTRL_MAX
}   BSS_FTCTRL_ENUM;

typedef enum BSS_STATE_ENUM
{
    BSS_STATE_IDLE=0,
    BSS_STATE_STANDBY,
    BSS_STATE_INIT,
    v_STATE_PROC1_READY,
    BSS_STATE_PROC2_READY,
    v_STATE_PROC1,
    BSS_STATE_PROC2,
    BSS_STATE_MAX,
} BSS_STATE_ENUM;

struct BSS_PARAM_STRUCT
{
    MUINT8  BSS_ON;
    MUINT32 BSS_ROI_WIDTH;
    MUINT32 BSS_ROI_HEIGHT;
    MUINT32 BSS_ROI_X0;
    MUINT32 BSS_ROI_Y0;
    MUINT32 BSS_SCALE_FACTOR;
    MUINT32 BSS_CLIP_TH0;
    MUINT32 BSS_CLIP_TH1;
    MUINT32 BSS_ZERO;
};


struct Gmv {
    MINT32  x;
    MINT32  y;
};


struct BSS_INPUT_DATA {
    MUINT32 inMEWidth;
    MUINT32 inMEHeight;
    //
    MUINT8 *Proc1QBImg;
    MUINT8 *Proc1QR1Img;
    MUINT8 *Proc1QR2Img;
    MUINT8 *Proc1QR3Img;
    //
    Gmv gmv[4];
};

struct BSS_OUTPUT_DATA {
    MUINT32 originalOrder[4];
    Gmv gmv[4];
};

/*****************************************************************************
    Main Module
******************************************************************************/


/*****************************************************************************
  BSS INIT
******************************************************************************/


/*******************************************************************************
*
********************************************************************************/
class MTKBss
{
public:
    static MTKBss* createInstance(DrvBssObject_e eobject);
    virtual void   destroyInstance() = 0;
    virtual ~MTKBss(){}
    virtual MRESULT BssInit(void* pParaIn, void* pParaOut);
    virtual MRESULT BssReset(void);
    virtual MRESULT BssMain(BSS_PROC_ENUM ProcId, void* pParaIn, void* pParaOut);
    virtual MRESULT BssFeatureCtrl(BSS_FTCTRL_ENUM FcId, void* pParaIn, void* pParaOut);
private:

};

class AppBssTmp : public MTKBss
{
public:

    static MTKBss* getInstance();
    virtual void destroyInstance();

    AppBssTmp() {};
    virtual ~AppBssTmp() {};
};
#endif

