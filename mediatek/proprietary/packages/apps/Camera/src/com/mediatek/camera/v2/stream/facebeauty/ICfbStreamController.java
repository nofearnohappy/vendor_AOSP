package com.mediatek.camera.v2.stream.facebeauty;


import com.mediatek.mmsdk.BaseParameters;

import java.util.List;

public interface ICfbStreamController {

    /**
     * open the effectStream,after opened,can setParameters() for take picture,
     * if you not change the parameters ,before startCaputre,you need first
     * getCaptureRequriement() for CFB FO requirement
     *
     * @param callback
     */
    public void openStream(StreamStatusCallback callback);

    public void startCapture(CaptureStatusCallback captureStatusCallback);

    public void setParameters(List<String> key, List<String> value);

    public void setCurrentVideoTag(boolean isVideo);

    public void closeStream();

    public interface StreamStatusCallback {

        public void onStreamClosed();

        public void onStreamOpenFailed();

        // the error is what ? details need think TODO
        public void onStreamError();

        public void onReadyForCapture();

        public void onSetupFailed();
    }

    public interface CaptureStatusCallback {

        public void onInputFrameProcessed(BaseParameters parameter, BaseParameters partialResult);

        public void onOutputFrameProcessed(BaseParameters parameter, BaseParameters partialResult);

        public void onCaptureCompleted(BaseParameters result, long uid);

        public void onCaptureAborted(BaseParameters result);

        public void onCaptureFailed(BaseParameters result);
    }
}
