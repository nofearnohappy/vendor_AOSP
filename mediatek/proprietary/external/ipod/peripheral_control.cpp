#include <powermanager/IPowerManager.h>
#include <utils/SystemClock.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include <cutils/properties.h>

#include "ipodmain.h"

#define BKL_LCD_PATH "/sys/class/leds/lcd-backlight/brightness"
#define BKL_KEYQWT_PATH "/sys/class/leds/keyboard-backlight/brightness"
#define BKL_KEYSTD_PATH "/sys/class/leds/button-backlight/brightness"
#define POWER_SUPPLY_PATH "/sys/class/power_supply"
#define LED_RED_PATH "/sys/class/leds/red/brightness"
#define LED_BLUE_PATH "/sys/class/leds/blue/brightness"
#define LED_GREEN_PATH "/sys/class/leds/green/brightness"
#define WAKELOCK_ACQUIRE_PATH "/sys/power/wake_lock"
#define WAKELOCK_RELEASE_PATH "/sys/power/wake_unlock"


using namespace android;

static int ipod_wakelock_ind = 0;

#define IPOWIN_SYNC_PROP "sys.ipowin.done"

static int backlight_on = 1;
int nBKLOnDelay = 0;

static void _start_backlight(void){
    const int delay_msec = 100;
    
    sp<IBinder> binder = defaultServiceManager()->getService(String16("power"));
    if(binder == NULL){
        ipod_log("failed to get power service");
        return;
    }
    sp<IPowerManager> pms = IPowerManager::asInterface(binder);
    if(pms == NULL){
        ipod_log("fail to get power service");
        return;
    }
    pms->startBacklight(delay_msec);
}

static void _stop_backlight(void){
    sp<IBinder> binder = defaultServiceManager()->getService(String16("power"));
    if(binder == NULL){
        ipod_log("failed to get power service");
        return;
    }
    sp<IPowerManager> pms = IPowerManager::asInterface(binder);
    if(pms == NULL){
        ipod_log("fail to get power service");
        return;
    }
    pms->stopBacklight();
}

unsigned int start_backlight()
{
	struct timeval start;
	char buf[PROPERTY_VALUE_MAX];

	ipod_log("backlight on with delay: %d", nBKLOnDelay);

    if(!backlight_on){
        // only unblank once if multiple unblank requests active simultaneously
        backlight_on++;
        
        // reset sys.ipowin.done before unblank
        property_set(IPOWIN_SYNC_PROP, "0");

	    status_cb(EVENT_REQUEST_UNBLANK, 1, 0);

        _start_backlight();

	    status_cb(EVENT_REQUEST_UNBLANK, 0, 0);

	    gettimeofday(&start, NULL);
	    while(!time_exceed(start, 2000))
	    {
	    	usleep(250*1000); //250ms
	    	if (property_get(IPOWIN_SYNC_PROP, buf, "0")) {
	    		ipod_log("ipowin state: %s", buf);
	    		if (!strncmp(buf, "1", 1)) {
	    			ipod_log("ipowin ready");
	    			// reset it to "0"
	    			property_set(IPOWIN_SYNC_PROP, "0");
	    			return true;
	    		}
	    	}
	    }
	    ipod_log("wait ipowin timeout, ignore it");
        return 1;
    }else
        return 0;
}

void stop_backlight()
{
    status_cb(EVENT_REQUEST_BLANK, 1, 0);
    _stop_backlight();
    status_cb(EVENT_REQUEST_BLANK, 0, 0);
    backlight_on = 0;
}

/* 
 * Wait specific wakelock being held.
 * Return:
 *        0, wakelock being held
 *        1, timeout
 */
int wait_wakelock(int type, int timeout_msec)
{
	struct timeval start;
	gettimeofday(&start, NULL);
	
	while(!time_exceed(start, timeout_msec))
	{
		if (ipod_wakelock_ind & (1<<type)) {
			ipod_log("wait wakelock done, type: %d", type);
			return 0;
		}
		usleep(50*1000); //50ms
	}
	ipod_log("wait wakelock timeout, type %d, timeout: %d", type, timeout_msec);
	return 1;
}

void acquire_wakelock(int type)
{
	ipod_wakelock_ind |= (1<<type);
	set_str_value(WAKELOCK_ACQUIRE_PATH, wakelock_name[type], true);
}

void release_wakelock(int type)
{
    if(ipod_wakelock_ind & (1<<type)){
	    ipod_wakelock_ind &= ~(1<<type);
	    set_str_value(WAKELOCK_RELEASE_PATH, wakelock_name[type], true);
    }
}

void release_all_wakelock()
{
	int i;
	ipod_log("%s: ipod_wakelock_ind: 0x%x",__FUNCTION__,ipod_wakelock_ind);
	for(i=0; i<IPOD_WAKELOCK_NUM; i++)
	{
		if(ipod_wakelock_ind & (1<<i))
		{
			release_wakelock(i);
			ipod_wakelock_ind &= ~(1<<i);
		}	
	}
	ipod_log("Done: ipod_wakelock_ind: 0x%x",ipod_wakelock_ind);
}
	
/*
 * tbl_bypass_batt_check()
 *
 * Description
 *     Only handle the case if tablet property is set
 *
 * return value
 *     0: need to do battery check
 *     1: by pass battery check
 *
 */
int tbl_bypass_batt_check()
{
	if (nTblSupport) {
		if (is_charging_source_ac_available())
			return true;
		else
			return false;
	}
	else
		return false;
}
