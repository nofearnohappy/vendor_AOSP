package com.mediatek.rcs.pam.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.client.AsyncUIPAMClient;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;

import java.util.List;

public class ComplainAccountActivity extends Activity {
    private static final String TAG = Constants.TAG_PREFIX
            + "ComplainAccountActivity";

    public static final String ACTION = "com.mediatek.pam.ComplainAccountActivity";
    public static final String KEY_UUID = "com.medaitek.pam.ComplainAccountActivity.KEY_UUID";
    public static final String KEY_NAME = "com.medaitek.pam.ComplainAccountActivity.KEY_NAME";

    private AsyncUIPAMClient mClient;
    private long mRequestId = Constants.INVALID;
    private Object mRequestLock = new Object();
    private String mUuid;
    private String mName;

    private Spinner mReasonSpinner;
    private EditText mDescription;
    private Button mComplainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complain_account);
        mReasonSpinner = (Spinner) findViewById(R.id.complain_reason_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.complain_account_resson_string_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mReasonSpinner.setAdapter(adapter);

        mDescription = (EditText) findViewById(R.id.complain_description);

        mComplainButton = (Button) findViewById(R.id.complain_button);
        mComplainButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                complain();
            }
        });

        mClient = new AsyncUIPAMClient(this, new AsyncUIPAMClient.Callback() {

            @Override
            public void reportSearchResult(long requestId, int resultCode,
                    List<PublicAccount> results) {
                // do nothing
            }

            @Override
            public void reportGetRecommendsResult(long requestId,
                    int resultCode, List<PublicAccount> results) {
                // do nothing
            }

            @Override
            public void reportGetMessageHistoryResult(long requestId,
                    int resultCode, List<MessageContent> results) {
                // do nothing
            }

            @Override
            public void reportComplainResult(long requestId, int resultCode) {
                synchronized (mRequestLock) {
                    // if (requestId != mRequestId) {
                    // throw new Error("Request ID does not match: " + requestId
                    // + ", " + mRequestId);
                    // }
                    // mRequestId = Constants.INVALID;
                    if (resultCode == ResultCode.SUCCESS) {
                        Toast.makeText(ComplainAccountActivity.this,
                                R.string.complain_account_success,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ComplainAccountActivity.this,
                                R.string.complain_account_failure,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        Bundle arguments = getIntent().getExtras();
        mUuid = arguments.getString(KEY_UUID, null);
        mName = arguments.getString(KEY_NAME, null);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(
                R.string.title_activity_complain_account)
                + mName);
    }

    private void complain() {
        mClient.complain(mUuid, Constants.COMPLAIN_TYPE_ACCOUNT, mReasonSpinner
                .getSelectedItem().toString(), null, mDescription.getText()
                .toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.complain_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
