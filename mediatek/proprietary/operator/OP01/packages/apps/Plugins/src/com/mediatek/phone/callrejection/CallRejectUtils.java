package com.mediatek.phone.callrejection;



import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import com.mediatek.op01.plugin.R;

public class CallRejectUtils {

    private static final String LOG_TAG = "CallRejectUtils";

    private static final int REJECT_LIST_FULL = 100;
    private static final Uri rejectUri = Uri.parse("content://reject/list");

    /** This class is never instantiated. */
    private CallRejectUtils() {
    }

    public static String allWhite(String str) {
        if (str != null) {
            str = str.replaceAll(" ", "");
        }
        return str;
    }

    public static boolean equalsNumber(String number1, String number2) {
        log("equalsNumber:number:" + number1 + " DBnumber:" + number2);
        if (number1 == null || number2 == null) {
            return false;
        }
        boolean isEquals = false;

        if (number1.equals(number2) || allWhite(number1).equals(allWhite(number2))) {
            isEquals = true;
        } else {
            isEquals = false;
        }
        log("equalsNumber:number:" + number1 + " DBnumber:" + number2 + " isEquals:" + isEquals);
        return isEquals;
    }

    public static ArrayList<String> getContactsNames(Context context, ArrayList<String> numbers) {
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < numbers.size(); i++) {
            String name = "";
            Cursor contactCursor = context.getContentResolver().query(
                    Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numbers.get(i))),
                    new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER}, null, null, null);
            if (contactCursor == null) return null;
            contactCursor.moveToFirst();
            try {
                /* seek all name and number*/
                while (!contactCursor.isAfterLast()) {
                    name = contactCursor.getString(0);
                    break;
                }
            } finally {
                contactCursor.close();
            }
            log("getContactsNames name:" + name );
            if (name.length() == 0) {
                name = context.getResources().getString(R.string.call_reject_no_name);
            }
            names.add(name) ;
        }
        return names;
    }

    public static String getContactsName(Context context, String number) {
        String name = "";
        Cursor contactCursor = context.getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER}, null, null, null);
        if (contactCursor == null) return null;
        contactCursor.moveToFirst();
        try {
                /* seek all name and number*/
            while (!contactCursor.isAfterLast()) {
                name = contactCursor.getString(0);
                break;
            }
        } finally {
            contactCursor.close();
        }
        if (name.length() == 0) {
            name = context.getResources().getString(R.string.call_reject_no_name);
        }
        return name;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}