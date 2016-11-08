package com.mediatek.mediatekdm.mdm;

public class SimpleSessionInitiator implements SessionInitiator {
    private final String mId;

    public SimpleSessionInitiator(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

}
