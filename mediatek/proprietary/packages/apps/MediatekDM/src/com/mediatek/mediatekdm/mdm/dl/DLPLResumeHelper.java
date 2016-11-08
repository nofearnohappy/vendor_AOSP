package com.mediatek.mediatekdm.mdm.dl;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.PLFile;
import com.mediatek.mediatekdm.mdm.PLStorage;
import com.mediatek.mediatekdm.pl.DmPLDeltaFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DLPLResumeHelper {

    private PLStorage mPls;

    public DLPLResumeHelper() {
        this.mPls = MdmEngine.getInstance().getPLStorage();
    }

    public DLPLResumer readResume() {
        Log.i(TAG.DL, "readResume from resumer.bak ..");

        DLPLResumer resumer = null;
        PLFile plReader = null;
        ObjectInputStream ois = null;

        try {
            byte[] len = new byte[4];
            plReader = (DmPLDeltaFile) mPls.open(PLStorage.ItemType.DLRESUME, PLStorage.AccessMode.READ);
            plReader.read(len);

            int length = byteToInt(len);
            Log.d(TAG.DL, "read resume byte array length: " + length);
            byte[] data = new byte[length];
            plReader.read(data);

            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            resumer = (DLPLResumer) ois.readObject();
        } catch (IOException e) {
            Log.e(TAG.DL, "IOException : " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(TAG.DL, "ClassNotFoundException : " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (plReader != null) {
                    plReader.close(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resumer;
    }

    public void writeResume(DLPLResumer resumer) {
        Log.i(TAG.DL, "writeResume ..");

        boolean isCommit = true;
        PLFile plWriter = null;
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(resumer);
            oos.flush();

            byte[] data = baos.toByteArray();
            byte[] len = intToByte(data.length);
            Log.d(TAG.DL, "write resume byte array length: " + len.length);

            plWriter = mPls.open(PLStorage.ItemType.DLRESUME, PLStorage.AccessMode.WRITE);
            plWriter.write(len);
            plWriter.write(data);
        } catch (IOException e) {
            Log.e(TAG.DL, "exception when write pl resume file !");
            isCommit = false;
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (oos != null) {
                    oos.close();
                }
                if (plWriter != null) {
                    plWriter.close(isCommit);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] intToByte(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0xff);
        b[1] = (byte) ((i & 0xff00) >> 8);
        b[2] = (byte) ((i & 0xff0000) >> 16);
        b[3] = (byte) ((i & 0xff000000) >> 24);
        return b;
    }

    private int byteToInt(byte[] b) {
        return ((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff);
    }
}
