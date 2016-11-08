package com.mediatek.flp.em;

import com.android.location.provider.FusedLocationHardware;
import com.mediatek.flp.service.FusedLocationService;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupMenu;

public class FlpDiagnostic extends Activity {
    public final static String TAG = "FlpEM2.FlpDiagnosticInput";

    Button mButtonResetDaemon;
    Button mButtonTestMode;
    CheckBox mCheckBoxSudoLocation;

    PopupMenu mPopupTestMode;

    boolean mHardwareReady;
    FusedLocationHardware mHardware;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diagnostic);

        mHardwareReady = FusedLocationService.isHardwareReady();
        if (mHardwareReady) {
            mHardware = FusedLocationService.hardware();
        }

        initUi();
        initUiListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initUi() {
        mButtonResetDaemon = (Button) findViewById(R.id.diag_reset_daemon);
        mButtonTestMode = (Button) findViewById(R.id.diag_test_mode);
        mCheckBoxSudoLocation = (CheckBox) findViewById(R.id.diag_sudo_loc);

        mPopupTestMode = new PopupMenu(this, mButtonTestMode);
        for (int i = 0; i < 30; i++) {
            mPopupTestMode.getMenu().add(0, i, Menu.NONE, "Test Mode " + i);
        }
    }

    private void initUiListeners() {
        mPopupTestMode
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (mHardwareReady) {
                            String data = "TEST_MODE_CMD=" + id;
                            log("injectDiagnosticData data=[" + data + "]");
                            mHardware.injectDiagnosticData(data);
                        }
                        return false;
                    }
                });

        mButtonResetDaemon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHardwareReady) {
                    String data = "CLR_RET=1";
                    log("injectDiagnosticData data=[" + data + "], ResetDaemon");
                    mHardware.injectDiagnosticData(data);
                }
            }
        });
        mButtonTestMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupTestMode.show();
            }
        });
        mCheckBoxSudoLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHardwareReady) {
                    if (mCheckBoxSudoLocation.isChecked()) {
                        String data = "ENA_SUDO_LOC=1";
                        log("injectDiagnosticData data=[" + data + "]");
                        mHardware.injectDiagnosticData(data);
                    } else {
                        String data = "ENA_SUDO_LOC=0";
                        log("injectDiagnosticData data=[" + data + "]");
                        mHardware.injectDiagnosticData(data);
                    }
                }
            }
        });
    }

    public static void log(Object msg) {
        Log.d(TAG, "" + msg);
    }
}
