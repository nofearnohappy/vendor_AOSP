package com.mediatek.rcs.message.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ForwardSendData implements Parcelable {
    private String mMineType;
    private String mContent;

    public ForwardSendData(Parcel source) {
        mMineType = source.readString();
        mContent = source.readString();
    }

    public ForwardSendData(String mimeType, String content) {
        mMineType = mimeType;
        mContent = content;
    }


    public String getMineType() {
        return mMineType;
    }

    public void setMineType(String mineType) {
        mMineType = mineType;
    }

    public String  getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public int describeContents() {
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMineType);
        dest.writeString(mContent);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public ForwardSendData createFromParcel(Parcel in) {
                return new ForwardSendData(in);
            }

            public ForwardSendData[] newArray(int size) {
                return new ForwardSendData[size];
            }
        };
}
