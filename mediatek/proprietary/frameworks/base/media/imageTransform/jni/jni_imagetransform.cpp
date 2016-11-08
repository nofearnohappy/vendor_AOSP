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

#define LOG_TAG "ImageTransform_JNI"
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
#include <mmsdk/IImageTransformUser.h>

using namespace android;
using namespace NSMMSdk;
using namespace NSMMSdk::NSImageTransform;

/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        ALOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        ALOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        ALOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        ALOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        ALOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)


struct Rect {
    int left;
    int top;
    int right;
    int bottom;
} rect;

struct OptionsClass {
    jfieldID roi_field;
    jfieldID flip_field;
    jfieldID rotation_field;
    jfieldID quality_field;
    jfieldID dither_field;
    jfieldID sharpness_field;
} g_optionsClass;

struct Options {
    jobject roi;
    jstring flip;
    jint rotation;
    jint quality;
    jboolean dither;
    jint sharpness;
} g_options;

struct ImageClass {
    jmethodID getFormat_method;
    jmethodID getWidth_method;
    jmethodID getHeight_method;
    jmethodID getTimestamp_method;
    jmethodID getPlanes_method;
} g_imageClass;

struct Plane {
    jint rowStride;
    jint pixelStride;
    jobject byteBuffer;
} g_plane;

struct PlaneClass {
    jmethodID getRowStride_method;
    jmethodID getPixelStride_method;
    jmethodID getByteBuffer_method;
} g_planeClass;


static int g_dump_file = 0;

/******************************************************************************
 * Get the MMSdk Service().
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
*******************************************************************************/
bool nativeImageTransform
(
    ImageInfo const &rSrcImg,
    const sp<IMemory>&srcDataPtr ,
    ImageInfo const &rDstImg,
    const sp<IMemory>&destDataPrt,
    TrasformOptions const &rOptions
)
{
    MY_LOGD("+");
    ATRACE_CALL();
    sp<IMMSdkService> cs = getMMSdkService();
    using namespace NSImageTransform;

    sp <NSImageTransform::IImageTransformUser> scaler;
    cs->connectImageTransformUser(scaler);
    //
    if (scaler == 0)
    {
        MY_LOGE("native image tranform connect fail");
        return false;
    }

    bool ret = false;
    //
    // Rotation is applied CLOCKWISE and AFTER TRANSFORM_FLIP_{H|V}.
    if ((rOptions.transform & (eFLIP_H|eFLIP_V)))
    {

        TrasformOptions rTempOptions;
        rTempOptions.rect = android::Rect(0, 0, rSrcImg.width, rSrcImg.height);
        rTempOptions.transform = (rOptions.transform & (eFLIP_H|eFLIP_V));
        rTempOptions.encQuality = 0;
        rTempOptions.isDither = false;
        rTempOptions.sharpnessLevel = 0;
        MY_LOGD("Do Flip operation:%d", rTempOptions.transform);
        ATRACE_BEGIN("native --> flip");
        ret = scaler->applyTransform(rSrcImg, srcDataPtr, rSrcImg, srcDataPtr, rTempOptions);
        ATRACE_END();
    }
    //
    TrasformOptions rTempOptions;
    memcpy(&rTempOptions, &rOptions, sizeof(TrasformOptions));
    rTempOptions.transform = rOptions.transform & (~(eFLIP_H|eFLIP_V));
    MY_LOGD("Do rotation operation:%d", rTempOptions.transform);
    ATRACE_BEGIN("native --> flip");
    ret = scaler->applyTransform(rSrcImg, srcDataPtr, rDstImg, destDataPrt,  rTempOptions);
    ATRACE_END();
    //
    scaler->disconnect();
    MY_LOGD("-");
    return ret;
}


/******************************************************************************
*
*******************************************************************************/
int convertTransform(String8 flip, int rotation)
{
    int i4Flip = 0;
    int i4Rotation = 0;
    //
    if (flip.length() != 0)
    {
        if (flip == "horizontally")
        {
            i4Flip = eFLIP_H;
        }
        else if (flip == "vertically")
        {
            i4Flip = eFLIP_V;
        }
    }
    //
    if (90 == rotation)
    {
        i4Rotation = eROT_90;
    }
    else if (180 == rotation)
    {
        i4Rotation = eROT_180;
    }
    else if (270 == rotation )
    {
        i4Rotation = eROT_270;
    }
    return (i4Flip|i4Rotation);
}

/******************************************************************************
*
*******************************************************************************/
bool convertJavaImageToNative(JNIEnv* env, jobject image, ImageInfo *imgInfo, uintptr_t *buffer[3])
{
    // Get the format, width, height, time stamp value of source image.
    jint format = env->CallIntMethod(image, g_imageClass.getFormat_method);
    jint width = env->CallIntMethod(image, g_imageClass.getWidth_method);
    jint height = env->CallIntMethod(image, g_imageClass.getHeight_method);
    //[FIXME],
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
*******************************************************************************/
bool convertJavaOptionsToNative(JNIEnv* env, jobject options, TrasformOptions *pOptions)
{
    // options
    if (options == NULL)
    {
        return false;
    }
    g_options.roi = env->GetObjectField(options, g_optionsClass.roi_field);
    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        MY_LOGE("can not find class android/graphics/Rect");
        return false;
    }
    jfieldID rect_leftID = env->GetFieldID(rect_class, "left", "I");
    jfieldID rect_topID = env->GetFieldID(rect_class, "top", "I");
    jfieldID rect_rightID = env->GetFieldID(rect_class, "right", "I");
    jfieldID rect_bottomID = env->GetFieldID(rect_class, "bottom", "I");
    // get left, top, right, bottom value of rect
    rect.left = env->GetIntField(g_options.roi, rect_leftID);
    rect.top = env->GetIntField(g_options.roi, rect_topID);
    rect.right = env->GetIntField(g_options.roi, rect_rightID);
    rect.bottom = env->GetIntField(g_options.roi, rect_bottomID);
    // get flip, rotation, quality, ditherEnable, sharpnessLevel value.1
    g_options.flip = static_cast<jstring>(env->GetObjectField(options, g_optionsClass.flip_field));
    g_options.rotation = env->GetIntField(options, g_optionsClass.rotation_field);
    g_options.quality = env->GetIntField(options, g_optionsClass.quality_field);
    g_options.dither = env->GetBooleanField(options, g_optionsClass.dither_field);
    g_options.sharpness = env->GetIntField(options, g_optionsClass.sharpness_field);
    //
    String8 s8Fllip;
    if (g_options.flip != NULL)
    {
        const jchar* pflip = env->GetStringCritical(g_options.flip, 0);
        s8Fllip = String8(reinterpret_cast<const char16_t*>(pflip), env->GetStringLength(g_options.flip));
        env->ReleaseStringCritical(g_options.flip, pflip);
    }
    pOptions->rect = android::Rect(rect.left, rect.top, rect.right, rect.bottom);
    pOptions->transform = convertTransform(s8Fllip, g_options.rotation);
    pOptions->encQuality = static_cast<int>(g_options.quality);
    pOptions->isDither = static_cast<int>(g_options.dither);
    pOptions->sharpnessLevel = static_cast<int>(g_options.sharpness);
    return true;
}

/******************************************************************************
*
*******************************************************************************/
jboolean com_mediatek_imageTransformFactory_applyTransform(JNIEnv* env, jobject thiz,
        jobject srcImage, jobject targetImage, jobject options)
{
    MY_LOGD("+");
    ATRACE_CALL();
    //
    ImageInfo rSrcImgInfo;
    uintptr_t* src_buffer[3] = {0, 0, 0};
    if (!convertJavaImageToNative(env, srcImage, &rSrcImgInfo, src_buffer))
    {
        MY_LOGE("convert src image to native fail");
        return false;
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
        return false;
    }
    //
    MY_LOGD("dst (fmt, w, h, plane)=(%d,%d,%d,%d), stride=(%d,%d,%d)",
                      rDestImgInfo.format, rDestImgInfo.width, rDestImgInfo.height, rDestImgInfo.numOfPlane,
                      rDestImgInfo.stride[0], rDestImgInfo.stride[1], rDestImgInfo.stride[2]);
    MY_LOGD("dst addr(0x%x, 0x%x, 0x%x", (uintptr_t)dest_buffer[0], (uintptr_t)dest_buffer[1], (uintptr_t)dest_buffer[2]);

    // TransformOptions
    TrasformOptions rOptions;
    if (!convertJavaOptionsToNative(env, options, &rOptions))
    {

        MY_LOGE("convert options to native fail");
        MY_LOGW("convert options to native fail, use default");
        rOptions.rect = android::Rect(0, 0, rSrcImgInfo.width, rSrcImgInfo.height);
    }
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
        return JNI_FALSE;
    }

    // copy source image content
    ATRACE_BEGIN("apply --> copySrc");
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
    //
    if (1 == g_dump_file)
    {
        saveBufToFile("/sdcard/jnisrc.yuv", (uint8_t*)pSrcHeap->base(), srcImgBufferSize);
    }
    //
    ATRACE_BEGIN("apply --> native");
    bool ret = nativeImageTransform(rSrcImgInfo, pSrcBuffer, rDestImgInfo, pDestBuffer, rOptions);
    ATRACE_END();
    //
    if (1 == g_dump_file)
    {
        saveBufToFile("/sdcard/jnidst.yuv", (uint8_t*)pDestHeap->base(), destImgBufferSize);
    }
    ptrSrc = reinterpret_cast<char*>(pDestHeap->base());
    ATRACE_BEGIN("apply --> copyDst");
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
    MY_LOGD("-");
    return ret;
}



const char *classPathName = "com/mediatek/imagetransform/ImageTransformFactory";

static JNINativeMethod camMethods[] = {
    {"native_applyTransform",
            "(Landroid/media/Image;Landroid/media/Image;Lcom/mediatek/imagetransform/ImageTransformFactory$Options;)Z",
            (jboolean *)com_mediatek_imageTransformFactory_applyTransform },
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
    g_planeClass.getRowStride_method = env->GetMethodID(plane_clazz, "getRowStride", "()I");
    g_planeClass.getPixelStride_method = env->GetMethodID(plane_clazz, "getPixelStride", "()I");
    g_planeClass.getByteBuffer_method = env->GetMethodID(plane_clazz, "getBuffer", "()Ljava/nio/ByteBuffer;");

    // get fields of Options
    jclass option_clazz = env->FindClass("com/mediatek/imagetransform/ImageTransformFactory$Options");
    if (option_clazz == NULL) {
        MY_LOGE("can not find the class:com/mediatek/imagetransform/ImageTransformFactory$Options");
        return JNI_FALSE;
    }
    g_optionsClass.roi_field = env->GetFieldID(option_clazz, "cropRoi", "Landroid/graphics/Rect;");
    g_optionsClass.flip_field = env->GetFieldID(option_clazz, "flip", "Ljava/lang/String;");
    g_optionsClass.rotation_field = env->GetFieldID(option_clazz, "rotation", "I");
    g_optionsClass.quality_field = env->GetFieldID(option_clazz, "encodingQuality", "I");
    g_optionsClass.dither_field = env->GetFieldID(option_clazz, "dither", "Z");
    g_optionsClass.sharpness_field = env->GetFieldID(option_clazz, "sharpness", "I");

    return JNI_TRUE;
}


/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 camMethods, sizeof(camMethods) / sizeof(camMethods[0]))) {
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
    property_get("debug.imgtransform.dump", value, "0");
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


