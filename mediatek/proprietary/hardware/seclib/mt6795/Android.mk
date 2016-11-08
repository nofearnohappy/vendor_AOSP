LOCAL_PATH:= $(call my-dir)

###############################################################################
# SEC DYNAMIC LIBRARY
###############################################################################

include $(CLEAR_VARS)
LOCAL_MODULE := libsecdl
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES_arm := lib/libsecdl.so
LOCAL_SRC_FILES_arm64 := lib64/libsecdl.so
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
LOCAL_MULTILIB := both 
include $(BUILD_PREBUILT)

