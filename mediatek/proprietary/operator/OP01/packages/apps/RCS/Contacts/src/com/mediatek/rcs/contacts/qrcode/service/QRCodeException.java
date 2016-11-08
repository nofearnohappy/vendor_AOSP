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

package com.mediatek.rcs.contacts.qrcode.service;

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.ConnectException;

/**
 * XCAP Exception class.
 */
public class QRCodeException extends Exception {

    public static final int NO_EXCEPTION = 0;
    public static final int CONNECTION_POOL_TIMEOUT_EXCEPTION = 1;
    public static final int CONNECT_TIMEOUT_EXCEPTION = 2;
    public static final int NO_HTTP_RESPONSE_EXCEPTION = 3;
    public static final int INVALID_CREDENTIALS_EXCEPTION = 4;
    public static final int AUTHENTICATION_EXCEPTION = 5;
    public static final int HTTP_EXCEPTION = 6;
    public static final int HTTP_CONNNECT_EXCEPTION = 7;

    public int mHttpErrorCode = 0;
    public int mExceptionCode = NO_EXCEPTION;
    public boolean mIsConnectionError = false;

    /**
     * Constructs an instance with error code.
     *
     * @param httpErrorCode XCAP error code
     */
    public QRCodeException(int httpErrorCode) {
        mHttpErrorCode = httpErrorCode;
    }

    /**
     * Constructs an instance with IO exception.
     *
     * @param httpException I/O error exception
     */
    public QRCodeException(IOException httpException) {
        if (httpException instanceof ConnectionPoolTimeoutException) {
            mExceptionCode = CONNECTION_POOL_TIMEOUT_EXCEPTION;
        } else if (httpException instanceof ConnectTimeoutException) {
            mExceptionCode = CONNECT_TIMEOUT_EXCEPTION;
        } else if (httpException instanceof NoHttpResponseException) {
            mExceptionCode = NO_HTTP_RESPONSE_EXCEPTION;
        } else if (httpException instanceof HttpHostConnectException) {
            mExceptionCode = HTTP_CONNNECT_EXCEPTION;
        } else if (httpException instanceof ConnectException) {
            mExceptionCode = HTTP_CONNNECT_EXCEPTION;
        }
        mIsConnectionError = true;
    }

    public boolean isConnectionError() {
        return mIsConnectionError;
    }

    public int getHttpErrorCode() {
        return mHttpErrorCode;
    }

    public int getExceptionCode() {
        return mExceptionCode;
    }
}

