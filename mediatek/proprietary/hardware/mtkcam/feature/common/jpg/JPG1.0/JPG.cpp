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

#define LOG_TAG "MtkCam/JPG"
//
#include "JPG.h"
#include "include/SDK_JPG.h"
#include <exif/IBaseCamExif.h>
#include <exif/StdExif.h>
#include <cutils/properties.h>  // For property_get().
#include <hardware/camera3.h> // For JPG File End data

#include <feature/common/utils/BufAllocUtil.h>

#define ENABLE_DEBUG_INFO     (0)

using namespace NSCam;
using namespace NSIoPipe;

using namespace std;


Mhal_jpg::
Mhal_jpg()
{
}

MBOOL
Mhal_jpg::
init()
{
	CAM_LOGD("[%s] +", __FUNCTION__);
	CAM_LOGD("[%s] -", __FUNCTION__);
	
    return OK;
}


MBOOL DOJPG_encode(my_encode_params& rParams)
{
    //
    MBOOL ret = MTRUE;
    //
    ALOGD("[%s] +", __FUNCTION__);
    NSSImager::ISImager* pSImager = NSSImager::ISImager::createInstance(rParams.pSrc);
    if( pSImager == NULL ) {
        CAM_LOGD("[%s] create SImage failed",__FUNCTION__);
        return MFALSE;
    }

    ret = pSImager->setTargetImgBuffer(rParams.pDst)

        && pSImager->setTransform(rParams.transform)

        && pSImager->setCropROI(rParams.crop)

        && pSImager->setEncodeParam(
                rParams.isSOI,
                rParams.quality,
                rParams.codecType
                )
        && pSImager->execute();

    pSImager->destroyInstance();
    pSImager = NULL;
    //
    if( !ret ) {
        ALOGD("[%s] encode failed",__FUNCTION__);
        return MFALSE;
    }
    //
    CAM_LOGD("[%s] -", __FUNCTION__);
    return OK;
}

MBOOL
Mhal_jpg::
SDKcreateJpegImg(IImageBuffer* JPGSrcImgBuffer, IImageBuffer* JPGDstImgBuffer, ExifParams* exifPara)
{

    MBOOL ret = MFALSE;

    my_encode_params params;
	CAM_LOGD("[%s] picture size w = %d h = %d", __FUNCTION__ ,JPGSrcImgBuffer->getImgSize().w, JPGSrcImgBuffer->getImgSize().h);
    IImageBuffer* jpegBuf = BufAllocUtil::getInstance().allocMem(LOG_TAG, eImgFmt_JPEG, JPGSrcImgBuffer->getImgSize().w, JPGSrcImgBuffer->getImgSize().h);

    // ==== Create Exif Parameters first ====

    ExifParams stdParams;
    ExifParams *pExifPara;
    if( exifPara == NULL || !exifPara)
    {
        
        memset(&stdParams, 0, sizeof(ExifParams));
        stdParams.u4ImageWidth = JPGSrcImgBuffer->getImgSize().w;       // Image width
        stdParams.u4ImageHeight = JPGSrcImgBuffer->getImgSize().h;      // Image height
        stdParams.u4FNumber = 28;// Format: F2.8 = 28
        stdParams.u4FocalLength =  350;// Format: FL 3.5 = 350
        stdParams.u4Facing  = 0;//(muFacing == MTK_LENS_FACING_BACK) ? 0 : 1;
        stdParams.u4Orientation  = 0;//jpgParams.Rotation;
        pExifPara = &stdParams;
    }
    else
    {
        
        exifPara->u4ImageWidth = JPGSrcImgBuffer->getImgSize().w;       // Image width
        exifPara->u4ImageHeight = JPGSrcImgBuffer->getImgSize().h;      // Image height
        pExifPara = exifPara;
    }

    CAM_LOGD("[%s] exif FocalLeng = %d, u4FNumber=%d", __FUNCTION__, pExifPara->u4FocalLength, pExifPara->u4FNumber);

    // ===== Encode Src to JPG =====

    params.pSrc = JPGSrcImgBuffer;
    params.pDst = jpegBuf;
    params.transform = 0; //TODO
    params.crop =MRect(MPoint(0,0), JPGSrcImgBuffer->getImgSize());
    params.isSOI = 0;
    params.quality = 80;
    params.codecType = NSSImager::JPEGENC_HW_FIRST;//NSSImager::JPEGENC_SW;//NSSImager::JPEGENC_HW_FIRST;//JPEGENC_SW JPEGENC_HW_FIRST


    CAM_LOGD("[%s] +", __FUNCTION__);

    ret = DOJPG_encode(params);
    if(ret != OK) {
        CAM_LOGD("[%s] SDKJPG main JPG is fail \n",__FUNCTION__);
    }
	
	IImageBuffer* mpPostviewImgBuf = BufAllocUtil::getInstance().allocMem(LOG_TAG, eImgFmt_YV12, 160, 128);

    ImgProcess(JPGSrcImgBuffer, JPGSrcImgBuffer->getImgSize().w, JPGSrcImgBuffer->getImgSize().h, eImgFmt_YV12, mpPostviewImgBuf, mpPostviewImgBuf->getImgSize().w, mpPostviewImgBuf->getImgSize().h, eImgFmt_YV12);

    IImageBuffer* thumbBuf = BufAllocUtil::getInstance().allocMem(LOG_TAG, eImgFmt_JPEG, 
                                    mpPostviewImgBuf->getImgSize().w, 
                                    mpPostviewImgBuf->getImgSize().h);
    params.pSrc = mpPostviewImgBuf;
    params.pDst = thumbBuf;
    params.transform = 0; //TODO
    params.crop =MRect(MPoint(0,0), mpPostviewImgBuf->getImgSize());
    params.isSOI = 1;
    params.quality = 80;
    params.codecType = NSSImager::JPEGENC_SW;//NSSImager::JPEGENC_HW_FIRST;//JPEGENC_SW JPEGENC_HW_FIRST

    ret = DOJPG_encode(params);
    if(ret != OK) {
        ALOGE("[%s] SDKJPG thumbnail is fail \n",__FUNCTION__);
    }

#ifdef Debug_Mode
    jpegBuf->saveToFile("/sdcard/MainResult.jpg");
    CAM_LOGD("[SDKcreateFBJpegImg] Main JPG: w = %d h = %d BitstreamSize = %d",  JPGSrcImgBuffer->getImgSize().w, JPGSrcImgBuffer->getImgSize().h,jpegBuf->getBitstreamSize());

    thumbBuf->saveToFile("/sdcard/ThumbResult.jpg");
    ALOGD("[SDKcreateFBJpegImg] Thumbnail JPG: w = %d h = %d BitstreamSize = %d",  mpPostviewImgBuf->getImgSize().w, mpPostviewImgBuf->getImgSize().h,thumbBuf->getBitstreamSize());

    jpegBuf->syncCache(eCACHECTRL_INVALID);
    thumbBuf->syncCache(eCACHECTRL_INVALID);
#endif

    CAM_LOGD("[%s] in create exif +",__FUNCTION__);

    MINT8 *pExifBuf = new MINT8[DBG_EXIF_SIZE];

    StdExif exif;//[++]name
    size_t exifSize  = exif.getHeaderSize();//EXIF_HEADER_SIZE  
    size_t thumbnailMaxSize = 0;

    thumbnailMaxSize = thumbBuf->getBitstreamSize();


    exif.init(*pExifPara, ENABLE_DEBUG_INFO);

    exif.setMaxThumbnail(thumbnailMaxSize);

    exif.make((MUINTPTR)pExifBuf,exifSize);

    exif.uninit();

    CAM_LOGD("[%s] create exif - : exifSize = %d thumbnailMaxSize = %d",__FUNCTION__,exifSize,thumbnailMaxSize);

    int outputsize = jpegBuf->getBitstreamSize() + thumbBuf->getBitstreamSize() + exifSize;
    MINT8 *pJpegBuf = (MINT8 *)JPGDstImgBuffer->getBufVA(0);//(MINT8 *)tempJpegBuf->getBufVA(0);

    // out buffer
    size_t const jpegBufSize = jpegBuf->getBitstreamSize()+ exifSize;
    size_t jpegsize_final    = 0;

    CAM_LOGD("[%s] integrate JPG +",__FUNCTION__);
    // main jpeg
    void* pMainJpegAddr = NULL;
    size_t mainJpegSize = 0;

    //thumb
    void* pThumbJpegAddr = NULL;
    size_t ThumbJpegSize = 0;

    pMainJpegAddr = reinterpret_cast<void*>(jpegBuf->getBufVA(0));
    mainJpegSize = jpegBuf->getBitstreamSize();

    pThumbJpegAddr = reinterpret_cast<void*>(thumbBuf->getBufVA(0));
    ThumbJpegSize = thumbBuf->getBitstreamSize();
   
    //1. copy header
    memcpy(pJpegBuf, pExifBuf, exifSize);
    jpegsize_final += exifSize;

    //2. copy thumb
    memcpy(pJpegBuf + jpegsize_final, pThumbJpegAddr, ThumbJpegSize );
    jpegsize_final += ThumbJpegSize;

    //3.copy jpeg
    memcpy(pJpegBuf + jpegsize_final, pMainJpegAddr, mainJpegSize );
    jpegsize_final += mainJpegSize;

    //4.add end information
    camera3_jpeg_blob jpeg_end;
    size_t tempjpegBufSize = JPGDstImgBuffer->getImgSize().w;
    jpeg_end.jpeg_blob_id = CAMERA3_JPEG_BLOB_ID;
    jpeg_end.jpeg_size	  = jpegsize_final;
	//
    memcpy(pJpegBuf + tempjpegBufSize - sizeof(camera3_jpeg_blob),
            &jpeg_end,
            sizeof(camera3_jpeg_blob)
		  );


    CAM_LOGD("[%s] integrate JPG - : jpegsize_final = %d %d",__FUNCTION__,jpegsize_final,JPGDstImgBuffer->getImgSize().w);

#ifdef Debug_Mode
    //saveBufToFile("/sdcard/test.jpg", (uint8_t*)pJpegBuf, jpegsize_final);
    JPGDstImgBuffer->saveToFile("/sdcard/result.JPG");
    //saveBufToFile("/sdcard/sdkfb.jpg", (uint8_t*)mDstImgBuffer->getBufVA(0), jpegsize_final);
#endif
	
    delete [] pExifBuf;

    BufAllocUtil::getInstance().deAllocMem(LOG_TAG, jpegBuf);
    BufAllocUtil::getInstance().deAllocMem(LOG_TAG, thumbBuf);
	BufAllocUtil::getInstance().deAllocMem(LOG_TAG, mpPostviewImgBuf);
    CAM_LOGD("[%s] -", __FUNCTION__);

    return ret;
}


MBOOL
Mhal_jpg::
ImgProcess(IImageBuffer const* Srcbufinfo, MUINT32 srcWidth, MUINT32 srcHeight, EImageFormat srctype, IImageBuffer const* Desbufinfo, MUINT32 desWidth, MUINT32 desHeight, EImageFormat destype, MUINT32 transform) const
{
	CAM_LOGD("[Resize] srcAdr 0x%p srcWidth %d srcHeight %d desAdr 0x%p desWidth %d desHeight %d ",Srcbufinfo,Srcbufinfo->getImgSize().w,Srcbufinfo->getImgSize().h,Desbufinfo,Desbufinfo->getImgSize().w,Desbufinfo->getImgSize().h);
	
	((IImageBuffer*)Srcbufinfo)->syncCache(eCACHECTRL_FLUSH);
	
	NSCam::NSIoPipe::NSSImager::ISImager *mpISImager = NSCam::NSIoPipe::NSSImager::ISImager::createInstance(Srcbufinfo);
	if (mpISImager == NULL)
	{
		CAM_LOGD("Null ISImager Obj \n");
		return MFALSE;
	}

	//
	mpISImager->setTargetImgBuffer(Desbufinfo);
	//
	mpISImager->setTransform(transform);
	//
	//mpISImager->setFlip(0);
	//
	//mpISImager->setResize(desWidth, desHeight);
	//
	mpISImager->setEncodeParam(1, 90);
	//
	//mpISImager->setROI(Rect(0, 0, srcWidth, srcHeight));
	//
	mpISImager->execute();
	//Sava Test
#ifdef Debug_Mode
	//if(count==0)
	{
	   CAM_LOGD("Save resize file");
	   char szFileName[100];
	   ::sprintf(szFileName, "/sdcard/imgprc_%d_%d_%d_%d_%d.yuv", (int)srctype, (int)destype,srcWidth,desWidth,count);
	   Desbufinfo->saveToFile(szFileName);
	   CAM_LOGD("Save resize file done");
	}
	count++;
#endif
	CAM_LOGD("[Resize] Out");
	return	MTRUE;
}

void
SDK_jpg::
init(void)
{
    CAM_LOGD("[%s] +", __FUNCTION__);
    CAM_LOGD("[%s] -", __FUNCTION__);
}

void
SDK_jpg::
uninit(void)
{
    CAM_LOGD("[%s] +", __FUNCTION__);	
    CAM_LOGD("[%s] -", __FUNCTION__);
}

/*******************************************************************************
*
*******************************************************************************/
MBOOL
SDK_jpg:: 
JPG_apply(
        IImageBuffer* SrcImgBuffer,
        IImageBuffer* DstImgBuffer,
        ExifParams*   exifPara
        )
{
    CAM_LOGD("[%s] +", __FUNCTION__);
	
    if(SrcImgBuffer == NULL || DstImgBuffer == NULL)
    {
        CAM_LOGD("[%s]TempSrcImgBuffer is Null",__FUNCTION__);
        return NULL;
    }

	Mhal_jpg*  dojpg = NULL;
	dojpg = new Mhal_jpg();
    if  ( dojpg == 0 ) {
        CAM_LOGE("[%s] new JPG fail\n", __FUNCTION__);
        return NULL;
    }
	dojpg->init();
	dojpg->SDKcreateJpegImg(SrcImgBuffer, DstImgBuffer, exifPara); 

    delete dojpg;
    dojpg = NULL;

    CAM_LOGD("[%s] -", __FUNCTION__);

	return MTRUE;

}


