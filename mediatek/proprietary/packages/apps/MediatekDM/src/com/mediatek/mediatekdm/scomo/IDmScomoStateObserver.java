package com.mediatek.mediatekdm.scomo;

import com.mediatek.mediatekdm.DmOperation;

public interface IDmScomoStateObserver {
    void notify(int state, int previousState, DmOperation operation, Object extra);
}
