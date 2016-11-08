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

#ifndef _MTK_HARDWARE_INCLUDE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDER_H_
#define _MTK_HARDWARE_INCLUDE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDER_H_
//
#include <utils/RefBase.h>
#include <utils/Vector.h>
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streambuf/StreamBufferProvider.h>
#include <v1/Processor/ResultProcessor.h>
#include <v1/camutils/IBuffer.h>
#include <v1/camutils/IImgBufQueue.h>


typedef NSCam::v3::Utils::HalImageStreamBuffer      HalImageStreamBuffer;
typedef NSCam::v3::IImageStreamInfo                 IImageStreamInfo;
typedef NSCam::v3::Utils::IStreamBufferProvider     IStreamBufferProvider;
//typedef android::MtkCamUtils::IImgBufProvider       IImgBufProvider;

using namespace android::MtkCamUtils;
#include <ImgBufProvidersManager.h>

/******************************************************************************
 *
 ******************************************************************************/
//
namespace NSCam {
namespace v1 {


class IImageCallback
    : public virtual android::RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual                             ~IImageCallback() {};

    /**
     *
     * Received result buffer.
     *
     * @param[in] RequestNo : request number.
     *
     * @param[in] pBuffer : IImageBuffer.
     *
     */
    virtual MERROR                      onResultReceived(
                                            MUINT32 const              RequestNo,
                                            android::sp<IImageBuffer>& pBuffer
                                        )                                                       = 0;
};

/******************************************************************************
 *
 ******************************************************************************/


class IBufferPool
    : public virtual android::RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:   //// operations.
    static sp<IBufferPool>              createInstance();

    /**
     * Try to acquire a buffer from the pool.
     *
     * @param[in] szCallerName: a null-terminated string for a caller name.
     *
     * @param[out] rpBuffer: a reference to a newly acquired buffer.
     *
     * @return 0 indicates success; non-zero indicates an error code.
     */
    virtual MERROR                      acquireFromPool(
                                            char const*                    szCallerName,
                                            android::sp<IImageBufferHeap>& rpBuffer
                                        )                                                       = 0;

    /**
     * Release a buffer to the pool.
     *
     * @param[in] szCallerName: a null-terminated string for a caller name.
     *
     * @param[in] pBuffer: a buffer to release.
     *
     * @return
     *      0 indicates success; non-zero indicates an error code.
     */
    virtual MERROR                      releaseToPool(
                                            char const*                   szCallerName,
                                            android::sp<IImageBufferHeap> pBuffer,
                                            MUINT64                       rTimeStamp,
                                            bool                          rErrorResult
                                        )                                                       = 0;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:    //// debug
    /**
     * Pool name.
     */
    virtual char const*                 poolName() const                                        = 0;

    /**
     * Dump information for debug.
     */
    virtual MVOID                       dumpPool() const                                        = 0;

public:    //// set stream info & buffer source
    /**
     *
     * Set image stream info.
     *
     * @param[in] pStreamInfo : IImageStreamInfo.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual MERROR                      setImageStreamInfo(
                                            char const* szCallerName,
                                            android::sp<IImageStreamInfo> pStreamInfo
                                        )                                                       = 0;

    /**
     * allocate buffer for pool.
     *
     * @param[in] szCallerName: a null-terminated string for a caller name.
     *
     * @param[in] maxNumberOfBuffers: maximum number of buffers which can be
     *  allocated from this pool.
     *
     * @param[in] minNumberOfInitialCommittedBuffers: minimum number of buffers
     *  which are initially committed.
     *
     * @return 0 indicates success; non-zero indicates an error code.
     */
    virtual MERROR                      allocateBuffer(
                                            char const* szCallerName,
                                            size_t maxNumberOfBuffers,
                                            size_t minNumberOfInitialCommittedBuffers
                                        )                                                       = 0;

    /**
     *
     * Set specific camClient as buffer source.
     *
     * @param[in] pSource : source CamClient.
     *
     */
    /*virtual MERROR                      setCamClient(
                                            char const* szCallerName,
                                            android::sp<IImgBufProvider> pSource
                                        )                                                       = 0;*/
    virtual MERROR                      setCamClient(
                                            char const*                         szCallerName,
                                            android::sp<ImgBufProvidersManager> pSource,
                                            MINT32                              rMode
                                        )                                                       = 0;

    /**
     *
     * User provide buffer.
     *
     * @param[in] pSource : source CamClient.
     *
     */
    virtual MERROR                      setUsersBuffer(
                                            char const* szCallerName,
                                            List<android::sp<IImageBuffer> > pSource
                                        )                                                       = 0;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:   //// operations.
    /**
     * Uninitialize the pool and free all buffers.
     *
     * @param[in] szCallerName: a null-terminated string for a caller name.
     */
    virtual MVOID                       uninitPool(
                                            char const* szCallerName
                                        )                                                       = 0;

};


/******************************************************************************
 *
 ******************************************************************************/
class IConsumerPool
    : public virtual android::RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual android::status_t           returnBuffer(
                                            MINT32                          rRequestNo
                                        )                                                       = 0;
};

/******************************************************************************
 *
 ******************************************************************************/
class ISelector
    : public virtual android::RefBase
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    /**
     * Send result metadata & buffer to selector.
     *
     * @param[in] rRequestNo: request number.
     *
     * @param[in] rResult: request number of error result.
     *
     * @param[in] rpHeap: result buffer.
     *
     * @return
     *      true indicates desire result, user should keep it.
     *      false indicates useless result, will return to pool.
     *
     */
    virtual bool                        selectResult(
                                            MINT32                          rRequestNo,
                                            IMetadata                       rResult,
                                            android::sp<IImageBufferHeap>   rpHeap
                                        )                                                       = 0;

    /**
     * Notify error result.
     *
     * @param[in] rRequestNo: request number of error result.
     */
    virtual android::status_t           errorResult(
                                            MINT32                          rRequestNo
                                        )                                                       = 0;

    /**
     *
     * Get a set of result [buffer, meta].
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           getResultSet(
                                            IMetadata&                       rResultMeta,
                                            android::sp<IImageBufferHeap>&   rpHeap
                                        )                                                       = 0;

    /**
     * Start return all buffer to buffer pool.
     *
     */
    virtual android::status_t           flush()                                                 = 0;

    /**
     * Return buffer to buffer pool.
     *
     */
    virtual android::status_t           setPool(
                                            android::wp<IConsumerPool>      rpPool
                                        )                                                       = 0;
};

/******************************************************************************
 *
 ******************************************************************************/
class StreamBufferProvider
    : public ResultProcessor::IListener
    , public IStreamBufferProvider
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual                             ~StreamBufferProvider() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    /**
     *
     * Set image stream info.
     *
     * @param[in] pStreamInfo : IImageStreamInfo.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           setImageStreamInfo(
                                            android::sp<IImageStreamInfo> pStreamInfo
                                        )                                                       = 0;

    /**
     *
     * Query image stream info.
     *
     * @param[out] pStreamInfo : IImageStreamInfo.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           queryImageStreamInfo(
                                            android::sp<IImageStreamInfo>& pStreamInfo
                                        )                                                       = 0;

    /**
     *
     * Set callback for image.
     *
     * @param[in] cb : image callback pointer.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           setImageCallback(
                                            android::wp< IImageCallback > cb
                                        )                                                       = 0;

    /**
     *
     * Set buffer pool to buffer provider.
     *
     * @param[in] pBufProvider : buffer pool.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           setBufferPool(
                                            android::sp< IBufferPool >    pBufProvider
                                        )                                                       = 0;

public: //// for consumer
    /**
     *
     * Buffer provider is consumer or provider?
     *
     * @return
     *      true indicates consumer; otherwise provider.
     */
    virtual bool                        isComsumer()                                            = 0;

    /**
     *
     * Set buffer select rule for consumer.
     * Will trigger flush for previous rule.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           setSelector(
                                            android::sp< ISelector > pRule
                                        )                                                       = 0;

    /**
     *
     * Get consumer's result metadata.
     *
     * @return
     *      0 indicates success; otherwise failure.
     */
    virtual android::status_t           getMetadata(
                                            IMetadata&  rResultMeta
                                        )                                                       = 0;
};

/******************************************************************************
 *
 ******************************************************************************/
};  //namespace v1
};  //namespace NSCam
#endif  //_MTK_HARDWARE_INCLUDE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDER_H_

