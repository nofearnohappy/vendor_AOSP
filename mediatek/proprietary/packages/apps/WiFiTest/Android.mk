LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := eng
LOCAL_PACKAGE_NAME := WiFiTest
LOCAL_PRIVILEGED_MODULE := true

LOCAL_CERTIFICATE := platform




include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
# include $(call all-makefiles-under,$(LOCAL_PATH))

