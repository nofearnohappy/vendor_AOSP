package com.hesine.nmsg.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.thirdparty.PNMessageHandler;

@SuppressLint({ "SdCardPath", "SimpleDateFormat" })
@SuppressWarnings("deprecation")
public class CommonUtils {

    public static void copy(String src, String dest) {// **********
        InputStream is = null;
        OutputStream os = null;

        try {
            is = new BufferedInputStream(new FileInputStream(src));
            os = new BufferedOutputStream(new FileOutputStream(dest));

            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        MLog.error(MLog.getStactTrace(e));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    MLog.error(MLog.getStactTrace(e));
                }
            }
        }
    }

    public static void nmsStream2File(byte[] stream, String filepath) throws IOException {
        FileOutputStream outStream = null;
        try {
            File f = new File(filepath);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            outStream = new FileOutputStream(f);
            outStream.write(stream);
            outStream.flush();
        } catch (IOException e) {
            MLog.error(MLog.getStactTrace(e));
            throw new RuntimeException(e.getMessage());
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    MLog.error(MLog.getStactTrace(e));
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public static boolean isPhoneNumberValid(String number) {
        boolean isValid = false;
        if (number == null || number.length() <= 0) {
            MLog.info("isPhoneNumberValid, number is null");
            return false;
        }
        Pattern phonePattern = Pattern.compile(// sdd = space, dot, or dash
                "(\\+[0-9]+[\\- \\.]*)?" // +<digits><sdd>*
                        + "(\\([0-9]+\\)[\\- \\.]*)?" // (<digits>)<sdd>*
                        + "([0-9][0-9\\- \\.][0-9\\- \\.]+[0-9])");
        Matcher matcher = phonePattern.matcher(number);
        isValid = matcher.matches();
        return isValid;
    }

    public static void createLoseSDCardNotice(Context context) {
        Toast.makeText(context, com.hesine.nmsg.R.string.chat_lose_sdcard, Toast.LENGTH_SHORT)
                .show();
    }

    public static void copyToClipboard(Context context, String s) {
        ClipboardManager cm = (ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(s);
    }

    public static boolean currentActivityIsNmsg(String account) {
        ActivityManager am = (ActivityManager) Application.getInstance().getSystemService(
                Application.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(2).get(0).topActivity;
        if (cn.getClassName().endsWith("com.hesine.nmsg.ui.ConversationActivity")) {
            String currentAccount = GlobalData.instance().getCurServiceAccount();
            if (null != currentAccount) {
                return currentAccount.equals(account);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isExistSystemContactViaAccount(ServiceInfo si, boolean judgeName) {
        String account = si.getAccount();
        if (TextUtils.isEmpty(account)) {
            MLog.error("isExistSystemContactViaEmail. email is empty!");
            return false;
        }

        String encodeAccount = Uri.encode(account);
        if (TextUtils.isEmpty(encodeAccount)) {
            MLog.error("isExistSystemContactViaEmail. encodeEmail is empty!");
            return false;
        }
        Cursor cursor = null;
        boolean result = false;
        try {
            Uri lookupUri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, encodeAccount);
            cursor = Application.getInstance().getContentResolver()
                    .query(lookupUri, null, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                if (cursor.getCount() > 0) {
                    if(judgeName){
                        if (cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
                                .equals(si.getName())) {
                            result = true;
                            break;
                        }
                    }else{
                        result = true;
                        break;
                    }
                }
            }

        } catch (NullPointerException e) {
            MLog.error("email: " + account + ". encodeEmail: " + encodeAccount+" e.toString():"+e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return result;
    }

    public static boolean addContactInPhonebook(ServiceInfo si) {
        Bitmap bp = Image.getBitmapFromFile(si.getIcon());
        ContentResolver resolver = Application.getInstance().getContentResolver();
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation op1 = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_NAME, null).build();
        operations.add(op1);

        Uri uri = Data.CONTENT_URI;
        ContentProviderOperation op2 = ContentProviderOperation.newInsert(uri)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, si.getName()).build();
        operations.add(op2);

        if (bp != null) {
            ContentProviderOperation op3 = ContentProviderOperation.newInsert(uri)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, Image.bitmap2Bytes(bp)).build();
            bp.recycle();
            bp = null;
            operations.add(op3);
        }
        ContentProviderOperation op4 = ContentProviderOperation.newInsert(uri)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                .withValue(Email.DATA, si.getAccount()).withValue(Email.TYPE, Email.TYPE_WORK)
                .build();
        operations.add(op4);
    
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operations);
            } catch (RemoteException e) {
                MLog.error(MLog.getStactTrace(e));
            } catch (OperationApplicationException e) {
                MLog.error(MLog.getStactTrace(e));
            }
            return true;
       
    }

    public static void updateContact(ServiceInfo si) {
        Cursor c = null;
        try {
            ContentResolver cr = Application.getInstance().getContentResolver();
            int rawContactId = 0;
            Uri uri = Data.CONTENT_URI;
            String select = String.format("%s=? AND %s='%s'", Email.DATA, Data.MIMETYPE,
                    Email.CONTENT_ITEM_TYPE);
            String[] project = new String[] { Data.RAW_CONTACT_ID };
            c = cr.query(uri, project, select, new String[] { si.getAccount() }, null);

            if (null != c && c.moveToFirst()) {
                rawContactId = c.getInt(c.getColumnIndex(Data.RAW_CONTACT_ID));
            } else {
                if (null != c) {
                    c.close();
                    c = null;
                }
                return;
            }

            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation op = ContentProviderOperation
                    .newUpdate(uri)
                    .withSelection(
                            Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?",
                            new String[] { String.valueOf(rawContactId),
                                    StructuredName.CONTENT_ITEM_TYPE })
                    .withValue(StructuredName.DISPLAY_NAME, si.getName())
                    .withValue(StructuredName.GIVEN_NAME, null)
                    .withValue(StructuredName.FAMILY_NAME, null)
                    .withValue(StructuredName.MIDDLE_NAME, null)
                    .withValue(StructuredName.SUFFIX, null).withValue(StructuredName.PREFIX, null)
                    .build();
            operations.add(op);

            cr.applyBatch(ContactsContract.AUTHORITY, operations);

        } catch (RemoteException e) {
            MLog.error(MLog.getStactTrace(e));
        } catch (OperationApplicationException e) {
            MLog.error(MLog.getStactTrace(e));
        } finally {
            if (null != c) {
                c.close();
                c = null;
            }
        }
    }
    
    public static void updateContactPhoto(ServiceInfo si) {
        Bitmap bp = Image.getBitmapFromFile(si.getIcon());
        Cursor c = null;
        if (null != bp) {
            try {
                ContentResolver cr = Application.getInstance().getContentResolver();
                int rawContactId = 0;
                int photoRow = 0;
                String select = String.format("%s=? AND %s='%s'", Email.DATA, Data.MIMETYPE,
                        Email.CONTENT_ITEM_TYPE);
                String[] project = new String[] { Data.RAW_CONTACT_ID };
                c = cr.query(Data.CONTENT_URI, project, select, new String[] { si.getAccount() },
                        null);

                if (null != c && c.moveToFirst()) {
                    rawContactId = c.getInt(c.getColumnIndex(Data.RAW_CONTACT_ID));
                }
                if (null != c) {
                    c.close();
                    c = null;
                }

                String where = Data.RAW_CONTACT_ID + " = " + rawContactId + " AND " + Data.MIMETYPE
                        + "='" + Photo.CONTENT_ITEM_TYPE + "'";
                c = cr.query(Data.CONTENT_URI, null, where, null, null);
                if (null != c && c.moveToFirst()) {
                    photoRow = c.getInt(c.getColumnIndexOrThrow(Data._ID));
                }

                if (null != c) {
                    c.close();
                    c = null;
                }

                ContentValues values = new ContentValues();

                values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                // values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
                values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, Image.bitmap2Bytes(bp));
                bp.recycle();
                bp = null;
                values.put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                if (photoRow > 0) {
                    cr.update(ContactsContract.Data.CONTENT_URI, values, Data._ID + " = "
                            + photoRow, null);
                } else {
                    cr.insert(Data.CONTENT_URI, values);
                }
            } catch (NullPointerException e) {
                MLog.error(MLog.getStactTrace(e));
                if (null != c) {
                    c.close();
                    c = null;
                }
            }
        }
    }

    public static List<String> getLatestWifiMsgIdsArray() {
        String strAll = Config.getLatestWifiMsgIds();
        List<String> arrayAccountMsgIds = new ArrayList<String>();
        if (TextUtils.isEmpty(strAll)) {
            return arrayAccountMsgIds;
        }
        String[] strsAccountMsgIds = strAll.split("[#]");
        if (null != strsAccountMsgIds) {
            for (String strAccountMsgIds : strsAccountMsgIds) {
                arrayAccountMsgIds.add(strAccountMsgIds);
            }
        }
        return arrayAccountMsgIds;
    }

    public static void saveLatestWifiMsgIdsArray(List<String> arrayAccountMsgIds) {
        StringBuilder strAll = new StringBuilder();
        int size = arrayAccountMsgIds.size();
        for (int i = 0; i < arrayAccountMsgIds.size(); i++) {
            if (i == size - 1) {
                strAll.append(arrayAccountMsgIds.get(i));
            } else {
                strAll.append(arrayAccountMsgIds.get(i) + "#");
            }
        }
        Config.saveLatestWifiMsgIds(strAll.toString());
    }

    public static final int CACHE_WIFI_MSG_COUNT = 10;

    public static void addLatestWifiMsgId(String account, List<String> msgIds) {
        List<String> arrayAccountMsgIds = getLatestWifiMsgIdsArray();
        int size = msgIds.size();
        int startOffset = 0;
        if (size > CACHE_WIFI_MSG_COUNT) {
            startOffset = size - CACHE_WIFI_MSG_COUNT;
        }

        for (int i = startOffset; i < size; i++) {
            if (arrayAccountMsgIds.size() >= CACHE_WIFI_MSG_COUNT) {
                arrayAccountMsgIds.remove(0);
            }
            arrayAccountMsgIds.add(account + "-" + msgIds.get(i));
        }
        saveLatestWifiMsgIdsArray(arrayAccountMsgIds);
    }

    public static void clearLatestWifiMsgIds() {
        Config.saveLatestWifiMsgIds(null);
    }

    public static synchronized void procRequestLatestWifiMsg() {
        if (!DeviceInfo.isNetworkReady(Application.getInstance())) {
            return;
        }
        List<String> strs = CommonUtils.getLatestWifiMsgIdsArray();
        if (null == strs || strs.size() == 0) {
            return;
        }
        List<String> msgIds = new ArrayList<String>();
        for (String str : strs) {
            String[] strArray = str.split("-");
            String account = strArray[0];
            String msgId = strArray[1];
            msgIds.clear();
            msgIds.add(msgId);
            PNMessageHandler.procRequestMsgs(account, msgIds);
        }
        CommonUtils.clearLatestWifiMsgIds();
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
