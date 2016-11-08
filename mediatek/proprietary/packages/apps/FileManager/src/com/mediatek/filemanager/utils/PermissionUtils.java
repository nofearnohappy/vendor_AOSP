package com.mediatek.filemanager.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

public class PermissionUtils {
    private static final String TAG = "PermissionUtil";
    public static boolean hasStorageWritePermission(Context ctx) {
        return (ctx.checkSelfPermission(Manifest.permission.
                WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasStorageReadPermission(Context ctx) {
        return (ctx.checkSelfPermission(Manifest.permission.
                READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestPermission(Activity ctx, String permission, int requestCode){
        ctx.requestPermissions(new String[]{permission}, requestCode);
    }

    public static boolean showWriteRational(Activity ctx){
        return ctx.shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static boolean showReadRational(Activity ctx){
        return ctx.shouldShowRequestPermissionRationale(
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }
}
