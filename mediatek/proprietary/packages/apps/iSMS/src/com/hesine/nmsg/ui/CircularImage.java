package com.hesine.nmsg.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class CircularImage extends MaskedImage {
    public CircularImage(Context paramContext) {
        super(paramContext);
    }

    public CircularImage(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    public CircularImage(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    public Bitmap createMask() {
        Bitmap.Config localConfig = Bitmap.Config.ARGB_8888;
        Bitmap localBitmap = Bitmap.createBitmap(getWidth(), getHeight(), localConfig);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(1);
        paint.setColor(-16777216);
        RectF localRectF = new RectF(0.0F, 0.0F, getWidth(), getHeight());
        canvas.drawOval(localRectF, paint);
        return localBitmap;
    }
}
