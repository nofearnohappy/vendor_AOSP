package com.hesine.nmsg.ui;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Statistics;

public final class NmsgNotification {
    private static NmsgNotification mInstance = null;
    private static Context mContext = null;
    private static NotificationManager mNotificationManager = null;

    private NmsgNotification() {
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static NmsgNotification getInstance(Context c) {
        if (null == mInstance) {
            if (c != null) {
                mContext = c.getApplicationContext();
            }
            mInstance = new NmsgNotification();
        }
        return mInstance;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void showNmsgNotification(long threadId, String account, String content) {
        MLog.info("showNotification " + "title:" + account + "  content:" + content);
        ServiceInfo serviceInfo = null;
        String ticker = null;
        String name = null;
        String title = null;
        long notificationId = 0;
        if (account != null) {
            serviceInfo = DBUtils.getServiceInfo(account);
            if (serviceInfo != null) {
                name = serviceInfo.getName();
                notificationId = serviceInfo.getId();
            }
        }

        if (name != null) {
            ticker = name + " : " + content;
            title = name;
        } else {
            ticker = account + " : " + content;
            title = account;
        }

        int msgNumber = DBUtils.getUnreadNumViaAccount(account);
        if (msgNumber > 1) {
            content = msgNumber + mContext.getString(R.string.new_msg);
            removeNotification((int) notificationId);
        }

        Bitmap btm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        Intent resultIntent = new Intent(mContext,
                com.hesine.nmsg.ui.ConversationActivity.class);
        resultIntent.putExtra("open_type", Statistics.OpenType.NOTIFICATION);
        resultIntent.putExtra("phone_number", account);
        resultIntent.putExtra("thread_id", threadId);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                (int) notificationId, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        int defaultsValue = 0;
        final AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        switch (audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            case AudioManager.VIBRATE_SETTING_ON:
                defaultsValue = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
                break;
            case AudioManager.VIBRATE_SETTING_OFF:
            case AudioManager.VIBRATE_SETTING_ONLY_SILENT:
            default:
                defaultsValue = Notification.DEFAULT_SOUND;
                break;
        }

        Notification noti = new Notification.Builder(mContext).setContentTitle(title)
                .setContentText(content).setSmallIcon(R.drawable.notification).setTicker(ticker)
                .setLargeIcon(btm).setDefaults(defaultsValue).setAutoCancel(true)
                .setContentIntent(resultPendingIntent).build();

        mNotificationManager.notify((int) notificationId, noti);
    }

    @SuppressLint("NewApi")
    public void showApkDownloadNotification(String url, String ticker, String title, String content) {
        Bitmap btm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);
        Intent resultIntent = new Intent();
        resultIntent.setAction(Intent.ACTION_VIEW);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri contenUri = Uri.parse(url);
        resultIntent.setData(contenUri);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification noti = new Notification.Builder(mContext).setContentTitle(title)
                .setContentText(content).setSmallIcon(R.drawable.notification).setTicker(ticker)
                .setLargeIcon(btm).setDefaults(Notification.DEFAULT_VIBRATE).setAutoCancel(true)
                .setContentIntent(resultPendingIntent).build();
        mNotificationManager.notify(0, noti);
    }

    public void removeNotification(int notificationId) {
        mNotificationManager.cancel(notificationId);
    }

    public void removeNotification(String account) {
        long notificationId = 0;
        ServiceInfo serviceInfo = null;
        if (account != null) {
            serviceInfo = DBUtils.getServiceInfo(account);
            if (serviceInfo != null) {
                notificationId = serviceInfo.getId();
            }
        }
        mNotificationManager.cancel((int) notificationId);
    }
}
