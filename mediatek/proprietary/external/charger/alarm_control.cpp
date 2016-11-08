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
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 
#include <linux/rtc.h>
#include "main.h"

static pthread_attr_t attr;
static pthread_t alarm_thread;
static int fd = -1, fd_alarm = -1;

#ifndef ANDROID_ALARM_GET_POWER_ON
#define ANDROID_ALARM_GET_POWER_ON          _IOR('a', 7, struct rtc_wkalrm)
#endif

static int rtc_trigger_by_poweron_alarm_boot(void)
{
    int result = 0;
    struct rtc_wkalrm alrm;

    memset(&alrm, 0, sizeof(struct rtc_wkalrm));
    //***** alarm device operation
    result = ioctl(fd_alarm, ANDROID_ALARM_GET_POWER_ON, &alrm);
    KPOC_LOGI("alarm fd = %d, result = %d\n", fd_alarm, result);
    if(result < 0) {
        KPOC_LOGI("Unable to get ANDROID_ALARM_GET_POWER_ON: %s\n", strerror(errno));
        alrm.pending = 0;
    }
    //***** alarm device operation done
    KPOC_LOGI("alarm result:0x%x, alrm.enabled:%d, alrm.pending:%d\n", result, alrm.enabled, alrm.pending);

    if (1 == alrm.pending && 0 == alrm.enabled)
        return true;

    return false;
}

static void* alarm_thread_routine(void *arg)
{
	int result, alrmboot = 0;
	int wait = 0, opened = 0;
	struct rtc_wkalrm alrm;

	unsigned long data;

	KPOC_LOGI("alarm thread start\n");
	fd_alarm = open(ALARM_DEV_PATH, O_RDWR);
	KPOC_LOGI("alarm fd:%d\n",fd_alarm);
	
	if (fd < 0) {
		fd = open(RTC_DEV_PATH, O_RDWR);
		if (fd < 0) {
			KPOC_LOGI("Cannot open /dev/rtc0!!\n");
			exit_charger(EXIT_ERROR_SHUTDOWN);
		}
	}
	KPOC_LOGI("rtc0 fd:%d\n",fd);

	while(fd >= 0) {
		result = read(fd, &data, sizeof(unsigned long));
		KPOC_LOGI("rtc0 fd = %d, result = %d, data = %ld\n",fd, result, data);

		if (!(data & RTC_AF)) {
			KPOC_LOGI("no RTC_AF flag");
		} else {
			alrmboot = rtc_trigger_by_poweron_alarm_boot();
			if (!alrmboot) {
				KPOC_LOGI("rtc trigger by no alarm\n");
				//exit_charger(EXIT_ERROR_SHUTDOWN);
			} else {
				close(fd);
				fd = -1;
				exit_charger(EXIT_ALARM_BOOT);
			}
		}
	} // end of while
	return NULL;
}


void alarm_control()
{
	int ret = 0;
	KPOC_LOGI("alarm_control\n");
	pthread_attr_init(&attr);
	
	ret = pthread_create(&alarm_thread, &attr, alarm_thread_routine, NULL);
	if (ret != 0) {
		KPOC_LOGI("create alarm pthread failed.\n");
		exit_charger(EXIT_ERROR_SHUTDOWN);
	}
}
