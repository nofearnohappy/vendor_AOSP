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

import java.util.List;


import javax2.sip.header.ExtensionHeader;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceEventSubscribeManager;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import javax.sip.header.SubjectHeader;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.ChatLog;

/**
 * Abstract Group chat session
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class GroupChatSession extends ChatSession {
	/**
	 * Conference event subscribe manager
	 */
	private ConferenceEventSubscribeManager conferenceSubscriber = new ConferenceEventSubscribeManager(this); 
		
	private boolean isMsrpFtsupport = false;
		
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
	 * Constructor for originating side
	 * 
	 * @param parent IMS service
	 * @param conferenceId Conference id
	 * @param participants List of invited participants
	 */
	public GroupChatSession(ImsService parent, String conferenceId, ListOfParticipant participants) {
		super(parent, conferenceId, participants);
		
		if(!RcsSettings.getInstance().isCPMSupported()){
		// Set feature tags
        setFeatureTags(ChatUtils.getSupportedFeatureTagsForGroupChat());
		}
		else{
			if (logger.isActivated()) {
	    		logger.info("CPMS GroupChatSession ");
	    	}
			// Set feature tags
			setCpimFeatureTags(ChatUtils.getCpimSupportedFeatureTagsForGroupChat(false));
			setCmccCpimFeatureTags(ChatUtils.getCpimSupportedFeatureTagsForGroupChat(true));
		}
		
		// Set accept-types
		String acceptTypes = CpimMessage.MIME_TYPE;	
        setAcceptTypes(acceptTypes);
				
		// Set accept-wrapped-types
		String wrappedTypes = InstantMessage.MIME_TYPE + " " + IsComposingInfo.MIME_TYPE + " " + ImdnDocument.MIME_TYPE;
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
        	wrappedTypes += " " + GeolocInfoDocument.MIME_TYPE;
        }
        if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
        	wrappedTypes += " " + FileTransferHttpInfoDocument.MIME_TYPE;
        }		
        if(LauncherUtils.supportOP01()) {
        	wrappedTypes += " " + InstantMessage.CLOUD_MIME_TYPE;
        }
        setWrappedTypes(wrappedTypes);
	}

	/**
	 * Is group chat
	 * 
	 * @return Boolean
	 */
	public void setMsrpFtSupport(boolean ftSupport) {
		this.isMsrpFtsupport = ftSupport;
	}

	/**
	 * Is group chat
	 * 
	 * @return Boolean
	 */
	public boolean getMsrpFtSupport() {
		return isMsrpFtsupport;
	}

	/**
	 * Is group chat
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChat() {
		return true;
	}
	
	/**
	 * Returns the list of participants currently connected to the session
	 * 
	 * @return List of participants
	 */
    public ListOfParticipant getConnectedParticipants() {
		return conferenceSubscriber.getParticipants();
	}
    
    public void removeParticipantFromSession(String participant) {
        conferenceSubscriber.getParticipants().removeParticipant(participant);
    }
    
    /**
	 * Get replaced session ID
	 * 
	 * @return Session ID
	 */
	public String getReplacedSessionId() {
		String result = null;
		ExtensionHeader sessionReplace = (ExtensionHeader)getDialogPath().getInvite().getHeader(SipUtils.HEADER_SESSION_REPLACES);
		if (sessionReplace != null) {
			result = sessionReplace.getValue();
		} else {
			String content = getDialogPath().getRemoteContent();
			if (content != null) {
				int index1 = content.indexOf("Session-Replaces=");
				if (index1 != -1) {
					int index2 = content.indexOf("\"", index1);
					result = content.substring(index1+17, index2);
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns the conference event subscriber
	 * 
	 * @return Subscribe manager
	 */
	public ConferenceEventSubscribeManager getConferenceEventSubscriber() {
		return conferenceSubscriber;
	}	

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        closeMsrpSession();
    }

    /**
	 * Terminate session
	 *  
	 * @param reason Reason
	 */
	public void terminateSession(int reason) {
		// Stop conference subscription
		 conferenceSubscriber.terminate();	
		
		// Terminate session
		super.terminateSession(reason);
	}	
	
    /**
     * Receive BYE request 
     * 
     * @param bye BYE request
     */
    public void receiveBye(SipRequest bye) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
       
        // Receive BYE request
        super.receiveBye(bye);
    }
    
    /**
     * Receive CANCEL request 
     * 
     * @param cancel CANCEL request
     */
    public void receiveCancel(SipRequest cancel) {
        // Stop conference subscription
        conferenceSubscriber.terminate();
       
        // Receive CANCEL request
        super.receiveCancel(cancel);
	}	

	/**
	 * Send message delivery status via MSRP
	 * 
	 * @param contact Contact that requested the delivery status
	 * @param msgId Message ID
	 * @param status Status
	 */
	public void sendMsrpMessageDeliveryStatus(String contact, String msgId, String status) {      
        // Send status in CPIM + IMDN headers
        String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
        String to = contact;
        //String to = this.getRemoteContact();
        String imdn = ChatUtils.buildDeliveryReport(msgId, status);
        String content = ChatUtils.buildCpimDeliveryReport(from, to, imdn);
        
        if (logger.isActivated()) {
			logger.info("sendMsrpMessageDeliveryStatus contact " + contact);
		}
        
        // Send data
        boolean result = sendDataChunks(ChatUtils.generateMessageId(), content, CpimMessage.MIME_TYPE);
        if (result) {
			if (logger.isActivated()) {
				logger.info("New delivery status for message " + msgId + ", status " + status + " contact " + contact);
			}
        	contact = PhoneUtils.formatNumberToInternational(contact);
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        }
	}
	
	/**
	 * Send a text message
	 * 
	 * @param msgId Message-ID
	 * @param txt Text message
	 */ 
	public void sendTextMessage(String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated(); 
        String from = "<" + ImsModule.IMS_USER_PROFILE.getPublicAddress() + ">";
		String to = ChatUtils.ANOMYNOUS_URI;
                if (logger.isActivated()) {
				logger.info("group sendTextMessage text: " + txt + "from : " + from + "useImdn: " + useImdn);
			}
		
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
			content = ChatUtils.buildCpimMessageWithDeliveredImdnAlias(from, to, msgId, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessageAlias(from, to, StringUtils.encodeUTF8(txt), InstantMessage.MIME_TYPE);
		}		
		
		if(!LauncherUtils.supportOP01()) {
		// Update rich messaging history
			if (!RichMessagingHistory.getInstance().isOne2OneMessageExists(msgId)) {
				if (logger.isActivated()) {
			    	logger.info("CPMS Group sendTextMessage Add in DB Msgid:" + msgId );
			    }
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn,null);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), msg,
				ChatLog.Message.Direction.OUTGOING);
		}
		}

		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(this.getRemoteContact(),msgId, ImdnDocument.DELIVERY_STATUS_FAILED,ImdnDocument.UNKNOWN,null);
			}
		}
	}
	
	/**
	 * Send a emoticons message
	 * 
	 * @param msgId Message-ID
	 * @param txt emoticons message
	 */ 
	public void sendEmoticonsMessage(String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated(); 
        String from = "<" + ImsModule.IMS_USER_PROFILE.getPublicAddress() + ">";
		String to = ChatUtils.ANOMYNOUS_URI;
                if (logger.isActivated()) {
				logger.info("group sendEmoticonsMessage text: " + txt + "from : " + from + "useImdn: " + useImdn);
			}
		
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
			content = ChatUtils.buildCpimMessageWithDeliveredImdnAlias(from, to, msgId, StringUtils.encodeUTF8(txt), InstantMessage.EMOTICONS_MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessageAlias(from, to, StringUtils.encodeUTF8(txt), InstantMessage.EMOTICONS_MIME_TYPE);
		}		
		
		if(!LauncherUtils.supportOP01()) {
		// Update rich messaging history
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn,null);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), msg,
				ChatLog.Message.Direction.OUTGOING);
		}

		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(this.getRemoteContact(),msgId, ImdnDocument.DELIVERY_STATUS_FAILED,ImdnDocument.UNKNOWN,null);
			}
		}
	}
	
	/**
	 * Send a emoticons message
	 * 
	 * @param msgId Message-ID
	 * @param txt emoticons message
	 */ 
	public void sendCloudMessage(String msgId, String txt) {
		boolean useImdn = getImdnManager().isImdnActivated(); 
        String from = "<" + ImsModule.IMS_USER_PROFILE.getPublicAddress() + ">";
		String to = ChatUtils.ANOMYNOUS_URI;
        if (logger.isActivated()) {
			logger.info("group sendCloudMessage text: " + txt + "from : " + from + "useImdn: " + useImdn);
		}
		
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
			content = ChatUtils.buildCpimMessageWithDeliveredImdnAlias(from, to, msgId, StringUtils.encodeUTF8(txt), InstantMessage.CLOUD_MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessageAlias(from, to, StringUtils.encodeUTF8(txt), InstantMessage.CLOUD_MIME_TYPE);
		}		
		
		if(!LauncherUtils.supportOP01()) {
		// Update rich messaging history
		InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), txt, useImdn,null);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), msg,
				ChatLog.Message.Direction.OUTGOING);
		}

		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(this.getRemoteContact(),msgId, ImdnDocument.DELIVERY_STATUS_FAILED,ImdnDocument.UNKNOWN,null);
			}
		}
	}
	
    
    /**
     * Set the new chairman(chairman privilege).
     * 
     * @param newChairman new chairman of the group, should be a group member 
     * @throws JoynServiceException
     */
    public void transferChairman(String newChairman) {
        try {
            if (logger.isActivated()) {
                logger.debug("transferChairman (" + newChairman + ") to the session");
            }
            
            
            // Re-use INVITE dialog path
            SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
            
            // Increment the Cseq number of the dialog path   
            getDialogPath().incrementCseq();   

            // Send REFER request
            if (logger.isActivated()) {
                logger.debug("Send REFER");
            }
            String chairmanUri = PhoneUtils.formatNumberToSipUri(this.getChairman());
            String newChairmanUri = PhoneUtils.formatNumberToSipUri(newChairman);
            SipRequest refer = null;
            
            refer = SipMessageFactory.createReferTransferChairman(getDialogPath(), chairmanUri, newChairmanUri, getContributionID());

             /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
           /* // Test if there is a subject
            if (newSubject != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(newSubject));
            }*/

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            if(getConversationID() != null){
                refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
            }
            if(RcsSettings.getInstance().isCPMSupported()){
                 if (logger.isActivated()) {
                    logger.info("CPMS GroupChatSession transferChairman");
                }
                if(getConversationID() != null){
                    refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                }
                if(getInReplyID() != null){
                    refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
                }
            }

            /**@}*/
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
    
            // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
                if (logger.isActivated()) {
                    logger.debug("407 response received");
                }

                // Set the Proxy-Authorization header
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                    logger.info("Send second REFER");
                }
                if(!RcsSettings.getInstance().isCPMSupported()) {
                    logger.info("Send second REFER");
                    refer = SipMessageFactory.createReferTransferChairman(getDialogPath(), chairmanUri, newChairmanUri, getContributionID());
                }

                if(RcsSettings.getInstance().isCPMSupported()){
                     if (logger.isActivated()) {
                        logger.info("CPMS GroupChatSession addParticipant 3");
                    }
                    if(getConversationID() != null){
                        refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                    }
                    if(getInReplyID() != null){
                        refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());                   
                    }
                }
                
                // Set the Authorization header
                authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
                ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                    if (logger.isActivated()) {
                        logger.debug("200 OK response received");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleTransferChairmanSuccessful(newChairman);
                    }
                } else {
                    // Error
                    if (logger.isActivated()) {
                        logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleTransferChairmanFailed(ctx.getStatusCode());
                    }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                // 200 OK received
                if (logger.isActivated()) {
                    logger.debug("200 OK response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleTransferChairmanSuccessful(newChairman);
                }
            } else {
                // Error responses
                if (logger.isActivated()) {
                    logger.debug("No response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleTransferChairmanFailed(ctx.getStatusCode());
                }
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("REFER request has failed", e);
            }
            
            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleTransferChairmanFailed(500);
            }
        }
    }
    
    /**
     * remove participant(chairman privilege).
     * 
     * @param participants list of participants to be removed 
     * @throws JoynServiceException
     */
    public void removeParticipants(String participant) {
        try {
            if (logger.isActivated()) {
                logger.debug("removeParticipants (" + participant + ") from the session");
            }
            
            
            // Re-use INVITE dialog path
            SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
            
            // Increment the Cseq number of the dialog path   
            getDialogPath().incrementCseq();   

            // Send REFER request
            if (logger.isActivated()) {
                logger.debug("Send REFER");
            }
            String chairmanUri = PhoneUtils.formatNumberToSipUri(this.getChairman());
            String participantUri = PhoneUtils.formatNumberToSipUri(participant);
            SipRequest refer = null;
            
            refer = SipMessageFactory.createReferRemoveParticipant(getDialogPath(), chairmanUri, participantUri, getContributionID());

             /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
           /* // Test if there is a subject
            if (newSubject != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(newSubject));
            }*/

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            if(RcsSettings.getInstance().isCPMSupported()){
                 if (logger.isActivated()) {
                    logger.info("CPMS GroupChatSession transferChairman");
                }
                if(getConversationID() != null){
                    refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                }
                if(getInReplyID() != null){
                    refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
                }
            }

            /**@}*/
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
    
            // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
                if (logger.isActivated()) {
                    logger.debug("407 response received");
                }

                // Set the Proxy-Authorization header
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                    logger.info("Send second REFER");
                }
                if(!RcsSettings.getInstance().isCPMSupported()) {
                    logger.info("Send second REFER");
                    refer = SipMessageFactory.createReferRemoveParticipant(getDialogPath(), chairmanUri, participantUri, getContributionID());
                }

                if(RcsSettings.getInstance().isCPMSupported()){
                     if (logger.isActivated()) {
                        logger.info("CPMS GroupChatSession addParticipant 3");
                    }
                    if(getConversationID() != null){
                        refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                    }
                    if(getInReplyID() != null){
                        refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());                   
                    }
                }
                
                // Set the Authorization header
                authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
                ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                    if (logger.isActivated()) {
                        logger.debug("200 OK response received");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleRemoveParticipantSuccessful(participant);
                    }
                } else {
                    // Error
                    if (logger.isActivated()) {
                        logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleRemoveParticipantFailed(ctx.getStatusCode());
                    }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                // 200 OK received
                if (logger.isActivated()) {
                    logger.debug("200 OK response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleRemoveParticipantSuccessful(participant);
                }
            } else {
                // Error responses
                if (logger.isActivated()) {
                    logger.debug("No response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleRemoveParticipantFailed(ctx.getStatusCode());
                }
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("REFER request has failed", e);
            }
            
            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleRemoveParticipantFailed(500);
            }
        }
    }
    
    /**
	 * Send a geoloc message
	 * 
	 * @param msgId Message ID
	 * @param geoloc Geoloc info
	 */ 
	public void sendGeolocMessage(String msgId, GeolocPush geoloc) {
		boolean useImdn = getImdnManager().isImdnActivated();
		String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
		String to = ChatUtils.ANOMYNOUS_URI;
		String geoDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
		
		String content;
		if (useImdn) {
			// Send message in CPIM + IMDN delivered
			content = ChatUtils.buildCpimMessageWithDeliveredImdn(from, to, msgId, geoDoc, GeolocInfoDocument.MIME_TYPE);
		} else {
			// Send message in CPIM
			content = ChatUtils.buildCpimMessage(from, to, geoDoc, GeolocInfoDocument.MIME_TYPE);
		}
		
		// Send data
		boolean result = sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);

		// Update rich messaging history
		GeolocMessage geolocMsg = new GeolocMessage(msgId, getRemoteContact(), geoloc, useImdn);
		RichMessagingHistory.getInstance().addGroupChatMessage(getContributionID(), geolocMsg,
				ChatLog.Message.Direction.OUTGOING);

		// Check if message has been sent with success or not
		if (!result) {
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageStatus(msgId, ChatLog.Message.Status.Content.FAILED);
			
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleMessageDeliveryStatus(this.getRemoteContact(),msgId, ImdnDocument.DELIVERY_STATUS_FAILED);
			}
		}
	}
	
	/**
	 * Send is composing status
	 * 
	 * @param status Status
	 */
	public void sendIsComposingStatus(boolean status) {
		String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
		String to = ChatUtils.ANOMYNOUS_URI;
		String msgId = ChatUtils.generateMessageId();
		String content = ChatUtils.buildCpimMessage(from, to, IsComposingInfo.buildIsComposingInfo(status), IsComposingInfo.MIME_TYPE);
		sendDataChunks(msgId, content, CpimMessage.MIME_TYPE);	
	}
	
	/**
	 * Add a participant to the session
	 * 
	 * @param participant Participant
	 */
	public void addParticipant(String participant) {
		try {
        	if (logger.isActivated()) {
        		logger.debug("Add one participant (" + participant + ") to the session");
        	}
    		
    		// Re-use INVITE dialog path
    		SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
    		
    		// Increment the Cseq number of the dialog path   
            getDialogPath().incrementCseq();   

            // Send REFER request
    		if (logger.isActivated()) {
        		logger.debug("Send REFER");
        	}
    		String contactUri = PhoneUtils.formatNumberToSipUri(participant);
	        SipRequest refer = null;
    		if(!RcsSettings.getInstance().isCPMSupported()){
    			 refer = SipMessageFactory.createRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
    		}
    		else{
				 if (logger.isActivated()) {
		    		logger.info("CPMS GroupChatSession addParticipant");
		    	}
    			 refer = SipMessageFactory.createCpimRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
    		}
    		 /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
            // Test if there is a subject
            if (getSubject() != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(getSubject()));
            }

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
			if(RcsSettings.getInstance().isCPMSupported()){
				 if (logger.isActivated()) {
		    		logger.info("CPMS GroupChatSession addParticipant 1");
		    	}
				if(getConversationID() != null){
					refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
				}
				if(getInReplyID() != null){
					refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
				}
			}

            /**@}*/
    		SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
	
	        // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
            	if (logger.isActivated()) {
            		logger.debug("407 response received");
            	}

    	        // Set the Proxy-Authorization header
            	authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                	logger.info("Send second REFER");
                }
				if(!RcsSettings.getInstance().isCPMSupported()){
    	        refer = SipMessageFactory.createRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
				}
				else{
					 if (logger.isActivated()) {
			    		logger.info("CPMS GroupChatSession addParticipant 2");
			    	}
					refer = SipMessageFactory.createCpimRefer(getDialogPath(), contactUri, getSubject(), getContributionID());
				}

				if(RcsSettings.getInstance().isCPMSupported()){
					 if (logger.isActivated()) {
			    		logger.info("CPMS GroupChatSession addParticipant 3");
			    	}
					if(getConversationID() != null){
						refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
					}
					if(getInReplyID() != null){
						refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());					
					}
				}
                
    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
        		ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.debug("200 OK response received");
                	}
                	
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
        	        }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
        	        }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.debug("200 OK response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
    	        }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
    	        }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(e.getMessage());
	        }
        }
	}

	/**
     * modify subject of group(chairman privilege).
     * 
     * @param newSubject new subject string 
     * @throws JoynServiceException
     */
    public void modifySubject(String newSubject) {
        try {
            if (logger.isActivated()) {
                logger.debug("modifySubject (" + newSubject + ") to the session");
            }

            // Re-use INVITE dialog path
            SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();

            // Increment the Cseq number of the dialog path
            getDialogPath().incrementCseq();

            // Send REFER request
            if (logger.isActivated()) {
                logger.debug("Send REFER");
            }
            String contact = getDialogPath().getTarget();
            String contactUri = PhoneUtils.formatNumberToSipUri(contact);
            SipRequest refer = null;

            refer = SipMessageFactory.createReferModifySubject(
                    getDialogPath(), contactUri, newSubject, getContributionID());

             /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
            // Test if there is a subject
            if (newSubject != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(newSubject));
            }

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            if(RcsSettings.getInstance().isCPMSupported()){
                 if (logger.isActivated()) {
                    logger.info("CPMS GroupChatSession addParticipant 1");
                }
                if(getConversationID() != null){
                    refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                }
                if(getInReplyID() != null){
                    refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
                }
            }

            /**@}*/
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
    
            // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
                if (logger.isActivated()) {
                    logger.debug("407 response received");
                }

                // Set the Proxy-Authorization header
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                    logger.info("Send second REFER");
                }
                if(!RcsSettings.getInstance().isCPMSupported()){
                refer = SipMessageFactory.createReferModifySubject(getDialogPath(), contactUri, newSubject, getContributionID());
                }
                else{
                     if (logger.isActivated()) {
                        logger.info("CPMS GroupChatSession addParticipant 2");
                    }
                    refer = SipMessageFactory.createCpimRefer(getDialogPath(), contactUri, newSubject, getContributionID());
                }

                if(RcsSettings.getInstance().isCPMSupported()){
                     if (logger.isActivated()) {
                        logger.info("CPMS GroupChatSession addParticipant 3");
                    }
                    if(getConversationID() != null){
                        refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                    }
                    if(getInReplyID() != null){
                        refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());                   
                    }
                }
                
                // Set the Authorization header
                authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
                ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                    if (logger.isActivated()) {
                        logger.debug("200 OK response received");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleModifySubjectSuccessful(newSubject);
                    }
                } else {
                    // Error
                    if (logger.isActivated()) {
                        logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleModifySubjectFailed(ctx.getStatusCode());
                    }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                // 200 OK received
                if (logger.isActivated()) {
                    logger.debug("200 OK response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleModifySubjectSuccessful(newSubject);
                }
            } else {
                // Error responses
                if (logger.isActivated()) {
                    logger.debug("No response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleModifySubjectFailed(ctx.getStatusCode());
                }
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("REFER request has failed", e);
            }
            
            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleModifySubjectFailed(500);
            }
        }
    }

    
    /**
     * modify nickname of participant.
     * 
     * @param contact contact of the participant
     * @param newNickname new nick name of participant 
     * @throws JoynServiceException
     */
    public void modifyMyNickName(String contact, String newNickname) {
        try {
            if (logger.isActivated()) {
                logger.debug("modifyMyNickName (" + contact + ") with nickname :"+ newNickname);
            }
            
            
            // Re-use INVITE dialog path
            SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
            
            // Increment the Cseq number of the dialog path   
            getDialogPath().incrementCseq();   

            // Send REFER request
            if (logger.isActivated()) {
                logger.debug("Send REFER");
            }
            String contactUri = PhoneUtils.formatNumberToSipUri(contact);
            SipRequest refer = null;
            
            refer = SipMessageFactory.createReferModifyNickname(getDialogPath(), contactUri, newNickname, getContributionID());

             /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
            // Test if there is a subject
            /*if (newSubject != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(newSubject));
            }*/

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            if(RcsSettings.getInstance().isCPMSupported()){
                 if (logger.isActivated()) {
                    logger.info("CPMS GroupChatSession addParticipant 1");
                }
                if(getConversationID() != null){
                    refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                }
                if(getInReplyID() != null){
                    refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
                }
            }

            /**@}*/
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
    
            // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
                if (logger.isActivated()) {
                    logger.debug("407 response received");
                }

                // Set the Proxy-Authorization header
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                getDialogPath().incrementCseq();

                // Create a second REFER request with the right token
                if (logger.isActivated()) {
                    logger.info("Send second REFER");
                }
                
                refer = SipMessageFactory.createReferModifyNickname(getDialogPath(), contactUri, newNickname, getContributionID());
                

                if(RcsSettings.getInstance().isCPMSupported()){
                     if (logger.isActivated()) {
                        logger.info("CPMS GroupChatSession addParticipant 3");
                    }
                    if(getConversationID() != null){
                        refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
                    }
                    if(getInReplyID() != null){
                        refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());                   
                    }
                }
                
                // Set the Authorization header
                authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
                ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                    if (logger.isActivated()) {
                        logger.debug("200 OK response received");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleModifyNicknameSuccessful(contact, newNickname);
                    }
                } else {
                    // Error
                    if (logger.isActivated()) {
                        logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((ChatSessionListener)getListeners().get(i)).handleModifyNicknameFailed(contact, ctx.getStatusCode());
                    }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                // 200 OK received
                if (logger.isActivated()) {
                    logger.debug("200 OK response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleModifyNicknameSuccessful(contact, newNickname);
                }
            } else {
                // Error responses
                if (logger.isActivated()) {
                    logger.debug("No response received");
                }
                
                // Notify listeners
                for(int i=0; i < getListeners().size(); i++) {
                    ((ChatSessionListener)getListeners().get(i)).handleModifyNicknameFailed(contact, ctx.getStatusCode());
                }
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("REFER request has failed", e);
            }
            
            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleModifyNicknameFailed(contact, 500);
            }
        }
    }
	/**
	 * Add a list of participants to the session
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(List<String> participants) {
		try {
			if (participants.size() == 1) {
				addParticipant(participants.get(0));
				return;
			}
			
        	if (logger.isActivated()) {
        		logger.debug("Add " + participants.size()+ " participants to the session");
        	}
    		
    		// Re-use INVITE dialog path
    		SessionAuthenticationAgent authenticationAgent = getAuthenticationAgent();
    		
            // Increment the Cseq number of the dialog path
    		getDialogPath().incrementCseq();
            
	        // Send REFER request
    		if (logger.isActivated()) {
        		logger.debug("Send REFER");
        	}
	        	SipRequest refer = null;
    		if(!RcsSettings.getInstance().isCPMSupported()){
    			 refer = SipMessageFactory.createRefer(getDialogPath(), participants, getSubject(), getContributionID());
    		}
    		else{
				 if (logger.isActivated()) {
		    		logger.info("CPMS GroupChatSession addParticipants 0");
		    	}
    			 refer = SipMessageFactory.createCpimRefer(getDialogPath(), participants, getSubject(), getContributionID());
    		}
	        /**
             * M: add contribution-id and subject referred to OMA SPEC @{
             */
            // Test if there is a subject
            if (getSubject() != null) {
                // Add a subject header
                refer.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(getSubject()));
            }

            // Add a contribution ID header
            refer.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());

			if(RcsSettings.getInstance().isCPMSupported()){
				 if (logger.isActivated()) {
		    		logger.info("CPMS GroupChatSession addParticipants 1");
		    	}
				if(getConversationID() != null){
					refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
				}
				if(getInReplyID() != null){
					refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
				}
			}

            /**@}*/
	        SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);
	
	        // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
            	if (logger.isActivated()) {
            		logger.debug("407 response received");
            	}

    	        // Set the Proxy-Authorization header
            	authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
            	getDialogPath().incrementCseq();

    			// Create a second REFER request with the right token
                if (logger.isActivated()) {
                	logger.info("Send second REFER");
                }
				if(!RcsSettings.getInstance().isCPMSupported()){
    	        refer = SipMessageFactory.createRefer(getDialogPath(), participants, getSubject(), getContributionID());
				}
				else{
					 if (logger.isActivated()) {
			    		logger.info("CPMS GroupChatSession addParticipants 2");
			    	}
					refer = SipMessageFactory.createCpimRefer(getDialogPath(), participants, getSubject(), getContributionID());
				}

				if(RcsSettings.getInstance().isCPMSupported()){
					if (logger.isActivated()) {
			    		logger.info("CPMS GroupChatSession addParticipants 3");
			    	}
					if(getConversationID() != null){
						refer.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
					}
					if(getInReplyID() != null){
						refer.addHeader(ChatUtils.HEADER_INREPLY_TO_CONTRIBUTION_ID, getInReplyID());
					}
				}
                
    	        // Set the Authorization header
    	        authenticationAgent.setProxyAuthorizationHeader(refer);
                
                // Send REFER request
    	        ctx = getImsService().getImsModule().getSipManager().sendSubsequentRequest(getDialogPath(), refer);

                // Analyze received message
                if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
                    // 200 OK response
                	if (logger.isActivated()) {
                		logger.debug("20x OK response received");
                	}
                	
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
        	        }
                } else {
                    // Error
                    if (logger.isActivated()) {
                    	logger.debug("REFER has failed (" + ctx.getStatusCode() + ")");
                    }
                    
        			// Notify listeners
        	    	for(int i=0; i < getListeners().size(); i++) {
        	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
        	        }
                }
            } else
            if ((ctx.getStatusCode() >= 200) && (ctx.getStatusCode() < 300)) {
	            // 200 OK received
            	if (logger.isActivated()) {
            		logger.debug("20x OK response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantSuccessful();
    	        }
	        } else {
	            // Error responses
            	if (logger.isActivated()) {
            		logger.debug("No response received");
            	}
            	
    			// Notify listeners
    	    	for(int i=0; i < getListeners().size(); i++) {
    	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(ctx.getReasonPhrase());
    	        }
	        }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("REFER request has failed", e);
        	}
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		((ChatSessionListener)getListeners().get(i)).handleAddParticipantFailed(e.getMessage());
	        }
        }
	}

	/**
     * Block messages in group, stack will not notify application about
     * any received message in this group
     *
     * @param flag true means block the message, false means unblock it
     * @throws JoynServiceException
     */
    public void blockMessages(final boolean flag) {
        if (logger.isActivated()) {
            logger.info("ABC block message the group session " + flag);           
        }
        this.setMessageBlocked(flag);
        // Update flag in DB
        if(flag == true) {
            RichMessagingHistory.getInstance().updateGroupBlockedStatus(this.getContributionID(), 1);
        } else {
            RichMessagingHistory.getInstance().updateGroupBlockedStatus(this.getContributionID(), 0);
        }
    }
	
	/**
     * chairman abort(leave) the group, Group session will abort
     *
     * @throws JoynServiceException
     */
    public void abortGroupSession(int reason) {
        if (logger.isActivated()) {
            logger.info("ABC Abort the group session " + reason);           
        }
        /*// Interrupt the session
        interruptSession();*/
        
        //if(!(netSwitchInfo.get_ims_off_by_network()) || !(this instanceof HttpFileTransferSession)){
        // Terminate session
        int statusCode = terminateGroupSession(reason);

        if (logger.isActivated()) {
            logger.info("ABC abortGroupSession statusCode " + statusCode);           
        }
        if ((statusCode >= 200) && (statusCode < 300)){
            if (logger.isActivated()) {
            logger.info("ABC Abort the session " + reason);
        }

            // Interrupt the session
            interruptSession();
        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleAbortConversationResult(ImsServiceSession.TERMINATION_BY_USER, 200);
            }
        } else {
            if (logger.isActivated()) {
                logger.info("ABC Abort failed ");
            }
            for(int i=0; i < getListeners().size(); i++) {
                ((ChatSessionListener)getListeners().get(i)).handleAbortConversationResult(ImsServiceSession.TERMINATION_BY_USER, statusCode);
            }
        }
        /*if(netSwitchInfo.get_ims_off_by_network()){
            netSwitchInfo.reset_ims_off_by_network();
        }*/
        
      //}
      /* if((this instanceof HttpFileTransferSession)){
            ((HttpFileTransferSession)this).pauseFileTransfer();
        }*/
    }


    /**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		rejectSession(603);
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        // Nothing to do in terminating side
        return null;
    }

    /**
     * Handle 200 0K response 
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Subscribe to event package
        if(!(this instanceof OriginatingOne2MultiStandAloneChatSession)){
        getConferenceEventSubscriber().subscribe();
    }
}
}
