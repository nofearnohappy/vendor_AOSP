# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
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

ifeq ($(TARGET_BUILD_PDK),)

ifdef MTK_PLATFORM
ifneq ($(strip $(MTK_PLATFORM)),banyan)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

$(call make-private-dependency,\
  $(BOARD_CONFIG_DIR)/configs/StageFright.mk \
)

LOCAL_SRC_FILES:=	\
	MtkVideoTranscoder.cpp

LOCAL_SHARED_LIBRARIES := \
	libstagefright libstagefright_foundation liblog libutils libbinder libdpframework

LOCAL_C_INCLUDES:= \
	$(JNI_H_INCLUDE) \
	frameworks/base/media/libstagefright \
	$(TOP)/$(MTK_ROOT)/frameworks/native/include/media/openmax \
	$(TOP)/$(MTK_ROOT)/hardware/dpframework/inc \


LOCAL_C_INCLUDES += \
	$(TOP)/$(MTK_PATH_SOURCE)/external/mhal/inc

LOCAL_CFLAGS += -Wno-multichar

ifeq ($(strip $(BOARD_USES_ANDROID_DEFAULT_CODE)),true)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif

LOCAL_MODULE_TAGS := optional

LOCAL_PRELINK_MODULE := false

LOCAL_MODULE:= libMtkVideoTranscoder
#LOCAL_MULTILIB := 32

include $(BUILD_SHARED_LIBRARY)




#include $(CLEAR_VARS)

#LOCAL_SRC_FILES:=	\
#	TranscoderTest.cpp

#LOCAL_SHARED_LIBRARIES := f
#	liblog libdl libutils libbinder libpowermanager
	
#LOCAL_MODULE_TAGS := optional

#LOCAL_MODULE:= TranscoderTest

#include $(BUILD_EXECUTABLE)

endif#ifneq ($(strip $(MTK_PLATFORM)),banyan)
endif#ifdef MTK_PLATFORM

endif #ifeq ($(TARGET_BUILD_PDK),)
