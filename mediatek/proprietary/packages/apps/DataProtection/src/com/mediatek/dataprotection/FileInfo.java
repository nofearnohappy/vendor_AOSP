package com.mediatek.dataprotection;

import java.io.File;

import android.media.MediaFile;
import android.provider.MediaStore;

import com.mediatek.dataprotection.utils.FeatureOptionsUtils;
import com.mediatek.dataprotection.utils.FileUtils;

public class FileInfo {

    public static final int IMAGE_FILE = 1;
    public static final int VIDEO_FILE = 2;
    public static final int AUDIO_FILE = 3;
    public static final int UNKOWN_FILE = 4;

    public static final String MIME_TYPE_IMAGE = "image";
    public static final String MIME_TYPE_AUDIO = "audio";
    public static final String MIME_TYPE_VIDEO = "video";
    // public static final String MIME_TYPE_IMAGE = "image";

    public static final String COLUMN_PATH = MediaStore.Files.FileColumns.DATA;
    public static final String COLUMN_LAST_MODIFIED = MediaStore.Files.FileColumns.DATE_MODIFIED;
    public static final String COLUMN_SIZE = MediaStore.Files.FileColumns.SIZE;
    public static final String COLUMN_IS_DIRECTORY = "format";
    public static final String COLUMN_MIME_TYPE = MediaStore.Files.FileColumns.MIME_TYPE;
    public static final String COLUMN_IS_DRM = MediaStore.MediaColumns.IS_DRM;

    public static final String ENCRYPT_FILE_EXTENSION = "mudp";
    private static final String EXT_DRM_CONTENT = "dcf";

    private String mPath;
    private File mFile;
    private boolean mIsChecked = false;
    private boolean mIsDirectory = false;
    private boolean mIsShowTemp = true;
    private long mLastModified = 0;
    private long mSize = 0;
    private String mMimeType = null;

    public FileInfo(String path, File file) {
        setPath(path);
        setFile(file);
        mIsDirectory = file.isDirectory();
        mLastModified = file.lastModified();
        if (!file.isDirectory()) {
            mSize = file.length();
        }
    }

    public FileInfo(String path, boolean isDirectory, long lastModified,
            long size, String mimeType) {
        mPath = path;
        mIsDirectory = isDirectory;
        mLastModified = lastModified;
        mSize = size;
        mMimeType = mimeType;
        mFile = new File(path);
    }

    public FileInfo(File file) {
        setPath(file.getAbsolutePath());
        setFile(file);
        mIsDirectory = file.isDirectory();
        mLastModified = file.lastModified();
        if (!file.isDirectory()) {
            mSize = file.length();
        }
    }

    public FileInfo(String path, long modify, long size) {
        mPath = path;
        mLastModified = modify;
        mSize = size;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public long getFileSize() {
        return mSize;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean mIsChecked) {
        this.mIsChecked = mIsChecked;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }

    public File getFile() {
        return mFile;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public String getShowName() {

        return FileUtils.getFileName(getShowPath());
    }

    public String getLockedFileShowName() {
        String name = mFile.getName();
        if (name != null && !name.isEmpty()) {
            if (name.startsWith(".")) {
                // name.replace(".", newChar)
                name = name.substring(1);
            }
            if (name.endsWith("." + FileInfo.ENCRYPT_FILE_EXTENSION)) {
                name = name.substring(0, name.lastIndexOf("."
                        + FileInfo.ENCRYPT_FILE_EXTENSION));
            }
        }
        return name;
    }

    public String getFileAbsolutePath() {
        if (null != mFile) {
            return mFile.getAbsolutePath();
        } else if (mPath != null) {
            return mPath;
        }
        return "";
    }

    public String getShowPath() {
        return MountPointManager.getInstance().getDescriptionPath(
                getFileAbsolutePath());
    }

    public boolean isDirectory() {
        return mIsDirectory;
    }

    public int getFileType() {
        String filePath = mFile.getAbsolutePath();
        int sepIndex = filePath.lastIndexOf(".");
        String ext = null;
        if (sepIndex != -1) {
            ext = filePath.substring(sepIndex + 1);
        }

        String mimeType = MediaFile.getMimeTypeForFile(filePath);
        String subFolder = "/.decrypted/";
        if (mimeType != null) {
            String type = mimeType.substring(0, mimeType.indexOf("/"));

            if (type.equalsIgnoreCase("image")) {
                return IMAGE_FILE;
            } else if (type.equalsIgnoreCase("video")) {
                return VIDEO_FILE;
            } else if (type.equalsIgnoreCase("audio")) {
                return AUDIO_FILE;
            } else {
                return UNKOWN_FILE;
            }
        } else {
            return UNKOWN_FILE;
        }
    }

    public boolean isNeedToShow() {
        return this.mIsShowTemp;
    }

    public void hiddenEncryptFile(boolean res) {
        mIsShowTemp = res;
    }

    public String getFileName() {
        return mFile.getName();
    }

    /**
     * The method check the file is DRM file, or not.
     *
     * @return true for DRM file, false for not DRM file.
     */
    public boolean isDrmFile() {
        if (mIsDirectory) {
            return false;
        }
        return isDrmFile(mPath);
    }

    /**
     * This static method check a file is DRM file, or not.
     *
     * @param fileName
     *            the file which need to be checked.
     * @return true for DRM file, false for not DRM file.
     */
    public static boolean isDrmFile(String fileName) {
        if (FeatureOptionsUtils.isMtkDrmApp()) {
            String extension = FileUtils.getFileExtension(fileName);
            if (extension != null
                    && extension.equalsIgnoreCase(EXT_DRM_CONTENT)) {
                return true; // all drm files cannot be copied
            }
        }
        return false;
    }
}
