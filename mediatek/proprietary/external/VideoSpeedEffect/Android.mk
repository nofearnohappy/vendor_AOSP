# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2014. All rights reserved.
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


ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)

ifeq ($(strip $(MTK_SLOW_MOTION_VIDEO_SUPPORT)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES += \
				VideoSpeedEffect.cpp
				
LOCAL_C_INCLUDES:= \
				$(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/include \
				$(TOP)/frameworks/av/services/audioflinger \
				$(TOP)/$(MTK_PATH_SOURCE)/external/AudioTimeStretch/inc \
        $(TOP)/frameworks/av/include \
         $(TOP)/frameworks/native/include \
        $(TOP)/frameworks/av/include/mediapwd \
        $(TOP)/$(MTK_ROOT)/frameworks/native/include/media/openmax \
        	$(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/media/libstagefright/include/omx_core \
				$(TOP)/$(MTK_PATH_SOURCE)/hardware/dpframework/inc \
				$(TOP)/$(MTK_PATH_SOURCE)/external/mhal/src/core/drv/inc \
				$(TOP)/$(MTK_PATH_SOURCE)/external/mhal/inc \
  
LOCAL_SHARED_LIBRARIES := \
      libbinder                 \
      libmedia                  \
      libutils                  \
      libcutils                 \
      libui		        \
      libgui                    \
      libskia                   \
      libstagefright            \
      libstagefright_foundation \
      libtimestretch	        \
      libMTKAudioTimeStretch	\
      libdpframework            \
      libvcodecdrv              \
      
      
LOCAL_CFLAGS += -Wno-multichar
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libMtkVideoSpeedEffect

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))

################################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=         \
        VideoSpeedEffectTest.cpp

LOCAL_SHARED_LIBRARIES := \
	libstagefright liblog libutils libcutils libbinder libstagefright_foundation

LOCAL_SHARED_LIBRARIES  += \
        libMtkVideoSpeedEffect \

LOCAL_C_INCLUDES:= \
	frameworks/av/media/libstagefright \
	$(TOP)/frameworks/native/include/media/openmax

LOCAL_CFLAGS += -Wno-multichar

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= VideoSpeedEffectTest
LOCAL_32_BIT_ONLY := true

include $(BUILD_EXECUTABLE)

################################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=         \
        VideoParseInfoTest.cpp

LOCAL_SHARED_LIBRARIES := \
	libstagefright liblog libutils libcutils libbinder libstagefright_foundation

LOCAL_SHARED_LIBRARIES  += \

LOCAL_C_INCLUDES:= \
	$(TOP)/frameworks/av/include \
  $(TOP)/frameworks/native/include \
  $(TOP)/frameworks/av/media/libstagefright \
  $(TOP)/frameworks/av/include/media/stagefright \

LOCAL_CFLAGS += -Wno-multichar

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= VideoParseInfoTest
LOCAL_32_BIT_ONLY := true

include $(BUILD_EXECUTABLE)


endif


endif
