package com.mediatek.hetcomm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Common UI activity for HetComm.
 *
 * @hide
 */
public class HetCommActivity extends Activity implements OnClickListener {
    private static final String TAG = "HetCommActivity";


    private AlertDialog mDialog;
    private Context mContext;
    private Switch mEnableSwitch;
    private TextView mEnableSwitchStatus;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;
    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder()
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
        .build();
    private boolean mIsVpnOn = false;
    private boolean mIsTetherOn = false;
    private boolean mIsRoaming = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.hetcomm_activity);
        mContext = this.getBaseContext();

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mTelephonyManager = (TelephonyManager)
                            mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mConnManager = (ConnectivityManager)
                            mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isHetCommOn = (Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, 0) == 1 ? true : false);

        mEnableSwitchStatus = (TextView) findViewById(R.id.enable_switch_status);
        mEnableSwitch = (Switch) findViewById(R.id.enable_switch);

        showHetCommSetting(isHetCommOn);

        preCheckStatus();
        //attach a listener to check for changes in state
        mEnableSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "onCheckedChanged = " + isChecked);
                //onCheckedChanged would be called while first time set the listener

                if (isChecked) {
                    if (!showConnectionPrompt()) {
                        turnOffHetComm();
                        return;
                    }
                }
                updateHetCommSetting(isChecked);
                showHetCommSetting(isChecked);
                runHetCommService(isChecked);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            Intent intent = new Intent(HetCommActivity.this, HetCommHelpActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        boolean isEnabled = (Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, 0) == 1 ? true : false);
        showHetCommSetting(isEnabled);
        runHetCommService(isEnabled);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        //finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    /**
     * display the HetComm settings status.
     *
     */
     private void showHetCommSetting(boolean isEnabled) {
        Log.i(TAG, "showHetCommSetting enable = " + isEnabled);
        mEnableSwitch.setChecked(isEnabled);

        if (isEnabled) {
            mEnableSwitchStatus.setText(R.string.enable_switch_on);
        } else {
            mEnableSwitchStatus.setText(R.string.enable_switch_off);
        }
     }

    /**
     * Save the HetComm enable setting into global settings.
     *
     */
    private void updateHetCommSetting(boolean isEnabled) {
        Log.i(TAG, "updateHetCommSetting enable = " + isEnabled);
        int value  = (isEnabled) ? 1 : 0;

        Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, value);
    }

    /**
     * Start/Stop HetComm service.
     *
     */
    private void runHetCommService(boolean isEnabled) {
        Intent serviceIntent = new Intent(HetCommActivity.this, HetCommService.class);

        if (isEnabled) {
            Log.i(TAG, "Start HetComm Service");
            startService(serviceIntent);
        } else {
            Log.i(TAG, "Stop HetComm Service");
            stopService(serviceIntent);
        }
    }

    /**
     * check if Tethering/Vpn already on
     */
    private void preCheckStatus() {
        mIsRoaming = mTelephonyManager.isNetworkRoaming(
                            SubscriptionManager.getDefaultDataSubId());
        mIsTetherOn = (mConnManager.getTetheredIfaces().length >= 1) ? true : false;
        mIsVpnOn = (mConnManager.getNetworkForType(
                                        ConnectivityManager.TYPE_VPN) != null);
    }

    /**
     * Utility function for check Wi-Fi or Mobile connection status.
     */
    private boolean showConnectionPrompt() {
        boolean isWifiEnabled = mWifiManager.isWifiEnabled() ;
        boolean isMobileEnabled = mTelephonyManager.getDataEnabled();
        int simState = mTelephonyManager.getSimState();

        final Resources r = mContext.getResources();

        preCheckStatus();
        Log.i(TAG, "Tether:" + mIsTetherOn + " vpn:" + mIsVpnOn + " roaming:" + mIsRoaming);
        if (mIsTetherOn || mIsVpnOn || mIsRoaming) {
            if (mIsTetherOn) {
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_tether),
                            Toast.LENGTH_SHORT).show();
            } else if (mIsVpnOn) {
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_vpn),
                            Toast.LENGTH_SHORT).show();
            } else if (mIsRoaming) {
                Toast.makeText(mContext, r.getString(R.string.turn_off_due_to_roaming),
                            Toast.LENGTH_SHORT).show();
            }
            Log.i(TAG, "not showConnectionPrompt");
            return false;
        }

        Log.i(TAG, "wif:" + isWifiEnabled + " mobile:" + isMobileEnabled);
        String notice_title = r.getString(R.string.conn_notice_title);
        String notice_detail = r.getString(R.string.conn_notice_detail);
        String notice_warning = r.getString(R.string.conn_charging_warning);

        if (!isWifiEnabled || !isMobileEnabled || simState != TelephonyManager.SIM_STATE_READY) {
            clearPrompt();
            String buttonText;
            String wlanConn = r.getString(R.string.connection_wlan);
            String mobileConn = r.getString(R.string.connection_mobile);

            if (!isWifiEnabled && !isMobileEnabled) {
                buttonText = r.getString(R.string.turn_on, wlanConn + " & " + mobileConn);
            } else if (!isWifiEnabled) {
                buttonText = r.getString(R.string.turn_on, wlanConn);
            } else {
                buttonText = r.getString(R.string.turn_on, mobileConn);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(HetCommActivity.this)
            .setTitle(r.getString(R.string.app_name))
            .setMessage(notice_title + notice_detail + notice_warning)
            .setPositiveButton(buttonText, this)
            .setNegativeButton(android.R.string.cancel, this)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    turnOffHetComm();
                }
            });
            mDialog = builder.create();
            mDialog.show();
        } else if (isWifiEnabled && isMobileEnabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(HetCommActivity.this)
            .setTitle(r.getString(R.string.app_name))
            .setMessage(notice_warning)
            .setPositiveButton(android.R.string.ok, this)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    turnOffHetComm();
                }
            });
            mDialog = builder.create();
            mDialog.show();
        }

        return true;
    }

    private void turnOffHetComm() {
        updateHetCommSetting(false);
        showHetCommSetting(false);
        runHetCommService(false);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.i(TAG, "onClick:" + which);
        if (DialogInterface.BUTTON_POSITIVE == which) {
            boolean isWifiEnabled = mWifiManager.isWifiEnabled() ;
            boolean isMobileEnabled = mTelephonyManager.getDataEnabled();
            int simState = mTelephonyManager.getSimState();


            if (simState != TelephonyManager.SIM_STATE_READY) {
                Log.i(TAG, "SIM not ready");
                try {
                    startActivity(new Intent(
                                "com.android.settings.sim.SIM_SUB_INFO_SETTINGS"));
                } catch (ActivityNotFoundException ae) {
                    Log.e(TAG, "No activity for sim setting");
                }
                return;
            }
            if (!isMobileEnabled) {
                mTelephonyManager.setDataEnabled(true);
                Log.i(TAG, "Turn on mobile data connection");
            }

            if (!isWifiEnabled) {
                Log.i(TAG, "Start Wi-Fi setting");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        } else if ( DialogInterface.BUTTON_NEGATIVE == which) {
            turnOffHetComm();
        }
    }

    /*
     * Clear the previous dialog instance.
     */
    private void clearPrompt() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
