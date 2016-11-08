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
#define LOG_TAG "MtkCam/ReqSetBud"
//
#include <metadata/client/mtk_metadata_tag.h>
#include <Hal3/mtk_platform_metadata_tag.h>

#include <metadata/IMetadata.h>
#include <metadata/IMetadataProvider.h>

using namespace android;
#include <v1/IParamsManager.h>
#include <v1/converter/RequestSettingBuilder.h>

using namespace NSCam;
using namespace std;

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

#define FUNC_START     MY_LOGD("+")
#define FUNC_END       MY_LOGD("-")
/******************************************************************************
 *
 ******************************************************************************/
#define FUNCTION_IN             MY_LOGD_IF(1<=mLogLevel, "+");
#define FUNCTION_OUT            MY_LOGD_IF(1<=mLogLevel, "-");
#define PUBLIC_API_IN           MY_LOGD_IF(1<=mLogLevel, "API +");
#define PUBLIC_API_OUT          MY_LOGD_IF(1<=mLogLevel, "API -");
#define MY_LOGD1(...)           MY_LOGD_IF(1<=mLogLevel, __VA_ARGS__)
#define MY_LOGD2(...)           MY_LOGD_IF(2<=mLogLevel, __VA_ARGS__)
/******************************************************************************
 *
 ******************************************************************************/
class RequestSettingBuilderImp
    : public RequestSettingBuilder
{
public:

                                    RequestSettingBuilderImp(MINT32 cameraId, sp<IParamsManager> pParamsMgr);

    virtual                         ~RequestSettingBuilderImp(){};

public:     ////                    Operations.

    // for flow control
    MERROR                          createDefaultRequest(int templateId, IMetadata &request) const;
    // AE
    MERROR                          triggerPrecaptureMetering(wp<IRequestCallback> pCB);
    // AF
    MERROR                          triggerAutofocus(wp<IRequestCallback> pCB);

    MERROR                          triggerCancelAutofocus();

    MERROR                          triggerTriggerZoom(MUINT32 const& index, wp<IRequestCallback> pCB);

    MERROR                          triggerCancelZoom();

    MERROR                          capture(IMetadata const& request/*, MUINT32 const& requestId*/);
    // repeat request
    MERROR                          setStreamingRequest(IMetadata const& request/*, MUINT32 const& requestId*/);

    // for RequestProvider
    // requestId[in]
    // nextRequest[out]
    MERROR                          getRequest(MUINT32 const& requestId, IMetadata &nextRequest);

protected:
    MINT32                          mLogLevel;

private:
    MERROR                          insertTriggers(IMetadata &request);

    ITemplateRequest*               getTemplateMetaRequest(MINT32 iOpenId);

    MRect                           calculateCropRegion() const;

    MINT32                          mOpenId;
    sp<IParamsManager>              mpParamsMgr;
    //MUINT32                         mFrameNumber;

    Mutex                           mInterfaceLock;
    Mutex                           mRequestLock;
    Mutex                           mRepeatingRequestLock;
    Mutex                           mInFlightRequestLock;

    //MUINT32                         mTriggerPreId;
    //MUINT32                         mTriggerAfId;
    MUINT32                         mTriggerZoomIndex;

    struct                          RequestTrigger {
                                        // Metadata tag number, e.g. MTK_CONTROL_AE_PRECAPTURE_TRIGGER
                                        MUINT32 metadataTag;
                                        // Metadata value, e.g. 'MTK_CONTROL_AE_PRECAPTURE_TRIGGER_START'
                                        MUINT8 entryValue;
                                    };

    typedef KeyedVector<MUINT32/*tag*/, MUINT8/*entryValue*/> TriggerMap;
    TriggerMap                      mTriggerMap;

    List<IMetadata>                 mRequestQueue;
    List<IMetadata>                 mRepeatingRequests;

    struct                          CallbackParam {

                                        sp<IRequestCallback> RequestCb;

                                        MINT32 frameNo;

                                        MINT32 type;

                                        MUINT32 _ext1;

                                        MUINT32 _ext2;
                                    };

    typedef CallbackParam           Callback_t;
    List<Callback_t>                mCbQueue;
    typedef KeyedVector<MUINT32/*index*/, MRect/*cropRegion*/> CropRegionMap;

    MRect                           mActiveArray;
    Condition                       mRequestQueueCond;
    Condition                       mRepeatingRequestQueueCond;
};

/******************************************************************************
 *
 ******************************************************************************/
sp<RequestSettingBuilder>
RequestSettingBuilder::
createInstance(MINT32 cameraId, sp<IParamsManager> pParamsMgr)
{
    return new RequestSettingBuilderImp(cameraId, pParamsMgr);
}


/******************************************************************************
 *
 ******************************************************************************/
RequestSettingBuilderImp::
RequestSettingBuilderImp(MINT32 cameraId, sp<IParamsManager> pParamsMgr)
    : mOpenId(cameraId)
    , mpParamsMgr(pParamsMgr)
{
    // keep this sp
    //mInFlightRequest =  InFlightRequest::createInstance();
    /*char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    mLogLevel = ::atoi(cLogLevel);*/
    mLogLevel = 1;

    sp<IMetadataProvider> pMetadataProvider = NSMetadataProviderManager::valueFor(cameraId);
    if( ! pMetadataProvider.get() ) {
        MY_LOGE("%d pMetadataProvider.get() is NULL", cameraId);
        pMetadataProvider = IMetadataProvider::create(cameraId);
        NSMetadataProviderManager::add(cameraId, pMetadataProvider.get());
    }
    IMetadata static_meta = pMetadataProvider->geMtktStaticCharacteristics();
    IMetadata::IEntry activeA = static_meta.entryFor(MTK_SENSOR_INFO_ACTIVE_ARRAY_REGION);
    if( !activeA.isEmpty() ) {
        mActiveArray = activeA.itemAt(0, Type2Type<MRect>());
    }
}


/******************************************************************************
 *
 ******************************************************************************/
/*virtual RequestSettingBuilderImp::
~RequestSettingBuilderImp()
{
}
*/

/******************************************************************************
 *
 ******************************************************************************/
#if 0
android::sp<InFlightRequest>
RequestSettingBuilder::getInFlightRequest()
{
    return mInFlightRequest;
}
#endif

/*******************************************************************************
*
********************************************************************************/
ITemplateRequest*
RequestSettingBuilderImp::
getTemplateMetaRequest(MINT32 iOpenId)
{
    ITemplateRequest* obj = NSTemplateRequestManager::valueFor(iOpenId);
    if(obj == NULL) {
        obj = ITemplateRequest::getInstance(iOpenId);
        NSTemplateRequestManager::add(iOpenId, obj);
    }
    return obj;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
createDefaultRequest(
        int templateId,
        IMetadata &request /*out*/
)const
{
    //ITemplateRequest* obj = getTemplateMetaRequest(0/*openId*/);
    //request = obj->getMtkData(templateId);

    ITemplateRequest* obj = NSTemplateRequestManager::valueFor(mOpenId);
    if(obj == NULL) {
        obj = ITemplateRequest::getInstance(mOpenId);
        NSTemplateRequestManager::add(mOpenId, obj);
    }
    request = obj->getMtkData(templateId);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/

MERROR
RequestSettingBuilderImp::
triggerPrecaptureMetering(wp<IRequestCallback> pCB)
{
    Mutex::Autolock il(mInterfaceLock);
    sp<IRequestCallback> cb = pCB.promote();
    if (cb != NULL) {
        Callback_t callback;
        callback.RequestCb = cb;
        callback.type = IRequestCallback::MSG_START_PRECAPTURE;
        mCbQueue.push_back (callback);
    }
    //mTriggerPreId = requestId;
    //MUINT8
    RequestTrigger trigger =
    {
        MTK_CONTROL_AE_PRECAPTURE_TRIGGER,/*metadataTag*/
        MTK_CONTROL_AE_PRECAPTURE_TRIGGER_START/*entryValue*/
    };

    MUINT32 metadataTag = trigger.metadataTag;
    ssize_t index = mTriggerMap.indexOfKey(metadataTag);

    if (index != NAME_NOT_FOUND) {
        mTriggerMap.editValueAt(index) = trigger.entryValue;
    } else {
        mTriggerMap.add(metadataTag, trigger.entryValue);
    }

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
triggerAutofocus(wp<IRequestCallback> pCB)
{

    Mutex::Autolock il(mInterfaceLock);

    sp<IRequestCallback> cb = pCB.promote();
    if (cb != NULL) {
        Callback_t callback;
        callback.RequestCb = cb;
        callback.type = IRequestCallback::MSG_START_AUTOFOCUS;
        mCbQueue.push_back (callback);
    }
    //mTriggerAfId = id;
    //MUINT8
    RequestTrigger trigger =
    {
        MTK_CONTROL_AF_TRIGGER,
        MTK_CONTROL_AF_TRIGGER_START
    };

    MUINT32 metadataTag = trigger.metadataTag;
    ssize_t index = mTriggerMap.indexOfKey(metadataTag);

    if (index != NAME_NOT_FOUND) {
        mTriggerMap.editValueAt(index) = trigger.entryValue;
    } else {
        mTriggerMap.add(metadataTag, trigger.entryValue);
    }

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
triggerCancelAutofocus()
{
    Mutex::Autolock il(mInterfaceLock);

    // when trigger cancel zoom, mCbQueue contain tag MSG_START_AUTOFOCUS will be removed from queue
    List<Callback_t>::iterator item = mCbQueue.begin();
    while ( item != mCbQueue.end() ) {
        switch (item->type) {
            case IRequestCallback::MSG_START_AUTOFOCUS :
                {
                    mCbQueue.erase(item);
                }
            default :
                break;
        }
        item++;
    }
    Callback_t callback;
    callback.type = IRequestCallback::MSG_CANCEL_AUTOFOCUS;
    mCbQueue.push_back (callback);

    //mTriggerAfId = id;
    //MUINT8
    RequestTrigger trigger =
    {
        MTK_CONTROL_AF_TRIGGER,
        MTK_CONTROL_AF_TRIGGER_CANCEL
    };

    MUINT32 metadataTag = trigger.metadataTag;
    ssize_t index = mTriggerMap.indexOfKey(metadataTag);

    if (index != NAME_NOT_FOUND) {
        mTriggerMap.editValueAt(index) = trigger.entryValue;
    } else {
        mTriggerMap.add(metadataTag, trigger.entryValue);
    }

    return OK;
}
/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
triggerTriggerZoom(MUINT32 const& index, wp<IRequestCallback> pCB)
{
    Mutex::Autolock il(mInterfaceLock);
    //mTriggerZoomIndex = index;

    sp<IRequestCallback> cb = pCB.promote();
    if (cb != NULL) {
        Callback_t callback;
        callback.RequestCb = cb;
        callback.type = IRequestCallback::MSG_START_ZOOM;
        callback._ext1 = index;
        mCbQueue.push_back (callback);
    }
    MY_LOGD1("triggerTriggerZoom index = %d", index);
    //calculate crop region
    {

    }
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
triggerCancelZoom()
{
    Mutex::Autolock il(mInterfaceLock);

    // when trigger cancel zoom, mCbQueue contain tag MSG_START_ZOOM will be removed from queue
    List<Callback_t>::iterator item = mCbQueue.begin();
    while ( item != mCbQueue.end() ) {
        switch (item->type) {
            case IRequestCallback::MSG_START_ZOOM :
                {
                    mCbQueue.erase(item);
                }
            default :
                break;
        }
        item++;
    }

    return OK;
}
/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
capture(IMetadata const& request/*, MUINT32 const& requestId*/)
{
    Mutex::Autolock l(mRequestLock);

    IMetadata requestL = request;

    // update request Id, not use
    /*IMetadata::IEntry entry(MTK_REQUEST_ID);
    entry.push_back(requestId, Type2Type< MINT32 >());
    requestL.update(MTK_REQUEST_ID, entry);

    printf("requestId=%d\n",requestId);*/
    List<IMetadata> requests;
    requests.push_back(requestL);

    //MTK_REQUEST_ID
    /*for (List<IMetadata>::iterator it = requests.begin();
                it != requests.end(); ++it) {
            IMetadata::IEntry entry(MTK_REQUEST_ID);
            entry.push_back(requestId, Type2Type< MINT32 >());
            it->update(MTK_REQUEST_ID, entry);
    }*/

    for (List<IMetadata>::iterator it = requests.begin(); it != requests.end();
            ++it) {
        mRequestQueue.push_back(*it);
    }
    mRequestQueueCond.signal();

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
setStreamingRequest(IMetadata const& request/*, MUINT32 const& requestId*/)
{
    Mutex::Autolock l(mRequestLock);

    IMetadata requestL = request;
    // update request Id
    //IMetadata::IEntry &entry =
    //        requests.editEntryFor(MTK_REQUEST_ID);
    //entry.editItemAt(0, Type2Type< MUINT32 >()) = requestId;

    List<IMetadata> requests;
    requests.push_back(requestL);

    //MTK_REQUEST_ID not use
    /*for (List<IMetadata>::iterator it = requests.begin();
                it != requests.end(); ++it) {
            IMetadata::IEntry entry(MTK_REQUEST_ID);
            entry.push_back(requestId, Type2Type< MINT32 >());
            it->update(MTK_REQUEST_ID, entry);
    }*/

    mRepeatingRequests.clear();
    const List<IMetadata> cRequests = requests;
    mRepeatingRequests.insert(mRepeatingRequests.begin(),
            cRequests.begin(), cRequests.end());
    //mRepeatingRequestQueueCond.signal();
    /*if(mRequestQueue.empty())
    {
        mRequestQueue = mRepeatingRequests;
    }*/

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
#if 0
MUINT32
RequestSettingBuilder::
getCaptureRequest(IMetadata &nextRequest)
{
    MERROR err = OK;
    Mutex::Autolock l(mRequestLock);

    while(mRequestQueue.empty())
    {
        status_t status = mRequestQueueCond.wait(mRequestLock);
        if  ( OK != status ) {
            MY_LOGW(
                "wait status:%d:%s, mRequestQueue.size:%zu",
                status, ::strerror(-status), mRequestQueue.size()
            );
        }
    }
    List< IMetadata >::iterator firstRequest = mRequestQueue.begin();
    nextRequest = *firstRequest;
    mRequestQueue.erase(firstRequest);

    // insert trigger from triggerMap
    MY_LOGD_IF(0,"insert trigger from triggerMap");
    err  = insertTriggers(nextRequest);

    // register in flight
    /*{
        //Mutex::Autolock l(mInFlightRequestLock);
        mFrameNumber++;
        InFlightRequest::FlightRequest flightRequest;
        flightRequest.precaptureTriggerId = mTriggerPreId;
        flightRequest.afTriggerId = mTriggerAfId;

        mInFlightRequest->enque(mFrameNumber, flightRequest);
    }*/
    return mFrameNumber;
}
#endif
/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
getRequest(MUINT32 const& requestId, IMetadata &nextRequest)
{
    MERROR err = OK;
    //IMetadata* nextRequest;
    //get a request and remove it

    Mutex::Autolock l(mRequestLock);

    // TODO:
    while (mRequestQueue.empty()) {
        if (!mRepeatingRequests.empty()) {
            const List< IMetadata > &requests = mRepeatingRequests;
            List< IMetadata >::const_iterator firstRequest = requests.begin();
            mRequestQueue.insert(mRequestQueue.end(),
                    firstRequest,
                    requests.end());

        }
        break;
    }

    List< IMetadata >::iterator firstRequest = mRequestQueue.begin();
    nextRequest = *firstRequest;
    mRequestQueue.erase(firstRequest);

    /*Mutex::Autolock l(mRepeatingRequestLock);

    while(mRepeatingRequests.empty())
    {
        status_t status = mRepeatingRequestQueueCond.wait(mRepeatingRequestLock);
        if  ( OK != status ) {
            MY_LOGW(
                "wait status:%d:%s, mRepeatingRequests.size:%zu",
                status, ::strerror(-status), mRepeatingRequests.size()
            );
        }
    }
    List< IMetadata >::iterator firstRequest = mRepeatingRequests.begin();
    nextRequest = *firstRequest;
    mRepeatingRequests.erase(firstRequest);
*/
    // insert trigger from triggerMap
    //err = insertTriggers(nextRequest);

    //mFrameNumber++;
    {
    Mutex::Autolock l(mInterfaceLock);
    bool hasZoom = false;// zoom once at get a request
    //bool hasCancelZoom = false;
    // mCbQueue
    //for (List<Callback_t> ::iterator i = mCbQueue.begin(); i != mCbQueue.end(); i++)

    List<Callback_t>::iterator item = mCbQueue.begin();
    // if MSG_CANCEL_ZOOM, erase queue after
    while ( item != mCbQueue.end() ) {
        switch (item->type) {
            case IRequestCallback::MSG_START_ZOOM :
                {
                    //if (hasCancelZoom) break;
                    if (hasZoom) break;
                    MRect reqCropRegion =  calculateCropRegion();
                    IMetadata::IEntry entry(MTK_SCALER_CROP_REGION);
                    entry.push_back(reqCropRegion, Type2Type<MRect>());
                    nextRequest.update(MTK_SCALER_CROP_REGION, entry);
                    hasZoom = true;
                    item->RequestCb->RequestCallback(requestId, item->type, item->_ext1);
                    mCbQueue.erase(item);
                    break;
                }
            /*case IRequestCallback::MSG_CANCEL_ZOOM :
                {
                    hasCancelZoom = true;
                }*/
            case IRequestCallback::MSG_START_AUTOFOCUS :
                {
                    IMetadata::IEntry entry(MTK_CONTROL_AF_TRIGGER);
                    entry.push_back(MTK_CONTROL_AF_TRIGGER_START, Type2Type< MUINT8 >());
                    nextRequest.update(entry.tag(), entry);
                    item->RequestCb->RequestCallback(requestId, item->type);
                    mCbQueue.erase(item);
                    break;
                }

            case IRequestCallback::MSG_CANCEL_AUTOFOCUS :
                {
                    IMetadata::IEntry entry(MTK_CONTROL_AF_TRIGGER);
                    entry.push_back(MTK_CONTROL_AF_TRIGGER_CANCEL, Type2Type< MUINT8 >());
                    nextRequest.update(entry.tag(), entry);
                    mCbQueue.erase(item);
                    break;
                }
            case IRequestCallback::MSG_START_PRECAPTURE :
                {
                    IMetadata::IEntry entry(MTK_CONTROL_AF_TRIGGER);
                    entry.push_back(MTK_CONTROL_AE_PRECAPTURE_TRIGGER_START, Type2Type< MUINT8 >());
                    nextRequest.update(entry.tag(), entry);
                    item->RequestCb->RequestCallback(requestId, item->type);
                    mCbQueue.erase(item);
                    break;
                }
            default :
                {
                    MY_LOGE("no available type : %d", item->type);
                    break;
                }
        }
        item++;
    }

    }//end mInterfaceLock
    // register in flight
    /*{
        Mutex::Autolock l(mInFlightRequestLock);
        mFrameNumber++;
        InFlightRequest::FlightRequest flightRequest;
        flightRequest.precaptureTriggerId = mTriggerPreId;
        flightRequest.afTriggerId = mTriggerAfId;

        mInFlightRequest->enque(mFrameNumber, flightRequest);
    }*/
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestSettingBuilderImp::
insertTriggers(IMetadata &request)
{
    Mutex::Autolock il(mInterfaceLock);

    size_t count = mTriggerMap.size();

    for (size_t i = 0; i < count; ++i) {
        MUINT32 tag          = mTriggerMap.keyAt(i);
        MUINT8 entryValue    = mTriggerMap.valueAt(i);

        IMetadata::IEntry entry(tag);
        entry.push_back(entryValue, Type2Type< MUINT8 >());
        request.update(entry.tag(), entry);
        MY_LOGD1("insert trigger tag %p, value %d\n", tag, entryValue);
    }

    mTriggerMap.clear();

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MRect RequestSettingBuilderImp::calculateCropRegion() const {

    float zoomLeft, zoomTop, zoomWidth, zoomHeight;
    uint32_t r = mpParamsMgr->getZoomRatio();

    float zoomRatio = (r / 100.0) ;
    MY_LOGD_IF(1, "ratio=%f, mRatio[mZoom]=%d, activeWidth=%d, activeHeight=%d",
          zoomRatio, r,
          mActiveArray.s.w, mActiveArray.s.h);

    zoomWidth = mActiveArray.s.w / zoomRatio;
    zoomHeight = mActiveArray.s.h / zoomRatio;

    zoomLeft = (mActiveArray.s.w - zoomWidth) / 2;
    zoomTop = (mActiveArray.s.h - zoomHeight) / 2;

    MY_LOGD_IF(1, "Crop region calculated (x=%d,y=%d,w=%f,h=%f)",
        (int32_t)zoomLeft, (int32_t)zoomTop, zoomWidth, zoomHeight);

    return MRect(MPoint(zoomLeft, zoomTop),MSize(zoomWidth, zoomHeight));
}