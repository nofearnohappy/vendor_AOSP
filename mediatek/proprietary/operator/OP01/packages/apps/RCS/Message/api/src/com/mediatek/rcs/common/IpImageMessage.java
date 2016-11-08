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

package com.mediatek.rcs.common;

import android.util.Log;

import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.service.FileStruct;

public class IpImageMessage extends IpAttachMessage {

    private int mWidthInPixel;
    private int mHeightInPixel;
    private String mCaption;         // max length is 100
    private String mThumbPath;       // for send msg, it's ""
    private String mFileName;

    private static final String TAG = "IpImageMessage";

    public IpImageMessage(FileStruct fileStruct, String remote) {
        super();
        Log.d(TAG, "PluginIpImageMessage(), fileStruct = " + fileStruct + " remote = " + remote);
        //setSimId((int) RcsMessageUtils.DUMMY_SIM_ID);
        setPath(fileStruct.mFilePath);
        setSize((int) (fileStruct.mSize));
        mFileName = fileStruct.mName;
        setType(IpMessageType.PICTURE);
        Log.d(TAG, " [BurnedMsg]: -------- image Burn = " + fileStruct.mSessionType);
        setBurnedMessage(fileStruct.mSessionType);
        setFrom(remote);
        setTo(remote);
        mThumbPath = fileStruct.mThumbnail;
        setTag(fileStruct.mFileTransferTag);
        analysisAttribute();
    }

    public IpImageMessage() {
        super();
    }

    private void analysisAttribute() {
        /*
        long imageId = -1;
        String whereClause = MediaStore.Images.Media.DISPLAY_NAME + "='" + mFileName
                + "'";
        ContentResolver cr = AndroidFactory.getApplicationContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] {
                    MediaStore.Images.Media._ID, MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
            }, whereClause, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                imageId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                int width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH));
                int hight = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT));
                setWidthInPixel(width);
                setHeightInPixel(hight);
            } else {
                MmsLog.w(TAG, "analysisAttribute(), cursor is null!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        if (imageId != -1) {
            whereClause = Thumbnails.IMAGE_ID + "='" + imageId + "'";
            try {
                cursor = cr.query(Thumbnails.EXTERNAL_CONTENT_URI, new String[] {
                    Thumbnails.DATA
                }, whereClause, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    String thumbnailPath = cursor.getString(cursor.getColumnIndex(Thumbnails.DATA));
                    if(thumbnailPath == null || thumbnailPath.equals(""))
                        thumbnailPath = thumbnail;
                    setThumbPath(thumbnailPath);
                } else {
                    MmsLog.w(TAG, "analysisAttribute(), cursor is null!");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }
        } else {
            if(thumbnail != null)
            setThumbPath(thumbnail);
            MmsLog.w(TAG, "analysisAttribute(), have no this image!");
        }
        */
    }

    public int getWidthInPixel() {
        return mWidthInPixel;
    }

    public void setWidthInPixel(int widthInPixel) {
        mWidthInPixel = widthInPixel;
    }

    public int getHeightInPixel() {
        return mHeightInPixel;
    }

    public void setHeightInPixel(int heightInPixel) {
        mHeightInPixel = heightInPixel;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getThumbPath() {
        return mThumbPath;
    }

    public void setThumbPath(String thumbPath) {
        mThumbPath = thumbPath;
    }
    public void setName(String name) {
        Log.d(TAG, "setName()");
        mFileName = name;
}

    public String getName() {
        Log.d(TAG, "getName()");
        return mFileName;
    }
}
