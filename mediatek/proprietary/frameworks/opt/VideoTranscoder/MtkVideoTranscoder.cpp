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

/*****************************************************************************
 *
 * Filename:
 * ---------
 *   MtkVideoTranscoder.cpp
 *
 * Project:
 * --------
 *
 *
 * Description:
 * ------------
 *   Video transcoder implementation
 *
 * Author:
 * -------
 *   Morris Yang
 *
 ****************************************************************************/
#include <utils/Log.h>
#undef LOG_TAG
#define LOG_TAG "VideoTranscoder"

#include <binder/ProcessState.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MPEG4Writer.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/OMXCodec.h>
#include <OMX_IVCommon.h>
#include <media/MediaPlayerInterface.h>
#ifdef USE_MTK_MDP_MHAL
#include "MediaHal.h"
#endif
#include "MtkVideoTranscoder.h"
#include "DpBlitStream.h"

#include <vdec_drv_if_public.h>
#include "val_types_public.h"
#include <fcntl.h>

#define MEM_ALIGN_32 32
#define ROUND_16(X)     ((X + 0xF) & (~0xF))
#define ROUND_32(X)     ((X + 0x1F) & (~0x1F))
#define YUV_SIZE(W,H)   (W * H * 3 >> 1)

enum
{
    UV_STRIDE_16_8_8,
    UV_STRIDE_16_16_16
};


using namespace android;

typedef struct
{
    bool     Cancelled;
    uint32_t Progress;
    uint32_t LastError;
} MtkVideoTranscoderInternalCtxt;

bool IsSupportedResolution(int32_t width, int32_t height, const char *mime)
{
    int32_t VIDEO_MAX_RESOLUTION = 0;
    VDEC_DRV_QUERY_VIDEO_FORMAT_T qinfo;
    VDEC_DRV_QUERY_VIDEO_FORMAT_T outinfo;
    memset(&qinfo, 0, sizeof(qinfo));
    memset(&outinfo, 0, sizeof(outinfo));
    qinfo.u4Width = width;
    qinfo.u4Height = height;
    if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_H263, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_MPEG4;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_MPEG4 (H263)");
    }
    else if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_MPEG4, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_MPEG4;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_MPEG4");
    }
    else if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_AVC, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_H264;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_H264");
    }
    else if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_HEVC, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_H265;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_H265");
    }
    else if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_VP8, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_VP8;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_VP8");
    }
    else if (!strcasecmp(MEDIA_MIMETYPE_VIDEO_WMV, mime))
    {
        qinfo.u4VideoFormat = VDEC_DRV_VIDEO_FORMAT_VC1;
        //ALOGD ("VDEC_DRV_VIDEO_FORMAT_VC1");
    }
    else
    {
        if (width > 4096 || height > 2176)
        {
            ALOGD("IsSupportedResolution return false1");
            return false;
        }
    }

#if 0
    ALOGD("Video: [%s], %dx%d, profile(%d), level(%d)", mime, qinfo.u4Width, qinfo.u4Height, qinfo.u4Profile, qinfo.u4Level);
    VDEC_DRV_MRESULT_T ret = eVDecDrvQueryCapability(VDEC_DRV_QUERY_TYPE_VIDEO_FORMAT, &qinfo, &outinfo);
    if (VDEC_DRV_MRESULT_OK != ret)
    {
        ALOGE("eVDecDrvQueryCapability failed: %d", ret);
        if (width > 4096 || height > 2176)
        {
            ALOGD("IsSupportedResolution return false2");
            return false;
        }
    }

    VIDEO_MAX_RESOLUTION = outinfo.u4Width * outinfo.u4Height;
    if (qinfo.u4Width > outinfo.u4Width || qinfo.u4Height > outinfo.u4Width ||
        qinfo.u4Width * qinfo.u4Height > outinfo.u4Width * outinfo.u4Height || (width <= 0) || (height <= 0))
    {
        ALOGD("IsSupportedResolution return false3");
        return false;
    }
#endif
    ALOGD("IsSupportedResolution return true");
    return true;
}
DpColorFormat OmxColorToDpColor(int32_t omx_color_format)
{
    DpColorFormat colorFormat;

    switch (omx_color_format)
    {
        case OMX_COLOR_FormatVendorMTKYUV:
            colorFormat = eNV12_BLK;
            break;
        case OMX_COLOR_FormatVendorMTKYUV_FCM:
            colorFormat = eNV12_BLK_FCM;
            break;
        case OMX_COLOR_FormatYUV420Planar:
            colorFormat = eYUV_420_3P;
            break;
        case OMX_MTK_COLOR_FormatYV12:
            colorFormat = eYV12;
            break;
        default:
            colorFormat = eYUV_420_3P;
            ALOGE("[Warning] Cannot find color mapping !!");
            break;
    }

    return colorFormat;
}

class MemorySource : public MediaSource
{
    public:
        MemorySource(MtkVideoTranscoderInternalCtxt *pInternalCtxt, const char *input_file, int32_t output_color_format, int32_t targetWidth, int32_t targetHeight, int32_t targetFrameRate, int32_t &status);

        virtual sp<MetaData> getFormat();
        virtual status_t start(MetaData *params);
        virtual status_t stop();
        virtual status_t read(MediaBuffer **buffer, const MediaSource::ReadOptions *options);

        bool cut(int64_t begin_ts, int64_t end_ts);
        void getActualTimeBoundary(int64_t &begin_ts, int64_t &end_ts, int64_t &clip_duration);

    protected:
        virtual ~MemorySource();

        void convertVideoFrame(uint8_t *input, int32_t srcWidth, int32_t srcHeight, int32_t srcStride, int32_t srcSliceHeight,
                               uint8_t *output, int32_t dstWidth, int32_t dstHeight);

    private:
        sp<MediaSource> mVideoTrack;   // parser
        sp<MediaSource> mVideoSource;  // decoder
        sp<MetaData>    mMetaData;
        sp<MetaData>    mExtractorMetaData;

        MediaBufferGroup *mPtrBufferGroup;
        int32_t mWidth, mHeight;
        int32_t mStride, mSliceHeight;
        int32_t mTargetWidth, mTargetHeight;
        int32_t mSrcColorFormat;
        int32_t mDstColorFormat;
        bool    mUseResizer;
        bool    mInitialRead;
        int64_t mSourceDurationUs;
        int64_t mBeginTs;
        int64_t mEndTs;
        int32_t mFrameDecCount;
        int32_t mFrameOutCount;
        int64_t mPrevFrameTs;
        int32_t mFrameDropInterval;
        int32_t mTargetFrameRate;

        MtkVideoTranscoderInternalCtxt *mInternalCtxt;

        MemorySource(const MemorySource &);
        MemorySource &operator=(const MemorySource &);
};

MemorySource::MemorySource(MtkVideoTranscoderInternalCtxt *pInternalCtxt, const char *input_file, int32_t output_color_format, int32_t targetWidth, int32_t targetHeight, int32_t targetFrameRate, int32_t &status)
:mPtrBufferGroup(NULL)
{
    ALOGD("+MemorySource::MemorySource (%s)", input_file);

    mInternalCtxt = pInternalCtxt;

    // init data source
    sp<DataSource> dataSource = new FileSource(input_file);
    status_t err = dataSource->initCheck();

    if (OK != err)
    {
        ALOGE("dataSource->initCheck failed");
        status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
        return;
    }

    DataSource::RegisterDefaultSniffers();
    sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);
    if (extractor == NULL)
    {
        ALOGE("MediaExtractor::Create failed");
        status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
        return;
    }

    // Necessary for certain extractor like MtkAVIExtractor
    mExtractorMetaData = extractor->getMetaData();

    uint32_t extractorFlags = extractor->flags();
/*   // zxy fix build error
    if (extractorFlags & MediaExtractor::MAY_PARSE_TOO_LONG)
    {
        ALOGD("MediaExtractor MAY_PARSE_TOO_LONG");
        err = extractor->finishParsing();
    }
*/

    // find a video track
    for (size_t i = 0; i < extractor->countTracks(); ++i)
    {
        sp<MetaData> meta = extractor->getTrackMetaData(i);
        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));
        //ALOGD ("track (%d) mime %s", i, mime);
        if (!strncasecmp(mime, "video/", 6))
        {
            ALOGD("found video track(%d), mime(%s)", i, mime);
            mVideoTrack = extractor->getTrack(i);

            int32_t width;
            int32_t height;
            CHECK(meta->findInt32(kKeyWidth, &width));
            CHECK(meta->findInt32(kKeyHeight, &height));
            ALOGD("@@ width(%d), height(%d)", width, height);
            if (false == IsSupportedResolution(width, height, mime))
            {
                status = MTK_VIDEO_TRANSCODER_ERROR_UNSUPPORTED_VIDEO;
                return;
            }
#if 1
            //exceed mpeg4 encoder limitation
            if (targetWidth > 1920 || targetHeight > 1088)
            {
                status = MTK_VIDEO_TRANSCODER_ERROR_UNSUPPORTED_VIDEO;
                return;
            }
#endif
            break;
        }
    }

    if (mVideoTrack == NULL)
    {
        ALOGE("No video track available");
        status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
        return;
    }

    // create video decoder
    OMXClient mClient;
    CHECK_EQ(mClient.connect(), (status_t)OK);
    mVideoSource = OMXCodec::Create(mClient.interface(), mVideoTrack->getFormat(), false, // createEncoder
                                    mVideoTrack, NULL, OMXCodec::kHardwareCodecsOnly /*flags*/);

    if (mVideoSource == NULL)
    {
        ALOGE("Oops, VideoSource creation failed. [Unsupported Video]");
        status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
        return;
    }

    if (mVideoTrack->getFormat()->findInt64(kKeyDuration, &mSourceDurationUs))
    {
        ALOGD("Duration = %lld us", mSourceDurationUs);
    }
    else
    {
        ALOGE("Get duration error");
        mSourceDurationUs = 0;
    }

    CHECK(mVideoTrack->getFormat()->findInt32(kKeyWidth, &mWidth));
    CHECK(mVideoTrack->getFormat()->findInt32(kKeyHeight, &mHeight));
    if (!mVideoSource->getFormat()->findInt32(kKeyStride, &mStride))
    {
        mStride = 0;
    }
    if (!mVideoSource->getFormat()->findInt32(kKeySliceHeight, &mSliceHeight))
    {
        mSliceHeight = 0;
    }
    CHECK(mVideoSource->getFormat()->findInt32(kKeyColorFormat, &mSrcColorFormat));
    mSrcColorFormat = OMX_COLOR_FormatYUV420Planar;
    mDstColorFormat = OMX_COLOR_FormatYUV420Planar;
    ALOGD("Width = %d, Height = %d, mStride = %d, mSliceHeight = %d, targetWidth = %d, targetHeight = %d, mSrcColorFormat = 0x%08X, mDstColorFormat = 0x%08X",
        mWidth, mHeight, mStride, mSliceHeight, targetWidth, targetHeight, mSrcColorFormat, mDstColorFormat);

    if ((targetWidth != mWidth) || (targetHeight != mHeight) || (mSrcColorFormat != mDstColorFormat))
    {
        mTargetWidth = ROUND_16(targetWidth);
        mTargetHeight = ROUND_16(targetHeight);

        ALOGD("Use Resizer: w %d, h %d -> mTargetWidth = %d, mTargetHeight = %d", mWidth, mHeight, mTargetWidth, mTargetHeight);
        mUseResizer = true;
#if 0
        // TODO: put more MediaBuffer
        for (int i = 0 ; i < 1 ; i++)
        {
            mBufferGroup.add_buffer(new MediaBuffer(ROUND_32(YUV_SIZE(mTargetWidth, mTargetHeight))));
        }
#endif
    }
    else
    {
        mTargetWidth = targetWidth;
        mTargetHeight = targetHeight;

        ALOGD("No Resizer Needed");
        mUseResizer = false;
    }

    mInitialRead = true;
    mBeginTs = -1;
    mEndTs = -1;
    mFrameDecCount = 0;
    mFrameOutCount = 0;
    mPrevFrameTs = -1;
    mFrameDropInterval = 0;
    mTargetFrameRate = targetFrameRate;
    mPtrBufferGroup = NULL;

    // init meta data
    mMetaData = new MetaData;
    if (mUseResizer == true)
    {
        mMetaData->setInt32(kKeyWidth, mTargetWidth);
        mMetaData->setInt32(kKeyHeight, mTargetHeight);
    }
    else
    {
        mMetaData->setInt32(kKeyWidth, mWidth);
        mMetaData->setInt32(kKeyHeight, mHeight);
    }
    mMetaData->setInt32(kKeyColorFormat, mDstColorFormat);
    mMetaData->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_RAW);
    mMetaData->setInt64(kKeyDuration, mSourceDurationUs);

    // keep rotation info
    int32_t rotationDegrees;
    if (mVideoSource->getFormat()->findInt32(kKeyRotation, &rotationDegrees))
    {
        mMetaData->setInt32(kKeyRotation, rotationDegrees);
    }
    else
    {
        ALOGD("Cannot find kKeyRotation A !!!");
    }

    ALOGD("-MemorySource::MemorySource");
}


MemorySource::~MemorySource()
{
    ALOGD("~MemorySource");
    if (mPtrBufferGroup != NULL)
    {
        delete mPtrBufferGroup;
        mPtrBufferGroup = NULL;
    }
}

sp<MetaData> MemorySource::getFormat()
{
    return mMetaData;
}

status_t MemorySource::start(MetaData *params)
{
    ALOGD("MemorySource::start");
    // start to decode
    status_t err = mVideoSource->start();
    return err;
}

status_t MemorySource::stop()
{
    ALOGD("MemorySource::stop");
    mVideoSource->stop();
    mVideoSource.clear();
    return OK;
}

status_t MemorySource::read(MediaBuffer **buffer, const MediaSource::ReadOptions *options)
{
    ALOGD("+MemorySource::read");
    status_t err = OK;

    for (;;)
    {
        MediaSource::ReadOptions vdecReadOptions;
        if (mInitialRead)
        {
            mInitialRead = false;

            if ((-1 == mBeginTs) || (-1 == mEndTs))   // convert entire file
            {
                vdecReadOptions.clearSeekTo();
            }
            else
            {
                ALOGD("Seek mBeginTs=%lld, mEndTs=%lld", mBeginTs, mEndTs);
                vdecReadOptions.setSeekTo(mBeginTs, ReadOptions::SEEK_CLOSEST_SYNC);
            }
        }
        err = mVideoSource->read(buffer, &vdecReadOptions);

        if (ERROR_END_OF_STREAM == err)
        {
            ALOGD("Got EOS mFrameOutCount(%d)", mFrameOutCount);
            if (mFrameOutCount == 0)
            {
                mInternalCtxt->LastError = MTK_VIDEO_TRANSCODER_FAILED_TO_TRANSCODE;
            }
            break;
        }

        if (INFO_FORMAT_CHANGED == err)
        {
            CHECK(mVideoSource->getFormat()->findInt32(kKeyWidth, &mWidth));
            CHECK(mVideoSource->getFormat()->findInt32(kKeyHeight, &mHeight));
            if (!mVideoSource->getFormat()->findInt32(kKeyStride, &mStride))
            {
                mStride = 0;
            }
            if (!mVideoSource->getFormat()->findInt32(kKeySliceHeight, &mSliceHeight))
            {
                mSliceHeight = 0;
            }
            if ((mTargetWidth != ROUND_16(mWidth)) || (mTargetHeight != ROUND_16(mHeight)))
            {
                mUseResizer = true;
                if (mPtrBufferGroup != NULL)
                {
                    delete mPtrBufferGroup;
                    mPtrBufferGroup = NULL;
                }
            }
            else if (mSrcColorFormat != mDstColorFormat)
            {
                mUseResizer = true;
            }
            else if ((mStride != ROUND_16(mWidth)) || (mSliceHeight != ROUND_16(mHeight)))
            {
                //for google default decoder mStride = mWidth, mSliceHeight = mHeight, so it need to do MDP to form encoder buffer format (stride 16/16)
                mUseResizer = true;
            }
            else
            {
                mUseResizer = false;
            }
            ALOGD("Decoder output format changed mWidth(%d), mHeight(%d), mStride(%d), mSliceHeight(%d), mUseResizer(%d)", mWidth, mHeight, mStride, mSliceHeight, mUseResizer);
            continue; //read next frame
        }

        if (OK != err)
        {
            ALOGE("decoder read err=0x%08X", err);
            mInternalCtxt->LastError = MTK_VIDEO_TRANSCODER_FAILED_TO_TRANSCODE;
            break;
        }

        // skip empty frame buffer
        if ((*buffer)->range_length() == 0)
        {
            ALOGD("Zero length frame buffer !!");
            (*buffer)->release();
            (*buffer) = NULL;
            continue;
        }

        int64_t timeUs;
        CHECK((*buffer)->meta_data()->findInt64(kKeyTime, &timeUs));

        if (timeUs < mPrevFrameTs)
        {
            ALOGD("[Warning] TS rollback timeUs(%lld), prevFrameTs(%lld), drop this frame!!", timeUs, mPrevFrameTs);
            (*buffer)->release();
            (*buffer) = NULL;
            continue;
        }

        if (mFrameDecCount == 1)    // second time read
        {
            if (mTargetFrameRate != 0)    // client specify the frame rate
            {
                int64_t frame_delta_new = ((double)1000 / mTargetFrameRate) * 1000L;
                int64_t frame_delta_old = timeUs - mPrevFrameTs;
                ALOGD("frame_delta_old = %lld, frame_delta_new = %lld", frame_delta_old, frame_delta_new);
                if (frame_delta_old > frame_delta_new)   // invalid
                {
                    mFrameDropInterval = 0;
                }
                else
                {
                    if (frame_delta_old != 0)
                    {
                        mFrameDropInterval = (int32_t)(frame_delta_new / frame_delta_old);
                        ALOGD("mFrameDropInterval = %d", mFrameDropInterval);
                    }
                    else
                    {
                        mFrameDropInterval = 0;
                    }
                }
            }
            else   // keep original frame rate
            {
                mFrameDropInterval = 0;
            }
        }

        mFrameDecCount++;

        if (0 != mFrameDropInterval)
        {
            if (mFrameDecCount % mFrameDropInterval)    // drop frame
            {
                ALOGD("drop TS = %lld", timeUs);
                (*buffer)->release();
                (*buffer) = NULL;
                continue;
            }
        }
        mPrevFrameTs = timeUs;

        ALOGD("TS = %lld", timeUs);

        /*
        if ((-1 != mBeginTs) && (-1 != mEndTs)) {
            g_uProgress = ((timeUs - mBeginTs)*100LL) / (mEndTs-mBeginTs);
        }
        */

        if ((-1 != mEndTs) && (timeUs > mEndTs))
        {
            ALOGD("Cutting end");
            (*buffer)->release();
            (*buffer) = NULL;
            err = ERROR_END_OF_STREAM;
            break;
        }

        mFrameOutCount++;

#if 0 // dump frame buffer
        char buf[255];
        sprintf(buf, "/sdcard/out_%d_%d.mtk.yuv", mWidth, mHeight);
        FILE *fp = fopen(buf, "ab");
        if (fp)
        {
            fwrite((void *)((*buffer)->data()), 1, YUV_SIZE(mWidth, mHeight), fp);
            fclose(fp);
        }
#endif

        if (mUseResizer)
        {
            MediaBuffer *output;
            if (mPtrBufferGroup == NULL)
            {
                mPtrBufferGroup = new MediaBufferGroup;
                mPtrBufferGroup->add_buffer(new MediaBuffer(ROUND_32(YUV_SIZE(mTargetWidth, mTargetHeight))));
            }
            CHECK_EQ(mPtrBufferGroup->acquire_buffer(&output), (status_t)OK);
            uint8_t *dst = (uint8_t *)output->data();
            //memcpy(dst, (*buffer)->data(), mTargetWidth*mTargetHeight*3>>1);
            //convertVideoFrame ((uint8_t*)(*buffer)->data(), ROUND_16(mWidth), ROUND_16(mHeight), dst, mTargetWidth, mTargetHeight);
            convertVideoFrame((uint8_t *)(*buffer)->data(), mWidth, mHeight, mStride, mSliceHeight, dst, mTargetWidth, mTargetHeight);
            output->set_range(0, YUV_SIZE(mTargetWidth, mTargetHeight));
            output->meta_data()->clear();
            output->meta_data()->setInt64(kKeyTime, timeUs);

            int32_t isSyncFrame;
            int32_t isCodecConfig;
            if ((*buffer)->meta_data()->findInt32(kKeyIsSyncFrame, &isSyncFrame))
            {
                output->meta_data()->setInt32(kKeyIsSyncFrame, isSyncFrame);
            }
            if ((*buffer)->meta_data()->findInt32(kKeyIsCodecConfig, &isCodecConfig))
            {
                output->meta_data()->setInt32(kKeyIsCodecConfig, isCodecConfig);
            }

            // release decoder output buffer
            (*buffer)->release();
            (*buffer) = NULL;

            // return converted buffer
            (*buffer) = output;
        }
        else
        {
            (*buffer)->set_range(0, YUV_SIZE(mTargetWidth, mTargetHeight));
        }

        //mVideoBuffer->release();
        //mVideoBuffer = NULL;
        break;
    }

    ALOGD("-MemorySource::read, err=%d", err);
    return err;
}


void MemorySource::convertVideoFrame(uint8_t *input, int32_t srcWidth, int32_t srcHeight, int32_t srcStride, int32_t srcSliceHeight,
                                     uint8_t *output, int32_t dstWidth, int32_t dstHeight)
{
    ALOGD("+MemorySource::convertVideoFrame, tid:%d Src[%d, %d] (%d, %d)[0x%08X], Dst[%d, %d][0x%08X]", gettid(), srcWidth, srcHeight, srcStride, srcSliceHeight, input, dstWidth, dstHeight, output);

#ifdef USE_MTK_MDP_MHAL
    MHAL_BOOL LockScenario = MHAL_FALSE;
    MHalLockParam_t inLockParam;
    inLockParam.mode = MHAL_MODE_BITBLT;
    inLockParam.waitMilliSec = 1000;
    inLockParam.waitMode = MHAL_MODE_BITBLT;

#if 1
    if (MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_LOCK_RESOURCE, (MHAL_VOID *)&inLockParam, sizeof(inLockParam), NULL, 0, NULL))
    {
        ALOGE("[BITBLT][ERROR] mHalIoCtrl() - MT65XX_HW_BITBLT Can't Lock!!!!, TID:%d", gettid());
        LockScenario = MHAL_FALSE;
    }
    else
    {
        ALOGE("[BITBLT] mHalIoCtrl() - MT65XX_HW_BITBLT Lock!!!!, TID:%d", gettid());
        LockScenario = MHAL_TRUE;
    }
#endif

    if (LockScenario == MHAL_FALSE)
    {
        ALOGD("Cannot lock HW !!!!, TID:%d", gettid());
        return;
    }

    MHAL_UINT8 *srcYUVbuf_va = NULL;
    MHAL_UINT8 *dstYUVbuf_va = NULL;
    MHAL_UINT32 srcBufferSize = 0;
    MHAL_UINT32 dstBufferSize = 0;

    mHalBltParam_t bltParam;
    memset(&bltParam, 0, sizeof(bltParam));

    //srcBufferSize = ((((srcWidth * srcHeight * 3) >> 1)+(MEM_ALIGN_32-1)) & ~(MEM_ALIGN_32-1));
    //srcBufferSize = ROUND_32(YUV_SIZE(srcWidth, srcHeight));
    srcBufferSize = ROUND_32(YUV_SIZE(ROUND_16(srcWidth), ROUND_16(srcHeight)));

    srcYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, srcBufferSize);    // 32 byte alignment for MDP

    /*
        dstBufferSize = ((((dstWidth * dstHeight * 3) >> 1)+(MEM_ALIGN_32-1)) & ~(MEM_ALIGN_32-1));
        dstYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, dstBufferSize);    // 32 byte alignment for MDP
    */
    dstYUVbuf_va = output;

    memcpy(srcYUVbuf_va, input, YUV_SIZE(ROUND_16(srcWidth), ROUND_16(srcHeight)));

    uint32_t color_format = MHAL_FORMAT_YUV_420;

#if 1
    switch (mSrcColorFormat)
    {
        case OMX_COLOR_FormatVendorMTKYUV:
            color_format = MHAL_FORMAT_MTK_YUV;
            break;
        case OMX_COLOR_FormatYUV420Planar:
            color_format = MHAL_FORMAT_YUV_420;
            break;
        default:
            color_format = MHAL_FORMAT_YUV_420;
            break;
    }
#endif

    bltParam.srcAddr = (MHAL_UINT32)srcYUVbuf_va;
    bltParam.srcFormat = color_format;
    bltParam.srcX = 0;
    bltParam.srcY = 0;
    bltParam.srcW = srcWidth;
    bltParam.srcWStride = ROUND_16(srcWidth);
    bltParam.srcH = srcHeight;
    bltParam.srcHStride = ROUND_16(srcHeight);

    bltParam.dstAddr = (MHAL_UINT32)dstYUVbuf_va;
    bltParam.dstFormat = color_format;
    bltParam.dstW = dstWidth;
    bltParam.dstH = dstHeight;
    bltParam.pitch = dstWidth; //_mDisp.dst_pitch;
    //bltParam.orientation = _mRotation;
    bltParam.orientation = 0;

    ALOGD("+MHAL_IOCTL_BITBLT");
#if 1
    if (MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_BITBLT, &bltParam, sizeof(bltParam), NULL, 0, NULL))
        //if(MHAL_NO_ERROR != mHalMdpIpc_BitBlt(&bltParam))
    {
        ALOGE("[BITBLT][ERROR] IDP_bitblt() can't do bitblt operation");
        free(srcYUVbuf_va);
        //free (dstYUVbuf_va);
        return;
    }
    else
    {
        //memcpy(output, dstYUVbuf_va, (dstWidth * dstHeight * 3) >> 1);
        free(srcYUVbuf_va);
        //free (dstYUVbuf_va);
    }
#endif
    ALOGD("-MHAL_IOCTL_BITBLT");

    MHAL_UINT32 lock_mode;
    lock_mode = MHAL_MODE_BITBLT;

#if 1
    if (MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_UNLOCK_RESOURCE, (MHAL_VOID *)&lock_mode, sizeof(lock_mode), NULL, 0, NULL))
    {
        ALOGD("[BITBLT][ERROR] mHalIoCtrl() - MT65XX_HW_BITBLT Can't UnLock!!!!, TID:%d", gettid());
    }
    else
    {
        ALOGD("[BITBLT] mHalIoCtrl() - MT65XX_HW_BITBLT UnLock!!!!, TID:%d", gettid());
    }
#endif

#if 0 // dump converted frame
    char buf[255];
    sprintf(buf, "/data/out_%d_%d.yuv", dstWidth, dstHeight);
    FILE *fp = fopen(buf, "ab");
    if (fp)
    {
        fwrite((void *)dstYUVbuf_va, 1, YUV_SIZE(dstWidth, dstHeight), fp);
        fclose(fp);
    }
#endif

#else
    uint8_t   *srcYUVbuf_va = NULL;
    uint8_t   *dstYUVbuf_va = NULL;
#if 0
    uint32_t  srcWStride = ROUND_16(srcWidth);
    uint32_t  srcHStride = ROUND_32(srcHeight);
    uint32_t  srcBufferSize = srcWStride * srcHStride * 3 >> 1;
    uint32_t  dstBufferSize = dstWidth * dstHeight * 3 >> 1;

    srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
    srcYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, srcBufferSize);    // 32 byte alignment for MDP
#else
    uint32_t  srcWStride = 0;
    uint32_t  srcHStride = 0;
    uint32_t  srcBufferSize = 0;
    uint32_t  dstBufferSize = dstWidth * dstHeight * 3 >> 1;

    srcBufferSize = 0;
    srcYUVbuf_va = 0;

    uint32_t uvStrideMode = UV_STRIDE_16_8_8;
#endif

    uint8_t *srcYUVbufArray[3];
    unsigned int srcYUVbufSizeArray[3];
    uint32_t numPlanes = 2;

    if (mSrcColorFormat == OMX_COLOR_FormatVendorMTKYUV
        || mSrcColorFormat == OMX_COLOR_FormatVendorMTKYUV_FCM)
    {
        if (srcStride != 0)
        {
            srcWStride = srcStride;
        }
        else
        {
            srcWStride = ROUND_16(srcWidth);
        }
        if (srcSliceHeight != 0)
        {
            srcHStride = srcSliceHeight;
        }
        else
        {
            srcHStride = ROUND_32(srcHeight);
        }

        srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
        srcYUVbuf_va = (uint8_t *)memalign(MEM_ALIGN_32, srcBufferSize);

        srcYUVbufArray[0] = srcYUVbuf_va;      // Y
        srcYUVbufArray[1] = srcYUVbuf_va + srcWStride * srcHStride;  // C
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = srcWStride * srcHStride / 2;
        numPlanes = 2;
        uvStrideMode = UV_STRIDE_16_8_8;
    }
    else if (mSrcColorFormat == OMX_COLOR_FormatYUV420Planar)
    {
        srcWStride = srcWidth;
        srcHStride = srcHeight;

        srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
        srcYUVbuf_va = (uint8_t *)memalign(MEM_ALIGN_32, srcBufferSize);

        srcYUVbufArray[0] = srcYUVbuf_va;
        srcYUVbufArray[1] = srcYUVbuf_va + (srcWStride * srcHStride);
        srcYUVbufArray[2] = srcYUVbufArray[1] + (srcWStride * srcHStride) / 4;
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = (srcWStride * srcHStride) / 4;
        srcYUVbufSizeArray[2] = (srcWStride * srcHStride) / 4;
        numPlanes = 3;
        uvStrideMode = UV_STRIDE_16_8_8;
    }
    else if (mSrcColorFormat == OMX_MTK_COLOR_FormatYV12)
    {
        if (srcStride != 0)
        {
            srcWStride = srcStride;
        }
        else
        {
            srcWStride = ROUND_16(srcWidth);
        }
        if (srcSliceHeight != 0)
        {
            srcHStride = srcSliceHeight;
        }
        else
        {
            srcHStride = ROUND_16(srcHeight);
        }

        // for YV12 16,16,16 stride
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = ROUND_16(srcWStride / 2) * (srcHStride / 2);
        srcYUVbufSizeArray[2] = ROUND_16(srcWStride / 2) * (srcHStride / 2);
        srcBufferSize = srcYUVbufSizeArray[0] + srcYUVbufSizeArray[1] + srcYUVbufSizeArray[2];
        srcYUVbuf_va = (uint8_t *)memalign(MEM_ALIGN_32, srcBufferSize);
        srcYUVbufArray[0] = srcYUVbuf_va;
        srcYUVbufArray[1] = srcYUVbuf_va + srcYUVbufSizeArray[0];
        srcYUVbufArray[2] = srcYUVbufArray[1] + srcYUVbufSizeArray[1];
        numPlanes = 3;
        uvStrideMode = UV_STRIDE_16_16_16;
    }
    else
    {
        ALOGE("ERROR not supported color format: mSrcColorFormat(0x%08X)", mSrcColorFormat);
    }

    dstYUVbuf_va = output;
    memcpy(srcYUVbuf_va, input, srcBufferSize);

    uint8_t *dstYUVbufArray[3];
    unsigned int dstYUVbufSizeArray[3];
    dstYUVbufArray[0] = dstYUVbuf_va;
    dstYUVbufArray[1] = dstYUVbuf_va + (dstWidth * dstHeight);
    dstYUVbufArray[2] = dstYUVbufArray[1] + (dstWidth * dstHeight) / 4;
    dstYUVbufSizeArray[0] = dstWidth * dstHeight;
    dstYUVbufSizeArray[1] = (dstWidth * dstHeight) / 4;
    dstYUVbufSizeArray[2] = (dstWidth * dstHeight) / 4;

    DpBlitStream blitStream;
    DpColorFormat srcColorFormat = OmxColorToDpColor(mSrcColorFormat);
    DpColorFormat dstColorFormat = OmxColorToDpColor(mDstColorFormat);

    ALOGD("srcBufferSize(%d), srcColorFormat(%d), dstBufferSize(%d), dstColorFormat(%d)", srcBufferSize, srcColorFormat, dstBufferSize, dstColorFormat);

    DpRect srcRoi;
    srcRoi.x = 0;
    srcRoi.y = 0;
    srcRoi.w = srcWidth;
    srcRoi.h = srcHeight;

    int32_t crop_padding_left, crop_padding_top, crop_padding_right, crop_padding_bottom;
    if (!mVideoSource->getFormat()->findRect(
            kKeyCropRect,
            &crop_padding_left, &crop_padding_top, &crop_padding_right, &crop_padding_bottom))
    {
        ALOGE("kKeyCropPaddingRect not found\n");
        //srcRoi.x = srcRoi.y = 0;
        //srcRoi.w = srcWidth - 1;
        //srcRoi.h = srcHeight - 1;
        srcRoi.x = 0;
        srcRoi.y = 0;
        srcRoi.w = srcWidth;
        srcRoi.h = srcHeight;
    }
#if 1
    else
    {
        srcRoi.x = crop_padding_left;
        srcRoi.y = crop_padding_top;
        srcRoi.w = crop_padding_right - crop_padding_left;
        srcRoi.h = crop_padding_bottom - crop_padding_top;
    }
#else
    sp<MetaData> inputFormat = mVideoSource->getFormat();
    const char *mime;
    if (inputFormat->findCString(kKeyMIMEType, &mime))
    {
        ALOGD("%s %s", mime, MEDIA_MIMETYPE_VIDEO_VP9);

        if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_VP9))
        {
            srcRoi.x = crop_padding_left;
            srcRoi.y = crop_padding_top;
            srcRoi.w = crop_padding_right - crop_padding_left;
            srcRoi.h = crop_padding_bottom - crop_padding_top;
            ALOGD("crop_padding_left i %d r %d t %d b %d",
                  crop_padding_left, crop_padding_right, crop_padding_top, crop_padding_bottom);
        }
    }
#endif

    ALOGD("@ srcRoi x %d y %d w %d h %d", srcRoi.x, srcRoi.y, srcRoi.w, srcRoi.h);


    blitStream.setSrcBuffer((void **)srcYUVbufArray, (unsigned int *)srcYUVbufSizeArray, numPlanes);
#if 0
    blitStream.setSrcConfig(srcWStride, srcHStride, srcColorFormat, eInterlace_None, &srcRoi);
#else
    unsigned int yPitch = ((srcWStride) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
    unsigned int uvPitch;

    switch (uvStrideMode)
    {
        case UV_STRIDE_16_16_16:
            uvPitch = (ROUND_16(srcWStride / 2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
            break;

        case UV_STRIDE_16_8_8:
            uvPitch = ((srcWStride / 2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
            break;

        default:  // use 16_8_8
            uvPitch = ((srcWStride / 2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
            break;
    }

    //ALOGD ("yPitch(%d), uvPitch(%d), srcWStride=%d srcHStride=%d", yPitch, uvPitch, srcWStride, srcHStride);
    blitStream.setSrcConfig(srcWStride, srcHStride, yPitch, uvPitch, srcColorFormat, DP_PROFILE_BT601, eInterlace_None, &srcRoi);
#endif

    DpRect dstRoi;
    dstRoi.x = 0;
    dstRoi.y = 0;
    dstRoi.w = dstWidth;
    dstRoi.h = dstHeight;
    blitStream.setDstBuffer((void **)dstYUVbufArray, (unsigned int *)dstYUVbufSizeArray, 3);
#if 0
    blitStream.setDstConfig(dstWidth, dstHeight, dstColorFormat, eInterlace_None, &dstRoi);
#else
    yPitch = ((dstWidth) * DP_COLOR_BITS_PER_PIXEL(dstColorFormat)) >> 3;
    uvPitch = ((dstWidth / 2) * DP_COLOR_BITS_PER_PIXEL(dstColorFormat)) >> 3;
    blitStream.setDstConfig(dstWidth, dstHeight, yPitch, uvPitch, dstColorFormat, DP_PROFILE_BT601, eInterlace_None, &dstRoi);
#endif
    blitStream.invalidate();

    if (srcYUVbuf_va)
    {
        free(srcYUVbuf_va);
        srcYUVbuf_va = NULL;
    }

#if 0 // dump converted frame
    char buf[255];
    sprintf(buf, "/storage/sdcard1/out_%d_%d.yuv", dstWidth, dstHeight);
    FILE *fp = fopen(buf, "ab");
    if (fp)
    {
        fwrite((void *)dstYUVbufArray[0], 1, YUV_SIZE(dstWidth, dstHeight), fp);
        fclose(fp);
    }
#endif
#endif
    ALOGD("-MemorySource::convertVideoFrame");
}

bool MemorySource::cut(int64_t begin_ts, int64_t end_ts)
{
    ALOGD("MemorySource::cut begin=%lld, end=%lld, duration=%lld", begin_ts, end_ts, mSourceDurationUs);

    if (mSourceDurationUs > 0)
    {
        if (begin_ts < 0) { begin_ts = 0; }
        if (end_ts > mSourceDurationUs) { end_ts = mSourceDurationUs - 1000L; }
        if (begin_ts < end_ts)
        {
            mBeginTs = begin_ts;
            mEndTs = end_ts;
            return true;
        }
        else
        {
            mBeginTs = -1;
            mEndTs = -1;
            return false;
        }
    }
    else
    {
        return false;
    }
}

void MemorySource::getActualTimeBoundary(int64_t &begin_ts, int64_t &end_ts, int64_t &clip_duration)
{
    begin_ts = mBeginTs;
    end_ts = mEndTs;
    clip_duration = mSourceDurationUs;
}

extern "C" int mtk_video_transcoder_transcode(Mtk_VideoTranscoder_Context pContext, Mtk_VideoTranscoder_Params params)
{
    android::ProcessState::self()->startThreadPool();


    ALOGD("mtk_video_transcoder_transcode pContext(0x%08X)", pContext);

    status_t err = OK;

    MtkVideoTranscoderInternalCtxt *pInternalCtxt = (MtkVideoTranscoderInternalCtxt *)pContext;

    sp<MemorySource> mMemSource = new MemorySource(pInternalCtxt, params.input_path, 0x7F000001, params.target_width, params.target_height, params.target_frame_rate,  err);

    if (err != OK)
    {
        ALOGE("@@ new MemorySource error!! (0x%08X)", err);
        return err;
    }

    mMemSource->cut(params.begin_ts * 1000LL, params.end_ts * 1000LL);

    int32_t width, height, stride, sliceHeight;
    int64_t durationUs;
    CHECK(mMemSource->getFormat()->findInt32(kKeyWidth, &width));
    CHECK(mMemSource->getFormat()->findInt32(kKeyHeight, &height));
    CHECK(mMemSource->getFormat()->findInt64(kKeyDuration, &durationUs));
    stride = width;
    sliceHeight = height;

    // setup meta data for encoder
    sp<MetaData> encMeta = new MetaData;
    encMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);
    encMeta->setInt32(kKeyWidth, width);
    encMeta->setInt32(kKeyHeight, height);
    //encMeta->setInt32(kKeyFrameRate, 30);
    //encMeta->setInt32(kKeyBitRate, 512*1024);
    if (params.target_frame_rate <= 1)
    {
        encMeta->setInt32(kKeyFrameRate, 30);
    }
    else
    {
        encMeta->setInt32(kKeyFrameRate,  params.target_frame_rate);
    }
    encMeta->setInt32(kKeyBitRate, params.target_bit_rate);
    encMeta->setInt32(kKeyStride, stride);
    encMeta->setInt32(kKeySliceHeight, sliceHeight);
    encMeta->setInt32(kKeyIFramesInterval, 1); //second
    //encMeta->setInt32(kKeyColorFormat, OMX_COLOR_FormatVendorMTKYUV);   // MT6573
    encMeta->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);    // MT6575

    // keep rotation info
    int32_t rotationDegrees;
    if (mMemSource->getFormat()->findInt32(kKeyRotation, &rotationDegrees))
    {
        encMeta->setInt32(kKeyRotation, rotationDegrees);
    }
    else
    {
        ALOGD("Cannot find kKeyRotation B !!!");
    }

    // create video encoder
    OMXClient mClient;
    CHECK_EQ(mClient.connect(), (status_t)OK);
    sp<MediaSource> encoder = OMXCodec::Create(mClient.interface(), encMeta, true /* createEncoder */, mMemSource);


    // create MPEG4 writer
    sp<MPEG4Writer> writer;
    int fd = open(params.output_path
    , O_CREAT | O_LARGEFILE | O_TRUNC | O_RDWR, S_IRUSR | S_IWUSR);
    if (fd >= 0) {
        writer = new MPEG4Writer(fd);
    }
    else {
        return MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
    }

    writer->addSource(encoder);
    writer->setMaxFileDuration(durationUs + 10 * 1000 * 1000LL); // MAX duration + 10 seconds

    // start to record
    //CHECK_EQ(OK, writer->start());
    //CHECK_EQ((status_t)OK, writer->start(encMeta.get()));  // keep rotation info
    if ((status_t)OK != writer->start(encMeta.get()))
    {
        ALOGE("MPEG4Writer start failed !");
        close(fd);
        return MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
    }

    int64_t _begin_ts, _end_ts, _clip_duration;
    mMemSource->getActualTimeBoundary(_begin_ts, _end_ts, _clip_duration);
    if ((_begin_ts == -1) || (_end_ts == -1))
    {
        _begin_ts = 0;
        _end_ts = _clip_duration;
    }
    if ((_begin_ts == 0) && (_end_ts == 0))
    {
        ALOGE("[ERROR] Invalid cut time");
        close(fd);
        return MTK_VIDEO_TRANSCODER_ERROR_INVALID_CUT_TIME;
    }
    //ALOGD ("@@ _begin_ts=%lld, _end_ts=%lld, _clip_duration=%lld", _begin_ts, _end_ts, _clip_duration);

    // calculate progress
    while ((!writer->reachedEOS()) && (!pInternalCtxt->Cancelled))
    {
        //fprintf(stderr, ".");
        //ALOGD ("@@ writer->getMaxDurationUs()=%lld", writer->getMaxDurationUs());
        // pInternalCtxt->Progress = ((writer->getMaxDurationUs() * 100LL) / (_end_ts - _begin_ts));  // zxy fix build error
        //if (g_uProgress < 0) g_uProgress = 0;
        if (pInternalCtxt->Progress > 100) { pInternalCtxt->Progress = 100; }
        fprintf(stderr, "\r[%3d%%]", pInternalCtxt->Progress);
        usleep(100000);
    }

    err = writer->stop();

    if (pInternalCtxt->LastError)
    {
        ALOGD("do_transcode return 0x%08X", pInternalCtxt->LastError);
        close(fd);
        return pInternalCtxt->LastError;
    }

    pInternalCtxt->Progress = 100;
    fprintf(stderr, "\r[%3d%%]\n", pInternalCtxt->Progress);
    close(fd);
    ALOGD("trasnscode exit");
    return MTK_VIDEO_TRANSCODER_ERROR_NONE;
}

extern "C" unsigned int mtk_video_transcoder_get_progress(Mtk_VideoTranscoder_Context pContext)
{
    MtkVideoTranscoderInternalCtxt *pInternalCtxt = (MtkVideoTranscoderInternalCtxt *)pContext;
    ALOGD("mtk_video_transcoder_get_progress (%d), pContext(0x%08X)", pInternalCtxt->Progress, pContext);
    return pInternalCtxt->Progress;
}

extern "C" void mtk_video_transcoder_cancel(Mtk_VideoTranscoder_Context pContext)
{
    ALOGD("mtk_video_transcoder_cancel pContext(0x%08X)", pContext);
    MtkVideoTranscoderInternalCtxt *pInternalCtxt = (MtkVideoTranscoderInternalCtxt *)pContext;
    pInternalCtxt->Cancelled = true;
}

extern "C" void mtk_video_transcoder_init(Mtk_VideoTranscoder_Context *pContext)
{
    MtkVideoTranscoderInternalCtxt *pInternalCtxt = new MtkVideoTranscoderInternalCtxt;
    pInternalCtxt->Cancelled = false;
    pInternalCtxt->Progress = 0;
    pInternalCtxt->LastError = 0;

    *pContext = pInternalCtxt;

    ALOGD("mtk_video_transcoder_init pContext(0x%08X)", pInternalCtxt);
}

extern "C" void mtk_video_transcoder_deinit(Mtk_VideoTranscoder_Context pContext)
{
    MtkVideoTranscoderInternalCtxt *pInternalCtxt = (MtkVideoTranscoderInternalCtxt *)pContext;
    ALOGD("mtk_video_transcoder_deinit pContext(0x%08X)", pInternalCtxt);
    delete pInternalCtxt;
}
