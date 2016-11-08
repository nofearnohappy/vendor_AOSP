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

#ifndef _IPODMAIN_H
#define _IPODMAIN_H


#ifdef MTK_LOG_ENABLE
#undef MTK_LOG_ENABLE
#endif
#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <bootlogo.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "IPODMAIN"
#endif

/**********************************************************************
 * IPO-H mode                                                         *
 * set IPOH_MODE to :                                                 *
 * 0 : disable IPO-H mode                                             *
 * 1 : IPO-H mode with hibernation                                    *
 * 2 : IPO-H mode with normal shutdown (original power off feature)   *
 *************************** ******************************************/
#define IPOH_MODE 1
#define IPO_DEFAULT_POWEROFF_TIME (30*60) //sec

#ifndef true
#define true  1
#endif

#ifndef false
#define false 0
#endif

enum {
    CONTROL_UNKNOWN        = 0,
    CONTROL_ON             = 1,
    CONTROL_OFF            = 2,
};

enum {
    LIGHTS_STATE_UNKNOWN = 0,
    LIGHTS_STATE_CHGFULL = 1,
    LIGHTS_STATE_CHGON   = 2,
    LIGHTS_STATE_EXIT    = 3,
};


enum {
    EVENT_DUMMY        = 0,
    EVENT_PREBOOT_IPO  = 1,
    EVENT_BOOT_IPO     = 2,
    EVENT_ALARM_RTC    = 3,
    EVENT_EXIT_IPOD    = 4,
    EVENT_KEY_PRESS    = 5,
    EVENT_UEVENT_IN    = 6,
    EVENT_DRAW_CHARGING_ANIM = 7,
    EVENT_LOWBATT_FAIL_BOOT = 8,
    EVENT_RADIOOFF_CHECK = 9,
    EVENT_REQUEST_BLANK = 10,
    EVENT_REQUEST_UNBLANK = 11,
    EVENT_CHECK_CHARGING = 12,
    EVENT_AMOUNT       = 13,

};

static const char *event_name[] = {
#define S(id) #id
    S(EVENT_DUMMY),             /* 0 */
    S(EVENT_PREBOOT_IPO),       /* 1 */
    S(EVENT_BOOT_IPO ),         /* 2 */
    S(EVENT_ALARM_RTC),         /* 3 */
    S(EVENT_EXIT_IPOD),         /* 4 */
    S(EVENT_KEY_PRESS),         /* 5 */
    S(EVENT_UEVENT_IN),         /* 6 */
    S(EVENT_DRAW_CHARGING_ANIM),/* 7 */
    S(EVENT_LOWBATT_FAIL_BOOT), /* 8 */
    S(EVENT_RADIOOFF_CHECK),    /* 9 */
    S(EVENT_REQUEST_BLANK),     /* 10 */
    S(EVENT_REQUEST_UNBLANK),   /* 11 */
    S(EVENT_CHECK_CHARGING),
};

enum {
    EXIT_POWER_UP         = 0,
    EXIT_REBOOT_UBOOT     = 1,
    EXIT_ERROR_SHUTDOWN   = 2,
    EXIT_DISABLE_IPOD     = 3,
    EXIT_DISABLE_IPOD_PROP = 4,
    EXIT_ALARM_BOOT		  = 5,
    EXIT_LOW_BATTERY	  = 6,
    EXIT_PM_FAIL          = 7,
    EXIT_CHARGING_MODE    = 8,
    EXIT_HIB_BOOT         = 9,
    EXIT_NORMAL_SHUTDOWN    = 10,
    EXIT_AMOUNT           =11,
};

static const char *exit_name[] = {
    S(EXIT_POWER_UP         ),
    S(EXIT_REBOOT_UBOOT     ),
    S(EXIT_ERROR_SHUTDOWN   ),
    S(EXIT_DISABLE_IPOD     ),
    S(EXIT_DISABLE_IPOD_PROP ),
    S(EXIT_ALARM_BOOT		   ),
    S(EXIT_LOW_BATTERY	   ),
    S(EXIT_PM_FAIL           ),
    S(EXIT_CHARGING_MODE     ),
    S(EXIT_HIB_BOOT          ),
    S(EXIT_NORMAL_SHUTDOWN   ),
};

static const char * wakelock_name[] =
{
	"IPOD_WAKELOCK",
	"IPOD_CHG_WAKELOCK",
	"IPOD_KEY_WAKELOCK",
	"IPOD_ALARM_WAKELOCK",
    "IPOD_HIB_WAKELOCK",
    "IPOD_RADIO_WAKELOCK",
    "IPOD_SHUTDOWN_WAKELOCK",
};

enum {
    IPOD_WAKELOCK          = 0,
    IPOD_CHARGING_WAKELOCK = 1,
    IPOD_KEY_WAKELOCK 	   = 2,
	IPOD_ALARM_WAKELOCK    = 3,
    IPOD_HIB_WAKELOCK      = 4,
    IPOD_RADIO_WAKELOCK    = 5,
    IPOD_SHUTDOWN_WAKELOCK  = 6,
    IPOD_WAKELOCK_NUM      = 7, //must the last one
};

enum {
    PARAM_IPO_VER        = 0, /*p0*/
    // PARAM_BK_SKEW        = 1, /*p1, deprecated, move to source release*/
    // PARAM_CHG_DUR        = 2, /*p2, deprecated, charging animation duration*/
    // PARAM_CHG_CB_DURA    = 3, /*p3, deprecated */
    // PARAM_BOOTTYPE_NOLOGO = 4, /*p4, deprecated */
    // PARAM_NOLOGO          = 5, /*p5, deprecated */
    // PARAM_BK_LEVEL		  = 6, /*p6, deprecated*/
    PARAM_PWROFF_TIME     = 1, /*p7*/
    //PARAM_BKL_ON_DELAY    = 8, /*p8, deprecated*/
    //PARAM_POWER_OFF_VOLTAGE = 9, /*9, deprecated*/
    //PARAM_POWER_ON_VOLTAGE = 10, /*10, deprecated*/
    PARAM_TB_WIFI_ONLY = 2, // 11, need
    //PARAM_CHARGING_MODE    = 12, /* 12 */
    PARAM_FAST_REBOOT = 3,         // need
    PARAM_RESET_MODEM       =   4,    // need
    PARAM_IPOH_MODE         =   5,   // need
    //PARAM_KERN_LOGGING      = 16,
    PARAM_AMOUNTS         =     6,
};

enum {
    TRIGGER_ANIM_START       = 0,
    TRIGGER_ANIM_START_RESET = 1,
    TRIGGER_ANIM_STOP        = 2,
    TRIGGER_NORMAL_BOOT	     = 3,
    TRIGGER_ALARM_BOOT       = 4,
    TRIGGER_REBOOT           = 5,
    TRIGGER_ANIM_INIT        = 6,
    TRIGGER_ANIM_UEVENT        = 7,
    TRIGGER_ANIM_KEY         = 8,
    TRIGGER_ANIM_STOP_HIB   = 9,
    TRIGGER_AMOUNT          = 10,
};

static const char *trigger_name[] = {
    S(TRIGGER_ANIM_START),
    S(TRIGGER_ANIM_START_RESET),
    S(TRIGGER_ANIM_STOP),
    S(TRIGGER_NORMAL_BOOT),
    S(TRIGGER_ALARM_BOOT),
    S(TRIGGER_REBOOT),
    S(TRIGGER_ANIM_INIT),
    S(TRIGGER_ANIM_UEVENT),
    S(TRIGGER_ANIM_KEY),
    S(TRIGGER_ANIM_STOP_HIB),
};


int lights_chgfull();
int lights_chgon();
int lights_chgexit();
void load_perfservice();

void set_int_value(const char*, const int, const bool);
int get_int_value(const char*, const bool);
void set_str_value(const char *, const char *, const bool);
int property_get_int(const char *prop, const int);
bool check_path_exist(const char *path);
bool time_exceed(struct timeval start, int duration_msec);
void key_control(int * pwrkeys, int pwrkeys_num, int * bklkeys, int bklkeys_num);
void charging_control();
bool is_charging_source_ac_available();
bool is_charging_source_available();
void start_charging_anim(int reason);
int wait_wakelock(int type, int timeout_msec);

#define WAIT_WAKELOCK_TIMEOUT 500
extern void (*libipod_log)(const char *fmt, ...);
#define ipod_log(fmt, ...) \
    ({ \
            SLOGI(fmt, ##__VA_ARGS__); \
            if(libipod_log != NULL) \
                libipod_log("[ipodmain]" fmt "\n", ##__VA_ARGS__); \
     })

void acquire_wakelock(int type);
void release_wakelock(int type);
void release_all_wakelock();

void stop_backlight();
unsigned int start_backlight();

extern unsigned int nTblSupport;
extern bool ipod_debug ;
void ipod_trigger_chganim(int);
int status_cb(int, int, int);
extern long params[];
void radiooff_check();
void finish_shutdown();
int get_ov_status();
int get_battnotify_status();
void updateTbWifiOnlyMode();
extern int showLowBattLogo;
void updateFastRebootMode();

int is_wireless_charging();
int get_voltage();
int tbl_bypass_batt_check();
#define VBAT_POWER_ON 3450
#define VBAT_POWER_OFF 3400

// IPO-H
extern struct timespec ts_setOff;
// -- end of IPO-H

#ifndef MTK_RESET_MODEM
#define MTK_RESET_MODEM (0)
#endif

#define ARRAY_SIZE(a) (sizeof (a) / sizeof ((a)[0]))
#define AM_CMD_LENGTH 128

void anim_thread_exit();
void exit_ipod(int);
void do_shutdown(const char *reason, bool service);

struct logo_config{
    unsigned int size;
    void (*logo1)();
    unsigned int logo1_dur_sec;
    void (*logo2)();
    unsigned int logo2_dur_sec;
    void (*logo3)();
    unsigned int logo3_dur_sec;
    void (*deinit)();
    unsigned int (*start_backlight)();
    void (*stop_backlight)();
};

struct ipod_param {
    unsigned int size;
    struct logo_config logo;
    int (*callback)(int, int, int);
    void (*charging_animation_trigger)(int);
    unsigned long power_off_timestamp;
    unsigned wifi_only_modem    : 1;
    unsigned fast_reboot        : 1;
    unsigned reset_modem        : 1;
    unsigned ipoh_mode          : 2;
};

extern struct ipod_param ipod_param;

#endif
