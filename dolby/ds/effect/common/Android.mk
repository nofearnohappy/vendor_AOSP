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

ifdef DOLBY_DAP

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	DlbEffect.cpp \
	EffectContext.cpp \
	EffectParamParser.cpp \
	DapParamCache.cpp \
	ProfileParamParser.cpp

LOCAL_CFLAGS += -O3
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/include

LOCAL_MODULE       := libdapcommon
LOCAL_MODULE_TAGS  := optional
LOCAL_MODULE_OWNER := dolby

LOCAL_STATIC_LIBRARIES := libdlbadapters
LOCAL_SHARED_LIBRARIES := \
    libcutils \
    liblog \
    libutils

LOCAL_32_BIT_ONLY := true
LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_STATIC_LIBRARY)

endif
