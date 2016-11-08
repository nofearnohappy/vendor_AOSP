package com.mediatek.mms.plugin;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import com.mediatek.mms.ext.DefaultOpMessagingNotificationExt;

/**
 * OP09MessagingNotificationExt.
 */
public class OP09MessagingNotificationExt extends DefaultOpMessagingNotificationExt {

    private static final int TONE_FULL_VOLUME = 100;
    private static final int MUTE_TONE_LENGTH_MS = 500;
    private static ToneGenerator sMuteModeToneGenerator;
    private static BluetoothHeadset sBluetoothHeadset;

    public OP09MessagingNotificationExt(Context context) {
        super(context);
        if (sMuteModeToneGenerator != null) {
            try {
                synchronized (sMuteModeToneGenerator) {
                    sMuteModeToneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,
                            TONE_FULL_VOLUME);
                }
            } catch (RuntimeException e) {
                sMuteModeToneGenerator = null;
            }
        }
    }

    @Override
    public void onUpdateNotification(boolean isNew) {
        if (isNew) {
            wakeUpScreen(this);
        }
    }

    private static void wakeUpScreen(Context context) {
        if (context == null) {
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean hasInsertedHeadSet = audioManager.isWiredHeadsetOn();
        boolean headsetIsOn = isBluetoothHandsetOn(context);
        if (hasInsertedHeadSet || headsetIsOn) {
            PowerManager powerManager =
                    (PowerManager) (context.getSystemService(Context.POWER_SERVICE));
            PowerManager.WakeLock wakeLock = null;
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "MMS_wake_lock");
            long wakeUpTime = 0;
        try {
                ContentResolver cr = context.getContentResolver();
                wakeUpTime = android.provider.Settings.System.getInt(cr,
                        Settings.System.SCREEN_OFF_TIMEOUT);
            } catch (SettingNotFoundException e) {
            }
            wakeLock.acquire(wakeUpTime);

            /// M: Play tone when mute or vibrate mode
            if (isMuteOrVibrate(context)) {
                if (sMuteModeToneGenerator != null) {
                    sMuteModeToneGenerator.startTone(
                            ToneGenerator.TONE_SUP_DIAL, MUTE_TONE_LENGTH_MS);
                }
            }
        }
    }


    /**
     * M: Check the bluetoothHandset whether has been connected or not.
     * @param context
     * @return
     */
    private static boolean isBluetoothHandsetOn(Context context) {
        ///M: get default bluetoothAdapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ///M: bluetoothprofile service listener
        BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    sBluetoothHeadset = (BluetoothHeadset) proxy;
                }
            }
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    sBluetoothHeadset = null;
                }
            }
        };
        // Establish connection to the proxy
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET);
        boolean headsetIsOn = false;
        if (sBluetoothHeadset != null) {
            List<BluetoothDevice> devicess = sBluetoothHeadset.getConnectedDevices();
            if (devicess != null && devicess.size() > 0) {
                for (BluetoothDevice device : devicess) {
                    int connectState = sBluetoothHeadset.getConnectionState(device);
                    if (connectState == BluetoothHeadset.STATE_DISCONNECTED
                            || connectState == BluetoothHeadset.STATE_DISCONNECTING) {
                        headsetIsOn = false;
                    } else {
                        headsetIsOn = true;
                        break;
                    }
                }
            } else {
                headsetIsOn = false;
            }
        } else {
            headsetIsOn = false;
        }
        try {
            // Close proxy connection after use.
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, sBluetoothHeadset);
        } catch (IllegalArgumentException e) {
        }
        return headsetIsOn;
    }

    private static boolean isMuteOrVibrate(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return AudioManager.RINGER_MODE_SILENT == audioManager.getRingerMode()
                || AudioManager.RINGER_MODE_VIBRATE == audioManager.getRingerMode()
                || 0 == audioManager.getStreamVolume(AudioManager.STREAM_RING);
    }
}
