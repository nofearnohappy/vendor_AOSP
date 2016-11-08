/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.contacts.vcard;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.util.RequestPermissionsActivity;
import com.mediatek.rcs.contacts.util.WeakAsyncTask;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryHandler;

import java.util.ArrayList;
import java.util.List;

public class VCardViewActivity extends Activity implements VCardEntryHandler {

    private static final String LOG_TAG = VCardViewActivity.class.getSimpleName();
    private static final int DIALOG_WAIT = 0;
    private ArrayList<ContentProviderOperation> mOperationList;
    private VCardParserResult mResult;
    private ViewGroup mVcardViewGroup;
    private ViewGroup[] mVcardNumbers;
    private ViewGroup[] mVcardEmails;
    private ImageView mImagePhoto;
    private View mView;
    private TextView mName;
    private TextView mCompany;
    private TextView mTitle;
    private int mNumberIndex;
    private int mEmailIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);

        /*Disable android runtime permission check in op01 operator*/
        //if (RequestPermissionsActivity.startPermissionActivity(this)) {
        //    log("requestPermissions");
        //    return;
        //}

        configureActionBar();
        setContentView(R.layout.rcs_vcard);

        mImagePhoto = (ImageView) findViewById(R.id.photo);
        mName = (TextView) findViewById(R.id.name);
        mCompany = (TextView) findViewById(R.id.company);
        mTitle = (TextView) findViewById(R.id.title);

        mVcardViewGroup = (ViewGroup) findViewById(R.id.vcard);
        mNumberIndex = mVcardViewGroup.indexOfChild((View)mVcardViewGroup.findViewById(R.id.number));
        mEmailIndex = mVcardViewGroup.indexOfChild((View)mVcardViewGroup.findViewById(R.id.email));

        Uri uri = getIntent().getData();
        log("uri: " + uri);
        String type = getIntent().getType();
        log("type: " + type);
        if((type.equals("text/x-vcard") || type.equals("text/directory") || 
                type.equals("text/vcard")) && uri != null) {
            parseVCard(uri);
        }else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_WAIT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }

    protected void configureActionBar() {
        log("configureActionBar()");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(R.layout.vcard_action_bar, null);

        //dispaly the "Cancel" button.
        ImageButton cancelView = (ImageButton) customActionBarView.findViewById(R.id.cancel_menu_item);
        cancelView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                log("exit vcard");
                finish();
                return;
            }
        });

        //dispaly the "OK" button.
        ImageButton doneView = (ImageButton) customActionBarView.findViewById(R.id.done_menu_item);
        doneView.setOnClickListener(getClickListenerOfActionBarOKButton());

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected OnClickListener getClickListenerOfActionBarOKButton() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                log("save vcard");
                new SaveContactTask(VCardViewActivity.this).execute();
                return;
            }
        };
    }

    private void parseVCard(Uri uri) {
        mParseVCardTask = new ParseVCardTask(this);
        mParseVCardTask.execute(uri);
    }

    private void parseResult(int type) {
        final String error;
        switch (type) {
            case VCardParserUtil.SAVE_CONATCT_SUCCESS:
                error = getString(R.string.contact_saved);
                break;

            case VCardParserUtil.SAVE_CONATCT_FAIL:
                error = getString(R.string.contact_saved_error);
                break; 
            
            case VCardParserUtil.VCARD_IO_ERROR:
                error = getString(R.string.fail_reason_io_error);
                break;

            case VCardParserUtil.VCARD_NO_FILE_ERROR:
                error = getString(R.string.fail_reason_no_vcard_file);
                break;

            default:
                error = getString(R.string.fail_reason_not_supported);
                break;
        }
        log("parseError: " + error);
        VCardViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        VCardViewActivity.this.getApplicationContext(), 
                        error, Toast.LENGTH_SHORT).show();
            }
        });      
    }

    private ParseVCardTask mParseVCardTask = null;
    /**
     * parse vCard data
     */
    public class ParseVCardTask extends WeakAsyncTask<Uri, Void, Integer, Activity> {

        public ParseVCardTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            super.onPreExecute(target);
            log("ParseVCardTask--onPreExecute");
            showDialog(DIALOG_WAIT);
        }

        @Override
        protected Integer doInBackground(Activity target, Uri... param) {
            log("ParseVCardTask--doInBackground");
            Uri uri = param[0];
            int result = VCardParserUtil.ParseVCard(uri, (VCardEntryHandler)target, 
                    VCardViewActivity.this.getContentResolver());
            log("result: " + result);
            return result;
        }

        @Override
        protected void onPostExecute(final Activity target, Integer result) {
            super.onPostExecute(target, result);
            log("ParseVCardTask--onPostExecute");
            dismissDialogSafely(DIALOG_WAIT);
            if (result != VCardParserUtil.VCARD_NO_ERROR) {
                parseResult(result);
            } else {
                updateUI();
            }
        }
    }

    /**
     * save Contact
     */
    public class SaveContactTask extends WeakAsyncTask<Void, Void, Uri, Activity> {

        public SaveContactTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            super.onPreExecute(target);
            log("SaveContactTask--onPreExecute");
            showDialog(DIALOG_WAIT);
        }

        @Override
        protected Uri doInBackground(Activity target, Void... param) {
            log("SaveContactTask--doInBackground");
            Uri uri = saveContact(mOperationList);
            log("result: " + uri);
            return uri;
        }

        @Override
        protected void onPostExecute(final Activity target, Uri result) {
            super.onPostExecute(target, result);
            log("SaveContactTask--onPostExecute");
            dismissDialogSafely(DIALOG_WAIT);
            if (result != null) {
                parseResult(VCardParserUtil.SAVE_CONATCT_SUCCESS);
            } else {
                parseResult(VCardParserUtil.SAVE_CONATCT_FAIL);
            }
            finish();
        }
    }

    /**
     * Activity onStart and parse onStart
     */
    @Override
    public void onStart() {
        log("onStart");
        super.onStart();
    }

    @Override
    public void onEntryCreated(VCardEntry entry) {
        mResult = VCardParserUtil.ParseVCardEntry(entry, this);
        mOperationList = entry.constructInsertOperations(this.getContentResolver(), mOperationList);
        log("onEntryCreated");
    }

    private void updateUI() {
        VCardParserResult result = mResult;
        if (result == null) {
            log("updateUI result null");
            return;
        }
        
        String name = result.getName();
        log("updateUI Name: " + name);
        if (name != null && !TextUtils.isEmpty(name)) {
            mName.setText(getString(R.string.name_tag) + name);
        }
        
        String organization = result.getOrganization();
        log("updateUI Organization: " + organization);
        if (organization != null && !TextUtils.isEmpty(organization)) {
            mCompany.setText(getString(R.string.company_tag) + organization);
        }
        
        String title = result.getTitle();
        log("updateUI Title: " + title);
        if (title != null && !TextUtils.isEmpty(title)) {
            mTitle.setText(getString(R.string.title_tag) + title);
        }

        byte[] photo = result.getPhoto();
        if (photo != null) {
            log("updateUI Photo: " + photo.toString());
            Bitmap bpPhoto = getPicFromBytes(photo);
            mImagePhoto.setImageBitmap(bpPhoto);
        }

        List<VCardData> numberList = result.getNumber();
        if(numberList != null) {
             for (int i = 0; i < numberList.size(); i++) {
                 LinearLayout newItem = newItemLayoutView(numberList.get(i).getData(), numberList.get(i).getType());
                 ++mNumberIndex;
                 ++mEmailIndex;
                 mVcardViewGroup.addView(newItem, mNumberIndex);
                 log("numberList.get(" + i + ").getData(): " + numberList.get(i).getData());
             }
        }

        List<VCardData> emailList = result.getEmail();
        if(emailList != null) {
            for (int i = 0; i < emailList.size(); i++) {
                LinearLayout newItem = newItemLayoutView(emailList.get(i).getData(), emailList.get(i).getType());
                ++mEmailIndex;
                mVcardViewGroup.addView(newItem, mEmailIndex);
                log("emailList.get(" + i + ").getData(): " + emailList.get(i).getData());
            }
       }
    }

    /**
     * parse end
     */
    @Override
    public void onEnd() {
        log("onEnd");
    }

    private void dismissDialogSafely(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            log("IllegalArgumentException");
        }
    }

    /**
     * Construct item view
     * @param text
     * @param label
     * @return new item LinearLayout
     */
    public LinearLayout newItemLayoutView(String text, String label) {
        log("newItemLayoutView");
        Resources r = getResources(); 
        int view_margin = r.getDimensionPixelSize(R.dimen.view_margin);
        int vCard_margin = r.getDimensionPixelSize(R.dimen.vCard_margin);
        int divider_margin = r.getDimensionPixelSize(R.dimen.divider_margin);
        float text_size2 = px2sp(this, r.getDimension(R.dimen.prompt_text_size2));
        float text_size3 = px2sp(this, r.getDimension(R.dimen.prompt_text_size3));

        //new linelayout
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        LinearLayout newItemLayoutView = new LinearLayout(this); 
        newItemLayoutView.setLayoutParams(params);
        newItemLayoutView.setOrientation(LinearLayout.VERTICAL);
        newItemLayoutView.setBaselineAligned(false);

        //text view
        LayoutParams numberParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        numberParams.setMargins(view_margin, vCard_margin, view_margin, 0);
        TextView textView = new TextView(this);
        textView.setLayoutParams(numberParams);
        textView.setEllipsize(TruncateAt.END);
        textView.setSingleLine();
        textView.setText(text);
        textView.setTextSize(text_size2);
        textView.setVisibility(View.VISIBLE);
        newItemLayoutView.addView(textView);

        //label text view
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        labelParams.setMargins(view_margin, 0, view_margin, vCard_margin);
        TextView labelView = new TextView(this);
        labelView.setLayoutParams(labelParams);
        labelView.setEllipsize(TruncateAt.END);
        labelView.setSingleLine();
        labelView.setText(label);
        labelView.setTextSize(text_size3);
        labelView.setVisibility(View.VISIBLE);
        newItemLayoutView.addView(labelView);

        //divider view
        LayoutParams dividerParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(divider_margin, 0, divider_margin, 0);
        View dividerView = new View(this);
        dividerView.setLayoutParams(dividerParams);
        dividerView.setBackgroundColor(R.color.secondary_header_separator_color);
        dividerView.setVisibility(View.VISIBLE);
        newItemLayoutView.addView(dividerView);

        return newItemLayoutView;
    }

    /**
     *px to sp
     * @param spValue
     * @param fontScale
     * @return sp
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * bytes to Bitmap
     * @param bytes
     * @return Bitmap
     */
    private Bitmap getPicFromBytes(byte[] bytes) {
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;  
    }

    private Uri saveContact(ArrayList<ContentProviderOperation> operationList) {
        if (operationList == null) {
            log("saveContact operation null"); 
            return null;
        }

        try {
            operationList.remove(0);
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newInsert(RawContacts.CONTENT_URI);
            builder.withValue(RawContacts.ACCOUNT_NAME, VCardParserUtil.ACCOUNT_NAME);
            builder.withValue(RawContacts.ACCOUNT_TYPE, VCardParserUtil.ACCOUNT_TYPE);
            builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
            //builder.withValue("aggregation_needed", 1);
            operationList.add(0, builder.build());
            
            final ContentProviderResult[] results = this.getContentResolver().applyBatch(
                    ContactsContract.AUTHORITY, operationList);

            return ((results == null || results.length == 0 || results[0] == null)
                            ? null : results[0].uri);
        } catch (RemoteException e) {
            log(String.format("%s: %s", e.toString(), e.getMessage()));
            return null;
        } catch (OperationApplicationException e) {
            log(String.format("%s: %s", e.toString(), e.getMessage()));
            return null;
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
