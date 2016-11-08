LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE:= thermald

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
    thermald.cpp 
 
LOCAL_SHARED_LIBRARIES := libcutils libc libutils libbinder

include $(BUILD_EXECUTABLE)
