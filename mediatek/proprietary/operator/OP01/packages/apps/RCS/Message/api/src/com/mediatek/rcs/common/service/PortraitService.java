package com.mediatek.rcs.common.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceConfiguration;
import com.cmcc.ccs.profile.ProfileListener;
import com.cmcc.ccs.profile.ProfileService;
import com.mediatek.gifdecoder.GifDecoder;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.utils.RCSUtils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;

/**
 * Provider head image and contacts name.
 */
public class PortraitService {
    private static final String TAG = "PortraitService";
    private static PortraitService sInstance;
    private Context mContext;
    private AsyncHandler mAsyncHandler;
    private static Looper sLooper = null;
    private Handler mWorkerThreadHandler;
    private static String mDefaultImage;
    private static String mBlankImage;

    private static ProfileService mProfileService;
    private static boolean mProfileServiceConnected;
    private PortraitJoynServiceListener mPortraitJoynServiceListener;
    private PortraitProfileListener mPortraitProfileListener;
    private static final String RCS_ON = "com.mediatek.intent.rcs.stack.LaunchService";

    public final static int TOKEN_QUERY_ONE = 0;
    public final static int TOKEN_QUERY_ONE_ONLY_CONTACT = 1;
    public final static int TOKEN_QUERY_GROUP = 2;
    public final static int TOKEN_QUERY_GROUP_THUMBNAIL = 3;
    public final static int TOKEN_UPDATE_GROUP_MEMBER = 4;

    private final static int MAX_CACHE_GROUP = 20;
    private final static int MAX_CACHE_PORTRAIT = 200;
    private final static int MIN_SIZE = 64; // Portrait Image Resolution width and height

    private static Portrait mMyPortrait;
    // Key is group chat id String,
    // Value is a HashMap, key is number, value is name
    private final static LruCache<String, LinkedHashMap<String, String>> mGroupCache
        = new LruCache<String, LinkedHashMap<String, String>>(MAX_CACHE_GROUP);
    // key is chatId, value is group thumbnail
    private final static LruCache<String, Thumbnail> mGroupThumbnail
        = new LruCache<String, Thumbnail>(MAX_CACHE_GROUP);
    // Key is mobile number String, Value is Portrait
    private final static LruCache<String, Portrait> mPortraitCache
        = new LruCache<String, Portrait>(MAX_CACHE_PORTRAIT);
    // Key is number, Value is ChatId
    private final static Map<String, String> mQueryProfile =
            new ConcurrentHashMap<String, String>();
    // String is Number, donot query reduntant number
    private final Set<String> mQueryOnlyContacts = new HashSet<String>();
    // No member in Group, means nullgroup
    private static Bitmap mNullGroupThumbnail;

    private final Set<UpdateListener> mListeners = new CopyOnWriteArraySet<UpdateListener>();
    public interface UpdateListener {
        // will pass the result to caller
        public void onPortraitUpdate(Portrait p, String chatId);
        //after query group info done, notify caller the group number set
        public void onGroupUpdate(String chatId, Set<String> numberSet);
        // will pass the result to caller
        public void onGroupThumbnailUpdate(String chatId, Bitmap thumbnail);
    }

    public void addListener(UpdateListener listener) {
        Log.d(TAG, "addListener: " + listener);
        mListeners.add(listener);
    }

    public void removeListener(UpdateListener listener) {
        Log.d(TAG, "removeListener: " + listener);
        mListeners.remove(listener);
    }

    /**
     * This API first directly return data from Cache, maybe old, then will
     * async query the latest data from Contacts, include name and head image;
     * after query done, will callback by
     * UpdateListener.onPortraitUpdate(Portrait, chatId=null) to caller.
     * @param number
     * @return
     */
    public Portrait requestPortrait(String number) {
        Log.d(TAG, "requestPortrait,number=" + number);
        queryContactsAsync(number);
        return getPortrait(null, number);
    }

    /**
     * This API first directly return data from Cache, maybe old, then will
     * async query the latest data from GroupMember Provider and Contacts,
     * include name and head image; after query done, will callback by
     * UpdateListener.onPortraitUpdate(Portrait, chatId) to caller.
     * @param number
     * @return
     */
    public Portrait requestMemberPortrait(String chatId, String number, Boolean queryProfile) {
        Log.d(TAG, "requestMemberPortrait, chatId=" + chatId + ",number=" + number
                + ",queryProfile=" + queryProfile);
        queryMember(chatId, number, queryProfile);
        return getPortrait(chatId, number);
    }

    /**
     * This API only directly return data from Cache, because we assume Caller
     * call updateGroup() before, so the current cache is the latest.
     * @param chatId
     * @param number
     * @return
     */
    public Portrait getMemberPortrait(String chatId, String number) {
        //Log.d(TAG, "getMemberPortrait, chatId=" + chatId + ",number=" + number);
        return getPortrait(chatId, number);
    }

    /**
     * When enter Group Chat or display Group Member Settings, need call this
     * API. This API will read the latest group member info from RCS Group
     * Provider, then query all member from Contacts Provider; after finish,
     * callback to caller by UpdateListener.onGroupUpdate(chatId, numberSet).
     * @param chatId
     * @param queryProfile, useless, will be phase out,
     * @return group member number Set
     */
    public Set<String> updateGroup(String chatId, boolean queryProfile) {

        queryGroup(chatId);

        Set<String> groupSet = new LinkedHashSet<String>();
        if (mGroupCache.get(chatId) != null) {
            groupSet.addAll(mGroupCache.get(chatId).keySet());
        }
        Log.d(TAG, "updateGroup, chatId=" + chatId + ",member count=" + groupSet.size());
        return groupSet;
    }

    /**
     * This API will directly return data from cache, maybe old, then it will async query
     * the newest image, and callback to caller by UpdateListener.onGroupThumbnailUpdate().
     * @param chatId
     * @return Bitmap Group Thumbnail
     */
    public Bitmap requestGroupThumbnail(String chatId) {
        Log.d(TAG, "requestGroupThumbnail, chatId=" + chatId);
        queryGroupThumbnail(chatId);
        Thumbnail thumbnail = mGroupThumbnail.get(chatId);
        if (thumbnail != null) {
            return thumbnail.bitmap;
        }
        return composeGroupThumbnail(chatId);
    }

    /**
     * This API is Blocking, it will directly sync query
     * the newest image, and return group thumbnail.
     * @param chatId
     * @return Bitmap Group Thumbnail
     */
    public Bitmap getGroupThumbnail(String chatId) {
        Log.d(TAG, "getGroupThumbnail, chatId=" + chatId);
        WorkerArgs args = new WorkerArgs();
        args.chatId = chatId;
        args.number = null;
        args.needNotify = true;
        queryRcsGroup(TOKEN_QUERY_GROUP_THUMBNAIL, args);

        Thumbnail thumbnail = mGroupThumbnail.get(chatId);
        if (thumbnail != null && args.needNotify == false) {
            return thumbnail.bitmap;
        }
        return composeGroupThumbnail(chatId);
    }

    /**
     * A sync api, get myself profile from ccs provider.
     * @return Portrait
     */
    public Portrait getMyProfile() {
        Portrait p = new Portrait(null, null, null, Portrait.FROM_DEFAULT);
        Portrait tempP = getMyProfileIntern();

        p.mNumber = tempP.mNumber;
        p.mName = tempP.mName;
        p.mImage = tempP.mImage;
        if (TextUtils.isEmpty(p.mName)) p.mName = p.mNumber;
        if (TextUtils.isEmpty(p.mImage)) p.mImage = mDefaultImage;
        return p;
    }

    public static Bitmap decodeString(String imgStr) {
        byte[] data = Base64.decode(imgStr, Base64.DEFAULT);
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bm;
    }

    // mName only come from Contacts APP
    // mImage come from Contacts, or Group member provider, or Profile
    public class Portrait {
        public String mNumber;
        public String mName;
        public String mImage;

        private int mType;
        private final static int FROM_DEFAULT = 0;
        private final static int FROM_CONTACTS = 1;
        private final static int FROM_PROFILE = 2;
        private final static int FROM_GROUP = 3;

        public Portrait(String number, String name, String image, int type) {
            mNumber = number;
            mName = name;
            mImage = image;
            mType = type;
        }
    }

    private PortraitService(Context cntx, int defaultImageResourceId, int blankImageResourceId) {
        mContext = cntx.getApplicationContext() != null ? cntx.getApplicationContext() : cntx;
        if (mDefaultImage == null) {
            mDefaultImage = genImageStrById(cntx, defaultImageResourceId);
        }
        if (mBlankImage == null) {
            mBlankImage = genImageStrById(cntx, blankImageResourceId);
        }
        mAsyncHandler = new AsyncHandler();
        if (sLooper == null) {
            HandlerThread thread = new HandlerThread("PortraitServiceAsyncHandler");
            thread.start();

            sLooper = thread.getLooper();
        }
        mWorkerThreadHandler = new WorkerHandler(sLooper);
        mPortraitJoynServiceListener = new PortraitJoynServiceListener();
        mPortraitProfileListener = new PortraitProfileListener();
        connectProfileService();
        Uri uri = Uri.parse("content://com.cmcc.ccs.profile");
        mContext.getContentResolver().registerContentObserver(uri, true, mMyProfileObserver);
        IntentFilter filter = new IntentFilter(RCS_ON);
        mContext.registerReceiver(mReceiver, filter, null, null);
    }

    /**
     * Singleton class.
     * @param context Context
     * @param defaultImageResourceId Resource ID
     * @param blankImageResourceId Resource ID
     * @return PortraitService object
     */
    public synchronized static PortraitService getInstance(Context context,
        int defaultImageResourceId, int blankImageResourceId) {
        if (sInstance == null) {
            sInstance = new PortraitService(context, defaultImageResourceId, blankImageResourceId);
        }
        return sInstance;
    }

    private String genImageStrById(Context cntx, int resId) {
        Bitmap bm = BitmapFactory.decodeResource(cntx.getResources(), resId);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);
    }

    private final static ContentObserver mMyProfileObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
            Log.d(TAG, "getMyProfile mMyProfileObserver,onChanged");
            mMyPortrait = null;
        }
    };

    private void connectProfileService() {
        if (mProfileServiceConnected == false
            && JoynServiceConfiguration.isServiceActivated(mContext)) {
            mProfileService = new ProfileService(mContext, mPortraitJoynServiceListener);
            mProfileService.connect();
            mProfileService.addProfileListener(mPortraitProfileListener);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(RCS_ON)) {
                Log.d(TAG, RCS_ON + ",mProfileServiceConnected:" + mProfileServiceConnected);
                connectProfileService();
            }
        }
    };

    private Portrait getPortrait(String chatId, String number) {

        Portrait p = new Portrait(number, null, null, Portrait.FROM_DEFAULT);
        Portrait cacheP = getProtraitInternal(number);
        if (cacheP != null) {
            p.mImage = cacheP.mImage;
            p.mName = cacheP.mName;
            p.mType = cacheP.mType;
        }

        if (chatId != null) {
            if (mGroupCache.get(chatId) != null) {
                String nickNameInGroup = mGroupCache.get(chatId).get(number);
                if (!TextUtils.isEmpty(nickNameInGroup)) {
                    p.mName = nickNameInGroup;
                }
            }
        }
        if (TextUtils.isEmpty(p.mName)) p.mName = number;
        if (TextUtils.isEmpty(p.mNumber)) p.mNumber = number;
        if (chatId == null && p.mType != Portrait.FROM_CONTACTS) {
            p.mImage = null;
        }
        if (p.mImage == null) p.mImage = mDefaultImage;

        Log.d(TAG, "getPortrait,chatId=" + chatId + ",number=" + p.mNumber + ",name=" + p.mName);
        return p;
    }

    private Portrait getProtraitInternal(String number) {
        return mPortraitCache.get(number);
    }

    /**
     * Args between AsyncHandler and WorkerHandler.
     */
    protected static final class WorkerArgs {
        public Handler handler;
        public String chatId;
        public String number;
        public String image;
        public boolean queryProfile;
        public boolean needNotify;
    }

    /**
     * request all aysnc work and wait result to dispatch.
     */
    private class AsyncHandler extends Handler {

        public AsyncHandler() {
            super();
            Log.d(TAG, "AsyncHandler, construction");
        }

        public final void startQuery(int token, String chatId,
                                       String number, boolean queryProfile) {
            Log.d(TAG, "startQuery, token:" + token + ",chatId:" + chatId
                + ",number:" + number + ",queryProfile:" + queryProfile);
            Message msg = mWorkerThreadHandler.obtainMessage(token);
            WorkerArgs args = new WorkerArgs();
            args.handler = this;
            args.chatId = chatId;
            args.number = number;
            args.queryProfile = queryProfile;
            args.needNotify = true;
            msg.obj = args;

            if (token == TOKEN_QUERY_GROUP) {
                mWorkerThreadHandler.removeMessages(TOKEN_QUERY_GROUP);
            }
            mWorkerThreadHandler.sendMessage(msg);
        }

        public final void startUpdate(int token, String chatId, String number, String image) {
            Log.d(TAG, "startUpdate, token:" + token + ",number:" + number);
            Message msg = mWorkerThreadHandler.obtainMessage(token);
            WorkerArgs args = new WorkerArgs();
            args.handler = this;
            args.chatId = chatId;
            args.number = number;
            args.queryProfile = false;
            args.needNotify = true;
            args.image = image;
            msg.obj = args;

            mWorkerThreadHandler.sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            int token = msg.what;
            Log.d(TAG, "AsyncHandler handleMessage, token:" + token + ",chatId:" + args.chatId
                + ",number:" + args.number + ",queryProfile:" + args.queryProfile);

            notifyAllListener(token, args.chatId, args.number);
            if (args.queryProfile
                && !TextUtils.isEmpty(args.chatId) && !TextUtils.isEmpty(args.number)) {
                queryProfile(token, args.chatId, args.number);
            }
        }
    }

    /**
     * handle all query database work.
     */
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
            Log.d(TAG, "WorkerHandler, construction");
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;

            int token = msg.what;
            switch (token) {
                case TOKEN_QUERY_ONE:
                case TOKEN_QUERY_GROUP:
                case TOKEN_QUERY_GROUP_THUMBNAIL:
                    queryRcsGroup(token, args);
                    break;
                case TOKEN_QUERY_ONE_ONLY_CONTACT:
                    queryContacts(token, args);
                    break;
                case TOKEN_UPDATE_GROUP_MEMBER:
                    updateRcsGroupMember(args);
                    break;
                default :
                    break;
            }

            if (args.needNotify == false) {
                return;
            }
            if (token == TOKEN_QUERY_GROUP_THUMBNAIL) {
                composeGroupThumbnail(args.chatId);
            }
            Message reply = args.handler.obtainMessage(token);
            reply.obj = args;
            reply.sendToTarget();

        }
    }

    private void queryRcsGroup(int token, WorkerArgs args) {
        Cursor cursor = null;
        String selection = TextUtils.isEmpty(args.chatId) ?
                null : GroupMemberData.COLUMN_CHAT_ID + "='" + args.chatId + "'";

        if (!TextUtils.isEmpty(args.chatId) && !TextUtils.isEmpty(args.number)) {
            selection = GroupMemberData.COLUMN_CONTACT_NUMBER + "='" + args.number + "'";
        }
        Log.d(TAG, "queryRcsGroup: Selection=" + selection);
        cursor = mContext.getContentResolver().query(
                RCSUtils.RCS_URI_GROUP_MEMBER, RCSUtils.PROJECTION_GROUP_MEMBER,
                selection, null, null);


        String chatId = null;
        String number = null;
        String name = null;
        String image = null;
        // maybe multi chatId, store member number as key, name as value
        Map<String, LinkedHashMap<String, String>> tempMap =
                new HashMap<String, LinkedHashMap<String, String>>();
        LinkedHashMap<String, String> groupMap;
        Set<String> numberSet = new LinkedHashSet<String>();
        boolean memberExistInGroup = false;

        try {
            if (cursor.moveToFirst()) {
                do {
                    chatId = cursor.getString(cursor.getColumnIndex(
                        GroupMemberData.COLUMN_CHAT_ID));
                    name = cursor.getString(cursor.getColumnIndex(
                        GroupMemberData.COLUMN_CONTACT_NAME));
                    number = cursor.getString(cursor.getColumnIndex(
                        GroupMemberData.COLUMN_CONTACT_NUMBER));
                    image = cursor.getString(cursor.getColumnIndex(
                        GroupMemberData.COLUMN_PORTRAIT));

                    if (image != null) {
                        updatePortraitCache(number, null, image, Portrait.FROM_GROUP);
                    }

                    groupMap = tempMap.get(chatId);
                    if (groupMap == null) {
                        groupMap = new LinkedHashMap<String, String>();
                    }
                    groupMap.put(number, name);
                    tempMap.put(chatId, groupMap);
                    numberSet.add(number);

                    if (token == TOKEN_QUERY_ONE && chatId.equals(args.chatId)
                        && number.equals(args.number)) {
                        memberExistInGroup = true;
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        if (token == TOKEN_QUERY_GROUP || token == TOKEN_QUERY_GROUP_THUMBNAIL) {
            for (String key : tempMap.keySet()) {
                mGroupCache.put(key, tempMap.get(key));
            }
            if (chatId == null) {
                Log.d(TAG, "onGroupMemberQueryComplete, nobody existing, clear cache");
                chatId = args.chatId;
                mGroupCache.remove(chatId);
                //mGroupThumbnail.remove(chatId);
            }
            if (token == TOKEN_QUERY_GROUP_THUMBNAIL
                && !needUpdateGroupThumbnail(chatId, numberSet)) {
                args.needNotify = false;
                return;
            }
        } else if (token == TOKEN_QUERY_ONE) { // only query one member
            if (memberExistInGroup == true) {
                if (mGroupCache.get(chatId) != null) {
                    mGroupCache.get(chatId).put(number, name);
                } else {
                    mGroupCache.put(chatId, tempMap.get(chatId));
                }
            } else {
                numberSet.add(args.number);
                chatId = args.chatId;
            }
        }

        Log.i(TAG, "onGroupMemberQueryComplete, will query Contacts, numberSet=" + numberSet);
        if (numberSet.size() > 0) {
            queryContacts(token, args);
        }
    }

    private boolean needUpdateGroupThumbnail(String chatId, Set<String> numberSet) {
        Thumbnail thumbnail = mGroupThumbnail.get(chatId);
        if (thumbnail == null && mGroupCache.get(chatId) == null) {
            return false;
        }
        if (thumbnail != null) {
            if (thumbnail.countInBitmap > numberSet.size()) {
                //return true;
            } else if (!numberSet.containsAll(thumbnail.numbers)
                       || (thumbnail.numbers.size() < MAX_COMPOSE_COUNT &&
                           numberSet.size() > thumbnail.numbers.size())) {
                //return true;
            } else {
                Log.d(TAG, "onGroupMemberQueryComplete, no need update group thunbnail");
                return false;
            }
        }
        Log.d(TAG, "onGroupMemberQueryComplete, need update group thunbnail");
        if (numberSet.size() == 0) {
        //    notifyAllListener(TOKEN_QUERY_GROUP_THUMBNAIL, chatId, null, null);
        }
        return true;
    }

    private void queryContacts(int token, WorkerArgs args) {
        InputStream inputStream = null;
        Cursor cursor = null;
        String name;
        String image;
        String myNumber = RCSMessageManager.getInstance().getMyNumber();
        final Set<String> numberSet = new LinkedHashSet<String>();
        if (!TextUtils.isEmpty(args.number)) {
            numberSet.add(args.number);
        } else {
            numberSet.addAll(mGroupCache.get(args.chatId).keySet());
        }
        ArrayList<String> numbers = new ArrayList<String>();
        for (String number : numberSet) {
            numbers.add(number);
        }
        Log.d(TAG, "queryContacts: " + numbers);
        for (int i = 0; i < numbers.size(); i++) {
            if (myNumber != null && myNumber.equals(numbers.get(i))) {
                args.queryProfile = false;
                updatePortraitCacheForMyself();
                continue;
            }
            try {
                name = null;
                image = null;
                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(numbers.get(i)));
                cursor = mContext.getContentResolver().query(uri,
                    new String[]{PhoneLookup._ID, PhoneLookup.DISPLAY_NAME},
                    null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                    long contactId = cursor.getLong(cursor.getColumnIndex(PhoneLookup._ID));
                    Uri displayPhotoUri = ContentUris.withAppendedId(
                        Contacts.CONTENT_URI, contactId);

                    try {
                        inputStream = Contacts.openContactPhotoInputStream(
                                mContext.getContentResolver(), displayPhotoUri);
                        if (inputStream != null) {
                            try {
                                byte data[] = new byte[inputStream.available()];
                                inputStream.read(data, 0, data.length);
                                image = Base64.encodeToString(data, Base64.DEFAULT);
                                if (image != null) {
                                    args.queryProfile = false;
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "parse contact image error");
                            }
                        }
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to close input stream.", e);
                            }
                        }
                    }
                }
                updatePortraitCache(numbers.get(i), name, image, Portrait.FROM_CONTACTS);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void updateRcsGroupMember(WorkerArgs args) {
        Log.d(TAG, "updateRcsGroupMember: number=" + args.number);

        ContentValues cv = new ContentValues();
        cv.put(GroupMemberData.COLUMN_PORTRAIT, args.image);
        String where = GroupMemberData.COLUMN_CONTACT_NUMBER + "='" + args.number + "'";
        mContext.getContentResolver().update(RCSUtils.RCS_URI_GROUP_MEMBER, cv, where, null);
    }

    private void notifyAllListener(int token, String chatId, String number) {
        for (UpdateListener l : mListeners) {
            Log.d(TAG, "notifyAllListener, token=" + token + ",updating listener=" + l);
            if (token == TOKEN_QUERY_GROUP) {
                Set<String> s = new LinkedHashSet<String>();
                if (mGroupCache.get(chatId) != null) {
                    s.addAll(mGroupCache.get(chatId).keySet());
                }
                Log.d(TAG, "updateGroup, chatId=" + chatId + ",member count=" + s.size()
                        + ",notify");
                l.onGroupUpdate(chatId, s);
            } else if (token == TOKEN_QUERY_ONE || token == TOKEN_UPDATE_GROUP_MEMBER) {
                Portrait p = getPortrait(chatId, number);
                l.onPortraitUpdate(p, chatId);
            } else if (token == TOKEN_QUERY_ONE_ONLY_CONTACT) { //contact - default
                if (mQueryOnlyContacts.contains(number)) {
                    mQueryOnlyContacts.remove(number);
                }
                Portrait p = getPortrait(null, number);
                l.onPortraitUpdate(p, chatId);
            } else if (token == TOKEN_QUERY_GROUP_THUMBNAIL) {
                Thumbnail thumbnail = mGroupThumbnail.get(chatId);
                if (thumbnail != null) {
                    l.onGroupThumbnailUpdate(chatId, thumbnail.bitmap);
                }
            }
        }
    }

    private void updatePortraitCache(String number, String name, String image, int type) {
        Portrait p = getProtraitInternal(number);
        if (p != null) {
            if (type == Portrait.FROM_CONTACTS) p.mName = name; // only store name form Contact

            if (image != null) {
                p.mImage = image;
                p.mType = type;
            }

            if ((type == Portrait.FROM_CONTACTS && p.mType == Portrait.FROM_CONTACTS) ||
                (type == Portrait.FROM_CONTACTS && p.mType != Portrait.FROM_CONTACTS
                && image != null) ||
                (type != Portrait.FROM_CONTACTS && p.mType != Portrait.FROM_CONTACTS) ||
                (type != Portrait.FROM_CONTACTS && p.mType == Portrait.FROM_CONTACTS
                && p.mImage == null)) {
                p.mImage = image;
                p.mType = type;
            }

        } else {
            if (name != null || image != null) {
                p = new Portrait(number, name, image, type);
                mPortraitCache.put(number, p);
            }
        }
    }

    private void updatePortraitCacheForMyself() {
        Portrait myPortrait = getMyProfileIntern();
        mPortraitCache.put(myPortrait.mNumber, myPortrait);
    }

    private final static int MAX_COMPOSE_COUNT = 3;
    private class Thumbnail {
        public Bitmap bitmap;
        public Set<String> numbers = new HashSet<String>();
        public int countInBitmap;
        public Thumbnail(Bitmap b, Set<String> n, int count) {
            bitmap = b;
            numbers.addAll(n);
            countInBitmap = count;
        }
    }

    private Bitmap composeGroupThumbnail(String chatId) {
        Set<String> numberSet = new LinkedHashSet<String>();
        Set<String> numberSelectedSet = new LinkedHashSet<String>();
        List<String> imageList = new ArrayList<String>();
        Bitmap image = null;
        Portrait p;
        if (mGroupCache.get(chatId) != null) {
            numberSet.addAll(mGroupCache.get(chatId).keySet());
            for (String number : numberSet) {
                if ((p = getProtraitInternal(number)) != null) {
                    if (p.mImage != null) {
                        imageList.add(p.mImage);
                        numberSelectedSet.add(number);
                    }
                }
                if (imageList.size() == MAX_COMPOSE_COUNT) {
                    break;
                }
            }
        } else if (mNullGroupThumbnail != null) {
            // no member in this group, and we have null group thumbnail, so directly reuse it
            return mNullGroupThumbnail;
        }

        if(image == null) {
            fillGroupThumbnail(imageList, numberSet);
            image = drawGroupThumbnail(imageList);
        }
        if (image != null) {
            int count = numberSet.size() > MAX_COMPOSE_COUNT ? MAX_COMPOSE_COUNT : numberSet.size();
            Log.d(TAG, "composeGroupThumbnail, chatId=" + chatId
                    + ",numberSelectedSet=" + numberSelectedSet + ",count=" + count);
            Thumbnail groupThumbnail = new Thumbnail(image, numberSelectedSet, count);
            mGroupThumbnail.put(chatId, groupThumbnail);

            if (mNullGroupThumbnail == null && numberSelectedSet.size() == 0 && count == 0) {
                mNullGroupThumbnail = image; // store nullgroup thumbnail, incase redraw everytime
            }
        }
        return image;
    }

    private void fillGroupThumbnail(List<String> imageList, Set<String> numberSet) {
        int imageCount = imageList.size();
        int numberCount = numberSet.size();

        if (imageCount == MAX_COMPOSE_COUNT)
            return;
        if (numberCount >= MAX_COMPOSE_COUNT) {
            for (int i = imageCount; i < MAX_COMPOSE_COUNT; i++) {
                imageList.add(mDefaultImage);
            }
        } else {
            for (int i = imageCount; i < numberCount; i++) {
                imageList.add(mDefaultImage);
            }
            for (int i = numberCount; i < MAX_COMPOSE_COUNT; i++) {
                imageList.add(mBlankImage);
            }
        }
    }

    private Bitmap drawGroupThumbnail(List<String> imageList) {
        if (imageList.size() != MAX_COMPOSE_COUNT) {
            Log.i(TAG, "drawGroupThumbnail size is wrong");
            return null;
        }

        int gap = 2;
        Bitmap Frist = decodeString(imageList.get(0));
        Bitmap Secon = decodeString(imageList.get(1));
        Bitmap Thrid = decodeString(imageList.get(2));
        Bitmap FristTemp, FristFinal, SeconFinal, ThridFinal;
        //if (Frist.getWidth() > MIN_SIZE || Frist.getHeight() > MIN_SIZE)
        FristTemp = Bitmap.createScaledBitmap(Frist, MIN_SIZE, MIN_SIZE, true);
        int block = FristTemp.getWidth() / 16;
        FristFinal = Bitmap.createBitmap(FristTemp, block * 4, 0, block * 9, FristTemp.getHeight());
        SeconFinal = Bitmap.createScaledBitmap(Secon, block * 7, FristFinal.getHeight() / 2, true);
        ThridFinal = Bitmap.createScaledBitmap(Thrid, block * 7, FristFinal.getHeight() / 2, true);
        Bitmap thumbnail = Bitmap.createBitmap(FristFinal.getWidth() + gap + SeconFinal.getWidth(),
                                       FristFinal.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbnail);
        canvas.drawBitmap(FristFinal, 0, 0, null);
        canvas.drawBitmap(SeconFinal, FristFinal.getWidth() + gap, 0, null);
        canvas.drawBitmap(ThridFinal,
            FristFinal.getWidth() + gap, SeconFinal.getHeight() + gap, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        bitmapRecycle(Frist);
        bitmapRecycle(Secon);
        bitmapRecycle(Thrid);
        bitmapRecycle(FristTemp);
        bitmapRecycle(FristFinal);
        bitmapRecycle(SeconFinal);
        bitmapRecycle(ThridFinal);
        return thumbnail;
    }

    private void bitmapRecycle(Bitmap b) {
        if (b != null && !b.isRecycled()) {
            b.recycle();
        }
    }

    private void queryGroup(String chatId) {
        mAsyncHandler.startQuery(TOKEN_QUERY_GROUP, chatId, null, false);
    }

    private void queryMember(String chatId, String number, boolean queryProfile) {
        mAsyncHandler.startQuery(TOKEN_QUERY_ONE, chatId, number, queryProfile);
    }

    private void queryGroupThumbnail(String chatId) {
        mAsyncHandler.startQuery(TOKEN_QUERY_GROUP_THUMBNAIL, chatId, null, false);
    }

    private void queryContactsAsync(String number) {
        if (!(mQueryOnlyContacts.contains(number))) {
            mQueryOnlyContacts.add(number);
            mAsyncHandler.startQuery(TOKEN_QUERY_ONE_ONLY_CONTACT, null, number, false);
        }
    }

    private Portrait getMyProfileIntern() {

        if (mMyPortrait != null) {
            return mMyPortrait;
        }

        String[] projections = {ProfileService.PHONE_NUMBER,
                ProfileService.FIRST_NAME,
                ProfileService.LAST_NAME,
                ProfileService.PORTRAIT,
                ProfileService.PORTRAIT_TYPE};
        Uri uri = Uri.parse("content://com.cmcc.ccs.profile");
        Cursor c = mContext.getContentResolver().query(uri, projections, null, null, null);
        String number = null, name = null;
        String image = null, imageType = null;
        int count = -1;
        try {
            if (c != null && c.moveToFirst()) {
                number = c.getString(c.getColumnIndex(ProfileService.PHONE_NUMBER));
                name = c.getString(c.getColumnIndex(ProfileService.FIRST_NAME));
                if (TextUtils.isEmpty(name)) {
                    name = c.getString(c.getColumnIndex(ProfileService.LAST_NAME));
                }
                image = c.getString(c.getColumnIndex(ProfileService.PORTRAIT));
                imageType = c.getString(c.getColumnIndex(ProfileService.PORTRAIT_TYPE));

                Log.i(TAG, "getMyProfile, number=" + number + ",name=" + name
                      + ",imageType=" + imageType);
                image = decodeStringFromProfile(image, imageType);
            }
        } finally {
            if (c != null) {
                count = c.getCount();
                c.close();
            }
        }

        Portrait p;
        String myNumber = RCSMessageManager.getInstance().getMyNumber();
        if (count >= 1) {
            p  = new Portrait(myNumber, name, image, Portrait.FROM_CONTACTS);
            mMyPortrait = p;
        } else {
            p = new Portrait(myNumber, null, null, Portrait.FROM_CONTACTS);
        }
        return p;
    }

    private void queryProfile(int token, String chatId, String number) {
        Log.i(TAG, "queryProfile, number=" + number);
        if (mProfileServiceConnected == false) {
            Log.i(TAG, "queryProfile, but ProfileService hasnot connected yet, number=" + number);
            return;
        }
        if (!mQueryProfile.containsKey(number)) {
            mProfileService.getContactPortrait(number);
        }
        mQueryProfile.put(number, chatId);
    }

    private class PortraitJoynServiceListener implements JoynServiceListener {

        @Override
        public void onServiceConnected() {
            Log.i(TAG, "PortraitJoynServiceListener, onServiceConnected");
            mProfileServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(int error) {
            Log.i(TAG, "PortraitJoynServiceListener, onServiceDisconnected, error=" + error);
            mProfileServiceConnected = false;
        }
    }

    private class PortraitProfileListener extends ProfileListener {

        @Override
        public void onUpdateProfile(int result) {
            // Do nothing
        }

        @Override
        public void onUpdateProfilePortrait(int result) {
            // Do nothing
        }

        @Override
        public void onGetProfile(int result) {
         // Do nothing
        }

        @Override
        public void onGetProfilePortrait (int result) {
            //Do nothind
        }

        /*for internal use*/
        public void onGetContactPortrait (int result, String portriat, String number,
                String mimeType){

            String chatId = mQueryProfile.remove(number);
            Log.i(TAG, "queryProfile onGetContactPortrait, result=" + result + ",chatId=" + chatId
                    + ",number=" + number + ",mime=" + mimeType
                    + ",image null:" + TextUtils.isEmpty(portriat));
            if (result != ProfileService.OK || chatId == null) return;

            String image = decodeStringFromProfile(portriat, mimeType);

            updatePortraitCache(number, null, image, Portrait.FROM_PROFILE);
            /*
            if (chatId != null) {
                //notifyAllListener(TOKEN_QUERY_ONE, chatId, number);
                WorkerArgs args = new WorkerArgs();
                args.chatId = chatId;
                args.number = number;
                args.queryProfile = false;
                args.needNotify = true;

                Message reply = mAsyncHandler.obtainMessage(TOKEN_QUERY_ONE);
                reply.obj = args;
                reply.sendToTarget();
            }
            */
            //update groupmember provider
            mAsyncHandler.startUpdate(TOKEN_UPDATE_GROUP_MEMBER, chatId, number, image);

        }
    }

    private String decodeStringFromProfile(String portriat, String type) {
        String image = portriat;
        if (type != null && type.equals("GIF")) {
            byte[] data = Base64.decode(portriat, Base64.DEFAULT);
            GifDecoder gifDecoder = new GifDecoder(data, 0, data.length);
            Bitmap firstFrame = gifDecoder.getFrameBitmap(0);
            image = compressBitmap(firstFrame);
            bitmapRecycle(firstFrame);
        } else if (image != null) {
            Bitmap bitmap = decodeString(image);
            Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, MIN_SIZE, MIN_SIZE, true);
            image = compressBitmap(bitmapScaled);
            bitmapRecycle(bitmap);
            bitmapRecycle(bitmapScaled);
        }

        return image;
    }

    private String compressBitmap(Bitmap bitmap) {
        final int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            Log.w(TAG, "Unable to serialize photo: " + e.toString());
            return null;
        }
    }

}
