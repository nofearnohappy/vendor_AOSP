package com.mediatek.mediatekdm.mdm;

/**
 * Session state observer. Receives notifications upon session state transition.
 */
public interface SessionStateObserver {
    /** Session States which applies to all kind of sessions. */
    public static enum SessionState {
        /** Session aborted. */
        ABORTED,
        /** Session completed successfully. */
        COMPLETE,
        /** Session started. */
        STARTED,
        /** Session paused. */
        PAUSED,
        /** @todo not implemented */
    }

    /** Session types supported by MDM engine. */
    public static enum SessionType {
        /** Bootstrap session. */
        BOOTSTRAP,
        /** OMA DL session. */
        DL,
        /** OMA DM session. */
        DM,
        /** Engine is idle. */
        IDLE,
    }

    /**
     * Notification on session state change. This method is called when a Bootstrap/DM/DL session
     * changes its state. Session is typically started and moved to either completed or aborted. In
     * DM/DL it may also be suspended/resumed. In case of ABORTED, the lastError parameter will
     * indicate the reason for the failure --- on all other states it will be 0.
     *
     * @param type
     *        The session type.
     * @param state
     *        The new state of the session.
     * @param lastError
     *        Abort reason.
     * @param initiator
     *        Session initiator.
     */
    void notify(SessionType type, SessionState state, int lastError, SessionInitiator initiator);
}
