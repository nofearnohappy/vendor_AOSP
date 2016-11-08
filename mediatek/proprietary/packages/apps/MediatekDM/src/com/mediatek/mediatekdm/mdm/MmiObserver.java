package com.mediatek.mediatekdm.mdm;

/**
 * MMI View observer. Application should notify engine with the user response via this observer.
 */
public interface MmiObserver {
    /**
     * Notify MDM Engine that the user has made a selection on a choice list given by the DM server.
     *
     * @param bitflags
     *        Each bit represents an item in the list. If set, then user has selected the item. In
     *        single-selection list, only one bit must be set.
     */
    void notifyChoicelistSelection(int bitflags);

    /**
     * Notify MDM Engine that the user has cancelled the operation. May be called from any MMI
     * screen instead of the screen's result.
     */
    void notifyCancelEvent();

    /**
     * Notify MDM Engine that the user has confirmed or declined an action prompted by the DM
     * server.
     *
     * @param confirmed
     *        true if user has confirmed the action, false if user has declined it.
     */
    void notifyConfirmationResult(boolean confirmed);

    /**
     * Notify MDM Engine that an information message from the DM server or MDM engine has been
     * closed by the user.
     */
    void notifyInfoMsgClosed();

    /**
     * Notify MDM Engine the user has entered input requested by the DM server.
     *
     * @param userInput
     *        Text entered by the user.
     */
    void notifyInputResult(String userInput);

    /**
     * Notify MDM Engine that a timeout event has occurred (maxDisplayTime seconds have passed)
     * without any user response. May be called from any MMI screen instead of the screen's result.
     */
    void notifyTimeoutEvent();
}
