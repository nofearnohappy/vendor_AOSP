/**
 *
 */
package com.mediatek.dm.test.ext;

import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.ext.MTKMediaContainer;

/**
 * @author MTK80987
 *
 */
public class MTKMediaContainerTest extends AndroidTestCase {
    private static final String TAG = "[MTKMediatekContainerTest]";
    private static final String ARCHIVE_PATH = "/data/data/com.mediatek.dm/files/packageinfo.zip";;
    private static final int SPACE_THRESHOLD = 1024 * 1024;

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCheckSpace() {
        Log.d(TAG, "test checkSpace begin");
        try {
            MTKMediaContainer container = new MTKMediaContainer(null);
            container.isValid();
            //container.checkSpace(ARCHIVE_PATH, SPACE_THRESHOLD);
            container.finish();
        } catch (Exception e) {
            Log.d(TAG, "test checkSpace fail");
            e.printStackTrace();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
