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
#include <dlfcn.h>
#include <linux/input.h>
#include <cutils/android_reboot.h>
#include <sys/resource.h>
#include <cutils/properties.h>
#include "ipodmain.h"

//using namespace android;

#define LIB_FULL_NAME "/system/lib/libipod.so"

//extern int (*ipod_chglights)[];


// sys.ipo.tbl: IPO boot even if the voltage is below the threshold
static const char *IPOD_TBL_PROP = "sys.ipo.tbl";
unsigned int nTblSupport = 0;

// {af,sys}.mdrst: to sync with modem that if modem need to be reset when IPO bootup
//          deprecated since mt6589
static const char *AF_MDRST_PROP = "af.mdrst";
static const char *SYS_MDRST_PROP = "sys.mdrst";

// sys.ipo.shutdown: sync with PMS that ipod will take care of the backlight control
//                  deprecated?
static const char *IPOD_SHUTDOWN_PROP = "sys.ipo.shutdown";

// stop IPO shutting down if 1) /data/misc/ipod_disable exist 2) sys.ipo.disable not 0 or empty
static const char *IPOD_OFF_FILE = "/data/misc/ipod_disable";
static const char *IPOD_OFF_PROP = "sys.ipo.disable";

// check operator for the bootlogo
static const char *IPOD_OPERATOR_PROP = "ro.operator.optr";
typedef struct {
    int logo1_dur;
    int logo2_dur;
    int anim_dur;
    char *optr;
} boot_time_custom_tag;

static boot_time_custom_tag boot_time_custom_tags[] = {
    {1, 1, 7, "OP01"},
    {1, 1, 7, "OP02"},
};

static const int IPOD_LOGO_DEFAULT =  1;
static const int IPOD_BOOTANIM_DEFAULT = 3;
static const char *IPOD_1ST_DUR_PROP = "sys.ipod_1st_dur";
static const char *IPOD_2ND_DUR_PROP = "sys.ipod_2nd_dur";
static const char *IPOD_3RD_DUR_PROP = "sys.ipod_3rd_dur";

static int ipod_1st_dur = 1;
static int ipod_2nd_dur = 1;
static int ipod_3rd_dur = 3;

static const char *IPOD_COUNTER_PROP = "sys.ipod.counter";

//static bool adb_enabled = false;

int bootlogo_init_done;

// callback to start bootanim
// which will be disabled when WMS is ready
static void _start_bootanim()
{
    property_set("service.bootanim.exit", "0");
    property_set("ctl.start", "bootanim");
}

// XXX: sync with libipod/ipod.h
struct ipod_param ipod_param = {
    .size                   = sizeof(struct ipod_param),
    .logo = {
        .size               = sizeof(struct logo_config),
        .logo1          = bootlogo_show_boot,
        .logo1_dur_sec      = 1,
        .logo2          = bootlogo_show_kernel,
        .logo2_dur_sec      = 1,
        .logo3          = _start_bootanim,
        .logo3_dur_sec      = 2,
        .deinit             = bootlogo_deinit,
        .start_backlight    = start_backlight,
        .stop_backlight     = stop_backlight,
    },
    .callback               = status_cb,
    .charging_animation_trigger      = ipod_trigger_chganim,
    .power_off_timestamp    = 0,
    .wifi_only_modem        = 0,
    .fast_reboot            = 0,
    .reset_modem            = 0,
    .ipoh_mode              = 0,
};

// add prefer power and backlight keys in below arrays.
int pwrkeys[] = {KEY_POWER, KEY_END};
int bklkeys[] = {KEY_HOME, KEY_POWER, KEY_HP};

 /* Specific customization
  * int params[] = {...}
  * p0: desired LIBIPOD version
  * p1: backlight skew timer (msec)
  * p2: charging animatoin total duration (msec)
  * p3: charging animation callback duration (msec) for re-drawing the framebuffer.
  * p4: bit mask for boot types without showing logos
  * p5: bit mask for logos not to show
  *
  * <P1>
  * For thebacklight skew timer, it must be implemented with lights_chgon func.
  * for example, if the requirement is led on for 2 sec, and off for 3 sec,
  * then we can have 2 ways to implement it
  * 1. set skew in 3000 (ms), and when calling into lights_chgon(),
  *  first turn on the led, and then sleep 2000ms, then turn off the led before return from lights_chgon().
  *    The flow will be:
  *    ...-> lights_chgon() -> turn on light -> sleep 2000ms -> turn off light ->...callback after 3000...->...
  *
  * 2. set skew in 2000 (ms), and when calling into lights_chgon(),
  *    first turn off the led, and then sleep 3000ms, then turn on the led before return from lights_chgon().
  *    The flow will be:
  *    ...-> lights_chgon() -> turn off light -> sleep 3000ms -> turn on light ->...callback after 2000...->...
  *
  * <p4> bit mask for boot types without showing logos
  * Currently support alarm, normal boot.You can OR them if needed. Set it to "0" if you don't need to config it.
  *    normal boot: 1<<0
  *    alarm boot:  1<<5
  *
  * <p5> bit mask for logos not to show
  *    logo1: 1<<0
  *    logo2: 1<<1
  *    logo3: 1<<2
  *
  * <p6> backlight level
  *    range from 0~255
  *
  * <P7>
  *     power off time
  *
  * <P8>
  *     Backlight on delay.
  *     Sometimes the SurfaceFlinger may update the LCM behind the logo/charging anim.
  *     This parameter is used to delay some time (in ms) to do the backlighting.
  *
  * <P9> Shutdown voltage, unit: mV
  *      0: default configuration, 3400mV
  *      Others: customization voltage. Must between 3000 and 4000.
  *      Note, use default value will take battery capacity into consideration.
  *      That is, IPO will do shutdown when voltage < 3400 and capacity = 0
  *      Other customization will bypass capacity check.
  * <P10> Power on voltage gate, unit: mV
  *       0: default configuration, 3450mV
  *       Others: customization voltage gate. Must between 3000 and 4000.
  *       Note when IPO detects the battery voltage under this value, IPO won't let device power on.
  *       Normally, it should be <P9> + 50mV
  */

#define MASK_TYPE_NORMAL (1 << 0)
#define MASK_TYPE_ALARM  (1 << 5)

#define MASK_LOGO1 (1 << 0)
#define MASK_LOGO2 (1 << 1)
#define MASK_LOGO3 (1 << 2)

void (*libipod_exit)(int reason) = NULL;
static void (*libipod_setup)(struct ipod_param *) = NULL;
void (*libipod_log)(const char *fmt, ...) = NULL;

void exit_ipod(int reason){
    if(libipod_exit) {
        libipod_exit(reason);
    } else {
        ipod_log("libipod_exit not load");
        do_shutdown("fail to load libipod", false);
    }
}

void loadlib()
{
        void *handle, * func;

        handle = dlopen(LIB_FULL_NAME, RTLD_NOW);
        if (handle == NULL) {
                ipod_log("Can't load library: %s", dlerror());
                goto fail_out;
        }

        if ((libipod_setup = (void (*)(struct ipod_param *))dlsym(handle, "libipod_setup")) == NULL) {
                ipod_log("load 'libipod_setup' error: %s", dlerror());
                goto close_handle;
        }

        if ((libipod_exit = (void (*)(int))dlsym(handle, "exit_ipod")) == NULL) {
                ipod_log("exit_ipod error: %s", dlerror());
                goto close_handle;
        }

        // logging into kernel log & /data/misc/ipod_log
        if ((libipod_log = (void (*)(const char *, ...))dlsym(handle, "ipod_log")) == NULL) {
                ipod_log("libipod_log error: %s", dlerror());
                goto close_handle;
        }

        ipod_log("loadlib success!");
	return;
close_handle:
        dlclose(handle);
        return;
fail_out:
        do_shutdown("fail to load libipod", false);
}

static void check_tablet_support(){
    nTblSupport = property_get_int(IPOD_TBL_PROP, 0);
	ipod_log("tbl support is %d", nTblSupport);
}

static void reset_mdrst_property(){
    property_set(AF_MDRST_PROP, "2");
    property_set(SYS_MDRST_PROP, "2");
}

/*
 * stop ipo shutdown if anyone of the condition fulfilled
 * 1) sys.ipo.disable not 0
 * 2) /data/misc/ipod_disable exist
 */
static void check_ipod_enable(){
	int fd;
    if(0 != property_get_int(IPOD_OFF_PROP, 0) ||
            check_path_exist(IPOD_OFF_FILE))
        exit_ipod(EXIT_DISABLE_IPOD_PROP);
}

static void check_boot_duration(){
	bool hasOptr = false;
	char buf[PROPERTY_VALUE_MAX];

	if (property_get(IPOD_OPERATOR_PROP, buf, "")) {
		ipod_log("found optr: %s", buf);
		for (int i = 0; i < (int) ARRAY_SIZE (boot_time_custom_tags); i++) {
			if (0 == strcmp(boot_time_custom_tags[i].optr, buf)) {
				ipod_1st_dur = boot_time_custom_tags[i].logo1_dur;
				ipod_2nd_dur = boot_time_custom_tags[i].logo2_dur;
				ipod_3rd_dur = boot_time_custom_tags[i].anim_dur;
				hasOptr = true;
				break;
			}
		}
	}

	ipod_1st_dur = property_get_int(IPOD_1ST_DUR_PROP, hasOptr?ipod_1st_dur:IPOD_LOGO_DEFAULT);
	ipod_2nd_dur = property_get_int(IPOD_2ND_DUR_PROP, hasOptr?ipod_2nd_dur:IPOD_LOGO_DEFAULT);
	ipod_3rd_dur = property_get_int(IPOD_3RD_DUR_PROP, hasOptr?ipod_3rd_dur:IPOD_BOOTANIM_DEFAULT);

    ipod_param.logo.logo1_dur_sec = ipod_1st_dur;
    ipod_param.logo.logo2_dur_sec = ipod_2nd_dur;
    ipod_param.logo.logo3_dur_sec = ipod_3rd_dur;

	ipod_log("ipo boot: %d, %d, %d", ipod_1st_dur, ipod_2nd_dur, ipod_3rd_dur);
}

static void update_ipod_counter(){
    char buf[PROPERTY_VALUE_MAX];
    int counter = property_get_int(IPOD_COUNTER_PROP, 0);
    snprintf(buf, PROPERTY_VALUE_MAX, "%d", ++counter);
    property_set(IPOD_COUNTER_PROP, buf);
}

extern void checkIPOHMode(void);
int main(int argc, char *argv[])
{
	loadlib();
    // comment out for selinux policy adjustment
    // load_perfservice();

    updateFastRebootMode();

    // IPO-H mode
    checkIPOHMode();
#ifdef MTK_TB_WIFI_ONLY
	updateTbWifiOnlyMode();
#else
	radiooff_check();
#endif
    finish_shutdown();

    ipod_param.reset_modem = MTK_RESET_MODEM;
    ipod_log("PARAM_RESET_MODEM: %d", ipod_param.reset_modem);

    // adjust priority & oom_adj to control the whole system during IPO shutdown
    setpriority(PRIO_PROCESS, 0, -20);
    FILE *oom_adj = fopen("/proc/self/oom_adj", "w");
    if(oom_adj){
        fputs("-17", oom_adj);
        fclose(oom_adj);
    }
    check_tablet_support();

    if(ipod_param.reset_modem)
        reset_mdrst_property();

    // to keep system awake until key_control is ready to go
    acquire_wakelock(IPOD_WAKELOCK);

    // stop backlight to enter IPO shutdown state
    // acquire backlight control from PowerManagerService
    stop_backlight();
	property_set(IPOD_SHUTDOWN_PROP, "1");

    check_ipod_enable();
    check_boot_duration();

    // count IPO shutting times in sys.ipod.counter
    update_ipod_counter();
    bootlogo_init_done = bootlogo_init();
    ipod_log("after bootlogo_init: %d", bootlogo_init_done);
    if (!bootlogo_init_done) {
	ipod_param.logo.logo1 = NULL;
	ipod_param.logo.logo2 = NULL;
	ipod_param.logo.deinit = NULL;
    }
    charging_control();

    if(libipod_setup){
        libipod_setup(&ipod_param);
        if(ipod_param.fast_reboot)
            exit_ipod(EXIT_POWER_UP);
    }else
        exit_ipod(EXIT_ERROR_SHUTDOWN);

	unsigned int i;
	for (i=0; i< ARRAY_SIZE(pwrkeys); i++)
		ipod_log("pwrkeys[%d]:%d",i,pwrkeys[i]);

	for (i=0; i< ARRAY_SIZE(bklkeys); i++)
		ipod_log("bklkeys[%d]:%d",i,bklkeys[i]);

	key_control(pwrkeys, ARRAY_SIZE(pwrkeys), bklkeys, ARRAY_SIZE(bklkeys)); //will loop inside
    return 0;
}

