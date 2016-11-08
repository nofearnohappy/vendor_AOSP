package com.mediatek.hotknotbeam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Log;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.FailureReason;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;


/**
 * Update {@link NotificationManager} to reflect current {@link DownloadInfo}
 * states. Collapses similar downloads into a single notification, and builds
 * {@link PendingIntent} that launch towards {@link DownloadReceiver}.
 */
public class DownloadNotifier {
    private final static String TAG = HotKnotBeamService.TAG;
    private final Context mContext;
    private final NotificationManager mNotifManager;

    private String mProgressFormat = null;


    public DownloadNotifier(Context context) {
        mContext = context;
        mNotifManager = (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
    }

    public void cancelAll() {
        mNotifManager.cancelAll();
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link DownloadInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(Collection<DownloadInfo> downloads) {
        final Resources res = mContext.getResources();

        try {
            // Build notification for each cluster
            for (Iterator infoIterator = downloads.iterator(); infoIterator.hasNext(); ) {
                DownloadInfo info = (DownloadInfo) infoIterator.next();
                String tagName = info.getTag();
                Log.d(TAG, "[DL]download info:" + info);

                final Notification.Builder builder = new Notification.Builder(mContext);

                builder.setWhen(System.currentTimeMillis());
                builder.setPriority(Notification.PRIORITY_HIGH);
                //builder.setPriority(Notification.PRIORITY_DEFAULT);

                // Show relevant icon & Build action intents
                if (info.mState == State.RUNNING || info.mState == State.CONNECTING) {
                    builder.setSmallIcon(R.drawable.stat_sys_download);
                    Log.d(TAG, "running : " + info.mId);

                    if (info.isGroup()) {
                        builder.setContentTitle(res.getString(R.string.notification_group_title,
                            info.mCount));
                    } else {
                        builder.setContentTitle(info.getTitle());
                    }

                    //Configure UI intent
                    if (info.isShowNotification()) {
                        Intent resultIntent = new Intent(mContext, HotKnotBeamRxActivity.class);
                        resultIntent.putExtra(DownloadInfo.EXTRA_ITEM_ID, info.mId);
                        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0,
                            resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(resultPendingIntent);
                    }

                    //Configure Cacnel procedure
                    final Intent intent = new Intent(HotKnotBeamService.HOTKNOT_BEAMING);
                    intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_BEAM_ID, info.mId);
                    PendingIntent cancelIntent = PendingIntent.getBroadcast(mContext,
                            info.mId, intent, 0);

                    String cancelWord = mContext.getString(android.R.string.cancel);
                    builder.setAutoCancel(false);
                    builder.setTicker(res.getText(R.string.notification_receiving));
                    builder.addAction(R.drawable.ic_action_cancel, (CharSequence) cancelWord,
                            cancelIntent);

                    if (info.isDead()) {
                        info.setFailReason(FailureReason.UNKNOWN_ERROR);
                    } else {
                        // Calculate and show progress
                        String percentText = null;
                        int percent = info.getPercent();
                        percentText = res.getString(R.string.percent, percent);

                        if (info.isGroup()) {
                            String contentText = "[" + percentText + "]" + info.getTitle();
                            builder.setContentText(contentText);
                            builder.setContentInfo(res.getString(R.string.group_info, info.mOrder,
                                    info.mCount));
                        } else {
                            builder.setContentText(getDowloadRemadingText(info.mCurrentBytes,
                                    info.mTotalBytes));
                            builder.setContentInfo(percentText);
                        }

                        Log.d(TAG, "percent:" + percent);
                        builder.setProgress(100, percent, false);
                    }
                } else if (info.mState == State.COMPLETE) {
                    Log.d(TAG, "COMPLETE : " + info.mId);
                    String mimeType = info.getMimeType();
                    String appIntent = info.getAppIntent();
                    boolean isCheck = info.isMimeTypeCheck();
                    Uri uri = info.getUri();

                    if (info.getResult()) {
                        final Intent intent = new Intent(HotKnotBeamService.HOTKNOT_DL_COMPLETE,
                                uri, mContext, HotKnotBeamReceiver.class);
                        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_INTENT, appIntent);
                        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_URI, uri);
                        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_MIMETYPE, mimeType);
                        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_ISCHECK, isCheck);

                        builder.setContentIntent(PendingIntent.getBroadcast(mContext, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                        builder.setSmallIcon(R.drawable.stat_sys_download_done_static);

                        if (info.isGroup()) {
                            builder.setContentTitle(res.getString(
                                R.string.notification_group_received, info.mCount));
                        } else {
                            builder.setContentTitle(info.getTitle());
                        }

                        builder.setContentText(res.getText(R.string.notification_download_done));
                        builder.setTicker(res.getText(R.string.notification_received));
                    } else {
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
                final Notification notif = builder.build();
                mNotifManager.notifyAsUser(tagName, 0, notif, UserHandle.CURRENT);
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
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
