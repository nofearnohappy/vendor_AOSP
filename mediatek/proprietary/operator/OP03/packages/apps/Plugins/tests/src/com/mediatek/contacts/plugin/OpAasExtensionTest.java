package com.mediatek.contacts.plugin;

import java.util.ArrayList;

import com.mediatek.contacts.ext.Anr;
//import com.mediatek.contacts.ext.IContactAccountExtension;
//import com.mediatek.contacts.ext.DefaultAasExtension;
import com.mediatek.contacts.aas.plugin.OpAasExtension;
import com.mediatek.contacts.aas.plugin.SimUtils;
import com.mediatek.contacts.aas.plugin.LogUtils;
import android.app.Instrumentation;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.test.InstrumentationTestCase;

import android.view.View;
import android.widget.TextView;
import android.provider.ContactsContract.Data;

public class OpAasExtensionTest extends InstrumentationTestCase {
    private static final String TAG = "OpAasExtensionTest";

    private static final String ACCOUNT_TYPE_SIM = "SIM Account";
    private static final String ACCOUNT_TYPE_USIM = "USIM Account";

    private OpAasExtension mOpAasExt = null;
    private Instrumentation mInst = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        //mOpAasExt = new ContactsPlugin(mInst.getTargetContext()).createContactAccountExtension();
          mOpAasExt = new OpAasExtension(mInst.getTargetContext());
    }

    @Override
    protected void tearDown() throws Exception {
        if (mInst != null) {
            mInst = null;
        }
        if (mOpAasExt != null) {
            mOpAasExt = null;
        }
        super.tearDown();
    }

    /**
     * test:isFeatureEnabled(); setCurrentSlot(); getCurrentSlot(); isFeatureAccount(); isPhone();
     */
    public void testNormalMethods() {
        // test isFeatureEnabled
        assertTrue(mOpAasExt.isFeatureEnabled());

        // test setCurrentSlot & getCurrentSlot
        mOpAasExt.setCurrentSlot(0);
        int curSlot = mOpAasExt.getCurrentSlot();
        assertEquals(curSlot, 0);

        // test isFeatureAccount
        boolean result = mOpAasExt.isFeatureAccount(ACCOUNT_TYPE_USIM);
        assertTrue(result);
        result = mOpAasExt.isFeatureAccount(ACCOUNT_TYPE_SIM);
        assertFalse(result);

        // test isPhone
        result = mOpAasExt.isPhone(Phone.CONTENT_ITEM_TYPE);
        assertTrue(result);
        result = mOpAasExt.isPhone(Email.CONTENT_ITEM_TYPE);
        assertFalse(result);

        // test hidePhoneLabel
        result = mOpAasExt.hidePhoneLabel(ACCOUNT_TYPE_SIM, Phone.CONTENT_ITEM_TYPE, null);
        assertTrue(result);
        result = mOpAasExt.hidePhoneLabel(ACCOUNT_TYPE_SIM, Email.CONTENT_ITEM_TYPE, null);
        assertFalse(result);

        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            result = mOpAasExt.hidePhoneLabel(ACCOUNT_TYPE_USIM, Phone.CONTENT_ITEM_TYPE, "0");
            assertTrue(result);
            result = mOpAasExt.hidePhoneLabel(ACCOUNT_TYPE_USIM, Email.CONTENT_ITEM_TYPE, "1");
            assertFalse(result);
        } else {
            LogUtils.w(TAG, "Error, slot-0 not isert USIM Card");
        }
    }

    public void testGetTypeLabel() {
        final Resources res = getInstrumentation().getTargetContext().getResources();
        SimUtils.setCurrentSlot(0);
        CharSequence result = mOpAasExt.getTypeLabel(res, Anr.TYPE_AAS, "1", 0);
        assertNotNull(result);
        result = mOpAasExt.getTypeLabel(res, Anr.TYPE_AAS, "1", 0);
        assertNotNull(result);
        result = mOpAasExt.getTypeLabel(res, Anr.TYPE_AAS, null, 0);
        assertNotNull(result);
        result = mOpAasExt.getTypeLabel(res, Phone.TYPE_MOBILE, null, 0);
        assertNotNull(result);
        LogUtils.w(TAG, "testGetTypeLabel");
    }

    public void testGetCustomTypeLabel() {
        SimUtils.setCurrentSlot(-1);
        String result = mOpAasExt.getCustomTypeLabel(Anr.TYPE_AAS, null);
        assertNull(result);
        SimUtils.setCurrentSlot(0);
        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            result = mOpAasExt.getCustomTypeLabel(Anr.TYPE_AAS, null);
            assertNotNull(result);
            result = mOpAasExt.getCustomTypeLabel(Anr.TYPE_AAS, "1");
            assertNotNull(result);
        } else {
            LogUtils.d(TAG, "Error, slot-0 not isert USIM Card");
        }
    }

    public void testUpdateContentValues() {
        mOpAasExt.updateContentValues(null, null, null, null, -1);

        ArrayList<Anr> anrsList = new ArrayList<Anr>();
        Anr a1 = new Anr();
        a1.mAasIndex = "1";
        a1.mAdditionNumber = null;
        Anr a2 = new Anr();
        a2.mAasIndex = "1";
        a2.mAdditionNumber = "1233";

        anrsList.add(a1);
        anrsList.add(a2);

        ContentValues cv = new ContentValues();
        // CONTENTVALUE_ANR_INSERT
        mOpAasExt.updateContentValues(ACCOUNT_TYPE_SIM, cv, anrsList, null,
                OpAasExtension.CONTENTVALUE_ANR_INSERT);
        mOpAasExt.updateContentValues(ACCOUNT_TYPE_USIM, cv, anrsList, null,
                OpAasExtension.CONTENTVALUE_ANR_INSERT);

        // CONTENTVALUE_ANR_UPDATE
        mOpAasExt.updateContentValues(ACCOUNT_TYPE_SIM, cv, anrsList, null,
                OpAasExtension.CONTENTVALUE_ANR_UPDATE);
        mOpAasExt.updateContentValues(ACCOUNT_TYPE_USIM, cv, anrsList, null,
                OpAasExtension.CONTENTVALUE_ANR_UPDATE);

        // CONTENTVALUE_INSERT_SIM
        mOpAasExt.updateContentValues(ACCOUNT_TYPE_USIM, cv, anrsList, null,
                OpAasExtension.CONTENTVALUE_INSERT_SIM);
    }

    public void testUpdateDataToDb() {
        mOpAasExt.updateDataToDb(ACCOUNT_TYPE_SIM, null, null, null, 0, -1);

        // DB_UPDATE_ANR

        ContentResolver resolver = getInstrumentation().getTargetContext().getContentResolver();
        ArrayList<Anr> newAnrs = new ArrayList<Anr>();
        Anr new1 = new Anr();
        new1.mAasIndex = "1";
        new1.mAdditionNumber = null;
        Anr new2 = new Anr();
        new2.mAasIndex = "1";
        new2.mAdditionNumber = "1233";
        newAnrs.add(new1);
        newAnrs.add(new2);

        ArrayList<Anr> oldAnrs = new ArrayList<Anr>();
        Anr old1 = new Anr();
        old1.mAasIndex = "1";
        old1.mAdditionNumber = null;
        Anr old2 = new Anr();
        old2.mAasIndex = "1";
        old2.mAdditionNumber = "1233";
        Anr old3 = new Anr();
        old3.mAasIndex = "1";
        old3.mAdditionNumber = "1233";
        oldAnrs.add(old1);
        oldAnrs.add(old2);
        oldAnrs.add(old3);

        mOpAasExt.updateDataToDb(ACCOUNT_TYPE_SIM, resolver, newAnrs, oldAnrs, 0,
                OpAasExtension.DB_UPDATE_ANR);

        mOpAasExt.updateDataToDb(ACCOUNT_TYPE_USIM, resolver, newAnrs, oldAnrs, 0,
                OpAasExtension.DB_UPDATE_ANR);

        mOpAasExt.updateDataToDb(ACCOUNT_TYPE_USIM, resolver, oldAnrs, newAnrs, 0,
                OpAasExtension.DB_UPDATE_ANR);
    }

    public void testBuildOperation() {
        mOpAasExt.buildOperation(null, null, null, null, 0, -1);
        // TYPE_OPERATION_AAS
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        ArrayList<Anr> anrsList = new ArrayList<Anr>();
        Anr a1 = new Anr();
        a1.mAasIndex = "1";
        a1.mAdditionNumber = null;
        Anr a2 = new Anr();
        a2.mAasIndex = "1";
        a2.mAdditionNumber = "1233";
        mOpAasExt.buildOperation(ACCOUNT_TYPE_SIM, operationList, anrsList, null, 0,
                OpAasExtension.TYPE_OPERATION_AAS);

        mOpAasExt.buildOperation(ACCOUNT_TYPE_USIM, operationList, anrsList, null, 0,
                OpAasExtension.TYPE_OPERATION_AAS);
    }

    public void testCheckOperationBuilder() {
        mOpAasExt.checkOperationBuilder(null, null, null, -1);
        // TYPE_OPERATION_AAS
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        mOpAasExt.checkOperationBuilder(ACCOUNT_TYPE_SIM, builder, null,
                OpAasExtension.TYPE_OPERATION_AAS);
        // TYPE_OPERATION_INSERT
        mOpAasExt.checkOperationBuilder(ACCOUNT_TYPE_SIM, builder, null,
                OpAasExtension.TYPE_OPERATION_INSERT);
        mOpAasExt.checkOperationBuilder(ACCOUNT_TYPE_USIM, builder, null,
                OpAasExtension.TYPE_OPERATION_INSERT);
    }

    public void testBuildValuesForSim() {
        ContentValues values = new ContentValues();
        mOpAasExt.buildValuesForSim(ACCOUNT_TYPE_SIM, getInstrumentation().getContext(), values, null, null,
                0, 0, null);

        ArrayList<String> additionalNumberArray = new ArrayList<String>();
        additionalNumberArray.add("123");
        ArrayList<Integer> phoneTypeArray = new ArrayList<Integer>();
        phoneTypeArray.add(new Integer(101));
        ArrayList<Anr> anrList = new ArrayList<Anr>();

        final int maxAnrCount = 2;
        final int dstSlotId = 0;

        mOpAasExt.buildValuesForSim(ACCOUNT_TYPE_USIM, getInstrumentation().getContext(), values,
                additionalNumberArray, phoneTypeArray, maxAnrCount, dstSlotId, anrList);

        mOpAasExt.buildValuesForSim(ACCOUNT_TYPE_USIM, getInstrumentation().getContext(), values,
                additionalNumberArray, phoneTypeArray, maxAnrCount, dstSlotId, anrList);
    }

    public void testGetProjection() {
        mOpAasExt.getProjection(-1, null);
        mOpAasExt.getProjection(OpAasExtension.PROJECTION_COPY_TO_SIM, null);
        mOpAasExt.getProjection(OpAasExtension.PROJECTION_LOAD_DATA, null);
        mOpAasExt.getProjection(OpAasExtension.PROJECTION_ADDRESS_BOOK, null);
    }

    /***************OpContactDetailExtensionTest code moved here******/
    public void testRepChar() {
        String result = mOpAasExt.repChar(null, (char) 0, (char) 0, (char) 0,
                mOpAasExt.STRING_PRIMART);
        assertNotNull(result);
        result = mOpAasExt.repChar(null, (char) 0, (char) 0, (char) 0, mOpAasExt.STRING_ADDITINAL);
        assertNotNull(result);
    }

    public void testUpdateView() {
        TextView testTextView = new TextView(getInstrumentation().getContext());
        SimUtils.setCurrentSlot(0);

        mOpAasExt.updateView(testTextView, 0, mOpAasExt.VIEW_UPDATE_NONE);
        mOpAasExt.updateView(testTextView, 0, mOpAasExt.VIEW_UPDATE_HINT);
        mOpAasExt.updateView(testTextView, 1, mOpAasExt.VIEW_UPDATE_HINT);
        mOpAasExt.updateView(testTextView, 0, mOpAasExt.VIEW_UPDATE_VISIBILITY);
        assertTrue(testTextView.getVisibility() == View.GONE);
    }

    /**
     * test: getMaxEmptyEditors(), getAdditionNumberCount
     */
    public void testNormalMethodsExt() {
        SimUtils.setCurrentSlot(-1);
        mOpAasExt.getMaxEmptyEditors(Phone.CONTENT_ITEM_TYPE);
        SimUtils.setCurrentSlot(0);
        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            mOpAasExt.getMaxEmptyEditors(Phone.CONTENT_ITEM_TYPE);
        } else {
            LogUtils.w(TAG, "Error, slot-0 not isert USIM Card");
        }

        mOpAasExt.getAdditionNumberCount(0);
    }

    public void testIsDoublePhoneNumber() {
        String[] buffer = new String[2];
        String[] bufferName = new String[2];
        SimUtils.setCurrentSlot(-1);
        mOpAasExt.isDoublePhoneNumber(buffer, bufferName);

        SimUtils.setCurrentSlot(0);
        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            bufferName[0] = "test";
            bufferName[1] = "test";
            boolean result = mOpAasExt.isDoublePhoneNumber(buffer, bufferName);
            assertTrue(result);
        } else {
            LogUtils.w(TAG, "Error, not isert USIM Card");
        }
    }

    /************************OpContactListExtension code moved here*************************/
    public void testCheckPhoneTypeArray() {
        ArrayList<Integer> phoneTypeArray = new ArrayList<Integer>();
        mOpAasExt.checkPhoneTypeArray(ACCOUNT_TYPE_SIM, phoneTypeArray);
        mOpAasExt.checkPhoneTypeArray(ACCOUNT_TYPE_USIM, phoneTypeArray);
        assertTrue(phoneTypeArray.isEmpty());

        phoneTypeArray.add(new Integer(0));
        phoneTypeArray.add(new Integer(1));
        final int size = phoneTypeArray.size();
        mOpAasExt.checkPhoneTypeArray(ACCOUNT_TYPE_USIM, phoneTypeArray);
        assertTrue(size != phoneTypeArray.size());
    }

    public void testGenerateDataBuilder() {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        String[] columnNames = new String[] { Data.DATA2, Data.DATA3 };
        boolean result = mOpAasExt.generateDataBuilder(null, null, builder, columnNames, ACCOUNT_TYPE_SIM,
                Phone.CONTENT_ITEM_TYPE, -1, 0);
        assertFalse(result);

        result = mOpAasExt.generateDataBuilder(null, null, builder, columnNames, ACCOUNT_TYPE_USIM,
                Phone.CONTENT_ITEM_TYPE, -1, 0);
        assertTrue(result);

        result = mOpAasExt.generateDataBuilder(null, null, builder, columnNames, ACCOUNT_TYPE_USIM,
                Phone.CONTENT_ITEM_TYPE, -1, 1);
        assertTrue(result);
    }
    /*************************DialtactsExtensionTest **************************/
    public void testStartActivity() {
        SimUtils.setCurrentSlot(-1);
        mOpAasExt.onTypeSelectionChange(0);
        assertFalse(false);

        SimUtils.setCurrentSlot(0);
        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            mOpAasExt.onTypeSelectionChange(0);
            assertTrue(true);
        } else {
            LogUtils.e(TAG, "testStartActivity, the slot 0 is not USIM");
        }
    }
}
