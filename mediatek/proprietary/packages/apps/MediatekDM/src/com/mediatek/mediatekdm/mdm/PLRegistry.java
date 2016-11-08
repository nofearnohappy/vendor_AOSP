package com.mediatek.mediatekdm.mdm;

public interface PLRegistry {
    void setStringValue(String key, String value) throws MdmException;

    /**
     * Retrieve a string value associated with a registry key.
     *
     * @param key
     * @return
     */
    String getStringValue(String key);

    void setIntValue(String key, int value) throws MdmException;

    Integer getIntValue(String key);

    void deleteKeysByPrefix(String prefix) throws MdmException;
}
