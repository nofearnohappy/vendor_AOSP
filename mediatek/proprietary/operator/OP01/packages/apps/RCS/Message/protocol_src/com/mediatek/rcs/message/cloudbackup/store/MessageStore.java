package com.mediatek.rcs.message.cloudbackup.store;

import android.content.Context;
import android.util.Log;

import com.mediatek.rcs.message.cloudbackup.store.imap.ImapConstants;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapElement;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapList;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapResponse;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapString;
import com.mediatek.rcs.message.cloudbackup.utils.IOUtils;
import com.mediatek.rcs.message.cloudbackup.utils.MessagingException;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener.UserCancelException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * IMAP API.
 *
 */
public class MessageStore {
    private static final boolean DEBUG = true;
    private static final String TAG = "RcsBR/MessageStore";
    private Context mContext;
    private ImapStore mImapStore;
    private ImapConnection mConnection;

    /**
     * Constructor.
     * @param context .
     */
    public MessageStore(Context context) {
        mContext = context;
    }

    /**
     * Constructor.
     * @param context .
     * @param user .
     * @param token .
     * @throws MessagingException .
     */
    public MessageStore(Context context, String user, String token)
            throws MessagingException {
        this(context);
        mImapStore = ImapStore.newInstance(mContext, user, token);
        mConnection = mImapStore.getConnection();
    }

    private ImapConnection getConnection() {
        ImapConnection connection;
        synchronized (this) {
            if (mConnection == null) {
                connection = mImapStore.getConnection();
            } else {
                connection = mConnection;
            }
        }

        return connection;
    }

    /**
     * login.
     * @return command result.
     */
    public CmdResultCode login() {
        CmdResultCode res = CmdResultCode.ERROR;
        ImapConnection connection = getConnection();

        if (connection != null) {
            try {
                connection.doLogin();
                res = CmdResultCode.OK;
            } catch (MessagingException me) {
                me.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return res;
    }

    /**
     * @return command result.
     */
    public CmdResultCode logout() {
        CmdResultCode res = CmdResultCode.ERROR;
        if (mConnection != null) {
            try {
                mConnection.doLogout();
                res = CmdResultCode.OK;
            } catch (MessagingException me) {
                me.printStackTrace();
            }
        }
        return res;
    }

    private CmdResultCode execute(String cmd) {
        Log.d(TAG, "execute " + cmd);
        ImapConnection connection = getConnection();

        try {
            connection.executeSimpleCommand(cmd);
            return CmdResultCode.OK;
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(connection, ioe);
        } finally {
            connection.destroyResponses();
            if (mConnection == null) {
                mImapStore.poolConnection(connection);
            }
        }
        return CmdResultCode.ERROR;
    }

    /**
     * Create path on cloud.
     *
     * @param path
     *            path to be created.
     * @return command result.
     */
    public CmdResultCode create(String path) {
        return execute(String.format(Locale.US, ImapConstants.CREATE + " %s", path));
    }

    /**
     * Get contents of a remote folder.
     *
     * @param path remote path.
     * @return list of the content.
     */
    public List<String> list(String path) {
        ImapConnection connection = getConnection();

        List<ImapResponse> responseList;
        try {
            responseList = connection.executeSimpleCommand(ImapConstants.LIST + " \"" + path
                                                           + "\" *");
            List<String> content = new ArrayList<String>();
            for (ImapResponse response : responseList) {
                if (response.isDataResponse(0, ImapConstants.LIST)) {
                    content.add(response.getStringOrEmpty(2).getString()
                            + response.getStringOrEmpty(3).getString());
                }
            }

            return content;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(connection, ioe);
        } finally {
            connection.destroyResponses();
            if (mConnection == null) {
                mImapStore.poolConnection(connection);
            }
        }

        return null;
    }

    /**
     * Select specified path.
     *
     * @param path remote path to select.
     * @return entry count on success and -1 on failure.
     */
    public int select(String path) {
        int ret = -1;

        ImapConnection connection = getConnection();
        try {
            String cmd = String.format(Locale.US, ImapConstants.SELECT + " %s", path);
            List<ImapResponse> responses = connection.executeSimpleCommand(cmd);
            for (ImapResponse r : responses) {
                if (r.isDataResponse(1, ImapConstants.EXISTS)) {
                    ret = r.getStringOrEmpty(0).getNumberOrZero();
                }
            }
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(connection, ioe);
        } finally {
            connection.destroyResponses();
            if (mConnection == null) {
                mImapStore.poolConnection(connection);
            }
        }

        return ret;
    }

    /**
     * Close remote folder.
     * @return command result.
     * @throws MessagingException .
     */
    public CmdResultCode close() throws MessagingException {
        return execute(ImapConstants.CLOSE);
    }

    /**
     * close transport connection.
     */
    public void closeConnection() {
        ImapConnection connection = getConnection();
        if (connection != null) {
            connection.destroyResponses();
            connection.close();
        }
    }

    /**
     * @param fileName load file to upload.
     * @param remotePath remote path to upload.
     * @param l  listener.
     * @return command result.
     */
    public CmdResultCode append(String fileName, String remotePath, ProgressListener l) {
        ImapConnection connection = getConnection();
        if (connection == null) {
            return CmdResultCode.ERROR;
        }

        boolean noTimeout = true;
        try {
            File tempFile = new File(fileName); // todo:
            long byteCount = tempFile.length(); // todo:

            connection.sendCommand(String.format(Locale.US,
                    ImapConstants.APPEND + " \"%s\" (%s) {%d}", remotePath,
                    ImapConstants.FLAG_SEEN, byteCount), false);
            ImapResponse response;
            do {
                final int socketTimeout = connection.mTransport.getSoTimeout();
                try {
                    // Need to set the timeout to unlimited since we might be
                    // upsyncing a pretty
                    // big attachment so who knows how long it'll take. It would
                    // sure be nice
                    // if this only timed out after the send buffer drained but
                    // welp.
                    if (noTimeout) {
                        // For now, only unset the timeout if we're doing a
                        // manual sync
                        connection.mTransport.setSoTimeout(0);
                    }

                    response = connection.readResponse();
                    if (response.isContinuationRequest()) {
                        final OutputStream transportOutputStream =
                            connection.mTransport.getOutputStream();
                        InputStream inputStream = new FileInputStream(tempFile);
                        if (inputStream != null) {
                            IOUtils.copyLarge(inputStream, transportOutputStream, l);
                            transportOutputStream.write('\r');
                            transportOutputStream.write('\n');
                            transportOutputStream.flush();
                            inputStream.close();
                        }
                    } else if (!response.isTagged()) {
                        // handleUntaggedResponse(response);
                    }
                } finally {
                    connection.mTransport.setSoTimeout(socketTimeout);
                }
            } while (!response.isTagged());

            return CmdResultCode.OK;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UserCancelException ioe) {
            return CmdResultCode.CANCELED;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(connection, ioe);
        } finally {
            connection.destroyResponses();
        }

        return CmdResultCode.ERROR;
    }

    /**
     * @param id id of the remote message.
     * @param localFileName local to save the message.
     * @param l listener.
     * @return .
     */
    public CmdResultCode fetch(String id, String localFileName, ProgressListener l) {
        ImapConnection connection = getConnection();

        if (connection == null) {
            return CmdResultCode.ERROR;
        }

        final LinkedHashSet<String> fetchFields = new LinkedHashSet<String>();
        fetchFields.add(ImapConstants.BODY);
        File bodyFile = null;
        OutputStream out = null;
        try {
            bodyFile = new File(localFileName);
            out = new FileOutputStream(bodyFile);
            connection.sendCommand(String.format(Locale.US, ImapConstants.FETCH
                    + " %s BODY", id), false);

            ImapResponse response;
            // File tempFile = new File(localFileName);

            do {
                response = null;
                // / M: set ui callback listener to network downloading.
                // RCS:IMAP porting
                response = connection.readResponse();

                if (!response.isDataResponse(1, ImapConstants.FETCH)) {
                    continue; // Ignore
                }

                String uid = response.getStringOrEmpty(0).getString();
                if (uid.compareTo(id) != 0) {
                    Log.d(TAG, uid + " is not " + id + "!");
                    continue;
                }

                final ImapList fetchList = response.getListOrEmpty(2);
                ImapString body = (ImapString) fetchList.getElementOrNone(1);
                if (body == ImapElement.NONE) {
                    Log.d(TAG, "No body data");
                    continue;
                }
                InputStream bodyStream = body.getAsStream();
                IOUtils.copyLarge(bodyStream, out, l);
            } while (!response.isTagged());
            return CmdResultCode.OK;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UserCancelException ioe) {
            return CmdResultCode.CANCELED;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(mConnection, ioe);
        } finally {
            if (mConnection != null) {
                mConnection.destroyResponses();
            }

            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return CmdResultCode.ERROR;
    }

    /**
     * Index 1 is used for server responses all the items regardless the index
     * specified.
     *
     * @param localPath local folder to save downloaded messages.
     * @param startID start id of the message.
     * @param endID end id of the message.
     * @param l listener.
     * @return .
     */
    public CmdResultCode fetch(String localPath, int startID, int endID, ProgressListener l) {
        ImapConnection connection = getConnection();

        if (connection == null) {
            return CmdResultCode.ERROR;
        }

        final LinkedHashSet<String> fetchFields = new LinkedHashSet<String>();
        fetchFields.add(ImapConstants.BODY);
        File bodyFile = null;
        OutputStream out = null;
        try {
            connection.sendCommand(
                    String.format(Locale.US, ImapConstants.FETCH + " "
                            + startID + ":" + endID + " FULL"), false);

            ImapResponse response;

            do {
                response = null;
                response = connection.readResponse(l);

                if (!response.isDataResponse(1, ImapConstants.FETCH)) {
                    continue; // Ignore
                }

                String id = response.getStringOrEmpty(0).getString();
                if (id.isEmpty()) {
                    Log.d(TAG, "id is empty!");
                    continue;
                }
                bodyFile = new File(localPath + "/" + id);
                out = new FileOutputStream(bodyFile);

                final ImapList fetchList = response.getListOrEmpty(2);
                ImapString body = (ImapString) fetchList.getElementOrNone(1);
                if (body == ImapElement.NONE) {
                    Log.d(TAG, "No body data");
                    continue;
                }

                InputStream bodyStream = body.getAsStream();
                IOUtils.copyLarge(bodyStream, out, l);
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } while (!response.isTagged());
            return CmdResultCode.OK;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UserCancelException e) {
            return CmdResultCode.CANCELED;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ioExceptionHandler(mConnection, ioe);
        } finally {
            if (mConnection != null) {
                mConnection.destroyResponses();
            }

            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return CmdResultCode.ERROR;
    }

    /**
     * result code.
     *
     */
    public enum CmdResultCode {
        OK, CANCELED, ERROR;
    }

    /**
     * name of the remote folder.
     *
     */
    public static class RemoteFolderName {
        /**
         * message backup folder.
         */
        public static final String MSG_BACKUP = "msgBackup";
        /**
         * folder for favorite message.
         */
        public static final String MSG_FAVORITE = "msgFavorite";
    }

    private MessagingException ioExceptionHandler(ImapConnection connection,
            IOException ioe) {
        if (DEBUG) {
            Log.d(TAG, "IO Exception detected: ", ioe);
        }

        connection.close();
        if (connection == mConnection) {
            synchronized (this) {
                mImapStore.poolConnection(mConnection);
                mConnection = null;
            }
        }

        return new MessagingException(MessagingException.IOERROR, "IO Error", ioe);
    }
}