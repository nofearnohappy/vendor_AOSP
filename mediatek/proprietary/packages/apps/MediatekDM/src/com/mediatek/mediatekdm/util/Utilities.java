package com.mediatek.mediatekdm.util;

import android.os.Environment;
import android.os.FileUtils;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utilities {
    private static final long MIN_STORAGE_NEEDED = 1 * 1024 * 1024; // 1MB

    /**
     * Dump data in hex format to android log.
     *
     * @param data
     */
    public static void hexdump(byte[] data) {
        final int rowBytes = 16;
        final int rowQtr1 = 3;
        final int rowHalf = 7;
        final int rowQtr2 = 11;
        int rows;
        int residue;
        int i;
        int j;
        byte[] saveBuf = new byte[rowBytes + 2];
        char[] hexBuf = new char[4];
        char[] idxBuf = new char[8];
        char[] hexChars = new char[20];

        hexChars[0] = '0';
        hexChars[1] = '1';
        hexChars[2] = '2';
        hexChars[3] = '3';
        hexChars[4] = '4';
        hexChars[5] = '5';
        hexChars[6] = '6';
        hexChars[7] = '7';
        hexChars[8] = '8';
        hexChars[9] = '9';
        hexChars[10] = 'A';
        hexChars[11] = 'B';
        hexChars[12] = 'C';
        hexChars[13] = 'D';
        hexChars[14] = 'E';
        hexChars[15] = 'F';

        rows = data.length >> 4;
        residue = data.length & 0x0000000F;
        for (i = 0; i < rows; i++) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * rowBytes);
            idxBuf[0] = hexChars[((hexVal >> 12) & 15)];
            idxBuf[1] = hexChars[((hexVal >> 8) & 15)];
            idxBuf[2] = hexChars[((hexVal >> 4) & 15)];
            idxBuf[3] = hexChars[(hexVal & 15)];

            String idxStr = new String(idxBuf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < rowBytes; j++) {
                saveBuf[j] = data[(i * rowBytes) + j];

                hexBuf[0] = hexChars[(saveBuf[j] >> 4) & 0x0F];
                hexBuf[1] = hexChars[saveBuf[j] & 0x0F];

                sb.append(hexBuf[0]);
                sb.append(hexBuf[1]);
                sb.append(' ');

                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }

                if (saveBuf[j] < 0x20 || saveBuf[j] > 0x7E) {
                    saveBuf[j] = (byte) '.';
                }
            }

            String saveStr = new String(saveBuf, 0, j);
            sb.append(" | " + saveStr + " |");
            Log.d(TAG.COMMON, sb.toString());
        }

        if (residue > 0) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * rowBytes);
            idxBuf[0] = hexChars[((hexVal >> 12) & 15)];
            idxBuf[1] = hexChars[((hexVal >> 8) & 15)];
            idxBuf[2] = hexChars[((hexVal >> 4) & 15)];
            idxBuf[3] = hexChars[(hexVal & 15)];

            String idxStr = new String(idxBuf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < residue; j++) {
                saveBuf[j] = data[(i * rowBytes) + j];

                hexBuf[0] = hexChars[(saveBuf[j] >> 4) & 0x0F];
                hexBuf[1] = hexChars[saveBuf[j] & 0x0F];

                sb.append(hexBuf[0]);
                sb.append(hexBuf[1]);
                sb.append(' ');

                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }

                if (saveBuf[j] < 0x20 || saveBuf[j] > 0x7E) {
                    saveBuf[j] = (byte) '.';
                }
            }

            for ( /* j INHERITED */; j < rowBytes; j++) {
                saveBuf[j] = (byte) ' ';
                sb.append("   ");
                if (j == rowQtr1 || j == rowHalf || j == rowQtr2) {
                    sb.append(" ");
                }
            }

            String saveStr = new String(saveBuf, 0, j);
            sb.append(" | " + saveStr + " |");
            // output
            Log.d(TAG.COMMON, sb.toString());
        }
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to
     * handlers, please use this method instead.
     *
     * @param what
     * @param arg1
     * @param arg2
     * @param obj
     * @return
     */
    public static Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        return msg;
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to
     * handlers, please use this method instead.
     *
     * @param what
     * @return
     */
    public static Message obtainMessage(int what) {
        Message msg = new Message();
        msg.what = what;
        return msg;
    }

    /**
     * For the messages which will be used for FUMO or SCOMO listeners and will never be sent to
     * handlers, please use this method instead.
     *
     * @param what
     * @param obj
     * @return
     */
    public static Message obtainMessage(int what, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        return msg;
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlock = stat.getAvailableBlocksLong();
        // Reserve MIN_STORAGE_NEEDED for minimum requirements of DM application
        long availableSize = (availableBlock * blockSize) - MIN_STORAGE_NEEDED;
        return availableSize;
    }

    public static void removeDirectoryRecursively(File directory) {
        Log.w(TAG.COMMON, "+removeDirectoryRecursively(" + directory + ")");
        File[] children = directory.listFiles();
        for (File child : children) {
            if (child.isFile()) {
                Log.d(TAG.COMMON, "Remove file " + child);
                child.delete();
            } else if (child.isDirectory()) {
                removeDirectoryRecursively(child);
                child.delete();
            }
        }
        Log.w(TAG.COMMON, "-removeDirectoryRecursively()");
    }

    public static void copyFile(File src, File dst) {
        Log.i(TAG.COMMON, "Copy from " + src + " to " + dst);
        final int bufferSize = 1024 * 50;
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[bufferSize];
            while (true) {
                int ins = in.read(buffer);
                if (ins == -1) {
                    in.close();
                    out.flush();
                    out.close();
                    Log.i(TAG.COMMON, "there is no more data");
                    break;
                } else {
                    out.write(buffer, 0, ins);
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }

    public static void openPermission(String dir) {
        FileUtils.setPermissions(dir, FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IXOTH,
                -1, -1);
    }

    public static final byte[] EMPTY_DATA = new byte[] { '0' };

    public static void addLeafNodeChecked(MdmTree tree, String nodeUri, String format, String type,
            byte[] data) throws MdmException {
        if (data == null || data.length == 0) {
            data = EMPTY_DATA;
        }
        tree.addLeafNode(nodeUri, format, type, data);
    }
}
