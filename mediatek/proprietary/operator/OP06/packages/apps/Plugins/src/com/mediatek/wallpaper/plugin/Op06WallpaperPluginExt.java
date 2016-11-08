package com.mediatek.wallpaper.plugin;

import android.content.Context;
//import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.OperationCanceledException;

//import android.database.ContentObserver;
//import android.os.Handler;

import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.regionalphone.RegionalPhone;

import com.mediatek.common.wallpaper.IWallpaperPlugin;
import com.mediatek.op06.plugin.R;

@PluginImpl(interfaceName="com.mediatek.common.wallpaper.IWallpaperPlugin")
public class Op06WallpaperPluginExt /*extends ContextWrapper */implements IWallpaperPlugin {

    private static final String TAG = "Op06WallpaperPluginExt";
    private static final String MCC_MNC_TIMESTAMP = RegionalPhone.WALLPAPER.MCC_MNC_TIMESTAMP; //"mcc_mnc_timestamp";
    private static final String IMAGE_FILE_NAME = RegionalPhone.WALLPAPER.IMAGE_FILE_NAME; //"fileName";
    private Uri mUri = RegionalPhone.WALLPAPER_URI; //Uri.parse("content://com.mediatek.provider.regionalphone/wallpaper");
    //private Cursor cr = null;
    private Context mContextWallpaperMgr = null;
    private Context mContext = null;

    //private RegionalPhoneContentObserver mObserver = new RegionalPhoneContentObserver(new Handler());

    public Op06WallpaperPluginExt(Context context) {
        //super(context);
        mContext = context;
        Log.d("@M_" + TAG, "Op06WallpaperPluginExt: call to constructor");
        if (context == null) {
            Log.d("@M_" + TAG, "Op06WallpaperPluginExt: cntx null");
        } else {
            Log.d("@M_" + TAG, "Op06WallpaperPluginExt: input parameter context valid");
        }
        /*mContext = getBaseContext();
        if(mContext == null){
            Log.d("@M_" + TAG, "mContext null 2");
        }else{
            Log.d("@M_" + TAG, "getBaseContext returned valid context");
        }*/
    }

    public Resources getPluginResources(Context context) {
        mContextWallpaperMgr = context;
        Cursor cr = null;
        try {
            cr = mContext.getContentResolver().query(
                        mUri, null, null, null, null);
            if (cr == null) {
                log("getPluginResources: null cr");
                return null;
            }
            if (cr.getCount() == 0) {
                log("getPluginResources: invalid cr");
                //mContext.getContentResolver().registerContentObserver(mUri, true, mObserver);
                //log("getPluginResources: registered RegionalPhoneContentObserver");
                cr.close();
                log("getPluginResources: cr closed");
                return null;
            }
            cr.close();
            return mContext.getResources();
        } catch (OperationCanceledException e) {
            Log.e(TAG, "getPluginResources exception: ", e);
            if (cr != null) {
                cr.close();
            }
            return null;
        }
    }

    public int getPluginDefaultImage() {
        String saved_mcc_mnc_timestamp = null;
        String saved_image_file_name = null;
        String mcc_mnc_timestamp = null;
        String image_file_name = null;
        int imageID = 0;
        SharedPreferences preferences;
        Cursor cr = null;
        try {
            cr = mContext.getContentResolver().query(
                        mUri, null, null, null, null);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "getPluginDefaultImage exception: ", e);
            if (cr != null) {
                cr.close();
                log("getPluginDefaultImage: cr closed");
            }
            return R.drawable.default_wallpaper;
        }
        if (cr == null) {
            log("getPluginDefaultImage: null cr");
            return R.drawable.default_wallpaper;
        }
        if (cr.moveToNext()) { // move to a valid row from -1 position
            // get the mcc_mnc_timestamp from the RM
            mcc_mnc_timestamp = cr.getString(cr.getColumnIndex(MCC_MNC_TIMESTAMP));    // TODO handle the exception or error
            log("getPluginDefaultImage: mcc_mnc_timestamp value retreived from the RM database: " + mcc_mnc_timestamp);

            //preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            preferences = mContextWallpaperMgr.getSharedPreferences("wallpaperPlugin", Context.MODE_PRIVATE); //MODE_WORLD_WRITEABLE);
            try {
                saved_mcc_mnc_timestamp = preferences.getString(MCC_MNC_TIMESTAMP, null);
                log("getPluginDefaultImage: mcc_mnc_timestamp retreived from the pref is " + saved_mcc_mnc_timestamp);
                saved_image_file_name = preferences.getString(IMAGE_FILE_NAME, "");
                log("image_file_name retreived from the pref is " + saved_image_file_name);
            } catch (ClassCastException e) {
                Log.e(TAG, "getPluginDefaultImage: exception while reading prref: ", e);
                if (cr != null) {
                cr.close();
                log("getPluginDefaultImage: cr closed");
                }
                return R.drawable.default_wallpaper;
                //ignore
            }
            if (mcc_mnc_timestamp.equals(saved_mcc_mnc_timestamp)) {
                image_file_name = saved_image_file_name;
            } else {
                // get the image file name from the RM
                image_file_name = cr.getString(cr.getColumnIndex(IMAGE_FILE_NAME));   // TODO handle the exception or error
                log("getPluginDefaultImage: image_file_name retreived from the RM database: " + image_file_name);
                // update the shared prefernces
                SharedPreferences.Editor edit = preferences.edit();
                edit.putString(MCC_MNC_TIMESTAMP, mcc_mnc_timestamp);
                edit.putString(IMAGE_FILE_NAME, image_file_name);
                if (!(edit.commit())) {
                    log("getPluginDefaultImage: error while writing to the default shared preferences");
                    //ignore
                }
            }
            if (image_file_name.equals("wallpaper_1.jpg")) {
                imageID = R.drawable.wallpaper_1;
                log("getPluginDefaultImage: image_file_name used is wallpaper_1");
            }
            else {
                imageID = R.drawable.default_wallpaper;
                log("getPluginDefaultImage: image_file_name used is default_wallpaper");
            } // else part is only for UT/IT purpose, will be removed later
            cr.close();
            log("getPluginDefaultImage: cr closed");
            return imageID;
            }
        else {
            log("getPluginDefaultImage: cr.moveToNext error");
            cr.close();
            log("getPluginDefaultImage: cr closed");
            return R.drawable.default_wallpaper;    // only for testing purpose, will be removed later
        }
    }
    /*
    private class RegionalPhoneContentObserver extends ContentObserver {

        public RegionalPhoneContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            WallpaperManager wm = WallpaperManager.getInstance(mContext);
            try{
                log("onChange: clearing wallpaper");
                wm.clear();
                log("onChange: wallpaper cleared");
            }catch(IOException e){
                Log.e(TAG, "IOException exception while clear wallpaper: ", e);
                //ignore
            }
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }
    */
    public void log(String text) {
        Log.d("@M_" + TAG, text);
    }

}
