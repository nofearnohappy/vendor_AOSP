package com.hesine.nmsg.business.bean;

public class ServiceInfo {

    private long id = 0;
    private String account = null;
    private String email = null;
    private String phoneNum = null;
    private String name = null;
    private String desc = null;
    private Long timeStamp = null;
    private String icon = null;
    private int status = 1;
    private int isExistInPhonebook;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getIsExist() {
        return isExistInPhonebook;
    }

    public void setIsExist(int isExist) {
        this.isExistInPhonebook = isExist;
    }
}
