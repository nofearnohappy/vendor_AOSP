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

ifdef DOLBY_DAP_SW

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    CrossfadeProcess.cpp \
    DapBufferAdapter.cpp \
    EndpointParamCache.cpp \
    DlbBufferProvider.cpp \
    EffectDap.cpp

LOCAL_STATIC_LIBRARIES := \
    libdapcommon \
    libdlbadapters

ifdef DOLBY_DAP2
    LOCAL_STATIC_LIBRARIES += \
        libdap2 \
        dlb_intrinsics \
        liboamdi_dec
    LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/include \
        external/gtest/include
else
    LOCAL_SRC_FILES += Dap1Process.cpp
    LOCAL_STATIC_LIBRARIES += \
        libdap1
    LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/include \
        external/gtest/include
endif

ifdef DOLBY_AUDIO_DUMP
    LOCAL_SRC_FILES += DapPcmDump.cpp
endif

LOCAL_CFLAGS += -O3

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    liblog \
    libutils

LOCAL_MODULE:= libswdap
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := dolby

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/include \
    external/gtest/include \

LOCAL_PROPRIETARY_MODULE := true
LOCAL_32_BIT_ONLY := true
LOCAL_MODULE_RELATIVE_PATH := soundfx

include $(BUILD_SHARED_LIBRARY)

#include $(LOCAL_PATH)/Android.gtest.mk
endif
