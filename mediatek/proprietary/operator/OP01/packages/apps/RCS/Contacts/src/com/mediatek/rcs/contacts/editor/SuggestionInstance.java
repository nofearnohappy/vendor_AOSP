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

package com.mediatek.rcs.contacts.editor;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ListPopupWindow;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.contacts.editor.JoinSuggestedContactDialogFragment;
import com.android.contacts.editor.SuggestionEditConfirmationDialogFragment;
import com.android.contacts.common.model.RawContactDelta;

import com.mediatek.rcs.contacts.editor.SuggestionEngine;
import com.mediatek.rcs.contacts.editor.SuggestionEngine.Suggestion;
import com.mediatek.rcs.contacts.R;

import java.util.List;

public class SuggestionInstance implements SuggestionEngine.Listener {
    private static final String TAG = SuggestionInstance.class.getSimpleName();
    private static final String ACCOUNT_TYPE_SIM = "SIM Account";
    private static final String ACCOUNT_TYPE_USIM = "USIM Account";
    private static final String ACCOUNT_TYPE_UIM = "UIM Account";
    
    private Context mContext;
    private ListPopupWindow mSuggestionPopup;    
    private SuggestionEngine mSuggestionEngine;
    private Fragment mFragment;
    private FragmentManager mFragmentManager;
    private RawContactDelta mState;
    
    public SuggestionInstance(Context context) {
        mContext = context;
        mContext.setTheme(R.style.SuggestionStyle);
    }

    public void startSuggestionEngine () {
        Log.d(TAG, "startSuggestionEngine");
        if (mSuggestionEngine == null) {
            Log.d(TAG, "startSuggestionEngine create");
            mSuggestionEngine = new SuggestionEngine(mContext);
            mSuggestionEngine.setListener(this);
            mSuggestionEngine.start();
        }
    }

    @Override
    public void onSuggestionChange(EditText view) {
        Log.d(TAG, "onSuggestionChange");
        closePopupListView();
        if (mSuggestionEngine == null) {
            Log.d(TAG, "onSuggestionChange mSuggestionEngine null");
            return;
        }      
        if (mSuggestionEngine.getSuggestedCount() == 0) {
            Log.d(TAG, "onSuggestionChange no count");
            return;
        }         
        if (view != null && view.isFocused()) {           
            createPopupListView(view);
        }
    }

    public void setState(RawContactDelta state) {
        mState = state;
    }

    public void setFragment(Fragment fragment, FragmentManager manager) {
        mFragment = fragment;
        mFragmentManager = manager;
    }

    private long getContactId() {
        if (mState != null) {
            Long contactId = mState.getValues().getAsLong(RawContacts.CONTACT_ID);
            if (contactId != null) {               
                return contactId;
            }
        }
        return 0;
    }

    private static final class SuggestionAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<Suggestion> mSuggestions;

        public SuggestionAdapter(Context context, List<Suggestion> suggestions) {
            mInflater = LayoutInflater.from(context);            
            mSuggestions = suggestions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {       
            final View view;
          
            view = mInflater.inflate(R.layout.rcs_editor_suggestions_item, null);
            Suggestion suggestion = (Suggestion) getItem(position);

            TextView name = (TextView) view.findViewById(R.id.suggestion_name); 
            name.setText(suggestion.name);
            Log.d(TAG, "getView name: " + name.getText());
            
            TextView data = (TextView) view.findViewById(R.id.suggestion_data);
            data.setText(suggestion.number);
            Log.d(TAG, "getView number: " + data.getText());

            ImageView photo = (ImageView) view.findViewById(R.id.suggestion_photo);    
            if (suggestion.photo != null) {
                photo.setImageBitmap(BitmapFactory.decodeByteArray(
                suggestion.photo, 0, suggestion.photo.length));
            } else {
                photo.setImageResource(R.drawable.ic_person_white_120dp);
            }
          
            return view;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public Object getItem(int position) {
            return mSuggestions.get(position);
        }
        
        @Override
        public int getCount() {
            return mSuggestions.size();
        }
    }

    public void onEditAction(Uri contactLookupUri) {
        Log.d(TAG, "onEditAction uri: " + contactLookupUri);
        SuggestionEditConfirmationDialogFragment dialog =
                new SuggestionEditConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("contactUri", contactLookupUri);
        dialog.setArguments(args);
        dialog.setTargetFragment(mFragment, 0);
        dialog.show(mFragmentManager, "edit");
    }

    public void onJoinAction(List<Long> rawContactIdList) {
        if (rawContactIdList != null) {
            long rawContactIds[] = new long[rawContactIdList.size()];
            for (int i = 0; i < rawContactIds.length; i++) {            
                rawContactIds[i] = rawContactIdList.get(i);
                Log.d(TAG, "onJoinAction rawContact: " + rawContactIds[i]);
            }
            JoinSuggestedContactDialogFragment dialog =
                    new JoinSuggestedContactDialogFragment();
            Bundle args = new Bundle();
            args.putLongArray("rawContactIds", rawContactIds);
            dialog.setArguments(args);
            dialog.setTargetFragment(mFragment, 0);
            dialog.show(mFragmentManager, "join");
        }
    }

    public void createPopupListView(EditText view) {
        Log.d(TAG, "CreatePopupListView view: " + view.toString());
        mSuggestionPopup = new ListPopupWindow(mContext, null);
        final SuggestionAdapter adapter = new SuggestionAdapter(mContext, 
            mSuggestionEngine.getSuggestions());
        mSuggestionPopup.setAnchorView(view);
        mSuggestionPopup.setWidth(view.getWidth());
        mSuggestionPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        mSuggestionPopup.setAdapter(adapter);
        mSuggestionPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Suggestion suggestion = adapter.mSuggestions.get(position);
                long contactId = suggestion.contactId;
                //if (mState.isContactInsert()) {
                String contactkey = suggestion.lookupKey;
                Uri uri = Contacts.getLookupUri(contactId, contactkey);
                onEditAction(uri);
                //} else {
                    //onJoinAction(suggestion.rawContacts);
                //}
                closePopupListView();
            }
        });
        mSuggestionPopup.show();
    }

    public void closePopupListView() {
        Log.d(TAG, "closePopupListView");
        if (mSuggestionPopup != null && mSuggestionPopup.isShowing()) {
            mSuggestionPopup.dismiss();
            mSuggestionPopup = null;
        }
    }

    public void suggestionLookup(String number, EditText view) {
        Log.d(TAG, "suggestionLookup view: " + view.toString());
        if (mState == null) {
            Log.d(TAG, "suggestionLookup mState null");
            return;
        }
        String accountType = mState.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        if (accountType != null && (accountType.equals(ACCOUNT_TYPE_SIM) 
                || accountType.equals(ACCOUNT_TYPE_USIM) || accountType.equals(ACCOUNT_TYPE_UIM))) {
            Log.d(TAG, "suggestionLookup sim type");
            return;
        }
        //if (mState.isContactInsert()) {
        mSuggestionEngine.setContactId(getContactId());
        mSuggestionEngine.scheduleSuggestionLookup(number, view);
        //}
    }

    public void closeLookup() {
        Log.d(TAG, "closeLookup");
        if (mSuggestionEngine != null) {
            Log.d(TAG, "close SuggestionEngine");
            mSuggestionEngine.quit();
            mSuggestionEngine = null;
        }
    }
}
