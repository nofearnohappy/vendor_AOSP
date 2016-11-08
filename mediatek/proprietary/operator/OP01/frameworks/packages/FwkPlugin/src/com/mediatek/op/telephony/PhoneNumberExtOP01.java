package com.mediatek.op.telephony;

import com.mediatek.common.PluginImpl;

/**
 * OP01 plugin implementation of IPhoneNumberExt.
 */
@PluginImpl(interfaceName="com.mediatek.common.telephony.IPhoneNumberExt")
public class PhoneNumberExtOP01 extends PhoneNumberExt {
    public boolean isPauseOrWait(char c) {
        return (c == 'p' || c == 'P' || c == 'w' || c == 'W');
    }
}
