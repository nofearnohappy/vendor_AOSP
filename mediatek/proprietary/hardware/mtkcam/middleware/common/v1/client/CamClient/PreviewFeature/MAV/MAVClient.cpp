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
#define LOG_TAG "MtkCam/CamClient/MAVClient"

#include <DpBlitStream.h>
#include "MAVClient.h"
#include "exif/IBaseCamExif.h"
//
using namespace NSCamClient;

//
/******************************************************************************
*
*******************************************************************************/
MAVClient*  MAVClientObj;
sem_t       MAVAddImgDone;
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


/******************************************************************************
 *
 ******************************************************************************/
//#define debug
#ifdef debug
#include <fcntl.h>
#include <sys/stat.h>
bool
savedataToFile(char const*const fname, MUINT8 *const buf, MUINT32 const size)
{
    int nw, cnt = 0;
    uint32_t written = 0;

    MY_LOGD("(name, buf, size) = (%s, %x, %d)", fname, buf, size);
    MY_LOGD("opening file [%s]\n", fname);
    int fd = ::open(fname, O_RDWR | O_CREAT | O_TRUNC, S_IRWXU);
    if (fd < 0) {
        MY_LOGE("failed to create file [%s]: %s", fname, ::strerror(errno));
        return false;
    }

    MY_LOGD("writing %d bytes to file [%s]\n", size, fname);
    while (written < size) {
        nw = ::write(fd,
                     buf + written,
                     size - written);
        if (nw < 0) {
            MY_LOGE("failed to write to file [%s]: %s", fname, ::strerror(errno));
            break;
        }
        written += nw;
        cnt++;
    }
    MY_LOGD("done writing %d bytes to file [%s] in %d passes\n", size, fname, cnt);
    ::close(fd);
    return true;
}
#endif

/******************************************************************************
 *
 ******************************************************************************/
MAVClient::
MAVClient(int ShotNum)
    : MAVnum(ShotNum)
{
    MY_LOGD("+ this(%p) num %d", this,MAVnum);
    MAVClientObj = this;
    //  create algorithm object
    //mpMAVObj = hal3DFBase::createInstance(HAL_MAV_OBJ_NORMAL);
    if ( ! mpMAVObj )
    {
        MY_LOGE("[init] mpMAVObj==NULL \n");
    }
    allocator = IImageBufferAllocator::getInstance();
    userCount = 0;
}


/******************************************************************************
 *
 ******************************************************************************/
MAVClient::
~MAVClient()
{
    MY_LOGD("-");
}

/******************************************************************************
*
*******************************************************************************/
MBOOL
MAVClient::
allocMem(IImageBufferAllocator::ImgParam &memBuf, sp<IImageBuffer> &imgBuf)
{
    imgBuf = allocator->alloc("MAVBuffer", memBuf);
    if  ( imgBuf.get() == 0 )
    {
        MY_LOGE("NULL Buffer\n");
        return MFALSE;
    }

    if ( !imgBuf->lockBuf( "MAVBuffer", (eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_MASK) ) )
    {
        MY_LOGE("lock Buffer failed\n");
        return MFALSE;
    }
    imgBuf->syncCache(eCACHECTRL_INVALID);
    return MTRUE;
}

/******************************************************************************
*
*******************************************************************************/
MBOOL
MAVClient::
deallocMem(sp<IImageBuffer> imgBuf)
{
    if( !imgBuf->unlockBuf( "MAVBuffer" ) )
    {
        CAM_LOGE("unlock Buffer failed\n");
        return MFALSE;
    }

    allocator->free(imgBuf.get());

    return MTRUE;
}

#ifdef image_transfer_at_Pipe
/******************************************************************************
 *   NV21 to YV12
 ******************************************************************************/
MINT32
MAVClient::
ImageTrans(MVOID * srcbufadr, int ImgWidth, int ImgHeight, MVOID * dstbufadr)
{
    bool ret = true;
    MY_LOGD("[ImageTrans] +");
    DpBlitStream Transstream;

    unsigned int src_addr_list[3];
    unsigned int src_size_list[3];
    unsigned int dst_addr_list[3];
    unsigned int dst_size_list[3];

    unsigned char *src_yp;
    unsigned char *dst_yuv;

    int src_ysize = ImgWidth * ImgHeight;
    int src_usize, src_vsize;
    src_usize = src_vsize = src_ysize / 4;
    MY_LOGD("[ImageTrans] src_ysize %d adr 0x%x w %d h %d",src_ysize,srcbufadr,ImgWidth,ImgHeight);
    int plane_num = 2;
    src_yp = (unsigned char *)srcbufadr;
    src_addr_list[0] = (unsigned int)src_yp;
    src_addr_list[1] = (unsigned int)(src_yp + src_ysize);

    src_size_list[0] = src_ysize;
    src_size_list[1] = src_vsize+src_usize;

    //*****************************************************************************//
    Transstream.setSrcBuffer((void**)src_addr_list, src_size_list, 2);
    Transstream.setSrcConfig(ImgWidth,ImgHeight, DP_COLOR_NV21);

     //***************************dst yv12********************************//
    dst_yuv = (unsigned char *)dstbufadr;
    dst_addr_list[0] = (unsigned int)dst_yuv;
    dst_addr_list[1] = (unsigned int)(dst_yuv + src_ysize);
    dst_addr_list[2] = (unsigned int)(dst_yuv + src_ysize + src_usize);

    dst_size_list[0] = src_ysize;
    dst_size_list[1] = src_vsize;
    dst_size_list[2] = src_usize;

    Transstream.setDstBuffer((void**)dst_addr_list, dst_size_list,3);
    Transstream.setDstConfig(ImgWidth, ImgHeight, DP_COLOR_YV12);
    Transstream.setRotate(0);

    //*****************************************************************************//

     MY_LOGD("DDP_Performance_NV21 to YV12 Start");

    // set & add pipe to stream
    if (Transstream.invalidate()<0)  //trigger HW
    {
          MY_LOGD("FDstream invalidate failed");
          return false;
    }
    #ifdef debug
    char sourceFiles[80];
    sprintf(sourceFiles, "%s%d_%dx%d.yuv", "/sdcard/motion", Mcount,ImgWidth,ImgHeight);
    savedataToFile((char *) sourceFiles, (MUINT8 *)dst_yuv , (ImgWidth * ImgHeight*3>>1));
    Mcount++;
    #endif
    MY_LOGD("DDP_Performance_RGB565 End");
    return ret;
}

/******************************************************************************
 *   YV12 to NV21
 ******************************************************************************/
MINT32
MAVClient::
ImageTransBack(MVOID * srcbufadr, int ImgWidth, int ImgHeight, MVOID * dstbufadr)
{
    bool ret = true;
    MY_LOGD("[ImageTransBack] +");
    DpBlitStream Transstream;

    unsigned int src_addr_list[3];
    unsigned int src_size_list[3];
    unsigned int dst_addr_list[3];
    unsigned int dst_size_list[3];

    unsigned char *src_yp;
    unsigned char *dst_yuv;

    int src_ysize = ImgWidth * ImgHeight;
    int src_usize, src_vsize;
    src_usize = src_vsize = src_ysize / 4;
    MY_LOGD("[ImageTrans] src_ysize %d adr 0x%x w %d h %d",src_ysize,srcbufadr,ImgWidth,ImgHeight);

    src_yp = (unsigned char *)srcbufadr;
    src_addr_list[0] = (unsigned int)src_yp;
    src_addr_list[1] = (unsigned int)(src_yp + src_ysize);
    src_addr_list[2] = (unsigned int)(src_yp + src_ysize + src_usize);

    src_size_list[0] = src_ysize;
    src_size_list[1] = src_vsize;
    src_size_list[2] = src_usize;

    //*****************************************************************************//
    Transstream.setSrcBuffer((void**)src_addr_list, src_size_list, 3);
    Transstream.setSrcConfig(ImgWidth,ImgHeight, DP_COLOR_YV12);

     //***************************dst yv12********************************//
    dst_yuv = (unsigned char *)dstbufadr;
    dst_addr_list[0] = (unsigned int)dst_yuv;
    dst_addr_list[1] = (unsigned int)(dst_yuv + src_ysize);

    dst_size_list[0] = src_ysize;
    dst_size_list[1] = src_vsize+src_usize;

    Transstream.setDstBuffer((void**)dst_addr_list, dst_size_list,2);
    Transstream.setDstConfig(ImgWidth, ImgHeight, DP_COLOR_NV21);
    Transstream.setRotate(0);

    //*****************************************************************************//

     MY_LOGD("DDP_Performance_YV12 to NV21 Start");

    // set & add pipe to stream
    if (Transstream.invalidate()<0)  //trigger HW
    {
          MY_LOGD("FDstream invalidate failed");
          return false;
    }
    #ifdef debug
    char sourceFiles[80];
    sprintf(sourceFiles, "%s%d_%dx%d.yuv", "/sdcard/motion", Mcount,ImgWidth,ImgHeight);
    savedataToFile((char *) sourceFiles, (MUINT8 *)dst_yuv , (ImgWidth * ImgHeight*3>>1));
    Mcount++;
    #endif
    MY_LOGD("DDP_Performance_RGB565 End");
    return ret;
}
#endif

/******************************************************************************
 *
 ******************************************************************************/
bool
MAVClient::
init(int bufwidth,int bufheight)
{
    bool ret = MTRUE;   // MTRUE: no error. MFALSE: error.
    MINT32 err = NO_ERROR;
    //
    MY_LOGD("+");

    mMAVFrameWidth  = bufwidth;
    mMAVFrameHeight = bufheight;
    mMAVFrameSize   = ((mMAVFrameWidth * mMAVFrameHeight * 3 / 2) + DBG_EXIF_SIZE);
    mCancel = MTRUE;
    mStop = MFALSE;
    //
    // (1) Create frame buffer buffer
    MY_LOGD("mMAVFrameWidth %d mMAVFrameHeight %d mMAVFrameSize %d MAVnum %d DBG_EXIF_SIZE %d",mMAVFrameWidth,mMAVFrameHeight,mMAVFrameSize,MAVnum,DBG_EXIF_SIZE);
    IImageBufferAllocator::ImgParam imgParam(mMAVFrameSize,0);

    for(int i=0;i<MAVnum;i++)
    {
        if(!(allocMem(imgParam,mpframeBuffer[i])))
        {
            MY_LOGE("[init] mpframeBuffer alloc fail");
            ret = MFALSE;
            return  ret;
        }
        MY_LOGD("[init] mpframeBuffer alloc index %d adr 0x%x",i,(MUINT32)mpframeBuffer[i]->getBufVA(0));
    }
    // (2) Create working buffer buffer
    IImageBufferAllocator::ImgParam PanoMotionPara((MOTION_MAX_IN_WIDTH * MOTION_MAX_IN_HEIGHT * 3),0);
    if(!(allocMem(PanoMotionPara,mpMotionBuffer)))
    {
        MY_LOGE("[init] mpMotionBuffer alloc fail");
        ret = MFALSE;
        return  ret;
    }

    IImageBufferAllocator::ImgParam MavMotionPara((MOTION_MAX_IN_WIDTH * MOTION_MAX_IN_HEIGHT * 3),0);
    if(!(allocMem(MavMotionPara,mpMAVMotionBuffer)))
    {
        MY_LOGE("[init] mpMAVMotionBuffer alloc fail");
        ret = MFALSE;
        return  ret;
    }

    IImageBufferAllocator::ImgParam WarpPara((mMAVFrameWidth * mMAVFrameHeight * 2 + 2048),0);
    if(!(allocMem(WarpPara,mpWarpBuffer)))
    {
        MY_LOGE("[init] mpWarpBuffer alloc fail");
        ret = MFALSE;
        return  ret;
    }

    // (3) initial algorithm
    err = mpMAVObj->mHal3dfInit(NULL, (void*)mpMotionBuffer->getBufVA(0), (void*)mpWarpBuffer->getBufVA(0), (void*)mpMAVMotionBuffer->getBufVA(0));
    if ( err < 0 ) {
        MY_LOGE("mpMAVObj->mHalMavinit() Err");
        ret = MFALSE;
        return  ret;
    }
    MUINT32 WokeSize;
    mpMAVObj->mHal3dfGetWokSize(mMAVFrameWidth,mMAVFrameHeight,WokeSize);
    MY_LOGD("[init] working buffer size %d",WokeSize);
    if(WokeSize==0)
    {
       WokeSize=(mMAVFrameWidth * mMAVFrameHeight * 4 * 10);
    }
    IImageBufferAllocator::ImgParam MavWorkPara((WokeSize),0);
    if(!(allocMem(MavWorkPara,mpMAVWorkingBuf)))
    {
        MY_LOGE("[init] mpMAVWorkingBuf alloc fail");
        ret = MFALSE;
        return  ret;
    }

    err = mpMAVObj->mHal3dfSetWokBuff((void*)mpMAVWorkingBuf->getBufVA(0));
    if ( err < 0 ) {
        MY_LOGE("mpMAVObj->mHal3dfSetWokBuff() Err");
        ret = MFALSE;
        return  ret;
    }
    // (4) reset member parameter
    mMAVaddImgIdx = 0;
    mMAVFrameIdx = 0;

    // (5) thread create
    sem_init(&MAVSemThread, 0, 0);
    sem_init(&MAVmergeDone, 0, 0);
    sem_init(&MAVAddImgDone, 0, 0);
    pthread_create(&MAVFuncThread, NULL, MAVthreadFunc, this);
    //
    userCount++;
    MY_LOGD("-. ret: %d.", ret);
    return  ret;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
MAVClient::
uninit()
{
    // Skip
    if( userCount < 1 ){
        return  true;
    }

    bool ret = MTRUE;   // MTRUE: no error. MFALSE: error.
    MY_LOGD("+");

    for(int i=0;i<MAVnum;i++)
    {
        MY_LOGD("mpframeBuffer free %d adr 0x%x",i,mpframeBuffer[i]->getBufVA(0));
        if(!(deallocMem(mpframeBuffer[i])))
        {
            MY_LOGE("[uninit] mpframeBuffer alloc fail");
            ret = MFALSE;
            goto lb_Abnormal_Exit;
        }
    }
    MY_LOGD("mpframeBuffer free done");
    if(!(deallocMem(mpMAVWorkingBuf)))
    {
        MY_LOGE("[uninit] mpMAVWorkingBuf alloc fail");
        ret = MFALSE;
        goto lb_Abnormal_Exit;
    }
    MY_LOGD("mpMAVWorkingBuf free done");
    if(!(deallocMem(mpMAVMotionBuffer)))
    {
        MY_LOGE("[uninit] mpMAVMotionBuffer alloc fail");
        ret = MFALSE;
        goto lb_Abnormal_Exit;
    }
    MY_LOGD("mpMAVMotionBuffer free done");
    if(!(deallocMem(mpMotionBuffer)))
    {
        MY_LOGE("[uninit] mpMotionBuffer alloc fail");
        ret = MFALSE;
        goto lb_Abnormal_Exit;
    }
    MY_LOGD("mpMotionBuffer free done");
    if(!(deallocMem(mpWarpBuffer)))
    {
        MY_LOGE("[uninit] mpWarpBuffer alloc fail");
        ret = MFALSE;
        goto lb_Abnormal_Exit;
    }

lb_Abnormal_Exit:
   MY_LOGD("uninit mpMAVObj %d",mpMAVObj);
    if (mpMAVObj) {
        mpMAVObj->mHal3dfUninit();
        mpMAVObj->destroyInstance();
        mpMAVObj = NULL;
    }
    userCount--;
    MY_LOGD("-. ret: %d.", ret);
    return  ret;

}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
MAVClient::
setImgCallback(ImgDataCallback_t data_cb)
{
    MY_LOGD("(notify_cb)=(%p)", data_cb);
    mDataCb = data_cb;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
MAVClient::
stopFeature(int cancel)
{
    bool ret = MTRUE;   // MTRUE: no error. MFALSE: error.
    int err;    // NO_ERROR (0): no error. Others: error code.
    MY_LOGD("+");


    MY_LOGD("CAM_CMD_STOP_MAV, do merge %d mMAVaddImgIdx %d MAVnum %d", cancel, mMAVaddImgIdx, MAVnum);
    mCancel = cancel;
    mStop = MTRUE;
    sem_post(&MAVSemThread);
    pthread_join(MAVFuncThread, NULL);

    if (mMAVaddImgIdx == MAVnum)
    {
        err = mHalCamFeatureMerge();
        sem_post(&MAVmergeDone);
        if (err != NO_ERROR)
        {
            ret = MFALSE;
            goto lb_Abnormal_Exit;
        }

    }

lb_Abnormal_Exit:
    MY_LOGD("-. ret: %d.", ret);
    return  ret;

}

/*******************************************************************************
*
********************************************************************************/
MINT32
MAVClient::
mHalCamFeatureAddImg()
{
    MINT32 err = NO_ERROR;

    if (mMAVaddImgIdx >= MAVnum){
        return err;
    }

    if(!mCancel)
    {
        MY_LOGD("mHalCamPanoramaAddImg exit mCancel %d", mCancel);
        return err;
    }

    MY_LOGD("mHalCamMAVAddImg(): %d", mMAVaddImgIdx);

    MavPipeImageInfo ImageInfo;
    ImageInfo.ImgAddr = mpframeBuffer[mMAVaddImgIdx]->getBufVA(0);
    ImageInfo.Width = mMAVFrameWidth;
    ImageInfo.Height = mMAVFrameHeight;
    ImageInfo.ControlFlow = 0;

    ImageInfo.MotionValue[0] = mpMAVResult.ImageInfo[mMAVaddImgIdx].MotionValue[0];
    ImageInfo.MotionValue[1] = mpMAVResult.ImageInfo[mMAVaddImgIdx].MotionValue[1];

    mpMAVResult.ImageInfo[mMAVaddImgIdx].Width = mMAVFrameWidth;
    mpMAVResult.ImageInfo[mMAVaddImgIdx].Height = mMAVFrameHeight;
    mpMAVResult.ImageInfo[mMAVaddImgIdx].ImgAddr = ImageInfo.ImgAddr;

    MY_LOGD("ImgAddr 0x%x, Width %d, Height %d, Motion: %d %d",
             ImageInfo.ImgAddr, ImageInfo.Width, ImageInfo.Height,
             ImageInfo.MotionValue[0], ImageInfo.MotionValue[1]);
    #ifdef debug
    char sourceFiles[80];
    sprintf(sourceFiles, "%s%d.yuv", "/sdcard/addimg", mMAVaddImgIdx);
    savedataToFile((char *) sourceFiles, (MUINT8 *)ImageInfo.ImgAddr, ((mMAVFrameWidth*mMAVFrameHeight*3)>>1));
    #endif
    err = mpMAVObj->mHal3dfAddImg((MavPipeImageInfo*)&ImageInfo);
    if (err != NO_ERROR) {
        MY_LOGE("mHal3dfAddImg Err");
        return err;
    }

    mMAVaddImgIdx++;
    MY_LOGD("mHalCamMAVAddImg mMAVaddImgIdx %d MAVnum %d",mMAVaddImgIdx,MAVnum);
    // Do merge

    MY_LOGD("mHalCamMAVAddImg X");
    return err;
}


/*******************************************************************************
*
********************************************************************************/
MINT32
MAVClient::
mHalCamFeatureMerge()
{
    MY_LOGD("mHalCamMAVdoMerge");

    MINT32 err = NO_ERROR;
    sem_wait(&MAVAddImgDone);
    MY_LOGD("mHalMavMerge");
    err = mpMAVObj->mHal3dfMerge((MUINT32*)&mpMAVResult);
    if (err != NO_ERROR) {
        MY_LOGE("mHal3dfMerge Err");
        return err;
    }

    // Align 16 for 89 because JPEG Encoder needs to align 16.
    mpMAVResult.ClipWidth = (mpMAVResult.ClipWidth>>4)<<4;
    mpMAVResult.ClipHeight = (mpMAVResult.ClipHeight>>4)<<4;

    MavPipeImageInfo ImageInfo;

    ImageInfo.ImgAddr = mpframeBuffer[0]->getBufVA(0);
    ImageInfo.Width = mpMAVResult.ImageInfo[0].Width;
    ImageInfo.Height = mpMAVResult.ImageInfo[0].Height;
    ImageInfo.ClipX = mpMAVResult.ImageInfo[0].ClipX;
    ImageInfo.ClipY = mpMAVResult.ImageInfo[0].ClipY;
    MY_LOGD("[mHalCamMAVMakeMPO] 0x%x w %d h %d cx %d cy %d", ImageInfo.ImgAddr,ImageInfo.Width ,ImageInfo.Height, ImageInfo.ClipX, ImageInfo.ClipY);

    #ifdef image_transfer_at_Pipe
    // note!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // image sequence and warp matrix sequence is not match. it should be modify
    // note!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    IImageBufferAllocator::ImgParam tmpParam((mMAVFrameWidth * mMAVFrameHeight * 2),0);
    sp<IImageBuffer> tmpBuffer;
    if(!(allocMem(tmpParam,tmpBuffer)))
    {
        MY_LOGE("[mHalCamFeatureMerge] tmpBuffer alloc fail");
        err = MFALSE;
        return  err;
    }
    /*************************Image transfer form NV21 to YV12 **********************************************/
    MY_LOGD("ImageTrans Start");
    //char sourceFiles[80];
    //sprintf(sourceFiles, "%s%d_%dx%d.nv21", "/sdcard/beforewarp", 30,ImageInfo.Width,ImageInfo.Height);
    //savedataToFile((char *) sourceFiles, (MUINT8 *)mpframeBuffer[0]->getBufVA(0), ImageInfo.Width*ImageInfo.Height*3>>1);
    ImageTrans((void*)mpframeBuffer[0]->getBufVA(0),ImageInfo.Width,ImageInfo.Height,(void*)tmpBuffer->getBufVA(0));
    //sprintf(sourceFiles, "%s%d_%dx%d.yuv", "/sdcard/beforewarp", 31,ImageInfo.Width,ImageInfo.Height);
    //savedataToFile((char *) sourceFiles, (MUINT8 *)tmpBuffer->getBufVA(0), ImageInfo.Width*ImageInfo.Height*3>>1);
    for(int i=1;i<MAVnum;i++)
    {
       MY_LOGD("ImageTrans B i %d ",i);
        ImageTrans((void*)mpframeBuffer[i]->getBufVA(0),ImageInfo.Width,ImageInfo.Height,(void*)mpframeBuffer[i-1]->getBufVA(0));
        mpframeBuffer[i]->flushAllCache(eCACHECTRL_FLUSH);
    }
    memcpy((void*)mpframeBuffer[MAVnum-1]->getBufVA(0),(void*)tmpBuffer->getBufVA(0),(ImageInfo.Width*ImageInfo.Height*2));
    mpframeBuffer[MAVnum-1]->flushAllCache(eCACHECTRL_FLUSH);
    /*************************Image transfer form NV21 to YV12 **********************************************/
    #endif

    #ifdef debug
    for (MUINT8 i = 0; i < MAVnum; i++)
    {
        sprintf(sourceFiles, "%s%d_%dx%d.yuv", "/sdcard/beforewarp", i,ImageInfo.Width,ImageInfo.Height);
        savedataToFile((char *) sourceFiles, (MUINT8 *)(mpframeBuffer[i]->getBufVA(0)), ImageInfo.Width*ImageInfo.Height*3>>1);
    }
    #endif

    err = mpMAVObj->mHal3dfWarp(&ImageInfo,(MUINT32*)&mpMAVResult, MAVnum);
    if (err != NO_ERROR) {
        MY_LOGE("mHal3dfWarp Err");
        return err;
    }

    #ifdef debug
    for (MUINT8 i = 0; i < MAVnum; i++)
    {
        sprintf(sourceFiles, "%s%d_%dx%d.yuv", "/sdcard/afterwarp", i,mpMAVResult.ClipWidth,mpMAVResult.ClipHeight);
        savedataToFile((char *) sourceFiles, (MUINT8 *)(mpframeBuffer[i]->getBufVA(0)), mpMAVResult.ClipWidth*mpMAVResult.ClipHeight*3>>1);
    }
    #endif

    MUINT32 result;
    MUINT32 width;
    MUINT32 height;
    err = mpMAVObj->mHal3dfGetResult(result, width , height);
    if ( err < 0 )
        return err;
    mpMAVResult.ClipWidth = (MINT16)width;
    mpMAVResult.ClipHeight = (MINT16)height;
    MY_LOGD("mHalMavGetResult result x %d y %d",width , height);

    #ifdef image_transfer_at_Pipe
    /*************************Image transfer form YV12 to NV21 **********************************************/
    mpframeBuffer[MAVnum - 1]->flushAllCache(eCACHECTRL_INVALID);
    MY_LOGD("ImageTrans A %d ",MAVnum-1);
    ImageTransBack((void*)mpframeBuffer[MAVnum - 1]->getBufVA(0),mpMAVResult.ClipWidth,mpMAVResult.ClipHeight,(void*)tmpBuffer->getBufVA(0));
    for (MINT8 i = (MAVnum - 2); i >=0  ; i--)
    {
      MY_LOGD("ImageTrans A i %d ",i);
       mpframeBuffer[i]->flushAllCache(eCACHECTRL_INVALID);
       ImageTransBack((void*)mpframeBuffer[i]->getBufVA(0),mpMAVResult.ClipWidth,mpMAVResult.ClipHeight,(void*)(mpframeBuffer[i+1]->getBufVA(0)));
    }
    memcpy((void*)mpframeBuffer[0]->getBufVA(0),(void*)tmpBuffer->getBufVA(0),(ImageInfo.Width*ImageInfo.Height*2));
    /*************************Image transfer form YV12 to NV21 **********************************************/
    #endif

    #ifdef debug
    for (MUINT8 i = 0; i < MAVnum; i++)
    {
        sprintf(sourceFiles, "%s%d_%dx%d.nv21", "/sdcard/warpNV21", i,mpMAVResult.ClipWidth,mpMAVResult.ClipHeight);
        savedataToFile((char *) sourceFiles, (MUINT8 *)(mpframeBuffer[i]->getBufVA(0)), mpMAVResult.ClipWidth*mpMAVResult.ClipHeight*3>>1);
    }
    #endif
    return err;
}


/*******************************************************************************
*
********************************************************************************/
MINT32
MAVClient::
mHalCamFeatureCompress()
{
    MY_LOGD("[mHalCamFeatureCompress]");

    MINT32 err = NO_ERROR;

    // (1) confirm merge is done; so mutex is not necessary
    sem_wait(&MAVmergeDone);
    MY_LOGD("get MAVmergeDone semaphore");
    MY_LOGD("mHalCamFeatureCompress 0x%x 0x%x",mpframeBuffer[0]->getBufVA(0),mpframeBuffer[0].get());
    MY_LOGD("mHalCamFeatureCompress 0x%x 0x%x",mpframeBuffer,&mpframeBuffer);
    mDataCb((MVOID*)mpframeBuffer,mpMAVResult.ClipWidth , mpMAVResult.ClipHeight);

    return err;
}

/*******************************************************************************
*
********************************************************************************/
MVOID*
MAVClient::
MAVthreadFunc(void *arg)
{
    MY_LOGD("[MAVthreadFunc] +");

    ::prctl(PR_SET_NAME,"MavTHREAD", 0, 0, 0);

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

    int SemValue = 0;
    // loop for thread until access uninit state
    while(((!MAVClientObj->mStop)||(SemValue)))
    {
        MY_LOGD("Semaphone value: %d", SemValue);
        sem_wait(&MAVClientObj->MAVSemThread);
        MINT32 err = MAVClientObj->mHalCamFeatureAddImg();
        if (err != NO_ERROR) {
            MY_LOGE("[mHalCamFeatureAddImg] fail");
        }
        sem_getvalue(&MAVClientObj->MAVSemThread, &SemValue);
        MY_LOGD("[MAV][MAVthreadFunc]: after do merge SemValue %d",SemValue);
    }
    sem_post(&MAVAddImgDone);
    MY_LOGD("[MAVthreadFunc] -");
    return NULL;
}
