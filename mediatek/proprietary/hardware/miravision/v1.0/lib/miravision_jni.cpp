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

#define LOG_TAG "MIRAVISION_JNI"

#define MTK_LOG_ENABLE 1
#include <jni.h>

#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/log.h>
#include <cutils/properties.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>

#if defined(MTK_AAL_SUPPORT)
#include "IAALService.h"
#include "AALClient.h"
#endif
#include "cust_gamma.h"
#include "cust_color.h"
#include "cust_tdshp.h"

using namespace android;

#define UNUSED(expr) do { (void)(expr); } while (0)

#define JNI_CLASS_NAME "com/mediatek/miravision/setting/MiraVisionJni"

/////////////////////////////////////////////////////////////////////////////////

DISP_PQ_PARAM pqparam;
int drvID;

static void Range_set(JNIEnv* env, jobject obj, jint min, jint max, jint defaultValue)
{
    jclass clazz = env->FindClass(JNI_CLASS_NAME "$Range");
    jmethodID setMethod = env->GetMethodID(clazz, "set", "(III)V");
    env->CallVoidMethod(obj, setMethod, min, max, defaultValue);
}

/////////////////////////////////////////////////////////////////////////////////
static jboolean enablePQColor(JNIEnv *env, jobject thiz, int isEnable)
{
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);
    int bypass;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] enablePQColor(), enable[%d]", isEnable);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return JNI_FALSE;
    }

    //  set bypass COLOR to disp driver.
    if (isEnable)
    {
        bypass = 0;
        ioctl(drvID, DISP_IOCTL_PQ_SET_BYPASS_COLOR, &bypass);
    }
    else
    {
        bypass = 1;
        ioctl(drvID, DISP_IOCTL_PQ_SET_BYPASS_COLOR, &bypass);
    }

    close(drvID);

    return JNI_TRUE;
}


static int getLcmIndexOfGamma(int dev)
{
    static int lcmIdx = -1;

    if (lcmIdx == -1) {
        int ret = ioctl(dev, DISP_IOCTL_GET_LCMINDEX, &lcmIdx);
        if (ret == 0) {
            if (lcmIdx < 0 || GAMMA_LCM_MAX <= lcmIdx) {
                ALOGE("Invalid LCM index %d, GAMMA_LCM_MAX = %d", lcmIdx, GAMMA_LCM_MAX);
                lcmIdx = 0;
            }
        } else {
            ALOGE("ioctl(DISP_IOCTL_GET_LCMINDEX) return %d", ret);
            lcmIdx = 0;
        }
    }

    ALOGI("LCM index: %d/%d", lcmIdx, GAMMA_LCM_MAX);

    return lcmIdx;
}


static void setGammaIndex(int dev, int index)
{
    if (index < 0 || GAMMA_INDEX_MAX <= index)
        index = GAMMA_INDEX_DEFAULT;

    DISP_GAMMA_LUT_T *driver_gamma = new DISP_GAMMA_LUT_T;

    int lcm_id = getLcmIndexOfGamma(dev);

    const gamma_entry_t *entry = &(cust_gamma[lcm_id][index]);
    driver_gamma->hw_id = DISP_GAMMA0;
    for (int i = 0; i < DISP_GAMMA_LUT_SIZE; i++) {
        driver_gamma->lut[i] = GAMMA_ENTRY((*entry)[0][i], (*entry)[1][i], (*entry)[2][i]);
    }

    ioctl(dev, DISP_IOCTL_SET_GAMMALUT, driver_gamma);

    delete driver_gamma;
}


/////////////////////////////////////////////////////////////////////////////////
static jint getPictureMode(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int mode = -1;

    UNUSED(env);
    UNUSED(thiz);

    property_get(PQ_PIC_MODE_PROPERTY_STR, value, PQ_PIC_MODE_DEFAULT);
    mode = atoi(value);
    ALOGD("[MiraVision][JNI] getPictureMode(), property get [%d]", mode);

    return mode;
}

/////////////////////////////////////////////////////////////////////////////////

static jboolean setPictureMode(JNIEnv *env, jobject thiz, int mode)
{
    char value[PROPERTY_VALUE_MAX];
    int ret, i;

    UNUSED(env);
    UNUSED(thiz);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);
    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return JNI_FALSE;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", mode);
    ret = property_set(PQ_PIC_MODE_PROPERTY_STR, value);
    ALOGD("[MiraVision][JNI] property set... picture mode[%d]", mode);

    if (mode == PQ_PIC_MODE_STANDARD)
    {
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_standard.u4SatGain, pqparam_standard.u4Contrast, pqparam_standard.u4Brightness);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_standard.u4HueAdj[0], pqparam_standard.u4HueAdj[1], pqparam_standard.u4HueAdj[2], pqparam_standard.u4HueAdj[3]);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_standard.u4SatAdj[0], pqparam_standard.u4SatAdj[1], pqparam_standard.u4SatAdj[2], pqparam_standard.u4SatAdj[3]);

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_standard);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_standard);

        setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }
    else if (mode == PQ_PIC_MODE_VIVID)
    {
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_vivid.u4SatGain, pqparam_vivid.u4Contrast, pqparam_vivid.u4Brightness);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_vivid.u4HueAdj[0], pqparam_vivid.u4HueAdj[1], pqparam_vivid.u4HueAdj[2], pqparam_vivid.u4HueAdj[3]);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_vivid.u4SatAdj[0], pqparam_vivid.u4SatAdj[1], pqparam_vivid.u4SatAdj[2], pqparam_vivid.u4SatAdj[3]);

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_vivid);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_vivid);

        setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }
    else if (mode == PQ_PIC_MODE_USER_DEF)
    {
        // USER MODE
        memcpy(&pqparam, &pqparam_vivid, sizeof(pqparam));   // default value from standard setting.

        property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[MiraVision][JNI] property get... tdshp[%d]", i);
        pqparam.u4SHPGain = i;

        property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[MiraVision][JNI] property get... gsat[%d]", i);
        pqparam.u4SatGain = i;

        property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[MiraVision][JNI] property get... contrast[%d]", i);
        pqparam.u4Contrast = i;

        property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[MiraVision][JNI] property get... pic bright[%d]", i);
        pqparam.u4Brightness = i;

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, shp[%d], gsat[%d], cont[%d], bri[%d] ", pqparam.u4SHPGain, pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
        ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);

        i = GAMMA_INDEX_DEFAULT;
        if (property_get(GAMMA_INDEX_PROPERTY_NAME, value, NULL) > 0)
            i = atoi(value);
        ALOGD("[MiraVision][JNI] property get... gamma[%d]", i);
        setGammaIndex(drvID, i);
    }
    else
    {
        ALOGE("[MiraVision][JNI] unknown picture mode!!");

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_standard);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_standard);

        setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }

    close(drvID);

    return JNI_TRUE;
}


/////////////////////////////////////////////////////////////////////////////////

static jboolean setPQColorRegion(JNIEnv *env, jobject thiz, int isEnable, int startX, int startY, int endX, int endY)
{
    DISP_PQ_WIN_PARAM win_param;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setPQColorRegion(), en[%d], sX[%d], sY[%d], eX[%d], eY[%d]", isEnable, startX, startY, endX, endY);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return JNI_FALSE;
    }

    if (isEnable)
    {
        win_param.split_en = 1;
        win_param.start_x = startX;
        win_param.start_y = startY;
        win_param.end_x = endX;
        win_param.end_y = endY;
    }
    else
    {
        win_param.split_en = 0;
    }

    ioctl(drvID, DISP_IOCTL_PQ_SET_WINDOW, &win_param);

    close(drvID);

    return JNI_TRUE;
}

/////////////////////////////////////////////////////////////////////////////////
static void getContrastIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, PQ_CONTRAST_INDEX_RANGE_NUM - 1, atoi(PQ_CONTRAST_INDEX_DEFAULT));
}

static jint getContrastIndex(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    UNUSED(env);
    UNUSED(thiz);

    property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[MiraVision][JNI] getContrastIndex(), property get [%d]", index);

    return index;
}

static void setContrastIndex(JNIEnv *env, jobject thiz, int index)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setContrastIndex...index[%d]", index);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_CONTRAST_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4Contrast = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);


    close(drvID);
}

/////////////////////////////////////////////////////////////////////////////////
static void getSaturationIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, PQ_GSAT_INDEX_RANGE_NUM - 1, atoi(PQ_GSAT_INDEX_DEFAULT));
}

static jint getSaturationIndex(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    UNUSED(env);
    UNUSED(thiz);

    property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[MiraVision][JNI] getSaturationIndex(), property get [%d]", index);

    return index;
}

static void setSaturationIndex(JNIEnv *env, jobject thiz, int index)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setSaturationIndex...index[%d]", index);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_GSAT_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4SatGain = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);


    close(drvID);
}

/////////////////////////////////////////////////////////////////////////////////
static void getPicBrightnessIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, PQ_PIC_BRIGHT_INDEX_RANGE_NUM - 1, atoi(PQ_PIC_BRIGHT_INDEX_DEFAULT));
}

static jint getPicBrightnessIndex(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    UNUSED(env);
    UNUSED(thiz);

    property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[MiraVision][JNI] getPicBrightnessIndex(), property get [%d]", index);

    return index;
}

static void setPicBrightnessIndex(JNIEnv *env, jobject thiz, int index)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setPicBrightnessIndex...index[%d]", index);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_PIC_BRIGHT_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4Brightness = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);


    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
    //ALOGD("[MiraVision][JNI] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);

    close(drvID);
}

static jboolean setTuningMode(JNIEnv *env, jobject thiz, int mode)
{
    UNUSED(env);
    UNUSED(thiz);

    if (mode)
    {
        property_set(PQ_DBG_SHP_TUNING_STR, "1");
    }
    else
    {
        property_set(PQ_DBG_SHP_TUNING_STR, "0");
    }
    return JNI_TRUE;
}

/////////////////////////////////////////////////////////////////////////////////
static void getSharpnessIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, PQ_TDSHP_INDEX_RANGE_NUM - 1, atoi(PQ_TDSHP_INDEX_DEFAULT));
}

static jint getSharpnessIndex(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    UNUSED(env);
    UNUSED(thiz);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return -1;
    }

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    index = pqparam.u4SHPGain;

    //property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_INDEX_DEFAULT);
    //index = atoi(value);
    ALOGD("[MiraVision][JNI] getSharpnessIndex(), property get...tdshp[%d]", index);

    close(drvID);

    return index;
}

static void setSharpnessIndex(JNIEnv *env, jobject thiz , int index)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setSharpnessIndex...index[%d]", index);

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_TDSHP_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4SHPGain = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    close(drvID);
}

/////////////////////////////////////////////////////////////////////////////////
static void getDynamicContrastIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, PQ_ADL_INDEX_RANGE_NUM, atoi(PQ_ADL_INDEX_DEFAULT));
}

static jint getDynamicContrastIndex(JNIEnv *env, jobject thiz)
{
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    UNUSED(env);
    UNUSED(thiz);

    property_get(PQ_ADL_PROPERTY_STR, value, PQ_ADL_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[MiraVision][JNI] getDynamicContrastIndex(), property get [%d]", index);

    return index;
}

static void setDynamicContrastIndex(JNIEnv *env, jobject thiz, int index)
{
    char value[PROPERTY_VALUE_MAX];
    int ret;

    UNUSED(env);
    UNUSED(thiz);

    ALOGD("[MiraVision][JNI] setDynamicContrastIndex...index[%d]", index);

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_ADL_PROPERTY_STR, value);
}

static void nativeGetGammaIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    UNUSED(clz);

    Range_set(env, range, 0, GAMMA_INDEX_MAX - 1, GAMMA_INDEX_DEFAULT);
}


static void nativeSetGammaIndex(JNIEnv* env, jclass clz, jint index)
{
    int dev = open("/proc/mtk_mira", O_RDONLY, 0);

    UNUSED(env);
    UNUSED(clz);

    if (dev >= 0) {
        setGammaIndex(dev, index);
        close(dev);
    }
}


static void nativeSetAALFunction(JNIEnv* env, jclass clz, jint func)
{
    UNUSED(env);
    UNUSED(clz);

#if defined(MTK_AAL_SUPPORT)
    AALClient::getInstance().setFunction(func);
#else
    UNUSED(func);
    ALOGE("nativeSetAALFunction(): MTK_AAL_SUPPORT disabled");
#endif
}

// OD
static jboolean enableODDemo(JNIEnv *env, jobject thiz, int isEnable)
{
    UNUSED(env);
    UNUSED(thiz);

    // enable 2 for query OD support
    if (isEnable == 2)
    {
#ifdef MTK_OD_SUPPORT
        return JNI_TRUE;
#endif
        return JNI_FALSE;
    }

    drvID = open("/proc/mtk_mira", O_RDONLY, 0);
    DISP_OD_CMD cmd;

    if (drvID < 0)
    {
        ALOGE("[MiraVision][JNI] open device fail!!");
        return JNI_FALSE;
    }

    memset(&cmd, 0, sizeof(cmd));
    cmd.size = sizeof(cmd);
    cmd.type = 2;

    if (isEnable)
    {
        cmd.param0 = 1;
    }
    else
    {
        cmd.param0 = 0;
    }

    ioctl(drvID, DISP_IOCTL_OD_CTL, &cmd);

    close(drvID);

    return JNI_TRUE;
}


/////////////////////////////////////////////////////////////////////////////////

//JNI register
////////////////////////////////////////////////////////////////
static const char *classPathName = "com/mediatek/miravision/setting/MiraVisionJni";

static JNINativeMethod g_methods[] = {

  /* PQ related APIs */
  //{"nativeEnablePQColor", "(I)Z", (void*)enablePQColor},
  //{"nativeGetPictureMode", "()I", (void*)getPictureMode},
  //{"nativeSetPictureMode", "(I)Z", (void*)setPictureMode},
  //{"nativeSetPQColorRegion", "(IIIII)Z", (void*)setPQColorRegion},
  //{"nativeGetContrastIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)getContrastIndexRange},
  //{"nativeGetContrastIndex", "()I", (void*)getContrastIndex},
  //{"nativeSetContrastIndex", "(I)V", (void*)setContrastIndex},
  //{"nativeGetSaturationIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)getSaturationIndexRange},
  //{"nativeGetSaturationIndex", "()I", (void*)getSaturationIndex},
  //{"nativeSetSaturationIndex", "(I)V", (void*)setSaturationIndex},
  //{"nativeGetPicBrightnessIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)getPicBrightnessIndexRange},
  //{"nativeGetPicBrightnessIndex", "()I", (void*)getPicBrightnessIndex},
  //{"nativeSetPicBrightnessIndex", "(I)V", (void*)setPicBrightnessIndex},
  //{"nativeSetTuningMode",  "(I)Z", (void*)setTuningMode },
  //{"nativeGetSharpnessIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)getSharpnessIndexRange},
  //{"nativeGetSharpnessIndex", "()I", (void*)getSharpnessIndex},
  //{"nativeSetSharpnessIndex", "(I)V", (void*)setSharpnessIndex},
  //{"nativeGetDynamicContrastIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)getDynamicContrastIndexRange},
  //{"nativeGetDynamicContrastIndex", "()I", (void*)getDynamicContrastIndex},
  //{"nativeSetDynamicContrastIndex", "(I)V", (void*)setDynamicContrastIndex},
  //{"nativeGetGammaIndexRange", "(L" JNI_CLASS_NAME "$Range;)V", (void*)nativeGetGammaIndexRange},
  //{"nativeSetGammaIndex", "(I)V", (void*)nativeSetGammaIndex},
  {"nativeSetAALFunction", "(I)V", (void*)nativeSetAALFunction},
  //{"nativeEnableODDemo", "(I)Z", (void*)enableODDemo},
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    UNUSED(reserved);

    ALOGI("JNI_OnLoad");

    if (JNI_OK != vm->GetEnv((void **)&env, JNI_VERSION_1_4)) {
        ALOGE("ERROR: GetEnv failed");
        goto bail;
    }

    if (!registerNativeMethods(env, classPathName, g_methods, sizeof(g_methods) / sizeof(g_methods[0]))) {
        ALOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

bail:
    return result;
}

