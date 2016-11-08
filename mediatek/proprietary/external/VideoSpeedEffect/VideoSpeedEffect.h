/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
 /*******************************************************************************
 *
 * Filename:
 * ---------
 * VideoSpeedEffect.h
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   This file provide the slow motion speed effect interface.
 *
 * Author:
 * -------
 *   Haizhen Wang(mtk80691)
 *
 *------------------------------------------------------------------------------
 *******************************************************************************/
  
#ifndef _VIDEO_SPEEDEFFECT_UTILS_H_
#define _VIDEO_SPEEDEFFECT_UTILS_H_

#include <sys/types.h>
#include <media/stagefright/foundation/AHandler.h>
#include <media/stagefright/MetaData.h>

#include <media/mediarecorder.h>
#include <media/stagefright/MediaDefs.h>
//#include <media/mediaplayer.h>

#include <media/stagefright/foundation/ALooper.h>

#include <media/stagefright/MediaExtractor.h>
//#include <media/stagefright/MediaBuffer.h>

//#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaWriter.h>



//namespace mtkspeedeffect {
namespace android {

using namespace android;
using android::status_t;

struct ABuffer;	

/**
  *@ event notifies to caller
  *@ SPEEDEFFECT_RECORD_EVENT_BEGIN : notify during recording, 
  *@ define a offset to avoid conflicting with the event defined in mediarecorder.h
  */
enum speedeffect_event_type {
   	SPEEDEFFECT_NOP             = 0, // interface test message
    SPEEDEFFECT_STARTED         = 1,
    SPEEDEFFECT_STOPPED			= 2,
    SPEEDEFFECT_COMPLETE		= 3,
    SPEEDEFFECT_PROGRESS_UPDATE = 4,
	SPEEDEFFECT_ERROR 			= 5,

	SPEEDEFFECT_UNSUPPORTED_VIDEO = 10,
	SPEEDEFFECT_UNSUPPORTED_AUDIO = 11,
	
		
	SPEEDEFFECT_RECORD_EVENT_BEGIN = 100,
    SPEEDEFFECT_RECORD_EVENT_ERROR = SPEEDEFFECT_RECORD_EVENT_BEGIN + MEDIA_RECORDER_EVENT_ERROR,
    SPEEDEFFECT_RECORD_EVENT_INFO = SPEEDEFFECT_RECORD_EVENT_BEGIN + MEDIA_RECORDER_EVENT_INFO,
	
};

/**
  *@ Description: Listener of VideoSpeedEffectUtils,
  *@			     It's a virtual class, caller can implement it in a inherited class  
  *@ Parameters:
  *@		msg: message type, such as events defined by speedeffect_event_type
  *@		ext1: message parameter 1
  *@ Return:
  *@ 	none
  */
	class VideoSpeedEffectListener: virtual public RefBase
	{
	public:
		VideoSpeedEffectListener(){};
		virtual ~VideoSpeedEffectListener(){};
		virtual void notify(int msg, int ext1, int ext2) = 0;
		
	};

/**
  *@ An object of this class facilitates handling video speed effect, such as slow motion, fast motion.
  *@ User can make slow motion or fast motion effect on the video
  *@ A new video with 3gp will be created containing the required codec parmeters
  *@ 
  */
class VideoSpeedEffect : public AHandler {
public:
	
	/**
	  *@ Description: VideoSpeedEffectUtils constructor
	  *@			
	  *@ Parameters:
	  *@		none
	  *@ Return:
	  *@		no return
	  */
	VideoSpeedEffect();
	
	/**
	  *@ Description: VideoSpeedEffectUtils destructor
	  *@			
	  *@ Parameters:
	  *@		none
	  *@ Return:
	  *@		no return
	  */
	 ~VideoSpeedEffect();

	/**
	  *@ Description: add speed effect parameters, such as slow motion interval, slow motion speed
	  *@			     caller need call this interface before start speed effect handling
	  *@ Parameters:
	  *@		startPos: start position of speed effect (such as slow motion) interval
	  *@		endPos:  end position of speed effect (such as slow motion) interval
	  *@		speedEffectParams: the speed effect parameters,such as "slow-motion-speed = 4;video-framerate = 30;mute-autio = 0"
	  *@ Return:
	  *@		status_t type, OK indicate successful, otherwise error type will return
	  */
	status_t addSpeedEffectParams(int64_t startPosMs,int64_t endPosMs,const String8 &speedEffectParams);

	/**
	  *@ Description: start save the speed effect		
	  *@ Parameters:
  	  *@ 	srcFd: File Description of the src file
  	  *@ 	dstFd: File Description of the dst file	  		 
	  *@ Return:
	  *@		status_t type, OK indicate successful, otherwise error type will return
	  */
	status_t startSaveSpeedEffect(int srcFd,int64_t srcOffset, int64_t srcLength, int dstFd);
	status_t startSaveSpeedEffect(int srcFd, int dstFd);
	/**
	  *@ Description: stop the speed effect opertion
	  *@			     Caller can call this interface if user has cancelled the speed effect. 
	  *@			     Part of the video will be transfered, caller can delete the output video if user cancel the operation
	  *@ Parameters:
  	  *@ 	none		 
	  *@ Return:
	  *@		status_t type, OK indicate successful, otherwise error type will return
	  */
	status_t stopSaveSpeedEffect();
	
	/**
	  *@ Description: set listener to VideoSpeedEffectUtils
	  *@			     VideoSpeedEffectUtils will call the notify function of the listener
	  *@ Parameters:
	  *@		listener: pointer to VideoSpeedEffectListener
	  *@ Return:
	  *@		none
	  */
	void setListener(sp<VideoSpeedEffectListener> listener) {
		mListener = listener;

	}
	
	/*----------------------------------------------------------------------------------------
	------------------------------------------------------------------------------------------*/
	//if caller care about the target video parameters,such as video and audio codec type, target video frame rate ,
	//caller can use the following interface to set these parameters before call startSaveSpeedEffect
	//otherwise startSaveSpeedEffect will use the default parameters:
	//target video and audio codec, audio channel count,video width and height -> same as the src file
	//target video frame rate -> 30fps
	//
	status_t setOutputFormat(output_format of);
	status_t setTargetVideoEncoder(video_encoder ve);
	status_t setTargetAudioEncoder(audio_encoder ae);

	status_t setParamAudioSamplingRate(int32_t sampleRate);
	status_t setParamAudioNumberOfChannels(int32_t channels);
	status_t setParamAudioEncodingBitRate(int32_t bitRate);
	status_t setParamVideoEncodingBitRate(int32_t bitRate);
	status_t setParamVideoFrameRate(int32_t frameRate);

	//Besides above set target video and audio params interfaces
	//Caller also can set all params throught setParameters
	//such as "video-target-framerate = 30;video-target-bitrate = 17000*1000,video-target-width=1280,vieo-target-height=720 "
	status_t setParameters(const String8 &Params);
	status_t setParameter(const String8 &key, const String8 &value);

	
	status_t notify(int msg, int ext1, int ext2);
	
protected:
    virtual void onMessageReceived(const sp<AMessage> &msg);
	status_t onNotifyProgress(const sp<AMessage> &msg);
private:
	class SpeedEffectSource;
	
	struct SpeedEffectParam {
		int64_t mStartPos; //Us
		int64_t mEndPos; //Us
		sp<MetaData> mMeta;
	};
	
	status_t transParamsToMeta(const String8 &Params,sp<MetaData>& meta);
	status_t setParamToMeta(const String8 &key, const String8 &value,sp<MetaData>& meta);
	status_t checkSpeedEffectList(List<SpeedEffectParam>& speedEffect,sp<MediaExtractor> extractor);
	bool needTranscode(sp<MetaData> srcMeta,sp<MetaData> dstMeta);
	void updateMetaSrc2Dst(sp<MetaData> srcMeta,sp<MetaData> dstMeta);
	List<SpeedEffectParam>	mSpeedEffectParamList;

	struct TrackInfo {
		int32_t mTrackId;
		//char * mime;
		sp<SpeedEffectSource> mSpeedEffectSource;
		
	};
	List<TrackInfo> mTrackInfoList;
		
	//
	int mOutputFormat;
	sp<MetaData> mTargetMeta;
	sp<MetaData> mTargetVideoMeta;
	sp<MetaData> mTargetAudioMeta;
	video_encoder mTargetVideoCodec;
	int32_t	mTargetVideoFrameRate;	
	int32_t mTargetVideoBitRate;
	int32_t mTargetVideoWidth;
	int32_t mTargetVideoHeight;
	
	audio_encoder mTargetAudioCodec;
	int32_t mTargetAudioSampleRate;
	int32_t mTargetAudioChannelCount;
	int32_t mTargetAudioBitRate;

	sp<DataSource> mFileSource;
	int64_t mSrcBitrate;

	sp<MetaData> mMetaData;
	sp<MediaExtractor> mExtractor;
	int64_t mFileDurationUs;
	
	video_encoder mSrcVideoCodec;
	int32_t	mSrcVideoFrameRate;	
	int32_t mSrcVideoBitRate;
	int32_t mSrcVideoWidth;
	int32_t mSrcVideoHeight;
	
	audio_encoder mSrcAudioCodec;
	int32_t mSrcAudioSampleRate;
	int32_t mSrcAudioChannelCount;
	int32_t mSrcAudioBitRate;

	//
	sp<ALooper> mLooper;
	

	sp<IMediaRecorderClient> mRecordListener;
	sp<MediaWriter> mWriter;

	Mutex mLock;
	bool mStarted;

	bool mNeedTransHEVC2H264;
	
public:
	sp<VideoSpeedEffectListener> mListener;
	Mutex mEffectListLock;
};

}
	
#endif// _VIDEO_SPEEDEFFECT_UTILS_H_	
