package com.mediatek.selfregister.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.test.ServiceTestCase;
import android.util.Base64;

import com.mediatek.common.dm.DmAgent;
import com.mediatek.selfregister.Const;
import com.mediatek.selfregister.RegisterMessage;
import com.mediatek.selfregister.RegisterService;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test case for register.
 */
public class RegisterServiceTest extends ServiceTestCase<RegisterService> {
    private static final String ACTION_TEST = "com.mediatek.testcase";
    private static final int FIVE_SECONDS = 5 * 1000;
    private static final int LONG_TIME = 30 * 1000;

    private static final String FIELD_REG_VERSION = "REGVER";
    private static final String FIELD_MEID = "MEID";
    private static final String FIELD_MODEL = "MODELSMS";
    private static final String FIELD_SW_VERSION = "SWVER";
    private static final String FIELD_IMSI_CDMA = "SIM1CDMAIMSI";
    private static final String FIELD_UE_TYPE = "UETYPE";
    private static final String FIELD_ICCID = "SIM1ICCID";
    private static final String FIELD_IMSI_LTE = "SIM1LTEIMSI";
    private static final String FIELD_SIM_TYPE = "SIM1TYPE";
    private static final String FIELD_IMSI_2 = "SIM2IMSI";
    private static final String FIELD_SID = "SID";
    private static final String FIELD_NID = "NID";
    private static final String FIELD_MACID = "MACID";
    private static final String VALUE_REG_VERSION = "1.0";

    private static final String REGEX_STRICT = "[a-zA-Z0-9]{1,3}-[ a-zA-Z0-9]{1,16}[+]{0,1}";
    private static final String REGEX_LOOSER = "[a-zA-Z0-9 |_+-]{1,20}";

    /**
     * Constructor for ServiceTestCase.
     */
    public RegisterServiceTest() {
        super(RegisterService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent(ACTION_TEST);
        intent.setClass(getSystemContext(), RegisterService.class);
        startService(intent);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the method getIccIDFromCard() in RegisterService.
     * @throws Exception All exceptions throwed during this case running.
     */
    public void test01GetICCID() throws Exception {
        RegisterService service = getService();

        Thread.sleep(FIVE_SECONDS);

        TelephonyManager telephonyManager = service.getTelephonyManager();
        assertNotNull("Get mTelephonyManager is null!", telephonyManager);

        /// M: Check if has card.
        assertTrue("No card in the device!", telephonyManager.hasIccCard());

        boolean ready = service.mPhoneValues.containsKey(RegisterService.KEY_ROAMING);
        String[] iccIDs = null;
        if (ready) {
            iccIDs = service.getIccIDFromCard();
        } else {
            Thread.sleep(FIVE_SECONDS);
            iccIDs = service.getIccIDFromCard();
        }

        for (int i = 0; i < iccIDs.length; i++) {
            assertNotNull("ICCID is null!", iccIDs[i]);
            assertEquals("ICCID " + i + " not equal!", getIccIDFromCard(i), iccIDs[i]);
        }
    }

    /**
     * Test the method getRegisterMessage() in RegisterMessage.
     * @throws Exception All exceptions throwed during this case running.
     */
    public void test02GetMessage() throws Exception {
        RegisterService service = getService();
        RegisterMessage registerMessage = new RegisterMessage(service);
        Thread.sleep(FIVE_SECONDS);
        String base64Msg = registerMessage.getRegisterMessage();
        byte[] message = Base64.decode(base64Msg, Base64.DEFAULT);
        JSONObject jsonMsg = new JSONObject(new String(message));

        assertEquals(VALUE_REG_VERSION, jsonMsg.getString(FIELD_REG_VERSION));

        String meid = service.getTelephonyManager().getDeviceId(0);
        assertEquals(meid.substring(0, meid.length() - 1), jsonMsg.getString(FIELD_MEID));

        String model = jsonMsg.getString(FIELD_MODEL);
        assertTrue(model.length() <= 20);
        Pattern pattern = Pattern.compile(REGEX_LOOSER);
        Matcher matcher = pattern.matcher(model);
        assertTrue("Model is invalid!", matcher.matches());

        String swVersion = jsonMsg.getString(FIELD_SW_VERSION);
        assertTrue("Software version is invalid!", swVersion.length() < 60);

        assertNotNull(jsonMsg.getString(FIELD_IMSI_CDMA));
        assertNotNull(jsonMsg.getString(FIELD_UE_TYPE));
        assertEquals(getIccIDFromCard(0), jsonMsg.getString(FIELD_ICCID));
        assertNotNull(jsonMsg.getString(FIELD_IMSI_LTE));
        assertNotNull(jsonMsg.getString(FIELD_SIM_TYPE));
        assertNotNull(jsonMsg.getString(FIELD_IMSI_2));
        assertNotNull(jsonMsg.getString(FIELD_SID));
        assertNotNull(jsonMsg.getString(FIELD_NID));
        assertNotNull(jsonMsg.getString(FIELD_MACID));
    }

    /**
     * Test the the first time register flow.
     * @throws Exception All exceptions throwed during this case running.
     */
    public void test03Register() throws Exception {
        /// M: Clear register flag.
        IBinder binder = ServiceManager.getService("DmAgent");
        DmAgent agent = DmAgent.Stub.asInterface(binder);
        String registerFlag = "0";
        agent.setRegisterFlag(registerFlag.getBytes(), registerFlag.getBytes().length);

        /// M: Register
        AlarmManager alarm = (AlarmManager) getSystemContext()
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(Const.ACTION_RETRY, null, getSystemContext(),
                RegisterService.class);
        PendingIntent operation = PendingIntent.getService(getSystemContext(), 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        alarm.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + Const.ONE_SECOND, operation);

        /// M: Check result
        Thread.sleep(LONG_TIME);
        registerFlag = new String(agent.readRegisterFlag());
        assertEquals("1", registerFlag);
    }

    /**
     * Test the register flow when SIM card changed.
     * @throws Exception All exceptions throwed during this case running.
     */
    public void test04CardChangeRegister() throws Exception {
        /// M: Clear register flag.
        IBinder binder = ServiceManager.getService("DmAgent");
        DmAgent agent = DmAgent.Stub.asInterface(binder);
        String iccID1 = "012345676543210";
        agent.writeIccID1(iccID1.getBytes(), iccID1.getBytes().length);

        /// M: Register
        AlarmManager alarm = (AlarmManager) getSystemContext()
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(Const.ACTION_RETRY, null, getSystemContext(),
                RegisterService.class);
        PendingIntent operation = PendingIntent.getService(getSystemContext(), 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        alarm.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + Const.ONE_SECOND, operation);

        /// M: Check result
        Thread.sleep(LONG_TIME);
        String iccIDRead = new String(agent.readIccID1());
        assertEquals("ICCID is not equal!", getIccIDFromCard(0), iccIDRead);
    }

    private String getIccIDFromCard(int slot) {
        if (!SubscriptionManager.isValidSlotId(slot)) {
            return null;
        }

        TelephonyManager telephonyManager = (TelephonyManager) getSystemContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        long[] subId = SubscriptionManager.getSubId(slot);
        if (subId == null || subId[0] == -1) {
            return Const.ICCID_DEFAULT_VALUE;
        } else {
            return telephonyManager.getSimSerialNumber(subId[0]);
        }
    }
}
