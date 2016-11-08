ifeq ($(MTK_NFC_OMAAC_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := SmartcardService
LOCAL_SRC_FILES := ./SmartcardService.apk
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif

#android:versionName="3.2.1.22.2
