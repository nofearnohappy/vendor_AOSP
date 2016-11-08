/**
 *
 */
package com.mediatek.dm.test;

import java.io.IOException;

import junit.framework.Assert;

import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmPLDeltaFile;
import com.redbend.vdm.PLStorage.AccessMode;

/**
 * @author MTK80987
 *
 */
public class DmPlDeltaFile extends AndroidTestCase {

    private static final String TAG = "[DmPLDeltaFileTest]";
    private static final String FILE_PATH = "deltatest.data";
    private static final String TEST_STR = "test";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void test001WriteDeltaFile() throws IOException {
        Log.d(TAG, "test write delta file begin");

        DmPLDeltaFile delta = new DmPLDeltaFile(FILE_PATH, this.getContext(), AccessMode.WRITE);
        Assert.assertNotNull(delta);
        delta.write(TEST_STR.getBytes());
        delta.close(true);
    }

    public void test002CreateDmPlDeltaFileWithRead() throws IOException {
        Log.d(TAG, "test read delta file begin");

        DmPLDeltaFile delta = new DmPLDeltaFile(FILE_PATH, this.getContext(), AccessMode.READ);
        Assert.assertNotNull(delta);
        byte[] data = new byte[5];
        delta.read(data);
        delta.close(false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
