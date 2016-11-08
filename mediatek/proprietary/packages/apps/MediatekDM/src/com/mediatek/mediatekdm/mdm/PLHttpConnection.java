package com.mediatek.mediatekdm.mdm;

public interface PLHttpConnection {

    boolean initialize(String url, int proxyType, String proxyAddr, int proxyPort);

    void destroy();

    boolean openComm();

    boolean closeComm();

    int sendData(byte[] data);

    int recvData(byte[] buffer);

    boolean addRequestProperty(String field, String value);

    int getHeadFieldInt(String field, int defValue);

    String getHeadField(String field);

    String getURL();

    int getContentLength();
}
