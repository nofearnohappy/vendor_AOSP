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

package com.mediatek.datatransfer;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Window;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.datatransfer.utils.NotifyManager;
import com.mediatek.datatransfer.utils.SDCardUtils;
import com.mediatek.datatransfer.utils.Constants.DialogID;

//import com.mediatek.telephony.SimInfoManager;
//import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

public class MainActivity extends Activity implements BackupTabFragment.BackupFinishCallBack {
    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/MainActivity";
    public static final String ACTION_WHERE = "action_where";
    public static final String ACTION_BACKUP = "action_backup";
    public static final String ACTION_RESTORE = "ction_restore";

    private CustomViewPager viewPager;
    private TabsAdapter mTabsAdapter;
    private Fragment mBackupFragment;
    public Fragment mRestoreFragment;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLogger.logD(CLASS_TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        setProgressBarIndeterminateVisibility(false);
        NotifyManager.getInstance(this).clearNotification();
        viewPager = (CustomViewPager) findViewById(R.id.pager);

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        initFragments();
        mTabsAdapter = new TabsAdapter(this, viewPager);
        mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.backup)),
                mBackupFragment, null);
        mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.restore)),
                mRestoreFragment, null);
//        viewPager.setAdapter(mTabsAdapter);
    }



    private void initFragments() {
        // TODO Auto-generated method stub
        if (mBackupFragment == null) {
            mBackupFragment = new BackupTabFragment();
        }
        if (mRestoreFragment == null) {
            mRestoreFragment = new RestoreTabFragment();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        MyLogger.logD(CLASS_TAG, "onResume");
    }


    protected void onDestroy() {
        super.onDestroy();
        MyLogger.logD(CLASS_TAG, "onDestroy");
    }


    protected Dialog onCreateDialog(int id, Bundle args) {

        MyLogger.logI(CLASS_TAG, "oncreateDialog, id = " + id);
        switch(id) {
        case DialogID.DLG_DELETE_AND_WAIT: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setCancelable(false);
                dialog.setMessage(getString(R.string.delete_please_wait));
                dialog.setIndeterminate(true);
                return dialog;
            }

        case DialogID.DLG_LOADING: {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.loading_please_wait));
            dialog.setIndeterminate(true);
            return dialog;
        }
        case DialogID.DLG_NO_SDCARD: {
            return new AlertDialog.Builder(this)
            .setIconAttribute(android.R.attr.alertDialogIcon).setTitle(R.string.notice)
            .setMessage(SDCardUtils.getSDStatueMessage(this))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    MainActivity.this.finish();
                }
            }).create();
        }
        }
        return null;
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements
            ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<Fragment> mTabs = new ArrayList<Fragment>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Fragment fragment, Bundle args) {
            tab.setTag(fragment);
            tab.setTabListener(this);
            mTabs.add(fragment);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mTabs.get(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
//          if(currentTab instanceof RestoreTabFragment){
//              ((RestoreTabFragment)currentTab).startScanFiles();
//          }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

    @Override
    public void onBackupFinish() {
        // TODO Auto-generated method stub
        MyLogger.logD(CLASS_TAG, "[onBackupFinish] called!");
        ((RestoreTabFragment) mRestoreFragment).startScanFiles();
    }
}
