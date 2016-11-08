package com.mediatek.rcs.message.aidl;

import java.util.List;

import android.content.Intent;
import android.net.Uri;

interface IMessageSender {
    boolean sendTextMessage(in List<String> numbers, String message);
    boolean sendTextMessageToGroup(long thread_id, String message);
    Uri insertMmsFromIntent(in Intent intent);
    boolean sendMms(int subId, in List<String> numbers, in Uri uri);
    boolean sendFavoriteMms(int subId, in List<String> numbers, in String fileName);
    Uri insertMmsFromPdu(in byte[] data);
    boolean deleteMms(in Uri pdu);
    boolean startActivity(in Intent intent);
}