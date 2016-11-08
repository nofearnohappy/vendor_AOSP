LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libhotknot
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES_arm := lib/libhotknot.so
LOCAL_SRC_FILES_arm64 := lib64/libhotknot.so
LOCAL_MODULE_SUFFIX := .so
LOCAL_MULTILIB := both
include $(BUILD_PREBUILT)

