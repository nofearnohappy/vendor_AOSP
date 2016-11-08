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

import android.content.Intent;
import android.net.Uri;

import com.mediatek.mms.ext.DefaultOpMmsConfigExt;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.provider.Browser;
import android.view.inputmethod.EditorInfo;

import com.mediatek.custom.CustomProperties;
import android.os.Bundle;

/**
 * Op01MmsConfigExt.
 *
 */
public class Op01MmsConfigExt extends DefaultOpMmsConfigExt {
    private static final String TAG = "Mms/Op01MmsConfigExt";

    private static final int SMS_TO_MMS_THRESHOLD = 11;
    private static final int MAX_CMCC_TEXT_LENGTH = 3100;
    private static final int RECIPIENTS_LIMIT = 50;
//    private static final String UAPROFURL_OP01 = "http://218.249.47.94/Xianghe/MTK_Athens15_UAProfile.xml";
    private static final int SOCKET_TIMEOUT = 90 * 1000;
    private static final int SOCKET_SEND_TIMEOUT = 30 * 1000;
    private static final int[] OP01DEFAULTRETRYSCHEME = {
        0, 5 * 1000, 15 * 1000, 30 * 1000, 5 * 60 * 1000, 10 * 60 * 1000, 30 * 60 * 1000};

    /**
     * getSmsToMmsTextThreshold.
     * @return the number of sms transfer to mms.
     */
    public int getSmsToMmsTextThreshold() {
        return SMS_TO_MMS_THRESHOLD;
    }

    public void setSmsToMmsTextThreshold(int value){
        // TODO: currently, for operator, this value is not customized by configuration file. why?
        //SMS_TO_MMS_THRESHOLD = value;
    }

    public int getMaxTextLimit() {
        return MAX_CMCC_TEXT_LENGTH;
    }

    public void setMaxTextLimit(int value) {
        //do nothing
    }

    public int getMmsRecipientLimit() {
        return RECIPIENTS_LIMIT;
    }

    public void setMmsRecipientLimit(int value) {
        //do nothing
    }

    public void setHttpSocketTimeout(int socketTimeout) {
        Log.d(TAG, "set socket timeout: " + socketTimeout);
    }

    public int getMmsRetryPromptIndex() {
        Log.d(TAG, "getMmsRetryPromptIndex");
        return 4;
    }

    public int[] getMmsRetryScheme() {
        Log.d(TAG, "getMmsRetryScheme");
        return OP01DEFAULTRETRYSCHEME;
    }

/*
    public void printMmsMemStat(Context context, String callerTag) {
        Log.d(TAG, callerTag + " call printMmsMemStat");
        String mmsProcName = "com.android.mms";
        int pid = 0;

        //Find Mms process
        //Context context = getApplicationContext();
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        //Map<Integer, String> pidMap = new TreeMap<Integer, String>();
        for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            //pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
            if (mmsProcName.equals(runningAppProcessInfo.processName)) {
                pid = runningAppProcessInfo.pid;
                printMmsMemStat(context, pid);
                break;
            }
        }
    }

    private void printMmsMemStat(Context context, int pid) {
        Log.d(TAG, "printMmsMemStat " + pid);

        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        int pids[] = new int[1];
        pids[0] = pid;

        android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
        if (memoryInfoArray[0] == null) {
            Log.d(TAG, "getProcessMemoryInfo failed!");
            return;
        }
        Log.d(TAG, "Mms Mem: [PrivateDirty = " + memoryInfoArray[0].getTotalPrivateDirty() + "]");
    }
*/
    /// M: ALPS00527989, Extend TextView URL handling @ {
    /**
     * setExtendUrlSpan.
     * @param textView TextView
     */
    public static void setExtendUrlSpan(TextView textView) {
        Log.d(TAG, "setExtendUrlSpan");

        CharSequence text = textView.getText();

        Spanned spanned = ((Spanned) text);
        URLSpan[] urlSpanArray = textView.getUrls();
        for (int i = 0; i < urlSpanArray.length; i++) {
            String url = urlSpanArray[i].getURL();
            Log.d(TAG, "find url:" + url);
            if (isWebUrl(url)) {
                URLSpan newurlSpan = new ExtendURLSpan(url);
                int spanStart = spanned.getSpanStart(urlSpanArray[i]);
                int spanEnd = spanned.getSpanEnd(urlSpanArray[i]);
                Spannable sp = (SpannableString) (text);
                ((SpannableString) (text)).removeSpan(urlSpanArray[i]);
                ((SpannableString) (text)).setSpan(newurlSpan, spanStart,
                                        spanEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
    }

    private static boolean isWebUrl(String urlString) {
        boolean isWebURL = false;
        Uri uri = Uri.parse(urlString);

        String scheme = uri.getScheme();
        if (scheme != null) {
            isWebURL = scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
                    || scheme.equalsIgnoreCase("rtsp");
        }
        return isWebURL;
    }
    /// @}

    /// M: Add MmsService configure param @{
    public static final String CONFIG_USER_AGENT = "userAgent";
    public static final String CONFIG_UA_PROF_URL = "uaProfUrl";

    public Bundle getMmsServiceConfig() {
        Log.d("Mms/Txn", "getMmsServiceConfig");
        Bundle bundle = new Bundle();
        if (bundle == null) {
            return null;
        }

        //UA
        String ua = getUserAgent();
        Log.d("Mms/Txn", "ua=" + ua);
        bundle.putString(CONFIG_USER_AGENT, ua);

        //UAProf
        String uaProf = getUaProfUrl();
        Log.d("Mms/Txn", "uaprof=" + uaProf);
        bundle.putString(CONFIG_UA_PROF_URL, uaProf);

        return bundle;
    }

    private static String getUserAgent() {
        /// M: @{
        String value = CustomProperties.getString(
                CustomProperties.MODULE_MMS,
                CustomProperties.USER_AGENT,
                "Android-Mms/0.1");
        /// @}
        return value;
    }

    private static String getUaProfUrl() {
        /// M: @{
        String value = CustomProperties.getString(
                CustomProperties.MODULE_MMS,
                CustomProperties.UAPROF_URL,
                "http://www.google.com/oha/rdf/ua-profile-kila.xml");
        /// @}
        return value;
    }
    /// @}

}

