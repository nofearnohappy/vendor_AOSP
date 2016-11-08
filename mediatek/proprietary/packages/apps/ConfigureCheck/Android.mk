LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under)

LOCAL_PACKAGE_NAME := ConfigureCheck

LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.lbs.em2.utils


include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))

