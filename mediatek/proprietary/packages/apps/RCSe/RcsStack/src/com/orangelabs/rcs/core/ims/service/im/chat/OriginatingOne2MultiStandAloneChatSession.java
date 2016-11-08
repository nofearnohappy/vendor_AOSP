/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.orangelabs.rcs.core.CoreException;
import javax.sip.header.RequireHeader;
import javax.sip.header.SubjectHeader;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.logger.Logger;


import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManagerException;
import java.security.KeyStoreException;


/**
 * Originating ad-hoc group chat session
 *
 * @author jexa7410
 */
public class OriginatingOne2MultiStandAloneChatSession extends GroupChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param conferenceId Conference ID
     * @param subject Subject associated to the session
     * @param participants List of invited participants
     */
    public OriginatingOne2MultiStandAloneChatSession(
            ImsService parent, String conferenceId,
            String subject, ListOfParticipant participants) {
        super(parent, conferenceId, participants);

        // Set subject
        if ((subject != null) && (subject.length() > 0)) {
            setSubject(subject);
        }

        // Create dialog path
        createOriginatingDialogPath();

        // Set contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);

        if(RcsSettings.getInstance().isCPMSupported()){
            if (logger.isActivated()) {
                logger.info("CPMS OriginatingAdhocGroupChatSession ");
            }
            if(RcsSettings.getInstance().isCPMSupported()) {
                if (logger.isActivated()) {
                    logger.info("CPMS OriginatingOne2OneChatSession old call id: "
                            + getDialogPath().getCallId());
                }
                if(RichMessagingHistory.getInstance().getCoversationID(
                        this.getContributionID(),2).equals("")) {
                    // Set Call-Id
                    String callId = getImsService().getImsModule().getSipManager().getSipStack()
                            .generateCallId();
                    if (logger.isActivated()) {
                        logger.info("CPMS OriginatingOne2OneChatSession callId: " + callId);
                    }
                    // Set conversation ID
                    String ConversationId = ContributionIdGenerator.getContributionId(callId);
                    setConversationID(ConversationId);
                    RichMessagingHistory.getInstance().UpdateCoversationID(
                            this.getRemoteContact(),ConversationId,1);
                } else {
                    setConversationID(RichMessagingHistory.getInstance()
                            .getCoversationID(this.getRemoteContact(),1));
                }
            }
        }
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new ad-hoc group chat session as originating chatid: "
                        + getSessionID());
            }

            /**
             * M: Modified to resolve the 403 error issue @{
             */
            SipRequest invite = createSipInvite();
            /**
             * @}
             */

            // Send INVITE request
            sendInvite(invite);
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }
    /**
     * M: Modified to resolve the 403 error issue @{
     */
    /**
     * @return A sip invite request
     */
    protected SipRequest createSipInvite(String callId) {
        logger.debug("createSipInvite(), callId = " + callId);
        createOriginatingDialogPath(callId);
        return createSipInvite();
    }

    private SipRequest createSipInvite() {
        logger.debug("createSipInvite()");
        // Set setup mode
        String localSetup = createSetupOffer();
        if (logger.isActivated()){
            logger.debug("Local setup attribute is " + localSetup);
        }

        // Set local port
        int localMsrpPort;
        if ("active".equals(localSetup)) {
            localMsrpPort = 9; // See RFC4145, Page 4
        } else {
            localMsrpPort = getMsrpMgr().getLocalMsrpPort();
        }

        // Build SDP part
        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
        String sdp = null;
        if(isSecureProtocolMessage()){
            sdp =   "v=0" + SipUtils.CRLF +
                    "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                    "s=-" + SipUtils.CRLF +
                    "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                    "t=0 0" + SipUtils.CRLF +
                    "m=message " + localMsrpPort + " " + getMsrpMgr().getLocalSocketProtocol() + " *" + SipUtils.CRLF +
                    "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF +
                    "a=fingerprint:" + KeyStoreManager.getFingerPrint() + SipUtils.CRLF +
                    "a=setup:" + localSetup + SipUtils.CRLF +
                    "a=accept-types:" + getAcceptTypes() + SipUtils.CRLF +
                    "a=accept-wrapped-types:" + getWrappedTypes() + SipUtils.CRLF +
                    "a=sendrecv" + SipUtils.CRLF;
        }
        else{
            sdp =   "v=0" + SipUtils.CRLF +
                    "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                    "s=-" + SipUtils.CRLF +
                    "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                    "t=0 0" + SipUtils.CRLF +
                    "m=message " + localMsrpPort + " " + getMsrpMgr().getLocalSocketProtocol() + " *" + SipUtils.CRLF +
                    "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF +
                    "a=setup:" + localSetup + SipUtils.CRLF +
                    "a=accept-types:" + getAcceptTypes() + SipUtils.CRLF +
                    "a=accept-wrapped-types:" + getWrappedTypes() + SipUtils.CRLF +
                    "a=sendrecv" + SipUtils.CRLF;
        }

        String cpim = null;
        if(true) {
            String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
            String to = ChatUtils.ANOMYNOUS_URI;
            String formattedMsg = null;
            String mime = InstantMessage.MIME_TYPE;
            if(LauncherUtils.supportOP01()) {
                cpim = ChatUtils.buildCMCCCpimMessageWithImdn(
                        from,
                        to,
                        this.getImdnMessageId(),
                        StringUtils.encodeUTF8(formattedMsg),
                        mime);
            } /*else {
                cpim = ChatUtils.buildCpimMessageWithImdn(
                        from,
                        to,
                        getFirstMessage().getMessageId(),
                        StringUtils.encodeUTF8(formattedMsg),
                        mime);
            }*/
        }

        // Generate the resource list for given participants
        String resourceList = ChatUtils.generateMultiChatResourceList(getParticipants().getList());

        String multipart = null;
        // Build multipart
        if( LauncherUtils.supportOP01()) {
            if (logger.isActivated()) {
                logger.info("CMCC Invite");
            }
            multipart =
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
                    "Content-Type: application/sdp" + SipUtils.CRLF +
                    "Content-Length: " + sdp.getBytes().length + SipUtils.CRLF +
                    SipUtils.CRLF +
                    sdp + SipUtils.CRLF +
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
                    "Content-Type: application/resource-lists+xml" + SipUtils.CRLF +
                    "Content-Length: " + resourceList.getBytes().length + SipUtils.CRLF +
                    "Content-Disposition: recipient-list" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    resourceList + SipUtils.CRLF +
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
                    "Content-Type: " + CpimMessage.MIME_TYPE + SipUtils.CRLF +
                    "Content-Length: "+ cpim.getBytes().length + SipUtils.CRLF +
                    SipUtils.CRLF +
                    cpim + SipUtils.CRLF +
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        } else {
            multipart =
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
                    "Content-Type: application/sdp" + SipUtils.CRLF +
                    "Content-Length: " + sdp.getBytes().length + SipUtils.CRLF +
                    SipUtils.CRLF +
                    sdp + SipUtils.CRLF +
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
                    "Content-Type: application/resource-lists+xml" + SipUtils.CRLF +
                    "Content-Length: " + resourceList.getBytes().length + SipUtils.CRLF +
                    "Content-Disposition: recipient-list" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    resourceList + SipUtils.CRLF +
                    Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        }

        // Set the local SDP part in the dialog path
        getDialogPath().setLocalContent(multipart);

        // Create an INVITE request
        if (logger.isActivated()) {
            logger.info("Create INVITE");
        }
        try {
            SipRequest invite = createInviteRequest(multipart);

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);

            return invite;
        } catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        logger.error("Create sip invite failed, return null.");
        return null;
    }
    /**
     * @}
     */

    /**
     * Create INVITE request
     *
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createInviteRequest(String content) throws SipException {
        SipRequest invite = null;
        if(!RcsSettings.getInstance().isCPMSupported()){
            invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                    getFeatureTags(),
                    content,
                    BOUNDARY_TAG);
        }
        else{
            if (logger.isActivated()) {
                logger.info("CPMS OriginatingAdhocGroupChatSession createInviteRequest 0");
            }
            if(this.isCloudMessage()){
                invite = SipMessageFactory.createOneToMultiCloudCpmMultipartInvite(
                        getDialogPath(),
                        InstantMessagingService.CMCC_CLOUD_FEATURE_TAGS,
                        content,
                        BOUNDARY_TAG);
            } else {
                invite = SipMessageFactory.createOneToMultiCpmMultipartInvite(
                        getDialogPath(),
                        InstantMessagingService.CPM_LARGE_MESSAGE_FEATURE_TAGS,
                        content,
                        BOUNDARY_TAG);
            }
        }

        // Test if there is a subject
        if (getSubject() != null) {
            // Add a subject header
            invite.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(getSubject()));
        }

        // Add a require header
        invite.addHeader(RequireHeader.NAME, "recipient-list-invite");

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
        if(RcsSettings.getInstance().isCPMSupported()){
            if (logger.isActivated()) {
                logger.info("CPMS OriginatingOne2MultiChatSession createInviteRequest 1");
            }
            if(getConversationID() != null){
                invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
            }
        }
        return invite;
    }

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException
     */
    public SipRequest createInvite() throws SipException {
        return createInviteRequest(getDialogPath().getLocalContent());
    }

    /**
     * Data has been transfered
     *
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {

        String contact = this.getRemoteContact();
        if (logger.isActivated()) {
            logger.info("Data transfered contact: " + contact);
        }

        if (msgId != null) {
            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i))
                    .handleMessageDeliveryStatus(contact,msgId, ImdnDocument.DELIVERY_STATUS_SENT);
            }
        }

        // Close the media session
        closeMediaSession();

        // Terminate session
        terminateSession(ImsServiceSession.TERMINATION_BY_USER);

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((ChatSessionListener)getListeners().get(j)).handleSessionAborted(0);
        }
    }
}
