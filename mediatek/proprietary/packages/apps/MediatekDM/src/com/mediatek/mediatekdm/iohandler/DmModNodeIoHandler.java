package com.mediatek.mediatekdm.iohandler;

import com.mediatek.custom.CustomProperties;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

public class DmModNodeIoHandler implements NodeIoHandler {

    public static final String URI = "./DevInfo/Mod";

    public int read(int offset, byte[] data) throws MdmException {
        String mod = CustomProperties.getString(CustomProperties.MODULE_DM, CustomProperties.MODEL,
                "MTK");
        byte[] src = mod.getBytes();

        if (data == null) {
            return src.length;
        } else {
            for (int i = 0; i < src.length; i++) {
                data[offset + i] = src[i];
            }
        }

        return src.length;
    }

    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        /* This method is intentionally left blank */
    }

}
