package com.mediatek.rcs.common.service;

import com.mediatek.rcs.common.service.Participant;

/**
 * This interface defines a chat window manager that manages the chat windows.
 */
interface IRCSChatServiceListener {
    //O2O Message arrived notification for the notification bar.
    void onNewMessage(in long msgId);

    void onNewGroupMessage(in String chatId, in long msgId, in String number);

    void onSendO2OMessageFailed(in long msgId);

    void onSendO2MMessageFailed(in long msgId);

    void onRequestBurnMessageCapabilityResult(in String contact, in boolean result);


    
    //for groupChat///////////////////////////////////////////

    void onSendGroupMessageFailed(in long msgId);
    /**
     * Participant join
     * 
     * @param participant
     */
    void onParticipantJoined(in String chatId, in Participant participant);

    /**
     * Participant left
     * 
     * @param participant
     */
    void onParticipantLeft(in String chatId, in Participant participant);

    /**
     * Participant removed
     */
    void onParticipantRemoved(in String chatId, in Participant participant);

    /**
     * Chairman changed from another participant
     * 
     * @param success
     * @param participant
     */
    void onChairmenChanged(in String chatId, in Participant participant, in boolean isMe);

    /**
     * Another participant modify subject
     * 
     * @param success
     * @param subject
     */
    void onSubjectModified(in String chatId, in String subject);

    void onParticipantNickNameModified(in String chatId, in String contact, in String nickName);
    /**
     * I was removed by another participant
     * 
     * @param contact
     */
    void onMeRemoved(in String chatId, in String contact);

    /**
     * abort by another participant
     */
    void onAbort(in String chatId);

    /**
     * 
     * @param participant
     * @param subject
     */
    void onNewInvite(in Participant participant, in String subject, in String chatId);

    /**
     * 
     */
    void onInvitationTimeout(in String chatId);

    /**
     * When the invited contact reject a group invitation, this function will be called
     * @param
     */
     void onAddParticipantFail(in Participant participant, in String chatId);

    /**
     * 
     * @param result
     */
    void onAddParticipantsResult(in String chatId, in boolean result);

    /**
     * 
     * @param participant
     * @param result
     */
    void onRemoveParticipantsResult(in String chatId, in boolean result);

    /**
     * I transfer chairmen, just need result, if true: changed success
     * 
     * @param result
     */
    void onTransferChairmenResult(in String chatId, in boolean result);

    /**
     * I modify my nick name, If true, modify success
     * 
     * @param success
     */
    void onMyNickNameModifiedResult(in String chatId, in boolean result);

    /**
     * I modify subject, If true, modify success
     * 
     * @param result
     */
    void onSubjectModifiedResult(in String chatId, in boolean result);

    /**
     * quit conversation, if true: quit success
     * 
     * @param success
     */
    void onQuitConversationResult(in String chatId, in boolean result);

    /**
     * I dissove the group chat, it true, dissolve success
     * 
     * @param result
     */
    void onAbortResult(in String chatId, in boolean result);

    /**
     * 
     */
    void onInitGroupResult(in boolean result, in String chatId);

    /**
     * 
     * @param result
     */
    void onAcceptInvitationResult(in String chatId, in boolean result);

    /**
     * 
     * @param result
     */
    void onRejectInvitationResult(in String chatId, in boolean result);
    void onUpdateFileTransferStatus(in long ipMsgId, in int stat, in int status);
    /**
     * 
     */
    void setFilePath(in long ipMsgId, in String filePath);
    
        /**
     * @param handleSpamReportResult
     * @return
     */
     void handleSpamReportResult(in String contact, in String msgId, in int errorcode);
 
    /**
     * @param handleFileSpamReportResult
     * @return
     */
     void handleFileSpamReportResult(in String contact, in String msgId, in int errorcode);
    
}
