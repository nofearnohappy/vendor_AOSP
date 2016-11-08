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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.android.camera.v2.uimanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.camera.R;
import com.android.camera.v2.Thumbnail;
import com.android.camera.v2.ui.RotateImageView;
import com.android.camera.v2.uimanager.ThumbnailAnimation.AnimationListener;
import com.android.camera.v2.util.CameraUtil;
import com.android.camera.v2.util.Storage;

import java.io.File;

public class ThumbnailManager extends AbstractUiManager {
    private static final String                     TAG = "ThumbnailManager";

    private static final String                     ACTION_UPDATE_PICTURE
            = "com.android.gallery3d.action.UPDATE_PICTURE";
    private static final String                     ACTION_IPO_SHUTDOWN
            = "android.intent.action.ACTION_SHUTDOWN_IPO";

    private static final int                        MSG_SAVE_THUMBNAIL = 0;
    private static final int                        MSG_CHECK_THUMBNAIL = 1;

    private Activity                                mActivity;
    private ContentResolver                         mContentResolver;
    private RotateImageView                         mThumbnailView;
    private RotateImageView                         mPreviewThumb;
    private Handler                                 mMaiHandler;
    private ThumbnailCreatorHandler                 mHandler;
    private Thumbnail                               mThumbnail;
    private ThumbnailAnimation                      mThumbnailAnimation;
    private OnThumbnailClickListener                mOnThumbnailClickListener;
    private AsyncTask<Void, Void, Thumbnail>        mLoadThumbnailTask;
    private Intent                                  mIntent;
    private Uri                                     mUri;
    // mShownByIntent is used to judge thumbnail should show or not by intent
    // action.
    private boolean                                 mShownByIntent = true;
    private boolean                                 mResumed = false;
    private boolean mIsSecureCamera = false;
    private boolean mNeedShowSecureCamera = true;

    public interface OnThumbnailClickListener {
        public void onThumbnailClick();
    }


    IntentFilter mUpdatePictureFilter = new IntentFilter(ACTION_UPDATE_PICTURE);
    private BroadcastReceiver mUpdatePictureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "mDeletePictureReceiver.onReceive(" + intent + ")");
            // why need add :getContext().isActivityOnpause() ?
            // CS -> onPause()->entry gallery to delete the CS picture,
            // so this time ,isShowing() & isFullScreen() is true,
            // but this time we don't need update the Thumbnail
            if (isShowing()) {
                getLastThumbnailUncached();
            } else {
                //mUpdateThumbnailDelayed = true;
            }
        }
    };

    private IntentFilter mIpoShutdownFilter = new IntentFilter(ACTION_IPO_SHUTDOWN);
    private BroadcastReceiver mIpoShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "[onReceive]intent = " + intent);
            saveThumbnailToFile();
        }
    };

    public ThumbnailManager(Activity activity, ViewGroup parent, boolean secureCamera) {
        super(activity, parent);
        mIsSecureCamera = secureCamera;
        setFilterEnable(false);
        mActivity = activity;
        mContentResolver = activity.getContentResolver();
        mMaiHandler = new Handler(activity.getMainLooper());
        HandlerThread t = new HandlerThread("thumbnail-creation-thread");
        t.start();
        mHandler = new ThumbnailCreatorHandler(t.getLooper());
        mThumbnailAnimation = new ThumbnailAnimation();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(mActivity);
        manager.registerReceiver(mUpdatePictureReceiver, mUpdatePictureFilter);
        mActivity.registerReceiver(mIpoShutdownReceiver, mIpoShutdownFilter);

        mIntent = activity.getIntent();
        String action = null;
        if (mIntent != null) {
            action = mIntent.getAction();
        }
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)
                || CameraUtil.ACTION_STEREO3D.equals(action)) {
            mShownByIntent = false;
        }
    }

    @Override
    public void show() {
        Log.i(TAG, "[show], mShownByIntent:" + mShownByIntent);
        if (mShownByIntent) {
            super.show();
        }
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.thumbnail_v2);
        mThumbnailView = (RotateImageView) view.findViewById(R.id.thumbnail);
        mThumbnailView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnThumbnailClickListener != null) {
                    mOnThumbnailClickListener.onThumbnailClick();
                }
            }
        });
        mPreviewThumb = (RotateImageView) view.findViewById(R.id.preview_thumb);
        return view;
    }

    @Override
    protected void onRefresh() {
        Log.i(TAG, "[onRefresh]...");
        updateThumbnailView();
    }

    @Override
    public void setEnable(boolean enable) {
        super.setEnable(enable);
        if (mThumbnailView != null) {
            mThumbnailView.setEnabled(enable);
            mThumbnailView.setClickable(enable);
        }
    }

    public Uri getThumbnailUri() {
        if (mUri != null) {
            return mUri;
        } else {
            return Uri.parse("file://" + mThumbnail.getFilePath());
        }
    }

    public String getThumbnailMimeType() {
            return getMimeType(mThumbnail.getFilePath());
    }

    private String getMimeType(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String mime = "image/jpeg";
        if (filePath != null) {
            try {
                retriever.setDataSource(filePath);
                mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        Log.i(TAG, "[getMimeType] mime = " + mime);
        return mime;
    }

    public void setOnThumbnailClickListener(OnThumbnailClickListener listener) {
        mOnThumbnailClickListener = listener;
    }

    public void onCreate() {

    }

    public void onResume() {
        Log.i(TAG, "[onResume] mShownByIntent = " + mShownByIntent);
        mResumed = true;
        if (mShownByIntent) {
            mLoadThumbnailTask = new LoadThumbnailTask(false).execute();
        }
    }

    public void onPause() {
        Log.i(TAG, "[onPause]...");
        mResumed = false;
        mUri = null;
        cancelLoadThumbnail();
        saveThumbnailToFile();
    }

    public void onDestroy() {
        Log.i(TAG, "[onDestroy]...");
        if (mHandler != null) {
            mHandler.getLooper().quit();
        }
    }

    public void notifyFileSaved(Uri uri) {
        Log.i(TAG, "[notifyFileSaved], uri:" + uri + ", mShownByIntent:" + mShownByIntent);
        if (uri == null) {
            return;
        }
        mUri = uri;
        cancelLoadThumbnail();
        mHandler.sendEmptyMessage(MSG_SAVE_THUMBNAIL);
    }

    public void updateNeedShowThumbnail(boolean isNeedShow) {
        Log.i(TAG, "[updateNeedShowThumbnail] isNeedShow " + isNeedShow);
        mNeedShowSecureCamera = isNeedShow;
    }

    private void updateThumbnailView() {
        Log.i(TAG, "[updateThumbnailView]this = " + this);
        if (mThumbnailView != null) {
            if (isShowing()) {
                if (mThumbnail != null && mThumbnail.getBitmap() != null) {
                    // here set bitmap null to avoid show last thumbnail in a
                    // moment.
                    Log.i(TAG, "[updateThumbnailView]showing is true,set VISIBLE.");
                    mThumbnailView.setBitmap(null);
                    mThumbnailView.setBitmap(mThumbnail.getBitmap());
                    mThumbnailView.setVisibility(View.VISIBLE);
                } else {
                    Log.i(TAG, "[updateThumbnailView]showing is true," +
                            "but thumbnail is null,set INVISIBLE!");
                    mThumbnailView.setBitmap(null);
                    mThumbnailView.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    private Thumbnail getLastThumbnailFromContentResolver(ContentResolver resolver,
            Thumbnail thumbnail) {
        Thumbnail result[] = new Thumbnail[1];
        // Load the thumbnail from the media provider.
        int code = Thumbnail.getLastThumbnailFromContentResolver(
                resolver,
                result,
                thumbnail);
        Log.d(TAG, "getLastThumbnailFromContentResolver code = " + code);
        switch (code) {
        case Thumbnail.THUMBNAIL_FOUND:
            return result[0];
        case Thumbnail.THUMBNAIL_NOT_FOUND:
            return null;
        case Thumbnail.THUMBNAIL_DELETED:
            return null;
        default:
            return null;
        }
    }

    private void updateThumbnailViewWithAnimation() {
        if (mThumbnail != null && mPreviewThumb != null) {
            mPreviewThumb.setBitmap(null);
            mPreviewThumb.setBitmap(mThumbnail.getBitmap());

            mThumbnailAnimation.doCaptureAnimation(mPreviewThumb, mActivity,
                    new AnimationListener() {
                @Override
                public void onAnimationEnd() {
                    updateThumbnailView();
                }
            });
        }
    }

    private void saveThumbnailToFile() {
        Log.d(TAG, "[saveThumbnailToFile], mThumbnail:" + mThumbnail);
        if (mThumbnail != null && !mThumbnail.fromFile()) {
            Log.d(TAG, "[saveThumbnailToFile]execute...");
            new SaveThumbnailTask().execute(mThumbnail);
        }
    }


    private void getLastThumbnailUncached() {
        Log.d(TAG, "[getLastThumbnailUncached]...");
        cancelLoadThumbnail();
        mLoadThumbnailTask = new LoadThumbnailTask(false)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void cancelLoadThumbnail() {
        if (mLoadThumbnailTask != null) {
            Log.d(TAG, "[cancelLoadThumbnail]...");
            mLoadThumbnailTask.cancel(true);
            mLoadThumbnailTask = null;
        }
    }

    private class ThumbnailCreatorHandler extends Handler {
        public ThumbnailCreatorHandler(Looper looper) {
            super(looper);
            Log.i(TAG, "[ThumbnailCreatorHandler]new...");
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]ThumbnailCreatorHandler,msg:" + msg);
            long now = System.currentTimeMillis();
            switch (msg.what) {
            case MSG_SAVE_THUMBNAIL:
                mThumbnail = getLastThumbnailFromContentResolver(mContentResolver, mThumbnail);
                // may be receiver new media uri when camera is paused,
                // but it still need to save this
                // new thumbnail to file.
                if (!mResumed) {
                    saveThumbnailToFile();
                }
                if (mMaiHandler != null) {
                    mMaiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateThumbnailViewWithAnimation();
                        }
                    });
                }
                break;

            case MSG_CHECK_THUMBNAIL:

                break;

            default:
                break;
            }
        }
    }

    private class LoadThumbnailTask extends AsyncTask<Void, Void, Thumbnail> {
        private boolean mLookAtCache;
        public LoadThumbnailTask(boolean lookAtCache) {
            mLookAtCache = lookAtCache;
        }

        @Override
        protected Thumbnail doInBackground(Void... params) {
            Log.i(TAG, "[doInBackground]begin.mLookAtCache = " + mLookAtCache);
            // Load the thumbnail from the file.
            Thumbnail t = null;
            if (mLookAtCache) {
                t = Thumbnail.getLastThumbnailFromFile(mActivity.getFilesDir(), mContentResolver);
            }
            if (isCancelled()) {
                Log.w(TAG, "[doInBackground]task is cancel,return.");
                return null;
            }

            if (t == null && Storage.isStorageReady()) {
                t = getLastThumbnailFromContentResolver(mContentResolver, null);
            }
            return t;
        }

        @Override
        protected void onPostExecute(Thumbnail thumbnail) {
            Log.d(TAG, "[onPostExecute]isCancelled()="
                    + isCancelled()+ ",mIsSecureCamera = " + mIsSecureCamera +
                    ",mNeedShowSecureCamera = " + mNeedShowSecureCamera);
            if (isCancelled()) {
                return;
            }
            // in secure camera, if getContext().getMediaItemCount() <= 0,
            // there is no need to get thumbnail and should invisible thumbnail
            // view
            if (mIsSecureCamera && !mNeedShowSecureCamera) {
                mThumbnail = null;
            } else {
                mThumbnail = thumbnail;
            }
            updateThumbnailView();
        }
    }

    private class SaveThumbnailTask extends AsyncTask<Thumbnail, Void, Void> {
        @Override
        protected Void doInBackground(Thumbnail... params) {
            final int n = params.length;
            Log.d(TAG, "[doInBackground]length = " + n);
            final File filesDir = mActivity.getFilesDir();
            for (int i = 0; i < n; i++) {
                params[i].saveLastThumbnailToFile(filesDir);
            }
            return null;
        }
    }
}
