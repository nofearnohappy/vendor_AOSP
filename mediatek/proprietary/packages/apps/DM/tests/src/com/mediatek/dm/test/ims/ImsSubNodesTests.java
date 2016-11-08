package com.mediatek.dm.test.ims;

import android.test.AndroidTestCase;

import android.net.Uri;
import android.telephony.SubscriptionManager;


import com.android.ims.ImsConfig;
import com.android.ims.ImsConfig.ConfigConstants;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.mediatek.dm.ims.DmImsNodeIoHandler;

import com.redbend.vdm.VdmException;


import junit.framework.Assert;

public class ImsSubNodesTests extends AndroidTestCase {
    private final static String TAG = "[ImsSubNodesTest]";
    private final static int BOOL_LEN_BYTES = 6; // ? suspicous
    private final static String PCSCF_ADRESS = "P-CSCF_Address";
    private final static String TIMER_ONE = "Timer_T1";
    private final static String TIMER_TWO = "Timer_T2";
    private final static String TIMER_FOUR = "Timer_T4";
    private final static String PRIVATE_USER_ID = "Private_user_identity";
    private final static String PUBLIC_USER_ID = "Public_user_identity";
    private final static String DOMAIN_NAME = "Home_network_domain_name";
    private final static String RESOURCE_ALLOCATION_MODE = "Resource_Allocation_Mode";
    private final static String VOICE_E_UTRAN = "Voice_Domain_Preference_E_UTRAN";
    private final static String SMS_IP_INDICATION = "SMS_Over_IP_Networks_Indication";
    private final static String KEEP_ALIVE_ENABLED = "Keep_Alive_Enabled";
    private final static String VOICE_UTRAN = "Voice_Domain_Preference_UTRAN";
    private final static String IMS_VOICE_TERMINATION = "Mobility_Management_IMS_Voice_Termination";
    private final static String REG_RETRY_BASE_TIME = "RegRetryBaseTime";
    private final static String REG_RETRY_MAX_TIME = "RegRetryMaxTime";

    private final static int MAX_BUFF_LEN = 100;

    private final static String[] SUB_NODES_STRING = {PCSCF_ADRESS, PRIVATE_USER_ID, PUBLIC_USER_ID, DOMAIN_NAME};

    private final static String[] SUB_NODES_INT = {TIMER_ONE, TIMER_TWO, TIMER_FOUR, VOICE_E_UTRAN, VOICE_UTRAN, REG_RETRY_BASE_TIME, REG_RETRY_MAX_TIME };
    private final static String[] SUB_NODES_BOOL = {RESOURCE_ALLOCATION_MODE, SMS_IP_INDICATION, KEEP_ALIVE_ENABLED, IMS_VOICE_TERMINATION};

    private final static int[] NODE_ID_STRING = {ConfigConstants.IMS_MO_PCSCF, ConfigConstants.IMS_MO_IMPI,
                                                    ConfigConstants.IMS_MO_IMPU, ConfigConstants.IMS_MO_DOMAIN};
    private final static int[] NODE_ID_INT = {ConfigConstants.SIP_T1_TIMER, ConfigConstants.SIP_T2_TIMER,
                                                ConfigConstants.SIP_TF_TIMER, ConfigConstants.IMS_MO_VOICE_E,
                                                ConfigConstants.IMS_MO_VOICE_U, ConfigConstants.IMS_MO_REG_BASE,
                                                ConfigConstants.IMS_MO_REG_MAX};
    private final static int[] NODE_ID_BOOL = {ConfigConstants.IMS_MO_RESOURCE, ConfigConstants.IMS_MO_SMS,
                                                ConfigConstants.IMS_MO_KEEPALIVE, ConfigConstants.IMS_MO_MOBILITY};
    private final static String URI_PREFIX = "./IMSMO/";

    private ImsConfig imsConfig = null;
    protected void setUp() throws Exception {
        super.setUp();
        ImsManager imsManager = ImsManager.getInstance(mContext, SubscriptionManager.getDefaultSubId());

        try {
            ImsConfig imsConfig = imsManager.getConfigInterface();
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testReadStringNodes() throws VdmException {
        int len = SUB_NODES_STRING.length;
        for (int i = 0; i < len; i++) {
            String uriPath = URI_PREFIX + SUB_NODES_STRING[i];
            DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));

            byte[] bufRead = new byte[MAX_BUFF_LEN];
            int ret = handler.read(0, bufRead);

            byte[] tmp = new byte[ret];
            for (int k = 0; k < ret; k++) {
                tmp[k] = bufRead[k];
            }

            String actual = new String(tmp);
            String expected = null;
            try {
                expected = imsConfig.getProvisionedStringValue(NODE_ID_STRING[i]);
            } catch (ImsException e) {
                e.printStackTrace();
            }

            Assert.assertEquals(expected, actual);
        }
    }

    public void testReadIntNodes() throws VdmException {
        int len = SUB_NODES_INT.length;

        for (int i = 0; i < len; i++) {
            String uriPath = URI_PREFIX + SUB_NODES_INT[i];
            DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));
            byte[] bufRead = new byte[MAX_BUFF_LEN];
            int ret = handler.read(0, bufRead);

            int expected = 0;
            try {
                expected = imsConfig.getProvisionedValue(NODE_ID_INT[i]);
            } catch (ImsException e) {
                e.printStackTrace();
            }
            Assert.assertEquals(handler.byteToInt(bufRead), expected);
        }
    }

    public void testReadBoolNodes() throws VdmException {
        int len = SUB_NODES_BOOL.length;
        for (int i = 0; i < len; i++) {
            String uriPath = URI_PREFIX + SUB_NODES_BOOL[i];
            DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));

            byte[] bufRead = new byte[BOOL_LEN_BYTES];
            int ret = handler.read(0, bufRead);

            int expected = 0;
            try {
                expected = imsConfig.getProvisionedValue(NODE_ID_BOOL[i]);
            } catch (ImsException e) {
                e.printStackTrace();
            }
            Assert.assertEquals(expected == 1 ? true : false, handler.byteToBoolean(bufRead));
        }
    }

    public void testWriteStringNodes() throws VdmException {
        int len = SUB_NODES_STRING.length;
        String strToWrite = "test";
        String uriPath = URI_PREFIX + PCSCF_ADRESS;
        DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));
        handler.write(0, strToWrite.getBytes(), strToWrite.getBytes().length);

        String expected = null;
        try {
            expected = imsConfig.getProvisionedStringValue(ConfigConstants.IMS_MO_PCSCF);
        } catch (ImsException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(strToWrite, expected);
    }

    public void testWriteIntNodes() throws VdmException {
        int len = SUB_NODES_INT.length;
        int intToWrite = 1;
        int expected = 0;

        for (int i = 0; i < len; i++) {
            String uriPath = URI_PREFIX + SUB_NODES_INT[i];
            DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));
            handler.write(0, handler.toByteArray(intToWrite), handler.toByteArray(intToWrite).length);

            try {
                expected = imsConfig.getProvisionedValue(NODE_ID_INT[i]);
            } catch (ImsException e) {
                e.printStackTrace();
            }
            Assert.assertEquals(expected, intToWrite);
        }
    }

    public void testWriteBoolNodes() throws VdmException {
        int len = SUB_NODES_BOOL.length;
        boolean boolToWrite = true;
        int expected = 0;

        for (int i = 0; i < len; i++) {
            String uriPath = URI_PREFIX + SUB_NODES_BOOL[i];
            DmImsNodeIoHandler handler = new DmImsNodeIoHandler(mContext, Uri.parse(uriPath));
            handler.write(0, handler.toByteArray(boolToWrite), handler.toByteArray(boolToWrite).length);

            try {
                expected = imsConfig.getProvisionedValue(NODE_ID_BOOL[i]);
            } catch (ImsException e) {
                e.printStackTrace();
            }
            Assert.assertEquals(boolToWrite, expected == 1 ? true : false);
        }
    }
}
