package com.mediatek.rcs.pam;

public class PAMException extends Exception {
    private static final long serialVersionUID = -8473389324125847185L;
    public final int resultCode;

    public PAMException(int result) {
        super("PAMException: " + result);
        resultCode = result;
    }
}
