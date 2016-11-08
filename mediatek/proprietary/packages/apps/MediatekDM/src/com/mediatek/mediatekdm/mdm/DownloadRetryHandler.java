package com.mediatek.mediatekdm.mdm;

/**
 * Download Retry Handler. Receives notification when a non-fatal error occurred during a download
 * session, before the Engine attempts to restore the connection.
 */
public interface DownloadRetryHandler {
    /**
     * Notify before retrying to recover a download session. Return 0 to continue with the retry
     * attempt, or an error code to abort the download session. The error code will be passed as the
     * lastError parameter of SessionStateObserver.notify() .
     *
     * @param url
     *        The url of the download object.
     * @param offset
     *        Current offset.
     * @param total
     *        Size of download object.
     * @return 0 to retry to continue download, or any error code.
     */
    int onDownloadRetry(String url, int offset, int total);
}
