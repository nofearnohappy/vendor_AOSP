package com.mediatek.rcs.pam.util;

public final class PaVcardData {
    private final String mData;
    private final String mType;
    
    public PaVcardData(String data, String type) {
        mData = data;
        mType = type;
    }
    
    public String getData() {
        return mData;
    }
    
    public String getType() {
        return mType;
    }
    
    @Override
    public String toString() {
        return String.format("\n\n%s", mData);
    }
}
