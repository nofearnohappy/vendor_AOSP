package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.DownloadDescriptor;

/**
 * Handle SCOMO DP notifications. An instance of this interface should be registered to MdmScomoDp.
 */
public interface MdmScomoDpHandler {
    /**
     * Notification that the delivery package is available for download and the Engine is awaiting
     * command to start download.
     *
     * @param scomoDpInstance
     *        SCOMO DP instance.
     * @param dd
     *        Download descriptor.
     * @return true to indicate to start downloading the package package, or false to postpone
     *         download execution until MdmScomoDp.resumeDLSession() is called.
     */
    boolean confirmDownload(MdmScomoDp scomoDpInstance, DownloadDescriptor dd);

    /**
     * Notification that the delivery package has been downloaded and the Engine is awaiting command
     * to start install.
     *
     * @param scomoDpInstance
     *        SCOMO DP instance.
     * @return true to launch the Installer to execute install, or false to postpone installation
     *         execution until either: MdmScomoDp.executeInstall() is called, or after device
     *         reboot.
     */
    boolean confirmInstall(MdmScomoDp scomoDpInstance);

    /**
     * Request the installer to start with the execution of the delivery package.
     *
     * @param scomoDpInstance
     *        SCOMO DP instance.
     * @param deliveryPkgPath
     *        Path to the downloaded delivery package.
     * @param isActive
     *        A flag to indicate whether to install DCs in their active state.
     * @return The return value is ignored.
     */
    ScomoOperationResult executeInstall(MdmScomoDp scomoDpInstance, String deliveryPkgPath,
            boolean isActive);
}
