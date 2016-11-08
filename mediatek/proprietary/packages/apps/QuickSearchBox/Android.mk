#
# Copyright (C) 2014 MediaTek Inc.
# Modification based on code covered by the mentioned copyright
# and/or permission notice(s).
#
#
# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

BUILD_QSB := yes

# M: Operator Specific Check to disable QSB
ifeq ($(OPTR_SPEC_SEG_DEF), OP09_SPEC0212_SEGDEFAULT)
   BUILD_QSB := no
endif

# M: A1 Specific Check to disable QSB
ifdef MTK_A1_FEATURE
    ifeq ($(strip $(MTK_A1_FEATURE)),yes)
       BUILD_QSB := no
    endif
endif

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                        org.apache.http.legacy

LOCAL_STATIC_JAVA_LIBRARIES := \
    guava \
    android-common \
    com.mediatek.qsb.ext

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-logtags-files-under, src)

LOCAL_OVERRIDES_PACKAGES := QuickSearchBox
LOCAL_PACKAGE_NAME := MtkQuickSearchBox
LOCAL_CERTIFICATE := shared

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# M: Disable QSB for specific feature checks
ifeq ($(strip $(BUILD_QSB)), yes)
    include $(BUILD_PACKAGE)

    # Also build our test apk
    include $(call all-makefiles-under,$(LOCAL_PATH))
endif
