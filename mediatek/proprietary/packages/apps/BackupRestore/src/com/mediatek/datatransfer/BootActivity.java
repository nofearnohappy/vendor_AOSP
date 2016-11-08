package com.mediatek.datatransfer;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.mediatek.datatransfer.utils.SDCardUtils;

public class BootActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.hide();
        }

        if (SDCardUtils.getExternalStoragePath(this) == null) {
            Toast.makeText(this, R.string.nosdcard_notice, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
