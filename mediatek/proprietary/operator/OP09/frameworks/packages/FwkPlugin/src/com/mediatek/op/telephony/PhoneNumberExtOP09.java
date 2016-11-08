package com.mediatek.op.telephony;

import com.mediatek.common.PluginImpl;

/**
 * OP09 plugin implementation of IPhoneNumberExt.
 */
@PluginImpl(interfaceName="com.mediatek.common.telephony.IPhoneNumberExt")
public class PhoneNumberExtOP09 extends PhoneNumberExt {
    // CT min match is 11 digits
    public int getMinMatch() {
        return 11;
    }
}
