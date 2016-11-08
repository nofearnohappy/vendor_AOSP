package com.mediatek.email.plugin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.emailcommon.provider.Mailbox;

import com.mediatek.common.PluginImpl;
import com.mediatek.email.ext.ISendNotification;
import com.mediatek.op09.plugin.R;

import java.util.HashMap;

/**
 * Plugin for sending notification
 */
@PluginImpl(interfaceName="com.mediatek.email.ext.ISendNotification")
public class SendNotificationPlugin extends ContextWrapper implements ISendNotification {
    private Notification.Builder mSendingBuilder;

    // The count of mails in sending
    private volatile int mSendCount = 0;
    // The count of mails sent failed
    private int mFailedCount = 0;
    // The count of mails sent successfully
    private int mCompletedCount = 0;

    // Thread for showing and maintaining the progress bar of mail sending
    private ProgressThread mProgressThread;

    // The progress of the whole sending process
    private volatile int mProgress = 0;
    // The progress length of one mail
    private volatile int mMailProgress = 0;

    // Event types definition
    public static final int SEND_MAIL = 0;
    public static final int SEND_FAILED = 1;
    public static final int SEND_COMPLETE = 2;

    // The account id of the last failed mail
    private long mFailedAccountId = -1;

    // Map to store the sending mail count of each account
    private HashMap<Long, Integer> mSendingCountMap = new HashMap<Long, Integer>();

    private NotificationManager mNotificationManager;

    // Define the notification id as a less likely duplicated one
    private static final int NOTIFICATION_ID_SEND_EMAIL = 0x00001000;

    // The length of the whole progress bar
    private static final int BAR_LENGTH = 10000;

    // The progress thread name, used for debugging
    private static final String THREAD_NAME = "Sending Notification Progress Thread";

    private static final String EMAIL_MODULE_NAME = "com.android.email";

    private static final long ACCOUNT_ID_COMBINED_VIEW = 0x1000000000000000L;

    private static final long QUERY_ALL_OUTBOX = -6;

    private static final String TAG = "SendNotificationPlugin";

    private static final int TYPE_OUTBOX = 4;

    public SendNotificationPlugin(Context base) {
        super(base);
    }

    @Override
    public synchronized void showSendingNotification(Context context, long accountId, int eventType,
            int messageCount) {
        // Check the validity of the parameters
        if (accountId <= 0 || context == null) {
            return;
        }

        // Get the NotificationManager for showing the sending notification
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        }

        // Get the context of Email process for accessing the resource from Email
        Context baseContext = getBaseContext();

        if (eventType == SEND_MAIL) {
            // This section is to get the real new sent mail count because
            // parameter "messageCount" may includes some mails already in sending
            int newMessageCount = messageCount;
            Integer sendingCount = mSendingCountMap.get(accountId);
            if (sendingCount != null) {
                messageCount -= sendingCount;
                // duplicate sending request
                if (messageCount == 0) {
                    Log.d(TAG, "duplicate sending request, sendingCount=" + sendingCount);
                    return;
                }
            }
            // Update the sending count for the account
            if (newMessageCount > 0) {
                mSendingCountMap.put(accountId, newMessageCount);
            } else {
                mSendingCountMap.remove(accountId);
            }
            // Start to show the notification
            if (mSendingBuilder == null) {
                int resId = context.getResources().getIdentifier("ic_notification_sending", "drawable", EMAIL_MODULE_NAME);
                mSendingBuilder = new Notification.Builder(context).setContentTitle(
                        baseContext.getResources().getString(R.string.sending_mail_title)).setContentText("")
                        .setWhen(System.currentTimeMillis()).setSmallIcon(resId)
                        .setTicker(baseContext.getResources().getString(R.string.sending_mail_title))
                        .setOngoing(true);
                // Sent failed notification was removed when new sending started, so reset the failed account id
                mFailedAccountId = -1;
            }

            // Enable user to open the related outbox as tap the sending notification
            PendingIntent pending = getOutboxIntent(context, mSendingCountMap.size() > 1 ?
                    ACCOUNT_ID_COMBINED_VIEW : accountId);
            if (pending == null) {
                cancelSendingNotification();
                return;
            }
            mSendingBuilder.setContentIntent(pending);

            // Update the total sending count
            mSendCount += messageCount;
            if (mSendCount <= 0) {
                Log.d(TAG, "Cancel sending notification because has no sending messages," +
                        " mSendCount=" + mSendCount);
                cancelSendingNotification();
                return;
            }
            // Caculator the progress length of one mail
            mMailProgress = BAR_LENGTH / mSendCount;
            // Set the minimum of the progress length of one mail
            if (mMailProgress < 1) {
                mMailProgress = 1;
            }
            // Update the notification content info (completed count/full count)
            mSendingBuilder.setContentInfo(mCompletedCount + "/" + mSendCount);

            // Start a seperate thread to continuously update the notification progress bar
            if (mProgressThread == null) {
                mProgressThread = new ProgressThread(THREAD_NAME);
                mProgressThread.start();
            } else {
                synchronized (mProgressThread) {
                    mProgressThread.notify();
                }
            }

            return;
        }

        // Just for checking the special case that the notification was already cancelled
        // when "low storage" happens
        if (mSendingBuilder == null) {
            return;
        }

        if (eventType == SEND_COMPLETE) {
            mCompletedCount++;
            // // Update the notification content info (completed count/full count)
            if (mCompletedCount < mSendCount) {
                mSendingBuilder.setContentInfo(mCompletedCount + "/" + mSendCount);
            }

            // Notify the progress thread
            synchronized (mProgressThread) {
                mProgress += mMailProgress;
                mProgressThread.notify();
            }

            // Update the sending count of current account
            if (mSendingCountMap.containsKey(accountId)) {
              int newMessageCount = mSendingCountMap.get(accountId) - messageCount;
              if (newMessageCount > 0) {
                  mSendingCountMap.put(accountId, newMessageCount);
              } else {
                  mSendingCountMap.remove(accountId);
              }
          }
        }

        if (eventType == SEND_FAILED) {
            mFailedCount += messageCount;
            // Just remember the account id of last failed mail
            mFailedAccountId = accountId;
        }

        // Finished the whole sending
        if (mCompletedCount + mFailedCount >= mSendCount) {
            // Stop the progress bar thread
            synchronized (mProgressThread) {
                mProgressThread.mStop = true;
                mProgressThread.notify();
            }

            // At least one mail had been sent failed
            if (mFailedCount > 0) {
                long outboxId = Mailbox.findMailboxOfType(context, mFailedAccountId, TYPE_OUTBOX);
                if (outboxId == SendNotificationUtils.NO_MAILBOX) {
                    cancelSendingNotification();
                    return;
                }
                // Tap the failed-notification can open the outbox of the last failed account
                PendingIntent pending = getOutboxIntent(context, accountId);
                if (pending == null) {
                    cancelSendingNotification();
                    return;
                }

                // Resend the notification for the new ticker
                mNotificationManager.cancel(NOTIFICATION_ID_SEND_EMAIL);
                int resId = context.getResources().getIdentifier("ic_notification_send_failed", "drawable", EMAIL_MODULE_NAME);
                mSendingBuilder.setContentInfo(null).setContentTitle(
                        baseContext.getResources().getString(R.string.sent_failed_title)).setContentText(
                                baseContext.getResources().getQuantityString(R.plurals.sent_failed_text,
                                        mFailedCount, mFailedCount)).setTicker(baseContext.getResources().getString(R.string.sent_failed_title))
                                        .setContentIntent(pending).setSmallIcon(resId).
                                        setWhen(System.currentTimeMillis()).setProgress(0, 0, false).setOngoing(false);
            } else {  // All mails sent successfully
                int resId = context.getResources().getIdentifier("ic_notification_send_succeed", "drawable", EMAIL_MODULE_NAME);
                mSendingBuilder.setContentInfo(null).setContentTitle(
                        baseContext.getResources().getString(R.string.sent_success_title)).setContentText(
                                baseContext.getResources().getQuantityString(R.plurals.sent_success_text,
                                        mCompletedCount, mCompletedCount)).setSmallIcon(resId).setContentIntent(null)
                                        .setProgress(0, 0, false).setOngoing(false);
                // The successful-notification would be disappeared in 3 seconds automatically
                Handler handler = new Handler(Looper.getMainLooper());
                boolean res = handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mNotificationManager.cancel(NOTIFICATION_ID_SEND_EMAIL);
                    }
                }, 3000);
            }
            // Update the notification appearance
            mNotificationManager.notify(NOTIFICATION_ID_SEND_EMAIL, mSendingBuilder.build());

            // Reset all the related variables
            mSendingCountMap.clear();
            mCompletedCount = 0;
            mFailedCount = 0;
            mSendCount = 0;
            mMailProgress = 0;
            mSendingBuilder = null;
            mProgressThread = null;
            mProgress = 0;
        }
    }

    private class ProgressThread extends Thread {
        // Indicate the whether this thread should be stopped
        public boolean mStop = false;
        // Current sending progress illustrated by the progress bar
        private int currentProgress;
        // Current sending count illustrated by the progress bar
        private int sendingCount;

        private static final int STEP_FACTOR = 40;

        ProgressThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!mStop) {
                if (sendingCount != mSendCount) { // New mail(s) will be sent
                    mProgress = mProgress * sendingCount / mSendCount;
                    currentProgress = currentProgress * sendingCount / mSendCount;
                    sendingCount = mSendCount;
                    if (mSendingBuilder != null) {
                        mSendingBuilder.setProgress(BAR_LENGTH, currentProgress, false);
                    }
                } else { // One mail sent completely
                    currentProgress = mProgress;
                    if (mSendingBuilder != null) {
                        mSendingBuilder.setProgress(BAR_LENGTH, mProgress, false);
                    }
                }

                int initProgress = mProgress;

                mNotificationManager.notify(NOTIFICATION_ID_SEND_EMAIL, mSendingBuilder.build());
                int step = Math.round(((float) mMailProgress) / STEP_FACTOR);
                // Make the step sensible
                if (step == 0) {
                    step = 1;
                }

                // The max progress length that the current mail can go
                int maxProgress = mProgress + mMailProgress - currentProgress;
                int loop = maxProgress / step;
                // Loop to make the progress bar go ahead
                for (int i = 1; i < loop; i++) {
                    // Update the currentProgress
                    currentProgress += step;
                    if (mSendingBuilder != null) {
                        mSendingBuilder.setProgress(BAR_LENGTH, currentProgress, false);
                    }
                    synchronized (this) {
                        // One mail is sent successfully just now
                        if (initProgress != mProgress || mStop || sendingCount != mSendCount) {
                            break;
                        }
                        mNotificationManager.notify(NOTIFICATION_ID_SEND_EMAIL, mSendingBuilder.build());
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Ignore InterruptedException");
                    }
                }
                // Waiting for current mail sent completely, since the progress bar is reach to the
                // limitation for current mail
                try {
                    synchronized (this) {
                        if (!mStop && initProgress == mProgress && sendingCount == mSendCount) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Ignore InterruptedException");
                }
            }
        }
    }

    /**
     * Failed-Notification may cancelled when user opening the outbox of
     * the last failed account
     */
    @Override
    public void suspendSendFailedNotification(long accountId) {
         if (mFailedAccountId != -1 && mFailedAccountId == accountId) {
             mNotificationManager.cancel(NOTIFICATION_ID_SEND_EMAIL);
             mFailedAccountId = -1;
         }
     }

    /**
     * Cancel the notification and reset all the related variables
     */
    @Override
    public synchronized void cancelSendingNotification() {
        // check whether mProgressThread is null first.
        if (mProgressThread != null) {
            synchronized (mProgressThread) {
                mProgressThread.mStop = true;
                mProgressThread.notify();
            }
        }
        // check whether mNotificationManager is null first.
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID_SEND_EMAIL);
        }
        mCompletedCount = 0;
        mFailedCount = 0;
        mSendCount = 0;
        mMailProgress = 0;
        mSendingBuilder = null;
        mProgressThread = null;
        mProgress = 0;
        mSendingCountMap.clear();
        mFailedAccountId = -1;
    }

    /**
     * Return a pendingIntent for opening the outbox of the specific account
     * @param context
     * @param accountId
     * @return the pendingIntent, null if failed to get
     */
    private PendingIntent getOutboxIntent(Context context, long accountId) {
        Intent intent = null;
        long outboxId = SendNotificationUtils.NO_MAILBOX;

        // If account id is for combined account, use QUERY_ALL_OUTBOX as outbox id
        if (accountId == ACCOUNT_ID_COMBINED_VIEW) {
            outboxId = QUERY_ALL_OUTBOX;
        } else {
            outboxId = Mailbox.findMailboxOfType(context, accountId, TYPE_OUTBOX);
            if (outboxId == SendNotificationUtils.NO_MAILBOX) {
                return null;
            }
        }

        intent = SendNotificationUtils.createOpenMailboxIntent(accountId, outboxId);

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            return PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return null;
    }
}
