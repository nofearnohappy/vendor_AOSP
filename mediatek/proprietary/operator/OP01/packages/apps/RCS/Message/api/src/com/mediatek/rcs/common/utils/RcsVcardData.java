package com.mediatek.rcs.common.utils;

/**
* this is a class of vcard data
*/

public final class RcsVcardData {
    private final String mData;
    private final String mType;

    /**
    * this is constructor.
    * @param
    * @return null
    */
    public RcsVcardData(String data, String type) {
        mData = data;
        mType = type;
    }

    /**
    * this is for get data(number, emails...).
    * @param  null
    * @return String data
    */
    public String getData() {
        return mData;
    }

    /**
    * this is for get TYPE
    * @param  null
    * @return String type
    */
    public String getType() {
        return mType;
    }

    @Override
    public String toString() {
        return String.format("%s", mData);
    }
}
