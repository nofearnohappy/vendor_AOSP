package com.mediatek.camera.v2.stream.facebeauty;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.mediatek.camera.util.jpegcodec.JpegEncoder;
import com.mediatek.camera.v2.stream.ICaptureStream;
import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.CameraEffect;
import com.mediatek.mmsdk.CameraEffect.StateCallback;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.CameraEffectManager;
import com.mediatek.mmsdk.CameraEffectSession;
import com.mediatek.mmsdk.CameraEffectSession.CaptureCallback;
import com.mediatek.mmsdk.EffectHalVersion;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this stream controller used for control CFB capture steam. this class will
 * connect effect stream to capture steam because CFB just effect the
 * capture,not influence the preview and recording,that's why just implements
 * the capture steam
 */
public class CfbStreamControllerImpl implements ICfbStreamController, ICaptureStream {

    private static final String TAG = "CfbStreamControllerImpl";
    private static final String EFFECT_HAL_HANDLER_NAME =
            "CfbStreamControllerImpl.mEffectHalHandler";
    private static final String SESSION_HANDLER_NAME = "CfbStreamControllerImpl.sessionHanlder";

    private static final boolean DEBUG = true;
    private boolean mIsInVideoMode = false;
    private boolean mIsSizeChange = true;

    private int mMajorVersion = 1;
    private int mMinorVersion = 0;
    private int mPictureFormat = -1;

    private Size mCurrentPictureSize;

    private CameraEffectManager mCameraEffectHalManager;
    private EffectHalVersion mEffectHalVersion;
    private CameraEffect mCameraEffect;
    private CameraEffectSession mCameraEffectHalSession;
    private Context mContext;
    private Handler mEffectHalHandler;
    private Handler mEffectSessionHandler;
    private List<BaseParameters> mSurfaceParameters = new ArrayList<BaseParameters>();
    private BaseParameters mBaseParameters;
    private List<Surface> mOutPutSurfaces = new ArrayList<>();
    private Map<String, Surface> mInputSurface = new HashMap<String, Surface>();

    // this callback will notify user current CFB effect HAL stream status
    private StreamStatusCallback mStreamStatusCallback;
    private CaptureStatusCallback mCaptureStatusCallback;

    private ICaptureStream mNextCaptureStream;

    private JpegEncoder mJpegEncoder;

    /**
     * Construct steam controller.
     * @param captureStreamController
     *            the next steam need to connect
     */
    public CfbStreamControllerImpl(ICaptureStream captureStreamController) {
        Log.i(TAG, "[CfbStreamControllerImpl]");
        mNextCaptureStream = captureStreamController;

        mEffectHalHandler = createHandler(EFFECT_HAL_HANDLER_NAME);
        mEffectSessionHandler = createHandler(SESSION_HANDLER_NAME);

        mCameraEffectHalManager = new CameraEffectManager(mContext);
        mEffectHalVersion = new EffectHalVersion();
        mEffectHalVersion.setName(BaseParameters.KEY_EFFECT_NAME_CFB);
        mEffectHalVersion.setMajor(mMajorVersion);
        mEffectHalVersion.setMinor(mMinorVersion);

        mBaseParameters = new BaseParameters();
        mBaseParameters.set(BaseParameters.KEY_OUT_PUT_CAPTURE_NUMBER, 1);
        mSurfaceParameters.add(mBaseParameters);
    }

    @Override
    public void openStream(StreamStatusCallback streamStatusCallback) {
        if (DEBUG) {
            Log.i(TAG, "[openStream] +++++,streamStatusCallback = " + streamStatusCallback);
        }
        // new JPEG encoder
        mJpegEncoder = JpegEncoder.newInstance(mContext, true);

        // the callback must not be null
        Assert.assertNotNull(streamStatusCallback);
        mStreamStatusCallback = streamStatusCallback;
        try {
            // current just open the CFB EffectHal
            mCameraEffect = mCameraEffectHalManager.openEffectHal(mEffectHalVersion,
                    mEffectHalStateCallback, mEffectHalHandler);
        } catch (CameraEffectHalException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "[openStream] error : " + e.getMessage());
            e.printStackTrace();
        }

        if (DEBUG) {
            Log.i(TAG, "[openStream] ----,mCameraEffect = " + mCameraEffect);
        }
    }

    @Override
    public void setParameters(List<String> key, List<String> value) {
        Assert.assertNotNull(key);
        Assert.assertNotNull(value);

        for (int i = 0; i < key.size(); i++) {
            if (key.get(i) != null && value.get(i) != null) {
                mBaseParameters.set(key.get(i), value.get(i));
            }
        }
        Log.i(TAG, "[setParameters] the parameter is : " + mBaseParameters.flatten());

        mCameraEffect.setParameters(mBaseParameters);
    }

    @Override
    public void startCapture(CaptureStatusCallback captureStatusCallback) {
        if (DEBUG) {
            Log.i(TAG, "[startCapture] captureStatusCallback = " + captureStatusCallback
                    + ",mOutPutSurfaces = " + mOutPutSurfaces.size() + ",mSurfaceParameters = "
                    + mSurfaceParameters.toString());
        }

        Assert.assertNotNull(captureStatusCallback);
        setOutputSurface(mOutPutSurfaces, mSurfaceParameters);
        mCaptureStatusCallback = captureStatusCallback;
        mJpegEncoder.startEncode();
        Assert.assertNotNull(mCameraEffectHalSession);
        mCameraEffectHalSession.startCapture(mSessionCaptureCallback, mEffectSessionHandler);

    }

    @Override
    public void setCurrentVideoTag(boolean isVideo) {
        Log.i(TAG, "[setCurrentVideoTag] isVideo = " + isVideo);
        mIsInVideoMode = isVideo;
    }

    @Override
    public void closeStream() {
        if (DEBUG) {
            Log.i(TAG, "[closeStream] mCameraEffectHalSession = " + mCameraEffectHalSession);
        }
        mIsSizeChange = true;
        mCurrentPictureSize = null;
        mPictureFormat = -1;
        mOutPutSurfaces.clear();
        if (mCameraEffectHalSession != null) {
            mCameraEffectHalSession.closeSession();
            mCameraEffectHalSession = null;
        }
        if (mCameraEffect != null) {
            mCameraEffect.closeEffect();
            mCameraEffect.close();
        }
        mJpegEncoder.release();
    }

    // ****************************************************************
    // ****************************************************************
    // follow is CaptureStreamController interface implements.....Begin
    // ****************************************************************
    // ****************************************************************
    @Override
    public void setCaptureStreamCallback(CaptureStreamCallback callback) {
        if (mNextCaptureStream != null) {
            mNextCaptureStream.setCaptureStreamCallback(callback);
        }
    }

    @Override
    public boolean updateCaptureSize(Size pictureSize, int pictureFormat) {
        Log.i(TAG, "[updateCaptureSize] picturesize's width = " + pictureSize.getWidth()
                + ",Heigth = " + pictureSize.getHeight() + ",pictureFormat = " + pictureFormat);

        if (mCurrentPictureSize != null && mCurrentPictureSize.getWidth() == pictureSize.getWidth()
                && mCurrentPictureSize.getHeight() == pictureSize.getHeight()
                && mPictureFormat == pictureFormat) {
            mIsSizeChange = false;
            Log.i(TAG, "picture size and format is not changed");
            return false;
        }

        mCurrentPictureSize = pictureSize;
        mPictureFormat = pictureFormat;
        mIsSizeChange = true;

        if (mNextCaptureStream != null) {
            mNextCaptureStream.updateCaptureSize(pictureSize, pictureFormat);
        }

        if (mIsInVideoMode) {
            Log.i(TAG, "[updateCaptureSize] current is in video mode,so do noting Effect Hal");
            return false;
        }

        // add the picture size in the base parameters
        mBaseParameters.set(BaseParameters.KEY_PICTURE_WIDTH, pictureSize.getWidth());
        mBaseParameters.set(BaseParameters.KEY_PICTURE_HEIGHT, pictureSize.getHeight());

        mCameraEffect.setParameters(mBaseParameters);

        Map<String, Surface> surfaceMap = mNextCaptureStream.getCaptureInputSurface();
        if (mOutPutSurfaces != null) {
            for (int i = 0; i < mOutPutSurfaces.size(); i++) {
                Log.i(TAG, "[updateCaptureSize],before clear the surface, curren have : "
                        + mOutPutSurfaces.get(i));
            }
            mOutPutSurfaces.clear();
        }

        for (int i = 0; i < surfaceMap.size(); i++) {
            Surface surface = surfaceMap.get(CAPUTRE_SURFACE_KEY);
            if (!mOutPutSurfaces.contains(surface)) {
                mOutPutSurfaces.add(surface);
            }
        }

        Log.i(TAG, "[updateCaptureSize],mOutPutSurfaces = " + mOutPutSurfaces);
        return true;
    }

    @Override
    public Map<String, Surface> getCaptureInputSurface() {
        Log.i(TAG, "[getCaptureInputSurface]+++++, mIsInVideoMode = " + mIsInVideoMode
                + ",isSizeChange  = " + mIsSizeChange);

        // if current is in Video mode, not need Effect HAL Surface
        if (mIsInVideoMode) {
            return mNextCaptureStream.getCaptureInputSurface();
        }

        if (!mIsSizeChange) {
            Log.i(TAG,
                    "[getCaptureInputSurface] the input surface have got once,not need only more");
            return mInputSurface;
        }

        List<Surface> inputSurfaces = mCameraEffect.getInputSurface();

        if (inputSurfaces.size() != 0) {
            for (int i = 0; i < inputSurfaces.size(); i++) {
                mInputSurface.put(CAPUTRE_SURFACE_KEY, inputSurfaces.get(i));
                Log.i(TAG, "getCaptureInputSurface ,surface = " + inputSurfaces.get(i));
            }
        }
        return mInputSurface;
    }

    @Override
    public void releaseCaptureStream() {
        if (mNextCaptureStream != null) {
            mNextCaptureStream.releaseCaptureStream();
        }

    }

    // ****************************************************************
    // ****************************************************************
    // follow is CaptureStreamController interface implements.....End
    // ****************************************************************
    // ****************************************************************

    private void setOutputSurface(List<Surface> surfaces, List<BaseParameters> surfaceParameters) {
        Log.i(TAG, "[setOutputSurface] surfaces'length = " + surfaces.size()
                + ",mCameraEffectHalSession = " + mCameraEffectHalSession + ",mCameraEffectHal = "
                + mCameraEffect);
        if (mCameraEffect != null) {
            // when APP calling this function,will create the
            // CameraEffectHalSession
            try {
                // get the capture input surface for mjpegEffect out put surface
                List<Surface> effectOutPutSurface = new ArrayList<Surface>();
                Surface jpegInputSurface = mJpegEncoder.configInputSurface(surfaces.get(0),
                        mCurrentPictureSize.getWidth(), mCurrentPictureSize.getHeight(),
                        mPictureFormat);
                effectOutPutSurface.add(jpegInputSurface);
                mCameraEffectHalSession = mCameraEffect.createCaptureSession(effectOutPutSurface,
                        surfaceParameters, mSessionStateCallback, null);
            } catch (CameraEffectHalException e) {
                e.printStackTrace();
            }
        }
    }

    private Handler createHandler(String handlerName) {
        HandlerThread thread = new HandlerThread(handlerName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    private StateCallback mEffectHalStateCallback = new StateCallback() {

        @Override
        public void onError(CameraEffect effect, int error) {
            Log.i(TAG, "[onError] effectHal = " + effect + ",error = " + error);
            // the error type need pass on to user //TODO
            mStreamStatusCallback.onStreamError();
            mCameraEffect.closeEffect();
            mCameraEffect.close();
        }

        @Override
        public void onDisconnected(CameraEffect effect) {
            Log.i(TAG, "[onDisconnected] effectHal = " + effect);
            mStreamStatusCallback.onStreamError();
        }
    };

    private com.mediatek.mmsdk.CameraEffectSession.SessionStateCallback mSessionStateCallback =
            new com.mediatek.mmsdk.CameraEffectSession.SessionStateCallback() {

        @Override
        public void onPrepared(CameraEffectSession session) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCameraEffectHalSession = session;
            mStreamStatusCallback.onSetupFailed();
        }

        // Current onConfigured notified before onPrepared,so AP get
        // CameraEffectHalSession
        // first time is here
        @Override
        public void onConfigured(CameraEffectSession session) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCameraEffectHalSession = session;
            mStreamStatusCallback.onReadyForCapture();
        }

        @Override
        public void onConfigureFailed(CameraEffectSession session) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mStreamStatusCallback.onSetupFailed();
        }

        @Override
        public void onClosed(CameraEffectSession session) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCameraEffectHalSession = session;
            mStreamStatusCallback.onStreamClosed();
            // when session is closed,need set the mCameraEffectHalSession to
            // null TODO
        }
    };

    private CaptureCallback mSessionCaptureCallback = new CaptureCallback() {

        @Override
        public void onOutputFrameProcessed(CameraEffectSession session, BaseParameters parameter,
                BaseParameters partialResult) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCaptureStatusCallback.onOutputFrameProcessed(parameter, partialResult);
        }

        @Override
        public void onInputFrameProcessed(CameraEffectSession session, BaseParameters parameter,
                BaseParameters partialResult) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCaptureStatusCallback.onInputFrameProcessed(parameter, partialResult);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraEffectSession session, BaseParameters result,
                long uid) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCaptureStatusCallback.onCaptureCompleted(result, uid);
        }

        @Override
        public void onCaptureSequenceAborted(CameraEffectSession session, BaseParameters result) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCaptureStatusCallback.onCaptureAborted(result);
        }

        @Override
        public void onCaptureFailed(CameraEffectSession session, BaseParameters result) {
            if (DEBUG) {
                Log.d(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }
            mCaptureStatusCallback.onCaptureFailed(result);
        }
    };

}
