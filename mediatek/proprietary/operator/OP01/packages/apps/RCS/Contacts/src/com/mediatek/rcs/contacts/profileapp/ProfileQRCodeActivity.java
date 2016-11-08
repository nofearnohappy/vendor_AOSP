package com.mediatek.rcs.contacts.profileapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.contacts.R;

/**
 * ProfileQRCodeActivity
 */
public class ProfileQRCodeActivity extends Activity
        implements ProfileManager.ProfileManagerListener {

    private ProfileInfo mProfile;
    private static String BUSINESS_SETTING = "business_info";
    private static int MENU_SETTING = Menu.FIRST;
    private static String TAG = "ProfileApp: ProfileQRCodeActivity";
    ProfileManager mProfileMgr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_qr_code);
        mProfileMgr = ProfileManager.getInstance(this);
        mProfileMgr.registerProfileManagerListener(this);
        mProfile = mProfileMgr.getMyProfileFromLocal();
        mProfileMgr.getProfileQRCodeFromLocal();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        updateQRCodeView();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProfileMgr.unregisterProfileManagerListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, MENU_SETTING, 0,
                getString(R.string.profile_business_info));
        item.setCheckable(true);
        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        Boolean checked = settings.getBoolean(BUSINESS_SETTING, false);
        Log.d(TAG, "onCreateOptionsMenu checked = " + checked);
        item.setChecked(checked);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == MENU_SETTING) {
            updateBusinessCheckState(!item.isChecked());
            item.setChecked(!item.isChecked());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update profile qr code view:
     */
    private void updateQRCodeView() {
        ImageView portrait = (ImageView)findViewById(R.id.portrait);
        TextView name = (TextView)findViewById(R.id.name);
        ImageView qrcode = (ImageView)findViewById(R.id.qrcode);
        ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
        /* First update qr code */
        if (mProfile.qrcode != null) {
            Bitmap qrmap;
            qrmap = BitmapFactory
                .decodeByteArray(mProfile.qrcode, 0, mProfile.qrcode.length);
            qrcode.setImageBitmap(qrmap);
            progress.setVisibility(View.GONE);
        } else {
            progress.setVisibility(View.VISIBLE);
        }
        /* Second update photo */
        if (mProfile.photo == null) {
            portrait.setImageResource(R.drawable.ic_contact_picture_holo_light);
        } else {
            Bitmap map;
            if (ProfilePhotoUtils.isGifFormatStream(mProfile.photo)) {
                map = ProfilePhotoUtils.getGifFrameBitmap(mProfile.photo, 0);
            } else {
                map = BitmapFactory
                    .decodeByteArray(mProfile.photo, 0, mProfile.photo.length);
            }
            portrait.setImageBitmap(map);
        }
        /* Third update name */
        name.setText(mProfile.getName());

    }

    /**
     * Update profile qr code mode status:
     * @param checked:
     */
    private void updateBusinessCheckState(boolean checked) {
        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        settings.edit().putBoolean(BUSINESS_SETTING, checked).apply();
        int mode = checked ? 1 : 0;
        mProfileMgr.updateProfileQRCodeModeToServer(mode);
        if (checked && !isHasBusinessInfo()) {
            Toast.makeText(this,
                    R.string.profile_please_add_business_info,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Check if profile has business infomation.
     */
    private boolean isHasBusinessInfo() {
        return !checkIfNone(ProfileInfo.getContentByKey(ProfileInfo.COMPANY))
                || !checkIfNone(ProfileInfo.getContentByKey(ProfileInfo.COMPANY_ADDR))
                || !checkIfNone(ProfileInfo.getContentByKey(ProfileInfo.COMPANY_FAX))
                || !checkIfNone(ProfileInfo.getContentByKey(ProfileInfo.TITLE))
                || !checkIfNone(ProfileInfo.getContentByKey(ProfileInfo.COMPANY_TEL));
    }

    /**
     * Check if str is null or "".
     * @param str: string to be checked.
     * @return boolean. none return true, other return false.
     */
    private boolean checkIfNone(String str) {
        return (str == null) || (str.equals(""));
    }

    @Override
    public void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile) {

    }

    /**
     * Override ProfileManagerListener. called when contact portrait updated.
     * @param flag :  update flag
     * @param number: contact number.
     * @param icon:   contact portrait
     */
    @Override
    public void onContactIconGotten(int flag, String number, byte[]icon) {

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code call back.
     * @param result:
     * @param mode:
     */
    @Override
    public void onGetProfileQRCode (final int result, int mode){
        Log.d(TAG, "onGetProfileQRCode: result = " + result + " mode = " + mode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result == 0) {
                    invalidateOptionsMenu();
                }
                updateQRCodeView();

            }
        });

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code mode call back.
     * @param result:
     * @param mode:
     */
    @Override
    public void onUpdateProfileQRCodeMode (final int result, int mode){
        Log.d(TAG, "onUpdateProfileQRCodeMode: result = " + result + " mode = " + mode);
        boolean qrmode;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int resId = -1;
                if (result != 0) {
                    invalidateOptionsMenu();
                    resId = R.string.profile_set_qr_mode_fail;
                } else {
                    resId = R.string.profile_set_qr_mode_sucess;
                }
                Toast.makeText(ProfileQRCodeActivity.this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
