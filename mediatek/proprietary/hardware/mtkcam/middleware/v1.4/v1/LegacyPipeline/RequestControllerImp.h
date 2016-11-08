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

#ifndef _MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_REQUESTCONTROLLERIMP_H_
#define _MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_REQUESTCONTROLLERIMP_H_
//
#include <utils/RefBase.h>
#include <utils/StrongPointer.h>
#include <utils/String8.h>
#include <utils/Vector.h>
//
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>
//
#include <metadata/IMetadata.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streambuf/StreamBufferProvider.h>
#include <v1/Processor/StreamingProcessor.h>
#include <LegacyPipeline/StreamBufferProvider.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <LegacyPipeline/IRequestController.h>

typedef NSCam::v3::IMetaStreamInfo            IMetaStreamInfo;
typedef NSCam::v3::IMetaStreamBuffer          IMetaStreamBuffer;
typedef NSCam::IMetadata                      IMetadata;
typedef NSCam::v3::Utils::HalMetaStreamBuffer HalMetaStreamBuffer;

#include <v1/IParamsManagerV3.h>
/******************************************************************************
 *
 ******************************************************************************/
#define REQUESTCONTROLLER_NAME       ("Cam@v1RequestThread")
#define REQUESTCONTROLLER_POLICY     (SCHED_OTHER)
#define REQUESTCONTROLLER_PRIORITY   (0)

/******************************************************************************
 *
 ******************************************************************************/
//
namespace NSCam {
namespace v1 {

namespace NSLegacyPipeline {


class RequestControllerImp
    : public virtual android::RefBase
    , public IRequestController
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
                                                RequestControllerImp(
                                                    char                 const *name,
                                                    android::sp< CamMsgCbInfo >   const &rpCamMsgCbInfo,
                                                    android::sp< IParamsManager > const &rpParamsManager
                                                );

    virtual                                     ~RequestControllerImp() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IRequestController Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual char const*                         getName() const;

public:  //// for adapter
    virtual MERROR                              autoFocus();

    virtual MERROR                              cancelAutoFocus();

    virtual MERROR                              precapture();

    virtual MERROR                              setParameters();

    virtual MERROR                              sendCommand(
                                                    int32_t cmd,
                                                    int32_t arg1, int32_t arg2);

public:  //// for feature
    virtual MERROR                              submitRequest(
                                                    Vector< IMetadata* >                settings,
                                                    android::sp<StreamBufferProvider>& resultBuffer
                                                ) const;

public:  //// pipeline
    virtual MERROR                              setRequestType( int type );

    virtual MERROR                              startPipeline(
                                                    MINT32                         aCameraId,
                                                    MINT32                         aStartRequestNumber,
                                                    MINT32                         aEndRequestNumber,
                                                    android::wp<ILegacyPipeline>   apPipeline,
                                                    android::wp<IRequestUpdater>   apRequestUpdater
                                                );

    virtual MERROR                              stopPipeline();

public:  //// control stream info
    virtual MERROR                              setControlAppStreamInfo(
                                                    android::sp<IMetaStreamInfo>    apControlAppStreamInfo
                                                );

    virtual android::sp<IMetaStreamInfo>        queryControlAppStreamInfo();

   virtual MERROR                               setControlHalStreamInfo(
                                                    android::sp<IMetaStreamInfo>    apControlHalStreamInfo
                                                );

    virtual android::sp<IMetaStreamInfo>        queryControlHalStreamInfo();

public:
    virtual MVOID                               dump();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  RefBase Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual MVOID                               onLastStrongRef( const void* /*id*/);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    android::sp< RequestSettingBuilder >        getRequestSettingBuilder();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    class RequestThread
        : public virtual android::RefBase
        , public Thread
    {
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //  Thread Interface.
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        public:
            // Ask this object's thread to exit. This function is asynchronous, when the
            // function returns the thread might still be running. Of course, this
            // function can be called from a different thread.
            virtual void        requestExit();

            // Good place to do one-time initializations
            virtual status_t    readyToRun();

        private:
            // Derived class must implement threadLoop(). The thread starts its life
            // here. There are two ways of using the Thread object:
            // 1) loop: if threadLoop() returns true, it will be called again if
            //          requestExit() wasn't called.
            // 2) once: if threadLoop() returns false, the thread will exit upon return.
            virtual bool        threadLoop();

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //  RefBase Interface.
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        public:
            virtual MVOID                           onLastStrongRef( const void* /*id*/);

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //  Implementations.
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        public:
                                                    RequestThread(
                                                        android::wp< RequestControllerImp > parent
                                                    );

            MERROR                                  start(
                                                        android::wp<ILegacyPipeline>&   apPipeline,
                                                        android::wp<IRequestUpdater>    apRequestUpdater,
                                                        MINT32               aStartRequestNumber,
                                                        MINT32               aEndRequestNumber
                                                    );

            MERROR                                  stop();

        protected:
            MERROR                                  waitForNextRequest(
                                                        android::sp<IMetaStreamBuffer>& apAppMetaStreamBuffer,
                                                        android::sp<HalMetaStreamBuffer>& apHalMetaStreamBuffer
                                                    );

            IMetaStreamBuffer*                      createMetaStreamBuffer(
                                                        const IMetadata&             pSetting,
                                                        android::sp<IMetaStreamInfo> pStreamInfo
                                                    );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //  Feature Implementations.
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        public:  //// for feature
            MERROR                                  submitRequest(
                                                        Vector< IMetadata* >       settings
                                                    ) const;

        public:  //// structure
            struct RequestItem : public android::RefBase
            {
                Vector< IMetadata* >                settings;
                android::sp<StreamBufferProvider>& resultBuffer;
            };

        protected:
            Vector< android::sp< RequestItem > >            mRequestQueue;

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //  Data Members.
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        protected:
            android::wp< RequestControllerImp >             mpRequestControllerImp;
            android::wp< ILegacyPipeline >                  mpPipeline;
            android::wp< IRequestUpdater >                  mpRequestUpdater;

        protected: ////                                     Logs.
            MINT32                                          mLogLevel;

        protected:
            bool                                            mRequestExit;
            MINT32                                          mRequestNumber;
            MINT32                                          mRequestNumberMin;
            MINT32                                          mRequestNumberMax;

        protected:
            bool                                            mStart;
            mutable Mutex                                   mLock;

    }; // class RequestThread

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    char const*                                     mName;
    MINT32                                          mCameraId;
    android::sp< CamMsgCbInfo >                     mpCamMsgCbInfo;
    android::sp< IParamsManager >                   mpParamsManager;
    android::sp< IParamsManagerV3 >                 mpParamsManagerV3;
    android::sp< RequestSettingBuilder >            mpRequestSettingBuilder;
    android::wp< ILegacyPipeline >                  mpPipeline;
    android::sp< StreamingProcessor >               mpStreamingProcessor;
    android::sp< RequestThread >                    mpRequestThread;

#warning "should remove"
protected:
    mutable Mutex                                   mStreamInfoLock;
    android::sp<IMetaStreamInfo>                    mpControlAppStreamInfo;
    android::sp<IMetaStreamInfo>                    mpControlHalStreamInfo;

};

/******************************************************************************
*
******************************************************************************/
};  //namespace NSPipelineContext
};  //namespace v1
};  //namespace NSCam
#endif  //_MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_REQUESTCONTROLLERIMP_H_

