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
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

ifeq ($(BUILD_MTK_LDVT),yes)
    LOCAL_CFLAGS += -DUSING_MTK_LDVT
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/external/ldvt/include
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/ldvt/$(PLATFORM)/include
    LOCAL_WHOLE_STATIC_LIBRARIES += libuvvf
endif

#-----------------------------------------------------------
LOCAL_SRC_FILES += DummyNode.cpp
LOCAL_SRC_FILES += BaseNode.cpp
LOCAL_SRC_FILES += P1Node.cpp
LOCAL_SRC_FILES += P2Node.cpp
LOCAL_SRC_FILES += JpegNode.cpp
#[EP_TEMP]#LOCAL_SRC_FILES += HwEventIrq.cpp
LOCAL_SRC_FILES += RAW16Node.cpp
#[EP_TEMP}#LOCAL_SRC_FILES += StereoRootNode.cpp
#[EP_TEMP}#LOCAL_SRC_FILES += P2aNode.cpp
#[EP_TEMP}#LOCAL_SRC_FILES += DualImageTransformNode.cpp
#LOCAL_SRC_FILES += HDRNode.cpp

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/feature/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/common/include/metadata
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/utils/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_ALGO_INCLUDE)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/include/$(PLATFORM)
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)/iopipe
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_DRV_INCLUDE)/drv
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_HAL_INCLUDE)
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_3A_INCLUDE)
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_3A_INCLUDE)/Hal3
#
LOCAL_C_INCLUDES += $(TOP)/system/media/camera/include
#LOCAL_C_INCLUDES += $(TOP)/external/jpeg
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/mtkjpeg
#
#LOCAL_C_INCLUDES += $(TOP)/bionic
#LOCAL_C_INCLUDES += $(TOP)/external/stlport/stlport
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/core
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/core/imageio_common/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/isp_tuning

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)
#

#-----------------------------------------------------------
LOCAL_STATIC_LIBRARIES +=
#
LOCAL_WHOLE_STATIC_LIBRARIES += libcam3_pipeline.fdNode

#-----------------------------------------------------------
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libcam_utils
LOCAL_SHARED_LIBRARIES += libcam.metadata
LOCAL_SHARED_LIBRARIES += libcam.metadataprovider
#
#LOCAL_SHARED_LIBRARIES += libstlport
LOCAL_SHARED_LIBRARIES += libstdc++

LOCAL_SHARED_LIBRARIES += libcam.iopipe
LOCAL_SHARED_LIBRARIES += libcam.halsensor
LOCAL_SHARED_LIBRARIES += libfeature_eis
#[EP_TEMP]#LOCAL_SHARED_LIBRARIES += libcamdrv
# JpegNode
LOCAL_SHARED_LIBRARIES += libcam.iopipe
#FIXME:TEMP_SKIP_JPEG_NODE_BUILD
LOCAL_SHARED_LIBRARIES += libcam.exif.v3
LOCAL_SHARED_LIBRARIES += libmtkjpeg
# irq
#[EP_TEMP]#LOCAL_SHARED_LIBRARIES += libcamdrv
# sensor
LOCAL_SHARED_LIBRARIES += libcam.utils.sensorlistener
# face feature
LOCAL_SHARED_LIBRARIES += libfeature.face
#-----------------------------------------------------------
# aaa
MTKCAM_3A_SIMULATOR := no
#$(info MTKCAM_3A_SIMULATOR = "$(MTKCAM_3A_SIMULATOR)")

ifeq ($(MTKCAM_3A_SIMULATOR),yes)
LOCAL_CFLAGS += -DUSING_3A_SIMULATOR=1
LOCAL_CFLAGS += -DUSING_3A_SIMULATOR_SOF=1
else
LOCAL_CFLAGS += -DUSING_3A_SIMULATOR=0
LOCAL_CFLAGS += -DUSING_3A_SIMULATOR_SOF=0
LOCAL_SHARED_LIBRARIES += libcam.hal3a.v3
endif

#-----------------------------------------------------------
LOCAL_MODULE := libcam3_hwnode

#-----------------------------------------------------------
include $(BUILD_SHARED_LIBRARY)



################################################################################
#
################################################################################
include $(call all-makefiles-under,$(LOCAL_PATH))

