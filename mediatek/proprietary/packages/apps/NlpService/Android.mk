ifeq ($(MTK_GPS_SUPPORT),yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := NlpService
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := lbsutil

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))


include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := lbsutil:libs/lbsutils.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif

