package com.mediatek.email.plugin;

import java.util.HashMap;

import android.content.Context;
import com.android.emailcommon.provider.Account;
import com.mediatek.email.ext.DefaultOperatorAccount;

import android.util.Log;
import com.mediatek.common.PluginImpl;

import com.android.email.mail.store.ImapFolder;
import com.android.email.mail.store.ImapStore;
import com.android.email.R;
import com.android.email.LegacyConversions;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent;
import com.android.email.mail.Store;

@PluginImpl(interfaceName = "com.mediatek.email.ext.IOperatorAccount")
public class OperatorAccountPlugin extends DefaultOperatorAccount {

    public String TAG = "OperatorAccountPlugin";

    public boolean isOrangeAccount(Context context, long accountId) {
        Account account = Account.restoreAccountWithId(context, accountId);
        String legacyImapProtocol = "imap";

        if (legacyImapProtocol.equalsIgnoreCase(account.getProtocol(context))) {
            String[] emailName = account.mEmailAddress.split("@|\\.");
            String suffix = emailName[1];
            if (!suffix.isEmpty() && suffix.equalsIgnoreCase("orange")
                    || suffix.equalsIgnoreCase("wanadoo")) {
                Log.i(TAG, "[EMAIL_OP03]isOrangeAccount:TRUE");
                return true;
            }
        }
        Log.i(TAG, "[EMAIL_OP03]isOrangeAccount:FALSE");
        return false;
    }

    @Override
    public boolean isDraftSyncNeeded(Context context, long accountId) {
        if (isOrangeAccount(context, accountId) == true) {
            Log.i(TAG, "[EMAIL_OP03]isDraftSyncNeeded:TRUE");
            return true;
        }
        Log.i(TAG, "[EMAIL_OP03]isDraftSyncNeeded:FALSE");
        return false;
    }

    @Override
    public boolean isFolderOverridingRequired(Context context, long accountId) {
        if (isOrangeAccount(context, accountId) == true) {
            Log.i(TAG, "[EMAIL_OP03]isFolderOverridingRequired:TRUE");
            return true;
        }
        Log.i(TAG, "[EMAIL_OP03]isFolderOverridingRequired:FALSE");
        return false;
    }

    @Override
    public void updateOperatorMailbox(Context mContext, long accountId,
            Object boxes, String folderName, char delimiterChar, Object impObj) {
        Log.i(TAG, "[EMAIL_OP03]updateOperatorMailbox");

        ImapStore imp = (ImapStore) impObj;
        HashMap<String, ImapFolder> mailboxes = (HashMap<String, ImapFolder>) boxes;

        String name = folderName.split("/")[1];
        if (name.equalsIgnoreCase("TRASH")) {
            name = mContext.getString(R.string.mailbox_name_server_trash);
        }
        if (name.equalsIgnoreCase("DRAFT")) {
            name = mContext.getString(R.string.mailbox_name_server_drafts);
        }
        int type = LegacyConversions.inferMailboxTypeFromName(mContext, name);
        if (type == Mailbox.TYPE_OUTBOX) {
            name = mContext.getString(R.string.mailbox_name_server_sent);
            type = Mailbox.TYPE_SENT;
        }
        if (type == Mailbox.TYPE_MAIL) {
            ImapFolder folder = addUserDefinedMailbox(mContext, accountId,
                    folderName, delimiterChar, true, Mailbox.TYPE_NONE, imp);
            mailboxes.put(name, folder);
        } else {
            final Mailbox mailbox = Mailbox.restoreMailboxOfType(mContext,
                    accountId, type);
            final ImapFolder folder = addSystemDefinedMailbox(mContext,
                    accountId, name, '\0', true, mailbox, folderName, imp);
            mailboxes.put(name, folder);
        }
    }

    /**
     * Creates a {@link Folder} and associated {@link Mailbox}. If the folder
     * does not already exist in the local database, a new row will immediately
     * be created in the mailbox table. Otherwise, the existing row will be
     * used. Any changes to existing rows, will not be stored to the database
     * immediately.
     * @param context
     *            context of app
     * @param accountId
     *            The ID of the account the mailbox is to be associated with
     * @param mailboxPath
     *            The path of the mailbox to add
     * @param delimiter
     *            A path delimiter. May be {@code null} if there is no
     *            delimiter.
     * @param selectable
     *            If {@code true}, the mailbox can be selected and used to store
     *            messages.
     * @param mailboxType
     *            The type of the mailbox
     * @param imp
     *            instance of the ImapStore class
     */
    private ImapFolder addUserDefinedMailbox(Context context, long accountId,
            String mailboxPath, char delimiter, boolean selectable,
            int mailboxType, ImapStore imp) {
        ImapFolder folder = (ImapFolder) imp.getFolder(mailboxPath);
        Mailbox mailbox = null;
        // For special-use mailboxes(Inbox, drafts, trash, etc.) gotten from
        // XLIST,
        // we check if the mailbox of that type was already existed. For other
        // mailboxes,
        // we checking the existence by their name
        if (mailboxType == Mailbox.TYPE_NONE
                || mailboxType == Mailbox.TYPE_MAIL) {
            mailbox = Mailbox
                    .getMailboxForPath(context, accountId, mailboxPath);
        } else {
            mailbox = Mailbox.restoreMailboxOfType(context, accountId,
                    mailboxType);
            if (mailbox == null) {
                mailbox = new Mailbox();
            }
        }
        /// M: not suitable for MTK solution
        // final ImapFolder folder = (ImapFolder) getFolder(mailboxPath);
        if (mailbox.isSaved()) {
            // existing mailbox
            // mailbox retrieved from database; save hash _before_ updating
            // fields
            folder.setLocalHashes(mailbox.getHashes());
        }

        if (mailboxType == Mailbox.TYPE_NONE) {
            updateMailbox(mailbox, accountId, mailboxPath, delimiter,
                    selectable, LegacyConversions.inferMailboxTypeFromName(
                            context, mailboxPath));
        } else {
            updateMailbox(mailbox, accountId, mailboxPath, delimiter,
                    selectable, mailboxType);
        }

        if ((folder.getLocalHashes()) == null) {
            // new mailbox
            // save hash after updating. allows tracking changes if the mailbox
            // is saved
            // outside of #saveMailboxList()
            folder.setLocalHashes(mailbox.getHashes());
            // We must save this here to make sure we have a valid ID for later

            // This is a newly created folder from the server. By definition, if
            // it came from
            // the server, it can be synched. We need to set the uiSyncStatus so
            // that the UI
            // will not try to display the empty state until the sync completes.
            mailbox.mUiSyncStatus = EmailContent.SYNC_STATUS_INITIAL_SYNC_NEEDED;
            mailbox.save(context);
        }
        folder.setLocalMailbox(mailbox);
        return folder;
    }

    /**
     * Updates the fields within the given mailbox. Only the fields that are
     * important to non-EAS accounts are modified.
     */
    private static void updateMailbox(Mailbox mailbox, long accountId,
            String mailboxPath, char delimiter, boolean selectable, int type) {
        mailbox.mAccountKey = accountId;
        mailbox.mDelimiter = delimiter;
        String displayPath = mailboxPath;
        int pathIndex = mailboxPath.lastIndexOf(delimiter);
        if (pathIndex > 0) {
            displayPath = mailboxPath.substring(pathIndex + 1);
        }
        mailbox.mDisplayName = displayPath;
        if (selectable) {
            mailbox.mFlags = Mailbox.FLAG_HOLDS_MAIL
                    | Mailbox.FLAG_ACCEPTS_MOVED_MAIL;
        }
        mailbox.mFlagVisible = true;
        // mailbox.mParentKey;
        // mailbox.mParentServerId;
        mailbox.mServerId = mailboxPath;
        // mailbox.mServerId;
        // mailbox.mSyncFrequency;
        // mailbox.mSyncKey;
        // mailbox.mSyncLookback;
        // mailbox.mSyncTime;
        mailbox.mType = type;
        // box.mUnreadCount;
    }

    private ImapFolder addSystemDefinedMailbox(Context context, long accountId,
            String mailboxPath, char delimiter, boolean selectable,
            Mailbox mailbox, String mailboxServerId, ImapStore imp) {
        ImapFolder folder = (ImapFolder) imp.getFolder(mailboxPath);
        if (mailbox == null) {
            mailbox = Mailbox
                    .getMailboxForPath(context, accountId, mailboxPath);
        }
        if (mailbox.isSaved()) {
            folder.setLocalHashes(mailbox.getHashes());
        }
        int type = LegacyConversions.inferMailboxTypeFromName(context,
                mailboxPath);
        updateMailboxForOperator(mailbox, accountId, mailboxServerId,
                delimiter, selectable, type);
        if ((folder.getLocalHashes()) == null) {
            folder.setLocalHashes(mailbox.getHashes());
            mailbox.save(context);
        }
        folder.setLocalMailbox(mailbox);
        return folder;
    }

    protected static void updateMailboxForOperator(Mailbox mailbox,
            long accountId, String mailboxPath, char delimiter,
            boolean selectable, int type) {
        mailbox.mAccountKey = accountId;
        mailbox.mDelimiter = delimiter;
        if (selectable) {
            mailbox.mFlags = Mailbox.FLAG_HOLDS_MAIL
                    | Mailbox.FLAG_ACCEPTS_MOVED_MAIL;
        }
        mailbox.mFlagVisible = true;
        mailbox.mServerId = mailboxPath;
        mailbox.mType = type;
    }
}
