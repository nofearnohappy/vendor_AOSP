package com.mediatek.rcs.message.plugin;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.widget.TextView;

import com.mediatek.mms.ipmessage.DefaultIpSearchActivityExt;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;

/**
 * Plugin implements. response SearchActivity.java in MMS host.
 *
 */
public class RcsSearchActivity extends DefaultIpSearchActivityExt {

    @Override
    public boolean onIpBindView(TextView textTitle, Cursor cursor) {

        int chatIdPos = cursor.getColumnIndex("chat_id");
        String chatId = cursor.getString(chatIdPos);
        ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(chatId);
        if (info != null && info.getSubject() != null) {
            textTitle.setText(info.getSubject());
            return true;
        }
        return false;
    }

    @Override
    public Uri startQuery(String searchString) {
        Uri uri = Uri.parse("content://mms-sms-rcs/search");
        Uri.Builder builder = uri.buildUpon();
        return builder.appendQueryParameter("pattern", searchString).build();
    }
}
