package com.mediatek.rcs.contacts.util;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity that requests permissions needed for VCardViewActivity.
 */
public class RequestPermissionsActivity extends Activity {
    private static final String TAG = RequestPermissionsActivity.class.getSimpleName();
    private static final String PREVIOUS_ACTIVITY_INTENT = "previous_intent";

    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 1;
    private static String[] sPermissions = new String[]{
            permission.READ_CONTACTS,
            permission.WRITE_CONTACTS,
    };

    private Intent mPreviousActivityIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreviousActivityIntent = (Intent) getIntent().getExtras().get(PREVIOUS_ACTIVITY_INTENT);
        requestPermissions();
    }

    /**
     * start permission activity when target activity read or write contacts.
     * @param activity Activity
     * @return true or false
     */
    public static boolean startPermissionActivity(Activity activity) {
        if (!hasPermissions(activity)) {
            final Intent intent = new Intent(activity,
                    RequestPermissionsActivity.class);
            intent.putExtra(PREVIOUS_ACTIVITY_INTENT, activity.getIntent());
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        if (isAllGranted(grantResults)) {
            mPreviousActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(mPreviousActivityIntent);
            finish();
            overridePendingTransition(0, 0);
        } else {
            finish();
        }
    }

    private boolean isAllGranted(int[] grantResult) {
        for (int result : grantResult) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        Log.d(TAG, "requestPermissions");
        requestPermissions(sPermissions, PERMISSIONS_REQUEST_ALL_PERMISSIONS);
    }

    /**
     * check whether has permissions of reading or writing contacts in table.
     * @param context Context
     * @return true or false
     */
    public static boolean hasPermissions(Context context) {
        Log.d(TAG, "hasPermission");
        for (String permission : sPermissions) {
            if (context.checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}