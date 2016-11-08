package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.DownloadDescriptor;

/**
 * Handle FUMO notifications.
 */
public interface FumoHandler {
    /**
     * Notification that the update package is available for download and the Engine is awaiting
     * command to start download.
     *
     * @param dd
     *        Download descriptor.
     * @param fumoInstance
     *        FUMO instance.
     * @return true to indicate to start downloading the update package, or false to postpone
     *         download execution until MdmFumo.resumeDLSession() is called.
     */
    boolean confirmDownload(DownloadDescriptor dd, MdmFumo fumoInstance);

    /**
     * Notification that the update package has been downloaded and the Engine is awaiting command
     * to start update.
     *
     * @param fumoInstance
     *        FUMO instance.
     * @return true to launch the Update Agent to execute update, or false to postpone update
     *         execution until either: MdmFumo.executeFwUpdate() is called, or after device reboot.
     */
    boolean confirmUpdate(MdmFumo fumoInstance);

    /**
     * Request the update agent to start with the execution of the firmware update.
     *
     * @param updatePkgPath
     *        Path to the downloaded update package.
     * @param fumoInstance
     *        FUMO instance.
     * @return MdmFumoUpdateResult
     */
    MdmFumoUpdateResult executeUpdate(String updatePkgPath, MdmFumo fumoInstance);
}
