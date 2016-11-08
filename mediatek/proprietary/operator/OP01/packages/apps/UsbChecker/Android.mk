LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext

LOCAL_PACKAGE_NAME := UsbChecker
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

# LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk
