package com.mediatek.mediatekdm.fumo;

import com.mediatek.mediatekdm.DmOperation;

public interface IDmFumoStateObserver {
    void notify(int newState, int previousState, DmOperation operation, Object extra);
}
