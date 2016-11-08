package com.mediatek.gallery3d.plugin;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.op01.plugin.R;

import java.util.List;

/**
 * OP01 plugin implementation of SettingsActivity.
 */
public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "Gallery3d/Plugin/SettingsActivity";
    private static final boolean LOG = true;

    private static final String PREF_KEY_APN = "apn_settings";
    private static final String PREF_KEY_ENABLE_RTSP_PROXY = "enable_rtsp_proxy";
    private static final String PREF_KEY_RTSP_PROXY = "rtsp_proxy_settings";
    private static final String PREF_KEY_ENABLE_HTTP_PROXY = "enable_http_proxy";
    private static final String PREF_KEY_HTTP_PROXY = "http_proxy_settings";
    private static final String PREF_KEY_UDP_PORT = "udp_port_settings";
    private static final String EXTRA_SUBID = "sub_id";

    private static final String SETTING_KEY_RTSP_PROXY_ENABLED =
                                        MediaStore.Streaming.Setting.RTSP_PROXY_ENABLED;
    private static final String SETTING_KEY_RTSP_PROXY_HOST =
                                        MediaStore.Streaming.Setting.RTSP_PROXY_HOST;
    private static final String SETTING_KEY_RTSP_PROXY_PORT =
                                        MediaStore.Streaming.Setting.RTSP_PROXY_PORT;

    private static final String SETTING_KEY_HTTP_PROXY_ENABLED =
                                        MediaStore.Streaming.Setting.HTTP_PROXY_ENABLED;
    private static final String SETTING_KEY_HTTP_PROXY_HOST =
                                        MediaStore.Streaming.Setting.HTTP_PROXY_HOST;
    private static final String SETTING_KEY_HTTP_PROXY_PORT =
                                        MediaStore.Streaming.Setting.HTTP_PROXY_PORT;

    private static final String SETTING_KEY_MAX_PORT = MediaStore.Streaming.Setting.MAX_UDP_PORT;
    private static final String SETTING_KEY_MIN_PORT = MediaStore.Streaming.Setting.MIN_UDP_PORT;

    private static final int UNKNOWN_PORT = -1;

    private static final String ACTION_APN = "android.settings.APN_SETTINGS";
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";

    private Preference mApnPref;
    private CheckBoxPreference mRtspProxyEnabler;
    private Preference mRtspProxyPref;
    private CheckBoxPreference mHttpProxyEnabler;
    private Preference mHttpProxyPref;
    private Preference mUdpPortPref;
    private ConnectivityManager mCM;

    private ProxyDialog mProxyDialog;
    private AlertDialog mUdpDialog;
    private ContentResolver mCr;
    private IntentFilter mMobileStateFilter;

    private static final String PREF_KEY_HTTP_BUFFER_SIZE = "http_buffer_size";
    private static final String PREF_KEY_RTSP_BUFFER_SIZE = "rtsp_buffer_size";
    private static final String KEY_HTTP_BUFFER_SIZE = "MTK-HTTP-CACHE-SIZE";
    private static final String KEY_RTSP_BUFFER_SIZE = "MTK-RTSP-CACHE-SIZE";
    private static final int DEFAULT_HTTP_BUFFER_SIZE = 10; //seconds
    private static final int DEFAULT_RTSP_BUFFER_SIZE = 6; //seconds
    private Preference mBufferSizeHttpPref;
    private Preference mBufferSizeRtspPref;

    private SubscriptionInfo mSimInfo;

    public static final String KEY_LOGO_BITMAP = "logo-bitmap";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG) {
            Log.v(TAG, "onCreate");
        }
        Bitmap logo = getIntent().getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            getActionBar().setLogo(new BitmapDrawable(getResources(), logo));
        }

        addPreferencesFromResource(R.xml.movie_settings);
        mApnPref = findPreference(PREF_KEY_APN);
        mRtspProxyEnabler = (CheckBoxPreference) findPreference(PREF_KEY_ENABLE_RTSP_PROXY);
        mRtspProxyPref = findPreference(PREF_KEY_RTSP_PROXY);
        mHttpProxyEnabler = (CheckBoxPreference) findPreference(PREF_KEY_ENABLE_HTTP_PROXY);
        mHttpProxyPref = findPreference(PREF_KEY_HTTP_PROXY);
        mUdpPortPref = findPreference(PREF_KEY_UDP_PORT);
        mBufferSizeHttpPref = findPreference(PREF_KEY_HTTP_BUFFER_SIZE);
        mBufferSizeRtspPref = findPreference(PREF_KEY_RTSP_BUFFER_SIZE);

        mCM = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mCr = this.getContentResolver();

        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mMobileStateFilter.addAction(TRANSACTION_START);
        mMobileStateFilter.addAction(TRANSACTION_STOP);
        mMobileStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshApn();
        refreshRtspProxy();
        refreshHttpProxy();
        refreshUdpPort();
        refreshBufferSizeHttp();
        refreshBufferSizeRtsp();
        registerReceiver(mMobileStateReceiver, mMobileStateFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mMobileStateReceiver);
    }

    private void refreshApn() {
        refreshSIMInfo();
        NetworkInfo networkInfo = mCM.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                mApnPref.setEnabled(false);
                mApnPref.setSummary(R.string.wifi_network_no_need_apn);
                return;
            }
        }
        if (mSimInfo == null) {
            mApnPref.setEnabled(false);
            mApnPref.setSummary(R.string.apn_settings_not_valid);
        } else {
            mApnPref.setEnabled(true);
            mApnPref.setSummary(String.format(
                getString(R.string.apn_settings_summary),
                mSimInfo.getDisplayName().toString(), getApnName()));
        }
    }

    private void refreshRtspProxy() {
        final boolean enableProxy =
                         (Settings.System.getInt(mCr, SETTING_KEY_RTSP_PROXY_ENABLED, 0) == 1);
        final String host = Settings.System.getString(mCr, SETTING_KEY_RTSP_PROXY_HOST);
        final int port = Settings.System.getInt(mCr, SETTING_KEY_RTSP_PROXY_PORT, UNKNOWN_PORT);
        if (enableProxy && host != null && host.length() != 0 && port != UNKNOWN_PORT) {
            mRtspProxyPref.setSummary(host + ":" + port);
        } else {
            mRtspProxyPref.setSummary(R.string.rtsp_proxy_settings_summary);
        }
        mRtspProxyEnabler.setChecked(enableProxy);
        if (LOG) {
            Log.v(TAG, "refreshRtspProxy() enableProxy=" + enableProxy + ", host=" +
                       host + ", mPort=" + port);
        }
    }

    private void refreshHttpProxy() {
        final boolean enableProxy =
                     (Settings.System.getInt(mCr, SETTING_KEY_HTTP_PROXY_ENABLED, 0) == 1);
        final String host = Settings.System.getString(mCr, SETTING_KEY_HTTP_PROXY_HOST);
        final int port = Settings.System.getInt(mCr, SETTING_KEY_HTTP_PROXY_PORT, UNKNOWN_PORT);
        if (enableProxy && host != null && host.length() != 0 && port != UNKNOWN_PORT) {
            mHttpProxyPref.setSummary(host + ":" + port);
        } else {
            mHttpProxyPref.setSummary(R.string.http_proxy_settings_summary);
        }
        mHttpProxyEnabler.setChecked(enableProxy);
        if (LOG) {
            Log.v(TAG, "refreshHttpProxy() enableProxy=" + enableProxy + ", host=" +
                  host + ", mPort=" + port);
        }
    }

    private void refreshUdpPort() {
        final int minport = Settings.System.getInt(mCr,
                                    MediaStore.Streaming.Setting.MIN_UDP_PORT, UNKNOWN_PORT);
        final int maxport = Settings.System.getInt(mCr,
                                    MediaStore.Streaming.Setting.MAX_UDP_PORT, UNKNOWN_PORT);
        if (minport != UNKNOWN_PORT && maxport != UNKNOWN_PORT && maxport >= minport) {
            mUdpPortPref.setSummary(minport + " - " + maxport);
        } else {
            mUdpPortPref.setSummary(R.string.udp_port_settings_summary);
        }
        if (LOG) {
            Log.v(TAG, "refreshUdpPort() maxport=" + maxport + ", minport=" + minport);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (LOG) {
            Log.v(TAG, "onPreferenceTreeClick(" + preference + ")");
        }
        if (preference == mApnPref) {
            showApnDialog();
        } else if (preference == mRtspProxyPref) {
            showProxyDialog(ProxyDialog.TYPE_RTSP);
        } else if (preference == mHttpProxyPref) {
            showProxyDialog(ProxyDialog.TYPE_HTTP);
        } else if (preference == mUdpPortPref) {
            showUdpPortDialog();
        } else if (preference == mRtspProxyEnabler) {
            final boolean enable = mRtspProxyEnabler.isChecked();
            Settings.System.putInt(mCr, SETTING_KEY_RTSP_PROXY_ENABLED, (enable ? 1 : 0));
            refreshRtspProxy();
        } else if (preference == mHttpProxyEnabler) {
            final boolean enable = mHttpProxyEnabler.isChecked();
            Settings.System.putInt(mCr, SETTING_KEY_HTTP_PROXY_ENABLED, (enable ? 1 : 0));
            refreshHttpProxy();
        } else if (preference == mBufferSizeHttpPref) {
            showBufferSizeHttpDialog();
        } else if (preference == mBufferSizeRtspPref) {
            showBufferSizeRtspDialog();
        }
        return true;
    }

    private void showUdpPortDialog() {
        if (mUdpDialog != null) {
            mUdpDialog.dismiss();
        }
        mUdpDialog = new PortDialog(this);
        mUdpDialog.getWindow().setSoftInputMode(
                               WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mUdpDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                refreshUdpPort();
            }

        });
        mUdpDialog.show();
    }

    private void showApnDialog() {
        if (mSimInfo != null) {
            final Intent intent = new Intent();
            intent.setAction(ACTION_APN);
            intent.putExtra(EXTRA_SUBID, mSimInfo.getSubscriptionId());
            startActivity(intent);
        } else {
            refreshApn();
        }
    }

    private void showProxyDialog(final int type) {
        if (mProxyDialog != null) {
            mProxyDialog.dismiss();
        }
        mProxyDialog = new ProxyDialog(this, type);
        mProxyDialog.getWindow().setSoftInputMode(
                     WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mProxyDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                if (ProxyDialog.TYPE_RTSP == mProxyDialog.getType()) {
                    refreshRtspProxy();
                } else {
                    refreshHttpProxy();
                }
            }

        });
        mProxyDialog.show();
    }

    //APN info
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_3 = 2;
    private static final int SIM_CARD_4 = 3;
    private static final int SIM_CARD_UNDEFINED = -1;

    public static final String PREFERAPN_NO_UPDATE_URI_USING_SUBID
        = "content://telephony/carriers/preferapn_no_update/subId/";

    private Uri mUri;
    private Uri mDefaultApnUri;
    private Uri mPreferedCarrierUri;

    private String getQueryWhere() {
        String where = "";
        mUri = Telephony.Carriers.CONTENT_URI;
        where = TelephonyManager.getTelephonyProperty(
                   SubscriptionManager.getPhoneId(mSimInfo.getSubscriptionId()),
                   TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC,
                    "-1");
        Log.v(TAG, "getQueryWhere() mUri=" + mUri);
        Log.v(TAG, "getQueryWhere() where=" + where);
        return where;
    }

    private String getApnName() {
        final String where = "numeric=\"" + getQueryWhere() + "\"";
        Cursor cursor = null;
        String name = getString(R.string.apn_settings_no_default_apn);
        if (mUri == null) {
            return null;
        }
        try {
            cursor = mCr.query(
                mUri,
                new String[] { "_id", "name" },
                where,
                null, Telephony.Carriers.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                final int key = getSelectedApnKey();
                if (key != -1) {
                    while (cursor.moveToNext()) {
                        if (key == cursor.getInt(0)) {
                            name = cursor.getString(1);
                            break;
                        }
                    }
                }
            }
        } catch (final SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            Log.v(TAG, "getApnName() return " + name);
        }
        return name;
    }

    private int getSelectedApnKey() {
        int key = -1;
        Cursor cursor = null;
        String subId = Integer.toString(mSimInfo.getSubscriptionId());
        Uri uri = Uri.withAppendedPath(
                      Uri.parse(PREFERAPN_NO_UPDATE_URI_USING_SUBID), subId);
        try {
            cursor = mCr.query(
                uri,
                new String[] {"_id"},
                null,
                null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
            Log.v(TAG, "getSelectedApnKey() Uri=" + uri + " cursor = " + cursor);
            if (cursor != null && cursor.moveToFirst()) {
                key = cursor.getInt(0);
            }
        } catch (final SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            Log.v(TAG, "getSelectedApnKey() key=" + key);
        }
        return key;
    }


    private void refreshSIMInfo() {
        mSimInfo = null;
        if (isGemini()) {
            mSimInfo = SubscriptionManager.from((Context) this).getDefaultDataSubscriptionInfo();
        } else {
            final List<SubscriptionInfo> list = SubscriptionManager.from((Context) this).
                                                          getActiveSubscriptionInfoList();
            if (list != null && list.size() > 0) {
                mSimInfo = list.get(0);
            }
        }
        if (LOG) {
            Log.v(TAG, "refreshSIMInfo() mSimInfo=" + mSimInfo);
        }
        if (LOG && mSimInfo != null) {
            Log.i(TAG, "refreshSIMInfo() simid=" + mSimInfo.getSubscriptionId()
                    + ", slot=" + mSimInfo.getSimSlotIndex()
                    + ", displayName=" + mSimInfo.getDisplayName().toString());
        }
    }

    private boolean isGemini() {
        final boolean gemini = (TelephonyManager.getDefault().getPhoneCount() > 1) ? true : false;
        if (LOG) {
            Log.v(TAG, "isGemini() return " + gemini);
        }
        return gemini;
    }

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (LOG) {
                Log.v(TAG, "mMobileStateReceiver.onReceive(" + intent + ")");
            }
            refreshApn();
        }
    };

    private void refreshBufferSizeHttp() {
        final int bufferSize = Settings.System.getInt(
                               mCr, KEY_HTTP_BUFFER_SIZE, DEFAULT_HTTP_BUFFER_SIZE);
        mBufferSizeHttpPref.setSummary(getString(
                               R.string.http_buffer_size_text, bufferSize));
        if (LOG) {
            Log.i(TAG, "refreshBufferSizeHttp() bufferSize=" + bufferSize);
        }
    }

    private void refreshBufferSizeRtsp() {
        final int bufferSize = Settings.System.getInt(
                               mCr, KEY_RTSP_BUFFER_SIZE, DEFAULT_RTSP_BUFFER_SIZE);
        mBufferSizeRtspPref.setSummary(getString(R.string.rtsp_buffer_size_text, bufferSize));
        if (LOG) {
            Log.i(TAG, "refreshBufferSizeRtsp() bufferSize=" + bufferSize);
        }
    }

    private void showBufferSizeHttpDialog() {
        showBufferSizeDialog(LimitDialog.TYPE_HTTP);
    }

    private void showBufferSizeRtspDialog() {
        showBufferSizeDialog(LimitDialog.TYPE_RTSP);
    }

    private void showBufferSizeDialog(final int type) {
        final LimitDialog limitDialog = new LimitDialog(this, type);
        limitDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        limitDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(final DialogInterface dialog) {
                if (type == LimitDialog.TYPE_HTTP) {
                    refreshBufferSizeHttp();
                } else {
                    refreshBufferSizeRtsp();
                }
            }

        });
        limitDialog.show();
    }
}
