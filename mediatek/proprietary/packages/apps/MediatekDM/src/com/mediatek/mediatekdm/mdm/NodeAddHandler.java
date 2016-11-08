package com.mediatek.mediatekdm.mdm;

/**
 * Handler for the ADD command on a node.
 */
public interface NodeAddHandler {
    public static class OperationPhase {
        public static final int SINGLE_MODE = 0;
    }

    /**
     * Notify when a new node has been added to the DM tree.
     *
     * @param data
     *        Node's value.
     * @param offset
     *        The byte offset from the start of the data.
     * @param total
     *        The total length of the value data. This may be different from data.length if partial
     *        writes are used.
     * @param format
     *        Format of the added node. "node" indicates an interior node, while "chr", "int",
     *        "bin", and so on indicate the format of the value of a leaf node.
     * @param nodeUri
     *        Full path of the node to Add.
     * @param phase
     *        The phase of the Add operation.
     * @return To set the DF properties of the newly created node, return a bit mask of the desired
     *         VdmTree.DFProperties flags.
     * @throws MdmException
     */
    int add(byte[] data, long offset, long total, String format, String nodeUri, int phase)
            throws MdmException;
}
