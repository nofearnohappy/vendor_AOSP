package com.mediatek.camera.v2.mode.facebeauty;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import com.mediatek.camera.v2.exif.ExifInterface;

import java.io.IOException;

public class CfbCaptureHelper {

    private static final String TAG = "CfbCaptureHelper";

    private static final String MIN_FACE_BEAUTY_VALUE = "-4";
    private static final String MAX_FACE_BEAUTY_VALUE = "4";

    private static final String WORKAROUND_MIN_FACE_BEAUTY_VALUE = "-12";
    private static final String WORKAROUND_MAX_FACE_BEAUTY_VALUE = "12";


    /**
     * in native:alps/vendor/mediatek/proprietary/platform/mt6752/hardware/
     * mtkcam/hal/aaa/aaa_hal_raw.cpp,
     * the get the lightSourceMode use this function:
     * 0     switch (m_rParam.u4AwbMode)
         {
         case MTK_CONTROL_AWB_MODE_AUTO:
         case MTK_CONTROL_AWB_MODE_WARM_FLUORESCENT:
         case MTK_CONTROL_AWB_MODE_TWILIGHT:
         case MTK_CONTROL_AWB_MODE_INCANDESCENT:
             rExifInfo.u4LightSource = eLightSourceId_Other;
             break;
         case MTK_CONTROL_AWB_MODE_DAYLIGHT:
             rExifInfo.u4LightSource = eLightSourceId_Daylight;
             break;
         case MTK_CONTROL_AWB_MODE_FLUORESCENT:
             rExifInfo.u4LightSource = eLightSourceId_Fluorescent;
             break;
     #if 0
         case MTK_CONTROL_AWB_MODE_TUNGSTEN:
             rExifInfo.u4LightSource = eLightSourceId_Tungsten;
             break;
     #endif
         case MTK_CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
             rExifInfo.u4LightSource = eLightSourceId_Cloudy;
             break;
         case MTK_CONTROL_AWB_MODE_SHADE:
             rExifInfo.u4LightSource = eLightSourceId_Shade;
             break;
         default:
             rExifInfo.u4LightSource = eLightSourceId_Other;
             break;
         }
     * @param awbMode current awbmode
     * @return according the awbmode ,calculate the right lightsource mode
     */
    public static short getLightSourceMode(int awbMode) {
        short lightSourceMode = -1;
        switch (awbMode) {
        case CameraMetadata.CONTROL_AWB_MODE_AUTO:
        case CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT:
        case CameraMetadata.CONTROL_AWB_MODE_TWILIGHT:
        case CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT:
            lightSourceMode = ExifInterface.LightSource.OTHER;
            break;

        case CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT:
            lightSourceMode = ExifInterface.LightSource.DAYLIGHT;
            break;

        case CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT:
            lightSourceMode = ExifInterface.LightSource.FLUORESCENT;
            break;

        case CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
            lightSourceMode = ExifInterface.LightSource.CLOUDY_WEATHER;
            break;

        case CameraMetadata.CONTROL_AWB_MODE_SHADE:
            lightSourceMode = ExifInterface.LightSource.SHADE;
            break;

        case CameraMetadata.CONTROL_AWB_MODE_OFF:

            break;

        default:
            lightSourceMode = ExifInterface.LightSource.OTHER;
            break;
        }
        Log.i(TAG, "[getLightSourceMode] awbMode = " + awbMode
                + ",lightSourceMode = " + lightSourceMode);

        return lightSourceMode;
    }


    /**
     * in native:alps/vendor/mediatek/proprietary/platform/mt6752/hardware
     * /mtkcam/hal/aaa/aaa_hal_raw.cpp,
     * the get the exposure program use this function:
     *  switch (m_rParam.u4SceneMode)
         {
         case MTK_CONTROL_SCENE_MODE_PORTRAIT:
             rExifInfo.u4ExpProgram = eExpProgramId_Portrait;
             break;
         case MTK_CONTROL_SCENE_MODE_LANDSCAPE:
             rExifInfo.u4ExpProgram = eExpProgramId_Landscape;
             break;
         default:
             rExifInfo.u4ExpProgram = eExpProgramId_NotDefined;
             break;
         }
     * @param sceneMode current sceneMode
     * @return according current scenemode,calculate the right exposure program mode
     */
    public static short getExposureProgram(int sceneMode) {
        short exposureProgram = -1;
        switch (sceneMode) {
        case CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT:
            exposureProgram = ExifInterface.ExposureProgram.PROTRAIT_MODE;
            break;

        case CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE:
            exposureProgram = ExifInterface.ExposureProgram.LANDSCAPE_MODE;
            break;

        default:
            exposureProgram = ExifInterface.ExposureProgram.NOT_DEFINED;

            break;
        }
        Log.i(TAG, "[getExposureProgram] sceneMode = " + sceneMode
                + ",exposureProgram = " + exposureProgram);

        return exposureProgram;
    }

    // TODO :location is not finished
    public static void saveJpegExifInfo(byte[] data, TotalCaptureResult result, int orientation) {
        Log.i(TAG, "[saveJpegExifInfo] ++++++++");
        try {
            ExifInterface exif = new ExifInterface();
            exif.readExif(data);

            if (exif != null) {
                float focusNumber = result.get(CaptureResult.LENS_APERTURE);
                float focusLength = result.get(CaptureResult.LENS_FOCAL_LENGTH);
                int awbMode = result.get(CaptureResult.CONTROL_AWB_MODE);
                int aeMode = result.get(CaptureResult.CONTROL_AE_MODE);
                int sceneMode = result.get(CaptureResult.CONTROL_SCENE_MODE);
                long exposureTime = result
                        .get(CaptureResult.SENSOR_EXPOSURE_TIME);
                int iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
                int exposureCompensation = result
                        .get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
                int jpegOrientation = orientation /*result
                        .get(CaptureResult.JPEG_ORIENTATION)*/;

                Log.i(TAG, "[result from capture],focusNumber = " + focusNumber
                        + ",focusLength = " + focusLength + ",awbMode ="
                        + awbMode + ",aeMode = " + aeMode + ",sceneMode = "
                        + sceneMode + ",exposureTime = " + exposureTime
                        + ",iso = " + iso + ",exposureCompensation = "
                        + exposureCompensation + ",jpegOrientation = "
                        + jpegOrientation);

                readJpegExifInfo(exif);

                boolean setFNumberOk = exif.setTagValue(
                        ExifInterface.TAG_APERTURE_VALUE, focusNumber);
                boolean setFocusLengthOk = exif.setTagValue(
                        ExifInterface.TAG_FOCAL_LENGTH, focusLength);
                boolean setWitheBalanceOk = exif.setTagValue(
                        ExifInterface.TAG_WHITE_BALANCE, awbMode);
                boolean setExposureModeOk = exif.setTagValue(
                        ExifInterface.TAG_EXPOSURE_MODE, aeMode);
                boolean setSceneModeOk = exif.setTagValue(
                        ExifInterface.TAG_SCENE_CAPTURE_TYPE, sceneMode);
                boolean setLightSourceOk = exif.setTagValue(
                        ExifInterface.TAG_LIGHT_SOURCE,
                        CfbCaptureHelper.getLightSourceMode(awbMode));
                boolean setExposureProgramOk = exif.setTagValue(
                        ExifInterface.TAG_EXPOSURE_PROGRAM,
                        CfbCaptureHelper.getExposureProgram(sceneMode));
                boolean setIsoOk = exif.setTagValue(
                        ExifInterface.TAG_ISO_SPEED_RATINGS, iso);

                // exposure time native not prepare
                boolean setExposureTimeOk = exif.setTagValue(
                        ExifInterface.TAG_EXPOSURE_TIME, exposureCompensation);
                boolean setExposureBiasOk = exif.setTagValue(
                        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                        exposureCompensation);

                short exifOrientation = 0;
                switch (jpegOrientation) {
                case 0:
                    exifOrientation = ExifInterface.Orientation.TOP_LEFT;
                    break;

                case 90:
                    exifOrientation = ExifInterface.Orientation.RIGHT_TOP;
                    break;

                case 180:
                    exifOrientation = ExifInterface.Orientation.BOTTOM_LEFT;
                    break;

                case 270:
                    exifOrientation = ExifInterface.Orientation.RIGHT_BOTTOM;
                    break;

                default:
                    exifOrientation = ExifInterface.Orientation.TOP_LEFT;
                    break;
                }
                boolean setOientationOk = exif.setTagValue(
                        ExifInterface.TAG_ORIENTATION, exifOrientation);

                Log.i(TAG, "[update exif state] setFNumberOk = " + setFNumberOk
                        + ",setFocusLengthOk = " + setFocusLengthOk
                        + ",setWitheBalanceOk = " + setWitheBalanceOk
                        + ",setExposureModeOk = " + setExposureModeOk
                        + ",setSceneModeOk = " + setSceneModeOk
                        + ",setLightSourceOk = " + setLightSourceOk
                        + ",setExposureProgramOk = " + setExposureProgramOk
                        + ",setIsoOk = " + setIsoOk + ",setExposureTimeOk = "
                        + setExposureTimeOk + ",setExposureBiasOk = "
                        + setExposureBiasOk + ",setOientationOk = "
                        + setOientationOk);

            }
        } catch (IOException e) {
            Log.e(TAG, "[saveJpegExifInfo] error", e);
            e.printStackTrace();
        }
        Log.i(TAG, "[saveJpegExifInfo] -----");

    }

    public static void readJpegExifInfo(ExifInterface exif) {
        Integer imageWidth = exif
                .getTagIntValue(ExifInterface.TAG_IMAGE_WIDTH);
        Integer imageHeight = exif
                .getTagIntValue(ExifInterface.TAG_IMAGE_LENGTH);
        Object exifFocusNumber = exif
                .getTagValue(ExifInterface.TAG_APERTURE_VALUE);
        Object exifFocusLength = exif
                .getTagValue(ExifInterface.TAG_FOCAL_LENGTH);
        int exifAwbMode = exif
                .getTagIntValue(ExifInterface.TAG_WHITE_BALANCE);
        Integer exifLightSourceMode = exif
                .getTagIntValue(ExifInterface.TAG_LIGHT_SOURCE);
        Integer exifExposureProgram = exif
                .getTagIntValue(ExifInterface.TAG_EXPOSURE_PROGRAM);
        int exifAeMode = exif
                .getTagIntValue(ExifInterface.TAG_EXPOSURE_MODE);
        int exifSceneMode = exif
                .getTagIntValue(ExifInterface.TAG_SCENE_CAPTURE_TYPE);
        int exifIso = exif
                .getTagIntValue(ExifInterface.TAG_ISO_SPEED_RATINGS);
        int exifJepgOrientation = exif
                .getTagIntValue(ExifInterface.TAG_ORIENTATION);
        long[] exifExposureTime = exif
                .getTagLongValues(ExifInterface.TAG_EXPOSURE_TIME);
        Integer exifExposureCompensation = exif
                .getTagIntValue(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);

        Log.i(TAG, "[result from exif],imageWidth = " + imageWidth
                + "imageHeight = " + imageHeight
                + ",exifFocusNumber = " + exifFocusNumber
                + ",exifAwbMode = " + exifAwbMode
                + ",exifLightSourceMode = " + exifLightSourceMode
                + ",exifExposureProgram = " + exifExposureProgram
                + ",exifFocusLength = " + exifFocusLength
                + ",exifFocusLength = " + exifFocusLength
                + ",exifAeMode = " + exifAeMode + ",exifSceneMode = "
                + exifSceneMode + ",exifExposureTime = "
                + exifExposureTime + ",exifIso = " + exifIso
                + ",exifExposureCompensation = "
                + exifExposureCompensation + ",exifJepgOrientation = "
                + exifJepgOrientation);
    }



    // because current CFB support max and min face properties value is -12 ~12,
    // but we not have the method to get the max or min value,and also not
    // need to change the value in settings,in Phase two,we will change this.
    public static String workAroundValue(String currentValue) {
        if (MIN_FACE_BEAUTY_VALUE.equals(currentValue)) {
            return WORKAROUND_MIN_FACE_BEAUTY_VALUE;
        } else if (MAX_FACE_BEAUTY_VALUE.equals(currentValue)) {
            return WORKAROUND_MAX_FACE_BEAUTY_VALUE;
        } else {
            return currentValue;
        }
    }
}
