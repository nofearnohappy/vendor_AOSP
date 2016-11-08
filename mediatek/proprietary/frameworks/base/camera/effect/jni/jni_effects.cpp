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

#define LOG_TAG "Effect_JNI"
#define ATRACE_TAG ATRACE_TAG_CAMERA
#include <utils/Trace.h>
#include <cutils/properties.h>

#include <jni.h>
#include <errno.h>
#include <fcntl.h>

#include <utils/Log.h>
#include <android/log.h>
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <utils/Log.h>
#include <android/log.h>
#include <cutils/log.h>

#include <binder/IMemory.h>
#include <binder/MemoryBase.h>
#include <binder/MemoryHeapBase.h>

#include <binder/IServiceManager.h>
#include <mmsdk/IMMSdkService.h>

#include <mmsdk/IEffectUpdateListener.h>
#include <mmsdk/IEffectUser.h>

#include <mmsdk/IEffectHal.h>

using namespace android;
using namespace NSMMSdk;
using namespace NSMMSdk::NSEffect;

/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        ALOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        ALOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        ALOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        ALOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        ALOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)


//
struct ImageClass {
    jmethodID getFormat_method;
    jmethodID getWidth_method;
    jmethodID getHeight_method;
    jmethodID getTimestamp_method;
    jmethodID getPlanes_method;
} g_imageClass;

//
struct Plane {
    jint rowStride;
    jint pixelStride;
    jobject byteBuffer;
} g_plane;

//
struct PlaneClass {
    jmethodID getRowStride_method;
    jmethodID getPixelStride_method;
    jmethodID getByteBuffer_method;
} g_planeClass;

struct FaceEffectParam {
    jint sharpness;
    jint skinColor;
    jint smooth;
    jint enlargeEye;
}g_faceEffectParam;

jobject g_faceBeautyeffectObject;
jobject g_effectUpdateListenerObject;
jmethodID g_onEffectUpdateMethodId;

sp <NSEffect::IEffectUser> g_effectClientFB = 0;
static int g_dump_file = 0;

/******************************************************************************
 *
 ******************************************************************************/
void callOnEffectUpdateds(JNIEnv *env, jobject effect, jobject info)
{
    MY_LOGI("[%s]", __func__);
    env->CallVoidMethod(g_effectUpdateListenerObject, g_onEffectUpdateMethodId,
            effect, info);
}

/******************************************************************************
 *
 ******************************************************************************/
struct EffectUpdateListener : public BnEffectUpdateListener {

    EffectUpdateListener()
    {
    }

    void onEffectUpdated(const sp<NSEffect::IEffectUser>& effect, void *info)
    {
        MY_LOGD("onEffectUpdated ");
        JNIEnv *env = AndroidRuntime::getJNIEnv();
        callOnEffectUpdateds(env, g_faceBeautyeffectObject, 0);
    }

};

/******************************************************************************
 * Get the MMSdk Service.
 ******************************************************************************/
sp<IMMSdkService>
getMMSdkService()
{
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == 0)
    {
        MY_LOGE("Can not get the service mananger");
    }
    // use checkService to avoid block
    sp<IBinder> binder = sm->checkService(String16("media.mmsdk"));
    if (binder == 0)
    {
        MY_LOGE("Can not get mmsdk service");
        return 0;
    }
    //
    sp<IMMSdkService> service = interface_cast<IMMSdkService>(binder);
    if (service == 0)
    {
        MY_LOGE("Null mmsdk service");
        return 0;
    }
    return service;
}

/******************************************************************************
* save the buffer to the file
*******************************************************************************/
bool
saveBufToFile(char const*const fname, uint8_t *const buf, uint32_t const size)
{
    int nw, cnt = 0;
    uint32_t written = 0;

    MY_LOGD("opening file [%s]", fname);
    int fd = ::open(fname, O_RDWR | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        MY_LOGE("failed to create file [%s]: %s", fname, ::strerror(errno));
        return false;
    }

    MY_LOGD("writing %d bytes to file [%s]", size, fname);
    while (written < size) {
        nw = ::write(fd,
                     buf + written,
                     size - written);
        if (nw < 0) {
            MY_LOGE("failed to write to file [%s]: %s", fname, ::strerror(errno));
            break;
        }
        written += nw;
        cnt++;
    }
    MY_LOGD("done writing %d bytes to file [%s] in %d passes", size, fname, cnt);
    ::close(fd);
    return true;
}

/******************************************************************************
 *
 ******************************************************************************/
bool nativeEffectFBApply
(
    ImageInfo const &rSrcImg,
    const sp<IMemory>&srcDataPtr ,
    ImageInfo const &rDstImg,
    const sp<IMemory>&destDataPrt
)
{
    bool ret = false;
    if (g_effectClientFB != 0)
    {
        //
        MY_LOGD("nativeEffectFBApply");

        sp<EffectUpdateListener> listener = new EffectUpdateListener();
        // register listener
        g_effectClientFB->setUpdateListener(listener);

        //
       ret = g_effectClientFB->apply(rSrcImg, srcDataPtr, rDstImg, destDataPrt);
    }
    return ret;
}

/******************************************************************************
 *
 ******************************************************************************/
void com_mediatek_effect_Effect_setUpdateListener(JNIEnv* env, jobject thiz, jobject listener)
{
    MY_LOGI("[%s]", __func__);
    g_effectUpdateListenerObject = env->NewGlobalRef(listener);
    jclass clazz = env->GetObjectClass(listener);

    g_onEffectUpdateMethodId = env->GetMethodID(clazz, "onEffectUpdateds",
            "(Lcom/mediatek/effect/Effect;Ljava/lang/Object;)V");
}

/******************************************************************************
 *
 ******************************************************************************/
jboolean com_mediatek_effect_EffectFactory_isEffectSupported(JNIEnv* env, jobject thiz,
        jstring effectName)
{
    MY_LOGI("[%s]", __func__);
    //
    String8 s8EffectName;
    if (effectName != NULL)
    {
        const jchar* pName = env->GetStringCritical(effectName, 0);
        s8EffectName = String8(reinterpret_cast<const char16_t*>(pName), env->GetStringLength(effectName));
        env->ReleaseStringCritical(effectName, pName);
    }
    //[FIXME], should call to native
    if (s8EffectName == "FaceBeautyEffect")
    {
        return true;
    }
    else
    {
        return false;
    }
}

/******************************************************************************
 *
 ******************************************************************************/
void com_mediatek_effect_FaceBeautyEffect_setup(JNIEnv* env, jobject thiz,
        jobject faceBeautyEffectObject)
{
    MY_LOGI("[%s]", __func__);
    g_faceBeautyeffectObject = env->NewGlobalRef(faceBeautyEffectObject);

    MY_LOGD("Connect to effect client");
    sp<IMMSdkService> cs = getMMSdkService();

    cs->connectEffect(String16("faceBeauty"), g_effectClientFB);
    //
    if (g_effectClientFB == 0)
    {
        MY_LOGE("connect fail");
        return;
    }
    MY_LOGD("getName():%s", g_effectClientFB->getName().string());

}

/******************************************************************************
 *
 ******************************************************************************/
void com_mediatek_effect_FaceBeautyEffect_setParameter(JNIEnv* env, jobject thiz,
        jstring parameterKey, jobject value)
{
    MY_LOGI("[%s]", __func__);
    ATRACE_CALL();
    //
    String8 s8Param;
    if (parameterKey != NULL)
    {
        const jchar* pParam = env->GetStringCritical(parameterKey, 0);
        s8Param = String8(reinterpret_cast<const char16_t*>(pParam), env->GetStringLength(parameterKey));
        env->ReleaseStringCritical(parameterKey, pParam);
    }
    //
    jclass intClass = env->FindClass("java/lang/Integer");
    jmethodID intValueMID  = env->GetMethodID(intClass, "intValue", "()I");
    jint intValue = (jint) env->CallIntMethod(value, intValueMID);
    if (g_effectClientFB != 0)
    {
        MY_LOGD("set fb effect parameter:%s, %d", s8Param.string(), intValue);
        g_effectClientFB->setParameter(s8Param, reinterpret_cast<void*>(&intValue));
    }
}

/******************************************************************************
*
*******************************************************************************/
bool convertJavaImageToNative(JNIEnv* env, jobject image, ImageInfo *imgInfo, uintptr_t *buffer[3])
{
    // get the format, width, height, time stamp value of source image.
    jint format = env->CallIntMethod(image, g_imageClass.getFormat_method);
    jint width = env->CallIntMethod(image, g_imageClass.getWidth_method);
    jint height = env->CallIntMethod(image, g_imageClass.getHeight_method);
    //FIXME,
    jint timeStamp = env->CallLongMethod(image, g_imageClass.getTimestamp_method);
    jobjectArray planes = static_cast<jobjectArray>(
            env->CallObjectMethod(image, g_imageClass.getPlanes_method));
    jsize numOfPlane = env->GetArrayLength(planes);
    jint stride[3] = {0, 0, 0};
    uintptr_t* buffers[3] = {0, 0, 0};
    //
    for (int i = 0; i < numOfPlane; i++)
    {
        jobject plane   = env->GetObjectArrayElement(planes, i);
        stride[i] = env->CallIntMethod(plane, g_planeClass.getRowStride_method);
        g_plane.byteBuffer = env->CallObjectMethod(plane, g_planeClass.getByteBuffer_method);
        // get buffer pointer
        buffers[i] = static_cast<uintptr_t*>(env->GetDirectBufferAddress(g_plane.byteBuffer));
        if (buffers[i] == NULL) {
            MY_LOGE("buffer_pointer[%d] is null", i);
            return false;
        }
    }
    //
    imgInfo->format = format;
    imgInfo->width = width;
    imgInfo->height = height;
    imgInfo->numOfPlane = numOfPlane;
    for (int i = 0; i < numOfPlane; i++)
    {
        imgInfo->stride[i] = stride[i];
        buffer[i] = buffers[i];
    }
    return true;
}


/******************************************************************************
 *
 ******************************************************************************/
void com_mediatek_effect_FaceBeautyEffect_apply(JNIEnv* env, jobject thiz,
        jobject srcImage, jobject targetImage)
{
    MY_LOGI("[%s]", __func__);
    ATRACE_CALL();
    //
    ImageInfo rSrcImgInfo;
    uintptr_t* src_buffer[3] = {0, 0, 0};
    if (!convertJavaImageToNative(env, srcImage, &rSrcImgInfo, src_buffer))
    {
        MY_LOGE("convert src image to native fail");
        return ;
    }
    //
    MY_LOGD("src (fmt, w, h, plane)=(%d,%d,%d,%d), stride=(%d,%d,%d)",
                     rSrcImgInfo.format, rSrcImgInfo.width, rSrcImgInfo.height, rSrcImgInfo.numOfPlane,
                     rSrcImgInfo.stride[0], rSrcImgInfo.stride[1], rSrcImgInfo.stride[2]);
    MY_LOGD("src addr(0x%x, 0x%x, 0x%x", (uintptr_t)src_buffer[0], (uintptr_t)src_buffer[1], (uintptr_t)src_buffer[2]);

    //
    ImageInfo rDestImgInfo;
    uintptr_t *dest_buffer[3] = {0, 0, 0};
    if (!convertJavaImageToNative(env, targetImage, &rDestImgInfo, dest_buffer))
    {
        MY_LOGE("convert dest image  to native fail");
        return ;
    }
    //
    MY_LOGD("dst (fmt, w, h, plane)=(%d,%d,%d,%d), stride=(%d,%d,%d)",
                      rDestImgInfo.format, rDestImgInfo.width, rDestImgInfo.height, rDestImgInfo.numOfPlane,
                      rDestImgInfo.stride[0], rDestImgInfo.stride[1], rDestImgInfo.stride[2]);
    MY_LOGD("dst addr(0x%x, 0x%x, 0x%x", (uintptr_t)dest_buffer[0], (uintptr_t)dest_buffer[1], (uintptr_t)dest_buffer[2]);

    //
    sp<MemoryHeapBase> pSrcHeap;
    sp<MemoryHeapBase> pDestHeap;
    sp<MemoryBase> pSrcBuffer;
    sp<MemoryBase> pDestBuffer;

    int srcImgBufferSize = (rSrcImgInfo.stride[0] + rSrcImgInfo.stride[1] + rSrcImgInfo.stride[2]) * rSrcImgInfo.height;
    int destImgBufferSize = (rDestImgInfo.stride[0] + rDestImgInfo.stride[1] + rDestImgInfo.stride[2]) * rDestImgInfo.height;
    MY_LOGD("src size = %d, dst size = %d",srcImgBufferSize, destImgBufferSize);
    pSrcHeap =  new MemoryHeapBase(srcImgBufferSize);
    pDestHeap = new MemoryHeapBase(destImgBufferSize);

    pSrcBuffer = new MemoryBase(pSrcHeap, 0, srcImgBufferSize );
    pDestBuffer = new MemoryBase(pDestHeap,0, destImgBufferSize);
    if (pSrcBuffer == 0 || pDestBuffer == 0)
    {
        //jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        MY_LOGE("Out ot memory");
        return;
    }

    // copy source image content
    ATRACE_BEGIN("apply->copySrc");
    char *ptrSrc = (char*)pSrcHeap->base();
    for (int i = 0; i < rSrcImgInfo.numOfPlane; i++)
    {
        MY_LOGD("srcAddr=%p, dstAddr=%p", ptrSrc, src_buffer[i]);
        memcpy(reinterpret_cast<char*>(ptrSrc),
               reinterpret_cast<char*>(src_buffer[i]),
               rSrcImgInfo.stride[i] * rSrcImgInfo.height);
        ptrSrc += rSrcImgInfo.stride[i] * rSrcImgInfo.height;
    }
    ATRACE_END();
    if (1 == g_dump_file)
    {
        saveBufToFile("/sdcard/jnisrc.yuv", (uint8_t*)pSrcHeap->base(), srcImgBufferSize);
    }
    //
    ATRACE_BEGIN("apply->native");
    bool ret = nativeEffectFBApply(rSrcImgInfo, pSrcBuffer, rDestImgInfo, pDestBuffer);
    ATRACE_END();
    //
    if (1 == g_dump_file)
    {
        saveBufToFile("/sdcard/jnidst.yuv", (uint8_t*)pDestHeap->base(), destImgBufferSize);
    }
    ATRACE_BEGIN("apply->copyDst");
    ptrSrc = reinterpret_cast<char*>(pDestHeap->base());
    for (int i = 0; i < rDestImgInfo.numOfPlane; i++)
    {
        MY_LOGD("srcAddr=%p, dstAddr=%p", ptrSrc, dest_buffer[i]);
        memcpy(reinterpret_cast<char*>(dest_buffer[i]),
               reinterpret_cast<char*>(ptrSrc),
               rDestImgInfo.stride[i] * rDestImgInfo.height);
        ptrSrc += rDestImgInfo.stride[i] * rDestImgInfo.height;
    }
    ATRACE_END();

    pSrcBuffer = 0;
    pDestBuffer = 0;
    return;
}

/******************************************************************************
 *
 ******************************************************************************/
void com_mediatek_effect_FaceBeautyEffect_release(JNIEnv* env, jobject thiz)
{
    MY_LOGI("[%s]", __func__);

    if (g_effectClientFB != 0)
    {
        MY_LOGD("disconnect");
        g_effectClientFB->disconnect();
        return;
    }
}

/******************************************************************************
 *
 ******************************************************************************/
const char *classPathEffectName = "com/mediatek/effect/Effect";
const char *classPathEffectFactoryName = "com/mediatek/effect/EffectFactory";
const char *classPathFaceBeautyName = "com/mediatek/effect/FaceBeautyEffect";

static JNINativeMethod effectMethods[] = {
    {"native_setUpdateListener", "(Lcom/mediatek/effect/Effect$EffectUpdateListener;)V",
            (void *)com_mediatek_effect_Effect_setUpdateListener},
};

static JNINativeMethod effectFactoryMethods[] = {
    {"native_isEffectSupporteds", "(Ljava/lang/String;)Z",
            (jboolean *)com_mediatek_effect_EffectFactory_isEffectSupported },
};

static JNINativeMethod faceBeautyMethods[] = {
    //fb effect native method
    {"native_setup", "(Lcom/mediatek/effect/FaceBeautyEffect;)V",
            (void*)com_mediatek_effect_FaceBeautyEffect_setup },
    {"native_setParameter", "(Ljava/lang/String;Ljava/lang/Object;)V",
            (void *)com_mediatek_effect_FaceBeautyEffect_setParameter },
    {"native_apply", "(Landroid/media/Image;Landroid/media/Image;)V",
            (void *)com_mediatek_effect_FaceBeautyEffect_apply },
    {"native_release", "()V", (void*)com_mediatek_effect_FaceBeautyEffect_release },

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
        MY_LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        MY_LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    // get methods of Image class
    jclass image_clazz = env->FindClass("android/media/Image");
    if (image_clazz == NULL) {
        MY_LOGE("can not find the class:android/media/Image");
        return JNI_FALSE;
    }
    g_imageClass.getFormat_method = env->GetMethodID(image_clazz, "getFormat", "()I");
    g_imageClass.getWidth_method = env->GetMethodID(image_clazz, "getWidth", "()I");
    g_imageClass.getHeight_method = env->GetMethodID(image_clazz, "getHeight", "()I");
    g_imageClass.getTimestamp_method = env->GetMethodID(image_clazz, "getTimestamp", "()J");
    g_imageClass.getPlanes_method = env->GetMethodID(image_clazz, "getPlanes", "()[Landroid/media/Image$Plane;");
    // get methods of Plane class
    jclass plane_clazz = env->FindClass("android/media/Image$Plane");
    if (plane_clazz == NULL) {
        MY_LOGE("can not find the class:android/media/Image$Plane");
        return JNI_FALSE;
    }
    //
    g_planeClass.getRowStride_method = env->GetMethodID(plane_clazz, "getRowStride", "()I");
    g_planeClass.getPixelStride_method = env->GetMethodID(plane_clazz, "getPixelStride", "()I");
    g_planeClass.getByteBuffer_method = env->GetMethodID(plane_clazz, "getBuffer", "()Ljava/nio/ByteBuffer;");

    return JNI_TRUE;
}


/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
    if (!registerNativeMethods(env, classPathEffectName,
            effectMethods, sizeof(effectMethods) / sizeof(effectMethods[0]))) {
        return JNI_FALSE;
    }
    if (!registerNativeMethods(env, classPathEffectFactoryName,
            effectFactoryMethods,
            sizeof(effectFactoryMethods) / sizeof(effectFactoryMethods[0]))) {
        return JNI_FALSE;
    }

    if (!registerNativeMethods(env, classPathFaceBeautyName,
            faceBeautyMethods, sizeof(faceBeautyMethods) / sizeof(faceBeautyMethods[0]))) {
            return JNI_FALSE;
        }

   return JNI_TRUE;
}

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("debug.effect.dump", value, "0");
    g_dump_file = ::atoi(value);

    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    MY_LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        MY_LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        MY_LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    result = JNI_VERSION_1_4;

    bail:
        return result;
}


