/**
 *
 */
package com.mediatek.dm.test;

import java.io.IOException;

import junit.framework.Assert;

import com.mediatek.dm.xml.DmXMLWriter;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * @author MTK80987
 *
 */
public class DmXmlWriterTest extends AndroidTestCase {
    private static final String TAG = "[DmXMLWriterTest]";
    private static final String XML_PATH = "/data/data/com.mediatek.dm/files/test.xml";
    private static final String XML_TAG = "TAG";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testFunctions() throws IOException {
        Log.d(TAG, "test Xml Writer functions begin");
        DmXMLWriter writer = new DmXMLWriter(XML_PATH);
        Assert.assertNotNull(writer);
        writer.writeStartTag(XML_TAG);
        writer.addValue(XML_TAG, "12345");
        writer.writeEndTag(XML_TAG);
        writer.close();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
