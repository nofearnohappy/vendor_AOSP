package com.mediatek.mediatekdm.mdm.dl;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.io.Serializable;

public class DownloadDescriptorHelper implements Serializable {
    private static final long serialVersionUID = -6839822512508431845L;

    private String mDDUri;

    private String mObjectURI;
    private String mSize;
    private String mType;
    private String mName;
    private String mDDVersion;
    private String mVendor;
    private String mDescription;
    private String mInstallNotifyURI;
    private String mNextURL;
    private String mInfoURL;
    private String mIconURI;
    private String mInstallParam;

    public DownloadDescriptorHelper(String ddUri) {
        Log.d(TAG.DL, "DownloadDescriptorHelper constructed");
        this.mDDUri = ddUri;
        Log.d(TAG.DL, "the DD URI:" + this.mDDUri);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nobjectURI : ").append(mObjectURI).append("\nsize : ").append(mSize)
                .append("\ntype : ").append(mType).append("\nname : ").append(mName)
                .append("\nDDVersion : ").append(mDDVersion).append("\nvendor : ").append(mVendor)
                .append("\ndescription : ").append(mDescription).append("\ninstallNotifyURI : ")
                .append(mInstallNotifyURI).append("\nnextURL : ").append(mNextURL)
                .append("\ninfoURL : ").append(mInfoURL).append("\niconURI : ").append(mIconURI)
                .append("\ninstallParam: ").append(mInstallParam);
        return sb.toString();
    }

    private String checkUrl(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("../")) {
            Log.d(TAG.DL, "uri is a relative uri");
            uri = mDDUri.substring(0, mDDUri.lastIndexOf("/") + 1) + uri;
        }
        if (uri.contains("amp;")) {
            Log.d(TAG.DL, "the uri has special charactors &amp;");
            String[] segments = uri.split("amp;");
            StringBuilder sb = new StringBuilder();
            for (String seg : segments) {
                sb.append(seg);
            }
            uri = sb.toString().trim();
            Log.d(TAG.DL, "after process, uri: " + uri);
        }
        return uri;
    }

    public void setDDUri(String uri) {
        this.mDDUri = uri;
    }

    public String getDDUri() {
        return mDDUri;
    }

    public String getObjectURI() {
        return mObjectURI;
    }

    public void setObjectURI(String uri) {
        this.mObjectURI = checkUrl(uri);
    }

    public String getSize() {
        return mSize;
    }

    public int getSizeInt() {
        return Integer.parseInt(mSize);
    }

    public void setSize(int size) {
        this.mSize = Integer.toString(size);
    }

    public void setSize(String size) {
        this.mSize = size;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getDDVersion() {
        return mDDVersion;
    }

    public void setDDVersion(String dDVersion) {
        mDDVersion = dDVersion;
    }

    public String getVendor() {
        return mVendor;
    }

    public void setVendor(String vendor) {
        this.mVendor = vendor;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public String getInstallNotifyURI() {
        return mInstallNotifyURI;
    }

    public void setInstallNotifyURI(String installNotifyURI) {
        this.mInstallNotifyURI = checkUrl(installNotifyURI);
    }

    public String getNextURL() {
        return mNextURL;
    }

    public void setNextURL(String nextURL) {
        this.mNextURL = checkUrl(nextURL);
    }

    public String getInfoURL() {
        return mInfoURL;
    }

    public void setInfoURL(String infoURL) {
        this.mInfoURL = checkUrl(infoURL);
    }

    public String getIconURI() {
        return mIconURI;
    }

    public void setIconURI(String iconURI) {
        this.mIconURI = checkUrl(iconURI);
    }

    public String getInstallParam() {
        return mInstallParam;
    }

    public void setInstallParam(String installParam) {
        this.mInstallParam = installParam;
    }
}
