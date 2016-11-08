package com.mediatek.rcs.message.group;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.mediatek.rcs.common.binder.RCSServiceManager.INotifyListener;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;

import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.R.drawable;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.QuickContact;
import android.service.dreams.DreamService;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

public class PortraitManager implements PortraitService.UpdateListener, INotifyListener {
    private static final String TAG = "PortraitManager";
    private static PortraitManager sPManager;
    private Context mContext;
    private PortraitService mPService;

    private HashMap<String, MemberInfo> mSinglePortraitMap =
                        new HashMap<String, PortraitManager.MemberInfo>();

    private HashMap<String, HashMap<String, MemberInfo>> mGroupPortraitMap =
                        new HashMap<String, HashMap<String,MemberInfo>>();
    private MemberInfo mProfile;
    private GroupThumbnailCache mGroupThumbnailCache = new GroupThumbnailCache(20);

    private PortraitManager(Context context) {
        mContext = context;
        mPService = PortraitService.getInstance(mContext, R.drawable.ic_default_contact,
                R.drawable.contact_blank_avatar);
        mPService.addListener(this);
        RCSServiceManager.getInstance().registNotifyListener(this);
    }

    /**
     * Init PortraitManager instance.
     * @param context
     */
    public static void init(Context context) {
        if (sPManager == null) {
            sPManager = new PortraitManager(context);
        }
    }

    /**
     * DeInit instance.
     */
    public static void unInit() {
        if (sPManager != null) {
            sPManager.destroy();
            sPManager = null;
        }
    }

    /**
     * Get PortraitManager instance.
     * @return
     */
    public static PortraitManager getInstance() {
        if (sPManager == null) {
            throw new RuntimeException("PortraitManager is not initiated");
        }
        return sPManager;
    }

    public synchronized void destroy() {
        if (mPService != null) {
            mPService.removeListener(this);
        }
    }

    public PortraitService getPortraitService() {
        return mPService;
    }

    public synchronized void invalidateGroupPortrait(String chatId) {
        HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.remove(chatId);
        if (infoMap != null) {
            Collection<MemberInfo>  infos = infoMap.values();
            for (MemberInfo info : infos) {
                info.mInvalided = true;
            }
        }
    }

    public synchronized void invalidatePortrait(String number) {
        number = number.replace("+86", "");
        MemberInfo info = mSinglePortraitMap.remove(number);
        if (info != null) {
            info.clearListeners();
        }
    }

    public synchronized void initOne2OneChatPortrait(String number) {
        MemberInfo info = mSinglePortraitMap.get(number);
        if (info == null) {
            info = new MemberInfo(mContext, number, number,
                    mContext.getResources().getDrawable(R.drawable.ic_contact_picture));
        }
    }

    /**
     * Get member info by number.
     * @param number contact number
     * @return MemberInfo
     */
    public synchronized MemberInfo getMemberInfo(String number) {
        Log.d(TAG, "[getMemberInfo]: number = " + number);
        if (TextUtils.isEmpty(number)) {
            return new MemberInfo(mContext, "", "",
                    mContext.getResources().getDrawable(R.drawable.ic_contact_picture));
        }
        number = number.replace("+86", "");
        MemberInfo info = mSinglePortraitMap.get(number);
        if (info == null) {
            Portrait p = mPService.requestPortrait(number);
            if (p != null) {
                info = new MemberInfo(mContext, number, p.mName, PortraitService.decodeString(p.mImage));
            } else {
                info = new MemberInfo(mContext, number, number,
                        mContext.getResources().getDrawable(R.drawable.ic_contact_picture));
            }
            mSinglePortraitMap.put(number, info);
        }
        return info;
    }

    public synchronized MemberInfo getMemberInfo(String chatId, String number) {
        HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.get(chatId);
        if (infoMap == null) {
            infoMap = new HashMap<String, PortraitManager.MemberInfo>();
            mGroupPortraitMap.put(chatId, infoMap);
        }

        MemberInfo info = infoMap.get(number);
        if (info == null) {
            Portrait p = mPService.requestMemberPortrait(chatId, number, false);
            info = new MemberInfo(mContext, number, p.mName, PortraitService.decodeString(p.mImage));
            infoMap.put(number, info);
        } else if (info.mInvalided) {
            //only to query, wait for update callback to update info
            mPService.requestMemberPortrait(chatId, number, false);
            info.mInvalided = false;
        }
        if (info.mNeedQueryProfile && !info.mProfileQueried) {
            Log.d(TAG, "need requery profile for the lasted portrait: " + number);
          //only to query, wait for update callback to update info
            Portrait p = mPService.requestMemberPortrait(chatId, number, true);
            info.mProfileQueried = true;
        }
        return info;
    }

    public synchronized void updateMemberRequeryState(String chatId, String number, boolean needQuery) {
        HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.get(chatId);
        if (infoMap != null) {
            MemberInfo info = infoMap.get(number);
            if (info == null) {
                Portrait p = mPService.requestMemberPortrait(chatId, number, false);
                info = new MemberInfo(mContext, number, p.mName, PortraitService.decodeString(p.mImage));
                infoMap.put(number, info);
            }
            info.mNeedQueryProfile = needQuery;
        } else {
            infoMap = new HashMap<String, PortraitManager.MemberInfo>();
            MemberInfo info = new MemberInfo(mContext, number, number, (Drawable)null);
            info.mNeedQueryProfile = true;
            infoMap.put(number, info);
            mGroupPortraitMap.put(chatId, infoMap);
        }
    }

    public synchronized void initGroupChatPortrait(String chatId) {
        Log.d(TAG, "initGroupChatPortrait: " + chatId);
        HashMap<String, MemberInfo> infos = mGroupPortraitMap.get(chatId);
        if (infos == null) {
            infos = new HashMap<String, PortraitManager.MemberInfo>();
            mGroupPortraitMap.put(chatId, infos);
        }
        //TODO it will be changed in future
        mPService.updateGroup(chatId, false);
    }

    public synchronized void destroyGroupChatPortrait(String chatId) {
        HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.remove(chatId);
        Log.d(TAG, "destroyGroupChatPortrait: chatId = " + chatId);
        if (infoMap != null) {
            Collection<MemberInfo>  infos = infoMap.values();
            for (MemberInfo info : infos) {
                info.clearListeners();
            }
        }
    }

    @Override
    public synchronized void onPortraitUpdate(Portrait p, String chatId) {
        // TODO Auto-generated method stub
        if (chatId == null) {
            MemberInfo info = mSinglePortraitMap.get(p.mNumber);
            if (info != null) {
                info.update(mContext, p.mName, PortraitService.decodeString(p.mImage));
            }
        } else {
            HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.get(chatId);
            if (infoMap != null) {
                MemberInfo info = infoMap.get(p.mNumber);
                if (info != null) {
                    info.update(mContext, p.mName, PortraitService.decodeString(p.mImage));
                }
            }
        }
    }

    @Override
    public synchronized void onGroupUpdate(String chatId, Set<String> numberSet) {
        HashMap<String, MemberInfo> infoMap = mGroupPortraitMap.get(chatId);
        if (infoMap == null) {
            Log.w(TAG, "onGroupUpdate. infoMap is null. chatId = " + chatId);
            return;
        }
        for (String number : numberSet) {
            MemberInfo info = infoMap.get(number);
            Portrait p = mPService.getMemberPortrait(chatId, number);
            if (info == null) {
                info = new MemberInfo(mContext, number, p.mName, PortraitService.decodeString(p.mImage));
                infoMap.put(number, info);
            } else {
                info.update(mContext, p.mName, PortraitService.decodeString(p.mImage));
            }
        }
    }

    public MemberInfo getMyInfo() {
        if (mProfile == null) {
            mProfile = new MemberInfo(mContext, "", "", (Drawable)null);
        }
        return mProfile;
    }

    public MemberInfo getMyInfo(int simId) {
        Portrait p = mPService.getMyProfile();
        return new MemberInfo(mContext, p.mNumber, p.mName, PortraitService.decodeString(p.mImage));
    }

    @Override
    public synchronized void onGroupThumbnailUpdate(String chatId, Bitmap thumbnail) {
        GroupThumbnail groupThumbnail = mGroupThumbnailCache.get(chatId);
        if (groupThumbnail != null) {
            groupThumbnail.update(thumbnail);
        }
    }


    public class MemberInfo {
        public Drawable mDrawable;
        public String mName;
        public String mNumber;
        boolean mNeedQueryProfile;
        boolean mProfileQueried;
        boolean mInvalided;
        private HashSet<onMemberInfoChangedListener> mListners =
                                new HashSet<PortraitManager.onMemberInfoChangedListener>();
        public boolean addChangedListener(onMemberInfoChangedListener l) {
            return mListners.add(l);
        }

        public boolean removeChangedListener(onMemberInfoChangedListener l) {
            return mListners.remove(l);
        }

        void clearListeners() {
            mListners.clear();
        }

        public MemberInfo(Context context, String number, String name, Bitmap bitmap) {
            mNumber = number;
            mName = name;
            if (bitmap != null) {
                mDrawable = new BitmapDrawable(context.getResources(), bitmap);
            } else {
                mDrawable = context.getResources().getDrawable(R.drawable.ic_contact_picture);
            }
            mNeedQueryProfile = false;
        }

        public MemberInfo(Context context, String number, String name, Drawable drawable) {
            mNumber = number;
            mName = name;
            if (drawable == null) {
                mDrawable = context.getResources().getDrawable(R.drawable.ic_contact_picture);
            } else {
                mDrawable = drawable;
            }
            mNeedQueryProfile = false;
        }

        public void update(Context context, String name, Bitmap bitmap) {
            update(name, new BitmapDrawable(context.getResources(), bitmap));
        }

        public void update(String name, Drawable drawable) {
            mName = name;
            mDrawable = drawable;
            mNeedQueryProfile = false;
            for (onMemberInfoChangedListener l : mListners) {
                l.onChanged(this);
            }
        }

    }

    public interface onMemberInfoChangedListener {
        public void onChanged(MemberInfo info);
    }

    public interface onGroupPortraitChangedListener {
        public void onChanged(Bitmap newBitmap);
    }

    public class GroupThumbnail {
        public String mChatId;
        public Bitmap mBitmap;
        boolean mNeedQuery;
        private HashSet<onGroupPortraitChangedListener> mListeners =
                new HashSet<PortraitManager.onGroupPortraitChangedListener>();
        public GroupThumbnail(String chatid, Bitmap bitmap) {
            mChatId = chatid;
            mBitmap = bitmap;
        }
        public void update(Bitmap bitmap) {
            mBitmap = bitmap;
            for (onGroupPortraitChangedListener l : mListeners) {
                l.onChanged(mBitmap);
            }
        }

        public boolean addChangedListener(onGroupPortraitChangedListener l) {
            return mListeners.add(l);
        }

        public boolean removeChangedListener(onGroupPortraitChangedListener l) {
            boolean ret= mListeners.remove(l);
            return ret;
        }

        public void evicted() {
            mListeners.clear();
        }
    }

    private HashMap<String, GroupThumbnail> mGroupThumbnails = new HashMap<String, GroupThumbnail>();
    public GroupThumbnail getGroupPortrait2(String chatId) {
        Bitmap bitmap = mPService.requestGroupThumbnail(chatId);
        GroupThumbnail groupThumbnail = mGroupThumbnails.get(chatId);
        if (groupThumbnail == null) {
            groupThumbnail = new GroupThumbnail(chatId, bitmap);
            mGroupThumbnails.put(chatId, groupThumbnail);
        } else {
            groupThumbnail.update(bitmap);
        }
        return groupThumbnail;
    }

    public GroupThumbnail getGroupPortrait(String chatId) {
        GroupThumbnail groupThumbnail = mGroupThumbnailCache.get(chatId);
        if (groupThumbnail == null) {
            Bitmap bitmap = mPService.requestGroupThumbnail(chatId);
            groupThumbnail = new GroupThumbnail(chatId, bitmap);
            mGroupThumbnailCache.put(chatId, groupThumbnail);
        }
        return groupThumbnail;
    }

    public static BitmapDrawable getGroupBitmapDrawable(Context context, String chatId) {
        if (TextUtils.isEmpty(chatId)) {
            Log.e(TAG, "[getGroupBitmapDrawable]: chatId is null");
        }
        Bitmap bitmap = PortraitManager.getInstance().mPService.getGroupThumbnail(chatId);
        if (bitmap != null) {
            return new BitmapDrawable(context.getResources(), bitmap);
        }
        Log.e(TAG, "getGroupBitmapDrawable: is null");
        return null;
    }

    public void clearAllGroupThumbnails() {
        mGroupThumbnailCache.evictAll();
    }

    public void updateGroupThumbnailbyChatId(String chatId) {
        GroupThumbnail groupThumbnail = mGroupThumbnailCache.get(chatId);
        if (groupThumbnail != null) {
            Bitmap bitmap = mPService.requestGroupThumbnail(chatId);
            groupThumbnail.update(bitmap);
        }
    }


    /**
     * GroupThumbnailCache.
     *
     */
    private class GroupThumbnailCache extends LruCache<String, GroupThumbnail> {
        public GroupThumbnailCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key,
                GroupThumbnail oldValue, GroupThumbnail newValue) {
            oldValue.evicted();
        }
    }

    @Override
    public void notificationsReceived(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "[notificationsReceived]action = " + action);
        int actionType = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0);
//        long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
        String chatId = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID);
        String stringArg = null;
        String body = null;
        switch (actionType) {
            case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_JOIN:
            case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_LEFT:
            case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_REMOVED:
            case IpMessageConsts.GroupActionList.VALUE_ME_REMOVED:
            case IpMessageConsts.GroupActionList.VALUE_GROUP_ABORTED:
                updateGroupThumbnailbyChatId(chatId);
            default:
                break;
        }
    }
}
