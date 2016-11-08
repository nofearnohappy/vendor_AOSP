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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.android.mms.ContentType;
import com.mediatek.op09.plugin.R;

/**
 * Plugin class of OP09 Mms UnsupportedFiles class.
 */
public class Op09MmsUnSupportedFilesExt {

    private static final String TAG = "Mms/Op09MmsUnSupportedFilesExt";
    private static final int TEXT_ID = 111;

    private Context mContext = null;
    private Uri mAudioUri = null;
    private TextView mUnsupportedTextView = null;
    private TextView mUnsupportedTextViewForImage = null;
    private TextView mUnsupportedTextViewForAudio = null;
    private TextView mUnsupportedTextViewForVideo = null;

    private static Op09MmsUnSupportedFilesExt sOp09MmsUnSupportedFilesExt;

    public static Op09MmsUnSupportedFilesExt getIntance(Context context) {
        if (sOp09MmsUnSupportedFilesExt == null) {
            sOp09MmsUnSupportedFilesExt = new Op09MmsUnSupportedFilesExt(context);
        }
        return sOp09MmsUnSupportedFilesExt;
    }

    /**
     * The type of view.
     */
    private static enum ViewType {
        Image, Video, Audio
    };

    /**
     * The Constructor.
     * @param base the Context.
     */
    public Op09MmsUnSupportedFilesExt(Context base) {
        mContext = base;
    }


    public boolean isSupportedFile(String contentType, String fileName) {
        boolean isSupport = MmsContentType.isSupportedType(contentType);
        if (!isSupport && !TextUtils.isEmpty(fileName)) {
            isSupport = MmsContentType.isSupportedType(getContentType(contentType, fileName));
        }
        Log.d(TAG, "[isSupprtedFile],contentType = " + contentType + ", fileName = " + fileName
            + "; support = " + isSupport);
        return isSupport;
    }

    private Resources getResources() {
        return mContext.getResources();
    }
    public Bitmap getUnsupportedAudioIcon() {
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.media_error);
    }


    public Bitmap getUnsupportedImageIcon() {
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.media_error);
    }


    public Bitmap getUnsupportedVideoIcon() {
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.media_error);
    }

    /**
     * M: judge the audio is supported or not.
     * @return true: support; false: not supportd.
     */
    private boolean isSupportedAudio() {
        if (mAudioUri == null) {
            return true;
        }
        Cursor cr = mContext.getContentResolver().query(mAudioUri, new String[] {"ct", "cl"}, null,
            null, null);
        if (cr != null) {
            try {
                cr.moveToFirst();
                String ct = cr.getString(0);
                String cl = cr.getString(1);
                boolean supported = isSupportedFile(ct, cl);
                Log.d(TAG, "[setAudioUnsupportedIcon]: end;  ct:" + ct + "\tcl:" + cl
                    + " supported:" + supported);
                return supported;
            } catch (SQLiteException e) {
                Log.e(TAG, "setAudioUnsupportedIcon(LinearLayout): failed; " + e.getMessage());
                return true;
            } finally {
                if (cr != null) {
                    cr.close();
                }
            }
        }
        return true;
    }


    public void setAudioUnsupportedIcon(LinearLayout audioView) {
        Log.d(TAG, "[setAudioUnsupportedIcon]: start");
        if (mContext == null || audioView == null || isSupportedAudio()) {
            return;
        }

        Log.d(TAG, "setAudioUnsupportedIcon(LinearLayout): end sucess");
        ImageView iv = (ImageView) audioView.getChildAt(0);
        iv.setImageBitmap(getUnsupportedAudioIcon());
    }


    public void setAudioUnsupportedIcon(ImageView audioView, String contentType, String fileName) {
        Log.d(TAG, "setAudioUnsupportedIcon: start");
        if (audioView == null || TextUtils.isEmpty(contentType)
            || isSupportedFile(contentType, fileName)) {
            return;
        }
        Log.d(TAG, "setAudioUnsupportedIcon: end sucess");
        audioView.setImageBitmap(getUnsupportedAudioIcon());
    }


    public void setAudioUri(Uri uri) {
        Log.d(TAG, "setAudioUri:" + uri);
        this.mAudioUri = uri;
    }


    public void setImageUnsupportedIcon(ImageView imageView, String contentType, String fileName) {
        Log.d(TAG, "OP09,setImageUnsupportedIcon: start");
        if (imageView == null || TextUtils.isEmpty(contentType)
            || isSupportedFile(contentType, fileName)) {
            return;
        }
        Log.d(TAG, "OP09,setImageUnsupportedIcon: end success");
        imageView.setImageBitmap(getUnsupportedImageIcon());
    }


    public void setVideoUnsupportedIcon(ImageView videoView, String contentType, String fileName) {
        Log.d(TAG, "setVideoUnsupportedIcon:START");
        if (videoView == null || TextUtils.isEmpty(contentType)
            || isSupportedFile(contentType, fileName)) {
            return;
        }
        Log.d(TAG, "setVideoUnsupportedIcon:END  Sucess");
        videoView.setImageBitmap(getUnsupportedVideoIcon());
    }

    /**
     * M: init info view.
     * @param viewGroup the viewGroup.
     * @param viewType the view's info type.
     */
    private void initUnsupportedView(ViewGroup viewGroup, ViewType viewType) {
        Log.d(TAG, "[initUnsupportedView] for " + viewType);
        if (viewGroup == null) {
            Log.d(TAG, "[initUnsupportedView] for " + viewType + " failed. viewGroup == null");
            return;
        }
        TextView textView = null;
        switch (viewType) {
            case Image:
                textView = mUnsupportedTextViewForImage;
                break;
            case Audio:
                textView = mUnsupportedTextViewForAudio;
                break;
            case Video:
                textView = mUnsupportedTextViewForVideo;
                break;
            default:
                break;
        }
        if (textView != null) {
            viewGroup.removeView(textView);
        }
        textView = createTextView();
        viewGroup.addView(textView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT));
        textView.setVisibility(View.GONE);
        Log.d(TAG, "[initUnsupportedView] for " + viewType + " success");
        switch (viewType) {
            case Image:
                mUnsupportedTextViewForImage = textView;
                break;
            case Audio:
                mUnsupportedTextViewForAudio = textView;
                break;
            case Video:
                mUnsupportedTextViewForVideo = textView;
                break;
            default:
                break;
        }
    }

    /**
     * M: Create text view.
     * @return the text view.
     */
    private TextView createTextView() {
        TextView textView = new TextView(mContext);
        SharedPreferences sp = mContext.getSharedPreferences("com.android.mms_preferences",
            Context.MODE_WORLD_READABLE);
        float textSize = sp.getFloat("message_font_size", 18);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.RED);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        textView.setText(this.getResources().getString(R.string.unsupported_files));
        textView.setId(TEXT_ID);
        return textView;
    }

    /**
     * M: set view visibility.
     * @param textView the textView.
     * @param show true: show; false: hide.
     */
    private void setUnsupportedViewVisibility(TextView textView, boolean show) {
        if (textView == null) {
            Log.d(TAG, "[setUnsupportedViewVisibility] failed, textView == null");
            return;
        }
        textView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    public void initUnsupportedViewForAudio(ViewGroup viewGroup) {
        Log.d(TAG, "initUnsupportedViewForAudio");
        initUnsupportedView(viewGroup, ViewType.Audio);
    }


    public void initUnsupportedViewForImage(ViewGroup viewGroup) {
        Log.d(TAG, "initUnsupportedViewForImage");
        initUnsupportedView(viewGroup, ViewType.Image);
        Log.d(TAG, "initUnsupportedViewForImage END: view = " + mUnsupportedTextViewForImage);
    }


    public void setUnsupportedViewVisibilityForAudio(boolean show) {
        Log.d(TAG, "[setUnsupportedViewVisibilityForAudio]:" + show);
        if (isSupportedAudio()) {
            Log.d(TAG, "[setUnsupportedViewVisibilityForAudio] failed, is supported");
            return;
        }
        setUnsupportedViewVisibility(mUnsupportedTextViewForAudio, show);
    }


    public void setUnsupportedViewVisibilityForImage(boolean show) {
        Log.d(TAG, "[setUnsupportedViewVisibilityForImage]:" + show);
        setUnsupportedViewVisibility(mUnsupportedTextViewForImage, show);
    }


    public void setUnsupportedMsg(LinearLayout linearLayout, View childView, boolean show) {
        if (linearLayout == null || childView == null) {
            Log.d(TAG, "set unsupported msg failed. layout is null");
            return;
        }
        int vIndex = linearLayout.indexOfChild(childView);
        if (vIndex < 0) {
            Log.d(TAG, "set unsupported msg fialed. show:" + show
                + " \t the childView is no exist. index:" + vIndex);
            return;
        }
        vIndex += 1;
        View msgInfo = linearLayout.getChildAt(vIndex);
        if (msgInfo != null && show && msgInfo.getId() == TEXT_ID) {
            msgInfo.setVisibility(View.VISIBLE);
            Log.d(TAG, "show unsupported view");
            return;
        } else if (msgInfo != null && !show && msgInfo.getId() == TEXT_ID) {
            linearLayout.removeView(msgInfo);
            Log.d(TAG, "remove unsupported view");
            return;
        } else if (!show) {
            Log.d(TAG, "just return no action");
            return;
        }

        Log.d(TAG, "add new unsupported view, index:" + vIndex);
        linearLayout.addView(createTextView(), vIndex, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    /**
     * M: get Content TYpe by fileName.
     * @param contentType the content type.
     * @param fileName the file name.
     * @return the content type.
     */
    private String getContentType(String contentType, String fileName) {
        Log.d(TAG, "[getContentType], cotnentType:" + contentType + " fileName:" + fileName);
        String finalContentType = "";
        if (contentType == null) {
            return contentType;
        }
        if (contentType.equalsIgnoreCase("application/oct-stream")
            || contentType.equalsIgnoreCase("application/octet-stream")) {
            if (fileName != null) {
                String suffix = fileName.contains(".") ? fileName.substring(fileName
                        .lastIndexOf("."), fileName.length()) : "";
                if (suffix.equals("")) {
                    return contentType;
                } else if (suffix.equalsIgnoreCase(".bmp")) {
                    finalContentType = MmsContentType.IMAGE_BMP;
                } else if (suffix.equalsIgnoreCase(".jpg")) {
                    finalContentType = ContentType.IMAGE_JPG;
                } else if (suffix.equalsIgnoreCase(".wbmp")) {
                    finalContentType = ContentType.IMAGE_WBMP;
                } else if (suffix.equalsIgnoreCase(".gif")) {
                    finalContentType = ContentType.IMAGE_GIF;
                } else if (suffix.equalsIgnoreCase(".png")) {
                    finalContentType = ContentType.IMAGE_PNG;
                } else if (suffix.equalsIgnoreCase(".jpeg")) {
                    finalContentType = ContentType.IMAGE_JPEG;
                } else if (suffix.equalsIgnoreCase(".vcs")) {
                    finalContentType = ContentType.TEXT_VCALENDAR;
                } else if (suffix.equalsIgnoreCase(".vcf")) {
                    finalContentType = ContentType.TEXT_VCARD;
                } else if (suffix.equalsIgnoreCase(".imy")) {
                    finalContentType = ContentType.AUDIO_IMELODY;
                    // M: fix bug ALPS00355917
                } else if (suffix.equalsIgnoreCase(".ogg")) {
                    finalContentType = ContentType.AUDIO_OGG;
                } else if (suffix.equalsIgnoreCase(".aac")) {
                    finalContentType = ContentType.AUDIO_AAC;
                } else if (suffix.equalsIgnoreCase(".mp2")) {
                    finalContentType = ContentType.AUDIO_MPEG;
                    /// M: 3gp audio contentType will be modified
                    /// when CMCC send to CU
                } else if (suffix.equalsIgnoreCase(".3gp")) {
                    finalContentType = ContentType.AUDIO_3GPP;
                } else {
                    String extension = fileName.contains(".") ? fileName.substring(fileName
                            .lastIndexOf(".") + 1, fileName.length()) : "";
                    finalContentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        extension);
                    if (finalContentType == null) {
                        return contentType;
                    }
                }
                return finalContentType;
            }
        }
        return contentType;
    }
}
