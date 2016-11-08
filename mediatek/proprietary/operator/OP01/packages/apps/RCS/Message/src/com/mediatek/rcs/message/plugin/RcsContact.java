package com.mediatek.rcs.message.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.mediatek.mms.callback.IContactCallback;
import com.mediatek.mms.ipmessage.DefaultIpContactExt;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * class RcsContact, plugin implements response Contact.
 *
 */
public class RcsContact extends DefaultIpContactExt {
    private final static String TAG = "RcsContact";
    private Context mContext;
    public IContactCallback mCallback;

    @Override
    public void onIpInit(Context context, IContactCallback callback) {
        mContext = context;
        mCallback = callback;
        super.onIpInit(context, callback);
    }


    @Override
    public Drawable onIpGetAvatar(Drawable defaultValue, final long threadId, String number) {
        String chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, threadId);
        if (!TextUtils.isEmpty(chatId)) {
            return RcsMessageUtils.getGroupDrawable(threadId);
        }
        return super.onIpGetAvatar(defaultValue, threadId, number);
    }



    public String getNumber() {
        return mCallback.getContactNumber();
    }

    public String getName() {
        return mCallback.getContactName();
    }

    public boolean isExistInDatabase() {
        return mCallback.isContactExistsInDatabase();
    }

    public BitmapDrawable getAvatar() {
        return mCallback.getAvatar();
    }
}
