LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
# MTK_CIP_SUPPORT := yes
# Module name should match apk name to be installed
LOCAL_MODULE := regionalphone.db
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/regionalphone

ifeq ($(strip $(MTK_CIP_SUPPORT)), yes)
LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/etc/regionalphone
endif

include $(BUILD_PREBUILT)
