package com.mediatek.mediatekdm.scomo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.R;

public class DmScomoNotification implements IDmScomoStateObserver, IDmScomoDownloadProgressObserver {

    private DmService mService;
    private NotificationManager mNotificationManager;
    private PendingIntent mDownloadDetailPendingIntent;

    public DmScomoNotification(DmService service) {
        this.mService = service;
        mNotificationManager = (NotificationManager) service
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(service, DmScomoDownloadDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mDownloadDetailPendingIntent = PendingIntent.getActivity(service, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void notifyDownloadNotification(CharSequence tickerText, CharSequence contentTitle,
            CharSequence contentText, long currentSize, long totalSize) {
        int progress = (int) ((float) currentSize / (float) totalSize * 100);
        Notification.Builder notifyBuilder = new Notification.Builder(mService).setOngoing(true)
                .setSmallIcon(R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(mDownloadDetailPendingIntent).setTicker(tickerText)
                .setContentTitle(contentTitle).setContentText(contentText)
                .setProgress(100, progress, false);
        mNotificationManager.notify(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION,
                notifyBuilder.build());
    }

    private void notifyConfirmNotification(CharSequence tickerText, CharSequence contentTitle,
            CharSequence contentText, int state, String reason) {
        Intent intent = new Intent(mService, DmScomoConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("action", state);
        Log.d(TAG.SCOMO, "reason is " + reason);
        if (reason != null) {
            intent.putExtra("reason", reason);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(mService, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder notifyBuilder = new Notification.Builder(mService).setAutoCancel(true)
                .setSmallIcon(R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent).setTicker(tickerText)
                .setContentTitle(contentTitle).setContentText(contentText);
        mNotificationManager.notify(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION,
                notifyBuilder.build());
    }

    @Override
    /* IDmScomoStateObserver */
    public void notify(int state, int previousState, DmOperation operation, Object extra) {
        ScomoComponent component = (ScomoComponent) DmApplication.getInstance()
                .findComponentByName(ScomoComponent.NAME);
        DmScomoState scomoState = component.getScomoManager().getScomoState();
        Log.i(TAG.SCOMO, "notification onUpdate: " + state);
        if (state == DmScomoState.DOWNLOADING) {
            CharSequence tickerText = mService.getText(R.string.downloading_scomo);
            String text = "";
            if (scomoState.totalSize == 0) {
                text = "NaN";
            } else {
                text = scomoState.currentSize * 100 / scomoState.totalSize + "%";
            }
            CharSequence tvText = mService.getResources().getString(R.string.downloading,
                    scomoState.getName());
            notifyDownloadNotification(tickerText, text, tvText, scomoState.currentSize,
                    scomoState.totalSize);
        } else if (state == DmScomoState.DOWNLOAD_PAUSED) {
            Log.d(TAG.SCOMO, "SCOMO Notificaition notified with state " + state + " and extra "
                    + extra);
            if (extra != null && ((String) extra).equals("USER_CANCELED")) {
                mNotificationManager.cancel(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION);
            } else {
                CharSequence tickerText = mService.getText(R.string.scomo_downloading_paused);
                CharSequence contentText = scomoState.currentSize / 1024 + "KB/"
                        + scomoState.totalSize / 1024 + "KB";
                CharSequence contentTitle = mService.getText(R.string.scomo_downloading_paused);
                notifyDownloadNotification(tickerText, contentTitle, contentText,
                        scomoState.currentSize, scomoState.totalSize);
            }
        } else if (state == DmScomoState.DOWNLOAD_FAILED) {
            Intent intent = new Intent(mService, DmScomoConfirmActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("action", state);
            mService.startActivity(intent);
        } else if (state == DmScomoState.NEW_DP_FOUND) {
            CharSequence tickerText = mService.getText(R.string.new_scomo_available);
            CharSequence contentText = mService.getString(R.string.new_scomo_available);
            CharSequence contentTitle = scomoState.getName();
            notifyConfirmNotification(tickerText, contentTitle, contentText, state, null);
        } else if (state == DmScomoState.CONFIRM_INSTALL) {
            CharSequence tickerText = mService.getText(R.string.confirm_update_scomo);
            CharSequence contentText = mService.getText(R.string.confirm_update_scomo);
            CharSequence contentTitle = scomoState.getName();
            notifyConfirmNotification(tickerText, contentTitle, contentText, state, null);
        } else if (state == DmScomoState.IDLE) {
            Log.d(TAG.SCOMO, "Scomo notification extra is " + extra);
            if (extra != null) {
                String reason = (String) extra;
                if (reason.equals("DM_FAILED") || reason.equals("FAILED")) {
                    CharSequence tickerText = mService.getText(R.string.scomo_failed);
                    CharSequence contentText = mService.getText(R.string.software_update);
                    CharSequence contentTitle = mService.getText(R.string.scomo_failed);
                    notifyConfirmNotification(tickerText, contentTitle, contentText, state, reason);
                } else if (reason.equals("INSTALL_OK")) {
                    CharSequence tickerText = mService.getText(R.string.scomo_install_ok);
                    CharSequence contentText = mService.getText(R.string.software_update);
                    CharSequence contentTitle = mService.getText(R.string.scomo_install_ok);
                    notifyConfirmNotification(tickerText, contentTitle, contentText, state, reason);
                } else if (reason.equals("INSTALL_FAILED")) {
                    CharSequence tickerText = mService.getText(R.string.scomo_install_failed);
                    CharSequence contentText = mService.getText(R.string.software_update);
                    CharSequence contentTitle = mService.getText(R.string.scomo_install_failed);
                    notifyConfirmNotification(tickerText, contentTitle, contentText, state, reason);
                } else {
                    mNotificationManager
                            .cancel(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION);
                }
            } else {
                mNotificationManager.cancel(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION);
            }
        } else if (state == DmScomoState.INSTALLING) {
            Intent intent = new Intent(mService, DmScomoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mService, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            Notification.Builder notifyBuilder = new Notification.Builder(mService)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.stat_download_waiting)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .setTicker(mService.getText(R.string.installing_scomo))
                    .setContentTitle(mService.getText(R.string.installing_scomo))
                    .setContentText(
                            mService.getResources().getString(R.string.installing,
                                    scomoState.getName()));
            mNotificationManager.notify(NotificationInteractionType.TYPE_SCOMO_NOTIFICATION,
                    notifyBuilder.build());
        }
    }

    @Override
    /* IDmScomoDownloadProgressObserver */
    public void updateProgress(long current, long total) {
        ScomoComponent component = (ScomoComponent) DmApplication.getInstance()
                .findComponentByName(ScomoComponent.NAME);
        DmScomoState scomoState = component.getScomoManager().getScomoState();
        CharSequence tickerText = mService.getText(R.string.downloading_scomo);
        String text = "";
        if (scomoState.totalSize == 0) {
            text = "NaN";
        } else {
            text = scomoState.currentSize * 100 / scomoState.totalSize + "%";
        }
        CharSequence tvText = mService.getResources().getString(R.string.downloading,
                scomoState.getName());
        notifyDownloadNotification(tickerText, text, tvText, scomoState.currentSize,
                scomoState.totalSize);
    }

}
