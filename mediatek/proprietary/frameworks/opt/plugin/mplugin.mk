# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2012. All rights reserved.
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

# MPlugin.
# ============================================================
# Define plug-in's upinfo file path, which will be the same as the APK
# with ".mpinfo" extension.
MPINFO_OUT := $(basename $(LOCAL_INSTALLED_MODULE)).mpinfo

# Generate mpinfo file using MPlugin tool.
$(MPINFO_OUT): PRIVATE_LOCAL_INSTALLED_MODULE := $(basename $(LOCAL_INSTALLED_MODULE)).mpinfo
$(MPINFO_OUT): PRIVATE_LOCAL_MODULE := $(LOCAL_MODULE)
$(MPINFO_OUT): PRIVATE_PATH := $(LOCAL_PATH)
$(MPINFO_OUT): $(full_classes_jar) $(full_java_lib_deps) | $(ACP)
	@mkdir -p $(dir $@)
	@java -jar vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin-tool.jar $(PRIVATE_PATH) $(PRIVATE_LOCAL_MODULE) $(PRIVATE_PATH)/AndroidManifest.xml $(filter-out $< $(word 1,$^),$^) $(word 1,$^) $(PRIVATE_LOCAL_INSTALLED_MODULE)

# We need this so that the installed files could be picked up based on the
# local module name.
ALL_MODULES.$(LOCAL_MODULE).INSTALLED += $(MPINFO_OUT)
$(LOCAL_MODULE): $(MPINFO_OUT)

