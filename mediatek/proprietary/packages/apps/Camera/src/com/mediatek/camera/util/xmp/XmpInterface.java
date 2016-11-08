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

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.properties.XMPProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class XmpInterface {
    private final static String TAG = "MtkGallery2/Xmp/XmpInterface";

    public static final String MEDIATEK_SEGMENT_NAMESPACE = "http://ns.mediatek.com/segment/";
    public static final String MTK_SEGMENT_PREFIX = "MSegment";
    public static final String SEGMENT_MASK_WIDTH = "SegmentMaskWidth";
    public static final String SEGMENT_MASK_HEIGHT = "SegmentMaskHeight";
    public static final String SEGMENT_X = "SegmentX";
    public static final String SEGMENT_Y = "SegmentY";
    public static final String SEGMENT_LEFT = "SegmentLeft";
    public static final String SEGMENT_TOP = "SegmentTop";
    public static final String SEGMENT_RIGHT = "SegmentRight";
    public static final String SEGMENT_BOTTOM = "SegmentBottom";

    public static final String SEGMENT_FACE_STRUCT_NAME = "FDInfo";
    public static final String SEGMENT_FACE_FIELD_NS = "FD";
    public static final String SEGMENT_FACE_PREFIX = "FD";
    public static final String SEGMENT_FACE_LEFT = "FaceLeft";
    public static final String SEGMENT_FACE_TOP = "FaceTop";
    public static final String SEGMENT_FACE_RIGHT = "FaceRight";
    public static final String SEGMENT_FACE_BOTTOM = "FaceBottom";
    public static final String SEGMENT_FACE_RIP = "FaceRip";
    public static final String SEGMENT_FACE_COUNT = "FaceCount";

    public static final int SOI = 0xFFD8;
    public static final int SOS = 0xFFDA;
    public static final int APP1 = 0xFFE1;
    public static final int APP15 = 0xFFEF;
    public static final int DQT = 0xFFDB;
    public static final int DHT = 0xFFC4;

    public final static int WRITE_XMP_AFTER_SOI = 0;
    public final static int WRITE_XMP_BEFORE_FIRST_APP1 = 1;
    public final static int WRITE_XMP_AFTER_FIRST_APP1 = 2;
    public final static int FIXED_BUFFER_SIZE = 1024 * 10; // 10KB

    private ArrayList<Section> mParsedSectionsForGallery;
    private XMPSchemaRegistry mRegister;

    public static class ByteArrayInputStreamExt extends ByteArrayInputStream {
        public ByteArrayInputStreamExt(byte[] buf) {
            super(buf);
            Log.d(TAG, "<ByteArrayInputStreamExt> new instance, buf count 0x"
                    + Integer.toHexString(buf.length));
        }

        public final int readUnsignedShort() {
            int hByte = read();
            int lByte = read();
            return hByte << 8 | lByte;
        }

        // high byte first int
        public final int readInt() {
            int firstByte = read();
            int secondByte = read();
            int thirdByte = read();
            int forthByte = read();
            return firstByte << 24 | secondByte << 16 | thirdByte << 8 | forthByte;
        }

        // low byte first int
        public final int readReverseInt() {
            int forthByte = read();
            int thirdByte = read();
            int secondByte = read();
            int firstByte = read();
            return firstByte << 24 | secondByte << 16 | thirdByte << 8 | forthByte;
        }

        public void seek(long offset) throws IOException {
            if (offset > count - 1)
                throw new IOException("offset out of buffer range: offset " + offset
                        + ", buffer count " + count);
            pos = (int) offset;
        }

        public long getFilePointer() {
            return pos;
        }

        public int read(byte[] buffer) {
            return read(buffer, 0, buffer.length);
        }
    }

    public static class ByteArrayOutputStreamExt extends ByteArrayOutputStream {
        public ByteArrayOutputStreamExt(int size) {
            super(size);
        }

        public final void writeShort(int val) {
            int hByte = val >> 8;
            int lByte = val & 0xff;
            write(hByte);
            write(lByte);
        }

        public final void writeInt(int val) {
            int firstByte = val >> 24;
            int secondByte = (val >> 16) & 0xff;
            int thirdByte = (val >> 8) & 0xff;
            int forthByte = val & 0xff;
            write(firstByte);
            write(secondByte);
            write(thirdByte);
            write(forthByte);
        }
    }

    public static class Section {
        int marker; // e.g. 0xffe1, exif
        long offset; // marker offset from start of file
        int length; // app length, follow spec, include 2 length bytes
        boolean isXmpMain;
        boolean isXmpExt;
        boolean isExif;
        boolean isJpsData;
        boolean isJpsMask;
        boolean isDepthData;
        boolean isXmpDepth;
        boolean isSegmentMask;

        public Section(int marker, long offset, int length, boolean isXmpMain, boolean isXmpExt,
                boolean isExif, boolean isJpsData, boolean isJpsMask, boolean isDepthData,
                boolean isXmpDepth, boolean isSegmentMask) {
            this.marker = marker;
            this.offset = offset;
            this.length = length;
            this.isXmpMain = isXmpMain;
            this.isXmpExt = isXmpExt;
            this.isExif = isExif;
            this.isJpsData = isJpsData;
            this.isJpsMask = isJpsMask;
            this.isDepthData = isDepthData;
            this.isXmpDepth = isXmpDepth;
            this.isSegmentMask = isSegmentMask;
        }
    }

    public XmpInterface(XMPSchemaRegistry register) {
        mRegister = register;
    }

    public void setSectionInfo(ArrayList<Section> sections) {
        mParsedSectionsForGallery = sections;
    }

    public void copyFileWithFixBuffer(RandomAccessFile rafIn, RandomAccessFile rafOut) {
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        long lastReadPosition = 0;
        try {
            while ((readCount = rafIn.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    rafOut.write(readBuffer);
                } else {
                    rafOut.write(readBuffer, 0, readCount);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "<copyFileWithFixBuffer> IOException", e);
        }
    }

    public XMPMeta getXmpMetaFromFile(String filePath) {
        File srcFile = new File(filePath);
        if (!srcFile.exists()) {
            Log.d(TAG, "<getXmpMetaFromFile> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        XMPMeta meta = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Section sec = null;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.isXmpMain) {
                    rafIn.seek(sec.offset + 2);
                    int len = rafIn.readUnsignedShort() - 2;
                    int xmpLen = len - XmpResource.XMP_HEADER_START.length();
                    byte[] buffer = new byte[xmpLen];
                    rafIn.skipBytes(XmpResource.XMP_HEADER_START.length());
                    rafIn.read(buffer, 0, buffer.length);
                    meta = XMPMetaFactory.parseFromBuffer(buffer);
                    if (meta == null) {
                        Log.d(TAG, "<getXmpMetaFromFile> parsed XMPMeta is null, create one!!!");
                        meta = XMPMetaFactory.create();
                    } else {
                        Log.d(TAG, "<getXmpMetaFromFile> return parsed XMPMeta");
                    }
                    return meta;
                }
            }
            meta = XMPMetaFactory.create();
            Log.d(TAG, "<getXmpMetaFromFile> no xmp main, then create XMPMeta!!!");
            return meta;
        } catch (IOException e) {
            Log.e(TAG, "<getXmpMetaFromFile> IOException ", e);
            return null;
        } catch (XMPException e) {
            Log.e(TAG, "<getXmpMetaFromFile> XMPException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<getXmpMetaFromFile> IOException when close ", e);
            }
        }
    }

    public void copyToStreamWithFixBuffer(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os) {
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        long lastReadPosition = 0;
        try {
            Log.d(TAG, "<copyToStreamWithFixBuffer> copy remain jpg data begin!!!");
            while ((readCount = is.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    os.write(readBuffer);
                } else {
                    os.write(readBuffer, 0, readCount);
                }
            }
            Log.d(TAG, "<copyToStreamWithFixBuffer> copy remain jpg data end!!!");
        } catch (Exception e) {
            Log.e(TAG, "<copyToStreamWithFixBuffer> Exception", e);
        }
    }

    public void writeSectionToFile(RandomAccessFile rafIn, RandomAccessFile rafOut, Section sec) {
        try {
            rafOut.writeShort(sec.marker);
            rafOut.writeShort(sec.length);

            rafIn.seek(sec.offset + 4);
            byte[] buffer = null;
            buffer = new byte[sec.length - 2];
            rafIn.read(buffer, 0, buffer.length);
            rafOut.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToFile> IOException", e);
        }
    }

    public void writeSectionToStream(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os,
            Section sec) {
        try {
            Log.d(TAG, "<writeSectionToStream> write section 0x" + Integer.toHexString(sec.marker));
            os.writeShort(sec.marker);
            os.writeShort(sec.length);
            is.seek(sec.offset + 4);
            byte[] buffer = null;
            buffer = new byte[sec.length - 2];
            is.read(buffer, 0, buffer.length);
            os.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToStream> IOException", e);
        }
    }

    public void registerNamespace(String nameSpace, String prefix) {
        try {
            mRegister.registerNamespace(nameSpace, prefix);
        } catch (XMPException e) {
            Log.e(TAG, "<registerNamespace> XMPException", e);
        }
    }

    public void setPropertyString(XMPMeta meta, String nameSpace, String propName, String value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyString> meta is null, return!!!");
            return;
        }
        try {
            meta.setProperty(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyString> XMPException", e);
        } catch (NullPointerException e) {
            // when jpg has this propName, it will throw NullPointerException
            Log.e(TAG, "<setPropertyString> NullPointerException!!!");
        }
    }

    public void setPropertyInteger(XMPMeta meta, String nameSpace, String propName, int value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyInteger> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyInteger(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyInteger> XMPException", e);
        } catch (NullPointerException e) {
            // when jpg has this propName, it will throw NullPointerException
            Log.e(TAG, "<setPropertyInteger> NullPointerException!!!", e);
        }
    }

    public void setPropertyBoolean(XMPMeta meta, String nameSpace, String propName, boolean value) {
        if (meta == null) {
            Log.d(TAG, "<setPropertyBoolean> meta is null, return!!!");
            return;
        }
        try {
            meta.setPropertyBoolean(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyBoolean> XMPException", e);
        } catch (NullPointerException e) {
            // when jpg has this propName, it will throw NullPointerException
            Log.e(TAG, "<setPropertyBoolean> NullPointerException!!!", e);
        }
    }

    public boolean getPropertyBoolean(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyBoolean> meta is null, return false!!!");
            return false;
        }
        try {
            return meta.getPropertyBoolean(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyBoolean> XMPException", e);
            return false;
        } catch (NullPointerException e) {
            // when jpg has this propName, it will throw NullPointerException
            Log.e(TAG, "<getPropertyBoolean> NullPointerException!!!", e);
            return false;
        }
    }

    public int getPropertyInteger(XMPMeta meta, String nameSpace, String propName) {
        if (meta == null) {
            Log.d(TAG, "<getPropertyInteger> meta is null, return -1!!!");
            return -1;
        }
        try {
            return meta.getPropertyInteger(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyInteger> XMPException", e);
            return -1;
        } catch (NullPointerException e) {
            // when jpg has this propName, it will throw NullPointerException
            Log.e(TAG, "<getPropertyInteger> NullPointerException!!!", e);
            return -1;
        }
    }

    public byte[] serialize(XMPMeta meta) {
        try {
            return XMPMetaFactory.serializeToBuffer(meta, new SerializeOptions()
                    .setUseCompactFormat(true).setOmitPacketWrapper(true));
        } catch (XMPException e) {
            Log.e(TAG, "<serialize> XMPException", e);
        }
        Log.d(TAG, "<serialize> return null!!!");
        return null;
    }

    public static boolean writeBufferToFile(String desFile, byte[] buffer) {
        if (buffer == null) {
            Log.d(TAG, "<writeBufferToFile> buffer is null");
            return false;
        }
        File out = new File(desFile);
        if (out.exists())
            out.delete();
        FileOutputStream fops = null;
        try {
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeBufferToFile> createNewFile error");
                return false;
            }
            fops = new FileOutputStream(out);
            fops.write(buffer);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeBufferToFile> IOException", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "<writeBufferToFile> Exception", e);
            return false;
        } finally {
            try {
                if (fops != null) {
                    fops.close();
                    fops = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<writeBufferToFile> close, IOException", e);
            }
        }
    }

    public byte[] readFileToBuffer(String filePath) {
        File inFile = new File(filePath);
        if (!inFile.exists()) {
            Log.d(TAG, "<readFileToBuffer> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(inFile, "r");
            int len = (int) inFile.length();
            byte[] buffer = new byte[len];
            rafIn.read(buffer);
            return buffer;
        } catch (Exception e) {
            Log.e(TAG, "<readFileToBuffer> Exception ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<readFileToBuffer> close IOException ", e);
            }
        }
    }

    public void writeStringToFile(String desFile, String value) {
        if (value == null) {
            Log.d(TAG, "<writeStringToFile> input string is null, return!!!");
            return;
        }
        File out = new File(desFile);
        PrintStream ps = null;
        try {
            if (out.exists())
                out.delete();
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeStringToFile> createNewFile error");
                return;
            }
            ps = new PrintStream(out);
            ps.println(value);
            ps.flush();
        } catch (Exception e) {
            Log.e(TAG, "<writeStringToFile> Exception ", e);
        } finally {
            out = null;
            if (ps != null) {
                ps.close();
                ps = null;
            }
        }
    }

    public ArrayList<Section> parseAppInfo(String filePath) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filePath, "r");
            int value = raf.readUnsignedShort();
            if (value != SOI) {
                Log.d(TAG, "<parseAppInfo> error, find no SOI");
            }
            int marker = -1;
            long offset = -1;
            int length = -1;
            ArrayList<Section> sections = new ArrayList<Section>();

            while ((value = raf.readUnsignedShort()) != -1 && value != SOS) {
                marker = value;
                offset = raf.getFilePointer() - 2;
                length = raf.readUnsignedShort();
                sections.add(new Section(marker, offset, length, false, false, false, false, false,
                        false, false, false));
                raf.skipBytes(length - 2);
            }
            // write exif/isXmp flag
            for (int i = 0; i < sections.size(); i++) {
                checkIfXmpOrExifOrJpsInApp1(raf, sections.get(i));
                Log.d(TAG, "<parseAppInfo> marker 0x" + Integer.toHexString(sections.get(i).marker)
                        + ", offset 0x" + Long.toHexString(sections.get(i).offset) + ", length 0x"
                        + Integer.toHexString(sections.get(i).length) + ", isExif "
                        + sections.get(i).isExif + ", isXmpMain " + sections.get(i).isXmpMain
                        + ", isXmpExt " + sections.get(i).isXmpExt + ", isJPSData "
                        + sections.get(i).isJpsData + ", isJPSMask " + sections.get(i).isJpsMask
                        + ", isDepthData " + sections.get(i).isDepthData + ", isXmpDepth "
                        + sections.get(i).isXmpDepth + ", isSegmentMask "
                        + sections.get(i).isSegmentMask);
            }
            return sections;
        } catch (IOException e) {
            Log.e(TAG, "<parseAppInfo> IOException, path " + filePath, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "<parseAppInfo> Exception, path " + filePath, e);
            return null;
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<parseAppInfo> IOException, path " + filePath, e);
            }
        }
    }

    public ArrayList<Section> parseAppInfoFromStream(ByteArrayInputStreamExt is) {
        if (is == null) {
            Log.d(TAG, "<parseAppInfoFromStream> input stream is null!!!");
            return null;
        }
        try {
            is.seek(0); // reset position at the file start
            int value = is.readUnsignedShort();
            if (value != SOI) {
                Log.d(TAG, "<parseAppInfoFromStream> error, find no SOI");
            }
            Log.d(TAG, "<parseAppInfoFromStream> parse begin!!!");
            int marker = -1;
            long offset = -1;
            int length = -1;
            ArrayList<Section> sections = new ArrayList<Section>();

            while ((value = is.readUnsignedShort()) != -1 && value != SOS) {
                marker = value;
                offset = is.getFilePointer() - 2;
                length = is.readUnsignedShort();
                sections.add(new Section(marker, offset, length, false, false, false, false, false,
                        false, false, false));
                is.skip(length - 2);
            }

            // write exif/isXmp flag
            for (int i = 0; i < sections.size(); i++) {
                checkIfXmpOrExifOrJpsInStream(is, sections.get(i));
                Log.d(TAG, "<parseAppInfoFromStream> marker 0x"
                        + Integer.toHexString(sections.get(i).marker) + ", offset 0x"
                        + Long.toHexString(sections.get(i).offset) + ", length 0x"
                        + Integer.toHexString(sections.get(i).length) + ", isExif "
                        + sections.get(i).isExif + ", isXmpMain " + sections.get(i).isXmpMain
                        + ", isXmpExt " + sections.get(i).isXmpExt + ", isJPSData "
                        + sections.get(i).isJpsData + ", isJPSMask " + sections.get(i).isJpsMask
                        + ", isDepthData " + sections.get(i).isDepthData + ", isXmpDepth "
                        + sections.get(i).isXmpDepth + ", isSegmentMask "
                        + sections.get(i).isSegmentMask);
            }
            is.seek(0); // reset position at the file start
            Log.d(TAG, "<parseAppInfoFromStream> parse end!!!");
            return sections;
        } catch (IOException e) {
            Log.e(TAG, "<parseAppInfoFromStream> IOException ", e);
            return null;
        }
    }

    // return marker: write xmp after this marker
    public int findProperLocationForXmp(ArrayList<Section> sections) {
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            if (sec.marker == APP1) {
                if (sec.isExif) {
                    return WRITE_XMP_AFTER_FIRST_APP1;
                } else {
                    return WRITE_XMP_BEFORE_FIRST_APP1;
                }
            }
        }
        // means no app1, write after SOI
        return WRITE_XMP_AFTER_SOI;
    }

    public static void printXMPMeta(XMPMeta meta, String title) {
        String name = meta.getObjectName();
        if (name != null && name.length() > 0) {
            Log.d(TAG, title + " (Name: '" + name + "'):");
        } else {
            Log.d(TAG, title + ":");
        }
        Log.d(TAG, meta.dumpObject());
    }

    public void setStructField(XMPMeta meta, String nameSpace, String structName, String fieldNS,
            String fieldName, String fieldValue) {
        if (meta == null) {
            Log.d(TAG, "<setStructField> meta is null, return!!!");
            return;
        }
        try {
            meta.setStructField(nameSpace, structName, fieldNS, fieldName, fieldValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setStructField> XMPException", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setStructField> NullPointerException!!!", e);
        }
    }

    public int getStructFieldInt(XMPMeta meta, String nameSpace, String structName, String fieldNS,
            String fieldName) {
        if (meta == null) {
            Log.d(TAG, "<getStructFieldInt> meta is null, return!!!");
            return -1;
        }
        try {
            XMPProperty property = meta.getStructField(nameSpace, structName, fieldNS, fieldName);
            return Integer.valueOf((String) property.getValue());
        } catch (XMPException e) {
            Log.e(TAG, "<getStructFieldInt> XMPException", e);
            return -1;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getStructFieldInt> NullPointerException!!!", e);
            return -1;
        }
    }

    /*
     * array index: start from 1, not 0
     */
    public void setArrayItem(XMPMeta meta, String nameSpace, String arrayName, int index,
            String itemValue) {
        if (meta == null) {
            Log.d(TAG, "<setArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.setArrayItem(nameSpace, arrayName, index, itemValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setArrayItem> XMPException", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setArrayItem> NullPointerException!!!", e);
        }
    }

    public void appendArrayItem(XMPMeta meta, String nameSpace, String arrayName,
            PropertyOptions arrayOption, String itemValue, PropertyOptions itmeOption) {
        if (meta == null) {
            Log.d(TAG, "<appendArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.appendArrayItem(nameSpace, arrayName, arrayOption, itemValue, itmeOption);
        } catch (XMPException e) {
            Log.e(TAG, "<appendArrayItem> XMPException", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<appendArrayItem> NullPointerException!!!", e);
        }
    }

    public void appendArrayItem(XMPMeta meta, String nameSpace,
            String arrayName, String itemValue) {
        if (meta == null) {
            Log.d(TAG, "<appendArrayItem> meta is null, return!!!");
            return;
        }
        try {
            meta.appendArrayItem(nameSpace, arrayName, itemValue);
        } catch (XMPException e) {
            Log.e(TAG, "<appendArrayItem> XMPException", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<appendArrayItem> NullPointerException!!!", e);
        }
    }

    private void checkIfXmpOrExifOrJpsInApp1(RandomAccessFile raf, Section section) {
        if (section == null) {
            Log.d(TAG, "<checkIfXmpOrExifOrJpsInApp1> section is null!!!");
            return;
        }
        byte[] buffer = null;
        String str = null;

        try {
            if (section.marker == APP15) {
                raf.seek(section.offset + 2 + 2 + 4);
                buffer = new byte[7];
                raf.read(buffer, 0, buffer.length);
                str = new String(buffer);

                if (XmpResource.TYPE_JPS_DATA.equals(str)) {
                    section.isJpsData = true;
                    return;
                }

                if (XmpResource.TYPE_JPS_MASK.equals(str)) {
                    section.isJpsMask = true;
                    return;
                }

                if (XmpResource.TYPE_DEPTH_DATA.equals(str)) {
                    section.isDepthData = true;
                    return;
                }

                if (XmpResource.TYPE_XMP_DEPTH.equals(str)) {
                    section.isXmpDepth = true;
                    return;
                }

                if (XmpResource.TYPE_SEGMENT_MASK.equals(str)) {
                    section.isSegmentMask = true;
                    return;
                }
            } else if (section.marker == APP1) {
                raf.seek(section.offset + 4);
                // main: "http://ns.adobe.com/xap/1.0/\0"
                // extension main: "http://ns.adobe.com/xmp/extension/"
                // extension slave:"http://ns.adobe.com/xmp/extension/"
                // use longest string as buffer length
                buffer = new byte[XmpResource.XMP_EXT_MAIN_HEADER1.length()];
                raf.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (XmpResource.XMP_EXT_MAIN_HEADER1.equals(str)) {
                    // ext main header is same as ext slave header
                    section.isXmpExt = true;
                    return;
                }

                str = new String(buffer, 0, XmpResource.XMP_HEADER_START.length());
                if (XmpResource.XMP_HEADER_START.equals(str)) {
                    section.isXmpMain = true;
                    return;
                }

                str = new String(buffer, 0, XmpResource.EXIF_HEADER.length());
                if (XmpResource.EXIF_HEADER.equals(str)) {
                    section.isExif = true;
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInApp1> UnsupportedEncodingException" + e);
        } catch (IOException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInApp1> IOException" + e);
        }
        // note, we need to close RandomAccessFile in the upper caller
    }

    private void checkIfXmpOrExifOrJpsInStream(ByteArrayInputStreamExt is,
            Section section) {
        if (is == null || section == null) {
            Log.d(TAG,
                    "<checkIfXmpOrExifOrJpsInStream> input stream or section is null!!!");
            return;
        }
        byte[] buffer = null;
        String str = null;

        try {
            if (section.marker == APP15) {
                is.seek(section.offset + 2 + 2 + 4);
                buffer = new byte[7];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);

                if (XmpResource.TYPE_JPS_DATA.equals(str)) {
                    section.isJpsData = true;
                    return;
                }

                if (XmpResource.TYPE_JPS_MASK.equals(str)) {
                    section.isJpsMask = true;
                    return;
                }

                if (XmpResource.TYPE_DEPTH_DATA.equals(str)) {
                    section.isDepthData = true;
                    return;
                }

                if (XmpResource.TYPE_XMP_DEPTH.equals(str)) {
                    section.isXmpDepth = true;
                    return;
                }

                if (XmpResource.TYPE_SEGMENT_MASK.equals(str)) {
                    section.isSegmentMask = true;
                    return;
                }
            } else if (section.marker == APP1) {
                is.seek(section.offset + 4);
                // main: "http://ns.adobe.com/xap/1.0/\0"
                // extension main: "http://ns.adobe.com/xmp/extension/"
                // extension slave:"http://ns.adobe.com/xmp/extension/"
                // use longest string as buffer length
                buffer = new byte[XmpResource.XMP_EXT_MAIN_HEADER1.length()];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (XmpResource.XMP_EXT_MAIN_HEADER1.equals(str)) {
                    // ext main header is same as ext slave header
                    section.isXmpExt = true;
                    return;
                }

                str = new String(buffer, 0, XmpResource.XMP_HEADER_START.length());
                if (XmpResource.XMP_HEADER_START.equals(str)) {
                    section.isXmpMain = true;
                    return;
                }

                str = new String(buffer, 0, XmpResource.EXIF_HEADER.length());
                if (XmpResource.EXIF_HEADER.equals(str)) {
                    section.isExif = true;
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInStream> UnsupportedEncodingException" + e);
        } catch (IOException e) {
            Log.e(TAG, "<checkIfXmpOrExifOrJpsInStream> IOException" + e);
        }
        // note, we need to close RandomAccessFile in the upper caller
    }

    private int locateXmpDataEnd(byte[] buffer) {
        int i = buffer.length - 1;
        for (; i > 3; i--) {
            if (buffer[i] == '>' && buffer[i - 1] == 'a' && buffer[i - 2] == 't'
                    && buffer[i - 3] == 'e' && buffer[i - 4] == 'm') {
                return i + 1;
            }
        }
        Log.d(TAG, "<locateXmpDataEnd> error, can not find XmpDataEnd!!!");
        return -1;
    }
}
