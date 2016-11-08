package com.mediatek.mediatekdm.mdm;

/**
 * Session Initiator. Passed to all Session State observers when a state of a session changes.
 */
public interface SessionInitiator {
    /**
     * Get session initiator's unique id.
     *
     * @return unique string id
     */
    String getId();
}
