package com.mediatek.mediatekdm.mdm.dl;

import java.io.Serializable;

public class DLPLResumer implements Serializable {

    private static final long serialVersionUID = -5874497834952393804L;

    private DownloadDescriptorHelper mDDHelper;
    private int mDownloaded;
    private int mType;

    public int isSameDownload(String url) {
        return mDDHelper.getDDUri().equals(url) ? 1 : 0;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public void setDdHelper(DownloadDescriptorHelper ddHelper) {
        this.mDDHelper = ddHelper;
    }

    public DownloadDescriptorHelper getDdHelper() {
        return mDDHelper;
    }

    public void setDownloaded(int downloaded) {
        this.mDownloaded = downloaded;
    }

    public int getDownloaded() {
        return mDownloaded;
    }
}
