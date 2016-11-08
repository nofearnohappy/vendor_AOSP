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

package com.mediatek.browser.hotknot;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.hotknot.HotKnotMessage;

public class HotKnotHandler {
    private static final String LOGTAG = "browser/HotKnotHandler";

    public static final String MIME_TYPE = "com.mediatek.browser.hotknot/com.mediatek.browser.hotknot.MIME_TYPE";
    private static final String ACTION_SHARE = "com.mediatek.hotknot.action.SHARE";
    private static final String EXTRA_SHARE_MSG = "com.mediatek.hotknot.extra.SHARE_MSG";
    private static HotKnotAdapter mHotKnotAdapter = null;
    private static Activity mActivity = null;

    public static void hotKnotInit(Activity activity) {
        mActivity = activity;
        mHotKnotAdapter = HotKnotAdapter.getDefaultAdapter(mActivity);
        if (null == mHotKnotAdapter) {
            Log.d(LOGTAG, "hotKnotInit fail, hotKnotAdapter is null");
            return;
        }

        Log.d(LOGTAG, "hotKnotInit completed");
    }

    public static boolean isHotKnotSupported() {
        if (null != mHotKnotAdapter) {
            return true;
        }
        return false;
    }

    public static void hotKnotStart(String url) {
        Log.d(LOGTAG, "hotKnotStart, url:" + url);
        if (null == mHotKnotAdapter) {
            Log.e(LOGTAG, "hotKnotStart fail, hotKnotAdapter is null");
            return;
        }
        if (null == url || url.length() == 0) {
            Log.e(LOGTAG, "hotKnotStart fail, url:" + (null == url ? "url" : url));
            return;
        }

        HotKnotMessage message = new HotKnotMessage(MIME_TYPE, url.getBytes());
        Intent intent = new Intent(ACTION_SHARE);
        intent.putExtra(EXTRA_SHARE_MSG, message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        mActivity.startActivity(intent);
    }
}
