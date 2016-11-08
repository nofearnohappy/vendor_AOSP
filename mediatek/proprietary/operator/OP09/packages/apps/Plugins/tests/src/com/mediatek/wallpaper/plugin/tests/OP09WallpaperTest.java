package com.mediatek.wallpaper.plugin.tests;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.Instrumentation;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.RemoteException;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.DisplayMetrics;
import android.util.Log;

import com.mediatek.op09.plugin.R;
import com.mediatek.wallpaper.plugin.WallpaperChooser;
import com.mediatek.wallpaper.plugin.WallpaperChooserDialogFragment;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wallpaper TestCase for OP09.
 */
public class OP09WallpaperTest
        extends ActivityInstrumentationTestCase2<WallpaperChooser> {

    private WallpaperChooser mActivity = null;
    private Instrumentation mInstrumentation = null;
    private Context mContext = null;
    private static final String TAG = "OP09WallpaperTest";

    private WallpaperManager mWm;

    /**
     * Constructs a new OP09WallpaperTest instance.
     */
    public OP09WallpaperTest() {
        super(WallpaperChooser.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mActivity = getActivity();

        mWm = (WallpaperManager) mContext.getSystemService(
                Context.WALLPAPER_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        mActivity = null;
        super.tearDown();
    }

    /**
     * TestCase: Launch WallpaperChooser.
     */
    @MediumTest
    public void testCase1LaunchWallpaperChooser() {
        assertNotNull(mActivity);
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);
    }

    /**
     * TestCase: Get default wallpaper.
     */
    @MediumTest
    public void testCase2GetDefaultWallpaper() {
        // WallpaperManager clear will trigger Setting default wallpaper.
        try {
            mWm.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = getCurrentWallpaperBitmap();
        Bitmap bitmap2 = loadBitmap(R.drawable.default_wallpaper); // same as the WallpaperManager

        // Assert the setted wallpaper equals the CT default wallpaper.
        assertTrue(isTwoBitmapsEqual(bitmap2, bitmap));
    }

    /**
     * TestCase: Set Wallpaper.
     */
    @MediumTest
    public void testCase3SetWallpaper() {
        Fragment fragment =
                mActivity.getFragmentManager().findFragmentById(R.id.wallpaper_chooser_fragment);
        if (fragment == null) {
            fragment = (DialogFragment) WallpaperChooserDialogFragment.newInstance();
        }

        if (fragment instanceof WallpaperChooserDialogFragment) {
            ((WallpaperChooserDialogFragment) fragment).onItemSelected(null, null, 1, 1);
            ((WallpaperChooserDialogFragment) fragment).onItemClick(null, null, 1, 1);
        } else {
            assertFalse(true);
        }

        // assert the setted wallpaper equals the 4th image.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = getCurrentWallpaperBitmap();
        Bitmap bitmap2 = loadBitmap(R.drawable.wallpaper_02);

        assertTrue(isTwoBitmapsEqual(bitmap, bitmap2));

    }

    // get current wallpaper image.
    private Bitmap getCurrentWallpaperBitmap() {
        Bitmap bitmap = mWm.getBitmap();
        return bitmap;
    }

    // load the bitmap and scale if necessary.
    private Bitmap loadBitmap(int resId) {
        InputStream is = mContext.getResources().openRawResource(resId);
        int width = 0;
        int height = 0;
        if (is != null) {
            try {
                width = mWm.getIWallpaperManager().getWidthHint();
                height = mWm.getIWallpaperManager().getHeightHint();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                // Enable PQ support for all static wallpaper bitmap decoding
                options.inPostProc = true;
                options.inPostProcFlag = 1;
                Bitmap bm = BitmapFactory.decodeStream(is, null, options);
                return generateBitmap(bm, width, height);
            } catch (OutOfMemoryError e) {
                Log.w("@M_" + TAG, "Can't decode stream", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // judge weather two images has the same content.
    private boolean isTwoBitmapsEqual(Bitmap bm1, Bitmap bm2) {
        if (bm1 == null || bm2 == null) {
            return false;
        }
        if (bm1.getWidth() != bm2.getWidth() || bm1.getHeight() != bm2.getHeight()) {
            return false;
        }
        for (int i = 0; i < bm1.getWidth(); i++) {
            for (int j = 0; j < bm1.getHeight(); j++) {
                if (bm1.getPixel(i, j) != bm2.getPixel(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    // The method is copied from WallpaperManager.
    private static Bitmap generateBitmap(Bitmap bm, int width, int height) {
        if (bm == null) {
            return null;
        }

        bm.setDensity(DisplayMetrics.DENSITY_DEVICE);

        if (width <= 0 || height <= 0
                || (bm.getWidth() == width && bm.getHeight() == height)) {
            return bm;
        }

        // This is the final bitmap we want to return.
        try {
            Bitmap newbm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            newbm.setDensity(DisplayMetrics.DENSITY_DEVICE);

            Canvas c = new Canvas(newbm);
            Rect targetRect = new Rect();
            targetRect.right = bm.getWidth();
            targetRect.bottom = bm.getHeight();

            int deltaw = width - targetRect.right;
            int deltah = height - targetRect.bottom;

            if (deltaw > 0 || deltah > 0) {
                // We need to scale up so it covers the entire area.
                float scale;
                if (deltaw > deltah) {
                    scale = width / (float) targetRect.right;
                } else {
                    scale = height / (float) targetRect.bottom;
                }
                targetRect.right = (int) (targetRect.right * scale);
                targetRect.bottom = (int) (targetRect.bottom * scale);
                deltaw = width - targetRect.right;
                deltah = height - targetRect.bottom;
            }

            targetRect.offset(deltaw / 2, deltah / 2);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            c.drawBitmap(bm, null, targetRect, paint);

            bm.recycle();
            c.setBitmap(null);
            return newbm;
        } catch (OutOfMemoryError e) {
            Log.w("@M_" + TAG, "Can't generate default bitmap", e);
            return bm;
        }
    }
}
