/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.rcs.message.cloudbackup.store;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.mediatek.rcs.message.cloudbackup.transport.MailTransport;
import com.mediatek.rcs.message.cloudbackup.utils.MessagingException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;


/**
 * <pre>
 *
 * ftp://ftp.isi.edu/in-notes/rfc2683.txt When a client asks for
 * certain information in a FETCH command, the server may return the requested
 * information in any order, not necessarily in the order that it was requested.
 * Further, the server may return the information in separate FETCH responses
 * and may also return information that was not explicitly requested (to reflect
 * to the client changes in the state of the subject message).
 * </pre>
 */
//public class ImapStore extends Store {
public class ImapStore  {
    private static final String LOG_TAG = "RcsBR/ImapStore";
    public static final Charset ASCII = Charset.forName("US-ASCII");

    //@VisibleForTesting
    static String sImapId = null;
    //@VisibleForTesting
    String mPathPrefix;
    //@VisibleForTesting
    String mPathSeparator;

    // RCS:IMAP porting
    protected Context mContext;
    protected String mUsername;
    protected String mPassword;
    protected MailTransport mTransport;
    private static String sUser;
    private static String sPassword;

    private final ConcurrentLinkedQueue<ImapConnection> mConnectionPool =
            new ConcurrentLinkedQueue<ImapConnection>();

    /**
     * Static named constructor.
     * @param context .
     * @return .
     * @throws MessagingException .
     */
    public static ImapStore newInstance(Context context)
            throws MessagingException {
        return new ImapStore(context);
    }

    /**
     * @param context .
     * @param user .
     * @param pwd .
     * @return .
     * @throws MessagingException .
     */
    public static ImapStore newInstance(Context context,
            String user, String pwd) throws MessagingException {
        sUser = user;
        sPassword = pwd;
        return new ImapStore(context);
    }

    /**
     * Creates a new store for the given account. Always use
     * {@link #newInstance(Account, Context)} to create an IMAP store.
     */
    private ImapStore(Context context) throws MessagingException {
        mContext = context;
        // RCS:IMAP porting
        mTransport = new MailTransport(context, "IMAP");
        mUsername = sUser.trim();
        mPassword = sPassword;
    }

    String getUsername() {
        return mUsername;
    }

    String getPassword() {
        Log.d(LOG_TAG, "getPassword()mPassword:" + mPassword);
        return mPassword;
    }

    //@VisibleForTesting
    Collection<ImapConnection> getConnectionPoolForTest() {
        return mConnectionPool;
    }

    /**
     * For testing only.  Injects a different root transport (it will be copied using
     * newInstanceWithConfiguration() each time IMAP sets up a new channel).  The transport
     * should already be set up and ready to use.  Do not use for real code.
     * @param testTransport The Transport to inject and use for all future communication.
     */
    //@VisibleForTesting
    void setTransportForTest(MailTransport testTransport) {
        mTransport = testTransport;
    }

    /**
     * Return, or create and return, an string suitable for use in an IMAP ID message.
     * This is constructed similarly to the way the browser sets up its user-agent strings.
     * See RFC 2971 for more details.  The output of this command will be a series of key-value
     * pairs delimited by spaces (there is no point in returning a structured result because
     * this will be sent as-is to the IMAP server).  No tokens, parenthesis or "ID" are included,
     * because some connections may append additional values.
     *
     * The following IMAP ID keys may be included:
     *   name                   Android package name of the program
     *   os                     "android"
     *   os-version             "version; model; build-id"
     *   vendor                 Vendor of the client/server
     *   x-android-device-model Model (only revealed if release build)
     *   x-android-net-operator Mobile network operator (if known)
     *   AGUID                  A device+account UID
     *
     * In addition, a vendor policy .apk can append key/value pairs.
     *
     * @param context .
     * @param userName the username of the account
     * @param host the host (server) of the account
     * @param capabilities a list of the capabilities from the server
     * @return a String for use in an IMAP ID message.
     */
    public static String getImapId(Context context, String userName,
            String host,            String capabilities) {
        // The first section is global to all IMAP connections, and generates the fixed
        // values in any IMAP ID message
        synchronized (ImapStore.class) {
            if (sImapId == null) {
                TelephonyManager tm =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String networkOperator = tm.getNetworkOperatorName();
                if (networkOperator == null) networkOperator = "";

                sImapId = makeCommonImapId(context.getPackageName(), Build.VERSION.RELEASE,
                        Build.VERSION.CODENAME, Build.MODEL, Build.ID, Build.MANUFACTURER,
                        networkOperator);
            }
        }

        // This section is per Store, and adds in a dynamic elements like UID's.
        // We don't cache the result of this work, because the caller does anyway.
        StringBuilder id = new StringBuilder(sImapId);

        // Optionally add any vendor-supplied id keys
        // RCS:IMAP porting
        // String vendorId = VendorPolicyLoader.getInstance(context).getImapIdValues(userName, host,
        //         capabilities);
        // if (vendorId != null) {
        //     id.append(' ');
        //     id.append(vendorId);
        // }

        // Generate a UID that mixes a "stable" device UID with the email address
        try {
            // RCS:IMAP porting
            //String devUID = Preferences.getPreferences(context).getDeviceUID();
            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(userName.getBytes());
            //messageDigest.update(devUID.getBytes());
            byte[] uid = messageDigest.digest();
            String hexUid = Base64.encodeToString(uid, Base64.NO_WRAP);
            id.append(" \"AGUID\" \"");
            id.append(hexUid);
            id.append('\"');
        } catch (NoSuchAlgorithmException e) {
            Log.d(LOG_TAG, "couldn't obtain SHA-1 hash for device UID");
        }
        return id.toString();
    }

    /**
     * Helper function that actually builds the static part of the IMAP ID string.  This is
     * separated from getImapId for testability.  There is no escaping or encoding in IMAP ID so
     * any rogue chars must be filtered here.
     *
     * @param packageName context.getPackageName()
     * @param version Build.VERSION.RELEASE
     * @param codeName Build.VERSION.CODENAME
     * @param model Build.MODEL
     * @param id Build.ID
     * @param vendor Build.MANUFACTURER
     * @param networkOperator TelephonyManager.getNetworkOperatorName()
     * @return the static (never changes) portion of the IMAP ID
     */
    //@VisibleForTesting
    static String makeCommonImapId(String packageName, String version,
            String codeName, String model, String id, String vendor, String networkOperator) {

        // Before building up IMAP ID string, pre-filter the input strings for "legal" chars
        // This is using a fairly arbitrary char set intended to pass through most reasonable
        // version, model, and vendor strings: a-z A-Z 0-9 - _ + = ; : . , / <space>
        // The most important thing is *not* to pass parens, quotes, or CRLF, which would break
        // the format of the IMAP ID list.
        Pattern p = Pattern.compile("[^a-zA-Z0-9-_\\+=;:\\.,/ ]");
        packageName = p.matcher(packageName).replaceAll("");
        version = p.matcher(version).replaceAll("");
        codeName = p.matcher(codeName).replaceAll("");
        model = p.matcher(model).replaceAll("");
        id = p.matcher(id).replaceAll("");
        vendor = p.matcher(vendor).replaceAll("");
        networkOperator = p.matcher(networkOperator).replaceAll("");

        // "name" "com.android.email"
        StringBuilder sb = new StringBuilder("\"name\" \"");
        sb.append(packageName);
        sb.append("\"");

        // "os" "android"
        sb.append(" \"os\" \"android\"");

        // "os-version" "version; build-id"
        sb.append(" \"os-version\" \"");
        if (version.length() > 0) {
            sb.append(version);
        } else {
            // default to "1.0"
            sb.append("1.0");
        }
        // add the build ID or build #
        if (id.length() > 0) {
            sb.append("; ");
            sb.append(id);
        }
        sb.append("\"");

        // "vendor" "the vendor"
        if (vendor.length() > 0) {
            sb.append(" \"vendor\" \"");
            sb.append(vendor);
            sb.append("\"");
        }

        // "x-android-device-model" the device model (on release builds only)
        if ("REL".equals(codeName)) {
            if (model.length() > 0) {
                sb.append(" \"x-android-device-model\" \"");
                sb.append(model);
                sb.append("\"");
            }
        }

        // "x-android-mobile-net-operator" "name of network operator"
        if (networkOperator.length() > 0) {
            sb.append(" \"x-android-mobile-net-operator\" \"");
            sb.append(networkOperator);
            sb.append("\"");
        }

        return sb.toString();
    }



    /**
     * @return .
     * @throws MessagingException .
     */
    public Bundle checkSettings() throws MessagingException {
        Bundle bundle = new Bundle();
        // TODO: why doesn't this use getConnection()? I guess this is only done during setup,
        // so there's need to look for a pooled connection?
        // But then why doesn't it use poolConnection() after it's done?
        ImapConnection connection = new ImapConnection(this);
        try {
            connection.open();
            connection.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            connection.destroyResponses();
        }
        //bundle.putInt(EmailServiceProxy.VALIDATE_BUNDLE_RESULT_CODE, result);
        return bundle;
    }

    /**
     * Returns whether or not the prefix has been set by the user. This can be determined by
     * the fact that the prefix is set, but, the path separator is not set.
     */
    boolean isUserPrefixSet() {
        return TextUtils.isEmpty(mPathSeparator) && !TextUtils.isEmpty(mPathPrefix);
    }

    /** Sets the path separator. */
    void setPathSeparator(String pathSeparator) {
        mPathSeparator = pathSeparator;
    }

    /** Sets the prefix. */
    void setPathPrefix(String pathPrefix) {
        mPathPrefix = pathPrefix;
    }

    /** Gets the context for this store. */
    Context getContext() {
        return mContext;
    }

    /**
     * Fixes the path prefix, if necessary. The path prefix must always end with the
     * path separator.
     */
    void ensurePrefixIsValid() {
        // Make sure the path prefix ends with the path separator
        if (!TextUtils.isEmpty(mPathPrefix) && !TextUtils.isEmpty(mPathSeparator)) {
            if (!mPathPrefix.endsWith(mPathSeparator)) {
                mPathPrefix = mPathPrefix + mPathSeparator;
            }
        }
    }

    /**
     * Gets a connection if one is available from the pool, or creates a new one if not.
     */
    ImapConnection getConnection() {
        ImapConnection connection = null;
        while ((connection = mConnectionPool.poll()) != null) {
            connection.close();
        }

        if (connection == null) {
            connection = new ImapConnection(this);
        }
        return connection;
    }

    /**
     * Save a {@link ImapConnection} in the pool for reuse. Any responses associated with the
     * connection are destroyed before adding the connection to the pool.
     */
    void poolConnection(ImapConnection connection) {
        if (connection != null) {
            connection.destroyResponses();
            mConnectionPool.add(connection);
        }
    }

    /**
     * .
     *
     */
    static class ImapException extends MessagingException {
        private static final long serialVersionUID = 1L;

        private final String mStatus;
        private final String mAlertText;
        private final String mResponseCode;

        public ImapException(String message, String status, String alertText,
                String responseCode) {
            super(message);
            mStatus = status;
            mAlertText = alertText;
            mResponseCode = responseCode;
        }

        public String getStatus() {
            return mStatus;
        }

        public String getAlertText() {
            return mAlertText;
        }

        public String getResponseCode() {
            return mResponseCode;
        }
    }

    /**
     * .
     */
    public void closeConnections() {
        ImapConnection connection;
        while ((connection = mConnectionPool.poll()) != null) {
            connection.close();
        }
    }

    /**
     * @param b .
     * @return .
     */
    public static String fromAscii(byte[] b) {
        return decode(ASCII, b);
    }

    private static String decode(Charset charset, byte[] b) {
        if (b == null) {
            return null;
        }
        final CharBuffer cb = charset.decode(ByteBuffer.wrap(b));
        return new String(cb.array(), 0, cb.length());
    }


    /**
     * Converts a String to ASCII bytes.
     * @param s .
     * @return .
     */
    public static byte[] toAscii(String s) {
        return encode(ASCII, s);
    }

    private static byte[] encode(Charset charset, String s) {
        if (s == null) {
            return null;
        }
        final ByteBuffer buffer = charset.encode(CharBuffer.wrap(s));
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * @return .
     */
    public MailTransport cloneTransport() {
        return mTransport.clone();
    }

}
