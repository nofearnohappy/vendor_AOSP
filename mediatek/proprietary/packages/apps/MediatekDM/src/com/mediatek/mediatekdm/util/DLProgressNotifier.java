package com.mediatek.mediatekdm.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.R;

public class DLProgressNotifier {
    private static final int DOWNLOADING_ICON = R.drawable.stat_download_waiting;
    private static final int DOWNLOADING_STR = R.string.status_bar_notifications_downloading;
    private static final int DOWNLOADING_NOTIFY_TYPE = NotificationInteractionType.TYPE_FUMO_DOWNLOADING;

    private NotificationManager mNotifyManager;
    private Notification.Builder mNotifyBuilder;

    public DLProgressNotifier(Context context, PendingIntent intent) {
        mNotifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyBuilder = new Notification.Builder(context);
        mNotifyBuilder.setOngoing(true).setSmallIcon(DOWNLOADING_ICON)
                .setContentTitle(context.getString(DOWNLOADING_STR))
                .setWhen(System.currentTimeMillis()).setContentIntent(intent)
                .setProgress(100, 0, false);
    }

    public void onProgressUpdate(long currentSize, long totalSize) {
        int progress = (int) ((float) currentSize / (float) totalSize * 100);
        String percent = String.valueOf(progress) + "%";

        mNotifyBuilder.setProgress(100, progress, false);
        mNotifyBuilder.setContentInfo(percent);
        mNotifyManager.notify(DOWNLOADING_NOTIFY_TYPE, mNotifyBuilder.build());
    }

    public void onFinish() {
        Log.d(TAG.COMMON, "### download notification cancelled ###");
        mNotifyManager.cancel(DOWNLOADING_NOTIFY_TYPE);
    }
}
