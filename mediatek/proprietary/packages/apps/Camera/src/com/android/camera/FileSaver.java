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
package com.android.camera;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;

import com.android.camera.Util.ImageFileNamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileSaver {
    private static final String TAG = "FileSaver";

    private static final int QUEUE_LIMIT = 100;
    private static final String TEMP_SUFFIX = ".tmp";

    private CameraActivity mContext;
    private ContentResolver mContentResolver;
    private List<FileSaverListener> mSaverListenerList = new CopyOnWriteArrayList<FileSaverListener>();
    private HashMap<Integer, ImageFileNamer> mFileNamer;
    private FileSaverService mSaverService;
    private Object mSaveServiceObject = new Object();

    public interface FileSaverListener {
        void onFileSaved(SaveRequest request);
    }

    public FileSaver(CameraActivity cameraActivity) {
        Log.i(TAG, "[FileSaver]constructor... ");
        mContext = cameraActivity;
        mContentResolver = mContext.getContentResolver();
    }

    public void bindSaverService() {
        Log.i(TAG, "[bindSaverService]...");
        Intent intent = new Intent((Context) mContext, FileSaverService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBindSaverService() {
        Log.i(TAG, "[unBindSaverService]...");
        synchronized (mSaveServiceObject) {
            if (mSaverService != null) {
                mSaverService = null;
            }
        }
        if (mConnection != null) {
            mContext.unbindService(mConnection);
        }
    }

    public void waitDone() {
        Log.i(TAG, "[waitDone]...");
        if (mSaverService == null) {
            Log.e(TAG, "[waitDone]mSaverService is null,return.");
            return;
        }
        synchronized (FileSaver.this) {
            while (mSaverService != null && !mSaverService.isNoneSaveTask()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Log.e(TAG, "[waitDone]exception :", ex);
                }
            }
        }
        Log.i(TAG, "[waitDone]");
    }

    public long getWaitingDataSize() {
        Log.d(TAG, "[getWaitingDataSize]...");
        return mSaverService.getWaitingDataSize();
    }

    public int getWaitingCount() {
        Log.d(TAG, "[getWaitingCount]...");
        if (mSaverService == null) {
            Log.e(TAG, "[getWaitingCount]mSaverService is null,return!");
            return -1;
        }
        return mSaverService.getWaitingCount();
    }

    // record type, location and datetaken
    public SaveRequest preparePhotoRequest(int fileType, int pictureType) {
        Log.i(TAG, "[preparePhotoRequest]fileType =" + fileType + ",pictureType = " + pictureType);
        SaveRequest request = null;
        if (fileType == Storage.FILE_TYPE_PANO) {
            request = new PanoOperator(pictureType);
        } else {
            request = new PhotoOperator(pictureType);
        }
        request.prepareRequest();
        mContext.applyParameterForCapture(request);

        return request;
    }

    // copy JpegRotation,location ,record type and datetaken
    public SaveRequest copyPhotoRequest(SaveRequest originRequest) {
        Log.i(TAG, "[copyPhotoRequest]...");
        SaveRequest request = null;
        if (originRequest instanceof PhotoOperator) {
            request = ((PhotoOperator) originRequest).copyRequest();
        }
        return request;
    }

    public SaveRequest prepareVideoRequest(int fileType, int outputFileFormat, String resolution,
            int rotation) {
        Log.i(TAG, "[prepareVideoRequest]fileType = " + fileType + ",outputFileFormat = "
                + outputFileFormat + ",resolution = " + resolution + ",rotation = " + rotation);
        // we should prepare file path for recording, so here we fill lots of
        // info.
        VideoOperator operator = new VideoOperator(fileType, outputFileFormat,
                resolution, rotation);
        operator.prepareRequest();
        return operator;
    }

    public boolean addListener(FileSaverListener listener) {
        Log.d(TAG, "[addListener]...");
        if (!mSaverListenerList.contains(listener)) {
            return mSaverListenerList.add(listener);
        }

        return false;
    }

    public boolean removeListener(FileSaverListener l) {
        Log.d(TAG, "[removeListener]...");
        return mSaverListenerList.remove(l);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder b) {
            Log.i(TAG, "[onServiceConnected]...");
            mSaverService = ((FileSaverService.LocalBinder) b).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "[onServiceDisconnected]...");
            // when process crashed,unregister file saver listener
            synchronized (mSaveServiceObject) {
                if (mSaverService != null) {
                    mSaverService = null;
                }
            }
        }
    };

    private FileSaverService.FileSaverListener mFileSaverListener = new FileSaverService.FileSaverListener() {
        @Override
        public void onQueueStatus(boolean full) {
            mContext.getCameraAppUI().getPhotoShutter().setEnabled(!full);
        }

        @Override
        public void onFileSaved(SaveRequest request) {
            for (FileSaverListener listener : mSaverListenerList) {
                listener.onFileSaved(request);
            }
        };

        @Override
        public void onSaveDone() {
            synchronized (FileSaver.this) {
                FileSaver.this.notifyAll();
            }
        }
    };

    private void addSaveRequest(SaveRequest r) {
        synchronized (this) {
            // some time onDestroy() execute before add save request which is
            // through
            // post runnable
            synchronized (mSaveServiceObject) {
                if (mSaverService == null) {
                    Log.e(TAG, "[addSaveRequest]mSaverService is null,return.");
                    return;
                }

                while (mSaverService.getWaitingCount() >= QUEUE_LIMIT) {
                    try {
                        Log.i(TAG, "[addSaveRequest],waiting count > QUEUE_LIMIT 100,wait...");
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                        Log.e(TAG, "[addSaveRequest]exception:", ex);
                    }
                }
                Log.i(TAG, "[addSaveRequest]mSaverService.addSaveRequest...");
                mSaverService.addSaveRequest(r);
            }
        }
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    // sychronized this method to avoid when mFileNamer is just construct by an
    // AsynTask but
    // another AsynTask quickly get element from mFileNamer, it will return
    // null.
    private synchronized String createName(int fileType, long dateTaken, int count) {
        if (mFileNamer == null) {
            mFileNamer = new HashMap<Integer, ImageFileNamer>();
            ImageFileNamer photo = new ImageFileNamer(
                    mContext.getString(R.string.image_file_name_format));
            mFileNamer.put(Storage.FILE_TYPE_PHOTO, photo);
            // pano_file_name_format changed to image format for UX design.
            mFileNamer.put(Storage.FILE_TYPE_PANO, photo);
            mFileNamer.put(Storage.FILE_TYPE_VIDEO,
                    new ImageFileNamer(mContext.getString(R.string.video_file_name_format)));
        }
        Date date = new Date(dateTaken);
        String name = null;
        // tag != 0 is used for the last blend picture
        if (count == 0 && mFileNamer.get(fileType) != null) {
            name = mFileNamer.get(fileType).generateName(dateTaken);
        } else if (count > 0) {
            name = mFileNamer.get(fileType).generateContinuousName(dateTaken, count);
        }
        Log.d(TAG, "[createName]fileType = " + fileType + ",name = " + name);

        return name;
    }

    private abstract class RequestOperator implements SaveRequest {
        String mTitle;
        String mFileName;
        String mFilePath;
        String mMimeType;
        String mResolution;
        String mTempFilePath;

        int mFileType;
        int mWidth;
        int mHeight;
        int mOrientation;
        int mSlowMotionSpeed;
        int mTag;
        int mTempPictureType;
        int mTempOutputFileFormat;
        int mTempJpegRotation;
        // M:ConShots
        int mGroupIndex = 0;

        long mDateTaken;
        long mDataSize;
        long mDuration;
        long mGroupId;
        long mFocusValueHigh;
        long mFocusValueLow;

        boolean mIgnoreThumbnail;
        boolean mIsContinuousRequest = false;

        // for raw data
        byte[] mData;
        Uri mUri;
        Location mLocation;
        FileSaverListener mListener;

        @Override
        public boolean isContinuousRequest() {
            return mIsContinuousRequest;
        }

        @Override
        public boolean isQueueFull() {
            return mSaverService.isQueueFull();
        }

        @Override
        public boolean isIgnoreThumbnail() {
            return mIgnoreThumbnail;
        }

        @Override
        public String getTempFilePath() {
            return mTempFilePath;
        }

        @Override
        public String getFilePath() {
            return mFilePath;
        }

        @Override
        public int getDataSize() {
            if (mData == null) {
                return 0;
            } else {
                return mData.length;
            }
        }

        @Override
        public Uri getUri() {
            return mUri;
        }

        @Override
        public void releaseUri() {
            mUri = null;
        }

        @Override
        public int getJpegRotation() {
            return mTempJpegRotation;
        }

        @Override
        public Location getLocation() {
            return mLocation;
        }

        @Override
        public void setContinuousRequest(boolean isContinuousRequest) {
            mIsContinuousRequest = isContinuousRequest;
        }

        @Override
        public void setIgnoreThumbnail(boolean ignore) {
            mIgnoreThumbnail = ignore;
        }

        @Override
        public void setData(byte[] data) {
            if (data == null) {
                Log.w(TAG, "[setData]data is null,please check the reason!");
            }
            mData = data;
        }

        @Override
        public void setDuration(long duration) {
            mDuration = duration;
        }

        @Override
        public void setSlowMotionSpeed(int speed) {
            mSlowMotionSpeed = speed;
        }

        @Override
        public void setTag(int tag) {
            mTag = tag;
        }

        @Override
        public void setJpegRotation(int jpegRotation) {
            Log.d(TAG, "[setJpegRotation]jpegRotation = " + jpegRotation);
            mTempJpegRotation = jpegRotation;
        }

        @Override
        public void setLocation(Location loc) {
            mLocation = loc;
        }

        @Override
        public void setTempPath(String path) {
            mTempFilePath = path;
        }

        @Override
        public void setFileName(String name) {
            mFileName = name;
        }

        @Override
        public void setListener(FileSaverListener listener) {
            mListener = listener;
        }

        @Override
        public void notifyListener() {
            if (mListener != null) {
                mListener.onFileSaved(this);
            }
        }

        @Override
        public void updateDataTaken(long time) {
            mDateTaken = time;
        }

        @Override
        public FileSaverService.FileSaverListener getFileSaverListener() {
            return mFileSaverListener;
        }

        public void saveImageToDatabase(RequestOperator r) {
            Log.i(TAG, "[saveImageToDatabase]...");
            // Insert into MediaStore.
            ContentValues values = new ContentValues(14);
            values.put(ImageColumns.TITLE, r.mTitle);
            values.put(ImageColumns.DISPLAY_NAME, r.mFileName);
            values.put(ImageColumns.DATE_TAKEN, r.mDateTaken);
            values.put(ImageColumns.MIME_TYPE, r.mMimeType);
            values.put(ImageColumns.DATA, r.mFilePath);
            values.put(ImageColumns.SIZE, r.mDataSize);
            if (r.mLocation != null) {
                values.put(ImageColumns.LATITUDE, r.mLocation.getLatitude());
                values.put(ImageColumns.LONGITUDE, r.mLocation.getLongitude());
            }
            values.put(ImageColumns.ORIENTATION, r.mOrientation);
            // M: ConShots
            values.put(Images.Media.GROUP_ID, r.mGroupId);
            values.put(Images.Media.GROUP_INDEX, r.mGroupIndex);
            values.put(Images.Media.FOCUS_VALUE_HIGH, r.mFocusValueHigh);
            values.put(Images.Media.FOCUS_VALUE_LOW, r.mFocusValueLow);
            // Add for Refocus image database
            values.put(Images.Media.CAMERA_REFOCUS, r.mTag);
            values.put(ImageColumns.WIDTH, r.mWidth);
            values.put(ImageColumns.HEIGHT, r.mHeight);
            try {
                r.mUri = mContentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
                if (r.mUri != null) {
                    mContext.addSecureAlbumItemIfNeeded(false, r.mUri);
                    if (mContext.isNonePickIntent()) {
                        // picture taken and saved by camera which is launched
                        // by 3rd apps will
                        // be inserted into DB. But do not broadcast
                        // "New_Picture" intent,
                        // otherwise, it will not pass camera CTS test.
                        Util.broadcastNewPicture(mContext, r.mUri);
                    }
                    Log.i(TAG, "[saveImageToDatabase]mUri = " + r.mUri);
                }

            } catch (IllegalArgumentException e) { // Here we keep google
                                                   // default, don't
                // follow check style
                // This can happen when the external volume is already mounted,
                // but
                // MediaScanner has not notify MediaProvider to add that volume.
                // The picture is still safe and MediaScanner will find it and
                // insert it into MediaProvider. The only problem is that the
                // user
                // cannot click the thumbnail to review the picture.
                Log.e(TAG,
                        "[saveImageToDatabase]Failed to write MediaStore,IllegalArgumentException:",
                        e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG,
                        "[saveImageToDatabase]Failed to write MediaStore," +
                        "UnsupportedOperationException:",
                        e);
            }
        }

        @Override
        public void saveSync() {
            if (mData == null) {
                Log.w(TAG, "[saveSync]why mData==null???", new Throwable());
                return;
            }
            FileOutputStream out = null;
            try {
                // Write to a temporary file and rename it to the final name.
                // This
                // avoids other apps reading incomplete data.
                out = new FileOutputStream(mTempFilePath);
                out.write(mData);
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "[saveSync]Failed to write image", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "[saveSync]exception : ", e);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return new StringBuilder().append("RequestOperator(mUri=").append(mUri)
                    .append(", mTempFilePath=").append(mTempFilePath).append(", mFilePath=")
                    .append(mFilePath).append(", mIgnoreThumbnail=").append(mIgnoreThumbnail)
                    .append(")").toString();
        }

    }

    private class PhotoOperator extends RequestOperator {
        private PhotoOperator(int pictureType) {
            mTempPictureType = pictureType;
        }

        @Override
        public void prepareRequest() {
            Log.i(TAG, "[prepareRequest]PhotoOperator...");
            mFileType = Storage.FILE_TYPE_PHOTO;
            mDateTaken = System.currentTimeMillis();
            Location loc = mContext.getLocationManager().getCurrentLocation();
            if (loc != null) {
                mLocation = new Location(loc);
            }
        }

        @Override
        public void addRequest() {
            if (mData == null) {
                Log.w(TAG, "[addRequest]PhotoOperator,data is null,return!");
                return;
            }
            Log.i(TAG, "[addRequest]PhotoOperator...");
            addSaveRequest(this);
        }

        public PhotoOperator copyRequest() {
            Log.i(TAG, "[copyRequest]PhotoOperator...");
            PhotoOperator newRequest = new PhotoOperator(mTempPictureType);
            newRequest.mFileType = Storage.FILE_TYPE_PHOTO;
            newRequest.mDateTaken = System.currentTimeMillis();
            newRequest.mLocation = this.mLocation;
            newRequest.mTempJpegRotation = this.mTempJpegRotation;

            return newRequest;
        }

        @Override
        public synchronized void saveRequest() {
            if (mData == null) {
                Log.w(TAG, "[saveRequest]mData is null,return!");
                return;
            }
            int orientation = Exif.getOrientation(mData);
            // M: ConShots
            mGroupId = Exif.getGroupId(mData);
            mGroupIndex = Exif.getGroupIndex(mData);
            mFocusValueHigh = Exif.getFocusValueHigh(mData);
            mFocusValueLow = Exif.getFocusValueLow(mData);
            mOrientation = orientation;
            mDataSize = mData.length;

            if (null != mFileName) {
                mTitle = mFileName.substring(0, mFileName.indexOf('.'));
            } else {
                mTitle = createName(mFileType, mDateTaken, mGroupIndex);
                mFileName = Storage.generateFileName(mTitle, mTempPictureType);
                Log.i(TAG, "[saveRequest]PhotoOperator,mFileName = " + mFileName);
            }
            mFilePath = Storage.generateFilepath(mFileName);
            mTempFilePath = mFilePath + TEMP_SUFFIX;
            saveImageToSDCard(mTempFilePath, mFilePath, mData);
            // camera decouple
            mMimeType = Storage.generateMimetype(mTitle, mTempPictureType);
            checkDataProperty();
            saveImageToDatabase(this);
        }

        private void checkDataProperty() {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(mFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (exif != null) {
                mWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                mHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
            } else {
                // In google default camera, it set picture size when
                // capture and onPictureTaken
                // Here we just set picture size in onPictureTaken.
                Size s = mContext.getParameters().getPictureSize();
                if (s != null) {
                    mWidth = s.width;
                    mHeight = s.height;
                }
            }
            Log.d(TAG, "[checkDataProperty] mWidth = " + mWidth + ",mHeight = " + mHeight);
        }

        private void saveImageToSDCard(String tempFilePath, String filePath, byte[] data) {
            FileOutputStream out = null;
            try {
                // Write to a temporary file and rename it to the final name.
                // This
                // avoids other apps reading incomplete data.
                Log.d(TAG, "[saveImageToSDCard]begin add the data to SD Card");
                out = new FileOutputStream(tempFilePath);
                out.write(data);
                out.close();
                new File(tempFilePath).renameTo(new File(filePath));
            } catch (IOException e) {
                Log.e(TAG, "[saveImageToSDCard]Failed to write image,ex:", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "[saveImageToSDCard]IOException:", e);
                    }
                }
            }
            Log.i(TAG, "[saveImageToSDCard]end of add the data to SD Card");
        }

        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null && mData != null) {
                // Create a thumbnail whose width is equal or bigger than
                // that of the thumbnail view.
                int ratio = (int) Math.ceil((double) mWidth / thumbnailWidth);
                int inSampleSize = Integer.highestOneBit(ratio);
                thumb = Thumbnail.createThumbnail(mData, mOrientation, inSampleSize, mUri);
                Log.i(TAG, "[createThumbnail]PhotoOperator,mFileName = " + mFileName);
            }

            return thumb;
        }
    }

    private class PanoOperator extends RequestOperator {
        private PanoOperator(int pictureType) {
            mTempPictureType = pictureType;
        }

        @Override
        public void prepareRequest() {
            Log.i(TAG, "[prepareRequest]PanoOperator,mFileName = " + mFileName);
            mFileType = Storage.FILE_TYPE_PANO;
            mDateTaken = System.currentTimeMillis();
            Location loc = mContext.getLocationManager().getCurrentLocation();
            if (loc != null) {
                mLocation = new Location(loc);
            }
            mTitle = createName(mFileType, mDateTaken, 0);
            mFileName = Storage.generateFileName(mTitle, mTempPictureType);
            mFilePath = Storage.generateFilepath(mFileName);
            mTempFilePath = mFilePath + TEMP_SUFFIX;
        }

        @Override
        public void addRequest() {
            addSaveRequest(this);
        }

        @Override
        public void saveRequest() {
            // title, file path, temp file path is ready
            FileOutputStream out = null;
            try {
                // Write to a temporary file and rename it to the final name.
                // This
                // avoids other apps reading incomplete data.
                out = new FileOutputStream(mTempFilePath);
                out.write(mData);
                out.close();
                new File(mTempFilePath).renameTo(new File(mFilePath));
            } catch (IOException e) {
                Log.e(TAG, "[saveRequest]PanoOperator,Failed to write image", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "[saveRequest]PanoOperator,exception:", e);
                    }
                }
            }
            mDataSize = new File(mFilePath).length();
            try {
                ExifInterface exif = new ExifInterface(mFilePath);
                int orientation = Util.getExifOrientation(exif);
                int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
                mWidth = width;
                mHeight = height;
                mOrientation = orientation;
            } catch (IOException ex) {
                Log.e(TAG, "[saveRequest]PanoOperator,cannot read exif:", ex);
            }
            if (null == mFileName) {
                mTitle = createName(mFileType, mDateTaken, mGroupIndex);
                mFileName = Storage.generateFileName(mTitle, mTempPictureType);
                Log.i(TAG, "[saveRequest]PhotoOperator,mFileName = " + mFileName);
            }
            mMimeType = Storage.generateMimetype(mTitle, mTempPictureType);

            saveImageToDatabase(this);
        }

        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null) {
                // Create a thumbnail whose width is equal or bigger than
                // that of the thumbnail view.
                int widthRatio = (int) Math.ceil((double) mWidth / mContext.getPreviewFrameWidth());
                int heightRatio = (int) Math.ceil((double) mWidth
                        / mContext.getPreviewFrameHeight());
                int inSampleSize = Integer.highestOneBit(Math.max(widthRatio, heightRatio));
                thumb = Thumbnail.createThumbnail(mFilePath, mOrientation, inSampleSize, mUri);
                Log.i(TAG, "[createThumbnail]PanoOperator,mFileName = " + mFileName);
            }

            return thumb;
        }
    }

    private class VideoOperator extends RequestOperator {
        private VideoOperator(int fileType, int outputFileFormat, String resolution, int rotation) {
            mFileType = fileType;
            mTempOutputFileFormat = outputFileFormat;
            mResolution = resolution;
            mOrientation = rotation;
        }

        @Override
        public void prepareRequest() {
            mFileType = Storage.FILE_TYPE_VIDEO;
            mDateTaken = System.currentTimeMillis();
            mTitle = createName(mFileType, mDateTaken, 0);
            mFileName = mTitle + convertOutputFormatToFileExt(mTempOutputFileFormat);
            mMimeType = convertOutputFormatToMimeType(mTempOutputFileFormat);
            mFilePath = Storage.generateFilepath(mFileName);
        }

        @Override
        public void addRequest() {
            addSaveRequest(this);
        }

        @Override
        public void saveRequest() {
            // need video compute duration
            try {
                File temp = new File(mTempFilePath);
                File file = new File(mFilePath);
                temp.renameTo(file);
                mDataSize = file.length();

                ContentValues values = new ContentValues(13);
                values.put(Video.Media.TITLE, mTitle);
                values.put(Video.Media.DISPLAY_NAME, mFileName);
                values.put(Video.Media.DATE_TAKEN, mDateTaken);
                values.put(Video.Media.MIME_TYPE, mMimeType);
                values.put(Video.Media.DATA, mFilePath);
                values.put(Video.Media.SIZE, mDataSize);
                values.put(Video.Media.ORIENTATION, mOrientation);
                if (mLocation != null) {
                    values.put(Video.Media.LATITUDE, mLocation.getLatitude());
                    values.put(Video.Media.LONGITUDE, mLocation.getLongitude());
                }
                values.put(Video.Media.RESOLUTION, mResolution);
                values.put(Video.Media.DURATION, mDuration);
                values.put(Video.Media.SLOW_MOTION_SPEED, "(0,0)x" + mSlowMotionSpeed);
                mUri = mContentResolver.insert(Video.Media.EXTERNAL_CONTENT_URI, values);
                if (mUri != null) {
                    mContext.addSecureAlbumItemIfNeeded(true, mUri);
                    mContext.sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO,
                            mUri));
                }
            } catch (IllegalArgumentException th) { // Here we keep google
                                                    // default, don't
                // follow check style
                // This can happen when the external volume is already mounted,
                // but
                // MediaScanner has not notify MediaProvider to add that volume.
                // The picture is still safe and MediaScanner will find it and
                // insert it into MediaProvider. The only problem is that the
                // user
                // cannot click the thumbnail to review the picture.
                Log.e(TAG, "[saveRequest]VideoOperator,Failed to write MediaStore,exception:", th);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG,
                        "[saveImageToDatabase]Failed to write MediaStore," +
                        "UnsupportedOperationException:",
                        e);
            }
            Log.i(TAG, "[saveRequest]VideoOperator,end of wirte to DB,mUri = " + mUri);
        }

        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null) {
                Bitmap videoFrame = Thumbnail.createVideoThumbnailBitmap(mFilePath, thumbnailWidth);
                if (videoFrame != null) {
                    thumb = Thumbnail.createThumbnail(mUri, videoFrame, 0);
                }
            }

            return thumb;
        }
    }
}
