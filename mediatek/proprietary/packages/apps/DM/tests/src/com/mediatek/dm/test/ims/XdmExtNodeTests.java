package com.mediatek.dm.test.ims;

import android.test.AndroidTestCase;


import com.android.ims.ImsConfig;
//import com.mediatek.common.ims.mo.ImsXcapInfo;

import com.redbend.vdm.VdmException;



public class XdmExtNodeTests extends AndroidTestCase {
    private static final String TAG = "[XdmExtNodeTest]";
    private ImsConfig imsConfig = null;
    private String URI_PREFIX = "./XDMMO/Ext/";
    private static int MAX_BUF_LEN = 100;

    private String[] CAP_TEST = {"test1", "test2", "test3", "test4"};

    private final static String XDM_URI = "URI";
    private final static String XDM_AUTH_NAME = "AAUTHNAME";
    private final static String XDM_AUTH_SECRET = "AAUTHSECRET";
    private final static String XDM_AUTH_TYPE = "AAUTHTYPE";

    private String[] SUB_NODES = {XDM_URI, XDM_AUTH_NAME, XDM_AUTH_SECRET, XDM_AUTH_TYPE};

    protected void setUp() throws Exception {
        super.setUp();
//        ImsManager imsManager = ImsManager.getInstance(mContext, SubscriptionManager.getDefaultSubId());
//        ImsConfig imsConfig = imsManager.getConfigInterface();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRead() throws VdmException {
/*
        int len = SUB_NODES.length;
        ImsXcapInfo info = imsConfig.readImsXcapInfoMo();

        for(int i = 0; i < len; i++) {
            String strUri = URI_PREFIX + SUB_NODES[i];
            byte[] buf = new byte[MAX_BUF_LEN];
            DmXdmNodeIoHandler handler = new DmXdmNodeIoHandler(mContext, Uri.parse(strUri));
            int ret = handler.read(0, buf);
            byte[] tmp = new byte[ret];
            for (int k = 0; k < ret; k++) {
            tmp[k] = buf[k];
            }

            String expected = null;
            switch(i) {
            case 0:
                expected = info.getXcapRootUri();
            break;
            case 1:
                expected = info.getXcapAuth();
            break;
            case 2:
                expected = info.getXcapAuthSecret();
            break;
            case 3:
                expected = info.getXcapAuthType();
            break;
            default:
            break;
            }
            String actual = new String(tmp);
            Log.i(TAG, actual);

            Assert.assertEquals(expected, actual);
        }
*/
    }

    public void testWrite() throws VdmException {
/*
        int len = SUB_NODES.length;
        for(int i = 0; i < len; i++) {
            String strUri = URI_PREFIX + SUB_NODES[i];
            DmXdmNodeIoHandler handler = new DmXdmNodeIoHandler(mContext, Uri.parse(strUri));
            handler.write(0, CAP_TEST[i].getBytes(), CAP_TEST[i].getBytes().length);
        }
        ImsXcapInfo info = imsConfig.readImsXcapInfoMo();

        Assert.assertEquals(CAP_TEST[0], info.getXcapRootUri());
        Assert.assertEquals(CAP_TEST[1], info.getXcapAuth());
        Assert.assertEquals(CAP_TEST[2], info.getXcapAuthSecret());
        Assert.assertEquals(CAP_TEST[3], info.getXcapAuthType());
*/
    }
}
