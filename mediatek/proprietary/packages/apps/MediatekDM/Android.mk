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


ifeq ($(MTK_MDM_APP), yes)

define all-common-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          ! -path "*mediatekdm/fumo*" \
          ! -path "*mediatekdm/scomo*" \
          ! -path "*mediatekdm/lawmo*" \
          ! -path "*mediatekdm/volte*" \
          ! -path "*mediatekdm/wfhs*" \
          ! -path "*mediatekdm/andsf*" \
          ! -path "*mdm/fumo*" \
          ! -path "*mdm/scomo*" \
          ! -path "*mdm/lawmo*" ) \
          ! -path "*mdm/andsf*" ) \
 )
endef

define all-fumo-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/fumo*" -o -path "*mdm/fumo*" \) ) \
 )
endef

define all-scomo-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/scomo*" -o -path "*mdm/scomo*" \) ) \
 )
endef

define all-lawmo-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/lawmo*" -o -path "*mdm/lawmo*" \) ) \
 )
endef

define all-volte-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/volte*" \) ) \
 )
endef

define all-wfhs-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/wfhs*" \) ) \
 )
endef

define all-andsf-files-under
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find $(1) -name "*.java" ! -name ".*" \
          \( -path "*mediatekdm/andsf*" -o -path "*mdm/andsf*" \) ) \
 )
endef

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PRELINK_MODULE := false

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.lbs.em2.utils

MDM_SRC_DIR := src/com/mediatek/mediatekdm

LOCAL_SRC_FILES := $(call all-common-files-under, $(MDM_SRC_DIR))

ifeq ($(MTK_MDM_FUMO), yes)
$(warning FUMO enabled)
LOCAL_SRC_FUMO_FILES := $(call all-fumo-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_FUMO_FILES)
endif

ifeq ($(MTK_MDM_SCOMO), yes)
$(warning SCOMO enabled)
LOCAL_SRC_SCOMO_FILES := $(call all-scomo-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_SCOMO_FILES)
endif

ifeq ($(MTK_MDM_LAWMO), yes)
$(warning LAWMO enabled)
LOCAL_SRC_LAWMO_FILES := $(call all-lawmo-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_LAWMO_FILES)
endif

ifeq ($(MTK_MDM_VOLTE), yes)
$(warning VOLTE enabled)
LOCAL_SRC_VOLTE_FILES := $(call all-volte-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_VOLTE_FILES)
endif

ifeq ($(MTK_PASSPOINT_R2_SUPPORT), yes)
$(warning WFHS enabled)
LOCAL_SRC_WFHS_FILES := $(call all-wfhs-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_WFHS_FILES)
endif

ifeq ($(MTK_MDM_ANDSF), yes)
$(warning ANDSF enabled)
LOCAL_SRC_ANDSF_FILES := $(call all-andsf-files-under, $(MDM_SRC_DIR))
LOCAL_SRC_FILES += $(LOCAL_SRC_ANDSF_FILES)
endif

LOCAL_REQUIRED_MODULES := libjni_mdm

LOCAL_PACKAGE_NAME := MediatekDM
LOCAL_CERTIFICATE := platform

# disable proguard option
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
