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

#ifndef _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_LEGACYPIPELINEMANAGER_H_
#define _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_LEGACYPIPELINEMANAGER_H_

namespace NSCam {
namespace v1 {
namespace NSLegacyPipeline {
/******************************************************************************
 *
 ******************************************************************************/
class LegacyPipelineManager
        : public virtual RefBase
{
friend class LegacyPipelineBuilder;
friend class ILegacyPipeline;

protected:  ////                    Interface for create and destroy LegacyPipelineManager.
                                        LegacyPipelineManager(MINT32 const sensorId);
                                        ~LegacyPipelineManager();

protected:  ////                    Interface for LegacyPipelineBuilder.
    /**
     * register legacy pipeline.
     *
     * @param[in] legacyPipeline: legacy pipeline.
     *
     * @return: legacy pipeline id, -1 if input is NULL
     *
     */
            MINT32                      registerLegacyPipeline(sp<ILegacyPipeline> const legacyPipeline);

    /**
     * get sensor id.
     *
     * @return: sensor id.
     *
     */
            MINT32                      getSensorId() { return mSensorId; }

public:     ////                    Interface for control flow, shot and LegacyPipelineBuilder to register consumer Stream Buffer Provider.
    /**
     * register consumer Stream Buffer Provider.
     *
     * @param[in] consumerBufferProvider: consumer Stream Buffer Provider.
     *
     */
            MERROR                      registerConsumerBufferProvider(sp<StreamBufferProvider> const consumerBufferProvider);

public:     ////                    Interface for control flow and shot.
    /**
     * get Legacy Pipeline Manager from sensor id.
     *
     * @param[in] sensorId: sensor id.
     *
     * @return: Legacy Pipeline Manager.
     *
     */
    static  sp<LegacyPipelineManager>   getInstance(MINT32 const sensorId);

    /**
     * get legacy pipeline from id.
     *
     * @param[in] legacyPipelineId: legacy pipeline id.
     *
     * @return: Legacy Pipeline.
     *
     */
            wp<ILegacyPipeline>         getLegacyPipeline(MINT32 const legacyPipelineId);

    /**
     * destroy legacy pipeline from id.
     *
     * @param[in] legacyPipelineId: legacy pipeline id.
     *
     */
            MVOID                       destroyLegacyPipeline(MINT32 const legacyPipelineId);

    /**
     * get consumer buffer provider from stream id.
     *
     * @param[in] streamId: stream id.
     *
     * @return: consumer Stream Buffer Provider.
     *
     */
            sp<StreamBufferProvider>    getConsumerBufferProvider(StreamId_T const streamId);

    /**
     * dump Legacy Pipeline Manager.
     *
     */
            MVOID                       dump();

protected:
    static  vector<wp<LegacyPipelineManager> >  mgrs;

            MINT32                              mSensorId;
            MINT32                              mPipelineIdCount;
            vector<sp<ILegacyPipeline> >        mspvLegacyPipeline;
            vector<sp<StreamBufferProvider> >   mspvConsumerBufferProvider;
};
}; //namespace NSLegacyPipeline
}; //namespace v1
}; //namespace NSCam
#endif
