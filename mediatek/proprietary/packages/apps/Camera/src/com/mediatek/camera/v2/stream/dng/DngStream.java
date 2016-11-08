package com.mediatek.camera.v2.stream.dng;

import java.util.Map;
import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.mediatek.camera.v2.stream.ICaptureStream.CaptureStreamCallback;
import com.mediatek.camera.v2.stream.ICaptureStream;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;

public class DngStream implements IDngStream {
    private static final String                   TAG = DngStream.class.getSimpleName();
    private static int                            MAX_RAW_CAPTURE_IMAGES = 1;
    private ImageReader                           mRawImageReader;
    private CaptureStreamCallback                 mCallback;
    private CameraCharacteristics                 mCharacteristics;
    private HandlerThread                         mRawCaptureHandlerThread;
    private Handler                               mRawCaptureHandler;
    private Surface                               mRawCaptureSurface;
    private ICaptureStream                        mCaptureStream;
    private Size                                  mRawSize;
    private int                                   mRawCaptureWidth;
    private int                                   mRawCaptureHeight;


    public DngStream(ICaptureStream captureStreamController) {
        mCaptureStream = captureStreamController;
    }

    @Override
    public void setCaptureStreamCallback(CaptureStreamCallback callback) {
        mCallback = callback;
        if (mCaptureStream != null) {
            mCaptureStream.setCaptureStreamCallback(callback);
        }
    }

    private ImageReader.OnImageAvailableListener  mRawCaptureImageListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.i(TAG, "mRawCaptureImageListener mCallback = " + mCallback);
                    if (mCallback == null) {
                        return;
                    }
                    synchronized (mCallback) {
                        mCallback.onCaptureCompleted(reader.acquireLatestImage());
                    }
                }
            };

    @Override
    public boolean updateCaptureSize(Size pictureSize, int pictureFormat) {
        boolean capture_updated = mCaptureStream.updateCaptureSize(pictureSize, pictureFormat);

        if (mRawCaptureHandler == null) {
            mRawCaptureHandlerThread = new HandlerThread("ImageReaderStream.RawCaptureThread");
            mRawCaptureHandlerThread.start();
            mRawCaptureHandler = new Handler(mRawCaptureHandlerThread.getLooper());
        }

        if (mRawImageReader != null && mRawCaptureWidth == mRawSize.getWidth()
                && mRawCaptureHeight == mRawSize.getHeight()) {
            Log.i(TAG, "[updateCaptureSize]- configure the same size, skip : " + "" +
                    " width  = " + mRawCaptureWidth +
                    " height = " + mRawCaptureHeight);
            return capture_updated;
        }

        mRawCaptureWidth = mRawSize.getWidth();
        mRawCaptureHeight = mRawSize.getHeight();

        if (mRawImageReader != null) {
            mRawImageReader.close();
            mRawImageReader = null;
        }

        //TODO: the raw size is queried form setting?
        Log.i(TAG, "[updateCaptureSize]-raw size:" +
                mRawSize.getWidth() + "x" + mRawSize.getHeight());
        mRawImageReader = mRawImageReader.newInstance(mRawSize.getWidth(),
                mRawSize.getHeight(), ImageFormat.RAW_SENSOR, MAX_RAW_CAPTURE_IMAGES);
        mRawImageReader.setOnImageAvailableListener(mRawCaptureImageListener, mRawCaptureHandler);
        mRawCaptureSurface = mRawImageReader.getSurface();
        Log.i(TAG, "[updateCaptureSize]-Raw reader:" + mRawImageReader);

        return true;
    }


    @Override
    public Map<String, Surface> getCaptureInputSurface() {
        Map<String, Surface> surfaceMap = mCaptureStream.getCaptureInputSurface();
        if (mRawCaptureSurface == null) {
            throw new IllegalStateException("You should call" +
                    " CaptureStream.updateCaptureSize firstly, " +
                    "when get input capture surface");
        }

        Log.i(TAG, "getCaptureInputSurface:" + mRawCaptureSurface);
        surfaceMap.put(CAPUTRE_RAW_SURFACE_KEY, mRawCaptureSurface);
        return surfaceMap;
    }


    @Override
    public void releaseCaptureStream() {

        if (mRawImageReader != null) {
            mRawImageReader.close();
            mRawImageReader = null;
            mRawCaptureSurface = null;
        }

        if (mRawCaptureHandlerThread != null) {
            mRawCaptureHandlerThread.quitSafely();
            mRawCaptureHandler = null;
        }
    }

    public void updateCameraCharacteristics(CameraCharacteristics csdata) {
        mCharacteristics = csdata;
        StreamConfigurationMap config = mCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] rawSizes = config.getOutputSizes(ImageFormat.RAW_SENSOR);
        for (int i = 0; i < rawSizes.length; i++) {
            Log.i(TAG, "raw supported size:" + rawSizes[i]);
        }
        mRawSize = rawSizes[0];
    }
}
