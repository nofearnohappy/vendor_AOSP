package com.mediatek.mediatekdm.mdm;

/**
 * Handler of a parsed Notification Initiated Alert (NIA) message.
 */
public interface NIAMsgHandler {
    /**
     * UI mode specified in NIA message.
     */
    public static enum UIMode {
        BACKGROUND, INFORMATIVE, NOT_SPECIFIED, UI,
    }

    /**
     * A NIA message content has been parsed successfully after Notification Initiated DM session
     * (NIDM) is triggered. The parsed content is also passed. After handling the notification alert
     * content, this method should call either MdmEngine.notifyNIASessionProceed() to proceed with
     * the DM session or MdmEngine.cancelSession()to abort the session.
     *
     * @param uiMode
     *        UI mode.
     * @param dmVersion
     *        DM protocol, which is always 0.
     * @param vendorSpecificData
     *        Vendor-specific data.
     * @param initiator
     *        Session initiator.
     * @throws MdmException
     */
    void notify(UIMode uiMode, short dmVersion, byte[] vendorSpecificData,
            SessionInitiator initiator) throws MdmException;
}
