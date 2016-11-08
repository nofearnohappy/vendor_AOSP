package com.mediatek.hotknotbeam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.util.Log;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.FailureReason;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;


/**
 * Update {@link NotificationManager} to reflect current {@link UploadInfo}
 * states. Collapses similar Uploads into a single notification, and builds
 * {@link PendingIntent} that launch towards {@link UploadReceiver}.
 */
public class UploadNotifier {
    private final static String TAG = HotKnotBeamService.TAG;
    private final Context mContext;
    private final NotificationManager mNotifManager;
    private static boolean mDisable = false;

    private String mProgressFormat = null;


    public UploadNotifier(Context context) {
        mContext = context;
        mNotifManager = (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

        mDisable = SystemProperties.getBoolean("sys.hotknot.tx.disable", false);

    }

    public void cancelAll() {
        mNotifManager.cancelAll();
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link UploadInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(Collection<UploadInfo> Uploads) {
        final Resources res = mContext.getResources();

        if (mDisable) {
            return;
        }

        try {

            // Build notification for each cluster
            for (Iterator infoIterator = Uploads.iterator(); infoIterator.hasNext(); ) {
                UploadInfo info = (UploadInfo) infoIterator.next();
                String tagName = info.getTag();
                Log.d(TAG, "[UL]Upload info:" + info);

                final Notification.Builder builder = new Notification.Builder(mContext);
                builder.setWhen(System.currentTimeMillis());
                //builder.setPriority(Notification.PRIORITY_DEFAULT);
                builder.setPriority(Notification.PRIORITY_HIGH);

                // Show relevant icon & Build action intents
                if (info.mState == State.RUNNING || info.mState == State.CONNECTING) {
                    builder.setSmallIcon(R.drawable.stat_sys_upload);
                    Log.d(TAG, "[UL]running : " + info.mId);

                    if (info.isGroup()) {
                        builder.setContentTitle(res.getString(R.string.notification_group_upload_title, info.mCount));
                    } else {
                        builder.setContentTitle(info.getTitle());
                    }

                    final Intent intent = new Intent(HotKnotBeamService.HOTKNOT_CANCEL_BEAMING);
                    intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_BEAM_ID, info.mId);
                    PendingIntent cancelIntent = PendingIntent.getBroadcast(mContext, info.mId, intent, 0);

                    String cancelWord = mContext.getString(android.R.string.cancel);

                    builder.setAutoCancel(false);
                    builder.setTicker(res.getText(R.string.notification_sending));
                    builder.addAction(R.drawable.ic_action_cancel, (CharSequence) cancelWord, cancelIntent);

                    if (info.isDead()) {
                        info.setFailReason(FailureReason.UNKNOWN_ERROR);
                    }
                } else if (info.mState == State.COMPLETE) {
                    Log.d(TAG, "[UL]complete");

                    if (info.getResult()) { /* Disable sound in Notificaiton*/
                        //builder.setSound(Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.magic));
                        builder.setDefaults(Notification.DEFAULT_SOUND);
                        builder.setSmallIcon(R.drawable.stat_sys_upload_done_static);

                        if (info.isGroup()) {
                            builder.setContentTitle(res.getString(R.string.notification_group_sent, info.mCount));
                        } else {
                            builder.setContentTitle(info.getTitle());
                        }

                        builder.setContentText(res.getText(R.string.notification_sent));
                        builder.setTicker(res.getText(R.string.notification_sent));
                    }
                    else {
                        builder.setSmallIcon(R.drawable.ic_hotknot_fail);
                        builder.setContentTitle(info.getFailureTitle());
                        String reason = info.getFailureText();
                        builder.setContentText(reason);
                        builder.setTicker(reason);
                    }

                    builder.setAutoCancel(true);
                    infoIterator.remove();
                }

                // Build titles and description
                final Notification notif;

                if (info.mState == State.RUNNING || info.mState == State.CONNECTING) {
                    // Calculate and show progress
                    String percentText = null;
                    int percent = info.getPercent();
                    percentText = res.getString(R.string.percent, percent);

                    if (info.isGroup()) {
                        String contentText = "[" + percentText + "]" + info.getTitle();
                        builder.setContentText(contentText);
                        builder.setContentInfo(res.getString(R.string.group_info, info.mOrder, info.mCount));
                    } else {
                        builder.setContentText(getDowloadRemadingText(info.mCurrentBytes, info.mTotalBytes));
                        builder.setContentInfo(percentText);
                    }

                    Log.d(TAG, "[UL]percent:" + percent);
                    builder.setProgress(100, percent, false);
                }

                notif = builder.build();
                mNotifManager.notifyAsUser(tagName, 0, notif, UserHandle.CURRENT);
            }
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "[UL]err msg:" + e.getMessage());
            return;
        }
    }

    private CharSequence getDowloadRemadingText(int progress, int max) {
        if (max > HotKnotBeamConstants.MAX_MB_SIZE) {
            mProgressFormat = HotKnotBeamConstants.MAX_MB_FORMAT;
            progress = (int) progress / HotKnotBeamConstants.MAX_MB_SIZE;
            max = (int) max / HotKnotBeamConstants.MAX_MB_SIZE;
        } else if (max > HotKnotBeamConstants.MAX_KB_SIZE) {
            mProgressFormat = HotKnotBeamConstants.MAX_KB_FORMAT;
            progress = (int) progress / HotKnotBeamConstants.MAX_KB_SIZE;
            max = (int) max / HotKnotBeamConstants.MAX_KB_SIZE;
        } else {
            mProgressFormat = HotKnotBeamConstants.MAX_FORMAT;
        }

        return String.format(mProgressFormat, progress, max);
    }

}
