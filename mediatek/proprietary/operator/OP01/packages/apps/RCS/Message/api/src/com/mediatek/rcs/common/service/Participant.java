/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.common.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;

/**
 * This parcelable class describes an IM chat participant.
 */
public class Participant implements Parcelable {

    /**
     * The key of a bundle data in the intent calling the ChatActivity.
     */
    public static final String KEY_PARTICIPANT = "participant";
    /**
     * The key of a bundle data in the intent calling the ChatActivity.
     */
    public static final String KEY_PARTICIPANT_LIST = "participantList";

    String mContact = null;

    String mDisplayName = null;

    int mState;

    private static String COUNTRY_CODE = "+86";
    private static String COUNTRY_AREA_CODE = "0";
    private static String COUNTRY_CODE_PLUS = "+";
    private static String sInternationalPrefix = null;
    private static final String METHOD_GET_METADATA = "getMetadataForRegion";

    private static final String REGION_CHINA = "";
    private static final String REGION_TW = "TW";
    private static final String INTERNATIONAL_PREFIX_TW = "0(?:0[25679] | 16 | 17 | 19)";

    static {
        sInternationalPrefix = getInternationalPrefix(getDefaultSimCountryIso());
    }

    /**
     * Constructor of Participant.
     * @param contact
     *            Typically a TEL or SIP URI.
     * @param displayName
     *            The name of the contact displayed in the ChatActvity.
     */
    public Participant(String contact, String displayName) {
        mContact = formatNumberToInternational(contact);
        mDisplayName = displayName;
    }

    protected Participant(Parcel source) {
        mContact = source.readString();
        mDisplayName = source.readString();
    }

    /**
     * Get the contact value of this participant.
     * @return The contact value, typically a TEL/SIP URI.
     */
    public String getContact() {
        return mContact;
    }

    /**
     * Get the display name of this participant.
     * @return The display name of this participant.
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }
    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation
     * @return Integer
     */
    public int describeContents() {
        return 0;
    }

    public static String formatNumberToInternational(String number) {
        if (number == null) {
            return null;
        }

        // Remove spaces
        number = number.trim();

        // Strip all non digits
        String phoneNumber = PhoneNumberUtils.stripSeparators(number);
        if (phoneNumber.equals("")) {
            return "";
        }
        if (sInternationalPrefix == null) {
            String countryIso = null;
            try {
                countryIso = getDefaultSimCountryIso();
            } catch (ClassCastException e) {
                e.printStackTrace();
                // Logger.e(TAG,
                // "formatNumberToInternational() plz check
                // whether your load matches your code base");
            }
            if (countryIso != null) {
                sInternationalPrefix = getInternationalPrefix(countryIso.toUpperCase());
            }
            // Logger.d(TAG, "formatNumberToInternational() countryIso: " + countryIso
            // + " sInternationalPrefix: " + sInternationalPrefix);
        }
        if (sInternationalPrefix != null) {
            Pattern pattern = Pattern.compile(sInternationalPrefix);
            Matcher matcher = pattern.matcher(number);
            StringBuilder formattedNumberBuilder = new StringBuilder();
            if (matcher.lookingAt()) {
                int startOfCountryCode = matcher.end();
                formattedNumberBuilder.append(COUNTRY_CODE_PLUS);
                formattedNumberBuilder.append(number.substring(startOfCountryCode));
                phoneNumber = formattedNumberBuilder.toString();
            }
        }
        // Logger.d(TAG, "formatNumberToInternational() number: " + number + " phoneNumber: "
        // + phoneNumber + " sInternationalPrefix: " + sInternationalPrefix);
        // Format into international
        if (phoneNumber.startsWith("00" + COUNTRY_CODE.substring(1))) {
            // International format
            phoneNumber = COUNTRY_CODE + phoneNumber.substring(4);
        } else if ((COUNTRY_AREA_CODE != null) && (COUNTRY_AREA_CODE.length() > 0)
                && phoneNumber.startsWith(COUNTRY_AREA_CODE)) {
            // National number with area code
            phoneNumber = COUNTRY_CODE + phoneNumber.substring(COUNTRY_AREA_CODE.length());
        } else if (!phoneNumber.startsWith("+")) {
            // National number
            phoneNumber = COUNTRY_CODE + phoneNumber;
        }
        return phoneNumber;
    }

    private static String getDefaultSimCountryIso() {
        int simId;
        String iso = null;
        /*
         * boolean geminiSupport = false; geminiSupport =
         * SystemProperties.get("ro.mtk_gemini_support").equals("1");
         * if (geminiSupport) { simId = PhoneConstants.SIM_ID_1;
         * if (!TelephonyManagerEx.getDefault().getDefault().hasIccCard(simId)) { simId =
         * PhoneConstants.SIM_ID_2 ^ simId; } iso =
         * TelephonyManagerEx.getDefault().getSimCountryIso(simId); } else { iso =
         * TelephonyManager.getDefault().getSimCountryIso(); }
         */
        return iso;
    }

    private static String getInternationalPrefix(String countryIso) {
        /*
         * try { PhoneNumberUtil util = PhoneNumberUtil.getInstance(); Method method =
         * PhoneNumberUtil.class.getDeclaredMethod(METHOD_GET_METADATA, String.class);
         * method.setAccessible(true); PhoneMetadata metadata = (PhoneMetadata) method.invoke(util,
         * countryIso); if (metadata != null) { String prefix = metadata.getInternationalPrefix();
         * if (countryIso.equalsIgnoreCase(REGION_TW)) { prefix = INTERNATIONAL_PREFIX_TW; } return
         * prefix; } } catch (NoSuchMethodException e) { e.printStackTrace(); } catch
         * (IllegalAccessException e) { e.printStackTrace(); } catch (InvocationTargetException e) {
         * e.printStackTrace(); }
         */
        return null;
    }

    /**
     * Write parcelable object
     * @param dest
     *            The Parcel in which the object should be written
     * @param flags
     *            Additional flags about how the object should be written
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mContact);
        dest.writeString(mDisplayName);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<Participant> CREATOR =
            new Parcelable.Creator<Participant>() {
                public Participant createFromParcel(Parcel source) {
                    return new Participant(source);
                }

                public Participant[] newArray(int size) {
                    return new Participant[size];
                }
            };

    @Override
    public boolean equals(Object o) {
        if (o instanceof Participant) {
            if (null == this.mContact) {
                return null == ((Participant) o).mContact;
            } else {
//                return this.mContact.equals(((Participant) o).mContact);
                return PhoneNumberUtils.compare(this.mContact, ((Participant) o).mContact);
            }
        } else {
            return false;
        }
    }

    /**
     * remember if override equals(), must override hashcode().by Shuo.
     */
    @Override
    public int hashCode() {
        if (null != mContact) {
            return mContact.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Contant: ");
        builder.append(mContact);
        builder.append("    DisplayName: ");
        builder.append(mDisplayName);
        return builder.toString();
    }
}

