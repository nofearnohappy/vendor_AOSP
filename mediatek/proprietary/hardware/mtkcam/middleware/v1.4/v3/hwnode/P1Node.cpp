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

#define LOG_TAG "MtkCam/P1NodeImp"
//
#include <Log.h>
#include "BaseNode.h"
#include "hwnode_utilities.h"
#include <v3/hwnode/P1Node.h>
#include <v3/stream/IStreamInfo.h>
#include <v3/stream/IStreamBuffer.h>
#include <v3/utils/streambuf/IStreamBufferPool.h>
//
#include <utils/RWLock.h>
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>

//
#include <IHal3A.h>
#include <iopipe/CamIO/IHalCamIO.h>
#include <iopipe/CamIO/INormalPipe.h>
#include <vector>
#include <iopipe/CamIO/PortMap.h>/*#include <iopipe_FrmB/CamIO/PortMap_FrmB.h>*/
#include <common/eis/eis_hal.h>

//
#include <mtk_platform_metadata_tag.h>
#include <metadata/client/mtk_metadata_tag.h>
//
#include "Profile.h"
#include <Trace.h>
#include <cutils/properties.h>
#include <utils/Atomic.h>
//
#include <imageio/ispio_utility.h> //(SUPPORT_SCALING_CROP)
#include <metadata/IMetadataProvider.h>
//
#include <imagebuf/IIonImageBufferHeap.h>
#include <imagebuf/IDummyImageBufferHeap.h>
//
#include <iopipe/PostProc/IFeatureStream.h> //(SUPPORT_HQC)
//
using namespace std;
using namespace android;
using namespace NSCam;
using namespace NSCam::v3;
//using namespace NSCam::Utils;
using namespace NSCam::Utils::Sync;
using namespace NSCam::NSIoPipe;
using namespace NSCam::NSIoPipe::NSCamIOPipe;
using namespace NSCam::NSIoPipe::NSPostProc;
using namespace NS3Av3;
using namespace NSImageio;
using namespace NSIspio;

/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)                  CAM_LOGV("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)                  CAM_LOGD("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)                  CAM_LOGI("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)                  CAM_LOGW("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)                  CAM_LOGE("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)                  CAM_LOGA("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)                  CAM_LOGF("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD_WITH_OPENID(fmt, arg...)      CAM_LOGD("[%s] (%d) " fmt, __FUNCTION__, getOpenId(), ##arg)

//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF_P1(cond, ...)    do { if ( (cond) ) { MY_LOGD_WITH_OPENID(__VA_ARGS__); } }while(0)

/******************************************************************************
 *
 ******************************************************************************/
#define SUPPORT_3A              (1)
#define SUPPORT_ISP             (1)
#define SUPPORT_PERFRAME_CTRL   (0)
#define SUPPORT_EIS             (0) // needed by 3DNR/*[EP_TEMP]*/

#define SUPPORT_HQC             (0) // High Quality Capture/*[EP_TEMP]*/

#define SUPPORT_SCALING_CROP    (1)
#define SUPPORT_SCALING_CROP_IMGO   (SUPPORT_SCALING_CROP && (0))
#define SUPPORT_SCALING_CROP_RRZO   (SUPPORT_SCALING_CROP && (1))

#define FORCE_EIS_ON                (SUPPORT_EIS && (0))
#define FORCE_3DNR_ON               (SUPPORT_EIS && (0))
#define DISABLE_BLOB_DUMMY_BUF      (0)

/******************************************************************************
 *
 ******************************************************************************/
#define FUNCTION_IN             MY_LOGD_IF(1<=mLogLevel, "+");
#define FUNCTION_OUT            MY_LOGD_IF(1<=mLogLevel, "-");
#define PUBLIC_API_IN           MY_LOGD_IF(1<=mLogLevel, "API +");
#define PUBLIC_API_OUT          MY_LOGD_IF(1<=mLogLevel, "API -");
#define MY_LOGD1(...)           MY_LOGD_IF(1<=mLogLevel, __VA_ARGS__)
#define MY_LOGD2(...)           MY_LOGD_IF(2<=mLogLevel, __VA_ARGS__)

#define FUNCTION_IN_P1          MY_LOGD_IF_P1(1<=mLogLevel, "+");
#define FUNCTION_OUT_P1         MY_LOGD_IF_P1(1<=mLogLevel, "-");

#define P1THREAD_POLICY         (SCHED_OTHER)
#define P1THREAD_PRIORITY       (ANDROID_PRIORITY_FOREGROUND-2)

#define P1SOFIDX_INIT_VAL       (0)
#define P1SOFIDX_LAST_VAL       (0xFF)
#define P1SOFIDX_NULL_VAL       (0xFFFFFFFF)

/******************************************************************************
 *
 ******************************************************************************/
namespace {
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#if 0
class AAAResult {
    protected:
        struct info{
            sp<IPipelineFrame> spFrame;
            IMetadata          resultVal;
            MUINT32            mFlag;
            info()
                : spFrame(0)
                , resultVal()
                , mFlag(0)
                {}
        };

    protected:
        enum KeyType {
            KeyType_StrobeState = 1,
            KeyType_Rest        = 2, //1,2,4,8,...
        };

    protected:
        mutable Mutex              mLock;
        KeyedVector<MUINT32, info> mData; //key: magicnum, val: info
        MUINT32                    mAllKey;

    public:

        AAAResult()
            : mLock()
            , mData()
            , mAllKey(KeyType_Rest)
            //, mAllKey(KeyType_StrobeState|KeyType_Rest)
            {}

        void add(MUINT32 magicNum, MUINT32 key, MUINT32 val)
        {
             Mutex::Autolock lock(mLock);
             if(key != MTK_FLASH_STATE) {
                 //unSupported
                 return;
             }

             IMetadata::IEntry entry(MTK_FLASH_STATE);
             entry.push_back(val, Type2Type< MUINT8 >()); //{MTK_FLASH_STATE, MUINT8}
             ssize_t i = mData.indexOfKey(magicNum);
             if(i < 0) {
                 info data;
                 data.resultVal.update(MTK_FLASH_STATE, entry);

    data.mFlag |= KeyType_StrobeState;
                 mData.add(magicNum, data);
             } else {
                 info& data = mData.editValueFor(magicNum);
                 data.resultVal.update(MTK_FLASH_STATE, entry);

    data.mFlag |= KeyType_StrobeState;
             }
        }

        void add(MUINT32 magicNum, sp<IPipelineFrame> pframe, IMetadata &rVal)
        {
             Mutex::Autolock lock(mLock);
             ssize_t i = mData.indexOfKey(magicNum);
             if(i < 0) {
                 info data;
                 data.spFrame = pframe;
                 data.resultVal = rVal;

data.mFlag |= KeyType_Rest;
                 mData.add(magicNum, data);
             } else {
                 info& data = mData.editValueFor(magicNum);
                 data.spFrame = pframe;
                 data.resultVal += rVal;
                 data.mFlag |= KeyType_Rest;
             }
        }

        const info& valueFor(const MUINT32& magicNum) const {
            return mData.valueFor(magicNum);
        }

        bool isCompleted(MUINT32 magicNum) {
            Mutex::Autolock lock(mLock);
            return (mData.valueFor(magicNum).mFlag & mAllKey) == mAllKey;
        }

        void removeItem(MUINT32 key) {
            Mutex::Autolock lock(mLock);
            mData.removeItem(key);
        }

        void clear() {
            debug();
            Mutex::Autolock lock(mLock);
            mData.clear();
        }

        void debug() {
            Mutex::Autolock lock(mLock);
            for(size_t i = 0; i < mData.size(); i++) {
                MY_LOGW_IF((mData.valueAt(i).mFlag & KeyType_StrobeState) == 0,
                           "No strobe result: (%d)", mData.keyAt(i));
                MY_LOGW_IF((mData.valueAt(i).mFlag & KeyType_Rest) == 0,
                           "No rest result: (%d)", mData.keyAt(i));
            }
        }
};
#endif
#if 0
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class Storage {

    protected:
        typedef DefaultKeyedVector<MINTPTR, sp<IImageBuffer> >  MapType;
        MapType                    mvStorageQ;
        mutable Mutex              mStorageLock;
        MINT32                     mLogEnable;
    public:
                                   Storage()
                                       : mvStorageQ()
                                       , mStorageLock()
                                       , mLogEnable(0)
                                       {}

        virtual                   ~Storage(){};

        void                       init(MINT32 logEnable)
                                   {
                                       mvStorageQ.clear();
                                       mLogEnable = logEnable;
                                   }

        void                       uninit()
                                   {
                                       mvStorageQ.clear();
                                   }

        void                       enque(sp<IImageStreamBuffer> const& key, sp<IImageBuffer> &value) {
                                       Mutex::Autolock lock(mStorageLock);
                                       MY_LOGD_IF(mLogEnable, "Storage-enque::(key)0x%x/(val)0x%x",
                                           key.get(), value.get());
                                       MY_LOGD_IF(mLogEnable, "Info::(val-pa)0x%x/%d/%d/%d/%d/%d",
                                        value->getBufPA(0),value->getImgSize().w, value->getImgSize().h,
                                        value->getBufStridesInBytes(0), value->getBufSizeInBytes(0), value->getPlaneCount());

                                       mvStorageQ.add(reinterpret_cast<MINTPTR>(key.get()), value);
                                   };


        sp<IImageBuffer>           deque(MINTPTR key) {
                                       Mutex::Autolock lock(mStorageLock);
                                       sp<IImageBuffer> pframe = mvStorageQ.valueFor(key);
                                       if (pframe != NULL)
                                       {
                                           mvStorageQ.removeItem(key); //should un-mark
                                           MY_LOGD_IF(mLogEnable, "Storage-deque::(key)0x%x/(val)0x%x",
                                            key, pframe.get());
                                           MY_LOGD_IF(mLogEnable, "(val-pa)0x%x",
                                            pframe->getBufPA(0));
                                           return pframe;
                                       }
                                       return NULL;
                                   }
        sp<IImageBuffer>           query(MINTPTR key) {
                                       Mutex::Autolock lock(mStorageLock);
                                       sp<IImageBuffer> pframe = mvStorageQ.valueFor(key);
                                       if (pframe != NULL)
                                       {
                                           MY_LOGD_IF(mLogEnable, "Storage-deque::(key)0x%x/(val)0x%x",
                                            key, pframe.get());
                                           MY_LOGD_IF(mLogEnable, "Info::(val-pa)0x%x",
                                            pframe->getBufPA(0));
                                           return pframe;
                                       }
                                       return NULL;
                                   }
};
#endif

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

//#if (SUPPORT_SCALING_CROP)
#ifdef MAX
#undef MAX
#endif
#ifdef MIN
#undef MIN
#endif
#define MAX(a,b) ((a) > (b) ? (a) : (b))
#define MIN(a,b) ((a) < (b) ? (a) : (b))

#ifdef ALIGN_UPPER
#undef ALIGN_UPPER
#endif
#ifdef ALIGN_LOWER
#undef ALIGN_LOWER
#endif
#define ALIGN_UPPER(x,a)    (((x)+((typeof(x))(a)-1))&~((typeof(x))(a)-1))
#define ALIGN_LOWER(x,a)    (((x))&~((typeof(x))(a)-1))


#ifdef CHECK_LAST_FRAME_SKIPPED
#undef CHECK_LAST_FRAME_SKIPPED
#endif
#define CHECK_LAST_FRAME_SKIPPED(LAST_SOF_IDX, THIS_SOF_IDX)\
    ((LAST_SOF_IDX == P1SOFIDX_NULL_VAL) ?\
        (true) :\
        ((LAST_SOF_IDX == P1SOFIDX_LAST_VAL) ?\
            ((THIS_SOF_IDX != 0) ? (true) : (false)) :\
            ((THIS_SOF_IDX != (LAST_SOF_IDX + 1)) ? (true) : (false))));

#ifdef RESIZE_RATIO_MAX_10X
#undef RESIZE_RATIO_MAX_10X
#endif
#define RESIZE_RATIO_MAX_10X    (4)

#ifdef P1_STUFF_BUF_HEIGHT
#undef P1_STUFF_BUF_HEIGHT
#endif
#define P1_STUFF_BUF_HEIGHT     (1)

#ifdef REQ_SET
#undef REQ_SET
#endif
#define REQ_SET(bit)        ((MUINT32)(0x1 << bit))
#ifdef REQ_SET_NONE
#undef REQ_SET_NONE
#endif
#define REQ_SET_NONE        (0x0)
#ifdef IS_OUT
#undef IS_OUT
#endif
#define IS_OUT(out, set)    ((set & REQ_SET(out)) == REQ_SET(out))
enum REQ_OUT
{
    REQ_OUT_RESIZER         = 0,
    REQ_OUT_RESIZER_STUFF,
    REQ_OUT_FULL,
    REQ_OUT_FULL_PURE,
    REQ_OUT_FULL_COMBO,
    REQ_OUT_FULL_STUFF,
    REQ_OUT_MAX
};
enum REQ_TYPE
{
    REQ_TYPE_UNKNOWN        = 0,
    REQ_TYPE_NORMAL,
    REQ_TYPE_INITIAL,
    //REQ_TYPE_PADDING,
    //REQ_TYPE_DUMMY,
    REQ_TYPE_REDO
};
//
#ifdef IS_PORT
#undef IS_PORT
#endif
#define IS_PORT(port, set)  ((set & port) == port)
enum CONFIG_PORT
{
    CONFIG_PORT_NONE        = (0x0),
    CONFIG_PORT_RRZO        = (0x1 << 0),
    CONFIG_PORT_IMGO        = (0x1 << 1),
    CONFIG_PORT_EISO        = (0x1 << 2),
    CONFIG_PORT_LCSO        = (0x1 << 3),
    CONFIG_PORT_ALL         = (0xFFFFFFFF) // MUINT32
};

#ifdef BIN_RESIZE
#undef BIN_RESIZE
#endif
#define BIN_RESIZE(x)  (x = (x >> 1))

#ifdef BIN_REVERT
#undef BIN_REVERT
#endif
#define BIN_REVERT(x)  (x = (x << 1))

/******************************************************************************
 *
 ******************************************************************************/
inline MBOOL
isEISOn(
    IMetadata* const inApp
)
{
    if (inApp == NULL) {
        return false;
    }
    MUINT8 eisMode = MTK_CONTROL_VIDEO_STABILIZATION_MODE_OFF;
    if(!tryGetMetadata<MUINT8>(inApp,
        MTK_CONTROL_VIDEO_STABILIZATION_MODE, eisMode)) {
        MY_LOGW_IF(1, "no MTK_CONTROL_VIDEO_STABILIZATION_MODE");
    }
#if FORCE_EIS_ON
    eisMode = MTK_CONTROL_VIDEO_STABILIZATION_MODE_ON;
#endif
    return eisMode == MTK_CONTROL_VIDEO_STABILIZATION_MODE_ON;
}

/******************************************************************************
 *
 ******************************************************************************/
inline MBOOL
is3DNROn(
    IMetadata* const inApp
)
{
    if (inApp == NULL) {
        return false;
    }
    MINT32 e3DnrMode = MTK_NR_FEATURE_3DNR_MODE_OFF;
    if(!tryGetMetadata<MINT32>(inApp,
        MTK_NR_FEATURE_3DNR_MODE, e3DnrMode)) {
        MY_LOGW_IF(1, "no MTK_NR_FEATURE_3DNR_MODE");
    }
#if FORCE_3DNR_ON
    e3DnrMode = MTK_NR_FEATURE_3DNR_MODE_ON;
#endif
    return e3DnrMode == MTK_NR_FEATURE_3DNR_MODE_ON;
}

#if SUPPORT_HQC
/******************************************************************************
 *
 ******************************************************************************/
inline MBOOL
isHighQualityCaptureOn(
    IMetadata* const inApp,
    IHal3A* p3A
)
{
    if (inApp == NULL) {
        return false;
    }
    MUINT8 mode = 0;
    // HQC on by (still-capure) and (flash-trigger)
    if (!tryGetMetadata<MUINT8>(inApp,
        MTK_CONTROL_CAPTURE_INTENT, mode) ||
        mode != MTK_CONTROL_CAPTURE_INTENT_STILL_CAPTURE) {
        return false;
    }
    MY_LOGD("HighQualityCapture mode(%d)", mode);
    if ((p3A == NULL) || (p3A->checkCapFlash() == false)) {
        return false;
    }
    MY_LOGD("HighQualityCapture On");
    return true;
}
#endif

#if 1 /*[EP_TEMP]*/ /*[temp-test]*/ // [FIXME] sync correct tag

#ifdef P1_REQ_CROP_TAG
#undef P1_REQ_CROP_TAG
#endif
#define P1_REQ_CROP_TAG (MTK_P1NODE_SCALAR_CROP_REGION) // [FIXME] sync correct tag

#endif

/******************************************************************************
 *
 ******************************************************************************/
MBOOL calculateCropInfoFull(
    MUINT32 pixelMode,
    MSize const& sensorSize,
    MSize const& bufferSize,
    MRect const& querySrcRect,
    MRect& resultSrcRect,
    MSize& resultDstSize,
    MINT32 mLogLevel = 0
)
{
    if ((querySrcRect.size().w == sensorSize.w) &&
        (querySrcRect.size().h == sensorSize.h)) {
        return false;
    }
    if ((querySrcRect.size().w > bufferSize.w || // cannot over buffer size
        querySrcRect.size().h > bufferSize.h) ||
        (((querySrcRect.leftTop().x + querySrcRect.size().w) > sensorSize.w) ||
        ((querySrcRect.leftTop().y + querySrcRect.size().h) > sensorSize.h))
        ) {
        MY_LOGD_IF((1 <= mLogLevel), "calculateCropInfoFull input invalid "
            "pixelMode(%d) sensorSize(%dx%d) bufferSize(%dx%d) "
            "querySrcRect_size(%dx%d) querySrcRect_start(%d,%d)", pixelMode,
            sensorSize.w, sensorSize.h, bufferSize.w, bufferSize.h,
            querySrcRect.size().w, querySrcRect.size().h,
            querySrcRect.leftTop().x, querySrcRect.leftTop().y);
        return false;
    }
    // TODO: query the valid value, currently do not crop in IMGO
    resultDstSize = MSize(sensorSize.w, sensorSize.h);
    resultSrcRect = MRect(MPoint(0, 0), resultDstSize);

    return true;
}

/******************************************************************************
 *
 ******************************************************************************/
MBOOL calculateCropInfoResizer(
    MUINT32 pixelMode,
    MUINT32 imageFormat,
    MSize const& sensorSize,
    MSize const& bufferSize,
    MRect const& querySrcRect,
    MRect& resultSrcRect,
    MSize& resultDstSize,
    MINT32 mLogLevel = 0
)
{
    if ((querySrcRect.size().w == sensorSize.w) &&
        (querySrcRect.size().h == sensorSize.h)) {
        return false;
    }
    if ((((querySrcRect.leftTop().x + querySrcRect.size().w) > sensorSize.w) ||
        ((querySrcRect.leftTop().y + querySrcRect.size().h) > sensorSize.h))
        ) {
        MY_LOGD_IF((1 <= mLogLevel), "calculateCropInfoResizer input invalid "
            "pixelMode(%d) sensorSize(%dx%d) bufferSize(%dx%d) "
            "querySrcRect_size(%dx%d) querySrcRect_start(%d,%d)", pixelMode,
            sensorSize.w, sensorSize.h, bufferSize.w, bufferSize.h,
            querySrcRect.size().w, querySrcRect.size().h,
            querySrcRect.leftTop().x, querySrcRect.leftTop().y);
        return false;
    }
    //
    MPoint::value_type src_crop_x = querySrcRect.leftTop().x;
    MPoint::value_type src_crop_y = querySrcRect.leftTop().y;
    MSize::value_type src_crop_w = querySrcRect.size().w;
    MSize::value_type src_crop_h = querySrcRect.size().h;
    MSize::value_type dst_size_w = 0;
    MSize::value_type dst_size_h = 0;
    if (querySrcRect.size().w < bufferSize.w) {
        dst_size_w = querySrcRect.size().w;
        // check start.x
        {
            NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo info;
            NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
                NSCam::NSIoPipe::PORT_RRZO.index,
                NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_CROP_START_X,
                (EImageFormat)imageFormat,
                src_crop_x, info);
            src_crop_x = info.crop_x;
        }
        // check size.w
        {
            NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo info;
            NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
                NSCam::NSIoPipe::PORT_RRZO.index,
                NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_X_PIX|
                NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_STRIDE_BYTE,
                (EImageFormat)imageFormat,
                dst_size_w, info);
            dst_size_w = info.x_pix;
        }
        //
        dst_size_w = MIN(dst_size_w, sensorSize.w);
        src_crop_w = dst_size_w;
        if (src_crop_w > querySrcRect.size().w) {
            if ((src_crop_x + src_crop_w) > sensorSize.w) {
                src_crop_x = sensorSize.w - src_crop_w;
            }
        }
    } else {
        if ((src_crop_w * RESIZE_RATIO_MAX_10X) > (bufferSize.w * 10)) {
            MY_LOGW("calculateCropInfoResizer resize width invalid "
                    "(%d):(%d)", src_crop_w, bufferSize.w);
            return false;
        }
        dst_size_w = bufferSize.w;
    }
    if (querySrcRect.size().h < bufferSize.h) {
        dst_size_h = querySrcRect.size().h;
        dst_size_h = MIN(ALIGN_UPPER(dst_size_h, 2), sensorSize.h);
        src_crop_h = dst_size_h;
        if (src_crop_h > querySrcRect.size().h) {
            if ((src_crop_y + src_crop_h) > sensorSize.h) {
                src_crop_y = sensorSize.h - src_crop_h;
            }
        }
    } else {
        if ((src_crop_h * RESIZE_RATIO_MAX_10X) > (bufferSize.h * 10)) {
            MY_LOGW("calculateCropInfoResizer resize height invalid "
                    "(%d):(%d)", src_crop_h, bufferSize.h);
            return false;
        }
        dst_size_h = bufferSize.h;
    }
    resultDstSize = MSize(dst_size_w, dst_size_h);
    resultSrcRect = MRect(MPoint(src_crop_x, src_crop_y),
                            MSize(src_crop_w, src_crop_h));
    return true;
}
//#endif

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  .
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class P1NodeImp
    : public BaseNode
    , public P1Node
    , public IHal3ACb
    , protected Thread
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////
    struct QueNode_T {
        MUINT32                             magicNum;
        MUINT32                             sofIdx;
        sp<IPipelineFrame>                  appFrame;
        sp<IImageStreamBuffer>              halFrame_full;
        sp<IImageStreamBuffer>              halFrame_resizer;
        sp<IImageBuffer>                    img_full;
        sp<IImageBuffer>                    img_resizer;
        //sp<IImageBuffer>                    img_combo;
        MUINT32                             reqOutSet;  /*REQ_SET(REQ_OUT)*/
        MUINT32                             reqType;    /*REQ_TYPE*/
        #if (SUPPORT_SCALING_CROP)
        MSize                               dstSize_full;
        MSize                               dstSize_resizer;
        MRect                               cropRect_full;
        MRect                               cropRect_resizer;
        #endif
        QueNode_T()
            : magicNum(0)
            , sofIdx(P1SOFIDX_INIT_VAL)
            , appFrame(NULL)
            , halFrame_full(NULL)
            , halFrame_resizer(NULL)
            , img_full(NULL)
            , img_resizer(NULL)
            //, img_combo(NULL)
            , reqOutSet(REQ_SET_NONE)
            , reqType(REQ_TYPE_UNKNOWN)
        {}
    };

    typedef Vector<QueNode_T> Que_T;

protected:  ////                    Data Members. (Config)
    mutable RWLock                  mConfigRWLock;
    mutable Mutex                   mInitLock;
    MBOOL                           mInit;

    SortedVector<StreamId_T>        mInStreamIds;
    sp<IMetaStreamInfo>             mInAppMeta;
    sp<IMetaStreamInfo>             mOutAppMeta;
    sp<IMetaStreamInfo>             mInHalMeta;
    sp<IMetaStreamInfo>             mOutHalMeta;
    ImageStreamInfoSetT             mvOutImage_full;
    sp<IImageStreamInfo>            mOutImage_resizer;
    sp<IImageStreamInfo>            mInImage_combo;
    sp<IImageStreamInfo>            mOutImage_combo;
    SensorParams                    mSensorParams;
    //RawParams                       mRawParams;
    sp<IImageStreamBufferPoolT>     mpStreamPool_full;
    sp<IImageStreamBufferPoolT>     mpStreamPool_resizer;

    //Storage                         mImageStorage;

protected:  ////                    Data Members. (System capability)
    static const int                mNumInMeta = 2;
    static const int                mNumOutMeta = 3;
    int                             m3AProcessedDepth;
    int                             mNumHardwareBuffer;
    int                             mDelayframe;

protected:  ////
    MUINT32                         mlastNum;
    mutable Mutex                   mlastNumLock;
    MUINT32                         mLastSofIdx;

protected:  ////                    Data Members. (Hardware)
    mutable Mutex                   mHardwareLock;
    mutable Mutex                   mActiveLock;
    MBOOL                           mActive;
    mutable Mutex                   mReadyLock;
    MBOOL                           mReady;
    INormalPipe*                    mpCamIO;
    IHal3A_T*                       mp3A;
    #if SUPPORT_EIS
    EisHal*                       mpEIS;
    #endif
    #if SUPPORT_SCALING_CROP
    MRect                           mActiveArray;
    MUINT32                         mPixelMode;
    #endif
    MUINT32                         mConfigPort;
    MBOOL                           mIsBinEn;
    //
    DefaultKeyedVector< sp<IImageBuffer>, android::String8 >
                                    mvStuffBufferInfo;
    mutable Mutex                   mStuffBufferLock;

protected:  ////                    Data Members. (Queue: Request)
    mutable Mutex                   mRequestQueueLock;
    Que_T                           mRequestQueue;

protected:  ////                    Data Members. (Queue: 3Alist)
    mutable Mutex                   mControls3AListLock;
    List<MetaSet_T>                 mControls3AList;
    Condition                       mControls3AListCond;

protected:  ////
    //AAAResult                       m3AStorage;

protected:  ////                    Data Members. (Queue: Processing)
    mutable Mutex                   mProcessingQueueLock;
    Condition                       mProcessingQueueCond;
    Que_T                           mProcessingQueue;

protected:  ////                    Data Members. (Queue: drop)
    mutable Mutex                   mDropQueueLock;
    Vector<MUINT>                   mDropQueue;

protected:  ////                    Data Members.
    mutable Mutex                   mThreadLock;
    Condition                       mThreadCond;

protected:  ////                    Data Members.
    DurationProfile                 mDequeThreadProfile;

protected:  ////                    Data Members.
    mutable Mutex                   mPublicLock;

protected:  ////                    Data Members.
    MINT32                          mInFlightRequestCnt;

protected:
    MINT32                          mLogLevel;

protected:  ////                    Operations.

    MVOID                           setActive(
                                        MBOOL active
                                    );

    MBOOL                           getActive(
                                        void
                                    );

    MVOID                           setReady(
                                        MBOOL ready
                                    );

    MBOOL                           getReady(
                                        void
                                    );

    MVOID                           setInit(
                                        MBOOL init
                                    );

    MBOOL                           getInit(
                                        void
                                    );

    MVOID                           onProcessFrame(
                                        MBOOL initial,
                                        MUINT32 magicNum = 0,
                                        MUINT32 sofIdx = P1SOFIDX_INIT_VAL
                                    );

#if 0
    MVOID                           onProcess3AResult(
                                        MUINT32 magicNum,
                                        MUINT32 key,
                                        MUINT32 val
                                    );
#endif

    MVOID                           onProcessEIS(
                                        QueNode_T const &pFrame,
                                        IMetadata &resultEIS,
                                        QBufInfo const &deqBuf
                                    );

    MVOID                           onProcessEnqueFrame(
                                        QueNode_T &pFrame
                                    );

    MERROR                          onProcessDequedFrame(
                                    );

    MERROR                          onProcessDropFrame(
                                    );

    MBOOL                           getProcessingFrame_ByAddr(
                                        IImageBuffer* const imgBuffer,
                                        MUINT32 magicNum,
                                        QueNode_T &pFrame
                                    );

    QueNode_T                       getProcessingFrame_ByNumber(
                                        MUINT32 magicNum
                                    );


    MVOID                           onHandleFlush(
                                        MBOOL wait
                                    );

    MVOID                           onReturnProcessingFrame(
                                        QueNode_T const& pFrame,
                                        QBufInfo const &deqBuf,
                                        MetaSet_T const &result3A,
                                        IMetadata const &resultEIS
                                    );

    MVOID                           onFlushProcessingFrame(
                                        QueNode_T const& pFrame
                                    );

    MVOID                           onFlushRequestFrame(
                                        QueNode_T const& pFrame
                                    );

    MVOID                           createNode(sp<IPipelineFrame> appframe,
                                               Que_T *Queue,
                                               Mutex *QueLock,
                                               List<MetaSet_T> *list,
                                               Mutex *listLock
                                    );

    MVOID                           createNode(List<MetaSet_T> *list,
                                               Mutex *listLock
                                    );

    MVOID                           createNode(Que_T &Queue);

protected:  ////                    Hardware Operations.
    MERROR                          hardwareOps_start(
                                    );

    MERROR                          hardwareOps_enque(
                                        QueNode_T &node
                                    );

    MERROR                          hardwareOps_deque(
                                        QBufInfo &deqBuf,
                                        MetaSet_T &result3A
                                    );

    MERROR                          hardwareOps_stop(
                                    );

#if SUPPORT_HQC
    MERROR                          hardwareOps_suspend(
                                    );

    MERROR                          hardwareOps_resume(
                                    );
#endif

    MERROR                          setupNodeBufInfo(
                                        QueNode_T &node,
                                        QBufInfo &info
                                    );

    MERROR                          createStuffBuffer(
                                        sp<IImageBuffer> & imageBuffer,
                                        sp<IImageStreamInfo> const& streamInfo,
                                        NSCam::MSize::value_type const
                                            changeHeight = 0
                                    );

    MERROR                          createStuffBuffer(
                                        sp<IImageBuffer> & imageBuffer,
                                        char const * szName,
                                        MINT32 format,
                                        MSize size,
                                        MUINT32 stride
                                    );

    MERROR                          destroyStuffBuffer(
                                        sp<IImageBuffer> & imageBuffer
                                    );

    MVOID                           generateAppMeta(
                                        sp<IPipelineFrame> const &request,
                                        MetaSet_T const &result3A,
                                        QBufInfo const &deqBuf,
                                        IMetadata &appMetadata
                                    );

    MVOID                           generateHalMeta(
                                        MetaSet_T const &result3A,
                                        QBufInfo const &deqBuf,
                                        IMetadata const &resultEIS,
                                        IMetadata const &inHalMetadata,
                                        IMetadata &halMetadata
                                    );

    #if (SUPPORT_SCALING_CROP)
    MVOID                           prepareCropInfo(
                                       IMetadata const* pAppMeta,
                                       IMetadata const* pHalMeta,
                                       QueNode_T& node
                                   );

    MVOID                           prepareCropInfo(
                                       IMetadata* pMetadata,
                                       QueNode_T& node
                                   );
    #endif

    MERROR                          check_config(
                                        ConfigParams const& rParams
                                    );

protected:  ///

    MERROR                          lockMetadata(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        IMetadata &rMetadata
                                    );

    MERROR                          returnLockedMetadata(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        MBOOL success = MTRUE
                                    );

    MERROR                          returnUnlockedMetadata(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId
                                    );

    MERROR                          lock_and_returnMetadata(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        IMetadata &metadata
                                    );


    MERROR                          lockImageBuffer(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        sp<IImageStreamBuffer>  &pOutpImageStreamBuffer,
                                        sp<IImageBuffer> &rImageBuffer
                                    );

    MERROR                          returnLockedImageBuffer(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        sp<IImageBuffer> const& pImageBuffer,
                                        MBOOL success = MTRUE
                                    );

    MERROR                          returnUnlockedImageBuffer(
                                        sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId
                                    );

    MERROR                          lockImageBuffer(
                                        sp<IImageStreamBuffer> const& pStreamBuffer,
                                        sp<IImageBuffer> &pImageBuffer
                                    );

    MERROR                          returnLockedImageBuffer(
                                        sp<IImageBuffer> const &pImageBuffer,
                                        sp<IImageStreamBuffer> const &pStreamBuffer,
                                        sp<IImageStreamBufferPoolT> const &pStreamPool
                                    );

    MERROR                          returnUnlockedImageBuffer(
                                        sp<IImageStreamBuffer> const& pStreamBuffer,
                                        sp<IImageStreamBufferPoolT> const &pStreamPool
                                    );

    MUINT32                         get_and_increase_magicnum()
                                    {
                                        Mutex::Autolock _l(mlastNumLock);
                                        MUINT32 ret = mlastNum++;
                                        //skip num = 0 as 3A would callback 0 when request stack is empty
                                        //skip -1U as a reserved number to indicate that which would never happen in 3A queue
                                        if(ret==0 || ret==-1U) ret=mlastNum=1;
                                        return ret;
                                    }
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operations in base class Thread
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////
    // Ask this object's thread to exit. This function is asynchronous, when the
    // function returns the thread might still be running. Of course, this
    // function can be called from a different thread.
    virtual void                    requestExit();

    // Good place to do one-time initializations
    virtual status_t                readyToRun();

private:
    // Derived class must implement threadLoop(). The thread starts its life
    // here. There are two ways of using the Thread object:
    // 1) loop: if threadLoop() returns true, it will be called again if
    //          requestExit() wasn't called.
    // 2) once: if threadLoop() returns false, the thread will exit upon return.
    virtual bool                    threadLoop();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
                                    P1NodeImp();
    virtual                        ~P1NodeImp();
    virtual MERROR                  config(ConfigParams const& rParams);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IPipelineNode Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.

    virtual MERROR                  init(InitParams const& rParams);

    virtual MERROR                  uninit();

    virtual MERROR                  flush();

    virtual MERROR                  queue(
                                        sp<IPipelineFrame> pFrame
                                    );

public:     ////                    Operations.

    virtual void                    doNotifyCb (
                                        MINT32  _msgType,
                                        MINTPTR _ext1,
                                        MINTPTR _ext2,
                                        MINTPTR _ext3
                                    );

    static void                     doNotifyDropframe(MUINT magicNum, void* cookie);

};
};  //namespace


/******************************************************************************
 *
 ******************************************************************************/
P1NodeImp::
P1NodeImp()
    : BaseNode()
    , P1Node()
    , mConfigRWLock()
    , mInitLock()
    , mInit(MTRUE)
    //, mImageStorage()
    //
    , m3AProcessedDepth(3)
    , mNumHardwareBuffer(3)
    , mDelayframe(3)
    , mlastNum(1)
    , mlastNumLock()
    , mLastSofIdx(P1SOFIDX_NULL_VAL)
    , mHardwareLock()
    , mActiveLock()
    , mActive(MFALSE)
    , mReadyLock()
    , mReady(MFALSE)
    , mpCamIO(NULL)
    , mp3A(NULL)
    #if SUPPORT_EIS
    , mpEIS(NULL)
    #endif
    //
    #if (SUPPORT_SCALING_CROP)
    , mPixelMode(0)
    #endif
    , mConfigPort(CONFIG_PORT_NONE)
    , mIsBinEn(false)
    //
    , mRequestQueueLock()
    , mRequestQueue()
    //
    , mControls3AListLock()
    , mControls3AList()
    , mControls3AListCond()
    //
    //, m3AStorage()
    //
    , mProcessingQueueLock()
    , mProcessingQueueCond()
    , mProcessingQueue()
    //
    , mDropQueueLock()
    , mDropQueue()
    //
    , mThreadLock()
    , mThreadCond()
    //
    , mDequeThreadProfile("P1Node::deque", 15000000LL)
    , mInFlightRequestCnt(0)
{
    char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    mLogLevel = ::atoi(cLogLevel);
    if ( 0 == mLogLevel ) {
        ::property_get("debug.camera.log.p1node", cLogLevel, "0");
        mLogLevel = ::atoi(cLogLevel);
    }
#if 1 /*[EP_TEMP]*/ //[FIXME] TempTestOnly
    #warning "[FIXME] force enable P1Node log"
    if (mLogLevel < 2) {
        mLogLevel = 2;
    }
#endif
}


/******************************************************************************
 *
 ******************************************************************************/
P1NodeImp::
~P1NodeImp()
{
    MY_LOGD("");
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
init(InitParams const& rParams)
{
    FUNCTION_IN;

    {
        RWLock::AutoWLock _l(mConfigRWLock);
        //
        mOpenId  = rParams.openId;
        mNodeId  = rParams.nodeId;
        mNodeName= rParams.nodeName;
    }

    MERROR err = run();

    FUNCTION_OUT;

    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
uninit()
{
    PUBLIC_API_IN;

    // flush the left frames if exist
    onHandleFlush(MFALSE);

    requestExit();

    PUBLIC_API_OUT;

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
check_config(ConfigParams const& rParams)
{
    CAM_TRACE_CALL();

    if (rParams.pInAppMeta == NULL ) {
        MY_LOGE("in metadata is null");
        return BAD_VALUE;
    }

    if (rParams.pOutAppMeta == NULL && !rParams.isStereoMain2) {
        MY_LOGE("out app metadata is null");
        return BAD_VALUE;
    }

    if (rParams.pOutHalMeta == NULL) {
        MY_LOGE("out hal metadata is null");
        return BAD_VALUE;
    }

    if (rParams.pvOutImage_full.size() == 0 && rParams.pOutImage_resizer == NULL) {
        MY_LOGE("image is empty");
        return BAD_VALUE;
    }


    if (rParams.pStreamPool_full != NULL && rParams.pvOutImage_full.size() == 0) {
        MY_LOGE("wrong full input");
        return BAD_VALUE;
    }

    if (rParams.pStreamPool_resizer != NULL && rParams.pOutImage_resizer == NULL) {
        MY_LOGE("wrong resizer input");
        return BAD_VALUE;
    }

    //
    {
        RWLock::AutoWLock _l(mConfigRWLock);
        //
        if(rParams.pInAppMeta != 0) {
            mInAppMeta = rParams.pInAppMeta;
            mInStreamIds.add(mInAppMeta->getStreamId());
        } else {
            mInAppMeta = NULL;
            mInStreamIds.clear();
        }
        //
        if(rParams.pInHalMeta != 0) {
            mInHalMeta = rParams.pInHalMeta;
            mInStreamIds.add(mInHalMeta->getStreamId());
        } else {
            mInHalMeta = NULL;
            mInStreamIds.clear();
        }
        mOutAppMeta          = (rParams.pOutAppMeta != 0)?         rParams.pOutAppMeta         : NULL;
        mOutHalMeta          = (rParams.pOutHalMeta != 0)?         rParams.pOutHalMeta         : NULL;
        mOutImage_resizer    = (rParams.pOutImage_resizer != 0)?   rParams.pOutImage_resizer   : NULL;
        mpStreamPool_full    = (rParams.pStreamPool_full != 0)?    rParams.pStreamPool_full    : NULL;
        mpStreamPool_resizer = (rParams.pStreamPool_resizer != 0)? rParams.pStreamPool_resizer : NULL;
        if(rParams.pvOutImage_full.size() != 0) {
            mvOutImage_full  = rParams.pvOutImage_full;
        } else {
            mvOutImage_full.clear();
        }
                                      mSensorParams                = rParams.sensorParams;
    }

    MY_LOGI_IF(mOutAppMeta!= NULL, "[Config] In Meta Id: 0x%x, Out APP Meta Id: 0x%x, Out Hal Meta Id: 0x%x",
            mInAppMeta->getStreamId(), mOutAppMeta->getStreamId(), mOutHalMeta->getStreamId());

    for(size_t i = 0; i < mvOutImage_full.size(); i++) {
        MY_LOGI("[Config] full image Id: 0x%x (%d,%d)",
                 mvOutImage_full[i]->getStreamId(), mvOutImage_full[i]->getImgSize().w, mvOutImage_full[i]->getImgSize().h);
    }
    if (mOutImage_resizer != NULL) {
        MY_LOGI("[Config] resizer image Id: 0x%x (%d,%d)",
                 mOutImage_resizer->getStreamId(), mOutImage_resizer->getImgSize().w, mOutImage_resizer->getImgSize().h);
    }

#if (SUPPORT_SCALING_CROP)
    {
        sp<IMetadataProvider> pMetadataProvider = NSMetadataProviderManager::valueFor(getOpenId());
        if( ! pMetadataProvider.get() ) {
            MY_LOGE(" ! pMetadataProvider.get() ");
            return DEAD_OBJECT;
        }
        IMetadata static_meta =
            pMetadataProvider->geMtktStaticCharacteristics();
        if( tryGetMetadata<MRect>(&static_meta,
            MTK_SENSOR_INFO_ACTIVE_ARRAY_REGION, mActiveArray) ) {
            MY_LOGD_IF(1,"active array(%d, %d, %dx%d)",
                    mActiveArray.p.x, mActiveArray.p.y,
                    mActiveArray.s.w, mActiveArray.s.h);
        } else {
            MY_LOGE("no static info: MTK_SENSOR_INFO_ACTIVE_ARRAY_REGION");
            #ifdef USING_MTK_LDVT
            mActiveArray = MRect(mSensorParams.size.w, mSensorParams.size.h);
            MY_LOGI("set sensor size to active array(%d, %d, %dx%d)",
                    mActiveArray.p.x, mActiveArray.p.y,
                    mActiveArray.s.w, mActiveArray.s.h);
            #else
            return UNKNOWN_ERROR;
            #endif
        }
    }
#endif

    return OK;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
config(ConfigParams const& rParams)
{
    PUBLIC_API_IN

    Mutex::Autolock _l(mPublicLock);
    //(1) check
    MERROR err = check_config(rParams);
    if (err != OK) {
        return err;
    }

    //(2) configure hardware

    if(getActive()) {
        MY_LOGD("active=%d", getActive());
        onHandleFlush(MFALSE);
    }

    err = hardwareOps_start();
    if (err != OK) {
        return err;
    }

    PUBLIC_API_OUT

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
queue(
    sp<IPipelineFrame> pFrame
)
{
    PUBLIC_API_IN

    CAM_TRACE_CALL();
    MY_LOGD1("active=%d", getActive());

    if (!getActive()) {
        MY_LOGI("[%s#%d]", __FUNCTION__,  __LINE__);
        sp<IPipelineFrame> t1Frame;
        MY_LOGI("[%s#%d]", __FUNCTION__,  __LINE__);
        hardwareOps_start();
        MY_LOGI("[%s#%d]", __FUNCTION__,  __LINE__);
        sp<IPipelineFrame> t2Frame;
        MY_LOGI("[%s#%d]", __FUNCTION__,  __LINE__);
    }

    {
        Mutex::Autolock _l(mControls3AListLock);

        //block condition 1: if pipeline is full
        while (mControls3AList.size() > (size_t)m3AProcessedDepth) {
            MY_LOGD1("wait: %d > %d", mControls3AList.size(), (size_t)m3AProcessedDepth);
            status_t status = mControls3AListCond.wait(mControls3AListLock);
            MY_LOGD1("wait-");
            if  ( OK != status ) {
                MY_LOGW(
                    "wait status:%d:%s, mControls3AList.size:%zu",
                    status, ::strerror(-status), mControls3AList.size()
                );
            }
        }
        //compensate to the number of mProcessedDepth
        while(mControls3AList.size() < (size_t)m3AProcessedDepth) {
            createNode(&mControls3AList, NULL);
        }

        //push node from appFrame
        createNode(pFrame, &mRequestQueue, &mRequestQueueLock, &mControls3AList, NULL);
        android_atomic_inc(&mInFlightRequestCnt);
        ATRACE_INT("P1_request_cnt", android_atomic_acquire_load(&mInFlightRequestCnt));
    }

    PUBLIC_API_OUT

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
flush()
{
    PUBLIC_API_IN

    CAM_TRACE_CALL();

    Mutex::Autolock _l(mPublicLock);

    onHandleFlush(MFALSE);

    //[TODO]
    //wait until deque thread going back to waiting state;
    //in case next node receives queue() after flush()

    PUBLIC_API_OUT

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
void
P1NodeImp::
requestExit()
{
    FUNCTION_IN_P1

    //let deque thread back
    Thread::requestExit();
    {
        Mutex::Autolock _l(mThreadLock);
        mThreadCond.broadcast();
    }
    join();

    //let enque thread back
    Mutex::Autolock _l(mControls3AListLock);
    mControls3AListCond.broadcast();

    FUNCTION_OUT_P1
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
P1NodeImp::
readyToRun()
{
    // set name
    ::prctl(PR_SET_NAME, (unsigned long)"Cam@P1NodeImp", 0, 0, 0);

    // set normal
    struct sched_param sched_p;
    sched_p.sched_priority = 0;
    ::sched_setscheduler(0, P1THREAD_POLICY, &sched_p);
    ::setpriority(PRIO_PROCESS, 0, P1THREAD_PRIORITY);   //  Note: "priority" is nice value.
    //
    ::sched_getparam(0, &sched_p);
    MY_LOGD(
        "Tid: %d, policy: %d, priority: %d"
        , ::gettid(), ::sched_getscheduler(0)
        , sched_p.sched_priority
    );

    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
bool
P1NodeImp::
threadLoop()
{
    // check if going to leave thread
    CAM_TRACE_CALL();
    {
        Mutex::Autolock _l(mThreadLock);

        if (!getReady()) {
            MY_LOGD("wait+");
            mThreadCond.wait(mThreadLock);
            MY_LOGD("wait-");
        }

        if (exitPending()) {
            MY_LOGD("leaving");
            return false;
        }
    }

    // deque buffer, and handle frame and metadata
    onProcessDequedFrame();


    // trigger point for the first time
    {
        RWLock::AutoWLock _l(mConfigRWLock);
        if (getInit()) {
            onProcessFrame(MTRUE);
            setInit(MFALSE);
        }
    }

    onProcessDropFrame();
    return true;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
setActive(
    MBOOL active
)
{
    Mutex::Autolock _l(mActiveLock);
    mActive = active;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
P1NodeImp::
getActive(
    void
)
{
    Mutex::Autolock _l(mActiveLock);
    return mActive;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
setReady(
    MBOOL ready
)
{
    Mutex::Autolock _l(mReadyLock);
    mReady = ready;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
P1NodeImp::
getReady(
    void
)
{
    Mutex::Autolock _l(mReadyLock);
    return mReady;
}



/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
setInit(
    MBOOL init
)
{
    Mutex::Autolock _l(mInitLock);
    mInit = init;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
P1NodeImp::
getInit(
    void
)
{
    Mutex::Autolock _l(mInitLock);
    return mInit;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onProcessFrame(
    MBOOL initial,
    MUINT32 magicNum,
    MUINT32 sofIdx
)
{
    FUNCTION_IN_P1

    //(1)
    if(!initial) { // [TODO] && VALID 3A PROCESSED NOTIFY
        QueNode_T node;
        bool exist = false;
        {
            Mutex::Autolock _l(mRequestQueueLock);
            Que_T::iterator it = mRequestQueue.begin();
            for(; it != mRequestQueue.end(); it++) {
                if ((*it).magicNum == magicNum) {
                    node = *it;
                    node.sofIdx = sofIdx;
                    mRequestQueue.erase(it);
                    exist = true;
                    break;
                }
            }
        }
        if (exist) {
            onProcessEnqueFrame(node);
            Mutex::Autolock _ll(mProcessingQueueLock);
            mProcessingQueue.push_back(node);
        } else {
            MY_LOGW_IF(magicNum!=0, "no: %d", magicNum);
            Mutex::Autolock _l(mRequestQueueLock);
            String8 str;
            str += String8::format("[req/size(%d)]: ", (int)(mRequestQueue.size()));
            Que_T::iterator it = mRequestQueue.begin();
            for(; it != mRequestQueue.end(); it++) {
                str += String8::format("%d ", (*it).magicNum);
            }
            MY_LOGD("%s", str.string());
        }
    }

    //(2)
    {
        Mutex::Autolock _l(mControls3AListLock);
        if (!mControls3AList.empty()) {
            mControls3AList.erase(mControls3AList.begin());
        }
        mControls3AListCond.broadcast();

        //dump
        MY_LOGD1("mControls3AList size %d", mControls3AList.size());
        String8 str("[3A]: ");
        List<MetaSet_T>::iterator it = mControls3AList.begin();
        for (; it != mControls3AList.end(); it++) {
            str += String8::format("%d ", it->halMeta.entryFor(MTK_P1NODE_PROCESSOR_MAGICNUM).itemAt(0, Type2Type< MINT32 >()));
        }
        MY_LOGD1("%s", str.string());

    }
    //(3)
    #if SUPPORT_3A
    if (mp3A) {
        mp3A->set(mControls3AList);
    }
    #endif
    FUNCTION_OUT_P1
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onProcessEnqueFrame(
    QueNode_T &pFrame
)
{
    FUNCTION_IN_P1;

    //(1)
    //pass request directly if it's a reprocessing one
    //[TODO]
    //if( mInHalMeta == NULL) {
    //    onDispatchFrame(pFrame);
    //    return;
    //}

    //(2)
    hardwareOps_enque(pFrame);

    FUNCTION_OUT_P1;
}


/******************************************************************************
 *
 ******************************************************************************/
P1NodeImp::QueNode_T
P1NodeImp::
getProcessingFrame_ByNumber(MUINT32 magicNum)
{
    FUNCTION_IN_P1;
    QueNode_T frame;

    Mutex::Autolock _l(mProcessingQueueLock);
    if (mProcessingQueue.empty()) {
        MY_LOGE("mProcessingQueue is empty");
        return frame;
    }

    #if 1
        Que_T::iterator it = mProcessingQueue.begin();
        for (; it != mProcessingQueue.end(); it++) {
            frame = *it;
            if (frame.magicNum == magicNum) {
                break;
            }
        }
        if (it == mProcessingQueue.end()) {
            MY_LOGE("cannot find the right node for num: %d", magicNum);
            return frame;
        }
        else {
            frame = *it;
            mProcessingQueue.erase(it);
            mProcessingQueueCond.broadcast();
        }
    #else
        frame = *mProcessingQueue.begin();
        mProcessingQueue.erase(mProcessingQueue.begin());
        mProcessingQueueCond.broadcast();
    #endif

    FUNCTION_OUT_P1;
    //
    return frame;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
P1NodeImp::
getProcessingFrame_ByAddr(IImageBuffer* const imgBuffer,
                          MUINT32 magicNum,
                          QueNode_T &frame
)
{
    FUNCTION_IN_P1;

    MBOOL ret = MFALSE;
    if (imgBuffer == NULL) {
        MY_LOGE("imgBuffer == NULL");
        return ret;
    }

    // get the right node from mProcessingQueue
    Mutex::Autolock _l(mProcessingQueueLock);
    if (mProcessingQueue.empty()) {
        MY_LOGE("ProQ is empty");
        return ret;
    }

    Que_T::iterator it = mProcessingQueue.begin();
    for (; it != mProcessingQueue.end(); it++) {
        frame = *it;
        if (imgBuffer == (*it).img_full.get() ||
            imgBuffer == (*it).img_resizer.get()) {
            if (frame.magicNum == magicNum) {
                ret = MTRUE;
            } else {
                #if SUPPORT_PERFRAME_CTRL
                MY_LOGE("magicNum from driver(%d), should(%d)",
                       magicNum, frame.magicNum);
                #else
                if((magicNum & 0x40000000) != 0) {
                    MY_LOGW("magicNum from driver(%b) is uncertain",
                          magicNum);
                    ret = MFALSE;
                } else {
                    ret = MTRUE;
                    MY_LOGW("magicNum from driver(%d), should(%d)",
                          magicNum, frame.magicNum);
                }
                #endif
            }
            break;
        } else {
            continue;
        }
    }

    if (it == mProcessingQueue.end()) {
        MY_LOGE("no node with imagebuf(0x%x), PA(0x%x), num(%d)",
                 imgBuffer, imgBuffer->getBufPA(0), magicNum);
        for (Que_T::iterator it = mProcessingQueue.begin(); it != mProcessingQueue.end(); it++) {
            MY_LOGW("[proQ] num(%d)", (*it).magicNum);
            MY_LOGW_IF((*it).img_full!=NULL, "[proQ] imagebuf(0x%x), PA(0x%x)",
                (*it).img_full.get(), (*it).img_full->getBufPA(0));
            MY_LOGW_IF((*it).img_resizer!=NULL, "[proQ] imagebuf(0x%x), PA(0x%x)",
                (*it).img_resizer.get(), (*it).img_resizer->getBufPA(0));
        }
        for (Que_T::iterator it = mRequestQueue.begin(); it != mRequestQueue.end(); it++) {
            MY_LOGW("[reqQ] magic %d:", (*it).magicNum);
        }
    }
    else {
        frame = *it;
        mProcessingQueue.erase(it);
        mProcessingQueueCond.broadcast();
        MY_LOGD1("magic: %d", magicNum);
    }

    FUNCTION_OUT_P1;
    //
    return ret;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
onProcessDropFrame()
{
    Vector<QueNode_T > nodeQ;
    {
        Mutex::Autolock _l(mDropQueueLock);
        for(size_t i = 0; i < mDropQueue.size(); i++) {
            QueNode_T node = getProcessingFrame_ByNumber(mDropQueue[i]);
            nodeQ.push_back(node);
            MY_LOGD("drop: %d", mDropQueue[i]);
        }
        mDropQueue.clear();
    }

    for(size_t i = 0; i < nodeQ.size(); i++) {
         onFlushProcessingFrame(nodeQ[i]);
    }

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
onProcessDequedFrame()
{

#if 0
    // [FIXME]  temp-WA for DRV currently not implement self-signal
    //          the dequeue might be blocked while en-queue empty
    //          it should be removed after DRV self-signal ready
    {
        Mutex::Autolock _ll(mProcessingQueueLock);
        if (mProcessingQueue.empty()) {
            return OK;
        }
    }
#endif

    FUNCTION_IN_P1;

    CAM_TRACE_CALL();

    QBufInfo deqBuf;
    MetaSet_T result3A;
    if(hardwareOps_deque(deqBuf, result3A) != OK) {
        return BAD_VALUE;
    }

    if (deqBuf.mvOut.size() == 0) {
        MY_LOGI("DeqBuf Out Size is 0");
        return ((getActive()) ? BAD_VALUE : OK);
    }

    QueNode_T pFrame;
    MBOOL match = getProcessingFrame_ByAddr(deqBuf.mvOut[0].mBuffer,
                                          deqBuf.mvOut[0].mMetaData.mMagicNum_hal,
                                          pFrame);
    #if 0
    if (pFrame.magicNum > 0 && pFrame.magicNum < 15) {
        printf("[%d]pFrame.img_resizer.get() = [%p]\n",
            pFrame.magicNum, pFrame.img_resizer.get());
        if (pFrame.img_resizer.get() != NULL) {
            char filename[256] = {0};
            sprintf(filename, "/sdcard/raw/P1B_%d_%dx%d.raw",
                pFrame.magicNum,
                pFrame.img_resizer->getImgSize().w,
                pFrame.img_resizer->getImgSize().h
                );
            printf("SAVE BUF [%s]\n", filename);
            pFrame.img_resizer->saveToFile(filename);
        }
    }
    #endif
    MERROR ret;
    if (match == MFALSE) {
        onFlushProcessingFrame(pFrame);
        ret = BAD_VALUE;
    }
    else {
        IMetadata resultEIS;
        IMetadata inAPP;
        if(OK == lockMetadata(pFrame.appFrame, mInAppMeta->getStreamId(), inAPP)){
            if (isEISOn(&inAPP) || is3DNROn(&inAPP)) {
                onProcessEIS(pFrame, resultEIS, deqBuf);
            }
        }
        onReturnProcessingFrame(pFrame, deqBuf, result3A, resultEIS);
        mLastSofIdx = pFrame.sofIdx;
        ret = OK;
    }

    #if SUPPORT_EIS
    if (IS_PORT(CONFIG_PORT_EISO, mConfigPort)) {
        // call EIS notify function
        mpEIS->NotifyEis(deqBuf);
    }
    #endif
    #if 0
    if (IS_PORT(CONFIG_PORT_LCSO, mConfigPort)) {
        // call LCS notify function
        // mpLCS->NotifyLcs(deqBuf);
    }
    #endif

    FUNCTION_OUT_P1;

    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onHandleFlush(
    MBOOL wait
)
{
    FUNCTION_IN_P1;
    CAM_TRACE_CALL();

    //wake up queue thread.
    {
        Mutex::Autolock _l(mControls3AListLock);
        mControls3AListCond.broadcast();
    }

    //stop hardware
    if (!wait) {
        hardwareOps_stop(); //include hardware and 3A
    }

    //(1) clear request queue
    {
        Mutex::Autolock _l(mRequestQueueLock);
        while(!mRequestQueue.empty()) {
            QueNode_T pFrame = *mRequestQueue.begin();
            mRequestQueue.erase(mRequestQueue.begin());
            onFlushRequestFrame(pFrame);
        }
    }

    //(2) clear processing queue
    //     wait until processing frame coming out
    if (wait) {
        Mutex::Autolock _l(mProcessingQueueLock);
        while(!mProcessingQueue.empty()) {
            mProcessingQueueCond.wait(mProcessingQueueLock);
        }
    } else {
        // must guarantee hardware has been stopped.
        Mutex::Autolock _l(mProcessingQueueLock);
        while(!mProcessingQueue.empty()) {
            QueNode_T pFrame = *mProcessingQueue.begin();
            mProcessingQueue.erase(mProcessingQueue.begin());
            onFlushProcessingFrame(pFrame);
        }
    }

    //(3) clear dummy queue

    //(4) clear drop frame queue
    onProcessDropFrame();


    //(5) clear all
    mRequestQueue.clear(); //suppose already clear
    mProcessingQueue.clear(); //suppose already clear
    mControls3AList.clear();
    //mImageStorage.uninit();
    //m3AStorage.clear();
    mlastNum = 1;

    FUNCTION_OUT_P1;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onReturnProcessingFrame(
    QueNode_T const &pFrame,
    QBufInfo const &deqBuf,
    MetaSet_T const &result3A,
    IMetadata const &resultEIS
)
{
    FUNCTION_IN_P1;

    CAM_TRACE_CALL();


    if (pFrame.appFrame != 0) {

        // Out Image Stream
        if (mOutImage_resizer != NULL) {
            returnLockedImageBuffer(pFrame.appFrame,
                mOutImage_resizer->getStreamId(), pFrame.img_resizer, true);
        }
        for(size_t i = 0; i < mvOutImage_full.size(); i++) {
            returnLockedImageBuffer(pFrame.appFrame,
                mvOutImage_full[i]->getStreamId(), pFrame.img_full, true);
        }

        // APP in Meta Stream
        if (mInAppMeta != NULL) {
            StreamId_T const streamId_InAppMeta = mInAppMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_InAppMeta);
        }

        // APP out Meta Stream
        if (mOutAppMeta != NULL){
            IMetadata appMetadata;
            StreamId_T const streamId_OutAppMeta = mOutAppMeta->getStreamId();
            generateAppMeta(pFrame.appFrame, result3A, deqBuf, appMetadata);
            #if 1
            lock_and_returnMetadata(pFrame.appFrame, streamId_OutAppMeta, appMetadata);
            #else
            m3AStorage.add(pFrame.magicNum, pFrame.appFrame, appMetadata);
            appMetadata = m3AStorage.valueFor(pFrame.magicNum).resultVal;
            if(m3AStorage.isCompleted(pFrame.magicNum)) {
                lock_and_returnMetadata(pFrame.appFrame, streamId_OutAppMeta, appMetadata);
                m3AStorage.removeItem(pFrame.magicNum);
            }
            #endif
        }

        // HAL out Meta Stream
        if (mOutHalMeta != NULL){
            IMetadata inHalMetadata;
            IMetadata outHalMetadata;
            StreamId_T const streamId_OutHalMeta = mOutHalMeta->getStreamId();

            if (mInHalMeta != NULL) lockMetadata(pFrame.appFrame, mInHalMeta->getStreamId(), inHalMetadata);
            generateHalMeta(result3A, deqBuf, resultEIS, inHalMetadata, outHalMetadata);
            lock_and_returnMetadata(pFrame.appFrame, streamId_OutHalMeta, outHalMetadata);
        }

        // HAL in Meta Stream
        if (mInHalMeta != NULL) {
            StreamId_T const streamId_InHalMeta = mInHalMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_InHalMeta);
        }

        // Apply buffers to release
        IStreamBufferSet& rStreamBufferSet  = pFrame.appFrame->getStreamBufferSet();
        rStreamBufferSet.applyRelease(getNodeId());

        // dispatch to next node
        onDispatchFrame(pFrame.appFrame);
        MY_LOGD1("[return OK]: (%d, %d)", pFrame.appFrame->getFrameNo(), pFrame.magicNum);
        android_atomic_dec(&mInFlightRequestCnt);
        ATRACE_INT("P1_request_cnt", android_atomic_acquire_load(&mInFlightRequestCnt));

    }
    else {
        if (pFrame.reqType == REQ_TYPE_INITIAL) {
            if (IS_OUT(REQ_OUT_RESIZER, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_RESIZER_STUFF, pFrame.reqOutSet)) {
                if (mpStreamPool_resizer != NULL) {
                    returnLockedImageBuffer(pFrame.img_resizer, pFrame.halFrame_resizer, mpStreamPool_resizer);
                } else {
                    sp<IImageBuffer> pImgBuf = pFrame.img_resizer;
                    destroyStuffBuffer(pImgBuf);
                }
            }
            if (IS_OUT(REQ_OUT_FULL, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_FULL_PURE, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_FULL_STUFF, pFrame.reqOutSet)) {
                if (mpStreamPool_full != NULL) {
                    returnLockedImageBuffer(pFrame.img_full, pFrame.halFrame_full, mpStreamPool_full);
                } else {
                    sp<IImageBuffer> pImgBuf = pFrame.img_full;
                    destroyStuffBuffer(pImgBuf);
                }
            }
        }
    }

    FUNCTION_OUT_P1;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onFlushRequestFrame(
    QueNode_T const& pFrame
)
{
    FUNCTION_IN_P1;
    CAM_TRACE_CALL();

    if (pFrame.appFrame != 0) {

        // Out Image Stream
        Vector<StreamId_T> streamId_Images;
        if (mOutImage_resizer != NULL) {
            streamId_Images.push_back(mOutImage_resizer->getStreamId());
        }
        for(size_t i = 0; i < mvOutImage_full.size(); i++) {
            streamId_Images.push_back(mvOutImage_full[i]->getStreamId());
        }

        for(size_t i = 0; i < streamId_Images.size(); i++) {
            StreamId_T const streamId_Image = streamId_Images[i];
            returnUnlockedImageBuffer(pFrame.appFrame, streamId_Image);
        }


        // APP in Meta Stream
        if (mInAppMeta != NULL) {
            StreamId_T const streamId_InAppMeta = mInAppMeta->getStreamId();
            returnUnlockedMetadata(pFrame.appFrame, streamId_InAppMeta);
        }

        // HAL in Meta Stream
        if (mInHalMeta != NULL) {
            StreamId_T const streamId_InHalMeta = mInHalMeta->getStreamId();
            returnUnlockedMetadata(pFrame.appFrame, streamId_InHalMeta);
        }

        // APP out Meta Stream
        if (mOutAppMeta != NULL) {
            StreamId_T const streamId_OutAppMeta = mOutAppMeta->getStreamId();
            returnUnlockedMetadata(pFrame.appFrame, streamId_OutAppMeta);
        }

        // HAL out Meta Stream
        if (mOutHalMeta != NULL) {
            StreamId_T const streamId_OutHalMeta = mOutHalMeta->getStreamId();
            returnUnlockedMetadata(pFrame.appFrame, streamId_OutHalMeta);
        }

        // Apply buffers to release.
        IStreamBufferSet& rStreamBufferSet  = pFrame.appFrame->getStreamBufferSet();
        rStreamBufferSet.applyRelease(getNodeId());

        // dispatch to next node
        onDispatchFrame(pFrame.appFrame);
        MY_LOGD1("[return flush]: (%d, %d)", pFrame.appFrame->getFrameNo(), pFrame.magicNum);
        android_atomic_dec(&mInFlightRequestCnt);
        ATRACE_INT("P1_request_cnt", android_atomic_acquire_load(&mInFlightRequestCnt));
    }
    else {
        if (pFrame.halFrame_full != 0) {
            returnUnlockedImageBuffer(pFrame.halFrame_full, mpStreamPool_full);
        }
        if (pFrame.halFrame_resizer != 0) {
            returnUnlockedImageBuffer(pFrame.halFrame_resizer, mpStreamPool_resizer);
        }
    }


    FUNCTION_OUT_P1;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onFlushProcessingFrame(
    QueNode_T const& pFrame
)
{
    FUNCTION_IN_P1;
    CAM_TRACE_CALL();

    if (pFrame.appFrame != 0) {
        MBOOL success = MFALSE;

        // Out Image Stream
        if (mOutImage_resizer != NULL) {
            returnLockedImageBuffer(pFrame.appFrame,
                mOutImage_resizer->getStreamId(), pFrame.img_resizer, success);
        }
        for(size_t i = 0; i < mvOutImage_full.size(); i++) {
            returnLockedImageBuffer(pFrame.appFrame,
                mvOutImage_full[i]->getStreamId(), pFrame.img_full, success);
        }

        // APP in Meta Stream
        if (mInAppMeta != NULL) {
            StreamId_T const streamId_InAppMeta = mInAppMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_InAppMeta, success);
        }

        // HAL in Meta Stream
        if (mInHalMeta != NULL) {
            StreamId_T const streamId_InHalMeta = mInHalMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_InHalMeta, success);
        }

        // APP out Meta Stream
        if (mOutAppMeta != NULL) {
            StreamId_T const streamId_OutAppMeta = mOutAppMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_OutAppMeta, success);
        }

        // HAL out Meta Stream
        if (mOutHalMeta != NULL) {
            StreamId_T const streamId_OutHalMeta  = mOutHalMeta->getStreamId();
            returnLockedMetadata(pFrame.appFrame, streamId_OutHalMeta, success);
        }

        // Apply buffers to release
        IStreamBufferSet& rStreamBufferSet  = pFrame.appFrame->getStreamBufferSet();
        rStreamBufferSet.applyRelease(getNodeId());

        // dispatch to next node
        onDispatchFrame(pFrame.appFrame);
        MY_LOGD1("[return flush]: (%d, %d)", pFrame.appFrame->getFrameNo(), pFrame.magicNum);
        android_atomic_dec(&mInFlightRequestCnt);
        ATRACE_INT("P1_request_cnt", android_atomic_acquire_load(&mInFlightRequestCnt));
    }
    else {
        if (pFrame.reqType == REQ_TYPE_INITIAL) {
            if (IS_OUT(REQ_OUT_RESIZER, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_RESIZER_STUFF, pFrame.reqOutSet)) {
                if (mpStreamPool_resizer != NULL) {
                    returnLockedImageBuffer(pFrame.img_resizer, pFrame.halFrame_resizer, mpStreamPool_resizer);
                } else {
                    sp<IImageBuffer> pImgBuf = pFrame.img_resizer;
                    destroyStuffBuffer(pImgBuf);
                }
            }
            if (IS_OUT(REQ_OUT_FULL, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_FULL_PURE, pFrame.reqOutSet) ||
                IS_OUT(REQ_OUT_FULL_STUFF, pFrame.reqOutSet)) {
                if (mpStreamPool_full != NULL) {
                    returnLockedImageBuffer(pFrame.img_full, pFrame.halFrame_full, mpStreamPool_full);
                } else {
                    sp<IImageBuffer> pImgBuf = pFrame.img_full;
                    destroyStuffBuffer(pImgBuf);
                }
            }
        }
    }

    FUNCTION_OUT_P1;
}

#if 0
/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onProcess3AResult(
    MUINT32 magicNum,
    MUINT32 key,
    MUINT32 val
)
{
    MY_LOGD2("%d", magicNum);

    if(magicNum == 0) return;

    m3AStorage.add(magicNum, key, val);
    if(m3AStorage.isCompleted(magicNum)) {
        sp<IPipelineFrame> spFrame = m3AStorage.valueFor(magicNum).spFrame;
        StreamId_T const streamId_OutAppMeta = mOutAppMeta->getStreamId();
        IMetadata appMetadata = m3AStorage.valueFor(magicNum).resultVal;
        lock_and_returnMetadata(spFrame, streamId_OutAppMeta, appMetadata);
        m3AStorage.removeItem(magicNum);

        IStreamBufferSet& rStreamBufferSet  = spFrame->getStreamBufferSet();
        rStreamBufferSet.applyRelease(getNodeId());
    }
}
#endif

/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
onProcessEIS(
    QueNode_T const &pFrame,
    IMetadata &resultEIS,
    QBufInfo const &deqBuf
)
{
    if(pFrame.appFrame ==NULL) {
        return;
    }
    if (deqBuf.mvOut.size() == 0) {
        MY_LOGW("DeQ Buf is empty, result count (%d)", resultEIS.count());
        return;
    }
    #if SUPPORT_EIS
    MUINT64 timestamp = deqBuf.mvOut[0].mMetaData.mTimeStamp;
    EIS_HAL_CONFIG_DATA config;
    config.p1ImgW = mSensorParams.size.w; // [AWARE] need to revise by platform
    config.p1ImgH = mSensorParams.size.h; //
    #if 1 // use RRZO DstSize
    for (size_t i = 0; i < deqBuf.mvOut.size(); i++) {
        if (deqBuf.mvOut[i].mPortID.index == PORT_RRZO.index) {
            config.p1ImgW = deqBuf.mvOut[i].mMetaData.mDstSize.w;
            config.p1ImgH = deqBuf.mvOut[i].mMetaData.mDstSize.h;
            break;
        } else {
            continue;
        }
    }
    #endif
    //mpEIS->DoEis(EIS_PASS_1, &config, timestamp);
    mpEIS->DoEis(EIS_PASS_1, deqBuf);   //[TODO]
    MBOOL isLastSkipped = CHECK_LAST_FRAME_SKIPPED(mLastSofIdx, pFrame.sofIdx);
    MUINT32 X_INT, Y_INT, X_FLOAT, Y_FLOAT, WIDTH, HEIGHT, MVtoCenterX, MVtoCenterY,ISFROMRRZ;
    mpEIS->GetEisResult(X_INT, X_FLOAT, Y_INT, Y_FLOAT, WIDTH, HEIGHT, MVtoCenterX, MVtoCenterY, ISFROMRRZ);
    MINT32 GMV_X, GMV_Y;
    MUINT32 ConfX,ConfY;
    mpEIS->GetEisGmv(GMV_X, GMV_Y, &ConfX, &ConfY);
    IMetadata::IEntry entry(MTK_EIS_REGION);
    entry.push_back(X_INT, Type2Type< MINT32 >());
    entry.push_back(X_FLOAT, Type2Type< MINT32 >());
    entry.push_back(Y_INT, Type2Type< MINT32 >());
    entry.push_back(Y_FLOAT, Type2Type< MINT32 >());
    entry.push_back(WIDTH, Type2Type< MINT32 >());
    entry.push_back(HEIGHT, Type2Type< MINT32 >());
    entry.push_back(GMV_X, Type2Type< MINT32 >());
    entry.push_back(GMV_Y, Type2Type< MINT32 >());
    resultEIS.update(MTK_EIS_REGION, entry);
    MY_LOGD1("(%dx%d) %d, %d, %d, %d, %d, %d, %d, %d, %d",
        config.p1ImgW, config.p1ImgH,
        X_INT, X_FLOAT, Y_INT, Y_FLOAT, WIDTH, HEIGHT, GMV_X, GMV_Y,
        isLastSkipped);
    #endif
}


/******************************************************************************
 *
 ******************************************************************************/
void
P1NodeImp::
doNotifyCb(
    MINT32  _msgType,
    MINTPTR _ext1,
    MINTPTR _ext2,
    MINTPTR /*_ext3*/
)
{
    FUNCTION_IN_P1;
    MY_LOGD1("P1 doNotifyCb(%d) %d %d", _msgType, _ext1, _ext2);
    switch(_msgType)
    {
        case IHal3ACb::eID_NOTIFY_3APROC_FINISH:
            onProcessFrame(MFALSE, (MUINT32)_ext1, (MUINT32)_ext2);
            break;
        case IHal3ACb::eID_NOTIFY_CURR_RESULT:
            //onProcess3AResult((MUINT32)_ext1,(MUINT32)_ext2, (MUINT32)_ext3); //magic, key, val
            break;
        default:
            break;
    }
    FUNCTION_OUT_P1;
}


/******************************************************************************
 *
 ******************************************************************************/
void
P1NodeImp::
doNotifyDropframe(MUINT magicNum, void* cookie)
{
   if (cookie == NULL) {
       MY_LOGE("return cookie is NULL");
       return;
   }

   Mutex::Autolock _l(reinterpret_cast<P1NodeImp*>(cookie)->mDropQueueLock);
   reinterpret_cast<P1NodeImp*>(cookie)->mDropQueue.push_back(magicNum);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
createStuffBuffer(sp<IImageBuffer> & imageBuffer,
    sp<IImageStreamInfo> const& streamInfo,
    NSCam::MSize::value_type const changeHeight)
{
    MUINT32 stride = streamInfo->getBufPlanes()[0].rowStrideInBytes;
    MSize size = streamInfo->getImgSize();
    // change the height while changeHeight > 0
    if (changeHeight > 0) {
        size.h = changeHeight;
    }
    //
    return createStuffBuffer(imageBuffer,
        streamInfo->getStreamName(), streamInfo->getImgFormat(), size, stride);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
createStuffBuffer(sp<IImageBuffer> & imageBuffer,
    char const* szName, MINT32 format, MSize size, MUINT32 stride)
{
    Mutex::Autolock _l(mStuffBufferLock);
    imageBuffer = NULL;
    // add information to buffer name
    android::String8 imgBufName = android::String8(szName);
    char str[64] = {0};
    snprintf(str, sizeof(str), ":Size%dx%d:Stride%d:Id%d",
        size.w, size.h, stride, (int)(mvStuffBufferInfo.size()));
    imgBufName += str;
    // create buffer
    MINT32 bufBoundaryInBytes[3] = {0, 0, 0};
    MUINT32 bufStridesInBytes[3] = {stride, 0, 0};
    IImageBufferAllocator::ImgParam imgParam =
        IImageBufferAllocator::ImgParam((EImageFormat)format,
            size, bufStridesInBytes, bufBoundaryInBytes, 1);
    sp<IIonImageBufferHeap> pHeap =
        IIonImageBufferHeap::create(imgBufName.string(), imgParam);
    if (pHeap == NULL) {
        MY_LOGE("[%s] Stuff ImageBufferHeap create fail", imgBufName.string());
        return BAD_VALUE;
    }
    sp<IImageBuffer> pImgBuf = pHeap->createImageBuffer();
    if (pImgBuf == NULL) {
        MY_LOGE("[%s] Stuff ImageBuffer create fail", imgBufName.string());
        return BAD_VALUE;
    }
    // lock buffer
    MUINT const usage = (GRALLOC_USAGE_SW_READ_OFTEN |
                        GRALLOC_USAGE_HW_CAMERA_READ |
                        GRALLOC_USAGE_HW_CAMERA_WRITE);
    if (!(pImgBuf->lockBuf(imgBufName.string(), usage))) {
        MY_LOGE("[%s] Stuff ImageBuffer lock fail", imgBufName.string());
        return BAD_VALUE;
    }
    mvStuffBufferInfo.add(pImgBuf, imgBufName);
    imageBuffer = pImgBuf;
    //
    MY_LOGD1("Create Stuff Buffer (%s) OK", imgBufName.string());
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
destroyStuffBuffer(sp<IImageBuffer> & imageBuffer)
{
    Mutex::Autolock _l(mStuffBufferLock);
    if (imageBuffer == NULL) {
        MY_LOGW("Stuff ImageBuffer not exist");
        return BAD_VALUE;
    }
    ssize_t index = mvStuffBufferInfo.indexOfKey(imageBuffer);
    imageBuffer->unlockBuf(mvStuffBufferInfo.valueAt(index).string());
    // destroy buffer
    mvStuffBufferInfo.removeItemsAt(index);
    //imageBuffer = NULL;
    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_start()
{
#if SUPPORT_ISP
    FUNCTION_IN_P1;
    CAM_TRACE_CALL();

    Mutex::Autolock _l(mHardwareLock);

    setActive(MTRUE);
    setInit(MTRUE);
    mLastSofIdx = P1SOFIDX_NULL_VAL;

    mDequeThreadProfile.reset();
    //mImageStorage.init(mLogLevel);

    //
    CAM_TRACE_BEGIN("isp init");
    #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly
    mpCamIO = INormalPipe::createInstance(getOpenId(), "iopipeUseTM");
    #else
    mpCamIO = INormalPipe::createInstance(getOpenId(), getNodeName());
    #endif
    if(!mpCamIO || !mpCamIO->init())
    {
        MY_LOGE("hardware init fail");
        return DEAD_OBJECT;
    }
    CAM_TRACE_END();

#if SUPPORT_EIS
    CAM_TRACE_BEGIN("eis config");
    mpEIS = EisHal::CreateInstance(LOG_TAG, getOpenId());
    mpEIS->Init();
    EIS_HAL_CONFIG_DATA  config;
    config.sensorType = IHalSensorList::get()->queryType(getOpenId());
    //config.memAlignment //[AWARE] may need to modify by platform
    config.configSce = EIS_SCE_EIS;
    CAM_TRACE_END();

    sp<IImageBuffer> pEISOBuf = NULL;
    // [TODO] get pEISOBuf from EIS
    mpEIS->GetBufEis(pEISOBuf);


#endif


    //
    IHalSensor::ConfigParam sensorCfg =
    {
        (MUINT)getOpenId(),                 /* index            */
        mSensorParams.size,                 /* crop */
        mSensorParams.mode,                 /* scenarioId       */
        0,                                  /* isBypassScenario */
        1,                                  /* isContinuous     */
        MFALSE,                             /* iHDROn           */
        #ifdef USING_MTK_LDVT /*[EP_TEMP]*/
        1,
        #else
        mSensorParams.fps,                  /* framerate        */
        #endif
        0,                                  /* two pixel on     */
        0,                                  /* debugmode        */
    };

    vector<IHalSensor::ConfigParam> vSensorCfg;
    vSensorCfg.push_back(sensorCfg);

    //
    vector<portInfo> vPortInfo;
    if (mvOutImage_full.size() != 0) //[TODO] || (mOutImage_combo != NULL)
    {
        portInfo OutPort(
                PORT_IMGO,
                (EImageFormat)mvOutImage_full[0]->getImgFormat(),
                mvOutImage_full[0]->getImgSize(),
                MRect(MPoint(0,0), mSensorParams.size),
                mvOutImage_full[0]->getBufPlanes().itemAt(0).rowStrideInBytes,
                0, //pPortCfg->mStrideInByte[1],
                0, //pPortCfg->mStrideInByte[2],
                0, // pureraw
                MTRUE);              //packed

        vPortInfo.push_back(OutPort);
        mConfigPort |= CONFIG_PORT_IMGO;
    }


    if (mOutImage_resizer != NULL)
    {
        portInfo OutPort(
                PORT_RRZO,
                (EImageFormat)mOutImage_resizer->getImgFormat(),
                mOutImage_resizer->getImgSize(),
                MRect(MPoint(0,0), mSensorParams.size),
                mOutImage_resizer->getBufPlanes().itemAt(0).rowStrideInBytes,
                0, //pPortCfg->mStrideInByte[1],
                0, //pPortCfg->mStrideInByte[2],
                0, // pureraw
                MTRUE);              //packed

        vPortInfo.push_back(OutPort);
        mConfigPort |= CONFIG_PORT_RRZO;
    }

    #if SUPPORT_EIS
    if (pEISOBuf != NULL)
    {
        portInfo OutPort(
                PORT_EISO,
                (EImageFormat)pEISOBuf->getImgFormat(),
                pEISOBuf->getImgSize(),
                MRect(MPoint(0,0),  pEISOBuf->getImgSize()),
                pEISOBuf->getBufStridesInBytes(0),
                0, //pPortCfg->mStrideInByte[1],
                0, //pPortCfg->mStrideInByte[2],
                0, // pureraw
                MTRUE);              //packed

        vPortInfo.push_back(OutPort);
        mConfigPort |= CONFIG_PORT_EISO;
    }
    #endif

    MBOOL bDynamicRawType = MTRUE;  // true:[AUTO] ; false:[OFF]
    QInitParam halCamIOinitParam(
               0,                           /*sensor test pattern */
               10,                          /* bit depth*/
               vSensorCfg,
               vPortInfo,
               bDynamicRawType);

    halCamIOinitParam.m_DropCB = doNotifyDropframe;
    halCamIOinitParam.m_returnCookie = this;

    //
    mIsBinEn = false;
    CAM_TRACE_BEGIN("isp config");
    if(!mpCamIO->configPipe(halCamIOinitParam))
    {
        MY_LOGE("hardware config pipe fail");
        return BAD_VALUE;
    } else {
        MSize binInfoSize = mSensorParams.size;
        if (mpCamIO->sendCommand(ENPipeCmd_GET_BIN_INFO,
            (MINTPTR)&binInfoSize.w, (MINTPTR)&binInfoSize.h, NULL)) {
            if (binInfoSize.w < mSensorParams.size.w ||
                binInfoSize.h < mSensorParams.size.h) {
                mIsBinEn = true;
            }
        }
        MY_LOGI("BinSize:%dx%d BinEn:%d",
            binInfoSize.w, binInfoSize.h, mIsBinEn);
    }
    CAM_TRACE_END();

#if SUPPORT_3A
    CAM_TRACE_BEGIN("3a createinstance");
    mp3A = IHal3AImp_T::createInstance(IHal3A::E_Camera_3, getOpenId(), getNodeName());
    mp3A->setSensorMode(mSensorParams.mode);
    CAM_TRACE_END();
#endif

   #if SUPPORT_EIS
    mpEIS->ConfigEis(EIS_PASS_1, config);
    #endif


    #if SUPPORT_3A
    CAM_TRACE_BEGIN("3A start");
    if (mp3A) {
        mp3A->attachCb(IHal3ACb::eID_NOTIFY_3APROC_FINISH, this);
        mp3A->attachCb(IHal3ACb::eID_NOTIFY_CURR_RESULT, this);
        mp3A->start();
        //m3AProcessedDepth = mp3A->getCapacity();
    }
    CAM_TRACE_END();
    #endif


    //register 3 real frames and 3 dummy frames
    //[TODO] in case that delay frame is above 3 but memeory has only 3, pending aquirefromPool
    CAM_TRACE_BEGIN("create node");
    {
        createNode(NULL, &mProcessingQueue, &mProcessingQueueLock,
                             &mControls3AList, &mControls3AListLock);
        hardwareOps_enque(mProcessingQueue.editItemAt(mProcessingQueue.size()-1));
        // Due to pipeline latency, delay frame should be above 3
        // if delay frame is more than 3, add node to mProcessingQueue here.
        for (int i = 0; i < mDelayframe - mNumHardwareBuffer; i++) {
            createNode(&mControls3AList, &mControls3AListLock);
        }
    }
    CAM_TRACE_END();
    //
    CAM_TRACE_BEGIN("isp start");
    #if 1
    if(!mpCamIO->start()) {
        MY_LOGE("hardware start fail");
        return BAD_VALUE;
    }
    #endif
    CAM_TRACE_END();

    {
        Mutex::Autolock _l(mThreadLock);
        setReady(MTRUE);
        mThreadCond.broadcast();
    }

    FUNCTION_OUT_P1;

    return OK;
#else
    return OK;
#endif

}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
generateAppMeta(sp<IPipelineFrame> const &request, MetaSet_T const &result3A,
                QBufInfo const &deqBuf, IMetadata &appMetadata)
{
    //[3A/Flash/sensor section]
    appMetadata = result3A.appMeta;

    //[request section]
    // android.request.frameCount
    {
        IMetadata::IEntry entry(MTK_REQUEST_FRAME_COUNT);
        entry.push_back( request->getFrameNo(), Type2Type< MINT32 >());
        appMetadata.update(MTK_REQUEST_FRAME_COUNT, entry);
    }
    // android.request.metadataMode
    {
        IMetadata::IEntry entry(MTK_REQUEST_METADATA_MODE);
        entry.push_back(MTK_REQUEST_METADATA_MODE_FULL, Type2Type< MUINT8 >());
        appMetadata.update(MTK_REQUEST_METADATA_MODE, entry);
    }

    //[sensor section]
    // android.sensor.timestamp
    {
        MINT64 frame_duration = 0; //IMetadata::IEntry entry(MTK_SENSOR_FRAME_DURATION);
                                   //should get from control.
        MINT64 timestamp = deqBuf.mvOut[0].mMetaData.mTimeStamp - frame_duration;
        IMetadata::IEntry entry(MTK_SENSOR_TIMESTAMP);
        entry.push_back(timestamp, Type2Type< MINT64 >());
        appMetadata.update(MTK_SENSOR_TIMESTAMP, entry);
    }

    //[sensor section]
    // android.sensor.rollingshutterskew
    // [TODO] should query from sensor
    {
        IMetadata::IEntry entry(MTK_SENSOR_ROLLING_SHUTTER_SKEW);
        entry.push_back(33000000, Type2Type< MINT64 >());
        appMetadata.update(MTK_SENSOR_ROLLING_SHUTTER_SKEW, entry);
    }


}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
P1NodeImp::
generateHalMeta(MetaSet_T const &result3A, QBufInfo const &deqBuf, IMetadata const &resultEIS, IMetadata const &inHalMetadata, IMetadata &halMetadata)
{
    if (deqBuf.mvOut.size() == 0) {
        MY_LOGE("deqBuf is empty");
        return;
    }

    //3a tuning
    halMetadata = result3A.halMeta;

    //eis
    halMetadata += resultEIS;

    // in hal meta
    halMetadata += inHalMetadata;

    //rrzo
    for (size_t i = 0; i < deqBuf.mvOut.size(); i++) {
        if (deqBuf.mvOut[i].mPortID.index == PORT_RRZO.index) {
            //crop region
            {
                MRect crop = deqBuf.mvOut[i].mMetaData.mCrop_s;
                if (mIsBinEn) {
                    BIN_REVERT(crop.p.x);
                    BIN_REVERT(crop.p.y);
                    BIN_REVERT(crop.s.w);
                    BIN_REVERT(crop.s.h);
                }
                IMetadata::IEntry entry(MTK_P1NODE_SCALAR_CROP_REGION);
                entry.push_back(crop, Type2Type< MRect >());
                halMetadata.update(MTK_P1NODE_SCALAR_CROP_REGION, entry);
            }
            //
            {
                IMetadata::IEntry entry(MTK_P1NODE_DMA_CROP_REGION);
                entry.push_back(deqBuf.mvOut[i].mMetaData.mCrop_d, Type2Type< MRect >());
                halMetadata.update(MTK_P1NODE_DMA_CROP_REGION, entry);
            }
            //
            {
                IMetadata::IEntry entry(MTK_P1NODE_RESIZER_SIZE);
                entry.push_back(deqBuf.mvOut[i].mMetaData.mDstSize, Type2Type< MSize >());
                halMetadata.update(MTK_P1NODE_RESIZER_SIZE, entry);
            }
            /*
            MY_LOGD("[CropInfo] CropS(%d, %d, %dx%d) "
                "CropD(%d, %d, %dx%d) DstSize(%dx%d)",
                deqBuf.mvOut[i].mMetaData.mCrop_s.leftTop().x,
                deqBuf.mvOut[i].mMetaData.mCrop_s.leftTop().y,
                deqBuf.mvOut[i].mMetaData.mCrop_s.size().w,
                deqBuf.mvOut[i].mMetaData.mCrop_s.size().h,
                deqBuf.mvOut[i].mMetaData.mCrop_d.leftTop().x,
                deqBuf.mvOut[i].mMetaData.mCrop_d.leftTop().y,
                deqBuf.mvOut[i].mMetaData.mCrop_d.size().w,
                deqBuf.mvOut[i].mMetaData.mCrop_d.size().h,
                deqBuf.mvOut[i].mMetaData.mDstSize.w,
                deqBuf.mvOut[i].mMetaData.mDstSize.h);
            */
        } else {
            continue;
        }
    }

    // stereo pipeline needs timestamp to check frame synchronization
    // use hal meta to pass this information
    MINT64 timestamp = deqBuf.mvOut[0].mMetaData.mTimeStamp;
    MY_LOGD1("MTK_P1NODE_SENSOR_TIMESTAMP=%lld", timestamp);
    IMetadata::IEntry entry(MTK_P1NODE_SENSOR_TIMESTAMP);
    entry.push_back(timestamp, Type2Type< MINT64 >());
    halMetadata.update(MTK_P1NODE_SENSOR_TIMESTAMP, entry);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
setupNodeBufInfo(
    QueNode_T &node,
    QBufInfo &info
)
{
    FUNCTION_IN_P1;
#if SUPPORT_ISP
    MUINT32 out = 0;
    //
    sp<IImageStreamBufferPoolT> pBufPool = NULL;
    sp<IImageStreamInfo> pImgStreamInfo = NULL;
    sp<IImageStreamBuffer> pImgStreamBuf = NULL;
    sp<IImageBuffer> pImgBuf = NULL;
    //
    NSCam::NSIoPipe::PortID portID = NSCam::NSIoPipe::PortID();
    MSize dstSize = MSize(0, 0);
    MRect cropRect = MRect(MPoint(0, 0), MSize(0, 0));
    MUINT32 rawOutFmt = 0;
    //
    sp<IImageStreamBuffer> * ppOutImgStreamBuf = NULL;
    sp<IImageBuffer> * ppOutImgBuf = NULL;
    //
    if ((node.reqType == REQ_TYPE_UNKNOWN) ||
        (node.reqType == REQ_TYPE_REDO)) {
        MY_LOGI("(%d) no need to setup type(%d)", node.magicNum, node.reqType);
        return OK;
    }
    //
    for (out = 0; out < REQ_OUT_MAX; out++) {
        if (!(IS_OUT(out, node.reqOutSet))) {
            continue;
        }
        pBufPool = NULL;
        pImgStreamInfo = NULL;
        pImgStreamBuf = NULL;
        pImgBuf = NULL;
        switch (out) {
            case REQ_OUT_RESIZER:
            case REQ_OUT_RESIZER_STUFF:
                if (mOutImage_resizer != NULL) {
                    pImgStreamInfo = mOutImage_resizer;
                }
                if (pImgStreamInfo == NULL){
                    MY_LOGE("(%d) no image stream info - resizer",
                        node.magicNum);
                    return BAD_VALUE;
                }
                portID = PORT_RRZO;
                dstSize = node.dstSize_resizer;
                cropRect = node.cropRect_resizer;
                rawOutFmt = (EPipe_PROCESSED_RAW);
                            //(MUINT32)((node.reqOutSet & REQ_OUT_FULL_PURE) ?
                            //(EPipe_PROCESSED_RAW) : (EPipe_PURE_RAW));
                ppOutImgStreamBuf = &(node.halFrame_resizer);
                ppOutImgBuf = &(node.img_resizer);
                if (node.reqType == REQ_TYPE_INITIAL ||
                    out == REQ_OUT_RESIZER_STUFF) {
                    if (mpStreamPool_resizer != NULL) {
                        pBufPool = mpStreamPool_resizer;
                    } else { // use stuff buffer with height:1
                        dstSize.h = P1_STUFF_BUF_HEIGHT;
                    }
                    cropRect.s = dstSize;
                }
                break;

            case REQ_OUT_FULL:
            case REQ_OUT_FULL_PURE:
            case REQ_OUT_FULL_COMBO:
                // [TODO] for combo raw
                // [TODO] need to check some value of stream info before use
            case REQ_OUT_FULL_STUFF:
                if  (mvOutImage_full.size() > 0) {
                    MUINT32 cnt = 0;
                    for (cnt = 0; cnt < mvOutImage_full.size(); cnt++) {
                        if (mvOutImage_full[cnt] != NULL) {
                            pImgStreamInfo = mvOutImage_full[cnt];
                            break;
                        }
                    }
                }
                if (pImgStreamInfo == NULL){
                    MY_LOGE("(%d) no image stream info - full",
                        node.magicNum);
                    return BAD_VALUE;
                }
                portID = PORT_IMGO;
                dstSize = node.dstSize_full;
                cropRect = node.cropRect_full;
                rawOutFmt = (MUINT32)((out == REQ_OUT_FULL_PURE) ?
                            (EPipe_PURE_RAW) : (EPipe_PROCESSED_RAW));
                ppOutImgStreamBuf = &(node.halFrame_full);
                ppOutImgBuf = &(node.img_full);
                if (node.reqType == REQ_TYPE_INITIAL ||
                    out == REQ_OUT_FULL_STUFF) {
                    if (mpStreamPool_full != NULL) {
                        pBufPool = mpStreamPool_full;
                    } else { // use stuff buffer with height:1
                        dstSize.h = P1_STUFF_BUF_HEIGHT;
                    }
                    cropRect.s = dstSize;
                }
                break;

            case REQ_OUT_MAX:
            default:
                continue;
        };
        //
        if (node.reqType == REQ_TYPE_INITIAL ||
            (out == REQ_OUT_FULL_STUFF || out == REQ_OUT_RESIZER_STUFF)) {
            MERROR err = OK;
            if (pBufPool != NULL) {
                err = pBufPool->acquireFromPool(
                    getNodeName(), pImgStreamBuf, ::s2ns(300));
                if (err != OK) {
                    if(err == TIMED_OUT) {
                        MY_LOGW("(%d) acquire timeout", node.magicNum);
                    } else {
                        MY_LOGE("(%d) acquire failed", node.magicNum);
                    }
                    pBufPool->dumpPool();
                    return BAD_VALUE;
                }
                lockImageBuffer(pImgStreamBuf, pImgBuf);
            } else if (pImgStreamInfo != NULL) {
                err = createStuffBuffer(pImgBuf, pImgStreamInfo, dstSize.h);
                if (err != OK) {
                    MY_LOGE("(%d) create stuff buffer with stream info failed",
                        node.magicNum);
                    return BAD_VALUE;
                }
            } else {
                char const* szName = "Hal:Image:P1:INITraw";
                MINT format = pImgStreamInfo->getImgFormat();
                MUINT32 stride = pImgStreamInfo->
                    getBufPlanes().itemAt(0).rowStrideInBytes;
                err = createStuffBuffer(pImgBuf,
                        szName, format, dstSize, stride);
                if (err != OK) {
                    MY_LOGE("(%d) create stuff buffer failed", node.magicNum);
                    return BAD_VALUE;
                }
            }
            *(ppOutImgStreamBuf) = pImgStreamBuf;
            *(ppOutImgBuf) = pImgBuf;
            MY_LOGD2("[BUF][%s] Size(%d) Stride(%d) PA(%p) VA(%p)",
                ((out == REQ_OUT_RESIZER_STUFF) ? "RRZO" : "IMGO"),
                pImgBuf->getBufSizeInBytes(0),
                pImgBuf->getBufStridesInBytes(0),
                (void*)pImgBuf->getBufPA(0),
                (void*)pImgBuf->getBufVA(0));
        } else  { // REQ_TYPE_NORMAL
            if (node.appFrame == NULL) {
                MY_LOGE("(%d) lock image buffer with NULL pipeline frame",
                    node.magicNum);
                return BAD_VALUE;
            }
            if (OK != lockImageBuffer(node.appFrame,
                    pImgStreamInfo->getStreamId(), pImgStreamBuf, pImgBuf)) {
                MY_LOGE("(%d) lockImageBuffer failed on StreamId=0x%X"
                    ,node.magicNum, pImgStreamInfo->getStreamId());
                return BAD_VALUE;
            } else {
                *(ppOutImgStreamBuf) = pImgStreamBuf;
                *(ppOutImgBuf) = pImgBuf;
            }
            MY_LOGD2("[BUF][%s] Size(%d) Stride(%d) PA(%p) VA(%p)",
                ((out == REQ_OUT_RESIZER) ? "RRZO" : "IMGO"),
                pImgBuf->getBufSizeInBytes(0),
                pImgBuf->getBufStridesInBytes(0),
                (void*)pImgBuf->getBufPA(0),
                (void*)pImgBuf->getBufVA(0));
        }
        //
        if (pImgBuf == NULL) {
            MY_LOGE("(%d) Can not get image buffer", node.magicNum);
            return BAD_VALUE;
        }
        //
        MY_LOGD1("(%d):(%d) [CropInfo] (%d,%d-%dx%d)(%dx%d)",
            node.magicNum, node.sofIdx,
            cropRect.p.x, cropRect.p.y, cropRect.s.w, cropRect.s.h,
            dstSize.w, dstSize.h);
        NSCam::NSIoPipe::NSCamIOPipe::BufInfo rBufInfo(
            portID,
            pImgBuf.get(),
            dstSize,
            cropRect,
            node.magicNum,
            node.sofIdx,
            rawOutFmt);
        info.mvOut.push_back(rBufInfo);
    }
    //
    {

        MSize dstSizeNone = MSize(0, 0);
        MRect cropRectNone = MRect(MPoint(0, 0), MSize(0, 0));
        // EISO
        #if SUPPORT_EIS
        if (IS_PORT(CONFIG_PORT_EISO, mConfigPort)) {
            sp<IImageBuffer> pImgBuf = NULL;
            // [TODO] get pImgBuf from EIS
            mpEIS->GetBufEis(pImgBuf);
            MY_LOGD1("GetBufEis: %p ",pImgBuf->getBufVA(0));
            NSCam::NSIoPipe::NSCamIOPipe::BufInfo rBufInfo(
                PORT_EISO,
                pImgBuf.get(),
                pImgBuf->getImgSize(),
                MRect(MPoint(0, 0), pImgBuf->getImgSize()),
                node.magicNum,
                node.sofIdx);
            info.mvOut.push_back(rBufInfo);
        }
        #endif
        // LCSO
        if (IS_PORT(CONFIG_PORT_LCSO, mConfigPort)) {
            sp<IImageBuffer> pImgBuf = NULL;
            // [TODO] get pImgBuf from LCS
            // mpLCS->GetBufLcs(pImgBuf);
            NSCam::NSIoPipe::NSCamIOPipe::BufInfo rBufInfo(
                PORT_LCSO,
                pImgBuf.get(),
                pImgBuf->getImgSize(),
                MRect(MPoint(0, 0), pImgBuf->getImgSize()),
                node.magicNum,
                node.sofIdx);
            info.mvOut.push_back(rBufInfo);
        }
    }
    //#endif
#endif
    FUNCTION_OUT_P1;
    return OK;
}



/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_enque(
    QueNode_T &node
)
{
    FUNCTION_IN_P1;
    if (!getActive()) {
        return BAD_VALUE;
    }
#if SUPPORT_ISP
    QBufInfo enBuf;
    setupNodeBufInfo(node, enBuf);
    //
    CAM_TRACE_FMT_BEGIN("enq #(%d/%d)",
        node.appFrame != NULL ? node.appFrame->getFrameNo() : 0,
        node.magicNum);
    if(!mpCamIO->enque(enBuf)) {
        MY_LOGE("enque fail");
        CAM_TRACE_FMT_END();
        return BAD_VALUE;
    }
    CAM_TRACE_FMT_END();
#endif
    FUNCTION_OUT_P1;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_deque(QBufInfo &deqBuf, MetaSet_T &result3A)
{

#if SUPPORT_ISP

    FUNCTION_IN_P1

    if (!getActive()) {
        return BAD_VALUE;
    }

    Mutex::Autolock _l(mHardwareLock);
    if (!getActive()) {
        return BAD_VALUE;
    }

    {
        // deque buffer, and handle frame and metadata
        MY_LOGD1("%ld, %f", mDequeThreadProfile.getAvgDuration(), mDequeThreadProfile.getFps());
        QPortID PortID;
        if (IS_PORT(CONFIG_PORT_IMGO, mConfigPort)) {//(mvOutImage_full.size() > 0) {
            PortID.mvPortId.push_back(PORT_IMGO);
        }
        if (IS_PORT(CONFIG_PORT_RRZO, mConfigPort)) {//(mOutImage_resizer != NULL) {
            PortID.mvPortId.push_back(PORT_RRZO);
        }
        #if SUPPORT_EIS /*[EP_TEMP]*/ // [TODO]: add EISO and LCSO
        if (IS_PORT(CONFIG_PORT_EISO, mConfigPort)) {
            PortID.mvPortId.push_back(PORT_EISO);
        }
        #endif
        #if 0
        if (IS_PORT(CONFIG_PORT_LCSO, mConfigPort)) {
            PortID.mvPortId.push_back(PORT_LCSO);
        }
        #endif
        mDequeThreadProfile.pulse_down();
        MY_LOGD2("mpCamIO->deque +++");
        if(!mpCamIO->deque(PortID, deqBuf)) {
            MY_LOGE("deque fail");
            return BAD_VALUE;
        }
        MY_LOGD2("mpCamIO->deque ---");
        //
        mDequeThreadProfile.pulse_up();
    }


    //

    static bool shouldPrint = false;
    if (shouldPrint) {
        for(size_t i = 0; i < deqBuf.mvOut.size(); i++) {
            char filename[256];
            sprintf(filename, "/data/P1_%d_%d_%d.raw", deqBuf.mvOut.at(i).mMetaData.mMagicNum_hal,
                deqBuf.mvOut.at(i).mBuffer->getImgSize().w,
                deqBuf.mvOut.at(i).mBuffer->getImgSize().h);
            NSCam::Utils::saveBufToFile(filename, (unsigned char*)deqBuf.mvOut.at(i).mBuffer->getBufVA(0), deqBuf.mvOut.at(i).mBuffer->getBufSizeInBytes(0));
            shouldPrint = false;
        }
    }
    #if 0 /*[EP_TEMP]*/
    if (mLogLevel >= 2) {
        MUINT32 num = deqBuf.mvOut.at(0).mMetaData.mMagicNum_hal;
        if (num > 0 && num < 2)
        {
            for(size_t i = 0; i < deqBuf.mvOut.size(); i++) {
                char filename[256] = {0};
                sprintf(filename, "/sdcard/raw/p1%d_%04dx%04d_%04d_%d.raw",
                    ((deqBuf.mvOut.at(i).mPortID.index == PORT_RRZO.index) ?
                    (0) : (1)),
                    (int)deqBuf.mvOut.at(i).mBuffer->getImgSize().w,
                    (int)deqBuf.mvOut.at(i).mBuffer->getImgSize().h,
                    (int)deqBuf.mvOut.at(i).mBuffer->getBufStridesInBytes(0),
                    num);
                deqBuf.mvOut.at(i).mBuffer->saveToFile(filename);
                MY_LOGI("save to file : %s", filename);
            }
        }
    }
    #endif


#if SUPPORT_3A
    if (getActive() && mp3A) {
        mp3A->notifyP1Done(deqBuf.mvOut[0].mMetaData.mMagicNum_hal);
        mp3A->get(deqBuf.mvOut[0].mMetaData.mMagicNum_hal, result3A);
    }
#endif


    FUNCTION_OUT_P1

    return OK;
#else
    return OK;
#endif

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_stop()
{
#if SUPPORT_ISP
    CAM_TRACE_CALL();

    FUNCTION_IN_P1

    //(1) handle active flag
    if (!getActive()) {
        MY_LOGD("active=%d", getActive());
        return OK;
    }
    setActive(MFALSE);

    if (getInit()) {
        MY_LOGI("mHardwareLock waiting");
        Mutex::Autolock _l(mHardwareLock);
    }
    MY_LOGI("mHardwareLock wait OK");

    //(2.1) stop EIS thread
    #if 0 //No need due to EIS skip waiting vsync
    CAM_TRACE_BEGIN("eis thread stop");
    if(mpEIS) {
        mpEIS->EisThreadStop();
    }
    CAM_TRACE_END();
    #endif

    //(2.2) stop 3A
    #if SUPPORT_3A
    CAM_TRACE_BEGIN("3A stop");
    if (mp3A) {
        mp3A->detachCb(IHal3ACb::eID_NOTIFY_3APROC_FINISH, this);
        mp3A->detachCb(IHal3ACb::eID_NOTIFY_CURR_RESULT, this);
        mp3A->stop();
    }
    CAM_TRACE_END();
    #endif

    //(2.3) stop isp
    {
        //Mutex::Autolock _l(mHardwareLock);
        CAM_TRACE_BEGIN("isp stop");
        MY_LOGD2("mpCamIO->stop +++");
        if(!mpCamIO || !mpCamIO->stop()) {
            MY_LOGE("hardware stop fail");
            return BAD_VALUE;
        }
        MY_LOGD2("mpCamIO->stop ---");
        CAM_TRACE_END();
    }

    //(3.1) destroy 3A
    #if SUPPORT_3A
    CAM_TRACE_BEGIN("3A destroy");
    if (mp3A) {
        mp3A->destroyInstance(getNodeName());
        mp3A = NULL;
    }
    CAM_TRACE_END();
    #endif

    //(3.2) destroy isp
    {
        //Mutex::Autolock _l(mHardwareLock);
        #if SUPPORT_EIS
        if(mpEIS) {
            mpEIS->Uninit();
            mpEIS->DestroyInstance(LOG_TAG);
            mpEIS = NULL;
        }
        #endif
        //
        CAM_TRACE_BEGIN("isp destroy");
        MY_LOGD2("mpCamIO->uninit +++");
        if(!mpCamIO->uninit() )
        {
            MY_LOGE("hardware uninit fail");
            return BAD_VALUE;
        }
        MY_LOGD2("mpCamIO->uninit ---");
        MY_LOGD2("mpCamIO->destroyInstance +++");
        #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly
        mpCamIO->destroyInstance("iopipeUseTM");
        #else
        mpCamIO->destroyInstance(getNodeName());
        #endif
        MY_LOGD2("mpCamIO->destroyInstance ---");
        mpCamIO = NULL;
        CAM_TRACE_END();
    }
    //
    FUNCTION_OUT_P1

    return OK;

#else
    return OK;
#endif

}


#if SUPPORT_HQC
/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_suspend()
{
    FUNCTION_IN_P1;

    if (getActive()) {
        hardwareOps_stop();
    }

    FUNCTION_OUT_P1;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
hardwareOps_resume()
{
    FUNCTION_IN_P1;

    if (!getActive()) {
        hardwareOps_start();
    }

    FUNCTION_OUT_P1;

    return OK;
}
#endif


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
lockMetadata(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId, IMetadata &rMetadata)
{
    if (pFrame == NULL) {
        //MY_LOGE("nput is NULL");
        return INVALID_OPERATION;
    }

    //  Input Meta Stream: Request
    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();
    sp<IMetaStreamBuffer> pMetaStreamBuffer = NULL;
    MERROR const err = ensureMetaBufferAvailable_(
        pFrame->getFrameNo(),
        streamId,
        rStreamBufferSet,
        pMetaStreamBuffer
    );
    if (err != OK) {
        return err;
    }
    rMetadata = *pMetaStreamBuffer->tryReadLock(getNodeName());

    return err;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnLockedMetadata(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId, MBOOL success)
{
    if (pFrame == NULL) {
        //MY_LOGE("nput is  NULL");
        return INVALID_OPERATION;
    }

    //  Input Meta Stream: Request
    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();
    sp<IMetaStreamBuffer> pMetaStreamBuffer = NULL;
    MERROR const err = ensureMetaBufferAvailable_(
        pFrame->getFrameNo(),
        streamId,
        rStreamBufferSet,
        pMetaStreamBuffer,
        MFALSE
    );
    if (err != OK) {
        return err;
    }

    if  ( 0 > mInStreamIds.indexOf(streamId) ) {
        if (success) {
            pMetaStreamBuffer->markStatus(STREAM_BUFFER_STATUS::WRITE_OK);
        } else {
            pMetaStreamBuffer->markStatus(STREAM_BUFFER_STATUS::WRITE_ERROR);
        }
    }

    //
    //  Mark this buffer as USED by this user.
    //  Mark this buffer as RELEASE by this user.
    rStreamBufferSet.markUserStatus(
        streamId, getNodeId(),
        IUsersManager::UserStatus::USED |
        IUsersManager::UserStatus::RELEASE
    );

    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnUnlockedMetadata(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId)
{
    if (pFrame == NULL) {
        //MY_LOGE("nput is NULL");
        return INVALID_OPERATION;
    }

    //  Input Meta Stream: Request
    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();
    //
    //  Mark this buffer as RELEASE by this user.
    rStreamBufferSet.markUserStatus(
        streamId, getNodeId(),
        IUsersManager::UserStatus::RELEASE
    );

    return OK;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
lock_and_returnMetadata(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId, IMetadata &metadata)
{
    if (pFrame == NULL) {
        //MY_LOGE("input is NULL");
        return INVALID_OPERATION;
    }

    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();

    //
    sp<IMetaStreamBuffer>   pMetaStreamBuffer  = NULL;
    MERROR err = ensureMetaBufferAvailable_(
        pFrame->getFrameNo(),
        streamId,
        rStreamBufferSet,
        pMetaStreamBuffer
    );
    if (err != OK) {
        return err;
    }
    IMetadata* pMetadata = pMetaStreamBuffer->tryWriteLock(getNodeName());
    if (pMetadata == NULL) {
        MY_LOGE("pMetadata == NULL");
        return BAD_VALUE;
    }

    *pMetadata = metadata;

    pMetaStreamBuffer->markStatus(STREAM_BUFFER_STATUS::WRITE_OK);
    pMetaStreamBuffer->unlock(getNodeName(), pMetadata);
    //
    //  Mark this buffer as USED by this user.
    //  Mark this buffer as RELEASE by this user.
    rStreamBufferSet.markUserStatus(
        streamId, getNodeId(),
        IUsersManager::UserStatus::USED |
        IUsersManager::UserStatus::RELEASE
    );

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
lockImageBuffer(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId,
                  sp<IImageStreamBuffer>  &pOutpImageStreamBuffer, sp<IImageBuffer> &rImageBuffer)
{
    if (pFrame == NULL) {
        //MY_LOGE("input is NULL");
        return INVALID_OPERATION;
    }

    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();

    MERROR const err = ensureImageBufferAvailable_(
        pFrame->getFrameNo(),
        streamId,
        rStreamBufferSet,
        pOutpImageStreamBuffer
    );
    if (err != OK) {
        return err;
    }

    MUINT const groupUsage = pOutpImageStreamBuffer->queryGroupUsage(getNodeId());
    sp<IImageBufferHeap>  pOutpImageBufferHeap = pOutpImageStreamBuffer->tryWriteLock(getNodeName());
    if (pOutpImageBufferHeap == NULL) {
        MY_LOGE("pOutpImageBufferHeap == NULL");
        return BAD_VALUE;
    }
    rImageBuffer = pOutpImageBufferHeap->createImageBuffer();
    rImageBuffer->lockBuf(getNodeName(), groupUsage);

    //mImageStorage.enque(pOutpImageStreamBuffer, rImageBuffer);


    MY_LOGD1("stream buffer: 0x%x, heap: 0x%x, buffer: 0x%x, usage: 0x%x",
        pOutpImageStreamBuffer.get(), pOutpImageBufferHeap.get(), rImageBuffer.get(), groupUsage);

    return err;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnLockedImageBuffer(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId, sp<IImageBuffer> const& pImageBuffer, MBOOL success)
{
    if (pFrame == NULL) {
        //MY_LOGE("input is NULL");
        return INVALID_OPERATION;
    }

    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();

    sp<IImageStreamBuffer>  pOutpImageStreamBuffer = NULL;
    MERROR const err = ensureImageBufferAvailable_(
        pFrame->getFrameNo(),
        streamId,
        rStreamBufferSet,
        pOutpImageStreamBuffer,
        MFALSE
    );
    if (err != OK) {
        return err;
    }

    sp<IImageBuffer> pOutpImageBuffer = pImageBuffer;// mImageStorage.deque(reinterpret_cast<MINTPTR>(pOutpImageStreamBuffer.get()));
    if (pOutpImageBuffer == NULL) {
        MY_LOGE("pImageBuffer == NULL");
        return BAD_VALUE;
    }

    if  ( 0 > mInStreamIds.indexOf(streamId) ) {
        if (success) {
            pOutpImageStreamBuffer->markStatus(STREAM_BUFFER_STATUS::WRITE_OK);
        } else {
            pOutpImageStreamBuffer->markStatus(STREAM_BUFFER_STATUS::WRITE_ERROR);
        }
    }

    pOutpImageBuffer->unlockBuf(getNodeName());
    pOutpImageStreamBuffer->unlock(getNodeName(), pOutpImageBuffer->getImageBufferHeap());
    //
    //  Mark this buffer as USED by this user.
    //  Mark this buffer as RELEASE by this user.
    rStreamBufferSet.markUserStatus(
        streamId, getNodeId(),
        IUsersManager::UserStatus::USED |
        IUsersManager::UserStatus::RELEASE
    );


    return OK;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnUnlockedImageBuffer(sp<IPipelineFrame> const& pFrame, StreamId_T const streamId)
{
    if (pFrame == NULL) {
        //MY_LOGE("nput is  NULL");
        return INVALID_OPERATION;
    }

    IStreamBufferSet& rStreamBufferSet  = pFrame->getStreamBufferSet();
    //  Mark this buffer as USED by this user.
    //  Mark this buffer as RELEASE by this user.
    rStreamBufferSet.markUserStatus(
        streamId, getNodeId(),
        IUsersManager::UserStatus::RELEASE
    );

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
lockImageBuffer(sp<IImageStreamBuffer> const& pStreamBuffer, sp<IImageBuffer> &pImageBuffer)
{
    if (pStreamBuffer == NULL) {
        return BAD_VALUE;
    }
    MUINT const usage = GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_HW_CAMERA_READ | GRALLOC_USAGE_HW_CAMERA_WRITE;
    sp<IImageBufferHeap>  pOutpImageBufferHeap = pStreamBuffer->tryWriteLock(getNodeName());
    if (pOutpImageBufferHeap == NULL) {
        MY_LOGE("pOutpImageBufferHeap == NULL");
        return BAD_VALUE;
    }
    pImageBuffer = pOutpImageBufferHeap->createImageBuffer();
    pImageBuffer->lockBuf(getNodeName(), usage);

    //mImageStorage.enque(pStreamBuffer, pImageBuffer);

    MY_LOGD1("streambuffer: 0x%x, heap: 0x%x, buffer: 0x%x, usage: 0x%x",
        pStreamBuffer.get(), pOutpImageBufferHeap.get(), pImageBuffer.get(), usage);

    return OK;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnLockedImageBuffer(sp<IImageBuffer> const &pImageBuffer,
                        sp<IImageStreamBuffer> const &pStreamBuffer,
                        sp<IImageStreamBufferPoolT> const &pStreamPool)
{
    if (pImageBuffer == NULL || pStreamBuffer == NULL || pStreamPool == NULL) {
        MY_LOGE_IF(pImageBuffer == NULL,  "pImageBuffer == NULL");
        MY_LOGE_IF(pStreamBuffer == NULL, "pStreamBuffer == NULL");
        MY_LOGE_IF(pStreamPool == NULL,   "pStreamPool == NULL");
        return BAD_VALUE;
    }

    pImageBuffer->unlockBuf(getNodeName());
    pStreamBuffer->unlock(getNodeName(), pImageBuffer->getImageBufferHeap());

    if(pStreamPool != NULL) {
        pStreamPool->releaseToPool(getNodeName(), pStreamBuffer);
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
P1NodeImp::
returnUnlockedImageBuffer(sp<IImageStreamBuffer> const &pStreamBuffer,
                          sp<IImageStreamBufferPoolT> const &pStreamPool)
{
    if (pStreamBuffer == NULL || pStreamPool == NULL ) {
        MY_LOGE_IF(pStreamBuffer == NULL, "pStreamBuffer == NULL");
        MY_LOGE_IF(pStreamPool == NULL,   "pStreamPool == NULL");
        return BAD_VALUE;
    }

    pStreamPool->releaseToPool(getNodeName(), pStreamBuffer);

    return OK;
}

#if (SUPPORT_SCALING_CROP)
/******************************************************************************
 *
 *****************************************************************************/
MVOID
P1NodeImp::
prepareCropInfo(
    IMetadata const* pAppMeta,
    IMetadata const* pHalMeta,
    QueNode_T& node)
{
    // set the default value
    if (mvOutImage_full.size() > 0) {
        node.dstSize_full = mvOutImage_full[0]->getImgSize();
        node.cropRect_full = MRect(MPoint(0, 0), mSensorParams.size);
    } else {
        node.dstSize_full = MSize(0, 0);
        node.cropRect_full = MRect(MPoint(0, 0), MSize(0, 0));
    }
    if (mOutImage_resizer != NULL) {
        node.dstSize_resizer = mOutImage_resizer->getImgSize();
        node.cropRect_resizer = MRect(MPoint(0, 0), mSensorParams.size);
    } else {
        node.dstSize_resizer= MSize(0, 0);
        node.cropRect_resizer = MRect(MPoint(0, 0), MSize(0, 0));
        return; // no need to check resizer
    }
    // check app meta
    if (pAppMeta != NULL) {
        // [TODO] check crop from app metadata
    }
    // check hal meta
    if (pHalMeta != NULL) {
        if( !tryGetMetadata<MRect>(pHalMeta, P1_REQ_CROP_TAG,
            node.cropRect_resizer) ) {
            MY_LOGI("Metadata exist - no P1_REQ_CROP_TAG");
        } else {
            if (node.dstSize_resizer.w > node.cropRect_resizer.s.w) {
                node.dstSize_resizer.w = node.cropRect_resizer.s.w;
            }
            if (node.dstSize_resizer.h > node.cropRect_resizer.s.h) {
                node.dstSize_resizer.h = node.cropRect_resizer.s.h;
            }
        }
    }
    // calculate resizer crop with its information
    if (mIsBinEn) {
        BIN_RESIZE(node.cropRect_resizer.p.x);
        BIN_RESIZE(node.cropRect_resizer.p.y);
        BIN_RESIZE(node.cropRect_resizer.s.w);
        BIN_RESIZE(node.cropRect_resizer.s.h);
    }
    // check start.x
    {
        NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo info;
        NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
            NSCam::NSIoPipe::PORT_RRZO.index,
            NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_CROP_START_X,
            (EImageFormat)(mOutImage_resizer->getImgFormat()),
            node.cropRect_resizer.p.x, info);
        node.cropRect_resizer.p.x = info.crop_x;
    }
    // check size.w
    {
        NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo info;
        NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
            NSCam::NSIoPipe::PORT_RRZO.index,
            NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_X_PIX|
            NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_STRIDE_BYTE,
            (EImageFormat)(mOutImage_resizer->getImgFormat()),
            node.cropRect_resizer.s.w, info);
        node.cropRect_resizer.s.w = info.x_pix;
    }
    // no scale up
    if (node.dstSize_resizer.w > node.cropRect_resizer.s.w) {
        node.dstSize_resizer.w = node.cropRect_resizer.s.w;
    }
    if (node.dstSize_resizer.h > node.cropRect_resizer.s.h) {
        node.dstSize_resizer.h = node.cropRect_resizer.s.h;
    }
    //
    MY_LOGD1("Crop Info F(%d,%d,%dx%d)(%dx%d) R(%d,%d,%dx%d)(%dx%d)",
                node.cropRect_full.p.x, node.cropRect_full.p.y,
                node.cropRect_full.s.w, node.cropRect_full.s.h,
                node.dstSize_full.w, node.dstSize_full.h,
                node.cropRect_resizer.p.x, node.cropRect_resizer.p.y,
                node.cropRect_resizer.s.w, node.cropRect_resizer.s.h,
                node.dstSize_resizer.w, node.dstSize_resizer.h);
}

/******************************************************************************
 *
 *****************************************************************************/
MVOID
P1NodeImp::
prepareCropInfo(IMetadata* pMetadata,
           QueNode_T& node)
{
    if (mvOutImage_full.size() > 0) {
        node.dstSize_full = mvOutImage_full[0]->getImgSize();
        node.cropRect_full = MRect(MPoint(0, 0), mSensorParams.size);
    } else {
        node.dstSize_full = MSize(0, 0);
        node.cropRect_full = MRect(MPoint(0, 0), MSize(0, 0));
    }
    if (mOutImage_resizer != NULL) {
        node.dstSize_resizer = mOutImage_resizer->getImgSize();
        node.cropRect_resizer = MRect(MPoint(0, 0), mSensorParams.size);
        if (mIsBinEn) {
            BIN_RESIZE(node.cropRect_resizer.p.x);
            BIN_RESIZE(node.cropRect_resizer.p.y);
            BIN_RESIZE(node.cropRect_resizer.s.w);
            BIN_RESIZE(node.cropRect_resizer.s.h);
            BIN_RESIZE(node.dstSize_resizer.w);
            BIN_RESIZE(node.dstSize_resizer.h);
        }
    } else {
        node.dstSize_resizer= MSize(0, 0);
        node.cropRect_resizer = MRect(MPoint(0, 0), MSize(0, 0));
    }
    //
    if (pMetadata != NULL) {
        MRect cropRect_metadata;    // get from metadata
        MRect cropRect_control;     // set to node

        if( !tryGetMetadata<MRect>(pMetadata, MTK_SCALER_CROP_REGION,
            cropRect_metadata) ) {
            MY_LOGI("Metadata exist - no MTK_SCALER_CROP_REGION, "
                "crop size set to full(%dx%d) resizer(%dx%d)",
                node.dstSize_full.w, node.dstSize_full.h,
                node.dstSize_resizer.w, node.dstSize_resizer.h);
        } else {
            if (mIsBinEn) {
                BIN_RESIZE(cropRect_metadata.p.x);
                BIN_RESIZE(cropRect_metadata.p.y);
                BIN_RESIZE(cropRect_metadata.s.w);
                BIN_RESIZE(cropRect_metadata.s.h);
            }
            simpleTransform tranActive2Sensor = simpleTransform(
                    MPoint(0,0), mActiveArray.size(), mSensorParams.size);
            cropRect_control.p = transform(tranActive2Sensor,
                                            cropRect_metadata.leftTop());
            cropRect_control.s = transform(tranActive2Sensor,
                                            cropRect_metadata.size());

            #if SUPPORT_EIS
            if (mpEIS)
            {
                MBOOL isEisOn = false;
                MRect const requestRect = MRect(cropRect_control);
                MSize const sensorSize = MSize(mSensorParams.size);
                MPoint const requestCenter=
                    MPoint((requestRect.p.x + (requestRect.s.w >> 1)),
                            (requestRect.p.y + (requestRect.s.h >> 1)));
                isEisOn = isEISOn(pMetadata);
                cropRect_control.s = mpEIS->QueryMinSize(isEisOn, sensorSize,
                                                        requestRect.size());

                if (cropRect_control.s.w != requestRect.size().w)
                {
                    MSize::value_type half_len =
                        ((cropRect_control.s.w + 1) >> 1);
                    if (requestCenter.x < half_len) {
                        cropRect_control.p.x = 0;
                    } else if ((requestCenter.x + half_len) > sensorSize.w) {
                        cropRect_control.p.x = sensorSize.w -
                                                cropRect_control.s.w;
                    } else {
                        cropRect_control.p.x = requestCenter.x - half_len;
                    }
                }
                if (cropRect_control.s.w != requestRect.size().w)
                {
                    MSize::value_type half_len =
                        ((cropRect_control.s.h + 1) >> 1);
                    if (requestCenter.y < half_len) {
                        cropRect_control.p.y = 0;
                    } else if ((requestCenter.y + half_len) > sensorSize.h) {
                        cropRect_control.p.y = sensorSize.h -
                                                cropRect_control.s.h;
                    } else {
                        cropRect_control.p.y = requestCenter.y - half_len;
                    }
                }
            }
            #endif
            /*
            MY_LOGD("[CropInfo] metadata(%d, %d, %dx%d) "
                "control(%d, %d, %dx%d) "
                "active(%d, %d, %dx%d) "
                "sensor(%dx%d)",
                cropRect_metadata.leftTop().x,
                cropRect_metadata.leftTop().y,
                cropRect_metadata.size().w, cropRect_metadata.size().h,
                cropRect_control.leftTop().x,
                cropRect_control.leftTop().y,
                cropRect_control.size().w, cropRect_control.size().h,
                mActiveArray.leftTop().x,
                mActiveArray.leftTop().y,
                mActiveArray.size().w, mActiveArray.size().h,
                mSensorParams.size.w, mSensorParams.size.h);
            */
            // TODO: check more case about crop region
            if ((cropRect_control.size().w < 0) ||
                (cropRect_control.size().h < 0) ||
                (cropRect_control.leftTop().x < 0) ||
                (cropRect_control.leftTop().y < 0) ||
                (cropRect_control.leftTop().x >= mSensorParams.size.w) ||
                (cropRect_control.leftTop().y >= mSensorParams.size.h)) {
                MY_LOGW("Metadata exist - invalid cropRect_control"
                    "(%d, %d, %dx%d) sensor(%dx%d)",
                    cropRect_control.leftTop().x,
                    cropRect_control.leftTop().y,
                    cropRect_control.size().w, cropRect_control.size().h,
                    mSensorParams.size.w, mSensorParams.size.h);
                return;
            }
            if ((cropRect_control.p.x + cropRect_control.s.w) >
                mSensorParams.size.w) {
                cropRect_control.s.w = mSensorParams.size.w -
                                        cropRect_control.p.x;
            }
            if ((cropRect_control.p.y + cropRect_control.s.h) >
                mSensorParams.size.h) {
                cropRect_control.s.h = mSensorParams.size.h -
                                        cropRect_control.p.y;
            }
            // calculate the crop region validity
            calculateCropInfoFull(mSensorParams.pixelMode,
                                    mSensorParams.size,
                                    (mvOutImage_full.size() > 0) ?
                                    (mvOutImage_full[0]->getImgSize()) :
                                    (MSize(0, 0)),
                                    cropRect_control,
                                    node.cropRect_full,
                                    node.dstSize_full,
                                    mLogLevel);
            calculateCropInfoResizer(mSensorParams.pixelMode,
                                    (mOutImage_resizer->getImgFormat()),
                                    mSensorParams.size,
                                    (mOutImage_resizer != NULL) ?
                                    (mOutImage_resizer->getImgSize()) :
                                    (MSize(0, 0)),
                                    cropRect_control,
                                    node.cropRect_resizer,
                                    node.dstSize_resizer,
                                    mLogLevel);
        }
    }
    MY_LOGD1("Crop-Info F(%d,%d,%dx%d)(%dx%d) R(%d,%d,%dx%d)(%dx%d)",
            node.cropRect_full.p.x, node.cropRect_full.p.y,
            node.cropRect_full.s.w, node.cropRect_full.s.h,
            node.dstSize_full.w, node.dstSize_full.h,
            node.cropRect_resizer.p.x, node.cropRect_resizer.p.y,
            node.cropRect_resizer.s.w, node.cropRect_resizer.s.h,
            node.dstSize_resizer.w, node.dstSize_resizer.h);
}
#endif


/******************************************************************************
 *
 *****************************************************************************/
MVOID
P1NodeImp::
createNode(sp<IPipelineFrame> appframe,
           Que_T *Queue,
           Mutex *QueLock,
           List<MetaSet_T> *list,
           Mutex *listLock)
{
    //create queue node
    MUINT32 newNum = get_and_increase_magicnum();
    MetaSet_T metaInfo;
    IMetadata* pAppMeta = NULL;
    IMetadata* pHalMeta = NULL;
    MUINT8 isHQC = 0;
    //
    MUINT32 nodeOut = REQ_SET_NONE;
    MUINT32 nodeType = REQ_TYPE_UNKNOWN;
    MBOOL isRedoRequest = false;
    IPipelineFrame::InfoIOMapSet rIOMapSet;
    //
    if (appframe != NULL) {
        if(OK != appframe->queryInfoIOMapSet(getNodeId(), rIOMapSet)) {
            MY_LOGE("queryInfoIOMap failed");
            return;
        }
        //
        // do some check
        IPipelineFrame::ImageInfoIOMapSet& imageIOMapSet =
                                                rIOMapSet.mImageInfoIOMapSet;
        if(!imageIOMapSet.size()) {
            MY_LOGW("no imageIOMap in frame");
            return;
        }
        //
        for (size_t i = 0; i < imageIOMapSet.size(); i++) {
            IPipelineFrame::ImageInfoIOMap const& imageIOMap = imageIOMapSet[i];
            if (imageIOMap.vIn.size() > 1) {
                isRedoRequest = true;
                break;
            } else {
                for (size_t j = 0; j < imageIOMap.vOut.size(); j++) {
                    StreamId_T const streamId = imageIOMap.vOut.keyAt(j);
                    if (mOutImage_resizer != NULL) {
                        if (streamId == mOutImage_resizer->getStreamId()) {
                            nodeOut |= REQ_SET(REQ_OUT_RESIZER);
                        }
                    }
                    for (size_t k = 0; k < mvOutImage_full.size(); k++) {
                        if (streamId == mvOutImage_full[k]->getStreamId()) {
                            nodeOut |= REQ_SET(REQ_OUT_FULL);
                        }
                    }
                    /*
                    if (mOutImage_combo != NULL) {
                        if (streamId == mOutImage_combo->getStreamId()) {
                            nodeOut |= REQ_SET(REQ_OUT_FULL_COMBO);
                        }
                    }
                    */
                }
            }
        }
    }

    if (isRedoRequest) {
        nodeType = REQ_TYPE_REDO;
        nodeOut = REQ_SET_NONE;
    } else if (list!=NULL) { // create list for 3A
        nodeType = (appframe == NULL) ? REQ_TYPE_INITIAL : REQ_TYPE_NORMAL;
        //fill in App metadata
        if (appframe != NULL) {
            if (mInAppMeta != NULL) {
                if (OK == lockMetadata(
                    appframe, mInAppMeta->getStreamId(), metaInfo.appMeta)) {
                    pAppMeta = &(metaInfo.appMeta);
                } else {
                    MY_LOGI("can not lock the app metadata");
                }
            }
            if (mInHalMeta != NULL) {
                if (OK == lockMetadata(
                    appframe, mInHalMeta->getStreamId(), metaInfo.halMeta)) {
                    pHalMeta = &(metaInfo.halMeta);
                } else {
                    MY_LOGI("can not lock the hal metadata");
                }
            }
        }
        //fill in hal metadata
        IMetadata::IEntry entry1(MTK_P1NODE_PROCESSOR_MAGICNUM);
        entry1.push_back(newNum, Type2Type< MINT32 >());
        metaInfo.halMeta.update(MTK_P1NODE_PROCESSOR_MAGICNUM, entry1);

        IMetadata::IEntry entry2(MTK_HAL_REQUEST_REPEAT);
        entry2.push_back(0, Type2Type< MUINT8 >());
        metaInfo.halMeta.update(MTK_HAL_REQUEST_REPEAT, entry2);


        MUINT8 isdummy =  appframe == NULL ? 1 : 0;
        IMetadata::IEntry entry3(MTK_HAL_REQUEST_DUMMY);
        entry3.push_back(isdummy, Type2Type< MUINT8 >());
        metaInfo.halMeta.update(MTK_HAL_REQUEST_DUMMY, entry3);

        #if SUPPORT_HQC
        if (isHQC > 0) {
            IMetadata::IEntry entryHQC(MTK_HAL_REQUEST_HIGH_QUALITY_CAP);
            entryHQC.push_back(isHQC, Type2Type< MUINT8 >());
            metaInfo.halMeta.update(MTK_HAL_REQUEST_HIGH_QUALITY_CAP, entryHQC);
        }
        #endif

        if(listLock != NULL) {
            Mutex::Autolock _l(*listLock);
            (*list).push_back(metaInfo);
        } else {
            (*list).push_back(metaInfo);
        }

    }

    if (nodeType == REQ_TYPE_NORMAL || nodeType == REQ_TYPE_INITIAL) {
        if (IS_PORT(CONFIG_PORT_IMGO, mConfigPort)
            && (0 == (IS_OUT(REQ_OUT_FULL, nodeOut) ||
                    IS_OUT(REQ_OUT_FULL_PURE, nodeOut) ||
                    IS_OUT(REQ_OUT_FULL_COMBO, nodeOut))
                )
            ) {
                nodeOut |= REQ_SET(REQ_OUT_FULL_STUFF);
            }
        if (IS_PORT(CONFIG_PORT_RRZO, mConfigPort)
            && (0 == IS_OUT(REQ_OUT_RESIZER, nodeOut))) {
                nodeOut |= REQ_SET(REQ_OUT_RESIZER_STUFF);
            }
    }
    MY_LOGD("node type(0x%X) out(0x%X)", nodeType, nodeOut);
    if (nodeType == REQ_TYPE_UNKNOWN) {
        MY_LOGW("request type UNKNOWN");
    } else if (nodeType == REQ_TYPE_REDO) {
        MY_LOGD("request type REDO");
        /*
        Mutex::Autolock _l(mRedoQueueLock);
        QueNode_T node;
        node.magicNum = newNum;
        node.sofIdx = P1SOFIDX_INIT_VAL;
        node.appFrame = appframe;
        node.reqOut = nodeOut;
        node.reqType = nodeType;
        mRedoQueue.push_back(node);
        */
    } else if ((nodeType == REQ_TYPE_NORMAL) && (nodeOut == REQ_SET_NONE)) {
        MY_LOGW("request out NONE");
    } else if(Queue!=NULL) {
        // node type is REQ_TYPE_NORMAL-with-output or REQ_TYPE_PADDING
        Mutex::Autolock _l(*QueLock);
        QueNode_T node;
        node.magicNum = newNum;
        node.sofIdx = P1SOFIDX_INIT_VAL;
        node.appFrame = appframe;
        node.reqType = nodeType;
        node.reqOutSet = nodeOut;
        //
        #if (SUPPORT_SCALING_CROP)
        prepareCropInfo(pAppMeta, node);
        //prepareCropInfo(pAppMeta, pHalMeta, node);
        #endif
        (*Queue).push_back(node);
    }
    //
    if (appframe != NULL) {
        MY_LOGD1("[New Request] frameNo: %u, magic Num: %d",
            appframe->getFrameNo(), newNum);
    } else {
        MY_LOGD1("[New Request: dummy] magic Num: %d",
            newNum);
    }

    MY_LOGD1("#%d mControls3AList size(%d)", __LINE__, mControls3AList.size());
    return;
}


/******************************************************************************
 *
 *****************************************************************************/
MVOID
P1NodeImp::
createNode(List<MetaSet_T> *list,
           Mutex *listLock)
{
    if (list == NULL) {
        MY_LOGW("list not exist");
        return;
    }
    MUINT32 newNum = get_and_increase_magicnum();
    MetaSet_T metaInfo;
    //fill in hal metadata
    IMetadata::IEntry entry1(MTK_P1NODE_PROCESSOR_MAGICNUM);
    entry1.push_back(newNum, Type2Type< MINT32 >());
    metaInfo.halMeta.update(MTK_P1NODE_PROCESSOR_MAGICNUM, entry1);
    //
    IMetadata::IEntry entry2(MTK_HAL_REQUEST_REPEAT);
    entry2.push_back(0, Type2Type< MUINT8 >());
    metaInfo.halMeta.update(MTK_HAL_REQUEST_REPEAT, entry2);
    //
    IMetadata::IEntry entry3(MTK_HAL_REQUEST_DUMMY);
    entry3.push_back(1, Type2Type< MUINT8 >());
    metaInfo.halMeta.update(MTK_HAL_REQUEST_DUMMY, entry3);

    if(listLock != NULL) {
        Mutex::Autolock _l(*listLock);
        (*list).push_back(metaInfo);
    } else {
        (*list).push_back(metaInfo);
    }

    MY_LOGD1("[New Request: padding] magic Num: %d", newNum);
    MY_LOGD1("#%d mControls3AList size(%d)", __LINE__, mControls3AList.size());
    return;
}


/******************************************************************************
 *
 *****************************************************************************/
MVOID
P1NodeImp::
createNode(Que_T &Queue)
{
    MUINT32 newNum = get_and_increase_magicnum();
    {
        QueNode_T node;
        node.magicNum = newNum;
        node.sofIdx = P1SOFIDX_INIT_VAL;
        node.appFrame = NULL;
        Queue.push_back(node);
    }

    MY_LOGD1("[New Request: dummy] magic Num: %d", newNum);
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<P1Node>
P1Node::
createInstance()
{
    return new P1NodeImp();

}

