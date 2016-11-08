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

################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
#-----------------------------------------------------------
MTKCAM_HAVE_MTKENG              ?= '1'
MTKCAM_HAVE_SENSOR_HAL          ?= '1'
MTKCAM_HAVE_3A_HAL              ?= '0'
MTKCAM_HAVE_CAMSHOT             ?= '0'
MTKCAM_HAVE_VIDEO_SNAPSHOT      ?= '1'
MTKCAM_HAVE_EIS                 ?= '1'

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)
#
ifeq "'1'" "$(strip $(MTKCAM_HAVE_SENSOR_HAL))"
LOCAL_CFLAGS += -DMTKCAM_HAVE_SENSOR_HAL="$(MTKCAM_HAVE_SENSOR_HAL)"
endif
#
ifeq "'1'" "$(strip $(MTKCAM_HAVE_3A_HAL))"
LOCAL_CFLAGS += -DMTKCAM_HAVE_3A_HAL="$(MTKCAM_HAVE_3A_HAL)"
endif
#
ifeq "'1'" "$(strip $(MTKCAM_HAVE_CAMSHOT))"
LOCAL_CFLAGS += -DMTKCAM_HAVE_CAMSHOT="$(MTKCAM_HAVE_CAMSHOT)"
endif

#------------------------------------------------------
ifeq "'1'" "$(strip $(MTKCAM_HAVE_MTKENG))"    
LOCAL_SRC_FILES += \
    ./v3/CaptureCmdQueThread.cpp \
    ./v3/DefaultAdapter.3A.cpp \
    ./v3/DefaultAdapter.Capture.cpp \
    ./v3/DefaultAdapter.CaptureCallback.cpp \
    ./v3/DefaultAdapter.Preview.cpp \
    ./v3/DefaultAdapter.Record.cpp \
    ./v3/DefaultAdapter.cpp \
    ./v3/DefaultAdapter.Param.cpp \
    ./v3/ZipImageCallbackThread.cpp \
    ./v3/DefaultAdapter.dump.cpp \   
      
endif


#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)
LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/utils/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/$(MTKCAM_HAL_VERSION)/v1/adapter
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/$(MTKCAM_HAL_VERSION)/v1/adapter/inc
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/$(MTKCAM_HAL_VERSION)/v1/adapter/Scenario/Shot/EngShot
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc
LOCAL_C_INCLUDES += $(MTKCAM_HAL_INCLUDE)
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)
#
LOCAL_C_INCLUDES += $(TOP)/bionic $(TOP)/external/stlport/stlport
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/include
#
LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES_PATH)
LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES_PATH)/inc
LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES_PATH)/Scenario/Shot/EngShot
#
LOCAL_C_INCLUDES += $(MTK_PATH_COMMON)/hal/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc/isp_tuning
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc/debug_exif/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc/isp_tuning
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc/debug_exif/aaa
#
$(warning "[Remove Me] should not include directly")
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/featureio/pipe/aaa
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/featureio/pipe/aaa/lsc_mgr
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/featureio/pipe/aaa/isp_tuning
LOCAL_C_INCLUDES += $(TOP)/system/media/camera/include
#
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1/mtkcam/algorithm/libcore
#
LOCAL_STATIC_LIBRARIES += 
#-----------------------------------------------------------
LOCAL_WHOLE_STATIC_LIBRARIES += libcam.camadapter.flowControl


#-----------------------------------------------------------
LOCAL_MODULE := libcam.camadapter.default

#-----------------------------------------------------------
ifeq "'1'" "$(strip $(MTKCAM_HAVE_MTKENG))"
include $(BUILD_STATIC_LIBRARY)
endif


################################################################################
#
################################################################################
include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

