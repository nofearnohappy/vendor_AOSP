package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;
import com.mediatek.mediatekdm.mdm.PLDlPkg;

public class DPDataIoHandler implements NodeIoHandler {
    private MdmScomoDp mDp;
    private MdmScomo mScomo;
    private PLDlPkg mDlPkg;

    public DPDataIoHandler(MdmScomo scomo, MdmScomoDp dp) {
        mDp = dp;
        mScomo = scomo;
        mDlPkg = mScomo.getEngine().getPLDlPkg();
    }

    public int read(int offset, byte[] data) throws MdmException {
        // Do NOT allow read.
        return 0;
    }

    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        if ((offset == 0 && data.length == 0) && mDlPkg.getMaxSize() < totalSize) {
            throw new MdmException(MdmError.MO_STORAGE);
        }
        mDp.setAlternativeDownload(false);
        String filename = mDlPkg.getFilename(mDp.getName());
        mDlPkg.writeChunk(filename, offset, data);
    }

}
