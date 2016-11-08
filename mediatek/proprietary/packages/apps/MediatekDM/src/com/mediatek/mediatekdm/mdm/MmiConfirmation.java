package com.mediatek.mediatekdm.mdm;

/**
 * Confirmation MMI view.
 */
public interface MmiConfirmation {
    /**
     * Default response command.
     */
    public static enum ConfirmCommand {
        /** Negative command is the default response. */
        NO,
        /** Positive command is the default response. */
        YES,
        /** No default response is defined. */
        UNDEFINED,
    }

    /**
     * Prompt user to confirm / decline a session. The MmiObserver must be notified upon a user
     * response, cancellation, or timeout.
     *
     * @param context
     *        Context of the screen to be displayed.
     * @param defaultCommand
     *        Which command (confirm/declined), if any, should be selected by default.
     * @return whether the MMI has been displayed successfully.
     */
    MmiResult display(MmiViewContext context, ConfirmCommand defaultCommand);
}
