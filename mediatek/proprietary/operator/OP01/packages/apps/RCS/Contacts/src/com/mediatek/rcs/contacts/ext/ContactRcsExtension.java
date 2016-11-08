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

package com.mediatek.rcs.contacts.ext;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.InputType;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.editor.ContactEditorBaseFragment;
import com.google.common.collect.ImmutableList;
import com.mediatek.common.PluginImpl;
import com.mediatek.contacts.ext.DefaultRcsExtension;
import com.mediatek.contacts.ext.IRcsExtension.ContactListItemRcsView;
import com.mediatek.contacts.ext.IRcsExtension.QuickContactRcsScroller;
import com.mediatek.contacts.list.PhoneNumbersPickerFragment;
import com.mediatek.rcs.contacts.PluginApiManager;
import com.mediatek.rcs.contacts.PluginApiManager.ContactInformation;
import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.editor.SuggestionInstance;
import com.mediatek.rcs.contacts.list.GroupMemberFragment;
import com.mediatek.rcs.contacts.profileapp.ProfilePlugin;
import com.mediatek.rcs.contacts.richscreen.RichScreenManager;
import com.mediatek.rcs.contacts.util.SubInfoUtils;

import org.gsma.joyn.JoynServiceConfiguration;

@PluginImpl(interfaceName="com.mediatek.contacts.ext.IRcsExtension")
public class ContactRcsExtension extends DefaultRcsExtension {

    private static final String TAG = "ContactRcsExtension";
    private static final String ACTION_PICK_MULTIPHONES = 
            "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String ACTION_PICK_GROUPMEMBER =
            "android.intent.action.rcs.contacts.GroupMemberActivity";
    private static final String ACTION_QRCODE_SCAN =
            "android.intent.action.rcs.contacts.qrcode.SCAN";
    private static final String FRAGMENT_ARGS = "rcsintent";
    
    private static final int MENU_CHAT = Menu.FIRST;
    private static final int MENU_GROUP = Menu.FIRST + 1;
    private static final int MENU_BACKUP_RESTORE = Menu.FIRST + 2;
    private static final int MENU_EDITOR = Menu.FIRST + 3;
    private static final int MENU_CONTACT_DETAIL = Menu.FIRST + 4;
    private static final int MENU_GROUP_DETAIL = Menu.FIRST + 5;
    private static final String PREF_REMIND = "pref_remind";
    private static final int INVALID_ID = -1;
    private static final String RCS_ACCOUNT = "Rich Communication Suite";

    private String mCountryCode;
    private Context mContext;
    private SuggestionInstance mSuggestion;
    private ContactExtention mContactPlugin = null;
    private PluginApiManager mInstance = null;

    public ContactRcsExtension(Context context) {
        mContext = context;
        Log.d(TAG, "[ContactRcsExtension] constuctor entry " + context);
        mContactPlugin = new ContactExtention(mContext);
        mInstance = PluginApiManager.getInstance();
        Log.d(TAG, "[ContactRcsExtension] constuctor mInstance = "
                + mInstance + " mContactPlugin = " + mContactPlugin);
    }

    private void setCountryCode() {
        if (mCountryCode == null) {
            final CountryDetector countryDetector =
                    (CountryDetector) mContext.getSystemService(Context.COUNTRY_DETECTOR);
            mCountryCode = countryDetector.detectCountry().getCountryIso();
        }
        Log.d(TAG, "setCountryCode: " + mCountryCode);
    }

    @Override
    public void getIntentData(Intent intent, Fragment fragment) {
        if (fragment instanceof PhoneNumbersPickerFragment) {
            Log.d(TAG, "[getIntentData]");
            PhoneNumbersPickerFragment listFragment =
                    (PhoneNumbersPickerFragment) fragment;
            Bundle bundle = new Bundle();
            bundle.putParcelable(FRAGMENT_ARGS, intent);
            listFragment.setArguments(bundle);
        }
    }

    @Override
    public void setListFilter(StringBuilder selection, Context context) {
        Log.d(TAG, "[setListFilter]");
        setCountryCode();
        Activity activity = (Activity) context;
        final Intent intent = activity.getIntent();
        String[] existNumbers = intent.getStringArrayExtra("ExistNumberArray");

        if (existNumbers != null) {
            StringBuilder numbers = new StringBuilder();
            StringBuilder numbersE164 = new StringBuilder();
            String numberE164 = null;
            boolean hasNumberE164 = false;
            numbers.append("(");
            numbersE164.append("(");
            for (String number : existNumbers) {
                numbers.append("'");
                numbers.append(number);
                numbers.append("'");
                numbers.append(",");
                //filter E164 number, eg:+8613510001000
                numberE164 = PhoneNumberUtils.formatNumberToE164(number, mCountryCode);
                if (numberE164 != null) {
                    numbersE164.append("'");
                    numbersE164.append(numberE164);
                    numbersE164.append("'");
                    numbersE164.append(",");
                    hasNumberE164 = true;
                }
            }
            if (existNumbers.length > 0) {
                numbers.deleteCharAt(numbers.length() - 1);
            }
            numbers.append(")");
            if (hasNumberE164) {
                numbersE164.deleteCharAt(numbersE164.length() - 1);
            }
            numbersE164.append(")");
            selection.append("(");
            selection.append(Phone.NUMBER + " NOT IN " + numbers);
            if (hasNumberE164) {
                selection.append(" AND ");
                selection.append("(");
                selection.append(Phone.NORMALIZED_NUMBER + " NOT IN " + numbersE164);
                selection.append(" OR ");
                selection.append(Phone.NORMALIZED_NUMBER + " IS NULL ");
                selection.append(")");
            }
            selection.append(")");
            Log.d(TAG, "[setListFilter]: " + selection.toString());
        }
    }

    @Override
    public Uri configListUri(Uri uri) {
        Log.d(TAG, "[configListUri]");
        Uri result;
        result = uri.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true").build();
        return result;
    }

    @Override
    public void addListMenuOptions(final Context context, Menu menu,
            MenuItem item, Fragment fragment) {
        if (fragment instanceof PhoneNumbersPickerFragment) {
            MenuItem group = menu.findItem(MENU_GROUP);
            if (group == null) {
                PhoneNumbersPickerFragment listFragment =
                        (PhoneNumbersPickerFragment) fragment;
                final Intent intent = listFragment.getArguments().getParcelable(FRAGMENT_ARGS);
                boolean enableGroup = intent.getBooleanExtra("Group", false);
                final String[] existNumbers = intent.getStringArrayExtra("ExistNumberArray");
                Log.d(TAG, "[addListMenuOptions]" + enableGroup);

                if (enableGroup) {
                    String string = mContext.getResources().getString(R.string.group_label);
                    group = menu.add(0, MENU_GROUP, 0, string);
                    group.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    group.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Intent intent = new Intent(ACTION_PICK_GROUPMEMBER);
                            intent.putExtra("Group", true);
                            intent.putExtra("ExistNumberArray", existNumbers);
                            Activity activity = (Activity) context;
                            activity.startActivityForResult(intent,
                                    GroupMemberFragment.GROUP_RESULT_CODE);
                            return true;
                        }
                    });
                }
            }
            //dismiss search button in phone number list
            if (item != null && item.isVisible()) {
                item.setVisible(false);
            }
        }
    }

    @Override
    public void getGroupListResult(Fragment fragment, long[] ids) {
        Log.d(TAG, "[getGroupListResult]");
        if (fragment instanceof PhoneNumbersPickerFragment) {
            Log.d(TAG, "[getGroupListResult]fragment: " + fragment);
            PhoneNumbersPickerFragment listFragment = (PhoneNumbersPickerFragment) fragment;
            listFragment.markItemsAsSelectedForCheckedGroups(ids);
        }    
    }
    
    @Override
    public void addGroupMenuOptions(Menu menu, final Context context) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "[addGroupMenuOptions]RCS off");
            return;
        }

        int size = menu.size();
        Log.d(TAG, "[addGroupMenuOptions]size: " + size);
        if (size > 0) {
            MenuItem add = menu.getItem(size - 1);
            if (add != null) {
                Log.d(TAG, "[addGroupMenuOptions]set add icon");
                Drawable addIcon = mContext.getResources().
                        getDrawable(R.drawable.ic_add_group_holo_dark);
                add.setIcon(addIcon);
            }
        }

        MenuItem chat = menu.findItem(MENU_CHAT);
        if (chat == null) {
            Log.d(TAG, "[addGroupMenuOptions]set chat item");
            String string = mContext.getResources().getString(R.string.group_chat);
            chat = menu.add(0, MENU_CHAT, 0, string);
            Drawable chatIcon = mContext.getResources().getDrawable(R.drawable.ic_add_chat_holo_dark);
            chat.setIcon(chatIcon);
            chat.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            
            chat.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent intent = new Intent("android.intent.action.rcs.contacts.GroupChatActivity");
                    context.startActivity(intent);
                    return true;
                }
            });
        }
    }

    @Override
    public void addGroupDetailMenuOptions(Menu menu, final long groupId,
            final Context context, int groupSize) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "[addGroupDetailMenuOptions]RCS off");
            return;
        }

        Log.d(TAG, "[addGroupDetailMenuOptions]");
        MenuItem detail = menu.findItem(MENU_GROUP_DETAIL);
        if (detail == null) {
            String string = mContext.getResources().getString(R.string.rich_screen);
            detail = menu.add(0, MENU_GROUP_DETAIL, 0, string);
            detail.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            detail.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    RichScreenManager richScreen = new RichScreenManager(mContext);
                    richScreen.startRichScreenForGroup(groupId, context);
                    return true;
                }
            });
        }
        if (groupSize > 0) {
            detail.setVisible(true);
        } else {
            detail.setVisible(false);
        }
    }

    @Override
    public void addEditorMenuOptions(final Fragment fragment, Menu menu, boolean isInsert) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "[addEditorMenuOptions]RCS off");
            return;
        }

        Log.d(TAG, "[addEditorMenuOptions]");
        if (isInsert) {
            MenuItem editor = menu.findItem(MENU_EDITOR);
            if (editor == null) {
                String string = mContext.getResources().getString(R.string.qrcode_card);
                editor = menu.add(0, MENU_EDITOR, 0, string);
                //Drawable editorIcon = mContext.getResources().getDrawable(R.drawable.qrcode_card);
                //editor.setIcon(editorIcon);
                editor.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                editor.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ContactEditorBaseFragment editor = (ContactEditorBaseFragment) fragment;
                        editor.onCancelEditConfirmed();
                        Intent intent = new Intent(ACTION_QRCODE_SCAN);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public void addQuickContactMenuOptions(Menu menu, final Uri uri, final Context context) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "[addQuickContactMenuOptions]RCS off");
            return;
        }

        Log.d(TAG, "[addQuickContactMenuOptions]");
        MenuItem detail = menu.findItem(MENU_CONTACT_DETAIL);
        if (detail == null) {
            String string = mContext.getResources().getString(R.string.rich_screen);
            detail = menu.add(0, MENU_CONTACT_DETAIL, 0, string);
            detail.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            detail.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    RichScreenManager richScreen = new RichScreenManager(mContext);
                    richScreen.startRichScreenForSingleContact(uri, context);
                    return true;
                }
            });
        }
    }

    /**
     * OP01 RCS will go to personal profile activity, once clicked profile item.
     * @param uri. contact lookup uri.
     * @param isEmpty. is current profile is empty.
     * @return 1: is profile, and listentenr set sucess. 0. listener set unsucessful
     */
    @Override
    public boolean addRcsProfileEntryListener(Uri uri, boolean isEmpty) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.d(TAG, "[addRcsProfileEntryListener]RCS off");
            return false;
        }
        long contactId = -1;
        if (uri != null) {
            Log.i(TAG, "[addRcsProfileEntryListener]id = " + uri.getLastPathSegment());
            contactId = ContentUris.parseId(uri);
            
        }
        if (isEmpty == true || ContactsContract.isProfileId(contactId)) {
            Intent i = new Intent("android.intent.action.view.profile");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
            return true;
        }
        return false;

    }
    
    /**
      * OP01 RCS will go to contact detail activity, update photo from rcs server.
      * @param lookupUri. contact lookup uri.
      * @param photo. Contact photoView.
      */
    @Override
    public void updateContactPhotoFromRcsServer(Uri lookupUri, ImageView photo, Context context) {
        Log.d(TAG, "[updateContactPhotoFromRcsServer]uri = " + lookupUri.toString());
        String last = lookupUri.getLastPathSegment();
        if (last.equals("profile")
                || last.equals("encoded")
                || !JoynServiceConfiguration.isServiceActivated(mContext)) {
            return ;
        }
        long contactId = ContentUris.parseId(lookupUri);

        final String type = mContext.getContentResolver().getType(lookupUri);
        Log.d(TAG, "[updateContactPhotoFromRcsServer]id = " + contactId + " type=" + type);
        if (type.equals(Contacts.CONTENT_ITEM_TYPE)) {
            String selection = RawContacts.CONTACT_ID + " = " + contactId;
            final Cursor cursor = mContext.getContentResolver()
                    .query(RawContacts.CONTENT_URI, new String[] {
                    RawContacts.CONTACT_ID, RawContacts._ID
            }, selection, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    final long rawId = cursor.getLong(cursor.getColumnIndex(RawContacts._ID));
                    Log.d(TAG, "[updateContactPhotoFromRcsServer]New raw id = " + rawId);
                    ProfilePlugin.getInstance()
                            .getContactPhotoFromServer(rawId,
                            context.getApplicationContext(), null);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (type.equals(RawContacts.CONTENT_ITEM_TYPE)) {
            ProfilePlugin.getInstance()
                    .getContactPhotoFromServer(contactId,
                    context.getApplicationContext(), null);
        }
    }

    /**
     * OP01 RCS remove rich communication Suite account.
     * @param rawContacts original rawContacts list.
     * @param isProfile if is a profile.
     * @return cofigured rawContacts list.
     */
    @Override
    public ImmutableList<RawContact> rcsConfigureRawContacts(
                    ImmutableList<RawContact> rawContacts, boolean isProfile) {

        if (isProfile) {
            return rawContacts;
        }

        ImmutableList.Builder<RawContact> rawContactsBuilder =
                    new ImmutableList.Builder<RawContact>();

        for (RawContact raw : rawContacts) {
            String accountName = raw.getAccountName();
            if (accountName != null && accountName.equals(RCS_ACCOUNT)) {
                Log.d(TAG, "remove " + accountName);
            } else {
                rawContactsBuilder.add(raw);
                Log.d(TAG, "add " + accountName);
            }
        }
        return rawContactsBuilder.build();
    }

    /**
     * OP01 RCS will get photo from rcs server, and refresh thumbnail Photo.
     * @param isLetterTile Letter photo or thumbnail Photo.
     * @param hasThemeColor Theme Color has set or not.
     * @return true or false.
     */
    @Override
    public boolean needUpdateContactPhoto(boolean isLetterTile, boolean hasThemeColor) {
        return isLetterTile && hasThemeColor;
    }

    @Override
    public void addPeopleMenuOptions(Menu menu) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "[addPeopleMenuOptions] RCS OFF ");
            return;
        }
        menu.add(0, MENU_BACKUP_RESTORE, 0, mContext.getResources().getString(R.string.backup_restore))
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG, "[addPeopleMenuOptions]launch MENU_BACKUP_RESTORE");
                Intent intent = new Intent();
                intent.setClassName("com.mediatek.rcs.contacts", "com.mediatek.rcs.contacts.networkcontacts.ui.HomeActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void setTextChangedListener(RawContactDelta state, EditText view,
            int inputType, String number) {
        Log.d(TAG, "[setTextChangedListener]inputType: " + inputType);
        if (inputType == InputType.TYPE_CLASS_PHONE) {
            if (mSuggestion == null) {
                Log.d(TAG, "[setTextChangedListener]create SuggestionInstance");
                mSuggestion = new SuggestionInstance(mContext);                
            }
            mSuggestion.startSuggestionEngine();
            mSuggestion.setState(state);
            Log.d(TAG, "[setTextChangedListener]number: " + number);
            mSuggestion.suggestionLookup(number, view);
        }
    }

    @Override
    public void closeTextChangedListener(boolean quit) {
        Log.d(TAG, "[closeTextChangedListener]");
        if (mSuggestion != null) {
            if (quit == false) {
                mSuggestion.closePopupListView();
            } else {
                Log.d(TAG, "[close SuggestionInstance]");
                mSuggestion.closeLookup();
                mSuggestion = null;
            }
        }
    }

    @Override
    public void setEditorFragment(Fragment fragment, FragmentManager manager) {
        Log.d(TAG, "[setEditorFragment]");
        if (mSuggestion == null) {
            Log.d(TAG, "[setEditorFragment]create SuggestionInstance");
            mSuggestion = new SuggestionInstance(mContext);                
        }
        mSuggestion.setFragment(fragment,manager);
    }

    /**
     * OP01 RCS will add public account entry to people list.
     * @param context. context of the activity
     * @param listView. list view of contact info
     */
    @Override
    public void createPublicAccountEntryView(ListView list) {
        if (!JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "RCS switch off, hide header");
            return;
        }

        Log.i(TAG, "createPublicAccountEntryView, list is " + list);
        mContext.setTheme(R.style.SuggestionStyle);
        list.addHeaderView(buildContactHeaderView(mContext));
    }

    private View buildContactHeaderView(final Context context) {
        LinearLayout entryItem = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.rcs_public_account_entry, null);

        entryItem.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.mediatek.rcs.pam.activities.AccountListActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        return entryItem;
    }

    /**
     * Interface implementation instance for Contact List API.
     *
     * @return the contact list item view custom
     */
    @Override
    public ContactListItemRcsView getContactListItemRcsView() {
        return mContactListItemRcsView;
    }

    /**
     * Interface implementation instance for adding Joyn Icon
     * in scrollable item on top in QuickContactActivity.
     * @return the quick contact scroller custom
     */
    @Override
    public QuickContactRcsScroller getQuickContactRcsScroller() {
        return mQuickContactRcsScroller;
    }

    /**
     * check if RCS service is run.
     *
     * @return RCS service is available
     */
    @Override
    public boolean isRcsServiceAvailable() {
        return JoynServiceConfiguration.isServiceActivated(mContext);
    }

    private ContactListItemRcsView mContactListItemRcsView = new ContactListItemRcsView() {

        private ImageView mJoynIconView = null;
        private View mIconLayout = null;
        private int mJoynIconViewWidth;
        private int mJoynIconViewHeight;
        private long mContactId = INVALID_ID;
        private static final int RCS_ICON_ID = 100;

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            Log.d(TAG, "[onMeasure] mJoynIconView:" + mJoynIconView);

            if (mContactPlugin == null || mIconLayout == null) {
                Log.d(TAG, "[onMeasure] mContactPlugin is null");
                return;
            }
            if (mContactPlugin.isEnabled() == false) {
                return;
            }

            if (isVisible(mJoynIconView)) {
                if (mContactPlugin != null) {
                    Drawable a = mContactPlugin.getAppIcon(false);
                    if (a != null) {
                        mJoynIconViewWidth = a.getIntrinsicWidth();
                        mJoynIconViewHeight = a.getIntrinsicHeight();
                        //mJoynIconView.measure(mJoynIconViewWidth, mJoynIconViewHeight);
                        mIconLayout.measure(mJoynIconViewWidth, mJoynIconViewHeight);
                    } else {
                        mJoynIconViewWidth = 0;
                        mJoynIconViewHeight = 0;
                    }
                } else {
                    mJoynIconViewWidth = 0;
                    mJoynIconViewHeight = 0;
                }
                Log.d(TAG, "[onMeasure] Width : " + mJoynIconViewWidth
                        + " | Height : " + mJoynIconViewHeight);
            } else {
                Log.d(TAG, "[onMeasure] mJoynIconView not visible");
            }
        }

        @Override
        public void onLayout(boolean changed, int leftBound, int topBound,
                int rightBound, int bottomBound) {
            if (mContactPlugin == null || mIconLayout == null) {
                Log.d(TAG, "[onLayout] mContactPlugin is null");
                return;
            }
            if (mContactPlugin.isEnabled() == false) {
                return;
            }
            if (isVisible(mJoynIconView)) {
                int photoTop1 = topBound + (bottomBound - topBound - mJoynIconViewHeight) / 2;
                /*mJoynIconView.layout(rightBound - (mJoynIconViewWidth), photoTop1, rightBound,
                        photoTop1 + mJoynIconViewHeight);*/
                mIconLayout.layout(rightBound - mJoynIconViewWidth, photoTop1, rightBound,
                        photoTop1 + mJoynIconViewHeight);
            }
        }

        protected boolean isVisible(View view) {
            return view != null && view.getVisibility() == View.VISIBLE;
        }

        /**
         * Is show RCS icon.
         */
        private boolean isSetIcon() {
            boolean canSetRCSIcon = false;
            if (mContactPlugin != null && mContactId != INVALID_ID) {
                boolean isEnabled = mContactPlugin.isEnabled();
                Log.d(TAG, "isSetIcon(), isEnabled : " + isEnabled);
                if (isEnabled) {
                    canSetRCSIcon = mContactPlugin.getContactPresencebyId(mContactId);
                }
            }
            Log.d(TAG, "isSetIcon(), " + canSetRCSIcon);
            return canSetRCSIcon;
        }

        /**
         * Inflates the view from xml, set the image view and return layout to be added
         * @param contactId contactId of the contact in list
         */
        private void createCustomView(long contactId) {
            Log.d(TAG, "[createCustomView] contactId:" + contactId);
            mContactId = contactId;
            mIconLayout = null;
            mJoynIconView = null;
            if (mContactPlugin == null) {
                Log.d(TAG, "[createCustomView] mContactPlugin is null");
                return;
            }

            //canSetRCSIcon true means contact is rcs contact, need to add imageview to viewgroup
            if (isSetIcon()) {
                try {
                    Log.d(TAG, "[createCustomView] inflating icon view");
                    LayoutInflater mInflater;
                    mInflater = LayoutInflater.from(mContext);
                    mIconLayout = mInflater.inflate(R.layout.rcs_icon_plugin, null);
                    mIconLayout.setId(RCS_ICON_ID);
                    mJoynIconView = (ImageView) mIconLayout.findViewById(R.id.rcs_icon);
                    mJoynIconView.setVisibility(View.VISIBLE);
                    mJoynIconView.setImageDrawable(mContactPlugin.getAppIcon(contactId, false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void addCustomView(long contactId, ViewGroup viewGroup) {
            View view = viewGroup.findViewById(RCS_ICON_ID);
            if (view != null) {
                viewGroup.removeView(view);
            }
            createCustomView(contactId);
            if (mIconLayout != null) {
                viewGroup.addView(mIconLayout);
            }
        }

        @Override
        public int getWidthWithPadding() {
            return mContactPlugin.getAppIcon(false).getIntrinsicWidth();
        }
    };

    private QuickContactRcsScroller mQuickContactRcsScroller = new QuickContactRcsScroller() {
        private ImageView mJoynIconView = null;
        private TextView mLargeTextView = null;
        private long mContactId = INVALID_ID;
        private Uri mLookupUri = null;
        private boolean mIsRegistered = false;

        private final BroadcastReceiver mRcsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "[mRcsReceiver]");
                if (mContactPlugin == null) {
                    return;
                }
                if (mJoynIconView != null) {
                    mJoynIconView.setImageDrawable(mContactPlugin.getAppIcon(mContactId, true));
                    if (isSetIcon()) {
                        Log.d(TAG, "[onCapabilitiesChanged] show");
                        mJoynIconView.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "[onCapabilitiesChanged] hide");
                        mJoynIconView.setVisibility(View.GONE);
                    }
                }
            }
        };

        public PluginApiManager.CapabilitiesChangeListener rcsCapabilitiesChangeListener  =
                new PluginApiManager.CapabilitiesChangeListener() {
            /**
             * On capabilities changed.
             * @param contact the contact
             * @param contactInformation the contact information
             */
            @Override
            public void onCapabilitiesChanged(String contact,
                    ContactInformation contactInformation) {
                Log.d(TAG, "[onCapabilitiesChanged]");
                Intent intent = new Intent();
                intent.setAction("com.mediatek.rcs.contacts.REFRESH_RCS_VIEW");
                mContext.sendBroadcast(intent);
            }

            /**
             * Called when CapabilityApi connected status is changed.
             * @param isConnected True if CapabilityApi is connected.
             */
            @Override
            public void onApiConnectedStatusChanged(boolean isConnected) {
            }
        };

        /**
         * Inflates icon layout, add it to container relative to anchorView
         * @param container main view in which need to add icon layout
         * @param anchorView icon is added relative to this view
         * @param lookupUri for extracting mContactId
         * @return imageview added
         */
        @Override
        public View createRcsIconView(View container, View anchorView,
                Uri lookupUri) {
            Log.d(TAG, "[createRcsIconView] Uri:" + lookupUri
                    + " container = " + container + " anchorView = "
                    + anchorView);
            mJoynIconView = null;
            if (mContactPlugin == null) {
                Log.d(TAG, "[createRcsIconView] mContactPlugin is null");
                return null;
            }
            mLargeTextView = (TextView) anchorView;
            mLookupUri = lookupUri;

            //query contact presence
            mContactPlugin.onContactDetailOpen(lookupUri);
            if (!mIsRegistered) {
                mInstance.addCapabilitiesChangeListener(rcsCapabilitiesChangeListener);
                IntentFilter mIntentFilter =
                    new IntentFilter("com.mediatek.rcs.contacts.REFRESH_RCS_VIEW");
                mContext.registerReceiver(mRcsReceiver, mIntentFilter);
                mIsRegistered = true;
            }

            //logic to extract mContactId from contacts db from Uri
            mContactId = extractContactIdFromUri(lookupUri);

            View iconLayout = null;
            FrameLayout layout = (FrameLayout) container;
            try {
                LayoutInflater mInflater;
                mInflater = LayoutInflater.from(mContext);
                iconLayout = mInflater.inflate(R.layout.rcs_icon_plugin, null);
                mJoynIconView = (ImageView) iconLayout.findViewById(R.id.rcs_icon);
                mJoynIconView.setImageDrawable(mContactPlugin.getAppIcon(mContactId, true));
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                params.setMarginStart(anchorView.getRight());
                layout.addView(iconLayout, params);
                if (isSetIcon()) {
                    mJoynIconView.setVisibility(View.VISIBLE);
                } else {
                    mJoynIconView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mJoynIconView;
        }

        /**
         * extract contact id.
         * @param Uri uri of the contact
         */
        public long extractContactIdFromUri(Uri uri) {

            long contactId = INVALID_ID;
            if (mContext == null) {
                return contactId;
            } else if (mContext.getContentResolver() == null) {
                return contactId;
            }
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(uri, new String[] {
                    Contacts._ID
                }, null, null, null);
                if (null == cursor || !cursor.moveToFirst()) {
                    Log.e(TAG, "extractContactIdFromUri() error when loading cursor");
                    return contactId;
                }
                int indexContactId = cursor.getColumnIndex(Contacts._ID);
                do {
                    contactId = cursor.getLong(indexContactId);
                } while (cursor.moveToNext());
            } finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
            return contactId;
        }

        /**
         * Is show RCS icon.
         */
        private boolean isSetIcon() {
            boolean canSetRCSIcon = false;
            if (mContactPlugin != null && mContactId != INVALID_ID) {
                boolean isEnabled = mContactPlugin.isEnabled();
                Log.d(TAG, "isSetIcon(), isEnabled : " + isEnabled);
                if (isEnabled) {
                    canSetRCSIcon = mContactPlugin.getContactPresencebyId(mContactId);
                }
            }
            Log.d(TAG, "isSetIcon(), " + canSetRCSIcon);
            return canSetRCSIcon;
        }

        /**
         * update the icon view as host view is scrolled by user.
         */
        @Override
        public void updateRcsContact(final Uri lookupUri, boolean isLoadFinished) {
            Log.d(TAG, "[updateRcsContact] new lookupUri: " + lookupUri);
            if (mContactPlugin == null) {
                return;
            }

            //query contact presence
            if (lookupUri != null
                && (!isLoadFinished || lookupUri.compareTo(mLookupUri) != 0)) {
                mContactPlugin.onContactDetailOpen(lookupUri);
            }

            AsyncTask<Void, Void, Void> updateTask;
            updateTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    //logic to extract mContactId from contacts db from Uri
                    if (lookupUri != null
                        && (mLookupUri == null || lookupUri.compareTo(mLookupUri) != 0)) {
                        mLookupUri = lookupUri;
                        mContactId = extractContactIdFromUri(lookupUri);
                        Log.d(TAG, "[updateRcsContact] contactID:" + mContactId);
                    }
                    mContactPlugin.updateNumbersByContactId(mContactId);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Log.d(TAG, "[updateRcsContact] mJoynIconView: " + mJoynIconView);
                    if (mJoynIconView != null) {
                        mJoynIconView.setImageDrawable(
                            mContactPlugin.getAppIcon(mContactId, true));
                        if (isSetIcon()) {
                            Log.d(TAG, "[updateRcsContact] show");
                            mJoynIconView.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(TAG, "[updateRcsContact] hide");
                            mJoynIconView.setVisibility(View.GONE);
                        }
                    }

                }
            };
            updateTask.execute();
        }

        /**
         * update the icon view as host view is scrolled by user
         */
        @Override
        public void updateRcsIconView() {
            Log.d(TAG, "[updateJoynIconView] entry: " + mJoynIconView);
            if (mContactPlugin == null) {
                return;
            }

            if (mJoynIconView != null) {
                if (isSetIcon()) {
                    mJoynIconView.setVisibility(View.VISIBLE);
                    try {
                        int[] location = new int[2];
                        mJoynIconView.getLocationOnScreen(location);
                        int joynBottom = location[1] + mJoynIconView.getHeight();
                        Log.d(TAG, "[updateRcsIconView] joynBottom =" + joynBottom);
                        mLargeTextView.getLocationOnScreen(location);
                        int largeTextTop = location[1];
                        if (largeTextTop - joynBottom <= 0) {
                            Log.d(TAG,
                                    "[updateRcsIconView] hide");
                            mJoynIconView.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG,
                                    "[updateRcsIconView] show");
                            mJoynIconView.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    mJoynIconView.setVisibility(View.GONE);
                }
            }
        }
    };
}