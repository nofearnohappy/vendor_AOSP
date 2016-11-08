package com.mediatek.ims.internal;

import java.util.ArrayList;
import java.lang.StringBuffer;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PcscfInfo implements Parcelable {
    public static final int IMC_PCSCF_ACQUIRE_BY_NONE = 0;
    public static final int IMC_PCSCF_ACQUIRE_BY_SIM = 1;
    public static final int IMC_PCSCF_ACQUIRE_BY_MO = 2;
    public static final int IMC_PCSCF_ACQUIRE_BY_PCO = 3;
    public static final int IMC_PCSCF_ACQUIRE_BY_DHCPv4 = 4;
    public static final int IMC_PCSCF_ACQUIRE_BY_DHCPv6 = 5;
    public static final int IMC_PCSCF_ACQUIRE_BY_MANUAL = 6;

    public int source = IMC_PCSCF_ACQUIRE_BY_NONE;
    public ArrayList<PcscfAddr> v4AddrList = new ArrayList<PcscfAddr>();
    public ArrayList<PcscfAddr> v6AddrList = new ArrayList<PcscfAddr>();

    public PcscfInfo() {
    }

    public PcscfInfo(int sourceNum, String[] pcscfArray) {
        if (pcscfArray != null && pcscfArray.length > 0) {
            source = sourceNum;
            for (String pcscf : pcscfArray)
                add(pcscf, 0); //port is not specified
        }
    }

    public PcscfInfo(int sourceNum, byte[] pcscfBytes, int port) {
        source = sourceNum;
        add(new String(pcscfBytes), port);
    }

    public void add(String pcscf, int port) {
        PcscfAddr pcscfAddr = new PcscfAddr(pcscf);
        pcscfAddr.port = port;

        if (pcscfAddr.protocol == PcscfAddr.IMC_PDP_ADDR_IPV4_ADDR_TYPE)
            v4AddrList.add(pcscfAddr);
        else
            v6AddrList.add(pcscfAddr);
    }

    public int getPcscfAddressCount() {
        return v4AddrList.size() + v6AddrList.size();
    }

    public void readFrom(Parcel p) {
        source = p.readInt();
        int v4AddrNumber = p.readInt();
        for (int i=0; i<v4AddrNumber; i++) {
            PcscfAddr addr = new PcscfAddr();
            addr.readFrom(p);
            v4AddrList.add(addr);
        }

        int v6AddrNumber = p.readInt();
        for (int i=0; i<v6AddrNumber; i++) {
            PcscfAddr addr = new PcscfAddr();
            addr.readFrom(p);
            v6AddrList.add(addr);
        }
    }

    public void writeTo(Parcel p) {
        p.writeInt(source);
        p.writeInt(v4AddrList.size());
        for (PcscfAddr addr : v4AddrList)
            addr.writeTo(p);

        p.writeInt(v6AddrList.size());
        for (PcscfAddr addr : v6AddrList)
            addr.writeTo(p);
    }

    public void readAddressFrom(int sourceNum, Parcel p) {
        String pcscfStr = p.readString();
        if (!TextUtils.isEmpty(pcscfStr)) {
            String[] pcscfArray = pcscfStr.split(" ");
            if (pcscfArray != null && pcscfArray.length > 0) {
                for (String pcscf : pcscfArray)
                    add(pcscf, 0); //port is not specified
            }
        }
    }

    public void writeAddressTo(Parcel p) {
        int count = 0;
        for (PcscfAddr addr : v4AddrList) {
            if (count == 0)
                p.writeString(addr.address);
            else
                p.writeString(" " + addr.address);

            ++count;
        }
        for (PcscfAddr addr : v6AddrList) {
            if (count == 0)
                p.writeString(addr.address);
            else
                p.writeString(" " + addr.address);

            ++count;
        }
    }

    //This method does not clone data to a new instance
    public void copyFrom(PcscfInfo pcscfInfo) {
        source = pcscfInfo.source;
        v4AddrList = pcscfInfo.v4AddrList;
        v6AddrList = pcscfInfo.v6AddrList;
    }

    public void reset() {
        source = IMC_PCSCF_ACQUIRE_BY_NONE;
        v4AddrList.clear();
        v6AddrList.clear();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("[source=" + source + ", V4[");
        for (PcscfAddr addr : v4AddrList)
            buf.append(addr.toString());
        buf.append("] V6[");
        for (PcscfAddr addr : v6AddrList)
            buf.append(addr.toString());
        buf.append("]");
        return buf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeTo(dest);
    }

    public static final Parcelable.Creator<PcscfInfo> CREATOR =
            new Parcelable.Creator<PcscfInfo>() {
        @Override
        public PcscfInfo createFromParcel(Parcel source) {
            PcscfInfo pcscfInfo = new PcscfInfo();
            pcscfInfo.readFrom(source);
            return pcscfInfo;
        }

        @Override
        public PcscfInfo[] newArray(int size) {
            return new PcscfInfo[size];
        }
    };
}
