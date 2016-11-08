ifeq ($(MTK_GPS_SUPPORT),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
            $(call all-subdir-java-files) \

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PACKAGE_NAME := FlpEM2
LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES := com.android.location.provider

include $(BUILD_PACKAGE)

endif