package com.example;

import java.util.Arrays;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.PopupMenu;
import android.app.Activity;
import android.content.Intent;

import com.android.internal.telephony.ITelephony;

public class AutoDialerActivity extends Activity {
    protected final static int PERMISSION_REQUEST_BITS_ALL = 0x07;
    protected final static String [] sNeededPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE};

    protected LocationManager locationMgr;
    protected MyHandler handler;
    protected boolean bSetupDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupSteps(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        if (bSetupDone) {
            doFinish();
        }
    }

    protected int setupSteps(boolean bReq) {
        if (checkAndRequestPermission(PERMISSION_REQUEST_BITS_ALL, bReq) ==
                PERMISSION_REQUEST_BITS_ALL) {
            initUI();
            initListener();

            locationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            handler = new MyHandler(this.getMainLooper(), buttonStartTest,
                    textViewInfo);
            bSetupDone = true;
            return PERMISSION_REQUEST_BITS_ALL;
        } else {
            log("setupSteps: no permission");
            return 0;
        }
    }

    // ===================== UI =====================
    protected ToggleButton buttonStartTest;

    protected LinearLayout layoutSettings;

    protected Button buttonLoops;
    protected TextView textViewLoops;
    protected PopupMenu popupLoops;

    protected RadioButton radioButtonHot;
    protected RadioButton radioButtonWarm;
    protected RadioButton radioButtonCold;
    protected RadioButton radioButtonFull;

    protected RadioButton radioButtonPhone911;
    protected RadioButton radioButtonPhone8378911;
    protected RadioButton radioButtonPhoneEdit;
    protected EditText editTextPhoneEdit;

    protected Button buttonEndCallDuration;
    protected Button buttonPhoneCallDuration;
    protected PopupMenu popupPhoneCall;
    protected PopupMenu popupEndCall;
    protected TextView textViewPhoneCall;
    protected TextView textViewEndCall;

    protected TextView textViewInfo;

    protected int loops = 1;
    protected float endCallDuration = 15;
    protected float phoneCallDuration = 45;


    protected void updateLoops(int loops) {
        this.loops = loops;
        textViewLoops.setText("[" + loops + "] loops");

    }

    protected void updateEndCallDuration(float endCallDuration) {
        this.endCallDuration = endCallDuration;
        textViewEndCall.setText("[" + (int) endCallDuration + "] seconds");
    }

    protected void updatePhoneCallDuration(float phoneCallDuration) {
        this.phoneCallDuration = phoneCallDuration;
        textViewPhoneCall.setText("[" + (int) phoneCallDuration + "] seconds");
    }

    protected void initUI() {
        buttonStartTest = (ToggleButton) findViewById(R.id.Button_StartTest);

        layoutSettings = (LinearLayout) findViewById(R.id.LinearLayout_Settings);
        buttonLoops = (Button) findViewById(R.id.Button_Loops);
        textViewLoops = (TextView) findViewById(R.id.TextView_Loops);
        radioButtonHot = (RadioButton) findViewById(R.id.RadioButton_Hot);
        radioButtonWarm = (RadioButton) findViewById(R.id.RadioButton_Warm);
        radioButtonCold = (RadioButton) findViewById(R.id.RadioButton_Cold);
        radioButtonFull = (RadioButton) findViewById(R.id.RadioButton_Full);
        radioButtonPhone911 = (RadioButton) findViewById(R.id.RadioButton_Phone_911);
        radioButtonPhone8378911 = (RadioButton) findViewById(R.id.RadioButton_Phone_8378911);
        radioButtonPhoneEdit = (RadioButton) findViewById(R.id.RadioButton_Phone_Edit);
        editTextPhoneEdit = (EditText) findViewById(R.id.EditText_PhoneEdit);
        buttonEndCallDuration = (Button) findViewById(R.id.Button_EndCallDuration);
        buttonPhoneCallDuration = (Button) findViewById(R.id.Button_PhoneCallDuration);
        textViewEndCall = (TextView) findViewById(R.id.TextView_EndCallDuration);
        textViewPhoneCall = (TextView) findViewById(R.id.TextView_PhoneCallDuration);
        textViewInfo = (TextView) findViewById(R.id.TextView_Info);

        popupEndCall = new PopupMenu(this, buttonEndCallDuration);
        popupEndCall
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        updateEndCallDuration(item.getItemId());
                        return false;
                    }
                });

        popupPhoneCall = new PopupMenu(this, buttonPhoneCallDuration);
        popupPhoneCall
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        updatePhoneCallDuration(item.getItemId());
                        return false;
                    }
                });

        popupLoops = new PopupMenu(this, buttonLoops);
        popupLoops
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        updateLoops(item.getItemId());
                        return false;
                    }
                });


        popupPhoneCall.getMenu().add(0, 1, Menu.NONE,
                "1 sec");
        popupEndCall.getMenu().add(0, 1, Menu.NONE,
                "1 sec");
        for (int i = 5; i <= 200; i+= 5) {
            popupPhoneCall.getMenu().add(0, i, Menu.NONE,
                    i + " sec");
            popupEndCall.getMenu().add(0, i, Menu.NONE,
                    i + " sec");
        }

        popupLoops.getMenu().add(0, 1, Menu.NONE, "1 loop");
        for (int i = 5; i <= 500; i+= 5) {
            popupLoops.getMenu().add(0, i, Menu.NONE,
                    i + " loops");
        }

        buttonStartTest.requestFocus();
        radioButtonHot.setChecked(true);
        radioButtonPhone911.setChecked(true);
        editTextPhoneEdit.setVisibility(View.GONE);
        editTextPhoneEdit.setText("035670766");
        updateLoops(loops);
        updateEndCallDuration(endCallDuration);
        updatePhoneCallDuration(phoneCallDuration);
    }

    protected String getPhoneNumber() {
        if (radioButtonPhone911.isChecked())
            return "911";
        if (radioButtonPhone8378911.isChecked())
            return "#8378911";
        if (radioButtonPhoneEdit.isChecked())
            return editTextPhoneEdit.getText().toString();
        return "911";
    }

    protected int getDeleteMode() {
        if (radioButtonHot.isChecked())
            return 0;
        if (radioButtonWarm.isChecked())
            return 1;
        if (radioButtonCold.isChecked())
            return 2;
        if (radioButtonFull.isChecked())
            return 3;
        return 0;
    }

    protected void initListener() {
        buttonStartTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (buttonStartTest.isChecked()) {
                    setEnableSettings(false);
                    sendConfig(loops, getPhoneNumber(), getDeleteMode());
                    doDeleteLater();
                } else {
                    setEnableSettings(true);
                    doFinish();
                }
            }
        });
        buttonLoops.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupLoops.show();
            }
        });
        radioButtonPhone911.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editTextPhoneEdit.setVisibility(View.GONE);
            }
        });
        radioButtonPhone8378911.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editTextPhoneEdit.setVisibility(View.GONE);

            }
        });
        radioButtonPhoneEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editTextPhoneEdit.setVisibility(View.VISIBLE);
            }
        });
        buttonEndCallDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupEndCall.show();
            }
        });
        buttonPhoneCallDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupPhoneCall.show();
            }
        });
    }

    protected void setEnableSettings(boolean enabled) {
        buttonLoops.setEnabled(enabled);
        radioButtonHot.setEnabled(enabled);
        radioButtonWarm.setEnabled(enabled);
        radioButtonCold.setEnabled(enabled);
        radioButtonFull.setEnabled(enabled);
        radioButtonPhone911.setEnabled(enabled);
        radioButtonPhone8378911.setEnabled(enabled);
        radioButtonPhoneEdit.setEnabled(enabled);
        buttonEndCallDuration.setEnabled(enabled);
        buttonPhoneCallDuration.setEnabled(enabled);
        editTextPhoneEdit.setEnabled(enabled);
    }

    // ===================== handler =====================
    // XXX
    protected void doDeleteLater() {
        sendMessage(EVENT_DELETE, (int)(endCallDuration * 500));
    }

    protected void doPhoneCallLater() {
        sendMessage(EVENT_PHONE_CALL, (int)(endCallDuration * 500));
    }

    protected void doEndCallLater() {
        sendMessage(EVENT_END_CALL, (int) (phoneCallDuration * 1000));
    }

    protected void doFinish() {
        sendMessage(EVENT_FINISH, 0);
        handler.removeMessages(EVENT_CONFIG);
        handler.removeMessages(EVENT_DELETE);
        handler.removeMessages(EVENT_PHONE_CALL);
        handler.removeMessages(EVENT_END_CALL);
    }

    protected void sendMessage(int what) {
        sendMessage(what, null, 0);
    }

    protected void sendMessage(int what, long delay) {
        sendMessage(what, null, delay);
    }

    protected void sendMessage(int what, Bundle data) {
        sendMessage(what, data, 0);
    }

    protected void sendMessage(int what, Bundle data, long delay) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.setData(data);
        handler.sendMessageDelayed(msg, delay);
    }

    protected void sendConfig(int loops, String phoneNumber, int deleteMode) {
        Bundle b = new Bundle();
        b.putInt("loops", loops);
        b.putString("phoneNumber", phoneNumber);
        b.putInt("deleteMode", deleteMode);
        sendMessage(EVENT_CONFIG, b, 0);
    }

    protected final static int EVENT_CONFIG = 0;
    protected final static int EVENT_DELETE = 1;
    protected final static int EVENT_PHONE_CALL = 2;
    protected final static int EVENT_END_CALL = 3;
    protected final static int EVENT_FINISH = 4;

    protected class MyHandler extends Handler {
        protected TextView textViewInfo;
        protected ToggleButton buttonStartTest;

        protected int currentLoops;
        protected int loops;
        protected String phoneNumber;
        protected int deleteMode;

        public MyHandler(Looper l, ToggleButton button, TextView textView) {
            super(l);
            buttonStartTest = button;
            textViewInfo = textView;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
            case EVENT_CONFIG:
                loops = data.getInt("loops");
                phoneNumber = data.getString("phoneNumber");
                deleteMode = data.getInt("deleteMode");
                textViewInfo.setText("loops=" + loops + " phoneNumber="
                        + phoneNumber + " deleteMode=" + deleteMode);
                break;
            case EVENT_DELETE:
                currentLoops++;
                log("loop (" + currentLoops + "/" + loops + ")");
                textViewInfo.setText("(" + currentLoops + "/" + loops
                        + ") EVENT_DELETE");
                delete(deleteMode);
                doPhoneCallLater();
                break;
            case EVENT_PHONE_CALL:
                textViewInfo.setText("(" + currentLoops + "/" + loops
                        + ") EVENT_PHONE_CALL");
                call(phoneNumber);
                doEndCallLater();
                break;
            case EVENT_END_CALL:
                textViewInfo.setText("(" + currentLoops + "/" + loops
                        + ") EVENT_END_CALL");
                if (currentLoops >= loops) {
                    buttonStartTest.setChecked(false);
                    setEnableSettings(true);
                    doFinish();
                } else {
                    if (endCall()) {
                        doDeleteLater();
                    } else {
                        doFinish();
                    }
                }
                break;
            case EVENT_FINISH:
                log("finish");
                textViewInfo.setText("EVENT_FINISH");
                currentLoops = 0;
                endCall();
                break;
            default:
                textViewInfo.setText("UNKNOWN what=" + msg.what);
                log("UNKNOWN what=" + msg.what);
                break;
            }
        }

        protected void call(String phoneNumber) {
            // Intent.ACTION_CALL_PRIVILEGED
            // TODO
            Intent intentDial = new Intent(
                "android.intent.action.CALL_PRIVILEGED", Uri.parse("tel:"
                + Uri.encode(phoneNumber)));
            startActivity(intentDial);
            log("phoneNumber=" + phoneNumber);
        }

        protected boolean endCall() {
            boolean ret = false;
            // TODO
            try {
                ITelephony phone =
                ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

                for (int i = 0; i < 4; i++) {
                    int[] subIds = SubscriptionManager.getSubId(i);
                    if (subIds != null && subIds.length > 0 && subIds[0] != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        subId = subIds[0];
                        log("endCall: subId=" + subId + " from phoneId=" + i);
                        break;
                    }
                }

                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    ret = phone.endCallForSubscriber(subId);
                } else {
                    log("endCall: failed due to no valid subId, stop test directly!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            log("endCall ret=" + ret);
            return ret;
        }

        protected void delete(int mode) {
            Bundle extras = new Bundle();
            if (mode == 0) {
                log("HotStart");
                extras.putBoolean("rti", true);
            } else if (mode == 1) {
                log("WarmStart");
                extras.putBoolean("ephemeris", true);
            } else if (mode == 2) {
                log("ColdStart");
                extras.putBoolean("ephemeris", true);
                extras.putBoolean("almanac", true);
                extras.putBoolean("position", true);
                extras.putBoolean("time", true);
                extras.putBoolean("iono", true);
                extras.putBoolean("utc", true);
            } else if (mode == 3) {
                log("FullStart");
                extras.putBoolean("all", true);
            } else {
                log("UNKNOWN reset mode=" + mode);
                return;
            }
            locationMgr.sendExtraCommand(LocationManager.GPS_PROVIDER,
                    "delete_aiding_data", extras);
        }
    }

    // ===================== utilities =====================
    protected static void log(Object msg) {
        Log.d("AutoDialer", "" + msg);
    }

    private int checkAndRequestPermission(int reqBitmap, boolean bReq) {
        boolean bDeniedWithNeverAsk = false;
        int grantResultBits = 0;
        reqBitmap &= PERMISSION_REQUEST_BITS_ALL;
        if (reqBitmap == 0) return grantResultBits;

        int total = Integer.bitCount(reqBitmap);
        String [] perms = new String [total];

        int i, j;
        for (i = j = 0; i < sNeededPermissions.length; i++) {
            if (((1 << i) & reqBitmap) != 0) {
                if (checkSelfPermission(sNeededPermissions[i])
                        != PackageManager.PERMISSION_GRANTED) {
                    perms[j++] = sNeededPermissions[i];
                   // Should we show an explanation?
                   if (shouldShowRequestPermissionRationale(
                           sNeededPermissions[i])) {
                       // Explain to the user why we need to read the contacts
                       log("We need " +
                               sNeededPermissions[i] + " to work");
                   } else {
                       bDeniedWithNeverAsk = true;
                   }
                } else {
                    reqBitmap &= ~(1 << i);
                    grantResultBits |= (1 << i);
                }
            }
        }

        if (bReq) {
            if (j < total) {
                total = j;
                if (total == 0) return grantResultBits;
                perms = Arrays.copyOf(perms, total);
            }

            requestPermissions(perms, reqBitmap);
        } else if (bDeniedWithNeverAsk) {
            Toast.makeText(this, R.string.msg_permission_steps,
                    Toast.LENGTH_LONG).show();
        }

        return grantResultBits;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        log("onRequestPermissionsResult: reqCode=" + requestCode);
        for (int i = 0; i < permissions.length; i++) {
            log("onRequestPermissionsResult: perm[" + i + "]=" +
                    permissions[i] + ", result=" + grantResults[i]);
        }
        if (PERMISSION_REQUEST_BITS_ALL != setupSteps(false)) {
            log("onRequestPermissionsResult: call finish()");
            finish();
        }
    }
}
