/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */



#define MTK_LOG_ENABLE 1
#include "googleota_binder.h"
#include <cutils/properties.h>
#include <fs_mgr.h>


#define START_BLOCK 0
#define BLOCK_SIZE 0x20000
#define OTA_RESULT_OFFSET    (2560)
#define FSTAB_PREFIX    "/fstab."


GoogleOtaAgent::GoogleOtaAgent()
{
    ALOGI("GoogleOtaAgent created");
}


void GoogleOtaAgent::instantiate()
{
    while(true)
	{
        status_t ret = defaultServiceManager()->addService(
            descriptor, new GoogleOtaAgent());
        if(ret == OK)
		{
            ALOGI("[GoogleOtaAgent]register OK.");
            break;
        }
		ALOGE("[GoogleOtaAgent]register FAILED. retrying in 5sec.");

        
        sleep(5); 
    } 
}

status_t BnGoogleOtaAgent::onTransact(uint32_t code,
			       const Parcel &data,
			       Parcel *reply,
			       uint32_t flags) 
{

    ALOGI("OnTransact   (%u,%u)", code, flags);
        
    switch(code) {

    case TRANSACTION_SET_REBOOT_FLAG:
		{
			ALOGI("setRebootFlag\n");
			data.enforceInterface (descriptor);
			reply->writeInt32 (setRebootFlag());
			return NO_ERROR;
		}
	break;

	case TRANSACTION_CLEAR_UPGRADE_RESULT: 
		{
		    ALOGI("clearUpgradeResult\n");
			data.enforceInterface(descriptor);
			reply->writeInt32(clearUpgradeResult());
			return NO_ERROR;
		}
	break;

    case TRANSACTION_READ_UPGRADE_RESULT: 
		{
		    ALOGI("readOtaResult\n");
			data.enforceInterface(descriptor);
			reply->writeInt32(readUpgradeResult());
			return NO_ERROR;
		}
	break;

    default:
		return BBinder::onTransact(code, data, reply, flags);
    }

    return NO_ERROR;
}


int GoogleOtaAgent::setRebootFlag() 
{

    char cmd[] = "boot-recovery";
    return writeRebootFlash(cmd);
}



int GoogleOtaAgent::writeRebootFlash(char *rebootCmd)
{
    int fd;
    int iWriteSize = 512 ;
    int iRealWriteSize = 0;
    int result;

    struct fstab *fstab;
    struct fstab_rec *rec = NULL;
    char *source = NULL;

    char fstab_filename[PROPERTY_VALUE_MAX + sizeof(FSTAB_PREFIX)];
    char propbuf[PROPERTY_VALUE_MAX];

#ifdef MTK_EMMC_SUPPORT
    int bootEndBlock = 0;
    ALOGI("[REBOOT_FLAG]:MTK_EMMC_SUPPORT defined\r\n");
#else
    int bootEndBlock = 4096;///2048;
    ALOGI("[REBOOT_FLAG]:MTK_NAND_SUPPORT defined\r\n");
#endif
    char *tempBuf=NULL;

    struct mtd_info_user info;
    struct erase_info_user erase_info;
    char devName[64];
    memset(devName, '\0', sizeof(devName));

    property_get("ro.hardware", propbuf, "");
    snprintf(fstab_filename, sizeof(fstab_filename), FSTAB_PREFIX"%s", propbuf);

    fstab = fs_mgr_read_fstab(fstab_filename);
    if (!fstab) {
        ALOGE("[clearUpgradeResult]fail to open %s\n", fstab_filename);
        return 0;
    }

    rec = fs_mgr_get_entry_for_mount_point(fstab, "/misc");
    if (!rec) {
        ALOGE("[clearUpgradeResult]fail to get misc partition entry\n");
	 fs_mgr_free_fstab(fstab);
        return 0;
    }

    asprintf(&source, "%s", rec->blk_device);
    ALOGE("[clearUpgradeResult]partition entry is %s\n", source);

    fs_mgr_free_fstab(fstab);

    strcpy(devName, source);

    fd = open(devName, O_RDWR);
    if(fd<0)
    {
        ALOGE("[clearUpgradeResult]Can't open %s\n(%s)\n", devName, strerror(errno));
        return 0;
    }

#ifdef MTK_EMMC_SUPPORT
#else

    result=ioctl(fd,MEMGETINFO,&info);
    if(result<0)
    {
      ALOGE("[REBOOT_FLAG]:mtd get info error\r\n");
      return 0;
    }
    iWriteSize=info.writesize;
    bootEndBlock = iWriteSize;
    ALOGI("[REBOOT_FLAG]:bootEndBlock = %d\r\n",bootEndBlock);

    erase_info.start=__u64(START_BLOCK);
    erase_info.length = info.erasesize;
    ALOGI("[REBOOT_FLAG]:info.erasesize = %d\r\n",info.erasesize);
    result=ioctl(fd, MEMERASE, &erase_info);
    if(result<0)
    {
      ALOGE("[REBOOT_FLAG]:mtd erase error result = %d errorno = [%d] err =[%s] \r\n", result, errno, strerror(errno));
      close(fd);
      free(tempBuf);
      return 0;
    }
	  
    ALOGI("[REBOOT_FLAG]:end to earse\r\n");

#endif

    tempBuf=(char *)malloc(iWriteSize);
      
    if(tempBuf==NULL)
    {
      ALOGE("[REBOOT_FLAG]:malloc error\r\n");
      close(fd);
      free(tempBuf);
      return 0;
    }
    memset(tempBuf,0,iWriteSize);
 
    iRealWriteSize=strlen(rebootCmd);
    memcpy(tempBuf,rebootCmd, iRealWriteSize);
	  
    ALOGI("[REBOOT_FLAG]:start to write\r\n");

    result=lseek(fd,bootEndBlock,SEEK_SET);
    if(result!=(bootEndBlock))
    {
      ALOGE("[REBOOT_FLAG]:mtd first lseek error\r\n");
      close(fd);
      free(tempBuf);
      return 0;
    }

    result=write(fd,tempBuf,iWriteSize);
    if(result!=iWriteSize)
    {
      ALOGE("[REBOOT_FLAG]:mtd write error,iWriteSize:%d\r\n",iWriteSize);
      close(fd);
      free(tempBuf);
      return 0;
    }

    result=lseek(fd,bootEndBlock,SEEK_SET);
    if(result!=(bootEndBlock))
    {
      ALOGE("[REBOOT_FLAG]:mtd second lseek error\r\n");
      close(fd);
      free(tempBuf);
      return 0;
    }

    ALOGI("[REBOOT_FLAG]:end to write\r\n");
    //read from misc partition to make sure it is correct 
    char *readBuf = (char *)malloc(iRealWriteSize+1);
    memset(readBuf, 0, iRealWriteSize+1);
    result=read(fd,readBuf,iRealWriteSize);
    if(result!=iRealWriteSize)
    {
      ALOGE("[REBOOT_FLAG]:mtd read error\r\n");
      free(readBuf);
      close(fd);
      free(tempBuf);
      return 0;
    }
	  
    ALOGI("[REBOOT_FLAG]:end to read  readbuf = %s\r\n",readBuf);
    ALOGI("[REBOOT_FLAG]:end to read  tempBuf = %s\r\n",tempBuf);
    if(strcmp(readBuf, tempBuf) != 0)
    {   
      ALOGE("[REBOOT_FLAG]:mtd readed value error\r\n");
      free(readBuf);
      close(fd);
      free(tempBuf);
      return 0;
    }
    free(tempBuf);	  
    free(readBuf);
    close(fd);
    return 1;
}


int GoogleOtaAgent::clearUpgradeResult()
{
    int dev = -1;
    char dev_name[64];
    int count;
    int i = 0;
    struct fstab *fstab;
    struct fstab_rec *rec = NULL;
    char *source = NULL;

    char fstab_filename[PROPERTY_VALUE_MAX + sizeof(FSTAB_PREFIX)];
    char propbuf[PROPERTY_VALUE_MAX];

    property_get("ro.hardware", propbuf, "");
    snprintf(fstab_filename, sizeof(fstab_filename), FSTAB_PREFIX"%s", propbuf);

    fstab = fs_mgr_read_fstab(fstab_filename);
    if (!fstab) {
        ALOGE("[clearUpgradeResult]fail to open %s\n", fstab_filename);
        return 0;
    }

    rec = fs_mgr_get_entry_for_mount_point(fstab, "/misc");
    if (!rec) {
        ALOGE("[clearUpgradeResult]fail to get misc partition entry\n");
	 fs_mgr_free_fstab(fstab);
        return 0;
    }

    asprintf(&source, "%s", rec->blk_device);
    ALOGE("[clearUpgradeResult]partition entry is %s\n", source);

    fs_mgr_free_fstab(fstab);

    strcpy(dev_name, source);

    dev = open(dev_name, O_WRONLY);
    if(dev<0)
    {
        ALOGE("[clearUpgradeResult]Can't open %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1) {
        ALOGE("[clearUpgradeResult]Failed seeking %s\n(%s)\n", dev_name, strerror(errno));
        close(dev);
        return 0;
    }

    count = write(dev, &i, sizeof(i));
    if (count != sizeof(i)) {
        ALOGE("[clearUpgradeResult]Failed writing %s\n(%s)\n", dev_name, strerror(errno));
        close(dev);
        return 0;
    }
    if (close(dev) != 0) {
        ALOGE("[clearUpgradeResult]Failed closing %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    return 1;
}

int GoogleOtaAgent::readUpgradeResult()
{
    int dev = -1;
    char dev_name[64];
    int count;
    int result = 1;

    struct fstab *fstab;
    struct fstab_rec *rec = NULL;
    char *source = NULL;

    char fstab_filename[PROPERTY_VALUE_MAX + sizeof(FSTAB_PREFIX)];
    char propbuf[PROPERTY_VALUE_MAX];

    property_get("ro.hardware", propbuf, "");
    snprintf(fstab_filename, sizeof(fstab_filename), FSTAB_PREFIX"%s", propbuf);

    fstab = fs_mgr_read_fstab(fstab_filename);
    if (!fstab) {
        ALOGE("[clearUpgradeResult]fail to open %s\n", fstab_filename);
        return 0;
    }

    rec = fs_mgr_get_entry_for_mount_point(fstab, "/misc");
    if (!rec) {
        ALOGE("[clearUpgradeResult]fail to get misc partition entry\n");
	 fs_mgr_free_fstab(fstab);
        return 0;
    }

    asprintf(&source, "%s", rec->blk_device);
    ALOGE("[clearUpgradeResult]partition entry is %s\n", source);

    fs_mgr_free_fstab(fstab);

    strcpy(dev_name, source);

    dev = open(dev_name, O_RDONLY);
    if(dev<0)
    {
        ALOGE("[clearUpgradeResult]Can't open %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1)
	{
		ALOGE("[readUpgradeResult]:Failed seeking %s\n(%s)\n",dev_name, strerror(errno));

        close(dev);
        return 0;
    }

    count = read(dev, &result, sizeof(result));
    if (count != sizeof(result))
	{
		ALOGE("[readUpgradeResult]:Failed reading %s\n(%s)\n",dev_name, strerror(errno));
        close(dev);
        return 0;
    }

    if (close(dev) != 0)
	{
		ALOGE("[readUpgradeResult]:Failed closing %s\n(%s)\n",dev_name, strerror(errno));

		return 0;
    }
	ALOGI("ota_result=%d\n", result);

    return result;

}

int main(int argc, char *argv[])
{

    GoogleOtaAgent::instantiate();
    ProcessState::self()->startThreadPool();

    ALOGI("GoogleOtaAgent Service is now ready");

    IPCThreadState::self()->joinThreadPool();
    return(0);
}

