/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#define LOG_TAG "MtkCam/CCSBHdl"

#include <Log.h>
#include <common.h>
using namespace NSCam;
//
#include <utils/Mutex.h>
#include <camera/MtkCamera.h>
#include <CamUtils.h>

using namespace android;
using namespace MtkCamUtils;
using namespace NSCam::Utils;
//using namespace NSCam::v3;
//using namespace NSCam::v3::Utils;

//
#include <cutils/properties.h>
//
#include <list>
#include <vector>
using namespace std;
//
#include <iopipe/Port.h>
//
#include <utils/include/ImageBufferHeap.h>
//
#include <v1/StreamIDs.h>
using namespace NSMtkStreamId;
#include <v3/utils/streambuf/IStreamBufferProvider.h>

#include <ImgBufProvidersManager.h>
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <v1/StreamBufferProviders/CamClientStreamBufHandler.h>
#include <metadata/client/mtk_metadata_tag.h>
//
//
#if defined(__func__)
#undef __func__
#endif
#define __func__ __FUNCTION__
//
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
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
/******************************************************************************
*
*******************************************************************************/
#define GET_IIMGBUF_IMG_STRIDE_IN_BYTE(pBuf, plane)         (Format::queryPlaneCount(Format::queryImageFormat(pBuf->getImgFormat().string())) >= (plane+1)) ? \
                                                            ((pBuf->getImgWidthStride(plane)*Format::queryPlaneBitsPerPixel(Format::queryImageFormat(pBuf->getImgFormat().string()),plane)))>>3 : 0
//#define GET_IIMAGEBUFFER_BUF_STRIDE_IN_BYTE(pBuf, plane)    (pBuf->getPlaneCount() >= (plane+1)) ? (pBuf->getBufStridesInBytes(plane)) : 0
//#define GET_IIMAGEBUFFER_BUF_SIZE(pBuf, plane)              (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufSizeInBytes(plane) : 0
//#define GET_IIMAGEBUFFER_BUF_VA(pBuf, plane)                (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufVA(plane) : 0
//#define GET_IIMAGEBUFFER_BUF_PA(pBuf, plane)                (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufPA(plane) : 0

#define GET_IIMAGEBUFFERHEAP_PLANE_BITS_PER_PIXCEL(pHaep, plane)    (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getPlaneBitsPerPixel(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_VA(pHaep, plane)                       (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufVA(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_SIZE(pHaep, plane)                     (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufSizeInBytes(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pHaep, plane)           (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufStridesInBytes(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_ID(pHaep, inxex)                       (pHaep->getHeapIDCount() >= (inxex+1)) ? (pHaep->getHeapID(inxex)) : 0




/*******************************************************************************
*
********************************************************************************/
namespace NSCamStreamBufProvider {

class CamClientStreamBufHandlerImpl : public CamClientStreamBufHandler
{
    public:
        CamClientStreamBufHandlerImpl(
            MUINT32                     openId,
            const char*                 userName,
            sp<ImgBufProvidersManager>  spImgBufProvidersMgr);
        ~CamClientStreamBufHandlerImpl();

    //DECLARE_ICAMBUFHANDLER_INTERFACE();
    MERROR                              dequeStreamBuffer(
                                            MUINT32 const iRequestNo,
                                            android::sp<IImageStreamInfo> const pStreamInfo,
                                            android::sp<HalImageStreamBuffer> &rpStreamBuffer);

    MERROR                              enqueStreamBuffer(
                                            android::sp<IImageStreamInfo> const pStreamInfo,
                                            android::sp<HalImageStreamBuffer> rpStreamBuffer,
                                            MUINT32  bBufStatus);
    //
    virtual MBOOL   init();
    virtual MBOOL   uninit();
    virtual MBOOL   setMetadata(
                        MUINT32 iReqNum,
                        IMetadata* pMetadata);
    virtual MBOOL   setForceRotation(
                        MBOOL   bIsForceRotation,
                        MUINT32 rotationAnagle);
    virtual MBOOL   mapPort(
                        EBufProvider    bufType,
                        MUINT32         streamId,
                        MUINT32         timeout = 0,
                        MBOOL           bPushFront = MFALSE);
    virtual MBOOL   unmapPort(EBufProvider bufType);

    //
    virtual MINT32  mapNode2Dst(MUINT streamID);
    //
    protected:
        //
        enum EOutPutPort
        {
            eOut_Port1,
            eOut_Port2,
            eOut_Port3,
            eOut_PortNum
        };
        //
        typedef struct
        {
            EBufProvider    bufType;
            MUINT32         streamId;
            MUINT32         timeout;
        }MAP_PORT_INFO;
        //
        typedef struct
        {
            MUINT iReqNum;
            android::sp<HalImageStreamBuffer> halImageStreamBuffer;
            ImgBufQueNode bufQueNode;
            // for buffer generated by self
            MBOOL isTemp;
            android::sp<IImageBufferHeap>    imageBuffer;
        }BUF_INFO;
        //
        typedef struct
        {
            MUINT iReqNum;
            android::sp<IImageStreamInfo> pStreamInfo;
            android::sp<HalImageStreamBuffer> rpStreamBuffer;
            MBOOL okToDelete;
        }BACK_UP_BUF_INFO;
        //
        typedef struct
        {
            MUINT iReqNum;
            MINT64 timeStamp;
        }TIME_INFO;
        //
        //
        MUINT32 getOpenId() {return mOpenId;}
        const char* getName() const {return msName;}

        //
    private:
        MUINT32                     mOpenId;
        mutable Mutex               mLock;
        mutable Mutex               mMetaLock;
        const char* const           msName;
        MBOOL                       mbEnableIImageBufferLog;
        MBOOL                       mbIsForceRotation;
        MUINT32                     mRotationAnagle;
        sp<ImgBufProvidersManager>  mspImgBufProvidersMgr;
        //vector<ImgBufQueNode>       mvBufQueNode[EPass2Out_DST_AMOUNT];
        vector<BUF_INFO>            mvBufQueNodeInfo[eOut_PortNum];
        vector<BACK_UP_BUF_INFO>    mvBufBKInfo[eOut_PortNum];
        list<MAP_PORT_INFO>         mlMapPort[eOut_PortNum];
        list<TIME_INFO>             mTimeStampInfo;
        MUINT32                     mLogLevel;
        MINT64                      mLastTimeStamp[eOut_PortNum];

    private:
        MBOOL setTimeStampInfo(MUINT32 reqNum, MINT64 timestamp);
        MBOOL getTimeStampInfo(MUINT32 reqNum, MINT64& timestamp);
}
;



/*******************************************************************************
*
********************************************************************************/
CamClientStreamBufHandler*
CamClientStreamBufHandler::
createInstance(
    MUINT32                     openId,
    const char*                 userName,
    sp<ImgBufProvidersManager>  spImgBufProvidersMgr)
{
    return new CamClientStreamBufHandlerImpl(openId, userName, spImgBufProvidersMgr);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
CamClientStreamBufHandler::
destroyInstance()
{
    //delete this;
}


/*******************************************************************************
*
********************************************************************************/
CamClientStreamBufHandler::
CamClientStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
CamClientStreamBufHandler::
~CamClientStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
CamClientStreamBufHandlerImpl::
CamClientStreamBufHandlerImpl(
    MUINT32                     openId,
    const char*                 userName,
    sp<ImgBufProvidersManager>  spImgBufProvidersMgr)
    : CamClientStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
    , mbEnableIImageBufferLog(MFALSE)
    , mbIsForceRotation(MFALSE)
    , mRotationAnagle(0)
    , mspImgBufProvidersMgr(spImgBufProvidersMgr)
    , mLogLevel(0)
{
    char cProperty[PROPERTY_VALUE_MAX] = {'\0'};
    ::property_get("debug.camera.streambufferprovider.loglevel", cProperty, "2");
    mLogLevel = ::atoi(cProperty);
    MY_LOGD("debug.camera.streambufferprovider.loglevel=%s", cProperty);
}


/*******************************************************************************
*
********************************************************************************/
CamClientStreamBufHandlerImpl::
~CamClientStreamBufHandlerImpl()
{
    FUNC_NAME;
    mspImgBufProvidersMgr = NULL;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
setTimeStampInfo(MUINT32 reqNum, MINT64 timestamp)
{
    Mutex::Autolock _l(mMetaLock);
    // 1. insert new ID and timestamp
    TIME_INFO tempTimeInfo;
    tempTimeInfo.iReqNum = reqNum;
    tempTimeInfo.timeStamp = timestamp;
    mTimeStampInfo.push_back(tempTimeInfo);
    // 2. if list size > 10, delete first one
    if(mTimeStampInfo.size() > 10 )
    {
        mTimeStampInfo.erase(mTimeStampInfo.begin());
    }
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
getTimeStampInfo(MUINT32 reqNum, MINT64& timestamp)
{
    Mutex::Autolock _l(mMetaLock);
    MBOOL isFound = MFALSE;
    timestamp = 0; // not found
    list<TIME_INFO>::iterator iterTimStamp;
    for(iterTimStamp = mTimeStampInfo.begin(); iterTimStamp != mTimeStampInfo.end(); iterTimStamp++)
    {
        if ((*iterTimStamp).iReqNum == reqNum)
        {
            timestamp = (*iterTimStamp).timeStamp;
            isFound = MTRUE;
            break;
        }
    }
    return isFound;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
init()
{
    FUNC_START;
    //
    for(MUINT32 i=0; i<eOut_PortNum; i++)
    {
        //mvBufQueNode[i].clear();
        mvBufQueNodeInfo[i].clear();
        mlMapPort[i].clear();
        mvBufBKInfo[i].clear();
        mLastTimeStamp[i] = 0;
    }
    mTimeStampInfo.clear();
    //
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get( "debug.enable.imagebuffer.log", value, "0");
    mbEnableIImageBufferLog = atoi(value);
    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
uninit()
{
    FUNC_START;
    //
    for(MUINT32 i=0; i<eOut_PortNum; i++)
    {
        //mvBufQueNode[i].clear();
        mvBufQueNodeInfo[i].clear();
        mvBufBKInfo[i].clear();
        mLastTimeStamp[i] = 0;
    }
    //
    mTimeStampInfo.clear();
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
setMetadata(MUINT32 iReqNum, IMetadata* pMetadata)
{
    //FUNC_START;
    MBOOL isTimeStampSet = MFALSE;
    //
    IMetadata::IEntry const entry = pMetadata->entryFor(MTK_SENSOR_TIMESTAMP);
    if ( ! entry.isEmpty() && entry.tag() == MTK_SENSOR_TIMESTAMP )
    {
        MINT64 const timestamp = entry.itemAt(0, Type2Type<MINT64>());
        setTimeStampInfo(iReqNum, timestamp);
        isTimeStampSet = MTRUE;
        MY_LOGD("ReqNum(%d),TS(%d.%06d)",
            iReqNum,
            (MINT32)((timestamp/1000)/1000000),
            (MINT32)((timestamp/1000)%1000000));

    }
    //
    //
    //FUNC_END;
    return isTimeStampSet;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
CamClientStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer)
{
    Mutex::Autolock _l(mLock);
    //
    MBOOL isDequeProvider = MFALSE, provideNull = MFALSE, provideTemp = MFALSE;
    MBOOL doCacheInvalid = MFALSE;
    StreamId_T data = pStreamInfo->getStreamId();
    MINT32 bufQueIdx = mapNode2Dst(data);
    ImgBufQueNode node;
    list<MAP_PORT_INFO>::iterator iterMapPort;
    //
    if(bufQueIdx == -1)
    {
        MY_LOGE("bufQueIdx = -1");
        return BAD_TYPE;
    }
    //
    //
    for(iterMapPort = mlMapPort[bufQueIdx].begin(); iterMapPort != mlMapPort[bufQueIdx].end(); iterMapPort++)
    {
        if(data == (*iterMapPort).streamId)
        {
            sp<IImgBufProvider> bufProvider = NULL;
            MUINT32 timeout = (*iterMapPort).timeout;

            switch((*iterMapPort).bufType)
            {
                case eBuf_Disp:
                {
                    //while(1)
                    {
                        bufProvider =  mspImgBufProvidersMgr->getDisplayPvdr();
                        if(bufProvider == 0)
                        {
                            if (timeout == 0)
                            {
                                provideNull = MTRUE;
                            }
                            else
                            {
                                provideTemp = MTRUE;
                            }
                        }
                    }
                    break;
                }
                case eBuf_Rec:
                {
                    bufProvider =  mspImgBufProvidersMgr->getRecCBPvdr();
                    if(bufProvider == 0)
                    {
                        if (timeout == 0)
                        {
                            provideNull = MTRUE;
                        }
                        else
                        {
                            provideTemp = MTRUE;
                        }
                    }
                    break;
                }
                case eBuf_AP:
                {
                    bufProvider =  mspImgBufProvidersMgr->getPrvCBPvdr();
                    doCacheInvalid = MTRUE;
                    if(bufProvider == 0)
                    {
                        if (timeout == 0)
                        {
                            provideNull = MTRUE;
                        }
                        else
                        {
                            provideTemp = MTRUE;
                        }
                    }
                    break;
                }
                case eBuf_Generic:
                {
                    bufProvider =  mspImgBufProvidersMgr->getGenericBufPvdr();
                    if(bufProvider == 0)
                    {
                        if (timeout == 0)
                        {
                            provideNull = MTRUE;
                        }
                        else
                        {
                            provideTemp = MTRUE;
                        }
                    }
                    break;
                }
                case eBuf_FD:
                {
                    bufProvider =  mspImgBufProvidersMgr->getFDBufPvdr();
                    if(bufProvider == 0)
                    {
                        if (timeout == 0)
                        {
                            provideNull = MTRUE;
                        }
                        else
                        {
                            provideTemp = MTRUE;
                        }
                    }
                    break;
                }
                case eBuf_OT:
                {
                    bufProvider =  mspImgBufProvidersMgr->getOTBufPvdr();
                    if(bufProvider == 0)
                    {
                        if (timeout == 0)
                        {
                            provideNull = MTRUE;
                        }
                        else
                        {
                            provideTemp = MTRUE;
                        };
                    }
                    break;
                }
                default:
                {
                    MY_LOGE("un-supported bufType(%d)",(*iterMapPort).bufType);
                    return BAD_TYPE;
                }
            }
            //
            if(bufProvider != 0)
            {
                MUINT32 timeout = (*iterMapPort).timeout;
                //
                while(1)
                {
                    if(bufProvider->dequeProvider(node))
                    {
                        node.setCookieDE((*iterMapPort).bufType);
                        isDequeProvider = MTRUE;
                        break;
                    }
                    else
                    {
                        if(timeout == 0)
                        {
                            if((*iterMapPort).timeout != 0)
                            {
                                provideTemp = MTRUE;
                                MY_LOGW("Wait bufType(%d) buffer timeout in %d ms",
                                        (*iterMapPort).bufType,
                                        (*iterMapPort).timeout);
                            }
                            else
                            {
                                MY_LOGW("Default Timeout value is 0, provide NULL buffer");
                                provideNull = MTRUE;
                            }
                            break;
                        }
                        else
                        {
                            MY_LOGW("Wait bufType(%d) buffer, timeout(%d/%d) count down",
                                    (*iterMapPort).bufType,
                                    timeout,
                                    (*iterMapPort).timeout);
                            timeout--;
                            usleep(10*1000);
                        }
                    }
                };
            }
            //
            if(isDequeProvider || provideTemp || provideNull)
            {
                break;
            }
        }
    }
    //
    if(isDequeProvider && !provideTemp && !provideNull )
    {
        BUF_INFO tempBuf;
        tempBuf.bufQueNode = node;
        size_t bufStridesInBytes[] = { GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 0),
                                        GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 1),
                                        GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 2)};
        size_t bufBoundaryInBytes[] = {0,0,0};
        IImageBufferAllocator::ImgParam imgParam = IImageBufferAllocator::ImgParam(
                                                        Format::queryImageFormat(node.getImgBuf()->getImgFormat().string()),
                                                        MSize(
                                                            node.getImgBuf()->getImgWidth(),
                                                            node.getImgBuf()->getImgHeight()),
                                                        bufStridesInBytes,
                                                        bufBoundaryInBytes,
                                                        Format::queryPlaneCount(Format::queryImageFormat(node.getImgBuf()->getImgFormat().string())));
        PortBufInfo_v1 portBufInfo = PortBufInfo_v1(
                                        node.getImgBuf()->getIonFd(),
                                        (MUINTPTR)node.getImgBuf()->getVirAddr(),
                                        0,
                                        node.getImgBuf()->getBufSecu(),
                                        node.getImgBuf()->getBufCohe());
        //
        sp<ImageBufferHeap> pHeap = ImageBufferHeap::create(
                                                        LOG_TAG,
                                                        imgParam,
                                                        portBufInfo,
                                                        mbEnableIImageBufferLog);
        if(pHeap == 0)
        {
            MY_LOGE("pHeap is NULL");
            return NO_MEMORY;
        }
        //
        /*
        IImageBuffer* tempBuffer = pHeap->createImageBuffer();
        tempBuffer->incStrong(tempBuffer);
        tempBuffer->lockBuf(
                        LOG_TAG,
                        eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_READ_OFTEN);

        if( doCacheInvalid )
        {
            if( !tempBuffer->syncCache(eCACHECTRL_INVALID) )
                MY_LOGE("invalid cache failed imgbuf 0x%x", tempBuffer);
        }
        */
        android::sp<HalImageStreamBufferProvider>
        pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, pHeap, this);
        rpStreamBuffer = pStreamBufferProvider;
        //
        if ( rpStreamBuffer == 0)
        {
            MY_LOGE("Set buffer as NULL by empty buffer container");
            return NO_MEMORY;
        }
        tempBuf.iReqNum = iRequestNo;
        tempBuf.halImageStreamBuffer = rpStreamBuffer;
        tempBuf.isTemp = MFALSE;
        tempBuf.imageBuffer = NULL;
        mvBufQueNodeInfo[bufQueIdx].push_back(tempBuf);
        //
        pHeap->lockBuf(LOG_TAG, eBUFFER_USAGE_SW_MASK);
        MSize imgSize = pHeap->getImgSize();
        MY_LOGD_IF((mLogLevel>=1),"Req(%d),StrBuf(%p),Heap(%p),VA(%p/%p/%p),BS(%d=%d+%d+%d),Id(%d/%d/%d),F(0x%08X),S(%dx%d),Str(%d,%d,%d)",
                iRequestNo,
                rpStreamBuffer.get(),
                pHeap.get(),
                GET_IIMAGEBUFFERHEAP_VA(pHeap,0),
                GET_IIMAGEBUFFERHEAP_VA(pHeap,1),
                GET_IIMAGEBUFFERHEAP_VA(pHeap,2),
                ( GET_IIMAGEBUFFERHEAP_SIZE(pHeap,0)+
                  GET_IIMAGEBUFFERHEAP_SIZE(pHeap,1)+
                  GET_IIMAGEBUFFERHEAP_SIZE(pHeap,2)
                ),
                GET_IIMAGEBUFFERHEAP_SIZE(pHeap,0),
                GET_IIMAGEBUFFERHEAP_SIZE(pHeap,1),
                GET_IIMAGEBUFFERHEAP_SIZE(pHeap,2),
                GET_IIMAGEBUFFERHEAP_ID(pHeap,0),
                GET_IIMAGEBUFFERHEAP_ID(pHeap,1),
                GET_IIMAGEBUFFERHEAP_ID(pHeap,2),
                pHeap->getImgFormat(),
                imgSize.w,
                imgSize.h,
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pHeap,0),
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pHeap,1),
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pHeap,2)
                );
        pHeap->unlockBuf(LOG_TAG);
        //
    }
    else if(provideTemp)
    {
        BUF_INFO tempBuf;
        //tempBuf.bufQueNode = 0;
        IImageStreamInfo::BufPlanes_t const& bufPlanes = pStreamInfo->getBufPlanes();
        size_t bufStridesInBytes[3] = {0};
        size_t bufBoundaryInBytes[3]= {0};
        for (size_t i = 0; i < bufPlanes.size(); i++)
        {
            bufStridesInBytes[i] = bufPlanes[i].rowStrideInBytes;
        }
        IImageBufferAllocator::ImgParam const imgParam(
            pStreamInfo->getImgFormat(),
            pStreamInfo->getImgSize(),
            bufStridesInBytes, bufBoundaryInBytes,
            bufPlanes.size()
        );

        //one-time allocation
        android::sp<IIonImageBufferHeap> pImageBufferHeap =
        IIonImageBufferHeap::create(
            pStreamInfo->getStreamName(),
            imgParam,
            IIonImageBufferHeap::AllocExtraParam(),
            MFALSE
        );
        //
        // 2. set ImageBufferHeap into StreamBuffer structure
        android::sp<HalImageStreamBufferProvider>
        pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, pImageBufferHeap, this);
        rpStreamBuffer = pStreamBufferProvider;
        if ( rpStreamBuffer == 0 || pImageBufferHeap == 0 )
        {
            MY_LOGE("buffer is NULL");
            return NO_MEMORY;
        }
        tempBuf.iReqNum = iRequestNo;
        tempBuf.halImageStreamBuffer = rpStreamBuffer;
        tempBuf.isTemp = MTRUE;
        tempBuf.imageBuffer = pImageBufferHeap;
        mvBufQueNodeInfo[bufQueIdx].push_back(tempBuf);

        pImageBufferHeap->lockBuf(LOG_TAG, eBUFFER_USAGE_SW_MASK);
        MSize imgSize = pImageBufferHeap->getImgSize();
        MY_LOGD("TMP:Req(%d),StrBuf(%p),Heap(%p),VA(%p/%p/%p),BS(%d=%d+%d+%d),Id(%d/%d/%d),F(0x%08X),S(%dx%d),Str(%d,%d,%d)",
                iRequestNo,
                rpStreamBuffer.get(),
                pImageBufferHeap.get(),
                GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,0),
                GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,1),
                GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,2),
                ( GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,0)+
                  GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,1)+
                  GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,2)
                ),
                GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,0),
                GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,1),
                GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,2),
                GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,0),
                GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,1),
                GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,2),
                pImageBufferHeap->getImgFormat(),
                imgSize.w,
                imgSize.h,
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,0),
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,1),
                GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,2)
                );
        pImageBufferHeap->unlockBuf(LOG_TAG);
    }
    else if(provideNull)
    {
        MY_LOGD("Set rpStreamBuffer as NULL");
        rpStreamBuffer = NULL;
    }
    else
    {
        MY_LOGE("Should not go to here");
        return BAD_VALUE;
    }
    //FUNC_END;
    return OK;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
CamClientStreamBufHandlerImpl::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)

{
    MUINT32 const BufHandleLost = 9999;
    //
    if ( rpStreamBuffer==0 )
    {
        MY_LOGE("Null buffer release to provider: %d", bBufStatus);
        return BAD_VALUE;
    }
    if ( pStreamInfo==NULL )
    {
        MY_LOGE("Null pStreamInfo");
        return BAD_VALUE;
    }
    //
    #if 1 //TBD
    StreamId_T data = pStreamInfo->getStreamId();
    #else
    StreamId_T data = STREAM_ID_CAM_CLIENT_DISP;
    #endif
    MINT32 i, bufQueIdx = mapNode2Dst(data);

    //FUNC_START;
    //
    // check lost buffer
    // if bBufStatus =! in check lost buffer status
    if (bBufStatus != BufHandleLost)
    {

        // if lost buffer[bufQueIdx] size is not 0
        //   check if the buffer can be return first
        //   by enqueStreamBuffer(, , bBufStatus = check lost buffer status)
        if (mvBufBKInfo[bufQueIdx].size() > 0)
        {
            MBOOL inserted = MFALSE;
            vector<BACK_UP_BUF_INFO>::iterator iterBufBKInfo = mvBufBKInfo[bufQueIdx].begin();
            MERROR retState;
            while(iterBufBKInfo != mvBufBKInfo[bufQueIdx].end())
            {
                android::sp<IImageStreamInfo> const pStreamInfoTmp = (*iterBufBKInfo).pStreamInfo;
                android::sp<HalImageStreamBuffer> rpStreamBufferTmp = (*iterBufBKInfo).rpStreamBuffer;
                MY_LOGD("Check return backup vector: req(%d),Buffer(%p)", (*iterBufBKInfo).iReqNum, rpStreamBufferTmp.get());
                retState = CamClientStreamBufHandlerImpl::enqueStreamBuffer(pStreamInfoTmp, rpStreamBufferTmp, BufHandleLost);
                if (retState == OK)
                {
                    MY_LOGD("  Found and Deleted");
                    iterBufBKInfo = mvBufBKInfo[bufQueIdx].erase(iterBufBKInfo);
                }
                else
                {
                    iterBufBKInfo++;
                }
            }
        }
    }

    Mutex::Autolock _l(mLock);

    ImgBufQueNode storedImgBufQueNode;
    MBOOL isTempBuffer = MFALSE;
    //
    #if 0
    MY_LOGD("data(0x%08X)",data);
    #endif
    //
    if(bufQueIdx == -1)
    {
        MY_LOGE("bufQueIdx = -1");
        return BAD_VALUE;
    }
    //
    if(bufQueIdx >= 0)
    {

        if(mvBufQueNodeInfo[bufQueIdx].size() > 0)
        {
            storedImgBufQueNode = mvBufQueNodeInfo[bufQueIdx][0].bufQueNode;
            isTempBuffer = mvBufQueNodeInfo[bufQueIdx][0].isTemp;
        }
        else
        {
            MY_LOGE("mvBufQueNodeInfo[%d] size(%d) = 0",
                    bufQueIdx,
                    mvBufQueNodeInfo[bufQueIdx].size());
            return BAD_VALUE;
        }
    }
    else
    {
        MY_LOGE("bufQueIdx(%d) < 0",bufQueIdx);
    }
    //

    android::sp<HalImageStreamBuffer> storedAddr = mvBufQueNodeInfo[bufQueIdx][0].halImageStreamBuffer;
    MUINT32 bufReqNum = mvBufQueNodeInfo[bufQueIdx][0].iReqNum;
    //
    if(storedAddr.get() == rpStreamBuffer.get() && !isTempBuffer)
    {

        MBOOL isAPClientFromFD = MFALSE;
        sp<IImgBufProvider> bufProvider = NULL;
        //IImageBuffer* tempBuffer = const_cast<IImageBuffer*>(pImageBuffer);
        //
        //switch(keepImgBufQueNode.getCookieDE())
        switch(storedImgBufQueNode.getCookieDE())
        {
            case eBuf_Disp:
            {
                bufProvider = mspImgBufProvidersMgr->getDisplayPvdr();
                break;
            }
            case eBuf_Rec:
            {
                bufProvider = mspImgBufProvidersMgr->getRecCBPvdr();
                break;
            }
            case eBuf_AP:
            {
                bufProvider = mspImgBufProvidersMgr->getPrvCBPvdr();
                const_cast<ImgBufQueNode*>(&storedImgBufQueNode)->setCookieDE(0); // 0 for preview
                break;
            }
            case eBuf_Generic:
            {
                bufProvider = mspImgBufProvidersMgr->getGenericBufPvdr();
                break;
            }
            case eBuf_FD:
            {
                bufProvider = mspImgBufProvidersMgr->getFDBufPvdr();
                isAPClientFromFD = MTRUE;
                break;
            }
            case eBuf_OT:
            {
                bufProvider = mspImgBufProvidersMgr->getOTBufPvdr();
                break;
            }
            default:
            {
                MY_LOGE("un-supported bufType(%d)",storedImgBufQueNode.getCookieDE());
                return BAD_TYPE;
            }
        }
        //
        if (bufProvider == NULL)
        {
            MY_LOGW("streamId(%d) is not available, drop it!", storedImgBufQueNode.getCookieDE());
            //mvBufQueNode[bufQueIdx].erase(mvBufQueNode[bufQueIdx].begin());
            mvBufQueNodeInfo[bufQueIdx].erase(mvBufQueNodeInfo[bufQueIdx].begin());
            return BAD_INDEX;
        }
        else
        {
            MBOOL isFoundTime = MFALSE;
            MINT64 bufferTimeStamp = 0;
            isFoundTime = getTimeStampInfo(bufReqNum, bufferTimeStamp);
            if(!isFoundTime)
            {
                MY_LOGW("Req(%d),timeStampNotFound", bufReqNum);
                bufferTimeStamp = mLastTimeStamp[bufQueIdx];
            }
            else
            {
                mLastTimeStamp[bufQueIdx] = bufferTimeStamp;
            }
            const_cast<ImgBufQueNode*>(&(storedImgBufQueNode))->setStatus(ImgBufQueNode::eSTATUS_DONE);
            const_cast<ImgBufQueNode*>(&(storedImgBufQueNode))->getImgBuf()->setTimestamp(bufferTimeStamp);  // time stamp
            //

            char debugMsg[150];
            MINT64 timeForPrint = bufferTimeStamp;
            sprintf(debugMsg, "Req(%d),StrBuf(%p),TS(%d.%06d),Port(%d),BS(%d)",
                                bufReqNum,
                                rpStreamBuffer.get(),
                                (MINT32)((timeForPrint/1000)/1000000),
                                (MINT32)((timeForPrint/1000)%1000000),
                                storedImgBufQueNode.getCookieDE(),
                                bBufStatus);

            bufProvider->enqueProvider(storedImgBufQueNode);
            if(isAPClientFromFD)
            {
                // If APClient exists, copy to it
                sp<IImgBufProvider> pBufProvider;
                pBufProvider = mspImgBufProvidersMgr->getAPClientBufPvdr();
                ImgBufQueNode APClientnode;
                if (pBufProvider != 0 && pBufProvider->dequeProvider(APClientnode))
                {
                    MY_LOGD("APClient size:%d, fdClient size:%d", APClientnode.getImgBuf()->getBufSize() ,  storedImgBufQueNode.getImgBuf()->getBufSize());
                    //if ( APClientnode.getImgBuf()->getBufSize() >= keepImgBufQueNode.getImgBuf()->getBufSize())
                    if (1)
                    {
                           MY_LOGD("APClient addr:0x%x, FDCLient addr:0x%x", APClientnode.getImgBuf()->getVirAddr(), storedImgBufQueNode.getImgBuf()->getVirAddr());
                           memcpy(APClientnode.getImgBuf()->getVirAddr(),
                           storedImgBufQueNode.getImgBuf()->getVirAddr(),
                           APClientnode.getImgBuf()->getBufSize());
                           //keepImgBufQueNode.getImgBuf()->getBufSize());
                           const_cast<ImgBufQueNode*>(&APClientnode)->setStatus(ImgBufQueNode::eSTATUS_DONE);
                    }
                    else
                    {
                        MY_LOGE("APClient buffer size < FD buffer size");
                        const_cast<ImgBufQueNode*>(&APClientnode)->setStatus(ImgBufQueNode::eSTATUS_CANCEL);
                    }
                    //
                    pBufProvider->enqueProvider(APClientnode);
                }
            }
            //
            //mvBufQueNode[bufQueIdx].erase(mvBufQueNode[bufQueIdx].begin());
            mvBufQueNodeInfo[bufQueIdx].erase(mvBufQueNodeInfo[bufQueIdx].begin());
            //
            #if 1
            MY_LOGD_IF((mLogLevel>=1),"%s",debugMsg);
            #endif
            //FUNC_END;
            return OK;
        }
    }
    else if(storedAddr.get() == rpStreamBuffer.get() && isTempBuffer)
    {
        // delete temp buffer
        char debugMsg[150];
        sprintf(debugMsg, "TMP:Req(%d),StrBuf(%p),BS(%d)",
                                bufReqNum,
                                rpStreamBuffer.get(),
                                bBufStatus);
        MY_LOGD("%s",debugMsg);
        mvBufQueNodeInfo[bufQueIdx].erase(mvBufQueNodeInfo[bufQueIdx].begin());
        return OK;
    }
    else
    {
        MBOOL idFound = MFALSE;
        MUINT bufReq = 0;
        // shot the enque is not by order
        MY_LOGW("Not Match: Addr(0x%p != 0x%p),Port(%d),BS(%d), skip it!",
                storedAddr.get(),
                rpStreamBuffer.get(),
                mvBufQueNodeInfo[bufQueIdx][0].bufQueNode.getCookieDE(),
                bBufStatus);
        if (bBufStatus != BufHandleLost)
        {
            // search the request ID of the buffer
            {
                vector<BUF_INFO>::iterator iterBufInfo;
                for(iterBufInfo = mvBufQueNodeInfo[bufQueIdx].begin(); iterBufInfo != mvBufQueNodeInfo[bufQueIdx].end(); iterBufInfo++)
                {
                    if ((*iterBufInfo).halImageStreamBuffer.get() == rpStreamBuffer.get())
                    {
                        bufReq = (*iterBufInfo).iReqNum;
                        idFound = MTRUE;
                        break;
                    }
                }
            }
            // if not exist, return bad value
            if (!idFound )
            {
                MY_LOGE("The enque buffer is not in the list");
                return BAD_VALUE;
            }
            else
            {
                MY_LOGD("insert to Backup vector bufQueIdx = %d, mvBufBKInfo size = %d", bufQueIdx, mvBufBKInfo[bufQueIdx].size());
                BACK_UP_BUF_INFO tempBufBKInfo;
                tempBufBKInfo.iReqNum = bufReq;
                tempBufBKInfo.pStreamInfo = pStreamInfo;
                tempBufBKInfo.rpStreamBuffer = rpStreamBuffer;
                tempBufBKInfo.okToDelete = MFALSE;
                // if exist and bBufStatus != check lost buffer status
                //   insert ID and pStreamInfo, rpStreamBuffer to lost BufQue[bufQueIdx]
                if (mvBufBKInfo[bufQueIdx].size() == 0)
                {
                    mvBufBKInfo[bufQueIdx].push_back(tempBufBKInfo);
                }
                else
                {
                    MBOOL inserted = MFALSE;
                    vector<BACK_UP_BUF_INFO>::iterator iterBufBKInfo;
                    for(iterBufBKInfo = mvBufBKInfo[bufQueIdx].begin(); iterBufBKInfo != mvBufBKInfo[bufQueIdx].end(); iterBufBKInfo++)
                    {
                        if(bufReq < (*iterBufBKInfo).iReqNum)
                        {
                            mvBufBKInfo[bufQueIdx].insert(iterBufBKInfo, tempBufBKInfo);
                            inserted = MTRUE;
                            break;
                        }
                    }
                    if (!inserted)
                    {
                        mvBufBKInfo[bufQueIdx].push_back(tempBufBKInfo);
                    }
                }
                return OK;
            }
            return BAD_VALUE;
        }
        else
        {
            return BAD_VALUE;
        }
    }
    //FUNC_END;
    //
    return UNKNOWN_ERROR;
}



/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
setForceRotation(
    MBOOL   bIsForceRotation,
    MUINT32 rotationAnagle)
{
    MY_LOGD("FR(%d,%d)",
            bIsForceRotation,
            rotationAnagle);
    mbIsForceRotation = bIsForceRotation;
    mRotationAnagle = rotationAnagle;
    //
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
mapPort(
    EBufProvider    bufType,
    MUINT32         streamId,
    MUINT32         timeout,
    MBOOL           bPushFront)
{
    Mutex::Autolock _l(mLock);
    //
    MINT32 bufQueIdx = mapNode2Dst(streamId);
    list<MAP_PORT_INFO>::iterator iterMapPort;
    //
    if(bufQueIdx == -1)
    {
        return MFALSE;
    }
    //
    MY_LOGD("bufType(%d),streamId(%d),timeout(%d),bPushFront(%d)",
            bufType,
            streamId,
            timeout,
            bPushFront);
    MAP_PORT_INFO mapInfo;
    mapInfo.bufType = bufType;
    mapInfo.streamId = streamId;
    mapInfo.timeout = timeout;
    if(bPushFront)
    {
        mlMapPort[bufQueIdx].push_front(mapInfo);
    }
    else
    {
        mlMapPort[bufQueIdx].push_back(mapInfo);
    }

    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
CamClientStreamBufHandlerImpl::
unmapPort(EBufProvider bufType)
{
    Mutex::Autolock _l(mLock);
    //
    MBOOL isFind= MFALSE;
    MUINT32 i;
    list<MAP_PORT_INFO>::iterator iterMapPort;
    //
    for(i=0; i<eOut_PortNum; i++)
    {
        for(iterMapPort = mlMapPort[i].begin(); iterMapPort != mlMapPort[i].end();)
        {
            if((*iterMapPort).bufType == bufType)
            {
                MY_LOGD("bufType(%d), streamId(%d)",
                        (*iterMapPort).bufType,
                        (*iterMapPort).streamId);
                iterMapPort = mlMapPort[i].erase(iterMapPort);
                isFind = MTRUE;
                break;
            }
            else
            {
                iterMapPort++;
            }
        }
    }
    //
    if(!isFind)
    {
        MY_LOGW("Can't find bufType(%d)",bufType);
    }
    //
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MINT32
CamClientStreamBufHandlerImpl::
mapNode2Dst(MUINT streamID)
{
    switch(streamID)
    {
        case STREAM_ID_PASS2_OUT1:
        {
            return eOut_Port1;
        }
        case STREAM_ID_PASS2_OUT2:
        {
            return eOut_Port2;
        }
        case STREAM_ID_PASS2_OUT3:
        {
            return eOut_Port2;
        }
        default:
        {
            MY_LOGE("un-supported streamID(%d)",streamID);
            return -1;
        }
    }
}



}; // namespace NSCamStreamBufProvider

