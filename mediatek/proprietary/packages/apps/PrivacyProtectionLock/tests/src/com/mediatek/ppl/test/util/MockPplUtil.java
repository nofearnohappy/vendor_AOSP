/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ppl.test.util;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.R;
import com.mediatek.ppl.ui.DialogChooseNumFragment;
import com.mediatek.ppl.ui.DialogChooseSimFragment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MockPplUtil {
    private static final String TAG = "PPL/MockPplUtil";
    public static final String SERVICE_NUMBER_1st = "10086";
    public static final String SERVICE_NUMBER_2nd = "100861";
    public static final String PASSWORD_ORIGINAL = "111111";
    public static final String PASSWORD_CHANGED = "000000";

    public static final String[] smsString = {
            "我的手机可能被盗，请保留发送此短信的号码",
            "#suoding#",
            "已接受到您的锁屏指令，锁屏成功。",
            "#jiesuo#",
            "已接受到您的解锁指令，解锁成功。",
            "#mima#",
            "您的手机防盗密码为%s。",
            "#xiaohui#",
            "远程删除数据已开始。",
            "远程数据删除已完成，您的隐私得到保护，请放心。",
            "我开启了手机防盗功能，已将你的手机号码设置为紧急联系人号码，这样手机丢失也能够远程控制啦。\n"
                    + "以下是相关指令：\n远程锁定： #suoding#\n远程销毁数据： #xiaohui#\n找回密码： #mima#" };

    /**
     * Test receiver in *Activities
     * */
    public static void testUiReceiver(Activity activity, String activityClass, String action) {
        receiveIntent(activity.getApplicationContext(), activity, "mEventReceiver", "com.mediatek.ppl.ui."
                + activityClass + "$EventReceiver", new Intent(action));
    }

    /**
     * Test receiver in *Service
     * */
    public static void testServiceReceiver(Context context, Service service, String serviceClass, Intent intent) {
        receiveIntent(context, service, "mReceiver", "com.mediatek.ppl." + serviceClass + "$EventReceiver", intent);
    }

    /**
     * Test onReceive method for receivers
     * */
    private static void receiveIntent(Context context, Object object, String filedName, String className,
            Intent intent) {
        try {
            Object eventReceiver = getField(object, filedName);

            Method fOnReceive = Class.forName(className).getDeclaredMethod("onReceive", Context.class, Intent.class);
            fOnReceive.setAccessible(true);
            fOnReceive.invoke(eventReceiver, context, intent);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "* ClassNotFoundException " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "* NoSuchMethodException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, "* InvocationTargetException " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get a filed object from an object, according to the name of filed
     * */
    private static Object getField(Object object, String fieldName) {
        Object fieldObject = null;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            Log.i(TAG, " field is " + field);

            field.setAccessible(true);
            fieldObject = field.get(object);

        } catch (NoSuchFieldException e) {
            Log.e(TAG, "* NoSuchFieldException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        }
        return fieldObject;
    }

    /**
     * Turn on airplane mode (in ViewManualActivity & SetupManualActivity)
     * */
    public static void turnonAirplaneMode(Activity activity) {
        Settings.Global.putString(activity.getApplicationContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, "1");
        activity.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED));
    }

    /**
     * Check airplane mode state (in ViewManualActivity & SetupManualActivity)
     * */
    public static boolean isAirplaneModeEnabled(Activity mActivity) {
        String state = Settings.Global.getString(mActivity.getApplicationContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON);
        return "1".equals(state);
    }

    /**
     * Display DialogChooseSimFragment (in ViewManualActivity & SetupManualActivity)
     * */
    public static void displaySimDiag(Activity activity, int[] insertedSim) {
        String[] itemList = new String[insertedSim.length];
        String itemTemplate = activity.getResources().getString(R.string.item_sim_n);
        for (int i = 0; i < insertedSim.length; ++i) {
            itemList[i] = itemTemplate + i;
        }
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        // Create and show the dialog.
        DialogFragment newFragment = new DialogChooseSimFragment();
        Bundle args = new Bundle();
        args.putStringArray("items", itemList);
        args.putIntArray("value", insertedSim);
        newFragment.setArguments(args);
        newFragment.show(ft, "choose_sim_dialog");
    }

    /**
     * Display ChoosePhoneNumberDialogFragment (in *ContactsActivities)
     * */
    public static void displayNumDiag(Activity activity, String[] items, String name, int index) {
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        DialogFragment newFragment = new DialogChooseNumFragment();
        Bundle args = new Bundle();
        args.putStringArray("items", items);
        args.putInt("index", index);
        args.putString("name", name);
        newFragment.setArguments(args);
        newFragment.show(ft, "choose_number_dialog");
    }

    /**
     * Test receiver in *Activities
     * */
    public static void formatLog(String tag, String content) {
        Log.i(tag, "----- " + content + "-----");
    }

    public static boolean compareSendMessage(byte type, int simId) {
        String checkMessage = buidMessagInfo(smsString[type], simId);

        MockPlatformManager platformManager = (MockPlatformManager) PplApplication.getPlatformManager();

        Log.i(TAG, "Check message " + checkMessage);
        Log.i(TAG, "Sent message " + platformManager.getMessageInfo());

        return checkMessage.equals(platformManager.getMessageInfo());
    }

    public static String buidMessagInfo(String text, int simId) {
        return simId + text;
    }

}
