package com.mediatek.mediatekdm.mdm.scomo;

/**
 * Handle SCOMO notifications. An instance of this interface should be registered to MdmScomo.
 */
public interface MdmScomoHandler {
    /**
     * This method will be invoked when a new delivery package is added.
     *
     * @param dpName
     *        the name of the added delivery package.
     */
    void newDpAdded(String dpName);
}
