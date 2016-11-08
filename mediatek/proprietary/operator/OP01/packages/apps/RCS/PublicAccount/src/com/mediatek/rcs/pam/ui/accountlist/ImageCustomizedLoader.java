package com.mediatek.rcs.pam.ui.accountlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;
import android.widget.ImageView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.ui.FileDownloader;

import java.io.File;

public class ImageCustomizedLoader {
    private static final String TAG = Constants.TAG_PREFIX
            + "ImageCustomizedLoader";

    private static final int INVALID_RES_ID = -1;

    public static final int TYPE_IMAGE_CIRCLE = 0;
    public static final int TYPE_IMAGE_ORIGINAL = 1;

    private int mShapeType;
    private Context mContext;
    private int mTmpResId;

    public ImageCustomizedLoader(Context context) {
        this(context, INVALID_RES_ID, TYPE_IMAGE_ORIGINAL);
    }

    public ImageCustomizedLoader(Context context, int resId, int shapeType) {
        mContext = context;
        mTmpResId = resId;
        mShapeType = shapeType;
    }

    public void updateImage(ImageView imageView, String logoUrl) {
        updateImage(imageView, logoUrl, null);
    }

    public void updateImage(final ImageView imageView, String logoUrl,
            String logoPath) {
        Log.i(TAG, "updateImage(), logoUrl = " + logoUrl + ", logoPath = "
                + logoPath);
        if (logoPath == null) {
            if (mTmpResId != INVALID_RES_ID) {
                setImageFromRes(imageView, mTmpResId);
            }
            downloadAndUpdate(imageView, logoUrl);
        } else {
            final File file = new File(logoPath);
            if (!(file.exists() && setImageFromFile(imageView, logoPath))) {
                if (mTmpResId != INVALID_RES_ID) {
                    setImageFromRes(imageView, mTmpResId);
                }
                downloadAndUpdate(imageView, logoUrl);
            }
        }
    }

    private void downloadAndUpdate(final ImageView imageView,
            final String logoUrl) {
        FileDownloader.getInstance().sendDownloadRequest(logoUrl,
                Constants.MEDIA_TYPE_PICTURE, -1, 0,
                new FileDownloader.DownloadListener() {

                    @Override
                    public void reportDownloadResult(int resultCode,
                            final String path, long mediaId, long msgId,
                            int index) {
                        Log.i(TAG, "result code is " + resultCode
                                + ", path is " + path);

                        if (resultCode == ResultCode.SUCCESS
                                && imageView != null) {

                            imageView.post(new Runnable() {

                                @Override
                                public void run() {
                                    Log.i(TAG, "Update image after download");
                                    setImageFromFile(imageView, path);
                                }
                            });
                        }
                    }

                    @Override
                    public void reportDownloadProgress(long msgId, int index,
                            int percentage) {

                    }
                });
    }

    private void setImageFromRes(ImageView imageView, int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                resId);

        if (mShapeType == TYPE_IMAGE_CIRCLE) {
            imageView.setImageBitmap(getCroppedBitmap(bitmap));
            imageView.setBackground(getCroppedDraw(Color.GRAY));
        } else {
            imageView.setImageBitmap(bitmap);
        }
        imageView.setAdjustViewBounds(true);
    }

    private Boolean setImageFromFile(ImageView imageView, String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        if (bitmap == null) {
            Log.e(TAG, "File error " + path);
            return false;

        } else {
            if (mShapeType == TYPE_IMAGE_CIRCLE) {
                imageView.setImageBitmap(getCroppedBitmap(bitmap));
                imageView.setBackground(getCroppedDraw(Color.TRANSPARENT));
            } else {
                imageView.setImageBitmap(bitmap);
            }
            imageView.setAdjustViewBounds(true);
            return true;
        }
    }

    private Bitmap getCroppedBitmap(Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(new RectF(rect), paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private ShapeDrawable getCroppedDraw(int backColor) {
        ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
        mDrawable.getPaint().setColor(backColor);
        return mDrawable;
    }
}
