package com.mediatek.mediatekdm.mdm;

public interface NodeIoHandler {
    /**
     * Read node's data from an external storage.
     *
     * @param offset
     *        The byte offset from the start of the data.
     * @param data
     *        Preallocated buffer to store the value data or null. Note: null is valid input. If
     *        null, do not throw MdmException - just return the total size.
     * @return The total length of the entire data in bytes.
     * @throws MdmException
     */
    int read(int offset, byte[] data) throws MdmException;

    /**
     * Write data to in external storage. Normally, the data is written in a single call to this
     * method. For large amounts of data, the data can be written using a series of calls. If this
     * partial write mechanism isn't supported, throw MDMException with
     * MDMException.MDMError.TREE_EXT_NOT_PARTIAL. If the data is not written in a single call, it
     * will always be written in a series of calls with increasing offsets starting with an offset
     * of 0. If offset + data.length equals totalSize, then all the data has been sent and the data
     * should now be stored if it has not been stored incrementally. Before attempting partial
     * writes, this method is called with offset and data.length both set to 0 and totalSize set to
     * the maximum length of the data. This is the only time data.length of 0 is supplied, unless
     * totalSize is also 0. Note: In certain circumstances, totalSize supplied with the last chunk
     * of data may be a few bytes shorter than that supplied with the preceding chunks. This is due
     * to the way the SyncML protocol handles Base64 encoded data. If the write fails part way
     * through while using the partial write mechanism, then MDM will normally try to set the length
     * of the data to 0, but it is possible that the data will simply be left unfinished. If this is
     * a problem, then partial writes should be rejected and MdmException with
     * MdmException.MdmError.TREE_EXT_NOT_PARTIAL should be thrown. If it is not possible to write
     * the value for some reason (for instance, it is in use by another program) then MdmException
     * with MdmException.MdmError.TREE_EXT_NOT_ALLOWED should be thrown.
     *
     * @param offset
     *        The byte offset from the start of the value data.
     * @param data
     *        Buffer storing the value data (may be partial data)
     * @param totalSize
     *        Buffer storing the value data (may be partial data)
     * @throws MdmException
     */
    void write(int offset, byte[] data, int totalSize) throws MdmException;
}
