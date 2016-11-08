package com.mediatek.rcs.common.service;
import com.mediatek.rcs.common.service.IMsgRestoreListener;
import android.net.Uri;


interface IRcsMessageRestoreService {
    boolean restoreSms(String vmsgPath);
    boolean restoreMms(String pduPath);
    void setListener(IMsgRestoreListener excuteListener);
    void setCancel(in boolean isCancel);
    Uri insertPdu(in String pduPath);
    int delMsg(in Uri uri);
}