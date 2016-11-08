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

#define LOG_TAG "MJC_JNI"

#define MTK_LOG_ENABLE 1
#include <jni.h>

#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/log.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>
#include <cutils/properties.h>

using namespace android;

typedef enum {
    MJC_MJC_fb_Adj = 0,
    MJC_MJC_demo,
    MJC_TuningDimension
} MJC_TuningIndex_t;

#define MJC_MJC_FALLBACK     (16)// mjc fallback
#define MJC_DEMO_MODE        (2)// demo mode

int drvID = -1;

const static jint g_u4Range[MJC_TuningDimension] =
{
 MJC_MJC_FALLBACK, MJC_DEMO_MODE
};

unsigned int u4Indics[MJC_TuningDimension];

static jint getRange(MJC_TuningIndex_t a_eIndex)
{
    return g_u4Range[a_eIndex];
}

#define SUPPORT_CLEARMOTION_PATH0 "/storage/sdcard0/SUPPORT_CLEARMOTION"
#define SUPPORT_CLEARMOTION_PATH1 "/storage/sdcard1/SUPPORT_CLEARMOTION"
#define TUNING_DATA_NUM sizeof(short)*2

static int getMjcIndex(MJC_TuningIndex_t a_eIndex)
{
    unsigned short value[2];
    int num = 0;
    FILE *fp;

    u4Indics[MJC_MJC_fb_Adj] = 16;
    u4Indics[MJC_MJC_demo] = 0;
    fp = fopen(SUPPORT_CLEARMOTION_PATH0, "rb+"); //check sdcard first
    if (!fp)
    { //check phone storage
        ALOGE("sdcard0 SUPPORT_CLEARMOTION not exist");
        fp = fopen(SUPPORT_CLEARMOTION_PATH1, "rb+");
        if(!fp)
        {
            ALOGE("sdcard1 SUPPORT_CLEARMOTION not exist");
        }
    }

    if (fp) {
        num = fread(value, 1, TUNING_DATA_NUM, fp);
        if(num==TUNING_DATA_NUM)
        {
            ALOGD("value %x %x",value[0],value[1]);
            value[0]=value[0]>>8;
            value[1]=value[1]>>8;
            u4Indics[MJC_MJC_fb_Adj] = (value[0]+1)/MJC_MJC_FALLBACK; //0~255 mappging to 0~MJC_MJC_FALLBACK
            if(value[1]<=2)
                u4Indics[MJC_MJC_demo] = value[1];
        }
        else
        {
            value[0]=u4Indics[MJC_MJC_fb_Adj];
            value[1]=u4Indics[MJC_MJC_demo];
            fwrite(value, 1, TUNING_DATA_NUM, fp);
            ALOGD("set default");
        }
        fclose(fp);
    }

    ALOGE("getMjcIndex[%d]=%d",a_eIndex, u4Indics[a_eIndex]);
    return (int)u4Indics[a_eIndex];
}

static jboolean setMjcIndex(MJC_TuningIndex_t a_eIndex , unsigned int a_u4Index)
{
    unsigned short value=0;
    char uc_value[PROPERTY_VALUE_MAX];
    int num = 0;
    FILE *fp;

    value = u4Indics[a_eIndex] = a_u4Index;
    if(a_eIndex == MJC_MJC_fb_Adj) //0~MJC_MJC_FALLBACK mapping to 0~255
    {
        value = a_u4Index*MJC_MJC_FALLBACK;
        if(value >= 0x100) value = 0xFF;
    }
    value = value<<8; //save as big edian
    fp = fopen(SUPPORT_CLEARMOTION_PATH0, "rb+"); //check sdcard first
    if (!fp)
    { //check phone storage
        ALOGE("sdcard0 SUPPORT_CLEARMOTION not exist");
        fp = fopen(SUPPORT_CLEARMOTION_PATH1, "rb+");
        if(!fp)
        {
            ALOGE("sdcard1 SUPPORT_CLEARMOTION not exist");
        }
    }

    if (fp) {
        fseek(fp, (a_eIndex-MJC_MJC_fb_Adj)*sizeof(short), SEEK_SET);
        fwrite((void*)&value, 1, sizeof(short), fp);
        fclose(fp);
    }
    else
    {   //for MiraVision Setting.apk, if receive index 3, then change to horizontal demo mode "1"
        if(a_eIndex == MJC_MJC_demo)
        {
            if(a_u4Index == 3)
            {
                u4Indics[a_eIndex] = a_u4Index = 1;
            }
            sprintf(uc_value,"%d",a_u4Index);
            property_set("sys.display.clearMotion.demo", uc_value);
        }
    }

    ALOGE("setMjcIndex[%d]=%d",a_eIndex, u4Indics[a_eIndex]);
    return JNI_TRUE;
}

static jint getFallbackRange(JNIEnv *env, jobject thiz)
{
    return getRange(MJC_MJC_fb_Adj);
}

static jint getFallbackIndex(JNIEnv *env, jobject thiz)
{
    return getMjcIndex(MJC_MJC_fb_Adj);
}

static jboolean setFallbackIndex(JNIEnv *env, jobject thiz , int index)
{
    return setMjcIndex(MJC_MJC_fb_Adj , index);
}

static jint getDemoMode(JNIEnv *env, jobject thiz)
{
    return getMjcIndex(MJC_MJC_demo);
}

static jboolean setDemoMode(JNIEnv *env, jobject thiz , int index)
{
    return setMjcIndex(MJC_MJC_demo , index);
}

/////////////////////////////////////////////////////////////////////////////////

//JNI register
////////////////////////////////////////////////////////////////
static const char *classPathName = "com/mediatek/galleryfeature/clearmotion/ClearMotionQualityJni";

static JNINativeMethod g_methods[] = {
  {"nativeGetFallbackIndex",  "()I", (void*)getFallbackIndex },
  {"nativeSetFallbackIndex",  "(I)Z", (void*)setFallbackIndex},
  {"nativeGetFallbackRange",  "()I", (void*)getFallbackRange },
  {"nativeGetDemoMode",  "()I", (void*)getDemoMode },
  {"nativeSetDemoMode",  "(I)Z", (void*)setDemoMode}
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

