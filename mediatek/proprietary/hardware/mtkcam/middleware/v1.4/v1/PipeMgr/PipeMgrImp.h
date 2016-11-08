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
#ifndef MTK_PIPE_MGR_IMP_INC_H
#define MTK_PIPE_MGR_IMP_INC_H
//
/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)[%s] " fmt, getOpenId(), __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)
//
#define MY_LOGD1(...)               MY_LOGD_IF((mLogLevel>=1),__VA_ARGS__)
#define MY_LOGD2(...)               MY_LOGD_IF((mLogLevel>=2),__VA_ARGS__)
#define MY_LOGD3(...)               MY_LOGD_IF((mLogLevel>=3),__VA_ARGS__)
//
#define FUNC_START                  MY_LOGD1("+")
#define FUNC_END                    MY_LOGD1("-")
#define FUNC_NAME                   MY_LOGD1("")
//
/*******************************************************************************
*
*******************************************************************************/
namespace NSMtkPipeMgrImp
{

class PipeMgrImp : public PipeMgr
                     , public Thread
{
    public:
        typedef enum{
            EImageInfo_FullRaw = 0,
            EImageInfo_ResizedRaw,
            EImageInfo_BufferRaw,
            EImageInfo_Port1,
            EImageInfo_Port2,
            EImageInfo_Port3,
            EImageInfo_PictureYuv,
            EImageInfo_ThumbnailYuv,
            EImageInfo_BufPicYuv,
            EImageInfo_BufThumbYuv,
            EImageInfo_Jpg,

            EImageInfo_Amount
        }EImageInfoId;

        typedef enum{
            EMetaInfo_Control_App = 0,
            EMetaInfo_Control_Hal,
            EMetaInfo_P1_App,
            EMetaInfo_P1_Hal,
            EMetaInfo_P2_App,
            EMetaInfo_P2_Hal,
            EMetaInfo_Jpg_App,

            EMetaInfo_Amount
        }EMetaInfoId;

        typedef enum{
            EIn = 0x0,
            EOut = 0x100,
            EInOutMask = EIn | EOut,

            EP1InAppMeta = 0x0 | EIn,
            EP1InHalMeta = 0x1 | EIn,
            EP1OutAppMeta = 0x2 | EOut,
            EP1OutHalMeta = 0x3 | EOut,
            EP1OutImageResizer = 0x4 | EOut,
            EP1OutImageFull = 0x5 | EOut,

            EP2InAppMeta = 0x10 | EIn,
            EP2InHalMeta = 0x11 | EIn,
            EP2OutAppMeta = 0x12 | EOut,
            EP2OutHalMeta = 0x13 | EOut,
            EP2InFullRaw = 0x14 | EIn,
            EP2InResizedRaw = 0x15 | EIn,
            EP2OutImage = 0x16 | EOut,
            EP2OutFDImage = 0x17 | EOut,

            EJpgInAppMeta = 0x50 | EIn,
            EJpgInHalMeta = 0x51 | EIn,
            EJpgOutAppMeta = 0x52 | EOut,
            EJpgInPictureYuv = 0x53 | EIn,
            EJpgInThumbnailYuv = 0x54 | EIn,
            EJpgOutJpg = 0x55 | EOut
        }ENodePort;

        typedef struct{
            MINT32      eNode;
            ENodePort   ePort;
            MINT32      streamInfoId;
            StreamIDs   streamId;
            eStreamType streamType;
            MINT32      usage;
        }PipeStreamConfig;

        typedef enum{
            ELoopCmd_StartLoop,
            ELoopCmd_StartOne,
            ELoopCmd_Exit
        }ELoopCmd;

        #define CAM_CLIENT_TEST     (0)
        #define REQUEST_NUM_MIN     (0)
        #define REQUEST_NUM_MAX     (0xFFFFFFFF)

        #define ALIGN_UP_SIZE(in,align)     ((in+align-1) & ~(align-1))
        #define ALIGN_DOWN_SIZE(in,align)   (in & ~(align-1))
        #define RRZO_SCALE_DOWN_RATIO_MAX   (0.4)

                PipeMgrImp(MUINT32 openId, EPipeScen pipeScenario);
                ~PipeMgrImp();
        //PipeMgr functions
        static sp<PipeMgr>  createInstance(MUINT32 openId, EPipeScen pipeScenario);
        void                    destroyInstance();

        MBOOL   createPipe(PipeMgrParams* pPipeMgrParams);
        MBOOL   destroyPipe();

        MBOOL   startLoop();
        MBOOL   startOne(/*IMetadata* capBufShotMeta, HalImageStreamBuffer* capBufShotImageBuf*/);
        MBOOL   stop();

        MBOOL   setPipeFlow(/*EPipeFlow pipeFlow*/) { return false; }

        MBOOL  calRrzoSize(
                    MRect&  crop,
                    MSize&  size,
                    MUINT32 zoomRatio,
                    MSize   dstSize);

        EPipeScen   getPipeScenario() { return mPipeScenario; }
        EPipeFlow   getPipeFlow() { return mPipeFlow; }

        MUINT32     getRequestNumMin() { return REQUEST_NUM_MIN; }
        MUINT32     getRequestNumMax() { return REQUEST_NUM_MAX; }

        //Thread functions
        status_t    readyToRun();
        bool        threadLoop();

    protected:
        //member functions
        MUINT32 getOpenId() {return mOpenId;}

        sp<ImageStreamInfo> createImageStreamInfo(
                                    char const*         streamName,
                                    StreamId_T          streamId,
                                    MUINT32             streamType,
                                    size_t              maxBufNum,
                                    size_t              minInitBufNum,
                                    MUINT               usageForAllocator,
                                    MINT                imgFormat,
                                    MSize const&        imgSize,
                                    MUINT32             transform
                                );

        sp<ImageStreamInfo> createRawImageStreamInfo(
                                    char const*         streamName,
                                    StreamId_T          streamId,
                                    MUINT32             streamType,
                                    size_t              maxBufNum,
                                    size_t              minInitBufNum,
                                    MUINT               usageForAllocator,
                                    MINT                imgFormat,
                                    MSize const&        imgSize,
                                    size_t const        stride
                                );

        MVOID   setupPipelineContext();
        MVOID   dumpPipelineStreamInfo();
        MVOID   dumpPipelineStreamConfig();

        MVOID   setupP1Node(MINT32 nodeId, char* nodeName);
        MVOID   dumpPipelineP1Config();
        MVOID   setupP2Node(MINT32 nodeId, char* nodeName);
        MVOID   dumpPipelineP2Config();
        MVOID   setupJpgNode(MINT32 nodeId, char* nodeName);
        MVOID   dumpPipelineJpgConfig();

        MVOID   finishPipelineContext();
        IMetaStreamBuffer* createMetaStreamBuffer(
                                android::sp<IMetaStreamInfo> pStreamInfo,
                                IMetadata const& rSettings,
                                MBOOL const repeating);

        //by scenario functions
        virtual MVOID   prepareSensor();
        virtual MVOID   closeSensor();
        virtual MVOID   prepareConfiguration();
        virtual MVOID   setupStreamBufferProvider();
        virtual MVOID   updateStreamBufferProvider();
        virtual MVOID   setupPipelineStreamInfo();
        virtual MVOID   setupPipelineStreamConfig();
        virtual MVOID   setupPipelineStream();
        virtual MVOID   setupPipelineNode();
        virtual MVOID   setupPipelineFlow();
        virtual MVOID   finishStreamBufferProvider();
        virtual sp<IPipelineFrame>  getPipelineFrame();
        virtual MBOOL   sendMetadata(
                            MINT32              requestNumber,
                            StreamId_T const    streamId,
                            IMetadata*          pMetadata);
        virtual MVOID   getInitRrzoSize(
                            MINT&   width,
                            MINT&   height);

        //member variables
        MUINT32     mOpenId;
        EPipeScen   mPipeScenario;
        MUINT32     mLogLevel;
        MBOOL       mbDumpInfo;
        EPipeFlow   mPipeFlow;

        sp<PipelineContext> mContext;

        Condition           mLoopCond;
        Condition           mLoopCondStop;
        Mutex               mLoopLock;
        list<ELoopCmd>      mlLoopCmd;
        MUINT32             mRequestCnt;

        sp<IMetaStreamBuffer>   mpAppMetaControlSB;
        sp<HalMetaStreamBuffer> mpHalMetaControlSB;

        Vector<PipeStreamConfig>    mvPipeStreamConfig;

        sp<IMetaStreamInfo>*    mAllMetaStreamInfo;
        sp<IImageStreamInfo>*   mAllImageStreamInfo;

        PipeMgrParams           mPipeMgrParams;

        IHalSensor* mpSensorHalObj;
        P1Node::SensorParams    mSensorParam;
};

}
#endif

