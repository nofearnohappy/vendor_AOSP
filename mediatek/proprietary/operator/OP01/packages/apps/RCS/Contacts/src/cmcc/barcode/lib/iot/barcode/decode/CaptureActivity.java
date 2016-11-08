/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cmcc.barcode.lib.iot.barcode.decode;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

//import com.cmcc.omp.sdk.rest.qrcodec.common.ImageDecodeUtils;
import com.cmcc.omp.sdk.rest.qrcodec.decode.camera.CameraManager;
import com.cmcc.omp.sdk.rest.qrcodec.decode.handle.CaptureActivityHandler;
import com.cmcc.omp.sdk.rest.qrcodec.decode.handle.InactivityTimer;

import com.google.common.collect.Lists;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.qrcode.result.AddressBookParsedResult;
import com.mediatek.rcs.contacts.qrcode.result.VCardResultParser;
import com.mediatek.rcs.contacts.qrcode.service.QRCodeException;
import com.mediatek.rcs.contacts.qrcode.service.QRCodeService;
import com.mediatek.rcs.contacts.util.WeakAsyncTask;

//import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final String[] EMAIL_TYPE_STRINGS = {"home", "work", "cell"};

    private static final String[] PHONE_TYPE_STRINGS = {"fax", "workfax",
            "home", "work", "cell"};

    private static final int[] EMAIL_TYPE_VALUES = {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME,
        ContactsContract.CommonDataKinds.Email.TYPE_WORK,
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,
    };

    private static final int[] PHONE_TYPE_VALUES = {
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    };

    private static final String ACCOUNT_NAME = "Phone";
    private static final String ACCOUNT_TYPE = "Local Phone Account";
    private static final String KEY_CAMERA_STOP = "cameraStop";
    private static final String KEY_PARSE_FAILED = "parseFailed";
    private static final String QR_CODE_TAG = "pcc-TwoDimensionCodeCard";
    private static final String QR_CODE_XCAP = "PCC.xml";
    private static final int TYPE_OTHER = -1;
    private static final int GET_PIC = 10;

    private CaptureActivityHandler mHandler;
    private ViewfinderView mViewfinderView;
    private TextView mStatusView;
    private TextView mErrorView;
    private boolean mHasSurface;
    private boolean mParseFailed;
    private InactivityTimer mInactivityTimer;
    private BeepManager mBeepManager;
    private ActionBar mActionBar;
    private ArrayList<ContentValues> mOperationList;
    private ParseImageTask mParseImageTask;

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Log.d(TAG, "onCreate");
        CameraManager.load(getApplication());
        CameraManager.init(getApplication());
        //init camera preview size
        CameraManager.get().setWidth(90);
        CameraManager.get().setHeight(60);
        CameraManager.get().setCenter(40);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                            | ActionBar.DISPLAY_SHOW_HOME);
        }

        mHasSurface = false;
        mInactivityTimer = new InactivityTimer(this);
        mBeepManager = new BeepManager(this);
        mOperationList = Lists.newArrayList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        mStatusView = (TextView) findViewById(R.id.status_view);
        mErrorView = (TextView) findViewById(R.id.error_view);
        mErrorView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "mErrorView onClick");
                //initHandler();
                if (mHandler != null) {
                    mHandler.restartPreviewAndDecode();
                }
                resetStatusView();
            }

        });

        mHandler = null;
        //set camera to portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (mHasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }

        resetStatusView();
        mBeepManager.update();
        mInactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        quitCameraHandler();
        mInactivityTimer.onPause();
        mBeepManager.close();
        if (CameraManager.get() != null) {
            CameraManager.get().closeDriver();
        }
        if (!mHasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mInactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;

            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;

            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (CameraManager.get() != null) {
                    CameraManager.get().setFlashOff();
                }
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                if (CameraManager.get() != null) {
                    CameraManager.get().setFlashTorch();
                }
                return true;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater menuInflater = getMenuInflater();
        //menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.menu_gallery:
                if (mParseFailed == false) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivityForResult(intent, GET_PIC);
                }
                break;*/

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
        //return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_PIC) {
                Uri uri = intent.getData();
                Log.d(TAG, "onActivityResult uri: " + uri);
                if (uri != null) {
                    mParseImageTask = new ParseImageTask(this);
                    mParseImageTask.execute(intent);
                }
            }
        }
    }

    /**
     * Parse image from local galleray.
     */
    public class ParseImageTask extends WeakAsyncTask<Intent, Void, Intent, Activity> {
        private ProgressDialog mProgress;
        //private ImageDecodeUtils mImageDecoder;

        /**
            * Constructed function.
            * @param target Activity
            */
        public ParseImageTask(Activity target) {
            super(target);
            //mImageDecoder = new ImageDecodeUtils(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            mProgress = new ProgressDialog(target);
            mProgress.setMessage(getResources().getString(R.string.please_wait));
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);
            mProgress.show();
            super.onPreExecute(target);
            Log.d(TAG, "ParseImageTask onPreExecute");
        }

        @Override
        protected Intent doInBackground(Activity target, Intent... param) {
            Log.d(TAG, "ParseImageTask doInBackground");
            Intent result = null;
            //Intent data = param[0];
            //Bitmap bitmap;

            //bitmap = mImageDecoder.getIntentBitmap(data);
            //if (bitmap == null) {
            //   return null;
            //}
            //String rawText = mImageDecoder.decodeBitmap(bitmap);
            //result = parseResult(rawText, true);
            return result;
        }

        @Override
        protected void onPostExecute(final Activity target, Intent result) {
            if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
                mProgress = null;
            }
            super.onPostExecute(target, result);
            startContactEditorActivity(result);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "surfaceCreated() null");
        }
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
    * Callback for QRcode SDK.
    * @param result String
    * @param img Bitmap
    * @param isScan boolean
    */
    public void handleDecode(String result, Bitmap img, boolean isScan) {
        boolean isParsingImage = false;
        if (mParseImageTask != null && mParseImageTask.getStatus() == Status.RUNNING) {
            isParsingImage = true;
        }
        Log.d(TAG, "handleDecode isParsingImage: " + isParsingImage);
        if (!isParsingImage) {
            mInactivityTimer.onActivity();
            mBeepManager.playBeepSoundAndVibrate(false);
            new ParseCameraTask(this).execute(result);
        }
    }

    /**
     * Parse image from camera.
     */
    public class ParseCameraTask extends WeakAsyncTask<String, Void, Intent, Activity> {
        private ProgressDialog mProgress;

        /**
            * Constructed function.
            * @param target Activity
            */
        public ParseCameraTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            mProgress = new ProgressDialog(target);
            mProgress.setMessage(getResources().getString(R.string.please_wait));
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);
            mProgress.show();
            super.onPreExecute(target);
            Log.d(TAG, "ParseCameraTask onPreExecute");
        }

        @Override
        protected Intent doInBackground(Activity target, String... param) {
            Log.d(TAG, "ParseCameraTask doInBackground");
            String data = param[0];
            Intent intent = parseResult(data, true);
            return intent;
        }

        @Override
        protected void onPostExecute(final Activity target, Intent result) {
            if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
                mProgress = null;
            }
            super.onPostExecute(target, result);
            startContactEditorActivity(result);
        }
    }

    private void quitCameraHandler() {
        if (mHandler != null) {
            Log.i(TAG, "quitCameraHandler");
            mHandler.quitSynchronously();
            mHandler = null;
        }
    }

    private Intent parseResult(String text, boolean isLocalFirst) {
        Intent intent = null;
        String tempText = text;

        Log.d(TAG, "parseResult text: " + tempText);
        if (tempText == null || tempText.isEmpty()) {
            return intent;
        }

        if (isLocalFirst) {
            String localData = getVCardContent(tempText);
            Log.d(TAG, "parseResult localData: " + localData);
            if (localData != null) {
                VCardResultParser parser = new VCardResultParser();
                AddressBookParsedResult result = parser.parse(localData, this);
                intent = addContact(result.getFullName(),
                        result.getPhoneNumbers(),
                        result.getPhoneTypes(),
                        result.getEmails(),
                        result.getEmailTypes(),
                        result.getOrg(),
                        result.getTitle());
                return intent;
            }
        }

        String url = getNetworkUrl(tempText);
        Log.d(TAG, "parseResult url: " + url);
        if (url != null) {
            //query vCard from network
            String content;
            try {
                content = QRCodeService.getInstance(this.getApplicationContext()).
                        getContactQRCode(url);
            } catch (QRCodeException e) {
                content = null;
                Log.d(TAG, "parseResult error: " + e.mHttpErrorCode + ", " + e.mExceptionCode);
            }
            Log.d(TAG, "parseResult network: " + content);
            if (content != null && !content.isEmpty()) {
                tempText = content;
            }
        }

        String vcard = getVCardContent(tempText);
        Log.d(TAG, "parseResult vcard: " + vcard);
        if (vcard != null) {
            VCardResultParser parser = new VCardResultParser();
            AddressBookParsedResult result = parser.parse(vcard, this);
            intent = addContact(result.getFullName(),
                    result.getPhoneNumbers(),
                    result.getPhoneTypes(),
                    result.getEmails(),
                    result.getEmailTypes(),
                    result.getOrg(),
                    result.getTitle());
        }
        return intent;
    }

    private void startContactEditorActivity(Intent intent) {
        if (intent == null) {
            Log.i(TAG, "startContactEditorActivity: failed");
            //quitCameraHandler();
            mErrorView.setVisibility(View.VISIBLE);
            mStatusView.setVisibility(View.GONE);
            mViewfinderView.setVisibility(View.GONE);
            mParseFailed = true;
        } else {
            Log.i(TAG, "startContactEditorActivity: success");
            mStatusView.setText(R.string.result_address_book);
            mStatusView.setVisibility(View.VISIBLE);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Log.i(TAG, "not found activity");
                ex.printStackTrace();
            }
            finish();
        }
    }

    private String getNetworkUrl(String text) {
        String result = null;
        if (text.startsWith("http://") && text.contains(QR_CODE_TAG)) {
            int urlIndex = text.indexOf(QR_CODE_XCAP);
            if (urlIndex != -1) {
                result = text.substring(0, urlIndex + QR_CODE_XCAP.length());
            }
        }
        return result;
    }

    private String getVCardContent(String text) {
        String result = null;
        int start = text.indexOf("BEGIN:VCARD");
        if (start != -1) {
            result = text.substring(start);
        }
        return result;
    }

    private void putExtra(Intent intent, String key, String value) {
        if (value != null && !value.isEmpty()) {
            intent.putExtra(key, value);
        }
    }

    private Intent addContact(String fullName,
            String[] phoneNumbers,
            String[] phoneTypes,
            String[] emails,
            String[] emailTypes,
            String org,
            String title) {

        Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        intent.setClassName("com.android.contacts",
                "com.android.contacts.activities.CompactContactEditorActivity");
        Account account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        intent.putExtra(Insert.EXTRA_ACCOUNT, account);

        Log.d(TAG, "addContact name: " + fullName);
        putExtra(intent, ContactsContract.Intents.Insert.NAME, fullName);
        Log.d(TAG, "addContact company: " + org);
        putExtra(intent, ContactsContract.Intents.Insert.COMPANY, org);
        Log.d(TAG, "addContact title: " + title);
        putExtra(intent, ContactsContract.Intents.Insert.JOB_TITLE, title);

        mOperationList.clear();
        int phoneCount = phoneNumbers != null ? phoneNumbers.length : 0;
        for (int x = 0; x < phoneCount; x++) {
            Log.d(TAG, "addContact number: " + phoneNumbers[x]);
            if (phoneNumbers[x] != null && !phoneNumbers[x].isEmpty()) {
                //filter space in number
                String formatNumber = phoneNumbers[x].replaceAll(" ", "");
                ContentValues phoneData = new ContentValues();
                phoneData.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                phoneData.put(Phone.NUMBER, formatNumber);

                if (phoneTypes != null && x < phoneTypes.length) {
                    Log.d(TAG, "addContact number type: " + phoneTypes[x]);
                    int type = doToContractType(phoneTypes[x],
                            PHONE_TYPE_STRINGS, PHONE_TYPE_VALUES);

                    if (type >= 0) {
                        phoneData.put(Phone.TYPE, type);
                    } else {
                        phoneData.put(Phone.TYPE, Phone.TYPE_OTHER);
                        Log.d(TAG, "addContact number type other");
                    }
                }
                mOperationList.add(phoneData);
            }
        }

        int emailCount = emails != null ? emails.length : 0;
        for (int x = 0; x < emailCount; x++) {
            Log.d(TAG, "addContact email: " + emails[x]);
            if (emails[x] != null && !emails[x].isEmpty()) {
                ContentValues emailData = new ContentValues();
                emailData.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                emailData.put(Email.ADDRESS, emails[x]);

                if (emailTypes != null && x < emailTypes.length) {
                    Log.d(TAG, "addContact email type: " + emailTypes[x]);
                    int type = doToContractType(emailTypes[x],
                            EMAIL_TYPE_STRINGS, EMAIL_TYPE_VALUES);

                    if (type >= 0) {
                        emailData.put(Email.TYPE, type);
                    } else {
                        emailData.put(Email.TYPE, Email.TYPE_OTHER);
                        Log.d(TAG, "addContact email type other");
                    }
                }
                mOperationList.add(emailData);
            }
        }

        if (!mOperationList.isEmpty()) {
            intent.putParcelableArrayListExtra(Insert.DATA, mOperationList);
        }

        return intent;
    }

    private void getPhoto(byte[] photo) {
        ContentValues data = new ContentValues();
        data.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        data.put(Photo.PHOTO, photo);
        mOperationList.add(data);
    }

    private int doToContractType(String typeString, String[] types, int[] values) {
        if (typeString == null) {
            return TYPE_OTHER;
        }
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            if (typeString.startsWith(type) ||
                    typeString.startsWith(type.toUpperCase(Locale.ENGLISH))) {
                return values[i];
            }
        }
        return TYPE_OTHER;
    }

    private void initHandler() {
        if (mHandler == null) {
            Log.d(TAG, "initHandler");
            mHandler = new CaptureActivityHandler(CaptureActivity.this, null);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            Log.w(TAG, "No SurfaceHolder provided");
            //displayCameraErrorDialog();
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        //try {
            if (CameraManager.get() != null) {
                CameraManager.get().openDriver(surfaceHolder);
            }
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            initHandler();
        //} catch (IOException e) {
        //    Log.w("Unexpected Exception initializating camera", e);
        //    displayCameraErrorDialog();
        //}
    }

    private void displayCameraErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.qrcode_card));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(android.R.string.ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    private void resetStatusView() {
        mStatusView.setText(R.string.msg_default_status);
        mStatusView.setVisibility(View.VISIBLE);
        mErrorView.setVisibility(View.GONE);
        mViewfinderView.setVisibility(View.VISIBLE);
        mParseFailed = false;
    }

    /**
     * Close camera listener.
     */
    public final class FinishListener implements
            DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

        private final Activity mActivity;

        /**
            * Constructed function.
            * @param activityToFinish Activity
            */
        public FinishListener(Activity activityToFinish) {
            mActivity = activityToFinish;
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            mActivity.finish();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            mActivity.finish();
        }
    }

}
