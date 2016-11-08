ifneq ($(strip $(MTK_DISABLE_ATCIJ)), yes)
ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := AtciService
LOCAL_MULTILIB := both
LOCAL_CERTIFICATE := platform
LOCAL_REQUIRED_MODULES := libatciserv_jni

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
endif
