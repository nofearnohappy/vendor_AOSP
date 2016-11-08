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
 * MediaTek Inc. (C) 2015. All rights reserved.
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
package com.mediatek.camera.util.xmp;

import android.graphics.Rect;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;

import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;

import com.mediatek.camera.util.json.StereoDebugInfoParser;
import com.mediatek.camera.util.xmp.SegmentMaskOperator.SegmentMaskInfo;
import com.mediatek.camera.util.xmp.XmpInterface.ByteArrayInputStreamExt;
import com.mediatek.camera.util.xmp.XmpInterface.ByteArrayOutputStreamExt;
import com.mediatek.camera.util.xmp.XmpInterface.Section;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class XmpOperator {
    private final static String TAG = "MtkGallery2/Xmp/XmpOperator";

    public static boolean ENABLE_BUFFER_DUMP = false;
    public static final String DUMP_PATH = Environment.getExternalStorageDirectory().getPath()
            + "/";
    public static String FILE_NAME;
    public static final String DUMP_FOLDER_NAME = "dump_jps_buffer" + "/";
    public static final String CFG_FILE_NAME = "dumpjps";
    public static final String CFG_PROPERTY_NAME = "dumpjps";

    private static final String DEFAULT_DUMP_SUBFOLDER_NAME = "IMG_XXXX";
    private static final long VERIFY_INFO_LENGTH = 48;
    private static final String GOOGLE_REFOCUS_NAMESPACE =
            "http://ns.google.com/photos/1.0/focus/";
    private static final String GOOGLE_DEPTH_NAMESPACE =
            "http://ns.google.com/photos/1.0/depthmap/";
    private static final String GOOGLE_IMAGE_NAMESPACE =
            "http://ns.google.com/photos/1.0/image/";
    private static final String MEDIATEK_IMAGE_REFOCUS_NAMESPACE =
            "http://ns.mediatek.com/refocus/jpsconfig/";

    private static final String REFOCUS_BLUR_INFINITY = "BlurAtInfinity";
    private static final String REFOCUS_FOCALDISTANCE = "FocalDistance";
    private static final String REFOCUS_FOCALPOINTX = "FocalPointX";
    private static final String REFOCUS_FOCALPOINTY = "FocalPointY";

    private static final String DEPTH_PREFIX = "GDepth";
    private static final String DEPTH_FORMAT = "Format";
    private static final String DEPTH_NEAR = "Near";
    private static final String DEPTH_FAR = "Far";
    private static final String DEPTH_MIME = "Mime";
    private static final String DEPTH_DATA = "Data";
    private static final String IMAGE_PREFIX = "GImage";

    private static final String MTK_REFOCUS_PREFIX = "MRefocus";
    private static final String JPS_WIDTH = "JpsWidth";
    private static final String JPS_HEIGHT = "JpsHeight";
    private static final String MASK_WIDTH = "MaskWidth";
    private static final String MASK_HEIGHT = "MaskHeight";
    private static final String POS_X = "PosX";
    private static final String POS_Y = "PosY";
    private static final String VIEW_WIDTH = "ViewWidth";
    private static final String VIEW_HEIGHT = "ViewHeight";
    private static final String ORIENTATION = "Orientation";
    private static final String MAIN_CAM_POS = "MainCamPos";
    private static final String TOUCH_COORDX_1ST = "TouchCoordX1st";
    private static final String TOUCH_COORDY_1ST = "TouchCoordY1st";
    // app needed parameters
    private static final String DEPTH_BUFFER_WIDTH = "DepthBufferWidth";
    private static final String DEPTH_BUFFER_HEIGHT = "DepthBufferHeight";
    private static final String XMP_DEPTH_WIDTH = "XmpDepthWidth";
    private static final String XMP_DEPTH_HEIGHT = "XmpDepthHeight";
    private static final String META_BUFFER_WIDTH = "MetaBufferWidth";
    private static final String META_BUFFER_HEIGHT = "MetaBufferHeight";
    private static final String TOUCH_COORDX_LAST = "TouchCoordXLast";
    private static final String TOUCH_COORDY_LAST = "TouchCoordYLast";
    private static final String DEPTH_OF_FIELD_LAST = "DepthOfFieldLast";
    private static final String DEPTH_BUFFER_FLAG = "DepthBufferFlag";
    private static final String XMP_DEPTH_FLAG = "XmpDepthFlag";

    private static final int SERIALIZE_EXTRA_HEAD_LEN = 0x36;
    private static final int XMP_EXTENSION_SIZE = 0xffb4 - 2;
    private static final int XMP_EXTENSION_INDEX_MAIN = 0;

    // app15 reserve 0xffff bytes
    private static final int APP15_RESERVE_LENGTH = 0xFFFF;

    private ArrayList<Section> mParsedSectionsForCamera;
    private ArrayList<Section> mParsedSectionsForGallery;
    private XMPSchemaRegistry sRegister = XMPMetaFactory.getSchemaRegistry();

    private XmpInterface mXmpInterface;
    private SegmentMaskOperator mSegmentMaskOperator;

    // add option to dump buffer
    static {
        File XmpCfg = new File(DUMP_PATH + CFG_FILE_NAME);
        if (XmpCfg.exists()) {
            ENABLE_BUFFER_DUMP = true;
        } else {
            ENABLE_BUFFER_DUMP = false;
        }
        if (!ENABLE_BUFFER_DUMP) {
            ENABLE_BUFFER_DUMP = SystemProperties.getInt(CFG_PROPERTY_NAME, 0) == 1 ? true : false;
        }
        if (ENABLE_BUFFER_DUMP) {
            makeDir(DUMP_PATH + DUMP_FOLDER_NAME);
        }
        Log.d(TAG, "ENABLE_BUFFER_DUMP: " + ENABLE_BUFFER_DUMP + ", DUMP_PATH: " + DUMP_PATH);
    }

    public static class JpsConfigInfo {
        public int jpsWidth;
        public int jpsHeight;
        public int maskWidth;
        public int maskHeight;
        public int posX;
        public int posY;
        public int viewWidth;
        public int viewHeight;
        public int orientation;
        public int mainCamPos;
        public int touchCoordX1st;
        public int touchCoordY1st;
        // verify tool info
        public byte[] verifyInfo;
        public int faceCount;
        // face detection info
        public ArrayList<FaceDetectionInfo> fdInfoArray;
        public JpsConfigInfo() {
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ConfigInfo:");
            sb.append("\n    jpsWidth = 0x"
            + Integer.toHexString(jpsWidth) + "(" + jpsWidth + ")");
            sb.append("\n    jpsHeight = 0x"
            + Integer.toHexString(jpsHeight) + "(" + jpsHeight
                    + ")");
            sb.append("\n    maskWidth = 0x"
                    + Integer.toHexString(maskWidth) + "(" + maskWidth
                    + ")");
            sb.append("\n    maskHeight = 0x"
                    + Integer.toHexString(maskHeight) + "(" + maskHeight
                    + ")");
            sb.append("\n    posX = 0x"
                    + Integer.toHexString(posX) + "(" + posX + ")");
            sb.append("\n    posY = 0x"
            + Integer.toHexString(posY) + "(" + posY + ")");
            sb.append("\n    viewWidth = 0x"
            + Integer.toHexString(viewWidth) + "(" + viewWidth
                    + ")");
            sb.append("\n    viewHeight = 0x"
                    + Integer.toHexString(viewHeight) + "(" + viewHeight
                    + ")");
            sb.append("\n    orientation = 0x"
                    + Integer.toHexString(orientation) + "("
                    + orientation + ")");
            sb.append("\n    mainCamPos = 0x"
                    + Integer.toHexString(mainCamPos) + "(" + mainCamPos
                    + ")");
            sb.append("\n    touchCoordX1st = 0x"
                    + Integer.toHexString(touchCoordX1st) + "("
                    + touchCoordX1st + ")");
            sb.append("\n    touchCoordY1st = 0x"
                    + Integer.toHexString(touchCoordY1st) + "("
                    + touchCoordY1st + ")");
            sb.append("\n    faceCount = 0x"
                    + Integer.toHexString(faceCount) + "(" + faceCount + ")");
            for (int i = 0; i < fdInfoArray.size(); i++) {
                sb.append("\n    face " + i + ": " + fdInfoArray.get(i));
            }
            return sb.toString();
        }
    }

    public static class FaceDetectionInfo {
        public int mFaceLeft;
        public int mFaceTop;
        public int mFaceRight;
        public int mFaceBottom;
        public int mFaceRip;

        public FaceDetectionInfo(int faceLeft, int faceTop, int faceRight, int faceBottom,
                int faceRip) {
            mFaceLeft = faceLeft;
            mFaceTop = faceTop;
            mFaceRight = faceRight;
            mFaceBottom = faceBottom;
            mFaceRip = faceRip;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FaceDetectionInfo:");
            sb.append("\n    mFaceLeft = " + mFaceLeft);
            sb.append("\n    mFaceTop = " + mFaceTop);
            sb.append("\n    mFaceRight = " + mFaceRight);
            sb.append("\n    mFaceBottom = " + mFaceBottom);
            sb.append("\n    mFaceRip = " + mFaceRip);
            return sb.toString();
        }
    }

    public static class DepthBufferInfo {
        // write below params to xmp main
        public boolean depthBufferFlag;
        public int depthBufferWidth;
        public int depthBufferHeight;
        public boolean xmpDepthFlag;
        public int xmpDepthWidth;
        public int xmpDepthHeight;
        public int metaBufferWidth;
        public int metaBufferHeight;
        public int touchCoordXLast;
        public int touchCoordYLast;
        public int depthOfFieldLast;
        // write below buffer to app1
        public byte[] depthData;
        public byte[] xmpDepthData;

        public DepthBufferInfo() {
            depthBufferFlag = false;
            depthBufferWidth = -1;
            depthBufferHeight = -1;
            xmpDepthFlag = false;
            xmpDepthWidth = -1;
            xmpDepthHeight = -1;
            metaBufferWidth = -1;
            metaBufferHeight = -1;
            touchCoordXLast = -1;
            touchCoordYLast = -1;
            depthOfFieldLast = -1;
            depthData = null;
            xmpDepthData = null;
        }

        public DepthBufferInfo(boolean depthBufferFlag,
                byte[] depthData, int depthBufferWidth,
                int depthBufferHeight, boolean xmpDepthFlag, byte[] xmpDepthData,
                int xmpDepthWidth, int xmpDepthHeight, int touchCoordXLast, int touchCoordYLast,
                int depthOfFieldLast) {
            this.depthBufferFlag = depthBufferFlag;
            this.depthBufferWidth = depthBufferWidth;
            this.depthBufferHeight = depthBufferHeight;
            this.xmpDepthFlag = xmpDepthFlag;
            this.xmpDepthWidth = xmpDepthWidth;
            this.xmpDepthHeight = xmpDepthHeight;
            this.touchCoordXLast = touchCoordXLast;
            this.touchCoordYLast = touchCoordYLast;
            this.depthOfFieldLast = depthOfFieldLast;
            this.depthData = depthData;
            this.xmpDepthData = xmpDepthData;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DepthBufferInfo:");
            sb.append("\n    depthBufferFlag = " + depthBufferFlag);
            sb.append("\n    xmpDepthFlag = " + xmpDepthFlag);
            sb.append("\n    depthBufferWidth = 0x" + Integer.toHexString(depthBufferWidth) + "("
                    + depthBufferWidth + ")");
            sb.append("\n    depthBufferHeight = 0x" + Integer.toHexString(depthBufferHeight) + "("
                    + depthBufferHeight + ")");
            sb.append("\n    xmpDepthWidth = 0x" + Integer.toHexString(xmpDepthWidth) + "("
                    + xmpDepthWidth + ")");
            sb.append("\n    xmpDepthHeight = 0x" + Integer.toHexString(xmpDepthHeight) + "("
                    + xmpDepthHeight + ")");
            sb.append("\n    metaBufferWidth = 0x" + Integer.toHexString(metaBufferWidth) + "("
                    + metaBufferWidth + ")");
            sb.append("\n    metaBufferHeight = 0x" + Integer.toHexString(metaBufferHeight) + "("
                    + metaBufferHeight + ")");
            sb.append("\n    touchCoordXLast = 0x" + Integer.toHexString(touchCoordXLast) + "("
                    + touchCoordXLast + ")");
            sb.append("\n    touchCoordYLast = 0x" + Integer.toHexString(touchCoordYLast) + "("
                    + touchCoordYLast + ")");
            sb.append("\n    depthOfFieldLast = 0x" + Integer.toHexString(depthOfFieldLast) + "("
                    + depthOfFieldLast + ")");
            if (depthData != null) {
                sb.append("\n    depthData length = 0x" + Integer.toHexString(depthData.length)
                        + "(" + depthData.length + ")");
            } else {
                sb.append("\n    depthData = null");
            }
            if (xmpDepthData != null) {
                sb.append("\n    xmpDepthData length = 0x"
                        + Integer.toHexString(xmpDepthData.length) + "(" + xmpDepthData.length
                        + ")");
            } else {
                sb.append("\n    xmpDepthData = null");
            }
            return sb.toString();
        }
    }

    public XmpOperator() {
        mXmpInterface = new XmpInterface(sRegister);
        mSegmentMaskOperator = new SegmentMaskOperator(mXmpInterface);
    }

    public boolean initialize(String srcFilePath) {
        File srcFile = new File(srcFilePath);
        if (!srcFile.exists()) {
            Log.d(TAG, "<initialize> " + srcFilePath + " not exists!!!");
            return false;
        }
        String fileFormat = srcFilePath.substring(srcFilePath.length() - 3, srcFilePath.length());
        Log.d(TAG, "<initialize> fileFormat " + fileFormat);
        if (!"JPG".equalsIgnoreCase(fileFormat)) {
            Log.d(TAG, "<initialize> " + srcFilePath + " is not JPG!!!");
            return false;
        }
        FILE_NAME = getFileNameFromPath(srcFilePath) + "/";
        makeDir(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME);
        if (ENABLE_BUFFER_DUMP) {
            mSegmentMaskOperator.enableDump(ENABLE_BUFFER_DUMP, DUMP_PATH + DUMP_FOLDER_NAME
                    + FILE_NAME);
        }
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        if (mParsedSectionsForGallery == null) {
            return false;
        }
        mXmpInterface.setSectionInfo(mParsedSectionsForGallery);
        mSegmentMaskOperator.setSectionInfo(mParsedSectionsForGallery);
        return true;
    }

    public void deInitialize() {
        mParsedSectionsForGallery = null;
    }

    public byte[] getJpsDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Log.d(TAG, "<getJpsDataFromJpgFile> begin!!! ");
            byte[] out = getJpsInfoFromSections(rafIn, true);
            if (ENABLE_BUFFER_DUMP) {
                mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                        + "Jps_Read.raw", out);
            }
            Log.d(TAG, "<getJpsDataFromJpgFile> end!!! ");
            return out;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getJpsDataFromJpgFile> IOException when close ", e);
            }
        }
    }

    public byte[] getJpsMaskFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Log.d(TAG, "<getJpsMaskFromJpgFile> begin!!! ");
            byte[] out = getJpsInfoFromSections(rafIn, false);
            if (ENABLE_BUFFER_DUMP) {
                mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                        + "Mask_Read.raw", out);
            }
            Log.d(TAG, "<getJpsMaskFromJpgFile> end!!! ");
            return out;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsMaskFromJpgFile> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getJpsMaskFromJpgFile> IOException when close ", e);
            }
        }
    }

    public boolean writeDepthBufferToJpg(String srcFilePath, DepthBufferInfo depthBufferInfo,
            boolean deleteJps) {
        String tempPath = srcFilePath + ".tmp";
        boolean result = writeDepthBufferToJpg(srcFilePath, tempPath, depthBufferInfo,
        /* !ENABLE_BUFFER_DUMP */false);
        // delete source file and rename new file to source file
        Log.d(TAG, "<writeDepthBufferToJpg> delete src file and rename back!!!");
        File srcFile = new File(srcFilePath);
        File outFile = new File(tempPath);
        srcFile.delete();
        outFile.renameTo(srcFile);
        Log.d(TAG, "<writeDepthBufferToJpg> refresh app sections!!!");
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        mXmpInterface.setSectionInfo(mParsedSectionsForGallery);
        return result;
    }

    public boolean writeSegmentMaskInfoToJpg(String srcFilePath, SegmentMaskInfo maskInfo) {
        return mSegmentMaskOperator.writeSegmentMaskInfoToJpg(srcFilePath, maskInfo);
    }

    public SegmentMaskInfo getSegmentMaskInfoFromFile(String filePath) {
        return mSegmentMaskOperator.getSegmentMaskInfoFromFile(filePath);
    }

    public byte[] writeJpsAndMaskAndConfigToJpgBuffer(byte[] jpgBuffer, byte[] jpsData,
            byte[] jsonBuffer) {
        return writeJpsAndMaskAndConfigToJpgBuffer(DEFAULT_DUMP_SUBFOLDER_NAME, jpgBuffer, jpsData,
                jsonBuffer);
    }

    public byte[] writeJpsAndMaskAndConfigToJpgBuffer(String fileName, byte[] jpgBuffer,
            byte[] jpsData, byte[] jsonBuffer) {
        if (jpgBuffer == null || jpsData == null || jsonBuffer == null) {
            Log.d(TAG,
                     "<writeJpsAndMaskAndConfigToJpgBuffer>" +
                     " jpgBuffer or jpsData or jsonBuffer is null!!!");
            return null;
        }
        Log.d(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> write begin!!!");
        String jsonString = new String(jsonBuffer);
        if (ENABLE_BUFFER_DUMP && fileName != null) {
            FILE_NAME = fileName + "/";
            makeDir(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME);
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME + "In.raw",
                    jpgBuffer);
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "Jps_Written.raw", jpsData);
            mXmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "Json_Written.txt", jsonString);
        }
        Log.d(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> jsonBuffer size: " + jsonBuffer.length
                + ", jsonString: " + jsonString);
        StereoDebugInfoParser parser = new StereoDebugInfoParser(jsonBuffer);
        byte[] jpsMask = parser.getMaskBuffer();
        if (jpsMask == null) {
            Log.d(TAG,
                    "<writeJpsAndMaskAndConfigToJpgBuffer> parsed jpsMask is null!!!");
            return null;
        }
        if (ENABLE_BUFFER_DUMP && fileName != null) {
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "Mask_Written.raw", jpsMask);
        }
        ByteArrayInputStreamExt is = null;
        ByteArrayOutputStreamExt os = null;
        try {
            is = new ByteArrayInputStreamExt(jpgBuffer);
            mParsedSectionsForCamera = mXmpInterface.parseAppInfoFromStream(is);
            mXmpInterface.setSectionInfo(mParsedSectionsForCamera);
            os = new ByteArrayOutputStreamExt(calcJpgOutStreamSize(mParsedSectionsForCamera,
                    jpgBuffer.length, jpsData.length, jpsMask.length));

            if (is.readUnsignedShort() != XmpInterface.SOI) {
                Log.d(TAG,
                        "<writeJpsAndMaskAndConfigToJpgBuffer> image is not begin with 0xffd8!!!");
                return null;
            }

            os.writeShort(XmpInterface.SOI);
            boolean hasWritenConfigInfo = false;
            boolean hasWritenJpsAndMask = false;
            int writenLocation = mXmpInterface.findProperLocationForXmp(mParsedSectionsForCamera);
            if (writenLocation == XmpInterface.WRITE_XMP_AFTER_SOI) {
                // means no APP1
                writeConfigToStream(os, jsonBuffer);
                hasWritenConfigInfo = true;
            }
            for (int i = 0; i < mParsedSectionsForCamera.size(); i++) {
                Section sec = mParsedSectionsForCamera.get(i);
                if (sec.isExif) {
                    mXmpInterface.writeSectionToStream(is, os, sec);
                    if (!hasWritenConfigInfo) {
                        writeConfigToStream(os, jsonBuffer);
                        hasWritenConfigInfo = true;
                    }
                } else {
                    if (!hasWritenConfigInfo) {
                        writeConfigToStream(os, jsonBuffer);
                        hasWritenConfigInfo = true;
                    }
                    // APPx must be before DQT/DHT
                    if (!hasWritenJpsAndMask
                            && (sec.marker == XmpInterface.DQT
                            || sec.marker == XmpInterface.DHT)) {
                        writeJpsAndMaskToStream(os, jpsData, jpsMask);
                        hasWritenJpsAndMask = true;
                    }
                    if (sec.isXmpMain || sec.isXmpExt || sec.isJpsData || sec.isJpsMask) {
                        // skip old jpsData and jpsMask
                        is.skip(sec.length + 2);
                    } else {
                        mXmpInterface.writeSectionToStream(is, os, sec);
                    }
                }
            }
            // write jps and mask to app15, before sos
            if (!hasWritenJpsAndMask) {
                writeJpsAndMaskToStream(os, jpsData, jpsMask);
                hasWritenJpsAndMask = true;
            }
            // write remain whole file (from SOS)
            mXmpInterface.copyToStreamWithFixBuffer(is, os);
            Log.d(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> write end!!!");
            byte[] out = os.toByteArray();
            if (ENABLE_BUFFER_DUMP && fileName != null) {
                mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                        + "Out.raw", out);
            }
            return out;
        } catch (Exception e) {
            Log.e(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> Exception ", e);
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> close IOException ", e);
            }
        }
    }

    public DepthBufferInfo getDepthBufferInfoFromFile(String filePath) {
        Log.d(TAG, "<getDepthBufferInfoFromFile> begin!!!");
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(filePath);
        DepthBufferInfo depthBufferInfo = parseDepthBufferInfo(meta);
        if (depthBufferInfo == null) {
            Log.d(TAG, "<getDepthBufferInfoFromFile> depthBufferInfo is null!!!");
            return null;
        }
        depthBufferInfo.depthData = getDepthDataFromJpgFile(filePath);
        depthBufferInfo.xmpDepthData = getXmpDepthDataFromJpgFile(filePath);
        Log.d(TAG, "<getDepthBufferInfoFromFile> " + depthBufferInfo);
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.depthData != null) {
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "DepthBuffer_Read.raw", depthBufferInfo.depthData);
        }
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.xmpDepthData != null) {
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "XMPDepthMap_Read.raw", depthBufferInfo.xmpDepthData);
        }
        if (ENABLE_BUFFER_DUMP) {
            mXmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "DepthBufferInfo_Read.txt", depthBufferInfo.toString());
        }
        Log.d(TAG, "<getDepthBufferInfoFromFile> end!!!");
        return depthBufferInfo;
    }

    public JpsConfigInfo getJpsConfigInfoFromFile(String filePath) {
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(filePath);
        JpsConfigInfo jpsConfigInfo = parseJpsConfigInfo(meta);
        Log.d(TAG, "<getJpsConfigInfoFromFile> " + jpsConfigInfo);
        return jpsConfigInfo;
    }

    private byte[] makeXmpMainData(JpsConfigInfo configInfo) {
        if (configInfo == null) {
            Log.d(TAG, "<makeXmpMainData> configInfo is null, so return null!!!");
            return null;
        }
        try {
            XMPMeta meta = XMPMetaFactory.create();
            meta = makeXmpMainDataInternal(meta, configInfo);
            byte[] bufferOutTmp = mXmpInterface.serialize(meta);

            byte[] bufferOut = new byte[bufferOutTmp.length
                                        + XmpResource.XMP_HEADER_START.length()];
            System.arraycopy(XmpResource.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                    XmpResource.XMP_HEADER_START.length());
            System.arraycopy(bufferOutTmp, 0, bufferOut
                    , XmpResource.XMP_HEADER_START.length(),
                    bufferOutTmp.length);
            return bufferOut;
        } catch (Exception e) {
            Log.e(TAG, "<writeXmpData> Exception", e);
            return null;
        }
    }

    // return buffer not include app1 tag and length
    private byte[] updateXmpMainDataWithDepthBuffer(XMPMeta meta,
            DepthBufferInfo depthBufferInfo) {
        if (depthBufferInfo == null || meta == null) {
            Log
                    .i(TAG,
                            "<updateXmpMainDataWithDepthBuffer>" +
                            " depthBufferInfo or meta is null, so return null!!!");
            return null;
        }
        try {
            mXmpInterface.registerNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MTK_REFOCUS_PREFIX);
            if (depthBufferInfo.depthBufferWidth != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        DEPTH_BUFFER_WIDTH, depthBufferInfo.depthBufferWidth);
            }
            if (depthBufferInfo.depthBufferHeight != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        DEPTH_BUFFER_HEIGHT, depthBufferInfo.depthBufferHeight);
            }
            if (depthBufferInfo.xmpDepthWidth != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        XMP_DEPTH_WIDTH, depthBufferInfo.xmpDepthWidth);
            }
            if (depthBufferInfo.xmpDepthHeight != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        XMP_DEPTH_HEIGHT, depthBufferInfo.xmpDepthHeight);
            }
            if (depthBufferInfo.metaBufferWidth != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        META_BUFFER_WIDTH, depthBufferInfo.metaBufferWidth);
            }
            if (depthBufferInfo.metaBufferHeight != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        META_BUFFER_HEIGHT, depthBufferInfo.metaBufferHeight);
            }
            if (depthBufferInfo.touchCoordXLast != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        TOUCH_COORDX_LAST, depthBufferInfo.touchCoordXLast);
            }
            if (depthBufferInfo.touchCoordYLast != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        TOUCH_COORDY_LAST, depthBufferInfo.touchCoordYLast);
            }
            if (depthBufferInfo.depthOfFieldLast != -1) {
                mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        DEPTH_OF_FIELD_LAST, depthBufferInfo.depthOfFieldLast);
            }
            if (depthBufferInfo.depthBufferFlag != mXmpInterface.getPropertyBoolean(meta,
                    MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG)) {
                mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        DEPTH_BUFFER_FLAG, depthBufferInfo.depthBufferFlag);
            }
            if (depthBufferInfo.xmpDepthFlag != mXmpInterface.getPropertyBoolean(meta,
                    MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG)) {
                mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                        XMP_DEPTH_FLAG, depthBufferInfo.xmpDepthFlag);
            }

            byte[] bufferOutTmp = mXmpInterface.serialize(meta);
            byte[] bufferOut = new byte[bufferOutTmp.length
                                        + XmpResource.XMP_HEADER_START.length()];
            System.arraycopy(XmpResource.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                    XmpResource.XMP_HEADER_START.length());
            System.arraycopy(bufferOutTmp, 0, bufferOut, XmpResource.XMP_HEADER_START.length(),
                    bufferOutTmp.length);
            return bufferOut;
        } catch (Exception e) {
            Log.e(TAG, "<updateXmpMainDataWithDepthBuffer> Exception", e);
            return null;
        }
    }

    private XMPMeta makeXmpMainDataInternal(XMPMeta meta, JpsConfigInfo configInfo) {
        if (configInfo == null || meta == null) {
            Log.d(TAG,
                    "<makeXmpMainDataInternal> error," +
                    " please make sure meta or JpsConfigInfo not null");
            return null;
        }
        mXmpInterface.registerNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MTK_REFOCUS_PREFIX);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_WIDTH,
                configInfo.jpsWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_HEIGHT,
                configInfo.jpsHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_WIDTH,
                configInfo.maskWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_HEIGHT,
                configInfo.maskHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_X,
                configInfo.posX);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_Y,
                configInfo.posY);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_WIDTH,
                configInfo.viewWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_HEIGHT,
                configInfo.viewHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, ORIENTATION,
                configInfo.orientation);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MAIN_CAM_POS,
                configInfo.mainCamPos);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_1ST,
                configInfo.touchCoordX1st);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_1ST,
                configInfo.touchCoordY1st);

        // add default value for DepthBufferInfo
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_BUFFER_WIDTH, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_BUFFER_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_WIDTH,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_HEIGHT,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_WIDTH,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                META_BUFFER_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_LAST,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_LAST,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_OF_FIELD_LAST, -1);
        mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG,
                false);
        mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG,
                false);

        // add for segment
        mXmpInterface.registerNamespace(XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.MTK_SEGMENT_PREFIX);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_MASK_WIDTH, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_MASK_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_X, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_Y, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_LEFT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_RIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_TOP, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_BOTTOM, -1);
        // write face detection info
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_FACE_COUNT, configInfo.faceCount);
        FaceDetectionInfo fdInfo = null;
        mXmpInterface.registerNamespace(XmpInterface.SEGMENT_FACE_FIELD_NS,
                XmpInterface.SEGMENT_FACE_PREFIX);
        for (int i = 0; i < configInfo.fdInfoArray.size(); i++) {
            fdInfo = configInfo.fdInfoArray.get(i);
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_LEFT, Integer.toString(fdInfo.mFaceLeft));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_TOP, Integer.toString(fdInfo.mFaceTop));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIGHT, Integer.toString(fdInfo.mFaceRight));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_BOTTOM, Integer.toString(fdInfo.mFaceBottom));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIP, Integer.toString(fdInfo.mFaceRip));
        }
        return meta;
    }

    // write to app15
    private void writeJpsAndMaskToStream(ByteArrayOutputStreamExt os,
            byte[] jpsData, byte[] jpsMask) {
        try {
            Log.d(TAG, "<writeJpsAndMaskToStream> write begin!!!");
            int totalCount = 0;
            ArrayList<byte[]> jpsAndMaskArray = makeJpsAndMaskData(jpsData, jpsMask);
            for (int i = 0; i < jpsAndMaskArray.size(); i++) {
                byte[] section = jpsAndMaskArray.get(i);
                if (section[0] == 'J' && section[1] == 'P' && section[2] == 'S'
                        && section[3] == 'D') {
                    // current section is jps data
                    totalCount = jpsData.length;
                } else if (section[0] == 'J' && section[1] == 'P' && section[2] == 'S'
                        && section[3] == 'M') {
                    // current section is jps mark
                    totalCount = jpsMask.length;
                }
                // os.writeShort(APP1);
                os.writeShort(XmpInterface.APP15);
                os.writeShort(section.length + 2 + 4);
                os.writeInt(totalCount); // 4 bytes
                os.write(section);
            }
            Log.d(TAG, "<writeJpsAndMaskToStream> write end!!!");
        } catch (Exception e) {
            Log.e(TAG, "<writeJpsAndMaskToStream> Exception", e);
        }
    }

    private void writeConfigToStream(ByteArrayOutputStreamExt os, byte[] jpsConfig) {
        try {
            Log.d(TAG, "<writeConfigToStream> write begin!!!");
            JpsConfigInfo configInfo = parseJpsConfigBuffer(jpsConfig);
            Log.d(TAG, "<writeConfigToStream> jpsConfigInfo " + configInfo);
            if (ENABLE_BUFFER_DUMP) {
                mXmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                        + "Config_Written.txt", configInfo.toString());
            }
            byte[] xmpMain = makeXmpMainData(configInfo);
            os.writeShort(XmpInterface.APP1);
            os.writeShort(xmpMain.length + 2); // need length 2 bytes
            os.write(xmpMain);
            Log.d(TAG, "<writeConfigToStream> write end!!!");
        } catch (Exception e) {
            Log.e(TAG, "<writeConfigToStream> Exception", e);
        }
    }

    private ArrayList<byte[]> makeJpsAndMaskData(byte[] jpsData, byte[] jpsMask) {
        if (jpsData == null || jpsMask == null) {
            Log.d(TAG, "<makeJpsAndMaskData> jpsData or jpsMask buffer is null!!!");
            return null;
        }

        int arrayIndex = 0;
        ArrayList<byte[]> jpsAndMaskArray = new ArrayList<byte[]>();

        for (int i = 0; i < 2; i++) {
            byte[] data = (i == 0 ? jpsData : jpsMask);
            String header = (i == 0 ? XmpResource.TYPE_JPS_DATA : XmpResource.TYPE_JPS_MASK);
            int dataRemain = data.length;
            int dataOffset = 0;
            int sectionCount = 0;

            while (header.length() + 1 + dataRemain >= XmpResource.JPS_PACKET_SIZE) {
                byte[] section = new byte[XmpResource.JPS_PACKET_SIZE];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // copy data
                System.arraycopy(data, dataOffset, section, header.length() + 1, section.length
                        - header.length() - 1);
                jpsAndMaskArray.add(arrayIndex, section);

                dataOffset += section.length - header.length() - 1;
                dataRemain = data.length - dataOffset;
                sectionCount++;
                arrayIndex++;
            }
            if (header.length() + 1 + dataRemain < XmpResource.JPS_PACKET_SIZE) {
                byte[] section = new byte[header.length() + 1 + dataRemain];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // write data
                System.arraycopy(data, dataOffset, section, header.length() + 1, dataRemain);
                jpsAndMaskArray.add(arrayIndex, section);
                arrayIndex++;
            }
        }
        return jpsAndMaskArray;
    }

    private byte[] getJpsInfoFromSections(RandomAccessFile rafIn, boolean isJpsDataOrMask) {
        try {
            Section sec = null;
            int dataLen = 0;
            // parse JPS Data or Mask length
            int i = 0;
            for (; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isJpsDataOrMask && sec.isJpsData) {
                    rafIn.seek(sec.offset + 2 + 2);
                    dataLen = rafIn.readInt();
                    break;
                }
                if (!isJpsDataOrMask && sec.isJpsMask) {
                    rafIn.seek(sec.offset + 2 + 2);
                    dataLen = rafIn.readInt();
                    break;
                }
            }
            if (i == mParsedSectionsForGallery.size()) {
                Log.d(TAG, "<getJpsInfoFromSections> can not find JPS INFO, return null");
                return null;
            }
            int app1Len = 0;
            int copyLen = 0;
            int byteOffset = 0;
            byte[] data = new byte[dataLen];

            for (i = i - 1; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isJpsDataOrMask && sec.isJpsData) {
                    rafIn.seek(sec.offset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpResource.TOTAL_LENGTH_TAG_BYTE
                            - XmpResource.TYPE_JPS_DATA.length()
                            - XmpResource.JPS_SERIAL_NUM_TAG_BYTE;
                    rafIn.skipBytes(XmpResource.TOTAL_LENGTH_TAG_BYTE
                            + XmpResource.TYPE_JPS_DATA.length()
                            + XmpResource.JPS_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
                if (!isJpsDataOrMask && sec.isJpsMask) {
                    rafIn.seek(sec.offset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpResource.TOTAL_LENGTH_TAG_BYTE
                            - XmpResource.TYPE_JPS_MASK.length()
                            - XmpResource.JPS_SERIAL_NUM_TAG_BYTE;
                    rafIn.skipBytes(XmpResource.TOTAL_LENGTH_TAG_BYTE
                            + XmpResource.TYPE_JPS_MASK.length()
                            + XmpResource.JPS_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
            }
            return data;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsInfoFromSections> IOException ", e);
            return null;
        }
    }

    private JpsConfigInfo parseJpsConfigInfo(XMPMeta xmpMeta) {
        if (xmpMeta == null) {
            Log.d(TAG, "<parseJpsConfigInfo> xmpMeta is null, return!!!");
            return null;
        }
        JpsConfigInfo configInfo = new JpsConfigInfo();
        configInfo.jpsWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_WIDTH);
        configInfo.jpsHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_HEIGHT);
        configInfo.maskWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_WIDTH);
        configInfo.maskHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_HEIGHT);
        configInfo.posX = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_X);
        configInfo.posY = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_Y);
        configInfo.viewWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_WIDTH);
        configInfo.viewHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_HEIGHT);
        configInfo.orientation = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, ORIENTATION);
        configInfo.mainCamPos = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MAIN_CAM_POS);
        configInfo.touchCoordX1st = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_1ST);
        configInfo.touchCoordY1st = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_1ST);
        // face detection info
        configInfo.faceCount = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_COUNT);
        configInfo.fdInfoArray = new ArrayList<FaceDetectionInfo>();
        for (int i = 0; i < configInfo.faceCount; i++) {
            int faceLeft = mXmpInterface
                    .getStructFieldInt(xmpMeta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                            XmpInterface.SEGMENT_FACE_STRUCT_NAME + i,
                            XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_LEFT);
            int faceTop = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_TOP);
            int faceRight = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIGHT);
            int faceBottom = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_BOTTOM);
            int faceRip = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_RIP);
            configInfo.fdInfoArray.add(i, new FaceDetectionInfo(faceLeft, faceTop, faceRight,
                    faceBottom, faceRip));
        }
        return configInfo;
    }

    private int calcJpgOutStreamSize(ArrayList<Section> sections, int jpgBufferSize,
            int jpsDataSize, int jpsMaskSize) {
        /*
         * new outPutStream length is: jpgBufferSize - (old jps data and mask
         * sections length) + (new jps data and mask sections and config length)
         */

        // calc old jps data and mask data length
        int oldJpsDataAndMskLen = 0;
        Log.d(TAG, "<calcJpgOutStreamSize> calc begin!!!");
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            if (sec.isXmpMain || sec.isXmpExt || sec.isJpsData || sec.isJpsMask) {
                oldJpsDataAndMskLen += (sec.length + 2);
            }
        }
        Log.d(TAG, "<calcJpgOutStreamSize> jpgBufferSize 0x" + Integer.toHexString(jpgBufferSize)
                + "oldJpsDataAndMskLen 0x" + Integer.toHexString(oldJpsDataAndMskLen));

        int dataSizePerSection = XmpResource.JPS_PURE_DATA_SIZE_PER_PACKET;

        // calc new jps data length
        int jpsDataPacketNum = (int) Math.ceil((double) jpsDataSize / (double) dataSizePerSection);
        int newJpsDataLen = XmpResource.JPS_PACKET_HEAD_SIZE_EXCLUDE_DATA * jpsDataPacketNum
                + jpsDataSize;
        Log.d(TAG, "<calcJpgOutStreamSize> jpsDataPacketNum 0x"
                + Integer.toHexString(jpsDataPacketNum) + ", newJpsDataLen 0x"
                + Integer.toHexString(newJpsDataLen));

        // calc new jps mask length
        int jpsMaskPacketNum = (int) Math.ceil((double) jpsMaskSize / (double) dataSizePerSection);
        int newJpsMskLen = XmpResource.JPS_PACKET_HEAD_SIZE_EXCLUDE_DATA * jpsMaskPacketNum
                + jpsMaskSize;
        Log.d(TAG, "<calcJpgOutStreamSize> jpsMaskPacketNum 0x"
                + Integer.toHexString(jpsMaskPacketNum) + ", newJpsMskLen 0x"
                + Integer.toHexString(newJpsMskLen));
        Log.d(TAG, "<calcJpgOutStreamSize> calc end!!!");
        return jpgBufferSize - oldJpsDataAndMskLen + newJpsDataLen + newJpsMskLen;
    }

    private JpsConfigInfo parseJpsConfigBuffer(byte[] configBuffer) {
        if (configBuffer == null) {
            Log.d(TAG, "<parseJpsConfigBuffer> configBuffer is null!!!");
            return null;
        }
        StereoDebugInfoParser parser = new StereoDebugInfoParser(configBuffer);
        JpsConfigInfo configInfo = new JpsConfigInfo();
        configInfo.jpsWidth = parser.getJpsWidth();
        configInfo.jpsHeight = parser.getJpsHeight();
        configInfo.maskWidth = parser.getMaskWidth();
        configInfo.maskHeight = parser.getMaskHeight();
        configInfo.posX = parser.getPosX();
        configInfo.posY = parser.getPosY();
        configInfo.viewWidth = parser.getViewWidth();
        configInfo.viewHeight = parser.getViewHeight();
        configInfo.orientation = parser.getOrientation();
        configInfo.mainCamPos = parser.getMainCamPos();
        configInfo.touchCoordX1st = parser.getTouchCoordX1st();
        configInfo.touchCoordY1st = parser.getTouchCoordY1st();
        // skip verify tool info

        // read face detection info
        configInfo.faceCount = parser.getFaceRectCount();
        configInfo.fdInfoArray = new ArrayList<FaceDetectionInfo>();
        for (int i = 0; i < configInfo.faceCount; i++) {
            Rect faceRect = parser.getFaceRect(i);
            if (faceRect != null) {
                configInfo.fdInfoArray.add(i, new FaceDetectionInfo(faceRect.left, faceRect.top,
                        faceRect.right, faceRect.bottom, parser.getFaceRip(i)));
            }
        }
        return configInfo;
    }

    private boolean writeDepthBufferToJpg(String srcFilePath, String dstFilePath,
            DepthBufferInfo depthBufferInfo, boolean deleteJps) {
        if (depthBufferInfo == null) {
            Log.d(TAG, "<writeDepthBufferToJpg> depthBufferInfo is null!!!");
            return false;
        }
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.depthData != null) {
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "DepthBuffer_Written.raw", depthBufferInfo.depthData);
        }
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.xmpDepthData != null) {
            mXmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "XMPDepthMap_Written.raw", depthBufferInfo.xmpDepthData);
        }
        if (ENABLE_BUFFER_DUMP) {
            mXmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + FILE_NAME
                    + "DepthBufferInfo_Written.txt", depthBufferInfo.toString());
        }
        Log.d(TAG, "<writeDepthBufferToJpg> write begin!!!");

        // begin to copy or replace
        RandomAccessFile rafIn = null;
        RandomAccessFile rafOut = null;
        try {
            File outFile = new File(dstFilePath);
            if (outFile.exists()) {
                outFile.delete();
            }
            outFile.createNewFile();
            rafIn = new RandomAccessFile(srcFilePath, "r");
            rafOut = new RandomAccessFile(outFile, "rw");

            if (rafIn.readUnsignedShort() != XmpInterface.SOI) {
                Log.d(TAG, "<writeDepthBufferToJpg> image is not begin with 0xffd8!!!");
                return false;
            }

            rafOut.writeShort(XmpInterface.SOI);
            boolean hasUpdateXmpMain = false;
            boolean hasWritenDepthData = false;
            XMPMeta meta = mXmpInterface.getXmpMetaFromFile(srcFilePath);
            int writenLocation = mXmpInterface.findProperLocationForXmp(mParsedSectionsForGallery);
            if (writenLocation == XmpInterface.WRITE_XMP_AFTER_SOI) {
                updateOnlyDepthInfoWoBuffer(rafOut, meta, depthBufferInfo);
                hasUpdateXmpMain = true;
            }
            boolean needUpdateDepthBuffer = depthBufferInfo.depthData != null ? true : false;
            boolean needUpdateXmpDepth = depthBufferInfo.xmpDepthData != null ? true : false;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                Section sec = mParsedSectionsForGallery.get(i);
                if (sec.isExif) {
                    mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                    if (!hasUpdateXmpMain) {
                        updateOnlyDepthInfoWoBuffer(rafOut, meta, depthBufferInfo);
                        hasUpdateXmpMain = true;
                    }
                } else {
                    if (!hasUpdateXmpMain) {
                        updateOnlyDepthInfoWoBuffer(rafOut, meta, depthBufferInfo);
                        hasUpdateXmpMain = true;
                    }
                    // APPx must be before DQT/DHT
                    if (!hasWritenDepthData
                            && (sec.marker == XmpInterface.DQT
                            || sec.marker == XmpInterface.DHT)) {
                        writeOnlyDepthBuffer(rafOut, depthBufferInfo);
                        hasWritenDepthData = true;
                    }
                    if (sec.isXmpMain || sec.isXmpExt) {
                        // delete old xmp main and ext
                        rafIn.skipBytes(sec.length + 2);
                    } else if (deleteJps && (sec.isJpsData || sec.isJpsMask)) {
                        rafIn.skipBytes(sec.length + 2);
                    } else if (needUpdateDepthBuffer && sec.isDepthData) {
                        // delete depth data
                        rafIn.skipBytes(sec.length + 2);
                    } else if (needUpdateXmpDepth && sec.isXmpDepth) {
                        // delete xmp depth
                        rafIn.skipBytes(sec.length + 2);
                    } else {
                        mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                    }
                }
            }
            // write buffer to app15
            if (!hasWritenDepthData) {
                writeOnlyDepthBuffer(rafOut, depthBufferInfo);
                hasWritenDepthData = true;
            }
            // write remain whole file (from SOS)
            mXmpInterface.copyFileWithFixBuffer(rafIn, rafOut);
            Log.d(TAG, "<writeDepthBufferToJpg> write end!!!");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "<writeDepthBufferToJpg> Exception", e);
            return false;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
                if (rafOut != null) {
                    rafOut.close();
                    rafOut = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<writeDepthBufferToJpg> raf close, IOException", e);
            }
        }
    }

    // just update depthBufferInfo(exclude
    // depthBufferInfo.depthData/xmpDepthData) to xmp main section
    private boolean updateOnlyDepthInfoWoBuffer(RandomAccessFile rafOut, XMPMeta meta,
            DepthBufferInfo depthBufferInfo) {
        if (rafOut == null || meta == null || depthBufferInfo == null) {
            Log.d(TAG, "<updateOnlyDepthInfoWoBuffer>" +
                    " input params are null, return false!!!");
            return false;
        }
        Log.d(TAG, "<updateOnlyDepthInfoWoBuffer> write begin!!!");
        // step 1: write property
        byte[] newXmpMainBuffer = updateXmpMainDataWithDepthBuffer(meta, depthBufferInfo);
        if (newXmpMainBuffer == null) {
            Log.d(TAG,
                    "<updateOnlyDepthInfoWoBuffer>" +
                    " updated xmp main data is null, return false!!!");
            return false;
        }
        try {
            rafOut.writeShort(XmpInterface.APP1);
            rafOut.writeShort(newXmpMainBuffer.length + 2);
            rafOut.write(newXmpMainBuffer);
            Log.d(TAG, "<updateOnlyDepthInfoWoBuffer> write end!!!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<updateOnlyDepthInfoWoBuffer> IOException ", e);
            return false;
        }
    }

    // just write depthBufferInfo.depthData/xmpDepthData) to app15
    private boolean writeOnlyDepthBuffer(RandomAccessFile rafOut,
            DepthBufferInfo depthBufferInfo) {
        if (rafOut == null || depthBufferInfo == null) {
            Log.d(TAG, "<writeOnlyDepthBuffer> input params are null, return false!!!");
            return false;
        }
        Log.d(TAG, "<writeOnlyDepthBuffer> write begin!!!");
        if (depthBufferInfo.depthData == null && depthBufferInfo.xmpDepthData == null) {
            Log.d(TAG,
                    "<writeOnlyDepthBuffer>" +
                    " 2 depth buffers are null, skip write depth buffer!!!");
            return true;
        }
        try {
            int totalCount = 0;
            ArrayList<byte[]> depthDataArray = makeDepthData(depthBufferInfo.depthData,
                    depthBufferInfo.xmpDepthData);
            if (depthDataArray == null) {
                Log.d(TAG,
                        "<writeOnlyDepthBuffer>" +
                        " depthDataArray is null, skip write depth buffer!!!");
                return true;
            }
            for (int i = 0; i < depthDataArray.size(); i++) {
                byte[] section = depthDataArray.get(i);
                if (section[0] == 'D' && section[1] == 'E' && section[2] == 'P'
                        && section[3] == 'T') {
                    // current section is depth buffer
                    totalCount = depthBufferInfo.depthData.length;
                    Log.d(TAG, "<writeOnlyDepthBuffer> write depthData total count: 0x"
                            + Integer.toHexString(totalCount));
                } else if (section[0] == 'X' && section[1] == 'M' && section[2] == 'P'
                        && section[3] == 'D') {
                    // current section is xmp depth buffer
                    totalCount = depthBufferInfo.xmpDepthData.length;
                    Log.d(TAG, "<writeOnlyDepthBuffer> write xmpDepthData total count: 0x"
                            + Integer.toHexString(totalCount));
                }
                rafOut.writeShort(XmpInterface.APP15);
                rafOut.writeShort(section.length + 2 + 4);
                rafOut.writeInt(totalCount); // 4 bytes
                rafOut.write(section);
            }
            Log.d(TAG, "<writeOnlyDepthBuffer> write end!!!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeOnlyDepthBuffer> IOException ", e);
            return false;
        }
    }

    private ArrayList<byte[]> makeDepthData(byte[] depthData, byte[] xmpDepthData) {
        if (depthData == null && xmpDepthData == null) {
            Log.d(TAG, "<makeDepthData> depthData and xmpDepthData are null, skip!!!");
            return null;
        }

        int arrayIndex = 0;
        ArrayList<byte[]> depthDataArray = new ArrayList<byte[]>();

        for (int i = 0; i < 2; i++) {
            if (i == 0 && depthData == null) {
                Log.d(TAG, "<makeDepthData> depthData is null, skip!!!");
                continue;
            }
            if (i == 1 && xmpDepthData == null) {
                Log.d(TAG, "<makeDepthData> xmpDepthData is null, skip!!!");
                continue;
            }
            byte[] data = (i == 0 ? depthData : xmpDepthData);
            String header = (i == 0 ? XmpResource.TYPE_DEPTH_DATA : XmpResource.TYPE_XMP_DEPTH);
            int dataRemain = data.length;
            int dataOffset = 0;
            int sectionCount = 0;

            while (header.length() + 1 + dataRemain >= XmpResource.DEPTH_PACKET_SIZE) {
                byte[] section = new byte[XmpResource.DEPTH_PACKET_SIZE];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // copy data
                System.arraycopy(data, dataOffset, section, header.length() + 1, section.length
                        - header.length() - 1);
                depthDataArray.add(arrayIndex, section);
                dataOffset += section.length - header.length() - 1;
                dataRemain = data.length - dataOffset;
                sectionCount++;
                arrayIndex++;
            }
            if (header.length() + 1 + dataRemain < XmpResource.DEPTH_PACKET_SIZE) {
                byte[] section = new byte[header.length() + 1 + dataRemain];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // write data
                System.arraycopy(data, dataOffset, section, header.length() + 1, dataRemain);
                depthDataArray.add(arrayIndex, section);
                arrayIndex++;
                // sectionCount++;
            }
        }
        return depthDataArray;
    }

    private byte[] getDepthDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            Log.d(TAG, "<getDepthDataFromJpgFile> run...");
            rafIn = new RandomAccessFile(filePath, "r");
            return getDepthDataFromSections(rafIn, true);
        } catch (IOException e) {
            Log.e(TAG, "<getDepthDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getDepthDataFromJpgFile> IOException when close ", e);
            }
        }
    }

    private byte[] getXmpDepthDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            Log.d(TAG, "<getXmpDepthDataFromJpgFile> run...");
            rafIn = new RandomAccessFile(filePath, "r");
            return getDepthDataFromSections(rafIn, false);
        } catch (IOException e) {
            Log.e(TAG, "<getXmpDepthDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getXmpDepthDataFromJpgFile> IOException when close ", e);
            }
        }
    }

    private byte[] getDepthDataFromSections(RandomAccessFile rafIn,
            boolean isDepthOrXmpDepth) {
        try {
            Section sec = null;
            int dataLen = 0;
            int i = 0;
            for (; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isDepthOrXmpDepth && sec.isDepthData) {
                    rafIn.seek(sec.offset + 2 + 2);
                    dataLen = rafIn.readInt();
                    Log.d(TAG, "<getDepthDataFromSections> type DEPTH DATA, dataLen: 0x"
                            + Integer.toHexString(dataLen));
                    break;
                }
                if (!isDepthOrXmpDepth && sec.isXmpDepth) {
                    rafIn.seek(sec.offset + 2 + 2);
                    dataLen = rafIn.readInt();
                    Log.d(TAG, "<getDepthDataFromSections> type XMP DEPTH, dataLen: 0x"
                            + Integer.toHexString(dataLen));
                    break;
                }
            }
            if (i == mParsedSectionsForGallery.size()) {
                Log.d(TAG, "<getDepthDataFromSections> can not find DEPTH INFO, return null");
                return null;
            }
            int app1Len = 0;
            int copyLen = 0;
            int byteOffset = 0;
            byte[] data = new byte[dataLen];

            for (i = i - 1; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isDepthOrXmpDepth && sec.isDepthData) {
                    rafIn.seek(sec.offset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpResource.TOTAL_LENGTH_TAG_BYTE
                            - XmpResource.TYPE_DEPTH_DATA.length()
                            - XmpResource.DEPTH_SERIAL_NUM_TAG_BYTE;
                    Log.d(TAG, "<getDepthDataFromSections> app1Len: 0x"
                            + Integer.toHexString(app1Len) + ", copyLen 0x"
                            + Integer.toHexString(copyLen));
                    rafIn.skipBytes(XmpResource.TOTAL_LENGTH_TAG_BYTE
                            + XmpResource.TYPE_DEPTH_DATA.length()
                            + XmpResource.DEPTH_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
                if (!isDepthOrXmpDepth && sec.isXmpDepth) {
                    rafIn.seek(sec.offset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpResource.TOTAL_LENGTH_TAG_BYTE
                            - XmpResource.TYPE_XMP_DEPTH.length()
                            - XmpResource.DEPTH_SERIAL_NUM_TAG_BYTE;
                    Log.d(TAG, "<getDepthDataFromSections> app1Len: 0x"
                            + Integer.toHexString(app1Len) + ", copyLen 0x"
                            + Integer.toHexString(copyLen));
                    rafIn.skipBytes(XmpResource.TOTAL_LENGTH_TAG_BYTE
                            + XmpResource.TYPE_XMP_DEPTH.length()
                            + XmpResource.DEPTH_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
            }
            return data;
        } catch (IOException e) {
            Log.e(TAG, "<getDepthDataFromSections> IOException ", e);
            return null;
        }
    }

    private DepthBufferInfo parseDepthBufferInfo(XMPMeta xmpMeta) {
        Log.d(TAG, "<parseDepthBufferInfo> xmpMeta is:" + xmpMeta);
        if (xmpMeta == null) {
            return null;
        }
        DepthBufferInfo depthBufferInfo = new DepthBufferInfo();
        depthBufferInfo.depthBufferWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_WIDTH);
        depthBufferInfo.depthBufferHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_HEIGHT);
        depthBufferInfo.xmpDepthWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_WIDTH);
        depthBufferInfo.xmpDepthHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_HEIGHT);
        depthBufferInfo.metaBufferWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_WIDTH);
        depthBufferInfo.metaBufferHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_HEIGHT);
        depthBufferInfo.touchCoordXLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_LAST);
        depthBufferInfo.touchCoordYLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_LAST);
        depthBufferInfo.depthOfFieldLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_OF_FIELD_LAST);
        depthBufferInfo.depthBufferFlag = mXmpInterface.getPropertyBoolean(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG);
        depthBufferInfo.xmpDepthFlag = mXmpInterface.getPropertyBoolean(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG);
        return depthBufferInfo;
    }

    private String getFileNameFromPath(String filePath) {
        if (filePath == null)
            return null;
        int start = filePath.lastIndexOf("/");
        if (start < 0 || start > filePath.length())
            return filePath;
        return filePath.substring(start);
    }

    private static void makeDir(String filePath) {
        if (filePath == null)
            return;
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
}
