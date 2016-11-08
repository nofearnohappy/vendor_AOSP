/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.mediatekdm;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService.DmBinder;
import com.mediatek.mediatekdm.util.DialogFactory;

/**
 * This activity provides the user interface of network initiated (NI for short) user interaction,
 * including notification (NIA) and alert. The invoker should specify the interaction type in the
 * intent as an extra with key DmNIInteractionActivity.EXTRA_KEY_TYPE.
 */
public class DmNIInteractionActivity extends Activity {
    public static final String EXTRA_KEY_TYPE = "Type";

    private int mItem = 0;
    private boolean[] mCheckedItem;
    private Integer mUiVisible = null;
    private Integer mUiInteract = null;
    private DmBinder mBinder = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String opName = DmConfig.getInstance().getCustomizedOperator();
        if (opName != null && opName.equalsIgnoreCase("cu")) {
            mUiVisible = R.string.usermode_visible_cu;
            mUiInteract = R.string.usermode_interact_cu;
        } else if (opName != null && opName.equalsIgnoreCase("cmcc")) {
            mUiVisible = R.string.usermode_visible_cmcc;
            mUiInteract = R.string.usermode_interact_cmcc;
        } else {
            mUiVisible = R.string.usermode_visible_cu;
            mUiInteract = R.string.usermode_interact_cu;
        }
        registerReceiver(mBroadcastReceiver, new IntentFilter(DmConst.IntentAction.DM_CLOSE_DIALOG));

        bindService();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (mBinder == null) {
            return null;
        }
        switch (id) {
            case DmConst.NotificationInteractionType.TYPE_ALERT_1100:
                return DialogFactory.newAlert(this).setTitle(R.string.app_name)
                        .setMessage(DmInfoMsg.sViewContext.displayText)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_ALERT_1100, onClick NeutralButton");
                                DmInfoMsg.sObserver.notifyInfoMsgClosed();
                                mBinder.clearDmNotification();
                                finish();
                            }
                        }).create();
            case DmConst.NotificationInteractionType.TYPE_ALERT_1101:
                Log.i(TAG.MMI, "displayText: " + mBinder.getAlertConfirmContext().displayText);
                return DialogFactory.newAlert(this).setTitle(R.string.app_name)
                        .setMessage(mBinder.getAlertConfirmContext().displayText)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_ALERT_1101, onClick PositiveButton");
                                sendAlertResponse(true);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_ALERT_1101, onClick NegativeButton");
                                sendAlertResponse(false);
                                finish();
                            }
                        }).create();
            case DmConst.NotificationInteractionType.TYPE_ALERT_1102:
                Log.w(TAG.MMI, "TYPE_ALERT_1102 is not implemented");
                return null;
            case DmConst.NotificationInteractionType.TYPE_ALERT_1103:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.app_name)
                        .setSingleChoiceItems(DmChoiceList.sItems, DmChoiceList.sSelected,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.v(TAG.MMI,
                                                "TYPE_ALERT_1103, onClick SingleChoiceItems, mItem is "
                                                        + whichButton);
                                        mItem = whichButton;
                                    }
                                })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI,
                                        "TYPE_ALERT_1103, onClick SingleChoice PositiveButton");
                                DmChoiceList.sObserver.notifyChoicelistSelection(mItem);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI,
                                        "TYPE_ALERT_1103, onClick SingleChoice NegativeButton");
                                DmChoiceList.sObserver.notifyCancelEvent();
                                finish();
                            }
                        }).create();
            case DmConst.NotificationInteractionType.TYPE_ALERT_1104:
                mCheckedItem = new boolean[DmChoiceList.sItems.length];
                for (int i = 0; i < DmChoiceList.sItems.length; i++) {
                    mCheckedItem[i] = ((DmChoiceList.sSelected & (1 << i)) > 0) ? true : false;
                }
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.app_name)
                        .setMultiChoiceItems(DmChoiceList.sItems, mCheckedItem,
                                new OnMultiChoiceClickListener() {
                                    public void
                                            onClick(DialogInterface arg0, int arg1, boolean arg2) {
                                        Log.v(TAG.MMI,
                                                "TYPE_ALERT_1103, onClick MultiChoiceItems, mItem "
                                                        + arg1 + " is " + arg2);
                                        mCheckedItem[arg1] = arg2;
                                    }
                                })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI,
                                        "TYPE_ALERT_1103, onClick MultiChoice PositiveButton");
                                int newSelection = 1;
                                for (int i = 0; i < DmChoiceList.sItems.length; i++) {
                                    if (mCheckedItem[i]) {
                                        newSelection |= newSelection << i;
                                    }
                                }
                                DmChoiceList.sObserver.notifyChoicelistSelection(newSelection);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI,
                                        "TYPE_ALERT_1103, onClick MultiChoice NegativeButton");
                                DmChoiceList.sObserver.notifyCancelEvent();
                                finish();
                            }
                        }).create();
            case DmConst.NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE:
                return DialogFactory.newAlert(this).setTitle(R.string.app_name)
                        .setMessage(mUiVisible)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_NOTIFICATION_VISIBLE, onClick NeutralButton");
                                mBinder.clearDmNotification();
                                finish();
                            }
                        }).create();
            case DmConst.NotificationInteractionType.TYPE_NOTIFICATION_INTERACT:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.app_name)
                        .setMessage(mUiInteract)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_NOTIFICATION_INTERACT, onClick PositiveButton");
                                sendNotificationResponse(true);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(TAG.MMI, "TYPE_NOTIFICATION_INTERACT, onClick NegativeButton");
                                sendNotificationResponse(false);
                                finish();
                            }
                        }).create();
            default:
                Log.w(TAG.MMI, "Invalid NotificationInteractionType " + id);
                break;
        }
        return null;
    }

    private void sendNotificationResponse(boolean confirmed) {
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction(DmConst.IntentAction.DM_NOTIFICATION_RESPONSE);
        serviceIntent.putExtra("response", confirmed);
        startService(serviceIntent);
    }

    private void sendAlertResponse(boolean confirmed) {
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction(DmConst.IntentAction.DM_ALERT_RESPONSE);
        serviceIntent.putExtra("response", confirmed);
        startService(serviceIntent);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DmNIInteractionActivity.this.finish();
        }
    };

    private void bindService() {
        Log.d(TAG.MMI, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(DmService.BIND_SERVICE);
        if (!bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new Error("Failed to bind to service.");
        }
        Log.d(TAG.MMI, "-bindService()");
    };

    @SuppressWarnings("deprecation")
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG.MMI, "onServiceConnected");
            mBinder = (DmBinder) binder;
            Intent intent = getIntent();
            int type = intent.getIntExtra(EXTRA_KEY_TYPE, 0);
            Log.d(TAG.MMI, "DmNIInteractionActivity type " + type);
            showDialog(type);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG.MMI, "onServiceDisconnected");
            mBinder = null;
        }
    };

}
