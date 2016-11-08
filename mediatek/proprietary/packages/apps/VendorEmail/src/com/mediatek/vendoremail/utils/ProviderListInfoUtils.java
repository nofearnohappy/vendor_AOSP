package com.mediatek.vendoremail.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.xmlpull.v1.XmlPullParserException;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.android.email.policy.R;

public class ProviderListInfoUtils {
    private static final String TAG = "EmailPolicy";

    /// M: Define string for protocols in provider information
    private static final String LEGACY_SCHEME_EAS = "eas";
    private static final String LEGACY_SCHEME_POP = "pop3";
    private static final String LEGACY_SCHEME_IMAP = "imap";

    private static final Configuration sOldConfiguration = new Configuration();
    private static LinkedHashMap<String, ProviderUiInfo> sProviderUiInfoMap = null;
    // Especially provide some server information by protocol.
    private static HashMap<String, ProviderInfo> sEasProviderInfoMap = null;
    private static HashMap<String, ProviderInfo> sPopProviderInfoMap = null;
    private static HashMap<String, ProviderInfo> sImapProviderInfoMap = null;
    private static final Object sProviderMapLock = new Object();
    private static TypedArray sProviderIconResArray = null;

    private static TypedArray getProviderIconResMap(final Context context) {
        if (sProviderIconResArray == null) {
            final Resources res = context.getResources();
            sProviderIconResArray = res.obtainTypedArray(R.array.email_provider_icon_list);
        }
        return sProviderIconResArray;
    }

    /**
     * Parse to get the providers' ui information map.
     * @param context
     * @return
     */
    private static void parseProviderUiInfo(final Context context) {
        sProviderUiInfoMap = new LinkedHashMap<String, ProviderUiInfo>();
        try {
            final Resources res = context.getResources();
            final XmlResourceParser xml = res.getXml(R.xml.provider_ui_info);
            int xmlEventType;
            // walk through senders.xml file.
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG &&
                        "provideruiinfo".equals(xml.getName())) {
                    final ProviderUiInfo info = new ProviderUiInfo();
                    final TypedArray ta =
                            res.obtainAttributes(xml, R.styleable.ESPListInfo);
                    info.name = ta.getString(R.styleable.ESPListInfo_name);
                    info.domain = ta.getString(R.styleable.ESPListInfo_serverdomain);
                    info.defaultProtocol = ta.getString(R.styleable.ESPListInfo_defaultprotocol);
                    info.hint = ta.getString(R.styleable.ESPListInfo_hint);
                    int index = ta.getInteger(R.styleable.ESPListInfo_icon, 0);
                    info.iconResId = getProviderIconResMap(context).getResourceId(index, 0);
                    Log.d(TAG, "Get Provider UI info: " + info);
                    sProviderUiInfoMap.put(info.domain, info);
                }
            }
        } catch (XmlPullParserException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Parse a xml to get provider server information.
     * @param context
     * @param id The xml to parse.
     * @return
     */
    private static void parseProviderInfo(final Context context, int id) {
        sEasProviderInfoMap = new HashMap<String, ProviderInfo>();
        sPopProviderInfoMap = new HashMap<String, ProviderInfo>();
        sImapProviderInfoMap = new HashMap<String, ProviderInfo>();
        try {
            final Resources res = context.getResources();
            final XmlResourceParser xml = res.getXml(id);
            int xmlEventType;
            // walk through senders.xml file.
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG &&
                        "providerinfo".equals(xml.getName())) {
                    final ProviderInfo info = new ProviderInfo();
                    final TypedArray ta =
                            res.obtainAttributes(xml, R.styleable.EmailProviderListInfo);
                    info.id = ta.getString(R.styleable.EmailProviderListInfo_id);
                    info.label = ta.getString(R.styleable.EmailProviderListInfo_label);
                    info.domain = ta.getString(R.styleable.EmailProviderListInfo_domain);
                    info.incomingUriTemplate = ta.getString(R.styleable.EmailProviderListInfo_incominguritemplate);
                    info.incomingUsernameTemplate = ta.getString(R.styleable.EmailProviderListInfo_incomingusernametemplate);
                    info.outgoingUriTemplate = ta.getString(R.styleable.EmailProviderListInfo_outgoinguritemplate);
                    info.outgoingUsernameTemplate = ta.getString(R.styleable.EmailProviderListInfo_outgoingusernametemplate);
                    info.note = ta.getString(R.styleable.EmailProviderListInfo_note);
                    URI uri = new URI(info.incomingUriTemplate);
                    String scheme = uri.getScheme();
                    String[] schemeParts = scheme.split("\\+");
                    String protocol = schemeParts[0];
                    if (LEGACY_SCHEME_EAS.equals(protocol)) {
                        sEasProviderInfoMap.put(info.domain, info);
                    } else if (LEGACY_SCHEME_POP.equals(protocol)) {
                        sPopProviderInfoMap.put(info.domain, info);
                    } else if (LEGACY_SCHEME_IMAP.equals(protocol)) {
                        sImapProviderInfoMap.put(info.domain, info);
                    } else {
                        Log.e(TAG, "provider information protocol error! Ignore one.");
                    }
                }
            }
        } catch (URISyntaxException e) {
            // ignore
            Log.e(TAG, "provider information configuration error! Ignore one.");
        } catch (XmlPullParserException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Parse provider_info.xml provider_info_exchange.xml provider_ui_info.xml file to find our
     * available email service providers' information
     */
    private static void prepareProviderInfoMaps(final Context context) {
        /**
         * We cache localized strings here, so make sure to regenerate the information maps if
         * the locale changes
         */
        if (sProviderUiInfoMap == null) {
            sOldConfiguration.setTo(context.getResources().getConfiguration());
        }

        final int delta =
                sOldConfiguration.updateFrom(context.getResources().getConfiguration());

        // Just return if we have prepare all necessary information and no need to update, which
        // means no need to force reload or no locale change happened.
        if (sProviderUiInfoMap != null && sEasProviderInfoMap != null
                && sPopProviderInfoMap != null && sImapProviderInfoMap != null
                && !Configuration.needNewResources(delta, ActivityInfo.CONFIG_LOCALE)) {
            return;
        }

        synchronized (sProviderMapLock) {
            Log.d(TAG, "prepareProviderInfoMaps");
            sProviderIconResArray = null;
            sProviderUiInfoMap = null;
            sEasProviderInfoMap = null;
            sPopProviderInfoMap = null;
            sImapProviderInfoMap = null;
            parseProviderUiInfo(context);
            parseProviderInfo(context, R.xml.provider_info);
        }
    }

    /**
     * Find the provider information for a specified domain, if no domain passed in or no provider
     * information found, just return null.
     * @param context
     * @param domain
     * @param protocol
     * @return
     */
    public static ProviderInfo findProviderForDomainProtocol(final Context context, String domain,
            String protocol) {
        if (TextUtils.isEmpty(domain)) {
            Log.d(TAG, "No domain specified!");
            return null;
        }
        prepareProviderInfoMaps(context);
        // find default protocol if none is set
        if (TextUtils.isEmpty(protocol)) {
            // find provider list in ui for this domain
            ProviderUiInfo defaultProvider = sProviderUiInfoMap.get(domain);
            if (defaultProvider != null) {
                protocol = defaultProvider.defaultProtocol;
                Log.d(TAG, "no protocol specified, use default");
            }
        }
        if (LEGACY_SCHEME_EAS.equals(protocol)) {
            return sEasProviderInfoMap.get(domain);
        } else if (LEGACY_SCHEME_POP.equals(protocol)) {
            return sPopProviderInfoMap.get(domain);
        } else if (LEGACY_SCHEME_IMAP.equals(protocol)) {
            return sImapProviderInfoMap.get(domain);
        } else if (TextUtils.isEmpty(protocol)) {
            // find a matched provider for this domain
            return findAMatchedProviderForDomain(domain);
        } else {
            Log.e(TAG, "Wrong protocol: " + protocol);
            return null;
        }
    }

    /**
     * Find order: eas-->imap-->pop3
     * @param domain
     * @return
     */
    private static ProviderInfo findAMatchedProviderForDomain(String domain) {
        ProviderInfo pi = null;
        pi = sEasProviderInfoMap.get(domain);
        if (pi == null) {
            pi = sImapProviderInfoMap.get(domain);
        }
        if (pi == null) {
            pi = sPopProviderInfoMap.get(domain);
        }
        return pi;
    }

    public static ProviderInfo findProviderForDomain(final Context context, String domain) {
        return findProviderForDomainProtocol(context, domain, null);
    }

    /**
     * Return the generic provider ui information map.
     * @param context
     * @return
     */
    public static LinkedHashMap<String, ProviderUiInfo> getProviderUiInfoMap(final Context context) {
        prepareProviderInfoMaps(context);
        return sProviderUiInfoMap;
    }
}
