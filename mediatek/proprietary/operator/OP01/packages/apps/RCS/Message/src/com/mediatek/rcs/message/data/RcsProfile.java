package com.mediatek.rcs.message.data;


import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.service.IRCSChatService;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

public class RcsProfile {
    final private Uri uri = Uri.parse("content://com.cmcc.ccs.profile");
    public static final String PROFILE_ACTION = "android.intent.action.view.profile";
    String mName;
    String mNumber;
    String mPortrait;
    Bitmap mPortraitBitMap;

    private static RcsProfile sInstance;
    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new RcsProfile();
        }
    }

    private RcsProfile() {
    }

    public void updateNumber(String number) {

    }

    public static RcsProfile getInstance() {
        if (sInstance == null) {
            sInstance = new RcsProfile();
        }
        return sInstance;
    }

    public String getName() {
        return mName;
    }

    public String getNumber() {
        if (TextUtils.isEmpty(mNumber)) {
            mNumber = RCSServiceManager.getInstance().getMyNumber();
        }
        return mNumber;
    }

    public Bitmap getPortrait() {
        return mPortraitBitMap;
    }

    public interface onProfileChangedListener {
        public void onChanged();
    }
}
