package com.mediatek.mediatekdm.mdm;

public interface MmiInfoMsg {
    public static enum InfoType {
        EXITING, GENERIC, STARTUP,
    }

    MmiResult display(MmiViewContext context, InfoType type);
}
