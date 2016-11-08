package com.mediatek.contacts.plugin;

import java.util.ArrayList;

import com.mediatek.contacts.sne.plugin.SimUtils;
import com.mediatek.contacts.sne.plugin.OpSneExtension;

import android.app.Instrumentation;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.test.InstrumentationTestCase;

public class OpSneExtensionTest extends InstrumentationTestCase {

    public OpSneExtension mOpSneExtension = null;
    private Instrumentation mInst = null;
    private Context mTargetContext = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //mOpContactAccountExtension = new ContactsPlugin(null).createContactAccountExtension();
        mOpSneExtension = new OpSneExtension();
        mInst = getInstrumentation();
        mTargetContext = mInst.getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mOpSneExtension != null) {
            mOpSneExtension = null;
        }
        super.tearDown();
    }

    public void testIsFeatureEnabled() {
        assertTrue(mOpSneExtension.isFeatureEnabled());
    }

    public void testIsFeatureAccount() {
        String accountType = SimUtils.ACCOUNT_TYPE_USIM;
        assertTrue(mOpSneExtension.isFeatureAccount(accountType));

        accountType = SimUtils.ACCOUNT_TYPE_SIM;
        assertFalse(mOpSneExtension.isFeatureAccount(accountType));

        accountType = SimUtils.ACCOUNT_TYPE_UIM;
        assertFalse(mOpSneExtension.isFeatureAccount(accountType));
    }

    // setCurrentSlot()
    public void testSetCurrentSlot() {
        int slotId = 0;
        mOpSneExtension.setCurrentSlot(slotId);
        assertEquals(slotId, SimUtils.getCurSlotId());
        assertEquals(SimUtils.ACCOUNT_TYPE_USIM, SimUtils.getCurAccount());
    }

    public void testHidePhoneLabel() {
        String accountType = SimUtils.ACCOUNT_TYPE_SIM;
        assertTrue(mOpSneExtension.hidePhoneLabel(accountType, null, null));

        accountType = SimUtils.ACCOUNT_TYPE_USIM;
        assertFalse(mOpSneExtension.hidePhoneLabel(accountType, null, null));

    }

    // set the nickname value
    public void testUpdateContentValues() {
        String accountType = SimUtils.ACCOUNT_TYPE_USIM;
        String text = null;
        ContentValues values = new ContentValues();
        int type = 0; // 1,2,3anr,---

        // 01--right type,not null text,put sne success
        text = "nicknameofme";
        if (mOpSneExtension.updateContentValues(accountType, values, null, text)) {
            assertEquals(text, values.get("sne"));
        }
        // 02-sim
        accountType = SimUtils.ACCOUNT_TYPE_SIM;
        assertFalse(mOpSneExtension.updateContentValues(accountType, values, null, text));

    }

    // test the length of nickname,the par--feature is not useful
    public void testIsTextValid() {
        String text = null;
        int slotId = 0;
        int feature = 0;

        // 01--empty
        assertTrue(mOpSneExtension.isTextValid(text, slotId));
        // 02--valid
        text = "nickmmmm";
        assertTrue(mOpSneExtension.isTextValid(text, slotId));
        // 03--too long
        text = "abcdefghijklmnopqrstu";
        assertFalse(mOpSneExtension.isTextValid(text, slotId));
        // 04
        text = "端午春asdfghjklop";
        assertFalse(mOpSneExtension.isTextValid(text, slotId));
        // 05
        text = "端午春as";
        assertTrue(mOpSneExtension.isTextValid(text, slotId));
    }

    // update nickname value
    // TODO
    public void testUpdateDataToDb() {
        String accountType = SimUtils.ACCOUNT_TYPE_USIM;
        ContentResolver resolver = mTargetContext.getContentResolver();

        //ArrayList<String> arrNickname = new ArrayList<String>();
        String updateNickname = "ghjkk";
        String oldNickname = "abc";
        int slotId = 0;
        //arrNickname.add(updateNickname);// 0
        //arrNickname.add(oldNickname);// 1
        long rawId = 0; // TODO how to get the rawId,// Contacts.CONTENT_URI
        int type = 0; // DB_UPDATE_NICKNAME = 0;DB_UPDATE_ANR=1
        // 1.sim
        accountType = SimUtils.ACCOUNT_TYPE_SIM;
        boolean updateSuccess = mOpSneExtension.updateDataToDb(slotId, accountType, resolver, updateNickname, oldNickname,
                rawId);
        assertFalse(updateSuccess);

        // 2.type not 0 wrong slot id,
        type = 1;
        updateSuccess = mOpSneExtension
                .updateDataToDb(-1, accountType, resolver, updateNickname, oldNickname, rawId);
        assertFalse(updateSuccess);

        // 3.usim--update
        accountType = SimUtils.ACCOUNT_TYPE_USIM;
        type = 0;
        updateSuccess = mOpSneExtension
                .updateDataToDb(slotId, accountType, resolver, updateNickname, oldNickname, rawId);
        assertTrue(updateSuccess);

        // 04--insert
        updateNickname = "newnicknamein";
        oldNickname = "";
        //arrNickname.add(updateNickname);
        //arrNickname.add(oldNickname);
        updateSuccess = mOpSneExtension
                .updateDataToDb(slotId, accountType, resolver, updateNickname, oldNickname, rawId);
        assertTrue(updateSuccess);

        // 05--delete--
        //arrNickname.clear();
        updateNickname = "";
        oldNickname = "haveaname";
        //arrNickname.add(updateNickname);
        //arrNickname.add(oldNickname);
        updateSuccess = mOpSneExtension
                .updateDataToDb(slotId, accountType, resolver, updateNickname, oldNickname, rawId);
        assertTrue(updateSuccess);
    }

    // buildsne value
    public void testBuildOperation() {
        String accountType = SimUtils.ACCOUNT_TYPE_USIM;
        int type = 1; // TYPE_OPERATION_SNE
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        String text = "nicknameoo";
        int backRef = 0;
        // 1.usim,right type
        assertTrue(mOpSneExtension.buildOperation(accountType, operationList, null, text, backRef));
        // 2.sim,
        accountType = SimUtils.ACCOUNT_TYPE_SIM;
        assertTrue(mOpSneExtension.buildOperation(accountType, operationList, null, text, backRef));
    }

    // build sne from curcor
    // public void testBuildOperationFromCursor() {
    // // buildOperationFromCursor(String accountType,
    // // ArrayList<ContentProviderOperation> operationList,
    // // final Cursor cursor, int index)
    // String accountType = SimUtils.ACCOUNT_TYPE_USIM;
    // ArrayList<ContentProviderOperation> operationList = new
    // ArrayList<ContentProviderOperation>();
    // int index = 7;// do not know ?? log info
    //
    // final int slotId = 0;
    // //final Uri iccUri = SimCardUtils.SimUri.getSimUri(slotId);
    // final Uri iccUri = Uri.parse("content://icc/pbr");//maybe other value
    // final String[] COLUMN_NAMES = new String[] { "index", "name", "number",
    // "emails", "additionalNumber", "groupIds" };
    // Cursor cursor = mTargetContext.getContentResolver().query(iccUri,
    // COLUMN_NAMES, null, null, null);
    // // 1.usim,
    // boolean buildSuccess = mOpSneExtension
    // .buildOperationFromCursor(accountType, operationList, cursor,
    // index);
    //
    // int sneColumnIdx = cursor.getColumnIndex("sne");
    // if (sneColumnIdx != -1) {
    // String nickname = cursor.getString(sneColumnIdx);
    // if (!TextUtils.isEmpty(nickname)) {
    // assertTrue(buildSuccess);
    // }
    // } else {
    // assertFalse(buildSuccess);
    // }
    // // 2.sim,
    // }
    // the other functions

    /**
    OpContactsListExtensionTest
    */
         // USIM card in slot(0)
    public void testBuildSimNickname() {
        String simNickname = null;
        String accountType = SimUtils.ACCOUNT_TYPE_USIM;
        int slotId = 0;
        // to construct an ContentValue---
        ContentValues values = new ContentValues();
        // values.put("", value);
        ArrayList<String> nicknameArray = new ArrayList<String>();
        String defValue = "d";
        int maxLength = SimUtils.getSneRecordMaxLen(slotId);

        // 01--sim,can not put it into values.try change the 01--02
        accountType = SimUtils.ACCOUNT_TYPE_SIM;
        String nickname = "abcdef";
        nicknameArray.add(nickname);
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        assertFalse(nickname.equals(simNickname));

        // 02--usim ,slotId--has sne;snelenth==17
        accountType = SimUtils.ACCOUNT_TYPE_USIM;
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        assertTrue(nickname.equals(simNickname));

        // test if the nickname is valid.
        // 03--usim,but the length of nickname is too long ,can't put it into
        // values.
        nicknameArray.clear();
        nickname = "wwwwweeeeetttttuuuuyyy";
        nicknameArray.add(nickname);
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        if (nickname.length() > maxLength) {
            assertFalse(nickname.equals(simNickname));
        }
        // 04--usim,the nickname array is null,so the returned value is null ,do
        // not the defValue.
        nicknameArray.clear();
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        assertFalse(defValue.equals(simNickname));

        // 05--usim,the nickname has chinese string ,
        nicknameArray.clear();
        nickname = "中秋lll";
        nicknameArray.add(nickname);
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        assertTrue(nickname.equals(values.get("sne")));

        // 06--usim,the nickname is all the chinese string,check the length.
        nicknameArray.clear();
        nickname = "中秋端午春中秋端午中秋";
        nicknameArray.add(nickname);
        simNickname = mOpSneExtension.buildSimNickname(accountType, values, nicknameArray, slotId, defValue);
        if (nickname.length() > ((maxLength - 1) >> 1)) {
            assertFalse(nickname.equals(simNickname));
        }
    }
}
