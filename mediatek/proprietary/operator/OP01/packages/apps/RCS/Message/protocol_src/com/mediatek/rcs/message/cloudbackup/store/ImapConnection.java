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

package com.mediatek.rcs.message.cloudbackup.store;

import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.message.cloudbackup.Config;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapConstants;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapResponse;
import com.mediatek.rcs.message.cloudbackup.store.imap.ImapResponseParser;
import com.mediatek.rcs.message.cloudbackup.transport.DiscourseLogger;
import com.mediatek.rcs.message.cloudbackup.transport.MailTransport;
import com.mediatek.rcs.message.cloudbackup.utils.AuthenticationFailedException;
import com.mediatek.rcs.message.cloudbackup.utils.CertificateValidationException;
import com.mediatek.rcs.message.cloudbackup.utils.ImapException;
import com.mediatek.rcs.message.cloudbackup.utils.MessagingException;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;

/**
 * A cacheable class that stores the details for a single IMAP connection.
 */
class ImapConnection {
    private static final boolean DEBUG = true;
    private static final String TAG = "RcsBR/ImapConnection";
    /** ID capability per RFC 2971. */
    public static final int CAPABILITY_ID        = 1 << 0;
    /** NAMESPACE capability per RFC 2342. */
    public static final int CAPABILITY_NAMESPACE = 1 << 1;
    /** STARTTLS capability per RFC 3501. */
    public static final int CAPABILITY_STARTTLS  = 1 << 2;
    /** UIDPLUS capability per RFC 4315. */
    public static final int CAPABILITY_UIDPLUS   = 1 << 3;
    /** M: Extended LIST capability. */
    public static final int CAPABILITY_XLIST     = 1 << 4;

    /** The capabilities supported; a set of CAPABILITY_* values. */
    private int mCapabilities;
    static final String IMAP_REDACTED_LOG = "[IMAP command redacted]";
    MailTransport mTransport;
    private ImapResponseParser mParser;
    private ImapStore mImapStore;
    private String mLoginPhrase;
    private String mLogoutPhrase;
    //private String mAccessToken;

    /** # of command/response lines to log upon crash. */
    private static final int DISCOURSE_LOGGER_SIZE = 64;
    private final DiscourseLogger mDiscourse = new DiscourseLogger(DISCOURSE_LOGGER_SIZE);
    /**
     * Next tag to use.  All connections associated to the same ImapStore instance share the same
     * counter to make tests simpler.
     * (Some of the tests involve multiple connections but only have a single counter to track the
     * tag.)
     */
    private final AtomicInteger mNextCommandTag = new AtomicInteger(0);

    // Keep others from instantiating directly
    ImapConnection(ImapStore store) {
        setStore(store);
    }

    void setStore(ImapStore store) {
        // TODO: maybe we should throw an exception if the connection is not closed here,
        // if it's not currently closed, then we won't reopen it, so if the credentials have
        // changed, the connection will not be reestablished.
        mImapStore = store;
        mLoginPhrase = null;
    }

    /**
     * Generates and returns the phrase to be used for authentication. This will be a LOGIN with
     * username and password, or an OAUTH authentication string, with username and access token.
     * Currently, these are the only two auth mechanisms supported.
     *
     * @throws IOException
     * @throws AuthenticationFailedException
     * @return the login command string to sent to the IMAP server
     */
    String getLoginPhrase() throws MessagingException, IOException {
        // build the LOGIN string once (instead of over-and-over again.)
        if (mLoginPhrase == null) {
            if (mImapStore.getUsername() != null
                    && mImapStore.getPassword() != null) {
                // build the LOGIN string once (instead of over-and-over again.)
                // apply the quoting here around the built-up password
                // mLoginPhrase = ImapConstants.LOGIN + " " +
                // mImapStore.getUsername() + " "
                // + ImapUtility.imapQuoted(mImapStore.getPassword());

                mLoginPhrase = ImapConstants.LOGIN + " "
                        + mImapStore.getUsername() + " "
                        + mImapStore.getPassword();

            }
        }
        return mLoginPhrase;
    }


    String getLogoutPhrase() throws MessagingException, IOException {
        if (mLogoutPhrase == null) {
            mLogoutPhrase = ImapConstants.LOGOUT;
        }

        return mLogoutPhrase;
    }

    void open() throws IOException, MessagingException {
        if (mTransport != null && mTransport.isOpen()) {
            return;
        }

        try {
            // copy configuration into a clean transport, if necessary
            if (mTransport == null) {
                mTransport = mImapStore.cloneTransport();
            }

            mTransport.open();
            createParser();
            if (Config.useGBA()) {
                // CAPABILITY
                ImapResponse capabilities = queryCapabilities();

                boolean hasStartTlsCapability = capabilities
                        .contains(ImapConstants.STARTTLS);

                // TLS
                if (hasStartTlsCapability) {
                    ImapResponse newCapabilities = doStartTls();
                    if (newCapabilities != null) {
                        capabilities = newCapabilities;
                    }
                }
            }

            // NOTE: An IMAP response MUST be processed before issuing any new IMAP
            // requests. Subsequent requests may destroy previous response data. As
            // such, we save away capability information here for future use.
            // setCapabilities(capabilities);
            // String capabilityString = capabilities.flatten();

            // ID
            // doSendId(isCapable(CAPABILITY_ID), capabilityString);

            doLogin();

            // NAMESPACE (only valid in the Authenticated state)
            //doGetNamespace(isCapable(CAPABILITY_NAMESPACE));

            // Gets the path separator from the server
            doGetPathSeparator();

            mImapStore.ensurePrefixIsValid();
        } catch (SSLException e) {
            if (DEBUG) {
                Log.d(TAG, e + "SSLException");
            }
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (IOException ioe) {
            // NOTE:  Unlike similar code in POP3, I'm going to rethrow as-is.  There is a lot
            // of other code here that catches IOException and I don't want to break it.
            // This catch is only here to enhance logging of connection-time issues.
            if (DEBUG) {
                Log.d(TAG, ioe + "IOException");
            }
            throw ioe;
        } finally {
            destroyResponses();
        }
    }

    /**
     * Closes the connection and releases all resources. This connection can not be used again
     * until {@link #setStore(ImapStore)} is called.
     */
    void close() {
        if (mTransport != null) {
            mTransport.close();
            mTransport = null;
        }
        destroyResponses();
        mParser = null;
        mImapStore = null;
    }

    /**
     * Returns whether or not the specified capability is supported by the server.
     * M: change modifier
     */
    public boolean isCapable(int capability) {
        return (mCapabilities & capability) != 0;
    }

    /**
     * Create an {@link ImapResponseParser} from {@code mTransport.getInputStream()} and
     * set it to {@link #mParser}.
     *
     * If we already have an {@link ImapResponseParser}, we
     * {@link #destroyResponses()} and throw it away.
     */
    private void createParser() {
        destroyResponses();
        mParser = new ImapResponseParser(mTransport.getInputStream(), mDiscourse);
    }

    void destroyResponses() {
        if (mParser != null) {
            mParser.destroyResponses();
        }
    }

    boolean isTransportOpenForTest() {
        return mTransport != null && mTransport.isOpen();
    }

    ImapResponse readResponse(ProgressListener l) throws IOException, MessagingException {
        mParser.setProgressListener(l);
        ImapResponse r = mParser.readResponse();
        mParser.removeProgressListener();
        return r;
    }

    ImapResponse readResponse() throws IOException, MessagingException {
        return mParser.readResponse();
    }

    /**
     * Send a single command to the server.  The command will be preceded by an IMAP command
     * tag and followed by \r\n (caller need not supply them).
     *
     * @param command The command to send to the server
     * @param sensitive If true, the command will not be logged
     * @return Returns the command tag that was sent
     */
    String sendCommand(String command, boolean sensitive)
            throws MessagingException, IOException {
        Log.d(TAG, "sendCommand: " + (sensitive ? IMAP_REDACTED_LOG : command));
        open();
        return sendCommandInternal(command, sensitive);
    }

    String sendCommandInternal(String command, boolean sensitive)
            throws MessagingException, IOException {
        if (mTransport == null) {
            throw new IOException("Null transport");
        }
        String tag = Integer.toString(mNextCommandTag.incrementAndGet());
        String commandToSend = tag + " " + command;
        mTransport.writeLine(commandToSend, sensitive ? IMAP_REDACTED_LOG : null);
        mDiscourse.addSentCommand(sensitive ? IMAP_REDACTED_LOG : commandToSend);
        return tag;
    }

    /**
     * Send a single, complex command to the server.  The command will be preceded by an IMAP
     * command tag and followed by \r\n (caller need not supply them).  After each piece of the
     * command, a response will be read which MUST be a continuation request.
     *
     * @param commands An array of Strings comprising the command to be sent to the server
     * @return Returns the command tag that was sent
     */
    String sendComplexCommand(List<String> commands, boolean sensitive) throws MessagingException,
            IOException {
        open();
        String tag = Integer.toString(mNextCommandTag.incrementAndGet());
        int len = commands.size();
        for (int i = 0; i < len; i++) {
            String commandToSend = commands.get(i);
            // The first part of the command gets the tag
            if (i == 0) {
                commandToSend = tag + " " + commandToSend;
            } else {
                // Otherwise, read the response from the previous part of the command
                ImapResponse response = readResponse();
                // If it isn't a continuation request, that's an error
                if (!response.isContinuationRequest()) {
                    throw new MessagingException("Expected continuation request");
                }
            }
            // Send the command
            mTransport.writeLine(commandToSend, null);
            //mTransport.writeLine(getCommandTag() + commandToSend, null);
            mDiscourse.addSentCommand(sensitive ? IMAP_REDACTED_LOG : commandToSend);
        }
        return tag;
    }

    List<ImapResponse> executeSimpleCommand(String command) throws IOException, MessagingException {
        return executeSimpleCommand(command, false);
    }

    /**
     * Read and return all of the responses from the most recent command sent to the server.
     *
     * @return a list of ImapResponses
     * @throws IOException
     * @throws MessagingException
     */
    List<ImapResponse> getCommandResponses() throws IOException, MessagingException {
        final List<ImapResponse> responses = new ArrayList<ImapResponse>();
        ImapResponse response;
        do {
            response = mParser.readResponse();
            responses.add(response);
        } while (!response.isTagged());

        if (!response.isOk()) {
            final String toString = response.toString();
            final String status = response.getStatusOrEmpty().getString();
            final String alert = response.getAlertTextOrEmpty().getString();
            final String responseCode = response.getResponseCodeOrEmpty().getString();
            destroyResponses();

            // if the response code indicates an error occurred within the server, indicate that
            if (ImapConstants.UNAVAILABLE.equals(responseCode)) {
                throw new MessagingException(MessagingException.SERVER_ERROR, alert);
            }

            throw new ImapException(toString, status, alert, responseCode);
        }

        Log.d(TAG, responses.toString());
        return responses;
    }

    /**
     * Execute a simple command at the server, a simple command being one that is sent in a single
     * line of text.
     *
     * @param command the command to send to the server
     * @param sensitive whether the command should be redacted in logs (used for login)
     * @return a list of ImapResponses
     * @throws IOException
     * @throws MessagingException
     */
     List<ImapResponse> executeSimpleCommand(String command, boolean sensitive)
            throws IOException, MessagingException {
         // TODO: It may be nice to catch IOExceptions and close the connection here.
         // Currently, we expect callers to do that, but if they fail to we'll be in a broken state.
        Log.d(TAG, "executeSimpleCommand: " + command);
         sendCommand(command, sensitive);
         return getCommandResponses();
    }

     /**
      * Execute a complex command at the server, a complex command being one that must be sent in
      * multiple lines due to the use of string literals.
      *
      * @param commands a list of strings that comprise the command to be sent to the server
      * @param sensitive whether the command should be redacted in logs (used for login)
      * @return a list of ImapResponses
      * @throws IOException
      * @throws MessagingException
      */
      List<ImapResponse> executeComplexCommand(List<String> commands, boolean sensitive)
            throws IOException, MessagingException {
          sendComplexCommand(commands, sensitive);
          return getCommandResponses();
      }

      /**
       * Query server for capabilities.
       */
      private ImapResponse queryCapabilities() throws IOException, MessagingException {
          ImapResponse capabilityResponse = null;
          for (ImapResponse r : executeSimpleCommand(ImapConstants.CAPABILITY)) {
              if (r.is(0, ImapConstants.CAPABILITY)) {
                  capabilityResponse = r;
                  break;
              }
          }
          if (capabilityResponse == null) {
              throw new MessagingException("Invalid CAPABILITY response received");
          }
          return capabilityResponse;
      }

    /**
     * Logs into the IMAP server.
     */
    public void doLogin() throws IOException, MessagingException, AuthenticationFailedException {
        try {
            Log.d(TAG, "doLogin():getLoginPhrase():" + getLoginPhrase());
            executeSimpleCommand(getLoginPhrase(), true);
        } catch (ImapException ie) {
            if (DEBUG) {
                Log.d(TAG, ie + "ImapException");
            }

            final String status = ie.getStatus();
            final String code = ie.getResponseCode();
            final String alertText = ie.getAlertText();

            // if the response code indicates expired or bad credentials, throw
            // a special exception
            if (ImapConstants.AUTHENTICATIONFAILED.equals(code)
                    || ImapConstants.EXPIRED.equals(code)
                    || (ImapConstants.NO.equals(status) && TextUtils
                            .isEmpty(code))) {
                throw new AuthenticationFailedException(alertText, ie);
            }

            throw new MessagingException(alertText, ie);
        }
    }


    public void doLogout() throws MessagingException {
        try {
            executeSimpleCommand(getLogoutPhrase(), true);
        } catch (ImapException ie) {
            if (DEBUG) {
                Log.d(TAG, ie + "ImapException");
            }

            final String status = ie.getStatus();
            final String code = ie.getResponseCode();
            final String alertText = ie.getAlertText();

            // if the response code indicates expired or bad credentials, throw a special exception
            if (ImapConstants.AUTHENTICATIONFAILED.equals(code)
                    || ImapConstants.EXPIRED.equals(code)
                    || (ImapConstants.NO.equals(status) && TextUtils
                            .isEmpty(code))) {
                throw new AuthenticationFailedException(alertText, ie);
            }

            throw new MessagingException(alertText, ie);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a TLS session with the IMAP server per RFC 3501. If the user has
     * not opted to use TLS or the server does not support the TLS capability,
     * this will perform no operation.
     */
    private ImapResponse doStartTls() throws IOException, MessagingException {
        // STARTTLS
        executeSimpleCommand(ImapConstants.STARTTLS);

        mTransport.reopenTls();
        createParser();
        // Per RFC requirement (3501-6.2.1) gather new capabilities
        return (queryCapabilities());
    }

    /**
     * Gets the path separator per the LIST command in RFC 3501. If the path
     * separator was obtained while obtaining the namespace or there is no
     * prefix defined, this will perform no operation.
     */
    private void doGetPathSeparator() throws MessagingException {
        // user did not specify a hard-coded prefix; try to get it from the server
        if (mImapStore.isUserPrefixSet()) {
            List<ImapResponse> responseList = Collections.emptyList();

            try {
                responseList = executeSimpleCommand(ImapConstants.LIST + " \"\" \"\"");
            } catch (ImapException ie) {
                // Log for debugging, but this is not a fatal problem.
                if (DEBUG) {
                    Log.d(TAG, ie + "ImapException");
                }
            } catch (IOException ioe) {
                // Special case to handle malformed OK responses and ignore them.
            }

            for (ImapResponse response: responseList) {
                if (response.isDataResponse(0, ImapConstants.LIST)) {
                    mImapStore.setPathSeparator(response.getStringOrEmpty(2).getString());
                }
            }
        }
    }


    /** @see DiscourseLogger#logLastDiscourse() */
    void logLastDiscourse() {
        mDiscourse.logLastDiscourse();
    }

}