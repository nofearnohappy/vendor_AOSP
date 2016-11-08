ifneq ($(TARGET_BUILD_PDK),true)
# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.


# Copyright 2006 The Android Open Source Project

# XXX using libutils for simulator build only...
#

ifneq ($(strip $(MTK_PLATFORM)),)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

AAL_MTK_PLATFORM := $(shell echo $(MTK_PLATFORM) | tr A-Z a-z)

LOCAL_MODULE:= atci_service
LOCAL_MULTILIB := 32
LOCAL_MODULE_TAGS:=optional

LOCAL_SRC_FILES:= \
    src/atci_service.c \
    src/atci_generic_cmd_dispatch.c \
    src/atci_audio_cmd.cpp \
    src/atci_telephony_cmd.c \
    src/atci_system_cmd.c \
    src/atci_pq_cmd.cpp \
    src/atci_battery_cmd.c \
    src/atci_mjc_cmd.c \
    src/atci_util.c \
    src/at_tok.c

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libmedia \
    libbinder \
    liblog

ifeq ($(MTK_TC1_FEATURE),yes)
LOCAL_SHARED_LIBRARIES += libtc1part
LOCAL_CFLAGS += -DMTK_TC1_FEATURE
endif

ifeq ($(MTK_BLUEANGEL_SUPPORT),yes)
LOCAL_SHARED_LIBRARIES += libexttestmode
LOCAL_CFLAGS += -D__MTK_BT_SUPPORT__
endif

LOCAL_C_INCLUDES += \
#        $(KERNEL_HEADERS) \
        $(TOP)/frameworks/base/include

MTK_CCTIA_SUPPORT := no
ifeq ($(MTK_CCTIA_SUPPORT),yes)
#Add Include Path for CCT AT Command
$(warning CCTIA is built)

LOCAL_SRC_FILES += \
    src/atci_cct_cmd.cpp
    
ifeq ($(strip $(TARGET_BOARD_PLATFORM)),mt6735 mt6755)
LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/custom/common/kernel/imgsensor/inc \
    $(MTK_PATH_SOURCE)/hardware/meta/common/inc \
    $(MTK_PATH_SOURCE)/external/meta/common/inc \
    $(MTKCAM_C_INCLUDES)/.. \
    $(MTKCAM_C_INCLUDES) \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/include \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/acdk/inc/cct \
    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
    $(MTK_PATH_SOURCE)/hardware/meta/$(TARGET_BOARD_PLATFORM)/cameratool/D1/CCAP \
    $(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc
else ifeq ($(strip $(TARGET_BOARD_PLATFORM)),mt6735m)
LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/custom/common/kernel/imgsensor/inc \
    $(MTK_PATH_SOURCE)/external/meta/common/inc \
    $(MTK_PATH_SOURCE)/hardware/include \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/include/D2 \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D2/acdk/inc/cct \
    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cameratool/D2/CCAP \
    $(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D2/inc
else
LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/custom/common/kernel/imgsensor/inc \
    $(MTK_PATH_SOURCE)/external/meta/common/inc \
    $(MTK_PATH_SOURCE)/hardware/include \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/include \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/acdk/inc/cct \
    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cameratool/CCAP \
    $(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \
    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa
endif

LOCAL_STATIC_LIBRARIES += libccap
LOCAL_LDFLAGS += -ldl

LOCAL_CFLAGS += \
    -DENABLE_CCAP_AT_CMD
endif

LOCAL_C_INCLUDES += ${LOCAL_PATH}/../atci/src

ifeq ($(MTK_CLEARMOTION_SUPPORT),yes)
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/libmjc/common
endif

ifeq ($(MTK_GPS_SUPPORT),yes)

LOCAL_SRC_FILES += \
    src/atci_gps_cmd.c

LOCAL_CFLAGS += \
    -DENABLE_GPS_AT_CMD

endif

ifeq ($(strip $(MTK_AAL_SUPPORT)),yes)
    LOCAL_SHARED_LIBRARIES += \
        libaal \
        libskia \
        libgui \
        libui \
        libdl

    LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/aal/include

    LOCAL_CFLAGS += -DMTK_AAL_SUPPORT
endif

ifeq ($(strip $(MTK_OD_SUPPORT)),yes)
    LOCAL_CFLAGS += -DMTK_OD_SUPPORT
endif

ifeq ($(MTK_NFC_SUPPORT),yes)

LOCAL_SRC_FILES += \
    src/atci_nfc_cmd.c

LOCAL_CFLAGS += \
    -DENABLE_NFC_AT_CMD

LOCAL_C_INCLUDES += \
    $(TOP)/vendor/mediatek/proprietary/external/mtknfc/inc \
    $(TOP)/packages/apps/Nfc/mtk-nfc/jni-dload \

LOCAL_SHARED_LIBRARIES += \
    libmtknfc_dynamic_load_jni

endif

# Add Flags and source code for MMC AT Command
LOCAL_SRC_FILES += \
    src/atci_mmc_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_MMC_AT_CMD

# Add Flags and source code for CODECRC AT Command
LOCAL_SRC_FILES += \
    src/atci_code_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_CODECRC_AT_CMD

#Add Flags and source code for  backlight and  vibrator AT Command
LOCAL_SRC_FILES += \
    src/atci_lcdbacklight_vibrator_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_BLK_VIBR_AT_CMD

#Add Flags and source code for kpd AT Command
LOCAL_SRC_FILES += \
    src/atci_kpd_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_KPD_AT_CMD

#Add Flags and source code for touchpanel AT Command
LOCAL_SRC_FILES += \
    src/atci_touchpanel_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_TOUCHPANEL_AT_CMD

#Add Flags and source code for touchpanel AT Command

ifeq ($(MTK_BT_SUPPORT),yes)
LOCAL_SRC_FILES += \
    src/atci_bt_cmd.c
LOCAL_CFLAGS += \
    -DENABLE_BLUETOOTH_AT_CMD
endif

ifeq ($(GEMINI),yes)
LOCAL_CFLAGS += \
    -DGEMINI
endif

ifeq ($(MTK_GEMINI_3SIM_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_GEMINI_3SIM_SUPPORT
endif

ifeq ($(MTK_GEMINI_4SIM_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_GEMINI_4SIM_SUPPORT
endif



include $(BUILD_EXECUTABLE)

endif
endif
