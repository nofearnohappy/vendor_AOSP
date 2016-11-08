# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2015. All rights reserved.
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
include $(CLEAR_VARS)
################################################################################
LOCAL_SRC_FILES += StreamingFeature_Common.cpp
LOCAL_SRC_FILES += StreamingFeatureTimer.cpp
LOCAL_SRC_FILES += StreamingFeatureData.cpp
LOCAL_SRC_FILES += StreamingFeatureNode.cpp
LOCAL_SRC_FILES += GpuWarpBase.cpp
LOCAL_SRC_FILES += GpuWarp_v1.cpp
LOCAL_SRC_FILES += P2ANode.cpp
LOCAL_SRC_FILES += P2BNode.cpp
LOCAL_SRC_FILES += GPUNode.cpp
LOCAL_SRC_FILES += FDNode.cpp
LOCAL_SRC_FILES += VFBNode.cpp
LOCAL_SRC_FILES += MDPNode.cpp
LOCAL_SRC_FILES += EISNode.cpp
LOCAL_SRC_FILES += StreamingFeaturePipe.cpp
LOCAL_SRC_FILES += IStreamingFeaturePipe.cpp
################################################################################
include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
################################################################################
LOCAL_C_INCLUDES += $(call include-path-for, camera)
################################################################################
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/feature/core
################################################################################
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/feature/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/dpframework/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
################################################################################
LOCAL_C_INCLUDES += $(MTKCAM_ALGO_INCLUDE)
LOCAL_C_INCLUDES += $(MTKCAM_ALGO_INCLUDE)/libcore
LOCAL_C_INCLUDES += $(MTKCAM_ALGO_INCLUDE)/libutility
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)/drv
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)/iopipe
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/include/$(PLATFORM)
################################################################################
LOCAL_SHARED_LIBRARIES += libc
LOCAL_SHARED_LIBRARIES += libcamalgo
LOCAL_SHARED_LIBRARIES += libcam_utils
LOCAL_SHARED_LIBRARIES += libcam.iopipe
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libdpframework
LOCAL_SHARED_LIBRARIES += libgralloc_extra
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libstdc++
LOCAL_SHARED_LIBRARIES += libui
LOCAL_SHARED_LIBRARIES += libutils
################################################################################
LOCAL_STATIC_LIBRARIES += libmtkcam.featurepipe.core
################################################################################
LOCAL_MODULE := libmtkcam.featurepipe.streaming
include $(BUILD_SHARED_LIBRARY)
################################################################################
#
################################################################################
