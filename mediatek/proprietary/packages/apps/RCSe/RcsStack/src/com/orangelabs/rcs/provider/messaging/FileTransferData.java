package com.orangelabs.rcs.provider.messaging;

import org.gsma.joyn.ft.FileTransferLog;

import android.net.Uri;


/**
 * File transfer data constants
 *
 * @author Jean-Marc AUFFRET
 */
public class FileTransferData {
    /**
     * Database URI
     */
    static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.ft/ft");

    /**
     * Column name
     */
    public static final String KEY_ID = FileTransferLog.ID;

    /**
     * Column name
     */
    public static final String KEY_FT_ID = FileTransferLog.FT_ID;

    /**
     * Column name
     */
    public static final String KEY_CHAT_ID = FileTransferLog.CHAT_ID;

    /**
     * Column name
     */
    public static final String KEY_TIMESTAMP = FileTransferLog.TIMESTAMP;

    /**
     * Column name
     */
    public static final String KEY_TIMESTAMP_SENT = FileTransferLog.TIMESTAMP_SENT;

    /**
     * Column name
     */
    public static final String KEY_TIMESTAMP_DELIVERED = FileTransferLog.TIMESTAMP_DELIVERED;

    /**
     * Column name
     */
    public static final String KEY_TIMESTAMP_DISPLAYED = FileTransferLog.TIMESTAMP_DISPLAYED;

    /**
     * Column name
     */
    public static final String KEY_CONTACT = FileTransferLog.CONTACT_NUMBER;

    /**
     * Column name
     */
    public static final String KEY_STATUS = FileTransferLog.STATE;

    /**
     * Column name
     */
    public static final String KEY_MIME_TYPE = FileTransferLog.MIME_TYPE;

    /**
     * Column name
     */
    public static final String KEY_NAME = FileTransferLog.FILENAME;

    /**
     * Column name
     */
    public static final String KEY_SIZE = FileTransferLog.TRANSFERRED;

    /**
     * Column name
     */
    static final String KEY_TOTAL_SIZE = FileTransferLog.FILESIZE;

    /**
     * Column name
     */
    static final String KEY_DIRECTION = FileTransferLog.DIRECTION;
    /**
     * Column name
     */
    static final String KEY_MSG_ID = FileTransferLog.MSG_ID;

    /**
     * Column name
     */
    static final String KEY_FILEICON = FileTransferLog.FILEICON;

    /**
     * Column name
     */
    static final String KEY_DURATION = FileTransferLog.DURATION;

    /**
     * Column name
     */
    static final String KEY_SESSION_TYPE = FileTransferLog.SESSION_TYPE;

    /**
     * Hash Code
     */
    static final String KEY_HASH = FileTransferLog.HASH;

    // file descriptor, no need to add this column to TAPI, we use it internal.
    static final String KEY_FILE_ID = "file_id";
}
