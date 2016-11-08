package com.mediatek.mediatekdm.mdm.scomo;

/**
 * Handle SCOMO DC notifications. An instance of this interface should be registered to MdmScomoDc.
 */
public interface MdmScomoDcHandler {
    /**
     * Notification the Engine is awaiting command to start uninstall.
     *
     * @param scomoDc
     *        the deployment component to be uninstalled
     * @return true to launch the Installer to execute uninstall, or false to postpone
     *         uninstallation execution until either MdmScomoDc.executeRemove() is called or after
     *         device reboot
     */
    boolean confirmRemove(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of uninstallation.
     *
     * @param scomoDc
     *        the deployment component to be uninstalled
     * @return the uninstallation result
     */
    ScomoOperationResult executeRemove(MdmScomoDc scomoDc);

    /**
     * Notification the Engine is awaiting command to start activation.
     *
     * @param scomoDc
     *        the deployment component to be activated
     * @return true to launch the Installer to execute activation, or false to postpone activation
     *         execution until either MdmScomoDc.executeActivate() is called or after device reboot
     */
    boolean confirmActivate(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of activation.
     *
     * @param scomoDc
     *        the deployment component to be activated
     * @return the activation result
     */
    ScomoOperationResult executeActivate(MdmScomoDc scomoDc);

    /**
     * Notification the Engine is awaiting command to start deactivation.
     *
     * @param scomoDc
     *        the deployment component to be deactivated
     * @return true to launch the Installer to execute deactivation, or false to postpone
     *         deactivation execution until either MdmScomoDc.executeDeactivate() is called or after
     *         device reboot
     */
    boolean confirmDeactivate(MdmScomoDc scomoDc);

    /**
     * Request the installer to start with the execution of deactivation.
     *
     * @param scomoDc
     *        the deployment component to be deactivated
     * @return the deactivation result
     */
    ScomoOperationResult executeDeactivate(MdmScomoDc scomoDc);
}
