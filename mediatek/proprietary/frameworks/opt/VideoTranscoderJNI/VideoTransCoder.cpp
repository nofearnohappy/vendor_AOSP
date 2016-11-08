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
 */


#include "utils/Log.h"
#include "jni.h"
#include "MtkVideoTranscoder.h"

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <dlfcn.h>

// ----------------------------------------------------------------------------

/*
 * Field/method IDs and class object references.
 *
 * You should not need to store the JNIEnv pointer in here.  It is
 * thread-specific and will be passed back in on every call.
 */
#ifdef LOG_TAG
    #undef LOG_TAG
#endif
#define LOG_TAG "VideoTranscoderJNI"

// library name for dynamic loading
#ifdef __LP64__
    #define LIB_NAME "/system/lib64/libMtkVideoTranscoder.so"
#else
    #define LIB_NAME "/system/lib/libMtkVideoTranscoder.so"
#endif

#define API_DO_TRANSCODE    "mtk_video_transcoder_transcode"
#define API_DO_GET_PROGRESS "mtk_video_transcoder_get_progress"
#define API_DO_CANCEL       "mtk_video_transcoder_cancel"
#define API_DO_INIT         "mtk_video_transcoder_init"
#define API_DO_DEINIT       "mtk_video_transcoder_deinit"
#define JAVA_CLASS_NAME     "com/mediatek/transcode/VideoTranscode"

#define DEFAULT_BITRATE 524288   // 512*1024
#define DEFAULT_FRAMERATE 30

void* pTranscoderLib = NULL;
typedef int (*transcoder_api_transcode)(Mtk_VideoTranscoder_Context, Mtk_VideoTranscoder_Params);
typedef unsigned int (*transcoder_api_get_progress)(Mtk_VideoTranscoder_Context);
typedef void (*transcoder_api_cancel)(Mtk_VideoTranscoder_Context);
typedef void (*transcoder_api_init)(Mtk_VideoTranscoder_Context*);
typedef void (*transcoder_api_deinit)(Mtk_VideoTranscoder_Context);

transcoder_api_transcode native_transcode;
transcoder_api_get_progress native_get_progress;
transcoder_api_cancel native_cancel;
transcoder_api_init native_init;
transcoder_api_deinit native_deinit;

// ----------------------------------------------------------------------------

/*
 * Helper function to throw an arbitrary exception.
 *
 * Takes the exception class name, a format string, and one optional integer
 * argument (useful for including an error code, perhaps from errno).
 */
static void throwException(JNIEnv *env, const char *ex, const char *fmt,
                           int data) {

    if (jclass cls = env->FindClass(ex)) {
        if (fmt != NULL) {
            char msg[1000];
            snprintf(msg, sizeof(msg), fmt, data);
            env->ThrowNew(cls, msg);
        } else {
            env->ThrowNew(cls, NULL);
        }

        /*
         * This is usually not necessary -- local references are released
         * automatically when the native code returns to the VM.  It's
         * required if the code doesn't actually return, e.g. it's sitting
         * in a native event loop.
         */
        env->DeleteLocalRef(cls);
    }
}


static void VideoTranscoder_cancel(JNIEnv *env, jclass clazz, jlong handle) {
    native_cancel((Mtk_VideoTranscoder_Context) handle);
}


static jint VideoTranscoder_startTranscode_adv(JNIEnv *env, jclass clazz, jlong handle,
        jstring inPath, jstring targetPath, jlong targetWidth, jlong targetHeight,
        jlong msBegin, jlong msEnd, jlong bitRate, jlong frameRate) {

    const char* inPathChars = env->GetStringUTFChars(inPath, NULL);
    if (inPathChars == NULL) {
        ALOGW("Couldn't get inpath chars\n");
        return -2;
    }
    const char* targetPathChars = env->GetStringUTFChars(targetPath, NULL);
    if (targetPathChars == NULL) {
        ALOGW("Couldn't get target path chars\n");
        env->ReleaseStringUTFChars(inPath, inPathChars);
        return -2;
    }
    Mtk_VideoTranscoder_Params params;

    params.target_width = targetWidth;
    params.target_height = targetHeight;
    params.begin_ts = msBegin;
    params.end_ts = msEnd;
    params.target_bit_rate = bitRate;
    params.target_frame_rate = frameRate;
    params.input_path = inPathChars;
    params.output_path = targetPathChars;

    int result = native_transcode((Mtk_VideoTranscoder_Context) handle, params);
    ALOGD("Transcode result: %d\n", result);

    env->ReleaseStringUTFChars(inPath, inPathChars);
    inPathChars = NULL;
    env->ReleaseStringUTFChars(targetPath, targetPathChars);
    targetPathChars = NULL;

    return (jint) result;
}


static jint VideoTranscoder_startTranscode(JNIEnv *env, jclass clazz, jlong handle, jstring inPath,
        jstring targetPath, jlong targetWidth, jlong targetHeight, jlong msBegin, jlong msEnd) {
    return VideoTranscoder_startTranscode_adv(env, clazz, handle, inPath, targetPath,
            targetWidth, targetHeight, msBegin, msEnd, (jlong) DEFAULT_BITRATE,
            (jlong) DEFAULT_FRAMERATE);
}


static jint VideoTranscoder_getProgress(JNIEnv *env, jclass clazz, jlong handle) {
    int progress = native_get_progress((Mtk_VideoTranscoder_Context) handle);
    return (jint) progress;
}


static jlong VideoTranscoder_init(JNIEnv *env) {
    Mtk_VideoTranscoder_Context context;
    native_init(&context);
    ALOGD("VideoTranscoder_init: %ld\n", (long) context);
    return (jlong) context;
}

static void VideoTranscoder_deinit(JNIEnv *env, jclass clazz, jlong handle) {
    ALOGD("VideoTranscoder_deinit: %ld\n", (long)handle);
    native_deinit((Mtk_VideoTranscoder_Context) handle);
}


// ----------------------------------------------------------------------------

/*
 * Array of methods.
 *
 * Each entry has three fields: the name of the method, the method
 * signature, and a pointer to the native implementation.
 */
static const JNINativeMethod gMethods[] = {
    { "cancel", "(J)V", (void*)VideoTranscoder_cancel},
    {
        "transcode", "(JLjava/lang/String;Ljava/lang/String;JJJJ)I",
        (void*)VideoTranscoder_startTranscode
    },
    {
        "transcodeAdv", "(JLjava/lang/String;Ljava/lang/String;JJJJJJ)I",
        (void*)VideoTranscoder_startTranscode_adv
    },
    { "getProgress", "(J)I", (void*)VideoTranscoder_getProgress},
    { "init", "()J", (void*)VideoTranscoder_init},
    { "deinit", "(J)V", (void*)VideoTranscoder_deinit}
};


/*
 * Explicitly register all methods for our class.
 *
 * While we're at it, cache some class references and method/field IDs.
 *
 * Returns 0 on success.
 */
static int registerMethods(JNIEnv *env) {
    static const char* const kClassName = JAVA_CLASS_NAME;
    jclass clazz;

    /* look up the class */
    clazz = env->FindClass(kClassName);
    if (clazz == NULL) {
        ALOGE("Can't find class %s\n", kClassName);
        return -1;
    }

    /* register all the methods */
    if (env->RegisterNatives(clazz, gMethods,
                             sizeof(gMethods) / sizeof(gMethods[0])) != JNI_OK) {
        ALOGE("Failed registering methods for %s\n", kClassName);
        return -1;
    }

    /* fill out the rest of the ID cache */
    return JNI_OK;
}

void logAndCleanUp(void *pLib, const char *logStr) {
    ALOGE("dlsym(), %s: %s", logStr, dlerror());
    dlclose(pLib);
}

// open and init the library pointers
jint loadTranscoderLibarary(void *pLib) {
    pLib = dlopen(LIB_NAME, RTLD_NOW);
    if (NULL == pLib) {
        ALOGE("dlopen(): %s", dlerror());
        return -1;
    }

    native_transcode = (transcoder_api_transcode) dlsym(pLib, API_DO_TRANSCODE);
    if (NULL == native_transcode) {
        logAndCleanUp(pLib, API_DO_TRANSCODE);
        return -1;
    }

    native_get_progress = (transcoder_api_get_progress) dlsym(pLib, API_DO_GET_PROGRESS);
    if (NULL == native_get_progress) {
        logAndCleanUp(pLib, API_DO_GET_PROGRESS);
        return -1;
    }

    native_cancel = (transcoder_api_cancel) dlsym(pLib, API_DO_CANCEL);
    if (NULL == native_cancel) {
        logAndCleanUp(pLib, API_DO_CANCEL);
        return -1;
    }

    native_init = (transcoder_api_init) dlsym(pLib, API_DO_INIT);
    if (NULL == native_init) {
        logAndCleanUp(pLib, API_DO_INIT);
        return -1;
    }

    native_deinit = (transcoder_api_deinit) dlsym(pLib, API_DO_DEINIT);
    if (NULL == native_deinit) {
        logAndCleanUp(pLib, API_DO_DEINIT);
        return -1;
    }

    return 1;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env = NULL;
    jint result = -1;
    ALOGD("JNI_onLoad()");

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (registerMethods(env) != 0) {
        ALOGE("PlatformLibrary native registration failed\n");
        goto bail;
    }

    result = loadTranscoderLibarary(pTranscoderLib);
    if (result != 1) {
        ALOGE("Unable to load native library\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}


void JNI_OnUnload(JavaVM *vm, void *reserved) {

    if (pTranscoderLib != NULL) {
        dlclose(pTranscoderLib);
        pTranscoderLib = NULL;
    }
}
