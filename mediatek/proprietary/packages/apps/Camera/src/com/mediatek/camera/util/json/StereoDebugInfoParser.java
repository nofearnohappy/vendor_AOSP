package com.mediatek.camera.util.json;

import android.graphics.Rect;
import android.util.Log;

public class StereoDebugInfoParser {
    private final static String TAG = "mtkGallery2/StereoDebugInfoParser";
    private final static String MASKINFO_TAG = "mask_info";
    private final static String MASKINFO_WIDTH = "width";
    private final static String MASKINFO_HEIGHT = "height";
    private final static String MASKINFO_MASK = "mask";
    private final static String JPSINFO_TAG = "JPS_size";
    private final static String JPSINFO_WIDTH = "width";
    private final static String JPSINFO_HEIGHT = "height";
    private final static String POSINFO_TAG = "main_cam_align_shift";
    private final static String POSINFO_X = "x";
    private final static String POSINFO_Y = "y";
    private final static String TOUCH_COORD_INFO_TAG = "focus_roi";
    private final static String TOUCH_COORD_INFO_LEFT = "left";
    private final static String TOUCH_COORD_INFO_TOP = "top";
    private final static String TOUCH_COORD_INFO_RIGHT = "right";
    private final static String TOUCH_COORD_INFO_BOTTOM = "bottom";
    private final static String VIEWINFO_TAG = "input_image_size";
    private final static String VIEWINFO_WIDTH = "width";
    private final static String VIEWINFO_HEIGHT = "height";
    private final static String ORIENTATIONINFO_TAG = "capture_orientation";
    private final static String ORIENTATIONINFO_ORIENTATION = "orientation";
    private final static String MAIN_CAM_POSITION_INFO_TAG = "sensor_relative_position";
    private final static String MAIN_CAM_POSITION_INFO_POSITION = "relative_position";
    private final static String VERIFY_GEO_INFO_TAG = "verify_geo_data";
    private final static String VERIFY_GEO_INFO_LEVEL = "qulity_level";
    private final static String VERIFY_GEO_INFO_STATISTICS = "statistics";
    private final static String VERIFY_PHO_INFO_TAG = "verify_pho_data";
    private final static String VERIFY_PHO_INFO_LEVEL = "qulity_level";
    private final static String VERIFY_PHO_INFO_STATISTICS = "statistics";
    private final static String VERIFY_MTK_CHA_INFO_TAG = "verify_mtk_cha";
    private final static String VERIFY_MTK_CHA_INFO_LEVEL = "qulity_level";
    private final static String VERIFY_MTK_CHA_INFO_STATISTICS = "statistics";
    private final static String FACE_DETECTION_INFO_TAG = "face_detections";
    private final static String FACE_DETECTION_INFO_LEFT = "left";
    private final static String FACE_DETECTION_INFO_TOP = "top";
    private final static String FACE_DETECTION_INFO_RIGHT = "right";
    private final static String FACE_DETECTION_INFO_BOTTOM = "bottom";
    private final static String FACE_DETECTION_INFO_RIP = "rotation-in-plane";

    private int mFaceRectCount = -1;
    private int mMainCamPostion = -1;
    private int mOrientation = -1;
    private int mViewWidth = -1;
    private int mViewHeight = -1;
    private int mTouchCoordX1st = -1;
    private int mTouchCoordY1st = -1;
    private int mPosX = -1;
    private int mPosY = -1;
    private int mMaskWidth = -1;
    private int mMaskHeight = -1;
    private int mMaskSize = -1;
    private int mJpsWidth = -1;
    private int mJpsHeight = -1;

    private JsonParser mParser;

    public StereoDebugInfoParser(String jsonString) {
        mParser = new JsonParser(jsonString);
    }

    public StereoDebugInfoParser(byte[] jsonBuffer) {
        mParser = new JsonParser(jsonBuffer);
    }

    public int getJpsWidth() {
        if (mJpsWidth != -1) {
            return mJpsWidth;
        }
        mJpsWidth = mParser.getValueIntFromObject(JPSINFO_TAG, null, JPSINFO_WIDTH);
        Log.d(TAG, "<getJpsWidth> mJpsWidth: " + mJpsWidth);
        return mJpsWidth;
    }

    public int getJpsHeight() {
        if (mJpsHeight != -1) {
            return mJpsHeight;
        }
        mJpsHeight = mParser.getValueIntFromObject(JPSINFO_TAG, null, JPSINFO_HEIGHT);
        Log.d(TAG, "<getJpsHeight> mJpsHeight: " + mJpsHeight);
        return mJpsHeight;
    }

    public int getMaskWidth() {
        if (mMaskWidth != -1) {
            return mMaskWidth;
        }
        mMaskWidth = mParser.getValueIntFromObject(MASKINFO_TAG, null, MASKINFO_WIDTH);
        Log.d(TAG, "<getMaskWidth> mMaskWidth: " + mMaskWidth);
        return mMaskWidth;
    }

    public int getMaskHeight() {
        if (mMaskHeight != -1) {
            return mMaskHeight;
        }
        mMaskHeight = mParser.getValueIntFromObject(MASKINFO_TAG, null, MASKINFO_HEIGHT);
        Log.d(TAG, "<getMaskHeight> mMaskHeight: " + mMaskHeight);
        return mMaskHeight;
    }

    public int getMaskSize() {
        if (mMaskSize != -1) {
            return mMaskSize;
        }
        mMaskSize = getMaskWidth() * getMaskHeight();
        Log.d(TAG, "<getMaskSize> mMaskSize: " + mMaskSize);
        return mMaskSize;
    }

    public byte[] getMaskBuffer() {
        mMaskSize = getMaskSize();
        int[][] encodedMaskArray = mParser.getInt2DArrayFromObject(MASKINFO_TAG, MASKINFO_MASK);
        if (encodedMaskArray == null) {
            Log.d(TAG, "<getMaskBuffer> Json mask array is null, return null!!");
            return null;
        }
        return decodeMaskBuffer(encodedMaskArray, mMaskSize);
    }

    public int getPosX() {
        if (mPosX != -1) {
            return mPosX;
        }
        mPosX = mParser.getValueIntFromObject(POSINFO_TAG, null, POSINFO_X);
        Log.d(TAG, "<getPosX> mPosX: " + mPosX);
        return mPosX;
    }

    public int getPosY() {
        if (mPosY != -1) {
            return mPosY;
        }
        mPosY = mParser.getValueIntFromObject(POSINFO_TAG, null, POSINFO_Y);
        Log.d(TAG, "<getPosY> mPosY: " + mPosY);
        return mPosY;
    }

    public int getViewWidth() {
        if (mViewWidth != -1) {
            return mViewWidth;
        }
        mViewWidth = mParser.getValueIntFromObject(VIEWINFO_TAG, null, VIEWINFO_WIDTH);
        Log.d(TAG, "<getViewWidth> mViewWidth: " + mViewWidth);
        return mViewWidth;
    }

    public int getViewHeight() {
        if (mViewHeight != -1) {
            return mViewHeight;
        }
        mViewHeight = mParser.getValueIntFromObject(VIEWINFO_TAG, null, VIEWINFO_HEIGHT);
        Log.d(TAG, "<getViewHeight> mViewHeight: " + mViewHeight);
        return mViewHeight;
    }

    public int getOrientation() {
        if (mOrientation != -1) {
            return mOrientation;
        }
        mOrientation = mParser.getValueIntFromObject(ORIENTATIONINFO_TAG, null,
                ORIENTATIONINFO_ORIENTATION);
        Log.d(TAG, "<getOrientation> mOrientation: " + mOrientation);
        return mOrientation;
    }

    public int getMainCamPos() {
        if (mMainCamPostion != -1) {
            return mMainCamPostion;
        }
        mMainCamPostion = mParser.getValueIntFromObject(MAIN_CAM_POSITION_INFO_TAG, null,
                MAIN_CAM_POSITION_INFO_POSITION);
        Log.d(TAG, "<getMainCamPos> mMainCamPostion: " + mMainCamPostion);
        return mMainCamPostion;
    }

    public int getTouchCoordX1st() {
        if (mTouchCoordX1st != -1) {
            return mTouchCoordX1st;
        }
        int left = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG,
                null, TOUCH_COORD_INFO_LEFT);
        int right = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null,
                TOUCH_COORD_INFO_RIGHT);
        mTouchCoordX1st = (left + right) / 2;
        Log.d(TAG, "<getTouchCoordX1st> mTouchCoordX1st: " + mTouchCoordX1st);
        return mTouchCoordX1st;
    }

    public int getTouchCoordY1st() {
        if (mTouchCoordY1st != -1) {
            return mTouchCoordY1st;
        }
        int top = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null,
                TOUCH_COORD_INFO_TOP);
        int bottom = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null,
                TOUCH_COORD_INFO_BOTTOM);
        mTouchCoordY1st = (top + bottom) / 2;
        Log.d(TAG, "<getTouchCoordY1st> mTouchCoordY1st: " + mTouchCoordY1st);
        return mTouchCoordY1st;
    }

    public int getGeoVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_GEO_INFO_TAG, null,
                VERIFY_GEO_INFO_LEVEL);
    }

    public int[] getGeoVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_GEO_INFO_TAG,
                VERIFY_GEO_INFO_STATISTICS);
    }

    public int getPhoVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_PHO_INFO_TAG,
                null, VERIFY_PHO_INFO_LEVEL);
    }

    public int[] getPhoVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_PHO_INFO_TAG,
                VERIFY_PHO_INFO_STATISTICS);
    }

    public int getMtkChaVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_MTK_CHA_INFO_TAG, null,
                VERIFY_MTK_CHA_INFO_LEVEL);
    }

    public int[] getMtkChaVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_MTK_CHA_INFO_TAG,
                VERIFY_MTK_CHA_INFO_STATISTICS);
    }

    public int getFaceRectCount() {
        if (mFaceRectCount != -1) {
            return mFaceRectCount;
        }
        mFaceRectCount = mParser.getArrayLength(FACE_DETECTION_INFO_TAG);
        Log.d(TAG, "<getFaceRectCount> mFaceRectCount: " + mFaceRectCount);
        return mFaceRectCount;
    }

    public Rect getFaceRect(int index) {
        int left = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG,
                index,
                FACE_DETECTION_INFO_LEFT);
        int top = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG,
                index,
                FACE_DETECTION_INFO_TOP);
        int right = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG,
                index,
                FACE_DETECTION_INFO_RIGHT);
        int bottom = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG,
                index,
                FACE_DETECTION_INFO_BOTTOM);
        if (left == -1 || top == -1 || right == -1 || bottom == -1) {
            Log.d(TAG,
                    "<getFaceRect> error: left == -1 || top == -1 || right == -1 || bottom == -1");
            return null;
        }
        return new Rect(left, top, right, bottom);
    }

    public int getFaceRip(int index) {
        return mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_RIP);
    }

    private byte[] decodeMaskBuffer(int[][] encodedMaskArray, int maskSize) {
        int startIndex = 0;
        int endIndex = 0;
        byte[] maskBuffer = new byte[maskSize];
        long begin = System.currentTimeMillis();
        // clear buffer
        for (int i = 0; i < maskSize; i++) {
            maskBuffer[i] = 0;
        }
        // just set valid mask to 0xff
        for (int i = 0; i < encodedMaskArray.length; i++) {
            startIndex = encodedMaskArray[i][0];
            endIndex = startIndex + encodedMaskArray[i][1];
            if (startIndex > maskSize || startIndex < 0 || endIndex < 0 || endIndex > maskSize) {
                Log.d(TAG, "<decodeMaskBuffer> error, startIndex: " + startIndex + ", endIndex: "
                        + endIndex + ", maskSize: " + maskSize);
                return null;
            }
            for (int j = startIndex; j < endIndex; j++) {
                maskBuffer[j] = (byte) 0xff;
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "<decodeMaskBuffer> performance, decode mask costs: " + (end - begin));
        return maskBuffer;
    }
}
