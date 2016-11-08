/*
 * Copyright 2007 ZXing authors
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

package com.mediatek.rcs.contacts.qrcode.result;

/**
 * VCard parser's result.
 */
public final class AddressBookParsedResult {

    private final String mFullName;
    private final String[] mPhoneNumbers;
    private final String[] mPhoneTypes;
    private final String[] mEmails;
    private final String[] mEmailTypes;
    private final String mOrg;
    private final String mTitle;

    /**
     * constructed function.
     * @param fullName String
     * @param phoneNumbers String[]
     * @param phoneTypes String[]
     * @param emails String[]
     * @param emailTypes String[]
     * @param org String
     * @param title String
     */
    public AddressBookParsedResult(String fullName,
            String[] phoneNumbers,
            String[] phoneTypes,
            String[] emails,
            String[] emailTypes,
            String org,
            String title) {
        this.mFullName = fullName;
        this.mPhoneNumbers = phoneNumbers;
        this.mPhoneTypes = phoneTypes;
        this.mEmails = emails;
        this.mEmailTypes = emailTypes;
        this.mOrg = org;
        this.mTitle = title;
    }

    public String getFullName() {
        return mFullName;
    }

    public String[] getPhoneNumbers() {
        return mPhoneNumbers;
    }

    public String[] getPhoneTypes() {
        return mPhoneTypes;
    }

    public String[] getEmails() {
        return mEmails;
    }

    public String[] getEmailTypes() {
        return mEmailTypes;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getOrg() {
        return mOrg;
    }

}
