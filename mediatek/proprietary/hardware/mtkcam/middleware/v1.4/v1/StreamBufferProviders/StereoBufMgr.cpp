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
#define LOG_TAG "MtkCam/StereoBufMgr"
//
#include <list>
#include <utils/threads.h>
using namespace android;
//
#include <Log.h>
#include <common.h>
using namespace NSCam;
//
#include <vector>
using namespace std;
//
#include <utils/include/Format.h>
using namespace NSCam::Utils::Format;
//
#include <utils/include/ImageBufferHeap.h>
#include <metadata/IMetadata.h>
#include <v1/StreamBufferProviders/StereoBufMgr.h>
//
#if defined(__func__)
#undef __func__
#endif
#define __func__ __FUNCTION__
//
#if 1
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s]" fmt, __func__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s]" fmt, __func__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s]" fmt, __func__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s]" fmt, __func__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s]" fmt, __func__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s]" fmt, __func__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s]" fmt, __func__, ##arg)
#else
#endif
//
#define FUNC_START  MY_LOGD("+")
#define FUNC_END    MY_LOGD("-")
#define FUNC_NAME   MY_LOGD("")

#define DEQUE_FAILED_WARNING_THRESHOLD 10
//
/*******************************************************************************
*
********************************************************************************/
namespace NSMtkBufMgr
{
//
class StereoBufMgrImp : public StereoBufMgr
{
    public:
        StereoBufMgrImp(MUINT32 openId);
        ~StereoBufMgrImp();
        //
        virtual MVOID   destroyInstance(void);
        virtual MBOOL   setMaxDequeNum(MUINT32 maxNum);
        virtual MBOOL   dequeBuf(sp<IImageBufferHeap>& pImageBuffer, IMetadata* pMetadata);
        virtual MBOOL   enqueBuf(sp<IImageBufferHeap> pImageBuffer);
        virtual MBOOL   popBuf(sp<IImageBufferHeap>& pImageBuffer);
        virtual MBOOL   pushBuf(sp<IImageBufferHeap> pImageBuffer, MUINT32 iReqNum, MBOOL usable);
        virtual MBOOL   getBufLen(MUINT32& bufLen, bufLenType lenType = BUF_LEN_CURRENT);
        virtual MBOOL   setMetadata(MUINT32 iReqNum, IMetadata* pMetadata);
        virtual MBOOL   startDequeBuf();
        virtual MBOOL   stopDequeBuf();
        //
    private:
        typedef struct
        {
            MUINT32                 iReqNum;
            sp<IImageBufferHeap>    imageBuffer;
            IMetadata               metadata;
            MBOOL                   setMetadata;
            MBOOL                   isUsed;
        }BUF_INFO;
        MUINT32                     mOpenId;
        mutable Mutex               mLock;
        mutable Condition           mCond;
        mutable Condition           mCondDequeBuf;
        list<BUF_INFO>              mlBufInfo;
        MBOOL                       mbUse;
        MUINT32                     mDequeBufCnt;
        MUINT32                     mDequeBufMaxNum;
        MUINT32                     mPushedBufferCount;
        MBOOL                       mIsDequeBufStopped;
};
/*******************************************************************************
*
********************************************************************************/
//static Mutex                gStereoBufMgrImpLock;
//static list<StereoBufMgrImp*>     glpStereoBufMgrImpList;
//-----------------------------------------------------------------------------
StereoBufMgr*
StereoBufMgr::
createInstance(MUINT32 openId)
{
    StereoBufMgrImp* pStereoBufMgrImp = new StereoBufMgrImp(openId);
    return pStereoBufMgrImp;
}
//-----------------------------------------------------------------------------
MVOID
StereoBufMgrImp::
destroyInstance(void)
{
    delete this;
}
//------------------------------------------------------------------------------
StereoBufMgrImp::
StereoBufMgrImp(MUINT32 openId)
{
    FUNC_NAME;

    mlBufInfo.clear();

    //
    mOpenId = openId;
    mbUse = MFALSE;
    mDequeBufCnt = 0;
    mDequeBufMaxNum = 0;
    mPushedBufferCount = 0;
    mIsDequeBufStopped = MFALSE;
}
//-----------------------------------------------------------------------------
StereoBufMgrImp::
~StereoBufMgrImp()
{
    FUNC_NAME;

    //
    mlBufInfo.clear();

}
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
setMaxDequeNum(MUINT32 maxNum)
{
    mDequeBufMaxNum = maxNum;
    return MTRUE;
}
//
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
setMetadata(MUINT32 iReqNum, IMetadata* pMetadata)
{
    Mutex::Autolock _l(mLock);
    MBOOL isFound = MFALSE;
    list<BUF_INFO>::iterator it;

    for(it = mlBufInfo.begin(); it != mlBufInfo.end(); it++)
    {
        if((*it).iReqNum == iReqNum)
        {
            MY_LOGD("Req(%d),meta(0x%X)", iReqNum, pMetadata);
            (*it).metadata = (*pMetadata);
            (*it).setMetadata = true;
            isFound = MTRUE;
            mCond.broadcast();
        }
    }
    if(!isFound)
    {
        MY_LOGD("Req(%d)not found, create new", iReqNum);
        BUF_INFO tempBuf;
        tempBuf.iReqNum = iReqNum;
        tempBuf.imageBuffer = NULL;
        tempBuf.metadata = (*pMetadata);
        tempBuf.setMetadata = true;
        tempBuf.isUsed = MFALSE;
        mlBufInfo.push_back(tempBuf);
        mCond.broadcast();
    }
    return isFound;
}

//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
dequeBuf(sp<IImageBufferHeap>& pImageBuffer, IMetadata* pMetadata)
{
    FUNC_START;
    //
    MUINT32 dequeFailedCount = 0;
    list<BUF_INFO>::iterator it;
    while(1)
    {
        Mutex::Autolock _l(mLock);
        if(mIsDequeBufStopped){
            pImageBuffer = NULL;
            pMetadata = NULL;
            MY_LOGD("bufMgr is forceStoppped, return no buffer and meta");
            return MFALSE;
        }
        if(mDequeBufCnt >= mDequeBufMaxNum)
        {
            MY_LOGW("DBC(%d) < DBMN(%d), Wait E",
                    mDequeBufCnt,
                    mDequeBufMaxNum);
            mCondDequeBuf.wait(mLock);
            MY_LOGW("DBC(%d), Wait X",mDequeBufCnt);
        }
        else
        {
            for(it = mlBufInfo.begin(); it != mlBufInfo.end(); it++)
            {
                if((*it).isUsed != MTRUE)
                {
                    break;
                }
            }
            //
            if(it == mlBufInfo.end() || (*it).imageBuffer == NULL || !((*it).setMetadata))
            {
                // show warning if deque fail count is too much
                dequeFailedCount++;
                if(dequeFailedCount > DEQUE_FAILED_WARNING_THRESHOLD){
                    MY_LOGE("Warning: dequeFailedCount=%d > DEQUE_FAILED_WARNING_THRESHOLD=%d !", dequeFailedCount, DEQUE_FAILED_WARNING_THRESHOLD);
                }
                // find one that is not used but meta or buffer not ready
                if(it == mlBufInfo.end()){
                    MY_LOGW("no buffer is ready, Wait E");
                }else{
                    MY_LOGW("buffer or meta is not ready (%p, %d), Wait E", (*it).imageBuffer.get(), (*it).setMetadata);
                }
                mCond.wait(mLock);
                MY_LOGW("Wait X");
                continue;
            }else{
                // ok, found a good one!
                mDequeBufCnt++;
                MY_LOGD("Id(%d), Buf(0x%p), metacount(%d), DBC(%d)",
                    mOpenId,
                    (*it).imageBuffer.get(),
                    (*it).metadata.count(),
                    mDequeBufCnt
                );
                //
                pImageBuffer = (*it).imageBuffer;
                (*pMetadata) = (*it).metadata;
                mlBufInfo.erase(it);
                break;
            }
        }
    }
    //
    FUNC_END;
    return MTRUE;
}
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
enqueBuf(sp<IImageBufferHeap> pImageBuffer)
{
    FUNC_START;
    Mutex::Autolock _l(mLock);
    //
    //pImageBuffer->setTimestamp(0);
    //
    BUF_INFO tempBuf;
    tempBuf.imageBuffer = pImageBuffer;
    tempBuf.setMetadata = false;
    tempBuf.isUsed = MTRUE;
    mlBufInfo.push_back(tempBuf);
    if(mDequeBufCnt == 0)
    {
        MY_LOGE("mDequeBufCnt is 0");
    }
    else
    {
        mDequeBufCnt--;
    }
    //
    mCondDequeBuf.signal();
    //
    MY_LOGD("Id(%d),Buf(0x%p),DBC(%d)",
            mOpenId,
            pImageBuffer.get(),
            mDequeBufCnt);
    //
    FUNC_END;
    return MTRUE;
}
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
popBuf(sp<IImageBufferHeap>& pImageBuffer)
{
    FUNC_START;

    Mutex::Autolock _l(mLock);
    list<BUF_INFO>::iterator it;
    MBOOL readyToPop = MFALSE;
    BUF_INFO popedBuf;

    // Check whether the buf is empty first
    if(mlBufInfo.empty())
    {
        if(mbUse)
        {
            MY_LOGW("buf is empty");
        }
        return MFALSE;
    }
    // itereate the list, keep erasing "orphan meta" until reach one buffer that is ready.
    while(mlBufInfo.size() > 0){
        popedBuf = mlBufInfo.front();
        mlBufInfo.pop_front();
        //
        if(popedBuf.imageBuffer == NULL && popedBuf.setMetadata == true){
            MY_LOGW("poor orphan Metadata for Req(%d) is left in the queue, just erase it", popedBuf.iReqNum);
            continue;
        }
        //
        if(popedBuf.imageBuffer != NULL){
            MY_LOGW("readyToPop");
            readyToPop = MTRUE;
            break;
        }
    }

    // ready to popBuf
    if(readyToPop){
        MY_LOGD("Id(%d),Buf(0x%p)",
            mOpenId,
            popedBuf.imageBuffer.get());

        pImageBuffer = popedBuf.imageBuffer;
        mPushedBufferCount--;
    }else{
        MY_LOGE("All the element in the list are not buffer ready. Should not have happended");
        return MFALSE;
    }

    FUNC_END;
    return MTRUE;
}
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
pushBuf(sp<IImageBufferHeap> pImageBuffer, MUINT32 iReqNum, MBOOL usable)
{
    FUNC_START;
    Mutex::Autolock _l(mLock);
    //
    MY_LOGD("Id(%d),Buf(0x%p), iReqNum(0x%d)",
            mOpenId,
            pImageBuffer.get(),
            iReqNum);
    //
    MBOOL isFound = MFALSE;
    list<BUF_INFO>::iterator it;

    for(it = mlBufInfo.begin(); it != mlBufInfo.end(); it++)
    {
        if((*it).iReqNum == iReqNum)
        {
            MY_LOGD("MetaExist Req(%d)",iReqNum);
            (*it).imageBuffer = pImageBuffer;
            (*it).isUsed = (!usable);
            isFound = MTRUE;
            break;
        }
    }
    //
    if(!isFound)
    {
        BUF_INFO tempBuf;
        tempBuf.imageBuffer = pImageBuffer;
        tempBuf.setMetadata = false;
        tempBuf.isUsed = (!usable);
        tempBuf.iReqNum = iReqNum;

        mlBufInfo.push_back(tempBuf);
    }
    mPushedBufferCount++;
    mCond.broadcast();
    if(!mbUse)
    {
        mbUse = MTRUE;
    }
    //
    FUNC_END;
    return MTRUE;
}

//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
getBufLen(MUINT32& bufLen, bufLenType lenType)
{
    MBOOL ret=MTRUE;
    MY_LOGD("lenType = %d", lenType);
    switch(lenType)
    {
        case BUF_LEN_CURRENT:
            bufLen = mlBufInfo.size();
            break;

        case BUF_LEN_MAX:
            bufLen = mDequeBufMaxNum;
            break;

        case BUF_LEN_LEFT:
            bufLen = mDequeBufMaxNum - mlBufInfo.size();
            break;
        /*
        case BUF_LEN_USED:
            {
                list<IImageBuffer*>::iterator it;
                bufLen = 0;
                for(it = mlpImgBuf.begin(); it != mlpImgBuf.end(); it++)
                {
                    if((*it)->getTimestamp() == 0)
                    {
                        bufLen++;
                    }
                }
            }
            break;
        */
        case BUF_LEN_PUSHED:
            bufLen = mPushedBufferCount;
            break;

        default:
            MY_LOGE("Not support this Length type (%d)", lenType);
            ret = MFALSE;
            break;

    }
    MY_LOGD("Id(%d), bufLen(%d)",mOpenId, bufLen);
    return ret;
}
//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
stopDequeBuf()
{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    mIsDequeBufStopped = MTRUE;

    mCond.broadcast();

    FUNC_END;
    return MTRUE;
}

//-----------------------------------------------------------------------------
MBOOL
StereoBufMgrImp::
startDequeBuf()
{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    mIsDequeBufStopped = MFALSE;

    mCond.broadcast();

    FUNC_END;
    return MTRUE;
}
//-----------------------------------------------------------------------------
};

