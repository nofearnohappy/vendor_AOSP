package com.android.email.policy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mediatek.vendoremail.utils.ProviderInfo;
import com.mediatek.vendoremail.utils.ProviderUiInfo;
import com.mediatek.vendoremail.utils.ProviderListInfoUtils;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * M: Email policy class for vendor, must implement getPolicy(String, Bundle)
 *
 */
public class EmailPolicy {

    private static final String TAG = "EmailPolicy";
    // call keys and i/o bundle keys
    // when there is only one parameter or return value, use call key
    private static final String USE_ALTERNATE_EXCHANGE_STRINGS = "useAlternateExchangeStrings";
    private static final String GET_IMAP_ID = "getImapId";
    private static final String GET_IMAP_ID_USER = "getImapId.user";
    private static final String GET_IMAP_ID_HOST = "getImapId.host";
    private static final String GET_IMAP_ID_CAPA = "getImapId.capabilities";
    private static final String FIND_PROVIDER = "findProvider";
    private static final String FIND_PROVIDER_IN_URI = "findProvider.inUri";
    private static final String FIND_PROVIDER_IN_USER = "findProvider.inUser";
    private static final String FIND_PROVIDER_OUT_URI = "findProvider.outUri";
    private static final String FIND_PROVIDER_OUT_USER = "findProvider.outUser";
    private static final String FIND_PROVIDER_NOTE = "findProvider.note";
    /// M: For find provider with specific protocol
    private static final String FIND_PROVIDER_BY_PROTOCOL = "findProviderByProtocol";
    /// M: For find ESP ui information
    private static final String FIND_ESP_UI_INFO = "getESPUiInfo";
    private static final String FIND_ESP_UI_INFO_COUNT = "getESPUiInfo.count"; // indicate number of ESPs
    private static final String FIND_ESP_UI_INFO_NAMES = "getESPUiInfo.names";
    private static final String FIND_ESP_UI_INFO_DOMAINS = "getESPUiInfo.domains";
    private static final String FIND_ESP_UI_INFO_ICON_IDS = "getESPUiInfo.icon.ids";
    private static final String FIND_ESP_UI_INFO_HINTS = "getESPUiInfo.hints";

    public static Bundle getPolicy(Context c, String policy, Bundle arguments) {
        Log.d(TAG, "getPolicy: " + policy);
        Bundle result = new Bundle();
        if (c == null) {
            Log.d(TAG, "Context null");
            return result;
        }
        // For Provider information policies
        if (FIND_PROVIDER.equals(policy)) {
            String domain = arguments.getString(FIND_PROVIDER);
            String protocol = arguments.getString(FIND_PROVIDER_BY_PROTOCOL);
            Log.d(TAG, "Find provider information for domain: " + domain + ", protocol: " + protocol);
            if (!TextUtils.isEmpty(domain)) {
                ProviderInfo pi = ProviderListInfoUtils.findProviderForDomainProtocol(c, domain, protocol);
                if (pi != null) {
                    result.putString(FIND_PROVIDER_IN_URI, pi.incomingUriTemplate);
                    result.putString(FIND_PROVIDER_IN_USER, pi.incomingUsernameTemplate);
                    result.putString(FIND_PROVIDER_OUT_URI, pi.outgoingUriTemplate);
                    result.putString(FIND_PROVIDER_OUT_USER, pi.outgoingUsernameTemplate);
                    result.putString(FIND_PROVIDER_NOTE, pi.note);
                }
            }
            return result;
        }
        // For Provider UI information policies
        if (FIND_ESP_UI_INFO.equals(policy)) {
            LinkedHashMap<String, ProviderUiInfo> providerUiInfos = ProviderListInfoUtils.getProviderUiInfoMap(c);
            int proUiCount = providerUiInfos.size();
            if (proUiCount > 0) {
                result.putInt(FIND_ESP_UI_INFO_COUNT, proUiCount);
                String[] ESPNames = new String[proUiCount];
                String[] ESPDomains = new String[proUiCount];
                int[] ESPIconIds = new int[proUiCount];
                String[] ESPHints = new String[proUiCount];
                int i = 0;
                Iterator<Entry<String, ProviderUiInfo>> it = providerUiInfos.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ProviderUiInfo> ent = (Map.Entry<String, ProviderUiInfo>) it.next();
                    ProviderUiInfo pui = ent.getValue();
                    ESPNames[i] = pui.name;
                    ESPDomains[i] = pui.domain;
                    ESPIconIds[i] = pui.iconResId;
                    ESPHints[i] = pui.hint;
                    ++i;
                }
                result.putStringArray(FIND_ESP_UI_INFO_NAMES, ESPNames);
                result.putStringArray(FIND_ESP_UI_INFO_DOMAINS, ESPDomains);
                result.putIntArray(FIND_ESP_UI_INFO_ICON_IDS, ESPIconIds);
                result.putStringArray(FIND_ESP_UI_INFO_HINTS, ESPHints);
            }
            return result;
        }

        // Other policies
        if (GET_IMAP_ID.equals(policy)) {
            return result;
        }
        if (USE_ALTERNATE_EXCHANGE_STRINGS.equals(policy)) {
            return result;
        } else {
            return result;
        }
    }
}
