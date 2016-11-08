#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <float.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/syscall.h>
#include <dlfcn.h>
#include <pthread.h>
#include <cutils/log.h>
#include <string.h>
#include <linux/input.h>
#include <ctype.h>
#include <linux/sched.h>
#include <linux/types.h>
#include <android/log.h>

#include "invensense.h"
#include "ml_stored_data.h"
#include "ml_load_dmp.h"
#include "ml_sysfs_helper.h"
#include "sensors_io.h"

#include "libpaw8001motion.h"

#define TAG "invmpld: "
#define ICM30628 "icm30628"
#define DEVICE_NAME					"/dev/icm30628"
#define CHIP_TYPE						"ICM"
#define CALIBRATION_FILE		"/data/misc/mpl/inv_cal_data.bin"
#define FIRMWARE_FILE		"/system/etc/firmware/icm30628fw.bin"
#define DMP3_FILE		"/system/etc/firmware/icm30628dmp3.bin"
#define DMP4_FILE		"/system/etc/firmware/icm30628dmp4.bin"

#define PIXART_HRM_LIBRARY

#define ROOT_UID		0
#define SYSTEM_UID	1000
#define SHELL_UID		2000

#define SIG_ICM30628						44
#define ICM30628_IOCTL_GROUP                  0x10
#define ICM30628_WRITE_DAEMON_PID			_IO(ICM30628_IOCTL_GROUP, 1)
#define ICM30628_DOWNLOAD_FIRMWARE		_IO(ICM30628_IOCTL_GROUP, 2)
#define ICM30628_DOWNLOAD_DMP3			_IO(ICM30628_IOCTL_GROUP, 3)
#define ICM30628_DOWNLOAD_DMP4			_IO(ICM30628_IOCTL_GROUP, 4)
#define ICM30628_LOAD_CAL					_IO(ICM30628_IOCTL_GROUP, 5)
#define ICM30628_STORE_CAL					_IO(ICM30628_IOCTL_GROUP, 6)
#define ICM30628_READ_SENSOR_DATA		_IO(ICM30628_IOCTL_GROUP, 7)
#define ICM30628_WRITE_SENSOR_DATA		_IO(ICM30628_IOCTL_GROUP, 8)
#define ICM30628_GET_FIFO_SIZE				_IO(ICM30628_IOCTL_GROUP, 9)
#define ICM30628_GET_ORIENTATION			_IO(ICM30628_IOCTL_GROUP, 10)
#define ICM30628_SEND_ORIENTATION			_IO(ICM30628_IOCTL_GROUP, 11)
#define ICM30628_FIRMWARE_SIZE				_IO(ICM30628_IOCTL_GROUP, 12)
#define ICM30628_GET_HRM_DATA				_IO(ICM30628_IOCTL_GROUP, 13)
#define ICM30628_SEND_HRM_DATA			_IO(ICM30628_IOCTL_GROUP, 14)
#define ICM30628_GET_ORIENTATION_DATA	_IO(ICM30628_IOCTL_GROUP, 15)
#define ICM30628_SEND_ORIENTATION_DATA	_IO(ICM30628_IOCTL_GROUP, 16)
#define ICM30628_DMP3_SIZE					_IO(ICM30628_IOCTL_GROUP, 17)
#define ICM30628_DMP4_SIZE					_IO(ICM30628_IOCTL_GROUP, 18)
#define ICM30628_KERNEL_LOG				_IO(ICM30628_IOCTL_GROUP, 19)

#define REQUEST_SIGNAL_LOAD_CALIBRATION			0x01
#define REQUEST_SIGNAL_STORE_CALIBRATION		0x02
#define REQUEST_SIGNAL_PROCESS_HRM				0x03
#define REQUEST_SIGNAL_PROCESS_ORIENTATION	0x04

#define SIZE_CALIBRATION_GAIN 36
#define SIZE_CALIBRATION_OFFSET 12

struct icm30628_calibration_info_t {
	unsigned char accel_calibration_gain[SIZE_CALIBRATION_GAIN];
	unsigned char accel_calibration_offset[SIZE_CALIBRATION_OFFSET];
	unsigned char gyro_calibration_gain[SIZE_CALIBRATION_GAIN];
	unsigned char gyro_calibration_offset[SIZE_CALIBRATION_OFFSET];
	unsigned char mag_calibration_gain[SIZE_CALIBRATION_GAIN];
	unsigned char mag_calibration_offset[SIZE_CALIBRATION_OFFSET];
};

