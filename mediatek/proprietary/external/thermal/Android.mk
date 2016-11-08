# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE:= thermal

#bobule workaround pdk build error, needing review
LOCAL_MULTILIB := 32

LOCAL_MODULE_TAGS:= optional 

LOCAL_SRC_FILES:= \
    thermal.c 
 
LOCAL_C_INCLUDES = \
 $(LOCAL_PATH)/ \
 $(TOPDIR)/hardware/libhardware_legacy/include \
 $(TOPDIR)/hardware/libhardware/include \
 $(MTK_PATH_SOURCE)/hardware/ccci/include

LOCAL_SHARED_LIBRARIES := libcutils libc libnetutils

include $(BUILD_EXECUTABLE)
