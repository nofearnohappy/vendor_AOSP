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

package com.mediatek.rcs.incallui.icon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.widget.ImageView;

public class CircleImageView extends ImageView {
    private static final Xfermode MASK_XFERMODE;
    private Bitmap mMask;
    private Paint  mPaint;
    private int mBorderWidth = 5;
    private int mBorderColor = Color.parseColor("#f2f2f2");

    static {
        PorterDuff.Mode localMode = PorterDuff.Mode.DST_IN;
        MASK_XFERMODE = new PorterDuffXfermode(localMode);
    }

    public CircleImageView(Context paramContext) {
        super(paramContext);
    }

    private boolean mUseDefaultStyle = false;

    public void setUseDefaultStyle(boolean useDefaultStyle) {
        this.mUseDefaultStyle = useDefaultStyle;
    }

    @Override
    protected void onDraw(Canvas paramCanvas) {
        if (mUseDefaultStyle) {
            super.onDraw(paramCanvas);
            return;
        }

        final Drawable localDrawable = getDrawable();
        if (localDrawable == null) {
            return;
        }

        if (localDrawable instanceof NinePatchDrawable) {
            return;
        }

        if (this.mPaint == null) {
            final Paint localPaint = new Paint();
            localPaint.setFilterBitmap(false);
            localPaint.setAntiAlias(true);
            localPaint.setXfermode(MASK_XFERMODE);
            this.mPaint = localPaint;
        }

        final int width = getWidth();
        final int height = getHeight();
        int layer = paramCanvas.saveLayer(0.0F, 0.0F, width, height, null, 31);
        localDrawable.setBounds(0, 0, width, height);
        localDrawable.draw(paramCanvas);

        if ((this.mMask == null) || (this.mMask.isRecycled())) {
            this.mMask = createCircleBitmap(width, height);
        }

        paramCanvas.drawBitmap(this.mMask, 0.0F, 0.0F, this.mPaint);
        paramCanvas.restoreToCount(layer);
        drawBorder(paramCanvas, width, height);
    }

    private void drawBorder(Canvas canvas, final int width, final int height) {
        if (mBorderWidth == 0) {
            return;
        }

        final Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(mBorderColor);
        borderPaint.setStrokeWidth(mBorderWidth);
        canvas.drawCircle(width >> 1, height >> 1, (width - mBorderWidth) >> 1, borderPaint);
    }

    public Bitmap createCircleBitmap(final int width, final int height) {
        Bitmap.Config localConfig = Bitmap.Config.ARGB_8888;
        Bitmap localBitmap = Bitmap.createBitmap(width, height, localConfig);
        Canvas localCanvas = new Canvas(localBitmap);
        Paint localPaint = new Paint();
        final int padding = mBorderWidth - 3;
        RectF localRectF = new RectF(padding, padding, width - padding, height - padding);
        localCanvas.drawOval(localRectF, localPaint);
        return localBitmap;
    }
}