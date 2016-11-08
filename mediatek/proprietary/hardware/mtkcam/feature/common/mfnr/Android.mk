## Copyright Statement:
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
#

MFLL_ENABLE := no # yes/no

ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
ifneq ($(strip $(MTK_CAM_MFB_SUPPORT)),0)
# TODO: to enable MFLL here
endif
endif

#
# MFLL config start from here.
#
ifeq ($(strip $(MFLL_ENABLE)), yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

MFLL_INCLUDE_PATH := $(LOCAL_PATH)/include

#
# Define MFLL core (algorithm) version and dependent middleware version
#
MFLL_CORE_VERSION_MAJOR := 1
MFLL_CORE_VERSION_MINOR := 5
MFLL_MODULE_VERSION := default

#
# MFLL core library name
#
MFLL_CORE_LIB_NAME := libmfllcore

#
# if defined, log will be displayed to stdout (yes or no)
#
MFLL_LOG_STDOUT := no

#
# Define share library.
#
MFLL_SHARED_LIBS := libutils
MFLL_SHARED_LIBS += libcutils

ifeq ($(strip $(MFLL_MIDDLEWARE_VERSION)),1.0)
MFLL_SHARED_LIBS += libm4u
MFLL_SHARED_LIBS += libcam.camshot
MFLL_SHARED_LIBS += libmedia
MFLL_SHARED_LIBS += libcamalgo
MFLL_SHARED_LIBS += libcameracustom
MFLL_SHARED_LIBS += libfeatureio
MFLL_SHARED_LIBS += libcam_utils
MFLL_SHARED_LIBS += libcam.iopipe
MFLL_SHARED_LIBS += libcam_hwutils
endif # MFLL_MFB_VER

# MFLL module static link lib name, usually you don't need to modify this.
MFLL_MODULE_LIB_NAME := libmfllmodules

include $(call all-makefiles-under, $(LOCAL_PATH))

endif # MFLL_ENABLE

