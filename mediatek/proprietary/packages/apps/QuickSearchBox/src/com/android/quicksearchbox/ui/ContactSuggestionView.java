/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchableSource;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * View for contacts appearing in the suggestions list.
 */
public class ContactSuggestionView extends DefaultSuggestionView {

    private static final String VIEW_ID = "contact";

    private ContactBadge mQuickContact;

    private static final String CONTACT_LOOKUP_URI
        = ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString();

    public ContactSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ContactSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactSuggestionView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQuickContact = (ContactBadge) findViewById(R.id.icon1);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        super.bindAsSuggestion(suggestion, userQuery);
        mQuickContact.assignContactUri(Uri.parse(suggestion.getSuggestionIntentDataString()));
        mQuickContact.setExtraOnClickListener(new ContactBadgeClickListener());
    }

    private class ContactBadgeClickListener implements View.OnClickListener {
        public void onClick(View v) {
            onSuggestionQuickContactClicked();
        }
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super(VIEW_ID, ContactSuggestionView.class, R.layout.contact_suggestion, context);
        }

        @Override
        public boolean canCreateView(Suggestion suggestion) {
            return isSearchableContacts(suggestion);
        }

        protected boolean isSearchableContacts(Suggestion suggestion) {
            Source source = suggestion.getSuggestionSource();
            if (source instanceof SearchableSource) {
                SearchableSource searchableSource = (SearchableSource) source;
                boolean contactsSuggestion = TextUtils.equals(ContactsContract.AUTHORITY,
                                searchableSource.getSearchableInfo().getSuggestAuthority());
                if (contactsSuggestion) {
                    String intentData = suggestion.getSuggestionIntentDataString();
                    return intentData != null && intentData.startsWith(CONTACT_LOOKUP_URI);
                }
            }
            return false;
        }
    }
}
