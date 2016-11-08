package com.mediatek.dialer.plugin;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TextView;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultCallDetailExtension;
import com.mediatek.op01.plugin.R;

@PluginImpl(interfaceName="com.mediatek.dialer.ext.ICallDetailExtension")
public class Op01CallDetailExtension extends DefaultCallDetailExtension {
    private static final String TAG = "Op01CallDetailExtension";

    private static final int BLACK_LIST_MENU_ID = 10002;
    private static final Uri CONTENT_URI = Uri.parse("content://com.cmcc.ccs.black_list");
    public static final String PHONE_NUMBER = "PHONE_NUMBER";
    public static final String NAME = "NAME";
    private static final String[] BLACK_LIST_PROJECTION = {
        PHONE_NUMBER
    };

    /**
     * for op01
     * @param durationView the duration text
     */
    @Override
    public void setDurationViewVisibility(TextView durationView) {
        Log.d(TAG, "setDurationViewVisibility : GONE");
        durationView.setVisibility(View.GONE);
    }

    /**
     * for op01,add for "blacklist" in call detail.
     * @param menu blacklist menu.
     * @param number phone number.
     * @param name contact name.
     */
    @Override
    public void onPrepareOptionsMenu(Context context, Menu menu, CharSequence number,
            CharSequence name) {
        /* feature options    
        if (!SystemProperties.get("ro.mtk_op01_rcs").equals("1") ) {
            return;
        }*/

        Log.d(TAG, "onPrepareOptionsMenu, number: " + number + " name: " + name);
        if (name == null || name.length() == 0) {
            return;
        }

        String strNumber = "";
        String strName = name.toString();
        if (number != null && number.length() > 0) {
            strNumber = number.toString();
        }
        int index = strNumber.indexOf(" ");
        if (index >= 0) {
            strNumber = strNumber.substring(index + 1);
            strNumber = strNumber.replace(",", "");
            strNumber = strNumber.replace(";", "");
        }
        String strCopyName = new String(strName);
        strCopyName = strCopyName.replace(",", "");
        strCopyName = strCopyName.replace(";", "");

        boolean isNumber = isNumberString(strNumber);
        boolean isNameNumber = isNumberString(strCopyName);
        Log.d(TAG, "onPrepareOptionsMenu, isNameNumber: " + isNameNumber + " isNumber: " + isNumber);

        if (isNumber) {
            Log.d(TAG, "onPrepareOptionsMenu, strName: " + strName);
        } else if (isNameNumber){
            strNumber = strCopyName;
            strName = "";
        } else if (!isNumber && !isNameNumber) {
            return;
        }

        final String phoneName = strName;
        final String phoneNumber = strNumber;
        Log.d(TAG, "onPrepareOptionsMenu, phoneNumber: " + phoneNumber + " phoneName: " + phoneName);
        if (phoneNumber == null || phoneNumber.length() == 0) {
            return;
        }

        MenuItem blackListMenu = menu.findItem(BLACK_LIST_MENU_ID);
        boolean isAutoRejectNumber = autoReject(context, phoneNumber);
        if (blackListMenu != null) {
            menu.removeItem(BLACK_LIST_MENU_ID);
        }

        int menuIndex = menu.size();
        final Context fnContext = context;
        try {
            final Context pluginContext = context.createPackageContext("com.mediatek.op01.plugin",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            if (isAutoRejectNumber) {
                blackListMenu = menu.add(Menu.NONE, BLACK_LIST_MENU_ID, menuIndex,
                        pluginContext.getText(R.string.remove_black_list));
                blackListMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        removeblackNumber(fnContext, phoneNumber);
                        return true;
                    }
                });
            } else {
                blackListMenu = menu.add(Menu.NONE, BLACK_LIST_MENU_ID, menuIndex,
                        pluginContext.getText(R.string.add_black_list));
                blackListMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        addblackNumber(fnContext, phoneNumber, phoneName);
                        return true;
                    }
                });
            }
        }  catch (NameNotFoundException e) {
            Log.d(TAG, "no com.mediatek.op01.plugin packages");
        }
    }

    /**
     * check if the call should be rejected.
     * @param number the incoming call number.
     * @return the result that the current number should be auto reject.
     */
    public boolean autoReject(Context context, String number) {
        Log.d(TAG, "auto Reject");
        boolean result = false;
        try {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                    BLACK_LIST_PROJECTION, null, null, null);
            if (cursor == null) {
                Log.d(TAG, "cursor is null...");
                return false;
            }
            try {
                String blockNumber;
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    blockNumber = cursor.getString(0);
                    if (PhoneNumberUtils.compare(number, blockNumber)) {
                        result = true;
                        break;
                    }
                    cursor.moveToNext();
                }
            }
            finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "autoReject cursor exception");
        }
        return result;
    }

    /**
     * Add a pair of number and name to device's blacklist.
     * @param number the phone number, it's mandatory.
     * @param name the display name, it's optional.
     * @return ture if the data was added successfully, otherwise false.
     */
    public boolean addblackNumber(Context context, String number, String name) {

        Log.d(TAG, "add black Number");

        ContentValues values = new ContentValues();
        values.put(PHONE_NUMBER, number);
        values.put(NAME, name);

        Uri resultUri = context.getContentResolver().insert(CONTENT_URI, values);

        if (resultUri == null) {
            return false;
        }

        return true;
    }

    /**
     * Remove a number from device's blacklist.
     * @param number the phone number, it's mandatory.
     * @return ture if the data was removed successfully, otherwise false.
     */
    public boolean removeblackNumber(Context context, String number) {
        Log.d(TAG, "remove black Number");
        Uri uri = Uri.withAppendedPath(CONTENT_URI, Uri.encode(number));
        int retCount = context.getContentResolver().delete(uri, null, null);
        if (retCount <= 0) {
            return false;
        }

        return true;
    }

    /**
     * @param string.
     * @return true if string is number string.
     */
    public boolean isNumberString(String string) {
        boolean isNumber = false;
        String strCopy = new String(string);
        /*char firstChar = strCopy.charAt(0);
        if (firstChar == '+') {
            strCopy = strCopy.substring(1);
        }*/
        int index = strCopy.indexOf('+');
        Log.d(TAG, "isNumberString index: " + index);
        if(index == 0) {
            strCopy = strCopy.substring(index+1);
        }
        if (strCopy.length() > 0) {
            isNumber = strCopy.matches("[0-9]+");
        }
        Log.d(TAG, "isNumberString strCopy: " + strCopy);
        return isNumber;
    }
}
