/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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


#define LOG_TAG "MtkHardwareRenderer"
#include <utils/Log.h>
#include <sys/time.h>
// Morris Yang 20100826
#include "pmem_util.h"
#include "MtkHardwareRenderer.h"
#include <ui/PixelFormat.h>
#include <ui/egl/android_natives.h>
#include <binder/MemoryHeapBase.h>
#include <binder/MemoryHeapBaseMTK.h>
#include <binder/MemoryHeapPmem.h>
#include <media/stagefright/MediaDebug.h>
#include <surfaceflinger/ISurface.h>
#include <cutils/properties.h>
    
       
namespace android {

int64_t getTickCountMs()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)(tv.tv_sec*1000LL + tv.tv_usec/1000);
}


MtkHardwareRenderer::MtkHardwareRenderer(
        OMX_COLOR_FORMATTYPE colorFormat,
        const sp<ISurface> &surface,
        size_t displayWidth, size_t displayHeight,
        size_t decodedWidth, size_t decodedHeight,
        int32_t rotationDegrees)
    : mColorFormat(colorFormat),
      mConverter(colorFormat, OMX_COLOR_Format16bitRGB565),
      mISurface(surface),
      mDisplayWidth(displayWidth),
      mDisplayHeight(displayHeight),
      mDecodedWidth(decodedWidth),
      mDecodedHeight(decodedHeight),
      mFrameSize(mDecodedWidth * mDecodedHeight * 2),  // RGB565
      mRotationDegrees(rotationDegrees),
      mIndex(0) {

    LOGD ("@@ MtkHardwareRenderer dynamic version");
    iIsFirstWriteFrameBuf = true;
    mFrameCount = 0;
    mFirstPostBufferTime = 0;
    mDbgFlags = 0;

    char value[PROPERTY_VALUE_MAX];
    property_get("sf.showfps", value, "1");	// enable by default temporarily
    bool _res = atoi(value);
    if (_res) mDbgFlags |= SF_SHOW_FPS;
}

MtkHardwareRenderer::~MtkHardwareRenderer() {
    mISurface->unregisterBuffers();
}

void MtkHardwareRenderer::render(
        const void *data, size_t size, void *platformPrivate) {

    PmemInfo pmemInfo;
    sf_pmem_get_info((void*)data, &pmemInfo);
  
    if (iIsFirstWriteFrameBuf)
    {
        iIsFirstWriteFrameBuf = false;

        int displayWidth = mDisplayWidth;
        int displayHeight = mDisplayHeight;
        int frameWidth = mDecodedWidth;
        int frameHeight = mDecodedHeight;
        int frameSize = mFrameSize;
        
        displayWidth = (displayWidth + 1) & -2;
        displayHeight = (displayHeight + 1) & -2;
        frameWidth = (frameWidth + 1) & -2;
        frameHeight = (frameHeight + 1) & -2;
        frameSize = frameWidth * frameHeight * 2;
            
        PmemInfo* _pmemInfo = (PmemInfo*)&pmemInfo;

        // create frame buffer heap and register with surfaceflinger
        LOGE("render first frame buffer - fd: %d, base: 0x0%X, size: %d (output format = 0x%X, RotationDegrees = %d)", 
            _pmemInfo->fd, (unsigned int)_pmemInfo->base, _pmemInfo->size, mColorFormat, mRotationDegrees);          
           
        PixelFormat pixelFormat;
        switch (mColorFormat) {
            case OMX_MTK_COLOR_FormatYUV:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER_MTK;
                break;
            case OMX_COLOR_FormatYUV420Planar:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER;
                break;
            case OMX_MTK_COLOR_FormatYV12:
                /*
                 * FIXME: correct pixelFormat
                 */  
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER;
                break;    
            default:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER_MTK;
                break;
       }
        
	const char *pmemDev = "/dev/pmem_multimedia";
	mHeap = new MemoryHeapBaseMTK(
                    _pmemInfo->shared_fd, _pmemInfo->base, _pmemInfo->size, MemoryHeapBase::DONT_MAP_LOCALLY, pmemDev);
        if (mHeap->heapID() < 0) {
            LOGE("Error creating frame buffer heap (MemoryHeapBaseMTK)");
            return;
        }

	uint32_t orientation;
	switch (mRotationDegrees) {
		case 0: orientation = ISurface::BufferHeap::ROT_0; break;
		case 90: orientation = ISurface::BufferHeap::ROT_90; break;
		case 180: orientation = ISurface::BufferHeap::ROT_180; break;
		case 270: orientation = ISurface::BufferHeap::ROT_270; break;
		default: orientation = ISurface::BufferHeap::ROT_0; break;
	}
	
        mBufferHeap = ISurface::BufferHeap(displayWidth, displayHeight,
                                         frameWidth, frameHeight, pixelFormat, orientation, 0, mHeap);

        mISurface->registerBuffers(mBufferHeap);
#if defined(MTK_TVOUT_SUPPORT)
	 mISurface->setLayerType(LAYER_TYPE_VIDEO);
#endif

	if (mDbgFlags & SF_SHOW_FPS) {
		mFirstPostBufferTime = getTickCountMs();
	}
    }

    //LOGE("post buffer(%d) in: base:0x%x, offset=0x%x", gettid(), pmemInfo.base, pmemInfo.offset);
    
	if (mDbgFlags & SF_SHOW_FPS) {
		mFrameCount++;
		if (0 == (mFrameCount % 60)) {        
			int64_t _diff = getTickCountMs() - mFirstPostBufferTime;
			double fps = (double)1000*mFrameCount/_diff;
			LOGE ("FPS = %.2f", fps);
		}
	}

    mISurface->postBuffer(pmemInfo.offset);
    //LOGE("post buffer(%d) out: %x", gettid(), pmemInfo.offset);
}


// <--- Morris Yang 20110322 add for RV resizing
void MtkHardwareRenderer::render(
            const void *data, size_t size, void *platformPrivate, uint32_t width, uint32_t height, uint32_t stride, uint32_t slice_height)
{

    PmemInfo pmemInfo;
    sf_pmem_get_info((void*)data, &pmemInfo);
  
    if (iIsFirstWriteFrameBuf)
    {
        iIsFirstWriteFrameBuf = false;

        int displayWidth = mDisplayWidth;
        int displayHeight = mDisplayHeight;
        int frameWidth = mDecodedWidth;
        int frameHeight = mDecodedHeight;
        int frameSize = mFrameSize;
        
        displayWidth = (displayWidth + 1) & -2;
        displayHeight = (displayHeight + 1) & -2;
        frameWidth = (frameWidth + 1) & -2;
        frameHeight = (frameHeight + 1) & -2;
        frameSize = frameWidth * frameHeight * 2;
            
        PmemInfo* _pmemInfo = (PmemInfo*)&pmemInfo;

	 LOGE ("(render2) displayWidth=%d, displayHeight=%d, frameWidth=%d, frameHeight=%d", displayWidth, displayHeight, frameWidth, frameHeight);

        
        // create frame buffer heap and register with surfaceflinger
        LOGE("(render2) render first frame buffer - fd: %d, base: 0x0%X, size: %d (output format = 0x%X, RotationDegrees = %d)", 
            _pmemInfo->fd, (unsigned int)_pmemInfo->base, _pmemInfo->size, mColorFormat, mRotationDegrees);          

        PixelFormat pixelFormat;
        switch (mColorFormat) {
            case OMX_MTK_COLOR_FormatYUV:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER_MTK;
                break;
            case OMX_COLOR_FormatYUV420Planar:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER;
                break;
            case OMX_MTK_COLOR_FormatYV12:
                /*
                 * FIXME: correct pixelFormat
                 */  
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER;
                break;                
            default:
                pixelFormat = PIXEL_FORMAT_YUV_420_PLANER_MTK;
                break;
       }

	const char *pmemDev = "/dev/pmem_multimedia";
	mHeap = new MemoryHeapBaseMTK(
                    _pmemInfo->shared_fd, _pmemInfo->base, _pmemInfo->size, MemoryHeapBase::DONT_MAP_LOCALLY, pmemDev);
        if (mHeap->heapID() < 0) {
            LOGE("Error creating frame buffer heap (MemoryHeapBaseMTK)");
            return;
        }

	uint32_t orientation;
	switch (mRotationDegrees) {
		case 0: orientation = ISurface::BufferHeap::ROT_0; break;
		case 90: orientation = ISurface::BufferHeap::ROT_90; break;
		case 180: orientation = ISurface::BufferHeap::ROT_180; break;
		case 270: orientation = ISurface::BufferHeap::ROT_270; break;
		default: orientation = ISurface::BufferHeap::ROT_0; break;
	}
	
        mBufferHeap = ISurface::BufferHeap(displayWidth, displayHeight,
                                         frameWidth, frameHeight, pixelFormat, orientation, 0, mHeap);

        mISurface->registerBuffers(mBufferHeap);
#if defined(MTK_TVOUT_SUPPORT)
	 //mISurface->setLayerType(LAYER_TYPE_VIDEO);
#endif

	if (mDbgFlags & SF_SHOW_FPS) {
		mFirstPostBufferTime = getTickCountMs();
	}
    }

    //LOGE("post buffer(%d) in: base:0x%x, offset=0x%x", gettid(), pmemInfo.base, pmemInfo.offset);
    
	if (mDbgFlags & SF_SHOW_FPS) {
		mFrameCount++;
		if (0 == (mFrameCount % 60)) {        
			int64_t _diff = getTickCountMs() - mFirstPostBufferTime;
			double fps = (double)1000*mFrameCount/_diff;
			LOGE ("FPS = %.2f", fps);
		}
	}

	//LOGE ("render2 (%u, %u)", width, height);
       mISurface->postBuffer(pmemInfo.offset, width, height, stride, slice_height);

    //LOGE("post buffer(%d) out: %x", gettid(), pmemInfo.offset);
}
// --->

}  // namespace android
