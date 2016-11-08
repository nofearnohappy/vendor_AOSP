/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.plugin;

import com.google.android.mms.ContentType;

import java.util.ArrayList;

/**
 * M: Utils Class for unsupported feature.
 */
public class MmsContentType {

    private static final ArrayList<String> sSupportedContentTypes = ContentType.getSupportedTypes();
    private static final ArrayList<String> sSupportedImageTypes = ContentType.getImageTypes();
    private static final ArrayList<String> sSupportedAudioTypes = ContentType.getAudioTypes();
    private static final ArrayList<String> sSupportedVideoTypes = ContentType.getVideoTypes();

    /// M: support some new ContentTypes
    private static final String APP_OCET_STREAM = "application/octet-stream";
    private static final String TEXT_TS = "text/texmacs";

    public static final String IMAGE_BMP = "image/x-ms-bmp";
    private static final String IMAGE_XBMP = "image/bmp";

    private static final String AUDIO_WAV = "audio/x-wav";
    private static final String AUDIO_AWB = "audio/amr-wb";
    private static final String AUDIO_WMA = "audio/x-ms-wma";
    private static final String AUDIO_VORBIS = "audio/vorbis";
    private static final String AUDIO_SP_MIDI = "audio/sp-midi";

    private static final String VIDEO_TS = "video/mp2ts";

    static {
        sSupportedContentTypes.add(TEXT_TS);
        sSupportedContentTypes.add(IMAGE_BMP);
        sSupportedContentTypes.add(IMAGE_XBMP);
        sSupportedContentTypes.add(AUDIO_WAV);
        sSupportedContentTypes.add(AUDIO_AWB);
        sSupportedContentTypes.add(AUDIO_WMA);
        sSupportedContentTypes.add(AUDIO_VORBIS);
        sSupportedContentTypes.add(VIDEO_TS);

        // add supported image types
        sSupportedImageTypes.add(IMAGE_BMP);
        sSupportedImageTypes.add(IMAGE_XBMP);

        // add supported audio types
        sSupportedAudioTypes.add(AUDIO_WAV);
        sSupportedAudioTypes.add(AUDIO_AWB);
        sSupportedAudioTypes.add(AUDIO_WMA);
        sSupportedAudioTypes.add(AUDIO_VORBIS);
        sSupportedAudioTypes.add(AUDIO_SP_MIDI);

        // add supported video types
        sSupportedVideoTypes.add(VIDEO_TS);
        sSupportedVideoTypes.add(TEXT_TS);
        sSupportedContentTypes.add(APP_OCET_STREAM);

    }

    /**
     * Check the contentType is supported whether or not.
     *
     * @param contentType
     *            the file contenType.
     * @return true: supported; false: unSupported.
     */
    public static boolean isSupportedType(String contentType) {
        return sSupportedContentTypes.contains(contentType)
            || sSupportedImageTypes.contains(contentType)
            || sSupportedAudioTypes.contains(contentType)
            || sSupportedVideoTypes.contains(contentType);
    }

}
