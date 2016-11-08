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

import android.content.Context;
import android.graphics.SurfaceTexture;

import android.hardware.Camera;
import android.hardware.Camera.AsdCallback;
import android.hardware.Camera.AutoRamaCallback;
import android.hardware.Camera.AutoRamaMoveCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ContinuousShotCallback;
import android.hardware.Camera.DistanceInfoCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.FbOriginalCallback;
import android.hardware.Camera.HdrOriginalCallback;
import android.hardware.Camera.GestureCallback;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.ObjectTrackingListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.SmileCallback;
import android.hardware.Camera.StereoCameraJpsCallback;
import android.hardware.Camera.StereoCameraMaskCallback;
import android.hardware.Camera.StereoCameraWarningCallback;
import android.hardware.Camera.ZSDPreviewDone;
import android.view.SurfaceHolder;


import java.io.IOException;

public class AndroidCamera implements ICamera {
    private static final String TAG = "AndroidCamera";

    protected Camera mCamera = null;

    public AndroidCamera(Camera camera) {
        Util.assertError(null != camera);
        mCamera = camera;
    }

    public Camera getInstance() {
        return mCamera;
    }

    public void addCallbackBuffer(byte[] callbackBuffer) {
        Log.i(TAG, "[addCallbackBuffer]...");
        mCamera.addCallbackBuffer(callbackBuffer);
    }

    public void addRawImageCallbackBuffer(byte[] callbackBuffer) {
        Log.i(TAG, "[addRawImageCallbackBuffer]...");
        mCamera.addRawImageCallbackBuffer(callbackBuffer);
    }

    public void autoFocus(AutoFocusCallback cb) {
        Log.i(TAG, "[autoFocus]...");
        mCamera.autoFocus(cb);
    }

    public void cancelAutoFocus() {
        Log.i(TAG, "[cancelAutoFocus]...");
        mCamera.cancelAutoFocus();
    }

    public void cancelContinuousShot() {
        Log.i(TAG, "[cancelContinuousShot]...");
        mCamera.cancelContinuousShot();
    }

    public void stopSmileDetection() {
        Log.i(TAG, "[stopSmileDetection]...");
        mCamera.stopSmileDetection();
    }

    public void stopGestureDetection() {
        Log.i(TAG, "[stopGestureDetection]...");
        mCamera.stopGestureDetection();
    }

    public void lock() {
        Log.i(TAG, "[lock]...");
        mCamera.lock();
    }

    public Parameters getParameters() {
        Log.i(TAG, "[getParameters]...");
        return mCamera.getParameters();
    }

    public void release() {
        Log.i(TAG, "[release]...");
        mCamera.release();
    }

    public void reconnect() throws IOException {
        Log.i(TAG, "[reconnect]...");
        mCamera.reconnect();
    }

    public void setAsdCallback(AsdCallback cb) {
        Log.i(TAG, "[setASDCallback]...");
        mCamera.setAsdCallback(cb);
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
        Log.i(TAG, "[setAutoFocusMoveCallback]...");
        mCamera.setAutoFocusMoveCallback(cb);
    }

    public void setUncompressedImageCallback(PictureCallback cb) {
        Log.i(TAG, "[setUncompressedImageCallback]...");
        mCamera.setUncompressedImageCallback(cb);
    }

    public void setAutoRamaCallback(AutoRamaCallback cb) {
        Log.i(TAG, "[setAutoRamaCallback]...");
        mCamera.setAutoRamaCallback(cb);
    }

    public void setAutoRamaMoveCallback(AutoRamaMoveCallback cb) {
        Log.i(TAG, "[setAutoRamaMoveCallback]...");
        mCamera.setAutoRamaMoveCallback(cb);
    }

    public void setHdrOriginalCallback(HdrOriginalCallback cb) {
        Log.i(TAG, "[setHdrOriginalCallback]...");
        mCamera.setHdrOriginalCallback(cb);
    }

    public void setFbOriginalCallback(FbOriginalCallback cb) {
        Log.i(TAG, "[setFbOriginalCallback]...");
        mCamera.setFbOriginalCallback(cb);
    }

    public void setContext(Context context) {
        Log.i(TAG, "[setContext]do nothing...");
    }

    public void setContinuousShotCallback(ContinuousShotCallback callback) {
        Log.i(TAG, "[setContinuousShotCallback]...");
        mCamera.setContinuousShotCallback(callback);
    }

    public void setContinuousShotSpeed(int speed) {
        Log.i(TAG, "[setContinuousShotSpeed]speed = " + speed);
        mCamera.setContinuousShotSpeed(speed);
    }

    public void setDisplayOrientation(int degrees) {
        Log.i(TAG, "[setDisplayOrientation]degrees = " + degrees);
        mCamera.setDisplayOrientation(degrees);
    }

    public void setErrorCallback(ErrorCallback cb) {
        Log.i(TAG, "[setErrorCallback]...");
        mCamera.setErrorCallback(cb);
    }

    public void setFaceDetectionListener(FaceDetectionListener listener) {
        Log.i(TAG, "[setFaceDetectionListener]...");
        mCamera.setFaceDetectionListener(listener);
    }

    public void setParameters(Parameters params) {
        Log.i(TAG, "[setParameters]...");
        mCamera.setParameters(params);
    }

    public void setPreviewCallbackWithBuffer(PreviewCallback cb) {
        Log.i(TAG, "[setPreviewCallbackWithBuffer]...");
        mCamera.setPreviewCallbackWithBuffer(cb);
    }

    public void setPreviewDoneCallback(ZSDPreviewDone callback) {
        Log.i(TAG, "[setPreviewDoneCallback]...");
        mCamera.setPreviewDoneCallback(callback);
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException {
        Log.i(TAG, "[setPreviewTexture]...");
        mCamera.setPreviewTexture(surfaceTexture);
    }

    public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        Log.i(TAG, "[setPreviewDisplay]...");
        mCamera.setPreviewDisplay(holder);
    }

    public void setSmileCallback(SmileCallback cb) {
        Log.i(TAG, "[setSmileCallback]...");
        mCamera.setSmileCallback(cb);
    }

    public void setGestureCallback(GestureCallback cb) {
        Log.i(TAG, "[setGestureCallback]...");
        mCamera.setGestureCallback(cb);
    }

    public void setJpsCallback(StereoCameraJpsCallback cb) {
        Log.i(TAG, "[setJpsCallback]...");
        mCamera.setStereoCameraJpsCallback(cb);
    }

    public void setMaskCallback(StereoCameraMaskCallback cb) {
        Log.i(TAG, "[setMaskCallback]...");
        mCamera.setStereoCameraMaskCallback(cb);
    }

    public void setWarningCallback(StereoCameraWarningCallback cb) {
        Log.i(TAG, "[setWarningCallback]...");
        mCamera.setStereoCameraWarningCallback(cb);
    }

    public void setDistanceInfoCallback(DistanceInfoCallback cb) {
        Log.i(TAG, "[setDistanceInfoCallback]...");
        mCamera.setDistanceInfoCallback(cb);
    }

    public void setZoomChangeListener(OnZoomChangeListener listener) {
        Log.i(TAG, "[setZoomChangeListener]...");
        mCamera.setZoomChangeListener(listener);
    }

    public void startAutoRama(int num) {
        Log.i(TAG, "[startAUTORAMA]num = " + num);
        mCamera.startAutoRama(num);
    }

    public void startFaceDetection() {
        Log.i(TAG, "[startFaceDetection]...");
        mCamera.startFaceDetection();
    }

    public void startObjectTracking(int x, int y) {
        Log.i(TAG, "[startObjectTracking]x= " + x + ",y = " + y);
        mCamera.startObjectTracking(x, y);
    }

    public void stopObjectTracking() {
        Log.i(TAG, "[stopOT]...");
        mCamera.stopObjectTracking();
    }

    public void setObjectTrackingListener(ObjectTrackingListener listener) {
        Log.i(TAG, "[setObjectTrackingListener]...");
        mCamera.setObjectTrackingListener(listener);
    }

    public void startPreview() {
        Log.i(TAG, "[startPreview]...");
        mCamera.startPreview();
    }

    public void startSmoothZoom(int value) {
        Log.i(TAG, "[startSmoothZoom]value = " + value);
        mCamera.startSmoothZoom(value);
    }

    public void startSmileDetection() {
        Log.i(TAG, "[startSmileDetection]...");
        mCamera.startSmileDetection();
    }

    public void startGestureDetection() {
        Log.i(TAG, "[startGestureDetection]...");
        mCamera.startGestureDetection();
    }

    public void stopAutoRama(int isMerge) {
        Log.i(TAG, "[stopAutoRama]isMerge = " + isMerge);
        mCamera.stopAutoRama(isMerge);
    }

    public void stopFaceDetection() {
        Log.i(TAG, "[stopFaceDetection]...");
        mCamera.stopFaceDetection();
    }

    public void stopPreview() {
        Log.i(TAG, "[stopPreview]...");
        mCamera.stopPreview();
    }

    public void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg) {
        Log.i(TAG, "[takePicture],callback 1...");
        mCamera.takePicture(shutter, raw, jpeg);
    }

    public void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview,
            PictureCallback jpeg) {
        Log.i(TAG, "[takePicture]callback 2...");
        mCamera.takePicture(shutter, raw, postview, jpeg);
    }

    public void unlock() {
        Log.i(TAG, "[unlock]...");
        mCamera.unlock();
    }

    public void start3DSHOT(int num) {
        Log.i(TAG, "[start3DSHOT]num = " + num);
        mCamera.start3DSHOT(num);
    }

    public void stop3DSHOT(int num) {
        Log.i(TAG, "[stop3DSHOT]num = " + num);
        mCamera.stop3DSHOT(num);
    }

    public void setPreview3DModeForCamera(boolean enable) {
        Log.i(TAG, "[setPreview3DModeForCamera]enable = " + enable);
        mCamera.setStereo3DModeForCamera(enable);
    }
    public void setOneShotPreviewCallback(PreviewCallback cb) {
        Log.i(TAG, "[setOneShotPreviewCallback]");
        mCamera.setOneShotPreviewCallback(cb);
    }

    public void setMainFaceCoordinate(int x, int y) {
        Log.i(TAG, "[setMainFaceCoordinate], x:" + x + ", y:" + y);
        mCamera.setMainFaceCoordinate(x, y);
    }
    public void cancelMainFaceInfo() {
        mCamera.cancelMainFaceInfo();
    }
}
