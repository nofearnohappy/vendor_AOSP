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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Vector;

import com.orangelabs.rcs.core.CoreException;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax2.sip.header.Header;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.FileTransferImpl;
import com.orangelabs.rcs.service.api.FileTransferServiceImpl;
import com.orangelabs.rcs.service.api.PauseResumeFileObject;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManagerException;
import java.security.KeyStoreException;


/**
 * Originating file transfer session
 * 
 * @author jexa7410
 */
public class OriginatingPublicFileSharingSession extends ImsFileSharingSession implements MsrpEventListener {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr = null;
	
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private boolean isResumed = false;
    
    private long byteTransferredCount = -1;
    
    private long totalFileSize = -1;
    
    long bytesTransferredtoSkip = 0;

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail
	 */
	public OriginatingPublicFileSharingSession(ImsService parent, MmContent content, String contact, byte[] thumbnail) {
		super(parent, content, contact, thumbnail);
		
		// Create dialog path
		createOriginatingDialogPath();
		
		// Set contribution ID
		String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
		setContributionID(id);	
		
		String newContact = PhoneUtils.extractNumberFromUri(contact);
        if (logger.isActivated()) {
    		logger.info("CPMS OriginatingFileSharingSession newContact: " + newContact);
    	}
		
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
	    		logger.info("FTS OriginatingFileSharingSession old call id: " + getDialogPath().getCallId());
	    	}
			if(RichMessagingHistory.getInstance().getCoversationID(newContact,1).equals("")) {
				// Set Call-Id
	    		String callId = getImsService().getImsModule().getSipManager().getSipStack().generateCallId();
				if (logger.isActivated()) {
		    		logger.info("FTS OriginatingFileSharingSession callId: " + callId);
	    	}
			// Set conversation ID
		        String ConversationId = ContributionIdGenerator.getContributionId(callId);
			    if (logger.isActivated()) {
		    		logger.info("FTS OriginatingFileSharingSession setConversationID: " + ConversationId);
	    		}
	            setConversationID(ConversationId);
				RichMessagingHistory.getInstance().UpdateCoversationID(newContact,ConversationId,1);
			} else {
				 setConversationID(RichMessagingHistory.getInstance().getCoversationID(newContact,1));
			}
		}

        byteTransferredCount = -1;
		if (content != null) {
			totalFileSize = content.getSize();
		}
		if (logger.isActivated()) {
			logger.info("FTS OriginatingFileSharingSession content size: "
					+ totalFileSize);
		}
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("FTS Initiate a file transfer session as originating");
	    	}
            /**
             * M: Modified to resolve the 403 error issue.@{
             */
            SipRequest invite = createSipInvite();
                     
            // Send INVITE request
            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("FTS Session initiation has failed", e);
            }
	    	
           // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        if (logger.isActivated()) {
            logger.debug("FTS End of thread");
        }
    }
	    	
    /**
     * M: Modified to resolve the 403 error issue.@{
     */
    /**
     * @return A sip invite request
     */

    protected SipRequest createSipInvite(String callId) {
        logger.debug("FTS createSipInvite(), callId = " + callId);
        createOriginatingDialogPath(callId);
        return createSipInvite();
    }

    private SipRequest createSipInvite() {
        logger.debug("FTS createSipInvite()");
    		// Set setup mode
	    	String localSetup = createSetupOffer();
            if (logger.isActivated()){
				logger.debug("FTS Local setup attribute is " + localSetup);
			}

            // Set local port
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
            }

			// Create the MSRP manager
			String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
            if (getImsService().getImsModule().isConnectedToWifiAccess()) {
                msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
            }

			// Build SDP part
	    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String encoding = getContent().getEncoding();
			String sdp = null;
            String sendMode = "a=sendonly";
			if(!isReceiveOnly){
				if (logger.isActivated()){
					logger.debug("FTS createSipInvite isReceiveOnly: " + isReceiveOnly() + "sendMode: sendOnly");
				}
				sendMode = "a=sendonly";
			} else{
				if (logger.isActivated()){
					logger.debug("FTS createSipInvite isReceiveOnly: " + isReceiveOnly() + "sendMode: recvOnly");
				}
				sendMode = "a=recvonly";
			}
			int timeLen = getTimeLen();
			if (logger.isActivated()){
				logger.debug("FTS createSipInvite timeLen: " + timeLen + " IsSecure: " + isSecureProtocolMessage() + " IsPaused: " + this.isFileTransferPaused());
			}
			
			if(timeLen > 0){
			if(isSecureProtocolMessage()){
	    	    sdp =
	    		"v=0" + SipUtils.CRLF +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "s=-" + SipUtils.CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +			
	            "m=message " + localMsrpPort + " " + msrpMgr.getLocalSocketProtocol() + " *" + SipUtils.CRLF +
	            "a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
	            "a=fingerprint:" + KeyStoreManager.getFingerPrint() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	            "a=accept-types: " + encoding + SipUtils.CRLF +
	    		"a=file-transfer-id:" + getFileTransferId() + SipUtils.CRLF +
	    		"a=file-disposition:timelen="+ timeLen + SipUtils.CRLF +
	    		sendMode + SipUtils.CRLF;
			}
			else{
				 sdp =
	    		"v=0" + SipUtils.CRLF +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "s=-" + SipUtils.CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +			
	            "m=message " + localMsrpPort + " " + msrpMgr.getLocalSocketProtocol() + " *" + SipUtils.CRLF +
	            "a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
	            "a=setup:" + localSetup + SipUtils.CRLF +
	            "a=accept-types: " + encoding + SipUtils.CRLF +
	    		"a=file-transfer-id:" + getFileTransferId() + SipUtils.CRLF +
		    		"a=file-disposition:timelen="+ timeLen + SipUtils.CRLF +
		    		sendMode + SipUtils.CRLF;
				}
			} else {		
				if(isSecureProtocolMessage()){
					sdp =
					"v=0" + SipUtils.CRLF +
					"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
					"s=-" + SipUtils.CRLF +
					"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
					"t=0 0" + SipUtils.CRLF +			
					"m=message " + localMsrpPort + " " + msrpMgr.getLocalSocketProtocol() + " *" + SipUtils.CRLF +
					"a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
					"a=fingerprint:" + KeyStoreManager.getFingerPrint() + SipUtils.CRLF +
					"a=setup:" + localSetup + SipUtils.CRLF +
					"a=accept-types: " + encoding + SipUtils.CRLF +
					"a=file-transfer-id:" + getFileTransferId() + SipUtils.CRLF +
					"a=file-disposition:attachment" + SipUtils.CRLF +
					sendMode + SipUtils.CRLF;
				}
				else{
					 sdp =
					"v=0" + SipUtils.CRLF +
					"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
					"s=-" + SipUtils.CRLF +
					"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
					"t=0 0" + SipUtils.CRLF +			
					"m=message " + localMsrpPort + " " + msrpMgr.getLocalSocketProtocol() + " *" + SipUtils.CRLF +
					"a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF +
					"a=setup:" + localSetup + SipUtils.CRLF +
					"a=accept-types: " + encoding + SipUtils.CRLF +
					"a=file-transfer-id:" + getFileTransferId() + SipUtils.CRLF +
	    		"a=file-disposition:attachment" + SipUtils.CRLF +
	    		sendMode + SipUtils.CRLF;
			}
			}
	    	int maxSize = FileSharingSession.getMaxFileSharingSize();
	    	if (maxSize > 0) {
	    		sdp += "a=max-size:" + maxSize + SipUtils.CRLF;
	    	}
	    	PauseResumeFileObject pauseResumeObject = FileTransferServiceImpl.getPauseInfo(oldFileTransferId);
	    	if(pauseResumeObject == null)
	    	{	    		
	    		pauseResumeObject = FileTransferServiceImpl.getPauseInfo(getSessionID());
	    		if (logger.isActivated()){
					logger.debug("FTS pauseResumeObject with oldFileTransferId  : " + oldFileTransferId + "pauseResumeObject is:" + pauseResumeObject );
				}
	    	}
	    	// Set File-selector attribute
	    	String selector = getFileSelectorAttribute();
			if (this.isFileTransferPaused() && pauseResumeObject != null) {
				if (logger.isActivated()){
					logger.debug("FTS pauseResumeObject & hashselector : " + pauseResumeObject + "selctor is:" + pauseResumeObject.hashSelector );
				}
				hashselector = pauseResumeObject.hashSelector;
			} else {
				hashselector = getHashSelectorAttribute();
			PauseResumeFileObject pauseResumeObjectTemp = FileTransferServiceImpl
						.getPauseInfo(getSessionID());
			if (pauseResumeObjectTemp != null) {
				pauseResumeObjectTemp.hashSelector = hashselector;
			}
			if (logger.isActivated()) {
				logger.debug("FTS First INVITE pauseResumeObject & hashselector : "
							+ pauseResumeObjectTemp
							+ "selctor is:"
							+ hashselector
							+ "sessionID:" + getSessionID());
			}
			}
	    	if (selector != null) {
	    		sdp += "a=file-selector:" + selector + hashselector + SipUtils.CRLF;
	    	}

	    	long bytesTransferrred = 0;

	    	if(this.isFileTransferPaused() && pauseResumeObject!= null)
	    	{
	    		 if (logger.isActivated()) {
	 	        	logger.info("File Resumed true while INVITE");
	 	        }	    		
	    		bytesTransferrred = pauseResumeObject.bytesTransferrred;	    		
	    		bytesTransferredtoSkip = bytesTransferrred;
	    		 if (logger.isActivated()) {
		 	        	logger.info("bytes transferred: " + bytesTransferrred + "Old TransferId: " + oldFileTransferId);
		 	       }
	    		String fileRange = "a=file-range:" + (bytesTransferrred + 1) + "-" + totalFileSize;	
	    		if(isReceiveOnly && pauseResumeObject.pausedStream == null)
                    fileRange = "a=file-range:" + 1 + "-" + totalFileSize; 
	    		sdp += fileRange + SipUtils.CRLF;
	    	}
	    	// Set File-location attribute
	    	String location = getFileLocationAttribute();
	    	if (location != null) {
	    		sdp += "a=file-location:" + location + SipUtils.CRLF;
	    	}

	    	if (getThumbnail() != null) {
	    		// Encode the thumbnail file
	    	    String imageEncoded = Base64.encodeBase64ToString(getThumbnail());
	    	    if(imageEncoded.length() < 10 * 1024 && imageEncoded.length() > 0){ // 10k is the maximum size of thumbnail as mentioned in RCS specs.
		    		sdp += "a=file-icon:cid:image@joyn.com" + SipUtils.CRLF;	    		

	    		// Build multipart
	    		String multipart = 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": application/sdp" + SipUtils.CRLF +
	    				ContentLengthHeader.NAME + ": " + sdp.getBytes().length + SipUtils.CRLF +
	    				SipUtils.CRLF +
	    				sdp + SipUtils.CRLF + 
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    				ContentTypeHeader.NAME + ": " + "image/jpeg" + SipUtils.CRLF +
	    				SipUtils.HEADER_CONTENT_TRANSFER_ENCODING + ": base64" + SipUtils.CRLF +
	    				SipUtils.HEADER_CONTENT_ID + ": <image@joyn.com>" + SipUtils.CRLF +
	    				ContentLengthHeader.NAME + ": "+ imageEncoded.length() + SipUtils.CRLF +
	    				ContentDispositionHeader.NAME + ": icon" + SipUtils.CRLF +
	    				SipUtils.CRLF +
	    				imageEncoded + SipUtils.CRLF +
	    				Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;

	    		// Set the local SDP part in the dialog path
	    		getDialogPath().setLocalContent(multipart);	    		
	    	} else {
	    	    	if (logger.isActivated()) {
	    	        	logger.info("FTS createSipInvite Thumbnail size is greater than 10K");
	    	        }
	    	    }
	    	} else {
	    		// Set the local SDP part in the dialog path
	    		getDialogPath().setLocalContent(sdp);
	    	}
	    	try{
	        // Create an INVITE request
	        if (logger.isActivated()) {
	        	logger.info("Send INVITE");
	        }
	        SipRequest invite = createPublicInvite();
	        
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
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
    	if (logger.isActivated()) {
        	logger.info("FTS prepareMediaSession isRecv: " + this.isReceiveOnly() + " isPaused: " + this.isFileTransferPaused());
        }
    	if(this.isReceiveOnly() && this.isFileTransferPaused()){
    		if (logger.isActivated()) {
                logger.error("FTS No need to prepareMediaSession ");
            }
    		return;
    	}
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        MediaAttribute attr = mediaDesc.getMediaAttribute("path");
        String remoteMsrpPath = attr.getValue();
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
        int remotePort = mediaDesc.port;

        // Create the MSRP client session
        MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort, remoteMsrpPath, this);
        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
    	if (logger.isActivated()) {
        	logger.info("FTS startMediaSession isRecv: " + this.isReceiveOnly() + " isPaused: " + this.isFileTransferPaused());
        }
    	if(this.isReceiveOnly() && this.isFileTransferPaused()){
    		if (logger.isActivated()) {
                logger.error("No need to startMediaSession ");
            }
    		return;
    	}
        // Open the MSRP session
        msrpMgr.openMsrpSession();

        try {
            // Start sending data chunks
            byte[] data = getContent().getData();
            final InputStream stream; 
            if (data == null) {
                // Load data from URL
                stream = FileFactory.getFactory().openFileInputStream(getContent().getUrl());
            } else {
                // Load data from memory
                stream = new ByteArrayInputStream(data);
            }
            if(isFileTransferPaused())
            {            	
            	//bytesTransferredtoSkip = FileTransferServiceImpl.getPauseInfo(getSessionID()).bytesTransferrred;
            	if(bytesToSkip != 0){
            		bytesTransferredtoSkip = bytesToSkip -1;
            	}
            	if(bytesTransferredtoSkip != 0)
            	    stream.skip(bytesTransferredtoSkip);
            	FileTransferServiceImpl.getPauseInfo(getSessionID()).bytesTransferrred = bytesTransferredtoSkip;
            	if (logger.isActivated()) {
                    logger.error("startMediaSession resumed interrupted file byteskipped :" + bytesTransferredtoSkip);
                }
            }            
         // Send text message
            Thread t = new Thread() {
        		public void run() {
        			try{
            msrpMgr.sendChunks(stream, ChatUtils.generateMessageId(), getContent().getEncoding(), getContent().getSize()-bytesTransferredtoSkip);
        } catch(Exception e) {
        				if (logger.isActivated()) {
        	                logger.error("Sending data chunk has failed", e);
        	            }
        				handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
        	                    e.getMessage()));
        			}
        		}
        	};
        	t.start();           
        } catch(Exception e) {
            // Unexpected error
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }
            handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

	/**
	 * Data has been transfered
	 * 
	 * @param msgId Message ID
	 */
	public void msrpDataTransfered(String msgId) {

		String contact = this.getRemoteContact();
    	if (logger.isActivated()) {
    		logger.info("FTS Data transfered contact: " + contact + " msgId: " + msgId);
    	}
    	
    	// File has been transfered
    	fileTransfered();
    	
        // Close the media session
        closeMediaSession();
		
		// Terminate session
		terminateSession(ImsServiceSession.TERMINATION_BY_USER);
	   	
    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((FileSharingSessionListener)getListeners().get(j)).handleFileTransfered(getContent().getUrl());
        }

        // Notify delivery
        //((InstantMessagingService) getImsService()).receiveFileDeliveryStatus(getSessionID(), ImdnDocument.DELIVERY_STATUS_DELIVERED,contact);
        //((InstantMessagingService) getImsService()).receiveFileDeliveryStatus(getSessionID(), ImdnDocument.DELIVERY_STATUS_DISPLAYED,contact);
	}
	
	/**
	 * Data transfer has been received
	 * 
	 * @param msgId Message ID
	 * @param data Received data
	 * @param mimeType Data mime-type 
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
		// Not used in originating side
	}
    
	/**
	 * Data transfer in progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((FileSharingSessionListener)getListeners().get(j)).handleTransferProgress(currentSize, totalSize);
        }
    	
    	PauseResumeFileObject pauseResumeObject = FileTransferServiceImpl.getPauseInfo(getSessionID());
    	if(pauseResumeObject != null) {
    	pauseResumeObject.bytesTransferrred = currentSize;
    	pauseResumeObject.hashSelector = hashselector;
    	}
		 if (logger.isActivated()) {
 	        	logger.info("FTS msrpTransferProgress bytes transferred: " + currentSize + "pauseResumeObject:" + pauseResumeObject + "FT TransferId: " + getSessionID()
 	        			+ "hasselector: " + hashselector);
 	     }
	}	

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used in originating side
        return false;
    }

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
    	if (logger.isActivated()) {
    		logger.info("FTS Data transfer aborted");
    	}
	}

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (logger.isActivated()) {
            logger.debug("FTS MSRP session has been closed");
        }
    }
}
