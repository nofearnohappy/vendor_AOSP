#include <hardware_legacy/uevent.h>
#include <cutils/properties.h>
#include "ipodmain.h"

#define UEVENT_BATTERY_CHANGE	"power_supply/batt"
#define BATT_CAPACITY_PATH "/sys/class/power_supply/battery/capacity"
#define BATT_VOLTAGE_PATH  "/sys/class/power_supply/battery/batt_vol"
#define VOLTAGE_AVG_PATH   "/sys/class/power_supply/battery/voltage_avg"

#define USB_ONLINE_PATH "/sys/class/power_supply/usb/online"
#define AC_ONLINE_PATH  "/sys/class/power_supply/ac/online"
#define WIRELESS_ONLINE_PATH "/sys/class/power_supply/wireless/online"

#define IPOD_IPOWIN_PROP "sys.ipowin.done"

static int bc = 0;
static bool exitAll = false;
static bool exitNow = false;

static int firstTime = 0;
static bool inDraw = false;
static int pre_chg_status = CONTROL_UNKNOWN;
static int cbTime = 0;
static int nCurrentState = LIGHTS_STATE_UNKNOWN;
static int nChgAnimDuration_msec = 6000;
static int nCbInterval_msec = 200, nResetTimer = 0;

pthread_mutex_t draw_thread_mutex, light_thread_mutex, light_state_mutex;
pthread_cond_t draw_thread_cond, light_thread_cond;
extern int bootlogo_init_done;
extern pthread_mutex_t light_charging_control_mutex;
extern int get_fast_charging_state();

// sync with libipod
bool is_charging_source_available()
{
    unsigned int usb_online  = get_int_value(USB_ONLINE_PATH, true);
    unsigned int ac_online  = get_int_value(AC_ONLINE_PATH, true);
    unsigned int wireless_online  = get_int_value(WIRELESS_ONLINE_PATH, false);

    ipod_log("in %s(), usb:%u ac:%u wireless:%u", __FUNCTION__,
            usb_online, ac_online, wireless_online);

    return (usb_online == 1 || ac_online ==1 || wireless_online == 1);
}

bool is_charging_source_ac_available()
{
	return (get_int_value(AC_ONLINE_PATH, true)==1);
}

void trigger_anim()
{
	if(inDraw)
		return;

	if(exitNow)
		return;

	if (!is_charging_source_available()){
        ipod_log("no charging source, skip drawing anim");
		return;
    }

	pthread_cond_signal(&draw_thread_cond);
}

void start_charging_anim(int reason)
{
	ipod_log("%s: inDraw:%d, reason:%s(%d), exit:%d",__FUNCTION__,
            inDraw,
            (reason >= 0 && reason < TRIGGER_AMOUNT)? trigger_name[reason] : "unknown_reason",
            reason, exitNow);
	trigger_anim();
}

static int get_capacity()
{
	return get_int_value(BATT_CAPACITY_PATH, true);
}

int get_voltage()
{
	int vol = get_int_value(BATT_VOLTAGE_PATH, false);
	ipod_log("%s:batt_vol: %d", __FUNCTION__, vol);
    if ( vol != 0 )
	    return vol;

    vol = get_int_value(VOLTAGE_AVG_PATH, true)/1000;
	ipod_log("%s:voltage_avg: %d", __FUNCTION__, vol);
    return vol;
}

static void set_light_state(int state)
{
	pthread_mutex_lock(&light_state_mutex);
	nCurrentState = state;
	pthread_mutex_unlock(&light_state_mutex);
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

	pthread_mutex_lock(&light_state_mutex);
	if (nCurrentState != LIGHTS_STATE_CHGON)
		leave = true;
	pthread_mutex_unlock(&light_state_mutex);

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
	ipod_log("on_uevent, %s", buf);
#endif
	if (!strcasestr(buf, UEVENT_BATTERY_CHANGE))
		return 1;

	//if ac or usb online
	if (is_charging_source_available())
	{
		if(pre_chg_status == CONTROL_OFF)
			start_charging_anim(TRIGGER_ANIM_UEVENT);

		bc = get_capacity();

		status_cb(EVENT_UEVENT_IN, bc, 0);

		if (bc >= 90) {
			lights_full();
		} else {
			if (cbTime > 0){
				ipod_log("Time: %d,  %d, %d", cbTime, nCurrentState, pre_chg_status);
				pthread_mutex_lock(&light_state_mutex);
				if (nCurrentState != LIGHTS_STATE_CHGON && pre_chg_status == CONTROL_OFF) {
					// in case light thread is in sleep from previous state (in while), but
					// the nCurrentState has been changed by others.
					nCurrentState = LIGHTS_STATE_CHGON; // already have lock.
					pthread_cond_signal(&light_thread_cond);
				}
				pthread_mutex_unlock(&light_state_mutex);
			}
			else
			{
				if (nCurrentState != LIGHTS_STATE_CHGON)
					set_light_state(LIGHTS_STATE_CHGON);
				lights_on();
			}
		}
		pre_chg_status = CONTROL_ON;
	}
	else
	{
		pre_chg_status = CONTROL_OFF;
		lights_exit();

		if (VBAT_POWER_OFF <= 3400) {
			if (get_voltage() <= VBAT_POWER_OFF && get_capacity() == 0)
			exit_ipod(EXIT_LOW_BATTERY);
		} else {
			if (get_voltage() <= VBAT_POWER_OFF)
				exit_ipod(EXIT_LOW_BATTERY);
		}
	}

	return 1;
}

static void* uevent_thread_routine(void *arg)
{
	char buf[1024];
	int len;

	if (!uevent_init())
	{
		ipod_log("uevent_init failed.");
		return 0;
	}

	while (1)
	{
		len = uevent_next_event(buf, sizeof(buf) - 1);
		if (exitAll)
			pthread_exit(NULL);
		if (len > 0) {
			if (!on_uevent(buf, len))
				break;
		}
	}
	pthread_exit(NULL);
	return NULL;
}

static void* light_thread_routine(void *arg)
{
	pthread_mutex_init(&light_charging_control_mutex, NULL);

	while (1)
	{
		ipod_log("light thread wait");
		pthread_mutex_lock(&light_thread_mutex);
		pthread_cond_wait(&light_thread_cond, &light_thread_mutex);

		ipod_log("light thread wake");
		set_light_state(LIGHTS_STATE_CHGON);

		while (is_charging_source_available() && !lights_on() && cbTime > 0)
				usleep(cbTime*1000);

		if (exitAll) {
			// here need to check if need to dismiss LED before boot.
			pthread_exit(NULL);
		}

		pthread_mutex_unlock(&light_thread_mutex);
	}
	pthread_exit(NULL);
	return NULL;
}

static void exit_charing_thread()
{
	inDraw = false;
	status_cb(EVENT_DRAW_CHARGING_ANIM, 0, 0);
	release_wakelock(IPOD_CHARGING_WAKELOCK);
	pthread_exit(NULL);
}

// total_time : ms
// interval : ms
static void draw_with_interval2(void (*func)(int, int), int bc, int total_time_msec, int interval_msec)
{
	struct timeval start;
	int started = 0, cnt = 0;
	//if support PUMP_EXPRESS or PUMP_EXPRESS_PLUS,wait for TA detect done
#if defined(MTK_PUMP_EXPRESS_SUPPORT) || defined(MTK_PUMP_EXPRESS_PLUS_SUPPORT)
	if (is_charging_source_available())
	{
		get_fast_charging_state();
	}
#endif
	gettimeofday(&start, NULL);

	if (nResetTimer)
		nResetTimer = 0;

	while(!time_exceed(start, total_time_msec))
	{
        // check if need to draw animation before performing drawing
		if (exitAll || exitNow || !is_charging_source_available())
			return;

		func(bc, ++cnt);
		if (!started) {
			start_backlight();
			started = 1;
		}

		usleep(interval_msec*1000);

		if (nResetTimer) {
			nResetTimer = 0;
			// get the time again.
			gettimeofday(&start, NULL);
		}
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

static void _charging_animation_func(int bc, int cnt){
    ipod_log("show charging animation");
    if (!bootlogo_init_done)
	    return;
    bootlogo_show_charging(bc, cnt);
}

static void* draw_thread_routine(void *arg)
{
	int i, bc, bc_offset = 0;
	char buf[PROPERTY_VALUE_MAX];

    // protect the following critical section for the 1st time
    pthread_mutex_lock(&draw_thread_mutex);
	do {
		ipod_log("draw thread working...");
        // move here to avoid suspend when syncing with surfaceflinger
		acquire_wakelock(IPOD_CHARGING_WAKELOCK);

 	    if (firstTime) {
			for (i=10; i>0; i--) {
                if(property_get_int(IPOD_IPOWIN_PROP, 0)){
					sleep(2); //give 2 sec tolerance.
                    break;
                } else {
                    sleep(1);
				    ipod_log("draw thread wait ipowin...%d",i);
                }
			}

            // the ac/usb is plugged in while IPO shutting down
            // to avoid showing charging animation again, switch the charging status to ON
            if(is_charging_source_available()){
                pre_chg_status = CONTROL_ON;
                ipod_log("charger detected in IPO right before 1st charging animation, switch to CONTROL_ON");
            }

            firstTime = 0;
        }

		if (exitAll)
			exit_charing_thread();

		inDraw = true;
		exitNow = false;

		status_cb(EVENT_DRAW_CHARGING_ANIM, 1, 0);

		// check the bc offest value
		bc = get_capacity();

		draw_with_interval2(_charging_animation_func, bc, nChgAnimDuration_msec, nCbInterval_msec);
		if (exitAll) {
			exit_charing_thread();
		}

		stop_backlight();

		status_cb(EVENT_DRAW_CHARGING_ANIM, 0, 0);

		inDraw = false;
		exitNow = false;

		release_wakelock(IPOD_CHARGING_WAKELOCK);
		pthread_mutex_unlock(&draw_thread_mutex);

		ipod_log("draw thread waiting...");
        pthread_mutex_lock(&draw_thread_mutex);
		pthread_cond_wait(&draw_thread_cond, &draw_thread_mutex);

	} while(1);
	pthread_exit(NULL);
	return NULL;
}

void charging_control()
{
	int ret = 0;
	pthread_attr_t uevent_thread_attr, draw_thread_attr, light_thread_attr;
	pthread_t uevent_thread, draw_thread, light_thread;

	//charging led control
	if (!is_charging_source_available()) {
		lights_exit();
	}

	pthread_mutex_init(&light_state_mutex, NULL);

	pthread_mutex_init(&draw_thread_mutex, NULL);
	pthread_cond_init(&draw_thread_cond, NULL);

	pthread_mutex_init(&light_thread_mutex, NULL);
	pthread_cond_init(&light_thread_cond, NULL);

	pthread_attr_init(&uevent_thread_attr);
	pthread_attr_init(&draw_thread_attr);
	pthread_attr_init(&light_thread_attr);

	inDraw = false;

    pre_chg_status = CONTROL_OFF;

	ret = pthread_create(&uevent_thread, &uevent_thread_attr, uevent_thread_routine, NULL);
	if (ret != 0)
	{
		ipod_log("create uevent pthread failed.");
		exit_ipod(EXIT_ERROR_SHUTDOWN);
	}

	firstTime = 1;
	ret = pthread_create(&draw_thread, &draw_thread_attr, draw_thread_routine, NULL);
	if (ret != 0)
	{
		ipod_log("create draw pthread failed.");
		exit_ipod(EXIT_ERROR_SHUTDOWN);
	}

	ret = pthread_create(&light_thread, &light_thread_attr, light_thread_routine, NULL);
	if (ret != 0)
	{
		ipod_log("create light pthread failed.");
		exit_ipod(EXIT_ERROR_SHUTDOWN);
	}
}

void anim_thread_exit()
{
	int i = 20;
	exitAll = true;
	if (inDraw)
	{
		while(i-- > 0) {
			if(inDraw) {
				ipod_log("waiting drawing thread exit: %d",20-i);
				usleep(500*1000);
			}
			else
				break;
    	}
	}
}

void ipod_trigger_chganim(int cmd)
{
	ipod_log("%s: inDraw:%d, config:%d",__FUNCTION__,inDraw, cmd);

	switch (cmd) {
		case TRIGGER_ANIM_START:
		case TRIGGER_ANIM_START_RESET:
				exitNow = false;
				nResetTimer = cmd;
				start_charging_anim(cmd);
				break;
		case TRIGGER_ANIM_STOP:
				if (inDraw)
					exitNow = true;
				break;
		case TRIGGER_NORMAL_BOOT:
				exit_ipod(EXIT_POWER_UP);
				break;
		case TRIGGER_ALARM_BOOT:
				exit_ipod(EXIT_ALARM_BOOT);
				break;
		case TRIGGER_REBOOT:
				exit_ipod(EXIT_REBOOT_UBOOT);
				break;
		case TRIGGER_ANIM_STOP_HIB:
				exitNow = true;
				break;
		default:
				break;
	}
}

