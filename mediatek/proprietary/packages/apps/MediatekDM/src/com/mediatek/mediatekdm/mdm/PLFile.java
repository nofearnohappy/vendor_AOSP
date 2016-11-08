package com.mediatek.mediatekdm.mdm;

import java.io.IOException;

/**
 * Porting Layer File Interface.
 */
public interface PLFile {
    /**
     * Close file.
     *
     * @param commit
     *        true if written data should be committed. false if storage was opened for reading.
     * @throws IOException
     */
    void close(boolean commit) throws IOException;

    /**
     * Read data from file. After the read operation is finished, the file pointer must be
     * positioned at the end of the data read.
     *
     * @param buf
     *        The buffer into which the data is read.
     * @return Number of bytes actually read. This will be equal to buf.length unless there no more
     *         available data to read (e.g. reached end of file). This value must be 0 (zero) when
     *         no more data is available.
     * @throws IOException
     */
    int read(byte[] buf) throws IOException;

    /**
     * Write data to file. After the write operation is finished, the file pointer must be
     * positioned at the end of the data written.
     *
     * @param data
     *        Data to write.
     * @throws IOException
     */
    void write(byte[] data) throws IOException;
}
