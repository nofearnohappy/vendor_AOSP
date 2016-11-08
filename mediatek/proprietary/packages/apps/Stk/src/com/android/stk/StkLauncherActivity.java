/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ITelephony;

import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import android.view.Gravity;
import android.widget.Toast;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.RemoteException;

/**
 * Launcher class. Serve as the app's MAIN activity, send an intent to the
 * StkAppService and finish.
 *
 */
public class StkLauncherActivity extends ListActivity {
    private TextView mTitleTextView = null;
    private ImageView mTitleIconView = null;
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    private ArrayList<Item> mStkMenuList = null;
    private int mSingleSimId = -1;
    private Context mContext = null;
    private TelephonyManager mTm = null;
    private Bitmap mBitMap = null;
    private boolean mAcceptUsersInput = true;
    private boolean mStkMainVisible = false;
    private StkAppService appService = StkAppService.getInstance();
    private SubscriptionManager mSubscriptionManager = null;
    // message id for accepting user input.
    static final int MSG_ID_ACCEPT_USER_INPUT = 1;

    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CatLog.d(LOG_TAG, "mSimReceiver action: " + action);

            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                CatLog.d(LOG_TAG, "simState[" + simState + "]");
                CatLog.d(LOG_TAG, "slotId[" + slotId + "]");
                if ((IccCardConstants.INTENT_VALUE_ICC_ABSENT).equals(simState) ||
                    (IccCardConstants.INTENT_VALUE_ICC_LOADED).equals(simState)) {
                    int itemSize = addStkMenuListItems();
                    if (itemSize == 0) {
                        finish();
                    } else if (itemSize == 1) {
                        if (true == mStkMainVisible) {
                            launchSTKMainMenu(mSingleSimId);
                        }
                        finish();
                    }
                }
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                addStkMenuListItems();
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE)) {
                // L-MR1: No need to handle ACTION_SUBINFO_CONTENT_CHANGE for DISPLAY_NAME,
                //since there is no UI interface for end user to change this field.
                //String updateRecord = (String)intent.getExtra(TelephonyIntents.EXTRA_COLUMN_NAME);
                //if (updateRecord != null && updateRecord.equals(SubscriptionManager.DISPLAY_NAME)) {
                //    CatLog.d(LOG_TAG, "SubInfo content changed(DISPLAY_NAME)");
                //    addStkMenuListItems();
                //}
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        CatLog.d(LOG_TAG, "onCreate+");
        //To disable 3 dots(overflow) menu.
        getApplicationInfo().targetSdkVersion = 14;
        mContext = getBaseContext();
        mTm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        StkAppService.mIsLauncherAcceptInput = true;
        mSubscriptionManager = SubscriptionManager.from(mContext);

        IntentFilter mSIMStateChangeFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        registerReceiver(mSimReceiver, mSIMStateChangeFilter);

        Intent newIntent = getIntent();
        if (null == newIntent) {
            CatLog.d(LOG_TAG, "Intent is null!");
            finish();
            return;
        }
        String strSourceKey = newIntent.getStringExtra(StkAppService.STK_SOURCE_KEY);
        if (strSourceKey != null) {
            if (!StkMain.isValidStkSourceKey(strSourceKey)) {
                CatLog.d(LOG_TAG, "Invalid Stk source key.");
                finish();
                return;
            }
        } else {
            CatLog.d(LOG_TAG, "Stk source key is null.");
            finish();
            return;
        }
        //Check if needs to show the meun list.
        if (isShowSTKListMenu()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.stk_menu_list);
            mTitleTextView = (TextView) findViewById(R.id.title_text);
            mTitleIconView = (ImageView) findViewById(R.id.title_icon);
            mTitleTextView.setText(R.string.app_name);
            mBitMap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher_sim_toolkit);
        } else {
            //setTheme(android.R.style.Theme_NoDisplay);
            //launch stk menu activity for the SIM.
            if (mSingleSimId < 0) {
                showTextToast(mContext, R.string.no_sim_card_inserted);
                finish();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        CatLog.d(LOG_TAG, "mAcceptUsersInput: " +
                StkAppService.mIsLauncherAcceptInput);//SystemProperties.get(StkAppService.PROPERTY_LAUNCHER_ACCEPT_INPUT));
        if (!StkAppService.mIsLauncherAcceptInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return;
        }
        int simCount = TelephonyManager.from(mContext).getSimCount();
        Item item = getSelectedItem(position);
        if (item == null) {
            CatLog.d(LOG_TAG, "Item is null");
            return;
        }
        CatLog.d(LOG_TAG, "launch stk menu id: " + item.id);
        if (item.id >= PhoneConstants.SIM_ID_1 && item.id < simCount) {
            StkAppService.mIsLauncherAcceptInput = false;//SystemProperties.set(StkAppService.PROPERTY_LAUNCHER_ACCEPT_INPUT, "0");
            launchSTKMainMenu(item.id);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CatLog.d(LOG_TAG, "mAcceptUsersInput: " +
                StkAppService.mIsLauncherAcceptInput);//SystemProperties.get(StkAppService.PROPERTY_LAUNCHER_ACCEPT_INPUT));
        if (!StkAppService.mIsLauncherAcceptInput) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                CatLog.d(LOG_TAG, "KEYCODE_BACK.");
                StkAppService.mIsLauncherAcceptInput = false;//SystemProperties.set(StkAppService.PROPERTY_LAUNCHER_ACCEPT_INPUT, "0");
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
        CatLog.d(LOG_TAG, "onResume");
        mStkMainVisible = true;
        StkAppService.mIsLauncherAcceptInput = true;//SystemProperties.set(StkAppService.PROPERTY_LAUNCHER_ACCEPT_INPUT, "1");
        int itemSize = addStkMenuListItems();
        if (itemSize == 0) {
            CatLog.d(LOG_TAG, "item size = 0 so finish.");
            showTextToast(mContext, R.string.no_sim_card_inserted);
            finish();
        } else if (itemSize == 1) {
            launchSTKMainMenu(mSingleSimId);
            finish();
        } else {
            CatLog.d(LOG_TAG, "resume to show multiple stk list.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause");
        mStkMainVisible = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSimReceiver);
        CatLog.d(LOG_TAG, "onDestroy");
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (mStkMenuList != null) {
            try {
                item = mStkMenuList.get(position);
            } catch (IndexOutOfBoundsException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "IOOBE Invalid menu");
                }
            } catch (NullPointerException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "NPE Invalid menu");
                }
            }
        }
        return item;
    }

    private int addStkMenuListItems() {
        String appName = mContext.getResources().getString(R.string.app_name);
        String stkItemName = null;
        int simCount = TelephonyManager.from(mContext).getSimCount();
        mStkMenuList = new ArrayList<Item>();

        CatLog.d(LOG_TAG, "addStkMenuListItems simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            if (mTm.hasIccCard(i)) {
                CatLog.d(LOG_TAG, "SIM " + i + " add to menu.");
                mSingleSimId = i;
                stkItemName = new StringBuilder(appName).append(" ")
                        .append(Integer.toString(i + 1)).toString();

                int subId[] = SubscriptionManager.getSubId(i);
                if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                    SubscriptionInfo info = mSubscriptionManager.getActiveSubscriptionInfo(subId[0]);
                    if (info != null) {
                        stkItemName = info.getDisplayName().toString();
                    } else {
                        CatLog.d(LOG_TAG, "SubscriptionInfo is null.");
                    }
                } else {
                    CatLog.d(LOG_TAG, "sub is null or invalid.");
                }
                CatLog.d(LOG_TAG, "item name: " + stkItemName);
                Item item = new Item(i + 1, stkItemName, mBitMap);
                item.id = i;
                mStkMenuList.add(item);
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        if (mStkMenuList != null && mStkMenuList.size() > 0) {
       	   if (mStkMenuList.size() > 1) {
                StkMenuAdapter adapter = new StkMenuAdapter(mContext,
                        mStkMenuList, null, false);
                // Bind menu list to the new adapter.
                this.setListAdapter(adapter);
       	   }
           return mStkMenuList.size();
        } else {
            CatLog.d(LOG_TAG, "No stk menu item add.");
            return 0;
        }
    }
    private boolean isShowSTKListMenu() {
        int simCount = TelephonyManager.from(mContext).getSimCount();
        int simInsertedCount = 0;
        int insertedSlotId = -1;

        CatLog.d(LOG_TAG, "simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            if (mTm.hasIccCard(i)) {
                CatLog.d(LOG_TAG, "SIM " + i + " is inserted.");
                mSingleSimId = i;
                simInsertedCount++;
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        if (simInsertedCount > 1) {
       	    return true;
       	} else {
       	    //No card or only one card.
            CatLog.d(LOG_TAG, "do not show stk list menu.");
            return false;
       	}
    }
    private void launchSTKMainMenu(int slotId) {
        Bundle args = new Bundle();
        CatLog.d(LOG_TAG, "launchSTKMainMenu.");

        if (!isStkAvailable(slotId)) {
            CatLog.d(LOG_TAG, "Stk " + slotId + " is not available.");
            finish();
            return;
        }
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LAUNCH_APP);
        args.putInt(StkAppService.SLOT_ID
                , PhoneConstants.SIM_ID_1 + slotId);
        startService(new Intent(this, StkAppService.class)
                .putExtras(args));
    }

    private boolean isStkAvailable(int slotId) {
        StkAppService service = StkAppService.getInstance();
        if (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER) {
            CatLog.d(LOG_TAG, "CurrentUser:" + ActivityManager.getCurrentUser() +
            " is not USER_OWNER:" + UserHandle.USER_OWNER + " !!!");
            int UnsupportResId = R.string.lable_sim_not_ready;
            showTextToast(mContext, UnsupportResId);
            return false;
        }
        if (service != null && service.StkQueryAvailable(slotId) !=
                StkAppService.STK_AVAIL_AVAILABLE) {
            CatLog.d(LOG_TAG, "Not available");
            int resId = R.string.lable_sim_not_ready;
            int simState = TelephonyManager.getDefault().getSimState(slotId);

            CatLog.d(LOG_TAG, "Not available simState:" + simState);
            if (true == isOnFlightMode()) {
                resId = R.string.lable_on_flight_mode;
            } else if (TelephonyManager.SIM_STATE_PIN_REQUIRED == simState ||
                    TelephonyManager.SIM_STATE_PUK_REQUIRED == simState ||
                    TelephonyManager.SIM_STATE_NETWORK_LOCKED == simState ||
                    !isRadioOnState(slotId)) {
                CatLog.d(LOG_TAG, "pinLock or not radio on");
            }
            showTextToast(mContext, resId);
            return false;
        }
        return true;
    }
    private boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            CatLog.d(LOG_TAG, "fail to get airlane mode");
            mode = 0;
        }

        CatLog.d(LOG_TAG, "airlane mode is " + mode);
        return (mode != 0);
    }
    boolean isRadioOnState(int slotId) {
        boolean radioOn = true;
        CatLog.d(LOG_TAG, "isRadioOnState check = " + slotId);

        try {
            ITelephony phone = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (phone != null) {
                int subId[] = SubscriptionManager.getSubId(slotId);
                radioOn = phone.isRadioOnForSubscriber(subId[0], mContext.getOpPackageName());
            }
            CatLog.d(LOG_TAG, "isRadioOnState - radio_on[" + radioOn + "]");
        } catch (RemoteException e) {
            e.printStackTrace();
            CatLog.d(LOG_TAG, "isRadioOnState - Exception happen ====");
        }
        return radioOn;
    }
    private void showTextToast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
}
