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

###############################################################################
# build start
###############################################################################

ifeq ($(MTK_NFC_SUPPORT), yes)
MTK_NFC_HAL=0
MTK_NFC_SKIP=0
BUILD_PORTING=no
BUILD_HALIMPL=no
BUILD_MW_LIB=no
AOSP_NFC_PATH=$(strip $(wildcard $(MTK_PATH_SOURCE)/hardware/nfc/Android.mk))
MTK_NFC_PATH=$(strip $(wildcard $(MTK_PATH_SOURCE)/external/mtknfc/Android.mk))

LOCAL_PATH := $(call my-dir)

ifndef MTK_NFC_PACKAGE
    
    ifeq ($(strip $(MTK_BASIC_PACKAGE)), yes)
        MTK_NFC_HAL=AOSP_B
    else ifeq ($(strip $(MTK_BSP_PACKAGE)), yes)
        MTK_NFC_HAL=AOSP_B
    else
        MTK_NFC_HAL=MTK
    endif         
else
    MTK_NFC_HAL=$(MTK_NFC_PACKAGE)
endif
#compare conflict fold
ifneq ($(MTK_NFC_PATH),null)
    ifneq ($(AOSP_NFC_PATH),null)
        MTK_NFC_SKIP=1
    endif  
else
    MTK_NFC_SKIP=0
endif    

########################################
# build option (yes/no)
########################################
ifeq ($(MTK_NFC_HAL), MTK)
    ifeq ($(MTK_NFC_SKIP),1)
        ifneq ($(LOCAL_PATH), $(MTK_PATH_SOURCE)/hardware/nfc)
BUILD_PORTING=yes
BUILD_HALIMPL=yes
            BUILD_MW_LIB=yes
        endif
    else
        BUILD_PORTING=yes
        BUILD_HALIMPL=yes
        BUILD_MW_LIB=yes
    endif    
else ifeq ($(MTK_NFC_HAL), AOSP_B)
    ifeq ($(MTK_NFC_SKIP),1)
        ifneq ($(LOCAL_PATH), $(MTK_PATH_SOURCE)/external/mtknfc)
            BUILD_PORTING=yes
            BUILD_HALIMPL=yes
            BUILD_MW_LIB=yes
        endif
    else
        BUILD_PORTING=yes
        BUILD_HALIMPL=yes
        BUILD_MW_LIB=yes
    endif   
endif

$(info BUILD_PORTING=$(BUILD_PORTING))
$(info BUILD_HALIMPL=$(BUILD_HALIMPL))
$(info BUILD_MW_LIB=$(BUILD_MW_LIB))
$(info MTK_NFC_HAL=$(MTK_NFC_HAL))
########################################
# prebuilt MTK NFC lib
########################################
ifeq ($(BUILD_MW_LIB), yes)
include $(LOCAL_PATH)/lib/Android.mk
include $(LOCAL_PATH)/lib64/Android.mk
# $(call add-prebuilt-files, SHARED_LIBRARIES, libmtknfc.so)

MY_LOCAL_PATH := $(LOCAL_PATH)
endif ########ifeq ($(BUILD_MW_LIB), yes)


########################################
# MTK NFC Executable
########################################
ifeq ($(BUILD_PORTING), yes)
include $(LOCAL_PATH)/PL.mk
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_SBIN_UNSTRIPPED)
LOCAL_MULTILIB := 32
include $(BUILD_EXECUTABLE)

endif ########ifeq ($(BUILD_PORTING), yes)

  
########################################
# MTK NFC AOSP HAL
########################################
ifeq ($(BUILD_HALIMPL), yes)
# Build shared library system/lib/hw/nfc_nci.*.so for Hardware Abstraction Layer.
# Android's generic HAL (libhardware.so) dynamically loads this shared library.
include $(LOCAL_PATH)/halimpl.mk
#LOCAL_MULTILIB := both
LOCAL_MODULE_RELATIVE_PATH := hw

include $(BUILD_SHARED_LIBRARY)
endif ########ifeq ($(BUILD_HALIMPL), yes)
endif ########ifeq ($(MTK_NFC_SUPPORT), yes)
