package com.mediatek.flp.em;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.location.GeofenceHardware;
import android.hardware.location.GeofenceHardwareCallback;
import android.hardware.location.GeofenceHardwareMonitorCallback;
import android.hardware.location.GeofenceHardwareMonitorEvent;
import android.hardware.location.GeofenceHardwareRequest;
import android.location.Location;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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

import com.mediatek.flp.service.GeofenceService;
import com.mediatek.flp.util.GeofenceXmlParser;
import com.mediatek.flp.util.MyToast;
import com.mediatek.flp.util.TextStringList;

public class FlpGeofence extends Activity {
    public final static String TAG = "FlpEM2.FlpGeofence";
    public final static int MAX_GEOFENCE_SIZE = 100;
    public final static int FUSED = GeofenceHardware.MONITORING_TYPE_FUSED_HARDWARE;
    public final static String PROFILE_NAME = "geofence_profiles.xml";

    TextView mTextViewSupportsGeofence;
    TextView mTextViewSupportsSystem;
    Button mButtonLoad;
    Button mButtonSave;
    TextView mTextViewConfigInfo;
    Button mButtonIdSelect;
    Button mButtonEdit;
    ToggleButton mToggleButtonStart1;
    ToggleButton mToggleButtonStart2;
    TextView mTextViewId;
    TextView mTextViewLatitude;
    TextView mTextViewLongitude;
    TextView mTextViewRadius;
    TextView mTextViewLastTransition;
    TextView mTextViewUnknownTimer;
    TextView mTextViewMonitorTransitions;
    TextView mTextViewResponsiveness;
    TextView mTextViewSource;
    TextView mTextViewStatus;
    Button mButtonStopAllSessions;
    Button mButtonClearInfo;
    TextView mTextViewInfo;
    TextView mTextViewGeofenceCount;
    TextView mTextViewGeofenceCallback;
    TextView mTextViewHardwareCount;
    TextView mTextViewHardwareCallback;

    PopupMenu mPopupGeofenceSelect;
    PopupMenu mPopupGeofenceProfile;

    static TextStringList sInfo = new TextStringList(8);
    TextStringList mGeofenceCallback = new TextStringList(8);
    TextStringList mHardwareCallback = new TextStringList(8);

    int mViewGeofence;
    static GeofenceSession mSessions[] = new GeofenceSession[MAX_GEOFENCE_SIZE];
    MyToast mToast;
    int mGeofenceCount;
    int mHardwareCount;
    boolean mHardwareReady;
    GeofenceHardware mHardware;
    GeofenceXmlParser mXml;
    int mTypes[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geofence);

        mToast = new MyToast(getApplicationContext());
        mHardwareReady = GeofenceService.isHardwareReady();
        log("is Geofence Hardware ready=" + mHardwareReady);
        if (mHardwareReady) {
            mHardware = GeofenceService.hardware();
            mTypes = mHardware.getMonitoringTypes();
            if (!mHardware.registerForMonitorStateChangeCallback(FUSED,
                    mMonitorCallback)) {
                loge("registerForMonitorStateChangeCallback failed");
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
            mHardware.unregisterForMonitorStateChangeCallback(FUSED,
                    mMonitorCallback);
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
        for (int i = 0; i < MAX_GEOFENCE_SIZE; i++) {
            mSessions[i] = new GeofenceSession(i);
            double latitude = 0;
            double longitude = 0;
            double radius = 0; // m
            int lastTransition = GeofenceHardware.GEOFENCE_UNCERTAIN;
            int unknownTimer = 30000; // ms
            int monitorTransitions = GeofenceHardware.GEOFENCE_ENTERED
                    | GeofenceHardware.GEOFENCE_EXITED
                    | GeofenceHardware.GEOFENCE_UNCERTAIN;
            int notificationResponsiveness = 5000; // ms
            int sourceTechnologies = GeofenceHardware.SOURCE_TECHNOLOGY_GNSS
                    | GeofenceHardware.SOURCE_TECHNOLOGY_WIFI
                    | GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS
                    | GeofenceHardware.SOURCE_TECHNOLOGY_CELL
                    | GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH;
            mSessions[i].createCircularGeofence(latitude, longitude, radius,
                    lastTransition, unknownTimer, monitorTransitions,
                    notificationResponsiveness, sourceTechnologies);
        }

        copyFileFromAssets(PROFILE_NAME, getGeofenceProfilePath());

        mXml = new GeofenceXmlParser();
        try {
            mXml.load(getGeofenceProfilePath());
            if (mXml.isProfileExist(mXml.getDefaultnName())) {
                mXml.updateGeofences(mSessions, mXml.getDefaultnName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUi() {
        mTextViewSupportsGeofence = (TextView) findViewById(R.id.supports_geofence);
        mTextViewSupportsSystem = (TextView) findViewById(R.id.supports_geofence_system);
        mButtonLoad = (Button) findViewById(R.id.geofence_load);
        mButtonSave = (Button) findViewById(R.id.geofence_save);
        mTextViewConfigInfo = (TextView) findViewById(R.id.geofence_config_info);
        mButtonIdSelect = (Button) findViewById(R.id.geofence_id_select);
        mButtonEdit = (Button) findViewById(R.id.geofence_edit);
        mToggleButtonStart1 = (ToggleButton) findViewById(R.id.geofence_start1);
        mToggleButtonStart2 = (ToggleButton) findViewById(R.id.geofence_start2);
        mTextViewId = (TextView) findViewById(R.id.geofence_id);
        mTextViewLatitude = (TextView) findViewById(R.id.geofence_latitude);
        mTextViewLongitude = (TextView) findViewById(R.id.geofence_longitude);
        mTextViewRadius = (TextView) findViewById(R.id.geofence_radius);
        mTextViewLastTransition = (TextView) findViewById(R.id.geofence_last_transition);
        mTextViewUnknownTimer = (TextView) findViewById(R.id.geofence_unkonwn_timer);
        mTextViewMonitorTransitions = (TextView) findViewById(R.id.geofence_monitor_transitions);
        mTextViewResponsiveness = (TextView) findViewById(R.id.geofence_notifiction_responsiveness);
        mTextViewSource = (TextView) findViewById(R.id.geofence_source);
        mTextViewStatus = (TextView) findViewById(R.id.geofence_session_status);
        mButtonStopAllSessions = (Button) findViewById(R.id.geofence_stop_all_sessions);
        mButtonClearInfo = (Button) findViewById(R.id.geofence_clear_info);
        mTextViewInfo = (TextView) findViewById(R.id.geofence_info);
        mTextViewGeofenceCount = (TextView) findViewById(R.id.geofence_count);
        mTextViewGeofenceCallback = (TextView) findViewById(R.id.geofence_callback);
        mTextViewHardwareCount = (TextView) findViewById(R.id.geofence_hardware_monitor_count);
        mTextViewHardwareCallback = (TextView) findViewById(R.id.geofence_hardware_monitor_callback);

        mPopupGeofenceSelect = new PopupMenu(this, mButtonIdSelect);
        for (int i = 0; i < MAX_GEOFENCE_SIZE; i++) {
            mPopupGeofenceSelect.getMenu()
                    .add(0, i, Menu.NONE, "Geofence " + i);
        }
        mPopupGeofenceProfile = new PopupMenu(this, mButtonLoad);
        for (String name : mXml.getAllNames()) {
            mPopupGeofenceProfile.getMenu().add(0, 0, Menu.NONE, name);
        }

        sInfo.updateUiThread();
        sInfo.setTextView(mTextViewInfo);
        mGeofenceCallback.updateUiThread();
        mGeofenceCallback.setTextView(mTextViewGeofenceCallback);
        mHardwareCallback.updateUiThread();
        mHardwareCallback.setTextView(mTextViewHardwareCallback);

        applyGeofenceToUi(mSessions[0]);
    }

    private void initUiListeners() {
        mPopupGeofenceSelect
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        mViewGeofence = id;
                        applyGeofenceToUi(mSessions[mViewGeofence]);
                        return false;
                    }
                });
        mPopupGeofenceProfile
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String name = item.getTitle().toString();
                        mXml.updateGeofences(mSessions, name);
                        mTextViewConfigInfo.setText("use [" + name + "]");

                        for (GeofenceSession s : mSessions) {
                            if (s.mState == GeofenceState.ON) {
                                addGeofence(s);
                            } else if (s.mState == GeofenceState.PAUSE) {
                                if (mHardwareReady) {
                                    addGeofence(s);
                                    pauseGeofence(s);
                                }
                            }
                        }
                        updateUi();
                        return false;
                    }
                });

        mButtonLoad.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAnySessionStarted()) {
                    new AlertDialog.Builder(FlpGeofence.this)
                            .setTitle("Notice")
                            .setMessage(
                                    "Before applying the Geofence Profile, the ongoing sessions must be stopped.\n"
                                            + "Do you want to stop ongoing session(s)?")
                            .setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            log("stop ongoing session for loading profile");
                                            closeStartedSessions();
                                            updateUi();
                                            loadProfile();
                                        }
                                    }).setNegativeButton("NO", null).show();
                } else {
                    log("load profile");
                    loadProfile();
                }
            }
        });
        mButtonSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfile();
            }
        });
        mButtonIdSelect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupGeofenceSelect.show();
            }
        });
        mButtonEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                editGeofence(mSessions[mViewGeofence]);
            }
        });
        mToggleButtonStart1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = mToggleButtonStart1.isChecked();
                GeofenceSession session = mSessions[mViewGeofence];
                if (enabled) {
                    session.mState = GeofenceState.ON;
                    mToggleButtonStart2.setEnabled(true);
                    mToggleButtonStart2.setChecked(true);
                    addGeofence(session);
                } else {
                    session.mState = GeofenceState.OFF;
                    mToggleButtonStart2.setEnabled(false);
                    mToggleButtonStart2.setChecked(false);
                    removeGeofence(session);
                }
                updateUi();
            }
        });
        mToggleButtonStart2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = mToggleButtonStart2.isChecked();
                GeofenceSession session = mSessions[mViewGeofence];
                if (enabled) {
                    session.mState = GeofenceState.ON;
                    resumeGeofence(session);
                } else {
                    session.mState = GeofenceState.PAUSE;
                    pauseGeofence(session);
                }
                updateUi();
            }
        });
        mButtonStopAllSessions.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                closeStartedSessions();
                updateUi();

                // // TODO demo
                // Location l = new Location("test");
                // l.setAccuracy(123f);
                // l.setAltitude(456f);
                // l.setBearing(789f);
                // l.setSpeed(555f);
                // mCallback
                // .onGeofenceTransition(123,
                // GeofenceHardware.GEOFENCE_UNCERTAIN, l,
                // 12345678, FUSED);
                //
                // GeofenceHardwareMonitorEvent e = new
                // GeofenceHardwareMonitorEvent(
                // FUSED, GeofenceHardware.MONITOR_CURRENTLY_AVAILABLE,
                // GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS, l);
                // mMonitorCallback.onMonitoringSystemChange(e);
            }
        });
        mButtonClearInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sInfo.clear();
                mGeofenceCallback.clear();
                mHardwareCallback.clear();
                mGeofenceCount = 0;
                mHardwareCount = 0;
                mTextViewGeofenceCount.setText("");
                mTextViewHardwareCount.setText("");
            }
        });
    }

    private void updateUi() {
        if (mHardwareReady) {
            String tmp = "";
            if (mTypes != null) {
                for (int type : mTypes) {
                    switch (type) {
                    case GeofenceHardware.MONITORING_TYPE_GPS_HARDWARE:
                        tmp += "GPS, ";
                        break;
                    case GeofenceHardware.MONITORING_TYPE_FUSED_HARDWARE:
                        tmp += "FUSED, ";
                        break;
                    default:
                        loge("unknown type=" + type);
                    }
                }
            } else {
                loge("getMonitoringTypes failed");
            }
            mTextViewSupportsSystem.setText(tmp);
            mTextViewSupportsGeofence.setText("Yes");
        } else {
            mTextViewSupportsGeofence.setTextColor(0xffff0000);
            mTextViewSupportsGeofence.setText("No");
            mTextViewSupportsSystem.setTextColor(0xffff0000);
            mTextViewSupportsSystem.setText("");
        }

        String status = "";
        for (GeofenceSession session : mSessions) {
            switch (session.mState) {
            case OFF:
                break;
            case ON:
                status += session.mId + " (On)\n";
                break;
            case PAUSE:
                status += session.mId + " (Pause)\n";
                break;
            }
        }
        // remove last new line
        if (status.length() > 0) {
            status = status.substring(0, status.length() - 1);
        }
        mTextViewStatus.setText(status);
        applyGeofenceToUi(mSessions[mViewGeofence]);
    }

    private void addGeofence(GeofenceSession session) {
        if (mHardwareReady) {
            log("addGeofence session=" + session);
            sInfo.print("addGeofence id=" + session.mId);
            if (!mHardware.addGeofence(session.mId, FUSED, session.mRequest,
                    mCallback)) {
                loge("addGeofence() fail");
            }
        } else {
            loge("hardware is not ready");
        }
        mapGeofenceAdd(session.mId, session.mRequest.getLatitude(),
                session.mRequest.getLongitude(),
                (int) session.mRequest.getRadius());
    }

    private void removeGeofence(GeofenceSession session) {
        if (mHardwareReady) {
            log("removeGeofence id=" + session.mId);
            sInfo.print("removeGeofence id=" + session.mId);
            if (!mHardware.removeGeofence(session.mId, FUSED)) {
                loge("removeGeofence() fail");
            }
        } else {
            loge("hardware is not ready");
        }
        mapGeofenceRemove(session.mId);
    }

    private void resumeGeofence(GeofenceSession session) {
        if (mHardwareReady) {
            log("resumeGeofence session=" + session);
            sInfo.print("resumeGeofence id=" + session.mId);
            if (!mHardware.resumeGeofence(session.mId, FUSED,
                    session.mRequest.getMonitorTransitions())) {
                loge("resumeGeofence() fail");
            }
        } else {
            loge("hardware is not ready");
        }
        mapGeofenceAdd(session.mId, session.mRequest.getLatitude(),
                session.mRequest.getLongitude(),
                (int) session.mRequest.getRadius());
    }

    private void pauseGeofence(GeofenceSession session) {
        if (mHardwareReady) {
            log("pauseGeofence id=" + session.mId);
            sInfo.print("pauseGeofence id=" + session.mId);
            if (!mHardware.pauseGeofence(session.mId, FUSED)) {
                loge("pauseGeofence() fail");
            }
        } else {
            loge("hardware is not ready");
        }
        mapGeofenceRemove(session.mId);
    }

    private void applyGeofenceToUi(GeofenceSession session) {
        GeofenceHardwareRequest request = session.mRequest;
        switch (session.mState) {
        case OFF:
            mToggleButtonStart1.setChecked(false);
            mToggleButtonStart2.setChecked(false);
            mToggleButtonStart2.setEnabled(false);
            mButtonEdit.setEnabled(true);
            break;
        case ON:
            mToggleButtonStart1.setChecked(true);
            mToggleButtonStart2.setChecked(true);
            mToggleButtonStart2.setEnabled(true);
            mButtonEdit.setEnabled(false);
            break;
        case PAUSE:
            mToggleButtonStart1.setChecked(true);
            mToggleButtonStart2.setChecked(false);
            mToggleButtonStart2.setEnabled(true);
            mButtonEdit.setEnabled(false);
            break;
        }
        mTextViewId.setText("" + session.mId);
        mTextViewLatitude.setText("" + request.getLatitude());
        mTextViewLongitude.setText("" + request.getLongitude());
        mTextViewRadius.setText("" + request.getRadius());
        mTextViewLastTransition.setText(transitionString(request
                .getLastTransition()));
        mTextViewUnknownTimer.setText("" + request.getUnknownTimer());
        mTextViewMonitorTransitions.setText(transitionString(request
                .getMonitorTransitions()));
        mTextViewResponsiveness.setText(""
                + request.getNotificationResponsiveness());
        mTextViewSource.setText(sourceString(request.getSourceTechnologies()));
    }

    private void editGeofence(final GeofenceSession session) {
        GeofenceHardwareRequest request = session.mRequest;
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Edit Geofence");
        dialog.setContentView(R.layout.geofence_edit);

        TextView editId = (TextView) dialog.findViewById(R.id.edit_geofence_id);
        final EditText editLat = (EditText) dialog
                .findViewById(R.id.edit_geofence_latitude);
        final EditText editLng = (EditText) dialog
                .findViewById(R.id.edit_geofence_longitude);
        final EditText editRadius = (EditText) dialog
                .findViewById(R.id.edit_geofence_radius);
        final RadioButton editLastEnter = (RadioButton) dialog
                .findViewById(R.id.edit_geofence_last_transition_enter);
        final RadioButton editLastExit = (RadioButton) dialog
                .findViewById(R.id.edit_geofence_last_transition_exit);
        final RadioButton editLastUncertain = (RadioButton) dialog
                .findViewById(R.id.edit_geofence_last_transition_uncertain);
        final EditText editUnknownTimer = (EditText) dialog
                .findViewById(R.id.edit_geofence_unknown_timer);
        final CheckBox editMonitorEnter = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_monitor_enter);
        final CheckBox editMonitorExit = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_monitor_exit);
        final CheckBox editMonitorUncertain = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_monitor_uncertain);
        final EditText editResponsiveness = (EditText) dialog
                .findViewById(R.id.edit_geofence_responsiveness);
        final CheckBox editSourceGnss = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_gnss);
        final CheckBox editSourceWifi = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_wifi);
        final CheckBox editSourceSensors = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_sensors);
        final CheckBox editSourceCell = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_cell);
        final CheckBox editSourceBt = (CheckBox) dialog
                .findViewById(R.id.edit_geofence_bt);
        Button ok = (Button) dialog.findViewById(R.id.edit_geofence_ok);
        Button cancel = (Button) dialog.findViewById(R.id.edit_geofence_cancel);

        editId.setText("" + session.mId);
        editLat.setText("" + request.getLatitude());
        editLng.setText("" + request.getLongitude());
        editRadius.setText("" + request.getRadius());
        int lastTransiition = request.getLastTransition();
        if ((lastTransiition & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
            editLastEnter.setChecked(true);
        }
        if ((lastTransiition & GeofenceHardware.GEOFENCE_EXITED) != 0) {
            editLastExit.setChecked(true);
        }
        if ((lastTransiition & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
            editLastUncertain.setChecked(true);
        }
        editUnknownTimer.setText("" + request.getUnknownTimer());
        int monitorTransition = request.getMonitorTransitions();
        if ((monitorTransition & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
            editMonitorEnter.setChecked(true);
        }
        if ((monitorTransition & GeofenceHardware.GEOFENCE_EXITED) != 0) {
            editMonitorExit.setChecked(true);
        }
        if ((monitorTransition & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
            editMonitorUncertain.setChecked(true);
        }
        editResponsiveness
                .setText("" + request.getNotificationResponsiveness());
        int source = request.getSourceTechnologies();
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_GNSS) != 0) {
            editSourceGnss.setChecked(true);
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_WIFI) != 0) {
            editSourceWifi.setChecked(true);
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS) != 0) {
            editSourceSensors.setChecked(true);
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_CELL) != 0) {
            editSourceCell.setChecked(true);
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH) != 0) {
            editSourceBt.setChecked(true);
        }

        ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // invalid input check
                if (!isDoubleValid(editLat)) {
                    mToast.show("the value of Latitude is invalid");
                    return;
                }
                if (!isDoubleValid(editLng)) {
                    mToast.show("the value of Longitude is invalid");
                    return;
                }
                if (!isDoubleValid(editRadius)) {
                    mToast.show("the value of Radius is invalid");
                    return;
                }
                if (!isIntValid(editUnknownTimer)) {
                    mToast.show("the value of Unknown Timer is invalid");
                    return;
                }
                if (!isIntValid(editResponsiveness)) {
                    mToast.show("the value of Notification Responsiveness is invalid");
                    return;
                }
                if (!editSourceGnss.isChecked() && !editSourceWifi.isChecked()
                        && !editSourceSensors.isActivated()
                        && !editSourceCell.isChecked()
                        && !editSourceBt.isChecked()) {
                    mToast.show("At least one valid source technology must be set");
                    return;
                }

                // start to assign the new value to this session
                double latitude = Double.valueOf(editLat.getText().toString());
                double longitude = Double.valueOf(editLng.getText().toString());
                double radius = Double.valueOf(editRadius.getText().toString());
                int lastTransition = 0;
                if (editLastEnter.isChecked()) {
                    lastTransition = GeofenceHardware.GEOFENCE_ENTERED;
                }
                if (editLastExit.isChecked()) {
                    lastTransition = GeofenceHardware.GEOFENCE_EXITED;
                }
                if (editLastUncertain.isChecked()) {
                    lastTransition = GeofenceHardware.GEOFENCE_UNCERTAIN;
                }
                int unknownTimer = Integer.valueOf(editUnknownTimer.getText()
                        .toString());
                int monitorTransitions = 0;
                if (editMonitorEnter.isChecked()) {
                    monitorTransitions |= GeofenceHardware.GEOFENCE_ENTERED;
                }
                if (editMonitorExit.isChecked()) {
                    monitorTransitions |= GeofenceHardware.GEOFENCE_EXITED;
                }
                if (editMonitorUncertain.isChecked()) {
                    monitorTransitions |= GeofenceHardware.GEOFENCE_UNCERTAIN;
                }
                int notificationResponsiveness = Integer
                        .valueOf(editResponsiveness.getText().toString());
                int sourceTechnologies = 0;
                if (editSourceGnss.isChecked()) {
                    sourceTechnologies |= GeofenceHardware.SOURCE_TECHNOLOGY_GNSS;
                }
                if (editSourceWifi.isChecked()) {
                    sourceTechnologies |= GeofenceHardware.SOURCE_TECHNOLOGY_WIFI;
                }
                if (editSourceSensors.isChecked()) {
                    sourceTechnologies |= GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS;
                }
                if (editSourceCell.isChecked()) {
                    sourceTechnologies |= GeofenceHardware.SOURCE_TECHNOLOGY_CELL;
                }
                if (editSourceBt.isChecked()) {
                    sourceTechnologies |= GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH;
                }
                session.createCircularGeofence(latitude, longitude, radius,
                        lastTransition, unknownTimer, monitorTransitions,
                        notificationResponsiveness, sourceTechnologies);

                applyGeofenceToUi(session);
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

    private boolean isAnySessionStarted() {
        for (GeofenceSession session : mSessions) {
            if (session.mState != GeofenceState.OFF) {
                return true;
            }
        }
        return false;
    }

    private void closeStartedSessions() {
        for (GeofenceSession session : mSessions) {
            if (session.mState == GeofenceState.ON
                    || session.mState == GeofenceState.PAUSE) {
                session.mState = GeofenceState.OFF;
                removeGeofence(session);
            }
        }
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

    private void loadProfile() {
        mPopupGeofenceProfile.show();
    }

    private void saveProfile() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(FlpGeofence.this);
        dialog.setTitle("Input New Profile Name");
        dialog.setMessage("save the current status of Geofences to Geofence profile,"
                + " you can load your Geofence profile from Load button anytime");
        final EditText editText = new EditText(FlpGeofence.this);
        InputFilter[] filterArray = new InputFilter[2];
        filterArray[0] = mCharAndNum;
        filterArray[1] = new InputFilter.LengthFilter(16);
        editText.setFilters(filterArray);
        dialog.setView(editText);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = editText.getText().toString();
                if (mXml.isProfileExist(name)) {
                    mToast.show("[" + name + "] already exists");
                } else {
                    try {
                        mXml.save(getGeofenceProfilePath(),
                                getGeofenceProfilePath(), name, mSessions);
                        mPopupGeofenceProfile.getMenu().add(0, 0, Menu.NONE,
                                name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    private String getGeofenceProfilePath() {
        return getFilesDir().getPath() + "/" + PROFILE_NAME;
    }

    private InputFilter mCharAndNum = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i))
                        && !Character.toString(source.charAt(i)).equals(" ")
                        && !Character.toString(source.charAt(i)).equals("-")
                        && !Character.toString(source.charAt(i)).equals("_")) {
                    return "";
                }
            }
            return source;
        }
    };

    private String statusString(int status) {
        switch (status) {
        case GeofenceHardware.GEOFENCE_SUCCESS:
            return "success";
        case GeofenceHardware.GEOFENCE_ERROR_TOO_MANY_GEOFENCES:
            return "too many geofences";
        case GeofenceHardware.GEOFENCE_ERROR_ID_EXISTS:
            return "id exists";
        case GeofenceHardware.GEOFENCE_ERROR_ID_UNKNOWN:
            return "id unknown";
        case GeofenceHardware.GEOFENCE_ERROR_INVALID_TRANSITION:
            return "invalid transition";
        case GeofenceHardware.GEOFENCE_FAILURE:
            return "failure";
        case GeofenceHardware.GEOFENCE_ERROR_INSUFFICIENT_MEMORY:
            return "insufficient memory";
        }
        return "unknown status " + status;
    }

    private String monitorTypeString(int type) {
        if (type == GeofenceHardware.MONITORING_TYPE_GPS_HARDWARE) {
            return "GPS";
        }
        if (type == GeofenceHardware.MONITORING_TYPE_FUSED_HARDWARE) {
            return "FUSED";
        }
        return "unknown type " + type;
    }

    private String monitorStatusString(int status) {
        if (status == GeofenceHardware.MONITOR_CURRENTLY_AVAILABLE) {
            return "AVAILABLE";
        }
        if (status == GeofenceHardware.MONITOR_CURRENTLY_UNAVAILABLE) {
            return "UNAVAILABLE";
        }
        if (status == GeofenceHardware.MONITOR_UNSUPPORTED) {
            return "UNSUPPORTED";
        }
        return "unknown status " + status;
    }

    private String transitionString(int transition) {
        String o = "";
        if ((transition & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
            o += "ENTER, ";
        }
        if ((transition & GeofenceHardware.GEOFENCE_EXITED) != 0) {
            o += "EXIT, ";
        }
        if ((transition & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
            o += "UNCERTAIN, ";
        }
        return o;
    }

    private String sourceString(int source) {
        String o = "";
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_GNSS) != 0) {
            o += "GNSS, ";
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_WIFI) != 0) {
            o += "WIFI, ";
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS) != 0) {
            o += "SENSORS, ";
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_CELL) != 0) {
            o += "CELL, ";
        }
        if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH) != 0) {
            o += "BT, ";
        }
        return o;
    }

    private GeofenceHardwareMonitorCallback mMonitorCallback = new GeofenceHardwareMonitorCallback() {
        public void onMonitoringSystemChange(GeofenceHardwareMonitorEvent event) {
            mHardwareCount++;
            mTextViewHardwareCount.setText("count=" + mHardwareCount);

            StringBuilder o = new StringBuilder();
            o.append("monitor=[" + monitorTypeString(event.getMonitoringType())
                    + "] ");
            o.append("status=["
                    + monitorStatusString(event.getMonitoringStatus()) + "] ");
            o.append("source=[" + sourceString(event.getSourceTechnologies())
                    + "] ");
            Location l = event.getLocation();
            if (l != null) {
                o.append("location [" + l.getProvider() + "] " + " "
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
            }
            mHardwareCallback.print(o.toString());
            log(o.toString());
        }
    };

    private GeofenceHardwareCallback mCallback = new GeofenceHardwareCallback() {
        public void onGeofenceTransition(int geofenceId, int transition,
                Location location, long timestamp, int monitoringType) {
            mGeofenceCount++;
            mTextViewGeofenceCount.setText("count=" + mGeofenceCount);
            Location l = location;
            StringBuilder o = new StringBuilder();
            o.append("id=[" + geofenceId + "] ");
            o.append("transition=[" + transitionString(transition) + "] ");
            o.append("location [" + l.getProvider() + "] " + " " + l.getTime()
                    + " " + l.getLatitude() + "," + l.getLongitude() + " ");
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
            o.append("timestamp=[" + timestamp + "] ");
            o.append("type=[" + monitorTypeString(monitoringType) + "] ");
            mGeofenceCallback.print(o.toString());
            log(o.toString());

            mapReportLocation(l.getLatitude(), l.getLongitude(),
                    l.hasAccuracy() ? (int) l.getAccuracy() : 0);
            mapGeofenceTransition(geofenceId, transition);
        }

        public void onGeofenceAdd(int geofenceId, int status) {
            String out = "onGeofenceAdd id=" + geofenceId + " ["
                    + statusString(status) + "]";
            log(out);
            sInfo.print(out);
        }

        public void onGeofenceRemove(int geofenceId, int status) {
            String out = "onGeofenceRemove id=" + geofenceId + " ["
                    + statusString(status) + "]";
            log(out);
            sInfo.print(out);
        }

        public void onGeofencePause(int geofenceId, int status) {
            String out = "onGeofencePause id=" + geofenceId + " ["
                    + statusString(status) + "]";
            log(out);
            sInfo.print(out);
        }

        public void onGeofenceResume(int geofenceId, int status) {
            String out = "onGeofenceResume id=" + geofenceId + " ["
                    + statusString(status) + "]";
            log(out);
            sInfo.print(out);
        }
    };

    public enum GeofenceState {
        OFF, ON, PAUSE
    }

    public static class GeofenceSession {
        public int mId;
        public GeofenceState mState;
        public GeofenceHardwareRequest mRequest;

        public String toString() {
            StringBuilder o = new StringBuilder();
            o.append("id=[" + mId + "] ");
            o.append("state=[" + mState + "] ");

            if (mRequest != null) {
                o.append("lat=[" + mRequest.getLatitude() + "] ");
                o.append("lng=[" + mRequest.getLongitude() + "] ");
                o.append("radius=[" + mRequest.getRadius() + "] ");
                String tmp = "";
                switch (mRequest.getLastTransition()) {
                case GeofenceHardware.GEOFENCE_ENTERED:
                    tmp = "ENTER";
                    break;
                case GeofenceHardware.GEOFENCE_EXITED:
                    tmp = "EXIT";
                    break;
                case GeofenceHardware.GEOFENCE_UNCERTAIN:
                    tmp = "UNCERTAIN";
                    break;
                default:
                    break;
                }
                o.append("lastTransition=[" + tmp + "] ");
                o.append("unknownTimer=[" + mRequest.getUnknownTimer() + "] ");
                o.append("monitorTransitions=[");
                int monitorTransitions = mRequest.getMonitorTransitions();
                if ((monitorTransitions & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
                    o.append("ENTER, ");
                }
                if ((monitorTransitions & GeofenceHardware.GEOFENCE_EXITED) != 0) {
                    o.append("EXIT, ");
                }
                if ((monitorTransitions & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
                    o.append("UNCERTAIN, ");
                }
                o.append("] ");
                o.append("notificationResponsiveness=["
                        + mRequest.getNotificationResponsiveness() + "] ");
                int source = mRequest.getSourceTechnologies();
                o.append("monitorTransitions=[");
                if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_GNSS) != 0) {
                    o.append("GNSS, ");
                }
                if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_WIFI) != 0) {
                    o.append("WIFI, ");
                }
                if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS) != 0) {
                    o.append("SENSORS, ");
                }
                if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_CELL) != 0) {
                    o.append("CELL, ");
                }
                if ((source & GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH) != 0) {
                    o.append("BT, ");
                }
                o.append("] ");
            }
            return o.toString();
        }

        public GeofenceSession(int id) {
            mId = id;
            mState = GeofenceState.OFF;
            mRequest = new GeofenceHardwareRequest();
        }

        public void createCircularGeofence(double latitude, double longitude,
                double radius) {
            mRequest = GeofenceHardwareRequest.createCircularGeofence(latitude,
                    longitude, radius);
        }

        public void createCircularGeofence(double latitude, double longitude,
                double radius, int lastTransition, int unknownTimer,
                int monitorTransitions, int notificationResponsiveness,
                int sourceTechnologies) {
            mRequest = GeofenceHardwareRequest.createCircularGeofence(latitude,
                    longitude, radius);
            mRequest.setLastTransition(lastTransition);
            mRequest.setUnknownTimer(unknownTimer);
            mRequest.setMonitorTransitions(monitorTransitions);
            mRequest.setNotificationResponsiveness(notificationResponsiveness);
            mRequest.setSourceTechnologies(sourceTechnologies);
        }

        public void set(GeofenceSession s) {
            GeofenceHardwareRequest request = s.mRequest;
            mState = s.mState;
            createCircularGeofence(request.getLatitude(),
                    request.getLongitude(), request.getRadius(),
                    request.getLastTransition(), request.getUnknownTimer(),
                    request.getMonitorTransitions(),
                    request.getNotificationResponsiveness(),
                    request.getSourceTechnologies());
        }

        public boolean isEqual(GeofenceSession s) {
            GeofenceHardwareRequest request = s.mRequest;
            if (mState != s.mState) {
                return false;
            }
            if (mRequest.getLatitude() != request.getLatitude()) {
                return false;
            }
            if (mRequest.getLongitude() != request.getLongitude()) {
                return false;
            }
            if (mRequest.getRadius() != request.getRadius()) {
                return false;
            }
            if (mRequest.getLastTransition() != request.getLastTransition()) {
                return false;
            }
            if (mRequest.getUnknownTimer() != request.getUnknownTimer()) {
                return false;
            }
            if (mRequest.getMonitorTransitions() != request
                    .getMonitorTransitions()) {
                return false;
            }
            if (mRequest.getNotificationResponsiveness() != request
                    .getNotificationResponsiveness()) {
                return false;
            }
            if (mRequest.getSourceTechnologies() != request
                    .getSourceTechnologies()) {
                return false;
            }
            return true;
        }

        public GeofenceSession clone() {
            GeofenceSession s = new GeofenceSession(mId);
            s.set(this);
            return s;
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

    public static void log(Object msg) {
        Log.d(TAG, "" + msg);
    }

    public static void loge(Object msg) {
        Log.d(TAG, "ERR: " + msg);
        sInfo.print(0xffff0000, "" + msg);
    }
}
