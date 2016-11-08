/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.incallui.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView {

    public static final float NO_RATIO = 0.0f;
    private float mAspectRatio = NO_RATIO;
    private boolean mSurfaceCreated = false;
    private SurfaceHolder mHolder;
    public int mWidth = -1;
    public boolean isReceiver = false;

    public VideoSurfaceView(Context context) {
        super(context);
        init();
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoSurfaceView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio((float) width / (float) height);
    }

    public void setAspectRatio(float ratio) {
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
            invalidate();
        }
    }

    protected void onMeasure(int widthMeasureSpec,
            int heightMeasureSpec) {
        if (mAspectRatio != NO_RATIO) {
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
            int width = widthSpecSize;
            int height = heightSpecSize;
            if (width > 0 && height > 0) {
                float defaultRatio = ((float) width)
                        / ((float) height);
                if (defaultRatio < mAspectRatio) {
                    height = (int) (width / mAspectRatio);
                } else if (defaultRatio > mAspectRatio) {
                    width = (int) (height * mAspectRatio);
                }
                width = Math.min(width, widthSpecSize);
                height = Math.min(height, heightSpecSize);
                if (mWidth != -1) {
                    setMeasuredDimension(mWidth, height);
                } else {
                    setMeasuredDimension(width, height);
                }
                return;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setImage(Bitmap bmp) {
        if (mSurfaceCreated) {
            Canvas canvas = null;
            try {
                synchronized (mHolder) {
                    canvas = mHolder.lockCanvas();
                }
            } finally {
                if (canvas != null) {
                    canvas.drawARGB(255, 0, 0, 0);
                    canvas.drawBitmap(bmp, null,
                            canvas.getClipBounds(), null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    public void clearImage() {
        if (mSurfaceCreated) {
            Canvas canvas = null;
            try {
                synchronized (mHolder) {
                    canvas = mHolder.lockCanvas();
                }
            } finally {
                if (canvas != null) {
                    canvas.drawARGB(255, 0, 0, 0);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void init() {
        mHolder = this.getHolder();
        mHolder.addCallback(mSurfaceCallback);
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format,
                int w, int h) {
        }
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceCreated = true;
            Log.d("surfaceCreated", " surfaceCreated commented Entry isReceiver" + isReceiver);
           if (isReceiver) {
               clearImage();
           }
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("surfaceCreated", " surfaceDestroyed Entry");
            mSurfaceCreated = false;
        }
    };
}
