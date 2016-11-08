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

import java.util.Arrays;
import java.util.List;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManagerException;
import java.security.KeyStoreException;

import org.gsma.joyn.chat.GroupChat;


/**
 * Rejoin a group chat session
 * 
 * @author Jean-Marc AUFFRET
 */
public class RejoinGroupChatSession extends GroupChatSession {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
/**
     * M: add for auto-rejoin group chat @{
     */
    /**
     * Max count to retry.
     */
    private static final int MAX_RETRY_COUNT = 10;
    /**
     * Current retry count.
     */
    private int mCurrentRetryCount = 0;

    /** @} */
    /**
     * Constructor
     *
     * @param parent IMS service
     * @param rejoinId Rejoin ID
     * @param chatId Chat ID or contribution ID
     * @param subject Subject
     * @param participants List of participants
     */
    public RejoinGroupChatSession(ImsService parent, String rejoinId, String chatId, String subject, List<String> participants) {
        super(parent, rejoinId, new ListOfParticipant(participants));

        // Set subject
        if ((subject != null) && (subject.length() > 0)) {
            setSubject(subject);
        }

        // Create dialog path
        createOriginatingDialogPath();

        // Set contribution ID
        setContributionID(chatId);

        if (RcsSettings.getInstance().isCPMSupported()) {
            if (logger.isActivated()) {
                logger.info("CPMS RejoinGroupChatSession old call id: "
                        + getDialogPath().getCallId());
            }
            RichMessagingHistory rmHistory = RichMessagingHistory.getInstance();
            String conversationId = rmHistory.getCoversationID(chatId, 2);
            if (conversationId.isEmpty()) {
                String callId = getImsService().getImsModule().getSipManager().getSipStack()
                        .generateCallId();
                if (logger.isActivated()) {
                    logger.info("CPMS RejoinGroupChatSession call id: " + callId);
                }
                // Set conversation ID
                conversationId = ContributionIdGenerator.getContributionId(callId);
                setConversationID(conversationId);
                rmHistory.UpdateCoversationID(chatId, conversationId, 1);
            } else {
                setConversationID(conversationId);
            }
        }
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Rejoin an existing group chat session");
            }

            /**
             * Modified to resolve the 403 error issue.@{
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
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		
	}

    /**
 * Modified to resolve the 403 error issue.@{
     */
    /**
     * @return A sip invite request
     */
    protected SipRequest createSipInvite(String callId) {
        SipRequest invite = null;
        mCurrentRetryCount++;
        if (logger.isActivated()) {
            logger.debug("createSipInvite(), callId: " + callId + " mCurrentRetryCount: "
                    + mCurrentRetryCount);
        }
        if (mCurrentRetryCount <= MAX_RETRY_COUNT) {
            try {
                Thread.sleep(SipManager.TIMEOUT * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (logger.isActivated()) {
                    logger.debug("createSipInvite() InterruptedException");
                }
            }
            createOriginatingDialogPath(callId);
            invite = createSipInvite();
        }
        return invite;
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
				sdp =
	    		"v=0" + SipUtils.CRLF +
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
				sdp =
	    		"v=0" + SipUtils.CRLF +
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

			// Set the local SDP part in the dialog path
	    	getDialogPath().setLocalContent(sdp);

	        // Create an INVITE request
	        if (logger.isActivated()) {
	        	logger.info("Send INVITE");
	        }
                SipRequest invite = null;
                try{
	         invite = createInviteRequest(sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

	        // Set initial request in the dialog path
	        getDialogPath().setInvite(invite);
	        
		} catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
		}		
        return invite;
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
           invite = SipMessageFactory.createInvite(getDialogPath(),
                getFeatureTags(),
                content);
		}
		else{
			if (logger.isActivated()) {
				logger.info("RejoinGroupChatSession createInviteRequest0 CPMS");
			} 
			 invite = SipMessageFactory.createCpmInvite(getDialogPath(),
		                getCpimFeatureTags(),
		                content);
		}

		if(LauncherUtils.supportOP01()) {
			List<String> list = Arrays.asList(getCmccCpimFeatureTags());  
			// Update Contact header
	    	StringBuffer acceptTags = new StringBuffer("*");
	    	for(int i=0; i < list.size(); i++) {
	    		acceptTags.append(";" + list.get(i));
	    	}  
	    	invite.addHeader(SipUtils.HEADER_ACCEPT_CONTACT, acceptTags.toString());
		}

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
				logger.info("RejoinGroupChatSession createInviteRequest 1 CPMS");
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
     * Handle 404 Session Not Found
     *
     * @param resp 404 response
     */
    public void handle404SessionNotFound(SipResponse resp) {
		// Rejoin session has failed, we update the database with status terminated by remote
        RichMessagingHistory.getInstance().updateGroupChatStatus(getContributionID(), GroupChat.State.TERMINATED);

		// Notify listener
        handleError(new ChatError(ChatError.SESSION_NOT_FOUND, resp.getReasonPhrase()));
    }
}
