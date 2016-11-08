package com.mediatek.calendar.hotknot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import com.android.calendar.EventInfoFragment;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKUtils;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.hotknot.HotKnotMessage;

public class HotKnotHandler {

    private static final String TAG = "HotKnotHandler";

    private static final String ACTION_SHARE = "com.mediatek.hotknot.action.SHARE";
    private static final String EXTRA_SHARE_MSG = "com.mediatek.hotknot.extra.SHARE_MSG";

    public static void hotKnotInit(Activity activity) {
        HotKnotAdapter hotKnotAdapter = HotKnotAdapter.getDefaultAdapter(activity);
        if (hotKnotAdapter == null) {
            LogUtil.d(TAG, "hotKnotInit disable hotKnot feature");
            return;
        }
        LogUtil.d(TAG, "hotKnotInit completed");
        hotKnotAdapter.setOnHotKnotCompleteCallback(
                new HotKnotAdapter.OnHotKnotCompleteCallback() {
                    public void onHotKnotComplete(int reason) {
                        LogUtil.d(TAG, "onHotKnotComplete reason:" + reason);
                    }
                }, activity);
    }

    public static void hotKnotSend(Activity activity,
            EventInfoFragment eventInfoFragment) {
        LogUtil.d(TAG, "hotKnotSend Start ACTION_SHARE");
        Intent intent = new Intent(ACTION_SHARE);
        intent.putExtra(EXTRA_SHARE_MSG,
                setCalendarHotKnotMessage(activity, eventInfoFragment));
        activity.startActivity(intent);
    }

    public static HotKnotMessage setCalendarHotKnotMessage(Activity activity,
            EventInfoFragment eventInfoFragment) {
        // Get the current event URI
        Uri eventUri = eventInfoFragment.getUri();
        ContentResolver resolver = activity.getContentResolver();
        if (eventUri != null) {
            final String eventId = eventUri.getLastPathSegment();
            LogUtil.i(TAG, "createHotKnotMessage, eventId = " + eventId);
            final Uri shareUri = Uri.parse(MTKUtils.VCALENDAR_URI + eventId);

            ByteArrayOutputStream hotKnotBytes = new ByteArrayOutputStream();
            InputStream vcalendarInputStream = null;
            byte[] buffer = new byte[1024];
            try {
                vcalendarInputStream = resolver.openInputStream(shareUri);
                if (vcalendarInputStream == null) {
                    LogUtil.i(TAG, "createHotKnotMessage, vcalendarInputStream = null");
                    return null;
                }
                try {
                    int r;
                    while ((r = vcalendarInputStream.read(buffer)) > 0) {
                        hotKnotBytes.write(buffer, 0, r);
                    }
                    HotKnotMessage message = new HotKnotMessage(
                            MTKUtils.VCALENDAR_TYPE, hotKnotBytes.toByteArray());
                    LogUtil.i(TAG, "createHotKnotMessage, message = " + message);
                    return message;
                } finally {
                    vcalendarInputStream.close();
                }
            } catch (IOException e) {
                LogUtil.e(TAG, "IOException creating vcalendar.");
                return null;
            }
        } else {
            LogUtil.w(TAG, "No event URI to share.");
            return null;
        }
    }
}
