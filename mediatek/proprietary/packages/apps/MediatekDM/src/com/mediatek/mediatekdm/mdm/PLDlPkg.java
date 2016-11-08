package com.mediatek.mediatekdm.mdm;

/**
 * Porting Layer Download Package Interface.
 * <p/>
 * DLOTA Download package. Download Over the Air (DLOTA) is a mechanism that allows wireless devices
 * to download packages and updates, as required. The PLDlPkg interface provides APIs to ensure that
 * there is sufficient space before downloading, to incrementally append downloaded chunks of a
 * package, and to remove the package from non volatile storage when it is no longer required. In a
 * typical file system,the downloaded package will be stored in a file. The MDM framework requires
 * that the download package is uniquely identified by a string. Since the framework may have to
 * access the downloaded package after restarting, the download package handle cannot be a pointer,
 * but must be a string.
 */
public interface PLDlPkg {
    /**
     * Get the filename for the package to be downloaded. This filename is later used to delete the
     * file.
     *
     * @param identifier
     *        A unique identifier created by MDM. This may be a node URI in the DM tree, or the
     *        value of the name field in the download descriptor. The returned string can be the
     *        full path and file name of the file, or any other string ID. This string will later be
     *        used to identify the file. Note: The input string may be an invalid filename (may
     *        contain invalid characters).
     * @return Filename of this package file.
     */
    String getFilename(String identifier);

    /**
     * Get maximum size (in bytes) allowed for a download package. Before downloading a package,
     * vDirect Mobile verifies that there is enough storage space for it.
     *
     * @return Maximum size allowed for a download package.
     */
    int getMaxSize();

    /**
     * Write the next chunk of bytes from a current Download session. A package is normally
     * downloaded in chunks. For each chunk, MDM passes the data, the chunk size, and its offset.
     *
     * @param filename
     *        Filename returned by getFilename.
     * @param offset
     *        The offset within the download to write the data chunk.
     * @param data
     *        The data to write.
     * @return bytes written.
     * @throws MdmException
     */
    int writeChunk(String filename, int offset, byte[] data) throws MdmException;

    /**
     * Delete a download package from device. When the package is downloaded, MDM notifies the
     * client application for processing. The client application can remove the package; if it
     * doesn't, MDM removes the package when it receives notification that processing is finished by
     * calling this function. Therefore, if the package doesn't exist, the function must assume that
     * the client application removed it.
     *
     * @param filename
     *        Package's filename returned by getFilename.
     */
    void remove(String filename);
}
