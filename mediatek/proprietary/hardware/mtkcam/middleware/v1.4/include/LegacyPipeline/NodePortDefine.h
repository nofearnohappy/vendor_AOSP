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

#ifndef _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_NODEPORTDEFINE_H_
#define _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_NODEPORTDEFINE_H_

namespace NSCam {
namespace v1 {
namespace NSLegacyPipeline {

    typedef enum{
        EIn = 0x0,
        EOut = 0x1000,
        EInOutMask = EIn | EOut,

        EP1InAppMeta = 0x0 | EIn,
        EP1InHalMeta = 0x1 | EIn,
        EP1OutAppMeta = 0x2 | EOut,
        EP1OutHalMeta = 0x3 | EOut,
        EP1OutImageResizer = 0x4 | EOut,
        EP1OutImageFull = 0x5 | EOut,

        EP2InAppMeta = 0x10 | EIn,
        EP2InHalMeta = 0x11 | EIn,
        EP2OutAppMeta = 0x12 | EOut,
        EP2OutHalMeta = 0x13 | EOut,
        EP2InFullRaw = 0x14 | EIn,
        EP2InResizedRaw = 0x15 | EIn,
        EP2OutImage = 0x16 | EOut,
        EP2OutFDImage = 0x17 | EOut,

        EJpegInAppMeta = 0x50 | EIn,
        EJpegInHalMeta = 0x51 | EIn,
        EJpegOutAppMeta = 0x52 | EOut,
        EJpegInPictureYuv = 0x53 | EIn,
        EJpegInThumbnailYuv = 0x54 | EIn,
        EJpegOutJpeg = 0x55 | EOut
    }ENodePort;

}; //namespace NSLegacyPipeline
}; //namespace v1
}; //namespace NSCam
#endif
