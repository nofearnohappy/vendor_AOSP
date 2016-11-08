package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import java.util.Vector;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ListOfParticipant;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.StorageUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.SettingUtils;

/**
 * Abstract file sharing session 
 * 
 * @author jexa7410
 */
public abstract class FileSharingSession extends ImsServiceSession {
    /**
     * Contribution ID
     */
    private String contributionId = null;	
    
    /**
     * Contversation ID
     */
    private String contversationId = null;	

	  /**
     * Contversation ID
     */
    private String inReplyId = null;	
    
    /**
     * Contversation ID
     */
    private String messageId = null;	
    
    public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	/**
	 * Content to be shared
	 */
	protected MmContent content;
	
	private String thumbUrl = null;
	
	private boolean isGeoLocFile = false;
	
	/**
	 * File transfered
	 */
	private boolean fileTransfered = false;

    /**
     * List of participants
     */
    protected ListOfParticipant participants = new ListOfParticipant();

    /**
	 * Thumbnail
	 */
	private byte[] thumbnail = null;
	
	/**
	 * Display Name
	 */
	private String displayName = null;
	
	/**
	 * File transfer paused
	 */
	protected boolean fileTransferPaused = false;  

    /**
	 * Old File transfer Id
	 */
	protected String oldFileTransferId = null;  

	public String getOldFileTransferId() {
		return oldFileTransferId;
	}

	public void setOldFileTransferId(String oldFileTransferId) {
		this.oldFileTransferId = oldFileTransferId;
	}
	
	protected long bytesToSkip = 0;  
	
	/**
	 * Old File transfer Id
	 */
	protected boolean isReceiveOnly = false; 
	
    public boolean isReceiveOnly() {
		return isReceiveOnly;
	}

	public void setReceiveOnly(boolean isReceiveOnly) {
		this.isReceiveOnly = isReceiveOnly;
	}
	
	protected boolean isSendOnly = false; 

	public boolean isSendOnly() {
		return isSendOnly;
	}

	public void setSendOnly(boolean isSendOnly) {
		this.isSendOnly = isSendOnly;
	}

	public String hashselector = "";
	
	public String getHashselector() {
		return hashselector;
	}

	private boolean isMultiFileTransfer = false;

	/**
     * File time length
     */
    private int timeLen = 0;

    
    /**
     * BURN mode
     */
    private boolean isBurnMessage = false;
    
    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(FileSharingSession.class.getName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail
	 */
	public FileSharingSession(ImsService parent, MmContent content, String contact, byte[] thumbnail) {
		super(parent, contact);
		
		this.content = content;
		this.thumbnail = thumbnail;
	}

	/**
	 * Return the contribution ID
	 * 
	 * @return Contribution ID
	 */
	public String getContributionID() {
		return contributionId;
	}	
	
	/**
	 * Set the contribution ID
	 * 
	 * @param id Contribution ID
	 */
	public void setContributionID(String id) {
		this.contributionId = id;
	}
	
	/**
	 * Return the conversation ID
	 * 
	 * @return conversation ID
	 */
	public String getConversationID() {
		return contversationId;
	}	
	
	  public void extractFileRange(SipResponse rsp){
		try {
	    	String remoteSdp = rsp.getSdpContent();
	    	SdpParser parser = new SdpParser(remoteSdp.getBytes());
			Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription mediaDesc = media.elementAt(0);
			
			MediaAttribute attr1 = mediaDesc.getMediaAttribute("file-range");
			if (attr1 != null) {
				bytesToSkip = Long.parseLong(attr1.getValue().substring(0,
						attr1.getValue().indexOf("-")));
			if (logger.isActivated()){
					logger.debug("extractFileRange, FileSharingSession bytesToSkip:"
							+ bytesToSkip);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
	    }
	
	/**
	 * Set the conversation ID
	 * 
	 * @param id conversation ID
	 */
	public void setConversationID(String id) {
		this.contversationId = id;
	}

	/**
	 * Return the conversation ID
	 * 
	 * @return conversation ID
	 */
	public String getInReplyID() {
		return inReplyId;
	}	
	
	/**
	 * Set the conversation ID
	 * 
	 * @param id conversation ID
	 */
	public void setInReplyID(String id) {
		this.inReplyId = id;
	}
	
	/**
	 * Return the contribution ID
	 * 
	 * @return Contribution ID
	 */
	public String getDisplayName() {
		return displayName;
	}	
	
	/**
	 * Set the contribution ID
	 * 
	 * @param id Contribution ID
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Returns the content
	 * 
	 * @return Content 
	 */
	public MmContent getContent() {
		return content;
	}
	
	/**
	 * Returns the list of participants involved in the transfer
	 * 
	 * @return List of participants 
	 */
	public ListOfParticipant getParticipants() {
		return participants;
	}
	
	/**
	 * Set the content
	 * 
	 * @param content Content  
	 */
	public void setContent(MmContent content) {
		this.content = content;
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
	 * File has been transfered
	 */
	public void fileTransfered() {
		this.fileTransfered = true;
		
	}
	
	/**
	 * Is file transfered
	 * 
	 * @return Boolean
	 */
	public boolean isFileTransfered() {
		return fileTransfered; 
	}
	
	/**
     * File has been transfered
     */
    public boolean isGeoLocFile() {
        return this.isGeoLocFile;
        
    }
    
    /**
     * Is file transfered
     * 
     * @return Boolean
     */
    public void setGeoLocFile() {
        this.isGeoLocFile = true; 
    }
	
	/**
	 * File has been paused
	 */
	public void fileTransferPaused() {
		this.fileTransferPaused = true;
	}
	
	 public int getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(int timeLen) {
        this.timeLen = timeLen;
    }
    
	/**
	 * File is resuming
	 */
	public void fileTransferResumed() {
		this.fileTransferPaused = false;
	}
	
	/**
	 * Is file transfer paused
	 * 
	 * @return fileTransferPaused
	 */
	public boolean isFileTransferPaused() {
		return fileTransferPaused; 
	}

	/**
	 * Returns max file sharing size
	 * 
	 * @return Size in bytes
	 */
	public static int getMaxFileSharingSize() {
		return RcsSettings.getInstance().getMaxFileTransferSize()*1024;
	}

    /** M: ftAutAccept @{ */
    public boolean shouldAutoAccept() {
    	if( RcsSettings.getInstance().isFileTransferAutoAccepted()){
			boolean isRoaming = SettingUtils.isRoaming(AndroidFactory
                .getApplicationContext());
	        // whether ftAutAccept is enabled if roaming.
	        if (isRoaming) {
	            return RcsSettings.getInstance()
	                    .isEnableFtAutoAcceptWhenRoaming();
	        } else {
	            return RcsSettings.getInstance()
	                    .isEnableFtAutoAcceptWhenNoRoaming();
	        }
    	}
		else{
        	return false;
		}
    }

    /** @} */

    /**
     * Returns the thumbnail
     * 
     * @return Thumbnail
     */
    public byte[] getThumbnail() {
    	return thumbnail;
    }

    /**
     * Set the thumbnail
     *
     * @param Thumbnail
     */
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public boolean isMultiFileTransfer() {
		return isMultiFileTransfer;
	}

	public void setMultiFileTransfer(boolean isMultiFileTransfer) {
		this.isMultiFileTransfer = isMultiFileTransfer;
	}


/**
     * Returns the thumbnail
     * 
     * @return Thumbnail
     */
    public String getThumbUrl() {
    	return thumbUrl;
    }

  /**
     * Set the thumbnail
     *
     * @param Thumbnail
     */
    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

	/**
	 * Check if file capacity is acceptable
	 * 
	 * @param fileSize File size in bytes
	 * @return Error or null if file capacity is acceptable
	 */
	public static FileSharingError isFileCapacityAcceptable(long fileSize) {
		boolean fileIsToBig = (FileSharingSession.getMaxFileSharingSize() > 0) ? fileSize > FileSharingSession.getMaxFileSharingSize() : false;
		boolean storageIsTooSmall = (StorageUtils.getExternalStorageFreeSpace() > 0) ? fileSize > StorageUtils.getExternalStorageFreeSpace() : false;
		if (fileIsToBig) {
			if (logger.isActivated()) {
				logger.warn("File is too big, reject the file transfer");
			}
			return new FileSharingError(FileSharingError.MEDIA_SIZE_TOO_BIG);
		} else {
			if (storageIsTooSmall) {
				if (logger.isActivated()) {
					logger.warn("Not enough storage capacity, reject the file transfer");
				}
				return new FileSharingError(FileSharingError.NOT_ENOUGH_STORAGE_SPACE);
			}
		}
		return null;
	}
	
	
	public void setBurnMessage(boolean burnState){
		isBurnMessage = burnState;
	}
	
	public boolean  isBurnMessage(){
		return isBurnMessage;
	}
	
	private boolean isPublicChatFile = false;

	public boolean isPublicChatFile() {
		return isPublicChatFile;
	}

	public void setPublicChatFile(boolean isPublicChatFile) {
		this.isPublicChatFile = isPublicChatFile;
	}
	
}
