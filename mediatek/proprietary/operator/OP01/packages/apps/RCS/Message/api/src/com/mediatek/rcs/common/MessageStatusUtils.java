package com.mediatek.rcs.common;

public class MessageStatusUtils {

    public static final class IFileTransfer {
        public static enum Status {
            /**
             * The file transfer invitation has to wait until the active queue
             * is available
             */
            PENDING,
            /**
             * The file transfer invitation is waiting for the acceptation
             */
            WAITING,
            /**
             * The file transfer is on-going
             */
            TRANSFERING,
            /**
             * The file transfer is canceled by current user
             */
            CANCEL,
            /**
             * The file transfer is canceled by the remote contact
             */
            CANCELED,
            /**
             * The file transfer has failed
             */
            FAILED,
            /**
             * The file transfer has been rejected
             */
            REJECTED,
            /**
             * The file transfer has been done with success
             */
            FINISHED,
            /**
             * The file transfer has been timeout with no response from receiver
             */
            TIMEOUT,
        }
    }

    /**
     * This enumerate defines the status of a sent message
     */
    public static enum Status {
        /**
         * The message is being sending
         */
        SENDING,
        /**
         * The message is being sent to server
         */
        SENT,
        /**
         * The message has been delivered to remote contact
         */
        DELIVERED,
        /**
         * The message has been displayed to the remote contact
         */
        DISPLAYED,
        /**
         * A failure happened when sending this message
         */
        FAILED,
        /**
         * Unknown status.
         */
        UNKNOWN
    }

}