package com.mediatek.rcs.pam;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.mediatek.rcs.pam.client.AsyncUIPAMClient;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.ui.AccountDetailsActivity;
import com.mediatek.rcs.pam.ui.LoadingMaskActivity;
import com.mediatek.rcs.pam.ui.accounthistory.AccountHistoryActivity;
import com.mediatek.rcs.pam.ui.accountlist.AccountListActivity;
import com.mediatek.rcs.pam.ui.accountlist.AccountSearchActivity;
import com.mediatek.rcs.pam.ui.messagehistory.MessageHistoryActivity;
import com.mediatek.rcs.pam.util.Utils;

import java.io.File;
import java.util.List;

public class MainActivity extends LoadingMaskActivity {
    public static final String TAG = "PAM/MainActivity";

    private static final String MTK01_UUID = "125200024018242722@as99.pa.rcs1.chinamobile.com";
    private static final String RCS_TEAM_UUID = "125200024018998130@as99.pa.rcs1.chinamobile.com";
    // private static final String MTK01_UUID = "125200024018835834@as99.pa.rcs1.chinamobile.com";
//    private static final String MTK02_UUID = "125200024018242721@as99.pa.rcs1.chinamobile.com";
    private static final String TEST_IMAGE_NAME = "longhorn_cowfish.jpg";
    private static final String TEST_IMAGE_THUMBNAIL_NAME = "longhorn_cowfish_thumb.jpg";
    private static final String TEST_AUDIO_NAME = "huluwa.mp3";
    private static final String TEST_VIDEO_NAME = "iphone6_leak.mp4";
    private static final String TEST_VIDEO_THUMBNAIL_NAME = "longhorn_cowfish_thumb.jpg";
    private static final String TEST_VCARD_STRING = "BEGIN:vCard\nVERSION:3.0\nFN:Frank\nORG:" +
            "China Mobile ResearchInstitute\nADR;TYPE=WORK,POSTAL,PARCEL:;;53AXibianmen Street;" +
            "Xicheng District;Beijing;100053;China\nTEL;TYPE=VOICE,MSG," +
            "WORK:+8613912345678\nEMAIL;TYPE=INTERNET,PREF:Frank@10086.cn\nEND:vCard";
    private static final String TEST_GEOLOC_STRING = "<rcsenvelope xmlns=\"urn:gsma:params:xml" +
            ":ns:rcs:rcs:geolocation\" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\" xmlns" +
            ":gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\" xmlns:gml=\"http://www.opengis." +
            "net/gml\" xmlns:gs=\"http://www.opengis.net/pidflo/1.0\" entity" +
            "=\"SIP:12520002234567891111@as1.pa.rcs.chinamobile.com\"><rcspushlocation id" +
            "=\"a1233\" label=\"meeting location\"><rpid:time-offset rpid:until=\"2012-03" +
            "-15T21:00:00-05:00\">-300</rpid:time-offset><gp:geopriv><gp:location-info>" +
            "<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\"><gml:pos>26.1181289 -80.1283921" +
            "</gml:pos><gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">10</gs:radius></gs:Circle>" +
            "</gp:location-info><gp:usage-rules><gp:retention-expiry>2012-03-15T21:00:00-05:00" +
            "</gp:retention-expiry></gp:usage-rules></gp:geopriv><timestamp>2012-03-15T16:09:44" +
            "-05:00</timestamp></rcspushlocation></rcsenvelope>";

    private Button mMessageHistoryButton;
    private Button mAccountDetailsButton;
    private Button mTestProviderButton;

    private Button mSubscribeButton;
    private Button mUnsubscribeButton;
    private Button mGetSubscribedButton;
    private Button mSearchButton;
    private Button mGetDetailsButton;
    private Button mGetMenuButton;
    private Button mGetMessageHistoryButton;
    private Button mComplainButton;
    private Button mGetRecommendsButton;
    private Button mSetAcceptStatusButton;
    private Button mSendPagerModeMessageButton;
    private Button mSendLargeModeMessageButton;
    private Button mSendImageButton;
    private Button mSendAudioButton;
    private Button mSendVideoButton;
    private Button mSendGeoLocButton;
    private Button mSendVcardButton;
    private Button mResendTextButton;

    private SimpleServiceCallback mServiceCallback;
    private PAService mService;
    private long mToken = Constants.INVALID;
    private AsyncUIPAMClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMessageHistoryButton = (Button) findViewById(R.id.button_message_history);
        mMessageHistoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(MessageHistoryActivity.ACTION);
                intent.setClass(MainActivity.this, MessageHistoryActivity.class);
                intent.putExtra(AccountDetailsActivity.KEY_UUID, MTK01_UUID);
                startActivity(intent);
            }
        });
        mAccountDetailsButton = (Button) findViewById(R.id.button_account_details);
        mAccountDetailsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(AccountDetailsActivity.ACTION);
                intent.setClass(MainActivity.this, AccountDetailsActivity.class);
                intent.putExtra(AccountDetailsActivity.KEY_UUID,
                    "125200024018998430@as99.pa.rcs1.chinamobile.com"/*MTK01_UUID*/);
                startActivity(intent);
            }
        });
        mTestProviderButton = (Button) findViewById(R.id.button_test_provider);
        mTestProviderButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver cr = getContentResolver();
                Cursor c = null;
                c = cr.query(PAContract.CcsAccountColumns.CONTENT_URI, null, null, null, null);
                c.close();
                c = cr.query(PAContract.AccountColumns.CONTENT_URI, null, null, null, null);
                c.close();
            }
        });

        mSubscribeButton = (Button) findViewById(R.id.button_subscribe);
        mSubscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.subscribe(mToken, MTK01_UUID);
            }
        });

        mUnsubscribeButton = (Button) findViewById(R.id.button_unsubscribe);
        mUnsubscribeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.unsubscribe(mToken, MTK01_UUID);
            }
        });

        mGetSubscribedButton = (Button) findViewById(R.id.button_subscribed);
        mGetSubscribedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.getSubscribedList(mToken, Constants.ORDER_BY_NAME, 10, 1);
            }
        });

        mSearchButton = (Button) findViewById(R.id.button_search);
        mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient.search("m", Constants.ORDER_BY_NAME, 10, 1);
            }
        });

        mGetDetailsButton = (Button) findViewById(R.id.button_details);
        mGetDetailsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.getDetails(mToken,
                    "125200024018998430@as99.pa.rcs1.chinamobile.com"/*MTK01_UUID*/, null);
            }
        });

        mGetMenuButton = (Button) findViewById(R.id.button_get_menu);
        mGetMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.getMenu(mToken, MTK01_UUID, null);
            }
        });

        mGetMessageHistoryButton = (Button) findViewById(R.id.button_history);
        mGetMessageHistoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient.getMessageHistory(MTK01_UUID,
                        Utils.covertTimestampToString(System.currentTimeMillis()),
                        Constants.ORDER_BY_TIMESTAMP_DESCENDING, 10, 1);
            }
        });

        mComplainButton = (Button) findViewById(R.id.button_complain);
        mComplainButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient.complain(MTK01_UUID,
                    Constants.COMPLAIN_TYPE_ACCOUNT, "test reason", null, "test description");
            }
        });

        mGetRecommendsButton = (Button) findViewById(R.id.button_recommends);
        mGetRecommendsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient.getRecommends(99, 10, 1);
            }
        });

        mSetAcceptStatusButton = (Button) findViewById(R.id.button_accept_status);
        mSetAcceptStatusButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.setAcceptStatus(mToken, MTK01_UUID, Constants.ACCEPT_STATUS_YES);
            }
        });

        mSendPagerModeMessageButton = (Button) findViewById(R.id.button_send_pager_mode_message);
        mSendPagerModeMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendMessage(mToken, getAccountId(MTK01_UUID), "Test Message: " +
                        Utils.covertTimestampToString(System.currentTimeMillis()), false);
            }
        });


        mSendLargeModeMessageButton = (Button) findViewById(R.id.button_send_large_mode_message);
        mSendLargeModeMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                char[] chars = new char[901];
                for (int i = 0; i < chars.length; ++i) {
                    chars[i] = (char)(((int)'a') + i % 26);
                }
                mService.sendMessage(mToken, getAccountId(MTK01_UUID),
                        new String(chars), false);
            }
        });

        mSendImageButton = (Button) findViewById(R.id.button_send_image);
        mSendImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = Environment.getExternalStorageDirectory();
                mService.sendImage(mToken,
                        getAccountId(MTK01_UUID),
                        f.getAbsolutePath() + File.separator + TEST_IMAGE_THUMBNAIL_NAME,
                        f.getAbsolutePath() + File.separator + TEST_IMAGE_THUMBNAIL_NAME);
            }
        });

        mSendAudioButton = (Button) findViewById(R.id.button_send_audio);
        mSendAudioButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = Environment.getExternalStorageDirectory();
                mService.sendAudio(mToken,
                        getAccountId(MTK01_UUID),
                        f.getAbsolutePath() + File.separator + TEST_AUDIO_NAME,
                        302);
            }
        });

        mSendVideoButton = (Button) findViewById(R.id.button_send_video);
        mSendVideoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = Environment.getExternalStorageDirectory();
                mService.sendVideo(mToken,
                        getAccountId(MTK01_UUID),
                        f.getAbsolutePath() + File.separator + TEST_VIDEO_NAME,
                        f.getAbsolutePath() + File.separator + TEST_VIDEO_THUMBNAIL_NAME,
                        0);
            }
        });

        mSendGeoLocButton = (Button) findViewById(R.id.button_send_geoloc);
        mSendGeoLocButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendGeoLoc(mToken,
                        getAccountId(MTK01_UUID),
                        TEST_GEOLOC_STRING);
            }
        });

        mSendVcardButton = (Button) findViewById(R.id.button_send_vcard);
        mSendVcardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendVcard(mToken,
                        getAccountId(MTK01_UUID),
                        TEST_VCARD_STRING);
            }
        });

        mResendTextButton = (Button) findViewById(R.id.button_resend_text);
        mResendTextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.resendMessage(mToken, 1);
            }
        });

        mServiceCallback = new SimpleServiceCallback() {

            @Override
            public void onNewMessage(long accountId, long messageId) throws RemoteException {
                Toast.makeText(MainActivity.this,
                    "New Message: " + messageId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReportMessageFailed(long messageId) throws RemoteException {
                Toast.makeText(MainActivity.this,
                    "Message Failed: " + messageId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReportMessageDisplayed(long messageId) throws RemoteException {
                Toast.makeText(MainActivity.this,
                    "Message Displayed: " + messageId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReportMessageDelivered(long messageId) throws RemoteException {
                Toast.makeText(MainActivity.this,
                    "Message Delivered: " + messageId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComposingEvent(long accountId, boolean status) throws RemoteException {
                Toast.makeText(MainActivity.this,
                        "Peer Composing Message: " + accountId + ", " + status,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTransferProgress(long messageId,
                    long currentSize, long totalSize) throws RemoteException {
                Log.d(TAG, "onTransferProgress: " + currentSize);
            }

            @Override
            public void reportSubscribeResult(
                    long requestId, int resultCode) throws RemoteException {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportUnsubscribeResult(long requestId,
                        int resultCode) throws RemoteException {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportGetSubscribedResult(long requestId, int resultCode, long[] accountIds)
                    throws RemoteException {
                Log.d(TAG, "RequestId: " + requestId);
                Log.d(TAG, "ResultCode: " + resultCode);
                Log.d(TAG, "AccountIds:");
                if (accountIds != null) {
                    for (long id : accountIds) {
                        Log.d(TAG, "    " + id);
                    }
                }
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportGetDetailsResult(long requestId,
                    int resultCode, long accountId) throws RemoteException {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportGetMenuResult(long requestId,
                    int resultCode) throws RemoteException {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "Service is connected", Toast.LENGTH_SHORT).show();
                        switchToNormalView();
                    }
                });
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "Service is disconnected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onServiceRegistered() throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "Service is registered", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onServiceUnregistered() throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "Service is unregistered", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAccountChanged(final String newAccount) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                            "onAccountChanged" + newAccount, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void reportDeleteMessageResult(final long requestId,
                    final int resultCode) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Delete Message: " +
                            requestId + " resultCode: " + resultCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        mService = PAService.getInstance();
        mToken = mService.registerCallback(
                new IPAServiceCallbackWrapper(mServiceCallback, this), false);
        mClient = new AsyncUIPAMClient(this, new AsyncUIPAMClient.SimpleCallback() {
            @Override
            public void reportSearchResult(long requestId,
                    int resultCode, List<PublicAccount> results) {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportGetRecommendsResult(long requestId,
                    int resultCode, List<PublicAccount> results) {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportGetMessageHistoryResult(long requestId,
                            int resultCode, List<MessageContent> results) {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void reportComplainResult(long requestId, int resultCode) {
                String resultString = (resultCode == ResultCode.SUCCESS) ?
                                        "Success" : "Failed: " + resultCode;
                Toast.makeText(MainActivity.this, resultString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            promptUser("settings");
            return true;
        } else if (id == R.id.action_history) {
            String uuid = MTK01_UUID;
            String title = "MTK01_UUID";
            openHistory(this, uuid, title);
            return true;
        } else if (id == R.id.action_list) {
            startActivity(new Intent(this, AccountListActivity.class));
            return true;
        } else if (id == R.id.action_search) {
            startActivity(new Intent(this, AccountSearchActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openHistory(Context context, String uuid, String title) {
        Log.i(TAG, "openHistory " + uuid + ", " + title);
        Intent intent = new Intent(context, AccountHistoryActivity.class);
        intent.putExtra(AccountHistoryActivity.KEY_ACCOUNT_UUID, uuid);
        intent.putExtra(AccountHistoryActivity.KEY_ACCOUNT_TITLE, title);
        context.startActivity(intent);
    }

    private void promptUser(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_main;
    }

    /*
     * Clear all the entries in all tables.
     */
    public void clearDatabase() {
        ContentResolver cr = getContentResolver();
        cr.delete(MediaColumns.CONTENT_URI, null, null);
        cr.delete(MediaBasicColumns.CONTENT_URI, null, null);
        cr.delete(MediaArticleColumns.CONTENT_URI, null, null);
        cr.delete(MessageColumns.CONTENT_URI, null, null);
        cr.delete(AccountColumns.CONTENT_URI, null, null);
    }

    @Override
    public void onDestroy() {
        if (mService != null) {
            mService.unregisterCallback(mToken);
        }
        super.onDestroy();
    }

    private long getAccountId(String uuid) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        long accountId = Constants.INVALID;
        try {
            c = cr.query(AccountColumns.CONTENT_URI,
                    new String[] { AccountColumns.ID }, AccountColumns.UUID + "=?",
                    new String[] { uuid }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                accountId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return accountId;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
