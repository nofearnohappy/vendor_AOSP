/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox;

import com.google.common.annotations.VisibleForTesting;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Some utilities for suggestions.
 */
public class SuggestionUtils {

    private static final String APPLICAITON_AUTHORITY = "applications";

    private SuggestionUtils() {
    }

    /**
     * M: Get the app Intent by suggestion, or create a new Intent.
     * @param suggestion    SueggestionCursor to get Intent if intent exists
     * @param appSearchData the data to create intent if intent does not exist
     * @return              if intent exists return it, else create a new one to return
     */
    public static Intent getSuggestionIntent(SuggestionCursor suggestion, Bundle appSearchData) {
        Intent intent = getAppIntent(suggestion);
        if (intent != null) {
            return intent;
        }
        return createIntent(suggestion, appSearchData);
    }

    private static Intent getAppIntent(SuggestionCursor suggestion) {
        Source source = suggestion.getSuggestionSource();
        String authority = source.getSuggestAuthority();
        if (authority == null) {
            return null;
        } else {
            if (APPLICAITON_AUTHORITY.equals(authority)) {
                String dataString = suggestion.getSuggestionIntentDataString();
                if (TextUtils.isEmpty(dataString)) {
                    return null;
                }
                ComponentName componentName
                        = Applications.uriToComponentName(Uri.parse(dataString));
                if (componentName == null) {
                    return null;
                }
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(componentName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                return intent;
            } else {
                return null;
            }
        }
    }

    private static Intent createIntent(SuggestionCursor suggestion, Bundle appSearchData) {
        String action = suggestion.getSuggestionIntentAction();

        String data = suggestion.getSuggestionIntentDataString();
        String query = suggestion.getSuggestionQuery();
        String userQuery = suggestion.getUserQuery();
        String extraData = suggestion.getSuggestionIntentExtraData();

        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data != null) {
            intent.setData(Uri.parse(data));
        }
        intent.putExtra(SearchManager.USER_QUERY, userQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (appSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
        }
        if (suggestion.isWebSearchSuggestion()) {
            intent.putExtra("INTERNAL", true);
        }

        intent.setComponent(suggestion.getSuggestionIntentComponent());
        return intent;
    }

    /**
     * Gets a unique key that identifies a suggestion. This is used to avoid
     * duplicate suggestions.
     */
    public static String getSuggestionKey(Suggestion suggestion) {
        String action = makeKeyComponent(suggestion.getSuggestionIntentAction());
        String data = makeKeyComponent(normalizeUrl(suggestion.getSuggestionIntentDataString()));
        String query = makeKeyComponent(normalizeUrl(suggestion.getSuggestionQuery()));
        // calculating accurate size of string builder avoids an allocation vs starting with
        // the default size and having to expand.
        int size = action.length() + 2 + data.length() + query.length();
        return new StringBuilder(size)
                .append(action)
                .append('#')
                .append(data)
                .append('#')
                .append(query)
                .toString();
    }

    private static String makeKeyComponent(String str) {
        return str == null ? "" : str;
    }

    private static final String SCHEME_SEPARATOR = "://";
    private static final String DEFAULT_SCHEME = "http";

    /**
     * Simple url normalization that adds http:// if no scheme exists, and
     * strips empty paths, e.g.,
     * www.google.com/ -> http://www.google.com.  Used to prevent obvious
     * duplication of nav suggestions, bookmarks and urls entered by the user.
     */
    @VisibleForTesting
    static String normalizeUrl(String url) {
        String normalized;
        if (url != null) {
            int start;
            int schemePos = url.indexOf(SCHEME_SEPARATOR);
            if (schemePos == -1) {
                // no scheme - add the default
                normalized = DEFAULT_SCHEME + SCHEME_SEPARATOR + url;
                start = DEFAULT_SCHEME.length() + SCHEME_SEPARATOR.length();
            } else {
                normalized = url;
                start = schemePos + SCHEME_SEPARATOR.length();
            }
            int end = normalized.length();
            if (normalized.indexOf('/', start) == end - 1) {
                end--;
            }
            return normalized.substring(0, end);
        }
        return url;
    }

}
