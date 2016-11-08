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

package com.android.quicksearchbox.preferences;

import android.content.Context;

import com.android.quicksearchbox.R;

/**
 * A CheckBoxPreference with an icon added.
 */
public class SearchEngineItemPreference extends RadioPreference {

//    private String mTitle;

    SearchEngineItemPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.radio_preference);
    }

    /*
    public void setTitle(String ttle) {
        mTitle = ttle;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(mTitle);
    }
    */
}
