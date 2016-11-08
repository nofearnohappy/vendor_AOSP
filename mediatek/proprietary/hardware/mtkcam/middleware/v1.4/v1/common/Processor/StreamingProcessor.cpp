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

#define LOG_TAG "MtkCam/StreamingProcessor"
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>
//
#include "MyUtils.h"
#include <v1/Processor/StreamingProcessor.h>
//
using namespace android;
using namespace NSCam;
using namespace NSCam::v1;

/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)
//
#define FUNCTION_IN        MY_LOGD("+")
#define FUNCTION_OUT       MY_LOGD("-")

/******************************************************************************
 *
 ******************************************************************************/
template <typename T>
inline MBOOL
tryGetMetadata(
    IMetadata* pMetadata,
    MUINT32 const tag,
    T & rVal
)
{
    if( pMetadata == NULL ) {
        MY_LOGW("pMetadata == NULL");
        return MFALSE;
    }

    IMetadata::IEntry entry = pMetadata->entryFor(tag);
    if( !entry.isEmpty() ) {
        rVal = entry.itemAt(0, Type2Type<T>());
        return MTRUE;
    }
    return MFALSE;
}

/******************************************************************************
 *
 ******************************************************************************/

class StreamingProcessorImp
    : public StreamingProcessor
    , public IRequestCallback
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IListener Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual void                onResultReceived(
                                    MUINT32         const requestNo,
                                    StreamId_T      const streamId,
                                    MBOOL           const errorResult,
                                    IMetadata*      const result
                                );

    virtual String8             getUserName();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  RefBase Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual void                onLastStrongRef( const void* /*id*/);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  StreamingProcessor Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual status_t            startAutoFocus();

    virtual status_t            cancelAutoFocus();

    virtual status_t            preCapture();

    virtual status_t            startSmoothZoom(int value);

    virtual status_t            stopSmoothZoom();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IRequestCallback Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    virtual void                RequestCallback(
                                    uint32_t requestNo,
                                    MINT32   type,
                                    MINTPTR  _ext1,
                                    MINTPTR  _ext2
                                );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
                                    StreamingProcessorImp(
                                        sp< CamMsgCbInfo >          const &rpCamMsgCbInfo,
                                        wp< RequestSettingBuilder > const &rpRequestSettingBuilder,
                                        sp< IParamsManager >        const &rpParamsManager
                                    );

                                    ~StreamingProcessorImp();

           bool                     msgTypeEnabled( int32_t msgType );

           void                     doNotifyCb(
                                        uint32_t requestNo,
                                        int32_t _msgType,
                                        int32_t _ext1,
                                        int32_t _ext2,
                                        int32_t _ext3
                                    );

           bool                     isFocusDone( int &isFocusFail );

           bool                     needFocusCallback();

           bool                     needZoomCallback(
                                        MUINT32 requestNo,
                                        MINT32  &zoomIndex
                                    );

public:     ////                    Definitions.

    typedef KeyedVector< MUINT32, MUINT32 > Que_T;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    sp< CamMsgCbInfo >              mpCamMsgCbInfo;
    sp< IParamsManager >            mpParamsManager;
    wp< RequestSettingBuilder >     mpRequestSettingBuilder;

protected: ////                     Logs.
    MINT32                          mLogLevel;

protected: ////                     3A information.
    MUINT8                          mAestate;
    MUINT8                          mAfstate;
    mutable Mutex                   m3ALock;

protected: ////                     zoom callback information.
    MUINT32                         mZoomTargetIndex;
    Que_T                           mZoomResultQueue;
    mutable Mutex                   mZoomLock;

protected: ////                     af callback information.
    MBOOL                           mListenAfResult;
    MBOOL                           mNeedAfCallback;
    MUINT32                         mAfRequest;
    mutable Mutex                   mAutoFocusLock;

protected:
    MBOOL                           mUninit;
};


/******************************************************************************
 *
 ******************************************************************************/
sp< StreamingProcessor >
StreamingProcessor::
createInstance(
    sp< CamMsgCbInfo >          const &rpCamMsgCbInfo,
    wp< RequestSettingBuilder > const &rpRequestSettingBuilder,
    sp< IParamsManager >        const &rpParamsManager
)
{
    return new StreamingProcessorImp(
                    rpCamMsgCbInfo,
                    rpRequestSettingBuilder,
                    rpParamsManager
               );
}

/******************************************************************************
 *
 ******************************************************************************/
StreamingProcessorImp::
StreamingProcessorImp(
    sp< CamMsgCbInfo >          const &rpCamMsgCbInfo,
    wp< RequestSettingBuilder > const &rpRequestSettingBuilder,
    sp< IParamsManager >        const &rpParamsManager
)
    : mLogLevel(1)
    , mpCamMsgCbInfo(rpCamMsgCbInfo)
    , mpRequestSettingBuilder(rpRequestSettingBuilder)
    , mpParamsManager(rpParamsManager)
    //
    , mUninit(false)
{
}


/******************************************************************************
 *
 ******************************************************************************/
StreamingProcessorImp::
~StreamingProcessorImp()
{
}

/******************************************************************************
 *
 ******************************************************************************/
void
StreamingProcessorImp::
onLastStrongRef(const void* /*id*/)
{
    mUninit = true;
}

/******************************************************************************
 *
 ******************************************************************************/
void
StreamingProcessorImp::
onResultReceived(
    MUINT32         const requestNo,
    StreamId_T      const /*streamId*/,
    MBOOL           const errorResult,
    IMetadata*      const result
)
{
    // update request number
    {
        Mutex::Autolock _l(mAutoFocusLock);
        mListenAfResult = ( requestNo == mAfRequest ) ? true : mListenAfResult;
    }
    //
    if ( errorResult ) return;
    // update 3A
    {
        Mutex::Autolock _l(m3ALock);
        tryGetMetadata< MUINT8 >(result, MTK_CONTROL_AF_STATE, mAfstate);
        tryGetMetadata< MUINT8 >(result, MTK_CONTROL_AE_STATE, mAestate);
    }
    //
    // check focus callback
    if ( needFocusCallback() ) {
        int isFocusFail = 1;
        if ( isFocusDone( isFocusFail ) ) {
            doNotifyCb(
                requestNo,
                CAMERA_MSG_FOCUS,
                isFocusFail, 0, 0
            );
        } else
        if ( msgTypeEnabled( CAMERA_MSG_FOCUS_MOVE ) ) {
            doNotifyCb(
                requestNo,
                CAMERA_MSG_FOCUS_MOVE,
                1, 0, 0
            );
        }
    }
    // check zoom callback
    MINT32 zoomIndex = 0;
    if ( needZoomCallback( requestNo, zoomIndex ) ) {
        doNotifyCb(
            requestNo,
            CAMERA_MSG_ZOOM,
            zoomIndex, 0, 0
        );
    }
}

/******************************************************************************
 *
 ******************************************************************************/
void
StreamingProcessorImp::
RequestCallback(
    uint32_t requestNo,
    MINT32   _type,
    MINTPTR  _ext1,
    MINTPTR  _ext2
)
{
    MY_LOGD("requestNo:%d _msgType:%d %d %d", requestNo, _type, _ext1, _ext2);
    switch(_type)
    {
        case IRequestCallback::MSG_START_AUTOFOCUS:
        {
            Mutex::Autolock _l(mAutoFocusLock);
            mAfRequest = requestNo;
            mNeedAfCallback = true;
        } break;
        case IRequestCallback::MSG_START_ZOOM:
        {
            Mutex::Autolock _l(mZoomLock);
            mZoomResultQueue.add(_ext1, requestNo);
        } break;
        default:
            MY_LOGE("Unsupported message type %d", _type);
        break;
    };
}

/******************************************************************************
 *
 ******************************************************************************/
bool
StreamingProcessorImp::
msgTypeEnabled(int32_t msgType)
{
    return  msgType == (msgType & ::android_atomic_release_load(&mpCamMsgCbInfo->mMsgEnabled));
}

/******************************************************************************
 *
 ******************************************************************************/
String8
StreamingProcessorImp::
getUserName()
{
    return String8::format("StreamingProcessor");
}

/******************************************************************************
 *
 ******************************************************************************/
bool
StreamingProcessorImp::
isFocusDone( int &isFocusFail )
{
    Mutex::Autolock _l(m3ALock);
    //
    isFocusFail = ( mAfstate == MTK_CONTROL_AF_STATE_FOCUSED_LOCKED );
    //
    return (   mAfstate == MTK_CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            || mAfstate == MTK_CONTROL_AF_STATE_FOCUSED_LOCKED);
}

/******************************************************************************
 *
 ******************************************************************************/
bool
StreamingProcessorImp::
needFocusCallback()
{
    Mutex::Autolock _l(mAutoFocusLock);
    return ( mNeedAfCallback && mListenAfResult );
}

/******************************************************************************
 *
 ******************************************************************************/
bool
StreamingProcessorImp::
needZoomCallback(
    MUINT32 requestNo,
    MINT32  &zoomIndex
)
{
    Mutex::Autolock _l(mZoomLock);
    for ( size_t i = 0; i < mZoomResultQueue.size(); ++i ) {
        if ( mZoomResultQueue.valueAt(i) == requestNo ) {
            zoomIndex = mZoomResultQueue.keyAt(i);
            return true;
        }
    }
    return false;
}
/******************************************************************************
 *
 ******************************************************************************/
void
StreamingProcessorImp::
doNotifyCb(
    uint32_t requestNo,
    int32_t _msgType,
    int32_t _ext1,
    int32_t _ext2,
    int32_t _ext3
)
{
    int32_t msg1 = _ext1;
    int32_t msg2 = _ext2;
    //
    switch (_msgType)
    {
        case CAMERA_MSG_FOCUS:
        {
            Mutex::Autolock _l(mAutoFocusLock);
            //
            MY_LOGD("CAMERA_MSG_FOCUS requestNo:%d _msgType:%d", requestNo, _msgType);
            mNeedAfCallback = false;
            mListenAfResult = false;
        } break;
        case CAMERA_MSG_FOCUS_MOVE:
        {
            MY_LOGD("CAMERA_MSG_FOCUS_MOVE requestNo:%d _msgType:%d", requestNo, _msgType);
        } break;
        case CAMERA_MSG_ZOOM:
        {
            Mutex::Autolock _l(mZoomLock);
            //
            mZoomResultQueue.removeItem(msg1);
            msg2 = (mUninit) || (msg1 == mZoomTargetIndex);
            MY_LOGD("smoothZoom requestNo:%d (%d, %d) target:%d uninit:%d",
                requestNo, msg1, msg2, mZoomTargetIndex, mUninit
            );
            //
            mpParamsManager->set(
                CameraParameters::KEY_ZOOM,
                msg1
            );
        } break;
        default:
            MY_LOGE("Unsupported message type %d", _msgType);
            return;
        break;
    };
    //
    mpCamMsgCbInfo->mNotifyCb(
        _msgType,
        msg1,
        msg2,
        mpCamMsgCbInfo->mCbCookie
    );
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
StreamingProcessorImp::
startAutoFocus()
{
    FUNCTION_IN;
    //
    MUINT8 afState;
    MUINT8 afMode;
    String8 const s = mpParamsManager->getStr(CameraParameters::KEY_FOCUS_MODE);
    if  ( !s.isEmpty() ) {
        afMode = PARAMSMANAGER_MAP_INST(IParamsManager::eMapFocusMode)->valueFor(s); \
    }

    {
        Mutex::Autolock _l(m3ALock);
        afState = mAfstate;
    }
    //
    {
        IParamsManager::IMap const* focusMap = IParamsManager::getMapInst(IParamsManager::int2type<IParamsManager::eMapFocusMode>());
        /**
          * If the camera does not support auto-focus, it is a no-op and
          * onAutoFocus(boolean, Camera) callback will be called immediately
          * with a fake value of success set to true.
          *
          * Similarly, if focus mode is set to INFINITY, there's no reason to
          * bother the HAL.
          */
        if ( focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_FIXED) ) == afMode ||
             focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_INFINITY) ) == afMode) {
            if ( msgTypeEnabled(CAMERA_MSG_FOCUS) ) {
                mpCamMsgCbInfo->mNotifyCb(CAMERA_MSG_FOCUS, 1, 0, mpCamMsgCbInfo->mCbCookie);
                //
                MY_LOGD_IF(
                    mLogLevel >= 1,
                    "afMode %d", afMode
                );
                //
                FUNCTION_OUT;
                return OK;
            }
        }

        /**
         * If we're in CAF mode, and AF has already been locked, just fire back
         * the callback right away; the HAL would not send a notification since
         * no state change would happen on a AF trigger.
         */
        if ( ( afMode == focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_CONTINUOUS_PICTURE) ) ||
               afMode == focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_CONTINUOUS_VIDEO) ) ) &&
               afState == ANDROID_CONTROL_AF_STATE_FOCUSED_LOCKED ) {
            if ( msgTypeEnabled(CAMERA_MSG_FOCUS) ) {
                mpCamMsgCbInfo->mNotifyCb(CAMERA_MSG_FOCUS, 1, 0, mpCamMsgCbInfo->mCbCookie);
                //
                MY_LOGD_IF(
                    mLogLevel >= 1,
                    "afMode %d", afMode
                );
                //
                FUNCTION_OUT;
                return OK;
            }
        }
    }

    sp< RequestSettingBuilder > builder = mpRequestSettingBuilder.promote();
    if ( builder != 0 ) {
        builder->triggerAutofocus(static_cast< IRequestCallback* >(this));
    } else {
        MY_LOGW("builder cannot be promoted.");
    }

    FUNCTION_OUT;
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
StreamingProcessorImp::
cancelAutoFocus()
{
    FUNCTION_IN;

    {
        Mutex::Autolock _l(mAutoFocusLock);
        mNeedAfCallback = false;
        mListenAfResult = false;
    }
    //
    MUINT8 afMode;
    String8 const s = mpParamsManager->getStr(CameraParameters::KEY_FOCUS_MODE);
    if  ( !s.isEmpty() ) {
        afMode = PARAMSMANAGER_MAP_INST(IParamsManager::eMapFocusMode)->valueFor(s);
    }

    // Canceling does nothing in FIXED or INFINITY modes
    IParamsManager::IMap const* focusMap = IParamsManager::getMapInst(IParamsManager::int2type<IParamsManager::eMapFocusMode>());
    if ( focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_FIXED) ) == afMode ||
         focusMap->valueFor( String8::format(CameraParameters::FOCUS_MODE_INFINITY) ) == afMode) {
        //
        MY_LOGD_IF(
            mLogLevel >= 1,
            "afMode %d", afMode
        );
        //
        FUNCTION_OUT;
        return OK;
    }

    sp< RequestSettingBuilder > builder = mpRequestSettingBuilder.promote();
    if ( builder != 0 ) {
        builder->triggerCancelAutofocus();
    } else {
        MY_LOGW("builder cannot be promoted.");
    }

    FUNCTION_OUT;
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
StreamingProcessorImp::
preCapture()
{
// TODO : fix this
    while (1) {
        MUINT8 aeState;
        {
            Mutex::Autolock _l(m3ALock);
            aeState = mAestate;
        }
        //
        /*if (mTimeoutCount <= 0) {
            ALOGW("Timed out waiting for precapture %s",
                    mAeInPrecapture ? "end" : "start");
            return STANDARD_CAPTURE;
        }*/
        if (aeState == ANDROID_CONTROL_AE_STATE_CONVERGED ||
            aeState == ANDROID_CONTROL_AE_STATE_FLASH_REQUIRED) {
            // It is legal to transit to CONVERGED or FLASH_REQUIRED
            // directly after a trigger.
            MY_LOGD_IF(
                mLogLevel >= 1,
                "AE is already in good state, start capture. aeState %d", aeState
            );
            break;
        }
    }
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
StreamingProcessorImp::
startSmoothZoom(int value)
{
    FUNCTION_IN;
    //
    MY_LOGD_IF( 1, "startSmoothZoom %d", value );
    //
    int currentIndex = 0;
    int inc = 0;
    {
        Mutex::Autolock _l(mZoomLock);
        mZoomTargetIndex = value;
        currentIndex = mpParamsManager->getInt(CameraParameters::KEY_ZOOM);
        inc = ( mZoomTargetIndex > currentIndex ) ? 1 : -1;
    }

    sp< RequestSettingBuilder > builder = mpRequestSettingBuilder.promote();
    if ( builder != 0 ) {
        for ( ; currentIndex != value + inc; currentIndex += inc ) {
            MY_LOGD_IF( mLogLevel >= 1 , "triggerTriggerZoom %d/%d", currentIndex, value );
            builder->triggerTriggerZoom( currentIndex, static_cast< IRequestCallback* >(this) );
        }
    } else {
        MY_LOGW("builder cannot be promoted.");
    }
    //
    FUNCTION_OUT;
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
StreamingProcessorImp::
stopSmoothZoom()
{
    FUNCTION_IN;
    //
    sp< RequestSettingBuilder > builder = mpRequestSettingBuilder.promote();
    if ( builder != 0 ) {
        builder->triggerCancelZoom();
    } else {
        MY_LOGW("builder cannot be promoted.");
    }
    //
    {
        Mutex::Autolock _l(mZoomLock);
        mZoomTargetIndex = mZoomResultQueue.valueAt(mZoomResultQueue.size() - 1);
    }
    //
    FUNCTION_OUT;
    return OK;
}