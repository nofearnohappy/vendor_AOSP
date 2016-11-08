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

#ifndef _MTK_HARDWARE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDERIMP_H_
#define _MTK_HARDWARE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDERIMP_H_
//
#include <utils/RefBase.h>
#include <utils/Vector.h>
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streambuf/StreamBufferProvider.h>
#include <v1/Processor/ResultProcessor.h>
#include <LegacyPipeline/StreamBufferProvider.h>
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>

typedef NSCam::v3::Utils::HalImageStreamBuffer      HalImageStreamBuffer;
typedef NSCam::v3::IImageStreamInfo                 IImageStreamInfo;
typedef NSCam::v3::Utils::IStreamBufferProvider     IStreamBufferProvider;

typedef NSCamStreamBufProvider::HalImageStreamBufferProvider HalImageStreamBufferProvider;

/******************************************************************************
 *
 ******************************************************************************/
//
namespace NSCam {
namespace v1 {


/******************************************************************************
 *
 ******************************************************************************/

class CStreamBufferProviderImp
    : public StreamBufferProvider
    , public IConsumerPool
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IListener Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual void                        onResultReceived(
                                            MUINT32         const requestNo,
                                            StreamId_T      const streamId,
                                            MBOOL           const errorResult,
                                            IMetadata*      const result
                                        );

    virtual android::String8            getUserName();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IConsumerPool Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual android::status_t           returnBuffer(
                                            MINT32                          rRequestNo
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IStreamBufferProvider Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual MERROR                      dequeStreamBuffer(
                                            MUINT32 const                       iRequestNo,
                                            android::sp<IImageStreamInfo> const rpStreamInfo,
                                            android::sp<HalImageStreamBuffer>&  rpStreamBuffer
                                        );

    virtual MERROR                      enqueStreamBuffer(
                                            android::sp<IImageStreamInfo> const rpStreamInfo,
                                            android::sp<HalImageStreamBuffer>   rpStreamBuffer,
                                            MUINT32                             bBufStatus
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
                                        CStreamBufferProviderImp();

                                        ~CStreamBufferProviderImp() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  StreamBufferProvider Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual android::status_t           setImageStreamInfo(
                                            android::sp<IImageStreamInfo> pStreamInfo
                                        );

    virtual android::status_t           queryImageStreamInfo(
                                            android::sp<IImageStreamInfo>& pStreamInfo
                                        );

    virtual android::status_t           setImageCallback(
                                            android::wp< IImageCallback > cb
                                        );

    virtual android::status_t           setBufferPool(
                                            android::sp< IBufferPool >    pBufProvider
                                        );

    virtual android::status_t           setSelector(
                                            android::sp< ISelector > pRule
                                        );

    virtual bool                        isComsumer();

    virtual android::status_t           getMetadata(
                                            IMetadata&  rResultMeta
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    MERROR                              getResultBufferFromProvider(
                                            MUINT32                       rRequestNo,
                                            android::sp<IImageBufferHeap> rpHep,
                                            MUINT32                       rbBufStatus
                                        );

protected:
    MERROR                              queueToSet(
                                            MUINT32                       rRequestNo,
                                            android::sp<IImageBufferHeap> rpHep
                                        );

    MERROR                              queueToSet(
                                            MUINT32                       rRequestNo,
                                            IMetadata                     rResultMeta
                                        );

    android::status_t                   clearSet(
                                            MUINT32 const                 iRequestNo,
                                            android::sp<IImageBufferHeap> rpHeap
                                        );

    android::status_t                   clearSet(
                                            MUINT32 const                 iRequestNo,
                                            IMetadata                     rResultMeta
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    android::sp< IBufferPool >          mpBufferPool;
    mutable Mutex                       mPoolLock;

protected:
    android::wp< IImageCallback >       mpImageCallback;
    android::sp< IImageStreamInfo >     mpImageStreamInfo;

protected:
    android::sp< ISelector >            mpRule;
    mutable Mutex                       mRuleLock;

protected:
    KeyedVector<
        MUINT32,
        android::sp<IImageBufferHeap>
    >                                     mSelectorKeepBufferMap;
    mutable Mutex                         mSKBMapLock;

protected:
    mutable Mutex                           mQListLock;
    List< android::sp<IImageBufferHeap> >   mQueryList;

protected:
    struct Buffer_T {
        MUINT32                       requestNo;
        android::sp<IImageBufferHeap> heap;
    };
    // <address of HalImageStreamBuffer, Buffer_T>
    KeyedVector<HalImageStreamBuffer*, Buffer_T> mBufferMap;
    mutable Mutex                                mBMapLock;

protected:
    struct ResultSet_T {
        MUINT32                       requestNo;
        android::sp<IImageBufferHeap> heap;
        Vector<IMetadata*>            resultMeta;
    };

    KeyedVector<MUINT32, ResultSet_T>     mResultSetMap;
    mutable Mutex                         mResultSetLock;
    MINT32                                mNumberOfWaitingMeta;
};


/******************************************************************************
 *
 ******************************************************************************/


class PStreamBufferProviderImp
    : public StreamBufferProvider
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IListener Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual void                        onResultReceived(
                                            MUINT32         const requestNo,
                                            StreamId_T      const streamId,
                                            MBOOL           const errorResult,
                                            IMetadata*      const result
                                        );

    virtual android::String8            getUserName();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IStreamBufferProvider Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual MERROR                      dequeStreamBuffer(
                                            MUINT32 const                       iRequestNo,
                                            android::sp<IImageStreamInfo> const rpStreamInfo,
                                            android::sp<HalImageStreamBuffer>&  rpStreamBuffer
                                        );

    virtual MERROR                      enqueStreamBuffer(
                                            android::sp<IImageStreamInfo> const rpStreamInfo,
                                            android::sp<HalImageStreamBuffer>   rpStreamBuffer,
                                            MUINT32                             bBufStatus
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
                                        PStreamBufferProviderImp();

                                        ~PStreamBufferProviderImp() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  StreamBufferProvider Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual android::status_t           setImageStreamInfo(
                                            android::sp<IImageStreamInfo> pStreamInfo
                                        );

    virtual android::status_t           queryImageStreamInfo(
                                            android::sp<IImageStreamInfo>& pStreamInfo
                                        );

    virtual android::status_t           setImageCallback(
                                            android::wp< IImageCallback > cb
                                        );

    virtual android::status_t           setBufferPool(
                                            android::sp< IBufferPool >    pBufProvider
                                        );

    virtual android::status_t           setSelector(
                                            android::sp< ISelector > pRule
                                        );

    virtual bool                        isComsumer();

    virtual android::status_t           getMetadata(
                                            IMetadata&  rResultMeta
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    MERROR                              setConsumer(
                                            android::sp< CStreamBufferProviderImp > pConsumer
                                        );

protected:
    android::status_t                   queueToSet(
                                            MUINT32 const                 iRequestNo,
                                            android::sp<IImageBufferHeap> rpHeap
                                        );

    android::status_t                   queueToSet(
                                            MUINT32 const                 iRequestNo,
                                            IMetadata                     rResultMeta
                                        );

    android::status_t                   clearSet(
                                            MUINT32 const                 iRequestNo,
                                            android::sp<IImageBufferHeap> rpHeap
                                        );

    android::status_t                   clearSet(
                                            MUINT32 const                 iRequestNo,
                                            IMetadata                     rResultMeta
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    android::sp< IBufferPool >              mpBufferPool;
    mutable Mutex                           mPoolLock;

protected:
    android::wp< IImageCallback >           mpImageCallback;
    android::sp< IImageStreamInfo >         mpImageStreamInfo;
    android::wp< CStreamBufferProviderImp > mpConsumer;

protected:
    struct Buffer_T {
        MUINT32                       requestNo;
        android::sp<IImageBufferHeap> heap;
    };
    // <address of HalImageStreamBuffer, Buffer_T>
    KeyedVector<HalImageStreamBuffer*, Buffer_T>
                                          mBufferMap;
    mutable Mutex                         mBufferLock;

protected:
    struct ResultSet_T {
        MUINT32                       requestNo;
        android::sp<IImageBufferHeap> heap;
        Vector<IMetadata*>            resultMeta;
    };

    KeyedVector<MUINT32, ResultSet_T>     mResultSetMap;
    mutable Mutex                         mResultSetLock;
    MINT32                                mNumberOfWaitingMeta;

};


};  //namespace v1
};  //namespace NSCam
#endif  //_MTK_HARDWARE_MTKCAM_V1_STREAMBUFFERPROVIDER_STREAMBUFFERPROVIDERIMP_H_

