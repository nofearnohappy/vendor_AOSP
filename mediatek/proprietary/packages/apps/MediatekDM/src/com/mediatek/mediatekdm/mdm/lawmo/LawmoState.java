package com.mediatek.mediatekdm.mdm.lawmo;

public enum LawmoState {
    UNDEFINED(0), FULLY_LOCKED(10), PARTIALLY_LOCKED(20), UNLOCKED(30);

    public final int val;

    private LawmoState(int value) {
        val = value;
    }

    public static LawmoState fromInt(int value) {
        for (LawmoState s : LawmoState.values()) {
            if (s.val == value) {
                return s;
            }
        }
        return null;
    }
}
