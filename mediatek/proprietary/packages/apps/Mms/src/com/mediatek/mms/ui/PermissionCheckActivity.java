package com.mediatek.mms.ui;

import com.android.mms.MmsApp;
import com.mediatek.mms.util.PermissionCheckUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PermissionCheckActivity extends Activity {
    private static final String TAG = "PermissionCheckActivity";
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " onCreate");
        final String[] missingArray
                = getIntent().getStringArrayExtra(PermissionCheckUtil.MISSING_PERMISSIONS);
        requestPermissions(missingArray, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                int[] grantResults) {
        finish();
        PermissionCheckUtil.setPermissionActivityCount(false);
        Log.d(TAG, " onRequestPermissionsResult Activity Count: "
                + PermissionCheckUtil.sPermissionsActivityStarted);

        if (PermissionCheckUtil.onRequestPermissionsResult(
                this, requestCode, permissions, grantResults, true)) {
            MmsApp.getApplication().onRequestPermissionsResult();
            Intent previousActivityIntent
                    = (Intent) getIntent().getExtras().get(
                            PermissionCheckUtil.PREVIOUS_ACTIVITY_INTENT);
            startActivity(previousActivityIntent);
        }
    }
}
