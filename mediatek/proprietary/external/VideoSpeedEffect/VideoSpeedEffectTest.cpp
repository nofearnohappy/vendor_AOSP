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
 *  VideoSpeedEffectTest.cpp
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   Video SpeedEffect API client sample code
 *
 * Author:
 * -------
 *   Haizhen.Wang(mtk80691)
 *
 ****************************************************************************/

#include "VideoSpeedEffect.h"
#include <fcntl.h>
#include <linux/stat.h>
#include <utils/String8.h>



using namespace android;


class Listener : public VideoSpeedEffectListener
{
public:
	Listener(){}
	virtual ~Listener(){}
	virtual void notify(int msg, int ext1, int ext2){
		printf("Listner receive one notify: msg(%d),ext1(%d),ext2(%d) \n",msg,ext1,ext2);
		if(msg == SPEEDEFFECT_COMPLETE){
			printf("receive completed \n");
		}
	}
	
};


int main(int argc, char **argv) {

	androidSetThreadPriority(0, ANDROID_PRIORITY_AUDIO);
	int srcfd = open("/sdcard/src.3gp", O_LARGEFILE | O_RDONLY, S_IRUSR);
	int dstfd = open("/sdcard/dst.3gp",O_CREAT | O_LARGEFILE | O_TRUNC | O_RDWR, S_IRUSR | S_IWUSR);	
	printf("create VideoSpeedEffect \n");
	sp<VideoSpeedEffect> vse = new VideoSpeedEffect();
	printf("create VideoSpeedEffect done \n");
	const char * param = "slow-motion-speed=4 \n";
	//String8* effectParam = new String8(param);
	String8 effectParam;
	effectParam = param;
	vse->addSpeedEffectParams(5000000,15000000,effectParam);
	printf("addSpeedEffectParams done %s \n",effectParam.string());

	int64_t srcLength = 0;
	
	if (srcfd >= 0) {
        srcLength = lseek64(srcfd, 0, SEEK_END);
		printf("srcLength =%lld \n",srcLength);

	}
	else
		printf("can not open sdcard/src.3gp \n");
	
	sp<VideoSpeedEffectListener> listenr = new Listener();
	
	vse->setListener(listenr);
	vse->startSaveSpeedEffect(srcfd,0,srcLength,dstfd);
	printf("start Save Speed effect done\n");
	printf("start sleep \n");
	sleep(50); //50s 
	
    printf("end sleep \n");
	printf("stop spped effect start\n");
	vse->stopSaveSpeedEffect();
	printf("stop spped effect done\n");
    return 0;
}
