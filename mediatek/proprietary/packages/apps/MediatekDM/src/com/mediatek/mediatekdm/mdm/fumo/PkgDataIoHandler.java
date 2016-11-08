package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;
import com.mediatek.mediatekdm.mdm.PLDlPkg;

/**
 * A node IO handler for "<x>/Update/PkgData" which writes package data to PLDlPkg.
 */
class PkgDataIoHandler implements NodeIoHandler {
    private MdmFumo mFumo;
    private PLDlPkg mDlPkg;

    public PkgDataIoHandler(MdmFumo fumo) {
        mFumo = fumo;
        mDlPkg = mFumo.getEngine().getPLDlPkg();
    }

    public int read(int offset, byte[] data) throws MdmException {
        // Do NOT allow read.
        return 0;
    }

    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        if ((offset == 0 && data.length == 0) && mDlPkg.getMaxSize() < totalSize) {
            throw new MdmException(MdmError.MO_STORAGE);
        }
        String filename = mDlPkg.getFilename(MdmTree.makeUri(mFumo.getRootUri(),
                MdmFumo.Uri.UPDATE_PKGDATA));
        mDlPkg.writeChunk(filename, offset, data);
    }
}
