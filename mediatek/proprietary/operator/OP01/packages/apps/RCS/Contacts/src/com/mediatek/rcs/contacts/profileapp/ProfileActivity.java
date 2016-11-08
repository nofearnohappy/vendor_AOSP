/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.contacts.profileapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cmcc.ccs.profile.ProfileService;
import com.mediatek.rcs.contacts.R;

public class ProfileActivity extends Activity implements ProfileManager.ProfileManagerListener {
    /* Fragments managed by profile activity */
    private ProfileEntryFragment mProfileEntryFragment;
    private ProfileDetailFragment mProfileDetailFragment;
    private ProfileInfo mProfile;
    private ProfileManager mProfileMgr;
    private static final String TAG = "ProfileActivity";

    private boolean mIsProfileLoading = false;
    private ProgressDialog mProgressDialog;

    /* Profile entry listener */
    ProfileEntryFragment.ProfileEntryListener mProfileEntryListener
            = new ProfileEntryFragment.ProfileEntryListener() {
        @Override
        public void onItemClicked(int type, String pkgName, String error) {
            if (type == 0) { /* profile type */
                Intent i = new Intent();
                i.setClassName(getPackageName(), ProfileDetailActivity.class.getName());
                startActivity(i);
            } else if (type == 1) { /* plugin-in type */
                 PackageManager pm = getPackageManager();
                 Intent intent = pm.getLaunchIntentForPackage(pkgName);
                if (intent != null) {
                     startActivity(intent);
                } else {
                     Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate:");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_main);

        /* use fragment manager to manage profile entry , detail and editor */
        FragmentManager fragmentMgr = getFragmentManager();
        FragmentTransaction ftr = fragmentMgr.beginTransaction();
        mProfileEntryFragment = (ProfileEntryFragment)fragmentMgr
                .findFragmentByTag(getString(R.string.tag_fragment_entry));

        if(mProfileEntryFragment == null) {
            mProfileEntryFragment = new ProfileEntryFragment();
            ftr.add(R.id.profile_container, mProfileEntryFragment,
                    getString(R.string.tag_fragment_entry));
        }
        mProfileEntryFragment.registerListener(mProfileEntryListener);
        ftr.show(mProfileEntryFragment);
        ftr.commit();
        fragmentMgr.executePendingTransactions();
        mProfileMgr = ProfileManager.getInstance(getApplicationContext());
        mProfile = mProfileMgr.getMyProfileFromLocal();
        mProfileMgr.registerProfileManagerListener(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(R.string.title_activity_profile_center);
        mProfileMgr.getMyProfileFromServer();
        mIsProfileLoading = true;

    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume: mIsProfileLoading = " + mIsProfileLoading);
        if (mIsProfileLoading) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.profile_loading));
            mProgressDialog.setCancelable(false);
            //mProgressDialog.show();
        }
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsProfileLoading = false;
        mProfileMgr.unregisterProfileManagerListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Press home as up, finish current activity */
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /* Notify profile updating information */
    public void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile) {
        Log.d(TAG, "onProfileInfoUpdated: flag= " + flag + "; operation=" + operation);

        //we dont need progressDialog anymore
        if (operation == ProfileManager.SERVER_RESULT_GET_PROFILE) {
            if (mProgressDialog != null) {
                Log.d(TAG, "onProfileInfoUpdated: dismiss dialog" );
                //mProgressDialog.dismiss();
            }
            mIsProfileLoading = false;
        }
        int resId = -1;
        if (flag == ProfileService.OK || flag == ProfileService.NOUPDATE) {
            if (operation == ProfileManager.SERVER_RESULT_GET_PROFILE) {
                resId = R.string.profile_get_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_GET_PORTRAIT) {
                resId = R.string.profile_get_portrait_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PROFILE) {
                resId = R.string.profile_set_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PORTRAIT) {
                resId = R.string.profile_set_portrait_sucess;
            }
        } else {
            if (operation == ProfileManager.SERVER_RESULT_GET_PROFILE) {
                resId = R.string.profile_get_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_GET_PORTRAIT) {
                resId = R.string.profile_get_portrait_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PROFILE) {
                resId = R.string.profile_set_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PORTRAIT) {
                resId = R.string.profile_set_portrait_fail;
            }
        }
        if (resId > 0) {
            final int id = resId;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ProfileActivity.this, id, Toast.LENGTH_SHORT).show();
                }
            });
        }
        mProfileEntryFragment.updateProfileInfo(profile);
    }

    /* Notify Contact icon update information */
    public void onContactIconGotten(int flag, String number, byte[]icon) {

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code call back.
     * @param result:
     * @param mode:
     */
    public void onGetProfileQRCode (int result, int mode) {
        Log.d(TAG, "onGetProfileQRCode: result = " + result + " mode = " + mode);

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code mode call back.
     * @param result:
     * @param mode:
     */
    public void onUpdateProfileQRCodeMode (int result, int mode) {
        Log.d(TAG, "onUpdateProfileQRCodeMode: result = " + result + " mode = " + mode);

    }
}
