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


#define LOG_TAG "Gesture_JNI"
#include <string.h>
#include <android/log.h>
#include <jni.h>
#include <android/bitmap.h>
#include <ui/Region.h>
#include <utils/RefBase.h>
#include <cstdio>
#include <stdio.h>
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include <cutils/properties.h>

#include <binder/IServiceManager.h>
#include <mmsdk/IMMSdkService.h>

#include <mmsdk/IHandDetectionListener.h>
#include <mmsdk/IGestureUser.h>

using namespace android;
using namespace NSMMSdk; 
using namespace android::NSMMSdk::NSGesture;


struct jHandDetectionEvent {
	jfieldID rect;
	jfieldID confidence;
	jfieldID id;
	jfieldID pose;
} gHandDetectionEvent;

jobject g_gestureObject;
jobject g_handDetecionListenerObject;
jmethodID g_onHandDetectedMethodId;

enum Pose {
	POSE_OPENPLAM = 0,
	POSE_VICTORY
};



sp <NSGesture::IGestureUser> g_GestureClient = 0; 

int g_pose = 0; 

/******************************************************************************
 *
 ******************************************************************************/
sp<IMMSdkService>
getMMSdkService()
{
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == 0)
    {
        ALOGE("Can not get the service mananger"); 
    }
    //
    sp<IBinder> binder = sm->checkService(String16("media.mmsdk"));
    if (binder == 0)
    {
        ALOGE("Can not get mmsdk service"); 
        return 0; 
    }
    //
    sp<IMMSdkService> service = interface_cast<IMMSdkService>(binder);
    if (service == 0)
    {
        ALOGE("Null mmsdk service"); 
        return 0; 
    }
    return service;
}



void callOnHandDetected(JNIEnv *env, int left, int top, int right, int bottom,
		float confidence, int id, int pose) {
	ALOGI("[%s] +", __func__);
	jclass clazz_event = env->FindClass("com/mediatek/gesture/Gesture$HandDetectionEvent");
	if (clazz_event == NULL) {
		ALOGE("can not find class:com/mediatek/gesture/Gesture$HandDetectionEvent");
		return;
	}
	jmethodID constructor_event = env->GetMethodID(clazz_event, "<init>", "(Lcom/mediatek/gesture/Gesture;)V");
	if (constructor_event == NULL) {
		ALOGE("can not find constructor:com/mediatek/gesture/Gesture$HandDetectionEvent");
		return;
	}
	jobject object_event = env->NewObject(clazz_event, constructor_event, g_gestureObject);
	if (object_event == NULL) {
		ALOGE("unable to create:com/mediatek/gesture/Gesture$HandDetectionEvent");
		return;
	}

	jclass clazz_rect = env->FindClass("android/graphics/Rect");
	if (clazz_rect == NULL) {
		ALOGE("can not find class:android/graphics/Rect");
		return;
	}
	jmethodID constructor_rect = env->GetMethodID(clazz_rect, "<init>", "(IIII)V");
	if (constructor_rect == NULL) {
		ALOGE("can not find constructor:android/graphics/Rect");
		return;
	}
	jobject object_rect = env->NewObject(clazz_rect, constructor_rect, left, top, right, bottom);
	if (object_rect == NULL) {
		ALOGE("unadble to create android/graphics/Rect");
		return;
	}

	jclass clazz_pose = env->FindClass("com/mediatek/gesture/Gesture$HandPose");
	jfieldID field_pose = NULL;
	if (clazz_pose == NULL) {
		ALOGE("can not find class:com/mediatek/gesture/Gesture$HandPose");
		return;
	}
	switch (pose) {
	case POSE_OPENPLAM:
		field_pose = env->GetStaticFieldID(clazz_pose, "POSE_OPENPLAM",
				"Lcom/mediatek/gesture/Gesture$HandPose;");
		break;
	case POSE_VICTORY:
		field_pose = env->GetStaticFieldID(clazz_pose, "POSE_VICTORY",
				"Lcom/mediatek/gesture/Gesture$HandPose;");
		break;
	default:
		break;
	}
	if (field_pose == NULL) {
		ALOGE("can not find enum:com/mediatek/gesture/Gesture$HandPose");
		return;
	}
	jobject object_pose = env->GetStaticObjectField(clazz_pose, field_pose);

	gHandDetectionEvent.rect = env->GetFieldID(clazz_event, "boundBox", "Landroid/graphics/Rect;");
	gHandDetectionEvent.confidence = env->GetFieldID(clazz_event, "confidence", "F");
	gHandDetectionEvent.id = env->GetFieldID(clazz_event, "id", "I");
	gHandDetectionEvent.pose = env->GetFieldID(clazz_event, "pose",
			"Lcom/mediatek/gesture/Gesture$HandPose;");

	env->SetObjectField(object_event, gHandDetectionEvent.rect, object_rect);
	env->SetFloatField(object_event, gHandDetectionEvent.confidence, confidence);
	env->SetIntField(object_event, gHandDetectionEvent.id, id);
	env->SetObjectField(object_event, gHandDetectionEvent.pose, object_pose);

	env->DeleteLocalRef(object_rect);
	env->DeleteLocalRef(object_pose);

	env->CallVoidMethod(g_handDetecionListenerObject, g_onHandDetectedMethodId, object_event);

	env->DeleteLocalRef(object_event);
	ALOGI("[%s] -", __func__);
}


struct HandDetectionListener : public BnHandDetectionListener {

    HandDetectionListener() 
    {
    }

    void onHandDetected(HandDetectionEvent const &event)
    {
        ALOGD("onHandDetected "); 
        JNIEnv *env = AndroidRuntime::getJNIEnv();
        callOnHandDetected(env, event.boundBox.left, 
                                event.boundBox.top, 
                                event.boundBox.right, 
                                event.boundBox.bottom,
                                event.confidence, 
                                event.id, 
                                event.pose);      
    }

};

sp<HandDetectionListener> g_listener = 0; 

void com_mediatek_gesture_setup(JNIEnv *env, jobject thiz, jobject object_this)
{
	ALOGI("[%s]", __func__);
	g_gestureObject = env->NewGlobalRef(object_this);


}

void com_mediatek_gesture_addGesture(JNIEnv *env, jobject thiz, jobject handDetectionListener, jobject handPose)
{
	ALOGI("[%s]", __func__);
	g_handDetecionListenerObject = env->NewGlobalRef(handDetectionListener);
	jclass clazz_listener = env->GetObjectClass(handDetectionListener);
	if (clazz_listener == NULL) {
		ALOGE("can not find class:HandDetectionListener");
		return;
	}

	g_onHandDetectedMethodId = env->GetMethodID(clazz_listener, "onHandDetected",
			"(Lcom/mediatek/gesture/Gesture$HandDetectionEvent;)V");

	jclass clazz = env->GetObjectClass(handPose);
	if (clazz == NULL) {
		ALOGE("can not find class:HandPose");
		return;
	}

	// just for test
	jmethodID methodId = env->GetMethodID(clazz, "getValue", "()I");
	jint pose = env->CallIntMethod(handPose, methodId);
	ALOGI("pose: %d", pose);

    sp<IMMSdkService> cs = getMMSdkService();
    if (g_GestureClient == 0)
    {
        cs->connectGesture(g_GestureClient); 
        // 
        if (g_GestureClient == 0)
        {
            ALOGE("connect fail"); 
            return ; 
        }
    }

	// just for test
	//callOnHandDetected(env, 0, 0, 100, 100, 100, 101, POSE_OPENPLAM);
    if (g_GestureClient != 0)
    {
        //  
        g_pose = pose; 
        g_listener = new HandDetectionListener(); 
        // register listener 

        g_GestureClient->addHandDetectionListener(g_listener, static_cast<EHandPose>(pose));     
    }
}

void com_mediatek_gesture_removeGesture (JNIEnv *env, jobject thiz, jobject handDetectionListener, jobject handPose)
{
	ALOGI("[%s]", __func__);
    if (g_listener != 0 && g_GestureClient != 0)
    {
        g_GestureClient->removeHandDetectionListener(g_listener, static_cast<EHandPose>(g_pose)); 
    } 
    g_GestureClient->disconnect(); 
}

const char *classPathName = "com/mediatek/gesture/Gesture";

JNINativeMethod methods[] = {
    {"native_setup", "(Lcom/mediatek/gesture/Gesture;)V", (void*)com_mediatek_gesture_setup},
    {"native_addGesture", "(Lcom/mediatek/gesture/Gesture$HandDetectionListener;Lcom/mediatek/gesture/Gesture$HandPose;)V",
    		(void*)com_mediatek_gesture_addGesture},
    {"native_removeGesture", "(Lcom/mediatek/gesture/Gesture$HandDetectionListener;Lcom/mediatek/gesture/Gesture$HandPose;)V",
    		(void*)com_mediatek_gesture_removeGesture},
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

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    ALOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        ALOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}

