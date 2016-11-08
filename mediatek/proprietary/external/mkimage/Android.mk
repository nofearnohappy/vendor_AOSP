LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := mkimage
LOCAL_SRC_FILES := mkimage.c
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_EXECUTABLE)
