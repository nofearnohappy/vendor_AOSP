/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.contacts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.mediatek.rcs.contacts.ext.ContactExtention.OnPresenceChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilitiesListener;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.Intents;
import org.gsma.joyn.JoynServiceConfiguration;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;

/**
 * This class manages the APIs which are used by plug-in, providing a convenient
 * way for API invocations.
 */
public class PluginApiManager {
    public static final String TAG = "PluginApiManager";

    private static PluginApiManager sInstance = null;
    private MyCapabilitiesListener mMyCapabilitiesListener = null;
    private MyJoynServiceListener mMyJoynServiceListener = null;
    private CapabilityService mCapabilitiesApi = null;
    private boolean mRcsEnabled = false;
    private Context mContext = null;
    private static final int MAX_CACHE_SIZE = 2048;
    private final LruCache<String, ContactInformation> mContactsCache =
            new LruCache<String, ContactInformation>(MAX_CACHE_SIZE);
    private final LruCache<Long, List<String>> mCache =
            new LruCache<Long, List<String>>(MAX_CACHE_SIZE);
    private static final String CONTACT_CAPABILITIES =
            "com.orangelabs.rcs.capability.CONTACT_CAPABILITIES";
    private static final String INTENT_RCS_ON =
            "com.mediatek.intent.rcs.stack.LaunchService";
    private static final String INTENT_RCS_OFF =
            "com.mediatek.intent.rcs.stack.StopService";
    private final List<CapabilitiesChangeListener> mCapabilitiesChangeListenerList =
            new ArrayList<CapabilitiesChangeListener>();
    private final List<RegistrationListener> mRegistrationListeners =
            new CopyOnWriteArrayList<RegistrationListener>();
    private Cursor mCursor = null;
    private List<Long> mQueryOngoingList = new ArrayList<Long>();
    private final ConcurrentHashMap<Long, OnPresenceChangedListener> mPresenceListeners =
            new ConcurrentHashMap<Long, OnPresenceChangedListener>();
    /**
     * MIME type for RCS registration state.
     */
    private static final String MIMETYPE_REGISTRATION_STATE =
            "vnd.android.cursor.item/com.orangelabs.rcs.registration-state";
    /**
     * MIME type for RCSE capabilities.
     */
    private static final String MIMETYPE_RCSE_CAPABILITIES =
            "vnd.android.cursor.item/com.orangelabs.rcse.capabilities";

    private static final String CN_NUMBER_PREFEX = "+86";
    private static final String KEY_CONTACT_NUMBER = "contact_number";
    private static final String KEY_REGISTRATION_STATE = "registration_state";
    private static final String KEY_BURN_AFTER_READ = "burn_after_reading";
    private static final Uri EAB_CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.eab/eab");
    private static final int RCS_CONTACT = 1;
    private static final int RCS_CAPABLE_CONTACT = 2;
    private static final int REGISTRATION_STATUS_ONLINE = 1;

    /**
     * The CapabilitiesChangeListener defined as a listener to notify the
     * specify observer that the capabilities has been changed.
     *
     * @see CapabilitiesChangeEvent
     */
    public interface CapabilitiesChangeListener {

        /**
         * On capabilities changed.
         *
         * @param contact the contact
         * @param contactInformation the contact information
         */
        void onCapabilitiesChanged(String contact,
                ContactInformation contactInformation);

        /**
         * Called when CapabilityApi connected status is changed.
         *
         * @param isConnected
         *            True if CapabilityApi is connected.
         */
        void onApiConnectedStatusChanged(boolean isConnected);
    }

    /**
     * Add presence changed listener.
     *
     * @param listener
     *            The presence changed listener.
     * @param contactId
     *            The contact id.
     */
    public void addOnPresenceChangedListener(
            OnPresenceChangedListener listener, long contactId) {
        mPresenceListeners.put(contactId, listener);
    }

    /**
     * Register the CapabilitiesChangeListener.
     *
     * @param listener            The CapabilitiesChangeListener used to register
     */
    public void addCapabilitiesChangeListener(
            CapabilitiesChangeListener listener) {
        Log.v(TAG, "addCapabilitiesChangeListener(), listener = " + listener);
        mCapabilitiesChangeListenerList.add(listener);
    }

    /**
     * Unregister the CapabilitiesChangeListener.
     *
     * @param listener            The CapabilitiesChangeListener used to unregister
     */
    public void removeCapabilitiesChangeListener(
            CapabilitiesChangeListener listener) {
        Log.v(TAG, "removeCapabilitiesChangeListener(), listener = "
                + listener);
        mCapabilitiesChangeListenerList.remove(listener);
    }

    /**
     * The RegistrationListener defined as a listener to notify the specify
     * observer that the registration status has been changed and
     * RegistrationAPi connected status.
     *
     * @see RegistrationEvent
     */
    public interface RegistrationListener {
        /**
         * Called when RegistrationApi connected status is changed.
         *
         * @param isConnected
         *            True if RegistrationApi is connected
         */
        void onApiConnectedStatusChanged(boolean isConnected);

        /**
         * Called when the status of RCS-e account is registered.
         *
         * @param status
         *            Current status of RCS-e account.
         */
        void onStatusChanged(boolean status);

        /**
         * Called when the rcse core service status has been changed.
         *
         * @param status
         *            Current status of rcse core service.
         */
        void onRcsCoreServiceStatusChanged(int status);

    }

    /**
     * Register the RegistrationListener.
     * @param listener            The RegistrationListener used to register
     */
    public void addRegistrationListener(RegistrationListener listener) {
        Log.v(TAG, "addRegistrationListener(), listener = " + listener);
        mRegistrationListeners.add(listener);
    }

    /**
     * Unregister the RegistrationListener.
     * @param listener            The RegistrationListener used to unregister
     */
    public void removeRegistrationListener(RegistrationListener listener) {
        Log.v(TAG, "removeRegistrationListener(), listener = "
                + listener);
        mRegistrationListeners.remove(listener);
    }

    /**
     * The class including some informations of contact: whether it is an Rcse
     * contact, the capabilities of IM, file transfer,CS call,image and video
     * share.
     */
    public static class ContactInformation {
        public int isRcsContact = 0; // 0 indicate not Rcs, 1 indicate Rcs
        public boolean isReadBurnSupported = false;
    }

    /**
     * Get the presence of number.
     *
     * @param snumber
     *            The number whose presence to be queried.
     * @return The presence of the number.
     */
    public int getContactPresencebyNumber(String snumber) {
        //Log.d(TAG, "getContactPresencebyNumber(), number:" + snumber);
        final String number = getAvailableNumber(snumber);
        ContactInformation info = null;
        synchronized (mContactsCache) {
            info = mContactsCache.get(number);
        }
        if (info != null) {
            Log.d(TAG, "getContactPresencebyNumber(), number:" + number
                    + " getisRcs:" + info.isRcsContact);
            return info.isRcsContact;
        } else {
            ContactInformation defaultInfo = new ContactInformation();
            synchronized (mContactsCache) {
                Log.d(TAG, "getContactPresencebyNumber(), setisRcs:0" + number + ",0");
                mContactsCache.put(number, defaultInfo);
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    queryNumberPresence(number);
                }
            });
        }
        return 0;
    }

    /**
     * Core service API connection.
     */
    private ServiceConnection mApiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    /**
     * Inits the data.
     */
    private void initData() {
        //Log.d(TAG, "initData() entry");
        Thread thread = new Thread() {
            public void run() {
                Looper.prepare();
                registerObserver();
                initContactsPresence();
                IntentFilter filter = new IntentFilter();
                filter.addAction(CONTACT_CAPABILITIES);
                filter.addAction(Intents.Client.SERVICE_UP);
                filter.addAction(INTENT_RCS_ON);
                filter.addAction(INTENT_RCS_OFF);
                mContext.registerReceiver(mBroadcastReceiver, filter);
            }
        };
        thread.start();
        Log.d(TAG, "initData() exit");
    }

    /**
     * Get presence of contact id.
     *
     * @param contactId
     *            The contact id whose presence to be queried.
     * @return The presence of the contact.
     */
    public int getContactPresencebyId(final long contactId) {
        final List<String> numbers = mCache.get(contactId);
        Log.d(TAG, "getContactPresencebyId(), contactId: " + contactId
                + " numbers: " + numbers);
        if (numbers != null) {
            synchronized (mPresenceListeners) {
                mPresenceListeners.remove(contactId);
            }
            ContactInformation info = null;
            boolean hasNull = false;
            for (String number : numbers) {
                number = getAvailableNumber(number);
                info = mContactsCache.get(number);
                if (info != null) {
                    //Log.d(TAG, "getisRCS:" + info.isRcsContact);
                    if (info.isRcsContact == 1) {
                        return 1;
                    }
                } else {
                    hasNull = true;
                }
            }
            if (mQueryOngoingList.contains(contactId)) {
                //Log.d(TAG, "getContactPresencebyId():" + contactId
                //        + " query is ongoing");
                return 0;
            }
            if (hasNull) {
                mQueryOngoingList.add(contactId);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        final List<String> list = getNumbersByContactId(contactId);
                        queryPresencebyId(contactId, list);
                        mQueryOngoingList.remove(contactId);
                    }
                });
            }
        } else {
            if (mQueryOngoingList.contains(contactId)) {
                //Log.d(TAG, "getContactPresencebyId():" + contactId
                //        + " query is ongoing");
                return 0;
            }
            mQueryOngoingList.add(contactId);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final List<String> list = getNumbersByContactId(contactId);
                    queryPresencebyId(contactId, list);
                    mQueryOngoingList.remove(contactId);
                }
            });
        }
        return 0;
    }

    /**
     * Register observer.
     */
    private void registerObserver() {
        //Log.d(TAG, "registerObserver entry");
        if (mCursor != null && mCursor.isClosed()) {
            Log.d(TAG, "registerObserver() close cursor");
            mCursor.close();
        }
        // Query contactContracts phone database
        mCursor = mContext.getContentResolver().query(Phone.CONTENT_URI, null,
                null, null, null);
        if (mCursor != null) {
            // Register content observer
            Log.d(TAG, "registerObserver() begin");
            mCursor.registerContentObserver(new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d(TAG, "onChange() entry" + selfChange);
                    if (mCache == null) {
                        Log.d(TAG, "onChange() mCache is null");
                        return;
                    }
                    Map<Long, List<String>> map = mCache.snapshot();
                    if (map != null) {
                        Set<Long> keys = map.keySet();
                        for (Long key : keys) {
                            getNumbersByContactId(key);
                        }
                    } else {
                        Log.d(TAG, "onChange() map is null");
                    }
                }
            });
        } else {
            Log.d(TAG, "registerObserver mCursor is null");
        }
        //Log.d(TAG, "registerObserver exit");
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (CONTACT_CAPABILITIES.equals(action)) {

                String number = intent.getStringExtra("contact");
                Capabilities capabilities = intent
                        .getParcelableExtra("capabilities");
                ContactInformation info = new ContactInformation();
                if (capabilities == null) {
                    Log.d(TAG, "onReceive(),capabilities get null");
                    return;
                }
                info.isRcsContact = capabilities
                        .isSupportedRcseContact() ? 1 : 0;
                info.isReadBurnSupported = capabilities.isBurnAfterRead();
                number = getAvailableNumber(number);
                synchronized (mContactsCache) {
                    Log.d(TAG, "onReceive(), setisRcs:" + info.isRcsContact + number
                            + "," + info.isReadBurnSupported);
                    mContactsCache.put(number, info);
                }
                for (CapabilitiesChangeListener listener : mCapabilitiesChangeListenerList) {
                    if (listener != null) {
                        listener.onCapabilitiesChanged(number, info);
                        return;
                    }
                }
            } else if (Intents.Client.SERVICE_UP.equals(action)) {
                Log.d(TAG, "onreceive(), rcscoreservice action");
                mCapabilitiesApi.connect();
            } else if (INTENT_RCS_ON.equals(action)) {
                mRcsEnabled = true;
                Log.d(TAG, "onreceive(), rcs on");
            } else if (INTENT_RCS_OFF.equals(action)) {
                mRcsEnabled = false;
                Log.d(TAG, "onreceive(), rcs off");
            }
        }
    };

    /**
     * Query contacts presence.
     */
    private void initContactsPresence() {
        Log.d(TAG, "initContactsPresence() entry");
        Builder builder = Phone.CONTENT_URI.buildUpon();
        builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");
        Uri uri = builder.build();
        String[] conProjection = {Phone.CONTACT_ID};

        // Get all registration state number entries
        String[] eabProjection = {KEY_CONTACT_NUMBER, KEY_BURN_AFTER_READ};
        String eabWhere = KEY_REGISTRATION_STATE + " LIKE '" + REGISTRATION_STATUS_ONLINE + "'";
        Cursor eabCur = mContext.getContentResolver().query(
                EAB_CONTENT_URI,
                eabProjection,
                eabWhere,
                null,
                null);
        if (eabCur != null) {
            while (eabCur.moveToNext()) {
                String number = eabCur.getString(0);
                Log.d(TAG, "initContactsPresence(): " + number);
                number = getAvailableNumber(number);
                ContactInformation info = new ContactInformation();
                info.isRcsContact = 1;
                info.isReadBurnSupported =
                    eabCur.getInt(1) == 1 ? true : false;
                synchronized (mContactsCache) {
                    Log.d(TAG, "initContactsPresence(), setisRcs:1"
                        + number + "," + info.isReadBurnSupported);
                    mContactsCache.put(number, info);
                }

                // Get contactId
                String conWhere = Phone.NUMBER + " LIKE '%" + number + "'";
                Cursor idCur = mContext.getContentResolver().query(
                        uri,
                        conProjection,
                        conWhere,
                        null,
                        null);
                if (idCur != null) {
                    while (idCur.moveToNext()) {
                        getNumbersByContactId(idCur.getLong(0));
                    }
                    idCur.close();
                }
            }

            eabCur.close();
        }

        Log.d(TAG, "initContactsPresence() exit");
    }

    /**
     * Query contacts presence.
     */
    private String getAvailableNumber(String number) {
        int size = number.length();
        String string = null;
        Log.d(TAG, "getAvailableNumber(), " + size);
        //if (size < 11) {
        //    return null;
        //}
        if (size == 14) {
            if (0 == CN_NUMBER_PREFEX.compareTo(number.substring(0, 3))) {
                return number.substring(size - 11);
            }
        }

        return number;
    }

    /**
     * Query contact presence.
     * @param number the number
     */
    private void queryNumberPresence(String number) {
        final String numbers = getAvailableNumber(number);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "queryNumberPresence(), number:" + numbers);
                getNumberCapabilities(numbers);
            }
        });
    }

    /**
     * Query a series of phone number.
     *
     * @param numbers The phone numbers list need to query
     */
    public void queryNumbersPresence(List<String> numbers) {
        for (String number : numbers) {
            Log.d(TAG, "queryNumbersPresence number: " + number);
            number = getAvailableNumber(number);
            try {
                getNumberCapabilities(number);
                if (mCapabilitiesApi != null) {
                    mCapabilitiesApi.requestContactCapabilities(number);
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Query capabilities of the phone number.
     * @param number The phone numbers list need to query
     */
    private ContactInformation getNumberCapabilities(String number) {
        String[] eabProjection = {KEY_REGISTRATION_STATE, KEY_BURN_AFTER_READ};
        String eabWhere = KEY_CONTACT_NUMBER + " LIKE '+86" + number + "'";
        Cursor eabCur = mContext.getContentResolver().query(
                EAB_CONTENT_URI,
                eabProjection,
                eabWhere,
                null,
                null);
        ContactInformation info = new ContactInformation();
        if (eabCur != null) {
            while (eabCur.moveToNext()) {
                info.isRcsContact =
                    eabCur.getInt(0) == 1 ? 1 : 0;
                info.isReadBurnSupported =
                    eabCur.getInt(1) == 1 ? true : false;
            }
            eabCur.close();
        }

        synchronized (mContactsCache) {
            Log.d(TAG, "getNumberCapabilities(), setisRcs:"
                    + info.isRcsContact + number + " " + info.isReadBurnSupported);
            mContactsCache.put(number, info);
        }
        return info;
    }

    /**
     * Query a series of phone number.
     *
     * @param contactId            The contact id
     * @param numbers            The phone numbers list need to query
     */
    private void queryPresencebyId(long contactId, List<String> numbers) {
        Log.d(TAG, "queryPresencebyId() entry, contactId: " + contactId);
        boolean needNotify = false;
        for (String number : numbers) {
            number = getAvailableNumber(number);
            ContactInformation cachedInfo = mContactsCache.get(number);
            if (cachedInfo == null) {
                cachedInfo = getNumberCapabilities(number);
            }

            if (cachedInfo.isRcsContact == 1) {
                needNotify = true;
            }
        }
        synchronized (mPresenceListeners) {
            if (needNotify) {
                OnPresenceChangedListener listener = mPresenceListeners
                        .get(contactId);
                if (listener != null) {
                    listener.onPresenceChanged(contactId, 1);
                }
            }
            mPresenceListeners.remove(contactId);
        }
        Log.d(TAG, "queryPresencebyId() contactId: " + contactId
                + " needNotify: " + needNotify);
    }

    /**
     * Obtain the phone numbers from a specific contact id.
     *
     * @param contactId            The contact id
     * @return The phone numbers of the contact id
     */
    public List<String> getNumbersByContactId(long contactId) {
        List<String> list = new ArrayList<String>();
        String[] projection = { Phone.NUMBER };
        String selection = Phone.CONTACT_ID + "=? ";
        String[] selectionArgs = { Long.toString(contactId) };
        Cursor cur = mContext.getContentResolver().query(Phone.CONTENT_URI,
                projection, selection, selectionArgs, null);
        try {
            if (cur != null) {
                while (cur.moveToNext()) {
                    String number = cur.getString(0);
                    if (!TextUtils.isEmpty(number)) {
                        list.add(number.replace(" ", ""));
                    } else {
                        Log.w(TAG,
                                "getNumbersByContactId() invalid number: "
                                        + number);
                    }
                }
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        mCache.put(contactId, list);
        Log.d(TAG, "getNumbersByContactId():" + contactId + ",list:" + list);
        return list;
    }

    /**
     * Return whether burn after read is supported.
     *
     * @param number
     *            The number whose capability is to be queried.
     * @return True if burn after read is supported, else false.
     */
    public boolean isReadBurnSupported(String number) {
        //Log.d(TAG, "isReadBurnSupported(num) entry, with number: " + number);
        if (number == null) {
            Log.w(TAG, "number is null");
            return false;
        }
        number = getAvailableNumber(number);
        ContactInformation info = mContactsCache.get(number);
        if (info != null) {
            //Log.d(TAG, "isReadBurnSupported(num), isReadburn" + info.isReadBurnSupported);
            return info.isReadBurnSupported;
        } else {
            Log.d(TAG, "isReadBurnSupported(num) info is null");
            queryNumberPresence(number);
            return false;
        }
    }

    /**
     * Return whether burn after read is supported.
     *
     * @param contactId
     *            The contactId whose capability is to be queried.
     * @return True if burn after read is supported, else false.
     */
    public boolean isReadBurnSupported(final long contactId) {
        //Log.d(TAG, "isReadBurnSupported(id) entry, with contact id: " + contactId);
        final List<String> numbers = mCache.get(contactId);
        if (numbers != null) {
            for (String number : numbers) {
                boolean isReadBurnSupported = isReadBurnSupported(number);
                if (isReadBurnSupported) {
                    //Log.d(TAG, "isReadBurnSupported(id) exit with true");
                    return true;
                }
            }
            //Log.d(TAG, "isReadBurnSupported(id) exit with false");
            return false;
        } else {
            Log.d(TAG, "isReadBurnSupported(id) numbers is null");
            return false;
        }
    }

    /**
     * This method should only be called from ApiService, for APIs
     * initialization.
     *
     * @param context
     *            The Context of this application.
     * @return true If initialize successfully, otherwise false.
     */
    public static synchronized boolean initialize(Context context) {
        Log.v(TAG, "initialize() entry");
        if (null != sInstance) {
            Log.w(
                    TAG,
                    "initialize() sInstance has existed, " +
                    "is it really the first time you call this method?");
            return true;
        } else {
            if (null != context) {
                PluginApiManager apiManager = new PluginApiManager(context);
                sInstance = apiManager;
                return true;
            } else {
                Log.e(TAG, "initialize() the context is null");
                return false;
            }
        }
    }

    /**
     * Get the context.
     *
     * @return Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get the instance of PluginApiManager.
     *
     * @return The instance of ApiManager, or null if the instance has not been
     *         initialized.
     */
    public static PluginApiManager getInstance() {
        if (null == sInstance) {
            throw new RuntimeException(
                    "Please call initialize() before calling this method");
        }
        return sInstance;
    }

    /**
     * Instantiates a new plugin api manager.
     *
     * @param context the context
     */
    private PluginApiManager(Context context) {
        Log.v(TAG, "PluginApiManager(), context = " + context);
        mContext = context;
        mMyCapabilitiesListener = new MyCapabilitiesListener();
        MyJoynServiceListener mMyJoynServiceListener = new MyJoynServiceListener();
        mCapabilitiesApi = new CapabilityService(context, mMyJoynServiceListener);
        mCapabilitiesApi.connect();
        mRcsEnabled = JoynServiceConfiguration.isServiceActivated(mContext);
        initData();
    }

    /**
     * The listener interface for receiving myJoynService events.
     * The class that is interested in processing a myJoynService
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's addMyJoynServiceListener method. When
     * the myJoynService event occurs, that object's appropriate
     * method is invoked.
     *
     * @see MyJoynServiceEvent
     */
    public class MyJoynServiceListener implements JoynServiceListener {

        /**
         * On service connected.
         */
        @Override
        public void onServiceConnected() {
            try {
                Log.d(TAG, "onServiceConnected");
                PluginApiManager.this.mCapabilitiesApi
                        .addCapabilitiesListener(mMyCapabilitiesListener);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

        }

        /**
         * On service disconnected.
         *
         * @param error the error
         */
        @Override
        public void onServiceDisconnected(int error) {
            try {
                Log.d(TAG, "onServiceDisConnected");
                if (PluginApiManager.this.mCapabilitiesApi != null) {
                    PluginApiManager.this.mCapabilitiesApi
                            .removeCapabilitiesListener(mMyCapabilitiesListener);
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * The listener interface for receiving myCapabilities events.
     * The class that is interested in processing a myCapabilities
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's addMyCapabilitiesListener method. When
     * the myCapabilities event occurs, that object's appropriate
     * method is invoked.
     *
     * @see MyCapabilitiesEvent
     */
    public class MyCapabilitiesListener extends CapabilitiesListener {

        /**
         * On capabilities received.
         *
         * @param contact the contact
         * @param capabilities the capabilities
         */
        @Override
        public void onCapabilitiesReceived(final String contact,
                Capabilities capabilities) {

            Log.d(TAG, "onCapabilitiesReceived(), contact = " + contact);
            if (null != contact && capabilities != null) {
                ContactInformation info = mContactsCache.remove(contact);
                //Log.v(TAG, "after remove from cache");
                if (info == null) {
                    //Log.v(TAG, "cache does not exist, so create a object.");
                    info = new ContactInformation();
                }
                info.isRcsContact = capabilities.isSupportedRcseContact() ? 1 : 0;
                info.isReadBurnSupported = capabilities.isBurnAfterRead();

                String number = contact;
                number = getAvailableNumber(number);
                mContactsCache.put(number, info);
                Log.d(TAG, "onCapabilitiesReceived() setisRcs:" + info.isRcsContact
                    + number + "," + info.isReadBurnSupported);
                for (CapabilitiesChangeListener listener : mCapabilitiesChangeListenerList) {
                    if (listener != null) {
                        Log.d(TAG, "Notify the listener");
                        listener.onCapabilitiesChanged(contact, info);
                    }
                }
            } else {
                Log.d(TAG,
                        "onCapabilitiesReceived() contact or capabilities invalid");
            }
        }
    }

    /**
     * Get the registration status.
     *
     * @return Registration status
    */
    public boolean getRegistrationStatus() {
        return mRcsEnabled;
    }

    /**
     * This constructor is just used for test case.
     */
    public PluginApiManager() {

    }

    /**
     * Clear all the information in the mContactsCache.
     */
    public void cleanContactCache() {
        Log.d(TAG, "cleanContactCache() entry");
        mContactsCache.evictAll();
    }

}
