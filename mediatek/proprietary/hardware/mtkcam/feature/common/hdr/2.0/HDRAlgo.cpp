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

#define DEBUG_LOG_TAG "ALG"

#include "HDR.h"

#include <common/hdr/2.0/utils/Debug.h>
#include <common/hdr/2.0/utils/ImageBufferUtils.h>

#if USE_HAL1
using namespace NSCam;
#include <mtkcam/featureio/capturenr.h>
#include <mtkcam/camshot/_params.h>
#endif
#include <isp_tuning/isp_tuning.h>

using namespace android;
#if USE_HAL1
using namespace NSCamNode;
using namespace NS3A;
using namespace NSCamShot;
#endif

// ---------------------------------------------------------------------------

MBOOL HDR::do_Normalization(unsigned int method)
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    // prepare HDR parameters
    HDR_PIPE_CONFIG_PARAM rHdrPipeConfigParam;
    rHdrPipeConfigParam.eHdrRound = 1;

    rHdrPipeConfigParam.u4SourceImgWidth = mu4W_yuv;
    rHdrPipeConfigParam.u4SourceImgHeight = mu4H_yuv;
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        rHdrPipeConfigParam.pSourceImgBufAddr[i] =
            (MUINT8 *) mpSourceImgBuf[i]->getBufVA(0);
        rHdrPipeConfigParam.pSmallImgBufAddr[i] =
            (MUINT8 *) mpSmallImgBuf[i]->getBufVA(0);
    }

    // 1: rank image, 0: normalization
    rHdrPipeConfigParam.manual_PreProcType = method ? 1 : 0;
    HDR_LOGD("[do_Normalization] method(%d)", method);

    // configure HDR Parameters
    ret = mpHdrHal->HdrSmallImgBufSet(rHdrPipeConfigParam);
    if (ret != MTRUE)
    {
        HDR_LOGE("set small image buffer failed");
        return ret;
    }

    // normalize small images; the normalized images are put back to SmallImgbuf[]
    {
        HDR_TRACE_NAME("do_Normalization");
        ret = mpHdrHal->Do_Normalization();
        if (ret != MTRUE)
        {
            HDR_LOGE("do normalization failed");
            return ret;
        }
    }

    // save normalized small image for debugging
    if (mDebugMode)
    {
        for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
        {
            char szFileName[100];
            if (method)
            {
                ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                        "%04d_3_normalized_rank_mpSmallImgBuf[%d]_%dx%d.y",
                        mu4RunningNumber, i, mu4W_small, mu4H_small);
            }
            else
            {
                ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                        "%04d_3_normalized_notrank_mpSmallImgBuf[%d]_%dx%d.y",
                        mu4RunningNumber, i, mu4W_small, mu4H_small);
            }
            mpSmallImgBuf[i]->saveToFile(szFileName);
        }
    }

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_SE()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    // prepare SE Input Information
    HDR_PIPE_SE_INPUT_INFO rHdrPipeSEInputInfo;
    rHdrPipeSEInputInfo.u2SEImgWidth  = mu4W_se;
    rHdrPipeSEInputInfo.u2SEImgHeight = mu4H_se;
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        rHdrPipeSEInputInfo.pSEImgBufAddr[i] =
            (MUINT8 *) mpSEImgBuf[i]->getBufVA(0);
    }

    // do SE
    ret = mpHdrHal->Do_SE(rHdrPipeSEInputInfo);

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_FeatureExtraction()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    // configure MAV hal init Information
    HDR_PIPE_FEATURE_EXTRACT_INPUT_INFO rHdrPipeFeatureExtractInputInfo;
    rHdrPipeFeatureExtractInputInfo.u2SmallImgW = mu4W_small;
    rHdrPipeFeatureExtractInputInfo.u2SmallImgH = mu4H_small;
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        rHdrPipeFeatureExtractInputInfo.pSmallImgBufAddr[i] =
            (MUINT8 *) mpSmallImgBuf[i]->getBufVA(0);
    }
    // assign working buffer
    rHdrPipeFeatureExtractInputInfo.pWorkingBuffer =
        (MUINT8 *) mpHdrWorkingBuf->getBufVA(0);

    // do feature extraction and feature matching
    ret = mpHdrHal->Do_FeatureExtraction(rHdrPipeFeatureExtractInputInfo);
    HDR_LOGE_IF(ret != MTRUE, "do feature extraction failed");

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_Alignment()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;

    ret = mpHdrHal->HdrWorkingBufSet(
            (MUINT8*) mpHdrWorkingBuf->getBufVA(0),
            mpHdrWorkingBuf->getBufSizeInBytes(0));

    HDR_LOGE_IF(ret != MTRUE, "set working buffer failed");

    // must set bmap buffer before do alignment
    ret = do_SetBmapBuffer();
    if (ret != MTRUE)
    {
        HDR_LOGE("set working buffer failed)");
        return ret;
    }

    // do alignment
    ret = mpHdrHal->Do_Alignment();
    HDR_LOGE_IF(ret != MTRUE, "do alignment failed");

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_SetBmapBuffer()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    HDR_LOGD("[do_SetBmapBuffer] bmap width(%d) height(%d) size(%d)",
            mHdrSetBmapInfo.bmap_width,
            mHdrSetBmapInfo.bmap_height,
            mHdrSetBmapInfo.bmap_image_size);

    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        HDR_LOGD("[do_SetBmapBuffer] bmap image_addr[%d](0x%x)",
                i, mHdrSetBmapInfo.bmap_image_addr[i]);
    }

    // set the resulting weighting map
    ret = mpHdrHal->WeightingMapInfoSet(&mHdrSetBmapInfo);
    HDR_LOGE_IF(ret != MTRUE, "set weighting map info set failed");

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_OriWeightMapGet()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    // get the resulting weighting map
    ret = mpHdrHal->WeightingMapInfoGet(OriWeight);
    if (ret != MTRUE)
    {
        HDR_LOGE("get weighting map info set failed");
        return ret;
    }

    // show the obtained original weighting map
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {

        HDR_LOGD("[do_OriWeightMapGet] OriWeight[%d] size(%dx%d) addr(%p)",
                i,
                OriWeight[i]->weight_table_width,
                OriWeight[i]->weight_table_height,
                OriWeight[i]->weight_table_data);

        // check if the obtained original weighting map is the same as
        // what we set from do_SetBmapBuffer()
        if (OriWeight[i]->weight_table_data != mHdrSetBmapInfo.bmap_image_addr[i])
        {
            HDR_LOGE("OriWeight[%d]->weight_table_data is %p, it should be %p"
                    , i
                    , OriWeight[i]->weight_table_data
                    , mHdrSetBmapInfo.bmap_image_addr[i]);
        }
    }

    if (mDebugMode)
    {
        for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
        {
            char szFileName[100];
            ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                    "%04d_5_WeightMap%d_%dx%d.y",
                    mu4RunningNumber, i,
                    OriWeight[i]->weight_table_width,
                    OriWeight[i]->weight_table_height);
            dumpToFile(szFileName, OriWeight[i]->weight_table_data,
                    OriWeight[i]->weight_table_width * OriWeight[i]->weight_table_height);
        }
    }

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_DownScaleWeightMap()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        //HDR_LOGD("[do_DownScaleWeightMap] CDPResize(%d/%d) src(0x%x) dst(0x%x)",
        //        i, u4OutputFrameNum,
        //        mWeightingBuf[i].virtAddr,
        //        mpDownSizedWeightMapBuf[i].virtAddr);
        {
            HDR_TRACE_NAME("CDPResize_DownScaleWeightMap");

            // TODO: check if mWeightingBuf can be replaced with pImageWeighting
            IImageBuffer *pImageWeighting =
                ImageBufferUtils::getInstance().createBufferAlias(
                        mWeightingBuf[i].get(),
                        OriWeight[i]->weight_table_width,
                        OriWeight[i]->weight_table_height,
                        eImgFmt_Y800);

            ret = CDPResize(pImageWeighting, mpDownSizedWeightMapBuf[i].get(), 0);
            HDR_LOGE_IF(ret != MTRUE, "downscaling weighting map(%d) failed", i);

            ImageBufferUtils::getInstance().removeBufferAlias(
                    mWeightingBuf[i].get(), pImageWeighting);
        }
    }

    if (mDebugMode)
    {
        for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
        {
            char szFileName[100];
            ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                    "%04d_6_mpDownSizedWeightMapBuf[%d]_%dx%d.y",
                    mu4RunningNumber, i, mu4W_dsmap, mu4H_dsmap);
            mpDownSizedWeightMapBuf[i]->saveToFile(szFileName);
        }
    }

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_UpScaleWeightMap()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;
    MUINT32 u4OutputFrameNum = getOutputFrameCount();

    // up-sample down-sized weighting map to make them blurry
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        HDR_TRACE_NAME("CDPResize_UpScaleWeightMap");

        ret = CDPResize(mpDownSizedWeightMapBuf[i].get(), mpBlurredWeightMapBuf[i].get(), 0);
        HDR_LOGE_IF(ret != MTRUE, "upscaling weighting map(%d) failed", i);
    }

    // show blurred weighting map information
    for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
    {
        HDR_LOGD("[do_UpScaleWeightMap] BlurredWeight[%d] size(%dx%d) addr(%p)",
                i,
                BlurredWeight[i]->weight_table_width,
                BlurredWeight[i]->weight_table_height,
                BlurredWeight[i]->weight_table_data);
    }

    if (mDebugMode)
    {
        for (MUINT32 i = 0; i < u4OutputFrameNum; i++)
        {
            char szFileName[100];
            ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                    "%04d_7_blurred_WeightMap%d_%dx%d.y",
                    mu4RunningNumber, i,
                    BlurredWeight[i]->weight_table_width,
                    BlurredWeight[i]->weight_table_height);
            dumpToFile(szFileName, BlurredWeight[i]->weight_table_data,
                    BlurredWeight[i]->weight_table_width * BlurredWeight[i]->weight_table_height);
        }
    }

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_Fusion()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;

    // set result buffer
    ret = mpHdrHal->ResultBufferSet(
            (MUINT8 *) mBlendingBuf->getBufVA(0),
            mBlendingBuf->getBufSizeInBytes(0));
    if (ret != MTRUE)
    {
        HDR_LOGE("set result buffer failed");
        return ret;
    }

    // do fusion
    ret = mpHdrHal->Do_Fusion(BlurredWeight);
    if (ret != MTRUE)
    {
        HDR_LOGE("do fusion failed");
        return ret;
    }

    FUNCTION_LOG_END;
    return ret;
}

MBOOL HDR::do_HdrCroppedResultGet()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;

    // get HDR result
    ret = mpHdrHal->HdrCroppedResultGet(mrHdrCroppedResult);

    // *3/2: YUV420 size
    MUINT32 u4HdrCroppedResultSize =
        mrHdrCroppedResult.output_image_width * mrHdrCroppedResult.output_image_height * 3 / 2;

    HDR_LOGD("[do_HdrCroppedResultGet] rCroppedHdrResult size(%dx%d) buffer size(%d) addr(%p)",
            mrHdrCroppedResult.output_image_width,
            mrHdrCroppedResult.output_image_height,
            u4HdrCroppedResultSize,
            mrHdrCroppedResult.output_image_addr);

    if (mDebugMode)
    {
        char szFileName[100];
        ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                "%04d_8_HdrResult_%dx%d.i420",
                mu4RunningNumber,
                mrHdrCroppedResult.output_image_width,
                mrHdrCroppedResult.output_image_height);
        dumpToFile(szFileName, mrHdrCroppedResult.output_image_addr, u4HdrCroppedResultSize);
    }

    FUNCTION_LOG_END;
    return ret;
}

#if USE_HAL1
// TODO:
// 1. move NR to a standalone clase
// 2. need to find the NR API for HAL3
MBOOL HDR::do_NR()
{
    HDR_TRACE_CALL();
    FUNCTION_LOG_START;

    MBOOL ret = MTRUE;

    IImageBuffer *pImageCropped;
    IImageBuffer *pImageNR;

    // create cropped image buffer
    pImageCropped =
        ImageBufferUtils::getInstance().createBufferAlias(
                mBlendingBuf,
                mrHdrCroppedResult.output_image_width,
                mrHdrCroppedResult.output_image_height,
                eImgFmt_I420);
    if (!pImageCropped)
    {
        HDR_LOGE("create cropped image buffer failed");
        ret = MFALSE;
        goto lbExit;
    }

    if (mDebugMode)
    {
        char szFileName[100];
        ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                "%04d_8_NR_HdrCroppedBuf_%dx%d.i420",
                mu4RunningNumber,
                mrHdrCroppedResult.output_image_width,
                mrHdrCroppedResult.output_image_height);
        pImageCropped->saveToFile(szFileName);
    }

    // create working image buffer
    // [xxx]: reuse the buffer headp of HDR working buffer here?
    pImageNR =
        ImageBufferUtils::getInstance().createBufferAlias(
                mpHdrWorkingBuf, mRotPicWidth, mRotPicHeight, eImgFmt_YUY2);
    if (!pImageNR)
    {
        HDR_LOGE("create NR image buffer failed");
        ret = MFALSE;
        goto lbExit;
    }

    // crop for jpeg
    MRect srcRect(MPoint(0, 0), MSize(
                mrHdrCroppedResult.output_image_width,
                mrHdrCroppedResult.output_image_height));
    MRect dstRect(MPoint(0, 0), MSize(
                mShotParam.mi4PictureWidth, mShotParam.mi4PictureHeight));

    // calculate crop
    {
        MFLOAT fSrcRatio = srcRect.s.w / (MFLOAT)(srcRect.s.h);
        MFLOAT fDesRatio = dstRect.s.w / (MFLOAT)(dstRect.s.h);

        // NOTE: the crop region is limited to the source crop's width or height.
        // if the aspect ratio of source buffer is larger than
        // the destination's one (e.g. 16:9 > 4:3),
        // then the source buffer is cropped vertically:
        //  +---O==================O---+
        //  |   I                  I   |
        //  |   I                  I   |
        //  |   I dst crop region  I   |
        //  |   I      (4:3)I      I   |
        //  |   I                  I   |
        //  |   I                  I   |
        //  |   I                  I   |
        //  +---O==================O---+
        //      src crop region (16:9)
        //
        // otherwise, the source buffer is cropped horizontally:
        //  +-------------------+
        //  |                   |
        //  O===================O
        //  I  dst crop region  I
        //  I      (16:9)       I
        //  I                   I
        //  O===================O
        //  |                   |
        //  +-------------------+
        //  src crop region (4:3)
        if( fSrcRatio > fDesRatio)
        {
            dstRect.s.h = srcRect.s.h;
            dstRect.s.w = dstRect.s.h * fDesRatio;
        }
        else
        {
            dstRect.s.w = srcRect.s.w;
            dstRect.s.h = dstRect.s.w / fDesRatio;
        }
    }

    // already applied ZoomRatio in source image,
    // no need to crop with zoom ratio here
    MRect cropRect;

    cropRect = calCrop(srcRect, dstRect);
    HDR_LOGD("zoom(%d)", 100);

    HDR_LOGD("srcRect size(%d, %d, %dx%d)",
            srcRect.p.x, srcRect.p.y, srcRect.s.w, srcRect.s.h);
    HDR_LOGD("dstRect size(%d, %d, %dx%d)",
            dstRect.p.x, dstRect.p.y, dstRect.s.w, dstRect.s.h);
    HDR_LOGD("cropRect size(%d, %d, %dx%d)",
            cropRect.p.x, cropRect.p.y, cropRect.s.w, cropRect.s.h);

    HDR_LOGD("mNrtype(%d)", mNrtype);
    switch (mNrtype)
    {
        case ECamShot_NRTYPE_NONE:
            {
                 // resize yuv buffer
                ret = convertImage(
                        pImageCropped, pImageNR,
                        0,
                        MTRUE, cropRect);
                if (ret != MTRUE)
                {
                    HDR_LOGE("convert iamge failed");
                    goto lbExit;
                }

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrNoNrBlendingBuf_%dx%d.yuy2",
                            mu4RunningNumber,
                            pImageNR->getImgSize().w, pImageNR->getImgSize().h);
                    pImageNR->saveToFile(szFileName);
                }
            }
            break;

        case ECamShot_NRTYPE_HWNR:
            {
                IImageBuffer *pImageHwNrWorkingBuf1;
                IImageBuffer *pImageHwNrWorkingBuf2;

                ret = ImageBufferUtils::getInstance().allocBuffer(
                        &pImageHwNrWorkingBuf1,
                        mrHdrCroppedResult.output_image_width,
                        mrHdrCroppedResult.output_image_height,
                        eImgFmt_YUY2);
                if (ret != MTRUE)
                {
                    HDR_LOGE("allocate pImageHwNrWorkingBuf1 failed");
                    goto lbExit;
                }

                ret = ImageBufferUtils::getInstance().allocBuffer(
                        &pImageHwNrWorkingBuf2,
                        mrHdrCroppedResult.output_image_width,
                        mrHdrCroppedResult.output_image_height,
                        eImgFmt_YUY2);
                if (ret != MTRUE)
                {
                    HDR_LOGE("allocate pImageHwNrWorkingBuf2 failed");
                    goto lbExit;
                }

                ret = convertImage(pImageCropped, pImageHwNrWorkingBuf1);
                if (ret != MTRUE)
                {
                    HDR_LOGE("convert image failed");
                    goto lbExit;
                }

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrHwNrWorkingBuf1_%dx%d.yuy2",
                            mu4RunningNumber,
                            mrHdrCroppedResult.output_image_width,
                            mrHdrCroppedResult.output_image_height);
                    pImageHwNrWorkingBuf1->saveToFile(szFileName);
                }

                // 1st run
                ret = doHwNR(mOpenID,
                        pImageHwNrWorkingBuf1,
                        pImageHwNrWorkingBuf2,
                        NULL,
                        MRect(MPoint(0), pImageHwNrWorkingBuf1->getImgSize()),
                        0,
                        EIspProfile_MFB_MultiPass_ANR_1);
                if (ret != MTRUE)
                {
                    HDR_LOGE("1st run HWNR failed");
                    goto lbExit;
                }

                ImageBufferUtils::getInstance().deallocBuffer(pImageHwNrWorkingBuf1);
                pImageHwNrWorkingBuf1 = NULL;

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrHwNrWorkingBuf2_%dx%d.yuy2",
                            mu4RunningNumber,
                            mrHdrCroppedResult.output_image_width,
                            mrHdrCroppedResult.output_image_height);
                    pImageHwNrWorkingBuf2->saveToFile(szFileName);
                }

                // 2nd run
                ret = ret && doHwNR(mOpenID,
                        pImageHwNrWorkingBuf2,
                        pImageNR,
                        NULL,
                        cropRect,
                        mShotParam.mu4Transform,
                        EIspProfile_MFB_MultiPass_ANR_2);
                if (ret != MTRUE)
                {
                    HDR_LOGE("2nd run HWNR failed");
                    goto lbExit;
                }

                ImageBufferUtils::getInstance().deallocBuffer(pImageHwNrWorkingBuf2);
                pImageHwNrWorkingBuf2 = NULL;

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrHwNrBlendingBuf_%dx%d.yuy2",
                            mu4RunningNumber,
                            pImageNR->getImgSize().w,
                            pImageNR->getImgSize().h);
                    pImageNR->saveToFile(szFileName);
                }
             }
            break;

        case ECamShot_NRTYPE_SWNR:
            {
                SwNR *swnr = new SwNR(mOpenID);
                ret = swnr->doSwNR(pImageCropped);
                delete swnr;
                if (ret != MTRUE)
                {
                    HDR_LOGE("run SWNR failed");
                    goto lbExit;
                }

                ret = pImageCropped->syncCache(eCACHECTRL_FLUSH);
                if (ret != MTRUE)
                {
                    HDR_LOGE("flush cache CPU to HW failed");
                    goto lbExit;
                }

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrSwNrBlendingBuf_%dx%d.i420",
                            mu4RunningNumber,
                            pImageCropped->getImgSize().w,
                            pImageCropped->getImgSize().h);
                    pImageCropped->saveToFile(szFileName);
                }

                ret = convertImage(
                        pImageCropped, pImageNR,
                        mShotParam.mu4Transform,
                        MTRUE, cropRect);
                if (ret != MTRUE)
                {
                    HDR_LOGE("convert image failed");
                    goto lbExit;
                }

                if (mDebugMode)
                {
                    char szFileName[100];
                    ::sprintf(szFileName, HDR_DEBUG_OUTPUT_FOLDER \
                            "%04d_8_NR_HdrSwNrForJpegBuf_%dx%d.yuy2",
                            mu4RunningNumber,
                            pImageNR->getImgSize().w,
                            pImageNR->getImgSize().h);
                    pImageNR->saveToFile(szFileName);
                }
            }
            break;

        default:
            HDR_LOGE("wrong NR type(%d)", mNrtype);
            ret = MFALSE;
    }

lbExit:
    if (pImageCropped != NULL)
    {
        ImageBufferUtils::getInstance().removeBufferAlias(
                mBlendingBuf, pImageCropped);
    }

    if (pImageNR != NULL)
    {
        ImageBufferUtils::getInstance().removeBufferAlias(
                mpHdrWorkingBuf, pImageNR);
    }

    FUNCTION_LOG_END;
    return ret;
}
#endif
