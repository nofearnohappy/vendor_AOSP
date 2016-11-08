package com.mediatek.mediatekdm.mdm.lawmo;

/**
 * LAWMO actions.
 */
public class LawmoAction {
    /** No action has been performed during the last session. */
    public static final int NONE = 0;
    /** Fully lock. */
    public static final int FULLY_LOCK_EXECUTED = 1;
    /** Partially lock. */
    public static final int PARTIALLY_LOCK_EXECUTED = 2;
    /** UnLock. */
    public static final int UNLOCK_EXECUTED = 4;
    /** Factory reset. */
    public static final int FACTORY_RESET_EXECUTED = 8;
    /** Wipe. */
    public static final int WIPE_EXECUTED = 16;
    public static final int ALL = FULLY_LOCK_EXECUTED | PARTIALLY_LOCK_EXECUTED | UNLOCK_EXECUTED
            | FACTORY_RESET_EXECUTED | WIPE_EXECUTED;

}
