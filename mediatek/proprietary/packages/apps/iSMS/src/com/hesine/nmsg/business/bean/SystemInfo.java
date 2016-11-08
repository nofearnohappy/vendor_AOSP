package com.hesine.nmsg.business.bean;

public class SystemInfo {

    private String phoneNum = null;
    private String imsi = null;
    private String imei = null;
    private String device = null;
    private String brand = null;
    private String language = null;
    private String osver = null;
    private String pnToken = null;
    private String pnType = null;
    private LocationInfo location = null;
    private String uuid = null;

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getImsi() {
        return imsi;
    }

    public String getImei() {
        return imei;
    }

    public String getDevice() {
        return device;
    }

    public String getBrand() {
        return brand;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPnToken() {
        return pnToken;
    }

    public void setPnToken(String pnToken) {
        this.pnToken = pnToken;
    }

    public String getPnType() {
        return pnType;
    }

    public void setPnType(String pnType) {
        this.pnType = pnType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public LocationInfo getLocation() {
        return location;
    }

    public void setLocation(LocationInfo location) {
        this.location = location;
    }

    public String getOsver() {
        return osver;
    }

    public void setOsver(String osver) {
        this.osver = osver;
    }

}
