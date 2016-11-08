#include <ctype.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <linux/input.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/limits.h>
#include <cutils/properties.h>

#include "ipodmain.h"

#define LONG_PRESS_DEFAULT_DURATION 500
#define INPUT_DEVICE_PATH "/dev/input"
#define IPOD_PWRKEY_TIME "sys.ipod_pwrkey_time"

static const char *keypad_device_name[] = {
    "kpd",
    "keypad",
};

static struct pollfd *ufds;
static char **device_names;
static int nfds, wd;
static pthread_mutex_t key_handle_mutex; 
pthread_cond_t key_handle_cond; 
static int keyDownPressed = false;
static int keyUpPressed = false;
static int powerKeyPressed = false;
static int longPressDuration = LONG_PRESS_DEFAULT_DURATION;
static int longPressDetecting = false;
static int needShowChargingAnimation = false;

static void* key_thread_routine(void *arg)
{
	struct timeval start;
	acquire_wakelock(IPOD_KEY_WAKELOCK);
	while(1)
	{
		release_wakelock(IPOD_KEY_WAKELOCK);
		pthread_mutex_lock(&key_handle_mutex);
		pthread_cond_wait(&key_handle_cond, &key_handle_mutex);
		acquire_wakelock(IPOD_KEY_WAKELOCK);

		longPressDetecting = true;

		gettimeofday(&start, NULL);
		ipod_log("pwr key long press check start");
		
		while(powerKeyPressed)
		{
			usleep(1000*100); //200ms
			if(time_exceed(start, longPressDuration))
			{
				ipod_log("pwr key reaches boot condition");
				if(get_voltage() > VBAT_POWER_ON || tbl_bypass_batt_check())
				{
				// ready to boot up.
				inotify_rm_watch(ufds[0].fd, wd);
				if (keyDownPressed || keyUpPressed)
					exit_ipod(EXIT_REBOOT_UBOOT);
				else
					exit_ipod(EXIT_POWER_UP);
				}
				else {
					ipod_log("VBAT <= %d", VBAT_POWER_ON);
					status_cb(EVENT_LOWBATT_FAIL_BOOT, 0, 0);
				}
			}
		}

		if (needShowChargingAnimation) 
		{
			ipod_log("check pwr key show anim...");
			if (is_charging_source_available()) {
				ipod_log("need to show anim");
				start_charging_anim(TRIGGER_ANIM_KEY);
				wait_wakelock(IPOD_CHARGING_WAKELOCK, WAIT_WAKELOCK_TIMEOUT);
			}
			needShowChargingAnimation = false;
		}
		longPressDetecting = false;
		ipod_log("pwr key long press check end");
		pthread_mutex_unlock(&key_handle_mutex);
	}
	return NULL;
}

void long_press_control()
{
	int ret = 0;
	pthread_attr_t attr;
	pthread_t pwrkey_thread;
	
	pthread_mutex_init(&key_handle_mutex, NULL);
	pthread_cond_init(&key_handle_cond, NULL);
	pthread_attr_init(&attr);

	ret = pthread_create(&pwrkey_thread, &attr, key_thread_routine, NULL);
	if (ret != 0) 
	{
		ipod_log("create key pthread failed.");
		exit_ipod(EXIT_ERROR_SHUTDOWN);
	}
}

static int is_keypad_device(const char *filename)
{
	int i;
	char name[PATH_MAX];
	char * strpos = NULL;
	
	for (i = 0; i < (int) ARRAY_SIZE(keypad_device_name); i++){
		ipod_log("check device name: %s v.s. %s ", filename, keypad_device_name[i]);
		strpos = strcasestr(filename, keypad_device_name[i]);
		if (strpos != NULL)
			return true;
	}
	return false;
}


static int open_device(const char *device)
{
    int version;
    int fd;
    struct pollfd *new_ufds;
    char **new_device_names;
    char name[80];
    char location[80];
    char idstr[80];
    struct input_id id;
	int print_flags = 0xffff;

    fd = open(device, O_RDWR);
    if(fd < 0) {
        ipod_log("could not open %s, %s", device, strerror(errno));
        return -1;
    }  
    if(ioctl(fd, EVIOCGVERSION, &version)) {
        ipod_log("could not get driver version for %s, %s", device, strerror(errno));
        return -1;
    }
    if(ioctl(fd, EVIOCGID, &id)) {
        ipod_log("could not get driver id for %s, %s", device, strerror(errno));
        return -1;
    }
    name[sizeof(name) - 1] = '\0';
    location[sizeof(location) - 1] = '\0';
    idstr[sizeof(idstr) - 1] = '\0';
    if(ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1)
        name[0] = '\0';

	if(!is_keypad_device(name))
		return -1;
	
    if(ioctl(fd, EVIOCGPHYS(sizeof(location) - 1), &location) < 1)
        location[0] = '\0';

    if(ioctl(fd, EVIOCGUNIQ(sizeof(idstr) - 1), &idstr) < 1)
		idstr[0] = '\0';

    new_ufds = (struct pollfd*)realloc(ufds, sizeof(ufds[0]) * (nfds + 1));
    if(new_ufds == NULL) {
        ipod_log("out of memory");
        return -1;
    }
    ufds = new_ufds;
    new_device_names = (char**)realloc(device_names, sizeof(device_names[0]) * (nfds + 1));
    if(new_device_names == NULL) {
        ipod_log("out of memory");
        return -1;
    }
    device_names = new_device_names;
    ufds[nfds].fd = fd;
    ufds[nfds].events = POLLIN;
    device_names[nfds] = strdup(device);
    nfds++;

    return 0;
}

int close_device(const char *device)
{
    int i;
    for(i = 1; i < nfds; i++) {
        if(strcmp(device_names[i], device) == 0) {
            int count = nfds - i - 1;
            ipod_log("remove device %d: %s", i, device);
            free(device_names[i]);
            memmove(device_names + i, device_names + i + 1, sizeof(device_names[0]) * count);
            memmove(ufds + i, ufds + i + 1, sizeof(ufds[0]) * count);
            nfds--;
            return 0;
        }
    }
    ipod_log("remote device: %s not found", device);
    return -1;
}

static int read_notify(const char *dirname, int nfd)
{
    int res;
    char devname[PATH_MAX];
    char *filename;
    char event_buf[512];
    int event_size;
    int event_pos = 0;
    struct inotify_event *event;

    res = read(nfd, event_buf, sizeof(event_buf));
    if(res < (int)sizeof(*event)) {
        if(errno == EINTR)
            return 0;
        ipod_log("could not get event, %s", strerror(errno));
        return 1;
    }

    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';

    while(res >= (int)sizeof(*event)) {
        event = (struct inotify_event *)(event_buf + event_pos);
        if(event->len) {
            strcpy(filename, event->name);
            if(event->mask & IN_CREATE) {
                open_device(devname);
            }
            else {
                close_device(devname);
            }
        }
        event_size = sizeof(*event) + event->len;
        res -= event_size;
        event_pos += event_size;
    }
    return 0;
}

static int scan_dir(const char *dirname)
{
    char devname[PATH_MAX];
    char *filename;
    DIR *dir;
    struct dirent *de;
    dir = opendir(dirname);
    if(dir == NULL)
        return -1;
    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.' &&
           (de->d_name[1] == '\0' ||
            (de->d_name[1] == '.' && de->d_name[2] == '\0')))
            continue;
		strcpy(filename, de->d_name);
        open_device(devname);
    }
    closedir(dir);
    return 0;
}

void check_long_press_duration()
{
	char buf[PROPERTY_VALUE_MAX];
	if(property_get(IPOD_PWRKEY_TIME,buf,"0")) {
		if(isdigit(buf[0]))
			longPressDuration = atoi(buf);
	}

	if(longPressDuration <= 0 )
		longPressDuration = LONG_PRESS_DEFAULT_DURATION;

	ipod_log("power key long press to boot: %d ms",longPressDuration);
}

void key_control(int * pwrkeys, int pwrkeys_num, int * bklkeys, int bklkeys_num)
{
	int res,i,j,k;
    int pollres;
    const char *device = NULL;
    struct input_event event;

	check_long_press_duration();
	long_press_control();
	
    nfds = 1;
    ufds = (struct pollfd *)calloc(1, sizeof(ufds[0]));
    ufds[0].fd = inotify_init();
    ufds[0].events = POLLIN;

	wd = inotify_add_watch(ufds[0].fd, INPUT_DEVICE_PATH, IN_DELETE | IN_CREATE);
    if(wd < 0) {
        ipod_log("could not add watch for %s, %s", INPUT_DEVICE_PATH, strerror(errno));
        exit_ipod(EXIT_ERROR_SHUTDOWN);
    }
    res = scan_dir(INPUT_DEVICE_PATH);
    if(res < 0) {	
        ipod_log("scan dir failed for %s", INPUT_DEVICE_PATH);
        exit_ipod(EXIT_ERROR_SHUTDOWN);
    }

    while(1) {
		release_wakelock(IPOD_WAKELOCK);
        pollres = poll(ufds, nfds, -1);
		acquire_wakelock(IPOD_WAKELOCK);
        if(ufds[0].revents & POLLIN) {
            read_notify(INPUT_DEVICE_PATH, ufds[0].fd);
        }
        for(i = 1; i < nfds; i++) {
            if(ufds[i].revents) {
                if(ufds[i].revents & POLLIN) {
                    res = read(ufds[i].fd, &event, sizeof(event));
                    if(res < (int)sizeof(event)) {
                        ipod_log("could not get event");
                        exit_ipod(EXIT_ERROR_SHUTDOWN);
                    }
#ifdef VERBOSE_OUTPUT
					ipod_log("%s: event.type:%d,%d:%d",__FUNCTION__,event.type,event.code,event.value);
#endif
					if (EV_KEY == event.type) {

						status_cb(EVENT_KEY_PRESS, event.code, event.value);
						
						//handling pwrkeys
						for (k=0; k<pwrkeys_num; k++) {
							if(event.code == pwrkeys[k]) {
								if (1 == event.value) {
									powerKeyPressed = true;
									pthread_cond_signal(&key_handle_cond);
									wait_wakelock(IPOD_KEY_WAKELOCK, WAIT_WAKELOCK_TIMEOUT);
								} else
									powerKeyPressed = false;
								break;
							}
						}
						//handling bklkeys
						for (k=0; k<bklkeys_num; k++) {
							if(event.code == bklkeys[k]) {
								if (1 == event.value) {
									if (!longPressDetecting)
										start_charging_anim(TRIGGER_ANIM_KEY);
									else
										needShowChargingAnimation = true;
								}
								break;
							}
						}
						switch (event.code) {
							case KEY_VOLUMEDOWN:
								if (1 == event.value) {
									keyDownPressed = true;
									keyUpPressed = false;
								} else {
									keyDownPressed = false;
								}
								continue;
							case KEY_VOLUMEUP:
								if (1 == event.value) {
									keyDownPressed = false;
									keyUpPressed = true;
								} else {
									keyUpPressed = false;
								}
								continue;
							default:
								continue;
						}
					}	
                }
            }
        }
    }
}

