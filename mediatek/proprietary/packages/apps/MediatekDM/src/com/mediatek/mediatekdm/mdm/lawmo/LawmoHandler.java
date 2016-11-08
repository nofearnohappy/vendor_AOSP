package com.mediatek.mediatekdm.mdm.lawmo;

public interface LawmoHandler {

    LawmoOperationResult executeFullyLock();

    LawmoOperationResult executePartiallyLock();

    LawmoOperationResult executeUnLock();

    /**
     * Notification that a Factory Reset operation is going to be executed.
     *
     * @return
     */
    LawmoOperationResult executeFactoryReset();

    LawmoOperationResult executeWipe(String[] dataToWipe);
}
