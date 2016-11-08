package com.hesine.nmsg.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

public class FileEx {
    private static final long AVALIABLE_SPACE = 5 * 1024 * 1024;

    public static int getFileSize(String filepath) {
        if (TextUtils.isEmpty(filepath)) {
            return -1;
        }
        File file = new File(filepath);
        return (int) file.length();
    }

    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    deleteFile(f);
                }
            }
            return file.delete();
        }

        return true;
    }

    public static boolean isFileExisted(String filepath) {
        if (TextUtils.isEmpty(filepath)) {
            return false;
        }
        File file = new File(filepath);
        return file.exists();
    }

    public static void write(String path, byte[] bytes) {
        try {
            File file;
            FileOutputStream out;
            // file = new File(path);
            // if (!file.exists()) {
            // file.mkdirs();
            // }
            file = null;
            String dir = path.substring(0, path.lastIndexOf("/"));
            file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
            String name = path.substring(path.lastIndexOf("/") + 1);
            file = new File(dir, name);
            file.createNewFile();
            out = new FileOutputStream(file);
            out.write(bytes);
            out.flush();
            out.close();
            out = null;
            file = null;
        } catch (IOException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    deleteFile(f);
                }
            }
            return file.delete();
        }

        return true;
    }

    public static boolean renameFile(String sourcePath, String destPath) {
        File sfile = new File(sourcePath);
        File dfile = new File(destPath);
        return sfile.renameTo(dfile);
    }

    public static String getSDCardPath() {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();

        if (TextUtils.isEmpty(sdStatus)) {
            return EnumConstants.STORAGE_PATH;
        }

        boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }

        return EnumConstants.STORAGE_PATH;
    }

    public static boolean getSDCardStatus() {
        boolean ret = false;
        String sdStatus = Environment.getExternalStorageState();

        if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            ret = true;
        }
        return ret;
    }

    public static byte[] readFile(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte[] buffer = new byte[length];
        fis.read(buffer);
        fis.close();
        fis = null;
        file = null;
        return buffer;
    }

    @SuppressWarnings("deprecation")
    private static long getSDcardAvailableSpace() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            if (sdcardDir == null) {
                return 0;
            }
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();
            return availCount * blockSize; // "Byte"
        } else {
            return 0;
        }

    }

    @SuppressWarnings("deprecation")
    private static long getDataStorageAvailableSpace() {
        File root = Environment.getRootDirectory();
        StatFs sf = new StatFs(root.getPath());
        long blockSize = sf.getBlockSize();
        long availCount = sf.getAvailableBlocks();
        return availCount * blockSize;
    }

    public static boolean getStorageFullStatus() {
        long sizeSD = getSDcardAvailableSpace();
        long sizeData = getDataStorageAvailableSpace();

        if (sizeSD <= AVALIABLE_SPACE && sizeData <= AVALIABLE_SPACE) {
            return true;
        }

        return false;
    }
}
