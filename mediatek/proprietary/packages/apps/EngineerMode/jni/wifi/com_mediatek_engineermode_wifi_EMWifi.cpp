/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

#undef LOG_NDEBUG 
#undef NDEBUG
#define LOG_TAG "EM-Wifi-JNI"
#define MTK_LOG_ENABLE 1
#include <cutils/log.h>

#include <stdio.h>
#include <unistd.h>

#include "jni.h"
#include "JNIHelp.h"
#include "type.h"
#include "android_runtime/AndroidRuntime.h"
#include "WiFi_EM_API.h"

using namespace android;

#define UNIMPLEMENT  	-2
#define STATUS_ERROR  -1
#define STATUS_OK     0
static CAdapter *s_adapter;
static COID *s_oid;

static int check_EMWifi_status(status_t status) {
	if (status == NO_ERROR) {
		return STATUS_OK;
	} else {
		return STATUS_ERROR;
	}
}

static int com_mediatek_engineermode_wifi_EMWifi_setTestMode(JNIEnv *env,
		jobject thiz) {
	ALOGE("JNI, Enter setTestMode succeed");
	return s_adapter->setTestMode();
}

static int com_mediatek_engineermode_wifi_EMWifi_setNormalMode(JNIEnv *env,
		jobject thiz) {
	ALOGE("JNI, Enter setNormalMode succeed");
	return s_adapter->setNormalMode();
}

static int com_mediatek_engineermode_wifi_EMWifi_setStandBy(JNIEnv *env,
		jobject thiz) {
	ALOGE("JNI, Enter setStandBy succeed");
	return s_adapter->setStandBy();
}

static int com_mediatek_engineermode_wifi_EMWifi_setEEPRomSize(JNIEnv *env,
		jobject thiz, jlong i4EepromSz) {
	INT_32 index = -1;
	ALOGE("JNI Set the eep_rom size is size = (%d)", ((int) i4EepromSz));
	return s_adapter->setEEPRomSize((INT_32)i4EepromSz);
}

static int com_mediatek_engineermode_wifi_EMWifi_setEEPRomFromFile(JNIEnv *env,
		jobject thiz, jstring atcFileName) {
	INT_32 index = -1;

	if (atcFileName == NULL) {
		return STATUS_ERROR;
	}

	INT_32 len = env->GetStringUTFLength(atcFileName);
	const CHAR *file_name = env->GetStringUTFChars(atcFileName, NULL);

	CHAR *name = new CHAR[len + 1];
	if (name) {
		memcpy(name, file_name, len);
		name[len] = '\0';

		ALOGV("setEEPRomFromFile, file name = (%s)", name);

		index = s_adapter->setEEPRomFromFile(name);

		delete[] name;
	} else {
		ALOGV("Error when to alloc memeory at setEEPRomFromFile");
	}
	env->ReleaseStringUTFChars(atcFileName, file_name);

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_readTxPowerFromEEPromEx(
		JNIEnv *env, jobject thiz, jlong i4ChnFreg, jlong i4Rate,
		jlongArray PowerStatus, jint arrayLength) {

	jlong valBuff[8] = {0};
	INT_32 pi4TxPwrGain = -1;
	INT_32 pi4Eerp = -1;
	INT_32 pi4ThermoVal = -1;
	INT_32 index = -1;

    if (PowerStatus == NULL) {
        ALOGE("NULL java array of TxPower from EE Prom Ex");
		return -2;
    }

	if (arrayLength != 3) {
		ALOGE("Error length pass to the array");
		return STATUS_ERROR;
	} else {
		index = s_adapter->readTxPowerFromEEPromEx((INT_32)i4ChnFreg, (INT_32)i4Rate,
				&pi4TxPwrGain, &pi4Eerp, &pi4ThermoVal);
		if (index == 0) {
			valBuff[0] = pi4TxPwrGain;
			valBuff[1] = pi4Eerp;
			valBuff[2] = pi4ThermoVal;
		} else {
			ALOGE("Error to call readTxPowerFromEEPromEx in native");
		}
	}
	ALOGE(
			"get the readTxPowerFromEEPromEx value, pi4TxPwrGain = (%d), pi4Eerp = (%d), pi4ThermoVal = (%d)",
			((int) pi4TxPwrGain), ((int) pi4Eerp), ((int) pi4ThermoVal));

    env->SetLongArrayRegion(PowerStatus, 0, 3, valBuff);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_getPacketTxStatusEx(
		JNIEnv *env, jobject thiz, jlongArray PktStatus, jint arraylen) {
	jlong valBuff[8] = {0};
	INT_32 index = -1;

	UINT_32 u4SentCount, u4AckCount;
    UINT_16 u2Alc;
    UINT_8  ucCckGainControl, ucOfdmGainControl;

	if (PktStatus == NULL) {
		ALOGE("NULL java array of Packet Tx Status Ex");
		return -2;
	}

	if (arraylen != 5) {
		ALOGE("Error length pass to the array");
		return STATUS_ERROR;
	} else {
		index = s_adapter->getPacketTxStatusEx(&u4SentCount, &u4AckCount,
				&u2Alc, &ucCckGainControl, &ucOfdmGainControl);
		if (index == 0) {
			valBuff[0] = u4SentCount;
			valBuff[1] = u4AckCount;
			valBuff[2] = u2Alc;
			valBuff[3] = ucCckGainControl;
			valBuff[4] = ucOfdmGainControl;
		}
	}
    env->SetLongArrayRegion(PktStatus, 0, 5, valBuff);

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setEEPromCKSUpdated(
		JNIEnv *env, jobject thiz) {
	ALOGE("JNI, Enter setEEPromCKSUpdated succeed");
	return s_adapter->setEEPromCKSUpdated();
}

static int com_mediatek_engineermode_wifi_EMWifi_getPacketRxStatus(JNIEnv *env,
		jobject thiz, jlongArray i4Init, jint arraylen) {

	ALOGE("JNI, Enter getPacketRxStatus succeed");
	INT_32 index = -1;
	jlong valBuff[4] = {0};
	INT_32 pi4RxOk, pi4RxCrcErr;

	if (arraylen != 2) {
		ALOGE("Wrong array length for getPacketRxStatus");
		return STATUS_ERROR;
	}

	if (i4Init == NULL) {
		ALOGE("NULL java array of getPacketRxStatus");
		return -2;
	}

	index = s_adapter->getPacketRxStatus(&pi4RxOk, &pi4RxCrcErr);
	if (index == 0) {
		valBuff[0] = pi4RxOk;
		valBuff[1] = pi4RxCrcErr;
		ALOGE("JNI, getPacketRxStatus value pi4RxOk = (%d), pi4RxCrcErr = (%d)",
				(int) pi4RxOk, (int) pi4RxCrcErr);
	} else {
		ALOGE("Native, get getPacketRxStatus failed");
	}
    env->SetLongArrayRegion(i4Init, 0, 2, valBuff);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setOutputPower(JNIEnv *env,
		jobject thiz, jlong i4Rate, jlong i4TxPwrGain, jlong i4TxAnt) {
	ALOGE(
			"JNI, Enter setOutputPower succeed, i4Rate = %d, i4TxPwrGain = %d, i4TxAnt = %d",
			(int) i4Rate, (int) i4TxPwrGain, (int) i4TxAnt);
	return s_adapter->setOutputPower(i4Rate, i4TxPwrGain, (INT_32)i4TxAnt);
}

static int com_mediatek_engineermode_wifi_EMWifi_setLocalFrequecy(JNIEnv *env,
		jobject thiz, jlong i4TxPwrGain, jlong i4TxAnt) {
	ALOGE("JNI, Enter setLocalFrequecy succeed, i4TxPwrGain = %d, i4TxAnt = %d",
			(int) i4TxPwrGain, (int) i4TxAnt);
	return s_adapter->setLocalFrequecy((INT_32)i4TxPwrGain, (INT_32)i4TxAnt);
}

static int com_mediatek_engineermode_wifi_EMWifi_setCarrierSuppression(
		JNIEnv *env, jobject thiz, jlong i4Modulation, jlong i4TxPwrGain,
		jlong i4TxAnt) {
	ALOGE(
			"JNI, Enter setCarrierSuppression succeed, i4Modulation = %d, i4TxPwrGain = %d, i4TxAnt = %d",
			(int) i4Modulation, (int) i4TxPwrGain, (int) i4TxAnt);
	return s_adapter->setCarrierSuppression((INT_32)i4Modulation, (INT_32)i4TxPwrGain, (INT_32)i4TxAnt);
}

static int com_mediatek_engineermode_wifi_EMWifi_setOperatingCountry(
		JNIEnv *env, jobject thiz, jstring acChregDomain) {
	INT_32 index = -1;
	const CHAR *file_name = env->GetStringUTFChars(acChregDomain, NULL);
	INT_32 len = env->GetStringLength(acChregDomain);
	CHAR *name = new char[len + 1];

	if (!name) {
		env->ReleaseStringUTFChars(acChregDomain, file_name);
		return STATUS_ERROR;
	}

	memcpy(name, file_name, len);
	name[len] = '\0';

	ALOGE("JNI, Enter setOperatingCountry succeed, country name = %s", name);
	index = s_adapter->setOperatingCountry(name);
	delete[] name;
	env->ReleaseStringUTFChars(acChregDomain, file_name);

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setChannel(JNIEnv *env,
		jobject thiz, jlong i4ChFreqKHz) {
	ALOGE("JNI, Enter setChannel succeed, country i4ChFreqKHz = %d",
			(int) i4ChFreqKHz);
	INT_32 index = -1;
	index = s_adapter->setChannel((INT_32)i4ChFreqKHz);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_getSupportedRates(JNIEnv *env,
		jobject thiz, jintArray pu2RateBuf, jlong i4MaxNum) {
	INT_32 index = -1;
	UINT_16 rate = 0;
	jint targetRate = 0;

	if (pu2RateBuf == NULL) {
		ALOGE("NULL java array of getSupportedRates");
		return -2;
	}
	index = s_adapter->getSupportedRates(&rate, (INT_32)i4MaxNum);
	if (index == 0) {
		targetRate = rate;
	} else {
		ALOGE("Native, methods call failed");
	}
	ALOGE(
			"JNI, Enter getSupportedRates succeed, pu2RateBuf = %d, i4MaxNum = %d",
			(int) rate, (int) i4MaxNum);
	env->SetIntArrayRegion(pu2RateBuf, 0, 1, &targetRate);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setOutputPin(JNIEnv *env,
		jobject thiz, jlong i4PinIndex, jlong i4OutputLevel) {
	INT_32 index = -1;

	ALOGE(
			"JNI, Enter setOutputPin succeed, i4PinIndex = %d, i4OutputLevel = %d",
			(int) i4PinIndex, (int) i4OutputLevel);
	return s_adapter->setOutputPin(i4PinIndex, i4OutputLevel);
}

static int com_mediatek_engineermode_wifi_EMWifi_readEEPRom16(JNIEnv *env,
		jobject thiz, jlong u4Offset, jlongArray pu4Value) {
	UINT_32 value = 0;
	INT_32 index = -1;
	jlong targVal = 0;

	if (pu4Value == NULL) {
		ALOGE("NULL java array of readEEPRom16");
		return -2;
	}

	index = s_adapter->readEEPRom16(u4Offset, &value);
	targVal = value;

	ALOGE("JNI, Enter readEEPRom16 succeed, u4Offset = %d, pu4Value = %d",
			(int) u4Offset, (int) value);
    env->SetLongArrayRegion(pu4Value, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_readSpecEEPRom16(JNIEnv *env,
		jobject thiz, jlong u4Offset, jlongArray pu4Value) {
	UINT_32 value = 0;
	INT_32 index = -1;
	jlong targVal = 0;

	if (pu4Value == NULL) {
		ALOGE("NULL java array of readSpecEEPRom16");
		return -2;
	}

	index = s_adapter->readSpecEEPRom16(u4Offset, &value);
	targVal = value;

	ALOGE("JNI, Enter readSpecEEPRom16 succeed, u4Offset = %d, pu4Value = %d",
			(int) u4Offset, (int) value);
    env->SetLongArrayRegion(pu4Value, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_writeEEPRom16(JNIEnv *env,
		jobject thiz, jlong u4Offset, jlong u4Value) {
	INT_32 index = -1;

	ALOGE("JNI, Enter writeEEPRom16 succeed, u4Offset = %d, u4Value = %d",
			(int) u4Offset, (int) u4Value);
	index = s_adapter->writeEEPRom16(u4Offset, u4Value);

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_eepromReadByteStr(JNIEnv *env,
        jobject thiz, jlong u4Addr, jlong u4Length, jbyteArray paucStr) {
    INT_32 index = -1;
    jbyte byteBuffer[512] = {0};

    index = s_adapter->eepromReadByteStr(u4Addr, u4Length, (char *)byteBuffer);

	if (index == 0) {
        env->SetByteArrayRegion(paucStr, 0, sizeof(byteBuffer), byteBuffer);
        ALOGE("JNI, Enter eepromReadByteStr succeed, paucStr = %s", byteBuffer);
	} else {
		ALOGE("Native, eepromReadByteStr call failed");
	}

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_eepromWriteByteStr(JNIEnv *env,
		jobject thiz, jlong u4Addr, jlong u4Length, jstring paucStr) {
	INT_32 index = -1;
	INT_32 len = env->GetStringUTFLength(paucStr);
	const CHAR *str = env->GetStringUTFChars(paucStr, NULL);

	CHAR *name = new CHAR[len + 1];
	if (name) {
		memcpy(name, str, len);
		name[len] = '\0';

		index = s_adapter->eepromWriteByteStr(u4Addr, u4Length, name);

		ALOGE("JNI, Enter eepromWriteByteStr succeed, name = %s", name);

		delete[] name;
	}

	ALOGE(
			"JNI, Enter eepromWriteByteStr succeed, u4Addr = %d, u4Length = %d, str = %s",
			(int) u4Addr, (int) u4Length, str);
	env->ReleaseStringUTFChars(paucStr, str);

	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setATParam(JNIEnv *env,
		jobject thiz, jlong u4FuncIndex, jlong pu4FuncData) {
	ALOGE("JNI, Enter setATParam succeed, u4FuncIndex = %d, pu4FuncData = %d",
			(unsigned int) u4FuncIndex, (unsigned int) pu4FuncData);
	return s_adapter->SetATParam(u4FuncIndex, pu4FuncData);
}

static int com_mediatek_engineermode_wifi_EMWifi_getATParam(JNIEnv *env,
		jobject thiz, jlong u4FuncIndex, jlongArray pu4FuncData) {
	INT_32 index = -1;
	UINT_32 value = 0;
	jlong targVal = 0;

	if (pu4FuncData == NULL) {
		ALOGE("NULL java array of getATParam");
		return -2;
	}

	index = s_adapter->GetATParam(u4FuncIndex, &value);
	targVal = value;

	ALOGE("JNI, Enter getATParam succeed, u4FuncIndex = %d, pu4FuncData = %d",
			(unsigned int) u4FuncIndex, (unsigned int) value);
    env->SetLongArrayRegion(pu4FuncData, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setXtalTrimToCr(JNIEnv *env,
		jobject thiz, jlong u4Value) {
	//	ALOGD("com_mediatek_engineermode_wifi_EMWifi_setXtalTrimToCr_u4Value = %l", u4Value);
	int index;
	ALOGE("JNI, Enter getATParam succeed, u4Value = %d", (int) u4Value);
	return s_adapter->setXtalTrimToCr(u4Value);
}

static int com_mediatek_engineermode_wifi_EMWifi_queryThermoInfo(JNIEnv *env,
		jobject thiz, jlongArray pi4Enable, jint len) {
	INT_32 value = 0;
	INT_32 index = -1;
	UINT_32 pu4Value = 0;
	jlong valBuff[2] = {0};

	if (len != 2) {
		ALOGE("Wrong length of queryThermoInfo array");
		return -2;
	}

	if (pi4Enable == NULL) {
		ALOGE("NULL java array of queryThermoInfo");
		return -2;
	}

	index = s_adapter->queryThermoInfo(&value, &pu4Value);
	if (index == 0) {
		valBuff[0] = value;
		valBuff[1] = pu4Value;
	} else {
		ALOGE("Native, Error to call queryThermoInfo");
	}

	ALOGE(
			"JNI, Enter queryThermoInfo succeed, pi4Enable1 = %d, pi4Enable2 = %d",
			(int) value, (int) pu4Value);

    env->SetLongArrayRegion(pi4Enable, 0, 2, valBuff);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_setThermoEn(JNIEnv *env,
		jobject thiz, jlong i4Enable) {
	ALOGE("JNI, Enter setThermoEn succeed, i4Enable = %d", (int) i4Enable);
	return s_adapter->setThermoEn(i4Enable);
}

static int com_mediatek_engineermode_wifi_EMWifi_getSpecEEPRomSize(JNIEnv *env,
		jobject thiz, jlongArray pi4EepromSz) {
	jlong targVal = 0;
	INT_32 value;
	INT_32 index;
	if (pi4EepromSz == NULL) {
		ALOGE("NULL java array of getSpecEEPRomSize");
		return -2;
	}
	index = s_adapter->getSpecEEPRomSize(&value);
	ALOGE("JNI, Enter getSpecEEPRomSize succeed, value = %d", (int) value);
	targVal = value;
	env->SetLongArrayRegion(pi4EepromSz, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_getEEPRomSize(JNIEnv *env,
		jobject thiz, jlongArray pi4EepromSz) {
	jlong targVal = 0;
	INT_32 value;
	INT_32 index;
	if (pi4EepromSz == NULL) {
		ALOGE("NULL java array of getEEPRomSize");
		return -2;
	}
	index = s_adapter->getEEPRomSize(&value);
	ALOGE("JNI, Enter getEEPRomSize succeed, value = %d", (int) value);
	targVal = value;
	env->SetLongArrayRegion(pi4EepromSz, 0, 1, &targVal);
	return index;
}

//static long com_mediatek_engineermode_wifi_EMWifi_getEEPRomSize(JNIEnv *env,
//		jobject thiz) {
//
//	UINT_32 result = 0;
//	INT_32 index = -1;
//
//	index = CWrapper::getXtalTrimToCr(&result);
//
//	ALOGE("step into ");
//
//	if (index != 0) {
//		return index;
//	}
//	return result;
//}

static int com_mediatek_engineermode_wifi_EMWifi_setPnpPower(JNIEnv *env,
		jobject thiz, jlong i4PowerMode) {
	ALOGE("JNI, Enter setPnpPower succeed, i4PowerMode = %d", (int) i4PowerMode);
	return s_adapter->setPnpPower(i4PowerMode);
}

static int com_mediatek_engineermode_wifi_EMWifi_setAnritsu8860bTestSupportEn(
		JNIEnv *env, jobject thiz, jlong i4Enable) {
	ALOGE("JNI, Enter setAnritsu8860bTestSupportEn succeed, i4Enable = %d",
			(int) i4Enable);
	return s_adapter->setAnritsu8860bTestSupportEn(i4Enable);
}

static int com_mediatek_engineermode_wifi_EMWifi_writeMCR32(JNIEnv *env,
		jobject thiz, jlong offset, jlong value) {
	ALOGE("JNI, Enter writeMCR32 succeed, offset = %d, value = %d",
			(int) offset, (int) value);
	return s_adapter->writeMCR32(offset, value);
}

static int com_mediatek_engineermode_wifi_EMWifi_readMCR32(JNIEnv *env,
		jobject thiz, jlong offset, jlongArray value) {
	jlong targVal = 0;
	UINT_32 val = 0;
	INT_32 index = -1;

	if (value == NULL) {
		ALOGE("NULL java array of readMCR32");
		return -2;
	}
	index = s_adapter->readMCR32(offset, &val);
	targVal = val;
	ALOGE("JNI, Enter writeMCR32 succeed, offset = %d, value = %d",
			(int) offset, (int) val);
    env->SetLongArrayRegion(value, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_initial(JNIEnv *env,
		jobject thiz/*, jlong chipID*/) {
	INT_32 index = -1;
    const char *ifname = "wlan0";
    const char *wrapperName="local";
    char desc[NAMESIZE];
    INT_32  ret = ERROR_RFTEST_GENERAL_ERROR;   
    UINT_32 chipID = 0x6620;
    s_oid = new CLocalOID(ifname);
    //s_oid->GetChipID(ChipID, ifname);
    sprintf(desc, "%x", chipID & DEVID_IDMSK);
    em_printf(MSG_DEBUG, (char*)"CWrapper name is %s desc is %s\n", ifname, desc);
    
    switch(chipID & DEVID_IDMSK)
    {
        case 0x6620: 
            em_printf(MSG_DEBUG, (char*)"Chip ID = 0x%x", chipID);
            s_adapter = new CMT66xx(ifname, desc, s_oid, wrapperName, 0x6620);
            //ret = ERROR_RFTEST_SUCCESS;   
            ret = 0x6620;
            break;  
        default: 
            s_adapter = NULL;
            em_printf(MSG_ERROR, (char*)"Not supported Chip ID = 0x%x", chipID);
            break;   

    }
	return ret;
}

static int com_mediatek_engineermode_wifi_EMWifi_unInitial(JNIEnv *env,
		jobject thiz) {
	INT_32 index = -1;
	ALOGE("JNI, Entener Uninitialize");
    delete s_oid;
    s_adapter->CloseDevice(); /* if reference equal to 0 or less than 0, will delete s_adapter */
    s_adapter = NULL;
    s_oid = NULL;
	return ERROR_RFTEST_SUCCESS;
}

static int com_mediatek_engineermode_wifi_EMWifi_getXtalTrimToCr(JNIEnv *env,
		jobject thiz, jlongArray value) {
	INT_32 index = -1;
	UINT_32 result = 0;
	jlong targVal = 0;

	if (value == NULL) {
		ALOGE("NULL java array of getXtalTrimToCr, can't get value");
		return -2;
	}
    index = s_adapter->getXtalTrimToCr(&result);

	targVal = result;
	ALOGE("step into getXtalTrimToCr and got the value result = (%d)",
			(int) result);

    env->SetLongArrayRegion(value, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_getDPDLength(JNIEnv *env,
		jobject thiz, jlongArray pi4DPDLength) {
	INT_32 value;
	INT_32 index;
	jlong targVal = 0;
	if (pi4DPDLength == NULL) {
		ALOGE("NULL java array of getDPDLength");
		return -2;
	}
    index = s_adapter->getDPDLength(&value);
	ALOGE("JNI, Enter getDPDLength succeed");
	targVal = value;
	env->SetLongArrayRegion(pi4DPDLength, 0, 1, &targVal);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_writeDPD32(JNIEnv *env,
		jobject thiz, jlong offset, jlong value) {
	ALOGE("JNI, Enter writeDPD32 succeed, offset = %d, value = %d",
			(int) offset, (int) value);
	return s_adapter->writeDPD32(offset, value);
}

static int com_mediatek_engineermode_wifi_EMWifi_readDPD32(JNIEnv *env,
		jobject thiz, jlong offset, jlongArray value) {
	UINT_32 val = 0;
	INT_32 index = -1;
	jlong targVal = 0;

	if (value == NULL) {
		ALOGE("NULL java array of readDPD32");
		return -2;
	}
	index = s_adapter->readDPD32(offset, &val);
	targVal = val;
	ALOGE("JNI, Enter readDPD32 succeed, offset = %d, value = %d",
			(int) offset, (int) val);
    env->SetLongArrayRegion(value, 0, 1, &targVal);
	return index;
}




static int com_mediatek_engineermode_wifi_EMWifi_setDPDFromFile(JNIEnv *env,
		jobject thiz, jstring atcFileName) {
	INT_32 index = -1;

	if (atcFileName == NULL) {
		return STATUS_ERROR;
	}

	INT_32 len = env->GetStringUTFLength(atcFileName);
	const CHAR *file_name = env->GetStringUTFChars(atcFileName, NULL);

	CHAR *name = new CHAR[len + 1];
	if (name) {
		memcpy(name, file_name, len);
		name[len] = '\0';

		ALOGV("setDPDFromFile, file name = (%s)", name);

		index = s_adapter->setDPDFromFile(name);

		delete[] name;
	} else {
		ALOGV("Error when to alloc memeory at setDPDFromFile");
	}
	env->ReleaseStringUTFChars(atcFileName, file_name);

	return index;
}

// Added by mtk54046 @ 2012-01-05 for get support channel list
static int com_mediatek_engineermode_wifi_EMWifi_getSupportChannelList(JNIEnv *env,
		jobject thiz, jlongArray pau4Channel) {
	#define MAX_CHANNEL_NUM 75
	UINT_32 value[MAX_CHANNEL_NUM] = {0};
    jlong buffer[MAX_CHANNEL_NUM] = {0};
	INT_32 index;
    if (pau4Channel == NULL) {
        ALOGE("NULL java array of getSupportChannelList");
		return -2;
    }
	index = s_adapter->getSupportChannelList(value);
	if (!index) {
		ALOGE("JNI, Enter getSupportChannelList succeed, channel list length is %d. value[1] is %d, value[2] is %d", (int)value[0], (int)value[1], (int)value[2]);
		buffer[0] = value[0];
		for (int i=1; i<= value[0]; i++) {
			buffer[i] = value[i];
		}
	}
	env->SetLongArrayRegion(pau4Channel, 0, value[0] + 1, buffer);
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_doCTIATestSet(JNIEnv *env,
		jobject thiz, jlong u4Id, jlong u4Value) {
	INT_32 index;
	ALOGE("JNI Set ID is %u, value is %lu", u4Id, u4Value);
	index = s_adapter->sw_cmd(1, u4Id, (UINT_32*)&u4Value);
	if (!index) {
		ALOGE("doCTIATestSet succeed, ID is %u", u4Id);
	}
	return index;
}

static int com_mediatek_engineermode_wifi_EMWifi_doCTIATestGet(JNIEnv *env,
		jobject thiz, jlong u4Id, jlongArray pu4Value) {
	INT_32 index;
	UINT_32 value = 0;
	jlong targVal = 0;
	if (pu4Value == NULL) {
		ALOGE("NULL java array of doCTIATestGet");
		return -2;
	}
	ALOGE("JNI Get ID is %u, value is %u", u4Id, value);
	index = s_adapter->sw_cmd(0, u4Id, &value);
	if (!index) {
		ALOGE("doCTIATestGet succeed, ID is %u, value is %u", u4Id, value);
	}
	targVal = value;
	env->SetLongArrayRegion(pu4Value, 0, 1, &targVal);
	return index;
}

//method register to vm
static JNINativeMethod
		gMethods[] = {
				{ "initial", "()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_initial },
				{ "unInitial", "()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_unInitial },
				{
						"getXtalTrimToCr",
						"([J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getXtalTrimToCr },
				{
						"setTestMode",
						"()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setTestMode },
				{
						"setNormalMode",
						"()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setNormalMode },
				{ "setStandBy", "()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setStandBy },
				{
						"setEEPRomSize",
						"(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setEEPRomSize },
				{
						"setEEPRomFromFile",
						"(Ljava/lang/String;)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setEEPRomFromFile },
				{
						"readTxPowerFromEEPromEx",
						"(JJ[JI)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_readTxPowerFromEEPromEx },
				{
						"setEEPromCKSUpdated",
						"()I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setEEPromCKSUpdated },
				{
						"getPacketTxStatusEx",
						"([JI)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getPacketTxStatusEx },
				{
						"getPacketRxStatus",
						"([JI)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getPacketRxStatus },
				{
						"setOutputPower",
						"(JJI)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setOutputPower },
				{
						"setLocalFrequecy",
						"(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setLocalFrequecy },
				{
						"setCarrierSuppression",
						"(JJJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setCarrierSuppression },
				{
						"setOperatingCountry",
						"(Ljava/lang/String;)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setOperatingCountry },
				{ "setChannel", "(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setChannel },
				{
						"getSupportedRates",
						"([IJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getSupportedRates },
				{
						"setOutputPin",
						"(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setOutputPin },
				{
						"readEEPRom16",
						"(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_readEEPRom16 },
				{
						"readSpecEEPRom16",
						"(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_readSpecEEPRom16 },
						
				{
						"writeEEPRom16",
						"(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_writeEEPRom16 },
				{
						"eepromReadByteStr",
						"(JJ[B)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_eepromReadByteStr },
				{
						"eepromWriteByteStr",
						"(JJLjava/lang/String;)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_eepromWriteByteStr },
				{ "setATParam", "(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setATParam },
				{ "getATParam", "(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getATParam },
				{
						"setXtalTrimToCr",
						"(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setXtalTrimToCr },
				{
						"queryThermoInfo",
						"([JI)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_queryThermoInfo },
				{
						"setThermoEn",
						"(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setThermoEn },
				{
						"getEEPRomSize",
						"([J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getEEPRomSize },
				{
						"getSpecEEPRomSize",
						"([J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getSpecEEPRomSize },
						
				{
						"setPnpPower",
						"(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setPnpPower },
				{
						"setAnritsu8860bTestSupportEn",
						"(J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setAnritsu8860bTestSupportEn },
				{ "writeMCR32", "(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_writeMCR32 },
				{ "readMCR32", "(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_readMCR32 }, 
				{
						"getDPDLength",
						"([J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getDPDLength },
				{ "writeDPD32", "(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_writeDPD32 },
				{ "readDPD32", "(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_readDPD32 }, 
				{
						"setDPDFromFile",
						"(Ljava/lang/String;)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_setDPDFromFile },
				// Added by mtk54046 @ 2012-01-05 for get support channel list
				{ "getSupportChannelList", "([J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_getSupportChannelList },
				{ "doCTIATestSet", "(JJ)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_doCTIATestSet },
				{ "doCTIATestGet", "(J[J)I",
						(void*) com_mediatek_engineermode_wifi_EMWifi_doCTIATestGet },
			};

static const char* const kClassPathName = "com/mediatek/engineermode/wifi";

int register_com_mediatek_engineermode_wifi_EMWifi(JNIEnv *env) {

	//	jclass EMWifi = env->FindClass(kClassPathName);
	//	LOG_FATAL_IF(EMWifi == NULL, "Unable to find class ");

	return AndroidRuntime::registerNativeMethods(env,
			"com/mediatek/engineermode/wifi/EMWifi", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	JNIEnv *env = NULL;
	jint result = -1;

	if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
		ALOGE("ERROR: GetEnv failed");
		goto bail;
	}

	assert(env != NULL);

	if (register_com_mediatek_engineermode_wifi_EMWifi(env) < 0) {
		ALOGE("ERROR: Wi-Fi native for engineermode registration failed\n");
		goto bail;
	}

	result = JNI_VERSION_1_4;

	bail: 
        return result;
}
