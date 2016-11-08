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
#include <fcntl.h>
#include "ipodmain.h"
#include <cutils/properties.h>
#include <linux/android_alarm.h>
#include <cutils/android_reboot.h>
#include <errno.h>
#include <time.h>
#include <sys/time.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <sys/stat.h>

#define RADIO_TIMEOUT_SEC 30
#define IPOD_POWER_DOWN_CAP "sys.ipo.pwrdncap"
#define IPOD_RADIO_OFF_STATE "ril.ipo.radiooff"
#define IPOD_RADIO_OFF_STATE2 "ril.ipo.radiooff.2"
#define IPOD_FINISH_SHUTDOWN_AWAKE_TIME (20)

#define WAKELOCK_ACQUIRE_PATH "/sys/power/wake_lock"
#define WAKELOCK_RELEASE_PATH "/sys/power/wake_unlock"

#define CHARGER_OVER_VOLTAGE 7000
#define CHARGER_VOLTAGE_PATH "/sys/class/power_supply/battery/ChargerVoltage"
#define WIRELESS_CHARGING "/sys/class/power_supply/wireless/online"
#define CHARGER_BATTNOTFY_PATH "/sys/devices/platform/mt-battery/BatteryNotify"

#define IPO_FAST_REBOOT "sys.ipo.fast_reboot"
#define IPO_BKL_ON_DELAY "sys.ipo.bklondelay"

/*
 * IPOD_RADIO_OFF_STATE
 * value:
 *       0: default value, dont bypass
 *       1: ShutdownThread ignores mountservice.shutdown only
 *       2: ShutdownThread ignores wait radio off. IPO will take over this job.
 *       3: ShutdownThread ignores both mountservice.shutdown and wait radio off. IPO will take over this job.
 */

// -- IPO-H
#define IPOH_MODE_ENABLE_PROP "ipo.ipoh.enable"
#define POWEROFF_ALARM_CHECK_TOLERANCE 60 //sec
#define POWEROFF_ALARM_TRIGGER_TOLERANCE 60 //sec
#define SYS_POWER_OFF_ALARM "sys.power_off_alarm"
#define IPO_POWER_OFF_TIME "ipo.power_off_time"
#define IPOH_PWROFFTIME "persist.ipoh.pot"
struct timespec ts_pwroff, ts_setOff;
int setOff = true;
// -- end of IPO-H

#define BOOTMODE_PATH "/sys/class/BOOT/BOOT/boot/boot_mode"

int showLowBattLogo = false;
int inExiting = false;
int radiooff_check_done = false;

#define VERBOSE_OUTPUT

/******/
// code comes from rild to judge if it is in DualTalkMode
#define PROPERTY_TELEPHONY_MODE "ril.telephony.mode"
static int getExternalModemSlot() {
    char property_value[PROPERTY_VALUE_MAX] = { 0 };
    property_get("ril.external.md", property_value, "0");
    return atoi(property_value)-1;
}

static int isInternationalRoamingEnabled() {
    char property_value[PROPERTY_VALUE_MAX] = { 0 };
    property_get("ril.evdo.irsupport", property_value, "0");
    return atoi(property_value);
}

static int getTelephonyMode() {
    char mode[PROPERTY_VALUE_MAX] = {0};
    property_get(PROPERTY_TELEPHONY_MODE, mode, 0);
    if (strlen(mode) > 0)
        return atoi(mode);
    else
        return 0;
}

static int isEvdoOnDualtalkMode() {
    char property_value[PROPERTY_VALUE_MAX] = { 0 };
    property_get("mediatek.evdo.mode.dualtalk", property_value, "1");
    int mode = atoi(property_value);
    ipod_log("evdoOnDualtalkMode mode: %d", mode);
    return mode;
}

static int isEVDODTSupport() {
    char property_value[PROPERTY_VALUE_MAX] = { 0 };
    property_get("ril.evdo.dtsupport", property_value, "0");
    return atoi(property_value);
}

static int getExternalModemSlotTelephonyMode() {
    char property_value[PROPERTY_VALUE_MAX] = { 0 };
    if (getExternalModemSlot() == 0) {
        property_get("mtk_telephony_mode_slot1", property_value, "1");
    } else {
        property_get("mtk_telephony_mode_slot2", property_value, "1");
    }
    return atoi(property_value);
}

static int isDualTalkMode() {
    /// M: when EVDODT support, check if external phone is CDMA.
    if (isEVDODTSupport()) {
        return (getExternalModemSlotTelephonyMode() == 0);
    }

    int telephonyMode = getTelephonyMode();
    if (telephonyMode == 0) {
        return (getExternalModemSlot() >= 0);
    } else if (telephonyMode >= 5) {
        return 1;
    } else {
        return 0;
    }
}
/******/

void do_shutdown(const char *reason, bool service)
{
    char buf[PROPERTY_VALUE_MAX];
    if(reason && strlen(reason) > 0)
        snprintf(buf, PROPERTY_VALUE_MAX, "shutdown,%s", reason);
    else
        strcpy(buf, reason);
    sleep(3); // delay to collect log before shutting down

    if(service)
        // use shutdown service to power down.
        property_set("ctl.start", "shutdown");
    else
        property_set(ANDROID_RB_PROPERTY, buf);
}

int property_get_int(const char *prop, const int def_value){
    char buf[PROPERTY_VALUE_MAX];
    if(property_get(prop, buf, "") && isdigit(buf[0]))
        return atoi(buf);
    return def_value;
}

bool check_path_exist(const char *path){
    struct stat statbuf;
    int ret = 0;
    ret = lstat(path, &statbuf);
    if(ret < 0 || S_ISLNK(statbuf.st_mode))
        return false;
    return true;
}

int write_to_file(const char* path, const char* buf, int size, bool force)
{
    if (!path) {
        ipod_log("null path to write");
        return 0;
    }
#ifdef VERBOSE_OUTPUT
    ipod_log("%s: path: %s, buf: %s, size: %d force:%d",__FUNCTION__, path ,buf, size, force);
#endif

    int fd = open(path, O_RDWR);
    if (fd == -1) {
        ipod_log("Could not open '%s'", path);
        if(force)
            exit_ipod(EXIT_ERROR_SHUTDOWN);

        return 0;
    }

    int count = write(fd, buf, size);
    close(fd);
    if (count != size) {
        ipod_log("write file (%s) fail, count: %d", path, count);
        if(force)
            exit_ipod(EXIT_ERROR_SHUTDOWN);

        return 0;
    }

    return count;
}


void set_int_value(const char * path, const int value, const bool force)
{
    char buf[32];
    sprintf(buf, "%d", value);
#ifdef VERBOSE_OUTPUT
    ipod_log("%s: %s, %s force:%d",__FUNCTION__, path ,buf, force);
#endif
    write_to_file(path, buf, strlen(buf), force);
}

/*   return value:
 *         0, error or read nothing
 *        !0, read counts
 */
int read_from_file(const char* path, char* buf, const int size, const bool force)
{
    if (!path) {
        return 0;
    }

    int fd = open(path, O_RDONLY);
    if (fd == -1) {
        if(force)
            exit_ipod(EXIT_ERROR_SHUTDOWN);

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

int get_int_value(const char * path, const bool force)
{
    int size = 32;
    char buf[size];
    if(!read_from_file(path, buf, size, force))
        return 0;
    return atoi(buf);
}

void set_str_value(const char * path, const char * str, const bool force)
{
    write_to_file(path, str, strlen(str), force);
}

// -- IPO-H
#define IPO_MIN_POWEROFF_TIME (RADIO_TIMEOUT_SEC+20) //sec
long getIPOPowerOffTime(void)
{
    char buf[PROPERTY_VALUE_MAX];
    unsigned long time = 0;
    unsigned long ptime = 0;

    if(property_get(IPO_POWER_OFF_TIME, buf, "0")) {
        time = strtoul(buf, NULL, 10);
        if (time > 0) {
            ipod_log("found ipo.power_off_time = %lu", time);
            time = (time > IPO_MIN_POWEROFF_TIME) ? time : IPO_MIN_POWEROFF_TIME;
            sprintf(buf, "%lu", time);
            property_set(IPOH_PWROFFTIME, buf);
        }
    }

    if (property_get(IPOH_PWROFFTIME, buf, "0")) {
        ptime = strtoul(buf, NULL, 10);
        if (ptime > 0)
            ipod_log("found persist.ipoh.pot = %lu ", ptime);
        ptime = (ptime > 0) ? ptime : IPO_DEFAULT_POWEROFF_TIME;
    }

    return (ptime < IPO_MIN_POWEROFF_TIME ? IPO_MIN_POWEROFF_TIME : ptime);
}

int getPowerOffAlarm()
{
    char buf[PROPERTY_VALUE_MAX];
    unsigned long time = 0;
    if(property_get(SYS_POWER_OFF_ALARM,buf,"0")) {
        time = strtoul(buf, NULL, 10);
        if(time > 0) {
            ts_pwroff.tv_sec = time;
            ts_pwroff.tv_nsec = 0;
            ipod_log("found power off alarm: %lu ",time);
            return true;
        }
    }
    return false;
}

int clearPowerOffAlarmProperty()
{
    char buf[PROPERTY_VALUE_MAX];
    unsigned long time = 0;
    if(property_get(SYS_POWER_OFF_ALARM,buf,"0")) {
        time = strtoul(buf, NULL, 10);
        if(time > 0) {
            ipod_log("reset power off alarm systemproperty");
            property_set(SYS_POWER_OFF_ALARM,"0");
            return true;
        }
    }
    return false;
}

int getTime(struct timespec *ts)
{
    time_t t;

    time(&t);
    ts->tv_sec = t;

    ipod_log("getTime: %ld", ts->tv_sec);
    return true;
}

void enablePowerOff(long offTimeSec)
{
    setOff = true;

    if (getTime(&ts_setOff)) {
        if (getPowerOffAlarm()) {
            // we have power off alarm set from AlarmManagerService.
            if ((ts_pwroff.tv_sec - ts_setOff.tv_sec) < (offTimeSec + POWEROFF_ALARM_CHECK_TOLERANCE)) {
                // If the power on alarm is set before the offTimeSec-60sec,
                // no need to set extra alarm to power off device because we will get power on
                // before we want to real power off in IPO.
                // The 60sec is the tolerance time. Suggest >= 60sec.
                setOff = false;
                ts_setOff.tv_sec = 0;
            }
        }
        if (setOff) {
            ts_setOff.tv_sec += offTimeSec;
            ipod_param.power_off_timestamp = ts_setOff.tv_sec;
            ipod_log("set power off time: %ld", ipod_param.power_off_timestamp);
        }
    }
}

void checkPowerOff()
{
    struct timespec ts;
    if (setOff) {
        if (getTime(&ts)) {
            ipod_log("checkPowerOff, now: %ld, set %ld", ts.tv_sec, ts_setOff.tv_sec);
            if (labs(ts.tv_sec - ts_setOff.tv_sec) < POWEROFF_ALARM_TRIGGER_TOLERANCE) {
                // If alarm is triggered and the trigger time is +-POWEROFF_ALARM_CHECK_TOLERANCE
                // sec to the expected real power off time, do the power off procedure.
                ipod_log("IPO-H shutdown device...");
                do_shutdown("IPOH shutdown", false);
                while(1) usleep(1000*1000);
            }
        }
    }
}

void checkIPOHMode(void)
{
    char buf[PROPERTY_VALUE_MAX];

    // runtime setting first
    if (!property_get(IPOH_MODE_ENABLE_PROP, buf, NULL)) {
        sprintf(buf, "%d", IPOH_MODE);
    }

#if !defined(MTK_IPOH_SUPPORT)
    // IPO-H feature disabled, force to disable mode
    sprintf(buf, "%d", 0);
#endif

#if defined(TRUSTONIC_TEE_SUPPORT)
    ipod_log("IPOH mode disabled due to TRUSTONIC_TEE_SUPPORT enabled.\n");
    sprintf(buf, "%d", 0);
#endif

    property_set(IPOH_MODE_ENABLE_PROP, buf);
    ipod_param.ipoh_mode = atoi(buf) & 0x3;

    ipod_log("IPO-H: mode (%d)", atoi(buf));

    // set power off time, if ipoh is enable
    if (isdigit(buf[0]) && atoi(buf) != 0) {
        enablePowerOff(getIPOPowerOffTime());
    }
}

int getIPOHMode(void)
{
    char buf[PROPERTY_VALUE_MAX];
    int mode;

    // re-confirm the value consistence
    property_get(IPOH_MODE_ENABLE_PROP, buf, "0");
    mode = isdigit(buf[0]) ? atoi(buf) : 0;
    if (mode != ipod_param.ipoh_mode) {
        ipod_log("IPO-H: sys. property value (%d), params[PARAM_IPOH_MODE] (%d) is in-consistent!!",
               mode, ipod_param.ipoh_mode);
    }

    return (int)ipod_param.ipoh_mode;
}
// -- end of IPO-H

void updateTbWifiOnlyMode()
{
    ipod_param.wifi_only_modem = 1;
}

static bool if_boot_graceful(int reason){
    if (reason == EXIT_ALARM_BOOT ||
            reason == EXIT_POWER_UP ||
            reason == EXIT_HIB_BOOT)
        return true;
    else
        return false;
}

static void (*perfBoostEnable)(int) = NULL;
static void (*perfBoostDisable)(int) = NULL;

void load_perfservice(){
    void *handle;
    if((handle = dlopen("/system/lib/libperfservice.so", RTLD_NOW)) == NULL){
        ipod_log("fail to load perfService library");
        return;
    }
    perfBoostEnable = (void (*)(int))dlsym(handle, "perfBoostEnable");
    perfBoostDisable = (void (*)(int))dlsym(handle, "perfBoostDisable");
    if(!perfBoostEnable  || !perfBoostDisable) {
        ipod_log("fail to load perfService functions");
        dlclose(handle);
    }
}

int status_cb(int event, int data1, int data2)
{
    /*
     * DO NOT BLOCK THIS FUNCTION!
     */

    int val = 0;
	ipod_log("status_cb: %s(%d), %d, %d",
            (event > EVENT_DUMMY && event < EVENT_AMOUNT)? event_name[event] : "unknown_event",
            event, data1, data2);

    switch (event) {
        case EVENT_PREBOOT_IPO:
            if (getIPOHMode() != 0) {
                if (data1 == 1) //after preboot_ipo intent is sent.
                    clearPowerOffAlarmProperty();
            }
            // disable perf boost when finishing sending PREBOOT_IPO
            if(data1 == 0 && data2 == 0 && perfBoostDisable)
                perfBoostDisable(1);
            break;

        case EVENT_BOOT_IPO:
            break;

        case EVENT_ALARM_RTC:
            if (getIPOHMode() != 0) {
                checkPowerOff();
            }
            break;

        case EVENT_DRAW_CHARGING_ANIM:
            if (data1 == 0)
                showLowBattLogo = 0;
            break;

        case EVENT_LOWBATT_FAIL_BOOT:
            // libipod indicate that user long press power key in low power state
            showLowBattLogo = 1;
            ipod_trigger_chganim(0);
            break;

            case EVENT_KEY_PRESS:
            break;

        case EVENT_UEVENT_IN:
            break;

        case EVENT_EXIT_IPOD:
            inExiting = true;
            // in case the releasing wakelock thread has no chance to execute
            release_wakelock(IPOD_SHUTDOWN_WAKELOCK);
            // release radio wakelock in IPO boot
            release_wakelock(IPOD_RADIO_WAKELOCK);
            if(ipod_param.fast_reboot)
                val = property_set(IPO_FAST_REBOOT, "0");
            if(if_boot_graceful(data1) && perfBoostEnable)
                perfBoostEnable(1);
            if(data1 == EXIT_ALARM_BOOT ||
                    data1 == EXIT_HIB_BOOT ||
                    data1 == EXIT_POWER_UP)
                anim_thread_exit();
            break;

        case EVENT_RADIOOFF_CHECK:
#ifdef MTK_TB_WIFI_ONLY
            val = 1;
#else
            val = radiooff_check_done;
#endif
            break;

        case EVENT_CHECK_CHARGING:
            break;

        default:
            break;
    }
    return val;
}

int getPowerDownCap()
{
    char buf[PROPERTY_VALUE_MAX];
    unsigned int value = 0;
    if(property_get(IPOD_POWER_DOWN_CAP, buf, "0")) {
        value = atoi(buf);
        if(value == 2 || value == 3) {
            ipod_log("radio off check is on (%d) ",value);
            return true;
        }
    }
    ipod_log("radio off check is off (%d) ",value);
    return false;
}

int getRadioOffState()
{
    char buf[PROPERTY_VALUE_MAX];
    unsigned int value = 0;
    if(property_get(IPOD_RADIO_OFF_STATE, buf, "0")) {
        value = atoi(buf);
        return value;
    }
    return false;
}

int getDualTalkRadioOffState(){
    char buf[PROPERTY_VALUE_MAX];
    unsigned int radiooff = 0;
    unsigned int radiooff2 = 0;

    if(property_get(IPOD_RADIO_OFF_STATE, buf, "0")) {
        radiooff = atoi(buf);
    }
    if(property_get(IPOD_RADIO_OFF_STATE2, buf, "0")) {
        radiooff2 = atoi(buf);
    }
    if(radiooff && radiooff2)
        return true;
    else
        return false;
}

static void* radiooff_check_routine(void *arg)
{
    int i=0;
    do {
        sleep(1);
        int DualTalkMode = isDualTalkMode();
        if ((DualTalkMode && getDualTalkRadioOffState()) ||
                (!DualTalkMode && getRadioOffState())){
            ipod_log("In %s, radio off done (%d sec).",
                    DualTalkMode?"DualTalkMode":"non-DualTalkMode",
                    i);
            radiooff_check_done = 1;
            release_wakelock(IPOD_RADIO_WAKELOCK);
            pthread_exit(NULL);
        }
    }while(++i < RADIO_TIMEOUT_SEC);

    if(ipod_param.fast_reboot) {
        ipod_log("radio off timeout (%d sec), reboot for fast_reboot mode", RADIO_TIMEOUT_SEC);
        property_set(ANDROID_RB_PROPERTY, "reboot,radiooff timeout");
    }else if(!inExiting){
        ipod_log("radio off timeout (%d sec), shutdown", RADIO_TIMEOUT_SEC);
        do_shutdown("radio off timeout", false);
    }else
        ipod_log("radio off timeout but exiting.");

    return 0;
}

void radiooff_check()
{
    int ret = 0;
    pthread_attr_t attr;
    pthread_t checkradiooff_thread;

    if (!getPowerDownCap())
        return;

    acquire_wakelock(IPOD_RADIO_WAKELOCK);
    pthread_attr_init(&attr);

    ret = pthread_create(&checkradiooff_thread, &attr, radiooff_check_routine, NULL);
    if (ret != 0)
    {
        ipod_log("create radio check pthread failed.");
        do_shutdown("create pthread fail", false);
    }
}

static void* release_wakelock_routine(void *arg){
    unsigned int i = 0;

    do{
        i++;
        sleep(1);
    }while(!inExiting && i < IPOD_FINISH_SHUTDOWN_AWAKE_TIME);

    ipod_log("release %s in %u s...", wakelock_name[IPOD_SHUTDOWN_WAKELOCK], i);
    release_wakelock(IPOD_SHUTDOWN_WAKELOCK);
    return 0;
}

void finish_shutdown(){
    int ret = 0;
    pthread_attr_t attr;
    pthread_t release_wakelock_thread;

    acquire_wakelock(IPOD_SHUTDOWN_WAKELOCK);
    pthread_attr_init(&attr);
    ret = pthread_create(&release_wakelock_thread, &attr, release_wakelock_routine, NULL);
    if (ret != 0)
    {
        ipod_log("create finish shutdown pthread failed.");
        do_shutdown("create pthread fail", false);
    }
}


/*
 * return value:
 *     1: over voltage
 *     0: normal voltage
 */
int get_ov_status()
{
    int voltage = get_int_value(CHARGER_VOLTAGE_PATH, true);
    ipod_log("charger voltage: %d",voltage);

    if (voltage >= CHARGER_OVER_VOLTAGE) {
        return 1;
    }
    return 0;
}

/*
 * return value:
 *     1: abnormal status
 *     0: normal status
 */
int get_battnotify_status()
{
	int battStatus = get_int_value(CHARGER_BATTNOTFY_PATH, false);
	ipod_log("charger battStatus: %d",battStatus);
	if (battStatus != 0) {
		return 1;
	}
	return 0;
}


void updateFastRebootMode(){
    char buf[PROPERTY_VALUE_MAX];

    if(property_get(IPO_FAST_REBOOT, buf, "0")){
        if(isdigit(buf[0]) && (buf[0]!='0')){
            ipod_log("Fast reboot is on");
            ipod_param.fast_reboot = 1;
        }else
            ipod_log("Fast reboot is off");
    }
}

int is_wireless_charging(){
    int wireless_charging = get_int_value(WIRELESS_CHARGING, false);
    ipod_log("wireless_charging: %d", wireless_charging);
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
