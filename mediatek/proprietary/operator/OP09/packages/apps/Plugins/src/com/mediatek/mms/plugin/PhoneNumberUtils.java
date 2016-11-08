 /*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

package com.mediatek.mms.plugin;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.mediatek.geocoding.GeoCodingQuery;

import java.util.Locale;

/**
 * M: For OP09 Util class for Phone Number.
 */
public class PhoneNumberUtils {

    private static final String TAG = "Mms/OP09PhoneNumberUtils";

    /**
     * M: Get number location.
     *
     * @param context
     *            the Context.
     * @param number
     *            the phone number.
     * @return the location content.
     */
    public static String getNumberLocation(Context context, String number) {
        return getGeoDescription(context, number);
    }

    /**
     * Get Location for number.
     *
     * @param context
     *            the Context.
     * @param number
     *            the phone number.
     * @return the location.
     */
    private static String getGeoDescription(Context context, String number) {
        Log.d("@M_" + TAG, "getGeoDescription(" + number + ")");
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        if (SystemProperties.get("ro.mtk_phone_number_geo").equals("1")) {
            GeoCodingQuery geoCodingQuery = GeoCodingQuery.getInstance(context);
            String cityName = geoCodingQuery.queryByNumber(number);
            Log.d("@M_" + TAG, "[GeoCodingQuery] cityName = " + cityName);
            if (!TextUtils.isEmpty(cityName)) {
                return cityName;
            }
        }

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();

        Locale locale = context.getResources().getConfiguration().locale;
        String countryIso = getCurrentCountryIso(context, locale);
        PhoneNumber pn = null;
        try {
            pn = util.parse(number, countryIso);
            Log.d("@M_" + TAG, "parsing '" + number + "' for countryIso '" + countryIso
                + "',parsed number '" + pn + "', used locale '" + locale + "'...");
        } catch (NumberParseException e) {
            Log.e("@M_" + TAG, "getGeoDescription: NumberParseException for incoming number '" + number
                + "'");
        }
        if (pn != null) {
            String description = geocoder.getDescriptionForNumber(pn, locale);
            Log.d("@M_" + TAG, "- got geoDescription : '" + description + "'");
            return description;
        }
        return null;
    }

    /**
     * M: Get current country iso.
     * @param context the Context.
     * @param locale the current locale.
     * @return the country iso.
     */
    private static String getCurrentCountryIso(Context context, Locale locale) {
        // Without framework function calls, this seems to be the most accurate location service
        // we can rely on.
        final TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String countryIso = telephonyManager.getNetworkCountryIso().toUpperCase();

        if (countryIso == null) {
            countryIso = locale.getCountry();
            Log.w("@M_" + TAG, "No CountryDetector; falling back to countryIso based on locale: "
                + countryIso);
        }
        return countryIso;
    }
}
