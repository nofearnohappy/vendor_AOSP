package com.mediatek.hotknotbeam;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.hotknot.HotKnotAdapter;

import android.net.Uri;
import android.provider.DocumentsContract;


public class HotKnotBeamReceiver extends BroadcastReceiver {
    static final String TAG = "HotKnotBeamReceiver";
    private static final String EXTRA_HOTKNOT_DATA = "hotknot";
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, HotKnotBeamService.class);
        mContext = context;


        // TODO Auto-generated method stub
        if (intent.getAction().equalsIgnoreCase(WifiManager.WIFI_STATE_CHANGED_ACTION))
        {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_DISABLED);
            Log.d(TAG, "Start/Stop hotknotbeam service on HotKnot enable/disable:" + state);

            if (state == WifiManager.WIFI_STATE_DISABLED) {
                context.stopService(serviceIntent);
            } else if (state == WifiManager.WIFI_STATE_ENABLED) {
                context.startService(serviceIntent);
            }
        } else if (intent.getAction().equalsIgnoreCase(
                    HotKnotAdapter.ACTION_ADAPTER_STATE_CHANGED)) {

            int state = intent.getIntExtra(HotKnotAdapter.EXTRA_ADAPTER_STATE,
                        HotKnotAdapter.STATE_DISABLED);
            Log.d(TAG, "Start/Stop hotknotbeam service on HotKnot enable/disable:" + state);

            if (state == HotKnotAdapter.STATE_DISABLED) {
                context.stopService(serviceIntent);
            } else if (state == HotKnotAdapter.STATE_ENABLED) {
                context.startService(serviceIntent);
            }
        } else if (intent.getAction().equalsIgnoreCase(
                    HotKnotBeamService.HOTKNOTBEAM_START)) {
            context.startService(serviceIntent);
        } else if (intent.getAction().equalsIgnoreCase(
                    HotKnotBeamService.HOTKNOT_DL_COMPLETE)) {

            String intentInfo = intent.getStringExtra(
                            HotKnotBeamService.HOTKNOT_EXTRA_APP_INTENT);
            Uri uri = (Uri) intent.getParcelableExtra(
                            HotKnotBeamService.HOTKNOT_EXTRA_APP_URI);
            String mimetype = intent.getStringExtra(
                            HotKnotBeamService.HOTKNOT_EXTRA_APP_MIMETYPE);
            boolean isCheck = intent.getBooleanExtra(
                            HotKnotBeamService.HOTKNOT_EXTRA_APP_ISCHECK, true);

            Log.i(TAG, "intentInfo:" + intentInfo);
            Log.i(TAG, "uri:" + uri + " mimetype:" + mimetype + ":" + isCheck);

            if (MimeUtilsEx.getFilePathFromUri(uri, mContext) == null) {
                Log.e(TAG, "No file info");
                Toast.makeText(context, R.string.download_no_file, Toast.LENGTH_SHORT).show();
                return;
            }

            if (intentInfo != null) {
                try {
                    final Intent appIntent = new Intent(intentInfo);
                    appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    appIntent.putExtra(HotKnotAdapter.EXTRA_DATA, uri);
                    appIntent.putExtra(EXTRA_HOTKNOT_DATA, true);
                    mContext.startActivity(appIntent);
                    return;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (ActivityNotFoundException ee) {
                    ee.printStackTrace();
                }
            }

            if (isCheck) {
                try {
                    final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    viewIntent.setDataAndTypeAndNormalize(uri, mimetype);
                    viewIntent.putExtra(EXTRA_HOTKNOT_DATA, true);
                    mContext.startActivity(viewIntent);
                    return;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (ActivityNotFoundException ee) {
                    ee.printStackTrace();
                }
            }

            showDocumentUI();
        }
    }


    private void showDocumentUI() {
        try {
            String path = ".";
            final Intent intent = new Intent(DocumentsContract.ACTION_MANAGE_ROOT);
            intent.setData(DocumentsContract.buildRootUri(HotKnotBeamConstants.STORAGE_AUTHORITY,
                           HotKnotBeamConstants.STORAGE_ROOT_ID));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, false);
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException eee) {
            eee.printStackTrace();
        }
    }
}