/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.instoredemoapp;

import java.io.IOException;
import java.io.InputStream;

import com.dolby.ds1appUI.R;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.ImageButton;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnErrorListener;

public class DlbInStoreDemoPlayer extends Activity implements
		OnCompletionListener, OnPreparedListener, OnErrorListener {
	private static final String TAG = "DlbInStoreDemoPlayer";
	
	private VideoView mVideoView;
	private ImageButton mReplayBtn;
	private ImageButton mStopBtn;
	private ImageButton mExitBtn;
	private MediaPlayer mMediaPlayer;
	private DlbApController mApController = null;
	private Handler mHandler = null;
	private boolean mIsPrepared = false;
	private boolean mReplayEnabled = false;
	private boolean mIsPlayingLoopMedia = false;
	private boolean mIsManualStop = false; 
    private boolean mIsResumed = false;
    private InputStream mAutoPilotDataStream = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.videoplayer);

		mVideoView = (VideoView)findViewById(R.id.movie_view);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnPreparedListener(this);
				
		mHandler = new Handler(){
			public void handleMessage(Message msg){
			    Log.d(TAG, "handle message in handler, msg.what = " + msg.what);
			    if (msg.what == ConstValue.AP_MSG_ID){
			    	boolean success = mApController.processApMessage(msg);
			    	
			    	if (!success && mStopBtn != null)
			    	{
			    		// If autopilot failed, then stop the demo
			    		mStopBtn.callOnClick();
			    		
			    		// Display a message about demo failed to continue.
			    	}
			    } else if (msg.what == ConstValue.UPDATE_TXT_MSG_ID){
			    	if (ConstValue.UPDATE_TEXT){
			    	    updateDisplayText(msg);
			    	}
			    } else if (msg.what == ConstValue.START_LOOP_MEDIA_PLAYBACK){
			    	Log.d(TAG, "handle START_LOOP_MEDIA_PLAYBACK event");
			    	mVideoView.setVideoURI(getLoopUri());
			    	mIsPlayingLoopMedia = true;
                    if (mIsResumed == true) {
                        mVideoView.start();
                    }
			    } else if (msg.what == ConstValue.START_DEMO_MEDIA_PLAYBACK){
			    	Log.d(TAG, "handle START_DEMO_MEDIA_PLAYBACK");
			    	mVideoView.setVideoURI(getDemoUri());
			    	//need to re-init the ap messages, so call this method.
			    	mApController.setApInfoFile(getAutoPilotXmlFile());
			    	mIsManualStop = false;
			    	mIsPlayingLoopMedia = false;
			    } else if (msg.what == ConstValue.DS1_SERVICE_CONNECTED){
			    	//use the message to enusre that the service is connected successfully
			    	if (mApController.saveCurrentDs1Data()){
			    		mVideoView.setVideoURI(getDemoUri());
			    	} else {
			    		mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
			    	}
			    } else if (msg.what == ConstValue.DS1_INSTOREDEMO_QUIT){
			    	finish();
			    }else {
			    	Log.e(TAG, "DlbInstoreDemoPlayer.mHandler.handleMessage(), unknown message id = " + msg.what);
			    }
			}
		};

		
		mStopBtn = (ImageButton)findViewById(R.id.stop_button);
		mStopBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mIsPlayingLoopMedia){
					mHandler.removeCallbacksAndMessages(null);
					mIsManualStop = true;
					mVideoView.stopPlayback();
					mHandler.sendEmptyMessage(ConstValue.START_LOOP_MEDIA_PLAYBACK);
				}
			}
		});
		
		mReplayBtn = (ImageButton)findViewById(R.id.replay_toggle_button);
		mReplayBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mReplayEnabled = !mReplayEnabled;
				Log.d(TAG, "mReplayEnabled = " + mReplayEnabled);
				if (mReplayEnabled){
					mReplayBtn.setBackgroundResource(R.drawable.replay_on);
				} else {
					mReplayBtn.setBackgroundResource(R.drawable.replay_off);
				}
			}
		});
		
		mExitBtn = (ImageButton)findViewById(R.id.exit_button);
		mExitBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		if (mApController == null) {
			mApController = new DlbApController(this);
			mApController.setHandler(mHandler);
			mApController.setApInfoFile(getAutoPilotXmlFile());
		}
	}
	
	@Override
	protected void onResume(){
	    Log.d(TAG, "onResume");
        mIsResumed = true;
	    if (mIsPlayingLoopMedia){
	    	mVideoView.start();
	    }
	    boolean ret = mApController.saveCurrentDs1Data();
	    if (!ret){
	    	Log.e(TAG, "DlbInstoreDemoPlayer.onResume(), failed to saveCurrentDs1Data");
	    }
	    super.onResume();
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
        mIsResumed = false;
		if (!mIsPlayingLoopMedia) {
			mIsManualStop = true;
			mVideoView.stopPlayback();
			mHandler.removeCallbacksAndMessages(null);
			mHandler.sendEmptyMessage(ConstValue.START_LOOP_MEDIA_PLAYBACK);
		} else {
			mVideoView.pause();
		}
		mApController.restoreAllDs1Data();
		super.onPause();
	}
	
	@Override
    protected void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	mHandler.removeCallbacksAndMessages(null);
    	mApController.onExit();
    	
    	AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    	am.abandonAudioFocus(mAFChangeListener);
    	
    	super.onDestroy();
    }

	public void onCompletion(MediaPlayer arg0) {
		Log.d(TAG, "onCompletion called");
		mHandler.removeCallbacksAndMessages(null);
		mIsPrepared = false;
        if (mReplayEnabled && !mIsManualStop){
        	mHandler.sendEmptyMessage(ConstValue.START_DEMO_MEDIA_PLAYBACK);
        } else {
        	if (!mIsPlayingLoopMedia){ //This is the first time to play loop-media
        	    mHandler.sendEmptyMessage(ConstValue.START_LOOP_MEDIA_PLAYBACK);
        	} else { //The loop-media is already loaded and played at least once
        		mVideoView.start(); 
        	}
        }
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		Log.d(TAG, "onPrepared called");
		mMediaPlayer = mediaplayer;
		mIsPrepared = true;
		if (mIsPrepared && !mIsPlayingLoopMedia) {
			getAudioFocus();
			mApController.setMediaPlayer(mMediaPlayer);
			mVideoView.requestFocus();
			mVideoView.start();
			mApController.sendApMessages();
		}
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d(TAG, "onError called, what = " + what + " extra = " + extra);
		return true;
	}
	
    @Override
	public boolean onTouchEvent (MotionEvent event){
		Log.d(TAG, "onTouchEvent called action = " + event.getAction());
		if (event.getAction() == MotionEvent.ACTION_DOWN){
			if (mIsPlayingLoopMedia){
				mVideoView.stopPlayback();
				mHandler.sendEmptyMessage(ConstValue.START_DEMO_MEDIA_PLAYBACK);
			} else {
				if (mVideoView.isPlaying()){
					return false;
				} else {
					mHandler.sendEmptyMessage(ConstValue.START_DEMO_MEDIA_PLAYBACK);
				}
			}
			
		}
		return false;
	}

	private void updateDisplayText(Message msg) {
		if (ConstValue.UPDATE_TEXT) {
			TextInfo ti = (TextInfo) msg.obj;
			TextView tv = (TextView) this.findViewById(R.id.caption_text);
			tv.setTextColor(getColorByName(ti.textColor));
			tv.setText(ti.text);
		}
    }
    
    private int getColorByName(String color){
    	int icolor = ConstValue.TEXT_COLOR_WHITE;
    	if (color.equalsIgnoreCase("White")){
    		icolor = ConstValue.TEXT_COLOR_WHITE;
    	} else if (color.equalsIgnoreCase("Black")){
    		icolor = ConstValue.TEXT_COLOR_BLACK;
    	} else if (color.equalsIgnoreCase("Red")){
    		icolor = ConstValue.TEXT_COLOR_RED;
    	} else if (color.equalsIgnoreCase("Yellow")){
    		icolor = ConstValue.TEXT_COLOR_YELLOW;
    	} else if (color.equalsIgnoreCase("Blue")) {
    		icolor = ConstValue.TEXT_COLOR_BLUE;
    	} else {
    		icolor = ConstValue.TEXT_COLOR_WHITE;
    	}
    	return icolor;
    }	
    
    private OnAudioFocusChangeListener mAFChangeListener = new OnAudioFocusChangeListener(){

		@Override
		public void onAudioFocusChange(int focusChange) {
			Log.d(TAG, "onAudioFocusChange, focusChange = " + focusChange);
			
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if(am.isAppInFocus("com.dolby.ds1appUI"))
			{
				return;
			}

			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
				mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS){
	            mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
			} else { // (focusChange == AudioManager.AUDIOFOCUS_GAIN)
				Log.d(TAG, "onAudioFocusChange, do nothing for value = " + focusChange);
			}
		}
	};
	
	private void getAudioFocus() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(mAFChangeListener, 
				                          AudioManager.STREAM_MUSIC,
				                          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
			Log.d(TAG, "DlbInstoreDemoPlayer.getAudioFocus, succeeded");
		} else {
		    Log.e(TAG, "DlbInstoreDemoPlayer.getAudioFocus failed, result = " + result);
		}
	}
    private Uri getDemoUri(){
    	Uri demoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.instore_demo_media);
    	Log.d(TAG, "demoUri = " + demoUri.toString()); 
    	return demoUri;
    }
    
    private Uri getLoopUri(){
    	Uri loopUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.instore_demo_loop);
    	Log.d(TAG, "loopUri = " + loopUri.toString());
    	return loopUri;
    }
    
	private InputStream getAutoPilotXmlFile() {
		if (mAutoPilotDataStream == null) {
			try {
				mAutoPilotDataStream = this.getResources().getAssets().open(ConstValue.APINFO_FILE_NAME);
			} catch (IOException ioe) {
				Log.e(TAG, "DlbInstoreDemoPlayer.getAutoPilotXmlFile, the file does not exist");
				ioe.printStackTrace();
				mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
			}
		}
    	return mAutoPilotDataStream;
    }
}
