LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libvoiceunlock
LOCAL_SRC_FILES_arm := libvoiceunlock_32.a
LOCAL_SRC_FILES_arm64 := libvoiceunlock_32.a
#LOCAL_SRC_FILES_arm64 := libvoiceunlock_64.a


LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_MULTILIB := 32
LOCAL_MODULE_SUFFIX := .a
include $(BUILD_PREBUILT)
