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

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ILteDataOnlyController;
import com.mediatek.op09.plugin.R;
import android.util.Log;


/**
 * M: OP09 mms utils.
 */
public class Op09MmsUtils {
    private static final String TAG = "Mms/Op09MmsUtils";


    String MASS_TEXT_MESSAGE_GROUP_ID = "mass_txt_msg_group_id";

    private static Op09MmsUtils sOp09MmsUtils;

    public static Op09MmsUtils getInstance() {
        if (sOp09MmsUtils == null) {
            sOp09MmsUtils = new Op09MmsUtils();
        }
        return sOp09MmsUtils;
    }

    public String formatDateAndTimeStampString(Context context, long msgDate, long msgDateSent,
            boolean fullFormat, String formatStr) {
        if (msgDateSent > 0) {
            return MessageUtils.formatDateOrTimeStampStringWithSystemSetting(context, msgDateSent,
                fullFormat);
        } else if (msgDate > 0) {
            return MessageUtils.formatDateOrTimeStampStringWithSystemSetting(context, msgDate,
                fullFormat);
        } else {
            return formatStr;
        }
    }


    /// M: called by WPMessageListItem,but not visible, so ignore
    public static void showSimTypeBySubId(Context context, int subId, TextView textView) {
        Drawable simTypeDraw = null;
        Log.d("@M_" + TAG, "showSimTypeBySimId");
        SubscriptionInfo simInfo = SubscriptionManager.from(context)
                .getActiveSubscriptionInfo(subId);
        if (simInfo != null) {
            Bitmap origenBitmap = simInfo.createIconBitmap(context);
            Bitmap bitmap = MessageUtils.resizeBitmap(context, origenBitmap);
            simTypeDraw = new BitmapDrawable(context.getResources(), bitmap);
        } else {
            simTypeDraw = context.getResources()
                .getDrawable(R.drawable.sim_indicator_no_sim_mms);
        }
        if (textView != null) {
            String text = textView.getText().toString().trim();
            textView.setText("  " + text + "  ");
            textView.setBackgroundDrawable(simTypeDraw);
        }
    }

    public String formatDateTime(Context context, long time, int formatFlags) {
        return MessageUtils.formatDateTime(context, time, formatFlags);
    }

    private static final String TEXT_SIZE = "message_font_size";
    private static final float DEFAULT_TEXT_SIZE = 18;
    private static final float MIN_TEXT_SIZE = 10;
    private static final float MAX_TEXT_SIZE = 32;

    /**
     * M: Get text size.
     * @param context the Context.
     * @return the text size.
     */
    public static float getTextSize(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        float size = sp.getFloat(TEXT_SIZE, DEFAULT_TEXT_SIZE);
        Log.d("@M_" + TAG, "getTextSize = " + size);
        if (size < MIN_TEXT_SIZE) {
            size = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            size = MAX_TEXT_SIZE;
        }
        return size;
    }

    /**
     * M: set text size.
     * @param context the context.
     * @param size the text size.
     */
    public static void setTextSize(Context context, float size) {
        float textSize;
        Log.d("@M_" + TAG, "setTextSize = " + size);

        if (size < MIN_TEXT_SIZE) {
            textSize = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            textSize = MAX_TEXT_SIZE;
        } else {
            textSize = size;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(TEXT_SIZE, textSize);
        editor.commit();
    }

    /**
     * M: Juduge the addres is dialable for ct.
     * @param address the phone number.
     * @return true: is phone number; false: is not phone number.
     */


    public void setIntentDateForMassTextMessage(Intent intent, long groupId) {
        if (intent == null) {
            return;
        }
        intent.putExtra(MASS_TEXT_MESSAGE_GROUP_ID, groupId < 0 ? groupId : -1L);
    }



    public boolean isCDMAType(Context context, int subId) {
        return MessageUtils.isCDMAType(context, subId);
    }

    public SubscriptionInfo getSubInfoBySubId(Context ctx, int subId) {
        return MessageUtils.getSimInfoBySubId(ctx, subId);
    }

    public SubscriptionInfo getFirstSimInfoBySlotId(Context ctx, int slotId) {
        return MessageUtils.getFirstSimInfoBySlotId(ctx, slotId);
    }

    public boolean is4GDataOnly(Context context, int subId) {
        Log.e(TAG, "@M [is4GDataOnly]");
        if (context == null) {
            return false;
        }
        boolean result = false;
        ILteDataOnlyController ldoc = (ILteDataOnlyController) MPlugin.createInstance(
            ILteDataOnlyController.class.getName(), context);
        if (ldoc == null) {
            Log.e(TAG, "@M [is4GDataOnly],ldoc == null, return false");
            result = false;
            return result;
        }
        if (ldoc.checkPermission(subId)) {
            result = false;
        } else {
            result = true;
        }
        Log.e(TAG, "@M [is4GDataOnly],result:" + result);
        return result;
    }


    /**
     * M: create sendbutton bitmap resource.
     * @param context the context.
     * @param resId the background image.
     * @param subId the sendbutton icon subId.
     * @param isActivated can be highlight to send message.
     * @return send button bitmap.
     */
    public static Bitmap createSendButtonBitMap(Context context, int resId, int subId,
            boolean isActivated) {
        SubscriptionInfo subscriptionInfo = SubscriptionManager.from(context)
                .getActiveSubscriptionInfo(subId);
        int iconTint = subscriptionInfo.getIconTint();
        Drawable resDrawable = context.getResources().getDrawable(resId);
        Bitmap iconBitmap = ((BitmapDrawable) resDrawable).getBitmap();
        int width = iconBitmap.getWidth();
        int height = iconBitmap.getHeight();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Bitmap workingBitmap = Bitmap.createBitmap(metrics, width, height,
                iconBitmap.getConfig());
        Canvas canvas = new Canvas(workingBitmap);
        Paint paint = new Paint();
        if (isActivated) {
            paint.setColorFilter(new PorterDuffColorFilter(iconTint,
                    PorterDuff.Mode.SRC_ATOP));
        }
        canvas.drawBitmap(iconBitmap, 0, 0, paint);
        paint.setColorFilter(null);
        float imgxOffset = width / 2 + 4;
        float imgyOffset = height / 2 + 4;
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inSampleSize = 2;
        Bitmap subIconBitmap = subscriptionInfo.createIconBitmap(context);
        int subIconWidth = subIconBitmap.getWidth();
        int subIconHeight = subIconBitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(0.6f, 0.6f);
        Bitmap scaleBitmap = Bitmap.createBitmap(subIconBitmap, 0, 0,
                subIconWidth, subIconHeight, matrix, true);
        canvas.drawBitmap(scaleBitmap, imgxOffset, imgyOffset, paint);
        return workingBitmap;
    }
    public static String formatTimeStampStringExtend(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            if ((now.yearDay - then.yearDay) != 1) {
                format_flags |= DateUtils.FORMAT_SHOW_DATE;
            }
        } else if (!((now.toMillis(false) - then.toMillis(false)) < 60000)) {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }
        return MessageUtils.formatDateTime(context, when, format_flags);
    }
    public static String getHumanReadableSize(long size) {
        /// @}
        String tag;
        float fsize = (float) size;
        if (size < 1024L) {
            tag = String.valueOf(size) + "B";
        } else if (size < 1024L * 1024L) {
            fsize /= 1024.0f;
            tag = String.format(Locale.ENGLISH, "%.2f", fsize) + "KB";
        } else {
            fsize /= 1024.0f * 1024.0f;
            tag = String.format(Locale.ENGLISH, "%.2f", fsize) + "MB";
        }
        return tag;
    }
}
