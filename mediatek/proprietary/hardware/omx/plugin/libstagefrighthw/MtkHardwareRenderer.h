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
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MTK_HARDWARE_RENDERER_H_

#define MTK_HARDWARE_RENDERER_H_

#include <surfaceflinger/ISurface.h>
#include <media/stagefright/ColorConverter.h>
#include <media/stagefright/VideoRenderer.h>
#include <utils/RefBase.h>

#define SF_SHOW_FPS (1 << 0)

namespace android {

class ISurface;
class MemoryHeapBase;
class MemoryHeapBaseMTK;

class MtkHardwareRenderer : public VideoRenderer {
public:
    MtkHardwareRenderer(
            OMX_COLOR_FORMATTYPE colorFormat,
            const sp<ISurface> &surface,
            size_t displayWidth, size_t displayHeight,
            size_t decodedWidth, size_t decodedHeight,
            int32_t rotationDegrees = 0);

    virtual ~MtkHardwareRenderer();

    virtual void render(
            const void *data, size_t size, void *platformPrivate);

// <--- Morris Yang 20110322 add for RV resizing
    virtual void render(
            const void *data, size_t size, void *platformPrivate, uint32_t width, uint32_t height, uint32_t stride, uint32_t slice_height);
// --->

private:
    OMX_COLOR_FORMATTYPE mColorFormat;
    ColorConverter mConverter;
    sp<ISurface> mISurface;
    size_t mDisplayWidth, mDisplayHeight;
    size_t mDecodedWidth, mDecodedHeight;
    size_t mFrameSize;
    int32_t mRotationDegrees;
    sp<MemoryHeapBaseMTK> mHeap;
    int mIndex;

    bool iIsFirstWriteFrameBuf;
    ISurface::BufferHeap mBufferHeap;

    MtkHardwareRenderer(const MtkHardwareRenderer &);
    MtkHardwareRenderer &operator=(const MtkHardwareRenderer &);
    uint32_t mFrameCount;
    int64_t mFirstPostBufferTime;
    uint32_t mDbgFlags;
};


}  // namespace android

#endif  // QCOM_HARDWARE_RENDERER_H_
