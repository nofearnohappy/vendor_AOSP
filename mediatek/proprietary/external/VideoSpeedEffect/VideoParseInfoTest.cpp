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

/*****************************************************************************
 *
 * Filename:
 * ---------
 *  VideoParseInfoTest.cpp
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   Parse Video info
 *
 * Author:
 * -------
 *   Haizhen.Wang(mtk80691)
 *
 ****************************************************************************/

//#include "VideoSpeedEffect.h"
#define LOG_TAG "VideoParseInfoTest"
#include <utils/Log.h>

#include <fcntl.h>
#include <linux/stat.h>
#include <utils/String8.h>


//#include <media/mediarecorder.h>
#include <media/stagefright/MediaDefs.h>
//#include <media/mediaplayer.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>

#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>


#include <media/stagefright/MediaBuffer.h>

#include <binder/ProcessState.h>

using namespace android;


bool isNonRefFrame(MediaBuffer* buffer,sp<MetaData> bufferMeta){
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


int main(int argc, char **argv) {

	if(argc != 2)
	{
		printf("Parse Video Info Usage:\n");
		printf("VideoParseInfoTest filepath \n");
		return 0;
	}

	sp<ProcessState> proc(ProcessState::self());  
	ProcessState::self()->startThreadPool(); 

	char * filepath = argv[1];
	
	DataSource::RegisterDefaultSniffers();
	
	//get src file format
	sp<DataSource> dataSource = new FileSource(filepath);

	status_t err = dataSource->initCheck();

	if (err != OK) {
	   ALOGE("startSaveSpeedEffect,FileSource create fail !");
	   return err;
	}

	//mFileSource = dataSource;
	ALOGI("startSaveSpeedEffect,dataSource =0x%x",dataSource.get());
	sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);

    if (extractor == NULL) {
		ALOGW("No extractor found!");
        return UNKNOWN_ERROR;
    }
	int32_t srcfps = 0;
	sp<MetaData> meta = extractor->getMetaData();
	
	for (size_t i = 0; i < extractor->countTracks(); ++i) {
			sp<MetaData> trackMeta = extractor->getTrackMetaData(i);
			
			const char *_mime;
			trackMeta->findCString(kKeyMIMEType, &_mime);
			String8 mime = String8(_mime);
			ALOGI("track[%d] mime =%s",i,mime.string());
	
		   if (!strncasecmp(mime.string(), "video/", 6)) {
		   	
			   	if(!trackMeta->findInt32(kKeyFrameRate, &srcfps) || srcfps <= 0){
			
					ALOGI("no frame rate info in src file");
					srcfps = 120;
				}
							
				sp<MediaSource> videoSource = extractor->getTrack(i);
				
				sp<MetaData> params = new MetaData();
				params->setInt32(kKeyWantsNALFragments, false);
				//let parser return buffer in frame
				err = videoSource->start(params.get());
				
				if (err != OK) {
					ALOGE("failed to start video source");
					videoSource.clear();
					return err;
				}

				int64_t mFrameCount = 0;
				MediaBuffer *mSourceBuffer = NULL;
				int64_t mLastTimeUs = 0;
				bool mIsFirstBuffer = true;
				int64_t frameDuration = 0;
				int64_t expectedFrameDurUs = 1000000ll/srcfps;
				ALOGI("srcfps =%d,expectedFrameDurUs =%lld",srcfps,expectedFrameDurUs);

				int32_t nonRefPFreq = 0;
				uint32_t nonRefP_numerator = 0;
				uint32_t nonRefP_denominator = 0;
				if(trackMeta->findInt32(kKeyNonRefPFreq,&nonRefPFreq)) {
					nonRefP_numerator = (nonRefPFreq >> 16) & 0x0000FFFF;
					nonRefP_denominator = nonRefPFreq & 0x0000FFFF;
		
					ALOGI("non-reference P info:%d/%d",\
						nonRefP_numerator,nonRefP_denominator);		
				}

				int32_t nonReferCount = 0;
				int64_t lastReferFrameNumb = 0;
				//parse all the bitstream
				for(;;){
					err = videoSource->read(&mSourceBuffer);
					if(OK != err) {
						if (ERROR_END_OF_STREAM == err) {
							ALOGD ("eos");
							printf("eos");
							break;
						}
						else{
							ALOGE ("err=0x%08X", err);
							videoSource->stop();
							printf("err=%d",err);
							return err; 
						}
					}

					//get timestamp
					int64_t timeUs = 0;
					mSourceBuffer->meta_data()->findInt64(kKeyTime, &timeUs);

					if(mIsFirstBuffer){
						mLastTimeUs = timeUs;
						//mIsFirstBuffer = false;
					}
					//get non-reference info
					bool nonReferenceP = isNonRefFrame(mSourceBuffer,trackMeta);

					
					frameDuration = timeUs- mLastTimeUs;

					ALOGD("[FileInfo] frame = %lld , reference = %d , timestampUs = %lld , last frame durationUs = %lld", \
						mFrameCount,!nonReferenceP,timeUs,frameDuration);
						
					if(mFrameCount > 0){

						if((frameDuration - expectedFrameDurUs) > 100)
{
							ALOGD("[TimeStampWarning][longer] frame = %lld , last durationUs = %lld ,timestampUs = %lld", \
							mFrameCount,frameDuration,timeUs);
							
						}
						if((frameDuration - expectedFrameDurUs) < -100)
{
							ALOGD("[TimeStampWarning][shorter] frame = %lld , last durationUs = %lld ,timestampUs = %lld", \
							mFrameCount,frameDuration,timeUs);
							
						}
					}
					if(!nonReferenceP){
						if(!mIsFirstBuffer){
							if(nonReferCount < nonRefP_numerator){
								ALOGW("[NonReferInfoWarning][littler] frame = %lld, non reference frame count between reference frame =%d",\
									mFrameCount,nonReferCount);
							}
							if(nonReferCount > nonRefP_numerator){
								ALOGW("[NonReferInfoWarning][bigger] frame = %lld, non reference frame count between reference frame =%d",\
																	mFrameCount,nonReferCount);
							}
						}
						nonReferCount = 0;
					}

					if(nonReferenceP){
						nonReferCount++;
					}

					if(mIsFirstBuffer)
						mIsFirstBuffer = false;
					
					mLastTimeUs = timeUs;		
					mFrameCount ++;
					
					mSourceBuffer->release();
	                mSourceBuffer = NULL;
					usleep(1000);
		
			}//for()
			videoSource->stop();
		  }//if video
		  
	}//for track
	printf("complete");
	
    return 0;
}
