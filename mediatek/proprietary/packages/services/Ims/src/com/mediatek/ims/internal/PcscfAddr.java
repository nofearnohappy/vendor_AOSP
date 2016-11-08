package com.mediatek.ims.internal;

import android.os.Parcel;
import android.os.Parcelable;

public class PcscfAddr implements Parcelable {
    public static final int IMC_PDP_ADDR_NONE_ADDR_TYPE = 0X0;
    public static final int IMC_PDP_ADDR_IPV4_ADDR_TYPE = 0X21;
    public static final int IMC_PDP_ADDR_IPV6_ADDR_TYPE = 0X57;
    public static final int IMC_PDP_ADDR_IPV4V6_ADDR_TYPE = 0X8D;
    public static final int IMC_PDP_ADDR_NULL_PDP_ADDR_TYPE = 0X03;

    public int protocol;
    public int port;
    public String address;

    public PcscfAddr() {
}

    public PcscfAddr(String addr) {
        address = addr;
        if (address == null) {
            protocol = IMC_PDP_ADDR_NONE_ADDR_TYPE;
        } else {
            if (address.split("\\.").length > 4) //dot is the reserved symbol
                protocol = IMC_PDP_ADDR_IPV6_ADDR_TYPE;
            else
                protocol = IMC_PDP_ADDR_IPV4_ADDR_TYPE;
        }
    }

    public void readFrom(Parcel p) {
        protocol = p.readInt();
        port = p.readInt();
        address = p.readString();
    }

    public void writeTo(Parcel p) {
        p.writeInt(protocol);
        p.writeInt(port);
        p.writeString(address);
    }

    public void reset() {
        protocol = 0;
        port = 0;
        address = null;
    }

    @Override
    public String toString() {
        return "[protocol=" + protocol + ", port=" + port + ", address=" + address + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeTo(dest);
    }

    public static final Parcelable.Creator<PcscfAddr> CREATOR =
            new Parcelable.Creator<PcscfAddr>() {
        @Override
        public PcscfAddr createFromParcel(Parcel source) {
            PcscfAddr pcscfAddr = new PcscfAddr();
            pcscfAddr.readFrom(source);
            return pcscfAddr;
        }

        @Override
        public PcscfAddr[] newArray(int size) {
            return new PcscfAddr[size];
        }
    };
}