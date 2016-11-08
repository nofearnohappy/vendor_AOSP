# MTK_AUTO_TEST := yes
ifeq ($(strip $(MTK_AUTO_TEST)), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES := librobotium4
# robotium
# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src)
$(warning SRC is $(SRC))
LOCAL_PACKAGE_NAME := MediatekDMTests
LOCAL_INSTRUMENTATION_FOR := MediatekDM

include $(BUILD_PACKAGE)

endif

# MTK_AUTO_TEST := no
