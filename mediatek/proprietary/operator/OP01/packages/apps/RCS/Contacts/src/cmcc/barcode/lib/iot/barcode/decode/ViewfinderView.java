/*
 * Copyright (C) 2008 ZXing authors
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

package cmcc.barcode.lib.iot.barcode.decode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.cmcc.omp.sdk.rest.qrcodec.decode.camera.CameraManager;

import com.mediatek.rcs.contacts.R;


/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 */
public final class ViewfinderView extends View {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;

    private final Paint mPaint;
    private final int mMaskColor;
    private final int mLaserColor;
    private int mScannerAlpha;

    /**
     * This constructor is used when the class is built from an XML resource.
     *@param context Context
     *@param attrs AttributeSet
     */
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        mPaint = new Paint();
        Resources resources = getResources();
        mMaskColor = resources.getColor(R.color.viewfinder_mask);
        mLaserColor = resources.getColor(R.color.viewfinder_laser);
        mScannerAlpha = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mPaint.setColor(mMaskColor);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);

        mPaint.setColor(mLaserColor);
        int length = (int) (Math.min(frame.height(), frame.width()) / 4.0f);

        canvas.drawRect(frame.left - 2, frame.top - 2,
                frame.left - 2 + length, frame.top, mPaint);
        canvas.drawRect(frame.left - 2, frame.top - 2,
                frame.left, frame.top - 2 + length, mPaint);
        canvas.drawRect(frame.right, frame.top - 2,
                frame.right + 2, frame.top - 2 + length, mPaint);
        canvas.drawRect(frame.right + 2 - length, frame.top - 2,
                frame.right + 2, frame.top, mPaint);
        canvas.drawRect(frame.left - 2, frame.bottom,
                frame.left - 2 + length, frame.bottom + 2, mPaint);
        canvas.drawRect(frame.left - 2, frame.bottom + 2 - length,
                frame.left, frame.bottom + 2, mPaint);
        canvas.drawRect(frame.right, frame.bottom + 2 - length,
                frame.right + 2, frame.bottom + 2, mPaint);
        canvas.drawRect(frame.right + 2 - length, frame.bottom,
                frame.right + 2, frame.bottom + 2, mPaint);

        // Draw a "laser scanner" line through the middle to show decoding is active
        mPaint.setColor(mLaserColor);
        mPaint.setAlpha(SCANNER_ALPHA[mScannerAlpha]);
        mScannerAlpha = (mScannerAlpha + 1) % SCANNER_ALPHA.length;

        int middleVertical = frame.height() / 2 + frame.top;
        int middleHorizontal = frame.width() / 2 + frame.left;
        int middle = (int) (Math.min(frame.height(), frame.width()) / 12.0f);
        int middleHalf = (int) (middle / 2.0f);
        canvas.drawRect(middleHorizontal - 2, middleVertical - middle - middleHalf,
                middleHorizontal + 2, middleVertical - middleHalf, mPaint);
        canvas.drawRect(middleHorizontal - 2, middleVertical + middleHalf,
                middleHorizontal + 2, middleVertical + middle + middleHalf, mPaint);
        canvas.drawRect(middleHorizontal - middle - middleHalf, middleVertical - 2,
                middleHorizontal - middleHalf, middleVertical + 2, mPaint);
        canvas.drawRect(middleHorizontal + middleHalf, middleVertical - 2,
                middleHorizontal + middle + middleHalf, middleVertical + 2, mPaint);

        // Request another update at the animation interval, but only repaint the laser line,
        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
                frame.right, frame.bottom);
    }

}
