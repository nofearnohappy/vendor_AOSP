package com.mediatek.settings.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.mediatek.op09.plugin.R;

/**
 * CT feature: Show UIM detect dialog:
 * 1) Insert 4G card suggestion 2) No UIM card suggestion.
 */
public class UimDetectDialogActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "UimDetectDialogActivity";
    private String mTextMsg;
    private TextView mMessageView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initFromIntent(getIntent());
        if (mTextMsg == null) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        setContentView(R.layout.uim_detect_dlg);
        mMessageView = (TextView) window.findViewById(R.id.dialog_message);

        Button okButton = (Button) findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        setFinishOnTouchOutside(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initFromIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMessageView.setText(mTextMsg);
        Log.d(TAG, "UimDetectDialogActivity showed onResume");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SimDialogService.TEXT, mTextMsg);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mTextMsg = savedInstanceState.getString(SimDialogService.TEXT);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "User onClick");
        Intent intent = new Intent(SimDialogService.ACTION_START_SELF);
        this.sendBroadcast(intent);
        Log.d(TAG, "send ACTION_START_SELF broadcast");
        finish();
    }

    private void initFromIntent(Intent intent) {

        if (intent != null) {
            mTextMsg = intent.getStringExtra(SimDialogService.TEXT);
            Log.d(TAG, "initFromIntent mTextMsg=" + mTextMsg);
        } else {
            finish();
        }
    }
}
