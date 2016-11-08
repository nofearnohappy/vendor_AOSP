LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
		  direct-coredump.c

LOCAL_SHARED_LIBRARIES := libcutils libdl

LOCAL_MODULE := libdirect-coredump
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)