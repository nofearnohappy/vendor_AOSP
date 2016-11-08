#
# libipod
#
ifeq (yes, $(strip $(MTK_IPO_SUPPORT)))

LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libipod
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES_arm := libipod.so
LOCAL_MULTILIB := 32
LOCAL_MODULE_SUFFIX := .so

include $(BUILD_PREBUILT)

endif
