package com.mediatek.mms.ext;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public interface IOpMessagingNotificationExt {
    /**
     * @internal
     */
    Uri getUndeliveredMessageCount(Uri uri);
    /**
     * @internal
     */
    boolean updateNotification(Context context, int uniqueThreadCount,
            long mostRecentThreadId, Class<?> cls, int messageCount, boolean isMostRecentSms,
            Uri mostRecentUri, Notification.Builder noti);

    /**
     * @internal
     */
    boolean notifyFailed(Context context, Class<?> cls,
            boolean allFailedInSameThread, boolean isDownload,
            long threadId, Notification notification, String title, String description);
    /**
     * @internal
     */
    void onUpdateNotification(boolean isNew);
}
