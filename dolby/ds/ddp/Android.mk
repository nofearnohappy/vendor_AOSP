################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2011-2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

#DOLBY_UDC
ifdef DOLBY_UDC

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    DDPDecoder.cpp \
    src/ddpdec_client.c \
    src/udc_user.c \
    src/EndpConfig.cpp \
    src/ARenderer/Dap2JocProcess.cpp \
    src/ARenderer/ARenderer.cpp \
    src/ARenderer/evo_parser.c

LOCAL_STATIC_LIBRARIES := \
    libdlbadapters \
    libudc

ifdef DOLBY_UDC_VIRTUALIZE_AUDIO
    LOCAL_STATIC_LIBRARIES += \
        libdap2 \
        dlb_intrinsics \
        liboamdi_dec \
        libdlb_bitbuf
else
    LOCAL_SRC_FILES += stubs/dap2/dap_cpdp_stubs.c
    LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/stubs/dap2/include \
        $(LOCAL_PATH)/stubs/dap2/include/dlb_buffer/include \
        $(LOCAL_PATH)/stubs/dap2/include/dlb_bitbuf/include \
        $(LOCAL_PATH)/stubs/dap2/include/oamdi/include
endif #DOLBY_UDC_VIRTUALIZE_AUDIO

LOCAL_C_INCLUDES += \
    frameworks/av/media/libstagefright/include \
    frameworks/av/include/media/stagefright \
    frameworks/native/include/media/openmax \
    $(LOCAL_PATH)/src

LOCAL_CFLAGS := -DOSCL_UNUSED_ARG= -DOSCL_IMPORT_REF= -DDOLBY_DAP_16BIT_AUDIO

#DOLBY_UDC_FORCE_JOC_OUTPUT
# Always configure UDC in JOCD mode. This flag is
# mainly used for testing purpose.
ifeq ($(DOLBY_UDC_FORCE_JOC_OUTPUT), true)
LOCAL_CFLAGS += -DDOLBY_UDC_FORCE_JOC_OUTPUT
endif
#DOLBY_UDC_FORCE_JOC_OUTPUT_END

#DOLBY_OUTPUT_FOUR_BYTES_PER_SAMPLE
# Note: For 24bit, set environment variable
# DOLBY_OUTPUT_FOUR_BYTES_PER_SAMPLE and discard the
# least 8bits while copying data to Hardware Codec
ifdef DOLBY_OUTPUT_FOUR_BYTES_PER_SAMPLE
  LOCAL_CFLAGS += -DDOLBY_UDC_OUTPUT_FOUR_BYTES_PER_SAMPLE
else
  LOCAL_CFLAGS += -DDOLBY_UDC_OUTPUT_TWO_BYTES_PER_SAMPLE
endif
#DOLBY_OUTPUT_FOUR_BYTES_PER_SAMPLE_END

LOCAL_SHARED_LIBRARIES := \
    libstagefright \
    libstagefright_omx \
    libstagefright_foundation \
    libutils \
    libcutils

LOCAL_MODULE := libstagefright_soft_ddpdec

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_OWNER := dolby

LOCAL_PROPRIETARY_MODULE := true

LOCAL_32_BIT_ONLY := true

include $(BUILD_SHARED_LIBRARY)

endif
#DOLBY_UDC_END
