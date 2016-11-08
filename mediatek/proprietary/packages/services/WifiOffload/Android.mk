LOCAL_PATH:= $(call my-dir)

# interface lib
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, lib/src)

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/lib/src

# aidl
LOCAL_SRC_FILES += \
    lib/src/com/mediatek/wfo/IWifiOffloadService.aidl \
    lib/src/com/mediatek/wfo/IWifiOffloadListener.aidl

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := wfo-common

include $(BUILD_JAVA_LIBRARY)

# JNI
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    jni/com_mediatek_wfo_WifiOffloadService.c

LOCAL_MULTILIB := both
LOCAL_ARM_MODE := arm

LOCAL_SHARED_LIBRARIES := \
    libnativehelper \
    libcutils \
    liblog \
    libutils \
    libmal \
    libmdfx

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libwfo_jni

include $(BUILD_SHARED_LIBRARY)

# apk
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES += telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES += wfo-common

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := WfoService
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAGS := $(proguard.flags)

include $(BUILD_PACKAGE)
