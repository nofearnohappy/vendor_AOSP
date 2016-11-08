package com.mediatek.contacts.sne.plugin;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.android.contacts.ContactsApplication;
import android.os.Looper;

import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountType.EditField;
import com.android.contacts.common.model.account.BaseAccountType.SimpleInflater;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.R;
import com.android.contacts.common.vcard.ProcessorBase;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.EncodeException;

import com.google.android.collect.Lists;

import com.mediatek.common.PluginImpl;
import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.ext.DefaultSneExtension;
import com.mediatek.contacts.simservice.SIMEditProcessor;
import com.mediatek.contacts.model.AccountWithDataSetEx;

import java.util.ArrayList;

@PluginImpl(interfaceName="com.mediatek.contacts.ext.ISneExtension")
public class OpSneExtension extends DefaultSneExtension {
    private final static String TAG = "OpSneExtension";

    protected static final int FLAGS_PERSON_NAME = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
            | EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME;
    //should be in sync with host app
    private static final String ACCOUNT_TYPE_USIM = "USIM Account";

    private Context mContext = null;
    private Context mHostAppContext = null;
    ArrayList<String> mNickNameArray = null;

    public OpSneExtension(Context context) {
        mContext = context;
    }

    private boolean buildNicknameValues(String accountType,
            ContentValues values, String nickName) {
        if (SimUtils.isUsim(accountType)) {
            values.put("sne", TextUtils.isEmpty(nickName) ? "" : nickName);
            return true;
        }
        return false;
    }

    private boolean isSneNicknameValid(String nickName, int subId) {
        if (TextUtils.isEmpty(nickName)) {
            LogUtils.d(TAG, "[isSneNicknameValid] nickname valid true");
            return true;
        }
        final int maxLength = SimUtils.getSneRecordMaxLen(subId);
        LogUtils.d(TAG, "[isSneNicknameValid] max sne length" + maxLength);
        try {
            GsmAlphabet.stringToGsm7BitPacked(nickName);
            LogUtils.d(TAG,
                    "[isSneNicknameValid] given sne length" + nickName.length());
            if (nickName.length() > maxLength) {
                LogUtils.d(TAG, "[isSneNicknameValid] length exceeds & false");
                return false;
            }
        } catch (EncodeException e) {
            if (nickName.length() > ((maxLength - 1) >> 1)) {
                LogUtils.d(TAG, "[isSneNicknameValid] exception & false");
                return false;
            }
        }
        return true;
    }

    private boolean updateDataToDb(int subId, String accountType,
            ContentResolver resolver, String updateNickname,
            String oldNickname, long rawId) {
        if (!SimUtils.hasSne(subId)) {
            LogUtils.e(TAG, "[updateDataToDb]DB_UPDATE_NICKNAME-error subId");
            return false;
        }
        return updateNicknameToDB(accountType, resolver, updateNickname,
                oldNickname, rawId);
    }

    private boolean updateNicknameToDB(String accountType,
            ContentResolver resolver, String updateNickname,
            String oldNickname, long rawId) {
        if (SimUtils.isUsim(accountType)) {
            ContentValues nicknamevalues = new ContentValues();
            String whereNickname = Data.RAW_CONTACT_ID + " = \'" + rawId + "\'"
                    + " AND " + Data.MIMETYPE + "='"
                    + Nickname.CONTENT_ITEM_TYPE + "'";

            updateNickname = TextUtils.isEmpty(updateNickname) ? ""
                    : updateNickname;
            LogUtils.d(TAG, "[updateNicknameToDB]whereNickname is="
                    + whereNickname + " updateNickname:=" + updateNickname);

            if (!TextUtils.isEmpty(updateNickname)
                    && !TextUtils.isEmpty(oldNickname)) {
                nicknamevalues.put(Nickname.NAME, updateNickname);
                int upNickname = resolver.update(Data.CONTENT_URI,
                        nicknamevalues, whereNickname, null);
                LogUtils.d(TAG, "[updateNickname] upNickname is " + upNickname);
            } else if (!TextUtils.isEmpty(updateNickname)
                    && TextUtils.isEmpty(oldNickname)) {
                nicknamevalues.put(Nickname.RAW_CONTACT_ID, rawId);
                nicknamevalues.put(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
                nicknamevalues.put(Nickname.NAME, updateNickname);
                Uri upNicknameUri = resolver.insert(Data.CONTENT_URI,
                        nicknamevalues);
                LogUtils.d(TAG, "[updateNickname] upNicknameUri is "
                        + upNicknameUri);
            } else if (TextUtils.isEmpty(updateNickname)) {
                // update nickname is null,delete name row
                int deleteNickname = resolver.delete(Data.CONTENT_URI,
                        whereNickname, null);
                LogUtils.d(TAG, "[updateNickname] deleteNickname is "
                        + deleteNickname);
            }
            return true;
        }
        return false;
    }

    private boolean buildSneOperation(String accountType,
            ArrayList<ContentProviderOperation> operationList, String nickname,
            int backRef) {
        LogUtils.d(TAG, "buildSneOperation entry");
        if (SimUtils.isUsim(accountType)) {
            // build SNE ContentProviderOperation
            LogUtils.d(TAG, "buildSneOperation isUSIM true");
            if (!TextUtils.isEmpty(nickname)) {
                LogUtils.d(TAG, "buildSneOperation nickname is not empty");
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Data.CONTENT_URI);
                LogUtils.d(TAG, "[buildSneOperation] nickname:" + nickname);
                builder.withValueBackReference(Nickname.RAW_CONTACT_ID, backRef);
                builder.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
                builder.withValue(Nickname.DATA, nickname);
                LogUtils.d(TAG, "[buildSneOperation] builder nickname:"
                        + nickname);
                operationList.add(builder.build());
                LogUtils.d(TAG, "[buildSneOperation] operationList:"
                        + operationList);
                return true;
            }
        }
        return true;
    }

    public boolean buildOperationFromCursor(
            ArrayList<ContentProviderOperation> operationList,
            final Cursor cursor, int index) {
        // build SNE ContentProviderOperation from cursor
        int sneColumnIdx = cursor.getColumnIndex("sne");
        LogUtils.d(TAG, "[buildOperationFromCursor] sneColumnIdx:"
                + sneColumnIdx);
        if (sneColumnIdx != -1) {
            String nickname = cursor.getString(sneColumnIdx);
            LogUtils.d(TAG, "[buildOperationFromCurson] nickname:" + nickname);
            if (!TextUtils.isEmpty(nickname)) {
                LogUtils.d(TAG,
                        "[buildOperationFromCursor] nickname is not empty");
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Nickname.RAW_CONTACT_ID, index);
                builder.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
                builder.withValue(Nickname.DATA, nickname);
                LogUtils.d(TAG, "[buildOperationFromCursor] nickname added"
                        + nickname);
                operationList.add(builder.build());
                return true;
            }
        }
        return false;
    }

    private boolean buildNicknameValueForInsert(int subId, ContentValues cv,
            String nickname) {
        if (SimUtils.hasSne(subId)) {
            LogUtils.d(TAG, "buildNicknameValueForInsert plugin");
            cv.put("sne", TextUtils.isEmpty(nickname) ? "" : nickname);
            return true;
        }
        return false;
    }

    private boolean hasSne(int subId) {
        LogUtils.d(TAG, "hasSne plugin");
        return SimUtils.hasSne(subId);

    }

    private boolean isNickname(String mimeType) {
        LogUtils.d(TAG, "isNickname plugin");
        return Nickname.CONTENT_ITEM_TYPE.equals(mimeType);
    }

    /** start this API is used contactlist */
    public String buildSimNickname(String accountType,
            ArrayList<String> nicknameArray, int subId, String defValue) {
        LogUtils.d(TAG, "buildSimNickname Entry");
        if (SimUtils.isUsim(accountType)) {
            LogUtils.d(TAG, "buildSimNickname USIM is true");
            boolean hasSne = SimUtils.hasSne(subId);
            String simNickname = null;
            if (!nicknameArray.isEmpty() && SimUtils.hasSne(subId)) {
                LogUtils.d(TAG,
                        "[buildSimNickname] nickname array is not empty");
                LogUtils.d(TAG, "[buildSimNickname] hasSne is:" + hasSne);
                simNickname = nicknameArray.remove(0);
                simNickname = TextUtils.isEmpty(simNickname) ? "" : simNickname;
                LogUtils.d(TAG, "buildSimNickname simNickname" + simNickname);
                int len = SimUtils.getSneRecordMaxLen(subId);

                LogUtils.d(TAG, "[buildSimNickname]before Endode simNickname="
                        + simNickname);
                try { // the code copy from CustomAasActivity.
                    GsmAlphabet.stringToGsm7BitPacked(simNickname);
                    if (simNickname.length() > len) {
                        simNickname = ""; // simNickname.substring(0, len);
                    }
                } catch (EncodeException e) {
                    LogUtils.e(TAG,
                            "Error at GsmAlphabet.stringToGsm7BitPacked()!");
                    if (simNickname.length() > ((len - 1) >> 1)) {
                        simNickname = ""; // simNickname.substring(0, len);
                    }
                }
                LogUtils.d(TAG, "[buildSimNickname]after Endode simNickname="
                        + simNickname);
            }
            return simNickname;
        }
        return defValue;
    }

    /**
     * New interface implementation here
     */

    public void onEditorBindEditors(RawContactDelta entity, AccountType type,
            int subId) {
        LogUtils.d(TAG, "[onEditorBindEditors] Entry");
        boolean hasSne = hasSne(subId);
        if (SimUtils.isUsim(type.accountType) && hasSne) {
            LogUtils.d(TAG, "[onEditorBindEditors] isUSIM");
            SimUtils.setCurrentSubId(subId);
            addDataKindNickname(type);
            DataKind dataKind = type
                    .getKindForMimetype(Nickname.CONTENT_ITEM_TYPE);
            if (dataKind != null) {
                LogUtils.d(TAG, "[onEditorBindEditors] datakind not null");
                updateNickname(dataKind, hasSne);
            }
            LogUtils.d(TAG, "[onEditorBindEditors] ensurekindexists");
            RawContactModifier.ensureKindExists(entity, type,
                    Nickname.CONTENT_ITEM_TYPE);
        }

    }

    private void updateNickname(DataKind kind, boolean hasSne) {
        LogUtils.d(TAG, "[updateNickname]for USIM,hasSne:" + hasSne);
        if (hasSne) {
            LogUtils.d(TAG, "[onEditorBindEditors] has sne true");
            if (kind.fieldList == null) {
                kind.fieldList = Lists.newArrayList();
            } else {
                kind.fieldList.clear();
            }
            kind.fieldList.add(new EditField(Nickname.NAME,
                    R.string.nicknameLabelsGroup, FLAGS_PERSON_NAME));
        } else if (!hasSne) {
            LogUtils.d(TAG, "[onEditorBindEditors] has sne false");
            kind.fieldList = null;
        }
    }
    
    @SuppressWarnings("illegalcatch")
    private void addDataKindNickname(AccountType accountType) {
        // 1.check if instanceof UsimAccountType,then can cast it to
        // UsimAccountType for invoke it's method
        // 2.refer:addDataKindNickname()
        LogUtils.d(TAG, "[addDataKindNickname]Entry");
        if (accountType instanceof UsimAccountType) {
            LogUtils.d(TAG,
                    "[addDataKindNickname]account type is instance of USIM");
            UsimAccountType uaccountType = (UsimAccountType) accountType;
            DataKind kind;
            try {
                kind = uaccountType.addKind(new DataKind(
                        Nickname.CONTENT_ITEM_TYPE,
                        R.string.nicknameLabelsGroup, 115, true));
            } catch (Exception de) {
                LogUtils.d(TAG,
                        "[addDataKindNickname]addkind Exception & return");
                return;
            }
            kind.typeOverallMax = 1;
            kind.actionHeader = new SimpleInflater(R.string.nicknameLabelsGroup);
            kind.actionBody = new SimpleInflater(Nickname.NAME);
            kind.defaultValues = new ContentValues();
            kind.defaultValues.put(Nickname.TYPE, Nickname.TYPE_DEFAULT);
            LogUtils.d(TAG, "[addDataKindNickname]adding kind");
        }
    }

    private void fillNickNameArray(Uri sourceUri) {
        LogUtils.d(TAG, "[fillNickNameArray] Entry");
        LogUtils.d(TAG, "[fillNickNameArray] mNickNameArray is empty");
        mNickNameArray = new ArrayList<String>();

        final String[] projection = new String[] { Contacts.Data.MIMETYPE,
                Contacts.Data.DATA1, };

        ContentResolver resolver = mContext.getContentResolver();
        Cursor c = resolver.query(sourceUri, projection, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                String mimeType = c.getString(0);
                if (isNickname(mimeType)) {
                    LogUtils.d(TAG, "[fillNickNameArray] mimetype of nickname");
                    String nickName = c.getString(1);
                    LogUtils.d(TAG, "[fillNickNameArray] nickname is:"
                            + nickName);
                    mNickNameArray.add(nickName);
                }
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }

    }

    // called when phone contacts are copied to USIM
    public void copySimSneToAccount(
            ArrayList<ContentProviderOperation> operationList,
            Account targetAccount, Uri sourceUri, int backRef) {
        // 1.get simNickname by uri
        String simNickname = null;
        LogUtils.d(TAG, "[copySimSneToAccount] Entry and sourceUri: "
                + sourceUri);
        fillNickNameArray(sourceUri);
        LogUtils.d(TAG, "[copySimSneToAccount] after fillNickNameArray");
        LogUtils.d(TAG, "[copySimSneToAccount] backRef " + backRef);
        // ContentValues updatevalues = new ContentValues();
        AccountWithDataSetEx account  = (AccountWithDataSetEx) targetAccount;
        int subId = account.getSubId();
        LogUtils.d(TAG, "[copySimSneToAccount] subId " + subId);
        simNickname = buildSimNickname(targetAccount.type, mNickNameArray,
                subId, simNickname);
        LogUtils.d(TAG,
                "[copySimSneToAccount] after buildSimNickname simNickname is:"
                        + simNickname);
        buildSneOperation(targetAccount.type, operationList, simNickname,
                backRef);
        LogUtils.d(TAG, "[copySimSneToAccount] after buildSneOperation");
    }

    public int importSimSne(ArrayList<ContentProviderOperation> operationList,
            Cursor cursor, int loopCheck) {
        // 1.get sne from cursor
        // 2.build it to operationList
        // default return 0
        LogUtils.d(TAG, "[importSimSne] Entry");
        return buildOperationFromCursor(operationList, cursor, loopCheck) ? 1
                : 0;
    }

    public void editSimSne(Intent intent, long indexInSim, int subId,
            long rawContactId) {
        // 1. get sNickname like getRawContactDataFromIntent()
        // 2. get mOldNickname like setOldRawContactData()
        // 3.check sNickname isTextValid() like editSIMContact()
        // 4.buildNicknameValueForInsert like setUpdateValues()
        // 5.save to sim first---if mOldNickname is empty,need insert,else
        // update
        // 6.then save to contacts db:updateDataToDb() like editSIMContact()

        String sNickname = null;
        String sOldNickname = null;
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        ArrayList<RawContactDelta> oldSimData = new ArrayList<RawContactDelta>();
        LogUtils.d(TAG, "[editSimSne] Entry");
        newSimData = intent.getParcelableArrayListExtra("simData");
        oldSimData = intent.getParcelableArrayListExtra("simOldData");
        String accountType = newSimData.get(0).getValues()
                .getAsString(RawContacts.ACCOUNT_TYPE);
        LogUtils.d(TAG, "[editSimSne] Accountype from newSimData:" + accountType);
        String data = null;
        String mimeType = null;
        if (!hasSne(subId) || !SimUtils.isUsim(accountType)) {
            // sim is not USIM and slot id dont have sne field
            LogUtils.d(TAG, "[editSimSne] do nothing & return ");
            return;
        }
        // calculate the newNickname
        int kindCount = newSimData.get(0).getContentValues().size();
        if (accountType.equals(ACCOUNT_TYPE_USIM)) {
            for (int countIndex = 0; countIndex < kindCount; countIndex++) {
                mimeType = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.MIMETYPE);
                data = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.DATA1);
                LogUtils.d(TAG, "[editSimSne]countIndex:" + countIndex
                        + ",mimeType:" + mimeType + "data:" + data);
                if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // this means this type is nickname type
                    sNickname = data;
                    sNickname = TextUtils.isEmpty(sNickname) ? "" : sNickname;
                    LogUtils.d(TAG, "[editSimSne] updated nickname is"
                            + sNickname);
                }

            }

        }

        // calculate the OldNickname
        if (oldSimData != null) {
            int oldCount = oldSimData.get(0).getContentValues().size();
            for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
                mimeType = oldSimData.get(0).getContentValues().get(oldIndex)
                        .getAsString(Data.MIMETYPE);
                data = oldSimData.get(0).getContentValues().get(oldIndex)
                        .getAsString(Data.DATA1);
                LogUtils.d(TAG, "[getOldRawContactData]Data.MIMETYPE: "
                        + mimeType + ",data:" + data);
                if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    sOldNickname = data;
                    LogUtils.d(TAG, "[editSimSne]sOldNickname=" + sOldNickname);
                }
            }
        }
        ContentResolver resolver = mContext.getContentResolver();
        // Write to PHB
        LogUtils.d(TAG, "[editSimSne] RawcontactId" + rawContactId);
        updateDataToDb(subId, accountType, resolver, sNickname, sOldNickname,
                rawContactId);

    }

    public int checkNickName(ProcessorBase processor, Intent intent,
            boolean checkAlone, int subId) {
        // 1.get data from intent,refer:getRawContactDataFromIntent
        // 2.check if is not empty
        // 3.check length and others...
        // 4.if checkAlone is true,you can toast,false should not
        LogUtils.d(TAG, "[checkNickName] checkNickName entry");
        LogUtils.d(TAG, "[checkNickName] checkalone value:" + checkAlone);
        LogUtils.d(TAG, "[checkNickName] subId value:" + subId);
        String sNickname = null;
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        newSimData = intent.getParcelableArrayListExtra("simData");
        String accountType = newSimData.get(0).getValues()
                .getAsString(RawContacts.ACCOUNT_TYPE);
        String data = null;
        String mimeType = null;
        int kindCount = newSimData.get(0).getContentValues().size();

        if (accountType.equals(ACCOUNT_TYPE_USIM)) {
            for (int countIndex = 0; countIndex < kindCount; countIndex++) {
                mimeType = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.MIMETYPE);
                data = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.DATA1);
                LogUtils.d(TAG, "[checkNickName]countIndex:" + countIndex
                        + ",mimeType:" + mimeType + "data:" + data);
                if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // this means this type is nickname type
                    sNickname = data;
                    sNickname = TextUtils.isEmpty(sNickname) ? "" : sNickname;
                    LogUtils.d(TAG, "[checkNickName] updated nickname is"
                            + sNickname);
                }

            }

        }

        if (TextUtils.isEmpty(sNickname)) {
            LogUtils.d(TAG, "[checkNickName] nickname is empty");
            LogUtils.d(TAG, "[checkNickName] return alue:" + "1");
            return 1;
        }
        //L migration need to pass subId as passed from interface
        if (!isSneNicknameValid(sNickname, subId)) {
            {
                if (checkAlone) {
                    LogUtils.d(TAG, "[checkNickName] check alone is true");

                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    mHostAppContext = ContactsApplication.getInstance();
                    if (processor instanceof SIMEditProcessor) {
                        SIMEditProcessor uprocessor = (SIMEditProcessor) processor;
                        LogUtils.d(TAG,
                                "[checkNickName] calling back to fragment");
                        uprocessor.deliverCallbackAndBackToFragment();
                    }
                    uiHandler.post(new Runnable() {
                        public void run() {
                            LogUtils.d(TAG,
                                    "[validateAndUpdateSne] return false as nickname is Invalid toast");
                            Toast.makeText(
                                    mHostAppContext,
                                    mContext.getString(com.mediatek.op06.plugin.R.string.nickname_too_long),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    LogUtils.d(TAG,
                            "[validateAndUpdateSne] return false as nickname is Invalid");
                }

            }
            LogUtils.d(TAG, "[checkNickName] nickname is not valid");
            return 2;
        } else {
            LogUtils.d(TAG, "[checkNickName] nickname is valid");
            LogUtils.d(TAG, "[checkNickName] return alue:" + "0");
            return 0;
        }
    }

    public void updateValues(Intent intent, int subId,
            ContentValues preContentValues) {
        String sNickname = null;

        if (!SimUtils.hasSne(subId)) {
            LogUtils.d(TAG, "[updateValues] hasSne false & return");
            return;
        }
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        newSimData = intent.getParcelableArrayListExtra("simData");
        String accountType = newSimData.get(0).getValues()
                .getAsString(RawContacts.ACCOUNT_TYPE);
        SimUtils.setCurrentSubId(subId);
        String data = null;
        String mimeType = null;
        int kindCount = newSimData.get(0).getContentValues().size();
        if (accountType.equals(ACCOUNT_TYPE_USIM)) {
            for (int countIndex = 0; countIndex < kindCount; countIndex++) {
                mimeType = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.MIMETYPE);
                data = newSimData.get(0).getContentValues().get(countIndex)
                        .getAsString(Data.DATA1);
                LogUtils.d(TAG, "[updateValues]countIndex:" + countIndex
                        + ",mimeType:" + mimeType + "data:" + data);
                if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // this means this type is nickname type
                    sNickname = data;
                    sNickname = TextUtils.isEmpty(sNickname) ? "" : sNickname;
                    LogUtils.d(TAG, "[updateValues] updated nickname is"
                            + sNickname);
                }

            }

        }

        {
            LogUtils.d(TAG, "[updateValues] hasSne and sne is:" + sNickname);
            preContentValues.put("sne", TextUtils.isEmpty(sNickname) ? ""
                    : sNickname);
        }

    }

    public void updateValuesforCopy(Uri sourceUri, int subId,
            String accountType, ContentValues simContentValues) {

        LogUtils.d(TAG, "[updateValuesforCopy] Entry sourceUri is :"
                + sourceUri);
        LogUtils.d(TAG, "[updateValuesforCopy] Entry subId is :" + subId);
        ArrayList<String> nickNameArray = new ArrayList<String>();

        final String[] projection = new String[] { Contacts.Data.MIMETYPE,
                Contacts.Data.DATA1, };
        if (!hasSne(subId)) {
            // nothing to to
            LogUtils.d(TAG, "[updateValuesforCopy] No sne field in SIM");
            return;
        }

        String simNickname = null;
        ContentResolver resolver = mContext.getContentResolver();
        Cursor c = resolver.query(sourceUri, projection, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                String mimeType = c.getString(0);
                if (isNickname(mimeType)) {
                    LogUtils.d(TAG,
                            "[updateValuesforCopy] mimetype of nickname");
                    String nickName = c.getString(1);
                    LogUtils.d(TAG, "[updateValuesforCopy] nickname is:"
                            + nickName);
                    nickNameArray.add(nickName);
                }
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }

        simNickname = buildSimNickname(accountType, nickNameArray, subId,
                simNickname);
        LogUtils.d(TAG, "[updateValuesforCopy] put values nickname is:"
                + simNickname);
        simContentValues.put("sne", simNickname);

    }

}
