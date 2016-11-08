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
 
#include "main.h"
#include <suspend/autosuspend.h>

#define UEVENT_BATTERY_CHANGE	"power_supply/batt"
#define BATT_CAPACITY_PATH "/sys/class/power_supply/battery/capacity"
#define BATT_VOLTAGE_PATH  "/sys/class/power_supply/battery/batt_vol"
//#define BATT_VOLTAGE_PATH  "/sys/class/power_supply/battery/voltage_avg"
#define CHARGER_VOLTAGE_PATH  "/sys/devices/platform/battery/ADC_Charger_Voltage"
#define USB_ONLINE_PATH "/sys/class/power_supply/usb/online"
#define AC_ONLINE_PATH  "/sys/class/power_supply/ac/online"
#define WIRELESS_ONLINE_PATH "/sys/class/power_supply/wireless/online"
#define BKL_LCD_PATH "/sys/class/leds/lcd-backlight/brightness"
#define BOOTMODE_PATH "/sys/class/BOOT/BOOT/boot/boot_mode"

static int bc = 0;
static int firstTime = 0;
int inDraw = 0;
static int nCurrentState = LIGHTS_STATE_UNKNOWN;
static int nChgAnimDuration_msec = 6000;
static int nCbInterval_msec = 200;
static int backlight_on = 1;

int VBAT_POWER_ON = VBAT_POWER_ON_DEFAULT;
int VBAT_POWER_OFF = VBAT_POWER_OFF_DEFAULT;
int lcd_backlight_level = LCD_BL_LEVEL;
pthread_mutex_t mutex, mutexlstate;
pthread_cond_t cond;

static int request_suspend(bool enable)
{
    if (enable)
        return autosuspend_enable();
    else
        return autosuspend_disable();
}

void start_backlight()
{
	int val_bootmode = get_int_value(BOOTMODE_PATH);
	int val_ac = get_int_value(AC_ONLINE_PATH);

    KPOC_LOGI("val_bootmode = %d, val_ac = %d\n", val_bootmode, val_ac);

    if ((val_bootmode == 9) && (val_ac == 0))
    {
#ifdef MTK_BATLOWV_NO_PANEL_ON_EARLY
        lcd_backlight_level = 0;
        if (get_voltage() >= VBAT_POWER_ON)
        {
            lcd_backlight_level = LCD_LOW_BAT_BL_LEVEL;
        }
#else
        lcd_backlight_level = LCD_LOW_BAT_BL_LEVEL;
#endif
    }

	set_int_value(BKL_LCD_PATH,  lcd_backlight_level);
}

void stop_backlight()
{
	set_int_value(BKL_LCD_PATH, 0);
    backlight_on = 0;
}

int is_charging_source_available()
{
    unsigned int usb_online  = get_int_value(USB_ONLINE_PATH);
    unsigned int ac_online  = get_int_value(AC_ONLINE_PATH);
    unsigned int wireless_online  = get_int_value(WIRELESS_ONLINE_PATH);
    unsigned int vchr  = get_int_value(CHARGER_VOLTAGE_PATH);

    KPOC_LOGI("in %s(), usb:%u ac:%u wireless:%u\n", __FUNCTION__,
            usb_online, ac_online, wireless_online);

    return (usb_online == 1 || ac_online == 1 || wireless_online == 1 || vchr != 0);
}

void trigger_anim()
{
	if(inDraw)
		return;
	if (!is_charging_source_available()){
        KPOC_LOGI("no charging source, skip drawing anim\n");
		return;
    }

	pthread_cond_signal(&cond);
}

void start_charging_anim(int reason)
{
	KPOC_LOGI("%s: inDraw:%d, reason:%d\n",__FUNCTION__, inDraw, reason);
	trigger_anim();
}

static int get_capacity()
{
#if defined(MTK_PUMP_EXPRESS_SUPPORT) || defined(MTK_PUMP_EXPRESS_PLUS_SUPPORT)
	while(get_int_value(BATT_CAPACITY_PATH)==-1);
#endif
	return get_int_value(BATT_CAPACITY_PATH);
}

int get_voltage()
{
	int vol = get_int_value(BATT_VOLTAGE_PATH);
	KPOC_LOGI("%s:batt_vol: %d\n", __FUNCTION__, vol);
	return vol;
}

static void set_light_state(int state)
{
	pthread_mutex_lock(&mutexlstate);
	nCurrentState = state;
	pthread_mutex_unlock(&mutexlstate);
}

static int lights_full()
{
	set_light_state(LIGHTS_STATE_CHGFULL);
	lights_chgfull();
	return 0;
}

//return 1: leave, 0: chgon
static int lights_on()
{
	int leave = false;
	
	pthread_mutex_lock(&mutexlstate);
	if (nCurrentState != LIGHTS_STATE_CHGON)
		leave = true;
	pthread_mutex_unlock(&mutexlstate);

	if (!leave) {
		lights_chgon();
		return 0;
	}
	return 1;
}

static int lights_exit()
{
	set_light_state(LIGHTS_STATE_EXIT);
	lights_chgexit();
	return 0;
}

static int on_uevent(const char *buf, int len_buf)
{
#ifdef VERBOSE_OUTPUT
	KPOC_LOGI("on_uevent, %s\n", buf);
#endif
	if (!strcasestr(buf, UEVENT_BATTERY_CHANGE))
		return 1;

	//if ac or usb online
	if (is_charging_source_available()) 
	{
		bc = get_capacity();

		if (bc >= 90) {
			lights_full();
		} else {
			if (nCurrentState != LIGHTS_STATE_CHGON)
                set_light_state(LIGHTS_CHGON);
			lights_on();
		}
	}
	else
        exit_charger(EXIT_CHARGING_MODE);
	
	return 1;
}

static void* uevent_thread_routine(void *arg)
{
	char buf[1024];
	int len;

	if (!uevent_init()) 
	{
		KPOC_LOGI("uevent_init failed.\n");
		return 0;
	}

	while (1) 
	{
		len = uevent_next_event(buf, sizeof(buf) - 1);
		if (len > 0) {
			if (!on_uevent(buf, len))
				break;
		}
	}
	pthread_exit(NULL);
	return NULL;
}

static void exit_charing_thread()
{
	inDraw = 0;
	pthread_exit(NULL);
}

// total_time : ms
// interval : ms
static void draw_with_interval(void (*func)(int, int), int bc, int total_time_msec, int interval_msec)
{
	struct timeval start;
	int resume_started = 0, backlight_started = 0, cnt = 0;
	gettimeofday(&start, NULL);

	while(!time_exceed(start, total_time_msec))
	{
        // check if need to draw animation before performing drawing
		if (!is_charging_source_available())
			return;
		if (!resume_started) {
			resume_started = 1;
			request_suspend(false);
		}
		
		func(bc, ++cnt);
		if (!backlight_started) {
			backlight_started = 1;
			usleep(200*1000);
			start_backlight();
		}

		usleep(interval_msec*1000);
	}
}

static int wait_until(int (*func)(void), int total_time_msec, int interval_msec){
	struct timeval start;
	gettimeofday(&start, NULL);

    while(!time_exceed(start, total_time_msec)){
        if(func()){
            return 1;
        }
		usleep(interval_msec*1000);
    }
    return 0;
}

#define charging_source_waiting_duration_ms 3000
#define charging_source_waiting_interval_ms 200

static void* draw_thread_routine(void *arg)
{
	int i, bc, bc_offset = 0;
	char buf[PROPERTY_VALUE_MAX];
	
    // protect the following critical section for the 1st time
    pthread_mutex_lock(&mutex);
	do {
		KPOC_LOGI("draw thread working...\n");
        // move here to avoid suspend when syncing with surfaceflinger

        if(firstTime){
            // make sure charging source online when in KPOC mode
            // add 2s tolerance
            if(wait_until(is_charging_source_available, 
                        charging_source_waiting_duration_ms,
                        charging_source_waiting_interval_ms))
            {
                KPOC_LOGI("wait until charging source available\n");
            }else{
                KPOC_LOGI("charging source not available for %d ms at KPOC starup\n",
                        charging_source_waiting_duration_ms);
            }
            firstTime = 0;
        }

		inDraw = 1;
		
		// check the bc offest value
		bc = get_capacity();
#if defined(MTK_PUMP_EXPRESS_PLUS_SUPPORT)
		usleep(2000*1000);
#endif
		draw_with_interval(bootlogo_show_charging, bc, nChgAnimDuration_msec, nCbInterval_msec);
		
		stop_backlight();

        // @@@ draw fb again to refresh ddp
        bootlogo_show_charging(bc, 1);
        request_suspend(true);

		inDraw = 0;
		
		pthread_mutex_unlock(&mutex);

		KPOC_LOGI("draw thread waiting...\n");
        pthread_mutex_lock(&mutex);
		pthread_cond_wait(&cond, &mutex);

	} while(1);
	pthread_exit(NULL);
	return NULL;
}

void charging_control()
{
	int ret = 0;
	pthread_attr_t attr, attrd, attrl;
	pthread_t uevent_thread, draw_thread, light_thread;

	//charging led control
	if (!is_charging_source_available()) {
		lights_exit();
	}

	pthread_mutex_init(&mutexlstate, NULL);

	pthread_mutex_init(&mutex, NULL);
	pthread_cond_init(&cond, NULL);

	pthread_attr_init(&attr);
	pthread_attr_init(&attrd);
	pthread_attr_init(&attrl);

	inDraw = 0;

	ret = pthread_create(&uevent_thread, &attr, uevent_thread_routine, NULL);
	if (ret != 0) 
	{
		KPOC_LOGI("create uevt pthread failed.\n");
		exit_charger(EXIT_ERROR_SHUTDOWN);
	}

	firstTime = 1;
	ret = pthread_create(&draw_thread, &attrd, draw_thread_routine, NULL);
	if (ret != 0) 
	{
		KPOC_LOGI("create draw pthread failed.\n");
		exit_charger(EXIT_ERROR_SHUTDOWN);
	}
}
