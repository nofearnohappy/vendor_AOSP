/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.wallpaper.plugin;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import com.mediatek.op09.plugin.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A custom DialogFragment that Choose Wallpaper.
 */
public class WallpaperChooserDialogFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {
    private static final String TAG = "WallpaperChooserDialogFragment";
    private static final String EMBEDDED_KEY = "WallpaperChooserDialogFragment.EMBEDDED_KEY";

    private static final boolean DEBUG = false;

    private boolean mEmbedded;
    private Bitmap mBitmap = null;

    private ArrayList<Integer> mThumbs;
    private ArrayList<Integer> mImages;
    private WallpaperLoader mLoader;
    private WallpaperDrawable mWallpaperDrawable = new WallpaperDrawable();

    /**
     * Returns a reference to a WallpaperChooserDialogFragment instance.
     * @return WallpaperChooserDialogFragment
     */
    public static WallpaperChooserDialogFragment newInstance() {
        WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
        fragment.setCancelable(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(EMBEDDED_KEY)) {
            mEmbedded = savedInstanceState.getBoolean(EMBEDDED_KEY);
        } else {
            mEmbedded = isInLayout();
        }
        if (DEBUG) {
            Log.d("@M_" + TAG, "onCreate: savedInstanceState = " + savedInstanceState
                    + ", mEmbedded = " + mEmbedded + ", this = " + this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EMBEDDED_KEY, mEmbedded);
        if (DEBUG) {
            Log.d("@M_" + TAG, "onSaveInstanceState: outState = " + outState + ", mEmbedded = "
                    + mEmbedded);
        }
    }

    /**
     * Attempts to cancel execution of this load task.
     */
    private void cancelLoader() {
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel(true);
            mLoader = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG) {
            Log.d("@M_" + TAG, "onDetach.");
        }
        cancelLoader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d("@M_" + TAG, "onDestroy: mLoader = " + mLoader + ", this = " + this);
        }
        cancelLoader();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d("@M_" + TAG, "onCreateView: mEmbedded = " + mEmbedded + ", container = "
                    + container);
        }
        findWallpapers();

        /*
         * If this fragment is embedded in the layout of this activity, then we
         * should generate a view to display. Otherwise, a dialog will be
         * created in onCreateDialog()
         */
        if (mEmbedded) {
            View view = inflater.inflate(R.layout.wallpaper_chooser, container, false);
            view.setBackground(mWallpaperDrawable);

            final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
            gallery.setCallbackDuringFling(false);
            gallery.setOnItemSelectedListener(this);
            gallery.setAdapter(new ImageAdapter(getActivity()));

            View setButton = view.findViewById(R.id.set);
            setButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectWallpaper(gallery.getSelectedItemPosition());
                }
            });
            return view;
        }
        return null;
    }

    /**
     * Set selected Wallpaper.
     * @param position the position of the currently selected item within the adapter's data set
     */
    private void selectWallpaper(int position) {
        if (DEBUG) {
            Log.d("@M_" + TAG, "selectWallpaper: position = " + position + ", this = " + this);
        }
        try {
            WallpaperManager wpm = (WallpaperManager) getActivity().getSystemService(
                    Context.WALLPAPER_SERVICE);
            wpm.setResource(mImages.get(position));
            Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        } catch (IOException e) {
            if (DEBUG) {
                Log.e("@M_" + TAG, "Failed to set wallpaper: " + e);
            }
        }
    }

    // Click handler for the Dialog's GridView
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectWallpaper(position);
    }

    // Selection handler for the embedded Gallery view
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel();
        }
        mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Find and add default wallpapers.
     */
    private void findWallpapers() {
        mThumbs = new ArrayList<Integer>(24);
        mImages = new ArrayList<Integer>(24);

        final Resources resources = getResources();
        // Context.getPackageName() may return the "original" package name,
        // com.android.launcher2; Resources needs the real package name,
        // com.android.launcher. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = resources.getResourcePackageName(R.array.wallpapers);

        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    /**
     * Find and add wallpapers from Resources.
     * @param resources accessing an application's resources
     * @param packageName the package name of the resource
     * @param list The desired resource identifier
     */
    private void addWallpapers(Resources resources, String packageName, int list) {
        final String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                final int thumbRes = resources.getIdentifier(extra + "_small",
                        "drawable", packageName);

                if (thumbRes != 0) {
                    mThumbs.add(thumbRes);
                    mImages.add(res);
                    // Log.d("@M_" + TAG, "add: [" + packageName + "]: " + extra + " ("
                    // + res + ")");
                }
            }
        }
    }

    /**
     * Custom ImageAdapter for Gallery.
     */
    private class ImageAdapter extends BaseAdapter implements ListAdapter, SpinnerAdapter {
        private LayoutInflater mLayoutInflater;

        /**
         * Constructs a new ImageAdapter instance.
         * @param activity the Activity this fragment is currently associated with.
         */
        ImageAdapter(Activity activity) {
            mLayoutInflater = activity.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mThumbs.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                view = convertView;
            }

            ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);

            int thumbRes = mThumbs.get(position);
            image.setImageResource(thumbRes);
            Drawable thumbDrawable = image.getDrawable();
            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                if (DEBUG) {
                    Log.e("@M_" + TAG, "Error decoding thumbnail resId=" + thumbRes + " for wallpaper #"
                            + position);
                }
            }

            return view;
        }
    }

    /**
     * Wallpaper Load Task.
     */
    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions;

        /**
         * Constructs a new WallpaperLoader instance.
         */
        WallpaperLoader() {
            mOptions = new BitmapFactory.Options();
            mOptions.inDither = false;
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled() || !isAdded()) {
                // If the fragment is not added(attached) to an activity, return
                // null.
                if (DEBUG) {
                    Log.d("@M_" + TAG, "WallpaperLoader doInBackground: canceled = " + isCancelled()
                            + ",isAdded() = " + isAdded() + ",activity = " + getActivity());
                }
                return null;
            }
            try {
                return BitmapFactory.decodeResource(getResources(),
                        mImages.get(params[0]), mOptions);
            } catch (OutOfMemoryError e) {
                if (DEBUG) {
                    Log.e("@M_" + TAG, "WallpaperLoader decode resource out of memory " + e.getMessage());
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b == null) {
                return;
            }

            if (!isCancelled() && !mOptions.mCancel) {
                // Help the GC
                if (mBitmap != null) {
                    mBitmap.recycle();
                }

                View v = getView();
                if (v != null) {
                    mBitmap = b;
                    mWallpaperDrawable.setBitmap(b);
                    v.postInvalidate();
                } else {
                    mBitmap = null;
                    mWallpaperDrawable.setBitmap(null);
                }
                mLoader = null;
            } else {
                b.recycle();
            }
        }

        /**
         * Attempts to cancel execution of this WallpaperLoader task.
         */
        void cancel() {
            mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }

    /**
     * Custom drawable that centers the bitmap fed to it.
     */
    static class WallpaperDrawable extends Drawable {

        Bitmap mBitmap;
        int mIntrinsicWidth;
        int mIntrinsicHeight;

        /**
         * Set Bitmap to Drawable.
         * @param bitmap Bitmap object
         */
        void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            if (mBitmap == null) {
                return;
            }
            mIntrinsicWidth = mBitmap.getWidth();
            mIntrinsicHeight = mBitmap.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            if (mBitmap == null) {
                return;
            }

            int x = -1;
            int y = -1;
            final int width = canvas.getWidth();
            final int height = canvas.getHeight();

            // scale if width and height is bigger than bitmap's size.
            final float scalew = width / (float) mIntrinsicWidth;
            final float scaleh = height / (float) mIntrinsicHeight;
            final float scale = Math.max(scalew, scaleh);

            final int scaledWidth = (int) (mIntrinsicWidth * scale);
            final int scaledHeight = (int) (mIntrinsicHeight * scale);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmap,
                    scaledWidth, scaledHeight, true);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
                    Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawBitmap(scaledBitmap, x, y, null);
            scaledBitmap.recycle();
            scaledBitmap = null;
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }

        @Override
        public void setAlpha(int alpha) {
            // Ignore
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // Ignore
        }
    }
}
