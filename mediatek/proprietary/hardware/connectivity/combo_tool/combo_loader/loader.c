
#include "loader.h"
#include "loader_pwr.h"
//#include <syslog.h>
#include <private/android_filesystem_config.h>
#include <utils/Log.h>
#include <string.h>

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "wmt_loader"

#define WCN_COMBO_LOADER_CHIP_ID_PROP    	"persist.mtk.wcn.combo.chipid"
#define WCN_DRIVER_READY_PROP                    "service.wcn.driver.ready"
#define WCN_COMBO_LOADER_DEV				"/dev/wmtdetect"
#define WCN_COMBO_DEF_CHIPID				"0x6582"
#define WMT_MODULES_PRE						"/system/lib/modules/"
#define WMT_MODULES_SUFF					".ko"
#define WMT_IOC_MAGIC        				'w'
#define COMBO_IOCTL_GET_CHIP_ID       _IOR(WMT_IOC_MAGIC, 0, int)
#define COMBO_IOCTL_SET_CHIP_ID       _IOW(WMT_IOC_MAGIC, 1, int)
#define COMBO_IOCTL_EXT_CHIP_DETECT   _IOR(WMT_IOC_MAGIC, 2, int)
#define COMBO_IOCTL_GET_SOC_CHIP_ID   _IOR(WMT_IOC_MAGIC, 3, int)
#define COMBO_IOCTL_DO_MODULE_INIT    _IOR(WMT_IOC_MAGIC, 4, int)
#define COMBO_IOCTL_MODULE_CLEANUP    _IOR(WMT_IOC_MAGIC, 5, int)
#define COMBO_IOCTL_EXT_CHIP_PWR_ON   _IOR(WMT_IOC_MAGIC, 6, int)
#define COMBO_IOCTL_EXT_CHIP_PWR_OFF  _IOR(WMT_IOC_MAGIC, 7, int)
#define COMBO_IOCTL_DO_SDIO_AUDOK     _IOR(WMT_IOC_MAGIC, 8, int)



#define STP_WMT_MODULE_PRE_FIX "mtk_stp_wmt"
#define STP_BT_MODULE_PRE_FIX "mtk_stp_bt"
#define STP_GPS_MODULE_PRE_FIX "mtk_stp_gps"
#define HIF_SDIO_MODULE_PRE_FIX "mtk_hif_sdio"
#define STP_SDIO_MODULE_PRE_FIX "mtk_stp_sdio"
#define STP_UART_MODULE_PRE_FIX "mtk_stp_uart"



static int gLoaderFd = -1;

static char DRIVER_MODULE_PATH[64]  = {0};
static char DRIVER_MODULE_ARG[8] = "";
static int chipid_array[] = {
0x6620,
0x6628,
0x6630,
0x6572,
0x6582,
0x6592,
0x8127,
0x6571,
0x6752,
0x6735,
0x0321,
0x0335,
0x0337,
0x8163,
0x6580,
0x6755,
0x0326,
0x6797,
0x0279
};
static char chip_version[PROPERTY_VALUE_MAX] = {0};

static int g_remove_ko_flag = 1;


extern int init_module(void *, unsigned long, const char *);
extern int delete_module(const char *, unsigned int);
extern int load_fm_module(int chip_id);
extern int load_wifi_module(int chip_id);
extern int load_ant_module(int chip_id);
//insmod
static int insmod(const char *filename, const char *args)
{
    void *module;
    unsigned int size;
    int ret = -1;
	int retry = 10;

	ALOGI("filename(%s)\n",filename);

    module = load_file(filename, &size);
    if (!module)
    {
    	ALOGI("load file fail\n");
        return -1;
    }

	while(retry-- > 0){
	    ret = init_module(module, size, args);

		if(ret < 0)
		{
			ALOGI("insmod module fail(%d)\n",ret);
			usleep(30000);
		}
		else
			break;

	}

    free(module);

    return ret;
}
static int is_chipId_vaild(int chipid)
{
	int iret;
	unsigned char i;
	iret = -1;
	
	for(i = 0;i < sizeof(chipid_array)/sizeof(0x6630); i++){
		if(chipid == chipid_array[i]){
			ALOGI("is_chipId_vaild: %d :0x%x!\n",i,chipid);
			iret = 0;
			break;
		}
	}
	return iret;
}
static int rmmod(const char *modname)
{
    int ret = -1;
    int maxtry = 10;

    while (maxtry-- > 0) {
        ret = delete_module(modname, O_EXCL);//O_NONBLOCK | O_EXCL);
        if (ret < 0 && errno == EAGAIN)
            usleep(500000);
        else
            break;
    }

    if (ret != 0)
        ALOGI("Unable to unload driver module \"%s\": %s,ret(%d)\n",
             modname, strerror(errno),ret);
    return ret;
}

static int insmod_by_path (char *nameBuf, char * modulePath, char *preFix, char *postFix )
{
	int iRet = -1;
	int len = 0;
	int path_len = 0;

	/*no need to check, upper layer API will makesure this condition fullfill*/
	strcat (nameBuf, modulePath);
	strcat (nameBuf, preFix);
	strcat (nameBuf, postFix);
	strcat (nameBuf, WMT_MODULES_SUFF);

	insmod_retry:
	iRet = insmod(nameBuf, DRIVER_MODULE_ARG);
	if(iRet)
	{
		ALOGI("insert <%s> failed, len(%d), iret(%d), retrying\n", nameBuf, sizeof(nameBuf), iRet);
		/*break;*/
		usleep(800000);
		goto insmod_retry;
	}else
	{
		ALOGI("insert <%s> succeed,len(%d)\n", nameBuf, len);
		iRet = 0;
	}
	return 0;
}


static int insert_wmt_module_for_soc(int chipid, char *modulePath, char *nameBuf, int nameBufLen)
{
	int iRet = -1;
	int len = 0;
	int path_len = 0;
	int i = 0;
	char postFixStr[10] = {0};
	int totalLen = 0;
	char *soc_modulse[] = {
		STP_WMT_MODULE_PRE_FIX,
		STP_BT_MODULE_PRE_FIX,
		STP_GPS_MODULE_PRE_FIX,
	};

#if 0
	path_len = strlen(modulePath);
	strncpy(nameBuf, modulePath,path_len);
	ALOGI("module subpath1(%s),sublen1(%d)\n",nameBuf,path_len);
	len = path_len;
#endif

	sprintf(postFixStr, "_%s", "soc");

#if 0
	switch (chipid)
	{
		case 0x6572:
		case 0x6582:
			strcpy(postFixStr, "_6582");
			break;
		case 0x6571:
			strcpy(postFixStr, "_6592");
		case 0x6592:
			strcpy(postFixStr, "_6592");
			break;
		default:

			break;
	}
#endif

	if (NULL == modulePath || NULL == nameBuf || 0 >= nameBufLen)
	{
		ALOGI("invalid parameter:modulePath(%p), nameBuf(%p), nameBufLen(%d)\n", modulePath, nameBuf, nameBufLen);
		return iRet;
	}

	for(i = 0;i < sizeof(soc_modulse)/sizeof(soc_modulse[0]);i++)
	{
		totalLen = sizeof (modulePath) + sizeof (soc_modulse[i]) + sizeof(postFixStr) + sizeof(WMT_MODULES_SUFF);
		if (nameBufLen > totalLen)
		{

			memset (nameBuf, 0, nameBufLen);
			insmod_by_path(nameBuf, modulePath, soc_modulse[i], postFixStr);
		}
		else
		{
			ALOGI("nameBuf length(%d) too short, (%d) needed\n", nameBufLen, totalLen);
		}
#if 0
		len = path_len;
		len += sprintf(nameBuf + len,"%s",soc_modulse[i]);
		ALOGI("module subpath2(%s),sublen2(%d)\n",nameBuf,len);

		len += sprintf(nameBuf + len,"%s", postFixStr);
		ALOGI("module subpath3(%s),sublen3(%d)\n",nameBuf,len);

		len += sprintf(nameBuf + len,"%s",WMT_MODULES_SUFF);
		ALOGI("module subpath4(%s),sublen4(%d)\n",nameBuf,len);

		nameBuf[len] = '\0';
		ALOGI("module subpath5(%s),sublen5(%d)\n",nameBuf,len);

		soc_retry:
		iRet = insmod(nameBuf, DRIVER_MODULE_ARG);
		if(iRet)
		{
			ALOGI("(%d):current modules(%s) insert fail,len(%d),iret(%d), retrying\n", i, nameBuf, len, iRet);
			/*break;*/
			usleep(300000);
			goto soc_retry;
		}else
		{
			ALOGI("(%d):current modules(%s) insert ok,len(%d)\n", i, nameBuf, len);
		}
#endif

	}



	return 0;
}

static int insert_wmt_module_for_combo(int chipid, char *modulePath, char *nameBuf, int nameBufLen)
{
	int iRet = -1;
	int len = 0;
	int path_len = 0;
	int i = 0;
	char postFixStr[10] = {0};
	int totalLen = 0;

	char *combo_modulse[] = {
		HIF_SDIO_MODULE_PRE_FIX,
		STP_WMT_MODULE_PRE_FIX,
		STP_UART_MODULE_PRE_FIX,
		STP_SDIO_MODULE_PRE_FIX,
		STP_BT_MODULE_PRE_FIX,
		STP_GPS_MODULE_PRE_FIX
	};

	if (NULL == modulePath || NULL == nameBuf || 0 >= nameBufLen)
	{
		ALOGI("invalid parameter:modulePath(%p), nameBuf(%p), nameBufLen(%d)\n", modulePath, nameBuf, nameBufLen);
		return iRet;
	}

#if 0
	path_len = strlen(modulePath);
	strncpy(nameBuf, modulePath,path_len);
	ALOGI("module subpath1(%s),sublen1(%d)\n",nameBuf,path_len);

	len = path_len;
#endif

	switch (chipid)
	{
		case 0x6620:
		case 0x6628:
			/*strcpy(postFixStr, "_6620_28");*/
			strcpy(postFixStr, "");
			break;
		case 0x6630:
			//strcpy(postFixStr, "_6630");
			strcpy(postFixStr, "");
			break;
		default:

			break;
	}

	for(i = 0;i < sizeof(combo_modulse)/sizeof(combo_modulse[0]);i++)
	{
		totalLen = sizeof (modulePath) + sizeof (combo_modulse[i]) + sizeof(postFixStr) + sizeof(WMT_MODULES_SUFF);
		if (nameBufLen > totalLen)
		{
			memset (nameBuf, 0, nameBufLen);
			insmod_by_path(nameBuf, modulePath, combo_modulse[i], postFixStr);
		}
		else
		{
			ALOGI("nameBuf length(%d) too short, (%d) needed\n", nameBufLen, totalLen);
		}
#if 0
		len = path_len;
		len += sprintf(nameBuf + len,"%s",combo_modulse[i]);
		ALOGI("module subpath2(%s),sublen2(%d)\n",nameBuf,len);

		len += sprintf(nameBuf + len,"%s",postFixStr);
		ALOGI("module subpath3(%s),sublen3(%d)\n",nameBuf,len);

		len += sprintf(nameBuf + len,"%s",WMT_MODULES_SUFF);
		ALOGI("module subpath4(%s),sublen4(%d)\n",nameBuf,len);

		nameBuf[len] = '\0';
		ALOGI("module subpath5(%s),sublen5(%d)\n",nameBuf,len);

		combo_retry:
		iRet = insmod(nameBuf, DRIVER_MODULE_ARG);
		if(iRet)
		{
			ALOGI("(%d):current modules(%s) insert fail,len(%d),iret(%d), retrying\n", i, nameBuf, len, iRet);
			/*break;*/
			usleep(300000);
			goto combo_retry;
		}else
		{
			ALOGI("(%d):current modules(%s) insert ok,len(%d)\n",i, nameBuf, len);
		}
#endif
	}


	return 0;
}



/******************************************************
arg1:
= 0:there is already a valid chipid in peroperty or there is no external combo chip
	chipid is just 	MT6582
> 0:there is no valid chipid in peroperty, boot system firstly

arg2: // handle combo chip (there is an external combo chip)
= 0:insert mtk_hif_sdio.ko for detech combo chipid
> 0:insert combo modules except mtk_hif_sdio.ko
******************************************************/

static int insert_wmt_modules(int chipid,int arg1,int arg2)
{
	int iRet = -1;

	switch (chipid)
	{
		case 0x6582:
		case 0x6572:
		case 0x6571:
		case 0x6592:
		case 0x8127:
		case 0x6752:
		case 0x6735:
		case 0x8163:
		case 0x6580:
        case 0x6755:
        case 0x6797:
			iRet = insert_wmt_module_for_soc(chipid, WMT_MODULES_PRE, DRIVER_MODULE_PATH, sizeof (DRIVER_MODULE_PATH));
			break;
		case 0x6620:
		case 0x6628:
		case 0x6630:
			iRet = insert_wmt_module_for_combo(chipid, WMT_MODULES_PRE, DRIVER_MODULE_PATH, sizeof (DRIVER_MODULE_PATH));
			break;
		default:
			break;
	}

	return iRet;
}

int do_kernel_module_init(int gLoaderFd, int chipId)
{
	int iRet = 0;
	if (gLoaderFd < 0)
	{
		ALOGI("invalid gLoaderFd: %d\n", gLoaderFd);
		return -1;
	}

	iRet = ioctl (gLoaderFd, COMBO_IOCTL_MODULE_CLEANUP, chipId);
	if (iRet)
	{
		ALOGI("do WMT-DETECT module cleanup failed: %d\n", iRet);
		return -2;
	}
 	iRet = ioctl (gLoaderFd, COMBO_IOCTL_DO_MODULE_INIT, chipId);
	if (iRet)
	{
		ALOGI("do kernel module init failed: %d\n", iRet);
		return -3;
	}
	ALOGI("do kernel module init succeed: %d\n", iRet);
	return 0;
}


int main(int argc, char *argv[])
{
	int iRet = -1;
	int noextChip = -1;
	int chipId = -1;
	int count = 0;
	char chipidStr[PROPERTY_VALUE_MAX] = {0};
        char readyStr[PROPERTY_VALUE_MAX] = {0};
	int loadFmResult = -1;
	int loadAntResult = -1;
	int loadWlanResult = -1;
	int retryCounter = 1;
	int autokRet = 0;
	do{
		gLoaderFd = open(WCN_COMBO_LOADER_DEV, O_RDWR | O_NOCTTY);
		if(gLoaderFd < 0)
		{
			count ++;
			ALOGI("Can't open device node(%s) count(%d)\n", WCN_COMBO_LOADER_DEV,count);
			usleep(300000);
		}
		else
			break;
	}while(1);

	//read from system property
	iRet = property_get(WCN_COMBO_LOADER_CHIP_ID_PROP, chipidStr, NULL);
	chipId = strtoul(chipidStr, NULL, 16);

	if ((0 != iRet) && (-1 != is_chipId_vaild(chipId)))
	{
		/*valid chipid detected*/
		ALOGI("key:(%s)-value:(%s),chipId:0x%04x,iRet(%d)\n", WCN_COMBO_LOADER_CHIP_ID_PROP, chipidStr, chipId,iRet);
		if (0x6630 == chipId)
		{
			retryCounter = 10;
			/*trigger autok process, incase last autok process is interrupted by abnormal power off or battery down*/
			do {
				/*power on combo chip*/
				iRet = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_PWR_ON);
				if (0 != iRet)
				{
					ALOGI("external combo chip power on failed\n");
					noextChip = 1;
				}
				else
				{
					/*detect is there is an external combo chip*/
					noextChip = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_DETECT,NULL);
				}

				if(noextChip)
				{
					 // do nothing
					 ALOGI("no external combo chip detected\n");
				}
				else
				{
					ALOGI("external combo chip detected\n");

					chipId = ioctl(gLoaderFd, COMBO_IOCTL_GET_CHIP_ID, NULL);
					ALOGI("chipid (0x%x) detected\n", chipId);
				}


				if(0 == noextChip)
				{
					autokRet = ioctl(gLoaderFd,COMBO_IOCTL_DO_SDIO_AUDOK,chipId);
					if (0 != autokRet)
					{
						ALOGI("do SDIO3.0 autok failed\n");
					}
					else
					{
						ALOGI("do SDIO3.0 autok succeed\n");
					}
				}
				iRet = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_PWR_OFF);
				if (0 != iRet)
				{
					ALOGI("external combo chip power off failed\n");
				}
				else
				{
					ALOGI("external combo chip power off succeed\n");
				}
				if ((0 == noextChip) && (-1 == chipId))
				{
					/*extenral chip detected, but no valid chipId detected, retry*/
					retryCounter--;
					ALOGI("chipId detect failed, retrying, left retryCounter:%d\n", retryCounter);
					usleep(500000);
				}
				else
					break;
			}while (0 < retryCounter);
			chipId = 0x6630;
		}
	}
	else
	{
		/*trigger external combo chip detect and chip identification process*/
		do {
			/*power on combo chip*/
			iRet = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_PWR_ON);
			if (0 != iRet)
			{
				ALOGI("external combo chip power on failed\n");
				noextChip = 1;
			}
			else
			{
				/*detect is there is an external combo chip*/
				noextChip = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_DETECT,NULL);
			}

			if(noextChip)// use soc itself
			{
				ALOGI("no external combo chip detected, get current soc chipid\n");
				chipId = ioctl(gLoaderFd, COMBO_IOCTL_GET_SOC_CHIP_ID, NULL);
				ALOGI("soc chipid (0x%x) detected\n", chipId);
			}
			else
			{
				ALOGI("external combo chip detected\n");

				chipId = ioctl(gLoaderFd, COMBO_IOCTL_GET_CHIP_ID, NULL);
				ALOGI("chipid (0x%x) detected\n", chipId);
			}

			sprintf (chipidStr, "0x%04x", chipId);
			iRet = property_set(WCN_COMBO_LOADER_CHIP_ID_PROP,chipidStr);
			if (0 != iRet)
			{
				ALOGI("set property(%s) to %s failed,iRet:%d, errno:%d\n", WCN_COMBO_LOADER_CHIP_ID_PROP, chipidStr, iRet, errno);
			}
			else
			{
				ALOGI("set property(%s) to %s succeed.\n", WCN_COMBO_LOADER_CHIP_ID_PROP, chipidStr);
			}
			if(0 == noextChip)
			{
				autokRet = ioctl(gLoaderFd,COMBO_IOCTL_DO_SDIO_AUDOK,chipId);
				if (0 != autokRet)
				{
					ALOGI("do SDIO3.0 autok failed\n");
				}
				else
				{
					ALOGI("do SDIO3.0 autok succeed\n");
				}
			}
			iRet = ioctl(gLoaderFd,COMBO_IOCTL_EXT_CHIP_PWR_OFF);
			if (0 != iRet)
			{
				ALOGI("external combo chip power off failed\n");
			}
			else
			{
				ALOGI("external combo chip power off succeed\n");
			}
			if ((0 == noextChip) && (-1 == chipId))
			{
			    /*extenral chip detected, but no valid chipId detected, retry*/
			    retryCounter--;
			    usleep(500000);
			    ALOGI("chipId detect failed, retrying, left retryCounter:%d\n", retryCounter);
			}
			else
			    break;
		}while (0 < retryCounter);
	}

	/*set chipid to kernel*/
	ioctl(gLoaderFd,COMBO_IOCTL_SET_CHIP_ID,chipId);

	if (g_remove_ko_flag)
	{
		if((0x0321 == chipId) || (0x0335 == chipId) || (0x0337 == chipId))
		{
			chipId = 0x6735;
		}
        if (0x0326 == chipId) {
            chipId = 0x6755;
        }
        if (0x0279 == chipId) {
            chipId = 0x6797;
        }
		do_kernel_module_init(gLoaderFd, chipId);
		if(gLoaderFd >= 0)
		{
			close(gLoaderFd);
			gLoaderFd = -1;
		}

	}
	else
	{
		if(gLoaderFd >= 0)
		{
			close(gLoaderFd);
			gLoaderFd = -1;
		}
		ALOGI("rmmod mtk_wmt_detect\n");
		rmmod("mtk_wmt_detect");

		/*INSERT TARGET MODULE TO KERNEL*/

		iRet = insert_wmt_modules(chipId, 0, -1);
		/*this process should never fail*/
		if(iRet)
		{
			ALOGI("insert wmt modules fail(%d):(%d)\n",iRet,__LINE__);
			/*goto done;*/
		}


		loadFmResult = load_fm_module(chipId);
		if(loadFmResult)
		{
			ALOGI("load FM modules fail(%d):(%d)\n",iRet,__LINE__);
			/*continue, we cannot let this process interrupted by subsystem module load fail*/
			/*goto done;*/
		}

		loadAntResult = load_ant_module(chipId);
		if(loadAntResult)
		{
			ALOGI("load ANT modules fail(%d):(%d)\n",iRet,__LINE__);
			/*continue, we cannot let this process interrupted by subsystem module load fail*/
			/*goto done;*/
		}

		loadWlanResult = load_wifi_module(chipId);
		if(loadWlanResult)
		{
			ALOGI("load WIFI modules fail(%d):(%d)\n",iRet,__LINE__);
			/*continue, we cannot let this process interrupted by subsystem module load fail*/
			/*goto done;*/
		}
	}



	if((chown("/proc/driver/wmt_dbg",AID_SHELL,AID_SYSTEM) == -1) || (chown("/proc/driver/wmt_aee",AID_SHELL,AID_SYSTEM) == -1))
	{
		ALOGI("chown wmt_dbg or wmt_aee fail:%s\n",strerror(errno));
	}

	if(chown("/proc/wmt_tm/wmt_tm",0,1000) == -1)
	{
		ALOGI("chown wmt_tm fail:%s\n",strerror(errno));
	}
	if (0/*0x6630 == chipId*/)
	{
		retryCounter = 0;
		int i_ret = -1;
		do {
			i_ret = loader_wmt_pwr_ctrl(1);
			if (0 == i_ret)
				break;
			else
			{
				loader_wmt_pwr_ctrl(0);
				ALOGI("power on %x failed, retrying, retry counter:%d\n", chipId, retryCounter);
				usleep(1000000);
			}
			retryCounter++;
		} while (retryCounter < 20);
        }
        iRet = property_get(WCN_DRIVER_READY_PROP, readyStr, NULL);
        if ((0 >= iRet) || (0 == strcmp(readyStr, "yes"))) {
                ALOGI("get property(%s) failed iRet:%d or property is %s\n", WCN_DRIVER_READY_PROP, iRet, readyStr);
        }
        /*set it to yes anyway*/
        sprintf(readyStr, "%s", "yes");
        iRet = property_set(WCN_DRIVER_READY_PROP, readyStr);
        if (0 != iRet) {
                ALOGI("set property(%s) to %s failed iRet:%d\n", WCN_DRIVER_READY_PROP, readyStr, iRet);
        } else {
                ALOGI("set property(%s) to %s succeed\n", WCN_DRIVER_READY_PROP, readyStr);
	}
#if 0
	while (loadWlanResult || loadFmResult)
	{
		if(loadFmResult)
		{
			static int retryCounter = 0;
			retryCounter++;
			ALOGI("retry loading fm module, retryCounter:%d\n", retryCounter);
			loadFmResult = load_fm_module(chipId);
		}

		if(loadWlanResult)
		{
			static int retryCounter = 0;
			retryCounter++;
			ALOGI("retry loading wlan module, retryCounter:%d\n", retryCounter);
			loadWlanResult = load_wifi_module(chipId);
		}
		usleep(1000000);
	}
#endif

	return iRet;
}



