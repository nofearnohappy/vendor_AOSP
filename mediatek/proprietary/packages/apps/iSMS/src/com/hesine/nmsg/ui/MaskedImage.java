package com.hesine.nmsg.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public abstract class MaskedImage extends ImageView {
    private static final Xfermode MASK_XFERMODE;
    private Bitmap mask;
    private Paint paint;

    static {
        PorterDuff.Mode localMode = PorterDuff.Mode.DST_IN;
        MASK_XFERMODE = new PorterDuffXfermode(localMode);
    }

    public MaskedImage(Context paramContext) {
        super(paramContext);
    }

    public MaskedImage(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    public MaskedImage(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    public abstract Bitmap createMask();

    protected void onDraw(Canvas paramCanvas) {
        Drawable localDrawable = getDrawable();
        if (localDrawable == null) {
            return;
        }
        if (this.paint == null) {
            this.paint = new Paint();
            this.paint.setFilterBitmap(false);
            this.paint.setXfermode(MASK_XFERMODE);
        }
        int i = paramCanvas.saveLayer(0.0F, 0.0F, getWidth(), getHeight(), null, 31);
        localDrawable.setBounds(0+getPaddingLeft(), 0+getPaddingTop(), 
                                getWidth()-getPaddingRight(), 
                                getHeight()-getPaddingBottom());
        localDrawable.draw(paramCanvas);
        if ((this.mask == null) || (this.mask.isRecycled())) {
            Bitmap localBitmap1 = createMask();
            this.mask = localBitmap1;
        }
        paramCanvas.drawBitmap(this.mask, 0.0F, 0.0F, this.paint);
        paramCanvas.restoreToCount(i);
        return;
    }
}
