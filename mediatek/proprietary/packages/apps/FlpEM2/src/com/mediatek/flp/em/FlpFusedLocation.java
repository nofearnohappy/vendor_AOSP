package com.mediatek.flp.em;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.location.provider.FusedLocationHardware;
import com.android.location.provider.FusedLocationHardwareSink;
import com.android.location.provider.GmsFusedBatchOptions;
import com.android.location.provider.GmsFusedBatchOptions.BatchFlags;
import com.android.location.provider.GmsFusedBatchOptions.SourceTechnologies;
import com.mediatek.flp.service.FusedLocationService;
import com.mediatek.flp.util.MyToast;
import com.mediatek.flp.util.TextStringList;

//TODO implement callback information to UI

public class FlpFusedLocation extends Activity {
    public final static String TAG = "FlpEM2.FlpFusedLocation";
    public final static int MAX_SESSION_SIZE = 10;
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

    TextView mTextViewFusedLocationSupported;
    TextView mTextViewSupportBatchSize;
    TextView mTextViewSupportsDeviceContextInjection;
    TextView mTextViewSupportsDiagnosticDataInjection;
    Button mButtonSession;
    Button mButtonSessionEdit;
    ToggleButton mToggleButtonStart;
    TextView mTextViewSessionId;
    TextView mTextViewMaxPower;
    TextView mTextViewSmallestDisplacement;
    TextView mTextViewPeriod;
    TextView mTextViewSource;
    TextView mTextViewBatchFlag;
    TextView mTextViewStartedId;
    Button mButtonStopAll;
    Button mButtonClearInfo;
    Button mButtonGetLocation;
    Button mButtonFlushLocations;
    TextView mTextViewInfo;
    TextView mTextViewLocationCount;
    TextView mTextViewLocationCallback;

    PopupMenu mPopupSessionSelect;
    PopupMenu mPopupGetLocation;

    static TextStringList sInfo = new TextStringList(8);
    TextStringList mLocationInfo = new TextStringList(8);
    MyToast mToast;

    int mViewSession = 0;
    FusedSession mSessions[] = new FusedSession[MAX_SESSION_SIZE];
    boolean mHardwareReady;
    FusedLocationHardware mHardware;
    int mLocationCount;
    int mSupportedBatchSize;
    boolean mSupportsDeviceContextInject;
    boolean mSupportsDiagnosticDataInject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fused);

        mToast = new MyToast(getApplicationContext());
        mHardwareReady = FusedLocationService.isHardwareReady();
        log("is Fused Location Hardware ready=" + mHardwareReady);
        if (mHardwareReady) {
            mHardware = FusedLocationService.hardware();
            mSupportedBatchSize = mHardware.getSupportedBatchSize();
            mSupportsDeviceContextInject = mHardware
                    .supportsDeviceContextInjection();
            mSupportsDiagnosticDataInject = mHardware
                    .supportsDiagnosticDataInjection();
            mHardware.registerSink(mSink, getMainLooper());
            if (mHardware.supportsDiagnosticDataInjection()) {
                String data = "ENA_CMD=1";
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeStartedSessions();

        if (mHardwareReady) {
            String data = "ENA_CMD=0";
            log("injectDiagnosticData data=[" + data + "]");
            mHardware.injectDiagnosticData(data);
            mHardware.unregisterSink(mSink);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void initSession() {
        for (int i = 0; i < MAX_SESSION_SIZE; i++) {
            mSessions[i] = new FusedSession(i);
            GmsFusedBatchOptions option = mSessions[i].mOption;
            option.setMaxPowerAllocationInMW(1000);
            option.setSmallestDisplacementMeters(0);
            option.setPeriodInNS(1000000000L);
            option.setSourceToUse(SourceTechnologies.GNSS);
            option.setSourceToUse(SourceTechnologies.WIFI);
            option.setSourceToUse(SourceTechnologies.SENSORS);
            option.setSourceToUse(SourceTechnologies.CELL);
            option.setSourceToUse(SourceTechnologies.BLUETOOTH);
            option.setFlag(BatchFlags.CALLBACK_ON_LOCATION_FIX);
        }
    }

    private void initUi() {
        mTextViewFusedLocationSupported = (TextView) findViewById(R.id.supports_fused_location);
        mTextViewSupportBatchSize = (TextView) findViewById(R.id.supported_batch_size);
        mTextViewSupportsDeviceContextInjection = (TextView) findViewById(R.id.supports_device_context_injection);
        mTextViewSupportsDiagnosticDataInjection = (TextView) findViewById(R.id.supports_diagnostic_data_injection);
        mButtonSession = (Button) findViewById(R.id.fused_session);
        mButtonSessionEdit = (Button) findViewById(R.id.fused_edit);
        mToggleButtonStart = (ToggleButton) findViewById(R.id.fused_start);
        mTextViewSessionId = (TextView) findViewById(R.id.fused_session_id);
        mTextViewMaxPower = (TextView) findViewById(R.id.max_power);
        mTextViewSmallestDisplacement = (TextView) findViewById(R.id.smallest_displacement);
        mTextViewPeriod = (TextView) findViewById(R.id.period);
        mTextViewSource = (TextView) findViewById(R.id.fused_source);
        mTextViewBatchFlag = (TextView) findViewById(R.id.batch_flag);
        mTextViewStartedId = (TextView) findViewById(R.id.fused_started_id);
        mButtonStopAll = (Button) findViewById(R.id.fused_stop_all_sessions);
        mButtonClearInfo = (Button) findViewById(R.id.fused_clear_info);
        mButtonGetLocation = (Button) findViewById(R.id.fused_get_batched_location);
        mButtonFlushLocations = (Button) findViewById(R.id.fused_flush_batched_locations);
        mTextViewInfo = (TextView) findViewById(R.id.fused_textview_info);
        mTextViewLocationCount = (TextView) findViewById(R.id.fused_count);
        mTextViewLocationCallback = (TextView) findViewById(R.id.fused_callback);

        mPopupSessionSelect = new PopupMenu(this, mButtonSession);
        for (int i = 0; i < MAX_SESSION_SIZE; i++) {
            mPopupSessionSelect.getMenu().add(0, i, Menu.NONE, "Session " + i);
        }
        mPopupGetLocation = new PopupMenu(this, mButtonGetLocation);
        for (int i = 0; i < 100; i++) {
            mPopupGetLocation.getMenu().add(0, i, Menu.NONE,
                    "Get " + i + " Locations");
        }

        sInfo.updateUiThread();
        sInfo.setTextView(mTextViewInfo);
        mLocationInfo.updateUiThread();
        mLocationInfo.setTextView(mTextViewLocationCallback);

        applySessionToUi(mSessions[0]);
    }

    private void initUiListeners() {
        mPopupSessionSelect
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        mViewSession = id;
                        applySessionToUi(mSessions[id]);
                        return false;
                    }
                });

        mPopupGetLocation
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int num = item.getItemId();
                        if (mHardwareReady) {
                            log("requestBatchOfLocations num=" + num);
                            sInfo.print("requestBatchOfLocations num=" + num);
                            mHardware.requestBatchOfLocations(num);
                        } else {
                            loge("hardware is not ready");
                        }
                        return false;
                    }
                });

        mButtonSession.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupSessionSelect.show();
            }
        });
        mButtonSessionEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                editSession(mSessions[mViewSession]);
            }
        });
        mToggleButtonStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = mToggleButtonStart.isChecked();
                FusedSession session = mSessions[mViewSession];
                session.mIsStarted = enabled;
                updateUi();
                if (mHardwareReady) {
                    if (enabled) {
                        log("startBatching session=" + session);
                        sInfo.print("startBatching id=" + session.mId);
                        mHardware.startBatching(session.mId, session.mOption);
                    } else {
                        log("stopBatching id=" + session.mId);
                        sInfo.print("stopBatching id=" + session.mId);
                        mHardware.stopBatching(session.mId);
                    }
                } else {
                    loge("hardware is not ready");
                }
            }
        });
        mButtonStopAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                closeStartedSessions();
                updateUi();

                // // TODO demo
                // mapReportLocation(25.080, 121.563, 200);
                // mapReportLocation(25.085, 121.563, 200);
                // mapReportLocation(25.085, 121.570, 200);
                // mapReportLocation(25.080, 121.570, 200);
                //
                // mapReportLocation(25.080, 121.5631, 200);
                // mapReportLocation(25.0851, 121.5632, 200);
                // mapReportLocation(25.0852, 121.5701, 200);
                // mapReportLocation(25.080, 121.5701, 200);
                //
                // mapReportLocation(25.0803, 121.5634, 200);
/*
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(System.currentTimeMillis());

                 Location l1 = new Location("FUSED");
                 l1.setLatitude(24.01);
                 l1.setLongitude(121.123);
                 l1.setTime(cal.getTimeInMillis());
                 l1.setAccuracy(123f);
                 l1.setAltitude(456f);
                 l1.setBearing(789f);
                 l1.setSpeed(555f);
                 Location l2 = new Location("FUSED");
                 l1.setLatitude(24.01);
                 l1.setLongitude(121.123);
                 l1.setTime(cal.getTimeInMillis());
                 l2.setAccuracy(123f);
                 l2.setAltitude(456f);
                 l2.setBearing(789f);
                 l2.setSpeed(555f);
                 mSink.onLocationAvailable(new Location[] { l1, l2 });
*/
            }
        });
        mButtonClearInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sInfo.clear();
                mLocationInfo.clear();
                mLocationCount = 0;
                mTextViewLocationCount.setText("");
            }
        });
        mButtonGetLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupGetLocation.show();
            }
        });
        mButtonFlushLocations.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHardwareReady) {
                    log("flushBatchedLocations");
                    sInfo.print("flushBatchedLocations");
                    mHardware.flushBatchedLocations();
                } else {
                    loge("hardware is not ready");
                }
            }
        });
    }

    private void updateUi() {
        if (mHardwareReady) {
            mTextViewFusedLocationSupported.setTextColor(0xff00ff00);
            mTextViewFusedLocationSupported.setText("Yes");
            log("supportedBatchSize=" + mSupportedBatchSize
                    + " supportsDeviceContextInject="
                    + mSupportsDeviceContextInject
                    + " supportsDiagnosticDataInject="
                    + mSupportsDiagnosticDataInject);
            mTextViewSupportBatchSize.setText("" + mSupportedBatchSize);
            mTextViewSupportsDeviceContextInjection.setText(""
                    + mSupportsDeviceContextInject);
            mTextViewSupportsDiagnosticDataInjection.setText(""
                    + mSupportsDiagnosticDataInject);
        } else {
            mTextViewFusedLocationSupported.setTextColor(0xffff0000);
            mTextViewFusedLocationSupported.setText("No");
            mTextViewSupportBatchSize.setTextColor(0xffff0000);
            mTextViewSupportBatchSize.setText("Unknown");
            mTextViewSupportsDeviceContextInjection.setTextColor(0xffff0000);
            mTextViewSupportsDeviceContextInjection.setText("No");
            mTextViewSupportsDiagnosticDataInjection.setTextColor(0xffff0000);
            mTextViewSupportsDiagnosticDataInjection.setText("No");
        }

        String ids = "";
        for (FusedSession session : mSessions) {
            if (session.mIsStarted) {
                ids += session.mId + ", ";
            }
        }
        mTextViewStartedId.setText(ids);
        mToggleButtonStart.setChecked(mSessions[mViewSession].mIsStarted);
    }

    private void applySessionToUi(FusedSession session) {
        GmsFusedBatchOptions option = session.mOption;
        mToggleButtonStart.setChecked(session.mIsStarted);
        mTextViewSessionId.setText("" + session.mId);
        mTextViewMaxPower.setText(""
                + (long) option.getMaxPowerAllocationInMW());
        mTextViewSmallestDisplacement.setText(""
                + (long) option.getSmallestDisplacementMeters());
        mTextViewPeriod.setText("" + option.getPeriodInNS() / 1000000000);
        String sources = "";
        if (option.isSourceToUseSet(SourceTechnologies.GNSS)) {
            sources += "GNSS, ";
        }
        if (option.isSourceToUseSet(SourceTechnologies.WIFI)) {
            sources += "WIFI, ";
        }
        if (option.isSourceToUseSet(SourceTechnologies.SENSORS)) {
            sources += "SENSORS, ";
        }
        if (option.isSourceToUseSet(SourceTechnologies.CELL)) {
            sources += "CELL, ";
        }
        if (option.isSourceToUseSet(SourceTechnologies.BLUETOOTH)) {
            sources += "BT, ";
        }
        mTextViewSource.setText(sources);
        String flag = "";
        if (option.isFlagSet(BatchFlags.CALLBACK_ON_LOCATION_FIX)) {
            flag = "Callback on location fix";
        } else if (option.isFlagSet(BatchFlags.WAKEUP_ON_FIFO_FULL)) {
            flag = "Wakeup on FIFO full";
        } else {
            flag = "None";
        }
        mTextViewBatchFlag.setText(flag);
    }

    private void closeStartedSessions() {
        for (FusedSession session : mSessions) {
            if (session.mIsStarted) {
                session.mIsStarted = false;
                if (mHardwareReady) {
                    log("stopBatching id=" + session.mId);
                    sInfo.print("stopBatching id=" + session.mId);
                    mHardware.stopBatching(session.mId);
                } else {
                    loge("hardware is not ready");
                }
            }
        }
    }

    private void editSession(final FusedSession session) {
        final GmsFusedBatchOptions option = session.mOption;
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Edit Fused Location");
        dialog.setContentView(R.layout.fused_edit);

        TextView editId = (TextView) dialog.findViewById(R.id.edit_session_id);
        final EditText editMaxPower = (EditText) dialog
                .findViewById(R.id.edit_max_power_allocation_in_mw);
        final EditText editSmallestDisplacement = (EditText) dialog
                .findViewById(R.id.edit_smallest_displacement_meters);
        final EditText editPeriod = (EditText) dialog
                .findViewById(R.id.edit_period);
        final CheckBox editGnss = (CheckBox) dialog
                .findViewById(R.id.edit_gnss);
        final CheckBox editWifi = (CheckBox) dialog
                .findViewById(R.id.edit_wifi);
        final CheckBox editSensors = (CheckBox) dialog
                .findViewById(R.id.edit_sensors);
        final CheckBox editCell = (CheckBox) dialog
                .findViewById(R.id.edit_cell);
        final CheckBox editBt = (CheckBox) dialog.findViewById(R.id.edit_bt);
        final RadioButton editNone = (RadioButton) dialog
                .findViewById(R.id.edit_none);
        final RadioButton editCallbackOnLocationFix = (RadioButton) dialog
                .findViewById(R.id.edit_callback_on_location_fix);
        final RadioButton editWakeupOnFifoFull = (RadioButton) dialog
                .findViewById(R.id.edit_wakeup_on_fifo_full);
        Button ok = (Button) dialog.findViewById(R.id.edit_ok);
        Button cancel = (Button) dialog.findViewById(R.id.edit_cancel);

        editId.setText("" + session.mId);
        editMaxPower.setText("" + (long) option.getMaxPowerAllocationInMW());
        editId.setText("" + session.mId);
        editSmallestDisplacement.setText("" + (long) option.getSmallestDisplacementMeters());
        editPeriod.setText("" + option.getPeriodInNS() / 1000000000);
        if (option.isSourceToUseSet(SourceTechnologies.GNSS)) {
            editGnss.setChecked(true);
        }
        if (option.isSourceToUseSet(SourceTechnologies.WIFI)) {
            editWifi.setChecked(true);
        }
        if (option.isSourceToUseSet(SourceTechnologies.SENSORS)) {
            editSensors.setChecked(true);
        }
        if (option.isSourceToUseSet(SourceTechnologies.CELL)) {
            editCell.setChecked(true);
        }
        if (option.isSourceToUseSet(SourceTechnologies.BLUETOOTH)) {
            editBt.setChecked(true);
        }
        if (option.isFlagSet(BatchFlags.CALLBACK_ON_LOCATION_FIX)) {
            editCallbackOnLocationFix.setChecked(true);
        } else if (option.isFlagSet(BatchFlags.WAKEUP_ON_FIFO_FULL)) {
            editWakeupOnFifoFull.setChecked(true);
        } else {
            editNone.setChecked(true);
        }

        ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // invalid input check
                if (!isDoubleValid(editMaxPower)) {
                    mToast.show("the value of Max Power Allocation is invalid");
                    return;
                }
                if (!isFloatValid(editSmallestDisplacement)) {
                    mToast.show("the value of Smallest Displacement is invalid");
                    return;
                }
                if (!isIntValid(editPeriod)) {
                    mToast.show("The value of Period is invalid");
                    return;
                }

                // start to assign the new value to this session
                option.setMaxPowerAllocationInMW(Double.valueOf(editMaxPower
                        .getText().toString()));
                option.setSmallestDisplacementMeters(Float.valueOf(editSmallestDisplacement
                        .getText().toString()));
                option.setPeriodInNS(Long.valueOf(editPeriod.getText()
                        .toString()) * 1000000000);
                if (editGnss.isChecked()) {
                    option.setSourceToUse(SourceTechnologies.GNSS);
                } else {
                    option.resetSourceToUse(SourceTechnologies.GNSS);
                }
                if (editWifi.isChecked()) {
                    option.setSourceToUse(SourceTechnologies.WIFI);
                } else {
                    option.resetSourceToUse(SourceTechnologies.WIFI);
                }
                if (editSensors.isChecked()) {
                    option.setSourceToUse(SourceTechnologies.SENSORS);
                } else {
                    option.resetSourceToUse(SourceTechnologies.SENSORS);
                }
                if (editCell.isChecked()) {
                    option.setSourceToUse(SourceTechnologies.CELL);
                } else {
                    option.resetSourceToUse(SourceTechnologies.CELL);
                }
                if (editBt.isChecked()) {
                    option.setSourceToUse(SourceTechnologies.BLUETOOTH);
                } else {
                    option.resetSourceToUse(SourceTechnologies.BLUETOOTH);
                }
                if (editNone.isChecked()) {
                    option.resetFlag(BatchFlags.CALLBACK_ON_LOCATION_FIX);
                    option.resetFlag(BatchFlags.WAKEUP_ON_FIFO_FULL);
                } else if (editCallbackOnLocationFix.isChecked()) {
                    option.setFlag(BatchFlags.CALLBACK_ON_LOCATION_FIX);
                    option.resetFlag(BatchFlags.WAKEUP_ON_FIFO_FULL);
                } else if (editWakeupOnFifoFull.isChecked()) {
                    option.resetFlag(BatchFlags.CALLBACK_ON_LOCATION_FIX);
                    option.setFlag(BatchFlags.WAKEUP_ON_FIFO_FULL);
                }
                if (session.mIsStarted) {
                    if (mHardwareReady) {
                        log("updateBatchingOptions session=" + session);
                        sInfo.print("updateBatchingOptions id=" + session.mId);
                        mHardware.updateBatchingOptions(session.mId, option);
                    } else {
                        loge("hardware is not ready");
                    }
                }
                applySessionToUi(session);
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean isDoubleValid(EditText input) {
        try {
            Double.valueOf(input.getText().toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isIntValid(EditText input) {
        try {
            Integer.valueOf(input.getText().toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isFloatValid(EditText input) {
        try {
            Float.valueOf(input.getText().toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private FusedLocationHardwareSink mSink = new FusedLocationHardwareSink() {

        @Override
        public void onDiagnosticDataAvailable(String data) {
        }

        @Override
        public void onLocationAvailable(Location[] locations) {
            mLocationCount++;
            mTextViewLocationCount.setText("count=" + mLocationCount);
            for (int i = 0; i < locations.length; i++) {
                Location l = locations[i];
                StringBuilder o = new StringBuilder();
                o.append("i=" + i + " [" + l.getProvider() + "] " + " "
                        + l.getTime() + " " + l.getLatitude() + ","
                        + l.getLongitude() + " ");
                if (l.hasAccuracy()) {
                    o.append("acc=" + l.getAccuracy() + " ");
                }
                if (l.hasAltitude()) {
                    o.append("atl=" + l.getAltitude() + " ");
                }
                if (l.hasSpeed()) {
                    o.append("speed=" + l.getSpeed() + " ");
                }
                if (l.hasBearing()) {
                    o.append("bearing=" + l.getBearing() + " ");
                }
                mLocationInfo.print(o.toString());
                log(o.toString());

                mapReportLocation(l.getLatitude(), l.getLongitude(),
                        l.hasAccuracy() ? (int) l.getAccuracy() : 0);
            }
        }

        @Override
        public void onCapabilities(int capabilities) {
            log("onCapabilities:" + capabilities);

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
            sInfo.print("onCapabilities:" + sources);
        }

        @Override
        public void onStatusChanged(int status) {
            log("onStatusChanged: " + status);

            if (status == FLP_STATUS_LOCATION_AVAILABLE) {
                sInfo.print("onStatusChanged:LocationAvailable");
            }
            if (status == FLP_STATUS_LOCATION_UNAVAILABLE) {
                sInfo.print("onStatusChanged:LocationUnavailable");
            }
        }

    };

    private class FusedSession {
        int mId;
        boolean mIsStarted;
        GmsFusedBatchOptions mOption;

        public String toString() {
            StringBuilder o = new StringBuilder();
            o.append("id=[" + mId + "] ");
            o.append("isStarted=[" + mIsStarted + "] ");
            o.append("power=[" + mOption.getMaxPowerAllocationInMW() + "] ");
            o.append("displacement=[" + mOption.getSmallestDisplacementMeters() + "] ");
            o.append("period=[" + mOption.getPeriodInNS() + "] ");
            o.append("source=[");
            if (mOption.isSourceToUseSet(SourceTechnologies.GNSS)) {
                o.append("GNSS, ");
            }
            if (mOption.isSourceToUseSet(SourceTechnologies.WIFI)) {
                o.append("WIFI, ");
            }
            if (mOption.isSourceToUseSet(SourceTechnologies.SENSORS)) {
                o.append("SENSORS, ");
            }
            if (mOption.isSourceToUseSet(SourceTechnologies.CELL)) {
                o.append("CELL, ");
            }
            if (mOption.isSourceToUseSet(SourceTechnologies.BLUETOOTH)) {
                o.append("BT, ");
            }
            o.append("] ");
            o.append("flag=[");
            if (mOption.isFlagSet(BatchFlags.CALLBACK_ON_LOCATION_FIX)) {
                o.append("callback on location fix");
            } else if (mOption.isFlagSet(BatchFlags.WAKEUP_ON_FIFO_FULL)) {
                o.append("wakeup on FIFO full");
            } else {
                o.append("none");
            }
            o.append("] ");
            return o.toString();
        }

        public FusedSession(int id) {
            mId = id;
            mIsStarted = false;
            mOption = new GmsFusedBatchOptions();
        }
    }

    public void mapReportLocation(double latitude, double longitude,
            int accuracy) {
        Intent intent = new Intent(FlpMap.REPORT_LOCATION);
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        intent.putExtra("acc", accuracy);
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
