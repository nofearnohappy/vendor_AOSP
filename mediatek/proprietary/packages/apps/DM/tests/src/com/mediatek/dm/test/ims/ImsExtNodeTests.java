package com.mediatek.dm.test.ims;

import android.test.AndroidTestCase;


import com.android.ims.ImsConfig;
//import com.mediatek.common.ims.mo.ImsAuthInfo;

import com.redbend.vdm.VdmException;



public class ImsExtNodeTests extends AndroidTestCase {
    private final static String TAG = "[ImsExtNodeTest]";
    private final static String URI_PREFIX = "./IMSMO/Ext/RCS/";
    private ImsConfig imsConfig = null;
    private final static int MAX_BUF_LEN = 100;
    private final static String IMS_AUTH_TYPE = "AuthType";
    private final static String IMS_REALM = "Realm";
    private final static String IMS_USER_NAME = "UserName";
    private final static String IMS_USER_PWD = "UserPwd";
    private final static String[] SUB_NODES = {IMS_AUTH_TYPE, IMS_REALM, IMS_USER_NAME, IMS_USER_PWD};
    private final static String[] AUTH_TEST = {"test1", "test2", "test3", "test4"};

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
        ImsAuthInfo info = imsConfig.readImsAuthInfoMo();
        if (info == null) {
            Log.i(TAG, "readImsAuthInfoMo return null");
        }

        for(int i = 0; i < len; i++) {
            String strUri = URI_PREFIX + SUB_NODES[i];
            byte[] buf = new byte[MAX_BUF_LEN];

            DmImsExtRcsNodeIoHandler handler = new DmImsExtRcsNodeIoHandler(mContext, Uri.parse(strUri));
            if (handler == null) {
                Log.i(TAG, "handler is null, why?");
            }
            int ret = handler.read(0, buf);
            byte[] tmp = new byte[ret];
            for(int k = 0; k < ret; k++) {
                tmp[k] = buf[k];
            }

            String expect = null;
            switch(i) {
            case 0:
                expect = info.getAuthType();
                break;
            case 1:
                expect = info.getRelam();
                break;
            case 2:
                expect = info.getUserName();
                break;
            case 3:
                expect = info.getUserPwd();
                break;
            default:
                break;
            }
            String actual = new String(tmp);
            Log.i(TAG, actual);
            Assert.assertEquals(expect, actual);
        }
*/
    }

    public void testWrite() throws VdmException {
/*
        int len = SUB_NODES.length;
        for(int i = 0; i < len; i++) {
            String strUri = URI_PREFIX + SUB_NODES[i];
            byte[] buf = new byte[MAX_BUF_LEN];
            DmImsExtRcsNodeIoHandler handler = new DmImsExtRcsNodeIoHandler(mContext, Uri.parse(strUri));
            handler.write(0, AUTH_TEST[i].getBytes(), AUTH_TEST[i].getBytes().length);
        }

        ImsAuthInfo info = imsConfig.readImsAuthInfoMo();

        Assert.assertEquals(AUTH_TEST[0], info.getAuthType());
        Assert.assertEquals(AUTH_TEST[1], info.getRelam());
        Assert.assertEquals(AUTH_TEST[2], info.getUserName());
        Assert.assertEquals(AUTH_TEST[3], info.getUserPwd());
*/
    }
}
