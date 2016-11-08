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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.op.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.common.PluginImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


@PluginImpl(interfaceName="com.mediatek.common.telephony.ISupplementaryServiceExt")
public class SupplementaryServiceExtOP01 extends SupplementaryServiceExt {
    static final String LOG_TAG = "SupplementaryServiceExtOP01";
    private static final String ACTION_SUPPLEMENTARY_SERVICE_UT_TEST
                = "android.intent.action.ACTION_SUPPLEMENTARY_SERVICE_UT_TEST";

    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_SERVICE_CODE = "serviceCode";
    private static final String EXTRA_SERVICE_INFO_A = "serviceInfoA";
    private static final String EXTRA_SERVICE_INFO_B = "serviceInfoB";
    private static final String EXTRA_SERVICE_INFO_C = "serviceInfoC";
    private static final String EXTRA_SERVICE_INFO_D = "serviceInfoD";
    private static final String EXTRA_PHONE_ID = "phoneId";

    private static final String SC_BAIC = "35";
    private static final String SC_BAICr = "351";
    private static final String SC_CFU = "21";
    private static final String SC_CFB = "67";
    private static final String SC_CFNRy = "61";
    private static final String SC_CFNR = "62";
    private static final String SC_WAIT = "43";

    private static final int ACTION_DEACTIVATE = 0;
    private static final int ACTION_ACTIVATE = 1;
    private static final int ACTION_INTERROGATE = 2;

    /**
     * Register to receive intent broadcasts.
     *
     * @param context The Context to register broadccaset receiver.
     * @param phoneId Indicate the phoneId for MSIM.
     */
    public void registerReceiver(Context context, int phoneId) {
        if (!SystemProperties.get("ro.mtk_ims_support").equals("1") ||
                !SystemProperties.get("ro.mtk_volte_support").equals("1")) {
            return;
        }

        if (phoneId == 0) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SUPPLEMENTARY_SERVICE_UT_TEST);
            context.registerReceiver(mBroadcastReceiver, filter);
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(LOG_TAG, "onReceive, action = " + action);

            if (action.equals(ACTION_SUPPLEMENTARY_SERVICE_UT_TEST)) {
                int ssAction = intent.getIntExtra(EXTRA_ACTION, -1);
                String serviceCode = intent.getStringExtra(EXTRA_SERVICE_CODE);
                String serviceInfoA = intent.getStringExtra(EXTRA_SERVICE_INFO_A);
                String serviceInfoB = intent.getStringExtra(EXTRA_SERVICE_INFO_B);
                String serviceInfoC = intent.getStringExtra(EXTRA_SERVICE_INFO_C);
                String serviceInfoD = intent.getStringExtra(EXTRA_SERVICE_INFO_D);
                int phoneId = intent.getIntExtra(EXTRA_PHONE_ID, -1);

                Log.d(LOG_TAG, "onReceive, ssAction = " + ssAction +
                        ", serviceCode = " + serviceCode + ", serviceInfoA = " + serviceInfoA +
                        ", serviceInfoB = " + serviceInfoB + ", serviceInfoC = " + serviceInfoC +
                        ", serviceInfoD = " + serviceInfoD + ", phoneId = " + phoneId);

                phoneId = getValidPhoneId(phoneId);

                Phone[] phone = PhoneFactory.getPhones();
                Phone activePhone = phone[phoneId];

                try {
                    if (isServiceCodeCallForwarding(serviceCode)) {
                        int cfAction = actionToCommandAction(ssAction);
                        int reason = scToCallForwardReason(serviceCode);
                        String dialingNumber = serviceInfoA;
                        int serviceClass = siToServiceClass(serviceInfoB);
                        int time = siToTime(serviceInfoC);
                        long[] timeSlot = convertToLongTime(serviceInfoD);

                        if (isInterrogate(ssAction)) {
                            if (reason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                                activePhone.getCallForwardInTimeSlot(reason, null);
                            } else {
                                activePhone.getCallForwardingOption(reason, null);
                            }
                        } else if (isActivate(ssAction) || isDeactivate(ssAction)) {
                            if (reason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                                activePhone.setCallForwardInTimeSlot(cfAction, reason,
                                        dialingNumber, time, timeSlot, null);
                            } else {
                                activePhone.setCallForwardingOption(cfAction, reason,
                                        dialingNumber, time, null);
                            }
                        } else {
                            Log.d(LOG_TAG, "onReceive: Not supported SS action");
                        }
                    } else if (isServiceCodeCallBarring(serviceCode)) {
                        boolean lockState = isActivate(ssAction);
                        String facility = scToBarringFacility(serviceCode);
                        int serviceClass = siToServiceClass(serviceInfoB);
                        if (isInterrogate(ssAction)) {
                            activePhone.getFacilityLock(facility, "1234", null);
                        } else if (isActivate(ssAction) || isDeactivate(ssAction)) {
                            activePhone.setFacilityLock(facility, lockState,
                                    "1234", null);
                        } else {
                            Log.d(LOG_TAG, "onReceive: Not supported SS action");
                        }
                    } else if (serviceCode != null && serviceCode.equals(SC_WAIT)) {
                        boolean enable = isActivate(ssAction);
                        int serviceClass = siToServiceClass(serviceInfoA);
                        if (isInterrogate(ssAction)) {
                            activePhone.getCallWaiting(null);
                        } else if (isActivate(ssAction) || isDeactivate(ssAction)) {
                            activePhone.setCallWaiting(enable, null);
                        } else {
                            Log.d(LOG_TAG, "onReceive: Not supported SS action");
                        }
                    } else {
                        Log.d(LOG_TAG, "onReceive: Not supported service code");
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static int getValidPhoneId(int phoneId) {
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            return phoneId;
        }
        return 0;
    }

    private static boolean isServiceCodeCallForwarding(String sc) {
        return sc != null &&
                (sc.equals(SC_CFU)
                || sc.equals(SC_CFB) || sc.equals(SC_CFNRy)
                || sc.equals(SC_CFNR));
    }

    private static boolean isServiceCodeCallBarring(String sc) {
        return sc != null &&
                (sc.equals(SC_BAIC)
                || sc.equals(SC_BAICr));
    }

    private static boolean isActivate(int action) {
        return action == ACTION_ACTIVATE;
    }

    private static boolean isDeactivate(int action) {
        return action == ACTION_DEACTIVATE;
    }

    private static boolean isInterrogate(int action) {
        return action == ACTION_INTERROGATE;
    }

    private static int actionToCommandAction(int action) {
        switch (action) {
            case ACTION_DEACTIVATE: return CommandsInterface.CF_ACTION_DISABLE;
            case ACTION_ACTIVATE: return CommandsInterface.CF_ACTION_REGISTRATION;
            case ACTION_INTERROGATE: return 2;
            default:
                throw new RuntimeException("invalid action command");
        }
    }

    private static int scToCallForwardReason(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call forward sc");
        }

        if (sc.equals(SC_CFU)) {
            return CommandsInterface.CF_REASON_UNCONDITIONAL;
        } else if (sc.equals(SC_CFB)) {
            return CommandsInterface.CF_REASON_BUSY;
        } else if (sc.equals(SC_CFNR)) {
            return CommandsInterface.CF_REASON_NOT_REACHABLE;
        } else if (sc.equals(SC_CFNRy)) {
            return CommandsInterface.CF_REASON_NO_REPLY;
        } else {
            throw new RuntimeException("invalid call forward sc");
        }
    }

    private static String scToBarringFacility(String sc) {
        if (sc == null) {
            throw new RuntimeException("invalid call barring sc");
        }

        if (sc.equals(SC_BAIC)) {
            return CommandsInterface.CB_FACILITY_BAIC;
        } else if (sc.equals(SC_BAICr)) {
            return CommandsInterface.CB_FACILITY_BAICr;
        } else {
            throw new RuntimeException("invalid call barring sc");
        }
    }

    private static int siToServiceClass(String si) {
        if (si == null || si.length() == 0) {
                return  CommandsInterface.SERVICE_CLASS_NONE;
        } else {
            int serviceCode = Integer.parseInt(si, 10);

            switch (serviceCode) {
                case 1: return CommandsInterface.SERVICE_CLASS_VOICE;
                case 2: return CommandsInterface.SERVICE_CLASS_VIDEO;
                default:
                    throw new RuntimeException("unsupported service class " + si);
            }
        }
    }

    private static int siToTime(String si) {
        if (si == null || si.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(si, 10);
        }
    }

    private static long[] convertToLongTime(String timeSlotString) {
        long[] timeSlot = null;
        if (timeSlotString != null) {
            String[] timeArray = timeSlotString.split(",", 2);
            if (timeArray.length == 2) {
                timeSlot = new long[2];
                for (int i = 0; i < 2; i++) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    try {
                        Date date = dateFormat.parse(timeArray[i]);
                        timeSlot[i] = date.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
        return timeSlot;
    }
}
