
package com.mediatek.cellbroadcastreceiver.plugin;


import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;

import com.mediatek.common.PluginImpl;
import com.mediatek.cellbroadcastreceiver.CellBroadcastMainSettings;
import com.mediatek.cmas.ext.DefaultCmasMainSettingsExt;

@PluginImpl(interfaceName = "com.mediatek.cmas.ext.ICmasMainSettingsExt")
public class OP08CmasMainSettingsExt extends DefaultCmasMainSettingsExt {
    
    private static final int PRESIDENT_ALERT_ID = 4370;
    private static final String TAG = "CellBroadcastReceiver/OP08CmasMainSettingsExt";  
    
    public static final String KEY_ALERT_SOUND_VOLUME = "enable_key_sound_volume";
    public static final String KEY_ENABLE_ALERT_VIBRATE = "enable_key_alert_vibrate";
    public static final String KEY_ENABLE_CELLBROADCAST = "enable_cell_broadcast";
    
    private float mAlertVolume = 1.0f;
    private MediaPlayer mMediaPlayer;
    private Context mContext;
   
    public PreferenceActivity mprefActivity;
    
    
    public OP08CmasMainSettingsExt(Context context) {
        super(context);
        mContext = context;
    }
        
    public float getAlertVolume(int msgId) {
        Log.d("@M_" + TAG, "[getAlertVolume]"); 
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        float alertVolume = prefs.getFloat(KEY_ALERT_SOUND_VOLUME, 1.0f);
        
        Log.d("@M_" + TAG, "[getAlertVolume] AlertVolume: " + alertVolume);
        
        if (msgId == PRESIDENT_ALERT_ID && alertVolume == 0.0f) {
            Log.d("@M_" + TAG, "[getAlertVolume] PRESIDENT_ALERT");
            return 1.0f;
        }
        return alertVolume;
    }
    
    public boolean getAlertVibration(int msgId) {
        Log.d("@M_" + TAG, "[getAlertVibration]");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        if (msgId == PRESIDENT_ALERT_ID) {
            Log.d("@M_" + TAG, "[getAlertVibration] PRESIDENT_ALERT");
            return true;
        }
        return prefs.getBoolean(KEY_ENABLE_ALERT_VIBRATE, true);
    }
    
    public boolean setAlertVolumeVibrate(int msgId, boolean currentValue) {
        Log.d("@M_" + TAG, "[setAlertVolumeVibrate]");
        if (msgId == PRESIDENT_ALERT_ID) {
            Log.d("@M_" + TAG, "[setAlertVolume] PRESIDENT_ALERT");
            return true;
        }
        Log.d("@M_" + TAG, "[setAlertVolumeVibrate] return currentValue:" + currentValue);
        return currentValue;
    }

    /**
     * Stop mediplayer when press Home key
     */
     public void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    
    public void addAlertSoundVolumeAndVibration(PreferenceActivity prefActivity) {
        Log.d("@M_" + TAG, "[addAlertSoundVolume]");
        mprefActivity = prefActivity;
        PreferenceScreen prefSet = prefActivity.getPreferenceScreen();          
        
        CheckBoxPreference vibrate = new CheckBoxPreference((CellBroadcastMainSettings) mprefActivity);          
        vibrate.setKey(KEY_ENABLE_ALERT_VIBRATE);
        vibrate.setChecked(true);
        vibrate.setDefaultValue(true);
        vibrate.setTitle(com.mediatek.cellbroadcastreceiver.R.string.enable_alert_vibrate_title);
        vibrate.setSummary(com.mediatek.cellbroadcastreceiver.R.string.enable_alert_vibrate_summary);             
        prefSet.addPreference(vibrate);
        
        vibrate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                
                boolean value = (Boolean) newValue;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putBoolean(KEY_ENABLE_ALERT_VIBRATE, value);
                editor.commit();
                return true;
            }
        });
        
        Preference volume = new Preference((CellBroadcastMainSettings) mprefActivity);
        volume.setKey(KEY_ALERT_SOUND_VOLUME);
        volume.setDefaultValue(1.0f);
        volume.setTitle(com.mediatek.cellbroadcastreceiver.R.string.alert_sound_volume);
        volume.setSummary(com.mediatek.cellbroadcastreceiver.R.string.alert_volume_summary);
        
        prefSet.addPreference(volume);
        Log.d("@M_" + TAG, "addAlertSoundVolume   Preference is added");
        
        OnPreferenceClickListener prefClickListener = new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Log.i("@M_" + TAG, "OnPreferenceClickListener onclicked ");
                final AlertDialog.Builder dialog = new AlertDialog.Builder((CellBroadcastMainSettings) mprefActivity);
                LayoutInflater flater = ((CellBroadcastMainSettings) mprefActivity).getLayoutInflater();
                View v = flater.inflate(com.mediatek.cellbroadcastreceiver.R.layout.alert_dialog_view, null);
                
                SeekBar sb = (SeekBar) v.findViewById(com.mediatek.cellbroadcastreceiver.R.id.seekbar);
                // set bar's progress
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                float pro = 1.0f;
                pro = prefs.getFloat(KEY_ALERT_SOUND_VOLUME, 1.0f);
                int progress = (int) (pro * 100);
                if (progress < 0) {
                    progress = 0;
                } else if (progress > 100) {
                    progress = 100;
                }
                Log.d("@M_" + TAG, "open volume setting,progress:" + progress + ",pro:" + pro);
                sb.setProgress(progress);
                sb.setOnSeekBarChangeListener(getSeekBarListener());
                dialog.setTitle(com.mediatek.cellbroadcastreceiver.R.string.alert_sound_volume)
                .setView(v)
                .setPositiveButton(com.mediatek.cellbroadcastreceiver.R.string.button_dismiss, new OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = prefs.edit();

                        editor.putFloat(KEY_ALERT_SOUND_VOLUME, mAlertVolume);
                        editor.commit();
                        
                        Log.d("@M_" + TAG, "Volume saved:" + mAlertVolume);
                        if (mMediaPlayer != null) {
                            mMediaPlayer.release();
                            mMediaPlayer = null;
                        }
                    }
                })
                .setNegativeButton(com.mediatek.cellbroadcastreceiver.R.string.button_cancel, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int arg1) {
                        // TODO Auto-generated method stub
                        dialog.dismiss();
                        
                        if (mMediaPlayer != null) {
                            mMediaPlayer.release();
                            mMediaPlayer = null;
                        }
                    }
                })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.cancel();

                            if (mMediaPlayer != null) {
                                mMediaPlayer.release();
                                mMediaPlayer = null;
                            }
                        }
                        return false;
                    }
                })
                .show();
                return true;
            }
        };  
        
        volume.setOnPreferenceClickListener(prefClickListener);       
    }
    
    
    private SeekBar.OnSeekBarChangeListener getSeekBarListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAlertVolume = progress / 100.0f;
                    Log.d("@M_" + TAG, "volume:" + mAlertVolume);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("@M_" + TAG, "onStartTrackingTouch");
                
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {                                                 
                    mMediaPlayer.stop();                     
                } else {                
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    try {
                        AssetFileDescriptor afd = ((CellBroadcastMainSettings) mprefActivity).getResources().openRawResourceFd(
                                com.mediatek.cellbroadcastreceiver.R.raw.attention_signal);
                        if (afd != null) {
                            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd
                                    .getStartOffset(), afd.getLength());
                            afd.close();
                        }
                    } catch (IOException e) {
                        Log.e("@M_" + TAG, "IOException onStartTrackingTouch: " + e);
                    }
                    catch (IllegalStateException e) {
                        Log.e("@M_" + TAG, "IllegalStateException onStartTrackingTouch: " + e);
                    }
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // make some sample sound     
                Log.d("@M_" + TAG, "onStopTrackingTouch");
                try {
                    mMediaPlayer.setVolume(mAlertVolume, mAlertVolume);                   
                    mMediaPlayer.prepare();           
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();                                                   
                } catch (IOException e) {
                    Log.e("@M_" + TAG, "exception onStopTrackingTouch: " + e);
                }
                catch (IllegalStateException e) {
                    Log.e("@M_" + TAG, "IllegalStateException onStartTrackingTouch: " + e);
                }
            }
        };
    }
}
