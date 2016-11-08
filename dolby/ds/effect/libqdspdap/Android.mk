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

ifdef DOLBY_DAP_HW

DOLBY_QDSP_PLATFORMS := apq8084 msm8974 msm8226

ifneq ($(filter $(DOLBY_QDSP_PLATFORMS), $(TARGET_BOARD_PLATFORM)),)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	EffectQdspDap.cpp \
	QdspParams.cpp

LOCAL_CFLAGS += -O3

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	liblog \
	libutils

LOCAL_STATIC_LIBRARIES := \
    libdapcommon \
    libdlbadapters

LOCAL_MODULE := libhwdap
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := dolby

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include

ifdef DOLBY_DAP_HW_QDSP_HAL_API
	LOCAL_SRC_FILES += QdspHalDriver.cpp
	LOCAL_SHARED_LIBRARIES += libhwdaphal
else
	LOCAL_SRC_FILES += QdspAlsaDriver.cpp

	LOCAL_C_INCLUDES += \
		$(TOP)/external/tinyalsa/include \
		$(TOP)/hardware/qcom/audio/hal/msm8974

	LOCAL_SHARED_LIBRARIES += libtinyalsa
endif#DOLBY_DAP_HW_QDSP_HAL_API

LOCAL_32_BIT_ONLY := true
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_RELATIVE_PATH := soundfx

include $(BUILD_SHARED_LIBRARY)

endif # filter TARGET_BOARD_PLATFORM
endif # DOLBY_DAP_HW
