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

package com.orangelabs.rcs.core.ims.service.im;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.format.Time;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionBasedServiceError;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.utils.logger.Logger;

import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManagerException;
import java.security.KeyStoreException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ISpamReportListener;

/**
 * Originating one-to-one chat session
 * 
 * @author jexa7410
 */
public class OriginatingSpamSession extends ImsServiceSession implements ImsSessionListener {	
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";
	
	/**
     * Contribution ID
     */
    private String contributionId = null;
    
    /**
     * Accept types
     */
    private String acceptTypes;

    public String getAcceptTypes() {
		return acceptTypes;
	}

	public void setAcceptTypes(String acceptTypes) {
		this.acceptTypes = acceptTypes;
	}

	/**
     * Wrapped types
     */
    private String wrappedTypes;
    
    public String getWrappedTypes() {
		return wrappedTypes;
	}

	public void setWrappedTypes(String wrappedTypes) {
		this.wrappedTypes = wrappedTypes;
	}

	public String getContributionId() {
		return contributionId;
	}
    
    private String mMsgid;
    
    private InstantMessage spamMessage = null;

	public InstantMessage getSpamMessage() {
		return spamMessage;
	}

	public void setSpamMessage(InstantMessage spamMessage) {
		this.spamMessage = spamMessage;
	}

	public void setContributionId(String contributionId) {
		this.contributionId = contributionId;
	}

	/**
     * Conversation ID
     */
    private String ConversationId = null;

	public String getConversationId() {
		return ConversationId;
	}

	public void setConversationId(String conversationId) {
		ConversationId = conversationId;
	}
	
	/**
	 * Spam listeners
	 */
	private Vector<ISpamReportListener> spamlisteners = new Vector<ISpamReportListener>();
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private MsrpManager msrpMgr;
	
	private String badUser = null;

    public MsrpManager getMsrpMgr() {
		return msrpMgr;
	}

	public void setMsrpMgr(MsrpManager msrpMgr) {
		this.msrpMgr = msrpMgr;
	}

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact
	 * @param msg First message of the session
	 */
	public OriginatingSpamSession(ImsService parent,String contact, String msgId) {
		
		super(parent,contact);
        // Create dialog path
        createOriginatingDialogPath();
        mMsgid = msgId;
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
		String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
		msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
		if (parent.getImsModule().isConnectedToWifiAccess()) {
			msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
		}
		InstantMessage msg = new InstantMessage(msgId,contact,StringUtils.encodeUTF8("Hi"),Core.getInstance().getImService().getImdnManager().isImdnActivated(),null);
        setSpamMessage(msg);
		// Set contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionId(id);
        badUser = contact;
		// Set accept-types
		String acceptTypes = CpimMessage.MIME_TYPE + " "
				+ IsComposingInfo.MIME_TYPE;
		setAcceptTypes(acceptTypes);

		// Set accept-wrapped-types
		String wrappedTypes = InstantMessage.MIME_TYPE + " "
				+ ImdnDocument.MIME_TYPE;
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
			wrappedTypes += " " + GeolocInfoDocument.MIME_TYPE;
		}
		if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
			wrappedTypes += " " + FileTransferHttpInfoDocument.MIME_TYPE;
		}
		setWrappedTypes(wrappedTypes);
		addListener(this);
	}
	
	/**
	 * Add a listener for receiving events
	 * 
	 * @param listener Listener
	 */
	public void addSpamListener(ISpamReportListener listener) {
		if (logger.isActivated()) {
    		logger.info("addSpamListener " + listener);
    	}
		spamlisteners.add(listener);
	}

	/**
	 * Remove a listener
	 */
	public void removeListener(ISpamReportListener listener) {
		if (logger.isActivated()) {
    		logger.info("removeSpamListener " + listener);
    	}
		spamlisteners.remove(listener);
	}
	
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
                    InstantMessagingService.CHAT_FEATURE_TAGS, 
                    content);
		}
		else{
			if (logger.isActivated()) {
	    		logger.info("CPMS OneOneChatSession createInviteRequest 0");
	    	}
			invite = SipMessageFactory.createCpmInvite(getDialogPath(), 
                    InstantMessagingService.CPM_LARGE_MESSAGE_FEATURE_TAGS, 
                    content);
		}

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getConversationId()); 
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
	    		logger.info("CPMS OneOneChatSession createInviteRequest 1");
	    	}
			if(getConversationId() != null){
				invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationId()); 
			}
		}

        return invite;
    }

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new spam session as originating");
	    	}          
	    	SipRequest invite = createSipInvite();	    	
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
			 logger.debug("isSecureProtocolMessage()");
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
	    		"a=sendonly" + SipUtils.CRLF;
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
	    		"a=sendonly" + SipUtils.CRLF;
			}
			
				 Cursor cursor = null;
			     int emptyCursor = 0;
			     long timeStamp = System.currentTimeMillis();
			     String text = null;
		     String[] selectionArg = { mMsgid };
			     ContentResolver contentResolver = AndroidFactory
			                .getApplicationContext().getContentResolver();  
			            try {
			                cursor = contentResolver.query(ChatLog.Message.CONTENT_URI,
			                        null, ChatLog.Message.MESSAGE_ID + "=?",
			                        selectionArg, null);
			                if (cursor.moveToFirst()) {
			                    emptyCursor = 1;			                   
			                    byte[] bodyData = cursor.getBlob(cursor
			                            .getColumnIndex(ChatLog.Message.BODY));		                 
			                    
			                    if (bodyData != null) {
			                        text = new String(bodyData);
			                    }			                                      
			                    timeStamp = cursor.getLong(cursor
			                            .getColumnIndex(ChatLog.Message.TIMESTAMP));
		                    badUser = cursor.getString(cursor
		                            .getColumnIndex(ChatLog.Message.CONTACT_NUMBER));
			                } 
			            }finally {
			                    if (null != cursor) {
			                        cursor.close();
			                    }
			                }
			
			// If there is a first message then builds a multipart content else builds a SDP content
	    	if (getSpamMessage() != null) {
		    	// Build CPIM part	 
	    		String to = ImsModule.IMS_USER_PROFILE.getPreferredUri();
	    		String from;
	    		if(to.startsWith("tel:"))
	    		{
	    			from = "<tel:" + badUser + ">";
	    		}
	    		else
	    		{
	    			from = "<sip:" + badUser + "@" + ImsModule.IMS_USER_PROFILE.getPreferredUri().substring(to.indexOf("@" )+ 1) + ">";	
	    		}
	    		to = "<" + to + ">";
				logger.info("Spam Session createSipInvite from :" + from + " to: " + to);
				
				String spamsdpinfo;
				// Retreive Data from databse to report to server
				
				
			    String localTime = DateUtils.encodeDate(System.currentTimeMillis());
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			    localTime = sdf.format(new Date(System.currentTimeMillis()));
				spamsdpinfo = 
				"Spam-From:" + from + SipUtils.CRLF + "Spam-To:" + to				
				+ SipUtils.CRLF + "DateTime:" + localTime ;
				
				boolean useImdn = ((InstantMessagingService)getImsService()).getImdnManager().isImdnActivated();
				String formattedMsg;
				String mime;				
				if (getSpamMessage() instanceof FileTransferMessage) {
					FileTransferMessage fileMsg = (FileTransferMessage)getSpamMessage();
					formattedMsg = fileMsg.getFileInfo();
					mime = FileTransferHttpInfoDocument.MIME_TYPE;
				} else {
					formattedMsg = getSpamMessage().getTextMessage();
					mime = InstantMessage.MIME_TYPE;
				}
				
				String cpim;
				if (useImdn) {
					// Send message in CPIM + IMDN
					cpim = ChatUtils.buildCpimMessageForSpam(
	    					from,
	    					to,
	    					mMsgid,
		        			StringUtils.encodeUTF8(text),
		        			mime,timeStamp);
				} else {
					// Send message in CPIM
					cpim = ChatUtils.buildCpimMessageForSpamNoIMDN(
	    					from,
	    					to,
		        			StringUtils.encodeUTF8(text),
		        			mime);
				}
		    	// Build multipart
		        String multipart = 
		        	Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +		        	 
		        	"Content-Type: text/plain;charset=UTF-8" + SipUtils.CRLF +
		        	SipUtils.CRLF + spamsdpinfo + SipUtils.CRLF +
		        	SipUtils.CRLF +
		        	Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +		        	
	    			"Content-Type: application/sdp" + SipUtils.CRLF +	    			
	    			SipUtils.CRLF +
	    			sdp + SipUtils.CRLF + 
	    			Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF +
	    			"Content-Type: " + CpimMessage.MIME_TYPE + SipUtils.CRLF +
	    			/*"Content-Length: "+ cpim.getBytes().length + SipUtils.CRLF +*/
	    			SipUtils.CRLF +
	    			cpim + SipUtils.CRLF +
	    			Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;

				// Set the local SDP part in the dialog path
		    	getDialogPath().setLocalContent(multipart);
	    	} else {
				// Set the local SDP part in the dialog path
		    	getDialogPath().setLocalContent(sdp);
	    	}   	
			
            try {
            SipRequest invite = createInvite();

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

        logger.error("Create sip invite failed, retrn null.");
        return null;
	}

    
	@Override
	public void prepareMediaSession() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startMediaSession() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeMediaSession() {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws SipException
     */
    private SipRequest createMultipartInviteRequest(String content) throws SipException {
    	SipRequest invite = null;
    	if(!RcsSettings.getInstance().isCPMSupported()){
    		 invite = SipMessageFactory.createMultipartInvite(getDialogPath(), 
    				 ChatUtils.getSupportedFeatureTagsForChat().toArray(new String[0]), 
                    content,
                    BOUNDARY_TAG);
    	}
		else{
			if (logger.isActivated()) {
	    		logger.info("CPMS OneOneChatSession createMultipartInviteRequest0 ");
	    	}
			invite = SipMessageFactory.createCpmSpamMultipartInvite(getDialogPath(), 
			        InstantMessagingService.CPM_LARGE_MESSAGE_FEATURE_TAGS, 
                    content,
                    BOUNDARY_TAG);
		}
        // Test if there is a text message
        if ((getSpamMessage() != null) && (getSpamMessage().getTextMessage() != null)) {
            // Add a subject header
            //invite.addHeader(SubjectHeader.NAME, StringUtils.encodeUTF8(getFirstMessage().getTextMessage()));
        }

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionId()); 
		if(RcsSettings.getInstance().isCPMSupported()){
			if (logger.isActivated()) {
	    		logger.info("CPMS OneOneChatSession createMultipartInviteRequest 1");
	    	}
			if(getConversationId() != null){
				invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationId());
			}
		}

        return invite;
    }


	@Override
	public SipRequest createInvite() throws SipException {
		// If there is a first message then builds a multipart content else builds a SDP content
        SipRequest invite; 
        if (getSpamMessage() != null) {
            invite = createMultipartInviteRequest(getDialogPath().getLocalContent());
        } else {
            invite = createInviteRequest(getDialogPath().getLocalContent());
        }
        return invite;
	}

	@Override
	public void handleError(ImsServiceError error) {
		if (logger.isActivated()) {
    		logger.info("originating Spam handleError" + error.getErrorCode());
    	} 
		if (spamlisteners != null) {
			for (int i = 0; i < spamlisteners.size(); i++) {
				try {
					if (logger.isActivated()) {
			    		logger.info("listener found ");
			    	}
					spamlisteners.get(i).onSpamReportFailed(badUser,mMsgid,error.getErrorCode());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	@Override
	public void handleSessionStarted() {
		if (logger.isActivated()) {
    		logger.info("originating Spam handleSessionStarted");
    	} 
		 // Notify listeners
		if (spamlisteners != null) {
			for (int i = 0; i < spamlisteners.size(); i++) {
				try {
					if (logger.isActivated()) {
			    		logger.info("listener found ");
			    	}
					spamlisteners.get(i).onSpamReportSuccess(badUser,mMsgid);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	@Override
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
    		logger.info("originating Spam handleSessionAborted" + reason);
    	} 
		 // Notify listeners
		if (spamlisteners != null) {
			for (int i = 0; i < spamlisteners.size(); i++) {
				try {
					if (logger.isActivated()) {
			    		logger.info("listener found ");
			    	}
					spamlisteners.get(i).onSpamReportFailed(badUser,mMsgid,reason);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	@Override
	public void handleSessionTerminatedByRemote() {
		// TODO Auto-generated method stub
		
	}
}
 /**
     * @}
     */
