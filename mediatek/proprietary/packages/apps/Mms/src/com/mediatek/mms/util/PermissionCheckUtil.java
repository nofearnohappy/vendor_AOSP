package com.mediatek.mms.util;

import java.util.ArrayList;

import com.android.mms.ui.MessageUtils;
import com.mediatek.mms.ui.PermissionCheckActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PermissionCheckUtil {
    private static final String TAG = "PermissionCheckUtil";

    private static final String[] ALL_PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECEIVE_SMS};

    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECEIVE_SMS};

    public static final String PREVIOUS_ACTIVITY_INTENT = "previous_intent";
    public static final String MISSING_PERMISSIONS = "missing_permissions";
    public static int sPermissionsActivityStarted = 0;

    public static boolean requestAllPermissions(Activity activity) {
        return requestPermissions(activity, ALL_PERMISSIONS);
    }

    public static boolean requestRequiredPermissions(Activity activity) {
        return requestPermissions(activity, REQUIRED_PERMISSIONS);
    }

    public static boolean requestPermissions(Activity activity, String[] permissions) {
        ArrayList<String> missingList = getMissingPermissions(activity, permissions);
        return requestPermissions(activity, missingList);
    }

    public static ArrayList<String> getMissingPermissions(
            Activity activity, String[] requiredPermissions) {
        final ArrayList<String> missingList = new ArrayList<String>();

        for (int i = 0; i < requiredPermissions.length; i++) {
            if (!hasPermission(activity, requiredPermissions[i])) {
                missingList.add(requiredPermissions[i]);
            }
        }

        return missingList;
    }

    public static boolean hasNeverGrantedPermissions(
            Activity activity, ArrayList<String> permissionList) {
        boolean isNeverGranted = false;
        for (int i = 0; i < permissionList.size(); i++) {
            if (isNeverGrantedPermission(activity, permissionList.get(i))) {
                isNeverGranted = true;
                Log.d(TAG, " hasNeverGrantedPermissions "
                        + permissionList.get(i) + " is always denied");
                break;
            }
        }

        return isNeverGranted;
    }

    public static boolean isNeverGrantedPermission(Activity activity, String permission) {
        return !activity.shouldShowRequestPermissionRationale(permission);
    }

    public static boolean requestPermissions(Activity activity, ArrayList<String> missingList) {
        if (missingList.size() == 0) {
            Log.d(TAG, " requestPermissions all permissions granted");
            return false;
        }

        final String[] missingArray = new String[missingList.size()];
        missingList.toArray(missingArray);

        Intent intentPermissions = new Intent(activity, PermissionCheckActivity.class);
        intentPermissions.putExtra(PREVIOUS_ACTIVITY_INTENT, activity.getIntent());
        intentPermissions.putExtra(MISSING_PERMISSIONS, missingArray);

        setPermissionActivityCount(true);
        activity.startActivity(intentPermissions);
        activity.finish();

        return true;
    }

    public static boolean checkAllPermissions(Context context) {
        return checkPermissions(context, ALL_PERMISSIONS);
    }

    public static boolean checkRequiredPermissions(Context context) {
        return checkPermissions(context, REQUIRED_PERMISSIONS);
    }

    public static boolean checkPermissions(Context context, String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            if (!hasPermission(context, permissions[i])) {
                Log.d(TAG, "checkPermissions false : " + permissions[i]);
                return false;
            }
        }

        return true;
    }

    public static boolean onRequestPermissionsResult(
            Activity activity, int requestCode, String[] permissions,
            int[] grantResults, boolean needFinish) {
        boolean result = true;
        boolean hasNeverGranted = false;
        Log.d(TAG, "onRequestPermissionsResult grantResults: "
                + (grantResults == null ? 0 : grantResults.length));

        if (grantResults == null || grantResults.length == 0) {
            Log.d(TAG, "onRequestPermissionsResult return : " + result);
            return result;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (!hasPermission(activity, permissions[i])) {
                Log.d(TAG, "onRequestPermissionsResult return false");
                result = false;
                // Show toast
                if (isRequiredPermission(permissions[i])
                        || isNeverGrantedPermission(activity, permissions[i])) {
                    showNoPermissionsToast(activity);
                    if (needFinish) {
                        activity.finish();
                    }
                    return result;
                }
            }
        }

        Log.d(TAG, "onRequestPermissionsResult return : " + result);
        return result;
    }

    public static boolean hasPermission(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasSMSPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void showNoPermissionsToast(Context context) {
        Toast.makeText(context, com.mediatek.internal.R.string.denied_required_permission,
                Toast.LENGTH_LONG).show();
    }

    public static boolean isPermissionChecking() {
        Log.d(TAG, " isPermissionChecking Activity Count: " + sPermissionsActivityStarted);
        return sPermissionsActivityStarted > 0;
    }

    /*
     * It means permission activity would be finished if startActivity is false.
     */
    public static void setPermissionActivityCount(boolean startActivity) {
        if (startActivity) {
            if (sPermissionsActivityStarted < 0) {
                sPermissionsActivityStarted = 0;
            }
            sPermissionsActivityStarted++;
        } else {
            sPermissionsActivityStarted--;
            if (sPermissionsActivityStarted < 0) {
                sPermissionsActivityStarted = 0;
            }
        }
        Log.d(TAG, "setPermissionActivityCount: "
                + sPermissionsActivityStarted + ", start: " + startActivity);
    }

    public static boolean isRequiredPermission(String permission) {
        for (int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
            if (REQUIRED_PERMISSIONS[i].equals(permission)) {
                Log.d(TAG, "isRequiredPermission: " + permission);
                return true;
            }
        }
        return false;
    }
}
