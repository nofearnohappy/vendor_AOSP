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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_CFLAGS += -DANDROID_KITKAT
LOCAL_MODULE := invmpld
LOCAL_MODULE_PATH := $(TARGET_OUT)/bin
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := invmpld.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/mllite
LOCAL_C_INCLUDES += $(LOCAL_PATH)/mllite/linux
LOCAL_SHARED_LIBRARIES += libcutils
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libdl
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_SHARED_LIBRARIES += libmllite
LOCAL_SHARED_LIBRARIES += libpaw8001motion
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := libmllite
LOCAL_SRC_FILES := libmllite.so
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := invensense
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
OVERRIDE_BUILT_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libpaw8001motion
LOCAL_SRC_FILES := libpaw8001motion.so
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := pixart
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
OVERRIDE_BUILT_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := icm30628fw.bin
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT)/etc/firmware
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := icm30628dmp4.bin
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT)/etc/firmware
include $(BUILD_PREBUILT)

