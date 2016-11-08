package com.mediatek.mediatekdm.mdm;

/**
 * This handler will be invoked before an interior/leaf node is deleted.
 */
public interface NodeOnDeleteHandler {
    void onDelete(String uri);
}
