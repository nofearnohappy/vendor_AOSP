package com.mediatek.mediatekdm.mdm;

/**
 * Internal exception for feature not implemented.
 */
public class NotImplementedError extends Error {
    private static final long serialVersionUID = -6751437046758920916L;

    public NotImplementedError() {
        super("Not implemented");
    }
}
