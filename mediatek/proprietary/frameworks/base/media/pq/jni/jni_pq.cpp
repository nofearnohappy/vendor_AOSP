/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "JNI_PQ"

#define MTK_LOG_ENABLE 1
#include <jni_pq.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <errno.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/log.h>
#include <cutils/properties.h>

#ifdef JNI_PQ_PQSERVICE
#include "PQClient.h"
#endif

#ifdef MTK_MIRAVISION_IMAGE_DC_SUPPORT
#include <PQDCHistogram.h>
#endif

#ifdef MTK_MIRAVISION_API_SUPPORT
#include "cust_color.h"
#include "cust_tdshp.h"
#include "cust_gamma.h"
#endif


using namespace android;

#define UNUSED(expr) do { (void)(expr); } while (0)
#define JNI_PQ_CLASS_NAME "com/mediatek/pq/PictureQuality"

int drvID = -1;
int ret = 0;
DISP_PQ_PARAM pqparam;
Mutex mLock;

static int CAPABILITY_MASK_COLOR       = 0x00000001;
static int CAPABILITY_MASK_SHARPNESS   = 0x00000002;
static int CAPABILITY_MASK_GAMMA       = 0x00000004;
static int CAPABILITY_MASK_DC          = 0x00000008;
static int CAPABILITY_MASK_OD          = 0x00000010;
/////////////////////////////////////////////////////////////////////////////////
static jint getCapability(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    int cap = (CAPABILITY_MASK_COLOR |
               CAPABILITY_MASK_SHARPNESS |
               CAPABILITY_MASK_GAMMA |
               CAPABILITY_MASK_DC);

    #ifdef MTK_OD_SUPPORT
        cap |= CAPABILITY_MASK_OD;
    #endif


    return cap;

#else

    ALOGE("[JNI_PQ] getCapability(), not supported!");
    return 0;

#endif
}

static void setCameraPreviewMode(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setDISPScenario(PQClient::SCENARIO_ISP_PREVIEW);
#else
    int drvID = -1;

    drvID = open("/proc/mtk_mira", O_RDONLY, 0);
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mira failed!! ");
        drvID = open("/proc/mtk_mdp_color", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_color failed!! ");
        drvID = open("/proc/mtk_mdp_cmdq", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_cmdq failed!! ");
        drvID = open("/dev/mtk_disp", O_RDONLY, 0);
    }
    if(drvID < 0)
    {
        drvID = open("/proc/mtk_disp", O_RDONLY, 0);
    }

    ioctl(drvID, DISP_IOCTL_GET_PQ_CAM_PARAM, &pqparam);

    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);

    if (drvID >= 0)
    {
        close(drvID);
    }
#endif

    UNUSED(env);
    UNUSED(thiz);

    return;
}

static void setGalleryNormalMode(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setDISPScenario(PQClient::SCENARIO_PICTURE);
#else
    int drvID = -1;

    drvID = open("/proc/mtk_mira", O_RDONLY, 0);
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mira failed!! ");
        drvID = open("/proc/mtk_mdp_color", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_color failed!! ");
        drvID = open("/proc/mtk_mdp_cmdq", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_cmdq failed!! ");
        drvID = open("/dev/mtk_disp", O_RDONLY, 0);
    }
    if(drvID < 0)
    {
        drvID = open("/proc/mtk_disp", O_RDONLY, 0);
    }

    ioctl(drvID, DISP_IOCTL_GET_PQ_GAL_PARAM, &pqparam);

    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);

    if (drvID >= 0)
    {
        close(drvID);
    }
#endif
    UNUSED(env);
    UNUSED(thiz);

    return;
}

static void setVideoPlaybackMode(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setDISPScenario(PQClient::SCENARIO_VIDEO);
#else
    int drvID = -1;

    drvID = open("/proc/mtk_mira", O_RDONLY, 0);

    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mira failed!! ");
        drvID = open("/proc/mtk_mdp_color", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_color failed!! ");
        drvID = open("/proc/mtk_mdp_cmdq", O_RDONLY, 0);
    }
    if (drvID < 0)
    {
        //ALOGE("[PQ JNI] setCameraPreviewMode, open /proc/mtk_mdp_cmdq failed!! ");
        drvID = open("/dev/mtk_disp", O_RDONLY, 0);
    }
    if(drvID < 0)
    {
        drvID = open("/proc/mtk_disp", O_RDONLY, 0);
    }

    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);

    if (drvID >= 0)
    {
        close(drvID);
    }
#endif
    UNUSED(env);
    UNUSED(thiz);

    return;
}

#ifdef MTK_MIRAVISION_IMAGE_DC_SUPPORT
static void Hist_set(JNIEnv* env, jobject obj, jint index, jint value)
{
    jclass clazz = env->FindClass(JNI_PQ_CLASS_NAME "$Hist");
    jmethodID setMethod = env->GetMethodID(clazz, "set", "(II)V");
    env->CallVoidMethod(obj, setMethod, index, value);

    env->DeleteLocalRef(clazz);
}
#endif

static void getDynamicContrastHistogram(JNIEnv* env, jclass clz, jbyteArray srcBuffer, jint srcWidth, jint srcHeight, jobject hist)
{
    Mutex::Autolock autoLock(mLock);

#ifdef MTK_MIRAVISION_IMAGE_DC_SUPPORT
    CPQDCHistogram *pDCHist = new CPQDCHistogram;
    DynCInput   input;
    DynCOutput  output;
    int i;

    input.pSrcFB = (unsigned char*)env->GetByteArrayElements(srcBuffer, 0);
    input.iWidth = srcWidth;
    input.iHeight = srcHeight;

    pDCHist->Main(input, &output);
    for (i = 0; i < DCHIST_INFO_NUM; i++)
    {
        Hist_set(env, hist, i, output.Info[i]);
    }

    env->ReleaseByteArrayElements(srcBuffer, (jbyte*)input.pSrcFB, 0);
    delete pDCHist;
#else
    ALOGE("[JNI_PQ] getDynamicContrastHistogram(), not supported!");

    UNUSED(env);
    UNUSED(srcBuffer);
    UNUSED(srcWidth);
    UNUSED(srcHeight);
    UNUSED(hist);
#endif
    UNUSED(clz);
}

static void Range_set(JNIEnv* env, jobject obj, jint min, jint max, jint defaultValue)
{
    jclass clazz = env->FindClass(JNI_PQ_CLASS_NAME "$Range");
    jmethodID setMethod = env->GetMethodID(clazz, "set", "(III)V");
    env->CallVoidMethod(obj, setMethod, min, max, defaultValue);
}

#ifdef MTK_MIRAVISION_API_SUPPORT
static int _getLcmIndexOfGamma(int dev)
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


static void _setGammaIndex(int dev, int index)
{
    if (index < 0 || GAMMA_INDEX_MAX <= index)
        index = GAMMA_INDEX_DEFAULT;

    DISP_GAMMA_LUT_T *driver_gamma = new DISP_GAMMA_LUT_T;

    int lcm_id = _getLcmIndexOfGamma(dev);

    const gamma_entry_t *entry = &(cust_gamma[lcm_id][index]);
    driver_gamma->hw_id = DISP_GAMMA0;
    for (int i = 0; i < DISP_GAMMA_LUT_SIZE; i++) {
        driver_gamma->lut[i] = GAMMA_ENTRY((*entry)[0][i], (*entry)[1][i], (*entry)[2][i]);
    }

    ioctl(dev, DISP_IOCTL_SET_GAMMALUT, driver_gamma);

    delete driver_gamma;
}
#endif

static jboolean enableColor(JNIEnv *env, jobject thiz, int isEnable);
static jboolean enableSharpness(JNIEnv *env, jobject thiz, int isEnable);
static jboolean enableDynamicContrast(JNIEnv *env, jobject thiz, int isEnable);
static jboolean enableGamma(JNIEnv *env, jobject thiz, int isEnable);
static jboolean enableOD(JNIEnv *env, jobject thiz, int isEnable);

/////////////////////////////////////////////////////////////////////////////////
static jboolean enablePQ(JNIEnv *env, jobject thiz, int isEnable)
{
    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    if (isEnable)
    {
        enableColor(env, thiz, 1);
        enableSharpness(env, thiz, 1);
        enableDynamicContrast(env, thiz, 1);
        enableOD(env, thiz, 1);
    }
    else
    {
        enableColor(env, thiz, 0);
        enableSharpness(env, thiz, 0);
        enableDynamicContrast(env, thiz, 0);
        enableOD(env, thiz, 0);
    }

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] setPictureMode(), not supported!");
    return JNI_FALSE;

#endif
}

/////////////////////////////////////////////////////////////////////////////////
static jboolean enableColor(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::DISPLAY_COLOR, isEnable);
    #else
        drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);
        int bypass;

        ALOGD("[JNI_PQ] enableColor(), enable[%d]", isEnable);

        if (drvID < 0)
        {
            ALOGE("[JNI_PQ] open device fail!!");
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

    #endif
    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableColor(), not supported!");
    return JNI_FALSE;

#endif
}

/////////////////////////////////////////////////////////////////////////////////
static jboolean enableSharpness(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::SHARPNESS, isEnable);
    #else
        char value[PROPERTY_VALUE_MAX];
        int ret;

        snprintf(value, PROPERTY_VALUE_MAX, "%d\n", isEnable);
        ret = property_set(PQ_DBG_SHP_EN_STR, value);
        ALOGD("[JNI_PQ] property set... enableSharpness[%d]", isEnable);
    #endif

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableSharpness(), not supported!");
    return JNI_FALSE;

#endif
}

/////////////////////////////////////////////////////////////////////////////////
static jboolean enableDynamicContrast(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::DYNAMIC_CONTRAST, isEnable);
    #else
        char value[PROPERTY_VALUE_MAX];
        int ret;

        snprintf(value, PROPERTY_VALUE_MAX, "%d\n", isEnable);
        ret = property_set(PQ_ADL_PROPERTY_STR, value);
        ALOGD("[JNI_PQ] property set... enableDynamicContrast[%d]", isEnable);
    #endif

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableDynamicContrast(), not supported!");
    return JNI_FALSE;

#endif
}

static jboolean enableColorEffect(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::CONTENT_COLOR_VIDEO, isEnable);
    #else
        char value[PROPERTY_VALUE_MAX];
        int ret;

        snprintf(value, PROPERTY_VALUE_MAX, "%d\n", isEnable);
        ret = property_set(PQ_MDP_COLOR_EN_STR  , value);
        ALOGD("[JNI_PQ] property set... enableColorEffect[%d]", isEnable);
    #endif

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableColorEffect(), not supported!");
    return JNI_FALSE;

#endif
}
/////////////////////////////////////////////////////////////////////////////////
static jboolean enableGamma(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::DISPLAY_GAMMA, isEnable);
    #else
        drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);
        if (drvID < 0)
        {
            ALOGE("[JNI_PQ] open device fail!!");
            return JNI_FALSE;
        }

        if (isEnable)
        {
            char value[PROPERTY_VALUE_MAX];
            char dvalue[PROPERTY_VALUE_MAX];
            int index;

            snprintf(dvalue, PROPERTY_VALUE_MAX, "%d\n", GAMMA_INDEX_DEFAULT);
            property_get(GAMMA_INDEX_PROPERTY_NAME, value, dvalue);
            index = atoi(value);
            _setGammaIndex(drvID, index);
        }
        else
        {
            _setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
        }

        close(drvID);
    #endif

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableGamma(), not supported!");
    return JNI_FALSE;

#endif
}

/////////////////////////////////////////////////////////////////////////////////
static jint getPictureMode(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    char value[PROPERTY_VALUE_MAX];
    int mode = -1;

    property_get(PQ_PIC_MODE_PROPERTY_STR, value, PQ_PIC_MODE_DEFAULT);
    mode = atoi(value);
    ALOGD("[JNI_PQ] getPictureMode(), property get [%d]", mode);

    return mode;

#else

    ALOGE("[JNI_PQ] getPictureMode(), not supported!");
    return 0;

#endif
}

/////////////////////////////////////////////////////////////////////////////////

static jboolean setPictureMode(JNIEnv *env, jobject thiz, int mode)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setPQMode(mode);
#else
    char value[PROPERTY_VALUE_MAX];
    int ret, i;

    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);
    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return JNI_FALSE;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", mode);
    ret = property_set(PQ_PIC_MODE_PROPERTY_STR, value);
    ALOGD("[JNI_PQ] property set... picture mode[%d]", mode);

    if (mode == PQ_PIC_MODE_STANDARD)
    {
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_standard.u4SatGain, pqparam_standard.u4Contrast, pqparam_standard.u4Brightness);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_standard.u4HueAdj[0], pqparam_standard.u4HueAdj[1], pqparam_standard.u4HueAdj[2], pqparam_standard.u4HueAdj[3]);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_standard.u4SatAdj[0], pqparam_standard.u4SatAdj[1], pqparam_standard.u4SatAdj[2], pqparam_standard.u4SatAdj[3]);

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_standard);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_standard);

        _setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }
    else if (mode == PQ_PIC_MODE_VIVID)
    {
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, gsat[%d], cont[%d], bri[%d] ", pqparam_vivid.u4SatGain, pqparam_vivid.u4Contrast, pqparam_vivid.u4Brightness);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam_vivid.u4HueAdj[0], pqparam_vivid.u4HueAdj[1], pqparam_vivid.u4HueAdj[2], pqparam_vivid.u4HueAdj[3]);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam_vivid.u4SatAdj[0], pqparam_vivid.u4SatAdj[1], pqparam_vivid.u4SatAdj[2], pqparam_vivid.u4SatAdj[3]);

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_vivid);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_vivid);

        _setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }
    else if (mode == PQ_PIC_MODE_USER_DEF)
    {
        // USER MODE
        memcpy(&pqparam, &pqparam_vivid, sizeof(pqparam));   // default value from standard setting.

        property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[JNI_PQ] property get... tdshp[%d]", i);
        pqparam.u4SHPGain = i;

        property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[JNI_PQ] property get... gsat[%d]", i);
        pqparam.u4SatGain = i;

        property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[JNI_PQ] property get... contrast[%d]", i);
        pqparam.u4Contrast = i;

        property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
        i = atoi(value);
        ALOGD("[JNI_PQ] property get... pic bright[%d]", i);
        pqparam.u4Brightness = i;

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, shp[%d], gsat[%d], cont[%d], bri[%d] ", pqparam.u4SHPGain, pqparam.u4SatGain, pqparam.u4Contrast, pqparam.u4Brightness);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, hue0[%d], hue1[%d], hue2[%d], hue3[%d] ", pqparam.u4HueAdj[0], pqparam.u4HueAdj[1], pqparam.u4HueAdj[2], pqparam.u4HueAdj[3]);
        ALOGD("[JNI_PQ] --DISP_IOCTL_SET_PQPARAM, sat0[%d], sat1[%d], sat2[%d], sat3[%d] ", pqparam.u4SatAdj[0], pqparam.u4SatAdj[1], pqparam.u4SatAdj[2], pqparam.u4SatAdj[3]);

        i = GAMMA_INDEX_DEFAULT;
        if (property_get(GAMMA_INDEX_PROPERTY_NAME, value, NULL) > 0)
            i = atoi(value);
        ALOGD("[JNI_PQ] property get... gamma[%d]", i);
        _setGammaIndex(drvID, i);
    }
    else
    {
        ALOGE("[JNI_PQ] unknown picture mode!!");

        ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam_standard);
        ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam_standard);

        _setGammaIndex(drvID, GAMMA_INDEX_DEFAULT);
    }

    close(drvID);

#endif
    return JNI_TRUE;

#else

    UNUSED(mode);

    ALOGE("[JNI_PQ] setPictureMode(), not supported!");
    return JNI_FALSE;

#endif


}

/////////////////////////////////////////////////////////////////////////////////

static jboolean setColorRegion(JNIEnv *env, jobject thiz, int isEnable, int startX, int startY, int endX, int endY)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    DISP_PQ_WIN_PARAM win_param;

    ALOGD("[JNI_PQ] setColorRegion(), en[%d], sX[%d], sY[%d], eX[%d], eY[%d]", isEnable, startX, startY, endX, endY);

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
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setColorRegion(win_param.split_en,win_param.start_x,win_param.start_y,win_param.end_x,win_param.end_y);
#else
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return JNI_FALSE;
    }
    ioctl(drvID, DISP_IOCTL_PQ_SET_WINDOW, &win_param);

    close(drvID);
#endif

    return JNI_TRUE;

#else

    ALOGE("[JNI_PQ] setColorRegion(), not supported!");
    return JNI_FALSE;

#endif

}

/////////////////////////////////////////////////////////////////////////////////
static void getContrastIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_CONTRAST_INDEX_RANGE_NUM - 1, atoi(PQ_CONTRAST_INDEX_DEFAULT));
#else
    ALOGE("[JNI_PQ] getContrastIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}

static jint getContrastIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_CONTRAST_PROPERTY_STR, value, PQ_CONTRAST_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getContrastIndex(), property get [%d]", index);

    return index;

#else

    ALOGE("[JNI_PQ] getContrastIndex(), not supported!");
    return 0;

#endif
}

static void setContrastIndex(JNIEnv *env, jobject thiz, int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setContrastIndex...index[%d]", index);

#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setPQIndex(index, PQClient::SCENARIO_PICTURE, TDSHP_FLAG_NORMAL,SET_PQ_CONTRAST);
    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_CONTRAST_PROPERTY_STR, value);
#else
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_CONTRAST_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4Contrast = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    close(drvID);

#endif

#else

    ALOGE("[JNI_PQ] setContrastIndex(), not supported!");
    UNUSED(index);

#endif

}

/////////////////////////////////////////////////////////////////////////////////
static void getSaturationIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_GSAT_INDEX_RANGE_NUM - 1, atoi(PQ_GSAT_INDEX_DEFAULT));
#else
    ALOGE("[JNI_PQ] getSaturationIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif

}

static jint getSaturationIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_GSAT_PROPERTY_STR, value, PQ_GSAT_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getSaturationIndex(), property get [%d]", index);

    return index;
#else
    ALOGE("[JNI_PQ] getSaturationIndex(), not supported!");
    return 0;
#endif
}

static void setSaturationIndex(JNIEnv *env, jobject thiz, int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT

    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setSaturationIndex...index[%d]", index);

#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setPQIndex(index, PQClient::SCENARIO_PICTURE, TDSHP_FLAG_NORMAL,SET_PQ_SAT_GAIN);
    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_GSAT_PROPERTY_STR, value);
#else
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_GSAT_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4SatGain = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    close(drvID);
#endif

#else

    UNUSED(index);

    ALOGE("[JNI_PQ] setSaturationIndex(), not supported!");

#endif
}

/////////////////////////////////////////////////////////////////////////////////
static void getPicBrightnessIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_PIC_BRIGHT_INDEX_RANGE_NUM - 1, atoi(PQ_PIC_BRIGHT_INDEX_DEFAULT));
#else
    ALOGE("[JNI_PQ] getPicBrightnessIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}

static jint getPicBrightnessIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_PIC_BRIGHT_PROPERTY_STR, value, PQ_PIC_BRIGHT_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getPicBrightnessIndex(), property get [%d]", index);

    return index;
#else
    ALOGE("[JNI_PQ] getPicBrightnessIndex(), not supported!");
    return 0;
#endif
}

static void setPicBrightnessIndex(JNIEnv *env, jobject thiz, int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setPicBrightnessIndex...index[%d]", index);
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setPQIndex(index, PQClient::SCENARIO_PICTURE, TDSHP_FLAG_NORMAL,SET_PQ_BRIGHTNESS);
    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_PIC_BRIGHT_PROPERTY_STR, value);
#else
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_PIC_BRIGHT_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4Brightness = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    close(drvID);
#endif
#else
    ALOGE("[JNI_PQ] setPicBrightnessIndex(), not supported!");
    UNUSED(index);
#endif
}

/////////////////////////////////////////////////////////////////////////////////
static void getSharpnessIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_TDSHP_INDEX_RANGE_NUM - 1, atoi(PQ_TDSHP_INDEX_DEFAULT));
#else
    ALOGE("[JNI_PQ] getSharpnessIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}

static jint getSharpnessIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_TDSHP_PROPERTY_STR, value, PQ_TDSHP_STANDARD_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getSharpnessIndex(), property get [%d]", index);

    return index;
#else
    ALOGE("[JNI_PQ] getSharpnessIndex(), not supported!");
    return 0;
#endif
}

static void setSharpnessIndex(JNIEnv *env, jobject thiz , int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setSharpnessIndex...index[%d]", index);
#ifdef JNI_PQ_PQSERVICE
    PQClient &client(PQClient::getInstance());
    client.setPQIndex(index, PQClient::SCENARIO_PICTURE, TDSHP_FLAG_NORMAL,SET_PQ_SHP_GAIN);
    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_TDSHP_PROPERTY_STR, value);
#else
    drvID = open(PQ_DEVICE_NODE, O_RDONLY, 0);

    if (drvID < 0)
    {
        ALOGE("[JNI_PQ] open device fail!!");
        return;
    }

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_TDSHP_PROPERTY_STR, value);

    ioctl(drvID, DISP_IOCTL_GET_PQPARAM, &pqparam);

    pqparam.u4SHPGain = index;
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    close(drvID);
#endif
#else
    UNUSED(index);
    ALOGE("[JNI_PQ] setSharpnessIndex(), not supported!");
#endif
}

/////////////////////////////////////////////////////////////////////////////////
static void getDynamicContrastIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_ADL_INDEX_RANGE_NUM, atoi(PQ_ADL_INDEX_DEFAULT));
#else
    ALOGE("[JNI_PQ] getDynamicContrastIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}

static jint getDynamicContrastIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_ADL_PROPERTY_STR, value, PQ_ADL_INDEX_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getDynamicContrastIndex(), property get [%d]", index);

    return index;
#else
    ALOGE("[JNI_PQ] getDynamicContrastIndex(), not supported!");
    return 0;
#endif
}

static void setDynamicContrastIndex(JNIEnv *env, jobject thiz, int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setDynamicContrastIndex...index[%d]", index);

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_ADL_PROPERTY_STR, value);
#else
    UNUSED(index);
    ALOGE("[JNI_PQ] setDynamicContrastIndex(), not supported!");
#endif
}

static void getColorEffectIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, PQ_MDP_COLOR_EN_INDEX_RANGE_NUM, atoi(PQ_MDP_COLOR_EN_DEFAULT));
#else
    ALOGE("[JNI_PQ] getColorEffectIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}

static jint getColorEffectIndex(JNIEnv *env, jobject thiz)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int index = -1;

    property_get(PQ_MDP_COLOR_EN_STR, value, PQ_MDP_COLOR_EN_DEFAULT);
    index = atoi(value);
    ALOGD("[JNI_PQ] getColorEffectIndex(), property get [%d]", index);

    return index;
#else
    ALOGE("[JNI_PQ] getColorEffectIndex(), not supported!");
    return 0;
#endif
}

static void setColorEffectIndex(JNIEnv *env, jobject thiz, int index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    char value[PROPERTY_VALUE_MAX];
    int ret;

    ALOGD("[JNI_PQ] setColorEffectIndex...index[%d]", index);

    snprintf(value, PROPERTY_VALUE_MAX, "%d\n", index);
    ret = property_set(PQ_MDP_COLOR_EN_STR, value);
#else
    UNUSED(index);
    ALOGE("[JNI_PQ] setColorEffectIndex(), not supported!");
#endif
}
static void getGammaIndexRange(JNIEnv* env, jclass clz, jobject range)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    Range_set(env, range, 0, GAMMA_INDEX_MAX - 1, GAMMA_INDEX_DEFAULT);
#else
    ALOGE("[JNI_PQ] getGammaIndexRange(), not supported!");
    Range_set(env, range, 0, 0, 0);
#endif
}


static void setGammaIndex(JNIEnv* env, jclass clz, jint index)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(clz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    int dev = open("/proc/mtk_mira", O_RDONLY, 0);

    if (dev >= 0) {
        _setGammaIndex(dev, index);
        close(dev);
    }
#else
    UNUSED(index);

    ALOGE("[JNI_PQ] setGammaIndex(), not supported!");
#endif
}

// OD
static jboolean enableOD(JNIEnv *env, jobject thiz, int isEnable)
{
    Mutex::Autolock autoLock(mLock);

    UNUSED(env);
    UNUSED(thiz);

#ifdef MTK_MIRAVISION_API_SUPPORT
    #ifdef JNI_PQ_PQSERVICE
        PQClient &client(PQClient::getInstance());
        client.setFeatureSwitch(IPQService::DISPLAY_OVER_DRIVE, isEnable);
    #else
        drvID = open("/proc/mtk_mira", O_RDONLY, 0);
        DISP_OD_CMD cmd;

        if (drvID < 0)
        {
            ALOGE("[JNI_PQ] open device fail!!");
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
    #endif

    return JNI_TRUE;
#else
    UNUSED(isEnable);

    ALOGE("[JNI_PQ] enableOD(), not supported!");
    return JNI_FALSE;
#endif
}


/////////////////////////////////////////////////////////////////////////////////

//JNI register
////////////////////////////////////////////////////////////////
static const char *classPathName = JNI_PQ_CLASS_NAME;

static JNINativeMethod g_methods[] = {

    // query features
    {"nativeGetCapability", "()I", (void*)getCapability},

    // Camera PQ switch
    {"nativeSetCameraPreviewMode", "()V", (void*)setCameraPreviewMode},
    {"nativeSetGalleryNormalMode", "()V", (void*)setGalleryNormalMode},
    {"nativeSetVideoPlaybackMode", "()V", (void*)setVideoPlaybackMode},
    // Image DC
    {"nativeGetDynamicContrastHistogram", "([BIIL" JNI_PQ_CLASS_NAME "$Hist;)V", (void*)getDynamicContrastHistogram},

    // MiraVision setting
    {"nativeEnablePQ", "(I)Z", (void*)enablePQ},
    {"nativeEnableColor", "(I)Z", (void*)enableColor},
    {"nativeEnableSharpness", "(I)Z", (void*)enableSharpness},
    {"nativeEnableDynamicContrast", "(I)Z", (void*)enableDynamicContrast},
    {"nativeEnableColorEffect", "(I)Z", (void*)enableColorEffect},
    {"nativeEnableGamma", "(I)Z", (void*)enableGamma},
    {"nativeGetPictureMode", "()I", (void*)getPictureMode},
    {"nativeSetPictureMode", "(I)Z", (void*)setPictureMode},
    {"nativeSetColorRegion", "(IIIII)Z", (void*)setColorRegion},
    {"nativeGetContrastIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getContrastIndexRange},
    {"nativeGetContrastIndex", "()I", (void*)getContrastIndex},
    {"nativeSetContrastIndex", "(I)V", (void*)setContrastIndex},
    {"nativeGetSaturationIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getSaturationIndexRange},
    {"nativeGetSaturationIndex", "()I", (void*)getSaturationIndex},
    {"nativeSetSaturationIndex", "(I)V", (void*)setSaturationIndex},
    {"nativeGetPicBrightnessIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getPicBrightnessIndexRange},
    {"nativeGetPicBrightnessIndex", "()I", (void*)getPicBrightnessIndex},
    {"nativeSetPicBrightnessIndex", "(I)V", (void*)setPicBrightnessIndex},
    {"nativeGetSharpnessIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getSharpnessIndexRange},
    {"nativeGetSharpnessIndex", "()I", (void*)getSharpnessIndex},
    {"nativeSetSharpnessIndex", "(I)V", (void*)setSharpnessIndex},
    {"nativeGetDynamicContrastIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getDynamicContrastIndexRange},
    {"nativeGetDynamicContrastIndex", "()I", (void*)getDynamicContrastIndex},
    {"nativeSetDynamicContrastIndex", "(I)V", (void*)setDynamicContrastIndex},
    {"nativeGetColorEffectIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getColorEffectIndexRange},
    {"nativeGetColorEffectIndex", "()I", (void*)getColorEffectIndex},
    {"nativeSetColorEffectIndex", "(I)V", (void*)setColorEffectIndex},
    {"nativeGetGammaIndexRange", "(L" JNI_PQ_CLASS_NAME "$Range;)V", (void*)getGammaIndexRange},
    {"nativeSetGammaIndex", "(I)V", (void*)setGammaIndex},
    {"nativeEnableOD", "(I)Z", (void*)enableOD},
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
