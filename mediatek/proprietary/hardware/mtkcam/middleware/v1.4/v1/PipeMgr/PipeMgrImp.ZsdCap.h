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
#ifndef MTK_PIPE_MGR_IMP_ZSD_CAP_INC_H
#define MTK_PIPE_MGR_IMP_ZSD_CAP_INC_H
//
/*******************************************************************************
*
*******************************************************************************/
namespace NSMtkPipeMgrImp
{

class PipeMgrImpZsdCap : public NSMtkPipeMgrImp::PipeMgrImp
{
    public:
                PipeMgrImpZsdCap(MUINT32 openId, EPipeScen pipeScenario);
                ~PipeMgrImpZsdCap();

    protected:
        //by scenario functions
                MVOID   prepareSensor() { return; }
                MVOID   closeSensor() { return; }
                MVOID   prepareConfiguration() { return; }
        virtual MVOID   setupStreamBufferProvider();
        virtual MVOID   updateStreamBufferProvider();
        virtual MVOID   setupPipelineStreamInfo();
        virtual MVOID   setupPipelineStreamConfig();
        virtual MVOID   setupPipelineStream();
        virtual MVOID   setupPipelineNode();
        virtual MVOID   setupPipelineFlow();
        virtual MVOID   finishStreamBufferProvider();
        virtual sp<IPipelineFrame>  getPipelineFrame();
                MBOOL   sendMetadata(
                            MINT32              requestNumber,
                            StreamId_T const    streamId,
                            IMetadata*          pMetadata) { return true; }

        //member variables
        sp<CamClientStreamBufHandler>       mspCamClientStreamBufHandler;
        sp<ZsdShotStreamBufHandler>         mspZsdShotStreamBufHandler;
        sp<JpgStreamBufHandler>             mspJpgStreamBufHandler;

        MSize   mRrzoSize;
        MINT    mRrzoFormat;
        size_t  mRrzoStride;
        MSize   mImgoSize;
        MINT    mImgoFormat;
        size_t  mImgoStride;

};

}
#endif

