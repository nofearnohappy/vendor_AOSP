#
# Copyright (C) 2008 The Android Open Source Project
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

# This makefile supplies the rules for building a library of JNI code for
# use by our example of how to bundle a shared library with an APK.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

LOCAL_MODULE_TAGS := samples

# This is the target being built.
LOCAL_MODULE:= libjni_lomoeffect


# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
    matrixeffect_jni.cpp \


D_1 = mt6735 mt6753
D_2 = mt6735m
D_12 = mt6735 mt6753 mt6735m

#ifeq ($(MTK_CAM_LOMO_SUPPORT),yes)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include

#ifneq ($(TARGET_BOARD_PLATFORM), $(filter-out $(D_1), $(TARGET_BOARD_PLATFORM)))
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_MTKCAM_PLATFORM)/include/mtkcam/jni
#endif

#ifneq ($(TARGET_BOARD_PLATFORM), $(filter-out $(D_2), $(TARGET_BOARD_PLATFORM)))
#    LOCAL_C_INCLUDES += $(TOP)/$(MTK_MTKCAM_PLATFORM)/include/mtkcam/jni
#endif
ifeq ($(TARGET_BOARD_PLATFORM), $(filter-out $(D_12), $(TARGET_BOARD_PLATFORM)))
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/mtkcam/jni
endif
#endif
# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libcutils \
    libutils \
    libui \
    libgui \
    libskia \
    libsurfaceflinger \

# No static libraries.
LOCAL_STATIC_LIBRARIES :=

ifeq ($(MTK_CAM_LOMO_SUPPORT),yes)
#LOCAL_SHARED_LIBRARIES  +=	libcam.drv.sharelomohaljni
LOCAL_SHARED_LIBRARIES  +=	libcam.jni.lomohaljni
endif

# No special compiler flags.
LOCAL_CFLAGS +=

include $(BUILD_SHARED_LIBRARY)
