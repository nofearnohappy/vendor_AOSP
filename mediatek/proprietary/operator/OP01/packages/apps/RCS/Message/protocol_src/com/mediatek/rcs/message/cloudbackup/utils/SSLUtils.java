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

package com.mediatek.rcs.message.cloudbackup.utils;

import android.net.SSLCertificateSocketFactory;

/**
 * Utils for SSL.
 *
 */
public class SSLUtils {
    // All secure factories are the same; all insecure factories are associated with HostAuth's
    private static SSLCertificateSocketFactory sSecureFactory;

    //private static final String TAG = "Email.Ssl";

    // A 30 second SSL handshake should be more than enough.
    private static final int SSL_HANDSHAKE_TIMEOUT = 30000;


    /**
     * @return a {@link javax.net.ssl.SSLSocketFactory}.
     */
    public synchronized static javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
            if (sSecureFactory == null) {
                sSecureFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory
                        .getDefault(SSL_HANDSHAKE_TIMEOUT, null);
            }
            return sSecureFactory;
    }

}
