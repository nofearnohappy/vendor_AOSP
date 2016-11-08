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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimIdentity;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingManager;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardNotifSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.TerminatingHttpFileSharingSession;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat session
 *
 * @author jexa7410
 */
public abstract class ChatSession extends ImsServiceSession implements MsrpEventListener {
    /**
     * Subject of group
     */
    private String subject = null;

    /**
     * Message blocked or not in group
     */
    private boolean isMessageBlocked = false;

    public boolean isMessageBlocked() {
        return isMessageBlocked;
    }

    public void setMessageBlocked(boolean isMessageBlocked) {
        this.isMessageBlocked = isMessageBlocked;
    }

    /**
     * Chairman of group
     */
    private String chairman = null;

    /**
     * First message
     */
    private InstantMessage firstMessage = null;

    /**
     * List of participants
     */
    private ListOfParticipant participants = new ListOfParticipant();

    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr = null;

    /**
     * Is composing manager
     */
    private IsComposingManager isComposingMgr = new IsComposingManager(this);

    /**
     * Chat activity manager
     */
    private ChatActivityManager activityMgr = new ChatActivityManager(this);

    /**
     * Max number of participants in the session
     */
    private int maxParticipants = RcsSettings.getInstance().getMaxChatParticipants();

    /**
     * Contribution ID
     */
    private String contributionId = null;

    /**
     * Conversation ID
     */
    private String ConversationId = null;

    /**
     * Conversation ID
     */
    private String InReplyId = null;

    /**
     * Feature tags
     */
    private List<String> featureTags = new ArrayList<String>();

    /**
     * Feature tags
     */
    private List<String> cpimFeatureTags = new ArrayList<String>();

    /**
     * Feature tags
     */
    private List<String> cmccCpimFeatureTags = new ArrayList<String>();

    /**
     * Accept types
     */
    private String acceptTypes;

    /**
     * Wrapped types
     */
    private String wrappedTypes;

    /**
     * Geolocation push supported by remote
     */
    private boolean geolocSupportedByRemote = false;

    /**
     * File transfer supported by remote
     */
    private boolean ftSupportedByRemote = false;

    /**
     * The logger
     */
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    // is burn message
    private boolean isBurnMessage = false;

    // is public account message
    private boolean isPublicAccountMessage = false;

    // is cloud message
    private boolean isCloudMessage = false;

    public boolean isCloudMessage() {
        return isCloudMessage;
    }

    public void setCloudMessage(boolean isCloudMessage) {
        this.isCloudMessage = isCloudMessage;
    }

    // is large mode
    private boolean isLargeMessageMode = false;

    public boolean isLargeMessageMode() {
        return isLargeMessageMode;
    }

    public void setLargeMessageMode(boolean isLargeMessageMode) {
        this.isLargeMessageMode = isLargeMessageMode;
    }

    // Imdn MesageId
    private String imdnMessageId = null;

    public String getImdnMessageId() {
        return imdnMessageId;
    }

    public void setImdnMessageId(String imdnMessageId) {
        this.imdnMessageId = imdnMessageId;
    }

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param contact Remote contact
     * @param participants List of participants
     */
    public ChatSession(ImsService parent, String contact, ListOfParticipant participants) {
        super(parent, contact);

        // Set the session participants
        this.participants = participants;

        // Create the MSRP manager
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
        String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
        if (parent.getImsModule().isConnectedToWifiAccess()) {
            msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
        }
    }

    /**
     * Get feature tags
     *
     * @return Feature tags
     */
    public String[] getFeatureTags() {
        return featureTags.toArray(new String[0]);
    }

    /**
     * Get feature tags
     *
     * @return Feature tags
     */
    public String[] getCpimFeatureTags() {
        return cpimFeatureTags.toArray(new String[0]);
    }

    /**
     * Get feature tags
     *
     * @return Feature tags
     */
    public String[] getCmccCpimFeatureTags() {
        return cmccCpimFeatureTags.toArray(new String[0]);
    }

    /**
     * Set feature tags
     *
     * @param tags Feature tags
     */
    public void setFeatureTags(List<String> tags) {
        this.featureTags = tags;
    }

    /**
     * Set feature tags
     *
     * @param tags Feature tags
     */
    public void setCpimFeatureTags(List<String> tags) {
        this.cpimFeatureTags = tags;
    }

    public void setCmccCpimFeatureTags(List<String> tags) {
        this.cmccCpimFeatureTags = tags;
    }

    /**
     * Get accept types
     *
     * @return Accept types
     */
    public String getAcceptTypes() {
        return acceptTypes;
    }

    /**
     * Set accept types
     *
     * @param types Accept types
     */
    public void setAcceptTypes(String types) {
        this.acceptTypes = types;
    }

    /**
     * Get wrapped types
     *
     * @return Wrapped types
     */
    public String getWrappedTypes() {
        return wrappedTypes;
    }

    /**
     * Set wrapped types
     *
     * @param types Wrapped types
     */
    public void setWrappedTypes(String types) {
        this.wrappedTypes = types;
    }

    /**
     * Return the first message of the session
     *
     * @return Instant message
     */
    public InstantMessage getFirstMessage() {
        return firstMessage;
    }

    /**
     * Set first message
     *
     * @param firstMessage First message
     */
    protected void setFirstMesssage(InstantMessage firstMessage) {
        this.firstMessage = firstMessage;
    }

    /**
     * Returns the subject of the session
     *
     * @return String
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the chairman of the session
     *
     * @return String
     */
    public String getChairman() {
        return chairman;
    }

    /**
     * Set the subject of the session
     *
     * @param subject Subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Set the chairman of the session
     *
     * @param chairman chairman
     */
    public void setChairman(String chairman) {
        logger.info("Chairman set " + chairman);
        this.chairman = chairman;
    }

    /**
     * Returns the IMDN manager
     *
     * @return IMDN manager
     */
    public ImdnManager getImdnManager() {
        return ((InstantMessagingService) getImsService()).getImdnManager();
    }

    /**
     * Returns the session activity manager
     *
     * @return Activity manager
     */
    public ChatActivityManager getActivityManager() {
        return activityMgr;
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
     * Return the contribution ID
     *
     * @return Contribution ID
     */
    public String getConversationID() {
        return ConversationId;
    }

    /**
     * Set the contribution ID
     *
     * @param id Contribution ID
     */
    public void setConversationID(String id) {
        this.ConversationId = id;
    }

    /**
     * Return the contribution ID
     *
     * @return Contribution ID
     */
    public String getInReplyID() {
        return InReplyId;
    }

    /**
     * Set the contribution ID
     *
     * @param id Contribution ID
     */
    public void setInReplyID(String id) {
        this.InReplyId = id;
    }

    /**
     * Returns the list of participants
     *
     * @return List of participants
     */
    public ListOfParticipant getParticipants() {
        return participants;
    }

    /**
     * Returns the list of participants currently connected to the session
     *
     * @return List of participants
     */
    public abstract ListOfParticipant getConnectedParticipants();

    /**
     * Returns the IM session identity
     *
     * @return Identity (e.g. SIP-URI)
     */
    public String getImSessionIdentity() {
        if (getDialogPath() != null) {
            return getDialogPath().getTarget();
        } else {
            return null;
        }
    }

    /**
     * Returns the MSRP manager
     *
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return msrpMgr;
    }

    /**
     * Is geolocation supported by remote
     *
     * @return Boolean
     */
    public boolean isGeolocSupportedByRemote() {
        return geolocSupportedByRemote;
    }

    /**
     * Set geolocation supported by remote
     *
     * @param suppported Supported
     */
    public void setGeolocSupportedByRemote(boolean supported) {
        this.geolocSupportedByRemote = supported;
    }

    /**
     * Is file transfer supported by remote
     *
     * @return Boolean
     */
    public boolean isFileTransferSupportedByRemote() {
        return ftSupportedByRemote;
    }

    /**
     * Set file transfer supported by remote
     *
     * @param suppported Supported
     */
    public void setFileTransferSupportedByRemote(boolean supported) {
        this.ftSupportedByRemote = supported;
    }

    /**
     * Close the MSRP session
     */
    public void closeMsrpSession() {
        if (logger.isActivated()) {
            logger.debug("ABC closeMsrpSession MSRP session has been closed");
        }
        if (getMsrpMgr() != null) {
            getMsrpMgr().closeSession();
            if (logger.isActivated()) {
                logger.debug("ABC MSRP session has been closed");
            }
        }
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
            logger.info("ABC Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }

        if (logger.isActivated()) {
            logger.info("ABC handleError ");
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i)).handleImError(new ChatError(error));
        }
    }

    /**
     * Handle 480 Temporarily Unavailable
     *
     * @param resp 480 response
     */
    public void handle480Unavailable(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 486 Busy
     *
     * @param resp 486 response
     */
    public void handle486Busy(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    /**
     * Handle 603 Decline
     *
     * @param resp 603 response
     */
    public void handle603Declined(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Data has been transfered
     *
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        if (logger.isActivated()) {
            logger.info("Data transfered");
        }

        if (msgId != null) {
            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                ((ChatSessionListener) getListeners().get(i)).handleMessageDeliveryStatus(
                        this.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_SENT);
            }
        }

        // Update the activity manager
        activityMgr.updateActivity();
    }

    /**
     * Data transfer has been received
     *
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        if (logger.isActivated()) {
            logger.info("Data received (type " + mimeType + ")");
        }
        String sData = new String(data);
        logger.info("MSRPHEAD msrpDataReceived " + sData);

        // Update the activity manager
        activityMgr.updateActivity();

        if ((data == null) || (data.length == 0)) {
            // By-pass empty data
            if (logger.isActivated()) {
                logger.debug("By-pass received empty data");
            }
            return;
        }

        if (ChatUtils.isApplicationIsComposingType(mimeType)) {
            // Is composing event
            receiveIsComposing(getRemoteContact(), data);
        } else if (ChatUtils.isTextPlainType(mimeType)) {
            // Text message
            receiveText(getRemoteContact(),
                    StringUtils.decodeUTF8(data), msgId, false, new Date(), null);
        } else if (ChatUtils.isMessageCpimType(mimeType)) {
            // Receive a CPIM message
            try {
                CpimParser cpimParser = new CpimParser(data);
                CpimMessage cpimMsg = cpimParser.getCpimMessage();
                if (cpimMsg != null) {
                    Date date = cpimMsg.getMessageDate();
                    String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
                    if (cpimMsgId == null) {
                        cpimMsgId = msgId;
                    }
                    String contentType = cpimMsg.getContentType();
                    if (logger.isActivated()) {
                        logger.info("msrpDataReceived: contentType is  " + contentType);
                    }

                    String from = getRemoteContact();
                    String pseudo = null;
                    if (isGroupChat()) {
                        from = cpimMsg.getHeader(CpimMessage.HEADER_FROM);
                    }
                    if (logger.isActivated()) {
                        logger.info("msrpDataReceived: from is  " + from);
                    }

                    // Extract URI and optional display name
                    try {
                        CpimIdentity cpimIdentity = new CpimIdentity(from);
                        pseudo = cpimIdentity.getDisplayName();
                        from = cpimIdentity.getUri();
                        if (logger.isActivated()) {
                            logger.info("Cpim Identity: " + cpimIdentity);
                        }
                    } catch (IllegalArgumentException e) {
                        // Intentionally blank
                    }
                    String number = PhoneUtils.extractNumberFromUri(from);

                    // Check if the message needs a delivery report
                    boolean imdnDisplayedRequested = false;
                    String dispositionNotification = cpimMsg
                            .getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
                    String contentDisposition = null;
                    try {
                        contentDisposition = cpimMsg.getContentDisposition();
                    } catch (Exception e) {

                    }
                    if (logger.isActivated()) {
                        logger.info("msrpDataReceived: dispositionNotification "
                                + contentDisposition);
                    }
                    boolean isPubliMessage = false;
                    try {
                        if (contentDisposition.contains("publicmessage")) {
                            isPubliMessage = true;
                            from = cpimMsg.getHeader(CpimMessage.HEADER_FROM);
                            if (logger.isActivated()) {
                                logger.info("msrpDataReceived: isPubliMessage is  "
                                        + isPubliMessage + " from" + from);
                            }
                        }
                    } catch (Exception e) {
                        isPubliMessage = true;
                    }

                    boolean isFToHTTP = ChatUtils.isFileTransferHttpType(contentType);
                    if (isFToHTTP) {
                        sendMsrpMessageDeliveryStatus(from, cpimMsgId,
                                ImdnDocument.DELIVERY_STATUS_DELIVERED);
                    } else if (dispositionNotification != null) {
                        if (dispositionNotification.contains(ImdnDocument.POSITIVE_DELIVERY)) {
                            if (this.isLargeMessageMode()) {
                                if (logger.isActivated()) {
                                    logger.info("msrpDataReceived: Standlone chat");
                                }
                                getImdnManager().sendMessageDeliveryStatusImmediately(
                                        getDialogPath().getRemoteParty(), cpimMsgId,
                                        ImdnDocument.DELIVERY_STATUS_DELIVERED,
                                        SipUtils.getRemoteInstanceID(getDialogPath().getInvite()));
                            } else {
                                // Positive delivery requested, send MSRP message with
                                // status "delivered"
                                sendMsrpMessageDeliveryStatus(from, cpimMsgId,
                                        ImdnDocument.DELIVERY_STATUS_DELIVERED);
                            }
                        }
                        if (dispositionNotification.contains(ImdnDocument.DISPLAY)) {
                            imdnDisplayedRequested = true;
                        }
                    }

                    // Analyze received message thanks to the MIME type
                    if (isFToHTTP) {
                        // File transfer over HTTP message
                        receiveHttpFileTransfer(number,
                                StringUtils.decodeUTF8(cpimMsg.getMessageContent()),
                                getDialogPath().getInvite(), cpimMsgId, pseudo);
                    } else if (ChatUtils.isTextPlainType(contentType)) {
                        logger.debug("Received123 Text");
                        // Text message
                        if (cpimMsg.getContentHeader(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                                != null) {
                            logger.debug("Received public encoding is BASE64 ");
                            String msg = new String(Base64.decodeBase64(cpimMsg.getMessageContent()
                                    .getBytes()));
                            receiveText(number, msg, cpimMsgId, imdnDisplayedRequested, date,
                                    pseudo);
                        } else {
                            receiveText(number,
                                    StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId,
                                    imdnDisplayedRequested, date, pseudo);
                        }
                    } else if (ChatUtils.isApplicationIsComposingType(contentType)) {
                        // Is composing event
                        receiveIsComposing(number, cpimMsg.getMessageContent().getBytes());
                    } else if (ChatUtils.isMessageImdnType(contentType)) {
                        // Delivery report
                        receiveMessageDeliveryStatus(number, cpimMsg.getMessageContent());
                    } else if (ChatUtils.isGeolocType(contentType)) {
                        logger.debug("Received123 Geoloc");
                        // Geoloc message
                        receiveGeoloc(number, StringUtils.decodeUTF8(cpimMsg.getMessageContent()),
                                cpimMsgId, imdnDisplayedRequested, date);
                    } else if (ChatUtils.isPublicChatType(contentType)) {
                        logger.debug("Received123 Publicchat");
                        if (ChatUtils.isPublicChatType(contentType)) {
                            logger.debug("Received123 contentType is Public chat");
                        }
                        // Geoloc message
                        if (cpimMsg.getContentHeader(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                                != null) {
                            logger.debug("Received public encoding is BASE64 ");
                            String msg = new String(Base64.decodeBase64(cpimMsg.getMessageContent()
                                    .getBytes()));
                            receivePublicMessage(from, msg, cpimMsgId, imdnDisplayedRequested,
                                    date, pseudo, isPubliMessage);
                        } else {
                            receivePublicMessage(from,
                                    StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId,
                                    imdnDisplayedRequested, date, pseudo, isPubliMessage);
                        }
                    } else if (ChatUtils.isCloudChatType(contentType)) {
                        logger.debug("Received123 cloud");
                        if (ChatUtils.isCloudChatType(contentType)) {
                            logger.debug("Received123 contentType is cloud chat");
                        }
                        if (cpimMsg.getContentHeader(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                                != null) {
                            logger.debug("Received cloud encoding is BASE64 ");
                            String msg = new String(Base64.decodeBase64(cpimMsg.getMessageContent()
                                    .getBytes()));
                            receiveCloudMessage(from, msg, cpimMsgId, imdnDisplayedRequested, date,
                                    pseudo, isPubliMessage);
                        } else {
                            // Geoloc message
                            receiveCloudMessage(from,
                                    StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId,
                                    imdnDisplayedRequested, date, pseudo, isPubliMessage);
                        }
                    } else if (ChatUtils.isEmoticonsChatType(contentType)) {
                        logger.debug("Received123 emoticons");
                        if (ChatUtils.isCloudChatType(contentType)) {
                            logger.debug("Received123 contentType is emoticons chat");
                        }
                        if (cpimMsg.getContentHeader(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                                != null) {
                            logger.debug("Received emoticons encoding is BASE64 ");
                            String msg = new String(Base64.decodeBase64(cpimMsg.getMessageContent()
                                    .getBytes()));
                            receiveEmoticonsMessage(from, msg, cpimMsgId, imdnDisplayedRequested,
                                    date, pseudo, isPubliMessage);
                        } else {
                            // Geoloc message
                            receiveEmoticonsMessage(from,
                                    StringUtils.decodeUTF8(cpimMsg.getMessageContent()), cpimMsgId,
                                    imdnDisplayedRequested, date, pseudo, isPubliMessage);
                        }
                    } else {
                        logger.debug("Received unknown content type");
                    }
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't parse the CPIM message", e);
                }
            }
        } else {
            // Not supported content
            if (logger.isActivated()) {
                logger.debug("Not supported content " + mimeType + " in chat session");
            }
        }
    }

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used by chat
    }

    /**
     * Data transfer in progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     */
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used by chat
        return false;
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        // Not used by chat
    }

    public int convertErrorStringToInt(String errorCode) {
        if (errorCode.contains("response timeout")) {
            return ImdnDocument.TIMEOUT;
        } else if (errorCode.contains("error report ")) {
            return ImdnDocument.INTERNAL_ERROR;
        }
        return ImdnDocument.UNKNOWN;
    }

    /**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
        if (isSessionInterrupted()) {
            if (logger.isActivated()) {
                logger.info("ABC Data transfer error " + error);
            }
            return;
        }

        if (logger.isActivated()) {
            logger.info("ABC Data transfer error " + error);
        }

        // Request capabilities
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getDialogPath().getRemoteParty());

        if (msgId != null) {

            int errorCode = convertErrorStringToInt(error);
            String statusCode = null;
            if (errorCode == 1) {
                try {
                    statusCode = error.substring(12);
                } catch (Exception e) {
                    logger.error("msrpTransferError ", e);
                }
            }
            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                ((ChatSessionListener) getListeners().get(i)).handleMessageDeliveryStatus(
                        this.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_FAILED,
                        errorCode, statusCode);
            }
        } else {
            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                ((ChatSessionListener) getListeners().get(i)).handleImError(new ChatError(
                        ChatError.MEDIA_SESSION_FAILED, error));
            }
        }
    }

    /**
     * Receive text message
     *
     * @param contact Contact
     * @param txt Text message
     * @param msgId Message Id
     * @param flag indicating that an IMDN "displayed" is requested for this message
     * @param date Date of the message
     */
    private void receiveText(String contact, String txt, String msgId,
            boolean imdnDisplayedRequested, Date date, String displayName) {
        if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
            if (logger.isActivated()) {
                logger.debug("receiveText Contact " + contact
                        + " is blocked, automatically reject the message");
            }
            return;
        }

        if (this instanceof GroupChatSession) {
            if (isMessageBlocked() == true) {
                if (logger.isActivated()) {
                    logger.debug("receiveText message sending is blocked, "
                            + "automatically reject the message txt:" + txt);
                }
                return;
            }
        }

        if (logger.isActivated()) {
            logger.debug("receiveText: Message not already received " + txt + "Display name"
                    + msgId);
        }

        // if burn message
        if (isBurnMessage()) {
            // Notify listeners
            if (logger.isActivated()) {
                logger.debug("receiveText12: " + txt + "Display name" + displayName);
            }

            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setBurnMessage(isBurnMessage());
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        } else {
            // if (!RichMessagingHistory.getInstance().isNewMessage(getContributionID(),
            // msgId)) {
            boolean p = false;
            if (p) {
                // Message already received
                if (logger.isActivated()) {
                    logger.debug("receiveText65: Message already received " + txt + "Display name"
                            + msgId);
                }
                // return;
            }

            if (logger.isActivated()) {
                logger.debug("receiveText46: " + txt + "Display name" + displayName);
            }

            // Is composing event is reset
            isComposingMgr.receiveIsComposingEvent(contact, false);

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                ((ChatSessionListener) getListeners().get(i))
                        .handleReceiveMessage(new InstantMessage(msgId, contact, txt,
                                imdnDisplayedRequested, date, displayName));
            }
        }

    }

    /**
     * Receive text message
     *
     * @param contact Contact
     * @param txt Text message
     * @param msgId Message Id
     * @param flag indicating that an IMDN "displayed" is requested for this message
     * @param date Date of the message
     */
    private void receivePublicMessage(
            String contact, String txt, String msgId,
            boolean imdnDisplayedRequested, Date date, String displayName,
            boolean isPublicMessage) {
        if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
            if (logger.isActivated()) {
                logger.debug("receivePublicMessage Contact " + contact
                        + " is blocked, automatically reject the message");
            }
            return;
        }

        if (date == null) {
            if (logger.isActivated()) {
                logger.debug("receivePublicMessage: date is null ");
                date = new Date();
                logger.debug("receivePublicMessage: date is  " + date);
            }
        }

        if (this instanceof GroupChatSession) {
            if (isMessageBlocked() == true) {
                if (logger.isActivated()) {
                    logger.debug("receiveText message sending is blocked, "
                            + "automatically reject the message txt:" + txt);
                }
                return;
            }
        }

        if (logger.isActivated()) {
            logger.debug("receivePublicMessage: Message not already received " + txt
                    + "Display name" + msgId);
        }

        // if burn message
        if (isBurnMessage()) {
            // Notify listeners
            if (logger.isActivated()) {
                logger.debug("receivePublicMessage34: " + txt + "Display name" + displayName);
            }

            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setBurnMessage(isBurnMessage());
                instMessage.setPublicMessage(isPublicMessage);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        } else {
            // if (!RichMessagingHistory.getInstance().isNewMessage(getContributionID(),
            // msgId)) {
            boolean p = false;
            if (p) {
                // Message already received
                if (logger.isActivated()) {
                    logger.debug("receivePublicMessage12: Message already received " + txt
                            + "Display name" + msgId);
                }
                // return;
            }

            if (logger.isActivated()) {
                // logger.debug("receivePublicMessage45: " + txt + "Display name" +
                // displayName);
            }

            // Is composing event is reset
            isComposingMgr.receiveIsComposingEvent(contact, false);

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setPublicMessage(isPublicMessage);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        }
    }

    /**
     * Receive text message
     *
     * @param contact Contact
     * @param txt Text message
     * @param msgId Message Id
     * @param flag indicating that an IMDN "displayed" is requested for this message
     * @param date Date of the message
     */
    private void receiveCloudMessage(
            String contact, String txt, String msgId,
            boolean imdnDisplayedRequested, Date date, String displayName,
            boolean isCloudMessage) {
        if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
            if (logger.isActivated()) {
                logger.debug("receiveCloudMessage Contact " + contact
                        + " is blocked, automatically reject the message");
            }
            return;
        }

        if (date == null) {
            if (logger.isActivated()) {
                logger.debug("receiveCloudMessage: date is null ");
                date = new Date();
                logger.debug("receiveCloudMessage: date is  " + date);
            }
        }

        if (this instanceof GroupChatSession) {
            if (isMessageBlocked() == true) {
                if (logger.isActivated()) {
                    logger.debug("receiveCloudMessage message sending is blocked, "
                            + "automatically reject the message txt:" + txt);
                }
                return;
            }
        }

        if (logger.isActivated()) {
            logger.debug("receiveCloudMessage: Message not already received " + txt
                    + "Display name" + msgId);
        }

        // if burn message
        if (isBurnMessage()) {
            // Notify listeners
            if (logger.isActivated()) {
                logger.debug("receiveCloudMessage34: " + txt + "Display name" + displayName);
            }

            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setBurnMessage(isBurnMessage());
                instMessage.setCloudMessage(isCloudMessage);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        } else {
            // if (!RichMessagingHistory.getInstance().isNewMessage(getContributionID(),
            // msgId)) {
            boolean p = false;
            if (p) {
                // Message already received
                if (logger.isActivated()) {
                    logger.debug("receiveCloudMessage12: Message already received " + txt
                            + "Display name" + msgId);
                }
                // return;
            }

            if (logger.isActivated()) {
                // logger.debug("receivePublicMessage45: " + txt + "Display name" +
                // displayName);
            }

            // Is composing event is reset
            isComposingMgr.receiveIsComposingEvent(contact, false);

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setCloudMessage(isCloudMessage);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        }
    }

    /**
     * Receive text message
     *
     * @param contact Contact
     * @param txt Text message
     * @param msgId Message Id
     * @param flag indicating that an IMDN "displayed" is requested for this message
     * @param date Date of the message
     */
    private void receiveEmoticonsMessage(String contact, String txt, String msgId,
            boolean imdnDisplayedRequested, Date date, String displayName,
            boolean isEmoticonsMessage) {
        if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
            if (logger.isActivated()) {
                logger.debug("receiveEmoticonsMessage Contact " + contact
                        + " is blocked, automatically reject the message");
            }
            return;
        }

        if (date == null) {
            if (logger.isActivated()) {
                logger.debug("receiveEmoticonsMessage: date is null ");
                date = new Date();
                logger.debug("receiveEmoticonsMessage: date is  " + date);
            }
        }

        if (this instanceof GroupChatSession) {
            if (isMessageBlocked() == true) {
                if (logger.isActivated()) {
                    logger.debug("receiveEmoticonsMessage message sending is blocked, "
                            + "automatically reject the message txt:" + txt);
                }
                return;
            }
        }

        if (logger.isActivated()) {
            logger.debug("receiveEmoticonsMessage: Message not already received " + txt
                    + "Display name" + msgId);
        }

        // if burn message
        if (isBurnMessage()) {
            // Notify listeners
            if (logger.isActivated()) {
                logger.debug("receiveEmoticonsMessage34: " + txt + "Display name" + displayName);
            }

            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setBurnMessage(isBurnMessage());
                instMessage.setEmoticonMessage(true);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        } else {
            // if (!RichMessagingHistory.getInstance().isNewMessage(getContributionID(),
            // msgId)) {
            boolean p = false;
            if (p) {
                // Message already received
                if (logger.isActivated()) {
                    logger.debug("receiveEmoticonsMessage12: Message already received " + txt
                            + "Display name" + msgId);
                }
                // return;
            }

            if (logger.isActivated()) {
                // logger.debug("receivePublicMessage45: " + txt + "Display name" +
                // displayName);
            }

            // Is composing event is reset
            isComposingMgr.receiveIsComposingEvent(contact, false);

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                InstantMessage instMessage = new InstantMessage(msgId, contact, txt,
                        imdnDisplayedRequested, date, displayName);
                instMessage.setEmoticonMessage(true);
                ((ChatSessionListener) getListeners().get(i)).handleReceiveMessage(instMessage);
            }
        }
    }

    /**
     * Receive is composing event
     *
     * @param contact Contact
     * @param event Event
     */
    private void receiveIsComposing(String contact, byte[] event) {
        if (ContactsManager.getInstance().isImBlockedForContact(contact)) {
            if (logger.isActivated()) {
                logger.debug("receiveIsComposing Contact " + contact
                        + " is blocked, automatically reject the message");
            }
            return;
        }
        isComposingMgr.receiveIsComposingEvent(contact, event);
    }

    /**
     * Send an empty data chunk
     */
    public void sendEmptyDataChunk() {
        try {
            msrpMgr.sendEmptyChunk();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Problem while sending empty data chunk", e);
            }
        }
    }

    /**
     * Receive geoloc event
     *
     * @param contact Contact
     * @param geolocDoc Geoloc document
     * @param msgId Message Id
     * @param flag Flag indicating that an IMDN "displayed" is requested for this message
     * @param date Date of the message
     */
    private void receiveGeoloc(String contact, String geolocDoc, String msgId,
            boolean imdnDisplayedRequested, Date date) {
        if (!RichMessagingHistory.getInstance().isNewMessage(getContributionID(), msgId)) {
            // Message already received
            return;
        }

        // Is composing event is reset
        isComposingMgr.receiveIsComposingEvent(contact, false);

        GeolocMessage geolocMsg = null;
        try {
            GeolocPush geoloc = ChatUtils.parseGeolocDocument(geolocDoc);
            if (geoloc != null) {
                geolocMsg = new GeolocMessage(msgId, contact, geoloc,
                        imdnDisplayedRequested, date);
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't parse received geolocation", e);
            }
            return;
        }

        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i)).handleReceiveGeoloc(geolocMsg);
        }
    }

    /**
     * Receive HTTP file transfer event
     *
     * @param fileInfo File info in XML
     * @param invite Incoming request
     * @param msgId Message ID
     */
    private void receiveHttpFileTransfer(String number, String fileInfo, SipRequest invite,
            String msgId, String displayName) {
        // Parse HTTP document
        FileTransferHttpInfoDocument fileTransferInfo = ChatUtils
                .parseFileTransferHttpDocument(fileInfo.getBytes());
        if (fileTransferInfo != null) {

            if (logger.isActivated()) {
                logger.info("receiveHttpFileTransfer thumbnail "
                        + fileTransferInfo.getFileThumbnail() + "Number is: " + number);
            }

            // Test if the contact is blocked
            if (ContactsManager.getInstance().isFtBlockedForContact(number)) {
                if (logger.isActivated()) {
                    logger.debug("Contact " + getRemoteContact()
                            + " is blocked, automatically reject the HTTP File transfer");
                }

                // TODO : reject (SIP MESSAGE ?)
                return;
            }

            // Auto reject if file too big
            int maxSize = FileSharingSession.getMaxFileSharingSize();
            if (maxSize > 0 && fileTransferInfo.getFileSize() > maxSize) {
                if (logger.isActivated()) {
                    logger.debug("File is too big, reject the HTTP File transfer");
                }

                // TODO : reject (SIP MESSAGE ?)
                return;
            }

            // Auto reject if number max of FT reached
            maxSize = RcsSettings.getInstance().getMaxFileTransferSessions();
            if (maxSize > 0
                    && getImsService().getImsModule().getInstantMessagingService()
                            .getFileTransferSessions().size() > maxSize) {
                if (logger.isActivated()) {
                    logger.debug("Max number of File Tranfer reached,"
                            + " rejecting the HTTP File transfer");
                }

                // TODO : reject (SIP MESSAGE ?)
                return;
            }

            // Create a new session
            FileSharingSession session = new TerminatingHttpFileSharingSession(
                    getImsService(), this, fileTransferInfo, msgId, displayName, number);

            // Start the session
            session.startSession();

            if (isGroupChat()) {
                if (logger.isActivated()) {
                    logger.debug("receiveHttpFileTransfer It is group chat");
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("receiveHttpFileTransfer it is not group chat");
                }
            }

            // Notify listener
            getImsService().getImsModule().getCoreListener()
                    .handleHttpFileTransferInvitation(number, session, isGroupChat());
        }
        // TODO : else return error to Originating side
    }

    /**
     * Send data chunk with a specified MIME type
     *
     * @param msgId Message ID
     * @param data Data
     * @param mime MIME type
     * @return Boolean result
     */
    public boolean sendDataChunks(String msgId, String data, String mime) {
        try {
            if (logger.isActivated()) {
                logger.error("sendDataChunks: " + data);
            }
            ByteArrayInputStream stream = new ByteArrayInputStream(data.getBytes());
            msrpMgr.sendChunks(stream, msgId, mime, data.getBytes().length);
            return true;
        } catch (Exception e) {
            // Error
            if (logger.isActivated()) {
                logger.error("Problem while sending data chunks", e);
            }
            return false;
        }
    }

    /**
     * Is group chat
     *
     * @return Boolean
     */
    public abstract boolean isGroupChat();

    /**
     * Is Store & Forward
     *
     * @return Boolean
     */
    public boolean isStoreAndForward() {
        if (this instanceof TerminatingStoreAndForwardMsgSession
                || this instanceof TerminatingStoreAndForwardNotifSession) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Send a GeoLoc message
     *
     * @param msgId Message-ID
     * @param geoloc GeoLocation
     * @return Boolean result
     */
    public abstract void sendGeolocMessage(String msgId, GeolocPush geoloc);

    /**
     * Send a text message
     *
     * @param msgId Message-ID
     * @param txt Text message
     * @return Boolean result
     */
    public abstract void sendTextMessage(String msgId, String txt);

    /**
     * Send is composing status
     *
     * @param status Status
     */
    public abstract void sendIsComposingStatus(boolean status);

    /**
     * Send message delivery status via MSRP
     *
     * @param contact Contact that requested the delivery status
     * @param msgId Message ID
     * @param status Status
     */
    public void sendMsrpMessageDeliveryStatus(String contact, String msgId, String status) {
        // Send status in CPIM + IMDN headers
        String from = ChatUtils.ANOMYNOUS_URI;
        String to = ChatUtils.ANOMYNOUS_URI;
        String imdn = ChatUtils.buildDeliveryReport(msgId, status);
        String content = ChatUtils.buildCpimDeliveryReport(from, to, imdn);

        // Send data
        boolean result = sendDataChunks(ChatUtils.generateMessageId(), content,
                CpimMessage.MIME_TYPE);
        if (result) {
            // Update rich messaging history
            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        }
    }

    /**
     * Send message delivery status via MSRP
     *
     * @param contact Contact that requested the delivery status
     * @param msgId Message ID
     * @param status Status
     */
    public void sendMsrpMessageBurnStatus(String contact, String msgId, String status) {
        // Send status in CPIM + IMDN headers
        String from = ChatUtils.ANOMYNOUS_URI;
        String to = ChatUtils.ANOMYNOUS_URI;
        String imdn = ChatUtils.buildDeliveryReport(msgId, status);
        String content = ChatUtils.buildCpimDeliveryReport(from, to, imdn);

        // Send data
        boolean result = sendDataChunks(ChatUtils.generateMessageId(), content,
                CpimMessage.MIME_TYPE);
        if (result) {
            // Update rich messaging history
            // RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId,
            // status);
        }
    }

    /**
     * Handle a message delivery status from a SIP message
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact, String msgId, String status) {
        if (logger.isActivated()) {
            logger.debug("ABCG handleMessageDeliveryStatus contact: " + contact);
        }

        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i)).handleMessageDeliveryStatus(
                    contact, msgId, status);
        }
    }

    /**
     **
     * Handle a message delivery status from a SIP message
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(
            String contact, String msgId, String status, int errorCode, String statusCode) {
        if (logger.isActivated()) {
            logger.debug("ABCG handleMessageDeliveryStatus contact: " + contact);
        }

        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i)).handleMessageDeliveryStatus(
                    contact, msgId, status, errorCode, statusCode);
        }
    }

    public void sendMsrpMessageDeliveryStatus(
            String contact, String msgId, String status, String failedReport) {
        // Send status in CPIM + IMDN headers
        String from = ChatUtils.ANOMYNOUS_URI;
        String to = ChatUtils.ANOMYNOUS_URI;
        String imdn = ChatUtils.buildDeliveryReport(msgId, status);
        String content = ChatUtils.buildCpimDeliveryReport(from, to, imdn, failedReport);

        // Send data
        boolean result = sendDataChunks(ChatUtils.generateMessageId(), content,
                CpimMessage.MIME_TYPE);
        if (result) {
            // Update rich messaging history
            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        }
    }

    /**
     * Handle a message delivery status from a SIP message
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String msgId, String status) {
        // Notify listeners
        for (int i = 0; i < getListeners().size(); i++) {
            ((ChatSessionListener) getListeners().get(i))
                    .handleMessageDeliveryStatus(msgId, status);
        }
    }

    /**
     * Receive a message delivery status from an XML document
     *
     * @param xml XML document
     */
    public void receiveMessageDeliveryStatus(String contact, String xml) {
        try {
            if (logger.isActivated()) {
                logger.info("receiveMessageDeliveryStatus def contac: " + contact);
            }
            ImdnDocument imdn = ChatUtils.parseDeliveryReport(xml);
            if ((imdn != null) && (imdn.getMsgId() != null) && (imdn.getStatus() != null)) {
                // Check if message delivery of a FileTransfer
                String ftSessionId = RichMessagingHistory.getInstance().getFileTransferId(
                        imdn.getMsgId());
                if (ftSessionId == null) {
                    // Notify listeners
                    for (int i = 0; i < getListeners().size(); i++) {
                        ((ChatSessionListener) getListeners().get(i)).handleMessageDeliveryStatus(
                                contact, imdn.getMsgId(), imdn.getStatus());
                    }
                } else {
                    if (isGroupChat()) {
                        ((InstantMessagingService) getImsService()).receiveFileDeliveryStatus(
                                ftSessionId, imdn.getStatus(), contact);
                    } else {
                        ((InstantMessagingService) getImsService()).receiveFileDeliveryStatus(
                                ftSessionId, imdn.getStatus(), contact);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't parse IMDN document", e);
            }
        }
    }

    /**
     * Get max number of participants in the session including the initiator
     * 
     * @return Integer
     */
    public int getMaxParticipants() {
        return maxParticipants;
    }

    /**
     * Set max number of participants in the session including the initiator
     *
     * @param maxParticipants Max number
     */
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    /**
     * Prepare media session
     *
     * @throws Exception
     */
    public void prepareMediaSession() throws Exception {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        MediaAttribute attr = mediaDesc.getMediaAttribute("path");
        String remoteMsrpPath = attr.getValue();
        String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
        int remotePort = mediaDesc.port;

        // Create the MSRP session
        MsrpSession session = getMsrpMgr().createMsrpClientSession(
                remoteHost, remotePort, remoteMsrpPath, this);
        if (LauncherUtils.supportOP01()) {
            session.setFailureReportOption(true);
        } else {
            session.setFailureReportOption(false);
        }
        session.setSuccessReportOption(false);

        // Open the MSRP session
        getMsrpMgr().openMsrpSession();

        // Send an empty packet
        sendEmptyDataChunk();
    }

    /**
     * Start media session
     *
     * @throws Exception
     */
    public void startMediaSession() throws Exception {
        // Nothing to do
    }

    /**
     * Chat inactivity event
     */
    public void handleChatInactivityEvent() {
        if (logger.isActivated()) {
            logger.info("Chat inactivity event");
        }

        // Abort the session
        abortSession(ImsServiceSession.TERMINATION_BY_TIMEOUT);
    }

    /**
     * Handle 200 0K response
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        super.handle200OK(resp);

        // Check if geolocation push supported by remote
        setGeolocSupportedByRemote(SipUtils.isFeatureTagPresent(
                resp, FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH));

        // Check if file transfer supported by remote
        setFileTransferSupportedByRemote(SipUtils.isFeatureTagPresent(resp,
                FeatureTags.FEATURE_RCSE_FT)
                || SipUtils.isFeatureTagPresent(resp, FeatureTags.FEATURE_RCSE_FT_HTTP));
    }

    public boolean isBurnMessage() {
        return isBurnMessage;
    }

    public void setBurnMessage(boolean burnFlag) {
        isBurnMessage = burnFlag;
    }

    public boolean isPublicAccountMessage() {
        return isPublicAccountMessage;
    }

    public void setPublicAccountMessage(boolean publicAccountFlag) {
        isPublicAccountMessage = publicAccountFlag;
    }
}
