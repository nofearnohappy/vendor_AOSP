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

package com.mediatek.mms.plugin;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.internal.telephony.ISms;
import com.android.internal.telephony.TelephonyIntents;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.mms.plugin.Op09MmsFeatureManagerExt;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;

import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M: For OP09; Message Utils.
 */
public class MessageUtils {

    private static final int SINGLE_SIM_CARD = 1;
    private static final int DOUBLE_SIM_CARD = 2;

    private static final int GEMINI_SIM_1 = 0;

    private static final String TIMEZONE_ID_BEIJING = "Asia/Shanghai";

    public static final int TOAST_TYPE_FOR_SAVE_DRAFT = 0;
    public static final int TOAST_TYPE_FOR_SEND_MSG = 1;
    public static final int TOAST_TYPE_FOR_ATTACH = 2;
    public static final int TOAST_TYPE_FOR_DOWNLOAD_MMS = 3;

    public static final String SMS_INPUT_MODE = "pref_key_sms_input_mode";

    public String MASS_TEXT_MESSAGE_GROUP_ID = "mass_txt_msg_group_id";

    /// M: the action name for low storage.
    public static final String ACTION_STORAGE_LOW
            = "android.intent.action.OP09_DEVICES_STORAGE_LOW";
    /// M: the action name for not low storage.
    public static final String ACTION_STORAGE_NOT_LOW
            = "android.intent.action.OP09_DEVICES_STORAGE_NOT_LOW";

    private static final String TAG = "OP09MessageUtils";

    private static Op09StorageLowExt sStorageLowPlugin;

    /// M: For OP09; Device storage low or not low.
    private static boolean mCTDeviceStorageLow = false;

    public static final String RESEND_MESSAGE_ACTION = "com.mediatek.mms.op09.RESEND_MESSAGE";

    public static final boolean MTK_WAPPUSH_SUPPORT
    = SystemProperties.get("ro.mtk_wappush_support").equals("1");
    public static final boolean MTK_C2K_SUPPORT
    = SystemProperties.get("ro.mtk_c2k_support").equals("1");
    public static final boolean MTK_GMO_ROM_OPTIMIZE
    = SystemProperties.get("ro.mtk_gmo_rom_optimize").equals("1");

    // allowable phone number separators
    private static final char[] NUMERIC_CHARS_SUGAR = {
        '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    };
    private static HashMap numericSugarMap = new HashMap (NUMERIC_CHARS_SUGAR.length);
    /**
     * M: get short time format.
     * @param context the Context.
     * @param time the time stamp.
     * @return the format string.
     */
    public static String getShortTimeString(Context context, long time) {
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
            | DateUtils.FORMAT_CAP_AMPM;
        formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        return formatDateTime(context, time, formatFlags);
    }

    /**
     * M: formate the date and time.
     * @param context the Context.
     * @param time the time stamp.
     * @param formatFlags the format flag.
     * @return the formated string.
     */
    public static String formatDateTime(Context context, long time, int formatFlags) {
        if (!isInternationalRoamingStatus(context)) {
            Log.d("@M_" + TAG, "formatDateTime Default");
            return DateUtils.formatDateTime(context, time, formatFlags);
        }
        int localNum = Settings.System.getInt(context.getContentResolver(),
            Settings.System.CT_TIME_DISPLAY_MODE, 0);
        if (localNum == 0) {
            Formatter f = new Formatter(new StringBuilder(50), Locale.CHINA);
            String str = DateUtils.formatDateRange(context, f, time, time, formatFlags,
                TIMEZONE_ID_BEIJING).toString();
            Log.d("@M_" + TAG, "FormateDateTime  Time:" + time + "\t formatFlags:" + formatFlags
                    + "\tTimeZone:" + TIMEZONE_ID_BEIJING);
            return str;
        } else {
            Log.d("@M_" + TAG, "FormateDateTime; time display mode: LOCAL");
            return DateUtils.formatDateTime(context, time, formatFlags);
        }
    }

    /**
     * M: check the first sim is whether in internation roaming status or not.
     * @param context the Context.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatus(Context context) {
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        int simCount = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
        /// M: Two SIMs inserted
        if (simCount == DOUBLE_SIM_CARD) {
            // isRoaming = isCdmaRoaming();
            isRoaming = telephonyManagerEx.isNetworkRoaming(GEMINI_SIM_1);
        } else if (simCount == SINGLE_SIM_CARD) {
            // One SIM inserted
            SubscriptionInfo sir = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoList().get(0);
            isRoaming = telephonyManagerEx.isNetworkRoaming(sir.getSimSlotIndex());
        } else { // Error: no SIM inserted
            Log.e("@M_" + TAG, "There is no SIM inserted!");
        }
        Log.d("@M_" + TAG, "isInternationalRoamingStatus:" + isRoaming);
        return isRoaming;
    }

    /**
     * M: check the subId sim is whether in internation roaming status or not.
     * @param context the Context.
     * @param subId the subId.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatus(Context context, long subId) {
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        int simCount = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();

        if (simCount <= 0) {
            Log.e("@M_" + TAG, "MessageUtils.isInternationalRoamingStatus(): Wrong subId!");
            return false;
        }
        isRoaming = telephonyManagerEx.isNetworkRoaming(SubscriptionManager.getSlotId((int) subId));
        Log.d("@M_" + TAG, "isInternationalRoamingStatus() isRoaming: " + isRoaming);
        return isRoaming;
    }

    /**
     * M: check the subId sim is whether in internation roaming status or not.
     * @param context the Context.
     * @param subId the sim subid.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatusBySubId(Context context, long subId) {
        SubscriptionInfo sir = getSimInfoBySubId(context, subId);
        if (sir == null) {
            return false;
        }
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        isRoaming = telephonyManagerEx.isNetworkRoaming(sir.getSimSlotIndex());
        Log.d("@M_" + TAG, "isInternationalRoamingStatus() isRoaming: " + isRoaming);
        return isRoaming;
    }

    /**
     * M: remove year date from date formated string.
     * @param allFormatStr the formated date string.
     * @return the reformated string.
     */
    private static String removeYearFromFormat(String allFormatStr) {
        if (TextUtils.isEmpty(allFormatStr)) {
            return allFormatStr;
        }
        String finalStr = "";
        int yearIndex = allFormatStr.indexOf("y");
        int monthIndex = allFormatStr.indexOf("M");
        int dayIndex = allFormatStr.indexOf("d");
        if (yearIndex >= 0) {
            if (yearIndex > monthIndex) {
                finalStr = allFormatStr.substring(0, yearIndex).trim();
            } else if (monthIndex > dayIndex) {
                finalStr = allFormatStr.substring(dayIndex, allFormatStr.length()).trim();
            } else {
                finalStr = allFormatStr.substring(monthIndex, allFormatStr.length()).trim();
            }
            if (finalStr.endsWith(",") || finalStr.endsWith("/") || finalStr.endsWith(".")
                || finalStr.endsWith("-")) {
                finalStr = finalStr.substring(0, finalStr.length() - 1);
            }
            return finalStr;
        } else {
            return allFormatStr;
        }
    }

    /**
     * M: format date or time stamp with the system settings.
     * @param context the Context.
     * @param when the date or time stamp.
     * @param fullFormat true: show time. false: not show time.
     * @return the formated sting.
     */
    public static String formatDateOrTimeStampStringWithSystemSetting(Context context, long when,
            boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
            | DateUtils.FORMAT_CAP_AMPM;
        SimpleDateFormat sdf = (SimpleDateFormat) (DateFormat.getDateFormat(context));
        String allDateFormat = sdf.toPattern();

        if (fullFormat) {
            String timeStr = getShortTimeString(context, when);
            String dateStr = DateFormat.format(allDateFormat, when).toString();
            formatFlags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
            String defaultDateStr = formatDateTime(context, when, formatFlags);
            if (defaultDateStr.indexOf(":") > 5) {
                return dateStr + " " + timeStr;
            }
            return timeStr + " " + dateStr;
        }
        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFlags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
            return DateFormat.format(allDateFormat, when).toString();
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            formatFlags |= DateUtils.FORMAT_SHOW_DATE;
            if ((now.yearDay - then.yearDay) == 1) {
                return context.getString(R.string.str_yesterday);
            } else {
            String dayMonthFormatStr = removeYearFromFormat(allDateFormat);
            return DateFormat.format(dayMonthFormatStr, when).toString();
            }
        } else if (0 <= (now.toMillis(false) - then.toMillis(false))
            && (now.toMillis(false) - then.toMillis(false)) < 60000) {
            return context.getString(R.string.time_now);
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        }
        return formatDateTime(context, when, formatFlags);
    }

    /**
     * M: handle Miessed Pasts for cascaed sms.
     * @param parts the smsMessage parts.
     * @return the message content string.
     */
    public static String handleMissedParts(SmsMessage[] parts) {
        if (parts == null || parts.length <= 0) {
            Log.e("@M_" + TAG, "[fake invalid message array");
            return null;
        }

        int totalPartsNum = parts[0].getUserDataHeader().concatRef.msgCount;

        String[] fakeContents = new String[totalPartsNum];
        for (SmsMessage msg : parts) {
            int seq = msg.getUserDataHeader().concatRef.seqNumber;
            Log.d("@M_" + TAG, "[fake add segment " + seq);
            fakeContents[seq - 1] = msg.getDisplayMessageBody();
        }
        for (int i = 0; i < fakeContents.length; ++i) {
            if (fakeContents[i] == null) {
                Log.d("@M_" + TAG, "[fake replace segment " + (i + 1));
                fakeContents[i] = "(...)";
            }
        }

        StringBuilder body = new StringBuilder();
        for (String s : fakeContents) {
            body.append(s);
        }
        return body.toString();
    }

    /**
     * M: the sim is cdma sim.
     * @param context the Context.
     * @param subId the subid.
     * @return true: is cdma sim; false: not.
     */
    public static boolean isCDMAType(Context context, int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Log.d("@M_" + TAG, "[isCDMAType]: phoneType = null");
            return false;
        }
        Log.d("@M_" + TAG, "[isCDMAType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
            || phoneType.equalsIgnoreCase("RUIM");
    }

    /**
     * M: get sim info by sub id.
     * @param ctx the context.
     * @param subId the sim's subId.
     * @return the sim Information record.
     */
    public static SubscriptionInfo getSimInfoBySubId(Context ctx, long subId) {
        return SubscriptionManager.from(ctx).getActiveSubscriptionInfo((int) subId);
    }

    /**
     * Get the first SubscriptionInfo with the same slotId.
     * @param ctx the Context.
     * @param slotId the slotId.
     * @return the subInforRecord.
     */
    public static SubscriptionInfo getFirstSimInfoBySlotId(Context ctx, int slotId) {
        return SubscriptionManager.from(ctx).getActiveSubscriptionInfoForSimSlotIndex(slotId);
    }

    /// M: OP09Feature: show dual send button for compose.
    public static boolean isDualSendButtonEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_DUAL_SEND_BUTTON);
    }

    /// M: OP09Feature: the switcher for whether the mass text feature is on or not.
    public static boolean isMassTextEnable() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(Op09MmsFeatureManagerExt.MASS_TEXT_MSG);
    }

    // enable Sms Encoding Type.
    public static boolean isEnableSmsEncodingType() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.ENABLED_SMS_ENCODING_TYPE);
    }

    /// M: OP09Feature: the switcher for changing the lengthRequired MMS to SMS;
    public static boolean isChangeLengthRequiredMmsToSmsEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.CHANGE_LENGTH_REQUIRED_MMS_TO_SMS);
    }

    /// M:OP09 Feature, for replace string.
    public static boolean isStringReplaceEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.STRING_REPLACE_MANAGEMENT);
    }

    /// M:OP09 Feature, for mms cc recipients.
    public static boolean isSupportSendMmsWithCc() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_CC_RECIPIENTS);
    }

    /// M: OP09Feature: sent date is used to show and the received date is used to sort.
    public static boolean isShowDateManagementEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.SHOW_DATE_MANAGEMENT);
    }

    /// M: OP09 Feature: for show dual time for received message item.
    public static boolean isShowDualTimeForMsgItemEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.SHOW_DUAL_TIME_FOR_MESSAGE_ITEM);
    }

    /// M: OP09Feature: the switcher for previewing VCard in MMS compose.
    public static boolean isSupportVCardPreview() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_VCARD_PREVIEW);
    }

    /// M: OP09 Feature: For unsupported Files;
    public static boolean isUnsupportedFilesOn() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_UNSUPPORTED_FILES);
    }

    /// M: OP09Feature: turn page after fling screen left or right;
    public static boolean enableTurnPageWithFlingScreen() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_PLAY_FILING_TURNPAGE);
    }

    /// M: OP09Feature: the switch for cancel download feature is on or off.
    public static boolean isCancelDownloadEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_CANCEL_DOWNLOAD);
    }

    /// M: OP09Feature: the switcher for indicate that the service deal the missed sms is on or off.
    public static boolean isMissedSmsReceiverEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_CANCEL_DOWNLOAD);
    }

    /// M: OP09Feature: the switcher for indicate whether the class zero model is
    ///show latest class_zero msg or not;
    public static boolean isClassZeroModelShowLatestEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.CLASS_ZERO_NEW_MODEL_SHOW_LATEST);
    }

    /// M: OP09Feature: the switch for SMS priority.
    public static boolean isSmsPriorityEnable() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(Op09MmsFeatureManagerExt.SMS_PRIORITY);
    }

    /**
     * the switcher for allow delivery report in roaming status.
     * @param context the Context.
     * @param subId the SubId.
     * @return true: allow to request delivery report. false: forbid request delivery report.
     */
    public static boolean isAllowDRWhenRoaming(Context context, long subId) {
        return Op09MmsConfigExt.getInstance().isAllowDRWhenRoaming(context, subId);
    }
    /// @}

    /// M:OP09 Feature, for mms contact begin
    public static boolean isWellFormedSmsAddress(String address) {
        if (!isDialable(address)) {
            return false;
        }
        String networkPortion =
                PhoneNumberUtils.extractNetworkPortion(address);

        return (!(networkPortion.equals("+")
                  || TextUtils.isEmpty(networkPortion)))
               && isDialable(networkPortion);
    }

    private static boolean isDialable(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isDialable(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** M: True if c is ISO-LATIN characters 0-9, *, # , +, WILD  */
    private static boolean isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == 'N'
                || c == '(' || c == ')';
    }
    /// M:OP09 Feature, for mms contact end

    /// M: OP09Feature: show dialog for new SI Msg;
    public static boolean isShowDialogForNewSIMsg() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(
                            Op09MmsFeatureManagerExt.SHOW_DIALOG_FOR_NEW_SI_MSG);
    }

    /// M:OP09Feature: the switcher for indicate whether the low memory notification
    public static boolean isLowMemoryNotifEnable() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_LOW_MEMORY);
    }
    /**
     * M: For OP09
     * @param context
     */
    public static void dealCTDeviceLowNotification(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        getStorageLowPlugin(context);
        if (getCTDeviceStorageLowStatus()) {
            String notificationTitle = sStorageLowPlugin.getNotificationTitle();
            Intent viewConvIntent = new Intent();
            viewConvIntent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
            viewConvIntent.setAction(Intent.ACTION_VIEW);
            viewConvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewConvIntent, 0);

            Notification notification = new Notification();
            notification.icon = R.drawable.stat_sys_no_sim;
            notification.tickerText = notificationTitle;
            notification.defaults = Notification.DEFAULT_ALL;

            RemoteViews contentView = new RemoteViews(context.getPackageName(),
                    R.layout.status_bar_sms_rejected);
            contentView.setImageViewResource(R.id.icon, R.drawable.stat_sys_no_sim);
            contentView.setTextViewText(R.id.title, notificationTitle);
            contentView.setTextViewText(R.id.text, sStorageLowPlugin.getNotificationBody());
            notification.contentView = contentView;
            notification.contentIntent = pendingIntent;

            sStorageLowPlugin.showNotification(nm, notification);
        } else {
            sStorageLowPlugin.cancelNotification(nm);
        }
    }

    /**
     * M: For OP09
     * @param context
     */
    public static void cancelCTDeviceLowNotification(Context context) {
        getStorageLowPlugin(context);
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        sStorageLowPlugin.cancelNotification(nm);
    }

    /**
     *  M: For OP09
     * @param context
     */
    private static void getStorageLowPlugin(Context context) {
        if (sStorageLowPlugin != null) {
            return;
        }

        sStorageLowPlugin = Op09StorageLowExt.getIntance(context.getApplicationContext());
    }

    /**
     * M: set OP09 Device storage low status
     *
     * @param low
     */
    public synchronized static void setCTDeviceStorageLowStatus(boolean low) {
        mCTDeviceStorageLow = low;
    }

    /**
     * M: get OP09 Device storage low status
     *
     * @return
     */
    public static boolean getCTDeviceStorageLowStatus() {
        return mCTDeviceStorageLow;
    }
    public static String checkAndModifyUrl(String url) {
        if (url == null) {
            return null;
        }

        Uri uri = Uri.parse(url);
        if (uri.getScheme() != null) {
            return url;
        }

        return "http://" + url;
    }

    public static String formatTimeStampString(Context context, long when) {
        return formatTimeStampString(context, when, false);
    }

    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
        // / M: Fix ALPS00419488 to show 12:00, so mark
        // DateUtils.FORMAT_ABBREV_ALL
        // DateUtils.FORMAT_ABBREV_ALL |
                DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make
        // showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return MessageUtils.formatDateTime(context, when, format_flags);

    }

    private static final String MMS_APP_PACKAGE = "com.android.mms";
    /// KK migration, for default MMS function. @{
    public static boolean isSmsEnabled(Context context) {
        String defaultSmsApplication = Telephony.Sms.getDefaultSmsPackage(context);

        if (defaultSmsApplication != null && defaultSmsApplication.equals(MMS_APP_PACKAGE)) {
            return true;
        }
        return false;
    }

    /**
     * M: For EVDO: check the sim is whether UIM or not.
     * @param subId the sim's sub id.
     * @return true: UIM; false: not UIM.
     */
    public static boolean isUSimType(int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Log.d(TAG, "[isUIMType]: phoneType = null");
            return false;
        }
        Log.d(TAG, "[isUIMType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
            || phoneType.equalsIgnoreCase("RUIM");
    }
    public static boolean isSimMessageAccessable(Context context, int... subId) {
        // First, forbid to access SIM message if this is not default MMS.
        boolean isSmsEnable = isSmsEnabled(context);
        if (!isSmsEnable) {
            Log.d(TAG, "isSimMessageAccessable Sms not enabled");
            return false;
        }

        // Second, check airplane mode
        boolean airplaneOn = Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        if (airplaneOn) {
            Log.d(TAG, "isSimMessageAccessable airplane is On");
            return false;
        }

        // Third, check whether has inserted SIM
        List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context.getApplicationContext())
                .getActiveSubscriptionInfoList();
        if (subInfoList == null || subInfoList.size() == 0) {
            Log.d(TAG, "isSimMessageAccessable SIM not insert");
            return false;
        }

        // Forth, check sms ready
        ISms mSmsManager = ISms.Stub.asInterface(ServiceManager.getService("isms"));
        if (mSmsManager == null) {
            Log.d(TAG, "isSimMessageAccessable mSmsManager is null");
            return false;
        }
        boolean isSimReady = false;
        try {
            if (subId.length == 1) {
                isSimReady = mSmsManager.isSmsReadyForSubscriber(subId[0]);
            } else {
                for (SubscriptionInfo subInfoRecord : subInfoList) {
                    isSimReady =
                            mSmsManager.isSmsReadyForSubscriber(subInfoRecord.getSubscriptionId());
                    if (isSimReady) {
                        break;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "isSimMessageAccessable failed to get sms state");
            isSimReady = false;
        }

        Log.d(TAG, "isSimMessageAccessable" + isSimReady);
        return isSimReady;
    }
    public static boolean isDeliveryReportInRoamingEnable() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(
                    Op09MmsFeatureManagerExt.DELIEVEEY_REPORT_IN_ROAMING);
    }

    public static boolean isSupportCBMessage(Context context, int simId) {
        return !MessageUtils.isCDMAType(context, simId);
    }

    public static boolean isCSIMInGsmMode(int subId) {
        if (isUSimType(subId)) {
            TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
            int vnt = tmEx.getPhoneType(SubscriptionManager.getSlotId(subId));
            Log.d(TAG,
                "[isCSIMInGsmMode]:[NO_PHONE = 0; GSM_PHONE = 1; CDMA_PHONE = 2;]; phoneType:"
                    + vnt);
            if (vnt == TelephonyManager.PHONE_TYPE_GSM) {
                return true;
            }
        }
        return false;
    }
    /// M: OP09 Feature:  support tab setting for mms setting.
    public static boolean isSupportTabSetting() {
        return Op09MmsFeatureManagerExt.isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_TAB_SETTING);
    }

    /// M: OP09Feature: format data and time stamp.
    public static boolean isFormatDateAndTimeStampEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.FORMAT_DATE_AND_TIME);
    }

    /// M: OP09Feautre: show preview for recipient.
    public static boolean isShowPreviewForRecipient() {
        return true;
    }

    /// M:OP09Feature: more strict validation for sms addr.
    public static boolean isMoreStrictValidateForSmsAddr() {
        return true;
    }

    /// M: OP09Feature: the switcher for multi compose
    public static boolean isMultiComposeEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_MULTI_COMPOSE);
    }

    /** M:
     * Get sms encoding type set by user.
     * @param context
     * @return encoding type
     */
    public static int getSmsEncodingType(Context context) {
        int type = SmsMessage.ENCODING_UNKNOWN;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String encodingType = null;
        encodingType = prefs.getString(SMS_INPUT_MODE, "Automatic");

        if ("Unicode".equals(encodingType)) {
            type = SmsMessage.ENCODING_16BIT;
        } else if ("GSM alphabet".equals(encodingType)) {
            type = SmsMessage.ENCODING_7BIT;
        }
        return type;
    }

    /// M: OP09Feature: show number location.
    public static boolean isNumberLocationEnable() {
        return Op09MmsFeatureManagerExt
                .isFeatureEnabled(Op09MmsFeatureManagerExt.MMS_NUMBER_LOCATION);
    }

    /// M: change this method to plugin
    public static int getMmsRecipientLimit() {
        return 20;
    }

    /**
     * Returns true if the address passed in is a valid MMS address.
     */
    public static boolean isValidMmsAddress(String address) {
        String retVal = parseMmsAddress(address);
        /// M: @{
        //return (retVal != null);
        return (retVal != null && !retVal.equals(""));
        /// @}
    }
    /**
     * parse the input address to be a valid MMS address.
     * - if the address is an email address, leave it as is.
     * - if the address can be parsed into a valid MMS phone number, return the parsed number.
     * - if the address is a compliant alias address, leave it as is.
     */
    public static String parseMmsAddress(String address) {
        // if it's a valid Email address, use that.
        if (Mms.isEmailAddress(address)) {
            return address;
        }

        // if we are able to parse the address to a MMS compliant phone number, take that.
        String retVal = parsePhoneNumberForMms(address);
        if (retVal != null) {
            return retVal;
        }

        // if it's an alias compliant address, use that.
        if (isAlias(address)) {
            return address;
        }

        // it's not a valid MMS address, return null
        return null;
    }
    // An alias (or commonly called "nickname") is:
    // Nickname must begin with a letter.
    // Only letters a-z, numbers 0-9, or . are allowed in Nickname field.
    public static boolean isAlias(String string) {
        return Op09MessagePluginExt.sCallback.isAlias(string);
    }
    /**
     * Given a phone number, return the string without syntactic sugar, meaning parens,
     * spaces, slashes, dots, dashes, etc. If the input string contains non-numeric
     * non-punctuation characters, return null.
     */
    private static String parsePhoneNumberForMms(String address) {
        StringBuilder builder = new StringBuilder();
        int len = address.length();

        for (int i = 0; i < len; i++) {
            char c = address.charAt(i);

            // accept the first '+' in the address
            if (c == '+' && builder.length() == 0) {
                builder.append(c);
                continue;
            }

            if (Character.isDigit(c)) {
                builder.append(c);
                continue;
            }

            if (numericSugarMap.get(c) == null) {
                return null;
            }
        }
        return builder.toString();
    }

    public static String getEmailGateway() {
        return   Op09MessagePluginExt.sCallback.getEmailGateway();
    }
    public static CharSequence getVisualTextName(Context context, String enumName,
            int choiceNameResId, int choiceValueResId) {
        CharSequence[] visualNames = context.getResources().getTextArray(choiceNameResId);

        visualNames = new Op09MessageUtilsExt(context).getVisualTextName(visualNames, context,
                choiceNameResId == R.array.pref_sms_save_location_choices);

        CharSequence[] enumNames = context.getResources().getTextArray(choiceValueResId);
        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }
        for (int i = 0; i < enumNames.length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }

    public static int calculateWallpaperSize(Context context, int height, int width) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int currentMaxHeight = windowManager.getDefaultDisplay().getHeight();
        int currentMaxWidth = windowManager.getDefaultDisplay().getWidth();
        int ratio = 1;
        while ((height / ratio) > currentMaxHeight || (width / ratio) > currentMaxWidth) {
            ratio *= 2;
        }
        return ratio;
    }

    public static String extractEncStrFromCursor(Cursor cursor,
            int columnRawBytes, int columnCharset) {
        String rawBytes = cursor.getString(columnRawBytes);
        int charset = cursor.getInt(columnCharset);

        if (TextUtils.isEmpty(rawBytes)) {
            return "";
        } else if (charset == CharacterSets.ANY_CHARSET) {
            return rawBytes;
        } else {
            return new EncodedStringValue(charset, PduPersister.getBytes(rawBytes)).getString();
        }
    }


    /**
    * resize the given bitmap,to make subicon bitmap to fit mms.
    * @param context the context.
    * @param origenBitmap the bitmap get from subscriptionInfo.
    * @return the resized bitmap fit mms icon size.
    */
    public static Bitmap resizeBitmap(Context context, Bitmap origenBitmap) {
        Drawable noSimDrawable = context.getResources().getDrawable(
                R.drawable.sim_indicator_no_sim_mms);
        BitmapDrawable bd = (BitmapDrawable) noSimDrawable;
        Bitmap newBitmap = bd.getBitmap();
        float scaleWidth = ((float) newBitmap.getWidth()) / origenBitmap.getWidth();
        float scaleHeight = ((float) newBitmap.getHeight()) / origenBitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        int resizedW = origenBitmap.getWidth();
        int resizedH = origenBitmap.getHeight();
        Log.d(TAG, "resizeBitmap: resizedw = " + resizedW + ", resizedH = " + resizedH);
        Bitmap bitmap = Bitmap.createBitmap(origenBitmap, 0, 0, resizedW, resizedH, matrix, true);
        return bitmap;
    }

    /**
     * Get active sub count.
     * @param context Context
     * @return return sub count.
     */
    public static int getActiveSubCount(Context context) {
        int count = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
        Log.d(TAG, "getActiveSubCount: " + count);
        return count;
    }

    public static boolean allowSafeDraft(final Activity activity, final Context resourceContext,
            boolean deviceStorageIsFull, boolean isNofityUser, int toastType) {
        Log.d("@M_" + TAG, "allowSafeDraft: deviceStorageIsFull:" +
            deviceStorageIsFull + " isNotifyUser:" + isNofityUser);
        if (activity == null || !deviceStorageIsFull) {
            return true;
        }
        if (deviceStorageIsFull && !isNofityUser) {
            return false;
        }
        if (deviceStorageIsFull && isNofityUser) {
            final String str;
            switch (toastType) {
                case TOAST_TYPE_FOR_SAVE_DRAFT:
                    str = resourceContext.getString(R.string.memory_full_cannot_save);
                    break;
                case TOAST_TYPE_FOR_SEND_MSG:
                    str = resourceContext.getString(R.string.memory_full_cannot_send);
                    break;
                case TOAST_TYPE_FOR_ATTACH:
                    str = resourceContext.getString(R.string.memory_full_cannot_attach);
                    break;
                case TOAST_TYPE_FOR_DOWNLOAD_MMS:
                    str = resourceContext.getString(R.string.memory_full_cannot_download_mms);
                    break;
                default:
                    str = "";
                    break;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), str, Toast.LENGTH_LONG).show();
                }
            });
        }
        return false;
    }
}
