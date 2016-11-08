ifeq ($(strip $(MTK_AUTO_TEST)), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES := librobotium4 junit-report

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SmsRegTests
LOCAL_INSTRUMENTATION_FOR := SmsReg

include $(BUILD_PACKAGE)

endif
