package com.mediatek.contacts.aas.plugin;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.contacts.ContactsApplication;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType.EditField;
import com.android.contacts.common.model.account.AccountType.EditType;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
//import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.editor.LabeledEditorView;
import com.android.contacts.editor.TextFieldsEditorView;

import com.google.android.collect.Lists;

//import com.mediatek.common.telephony.AlphaTag;
import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.uicc.AlphaTag;
import com.mediatek.contacts.ext.DefaultAasExtension;
import com.mediatek.contacts.model.UsimAccountType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@PluginImpl(interfaceName="com.mediatek.contacts.ext.IAasExtension")
public class OpAasExtension extends DefaultAasExtension {
    public static final int TYPE_FOR_PHONE_NUMBER = 0;
    public static final int TYPE_FOR_ADDITIONAL_NUMBER = 1;
    public static final int VIEW_UPDATE_NONE = 0;
    public static final int VIEW_UPDATE_HINT = 1;
    public static final int VIEW_UPDATE_VISIBILITY = 2;
    private final static String TAG = "OpAasExtension";
    public static final int TYPE_OPERATION_AAS = 0;
    public static final int VIEW_TYPE_SUB_KIND_TITLE_ENTRY = 6;

    public static final int OPERATION_CONTACT_INSERT = 1;
    public static final int OPERATION_CONTACT_EDIT = 2;
    public static final int OPERATION_CONTACT_COPY = 3;
    //should be in sync with host app
    private static final int FLAGS_USIM_NUMBER = EditorInfo.TYPE_CLASS_PHONE;
    protected static final int FLAGS_EMAIL = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    protected static final int FLAGS_PHONE = EditorInfo.TYPE_CLASS_PHONE;
    private Context mContext = null;
    private static OpAasExtension sOpAasExtension = null;
    private Context mhostAppContext = null;
    private ArrayList<Anr> mOldAnrsList = new ArrayList<Anr>();
    private ArrayList<Anr> mAnrsList = new ArrayList<Anr>();
    private ArrayList<Anr> mCopyAnrList = null;
    //private ArrayList<ContactDetailFragment.DetailViewEntry> mAnrEntries = new ArrayList<ContactDetailFragment.DetailViewEntry>();
    private Uri mCopyUri = null;
    private int mInsertFlag = 0;
    private static final String SIM_NUM_PATTERN = "[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*";
    private static final int SLOT_ID1 = com.android.internal.telephony.PhoneConstants.SIM_ID_1;
    private static final int SLOT_ID2 = com.android.internal.telephony.PhoneConstants.SIM_ID_2;
    private static boolean isBack = false;

    public OpAasExtension(Context context) {
        mContext = context;
        sOpAasExtension = this;
        mhostAppContext = ContactsApplication.getInstance();
    }

    public void setCurrentSlot(int slotId) {

        LogUtils.d(TAG, "[setCurrentSlot] slot: = " + slotId);
        SimUtils.setCurrentSlot(slotId);
    }

    public boolean shouldStopSave(boolean isSimContact) {
        LogUtils.d(TAG, "[shouldStopSave] isSimContact: = " + isSimContact);
        LogUtils.d(TAG, "[shouldStopSave] isBack: = " + isBack);
        if ((isSimContact == true) && isBack) {
            return true;
        }
        return false;
    }

    // IP start
    public void setCurrentSubId(int subId) {
        LogUtils.d(TAG, "[setCurrentSubId] subId: = " + subId);
        SimUtils.setCurrentSubId(subId);
    }

    public void updatePhoneType(int subId, DataKind kind) {
        if (kind.typeList == null) {
            kind.typeList = Lists.newArrayList();
        } else {
            kind.typeList.clear();
        }
        List<AlphaTag> atList = SimUtils.getAAS(subId);
        final int specificMax = -1;
        LogUtils.d(TAG, "[updatePhoneType] subId = " + subId + " specificMax="
                + specificMax);

        kind.typeList.add((new EditType(Anr.TYPE_AAS, Phone
                .getTypeLabelResource(Anr.TYPE_AAS))).setSpecificMax(
                specificMax).setCustomColumn(String.valueOf(-1)));
        for (AlphaTag tag : atList) {
            final int recordIndex = tag.getRecordIndex();
            LogUtils.d(TAG, "updatePhoneType() label=" + tag.getAlphaTag());

            kind.typeList.add((new EditType(Anr.TYPE_AAS, Phone
                    .getTypeLabelResource(Anr.TYPE_AAS))).setSpecificMax(
                    specificMax).setCustomColumn(String.valueOf(recordIndex)));

        }
        kind.typeList.add((new EditType(Phone.TYPE_CUSTOM, Phone
                .getTypeLabelResource(Phone.TYPE_CUSTOM)))
                .setSpecificMax(specificMax));

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER,
                com.android.contacts.R.string.phoneLabelsGroup,
                FLAGS_USIM_NUMBER));
    }

    private void ensureKindExists(RawContactDelta state, String mimeType,
            DataKind kind, int subId) {
        if (kind != null) {
            ArrayList<ValuesDelta> values = state.getMimeEntries(mimeType);
            final int slotAnrSize = SimUtils.getAnrCount(subId);
            if (values != null && values.size() == slotAnrSize + 1) {
                // primary number + slotNumber size
                int anrSize = 0;
                for (ValuesDelta value : values) {
                Integer isAnr = value.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                if (isAnr != null && (isAnr.intValue() == 1)) {
                    anrSize++;
                }
        }
                LogUtils.d(TAG, "ensureAASKindExists() size=" + values.size() + " slotAnrSize=" + slotAnrSize + " anrSize=" + anrSize);
                if (anrSize < slotAnrSize && values.size() > 1) {
            for (int i = 1; i < values.size() ; i++) {
                values.get(i).put(Data.IS_ADDITIONAL_NUMBER, 1);
            }
            }

                return;
            }
            if (values == null || values.isEmpty()) {
                LogUtils.d(TAG,
                        "ensureKindExists() Empty, insert primary:1 and anr:"
                                + slotAnrSize);
                // Create child when none exists and valid kind
                final ValuesDelta child = RawContactModifier.insertChild(state,
                        kind);
                if (kind.mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    child.setFromTemplate(true);
                }

                for (int i = 0; i < slotAnrSize; i++) {
                    final ValuesDelta slotChild = RawContactModifier
                            .insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            } else {
                int pnrSize = 0;
                int anrSize = 0;
                if (values != null) {
                    for (ValuesDelta value : values) {
                        Integer isAnr = value
                                .getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                        if (isAnr != null && (isAnr.intValue() == 1)) {
                            anrSize++;
                        } else {
                            pnrSize++;
                        }
                    }
                }
                LogUtils.d(TAG, "ensureKindExists() pnrSize=" + pnrSize
                        + ", anrSize=" + slotAnrSize);
                if (pnrSize < 1) {
                    // insert a empty primary number if not exists.
                    final ValuesDelta slotChild = RawContactModifier.insertChild(state, kind);
                    slotChild.put(Data.DATA2, 2);
                }
                for (; anrSize < slotAnrSize; anrSize++) {
                    // insert additional numbers if not full.
                    final ValuesDelta slotChild = RawContactModifier
                            .insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            }
        }
    }

    public boolean ensurePhoneKindForEditor(AccountType type, int subId,
            RawContactDelta entity) {
        SimUtils.setCurrentSubId(subId);
        isBack = false;
        String accountType = entity.getAccountType();

        if (SimUtils.isUsim(accountType)) {
            DataKind dataKind = type
                    .getKindForMimetype(Phone.CONTENT_ITEM_TYPE);
            if (dataKind != null) {
                updatePhoneType(subId, dataKind);
            }
            ensureKindExists(entity, Phone.CONTENT_ITEM_TYPE, dataKind, subId);
        }
        return true; // need to check later
    }

    public boolean handleLabel(DataKind kind, ValuesDelta entry,
            RawContactDelta state) {

        String accountType = state.getAccountType();

        if (SimUtils.isSim(accountType) && SimUtils.isPhone(kind.mimeType)) {
            LogUtils.d(TAG, "handleLabel, hide label for sim card.");
            return true;
        }

        if (SimUtils.isUsim(accountType)
                && SimUtils.isPhone(kind.mimeType)
                && !SimUtils.IS_ADDITIONAL_NUMBER.equals(entry
                        .getAsString(Data.IS_ADDITIONAL_NUMBER))) {
            // primary number, hide phone label
            LogUtils.d(TAG, "handleLabel, hide label for primary number.");
            return true;
        }
        if (SimUtils.isUsim(accountType)
                && Email.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
            LogUtils.d(TAG, "handleLabel, hide label for email");
            return true;
        }

        return false;
    }

    public ArrayList<ValuesDelta> rebuildFromState(RawContactDelta state,
            String mimeType) {

        String accountType = state.getAccountType();
        if (SimUtils.isPhone(mimeType)) {
            ArrayList<ValuesDelta> values = state.getMimeEntries(mimeType);
            if (values != null) {
                ArrayList<ValuesDelta> orderedDeltas = new ArrayList<ValuesDelta>();
                for (ValuesDelta entry : values) {
                    if (isAdditionalNumber(entry)) {
                        if (SimUtils.isUsim(accountType)) {
                            orderedDeltas.add(entry);
                        } else {
                            orderedDeltas.add(entry);
                        }
                    } else {
                        if (SimUtils.isUsim(accountType)) {
                            // add primary number to first.
                            orderedDeltas.add(0, entry);
                        } else {
                            orderedDeltas.add(entry);
                        }
                    }
                }
                return orderedDeltas;
            }
        }
        return super.rebuildFromState(state, mimeType);
    }

    public boolean updateView(RawContactDelta state, View view,
            ValuesDelta entry, int action) {
        int type = (entry == null) ? 0 : (isAdditionalNumber(entry) ? 1 : 0);
        String accountType = state.getAccountType();
        LogUtils.d(TAG, "updateView(), type=" + type + " action=" + action
                + " accountType=" + accountType);
        switch (action) {
        case VIEW_UPDATE_HINT:
            if (SimUtils.isUsim(accountType)) {
                if (view instanceof TextView) {
                    if (type == STRING_PRIMART) {
                        ((TextView) view)
                                .setHint(mContext
                                        .getResources()
                                        .getString(
                                                com.mediatek.op03.plugin.R.string.aas_phone_primary));
                    } else if (type == STRING_ADDITINAL) {
                        ((TextView) view)
                                .setHint(mContext
                                        .getResources()
                                        .getString(
                                                com.mediatek.op03.plugin.R.string.aas_phone_additional));
                    }
                } else {
                    LogUtils.e(TAG,
                            "updateView(), VIEW_UPDATE_HINT but view is not a TextView");
                }
            }
            break;
        case VIEW_UPDATE_VISIBILITY:
            if (SimUtils.isUsim(accountType)) {
                view.setVisibility(View.GONE);
            } else {
                return false;
            }
            break;
        case VIEW_UPDATE_DELETE_EDITOR:
            if (!SimUtils.isUsim(accountType)) {
                return false;
            }
            break;
        case VIEW_UPDATE_LABEL:
            if (isBack && SimUtils.isUsim(accountType)) {
                isBack = false;
                final ValuesDelta values = state.getValues();
                if (!values.isVisible())
                    return true;
                final String dataSet = values.getAsString(RawContacts.DATA_SET);
                final AccountType accType = AccountTypeManager.getInstance(
                        mhostAppContext).getAccountType(accountType, dataSet);
                DataKind dataKind = accType
                        .getKindForMimetype(Phone.CONTENT_ITEM_TYPE);
                //get subId here & pass to below API, as host has already called the setcurrentsubid before entry editor
                int subId = SimUtils.getCurSubId();
                updatePhoneType(subId, dataKind);
                updateEditorViewsLabel((ViewGroup) view);
            }
            break;

        default:
            break;
        }
        return true;
    }

    public int getMaxEmptyEditors(RawContactDelta state, String mimeType) {
        String accountType = state.getAccountType();
        LogUtils.d(TAG, "getMaxEmptyEditors() accountType=" + accountType
                + "mimeType=" + mimeType);
        if (SimUtils.isUsim(accountType) && SimUtils.isPhone(mimeType)) {
            //Host at the entry have already set for subId, we will get subId from simutils
            int subId = SimUtils.getCurSubId();
            int max = SimUtils.getAnrCount(subId) + 1;
            LogUtils.d(TAG, "getMaxEmptyEditors() max=" + max);
            return max;
        }
        return super.getMaxEmptyEditors(state, mimeType);
    }

    public String getCustomTypeLabel(int type, String customColumn) {
        LogUtils.d(TAG, "getCustomTypeLabel() type=" + type + " customColumn="
                + customColumn + " subId=" + SimUtils.getCurSubId());
        if (SimUtils.isUsim(SimUtils.getCurAccount())
                && SimUtils.isAasPhoneType(type)) {
            if (!TextUtils.isEmpty(customColumn)) {
                int aasIdx = Integer.valueOf(customColumn).intValue();
                if (aasIdx > 0) {
                    final String tag = SimUtils.getAASById(
                            SimUtils.getCurSubId(), aasIdx);
                    LogUtils.d(TAG, "getCustomTypeLabel() index" + aasIdx
                            + " tag=" + tag);
                    return tag;
                }
            }
            return mContext.getResources().getString(
                    com.mediatek.op03.plugin.R.string.aas_phone_type_none);
        }
        return null;
    }

    public boolean rebuildLabelSelection(RawContactDelta state, Spinner label,
            ArrayAdapter<EditType> adapter, EditType item, DataKind kind) {
        if (item == null || kind == null) {
            super.rebuildLabelSelection(state, label, adapter, item, kind);
            return false;
        }
        if (SimUtils.isUsim(state.getAccountType())
                && SimUtils.isPhone(kind.mimeType)
                && SimUtils.isAasPhoneType(item.rawValue)) {
            for (int i = 0; i < adapter.getCount(); i++) {
                EditType type = adapter.getItem(i);
                if (type.customColumn != null
                        && type.customColumn.equals(item.customColumn)) {
                    label.setSelection(i);
                    LogUtils.d(TAG, "rebuildLabelSelection() position=" + i);
                    return true;
                }
            }
        }
        super.rebuildLabelSelection(state, label, adapter, item, kind);
        return false;
    }

    public boolean onTypeSelectionChange(RawContactDelta rawContact,
            ValuesDelta entry, DataKind kind,
            ArrayAdapter<EditType> editTypeAdapter, EditType select,
            EditType type) {
        String accountType = rawContact.getAccountType();
        LogUtils.d(TAG, " onTypeSelectionChange Entry: accountType= "
                + accountType);
        if (SimUtils.isUsim(accountType) && SimUtils.isPhone(kind.mimeType)) {

            if (type == select) {
                Log.i(TAG, "same select");
                return true;
            }
            if (Phone.TYPE_CUSTOM == select.rawValue) {
                Log.i(TAG, "[onTypeSelectionChange] Custom Selected");
                onTypeSelectionChange(select.rawValue);
                isBack = true;

            } else {
                type = select; // modifying the type of host app so passed in
                // para
                Log.i(TAG, "[onTypeSelectionChange] different Selected");
                entry.put(kind.typeColumn, type.rawValue);
                // insert aas index to entry.
                updatemEntryValue(entry, type); // TODO it should be
                                                // SimUtilsPlugin.updatemEntryValue(entry,
                                                // type);
                // rebuildLabel();
                // onLabelRebuilt(); not required
            }
            return true;
        }
        return false;
    }

    private void onTypeSelectionChange(int position) {
        LogUtils.d(TAG, " onTypeSelectionChange private");
        if (SimUtils.isUsim(SimUtils.getCurAccount())) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("com.mediatek.contacts.action.EDIT_AAS");
            int subId = SimUtils.getCurSubId();
            LogUtils.d(TAG, " onTypeSelectionChange internal: subId to fill in slot_key= "
                + subId);
            intent.putExtra(SimUtils.KEY_SLOT, SimUtils.getCurSubId());
            LogUtils.d(TAG, " call for startActivity");
            mContext.startActivity(intent);
        }

    }

    public EditType getCurrentType(ValuesDelta entry, DataKind kind,
            int rawValue) {
        if (SimUtils.isAasPhoneType(rawValue)) {
            LogUtils.d(TAG, "getCurrentType return getAasEditType");
            return getAasEditType(entry, kind, rawValue);
        }
        LogUtils.d(TAG, "getCurrentType calling default");
        return super.getCurrentType(entry, kind, rawValue);
    }

    public static boolean updatemEntryValue(ValuesDelta entry, EditType type) {
        if (SimUtils.isAasPhoneType(type.rawValue)) {
            entry.put(Phone.LABEL, type.customColumn);
            return true;
        }
        return false;
    }

    // IP end

    // Amit added

    // -----------for SIMImportProcessor.java start--------------//
    // interface will be called when USIM part are copied from USIM to create
    // master database
    public void updateOperation(String accountType,
            ContentProviderOperation.Builder builder, Cursor cursor, int type) {
        LogUtils.d(TAG, "[updateOperation] Entry type: " + type);
        LogUtils.d(TAG, "[updateOperation] Entry accountType: " + accountType);
        switch (type) {
        case TYPE_FOR_ADDITIONAL_NUMBER:
            checkAasOperationBuilder(accountType, builder, cursor);
        }
    }

    private boolean checkAasOperationBuilder(String accountType,
            ContentProviderOperation.Builder builder, Cursor cursor) {
        if (SimUtils.isUsim(accountType)) {
            int aasColumn = cursor.getColumnIndex("aas");
            LogUtils.d(TAG, "[checkAasOperationBuilder] aasColumn " + aasColumn);
            if (aasColumn >= 0) {
                String aas = cursor.getString(aasColumn);
                LogUtils.d(TAG, "[checkAasOperationBuilder] aas " + aas);
                builder.withValue(Data.DATA2, Anr.TYPE_AAS);
                builder.withValue(Data.DATA3, aas);
            }
            return true;
        }
        return false;
    }

    // -----------for SIMImportProcessor.java ends--------------//

    // -----------for CopyProcessor.java starts--------//

    private boolean buildAnrOperation(String accountType,
            ArrayList<ContentProviderOperation> operationList,
            ArrayList anrList, int backRef) {
        if (SimUtils.isUsim(accountType)) {
            // build Anr ContentProviderOperation
            for (Object obj : anrList) {
                Anr anr = (Anr) obj;
                if (!TextUtils.isEmpty(anr.mAdditionNumber)) {
                    LogUtils.d(TAG, "additionalNumber=" + anr.mAdditionNumber
                            + " aas=" + anr.mAasIndex);

                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newInsert(Data.CONTENT_URI);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID,
                            backRef);
                    builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    builder.withValue(Data.DATA2, Anr.TYPE_AAS);
                    int aasIndex = SimUtils.getAasIndexByName(anr.mAasIndex,
                            SimUtils.getCurSubId());

                    builder.withValue(Phone.NUMBER, anr.mAdditionNumber);
                    // builder.withValue(Data.DATA3, anr.mAasIndex);
                    builder.withValue(Data.DATA3, String.valueOf(aasIndex));

                    builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
                    operationList.add(builder.build());
                }
            }
            return true;
        }
        return false;
    }

    private int mCopyCount = 0;
    private ArrayList<Anr> additionalArray = new ArrayList<Anr>();

    public void updateValuesforCopy(Uri sourceUri, int subId,
            String accountType, ContentValues simContentValues) {

        LogUtils.d(TAG, "[updateValuesforCopy] Entry");
        SimUtils.setCurrentSubId(subId);

        if (!SimUtils.isUsim(accountType)) {
            LogUtils.d(TAG, "[updateValuesforCopy] return account is not USIM");
            return;
        }

        mInsertFlag = OPERATION_CONTACT_COPY;
        // if (mCopyUri != sourceUri)
        if (mCopyCount == 0) {
            ArrayList<Anr> phoneArray = new ArrayList<Anr>();

            ContentResolver resolver = mContext.getContentResolver();
            final String[] newProjection = new String[] { Contacts._ID,
                    Contacts.Data.MIMETYPE, Contacts.Data.DATA1,
                    Contacts.Data.IS_ADDITIONAL_NUMBER, Contacts.Data.DATA2,
                    Contacts.Data.DATA3 };

            Cursor c = resolver.query(sourceUri, newProjection, null, null,
                    null);
            if (c != null && c.moveToFirst()) {
                do {
                    String mimeType = c.getString(1);
                    if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        String number = c.getString(2);
                        Anr entry = new Anr();

                        entry.mAdditionNumber = c.getString(2);
                        LogUtils.d(TAG, "[updateValuesforCopy] simAnrNum:"
                                + entry.mAdditionNumber);

                        entry.mAasIndex = mhostAppContext.getString(Phone
                                .getTypeLabelResource(c.getInt(4)));
                        LogUtils.d(TAG, "[updateValuesforCopy] aasIndex:"
                                + entry.mAasIndex);

                        if (c.getInt(3) == 1) {
                            additionalArray.add(entry);
                        } else {
                            phoneArray.add(entry);
                        }

                    }
                } while (c.moveToNext());
            }
            if (c != null) {
                c.close();
            }

            if (phoneArray.size() > 0) {
                phoneArray.remove(0); // This entry is handled by host app for
                // primary
                // number
            }
            additionalArray.addAll(phoneArray);
            mCopyCount = additionalArray.size();

        } else {
            additionalArray.remove(0);
            mCopyCount--;
        }

        int uSimMaxAnrCount = SimUtils.getAnrCount(subId); // use this count in
                                                            // case of multiple
                                                            // anr
        int count = additionalArray.size() > uSimMaxAnrCount ? uSimMaxAnrCount
                : additionalArray.size();

        mCopyAnrList = new ArrayList<Anr>();

        for (int i = 0; i < count; i++) {

            Anr entry = additionalArray.remove(0);
            // String phoneTypeName =
            // mhostAppContext.getString(Phone.getTypeLabelResource(Integer.parseInt(entry.mAasIndex)));
            int aasIndex = SimUtils.getAasIndexByName(entry.mAasIndex, subId);
            LogUtils.d(TAG, "[updateValuesforCopy] additionalNumber:"
                    + entry.mAdditionNumber);
            entry.mAdditionNumber = TextUtils.isEmpty(entry.mAdditionNumber) ? ""
                    : entry.mAdditionNumber.replace("-", "");
            LogUtils.d(TAG, "[updateValuesforCopy] aasIndex:" + aasIndex);
            simContentValues.put("anr" + SimUtils.getSuffix(i),
                    PhoneNumberUtils.stripSeparators(entry.mAdditionNumber));
            simContentValues.put("aas" + SimUtils.getSuffix(i), aasIndex);
            mCopyAnrList.add(entry);
            mCopyCount--;
        }

    }

    public boolean cursorColumnToBuilder(Cursor srcCursor, Builder destBuilder,
            String srcAccountType, String srcMimeType, int destSubId,
            int indexOfColumn) {
        LogUtils.d(TAG, "[cursorColumnToBuilder] Entry");
        String[] columnNames = srcCursor.getColumnNames();
        return generateDataBuilder(null, srcCursor, destBuilder, columnNames,
                srcAccountType, srcMimeType, destSubId, indexOfColumn);

    }

    public boolean generateDataBuilder(Context context, Cursor dataCursor,
            Builder builder, String[] columnNames, String accountType,
            String mimeType, int destSubId, int index) {
        if (SimUtils.isUsim(accountType) && SimUtils.isPhone(mimeType)) {
            String isAnr = dataCursor.getString(dataCursor
                    .getColumnIndex(Data.IS_ADDITIONAL_NUMBER));

            if (Data.DATA2.equals(columnNames[index])) {
                LogUtils.d(TAG, "generateDataBuilder Anr:" + isAnr);
                if ("1".equals(isAnr)) {
                    builder.withValue(Data.DATA2, Phone.TYPE_OTHER);
                    LogUtils.d(TAG,
                            "generateDataBuilder, DATA2 to be TYPE_OTHER ");
                } else {
                    builder.withValue(Data.DATA2, Phone.TYPE_MOBILE);
                    LogUtils.d(TAG,
                            "generateDataBuilder, DATA2 to be TYPE_MOBILE ");
                }
                return true;
            }
            if (Data.DATA3.equals(columnNames[index])) {
                LogUtils.d(TAG, "generateDataBuilder, DATA3 to be null");
                builder.withValue(Data.DATA3, null);
                return true;
            }
        }
        LogUtils.d(TAG, "[generateDataBuilder] false.");
        return false;
    }

    // -----------for CopyProcessor.java ends--------//
    // -------------------for SIMEditProcessor.java starts-------------//
    // this interface to check whether a entry is of additional number or not
    // for SIM & USIM
    public boolean checkAasEntry(ContentValues cv) {
        LogUtils.d(TAG, "[checkAasEntry] para = " + cv);
        if (isAdditionalNumber(cv)) {
            return true;
        }
        return false;
    }

    public String getSubheaderString(int subId, int type) {
        LogUtils.d(TAG, "[getSubheaderString] subId = " + subId);
        if (subId == -1) {
            LogUtils.d(TAG, "[getSubheaderString] Phone contact");
            return null;
        }
        String accountType = SimUtils.getAccountTypeBySub(subId);
        if (SimUtils.isUsim(accountType)) {
            if (SimUtils.isAasPhoneType(type)) {
                LogUtils.d(TAG, "[getSubheaderString] USIM additional number");
                return mContext.getResources().getString(com.mediatek.op03.plugin.R.string.aas_phone_additional);

            } else
                LogUtils.d(TAG, "[getSubheaderString] USIM primary number ");
                return mContext.getResources().getString(com.mediatek.op03.plugin.R.string.aas_phone_primary);
        }
        LogUtils.d(TAG, "[getSubheaderString] Account is SIM ");
        return null;

    }

    // this interface to update additional number & aasindex while writing or
    // updating the USIMcard contact
    public boolean updateValues(Intent intent, int subId,
            ContentValues contentValues) {
        LogUtils.d(TAG, "[updateValues] Entry.");
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        newSimData = intent.getParcelableArrayListExtra("simData");
        ArrayList<RawContactDelta> oldSimData = new ArrayList<RawContactDelta>();
        oldSimData = intent.getParcelableArrayListExtra("simOldData");
        String accountType = newSimData.get(0).getValues()
                .getAsString(RawContacts.ACCOUNT_TYPE);
        SimUtils.setCurrentSubId(subId);
        if (!SimUtils.isUsim(accountType)) {
            LogUtils.d(TAG, "[updateValues] Account type is not USIM.");
            return false;
        }

        // case of new contact
        if (oldSimData == null) {
            // put values for anr
            // aas as anr.mAasIndex
            // set the mInsertFlag to insert, will be used later in
            // updateoperationList, prepare newanrlist
            LogUtils.d(TAG, "[updateValues] for new contact.");
            mInsertFlag = OPERATION_CONTACT_INSERT;
            prepareNewAnrList(intent);
            LogUtils.d(TAG, "[updateValues] for new contact Newanrlist filled");
            return buildAnrInsertValues(accountType, contentValues, mAnrsList);
        }
        // case of edit contact
        else {

            // put values for newAnr
            // aas as anr.mAasIndex
            // set the mInsertFlag to edit, prepare old & new both anr list
            LogUtils.d(TAG, "[updateValues] for Edit contact.");
            mInsertFlag = OPERATION_CONTACT_EDIT;
            prepareNewAnrList(intent);
            LogUtils.d(TAG, "[updateValues] for New anrlist filled");
            prepareOldAnrList(intent);
            LogUtils.d(TAG, "[updateValues] for Old anrlist filled");
            return buildAnrUpdateValues(accountType, contentValues, mAnrsList);
        }

    }

    private void prepareNewAnrList(Intent intent) {
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        newSimData = intent.getParcelableArrayListExtra("simData");
        mAnrsList.clear();
        if (newSimData == null) {
            return;
        }
        // now fill tha data for newanrlist
        int kindCount = newSimData.get(0).getContentValues().size();
        String mimeType = null;
        String data = null;
        for (int countIndex = 0; countIndex < kindCount; countIndex++) {
            mimeType = newSimData.get(0).getContentValues().get(countIndex)
                    .getAsString(Data.MIMETYPE);
            data = newSimData.get(0).getContentValues().get(countIndex)
                    .getAsString(Data.DATA1);
            LogUtils.d(TAG, "[prepareNewAnrList]countIndex:" + countIndex
                    + ",mimeType:" + mimeType + "data:" + data);
            if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                final ContentValues cv = newSimData.get(0).getContentValues()
                        .get(countIndex);
                if (isAdditionalNumber(cv)) {
                    Anr addPhone = new Anr();
                    addPhone.mAdditionNumber = replaceCharOnNumber(cv
                            .getAsString(Data.DATA1));
                    LogUtils.d(TAG, "[prepareNewAnrList] additional number:"
                            + addPhone.mAdditionNumber);
                    addPhone.mAasIndex = cv.getAsString(Data.DATA3);
                    LogUtils.d(TAG,
                            "[prepareNewAnrList] additional number index:"
                                    + addPhone.mAasIndex);
                    mAnrsList.add(addPhone);
                }

            }

        }

    }

    private void prepareOldAnrList(Intent intent) {
        ArrayList<RawContactDelta> oldSimData = new ArrayList<RawContactDelta>();
        oldSimData = intent.getParcelableArrayListExtra("simOldData");
        mOldAnrsList.clear();
        if (oldSimData == null) {
            return;
        }
        // now fill the data for oldAnrlist
        int oldCount = oldSimData.get(0).getContentValues().size();
        String mimeType = null;
        String data = null;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            mimeType = oldSimData.get(0).getContentValues().get(oldIndex)
                    .getAsString(Data.MIMETYPE);
            data = oldSimData.get(0).getContentValues().get(oldIndex)
                    .getAsString(Data.DATA1);
            LogUtils.d(TAG, "[prepareOldAnrList]Data.MIMETYPE: " + mimeType
                    + ",data:" + data);
            if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                ContentValues cv = oldSimData.get(0).getContentValues()
                        .get(oldIndex);
                if (isAdditionalNumber(cv)) {
                    Anr addPhone = new Anr();
                    addPhone.mAdditionNumber = replaceCharOnNumber(cv
                            .getAsString(Data.DATA1));
                    LogUtils.d(TAG, "[prepareOldAnrList] additional number:"
                            + addPhone.mAdditionNumber);
                    addPhone.mAasIndex = cv.getAsString(Phone.DATA3);
                    LogUtils.d(TAG,
                            "[prepareOldAnrList] additional number index:"
                                    + addPhone.mAasIndex);
                    addPhone.mId = cv.getAsInteger(Data._ID);
                    mOldAnrsList.add(addPhone);
                }
            }

        }

    }

    private boolean buildAnrInsertValues(String accountType,
            ContentValues values, ArrayList anrsList) {
        if (SimUtils.isUsim(accountType)) {
            int count = 0;
            for (Object obj : anrsList) {
                Anr anr = (Anr) obj;
                String additionalNumber = TextUtils
                        .isEmpty(anr.mAdditionNumber) ? ""
                        : anr.mAdditionNumber;
                String additionalNumberToInsert = additionalNumber;
                if (!TextUtils.isEmpty(additionalNumber)) {
                    additionalNumberToInsert = PhoneNumberUtils
                            .stripSeparators(additionalNumber);
                    if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtils
                            .extractCLIRPortion(additionalNumberToInsert))) {
                        boolean resultInvalidNumber = true;
                        LogUtils.d(TAG,
                                "[buildAnrInsertValues] additionalNumber Invalid ");
                    }
                    LogUtils.d(TAG,
                            "[buildAnrInsertValues] additionalNumber updated : "
                                    + additionalNumberToInsert);
                }
                values.put("anr" + SimUtils.getSuffix(count),
                        additionalNumberToInsert);
                values.put("aas" + SimUtils.getSuffix(count), anr.mAasIndex);
                count++;
                LogUtils.d(TAG, "[buildAnrInsertValues] aasIndex="
                        + anr.mAasIndex + ", additionalNumber="
                        + additionalNumber);
            }
            return true;
        }
        return false;
    }

    private boolean buildAnrUpdateValues(String accountType,
            ContentValues updatevalues, ArrayList<Anr> anrsList) {
        if (SimUtils.isUsim(accountType)) {
            int count = 0;
            for (Anr anr : anrsList) {
                LogUtils.d(TAG, "[buildAnrUpdateValues] additionalNumber : "
                        + anr.mAdditionNumber);
                if (!TextUtils.isEmpty(anr.mAdditionNumber)) {
                    String additionalNumber = anr.mAdditionNumber;
                    String additionalNumberToInsert = additionalNumber;
                    additionalNumberToInsert = PhoneNumberUtils
                            .stripSeparators(additionalNumber);
                    if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtils
                            .extractCLIRPortion(additionalNumberToInsert))) {
                        boolean resultInvalidNumber = true;
                        LogUtils.d(TAG,
                                "[buildAnrUpdateValues] additionalNumber Invalid");
                    }
                    LogUtils.d(TAG,
                            "[buildAnrUpdateValues] additionalNumber updated: "
                                    + additionalNumberToInsert);
                    updatevalues.put("newAnr" + SimUtils.getSuffix(count),
                            additionalNumberToInsert);
                    updatevalues.put("aas" + SimUtils.getSuffix(count),
                            anr.mAasIndex);
                }
                count++;
            }
            return true;
        }
        return false;
    }

    public boolean updateAdditionalNumberToDB(Intent intent, long rawContactId) {
        ArrayList<RawContactDelta> newSimData = new ArrayList<RawContactDelta>();
        ArrayList<RawContactDelta> oldSimData = new ArrayList<RawContactDelta>();
        LogUtils.d(TAG, "[updateAdditionalNumberToDB] Entry");

        newSimData = intent.getParcelableArrayListExtra("simData");
        oldSimData = intent.getParcelableArrayListExtra("simOldData");
        String accountType = newSimData.get(0).getValues()
                .getAsString(RawContacts.ACCOUNT_TYPE);

        if (!SimUtils.isUsim(accountType)) {
            LogUtils.d(TAG,
                    "[updateAdditionalNumberToDB] return false, account is not USIM");
            return false;
        }
        ContentResolver resolver = mContext.getContentResolver();
        // amit todo check whether passed anrlist & oldanr list are already
        // filled correctly by prevuious interfaces or
        // do we need to take it from intent
        LogUtils.d(TAG, "[updateAdditionalNumberToDB] mAnrlist" + mAnrsList);
        LogUtils.d(TAG, "[updateAdditionalNumberToDB] mOldAnrsList"
                + mOldAnrsList);
        return updateAnrToDb(accountType, resolver, mAnrsList, mOldAnrsList,
                rawContactId);

    }

    private boolean updateAnrToDb(String accountType, ContentResolver resolver,
            ArrayList anrsList, ArrayList oldAnrsList, long rawId) {
        if (SimUtils.isUsim(accountType)) {
            String whereadditional = Data.RAW_CONTACT_ID + " = \'" + rawId
                    + "\'" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "'" + " AND "
                    + Data.IS_ADDITIONAL_NUMBER + " =1" + " AND " + Data._ID
                    + " =";
            LogUtils.d(TAG, "[updateAnrInfoToDb] whereadditional:"
                    + whereadditional);

            // Here, mAnrsList.size() should be the same as mOldAnrsList.size()
            int newSize = anrsList.size();
            int oldSize = oldAnrsList.size();
            int count = Math.min(newSize, oldSize);
            String additionNumber;
            String aas;
            String oldAdditionNumber;
            String oldAas;
            long dataId;
            String where;
            ContentValues additionalvalues = new ContentValues();

            int i = 0;
            for (; i < count; i++) {
                Anr newAnr = (Anr) anrsList.get(i);
                Anr oldAnr = (Anr) oldAnrsList.get(i);
                where = whereadditional + oldAnr.mId;

                additionalvalues.clear();
                if (!TextUtils.isEmpty(newAnr.mAdditionNumber)
                        && !TextUtils.isEmpty(oldAnr.mAdditionNumber)) { // update
                    additionalvalues.put(Phone.NUMBER, newAnr.mAdditionNumber);
                    additionalvalues.put(Data.DATA2, Anr.TYPE_AAS);
                    additionalvalues.put(Data.DATA3, newAnr.mAasIndex);

                    int upadditional = resolver.update(Data.CONTENT_URI,
                            additionalvalues, where, null);
                    LogUtils.d(TAG, "upadditional is " + upadditional);
                } else if (!TextUtils.isEmpty(newAnr.mAdditionNumber)
                        && TextUtils.isEmpty(oldAnr.mAdditionNumber)) { // insert
                    additionalvalues.put(Phone.RAW_CONTACT_ID, rawId);
                    additionalvalues
                            .put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    additionalvalues.put(Phone.NUMBER, newAnr.mAdditionNumber);
                    additionalvalues.put(Data.DATA2, Anr.TYPE_AAS);
                    additionalvalues.put(Data.DATA3, newAnr.mAasIndex);
                    additionalvalues.put(Data.IS_ADDITIONAL_NUMBER, 1);

                    Uri upAdditionalUri = resolver.insert(Data.CONTENT_URI,
                            additionalvalues);
                    LogUtils.d(TAG, "upAdditionalUri is " + upAdditionalUri);
                } else if (TextUtils.isEmpty(newAnr.mAdditionNumber)) { // delete
                    int deleteAdditional = resolver.delete(Data.CONTENT_URI,
                            where, null);
                    LogUtils.d(TAG, "deleteAdditional is " + deleteAdditional);
                }
            }

            // in order to avoid error, do the following operations.
            while (i < oldSize) { // delete one
                Anr oldAnr = (Anr) oldAnrsList.get(i);
                dataId = oldAnr.mId;
                where = whereadditional + dataId;
                int deleteAdditional = resolver.delete(Data.CONTENT_URI, where,
                        null);
                LogUtils.d(TAG, "deleteAdditional is " + deleteAdditional);
                i++;
            }

            while (i < newSize) { // insert one
                Anr newAnr = (Anr) anrsList.get(i);
                additionalvalues.clear();
                if (!TextUtils.isEmpty(newAnr.mAdditionNumber)) {
                    additionalvalues.put(Phone.RAW_CONTACT_ID, rawId);
                    additionalvalues
                            .put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    additionalvalues.put(Phone.NUMBER, newAnr.mAdditionNumber);
                    additionalvalues.put(Data.DATA2, Anr.TYPE_AAS);
                    additionalvalues.put(Data.DATA3, newAnr.mAasIndex);
                    additionalvalues.put(Data.IS_ADDITIONAL_NUMBER, 1);

                    Uri upAdditionalUri = resolver.insert(Data.CONTENT_URI,
                            additionalvalues);
                    LogUtils.d(TAG, "upAdditionalUri is " + upAdditionalUri);
                }
                i++;
            }
            return true;
        }
        return false;
    }

    // writing a common api interface for copy & insert as we have to update the
    // operation list, based on mInsertFlag will decide which list have to
    // process
    public boolean updateOperationList(Account accounType,
            ArrayList<ContentProviderOperation> operationList, int backRef) {
        // bassed on op we have to decide which list we have to parse
        // for copy
        if (!SimUtils.isUsim(accounType.type)) {
            LogUtils.d(TAG,
                    "[updateOperationList] Account is not USIM so return false");
            return false;
        }

        if (mInsertFlag == OPERATION_CONTACT_COPY) {
            if (mCopyAnrList != null && mCopyAnrList.size() > 0) {
                LogUtils.d(TAG, "[updateOperationList] for copy ");
                boolean result = buildAnrOperation(accounType.type,
                        operationList, mCopyAnrList, backRef);
                LogUtils.d(TAG, "[updateOperationList] result : " + result);
                mCopyAnrList.clear();
                mCopyAnrList = null;
                return result;
            }
            LogUtils.d(TAG, "[updateOperationList] result false");
            return false;

        }

        // for insert
        else {
            if (SimUtils.isUsim(accounType.type)) {
                LogUtils.d(TAG, "[updateOperationList] for Insert contact ");
                // build Anr ContentProviderOperation
                for (Object obj : mAnrsList) {
                    Anr anr = (Anr) obj;
                    if (!TextUtils.isEmpty(anr.mAdditionNumber)) {
                        LogUtils.d(TAG, "additionalNumber="
                                + anr.mAdditionNumber + " aas=" + anr.mAasIndex);

                        ContentProviderOperation.Builder builder = ContentProviderOperation
                                .newInsert(Data.CONTENT_URI);
                        builder.withValueBackReference(Phone.RAW_CONTACT_ID,
                                backRef);
                        builder.withValue(Data.MIMETYPE,
                                Phone.CONTENT_ITEM_TYPE);
                        builder.withValue(Data.DATA2, Anr.TYPE_AAS);
                        builder.withValue(Phone.NUMBER, anr.mAdditionNumber);
                        builder.withValue(Data.DATA3, anr.mAasIndex);

                        builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
                        operationList.add(builder.build());
                    }
                }
                LogUtils.d(TAG, "[updateOperationList] result true");
                return true;
            }

        }
        LogUtils.d(TAG, "[updateOperationList] result false");
        return false;

    }

    public CharSequence getLabelForBindData(Resources res, int type,
            String customLabel, String mimeType, Cursor cursor,
            CharSequence defaultValue) {

        LogUtils.d(TAG, "getLabelForBindData() Entry mimetype:" + mimeType);
        CharSequence label = defaultValue;
        final int indicate = cursor.getColumnIndex(Contacts.INDICATE_PHONE_SIM);
        int subId = -1;
        if (indicate != -1) {
            subId = cursor.getInt(indicate);
        }
        String accountType = SimUtils.getAccountTypeBySub(subId);
        if (SimUtils.isUsim(accountType)
                && mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
            label = "";
        } else {

            label = getTypeLabel(type, (CharSequence) customLabel,
                    (String) defaultValue, subId);

        }
        return label;

    }

    private String replaceCharOnNumber(String number) {
        String trimNumber = number;
        if (!TextUtils.isEmpty(trimNumber)) {
            LogUtils.d(TAG, "[replaceCharOnNumber]befor replaceall number : "
                    + trimNumber);
            trimNumber = trimNumber.replaceAll("-", "");
            trimNumber = trimNumber.replaceAll(" ", "");
            LogUtils.d(TAG, "[replaceCharOnNumber]after replaceall number : "
                    + trimNumber);
        }
        return trimNumber;
    }

    public CharSequence getTypeLabel(int type, CharSequence label,
            String defvalue, int subId) {
        LogUtils.d(TAG, "getTypeLabel_new() Entry subId:" + subId);

        String accountType = SimUtils.getAccountTypeBySub(subId);
        LogUtils.d(TAG, "getTypeLabel_new() subId=" + subId + " accountType="
                + accountType);
        if (SimUtils.isSim(accountType)) {
            LogUtils.d(TAG, "getTypeLabel_new() SIM Account no Label.");
            return "";
        }
        if (SimUtils.isUsim(accountType) && SimUtils.isAasPhoneType(type)) {
            LogUtils.d(TAG, "getTypeLabel_new() USIM Account label=" + label);
            if (TextUtils.isEmpty(label)) {
                LogUtils.d(TAG, "getTypeLabel_new Empty");
                return "";
            }
            try {
                final Integer aasIdx = Integer.valueOf(label.toString());
                final String tag = SimUtils.getAASById(subId,
                        aasIdx.intValue());
                LogUtils.d(TAG, "getTypeLabel_new() index" + aasIdx + " tag="
                        + tag);
                return tag;
            } catch (NumberFormatException e) {
                LogUtils.e(TAG, "getTypeLabel_new() return label=" + label);
            }
        }
        if (SimUtils.isUsim(accountType) && !SimUtils.isAasPhoneType(type)) {
            LogUtils.d(TAG,
                    "getTypeLabel_new account is USIM but type is not additional");
            return "";
        }
        LogUtils.d(TAG, "getTypeLabel_new return defvalue");
        return defvalue;
    }

    // Amit ends

    private boolean isAdditionalNumber(ValuesDelta entry) {
        final String key = Data.IS_ADDITIONAL_NUMBER;
        Integer isAnr = entry.getAsInteger(key);
        return isAnr != null && 1 == isAnr.intValue();
    }

    private boolean isAdditionalNumber(final ContentValues cv) {
        final String key = Data.IS_ADDITIONAL_NUMBER;
        Integer isAnr = null;
        if (cv != null && cv.containsKey(key)) {
            isAnr = cv.getAsInteger(key);
        }
        return isAnr != null && 1 == isAnr.intValue();
    }

    private EditType getAasEditType(ValuesDelta entry, DataKind kind,
            int phoneType) {
        if (phoneType == Anr.TYPE_AAS) {
            String customColumn = entry.getAsString(Data.DATA3);
            LogUtils.d(TAG, "getAasEditType() customColumn=" + customColumn);
            if (customColumn != null) {
                for (EditType type : kind.typeList) {
                    if (type.rawValue == Anr.TYPE_AAS
                            && customColumn.equals(type.customColumn)) {
                        LogUtils.d(TAG, "getAasEditType() type");
                        return type;
                    }
                }
            }
            return null;
        }
        LogUtils.e(TAG, "getAasEditType() error Not Anr.TYPE_AAS, type="
                + phoneType);
        return null;
    }

    private void updateEditorViewsLabel(ViewGroup viewGroup) {
        LogUtils.e(TAG, "updateEditorViewsLabel() Entry");
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof TextFieldsEditorView) {
                ((LabeledEditorView) v).updateValues();
            } else if (v instanceof ViewGroup) {
                updateEditorViewsLabel((ViewGroup) v);
            }
        }
    }

    public void ensurePhoneKindForCompactEditor(RawContactDeltaList state,
            int subId, Context context) {

        int numRawContacts = state.size();
        LogUtils.e(TAG,
                "ensurePhoneKindForCompactEditor() Entry numRawContacts= "
                        + numRawContacts);
        final AccountTypeManager accountTypes = AccountTypeManager
                .getInstance(mContext);
        for (int i = 0; i < numRawContacts; i++) {
            final RawContactDelta rawContactDelta = state.get(i);
            final AccountType type = rawContactDelta
                    .getAccountType(accountTypes);
            LogUtils.e(TAG, "ensurePhoneKindForCompactEditor() loop subid="
                    + subId);
            ensurePhoneKindForEditor(type, subId, rawContactDelta);
        }
    }

}
