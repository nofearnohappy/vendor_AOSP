# Copyright 2006 The Android Open Source Project


# ===========================
# = Configuration of rild   =
# ===========================
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	simmelock_ipc.c

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libc
	
LOCAL_LDLIBS:= -llog
	
LOCAL_CFLAGS := -DRIL_SHLIB

LOCAL_MODULE := libsimmelock
LOCAL_MODULE_TAGS := eng

include $(BUILD_SHARED_LIBRARY)
