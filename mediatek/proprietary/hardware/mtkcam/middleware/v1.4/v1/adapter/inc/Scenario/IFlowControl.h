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

#ifndef _MTK_HARDWARE_INCLUDE_MTKCAM_V1_LEGACYPIPELINE_IFLOWCONTROL_H_
#define _MTK_HARDWARE_INCLUDE_MTKCAM_V1_LEGACYPIPELINE_IFLOWCONTROL_H_
//
#include <utils/RefBase.h>
#include <utils/Vector.h>
#include <LegacyPipeline/StreamBufferProvider.h>
#include <v1/IParamsManager.h>
using namespace android;
using namespace android::MtkCamUtils;
#include <v1/camutils/IImgBufQueue.h>
#include <ImgBufProvidersManager.h>

typedef NSCam::v3::Utils::HalMetaStreamBuffer HalMetaStreamBuffer;

/******************************************************************************
 *
 ******************************************************************************/
//
namespace NSCam {
namespace v1 {
namespace NSLegacyPipeline {

class IFlowControl
    : public virtual RefBase
{
public:

    struct ControlType_T{
        enum {
            CONTROL_DEFAULT,
            CONTROL_ENG
        };
    };

    static sp< IFlowControl >   createInstance(
                                    char const*                pcszName,
                                    MINT32 const               i4OpenId,
                                    MINT32                     type,
                                    sp<IParamsManager>         pParamsManager,
                                    sp<ImgBufProvidersManager> pImgBufProvidersManager
                                );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interfaces.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////
    virtual                         ~IFlowControl() {};

    virtual char const*             getName()   const                       = 0;

    virtual int32_t                 getOpenId() const                       = 0;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interfaces for CamAdapter.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    /**
     * Start preview mode.
     */
    virtual status_t                startPreview()                          = 0;

    /**
     * Stop a previously started preview.
     */
    virtual status_t                stopPreview()                           = 0;

    /**
     * Start record mode. When a record image is available a CAMERA_MSG_VIDEO_FRAME
     * message is sent with the corresponding frame. Every record frame must be released
     * by a cameral hal client via releaseRecordingFrame() before the client calls
     * disableMsgType(CAMERA_MSG_VIDEO_FRAME). After the client calls
     * disableMsgType(CAMERA_MSG_VIDEO_FRAME), it is camera hal's responsibility
     * to manage the life-cycle of the video recording frames, and the client must
     * not modify/access any video recording frames.
     */
    virtual status_t                startRecording()                        = 0;

    /**
     * Stop a previously started recording.
     */
    virtual status_t                stopRecording()                         = 0;

    /**
     * Start auto focus, the notification callback routine is called
     * with CAMERA_MSG_FOCUS once when focusing is complete. autoFocus()
     * will be called again if another auto focus is needed.
     */
    virtual status_t                autoFocus()                             = 0;

    /**
     * Cancels auto-focus function. If the auto-focus is still in progress,
     * this function will cancel it. Whether the auto-focus is in progress
     * or not, this function will return the focus position to the default.
     * If the camera does not support auto-focus, this is a no-op.
     */
    virtual status_t                cancelAutoFocus()                       = 0;

    /**
     * Start pre-capture.
     */
    virtual status_t                precapture()                            = 0;

    /**
     * Set the camera parameters. This returns BAD_VALUE if any parameter is
     * invalid or not supported.
     */
    virtual status_t                setParameters()                         = 0;

    /**
     * Send command to camera driver.
     */
    virtual status_t                sendCommand(
                                        int32_t cmd,
                                        int32_t arg1,
                                        int32_t arg2
                                    )                                       = 0;

    /**
     *
     */
    virtual status_t                dump(
                                        int fd,
                                        Vector<String8>const& args
                                    )                                       = 0;

};

class IFeatureFlowControl
    : public virtual RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual status_t                queryCurrentPipelineId(int& id)         = 0;

    /**
     * Submit user defined request.
     *
     * @param[in] setting: setting for pipeline frame.
     *
     * @param[in] resultBuffer: empty buffer pool to get result buffers.
     *
     */
    virtual MERROR                  submitRequest(
                                        Vector< IMetadata* >                settings,
                                        android::sp<StreamBufferProvider>& resultBuffer
                                    ) const                                 = 0;

};

class IRequestUpdater
    : public virtual RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    /**
     * Update App request setting by scenario.
     */
    virtual MERROR                    updateAppSetting(
                                          IMetadata* setting
                                      )                                     = 0;

    /**
     * Update Hal request setting by scenario.
     */
    virtual MERROR                    updateHalSetting(
                                          IMetadata* setting
                                      )                                     = 0;

    /**
     * Notify request number for specific setting.
     */
#warning "TBD"
    virtual MERROR                    notifySettingRequestNo(
                                          MINT32    rRequestNo,
                                          IMetadata* setting
                                      )                                     = 0;

#warning "FIXME"
    virtual android::sp<IPipelineFrame> constructPipelineFrame(
                                                    MINT32  requestNo,
                                                    android::sp<IMetaStreamBuffer> pAppMetaControlSB,
                                                    android::sp<HalMetaStreamBuffer> pHalMetaControlSB
                                                ) = 0;
};

/******************************************************************************
 *
 ******************************************************************************/
};  //namespace NSLegacyPipeline
};  //namespace v1
};  //namespace NSCam
#endif  //_MTK_HARDWARE_INCLUDE_MTKCAM_V1_LEGACYPIPELINE_IFLOWCONTROL_H_
