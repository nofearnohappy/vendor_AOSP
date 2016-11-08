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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpConstants;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract IMS file transfer session
 * 
 * @author jexa7410
 */
public abstract class ImsFileSharingSession extends FileSharingSession {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * Default SO_TIMEOUT value (in seconds)
	 */
	public final static int DEFAULT_SO_TIMEOUT = 30;
	 boolean isImdnSupport = false;

    public boolean isImdnSupport() {
		return LauncherUtils.supportOP01() ;
	}

	public void setImdnSupport(boolean isImdnSupport) {
		this.isImdnSupport = isImdnSupport;
	}

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail
	 */
	public ImsFileSharingSession(ImsService parent, MmContent content, String contact, byte[] thumbnail) {
		super(parent, content, contact, thumbnail);
	}
	
	/**
	 * Returns the "file-transfer-id" attribute
	 * 
	 * @return String
	 */
	public String getFileTransferId() {
		return "" + System.currentTimeMillis();
	}	
	
	/**
	 * Returns the "file-selector" attribute
	 * 
	 * @return String
	 */
	public String getFileSelectorAttribute() {
		return "name:\"" + getContent().getName() + "\"" + 
			" type:" + getContent().getEncoding() +
			" size:" + getContent().getSize();
	}
	
	/**
	 * Returns the "file-selector" attribute
	 * 
	 * @return String
	 */
	public String getHashSelectorAttribute() {
		String hash = "";
		try {
			// Start sending data chunks
			byte data[] = null;
			try {
				InputStream stream = FileFactory.getFactory()
						.openFileInputStream(getContent().getUrl());
				int size = (int) getContent().getSize();
				data = new byte[size];
				stream.read(data);
			} catch (IOException e) {
				e.printStackTrace();
			}

            hash = ChatUtils.SHA1(data);
			hash = hash.replaceAll("..(?!$)", "$0:");
			hash = hash.toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (logger.isActivated()) {
    		logger.info("getHashSelectorAttribute" + hash);
    	}
		return " hash:sha-1:" + hash;
	}
	
	/**
	 * Returns the "file-location" attribute
	 * 
	 * @return String
	 */
	public String getFileLocationAttribute() {
		if ((getContent().getUrl() != null) && getContent().getUrl().startsWith("http")) {
			return getContent().getUrl();
		} else {
			return null;
		}
	}

	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
		super.receiveBye(bye);
		
		// If the content is not fully transfered then request capabilities to the remote
		if (!isFileTransfered()) {
			getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
		}
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        SipRequest invite;
		if(!RcsSettings.getInstance().isCPMSupported()){
    	if (getThumbnail() != null) {
	        invite = SipMessageFactory.createMultipartInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
    	} else {
	        invite = SipMessageFactory.createInvite(
	                getDialogPath(),
	                InstantMessagingService.FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent());
    	}
		}
		else{
		 	if (logger.isActivated()) {
	    		logger.info("CPMS ImsfileSharing createInvite 0");
	    	}
			if (getThumbnail() != null || (isImdnSupport() && !(this instanceof OriginatingGroupFileSharingSession))) {
	        invite = SipMessageFactory.createCpmMultipartInvite(
	                getDialogPath(),
	                InstantMessagingService.CPM_FT_FEATURE_TAGS,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
	    	} else {
		        invite = SipMessageFactory.createCpmInvite(
		                getDialogPath(),
		                InstantMessagingService.CPM_FT_FEATURE_TAGS,
		                getDialogPath().getLocalContent());
    		}
		}
        
    	// Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
	    		logger.info("CPMS ImsfileSharing createInvite 1");
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
    public SipRequest createPublicInvite() throws SipException {
        SipRequest invite;
		if(!RcsSettings.getInstance().isCPMSupported()){
    	if (getThumbnail() != null) {
	        invite = SipMessageFactory.createCpmMultipartPublicInvite(
	                getDialogPath(),
	                InstantMessagingService.CMCC_PUBLIC_ACCOUNT_FEATURE_TAGS1,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
    	} else {
	        invite = SipMessageFactory.createPublicAccountCpmInvite(
	                getDialogPath(),
	                InstantMessagingService.CMCC_PUBLIC_ACCOUNT_FEATURE_TAGS1,
	                getDialogPath().getLocalContent());
    	}
		}
		else {
		 	if (logger.isActivated()) {
	    		logger.info("CPMS ImsfileSharing createInvite 0");
	    	}
			if (getThumbnail() != null) {
	        invite = SipMessageFactory.createCpmMultipartPublicInvite(
	                getDialogPath(),
	                InstantMessagingService.CMCC_PUBLIC_ACCOUNT_FEATURE_TAGS1,
	                getDialogPath().getLocalContent(),
	                BOUNDARY_TAG);
	    	} else {
		        invite = SipMessageFactory.createPublicAccountCpmInvite(
		                getDialogPath(),
		                InstantMessagingService.CMCC_PUBLIC_ACCOUNT_FEATURE_TAGS1,
		                getDialogPath().getLocalContent());
    		}
		}
        
    	// Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
	    		logger.info("CPMS ImsfileSharing createInvite 1");
	    	}
			if(getConversationID() != null){
				invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
			}
		}

        return invite;
    }

    /**
     * Create large mode file INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createLargeModeFileInvite() throws SipException {
        SipRequest invite;
        if (logger.isActivated()) {
            logger.info("CPMS createLargeModeFileInvite  0");
        }
        if (getThumbnail() != null || isImdnSupport()) {
        invite = SipMessageFactory.createCpmMultipartInvite(
                getDialogPath(),
                InstantMessagingService.CPM_LARGE_MESSAGE_FEATURE_TAGS,
                getDialogPath().getLocalContent(),
                BOUNDARY_TAG);
        } else {
            invite = SipMessageFactory.createCpmInvite(
                    getDialogPath(),
                    InstantMessagingService.CPM_LARGE_MESSAGE_FEATURE_TAGS,
                    getDialogPath().getLocalContent());
        }
        
        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
        if(RcsSettings.getInstance().isCPMSupported()){
            if (logger.isActivated()) {
                logger.info("CPMS createLargeModeFileInvite  1");
            }
            if(getConversationID() != null){
                invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
            }
        }

        return invite;
    }

    /**
	 * Build a CPIM message with full IMDN headers
	 * 
	 * @param from From URI
	 * @param to To URI
	 * @param messageId Message ID
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithImdn(String from, String to, String messageId) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUriAlias(from) + SipUtils.CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + SipUtils.CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + SipUtils.CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + SipUtils.CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + SipUtils.CRLF + 
			ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY  + SipUtils.CRLF +
			SipUtils.CRLF;	
		return cpim;
	}

    /**
     * Handle error 
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }

        // Error    
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleTransferError(new FileSharingError(error));
        }
    }

    /**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
        if (isSessionInterrupted()) {
        	return;
        }
        
        if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }

        try {
            // Terminate session
            terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

            // Close the media session
            closeMediaSession();
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't close correctly the file transfer session", e);
            }
        }

        // Request capabilities
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleTransferError(new FileSharingError(FileSharingError.MEDIA_TRANSFER_FAILED, error));
        }
    }
}
