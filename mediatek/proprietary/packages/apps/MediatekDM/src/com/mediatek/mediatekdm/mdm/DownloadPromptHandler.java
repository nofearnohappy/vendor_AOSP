package com.mediatek.mediatekdm.mdm;

/**
 * OMA DL download prompt handler. Client will be notified via this handler when DD is downloaded.
 */
public interface DownloadPromptHandler {
    /**
     * Notification that the update package is available for download and the Engine is awaiting
     * command to start download. Invoke {@link MdmEngine#notifyDLSessionProceed()} to proceed, or
     * invoke {@link MdmEngine#cancelSession()} to cancel it.
     *
     * @param dd
     *        Download descriptor.
     * @param initiator
     *        Session initiator.
     * @throws MdmException
     */
    void notify(DownloadDescriptor dd, SessionInitiator initiator) throws MdmException;
}
