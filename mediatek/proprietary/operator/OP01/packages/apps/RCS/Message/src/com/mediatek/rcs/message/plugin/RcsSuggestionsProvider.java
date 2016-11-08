package com.mediatek.rcs.message.plugin;

import android.net.Uri;
import com.mediatek.mms.ipmessage.DefaultIpSuggestionsProviderExt;

/**
 * Plugin implements. response SuggestionsProvider.java in MMS host.
 *
 */
public class RcsSuggestionsProvider extends DefaultIpSuggestionsProviderExt {

    @Override
    public Uri query(String selectionArgs) {
        return Uri.parse(String.format("content://mms-sms-rcs/searchSuggest?pattern=%s",
                selectionArgs));
    }
}