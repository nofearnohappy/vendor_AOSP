package com.orangelabs.rcs.service.api;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.ISpamReportListener;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteCallbackList;
import android.text.TextUtils;
import android.util.Base64;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingOne2OneStandAloneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;

import gov2.nist.javax2.sip.header.SIPHeader;

/**
* Chat implementation
*
 * @author Jean-Marc AUFFRET
*/
public class ChatImpl extends IChat.Stub implements ChatSessionListener {
    /**
     * Remote contact
     */
    private String contact;

    private final static String BOUNDARY_TAG = "jXfWUFcrCxZEXdN";

    private final static String SPAM_REPORT_SERVER_NUMBER = "+86100869999";


    private ImsService imsService;
    /**
     * Core session
     */
    private OneOneChatSession session;

    /**
     * List of listeners
     */
    private RemoteCallbackList<IChatListener> listeners = new RemoteCallbackList<IChatListener>();

    /**
     * List of listeners
     */
    private RemoteCallbackList<ISpamReportListener> spamListeners =
            new RemoteCallbackList<ISpamReportListener>();

    /**
     * Lock used for synchronisation
     */
    private Object lock = new Object();

    private CopyOnWriteArrayList<InstantMessage> mPendingMessage =
        new CopyOnWriteArrayList<InstantMessage>();
    
    private final CopyOnWriteArrayList<String> mPublicMessages = new CopyOnWriteArrayList<String>();
    private final CopyOnWriteArrayList<String> mCloudMessages = new CopyOnWriteArrayList<String>();
    
    String pendingMessage = null;
    String pendingMessageId = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param contact Remote contact
     */
    public ChatImpl(String contact) {
        this.contact = contact;
        this.session = null;
        imsService = Core.getInstance().getImService();
    }

    public String getChatId(){
        return session.getRemoteContact();
    }

    /**
     * Constructor
     *
     * @param contact Remote contact
     * @param session Session
     */
    public ChatImpl(String contact, OneOneChatSession session) {
        this.contact = contact;
        this.session = session;

        session.addListener(this);
    }

    /**
     * Set core session
     *
     * @param session Core session
     */
    public void setCoreSession(OneOneChatSession session) {
        this.session = session;

        session.addListener(this);
    }

    /**
     * Reset core session
     */
    public void resetCoreSession() {
        this.session = null;
    }

    /**
     * Get core session
     *
     * @return Core session
     */
    public OneOneChatSession getCoreSession() {
        return session;
    }

    /**
     * Returns the state of the group chat message
     *
     * @return State of the message
     * @see GroupChat.MessageState
     */
    public int getState(String messageId) {
        //int messageStatus = RichMessagingHistory.getInstance().getMessageStatus(messageId);
        int messageStatus = 0;
        switch(messageStatus){
            case ChatLog.Message.Status.Content.SENDING:
                return Chat.MessageState.SENDING;

            case ChatLog.Message.Status.Content.SENT:
                return Chat.MessageState.SENT;

            case ChatLog.Message.Status.Content.UNREAD_REPORT:
            case ChatLog.Message.Status.Content.UNREAD:
            case ChatLog.Message.Status.Content.READ:
                return Chat.MessageState.DELIVERED;

            case ChatLog.Message.Status.Content.FAILED:
                return Chat.MessageState.FAILED;

            default:
                return Chat.MessageState.FAILED;
        }
    }

    /**
     * Returns the remote contact
     *
     * @return Contact
     */
    public String getRemoteContact() {
        return PhoneUtils.extractNumberFromUri(contact);
    }

    /**
     * Sends a plain text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendMessage(String message) {
        if (logger.isActivated()) {
            logger.debug("Send text message:" + message);
        }

        // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        // Send message
        return sendChatMessage(msg);
    }

    public String sendMessageByPagerMode(
            String message, boolean isBurnMessage,
            boolean isPublicMessage, boolean isMultiMessage,
            boolean isPayEmoticon, List<String>  participants) {
        return sendMessageByPagerMode(message, isBurnMessage,
                isPublicMessage, isMultiMessage, isPayEmoticon,null,  participants);
    }

    public String sendCloudMessage(String message){
        return sendMessageByPagerMode( message,  false, false,  false, false,true, null, null);
    }

    public String sendMessageByPagerMode(
            String message, boolean isBurnMessage,
            boolean isPublicMessage, boolean isMultiMessage,
            boolean isPayEmoticon, String resendMsgId, List<String>  participants) {
        return sendMessageByPagerMode(message, isBurnMessage,
                isPublicMessage,  isMultiMessage, isPayEmoticon,false, resendMsgId, participants);
    }


    public String sendMessageByPagerMode(
            String message, boolean isBurnMessage,
            boolean isPublicMessage, boolean isMultiMessage,
            boolean isPayEmoticon,boolean isCloudMsg,
            String resendMsgId, List<String>  participants) {
        if (logger.isActivated()) {
            logger.debug("sendMessageByPagerMode Burn:" +
                    isBurnMessage + "Public:" + isPublicMessage + " MultiMessage "
                    + isMultiMessage + "emoticon" + isPayEmoticon);
        }

        if (isMultiMessage == true) {
            return sendMultiMessageByPagerMode(message,participants);
        }

        String mime = null;
        String featureTags = null;
        if (isCloudMsg == true) {
            mime = CpimMessage.MIME_TYPE_CLOUDMESSAGE;
            featureTags = FeatureTags.FEATURE_CMCC_CLOUD_FILE;
        }
        else if (isPayEmoticon == true) {
            mime = CpimMessage.MIME_TYPE_PAYEMOTICON;
            featureTags = FeatureTags.FEATURE_CMCC_PAY_EMOTICON;
        }
        else if (isPublicMessage == true) {
            // assuming its a message
            mime = CpimMessage.MIME_TYPE;
            featureTags = FeatureTags.FEATURE_CMCC_PUBLIC_ACCOUNT_MSG;
        } else {
            mime = CpimMessage.MIME_TYPE;
            featureTags = FeatureTags.FEATURE_RCSE_PAGER_MSG ;
        }

        // Create a text message
        InstantMessage msgg = ChatUtils.createTextMessage(
                contact,
                message, Core.getInstance().getImService().getImdnManager().isImdnActivated());
        if (resendMsgId != null) {
            msgg.setMessageId(resendMsgId);
            msgg.setBurnMessage(RichMessagingHistory.getInstance().isBurnMessage(resendMsgId));
            msgg.setPublicMessage(RichMessagingHistory.getInstance().isPublicMessage(resendMsgId));
            msgg.setCloudMessage(RichMessagingHistory.getInstance().isCloudMessage(resendMsgId));
            msgg.setEmoticonMessage(RichMessagingHistory.getInstance().isEmoticonMessage(resendMsgId));
        }

        String content;
        if (isPayEmoticon == true){
            content = message; // app will pass the emoticon xml
        } else {//normal &burn  multipart
            String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
            String to = ChatUtils.ANOMYNOUS_URI;

            if (RcsSettings.getInstance().isImReportsActivated()) {
                // Send message in CPIM + IMDN
                if (logger.isActivated()) {
                    logger.debug("Send text236 message:" + message);
                }
                try{
                    if (logger.isActivated()) {
                        logger.debug("Send text237 message:" +
                                Base64.encodeToString(message.getBytes(), Base64.NO_WRAP));
                    }
                    if (logger.isActivated()) {
                        logger.debug("Send text238 message:" + StringUtils.encodeUTF8(message));
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                content = ChatUtils.buildCpimMessageWithImdn(
                        from, to,
                        msgg.getMessageId(),
                        StringUtils.encodeUTF8(message),
                        InstantMessage.MIME_TYPE);
            } else {
                // Send message in CPIM
                if (logger.isActivated()) {
                    logger.debug("Send text236 message:" + message);
                }
                try{
                    if (logger.isActivated()) {
                        logger.debug("Send text236 message:" +
                                Base64.encodeToString(message.getBytes(), Base64.NO_WRAP));
                    }
                }catch(Exception e){
                }
                content = ChatUtils.buildCpimMessage(from, to, StringUtils.encodeUTF8(msgg.getMessageId()),
                        InstantMessage.MIME_TYPE);
            }
        }

        if (resendMsgId == null) {
            if (isBurnMessage) {
                msgg.setBurnMessage(isBurnMessage);
                RichMessagingHistory.getInstance().addBurnChatMessage(
                        msgg, ChatLog.Message.Direction.OUTGOING);
            } else if (isPublicMessage) {
                msgg.setPublicMessage(isPublicMessage);
                RichMessagingHistory.getInstance().addPublicChatMessage(
                        msgg, ChatLog.Message.Direction.OUTGOING);
            } else if (isCloudMsg) {
                msgg.setCloudMessage(isCloudMsg);
                RichMessagingHistory.getInstance().addCloudMessage(
                        msgg, ChatLog.Message.Direction.OUTGOING);
            } else if (isPayEmoticon) {
                msgg.setEmoticonMessage(isPayEmoticon);
                RichMessagingHistory.getInstance().addEmoticonMessage(
                        msgg, ChatLog.Message.Direction.OUTGOING);
            } else {
                RichMessagingHistory.getInstance().addChatMessage(
                        msgg, ChatLog.Message.Direction.OUTGOING);
            }
        }



        try {
            SipDialogPath dialogPath = null;
            if (isPublicMessage) {
                // Create a dialog path
                //donot make
                 dialogPath = new SipDialogPath(
                         imsService.getImsModule().getSipManager().getSipStack(),
                         imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
                         1,
                         contact,
                         ImsModule.IMS_USER_PROFILE.getPublicUri(),
                         contact,
                         imsService.getImsModule().getSipManager().getSipStack()
                             .getServiceRoutePath());
            }else{
                // Create a dialog path
                dialogPath = new SipDialogPath(
                        imsService.getImsModule().getSipManager().getSipStack(),
                        imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
                        1,
                        PhoneUtils.formatNumberToSipUri(contact),
                        ImsModule.IMS_USER_PROFILE.getPublicUri(),
                        PhoneUtils.formatNumberToSipUri(contact),
                        imsService.getImsModule().getSipManager().getSipStack()
                            .getServiceRoutePath());
           }

            SipRequest request = SipMessageFactory.createMessageCPM(
                    dialogPath, featureTags, mime, content);

            // Allow header
            SipUtils.buildAllowHeader(request.getStackMessage());

            // Set contribution Id
            String contributionId = ContributionIdGenerator.getContributionId(
                        dialogPath.getCallId());
            request.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, contributionId);

            // set Conversation Id
            String conversationId = (RichMessagingHistory.getInstance().getCoversationID(
                    this.getRemoteContact(), 1));
            if (conversationId.equals("")) {
                // Set Call-Id
                String callId = imsService.getImsModule().getSipManager().getSipStack()
                        .generateCallId();
                conversationId = ContributionIdGenerator.getContributionId(callId);
            }
            request.addHeader(ChatUtils.HEADER_CONVERSATION_ID, conversationId);

            if (isPublicMessage) {
                String headerString = ((SIPHeader)request.getHeader(
                        SipUtils.HEADER_ACCEPT_CONTACT)).getHeaderValue();
                headerString += ";" + FeatureTags.FEATURE_CMCC_PUBLIC_ACCOUNT;
                Header acceptContactHeader;
                acceptContactHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_ACCEPT_CONTACT, headerString);
                request.getStackMessage().setHeader(acceptContactHeader);
            }

            if (isBurnMessage) {
                logger.debug("isBurnMessage() : " + isBurnMessage);
                try {
                    logger.debug("updating header : " + SipUtils.HEADER_ACCEPT_CONTACT);
                    ExtensionHeader oldHeader = (ExtensionHeader)request.getHeader(
                            SipUtils.HEADER_ACCEPT_CONTACT);
                    Header header;
                    if (oldHeader != null) {
                        String txtValue = oldHeader.getValue();
                        logger.debug("old value : " + featureTags);
                        txtValue += ";";
                        txtValue += FeatureTags.FEATURE_RCSE_CPM_BURN_AFTER_READING_FEATURE_TAG;
                        header = SipUtils.HEADER_FACTORY.createHeader(
                                    SipUtils.HEADER_ACCEPT_CONTACT, txtValue);
                    } else {
                        header = SipUtils.HEADER_FACTORY.createHeader(
                                    SipUtils.HEADER_ACCEPT_CONTACT,
                                    FeatureTags.FEATURE_RCSE_CPM_BURN_AFTER_READING_FEATURE_TAG);
                    }
                    request.getStackMessage().setHeader(header);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // Send MESSAGE request
            final SipRequest req = request;
            final InstantMessage msg = msgg;

            Thread t = new Thread() {
                public void run() {

                    SipTransactionContext ctx = null;
                    String number = msg.getRemote();
                    try {
                        ctx = imsService.getImsModule().getSipManager().sendSipMessageAndWait(req);
                        // Analyze received message
                        if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                            // 200 OK received
                            if (logger.isActivated()) {
                                logger.info("20x OK response received");
                            }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(),ImdnDocument.DELIVERY_STATUS_SENT);
                        }  else {
                            // Error responses
                            if (logger.isActivated()) {
                                logger.info("failed: " + ctx.getStatusCode() + " response received");
                            }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                        }
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.info("exception while sending pager message");
                        }
                        handleMessageDeliveryStatus(
                                number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                    }
                }
            };
            t.start();

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send Pager Mode MESSAGE request", e);
            }
            handleMessageDeliveryStatus(
                    msgg.getRemote(), msgg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
        }

        return msgg.getMessageId();
    }


    public void sendSpamMessageByPagerMode(String contact, final String messageId) {
        if (logger.isActivated()) {
            logger.info("sendSpamMessageByPagerMode messageId : + messageId");
        }
        contact = SPAM_REPORT_SERVER_NUMBER;
        String content = null;
        String mime = InstantMessage.MIME_TYPE;
        String badUser ="";
        String featureTags = FeatureTags.FEATURE_RCSE_PAGER_MSG;
        Cursor cursor = null;
        int emptyCursor = 0;
        long timeStamp = System.currentTimeMillis();
        String text = null;

        String[] selectionArg = { messageId };
        ContentResolver contentResolver = AndroidFactory
                .getApplicationContext().getContentResolver();
        try {
            cursor = contentResolver.query(ChatLog.Message.CONTENT_URI, null,
                    ChatLog.Message.MESSAGE_ID + "=?", selectionArg, null);
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
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        //Building spamInfo here
        String to = ImsModule.IMS_USER_PROFILE.getPreferredUri();
        String from;
        if (to.startsWith("tel:")) {
            from = "<tel:" + badUser + ">";
        } else {
            from = "<sip:"
                    + badUser
                    + "@"
                    + ImsModule.IMS_USER_PROFILE.getPreferredUri().substring(
                            to.indexOf("@") + 1) + ">";
        }
        to = "<" + to + ">";
        logger.info("Spam Session createSipInvite from :" + from + " to: " + to);

        String spamsdpinfo;
        // Retreive Data from databse to report to server

        String localTime = DateUtils.encodeDate(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        localTime = sdf.format(new Date(System.currentTimeMillis()));
        spamsdpinfo = "Spam-From:" + from + SipUtils.CRLF + "Spam-To:" + to
                + SipUtils.CRLF + "DateTime:" + localTime;

        //Creating CPIM Message here
        text = StringUtils.encodeUTF8(text);
        int contentLength = text.getBytes().length;
        if (RcsSettings.getInstance().isBase64EncodingSupported()) {
            text = Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);
            contentLength = text.getBytes().length;
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        localTime = sdf.format(new Date(timeStamp));
        String cpim =
                CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + SipUtils.CRLF
                + CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + SipUtils.CRLF
                + CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + SipUtils.CRLF
                + ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + SipUtils.CRLF
                + CpimMessage.HEADER_DATETIME + ": " + localTime + SipUtils.CRLF
                + ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.DISPLAY + SipUtils.CRLF
                + SipUtils.CRLF
                + CpimMessage.HEADER_CONTENT_TYPE + ": " + mime + ";charset=utf-8" + SipUtils.CRLF
                + CpimMessage.HEADER_CONTENT_LENGTH + ": " + contentLength + SipUtils.CRLF;

        if (RcsSettings.getInstance().isBase64EncodingSupported()) {
            byte[] c = text.getBytes(Charset.forName("UTF-8"));
            cpim = cpim + SipUtils.HEADER_CONTENT_TRANSFER_ENCODING + ": "
                    + "base64" + SipUtils.CRLF + SipUtils.CRLF + text;
        } else {
            cpim = cpim + SipUtils.CRLF + text;
        }

        // Building multipart Cpim message here

        content = Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type: text/plain;charset=UTF-8" + SipUtils.CRLF
                + SipUtils.CRLF + spamsdpinfo + SipUtils.CRLF + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type: " + CpimMessage.MIME_TYPE + SipUtils.CRLF +
                /* "Content-Length: "+ cpim.getBytes().length + SipUtils.CRLF + */
                SipUtils.CRLF + cpim + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG
                + Multipart.BOUNDARY_DELIMITER;

        if (RcsSettings.getInstance().isImReportsActivated()) {
            //content = ChatUtils.buildCpimMessageForSpam(from, to, messageId,
            //content, mime, timeStamp);

        } else {
            content = ChatUtils.buildCpimMessageForSpamNoIMDN(from, to,
                    content, mime);
        }

        try {
            SipDialogPath dialogPath = null;

            // Create a dialog path
            dialogPath = new SipDialogPath(
                    imsService.getImsModule().getSipManager().getSipStack(),
                    imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
                    1,
                    PhoneUtils.formatNumberToSipUri(SPAM_REPORT_SERVER_NUMBER),
                    ImsModule.IMS_USER_PROFILE.getPublicUri(),
                    PhoneUtils.formatNumberToSipUri(SPAM_REPORT_SERVER_NUMBER),
                    imsService.getImsModule().getSipManager().getSipStack()
                        .getServiceRoutePath());

            SipRequest request = SipMessageFactory.createMessageCPM(dialogPath,
                    featureTags, mime, content);

            // Allow header
            SipUtils.buildAllowHeader(request.getStackMessage());

            // Set contribution Id
            String contributionId = ContributionIdGenerator
                    .getContributionId(dialogPath.getCallId());
            request.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, contributionId);

            // set Conversation Id
            String conversationId = (RichMessagingHistory.getInstance()
                    .getCoversationID(this.getRemoteContact(), 1));
            if (conversationId.equals("")) {
                // Set Call-Id
                String callId = imsService.getImsModule().getSipManager()
                        .getSipStack().generateCallId();
                conversationId = ContributionIdGenerator
                        .getContributionId(callId);
            }
            request.addHeader(ChatUtils.HEADER_CONVERSATION_ID, conversationId);

            // Send MESSAGE request

            final SipRequest req = request;

            Thread t = new Thread() {
                public void run() {
                    SipTransactionContext ctx = null;
                    try {
                        ctx = imsService.getImsModule().getSipManager()
                                .sendSipMessageAndWait(req);
                        // Analyze received message
                        if ((ctx.getStatusCode() == 200)
                                || (ctx.getStatusCode() == 202)) {
                            // 200 OK received
                            if (logger.isActivated()) {
                                logger.info("20x OK response received");
                            }
                            handleSpamReportStatus(
                                    SPAM_REPORT_SERVER_NUMBER,
                                    messageId,
                                    ImdnDocument.DELIVERY_STATUS_DELIVERED,
                                    200);

                        } else {
                            // Error responses
                            if (logger.isActivated()) {
                                logger.info("failed: " + ctx.getStatusCode()
                                        + " response received");
                            }
                            handleSpamReportStatus(
                                    SPAM_REPORT_SERVER_NUMBER,
                                    messageId,
                                    ImdnDocument.DELIVERY_STATUS_ERROR,
                                    ctx.getStatusCode() );
                        }
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.info("exception while sending spam pager message");
                        }
                        // callback("failed)";
                    }
                }
            };
            t.start();

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send spam pager Mode MESSAGE request", e);
            }

            // callback("failed)";
        }

        return;
    }

    /* this should be used by application */
    public String sendOnetoMultiMessage(String message,  List<String> participants) {
        if(message.length()<1300)//TODO use configuration , add in chat configuration
        {
            return sendMultiMessageByPagerMode(message , participants);
        }
        return null;
    }

    /* this should be used by application */
    public String sendOnetoMultiEmoticonsMessage(String message,  List<String> participants) {
        if(message.length()<1300)//TODO use configuration , add in chat configuration
        {
            return sendMultiMessagesByPagerMode(message , null,participants,"emoticons");
        }
        return null;
    }

    /* this should be used by application */
    public String sendOnetoMultiCloudMessage(String message,  List<String> participants) {
        if(message.length()<1300)//TODO use configuration , add in chat configuration
        {
            return sendMultiMessagesByPagerMode(message , null,participants,"cloud");
        }
        return null;
    }

    /* this should be used by application */
    public String sendEmoticonShopMessage(String message) {
        if(message.length()<900)//TODO use configuration , add in chat configuration
        {
            return  sendMessageByPagerMode(message,false,false,false,true,null);
        }
        return null;
    }

    public int reSendMultiMessageByPagerMode(String messageID) {
        sendMultiMessageByPagerMode(
                RichMessagingHistory.getInstance().getMessageText(messageID),
                messageID,
                RichMessagingHistory.getInstance().getMultiMessageParticipants(messageID));
        return getState(messageID);
    }

    public String sendMultiMessageByPagerMode(String message, List<String> participants) {
        return sendMultiMessageByPagerMode( message, null, participants);
    }

    public String sendMultiMessageByPagerMode(
            String message,String msgId, List<String> participants) {
        if (logger.isActivated()) {
            logger.debug("sendMultiMessageByPagerMode");
        }

        String mime = CpimMessage.MIME_TYPE_MULTIMESSAGE ;
        String featureTags = FeatureTags.FEATURE_RCSE_PAGER_MSG;

        // Create a text message
        InstantMessage msgg = ChatUtils.createTextMessage(
                contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        if(msgId!=null) {
            msgg.setMessageId(msgId);
            msgg.setBurnMessage(RichMessagingHistory.getInstance().isBurnMessage(msgId));
            msgg.setPublicMessage(RichMessagingHistory.getInstance().isPublicMessage(msgId));
        }

        String content;

        String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
        String to = ChatUtils.ANOMYNOUS_URI;//ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri();

        String resourceList = ChatUtils.generateMultiChatResourceList(participants);

        content = Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type: application/resource-lists+xml" + SipUtils.CRLF
                + "Content-Disposition: recipient-list "
                + SipUtils.CRLF + SipUtils.CRLF
                + resourceList + SipUtils.CRLF
                + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type:"+ CpimMessage.MIME_TYPE + SipUtils.CRLF
                + SipUtils.CRLF
                + CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + SipUtils.CRLF
                + CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + SipUtils.CRLF
                + CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE +  SipUtils.CRLF
                + ImdnUtils.HEADER_IMDN_MSG_ID + ": " + msgg.getMessageId() +  SipUtils.CRLF
                + CpimMessage.HEADER_DATETIME + ": "
                    + DateUtils.encodeDate(System.currentTimeMillis()) +  SipUtils.CRLF
                + ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": "
                    + ImdnDocument.POSITIVE_DELIVERY + ", " + ImdnDocument.DISPLAY + SipUtils.CRLF
                + SipUtils.CRLF;

        if (RcsSettings.getInstance().isBase64EncodingSupported()){
             String m =  Base64.encodeToString(message.getBytes(), Base64.NO_WRAP);
             int contentLength = m.getBytes().length;;
              content = content  + CpimMessage.HEADER_CONTENT_TYPE + ": "
                      + "text/plain" + ";charset=utf-8" + SipUtils.CRLF
                + CpimMessage.HEADER_CONTENT_LENGTH + ": " + contentLength + SipUtils.CRLF
                + SipUtils.HEADER_CONTENT_TRANSFER_ENCODING + ": " + "base64" + SipUtils.CRLF
                + SipUtils.CRLF
                + m  + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        }
        else{
            content = content + CpimMessage.HEADER_CONTENT_TYPE + ": "
                    + "text/plain" + ";charset=utf-8" + SipUtils.CRLF
                + CpimMessage.HEADER_CONTENT_LENGTH + ": " + message.getBytes().length
                    + SipUtils.CRLF
                + message + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        }

        String participantList = "";
        participantList = TextUtils.join(",",participants);
        msgg.setRemote(participantList);
        if (! RichMessagingHistory.getInstance().isOne2OneMessageExists(msgg.getMessageId())) {
            RichMessagingHistory.getInstance().addChatMessage(
                    msgg, ChatLog.Message.Direction.OUTGOING);
        }
        try {
            // Create a dialog path
            SipDialogPath dialogPath = new SipDialogPath(
                    imsService.getImsModule().getSipManager().getSipStack(),
                    imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
                    1,
                    ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri(),
                    ImsModule.IMS_USER_PROFILE.getPublicUri(),
                    ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri(),
                    imsService.getImsModule().getSipManager().getSipStack().getServiceRoutePath());

            SipRequest request = SipMessageFactory.createMessageMultiCPM(
                    dialogPath, featureTags, mime, content);

            // Allow header
            SipUtils.buildAllowHeader(request.getStackMessage());

            // Set contribution Id
            String contributionId = ContributionIdGenerator.getContributionId(
                    dialogPath.getCallId());
            request.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, contributionId);

            // set Conversation Id
            String conversationId = (RichMessagingHistory.getInstance().getCoversationID(
                    this.getRemoteContact(), 1));
            if (conversationId.equals("")) {
                // Set Call-Id
                String callId = imsService.getImsModule().getSipManager().getSipStack()
                        .generateCallId();
                conversationId = ContributionIdGenerator.getContributionId(callId);
            }
            request.addHeader(ChatUtils.HEADER_CONVERSATION_ID, conversationId);

            // Send MESSAGE request

            final SipRequest req = request;
            final InstantMessage msg = msgg;

            Thread t = new Thread() {
                public void run() {

                    SipTransactionContext ctx = null;
                    String number = PhoneUtils.extractNumberFromUri(msg.getRemote());
                    try {
                        ctx = imsService.getImsModule().getSipManager().sendSipMessageAndWait(req);
                        // Analyze received message
                        if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                            // 200 OK received
                            if (logger.isActivated()) {
                                logger.info("20x OK response received"); 
                            }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(),ImdnDocument.DELIVERY_STATUS_SENT);

                        }  else {
                            // Error responses
                            if (logger.isActivated()) {
                                logger.info("failed: " + ctx.getStatusCode() + " response received");
                        }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                        }
                    } catch (Exception e) {
                                 if (logger.isActivated()) {
                             logger.info("exception while sending pager message");
                         }  
                          handleMessageDeliveryStatus(
                                  number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                    }
                }
            };
            t.start();

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send Pager Mode MESSAGE request", e);
            }
        }

        return msgg.getMessageId();
    }

    public String sendMultiMessagesByPagerMode(
            String message,String msgId, List<String> participants,String mode) {
        if (logger.isActivated()) {
            logger.debug("sendMultiMessageByPagerMode");
        }

        String mime = CpimMessage.MIME_TYPE_MULTIMESSAGE ;
        String featureTags = FeatureTags.FEATURE_RCSE_PAGER_MSG;

        // Create a text message
        InstantMessage msgg = ChatUtils.createTextMessage(
                contact,
                message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        if(msgId!=null) {
            msgg.setMessageId(msgId);
            msgg.setBurnMessage( RichMessagingHistory.getInstance().isBurnMessage(msgId));
            msgg.setPublicMessage( RichMessagingHistory.getInstance().isPublicMessage(msgId));
        }

        String content;
        String from = ImsModule.IMS_USER_PROFILE.getPublicUri();
        String to = ChatUtils.ANOMYNOUS_URI;//ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri();
        String resourceList = ChatUtils.generateMultiChatResourceList(participants);

        content = Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type: application/resource-lists+xml" + SipUtils.CRLF
                + "Content-Disposition: recipient-list "
                + SipUtils.CRLF + SipUtils.CRLF
                + resourceList + SipUtils.CRLF
                + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                + "Content-Type:"+ CpimMessage.MIME_TYPE + SipUtils.CRLF
                + SipUtils.CRLF
                + CpimMessage.HEADER_FROM + ": " + from+ SipUtils.CRLF
                + CpimMessage.HEADER_TO + ": " + to + SipUtils.CRLF
                +CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE +  SipUtils.CRLF
                +ImdnUtils.HEADER_IMDN_MSG_ID + ": " + msgg.getMessageId() +  SipUtils.CRLF
                +CpimMessage.HEADER_DATETIME + ": "
                    + DateUtils.encodeDate(System.currentTimeMillis()) +  SipUtils.CRLF
                +ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": "
                        + ImdnDocument.POSITIVE_DELIVERY + ", " + ImdnDocument.DISPLAY
                        + SipUtils.CRLF
                + SipUtils.CRLF;

        String modeContentType = null;

        if(mode.equalsIgnoreCase("emoticons")){
            modeContentType = CpimMessage.MIME_TYPE_PAYEMOTICON;
        } else if(mode.equalsIgnoreCase("cloud")){
            modeContentType = CpimMessage.MIME_TYPE_CLOUDMESSAGE;
        }

        if(RcsSettings.getInstance().isBase64EncodingSupported()){
            String m =  Base64.encodeToString(message.getBytes(), Base64.NO_WRAP);
            int contentLength = m.getBytes().length;
            content = content  + CpimMessage.HEADER_CONTENT_TYPE + ": " + modeContentType
                        + ";charset=utf-8" + SipUtils.CRLF
                    + CpimMessage.HEADER_CONTENT_LENGTH + ": " + contentLength + SipUtils.CRLF
                    + SipUtils.HEADER_CONTENT_TRANSFER_ENCODING + ": " + "base64" + SipUtils.CRLF
                    + SipUtils.CRLF
                    + m + SipUtils.CRLF
                    + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        }
        else{
            content = content + CpimMessage.HEADER_CONTENT_TYPE + ": " + modeContentType
                        + ";charset=utf-8" + SipUtils.CRLF
                    + CpimMessage.HEADER_CONTENT_LENGTH + ": " + message.getBytes().length
                        + SipUtils.CRLF
                    + message + SipUtils.CRLF
                    + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + Multipart.BOUNDARY_DELIMITER;
        }

        String participantList = "";
        participantList = TextUtils.join(",",participants);
        msgg.setRemote(participantList);
        if(! RichMessagingHistory.getInstance().isOne2OneMessageExists(msgg.getMessageId())){
            RichMessagingHistory.getInstance().addChatMessage(
                    msgg, ChatLog.Message.Direction.OUTGOING);
        }
        try {
            // Create a dialog path
            SipDialogPath dialogPath = new SipDialogPath(
                    imsService.getImsModule().getSipManager().getSipStack(),
                    imsService.getImsModule().getSipManager().getSipStack().generateCallId(),
                    1,
                    ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri(),
                    ImsModule.IMS_USER_PROFILE.getPublicUri(),
                    ImsModule.IMS_USER_PROFILE.getMultiImConferenceUri(),
                    imsService.getImsModule().getSipManager().getSipStack().getServiceRoutePath());

            SipRequest request = SipMessageFactory.createMessageMultiCPM(
                    dialogPath, featureTags, mime, content);

            // Allow header
            SipUtils.buildAllowHeader(request.getStackMessage());

            // Set contribution Id
            String contributionId = ContributionIdGenerator.getContributionId(
                    dialogPath.getCallId());
            request.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, contributionId);

            // set Conversation Id
            String conversationId = (RichMessagingHistory.getInstance().getCoversationID(
                    this.getRemoteContact(), 1));
            if (conversationId.equals("")) {
                // Set Call-Id
                String callId = imsService.getImsModule().getSipManager().getSipStack()
                        .generateCallId();
                conversationId = ContributionIdGenerator.getContributionId(callId);
            }
            request.addHeader(ChatUtils.HEADER_CONVERSATION_ID, conversationId);

            // Send MESSAGE request

            final SipRequest req = request;
            final InstantMessage msg = msgg;

            Thread t = new Thread() {
                public void run() {

                    SipTransactionContext ctx = null;
                    String number = PhoneUtils.extractNumberFromUri(msg.getRemote());
                    try {
                        ctx = imsService.getImsModule().getSipManager().sendSipMessageAndWait(req);
                        // Analyze received message
                        if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                            // 200 OK received
                            if (logger.isActivated()) {
                                logger.info("20x OK response received");
                            }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(),ImdnDocument.DELIVERY_STATUS_SENT);
                        }  else {
                            // Error responses
                            if (logger.isActivated()) {
                                logger.info("failed: " + ctx.getStatusCode()
                                        + " response received");
                            }
                            handleMessageDeliveryStatus(
                                    number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                        }
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.info("exception while sending pager message");
                        }
                        handleMessageDeliveryStatus(
                                number, msg.getMessageId(), ImdnDocument.DELIVERY_STATUS_ERROR);
                    }
                }
            };
            t.start();

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send Pager Mode MESSAGE request", e);
            }
        }

        return msgg.getMessageId();
    }

/**
     * Sends a plain text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendPublicAccountMessageByLargeMode(String message) {

        if (logger.isActivated()) {
            logger.debug("sendPublicAccountMessageByLargeMode PAM:" + message);
        }

       // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(this.contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());
        mPublicMessages.add(msg.getMessageId());
        return sendMessageByLargeMode(msg,false,null);
    }

    /**
     * Sends a plain text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendMessageByLargeMode(String message) {
        if (logger.isActivated()) {
            logger.debug("sendMessageByLargeMode LMM:" + message);
        }

       // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(this.contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());
        return sendMessageByLargeMode(msg,false,null);
    }

    /**
     * Sends a plain text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendCloudMessageByLargeMode(String message) {
        if (logger.isActivated()) {
            logger.debug("sendCloudMessageByLargeMode LMM:" + message);
        }

       // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(this.contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());
        mCloudMessages.add(msg.getMessageId());
        return sendMessageByLargeMode(msg,false,null);
    }

   public int resendMessage(String msgId) {
       if (logger.isActivated()) {
            logger.debug("resendMessage MsgId:" + msgId);
        }
        String message = RichMessagingHistory.getInstance().getMessageText(msgId);
        if (logger.isActivated()) {
            logger.debug("resendMessage message:" + message);
        }

     // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(this.contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());
        msg.setMessageId(msgId);

        if(msg.getTextMessage().length() >900){
            msg.setMessageId(msgId);
            msg.setBurnMessage(RichMessagingHistory.getInstance().isBurnMessage(msgId));
            msg.setBurnMessage(RichMessagingHistory.getInstance().isPublicMessage(msgId));
            msg.setCloudMessage(RichMessagingHistory.getInstance().isCloudMessage(msgId));
            sendMessageByLargeMode(msg,false,null);
        }
        else
            sendMessageByPagerMode( msg.getTextMessage(), false, false, false,false,msgId, null);

        return getState(msgId);
  }

   public boolean isPublicAccountMessage(String msgId) {
       Iterator<String> publicAccountIterator = mPublicMessages.iterator();
       while(publicAccountIterator.hasNext()){
           try{
                if(msgId.contains(publicAccountIterator.next())) {
                    if (logger.isActivated()) {
                        logger.debug("PAM isPublicAccountMessage return true MsgId:" + msgId);
                    }
                    return true;
                }
           } catch(Exception e) {
               e.printStackTrace();
               if (logger.isActivated()) {
                logger.debug("PAM isPublicAccountMessage return false MsgId:" + msgId);
            }
               return false;
           }
       }
       if (logger.isActivated()) {
            logger.debug("PAM isPublicAccountMessage return false MsgId:" + msgId);
        }
       return false;
   }

   public boolean isCloudMessage(String msgId) {
       Iterator<String> cloudMessageIterator = mCloudMessages.iterator();
       while(cloudMessageIterator.hasNext()){
           try{
                if(msgId.contains(cloudMessageIterator.next())) {
                    if (logger.isActivated()) {
                        logger.debug("PAM isCloudMessage return true MsgId:" + msgId);
                    }
                    return true;
                }
           } catch(Exception e) {
               e.printStackTrace();
               if (logger.isActivated()) {
                logger.debug("PAM isCloudMessage return false MsgId:" + msgId);
            }
               return false;
           }
       }
       if (logger.isActivated()) {
            logger.debug("PAM isCloudMessage return false MsgId:" + msgId);
        }
       return false;
   }

    /**
     * Sends a plain text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendMessageByLargeMode(InstantMessage msg,boolean isPending,String[] extraParams) {
        if (logger.isActivated()) {
            logger.debug("LMM sendMessageByLargeMode isPending:"
                    + isPending + " Text:" + msg.getTextMessage() + " msgId:"+msg.getMessageId());
        }
        synchronized(lock) {
            String contact = this.contact;
            String message = msg.getTextMessage();

            boolean isBurnMessage = msg.isBurnMessage();
            if(extraParams!=null){
                isBurnMessage = Boolean.parseBoolean(extraParams[0]); //get if Boolean msg or not
                if (logger.isActivated()) {
                    logger.debug("LMM isBurnmessage : " +isBurnMessage);
                }
            }

            try {
                if(!isPending){
                    if(session != null){
                        if (logger.isActivated()) {
                            logger.debug("LMM Session is not null. Add into pending list");
                        }

                        //set as burn message
                        if (!RichMessagingHistory.getInstance().isOne2OneMessageExists(
                                msg.getMessageId())) {
                            String message1 = RichMessagingHistory.getInstance().getMessageText(
                                    msg.getMessageId());
                            if (logger.isActivated()) {
                                logger.debug("resendMessage message:" + message1);
                            }

                            if(isBurnMessage){
                                msg.setBurnMessage(isBurnMessage);
                                RichMessagingHistory.getInstance().addBurnChatMessage(
                                        msg, ChatLog.Message.Direction.OUTGOING);
                            }else{
                                if(isCloudMessage(msg.getMessageId()) || msg.isCloudMessage()){
                                    RichMessagingHistory.getInstance().addCloudMessage(
                                            msg, ChatLog.Message.Direction.OUTGOING);
                                }else{
                                    // Update rich messaging history
                                    RichMessagingHistory.getInstance().addChatMessage(
                                            msg, ChatLog.Message.Direction.OUTGOING);
                                }
                            }
                        }

                        if (logger.isActivated()) {
                            logger.debug("LMM Add into pending list text: " +
                                    message + " MsgId:" + msg.getMessageId());
                        }
                        if(RichMessagingHistory.getInstance().isOne2OneMessageExists(
                                msg.getMessageId())){
                            if (logger.isActivated()) {
                                logger.debug("LMM O2O Message exists");
                            }
                        }
                        String message2 = RichMessagingHistory.getInstance().getMessageText(
                                msg.getMessageId());
                        if (logger.isActivated()) {
                            logger.debug("resendMessage2 message:" + message2);
                        }
                        mPendingMessage.add(msg);
                        return msg.getMessageId();
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("LMM initiate a new session to send the message");
                        }

                        pendingMessage = message;
                        pendingMessageId = msg.getMessageId();

                         /**
                          * IF BURN MESSAGE
                          */
                        if(isBurnMessage){
                            msg.setBurnMessage(isBurnMessage);
                            // Initiate a new session with burn flag
                            if(isPublicAccountMessage(msg.getMessageId()) ||
                                    msg.isPublicMessage()){
                                session = (OneOneChatSession)Core.getInstance().getImService()
                                        .initiateOne2OnePublicChatSession(contact, null, message);
                                session.setPublicAccountMessage(true);
                            } else {
                                 // Create a text message
                                InstantMessage tempMsg = ChatUtils.createTextMessage(
                                        this.contact, message,
                                        Core.getInstance().getImService().getImdnManager()
                                            .isImdnActivated());
                                tempMsg.setMessageId(msg.getMessageId());
                                session = (OneOneChatSession)Core.getInstance().getImService()
                                        .initiateOne2OneStandAloneChatSession(
                                                contact, tempMsg, message);
                            }
                           //set burn message
                           session.setBurnMessage(isBurnMessage);
                           session.setCloudMessage(
                                   isCloudMessage(msg.getMessageId()) || msg.isCloudMessage());
                           session.setLargeMessageMode(true);

                           // Update with new session
                            setCoreSession(session);

                            // Update rich messaging history
                            if(! RichMessagingHistory.getInstance().isOne2OneMessageExists(
                                    msg.getMessageId())){
                                RichMessagingHistory.getInstance().addBurnChatMessage(
                                        msg, ChatLog.Message.Direction.OUTGOING);
                            }
                         } else{
                             if (logger.isActivated()) {
                                 logger.debug("LMM Initiate a O2O Standalone chat");
                             }

                            // Initiate a new session
                             if(isPublicAccountMessage(msg.getMessageId()) ||
                                     msg.isPublicMessage()){
                                session = (OneOneChatSession)Core.getInstance().getImService()
                                        .initiateOne2OnePublicChatSession(contact, null, message);
                                session.setPublicAccountMessage(true);
                             } else {
                                 // Create a text message
                                 InstantMessage tempMsg = ChatUtils.createTextMessage(
                                         this.contact, message,
                                         Core.getInstance().getImService().getImdnManager()
                                             .isImdnActivated());
                                 tempMsg.setMessageId(msg.getMessageId());
                                 session = (OneOneChatSession)Core.getInstance().getImService()
                                        .initiateOne2OneStandAloneChatSession(
                                                contact, tempMsg, message);
                             }
                             session.setCloudMessage(isCloudMessage(msg.getMessageId()) ||
                                     msg.isCloudMessage());
                             session.setLargeMessageMode(true);

                             // Update with new session
                             setCoreSession(session);

                             // Update rich messaging history
                             if (!RichMessagingHistory.getInstance().isOne2OneMessageExists(
                                     msg.getMessageId())) {
                                if(isCloudMessage(msg.getMessageId())){
                                    RichMessagingHistory.getInstance().addCloudMessage(
                                            msg, ChatLog.Message.Direction.OUTGOING);
                                } else {
                                    // Update rich messaging history
                                    RichMessagingHistory.getInstance().addChatMessage(
                                            msg, ChatLog.Message.Direction.OUTGOING);
                                }
                            }
                         }

                        // Start the session
                        Thread t = new Thread() {
                            public void run() {
                                session.startSession();
                            }
                        };
                        t.start();
                        String message2 = RichMessagingHistory.getInstance().getMessageText(
                                msg.getMessageId());
                        if (logger.isActivated()) {
                            logger.debug("resendMessage2 message:" + message2);
                        }
                        return msg.getMessageId();
                    }
                }else{
                    if(session != null){
                        if (logger.isActivated()) {
                            logger.debug("LMM Session is not null. Add into pending list");
                        }
                        mPendingMessage.add(msg);
                        return msg.getMessageId();
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("LMM initiate a new session to send pending message text:"
                                + message);
                    }
                    pendingMessage = message;
                 // Initiate a new session
                    if(isPublicAccountMessage(msg.getMessageId()) || msg.isPublicMessage()){
                        session = (OneOneChatSession)Core.getInstance().getImService()
                                .initiateOne2OnePublicChatSession(contact, null, message);
                        session.setPublicAccountMessage(true);
                    } else {
                        // Create a text message
                        InstantMessage tempMsg = ChatUtils.createTextMessage(
                                this.contact, message,
                                Core.getInstance().getImService().getImdnManager()
                                    .isImdnActivated());
                        tempMsg.setMessageId(msg.getMessageId());
                        session = (OneOneChatSession)Core.getInstance().getImService()
                                .initiateOne2OneStandAloneChatSession(contact, tempMsg, message);
                    }

                    session.setLargeMessageMode(true);

                    if(isBurnMessage){
                       //set burn message
                       session.setBurnMessage(isBurnMessage);
                    }

                    // Update with new session
                    setCoreSession(session);
                    // Start the session
                    Thread t = new Thread() {
                        public void run() {
                            session.startSession();
                        }
                    };
                    t.start();
                    return null;
                    }
                }
            } catch(Exception e) {
                if (logger.isActivated()) {
                    logger.error("LMM Can't send a new chat message", e);
                }
                return null;
            }
        }
    }

    /**
     * Sends a Burn After Read text message
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendBurnMessage(String message) {
        if (logger.isActivated()) {
            logger.debug("Send Burn text message");
        }

        // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        // Send message
        return sendChatMessage(msg);
    }

    /**
     * Sends a Burn After Read text message in Pager Mode
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendPagerModeBurnMessage(String message) {
        if (logger.isActivated()) {
            logger.debug("Send Burn text message");
        }

        return sendMessageByPagerMode(message,true,false,false,false,null);
    }


    /**
     * Sends a Burn After Read text message in large message Mode
     *
     * @param message Text message
     * @return Unique message ID or null in case of error
     */
    public String sendLargeModeBurnMessage(String message) {
        if (logger.isActivated()) {
            logger.debug("Send large mode burn text message");
        }
        // Create a text message
        InstantMessage msg = ChatUtils.createTextMessage(contact, message,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        String burnFlag[] = {"true"};
        return sendMessageByLargeMode(msg,false,burnFlag);
    }


    /**
     * Sends a geoloc message
     *
     * @param geoloc Geoloc
     * @return Unique message ID or null in case of error
     */
    public String sendGeoloc(Geoloc geoloc) {
        if (logger.isActivated()) {
            logger.debug("Send geoloc message");
        }

        // Create a geoloc message
        GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
                geoloc.getLatitude(), geoloc.getLongitude(),
                geoloc.getExpiration(), geoloc.getAccuracy());

        // Create a geoloc message
        GeolocMessage msg = ChatUtils.createGeolocMessage(contact, geolocPush,
                Core.getInstance().getImService().getImdnManager().isImdnActivated());

        // Send message
        return sendChatMessage(msg);
    }

    /**
     * Sends a chat message
     *
     * @param msg Message
     * @return Unique message ID or null in case of error
     */
    private String sendChatMessage(final InstantMessage msg) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.debug("LMM Send chat message");
            }

            // Check if a session should be initiated or not
            if ((session == null) ||
                    session.getDialogPath().isSessionTerminated() ||
                    !session.getDialogPath().isSessionEstablished()) {
                try {
                    if (logger.isActivated()) {
                        logger.debug("ABC Core session is not yet established:"
                                + " initiate a new session to send the message");
                    }

                    // Initiate a new session
                    session = (OneOneChatSession)Core.getInstance().getImService()
                            .initiateOne2OneChatSession(contact, msg);

                    // Update with new session
                    setCoreSession(session);

                    // Update rich messaging history
                    if(! RichMessagingHistory.getInstance().isOne2OneMessageExists(
                            msg.getMessageId())){

                        RichMessagingHistory.getInstance().addChatMessage(
                                msg, ChatLog.Message.Direction.OUTGOING);
                    }
                    // Start the session
                    Thread t = new Thread() {
                        public void run() {
                            session.startSession();
                        }
                    };
                    t.start();
                    return session.getFirstMessage().getMessageId();
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("ABC Can't send a new chat message", e);
                    }
                    return null;
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("ABC Core session is established:"
                            + " use existing one to send the message");
                }

                // Generate a message Id
                final String msgId = ChatUtils.generateMessageId();

                // Send message
                Thread t = new Thread() {
                    public void run() {
                        if (msg instanceof GeolocMessage) {
                            session.sendGeolocMessage(msgId, ((GeolocMessage)msg).getGeoloc());
                        } else {
                            session.sendTextMessage(msgId, msg.getTextMessage());
                        }
                    }
                };
                t.start();
                return msgId;
            }
        }
    }

    /**
     * Sends a displayed delivery report for a given message ID
     *
     * @param msgId Message ID
     */
    public void sendDisplayedDeliveryReport(final String msgId) {
        try {
            if (logger.isActivated()) {
                logger.info("LMM Set displayed delivery report for "
                        + msgId + "contact: " + session.getRemoteContact());
            }

            // Send delivery status
            if ((session != null) &&
                    (session.getDialogPath() != null) &&
                    (session.getDialogPath().isSessionEstablished()) &&
                    session.getMsrpMgr() != null &&
                    session.getMsrpMgr().getMsrpSession() != null &&
                    !session.isLargeMessageMode()) {
                // Send via MSRP
                Thread t = new Thread() {
                    public void run() {
                        session.sendMsrpMessageDeliveryStatus(
                                session.getRemoteContact(),
                                msgId,
                                ImdnDocument.DELIVERY_STATUS_DISPLAYED);
                    }
                };
                t.start();
            } else {
                String contact = SipUtils.extractUriFromAddress(session.getRemoteContact());
                if (logger.isActivated()) {
                    logger.info("LMM Set displayed delivery report for1 " + "contact: " + contact);
                }
                String subStr =  contact.substring(0,3);
                String newContact = null;
                if(!(subStr.equals("sip") || subStr.equals("tel"))){
                    newContact = "tel:" + contact ;
                }
                else{
                    newContact = contact;
                }
                if (logger.isActivated()) {
                    logger.info("LMM Set displayed delivery report for1 "
                            + "newContact: " + newContact + "subStr: " + subStr);
                }
                // Send via SIP MESSAGE
                Core.getInstance().getImService().getImdnManager().sendMessageDeliveryStatus(
                        newContact, msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("LMM Could not send MSRP delivery status",e);
            }
        }
    }


    /**
     * Sends a Burn delivery report for a given message ID
     *
     * @param msgId Message ID
     */
    public void sendBurnDeliveryReport(final String msgId) {
        try {
            if (logger.isActivated()) {
                logger.info("Set displayed delivery report for " + msgId + "contact: "
                        + session.getRemoteContact());
            }

            // Send delivery status
            if ((session != null) &&
                    (session.getDialogPath() != null) &&
                    (session.getDialogPath().isSessionEstablished()) &&
                    session.getMsrpMgr() != null &&
                    session.getMsrpMgr().getMsrpSession() != null) {
                // Send via MSRP
                Thread t = new Thread() {
                    public void run() {
                        session.sendMsrpMessageBurnStatus(
                                session.getRemoteContact(), msgId, ImdnDocument.BURN_STATUS_BURNED);
                    }
                };
                t.start();
            } else {
                String contact = SipUtils.extractUriFromAddress(session.getRemoteContact());
                if (logger.isActivated()) {
                    logger.info("Set displayed delivery report for1 " + "contact: " + contact);
                }
                String subStr =  contact.substring(0,3);
                String newContact = null;
                if(!(subStr.equals("sip") || subStr.equals("tel"))){
                    newContact = "tel:" + contact ;
                }
                else{
                    newContact = contact;
                }
                if (logger.isActivated()) {
                    logger.info("Set displayed delivery report for1 "
                            + "newContact: " + newContact + "subStr: " + subStr);
                }
                // Send via SIP MESSAGE
                Core.getInstance().getImService().getImdnManager().sendMessageDeliveryStatus(
                        newContact, msgId, ImdnDocument.BURN_STATUS_BURNED);
            }
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Could not send MSRP delivery status",e);
            }
        }
    }


    /**
     * Sends an is-composing event. The status is set to true when
     * typing a message, else it is set to false.
     *
     * @param status Is-composing status
     */
    public void sendIsComposingEvent(final boolean status) {
        if (logger.isActivated()) {
            logger.info("LMM sendIsComposingEvent " + session.isLargeMessageMode());
        }
        if (session != null && !session.isLargeMessageMode()) {
            Thread t = new Thread() {
                public void run() {
                    session.sendIsComposingStatus(status);
                }
            };
            t.start();
        }
    }

    /**
     * Adds a listener on chat events
     *
     * @param listener Chat event listener
     */
    public void addEventListener(IChatListener listener) {
        if (logger.isActivated()) {
            logger.info("LMM Add an event listener");
        }

        synchronized(lock) {
            listeners.register(listener);
        }
    }

    /**
     * Removes a listener on chat events
     *
     * @param listener Chat event listener
     */
    public void removeEventListener(IChatListener listener) {
        if (logger.isActivated()) {
            logger.info("LMM Remove an event listener");
        }

        synchronized(lock) {
            listeners.unregister(listener);
        }
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /**
     * Session is started
     */
    public void handleSessionStarted() {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM Session started");
            }

            // Update rich messaging history
            // Nothing done in database
        }

        if (logger.isActivated()) {
            logger.info("LMM handleSessionStarted:" + pendingMessage + "MsgId:"
                    + pendingMessageId);
        }

        if(pendingMessage != null) {
            // Send message
            Thread t = new Thread() {
                public void run() {
                    boolean isCloud = RichMessagingHistory.getInstance().isCloudMessage(
                            pendingMessageId);
                    if (logger.isActivated()) {
                        logger.info("LMM handleSessionStarted:" + isCloud
                                    + ", MsgId:" + pendingMessageId);
                    }
                    if(isCloud){
                        session.sendCloudMessage(pendingMessageId, pendingMessage);
                    } else {
                        session.sendTextMessage(pendingMessageId, pendingMessage);
                    }
                }};
                t.start();
        }
    }

    /**
     * Session has been aborted
     *
     * @param reason Termination reason
     */
    public void handleSessionAborted(int reason) {
        synchronized(lock) {
            String remoteContact = session.getRemoteContact();
            String number = PhoneUtils.extractNumberFromUri(remoteContact);
            if (logger.isActivated()) {
                logger.info("LMM handleSessionAborted: " + remoteContact + "Number: " + number);
            }

            // Update rich messaging history
            // Nothing done in database

            // Remove session from the list
            if(session.isStoreAndForward()){
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove storeChatSessions "
                            + session.getSessionID());
                }
                ChatServiceImpl.removeStoreChatSession(number);
            }
            else{
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove chatSessions " + session.getSessionID());
                }
                ChatServiceImpl.removeChatSession(number);
            }
        }

        if(!mPendingMessage.isEmpty()){
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    InstantMessage imMessage = mPendingMessage.get(0);
                    if (logger.isActivated()) {
                        logger.info("LMM handleSessionAborted text:" + imMessage.getTextMessage()
                                + "MsgId:" + imMessage.getMessageId());
                    }
                    mPendingMessage.remove(imMessage);
                    pendingMessageId = imMessage.getMessageId();

                    if(imMessage.isBurnMessage()){
                        String[] burnFlag = {"true"};
                        sendMessageByLargeMode(imMessage, true, burnFlag);
                    }else{
                        sendMessageByLargeMode(imMessage, true, null);
                    }
                }
            });
        }
    }

    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
        synchronized(lock) {

            String remoteContact = session.getRemoteContact();
            String number = PhoneUtils.extractNumberFromUri(remoteContact);
            if (logger.isActivated()) {
                logger.info("LMM handleSessionTerminatedByRemote: "
                        + remoteContact + "Number: " + number);
            }

            // Update rich messaging history
            // Nothing done in database

            // Remove session from the list
            if(session.isStoreAndForward()){
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove storeChatSessions "
                            + session.getSessionID());
                }
                ChatServiceImpl.removeStoreChatSession(number);
            }
            else{
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove chatSessions " + session.getSessionID());
                }
                ChatServiceImpl.removeChatSession(number);
            }
        }
        if(!mPendingMessage.isEmpty()){
                if (logger.isActivated()) {
                logger.info("LMM handleSessionAborted mPendingMessage is not empty");
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    InstantMessage imMessage = mPendingMessage.get(0);
                    if (logger.isActivated()) {
                        logger.info("LMM handleSessionTerminatedByRemote text:"
                                + imMessage.getTextMessage()
                                + "MsgId:" + imMessage.getMessageId());
                    }
                    mPendingMessage.remove(imMessage);
                    pendingMessageId = imMessage.getMessageId();

                    if(imMessage.isBurnMessage()){
                        String[] burnFlag = {"true"};
                        sendMessageByLargeMode(imMessage, true, burnFlag);
                    }else{
                        sendMessageByLargeMode(imMessage, true, null);
                    }
                }
            });
        }
    }

    /**
     * New text message received
     *
     * @param text Text message
     */
    public void handleReceiveMessage(InstantMessage message) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM handleReceiveMessage New IM received: "
                        + message.getTextMessage());
            }

            if(message.isBurnMessage()){
                if (logger.isActivated()) {
                    logger.info("LMM handleReceiveMessage Burn msgId: " + message.getMessageId());
                }
                RichMessagingHistory.getInstance().addBurnChatMessage(
                        message, ChatLog.Message.Direction.INCOMING);
            }else{
                if(message.isCloudMessage()){
                    if (logger.isActivated()) {
                        logger.info("LMM handleReceiveMessage Cloud msgId: "
                                + message.getMessageId());
                    }
                    RichMessagingHistory.getInstance().addCloudMessage(
                            message, ChatLog.Message.Direction.INCOMING);
                } else {
                    // Update rich messaging history
                    RichMessagingHistory.getInstance().addChatMessage(
                            message, ChatLog.Message.Direction.INCOMING);
                }
            }

            // Create a chat message
            ChatMessage msgApi = null;
            if(message.isPublicMessage()){
                if (logger.isActivated()) {
                    logger.info("LMM handleReceiveMessage Public Chat msgId: "
                            + message.getMessageId());
                }
                msgApi = new ChatMessage(
                            message.getMessageId(),
                            message.getRemote(),
                            message.getTextMessage(),
                            message.getServerDate(),
                            message.isImdnDisplayedRequested(),
                            message.getDisplayName());
            }else {
                msgApi = new ChatMessage(
                            message.getMessageId(),
                            PhoneUtils.extractNumberFromUri(message.getRemote()),
                            message.getTextMessage(),
                            message.getServerDate(),
                            message.isImdnDisplayedRequested(),
                            message.getDisplayName());
            }

                msgApi.setBurnMessage(message.isBurnMessage());
                msgApi.setPublicMessage(message.isPublicMessage());
                msgApi.setCloudMessage(message.isCloudMessage());


            // Broadcast intent related to the received invitation
            Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
            intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
            intent.putExtra(ChatIntent.EXTRA_CONTACT, msgApi.getContact());
            intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
            intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
            AndroidFactory.getApplicationContext().sendBroadcast(intent);

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleReceiveMessage N:" + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    if(message.isBurnMessage()){
                        //listeners.getBroadcastItem(i).onNewBurnMessageArrived(msgApi);
                    }else{
                        //listeners.getBroadcastItem(i).onNewMessage(msgApi);
                    }
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

    /**
     * New geoloc message received
     *
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("New geoloc received");
            }

            // Update rich messaging history
            RichMessagingHistory.getInstance().addChatMessage(
                        geoloc, ChatLog.Message.Direction.INCOMING);

            // Create a geoloc message
            Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
                    geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
                    geoloc.getGeoloc().getExpiration());
            org.gsma.joyn.chat.GeolocMessage msgApi = new org.gsma.joyn.chat.GeolocMessage(
                    geoloc.getMessageId(),
                    PhoneUtils.extractNumberFromUri(geoloc.getRemote()),
                    geolocApi, geoloc.getDate(), geoloc.isImdnDisplayedRequested());

            // Broadcast intent related to the received invitation
            Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
            intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
            intent.putExtra(ChatIntent.EXTRA_CONTACT, msgApi.getContact());
            intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
            intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
            AndroidFactory.getApplicationContext().sendBroadcast(intent);

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onNewGeoloc(msgApi);
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

    /**
     * IM session error
     *
     * @param error Error
     */
    public void handleImError(ChatError error) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM IM error " + error.getErrorCode() );
            }
            String remoteContact = session.getRemoteContact();
            String number = PhoneUtils.extractNumberFromUri(remoteContact);

            if (logger.isActivated()) {
                logger.info("LMM handleImError: " + remoteContact + "Number: " + number);
            }

            // Update rich messaging history
            switch(error.getErrorCode()){
                case ChatError.SESSION_INITIATION_FAILED:
                case ChatError.SESSION_INITIATION_CANCELLED:
                    RichMessagingHistory.getInstance().updateChatMessageStatus(
                            session.getFirstMessage().getMessageId(),
                            ChatLog.Message.Status.Content.FAILED);
                    // TODO: notify listener
                    // Notify event listeners
                    final int N = listeners.beginBroadcast();
                    if (logger.isActivated()) {
                        logger.info("LMM handleImError N:" + N);
                    }
                    for (int i=0; i < N; i++) {
                        try {
                            listeners.getBroadcastItem(i).onReportMessageFailed(
                                    session.getFirstMessage().getMessageId());
                        } catch(Exception e) {
                            if (logger.isActivated()) {
                                logger.error("Can't notify listener", e);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            // Remove session from the list
            if(session.isStoreAndForward()){
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove storeChatSessions "
                            + session.getSessionID());
                }
                ChatServiceImpl.removeStoreChatSession(number);
            }
            else{
                if (logger.isActivated()) {
                    logger.debug("LMM AddRemove remove chatSessions " + session.getSessionID());
                }
                ChatServiceImpl.removeChatSession(number);
            }
        }
        if(!mPendingMessage.isEmpty()){
                if (logger.isActivated()) {
                logger.info("LMM handleSessionAborted1 mPendingMessage is not empty");
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    InstantMessage imMessage = mPendingMessage.get(0);
                    if (logger.isActivated()) {
                        logger.info("LMM handleImError text:" + imMessage.getTextMessage()
                                + "MsgId:" + imMessage.getMessageId());
                    }
                    mPendingMessage.remove(imMessage);
                    pendingMessageId = imMessage.getMessageId();
                    sendMessageByLargeMode(imMessage, true, null);
                }
            });
        }
    }

    /**
     * Is composing event
     *
     * @param contact Contact
     * @param status Status
     */
    public void handleIsComposingEvent(String contact, boolean status) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info(contact + " is composing status set to " + status);
            }

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleIsComposingEvent N:" + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onComposingEvent(status);
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

    public void removePublicMessagesFromList(String msgId) {
        try {
            if (logger.isActivated()) {
                logger.info("PAM removePublicMessagesFromList: " + msgId);
            }
            mPublicMessages.remove(msgId);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void removeCloudMessagesFromList(String msgId) {
        try {
            if (logger.isActivated()) {
                logger.info("PAM removeCloudMessagesFromList: " + msgId);
            }
            mCloudMessages.remove(msgId);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * New message delivery status
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String msgId, String status) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM New message delivery status for message "
                        + msgId + ", status " + status);
            }

            // Update rich messaging history
            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleMessageDeliveryStatus2 N:" + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
                        listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_SENT)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportSentMessage(msgId);
                    }
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

     /**
     * New message delivery status
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact,String msgId, String status) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM New message delivery status for message "
                        + msgId + ", status " + status);
            }

            // Update rich messaging history
            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleMessageDeliveryStatus1 N:" + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
                        listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_SENT)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        listeners.getBroadcastItem(i).onReportSentMessage(msgId);
                    }
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

     /**
     * Adds a listener on chat events
     *
     * @param listener Chat event listener
     */
    public void addSpamReportListener(ISpamReportListener listener) {
        if (logger.isActivated()) {
            logger.info("addSpamReportListener - " + listener);
        }

        synchronized(lock) {
            spamListeners.register(listener);
        }
    }

    /**
     * Removes a listener on chat events
     *
     * @param listener Chat event listener
     */
    public void removeSpamReportListener(ISpamReportListener listener) {
        if (logger.isActivated()) {
            logger.info("removeSpamReportListener");
        }

        synchronized(lock) {
            spamListeners.unregister(listener);
        }
    }

    /**
     * New message delivery status
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleSpamReportStatus(String contact,String msgId, String status, int errorCode) {
           // Notify event listeners
            final int N = spamListeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleSpamReportStatus N:" + N
                        + "status " + status + "msgId" + msgId);
            }
            for (int i=0; i < N; i++) {
                try {
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
                        removePublicMessagesFromList(msgId);
                        removeCloudMessagesFromList(msgId);
                        spamListeners.getBroadcastItem(i).onSpamReportSuccess(contact, msgId);
                    } else
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
                        spamListeners.getBroadcastItem(i).onSpamReportFailed(
                                contact, msgId, errorCode);
                    }
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            spamListeners.finishBroadcast();
        }

     /**
     * New message delivery status
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(
            String contact,String msgId, String status, int errorCode ,String statusCode) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("LMM New message delivery status for message " + msgId
                        + ", status " + status);
            }
            int codeError = 0;

            // Update rich messaging history
            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
            switch(errorCode){
                case ImdnDocument.TIMEOUT:
                    codeError = Chat.ErrorCodes.TIMEOUT;
                    break;
                case ImdnDocument.INTERNAL_ERROR:
                    codeError = Chat.ErrorCodes.INTERNAL_EROR;
                    break;
                case ImdnDocument.OUT_OF_SIZE:
                    codeError = Chat.ErrorCodes.OUT_OF_SIZE;
                    break;
                case ImdnDocument.UNKNOWN:
                    codeError = Chat.ErrorCodes.UNKNOWN;
                    break;
                default:
                    codeError = Chat.ErrorCodes.TIMEOUT;
                    break;
            }

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("LMM handleMessageDeliveryStatus0 N:" + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
                        listeners.getBroadcastItem(i).onReportFailedMessage(
                                msgId,codeError,statusCode);
                    }
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
    }

    /**
     * Conference event
     *
     * @param contact Contact
     * @param contactDisplayname Contact display name
     * @param state State associated to the contact
     */
    public void handleConferenceEvent(
            String contact, String contactDisplayname,
            String state, String method,
            String userStateParameter, String conferenceState) {
        // Not used here
    }

    /**
     * Request to add participant is successful
    */
    public void handleAddParticipantSuccessful() {
        // Not used in single chat
    }

    /**
     * Request to add participant has failed
     *
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason) {
        // Not used in single chat
    }

    @Override
    public void handleModifySubjectSuccessful(String subject) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleModifySubjectFailed(int statusCode) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleTransferChairmanSuccessful(String newChairman) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleTransferChairmanFailed(int statusCode) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleRemoveParticipantSuccessful(String removedParticipant){
        // TODO Auto-generated method stub
    }

    @Override
    public void handleRemoveParticipantFailed(int statusCode){
        // TODO Auto-generated method stub
    }

    @Override
    public void handleAbortConversationResult(int reason, int code) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleSessionTerminatedByGroupRemote(String cause, String text) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleQuitConversationResult(int code) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleModifySubjectByRemote(String subject) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleModifyNicknameSuccessful(String contact,
            String newNickName) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleModifyNicknameFailed(String contact, int statusCode) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleModifyNicknameByRemote(String contact, String newNickname) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleTransferChairmanByRemote(String newChairman) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleConferenceNotify(String confState, List<ConferenceUser> users) {
        // TODO Auto-generated method stub
    }
}
