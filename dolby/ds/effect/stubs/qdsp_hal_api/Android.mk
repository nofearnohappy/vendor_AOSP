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

ifdef DOLBY_DAP_HW_QDSP_HAL_API

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := dap_hal_api.cpp

LOCAL_CFLAGS += -O3

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    liblog \
    libutils

LOCAL_MODULE := libhwdaphal
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := dolby

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
# Export the include path so it is automatically added to the project using this library
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/include

include $(BUILD_SHARED_LIBRARY)

endif # DOLBY_DAP_HW_QDSP_HAL_API
