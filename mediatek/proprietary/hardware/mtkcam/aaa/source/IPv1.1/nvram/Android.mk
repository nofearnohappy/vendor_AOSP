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
#
################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

#-----------------------------------------------------------
sinclude $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

#-----------------------------------------------------------
LOCAL_SRC_FILES += nvbuf_util.cpp
LOCAL_SRC_FILES += nvram_buf_mgr.cpp
LOCAL_SRC_FILES += nvram_drv.cpp

#Drv
MTKCAM_DRV_SENSOR_PATH := $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/src/sensor/$(PLATFORM)/
MTKCAM_DRV_ISP_INCLUDE := $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/src/isp/$(PLATFORM)/inc/
#ACDK
MTKCAM_ACDK := $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/include/$(PLATFORM)/acdk/

#------------------ip base new include path-----------------------
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/common/include/
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/
LOCAL_C_INCLUDES += $(MTKCAM_3A_INCLUDE)/
LOCAL_C_INCLUDES += $(MTKCAM_3A_INCLUDE)/Hal3


#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/nvram/libnvram/
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/nvram/nvramagentclient
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/bionic
LOCAL_C_INCLUDES += $(TOP)/external/stlport/stlport
LOCAL_C_INCLUDES += $(MTK_PATH_COMMON)/kernel/imgsensor/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/isp_tuning
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/mtkcam
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/camera_3a

LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/cgen/cfgfileinc

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)
#

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

#-----------------------------------------------------------
#
#LOCAL_STATIC_LIBRARIES += libfeatureiodrv_mem
#LOCAL_STATIC_LIBRARIES += libispfeature
#-----------------------------------------------------------
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libcutils
#
#LOCAL_SHARED_LIBRARIES += libstlport
#
LOCAL_SHARED_LIBRARIES += libcam.halsensor
LOCAL_SHARED_LIBRARIES += libnvramagentclient

LOCAL_SHARED_LIBRARIES += libdl
LOCAL_SHARED_LIBRARIES += libbinder

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_SHARED_LIBRARIES += libaed
endif
#
ifneq ($(BUILD_MTK_LDVT),true)
    LOCAL_SHARED_LIBRARIES += libnvram
    LOCAL_SHARED_LIBRARIES += libcameracustom
endif
#-----------------------------------------------------------
LOCAL_MODULE := libcam.hal3a.v3.nvram

ifeq ($(BUILD_MTK_LDVT),yes)
$(info chrisotpher)
    LOCAL_CFLAGS += -DUSING_MTK_LDVT
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/external/ldvt/include
    LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_INCLUDE)
    LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)
    LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)/iopipe
    LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)/drv
    LOCAL_SHARED_LIBRARIES += libcam.iopipe
    LOCAL_WHOLE_STATIC_LIBRARIES += libuvvf
endif

#-----------------------------------------------------------
include $(BUILD_SHARED_LIBRARY)

################################################################################
#
################################################################################
#include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

