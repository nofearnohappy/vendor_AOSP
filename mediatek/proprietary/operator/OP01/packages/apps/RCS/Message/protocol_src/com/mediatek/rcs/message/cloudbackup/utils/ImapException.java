/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
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


package com.mediatek.rcs.message.cloudbackup.utils;

/**
 * ImapException.
 *
 */
public class ImapException extends MessagingException {
    private static final long serialVersionUID = 1L;

    private final String mStatus;
    private final String mAlertText;
    private final String mResponseCode;

    /**
     * @param message .
     * @param status .
     * @param alertText .
     * @param responseCode .
     */
    public ImapException(String message, String status, String alertText,
                         String responseCode) {
        super(message);
        mStatus = status;
        mAlertText = alertText;
        mResponseCode = responseCode;
    }

    /**
     * @return status.
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * @return Alert text.
     */
    public String getAlertText() {
        return mAlertText;
    }

    /**
     * @return response code.
     */
    public String getResponseCode() {
        return mResponseCode;
    }
}