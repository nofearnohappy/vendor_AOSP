package com.mediatek.rcs.message.cloudbackup.utils;

import java.io.IOException;

/**
 * Listener of the progress.
 *
 */
public interface ProgressListener {
    /**
     * exception is thrown on user cancel.
     *
     */
    public static class UserCancelException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * @param phaseId id of progress phase.
     * @param total total size.
     * @param complete complete size.
     * @throws UserCancelException thrown on user cancel.
     */
    public void updateProgress(int phaseId, long total, long complete)
            throws UserCancelException;
}
