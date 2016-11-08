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

#define LOG_TAG "MtkCam/PipeMgr"

#include <list>
#include "MyUtils.h"
#include <v1/IParamsManager.h>

#include <metadata/client/mtk_metadata_tag.h>

#include <v3/pipeline/IPipelineDAG.h>
#include <v3/pipeline/IPipelineNode.h>
#include <v3/pipeline/IPipelineNodeMapControl.h>
#include <v3/pipeline/IPipelineFrameControl.h>
#include <v3/pipeline/PipelineContext.h>
#include <v3/pipeline/PipelineContextImpl.h>

#include <v3/utils/streambuf/StreamBufferPool.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>

#include <IHalSensor.h>

#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>

#include <imageio/ispio_utility.h>

#include <metadata/ITemplateRequest.h>
#include <metadata/IMetadataProvider.h>

using namespace NSCam;
using namespace v3;
using namespace NSCam::v3::Utils;
using namespace android;
using namespace NSCam::v3::NSPipelineContext;
#include <v1/StreamIDs.h>
using namespace NSMtkStreamId;
//
using namespace MtkCamUtils;
#include <v1/adapter/inc/ImgBufProvidersManager.h>
#include <v3/pipeline/IPipelineBufferSetFrameControl.h>
using namespace NSCam::v3;
#include <v1/StreamBufferProviders/BufMgr.h>
using namespace NSMtkBufMgr;
#include <v1/PipeDataInfo.h>
using namespace NSMtkPipeDataInfo;
#include <v1/StreamBufferProviders/CamClientStreamBufHandler.h>
#include <v1/StreamBufferProviders/ZsdPrvStreamBufHandler.h>
#include <v1/StreamBufferProviders/ZsdShotStreamBufHandler.h>
#include <v1/StreamBufferProviders/JpgStreamBufHandler.h>
using namespace NSCamStreamBufProvider;
#include <v1/converter/RequestSettingBuilder.h>
#include <v1/PipeMgr/PipeMgr.h>
using namespace NSMtkPipeMgr;
//
#include "PipeMgrImp.h"
//
#include "PipeMgrImp.NorPrv.h"
#include "PipeMgrImp.NorCap.h"
#include "PipeMgrImp.VdoRec.h"
#include "PipeMgrImp.ZsdPrv.h"
#include "PipeMgrImp.ZsdCap.h"
using namespace NSMtkPipeMgrImp;
//
/******************************************************************************
 *
 ******************************************************************************/
sp<PipeMgr>
PipeMgr::
createInstance(
    MUINT32     openId,
    EPipeScen   pipeScenario)
{
    CAM_LOGD("[createInstance]+ openId(%d), pipeScenario(%d)",
            openId,
            pipeScenario);
    //
    sp<PipeMgr> spPipeMgrBase = NULL;
    //
    switch(pipeScenario)
    {
        case EPipeScen_NorPrv:
        {
            spPipeMgrBase = new PipeMgrImpNorPrv(openId, pipeScenario);
            break;
        }
        case EPipeScen_NorCap:
        {
            spPipeMgrBase = new PipeMgrImpNorCap(openId, pipeScenario);
            break;
        }
        case EPipeScen_VdoRec:
        {
            spPipeMgrBase = new PipeMgrImpVdoRec(openId, pipeScenario);
            break;
        }
        case EPipeScen_VdoRecLowPwr:
        {
            //TBD
            break;
        }
        case EPipeScen_VdoRecSlowMontion:
        {
            //TBD
            break;
        }
        case EPipeScen_Vss:
        {
            //TBD
            break;
        }
        case EPipeScen_VssLowPwr:
        {
            //TBD
            break;
        }
        case EPipeScen_ZsdPrv:
        {
            spPipeMgrBase = new PipeMgrImpZsdPrv(openId, pipeScenario);
            break;
        }
        case EPipeScen_ZsdCap:
        {
            spPipeMgrBase = new PipeMgrImpZsdCap(openId, pipeScenario);
            break;
        }
        default:
        {
            CAM_LOGE("[createInstance]un-supported scenario %d",pipeScenario);
            break;
        }
    }
    //
    CAM_LOGD("[createInstance]-");
    return spPipeMgrBase;
}

