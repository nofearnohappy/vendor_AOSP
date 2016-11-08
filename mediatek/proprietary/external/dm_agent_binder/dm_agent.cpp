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
#include "dm_agent.h"
#include <cutils/properties.h>
#include <sys/types.h>
#include <sys/stat.h>

#define DM_ROOT_PATH "/data/nvram/dm/"
#define DM_TREE_PATH DM_ROOT_PATH"tree"
#define DM_TREE_PATH_BACKUP DM_ROOT_PATH"tree~"
#define DM_LOCK_PATH DM_ROOT_PATH"lock"
#define DM_WIPE_PATH DM_ROOT_PATH"wipe"
#define DM_IMSI_PATH DM_ROOT_PATH"imsi"
#define DM_OPERATOR_PATH DM_ROOT_PATH"operator"
#define DM_SWITCH_PATH DM_ROOT_PATH"register_switch"
#define RECOVERY_COMMAND "/cache/recovery/command"
#define RECOVERY_FOLDER "/cache/recovery"

#ifdef MTK_GPT_SCHEME_SUPPORT
#define MISC_PATH "/dev/block/platform/mtk-msdc.0/by-name/para"
#else
#define MISC_PATH "/dev/misc"
#endif

#define DM_CONF_SWITCH_PATH DM_ROOT_PATH"switch"
#define DM_DM_SWITCH_PATH DM_ROOT_PATH"dmswitch"
#define DM_SMSREG_SWITCH_PATH DM_ROOT_PATH"smsregswitch"

//added for CT-Reg
#define REGISTER_FLAG_PATH DM_ROOT_PATH"register_flag"
#define REGISTER_IMSI1_PATH DM_ROOT_PATH"imsi1"
#define REGISTER_IMSI2_PATH DM_ROOT_PATH"imsi2"

// Add for CT_4G_REG
#define WIFI_MAC_PATH "/data/nvram/APCFG/APRDEB/WIFI"
#define MAC_ADDRESS_DIGITS 6
#define REGISTER_ICCID1_PATH DM_ROOT_PATH"iccID1"
#define REGISTER_ICCID2_PATH DM_ROOT_PATH"iccID2"
#define SELF_REGISTER_FLAG_PATH DM_ROOT_PATH"self_reg_flag"

#define MAX_FILE_SIZE 300*1024

#define DM_READ_NO_EXCEPTION 0

#define OPERATOR_LEN 10
//#define START_BLOCK 0x10C0000
#define START_BLOCK 0
#define DM_BLOCK_SIZE 0x20000
//#define BOOT_PARTITION 7
//#define UPGRADE_PARTITION 1

#define OTA_RESULT_OFFSET    (2560)

//for eMMC ONLY.
int get_ota_result(int *i) {

	int dev = -1;
	char dev_name[64];
	int count;
	int result;

	strcpy(dev_name, MISC_PATH);

	dev = open(dev_name, O_RDONLY);
	if (dev < 0) {
		printf("Can't open %s\n(%s)\n", dev_name, strerror(errno));
		return -1;
	}

	if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1) {
		printf("Failed seeking %s\n(%s)\n", dev_name, strerror(errno));
		close(dev);
		return -1;
	}

	count = read(dev, &result, sizeof(result));
	if (count != sizeof(result)) {
		printf("Failed reading %s\n(%s)\n", dev_name, strerror(errno));
    close(dev);
		return -1;
	}
	if (close(dev) != 0) {
		printf("Failed closing %s\n(%s)\n", dev_name, strerror(errno));
		return -1;
	}

	*i = result;
	return 0;
}

int file_exist(const char * path) {
	struct stat lock_stat;
	bzero(&lock_stat, sizeof(lock_stat));
	int ret = stat(path, &lock_stat);
	return ret + 1; // 1 if exists,0 if not
}

void DmAgent::instantiate() {
	while (true) {
		DmAgent *agent = new DmAgent();
		status_t ret = defaultServiceManager()->addService(descriptor, agent);
		if (ret == OK) {
			ALOGI("[DmAgent]register OK.");
			break;
		}

		ALOGD("[DmAgent]register FAILED. retrying in 5sec.");

		sleep(5);
	}
}

DmAgent::DmAgent() {
	ALOGI("DmAgent created");
}

status_t BnDmAgent::onTransact(uint32_t code, const Parcel &data,
		Parcel *reply, uint32_t flags) {

	ALOGI("OnTransact   (%u,%u)", code, flags);
	reply->writeInt32(DM_READ_NO_EXCEPTION);//used for readException

	switch (code) {
	case TRANSACTION_setLockFlag: {
		/*	ALOGI("setLockFlag\n");
		 data.enforceInterface (descriptor);
		 reply->writeInt32 (setLockFlag ());
		 // ALOGI("locked\n");
		 return NO_ERROR;
		 */
		ALOGI("setLockFlag\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setLockFlag len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setLockFlag buff  = %s\n", buff);
			reply->writeInt32(setLockFlag(buff, len));
		}
		ALOGI("setLockFlag done\n");
		return NO_ERROR;

	}
		break;
	case TRANSACTION_clearLockFlag: {
		ALOGI("clearLockFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearLockFlag());
		ALOGI("cleared\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readDmTree: {
		ALOGI("readDmTree\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readDmTree(size);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("DmTree read done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeDmTree: {
		ALOGI("writeDmTree\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			reply->writeInt32(writeDmTree(buff, len));
		}
		ALOGI("DmTree wrote\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isLockFlagSet: {
		ALOGI("isLockFlagSet\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isLockFlagSet());
		ALOGI("isLockFlagSet done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readImsi: {
		ALOGI("readImsi\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readImsi(size);
		ALOGD("readImsi = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("readImsi done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeImsi: {
		ALOGI("writeImsi\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("writeImsi len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("writeImsi buff  = %s\n", buff);
			reply->writeInt32(writeImsi(buff, len));
		}
		ALOGI("writeImsi done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getRegisterSwitch: {
		ALOGI("getRegisterSwitch\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = getRegisterSwitch(size);
		ALOGD("getRegisterSwitch = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("getRegisterSwitch done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setRegisterSwitch: {
		ALOGI("setRegisterSwitch\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setRegisterSwitch len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setRegisterSwitch buff  = %s\n", buff);
			reply->writeInt32(setRegisterSwitch(buff, len));
		}
		ALOGI("setRegisterSwitch done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readOperatorName: {
		ALOGI("readOperatorName\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readOperatorName(size);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("readOperatorName done\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_setRebootFlag: {
		ALOGI("setRebootFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(setRebootFlag());
		ALOGI("setRebootFlag done\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_getLockType: {
		ALOGI("getLockType\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getLockType());
		ALOGI("getLockType done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getOperatorId: {
		ALOGI("getOperatorId\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getOperatorId());
		ALOGI("getOperatorId done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getOperatorName: {
		ALOGI("getOperatorName\n");
		data.enforceInterface(descriptor);
		char * ret = getOperatorName();
		if (ret == NULL)
			reply->writeInt32(-1);
		else
			reply->writeInt32(0);
		ALOGI("getOperatorName done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isHangMoCallLocking: {
		ALOGI("isHangMoCallLocking\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isHangMoCallLocking());
		ALOGI("isHangMoCallLocking done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isHangMtCallLocking: {
		ALOGI("isHangMtCallLocking\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isHangMtCallLocking());
		ALOGI("isHangMtCallLocking\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_clearRebootFlag: {
		ALOGI("clearRebootFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearRebootFlag());
		ALOGI("clearRebootFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isBootRecoveryFlag: {
		ALOGI("isBootRecoveryFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isBootRecoveryFlag());
		ALOGI("isBootRecoveryFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isWipeSet: {
		ALOGI("isWipeset\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isWipeSet());
		ALOGI("isWipeset done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setWipeFlag: {
		ALOGI("setWipeFlag\n");
		data.enforceInterface(descriptor);
		//int len=data.readInt32 ();
		reply->writeInt32(setWipeFlag((char *)"FactoryReset", sizeof("FactoryReset")));
		ALOGI("setWipeFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_clearWipeFlag: {
		ALOGI("clearWipeFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearWipeFlag());
		ALOGI("clearWipeFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getUpgradeStatus: {
		ALOGI("getUpgradeStatus\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getUpgradeStatus());
		ALOGI("getUpgradeStatus done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_restartAndroid: {
		ALOGI("restartAndroid\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(restartAndroid());
		ALOGI("restartAndroid\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readOtaResult: {
		ALOGI("readOtaResult\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(readOtaResult());
		return NO_ERROR;
	}
		break;
	case TRANSACTION_clearOtaResult: {
		ALOGI("clearOtaResult\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearOtaResult());
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readRegisterFlag: {
		ALOGI("readRegisterFlag\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readRegisterFlag(size);
		ALOGD("readRegisterFlag = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("readRegisterFlag done\n");
		return NO_ERROR;
	}
	    break;
	case TRANSACTION_setRegisterFlag: {
		ALOGI("setRegisterFlag\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setRegisterFlag len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setRegisterFlag buff  = %s\n", buff);
			reply->writeInt32(setRegisterFlag(buff, len));
		}
		ALOGI("setRegisterFlag done\n");
		return NO_ERROR;
	}
		break;
case TRANSACTION_writeImsi1: {
		ALOGI("writeIMSI1\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("writeIMSI1 len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("writeIMSI1 buff  = %s\n", buff);
			reply->writeInt32(writeImsi1(buff, len));
		}
		ALOGI("writeIMSI1 done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeImsi2: {
		ALOGI("writeIMSI2\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("writeIMSI2 len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("writeIMSI2 buff  = %s\n", buff);
			reply->writeInt32(writeImsi2(buff, len));
		}
		ALOGI("writeIMSI2 done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readImsi1: {
		ALOGI("readIMSI1\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readImsi1(size);
		ALOGD("readIMSI1 = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("readIMSI1 done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readImsi2: {
		ALOGI("readIMSI2\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readImsi2(size);
		ALOGD("readIMSI2 = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("readIMSI2 done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getSwitchValue: {
		ALOGI("getSwitchValue\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = getSwitchValue(size);
		ALOGD("getSwitchValue = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("getSwitchValue done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setSwitchValue: {
		ALOGI("setSwitchValue\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setSwitchValue len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setSwitchValue buff  = %s\n", buff);
			reply->writeInt32(setSwitchValue(buff, len));
		}
		ALOGI("setSwitchValue done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getDmSwitchValue: {
		ALOGI("getDmSwitchValue\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = getDmSwitchValue(size);
		ALOGD("getDmSwitchValue = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("getDmSwitchValue done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setDmSwitchValue: {
		ALOGI("setDmSwitchValue\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setDmSwitchValue len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setDmSwitchValue buff  = %s\n", buff);
			reply->writeInt32(setDmSwitchValue(buff, len));
		}
		ALOGI("setDmSwitchValue done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getSmsRegSwitchValue: {
		ALOGI("getSmsRegSwitchValue\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = getSmsRegSwitchValue(size);
		ALOGD("getSmsRegSwitchValue = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		ALOGI("getSmsRegSwitchValue done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setSmsRegSwitchValue: {
		ALOGI("setSmsRegSwitchValue\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		ALOGD("setSmsRegSwitchValue len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			ALOGD("setSmsRegSwitchValue buff  = %s\n", buff);
			reply->writeInt32(setSmsRegSwitchValue(buff, len));
		}
		ALOGI("setSmsRegSwitchValue done\n");
		return NO_ERROR;
	}
		break;
    case TRANSACTION_writeIccID1: {
        ALOGI("writeIccID1\n");
        data.enforceInterface(descriptor);
        int len = data.readInt32();
        ALOGD("writeIccID1 len  = %d\n", len);
        if (len == -1) { // array is null
            reply->writeInt32(0);
        } else {
            char buff[len];
            data.read(buff, len);
            ALOGD("writeIccID1 buff  = %s\n", buff);
            reply->writeInt32(writeIccID1(buff, len));
        }
        ALOGI("writeIccID1 done\n");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_writeIccID2: {
        ALOGI("writeIccID2\n");
        data.enforceInterface(descriptor);
        int len = data.readInt32();
        ALOGD("writeIccID2 len  = %d\n", len);
        if (len == -1) { // array is null
            reply->writeInt32(0);
        } else {
            char buff[len];
            data.read(buff, len);
            ALOGD("writeIccID2 buff  = %s\n", buff);
            reply->writeInt32(writeIccID2(buff, len));
        }
        ALOGI("writeIccID2 done\n");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_readIccID1: {
        ALOGI("readIccID1\n");
        data.enforceInterface(descriptor);
        int size = 0;
        char * ret = readIccID1(size);
        ALOGD("readIccID1 = %s\n", ret);
        if (ret == NULL) {
            reply->writeInt32(-1);
        } else {
            reply->writeInt32(size);
            reply->write(ret, size);
            free(ret);
        }
        ALOGI("readIccID1 done\n");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_readIccID2: {
        ALOGI("readIccID2\n");
        data.enforceInterface(descriptor);
        int size = 0;
        char * ret = readIccID2(size);
        ALOGD("readIccID2 = %s\n", ret);
        if (ret == NULL) {
            reply->writeInt32(-1);
        } else {
            reply->writeInt32(size);
            reply->write(ret, size);
            free(ret);
        }
        ALOGI("readIccID2 done\n");
        return NO_ERROR;
    }
        break;

    case TRANSACTION_getMacAddr: {
        ALOGI("getMacAddr\n");
        data.enforceInterface(descriptor);
        int size = 0;
        char * ret = getMacAddr(size);
        ALOGD("getMacAddr = %s\n", ret);
        if (ret == NULL) {
            reply->writeInt32(-1);
        } else {
            reply->writeInt32(size);
            reply->write(ret, size);
            free(ret);
        }
        ALOGI("getMacAddr done\n");
        return NO_ERROR;
    }
        break;

    case TRANSACTION_readSelfRegisterFlag: {
        ALOGI("readSelfRegisterFlag\n");
        data.enforceInterface(descriptor);
        int size = 0;
        char * ret = readSelfRegisterFlag(size);
        ALOGD("readSelfRegisterFlag = %s\n", ret);
        if (ret == NULL) {
            reply->writeInt32(-1);
        } else {
            reply->writeInt32(size);
            reply->write(ret, size);
            free(ret);
        }
        ALOGI("readSelfRegisterFlag done\n");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_setSelfRegisterFlag: {
        ALOGI("setSelfRegisterFlag\n");
        data.enforceInterface(descriptor);
        int len = data.readInt32();
        ALOGD("setSelfRegisterFlag len  = %d\n", len);
        if (len == -1) { // array is null
            reply->writeInt32(0);
        } else {
            char buff[len];
            data.read(buff, len);
            ALOGD("setSelfRegisterFlag buff  = %s\n", buff);
            reply->writeInt32(setSelfRegisterFlag(buff, len));
        }
        ALOGI("setSelfRegisterFlag done\n");
        return NO_ERROR;
    }
        break;
	default:
		return BBinder::onTransact(code, data, reply, flags);
	}

	return NO_ERROR;
}

char* DmAgent::readDmTree(int & size) {
	int dm_fd = open(DM_TREE_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_TREE_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		return buff;
	}
}

int DmAgent::writeDmTree(char* tree, int size) {
	if (tree == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_backup_fd = open(DM_TREE_PATH_BACKUP, O_CREAT | O_WRONLY | O_TRUNC,
			0775);
	if (dm_backup_fd == -1) {
		return 0;
	}
	write(dm_backup_fd, tree, size);
	fsync(dm_backup_fd);
	close(dm_backup_fd);
	int dm_fd = open(DM_TREE_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	dm_backup_fd = open(DM_TREE_PATH_BACKUP, O_RDONLY);
	if (dm_fd == -1 || dm_backup_fd == -1) {
		if (dm_fd != -1) {
			close(dm_fd);
		}
		if (dm_backup_fd != -1) {
			close(dm_backup_fd);
		}
		return 0;
	} else {
		int count = 0;
		char buff[512];
		while ((count = read(dm_backup_fd, buff, 512)) > 0) {
			write(dm_fd, buff, count);
		}
		fsync(dm_fd);
		close(dm_fd);
		close(dm_backup_fd);
		unlink(DM_TREE_PATH_BACKUP);
		FileOp_BackupToBinRegionForDM();
	}
	return 1;
}

int DmAgent::isLockFlagSet() {
	return ::file_exist(DM_LOCK_PATH);
}

int DmAgent::setLockFlag(char *lockType, int len) {

	ALOGD("the lockType  is %s  len = %d\n", lockType, len);
	if (lockType == NULL || len == 0 || len > MAX_FILE_SIZE) {
		return 0;
	}

	int fd = open(DM_LOCK_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (fd == -1) {
		ALOGE("Open LOCK FILE error\n");
		return 0;
	}
	int count = write(fd, lockType, len);
	fsync(fd);
	close(fd);
	FileOp_BackupToBinRegionForDM();
	property_set("persist.dm.lock", "true");
	if (!::file_exist(DM_LOCK_PATH) && count == len) {
		return 0;
	}

	return 1;
}

int DmAgent::clearLockFlag() {
	if (::file_exist(DM_LOCK_PATH)) {
		unlink(DM_LOCK_PATH);
		property_set("persist.dm.lock", "false");
		FileOp_BackupToBinRegionForDM();
		if (::file_exist(DM_LOCK_PATH)) {
			return 0;
		}
	}
	return 1;
}
int DmAgent::isWipeSet() {
	return ::file_exist(DM_WIPE_PATH);
}

int DmAgent::setWipeFlag(char *wipeType, int len) {

	ALOGD("the wipeType  is %s  len = %d\n", wipeType, len);
	if (wipeType == NULL || len == 0 || len > MAX_FILE_SIZE) {
		return 0;
	}

	int fd = open(DM_WIPE_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (fd == -1) {
		ALOGE("Open WIPE FILE error\n");
		return 0;
	}
	int count = write(fd, wipeType, len);
	fsync(fd);
	close(fd);
	FileOp_BackupToBinRegionForDM();
	if (!::file_exist(DM_WIPE_PATH) && count == len) {
		return 0;
	}

	return 1;
}

int DmAgent::clearWipeFlag() {
	if (::file_exist(DM_WIPE_PATH)) {
		unlink(DM_WIPE_PATH);
		FileOp_BackupToBinRegionForDM();
		if (::file_exist(DM_WIPE_PATH)) {
			return 0;
		}
	}
	return 1;
}
char * DmAgent::readImsi(int & size) {
	int dm_fd = open(DM_IMSI_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_IMSI_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the readImsi buffer = %s\n", buff);
		return buff;
	}
}

int DmAgent::writeImsi(char * imsi, int size) {
	ALOGD("the imsi want to save is %s\n", imsi);
	if (imsi == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_IMSI_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, imsi, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}
char * DmAgent::getRegisterSwitch(int & size) {
	int dm_fd = open(DM_SWITCH_PATH, O_RDONLY);
	if (dm_fd == -1) {
		// Get value from custom.conf, like dm.SmsRegState = 0
		char switch_value[MAX_VALUE_LEN];
		custom_get_string(MODULE_DM, "SmsRegState", switch_value, "0");
		ALOGD("value of switch is %s\n", switch_value);
		switch_value[1] = '\0';

		size = strlen(switch_value);
		char *buff = (char *) malloc(size+1);
		strcpy(buff, switch_value);
		ALOGD("the getRegisterSwitch buffer = %s\n", buff);
		return buff;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_SWITCH_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the getRegisterSwitch buffer = %s\n", buff);
		return buff;
	}
}

int DmAgent::setRegisterSwitch(char * registerSwitch, int size) {
	ALOGD("the registerSwitch want to save is %s\n", registerSwitch);
	if (registerSwitch == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_SWITCH_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, registerSwitch, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}
char * DmAgent::readOperatorName(int & size) {
	int dm_fd = open(DM_OPERATOR_PATH, O_RDONLY);
	if (dm_fd == -1) {
		ALOGE("readopertorname fd is -1");
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_OPERATOR_PATH, &file_stat);
		size = file_stat.st_size - 1;
		ALOGD("readopertorname size is %d", size);
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		return buff;
	}
}

int DmAgent::setRecoveryCommand() {
	ALOGD("Enter to save recovery command");
	if (::file_exist(RECOVERY_COMMAND)) {
		unlink(RECOVERY_COMMAND);
	}

	int fd = open(RECOVERY_COMMAND, O_CREAT | O_WRONLY | O_TRUNC, 0746);
	if (fd == -1) {
		ALOGE("Open RECOVERY_COMMAND error: [%d]\n",errno);
		return 0;
	}
	char command[] = "--fota_delta_path=/cache/delta";
	int len = sizeof(command);
	ALOGD("recovery command lenth is [%d]\n", len);
	int count = write(fd, command, len);
	fsync(fd);
	ALOGD("--recovery command fsync--");
	close(fd);
	if (count < 0 || count != len) {
		ALOGE("Recovery command write error or the count =[%d] is not the len",
				count);
		return 0;
	}
	return 1;

}

int DmAgent::getLockType() {
	//0 -partially lock 1- fully lock
	if (::file_exist(DM_LOCK_PATH)) {
		//if file exist then get the type
		int lock_fd = open(DM_LOCK_PATH, O_RDONLY);
		if (lock_fd == -1) {
			ALOGE("read lock file fd is -1");
			return -1;
		} else {
			// get file size
			struct stat file_stat;
			bzero(&file_stat, sizeof(file_stat));
			stat(DM_LOCK_PATH, &file_stat);
			//int size=file_stat.st_size-1;
			int size = file_stat.st_size;
			ALOGD("read lock file size is %d", size);
			char *buff = (char *) malloc(size);
			read(lock_fd, buff, size);
			close(lock_fd);

			ALOGD("Read lock file buff = [%s]\n", buff);
			if (strncmp(buff, "partially", 9) == 0) {
				ALOGD("Partially lock");
        free(buff);
				return 0;
			} else if (strncmp(buff, "fully", 5) == 0) {
				ALOGD("fully lock");
        free(buff);
				return 1;
			} else {
				ALOGE("Not partially lock and fully lock, error!");
        free(buff);
				return -1;
			}
		}
	} else
		return NO_ERROR;
}

int DmAgent::getOperatorId() {
	//0 -partially lock 1- fully lock
	return 46002;
}

char* DmAgent::getOperatorName() {
	int len = OPERATOR_LEN;
	return readOperatorName(len);
}

int DmAgent::isHangMoCallLocking() {
	//0 -ture 1 -false
	//if the lock file is exist then Mo call is NOT allow
	if (!::file_exist(DM_LOCK_PATH)) {
		return 0;
	}
	return 1;
}

int DmAgent::isHangMtCallLocking() {
	//0 -ture 1 -false
	if (!::file_exist(DM_LOCK_PATH)) {
		return 0;
	}
	return 1;
	/*if(getLockType()==0){
	 return 1;
	 }else if(getLockType()==1){
	 return 0;
	 }else{
	 ALOGE("error cmd\n");
	 return -1;
	 }*/

}

int DmAgent::setRebootFlag() {
	ALOGD("[REBOOT_FLAG] : enter setRebootFlag");
	char cmd[] = "boot-recovery";
	int ret = writeRebootFlash(cmd);
	if (ret < 1) {
		ALOGE("Write boot-recovery to misc error");
		return ret;
	}

	ret = setRecoveryCommand();
	if (ret < 1) {
		ALOGE("Wirte recovery command error");
	}

	return ret;
	//	char cmd[] = "boot-recovery";
	//	return writeRebootFlash(cmd);
}

int DmAgent::clearRebootFlag() {
	ALOGD("[REBOOT_FLAG] : enter clearRebootFlag");
	//boot to android the command is null
	char cmd[] = "";
	return writeRebootFlash(cmd);
}

int DmAgent::isBootRecoveryFlag() {
	int fd;
	int readSize = 0;
	int result = 0;
	//    int miscNumber = 0; //we can get this num from SS6
	int bootEndBlock = 2048;
	//    miscNumber = get_partition_numb("misc");

	readSize = sizeof("boot-recovery");
	char *readResult = readMiscPartition(readSize);
	if (readResult == NULL) {
		ALOGE("[isBootRecoveryFlag] : read misc partition recovery is error");
        return result;
	} else if (strcmp(readResult, "boot-recovery") == 0) {
		//the next reboot is recovery
		result = 1;
	} else if (strcmp(readResult, "") == 0) {
		//the next reboot is normal mode
		result = 0;
	}
  free(readResult);
  readResult = NULL;
	return result;
}

char* DmAgent::readMiscPartition(int readSize) {
	int fd;
	int result;
	int iRealReadSize = readSize;
	char *readBuf = (char *) malloc(iRealReadSize);
	if (NULL == readBuf) {
		ALOGE("[readMiscPartition] : malloc error");
		return NULL;
	}

	memset(readBuf, '\0', iRealReadSize);
	//    int miscPartition = miscNum; //we can get this num from SS6
	int readEndBlock = 2048;

//	ALOGD("[ReadMiscPartion]:misc number is [%d] read size is  [%d]\r\n",
//			miscPartition, iRealReadSize);
	struct mtd_info_user info;
	char devName[64];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		ALOGD("[ReadMiscPartition]:mtd open error\r\n");
        free(readBuf);
		return NULL;
	}

#ifndef MTK_EMMC_SUPPORT
	//need lseek 2048 for NAND only
	result = lseek(fd, readEndBlock, SEEK_SET);
	if (result != (readEndBlock)) {
		ALOGE("[ReadMiscPartition]:mtd lseek error\r\n");
        free(readBuf);
		return NULL;
	}
#endif

	//read from misc partition to make sure it is correct
	result = read(fd, readBuf, iRealReadSize);
	if (result != iRealReadSize) {
		ALOGE("[ReadMiscPartition]:mtd read error\r\n");
		free(readBuf);
		readBuf = NULL;
		close(fd);
		return NULL;
	}

	ALOGD("[ReadMiscPartition]:end to read  readbuf = %s\r\n", readBuf);
	close(fd);
	return readBuf;

}

//int DmAgent::getUpgradeStatus()
int DmAgent::getUpgradeStatus() {
	int fd;
	int readSize = 32;
	//int miscNumber = UPGRADE_PARTITION;
	//    int miscNumber = 0;
	//    miscNumber = get_partition_numb("misc");
	int iWriteSize = 512;
	int result;
	int statusEndBlock = 2048;
	char *readBuf = NULL;

	int iRealWriteSize = 0;
	//    int miscPartition = get_partition_numb("misc"); //we can get this num from SS6

	// for test
	char *tempBuf = NULL;

	struct mtd_info_user info;
	struct erase_info_user erase_info;
	ALOGD("[getUpgradeStatus]:enter write flash\r\n");
	char devName[64];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		ALOGE("[getUpgradeStatus]:mtd open error\r\n");
		return 0;
	}

	ALOGD("[getUpgradeStatus]:before memget ioctl fd = %d\r\n", fd);

	result = ioctl(fd, MEMGETINFO, &info);
	if (result < 0) {
		ALOGE("[getUpgradeStatus]:mtd get info error\r\n");
        close(fd);
		return 0;
	}
	iWriteSize = info.writesize;

	ALOGD("[getUpgradeStatus]:after memget ioctl fd = %d\r\n", fd);

	ALOGI("[getUpgradeStatus]:start to earse\r\n");
	erase_info.start = __u64(START_BLOCK);
	erase_info.length = __u64(DM_BLOCK_SIZE);
	ALOGD("[getUpgradeStatus]:before erase ioctl u64 convert fd = %d\r\n", fd);
	result = ioctl(fd, MEMERASE, &erase_info);
	if (result < 0) {
		ALOGE(
				"[getUpgradeStatus]:mtd erase error result = %d errorno = [%d] err =[%s] \r\n",
				result, errno, strerror(errno));
		close(fd);
		free(tempBuf);
		return 0;
	}

	ALOGI("[getUpgradeStatus]:end to earse\r\n");

	tempBuf = (char *) malloc(iWriteSize);

	if (tempBuf == NULL) {
		ALOGE("[getUpgradeStatus]:malloc error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);
	iRealWriteSize = sizeof("-12");
	memcpy(tempBuf, "-12", iRealWriteSize);

	ALOGI("[getUpgradeStatus]:start to write\r\n");

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, statusEndBlock, SEEK_SET);
	if (result != (statusEndBlock)) {
		ALOGE("[getUpgradeStatus]:mtd first lseek error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
#endif

	result = write(fd, tempBuf, iWriteSize);
	fsync(fd);
	if (result != iWriteSize) {
		ALOGE("[getUpgradeStatus]:mtd write error,iWriteSize:%d\r\n",
				iWriteSize);
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, statusEndBlock, SEEK_SET);
	if (result != (statusEndBlock)) {
		ALOGE("[getUpgradeStatus]:mtd second lseek error\r\n");
		free(tempBuf);
        close(fd);
		return 0;
	}
#endif

	ALOGI("[getUpgradeStatus]:end to write\r\n");
	//for test end
  close(fd);

	readBuf = readMiscPartition(readSize);
	if (readBuf == NULL) {
		ALOGE("[getUpgradeStatus] read Misc paartition error");
		result = 1;
	} else {
		//tranfer char * to int
		ALOGD("[getUpgradeStatus] : the readbuf is [%s]", readBuf);
		result = atoi(readBuf);
        free(readBuf);
	}
  free(tempBuf);
	return result;

}

//int DmAgent::writeRebootFlash(unsigned int iMagicNum)
int DmAgent::writeRebootFlash(char *rebootCmd) {
	int fd;
	int iWriteSize = 512;
	int iRealWriteSize = 0;
	int result;
	//    int miscPartition = 0; //we can get this num from SS6, not used
	int bootEndBlock = 2048;
	char *tempBuf = NULL;
	//    miscPartition = get_partition_numb("misc");
	struct mtd_info_user info;
	struct erase_info_user erase_info;
	ALOGD("[REBOOT_FLAG]:enter write flash  the cmd is [%s]\r\n", rebootCmd);
	char devName[64];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		ALOGD("[REBOOT_FLAG]:mtd open error\r\n");
		return 0;
	}

	ALOGD("[REBOOT_FLAG]:open MISC_PATH fd = %d\r\n", fd);

#ifdef MTK_GPT_SCHEME_SUPPORT
    result = ioctl(fd, BLKSSZGET, &iWriteSize);
    if (result < 0) {
        ALOGE("[REBOOT_FLAG]:MISC get BLKSSZGET error\r\n");
        close(fd);
        return 0;
    }

#else
	result = ioctl(fd, MEMGETINFO, &info);
	if (result < 0) {
		ALOGE("[REBOOT_FLAG]:mtd get info error\r\n");
		close(fd);
		return 0;
	}
	iWriteSize = info.writesize;

	ALOGD("[REBOOT_FLAG]:after memget ioctl fd = %d\r\n", fd);

	ALOGI("[REBOOT_FLAG]:start to earse\r\n");
	erase_info.start = __u64(START_BLOCK);
	erase_info.length = __u64(DM_BLOCK_SIZE);
	ALOGD("[REBOOT_FLAG]:before erase ioctl u64 convert fd = %d\r\n", fd);
	result = ioctl(fd, MEMERASE, &erase_info);
	if (result < 0) {
		ALOGE(
				"[REBOOT_FLAG]:mtd erase error result = %d errorno = [%d] err =[%s] \r\n",
				result, errno, strerror(errno));
		close(fd);
		free(tempBuf);
		return 0;
	}

	ALOGI("[REBOOT_FLAG]:end to earse\r\n");

#endif

    ALOGD("[REBOOT_FLAG]:get iWriteSize = %d\r\n", iWriteSize);
	tempBuf = (char *) malloc(iWriteSize);

	if (tempBuf == NULL) {
		ALOGE("[REBOOT_FLAG]:malloc error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);
	iRealWriteSize = strlen(rebootCmd);
	memcpy(tempBuf, rebootCmd, iRealWriteSize);

	ALOGD("[REBOOT_FLAG]:start to write tempBuff = %s\r\n", tempBuf);

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, bootEndBlock, SEEK_SET);
	if (result != (bootEndBlock)) {
		ALOGE("[REBOOT_FLAG]:mtd first lseek error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
#endif

	result = write(fd, tempBuf, iWriteSize);
	fsync(fd);
	if (result != iWriteSize) {
		ALOGE("[REBOOT_FLAG]:mtd write error,iWriteSize:%d\r\n", iWriteSize);
		close(fd);
		free(tempBuf);
		return 0;
	}

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, bootEndBlock, SEEK_SET);
	if (result != (bootEndBlock)) {
		ALOGE("[REBOOT_FLAG]:mtd second lseek error\r\n");
		free(tempBuf);
		return 0;
	}
#endif

	ALOGD("[REBOOT_FLAG]:end to write iRealWriteSize = %d \r\n", iRealWriteSize);

	free(tempBuf);
	close(fd);
	return 1;
}

int DmAgent::restartAndroid() {
	ALOGI(
			"Before restart android DM is going to restart Andorid sleep 10 seconds");
	sleep(10);
	property_set("ctl.stop", "runtime");
	property_set("ctl.stop", "zygote");
	property_set("ctl.start", "zygote");
	property_set("ctl.start", "runtime");
	ALOGI("DM has restarting Andoird");
	return 1;
}

int DmAgent::clearOtaResult()
{
    int dev = -1;
    char dev_name[64];
    int count;
	int i = 0;

    strcpy(dev_name, MISC_PATH);

    dev = open(dev_name, O_WRONLY);
    if (dev < 0)  {
        ALOGE("[clearUpgradeResult]Can't open %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1) {
        ALOGE("[clearUpgradeResult]Failed seeking %s\n(%s)\n", dev_name, strerror(errno));
        close(dev);
        return 0;
    }

    count = write(dev, &i, sizeof(i));
	fsync(dev);
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

int DmAgent::readOtaResult() {
	int result;

	get_ota_result(&result);
	ALOGD("ota_result=%d\n", result);

	return result;
}

char * DmAgent::getSwitchValue(int & size) {
	int dm_fd = open(DM_CONF_SWITCH_PATH, O_RDONLY);
	if (dm_fd == -1) {
		// Get value from custom.conf, like dm.MediatekDMState = 0
		char switch_value[MAX_VALUE_LEN];
		custom_get_string(MODULE_DM, "MediatekDMState", switch_value, "0");
		ALOGD("value of switch is %s\n", switch_value);
		switch_value[1] = '\0';

		size = strlen(switch_value);
		char *buff = (char *) malloc(size+1);
		strcpy(buff, switch_value);
		ALOGD("the getSwitchValue buffer = %s\n", buff);
		return buff;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_CONF_SWITCH_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the getSwitchValue buffer = %s\n", buff);
		return buff;
	}
}

int DmAgent::setSwitchValue(char * switch_value, int size) {
	ALOGD("the switch value want to save is %s\n", switch_value);
	if (switch_value == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_CONF_SWITCH_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, switch_value, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::getDmSwitchValue(int & size) {
	int dm_fd = open(DM_DM_SWITCH_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_DM_SWITCH_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the DM switch value buffer = %s\n", buff);
		return buff;
	}
}

int DmAgent::setDmSwitchValue(char * switch_value, int size) {
	ALOGD("the DM switch value want to save is %s\n", switch_value);
	if (switch_value == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_DM_SWITCH_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, switch_value, size);
	fsync(dm_fd);
	close(dm_fd);
	// Do not backup to NVRAM.
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::getSmsRegSwitchValue(int & size) {
	int dm_fd = open(DM_SMSREG_SWITCH_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_SMSREG_SWITCH_PATH, &file_stat);
		size = file_stat.st_size;
		char * buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the SmsReg switch value buffer = %s\n", buff);
		return buff;
	}
}

int DmAgent::setSmsRegSwitchValue(char * switch_value, int size) {
	ALOGD("the SmsReg switch value want to save is %s\n", switch_value);
	if (switch_value == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_SMSREG_SWITCH_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, switch_value, size);
	fsync(dm_fd);
	close(dm_fd);
	// Do not backup to NVRAM.
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::readRegisterFlag(int & size) {
	int dm_fd = open(REGISTER_FLAG_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(REGISTER_FLAG_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the readRegisterFlag buffer = %s\n", buff);
		return buff;
	}
}

bool DmAgent::setRegisterFlag(char * flag, int size) {
	ALOGD("the register flag want to save is %s\n", flag);
	if (flag == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(REGISTER_FLAG_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, flag, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::readImsi1(int & size) {
	int dm_fd = open(REGISTER_IMSI1_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(REGISTER_IMSI1_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the readIMSI1 buffer = %s\n", buff);
		return buff;
	}
}

bool DmAgent::writeImsi1(char * imsi, int size) {
	ALOGD("the imsi1 want to save is %s\n", imsi);
	if (imsi == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(REGISTER_IMSI1_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, imsi, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::readImsi2(int & size) {
	int dm_fd = open(REGISTER_IMSI2_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(REGISTER_IMSI2_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		ALOGD("the readIMSI2 buffer = %s\n", buff);
		return buff;
	}
}

bool DmAgent::writeImsi2(char * imsi, int size) {
	ALOGD("the imsi2 want to save is %s\n", imsi);
	if (imsi == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(REGISTER_IMSI2_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, imsi, size);
	fsync(dm_fd);
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}

char * DmAgent::readIccID1(int & size) {
    int fd = open(REGISTER_ICCID1_PATH, O_RDONLY);
    if (fd == -1) {
        return NULL;
    } else {
        // get file size
        struct stat file_stat;
        bzero(&file_stat, sizeof(file_stat));
        stat(REGISTER_ICCID1_PATH, &file_stat);
        size = file_stat.st_size;
        char *buff = (char *) malloc(size);
        read(fd, buff, size);
        close(fd);
        ALOGD("the readIccID1 buffer = %s\n", buff);
        return buff;
    }
}

bool DmAgent::writeIccID1(char * iccID, int size) {
    ALOGD("the ICCID1 want to save is %s\n", iccID);
    if (iccID == NULL || size == 0 || size > MAX_FILE_SIZE) {
        return 0;
    }
    int fd = open(REGISTER_ICCID1_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
    if (fd == -1) {
        return 0;
    }
    int count = write(fd, iccID, size);
    fsync(fd);
    close(fd);
    FileOp_BackupToBinRegionForDM();
    if (count == size) {
        return 1;
    } else {
        return 0;
    }
}

char * DmAgent::readIccID2(int & size) {
    int fd = open(REGISTER_ICCID2_PATH, O_RDONLY);
    if (fd == -1) {
        return NULL;
    } else {
        // get file size
        struct stat file_stat;
        bzero(&file_stat, sizeof(file_stat));
        stat(REGISTER_ICCID2_PATH, &file_stat);
        size = file_stat.st_size;
        char *buff = (char *) malloc(size);
        read(fd, buff, size);
        close(fd);
        ALOGD("the readIMSI2 buffer = %s\n", buff);
        return buff;
    }
}

bool DmAgent::writeIccID2(char * iccID, int size) {
    ALOGD("the ICCID2 want to save is %s\n", iccID);
    if (iccID == NULL || size == 0 || size > MAX_FILE_SIZE) {
        return 0;
    }
    int fd = open(REGISTER_ICCID2_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
    if (fd == -1) {
        return 0;
    }
    int count = write(fd, iccID, size);
    fsync(fd);
    close(fd);
    FileOp_BackupToBinRegionForDM();
    if (count == size) {
        return 1;
    } else {
        return 0;
    }
}

char * DmAgent::getMacAddr(int & size) {
    int fd = open(WIFI_MAC_PATH, O_RDONLY);
    if (fd == -1) {
        return NULL;
    } else {
        // get file size
        struct stat file_stat;
        bzero(&file_stat, sizeof(file_stat));
        stat(WIFI_MAC_PATH, &file_stat);
        size = file_stat.st_size;
        char *buffTemp = (char *) malloc(size);
        read(fd, buffTemp, size);
        close(fd);

        // Get the MAC, byte 5-10.
        char *buff = (char *) malloc(MAC_ADDRESS_DIGITS);
        memcpy(buff, buffTemp + 4, MAC_ADDRESS_DIGITS);
        free(buffTemp);
        size = MAC_ADDRESS_DIGITS;
        ALOGD("the getMacAddr buffer = %s\n", buff);
        return buff;
    }
}

char * DmAgent::readSelfRegisterFlag(int & size) {
    int dm_fd = open(SELF_REGISTER_FLAG_PATH, O_RDONLY);
    if (dm_fd == -1) {
        return NULL;
    } else {
        // get file size
        struct stat file_stat;
        bzero(&file_stat, sizeof(file_stat));
        stat(SELF_REGISTER_FLAG_PATH, &file_stat);
        size = file_stat.st_size;
        char *buff = (char *) malloc(size);
        read(dm_fd, buff, size);
        close(dm_fd);
        ALOGD("the readSelfRegisterFlag buffer = %s\n", buff);
        return buff;
    }
}

bool DmAgent::setSelfRegisterFlag(char * flag, int size) {
    ALOGD("the SelfRegister flag want to save is %s\n", flag);
    if (flag == NULL || size == 0 || size > MAX_FILE_SIZE) {
        return 0;
    }
    int dm_fd = open(SELF_REGISTER_FLAG_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
    if (dm_fd == -1) {
        return 0;
    }
    int count = write(dm_fd, flag, size);
    fsync(dm_fd);
    close(dm_fd);
    FileOp_BackupToBinRegionForDM();
    if (count == size) {
        return 1;
    } else {
        return 0;
    }
}

int main(int argc, char *argv[]) {
	//    daemon (0,0);
	umask(000);
	mkdir(DM_ROOT_PATH, 0775);

    int ret = mkdir(RECOVERY_FOLDER, 0775);
    if (ret < 0) {
        ALOGE("Create RECOVERY_FOLDER error: [%d]\n", errno);
    } else {
        ALOGD("RECOVERY_FOLDER created end.");
    }

	DmAgent::instantiate();
	if (::file_exist(DM_LOCK_PATH)) {
		property_set("persist.dm.lock", "true");
	} else {
		property_set("persist.dm.lock", "false");
	}
	ProcessState::self()->startThreadPool();
	ALOGD("DmAgent Service is now ready");
	IPCThreadState::self()->joinThreadPool();
	return (0);
}

