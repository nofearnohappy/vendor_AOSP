package com.hesine.nmsg.business.net;

import com.hesine.nmsg.business.Pipe;

public class RequestTask {

    private String method = "POST";
    private String url = "";
    private Pipe listener = null;
    private byte[] constructData = null;
    private byte[] parseData = null;
    private boolean useGZip = false;

    public RequestTask(String url, byte[] constructData, Pipe listener) {
        this.setUrl(url);
        this.setConstructData(constructData);
        this.setListener(listener);
    }

    public RequestTask(String method, String url, byte[] constructData, Pipe listener) {
        this.setMethod(method);
        this.setUrl(url);
        this.setConstructData(constructData);
        this.setListener(listener);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Pipe getListener() {
        return listener;
    }

    public void setListener(Pipe listener) {
        this.listener = listener;
    }

    public byte[] getConstructData() {
        return constructData;
    }

    public void setConstructData(byte[] constructData) {
        this.constructData = constructData;
    }

    public byte[] getParseData() {
        return parseData;
    }

    public void setParseData(byte[] parseData) {
        this.parseData = parseData;
    }

    public boolean isUseGZip() {
        return useGZip;
    }

    public void setUseGZip(boolean useGZip) {
        this.useGZip = useGZip;
    }
}
