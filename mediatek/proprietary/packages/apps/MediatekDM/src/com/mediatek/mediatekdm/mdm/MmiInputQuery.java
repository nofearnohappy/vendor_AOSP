package com.mediatek.mediatekdm.mdm;

public interface MmiInputQuery {
    public static enum EchoType {
        MASKED, PLAIN, UNDEFINED,
    }

    public static enum InputType {
        ALPHANUMERIC, DATE, IP_ADDRESS, NUMERIC, PHONE, TIME, UNDEFINED,
    }

    MmiResult display(MmiViewContext context, InputType inputType, EchoType echoType,
            int maxLength, String defaultInput);
}
