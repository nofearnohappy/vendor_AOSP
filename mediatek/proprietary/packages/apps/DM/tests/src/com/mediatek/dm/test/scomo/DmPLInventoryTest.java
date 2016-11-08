/**
 *
 */
package com.mediatek.dm.test.scomo;

import junit.framework.Assert;

import com.mediatek.dm.scomo.DmPLInventory;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * @author MTK80987
 *
 */
public class DmPLInventoryTest extends AndroidTestCase {
    private static final String TAG = "[DmPLInventoryTest]";
    private static final String PACK_PATH = "/data/data/com.meidatek.dm/files/packageinfo.zip";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCommon() {
        Log.d(TAG, "test common functions begin");

        DmPLInventory inventory = new DmPLInventory();

        Assert.assertNotNull(inventory.getInstance());
        inventory.addComponent(null);
        inventory.deleteComponent(null);
        inventory.findComponentById(PACK_PATH);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
