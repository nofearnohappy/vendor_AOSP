LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext

LOCAL_PACKAGE_NAME := Op01RcsGenericUI
LOCAL_CERTIFICATE := platform

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
