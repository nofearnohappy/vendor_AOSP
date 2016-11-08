package com.mediatek.rcs.message.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.cmcc.ccs.chat.ChatMessage;
import com.cmcc.ccs.chat.ChatService;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.GenericPdu;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
//
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.mediatek.mms.callback.IMultiDeleteActivityCallback;
import com.mediatek.mms.ipmessage.DefaultIpMultiDeleteActivityExt;
import com.mediatek.mms.ipmessage.IIpMessageListAdapterExt;
import com.mediatek.mms.ipmessage.IIpMessageItemExt;
import com.mediatek.rcs.common.RcsLog;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.provider.FavoriteMsgData;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.plugin.RcsMessageListAdapter.RCSColumnsMap;
import com.mediatek.rcs.message.provider.FavoriteMsgProvider;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * Plugin implements. response MultiDeleteActivity.java in MMS host.
 *
 */
public class RcsMultiDeleteActivity extends DefaultIpMultiDeleteActivityExt {
    private static String TAG = "RcsMultiDeleteActivity";

    public static final String IPMSG_IDS = "forward_ipmsg_ids";
    private static final int MENU_FAVORITE = 1001;
    private static final int MAX_COUNT_OF_COMBINE_FORWARD = 10;
    /// M: add for ipmessage, record the ipmessage id.
    private Map<Long, Long> mSelectedIpMessageIds = new HashMap<Long, Long>();
    private Activity mContext;
    private Context mPluginContext;
    private long mThreadId;
    private int mForwardMenuId;
    private String mChatId;
    private boolean mIsGroupChat;

    //favorite
    private String COLUMN_NAME_ID = "_id";
    private String COLUMN_NAME_TYPE = "m_type";
    private String COLUMN_NAME_DATE = "date";
    private String COLUMN_NAME_MESSAGE_BOX = "msg_box";
    private String COLUMN_NAME_READ = "read";
    private String FILE_EXT_PDU = ".pdu";

    private static final String SMS_FORWARD_WITH_SENDER = "pref_key_forward_with_sender";

    private ProgressDialog mProgressDialog;
    boolean favoriteSmsSuccess;
    boolean favoriteMmsSuccess;

    private static final String[] SMS_PROJECTION =
    { "_id", "body", "address", "ipmsg_id"};

    private IMultiDeleteActivityCallback mCallback;
    private RcsMessageListAdapter mAdapter;
    RCSMessageManager mRcsMessageManager;

    @Override
    public boolean MultiDeleteActivityInit(Activity context,
                                        IMultiDeleteActivityCallback callback) {
        mContext = context;
        mCallback = callback;
        mRcsMessageManager = RCSMessageManager.getInstance();
        mPluginContext = ContextCacher.getPluginContext();
        mThreadId = mContext.getIntent().getLongExtra("thread_id", 0);
        mChatId = RcsMessageUtils.blockingGetGroupChatIdByThread(context, mThreadId);
        mIsGroupChat = !TextUtils.isEmpty(mChatId);
        return true;
    }

    @Override
    public boolean onIpMultiDeleteClick(AsyncQueryHandler handler, int token, Object cookie,
            int deleteRunningCount, boolean deleteLocked) {
        /// M: delete ipmessage
        if (mSelectedIpMessageIds.size() > 0) {
            Iterator<Entry<Long, Long>> iter = mSelectedIpMessageIds.entrySet().iterator();
            HashSet<Long> mRcsMessageIds = new HashSet<Long>();
            while (iter.hasNext()) {
                 Map.Entry<Long, Long> entry = iter.next();
                 long key = entry.getKey();
                 long msgId = RcsMessageListAdapter.getRcsMsgIdByKey(key);
                 mRcsMessageIds.add(msgId);
            }
            String selection = combineRcsMessageSelection(mRcsMessageIds);
            Log.d(TAG, "[onIpMultiDeleteClick] selection = " + selection);
            handler.startDelete(token, cookie, RcsLog.MessageColumn.CONTENT_URI, selection, null);
            mCallback.setDeleteRunningCount(deleteRunningCount++);
            Log.d(TAG, "delete ipmessage, id:" + mSelectedIpMessageIds.size());
            mSelectedIpMessageIds.clear();
            return true;
        }
        return false;
    }

    private String combineRcsMessageSelection(Set<Long> idSet) {
        if (idSet == null || idSet.size() <= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(RcsLog.MessageColumn.ID +" In (");
        boolean firstItem = true;
        for (long id : idSet) {
            if (!firstItem) {
                sb.append(",");
            }
            sb.append(id);
            firstItem = false;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean onIpDeleteLockedIpMsg(long msgId) {
        if (mSelectedIpMessageIds.size() > 0 && mSelectedIpMessageIds.containsKey(msgId)) {
            mSelectedIpMessageIds.remove(msgId);
            return true;
        }
        return false;
    }

    @Override
    public boolean onIpHandleItemClick(IIpMessageItemExt item, long ipMessageId, boolean isSelected,
            long key) {
        RcsMessageItem rcsItem = (RcsMessageItem) item;
        if (rcsItem.isRcs()) {
            if (isSelected) {
                mSelectedIpMessageIds.put(key, ipMessageId);
            } else {
                mSelectedIpMessageIds.remove(key);
            }
        }
        return true;
    }

    @Override
    public boolean onIpMarkCheckedState(Cursor cursor, boolean checkedState) {
        if (checkedState) {
            RCSColumnsMap columnMap = mAdapter.mRcsColumnsMap;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(columnMap.getColumnMsgType());
                    if (type.equals("rcs")) {
                        long msgId = cursor.getLong(columnMap.getColumnMsgId());
                        long ipMessageId = cursor.getLong(columnMap.mColumnRcsMessageIpMsgId);
                        long key = RcsMessageListAdapter.getKey(type, msgId);
                        if (!mSelectedIpMessageIds.containsKey(key)) {
                            mSelectedIpMessageIds.put(key, ipMessageId);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } else {
            mSelectedIpMessageIds.clear();
        }
        return false;
    }

    @Override
    public boolean onAddSelectedIpMessageId(boolean checkedState, long msgId, long ipMessageId) {
        /// M: add for ipmessage
//        if (checkedState && ipMessageId != 0) {
//            mSelectedIpMessageIds.put(msgId, ipMessageId);
//        }
        return true;
    }

    @Override
    public boolean onIpDeleteThread(Collection<Long> threads, int maxSmsId) {
        Log.d(TAG, "onIpDeleteThread, threads = " + threads);
        Iterator<Long> iter = threads.iterator();
        // TODO unfinished function, deleteAll from multiDeleteActivity
//        while (iter.hasNext()) {
//            long threadId = iter.next();
//            mRcsMessageManager.deleteThreadFromMulti(threadId, maxSmsId);
//        }
        mSelectedIpMessageIds.clear();
        return true;
    }


    //favorite
    @Override
    public boolean onCreateIpActionMode(final ActionMode mode, Menu menu) {
      /// M: add for ipmessage menu
        //if (RCSServiceManager.getInstance().serviceIsReady()) {
            MenuItem item = menu
                    .add(0, MENU_FAVORITE, 0, mPluginContext.getString(R.string.menu_favorite))
                    .setTitle(mPluginContext.getString(R.string.menu_favorite));
            item.setVisible(true);
        //}
        return true;
    }

    @Override
    public boolean onPrepareIpActionMode(ActionMode mode, Menu menu, int selectNum, int ForwardMenuId) {
        mForwardMenuId = ForwardMenuId;
        MenuItem favoriteItem = menu.findItem(MENU_FAVORITE);
        if (favoriteItem == null) {
            return true;
        }
        if (RCSServiceManager.getInstance().serviceIsReady()) {
            if (selectNum > 0) {
                favoriteItem.setVisible(true);
            } else {
                favoriteItem.setVisible(false);
            }
        } else {
            //favoriteItem.setVisible(false);
            //for IT
            if (selectNum > 0) {
                favoriteItem.setVisible(true);
            } else {
                favoriteItem.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onIpActionItemClicked(ActionMode mode, MenuItem item, long[][] selectedIds,
            String[] contacts, Cursor cursor) {
        if (item.getItemId() == MENU_FAVORITE || item.getItemId() == mForwardMenuId) {
            Log.d(TAG, "thiss onIpActionItemClicked");
            HashSet<Long> smsIds = null;
            HashSet<Long> mmsIds = null;
            HashSet<Long> rcsIds = null;
            boolean hasMms = false;
            boolean hasSms = false;
            boolean hasRcs = false;
            boolean mHasUnDownloadMsg = false;
            if (selectedIds[0] != null && selectedIds[0].length > 0) {
                smsIds = collectIds(selectedIds[0]);
                if (mSmsItem == null) {
                    mSmsItem = new HashMap<Long, smsBodyandAddress>();
                } else {
                    mSmsItem.clear();
                }
                if (smsIds.size() > 0) {
                    hasSms = true;
                }
            }
            if (selectedIds[1] != null && selectedIds[1].length > 0) {
                mmsIds = collectIds(selectedIds[1]);
                if (mMmsItem == null) {
                    mMmsItem = new HashMap<Long, mmsSubjectandType>();
                } else {
                    mMmsItem.clear();
                }
                if (mmsIds.size() > 0) {
                    hasMms = true;
                }
            }
            if (selectedIds.length > 2 && selectedIds[2] != null && selectedIds[2].length > 0) {
                rcsIds = collectIds(selectedIds[2]);
                if (rcsIds.size() > 0) {
                    hasRcs = true;
                }
            }

            if (item.getItemId() == MENU_FAVORITE) {
                Log.d(TAG, "thiss onIpActionItemClicked MENU_FAVORITE");
                doFavorite(cursor, smsIds, mmsIds, rcsIds);
                return true;
            } else if (item.getItemId() == mForwardMenuId) {
                Log.d(TAG, "thiss onIpActionItemClicked mForwardMenuId");
                return doForward(cursor, smsIds, mmsIds, rcsIds);
            }
        }
        return false;
    }

    private boolean doFavorite(Cursor cursor, HashSet<Long> smsIds, HashSet<Long> mmsIds,
                                        HashSet<Long> rcsIds) {
        if (cursor == null || cursor.getCount() <= 0) {
            return false;
        }
        boolean hasUnDownloadMsg = false;
        try {
            if (cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(0);
                    long msgId = cursor.getLong(1);
                    long key = RcsMessageListAdapter.getKey(type, msgId);
                    if (type.equals("mms") && mmsIds.contains(key)) {
                        int boxId = cursor.getInt(cursor.getColumnIndex(Mms.MESSAGE_BOX));
                        int messageType = cursor.getInt(
                                                cursor.getColumnIndexOrThrow(Mms.MESSAGE_TYPE));
                        long date = cursor.getInt(cursor.getColumnIndex(Mms.DATE));
                        if (boxId == Mms.MESSAGE_BOX_INBOX
                                && messageType == MESSAGE_TYPE_NOTIFICATION_IND) {
                            hasUnDownloadMsg  = true;
                            continue;
                        }
                        String sub = cursor.getString(cursor.getColumnIndexOrThrow(Mms.SUBJECT));
                        int sub_cs = 0;
                        if (!TextUtils.isEmpty(sub)) {
                            sub_cs =cursor.getInt(
                                                cursor.getColumnIndexOrThrow(Mms.SUBJECT_CHARSET));
                        }
//                        mmsSubjectandType st =
//                                    new mmsSubjectandType(boxId, messageType, sub, sub_cs, date);
//                        mMmsItem.put(msgId, st);
                    } else if (type.equals("sms") && rcsIds.contains(key)) {
                        String body    = cursor.getString(cursor.getColumnIndexOrThrow(Sms.BODY));
                        String address = cursor.getString(
                                                cursor.getColumnIndexOrThrow(Sms.ADDRESS));
                        int boxId = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.TYPE));
                    } else if (type.equals("rcs") && smsIds.contains(key)) {
                        RCSColumnsMap rcsColumnsMap = mAdapter.mRcsColumnsMap;
                        int messageClass = cursor.getInt(rcsColumnsMap.mColumnRcsMessageClass);
                        int messageType = cursor.getInt(rcsColumnsMap.mColumnRcsMessageType);
                        int direction = cursor.getInt(rcsColumnsMap.mColumnRcsMessageDirection);
//                        int status = cursor.getInt(rcsColumnsMap.mColumnRcsMessageStatus);
                        if (messageClass == RcsLog.Class.BURN) {
                            hasUnDownloadMsg = true;
                            continue;
                        } else if (messageType == RcsLog.MessageType.FT
                                && direction == RcsLog.Direction.INCOMING) {
                            // incoming ft
                            IpAttachMessage ftMessage =
                                    (IpAttachMessage) mRcsMessageManager.getRCSMessageInfo(msgId);
                            if (ftMessage.getRcsStatus() != Status.FINISHED) {
                                hasUnDownloadMsg = true;
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        final String address = getStringContact(contacts);
//        if (mHasUnDownloadMsg) {
//            showTipsDialog(address, hasMms, hasSms);
//        } else {
//            startFavorite(address, hasMms, hasSms);
//        }
        return true;
    }

    private boolean doForward(Cursor cursor, HashSet<Long> smsKeys, HashSet<Long> mmsKeys,
            HashSet<Long> rcsKeys) {
      //forward single ft
        //only one rcsMessage
        int smsNumber = rcsKeys == null ? 0 : rcsKeys.size();
        int mmsNumber = rcsKeys == null ? 0 : rcsKeys.size();
        int rcsNumber = rcsKeys == null ? 0 : rcsKeys.size();

        if (smsNumber == 0 && mmsNumber == 0 && rcsNumber == 1) {
            //only one rcs message
            Long[] rcsKeysArray = rcsKeys.toArray(new Long[1]);
            long rcsId = RcsMessageListAdapter.getRcsMsgIdByKey(rcsKeysArray[0]);
            IpMessage message = mRcsMessageManager.getRCSMessageInfo(rcsId);
            if (message.getBurnedMessage()) {
                // burned message
                Toast.makeText(mContext,
                        mPluginContext.getString(R.string.toast_burned_messsage_forward),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            if (message instanceof IpAttachMessage) {
                IpAttachMessage attMessage = (IpAttachMessage) message;
                if ((attMessage.getStatus() == MessageStatus.READ
                        || attMessage.getStatus() == MessageStatus.UNREAD)
                    && attMessage.getRcsStatus() != Status.FINISHED) {
                    showFtDialog();
                } else {
                    if (RCSServiceManager.getInstance().serviceIsReady()) {
                        Intent forwardintent =
                            RcsMessageUtils.createForwordIntentFromIpmessage(mContext, attMessage);
                        mContext.startActivity(forwardintent);
                    } else {
                        Toast.makeText(mContext,
                                       mPluginContext.getString(R.string.toast_sms_unable_forward),
                                       Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        }
        boolean hasFt = hasFTSelected(rcsKeys);
        if (mmsNumber > 0 && !hasFt) {
            return false;
        }
        if (hasFt) {
          //showdialog and call back
            showMmsFtDialog();
            return true;
        }
        return false;
    }

    private boolean hasFTSelected(HashSet<Long> rcsKeys) {
        boolean ret = false;
        String selection = combineRcsMessageSelection(rcsKeys);
        String[] projection = new String[] {RcsLog.MessageColumn.TYPE, RcsLog.MessageColumn.CLASS};
        Cursor cursor = mContext.getContentResolver().query(RcsLog.MessageColumn.CONTENT_URI,
                        projection, selection, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int type = cursor.getInt(0);
                    if (type == RcsLog.MessageType.FT) {
                        ret = true;
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return ret;
    }

    public boolean needShowNoTextToast(int mSize) {
        int ftCount = getFtCount();
        Log.d(TAG, "needShowNoTextToast ftCount ="+ftCount + " mSize ="+ mSize);
        if (mSize <= 0 || ftCount == mSize) {
            Log.d(TAG, "needShowNoTextToast return true");
            return true;
        }
        return false;
    }

    public boolean needShowReachLimit() {
        if (RCSServiceManager.getInstance().serviceIsReady()) {
            return true;
        } else {
            return false;
        }
    }

    private int getFtCount() {
        int ftCount = 0;
        if (mSelectedIpMessageIds.size() > 0) {
            Iterator<Entry<Long, Long>> iter = mSelectedIpMessageIds.entrySet().iterator();
            while (iter.hasNext()) {
                 Map.Entry<Long, Long> entry = iter.next();
                 if (entry.getValue() < 0) {
                     ftCount += 1;
                 }
            }
        }
        return ftCount;
    }

    private void showMmsFtDialog() {
        Log.d(TAG, "thiss showMmsFtDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.discard_mmsft_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setMessage(mPluginContext.getString(R.string.discard_mmsft_content))
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       mCallback.onForwardActionItemClick();
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .show();
    }

    private void showFtDialog() {
        Log.d(TAG, "thiss showFtDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.forward_ft_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setMessage(mPluginContext.getString(R.string.forward_ft_body))
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       Toast.makeText(mContext, mPluginContext.getString(R.string.toast_sms_forward),
                               Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .show();
    }

    private void startFavorite(final String addresses, final boolean favoriteMms, final boolean favoriteSms) {
        showProgressIndication();
        favoriteSmsSuccess = false;
        favoriteMmsSuccess = false;
        new Thread(new Runnable() {
            public void run() {
                if (favoriteSms) {
                    favoriteSmsSuccess = setSmsFavorite();//sms
                }
                if (favoriteMms) {
                    favoriteMmsSuccess = setMmsFavorite(addresses);//mms  need address
                }
                mContext.runOnUiThread(new Runnable() {
                    public void run() {
                        dismissProgressIndication();
                        if (favoriteSmsSuccess || favoriteMmsSuccess) {
                            Toast.makeText(mContext, mPluginContext.getString(
                                    R.string.toast_favorite_success), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, mPluginContext.getString(
                                    R.string.toast_sms_favorite), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void showProgressIndication() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mPluginContext.getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void dismissProgressIndication() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private Map<Long, smsBodyandAddress> mSmsItem;
    private Map<Long, mmsSubjectandType> mMmsItem;
    class smsBodyandAddress {
        int boxId;
        long ipmsgid;
        String mAddress;
        String mBody;
        long mDate;
        public smsBodyandAddress(String mAddress, String mBody, long ipmsgid, int boxId, long mDate) {
            super();
            this.ipmsgid = ipmsgid;
            this.mAddress = mAddress;
            this.mBody = mBody;
            this.boxId = boxId;
            this.mDate = mDate;
        }
    }

    class mmsSubjectandType {
        int boxId;
        int mMessageType;
        String mSubject;
        int sub_cs;
        long mDate;
        public mmsSubjectandType(int boxId, int mMessageType, String mSubject, int sub_cs, long mDate) {
            super();
            this.boxId = boxId;
            this.mMessageType = mMessageType;
            this.mSubject = mSubject;
            this.sub_cs = sub_cs;
            this.mDate = mDate;
        }
    }

    private String getStringId(final long Ids[]) {
        String idSelect = null;
        if (Ids != null && Ids.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            for (long id : Ids) {
                strBuf.append(id + ",");
            }
            String str = strBuf.toString();
            idSelect = str.substring(0, str.length() - 1);
        }
        return idSelect;
    }

    private String getStringContact(String Contacts[]) {
        String ContactsSelect = null;
        if (Contacts != null && Contacts.length > 0) {
            StringBuffer strBuf = new StringBuffer();
            for (String contact : Contacts) {
                strBuf.append(contact + ",");
            }
            String str = strBuf.toString();
            ContactsSelect = str.substring(0, str.length() - 1);
        }
        return ContactsSelect;
    }

    private HashSet<Long> collectIds(long Ids[]) {
        HashSet<Long> idSet = new HashSet<Long>();
        for (long id : Ids) {
            if (id > 0) {
                idSet.add(id);
            }
        }
        return idSet;
    }

//    private HashSet<Long> getSmsCollectIds(long keys[]) {
//        HashSet<Long> idSet = new HashSet<Long>();
//        if (keys != null) {
//            for (long key : keys) {
//                if (!RcsMessageListAdapter.isRcsKey(key)) {
//                    idSet.add(key);
//                }
//            }
//        }
//        return idSet;
//    }
//
//    private HashSet<Long> getRcsCollectIds(long keys[]) {
//        HashSet<Long> idSet = new HashSet<Long>();
//        if (keys != null) {
//            for (long key : keys) {
//                if (RcsMessageListAdapter.isRcsKey(key)) {
//                    idSet.add(RcsMessageListAdapter.getRcsMsgIdByKey(key));
//                }
//            }
//        }
//        return idSet;
//    }

    private boolean setTextIpmessageFavorite(long id, int ct, ContentValues values) {
        if (ct == IpMessageType.EMOTICON) {
            values.put(FavoriteMsgData.COLUMN_DA_TYPE, FavoriteMsgProvider.FAVORITEEMOJI);
        } else {
            values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.IM);//sms
        }
        values.put(FavoriteMsgData.COLUMN_DA_MIME_TYPE, "text/plain");
        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
        if (mChatId != null) {
            values.put(FavoriteMsgData.COLUMN_CHATID, mChatId);
        }
        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
        return true;
    }

    private boolean setAttachIpmessageFavorite(long id, String mPath, ContentValues values) {
        values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.FT);//sms
        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
        if (mChatId != null) {
            values.put(FavoriteMsgData.COLUMN_CHATID, mChatId);
        }
        if (mPath != null) {
            String imFilePath = RcsMessageUtils.getFavoritePath(mContext, "favorite_ipmessage");
            Log.d(TAG, "thiss imFilePath =" + imFilePath);
            if (imFilePath == null) {
                return false;
            }
            String mFileName = imFilePath + "/" + getFileName(mPath);
            String mNewpath = RcsMessageUtils.getUniqueFileName(mFileName);
            Log.d(TAG, "thiss mNewpath =" + mNewpath);
            copyFile(mPath,mNewpath);
            values.put(FavoriteMsgData.COLUMN_DA_FILENAME, mNewpath);
            String mimeType = RCSUtils.getFileType(getFileName(mPath));
            if (mimeType != null) {
                values.put(FavoriteMsgData.COLUMN_DA_MIME_TYPE, mimeType);
            }
        }
        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
        return true;
    }

    private String getFileName(String mFile) {
        return mFile.substring(mFile.lastIndexOf("/") + 1);
    }

    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) {
                inStream = new FileInputStream(oldPath);
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[3000];
                while ( (byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
            if (null != fs) {
                try {
                    fs.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
    }

    private void showTipsDialog(final String address, final boolean isHasMms, final boolean isHasSms) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mPluginContext.getString(R.string.favorite_tips_title))
               .setIconAttribute(android.R.attr.alertDialogIcon)
               .setCancelable(true)
               .setPositiveButton(mPluginContext.getString(R.string.dialog_continue), new DialogInterface.OnClickListener() {
                   public final void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       startFavorite(address, isHasMms, isHasSms);
                   }
               })
               .setNegativeButton(mPluginContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                       if (mSmsItem != null) {
                           mSmsItem.clear();
                       }
                       if (mMmsItem != null) {
                           mMmsItem.clear();
                       }
                   }
               })//Cancel need to clear hashmap cache
               .setMessage(mPluginContext.getString(R.string.favorite_tips_body))
               .show();
    }

    private boolean setSmsFavorite() {
        if (mSmsItem != null && mSmsItem.size() > 0) {
            Iterator<Entry<Long, smsBodyandAddress>> iter = mSmsItem.entrySet().iterator();
            while (iter.hasNext()) {
                 Map.Entry<Long, smsBodyandAddress> entry = iter.next();
                 long ipMsgid = entry.getValue().ipmsgid;
                 long id = entry.getKey();
                 long date = entry.getValue().mDate;
                 String contact = entry.getValue().mAddress;
                 ContentValues values = new ContentValues();
                 if (entry.getValue().boxId == 1) {
                     values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.INCOMING);
                 } else {
                     values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.OUTCOMING);
                 }

                 String mBody = entry.getValue().mBody;

                 //FLAG
                 int flag = 0;
                 if (mIsGroupChat) {
                     flag = ChatMessage.MTM;
                 } else if (contact.indexOf(",") > 0) {
                     flag = ChatMessage.OTM;
                 } else {
                     flag = ChatMessage.OTO;
                 }
                 values.put(FavoriteMsgData.COLUMN_DA_FLAG, flag);

                 //CONTACT_NUMBER
                 if (flag == ChatMessage.MTM) {
                     if (mChatId != null) {
                         values.put(FavoriteMsgData.COLUMN_DA_CONTACT, mChatId);
                     }
                 } else {
                     values.put(FavoriteMsgData.COLUMN_DA_CONTACT, contact);
                 }
                 values.put(FavoriteMsgData.COLUMN_DA_TIMESTAMP, date);
                 if (ipMsgid != 0) {
                     IpMessage ipMessageForFavorite = mRcsMessageManager.getRCSMessageInfo(id);
                     values.put(FavoriteMsgData.COLUMN_DA_ID, ipMessageForFavorite.getMessageId());
                     values.put(FavoriteMsgData.COLUMN_DA_MESSAGE_STATUS, ipMessageForFavorite.getStatus());
                     if (ipMsgid > 0) {
                         values.put(FavoriteMsgData.COLUMN_DA_BODY, mBody);
                         setTextIpmessageFavorite(id, 10, values);//10 is defined by self
                     } else if (ipMsgid < 0) {
                         IpAttachMessage attachMessage = (IpAttachMessage) ipMessageForFavorite;
                         setAttachIpmessageFavorite(id, attachMessage.getPath(), values);
                     }
                 } else {
                     values.put(FavoriteMsgData.COLUMN_DA_ID, id);
                     values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.SMS);//sms
                     values.put(FavoriteMsgData.COLUMN_DA_BODY, mBody);
                     values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
                     mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
                 }
            }
            mSmsItem.clear();
        } else {
            return false;
        }
        return true;
    }

    private boolean setMmsFavorite(final String Contacts) {
        byte[] pduMid;
        String pduFilePath = RcsMessageUtils.getFavoritePath(mContext, "favorite_pdu");
        Log.d(TAG, "thiss pduFilePath =" + pduFilePath);
        Log.d(TAG, "thiss time1 =" + System.currentTimeMillis() + "; Contacts = " + Contacts);
        if (pduFilePath == null) {
            return false;
        }

        if (mMmsItem != null && mMmsItem.size() > 0) {
            Iterator<Entry<Long, mmsSubjectandType>> iter = mMmsItem.entrySet().iterator();
            try {
                while (iter.hasNext()) {
                    Map.Entry<Long, mmsSubjectandType> entry = iter.next();
                    long id = entry.getKey();
                    int mBoxId = entry.getValue().boxId;
                    int type = entry.getValue().mMessageType;
                    String mSubject = entry.getValue().mSubject;
                    long date = entry.getValue().mDate;
                    if (!TextUtils.isEmpty(mSubject)) {
                        int charset = entry.getValue().sub_cs;
                        EncodedStringValue v = new EncodedStringValue(charset,
                                PduPersister.getBytes(mSubject));
                        mSubject = v.getString();
                    }

                    Uri realUri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
                    Log.d(TAG, "thiss realUri =" + realUri);
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    Log.d(TAG, "thiss mBoxId =" + mBoxId);
                    if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                        if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
//                            NotificationInd nPdu = (NotificationInd) p.load(realUri);
//                            pduMid = new PduComposer(mContext, nPdu).make(true);
                            pduMid = null;
                        } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
                            RetrieveConf rPdu = (RetrieveConf) p.load(realUri, true);
                            pduMid = new PduComposer(mContext, rPdu).make(true);
                        } else {
                            pduMid = null;
                        }
                    } else {
                        SendReq sPdu = (SendReq) p.load(realUri);
                        pduMid = new PduComposer(mContext, sPdu).make();
                        Log.d(TAG, "thiss SendReq pduMid =" + pduMid);
                    }
                    String mFile = pduFilePath + "/" + System.currentTimeMillis() + FILE_EXT_PDU;
                    if (pduMid != null) {
                        byte[] pduByteArray = pduMid;
                        Log.d(TAG, "thiss fileName =" + mFile);
                        writeToFile(mFile, pduByteArray);
                    }
                    if (pduMid != null) {
                        ContentValues values = new ContentValues();
                        int flag = 0;
                        if (Contacts.indexOf(",") > 0) {
                             flag = ChatMessage.OTM;
                        } else {
                             flag = ChatMessage.OTO;
                        }
                        values.put(FavoriteMsgData.COLUMN_DA_ID, date);
                        values.put(FavoriteMsgData.COLUMN_DA_TIMESTAMP, date);
                        values.put(FavoriteMsgData.COLUMN_DA_CONTACT, Contacts);
                        values.put(FavoriteMsgData.COLUMN_DA_FLAG, flag);
                        values.put(FavoriteMsgData.COLUMN_DA_TYPE, ChatService.MMS);
                        values.put(FavoriteMsgData.COLUMN_DA_FILENAME, mFile);
                        values.put(FavoriteMsgData.COLUMN_DATE, System.currentTimeMillis());
                        if (mBoxId == Mms.MESSAGE_BOX_INBOX) {
                            values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.INCOMING);
                        } else {
                            values.put(FavoriteMsgData.COLUMN_DA_DIRECTION, ChatMessage.OUTCOMING);
                        }
                        if (mSubject != null) {
                            values.put(FavoriteMsgData.COLUMN_DA_BODY, mSubject);
                        }
                        mContext.getContentResolver().insert(FavoriteMsgData.CONTENT_URI, values);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean ipForwardOneMms(Uri mUri) {
        Intent mIntent = RcsMessageUtils.createForwardIntentFromMms(mContext, mUri);
        if (mIntent != null) {
            mContext.startActivity(mIntent);
            mContext.finish();
            return true;
        }
        return false;
    }

    public boolean forwardIpMessage(String mBody) {
        if (TextUtils.isEmpty(mBody)) {
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, mPluginContext.getString(R.string.toast_sms_forward),
                            Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        Intent mIntent = RcsMessageUtils.createForwordIntentFromSms(mContext, mBody);
        if (mIntent != null) {
            mContext.startActivity(mIntent);
            mContext.finish();
            return true;
        }
        return false;
    }

    // forwardTextMessage in plugin
    public boolean forwardTextMessage(ArrayList<Long> smsList, int maxLength) {
        if (needShowNoTextToast(smsList.size())) {
            Log.d(TAG, "forwardTextMessage  needShowNoTextToast");
            mCallback.noSmsForward();
            return true;
        }

        if (smsList.size() > MAX_COUNT_OF_COMBINE_FORWARD) {
            String toastString = mPluginContext.getString(R.string.forward_item_too_much);
            Toast.makeText(mContext, toastString, Toast.LENGTH_LONG).show();
            return true;
        }

//        Collections.sort(smsList);

        Cursor cursor = mAdapter.getCursor();
        RCSColumnsMap column = mAdapter.mRcsColumnsMap;
        StringBuffer strbuf = new StringBuffer();
        String tempbuf = null;
        boolean reachLimitFlag = false;
        long ipId = 0;
        if (cursor.moveToFirst()) {
            do {
                 String type = cursor.getString(column.getColumnMsgType());
                 long msgId = cursor.getLong(column.getColumnMsgId());
                 long key = msgId;
                 if (type.equals("rcs")) {
                     key = RcsMessageListAdapter.getKey(type, key);
                 }
                 if (!mSelectedIpMessageIds.containsKey(key)) {
                     continue;
                 }
                 if (type.equals("sms")) {
                     mCallback.formatSmsBody(msgId, strbuf);
                     strbuf.append("\n");
                 } else if (type.equals("rcs")) {
                     String body = null;
                     String address = null;
                     String formatedBody = null;
                     int messageClass = cursor.getInt(column.mColumnRcsMessageClass);
                     if (messageClass == RcsLog.Class.NORMAL) {
                         body = cursor.getString(column.mColumnRcsMessageBody);
                         address = cursor.getString(column.mColumnRcsMessageAddress);
                     }
                     int direction = cursor.getInt(column.mColumnRcsMessageDirection);
                     boolean isIncoming = direction == RcsLog.Direction.INCOMING;
                     if (!TextUtils.isEmpty(body)) {
                         if (isForwardWithSender() && !TextUtils.isEmpty(address)) {
                             Contact contact = Contact.get(address, false);
                             String number = Contact.formatNameAndNumber(contact.getName(),
                                     contact.getNumber(), "");
                             formatedBody = appendSenderInfoForForwardItem(body, number,
                                                             isIncoming);
                         } else {
                             formatedBody = body;
                         }
                         strbuf.append(formatedBody).append("\n");
                     }
                 }
                 if (strbuf.length() > maxLength && !needShowReachLimit()) {
                     reachLimitFlag = true;
                     /// M: fix bug ALPS00444391, remove the last "\n" when > maxLength @{
                     if (tempbuf != null && tempbuf.endsWith("\n")) {
                         tempbuf = tempbuf.substring(0, tempbuf.length() - 1);
                     }
                     /// @}
                     break;
                 } else {
                     tempbuf = strbuf.toString();
                 }
            } while (cursor.moveToNext());
        }
        if (tempbuf != null && tempbuf.endsWith("\n")) {
            tempbuf = tempbuf.substring(0, tempbuf.length() - 1);
        }


        if (reachLimitFlag) {
            final String contentbuf = tempbuf;
            mContext.runOnUiThread(new Runnable() {
                public void run() {
                    mCallback.showReachLimitDialog(contentbuf);
                }
            });
            return true;
        }

        if (!forwardIpMessage(tempbuf)) {
            mCallback.beginForward(tempbuf);
            return true;
        }
        return true;
    }

    private boolean isForwardWithSender() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean smsForwardWithSender = prefs.getBoolean(SMS_FORWARD_WITH_SENDER, true);
        Log.d(TAG, "isForwardWithSender(): SMS Forward With Sender ?= " + smsForwardWithSender);
        return smsForwardWithSender;
    }

    /**
     * Format a RCS text item for forwarding.
     * @param body text body
     * @param contact contact's number and name formatted string
     * @param isIncoming if the origin item is incoing message, it's true; else it's false
     * @return formated message
     */
    private String appendSenderInfoForForwardItem(String body, String contact, boolean isIncoming) {
        String formattedMessage = null;
        if (isIncoming) {
            formattedMessage = contact + ":\n" + body;
        } else {
            formattedMessage = mPluginContext.getString(R.string.messagelist_sender_self)
                                        + ":\n" + body;
        }
        return formattedMessage;
    }

    @Override
    public boolean startMsgListQuery(AsyncQueryHandler mQueryHandler, int token, Object cookie,
            Uri uri, String[]projection, String selection, String[] selectionArgs, String orderBy) {
        String[] rcsProjections = RcsMessageUtils.combineTwoStringArrays(projection,
                RcsMessageListAdapter.RCS_MESSAGE_PROJECTION_EXTENDS);
        Uri rcsUri = RcsConversation.getConversationUri(mThreadId).buildUpon()
                                         .appendQueryParameter("MultiDelete", "true").build();
        mQueryHandler.startQuery(token, cookie, rcsUri, rcsProjections, selection, selectionArgs,
                orderBy);
        return true;
    }

    public boolean onIpParseDeleteMsg(long key) {
        if (RcsMessageListAdapter.isRcsKey(key)) {
            if (mSelectedIpMessageIds.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initMessageList(IIpMessageListAdapterExt adapter) {
        mAdapter = (RcsMessageListAdapter) adapter;
    }

    @Override
    public long[][] getSelectedMsgIds(Map<Long, Boolean> selectMap, int size) {
        Iterator<Entry<Long, Boolean>> selectIter = selectMap.entrySet().iterator();
        long[][] selectMessageIds = new long[3][size];
        int smsIndex = 0;
        int mmsIndex = 0;
        int rcsIndex = 0;
        while (selectIter.hasNext()) {
            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) selectIter.next();
            if (entry.getValue()) {
                long key = entry.getKey();
                if (key > 0) {
                    if (RcsMessageListAdapter.isRcsKey(key)) {
                        selectMessageIds[2][rcsIndex++] = key;
                    } else {
                        selectMessageIds[0][smsIndex++] = key;
                    }
                } else {
                    selectMessageIds[1][mmsIndex++] = -entry.getKey();
                }
            }
        }
        return selectMessageIds;
    }

    @Override
    public void markAsLocked(final long[][] ids, final boolean lock) {
        // do nothing
        if (ids == null || ids.length < 3) {
            Log.e(TAG, "[markAsLocked] wrong: " + ids);
            return;
        }
        Uri uri = RcsLog.MessageColumn.CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(RcsLog.MessageColumn.LOCKED, lock ? 1 : 0);
        StringBuffer strBuf = new StringBuffer();
        long[] rcsIds = ids[2];
        for (long key : rcsIds) {
            long id = RcsMessageListAdapter.getRcsMsgIdByKey(key);
            strBuf.append(id + ",");
        }
        String str = strBuf.toString();
        String idSelect = str.substring(0, str.length() - 1);
        String args = RcsLog.MessageColumn.ID + " in (" + idSelect + ")";
        mContext.getContentResolver().update(uri, values, args, null);
    }
}
