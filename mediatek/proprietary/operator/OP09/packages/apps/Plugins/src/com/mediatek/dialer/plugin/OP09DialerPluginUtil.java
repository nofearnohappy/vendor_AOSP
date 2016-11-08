package com.mediatek.dialer.plugin;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Contacts.People;
import android.provider.Contacts.Intents.Insert;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.util.Constants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.telephony.TelephonyManagerEx;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class OP09DialerPluginUtil {

    private static final String TAG = "OP09DialerPluginUtil";

    private Context mPluginContext;
    private Context mHostContext;
    private int mTimezoneRawOffset = 0;
    private static final String TEL_SERVICE = "TelephonyConnectionService";

    public OP09DialerPluginUtil(Context context) {
        mHostContext = context.getApplicationContext();
        try {
            mPluginContext = context.createPackageContext("com.mediatek.op09.plugin", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "no com.mediatek.op09.plugin packages");
        }
    }

    public Context getPluginContext() {
        return mPluginContext;
    }

    public Context getHostContext() {
        return mHostContext;
    }

    public static boolean isSimInsert(final int slot) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        boolean isSimInsert = false;
        log("[isSimInserted] slot :" + slot);
        try {
            if (iTel != null && SubscriptionManager.isValidSlotId(slot)) {
                isSimInsert = iTel.hasIccCardUsingSlotId(slot);
                log("[isSimInserted] isSimInsert :" + isSimInsert);
            }
        } catch (RemoteException e) {
            log("[isSimInserted]catch exception:");
            e.printStackTrace();
            isSimInsert = false;
        }
        return isSimInsert;
    }

    /**
     * @return subId for a slotId
     */
    public static int getSubIdUsingSlotId(int slotId) {
        int[] sub = SubscriptionManager.getSubId(slotId);
        int subId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        if (sub != null) {
            subId = sub[0];
        }
        return subId;
    }

    public static PhoneAccountHandle getPstnPhoneAccountHandleBySlotId(
                Context context, int slotId) {
        PhoneAccountHandle accountHandle = null;
        TelecomManager telecomManager = TelecomManager.from(context);
        TelephonyManager telephonyManager = TelephonyManager.from(context);

        final List<PhoneAccountHandle> accountHandles = telecomManager
                .getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : accountHandles) {
            if (handle.getComponentName().getShortClassName().endsWith(TEL_SERVICE)) {
                if (handle.getId() != null) {
                    PhoneAccount account = telecomManager.getPhoneAccount(handle);
                    int subId = telephonyManager.getSubIdForPhoneAccount(account);
                    log("subId: " + subId + " slot " + SubscriptionManager.getSlotId(subId));
                    if (slotId == SubscriptionManager.getSlotId(subId)) {
                        return handle;
                    }
                }
            }
        }
        return accountHandle;
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account} and {@code VideoCallProfile} state.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(String number, PhoneAccountHandle accountHandle) {
        final Intent intent = new Intent(Intent.ACTION_CALL, getCallUri(number));
        if (accountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
        }

        return intent;
    }
    /**
     * Return Uri with an appropriate scheme, accepting both SIP and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (isUriNumber(number)) {
            return Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null);
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
    }

    public static boolean isUriNumber(String number) {
        // Note we allow either "@" or "%40" to indicate a URI, in case
        // the passed-in string is URI-escaped. (Neither "@" nor "%40"
        // will ever be found in a legal PSTN number.)
        return number != null && (number.contains("@") || number.contains("%40"));
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
