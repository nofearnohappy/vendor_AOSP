package com.mediatek.flp.em;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.location.provider.FusedLocationHardware;
import com.android.location.provider.FusedLocationHardwareSink;
import com.mediatek.flp.service.FusedLocationService;
import com.mediatek.flp.util.TextStringList;

public class FlpStatus extends Activity {
    public final static String TAG = "FlpEM2.FlpDiagnosticStatus";

    /**
     * The following values must match the definitions in fused_location.h
     * The constant used to indicate that the monitoring system supports GNSS,WIFI,CELL.
     */
    public static final int CAPABILITY_GNSS = (1<<0);
    public static final int CAPABILITY_WIFI = (1<<1);
    public static final int CAPABILITY_CELL = (1<<2);
    //The following values must match the definitions in fused_location.h
    public static final int FLP_STATUS_LOCATION_AVAILABLE = 0;
    public static final int FLP_STATUS_LOCATION_UNAVAILABLE = 1;

    TextView mTextViewGnss;
    TextView mTextViewWifi;
    TextView mTextViewWifiAp;
    TextView mTextViewSensors;
    TextView mTextViewBt;
    TextView mTextViewBtAp;
    TextView mTextViewFlp;
    TextView mTextViewFlpCapabilities;
    TextView mTextViewFlpStatus;
    Button mButtonClear;
    Button mButtonLoopBackTest;
    TextView mTextViewLogCount;
    TextView mTextViewLog;

    static TextStringList sInfo = new TextStringList(32);
    boolean mHardwareReady;
    FusedLocationHardware mHardware;
    int mLogCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status);

        mHardwareReady = FusedLocationService.isHardwareReady();
        if (mHardwareReady) {
            mHardware = FusedLocationService.hardware();
            mHardware.registerSink(mSink, getMainLooper());
        }

        initUi();
        initUiListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHardwareReady) {
            mHardware.unregisterSink(mSink);
        }
    }

    private boolean isDoubleValid(String input) {
        try {
            Double.valueOf(input);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean isIntValid(String input) {
        try {
            Integer.valueOf(input);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void parseWifiApInfo(String info) {
        mapWifiApClear();
        String[] lines = info.split("\n");
        log("WIFI AP size=" + lines.length);
        for (String line : lines) {
            String id = "";
            double lat = 0;
            double lng = 0;
            String[] fields = line.split("\t");
            for (String field : fields) {
                String[] data = field.split("::");
                if (data[0].equals("SSID")) {
                    id = data[1];
                }
                if (data[0].equals("LAT")) {
                    if (isDoubleValid(data[1])) {
                        lat = Double.valueOf(data[1]);
                    } else {
                        loge("invalid diagnostic data=[" + info + "]");
                        return;
                    }
                }
                if (data[0].equals("LNG")) {
                    if (isDoubleValid(data[1])) {
                        lng = Double.valueOf(data[1]);
                    } else {
                        loge("invalid diagnostic data=[" + info + "]");
                        return;
                    }
                }
            }
            mapWifiApInfo(id, lat, lng);
        }
    }

    private void parseBtApInfo(String info) {
        mapBtApClear();
        String[] lines = info.split("\n");
        log("BT AP size=" + lines.length);
        for (String line : lines) {
            String id = "";
            double lat = 0;
            double lng = 0;
            String[] fields = line.split("\t");
            for (String field : fields) {
                String[] data = field.split("::");
                if (data[0].equals("MAC")) {
                    id = data[1];
                }
                if (data[0].equals("LAT")) {
                    if (isDoubleValid(data[1])) {
                        lat = Double.valueOf(data[1]);
                    } else {
                        loge("invalid diagnostic data=[" + info + "]");
                        return;
                    }
                }
                if (data[0].equals("LNG")) {
                    if (isDoubleValid(data[1])) {
                        lng = Double.valueOf(data[1]);
                    } else {
                        loge("invalid diagnostic data=[" + info + "]");
                        return;
                    }
                }
            }
            mapBtApInfo(id, lat, lng);
        }
    }

    private void parseStepInfo(String info) {
        int stepCount = 0;
        int stepLength = 0;
        String[] fields = info.split("\t");
        for (String field : fields) {
            String[] data = field.split("::");
            if (data[0].equals("COUNT")) {
                if (isIntValid(data[1])) {
                    stepCount = Integer.valueOf(data[1]);
                } else {
                    loge("invalid diagnostic data=[" + info + "]");
                    return;
                }
            }
            if (data[0].equals("LENGTH")) {
                if (isIntValid(data[1])) {
                    stepLength = Integer.valueOf(data[1]);
                } else {
                    loge("invalid diagnostic data=[" + info + "]");
                    return;
                }
            }
        }
        mapStepCount(stepCount);
        mapStepLength(stepLength);
    }

    private FusedLocationHardwareSink mSink = new FusedLocationHardwareSink() {

        @Override
        public void onDiagnosticDataAvailable(String data) {
            mLogCount++;
            mTextViewLogCount.setText("count=" + mLogCount);
            sInfo.print(data);

            String[] tmp = data.split("=");
            if (tmp.length < 2) {
                return;
            }

            String tag = tmp[0];
            String content = tmp[1];
            if (tag.equals("GNSS_NTF")) {
                mTextViewGnss.setText(content);
            }
            if (tag.equals("RTT_NTF")) {
                mTextViewWifi.setText(content);
            }
            if (tag.equals("WIFI_AP_NTF")) {
                log("WIFI_AP_NTF [\n" + content + "\n]");
                mTextViewWifiAp.setText(content);
                parseWifiApInfo(content);
            }
            if (tag.equals("SENS_NTF")) {
                mTextViewSensors.setText(content);
            }
            if (tag.equals("BT_NTF")) {
                mTextViewBt.setText(content);
            }
            if (tag.equals("BT_AP_NTF")) {
                log("BT_AP_NTF [\n" + content + "\n]");
                mTextViewBtAp.setText(content);
                parseBtApInfo(content);
            }
            if (tag.equals("FLP_NTF")) {
                mTextViewFlp.setText(content);
            }
            if (tag.equals("STEP_NTF")) {
                log("STEP_NTF [" + content + "]");
                parseStepInfo(content);
            }
        }

        @Override
        public void onLocationAvailable(Location[] locations) {
        }

        @Override
        public void onCapabilities(int capabilities) {
            log("flpCapabilities:" + capabilities);

            String sources = "";
            if (capabilities == CAPABILITY_GNSS) {
                sources += "GNSS ";
            }
            if (capabilities == CAPABILITY_WIFI) {
                sources += "WIFI ";
            }
            if (capabilities == CAPABILITY_CELL) {
                sources += "CELL ";
            }
            mTextViewFlpCapabilities.setText(sources);
            sInfo.print("flpCapabilities:" + sources);
        }

        @Override
        public void onStatusChanged(int status) {
            log("flpStatusChanged: " + status);

            if (status == FLP_STATUS_LOCATION_AVAILABLE) {
                mTextViewFlpStatus.setText("LocationAvailable");
                sInfo.print("flpStatusChanged:LocationAvailable");
            }
            if (status == FLP_STATUS_LOCATION_UNAVAILABLE) {
             mTextViewFlpStatus.setText("LocationUnavailable");
                sInfo.print("flpStatusChanged:LocationUnavailable");
            }
        }

    };

    private void initUi() {
        mTextViewGnss = (TextView) findViewById(R.id.diag_gnss);
        mTextViewWifi = (TextView) findViewById(R.id.diag_wifi);
        mTextViewWifiAp = (TextView) findViewById(R.id.diag_wifi_ap);
        mTextViewSensors = (TextView) findViewById(R.id.diag_sensors);
        mTextViewBt = (TextView) findViewById(R.id.diag_bt);
        mTextViewBtAp = (TextView) findViewById(R.id.diag_bt_ap);
        mTextViewFlp = (TextView) findViewById(R.id.diag_flp);
        mTextViewFlpCapabilities = (TextView) findViewById(R.id.diag_flp_capabilites);
        mTextViewFlpStatus = (TextView) findViewById(R.id.diag_flp_status);
        mButtonClear = (Button) findViewById(R.id.diag_clear);
        mButtonLoopBackTest = (Button) findViewById(R.id.diag_loop_back);
        mTextViewLog = (TextView) findViewById(R.id.diag_log);
        mTextViewLogCount = (TextView) findViewById(R.id.diag_log_count);

        sInfo.updateUiThread();
        sInfo.setTextView(mTextViewLog);

        mTextViewGnss.setText("no update");
        mTextViewWifi.setText("no update");
        mTextViewWifiAp.setText("no update");
        mTextViewSensors.setText("no update");
        mTextViewBt.setText("no update");
        mTextViewBtAp.setText("no update");
        mTextViewFlp.setText("no update");
        mTextViewFlpCapabilities.setText("no update");
        mTextViewFlpStatus.setText("no update");
    }

    private void initUiListeners() {
        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mLogCount = 0;
                mTextViewLogCount.setText("");
                sInfo.clear();
            }
        });
        mButtonLoopBackTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHardwareReady) {
                    String data = "LPB_CMD=1234567890";
                    log("injectDiagnosticData data=[" + data
                            + "], LoopBackTest");
                    mHardware.injectDiagnosticData(data);
                }

                // // TODO demo
                // 25.084, 121.56
                // mSink.onDiagnosticDataAvailable("aaaa\rddd");
                // mSink.onDiagnosticDataAvailable("bbb");
                // mSink.onDiagnosticDataAvailable("GNSS_NTF=gnss");
                // mSink.onDiagnosticDataAvailable("RTT_NTF=rtt");
                // mSink.onDiagnosticDataAvailable("WIFI_AP_NTF=SSID::ABCDEFG\tLAT::25.084\tLNG::121.56\nSSID::defg\tLAT::25.083\tLNG::121.56");
                // mSink.onDiagnosticDataAvailable("BT_AP_NTF=MAC::12345\tLAT::25.084\tLNG::121.55\nMAC::45678\tLAT::25.083\tLNG::121.54");
                // mSink.onDiagnosticDataAvailable("SENS_NTF=sens");
                // mSink.onDiagnosticDataAvailable("BT_NTF=bt");
                // mSink.onDiagnosticDataAvailable("FLP_NTF=flp");
                // mSink.onDiagnosticDataAvailable("STEP_NTF=COUNT::"
                // + new Random().nextInt() + "\tLENGTH::10");
                // mSink.onCapabilities(1);
                // mSink.onStatusChanged(0);
            }
        });
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

    public static void log(Object msg) {
        Log.d(TAG, "" + msg);
    }

    public static void loge(Object msg) {
        Log.d(TAG, "ERR: " + msg);
        sInfo.print(0xffff0000, "" + msg);
    }
}
