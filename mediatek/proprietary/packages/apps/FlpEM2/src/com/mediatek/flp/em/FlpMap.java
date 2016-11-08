package com.mediatek.flp.em;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.location.GeofenceHardware;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;

import com.mediatek.flp.em.FlpGeofence.GeofenceSession;
import com.mediatek.flp.em.FlpGeofence.GeofenceState;
import com.mediatek.flp.util.MyToast;

@SuppressLint("SetJavaScriptEnabled")
public class FlpMap extends Activity {
    public final static String TAG = "FlpEM2.FlpMap";
    public final static String MAP_URL = "file:///android_asset/FlpEM2.html";
    public final static String REPORT_LOCATION = "com.mediatek.flp.em.FlpMap.REPORT_LOCATION";
    public final static String THREE_LOCATION = "com.mediatek.flp.em.FlpMap.THREE_LOCATION";
    public final static String THREE_LOCATION_CLEAR = "com.mediatek.flp.em.FlpMap.THREE_LOCATION_CLEAR";
    public final static String GEOFENCE_ADD = "com.mediatek.flp.em.FlpMap.GEOFENCE_ADD";
    public final static String GEOFENCE_REMOVE = "com.mediatek.flp.em.FlpMap.GEOFENCE_REMOVE";
    public final static String GEOFENCE_TRANSITION = "com.mediatek.flp.em.FlpMap.GEOFENCE_TRANSITION";
    public final static String STEP_COUNT = "com.mediatek.flp.em.FlpMap.STEP_COUNT";
    public final static String STEP_LENGTH = "com.mediatek.flp.em.FlpMap.STEP_LENGTH";
    public final static String WIFI_AP_CLEAR = "com.mediatek.flp.em.FlpMap.WIFI_AP_CLEAR";
    public final static String WIFI_AP_INFO = "com.mediatek.flp.em.FlpMap.WIFI_AP_INFO";
    public final static String BT_AP_CLEAR = "com.mediatek.flp.em.FlpMap.BT_AP_CLEAR";
    public final static String BT_AP_INFO = "com.mediatek.flp.em.FlpMap.BT_AP_INFO";

    WebView mWebView;
    Button mButtonMenu;
    PopupMenu mMenuPopup;
    MyToast mToast;
    boolean mIsEnable;
    JavaScriptCaller mCaller;
    Location mLastLocation;
    int mStepCountLast = 0;
    /**
     * the value is always accumulated
     * */
    int mStepCount = 0;
    /**
     * the value is only accumulated between last report and current report
     * */
    int mStepLength = 0;
    int mTrueDistance = -1;
    ArrayList<ApInfo> mWifiAp = new ArrayList<ApInfo>();
    ArrayList<ApInfo> mBtAp = new ArrayList<ApInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        copyFileFromAssets("wifi_icon.png", getFilesDir().getPath() + "/"
                + "wifi_icon.png");
        copyFileFromAssets("bt_icon.png", getFilesDir().getPath() + "/"
                + "bt_icon.png");

        mToast = new MyToast(getApplicationContext());

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(REPORT_LOCATION);
        intentFilter.addAction(THREE_LOCATION);
        intentFilter.addAction(THREE_LOCATION_CLEAR);
        intentFilter.addAction(GEOFENCE_ADD);
        intentFilter.addAction(GEOFENCE_REMOVE);
        intentFilter.addAction(GEOFENCE_TRANSITION);
        intentFilter.addAction(STEP_COUNT);
        intentFilter.addAction(STEP_LENGTH);
        intentFilter.addAction(WIFI_AP_CLEAR);
        intentFilter.addAction(WIFI_AP_INFO);
        intentFilter.addAction(BT_AP_CLEAR);
        intentFilter.addAction(BT_AP_INFO);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        initUi();
        initUiListeners();

        mCaller = new JavaScriptCaller(mWebView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNetworkAvailable()) {
            if (!mIsEnable) {
                setupWebView();
            }
        } else {
            mToast.show("Network is unavailable");
        }
    }

    private void initUi() {
        mWebView = (WebView) findViewById(R.id.map_webview);
        mButtonMenu = (Button) findViewById(R.id.map_menu);

        mMenuPopup = new PopupMenu(this, mButtonMenu);
        mMenuPopup.getMenu().add(0, 0, Menu.NONE,
                "Go to current location immediately");
        mMenuPopup.getMenu().add(0, 1, Menu.NONE, "Move if location report")
                .setCheckable(true).setChecked(true);
        mMenuPopup.getMenu().add(0, 2, Menu.NONE, "Move if Geofence set")
                .setCheckable(true).setChecked(true);
        mMenuPopup.getMenu().add(0, 3, Menu.NONE, "Clear Tracking");
        mMenuPopup.getMenu()
                .add(0, 4, Menu.NONE, "Input True Distance for PDR");
    }

    private void setupWebView() {
        mToast.show("Google Map is loading...");
        mIsEnable = true;
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                log("onPageFinished");
                updateSensorInfo();
                updateApInfo();
                updateGeofenceInfo();
                updateLastLocation(false);
            }
        });
        mWebView.loadUrl(MAP_URL);
        mWebView.addJavascriptInterface(new JavaScriptCallback(), "Android");
    }

    private void copyFileFromAssets(String fileInAsset, String outputPath) {
        File file = new File(outputPath);
        if (!file.exists()) {
            log("fileInAssert=[" + fileInAsset + "] copy to [" + outputPath
                    + "]");
            try {
                AssetManager assetManager = getAssets();
                InputStream is = assetManager.open(fileInAsset);
                FileOutputStream fos = new FileOutputStream(outputPath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                byte[] buff = new byte[8192];
                while (is.available() > 0) {
                    is.read(buff);
                    bos.write(buff);
                }
                is.close();
                bos.flush();
                bos.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private void updateApInfo() {
        for (ApInfo a : mWifiAp) {
            mapWifiApInfo(a.mId, a.mLatitude, a.mLongitude);
        }
        for (ApInfo a : mBtAp) {
            mapBtApInfo(a.mId, a.mLatitude, a.mLongitude);
        }
    }

    private void updateGeofenceInfo() {
        GeofenceSession[] sessions = FlpGeofence.mSessions;
        for (GeofenceSession s : sessions) {
            if (s.mState == GeofenceState.ON) {
                mapGeofenceAdd(s.mId, s.mRequest.getLatitude(),
                        s.mRequest.getLongitude(), (int) s.mRequest.getRadius());
            }
        }
    }

    private void updateLastLocation(boolean showToast) {
        if (mLastLocation == null) {
            if (showToast) {
                mToast.show("current location is unavailable");
            }
        } else {
            mCaller.reportLocation(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(),
                    mLastLocation.hasAccuracy() ? (int) mLastLocation
                            .getAccuracy() : 0);
        }
    }

    private void updateSensorInfo() {
        String o = "";
        o += "Step Count=" + (mStepCount - mStepCountLast) + "<br>";
        o += "Step Length=" + mStepLength + " (cm)";
        if (mTrueDistance != -1) {
            o += "<br>";
            int trueDistance = mTrueDistance * 100;
            float errorPercentage = (Math.abs((float) mStepLength
                    - trueDistance) / trueDistance) * 100;
            o += "True Distance=" + mTrueDistance + " (m)<br>";
            o += "Error Percentage=" + errorPercentage + " %";
        }
        mCaller.updateCornerInfo(o);
    }

    private boolean isIntValid(String input) {
        try {
            Integer.valueOf(input);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void inputTrueDistanceForPDR() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(FlpMap.this);
        dialog.setTitle("Input True Distance");
        dialog.setMessage("The unit is meter.\n"
                + "The input value will be calculated with step length to obtain a error percentage");
        final EditText editText = new EditText(FlpMap.this);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(9);
        editText.setFilters(filterArray);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialog.setView(editText);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String trueString = editText.getText().toString();
                if (!isIntValid(trueString)) {
                    mToast.show("invalid input value=[" + trueString + "]");
                    return;
                }
                int trueDistance = Integer.valueOf(trueString);
                if (trueDistance == 0) {
                    mToast.show("invalid input value=[" + trueDistance + "]");
                    return;
                }
                mTrueDistance = trueDistance;
                updateSensorInfo();
            }
        });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    private void initUiListeners() {
        mMenuPopup
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == 0) {
                            // Go to current location immediately
                            updateLastLocation(true);
                        }
                        if (id == 1) {
                            // Move if location report
                            item.setChecked(!item.isChecked());
                            mCaller.mMoveIfLocationReport = item.isChecked();
                        }
                        if (id == 2) {
                            // Move if Geofence set
                            item.setChecked(!item.isChecked());
                            mCaller.mMoveIfGeofenceSet = item.isChecked();
                        }
                        if (id == 3) {
                            // Clear Tracking
                            mTrueDistance = -1;
                            mStepCountLast = mStepCount;
                            mStepLength = 0;
                            mCaller.clearTracking();
                            updateSensorInfo();
                        }
                        if (id == 4) {
                            // Input True Distance for PDR
                            inputTrueDistanceForPDR();
                        }
                        return false;
                    }
                });

        mButtonMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenuPopup.show();
            }
        });
    }

    @SuppressWarnings("unused")
    static class JavaScriptCaller {
        WebView mWebView;
        boolean mMoveIfLocationReport = true;
        boolean mMoveIfGeofenceSet = true;

        public JavaScriptCaller(WebView webView) {
            if (webView == null) {
                throw new RuntimeException("WebView cannot be null");
            }
            mWebView = webView;
        }

        public void reportLocation(double latitude, double longitude,
                int accuracy) {
            if (mMoveIfLocationReport) {
                setMapCenter(latitude, longitude);
            }
            enableMarker(true);
            updateMarker(latitude, longitude);
            enableAccuracy(true);
            updateAccuracy(latitude, longitude, accuracy);
            addLocation(latitude, longitude);
        }

        public void threeLocation(double latitude, double longitude) {
            if (mMoveIfLocationReport) {
                setMapCenter(latitude, longitude);
            }
            addThreeLocation(latitude, longitude);
        }

        public void clearThreeLocation() {
            clearTracking();
        }

        public void clearTracking() {
            clearLocation();
        }

        public void addFence(int id, double latitude, double longitude,
                int radius) {
            if (mMoveIfGeofenceSet) {
                setMapCenter(latitude, longitude);
            }
            enableFence(id, true);
            updateFence(id, latitude, longitude, radius, 0x606060);
        }

        public void updateFence(int id, int monitorTransition) {
            int color = 0;
            if ((monitorTransition & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
                color |= 0x00ff00;
            }
            if ((monitorTransition & GeofenceHardware.GEOFENCE_EXITED) != 0) {
                color |= 0x0000ff;
            }
            if ((monitorTransition & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
                color |= 0xff0000;
            }
            updateFence2(id, color);
        }

        public void removeFence(int id) {
            enableFence(id, false);
        }

        public void addBtApInfo(String id, double latitude, double longitude) {
            mWebView.loadUrl("javascript:addBtApInfo('" + id + "'," + latitude
                    + "," + longitude + ")");
        }

        public void clearBtApInfo() {
            mWebView.loadUrl("javascript:clearBtApInfo()");
        }

        public void addWifiApInfo(String id, double latitude, double longitude) {
            mWebView.loadUrl("javascript:addWifiApInfo('" + id + "',"
                    + latitude + "," + longitude + ")");
        }

        public void clearWifiApInfo() {
            mWebView.loadUrl("javascript:clearWifiApInfo()");
        }

        public void updateCornerInfo(String message) {
            mWebView.loadUrl("javascript:updateCornerInfo('" + message + "')");
        }

        private void addLocation(double latitude, double longitude) {
            mWebView.loadUrl("javascript:addLocation(" + latitude + ","
                    + longitude + ")");
        }

        private void addThreeLocation(double latitude, double longitude) {
            mWebView.loadUrl("javascript:addThreeLocation(" + latitude + ","
                    + longitude + ")");
        }

        private void clearLocation() {
            mWebView.loadUrl("javascript:clearLocation()");
        }

        private void enableAccuracy(boolean enabled) {
            mWebView.loadUrl("javascript:enableAccuracy(" + enabled + ")");
        }

        private void updateAccuracy(double latitude, double longitude,
                int accuracy) {
            mWebView.loadUrl("javascript:updateAccuracy(" + latitude + ","
                    + longitude + "," + accuracy + ")");
        }

        private void setMapCenter(double latitude, double longitude) {
            mWebView.loadUrl("javascript:setMapCenter(" + latitude + ","
                    + longitude + ")");
        }

        private void updateInfo(double latitude, double longitude,
                String message) {
            mWebView.loadUrl("javascript:updateInfo(" + latitude + ","
                    + longitude + ",'" + message + "')");
        }

        private void appendInfo(String message) {
            mWebView.loadUrl("javascript:appendInfo('" + message + "')");
        }

        private void enableMarker(boolean enabled) {
            mWebView.loadUrl("javascript:enableMarker(" + enabled + ")");
        }

        private void updateMarker(double latitude, double longitude) {
            mWebView.loadUrl("javascript:updateMarker(" + latitude + ","
                    + longitude + ")");
        }

        private void enableFence(int id, boolean enabled) {
            mWebView.loadUrl("javascript:enableFence(" + id + ", " + enabled
                    + ")");
        }

        private void updateFence(int id, double latitude, double longitude,
                int radius, int color) {
            mWebView.loadUrl("javascript:updateFence(" + id + "," + latitude
                    + "," + longitude + "," + radius + "," + color + ")");
        }

        private void updateFence2(int id, int color) {
            mWebView.loadUrl("javascript:updateFence2(" + id + "," + color
                    + ")");
        }

        private void createCircle(double latitude, double longitude, int radius) {
            mWebView.loadUrl("javascript:createCircle(" + latitude + ","
                    + longitude + "," + radius + ")");
        }

        private void createTriangle(double lat1, double lng1, double lat2,
                double lng2, double lat3, double lng3) {
            mWebView.loadUrl("javascript:createTriangle(" + lat1 + "," + lng1
                    + "," + lat2 + "," + lng2 + "," + lat3 + "," + lng3 + ")");
        }

        private void createLine(double lat1, double lng1, double lat2,
                double lng2) {
            mWebView.loadUrl("javascript:createLine(" + lat1 + "," + lng1 + ","
                    + lat2 + "," + lng2 + ")");
        }

        private void test(int data) {
            mWebView.loadUrl("javascript:test(" + data + ")");
        }

    }

    public class JavaScriptCallback {
        @JavascriptInterface
        public void onClicked(float latitude, float longitude) {
            // mToast.show("onClicked " + latitude + "," + longitude);
        }

        @JavascriptInterface
        public void onDoubleClicked(float latitude, float longitude) {
            // mToast.show("onDoubleClicked " + latitude + "," + longitude);
        }

        @JavascriptInterface
        public void onCenterChanged(float latitude, float longitude) {
            // mToast.show("onCenterChanged " + latitude + "," + longitude);
        }

        /**
         * zoom = 0, highest altitude to see earth, 2000 km <br>
         * zoom = 21, lowest altitude to see earth, 5 m <br>
         * */
        @JavascriptInterface
        public void onZoomChanged(int zoom) {
            // mToast.show("onZoomChanged zoom=" + zoom);
        }

        @JavascriptInterface
        public void log(String message) {
            FlpMap.log(message);
        }

        @JavascriptInterface
        public void toast(String message) {
            mToast.show(message);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (!mIsEnable && isNetworkAvailable()) {
                    setupWebView();
                }
            }
            if (action.equals(REPORT_LOCATION)) {
                double lat = b.getDouble("lat");
                double lng = b.getDouble("lng");
                int acc = b.getInt("acc");
                if (mLastLocation == null) {
                    mLastLocation = new Location("last");
                }
                mLastLocation.setLatitude(lat);
                mLastLocation.setLongitude(lng);
                mLastLocation.setAccuracy(acc);
                mCaller.reportLocation(lat, lng, acc);
            }
            if (action.equals(THREE_LOCATION)) {
                double lat = b.getDouble("lat");
                double lng = b.getDouble("lng");
                if (mLastLocation == null) {
                    mLastLocation = new Location("last");
                }
                mCaller.threeLocation(lat, lng);
            }
            if (action.equals(THREE_LOCATION_CLEAR)) {
                mCaller.clearThreeLocation();
            }
            if (action.equals(GEOFENCE_ADD)) {
                int id = b.getInt("id");
                double lat = b.getDouble("lat");
                double lng = b.getDouble("lng");
                int radius = b.getInt("radius");
                mCaller.addFence(id, lat, lng, radius);
            }
            if (action.equals(GEOFENCE_REMOVE)) {
                int id = b.getInt("id");
                mCaller.removeFence(id);
            }
            if (action.equals(GEOFENCE_TRANSITION)) {
                int id = b.getInt("id");
                int monitorTransition = b.getInt("monitorTransition");
                mCaller.updateFence(id, monitorTransition);
            }
            if (action.equals(STEP_COUNT)) {
                mStepCount = b.getInt("stepCount");
                if (mStepCountLast == 0) {
                    mStepCountLast = mStepCount;
                }
                updateSensorInfo();
            }
            if (action.equals(STEP_LENGTH)) {
                mStepLength += b.getInt("stepLength");
                updateSensorInfo();
            }
            if (action.equals(WIFI_AP_CLEAR)) {
                mCaller.clearWifiApInfo();
                mWifiAp.clear();
            }
            if (action.equals(WIFI_AP_INFO)) {
                String id = b.getString("id");
                double lat = b.getDouble("lat");
                double lng = b.getDouble("lng");
                mCaller.addWifiApInfo(id, lat, lng);
                mWifiAp.add(new ApInfo(id, lat, lng));
            }
            if (action.equals(BT_AP_CLEAR)) {
                mCaller.clearBtApInfo();
                mBtAp.clear();
            }
            if (action.equals(BT_AP_INFO)) {
                String id = b.getString("id");
                double lat = b.getDouble("lat");
                double lng = b.getDouble("lng");
                mCaller.addBtApInfo(id, lat, lng);
                mBtAp.add(new ApInfo(id, lat, lng));
            }
        }
    };

    public void mapReportLocation(double latitude, double longitude,
            int accuracy) {
        Intent intent = new Intent(FlpMap.REPORT_LOCATION);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        intent.putExtra("acc", accuracy);
        sendBroadcast(intent);
    }

    public void mapThreeLocation(double latitude, double longitude) {
        Intent intent = new Intent(FlpMap.THREE_LOCATION);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        sendBroadcast(intent);
    }

    public void mapThreeLocationClear() {
        Intent intent = new Intent(FlpMap.THREE_LOCATION_CLEAR);
        sendBroadcast(intent);
    }

    public void mapGeofenceAdd(int id, double latitude, double longitude,
            int radius) {
        Intent intent = new Intent(FlpMap.GEOFENCE_ADD);
        intent.putExtra("id", id);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        intent.putExtra("radius", radius);
        sendBroadcast(intent);
    }

    public void mapGeofenceRemove(int id) {
        Intent intent = new Intent(FlpMap.GEOFENCE_REMOVE);
        intent.putExtra("id", id);
        sendBroadcast(intent);
    }

    public void mapGeofenceTransition(int id, int monitorTransition) {
        Intent intent = new Intent(FlpMap.GEOFENCE_TRANSITION);
        intent.putExtra("id", id);
        intent.putExtra("monitorTransition", monitorTransition);
        sendBroadcast(intent);
    }

    public void mapStepCount(int stepCount) {
        Intent intent = new Intent(FlpMap.STEP_COUNT);
        intent.putExtra("stepCount", stepCount);
        sendBroadcast(intent);
    }

    public void mapStepLength(int stepLength) {
        Intent intent = new Intent(FlpMap.STEP_LENGTH);
        intent.putExtra("stepLength", stepLength);
        sendBroadcast(intent);
    }

    public void mapWifiApClear() {
        Intent intent = new Intent(FlpMap.WIFI_AP_CLEAR);
        sendBroadcast(intent);
    }

    public void mapWifiApInfo(String id, double latitude, double longitude) {
        Intent intent = new Intent(FlpMap.WIFI_AP_INFO);
        intent.putExtra("id", id);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        sendBroadcast(intent);
    }

    public void mapBtApClear() {
        Intent intent = new Intent(FlpMap.BT_AP_CLEAR);
        sendBroadcast(intent);
    }

    public void mapBtApInfo(String id, double latitude, double longitude) {
        Intent intent = new Intent(FlpMap.BT_AP_INFO);
        intent.putExtra("id", id);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        sendBroadcast(intent);
    }

    public static class ApInfo {
        String mId;
        double mLatitude;
        double mLongitude;

        public ApInfo(String id, double latitude, double longitude) {
            mId = id;
            mLatitude = latitude;
            mLongitude = longitude;
        }
    }

    public static void log(Object msg) {
        Log.d(TAG, "" + msg);
    }

    public static void loge(Object msg) {
        Log.d(TAG, "ERR: " + msg);
    }
}
