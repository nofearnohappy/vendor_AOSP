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
 * VideoSpeedEffect.cpp
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   This file implements saving the slow motion speed effect.
 *
 * Author:
 * -------
 *   Haizhen Wang(mtk80691)
 *
 *------------------------------------------------------------------------------
 *******************************************************************************/

#define LOG_TAG "VideoSpeedEffect"
#include <utils/Log.h>

#include "VideoSpeedEffect.h"

#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MetaData.h>

#include <media/stagefright/MediaDefs.h>

#include <media/mediarecorder.h>

#include <media/openmax/OMX_Audio.h>
#include <media/stagefright/MPEG4Writer.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/OMXCodec.h>
#include <media/stagefright/MediaBufferGroup.h>
#include "DpBlitStream.h"
#include <OMX_IVCommon.h>
#include <binder/ProcessState.h>
#include <cutils/properties.h>

#include <utils/Errors.h>
#include <sys/types.h>
#include <ctype.h>
#include <unistd.h>

#include "AudioMTKTimeStretch.h"
//#include <AudioMTKTimeStretch.h>
//for query encoder bitrate setting
#include "venc_drv_if_public.h" 

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>

//namespace mtkspeedeffect {

namespace android {


using namespace android;
using android::status_t;

#define MEM_ALIGN_32 32
#define ROUND_16(X)     ((X + 0xF) & (~0xF))
#define ROUND_32(X)     ((X + 0x1F) & (~0x1F))

#define YUV_SIZE(W,H)   (W * H * 3 >> 1)
enum {
    UV_STRIDE_16_8_8,
    UV_STRIDE_16_16_16
};

enum {
	 kKeySlowMotionSpeed	= 'slms', //int32_t, slow motion speed
	 kKeyFastMotionSpeed 	= 'ftms', //int32_t, fast motion speed
	 kKeyVideoDropRate		= 'dprt', //float, video frame drop rate
	 //kKeyTargetFrameRate	= 'tgfr', //int32_t,target video frame rate
	 kKeyFirstVideoSectBuf = 'firV', //
	 kKeyFirstAudioSectBuf = 'firA', //
	 kKeyTargetAudioSampleRate = 'aspr', //
	 kKeyNonRefPInfo = 'npin',//int32_t non-reference p info
	 kKeyFirstAudiotimeUs = 'fatm', // 
	 kKeyFirstVideotimeUs = 'fvtm', // 
	 kKeySectionFrameNumb = 'sefn', //section frame number
};

#define TARGET_FRAMERATE 30
#define TIMESTRETCH_INTERNAL_BUFFER_SAMPLE_COUNT 512
static int64_t kProgressDelayUs = 200000ll;
static int32_t kTargetVideoColorFormat = OMX_COLOR_FormatYUV420Planar;

class CSpeedEffectRecordListner : public BnMediaRecorderClient{
	
public:
	CSpeedEffectRecordListner(VideoSpeedEffect* pdelegation)
	{
		m_pdelegation = pdelegation;
	}
	~CSpeedEffectRecordListner(){}
	
	virtual void  notify(int msg, int ext1, int ext2)
	{
		ALOGD("receive MPEG4Writer notify:msg(%d),ext1(%d),ext2(%d)",msg,ext1,ext2);
		
		m_pdelegation->notify(SPEEDEFFECT_RECORD_EVENT_BEGIN + msg,ext1,ext2);
	}
	
private:	
	VideoSpeedEffect* m_pdelegation;
		
};
class VideoSpeedEffect::SpeedEffectSource:public MediaSource {

public:
	/*
	SpeedEffectSource(sp<MediaSource> source,int32_t queueBufferNum,
							sp<VideoSpeedEffect> owner,int32_t trackID);
							*/
	SpeedEffectSource(sp<MediaSource> source,bool isBitstream,bool isAudio,sp<MetaData> targetMeta,/*int32_t queueBufferNum,*/
					sp<VideoSpeedEffect> owner,int32_t trackID,List<SpeedEffectParam> *pSpeedEffectParamList);

	~SpeedEffectSource();

	virtual status_t start(MetaData *params = NULL);
	virtual status_t stop();
	virtual sp<MetaData> getFormat();

	virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options = NULL);

	
	//status_t push(sp<MediaBuffer> buffer);
	status_t getPosition(int64_t *positionUs);
	
	void setFirstBuffer(MediaBuffer* firstBuffer) {mSourceBuffer = firstBuffer;}
	
private:
	bool SpeedEffectVideoIfNeed(MediaBuffer*& buffer);
	bool SpeedEffectAudioIfNeed(MediaBuffer*& buffer,MediaBuffer*& outbuffer);
	status_t videoFormatChange(MediaBuffer*& buffer,sp<MetaData> srcMeta,sp<MetaData> dstMeta);
	void convertVideoFrame(uint8_t* input, int32_t srcWidth, int32_t srcHeight, int32_t srcStride, int32_t srcSliceHeight,
                                     uint8_t* output, int32_t dstWidth, int32_t dstHeight) ;

	bool needFormatChange(sp<MetaData> srcMeta,sp<MetaData> dstMeta);
	bool isNonReferenceFrame(MediaBuffer* buffer,sp<MetaData> bufferMeta); 
	sp<MetaData> mMeta;
	int32_t mSrcColorFormat;
    Mutex mLock;
	sp<MediaSource> msource;
	//int32_t mQueueBufferNum;
	sp<VideoSpeedEffect> mOwner;
	int32_t mTrackId;
	
	bool mStarted;
	bool mIsBitstream;
	bool mIsAudio;
	bool mIsAvc;
	// for video track parameter
	sp<MetaData> mTargetMeta;
	int32_t mTargetWidth;
	int32_t mTargetHeight;
	int32_t mDstColorFormat;
	MediaBufferGroup* mPtrBufferGroup;
	bool mNeedFormatChange;
	
	bool mPrintDebugInfo;
	int64_t mFrameCount; //for debug
	int64_t mLastNewTimeUs; // for debug
	int64_t mLastKeepFrameNewTimeUs; // for debug
	int64_t mKeepFrameNum; // for debug
	int64_t mExpectedTargetFrameDurUs; // for debug
	
	
	//for timetamp handle for target speed
	bool mIsFirstFrame;
	int64_t mOrigLastTimestamp;
	int64_t mLastTimestamp;

	int64_t mNextAudioTimestamp;
	List<SpeedEffectParam>	*mpSpeedEffectParamList;

	int64_t mSectionBufferDurUs;
	int64_t mSectionFirstOrigTimestamp;
	int64_t mSectinoFirstNewTimestamp;
	int64_t mLastMaintainTimestamp;
	int64_t mLastOrigTimestamp;
	int64_t mLastDelta;
	bool mGetNewSpeed;
	int32_t mLastSLMSpeed;
	int32_t mNowSLMSpeed;
	bool mDequedLastBuffer; //for internal buffer in audio time stretch

	
    //For audio track parameters
	MediaBuffer * mSourceBuffer;
	int32_t mSampleRate;;
	int32_t mChannelCount;
	audio_format_t mAudioFormat;
	int32_t mBytesPerSample;
	
	AudioMTKTimeStretch* mAudioTimeStrecth;
	int32_t mTStrIntBufSampeCount;

	template <class Type>
	class VideoFpsControlTable : public RefBase{
		//table by time --class members
		Type mTableStartPoint;
		Type mTableMaxPoint;
		Type mStepInterval;
		int32_t mTableNumEntry;
		
	public:	
		struct FpsControlEntry{
			Type mEntryStartPoint;
			Type mEntryStopPoint;
			bool mHasFrame;
		};
		Vector<FpsControlEntry> mTable;
	
		static VideoFpsControlTable<Type>* create(Type tableStartPoint,int32_t entryNum, Type stepInterval){
			VideoFpsControlTable<Type>* controlTable = new VideoFpsControlTable(tableStartPoint,entryNum,stepInterval);
			return controlTable;
		}
		
		VideoFpsControlTable(Type tableStartPoint,int32_t entryNum,Type stepInterval){
		
			mTableStartPoint = tableStartPoint;
			mTableMaxPoint = tableStartPoint;
			mTableNumEntry = entryNum;
			mStepInterval = stepInterval;
			
			Type lastPoint = tableStartPoint;
			
			for( int i = 0 ; i< mTableNumEntry; i++){
								
				FpsControlEntry item;
				item.mEntryStartPoint = lastPoint;
				item.mEntryStopPoint = lastPoint + mStepInterval;
				item.mHasFrame= false;
				lastPoint = item.mEntryStopPoint;
				mTable.push_back(item);
				
			}
			//update max timeUs
			mTableMaxPoint = lastPoint;  
			ALOGI("createFpsControlTable,table [%lld-%lld],entry num [%d],",(long long)mTableStartPoint,(long long)mTableMaxPoint,mTableNumEntry);
			
		}
		
		FpsControlEntry* findEntry(Type accessPoint){
			
			FpsControlEntry* entry = NULL;
			
			if(accessPoint > mTableMaxPoint){
				//need update table
				update(mTableMaxPoint,mTableNumEntry,mStepInterval);

			}
			int i = 0;
			for(i = 0; i< mTableNumEntry; i++){
				FpsControlEntry& item = mTable.editItemAt(i);
				
				if(accessPoint > item.mEntryStartPoint && accessPoint <= item.mEntryStopPoint){
					ALOGD("video,getEntryValue entry[%lld-%lld],hasFrame=%d",\
					(long long)item.mEntryStartPoint,(long long)item.mEntryStopPoint,item.mHasFrame);
					
					entry = &item;
					break;
				}
			}
			if(i == mTableNumEntry){
				ALOGW("can not find entry contain [%lld]",(long long)accessPoint);
			}
			return entry;

		}
		
		status_t update(Type tableStartPoint,int32_t entryNum,Type stepInterval){

			mTableStartPoint = tableStartPoint;
			mStepInterval = stepInterval;
			mTableMaxPoint = tableStartPoint;
			
			
			if(mTableNumEntry > entryNum){
				size_t removeNum = mTableNumEntry - entryNum;
				mTable.removeItemsAt(entryNum,removeNum);
				mTableNumEntry = entryNum;
			}
			
			Type lastPoint = tableStartPoint;
			for(int i = 0; i < entryNum; i++){
				
				if(i < mTableNumEntry){
					FpsControlEntry& item = mTable.editItemAt(i);
					item.mEntryStartPoint = lastPoint;
					item.mEntryStopPoint = lastPoint + mStepInterval;
					item.mHasFrame = false;
					lastPoint = item.mEntryStopPoint;
				} else{
				
					FpsControlEntry item;
					item.mEntryStartPoint = lastPoint;
					item.mEntryStopPoint = lastPoint + mStepInterval;
					item.mHasFrame= false;
					lastPoint = item.mEntryStopPoint;
					mTable.push_back(item);
				}
									
			}
			mTableMaxPoint = lastPoint;
			mTableNumEntry = entryNum;
			ALOGI("update control table,table [%lld-%lld],entry num [%d],",(long long)mTableStartPoint,(long long)mTableMaxPoint,mTableNumEntry);
			return OK;
		}
			
		
		~VideoFpsControlTable(){}
	};
	
	//sp<VideoFpsControlTable<Type>> mVideoFpsControlTable; 
	void* mVideoFpsControlTable;
	
};

VideoSpeedEffect::VideoSpeedEffect() {
	ALOGI("VideoSpeedEffect constructor");
	mTargetVideoCodec = VIDEO_ENCODER_DEFAULT;
	mTargetVideoFrameRate = TARGET_FRAMERATE;	
	mTargetVideoBitRate = -1;
	mTargetVideoWidth = -1;
	mTargetVideoHeight = -1;
	
	mTargetAudioCodec = AUDIO_ENCODER_DEFAULT;
	mTargetAudioSampleRate = -1;
	mTargetAudioChannelCount = -1;
	mTargetAudioBitRate = -1;
	mFileDurationUs = -1;
	
	mOutputFormat = OUTPUT_FORMAT_THREE_GPP;	
	//mTargetMeta = new MetaData;
	mTargetVideoMeta = new MetaData;
	mTargetAudioMeta = new MetaData;

	mSrcVideoFrameRate = -1;
	mStarted = false;
	DataSource::RegisterDefaultSniffers();
	mTargetMeta = new MetaData();
	mTargetVideoMeta = new MetaData();
	mTargetAudioMeta = new MetaData();

	mNeedTransHEVC2H264 = true;
    mSrcBitrate = -1;
    mSrcVideoCodec = VIDEO_ENCODER_DEFAULT;
    mSrcVideoBitRate = -1;
    mSrcVideoHeight = -1;
    mSrcVideoWidth = -1;
    mSrcAudioBitRate = -1;
    mSrcAudioChannelCount = -1;
    mSrcAudioCodec = AUDIO_ENCODER_DEFAULT;
    mSrcAudioSampleRate = -1;
}

VideoSpeedEffect::~VideoSpeedEffect() {
        ALOGI("VideoSpeedEffect destructor ++");
	stopSaveSpeedEffect();
	if(mLooper != NULL){
	mLooper->unregisterHandler(id());
		mLooper->stop();
		mLooper = NULL;
	}
	if(mWriter != NULL){
		mWriter->stop();
		mWriter = NULL;
	}
	//Trackinfo delete
	mTrackInfoList.clear();
	ALOGI("VideoSpeedEffect destructor --");
}


status_t VideoSpeedEffect::addSpeedEffectParams(int64_t startPosMs,int64_t endPosMs,const String8 &speedEffectParams) {
	Mutex::Autolock autoLock(mLock);
	ALOGI("addSpeedEffectParams,%lld-%lld,effect:%s",startPosMs,endPosMs,speedEffectParams.string());
	//check whether there are speed effect on the start-end
	//if there one speed effect on the start-end, then replace it using the new one

	SpeedEffectParam speedEffect;
	//speedEffect.mStartPos = startPosUs;
	//speedEffect.mEndPos = endPosUs;
	//convert ms to us
	speedEffect.mStartPos = startPosMs * 1000;
	speedEffect.mEndPos = endPosMs * 1000;
	
	speedEffect.mMeta = new MetaData();
	transParamsToMeta(speedEffectParams,speedEffect.mMeta);

	mSpeedEffectParamList.push_back(speedEffect);
	return OK;
}

status_t VideoSpeedEffect::startSaveSpeedEffect(int srcFd, int dstFd){
	if ((srcFd >= 0) && (dstFd >= 0)) {
        int64_t srcLength = lseek64(srcFd, 0, SEEK_END);
		ALOGI("srcLength =%lld",srcLength);
	
		return startSaveSpeedEffect(srcFd,0,srcLength,dstFd);
		
	}
	else{
		ALOGE("startSaveSpeedEffect,fd err,srcFd =%d,dstFd=%d",srcFd,dstFd);
		return UNKNOWN_ERROR;
	}
		
}

status_t VideoSpeedEffect::startSaveSpeedEffect(int srcFd,int64_t srcOffset, int64_t srcLength, int dstFd) {
	 Mutex::Autolock autoLock(mLock);
	 ALOGI("startSaveSpeedEffect,srcFd=%d,srcLength=%lld,dstFd=%d,",srcFd,srcLength,dstFd);
	 
	 if(mStarted){
	 	ALOGW("VideoSpeedEffect already started !");
		return OK;
	 }
	 
	 android::ProcessState::self()->startThreadPool();
	//get src file format
    int Fd = -1;
    Fd = dup(srcFd);
    if(Fd == -1){
        ALOGE("dup src fd fail");
        return UNKNOWN_ERROR;
    }
	sp<DataSource> dataSource = new FileSource(Fd, srcOffset, srcLength);

	status_t err = dataSource->initCheck();

	if (err != OK) {
	   ALOGE("startSaveSpeedEffect,FileSource create fail !");
	   return err;
	}

//	mFileSource = dataSource;
	sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);

    if (extractor == NULL) {
		ALOGW("No extractor found!");
        return UNKNOWN_ERROR;
    }
	
	sp<MetaData> meta = extractor->getMetaData();

	checkSpeedEffectList(mSpeedEffectParamList,extractor);

	meta->findInt64(kKeyDuration,&mFileDurationUs);
	ALOGI("startSaveSpeedEffect, mFileDurationUs=%lld",mFileDurationUs);
	mExtractor = extractor;
	
	bool haveAudio = false;
    bool haveVideo = false;

	//Create Alooper for message notify
	mLooper = new ALooper;
	mLooper->registerHandler(this);
	mLooper->start();
	sp<AMessage> progressNotify = new AMessage('prog', this);
	progressNotify->post();
	
	// create MPEG4 writer
	ALOGI("startSaveSpeedEffect,new MPEG4Writer");
	mWriter = new MPEG4Writer(dstFd);
	sp<MediaSource> source;
	ALOGI("track number of source file =%d",extractor->countTracks());
	for (size_t i = 0; i < extractor->countTracks(); ++i) {
		sp<MetaData> trackMeta = extractor->getTrackMetaData(i);
		
		const char *_mime;
		CHECK(trackMeta->findCString(kKeyMIMEType, &_mime));
		String8 mime = String8(_mime);
		ALOGI("track[%d] mime =%s",i,mime.string());

	   if (!haveVideo && !strncasecmp(mime.string(), "video/", 6)) {
            haveVideo = true;
			ALOGI("startSaveSpeedEffect,find video track");
			TrackInfo videoTrackInfo;
			//videoTrackInfo.mTrack = extractor->getTrack(i);
			//videoTrackInfo.mime = mime;
			videoTrackInfo.mTrackId = i;
			int32_t videofps = 0;
			if(!mTargetVideoMeta->findInt32(kKeyFrameRate, &videofps) || videofps <= 0){
		
				mTargetVideoMeta->setInt32(kKeyFrameRate, mTargetVideoFrameRate);
			}
			 // keep rotation info to file container
			int32_t video_rotation = 0;
			if (trackMeta->findInt32(kKeyRotation, &video_rotation)) {
				ALOGI("set file meta: rotation = %d",video_rotation);
				mTargetMeta->setInt32(kKeyRotation, video_rotation);
			}
			
			bool needTrans = needTranscode(trackMeta,mTargetVideoMeta);
			ALOGI("startSaveSpeedEffect,video track need transcode = %d",needTrans);
			
			sp<MediaSource> videoSource = extractor->getTrack(i);
			//sp<MediaSource> source  = videoSource;
			if(needTrans){
				
				 // create video decoder
				 //we can modify the output number
				ALOGI("video deocoder create start");
				OMXClient mDecoderClient;
				CHECK_EQ(mDecoderClient.connect(), (status_t)OK);
				sp<MediaSource> mVideoDecoder = OMXCodec::Create(mDecoderClient.interface(), videoSource->getFormat(), false, // createdecoder
																				videoSource, NULL, 0 /*flags*/);
				if (mVideoDecoder == NULL) {
					ALOGE ("Oops, Video decoder creation failed. [Unsupported Video]");
					notify(SPEEDEFFECT_UNSUPPORTED_VIDEO,0,0);
					//return OK;
					continue;
				}
				ALOGI("video deocoder create end");
				//first start decoder, 
				//give a chance to check whether decoder support
				if(OK != mVideoDecoder->start()){
					ALOGE ("Oops, Video decoder start failed. [Unsupported Video]");
					notify(SPEEDEFFECT_UNSUPPORTED_VIDEO,0,0);
					//return OK;
					continue;
				}
				ALOGI("video deocoder started");

				//check whether need format change
				MediaBuffer* firstBuf = NULL;
				status_t firstBufferResult = OK;
				firstBufferResult = mVideoDecoder->read(&firstBuf);
				if (firstBufferResult == INFO_FORMAT_CHANGED) {
			        ALOGW("Video INFO_FORMAT_CHANGED!!!");
			        CHECK(firstBuf == NULL);  
					//update src meta and targe meta
					//sp<MetaData> format = mVideoDecoder->getFormat();
					//updateMetaSrc2Dst(format,mTargetVideoMeta);				
			    }else if(firstBufferResult != OK){
					ALOGE ("Oops, Video decoder read first buffer failed. [Unsupported Video]");
					mVideoDecoder->stop();
					notify(SPEEDEFFECT_UNSUPPORTED_VIDEO,0,0);
					continue;
				}
				 
				mTargetVideoMeta->setInt32(kKeyColorFormat, kTargetVideoColorFormat);    

				//create SpeedEffectSource
				sp<SpeedEffectSource> effectsource = new SpeedEffectSource(mVideoDecoder,false,false,mTargetVideoMeta,this,i,&mSpeedEffectParamList);

				 if(firstBuf != NULL){
					effectsource->setFirstBuffer(firstBuf);
				}
				 
				//initEncoderMeta();
				//creat video encoder		
			    int32_t width, height, stride, sliceHeight;
			    int64_t durationUs;
			    CHECK(mTargetVideoMeta->findInt32(kKeyWidth, &width));
			    CHECK(mTargetVideoMeta->findInt32(kKeyHeight, &height));
				
			    stride = ROUND_16(width);
			    sliceHeight = ROUND_16(height);
				
				mTargetVideoMeta->setInt32(kKeyStride, stride);
			    mTargetVideoMeta->setInt32(kKeySliceHeight, sliceHeight);
				ALOGI("Target Video Meta set to encoder,width=%d,height=%d,stride=%d,sliceHeight=%d",width,height,stride,sliceHeight);
				//whether we can not set bitrate
			    //mTargetMeta->setInt32(kKeyBitRate, xxx);
			    
			    mTargetVideoMeta->setInt32(kKeyIFramesInterval, 1); //second,should refine 
			    int32_t colorFormat = 0;
			   	effectsource->getFormat()->findInt32(kKeyColorFormat, &colorFormat);
			   	ALOGI("decoder output color format =0x%x",colorFormat);
			   	//decoder output color format may not suppor by encoder
			    mTargetVideoMeta->setInt32(kKeyColorFormat, kTargetVideoColorFormat);    
				//mTargetVideoMeta->setInt32(kKeyColorFormat, OMX_MTK_COLOR_FormatYV12);

			    // keep rotation info
			    int32_t rotationDegrees = 0;
			    if (trackMeta->findInt32(kKeyRotation, &rotationDegrees)) {
					ALOGI("src video track rotation = %d,set to target video meta",rotationDegrees);
			        mTargetVideoMeta->setInt32(kKeyRotation, rotationDegrees);
			    }
			    else {
			        ALOGD ("Cannot find kKeyRotation B !!!");
			    }
				
				const char * target_mime = NULL;
				CHECK(mTargetVideoMeta->findCString(kKeyMIMEType, &target_mime));
				ALOGI("Target Video Meta,mime=%s",target_mime);
				//target_mime string may be changed after mTargetVideoMeta set some meta
				//need refine just keep mTargetVideoMeta read only
				//and create new meta for encoder
                int32_t videoBitrate = 0;
				if(!mTargetVideoMeta->findInt32(kKeyBitRate, &videoBitrate) || videoBitrate <= 0){
					//query bitrate setting from codec
					VENC_DRV_VIDEO_PROPERTY_T rVideoProp;
					memset(&rVideoProp,0,sizeof(rVideoProp));
					VAL_UINT32_T u4BitRate = 0;
					if(!strcasecmp(target_mime, MEDIA_MIMETYPE_VIDEO_AVC)){
						rVideoProp.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H264;
					}else if(!strcasecmp(target_mime, MEDIA_MIMETYPE_VIDEO_H263)){
						rVideoProp.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H263;
					}else if(!strcasecmp(target_mime, MEDIA_MIMETYPE_VIDEO_MPEG4)){
						rVideoProp.eVideoFormat = VENC_DRV_VIDEO_FORMAT_MPEG4;
					}else if(!strcasecmp(target_mime, MEDIA_MIMETYPE_VIDEO_HEVC)){
						rVideoProp.eVideoFormat = VENC_DRV_VIDEO_FORMAT_HEVC;
					}else{
						ALOGI("Target Video Meta,mime2=%s",target_mime);
						ALOGE("Can't query bitrate setting for %s",target_mime);
						notify(SPEEDEFFECT_UNSUPPORTED_VIDEO,0,0);
						effectsource.clear();
						mVideoDecoder->stop();
						mVideoDecoder.clear();
						continue;
					}
					
					rVideoProp.u4Width = width;
					rVideoProp.u4Height = height;
					int32_t targetFps = 30;
					mTargetVideoMeta->findInt32(kKeyFrameRate, &targetFps);
					rVideoProp.u4FrameRate = targetFps;

					if(VENC_DRV_MRESULT_OK != 
						eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_PROPERTY,&rVideoProp,&u4BitRate)){
						ALOGE("Bitrate query fail! mime(%s),fps(%d),resolution(%d,%d)",\
							target_mime,targetFps,width,height);
						effectsource.clear();
						mVideoDecoder->stop();
						mVideoDecoder.clear();
						continue;
					}
					
					ALOGD("Bitrate setting = %d : mime(%s),fps(%d),resolution(%d,%d)",\
							u4BitRate,target_mime,targetFps,width,height);
					
					mTargetVideoMeta->setInt32(kKeyBitRate, u4BitRate);	
				}
				
				//mTargetVideoMeta->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);
				//ALOGI("color format %d",OMX_COLOR_FormatYUV420Planar);
				// create video encoder
				//we can modify the input buffer number
				ALOGI("Video encoder create start");
			    OMXClient mEncoderClient;
			    CHECK_EQ(mEncoderClient.connect(), (status_t)OK);
			    sp<MediaSource> encoder = OMXCodec::Create(mEncoderClient.interface(), mTargetVideoMeta, true /* createEncoder */, effectsource);
				if (encoder == NULL) {
					ALOGE ("Oops, Video encoder creation failed. [Unsupported Video]");
					notify(SPEEDEFFECT_UNSUPPORTED_VIDEO,0,0);
					effectsource.clear();
					mVideoDecoder->stop();
					mVideoDecoder.clear();
					//return OK;
					continue;
				}
				ALOGI("Video encoder create done");
				source = encoder;
				videoTrackInfo.mSpeedEffectSource = effectsource;
			}
			else { // if not need transcode
			
				//create SpeedEffectSource
				sp<SpeedEffectSource> effectsource = new SpeedEffectSource(videoSource,true,false,mTargetVideoMeta,this,i,&mSpeedEffectParamList);
				source = effectsource;
				videoTrackInfo.mSpeedEffectSource = effectsource;
			}
			
		    mWriter->addSource(source);
			mTrackInfoList.push_back(videoTrackInfo);
		}
	   	else if (!haveAudio && !strncasecmp(mime.string(), "audio/", 6)) {
			ALOGI("startSaveSpeedEffect,find audio track");
            haveAudio = true;
			sp<MediaSource> audioSource = extractor->getTrack(i);
			
			bool needTrans = true; //audio always need transcode
			needTrans = needTranscode(trackMeta,mTargetAudioMeta);
			
			TrackInfo audioTrackInfo;
			//videoTrackInfo.mTrack = extractor->getTrack(i);
			//audioTrackInfo.mime = mime;
			audioTrackInfo.mTrackId = i;
			
			if(needTrans){		
				 // create audio decoder
				ALOGI("create audio decoder start");
				OMXClient mADecoderClient;
				CHECK_EQ(mADecoderClient.connect(), (status_t)OK);
				sp<MediaSource> mAudioDecoder = OMXCodec::Create(mADecoderClient.interface(), trackMeta, false, // createdecoder
																					audioSource, NULL, 0 /*flags*/);
				if (mAudioDecoder == NULL) {
					ALOGE ("Oops, audio decoder creation failed. [Unsupported Audio]");
					//status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
					notify(SPEEDEFFECT_UNSUPPORTED_AUDIO,0,0);
					//return OK;
					continue;
				}
				ALOGI("create audio decoder done");
				//give a chance to check whether decoder support
				if(OK != mAudioDecoder->start()){
					ALOGE ("Oops, Audio decoder start failed. [Unsupported Audio]");
					notify(SPEEDEFFECT_UNSUPPORTED_AUDIO,0,0);
					//return OK;
					continue;
				}
				ALOGI("audio decoder started");
				
				//check whether need format change
				MediaBuffer* firstBuf = NULL;
				status_t firstBufferResult = OK;
				firstBufferResult = mAudioDecoder->read(&firstBuf);
				if (firstBufferResult == INFO_FORMAT_CHANGED) {
			        ALOGW("Audio INFO_FORMAT_CHANGED!!!");
			        CHECK(firstBuf == NULL);  
					//update src meta and targe meta
					sp<MetaData> format = mAudioDecoder->getFormat();
					updateMetaSrc2Dst(format,mTargetAudioMeta);				
			    }else if(firstBufferResult != OK){
					ALOGE ("Oops, Audio decoder read first buffer failed. [Unsupported Audio]");
					mAudioDecoder->stop();
					notify(SPEEDEFFECT_UNSUPPORTED_AUDIO,0,0);
					continue;
				}
				
				//create SpeedEffectSource
				sp<SpeedEffectSource> effectsource = new SpeedEffectSource(mAudioDecoder,false,true,mTargetAudioMeta,this,i,&mSpeedEffectParamList);
				audioTrackInfo.mSpeedEffectSource = effectsource;

				if(firstBuf != NULL){
					effectsource->setFirstBuffer(firstBuf);
				}
				
				ALOGI("create audio encoder start");

				//set encoder input buffer size
				int32_t max_size = 4096;
				int32_t mMaxSlmSpeed = 4;
				if (!((effectsource->getFormat())->findInt32(kKeyMaxInputSize, &max_size))){
                        max_size = 20000;             
				}
				 mTargetAudioMeta->setInt32(kKeyMaxInputSize, max_size *mMaxSlmSpeed);
				ALOGI("audio max_size:%d,encoder max input size=%d",max_size,max_size *mMaxSlmSpeed);

				// create audio encoder
			    OMXClient mAEncoderClient;
			    CHECK_EQ(mADecoderClient.connect(), (status_t)OK);
			    sp<MediaSource> encoder = OMXCodec::Create(mADecoderClient.interface(), mTargetAudioMeta, true /* createEncoder */, effectsource);
				if (encoder == NULL) {
					ALOGE ("Oops, audio encoder creation failed. [Unsupported Audio]");
					//status = MTK_VIDEO_TRANSCODER_ERROR_UNKNOWN;
					notify(SPEEDEFFECT_UNSUPPORTED_AUDIO,0,0);
					effectsource.clear();
					mAudioDecoder->stop();
					mAudioDecoder.clear();
					//return OK;
					continue;
				}
				source = encoder;
				ALOGI("create audio encoder done");
			}
			
		    mWriter->addSource(source);
			mTrackInfoList.push_back(audioTrackInfo);
		}

	}
	mRecordListener	 = new CSpeedEffectRecordListner(this); 
	mWriter->setListener(mRecordListener);

	//int64_t startTimeUs = systemTime() / 1000;
	//ALOGI("startSaveSpeedEffect,startTimeUs=%lld",startTimeUs);

	//sp<MetaData> meta = new MetaData;
	//setupMPEG4MetaData(startTimeUs, totalBitRate, &meta);
	mTargetMeta->setInt32(kKeyFileType, mOutputFormat);
	// start to record
    //CHECK_EQ(OK, writer->start());
    //CHECK_EQ((status_t)OK, writer->start(encMeta.get()));  // keep rotation info
    ALOGI("start MPEG4Writer");
    if ((status_t)OK != mWriter->start(mTargetMeta.get())) {
    	ALOGE ("MPEG4Writer start failed !");
        return UNKNOWN_ERROR;
    }
	mStarted= true;
	notify(SPEEDEFFECT_STARTED,0,0);
	ALOGI("startSaveSpeedEffect done");
	return OK;
	
}
status_t VideoSpeedEffect::stopSaveSpeedEffect(){
	ALOGI("stopSaveSpeedEffect start,require lock");
	Mutex::Autolock autoLock(mLock);
	ALOGI("stopSaveSpeedEffect start,mStarted=%d",mStarted);
	if(mStarted) {
		if(mLooper != NULL){
			mLooper->unregisterHandler(id());
			mLooper->stop();
			mLooper = NULL;
		}
		if(mWriter != NULL){
			mWriter->stop();
			mWriter = NULL;
		}
		//Trackinfo delete
		mTrackInfoList.clear();
		notify(SPEEDEFFECT_STOPPED,0,0);
		mStarted = false;
	}	
	ALOGI("stopSaveSpeedEffect done,mStarted=%d",mStarted);
	return OK;
}

//caller can call these interface to set target video and audio param---start
status_t VideoSpeedEffect::setOutputFormat(output_format of) {
    ALOGI("setOutputFormat: %d", of);
    if (of < OUTPUT_FORMAT_DEFAULT ||
        of >= OUTPUT_FORMAT_LIST_END) {
        ALOGE("Invalid output format: %d", of);
        return BAD_VALUE;
    }

    if (of == OUTPUT_FORMAT_DEFAULT) {
        mOutputFormat = OUTPUT_FORMAT_THREE_GPP;
    } else {
        mOutputFormat = of;
    }

    return OK;
}

status_t VideoSpeedEffect::setTargetVideoEncoder(video_encoder ve){
	ALOGI("setTargetVideoEncoder,target video encoder = %d",ve);
	switch (ve) {
		case VIDEO_ENCODER_H263:
			mTargetVideoMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_H263);
			break;

		case VIDEO_ENCODER_MPEG_4_SP:
			mTargetVideoMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);
			break;

		case VIDEO_ENCODER_H264:
			mTargetVideoMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_AVC);
			break;
#ifdef MTK_VIDEO_HEVC_SUPPORT
		case VIDEO_ENCODER_HEVC:
			mTargetVideoMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_HEVC);
			break;
#endif
		default:
			CHECK(!"Should not be here, unsupported video encoding.");
			return UNKNOWN_ERROR;
			
	}
	return OK;
}
status_t VideoSpeedEffect::setTargetAudioEncoder(audio_encoder ae){
	ALOGI("setTargetAudioEncoder,target audio encoder = %d",ae);
	switch(ae) {
		  case AUDIO_ENCODER_AMR_NB:
		  case AUDIO_ENCODER_AMR_WB:
		  case AUDIO_ENCODER_AAC:
		  case AUDIO_ENCODER_HE_AAC:
		  case AUDIO_ENCODER_AAC_ELD:
			  break;
	
		  default:
			  ALOGE("Unsupported audio encoder: %d", ae);
			  return UNKNOWN_ERROR;
	 }
	
	const char *mime;
    switch (ae) {
        case AUDIO_ENCODER_AMR_NB:
        case AUDIO_ENCODER_DEFAULT:
            mime = MEDIA_MIMETYPE_AUDIO_AMR_NB;
            break;
        case AUDIO_ENCODER_AMR_WB:
            mime = MEDIA_MIMETYPE_AUDIO_AMR_WB;
            break;
        case AUDIO_ENCODER_AAC:
            mime = MEDIA_MIMETYPE_AUDIO_AAC;
            mTargetAudioMeta->setInt32(kKeyAACProfile, OMX_AUDIO_AACObjectLC);
            break;
        case AUDIO_ENCODER_HE_AAC:
            mime = MEDIA_MIMETYPE_AUDIO_AAC;
            mTargetAudioMeta->setInt32(kKeyAACProfile, OMX_AUDIO_AACObjectHE);
            break;
        case AUDIO_ENCODER_AAC_ELD:
            mime = MEDIA_MIMETYPE_AUDIO_AAC;
            mTargetAudioMeta->setInt32(kKeyAACProfile, OMX_AUDIO_AACObjectELD);
            break;

        default:
            ALOGE("Unknown audio encoder: %d", ae);
            return UNKNOWN_ERROR;
    }
    mTargetAudioMeta->setCString(kKeyMIMEType, mime);
	return OK;

}
status_t VideoSpeedEffect::setParamAudioSamplingRate(int32_t sampleRate) {
    ALOGI("setParamAudioSamplingRate: %d", sampleRate);
    if (sampleRate <= 0) {
        ALOGE("Invalid audio sampling rate: %d", sampleRate);
        return BAD_VALUE;
    }

    // Additional check on the sample rate will be performed later.
    //mSampleRate = sampleRate;
	
    mTargetAudioMeta->setInt32(kKeySampleRate, sampleRate);
    return OK;
}

status_t VideoSpeedEffect::setParamAudioNumberOfChannels(int32_t channels) {
    ALOGI("setParamAudioNumberOfChannels: %d", channels);
    if (channels <= 0 || channels >= 3) {
        ALOGE("Invalid number of audio channels: %d", channels);
        return BAD_VALUE;
    }

    // Additional check on the number of channels will be performed later.
    //mAudioChannels = channels;
    mTargetAudioMeta->setInt32(kKeyChannelCount, channels);
    return OK;
}

status_t VideoSpeedEffect::setParamAudioEncodingBitRate(int32_t bitRate) {
    ALOGI("setParamAudioEncodingBitRate: %d", bitRate);
    if (bitRate <= 0) {
        ALOGE("Invalid audio encoding bit rate: %d", bitRate);
        return BAD_VALUE;
    }

    // The target bit rate may not be exactly the same as the requested.
    // It depends on many factors, such as rate control, and the bit rate
    // range that a specific encoder supports. The mismatch between the
    // the target and requested bit rate will NOT be treated as an error.
    //mAudioBitRate = bitRate;
    mTargetAudioMeta->setInt32(kKeyBitRate, bitRate);
    return OK;
}
status_t VideoSpeedEffect::setParamVideoEncodingBitRate(int32_t bitRate) {
    ALOGI("setParamVideoEncodingBitRate: %d", bitRate);
    if (bitRate <= 0) {
        ALOGE("Invalid video encoding bit rate: %d", bitRate);
        return BAD_VALUE;
    }

    // The target bit rate may not be exactly the same as the requested.
    // It depends on many factors, such as rate control, and the bit rate
    // range that a specific encoder supports. The mismatch between the
    // the target and requested bit rate will NOT be treated as an error.
    //mVideoBitRate = bitRate;
    mTargetVideoMeta->setInt32(kKeyBitRate, bitRate);
    return OK;
}
status_t VideoSpeedEffect::setParamVideoFrameRate(int32_t frameRate){
	 ALOGI("setParamVideoFrameRate: %d", frameRate);
    if (frameRate <= 0) {
        ALOGE("Invalid video encoding frame rate: %d", frameRate);
        return BAD_VALUE;
    }

    // The target bit rate may not be exactly the same as the requested.
    // It depends on many factors, such as rate control, and the bit rate
    // range that a specific encoder supports. The mismatch between the
    // the target and requested bit rate will NOT be treated as an error.
    mTargetVideoFrameRate = frameRate;
    mTargetVideoMeta->setInt32(kKeyFrameRate, frameRate);
    return OK;
}

//Besides above set target video and audio params interfaces
//Caller also can set all params throught setParameters
//such as "video-target-framerate = 30;video-target-bitrate = 17000*1000,video-target-width=1280,vieo-target-height=720 "

// Attempt to parse an int64 literal optionally surrounded by whitespace,
// returns true on success, false otherwise.
static bool safe_strtoi64(const char *s, int64_t *val) {
    char *end;

    // It is lame, but according to man page, we have to set errno to 0
    // before calling strtoll().
    errno = 0;
    *val = strtoll(s, &end, 10);

    if (end == s || errno == ERANGE) {
        return false;
    }

    // Skip trailing whitespace
    while (isspace(*end)) {
        ++end;
    }

    // For a successful return, the string must contain nothing but a valid
    // int64 literal optionally surrounded by whitespace.

    return *end == '\0';
}

// Return true if the value is in [0, 0x007FFFFFFF]
static bool safe_strtoi32(const char *s, int32_t *val) {
    int64_t temp;
    if (safe_strtoi64(s, &temp)) {
        if (temp >= 0 && temp <= 0x007FFFFFFF) {
            *val = static_cast<int32_t>(temp);
            return true;
        }
    }
    return false;
}

// Trim both leading and trailing whitespace from the given string.
static void TrimString(String8 *s) {
    size_t num_bytes = s->bytes();
    const char *data = s->string();

    size_t leading_space = 0;
    while (leading_space < num_bytes && isspace(data[leading_space])) {
        ++leading_space;
    }

    size_t i = num_bytes;
    while (i > leading_space && isspace(data[i - 1])) {
        --i;
    }

    s->setTo(String8(&data[leading_space], i - leading_space));
}

status_t VideoSpeedEffect::setParameters(const String8 &params) {
    ALOGI("setParameters: %s", params.string());
    const char *cparams = params.string();
    const char *key_start = cparams;
    for (;;) {
        const char *equal_pos = strchr(key_start, '=');
        if (equal_pos == NULL) {
            ALOGE("Parameters %s miss a value", cparams);
            return BAD_VALUE;
        }
        String8 key(key_start, equal_pos - key_start);
        TrimString(&key);
        if (key.length() == 0) {
            ALOGE("Parameters %s contains an empty key", cparams);
            return BAD_VALUE;
        }
        const char *value_start = equal_pos + 1;
        const char *semicolon_pos = strchr(value_start, ';');
        String8 value;
        if (semicolon_pos == NULL) {
            value.setTo(value_start);
        } else {
            value.setTo(value_start, semicolon_pos - value_start);
        }
        if (setParameter(key, value) != OK) {
            return BAD_VALUE;
        }
        if (semicolon_pos == NULL) {
            break;  // Reaches the end
        }
        key_start = semicolon_pos + 1;
    }
    return OK;
}

status_t VideoSpeedEffect::setParameter(
        const String8 &key, const String8 &value) {
    ALOGI("setParameter: key (%s) => value (%s)", key.string(), value.string());
     if (key == "audio-param-sampling-rate") {
        int32_t sampling_rate;
        if (safe_strtoi32(value.string(), &sampling_rate)) {
            return setParamAudioSamplingRate(sampling_rate);
        }
    } else if (key == "audio-param-number-of-channels") {
        int32_t number_of_channels;
        if (safe_strtoi32(value.string(), &number_of_channels)) {
            return setParamAudioNumberOfChannels(number_of_channels);
        }
    } else if (key == "audio-param-encoding-bitrate") {
        int32_t audio_bitrate;
        if (safe_strtoi32(value.string(), &audio_bitrate)) {
            return setParamAudioEncodingBitRate(audio_bitrate);
        }
    } else if (key == "video-param-encoding-bitrate") {
        int32_t video_bitrate;
        if (safe_strtoi32(value.string(), &video_bitrate)) {
            return setParamVideoEncodingBitRate(video_bitrate);
        }
    }  else if (key == "video-param-frame-rate") {
        int32_t video_framerate;
        if (safe_strtoi32(value.string(), &video_framerate)) {
            return setParamVideoFrameRate(video_framerate);
        }
    }   
	else {
        ALOGE("setParameter: failed to find key %s", key.string());
    }
    return BAD_VALUE;
}

//caller can call these interface to set target video and audio param---stop


status_t VideoSpeedEffect::transParamsToMeta(const String8 &params,sp<MetaData>& meta) {
	ALOGD("transParamsToMeta: %s", params.string());
   const char *cparams = params.string();
   const char *key_start = cparams;
   for (;;) {
	   const char *equal_pos = strchr(key_start, '=');
	   if (equal_pos == NULL) {
		   ALOGE("Parameters %s miss a value", cparams);
		   return BAD_VALUE;
	   }
	   String8 key(key_start, equal_pos - key_start);
	   TrimString(&key);
	   if (key.length() == 0) {
		   ALOGE("Parameters %s contains an empty key", cparams);
		   return BAD_VALUE;
	   }
	   const char *value_start = equal_pos + 1;
	   const char *semicolon_pos = strchr(value_start, ';');
	   String8 value;
	   if (semicolon_pos == NULL) {
		   value.setTo(value_start);
	   } else {
		   value.setTo(value_start, semicolon_pos - value_start);
	   }
	   if (setParamToMeta(key, value,meta) != OK) {
		   return BAD_VALUE;
	   }
	   if (semicolon_pos == NULL) {
		   break;  // Reaches the end
	   }
	   key_start = semicolon_pos + 1;
   }
   return OK;

}
status_t VideoSpeedEffect::setParamToMeta(const String8 &key, const String8 &value,sp<MetaData>& meta) {
	ALOGD("setParamToMeta: key (%s) => value (%s)", key.string(), value.string());
	if (key == "videoframe-droprate") {
	    int32_t videoDropRate;
		if (safe_strtoi32(value.string(), &videoDropRate)) {
					meta->setInt32(kKeyVideoDropRate,videoDropRate);
		}
		return OK;
	} else if (key == "slow-motion-speed") {
		int32_t slmSpeed;
		if(safe_strtoi32(value.string(), &slmSpeed)){
			meta->setInt32(kKeySlowMotionSpeed,slmSpeed);
		}
		return OK;
	} else if (key == "fast-motion-speed"){
		int32_t fmSpeed;
		if(safe_strtoi32(value.string(), &fmSpeed)){
			meta->setInt32(kKeyFastMotionSpeed,fmSpeed);
		}
		return OK;

	}else {
		ALOGE("setParamToMeta: failed to find key %s", key.string());
	}
	return BAD_VALUE;

}
status_t VideoSpeedEffect::notify(int msg, int ext1, int ext2) {
	sp<AMessage> notify = new AMessage('notf',this);
	notify->setInt32("msg",msg);
	notify->setInt32("ext1",ext1);
	notify->setInt32("ext2",ext2);
	notify->post();
	//mListener->notify(msg,ext1,ext2);
	return OK;
}

bool VideoSpeedEffect::needTranscode(sp<MetaData> srcMeta,sp<MetaData> dstMeta) {
	bool needTrans = false;
	const char* srcMime = NULL;
	const char* dstMime = NULL;
	const char* newDstMime = NULL;
	CHECK(srcMeta->findCString(kKeyMIMEType, &srcMime));

	if(!(dstMeta->findCString(kKeyMIMEType, &dstMime))){
		dstMime = srcMime;
		//if src is HEVC and if need transcode hevc to h.264
#ifdef MTK_VIDEO_HEVC_SUPPORT
		if(!strcmp(MEDIA_MIMETYPE_VIDEO_HEVC, dstMime)
				&& mNeedTransHEVC2H264){
			
			dstMime = MEDIA_MIMETYPE_VIDEO_AVC;
			ALOGI("Transcode HEVC to AVC");
		}
#endif
		dstMeta->setCString(kKeyMIMEType,dstMime);

		if(!strcmp(MEDIA_MIMETYPE_AUDIO_AAC, dstMime)){
			int32_t aacProfile = OMX_AUDIO_AACObjectLC;
			srcMeta->findInt32(kKeyAACProfile, &aacProfile);
			dstMeta->setInt32(kKeyAACProfile, aacProfile);
			ALOGI("AAC profile =%d",aacProfile);
		}
	}
	ALOGI("needTranscode,source mime =%s,dest mime =%s",srcMime,dstMime);
	
	if(!strncasecmp(srcMime, "video/", 6)){
		int32_t srcWidth = 0, srcHeight = 0;
		int32_t dstWidth = 0,dstHeight = 0;
		CHECK(srcMeta->findInt32(kKeyWidth, &srcWidth) && (srcWidth > 0));
		CHECK(srcMeta->findInt32(kKeyHeight, &srcHeight) && (srcHeight > 0));

		if(!dstMeta->findInt32(kKeyWidth, &dstWidth)){
			dstWidth = srcWidth;
			dstMeta->setInt32(kKeyWidth,dstWidth);
		}
		if(!dstMeta->findInt32(kKeyHeight, &dstHeight)){
			dstHeight = srcHeight;
			dstMeta->setInt32(kKeyHeight,dstHeight);
		}
		
		int32_t srcFrameRate = 0,dstFrameRate = 0;
		CHECK(srcMeta->findInt32(kKeyFrameRate, &srcFrameRate) && (srcFrameRate > 0));

		if(!dstMeta->findInt32(kKeyFrameRate, &dstFrameRate) ||(dstFrameRate > srcFrameRate)){
			dstFrameRate = srcFrameRate;
			dstMeta->setInt32(kKeyFrameRate,dstFrameRate);
		}
		ALOGI("needTranscode,src width =%d,src height =%d,src framerate =%d",\
			 srcWidth,srcHeight,srcFrameRate);
		ALOGI("needTranscode,dst width =%d,dst height =%d,dst framerate =%d",\
			dstWidth,dstHeight,dstFrameRate);
		
		dstMeta->findCString(kKeyMIMEType, &newDstMime);
		if(strcasecmp(srcMime,newDstMime)){
			needTrans = true;
			ALOGI("needTrasncode,mime need change,need Transcode");
			return needTrans;
		}
		else if((dstWidth != srcWidth) || (dstHeight != srcHeight)){
			needTrans = true;
			ALOGI("needTrasncode,video size need change,need Transcode");
			return needTrans;
		}
		else if((dstFrameRate != srcFrameRate)) {
			needTrans = false;
			//if frame rate need change,
			//need further check whether all the effect no need to drop frame
			//if so, no need transcode
			//int64_t duraitonUs= 0;
			//srcMeta->findInt64(kKeyDuration, &duraitonUs)
			int32_t nonRefPFreq = 0;
			uint32_t nonRefP_numerator = 0;
			uint32_t nonRefP_denominator = 0;
			float supportDropRate = 1;
			if(srcMeta->findInt32(kKeyNonRefPFreq,&nonRefPFreq)) {
				nonRefP_numerator = (nonRefPFreq >> 16) & 0x0000FFFF;
				nonRefP_denominator = nonRefPFreq & 0x0000FFFF;
				if(nonRefP_denominator > 0 && nonRefP_denominator > 0){
					supportDropRate = (float)nonRefP_denominator/(nonRefP_denominator-nonRefP_numerator);
				}
				ALOGI("non-reference P info:%d/%d,supported max drop rate = %f",\
					nonRefP_numerator,nonRefP_denominator,supportDropRate);		
			}
			for(List<SpeedEffectParam>::iterator i = mSpeedEffectParamList.begin(); i != mSpeedEffectParamList.end(); i++){
				float droprate = 1;
				if((*i).mMeta->findFloat(kKeyVideoDropRate,&droprate) && droprate > 1){
					
					ALOGD("speed effect:%lld--%lld,need droprate=%f, supported drop rate=%f",\
						(*i).mStartPos,(*i).mEndPos,droprate,supportDropRate);
					if(droprate > supportDropRate){
						ALOGD("dropRate > supported with non-reference P,need Transcoding");
						needTrans = true;
						break;
					}
				}		
			}
			//return needTranscode;
			
		//if frame rate need change, 
		//need further check if source file has frames encoded with non-reference p

		//whether can get non-reference p info from file format?
		//if no, need write the non-Reference P info (frame rate) to file

		//compare non reference p frame rate with frame drop  rate
		//to check whether the non reference p frame rate is enough for drop to target frame rate 
			/*
			int32_t nonRefPInfo = 0;
			if(srcMeta->findInt32(kKeyNonRefPInfo,&nonRefPInfo) && (nonRefPInfo > 0)) {
				int32_t	frameDropRate  = 0;
				frameDropRate = dstFrameRate / srcFrameRate;	
				if(nonRefPInfo > frameDropRate){
					ALOGI("needTranscode,non-reference p info enough, no need transcode");
					needTrans = false;
				}		
						
			}
			*/
			//needTrans = false;//for test
			return needTrans;
		}
			
	}else if(!strncasecmp(srcMime, "audio/", 6)){
		needTrans = true;
		
		int32_t sampleRate = 48000;
		int32_t channels = 2;
		int32_t bitrate = 128000;
		
		if(!(dstMeta->findInt32(kKeySampleRate, &sampleRate))){
			srcMeta->findInt32(kKeySampleRate, &sampleRate);
			dstMeta->setInt32(kKeySampleRate, sampleRate);
		}
		if(!(dstMeta->findInt32(kKeyChannelCount, &channels))){
			srcMeta->findInt32(kKeyChannelCount, &channels);
			dstMeta->setInt32(kKeyChannelCount, channels);
		}
		if(!(dstMeta->findInt32(kKeyBitRate, &bitrate))){
			srcMeta->findInt32(kKeyBitRate, &bitrate);
			dstMeta->setInt32(kKeyBitRate, bitrate);
		}
		if(!strcasecmp(dstMime, MEDIA_MIMETYPE_AUDIO_AAC)){
			int32_t aacProfile = OMX_AUDIO_AACObjectLC;
			srcMeta->findInt32(kKeyAACProfile, &aacProfile);
			dstMeta->setInt32(kKeyAACProfile, aacProfile);
			ALOGI("target audio AAC profile =%d",aacProfile);
		}
		ALOGI("Target Audio Meta: mime(%s),SampleRate(%d),channel(%d),bitrate(%d)",\
					dstMime,sampleRate,channels,bitrate);
	}
	return needTrans;
}
void VideoSpeedEffect::updateMetaSrc2Dst(sp<MetaData> srcMeta,sp<MetaData> dstMeta){
	bool success = false;	
	int32_t sampleRate = 16000;
    success = srcMeta->findInt32(kKeySampleRate, &sampleRate);
    CHECK(success);
	dstMeta->setInt32(kKeySampleRate, sampleRate);

    int32_t numChannels = 2;
    success = srcMeta->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);
	dstMeta->setInt32(kKeyChannelCount, numChannels);
	ALOGI("updateMetaSrc2Dst,sampleRate=%d,numChannels=%d",sampleRate,numChannels);
}

status_t VideoSpeedEffect::checkSpeedEffectList(List<SpeedEffectParam>& speedEffect,sp<MediaExtractor> extractor){
	ALOGI("checkSpeedEffectList ++");
	//get file meta, video meta
	sp<MetaData> fileMeta,meta,videometa,audiometa;
	int64_t durationUs = 0;
	//fileMeta = extractor->getMetaData();
	//fileMeta->findInt64(kKeyDuration,&durationUs);
	//ALOGI("checkSpeedEffectList,file duration=%lld",durationUs);
	//mSrcVideoFrameRate= mTargetVideoFrameRate= 20;
	const char *_mime;
	for (size_t i = 0; i < extractor->countTracks(); ++i) {
		meta = extractor->getTrackMetaData(i);
		CHECK(meta->findCString(kKeyMIMEType, &_mime));
		String8 mime = String8(_mime);
		if(!strncasecmp(mime.string(), "video/", 6)){
			videometa =meta;
			videometa->findInt64(kKeyDuration, &durationUs);
			ALOGI("checkSpeedEffectList,video track duration =%lld",durationUs);
			if(mFileDurationUs < 0 || durationUs > mFileDurationUs){
           		 mFileDurationUs = durationUs;
			}
			videometa->findInt32(kKeyFrameRate,&mSrcVideoFrameRate);
			ALOGI("checkSpeedEffectList,has video track,src video fps =%d",mSrcVideoFrameRate);
		}
		if(!strncasecmp(mime.string(), "audio/", 6)){
			audiometa = meta;
			audiometa->findInt64(kKeyDuration, &durationUs);
			ALOGI("checkSpeedEffectList,Audio Track duration =%lld",durationUs);
			if(mFileDurationUs < 0 || durationUs > mFileDurationUs){
           		 mFileDurationUs = durationUs;
			}
		}
	}
	//mSrcVideoFrameRate= mTargetVideoFrameRate= 20;
	float dropRate = 1.0;
	for(List<SpeedEffectParam>::iterator i = speedEffect.begin(); i != speedEffect.end();i++ ){
		if(i == speedEffect.begin()){
			if((*i).mStartPos > 0){
				SpeedEffectParam item;
				item.mStartPos = 0;
				item.mEndPos = (*i).mStartPos;
				item.mMeta = new MetaData();
				ALOGI("add speed effect item at the begin, 0--(%lld)",(*i).mStartPos);
				item.mMeta->setInt32(kKeySlowMotionSpeed,1);//no speed effect
				
				//float dropRate = 1;
				if(videometa.get() && (mSrcVideoFrameRate > 0)){
					dropRate = (float)mSrcVideoFrameRate/mTargetVideoFrameRate;
					ALOGI("add speed effect itme at he begin,dropRate =%f,targetfps=%d",dropRate,mTargetVideoFrameRate);
					item.mMeta->setFloat(kKeyVideoDropRate,dropRate);
					item.mMeta->setInt32(kKeyFrameRate,mTargetVideoFrameRate);	
				}
				speedEffect.push_front(item);
					
			}
		}
		//if not drop rate set,set the drop rate according to the target frame rate
	
		if(videometa.get() && (mSrcVideoFrameRate > 0)){
			if(!(*i).mMeta->findFloat(kKeyVideoDropRate,&dropRate)){
				int32_t iTargetfps = mTargetVideoFrameRate;
				(*i).mMeta->setInt32(kKeyFrameRate,iTargetfps);
					
				int slmSpeed = 1;
				(*i).mMeta->findInt32(kKeySlowMotionSpeed, &slmSpeed);
				int newSrcfps = mSrcVideoFrameRate/slmSpeed;

				if(iTargetfps <= newSrcfps){
					(*i).mMeta->setFloat(kKeyVideoDropRate,(float)newSrcfps/iTargetfps);
				}
				ALOGI("old item,target frame rate=%d,droprate=%f",iTargetfps,(float)newSrcfps/iTargetfps);
			}
			//add handle for with drop rate setting
		}
		List<SpeedEffectParam>::iterator j = i;
		if((++j) == speedEffect.end()){
			ALOGI("now the item is the last one");
			if((mFileDurationUs > 0) &&((*i).mEndPos < mFileDurationUs)){
				SpeedEffectParam item;
				item.mStartPos = (*i).mEndPos;
				item.mEndPos = mFileDurationUs;	
				item.mMeta = new MetaData();
				ALOGI("add speed effect item at the end, (%lld)--(%lld)",item.mStartPos,item.mEndPos);
				item.mMeta->setInt32(kKeySlowMotionSpeed,1);//no speed effect

				//float dropRate = 1;
				if(videometa.get() && (mSrcVideoFrameRate > 0)){
					dropRate = (float)mSrcVideoFrameRate/mTargetVideoFrameRate;
					item.mMeta->setFloat(kKeyVideoDropRate,dropRate);
					item.mMeta->setInt32(kKeyFrameRate,mTargetVideoFrameRate);
					ALOGI("new end item,target frame rate=%d,droprate=%f",mTargetVideoFrameRate,dropRate);
					
				}
				speedEffect.push_back(item);
				i++;
			}
		}else
			ALOGI("now the item is not the last one");
	}
	
	ALOGI("checkSpeedEffectList-- ,speedEffect list num =%d",speedEffect.size());

	return OK;
}

void VideoSpeedEffect::onMessageReceived(const sp<AMessage> &msg) {
	switch (msg->what()) {
		case 'prog':
		{
			onNotifyProgress(msg);
			break;
		}
		case 'notf':
		{
			int message = SPEEDEFFECT_NOP; 
			int ext1, ext2;
			msg->findInt32("msg",&message);
			msg->findInt32("ext1",&ext1);
			msg->findInt32("ext2",&ext2);
			mListener->notify(message,ext1,ext2);
			break;
		}
		default:
            ALOGE("UNKNOW message"); 
            break;
	}
	
}
status_t VideoSpeedEffect::onNotifyProgress(const sp<AMessage> &msg){
	ALOGI("onNotifyProgress,before lock");
	Mutex::Autolock autoLock(mLock);
	ALOGI("onNotifyProgress,mTrackInfoList num =%d",mTrackInfoList.size());
	int64_t progressUs = mFileDurationUs;
	for(List<TrackInfo>::iterator i = mTrackInfoList.begin(); i != mTrackInfoList.end(); i++){
		int64_t trackPositionUs = 0;
		(*i).mSpeedEffectSource->getPosition(&trackPositionUs);
		ALOGI("onNotifyProgress,trackPositon =%lld",trackPositionUs);
		if(trackPositionUs <= progressUs){
			progressUs = trackPositionUs;
		}
	}
	
	int32_t percentage = 100.0 * (double)progressUs/ mFileDurationUs ;
	ALOGI("onNotifyProgress,percentage%d,progressUs=%lld",percentage,progressUs);
	bool eos = mWriter->reachedEOS();
	if(!eos && (percentage >= 100)){
		ALOGW("not eos,but percentage(%d) >100",percentage);
		percentage = 99;
	}
	else if(eos){
		ALOGI("EOS received");
		percentage = 100;
		mListener->notify(SPEEDEFFECT_COMPLETE,0,0);
	}	
	mListener->notify(SPEEDEFFECT_PROGRESS_UPDATE,percentage,0);

	msg->post(kProgressDelayUs);//update progress in  200ms
	return OK;
}

/*
static inline size_t audio_bytes_per_sample(audio_format_t format)
{
    size_t size = 0;
    switch (format) {
    case AUDIO_FORMAT_PCM_32_BIT:
    case AUDIO_FORMAT_PCM_8_24_BIT:
        size = sizeof(int32_t);
        break;
    case AUDIO_FORMAT_PCM_16_BIT:
        size = sizeof(int16_t);
        break;
    case AUDIO_FORMAT_PCM_8_BIT:
        size = sizeof(uint8_t);
        break;
    default:
        break;
    }
    return size;
}
*/

VideoSpeedEffect::SpeedEffectSource::SpeedEffectSource(sp<MediaSource> source,bool isBitstream,bool isAudio,sp<MetaData> targetMeta,/*int32_t queueBufferNum,*/
														sp<VideoSpeedEffect> owner,int32_t trackID,List<SpeedEffectParam> *pSpeedEffectParamList){
	ALOGI("SpeedEffectSource constructor");
	msource = source;
	mMeta = source->getFormat();
	//remove slow motion flag:
	mMeta->remove(kKeySlowMotionSpeedValue);
	const char *mime;
    mMeta->findCString(kKeyMIMEType, &mime);
	mIsAvc = !strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_AVC);
	mSrcColorFormat = 0;
	mMeta->findInt32(kKeyColorFormat, &mSrcColorFormat);
	//mQueueBufferNum = queueBufferNum;
	mOwner = owner;
	mTrackId = trackID;		
	mStarted = false;
	mIsBitstream = isBitstream;
	mIsAudio = isAudio;
	mpSpeedEffectParamList = pSpeedEffectParamList;
	
	mLastTimestamp = -1;
	mLastOrigTimestamp = 0;
	mLastDelta = 0;
	mGetNewSpeed = false;
	mLastSLMSpeed = 1; 
	mNowSLMSpeed = 1;
	mDequedLastBuffer = false;

	mSampleRate = 48000;
	mChannelCount = 2;
	mAudioFormat = AUDIO_FORMAT_PCM_16_BIT;
	mBytesPerSample = audio_bytes_per_sample(mAudioFormat);
	mTStrIntBufSampeCount = TIMESTRETCH_INTERNAL_BUFFER_SAMPLE_COUNT;
	if(mIsAudio){
		mMeta->findInt32(kKeySampleRate, &mSampleRate);
		mMeta->findInt32(kKeyChannelCount,&mChannelCount);
		ALOGI("audio,sample rate = %d,channel count = %d",mSampleRate,mChannelCount);
		//all pcm from audio decoder is 16 bit
		mAudioFormat = AUDIO_FORMAT_PCM_16_BIT;
		mBytesPerSample = audio_bytes_per_sample(mAudioFormat);
	}
	mSourceBuffer  = NULL;

	mSectionBufferDurUs = 0;
	mAudioTimeStrecth = NULL;
	mIsFirstFrame = true;

	//video parameters init
	mTargetWidth = 0;
	mTargetHeight = 0;
	mPtrBufferGroup = NULL;
	mNeedFormatChange = false;
	if(targetMeta.get()){
		mTargetMeta = targetMeta;
	}

	mVideoFpsControlTable = NULL;

	//for print debug info
	mPrintDebugInfo = false;
	mFrameCount = 0;
	mLastNewTimeUs = 0; // for debug
	mLastKeepFrameNewTimeUs = 0; // for debug
	mKeepFrameNum = 0;
	mExpectedTargetFrameDurUs = 1000000ll/30; // for debug,default as 30fps
	
	if(!mIsAudio){
	    CHECK(mTargetMeta->findInt32(kKeyWidth, &mTargetWidth));
	    CHECK(mTargetMeta->findInt32(kKeyHeight, &mTargetHeight));
	    mTargetMeta->findInt32(kKeyColorFormat, &mDstColorFormat);
		if(!mIsBitstream){
			mNeedFormatChange = needFormatChange(mMeta,mTargetMeta);
			ALOGI("video need format change =%d",mNeedFormatChange);
		}	

		int32_t targetfps = 30;
		mTargetMeta->findInt32(kKeyFrameRate, &targetfps);

		
		mExpectedTargetFrameDurUs = 1000000ll/targetfps;
		ALOGD("SpeedEffectSource,mExpectedTargetFrameDurUs =%lld",mExpectedTargetFrameDurUs);

		//for debug info
	   	char value[PROPERTY_VALUE_MAX];
        if (property_get("media.speedEffect.debug", value, NULL)
                && (!strcmp(value, "1") || !strcasecmp(value, "true"))) {
            mPrintDebugInfo = true;
			ALOGI("SpeedEffectSource,print debug info enable");
        }
	}
    mOrigLastTimestamp = -1;
    mNextAudioTimestamp = -1;
    mSectionFirstOrigTimestamp = -1;
    mSectinoFirstNewTimestamp = -1;
    mLastMaintainTimestamp = -1;
}

VideoSpeedEffect::SpeedEffectSource::~SpeedEffectSource(){
	ALOGI("SpeedEffectSource destructor");
	if(mAudioTimeStrecth){
		delete mAudioTimeStrecth;
	}
  	if (mPtrBufferGroup != NULL) {
        delete mPtrBufferGroup;
	    mPtrBufferGroup = NULL;
    }
	if(mSourceBuffer != NULL){
		mSourceBuffer->release();
	}
	if(mVideoFpsControlTable){
		if(mIsBitstream){
			VideoFpsControlTable<float>* pFpsControlTable = (VideoFpsControlTable<float>*)mVideoFpsControlTable;
			delete pFpsControlTable;
			pFpsControlTable = NULL;
			mVideoFpsControlTable = NULL;
		}else{
			VideoFpsControlTable<int64_t>* pFpsControlTable = (VideoFpsControlTable<int64_t>*)mVideoFpsControlTable;
			delete pFpsControlTable;
			pFpsControlTable = NULL;
			mVideoFpsControlTable = NULL;
		}
		
	}
}

status_t VideoSpeedEffect::SpeedEffectSource::start(MetaData *params){

    Mutex::Autolock autoLock(mLock);
	ALOGI("SpeedEffectSource start");
	if(mStarted){
		ALOGW("SpeedEffectSource already in started status");
		return OK;
	}
    //CHECK(!mStarted);

	status_t err = OK;
	if(mIsBitstream && !mIsAudio){
		ALOGI("SpeedEffectSource start,start source start");
		params->setInt32(kKeyWantsNALFragments, false);
		//let parser return buffer in frame
		err = msource->start(params);
		
		ALOGI("SpeedEffectSource start,start source done,err=%d",err);
	}
	if (err != OK) {
		ALOGE("failed to start video source");
		msource.clear();
		return err;
	}

	mStarted = true;
	
	/*
	if(!msource.get() && queueBufferNum > 0){
	//push mode

	//create buffer queue
	
	}

	mGroup = new MediaBufferGroup;

    int32_t max_size;
    CHECK(mFormat->findInt32(kKeyMaxInputSize, &max_size));

    mGroup->add_buffer(new MediaBuffer(max_size));

	mSrcBuffer = new uint8_t[max_size];

    mStarted = true;
    */

    return OK;

}
status_t VideoSpeedEffect::SpeedEffectSource::stop(){
	ALOGI("SpeedEffectSource stop,require lock");
	Mutex::Autolock autoLock(mLock);
	ALOGI("SpeedEffectSource stop,mStarted=%d",mStarted);
	
	if(mStarted){
		ALOGI("SpeedEffectSource stop, stop source begin ");
		msource->stop();
		ALOGI("SpeedEffectSource stop, stop source ends ");
		mStarted = false;
	}
    return OK;
}
sp<MetaData> VideoSpeedEffect::SpeedEffectSource::getFormat(){
	Mutex::Autolock autoLock(mLock);
	return mMeta;
}
status_t VideoSpeedEffect::SpeedEffectSource::getPosition(int64_t *positionUs){
	Mutex::Autolock autoLock(mLock);
	//ALOGI("SpeedEffectSource,getPosition %lld",mLastOrigTimestamp);
	*positionUs = mLastOrigTimestamp;
	return OK;
}
bool VideoSpeedEffect::SpeedEffectSource::needFormatChange(sp<MetaData> srcMeta,sp<MetaData> dstMeta){
	bool needFormatChange = false;
	int32_t srcColorFormat = OMX_COLOR_FormatVendorMTKYUV;
	int32_t dstColorFormat = kTargetVideoColorFormat;
	int32_t srcWidth = 0,srcHeight = 0,dstWidth = 0,dstHeight = 0;
	
	srcMeta->findInt32(kKeyColorFormat, &srcColorFormat);
	dstMeta->findInt32(kKeyColorFormat, &dstColorFormat);
	
	srcMeta->findInt32(kKeyWidth, &srcWidth);
	srcMeta->findInt32(kKeyHeight, &srcHeight);

	dstMeta->findInt32(kKeyWidth, &dstWidth);
	dstMeta->findInt32(kKeyHeight, &dstHeight);

	if(srcColorFormat != dstColorFormat ||
		srcWidth != dstWidth || srcHeight != dstHeight ){
		needFormatChange = true;
	}
	return needFormatChange;
}
bool VideoSpeedEffect::SpeedEffectSource::isNonReferenceFrame(MediaBuffer* buffer,sp<MetaData> bufferMeta){
	bool isNonReferneceFrame  = false;
	const char *mime = NULL;
    bufferMeta->findCString(kKeyMIMEType, &mime);
	//ALOGD("isNonReferenceFrame,mime=%s",mime);
	if(!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_AVC)){
		if (buffer->range_length() < 4) {
       		return isNonReferneceFrame;
    	}

	    const uint8_t *ptr =
	        (const uint8_t *)buffer->data() + buffer->range_offset();

		//ALOGD("ptr[0]=0x%x,ptr[1]=0x%x,ptr[2]=0x%x,ptr[3]=0x%x,ptr[4]=0x%x",\
		//	*ptr,*(ptr+1),*(ptr+2),*(ptr+3),*(ptr+4));
		if (!memcmp(ptr, "\x00\x00\x00\x01", 4)) {
	        ptr += 4;
			
			uint32_t nal_ref_idc = (*ptr) >> 5;
			//ALOGD("nal_ref_idc =%d",nal_ref_idc);
			if(nal_ref_idc == 0){
				ALOGD("nal_ref_idc == 0,not reference by other frame,can drop ");
				isNonReferneceFrame = true;
			}
	    }
				
	}
	
#ifdef MTK_VIDEO_HEVC_SUPPORT
	if(!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_HEVC)){
		if (buffer->range_length() < 4) {
       		return isNonReferneceFrame;
    	}
		 const uint8_t *ptr =
	        (const uint8_t *)buffer->data() + buffer->range_offset();

	    if (!memcmp(ptr, "\x00\x00\x00\x01", 4)) {
	        ptr += 4;
			
			uint32_t nal_ref_flag = (*ptr) >> 6;
			//ALOGD("nal_ref_flag =%d",nal_ref_flag);
			if(nal_ref_flag == 0){
				ALOGD("nal_ref_flag == 0,not reference by other frame,can drop ");
				isNonReferneceFrame = true;
			}
	    }	
		
	}
#endif 
	return isNonReferneceFrame;
}


DpColorFormat OmxColorToDpColor(int32_t omx_color_format) {
    DpColorFormat colorFormat;

    switch (omx_color_format) {
        case OMX_COLOR_FormatVendorMTKYUV:
            colorFormat = eNV12_BLK;
            break;
        case OMX_COLOR_FormatYUV420Planar:
            colorFormat = eYUV_420_3P;
            break;
		case OMX_MTK_COLOR_FormatYV12:
		    colorFormat = eYV12;
		    break;
        default:
            colorFormat = eYUV_420_3P;
            ALOGE ("[Warning] Cannot find color mapping !!");
            break;
    }

    return colorFormat;
}
void VideoSpeedEffect::SpeedEffectSource::convertVideoFrame(uint8_t* input, int32_t srcWidth, int32_t srcHeight, int32_t srcStride, int32_t srcSliceHeight,
                                     uint8_t* output, int32_t dstWidth, int32_t dstHeight) {
    ALOGD("+SpeedEffectSource::convertVideoFrame, tid:%d Src[%d, %d] (%d, %d)[0x%08X], Dst[%d, %d][0x%08X]", gettid(), srcWidth, srcHeight, srcStride, srcSliceHeight, input, dstWidth, dstHeight, output);

#ifdef USE_MTK_MDP_MHAL
    MHAL_BOOL LockScenario = MHAL_FALSE;
    MHalLockParam_t inLockParam;
    inLockParam.mode = MHAL_MODE_BITBLT;
    inLockParam.waitMilliSec = 1000;
    inLockParam.waitMode = MHAL_MODE_BITBLT;

#if 1
    if(MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_LOCK_RESOURCE, (MHAL_VOID *)&inLockParam, sizeof(inLockParam), NULL, 0, NULL))
    {
        ALOGE("[BITBLT][ERROR] mHalIoCtrl() - MT65XX_HW_BITBLT Can't Lock!!!!, TID:%d", gettid());
        LockScenario = MHAL_FALSE;
    }
    else
    {
        ALOGE("[BITBLT] mHalIoCtrl() - MT65XX_HW_BITBLT Lock!!!!, TID:%d", gettid());
        LockScenario = MHAL_TRUE;
    }
#endif

    if (LockScenario == MHAL_FALSE) {
    	ALOGD("Cannot lock HW !!!!, TID:%d", gettid());
        return;
    }

    MHAL_UINT8 *srcYUVbuf_va = NULL;
    MHAL_UINT8 *dstYUVbuf_va = NULL;
    MHAL_UINT32 srcBufferSize = 0;
    MHAL_UINT32 dstBufferSize = 0;

    mHalBltParam_t bltParam;
    memset(&bltParam, 0, sizeof(bltParam));

    //srcBufferSize = ((((srcWidth * srcHeight * 3) >> 1)+(MEM_ALIGN_32-1)) & ~(MEM_ALIGN_32-1));
    //srcBufferSize = ROUND_32(YUV_SIZE(srcWidth, srcHeight));
    srcBufferSize = ROUND_32(YUV_SIZE(ROUND_16(srcWidth), ROUND_16(srcHeight)));

    srcYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, srcBufferSize);    // 32 byte alignment for MDP

/*
    dstBufferSize = ((((dstWidth * dstHeight * 3) >> 1)+(MEM_ALIGN_32-1)) & ~(MEM_ALIGN_32-1));
    dstYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, dstBufferSize);    // 32 byte alignment for MDP
*/
    dstYUVbuf_va = output;

    memcpy(srcYUVbuf_va, input, YUV_SIZE(ROUND_16(srcWidth), ROUND_16(srcHeight)));

    uint32_t color_format = MHAL_FORMAT_YUV_420;

#if 1
    switch (mSrcColorFormat) {
        case OMX_COLOR_FormatVendorMTKYUV:
            color_format = MHAL_FORMAT_MTK_YUV;
            break;
        case OMX_COLOR_FormatYUV420Planar:
            color_format = MHAL_FORMAT_YUV_420;
            break;
        default:
            color_format = MHAL_FORMAT_YUV_420;
            break;
    }
#endif

    bltParam.srcAddr = (MHAL_UINT32)srcYUVbuf_va;
    bltParam.srcFormat = color_format;
    bltParam.srcX = 0;
    bltParam.srcY = 0;
    bltParam.srcW = srcWidth;
    bltParam.srcWStride = ROUND_16(srcWidth);
    bltParam.srcH = srcHeight;
    bltParam.srcHStride = ROUND_16(srcHeight);

    bltParam.dstAddr = (MHAL_UINT32)dstYUVbuf_va;
    bltParam.dstFormat = color_format;
    bltParam.dstW = dstWidth;
    bltParam.dstH = dstHeight;
    bltParam.pitch = dstWidth; //_mDisp.dst_pitch;
    //bltParam.orientation = _mRotation;
    bltParam.orientation = 0;

    ALOGD ("+MHAL_IOCTL_BITBLT");
#if 1
    if (MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_BITBLT, &bltParam, sizeof(bltParam), NULL, 0, NULL))
    //if(MHAL_NO_ERROR != mHalMdpIpc_BitBlt(&bltParam))
    {
        ALOGE("[BITBLT][ERROR] IDP_bitblt() can't do bitblt operation");
        free (srcYUVbuf_va);
        //free (dstYUVbuf_va);
        return;
    }
    else {
    	//memcpy(output, dstYUVbuf_va, (dstWidth * dstHeight * 3) >> 1);
        free (srcYUVbuf_va);
        //free (dstYUVbuf_va);
    }
#endif
    ALOGD ("-MHAL_IOCTL_BITBLT");

    MHAL_UINT32 lock_mode;
    lock_mode = MHAL_MODE_BITBLT;

#if 1
    if(MHAL_NO_ERROR != mHalIoCtrl(MHAL_IOCTL_UNLOCK_RESOURCE, (MHAL_VOID *)&lock_mode, sizeof(lock_mode), NULL, 0, NULL))
    {
        ALOGD("[BITBLT][ERROR] mHalIoCtrl() - MT65XX_HW_BITBLT Can't UnLock!!!!, TID:%d", gettid());
    }
    else
    {
        ALOGD("[BITBLT] mHalIoCtrl() - MT65XX_HW_BITBLT UnLock!!!!, TID:%d", gettid());
    }
#endif

#if 0 // dump converted frame
    char buf[255];
    sprintf (buf, "/data/out_%d_%d.yuv", dstWidth, dstHeight);
    FILE *fp = fopen(buf, "ab");
    if(fp)
    {
        fwrite((void *)dstYUVbuf_va, 1, YUV_SIZE(dstWidth, dstHeight), fp);
        fclose(fp);
    }
#endif

#else
    uint8_t   *srcYUVbuf_va = NULL;
    uint8_t   *dstYUVbuf_va = NULL;
#if 0
    uint32_t  srcWStride = ROUND_16(srcWidth);
    uint32_t  srcHStride = ROUND_32(srcHeight);
    uint32_t  srcBufferSize = srcWStride * srcHStride * 3 >> 1;
    uint32_t  dstBufferSize = dstWidth * dstHeight * 3 >> 1;

    srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
    srcYUVbuf_va = (MHAL_UINT8 *)memalign(MEM_ALIGN_32, srcBufferSize);    // 32 byte alignment for MDP
#else
    uint32_t  srcWStride = 0;
    uint32_t  srcHStride = 0;
    uint32_t  srcBufferSize = 0;
    uint32_t  dstBufferSize = dstWidth * dstHeight * 3 >> 1;

    srcBufferSize = 0;
    srcYUVbuf_va = 0;

    uint32_t uvStrideMode = UV_STRIDE_16_8_8;
#endif

    uint8_t* srcYUVbufArray[3];
    unsigned int srcYUVbufSizeArray[3];
    uint32_t numPlanes = 2;

    if (mSrcColorFormat == OMX_COLOR_FormatVendorMTKYUV) {
        if (srcStride != 0) {
            srcWStride = srcStride;
        }
        else {
            srcWStride = ROUND_16(srcWidth);
        }
        if (srcSliceHeight != 0) {
            srcHStride = srcSliceHeight;
        }
        else {
            srcHStride = ROUND_32(srcHeight);
        }      
//      srcBufferSize = srcWStride * srcHStride * 3 >> 1;
        srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
        srcYUVbuf_va = (uint8_t*)memalign(MEM_ALIGN_32, srcBufferSize);

        srcYUVbufArray[0] = srcYUVbuf_va;      // Y
        srcYUVbufArray[1] = srcYUVbuf_va + srcWStride * srcHStride;  // C
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = srcWStride * srcHStride / 2;
        numPlanes = 2;
	uvStrideMode = UV_STRIDE_16_8_8;
    }
    else if (mSrcColorFormat == OMX_COLOR_FormatYUV420Planar) {
        if (srcStride != 0) {
            srcWStride = srcStride;
        }
        else {
            srcWStride = ROUND_16(srcWidth);
        }
        if (srcSliceHeight != 0) {
            srcHStride = srcSliceHeight;
        }
        else {
            srcHStride = ROUND_16(srcHeight);
        }
//      srcBufferSize = srcWStride * srcHStride * 3 >> 1;
        srcBufferSize = ROUND_32(YUV_SIZE(srcWStride, srcHStride));
        srcYUVbuf_va = (uint8_t*)memalign(MEM_ALIGN_32, srcBufferSize);

        srcYUVbufArray[0] = srcYUVbuf_va;
        srcYUVbufArray[1] = srcYUVbuf_va + (srcWStride * srcHStride);
        srcYUVbufArray[2] = srcYUVbufArray[1] + (srcWStride * srcHStride) /4;
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = (srcWStride * srcHStride) /4;
        srcYUVbufSizeArray[2] = (srcWStride * srcHStride) /4;
        numPlanes = 3;
	uvStrideMode = UV_STRIDE_16_8_8;
    }
    else if (mSrcColorFormat == OMX_MTK_COLOR_FormatYV12) {
        if (srcStride != 0) {
            srcWStride = srcStride;
        }
        else {
            srcWStride = ROUND_16(srcWidth);
        }
        if (srcSliceHeight != 0) {
            srcHStride = srcSliceHeight;
        }
        else {
            srcHStride = ROUND_16(srcHeight);
        }

        // for YV12 16,16,16 stride
        srcYUVbufSizeArray[0] = srcWStride * srcHStride;
        srcYUVbufSizeArray[1] = ROUND_16(srcWStride/2) * (srcHStride/2);
        srcYUVbufSizeArray[2] = ROUND_16(srcWStride/2) * (srcHStride/2);
        srcBufferSize = srcYUVbufSizeArray[0]+srcYUVbufSizeArray[1]+srcYUVbufSizeArray[2];
        srcYUVbuf_va = (uint8_t*)memalign(MEM_ALIGN_32, srcBufferSize);
        srcYUVbufArray[0] = srcYUVbuf_va;
        srcYUVbufArray[1] = srcYUVbuf_va + srcYUVbufSizeArray[0];
        srcYUVbufArray[2] = srcYUVbufArray[1] + srcYUVbufSizeArray[1];
        numPlanes = 3;
	uvStrideMode = UV_STRIDE_16_16_16;
    }
    else {
    	ALOGE ("ERROR not supported color format: mSrcColorFormat(0x%08X)", mSrcColorFormat);
    }

    dstYUVbuf_va = output;
    memcpy(srcYUVbuf_va, input, srcBufferSize);

    uint8_t* dstYUVbufArray[3];
    unsigned int dstYUVbufSizeArray[3];
    dstYUVbufArray[0] = dstYUVbuf_va;
    dstYUVbufArray[1] = dstYUVbuf_va + (dstWidth * dstHeight);
    dstYUVbufArray[2] = dstYUVbufArray[1] + (dstWidth * dstHeight) /4;
    dstYUVbufSizeArray[0] = dstWidth * dstHeight;
    dstYUVbufSizeArray[1] = (dstWidth * dstHeight) /4;
    dstYUVbufSizeArray[2] = (dstWidth * dstHeight) /4;

    DpBlitStream blitStream;
    DpColorFormat srcColorFormat = OmxColorToDpColor(mSrcColorFormat);
    DpColorFormat dstColorFormat = OmxColorToDpColor(mDstColorFormat);

    ALOGD("srcBufferSize(%d), mSrcColorFormat(%d), dstBufferSize(%d), mDstColorFormat(%d)", srcBufferSize, mSrcColorFormat, dstBufferSize, mDstColorFormat);

    DpRect srcRoi;
    srcRoi.x = 0;
    srcRoi.y = 0;
    srcRoi.w = srcWidth;
    srcRoi.h = srcHeight;

    int32_t crop_padding_left, crop_padding_top, crop_padding_right, crop_padding_bottom;
    if (!mMeta->findRect(
        kKeyCropPaddingRect,
        &crop_padding_left, &crop_padding_top, &crop_padding_right, &crop_padding_bottom)) {
        ALOGE("kKeyCropPaddingRect not found\n");
        //srcRoi.x = srcRoi.y = 0;
        //srcRoi.w = srcWidth - 1;
        //srcRoi.h = srcHeight - 1;
        srcRoi.x = 0;
        srcRoi.y = 0;
        srcRoi.w = srcWidth;
        srcRoi.h = srcHeight;
    }
#if 1
    else
    {
        srcRoi.x = crop_padding_left;
        srcRoi.y = crop_padding_top;
        srcRoi.w = crop_padding_right-crop_padding_left;
        srcRoi.h = crop_padding_bottom-crop_padding_top;
    }
#else
    sp<MetaData> inputFormat = mVideoSource->getFormat();
    const char *mime;
    if (inputFormat->findCString(kKeyMIMEType, &mime)) {
        ALOGD("%s %s",mime,MEDIA_MIMETYPE_VIDEO_VP9);

        if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_VP9)) {
            srcRoi.x = crop_padding_left;
            srcRoi.y = crop_padding_top;
            srcRoi.w = crop_padding_right-crop_padding_left;
            srcRoi.h = crop_padding_bottom-crop_padding_top;
ALOGD("crop_padding_left i %d r %d t %d b %d",
    crop_padding_left, crop_padding_right, crop_padding_top, crop_padding_bottom);
        }
    }
#endif

    ALOGD("@ srcRoi x %d y %d w %d h %d", srcRoi.x, srcRoi.y, srcRoi.w, srcRoi.h);


    blitStream.setSrcBuffer((void**)srcYUVbufArray, (unsigned int*)srcYUVbufSizeArray, numPlanes);
#if 0
    blitStream.setSrcConfig(srcWStride, srcHStride, srcColorFormat, eInterlace_None, &srcRoi);
#else
    unsigned int yPitch = ((srcWStride) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
    unsigned int uvPitch;

    switch (uvStrideMode) {
        case UV_STRIDE_16_16_16:
	    uvPitch = (ROUND_16(srcWStride/2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
	    break;

        case UV_STRIDE_16_8_8:
	    uvPitch = ((srcWStride/2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
	    break;

	default:  // use 16_8_8
	    uvPitch = ((srcWStride/2) * DP_COLOR_BITS_PER_PIXEL(srcColorFormat)) >> 3;
	    break;
    }

    //ALOGD ("yPitch(%d), uvPitch(%d), srcWStride=%d", yPitch, uvPitch, srcWStride);
    blitStream.setSrcConfig(srcWStride, srcHStride, yPitch, uvPitch, srcColorFormat, DP_PROFILE_BT601, eInterlace_None, &srcRoi);
#endif

    DpRect dstRoi;
    dstRoi.x = 0;
    dstRoi.y = 0;
    dstRoi.w = dstWidth;
    dstRoi.h = dstHeight;
    blitStream.setDstBuffer((void**)dstYUVbufArray, (unsigned int*)dstYUVbufSizeArray, 3);
#if 0
    blitStream.setDstConfig(dstWidth, dstHeight, dstColorFormat, eInterlace_None, &dstRoi);
#else
    yPitch = ((dstWidth) * DP_COLOR_BITS_PER_PIXEL(dstColorFormat)) >> 3;
    uvPitch = ((dstWidth/2) * DP_COLOR_BITS_PER_PIXEL(dstColorFormat)) >> 3;
    blitStream.setDstConfig(dstWidth, dstHeight, yPitch, uvPitch, dstColorFormat, DP_PROFILE_BT601, eInterlace_None, &dstRoi);
#endif
    blitStream.invalidate();

    if (srcYUVbuf_va) {
    	free (srcYUVbuf_va);
    	srcYUVbuf_va = NULL;
    }

#if 0 // dump converted frame
        char buf[255];
        sprintf (buf, "/storage/sdcard1/out_%d_%d.yuv", dstWidth, dstHeight);
        FILE *fp = fopen(buf, "ab");
        if(fp)
        {
            fwrite((void *)dstYUVbufArray[0], 1, YUV_SIZE(dstWidth, dstHeight), fp);
            fclose(fp);
        }
#endif
#endif
    ALOGD("-SpeedEffectSource::convertVideoFrame,");
}


status_t VideoSpeedEffect::SpeedEffectSource::read(MediaBuffer **buffer, const ReadOptions *options) {
	ALOGV("%s, SpeedEffectSource read before lock",mIsAudio?"audio":"video");

	Mutex::Autolock autoLock(mLock);
	ALOGV("%s,SpeedEffectSource read after lock",mIsAudio?"audio":"video");
	status_t err = OK;

	*buffer = NULL;
	
	if(!mStarted){
		//if not started or has stopped
		ALOGE("%s,SpeedEffectSource not started,read buffer failed!",mIsAudio?"audio":"video");
		return UNKNOWN_ERROR;
	}
	
	for(;;){
		ALOGV("%s,read buffer from source start",mIsAudio?"audio":"video");
		
		if(!mSourceBuffer) {
			err = msource->read(&mSourceBuffer,options);
			ALOGD("%s,read buffer from source done,err=%d",mIsAudio?"audio":"video",err);
			mFrameCount ++;
		}
		
		if(OK != err) {
			if (ERROR_END_OF_STREAM == err) {
				ALOGD ("%s,SpeedEffectSource Got EOS ",mIsAudio?"audio":"video");
				return err;	
				//mOrigLastTimestamp = mDurationUs;
			}
			if(INFO_FORMAT_CHANGED == err){
				ALOGI("%s,format change",mIsAudio?"audio":"video");
			}
			else{
				ALOGE ("%s,SpeedEffectSource read err=0x%08X", mIsAudio?"audio":"video",err);
				return err;	
			}
			
		}
		
		//ALOGV("%s,sourceBuffer =0x%x",mIsAudio?"audio":"video",mSourceBuffer);
		
		if(mSourceBuffer){	
			ALOGD("%s,sourceBuffer legnth =%d",mIsAudio?"audio":"video",mSourceBuffer->range_length());

			if(0 == mSourceBuffer->range_length()){
				ALOGW("%s,read buffer from source length == 0",mIsAudio?"audio":"video");
				mSourceBuffer->release();
				mSourceBuffer = NULL;
				continue;
			}
			
			if(mIsAudio) {
				//int64_t timeUs;
				//CHECK(sourceBuffer->meta_data()->findInt64(kKeyTime, &timeUs));
				MediaBuffer * outBuffer = NULL;
				bool needDropFrame = SpeedEffectAudioIfNeed(mSourceBuffer,outBuffer);
				
				if(needDropFrame){
					ALOGD("audio,read,need drop this frame,continue read next buffer");
					mSourceBuffer->release();
	                mSourceBuffer = NULL;
	                continue;
				}
				
				if(outBuffer != NULL){
					int32_t outBufLength = outBuffer->range_length();
					ALOGD("audio,read,speed effect return outBuffer,outBufLength=%d",outBufLength);
					if( 0 == outBufLength){
						outBuffer->release();
						outBuffer = NULL;
						continue;
					}
					*buffer = outBuffer;
					//ALOGD("audio,read return SpeedEffect Buffer ");
					return OK;
				}
				
				*buffer = mSourceBuffer; 
				mSourceBuffer = NULL;
				ALOGD("audio,read, return source Buffer ");
				return OK;
			}
			else {
				//int64_t timeUs;
				//CHECK(sourceBuffer->meta_data()->findInt64(kKeyTime, &timeUs));

				bool needDropFrame = SpeedEffectVideoIfNeed(mSourceBuffer);

				//for debug --start
				int64_t newTimestamp = 0;
				if(mPrintDebugInfo){	
					mSourceBuffer->meta_data()->findInt64(kKeyTime, &newTimestamp);
					if(mFrameCount == 1){
						mLastNewTimeUs = newTimestamp;
					}
					ALOGD("[VideoDebug][Info]frame = %lld,keep frame = %d, frame new timestamp = %lld, frame duration = %lld", \
						mFrameCount - 1,!needDropFrame,newTimestamp,newTimestamp - mLastNewTimeUs);

					mLastNewTimeUs = newTimestamp;
				}
				//for debug --end
				
				if(needDropFrame){
					//ALOGD("Video,need drop this frame,frame num =%lld",mFrameCount);
					mSourceBuffer->release();
	                mSourceBuffer = NULL;
	                continue;
				}
				
				//for debug --start
				//need keep this frame,check the timestamp
				if(mPrintDebugInfo){
					if(mKeepFrameNum == 0){
						mLastKeepFrameNewTimeUs = newTimestamp;
					}
					int64_t iFrameDurUs = 0;
					iFrameDurUs = newTimestamp - mLastKeepFrameNewTimeUs;
					mLastKeepFrameNewTimeUs = newTimestamp;
					ALOGD("[VideoDebug][KeepFrame]FrameDurUs=%lld",iFrameDurUs);

					if(mKeepFrameNum > 0){
						if((iFrameDurUs - mExpectedTargetFrameDurUs) > 1000) {
							ALOGW("[VideoDebug][KeepFrame][TimeStampWarning][longer] frame = %lld,keepFrameNum =%lld,frame new timestamp = %lld, frame duration = %lld",\
								mFrameCount - 1,mKeepFrameNum,newTimestamp,iFrameDurUs);
						}
						if((iFrameDurUs - mExpectedTargetFrameDurUs) < -1000){
							ALOGW("[VideoDebug][KeepFrame][TimeStampWarning][shorter] frame = %lld,keepFrameNum =%lld,frame new timestamp = %lld, frame duration = %lld",\
								mFrameCount - 1,mKeepFrameNum,newTimestamp,iFrameDurUs);
						}
					}
					mKeepFrameNum++;
				
				}

                int64_t keytime = -1;
                if(mSourceBuffer->meta_data()->findInt64(kKeyTime, &keytime)){
                    ALOGV("find buffer keytime %lld",keytime);
                    int64_t decodertime = -1;
                    if(mSourceBuffer->meta_data()->findInt64(kKeyDecodingTime, &decodertime)){
                       ALOGV("find decoder time %lld",decodertime);
                    }else{
                        mSourceBuffer->meta_data()->setInt64(kKeyDecodingTime, keytime);
                        ALOGD("no Keydecodingtime ,set it to keytime %lld",(long long)keytime);
                    }
                 }
				//for debug --end

				if(!mIsBitstream){
					MediaBuffer *output = NULL;
				 	if(mNeedFormatChange){
						ALOGV("change video format start");
				 		videoFormatChange(mSourceBuffer,mMeta,mTargetMeta);//change format of SourceBuffer to target meta
						ALOGV("change video format done");
					}else {
				 		mSourceBuffer->set_range(0, YUV_SIZE(mTargetWidth, mTargetHeight));
				 	}
				}

				*buffer = mSourceBuffer; 
				mSourceBuffer = NULL;
				ALOGD("Video,read return source Buffer ");
				return OK;
			}
		}
		else
			ALOGE("%s,read buffer from source return NULL",mIsAudio?"audio":"video");	
	}		
		
}
status_t VideoSpeedEffect::SpeedEffectSource::videoFormatChange(MediaBuffer*& buffer,sp<MetaData> srcMeta,sp<MetaData> dstMeta) {
	int32_t targetWidth = 0,targetHeight = 0;
	dstMeta->findInt32(kKeyWidth,&targetWidth);
	dstMeta->findInt32(kKeyHeight,&targetHeight);
	
	MediaBuffer* output = NULL;
	output = new MediaBuffer(ROUND_32(YUV_SIZE(targetWidth, targetHeight)));
    uint8_t* dst = (uint8_t*)(output)->data();
	
    //memcpy(dst, (*buffer)->data(), mTargetWidth*mTargetHeight*3>>1);
    //convertVideoFrame ((uint8_t*)(*buffer)->data(), ROUND_16(mWidth), ROUND_16(mHeight), dst, mTargetWidth, mTargetHeight);
    int32_t srcWidth,srcHeight,srcStride,srcSliceHeight;
    srcMeta->findInt32(kKeyWidth, &srcWidth);
    srcMeta->findInt32(kKeyHeight, &srcHeight);
    srcMeta->findInt32(kKeyStride, &srcStride);
	srcMeta->findInt32(kKeySliceHeight, &srcSliceHeight);
    ALOGI("videoFormatChange,decoder output,width(%d),height(%d),stride(%d),sliceHeight(%d)",srcWidth,srcHeight,srcStride,srcSliceHeight);
    convertVideoFrame ((uint8_t*)(buffer->data()), srcWidth, srcHeight, srcStride, srcSliceHeight, dst, targetWidth, targetHeight);
    (output)->set_range(0, YUV_SIZE(targetWidth, targetHeight));
    (output)->meta_data()->clear();
    int64_t timeUs = 0;
    CHECK(mSourceBuffer->meta_data()->findInt64(kKeyTime, &timeUs));
    (output)->meta_data()->setInt64(kKeyTime, timeUs);

	int32_t isSyncFrame;
    int32_t isCodecConfig;
    if (buffer->meta_data()->findInt32(kKeyIsSyncFrame, &isSyncFrame)) {
    	(output)->meta_data()->setInt32(kKeyIsSyncFrame, isSyncFrame);
    }
    if (buffer->meta_data()->findInt32(kKeyIsCodecConfig, &isCodecConfig)) {
    	(output)->meta_data()->setInt32(kKeyIsCodecConfig, isCodecConfig);
    }
	// release decoder output buffer
    buffer->release();
    buffer = NULL;

    // return converted buffer
    buffer = output;
	
	return OK;
}
bool VideoSpeedEffect::SpeedEffectSource::SpeedEffectAudioIfNeed(MediaBuffer*& buffer,MediaBuffer*& outbuffer){
	bool needDrop = false;
	int64_t timeUs,newTimeUs;
	CHECK(buffer->meta_data()->findInt64(kKeyTime, &timeUs));
	outbuffer = NULL;
	ALOGI("SpeedEffectAudioIfNeed,orig buffer timeStamp =%lld",timeUs);
	
	Mutex::Autolock l(mOwner->mEffectListLock); //video,audio track will access SpeedEffect in two thread
	for(List<SpeedEffectParam>::iterator i = mpSpeedEffectParamList->begin(); i != mpSpeedEffectParamList->end(); i++){
		if(timeUs >= (*i).mStartPos && timeUs <= (*i).mEndPos){

			ALOGI("audio,find speed effect item %lld-%lld",(*i).mStartPos,(*i).mEndPos);
			//int32_t slmSpeed = 1 ;
			//	
			int32_t isFirstSpeedEffectBuffer = 0;
			if(!((*i).mMeta->findInt32(kKeyFirstAudioSectBuf,&isFirstSpeedEffectBuffer)))
			{
				ALOGI("audio, first frame of  speed effect section");
				if(!mGetNewSpeed) {
					mLastSLMSpeed = mNowSLMSpeed;
					(*i).mMeta->findInt32(kKeySlowMotionSpeed,&mNowSLMSpeed);
					mGetNewSpeed = true;
				}
				ALOGI("audio, find speed effect item slm speed=%d",mNowSLMSpeed);
				
				
				//(*i).mMeta->setInt32(kKeyFirstSpeedEffectBuffer,1);
				//the first frame of file or of first section
				//shoul not drop
				needDrop= false;
			
				if(mIsFirstFrame){
					mIsFirstFrame = false;
					mLastOrigTimestamp = timeUs;
					mLastMaintainTimestamp = timeUs;
					mSectionBufferDurUs = 0; 

					mSectionFirstOrigTimestamp = timeUs;
					mSectinoFirstNewTimestamp = timeUs;
					ALOGI("audio,first Frame,mLastOrigTimestamp=mLastMaintainTimestamp =%lld",mLastOrigTimestamp);
				}
			
				(*i).mMeta->setInt64(kKeyFirstAudiotimeUs,timeUs);
				 
				if(mLastSLMSpeed != 1){
					ALOGI("audio,last slm spped =%d",mLastSLMSpeed);
					if(!mDequedLastBuffer){
						ALOGI("audio,Last buffer in time stretcher not dequeued");
						//need flush internal buffer in audio mtk stretcher
						if(!mAudioTimeStrecth){
							ALOGW("audio,last slm speed !=1,but no time stretcher,should not be here");
							//int32_t bufferSampleCount = (buffer->range_length())/(mBytesPerSample * mChannelCount);
							mAudioTimeStrecth = new AudioMTKTimeStretch(mTStrIntBufSampeCount);
							//mAudioTimeStrecth = new AudioMTKTimeStretch(mLastSLMSpeed,mChannelCount,mSampleRate,bufferSampleCount);
							mAudioTimeStrecth->init(mSampleRate,mChannelCount,mLastSLMSpeed*100);
						}
						int32_t speed = mLastSLMSpeed *100;
						mAudioTimeStrecth->setParameters(&speed);

						MediaBuffer* tempBuffer = new MediaBuffer(mTStrIntBufSampeCount *mChannelCount *mBytesPerSample);
						memset(tempBuffer->data(),0,tempBuffer->range_length());
						
						outbuffer = new MediaBuffer((tempBuffer->range_length()) * mLastSLMSpeed);
						
						int32_t InputSampleCount = tempBuffer->range_length();
						int32_t OutputSampleCount = outbuffer->range_length();
						
						mAudioTimeStrecth->process((short*)(tempBuffer->data()),(short*)(outbuffer->data()),\
															&InputSampleCount,&OutputSampleCount);
						ALOGI("audio,flush last internal buffer in stretcher,InputSampleCount(%d),OutputSampleCount(%d)",\
															InputSampleCount,OutputSampleCount);
						//check the result
						//InputSampleCount,the number of intput bytes that is NOT consumed by timestretch.
						//OutputSampleCount,the number of	generated output bytes.
					
						if(InputSampleCount != 0){
							ALOGE("audio,Time Strecher not flush clear,InputSampleCount=%d",InputSampleCount);
						}
						if(OutputSampleCount != (tempBuffer->range_length()) * mLastSLMSpeed){
							ALOGE("audio,Time Strecher not flush clear,OutputSampleCount=%d",OutputSampleCount);
						}
						mAudioTimeStrecth->reset();
						tempBuffer->release();
						tempBuffer = NULL;

						outbuffer->meta_data()->setInt64(kKeyTime,mSectinoFirstNewTimestamp + mSectionBufferDurUs);
						mSectionBufferDurUs += (int64_t)OutputSampleCount * 1000000 /(mChannelCount*mBytesPerSample *mSampleRate);

						mDequedLastBuffer = true;
						needDrop = false;
						return needDrop;
					}
					else{
						int64_t newTimeUs = mSectinoFirstNewTimestamp + mSectionBufferDurUs;
						mSectionFirstOrigTimestamp = timeUs;
						mSectinoFirstNewTimestamp = newTimeUs;
						ALOGI("audio,mSectionFirstOrigTimestamp=%lld,mSectinoFirstNewTimestamp=%lld",mSectionFirstOrigTimestamp,mSectinoFirstNewTimestamp);

					}
				}
				else if(mLastSLMSpeed == 1){
					int64_t newTimeUs = mLastMaintainTimestamp + (timeUs - mLastOrigTimestamp);
					mSectionFirstOrigTimestamp = timeUs;
					mSectinoFirstNewTimestamp = newTimeUs;
					ALOGI("audio, last speed =1,mLastOrigTimestamp(%lld),mLastMaintainTimestamp(%lld)",mLastOrigTimestamp,mLastMaintainTimestamp);
					ALOGI("audio, last speed =1,mSectionFirstOrigTimestamp=%lld,mSectinoFirstNewTimestamp=%lld",mSectionFirstOrigTimestamp,mSectinoFirstNewTimestamp);
				}
			
				if(mNowSLMSpeed == 1){
					buffer->meta_data()->setInt64(kKeyTime, mSectinoFirstNewTimestamp);
					ALOGI("audio,first buffer of section,speed=1,mSectinoFirstNewTimestamp(%lld)",mSectinoFirstNewTimestamp);
					//return false;
				}else{
				
					int32_t bufferSampleCount = (buffer->range_length())/(mBytesPerSample * mChannelCount);
					ALOGI("audio,first buffer of section,mNowSLMSpeed=%d,bufferSampleCount=%d",mNowSLMSpeed,bufferSampleCount);
					if(!mAudioTimeStrecth){
							ALOGW("audio,new speed effect section");
							//int32_t bufferSampleCount = (buffer->range_length())/(mBytesPerSample * mChannelCount);
							mAudioTimeStrecth = new AudioMTKTimeStretch(bufferSampleCount);
							//mAudioTimeStrecth = new AudioMTKTimeStretch(mLastSLMSpeed,mChannelCount,mSampleRate,bufferSampleCount);
							mAudioTimeStrecth->init(mSampleRate,mChannelCount,mNowSLMSpeed*100);
					}
					int32_t speed = mNowSLMSpeed *100;
					mAudioTimeStrecth->setParameters(&speed);
					
					outbuffer = new MediaBuffer((buffer->range_length()) * mNowSLMSpeed);
					int32_t InputSampleCount = buffer->range_length();
					
					int32_t OutputSampleCount = outbuffer->range_length();
					ALOGI("audio,first buffer of section,before process,InputSampleCount(%d),OutputSampleCount(%d)",\
																											InputSampleCount,OutputSampleCount);
					mAudioTimeStrecth->process((short*)buffer->data(),(short*)outbuffer->data(),&InputSampleCount,&OutputSampleCount);
					ALOGI("audio,first buffer of section,after process,InputSampleCount(%d),OutputSampleCount(%d)",\
																					InputSampleCount,OutputSampleCount);

					if(InputSampleCount == 0){
						ALOGI("audio,source buffer has cosumued complete");
						buffer->release();
						buffer = NULL;
					}else{
						buffer->set_range((buffer->range_length())-InputSampleCount,InputSampleCount);
					}
					if(OutputSampleCount == 0){
						outbuffer->set_range(0,0);
						//return false;
					}else{
						outbuffer->set_range(0,OutputSampleCount);
					}
					outbuffer->meta_data()->setInt64(kKeyTime, mSectinoFirstNewTimestamp);
					mSectionBufferDurUs = (int64_t)outbuffer->range_length() * 1000000 /(mChannelCount*mBytesPerSample *mSampleRate);
					ALOGI("audio,outbuffer->range_length()=%d,mChannelCount=%d,mBytesPerSample=%d,mSampleRate=%d",outbuffer->range_length(),mChannelCount,mBytesPerSample,mSampleRate);
					ALOGI("audio,First buffer of section,mSectionBufferDurUs = %lld",mSectionBufferDurUs);
				}
				needDrop= false;
				mLastOrigTimestamp = mSectionFirstOrigTimestamp;
				mLastMaintainTimestamp = mSectinoFirstNewTimestamp;
						
				//for A/V sync
				//get kKeyFirstVideotimeUs time, cal A,V timeoffset
				//change mSectinoFirstNewTimestamp
										
				mLastOrigTimestamp = timeUs;
				mLastMaintainTimestamp = mSectinoFirstNewTimestamp;
				//mLastDelta = 0;
				//mLastSLMSpeed = slmSpeed;
				(*i).mMeta->setInt32(kKeyFirstAudioSectBuf,1);
				//for next section
				mGetNewSpeed = false;
				return needDrop;
			}
				
			else {
				if(mNowSLMSpeed == 1){
					newTimeUs = mSectinoFirstNewTimestamp + (timeUs - mSectionFirstOrigTimestamp);
					buffer->meta_data()->setInt64(kKeyTime,newTimeUs);
					ALOGI("audio, now speed =1,buffer new timestamp=%lld",newTimeUs);
				}else{
				
					int32_t bufferSampleCount = (buffer->range_length())/(mBytesPerSample * mChannelCount);
					ALOGI("audio,mNowSLMSpeed=%d,bufferSampleCount=%d",mNowSLMSpeed,bufferSampleCount);

					if(!mAudioTimeStrecth){
							ALOGW("audio,no timestrecth created for this section");
							//int32_t bufferSampleCount = (buffer->range_length())/(mBytesPerSample * mChannelCount);
							mAudioTimeStrecth = new AudioMTKTimeStretch(bufferSampleCount);
							//mAudioTimeStrecth = new AudioMTKTimeStretch(mLastSLMSpeed,mChannelCount,mSampleRate,bufferSampleCount);
							mAudioTimeStrecth->init(mSampleRate,mChannelCount,mNowSLMSpeed*100);
					}
					int32_t speed = mNowSLMSpeed * 100;
					mAudioTimeStrecth->setParameters(&speed);
					
					outbuffer = new MediaBuffer((buffer->range_length()) * mNowSLMSpeed);
					int32_t InputSampleCount = buffer->range_length();
					int32_t OutputSampleCount = outbuffer->range_length();
					ALOGD("audio,before timestrecth,InputSampleCount(%d),OutputSampleCount(%d)",\
																InputSampleCount,OutputSampleCount);
					mAudioTimeStrecth->process((short*)buffer->data(),(short*)outbuffer->data(),&InputSampleCount,&OutputSampleCount);
					ALOGD("audio,after timestrecth,InputSampleCount(%d),OutputSampleCount(%d)",\
																InputSampleCount,OutputSampleCount);
					if(InputSampleCount == 0){
						ALOGV("audio,source buffer has cosumued complete");
						buffer->release();
						buffer = NULL;
					}else{
						buffer->set_range((buffer->range_length())-InputSampleCount,InputSampleCount);
					}
					if(OutputSampleCount == 0){
						outbuffer->set_range(0,0);
						//return false;
					}else{
						outbuffer->set_range(0,OutputSampleCount);
					}
					newTimeUs = mSectinoFirstNewTimestamp + mSectionBufferDurUs;	
					outbuffer->meta_data()->setInt64(kKeyTime, newTimeUs );
					mSectionBufferDurUs += (int64_t)outbuffer->range_length() * 1000000 /(mChannelCount*mBytesPerSample *mSampleRate);
					ALOGI("audio,now speed(%d),buffer timestamp=%lld,mSectionBufferDurUs=%lld",mNowSLMSpeed,newTimeUs,mSectionBufferDurUs);
				}
				needDrop = false;
								
				mLastOrigTimestamp = timeUs;
				//mLastSLMSpeed = slmSpeed;
				if(!needDrop) {
					mLastMaintainTimestamp = newTimeUs;
					//mLastDelta = 0;
				}
				//else {
					//mLastDelta = newTimeUs - mLastMaintainTimestamp;
				//}
				ALOGV("audio,SpeedEffectIfNeed,mLastMaintainTimestamp =%lld,mLastDelta=%lld",mLastMaintainTimestamp,mLastDelta);
				return needDrop;	
			}		
		}
	}
	ALOGE("Can not find speed effect param for this buffer");
	return false;

}

bool VideoSpeedEffect::SpeedEffectSource::SpeedEffectVideoIfNeed(MediaBuffer*& buffer){
	bool needDrop = false;
	int64_t timeUs,newTimeUs;
	CHECK(buffer->meta_data()->findInt64(kKeyTime, &timeUs));
	
	ALOGI("SpeedEffectVideoIfNeed,orig buffer timeStamp =%lld",timeUs);
	Mutex::Autolock l(mOwner->mEffectListLock);
	for(List<SpeedEffectParam>::iterator i = mpSpeedEffectParamList->begin(); i != mpSpeedEffectParamList->end(); i++){
		if(timeUs >= (*i).mStartPos && timeUs <= (*i).mEndPos){

			ALOGI("video,find speed effect item %lld-%lld",(*i).mStartPos,(*i).mEndPos);
			//int32_t slmSpeed = 1 ;
			//	
			int32_t isFirstSpeedEffectBuffer = 0;
			if(!((*i).mMeta->findInt32(kKeyFirstVideoSectBuf,&isFirstSpeedEffectBuffer)))
			{
				ALOGI("video,SpeedEffectIfNeed,the first frame of this speed effect section");
				if(!mGetNewSpeed) {
					mLastSLMSpeed = mNowSLMSpeed;
					(*i).mMeta->findInt32(kKeySlowMotionSpeed,&mNowSLMSpeed);
					mGetNewSpeed = true;
				}
				ALOGI("video,find speed effect item slm speed=%d",mNowSLMSpeed);
				
				
				//(*i).mMeta->setInt32(kKeyFirstSpeedEffectBuffer,1);
				//the first frame of file or of first section
				//shoul not drop
				needDrop= false;
				
				if(mIsFirstFrame){
					mIsFirstFrame = false;
					mLastOrigTimestamp = timeUs;
					mLastMaintainTimestamp = timeUs;

					mSectionFirstOrigTimestamp = timeUs;
					mSectinoFirstNewTimestamp = timeUs;
					ALOGI("video,SpeedEffectIfNeed, is first Frame,mLastOrigTimestamp=mLastMaintainTimestamp =%lld",mLastOrigTimestamp);
				}
				
				(*i).mMeta->setInt64(kKeyFirstVideotimeUs,timeUs);
				ALOGI("video,SpeedEffectIfNeed,first orig video timestamp= (%lld) of this section,mLastOrigTimestamp=%lld ",(long long)timeUs,(long long)mLastOrigTimestamp);
				mSectionFirstOrigTimestamp = timeUs;
				mLastDelta += timeUs - mLastOrigTimestamp;
				mSectinoFirstNewTimestamp = mLastMaintainTimestamp +  mLastDelta;
				buffer->meta_data()->setInt64(kKeyTime, mSectinoFirstNewTimestamp);
				ALOGI("video,SpeedEffectIfNeed,first new video timestamp= (%lld) of this section ",(long long)mSectinoFirstNewTimestamp);
				//for A/V sync
				//get kKeyFirstAudiotimeUs time, cal A,V timeoffset
				//change mSectinoFirstNewTimestamp

				//create frame drop table
				float dropRate = 1;
				(*i).mMeta->findFloat(kKeyVideoDropRate,&dropRate);
				if(dropRate > 1){
					int32_t targetfps = 0;
					(*i).mMeta->findInt32(kKeyFrameRate,&targetfps);
					int64_t stepInterval = (int64_t)1000000ll/targetfps;
					if(!mIsBitstream){
						if(!mVideoFpsControlTable){
							mVideoFpsControlTable = VideoFpsControlTable<int64_t>::create(mSectinoFirstNewTimestamp,targetfps,stepInterval);
						}else{
							((VideoFpsControlTable<int64_t> *)mVideoFpsControlTable)->update(mSectinoFirstNewTimestamp,targetfps,stepInterval);
						}
					}
					else if(mIsBitstream){
						(*i).mMeta->setInt64(kKeySectionFrameNumb,0);
						if(!mVideoFpsControlTable){
							mVideoFpsControlTable = VideoFpsControlTable<float>::create(0,targetfps,dropRate);
						}else{
							((VideoFpsControlTable<float> *)mVideoFpsControlTable)->update(0,targetfps,dropRate);
						}
					}
					
				}
				
				mLastOrigTimestamp = timeUs;
				mLastMaintainTimestamp = mSectinoFirstNewTimestamp;
				mLastDelta = 0;
				//mLastSLMSpeed = slmSpeed;
				(*i).mMeta->setInt32(kKeyFirstVideoSectBuf,1);
				//for next section
				mGetNewSpeed = false;
				return needDrop;
			}
			else {
				
				newTimeUs = mSectinoFirstNewTimestamp + (timeUs - mSectionFirstOrigTimestamp)* mNowSLMSpeed;
				buffer->meta_data()->setInt64(kKeyTime, newTimeUs);
				ALOGI("video,SpeedEffectIfNeed,new tiemstamp =%lld of this buffer", newTimeUs);
				//drop frame if needed
				float dropRate = 1;
				(*i).mMeta->findFloat(kKeyVideoDropRate,&dropRate);
				ALOGD("video,SpeedEffectIfNeed,droprate=%f",dropRate);
				if(dropRate > 1){
					bool hasFrame = false;
					if(!mIsBitstream){
						VideoFpsControlTable<int64_t>::FpsControlEntry* pEntryItem = NULL;
						pEntryItem = ((VideoFpsControlTable<int64_t> *)mVideoFpsControlTable)->findEntry(newTimeUs);
						hasFrame = pEntryItem->mHasFrame;
						if(hasFrame){
							//if already has one frame kept in this Entry
							//need drop other frames in this Entry
							needDrop = true;
						}else{
							//if no one frame kept in this Entry
							//need maintain this frame
							needDrop = false;
							pEntryItem->mHasFrame = true;
						}
							
					}else if(mIsBitstream){
						//update section frame number
						int64_t sectionFrameNumb = 0;
						(*i).mMeta->findInt64(kKeySectionFrameNumb,&sectionFrameNumb);
						sectionFrameNumb++;
						(*i).mMeta->setInt64(kKeySectionFrameNumb,sectionFrameNumb);
						
						VideoFpsControlTable<float>::FpsControlEntry* pEntryItem = NULL;
						pEntryItem = ((VideoFpsControlTable<float> *)mVideoFpsControlTable)->findEntry(sectionFrameNumb);
						hasFrame = pEntryItem->mHasFrame;

						//check whether is reference frame
						bool nonReferenceFrame = false;
						nonReferenceFrame = isNonReferenceFrame(buffer,mMeta);

						if(hasFrame){
							//if already has one fram in this entry
							//drop non-reference frame
							//keep reference frame
							needDrop = nonReferenceFrame? true:false;
						}else{
							//if no frame chosen for this entry
							//keep the reference frame if has one
							//or keep the last non-reference frame if no reference p frame
							if(!nonReferenceFrame){
								needDrop = false;
								pEntryItem->mHasFrame = true;
							}else{
								float intervalFromStop = pEntryItem->mEntryStopPoint - (float)sectionFrameNumb;
								//drop the non-reference frame
								//except the last point of the entry
								if(intervalFromStop >= 0.0 && intervalFromStop < 1.0){
									//is the last point of the entry
									needDrop = false;
									pEntryItem->mHasFrame = true;
								}else{
									needDrop = true;
								}
							}
						}
	
					}
	
				}else {
					needDrop = false;
				}
				
				mLastOrigTimestamp = timeUs;
				//mLastSLMSpeed = slmSpeed;
				if(!needDrop) {
					mLastMaintainTimestamp = newTimeUs;
					mLastDelta = 0;
				}else {
					mLastDelta = newTimeUs - mLastMaintainTimestamp;
				}
				ALOGD("video,SpeedEffectIfNeed,drop = %d,mLastMaintainTimestamp =%lld,mLastDelta=%lld",needDrop,mLastMaintainTimestamp,mLastDelta);
				return needDrop;			
				
			}	
		}
	}
	ALOGE("Can not find speed effect param for this buffer");
	return false;

}

} //namespace mtkspeedeffect
