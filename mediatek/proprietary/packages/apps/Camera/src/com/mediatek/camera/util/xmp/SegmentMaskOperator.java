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

import android.util.Log;

import com.adobe.xmp.XMPMeta;

import com.mediatek.camera.util.xmp.XmpInterface.Section;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class SegmentMaskOperator {
    private final static String TAG = "MtkGallery2/Xmp/SegmentMaskOperator";
    private boolean mEnableDump;
    private String mDumpPath;
    private XmpInterface mXmpInterface;
    private ArrayList<Section> mParsedSectionsForGallery;

    public static class SegmentMaskInfo {
        public int mMaskWidth;
        public int mMaskHeight;
        public int mSegmentX;
        public int mSegmentY;
        public int mSegmentLeft;
        public int mSegmentTop;
        public int mSegmentRight;
        public int mSegmentBottom;
        public byte[] mMaskBuffer;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SegmentMaskInfo:");
            sb.append("\n    mMaskWidth = 0x" + Integer.toHexString(mMaskWidth) + "(" + mMaskWidth
                    + ")");
            sb.append("\n    mMaskHeight = 0x" + Integer.toHexString(mMaskHeight) + "("
                    + mMaskHeight + ")");
            sb.append("\n    mSegmentX = 0x" + Integer.toHexString(mSegmentX) + "(" + mSegmentX
                    + ")");
            sb.append("\n    mSegmentY = 0x" + Integer.toHexString(mSegmentY) + "(" + mSegmentY
                    + ")");
            sb.append("\n    mSegmentLeft = 0x" + Integer.toHexString(mSegmentLeft) + "("
                    + mSegmentLeft + ")");
            sb.append("\n    mSegmentTop = 0x" + Integer.toHexString(mSegmentTop) + "("
                    + mSegmentTop + ")");
            sb.append("\n    mSegmentRight = 0x" + Integer.toHexString(mSegmentRight) + "("
                    + mSegmentRight + ")");
            sb.append("\n    mSegmentBottom = 0x" + Integer.toHexString(mSegmentBottom) + "("
                    + mSegmentBottom + ")");
            String str2 = null;
            if (mMaskBuffer != null) {
                sb.append("\n    mMaskBuffer length = 0x"
                        + Integer.toHexString(mMaskBuffer.length)
                        + "(" + mMaskBuffer.length + ")");
            } else {
                sb.append("\n    mMaskBuffer = null");
            }
            return sb.toString();
        }
    }

    public SegmentMaskOperator(XmpInterface xmpInterface) {
        mXmpInterface = xmpInterface;
        mXmpInterface.registerNamespace(XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.MTK_SEGMENT_PREFIX);
    }

    public void enableDump(boolean enable, String dumpPath) {
        mEnableDump = enable;
        mDumpPath = dumpPath;
    }

    public void setSectionInfo(ArrayList<Section> sections) {
        mParsedSectionsForGallery = sections;
    }

    public boolean writeSegmentMaskInfoToJpg(String srcFilePath,
            SegmentMaskInfo maskInfo) {
        String tempPath = srcFilePath + ".tmp";
        boolean result = writeSegmentMaskInfoToJpg(srcFilePath, tempPath, maskInfo);
        // delete source file and rename new file to source file
        Log.d(TAG, "<writeSegmentMaskInfoToJpg> delete src file and rename back!!!");
        File srcFile = new File(srcFilePath);
        File outFile = new File(tempPath);
        srcFile.delete();
        outFile.renameTo(srcFile);
        Log.d(TAG, "<writeSegmentMaskInfoToJpg> refresh app sections!!!");
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        mXmpInterface.setSectionInfo(mParsedSectionsForGallery);
        return result;
    }

    public SegmentMaskInfo getSegmentMaskInfoFromFile(String filePath) {
        Log.d(TAG, "<getSegmentMaskInfoFromFile> begin!!!");
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(filePath);
        SegmentMaskInfo segmentMaskInfo = parseSegmentMaskInfo(meta);
        if (segmentMaskInfo == null) {
            Log.d(TAG, "<getSegmentMaskInfoFromFile> segmentMaskInfo is null!!!");
            return null;
        }
        segmentMaskInfo.mMaskBuffer = getSegmentMaskFromJpgFile(filePath);
        if (segmentMaskInfo.mMaskBuffer == null) {
            Log.d(TAG, "<getSegmentMaskInfoFromFile> segmentMaskInfo.mMaskBuffer is null!!!");
            return null;
        }
        Log.d(TAG, "<getSegmentMaskInfoFromFile> " + segmentMaskInfo);
        if (mEnableDump && segmentMaskInfo.mMaskBuffer != null) {
            mXmpInterface.writeBufferToFile(mDumpPath + "SegmentMaskBuffer_Read.raw",
                    segmentMaskInfo.mMaskBuffer);
        }
        if (mEnableDump) {
            mXmpInterface.writeStringToFile(mDumpPath + "SegmentMaskInfo_Read.txt",
                    segmentMaskInfo.toString());
        }
        Log.d(TAG, "<getSegmentMaskInfoFromFile> end!!!");
        return segmentMaskInfo;
    }

    private SegmentMaskInfo parseSegmentMaskInfo(XMPMeta xmpMeta) {
        Log.d(TAG, "<parseSegmentMaskInfo> xmpMeta is:" + xmpMeta);
        if (xmpMeta == null) {
            return null;
        }
        SegmentMaskInfo segmentMaskInfo = new SegmentMaskInfo();
        segmentMaskInfo.mMaskWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_MASK_WIDTH);
        segmentMaskInfo.mMaskHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_MASK_HEIGHT);
        segmentMaskInfo.mSegmentX = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_X);
        segmentMaskInfo.mSegmentY = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_Y);
        segmentMaskInfo.mSegmentLeft = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_LEFT);
        segmentMaskInfo.mSegmentTop = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_TOP);
        segmentMaskInfo.mSegmentRight = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_RIGHT);
        segmentMaskInfo.mSegmentBottom = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_BOTTOM);
        return checkSegmentMaskInfoIfValid(segmentMaskInfo);
    }

    private SegmentMaskInfo checkSegmentMaskInfoIfValid(SegmentMaskInfo info) {
        if (info.mMaskWidth == -1 || info.mMaskHeight == -1 || info.mSegmentX == -1
                || info.mSegmentY == -1 || info.mSegmentLeft == -1 || info.mSegmentTop == -1
                || info.mSegmentRight == -1 || info.mSegmentBottom == -1) {
            Log.d(TAG,
                    "<checkSegmentMaskInfoIfValid> invalid SegmentMaskInfo, return null!!");
            return null;
        }
        return info;
    }

    private byte[] getSegmentMaskFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            Log.d(TAG, "<getSegmentMaskFromJpgFile> run...");
            rafIn = new RandomAccessFile(filePath, "r");
            return getSegmentMaskBufferFromSections(rafIn);
        } catch (IOException e) {
            Log.d(TAG, "<getSegmentMaskFromJpgFile> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getSegmentMaskFromJpgFile> IOException when close ", e);
            }
        }
    }

    private byte[] getSegmentMaskBufferFromSections(RandomAccessFile rafIn) {
        try {
            Section sec = null;
            int dataLen = 0;
            int i = 0;
            for (; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.isSegmentMask) {
                    rafIn.seek(sec.offset + 2 + 2);
                    dataLen = rafIn.readInt();
                    Log.d(TAG,
                            "<getSegmentMaskBufferFromSections> type SEGMASK, dataLen: 0x"
                            + Integer.toHexString(dataLen));
                    break;
                }
            }
            if (i == mParsedSectionsForGallery.size()) {
                Log.d(TAG,
                        "<getSegmentMaskBufferFromSections> can not find SEGMASK return null");
                return null;
            }
            int app1Len = 0;
            int copyLen = 0;
            int byteOffset = 0;
            byte[] data = new byte[dataLen];

            for (i = i - 1; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.isSegmentMask) {
                    rafIn.seek(sec.offset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpResource.TOTAL_LENGTH_TAG_BYTE
                            - XmpResource.TYPE_SEGMENT_MASK.length()
                            - XmpResource.SEGMENT_SERIAL_NUM_TAG_BYTE;
                    Log.d(TAG, "<getSegmentMaskBufferFromSections> app1Len: 0x"
                            + Integer.toHexString(app1Len) + ", copyLen 0x"
                            + Integer.toHexString(copyLen));
                    rafIn.skipBytes(XmpResource.TOTAL_LENGTH_TAG_BYTE
                            + XmpResource.TYPE_SEGMENT_MASK.length()
                            + XmpResource.SEGMENT_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
            }
            return data;
        } catch (IOException e) {
            Log.d(TAG, "<getSegmentMaskBufferFromSections> IOException ", e);
            return null;
        }
    }

    private boolean writeSegmentMaskInfoToJpg(String srcFilePath, String dstFilePath,
            SegmentMaskInfo maskInfo) {
        if (maskInfo == null) {
            Log.d(TAG, "<writeSegmentMaskInfoToJpg> segmentMaskInfo is null!!!");
            return false;
        }
        if (mEnableDump && maskInfo.mMaskBuffer != null) {
            mXmpInterface.writeBufferToFile(mDumpPath + "SegmentMaskBuffer_Written.raw",
                    maskInfo.mMaskBuffer);
        }
        if (mEnableDump) {
            mXmpInterface.writeStringToFile(mDumpPath + "SegmentMaskInfo_Written.txt",
                    maskInfo.toString());
        }
        Log.d(TAG, "<writeSegmentMaskInfoToJpg> write begin!!!");

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
                Log.d(TAG, "<writeSegmentMaskInfoToJpg> image is not begin with 0xffd8!!!");
                return false;
            }

            rafOut.writeShort(XmpInterface.SOI);
            boolean hasUpdateXmpMain = false;
            boolean hasWritenSegmentMaskData = false;
            XMPMeta meta = mXmpInterface.getXmpMetaFromFile(srcFilePath);
            int writenLocation =
                    mXmpInterface.findProperLocationForXmp(mParsedSectionsForGallery);
            if (writenLocation == XmpInterface.WRITE_XMP_AFTER_SOI) {
                updateOnlySegmentMaskInfoWoBuffer(rafOut, meta, maskInfo);
                hasUpdateXmpMain = true;
            }
            boolean needUpdateMaskBuffer = maskInfo.mMaskBuffer != null ? true : false;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                Section sec = mParsedSectionsForGallery.get(i);
                if (sec.isExif) {
                    mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                    if (!hasUpdateXmpMain) {
                        updateOnlySegmentMaskInfoWoBuffer(rafOut, meta, maskInfo);
                        hasUpdateXmpMain = true;
                    }
                } else {
                    if (!hasUpdateXmpMain) {
                        updateOnlySegmentMaskInfoWoBuffer(rafOut, meta, maskInfo);
                        hasUpdateXmpMain = true;
                    }
                    // APPx must be before DQT/DHT
                    if (!hasWritenSegmentMaskData
                            && (sec.marker == XmpInterface.DQT ||
                            sec.marker == mXmpInterface.DHT)) {
                        writeOnlySegmentMaskBuffer(rafOut, maskInfo);
                        hasWritenSegmentMaskData = true;
                    }
                    if (sec.isXmpMain || sec.isXmpExt) {
                        // delete old xmp main and ext
                        rafIn.skipBytes(sec.length + 2);
                    } else if (needUpdateMaskBuffer && sec.isSegmentMask) {
                        // delete depth data
                        rafIn.skipBytes(sec.length + 2);
                    } else {
                        mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                    }
                }
            }
            // write buffer to app15
            if (!hasWritenSegmentMaskData) {
                writeOnlySegmentMaskBuffer(rafOut, maskInfo);
                hasWritenSegmentMaskData = true;
            }
            // write remain whole file (from SOS)
            mXmpInterface.copyFileWithFixBuffer(rafIn, rafOut);
            Log.d(TAG, "<writeSegmentMaskInfoToJpg> write end!!!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "<writeSegmentMaskInfoToJpg> Exception", e);
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
                Log.e(TAG, "<writeSegmentMaskInfoToJpg> raf close, IOException", e);
            }
        }
    }

    private boolean updateOnlySegmentMaskInfoWoBuffer(RandomAccessFile rafOut, XMPMeta meta,
            SegmentMaskInfo maskInfo) {
        if (rafOut == null || meta == null || maskInfo == null) {
            Log.d(TAG,
                    "<updateOnlySegmentMaskInfoWoBuffer> input params are null, return false!!!");
            return false;
        }
        Log.d(TAG, "<updateOnlySegmentMaskInfoWoBuffer> write begin!!!");
        // step 1: write property
        byte[] newXmpMainBuffer = updateXmpMainDataWithSegmentMaskInfo(meta, maskInfo);
        if (newXmpMainBuffer == null) {
            Log.d(TAG,
                    "<updateOnlySegmentMaskInfoWoBuffer> " +
                    "updated xmp main data is null, return false!!!");
            return false;
        }
        try {
            rafOut.writeShort(XmpInterface.APP1);
            rafOut.writeShort(newXmpMainBuffer.length + 2);
            rafOut.write(newXmpMainBuffer);
            Log.d(TAG, "<updateOnlySegmentMaskInfoWoBuffer> write end!!!");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "<updateOnlySegmentMaskInfoWoBuffer> IOException ", e);
            return false;
        }
    }

    private byte[] updateXmpMainDataWithSegmentMaskInfo(XMPMeta meta, SegmentMaskInfo maskInfo) {
        if (maskInfo == null || meta == null) {
            Log.d(TAG,
                    "<updateXmpMainDataWithSegmentMaskInfo> " +
                    "maskInfo or meta is null, so return null!!!");
            return null;
        }
        try {
            mXmpInterface.registerNamespace(XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.MTK_SEGMENT_PREFIX);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_MASK_WIDTH, maskInfo.mMaskWidth);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_MASK_HEIGHT, maskInfo.mMaskHeight);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_X, maskInfo.mSegmentX);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_Y, maskInfo.mSegmentY);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_LEFT, maskInfo.mSegmentLeft);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_TOP, maskInfo.mSegmentTop);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_RIGHT, maskInfo.mSegmentRight);
            mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_BOTTOM, maskInfo.mSegmentBottom);

            byte[] bufferOutTmp = mXmpInterface.serialize(meta);
            byte[] bufferOut = new byte[bufferOutTmp.length
                                        + XmpResource.XMP_HEADER_START.length()];
            System.arraycopy(XmpResource.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                    XmpResource.XMP_HEADER_START.length());
            System.arraycopy(bufferOutTmp, 0, bufferOut, XmpResource.XMP_HEADER_START.length(),
                    bufferOutTmp.length);
            return bufferOut;
        } catch (Exception e) {
            Log.d(TAG, "<updateXmpMainDataWithSegmentMaskInfo> Exception", e);
            return null;
        }
    }

    private boolean writeOnlySegmentMaskBuffer(RandomAccessFile rafOut, SegmentMaskInfo maskInfo) {
        if (rafOut == null || maskInfo == null) {
            Log.d(TAG, "<writeOnlySegmentMaskBuffer> input params are null, return false!!!");
            return false;
        }
        Log.d(TAG, "<writeOnlySegmentMaskBuffer> write begin!!!");
        if (maskInfo.mMaskBuffer == null) {
            Log.d(TAG,
                    "<writeOnlySegmentMaskBuffer> mMaskBuffer is null," +
                    " skip write mMaskBuffer!!!");
            return true;
        }
        try {
            int totalCount = 0;
            ArrayList<byte[]> segmentMaskArray = makeSegmentMaskData(maskInfo.mMaskBuffer);
            if (segmentMaskArray == null) {
                Log.d(TAG,
                        "<writeOnlySegmentMaskBuffer>" +
                        " segmentMaskArray is null, skip write mMaskBuffer!!!");
                return true;
            }
            for (int i = 0; i < segmentMaskArray.size(); i++) {
                byte[] section = segmentMaskArray.get(i);
                if (section[0] == 'S' && section[1] == 'E' && section[2] == 'G'
                        && section[3] == 'M') {
                    // current section is depth buffer
                    totalCount = maskInfo.mMaskBuffer.length;
                    Log.d(TAG,
                            "<writeOnlySegmentMaskBuffer> write mMaskBuffer total count: 0x"
                            + Integer.toHexString(totalCount));
                };
                rafOut.writeShort(XmpInterface.APP15);
                rafOut.writeShort(section.length + 2 + 4);
                rafOut.writeInt(totalCount);
                rafOut.write(section);
            }
            Log.d(TAG, "<writeOnlySegmentMaskBuffer> write end!!!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeOnlySegmentMaskBuffer> IOException ", e);
            return false;
        }
    }

    private ArrayList<byte[]> makeSegmentMaskData(byte[] maskData) {
        if (maskData == null) {
            Log.d(TAG, "<makeSegmentMaskData> maskData is null, skip!!!");
            return null;
        }

        int arrayIndex = 0;
        ArrayList<byte[]> maskDataArray = new ArrayList<byte[]>();

        byte[] data = maskData;
        String header = XmpResource.TYPE_SEGMENT_MASK;
        int dataRemain = data.length;
        int dataOffset = 0;
        int sectionCount = 0;

        while (header.length() + 1 + dataRemain >= XmpResource.SEGMENT_MASK_PACKET_SIZE) {
            byte[] section = new byte[XmpResource.SEGMENT_MASK_PACKET_SIZE];
            // copy type
            System.arraycopy(header.getBytes(), 0, section, 0, header.length());
            // write section number
            section[header.length()] = (byte) sectionCount;
            // copy data
            System.arraycopy(data, dataOffset, section, header.length() + 1, section.length
                    - header.length() - 1);
            maskDataArray.add(arrayIndex, section);
            dataOffset += section.length - header.length() - 1;
            dataRemain = data.length - dataOffset;
            sectionCount++;
            arrayIndex++;
        }
        if (header.length() + 1 + dataRemain < XmpResource.SEGMENT_MASK_PACKET_SIZE) {
            byte[] section = new byte[header.length() + 1 + dataRemain];
            // copy type
            System.arraycopy(header.getBytes(), 0, section, 0, header.length());
            // write section number
            section[header.length()] = (byte) sectionCount;
            // write data
            System.arraycopy(data, dataOffset, section, header.length() + 1, dataRemain);
            maskDataArray.add(arrayIndex, section);
            arrayIndex++;
        }
        return maskDataArray;
    }
}
