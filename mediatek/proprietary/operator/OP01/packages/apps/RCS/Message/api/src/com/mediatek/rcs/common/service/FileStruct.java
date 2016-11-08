package com.mediatek.rcs.common.service;

import org.gsma.joyn.ft.FileTransfer;
import java.util.Date;
import java.util.Set;
import java.io.File;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.UUID;

/**
 * This class represents a file structure used to be shared
 */
public class FileStruct {
    public static final String TAG = "FileStruct";

    /**
     * Generate a file struct instance using a session and a path, this
     * method should only be called for Received File Transfer
     * @param fileTransferSession The session of the file transfer
     * @param filePath The path of the file
     * @return The file struct instance
     * @throws RemoteException
     */
    public static FileStruct from(FileTransfer fileTransferObject, boolean isBurn,String filePath)
            throws Exception {
        FileStruct fileStruct = null;
        String fileName = null;
        try{
            fileName = fileTransferObject.getFileName();
            filePath = fileName;
            long fileSize = fileTransferObject.getFileSize();
            String fileTransferId = fileTransferObject.getTransferId();
            Date date = new Date();
                Log.v(TAG, "FileStruct thumbnail supported" + fileTransferObject.getFileIconName());
            fileStruct = new FileStruct(filePath,
                fileName,
                fileSize,
                fileTransferId,
                date,
                isBurn,
                fileTransferObject.getRemoteContact(),
                fileTransferObject.getFileIconName());
        }
        catch(Exception e){
        }
        return fileStruct;
    }

    /**
     * Generate a file struct instance using a path, this method should only
     * be called for Sent File Transfer
     *
     * @param filePath The path of the file
     * @return The file struct instance
     */
    public static FileStruct from(String filePath) {
        FileStruct fileStruct = null;
        File file = new File(filePath);
        if (file.exists()) {
            Date date = new Date();
            fileStruct =
                    new FileStruct(filePath, file.getName(), file.length(), new ParcelUuid(UUID
                            .randomUUID()).toString(), date);
        }
        Log.d(TAG, "from() fileStruct: " + fileStruct);
        return fileStruct;
    }

    public FileStruct(String filePath, String name, long size, String fileTransferTag, Date date) {
        mFilePath = filePath;
        mName = name;
        mSize = size;
        mFileTransferTag = fileTransferTag;
        mDate = (Date) date.clone();
    }

    public FileStruct(String filePath, String name, long size, String fileTransferTag,
            Date date, String remote) {
        this(filePath, name, size, fileTransferTag, date);
        mRemote = remote;
    }

    public FileStruct(String filePath, String name, long size, String fileTransferTag,
            Date date,boolean isBurn, String remote, String thumbNail) {
        this(filePath, name, size, fileTransferTag, date, remote);
        mSessionType = isBurn;
        mThumbnail = thumbNail;
    }

    public FileStruct(String filePath, String name, long size, String fileTransferTag,
            Date date, String remote, String thumbNail, boolean burnType, int duration) {
        this(filePath, name, size, fileTransferTag, date, burnType, remote, thumbNail);
        mDuration = duration;
    }

    public FileStruct(String filePath, String name, long size, String fileTransferTag,
            Date date, Set<String> remotes, String thumbNail, int duration) {
        this(filePath, name, size, fileTransferTag, date);
        mRemotes = remotes;
        mThumbnail = thumbNail;
        mDuration = duration;
    }

    public String mFilePath = null;

    public String mThumbnail = null;

    public String mName = null;

    public long mSize = -1;

    public String mFileTransferTag = null;

    public Date mDate = null;

    public String mRemote = null;

    public Set<String> mRemotes = null;

    public boolean mSessionType = false; // message or burn message
    public int mDuration = 0;
    public String toString() {
        return TAG + "file path is " + mFilePath + " file name is " + mName + " size is "
                + mSize + " FileTransferTag is " + mFileTransferTag + " date is " + mDate;
    }
}

