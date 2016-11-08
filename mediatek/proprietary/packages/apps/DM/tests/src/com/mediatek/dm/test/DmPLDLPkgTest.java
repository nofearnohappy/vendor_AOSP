/**
 *
 */
package com.mediatek.dm.test;

import junit.framework.Assert;
import android.test.AndroidTestCase;
import android.util.Log;
import android.content.Context;

import com.mediatek.dm.DmPLDlPkg;


/**
 * @author MTK80987
 *
 */
public class DmPLDLPkgTest extends AndroidTestCase {

    private static final String TAG = "[DmPLDLPkgTest]";
    private static final String FILE_PATH = "/data/data/com.mediatek.dm/files/test.txt";
    private Context mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    public void testWriteChunk() {
        Log.d(TAG, "test writeChunk begin");
        DmPLDlPkg pdPkg = new DmPLDlPkg(mContext);
        byte[] data = {'t', 'e', 's', 't'};
        try {
            long nRet = pdPkg.writeChunk("test.txt", 0, data);
            Assert.assertTrue(nRet != 0);
        }
        catch (Exception e) {
            Log.d(TAG, "test writeChunk fail");
            e.printStackTrace();
        }

    }

    public void testGetPkgSize() {
        Log.d(TAG, "test getFileSize begin");

        try {
            DmPLDlPkg pdPkg = new DmPLDlPkg(mContext);
            long nRet = pdPkg.getPkgSize("test.txt");
        }
        catch (Exception e) {
            Log.d(TAG, "test getFileSize fail");
            e.printStackTrace();
        }
    }

    public void testIsFileExist() {
        Log.d(TAG, "test isFileExist begin");

        try {
            boolean flag = DmPLDlPkg.isFileExist(mContext, "test.txt");
        }
        catch (Exception e) {
            Log.d(TAG, "test isFileExist fail");
            e.printStackTrace();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
