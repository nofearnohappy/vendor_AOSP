package com.cmcc.ccs.capability;

import org.gsma.joyn.capability.Capabilities;

import java.util.Set;

public class ExtCapabilities extends Capabilities {
    public ExtCapabilities(boolean arg0, boolean arg1, boolean arg2, boolean arg3, boolean arg4,
                           boolean arg5, boolean arg6, Set<String> arg7, boolean arg8, boolean arg9,
                           boolean arg10, boolean arg11, boolean arg12, boolean arg13) {
        super(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11,
              arg12, arg13);
        // TODO Auto-generated constructor stub
    }

    /* Return true directly for CMCC */
    public boolean isPublicAccountSupport() {
        return true;
    }
}
