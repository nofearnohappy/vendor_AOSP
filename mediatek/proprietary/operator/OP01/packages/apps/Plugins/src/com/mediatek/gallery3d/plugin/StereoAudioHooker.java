package com.mediatek.gallery3d.plugin;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.mediatek.op01.plugin.R;

/**
 * OP01 plugin implementation of StereoAudioHooker.
 */
public class StereoAudioHooker extends PluginBaseHooker {
    private static final String TAG = "Gallery2/VideoPlayer/StereoAudioHooker";
    private static final boolean LOG = true;

    private static final int MENU_STEREO_AUDIO = 1;
    private MenuItem mMenuStereoAudio;

    private static final String KEY_STEREO = "EnableStereoOutput";
    private boolean mSystemStereoAudio;
    private boolean mCurrentStereoAudio;
    private boolean mIsInitedStereoAudio;
    private AudioManager mAudioManager;

    /**
     * @hide
     *
     * @param context context instance
     */
    public StereoAudioHooker(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        enableStereoAudio();
    }
    @Override
    public void onStop() {
        super.onStop();
        restoreStereoAudio();
    }
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuStereoAudio = menu.add(MENU_HOOKER_GROUP_ID,
         getMenuActivityId(MENU_STEREO_AUDIO), 0, mPluginContext.getString(R.string.single_track));
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateStereoAudioIcon();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_STEREO_AUDIO:
            mCurrentStereoAudio = !mCurrentStereoAudio;
            setStereoAudio(mCurrentStereoAudio);
            updateStereoAudioIcon();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        if (mMenuStereoAudio != null) {
            mMenuStereoAudio.setVisible(visible);
        }
    }

    private boolean getStereoAudio() {
        boolean isstereo = false;
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        final String stereo = mAudioManager.getParameters(KEY_STEREO);
        final String key = KEY_STEREO + "=1";
        if (stereo != null && stereo.indexOf(key) > -1) {
            isstereo = true;
        } else {
            isstereo = false;
        }
        if (LOG) {
            Log.v(TAG, "getStereoAudio() isstereo=" + isstereo + ", stereo="
                  + stereo + ", key=" + key);
        }
        return isstereo;
    }

    private void setStereoAudio(final boolean flag) {
        final String value = KEY_STEREO + "=" + (flag ? "1" : "0");
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        mAudioManager.setParameters(value);
        if (LOG) {
            Log.v(TAG, "setStereoAudio(" + flag + ") value=" + value);
        }
    }

    private void updateStereoAudioIcon() {
        if (mMenuStereoAudio != null) {
            if (mCurrentStereoAudio) {
                mMenuStereoAudio.setTitle(mPluginContext.getString(R.string.single_track));
            } else {
                mMenuStereoAudio.setTitle(mPluginContext.getString(R.string.stereo));
            }
        }
    }

    private void enableStereoAudio() {
        if (LOG) {
            Log.v(TAG, "enableStereoAudio() mIsInitedStereoAudio=" + mIsInitedStereoAudio
                    + ", mCurrentStereoAudio=" + mCurrentStereoAudio);
        }
        mSystemStereoAudio = getStereoAudio();
        if (!mIsInitedStereoAudio) {
            mCurrentStereoAudio = mSystemStereoAudio;
            mIsInitedStereoAudio = true;
        } else {
            //if activity is not from onCreate()
            //restore old stereo type
            setStereoAudio(mCurrentStereoAudio);
        }
        updateStereoAudioIcon();
    }

    private void restoreStereoAudio() {
        setStereoAudio(mSystemStereoAudio);
    }
}
