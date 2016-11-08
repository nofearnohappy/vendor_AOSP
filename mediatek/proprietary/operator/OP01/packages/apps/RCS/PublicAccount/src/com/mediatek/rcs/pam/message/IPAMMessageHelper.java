package com.mediatek.rcs.pam.message;

import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.PublicAccountChat;
import org.gsma.joyn.chat.SpamReportListener;
import org.gsma.joyn.ft.FileSpamReportListener;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;

public interface IPAMMessageHelper {

    ChatService getChatService();

    FileTransferService getFileTransferService();

    ChatListener getChatServiceListener(long msgId);

    FileTransferListener getFileTransferListener(long msgId);

    /**
     * Cache related Interface
     */
    PublicAccountChat getChatCache(long token, String uuid);

    void updateChatCache(long token, String uuid, PublicAccountChat chat);

    /**
     * SpamReport related Interface
     */
    SpamReportListener getSpamReportListener(
            long token, String sourceId, long msgId);

    FileSpamReportListener getFileSpamReportListener(
            long token, String sourceId, long msgId);

}
