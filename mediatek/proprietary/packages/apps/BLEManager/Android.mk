
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := BLEManager

LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.ngin3d-static

LOCAL_JNI_SHARED_LIBRARIES := libja3m liba3m

LOCAL_JAVA_LIBRARIES := mediatek-framework

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

#include $(call all-makefiles-under,$(LOCAL_PATH))

