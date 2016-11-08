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

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <utils/Log.h>
#include <fcntl.h>
#include "main.h"
#include <cutils/properties.h>
#include <linux/android_alarm.h>
#include <sys/reboot.h>
#include <errno.h>
#include <time.h>
#include <stdarg.h>

#define CHARGER_OVER_VOLTAGE 7000
#define CHARGER_VOLTAGE_PATH "/sys/class/power_supply/battery/ChargerVoltage"
#define WIRELESS_CHARGING "/sys/class/power_supply/wireless/online"
#define CHARGER_BATTNOTFY_PATH "/sys/devices/platform/mt-battery/BatteryNotify"

int showLowBattLogo = false;
int inExiting = false;

static int write_to_file(const char* path, const char* buf, int size)
{
    if (!path) {
        KPOC_LOGE("null path to write");
        return 0;
    }
#ifdef VERBOSE_OUTPUT
    KPOC_LOGI("%s: path: %s, buf: %s, size: %d\n",__FUNCTION__, path ,buf, size);
#endif

    int fd = open(path, O_RDWR);
    if (fd == -1) {
        KPOC_LOGE("Could not open '%s'\n", path);
        return 0;
    }

    int count = write(fd, buf, size); 
    if (count != size) {
        KPOC_LOGE("write file (%s) fail, count: %d\n", path, count);
        close(fd);
        return 0;
    }

    close(fd);
    return count;
}


void set_int_value(const char * path, const int value)
{
    char buf[32];
    sprintf(buf, "%d", value);
#ifdef VERBOSE_OUTPUT
    KPOC_LOGI("%s: %s, %s \n",__FUNCTION__, path ,buf);
#endif
    write_to_file(path, buf, strlen(buf));
}

/*   return value:
 *         0, error or read nothing
 *        !0, read counts
 */
static int read_from_file(const char* path, char* buf, int size)
{
    if (!path) {	
        return 0;
    }

    int fd = open(path, O_RDONLY);
    if (fd == -1) {
        return 0;
    }

    int count = read(fd, buf, size); 
    if (count > 0) {
        count = (count < size) ? count : size - 1;
        while (count > 0 && buf[count-1] == '\n') count--;
        buf[count] = '\0';
    } else {
        buf[0] = '\0';
    }

    close(fd);
    return count;
}

int get_int_value(const char * path)
{
    int size = 32;
    char buf[size];
    if(!read_from_file(path, buf, size))
        return 0;
    return atoi(buf);
}

/*
 * return value:
 *     1: abnormal status
 *     0: normal status
 */
int get_battnotify_status()
{
	int battStatus = get_int_value(CHARGER_BATTNOTFY_PATH);
	KPOC_LOGI("charger battStatus: %d\n",battStatus);
	if (battStatus != 0) {
		return 1;
	}
	return 0;
}

int is_wireless_charging(){
    int wireless_charging = get_int_value(WIRELESS_CHARGING);
    KPOC_LOGI("wireless_charging: %d\n", wireless_charging);
    return wireless_charging;
}

bool time_exceed(struct timeval start, int duration_msec)
{
	struct timeval now;
	gettimeofday(&now, NULL);

	if((now.tv_sec - start.tv_sec)*1000000 + now.tv_usec - start.tv_usec > duration_msec*1000)
		return true;
	else
		return false;
}
