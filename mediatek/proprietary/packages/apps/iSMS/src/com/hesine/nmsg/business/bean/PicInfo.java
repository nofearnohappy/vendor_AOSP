package com.hesine.nmsg.business.bean;

import java.io.File;

import com.hesine.nmsg.common.EnumConstants;

public class PicInfo {

    public static final String SAVE_DIR = EnumConstants.STORAGE_PATH + File.separator + "files" + File.separator
            + "avatar/";

    private String type = null;
    private String name = null;
    private String size = null;
    private String pic = null;

    private String account;
    private String url;
    private String md5;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public static String getPicPath(PicInfo obj) {
        return SAVE_DIR + obj.getName() + "." + obj.getType();
    }
}
