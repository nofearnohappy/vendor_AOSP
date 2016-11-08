# Copyright 2006 The Android Open Source Project


# ===========================================
# = Configuration of libvsim-adaptor-client =
# ===========================================
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	vsim_adaptor_ipc.c

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libc
	
LOCAL_LDLIBS:= -llog
	
LOCAL_CFLAGS := -DRIL_SHLIB

LOCAL_MODULE := libvsim-adaptor-client
LOCAL_MODULE_TAGS := eng

include $(BUILD_SHARED_LIBRARY)
