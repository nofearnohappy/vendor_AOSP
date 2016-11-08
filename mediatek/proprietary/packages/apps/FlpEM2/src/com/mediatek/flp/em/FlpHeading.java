package com.mediatek.flp.em;

import com.android.location.provider.FusedLocationHardware;
import com.android.location.provider.FusedLocationHardwareSink;
import com.android.location.provider.GmsFusedBatchOptions;
import com.android.location.provider.GmsFusedBatchOptions.BatchFlags;
import com.android.location.provider.GmsFusedBatchOptions.SourceTechnologies;
import com.mediatek.flp.service.FusedLocationService;
import com.mediatek.flp.util.TextStringList;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

public class FlpHeading extends Activity {
    public final static String TAG = "FlpEM2.FlpHeading";

    TextView mTextViewSource;
    TextView mTextViewStartedId;
    ToggleButton mToggleButtonStart;
    Button mButtonGetPointB;
    TextView mTextViewInfo;
    TextView mTextViewHeadingError;

    static TextStringList sInfo = new TextStringList(8);
    FusedSession mSessions[] = new FusedSession[1];
    boolean mHardwareReady;
    FusedLocationHardware mHardware;
    boolean mSupportsDiagnosticDataInject;
    Location mLastLocation;
    Location mFirstLocation;
    Location mSecondLocation;
    Location mThirdLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heading);

        mHardwareReady = FusedLocationService.isHardwareReady();
        log("is Fused Location Hardware ready=" + mHardwareReady);
        if (mHardwareReady) {
            mHardware = FusedLocationService.hardware();
            mSupportsDiagnosticDataInject = mHardware
                    .supportsDiagnosticDataInjection();
            mHardware.registerSink(mSink, getMainLooper());
            if (mHardware.supportsDiagnosticDataInjection()) {
                String data = "SET_PROPERTY=SENSOR_INIT_POS::1";
                log("injectDiagnosticData data=[" + data + "]");
                mHardware.injectDiagnosticData(data);
            } else {
                loge("diagnostic data injection is not support");
            }
        }
        initSession();
        initUi();
        initUiListeners();
    }

    private void initSession() {
        int i = 0;
        mSessions[i] = new FusedSession(i);
        GmsFusedBatchOptions option = mSessions[i].mOption;
        option.setMaxPowerAllocationInMW(1000);
        option.setPeriodInNS(1000000000L);
        option.setSourceToUse(SourceTechnologies.SENSORS);
        option.setFlag(BatchFlags.CALLBACK_ON_LOCATION_FIX);
    }

    private class FusedSession {
        int mId = 10;
        boolean mIsStarted;
        GmsFusedBatchOptions mOption;

        public String toString() {
            StringBuilder o = new StringBuilder();
            o.append("id=[" + mId + "] ");
            o.append("isStarted=[" + mIsStarted + "] ");
            o.append("source=[");
            if (mOption.isSourceToUseSet(SourceTechnologies.SENSORS)) {
                o.append("SENSORS");
            }
            o.append("] ");

            return o.toString();
        }

        public FusedSession(int id) {
            mId = 10;
            mIsStarted = false;
            mOption = new GmsFusedBatchOptions();
        }
    }

    private void initUi() {
        mTextViewSource = (TextView) findViewById(R.id.fused_source);
        mTextViewStartedId = (TextView) findViewById(R.id.fused_started_id);
        mToggleButtonStart = (ToggleButton) findViewById(R.id.heading_start);
        mButtonGetPointB = (Button) findViewById(R.id.heading_get_point_B);
        mTextViewInfo = (TextView) findViewById(R.id.fused_textview_info);
        mTextViewHeadingError = (TextView) findViewById(R.id.fused_textview_heading_error);

        mButtonGetPointB.setEnabled(false);
        mTextViewStartedId.setText("");
        sInfo.updateUiThread();
        sInfo.setTextView(mTextViewInfo);
    }

    private void initUiListeners() {

        mToggleButtonStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = mToggleButtonStart.isChecked();
                FusedSession session = mSessions[0];
                session.mIsStarted = enabled;
                if (mHardwareReady) {
                    if (enabled) {
                        updateUi();
                        mapThreeLocationClear();
                        sInfo.clear();
                        mTextViewHeadingError.setText("");
                        mButtonGetPointB.setEnabled(true);
                        log("startBatching session=" + session);
                        sInfo.print("startBatching id=" + session.mId);
                        mHardware.startBatching(session.mId, session.mOption);
                        mFirstLocation = null;
                    } else {
                        //Get Point C
                        StringBuilder o = new StringBuilder();
                        if (mLastLocation != null) {
                            log("Get point C Location");
                            o.append(" [" + mLastLocation.getProvider() + "] " + " "
                                + mLastLocation.getTime() + " " + mLastLocation.getLatitude() + ","
                                + mLastLocation.getLongitude() + " ");
                            if (mLastLocation.hasAccuracy()) {
                                o.append("acc=" + mLastLocation.getAccuracy() + " ");
                            }
                            if (mLastLocation.hasAltitude()) {
                                o.append("atl=" + mLastLocation.getAltitude() + " ");
                            }
                            if (mLastLocation.hasSpeed()) {
                                o.append("speed=" + mLastLocation.getSpeed() + " ");
                            }
                            if (mLastLocation.hasBearing()) {
                                o.append("bearing=" + mLastLocation.getBearing() + " ");
                            }
                            log(o.toString());

                            mThirdLocation = mLastLocation;
                            mapThreeLocation(mThirdLocation.getLatitude(), mThirdLocation.getLongitude());
                            sInfo.print("GetPointC Location= (" + mThirdLocation.getLatitude()+ ","
                                    + mThirdLocation.getLongitude() + ")");
                        }
                        headingError();
                        mButtonGetPointB.setEnabled(false);
                        mTextViewStartedId.setText("");
                        log("stopBatching id=" + session.mId);
                        sInfo.print("stopBatching id=" + session.mId);
                        mHardware.stopBatching(session.mId);
                    }
                } else {
                    loge("hardware is not ready");
                }
            }
        });
        mButtonGetPointB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHardwareReady) {
                    StringBuilder o = new StringBuilder();
                    if (mLastLocation != null) {
                        log("Get point B Location");
                        o.append(" [" + mLastLocation.getProvider() + "] " + " "
                            + mLastLocation.getTime() + " " + mLastLocation.getLatitude() + ","
                            + mLastLocation.getLongitude() + " ");
                        if (mLastLocation.hasAccuracy()) {
                            o.append("acc=" + mLastLocation.getAccuracy() + " ");
                        }
                        if (mLastLocation.hasAltitude()) {
                            o.append("atl=" + mLastLocation.getAltitude() + " ");
                        }
                        if (mLastLocation.hasSpeed()) {
                            o.append("speed=" + mLastLocation.getSpeed() + " ");
                        }
                        if (mLastLocation.hasBearing()) {
                            o.append("bearing=" + mLastLocation.getBearing() + " ");
                        }
                        log(o.toString());

                        mSecondLocation = mLastLocation;
                        mapThreeLocation(mSecondLocation.getLatitude(), mSecondLocation.getLongitude());
                        sInfo.print("GetPointB Location= (" + mSecondLocation.getLatitude()+ ","
                                + mSecondLocation.getLongitude() + ")");
                    }
                    mButtonGetPointB.setEnabled(false);
                } else {
                    loge("hardware is not ready");
                }
            }
        });
    }

    private void headingError() {
        if (mSecondLocation != null) {
            double latitudeA = mFirstLocation.getLatitude();
            double longitudeA = mFirstLocation.getLongitude();
            double latitudeB = mSecondLocation.getLatitude();
            double longitudeB = mSecondLocation.getLongitude();
            double latitudeC = mThirdLocation.getLatitude();
            double longitudeC = mThirdLocation.getLongitude();

            double distanceAB = Math.sqrt(Math.pow((latitudeA - latitudeB), 2)
                + Math.pow((longitudeA - longitudeB), 2));
            double distanceBC = Math.sqrt(Math.pow((latitudeB - latitudeC), 2)
                + Math.pow((longitudeB - longitudeC), 2));
            double distanceAC = Math.sqrt(Math.pow((latitudeA - latitudeC), 2)
                + Math.pow((longitudeA - longitudeC), 2));

            if ((distanceAB != 0) || (distanceBC != 0)) {
                double angle = Math.acos((Math.pow(distanceAB, 2) + Math.pow(distanceBC, 2)
                    - Math.pow(distanceAC, 2)) / (2 * distanceAB * distanceBC));
                sInfo.print("Heading error=" + Math.toDegrees(angle));
                mTextViewHeadingError.setText("" + Math.toDegrees(angle));
            } else {
                Toast.makeText(this, "distance can't be zero", Toast.LENGTH_LONG).show();
                loge("distance can't be zero");
            }
        } else {
            Toast.makeText(this, "distance can't be zero", Toast.LENGTH_LONG).show();
            loge("SecondLocation can't be zero");
        }
    }

    private void updateUi() {
        if (mHardwareReady) {
            mToggleButtonStart.setChecked(mSessions[0].mIsStarted);
            mTextViewSource.setText("SENSORS");
            mTextViewStartedId.setText("10");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //closeStartedSessions();

        if (mHardwareReady) {
            String data = "SET_PROPERTY=SENSOR_INIT_POS::0";
            log("injectDiagnosticData data=[" + data + "]");
            mHardware.injectDiagnosticData(data);
            mHardware.unregisterSink(mSink);
        }
    }

    private FusedLocationHardwareSink mSink = new FusedLocationHardwareSink() {

        @Override
        public void onDiagnosticDataAvailable(String data) {
        }

        @Override
        public void onLocationAvailable(Location[] locations) {
            if (locations.length <= 0) {
                return;
            }
            mLastLocation = locations[0];
   if (mFirstLocation == null) {
                log("Get point A Location");
                mFirstLocation = mLastLocation;
                mapThreeLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                sInfo.print("GetPointA Location= (" + mFirstLocation.getLatitude()+ ","
                        + mFirstLocation.getLongitude() + ")");
            }

            for (int i = 1; i < locations.length; i++) {
                Location mLastLocation = locations[i];
                StringBuilder o = new StringBuilder();
                o.append("i=" + i + " [" + mLastLocation.getProvider() + "] " + " "
                        + mLastLocation.getTime() + " " + mLastLocation.getLatitude() + ","
                        + mLastLocation.getLongitude() + " ");
                if (mLastLocation.hasAccuracy()) {
                    o.append("acc=" + mLastLocation.getAccuracy() + " ");
                }
                if (mLastLocation.hasAltitude()) {
                    o.append("atl=" + mLastLocation.getAltitude() + " ");
                }
                if (mLastLocation.hasSpeed()) {
                    o.append("speed=" + mLastLocation.getSpeed() + " ");
                }
                if (mLastLocation.hasBearing()) {
                    o.append("bearing=" + mLastLocation.getBearing() + " ");
                }
                log(o.toString());

                mapReportLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                  mLastLocation.hasAccuracy() ? (int) mLastLocation.getAccuracy() : 0);
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

    public static void log(Object msg) {
        Log.d(TAG, "" + msg);
    }

    public static void loge(Object msg) {
        Log.d(TAG, "ERR: " + msg);
        sInfo.print(0xffff0000, "" + msg);
    }
}
