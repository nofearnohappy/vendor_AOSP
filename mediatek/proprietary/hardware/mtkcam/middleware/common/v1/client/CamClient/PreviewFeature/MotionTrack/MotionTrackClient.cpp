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
#define LOG_TAG "MtkCam/CamClient/MotionTrackClient"

#include "DpBlitStream.h"
#include "MotionTrackClient.h"

//
using namespace NSCamClient;

//
/******************************************************************************
*
*******************************************************************************/

MotionTrackClient*   MotionTrackClientObj;
/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

static inline MUINT32 _align_mcu(MUINT32 x)
{
    const MUINT32 MCU_ALIGN_LENGTH = 16;
    return (((x) + (MCU_ALIGN_LENGTH - 1)) / MCU_ALIGN_LENGTH) * MCU_ALIGN_LENGTH;
}

/******************************************************************************
 *
 ******************************************************************************/
//#define MOTIONTRACK_DEBUG
#ifdef MOTIONTRACK_DEBUG
#include <fcntl.h>
#include <sys/stat.h>
bool
static dumpBufToFile(char const*const fname, MUINT8 *const buf, MUINT32 const size)
{
    int nw, cnt = 0;
    uint32_t written = 0;

    CAM_LOGD("(name, buf, size) = (%s, %x, %d)", fname, buf, size);
    CAM_LOGD("opening file [%s]\n", fname);
    int fd = ::open(fname, O_RDWR | O_CREAT | O_TRUNC, S_IRWXU);
    if (fd < 0) {
        CAM_LOGE("failed to create file [%s]: %s", fname, ::strerror(errno));
        return false;
    }

    CAM_LOGD("writing %d bytes to file [%s]\n", size, fname);
    while (written < size) {
        nw = ::write(fd,
                     buf + written,
                     size - written);
        if (nw < 0) {
            CAM_LOGE("failed to write to file [%s]: %s", fname, ::strerror(errno));
            break;
        }
        written += nw;
        cnt++;
    }
    CAM_LOGD("done writing %d bytes to file [%s] in %d passes\n", size, fname, cnt);
    ::close(fd);
    return true;
}
#endif

/******************************************************************************
 *
 ******************************************************************************/
MotionTrackClient::
MotionTrackClient(int ShotNum)
    : MotionTrackNum(ShotNum)
{
    MY_LOGD("+ this(%p) num %d", this,MotionTrackNum);
    MotionTrackClientObj = this;
    allocator = IImageBufferAllocator::getInstance();
    if(!allocator)
     MY_LOGD("+ Memory allocator is Null");
}


/******************************************************************************
 *
 ******************************************************************************/
MotionTrackClient::
~MotionTrackClient()
{
    MY_LOGD("-");
}

/******************************************************************************
*
*******************************************************************************/
MBOOL
MotionTrackClient::
allocMem(IImageBufferAllocator::ImgParam &memBuf, sp<IImageBuffer> &pImgBuf)
{
    pImgBuf = allocator->alloc(LOG_TAG, memBuf);
    if  ( pImgBuf.get() == 0 )
    {
        MY_LOGE("NULL Buffer\n");
        return MFALSE;
    }

    if ( !pImgBuf->lockBuf( "MAVBuffer", (eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_MASK) ) )
    {
        MY_LOGE("lock Buffer failed\n");
        return MFALSE;
    }

    pImgBuf->syncCache(eCACHECTRL_INVALID);

    return MTRUE;
}

/******************************************************************************
*
*******************************************************************************/
MBOOL
MotionTrackClient::
deallocMem(sp<IImageBuffer> &pImgBuf)
{
    if( !pImgBuf->unlockBuf(LOG_TAG) )
    {
        CAM_LOGE("unlock Buffer failed\n");
        return MFALSE;
    }
   allocator->free(pImgBuf.get());

    return MTRUE;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
MotionTrackClient::
init(int bufwidth,int bufheight)
{
    bool ret = false;
    status_t status = NO_ERROR;
    //
    MY_LOGD("+");

    mMotionTrackFrameWidth  = bufwidth;
    mMotionTrackFrameHeight = bufheight;
    mMotionTrackFrameSize   =(mMotionTrackFrameWidth * mMotionTrackFrameHeight * 3 / 2);
    mMotionTrackThumbSize   =((mMotionTrackFrameWidth/8) * (mMotionTrackFrameHeight/8) * 3 / 2);
    mPreviewFrameCount = 0;
 mNumBlendImages = 0;
    mCancel = MFALSE;

    // (1) Create frame buffer buffer
    MY_LOGD("mMotionTrackFrameWidth %d mMotionTrackFrameHeight %d mMotionTrackFrameSize %d mMotionTrackThumbSize %d MotionTrackNum %d",mMotionTrackFrameWidth,mMotionTrackFrameHeight,mMotionTrackFrameSize,mMotionTrackThumbSize,MotionTrackNum);
    {
        IImageBufferAllocator::ImgParam imgParam((mMotionTrackFrameSize * MotionTrackNum),0);
        if(!(allocMem(imgParam, mpFrameBuffer)))
        {
            MY_LOGE("[init] mpFrameBuffer alloc fail");
            return false;
        }
    }
    {
        MUINT32 size = _align_mcu(mMotionTrackFrameWidth) * _align_mcu(mMotionTrackFrameHeight) * 3 / 2; //aligned width and height with MCU access to avoid JPEG translation fault
        IImageBufferAllocator::ImgParam imgParam(size,0);
        if(!(allocMem(imgParam, mpCallbackBuffer)))
        {
            MY_LOGE("[init] mpCallbackBuffer alloc fail");
            return false;
        }
    }

    // (2) create algorithm object
    mpMotionTrackObj = NULL;
    mpMotionTrackObj = halMOTIONTRACKBase::createInstance();
    if ( ! mpMotionTrackObj )
    {
        MY_LOGE("[init] mpMotionTrackObj==NULL \n");
        return false;
    }

    // (3) Initial algorithm
    MTKPipeMotionTrackEnvInfo mMotionTrackEnvInfo;
    mMotionTrackEnvInfo.SrcImgWidth = mMotionTrackFrameWidth ;
    mMotionTrackEnvInfo.SrcImgHeight = mMotionTrackFrameHeight;

    ret = mpMotionTrackObj->mHalMotionTrackInit(mMotionTrackEnvInfo);
    if (!ret) {
        MY_LOGE("mHalMotionTrackInit Err \n");
        return false;
    }

    // (4) Create working buffer buffer
    {
        IImageBufferAllocator::ImgParam imgParam((mMotionTrackThumbSize),0);
        if(!(allocMem(imgParam, mpThumbBuffer)))
        {
            MY_LOGE("[init] mpThumbBuffer alloc fail");
            return false;
        }
    }
    MINT32 initBufSize = 0;

    MTKPipeMotionTrackWorkBufInfo MotionTrackWorkBufInfo;
    ret = mpMotionTrackObj->mHalMotionTrackGetWorkSize(&MotionTrackWorkBufInfo);
    if (!ret) {
        MY_LOGE("mHalMotionTrackGetWorkSize Err \n");
        return false;
    }
    MY_LOGD("[init] algorithm working buffer size %d", MotionTrackWorkBufInfo.WorkBufSize);
    {
        IImageBufferAllocator::ImgParam imgParam((MotionTrackWorkBufInfo.WorkBufSize),0);
        if(!(allocMem(imgParam, mpMotionTrackWorkingBuf)))
        {
            MY_LOGE("[init] mpMotionTrackWorkingBuf alloc fail");
            return false;
        }
    }
    MotionTrackWorkBufInfo.WorkBufAddr = (MUINT8*) mpMotionTrackWorkingBuf->getBufVA(0);
    ret = mpMotionTrackObj->mHalMotionTrackSetWorkBuf(MotionTrackWorkBufInfo);
    if (!ret) {
        MY_LOGE("mHalMotionTrackSetWorkBuf Err \n");
        return false;
    }

    // (5) reset member parameter
    mMotionTrackaddImgIdx = 0;
    mMotionTrackFrameIdx = 0;

    // (6) thread create
    sem_init(&MotionTrackBlendDone, 0, 0);
    sem_init(&MotionTrackAddImgDone, 0, 0);

    //pthread_create(&MotionTrackFuncThread, NULL, MotionTrackthreadFunc, this);

    //
    ret = true;
    MY_LOGD("-");
    return  ret;
}


/******************************************************************************
 *
 ******************************************************************************/
bool
MotionTrackClient::
uninit()
{
    Mutex::Autolock lock(mLockUninit);

    MY_LOGD("+");

    if(!(deallocMem(mpFrameBuffer)))
    {
        MY_LOGE("[uninit] mpFrameBuffer dealloc fail");
        return  MFALSE;
    }
    if(!(deallocMem(mpCallbackBuffer)))
    {
        MY_LOGE("[uninit] mpCallbackBuffer dealloc fail");
        return  MFALSE;
    }
    if(!(deallocMem(mpMotionTrackWorkingBuf)))
    {
        MY_LOGE("[uninit] mpMotionTrackWorkingBuf dealloc fail");
        return  MFALSE;
    }
    if(!(deallocMem(mpThumbBuffer)))
    {
        MY_LOGE("[uninit] mpThumbBuffer dealloc fail");
        return  MFALSE;
    }

    if (mpMotionTrackObj) {
        mpMotionTrackObj->mHalMotionTrackUninit();
        mpMotionTrackObj->destroyInstance();
        mpMotionTrackObj = NULL;
    }

    MY_LOGD("-");
    return  true;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
MotionTrackClient::
setImgCallback(ImgDataCallback_t data_cb)
{
    MY_LOGD("(notify_cb)=(%p)", data_cb);
    mDataCb = data_cb;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
MotionTrackClient::
stopFeature(int cancel)
{
    MY_LOGD("+");
    Mutex::Autolock lock(mLockUninit);

    bool ret = false;
   int err;
    MY_LOGD("CAM_CMD_STOP_MOTIONTRACK%s, mMotionTrackaddImgIdx %d MotionTrackNum %d",
                    (cancel == true)? " (cancel)": "",mMotionTrackaddImgIdx, MotionTrackNum);
    mCancel = MTRUE;

    /*
    sem_post(&MotionTrackSemThread);
    pthread_join(MotionTrackFuncThread, NULL);
    */
    if(mpMotionTrackObj)
    {
        if ((cancel == true) || (mMotionTrackaddImgIdx > 1))
        {
            // Do merge
            MY_LOGD("  CAM_CMD_STOP_MOTIONTRACK: Merge Accidently ");
            if (mHalCamFeatureBlend() != true)
            {
                MY_LOGD("  mHalCamFeatureBlend fail");
            }
        }
        else
        {
            MY_LOGD("  CAM_CMD_STOP_MOTIONTRACK: Cancel");
        }
    }
    else
    {
       MY_LOGE("CAM_CMD_STOP_MOTIONTRACK fail: mpMotionTrackObj is NULL");
    }
    sem_post(&MotionTrackBlendDone);

    MY_LOGD("-");
    return  true;
}

MINT32
MotionTrackClient::
ConvertNV21toYV12(MVOID * srcbufadr, int ImgWidth, int ImgHeight, MVOID * dstbufadr)
{
    bool ret = true;
    MY_LOGD("[ConvertNV21toYV12] +");
    DpBlitStream Motionstream;

    MINTPTR src_addr_list[2];
    unsigned int src_size_list[2];

    unsigned char *src_yp;

    int src_ysize = ImgWidth * ImgHeight;
    int src_uvsize;
    src_uvsize = src_ysize / 2;
    MY_LOGD("[ConvertNV21toYV12] src_ysize %d adr 0x%x w %d h %d",src_ysize,srcbufadr,ImgWidth,ImgHeight);
    int plane_num = 2;
    src_yp = (unsigned char *)srcbufadr;
    src_addr_list[0] = (MINTPTR)src_yp;
    src_addr_list[1] = (MINTPTR)(src_yp + src_ysize);

    src_size_list[0] = src_ysize;
    src_size_list[1] = src_uvsize;
    //*****************************************************************************//
    Motionstream.setSrcBuffer((void**)src_addr_list, src_size_list, plane_num);
    Motionstream.setSrcConfig(ImgWidth,ImgHeight, DP_COLOR_NV21);

    //***************************dst YV12********************************//
    int dst_ysize = ImgWidth * ImgHeight;
    int dst_usize, dst_vsize;
    dst_usize = dst_vsize = dst_ysize / 4;
    MINTPTR dst_addr_list[3];
    unsigned int dst_size_list[3];
    plane_num = 3;
    dst_addr_list[0] = (MINTPTR)dstbufadr;
    dst_addr_list[1] = (MINTPTR)(dstbufadr + dst_ysize);
    dst_addr_list[2] = (MINTPTR)(dstbufadr + dst_ysize + dst_usize);

    dst_size_list[0] = dst_ysize;
    dst_size_list[1] = dst_vsize;
    dst_size_list[2] = dst_usize;
    Motionstream.setDstBuffer((void **)dst_addr_list, dst_size_list, plane_num);
    Motionstream.setDstConfig(ImgWidth, ImgHeight, DP_COLOR_YV12);
    Motionstream.setRotate(0);

    // set & add pipe to stream
    if (Motionstream.invalidate())  //trigger HW
    {
          MY_LOGD("[ConvertNV21toYV12] FDstream invalidate failed");
          return false;
    }
    return ret;
}

MINT32
MotionTrackClient::
ConvertYV12toNV21(MVOID * srcbufadr, int ImgWidth, int ImgHeight, MVOID * dstbufadr)
{
    bool ret = true;
    MY_LOGD("[ConvertYV12toNV21] +");
    DpBlitStream Motionstream;

    //***************************src YV12********************************//
    int src_ysize = ImgWidth * ImgHeight;
    int src_usize, src_vsize;
    src_usize = src_vsize = src_ysize / 4;
    MINTPTR src_addr_list[3];
    unsigned int src_size_list[3];
    int plane_num = 3;
    src_addr_list[0] = (MINTPTR)srcbufadr;
    src_addr_list[1] = (MINTPTR)(srcbufadr + src_ysize);
    src_addr_list[2] = (MINTPTR)(srcbufadr + src_ysize + src_usize);

    src_size_list[0] = src_ysize;
    src_size_list[1] = src_vsize;
    src_size_list[2] = src_usize;
    Motionstream.setSrcBuffer((void **)src_addr_list, src_size_list, plane_num);
    Motionstream.setSrcConfig(ImgWidth, ImgHeight, DP_COLOR_YV12);

    //***************************dst NV21********************************//
    int dst_ysize = ImgWidth * ImgHeight;
    int dst_uvsize;
    dst_uvsize = dst_ysize / 2;
    MINTPTR dst_addr_list[2];
    unsigned int dst_size_list[2];
    plane_num = 2;
    dst_addr_list[0] = (MINTPTR)dstbufadr;
    dst_addr_list[1] = (MINTPTR)(dstbufadr + dst_ysize);

    dst_size_list[0] = dst_ysize;
    dst_size_list[1] = dst_uvsize;
    //*****************************************************************************//
    Motionstream.setDstBuffer((void**)dst_addr_list, dst_size_list, plane_num);
    Motionstream.setDstConfig(ImgWidth,ImgHeight, DP_COLOR_NV21);
    Motionstream.setRotate(0);

    // set & add pipe to stream
    if (Motionstream.invalidate())  //trigger HW
    {
          MY_LOGD("[ConvertYV12toNV21] FDstream invalidate failed");
          return false;
    }
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32
MotionTrackClient::
mHalCamFeatureBlend()
{
    //sem_wait(&MotionTrackAddImgDone);
    MY_LOGD("mHalCamFeatureBlend");

    /* Get selected image information */
    MTKPipeMotionTrackSelectImageInfo MotionTrackSelectImageInfo;
    if ((mpMotionTrackObj->mHalMotionTrackSelectImage(&MotionTrackSelectImageInfo) != true) ||
        (MotionTrackSelectImageInfo.NumCandidateImages > MOTIONTRACK_MAX_NUM_OF_BLENDING_IMAGES) ||
        (MotionTrackSelectImageInfo.NumCandidateImages == 0))
    {
        MY_LOGE("mHalCamFeatureBlend: mHalMotionTrackSelectImage failed");
        return false;
    }
    for (int i = 0; i < MotionTrackSelectImageInfo.NumCandidateImages; i++)
    {
        if (MotionTrackSelectImageInfo.CandidateImageIndex[i] > mMotionTrackaddImgIdx)
        {
            MY_LOGE("mHalCamFeatureBlend: mHalMotionTrackSelectImage failed");
            return false;
        }
    }

    /* Get intermediate data */
    MTKPipeMotionTrackIntermediateData MotionTrackIntermediateData;
    if (mpMotionTrackObj->mHalMotionTrackGetIntermediateDataSize(&MotionTrackIntermediateData) != true)
    {
        MY_LOGE("mHalCamFeatureBlend: mHalMotionTrackGetIntermediateDataSize failed");
        return false;
    }
    sp<IImageBuffer>  mpIntermediateDataBuffer;
    {
        IImageBufferAllocator::ImgParam imgParam((MotionTrackIntermediateData.DataSize),0);
        if(!(allocMem(imgParam, mpIntermediateDataBuffer)))
        {
            MY_LOGE("mHalCamFeatureBlend: mpIntermediateDataBuffer alloc fail");
            return false;
        }
        MotionTrackIntermediateData.DataAddr = (void*) mpIntermediateDataBuffer->getBufVA(0);
    }
    if (mpMotionTrackObj->mHalMotionTrackGetIntermediateData(MotionTrackIntermediateData) != true)
    {
        MY_LOGE("mHalCamFeatureBlend: mHalMotionTrackGetIntermediateData failed");
        deallocMem(mpIntermediateDataBuffer);
        return false;
    }
    MINTPTR cbdata[3];
    cbdata[0] = 3;  /* Callback on Intermediate Data */
    cbdata[1] = mpIntermediateDataBuffer->getBufSizeInBytes(0);
    cbdata[2] = mpIntermediateDataBuffer->getBufVA(0);
    mDataCb((MVOID*) cbdata, 0, 0);
    if(!(deallocMem(mpIntermediateDataBuffer)))
    {
        MY_LOGE("mHalCamFeatureBlend: mpIntermediateDataBuffer dealloc failed");
    }

    /* Start to blend */
    MTKPipeMotionTrackBlendImageInfo MotionTrackBlendImageInfo;
 MotionTrackBlendImageInfo.NumBlendImages = MotionTrackSelectImageInfo.NumCandidateImages;
    {
        MUINT32 alignedFrameSize = _align_mcu(mMotionTrackFrameWidth) * _align_mcu(mMotionTrackFrameHeight) * 3 / 2; //Align width and height with MCU access to avoid JPEG translation fault
        IImageBufferAllocator::ImgParam imgParam((mMotionTrackFrameSize * (MotionTrackBlendImageInfo.NumBlendImages + 1)),0); /* one more buffer for format conversion */
        if(!(allocMem(imgParam, mpBlendBuffer)))
        {
            MY_LOGE("mHalCamFeatureBlend: mpBlendBuffer alloc fail");
            return false;
        }
    }
    for (int i = 0; i < MotionTrackBlendImageInfo.NumBlendImages; i++)
    {
        MotionTrackBlendImageInfo.BlendImageIndex[i] = MotionTrackSelectImageInfo.CandidateImageIndex[i];
        MotionTrackBlendImageInfo.SrcImageAddr[i] = (MUINT8*) (mpFrameBuffer->getBufVA(0) + (mMotionTrackFrameSize * MotionTrackSelectImageInfo.CandidateImageIndex[i]));
        MotionTrackBlendImageInfo.ResultImageAddr[i] = (MUINT8*) (mpBlendBuffer->getBufVA(0) + (mMotionTrackFrameSize * (i + 1)));

        #if 0//def MOTIONTRACK_DEBUG
        char sourceFiles[80];
        sprintf(sourceFiles, "%s_%dx%d_%02d.yuv", "/sdcard/slctd", mMotionTrackFrameWidth, mMotionTrackFrameHeight, i);
        dumpBufToFile((char *) sourceFiles, MotionTrackBlendImageInfo.SrcImageAddr[i], mMotionTrackFrameSize);
        #endif
    }
    MTKPipeMotionTrackResultImageInfo MotionTrackResultImageInfo;
    if (mpMotionTrackObj->mHalMotionTrackBlendImage(MotionTrackBlendImageInfo, &MotionTrackResultImageInfo) != true)
    {
        MY_LOGE("mHalCamFeatureBlend: mHalMotionTrackBlendImage failed");
        deallocMem(mpBlendBuffer);
        return false;
    }
    mNumBlendImages = MotionTrackBlendImageInfo.NumBlendImages;
    mMotionTrackOutputWidth = MotionTrackResultImageInfo.OutputImgWidth;
    mMotionTrackOutputHeight = MotionTrackResultImageInfo.OutputImgHeight;
    MY_LOGD("mHalCamFeatureBlend: Output width %d, height %d", mMotionTrackOutputWidth, mMotionTrackOutputHeight);

    #ifdef MOTIONTRACK_DEBUG
    char sourceFiles[80];
    for (int i = 0; i < mNumBlendImages; i++)
    {
        sprintf(sourceFiles, "%s_%dx%d_%d.yuv", "/sdcard/Blend", mMotionTrackOutputWidth, mMotionTrackOutputHeight, i);
        dumpBufToFile((char *) sourceFiles, (MUINT8*) (mpBlendBuffer->getBufVA(0) + (mMotionTrackFrameSize * (i + 1))), mMotionTrackFrameSize);
    }
    #endif

    mpBlendBuffer->syncCache(eCACHECTRL_FLUSH);
    /* Convert from YV12 to NV21 */
    for (int i = 0; i < mNumBlendImages; i++)
    {
        ConvertYV12toNV21((MVOID*) (mpBlendBuffer->getBufVA(0) + (mMotionTrackFrameSize * (i + 1))),
                          mMotionTrackOutputWidth,
                          mMotionTrackOutputHeight,
                          (MVOID*) (mpBlendBuffer->getBufVA(0) + (mMotionTrackFrameSize * i)));
    }
    mpBlendBuffer->syncCache(eCACHECTRL_INVALID);

    return true;
}


/*******************************************************************************
*
********************************************************************************/
MINT32
MotionTrackClient::
mHalCamFeatureCompress()
{
    MY_LOGD("[mHalCamFeatureCompress]");

    MINT32 err = NO_ERROR;

    // (1) confirm merge is done; so mutex is not necessary

    sem_wait(&MotionTrackBlendDone);
    MY_LOGD("get MotionTrackBlendDone semaphore");

    if (mNumBlendImages)
    {
        MINTPTR cbdata[5];
        cbdata[0] = 2;  /* Callback on Blended Images */
        cbdata[2] = mNumBlendImages;
        for (int i = 0; i < mNumBlendImages; i++)
        {
            cbdata[1] = i;
            cbdata[4] = mpBlendBuffer->getBufVA(0) + (mMotionTrackFrameSize * i);
            mDataCb((MVOID*) cbdata, mMotionTrackOutputWidth, mMotionTrackOutputHeight);
        }

        if(!(deallocMem(mpBlendBuffer)))
        {
            MY_LOGE("mpBlendBuffer dealloc fail");
        }
    }
    else
    {
        /* No blended images */
        MINTPTR cbdata[3];
        cbdata[0] = 2;  /* Callback on Blended Images */
        cbdata[1] = 0;
        cbdata[2] = 0;
        mDataCb((MVOID*) cbdata, mMotionTrackFrameWidth, mMotionTrackFrameHeight);
    }

    return err;
}

#if 0
/*******************************************************************************
*
********************************************************************************/
MVOID*
MotionTrackClient::
MotionTrackthreadFunc(void *arg)
{
    MY_LOGD("[MotionTrackthreadFunc] +");

    ::prctl(PR_SET_NAME,"PanoTHREAD", 0, 0, 0);

    // (1) set policy/priority
    int const policy    = SCHED_OTHER;
    int const priority  = 0;
    //
    //
    struct sched_param sched_p;
    ::sched_getparam(0, &sched_p);
    sched_p.sched_priority = priority;  //  Note: "priority" is nice value
    sched_setscheduler(0, policy, &sched_p);
    setpriority(PRIO_PROCESS, 0, priority);
    //
    //  get
    ::sched_getparam(0, &sched_p);
    //
    MY_LOGD(
        "policy:(expect, result)=(%d, %d), priority:(expect, result)=(%d, %d)"
        , policy, ::sched_getscheduler(0)
        , priority, sched_p.sched_priority
    );

    // loop for thread until access uninit state
    while(!MotionTrackClientObj->mCancel)
    {
        MY_LOGD("[MotionTrack][MotionTrackthreadFunc]: wait thread");
        int SemValue;
        sem_getvalue(&MotionTrackClientObj->MotionTrackSemThread, &SemValue);
        MY_LOGD("Semaphone value: %d", SemValue);
        sem_wait(&MotionTrackClientObj->MotionTrackSemThread);
        MY_LOGD("get MotionTrackSemThread Semaphone");
        MINT32 err = MotionTrackClientObj->mHalCamFeatureAddImg();
        if (err != NO_ERROR) {
             MY_LOGD("[mHalCamFeatureAddImg] fail");
        }
        MY_LOGD("[MotionTrack][MotionTrackthreadFunc]: after do merge");
    }
    sem_post(&MotionTrackAddImgDone);
    MY_LOGD("[MotionTrackthreadFunc] -");
    return NULL;
}
#endif
