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

package com.mediatek.mediatekdm.test;

import android.content.Context;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import java.io.File;

public class MockRenamingDelegatingContext extends RenamingDelegatingContext {
    public static final String DEFAULT_PREFIX = "test_";

    private static final String TAG = "MDMTest/MockRenamingDelegatingContext";
    private final Object mSync = new Object();
    private final Context mTargetContext;
    private final String mFilePrefix;

    public MockRenamingDelegatingContext(Context targetContext, String prefix) {
        super(targetContext, prefix);
        makeExistingFilesAndDbsAccessible();
        Log.d(TAG, "MockRenamingDelegatingContext(" + targetContext + ", " + prefix + ")");
        mTargetContext = targetContext;
        mFilePrefix = prefix;
    }

    public MockRenamingDelegatingContext(Context targetContext) {
        this(targetContext, DEFAULT_PREFIX);
    }

    // @Override
    // public File getDir(String name, int mode) {
    // return mTargetContext.getDir(renamedFileName(name), mode);
    // }
    //
    // @Override
    // public File getFilesDir() {
    // File filesDir = new File(mTargetContext.getFilesDir(), renamedFileName("files"));
    // synchronized (mSync) {
    // if (!filesDir.exists()) {
    // if (!filesDir.mkdirs()) {
    // Log.w(TAG, "Unable to create files directory");
    // return null;
    // }
    // FileUtils.setPermissions(
    // filesDir.getPath(),
    // FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IXOTH,
    // -1,
    // -1);
    // }
    // }
    // return filesDir;
    // }
    //
    private String renamedFileName(String name) {
        return mFilePrefix + name;
    }

    public void clearRenamedFiles() {
        File filesDir = mTargetContext.getFilesDir();
        Log.d(TAG, "Parent is " + filesDir.getParentFile());
        Utilities.removeDirectoryRecursively(filesDir.getParentFile(), mFilePrefix);
    }
}
