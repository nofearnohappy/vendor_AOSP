package com.mediatek.gallery3d.plugin;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.gallery3d.ext.DefaultServerTimeoutExtension;
import com.mediatek.gallery3d.video.IMoviePlayer;
import com.mediatek.gallery3d.video.MovieUtils;
import com.mediatek.op01.plugin.R;

/**
 * OP01 plugin implementation of Streaming Server Timeout.
 */
public class ServerTimeout extends DefaultServerTimeoutExtension {
    private static final String TAG = "Gallery2/VideoPlayer/ServerTimeout";
    private static final boolean LOG = true;

    private int mServerTimeout = -1;
    private long mLastDisconnectTime;
    private boolean mIsShowDialog = false;
    private AlertDialog mServerTimeoutDialog;
    private Context mPluginContext;
    private IMoviePlayer mPlayer;

    private static final String KEY_VIDEO_LAST_DISCONNECT_TIME = "last_disconnect_time";

    /**
     * @hide
     *
     */
    public ServerTimeout() {
        super();
    }

    /**
     * @hide
     *
     * @param context context instance
     */
    public ServerTimeout(Context context) {
        super();
        mPluginContext = context;
    }

    @Override
    public void recordDisconnectTime() {
        if (MovieUtils.isRtspOrSdp(getPlayer().getVideoType())) {
            //record the time disconnect from server
            mLastDisconnectTime = System.currentTimeMillis();
        }
        if (LOG) {
            Log.v(TAG, "recordDisconnectTime() mLastDisconnectTime=" + mLastDisconnectTime);
        }
    }

    @Override
    public void clearServerInfo() {
        mServerTimeout = -1;
    }

    @Override
    public void clearTimeoutDialog() {
        if (mServerTimeoutDialog != null && mServerTimeoutDialog.isShowing()) {
            mServerTimeoutDialog.dismiss();
        }
        mServerTimeoutDialog = null;
    }

    @Override
    public void onRestoreInstanceState(Bundle icicle) {
        mLastDisconnectTime = icicle.getLong(KEY_VIDEO_LAST_DISCONNECT_TIME);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(KEY_VIDEO_LAST_DISCONNECT_TIME, mLastDisconnectTime);
    }

    @Override
    public boolean handleOnResume() {
        if (mIsShowDialog && !MovieUtils.isLiveStreaming(getPlayer().getVideoType())) {
            //wait for user's operation
            return true;
        }
        if (!passDisconnectCheck()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //if we are showing a dialog, cancel the error dialog
        if (mIsShowDialog) {
            return true;
        }
        return false;
    }

    @Override
    public void setVideoInfo(Metadata data) {
        if (data.has(Metadata.SERVER_TIMEOUT)) {
            mServerTimeout = data.getInt(Metadata.SERVER_TIMEOUT);
            if (LOG) {
                Log.v(TAG, "get server timeout from metadata. mServerTimeout=" + mServerTimeout);
            }
        }
    }

    private boolean passDisconnectCheck() {

        if (MovieUtils.isRtspOrSdp(getPlayer().getVideoType())) {
            //record the time disconnect from server
            long now = System.currentTimeMillis();
            if (LOG) {
                Log.v(TAG, "passDisconnectCheck() now=" + now + ", mLastDisconnectTime=" +
                      mLastDisconnectTime + ", mServerTimeout=" + mServerTimeout);
            }
            if (mServerTimeout > 0 && (now - mLastDisconnectTime) > mServerTimeout) {
                //disconnect time more than server timeout, notify user
                notifyServerTimeout();
                return false;
            }
        }
        return true;
    }

    private void notifyServerTimeout() {
        if (mServerTimeoutDialog == null) {
            //for updating last position and duration.
            if (getPlayer().isVideoCanSeek() || getPlayer().canSeekForward()) {
                getPlayer().seekTo(getPlayer().getVideoPosition());
            }
            getPlayer().setDuration(getPlayer().getVideoLastDuration());
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            mServerTimeoutDialog = builder.setTitle(
                                   mPluginContext.getString(R.string.server_timeout_title))
                .setMessage(mPluginContext.getString(R.string.server_timeout_message))
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (LOG) {
                            Log.v(TAG, "NegativeButton.onClick() mIsShowDialog=" + mIsShowDialog);
                        }
                        getPlayer().showEnded();
                        getPlayer().notifyCompletion();
                    }

                })
                .setPositiveButton(mPluginContext.getString(R.string.resume_playing_resume),
                                   new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (LOG) {
                            Log.v(TAG, "PositiveButton.onClick() mIsShowDialog=" + mIsShowDialog);
                        }
                        getPlayer().startVideo(true, getPlayer().getVideoPosition(),
                                               getPlayer().getVideoLastDuration());

                        getPlayer().updateProgressBar();
                    }

                })
                .create();
            mServerTimeoutDialog.setOnDismissListener(new OnDismissListener() {

                    public void onDismiss(DialogInterface dialog) {
                        if (LOG) {
                            Log.v(TAG, "mServerTimeoutDialog.onDismiss()");
                        }
                        mIsShowDialog = false;
                    }

                });
            mServerTimeoutDialog.setOnShowListener(new OnShowListener() {

                    public void onShow(DialogInterface dialog) {
                        if (LOG) {
                            Log.v(TAG, "mServerTimeoutDialog.onShow()");
                        }
                        mIsShowDialog = true;
                    }

                });
        }
        mServerTimeoutDialog.show();
    }

    @Override
    public void setParameter(String key, Object value) {
        super.setParameter(key, value);
        if (value instanceof IMoviePlayer) {
            mPlayer = (IMoviePlayer) value;
        }
    }

    private IMoviePlayer getPlayer() {
        return mPlayer;
    }
}
