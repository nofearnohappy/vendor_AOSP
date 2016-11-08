################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  Dap2Process.cpp

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/include \
    external/gtest/include

LOCAL_EXPORT_C_INCLUDE_DIRS := \
    $(LOCAL_PATH)/include \
    external/gtest/include

ifdef DOLBY_UDC_VIRTUALIZE_AUDIO
    LOCAL_STATIC_LIBRARIES := \
        dlb_intrinsics \
        libdap2 \
        liboamdi_dec
else
    LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/../ddp/stubs/dap2/include \
        $(LOCAL_PATH)/../ddp/stubs/dap2/include/dlb_buffer/include \
        $(LOCAL_PATH)/../ddp/stubs/dap2/include/dlb_bitbuf/include
endif #DOLBY_UDC_VIRTUALIZE_AUDIO


LOCAL_MODULE := libdlbadapters
LOCAL_MODULE_TAGS  := optional
LOCAL_MODULE_OWNER := dolby

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	liblog \
	libutils

include $(BUILD_STATIC_LIBRARY)
