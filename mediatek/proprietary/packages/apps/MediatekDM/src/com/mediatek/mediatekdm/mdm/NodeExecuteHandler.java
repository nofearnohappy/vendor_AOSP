package com.mediatek.mediatekdm.mdm;

import com.mediatek.mediatekdm.mdm.MdmException.MdmError;

/**
 * Handler for the EXEC command on a node.
 */
public interface NodeExecuteHandler {
    /**
     * Notify to execute a node in the DM tree.
     *
     * @param data
     *        The data supplied with the EXEC command.
     * @param correlator
     *        String to be used in the report sent to the server.
     * @return In case of asynchronous execution, the function should return 0 after invoking actual
     *         process, and MDM will send 202 ("Accepted for processing") as a result code to the DM
     *         server. In case of synchronous execution, the function should return the value which
     *         will be sent as the result code to the DM server.
     * @throws MdmException
     *         with any {@link MdmError} error code
     */
    int execute(byte[] data, String correlator) throws MdmException;
}
