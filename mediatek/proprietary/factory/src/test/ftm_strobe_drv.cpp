#define LOG_TAG "FlashlightDrv"
#include "flash_drv.h"
#include <utils/Errors.h>
#include <cutils/log.h>
#include <fcntl.h>
#include "kd_flashlight.h"

#define STROBE_DEV_NAME    "/dev/kd_camera_flashlight"

#include <time.h>


int ioctlEx(int stb, int cmd, int sensorDev, int arg)
{
    //StrobeDrvArg stbArg;
    //stbArg.sensorDev=sensorDev;

    //return ioctl(stb, cmd, &stbArg);


    kdStrobeDrvArg stbArg;
    stbArg.sensorDev=sensorDev;
    stbArg.strobeId = 1;
    stbArg.arg=arg;
    return ioctl(stb, cmd, &stbArg);


    //return 0;


}

int ioctlRetEx(int stb, int cmd, int sensorDev, int* arg)
{
    kdStrobeDrvArg stbArg;
    stbArg.sensorDev=sensorDev;
    stbArg.strobeId = 1;
    stbArg.arg=0;
    int ret;
    ret = ioctl(stb, cmd, &stbArg);
    *arg = stbArg.arg;
    return ret;
}


static int getMs()
{
	//	max:
	//	2147483648 digit
	//	2147483.648 second
	//	35791.39413 min
	//	596.5232356 hour
	//	24.85513481 day
	int t;
	struct timeval tv;
	gettimeofday(&tv, NULL);
	t = (tv.tv_sec*1000 + (tv.tv_usec+500)/1000);
	return t;
}

FlashSimpleDrv::FlashSimpleDrv()
{
    mVer=0;
	m_preOnTime=-1;
	mSensorDev=e_CAMERA_MAIN_SENSOR;
}
FlashSimpleDrv::~FlashSimpleDrv()
{
}
FlashSimpleDrv* FlashSimpleDrv::getInstance()
{
	static FlashSimpleDrv obj;
	return &obj;
}
int FlashSimpleDrv::init(unsigned long sensorDev)
{
	m_fdSTROBE = open(STROBE_DEV_NAME, O_RDWR);
	mVer = ioctlEx(m_fdSTROBE,FLASH_IOC_GET_PROTOCOL_VERSION, mSensorDev, 0);
	//mVer=0;

	int value;
	int err;
	if(mVer==1)
	{

	    err = ioctlRetEx(m_fdSTROBE,FLASH_IOC_GET_MAIN_PART_ID, mSensorDev, &value);
	    err = ioctlRetEx(m_fdSTROBE,FLASH_IOC_GET_SUB_PART_ID, mSensorDev, &value);
	    err = ioctlEx(m_fdSTROBE,FLASHLIGHTIOC_X_SET_DRIVER, mSensorDev, sensorDev);
	}
	else
	{
    //	ioctl(m_fdSTROBE,FLASH_IOC_GET_MAIN_PART_ID,&value);
    	//ioctl(m_fdSTROBE,FLASH_IOC_GET_SUB_PART_ID,&value);
	ioctl(m_fdSTROBE,FLASHLIGHTIOC_X_SET_DRIVER,sensorDev);
    }
	return 0;
}
int FlashSimpleDrv::setOnOff(int a_isOn)
{
	ALOGD("setOnOff ln=%d isOn=%d kerVer=%d", __LINE__,a_isOn, mVer);
	int err;
	if(mVer==1)
    {
        err = ioctlEx(m_fdSTROBE,FLASH_IOC_SET_ONOFF, mSensorDev, a_isOn);
    }
    else
    {
	ioctl(m_fdSTROBE,FLASH_IOC_SET_TIME_OUT_TIME_MS,0);
	if(a_isOn==0)
	{
		//ALOGD("setOnOff ln=%d", __LINE__);
		ioctl(m_fdSTROBE,FLASH_IOC_SET_ONOFF,0);
		m_preOnTime=-1;
	}
	else
	{
		//ALOGD("setOnOff ln=%d", __LINE__);
		int err;
		int minPreOnTime;
		err = getPreOnTimeMs(&minPreOnTime);
		if(err<0)
		{
			//ALOGD("setOnOff ln=%d", __LINE__);

		}
		else
		{
			//ALOGD("setOnOff ln=%d m_preOnTime=%d", __LINE__,m_preOnTime);
			if(m_preOnTime==-1)
			{
				//ALOGD("setOnOff ln=%d", __LINE__);
				setPreOn();
				usleep(minPreOnTime*1000);
			}
			else
			{
				//ALOGD("setOnOff ln=%d", __LINE__);
				int curTime;
				int sleepTimeMs;
				curTime = getMs();
				sleepTimeMs = (minPreOnTime-(curTime-m_preOnTime));
				if(sleepTimeMs>0)
				{
					//ALOGD("setOnOff ln=%d", __LINE__);
					usleep( sleepTimeMs*1000);
				}
			}
		}
		//ALOGD("setOnOff ln=%d", __LINE__);
		ioctl(m_fdSTROBE,FLASH_IOC_SET_ONOFF,1);
		//ALOGD("setOnOff ln=%d", __LINE__);
	}
	//ALOGD("setOnOff ln=%d", __LINE__);
    }
	return 0;
}
int FlashSimpleDrv::uninit()
{
    setOnOff(0);
	close(m_fdSTROBE);
	return 0;
}


int FlashSimpleDrv::getPreOnTimeMs(int* ms)
{
	int err=0;
	*ms=0;
	if(mVer==1)
	    ;
	else
	err = ioctl(m_fdSTROBE,FLASH_IOC_GET_PRE_ON_TIME_MS, ms);
	return err;
}

int FlashSimpleDrv::setPreOn()
{
	int err = 0;
	if(mVer==1)
	    ;
    else
	err = ioctl(m_fdSTROBE,FLASH_IOC_PRE_ON,0);
	m_preOnTime=getMs();
	return err;
}


