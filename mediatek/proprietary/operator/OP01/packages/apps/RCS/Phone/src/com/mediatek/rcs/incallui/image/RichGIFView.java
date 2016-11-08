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

package com.mediatek.rcs.incallui.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.mediatek.rcs.incallui.image.AsyncPhotoManager.ViewItem;

public class RichGIFView extends ImageView {
    private static final String TAG = "RichGIFView";
    private static final int MESSAGE_TYPE_RES     = 1;
    private static final int MESSAGE_TYPE_UPDATE  = 2;
    private static final int MESSAGE_TYPE_CONTROL = 3;
    private static final int MESSAGE_TYPE_QUIT    = 4;

    private WorkHandler     mWorkHandler;
    private HandlerThread   mHandlerThread;
    private ViewItem        mViewItem;
    private Rect            mRect;

    public RichGIFView(Context cnx) {
        super(cnx);
        init();
    }

    //Using to update the bitmap of this view
    private Handler mRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.d(TAG, "mRefreshHandler, type = " + msg.what);
            switch(msg.what) {
                case MESSAGE_TYPE_UPDATE:
                    invalidate();
                    break;
                default:
                    break;
            }
        }
    };

    private void init() {
        mHandlerThread = new HandlerThread("RichGIFView");
        mHandlerThread.start();
        mWorkHandler = new WorkHandler(mHandlerThread.getLooper());
    }

    //Use this handler to control the show time of next frame, and also
    //control the gif pasue and run
    private class WorkHandler extends Handler {
        private Looper   mLooper;
        private ViewItem mViewItem;
        private boolean  mPause;
        WorkHandler(Looper looper) {
            super(looper);
            mLooper = looper;
        }

        @Override
        public void handleMessage(Message msg) {
            //Log.d(TAG, "WorkHandler, handleMessage(), msg = " + msg);
            switch(msg.what) {
                case MESSAGE_TYPE_RES:
                    this.initResource((ViewItem) msg.obj);
                    break;
                case MESSAGE_TYPE_UPDATE:
                    this.updatedResource();
                    break;
                case MESSAGE_TYPE_CONTROL:
                    this.setPauseStatus();
                    break;
                case MESSAGE_TYPE_QUIT:
                    this.quitWorkHandler();
                    break;
                default:
                    Log.d(TAG, "handleMessage, unknown type!");
                    break;
            }
        }

        private void initResource(ViewItem item) {
            mViewItem = item;
            if (mViewItem != null) {
                mPause = false;
                int internal = mViewItem.getNextInternal();
                SystemClock.sleep(internal);
                this.sendMessage(this.obtainMessage(MESSAGE_TYPE_UPDATE));
            }
        }

        private void updatedResource() {
            if (mViewItem != null && mPause == false) {
                //Get the internal the next pic show
                int internal = mViewItem.getNextInternal();
                Message message = mRefreshHandler.obtainMessage(MESSAGE_TYPE_UPDATE);
                mRefreshHandler.sendMessage(message);
                SystemClock.sleep(internal);
                this.sendMessage(this.obtainMessage(MESSAGE_TYPE_UPDATE));
            }
        }

        private void setPauseStatus() {
            mPause = true;
        }

        private void quitWorkHandler() {
            if (mLooper != null) {
                mLooper.quitSafely();
            }
        }

    }

    public void setViewItem(ViewItem item) {
        Log.d(TAG, "setViewItem");
        mViewItem = item;
        mViewItem.resetCurentId();
        showCover();

        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MESSAGE_TYPE_UPDATE);
        }

        if (mRefreshHandler != null) {
            mRefreshHandler.removeMessages(MESSAGE_TYPE_UPDATE);
        }

        Message msg = mWorkHandler.obtainMessage(MESSAGE_TYPE_RES);
        msg.obj = mViewItem;
        mWorkHandler.sendMessage(msg);
    }

    public void pauseGif() {
        removeMessages();
        if (mWorkHandler != null) {
            Message msg = mWorkHandler.obtainMessage(MESSAGE_TYPE_CONTROL);
            mWorkHandler.sendMessage(msg);
        }
    }

    public void releaseResource() {
        removeMessages();

        if (mWorkHandler != null) {
            Message msg = mWorkHandler.obtainMessage(MESSAGE_TYPE_QUIT);
            mWorkHandler.sendMessage(msg);
            mWorkHandler = null;
        }

        mViewItem = null;
    }

    private void removeMessages() {
        if (mRefreshHandler != null) {
            mRefreshHandler.removeMessages(MESSAGE_TYPE_UPDATE);
        }

        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MESSAGE_TYPE_RES);
            mWorkHandler.removeMessages(MESSAGE_TYPE_UPDATE);
            mWorkHandler.removeMessages(MESSAGE_TYPE_CONTROL);
        }
    }

    public void setRect(int height, int width) {
        if (height > 0 && width > 0) {
            mRect = new Rect();
            mRect.left = 0;
            mRect.top = 0;
            mRect.right = width;
            mRect.bottom = height;
        }
    }

    private void showCover() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cns) {
        super.onDraw(cns);

        if (mViewItem == null) {
            return;
        }

        Bitmap bitmap = mViewItem.getNextBitmap();
        if (bitmap == null) {
            return;
        }

        int count = cns.getSaveCount();
        cns.save();
        cns.translate(getPaddingLeft(), getPaddingTop());
        cns.drawBitmap(bitmap, null, mRect, null);

        cns.restoreToCount(count);
    }
}
