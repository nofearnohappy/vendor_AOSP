package com.mediatek.mediatekdm.mdm;

import java.io.IOException;

/**
 * Porting Layer Device Storage Interface.
 */
public interface PLStorage {
    /**
     * Access mode for storage items (files). A storage item can be opened in read-only or write
     * modes. If a non-existent storage item is opened in write mode, item must be created.
     */
    public static enum AccessMode {
        /** Read mode */
        READ,
        /** Write mode */
        WRITE,
    }

    /**
     * MDM defines two types of storage items that it uses when calling the storage Porting Layer
     * API functions.
     */
    public static enum ItemType {
        /** Download Resume data */
        DLRESUME,
        /** Device Management tree */
        DMTREE,
        /** Reserved */
        RESERVED,
    }

    /**
     * Delete an item from storage.
     *
     * @param type
     *        Type of storage item.
     */
    void delete(ItemType type);

    /**
     * Open item (file) in storage. A storage item can be opened in read-only or write modes. If a
     * non-existent storage item is opened in write mode, this function must create the item.
     *
     * @param type
     *        Type of storage item.
     * @param mode
     *        Read or Write.
     * @return Handle to the open storage item.
     * @throws IOException
     */
    PLFile open(ItemType type, AccessMode mode) throws IOException;
}
