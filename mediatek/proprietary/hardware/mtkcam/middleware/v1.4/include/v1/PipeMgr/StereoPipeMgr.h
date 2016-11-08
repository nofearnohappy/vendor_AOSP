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

#ifndef MTK_STEREO_PIPE_MGR_INC_H
#define MTK_STEREO_PIPE_MGR_INC_H
/*******************************************************************************
*
*******************************************************************************/
namespace NSMtkStereoPipeMgr
{

class StereoPipeMgr : public virtual RefBase
{
    public:

        typedef enum{
            EPipeScen_StereoPrvP1 = 0,
            EPipeScen_StereoPrvP1Main2,
            EPipeScen_StereoPrvP2,
            EPipeScen_StereoCapP2,
            EPipeScen_StereoVdoP1,
            EPipeScen_StereoVdoP1Main2,
            EPipeScen_StereoZsdP1,
            EPipeScen_StereoZsdP1Main2
        }EPipeScen;

        typedef enum{//for runtime change flow
            EPipeFlow_Default = 0,
            EPipeFlow_Record1080p,
            EPipeFlow_Record720p
        }EPipeFlow;

        typedef struct{
            sp<IParamsManager>                                  spParamsMgr;
            sp<ImgBufProvidersManager>                          spImgBufProvidersMgr;
            sp<StereoStreamBufHandler>                          spStreamBufHandler;
            StereoBufMgr*                                       pBufMgr_IMGO_Main1;
            StereoBufMgr*                                       pBufMgr_RRZO_Main1;
            StereoBufMgr*                                       pBufMgr_IMGO_Main2;
            StereoBufMgr*                                       pBufMgr_RRZO_Main2;
            PipeDataCallback_t                                  pipeImgCb;
            MVOID*                                              pipeImgCbUser;
            sp<IPipelineBufferSetFrameControl::IAppCallback>    spResultProcessor;
            sp<RequestSettingBuilder>                           spRequestSettingBuilder;
            // For UT use.
            MetaStreamManager::metadata_info_setting*           pMetadata_Info_Setting      = NULL;
            ImageStreamManager::image_stream_info_pool_setting* pImageData_Info_Setting     = NULL;
            MINT32*                                             pNodeConnect_Info_Setting   = NULL;
        }PipeMgrParams;

        static sp<StereoPipeMgr>  createInstance(MUINT32 openId, EPipeScen pipeScenario);
        virtual void        destroyInstance() = 0;

        virtual MBOOL       createPipe(PipeMgrParams* pPipeMgrParams) = 0;
        virtual MBOOL       destroyPipe() = 0;

        virtual MBOOL       startLoop() = 0;
        virtual MBOOL       startOne(/*IMetadata* capBufShotMeta, HalImageStreamBuffer* capBufShotImageBuf*/) = 0;
        virtual MBOOL       stop() = 0;//blocking

        virtual MBOOL       setPipeFlow(/*EPipeFlow pipeFlow*/) = 0;

        virtual MBOOL       sendMetadata(
                                MINT32              requestNumber,
                                StreamId_T const    streamId,
                                IMetadata*          pMetadata) = 0;

        virtual EPipeScen   getPipeScenario() = 0;
        virtual EPipeFlow   getPipeFlow() = 0;

        virtual MUINT32     getRequestNumMin() = 0;
        virtual MUINT32     getRequestNumMax() = 0;
};

}
#endif // end of #ifndef MTK_STEREO_PIPE_MGR_INC_H

